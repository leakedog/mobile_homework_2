package com.example.homework2;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "monitored_urls")
public class MonitoredUrl {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String url;
    public String contentHash;

    public MonitoredUrl(String url, String contentHash) {
        this.url = url;
        this.contentHash = contentHash;
    }
} 