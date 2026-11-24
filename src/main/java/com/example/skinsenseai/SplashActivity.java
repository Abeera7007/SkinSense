package com.example.skinsenseai;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

/** Branded entry screen shown briefly before the home dashboard. */
public class SplashActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable openHome = () -> {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(openHome, 1400);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openHome);
        super.onDestroy();
    }
}
