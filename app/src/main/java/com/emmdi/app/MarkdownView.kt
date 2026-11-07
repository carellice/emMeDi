package com.emmdi.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.delay
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

private val parser: Parser = Parser.builder()
    .extensions(listOf(TablesExtension.create(), StrikethroughExtension.create(), TaskListItemsExtension.create()))
    .includeSourceSpans(IncludeSourceSpans.BLOCKS)
    .build()

private fun Node.kids(): List<Node> = generateSequence(firstChild) { it.next }.toList()
private fun Node.lines(): IntRange? = sourceSpans.takeIf { it.isNotEmpty() }?.let { it.first().lineIndex..it.last().lineIndex }

/** In modalità modifica i tocchi sul blocco aprono l'editor, quindi le righe delle to-do non si spuntano toccando il testo. */
private val LocalEditing = compositionLocalOf { false }
private val LocalToggle = compositionLocalOf<(Int) -> Unit> { {} }

private data class Templ(val label: String, val icon: ImageVector, val md: String)
private val TEMPLATES = listOf(
    Templ("To-do", Icons.Outlined.CheckBox, "- [ ] "),
    Templ("Testo", Icons.Outlined.Notes, ""),
    Templ("Titolo", Icons.Outlined.Title, "## "),
    Templ("Elenco", Icons.AutoMirrored.Outlined.FormatListBulleted, "- "),
    Templ("Citazione", Icons.Outlined.FormatQuote, "> "),
    Templ("Codice", Icons.Outlined.Code, "```\n\n```"),
)

@Composable
fun MarkdownPreview(
    text: String,
    editing: Boolean,
    onToggleTask: (Int) -> Unit,
    onReplace: (Int, Int, String) -> Unit,
    onAppend: (String) -> Unit,
) {
    val blocks = remember(text) { parser.parse(text).kids().mapNotNull { b -> b.lines()?.let { b to it } } }
    var editLine by remember { mutableStateOf<Int?>(null) }
    var draft by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(editing) { if (!editing) { editLine = null; draft = null } }

    CompositionLocalProvider(LocalEditing provides editing, LocalToggle provides onToggleTask) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hint") {
                AnimatedVisibility(editing, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.TouchApp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Tocca un blocco per modificarlo", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            if (blocks.isEmpty() && !editing) item(key = "empty") {
                Text(
                    "Documento vuoto.\nTocca Modifica per iniziare a scrivere.",
                    Modifier.fillMaxWidth().padding(top = 64.dp), textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge,
                )
            }
            items(blocks, key = { it.second.first }) { (node, range) ->
                Box(Modifier.animateItem()) {
                    if (editLine == range.first) {
                        BlockEditor(
                            initial = MdOps.slice(text, range.first, range.last),
                            onDone = { s -> editLine = null; onReplace(range.first, range.last, s) },
                            onCancel = { editLine = null },
                            onDelete = { editLine = null; onReplace(range.first, range.last, "") },
                        )
                    } else {
                        EditableFrame(editing, onClick = { draft = null; editLine = range.first }) { RenderBlock(node) }
                    }
                }
            }
            if (editing) item(key = "add") {
                Column(Modifier.animateItem().animateContentSize()) {
                    val d = draft
                    if (d != null) {
                        BlockEditor(
                            initial = d, isNew = true,
                            onDone = { s -> draft = null; if (s.isNotBlank()) onAppend(s) },
                            onCancel = { draft = null },
                            onDelete = { draft = null },
                        )
                    } else {
                        Text(
                            "Aggiungi", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                        )
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TEMPLATES.forEach { t ->
                                AssistChip(
                                    onClick = { editLine = null; draft = t.md },
                                    label = { Text(t.label) },
                                    leadingIcon = { Icon(t.icon, null, Modifier.size(18.dp)) },
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableFrame(editing: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val border by animateColorAsState(
        if (editing) MaterialTheme.colorScheme.outlineVariant else Color.Transparent, tween(300), label = "frame",
    )
    val pad by animateFloatAsState(if (editing) 10f else 0f, tween(300), label = "framePad")
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .then(if (editing) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(pad.dp),
    ) { content() }
}

@Composable
private fun BlockEditor(initial: String, onDone: (String) -> Unit, onCancel: () -> Unit, onDelete: () -> Unit, isNew: Boolean = false) {
    var tfv by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length.let { if (initial.endsWith("\n```")) initial.indexOf('\n') + 1 else it }))) }
    val focus = remember { FocusRequester() }
    val into = remember { BringIntoViewRequester() }
    LaunchedEffect(Unit) {
        focus.requestFocus()
        // Aspetta che la tastiera sia salita, poi porta in vista anche i pulsanti.
        repeat(3) { delay(200); into.bringIntoView() }
    }
    val colors = rememberHiColors()
    val hl = remember(colors) { MdHighlighter(colors) }
    Surface(
        Modifier.fillMaxWidth().bringIntoViewRequester(into), shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 6.dp)) {
            BasicTextField(
                value = tfv, onValueChange = { tfv = it },
                textStyle = MonoStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = hl,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(end = 6.dp).focusRequester(focus),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, if (isNew) "Scarta" else "Elimina blocco", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCancel) { Text("Annulla") }
                Spacer(Modifier.width(4.dp))
                FilledTonalButton(onClick = { onDone(tfv.text) }) {
                    Icon(Icons.Filled.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (isNew) "Aggiungi" else "Fatto")
                }
            }
        }
    }
}

