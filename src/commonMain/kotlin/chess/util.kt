package chess

val <T, K>Pair<T, K>.x
	get() = first
val <T, K>Pair<T, K>.y
	get() = second

inline fun assert(b: Boolean, msg: () -> Any? = {null}) {
	if (!b) {
		val m = msg()
		if (m == null) {
			throw Error("Assertion error")
		} else {
			throw Error("Assertion error: $m")
		}
	}
}