package com.daasuu.gpuvideoandroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 88888;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.movie_preview).setOnClickListener(v -> {
            PlayerActivity.startActivity(MainActivity.this);
        });
        findViewById(R.id.camera_record).setOnClickListener(v -> {
            CameraSelectActivity.startActivity(MainActivity.this);
        });
        findViewById(R.id.mp4_compose).setOnClickListener(v -> {
            Mp4ComposeActivity.startActivity(MainActivity.this);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        // request camera permission if it has not been grunted.
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ) {

            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "permission has been grunted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "[WARN] permission is not grunted.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
