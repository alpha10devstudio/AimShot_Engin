package com.alpha10.aimshot;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alpha10.aimshot.adapter.NotificationAdapter;
import com.alpha10.aimshot.model.NotificationItem;
import com.alpha10.aimshot.repository.NotificationsRepository;

import java.util.List;

/**
 * Dedicated Notifications Activity displaying official announcements, updates,
 * synchronization states, and read tracking.
 */
public class NotificationsActivity extends AppCompatActivity {

    private NotificationsRepository repository;
    private NotificationAdapter adapter;
    private RecyclerView rvNotifications;
    private TextView tvUnreadSummary;
    private View layoutEmpty;
    private ProgressBar pbSyncing;
    private TextView btnMarkAllRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        repository = NotificationsRepository.getInstance(this);

        initViews();
        setupList();
        refreshUi();
        syncWithServer();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btn_notif_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnMarkAllRead = findViewById(R.id.btn_notif_mark_all_read);
        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                repository.markAllAsRead();
                refreshUi();
                Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
            });
        }

        View btnSync = findViewById(R.id.btn_notif_sync);
        if (btnSync != null) {
            btnSync.setOnClickListener(v -> syncWithServer());
        }

        rvNotifications = findViewById(R.id.rv_notifications_list);
        tvUnreadSummary = findViewById(R.id.tv_notif_unread_summary);
        layoutEmpty = findViewById(R.id.layout_notif_empty);
        pbSyncing = findViewById(R.id.pb_notif_syncing);
    }

    private void setupList() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        List<NotificationItem> items = repository.getAllNotifications();
        adapter = new NotificationAdapter(items, item -> {
            repository.markAsRead(item.getId());
            refreshUi();
            showNotificationDetailDialog(item);
        });
        rvNotifications.setAdapter(adapter);
    }

    private void refreshUi() {
        List<NotificationItem> items = repository.getAllNotifications();
        int unread = repository.getUnreadCount();

        if (adapter != null) {
            adapter.updateData(items);
        }

        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (rvNotifications != null) {
            rvNotifications.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (tvUnreadSummary != null) {
            if (unread > 0) {
                tvUnreadSummary.setText(unread + " unread announcement" + (unread > 1 ? "s" : ""));
            } else {
                tvUnreadSummary.setText("All announcements caught up");
            }
        }
    }

    private void syncWithServer() {
        if (pbSyncing != null) pbSyncing.setVisibility(View.VISIBLE);

        repository.syncNotifications(new NotificationsRepository.SyncCallback() {
            @Override
            public void onSuccess(List<NotificationItem> items, int unreadCount) {
                runOnUiThread(() -> {
                    if (pbSyncing != null) pbSyncing.setVisibility(View.GONE);
                    refreshUi();
                    Toast.makeText(NotificationsActivity.this, "Announcements synchronized", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error, List<NotificationItem> fallbackItems) {
                runOnUiThread(() -> {
                    if (pbSyncing != null) pbSyncing.setVisibility(View.GONE);
                    refreshUi();
                });
            }
        });
    }

    private void showNotificationDetailDialog(NotificationItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_info, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_info_title);
        TextView tvMsg = dialogView.findViewById(R.id.tv_dialog_info_message);
        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_info_icon);
        Button btnOk = dialogView.findViewById(R.id.btn_dialog_info_positive);

        if (tvTitle != null) tvTitle.setText(item.getTitle());
        if (tvMsg != null) {
            String fullDetails = item.getMessage() + "\n\n" +
                    "Category: " + item.getCategory().toUpperCase() + "\n" +
                    "Timestamp: " + item.getTimestamp() + "\n" +
                    "Status: Verified Official Announcement";
            tvMsg.setText(fullDetails);
        }
        if (ivIcon != null) {
            ivIcon.setImageResource(R.drawable.ic_bell);
        }
        if (btnOk != null) {
            btnOk.setText("Close");
            btnOk.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
