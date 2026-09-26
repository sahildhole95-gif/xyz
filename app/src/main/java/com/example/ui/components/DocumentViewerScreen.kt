package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.converter.DocxElement
import com.example.converter.DocxParser
import com.example.converter.ParsedDocx
import com.example.data.model.ConversionRecord
import com.example.ui.theme.PdfRed
import com.example.ui.theme.WordBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    record: ConversionRecord,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onPrint: () -> Unit,
    onSaveToDevice: () -> Unit
) {
    val file = remember(record.filePath) { File(record.filePath) }
    val isPdf = remember(record) { record.isWordToPdf || file.name.endsWith(".pdf", ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = record.convertedFileName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${record.formattedResultSize} • ${if (isPdf) "PDF Document" else "Word DOCX"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("viewer_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShare, modifier = Modifier.testTag("viewer_share_button")) {
                        Icon(Icons.Default.Share, contentDescription = "Share Document")
                    }
                    if (isPdf) {
                        IconButton(onClick = onPrint, modifier = Modifier.testTag("viewer_print_button")) {
                            Icon(Icons.Default.Print, contentDescription = "Print PDF")
                        }
                    }
                    IconButton(onClick = onOpen, modifier = Modifier.testTag("viewer_open_button")) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open in External App")
                    }
                    IconButton(onClick = onSaveToDevice, modifier = Modifier.testTag("viewer_export_button")) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export to Storage")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!file.exists()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("File no longer exists on device storage.", color = MaterialTheme.colorScheme.error)
                }
            } else if (isPdf) {
                PdfViewer(file = file)
            } else {
                DocxViewer(file = file)
            }
        }
    }
}

@Composable
fun PdfViewer(file: File) {
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var descriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                descriptor = pfd
                val pr = PdfRenderer(pfd)
                renderer = pr
                pageCount = pr.pageCount
                if (pr.pageCount > 0) {
                    val page = pr.openPage(0)
                    val width = page.width * 2
                    val height = page.height * 2
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    currentBitmap = bmp
                }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                renderer?.close()
                descriptor?.close()
            } catch (_: Exception) {}
        }
    }

    fun loadPage(index: Int) {
        val r = renderer ?: return
        if (index in 0 until pageCount) {
            currentPageIndex = index
            try {
                val page = r.openPage(index)
                val width = page.width * 2
                val height = page.height * 2
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                currentBitmap = bmp
            } catch (_: Exception) {}
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PdfRed)
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page navigation bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPageIndex > 0) loadPage(currentPageIndex - 1) },
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Page")
                    }

                    Text(
                        text = "Page ${currentPageIndex + 1} of $pageCount",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(
                        onClick = { if (currentPageIndex < pageCount - 1) loadPage(currentPageIndex + 1) },
                        enabled = currentPageIndex < pageCount - 1
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Page")
                    }
                }
            }

            // Rendered Page Bitmap Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                currentBitmap?.let { bmp ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                } ?: Text("Unable to preview page content")
            }
        }
    }
}

@Composable
fun DocxViewer(file: File) {
    var parsedDocx by remember { mutableStateOf<ParsedDocx?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            parsedDocx = DocxParser.parse(file)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = WordBlue)
        }
    } else {
        val docx = parsedDocx
        if (docx == null || docx.elements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No readable content found in Word document.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Document Paper Container
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = docx.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = WordBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Word Document Reader (${docx.elements.size} blocks)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(docx.elements) { element ->
                    when (element) {
                        is DocxElement.Paragraph -> {
                            val isHeading = element.isHeading
                            val fullText = element.runs.joinToString("") { it.text }
                            if (fullText.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (element.isBullet) {
                                        Text(
                                            text = "• ",
                                            color = WordBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                    Text(
                                        text = fullText,
                                        style = if (isHeading) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isHeading) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                        lineHeight = if (isHeading) 24.sp else 20.sp
                                    )
                                }
                            }
                        }
                        is DocxElement.Table -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    element.rows.forEachIndexed { rowIndex, row ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (rowIndex == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                    else Color.Transparent
                                                )
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            row.cells.forEach { cell ->
                                                Text(
                                                    text = cell.text,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is DocxElement.Image -> {
                            Image(
                                bitmap = element.bitmap.asImageBitmap(),
                                contentDescription = "Embedded Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        DocxElement.PageBreak -> {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}
