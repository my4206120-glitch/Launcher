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
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** خدمة الأزرار: CH للسطوع، وزر للواي فاي، ومقياس سرعة اختياري فوق كل التطبيقات. أي زر آخر يمر دون معالجة. */
public class BrightnessService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** نسخة الخدمة العاملة، ليستعملها زر إطفاء الشاشة في الواجهة */
    public static BrightnessService instance;

    /** يرفع خطوة واحدة (5) أو ينزل خطوة، ويثبّت الرقم على مضاعفات الخمسة */
    public static int snap(int cur, int dir) {
        if (dir > 0) return Math.min(100, (cur / 5 + 1) * 5);
        return Math.max(0, ((cur + 4) / 5 - 1) * 5);
    }

    /** الرقم والوحدة منفصلين: مثل مقياس السرعة في هواتف شاومي */
    public static String[] speedParts(long bps) {
        if (bps < 0) bps = 0;
        if (bps >= 1048576L) {
            return new String[]{String.format(Locale.US, "%.1f", bps / 1048576.0), "MB/s"};
        }
        return new String[]{String.valueOf(bps / 1024L), "KB/s"};
    }

    /** إجمالي البايتات المستلمة والمرسلة: من TrafficStats، وإن لم تدعمها الشاشة فمن /proc/net/dev */
    public static long[] readNet() {
        long rx = TrafficStats.getTotalRxBytes();
        long tx = TrafficStats.getTotalTxBytes();
        if (rx != TrafficStats.UNSUPPORTED && tx != TrafficStats.UNSUPPORTED) {
            return new long[]{rx, tx};
        }
        try {
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader("/proc/net/dev"));
            String line;
            long srx = 0, stx = 0;
            boolean any = false;
            while ((line = r.readLine()) != null) {
                int c = line.indexOf(':');
                if (c < 0) continue;
                String name = line.substring(0, c).trim();
                if (name.equals("lo") || name.startsWith("tun") || name.startsWith("dummy")) continue;
                String[] f = line.substring(c + 1).trim().split("\\s+");
                if (f.length < 9) continue;
                srx += Long.parseLong(f[0]);
                stx += Long.parseLong(f[8]);
                any = true;
            }
            r.close();
            if (any) return new long[]{srx, stx};
        } catch (Exception ignored) { }
        return new long[]{-1, -1};
    }

    /** رقم السطوع فقط (0 إلى 100) داخل كبسولة داكنة صغيرة */
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
            setMeasuredDimension((int) (104 * d), (int) (54 * d));
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
    TextView speedView;
    boolean speedShowing = false;
    long lastRx = 0, lastT = 0;
    BroadcastReceiver screenReceiver;
    int cur = 100;
    int keys = 0;
    int lastApplied = -1;
    int wifiKey = 0;
    String learning = "";
    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable hideBar = new Runnable() {
        @Override
        public void run() {
            removeBar();
        }
    };
    final Runnable speedTick = new Runnable() {
        @Override
        public void run() {
            if (!speedShowing) return;
            updateSpeedText();
            handler.postDelayed(this, 1000);
        }
    };

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        try {
            instance = this;
            sp = getSharedPreferences("launcher", MODE_PRIVATE);
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            cur = sp.getInt("bright", 100);
            keys = sp.getInt("svc_keys", 0);
            wifiKey = sp.getInt("wifi_key", 0);
            learning = sp.getString("learn_key", "");
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
            if (sp.contains("bright")) apply(cur);
            updateSpeedState();
        } catch (Exception ignored) { }
    }

    /** عند إطفاء الشاشة يعود السطوع إلى 100 */
    void resetBrightness() {
        try {
            cur = 100;
            sp.edit().putInt("bright", 100).apply();
            apply(100);
        } catch (Exception ignored) { }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences s, String key) {
        try {
            if ("bright".equals(key)) {
                cur = s.getInt("bright", 100);
                if (cur != lastApplied) apply(cur);
            } else if ("wifi_key".equals(key)) {
                wifiKey = s.getInt("wifi_key", 0);
            } else if ("learn_key".equals(key)) {
                learning = s.getString("learn_key", "");
            } else if ("speed_overlay".equals(key) || "launcher_fg".equals(key)) {
                updateSpeedState();
            }
        } catch (Exception ignored) { }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    boolean isNavKey(int k) {
        return k == KeyEvent.KEYCODE_BACK || k == KeyEvent.KEYCODE_HOME
                || k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER
                || k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN
                || k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    /** الأزرار العادية تمر فوراً دون أي قراءة أو كتابة */
    @Override
    protected boolean onKeyEvent(KeyEvent ev) {
        int k = ev.getKeyCode();
        boolean down = ev.getAction() == KeyEvent.ACTION_DOWN;

        if (k == KeyEvent.KEYCODE_CHANNEL_UP || k == KeyEvent.KEYCODE_CHANNEL_DOWN) {
            if (sp == null) return false;
            try {
                if (down) {
                    int lvl = snap(cur, k == KeyEvent.KEYCODE_CHANNEL_UP ? 1 : -1);
                    cur = lvl;
                    keys++;
                    sp.edit().putInt("bright", lvl).putInt("svc_keys", keys)
                            .putLong("svc_lastkey_t", System.currentTimeMillis()).apply();
                    apply(lvl);
                    showBar(lvl);
                }
            } catch (Exception ignored) { }
            return true;
        }

        if (!learning.isEmpty()) {
            if (isNavKey(k)) return false;
            try {
                if (down) {
                    sp.edit().putInt("wifi_key", k).remove("learn_key").apply();
                    wifiKey = k;
                    learning = "";
                }
            } catch (Exception ignored) { }
            return true;
        }

        if (wifiKey != 0 && k == wifiKey) {
            if (down && ev.getRepeatCount() == 0) openWifi();
            return true;
        }
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

    /** مقياس السرعة فوق التطبيقات: يظهر إن فعّلته وكانت الواجهة ليست في المقدمة */
    void updateSpeedState() {
        boolean want = sp.getBoolean("speed_overlay", false) && !sp.getBoolean("launcher_fg", false);
        if (want && !speedShowing) showSpeed();
        else if (!want && speedShowing) hideSpeed();
    }

    void showSpeed() {
        try {
            speedView = new TextView(this);
            speedView.setTextColor(0xFFFFFFFF);
            speedView.setTextSize(16);
            speedView.setGravity(Gravity.CENTER);
            speedView.setShadowLayer(5f, 0f, 1f, 0xCC000000);
            speedView.setText("0\nKB/s");
            WindowManager.LayoutParams lp = overlayParams(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.TOP | Gravity.RIGHT;
            lp.x = dp(24);
            lp.y = dp(16);
            wm.addView(speedView, lp);
            speedShowing = true;
            lastT = 0;
            handler.post(speedTick);
        } catch (Exception e) {
            speedShowing = false;
        }
    }

    void updateSpeedText() {
        long[] net = readNet();
        long rx = net[0];
        long now = System.nanoTime();
        if (rx < 0) {
            speedView.setText("—");
            return;
        }
        if (lastT != 0) {
            double dt = (now - lastT) / 1e9;
            if (dt > 0) {
                long dr = (long) ((rx - lastRx) / dt);
                String[] ps = speedParts(dr);
                String text = ps[0] + "\n" + ps[1];
                SpannableString ss = new SpannableString(text);
                ss.setSpan(new RelativeSizeSpan(0.6f), ps[0].length() + 1, text.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                speedView.setText(ss);
            }
        }
        lastRx = rx;
        lastT = now;
    }

    void hideSpeed() {
        speedShowing = false;
        handler.removeCallbacks(speedTick);
        try {
            if (speedView != null) {
                wm.removeView(speedView);
                speedView = null;
            }
        } catch (Exception ignored) { }
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
                lp.y = dp(60);
                wm.addView(bar, lp);
            }
            bar.setLevel(lvl);
            handler.removeCallbacks(hideBar);
            handler.postDelayed(hideBar, 1500);
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
            instance = null;
            handler.removeCallbacks(hideBar);
            removeBar();
            hideSpeed();
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
