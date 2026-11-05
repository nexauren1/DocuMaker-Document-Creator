package com.documaker.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.OpenableColumns
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.InsertPhoto
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.documaker.app.data.DocItem
import com.documaker.app.data.DocumentExporter
import com.documaker.app.data.DocumentImporter
import com.documaker.app.data.DocumentStore
import com.documaker.app.data.DocumentTemplates
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

private val Ink = Color(0xFF24233A)
private val Muted = Color(0xFF77768D)
private val Accent = Color(0xFF6557D2)
private val AccentSoft = Color(0xFFF0EDFF)
private val Canvas = Color(0xFFF6F5FA)
private val Teal = Color(0xFF36BFA9)

class MainActivity : ComponentActivity() {
    private lateinit var store: DocumentStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = DocumentStore(this)
        setContent { DocuMakerApp(store) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocuMakerApp(store: DocumentStore) {
    val context = LocalContext.current
    val docs = remember { mutableStateListOf<DocItem>().apply { addAll(store.all()) } }
    var editing by remember { mutableStateOf<DocItem?>(null) }
    var createSheet by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<ExportPayload?>(null) }

    fun refresh() { docs.clear(); docs.addAll(store.all()) }
    fun createFromTemplate(template: Pair<String, String>) {
        val title = when (template.first) {
            "Blank document" -> "Untitled document"
            else -> template.first
        }
        editing = store.create(title, template.second)
        refresh()
        createSheet = false
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                }
                val imported = DocumentImporter.read(context, uri, name)
                editing = store.create(imported.title, imported.html)
                refresh()
            } catch (e: Exception) { importError = e.message ?: "Could not import this file" }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val payload = pendingExport
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null && payload != null) {
            try {
                DocumentExporter.write(context, uri, payload.format, payload.title, payload.html, payload.text)
                Toast.makeText(context, "Export completed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Export failed", Toast.LENGTH_LONG).show()
            }
        }
        pendingExport = null
    }

    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(
        primary = Accent, onPrimary = Color.White, secondary = Teal, background = Canvas,
        surface = Color.White, onSurface = Ink, onBackground = Ink
    )) {
        if (editing == null) {
            LibraryScreen(
                docs = docs,
                onOpen = { editing = store.get(it) },
                onCreate = { createSheet = true },
                onTemplate = { createFromTemplate(it) },
                onImport = { importLauncher.launch(arrayOf("text/*", "application/*", "image/*")) },
                onStar = { id, value -> store.toggleStar(id, value); refresh() },
                onDuplicate = { store.duplicate(it); refresh() },
                onDelete = { store.delete(it); refresh() },
                onRename = { id, title -> store.rename(id, title); refresh() }
            )
        } else {
            val doc = editing!!
            EditorScreen(
                doc = doc,
                onBack = { refresh(); editing = null },
                onSave = { html, _ -> store.update(doc.id, doc.title, html) },
                onRename = { title -> store.rename(doc.id, title); editing = store.get(doc.id) },
                onDuplicate = { val copy = store.duplicate(doc.id); refresh(); if (copy != null) editing = copy },
                onMove = { folder -> store.move(doc.id, folder); refresh() },
                onStar = { store.toggleStar(doc.id, !doc.starred); refresh() },
                onExport = { format, title, html, text ->
                    if (format == "pdf") {
                        Toast.makeText(context, "Choose Print / Save as PDF from the print screen", Toast.LENGTH_LONG).show()
                    } else {
                        pendingExport = ExportPayload(format, title, html, text)
                        val ext = format.lowercase()
                        val mime = when (ext) {
                            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                            "odt" -> "application/vnd.oasis.opendocument.text"
                            "rtf" -> "application/rtf"
                            "epub" -> "application/epub+zip"
                            "html" -> "text/html"
                            else -> "text/plain"
                        }
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE); type = mime
                            putExtra(Intent.EXTRA_TITLE, "${title.ifBlank { "Document" }}.$ext")
                        }
                        exportLauncher.launch(intent)
                    }
                },
                onShare = { text ->
                    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, doc.title); putExtra(Intent.EXTRA_TEXT, text) }
                    context.startActivity(Intent.createChooser(intent, "Share document text"))
                }
            )
        }

        if (createSheet) {
            ModalBottomSheet(onDismissRequest = { createSheet = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create a document", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Start from a blank page or use a ready-made structure.", color = Muted, fontSize = 13.sp)
                    DocumentTemplates.items.forEachIndexed { index, template ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Canvas).clickable { createFromTemplate(template) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(10.dp), color = AccentSoft, modifier = Modifier.size(42.dp)) { Box(contentAlignment = Alignment.Center) { Icon(if (index == 0) Icons.Default.Description else Icons.Default.Article, null, tint = Accent) } }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(template.first, fontWeight = FontWeight.SemiBold, color = Ink); Text(if(index == 0) "A clean page for your ideas" else "A structured starting point", fontSize = 12.sp, color = Muted) }
                            Icon(Icons.Default.Add, null, tint = Accent)
                        }
                    }
                    OutlinedButton(onClick = { createSheet = false; importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Import an existing file") }
                }
            }
        }
        importError?.let { message -> AlertDialog(onDismissRequest = { importError = null }, title = { Text("Import problem") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { importError = null }) { Text("OK") } }) }
    }
}

