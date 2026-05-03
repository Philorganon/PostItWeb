package com.postit.dat.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.postit.dat.R;
import com.postit.dat.adapters.DataItemAdapter;
import com.postit.dat.models.DataItem;
import com.postit.dat.utils.GitHubApi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderDetailActivity extends AppCompatActivity {

    private String folderName;
    private RecyclerView recyclerView;
    private DataItemAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private List<DataItem> items = new ArrayList<>();
    private ExecutorService executor = Executors.newFixedThreadPool(3);
    private GitHubApi githubApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_detail);

        folderName = getIntent().getStringExtra("folder_name");
        githubApi = new GitHubApi(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📁 " + folderName);
        }

        recyclerView = findViewById(R.id.recycler_items);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        tvEmpty = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DataItemAdapter(items,
            // Klik item
            item -> showItemOptions(item),
            // Tombol Terima
            item -> acceptItem(item)
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadItems);
        loadItems();
    }

    private void loadItems() {
        swipeRefresh.setRefreshing(true);
        executor.execute(() -> {
            List<DataItem> result = githubApi.getFilesInFolder(folderName);
            runOnUiThread(() -> {
                items.clear();
                items.addAll(result);
                adapter.notifyDataSetChanged();
                swipeRefresh.setRefreshing(false);
                tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void showItemOptions(DataItem item) {
        String[] options;
        if ("text".equals(item.getType())) {
            options = new String[]{"Lihat Isi", "Terima (Simpan & Hapus dari GitHub)", "Batal"};
        } else if ("image".equals(item.getType())) {
            options = new String[]{"Lihat Gambar", "Terima (Simpan & Hapus dari GitHub)", "Batal"};
        } else {
            options = new String[]{"Terima (Simpan & Hapus dari GitHub)", "Batal"};
        }

        new AlertDialog.Builder(this)
            .setTitle(item.getName())
            .setMessage("Ukuran: " + item.getFormattedSize())
            .setItems(options, (dialog, which) -> {
                if ("text".equals(item.getType()) || "image".equals(item.getType())) {
                    if (which == 0) previewItem(item);
                    else if (which == 1) confirmAccept(item);
                } else {
                    if (which == 0) confirmAccept(item);
                }
            })
            .show();
    }

    private void previewItem(DataItem item) {
        if ("image".equals(item.getType())) {
            Intent intent = new Intent(this, ImageViewerActivity.class);
            intent.putExtra("image_url", item.getDownloadUrl());
            intent.putExtra("image_name", item.getName());
            startActivity(intent);
        } else if ("text".equals(item.getType())) {
            // Download dan tampilkan teks
            executor.execute(() -> {
                byte[] data = githubApi.downloadFile(item.getDownloadUrl());
                if (data != null) {
                    String content = new String(data);
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(this)
                            .setTitle(item.getName())
                            .setMessage(content)
                            .setPositiveButton("Terima", (d, w) -> confirmAccept(item))
                            .setNegativeButton("Tutup", null)
                            .show();
                    });
                }
            });
        }
    }

    private void confirmAccept(DataItem item) {
        new AlertDialog.Builder(this)
            .setTitle("Terima File")
            .setMessage("File akan disimpan ke SDCard/Dataget/" + folderName + "/ dan dihapus dari GitHub.\n\nLanjutkan?")
            .setPositiveButton("Ya, Terima", (d, w) -> acceptItem(item))
            .setNegativeButton("Batal", null)
            .show();
    }

    private void acceptItem(DataItem item) {
        Toast.makeText(this, "Mengunduh " + item.getName() + "...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            // Download file
            byte[] data = githubApi.downloadFile(item.getDownloadUrl());
            if (data == null) {
                runOnUiThread(() -> Toast.makeText(this, "Gagal download file!", Toast.LENGTH_SHORT).show());
                return;
            }

            // Simpan ke SDCard
            File saved = githubApi.saveToSdCard(item.getFolder(), item.getName(), data);
            if (saved == null) {
                runOnUiThread(() -> Toast.makeText(this, "Gagal simpan ke SDCard!", Toast.LENGTH_SHORT).show());
                return;
            }

            // Hapus dari GitHub
            boolean deleted = githubApi.deleteFile(item.getPath(), item.getSha());

            runOnUiThread(() -> {
                if (deleted) {
                    Toast.makeText(this,
                        "✅ Tersimpan di SDCard/Dataget/" + item.getFolder() + "/" + item.getName(),
                        Toast.LENGTH_LONG).show();
                    items.remove(item);
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(this,
                        "✅ Tersimpan di SDCard tapi gagal hapus dari GitHub",
                        Toast.LENGTH_LONG).show();
                    loadItems();
                }
            });
        });
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
