package com.mohammad.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class BrightnessService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    SharedPreferences sp;
    WindowManager wm;
    View dim;
    Toast toast;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sp = getSharedPreferences("launcher", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        sp.registerOnSharedPreferenceChangeListener(this);
        sp.edit().putBoolean("svc_on", true).apply();
        if (sp.contains("bright")) apply(sp.getInt("bright", 100));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences s, String key) {
        if ("bright".equals(key)) apply(s.getInt("bright", 100));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    protected boolean onKeyEvent(KeyEvent ev) {
        int k = ev.getKeyCode();
        if (k != KeyEvent.KEYCODE_CHANNEL_UP && k != KeyEvent.KEYCODE_CHANNEL_DOWN) return false;
        if (ev.getAction() == KeyEvent.ACTION_DOWN) {
            int lvl = sp.getInt("bright", 100) + (k == KeyEvent.KEYCODE_CHANNEL_UP ? 10 : -10);
            if (lvl < 10) lvl = 10;
            if (lvl > 100) lvl = 100;
            sp.edit().putInt("bright", lvl).apply();
            apply(lvl);
            if (toast != null) toast.cancel();
            toast = Toast.makeText(this, "💡 السطوع " + lvl + "%", Toast.LENGTH_SHORT);
            toast.show();
        }
        return true;
    }

    void apply(int lvl) {
        boolean sysOk = false;
        if (Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                        Math.round(lvl * 255f / 100f));
                sysOk = true;
            } catch (Exception ignored) { }
        }
        setDim(sysOk ? 0f : (100 - lvl) / 90f * 0.6f);
    }

    void setDim(float a) {
        try {
            if (a <= 0f) {
                if (dim != null) {
                    wm.removeView(dim);
                    dim = null;
                }
                return;
            }
            int color = Color.argb((int) (a * 255), 0, 0, 0);
            if (dim == null) {
                dim = new View(this);
                dim.setBackgroundColor(color);
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT);
                wm.addView(dim, lp);
            } else {
                dim.setBackgroundColor(color);
            }
        } catch (Exception ignored) { }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {
            sp.unregisterOnSharedPreferenceChangeListener(this);
            sp.edit().putBoolean("svc_on", false).apply();
            setDim(0f);
        } catch (Exception ignored) { }
        return super.onUnbind(intent);
    }
}
