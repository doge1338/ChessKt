package chess

import kotlin.math.abs

enum class ChessColour {
	BLACK,
	WHITE;
	operator fun not() = when (this) {
		BLACK -> WHITE
		WHITE -> BLACK
	}
}

sealed class ChessPiece(
	val colour: ChessColour,
	val board: ChessBoard,
	var pos: Pair<Int, Int>
) {
	abstract fun availableMoves(): List<Pair<Int, Int>>
	abstract fun canHit(targetPos: Pair<Int, Int>): Boolean
	abstract val name: String
	abstract val symbol: String
	
	internal var hasMoved = false
	
	override fun toString(): String {
		val col = when (colour) {
			ChessColour.WHITE -> "W"
			ChessColour.BLACK -> "B"
		}
		return "($col $name ${ChessBoard.stringifyPosition(pos.x, pos.y)})"
	}
	
	class King(
		colour: ChessColour,
		board: ChessBoard,
		pos: Pair<Int, Int>
	) : ChessPiece(colour, board, pos) {
		override val name = "King"
		override val symbol = "K"
		
		override fun availableMoves(): List<Pair<Int, Int>> {
			val moves = arrayListOf(
				Pair(pos.x-1, pos.y+1),
				Pair(pos.x,   pos.y+1),
				Pair(pos.x+1, pos.y+1),
				Pair(pos.x+1, pos.y),
				Pair(pos.x+1, pos.y-1),
				Pair(pos.x,   pos.y-1),
				Pair(pos.x-1, pos.y-1),
				Pair(pos.x-1, pos.y),
			).filter { candidate ->
				candidate.x in 0..7 &&
				candidate.y in 0..7 &&
				!board.hasColouredPieceAt(candidate.x, candidate.y, colour)
			}.toMutableList()
			
			// Castling
			if (!hasMoved) {
				val rooks = board.rooks(colour).filter { !it.hasMoved }
				if (rooks.isEmpty()) {
					return moves
				}
				for (rook in rooks) {
					if (canCastle(rook)) {
						moves += when (rook.pos.x) {
							0 -> Pair(rook.pos.x+1, rook.pos.y)
							7 -> Pair(rook.pos.x-1, rook.pos.y)
							else -> throw Error("unreachable")
						}
					}
				}
			}
			
			return moves
		}
		
		private fun canCastle(rook: Rook): Boolean {
			// Castling rules (https://en.wikipedia.org/wiki/Castling)
			// 1. The castling must be kingside or queenside.
			// 2. Neither the king nor the chosen rook has previously moved.
			// 3. There are no pieces between the king and the chosen rook.
			// 4. The king is not currently in check.
			// 5. The king does not pass through a square that is attacked by an enemy piece.
			// 6. The king does not end up in check. (True of any legal move.)
			
			if (this.hasMoved || rook.hasMoved || board.isEndangered(pos, colour)) {
				return false
			}
			
			return when (rook.pos.x) {
				// Castling long
				0 -> !(
					board.hasPieceAt(pos.x-1, pos.y) ||
					board.hasPieceAt(pos.x-2, pos.y) ||
					board.hasPieceAt(pos.x-3, pos.y) ||
					board.isEndangered(Pair(pos.x-1, pos.y), colour) ||
					board.isEndangered(Pair(pos.x-2, pos.y), colour) ||
					board.isEndangered(Pair(pos.x-3, pos.y), colour)
				)
				// Castling short
				7 -> !(
					board.hasPieceAt(pos.x+1, pos.y) ||
					board.hasPieceAt(pos.x+2, pos.y) ||
					board.isEndangered(Pair(pos.x+1, pos.y), colour) ||
					board.isEndangered(Pair(pos.x+2, pos.y), colour)
				)
				else -> false
			}
		}
		
		override fun canHit(targetPos: Pair<Int, Int>): Boolean {
			return abs(pos.x-targetPos.x) <= 1 && abs(pos.y-targetPos.y) <= 1
		}
	}
	
	class Queen(
		colour: ChessColour,
		board: ChessBoard,
		pos: Pair<Int, Int>
	) : ChessPiece(colour, board, pos) {
		override val name = "Queen"
		override val symbol = "Q"
		
		override fun availableMoves(): List<Pair<Int, Int>> {
			val moves = ArrayList<Pair<Int, Int>>()
			// Left, right, bottom, top
			var lx = pos.x; var rx = pos.x; var by = pos.y; var ty = pos.y
			while (lx > 0) {
				when (board.pieceAt(--lx, pos.y)?.colour) {
					colour -> break
					!colour -> { moves += Pair(lx, pos.y); break}
					else -> { moves += Pair(lx, pos.y) }
				}
			}
			while (rx < 7) {
				when (board.pieceAt(++rx, pos.y)?.colour) {
					colour -> break
					!colour -> { moves += Pair(rx, pos.y); break}
					else -> { moves += Pair(rx, pos.y) }
				}
			}
			while (by > 0) {
				when (board.pieceAt(pos.x, --by)?.colour) {
					colour -> break
					!colour -> { moves += Pair(pos.x, by); break}
					else -> { moves += Pair(pos.x, by) }
				}
			}
			while (ty < 7) {
				when (board.pieceAt(pos.x, ++ty)?.colour) {
					colour -> break
					!colour -> { moves += Pair(pos.x, ty); break}
					else -> { moves += Pair(pos.x, ty) }
				}
			}
			// Top left
			var tlx = pos.x; var tly = pos.y
			while (tlx != 0 && tly != 7) {
				when (board.pieceAt(--tlx, ++tly)?.colour) {
					// Opponent - can attack, and can't reach further
					!colour -> { moves += Pair(tlx, tly); break }
					// Friendly piece - can't reach further
					colour -> break
					// No piece - continue
					else -> { moves += Pair(tlx, tly) }
				}
			}
			// Top right
			var trx = pos.x; var `try` = pos.y
			while (trx != 7 && `try` != 7) {
				when (board.pieceAt(++trx, ++`try`)?.colour) {
					!colour -> { moves += Pair(trx, `try`); break }
					colour -> break
					else -> { moves += Pair(trx, `try`) }
				}
			}
			// Bottom left
			var blx = pos.x; var bly = pos.y
			while (blx != 0 && bly != 0) {
				when (board.pieceAt(--blx, --bly)?.colour) {
					!colour -> { moves += Pair(blx, bly); break }
					colour -> break
					else -> { moves += Pair(blx, bly) }
				}
			}
			// Bottom right
			var brx = pos.x; var bry = pos.y
			while (brx != 7 && bry != 0) {
				when (board.pieceAt(++brx, --bry)?.colour) {
					!colour -> { moves += Pair(brx, bry); break }
					colour -> break
					else -> { moves += Pair(brx, bry) }
				}
			}
			
			return moves
		}
		
		override fun canHit(targetPos: Pair<Int, Int>): Boolean {
			if (pos == targetPos) {
				return false
			}
			
			if (pos.x == targetPos.x) {
				var y = pos.y
				val delta = if (targetPos.y < pos.y) -1 else 1
				while (true) {
					y += delta
					if (targetPos.y == y) {
						return true
					}
					if (board.hasPieceAt(pos.x, y)) {
						return false
					}
					assert(y in 0..7) { "1, p: $this; tp: $targetPos; y: $y" }
				}
			} else if (pos.y == targetPos.y) {
				var x = pos.x
				val delta = if (targetPos.x < pos.x) -1 else 1
				while (true) {
					x += delta
					if (targetPos.x == x) {
						return true
					}
					if (board.hasPieceAt(x, pos.y)) {
						return false
					}
					assert(x in 0..7) { "2, p: $this; tp: $targetPos; x: $x" }
				}
			} else if (board.squareColour(pos.x, pos.y) == board.squareColour(targetPos.x, targetPos.y)) {
				val mulX = when {
					targetPos.x < pos.x -> -1
					else -> 1
				}
				val mulY = when {
					targetPos.y < pos.y -> -1
					else -> 1
				}
				var cx = pos.x; var cy = pos.y
				while (cx != targetPos.x && cy != targetPos.y && cx in 0..7 && cy in 0..7) {
					cx += mulX; cy += mulY
					if (board.hasPieceAt(cx, cy)) {
						break
					}
				}
				return cx == targetPos.x && cy == targetPos.y
			}
			
			return false
		}
	}
	
	class Rook(
		colour: ChessColour,
		board: ChessBoard,
		pos: Pair<Int, Int>
	) : ChessPiece(colour, board, pos) {
		override val name = "Rook"
		override val symbol = "R"
		
		override fun availableMoves(): List<Pair<Int, Int>> {
			val moves = ArrayList<Pair<Int, Int>>()
			// Left, right, bottom, top
			var lx = pos.x; var rx = pos.x; var by = pos.y; var ty = pos.y
			while (lx > 0) {
				when (board.pieceAt(--lx, pos.y)?.colour) {
					colour -> break
					!colour -> { moves += Pair(lx, pos.y); break}
					else -> { moves += Pair(lx, pos.y) }
				}
			}
			while (rx < 7) {
				when (board.pieceAt(++rx, pos.y)?.colour) {
					colour -> break
					!colour -> { moves += Pair(rx, pos.y); break}
					else -> { moves += Pair(rx, pos.y) }
				}
			}
			while (by > 0) {
				when (board.pieceAt(pos.x, --by)?.colour) {
					colour -> break
					!colour -> { moves += Pair(pos.x, by); break}
					else -> { moves += Pair(pos.x, by) }
				}
			}
			while (ty < 7) {
				when (board.pieceAt(pos.x, ++ty)?.colour) {
					colour -> break
					!colour -> { moves += Pair(pos.x, ty); break}
					else -> { moves += Pair(pos.x, ty) }
				}
			}
			
			return moves
		}
		
		override fun canHit(targetPos: Pair<Int, Int>): Boolean {
			if ((pos.x != targetPos.x && pos.y != targetPos.y) || pos == targetPos) {
				return false
			}
			if (pos.x == targetPos.x) {
				var y = pos.y
				val delta = if (targetPos.y < pos.y) -1 else 1
				while (true) {
					y += delta
					if (targetPos.y == y) {
						return true
					}
					if (board.hasPieceAt(pos.x, y)) {
						return false
					}
					assert(y in 0..7) { "3, p: $this; tp: $targetPos; y: $y" }
				}
			} else {
				var x = pos.x
				val delta = if (targetPos.x < pos.x) -1 else 1
				while (true) {
					x += delta
					if (targetPos.x == x) {
						return true
					}
					if (board.hasPieceAt(x, pos.y)) {
						return false
					}
					assert(x in 0..7) { "4, p: $this; tp: $targetPos; x: $x" }
				}
			}
		}
	}
	
	class Bishop(
		colour: ChessColour,
		board: ChessBoard,
		pos: Pair<Int, Int>
	) : ChessPiece(colour, board, pos) {
		override val name = "Bishop"
		override val symbol = "B"
		
		private val squareColour = board.squareColour(pos.x, pos.y)
		
		override fun availableMoves(): List<Pair<Int, Int>> {
			val moves = ArrayList<Pair<Int, Int>>()
			// Top left
			var tlx = pos.x; var tly = pos.y
			while (tlx != 0 && tly != 7) {
				when (board.pieceAt(--tlx, ++tly)?.colour) {
					// Opponent - can attack, and can't reach further
					!colour -> { moves += Pair(tlx, tly); break }
					// Friendly piece - can't reach further
					colour -> break
					// No piece - continue
					else -> { moves += Pair(tlx, tly) }
				}
			}
			// Top right
			var trx = pos.x; var `try` = pos.y
			while (trx != 7 && `try` != 7) {
				when (board.pieceAt(++trx, ++`try`)?.colour) {
					!colour -> { moves += Pair(trx, `try`); break }
					colour -> break
					else -> { moves += Pair(trx, `try`) }
				}
			}
			// Bottom left
			var blx = pos.x; var bly = pos.y
			while (blx != 0 && bly != 0) {
				when (board.pieceAt(--blx, --bly)?.colour) {
					!colour -> { moves += Pair(blx, bly); break }
					colour -> break
					else -> { moves += Pair(blx, bly) }
				}
			}
			// Bottom right
			var brx = pos.x; var bry = pos.y
			while (brx != 7 && bry != 0) {
				when (board.pieceAt(++brx, --bry)?.colour) {
					!colour -> { moves += Pair(brx, bry); break }
					colour -> break
					else -> { moves += Pair(brx, bry) }
				}
			}
			return moves
		}
		
		override fun canHit(targetPos: Pair<Int, Int>): Boolean {
			if (pos == targetPos || squareColour != board.squareColour(targetPos.x, targetPos.y)) {
				return false
			}
			val mulX = when {
				targetPos.x < pos.x -> -1
				else -> 1
			}
			val mulY = when {
				targetPos.y < pos.y -> -1
				else -> 1
			}
			var cx = pos.x; var cy = pos.y
			while (cx != targetPos.x && cy != targetPos.y && cx in 0..7 && cy in 0..7) {
				cx += mulX; cy += mulY
				if (board.hasPieceAt(cx, cy)) {
					break
				}
			}
			return cx == targetPos.x && cy == targetPos.y
		}
	}
	
	class Knight(
		colour: ChessColour,
		board: ChessBoard,
		pos: Pair<Int, Int>
	) : ChessPiece(colour, board, pos) {
		override val name = "Knight"
		override val symbol = "H"
		
		override fun availableMoves(): List<Pair<Int, Int>> {
			return arrayListOf(
				Pair(pos.x - 1, pos.y - 2),
				Pair(pos.x + 1, pos.y - 2),
				Pair(pos.x - 1, pos.y + 2),
				Pair(pos.x + 1, pos.y + 2),
				Pair(pos.x - 2, pos.y + 1),
				Pair(pos.x - 2, pos.y - 1),
				Pair(pos.x + 2, pos.y + 1),
				Pair(pos.x + 2, pos.y - 1),
			).filter { candidatePosition ->
				return@filter candidatePosition.x in 0..7 &&
						candidatePosition.y in 0..7 &&
						!board.hasColouredPieceAt(candidatePosition.x, candidatePosition.y, colour)
			}
		}
		
		override fun canHit(targetPos: Pair<Int, Int>): Boolean {
			return (abs(targetPos.x-pos.x) == 2 && abs(targetPos.y-pos.y) == 1) ||
				   (abs(targetPos.x-pos.x) == 1 && abs(targetPos.y-pos.y) == 2)
		}
	}
	
	class Pawn(
		colour: ChessColour,
		board: ChessBoard,
		pos: Pair<Int, Int>
	) : ChessPiece(colour, board, pos) {
		override val name = "Pawn"
		override val symbol = "P"
		
		override fun availableMoves(): List<Pair<Int, Int>> {
			val moves = ArrayList<Pair<Int, Int>>(2)
			val mul = when (colour) {
				ChessColour.WHITE -> 1
				ChessColour.BLACK -> -1
			}
			// Forward
			if (!board.hasPieceAt(pos.x, pos.y+mul) && pos.y+mul in 0..7) {
				moves += Pair(pos.x, pos.y+mul)
				// Double forward
				val isAtStart = ((mul == 1 && pos.y == 1) || (mul == -1 && pos.y == 6))
				if (isAtStart && !board.hasPieceAt(pos.x, pos.y+(mul*2))) {
					moves += Pair(pos.x, pos.y+(2*mul))
				}
			}
			// Attack
			if (board.hasColouredPieceAt(pos.x-1, pos.y+mul, !colour))
				moves += Pair(pos.x-1, pos.y+mul)
			if (board.hasColouredPieceAt(pos.x+1, pos.y+mul, !colour))
				moves += Pair(pos.x+1, pos.y+mul)
			
			return moves
		}
		
		override fun canHit(targetPos: Pair<Int, Int>): Boolean {
			return pos.y + when (colour) {
				ChessColour.WHITE -> 1
				ChessColour.BLACK -> -1
			} == targetPos.y && abs(pos.x-targetPos.x) == 1
		}
	}
}
