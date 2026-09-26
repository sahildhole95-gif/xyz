package com.example.converter

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PdfPrintAdapter(private val pdfFile: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val pdi = PrintDocumentInfo.Builder(pdfFile.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback?.onLayoutFinished(pdi, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        if (destination == null) {
            callback?.onWriteFailed("No destination file descriptor")
            return
        }

        try {
            FileInputStream(pdfFile).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    input.copyTo(output)
                }
            }
            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
            } else {
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        } catch (e: Exception) {
            callback?.onWriteFailed(e.localizedMessage)
        }
    }
}
