package com.alpha10.aimshot.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.model.GameItem;

import java.util.List;

/**
 * Adapter for displaying supported game tiles on the Home screen grid.
 * Carrom Disc Pool is active (alpha 1.0f).
 * 8 Ball Pool and other games are locked (alpha 0.45f).
 * Includes dynamic PNG/drawable resource lookup with safe fallback.
 */
public class GameGridAdapter extends RecyclerView.Adapter<GameGridAdapter.ViewHolder> {

    private final List<GameItem> gameList;
    private final OnGameClickListener listener;

    public interface OnGameClickListener {
        void onGameClick(GameItem item);
    }

    public GameGridAdapter(List<GameItem> gameList, OnGameClickListener listener) {
        this.gameList = gameList;
        this.listener = listener;
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

        // Dynamic resource loader for game icons (PNG in res/drawable or vector)
        int resolvedResId = 0;
        if (item.getDrawableName() != null && !item.getDrawableName().isEmpty()) {
            resolvedResId = context.getResources().getIdentifier(item.getDrawableName(), "drawable", context.getPackageName());
        }
        if (resolvedResId != 0) {
            holder.ivGameIcon.setImageResource(resolvedResId);
        } else if (item.getIconResId() != 0) {
            holder.ivGameIcon.setImageResource(item.getIconResId());
        } else {
            holder.ivGameIcon.setImageResource(R.drawable.ic_aimshot_logo);
        }

        // Apply visual lock opacity (0.45f for locked games, 1.0f for active)
        if (item.isActive()) {
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.itemView.setAlpha(0.45f);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        final ImageView ivGameIcon;
        final TextView tvGameName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGameIcon = itemView.findViewById(R.id.iv_grid_game_icon);
            tvGameName = itemView.findViewById(R.id.tv_grid_game_name);
        }
    }
}
