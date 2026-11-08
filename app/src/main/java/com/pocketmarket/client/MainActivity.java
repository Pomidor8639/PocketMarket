package com.pocketmarket.client;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pocketmarket.client.adapter.ModAdapter;
import com.pocketmarket.client.api.PocketMarketApi;
import com.pocketmarket.client.installer.ModInstaller;
import com.pocketmarket.client.model.ModItem;
import com.pocketmarket.client.utils.FontUtils;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FontUtils.applyMinecraftFont(findViewById(android.R.id.content));

        initViews();
        setupRecyclerView();
        setupTabs();
        setupSearch();
        setupListeners();

        selectTab("apks");
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);

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
        });
        rvItems.setAdapter(modAdapter);
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
    }

    private void loadItems(boolean showLoading) {
        if (showLoading) {
            progressBar.setVisibility(View.VISIBLE);
        }

        PocketMarketApi.getInstance().fetchItems(currentCategory, currentSearch, new PocketMarketApi.ApiCallback<List<ModItem>>() {
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
                Toast.makeText(MainActivity.this, "Сервер онлайн. Каталог пуст или обновляется.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        if ("apks".equals(currentCategory)) {
            ivEmptyIcon.setImageResource(R.drawable.ic_apk_package);
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
        tvEmptySub.setText(R.string.empty_sub);
    }

    private void quickInstall(ModItem item) {
        Toast.makeText(this, "Подготовка к загрузке " + item.getTitle() + "...", Toast.LENGTH_SHORT).show();

        PocketMarketApi.getInstance().recordDownload(item.getId(), new PocketMarketApi.ApiCallback<String>() {
            @Override
            public void onSuccess(String downloadUrl) {
                String ext = item.isApk() ? ".apk" : "." + item.getFileType();
                String fileName = item.getId() + ext;

                Toast.makeText(MainActivity.this, "Скачивание файла...", Toast.LENGTH_SHORT).show();

                ModInstaller.downloadFile(MainActivity.this, downloadUrl, fileName, new ModInstaller.DownloadListener() {
                    @Override
                    public void onProgress(int percent) {
                    }

                    @Override
                    public void onComplete(File file) {
                        ModInstaller.install(MainActivity.this, item, file);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, "Ошибка сервера: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
