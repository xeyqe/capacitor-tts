package com.example.tts;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import java.util.function.Consumer;

/** Owns the engine independently of the activity and stays foreground while reading. */
public class TTSForegroundService extends Service {

    private static final String CHANNEL_ID = "tts_reading";
    private static final int NOTIFICATION_ID = 7401;
    private static final String ACTION_STOP = "com.example.tts.STOP_READING";

    private final IBinder binder = new LocalBinder();
    private TTS implementation;
    private Consumer<TTSResultCallback> pendingRead;
    private ReadingCallback reading;
    private boolean foreground;
    private PowerManager.WakeLock wakeLock;

    public class LocalBinder extends Binder {

        TTSForegroundService getService() {
            return TTSForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        implementation = new TTS(getApplicationContext());
        wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":tts-reading");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tts_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    TTS getTTS() {
        return implementation;
    }

    void startReading(Consumer<TTSResultCallback> task, TTSResultCallback callback) {
        // Keep an existing foreground session alive when replacing a read from the background.
        ReadingCallback previous = reading;
        reading = null;
        if (previous != null) previous.delegate.onError("Canceled: replaced by another read.");
        implementation.stop();
        reading = new ReadingCallback(callback);
        pendingRead = task;
        if (foreground) {
            runPendingRead();
            return;
        }
        try {
            ContextCompat.startForegroundService(this, new Intent(this, TTSForegroundService.class));
        } catch (Exception ex) {
            reading.onError("Unable to start background reading: " + ex.getLocalizedMessage());
        }
    }

    @Override
    // Reading has no fixed duration; stopForegroundReading() and onDestroy() release the lock.
    @SuppressLint("WakelockTimeout")
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Even a canceled request must satisfy startForegroundService's promotion deadline.
        try {
            Notification notification = createNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            foreground = true;
            // Keep the native segment loop running between utterances with the screen off.
            if (!wakeLock.isHeld()) wakeLock.acquire();
        } catch (Exception ex) {
            if (reading != null) reading.onError("Unable to show reading notification: " + ex.getLocalizedMessage());
            stopForegroundReading();
            return START_NOT_STICKY;
        }

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopReading();
        } else if (pendingRead != null) {
            runPendingRead();
        } else if (reading == null) {
            stopForegroundReading();
        }
        // A killed process cannot restore the in-memory texts or the JavaScript promise.
        return START_NOT_STICKY;
    }

    private void runPendingRead() {
        Consumer<TTSResultCallback> task = pendingRead;
        pendingRead = null;
        ReadingCallback current = reading;
        try {
            task.accept(current);
        } catch (Exception ex) {
            current.onError(ex.getLocalizedMessage());
        }
    }

    void stopReading() {
        pendingRead = null;
        if (reading != null) reading.onError("Canceled: reading stopped.");
        implementation.stop();
        stopForegroundReading();
    }

    private void stopForegroundReading() {
        foreground = false;
        if (wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    void cancelReading() {
        if (reading != null) stopReading();
    }

    private Notification createNotification() {
        PendingIntent stop = PendingIntent.getService(
            this,
            0,
            new Intent(this, TTSForegroundService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tts_notification)
            .setContentTitle(getString(R.string.tts_notification_title))
            .setContentText(getString(R.string.tts_notification_text))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.tts_notification_stop), stop);
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launch != null) {
            builder.setContentIntent(
                PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)
            );
        }
        return builder.build();
    }

    private class ReadingCallback implements TTSResultCallback {

        private final TTSResultCallback delegate;

        ReadingCallback(TTSResultCallback delegate) {
            this.delegate = delegate;
        }

        private boolean finish() {
            if (reading != this) return false;
            reading = null;
            pendingRead = null;
            stopForegroundReading();
            return true;
        }

        @Override
        public void onDone() {
            if (finish()) delegate.onDone();
        }

        @Override
        public void onError(String message) {
            if (finish()) delegate.onError(message);
        }

        @Override
        public void onProgress(JSObject progress) {
            if (reading == this) delegate.onProgress(progress);
        }

        @Override
        public void onArrayProgress(JSObject progress) {
            if (reading == this) delegate.onArrayProgress(progress);
        }
    }

    @Override
    public void onDestroy() {
        stopReading();
        implementation.onDestroy();
        super.onDestroy();
    }
}
