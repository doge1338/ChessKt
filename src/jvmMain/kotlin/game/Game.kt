package game

import Message
import chess.ChessColour
import chess.ChessGame
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.html.*
import matchmaker.Matchmaker
import util.Logger
import util.materialIcon
import kotlin.random.Random
import kotlin.time.seconds

class Game(
	val code: String,
	host: PlayerConnection,
	guest: PlayerConnection,
) {
	val white: PlayerConnection
	val black: PlayerConnection
	
	fun player(c: ChessColour) = when (c) {
		ChessColour.WHITE -> white
		ChessColour.BLACK -> black
	}
	
	var chessGame: ChessGame? = null
		private set
	var gameStarted: Boolean = false
		private set
	var ended: Boolean = false
		private set
	
	init {
		if (Random.nextBoolean()) {
			white = host
			black = guest
		} else {
			white = guest
			black = host
		}
	}
	
	suspend fun broadcast(msg: Message) {
		white.sendMessage(msg)
		black.sendMessage(msg)
	}
	
	suspend fun end() {
		try {
			broadcast(Message.Exit)
		} catch (e: Throwable) {}
		Matchmaker.endGame(code)
		ended = true
		delay(2.seconds)
		white.disconnect()
		black.disconnect()
	}
	
	suspend fun init() {
		Logger.info("($code) Game started!")
		white.sendMessage(Message.PieceColour(true))
		black.sendMessage(Message.PieceColour(false))
		
		white.listenFor(Message.Chat.OPCODE) { it as Message.Chat
			val escaped = it.msg.escapeHTML().trim()
			if (escaped.isNotBlank() && escaped.length < 256) {
				Logger.info("($code) W: $escaped")
				broadcast(Message.Chat("White", escaped))
			}
		}
		black.listenFor(Message.Chat.OPCODE) { it as Message.Chat
			val escaped = it.msg.escapeHTML().trim()
			if (escaped.isNotBlank() && escaped.length < 256) {
				Logger.info("($code) B: $escaped")
				broadcast(Message.Chat("Black", escaped))
			}
		}
		
		white.listenFor(Message.Exit.OPCODE) {
			Logger.info("($code) White exited early")
			end()
		}
		black.listenFor(Message.Exit.OPCODE) {
			Logger.info("($code) Black exited early")
			end()
		}
		
		delay(10.seconds)
		gameStarted = true
		start()
	}
	
	private suspend fun start() {
		broadcast(Message.StartGame)
		
		val game = ChessGame().also { chessGame = it }
		while (true) {
			val cur = player(game.currentPlayer)
			cur.sendMessage(Message.YourMove)
			val move = cur.waitFor(Message.BasicMove.OPCODE) as Message.BasicMove
			
			val state = game.makeMove(Pair(move.x, move.y), Pair(move.x1, move.y1))
			
			// This shouldn't happen unless the player messes with the source code trying to send invalid moves
			// In that case just ignore the message
			if (state == ChessGame.GameState.ILLEGAL_MOVE) {
				continue
			}
			player(game.currentPlayer).sendMessage(move)
			if (state == ChessGame.GameState.CONTINUE) {
				continue
			}
			Logger.info("($code) Game ended with: $state")
			// Mate
			// Don't end the game so the players can still chat
			//end()
			break
		}
	}
}

fun game(gameId: String): (HTML.() -> Unit)? {
	val game = Matchmaker.waitingGames[gameId] ?: return null
	
	return {
		head {
			title("Chess")
			link(href = "/static/main.css", rel = "stylesheet")
			link(href = "https://fonts.gstatic.com", rel = "preconnect")
			link(href = "https://fonts.googleapis.com/css2?family=Lato:wght@400;700&display=swap", rel = "stylesheet")
			link(href = "https://fonts.googleapis.com/icon?family=Material+Icons", rel = "stylesheet")
			meta("viewport", "width=device-width, initial-scale=1.0")
			script(src = "/static/main.js") {
				attributes["game-id"] = gameId
				if (!game.isPending) {
					attributes["invited"] = "true"
				}
			}
		}
		body {
			div {
				id = "content"
				div {
					id = "mainLabel"
					+"Joining game..."
				}
				div {
					id = "gameContent"
					
					div {
						id = "chat"
						div {
							id = "chatbox"
						}
						div {
							id = "chatInput"
							input(type = InputType.text) {
								spellCheck = false
								maxLength = "256"
							}
							materialIcon("send")
						}
					}
				}
			}
		}
	}
}
