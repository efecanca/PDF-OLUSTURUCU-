package com.pdfstudio.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.pdfstudio.pdf.PdfExportOptions;
import com.pdfstudio.pdf.PdfPageSize;
import com.pdfstudio.pdf.PhotoToPdfConverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final List<Uri> selectedPhotos = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView selectionStatus;
    private TextView resultText;
    private EditText fileNameInput;
    private Button convertButton;
    private Button openButton;
    private Button shareButton;
    private View postConvertActions;
    private ProgressBar progressBar;

    private File lastGeneratedFile;

    private ActivityResultLauncher<String> pickPhotosLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectionStatus = findViewById(R.id.selectionStatus);
        resultText = findViewById(R.id.resultText);
        fileNameInput = findViewById(R.id.fileNameInput);
        convertButton = findViewById(R.id.convertButton);
        openButton = findViewById(R.id.openButton);
        shareButton = findViewById(R.id.shareButton);
        postConvertActions = findViewById(R.id.postConvertActions);
        progressBar = findViewById(R.id.progressBar);
        Button pickButton = findViewById(R.id.pickPhotosButton);

        pickPhotosLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    selectedPhotos.clear();
                    if (uris != null) selectedPhotos.addAll(uris);
                    updateSelectionStatus();
                });

        pickButton.setOnClickListener(v -> pickPhotosLauncher.launch("image/*"));
        convertButton.setOnClickListener(v -> convertToPdf());
        openButton.setOnClickListener(v -> openPdf(lastGeneratedFile));
        shareButton.setOnClickListener(v -> sharePdf(lastGeneratedFile));
    }

    private void updateSelectionStatus() {
        postConvertActions.setVisibility(View.GONE);
        if (selectedPhotos.isEmpty()) {
            selectionStatus.setText(R.string.no_photos_selected);
            convertButton.setEnabled(false);
        } else {
            selectionStatus.setText(selectedPhotos.size() + " fotoğraf seçildi");
            convertButton.setEnabled(true);
        }
        resultText.setText("");
    }

    /** Kullanıcının girdiği adı dosya sistemi için güvenli hale getirir. */
    private String resolveFileName() {
        String raw = fileNameInput.getText().toString().trim();
        if (raw.isEmpty()) {
            return "katalog_" + System.currentTimeMillis();
        }
        String safe = raw.replaceAll("[^a-zA-Z0-9ığüşöçİĞÜŞÖÇ _-]", "");
        return safe.isEmpty() ? ("katalog_" + System.currentTimeMillis()) : safe;
    }

    private void convertToPdf() {
        progressBar.setVisibility(View.VISIBLE);
        convertButton.setEnabled(false);
        postConvertActions.setVisibility(View.GONE);
        resultText.setText("");

        List<Uri> photosSnapshot = new ArrayList<>(selectedPhotos);
        String fileName = resolveFileName();

        executor.execute(() -> {
            try {
                PdfExportOptions options = new PdfExportOptions()
                        .setPageSize(PdfPageSize.A4)
                        .setOrientation(PdfExportOptions.Orientation.PORTRAIT)
                        .setFitMode(PdfExportOptions.FitMode.FILL)
                        .setJpegQuality(0.85f);

                File outFile = new File(getExternalFilesDir(null), fileName + ".pdf");

                try (FileOutputStream out = new FileOutputStream(outFile)) {
                    new PhotoToPdfConverter(this).convert(photosSnapshot, options, out);
                }

                mainHandler.post(() -> onConvertSuccess(outFile));
            } catch (IOException e) {
                mainHandler.post(() -> onConvertError(e));
            }
        });
    }

    private void onConvertSuccess(File outFile) {
        lastGeneratedFile = outFile;
        progressBar.setVisibility(View.GONE);
        convertButton.setEnabled(true);
        postConvertActions.setVisibility(View.VISIBLE);
        resultText.setText("PDF oluşturuldu: " + outFile.getName());
        Toast.makeText(this, "PDF hazır", Toast.LENGTH_SHORT).show();
    }

    private void onConvertError(Exception e) {
        progressBar.setVisibility(View.GONE);
        convertButton.setEnabled(true);
        resultText.setText("Hata: " + e.getMessage());
    }

    private Uri uriFor(File file) {
        return FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider", file);
    }

    private void openPdf(File file) {
        if (file == null) return;
        Uri uri = uriFor(file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "PDF görüntüleyecek uygulama bulunamadı", Toast.LENGTH_LONG).show();
        }
    }

    private void sharePdf(File file) {
        if (file == null) return;
        Uri uri = uriFor(file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "PDF'i paylaş"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
