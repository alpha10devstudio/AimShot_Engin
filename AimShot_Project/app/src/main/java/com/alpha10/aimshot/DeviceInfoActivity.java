package com.alpha10.aimshot;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alpha10.aimshot.service.DeviceIdManager;

/**
 * Screen presenting device hardware diagnostics and privacy-safe app-scoped identifier.
 */
public class DeviceInfoActivity extends BaseActivity {

    private DeviceIdManager deviceIdManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        deviceIdManager = new DeviceIdManager(this);

        initViews();
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btn_device_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final TextView tvDeviceId = findViewById(R.id.tv_device_id_val);
        Button btnCopy = findViewById(R.id.btn_copy_device_id);

        TextView tvAndroidVersion = findViewById(R.id.tv_spec_android_version);
        TextView tvModel = findViewById(R.id.tv_spec_model);
        TextView tvBrand = findViewById(R.id.tv_spec_brand);
        TextView tvRam = findViewById(R.id.tv_spec_ram);
        TextView tvResolution = findViewById(R.id.tv_spec_resolution);
        TextView tvCpu = findViewById(R.id.tv_spec_cpu);

        final String formattedId = deviceIdManager.getFormattedDeviceId();
        tvDeviceId.setText(formattedId);

        tvAndroidVersion.setText(deviceIdManager.getAndroidVersion());
        tvModel.setText(deviceIdManager.getDeviceModel());
        tvBrand.setText(deviceIdManager.getDeviceBrand());
        tvRam.setText(deviceIdManager.getTotalRam());
        tvResolution.setText(deviceIdManager.getScreenResolution());
        tvCpu.setText(deviceIdManager.getCpuName());

        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("AimShot Device ID", formattedId);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(DeviceInfoActivity.this, R.string.device_id_copied, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
