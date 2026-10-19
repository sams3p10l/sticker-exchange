const grid = document.querySelector("[data-grid]");
const errorBox = document.querySelector("[data-grid-error]");

if (grid) {
    grid.addEventListener("click", (event) => {
        const target = event.target instanceof Element ? event.target : event.target.parentElement;
        const button = target && target.closest("button");
        const cell = button && button.closest("[data-sticker]");
        if (!button || !cell || !grid.contains(cell) || cell.classList.contains("pending")) {
            return;
        }
        const action = button.dataset.action;
        if (!action) {
            return;
        }
        save(cell, action);
    });
}

async function save(cell, action) {
    cell.classList.add("pending");
    if (errorBox) {
        errorBox.hidden = true;
    }
    try {
        const response = await fetch("/albums/" + cell.dataset.album + "/stickers/" + cell.dataset.sticker, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: "action=" + encodeURIComponent(action),
        });
        const type = response.headers.get("content-type") || "";
        if (!response.ok || !type.includes("application/json")) {
            throw new Error("save failed");
        }
        const body = await response.json();
        paint(cell, body);
        paintProgress(body);
    } catch (error) {
        if (errorBox) {
            errorBox.hidden = false;
            errorBox.textContent = "Nije sačuvano. Pokušaj ponovo.";
        }
    } finally {
        cell.classList.remove("pending");
    }
}

function paint(cell, body) {
    cell.dataset.status = body.status;
    const count = cell.querySelector(".count");
    if (count) {
        count.textContent = body.count == null ? "" : String(body.count);
    }
    const tap = cell.querySelector(".tap");
    if (tap) {
        tap.setAttribute("aria-label", label(cell.dataset.code, body.status, body.count));
    }
}

function paintProgress(body) {
    setText("[data-missing]", "Fali " + body.missing + " od " + body.total + ".");
    setText("[data-owned]", "Imam " + body.owned + ".");
    setText("[data-mine-wanted]", "Tražim " + body.mineWanted + ".");
    setText("[data-collectors]", "Kolekcionara: " + body.collectors + ".");
    setText("[data-offered]", "Ponuđeno: " + body.offered + ".");
    setText("[data-wanted]", "Traženo: " + body.wanted + ".");
}

function setText(selector, text) {
    const node = document.querySelector(selector);
    if (node) {
        node.textContent = text;
    }
}

function label(code, status, count) {
    if (status === "have") {
        return code + ", imam";
    }
    if (status === "duplicate") {
        return code + ", duplikat " + count;
    }
    if (status === "want") {
        return code + ", tražim";
    }
    return code + ", prazno";
}
