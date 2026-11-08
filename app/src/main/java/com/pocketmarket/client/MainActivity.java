package com.pocketmarket.client;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageView btnRefresh;
    private TextView tabAll;
    private TextView tabMods;
    private TextView tabTextures;
    private TextView tabApks;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvItems;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private ModAdapter modAdapter;
    private String currentCategory = "all";
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

        loadItems(true);
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        btnRefresh = findViewById(R.id.btnRefresh);
        tabAll = findViewById(R.id.tabAll);
        tabMods = findViewById(R.id.tabMods);
        tabTextures = findViewById(R.id.tabTextures);
        tabApks = findViewById(R.id.tabApks);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvItems = findViewById(R.id.rvItems);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(this, R.color.mc_green),
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
        tabAll.setOnClickListener(v -> selectTab("all"));
        tabMods.setOnClickListener(v -> selectTab("mods"));
        tabTextures.setOnClickListener(v -> selectTab("textures"));
        tabApks.setOnClickListener(v -> selectTab("apks"));
    }

    private void selectTab(String category) {
        currentCategory = category;

        updateTabStyle(tabAll, "all".equals(category), R.color.mc_green);
        updateTabStyle(tabMods, "mods".equals(category), R.color.mc_green);
        updateTabStyle(tabTextures, "textures".equals(category), R.color.mc_aqua);
        updateTabStyle(tabApks, "apks".equals(category), R.color.mc_gold);

        loadItems(true);
    }

    private void updateTabStyle(TextView tab, boolean isSelected, int activeColorRes) {
        if (isSelected) {
            tab.setBackgroundResource(R.drawable.bg_tab_selected);
            tab.setTextColor(ContextCompat.getColor(this, activeColorRes));
        } else {
            tab.setBackgroundResource(R.drawable.bg_tab_unselected);
            tab.setTextColor(ContextCompat.getColor(this, R.color.text_body));
        }
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
        btnRefresh.setOnClickListener(v -> loadItems(true));
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

                if (items.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                }

                modAdapter.setItems(items);
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(MainActivity.this, "Ошибка соединения с сервером PocketMarket", Toast.LENGTH_SHORT).show();
            }
        });
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
