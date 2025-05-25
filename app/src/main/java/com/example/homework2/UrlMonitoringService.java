package com.example.homework2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class UrlMonitoringService extends Service {
    private static final String TAG = "UrlMonitoringService";
    public static final String ACTION_START_MONITORING = "com.example.homework2.ACTION_START_MONITORING";
    public static final String ACTION_STOP_MONITORING = "com.example.homework2.ACTION_STOP_MONITORING";

    private MonitoredUrlDao monitoredUrlDao;
    private volatile HandlerThread handlerThread;
    private volatile ServiceHandler serviceHandler;

    private static final int MSG_PERFORM_MONITORING = 1;
    private static final long MONITORING_INTERVAL_MS = 2000; // 2 seconds (Not really good in real use case, will be timeout)

    private static final String FOREGROUND_CHANNEL_ID = "UrlMonitorForegroundServiceChannel";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_PERFORM_MONITORING) {
                performMonitoring();
                // Reschedule the next monitoring task
                sendEmptyMessageDelayed(MSG_PERFORM_MONITORING, MONITORING_INTERVAL_MS);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        monitoredUrlDao = AppDatabase.getDatabase(this).monitoredUrlDao();

        handlerThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        serviceHandler = new ServiceHandler(handlerThread.getLooper());

        createForegroundNotificationChannel();
        Log.d(TAG, "Service Created and foreground channel created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand received action: " + (intent != null ? intent.getAction() : "null intent"));

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START_MONITORING.equals(action)) {
                startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());
                Log.d(TAG, "Service started in foreground.");

                // Stop any pending monitoring
                serviceHandler.removeMessages(MSG_PERFORM_MONITORING);

                // Send message to start new monitoring
                serviceHandler.sendEmptyMessage(MSG_PERFORM_MONITORING);
            } else if (ACTION_STOP_MONITORING.equals(action)) {
                Log.d(TAG, "Stopping foreground service and monitoring.");
                stopForeground(true); // True to remove notification
                serviceHandler.removeMessages(MSG_PERFORM_MONITORING); // Stop any pending monitoring
                stopSelf(); // Stop the service
                return START_NOT_STICKY;
            }
        }

        return START_STICKY;
    }

    private void createForegroundNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "URL Monitor Foreground Service",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("URL Monitor Active")
                .setContentText("Monitoring URLs in the background...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void performMonitoring() {
        Log.d(TAG, "Performing URL monitoring...");
        List<MonitoredUrl> urls = monitoredUrlDao.getAllForWorker();
        if (urls.isEmpty()) {
            Log.d(TAG, "No URLs to monitor.");
            return;
        }
        for (MonitoredUrl monitoredUrl : urls) {
            try {
                Log.d(TAG, "Processing URL: " + monitoredUrl.url);
                URL url = new URL(monitoredUrl.url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                reader.close();
                connection.disconnect();

                String newHash = calculateHash(content.toString());
                if (monitoredUrl.contentHash == null || (!monitoredUrl.contentHash.equals(newHash))) {
                    if (monitoredUrl.contentHash != null) {
                        Log.d(TAG, "Content changed for " + monitoredUrl.url + ", sending notification.");
                        sendUserNotification("URL Content Changed", "Content of " + monitoredUrl.url + " has changed.");
                    } else {
                        Log.d(TAG, "First fetch for " + monitoredUrl.url);
                    }
                } else {
                    Log.d(TAG, "Content hasn't changed for " + monitoredUrl.url);
                }
                monitoredUrl.contentHash = newHash;
                monitoredUrlDao.update(monitoredUrl);
            } catch (Exception e) {
                Log.e(TAG, "Error monitoring URL: " + monitoredUrl.url, e);
            }
        }
        Log.d(TAG, "URL monitoring finished for this cycle.");
    }

    private String calculateHash(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
        for (byte b : encodedhash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private void sendUserNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String userChannelId = "url_monitor_user_notifications";

        NotificationChannel channel = new NotificationChannel(userChannelId, "URL Change Alerts", NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, userChannelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        Log.d(TAG, "destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 