package com.postit.dat;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.multidex.MultiDex;
import android.content.Context;

public class PostItDatApp extends Application {

    public static final String CHANNEL_ID_NEW_DATA = "channel_new_data";
    public static final String CHANNEL_ID_SERVICE = "channel_service";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);

            // Channel untuk data baru masuk
            NotificationChannel newDataChannel = new NotificationChannel(
                CHANNEL_ID_NEW_DATA,
                "Data Baru",
                NotificationManager.IMPORTANCE_HIGH
            );
            newDataChannel.setDescription("Notifikasi ketika ada data baru dari webhook");
            nm.createNotificationChannel(newDataChannel);

            // Channel untuk foreground service
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Polling Service",
                NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Service berjalan di background untuk polling data");
            nm.createNotificationChannel(serviceChannel);
        }
    }
}
