sealed class Message(val opcode: Int) {
	abstract fun pack(): ByteArray
	
	companion object {
		fun decode(arr: ByteArray): Message {
			return when (Buffer(arr).readInt()) {
				-3 -> Ready
				-2 -> Exit
				-1 -> Hello
				0 -> Ping
				1 -> Chat(arr)
				2 -> PieceColour(arr)
				3 -> GuestJoined
				4 -> StartGame
				5 -> BasicMove(arr)
				6 -> YourMove
				else -> throw UnsupportedOperationException("Unknown opcode ${arr[0]}")
			}
		}
	}
	
	object Ready: Message(-3) {
		const val OPCODE: Int = -3
		override fun pack() = Buffer(4).write(OPCODE).collect()
	}
	object Exit: Message(-2) {
		const val OPCODE: Int = -2
		override fun pack() = Buffer(4).write(OPCODE).collect()
	}
	object Hello: Message(-1) {
		const val OPCODE: Int = -1
		override fun pack() = Buffer(4).write(OPCODE).collect()
	}
	object Ping: Message(0) {
		const val OPCODE: Int = 0
		override fun pack() = Buffer(4).write(OPCODE).collect()
	}
	class Chat: Message {
		companion object { const val OPCODE: Int = 1 }
		val author: String; val msg: String
		
		constructor(author: String, msg: String): super(OPCODE) {
			this.author = author; this.msg = msg
		}
		constructor(arr: ByteArray): super(OPCODE) {
			Buffer(arr)
				.apply {
					require(OPCODE == read<Int>())
					author = read(); msg = read()
				}
		}
		
		override fun pack(): ByteArray {
			return Buffer(1 + 4 + author.length + msg.length)
				.write(opcode, author, msg)
				.collect()
		}
	}
	class PieceColour: Message {
		companion object { const val OPCODE: Int = 2 }
		val isWhite: Boolean
		
		constructor(isWhite: Boolean): super(OPCODE) {
			this.isWhite = isWhite
		}
		constructor(arr: ByteArray): super(OPCODE) {
			Buffer(arr)
				.apply {
					require(OPCODE == read<Int>())
					isWhite = read()
				}
		}
		
		override fun pack(): ByteArray {
			return Buffer(1+1)
				.write(opcode, isWhite)
				.collect()
		}
	}
	object GuestJoined: Message(3) {
		const val OPCODE: Int = 3
		override fun pack() = Buffer(4).write(OPCODE).collect()
	}
	object StartGame: Message(4) {
		const val OPCODE: Int = 4
		override fun pack() = Buffer(4).write(OPCODE).collect()
	}
	class BasicMove: Message {
		companion object { const val OPCODE: Int = 5 }
		val x: Int; val y: Int; val x1: Int; val y1: Int
		
		constructor(x: Int, y: Int, x1: Int, y1: Int): super(OPCODE) {
			this.x = x; this.y = y; this.x1 = x1; this.y1 = y1
		}
		constructor(arr: ByteArray): super(OPCODE) {
			Buffer(arr)
				.apply {
					require(OPCODE == read<Int>())
					x = read(); y = read(); x1 = read(); y1 = read()
				}
		}
		
		override fun pack(): ByteArray {
			return Buffer(1+16)
				.write(opcode, x, y, x1, y1)
				.collect()
		}
	}
	object YourMove: Message(6) {
		const val OPCODE: Int = 6
		override fun pack() = Buffer(4).write(OPCODE).collect()
	}
	
	override fun toString(): String {
		return "($opcode : ${this::class.simpleName})"
	}
}

