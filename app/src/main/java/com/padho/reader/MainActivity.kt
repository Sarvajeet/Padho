package com.padho.reader

import android.graphics.Bitmap
import android.graphics.Color as NativeColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Green = Color(0xFF47765B)
private val Paper = Color(0xFFF7F6F0)
private val Ink = Color(0xFF20231F)
private val Muted = Color(0xFF777C73)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        setContent { PadhoApp() }
    }
}

private fun targetCode(language: String): String = when (language) {
    "ગુજરાતી" -> TranslateLanguage.GUJARATI
    "मराठी" -> TranslateLanguage.MARATHI
    "தமிழ்" -> TranslateLanguage.TAMIL
    else -> TranslateLanguage.HINDI
}

private fun extractPageText(context: android.content.Context, uri: Uri, pageIndex: Int): String {
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Unable to open this PDF." }
        PDDocument.load(input).use { document ->
            if (pageIndex !in 0 until document.numberOfPages) return ""
            val stripper = PDFTextStripper().apply {
                startPage = pageIndex + 1
                endPage = pageIndex + 1
                sortByPosition = true
            }
            return stripper.getText(document).trim()
        }
    }
}

@Composable
private fun PadhoApp() {
    var uri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("Your PDF") }
    var language by remember { mutableStateOf("हिन्दी") }
    var page by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var translating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dark by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val translationCache = remember { mutableStateMapOf<String, String>() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            try { context.contentResolver.takePersistableUriPermission(picked, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            uri = picked
            title = picked.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Your PDF"
            page = 0
            error = null
            showOriginal = false
        }
    }

    LaunchedEffect(uri, page, language) {
        val documentUri = uri ?: return@LaunchedEffect
        loading = true
        translating = false
        error = null
        pageText = ""
        translatedText = ""
        try {
            val rendered = withContext(Dispatchers.IO) {
                context.contentResolver.openFileDescriptor(documentUri, "r")!!.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        val count = renderer.pageCount
                        if (count == 0) return@withContext Pair(null, 0)
                        renderer.openPage(page.coerceIn(0, count - 1)).use { pdfPage ->
                            val width = 1200
                            val height = (width.toFloat() * pdfPage.height / pdfPage.width).toInt().coerceAtLeast(1)
                            val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            image.eraseColor(NativeColor.WHITE)
                            pdfPage.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            Pair(image, count)
                        }
                    }
                }
            }
            bitmap = rendered.first
            pageCount = rendered.second
            val extracted = withContext(Dispatchers.IO) { extractPageText(context, documentUri, page) }
            pageText = extracted
            val key = "${documentUri}|${page}|${language}"
            val cached = translationCache[key]
            if (cached != null) translatedText = cached
            else if (extracted.isNotBlank()) {
                translating = true
                val result = withContext(Dispatchers.IO) {
                    val options = TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(targetCode(language)).build()
                    val translator: Translator = Translation.getClient(options)
                    try {
                        Tasks.await(translator.downloadModelIfNeeded())
                        Tasks.await(translator.translate(extracted))
                    } finally { translator.close() }
                }
                translatedText = result
                translationCache[key] = result
            }
        } catch (e: Exception) {
            error = e.message ?: "Couldn't read or translate this PDF page."
        } finally {
            loading = false
            translating = false
        }
    }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = if (dark) Color(0xFF171916) else Paper) {
            if (uri == null) WelcomeScreen(language, { language = it }) { picker.launch(arrayOf("application/pdf")) }
            else ReaderScreen(
                title, page, pageCount, bitmap, pageText, translatedText, loading, translating,
                error, language, dark, showOriginal,
                { if (page > 0) page-- }, { if (page + 1 < pageCount) page++ },
                { language = it; showOriginal = false }, { showOriginal = !showOriginal },
                { dark = !dark }, { picker.launch(arrayOf("application/pdf")) }
            )
        }
    }
}

