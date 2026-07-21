package com.pdfstudio.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Şimdilik yer tutucu ekran. Fotoğraf seçme + PDF dönüştürme akışı
 * (PhotoToPdfConverter kullanılarak) bir sonraki adımda buraya bağlanacak.
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText(R.string.app_name);
        tv.setTextSize(20f);
        tv.setPadding(48, 96, 48, 48);
        setContentView(tv);
    }
}
