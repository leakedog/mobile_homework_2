package com.example.homework2;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {MonitoredUrl.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract MonitoredUrlDao monitoredUrlDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "url_monitor_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
} 