package com.pocketmarket.client.installer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModInstaller {

    public interface DownloadListener {
        void onProgress(int percent);
        void onComplete(File file);
        void onError(Exception e);
    }

    private static final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void downloadFile(Context context, String fileUrl, String targetFileName, DownloadListener listener) {
        downloadExecutor.execute(() -> {
            HttpURLConnection conn = null;
            InputStream is = null;
            FileOutputStream fos = null;
            try {
                URL url = new URL(fileUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP error code: " + responseCode);
                }

                int fileLength = conn.getContentLength();
                File downloadDir = context.getExternalFilesDir(null);
                if (downloadDir == null) {
                    downloadDir = context.getFilesDir();
                }

                File outputFile = new File(downloadDir, targetFileName);
                if (outputFile.exists()) {
                    outputFile.delete();
                }

                is = conn.getInputStream();
                fos = new FileOutputStream(outputFile);

                byte[] buffer = new byte[8192];
                long total = 0;
                int count;
                int lastPercent = 0;

                while ((count = is.read(buffer)) != -1) {
                    total += count;
                    fos.write(buffer, 0, count);

                    if (fileLength > 0) {
                        int currentPercent = (int) (total * 100 / fileLength);
                        if (currentPercent > lastPercent) {
                            lastPercent = currentPercent;
                            final int p = currentPercent;
                            mainHandler.post(() -> listener.onProgress(p));
                        }
                    }
                }

                fos.flush();
                mainHandler.post(() -> listener.onComplete(outputFile));
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

        if (item.isApk()) {
            installApk(activity, fileUri);
        } else {
            installToMinecraft(activity, fileUri, item.getFileType());
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

    private static void installApk(Activity activity, Uri fileUri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.getPackageManager().canRequestPackageInstalls()) {
                    Toast.makeText(activity, "Разрешите установку из этого источника в настройках", Toast.LENGTH_LONG).show();
                    Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    settingsIntent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(settingsIntent);
                    return;
                }
            }

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(installIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "Ошибка запуска установки APK: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
