package com.postit.dat.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.postit.dat.R;
import com.postit.dat.adapters.FolderAdapter;
import com.postit.dat.services.PollingService;
import com.postit.dat.utils.AppConfig;
import com.postit.dat.utils.GitHubApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FolderAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private List<String> folders = new ArrayList<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private GitHubApi githubApi;

    public static final String ACTION_DATA_UPDATED = "com.postit.dat.DATA_UPDATED";

    // Receiver untuk update dari PollingService
    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadFolders();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkStoragePermission();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        githubApi = new GitHubApi(this);

        recyclerView = findViewById(R.id.recycler_folders);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FolderAdapter(folders, folderName -> {
            // Klik folder → buka detail
            Intent intent = new Intent(this, FolderDetailActivity.class);
            intent.putExtra("folder_name", folderName);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.sky_blue),
            ContextCompat.getColor(this, R.color.blue_primary)
        );
        swipeRefresh.setOnRefreshListener(this::loadFolders);

        // Request permissions
        requestPermissions();

        // Start polling service
        startPollingService();

        // Load folder
        loadFolders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(updateReceiver, new IntentFilter(ACTION_DATA_UPDATED));
        loadFolders();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
    }

    private void loadFolders() {
        swipeRefresh.setRefreshing(true);
        executor.execute(() -> {
            List<String> result = githubApi.getFolders();
            runOnUiThread(() -> {
                folders.clear();
                folders.addAll(result);
                adapter.notifyDataSetChanged();
                swipeRefresh.setRefreshing(false);

                if (folders.isEmpty()) {
                    Toast.makeText(this, "Belum ada folder di GitHub repo", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void startPollingService() {
        Intent serviceIntent = new Intent(this, PollingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                permissions.toArray(new String[0]), 100);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.menu_refresh) {
            loadFolders();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(android.net.Uri.parse(String.format("package:%s", getPackageName())));
                    startActivityForResult(intent, 123);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, 123);
                }
            }
        }
    }
}
