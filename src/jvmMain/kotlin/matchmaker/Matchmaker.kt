package matchmaker

import Message
import game.Game
import game.PlayerConnection
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import util.Interval
import util.Logger
import util.rotate
import util.rust
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.time.TimeSource
import kotlin.time.seconds

object Matchmaker {
	// Holds played games
	val games: MutableMap<String, Game> = mutableMapOf()
	
	fun endGame(code: String) = games.remove(code)
	
	class WaitingGame(val code: String) {
		// Used to expire pending games after 30 seconds
		var createdAt = TimeSource.Monotonic.markNow()
			private set
		// Whether the game is not joined by anyone yet
		var isPending = true
			private set
		
		private var host: PlayerConnection? = null
		
		// Closes the game and removes it from waitingGames
		fun close() = synchronized(waitingGames) {
			waitingGames.remove(code)
		}
		
		val connectionListeners = ArrayList<PlayerConnection.Listener>(2)
		
		private val enterMutex = Mutex()
		// Enters a player into the game
		suspend fun enter(conn: PlayerConnection) = enterMutex.withLock {
			conn.waitFor(Message.Ready.OPCODE)
			if (isPending) {
				host = conn
				isPending = false
				// Echo chat
				connectionListeners += conn.listenFor(Message.Chat.OPCODE) { it as Message.Chat
					val escaped = it.msg.escapeHTML().trim()
					if (escaped.isNotBlank()) {
						conn.sendMessage(Message.Chat("Host", escaped))
					}
				}
				// Mark the game as pending again after the host leaves
				connectionListeners += conn.listenFor(Message.Exit.OPCODE) {
					Logger.info("($code): Host disconnected")
					enterMutex.withLock {
						isPending = true
						host = null
						connectionListeners
							.onEach { it.stop() }
							.clear()
						createdAt = TimeSource.Monotonic.markNow()
					}
				}
			} else {
				val game = Game(code, host!!, conn)
				synchronized(waitingGames) {
					waitingGames.remove(code)
				}
				synchronized(games) {
					games[code] = game
				}
				connectionListeners
					.onEach { it.stop() }
					.clear()
				rust {
					game.init()
				}
			}
		}
	}
	// Holds games in which either:
	// 1. A game is yet to be joined by anyone
	// 2. A player is already waiting for another player
	val waitingGames: MutableMap<String, WaitingGame> = mutableMapOf()
	
	// Expire pending games
	init {
		Interval(30.seconds) {
			if (waitingGames.isNotEmpty()) {
				val toRemove = ArrayList<String>()
				synchronized(waitingGames) {
					waitingGames.forEach { (code, game) ->
						if (game.isPending && game.createdAt.elapsedNow() >= 30.seconds) {
							Logger.info("($code) Removed game after a 30 seconds timeout")
							toRemove += code
						}
					}
					toRemove.forEach { waitingGames.remove(it) }
				}
			}
		}
	}
	
	// I don't think there'll be any more than 65k games at once
	var ctr: AtomicInteger = AtomicInteger(Random.nextInt().toShort().toInt())
	@Synchronized
	fun createPendingGame(): String {
		val code = (ctr.getAndIncrement()).toShort().rotate()
		ctr.set(ctr.get() and 0xffff)
		synchronized(waitingGames) {
			waitingGames[code] = WaitingGame(code)
		}
		Logger.info("($code) Created pending game")
		return code
	}
}
