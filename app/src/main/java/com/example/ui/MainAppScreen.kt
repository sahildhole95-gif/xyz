package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.ConversionRecord
import com.example.ui.components.ConversionProgressDialog
import com.example.ui.components.DocumentViewerScreen
import com.example.ui.components.DragDropZone
import com.example.ui.components.FileQueueCard
import com.example.ui.components.HistoryScreen
import com.example.ui.components.SettingsScreen
import java.io.File

enum class ScreenTab {
    CONVERT,
    HISTORY,
    VIEWER,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: ConverterViewModel,
    initialSharedUri: Uri? = null
) {
    val context = LocalContext.current
    val currentMode by viewModel.conversionMode.collectAsStateWithLifecycle()
    val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()
    val conversionProgress by viewModel.conversionProgress.collectAsStateWithLifecycle()
    val allConversions by viewModel.allConversions.collectAsStateWithLifecycle()
    val renderOptions by viewModel.renderOptions.collectAsStateWithLifecycle()
    val currentPreviewRecord by viewModel.currentPreviewRecord.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(ScreenTab.CONVERT) }
    var fileToExport by remember { mutableStateOf<ConversionRecord?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Multi-file document picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addFiles(uris, context)
        }
    }

    // Export / Save document launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri ->
        if (destinationUri != null && fileToExport != null) {
            viewModel.exportToDestination(context, fileToExport!!, destinationUri)
            fileToExport = null
        }
    }

    // Handle shared incoming URI if present
    LaunchedEffect(initialSharedUri) {
        if (initialSharedUri != null) {
            viewModel.addFiles(listOf(initialSharedUri), context)
            activeTab = ScreenTab.CONVERT
        }
    }

    // Show toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Back handling
    BackHandler(enabled = activeTab != ScreenTab.CONVERT) {
        activeTab = ScreenTab.CONVERT
    }

    // Active conversion or completion dialog
    ConversionProgressDialog(
        progress = conversionProgress,
        onDismiss = { viewModel.dismissProgress() },
        onViewResult = { record ->
            viewModel.setPreviewRecord(record)
            viewModel.dismissProgress()
            activeTab = ScreenTab.VIEWER
        },
        onShareResult = { record ->
            viewModel.shareRecord(record)
        },
        onOpenResult = { record ->
            viewModel.openRecord(record)
        },
        onSaveToDevice = { record ->
            fileToExport = record
            val mimeType = if (record.isWordToPdf) "application/pdf" else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            exportLauncher.launch(record.convertedFileName)
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (activeTab != ScreenTab.VIEWER) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "DocuConvert",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "DocuConvert",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Word DOCX ⇋ PDF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = activeTab == ScreenTab.CONVERT,
                    onClick = { activeTab = ScreenTab.CONVERT },
                    icon = {
                        if (selectedFiles.isNotEmpty()) {
                            BadgedBox(badge = { Badge { Text("${selectedFiles.size}") } }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Convert")
                            }
                        } else {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Convert")
                        }
                    },
                    label = { Text("Convert") },
                    modifier = Modifier.testTag("nav_convert_tab")
                )

                NavigationBarItem(
                    selected = activeTab == ScreenTab.HISTORY,
                    onClick = { activeTab = ScreenTab.HISTORY },
                    icon = {
                        if (allConversions.isNotEmpty()) {
                            BadgedBox(badge = { Badge { Text("${allConversions.size}") } }) {
                                Icon(Icons.Default.History, contentDescription = "History")
                            }
                        } else {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    },
                    label = { Text("History") },
                    modifier = Modifier.testTag("nav_history_tab")
                )

                NavigationBarItem(
                    selected = activeTab == ScreenTab.VIEWER,
                    onClick = { activeTab = ScreenTab.VIEWER },
                    icon = { Icon(Icons.Default.Visibility, contentDescription = "Viewer") },
                    label = { Text("Viewer") },
                    enabled = currentPreviewRecord != null,
                    modifier = Modifier.testTag("nav_viewer_tab")
                )

                NavigationBarItem(
                    selected = activeTab == ScreenTab.SETTINGS,
                    onClick = { activeTab = ScreenTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                ScreenTab.CONVERT -> {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        DragDropZone(
                            selectedMode = currentMode,
                            onModeChange = { viewModel.setMode(it) },
                            onFilesPicked = { uris -> viewModel.addFiles(uris, context) },
                            onSampleWordClick = { viewModel.convertSampleWord(context) },
                            onSamplePdfClick = { viewModel.convertSamplePdf(context) },
                            onBrowseClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/pdf",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/msword",
                                        "*/*"
                                    )
                                )
                            }
                        )

                        FileQueueCard(
                            files = selectedFiles,
                            mode = currentMode,
                            onRemoveFile = { viewModel.removeSelectedFile(it) },
                            onClearAll = { viewModel.clearSelectedFiles() },
                            onConvertClick = { viewModel.convertSelectedFiles() }
                        )
                    }
                }

                ScreenTab.HISTORY -> {
                    HistoryScreen(
                        conversions = allConversions,
                        onSelectPreview = { record ->
                            viewModel.setPreviewRecord(record)
                            activeTab = ScreenTab.VIEWER
                        },
                        onShare = { viewModel.shareRecord(it) },
                        onOpen = { viewModel.openRecord(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDelete = { viewModel.deleteConversion(it) },
                        onClearAll = { viewModel.clearAllConversions() },
                        onGoToConvert = { activeTab = ScreenTab.CONVERT }
                    )
                }

                ScreenTab.VIEWER -> {
                    val record = currentPreviewRecord
                    if (record != null) {
                        DocumentViewerScreen(
                            record = record,
                            onBack = { activeTab = ScreenTab.CONVERT },
                            onShare = { viewModel.shareRecord(record) },
                            onOpen = { viewModel.openRecord(record) },
                            onPrint = { viewModel.printRecord(record) },
                            onSaveToDevice = {
                                fileToExport = record
                                exportLauncher.launch(record.convertedFileName)
                            }
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No document selected for viewing.")
                        }
                    }
                }

                ScreenTab.SETTINGS -> {
                    SettingsScreen(
                        options = renderOptions,
                        onOptionsChange = { viewModel.updateRenderOptions(it) },
                        onClearCache = {
                            try {
                                val cacheFiles = context.cacheDir.listFiles()
                                cacheFiles?.forEach { it.deleteRecursively() }
                                Toast.makeText(context, "Conversion cache cleared", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }
}
