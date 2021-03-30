package chess

class ChessGame(val debug: Boolean = false) {
	val board = ChessBoard()
	private val whiteKing = board.pieceAt('e', 1) as ChessPiece.King
	private val blackKing = board.pieceAt('e', 8) as ChessPiece.King
	
	var currentPlayer = ChessColour.WHITE
		private set
	val currentKing get() = when (currentPlayer) {
		ChessColour.WHITE -> whiteKing
		ChessColour.BLACK -> blackKing
	}
	
	enum class GameState {
		ILLEGAL_MOVE,
		CONTINUE,
		BLACK_WIN,
		WHITE_WIN,
		STALEMATE;
	}
	fun makeMove(from: Pair<Int, Int>, to: Pair<Int, Int>): GameState {
		if (from.x !in 0..7 || from.y !in 0..7 || to.x !in 0..7 || to.y !in 0..7) {
			if (debug) {
				println("Illegal move: (${from.x}, ${from.y}) -> (${to.x}, ${to.y}) not on chessboard")
			}
			return GameState.ILLEGAL_MOVE
		}

		val sq = board[from.x][from.y] ?: return GameState.ILLEGAL_MOVE
		if (sq.colour != currentPlayer) {
			if (debug) {
				println("Illegal move: $currentPlayer player trying to move ${sq.colour} pieces")
			}
			return GameState.ILLEGAL_MOVE
		}
		val legalMoves = sq.availableMoves()
		if (to !in legalMoves) {
			if (debug) {
				println("Illegal move: move (${
					ChessBoard.stringifyPosition(
						to.x,
						to.y
					)
				}) not in available moves for $sq: ${legalMoves.joinToString {
					ChessBoard.stringifyPosition(
						it.x,
						it.y
					)
				}}")
			}
			return GameState.ILLEGAL_MOVE
		}
		if (board.isCheck(currentPlayer) && board.isMoveForbidden(sq, to.x, to.y)) {
			if (debug) {
				println("Illegal move: move impossible during check")
			}
			return GameState.ILLEGAL_MOVE
		}
		board.movePiece(from.x, from.y, to.x, to.y)
		currentPlayer = !currentPlayer
		
		if (board.isMate(currentPlayer)) {
			if (debug) {
				println("$currentPlayer is mated")
			}
			return when {
				board.isCheck(currentPlayer) -> when(sq.colour) {
					ChessColour.WHITE -> GameState.WHITE_WIN
					ChessColour.BLACK -> GameState.BLACK_WIN
				}
				else -> GameState.STALEMATE
			}
		}
		return GameState.CONTINUE
	}
}