package com.pdfstudio.pdf;

/**
 * Desteklenen sayfa boyutları. Değerler "point" cinsindendir (1 point = 1/72 inch),
 * PDFBox'ın PDRectangle birimiyle birebir uyumludur.
 */
public enum PdfPageSize {
    A4(595.28f, 841.89f),
    A5(419.53f, 595.28f),
    LETTER(612f, 792f);

    public final float widthPt;
    public final float heightPt;

    PdfPageSize(float widthPt, float heightPt) {
        this.widthPt = widthPt;
        this.heightPt = heightPt;
    }

    /** Özel (custom) boyut için mm cinsinden değer alıp point'e çevirir. */
    public static float[] customFromMm(float widthMm, float heightMm) {
        float mmToPt = 72f / 25.4f;
        return new float[]{widthMm * mmToPt, heightMm * mmToPt};
    }
}
