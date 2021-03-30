const $ = q => document.querySelector(q)
EventTarget.prototype.on = EventTarget.prototype.addEventListener

window.on("DOMContentLoaded", () => {
    $("#newGame").on("click", () => location.assign("/new-game"))
    $("#joinGame").on("click", () => {
        const code = prompt("Paste the link or game code")
        if (code.includes("/game/")) {
            if (code.startsWith("http://") || code.startsWith("https://")) {
                return location.assign(code)
            } else {
                return location.assign("http://"+code)
            }
        }
        if (code.match(/^[0-9a-f]{4}$/i)) {
            return location.assign(`http://${location.host}/game/${code.toUpperCase()}`)
        }
        alert(`'${code}' is not a valid link or game code.`)
    })
})