// ---------- Rendering dei blocchi ----------

@Composable
private fun RenderBlock(node: Node) {
    val cs = MaterialTheme.colorScheme
    when (node) {
        is Heading -> HeadingView(node)
        is Paragraph -> InlineText(node, MaterialTheme.typography.bodyLarge)
        is BlockQuote -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().clip(CircleShape).background(cs.primary.copy(alpha = 0.6f)))
            Spacer(Modifier.width(14.dp))
            CompositionLocalProvider(LocalContentColor provides cs.onSurfaceVariant) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { node.kids().forEach { RenderBlock(it) } }
            }
        }
        is ListBlock -> ListView(node)
        is FencedCodeBlock -> CodeBlockView(node.literal.trimEnd('\n'), node.info)
        is IndentedCodeBlock -> CodeBlockView(node.literal.trimEnd('\n'), null)
        is HtmlBlock -> CodeBlockView(node.literal.trimEnd('\n'), "html")
        is ThematicBreak -> Box(
            Modifier.fillMaxWidth().padding(vertical = 8.dp).height(2.dp).clip(CircleShape)
                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Transparent, cs.primary.copy(alpha = 0.5f), Color.Transparent))),
        )
        is TableBlock -> TableView(node)
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { node.kids().forEach { RenderBlock(it) } }
    }
}

