package com.postit.dat.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import com.postit.dat.models.DataItem;
import com.postit.dat.utils.GitHubApi;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AcceptReceiver extends BroadcastReceiver {
    public static final String ACTION_ACCEPT = "com.postit.dat.ACTION_ACCEPT";
    private static final String TAG = "AcceptReceiver";
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_ACCEPT.equals(intent.getAction())) return;

        DataItem item = (DataItem) intent.getSerializableExtra("item");
        int notifId = intent.getIntExtra("notif_id", -1);

        if (item == null) return;

        // Tutup notifikasi
        if (notifId != -1) {
            NotificationManagerCompat.from(context).cancel(notifId);
        }

        GitHubApi api = new GitHubApi(context);

        executor.execute(() -> {
            // Download
            byte[] data = api.downloadFile(item.getDownloadUrl());
            if (data == null) {
                Log.e(TAG, "Gagal download: " + item.getName());
                return;
            }

            // Simpan ke SDCard
            File saved = api.saveToSdCard(item.getFolder(), item.getName(), data);
            if (saved == null) {
                Log.e(TAG, "Gagal simpan ke SDCard: " + item.getName());
                return;
            }

            // Hapus dari GitHub
            boolean deleted = api.deleteFile(item.getPath(), item.getSha());
            Log.d(TAG, "Accept: " + item.getName() + " | deleted=" + deleted);
        });
    }
}
