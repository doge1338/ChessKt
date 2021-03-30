import kotlinx.html.*
import util.materialIcon

val index: HTML.() -> Unit = {
	head {
		title("Chess")
		link(href = "/static/index.css", rel = "stylesheet")
		link(href = "https://fonts.gstatic.com", rel = "preconnect")
		link(href = "https://fonts.googleapis.com/css2?family=Lato:wght@400;700&display=swap", rel = "stylesheet")
		link(href = "https://fonts.googleapis.com/icon?family=Material+Icons", rel = "stylesheet")
		meta("viewport", "width=device-width, initial-scale=1.0")
		script(src = "/static/index.js") {}
	}
	body {
		div {
			id = "content"
			h1 { +"chess" }
			div("mainButton") {
				id = "newGame"
				materialIcon("add_box", "bigIcon")
				+"Create a new game"
			}
			div("mainButton") {
				id = "joinGame"
				materialIcon("public", "bigIcon")
				+"Join a game"
			}
		}
	}
}
