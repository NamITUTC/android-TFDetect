package org.tensorflow.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

public class MainActivity extends Activity implements View.OnClickListener {
    private Button btnSource;
    private Button btnCamera;
    private ProgressBar prLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
        btnCamera = findViewById(R.id.btn_camera);
        btnSource = findViewById(R.id.btn_source);
        prLoad = findViewById(R.id.pr_load);
        btnSource.setOnClickListener(this);
        btnCamera.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_camera:
                prLoad.setVisibility(View.VISIBLE);
                startActivity(new Intent(this, DetectorActivity.class));

                break;
            case R.id.btn_source:
                prLoad.setVisibility(View.VISIBLE);
                startActivity(new Intent(this, DetectSourceActivity.class));

                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        prLoad.setVisibility(View.GONE);
    }
}
