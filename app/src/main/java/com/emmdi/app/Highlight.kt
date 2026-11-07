package com.emmdi.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

data class HiColors(val accent: Color, val check: Color, val muted: Color, val code: Color, val codeBg: Color, val link: Color)

/** Colora il sorgente Markdown senza toccare il testo: gli offset restano identici. */
class MdHighlighter(private val c: HiColors) : VisualTransformation {
    private var lastIn: AnnotatedString? = null
    private var lastOut: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        if (text != lastIn) { lastIn = text; lastOut = highlight(text.text, c) }
        return TransformedText(lastOut!!, OffsetMapping.Identity)
    }
}

private val HEADING = Regex("""^\s{0,3}#{1,6}(\s|$)""")
private val HR = Regex("""^\s{0,3}([-*_])(\s*\1){2,}\s*$""")
private val LIST = Regex("""^((?:\s*>)*\s*)([-*+]|\d+[.)])(\s+)(\[[ xX]])?""")
private val INLINE_CODE = Regex("`[^`\n]+`")
private val BOLD = Regex("""(\*\*|__)(?=\S)(.+?)(?<=\S)\1""")
private val ITALIC = Regex("""(?<![*_\w])([*_])(?=\S)([^*_\n]+?)(?<=\S)\1(?![*_\w])""")
private val STRIKE = Regex("~~(?=\\S)(.+?)(?<=\\S)~~")
private val LINK = Regex("""!?\[([^\]\n]*)]\(([^)\n]*)\)""")

fun highlight(s: String, c: HiColors): AnnotatedString {
    val b = AnnotatedString.Builder(s)
    var pos = 0
    var inFence = false
    for (line in s.split('\n')) {
        val end = pos + line.length
        val t = line.trimStart()
        val fence = t.startsWith("```") || t.startsWith("~~~")
        when {
            fence -> { b.addStyle(SpanStyle(color = c.muted, background = c.codeBg), pos, end); inFence = !inFence }
            inFence -> b.addStyle(SpanStyle(color = c.code, background = c.codeBg), pos, end)
            HEADING.containsMatchIn(line) -> b.addStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold), pos, end)
            HR.matches(line) -> b.addStyle(SpanStyle(color = c.muted), pos, end)
            t.startsWith(">") -> b.addStyle(SpanStyle(color = c.muted, fontStyle = FontStyle.Italic), pos, end)
        }
        if (!fence && !inFence) {
            LIST.find(line)?.let { m ->
                val mk = m.groups[2]!!.range
                b.addStyle(SpanStyle(color = c.accent, fontWeight = FontWeight.Bold), pos + mk.first, pos + mk.last + 1)
                m.groups[4]?.range?.let { r ->
                    b.addStyle(SpanStyle(color = c.check, fontWeight = FontWeight.Bold), pos + r.first, pos + r.last + 1)
                }
            }
            fun style(re: Regex, st: SpanStyle) = re.findAll(line).forEach { b.addStyle(st, pos + it.range.first, pos + it.range.last + 1) }
            style(BOLD, SpanStyle(fontWeight = FontWeight.Bold))
            style(ITALIC, SpanStyle(fontStyle = FontStyle.Italic))
            style(STRIKE, SpanStyle(textDecoration = TextDecoration.LineThrough))
            LINK.findAll(line).forEach { m ->
                b.addStyle(SpanStyle(color = c.link), pos + m.range.first, pos + m.range.last + 1)
                val u = m.groups[2]!!.range
                b.addStyle(SpanStyle(color = c.muted), pos + u.first, pos + u.last + 1)
            }
            style(INLINE_CODE, SpanStyle(color = c.code, background = c.codeBg))
        }
        pos = end + 1
    }
    return b.toAnnotatedString()
}
