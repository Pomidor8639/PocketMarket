package com.pocketmarket.client.installer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.pocketmarket.client.model.ModItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModInstaller {

    public interface DownloadListener {
        void onProgress(int percent, long downloadedBytes, long totalBytes);
        void onComplete(File file);
        void onError(Exception e);
    }

    private static final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static File pendingApkFile = null;
    private static ModItem pendingApkItem = null;

    public static boolean checkInstallPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return activity.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    public static void requestInstallPermission(Activity activity, File apkFile, ModItem item) {
        pendingApkFile = apkFile;
        pendingApkItem = item;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Toast.makeText(activity, "Включите разрешение на установку. После возврата файл запустится автоматически.", Toast.LENGTH_LONG).show();
            Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            settingsIntent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(settingsIntent);
        }
    }

    public static void resumePendingInstallIfPossible(Activity activity) {
        if (pendingApkFile != null && pendingApkFile.exists() && checkInstallPermission(activity)) {
            File fileToInstall = pendingApkFile;
            ModItem itemToInstall = pendingApkItem;
            pendingApkFile = null;
            pendingApkItem = null;
            install(activity, itemToInstall != null ? itemToInstall : new ModItem(), fileToInstall);
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 Б";
        if (bytes < 1024) return bytes + " Б";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f КБ", bytes / 1024.0);
        return String.format(Locale.US, "%.1f МБ", bytes / (1024.0 * 1024.0));
    }

    public static void downloadFile(Context context, String fileUrl, String targetFileName, DownloadListener listener) {
        downloadExecutor.execute(() -> {
            HttpURLConnection conn = null;
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                URL url = new URL(fileUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("User-Agent", "PocketMarket/1.0.0 (Android)");
                conn.connect();

                int responseCode = conn.getResponseCode();
                // Follow redirect if needed
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String newUrl = conn.getHeaderField("Location");
                    if (newUrl != null && !newUrl.isEmpty()) {
                        conn.disconnect();
                        url = new URL(newUrl);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(60000);
                        conn.setRequestProperty("User-Agent", "PocketMarket/1.0.0 (Android)");
                        conn.connect();
                        responseCode = conn.getResponseCode();
                    }
                }

                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != 206) {
                    throw new Exception("HTTP ошибка сервера: " + responseCode);
                }

                long fileLength = conn.getContentLengthLong();

                File downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir == null) {
                    downloadDir = new File(context.getFilesDir(), "downloads");
                }
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                File outputFile = new File(downloadDir, targetFileName);
                if (outputFile.exists()) {
                    outputFile.delete();
                }

                is = conn.getInputStream();
                fos = new FileOutputStream(outputFile);

                byte[] buffer = new byte[32768];
                long total = 0;
                int count;
                long lastUpdateTime = 0;

                while ((count = is.read(buffer)) != -1) {
                    total += count;
                    fos.write(buffer, 0, count);

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime > 80 || (fileLength > 0 && total == fileLength)) {
                        lastUpdateTime = currentTime;
                        final long curTotal = total;
                        final long curLength = fileLength;
                        final int currentPercent = curLength > 0 ? (int) (curTotal * 100 / curLength) : -1;
                        mainHandler.post(() -> listener.onProgress(currentPercent, curTotal, curLength));
                    }
                }

                fos.flush();
                final File finalFile = outputFile;
                mainHandler.post(() -> listener.onComplete(finalFile));
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError(e));
            } finally {
                try {
                    if (fos != null) fos.close();
                    if (is != null) is.close();
                    if (conn != null) conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        });
    }

    public static void install(Activity activity, ModItem item, File downloadedFile) {
        if (downloadedFile == null || !downloadedFile.exists()) {
            Toast.makeText(activity, "Файл не найден на устройстве", Toast.LENGTH_SHORT).show();
            return;
        }

        if (item != null && item.isApk()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.getPackageManager().canRequestPackageInstalls()) {
                requestInstallPermission(activity, downloadedFile, item);
                return;
            }
        }

        Uri fileUri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".provider",
                downloadedFile
        );

        if (item != null && item.isApk()) {
            installApk(activity, downloadedFile, fileUri);
        } else {
            installToMinecraft(activity, fileUri, item != null ? item.getFileType() : "mcaddon");
        }
    }

    private static void installToMinecraft(Activity activity, Uri fileUri, String fileType) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/octet-stream");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // First try launching directly with official Minecraft package
            intent.setPackage("com.mojang.minecraftpe");
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(intent);
                Toast.makeText(activity, "Импорт в Minecraft запущен!", Toast.LENGTH_LONG).show();
            } else {
                // Otherwise let the user choose app (e.g. PojavLauncher, custom Bedrock clones)
                intent.setPackage(null);
                Intent chooser = Intent.createChooser(intent, "Открыть через Minecraft");
                activity.startActivity(chooser);
            }
        } catch (Exception e) {
            Toast.makeText(activity, "Ошибка запуска Minecraft: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static void installApk(Activity activity, File downloadedFile, Uri fileUri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.getPackageManager().canRequestPackageInstalls()) {
                    requestInstallPermission(activity, downloadedFile, null);
                    return;
                }
            }

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Grant read permission to all matching activities (crucial for MIUI / HyperOS PackageInstaller!)
            List<ResolveInfo> resolveInfoList = activity.getPackageManager()
                    .queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resolveInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                activity.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            activity.startActivity(installIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "Ошибка запуска установки APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
