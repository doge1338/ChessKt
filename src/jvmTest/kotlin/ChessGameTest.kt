import chess.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal class ChessGameTest {
	private fun ChessGame.makeMove(a: Char, b: Int, c: Char, d: Int): ChessGame.GameState {
		return makeMove(ChessBoard.fromBoardPosition(a, b), ChessBoard.fromBoardPosition(c, d))
	}
	private fun fbp(a: Char, b: Int) = ChessBoard.fromBoardPosition(a, b)
	
	@org.junit.jupiter.api.Test
	fun initMoves() {
		val game = ChessGame(true)
		
		val tests = mutableMapOf<Pair<Int, Int>, List<Pair<Int, Int>>>()
		// White pawns
		tests += mapOf(
			fbp('a', 2) to listOf(fbp('a', 3), fbp('a', 4)),
			fbp('b', 2) to listOf(fbp('b', 3), fbp('b', 4)),
			fbp('c', 2) to listOf(fbp('c', 3), fbp('c', 4)),
			fbp('d', 2) to listOf(fbp('d', 3), fbp('d', 4)),
			fbp('e', 2) to listOf(fbp('e', 3), fbp('e', 4)),
			fbp('f', 2) to listOf(fbp('f', 3), fbp('f', 4)),
			fbp('g', 2) to listOf(fbp('g', 3), fbp('g', 4)),
			fbp('h', 2) to listOf(fbp('h', 3), fbp('h', 4)),
		)
		// Other white pieces
		tests += mapOf(
			fbp('a', 1) to listOf(),
			fbp('b', 1) to listOf(fbp('a', 3), fbp('c', 3)),
			fbp('c', 1) to listOf(),
			fbp('d', 1) to listOf(),
			fbp('e', 1) to listOf(),
			fbp('f', 1) to listOf(),
			fbp('g', 1) to listOf(fbp('f', 3), fbp('h', 3)),
			fbp('h', 1) to listOf(),
		)
		// Black pawns
		tests += mapOf(
			fbp('a', 7) to listOf(fbp('a', 6), fbp('a', 5)),
			fbp('b', 7) to listOf(fbp('b', 6), fbp('b', 5)),
			fbp('c', 7) to listOf(fbp('c', 6), fbp('c', 5)),
			fbp('d', 7) to listOf(fbp('d', 6), fbp('d', 5)),
			fbp('e', 7) to listOf(fbp('e', 6), fbp('e', 5)),
			fbp('f', 7) to listOf(fbp('f', 6), fbp('f', 5)),
			fbp('g', 7) to listOf(fbp('g', 6), fbp('g', 5)),
			fbp('h', 7) to listOf(fbp('h', 6), fbp('h', 5)),
		)
		// Other black pieces
		tests += mapOf(
			fbp('a', 8) to listOf(),
			fbp('b', 8) to listOf(fbp('a', 6), fbp('c', 6)),
			fbp('c', 8) to listOf(),
			fbp('d', 8) to listOf(),
			fbp('e', 8) to listOf(),
			fbp('f', 8) to listOf(),
			fbp('g', 8) to listOf(fbp('f', 6), fbp('h', 6)),
			fbp('h', 8) to listOf(),
		)
		
		tests.forEach { (t, u) ->
			val piece = game.board[t.x][t.y]!!
			val moves = piece.availableMoves()
			assert(moves.containsAll(u) && u.containsAll(moves)) {
				"FAIL: $piece; Moves: ${moves.joinToString { ChessBoard.stringifyPosition(it.x, it.y) }}"
			}
			println("SUCCESS: $piece; Moves: ${moves.joinToString { ChessBoard.stringifyPosition(it.x, it.y) }}")
		}
	}
	
	@org.junit.jupiter.api.Test
	fun noPawnMoves() {
		val game = ChessGame(true)
		// Remove pawns
		for (x in game.board.array.indices) for (y in game.board.array[x].indices) {
			if (game.board.array[x][y] is ChessPiece.Pawn) {
				game.board.array[x][y] = null
			}
		}
		val tests = mutableMapOf<Pair<Int, Int>, List<Pair<Int, Int>>>()
		// White pieces
		tests += mapOf(
			// Rook
			fbp('a', 1) to listOf(fbp('a', 2), fbp('a', 3), fbp('a', 4), fbp('a', 5), fbp('a', 6), fbp('a', 7), fbp('a', 8)),
			// Knight
			fbp('b', 1) to listOf(fbp('a', 3), fbp('c', 3), fbp('d', 2)),
			// Bishop
			fbp('c', 1) to listOf(
				fbp('b', 2), fbp('a', 3),
				fbp('d', 2), fbp('e', 3), fbp('f', 4), fbp('g', 5), fbp('h', 6)
			),
			// Queen
			fbp('d', 1) to listOf(
				fbp('c', 2), fbp('b', 3), fbp('a', 4),
				fbp('d', 2), fbp('d', 3), fbp('d', 4), fbp('d', 5), fbp('d', 6), fbp('d', 7), fbp('d', 8),
				fbp('e', 2), fbp('f', 3), fbp('g', 4), fbp('h', 5)
			),
			// King
			fbp('e', 1) to listOf(fbp('d', 2), fbp('e', 2), fbp('f', 2))
		)
		
		tests.forEach { (t, u) ->
			val piece = game.board[t.x][t.y]!!
			val moves = piece.availableMoves()
			assert(moves.containsAll(u) && u.containsAll(moves)) {
				"FAIL: $piece; Moves: ${moves.joinToString { ChessBoard.stringifyPosition(it.x, it.y) }}; Expected: ${u.joinToString { ChessBoard.stringifyPosition(it.x, it.y) }}"
			}
			println("SUCCESS: $piece; Moves: ${moves.joinToString { ChessBoard.stringifyPosition(it.x, it.y) }}")
		}
	}
	
	@org.junit.jupiter.api.Test
	fun simpleCheckmate() {
		val game = ChessGame(true)

		val whiteMoves = listOf(
			Pair(Pair('f', 2), Pair('f', 3)),
			Pair(Pair('g', 2), Pair('g', 4)),
		)
		val blackMoves = listOf(
			Pair(Pair('e', 7), Pair('e', 6)),
			Pair(Pair('d', 8), Pair('h', 4)),
		)

		for (i in 0..3) {
			println(game.board.visualize())
			println("--------")
			if (i%2 == 0) {
				val move = whiteMoves[i/2]
				val sq = game.board.pieceAt(move.x.x, move.x.y)
				val result = game.makeMove(move.x.x, move.x.y, move.y.x, move.y.y)
				println("W ${sq!!.name} ${move.x.x}${move.x.y} -> ${ChessBoard.stringifyPosition(sq.pos.x, sq.pos.y)}, ${result.name}")
				if (result == ChessGame.GameState.STALEMATE) {
					//println(game.board.array.flatten().filter { piece -> piece?.colour == ChessColour.BLACK })
					println(game.board.visualize())
				}
				assert(result == ChessGame.GameState.CONTINUE)
			} else {
				val move = blackMoves[i/2]
				val sq = game.board.pieceAt(move.x.x, move.x.y)
				val result = game.makeMove(move.x.x, move.x.y, move.y.x, move.y.y)
				println("B ${sq!!.name} ${move.x.x}${move.x.y} -> ${ChessBoard.stringifyPosition(sq.pos.x, sq.pos.y)}, ${result.name}")
				if (result == ChessGame.GameState.STALEMATE) {
					println(game.board.array.flatten().filter { piece -> piece?.colour == ChessColour.BLACK })
				}
				assert(result == ChessGame.GameState.CONTINUE || result == ChessGame.GameState.BLACK_WIN)
			}
		}
		println(game.board.visualize())
		println("--------")
	}
	
	
	@ExperimentalTime
	@org.junit.jupiter.api.Test
	fun randomGame() {
		val game = ChessGame(true)
		
		var i = 0
		while (i++ < 1000) {
			val cur = game.currentPlayer
			// Pick a random move
			val move: Pair<Pair<Int, Int>, Pair<Int, Int>>
			val timeRng = measureTime {
				move = game.board.array
					.flatten()
					.filterNotNull()
					.filter { it.colour == cur }
					.flatMap { piece -> piece.availableMoves().filter { !game.board.isMoveForbidden(piece, it.x, it.y) }.map { Pair(piece.pos, it) } }
					.random()
			}
			println("Creating a random move took $timeRng")
			val result: ChessGame.GameState
			val timeResult = measureTime {
				result = game.makeMove(move.x, move.y)
			}
			println("Making a move took $timeResult")
			assert(result != ChessGame.GameState.ILLEGAL_MOVE)
			if (result != ChessGame.GameState.CONTINUE) {
				println("End: $result")
				break
			}
			println(game.board.visualize())
			println("-------- $i")
		}
	}
}