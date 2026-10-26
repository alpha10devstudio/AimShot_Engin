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

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (findViewById(android.R.id.content) != null) {
            FontHelper.applyRegularFont(findViewById(android.R.id.content));
        }
    }
}
