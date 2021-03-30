package util
import chess.ChessColour
import chess.ChessPiece
import org.w3c.dom.Element
import org.w3c.dom.HTMLCollection
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.get

fun HTMLCollection.toArray(): Array<Element> {
	return Array(length) { this[it]!! }
}

fun EventTarget.on(name: String, fn: (Event) -> Unit) {
	addEventListener(name, { fn(it) })
}

fun ChessPiece.cssClass() = buildString(2) {
	append(when (colour) {
		ChessColour.WHITE -> 'w'
		ChessColour.BLACK -> 'b'
	})
	if (this@cssClass is ChessPiece.Knight) {
		append('n')
	} else {
		append(name[0].toLowerCase())
	}
}