package game

import Message
import chess.ChessColour
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import util.Interval
import util.rust
import kotlin.time.seconds

class PlayerConnection(
	private val conn: WebSocketServerSession
) {
	var playing = false
		private set
	var colour: ChessColour? = null
		private set
	var game: Game? = null
		private set
	
	private var exited = false
	private val listeners: MutableMap<Int, HashSet<Listener>> = mutableMapOf()
	
	abstract class Listener {
		abstract val id: Int
		abstract suspend fun callback(msg: Message)
		abstract suspend fun stop()
	}
	suspend fun listenFor(id: Int, fn: suspend Listener.(Message) -> Unit): Listener {
		val listener = object : Listener() {
			override val id: Int = id
			override suspend fun callback(msg: Message) = fn(msg)
			override suspend fun stop() {
				listenersMutex.withLock {
					listeners[id]?.remove(this)
				}
			}
		}
		listenersMutex.withLock {
			val eventListeners = listeners[id] ?: run {
				val set = HashSet<Listener>(1)
				listeners[id] = set
				set
			}
			
			eventListeners.add(listener)
			return listener
		}
	}
	suspend fun once(id: Int, fn: suspend (Message) -> Unit) {
		val listener = object : Listener() {
			override val id: Int = id
			override suspend fun callback(msg: Message) {
				fn(msg)
				stop()
			}
			override suspend fun stop() {
				listenersMutex.withLock {
					listeners[id]?.remove(this)
				}
			}
		}
		listenersMutex.withLock {
			val eventListeners = listeners[id] ?: run {
				val set = HashSet<Listener>(1)
				listeners[id] = set
				set
			}
			eventListeners.add(listener)
		}
	}
	suspend fun waitFor(id: Int): Message {
		val channel = Channel<Message>(1)
		once(id) {
			channel.sendBlocking(it)
		}
		return channel.receive()
	}
	
	private val listenersMutex = Mutex()
	private suspend fun notify(msg: Message) = listenersMutex.withLock {
		listeners[msg.opcode]?.forEach { rust { it.callback(msg) } }
	}
	
	suspend fun handleMessages() {
		once(Message.Exit.OPCODE) {
			exited = true
			conn.close(CloseReason(CloseReason.Codes.NORMAL, ""))
		}
		
		sendMessage(Message.Hello)
		val arr = conn.incoming.receive().buffer
		if (Message.decode(arr.array()) !is Message.Hello) {
			throw Error("Broken connection")
		}
		
		notify(Message.Ready)
		
		Interval(5.seconds) {
			sendMessage(Message.Ping)
			val hasReceivedPing = withTimeoutOrNull(10.seconds) {
				waitFor(Message.Ping.OPCODE)
			} != null
			if (!hasReceivedPing) {
				notify(Message.Exit)
			}
		}.run { once(Message.Exit.OPCODE) { stop() } }
		
		try {
			for (packet in conn.incoming) {
				val data = packet.data
				if (data.isEmpty()) {
					continue
				}
				val msg = try {
					Message.decode(packet.data)
				} catch (e: Error) {
					if (e.message == "EOF") {
						throw ClosedReceiveChannelException("Client sent invalid message")
					} else {
						throw e
					}
				}
				notify(msg)
				
				if (exited) {
					break
				}
			}
			notify(Message.Exit)
		} catch (e: ClosedReceiveChannelException) {
			notify(Message.Exit)
		}
	}
	
	suspend fun sendMessage(msg: Message) =
		conn.outgoing.send(Frame.Binary(true, msg.pack()))
	
	fun play(g: Game, c: ChessColour) {
		require(game == null)
		game = g
		colour = c
		playing = true
	}
	
	suspend fun disconnect() = conn.close()
}
