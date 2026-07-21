package com.pdfstudio.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Fotoğraf listesini tek bir PDF dosyasına dönüştürür.
 *
 * Kullanım:
 *   PDFBoxResourceLoader.init(context) uygulama başlangıcında bir kez çağrılmış olmalı.
 *
 *   new PhotoToPdfConverter(context)
 *       .convert(photoUris, options, outputStream);
 */
public class PhotoToPdfConverter {

    private final Context context;

    public PhotoToPdfConverter(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * @param photoUris  Sayfaya eklenecek fotoğrafların Uri listesi (her biri ayrı sayfa olur)
     * @param options    Sayfa boyutu, yön, fit modu, filigran, şifre vb. ayarlar
     * @param out        PDF'in yazılacağı çıktı akışı (örn. FileOutputStream); metot kapatmaz
     */
    public void convert(List<Uri> photoUris, PdfExportOptions options, OutputStream out) throws IOException {
        if (photoUris == null || photoUris.isEmpty()) {
            throw new IllegalArgumentException("photoUris boş olamaz");
        }

        try (PDDocument document = new PDDocument()) {
            float[] pageSizePt = options.resolvedPageSizePt();
            PDRectangle pageRect = new PDRectangle(pageSizePt[0], pageSizePt[1]);

            for (Uri uri : photoUris) {
                addPhotoPage(document, uri, pageRect, options);
            }

            if (options.getOwnerPassword() != null) {
                applyProtection(document, options);
            }

            document.save(out);
        }
    }

    private void addPhotoPage(PDDocument document, Uri uri, PDRectangle pageRect, PdfExportOptions options) throws IOException {
        Bitmap bitmap = loadBitmap(uri);
        if (bitmap == null) {
            throw new IOException("Fotoğraf okunamadı: " + uri);
        }

        PDPage page = new PDPage(pageRect);
        document.addPage(page);

        PDImageXObject image = JPEGFactory.createFromImage(document, bitmap, options.getJpegQuality());

        float pageW = pageRect.getWidth();
        float pageH = pageRect.getHeight();
        float imgW = image.getWidth();
        float imgH = image.getHeight();

        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            if (options.getFitMode() == PdfExportOptions.FitMode.FILL) {
                drawFill(cs, image, pageW, pageH, imgW, imgH);
            } else {
                drawFit(cs, image, pageW, pageH, imgW, imgH, options.getMarginPt());
            }

            if (options.getWatermarkText() != null && !options.getWatermarkText().isEmpty()) {
                drawWatermark(document, cs, pageW, pageH, options);
            }
        }

        bitmap.recycle();
    }

    /** Aspect-ratio korunarak sayfaya sığdırır; boşluk kalabilir. */
    private void drawFit(PDPageContentStream cs, PDImageXObject image, float pageW, float pageH,
                          float imgW, float imgH, float marginPt) throws IOException {
        float availW = pageW - 2 * marginPt;
        float availH = pageH - 2 * marginPt;

        float scale = Math.min(availW / imgW, availH / imgH);
        float drawW = imgW * scale;
        float drawH = imgH * scale;

        float x = (pageW - drawW) / 2f;
        float y = (pageH - drawH) / 2f;

        cs.drawImage(image, x, y, drawW, drawH);
    }

    /** Aspect-ratio korunarak sayfayı tamamen doldurur; taşan kısım kırpılır (full-bleed). */
    private void drawFill(PDPageContentStream cs, PDImageXObject image, float pageW, float pageH,
                           float imgW, float imgH) throws IOException {
        float scale = Math.max(pageW / imgW, pageH / imgH);
        float drawW = imgW * scale;
        float drawH = imgH * scale;

        // Kırpma için sayfa sınırlarını clip et, sonra ortalanmış büyük görseli çiz.
        cs.saveGraphicsState();
        cs.addRect(0, 0, pageW, pageH);
        cs.clip();

        float x = (pageW - drawW) / 2f;
        float y = (pageH - drawH) / 2f;

        cs.drawImage(image, x, y, drawW, drawH);
        cs.restoreGraphicsState();
    }

    /** Sayfa ortasına, çapraz, yarı saydam metin filigranı ekler. */
    private void drawWatermark(PDDocument document, PDPageContentStream cs, float pageW, float pageH,
                                PdfExportOptions options) throws IOException {
        PDFont font = PDType1Font.HELVETICA_BOLD;
        String text = options.getWatermarkText();
        float fontSize = options.getWatermarkFontSize();
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;

        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(options.getWatermarkOpacity());
        cs.setGraphicsStateParameters(gs);

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(128, 128, 128);

        float cx = pageW / 2f;
        float cy = pageH / 2f;
        double angleRad = Math.toRadians(45);

        // Metni merkeze göre döndürüp ortalayarak yerleştir.
        float tx = (float) (cx - (textWidth / 2f) * Math.cos(angleRad));
        float ty = (float) (cy - (textWidth / 2f) * Math.sin(angleRad));

        cs.setTextMatrix(com.tom_roush.harmony.awt.geom.AffineTransform.getRotateInstance(
                angleRad, tx, ty));
        cs.showText(text);
        cs.endText();
    }

    private void applyProtection(PDDocument document, PdfExportOptions options) throws IOException {
        AccessPermission permission = new AccessPermission();
        StandardProtectionPolicy policy = new StandardProtectionPolicy(
                options.getOwnerPassword(),
                options.getUserPassword() != null ? options.getUserPassword() : "",
                permission);
        policy.setEncryptionKeyLength(128);
        document.protect(policy);
    }

    private Bitmap loadBitmap(Uri uri) throws IOException {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            return BitmapFactory.decodeStream(is);
        }
    }
}
