package com.example.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import android.view.DragEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.converter.ConversionMode
import com.example.ui.theme.PdfRed
import com.example.ui.theme.WordBlue

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DragDropZone(
    selectedMode: ConversionMode,
    onModeChange: (ConversionMode) -> Unit,
    onFilesPicked: (List<Uri>) -> Unit,
    onSampleWordClick: () -> Unit,
    onSamplePdfClick: () -> Unit,
    onBrowseClick: () -> Unit
) {
    var isDraggingOver by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val targetCallback = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                isDraggingOver = true
            }

            override fun onEntered(event: DragAndDropEvent) {
                isDraggingOver = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDraggingOver = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDraggingOver = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDraggingOver = false
                val androidEvent = event.toAndroidDragEvent()
                val clipData = androidEvent.clipData ?: return false
                val uris = mutableListOf<Uri>()
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    if (uri != null) {
                        uris.add(uri)
                    }
                }
                if (uris.isNotEmpty()) {
                    onFilesPicked(uris)
                    return true
                }
                return false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Card with Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.doc_convert_hero_1790437517799),
                    contentDescription = "DocuConvert Hero",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC0F172A))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Instant File Converter",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Word DOCX ⇋ PDF • 100% On-Device Privacy",
                        color = Color(0xFFCBD5E1),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Mode Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedMode == ConversionMode.AUTO,
                onClick = { onModeChange(ConversionMode.AUTO) },
                label = { Text("Auto Detect", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("mode_auto_chip")
            )

            FilterChip(
                selected = selectedMode == ConversionMode.WORD_TO_PDF,
                onClick = { onModeChange(ConversionMode.WORD_TO_PDF) },
                label = { Text("Word → PDF", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Description, contentDescription = null, tint = WordBlue, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("mode_word_to_pdf_chip")
            )

            FilterChip(
                selected = selectedMode == ConversionMode.PDF_TO_WORD,
                onClick = { onModeChange(ConversionMode.PDF_TO_WORD) },
                label = { Text("PDF → Word", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PdfRed, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("mode_pdf_to_word_chip")
            )
        }

        // Interactive Drag-and-Drop Drop Zone
        val borderColor by animateColorAsState(
            targetValue = if (isDraggingOver) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            label = "borderColor"
        )
        val containerColor by animateColorAsState(
            targetValue = if (isDraggingOver) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
            label = "containerColor"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(containerColor)
                .border(
                    width = if (isDraggingOver) 2.5.dp else 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(20.dp)
                )
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        val clipDesc = event.toAndroidDragEvent().clipDescription
                        clipDesc != null && (
                            clipDesc.hasMimeType("application/pdf") ||
                            clipDesc.hasMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                            clipDesc.hasMimeType("application/msword") ||
                            clipDesc.hasMimeType("text/*") ||
                            clipDesc.hasMimeType("application/octet-stream") ||
                            clipDesc.hasMimeType("application/*")
                        )
                    },
                    target = targetCallback
                )
                .clickable { onBrowseClick() }
                .padding(vertical = 28.dp, horizontal = 20.dp)
                .testTag("drag_drop_zone"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated Upload Icon Circle
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .scale(if (isDraggingOver) pulseScale else 1f),
                    shape = CircleShape,
                    color = when (selectedMode) {
                        ConversionMode.WORD_TO_PDF -> WordBlue.copy(alpha = 0.15f)
                        ConversionMode.PDF_TO_WORD -> PdfRed.copy(alpha = 0.15f)
                        ConversionMode.AUTO -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload",
                            tint = when (selectedMode) {
                                ConversionMode.WORD_TO_PDF -> WordBlue
                                ConversionMode.PDF_TO_WORD -> PdfRed
                                ConversionMode.AUTO -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isDraggingOver) "Drop files here!" else "Drag & Drop files here",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "or tap anywhere to browse files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Supported Formats Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatBadge(label = "DOCX", color = WordBlue)
                    FormatBadge(label = "DOC", color = WordBlue.copy(alpha = 0.8f))
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = "Convert",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    FormatBadge(label = "PDF", color = PdfRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Test with Preloaded Sample Files
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Instant Test Samples",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSampleWordClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("sample_word_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = WordBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Word → PDF",
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            OutlinedButton(
                onClick = onSamplePdfClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("sample_pdf_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = PdfRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PDF → Word",
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun FormatBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
