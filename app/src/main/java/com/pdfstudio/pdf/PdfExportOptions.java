package com.pdfstudio.pdf;

/**
 * PDF dışa aktarma ayarları. Tek bir converter çağrısı için tüm parametreleri taşır.
 */
public class PdfExportOptions {

    public enum FitMode {
        /** Aspect-ratio korunur, sayfada boşluk kalabilir. */
        FIT,
        /** Aspect-ratio korunur, taşan kısım kırpılır, sayfa tamamen dolar (full-bleed). */
        FILL
    }

    public enum Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    private PdfPageSize pageSize = PdfPageSize.A4;
    private Orientation orientation = Orientation.PORTRAIT;
    private FitMode fitMode = FitMode.FIT;

    /** JPEG sıkıştırma kalitesi 0.0 - 1.0 arası. 1.0 = kalite kaybı yok. */
    private float jpegQuality = 0.85f;

    /** null ise filigran eklenmez. Sadece metin filigranı desteklenir. */
    private String watermarkText = null;
    private float watermarkOpacity = 0.3f;
    private float watermarkFontSize = 48f;

    /** null ise şifre eklenmez. */
    private String ownerPassword = null;
    private String userPassword = null;

    /** Fit modunda sayfa kenar boşluğu (point). Fill modunda kullanılmaz. */
    private float marginPt = 0f;

    // --- getters / setters (builder-style, zincirlenebilir) ---

    public PdfPageSize getPageSize() { return pageSize; }
    public PdfExportOptions setPageSize(PdfPageSize pageSize) { this.pageSize = pageSize; return this; }

    public Orientation getOrientation() { return orientation; }
    public PdfExportOptions setOrientation(Orientation orientation) { this.orientation = orientation; return this; }

    public FitMode getFitMode() { return fitMode; }
    public PdfExportOptions setFitMode(FitMode fitMode) { this.fitMode = fitMode; return this; }

    public float getJpegQuality() { return jpegQuality; }
    public PdfExportOptions setJpegQuality(float jpegQuality) { this.jpegQuality = jpegQuality; return this; }

    public String getWatermarkText() { return watermarkText; }
    public PdfExportOptions setWatermarkText(String watermarkText) { this.watermarkText = watermarkText; return this; }

    public float getWatermarkOpacity() { return watermarkOpacity; }
    public PdfExportOptions setWatermarkOpacity(float watermarkOpacity) { this.watermarkOpacity = watermarkOpacity; return this; }

    public float getWatermarkFontSize() { return watermarkFontSize; }
    public PdfExportOptions setWatermarkFontSize(float watermarkFontSize) { this.watermarkFontSize = watermarkFontSize; return this; }

    public String getOwnerPassword() { return ownerPassword; }
    public PdfExportOptions setOwnerPassword(String ownerPassword) { this.ownerPassword = ownerPassword; return this; }

    public String getUserPassword() { return userPassword; }
    public PdfExportOptions setUserPassword(String userPassword) { this.userPassword = userPassword; return this; }

    public float getMarginPt() { return marginPt; }
    public PdfExportOptions setMarginPt(float marginPt) { this.marginPt = marginPt; return this; }

    /** Yön dikkate alınarak gerçek sayfa genişlik/yükseklik değerini döner. */
    public float[] resolvedPageSizePt() {
        float w = pageSize.widthPt;
        float h = pageSize.heightPt;
        if (orientation == Orientation.LANDSCAPE) {
            float tmp = w; w = h; h = tmp;
        }
        return new float[]{w, h};
    }
}
