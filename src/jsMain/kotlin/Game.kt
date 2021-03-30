import chess.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.*
import org.w3c.dom.events.KeyboardEvent
import util.*
import kotlin.time.seconds

class Game(
	gameId: String,
	isInvite: Boolean,
) {
	var game: ChessGame? = null
		private set
	var pieceColour: ChessColour? = null
		private set
	var isYourMove: Boolean = false
		private set
	var hasGameStarted: Boolean = false
		private set
	
	private val cellMoves = HashMap<Int, Pair<ChessPiece, Pair<Int, Int>>>(8)
	private var cellMovesCounter = 1
	
	fun cleanupSelected() {
		qAll(".selected-movable-to")
			.let { els -> Array(els.length) { els[it] as HTMLElement } }
			.forEach {
				it.classList.remove("selected-movable-to")
				val actionId = it.getAttribute("x-chess-action")!!.toInt()
				cellMoves.remove(actionId)
				it.removeAttribute("x-chess-action")
			}
	}
	
	fun setCell(cell: HTMLTableCellElement, piece: ChessPiece?) {
		cell.classList.remove("piece", "friend", "opponent",
			"wp", "wr", "wn", "wb", "wk", "wq",
			"bp", "br", "bn", "bb", "bk", "bq")
		if (piece != null) {
			cell.classList.add(piece.cssClass(), "piece", if (piece.colour == pieceColour) "friend" else "opponent")
		}
	}
	
	fun reSetBoard() {
		boardCells.forEachIndexed { posY, row ->
			row.forEachIndexed { posX, cell ->
				setCell(cell, game!!.board.pieceAt(posX, posY))
			}
		}
	}
	
	fun initChessGame() {
		conn.once(Message.StartGame.OPCODE) {
			hasGameStarted = true
			mainLabel("It's ${!pieceColour!!}'s move")
			appendChatMessage("Server", "The game has started")
		}
		conn.addHandler(Message.YourMove.OPCODE) {
			isYourMove = true
			val isCheck = game!!.board.isCheck(pieceColour!!)
			if (isCheck) {
				mainLabel("You are being checked (It's YOUR move)")
			} else {
				mainLabel("It's YOUR move")
			}
		}
		conn.addHandler(Message.BasicMove.OPCODE) { it as Message.BasicMove
			q(".last-moved")?.classList?.remove("last-moved")
			val result = game!!.makeMove(Pair(it.x, it.y), Pair(it.x1, it.y1))
			if (result == ChessGame.GameState.ILLEGAL_MOVE) {
				window.alert("Server error")
			}
			boardCells[it.y1][it.x1].classList.add("last-moved")
			when (result) {
				ChessGame.GameState.ILLEGAL_MOVE -> window.alert("Server error")
				ChessGame.GameState.CONTINUE -> {}
				ChessGame.GameState.WHITE_WIN -> {
					mainLabel("White won")
					appendChatMessage("Server", "The game has ended")
					//window.alert("White won")
					hasGameStarted = false
				}
				ChessGame.GameState.BLACK_WIN -> {
					mainLabel("Black won")
					appendChatMessage("Server", "The game has ended")
					//window.alert("Black won")
					hasGameStarted = false
				}
				ChessGame.GameState.STALEMATE -> {
					mainLabel("The match ended in a draw")
					appendChatMessage("Server", "The game has ended")
					//window.alert("Draw")
					hasGameStarted = false
				}
			}
			reSetBoard()
		}
		
		game = ChessGame(true)
		println(game!!.board.visualize())
		initHTMLChessboard()
		var selectedPiece: HTMLTableCellElement? = null
		boardCells.forEachIndexed { posY, row ->
			row.forEachIndexed { posX, cell ->
				setCell(cell, game!!.board.pieceAt(posX, posY))
				val highLightedCells = ArrayList<HTMLTableCellElement>(8)
				val selectedHighlightedCells = ArrayList<HTMLTableCellElement>(8)
				cell.on("mouseenter") {
					if (!hasGameStarted) return@on
					
					val p = game!!.board.pieceAt(posX, posY)
					if (p != null) {
						val moves = possibleMoves(p)
						moves.forEach { pos -> highLightedCells.add(boardCells[pos.y][pos.x].also {
							it.classList.add("movable-to")
						}) }
					}
				}
				cell.on("mouseleave") {
					if (!hasGameStarted) return@on
					
					if (highLightedCells.size != 0) {
						highLightedCells.forEach { it.classList.remove("movable-to") }
						highLightedCells.clear()
					}
				}
				cell.on("click") {
					if (!hasGameStarted || !isYourMove) return@on
					
					val p = game!!.board.pieceAt(posX, posY)
					if (cell.hasAttribute("x-chess-action")) {
						val action = cellMoves[cell.getAttribute("x-chess-action")!!.toInt()]!!
						cleanupSelected()
						val pieceToMove = action.x
						val moveTo = action.y
						val move = Message.BasicMove(pieceToMove.pos.x, pieceToMove.pos.y, moveTo.x, moveTo.y)
						
						boardCells[pieceToMove.pos.y][pieceToMove.pos.x].classList.remove("piece-selected")
						
						val state = game!!.makeMove(pieceToMove.pos, moveTo)
						println(game!!.board.visualize())
						when (state) {
							ChessGame.GameState.WHITE_WIN -> {
								mainLabel("White won")
								appendChatMessage("Server", "The game has ended")
								//window.alert("White won")
								hasGameStarted = false
							}
							ChessGame.GameState.BLACK_WIN -> {
								mainLabel("Black won")
								appendChatMessage("Server", "The game has ended")
								//window.alert("Black won")
								hasGameStarted = false
							}
							ChessGame.GameState.STALEMATE -> {
								mainLabel("The match ended in a draw.")
								appendChatMessage("Server", "The game has ended")
								//window.alert("Draw")
								hasGameStarted = false
							}
							ChessGame.GameState.ILLEGAL_MOVE -> {
								window.alert("Illegal move")
								return@on
							}
							ChessGame.GameState.CONTINUE -> {
								mainLabel("It's ${!pieceColour!!}'s move")
							}
						}
						q(".last-moved")?.classList?.remove("last-moved")
						boardCells[move.y1][move.x1].classList.add("last-moved")
						conn.send(move)
						isYourMove = false
						reSetBoard()
					} else if (p != null && p.colour == pieceColour) {
						val moves = possibleMoves(p)
						cleanupSelected()
						moves.forEach { pos -> selectedHighlightedCells.add(boardCells[pos.y][pos.x].also {
							it.classList.remove("movable-to")
							it.classList.add("selected-movable-to")
							cellMoves[cellMovesCounter] = Pair(p, pos)
							it.setAttribute("x-chess-action", (cellMovesCounter++).toString())
						}) }
						highLightedCells.clear()
						
						selectedPiece?.classList?.remove("piece-selected")
						cell.classList.add("piece-selected")
						selectedPiece = cell
					}
				}
			}
		}
	}
	
	fun possibleMoves(piece: ChessPiece): List<Pair<Int, Int>> {
		val moves = piece.availableMoves()
		return moves.filter { !game!!.board.isMoveForbidden(piece, it.x, it.y) }
	}
	
	val conn = Connection(gameId)
	
	// Add basic event handlers
	init {
		conn.addHandler(Message.Ready.OPCODE) {
			mainLabel("Waiting for another player...")
		}
		conn.addHandler(Message.Chat.OPCODE) { it as Message.Chat
			appendChatMessage(it.author, it.msg)
		}
		conn.once(Message.GuestJoined.OPCODE) {
			appendChatMessage("Server", "Guest has joined")
		}
		conn.once(Message.PieceColour.OPCODE) { it as Message.PieceColour
			if (it.isWhite) {
				pieceColour = ChessColour.WHITE
				appendChatMessage("Server", "You are playing using <b>white</b> pieces")
			} else {
				pieceColour = ChessColour.BLACK
				appendChatMessage("Server", "You are playing using <b>black</b> pieces")
			}
			appendChatMessage("Server", "The game is starting in 10 seconds")
			GlobalScope.launch {
				for (i in 10 downTo 1) {
					if (hasGameStarted) {
						break
					}
					mainLabel("Starting in $i...")
					delay(1.seconds)
				}
			}
			initChessGame()
		}
		conn.once(Message.Exit.OPCODE) {
			conn.close()
			appendChatMessage("Game", "Disconnected")
			conn
		}
	}
	
	val table: HTMLTableElement
	val boardCells: Array<Array<HTMLTableCellElement>>
	
	fun initHTMLChessboard() {
		// Reverse the table rows and cells if playing black
		if (pieceColour == ChessColour.BLACK) {
			table.children.toArray().forEach { row ->
				table.prepend(row)
				row.children.toArray().forEach { cell ->
					row.prepend(cell)
				}
			}
		}
	}
	
	fun appendChatMessage(author: String, content: String) {
		val el = HTML.createChatMessage(author, content)
		HTML.chatbox.append(el)
		el.scrollIntoView()
	}
	fun mainLabel(s: String) {
		HTML.mainLabel.innerText = s
	}
	
	// Setup HTML and events
	init {
		if (!isInvite) {
			val msg = "Share the game using this link: " +
					"<a href=\"https://${window.location.host}/game/$gameId\">" +
					"https://${window.location.host}/game/$gameId" +
					"</a>"
			appendChatMessage("Game", msg)
		}
		
		val (boardTable, boardCells) = HTML.createChessboard()
		HTML.gameContent.prepend(boardTable)
		this.boardCells = boardCells.reversedArray()
		table = boardTable
		
		val chatInput = document.querySelector("#chatInput input") as HTMLInputElement
		
		val sendMessage = {
			if (conn.ready == true && chatInput.value.isNotBlank()) {
				conn.send(Message.Chat("", chatInput.value))
				chatInput.value = ""
			}
		}
		
		document.querySelector("#chatInput .material-icons")!!.on("click") { sendMessage() }
		chatInput.on("keypress") { it as KeyboardEvent
			if (it.key == "Enter") {
				sendMessage()
			}
		}
		window.on("keypress") { it as KeyboardEvent
			if (it.key == "Enter") {
				chatInput.focus()
			}
		}
	}
}