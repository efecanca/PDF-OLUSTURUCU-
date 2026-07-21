package com.pdfstudio.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
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
    private Button convertButton;
    private ProgressBar progressBar;

    private ActivityResultLauncher<String> pickPhotosLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectionStatus = findViewById(R.id.selectionStatus);
        resultText = findViewById(R.id.resultText);
        convertButton = findViewById(R.id.convertButton);
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
    }

    private void updateSelectionStatus() {
        if (selectedPhotos.isEmpty()) {
            selectionStatus.setText(R.string.no_photos_selected);
            convertButton.setEnabled(false);
        } else {
            selectionStatus.setText(selectedPhotos.size() + " fotoğraf seçildi");
            convertButton.setEnabled(true);
        }
        resultText.setText("");
    }

    private void convertToPdf() {
        progressBar.setVisibility(View.VISIBLE);
        convertButton.setEnabled(false);
        resultText.setText("");

        List<Uri> photosSnapshot = new ArrayList<>(selectedPhotos);

        executor.execute(() -> {
            try {
                PdfExportOptions options = new PdfExportOptions()
                        .setPageSize(PdfPageSize.A4)
                        .setOrientation(PdfExportOptions.Orientation.PORTRAIT)
                        .setFitMode(PdfExportOptions.FitMode.FILL)
                        .setJpegQuality(0.85f);

                File outFile = new File(getExternalFilesDir(null),
                        "katalog_" + System.currentTimeMillis() + ".pdf");

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
        progressBar.setVisibility(View.GONE);
        convertButton.setEnabled(true);
        resultText.setText("PDF oluşturuldu: " + outFile.getName());
        Toast.makeText(this, "PDF hazır", Toast.LENGTH_SHORT).show();
        openPdf(outFile);
    }

    private void onConvertError(Exception e) {
        progressBar.setVisibility(View.GONE);
        convertButton.setEnabled(true);
        resultText.setText("Hata: " + e.getMessage());
    }

    private void openPdf(File file) {
        Uri uri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".fileprovider", file);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
