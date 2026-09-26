package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Margin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.converter.PdfRenderOptions
import com.example.ui.theme.EmeraldSuccess
import java.io.File

@Composable
fun SettingsScreen(
    options: PdfRenderOptions,
    onOptionsChange: (PdfRenderOptions) -> Unit,
    onClearCache: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Converter Preferences",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Privacy Guarantee Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = EmeraldSuccess.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Security",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "100% On-Device Conversion",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your files are never uploaded to any remote server or third-party cloud. All conversions happen entirely on your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // PDF Page Format Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PDF Output Page Size",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val isA4 = options.pageWidth == 595
                val isLetter = options.pageWidth == 612 && options.pageHeight == 792
                val isLegal = options.pageHeight == 1008

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isA4,
                        onClick = { onOptionsChange(options.copy(pageWidth = 595, pageHeight = 842)) },
                        label = { Text("A4 (Standard)") },
                        modifier = Modifier.testTag("size_a4_chip")
                    )
                    FilterChip(
                        selected = isLetter,
                        onClick = { onOptionsChange(options.copy(pageWidth = 612, pageHeight = 792)) },
                        label = { Text("US Letter") },
                        modifier = Modifier.testTag("size_letter_chip")
                    )
                    FilterChip(
                        selected = isLegal,
                        onClick = { onOptionsChange(options.copy(pageWidth = 612, pageHeight = 1008)) },
                        label = { Text("Legal") },
                        modifier = Modifier.testTag("size_legal_chip")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Page Margins",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                val isNormal = options.marginLeft == 45f
                val isCompact = options.marginLeft == 25f
                val isWide = options.marginLeft == 60f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isNormal,
                        onClick = {
                            onOptionsChange(
                                options.copy(
                                    marginLeft = 45f,
                                    marginRight = 45f,
                                    marginTop = 55f,
                                    marginBottom = 55f
                                )
                            )
                        },
                        label = { Text("Normal (45pt)") },
                        modifier = Modifier.testTag("margin_normal_chip")
                    )
                    FilterChip(
                        selected = isCompact,
                        onClick = {
                            onOptionsChange(
                                options.copy(
                                    marginLeft = 25f,
                                    marginRight = 25f,
                                    marginTop = 35f,
                                    marginBottom = 35f
                                )
                            )
                        },
                        label = { Text("Compact") },
                        modifier = Modifier.testTag("margin_compact_chip")
                    )
                    FilterChip(
                        selected = isWide,
                        onClick = {
                            onOptionsChange(
                                options.copy(
                                    marginLeft = 60f,
                                    marginRight = 60f,
                                    marginTop = 70f,
                                    marginBottom = 70f
                                )
                            )
                        },
                        label = { Text("Wide") },
                        modifier = Modifier.testTag("margin_wide_chip")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Running Header & Footer switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Include Running Header",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Displays title and separator line at top of each page",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = options.includeHeader,
                        onCheckedChange = { onOptionsChange(options.copy(includeHeader = it)) },
                        modifier = Modifier.testTag("header_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Include Running Footer",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Displays page numbers (\"Page X of Y\")",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = options.includeFooter,
                        onCheckedChange = { onOptionsChange(options.copy(includeFooter = it)) },
                        modifier = Modifier.testTag("footer_switch")
                    )
                }
            }
        }

        // Storage Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Storage & Cache",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Clean temporary files generated during conversions to save space.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.fillMaxWidth().testTag("clear_cache_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clean Conversion Cache")
                }
            }
        }

        // About Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DocuConvert v1.0",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Native Word & PDF conversion suite powered by Android Graphics PDF and Apache PDFBox engine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
