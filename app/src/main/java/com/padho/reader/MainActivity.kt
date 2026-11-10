package com.padho.reader

import android.graphics.Bitmap
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.nl.translate.TranslateLanguage
import com.padho.reader.data.TranslationCache
import com.padho.reader.data.PdfPageTextExtractor
import com.padho.reader.data.PdfDocumentSession
import com.padho.reader.translation.MlKitTranslationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import java.security.MessageDigest

private val Green = Color(0xFF47765B)
private val Paper = Color(0xFFF7F6F0)
private val Ink = Color(0xFF20231F)
private val Muted = Color(0xFF777C73)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        renderApp(resolveIncomingPdf(intent))
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderApp(resolveIncomingPdf(intent))
    }

    private fun renderApp(incomingPdf: Uri?) {
        setContent { PadhoApp(incomingPdf) }
    }

    private fun resolveIncomingPdf(source: android.content.Intent?): Uri? {
        if (source == null) return null
        return source.data ?: if (source.action == android.content.Intent.ACTION_SEND) {
            @Suppress("DEPRECATION")
            source.getParcelableExtra(android.content.Intent.EXTRA_STREAM) as? Uri
        } else null
    }
}

private fun targetCode(language: String): String = when (language) {
    "ગુજરાતી" -> TranslateLanguage.GUJARATI
    "मराठी" -> TranslateLanguage.MARATHI
    "தமிழ்" -> TranslateLanguage.TAMIL
    else -> TranslateLanguage.HINDI
}

