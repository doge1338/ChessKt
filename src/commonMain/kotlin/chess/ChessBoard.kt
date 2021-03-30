package chess

import kotlin.math.abs

class ChessBoard {
	val array = Array(8) { Array<ChessPiece?>(8) { null } }
	
	init {
		val initSide = { n1: Int, n2: Int, colour: ChessColour ->
			for (c in 'a'..'h') {
				put(c, n2, colour, ChessPiece::Pawn)
			}
			put('a', n1, colour, ChessPiece::Rook)
			put('b', n1, colour, ChessPiece::Knight)
			put('c', n1, colour, ChessPiece::Bishop)
			put('d', n1, colour, ChessPiece::Queen)
			put('e', n1, colour, ChessPiece::King)
			put('f', n1, colour, ChessPiece::Bishop)
			put('g', n1, colour, ChessPiece::Knight)
			put('h', n1, colour, ChessPiece::Rook)
		}
		initSide(1, 2, ChessColour.WHITE)
		initSide(8, 7, ChessColour.BLACK)
	}
	
	private val pieces = hashSetOf(*array.flatten().filterNotNull().toTypedArray())
	
	fun visualize(): String {
		val builder = StringBuilder()
		for (y in 7 downTo 0) {
			for (x in 0..7) {
				val piece = this[x][y]
				if (piece != null) {
					val sym = when(piece.colour) {
						ChessColour.WHITE -> "\u001B[31m"+piece.symbol+"\u001B[0m"
						ChessColour.BLACK -> "\u001B[34m"+piece.symbol+"\u001B[0m"
					}
					builder.append(sym)
				} else {
					builder.append(' ')
				}
			}
			builder.append('\n')
		}
		return builder.toString()
	}
	
	val whiteKing = pieceAt('e', 1) as ChessPiece.King
	val blackKing = pieceAt('e', 8) as ChessPiece.King
	fun king(c: ChessColour) = when (c) {
		ChessColour.WHITE -> whiteKing
		ChessColour.BLACK -> blackKing
	}
	fun rooks(c: ChessColour): List<ChessPiece.Rook> {
		return pieces.filterIsInstance<ChessPiece.Rook>().filter { it.colour == c }
	}
	fun isCheck(c: ChessColour): Boolean {
		return isEndangered(king(c).pos, c)
	}
	fun isMate(c: ChessColour): Boolean {
		// Copy the set to avoid ConcurrentModificationException
		return HashSet(pieces).none { piece ->
			piece.colour == c && piece.availableMoves().any { pos -> !isMoveForbidden(piece, pos.x, pos.y) }
		}
	}
	
	
	companion object {
		fun fromBoardPosition(a: Char, b: Int): Pair<Int, Int> {
			require(a in 'a'..'h' && b in 1..8)
			return Pair(a.toLowerCase()-'a', b-1)
		}
		fun stringifyPosition(x: Int, y: Int): String {
			require(x in 0..7 && y in 0..7)
			return "${(x+'a'.toInt()).toChar()}${y+1}"
		}
	}
	
	fun pieceAt(x: Int, y: Int): ChessPiece? {
		if (x in 0..7 && y in 0..7) {
			return this[x][y]
		}
		return null
	}
	fun hasPieceAt(x: Int, y: Int): Boolean {
		return x in 0..7 && y in 0..7 && this[x][y] != null
	}
	fun hasColouredPieceAt(x: Int, y: Int, colour: ChessColour): Boolean {
		return x in 0..7 && y in 0..7 && this[x][y]?.colour == colour
	}
	fun isEndangered(checkedPos: Pair<Int, Int>, colour: ChessColour) =
		pieces.any { piece -> piece.colour != colour && piece.canHit(checkedPos)  }
	
	fun setPieceAt(a: Char, b: Int, p: ChessPiece) {
		require(a in 'a'..'h' && b in 1..8)
		this[a.toLowerCase()-'a'][b-1] = p
	}
	fun <T: ChessPiece>put(a: Char, b: Int, colour: ChessColour, c: (ChessColour, ChessBoard, Pair<Int, Int>) -> T) {
		require(a in 'a'..'h' && b in 1..8)
		setPieceAt(a, b, c(colour, this, fromBoardPosition(a, b)))
	}
	// Helper functions that can work with stuff like `board.pieceAt('c', 5)`
	fun pieceAt(a: Char, b: Int): ChessPiece? {
		require(a in 'a'..'h' && b in 1..8)
		return pieceAt(a.toLowerCase()-'a', b-1)
	}
	fun hasPieceAt(a: Char, b: Int) = pieceAt(a, b) != null
	
