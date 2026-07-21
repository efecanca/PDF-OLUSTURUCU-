package com.pdfstudio.app;

import android.app.Application;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class PdfStudioApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PDFBoxResourceLoader.init(getApplicationContext());
    }
}
