package com.emmdi.app

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun App(vm: MainVm) {
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let { snack.showSnackbar(it); vm.message = null }
    }
    val open = vm.uri != null
    BackHandler(open) { if (vm.editing) vm.editing = false else vm.close() }

    Scaffold(snackbarHost = { SnackbarHost(snack) }, contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)) { pad ->
        AnimatedContent(
            targetState = open,
            modifier = Modifier.padding(pad),
            transitionSpec = {
                val dir = if (targetState) 1 else -1
                (slideInHorizontally(tween(380)) { it * dir / 4 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(380)) { -it * dir / 4 } + fadeOut(tween(220)))
            },
            label = "screen",
        ) { isOpen -> if (isOpen) DocScreen(vm) else HomeScreen(vm) }
    }
}

// ---------- Home ----------

@Composable
private fun HomeScreen(vm: MainVm) {
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::open) }
    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { it?.let(vm::create) }
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AnimatedVisibility(appear, enter = fadeIn(tween(500)) + slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) { -it / 3 }) {
                Hero(onOpen = { openLauncher.launch(arrayOf("*/*")) }, onNew = { createLauncher.launch("Nuovo documento.md") })
            }
        }
        item {
            Text(
                "Recenti", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp, bottom = 2.dp, start = 4.dp),
            )
        }
        if (vm.recents.isEmpty()) item {
            AnimatedVisibility(appear, enter = fadeIn(tween(600, 250))) { EmptyRecents() }
        }
        items(vm.recents, key = { it.uri }) { r ->
            RecentRow(r, Modifier.animateItem(), onClick = { vm.open(android.net.Uri.parse(r.uri)) }, onRemove = { vm.forget(r.uri) })
        }
    }
}

@Composable
private fun Hero(onOpen: () -> Unit, onNew: () -> Unit) {
    Box(
        Modifier
            .statusBarsPadding()
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(BrandGradient)
            .padding(24.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.ic_launcher_art), null,
                    Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)),
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("emMeDi", style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Text("Leggi, spunta e scrivi Markdown", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onOpen, modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF3B2F8F)),
                ) {
                    Icon(Icons.Outlined.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Apri file", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onNew, modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.18f), contentColor = Color.White),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.NoteAdd, null); Spacer(Modifier.width(8.dp)); Text("Nuovo", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyRecents() {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Description, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text("Nessun file aperto di recente", style = MaterialTheme.typography.titleMedium)
        Text(
            "Apri un file .md o creane uno nuovo", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecentRow(r: Recent, modifier: Modifier, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text("M↓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    DateUtils.getRelativeTimeSpanString(r.time).toString(),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, "Rimuovi dai recenti", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------- Documento ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocScreen(vm: MainVm) {
    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    navigationIcon = { IconButton(onClick = vm::close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(vm.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            SaveIndicator(vm.save)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
                ModeToggle(vm.mode, { vm.mode = it }, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp))
            }
        },
        floatingActionButton = { EditFab(vm.editing) { vm.editing = !vm.editing } },
    ) { pad ->
        AnimatedContent(
            targetState = vm.mode,
            modifier = Modifier.padding(pad).imePadding(),
            transitionSpec = {
                val dir = if (targetState == Mode.CODE) 1 else -1
                ContentTransform(
                    slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it * dir / 5 } + fadeIn(tween(250)),
                    slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { -it * dir / 5 } + fadeOut(tween(150)),
                )
            },
            label = "mode",
        ) { mode ->
            when (mode) {
                Mode.PREVIEW -> MarkdownPreview(
                    text = vm.text, editing = vm.editing,
                    onToggleTask = vm::toggleTask,
                    onReplace = { f, l, s -> vm.update(MdOps.replaceLines(vm.text, f, l, s), immediate = true) },
                    onAppend = { vm.update(MdOps.append(vm.text, it), immediate = true) },
                )
                Mode.CODE -> CodeView(vm.text, vm.editing, vm::update)
            }
        }
    }
}

@Composable
private fun SaveIndicator(s: SaveState) {
    AnimatedContent(s, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "save") { st ->
        val (icon, label, color) = when (st) {
            SaveState.SAVED -> Triple(Icons.Outlined.CloudDone, "Salvato", MaterialTheme.colorScheme.secondary)
            SaveState.SAVING -> Triple(Icons.Outlined.Sync, "Salvataggio…", MaterialTheme.colorScheme.onSurfaceVariant)
            SaveState.ERROR -> Triple(Icons.Outlined.ErrorOutline, "Non salvato", MaterialTheme.colorScheme.error)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(13.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun ModeToggle(mode: Mode, onChange: (Mode) -> Unit, modifier: Modifier) {
    val itemW = 136.dp
    val offset by animateDpAsState(
        if (mode == Mode.PREVIEW) 0.dp else itemW,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow), label = "pill",
    )
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        Box(
            Modifier.offset(x = offset).width(itemW).height(40.dp)
                .clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        )
        Row {
            listOf(Mode.PREVIEW to ("Anteprima" to Icons.Outlined.Visibility), Mode.CODE to ("Codice" to Icons.Outlined.Code))
                .forEach { (m, li) ->
                    val selected = m == mode
                    val c by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        tween(250), label = "pillText",
                    )
                    Row(
                        Modifier.width(itemW).height(40.dp).clip(CircleShape).clickable { onChange(m) },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(li.second, null, Modifier.size(18.dp), tint = c)
                        Spacer(Modifier.width(8.dp))
                        Text(li.first, color = c, style = MaterialTheme.typography.labelLarge)
                    }
                }
        }
    }
}

@Composable
private fun EditFab(editing: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (editing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, tween(300), label = "fab",
    )
    val content by animateColorAsState(
        if (editing) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary, tween(300), label = "fabC",
    )
    ExtendedFloatingActionButton(onClick = onClick, containerColor = container, contentColor = content, shape = RoundedCornerShape(20.dp)) {
        AnimatedContent(
            editing,
            transitionSpec = {
                (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith (scaleOut() + fadeOut())
            },
            label = "fabIcon",
        ) { e ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (e) Icons.Filled.Check else Icons.Outlined.Edit, null)
                Spacer(Modifier.width(10.dp))
                Text(if (e) "Fine" else "Modifica", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------- Codice ----------

@Composable
fun rememberHiColors(): HiColors {
    val cs = MaterialTheme.colorScheme
    return remember(cs) {
        HiColors(
            accent = cs.primary, check = cs.secondary, muted = cs.onSurfaceVariant,
            code = cs.tertiary, codeBg = cs.surfaceContainerHigh, link = cs.primary,
        )
    }
}

@Composable
fun CodeView(text: String, editing: Boolean, onChange: (String) -> Unit) {
    var tfv by remember { mutableStateOf(TextFieldValue(text)) }
    LaunchedEffect(text) {
        if (text != tfv.text) tfv = tfv.copy(text = text, selection = TextRange(tfv.selection.start.coerceAtMost(text.length)))
    }
    val colors = rememberHiColors()
    val hl = remember(colors) { MdHighlighter(colors) }
    val border by animateColorAsState(
        if (editing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent, tween(300), label = "codeBorder",
    )
    Surface(
        Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(bottom = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, border),
    ) {
        BasicTextField(
            value = tfv,
            onValueChange = { tfv = it; if (it.text != text) onChange(it.text) },
            readOnly = !editing,
            textStyle = MonoStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = hl,
            modifier = Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 96.dp),
        )
    }
}
