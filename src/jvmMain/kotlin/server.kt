
import game.PlayerConnection
import game.game
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import matchmaker.Matchmaker
import util.Logger

fun main() {
	embeddedServer(
		Netty,
		host = "0.0.0.0",
		port = 8080,
	) {
		install(WebSockets)
		
		routing {
			get("/") { call.respondHtml(HttpStatusCode.OK, index) }
			get("/game/{id}") {
				val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.NotFound)
				val game = game(id) ?: return@get call.respond(HttpStatusCode.NotFound)
				
				call.respondHtml(HttpStatusCode.OK, game)
			}
			get("/new-game") {
				val id = Matchmaker.createPendingGame()
				call.respondRedirect("/game/$id")
			}
			
			webSocket("/ws/{id}") {
				val id = call.parameters["id"] ?: return@webSocket call.respond(HttpStatusCode.NotFound)
				val game = Matchmaker.waitingGames[id] ?: return@webSocket call.respond(HttpStatusCode.NotFound)
				Logger.info("($id) Received connection")
				val conn = PlayerConnection(this)
				val task = async { conn.handleMessages() }
				game.enter(conn)
				task.await()
			}
			
			static("/static") { resources("static") }
		}
	}.start(wait = true)
}
