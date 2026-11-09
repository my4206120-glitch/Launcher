package com.mohammad.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class BrightnessService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** يرفع خطوة واحدة (5) أو ينزل خطوة، ويثبّت الرقم على مضاعفات الخمسة */
    public static int snap(int cur, int dir) {
        if (dir > 0) return Math.min(100, (cur / 5 + 1) * 5);
        return Math.max(0, ((cur + 4) / 5 - 1) * 5);
    }

    /** رقم السطوع فقط (0 إلى 100) داخل كبسولة داكنة */
    public static class BarView extends View {
        int level = 100;
        final float d;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        public BarView(Context c) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
        }

        public void setLevel(int l) {
            level = l;
            invalidate();
        }

        @Override
        protected void onMeasure(int a, int b) {
            setMeasuredDimension((int) (150 * d), (int) (84 * d));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xE6101820);
            c.drawRoundRect(new RectF(0, 0, w, h), h / 2f, h / 2f, p);
            p.setColor(0xFFFFFFFF);
            p.setTextSize(h * 0.5f);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(String.valueOf(level), w / 2f, h / 2f + h * 0.18f, p);
        }
    }

    SharedPreferences sp;
    WindowManager wm;
    View dim;
    BarView bar;
    BroadcastReceiver screenReceiver;
    int lastApplied = -1;
    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable hideBar = new Runnable() {
        @Override
        public void run() {
            removeBar();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        try {
            sp = getSharedPreferences("launcher", MODE_PRIVATE);
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            sp.registerOnSharedPreferenceChangeListener(this);
            sp.edit().putBoolean("svc_on", true).putBoolean("svc_want", true)
                    .putLong("svc_conn", System.currentTimeMillis()).apply();
            screenReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent i) {
                    resetBrightness();
                }
            };
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_SCREEN_OFF);
            f.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(screenReceiver, f);
            if (sp.contains("bright")) apply(sp.getInt("bright", 100));
        } catch (Exception ignored) { }
    }

    /** عند إطفاء الشاشة يعود السطوع إلى 100 */
    void resetBrightness() {
        try {
            sp.edit().putInt("bright", 100).apply();
            apply(100);
        } catch (Exception ignored) { }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences s, String key) {
        try {
            if ("bright".equals(key)) {
                int lvl = s.getInt("bright", 100);
                if (lvl != lastApplied) apply(lvl);
            }
        } catch (Exception ignored) { }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    protected boolean onKeyEvent(KeyEvent ev) {
        try {
            int k = ev.getKeyCode();
            boolean down = ev.getAction() == KeyEvent.ACTION_DOWN;

            if (down) {
                sp.edit().putInt("svc_keys", sp.getInt("svc_keys", 0) + 1)
                        .putString("svc_lastkey", KeyEvent.keyCodeToString(k))
                        .putLong("svc_lastkey_t", System.currentTimeMillis()).apply();
            }

            if (sp.getBoolean("learn_key", false)) {
                if (k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_HOME
                        || k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER
                        || k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN
                        || k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return false;
                }
                if (down) {
                    sp.edit().putInt("wifi_key", k).putBoolean("learn_key", false).apply();
                }
                return true;
            }

            int wk = sp.getInt("wifi_key", 0);
            if (wk != 0 && k == wk) {
                if (down && ev.getRepeatCount() == 0) openWifi();
                return true;
            }

            if (k == KeyEvent.KEYCODE_CHANNEL_UP || k == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                if (down) {
                    int lvl = snap(sp.getInt("bright", 100), k == KeyEvent.KEYCODE_CHANNEL_UP ? 1 : -1);
                    sp.edit().putInt("bright", lvl).apply();
                    apply(lvl);
                    showBar(lvl);
                }
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    void openWifi() {
        try {
            Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(Settings.ACTION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception e2) {
                Toast.makeText(this, "تعذر فتح إعدادات الواي فاي", Toast.LENGTH_SHORT).show();
            }
        }
    }

    void apply(int lvl) {
        if (lvl < 0) lvl = 0;
        if (lvl > 100) lvl = 100;
        lastApplied = lvl;
        boolean sysOk = false;
        if (Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                        Math.max(1, Math.round(lvl * 255f / 100f)));
                sysOk = true;
            } catch (Exception ignored) { }
        }
        setDim(sysOk ? 0f : (100 - lvl) / 100f * 0.6f);
    }

    WindowManager.LayoutParams overlayParams(int w, int h) {
        return new WindowManager.LayoutParams(w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
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
                wm.addView(dim, overlayParams(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT));
            } else {
                dim.setBackgroundColor(color);
            }
        } catch (Exception ignored) { }
    }

    void showBar(int lvl) {
        try {
            if (bar == null) {
                bar = new BarView(this);
                WindowManager.LayoutParams lp = overlayParams(WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
                lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                lp.y = (int) (60 * getResources().getDisplayMetrics().density);
                wm.addView(bar, lp);
            }
            bar.setLevel(lvl);
            handler.removeCallbacks(hideBar);
            handler.postDelayed(hideBar, 1600);
        } catch (Exception ignored) { }
    }

    void removeBar() {
        try {
            if (bar != null) {
                wm.removeView(bar);
                bar = null;
            }
        } catch (Exception ignored) { }
    }

    void cleanup(String reason) {
        try {
            handler.removeCallbacks(hideBar);
            removeBar();
            setDim(0f);
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver);
                screenReceiver = null;
            }
            if (sp != null) {
                sp.unregisterOnSharedPreferenceChangeListener(this);
                sp.edit().putBoolean("svc_on", false)
                        .putLong("svc_stop", System.currentTimeMillis())
                        .putString("svc_reason", reason).apply();
            }
        } catch (Exception ignored) { }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanup("unbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanup("destroy");
        super.onDestroy();
    }
}
