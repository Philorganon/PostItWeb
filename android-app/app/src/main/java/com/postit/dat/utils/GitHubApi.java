package com.postit.dat.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.postit.dat.models.DataItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GitHubApi {
    private static final String TAG = "GitHubApi";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client;
    private AppConfig config;

    public GitHubApi(Context context) {
        client = new OkHttpClient();
        config = AppConfig.getInstance(context);
    }

    // Ambil list folder dari root repo
    public List<String> getFolders() {
        List<String> folders = new ArrayList<>();
        try {
            String url = config.getApiBase();
            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "token " + config.getGithubToken())
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    if ("dir".equals(obj.get("type").getAsString())) {
                        folders.add(obj.get("name").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getFolders error: " + e.getMessage());
        }
        return folders;
    }

    // Ambil list file di folder tertentu
    public List<DataItem> getFilesInFolder(String folderName) {
        List<DataItem> items = new ArrayList<>();
        try {
            String url = config.getApiBase() + folderName + "/";
            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "token " + config.getGithubToken())
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string();
                JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    String name = obj.get("name").getAsString();
                    String sha = obj.get("sha").getAsString();
                    String downloadUrl = obj.has("download_url") && !obj.get("download_url").isJsonNull()
                        ? obj.get("download_url").getAsString() : "";
                    String path = obj.get("path").getAsString();
                    long size = obj.has("size") ? obj.get("size").getAsLong() : 0;

                    DataItem item = new DataItem();
                    item.setName(name);
                    item.setSha(sha);
                    item.setDownloadUrl(downloadUrl);
                    item.setPath(path);
                    item.setFolder(folderName);
                    item.setSize(size);
                    item.setType(getFileType(name));
                    items.add(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getFilesInFolder error: " + e.getMessage());
        }
        return items;
    }

    // Download file dari GitHub
    public byte[] downloadFile(String downloadUrl) {
        try {
            Request request = new Request.Builder()
                .url(downloadUrl)
                .header("Authorization", "token " + config.getGithubToken())
                .get()
                .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().bytes();
            }
        } catch (Exception e) {
            Log.e(TAG, "downloadFile error: " + e.getMessage());
        }
        return null;
    }

    // Hapus file dari GitHub (setelah diterima)
    public boolean deleteFile(String path, String sha) {
        try {
            String url = "https://api.github.com/repos/"
                + config.getGithubUser() + "/"
                + config.getGithubRepo()
                + "/contents/" + path;

            JsonObject body = new JsonObject();
            body.addProperty("message", "Deleted by PostItDat app");
            body.addProperty("sha", sha);

            Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "token " + config.getGithubToken())
                .header("Accept", "application/vnd.github.v3+json")
                .delete(RequestBody.create(body.toString(), JSON))
                .build();

            Response response = client.newCall(request).execute();
            return response.isSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "deleteFile error: " + e.getMessage());
            return false;
        }
    }

    // Simpan file ke SDCard
    public File saveToSdCard(String folderName, String fileName, byte[] data) {
        try {
            File storageDir = Environment.getExternalStorageDirectory();
            File dataGetDir = new File(storageDir, "Dataget");
            if (!dataGetDir.exists()) dataGetDir.mkdirs();

            File baseDir = new File(dataGetDir, folderName);
            if (!baseDir.exists()) baseDir.mkdirs();

            File file = new File(baseDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "saveToSdCard error: " + e.getMessage());
            return null;
        }
    }

    // Tentukan tipe file berdasarkan ekstensi
    private String getFileType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".txt")) return "text";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")
            || lower.endsWith(".png") || lower.endsWith(".gif")
            || lower.endsWith(".webp")) return "image";
        if (lower.endsWith(".mp4") || lower.endsWith(".mkv")
            || lower.endsWith(".avi") || lower.endsWith(".mov")
            || lower.endsWith(".3gp")) return "video";
        return "file";
    }
}
