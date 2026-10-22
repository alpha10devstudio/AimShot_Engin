package com.alpha10.aimshot.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.model.GameItem;

import java.util.List;

/**
 * Adapter for displaying supported game tiles on the Home screen grid.
 * Carrom Disc Pool is active & selected by default with cyan glowing border.
 * Each tile displays its genuine logo, active/locked status dot, and status label.
 */
public class GameGridAdapter extends RecyclerView.Adapter<GameGridAdapter.ViewHolder> {

    private final List<GameItem> gameList;
    private final OnGameClickListener listener;
    private String selectedGameId = "carrom";

    public interface OnGameClickListener {
        void onGameClick(GameItem item);
    }

    public GameGridAdapter(List<GameItem> gameList, OnGameClickListener listener) {
        this.gameList = gameList;
        this.listener = listener;
    }

    public void setSelectedGameId(String id) {
        this.selectedGameId = id;
        notifyDataSetChanged();
    }

    public String getSelectedGameId() {
        return selectedGameId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final GameItem item = gameList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvGameName.setText(item.getName());

        // Resolve high quality drawable for game logo
        int resolvedResId = 0;
        if (item.getDrawableName() != null && !item.getDrawableName().isEmpty()) {
            resolvedResId = context.getResources().getIdentifier(item.getDrawableName(), "drawable", context.getPackageName());
        }
        if (resolvedResId == 0 && item.getId() != null) {
            resolvedResId = context.getResources().getIdentifier("game_" + item.getId(), "drawable", context.getPackageName());
        }
        if (resolvedResId != 0) {
            holder.ivGameIcon.setImageResource(resolvedResId);
        } else if (item.getIconResId() != 0) {
            holder.ivGameIcon.setImageResource(item.getIconResId());
        } else {
            holder.ivGameIcon.setImageResource(R.drawable.ic_aimshot_logo);
        }

        // Selection styling: cyan border and cyan text
        boolean isSelected = item.getId() != null && item.getId().equals(selectedGameId);
        if (isSelected) {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_game_card_selected);
            holder.tvGameName.setTextColor(ContextCompat.getColor(context, R.color.accent_primary));
        } else {
            holder.layoutCard.setBackgroundResource(R.drawable.bg_game_card_normal);
            holder.tvGameName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }

        // Active vs Locked status dot and label
        if (item.isActive()) {
            holder.ivStatusDot.setImageResource(R.drawable.dot_game_active);
            holder.tvGameStatus.setText("Active");
            holder.tvGameStatus.setTextColor(Color.parseColor("#49DCA0"));
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.ivStatusDot.setImageResource(R.drawable.dot_game_locked);
            holder.tvGameStatus.setText("Locked");
            holder.tvGameStatus.setTextColor(Color.parseColor("#8D9BB0"));
            holder.itemView.setAlpha(isSelected ? 1.0f : 0.85f);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedGameId = item.getId();
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onGameClick(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return gameList != null ? gameList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View layoutCard;
        final ImageView ivGameIcon;
        final ImageView ivStatusDot;
        final TextView tvGameName;
        final TextView tvGameStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutCard = itemView.findViewById(R.id.layout_game_card);
            ivGameIcon = itemView.findViewById(R.id.iv_grid_game_icon);
            ivStatusDot = itemView.findViewById(R.id.iv_grid_status_dot);
            tvGameName = itemView.findViewById(R.id.tv_grid_game_name);
            tvGameStatus = itemView.findViewById(R.id.tv_grid_game_status);
        }
    }
}
