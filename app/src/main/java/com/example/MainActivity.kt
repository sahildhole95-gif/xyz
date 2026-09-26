package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ConverterViewModel
import com.example.ui.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize PDFBox for Android
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (_: Exception) {}

        // Check for incoming shared or opened file URI
        val incomingUri = extractIncomingUri(intent)

        setContent {
            MyApplicationTheme {
                val converterViewModel: ConverterViewModel = viewModel()
                MainAppScreen(
                    viewModel = converterViewModel,
                    initialSharedUri = incomingUri
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractIncomingUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
            }
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
    }
}