private fun translationCacheKey(uri: Uri, pageIndex: Int, language: String, sourceText: String, engineVersion: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(sourceText.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    return "$engineVersion|pdftext-v2|en|$language|$uri|$pageIndex|$digest"
}

@Composable
private fun PadhoApp(incomingPdf: Uri?) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("padho_preferences", android.content.Context.MODE_PRIVATE) }
    val savedUri = remember {
        preferences.getString("last_pdf_uri", null)?.let { saved -> runCatching { Uri.parse(saved) }.getOrNull() }
    }
    val initialUri = incomingPdf ?: savedUri
    var uri by remember { mutableStateOf(initialUri) }
    var title by remember {
        mutableStateOf(initialUri?.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Your PDF")
    }
    var language by remember { mutableStateOf(preferences.getString("reading_language", "हिन्दी") ?: "हिन्दी") }
    var page by remember { mutableIntStateOf(if (incomingPdf == null) preferences.getInt("last_page_index", 0).coerceAtLeast(0) else 0) }
    var hasPersistableUriAccess by remember { mutableStateOf(incomingPdf == null && savedUri != null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var translating by remember { mutableStateOf(false) }
    var translationStatus by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var retryNonce by remember { mutableIntStateOf(0) }
    var dark by remember { mutableStateOf(preferences.getBoolean("dark_theme", false)) }
    var fontSizeSp by remember { mutableFloatStateOf(preferences.getFloat("reader_font_size_sp", 19f).coerceIn(15f, 29f)) }
    var showOriginal by remember { mutableStateOf(false) }
    LaunchedEffect(language) { preferences.edit().putString("reading_language", language).apply() }
    LaunchedEffect(dark) { preferences.edit().putBoolean("dark_theme", dark).apply() }
    LaunchedEffect(fontSizeSp) { preferences.edit().putFloat("reader_font_size_sp", fontSizeSp).apply() }
    LaunchedEffect(incomingPdf) {
        incomingPdf?.let { incoming ->
            hasPersistableUriAccess = try {
                context.contentResolver.takePersistableUriPermission(
                    incoming, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                true
            } catch (_: Exception) {
                // Some shared URIs are temporary. Do not save them for a future launch.
                false
            }
            if (hasPersistableUriAccess) {
                preferences.edit()
                    .putString("last_pdf_uri", incoming.toString())
                    .putInt("last_page_index", 0)
                    .apply()
            } else {
                preferences.edit().remove("last_pdf_uri").remove("last_page_index").apply()
            }
        }
    }
    LaunchedEffect(uri, page) {
        if (uri != null && hasPersistableUriAccess) {
            preferences.edit()
                .putString("last_pdf_uri", uri.toString())
                .putInt("last_page_index", page)
                .apply()
        }
    }
    val translationCache = remember { TranslationCache(context) }
    val cacheScope = rememberCoroutineScope()
    val translationProvider = remember { MlKitTranslationProvider() }
    val pdfSession = remember { PdfDocumentSession(context) }
    DisposableEffect(pdfSession) {
        onDispose { pdfSession.close() }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            hasPersistableUriAccess = try {
                context.contentResolver.takePersistableUriPermission(
                    picked, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                true
            } catch (_: Exception) {
                false
            }
            if (hasPersistableUriAccess) {
                preferences.edit()
                    .putString("last_pdf_uri", picked.toString())
                    .putInt("last_page_index", 0)
                    .apply()
            } else {
                preferences.edit().remove("last_pdf_uri").remove("last_page_index").apply()
            }
            uri = picked
            title = picked.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Your PDF"
            page = 0
            error = null
            showOriginal = false
        }
    }

    LaunchedEffect(uri, page, language, retryNonce) {
        val documentUri = uri ?: return@LaunchedEffect
        loading = true
        translating = false
        translationStatus = "Preparing page…"
        error = null
        var translationStarted = false
        pageText = ""
        translatedText = ""
        try {
            val rendered = withContext(Dispatchers.IO) {
                val count = pdfSession.pageCount(documentUri)
                val validPage = if (count == 0) 0 else page.coerceIn(0, count - 1)
                val image = if (count == 0) null else pdfSession.renderPage(documentUri, validPage)
                Triple(image, count, validPage)
            }
            bitmap = rendered.first
            pageCount = rendered.second
            if (page != rendered.third) page = rendered.third
            val activePage = rendered.third
            val extracted = withContext(Dispatchers.IO) { PdfPageTextExtractor.extractPageText(context, documentUri, activePage, pdfSession) }
            pageText = extracted
            val key = translationCacheKey(documentUri, activePage, language, extracted, translationProvider.cacheVersion)
            val cached = withContext(Dispatchers.IO) { translationCache.get(key) }
            if (cached != null) translatedText = cached
            else if (extracted.isNotBlank()) {
                translating = true
                translationStarted = true
                translationStatus = "Checking language model…"
                val result = withContext(Dispatchers.IO) {
                    translationProvider.translate(extracted, targetCode(language)) { status ->
                        withContext(Dispatchers.Main) { translationStatus = status }
                    }
                }
                translatedText = result
                withContext(Dispatchers.IO) { translationCache.put(key, result) }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            error = if (translationStarted) TranslationErrorMessages.friendly(e) else "Couldn't open this PDF page. Check that the file is still available, then try again."
        } finally {
            // A cancelled, outdated page request must not clear the new page's loading state.
            if (currentCoroutineContext().isActive) {
                loading = false
                translating = false
                translationStatus = ""
            }
        }

        // Warm the next page in the background so forward navigation feels instant.
        val nextPage = page + 1
        if (nextPage < pageCount) {
            launch(Dispatchers.IO) {
                try {
                    val nextText = PdfPageTextExtractor.extractPageText(context, documentUri, nextPage, pdfSession)
                    val nextKey = translationCacheKey(documentUri, nextPage, language, nextText, translationProvider.cacheVersion)
                    if (nextText.isNotBlank() && translationCache.get(nextKey) == null) {
                        val nextTranslation = translationProvider.translate(nextText, targetCode(language))
                        currentCoroutineContext().ensureActive()
                        translationCache.put(nextKey, nextTranslation)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Prefetch is opportunistic. The visible page remains the priority.
                }
            }
        }
    }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = if (dark) Color(0xFF171916) else Paper) {
            if (uri == null) WelcomeScreen(language, { language = it }) { picker.launch(arrayOf("application/pdf")) }
            else ReaderScreen(
                title, page, pageCount, bitmap, pageText, translatedText, loading, translating,
                error, language, dark, showOriginal, fontSizeSp, translationStatus,
                { if (page > 0) page-- }, { if (page + 1 < pageCount) page++ },
                { language = it; showOriginal = false }, { showOriginal = !showOriginal },
                { dark = !dark }, { picker.launch(arrayOf("application/pdf")) }, { retryNonce++ },
                { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(15f) },
                { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(29f) },
                { cacheScope.launch(Dispatchers.IO) { translationCache.clearAll() } }
            )
        }
    }
}

@Composable
private fun WelcomeScreen(language: String, onLanguage: (String) -> Unit, onOpen: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 28.dp, vertical = 30.dp)) {
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
    showOriginal: Boolean, fontSizeSp: Float, translationStatus: String, previous: () -> Unit, next: () -> Unit, onLanguage: (String) -> Unit,
    toggleOriginal: () -> Unit, toggleTheme: () -> Unit, openAnother: () -> Unit, retry: () -> Unit,
    decreaseFont: () -> Unit, increaseFont: () -> Unit, clearTranslations: () -> Unit
) {
    var showLanguages by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val foreground = if (dark) Color(0xFFF2F0E8) else Ink
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = openAnother) { Icon(Icons.Outlined.ChevronLeft, "Open another PDF", tint = foreground) }
            Column(Modifier.weight(1f)) {
                Text(title, color = foreground, fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 14.sp)
                Text("PADHO READER", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp)
            }
            IconButton(onClick = { showLanguages = !showLanguages }) { Icon(Icons.Outlined.Language, "Reading language", tint = Green) }
            TextButton(onClick = decreaseFont, enabled = fontSizeSp > 15f) { Text("A−", color = foreground) }
            TextButton(onClick = increaseFont, enabled = fontSizeSp < 29f) { Text("A+", color = foreground) }
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
            TextButton(onClick = { showClearCacheDialog = true }) {
                Text("Clear saved translations", color = Muted, fontSize = 12.sp)
            }
        }
        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text("Clear saved translations?") },
                text = { Text("This removes cached translations for all PDFs on this device. Your original PDFs and reading preferences will remain unchanged.") },
                confirmButton = {
                    TextButton(onClick = {
                        clearTranslations()
                        showClearCacheDialog = false
                    }) { Text("Clear translations", color = Green) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
                }
            )
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
                    Text("No readable text was detected on this page. It may be a low-quality scan or an unsupported layout.", color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp)
                }
                error != null && pageText.isBlank() -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(error, color = foreground, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = retry, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Try again") }
                }
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 26.dp)) {
                    Text(
                        if (showOriginal || translatedText.isBlank()) pageText else translatedText,
                        color = foreground,
                        style = TextStyle(
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.72f).sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    if (translating && translatedText.isBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Green)
                            Spacer(Modifier.width(10.dp))
                            Text(translationStatus.ifBlank { "Preparing translation…" }, color = Muted, fontSize = 12.sp)
                        }
                    }
                    if (error != null && pageText.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Translation unavailable: $error", color = Muted, fontSize = 12.sp)
                        TextButton(onClick = retry) { Text("Try again", color = Green) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = previous, enabled = page > 0) { Icon(Icons.Outlined.ChevronLeft, "Previous page", tint = foreground) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${page + 1} / $count", color = foreground, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(if (translating) translationStatus.ifBlank { "Preparing translation…" } else "Reading in $language", color = Muted, fontSize = 11.sp)
            }
            IconButton(onClick = next, enabled = page + 1 < count) { Icon(Icons.Outlined.ChevronRight, "Next page", tint = foreground) }
        }
    }
}
