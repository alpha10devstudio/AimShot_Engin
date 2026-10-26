package com.alpha10.aimshot;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alpha10.aimshot.model.SellerInfo;

/**
 * Screen presenting verified seller channels with reliable external intents.
 */
public class SellerContactActivity extends BaseActivity {

    private SellerInfo sellerInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_contact);

        sellerInfo = SellerInfo.getDefaultSeller();

        setupViews();
        setupContactActions();
    }

    private void setupViews() {
        View btnBack = findViewById(R.id.btn_seller_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView tvSellerName = findViewById(R.id.tv_seller_name);
        TextView tvWhatsapp = findViewById(R.id.tv_seller_whatsapp);
        TextView tvTelegram = findViewById(R.id.tv_seller_telegram);
        TextView tvEmail = findViewById(R.id.tv_seller_email);

        tvSellerName.setText(sellerInfo.getSellerName());
        tvWhatsapp.setText(sellerInfo.getWhatsappNumber());
        tvTelegram.setText(sellerInfo.getTelegramHandle());
        tvEmail.setText(sellerInfo.getEmailAddress());
    }

    private void setupContactActions() {
        // WhatsApp Action
        View btnWhatsapp = findViewById(R.id.btn_contact_whatsapp);
        btnWhatsapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWhatsApp(sellerInfo.getWhatsappNumber());
            }
        });

        // Telegram Action
        View btnTelegram = findViewById(R.id.btn_contact_telegram);
        btnTelegram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTelegram(sellerInfo.getTelegramHandle());
            }
        });

        // Email Action
        View btnEmail = findViewById(R.id.btn_contact_email);
        btnEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEmail(sellerInfo.getEmailAddress());
            }
        });

        // Primary Contact Now button
        Button btnContactNow = findViewById(R.id.btn_action_contact_now);
        btnContactNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTelegram(sellerInfo.getTelegramHandle());
            }
        });
    }

    private void openWhatsApp(String rawNumber) {
        try {
            String cleanNumber = rawNumber.replaceAll("[^0-9]", "");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/" + cleanNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open WhatsApp. Please check if browser or WhatsApp is available.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTelegram(String handle) {
        try {
            String cleanHandle = handle.replace("@", "");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://t.me/" + cleanHandle));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open Telegram. Please check if browser or Telegram is available.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openEmail(String email) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "AimShot Engine License Inquiry");
            startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (Exception e) {
            Toast.makeText(this, "No email application found.", Toast.LENGTH_SHORT).show();
        }
    }
}