@Composable
private fun HeadingView(h: Heading) {
    val t = MaterialTheme.typography
    val style = when (h.level) { 1 -> t.headlineMedium; 2 -> t.headlineSmall; 3 -> t.titleLarge; 4 -> t.titleMedium; else -> t.titleSmall }
    Column(Modifier.padding(top = if (h.level <= 2) 8.dp else 4.dp)) {
        InlineText(h, style)
        if (h.level == 1) Box(
            Modifier.padding(top = 8.dp).width(56.dp).height(4.dp).clip(CircleShape).background(BrandGradient),
        ) else if (h.level == 2) HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ListView(list: ListBlock) {
    val editing = LocalEditing.current
    val toggle = LocalToggle.current
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(if (list.isTight) 2.dp else 8.dp)) {
        var n = (list as? OrderedList)?.markerStartNumber ?: 1
        list.kids().filterIsInstance<ListItem>().forEach { item ->
            val marker = item.firstChild as? TaskListItemMarker
            val line = item.lines()?.first
            val onTap: (() -> Unit)? = if (marker != null && line != null) {
                {
                    haptic.performHapticFeedback(if (marker.isChecked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                    toggle(line)
                }
            } else null
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .then(if (onTap != null && !editing) Modifier.clickable(onClick = onTap) else Modifier)
                    .padding(vertical = 2.dp),
            ) {
                when {
                    marker != null -> TaskBox(marker.isChecked, onTap ?: {})
                    list is OrderedList -> Text(
                        "$n.", Modifier.widthIn(min = 28.dp).padding(top = 2.dp), color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge,
                    )
                    else -> Box(Modifier.width(28.dp).height(26.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
                Column(Modifier.weight(1f).padding(top = if (marker != null) 7.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.kids().filter { it !is TaskListItemMarker }.forEach { child ->
                        if (child is Paragraph) InlineText(child, MaterialTheme.typography.bodyLarge, done = marker?.isChecked == true)
                        else RenderBlock(child)
                    }
                }
            }
            n++
        }
    }
}

@Composable
private fun TaskBox(checked: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (checked) cs.secondary else Color.Transparent, tween(220), label = "boxFill")
    val stroke by animateColorAsState(if (checked) cs.secondary else cs.outline, tween(220), label = "boxStroke")
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(checked) {
        bounce.snapTo(0.8f)
        bounce.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
    }
    Box(
        Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.scale(bounce.value).size(22.dp).clip(RoundedCornerShape(7.dp)).background(fill)
                .border(2.dp, stroke, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(checked, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = fadeOut(tween(120))) {
                Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = cs.onSecondary)
            }
        }
    }
}

@Composable
private fun CodeBlockView(code: String, info: String?) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(vertical = 12.dp)) {
            if (!info.isNullOrBlank()) Text(
                info.uppercase(), Modifier.padding(start = 16.dp, bottom = 6.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp,
            )
            Text(
                code, Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                style = MonoStyle.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface), softWrap = false,
            )
        }
    }
}

@Composable
private fun TableView(table: TableBlock) {
    val rows = table.kids().flatMap { it.kids() }.filterIsInstance<TableRow>()
    val cols = rows.maxOfOrNull { it.kids().size } ?: 0
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, cs.outlineVariant, RoundedCornerShape(14.dp))
            .horizontalScroll(rememberScrollState()),
    ) {
        // Per colonne: senza vincolo di larghezza il testo resta su una riga, quindi le righe restano allineate.
        Row {
            for (c in 0 until cols) {
                Column(Modifier.width(IntrinsicSize.Max)) {
                    rows.forEachIndexed { r, row ->
                        val cell = row.kids().getOrNull(c) as? TableCell
                        val header = cell?.isHeader == true
                        Box(
                            Modifier.fillMaxWidth()
                                .background(if (header) cs.primaryContainer else if (r % 2 == 0) cs.surfaceContainerLow else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            if (cell != null) InlineText(
                                cell,
                                MaterialTheme.typography.bodyMedium.copy(fontWeight = if (header) FontWeight.Bold else FontWeight.Normal),
                                softWrap = false,
                            ) else Text(" ")
                        }
                    }
                }
            }
        }
    }
}

// ---------- Testo in linea ----------

@Composable
private fun InlineText(node: Node, style: TextStyle, done: Boolean = false, softWrap: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    val base = LocalContentColor.current.takeIf { it != Color.Unspecified } ?: cs.onSurface
    val color by animateColorAsState(if (done) cs.onSurfaceVariant.copy(alpha = 0.6f) else base, tween(300), label = "done")
    val s = remember(node, cs) {
        buildAnnotatedString {
            appendInlines(node, cs.primary, cs.tertiary, cs.surfaceContainerHigh) { url -> runCatching { uri.openUri(url) } }
        }
    }
    Text(
        s, style = style, color = color, softWrap = softWrap,
        textDecoration = if (done) TextDecoration.LineThrough else null,
    )
}

private fun AnnotatedString.Builder.appendInlines(n: Node, link: Color, code: Color, codeBg: Color, open: (String) -> Unit) {
    for (c in n.kids()) when (c) {
        is org.commonmark.node.Text -> append(c.literal)
        is SoftLineBreak -> append(' ')
        is HardLineBreak -> append('\n')
        is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { appendInlines(c, link, code, codeBg, open) }
        is StrongEmphasis -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendInlines(c, link, code, codeBg, open) }
        is Strikethrough -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { appendInlines(c, link, code, codeBg, open) }
        is Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = code, background = codeBg, fontSize = 0.88.em)) {
            append(" ${c.literal} ")
        }
        is Link -> withLink(
            LinkAnnotation.Clickable(
                c.destination,
                TextLinkStyles(SpanStyle(color = link, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)),
            ) { open(c.destination) },
        ) { appendInlines(c, link, code, codeBg, open) }
        // shortcut: le immagini mostrano solo il testo alternativo; per vederle servirebbe un caricatore (es. Coil).
        is Image -> withStyle(SpanStyle(color = link, fontStyle = FontStyle.Italic)) { append("🖼 "); appendInlines(c, link, code, codeBg, open) }
        is HtmlInline -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = code)) { append(c.literal) }
        else -> appendInlines(c, link, code, codeBg, open)
    }
}
