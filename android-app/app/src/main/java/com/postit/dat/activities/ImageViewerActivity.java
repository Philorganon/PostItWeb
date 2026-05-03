package com.postit.dat.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.postit.dat.R;
import com.postit.dat.utils.AppConfig;

public class ImageViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        String imageUrl = getIntent().getStringExtra("image_url");
        String imageName = getIntent().getStringExtra("image_name");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(imageName != null ? imageName : "Preview");
        }

        ImageView imageView = findViewById(R.id.image_view);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            String token = AppConfig.getInstance(this).getGithubToken();
            GlideUrl glideUrl = new GlideUrl(imageUrl,
                new LazyHeaders.Builder()
                    .addHeader("Authorization", "token " + token)
                    .build());

            Glide.with(this)
                .load(glideUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(imageView);
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
