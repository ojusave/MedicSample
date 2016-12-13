package com.neura.medicationaddon;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * This service is used for 2 purposes :
 * 1. Set the fallback to 11 am every time the phone restarts (by listening to {@link Intent#ACTION_BOOT_COMPLETED}
 * 2. Receives the fallback at 11am and handle it like a 'fake' wake up event.
 */
public class PillsService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))
            NeuraManager.getInstance().setMorningPillFallback(this);
        else
            NeuraManager.getInstance().generateNotification(this, intent.getAction());

        return START_STICKY;
    }
}
