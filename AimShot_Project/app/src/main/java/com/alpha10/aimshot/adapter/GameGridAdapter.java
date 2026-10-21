package com.alpha10.aimshot.adapter;

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
        holder.tvGameName.setText(item.getName());
        holder.ivGameIcon.setImageResource(item.getIconResId());

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