	fun movePiece(x: Int, y: Int, nx: Int, ny: Int) {
		val piece = this[x][y] ?: run {
			println(visualize())
			throw Error("(MOVE - ($x, $y) -> ($nx, $ny)) No piece at $x:$y (\n${visualize()}\n)")
		}
		if (this[nx][ny] is ChessPiece.King) {
			println(visualize())
			throw Error("(MOVE - ($x, $y) -> ($nx, $ny)) Logic error: king cannot be killed (\n${visualize()}\n)")
		}
		if (this[nx][ny]?.colour == piece.colour) {
			println(visualize())
			throw Error("(MOVE - ($x, $y) -> ($nx, $ny)) Logic error: cannot kill friendly pieces")
		}
		// Promotion
		if (piece is ChessPiece.Pawn && (ny == 7 || ny == 0)) {
			pieces.remove(piece)
			val np = ChessPiece.Queen(piece.colour, this, Pair(nx, ny))
			pieces.add(np)
			this[x][y] = null
			this[nx][ny] = np
			return
		}
		// Castling
		if (piece is ChessPiece.King && abs(piece.pos.x-nx) > 1) {
			piece.pos = Pair(nx, ny)
			this[nx][ny] = piece
			this[x][y] = null
			
			val mul = if (nx == 6) 1 else -1
			val rook = this[nx+mul][ny]!!
			this[nx-mul][ny] = rook
			rook.pos = Pair(nx-mul, ny)
			this[nx+mul][ny] = null
			
			piece.hasMoved = true
			rook.hasMoved = true
			return
		}
		pieces.remove(this[nx][ny])
		this[nx][ny] = piece
		this[x][y] = null
		piece.pos = Pair(nx, ny)
		piece.hasMoved = true
	}
	
	
	// Returns true if a move is forbidden due to a check
	fun isMoveForbidden(piece: ChessPiece, nx: Int, ny: Int): Boolean {
		if (this[nx][ny] is ChessPiece.King) {
			return true
		}
		return try {
			val savedState = simulateMove(piece.pos.x, piece.pos.y, nx, ny)
			val isChecked = isCheck(piece.colour)
			revertMove(savedState)
			isChecked
		} catch (e: Error) {
			println("SIM ERROR: $e")
			true
		}
	}
	
	class SimMove(
		val x: Int, val y: Int, val nx: Int, val ny: Int,
		val killed: ChessPiece?,
		val hasNotMoved: Boolean = false,
		val hasPromoted: Boolean = false,
		val castle: Boolean = false,
	)
	private fun simulateMove(x: Int, y: Int, nx: Int, ny: Int): SimMove {
		val piece = this[x][y] ?: run {
			println(visualize())
			throw Error("(SIM - ($x, $y) -> ($nx, $ny)) No piece at $x:$y")
		}
		if (this[nx][ny] is ChessPiece.King) {
			println(visualize())
			throw Error("(SIM - ($x, $y) -> ($nx, $ny)) Logic error: king cannot be killed")
		}
		if (this[nx][ny]?.colour == piece.colour) {
			println(visualize())
			throw Error("(SIM - ($x, $y) -> ($nx, $ny)) Logic error: cannot kill friendly pieces")
		}
		val killed = this[nx][ny]
		pieces.remove(killed)
		// Promotion
		if (piece is ChessPiece.Pawn && (ny == 7 || ny == 0)) {
			pieces.remove(piece)
			val np = ChessPiece.Queen(piece.colour, this, Pair(nx, ny))
			pieces.add(np)
			this[nx][ny] = np
			this[x][y] = null
			return SimMove(x, y, nx, ny, killed, hasPromoted = true)
		}
		// Castling
		if (piece is ChessPiece.King && abs(piece.pos.x-nx) > 1) {
			piece.pos = Pair(nx, ny)
			this[nx][ny] = piece
			this[x][y] = null
			
			val mul = if (nx == 6) 1 else -1
			val rook = this[nx+mul][ny]!!
			this[nx-mul][ny] = rook
			rook.pos = Pair(nx-mul, ny)
			this[nx+mul][ny] = null
			
			piece.hasMoved = true
			rook.hasMoved = true
			return SimMove(x, y, nx, ny, /* shouldn't be possible, but still... */ killed, hasNotMoved = true, castle = true)
		}
		this[nx][ny] = piece
		this[x][y] = null
		piece.pos = Pair(nx, ny)
		piece.hasMoved = false
		return SimMove(x, y, nx, ny, killed, !piece.hasMoved)
	}
	private fun revertMove(sim: SimMove) {
		val piece = this[sim.nx][sim.ny]!!
		if (sim.castle) {
			piece.pos = Pair(sim.x, sim.y)
			this[sim.x][sim.y] = piece
			this[sim.nx][sim.ny] = sim.killed
			// Revert rook
			val mul = if (sim.nx == 6) 1 else -1
			val rook = this[sim.nx-mul][sim.ny]!!
			this[sim.nx+mul][sim.ny] = rook
			rook.pos = Pair(sim.nx+mul, sim.ny)
			this[sim.nx-mul][sim.ny] = null
			
			piece.hasMoved = false
			rook.hasMoved = false
			return
		}
		if (sim.hasPromoted) {
			val pawn = ChessPiece.Pawn(piece.colour, this, Pair(sim.x, sim.y))
			pawn.hasMoved = true
			this[sim.x][sim.y] = pawn
			this[sim.nx][sim.ny] = sim.killed
			sim.killed?.also { pieces.add(it) }
			pieces.remove(piece)
			return
		}
		piece.pos = Pair(sim.x, sim.y)
		this[sim.x][sim.y] = piece
		this[sim.nx][sim.ny] = sim.killed
		sim.killed?.also { pieces.add(it) }
		
		piece.hasMoved = !sim.hasNotMoved
	}
	
	fun squareColour(x: Int, y: Int) = when {
		x%2 == 0 -> when {
			y%2 == 0 -> ChessColour.BLACK
			else -> ChessColour.WHITE
		}
		else -> when {
			y%2 == 0 -> ChessColour.WHITE
			else -> ChessColour.BLACK
		}
	}
	
	operator fun get(i: Int) = array[i]
}
