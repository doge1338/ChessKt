package util

import kotlinx.browser.document
import org.w3c.dom.*

fun q(query: String) = document.querySelector(query)
fun qAll(query: String) = document.querySelectorAll(query)

object HTML {
	//val content = q("#content") as HTMLDivElement
	val gameContent = q("#gameContent") as HTMLDivElement
	val chatbox = q("#chatbox") as HTMLDivElement
	val mainLabel = q("#mainLabel") as HTMLDivElement
	
	fun createChessboard(): Pair<HTMLTableElement, Array<Array<HTMLTableCellElement>>> {
		val table = (document.createElement("table") as HTMLTableElement)
			.apply { classList.add("chessboard") }
		val rows = Array(8) {
			document.createElement("tr") as HTMLTableRowElement
		}
		rows.forEachIndexed { rowIt, row ->
			row.append(*Array(8) { col ->
				val cell = document.createElement("td") as HTMLTableCellElement
				when {
					rowIt % 2 == 0 -> when {
						col % 2 == 0 -> cell.classList.add("cell-white")
						else  -> cell.classList.add("cell-black")
					}
					else -> when {
						col % 2 == 0 -> cell.classList.add("cell-black")
						else -> cell.classList.add("cell-white")
					}
				}
				cell
			})
		}
		table.append(*rows)
		
		return Pair(table, Array(8) { row -> Array(8) { col -> rows[row].children[col] as HTMLTableCellElement } })
	}
	
	fun createChatMessage(author: String, content: String): HTMLDivElement {
		val authorElement = (document.createElement("div") as HTMLDivElement)
			.apply {
				classList.add("chatElementAuthor")
				innerText = author
			}
		val contentElement = (document.createElement("div") as HTMLDivElement)
			.apply {
				classList.add("chatElementMessage")
				innerHTML = content
			}
		return (document.createElement("div") as HTMLDivElement)
			.apply {
				classList.add("chatElement")
				append(authorElement, contentElement)
			}
	}
	
//	fun materialIcon(name: String): HTMLSpanElement {
//		val el = document.createElement("span") as HTMLSpanElement
//		el.classList.add("material-icons")
//		el.innerText = name
//		return el
//	}
}