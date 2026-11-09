package com.pocketmarket.client;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pocketmarket.client.adapter.ModAdapter;
import com.pocketmarket.client.api.PocketMarketApi;
import com.pocketmarket.client.installer.ModInstaller;
import com.pocketmarket.client.model.ModItem;
import com.pocketmarket.client.utils.AuthManager;
import com.pocketmarket.client.utils.FontUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView btnSettings;
    private LinearLayout navItemAbout;
    private EditText etSearch;

    // Bottom Navigation Elements (Flat, No outlines)
    private LinearLayout tabApks;
    private LinearLayout tabTextures;
    private LinearLayout tabMods;

    private ImageView ivTabApks;
    private ImageView ivTabTextures;
    private ImageView ivTabMods;

    private TextView tvTabApks;
    private TextView tvTabTextures;
    private TextView tvTabMods;

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvItems;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private ImageView ivEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptySub;

    private ModAdapter modAdapter;
    private String currentCategory = "apks";
    private String currentSearch = "";
    private String currentVersion = "all";

    // Version filter
    private LinearLayout llVersionContainer;
    private final List<String> mcVersionList = new ArrayList<>(Arrays.asList(
            "1.21.51", "1.21.31", "1.20.81", "1.20.51"
    ));
    private final List<String> customVersionList = new ArrayList<>();

    // Drawer user profile & version views
    private ImageView ivDrawerUserAvatar;
    private TextView tvDrawerUserName;
    private TextView tvDrawerUserEmail;
    private TextView tvDrawerUserStatus;
    private LinearLayout navItemAuthAction;
    private ImageView ivNavAuthIcon;
    private TextView tvNavAuthTitle;
    private TextView tvDrawerVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AuthManager.init(this);
        if (AuthManager.getInstance().isFirstLaunch()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        FontUtils.applyMinecraftFont(findViewById(android.R.id.content));

        initViews();
        setupRecyclerView();
        setupVersionFilter();
        setupTabs();
        setupSearch();
        setupListeners();
        updateDrawerUserSection();

        selectTab("apks");
        checkAppUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ModInstaller.resumePendingInstallIfPossible(this);
        updateDrawerUserSection();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        btnSettings = findViewById(R.id.btnSettings);
        navItemAbout = findViewById(R.id.navItemAbout);

        etSearch = findViewById(R.id.etSearch);
        llVersionContainer = findViewById(R.id.llVersionContainer);

        ivDrawerUserAvatar = findViewById(R.id.ivDrawerUserAvatar);
        tvDrawerUserName = findViewById(R.id.tvDrawerUserName);
        tvDrawerUserEmail = findViewById(R.id.tvDrawerUserEmail);
        tvDrawerUserStatus = findViewById(R.id.tvDrawerUserStatus);
        navItemAuthAction = findViewById(R.id.navItemAuthAction);
        ivNavAuthIcon = findViewById(R.id.ivNavAuthIcon);
        tvNavAuthTitle = findViewById(R.id.tvNavAuthTitle);
        tvDrawerVersion = findViewById(R.id.tvDrawerVersion);

        tabApks = findViewById(R.id.tabApks);
        tabTextures = findViewById(R.id.tabTextures);
        tabMods = findViewById(R.id.tabMods);

        ivTabApks = findViewById(R.id.ivTabApks);
        ivTabTextures = findViewById(R.id.ivTabTextures);
        ivTabMods = findViewById(R.id.ivTabMods);

        tvTabApks = findViewById(R.id.tvTabApks);
        tvTabTextures = findViewById(R.id.tvTabTextures);
        tvTabMods = findViewById(R.id.tvTabMods);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvItems = findViewById(R.id.rvItems);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        ivEmptyIcon = findViewById(R.id.ivEmptyIcon);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySub = findViewById(R.id.tvEmptySub);

        swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.mc_red),
                ContextCompat.getColor(this, R.color.mc_gold),
                ContextCompat.getColor(this, R.color.mc_aqua)
        );
        swipeRefresh.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(this, R.color.bg_surface)
        );
    }

    private void setupRecyclerView() {
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        if (rvItems.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) rvItems.getItemAnimator()).setSupportsChangeAnimations(false);
        }
        modAdapter = new ModAdapter(new ModAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ModItem item) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra(DetailActivity.EXTRA_MOD_ITEM, item);
                startActivity(intent);
            }

            @Override
            public void onInstallClick(ModItem item) {
                quickInstall(item);
            }

            @Override
            public void onLikeClick(ModItem item, int position) {
                if (!AuthManager.getInstance(MainActivity.this).isLoggedIn()) {
                    showLoginPrompt();
                    return;
                }
                PocketMarketApi.getInstance().toggleLike(item.getId(), new PocketMarketApi.ApiCallback<PocketMarketApi.LikeResult>() {
                    @Override
                    public void onSuccess(PocketMarketApi.LikeResult result) {
                        item.setLiked(result.liked);
                        item.setLikes(result.likes);
                        modAdapter.notifyItemChanged(position);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(MainActivity.this, "Ошибка отметки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        rvItems.setAdapter(modAdapter);
    }

    private void setupVersionFilter() {
        if (llVersionContainer == null) return;
        llVersionContainer.removeAllViews();

        // 1. Chip "Все"
        addVersionChip("Все", "all".equals(currentVersion), v -> {
            currentVersion = "all";
            setupVersionFilter();
            loadItems(true);
        });

        // 2. Chip "Ввести свою" right at the beginning (in place of 26.40)
        TextView customChip = new TextView(this);
        customChip.setText("Ввести свою");
        customChip.setTextSize(11);
        customChip.setTypeface(FontUtils.getMinecraftFont(this));
        LinearLayout.LayoutParams customLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        customLp.setMargins(0, 0, 16, 0);
        customChip.setLayoutParams(customLp);

        boolean isCustomSelected = !currentVersion.equals("all") && !mcVersionList.contains(currentVersion);
        if (isCustomSelected) {
            customChip.setBackgroundResource(R.drawable.bg_chip_version_selected);
            customChip.setTextColor(ContextCompat.getColor(this, R.color.mc_red));
        } else {
            customChip.setBackgroundResource(R.drawable.bg_chip_version);
            customChip.setTextColor(ContextCompat.getColor(this, R.color.mc_gold));
        }

        customChip.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
            }).start();
            showCustomVersionDialog();
        });
        llVersionContainer.addView(customChip);

        // 3. Custom entered versions (displayed right after "Ввести свою")
        for (String customVer : customVersionList) {
            boolean isSelected = customVer.equals(currentVersion);
            addVersionChip(customVer, isSelected, v -> {
                currentVersion = customVer;
                setupVersionFilter();
                loadItems(true);
            });
        }

        // 4. Preset versions (1.21.51, 1.21.31, 1.20.81, 1.20.51)
        for (String version : mcVersionList) {
            boolean isSelected = version.equals(currentVersion);
            addVersionChip(version, isSelected, v -> {
                currentVersion = version;
                setupVersionFilter();
                loadItems(true);
            });
        }
    }

    private void addVersionChip(String text, boolean isSelected, View.OnClickListener onClickListener) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(11);
        chip.setTypeface(FontUtils.getMinecraftFont(this));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 16, 0);
        chip.setLayoutParams(lp);

        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_version_selected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.mc_red));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_version);
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_body));
        }

        chip.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(60).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
            }).start();
            onClickListener.onClick(v);
        });

        llVersionContainer.addView(chip);
    }

    private void showCustomVersionDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_custom_version);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
        FontUtils.applyMinecraftFont(dialog.findViewById(android.R.id.content));

        EditText etVersion = dialog.findViewById(R.id.etCustomVersionInput);
        View btnCancel = dialog.findViewById(R.id.btnCancelCustomVersion);
        View btnApply = dialog.findViewById(R.id.btnApplyCustomVersion);

        if (etVersion != null && !currentVersion.equals("all") && !mcVersionList.contains(currentVersion)) {
            etVersion.setText(currentVersion);
            etVersion.setSelection(currentVersion.length());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                if (etVersion == null) return;
                String input = etVersion.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(this, "Введите версию", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isValidCustomVersion(input)) {
                    Toast.makeText(this, "Поддерживаются версии от 1.20.0 (например 1.20.51 или 26.40)", Toast.LENGTH_LONG).show();
                    return;
                }

                String cleanVer = input.replaceFirst("^[vV]", "").trim();
                if (!customVersionList.contains(cleanVer) && !mcVersionList.contains(cleanVer)) {
                    customVersionList.add(0, cleanVer);
                }
                currentVersion = cleanVer;
                setupVersionFilter();
                loadItems(true);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private boolean isValidCustomVersion(String version) {
        if (version == null) return false;
        String v = version.trim().replaceFirst("^[vV]", "");
        String[] parts = v.split("\\.");
        if (parts.length < 2) return false;
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            if (major >= 26) return true;
            if (major == 1 && minor >= 20) return true;
        } catch (NumberFormatException ignored) {}
        return false;
    }

    private void setupTabs() {
        tabApks.setOnClickListener(v -> selectTab("apks"));
        tabTextures.setOnClickListener(v -> selectTab("textures"));
        tabMods.setOnClickListener(v -> selectTab("mods"));
    }

    private void selectTab(String category) {
        currentCategory = category;

        int activeColor = ContextCompat.getColor(this, R.color.mc_red);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_muted);

        // Update APK tab
        boolean isApk = "apks".equals(category);
        ivTabApks.setColorFilter(isApk ? activeColor : inactiveColor);
        tvTabApks.setTextColor(isApk ? activeColor : inactiveColor);

        // Update Textures tab
        boolean isTexture = "textures".equals(category);
        ivTabTextures.setColorFilter(isTexture ? activeColor : inactiveColor);
        tvTabTextures.setTextColor(isTexture ? activeColor : inactiveColor);

        // Update Mods tab
        boolean isMod = "mods".equals(category);
        ivTabMods.setColorFilter(isMod ? activeColor : inactiveColor);
        tvTabMods.setTextColor(isMod ? activeColor : inactiveColor);

        loadItems(true);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                loadItems(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(() -> loadItems(false));

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                // Smooth gear rotation animation (360 degrees)
                btnSettings.animate()
                        .rotationBy(360f)
                        .setDuration(450)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();

                if (drawerLayout != null) {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        if (navItemAbout != null) {
            navItemAbout.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                showAboutDialog();
            });
        }
    }

    private void showAboutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_about);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }

        FontUtils.applyMinecraftFont(dialog.findViewById(android.R.id.content));

        TextView tvAboutVersion = dialog.findViewById(R.id.tvAboutVersion);
        if (tvAboutVersion != null) {
            String verName = "1.1.3";
            int verCode = 4;
            try {
                android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                verName = pi.versionName;
                verCode = pi.versionCode;
            } catch (Exception ignored) {}
            tvAboutVersion.setText("PocketMarket v" + verName + " (Сборка " + verCode + ")");
        }

        View btnGithub = dialog.findViewById(R.id.btnAboutGithub);
        if (btnGithub != null) {
            btnGithub.setOnClickListener(v -> {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Pomidor8639/PocketMarket"));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnClose = dialog.findViewById(R.id.btnAboutClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void loadItems(boolean showLoading) {
        if (showLoading) {
            progressBar.setVisibility(View.VISIBLE);
        }

        PocketMarketApi.getInstance().fetchItems(currentCategory, currentSearch, currentVersion, new PocketMarketApi.ApiCallback<List<ModItem>>() {
            @Override
            public void onSuccess(List<ModItem> items) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (items == null || items.isEmpty()) {
                    showEmptyState();
                } else {
                    emptyStateLayout.setVisibility(View.GONE);
                }

                modAdapter.setItems(items);
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showEmptyState();
            }
        });
    }

    private void updateDrawerUserSection() {
        if (tvDrawerVersion != null) {
            String verName = "1.1.3";
            int verCode = 4;
            try {
                android.content.pm.PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                verName = pi.versionName;
                verCode = pi.versionCode;
            } catch (Exception ignored) {}
            tvDrawerVersion.setText("PocketMarket v" + verName + " (сборка " + verCode + ")");
        }

        AuthManager auth = AuthManager.getInstance(this);
        if (auth.isLoggedIn()) {
            if (tvDrawerUserName != null) tvDrawerUserName.setText(auth.getNickname());
            if (tvDrawerUserEmail != null) {
                tvDrawerUserEmail.setVisibility(View.VISIBLE);
                tvDrawerUserEmail.setText(auth.getEmail());
            }
            if (tvDrawerUserStatus != null) {
                tvDrawerUserStatus.setText("АВТОРИЗОВАН");
                tvDrawerUserStatus.setTextColor(ContextCompat.getColor(this, R.color.mc_green));
            }
            if (tvNavAuthTitle != null) tvNavAuthTitle.setText("Выйти из аккаунта");
            if (ivNavAuthIcon != null) ivNavAuthIcon.setImageResource(R.drawable.ic_logout);
            if (navItemAuthAction != null) {
                navItemAuthAction.setOnClickListener(v -> showLogoutConfirmationDialog());
            }
        } else {
            if (tvDrawerUserName != null) tvDrawerUserName.setText("Гостевой режим");
            if (tvDrawerUserEmail != null) tvDrawerUserEmail.setVisibility(View.GONE);
            if (tvDrawerUserStatus != null) {
                tvDrawerUserStatus.setText("ГОСТЕВОЙ РЕЖИМ");
                tvDrawerUserStatus.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            }
            if (tvNavAuthTitle != null) tvNavAuthTitle.setText("Войти в аккаунт");
            if (ivNavAuthIcon != null) ivNavAuthIcon.setImageResource(R.drawable.ic_login);
            if (navItemAuthAction != null) {
                navItemAuthAction.setOnClickListener(v -> {
                    if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                    startActivity(new Intent(MainActivity.this, AuthActivity.class));
                });
            }
        }
    }

    private void showLogoutConfirmationDialog() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти из учетной записи?")
                .setPositiveButton("ВЫЙТИ", (d, w) -> {
                    AuthManager.getInstance(MainActivity.this).logout();
                    Toast.makeText(MainActivity.this, "Вы вышли из учетной записи", Toast.LENGTH_SHORT).show();
                    updateDrawerUserSection();
                    loadItems(true);
                })
                .setNegativeButton("ОТМЕНА", null)
                .show();
    }

    private void showLoginPrompt() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Требуется вход")
                .setMessage("Для того чтобы ставить отметки «Нравится», необходимо войти в аккаунт.")
                .setPositiveButton("ВОЙТИ", (d, w) -> {
                    startActivity(new Intent(MainActivity.this, AuthActivity.class));
                })
                .setNegativeButton("ОТМЕНА", null)
                .show();
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        if ("apks".equals(currentCategory)) {
            ivEmptyIcon.setImageResource(R.drawable.apk_install_48);
            ivEmptyIcon.setColorFilter(ContextCompat.getColor(this, R.color.mc_gold));
            tvEmptyTitle.setText(R.string.empty_apks);
        } else if ("textures".equals(currentCategory)) {
            ivEmptyIcon.setImageResource(R.drawable.ic_palette);
            ivEmptyIcon.setColorFilter(ContextCompat.getColor(this, R.color.mc_aqua));
            tvEmptyTitle.setText(R.string.empty_textures);
        } else {
            ivEmptyIcon.setImageResource(R.drawable.ic_extension);
            ivEmptyIcon.setColorFilter(ContextCompat.getColor(this, R.color.mc_red));
            tvEmptyTitle.setText(R.string.empty_mods);
        }
        if (tvEmptySub != null) {
            tvEmptySub.setVisibility(View.GONE);
        }
    }

    private void quickInstall(ModItem item) {
        Dialog downloadDialog = new Dialog(this);
        downloadDialog.setContentView(R.layout.dialog_download);
        if (downloadDialog.getWindow() != null) {
            downloadDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            downloadDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
        FontUtils.applyMinecraftFont(downloadDialog.findViewById(android.R.id.content));

        TextView tvTitle = downloadDialog.findViewById(R.id.tvDownloadTitle);
        TextView tvStats = downloadDialog.findViewById(R.id.tvDownloadStats);
        ProgressBar progressBar = downloadDialog.findViewById(R.id.pbDownloadProgress);
        ImageView ivIcon = downloadDialog.findViewById(R.id.ivDownloadIcon);

        if (tvTitle != null) tvTitle.setText(item.getTitle());
        if (tvStats != null) tvStats.setText("Подключение к серверу...");
        if (progressBar != null) progressBar.setIndeterminate(true);
        if (ivIcon != null) {
            if (item.isApk()) {
                ivIcon.setImageResource(R.drawable.apk_install_48);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.mc_gold));
            } else if ("textures".equalsIgnoreCase(item.getCategory())) {
                ivIcon.setImageResource(R.drawable.ic_palette);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.mc_aqua));
            } else {
                ivIcon.setImageResource(R.drawable.ic_extension);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.mc_green));
            }
        }

        downloadDialog.setCancelable(false);
        downloadDialog.show();

        PocketMarketApi.getInstance().recordDownload(item.getId(), new PocketMarketApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String downloadUrl) {
                String ext = item.isApk() ? ".apk" : "." + item.getFileType();
                String fileName = item.getId() + ext;

                ModInstaller.downloadFile(MainActivity.this, downloadUrl, fileName, new ModInstaller.DownloadListener() {
                    @Override
                    public void onProgress(int percent, long downloadedBytes, long totalBytes) {
                        if (progressBar != null) {
                            if (percent >= 0) {
                                progressBar.setIndeterminate(false);
                                progressBar.setProgress(percent);
                            } else {
                                progressBar.setIndeterminate(true);
                            }
                        }
                        if (tvStats != null) {
                            if (totalBytes > 0 && percent >= 0) {
                                tvStats.setText(String.format("Скачано: %s / %s (%d%%)",
                                        ModInstaller.formatBytes(downloadedBytes),
                                        ModInstaller.formatBytes(totalBytes),
                                        percent));
                            } else {
                                tvStats.setText("Скачано: " + ModInstaller.formatBytes(downloadedBytes));
                            }
                        }
                    }

                    @Override
                    public void onComplete(File file) {
                        if (tvStats != null) tvStats.setText("Завершено! Запуск...");
                        downloadDialog.dismiss();
                        ModInstaller.install(MainActivity.this, item, file);
                    }

                    @Override
                    public void onError(Exception e) {
                        downloadDialog.dismiss();
                        Toast.makeText(MainActivity.this, "Ошибка скачивания: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                downloadDialog.dismiss();
                Toast.makeText(MainActivity.this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAppUpdates() {
        PocketMarketApi.getInstance().checkForUpdates(new PocketMarketApi.ApiCallback<PocketMarketApi.UpdateInfo>() {
            @Override
            public void onSuccess(PocketMarketApi.UpdateInfo info) {
                int currentCode = 1;
                try {
                    currentCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                } catch (Exception ignored) {
                }

                if (info != null && info.versionCode > currentCode) {
                    showUpdateDialog(info);
                }
            }

            @Override
            public void onError(Exception e) {
                // Ignore silent update errors
            }
        });
    }

    private void showUpdateDialog(PocketMarketApi.UpdateInfo info) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_update);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
        FontUtils.applyMinecraftFont(dialog.findViewById(android.R.id.content));

        TextView tvVer = dialog.findViewById(R.id.tvUpdateVersion);
        TextView tvChangelog = dialog.findViewById(R.id.tvUpdateChangelog);
        View btnLater = dialog.findViewById(R.id.btnUpdateLater);
        View btnUpdate = dialog.findViewById(R.id.btnUpdateNow);

        if (tvVer != null) tvVer.setText("PocketMarket v" + info.versionName + " (" + info.fileSize + ")");
        if (tvChangelog != null) tvChangelog.setText(info.changelog);

        if (btnLater != null) {
            btnLater.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                dialog.dismiss();
                startAppSelfUpdate(info.downloadUrl);
            });
        }

        dialog.show();
    }

    private void startAppSelfUpdate(String downloadUrl) {
        Dialog downloadDialog = new Dialog(this);
        downloadDialog.setContentView(R.layout.dialog_download);
        if (downloadDialog.getWindow() != null) {
            downloadDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            downloadDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
        FontUtils.applyMinecraftFont(downloadDialog.findViewById(android.R.id.content));

        TextView tvTitle = downloadDialog.findViewById(R.id.tvDownloadTitle);
        TextView tvStats = downloadDialog.findViewById(R.id.tvDownloadStats);
        ProgressBar progressBar = downloadDialog.findViewById(R.id.pbDownloadProgress);
        ImageView ivIcon = downloadDialog.findViewById(R.id.ivDownloadIcon);

        if (tvTitle != null) tvTitle.setText("Обновление PocketMarket...");
        if (tvStats != null) tvStats.setText("Загрузка обновления...");
        if (ivIcon != null) ivIcon.setImageResource(R.drawable.apk_install_48);

        downloadDialog.setCancelable(false);
        downloadDialog.show();

        ModInstaller.downloadFile(this, downloadUrl, "PocketMarket_Update.apk", new ModInstaller.DownloadListener() {
            @Override
            public void onProgress(int percent, long downloadedBytes, long totalBytes) {
                if (progressBar != null) {
                    if (percent >= 0) {
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress(percent);
                    } else {
                        progressBar.setIndeterminate(true);
                    }
                }
                if (tvStats != null) {
                    if (totalBytes > 0 && percent >= 0) {
                        tvStats.setText(String.format("Скачано: %s / %s (%d%%)",
                                ModInstaller.formatBytes(downloadedBytes),
                                ModInstaller.formatBytes(totalBytes),
                                percent));
                    } else {
                        tvStats.setText("Скачано: " + ModInstaller.formatBytes(downloadedBytes));
                    }
                }
            }

            @Override
            public void onComplete(File file) {
                downloadDialog.dismiss();
                ModItem appItem = new ModItem();
                appItem.setCategory("apks");
                ModInstaller.install(MainActivity.this, appItem, file);
            }

            @Override
            public void onError(Exception e) {
                downloadDialog.dismiss();
                Toast.makeText(MainActivity.this, "Ошибка загрузки обновления: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
