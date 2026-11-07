package com.emmdi.app

/** Modifiche al testo Markdown per righe: le righe arrivano dal parser (indice 0-based, separatore '\n'). */
object MdOps {
    // Voce di elenco con casella, anche dentro una citazione: "> - [ ] testo", "1. [x] testo".
    private val TASK = Regex("""^((?:\s*>)*\s*(?:[-*+]|\d+[.)])\s+\[)([ xX])(].*)$""")

    fun toggleTask(text: String, line: Int): String {
        val lines = text.split('\n').toMutableList()
        if (line !in lines.indices) return text
        val m = TASK.find(lines[line]) ?: return text
        val (pre, mark, post) = m.destructured
        lines[line] = pre + (if (mark == " ") "x" else " ") + post
        return lines.joinToString("\n")
    }

    fun slice(text: String, first: Int, last: Int): String {
        val lines = text.split('\n')
        return lines.subList(first.coerceIn(0, lines.size), (last + 1).coerceIn(0, lines.size)).joinToString("\n")
    }

    /** Sostituisce le righe first..last; testo vuoto = elimina il blocco (e una riga vuota di troppo). */
    fun replaceLines(text: String, first: Int, last: Int, replacement: String): String {
        val lines = text.split('\n')
        val before = lines.take(first)
        var after = lines.drop(last + 1)
        val mid = if (replacement.isEmpty()) {
            if (after.firstOrNull()?.isBlank() == true && (before.isEmpty() || before.last().isBlank())) after = after.drop(1)
            emptyList()
        } else replacement.trimEnd('\n').split('\n')
        return (before + mid + after).joinToString("\n")
    }

    fun append(text: String, block: String): String {
        val base = text.trimEnd('\n', ' ')
        return (if (base.isEmpty()) "" else "$base\n\n") + block.trimEnd('\n') + "\n"
    }
}
