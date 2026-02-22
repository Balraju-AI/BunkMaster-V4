package com.example.bunkmaster;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private TextView tvName, tvRoll, tvStats, tvPercent;
    private SharedPreferences prefs;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("BunkMasterPrefs", MODE_PRIVATE);
        String name = prefs.getString("student_name", "");

        // V4: Force onboarding if student details are missing
        if (name.isEmpty()) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        // Initialize UI components
        tvName = findViewById(R.id.tvStudentName);
        tvRoll = findViewById(R.id.tvRollNo);
        tvStats = findViewById(R.id.tvAttendanceStats);
        tvPercent = findViewById(R.id.tvPercentage);

        tvName.setText(name);
        tvRoll.setText(prefs.getString("roll_no", ""));

        // Setup Scanning Buttons
        findViewById(R.id.btnScanTimetable).setOnClickListener(v -> openCamera());
        findViewById(R.id.btnGalleryUpload).setOnClickListener(v -> openGallery());

        // Swipeable Timetable Setup
        viewPager = findViewById(R.id.viewPager);
        TimetableAdapter adapter = new TimetableAdapter(this);
        viewPager.setAdapter(adapter);

        refreshUI();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 101);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 102);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            try {
                Bitmap bitmap;
                if (requestCode == 101) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                } else {
                    Uri uri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                }
                processTimetable(bitmap);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void processTimetable(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image).addOnSuccessListener(text -> {
            Toast.makeText(this, "KPRIT Timetable Processed!", Toast.LENGTH_SHORT).show();
            updateAttendance();
        });
    }

    private void updateAttendance() {
        // Incrementing total count for KPRIT classes
        int attended = prefs.getInt("attended_count", 21);
        int total = prefs.getInt("total_classes", 28);
        total++;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("total_classes", total);
        editor.apply();
        refreshUI();
    }

    private void refreshUI() {
        // Using your 21/28 data as the baseline
        int attended = prefs.getInt("attended_count", 21);
        int total = prefs.getInt("total_classes", 28);

        if (tvStats != null) tvStats.setText(attended + " / " + total);

        float p = ((float) attended / total) * 100;
        if (tvPercent != null) tvPercent.setText(String.format("%.1f%%", p));
    }
}