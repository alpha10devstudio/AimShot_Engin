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

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying detailed game cards on the Supported Games activity.
 * Supports dynamic icon lookup from res/drawable (PNG or XML) with fallback to ic_aimshot_logo.
 * Applies 0.45f alpha on locked games (8 Ball Pool, PUBG, etc.) and 1.0f on Carrom Disc Pool.
 */
public class GameListAdapter extends RecyclerView.Adapter<GameListAdapter.ViewHolder> {

    private List<GameItem> gameList;
    private final OnGameActionListener listener;

    public interface OnGameActionListener {
        void onGameSelected(GameItem item);
    }

    public GameListAdapter(List<GameItem> gameList, OnGameActionListener listener) {
        this.gameList = new ArrayList<>(gameList);
        this.listener = listener;
    }

    public void updateData(List<GameItem> newList) {
        this.gameList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final GameItem item = gameList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvGameName.setText(item.getName());
        holder.tvGameFeatures.setText(item.getFeatureSummary());

        // Dynamic resource resolution
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

        // Active vs locked presentation
        if (item.isActive()) {
            holder.itemView.setAlpha(1.0f);
            holder.ivGameCheck.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setAlpha(0.45f);
            holder.ivGameCheck.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onGameSelected(item);
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
        final TextView tvGameFeatures;
        final ImageView ivGameCheck;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGameIcon = itemView.findViewById(R.id.iv_item_game_icon);
            tvGameName = itemView.findViewById(R.id.tv_item_game_name);
            tvGameFeatures = itemView.findViewById(R.id.tv_item_game_features);
            ivGameCheck = itemView.findViewById(R.id.iv_item_game_check);
        }
    }
}
