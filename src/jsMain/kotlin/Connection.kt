import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.khronos.webgl.Int8Array
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.fetch.Response
import org.w3c.files.Blob
import util.on

class Connection(gameId: String) {
	private val ws: WebSocket
	var ready: Boolean? = false
		private set
	
	private var isCloseExpected = false
	
	// Initialize websocket and event handlers
	init {
		val protocol = if (window.location.protocol == "https:") "wss:" else "ws:"
		val host = window.location.host
		ws = WebSocket("$protocol//$host/ws/$gameId")
		
		ws.on("open") {
			GlobalScope.launch {
				waitFor(Message.Hello.OPCODE)
				send(Message.Hello)
				ready = true
				handleMessage(Message.Ready)
			}
		}
		ws.on("message") { it as MessageEvent
			GlobalScope.launch {
				handleMessage(Message.decode(Int8Array(
					Response(it.data as Blob).arrayBuffer().await()
				).unsafeCast<ByteArray>()))
			}
		}
		ws.on("close") {
			if (ready != null && !isCloseExpected) {
				window.alert("Connection to the server was unexpectedly closed.")
			}
		}
		ws.on("error") {
			window.alert("Connection error: $it")
		}
	}
	
	fun send(msg: Message) {
		ws.send(msg.pack().unsafeCast<Int8Array>().buffer)
	}
	
	private val handlers: MutableMap<Int, ArrayList<suspend (Message) -> Unit>> = mutableMapOf()
	
	private fun handleMessage(msg: Message) {
		handlers[msg.opcode]?.forEach { handler ->
			GlobalScope.launch { handler(msg) }
		}
	}
	
	fun addHandler(id: Int, handler: suspend (Message) -> Unit) {
		val handlers = handlers[id] ?: run {
			handlers[id] = arrayListOf()
			handlers[id]!!
		}
		handlers += handler
	}
	fun removeHandler(id: Int, handler: suspend (Message) -> Unit) {
		handlers[id]!!.remove(handler)
	}
	fun once(id: Int, handler: suspend (Message) -> Unit) {
		suspend fun proxyHandler(msg: Message) {
			handler(msg)
			removeHandler(id, ::proxyHandler)
		}
		addHandler(id, ::proxyHandler)
	}
	suspend fun waitFor(id: Int): Message {
		val chan = Channel<Message>()
		once(id) { chan.send(it) }
		return chan.receive()
	}
	
	fun close() {
		isCloseExpected = true
		ws.close()
	}
	
	// Initialize basic message handlers
	init {
		addHandler(Message.Ping.OPCODE) {
			send(Message.Ping)
		}
	}
}