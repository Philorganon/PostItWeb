package com.postit.dat.models;

import java.io.Serializable;

public class DataItem implements Serializable {
    private String name;
    private String sha;
    private String downloadUrl;
    private String path;
    private String folder;
    private long size;
    private String type; // text, image, video, file
    private boolean isNew = false;
    private long timestamp;

    public DataItem() {
        this.timestamp = System.currentTimeMillis();
    }

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isNew() { return isNew; }
    public void setNew(boolean aNew) { isNew = aNew; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFormattedSize() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024));
    }

    public String getUniqueId() {
        return folder + "/" + name;
    }
}
