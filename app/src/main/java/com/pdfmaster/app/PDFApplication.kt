package com.pdfmaster.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class PDFApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize PDFBox for offline document manipulation
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
