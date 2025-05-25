package com.example.homework2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private MonitoredUrlViewModel monitoredUrlViewModel;
    private EditText editTextUrl;
    private MonitoredUrlAdapter adapter;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextUrl = findViewById(R.id.editTextUrl);
        Button buttonAddUrl = findViewById(R.id.buttonAddUrl);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        adapter = new MonitoredUrlAdapter(new ArrayList<>(), id -> {
            databaseExecutor.execute(() -> {
                monitoredUrlViewModel.deleteById(id);
            });
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        monitoredUrlViewModel = new ViewModelProvider(this).get(MonitoredUrlViewModel.class);
        monitoredUrlViewModel.getAllUrls().observe(this, monitoredUrls -> {
            adapter.setUrls(monitoredUrls);
        });

        buttonAddUrl.setOnClickListener(v -> {
            String url = editTextUrl.getText().toString();
            if (!url.isEmpty()) {
                // Maybe copied without http prefix, so add one
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://" + url;
                }
                MonitoredUrl newUrl = new MonitoredUrl(url, null);
                databaseExecutor.execute(() -> monitoredUrlViewModel.insert(newUrl));
                editTextUrl.setText("");
            }
        });

        requestNotificationPermission();
        startUrlMonitoringForegroundService();
    }

    private void startUrlMonitoringForegroundService() {
        Intent serviceIntent = new Intent(this, UrlMonitoringService.class);
        serviceIntent.setAction(UrlMonitoringService.ACTION_START_MONITORING);

        ContextCompat.startForegroundService(this, serviceIntent);
        Log.d("MainActivity", "Requested to start UrlMonitoringService in foreground (O+).");
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int permissionStatus = ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS_PERMISSION);
            Log.d("MainActivity", "Checking notification permission. Status: " + (permissionStatus == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "NOT GRANTED"));

            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting notification permission pop-up.");
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS_PERMISSION}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                Log.d("MainActivity", "Notification permission was already granted (or not needed for this OS version check path).");
            }
        } else {
            Log.d("MainActivity", "Notification permission not explicitly requested (OS version < TIRAMISU).");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Got permission");
            } else {
                Log.w("MainActivity", "Permission denied");
            }
        }
    }
}