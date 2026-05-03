package com.postit.dat.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.postit.dat.PostItDatApp;
import com.postit.dat.R;
import com.postit.dat.activities.MainActivity;
import com.postit.dat.models.DataItem;
import com.postit.dat.receivers.AcceptReceiver;
import com.postit.dat.utils.AppConfig;
import com.postit.dat.utils.GitHubApi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PollingService extends Service {
    private static final String TAG = "PollingService";
    private static final int NOTIF_ID_SERVICE = 1;
    private static final int NOTIF_ID_DATA_BASE = 1000;

    private Handler handler;
    private Runnable pollRunnable;
    private ExecutorService executor;
    private GitHubApi githubApi;
    private AppConfig config;
    private int notifCounter = NOTIF_ID_DATA_BASE;

    // Track item yang sudah pernah ada (supaya tau item baru)
    private Set<String> knownItems = new HashSet<>();
    private boolean isFirstPoll = true;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        executor = Executors.newSingleThreadExecutor();
        githubApi = new GitHubApi(this);
        config = AppConfig.getInstance(this);

        startForeground(NOTIF_ID_SERVICE, buildServiceNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startPolling();
        return START_STICKY;
    }

    private void startPolling() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (config.isConfigured()) {
                    executor.execute(() -> doPoll());
                }
                handler.postDelayed(this, config.getPollingInterval());
            }
        };
        handler.post(pollRunnable);
    }

    private void doPoll() {
        try {
            List<String> folders = githubApi.getFolders();
            boolean hasNew = false;

            for (String folder : folders) {
                List<DataItem> items = githubApi.getFilesInFolder(folder);
                for (DataItem item : items) {
                    String id = item.getUniqueId();
                    if (!knownItems.contains(id)) {
                        knownItems.add(id);
                        if (!isFirstPoll) {
                            // Item baru! Kirim notifikasi
                            sendNewItemNotification(item);
                            hasNew = true;
                        }
                    }
                }
            }

            if (isFirstPoll) {
                isFirstPoll = false;
            }

            if (hasNew) {
                // Broadcast ke MainActivity untuk refresh
                LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(new Intent(MainActivity.ACTION_DATA_UPDATED));
            }
        } catch (Exception e) {
            Log.e(TAG, "Poll error: " + e.getMessage());
        }
    }

    private void sendNewItemNotification(DataItem item) {
        int notifId = notifCounter++;

        // Intent buka app
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(this, notifId,
            openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent tombol "Terima" di notifikasi
        Intent acceptIntent = new Intent(this, AcceptReceiver.class);
        acceptIntent.setAction(AcceptReceiver.ACTION_ACCEPT);
        acceptIntent.putExtra("item", item);
        acceptIntent.putExtra("notif_id", notifId);
        PendingIntent acceptPending = PendingIntent.getBroadcast(this, notifId,
            acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String typeEmoji = "📄";
        if ("image".equals(item.getType())) typeEmoji = "🖼️";
        else if ("video".equals(item.getType())) typeEmoji = "🎬";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, PostItDatApp.CHANNEL_ID_NEW_DATA)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(typeEmoji + " Data Baru di [" + item.getFolder() + "]")
            .setContentText(item.getName() + " (" + item.getFormattedSize() + ")")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "✅ Terima", acceptPending);

        try {
            NotificationManagerCompat.from(this).notify(notifId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission denied: " + e.getMessage());
        }
    }

    private Notification buildServiceNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, PostItDatApp.CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("PostItDat aktif")
            .setContentText("Memantau data baru dari GitHub...")
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && pollRunnable != null) {
            handler.removeCallbacks(pollRunnable);
        }
        if (executor != null) executor.shutdownNow();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
