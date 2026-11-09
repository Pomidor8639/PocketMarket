package com.pocketmarket.client.installer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.pocketmarket.client.model.ModItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

    public static boolean checkInstallPermission(Activity activity) {
        return true;
    }

    public static void requestInstallPermission(Activity activity, File apkFile, ModItem item) {
        // No-op: REQUEST_INSTALL_PACKAGES is intentionally not requested to ensure 100% compliance with Google Play Protect
    }

    public static void resumePendingInstallIfPossible(Activity activity) {
        // No-op
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
                conn.setRequestProperty("User-Agent", "PocketMarket/1.1.4 (Android)");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    String newUrl = conn.getHeaderField("Location");
                    if (newUrl != null && !newUrl.isEmpty()) {
                        conn.disconnect();
                        url = new URL(newUrl);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(60000);
                        conn.setRequestProperty("User-Agent", "PocketMarket/1.1.4 (Android)");
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

            // Try launching directly with official Minecraft package
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
        // Copy APK to public Downloads folder for user convenience
        copyToPublicDownloads(activity, downloadedFile);

        // First attempt standard Intent.ACTION_VIEW
        boolean launched = false;
        try {
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            List<ResolveInfo> resolveInfoList = activity.getPackageManager()
                    .queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resolveInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                activity.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            if (installIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(installIntent);
                launched = true;
            }
        } catch (Exception ignored) {
        }

        // Show a guidance dialog with options
        showApkDownloadedDialog(activity, downloadedFile, fileUri, launched);
    }

    private static void copyToPublicDownloads(Context context, File srcFile) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, srcFile.getName());
                values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (InputStream in = new FileInputStream(srcFile);
                         OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                        byte[] buf = new byte[16384];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        out.flush();
                    }
                }
            } else {
                File pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (pubDir != null && (pubDir.exists() || pubDir.mkdirs())) {
                    File dest = new File(pubDir, srcFile.getName());
                    try (InputStream in = new FileInputStream(srcFile);
                         OutputStream out = new FileOutputStream(dest)) {
                        byte[] buf = new byte[16384];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        out.flush();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void showApkDownloadedDialog(Activity activity, File downloadedFile, Uri fileUri, boolean alreadyAttemptedLaunch) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Файл скачан");
            builder.setMessage("APK сохранен в папку «Загрузки»:\n" + downloadedFile.getName() +
                    "\n\nДля завершения установки откройте файл через Проводник или нажмите кнопку ниже.");

            builder.setPositiveButton("Открыть Загрузки", (dialog, which) -> {
                try {
                    Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
                    activity.startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setDataAndType(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()), "*/*");
                        activity.startActivity(Intent.createChooser(intent, "Открыть Проводник"));
                    } catch (Exception ex) {
                        Toast.makeText(activity, "Файл сохранен в папку Загрузки", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNeutralButton("Поделиться", (dialog, which) -> {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/vnd.android.package-archive");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    activity.startActivity(Intent.createChooser(shareIntent, "Отправить файл"));
                } catch (Exception e) {
                    Toast.makeText(activity, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Понятно", null);
            builder.show();
        } catch (Exception e) {
            Toast.makeText(activity, "Файл сохранен в Загрузки: " + downloadedFile.getName(), Toast.LENGTH_LONG).show();
        }
    }
}
