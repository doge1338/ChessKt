class Buffer {
	var buf: ByteArray
		private set
	var capacity: Int
		private set
	var pointer = 0
		private set
	
	constructor(initialCapacity: Int) {
		buf = ByteArray(initialCapacity)
		capacity = initialCapacity
	}
	
	constructor(arr: ByteArray) {
		buf = arr
		capacity = arr.size
	}
	
	private fun ensureCapacity(delta: Int): Buffer {
		if (pointer+delta+1 >= capacity) {
			val oldBuf = buf
			while (pointer+delta+1 >= capacity) {
				capacity *= 2
			}
			buf = ByteArray(capacity)
			oldBuf.copyInto(buf)
		}
		return this
	}
	private fun assertCapacity(delta: Int): Buffer {
		if (pointer+delta >= capacity) {
			throw Error("EOF")
		}
		return this
	}
	// Returns the buffer byte array sliced to pointer
	fun collect(): ByteArray {
		return buf.sliceArray(0..pointer)
	}
	
	fun write(v: Long): Buffer {
		ensureCapacity(8)
		buf[pointer++] = ((v ushr 0)  and 0xff).toByte()
		buf[pointer++] = ((v ushr 8)  and 0xff).toByte()
		buf[pointer++] = ((v ushr 16) and 0xff).toByte()
		buf[pointer++] = ((v ushr 24) and 0xff).toByte()
		buf[pointer++] = ((v ushr 32) and 0xff).toByte()
		buf[pointer++] = ((v ushr 40) and 0xff).toByte()
		buf[pointer++] = ((v ushr 48) and 0xff).toByte()
		buf[pointer++] = ((v ushr 54) and 0xff).toByte()
		return this
	}
	fun write(v: Int): Buffer {
		ensureCapacity(4)
		buf[pointer++] = ((v ushr 0)  and 0xff).toByte()
		buf[pointer++] = ((v ushr 8)  and 0xff).toByte()
		buf[pointer++] = ((v ushr 16) and 0xff).toByte()
		buf[pointer++] = ((v ushr 24) and 0xff).toByte()
		return this
	}
	fun write(v: Short): Buffer {
		ensureCapacity(2)
		buf[pointer++] = (v % 256).toByte()
		buf[pointer++] = (v / 256).toByte()
		return this
	}
	fun write(v: Char) = write(v.toShort())
	fun write(b: Byte): Buffer {
		ensureCapacity(1)
		buf[pointer++] = b
		return this
	}
	fun write(arr: ByteArray): Buffer {
		ensureCapacity(4+arr.size)
		write(arr.size)
		for (b in arr) {
			buf[pointer++] = b
		}
		return this
	}
	fun write(s: String) = write(s.encodeToByteArray())
	fun write(b: Boolean) = write(if (b) 1 else 0)
	
	fun write(vararg mt: Any): Buffer {
		mt.forEach {
			when (it) {
				is Long -> write(it)
				is Int -> write(it)
				is Short -> write(it)
				is Char -> write(it)
				is Byte -> write(it)
				is ByteArray -> write(it)
				is String -> write(it)
				is Boolean -> write(it)
				else -> throw UnsupportedOperationException("Can't write type '${it::class.simpleName}'")
			}
		}
		return this
	}
	
	inline fun<reified T> read(): T = when(T::class) {
		Long::class -> readLong() as T
		Int::class -> readInt() as T
		Short::class -> readShort() as T
		Char::class -> readChar() as T
		Byte::class -> readByte() as T
		ByteArray::class -> readBytes() as T
		String::class -> readString() as T
		Boolean::class -> readBoolean() as T
		else -> throw UnsupportedOperationException("Can't read type '${T::class.simpleName}'")
	}
	
	fun readLong(): Long {
		assertCapacity(8)
		var cur = 0L
		cur = cur or (buf[pointer++].toLong() and 0xff shl 0)
		cur = cur or (buf[pointer++].toLong() and 0xff shl 8)
		cur = cur or (buf[pointer++].toLong() and 0xff shl 16)
		cur = cur or (buf[pointer++].toLong() and 0xff shl 24)
		cur = cur or (buf[pointer++].toLong() and 0xff shl 32)
		cur = cur or (buf[pointer++].toLong() and 0xff shl 40)
		cur = cur or (buf[pointer++].toLong() and 0xff shl 48)
		cur = cur or (buf[pointer++].toLong() and 0xff shl 56)
		return cur
	}
	fun readInt(): Int {
		assertCapacity(4)
		var cur = 0
		cur = cur or (buf[pointer++].toInt() and 0xff shl 0)
		cur = cur or (buf[pointer++].toInt() and 0xff shl 8)
		cur = cur or (buf[pointer++].toInt() and 0xff shl 16)
		cur = cur or (buf[pointer++].toInt() and 0xff shl 24)
		return cur
	}
	fun readShort(): Short {
		assertCapacity(2)
		var cur = 0
		cur = cur or (buf[pointer++].toInt() and 0xff shl 0)
		cur = cur or (buf[pointer++].toInt() and 0xff shl 8)
		return cur.toShort()
	}
	fun readChar() = readShort().toChar()
	fun readByte(): Byte {
		assertCapacity(1)
		return buf[pointer++]
	}
	fun readBytes(): ByteArray {
		val len = readInt()
		assertCapacity(len)
		val arr = buf.sliceArray(pointer until pointer+len)
		pointer += len
		return arr
	}
	fun readString() = readBytes().decodeToString()
	fun readBoolean() = readByte() != 0.toByte()
	
	// Writes n null bytes to the buffer
//	fun writeNullBytes(n: Int) {
//		ensureCapacity(n)
//		repeat(n) {
//			buf[pointer++] = 0
//		}
//	}
//	// Skips n bytes in the buffer
//	fun ignoreBytes(n: Int) {
//		assertCapacity(n)
//		pointer += n
//	}
//	// Jumps to a position in the buffer
//	fun jumpTo(ptr: Int) {
//		require(ptr in 0 until capacity)
//		pointer = ptr
//	}
}