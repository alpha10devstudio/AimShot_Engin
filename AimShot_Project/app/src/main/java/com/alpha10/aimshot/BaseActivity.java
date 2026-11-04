package com.alpha10.aimshot;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alpha10.aimshot.util.FontHelper;
import com.alpha10.aimshot.util.LocaleHelper;

/**
 * Base activity propagating dynamic locale and font management across all screens.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private String currentLanguageCode;

    @Override
    protected void attachBaseContext(Context newBase) {
        currentLanguageCode = LocaleHelper.getPersistedData(newBase, "en");
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String persistedCode = LocaleHelper.getPersistedData(this, "en");
        if (currentLanguageCode != null && !currentLanguageCode.equals(persistedCode)) {
            currentLanguageCode = persistedCode;
            recreate();
            return;
        }

        if (findViewById(android.R.id.content) != null) {
            FontHelper.applyRegularFont(findViewById(android.R.id.content));
        }
    }

    @Override
    public void startActivity(android.content.Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void startActivity(android.content.Intent intent, @Nullable Bundle options) {
        super.startActivity(intent, options);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
