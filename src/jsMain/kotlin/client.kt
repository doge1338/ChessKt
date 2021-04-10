import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.get
import util.on

val gameId = ((document.currentScript as HTMLScriptElement).attributes["game-id"] ?: throw Error("Game ID not specified")).value
val isInvite = (document.currentScript as HTMLScriptElement).attributes["invited"] != null

fun main() {
	window.on("DOMContentLoaded") {
		Game(gameId, isInvite)
	}
}
