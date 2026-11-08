package com.pocketmarket.client;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.pocketmarket.client.api.PocketMarketApi;
import com.pocketmarket.client.installer.ModInstaller;
import com.pocketmarket.client.model.ModItem;
import com.pocketmarket.client.utils.FontUtils;

import java.io.File;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_MOD_ITEM = "extra_mod_item";

    private ModItem modItem;

    private ImageView btnBack;
    private TextView tvToolbarTitle;

    private LinearLayout detailHeaderCard;
    private ImageView ivDetailIcon;
    private TextView tvDetailTitle;
    private TextView tvDetailAuthor;
    private TextView tvDetailCategoryBadge;

    private TextView tvDetailDownloads;
    private TextView tvDetailSize;
    private TextView tvDetailMcVersionSpec;

    private Button btnDetailInstall;
    private ProgressBar detailProgressBar;
    private TextView tvDownloadStatus;

    private TextView tvDetailMcVersion;
    private TextView tvDetailInstallType;
    private TextView tvDetailDescription;

    private LinearLayout changelogCard;
    private TextView tvDetailChangelog;

    private boolean isDownloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        FontUtils.applyMinecraftFont(findViewById(android.R.id.content));

        modItem = (ModItem) getIntent().getSerializableExtra(EXTRA_MOD_ITEM);
        if (modItem == null) {
            Toast.makeText(this, "Ошибка загрузки элемента", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        detailHeaderCard = findViewById(R.id.detailHeaderCard);
        ivDetailIcon = findViewById(R.id.ivDetailIcon);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailAuthor = findViewById(R.id.tvDetailAuthor);
        tvDetailCategoryBadge = findViewById(R.id.tvDetailCategoryBadge);

        tvDetailDownloads = findViewById(R.id.tvDetailDownloads);
        tvDetailSize = findViewById(R.id.tvDetailSize);
        tvDetailMcVersionSpec = findViewById(R.id.tvDetailMcVersionSpec);

        btnDetailInstall = findViewById(R.id.btnDetailInstall);
        detailProgressBar = findViewById(R.id.detailProgressBar);
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus);

        tvDetailMcVersion = findViewById(R.id.tvDetailMcVersion);
        tvDetailInstallType = findViewById(R.id.tvDetailInstallType);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);

        changelogCard = findViewById(R.id.changelogCard);
        tvDetailChangelog = findViewById(R.id.tvDetailChangelog);
    }

    private void bindData() {
        tvToolbarTitle.setText(modItem.getTitle());
        tvDetailTitle.setText(modItem.getTitle());
        tvDetailAuthor.setText(getString(R.string.author_prefix) + modItem.getAuthor());

        tvDetailDownloads.setText(formatDownloads(modItem.getDownloads()));
        tvDetailSize.setText(modItem.getFileSize());
        if (tvDetailMcVersionSpec != null) {
            tvDetailMcVersionSpec.setText(modItem.getMcVersion());
        }

        tvDetailMcVersion.setText("Minecraft Bedrock " + modItem.getMcVersion());
        tvDetailDescription.setText(modItem.getDescription());

        boolean isApk = modItem.isApk();
        boolean isTexture = "textures".equalsIgnoreCase(modItem.getCategory());

        btnDetailInstall.setBackgroundResource(R.drawable.btn_red);

        if (isApk) {
            detailHeaderCard.setBackgroundResource(R.drawable.bg_card_apk);
            ivDetailIcon.setImageResource(R.drawable.apk_install_48);
            ivDetailIcon.setColorFilter(null);
            tvDetailCategoryBadge.setText("APK МОД (.APK)");
            tvDetailCategoryBadge.setTextColor(ContextCompat.getColor(this, R.color.mc_gold));
            btnDetailInstall.setText(R.string.download_apk);
            tvDetailInstallType.setText("Тип установки: Системный установщик Android (PackageInstaller)");
        } else if (isTexture) {
            detailHeaderCard.setBackgroundResource(R.drawable.bg_card_black);
            ivDetailIcon.setImageResource(R.drawable.ic_palette);
            ivDetailIcon.setColorFilter(ContextCompat.getColor(this, R.color.mc_aqua));
            tvDetailCategoryBadge.setText("ТЕКСТУРПАК (.MCPACK)");
            tvDetailCategoryBadge.setTextColor(ContextCompat.getColor(this, R.color.mc_aqua));
            btnDetailInstall.setText(R.string.install);
            tvDetailInstallType.setText("Тип установки: Авто-импорт в Minecraft через Intent");
        } else {
            detailHeaderCard.setBackgroundResource(R.drawable.bg_card_black);
            ivDetailIcon.setImageResource(R.drawable.ic_extension);
            ivDetailIcon.setColorFilter(ContextCompat.getColor(this, R.color.mc_red));
            tvDetailCategoryBadge.setText("МОД (.MCADDON)");
            tvDetailCategoryBadge.setTextColor(ContextCompat.getColor(this, R.color.mc_red));
            btnDetailInstall.setText(R.string.install);
            tvDetailInstallType.setText("Тип установки: Авто-импорт в Minecraft через Intent");
        }

        if (modItem.getChangelog() != null && !modItem.getChangelog().trim().isEmpty()) {
            changelogCard.setVisibility(View.VISIBLE);
            tvDetailChangelog.setText(modItem.getChangelog());
        } else {
            changelogCard.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnDetailInstall.setOnClickListener(v -> {
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                startDownloadAndInstall();
            }).start();
        });
    }

    private void startDownloadAndInstall() {
        if (isDownloading) return;

        isDownloading = true;
        btnDetailInstall.setEnabled(false);
        detailProgressBar.setVisibility(View.VISIBLE);
        detailProgressBar.setProgress(0);
        tvDownloadStatus.setVisibility(View.VISIBLE);
        tvDownloadStatus.setText(getString(R.string.downloading));

        PocketMarketApi.getInstance().recordDownload(modItem.getId(), new PocketMarketApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String downloadUrl) {
                String ext = modItem.isApk() ? ".apk" : "." + modItem.getFileType();
                String fileName = modItem.getId() + ext;

                ModInstaller.downloadFile(DetailActivity.this, downloadUrl, fileName, new ModInstaller.DownloadListener() {
                    @Override
                    public void onProgress(int percent, long downloadedBytes, long totalBytes) {
                        if (percent >= 0) {
                            detailProgressBar.setIndeterminate(false);
                            detailProgressBar.setProgress(percent);
                            tvDownloadStatus.setText(String.format("Скачано: %s / %s (%d%%)",
                                    ModInstaller.formatBytes(downloadedBytes),
                                    ModInstaller.formatBytes(totalBytes),
                                    percent));
                        } else {
                            detailProgressBar.setIndeterminate(true);
                            tvDownloadStatus.setText("Скачано: " + ModInstaller.formatBytes(downloadedBytes));
                        }
                    }

                    @Override
                    public void onComplete(File file) {
                        isDownloading = false;
                        btnDetailInstall.setEnabled(true);
                        detailProgressBar.setVisibility(View.GONE);
                        tvDownloadStatus.setText(modItem.isApk() ? getString(R.string.apk_installing) : getString(R.string.installing));

                        ModInstaller.install(DetailActivity.this, modItem, file);
                    }

                    @Override
                    public void onError(Exception e) {
                        isDownloading = false;
                        btnDetailInstall.setEnabled(true);
                        detailProgressBar.setVisibility(View.GONE);
                        tvDownloadStatus.setVisibility(View.GONE);
                        Toast.makeText(DetailActivity.this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                isDownloading = false;
                btnDetailInstall.setEnabled(true);
                detailProgressBar.setVisibility(View.GONE);
                tvDownloadStatus.setVisibility(View.GONE);
                Toast.makeText(DetailActivity.this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatDownloads(int count) {
        if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
