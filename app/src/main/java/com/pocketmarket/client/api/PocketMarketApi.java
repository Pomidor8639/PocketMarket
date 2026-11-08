package com.pocketmarket.client.api;

import android.os.Handler;
import android.os.Looper;

import com.pocketmarket.client.model.ModItem;
import com.pocketmarket.client.model.Review;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PocketMarketApi {

    public static final String DEFAULT_BASE_URL = "http://87.120.187.254:3000";
    private static String baseUrl = DEFAULT_BASE_URL;
    private static PocketMarketApi instance;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public static void setBaseUrl(String url) {
        baseUrl = url;
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    private PocketMarketApi() {
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized PocketMarketApi getInstance() {
        if (instance == null) {
            instance = new PocketMarketApi();
        }
        return instance;
    }

    public void fetchItems(String category, String search, ApiCallback<List<ModItem>> callback) {
        executor.execute(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(getBaseUrl()).append("/api/items?");
                if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
                    urlBuilder.append("category=").append(URLEncoder.encode(category, "UTF-8")).append("&");
                }
                if (search != null && !search.trim().isEmpty()) {
                    urlBuilder.append("search=").append(URLEncoder.encode(search.trim(), "UTF-8")).append("&");
                }

                String response = executeGet(urlBuilder.toString());
                JSONObject json = new JSONObject(response);
                JSONArray array = json.getJSONArray("items");

                List<ModItem> items = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    items.add(parseModItem(array.getJSONObject(i)));
                }

                mainHandler.post(() -> callback.onSuccess(items));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void fetchItem(String id, ApiCallback<ModItem> callback) {
        executor.execute(() -> {
            try {
                String url = getBaseUrl() + "/api/items/" + URLEncoder.encode(id, "UTF-8");
                String response = executeGet(url);
                ModItem item = parseModItem(new JSONObject(response));
                mainHandler.post(() -> callback.onSuccess(item));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void recordDownload(String id, ApiCallback<String> callback) {
        executor.execute(() -> {
            try {
                String url = getBaseUrl() + "/api/items/" + URLEncoder.encode(id, "UTF-8") + "/download";
                String response = executePost(url, "{}");
                JSONObject json = new JSONObject(response);
                String downloadUrl = json.optString("downloadUrl");
                if (downloadUrl.isEmpty()) {
                    downloadUrl = getBaseUrl() + json.optString("fileUrl");
                }
                final String finalUrl = downloadUrl;
                mainHandler.post(() -> callback.onSuccess(finalUrl));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void submitRating(String id, double rating, String comment, String user, ApiCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                String url = getBaseUrl() + "/api/items/" + URLEncoder.encode(id, "UTF-8") + "/rate";
                JSONObject body = new JSONObject();
                body.put("rating", rating);
                body.put("review", comment);
                body.put("user", user);

                executePost(url, body.toString());
                mainHandler.post(() -> callback.onSuccess(true));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private String executeGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP error code: " + code);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private String executePost(String urlString, String jsonBody) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP POST error code: " + code);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    public static ModItem parseModItem(JSONObject json) {
        ModItem item = new ModItem();
        item.setId(json.optString("id"));
        item.setTitle(json.optString("title"));
        item.setCategory(json.optString("category"));
        item.setCategoryName(json.optString("categoryName"));
        item.setFileType(json.optString("fileType"));
        item.setVersion(json.optString("version"));
        item.setMcVersion(json.optString("mcVersion"));
        item.setAuthor(json.optString("author"));
        item.setFileSize(json.optString("fileSize"));
        item.setFileSizeBytes(json.optLong("fileSizeBytes"));
        item.setDownloads(json.optInt("downloads"));
        item.setRating(json.optDouble("rating", 5.0));
        item.setRatingsCount(json.optInt("ratingsCount"));
        item.setShortDescription(json.optString("shortDescription"));
        item.setDescription(json.optString("description"));
        item.setIcon(json.optString("icon"));
        item.setFileUrl(json.optString("fileUrl"));
        item.setInstallType(json.optString("installType"));
        item.setPackageName(json.optString("packageName"));
        item.setChangelog(json.optString("changelog"));

        JSONArray reviewsArray = json.optJSONArray("reviews");
        if (reviewsArray != null) {
            List<Review> reviews = new ArrayList<>();
            for (int i = 0; i < reviewsArray.length(); i++) {
                JSONObject r = reviewsArray.optJSONObject(i);
                if (r != null) {
                    reviews.add(new Review(
                            r.optString("user"),
                            r.optDouble("rating", 5.0),
                            r.optString("comment"),
                            r.optString("date")
                    ));
                }
            }
            item.setReviews(reviews);
        }

        return item;
    }
}
