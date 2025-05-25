package com.example.homework2;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MonitoredUrlDao {

    @Query("SELECT * FROM monitored_urls")
    List<MonitoredUrl> getAllForWorker();

    @Query("SELECT * FROM monitored_urls")
    LiveData<List<MonitoredUrl>> getAllLiveData();

    @Insert
    void insert(MonitoredUrl monitoredUrl);

    @Update
    void update(MonitoredUrl monitoredUrl);

    @Query("DELETE FROM monitored_urls WHERE id = :id")
    void deleteById(int id);
} 