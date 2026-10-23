package com.alpha10.aimshot.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alpha10.aimshot.R;
import com.alpha10.aimshot.model.NotificationItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter presenting official AimShot announcements with category pills,
 * read states, and interactive selection.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final List<NotificationItem> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    public NotificationAdapter(List<NotificationItem> initialItems, OnNotificationClickListener listener) {
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
        this.listener = listener;
    }

    public void updateData(List<NotificationItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvTitle;
        private final TextView tvCategory;
        private final TextView tvMessage;
        private final TextView tvTime;
        private final View viewUnreadDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_notif_icon);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvCategory = itemView.findViewById(R.id.tv_notif_category);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            viewUnreadDot = itemView.findViewById(R.id.view_unread_dot);
        }

        public void bind(final NotificationItem item, final OnNotificationClickListener listener) {
            tvTitle.setText(item.getTitle());
            tvMessage.setText(item.getMessage());
            tvTime.setText(item.getTimestamp());

            String cat = item.getCategory().toUpperCase();
            tvCategory.setText(cat);

            // Icon according to category
            if ("FEATURE".equals(cat)) {
                ivIcon.setImageResource(R.drawable.ic_shield);
            } else if ("UPDATE".equals(cat)) {
                ivIcon.setImageResource(R.drawable.ic_chip);
            } else if ("MAINTENANCE".equals(cat)) {
                ivIcon.setImageResource(R.drawable.ic_settings);
            } else {
                ivIcon.setImageResource(R.drawable.ic_bell);
            }

            // Unread dot indicator
            viewUnreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onNotificationClick(item);
                    }
                }
            });
        }
    }
}