private data class ExportPayload(val format: String, val title: String, val html: String, val text: String)

@Composable
private fun LibraryScreen(
    docs: List<DocItem>, onOpen: (Long) -> Unit, onCreate: () -> Unit, onTemplate: (Pair<String, String>) -> Unit, onImport: () -> Unit,
    onStar: (Long, Boolean) -> Unit, onDuplicate: (Long) -> Unit, onDelete: (Long) -> Unit, onRename: (Long, String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Recent") }
    val filtered = docs.filter { it.title.contains(search, true) && (filter != "Starred" || it.starred) }
        .sortedByDescending { it.updatedAt }
    Scaffold(
        containerColor = Canvas,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate, containerColor = Accent, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Add, contentDescription = "Create document", modifier = Modifier.size(28.dp)) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = Ink, modifier = Modifier.size(44.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, null, tint = Color.White, modifier = Modifier.size(24.dp)) } }
                Column(Modifier.weight(1f).padding(start = 11.dp)) {
                    Text("DocuMaker", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("DOCUMENT WORKSPACE", fontSize = 9.sp, letterSpacing = 1.4.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onImport) { Icon(Icons.Default.FolderOpen, "Import", tint = Ink) }
                IconButton(onClick = onCreate) { Icon(Icons.Default.CreateNewFolder, "New", tint = Accent) }
            }
            OutlinedTextField(
                value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search documents", color = Muted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFFE5E2EF), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )
            Row(Modifier.padding(top = 21.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your workspace", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Create, edit and keep everything organised.", fontSize = 12.sp, color = Muted)
                }
                AssistChip(onClick = { filter = if (filter == "Recent") "Starred" else "Recent" }, label = { Text(filter) }, leadingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp), tint = Accent) })
            }
            Text("START WITH A TEMPLATE", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Blank", "Meeting", "Report", "Resume", "Invoice").forEach { name ->
                    val template = DocumentTemplates.items.firstOrNull { it.first.startsWith(name, ignoreCase = true) } ?: DocumentTemplates.items[0]
                    Card(onClick = { onTemplate(template) }, modifier = Modifier.width(118.dp).height(86.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEAE8F1))) {
                        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Icon(if (name == "Blank") Icons.Default.Add else Icons.Default.Description, null, tint = Accent)
                            Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 23.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (filter == "Starred") "Starred documents" else "Recent documents", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f)); Text("${filtered.size} files", fontSize = 11.sp, color = Muted)
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(shape = CircleShape, color = AccentSoft, modifier = Modifier.size(76.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, null, tint = Accent, modifier = Modifier.size(35.dp)) } }
                        Spacer(Modifier.height(12.dp)); Text(if (search.isBlank()) "Your workspace is ready" else "No matching documents", fontWeight = FontWeight.Bold, color = Ink)
                        Text(if (search.isBlank()) "Create your first document to get started." else "Try another search term.", color = Muted, fontSize = 12.sp)
                        if (search.isBlank()) Button(onClick = onCreate, modifier = Modifier.padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Create document") }
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(bottom = 100.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered, key = { it.id }) { doc -> DocumentCard(doc, onOpen, onStar, onDuplicate, onDelete, onRename) }
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(doc: DocItem, onOpen: (Long) -> Unit, onStar: (Long, Boolean) -> Unit, onDuplicate: (Long) -> Unit, onDelete: (Long) -> Unit, onRename: (Long, String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    var title by remember(doc.id) { mutableStateOf(doc.title) }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEAE8F1)), modifier = Modifier.fillMaxWidth().clickable { onOpen(doc.id) }) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = AccentSoft, shape = RoundedCornerShape(9.dp), modifier = Modifier.size(33.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, null, tint = Accent, modifier = Modifier.size(19.dp)) } }
                Spacer(Modifier.weight(1f))
                if (doc.starred) Icon(Icons.Default.Star, null, tint = Color(0xFFE7AA3A), modifier = Modifier.size(17.dp))
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.MoreVert, null, tint = Muted, modifier = Modifier.size(19.dp)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(if (doc.starred) "Remove from starred" else "Add to starred") }, leadingIcon = { Icon(Icons.Default.Star, null) }, onClick = { onStar(doc.id, !doc.starred); menu = false })
                        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { title = doc.title; renameDialog = true; menu = false })
                        DropdownMenuItem(text = { Text("Make a copy") }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) }, onClick = { onDuplicate(doc.id); menu = false })
                        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { onDelete(doc.id); menu = false })
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            Text(doc.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 2, minLines = 2)
            Spacer(Modifier.height(7.dp))
            Text("Edited ${relativeDate(doc.updatedAt)}", fontSize = 10.sp, color = Muted)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(65.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFF9F8FC)).padding(9.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val preview = doc.html.replace("<[^>]*>".toRegex(), " ").replace("&nbsp;", " ").trim()
                    Text(preview.ifBlank { "No content yet" }, fontSize = 9.sp, color = Color(0xFF8B899A), maxLines = 3, lineHeight = 13.sp)
                    Box(Modifier.fillMaxWidth(.68f).height(2.dp).background(Color(0xFFE5E2EF), RoundedCornerShape(2.dp)))
                }
            }
        }
    }
    if (renameDialog) AlertDialog(onDismissRequest = { renameDialog = false }, title = { Text("Rename document") }, text = { OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true) }, confirmButton = { TextButton(onClick = { onRename(doc.id, title); renameDialog = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(
    doc: DocItem,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit,
    onRename: (String) -> Unit,
    onDuplicate: () -> Unit,
    onMove: (String) -> Unit,
    onStar: () -> Unit,
    onExport: (String, String, String, String) -> Unit,
    onShare: (String) -> Unit
) {
    val context = LocalContext.current
    val saveCallback by rememberUpdatedState(onSave)
    val exportCallback by rememberUpdatedState(onExport)
    var html by remember(doc.id) { mutableStateOf(doc.html) }
    var title by remember(doc.id) { mutableStateOf(doc.title) }
    var panel by remember { mutableStateOf<String?>(null) }
    var moreMenu by remember { mutableStateOf(false) }
    var formatStyle by remember { mutableStateOf("Normal text") }
    var fontName by remember { mutableStateOf("Arial") }
    var fontSize by remember { mutableStateOf(16) }
    var showFind by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var showLink by remember { mutableStateOf(false) }
    var linkText by remember { mutableStateOf("https://") }
    var showComment by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var showTable by remember { mutableStateOf(false) }
    var tableRows by remember { mutableStateOf("3") }
    var tableCols by remember { mutableStateOf("3") }
    var showPageSetup by remember { mutableStateOf(false) }
    var pageSize by remember { mutableStateOf("A4") }
    var margins by remember { mutableStateOf("26") }
    var showRename by remember { mutableStateOf(false) }
    var showCount by remember { mutableStateOf(false) }
    var countResult by remember { mutableStateOf("Calculating…") }
    var showDetails by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var moveFolder by remember { mutableStateOf(doc.folder) }
    var showOutline by remember { mutableStateOf(false) }
    var printLayout by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf(false) }
    var offline by remember { mutableStateOf(true) }
    var pendingExportFormat by remember { mutableStateOf("docx") }

    val webView = remember(doc.id) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.rgb(246, 245, 250))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            webChromeClient = WebChromeClient()
            addJavascriptInterface(object {
                @JavascriptInterface fun onChange(newHtml: String, plain: String, ignoredTitle: String) {
                    Handler(Looper.getMainLooper()).post {
                        html = newHtml
                        saveCallback(newHtml, plain)
                    }
                }
                @JavascriptInterface fun onEditorReady() {
                    Handler(Looper.getMainLooper()).post {
                        evaluateJavascript("window.DocuMaker.setContent(${JSONObject.quote(doc.html)})", null)
                    }
                }
                @JavascriptInterface fun onExport(format: String, exportTitle: String, bodyHtml: String, plain: String) {
                    Handler(Looper.getMainLooper()).post { exportCallback(format, exportTitle, bodyHtml, plain) }
                }
            }, "Native")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    view?.evaluateJavascript("window.DocuMaker.setContent(${JSONObject.quote(doc.html)})", null)
                }
            }
            loadUrl("file:///android_asset/editor.html")
        }
    }

    fun js(code: String) { webView.evaluateJavascript("window.DocuMaker && window.DocuMaker.$code", null) }
    fun requestExport(format: String) {
        pendingExportFormat = format
        if (format == "pdf") {
            val activity = context.findActivity()
            val manager = activity?.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (manager != null) {
                val adapter = webView.createPrintDocumentAdapter(title.ifBlank { "DocuMaker document" })
                manager.print(title.ifBlank { "DocuMaker document" }, adapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
            } else Toast.makeText(context, "Printing is not available on this device", Toast.LENGTH_SHORT).show()
        } else {
            val fmt = JSONObject.quote(format)
            val name = JSONObject.quote(title)
            webView.evaluateJavascript("(function(){var d=window.DocuMaker.export();Native.onExport($fmt,$name,d.html,d.text);})()", null)
        }
        panel = null
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: byteArrayOf()
                val mime = context.contentResolver.getType(uri) ?: "image/png"
                val encoded = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val data = JSONObject.quote("data:$mime;base64,$encoded")
                webView.evaluateJavascript("window.DocuMaker.insertImage($data,'Inserted image')", null)
            } catch (e: Exception) { Toast.makeText(context, "Image could not be inserted", Toast.LENGTH_SHORT).show() }
        }
    }

    DisposableEffect(webView) { onDispose { webView.stopLoading(); webView.destroy() } }

    Scaffold(
        containerColor = Canvas,
        topBar = {
            Column {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF0EEFA)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Check, "Done", tint = Accent) }
                    Column(Modifier.weight(1f).clickable { showRename = true }.padding(start = 3.dp)) {
                        Text(title, maxLines = 1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text("Saved on this device", fontSize = 10.sp, color = Muted)
                    }
                    IconButton(onClick = { js("undo()") }) { Icon(Icons.Default.Undo, "Undo", tint = Ink) }
                    IconButton(onClick = { js("redo()") }) { Icon(Icons.Default.Redo, "Redo", tint = Ink) }
                    IconButton(onClick = { panel = "insert" }) { Icon(Icons.Default.Add, "Insert", tint = Ink) }
                    IconButton(onClick = { showComment = true }) { Icon(Icons.Default.Comment, "Comment", tint = Ink) }
                    Box {
                        IconButton(onClick = { moreMenu = true }) { Icon(Icons.Default.MoreVert, "More options", tint = Ink) }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }, modifier = Modifier.width(260.dp).height(560.dp)) {
                            DropdownMenuItem(text = { Text("Print layout") }, onClick = { printLayout = !printLayout; js("printLayout($printLayout)"); moreMenu = false }, leadingIcon = { Icon(Icons.Default.Print, null) }, trailingIcon = { Switch(checked = printLayout, onCheckedChange = { printLayout = it; js("printLayout($it)") }) })
                            DropdownMenuItem(text = { Text("Suggest edits") }, onClick = { suggestions = !suggestions; js("suggestions($suggestions)"); moreMenu = false }, leadingIcon = { Icon(Icons.Default.Edit, null) }, trailingIcon = { Switch(checked = suggestions, onCheckedChange = { suggestions = it; js("suggestions($it)") }) })
                            DropdownMenuItem(text = { Text("Document outline") }, onClick = { showOutline = true; moreMenu = false }, leadingIcon = { Icon(Icons.Default.FormatSize, null) })
                            DropdownMenuItem(text = { Text("Find and replace") }, onClick = { showFind = true; moreMenu = false }, leadingIcon = { Icon(Icons.Default.Search, null) })
                            DropdownMenuItem(text = { Text("Spelling and grammar") }, onClick = { webView.evaluateJavascript("document.getElementById('editor').setAttribute('spellcheck','true')", null); moreMenu = false }, leadingIcon = { Icon(Icons.Default.TextFields, null) })
                            DropdownMenuItem(text = { Text("Page setup") }, onClick = { showPageSetup = true; moreMenu = false }, leadingIcon = { Icon(Icons.Default.Article, null) })
                            DropdownMenuItem(text = { Text("Word count") }, onClick = { webView.evaluateJavascript("JSON.stringify(window.DocuMaker.count())") { raw -> countResult = raw?.removeSurrounding("\"", "\"") ?: ""; countResult = countResult.replace("\\\"", "\""); showCount = true }; moreMenu = false }, leadingIcon = { Icon(Icons.Default.FormatSize, null) })
                            Divider()
                            DropdownMenuItem(text = { Text("Make a copy") }, onClick = { onDuplicate(); moreMenu = false }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                            DropdownMenuItem(text = { Text("Share and export") }, onClick = { panel = "export"; moreMenu = false }, leadingIcon = { Icon(Icons.Default.Share, null) })
                            DropdownMenuItem(text = { Text("Move") }, onClick = { showMove = true; moreMenu = false }, leadingIcon = { Icon(Icons.Default.FolderOpen, null) })
                            DropdownMenuItem(text = { Text("Available offline") }, onClick = { offline = !offline; moreMenu = false }, leadingIcon = { Icon(Icons.Default.InsertDriveFile, null) }, trailingIcon = { Switch(checked = offline, onCheckedChange = { offline = it }) })
                            DropdownMenuItem(text = { Text("Star document") }, onClick = { onStar(); moreMenu = false }, leadingIcon = { Icon(Icons.Default.Star, null) })
                            DropdownMenuItem(text = { Text("Details") }, onClick = { showDetails = true; moreMenu = false }, leadingIcon = { Icon(Icons.Default.Article, null) })
                            DropdownMenuItem(text = { Text("Help and feedback") }, onClick = { Toast.makeText(context, "Help centre will be added in a future update", Toast.LENGTH_SHORT).show(); moreMenu = false })
                        }
                    }
                }
                Divider(color = Color(0xFFE4E1F1))
            }
        },
        bottomBar = {
            Surface(color = Color(0xFFF0EEFA), shadowElevation = 6.dp) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 7.dp, vertical = 6.dp).padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().coerceAtMost(8.dp)), verticalAlignment = Alignment.CenterVertically) {
                    FormatIcon(Icons.Default.FormatBold, "Bold") { js("bold()") }
                    FormatIcon(Icons.Default.FormatItalic, "Italic") { js("italic()") }
                    FormatIcon(Icons.Default.FormatUnderlined, "Underline") { js("underline()") }
                    FormatIcon(Icons.Default.FormatPaint, "Highlight") { js("highlight('#fff1a8')") }
                    FormatIcon(Icons.Default.FormatAlignLeft, "Align left") { js("align('left')") }
                    FormatIcon(Icons.Default.FormatAlignCenter, "Center") { js("align('center')") }
                    FormatIcon(Icons.Default.FormatListBulleted, "Bulleted list") { js("list('bullet')") }
                    FormatIcon(Icons.Default.FormatListNumbered, "Numbered list") { js("list('ordered')") }
                    FormatIcon(Icons.Default.FormatIndentIncrease, "Indent") { js("indent()") }
                    IconButton(onClick = { panel = "format" }) { Icon(Icons.Default.FormatSize, "More formatting", tint = Accent) }
                }
            }
        }
    ) { padding ->
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize().padding(padding), update = { })
    }

    if (panel != null) {
        ModalBottomSheet(onDismissRequest = { panel = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
            when (panel) {
                "format" -> {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("Format", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.weight(1f)); IconButton(onClick = { panel = null }) { Icon(Icons.Default.Close, null) } }
                        Text("TEXT STYLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.sp)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Normal text", "Title", "Subtitle", "Heading 1", "Heading 2", "Heading 3").forEach { style ->
                                AssistChip(onClick = { formatStyle = style; val tag = when(style){"Title"->"h1";"Subtitle"->"h2";"Heading 1"->"h1";"Heading 2"->"h2";"Heading 3"->"h3";else->"p"}; js("block('$tag')") }, label = { Text(style, fontSize = 11.sp) })
                            }
                        }
                        Text("FONT FAMILY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.sp)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Arial", "Roboto", "Georgia", "Times New Roman", "Courier New", "Verdana").forEach { family ->
                                AssistChip(onClick = { fontName = family; js("font(${JSONObject.quote(family)})") }, label = { Text(family, fontSize = 11.sp) })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Text size", fontWeight = FontWeight.SemiBold, color = Ink)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { fontSize = (fontSize - 1).coerceAtLeast(8); js("fontSize($fontSize)") }) { Text("−", fontSize = 22.sp) }
                            Text("$fontSize pt", fontWeight = FontWeight.Bold, color = Ink)
                            IconButton(onClick = { fontSize = (fontSize + 1).coerceAtMost(72); js("fontSize($fontSize)") }) { Icon(Icons.Default.Add, null, tint = Accent) }
                        }
                        Divider()
                        Text("PARAGRAPH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            FormatIcon(Icons.Default.FormatAlignLeft, "Left") { js("align('left')") }
                            FormatIcon(Icons.Default.FormatAlignCenter, "Center") { js("align('center')") }
                            FormatIcon(Icons.Default.FormatAlignRight, "Right") { js("align('right')") }
                            FormatIcon(Icons.Default.FormatAlignJustify, "Justify") { js("align('justify')") }
                            FormatIcon(Icons.Default.FormatListBulleted, "Bullets") { js("list('bullet')") }
                            FormatIcon(Icons.Default.FormatListNumbered, "Numbered") { js("list('ordered')") }
                        }
                        Text("TEXT COLOUR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted, letterSpacing = 1.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            listOf("#25243A", "#6557D2", "#008C78", "#D24C64", "#2672C9", "#D18A22").forEach { color ->
                                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(color))).clickable { js("color('$color')") })
                            }
                        }
                    }
                }
                "insert" -> {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 25.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Insert into document", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                        InsertAction("Image", "Choose a photo from your device", Icons.Default.InsertPhoto) { panel = null; imageLauncher.launch("image/*") }
                        InsertAction("Table", "Create a grid of rows and columns", Icons.Default.TableChart) { panel = null; showTable = true }
                        InsertAction("Link", "Add a clickable web address", Icons.Default.InsertLink) { panel = null; showLink = true }
                        InsertAction("Comment", "Attach a note to selected text", Icons.Default.Comment) { panel = null; showComment = true }
                        InsertAction("Checklist", "Add a task item", Icons.Default.CheckBox) { js("insertChecklist()"); panel = null }
                        InsertAction("Horizontal line", "Separate sections visually", Icons.Default.FormatAlignLeft) { js("insertRule()"); panel = null }
                        InsertAction("Page break", "Start a new page", Icons.Default.Article) { js("insertPageBreak()"); panel = null }
                        InsertAction("Bookmark", "Mark a place in the document", Icons.Default.Bookmark) { js("addBookmark('Bookmark')"); panel = null }
                        InsertAction("Header", "Add a document heading area", Icons.Default.Article) { js("insertHeaderFooter('header')"); panel = null }
                        InsertAction("Footer", "Add a document footer area", Icons.Default.Article) { js("insertHeaderFooter('footer')"); panel = null }
                    }
                }
                "export" -> {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 25.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Share & export", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Choose a format to save a copy of this document.", color = Muted, fontSize = 12.sp)
                        listOf("Word document (.docx)" to "docx", "PDF document (.pdf)" to "pdf", "OpenDocument (.odt)" to "odt", "Rich Text (.rtf)" to "rtf", "Plain text (.txt)" to "txt", "Web page (.html)" to "html", "EPUB publication (.epub)" to "epub").forEach { item ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { requestExport(item.second) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FileDownload, null, tint = Accent); Text(item.first, modifier = Modifier.weight(1f).padding(start = 12.dp), color = Ink); Icon(Icons.Default.ArrowBack, null, tint = Muted, modifier = Modifier.size(16.dp))
                            }
                        }
                        OutlinedButton(onClick = { onShare(html.replace("<[^>]*>".toRegex(), " ").trim()); panel = null }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp)); Text("Share text") }
                    }
                }
            }
        }
    }

    if (showRename) AlertDialog(onDismissRequest = { showRename = false }, title = { Text("Document name") }, text = { OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true, label = { Text("Title") }) }, confirmButton = { TextButton(onClick = { onRename(title); showRename = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } })
    if (showFind) AlertDialog(onDismissRequest = { showFind = false }, title = { Text("Find and replace") }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { OutlinedTextField(findText, { findText = it }, label = { Text("Find") }, singleLine = true); OutlinedTextField(replaceText, { replaceText = it }, label = { Text("Replace with") }, singleLine = true) } }, confirmButton = { Row { TextButton(onClick = { webView.evaluateJavascript("window.DocuMaker.find(${JSONObject.quote(findText)})", null) }) { Text("Find next") }; TextButton(onClick = { webView.evaluateJavascript("window.DocuMaker.replace(${JSONObject.quote(findText)},${JSONObject.quote(replaceText)})", null); showFind = false }) { Text("Replace all") } } }, dismissButton = { TextButton(onClick = { showFind = false }) { Text("Close") } })
    if (showLink) AlertDialog(onDismissRequest = { showLink = false }, title = { Text("Insert link") }, text = { OutlinedTextField(linkText, { linkText = it }, label = { Text("Web address") }, singleLine = true) }, confirmButton = { TextButton(onClick = { js("insertLink(${JSONObject.quote(linkText)})"); showLink = false }) { Text("Insert") } }, dismissButton = { TextButton(onClick = { showLink = false }) { Text("Cancel") } })
    if (showComment) AlertDialog(onDismissRequest = { showComment = false }, title = { Text("Add comment") }, text = { OutlinedTextField(commentText, { commentText = it }, label = { Text("Your comment") }, minLines = 2) }, confirmButton = { TextButton(onClick = { js("addComment(${JSONObject.quote(commentText)})"); commentText = ""; showComment = false }) { Text("Add comment") } }, dismissButton = { TextButton(onClick = { showComment = false }) { Text("Cancel") } })
    if (showTable) AlertDialog(onDismissRequest = { showTable = false }, title = { Text("Insert table") }, text = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(tableRows, { tableRows = it.filter(Char::isDigit).take(2) }, modifier = Modifier.weight(1f), label = { Text("Rows") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); OutlinedTextField(tableCols, { tableCols = it.filter(Char::isDigit).take(2) }, modifier = Modifier.weight(1f), label = { Text("Columns") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) } }, confirmButton = { TextButton(onClick = { js("insertTable(${tableRows.toIntOrNull()?.coerceIn(1,20) ?: 3},${tableCols.toIntOrNull()?.coerceIn(1,10) ?: 3})"); showTable = false }) { Text("Insert") } }, dismissButton = { TextButton(onClick = { showTable = false }) { Text("Cancel") } })
    if (showPageSetup) AlertDialog(onDismissRequest = { showPageSetup = false }, title = { Text("Page setup") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Paper size"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("A4", "Letter", "Full width").forEach { AssistChip(onClick = { pageSize = it }, label = { Text(it) }) } }; Text("Page margins"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("16", "26", "40", "56").forEach { AssistChip(onClick = { margins = it }, label = { Text("${it}dp") }) } } } }, confirmButton = { TextButton(onClick = { js("pageSetup(${JSONObject.quote(pageSize)},${margins.toIntOrNull() ?: 26})"); showPageSetup = false }) { Text("Apply") } }, dismissButton = { TextButton(onClick = { showPageSetup = false }) { Text("Cancel") } })
    if (showCount) AlertDialog(onDismissRequest = { showCount = false }, title = { Text("Document statistics") }, text = { Text(countResult.replace("{", "").replace("}", "").replace(",", "\n").replace("\"", "").replace(":", ": ")) }, confirmButton = { TextButton(onClick = { showCount = false }) { Text("Done") } })
    if (showDetails) AlertDialog(onDismissRequest = { showDetails = false }, title = { Text("Document details") }, text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(title, fontWeight = FontWeight.Bold); Text("Folder: ${doc.folder}", color = Muted); Text("Created: ${DateFormat.getDateTimeInstance().format(Date(doc.createdAt))}", color = Muted); Text("Last modified: ${DateFormat.getDateTimeInstance().format(Date(doc.updatedAt))}", color = Muted); Text("Storage: On this device", color = Muted); Text("Format: DocuMaker HTML document", color = Muted) } }, confirmButton = { TextButton(onClick = { showDetails = false }) { Text("Close") } })
    if (showMove) AlertDialog(onDismissRequest = { showMove = false }, title = { Text("Move document") }, text = { OutlinedTextField(moveFolder, { moveFolder = it }, label = { Text("Folder name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { onMove(moveFolder.ifBlank { "My documents" }); showMove = false }) { Text("Move") } }, dismissButton = { TextButton(onClick = { showMove = false }) { Text("Cancel") } })
    if (showOutline) {
        val headings = Regex("(?is)<h([1-6])[^>]*>(.*?)</h[1-6]>").findAll(html).map { it.groupValues[1].toInt() to it.groupValues[2].replace("<[^>]*>".toRegex(), "").trim() }.toList()
        AlertDialog(onDismissRequest = { showOutline = false }, title = { Text("Document outline") }, text = { if (headings.isEmpty()) Text("Add Heading 1, Heading 2 or Heading 3 styles to build an outline.", color = Muted) else Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { headings.forEach { (level, text) -> Text(text.ifBlank { "Untitled heading" }, modifier = Modifier.padding(start = ((level - 1) * 13).dp), color = Ink) } } }, confirmButton = { TextButton(onClick = { showOutline = false }) { Text("Close") } })
    }
}

@Composable
private fun FormatIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) { Icon(icon, label, tint = Ink, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun InsertAction(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = AccentSoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(40.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Accent) } }
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, fontWeight = FontWeight.SemiBold, color = Ink); Text(description, fontSize = 11.sp, color = Muted) }
    }
}

private fun relativeDate(timestamp: Long): String {
    val delta = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
    return when {
        delta < 60_000 -> "just now"
        delta < 3_600_000 -> "${delta / 60_000} min ago"
        delta < 86_400_000 -> "${delta / 3_600_000} hr ago"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}