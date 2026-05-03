package com.postit.dat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.postit.dat.R;
import com.postit.dat.utils.AppConfig;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tvTitle = findViewById(R.id.tv_splash_title);
        TextView tvSub = findViewById(R.id.tv_splash_sub);

        // Animasi fade in
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);
        tvTitle.startAnimation(fadeIn);
        tvSub.startAnimation(fadeIn);

        new Handler().postDelayed(() -> {
            // Cek apakah sudah dikonfigurasi
            AppConfig config = AppConfig.getInstance(this);
            if (!config.isConfigured()) {
                startActivity(new Intent(this, SettingsActivity.class)
                    .putExtra("first_run", true));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }, 1500);
    }
}
