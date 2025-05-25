package com.example.homework2;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonitoredUrlViewModel extends AndroidViewModel {

    private MonitoredUrlDao monitoredUrlDao;
    private LiveData<List<MonitoredUrl>> allUrls;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();


    public MonitoredUrlViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        monitoredUrlDao = db.monitoredUrlDao();
        allUrls = monitoredUrlDao.getAllLiveData();
    }

    LiveData<List<MonitoredUrl>> getAllUrls() {
        return allUrls;
    }

    public void insert(MonitoredUrl url) {
        databaseExecutor.execute(() -> {
            monitoredUrlDao.insert(url);
        });
    }

    public void deleteById(int id) {
        databaseExecutor.execute(() -> {
            monitoredUrlDao.deleteById(id);
        });
    }
} 