package com.emmdi.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val vm: MainVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent { EmTheme { App(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onPause() {
        super.onPause()
        vm.flush()
    }

    private fun handle(i: Intent?) {
        if (i?.action == Intent.ACTION_VIEW || i?.action == Intent.ACTION_EDIT) i.data?.let { vm.open(it) }
    }
}

data class Recent(val uri: String, val name: String, val time: Long)
enum class Mode { PREVIEW, CODE }
enum class SaveState { SAVED, SAVING, ERROR }

class MainVm(app: Application) : AndroidViewModel(app) {
    private val resolver = app.contentResolver
    private val prefs = app.getSharedPreferences("emmdi", 0)

    var recents by mutableStateOf(loadRecents()); private set
    var uri by mutableStateOf<Uri?>(null); private set
    var name by mutableStateOf(""); private set
    var text by mutableStateOf(""); private set
    var mode by mutableStateOf(Mode.PREVIEW)
    var editing by mutableStateOf(false)
    var save by mutableStateOf(SaveState.SAVED); private set
    var message by mutableStateOf<String?>(null)

    private var crlf = false
    private var saveJob: Job? = null

    // ---- aggiornamenti dall'app ----
    var update by mutableStateOf<UpdateState>(UpdateState.Idle); private set

    /** Al primo avvio in silenzio; dal pulsante dice anche se qualcosa va storto. */
    fun checkUpdate(manual: Boolean = false) {
        if (update is UpdateState.Checking || update is UpdateState.Downloading || update is UpdateState.Ready) return
        update = UpdateState.Checking
        viewModelScope.launch {
            update = runCatching { Updater.check(getApplication()) }.fold(
                onSuccess = { r -> r?.let { UpdateState.Available(it) } ?: UpdateState.UpToDate },
                onFailure = {
                    if (manual) message = "Impossibile controllare gli aggiornamenti"
                    UpdateState.Idle
                },
            )
        }
    }

    fun downloadUpdate() {
        val release = (update as? UpdateState.Available)?.release ?: return
        update = UpdateState.Downloading(release, 0f)
        viewModelScope.launch {
            update = runCatching {
                Updater.download(getApplication(), release) { p -> update = UpdateState.Downloading(release, p) }
            }.fold(
                onSuccess = { UpdateState.Ready(release, it).also { r -> installUpdate(r) } },
                onFailure = {
                    message = "Download non riuscito"
                    UpdateState.Available(release)
                },
            )
        }
    }

    fun installUpdate(ready: UpdateState.Ready? = update as? UpdateState.Ready) {
        ready ?: return
        val app = getApplication<Application>()
        // Android chiede una volta il permesso di installare app: poi si torna qui e si tocca Installa.
        if (!Updater.canInstall(app)) Updater.openInstallPermission(app) else Updater.install(app, ready.file)
    }

    fun open(u: Uri, startEditing: Boolean = false) {
        viewModelScope.launch {
            try {
                val raw = withContext(Dispatchers.IO) {
                    resolver.openInputStream(u)!!.use { it.readBytes().decodeToString() }
                }.removePrefix("﻿")
                runCatching { resolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
                    .onFailure { runCatching { resolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
                crlf = "\r\n" in raw
                text = raw.replace("\r\n", "\n")
                name = displayName(u)
                mode = Mode.PREVIEW
                editing = startEditing
                save = SaveState.SAVED
                uri = u
                remember(Recent(u.toString(), name, System.currentTimeMillis()))
            } catch (e: Exception) {
                message = "Impossibile aprire il file"
                forget(u.toString())
            }
        }
    }

    fun create(u: Uri) {
        viewModelScope.launch {
            val ok = write(u, "# Nuovo documento\n\n- [ ] Prima cosa da fare\n")
            if (ok) open(u, startEditing = true) else message = "Impossibile creare il file"
        }
    }

    fun close() {
        flush()
        editing = false
        uri = null
    }

    fun update(new: String, immediate: Boolean = false) {
        if (new == text) return
        text = new
        save = SaveState.SAVING
        saveJob?.cancel()
        val u = uri ?: return
        saveJob = viewModelScope.launch {
            if (!immediate) delay(600)
            save = if (write(u, new)) SaveState.SAVED else SaveState.ERROR
            if (save == SaveState.ERROR) message = "Salvataggio non riuscito: il file potrebbe essere in sola lettura"
        }
    }

    /** Salva subito l'eventuale modifica in attesa (uscita dal file o dall'app). */
    fun flush() {
        if (save != SaveState.SAVING || saveJob?.isActive != true) return
        saveJob?.cancel()
        val u = uri ?: return
        val t = text
        saveJob = viewModelScope.launch { save = if (write(u, t)) SaveState.SAVED else SaveState.ERROR }
    }

    fun toggleTask(line: Int) = update(MdOps.toggleTask(text, line), immediate = true)

    fun forget(u: String) {
        recents = recents.filterNot { it.uri == u }
        storeRecents()
    }

    private suspend fun write(u: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        val bytes = (if (crlf) content.replace("\n", "\r\n") else content).toByteArray()
        // "wt" tronca il file; alcuni provider conoscono solo "w".
        listOf("wt", "w").any { m ->
            runCatching { resolver.openOutputStream(u, m)!!.use { it.write(bytes) } }.isSuccess
        }
    }

    private fun displayName(u: Uri): String =
        runCatching {
            resolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: u.lastPathSegment?.substringAfterLast('/') ?: "Documento"

    private fun remember(r: Recent) {
        recents = (listOf(r) + recents.filterNot { it.uri == r.uri }).take(20)
        storeRecents()
    }

    private fun loadRecents(): List<Recent> = runCatching {
        val a = JSONArray(prefs.getString("recents", "[]"))
        List(a.length()) { a.getJSONObject(it).run { Recent(getString("u"), getString("n"), getLong("t")) } }
    }.getOrDefault(emptyList())

    private fun storeRecents() {
        val a = JSONArray()
        recents.forEach { a.put(JSONObject().put("u", it.uri).put("n", it.name).put("t", it.time)) }
        prefs.edit().putString("recents", a.toString()).apply()
    }
}
