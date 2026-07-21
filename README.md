# PDF-OLUSTURUCU-
# PDF Oluşturma Modülü

## 1. Gradle bağımlılığı (app/build.gradle)

```gradle
dependencies {
    implementation 'com.tom-roush:pdfbox-android:2.0.27.0'
}
```

## 2. Application sınıfında bir kez init

```java
public class PdfStudioApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        com.tom_roush.pdfbox.util.PDFBoxResourceLoader.init(getApplicationContext());
    }
}
```

`AndroidManifest.xml` içinde `<application android:name=".PdfStudioApp" ...>` olarak tanımlanmalı.

## 3. Kullanım örneği

```java
List<Uri> photos = ...; // galeri/kamera seçimi

PdfExportOptions options = new PdfExportOptions()
        .setPageSize(PdfPageSize.A4)
        .setOrientation(PdfExportOptions.Orientation.PORTRAIT)
        .setFitMode(PdfExportOptions.FitMode.FILL)   // full-bleed, kristal.pdf tarzı
        .setJpegQuality(0.85f)
        .setWatermarkText(null)                       // filigran istenmiyorsa null
        .setOwnerPassword(null);                      // şifre istenmiyorsa null

File outFile = new File(getExternalFilesDir(null), "katalog.pdf");
try (OutputStream out = new FileOutputStream(outFile)) {
    new PhotoToPdfConverter(this).convert(photos, options, out);
}
```

## Notlar / bilinen sınırlar

- `drawWatermark` içindeki `AffineTransform` çağrısı PDFBox-Android'in kendi
  `com.tom_roush.harmony.awt.geom` paketini kullanır (Android'de `java.awt` yok).
  Gerçek cihazda ilk denemede import/metot imzasını doğrulamanı öneririm —
  PDFBox-Android sürümleri arasında bu API küçük farklar gösterebiliyor.
- Şifre koruma (`applyProtection`) `document.save()`'den **önce** çağrılmalı;
  kod bu sırayı zaten koruyor.
- `Fill` modu şu an sadece merkezden kırpıyor (crop-to-center). İleride
  "odak noktası" (örn. yüz algılama ile kırpma merkezi kaydırma) eklenebilir —
  textile-catalogs iş akışındaki yüz algılama mantığı buraya taşınabilir.