@Composable
private fun WelcomeScreen(language: String, onLanguage: (String) -> Unit, onOpen: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 30.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Green), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Book, null, tint = Color.White, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("padho", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Spacer(Modifier.height(64.dp))
        Text("A world of words.", fontSize = 35.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Text("In your language.", fontSize = 35.sp, lineHeight = 42.sp, fontWeight = FontWeight.SemiBold, color = Green)
        Spacer(Modifier.height(18.dp))
        Text("Your PDFs, made easier to read. Choose a language and settle into the story.", fontSize = 16.sp, lineHeight = 25.sp, color = Muted)
        Spacer(Modifier.height(36.dp))
        Text("YOUR READING LANGUAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = Muted)
        Spacer(Modifier.height(12.dp))
        listOf(listOf("हिन्दी", "ગુજરાતી"), listOf("मराठी", "தமிழ்")).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { item ->
                    OutlinedButton(onClick = { onLanguage(item) }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (language == item) Color(0xFFE3EDE3) else Color.Transparent)) {
                        Text(item, fontSize = 16.sp, fontWeight = if (language == item) FontWeight.Bold else FontWeight.Medium, color = if (language == item) Green else Ink)
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
            Icon(Icons.Outlined.FolderOpen, null)
            Spacer(Modifier.width(10.dp))
            Text("Open a PDF", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(14.dp))
        Text("Made for focus. Built for reading.", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun ReaderScreen(
    title: String, page: Int, count: Int, bitmap: Bitmap?, pageText: String, translatedText: String,
    loading: Boolean, translating: Boolean, error: String?, language: String, dark: Boolean,
    showOriginal: Boolean, previous: () -> Unit, next: () -> Unit, onLanguage: (String) -> Unit,
    toggleOriginal: () -> Unit, toggleTheme: () -> Unit, openAnother: () -> Unit
) {
    var showLanguages by remember { mutableStateOf(false) }
    val foreground = if (dark) Color(0xFFF2F0E8) else Ink
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = openAnother) { Icon(Icons.Outlined.ChevronLeft, "Open another PDF", tint = foreground) }
            Column(Modifier.weight(1f)) {
                Text(title, color = foreground, fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 14.sp)
                Text("PADHO READER", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
            }
            IconButton(onClick = { showLanguages = !showLanguages }) { Icon(Icons.Outlined.Language, "Reading language", tint = Green) }
            TextButton(onClick = toggleTheme) { Text(if (dark) "Light" else "Dark", color = foreground) }
        }
        if (showLanguages) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("हिन्दी", "ગુજરાતી", "मराठी", "தமிழ்").forEach { item ->
                    TextButton(onClick = { onLanguage(item); showLanguages = false }, modifier = Modifier.weight(1f)) {
                        Text(item, color = if (item == language) Green else foreground, fontSize = 11.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(if (showOriginal) "Original English" else language, fontSize = 12.sp, color = Muted)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = toggleOriginal, enabled = pageText.isNotBlank()) { Text(if (showOriginal) "Show translation" else "Original", color = Green) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (dark) Color(0xFF242722) else Color.White), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator(color = Green)
                pageText.isBlank() && bitmap != null -> Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Image(bitmap.asImageBitmap(), "Original PDF page ${page + 1}", Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Fit)
                    Spacer(Modifier.height(12.dp))
                    Text("This page has no selectable text. Scanned-PDF OCR is planned for a later version.", color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp)
                }
                error != null && pageText.isBlank() -> Text(error, color = foreground, modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center)
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 26.dp)) {
                    Text(if (showOriginal || translatedText.isBlank()) pageText else translatedText, color = foreground, fontSize = 19.sp, lineHeight = 30.sp)
                    if (translating && translatedText.isBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Green)
                            Spacer(Modifier.width(10.dp))
                            Text("Preparing translation…", color = Muted, fontSize = 12.sp)
                        }
                    }
                    if (error != null && pageText.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Translation unavailable: $error", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = previous, enabled = page > 0) { Icon(Icons.Outlined.ChevronLeft, "Previous page", tint = foreground) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${page + 1} / $count", color = foreground, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(if (translating) "Preparing language model…" else "Reading in $language", color = Muted, fontSize = 11.sp)
            }
            IconButton(onClick = next, enabled = page + 1 < count) { Icon(Icons.Outlined.ChevronRight, "Next page", tint = foreground) }
        }
    }
}
