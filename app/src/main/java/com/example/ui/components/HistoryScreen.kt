package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversionRecord
import com.example.ui.theme.PdfRed
import com.example.ui.theme.WordBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    conversions: List<ConversionRecord>,
    onSelectPreview: (ConversionRecord) -> Unit,
    onShare: (ConversionRecord) -> Unit,
    onOpen: (ConversionRecord) -> Unit,
    onToggleFavorite: (ConversionRecord) -> Unit,
    onDelete: (ConversionRecord) -> Unit,
    onClearAll: () -> Unit,
    onGoToConvert: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, WORD_TO_PDF, PDF_TO_WORD, FAVORITES
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredList by remember(conversions, searchQuery, selectedFilter) {
        derivedStateOf {
            conversions.filter { record ->
                val matchesQuery = searchQuery.isBlank() ||
                        record.convertedFileName.contains(searchQuery, ignoreCase = true) ||
                        record.originalFileName.contains(searchQuery, ignoreCase = true)

                val matchesFilter = when (selectedFilter) {
                    "WORD_TO_PDF" -> record.isWordToPdf
                    "PDF_TO_WORD" -> !record.isWordToPdf
                    "FAVORITES" -> record.isFavorite
                    else -> true
                }
                matchesQuery && matchesFilter
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All History?") },
            text = { Text("This will permanently remove all conversion logs and cached files.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search & Filter Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Conversion History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (conversions.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            Icons.Default.ClearAll,
                            contentDescription = "Clear History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by file name...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${conversions.size})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "WORD_TO_PDF",
                    onClick = { selectedFilter = "WORD_TO_PDF" },
                    label = { Text("Word → PDF", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "PDF_TO_WORD",
                    onClick = { selectedFilter = "PDF_TO_WORD" },
                    label = { Text("PDF → Word", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == "FAVORITES",
                    onClick = { selectedFilter = "FAVORITES" },
                    label = { Text("Starred", fontSize = 12.sp) }
                )
            }
        }

        // List or Empty State
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (searchQuery.isNotEmpty()) "No files match \"$searchQuery\"" else "No conversions yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Drop a Word or PDF file in the Convert tab to start.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = onGoToConvert, modifier = Modifier.testTag("go_to_convert_button")) {
                        Text("Start First Conversion", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { record ->
                    HistoryItemCard(
                        record = record,
                        onClick = { onSelectPreview(record) },
                        onShare = { onShare(record) },
                        onOpen = { onOpen(record) },
                        onToggleFavorite = { onToggleFavorite(record) },
                        onDelete = { onDelete(record) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    record: ConversionRecord,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val isWordToPdf = record.isWordToPdf
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(record.timestamp) { dateFormat.format(Date(record.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("history_item_${record.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge Icon
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isWordToPdf) PdfRed.copy(alpha = 0.12f) else WordBlue.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isWordToPdf) Icons.Default.PictureAsPdf else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isWordToPdf) PdfRed else WordBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.convertedFileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "From: ${record.originalFileName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp).testTag("fav_btn_${record.id}")
                ) {
                    Icon(
                        imageVector = if (record.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (record.isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Meta Info + Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = record.formattedResultSize,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (record.pageCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${record.pageCount} ${if (record.pageCount == 1) "page" else "pages"}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(32.dp).testTag("preview_btn_${record.id}")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp).testTag("share_btn_${record.id}")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onOpen,
                        modifier = Modifier.size(32.dp).testTag("open_btn_${record.id}")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_btn_${record.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
