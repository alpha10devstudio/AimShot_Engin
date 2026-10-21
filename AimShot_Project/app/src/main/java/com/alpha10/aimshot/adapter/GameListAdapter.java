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

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying detailed game cards on the Supported Games activity.
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
        holder.tvGameName.setText(item.getName());
        holder.tvGameFeatures.setText(item.getFeatureSummary());
        holder.ivGameIcon.setImageResource(item.getIconResId());
        holder.ivGameCheck.setVisibility(item.isVerified() ? View.VISIBLE : View.GONE);

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
