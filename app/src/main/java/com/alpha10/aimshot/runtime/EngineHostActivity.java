package com.alpha10.aimshot.runtime;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.alpha10.aimshot.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Non-exported internal container Activity registered in AimShot Launcher's AndroidManifest.xml.
 *
 * - No launcher intent-filter: NEVER appears in the phone's App Drawer or Home Screen.
 * - Strictly enforces active license verification in AimShot Launcher.
 * - Primary path: Replaced at instantiation by PluginInstrumentation so the target plugin Activity
 *   runs as a native Android Activity.
 * - Fallback path: If an OEM ROM blocks Instrumentation replacement, delegates lifecycle events
 *   directly to the instantiated plugin Activity attached to this host Window.
 */
public class EngineHostActivity extends Activity {

    private static final String TAG = "AimShot_EngineHostAct";

    private Activity fallbackDelegateActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!PrivateEngineRuntime.isLicenseActive(this)) {
            Toast.makeText(this, R.string.err_invalid_key, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent intent = getIntent();
        String targetClass = intent != null
                ? intent.getStringExtra(PrivateEngineRuntime.EXTRA_PLUGIN_ACTIVITY_CLASS)
                : null;

        if (targetClass == null || targetClass.isEmpty()) {
            finish();
            return;
        }

        // Fallback proxy execution when Instrumentation hook did not substitute EngineHostActivity
        try {
            PrivateEngineRuntime.LoadedPlugin plugin = PrivateEngineRuntime.ensureLoaded(this);
            int themeRes = plugin.resolveActivityTheme(targetClass);
            PluginContextWrapper pluginCtx = new PluginContextWrapper(this, plugin, themeRes);

            Class<?> cls = plugin.getClassLoader().loadClass(targetClass);
            Activity target = (Activity) cls.newInstance();

            attachFallbackDelegate(target, plugin, pluginCtx, themeRes);

            Method onCreateMethod = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(target, savedInstanceState);

            this.fallbackDelegateActivity = target;
        } catch (Throwable t) {
            Log.e(TAG, "Fallback container failed to launch " + targetClass + ": " + t.getMessage(), t);
            Toast.makeText(this, R.string.failed_to_launch_main_apk, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void attachFallbackDelegate(Activity target,
                                        PrivateEngineRuntime.LoadedPlugin plugin,
                                        PluginContextWrapper pluginCtx,
                                        int themeRes) throws Exception {
        Field mBaseField = ContextWrapper.class.getDeclaredField("mBase");
        mBaseField.setAccessible(true);
        mBaseField.set(target, pluginCtx);

        try {
            Field mResField = ContextThemeWrapper.class.getDeclaredField("mResources");
            mResField.setAccessible(true);
            mResField.set(target, plugin.getResources());
        } catch (Throwable ignored) {
        }

        copyFieldValue(Activity.class, "mWindow", this, target);
        copyFieldValue(Activity.class, "mWindowManager", this, target);
        copyFieldValue(Activity.class, "mToken", this, target);
        copyFieldValue(Activity.class, "mMainThread", this, target);
        copyFieldValue(Activity.class, "mInstrumentation", this, target);
        copyFieldValue(Activity.class, "mComponent", this, target);

        if (plugin.getPluginApplication() != null) {
            try {
                Field mAppField = Activity.class.getDeclaredField("mApplication");
                mAppField.setAccessible(true);
                mAppField.set(target, plugin.getPluginApplication());
            } catch (Throwable ignored) {
            }
        }

        Window win = getWindow();
        if (win != null) {
            win.setCallback(target);
        }

        if (themeRes != 0) {
            target.setTheme(themeRes);
        }
    }

    private static void copyFieldValue(Class<?> owner, String fieldName, Object from, Object to) {
        try {
            Field f = owner.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(to, f.get(from));
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        invokeLifecycle(fallbackDelegateActivity, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        invokeLifecycle(fallbackDelegateActivity, "onResume");
    }

    @Override
    protected void onPause() {
        invokeLifecycle(fallbackDelegateActivity, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        invokeLifecycle(fallbackDelegateActivity, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        invokeLifecycle(fallbackDelegateActivity, "onDestroy");
        fallbackDelegateActivity = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (fallbackDelegateActivity != null) {
            fallbackDelegateActivity.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (fallbackDelegateActivity != null && fallbackDelegateActivity.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static void invokeLifecycle(Activity delegate, String methodName) {
        if (delegate == null) return;
        try {
            Method m = Activity.class.getDeclaredMethod(methodName);
            m.setAccessible(true);
            m.invoke(delegate);
        } catch (Throwable ignored) {
        }
    }
}
