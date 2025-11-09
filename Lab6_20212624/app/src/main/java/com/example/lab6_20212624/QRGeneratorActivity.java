package com.example.lab6_20212624;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONException;
import org.json.JSONObject;

public class QRGeneratorActivity extends AppCompatActivity {

    public static final String EXTRA_PLATE = "extra_plate";
    public static final String EXTRA_LAST_MILEAGE = "extra_last_mileage";
    public static final String EXTRA_LAST_REVIEW = "extra_last_review";

    private ImageView ivQRCode;
    private TextView tvPlate, tvMileage, tvLastReview;
    private MaterialButton btnClose;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_generator);

        ivQRCode = findViewById(R.id.ivQRCode);
        tvPlate = findViewById(R.id.tvPlate);
        tvMileage = findViewById(R.id.tvMileage);
        tvLastReview = findViewById(R.id.tvLastReview);
        btnClose = findViewById(R.id.btnClose);

        String plate = getIntent().getStringExtra(EXTRA_PLATE);
        int lastMileage = getIntent().getIntExtra(EXTRA_LAST_MILEAGE, -1);
        String lastReview = getIntent().getStringExtra(EXTRA_LAST_REVIEW);

        if (plate == null) {
            Toast.makeText(this, "Falta información del vehículo", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvPlate.setText(plate);
        tvMileage.setText(lastMileage >= 0 ? String.valueOf(lastMileage) + " km" : "N/A");
        tvLastReview.setText(lastReview != null ? lastReview : "N/A");

        // generar JSON con los datos y codificar en QR (coment en espaniol)
        try {
            JSONObject obj = new JSONObject();
            obj.put("PlacaRodaje", plate);
            obj.put("UltimoKilometraje", lastMileage);
            obj.put("UlimaRevision", lastReview);

            String payload = obj.toString();

            BarcodeEncoder encoder = new BarcodeEncoder();
            BitMatrix bitMatrix = new com.google.zxing.MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 600, 600);
            Bitmap bitmap = encoder.createBitmap(bitMatrix);
            ivQRCode.setImageBitmap(bitmap);
        } catch (WriterException | JSONException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        btnClose.setOnClickListener(v -> finish());
    }
}
