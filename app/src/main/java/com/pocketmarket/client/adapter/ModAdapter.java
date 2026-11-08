package com.pocketmarket.client.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pocketmarket.client.R;
import com.pocketmarket.client.model.ModItem;
import com.pocketmarket.client.utils.FontUtils;

import java.util.ArrayList;
import java.util.List;

public class ModAdapter extends RecyclerView.Adapter<ModAdapter.ModViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ModItem item);
        void onInstallClick(ModItem item);
        void onLikeClick(ModItem item, int position);
    }

    private final List<ModItem> items = new ArrayList<>();
    private final OnItemClickListener listener;
    private int lastAnimatedPosition = -1;

    public ModAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ModItem> newItems) {
        items.clear();
        lastAnimatedPosition = -1;
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mod, parent, false);
        FontUtils.applyMinecraftFont(view);
        return new ModViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModViewHolder holder, int position) {
        ModItem item = items.get(position);
        Context context = holder.itemView.getContext();

        // Item entrance animation (slide up + fade in)
        animateItemEntrance(holder.itemView, position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvAuthor.setText(context.getString(R.string.author_prefix) + item.getAuthor());
        holder.tvShortDesc.setText(item.getShortDescription());
        holder.tvDownloads.setText(formatDownloads(item.getDownloads()));
        holder.tvFileSize.setText(item.getFileSize());
        holder.tvMcVersion.setText("MC " + item.getMcVersion());

        boolean isApk = item.isApk();
        boolean isTexture = "textures".equalsIgnoreCase(item.getCategory());

        // Always red button style as requested
        holder.btnAction.setBackgroundResource(R.drawable.btn_red);

        if (isApk) {
            // Clean flat APK card without extra border outlines
            holder.cardContainer.setBackgroundResource(R.drawable.bg_card_apk);
            holder.ivIcon.setImageResource(R.drawable.apk_install_48);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.mc_gold));
            holder.tvCategoryBadge.setText("APK МОД");
            holder.tvCategoryBadge.setTextColor(ContextCompat.getColor(context, R.color.mc_gold));
            holder.btnAction.setText(R.string.download_apk);
        } else if (isTexture) {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_card_black);
            holder.ivIcon.setImageResource(R.drawable.ic_palette);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.mc_aqua));
            holder.tvCategoryBadge.setText("ТЕКСТУРЫ");
            holder.tvCategoryBadge.setTextColor(ContextCompat.getColor(context, R.color.mc_aqua));
            holder.btnAction.setText(R.string.install);
        } else {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_card_black);
            holder.ivIcon.setImageResource(R.drawable.ic_extension);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.mc_red));
            holder.tvCategoryBadge.setText("МОДЫ");
            holder.tvCategoryBadge.setTextColor(ContextCompat.getColor(context, R.color.mc_red));
            holder.btnAction.setText(R.string.install);
        }

        // Heart Like state
        holder.tvLikesCount.setText(String.valueOf(item.getLikes()));
        holder.ivHeart.setImageResource(item.isLiked() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        holder.btnLike.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
            }).start();
            if (listener != null) {
                listener.onLikeClick(item, holder.getAdapterPosition());
            }
        });

        // Tactile press animations
        holder.cardContainer.setOnClickListener(v -> {
            v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                if (listener != null) listener.onItemClick(item);
            }).start();
        });

        holder.btnAction.setOnClickListener(v -> {
            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(70).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                if (listener != null) listener.onInstallClick(item);
            }).start();
        });
    }

    private void animateItemEntrance(View view, int position) {
        if (position > lastAnimatedPosition) {
            view.setAlpha(0f);
            view.setTranslationY(40f);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(280)
                    .setStartDelay(Math.min(position * 35, 250))
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            lastAnimatedPosition = position;
        }
    }

    private String formatDownloads(int count) {
        if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ModViewHolder extends RecyclerView.ViewHolder {
        LinearLayout cardContainer;
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvAuthor;
        TextView tvCategoryBadge;
        TextView tvShortDesc;
        TextView tvDownloads;
        TextView tvFileSize;
        TextView tvMcVersion;
        LinearLayout btnLike;
        ImageView ivHeart;
        TextView tvLikesCount;
        Button btnAction;

        public ModViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
            tvShortDesc = itemView.findViewById(R.id.tvShortDesc);
            tvDownloads = itemView.findViewById(R.id.tvDownloads);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvMcVersion = itemView.findViewById(R.id.tvMcVersion);
            btnLike = itemView.findViewById(R.id.btnLike);
            ivHeart = itemView.findViewById(R.id.ivHeart);
            tvLikesCount = itemView.findViewById(R.id.tvLikesCount);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
