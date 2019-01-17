package com.daasuu.gpuvideoandroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CameraSelectActivity extends AppCompatActivity {

    public static void startActivity(Activity activity) {
        Intent intent = new Intent(activity, CameraSelectActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        findViewById(R.id.portrate).setOnClickListener(v -> {
            PortraitCameraActivity.startActivity(CameraSelectActivity.this);
        });
        findViewById(R.id.landscape).setOnClickListener(v -> {
            LandscapeCameraActivity.startActivity(CameraSelectActivity.this);
        });
        findViewById(R.id.square).setOnClickListener(v -> {
            SquareCameraActivity.startActivity(CameraSelectActivity.this);
        });

    }
}
