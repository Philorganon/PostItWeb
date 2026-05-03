package com.postit.dat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.postit.dat.R;
import com.postit.dat.utils.AppConfig;

public class SettingsActivity extends AppCompatActivity {

    private EditText etToken, etUser, etRepo;
    private boolean tokenVisible = false;
    private AppConfig config;
    private boolean firstRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        firstRun = getIntent().getBooleanExtra("first_run", false);
        config = AppConfig.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(firstRun ? "Setup Awal" : "Pengaturan");
            if (!firstRun) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etToken = findViewById(R.id.et_github_token);
        etUser = findViewById(R.id.et_github_user);
        etRepo = findViewById(R.id.et_github_repo);
        Button btnSave = findViewById(R.id.btn_save_settings);
        ImageButton btnToggleToken = findViewById(R.id.btn_toggle_token);

        // Load existing config
        etToken.setText(config.getGithubToken());
        etUser.setText(config.getGithubUser());
        etRepo.setText(config.getGithubRepo());

        // Toggle visibility token
        btnToggleToken.setOnClickListener(v -> {
            tokenVisible = !tokenVisible;
            if (tokenVisible) {
                etToken.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnToggleToken.setImageResource(R.drawable.ic_eye_off);
            } else {
                etToken.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnToggleToken.setImageResource(R.drawable.ic_eye);
            }
            etToken.setSelection(etToken.getText().length());
        });

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void saveSettings() {
        String token = etToken.getText().toString().trim();
        String user = etUser.getText().toString().trim();
        String repo = etRepo.getText().toString().trim();

        if (token.isEmpty() || user.isEmpty() || repo.isEmpty()) {
            Toast.makeText(this, "Semua field wajib diisi!", Toast.LENGTH_SHORT).show();
            return;
        }

        config.setGithubToken(token);
        config.setGithubUser(user);
        config.setGithubRepo(repo);

        Toast.makeText(this, "✅ Pengaturan tersimpan!", Toast.LENGTH_SHORT).show();

        if (firstRun) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
