package com.mohammad.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    static final int REQ_IMG = 101;
    static final int[][] PD = {
        {0xFF0F2A33, 0xFF1D4B52}, {0xFF1B1035, 0xFF3B2A6B}, {0xFF2A1210, 0xFF5C2A1E},
        {0xFF0E1E14, 0xFF22513A}, {0xFF121212, 0xFF2B2B2B}};
    static final int[][] PL = {
        {0xFFE9F3F5, 0xFFB9D8DE}, {0xFFEFE9F8, 0xFFCFC2EC}, {0xFFF8EDE6, 0xFFEBC2AE},
        {0xFFE8F4EC, 0xFFB7DEC5}, {0xFFF5F5F5, 0xFFDADADA}};
    static final String[] PN = {"أزرق بحري", "بنفسجي ليلي", "ترابي دافئ", "أخضر غامق", "رمادي فحمي"};

    interface OnPick { void on(String pkg); }

    /** أيقونات الشريط العلوي: 0 واي فاي، 1 مصدر الإشارة، 2 إعدادات */
    static class IconBtn extends View {
        final int type;
        boolean on = true;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int color = 0xFFF4EFE6;

        IconBtn(Context c, int type) {
            super(c);
            this.type = type;
            setFocusable(true);
            setClickable(true);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float s = Math.min(w, h);
            float cx = w / 2f, cy = h / 2f;
            p.setColor(color);
            if (type == 0) {
                float r = s * 0.44f;
                float by = cy + s * 0.2f;
                RectF rf = new RectF(cx - r, by - r, cx + r, by + r);
                p.setStyle(Paint.Style.FILL);
                p.setAlpha(on ? 255 : 110);
                c.drawArc(rf, 225, 90, true, p);
                if (!on) {
                    p.setAlpha(255);
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.07f);
                    c.drawLine(cx - 0.3f * s, cy - 0.3f * s, cx + 0.3f * s, cy + 0.3f * s, p);
                }
            } else if (type == 1) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.07f);
                Path box = new Path();
                box.moveTo(cx + 0.02f * s, cy - 0.26f * s);
                box.lineTo(cx + 0.27f * s, cy - 0.26f * s);
                box.lineTo(cx + 0.27f * s, cy + 0.26f * s);
                box.lineTo(cx + 0.02f * s, cy + 0.26f * s);
                c.drawPath(box, p);
                c.drawLine(cx - 0.30f * s, cy, cx + 0.12f * s, cy, p);
                Path ar = new Path();
                ar.moveTo(cx - 0.02f * s, cy - 0.13f * s);
                ar.lineTo(cx + 0.12f * s, cy);
                ar.lineTo(cx - 0.02f * s, cy + 0.13f * s);
                c.drawPath(ar, p);
            } else {
                Path g = new Path();
                int teeth = 8;
                float rb = s * 0.30f, rt = s * 0.40f;
                double step = Math.toRadians(360.0 / teeth);
                for (int k = 0; k < teeth; k++) {
                    double a0 = k * step;
                    double bw = step * 0.30, tw = step * 0.17;
                    double[] angs = {a0 - bw, a0 - tw, a0 + tw, a0 + bw};
                    float[] rads = {rb, rt, rt, rb};
                    for (int j = 0; j < 4; j++) {
                        float x = cx + (float) (Math.cos(angs[j]) * rads[j]);
                        float y = cy + (float) (Math.sin(angs[j]) * rads[j]);
                        if (k == 0 && j == 0) g.moveTo(x, y);
                        else g.lineTo(x, y);
                    }
                    double mid = a0 + step / 2;
                    g.lineTo(cx + (float) (Math.cos(mid) * rb), cy + (float) (Math.sin(mid) * rb));
                }
                g.close();
                g.addCircle(cx, cy, s * 0.14f, Path.Direction.CCW);
                g.setFillType(Path.FillType.EVEN_ODD);
                p.setStyle(Paint.Style.FILL);
                c.drawPath(g, p);
            }
        }
    }

    /** أيقونة الطقس مرسومة بالكود */
    static class WIcon extends View {
        int code = 3;
        boolean night = false;
        int cloudColor = 0xFFEAF0F6;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        WIcon(Context c) {
            super(c);
            p.setStrokeCap(Paint.Cap.ROUND);
        }

        void set(int code, boolean night) {
            this.code = code;
            this.night = night;
            invalidate();
        }

        static int kind(int c) {
            if (c == 0) return 0;
            if (c == 1 || c == 2) return 1;
            if (c == 3) return 2;
            if (c == 45 || c == 48) return 3;
            if ((c >= 51 && c <= 67) || (c >= 80 && c <= 82)) return 4;
            if ((c >= 71 && c <= 77) || c == 85 || c == 86) return 5;
            if (c >= 95) return 6;
            return 2;
        }

        void cloud(Canvas c, float s, float ox, float oy, float sc) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(cloudColor);
            c.drawCircle((ox + 0.32f * sc) * s, (oy + 0.58f * sc) * s, 0.15f * sc * s, p);
            c.drawCircle((ox + 0.50f * sc) * s, (oy + 0.46f * sc) * s, 0.20f * sc * s, p);
            c.drawCircle((ox + 0.70f * sc) * s, (oy + 0.57f * sc) * s, 0.15f * sc * s, p);
            RectF r = new RectF((ox + 0.18f * sc) * s, (oy + 0.55f * sc) * s,
                    (ox + 0.84f * sc) * s, (oy + 0.74f * sc) * s);
            c.drawRoundRect(r, 0.09f * sc * s, 0.09f * sc * s, p);
        }

        void sun(Canvas c, float s, float cx, float cy, float r) {
            p.setColor(0xFFFFC845);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx * s, cy * s, r * s, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.045f);
            for (int k = 0; k < 8; k++) {
                double a = Math.toRadians(k * 45);
                float x1 = (cx + (float) Math.cos(a) * r * 1.45f) * s;
                float y1 = (cy + (float) Math.sin(a) * r * 1.45f) * s;
                float x2 = (cx + (float) Math.cos(a) * r * 1.85f) * s;
                float y2 = (cy + (float) Math.sin(a) * r * 1.85f) * s;
                c.drawLine(x1, y1, x2, y2, p);
            }
        }

        void moon(Canvas c, float s, float cx, float cy, float r) {
            Path m = new Path();
            m.addCircle(cx * s, cy * s, r * s, Path.Direction.CCW);
            Path cut = new Path();
            cut.addCircle((cx + 0.45f * r) * s, (cy - 0.3f * r) * s, r * 0.85f * s, Path.Direction.CCW);
            m.op(cut, Path.Op.DIFFERENCE);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFE8E4D0);
            c.drawPath(m, p);
        }

        @Override
        protected void onDraw(Canvas c) {
            float s = Math.min(getWidth(), getHeight());
            c.save();
            c.translate((getWidth() - s) / 2f, (getHeight() - s) / 2f);
            int k = kind(code);
            if (k == 0) {
                if (night) moon(c, s, 0.5f, 0.5f, 0.28f);
                else sun(c, s, 0.5f, 0.5f, 0.2f);
            } else if (k == 1) {
                if (night) moon(c, s, 0.36f, 0.36f, 0.2f);
                else sun(c, s, 0.36f, 0.36f, 0.14f);
                cloud(c, s, 0.12f, 0.1f, 0.88f);
            } else if (k == 2) {
                cloud(c, s, 0f, 0f, 1f);
            } else if (k == 3) {
                cloud(c, s, 0f, -0.1f, 1f);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0xFF9FB0C0);
                p.setStrokeWidth(s * 0.045f);
                c.drawLine(0.22f * s, 0.78f * s, 0.78f * s, 0.78f * s, p);
                c.drawLine(0.3f * s, 0.89f * s, 0.7f * s, 0.89f * s, p);
            } else if (k == 4) {
                cloud(c, s, 0f, -0.08f, 1f);
                p.setStyle(Paint.Style.STROKE);
                p.setColor(0xFF5CB8FF);
                p.setStrokeWidth(s * 0.05f);
                c.drawLine(0.38f * s, 0.7f * s, 0.32f * s, 0.86f * s, p);
                c.drawLine(0.54f * s, 0.7f * s, 0.48f * s, 0.86f * s, p);
                c.drawLine(0.7f * s, 0.7f * s, 0.64f * s, 0.86f * s, p);
            } else if (k == 5) {
                cloud(c, s, 0f, -0.08f, 1f);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFFFFFFFF);
                c.drawCircle(0.34f * s, 0.8f * s, 0.04f * s, p);
                c.drawCircle(0.52f * s, 0.9f * s, 0.04f * s, p);
                c.drawCircle(0.7f * s, 0.8f * s, 0.04f * s, p);
            } else {
                cloud(c, s, 0f, -0.1f, 1f);
                Path bolt = new Path();
                bolt.moveTo(0.54f * s, 0.6f * s);
                bolt.lineTo(0.42f * s, 0.8f * s);
                bolt.lineTo(0.51f * s, 0.8f * s);
                bolt.lineTo(0.46f * s, 0.97f * s);
                bolt.lineTo(0.65f * s, 0.72f * s);
                bolt.lineTo(0.55f * s, 0.72f * s);
                bolt.close();
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFFFFD34D);
                c.drawPath(bolt, p);
            }
            c.restore();
        }
    }

    /** أيقونة المزيد: تسع نقاط */
    static class DotsView extends View {
        int color = 0xFFF4EFE6;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        DotsView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            float s = Math.min(getWidth(), getHeight());
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            p.setColor(color);
            p.setStyle(Paint.Style.FILL);
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    c.drawCircle(cx + i * s * 0.28f, cy + j * s * 0.28f, s * 0.075f, p);
                }
            }
        }
    }

    SharedPreferences sp;
    float d;
    boolean dark;
    ImageView bgView;
    View overlay, dimView;
    TextView clockTv, dateTv, weatherTv;
    WIcon weatherIcon;
    IconBtn wifiBtn;
    LinearLayout midRow, bottomRow;
    String loadedKey = null;
    BrightnessService.BarView bar;
    View[] tiles = new View[10];
    int lastFocus = -1;
    TextView speedDown, speedUp;
    long lastRx = 0, lastTx = 0, lastT = 0;
    static boolean procMarked = false;
    boolean built = false;
    Runnable speedTicker = new Runnable() {
        @Override
        public void run() {
            updateSpeed();
            h.postDelayed(this, 1000);
        }
    };
    Runnable hideBar = new Runnable() {
        @Override
        public void run() {
            if (bar != null) bar.setVisibility(View.INVISIBLE);
        }
    };
    final List<View> glass = new ArrayList<>();
    Handler h = new Handler(Looper.getMainLooper());
    Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            h.postDelayed(this, 10000);
        }
    };

    int dp(int v) { return (int) (v * d + 0.5f); }
    int textColor() { return dark ? 0xFFF4EFE6 : 0xFF14262C; }
    int focusColor() { return dark ? 0xFFFFFFFF : 0xFF14262C; }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("launcher", MODE_PRIVATE);
        d = getResources().getDisplayMetrics().density;
        dark = sp.getBoolean("dark", true);
        if (!procMarked) {
            procMarked = true;
            sp.edit().putInt("proc_starts", sp.getInt("proc_starts", 0) + 1)
                    .putLong("proc_start_t", System.currentTimeMillis()).apply();
        }

        FrameLayout frame = new FrameLayout(this);
        bgView = new ImageView(this);
        bgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(bgView, new FrameLayout.LayoutParams(-1, -1));
        overlay = new View(this);
        frame.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(28), dp(18), dp(28), dp(18));
        frame.addView(root, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout head = new LinearLayout(this);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        head.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout timeBox = new LinearLayout(this);
        timeBox.setOrientation(LinearLayout.VERTICAL);
        clockTv = new TextView(this);
        clockTv.setTextSize(28);
        clockTv.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        dateTv = new TextView(this);
        dateTv.setTextSize(13);
        dateTv.setAlpha(0.7f);
        timeBox.addView(clockTv);
        timeBox.addView(dateTv);
        head.addView(timeBox);

        wifiBtn = iconBtn(0, v -> openWifi());
        head.addView(wifiBtn, iconLp());
        head.addView(iconBtn(2, v -> openSettings()), iconLp());

        LinearLayout wGroup = new LinearLayout(this);
        wGroup.setOrientation(LinearLayout.HORIZONTAL);
        wGroup.setGravity(Gravity.CENTER_VERTICAL);
        wGroup.setPadding(dp(12), dp(4), dp(14), dp(4));
        wGroup.setFocusable(true);
        wGroup.setClickable(true);
        wGroup.setTag("flat");
        weatherIcon = new WIcon(this);
        wGroup.addView(weatherIcon, new LinearLayout.LayoutParams(dp(38), dp(38)));
        weatherTv = new TextView(this);
        weatherTv.setTextSize(24);
        weatherTv.setPadding(dp(8), 0, 0, 0);
        wGroup.addView(weatherTv);
        wGroup.setOnClickListener(v -> {
            if (sp.getString("lat", null) == null) {
                Toast.makeText(this, "اختر المدينة من ⚙ ثم مدينة الطقس", Toast.LENGTH_LONG).show();
            } else {
                refreshWeather(true);
            }
        });
        wGroup.setOnLongClickListener(v -> {
            showForecast();
            return true;
        });
        wGroup.setOnFocusChangeListener((v, f) -> glassBg(v, f));
        glass.add(wGroup);
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(-2, -2);
        wl.setMarginStart(dp(14));
        head.addView(wGroup, wl);

        IconBtn src = iconBtn(1, v -> onSourceClick());
        src.setOnLongClickListener(v -> {
            openSourceMenu();
            return true;
        });
        head.addView(src, iconLp());

        head.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        LinearLayout speedBox = new LinearLayout(this);
        speedBox.setOrientation(LinearLayout.VERTICAL);
        speedBox.setGravity(Gravity.RIGHT);
        speedDown = new TextView(this);
        speedDown.setTextSize(16);
        speedDown.setText("↓ 0 KB/s");
        speedDown.setGravity(Gravity.RIGHT);
        speedUp = new TextView(this);
        speedUp.setTextSize(13);
        speedUp.setAlpha(0.7f);
        speedUp.setText("↑ 0 KB/s");
        speedUp.setGravity(Gravity.RIGHT);
        speedBox.addView(speedDown);
        speedBox.addView(speedUp);
        head.addView(speedBox);
        root.addView(head, new LinearLayout.LayoutParams(-1, dp(76)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        midRow = new LinearLayout(this);
        midRow.setGravity(Gravity.CENTER_HORIZONTAL);
        bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.CENTER_HORIZONTAL);
        body.addView(midRow, new LinearLayout.LayoutParams(-1, -2));
        body.addView(bottomRow, new LinearLayout.LayoutParams(-1, -2));

        dimView = new View(this);
        dimView.setFocusable(false);
        dimView.setClickable(false);
        frame.addView(dimView, new FrameLayout.LayoutParams(-1, -1));

        bar = new BrightnessService.BarView(this);
        bar.setVisibility(View.INVISIBLE);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(-2, -2,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        blp.bottomMargin = dp(60);
        frame.addView(bar, blp);

        setContentView(frame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLook();
        if (!built) build();
        else restoreFocus();
        tick();
        checkService();
        if (sp.contains("bright")) applyBrightness(sp.getInt("bright", 100));
        h.removeCallbacks(ticker);
        h.postDelayed(ticker, 10000);
        lastT = 0;
        h.removeCallbacks(speedTicker);
        h.post(speedTicker);
        refreshWeather(false);
        if (sp.getBoolean("keep", true)) startKeepAlive();
    }

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(ticker);
        h.removeCallbacks(speedTicker);
    }

    /** نحرّر صورة الخلفية في الخلفية لنوفّر الذاكرة للتطبيقات الأخرى */
    @Override
    protected void onStop() {
        super.onStop();
        bgView.setImageDrawable(null);
        loadedKey = null;
    }

    void startKeepAlive() {
        try {
            Intent i = new Intent(this, KeepAliveService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
        } catch (Exception ignored) { }
    }

    @Override
    public void onBackPressed() { }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
            stepBrightness(keyCode == KeyEvent.KEYCODE_CHANNEL_UP ? 1 : -1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    void stepBrightness(int dir) {
        int lvl = BrightnessService.snap(sp.getInt("bright", 100), dir);
        sp.edit().putInt("bright", lvl).apply();
        applyBrightness(lvl);
        bar.setLevel(lvl);
        bar.setVisibility(View.VISIBLE);
        h.removeCallbacks(hideBar);
        h.postDelayed(hideBar, 1600);
    }

    IconBtn iconBtn(int type, View.OnClickListener l) {
        final IconBtn b = new IconBtn(this, type);
        b.setOnClickListener(l);
        b.setOnFocusChangeListener((v, f) -> glassBg(v, f));
        glass.add(b);
        return b;
    }

    LinearLayout.LayoutParams iconLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(44), dp(44));
        p.setMarginStart(dp(16));
        return p;
    }

    void glassBg(View v, boolean focused) {
        boolean flat = v instanceof IconBtn || "flat".equals(v.getTag());
        if (flat) {
            if (focused) {
                GradientDrawable g = new GradientDrawable();
                g.setShape(v instanceof IconBtn ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
                g.setCornerRadius(dp(22));
                g.setColor(dark ? 0x33FFFFFF : 0x33000000);
                g.setStroke(dp(2), focusColor());
                v.setBackground(g);
            } else {
                v.setBackground(null);
            }
            return;
        }
        int fill = dark ? 0x30FFFFFF : 0xB0FFFFFF;
        int stroke = dark ? 0x45FFFFFF : 0x40000000;
        bg(v, focused, focused ? (dark ? 0x4DFFFFFF : 0xE6FFFFFF) : fill, stroke);
    }

    void bg(View v, boolean focused, int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(18));
        g.setStroke(dp(focused ? 4 : 1), focused ? focusColor() : stroke);
        v.setBackground(g);
    }

    int moonId() {
        return getResources().getIdentifier("moon", "drawable", getPackageName());
    }

    void applyLook() {
        int tc = textColor();
        clockTv.setTextColor(tc);
        dateTv.setTextColor(tc);
        weatherTv.setTextColor(tc);
        speedDown.setTextColor(tc);
        speedUp.setTextColor(tc);
        weatherIcon.cloudColor = dark ? 0xFFEAF0F6 : 0xFF6F8397;
        weatherIcon.invalidate();
        for (View v : glass) {
            glassBg(v, v.hasFocus());
            if (v instanceof IconBtn) {
                ((IconBtn) v).color = tc;
                v.invalidate();
            }
        }
        String mode = sp.getString("wp_mode", "moon");
        String uri = sp.getString("wp_uri", null);
        int idx = sp.getInt("wp_idx", 0);
        if (idx < 0 || idx >= PD.length) idx = 0;
        bgView.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                dark ? PD[idx] : PL[idx]));
        if ("moon".equals(mode) && moonId() != 0) {
            overlay.setBackgroundColor(dark ? 0x33000000 : 0x99FFFFFF);
            if (!"moon".equals(loadedKey)) {
                loadedKey = "moon";
                loadImage(null, moonId());
            }
        } else if ("photo".equals(mode) && uri != null) {
            overlay.setBackgroundColor(dark ? 0x77000000 : 0x66FFFFFF);
            if (!uri.equals(loadedKey)) {
                loadedKey = uri;
                loadImage(Uri.parse(uri), 0);
            }
        } else {
            loadedKey = null;
            bgView.setImageDrawable(null);
            overlay.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    void loadImage(final Uri uri, final int resId) {
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        new Thread(() -> {
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                o.inScaled = false;
                if (uri != null) {
                    InputStream in = getContentResolver().openInputStream(uri);
                    BitmapFactory.decodeStream(in, null, o);
                    in.close();
                } else {
                    BitmapFactory.decodeResource(getResources(), resId, o);
                }
                int maxW = Math.min(1280, dm.widthPixels);
                int maxH = Math.min(720, dm.heightPixels);
                int s = 1;
                while (o.outWidth / (s * 2) >= maxW && o.outHeight / (s * 2) >= maxH) s *= 2;
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = s;
                o2.inScaled = false;
                o2.inPreferredConfig = Bitmap.Config.RGB_565;
                final Bitmap bmp;
                if (uri != null) {
                    InputStream in2 = getContentResolver().openInputStream(uri);
                    bmp = BitmapFactory.decodeStream(in2, null, o2);
                    in2.close();
                } else {
                    bmp = BitmapFactory.decodeResource(getResources(), resId, o2);
                }
                runOnUiThread(() -> {
                    if (bmp != null) bgView.setImageBitmap(bmp);
                });
            } catch (Exception e) {
                loadedKey = null;
                runOnUiThread(() -> Toast.makeText(this, "تعذر فتح الصورة", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    boolean canWriteSystem() {
        return Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this);
    }

    void applyBrightness(int lvl) {
        if (lvl < 0) lvl = 0;
        if (lvl > 100) lvl = 100;
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (sp.getBoolean("svc_on", false)) {
            sp.edit().putInt("bright", lvl).apply();
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            getWindow().setAttributes(lp);
            dimView.setBackgroundColor(Color.TRANSPARENT);
            return;
        }
        boolean sysOk = false;
        if (canWriteSystem()) {
            try {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                        Math.max(1, Math.round(lvl * 255f / 100f)));
                sysOk = true;
            } catch (Exception ignored) { }
        }
        float dim = 0f;
        if (sysOk) {
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            lp.screenBrightness = lvl >= 100
                    ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    : Math.max(0.02f, lvl / 100f);
            dim = (100 - lvl) / 100f * 0.55f;
        }
        getWindow().setAttributes(lp);
        dimView.setBackgroundColor(Color.argb((int) (dim * 255), 0, 0, 0));
    }

    void brightnessDialog() {
        if (!canWriteSystem() && !sp.getBoolean("perm_skip", false)
                && !sp.getBoolean("svc_on", false)) {
            new AlertDialog.Builder(this)
                    .setTitle("سطوع كل التطبيقات")
                    .setMessage("ليعمل السطوع داخل كل التطبيقات اسمح لواجهتي بتعديل إعدادات النظام. بدونه يعمل السطوع داخل الواجهة فقط.")
                    .setPositiveButton("فتح الإعدادات", (dlg, w) -> allowWriteSettings())
                    .setNegativeButton("لاحقاً", (dlg, w) -> {
                        sp.edit().putBoolean("perm_skip", true).apply();
                        showBrightnessSlider();
                    })
                    .show();
            return;
        }
        showBrightnessSlider();
    }

    void allowWriteSettings() {
        sp.edit().remove("perm_skip").apply();
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            sp.edit().putBoolean("perm_skip", true).apply();
            Toast.makeText(this, "الشاشة لا تدعم هذا الإذن", Toast.LENGTH_LONG).show();
        }
    }

    void openAccessibility() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "الشاشة لا تدعم هذه الإعدادات", Toast.LENGTH_LONG).show();
        }
    }

    void openHomeSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
            } catch (Exception e2) {
                Toast.makeText(this, "افتح الإعدادات ثم التطبيقات ثم التطبيقات الافتراضية ثم الشاشة الرئيسية", Toast.LENGTH_LONG).show();
            }
        }
    }

    void showBrightnessSlider() {
        int cur = sp.getInt("bright", 100);
        final TextView tv = new TextView(this);
        tv.setTextSize(20);
        tv.setText("السطوع: " + cur + "%");
        tv.setPadding(0, 0, 0, dp(12));
        final SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(cur);
        sb.setKeyProgressIncrement(5);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int lvl = p;
                tv.setText("السطوع: " + lvl + "%");
                applyBrightness(lvl);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) { }

            @Override
            public void onStopTrackingTouch(SeekBar s) { }
        });
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(16), dp(24), dp(8));
        box.addView(tv);
        box.addView(sb, new LinearLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(this)
                .setTitle("السطوع")
                .setView(box)
                .setPositiveButton("تم", null)
                .setOnDismissListener(dlg -> sp.edit().putInt("bright", sb.getProgress()).apply())
                .show();
    }

    void tick() {
        Date now = new Date();
        clockTv.setText(new SimpleDateFormat("hh:mm a", new Locale("ar")).format(now));
        dateTv.setText(new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar")).format(now));
        updateWifi();
    }

    void updateWifi() {
        boolean on = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            on = ni != null && ni.isConnected();
        } catch (Exception ignored) { }
        if (wifiBtn.on != on) {
            wifiBtn.on = on;
            wifiBtn.invalidate();
        }
    }

    String http(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }

    String desc(int c) {
        int k = WIcon.kind(c);
        if (k == 0) return "صافٍ";
        if (k == 1) return "غائم جزئياً";
        if (k == 2) return "غائم";
        if (k == 3) return "ضباب";
        if (k == 4) return "مطر";
        if (k == 5) return "ثلج";
        return "عاصفة رعدية";
    }

    void showWeather(String json) {
        try {
            JSONObject cur = new JSONObject(json).getJSONObject("current");
            int t = (int) Math.round(cur.getDouble("temperature_2m"));
            int code = cur.getInt("weather_code");
            boolean night = cur.optInt("is_day", 1) == 0;
            weatherIcon.set(code, night);
            weatherTv.setText(t + "°");
        } catch (Exception e) {
            weatherTv.setText("--");
        }
    }

    void refreshWeather(boolean force) {
        final String lat = sp.getString("lat", null);
        final String lon = sp.getString("lon", null);
        String cached = sp.getString("w_json", null);
        if (cached != null) showWeather(cached);
        if (lat == null || lon == null) {
            if (cached == null) {
                weatherIcon.set(3, false);
                weatherTv.setText("--");
            }
            return;
        }
        long last = sp.getLong("w_time", 0);
        if (!force && cached != null && System.currentTimeMillis() - last < 20 * 60 * 1000) return;
        new Thread(() -> {
            try {
                final String json = http("https://api.open-meteo.com/v1/forecast?latitude=" + lat
                        + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code,is_day"
                        + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                        + "&forecast_days=5&timezone=auto");
                new JSONObject(json).getJSONObject("current");
                sp.edit().putString("w_json", json).putLong("w_time", System.currentTimeMillis()).apply();
                runOnUiThread(() -> showWeather(json));
            } catch (Exception e) {
                // نُبقي آخر قيمة محفوظة
            }
        }).start();
    }

    void showForecast() {
        String json = sp.getString("w_json", null);
        if (json == null) {
            Toast.makeText(this, "لا توجد بيانات طقس بعد. اختر المدينة من ⚙", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONObject cur = root.getJSONObject("current");
            JSONObject daily = root.getJSONObject("daily");
            JSONArray days = daily.getJSONArray("time");
            JSONArray codes = daily.getJSONArray("weather_code");
            JSONArray mx = daily.getJSONArray("temperature_2m_max");
            JSONArray mn = daily.getJSONArray("temperature_2m_min");

            final Dialog dlg = new Dialog(this);
            dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
            LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            panel.setGravity(Gravity.CENTER_HORIZONTAL);
            panel.setPadding(dp(28), dp(22), dp(28), dp(22));
            GradientDrawable pg = new GradientDrawable();
            pg.setColor(0xF2101820);
            pg.setCornerRadius(dp(24));
            pg.setStroke(dp(1), 0x55FFFFFF);
            panel.setBackground(pg);

            TextView city = new TextView(this);
            city.setText(sp.getString("city", ""));
            city.setTextSize(18);
            city.setTextColor(0xCCF4EFE6);
            city.setGravity(Gravity.CENTER);
            panel.addView(city);

            LinearLayout now = new LinearLayout(this);
            now.setGravity(Gravity.CENTER);
            now.setPadding(0, dp(6), 0, dp(14));
            WIcon ni = new WIcon(this);
            ni.set(cur.getInt("weather_code"), cur.optInt("is_day", 1) == 0);
            now.addView(ni, new LinearLayout.LayoutParams(dp(64), dp(64)));
            TextView nt = new TextView(this);
            nt.setText((int) Math.round(cur.getDouble("temperature_2m")) + "°");
            nt.setTextSize(46);
            nt.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            nt.setTextColor(0xFFF4EFE6);
            nt.setPadding(dp(14), 0, dp(14), 0);
            now.addView(nt);
            TextView nd = new TextView(this);
            nd.setText(desc(cur.getInt("weather_code")));
            nd.setTextSize(20);
            nd.setTextColor(0xCCF4EFE6);
            now.addView(nd);
            panel.addView(now);

            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("EEEE", new Locale("ar"));
            int n = Math.min(5, days.length());
            for (int i = 0; i < n; i++) {
                LinearLayout col = new LinearLayout(this);
                col.setOrientation(LinearLayout.VERTICAL);
                col.setGravity(Gravity.CENTER);
                col.setPadding(dp(14), dp(10), dp(14), dp(10));
                GradientDrawable cg = new GradientDrawable();
                cg.setColor(i == 0 ? 0x26FFFFFF : 0x10FFFFFF);
                cg.setCornerRadius(dp(16));
                col.setBackground(cg);

                TextView dn = new TextView(this);
                String name;
                if (i == 0) {
                    name = "اليوم";
                } else {
                    try {
                        name = out.format(in.parse(days.getString(i)));
                    } catch (Exception e) {
                        name = days.getString(i);
                    }
                }
                dn.setText(name);
                dn.setTextSize(16);
                dn.setTextColor(0xFFF4EFE6);
                dn.setGravity(Gravity.CENTER);
                col.addView(dn);

                WIcon wi = new WIcon(this);
                wi.set(codes.getInt(i), false);
                LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(dp(48), dp(48));
                wlp.setMargins(0, dp(6), 0, dp(6));
                col.addView(wi, wlp);

                TextView hi = new TextView(this);
                hi.setText((int) Math.round(mx.getDouble(i)) + "°");
                hi.setTextSize(22);
                hi.setTextColor(0xFFF4EFE6);
                hi.setGravity(Gravity.CENTER);
                col.addView(hi);
                TextView lo = new TextView(this);
                lo.setText((int) Math.round(mn.getDouble(i)) + "°");
                lo.setTextSize(16);
                lo.setTextColor(0x99F4EFE6);
                lo.setGravity(Gravity.CENTER);
                col.addView(lo);

                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(104), -2);
                clp.setMargins(dp(5), 0, dp(5), 0);
                row.addView(col, clp);
            }
            panel.addView(row);

            dlg.setContentView(panel);
            if (dlg.getWindow() != null) {
                dlg.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            dlg.show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر عرض التوقعات", Toast.LENGTH_LONG).show();
        }
    }

    void askCity() {
        final EditText et = new EditText(this);
        et.setText(sp.getString("city", ""));
        et.setSingleLine(true);
        et.setHint("اسم المدينة");
        new AlertDialog.Builder(this)
                .setTitle("مدينة الطقس")
                .setView(et)
                .setPositiveButton("حفظ", (dlg, w) -> findCity(et.getText().toString().trim()))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    void findCity(final String name) {
        if (name.isEmpty()) return;
        Toast.makeText(this, "جارٍ البحث...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                String json = http("https://geocoding-api.open-meteo.com/v1/search?name="
                        + URLEncoder.encode(name, "UTF-8") + "&count=1&language=ar&format=json");
                JSONArray arr = new JSONObject(json).optJSONArray("results");
                if (arr == null || arr.length() == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "لم أجد المدينة", Toast.LENGTH_LONG).show());
                    return;
                }
                JSONObject o = arr.getJSONObject(0);
                sp.edit()
                        .putString("lat", String.valueOf(o.getDouble("latitude")))
                        .putString("lon", String.valueOf(o.getDouble("longitude")))
                        .putString("city", o.optString("name", name))
                        .remove("w_json")
                        .apply();
                runOnUiThread(() -> refreshWeather(true));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "تعذر الاتصال بالإنترنت", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    boolean svcEnabled() {
        try {
            String en = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return en != null && en.contains(getPackageName() + "/");
        } catch (Exception e) {
            return false;
        }
    }

    /** يعيد تفعيل الخدمة بلا تدخل منك إن مُنح التطبيق إذن WRITE_SECURE_SETTINGS (إعداد اختياري لمرة واحدة) */
    boolean enableServiceSilently() {
        try {
            String comp = getPackageName() + "/" + BrightnessService.class.getName();
            String cur = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (cur != null && cur.contains(comp)) return true;
            String val = (cur == null || cur.isEmpty()) ? comp : cur + ":" + comp;
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, val);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** إن أوقف النظام الخدمة: نعيد تفعيلها تلقائياً إن أمكن، وإلا تظهر نافذة بزر واحد */
    void checkService() {
        if (sp.getBoolean("svc_want", false) && !svcEnabled()) {
            if (enableServiceSilently()) {
                Toast.makeText(this, "أُعيد تفعيل أزرار CH تلقائياً", Toast.LENGTH_SHORT).show();
                return;
            }
            sp.edit().putBoolean("svc_on", false).putBoolean("svc_want", false).apply();
            new AlertDialog.Builder(this)
                    .setTitle("أزرار CH متوقفة")
                    .setMessage("أوقف نظام الشاشة خدمة الأزرار. فعّلها من جديد لتعمل داخل التطبيقات.")
                    .setPositiveButton("فتح إمكانية الوصول", (dlg, w) -> openAccessibility())
                    .setNegativeButton("لاحقاً", null)
                    .show();
        }
    }

    String fmt(long t) {
        return t == 0 ? "—" : new SimpleDateFormat("d/M HH:mm:ss", Locale.US).format(new Date(t));
    }

    void showServiceStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("مفعّلة في النظام: ").append(svcEnabled() ? "نعم" : "لا").append("\n");
        sb.append("متصلة الآن: ").append(sp.getBoolean("svc_on", false) ? "نعم" : "لا").append("\n");
        sb.append("آخر اتصال: ").append(fmt(sp.getLong("svc_conn", 0))).append("\n");
        sb.append("آخر توقف: ").append(fmt(sp.getLong("svc_stop", 0)))
                .append(" (").append(sp.getString("svc_reason", "—")).append(")\n");
        sb.append("عدد ضغطات CH: ").append(sp.getInt("svc_keys", 0)).append("\n");
        sb.append("آخر ضغطة CH: ").append(fmt(sp.getLong("svc_lastkey_t", 0))).append("\n");
        sb.append("مرات تشغيل الواجهة من الصفر: ").append(sp.getInt("proc_starts", 0)).append("\n");
        sb.append("آخر تشغيل من الصفر: ").append(fmt(sp.getLong("proc_start_t", 0)));
        new AlertDialog.Builder(this)
                .setTitle("حالة الخدمة")
                .setMessage(sb.toString())
                .setPositiveButton("إغلاق", null)
                .show();
    }

    void updateSpeed() {
        long[] net = BrightnessService.readNet();
        long rx = net[0], tx = net[1];
        long now = System.nanoTime();
        if (rx < 0) {
            speedDown.setText("↓ غير متاح");
            speedUp.setText("");
            return;
        }
        if (lastT != 0) {
            double dt = (now - lastT) / 1e9;
            if (dt > 0) {
                long dr = (long) ((rx - lastRx) / dt);
                long du = (long) ((tx - lastTx) / dt);
                speedDown.setText("↓ " + BrightnessService.fmtSpeed(dr));
                speedUp.setText("↑ " + BrightnessService.fmtSpeed(du));
            }
        }
        lastRx = rx;
        lastTx = tx;
        lastT = now;
    }

    void speedTest() {
        Toast.makeText(this, "جارٍ قياس السرعة...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            double mbps = -1;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(
                        "https://speed.cloudflare.com/__down?bytes=15000000").openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                InputStream in = c.getInputStream();
                byte[] buf = new byte[65536];
                long total = 0;
                long t0 = System.nanoTime();
                int n;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (System.nanoTime() - t0 > 8000000000L) break;
                }
                double sec = (System.nanoTime() - t0) / 1e9;
                in.close();
                if (sec > 0) mbps = total * 8 / sec / 1e6;
            } catch (Exception ignored) { }
            final double r = mbps;
            runOnUiThread(() -> new AlertDialog.Builder(this)
                    .setTitle("سرعة الإنترنت")
                    .setMessage(r < 0 ? "تعذر القياس. تأكد من الاتصال."
                            : String.format(Locale.US, "التحميل: %.1f ميغابت/ث", r))
                    .setPositiveButton("حسناً", null)
                    .show());
        }).start();
    }

    void learnKey(final String which, String title) {
        if (!svcEnabled()) {
            Toast.makeText(this, "فعّل أزرار CH أولاً من إمكانية الوصول، فالزر يعمل عبرها", Toast.LENGTH_LONG).show();
            openAccessibility();
            return;
        }
        final String prefKey = "wifi".equals(which) ? "wifi_key" : "speed_key";
        sp.edit().putString("learn_key", which).apply();
        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("اضغط الآن على الزر الذي تريده في الريموت.\n(لا يصلح الرجوع ولا الرئيسية ولا الموافق ولا الأسهم ولا CH)")
                .setNegativeButton("إلغاء", null)
                .setNeutralButton("حذف الزر المحفوظ", (d2, w) -> sp.edit().remove(prefKey).apply())
                .setOnDismissListener(d3 -> sp.edit().remove("learn_key").apply())
                .show();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!dlg.isShowing()) return;
                if (sp.getString("learn_key", "").isEmpty()) {
                    dlg.dismiss();
                    Toast.makeText(MainActivity.this, "تم تعيين الزر: "
                            + KeyEvent.keyCodeToString(sp.getInt(prefKey, 0)), Toast.LENGTH_LONG).show();
                } else {
                    h.postDelayed(this, 400);
                }
            }
        }, 400);
    }

    void openSettings() {
        String[] items = {
            "🎨  تغيير الخلفية",
            dark ? "☀️  الوضع الفاتح" : "🌙  الوضع الداكن",
            "📍  مدينة الطقس",
            "🔄  تحديث الطقس",
            "💡  السطوع",
            "🔐  السماح بتعديل سطوع النظام",
            "⌨️  أزرار CH للسطوع: " + (svcEnabled() ? "مفعّلة ✅" : "غير مفعّلة ❌"),
            "📋  حالة خدمة الأزرار",
            "📶  تعيين زر الواي فاي في الريموت",
            "⚡  تعيين زر سرعة الإنترنت في الريموت",
            "🚀  اختبار سرعة الإنترنت",
            "🔒  إبقاء الواجهة نشطة: " + (sp.getBoolean("keep", true) ? "مفعّل" : "معطّل"),
            "🏠  تعيين كواجهة افتراضية",
            "🔎  إعداد زر المصدر"};
        new AlertDialog.Builder(this)
                .setTitle("الإعدادات")
                .setItems(items, (dlg, which) -> {
                    if (which == 0) {
                        wallpaperMenu();
                    } else if (which == 1) {
                        dark = !dark;
                        sp.edit().putBoolean("dark", dark).apply();
                        applyLook();
                        build();
                    } else if (which == 2) {
                        askCity();
                    } else if (which == 3) {
                        refreshWeather(true);
                    } else if (which == 4) {
                        brightnessDialog();
                    } else if (which == 5) {
                        allowWriteSettings();
                    } else if (which == 6) {
                        openAccessibility();
                    } else if (which == 7) {
                        showServiceStatus();
                    } else if (which == 8) {
                        learnKey("wifi", "زر الواي فاي");
                    } else if (which == 9) {
                        learnKey("speed", "زر سرعة الإنترنت");
                    } else if (which == 10) {
                        speedTest();
                    } else if (which == 11) {
                        boolean on = !sp.getBoolean("keep", true);
                        sp.edit().putBoolean("keep", on).apply();
                        if (on) startKeepAlive();
                        else stopService(new Intent(this, KeepAliveService.class));
                        Toast.makeText(this, on ? "تم التفعيل" : "تم التعطيل", Toast.LENGTH_SHORT).show();
                    } else if (which == 12) {
                        openHomeSettings();
                    } else {
                        openSourceMenu();
                    }
                }).show();
    }

    void wallpaperMenu() {
        final String[] items = new String[PN.length + 2];
        items[0] = "🌙  القمر (الافتراضية)";
        for (int k = 0; k < PN.length; k++) items[k + 1] = PN[k];
        items[PN.length + 1] = "🖼  صورة من الملفات";
        new AlertDialog.Builder(this)
                .setTitle("الخلفية")
                .setItems(items, (dlg, which) -> {
                    if (which == 0) {
                        sp.edit().putString("wp_mode", "moon").apply();
                        loadedKey = null;
                        applyLook();
                    } else if (which <= PN.length) {
                        sp.edit().putString("wp_mode", "preset").putInt("wp_idx", which - 1).apply();
                        applyLook();
                    } else {
                        pickPhoto();
                    }
                }).show();
    }

    void pickPhoto() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(i, REQ_IMG);
        } catch (Exception e) {
            try {
                Intent g = new Intent(Intent.ACTION_GET_CONTENT);
                g.setType("image/*");
                startActivityForResult(g, REQ_IMG);
            } catch (Exception e2) {
                Toast.makeText(this, "الشاشة لا تدعم اختيار الملفات", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_IMG && res == RESULT_OK && data != null && data.getData() != null) {
            Uri u = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) { }
            sp.edit().putString("wp_uri", u.toString()).putString("wp_mode", "photo").apply();
            loadedKey = null;
            applyLook();
        }
    }

    void openWifi() {
        try {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception e2) {
                Toast.makeText(this, "تعذر فتح الإعدادات", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** ضغطة واحدة: تفتح قائمة المصدر المحفوظة مباشرة، وإن لم تُحفظ بعد تظهر قائمة الإعداد */
    void onSourceClick() {
        String cmp = sp.getString("src_cmp", null);
        if (cmp == null) {
            openSourceMenu();
        } else {
            launchComponent(cmp);
        }
    }

    boolean launchComponent(String flat) {
        try {
            ComponentName cn = ComponentName.unflattenFromString(flat);
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(cn);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح قائمة المصدر. اضغط مطولاً على الأيقونة لإعادة الإعداد", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    void openSourceMenu() {
        final List<String> labels = new ArrayList<>();
        final List<String> ids = new ArrayList<>();
        try {
            TvInputManager tm = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (tm != null) {
                for (TvInputInfo info : tm.getTvInputList()) {
                    if (info.isPassthroughInput()) {
                        labels.add("📺  " + String.valueOf(info.loadLabel(this)));
                        ids.add(info.getId());
                    }
                }
            }
        } catch (Exception ignored) { }
        labels.add("🔎  البحث عن قائمة المصدر في الشاشة");
        ids.add("#find");
        if (sp.getString("src_cmp", null) != null) {
            labels.add("🗑  حذف القائمة المحفوظة");
            ids.add("#clear");
        }
        labels.add("ℹ️  تفاصيل المداخل (للتشخيص)");
        ids.add("#diag");
        new AlertDialog.Builder(this)
                .setTitle("المصدر")
                .setItems(labels.toArray(new String[0]), (dlg, which) -> {
                    String id = ids.get(which);
                    if (id.equals("#find")) {
                        findSourceActivities();
                    } else if (id.equals("#clear")) {
                        sp.edit().remove("src_cmp").apply();
                        Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
                    } else if (id.equals("#diag")) {
                        showInputsInfo();
                    } else {
                        switchInput(id);
                    }
                }).show();
    }

    void findSourceActivities() {
        Toast.makeText(this, "جارٍ البحث...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            final List<String[]> res = new ArrayList<>();
            try {
                PackageManager pm = getPackageManager();
                List<PackageInfo> pkgs = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
                for (PackageInfo pi : pkgs) {
                    if (pi.activities == null) continue;
                    if (pi.packageName.equals(getPackageName())) continue;
                    for (ActivityInfo ai : pi.activities) {
                        if (!ai.exported) continue;
                        String n = (ai.name + " " + ai.packageName).toLowerCase(Locale.US);
                        if (n.contains("inputmethod") || n.contains("keyboard")) continue;
                        int score = 0;
                        if (n.contains("source")) score += 3;
                        if (n.contains("hdmi")) score += 2;
                        if (n.contains("signal")) score += 2;
                        if (n.contains("input")) score += 2;
                        if (score == 0) continue;
                        if (n.contains("tv")) score += 1;
                        String cls = ai.name;
                        int dot = cls.lastIndexOf('.');
                        String shortName = dot >= 0 ? cls.substring(dot + 1) : cls;
                        String label = String.valueOf(ai.loadLabel(pm));
                        res.add(new String[]{String.valueOf(100 - score),
                                label + "  ·  " + shortName,
                                new ComponentName(ai.packageName, ai.name).flattenToString()});
                    }
                }
            } catch (Exception ignored) { }
            Collections.sort(res, (a, c) -> a[0].compareTo(c[0]));
            runOnUiThread(() -> showSourceCandidates(res));
        }).start();
    }

    void showSourceCandidates(final List<String[]> res) {
        if (res.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("المصدر")
                    .setMessage("لم أجد شاشة للمصدر. جرّب خيار «تفاصيل المداخل» وابعث لي صورته.")
                    .setPositiveButton("إغلاق", null)
                    .show();
            return;
        }
        int n = Math.min(40, res.size());
        String[] labels = new String[n];
        for (int k = 0; k < n; k++) labels[k] = res.get(k)[1];
        new AlertDialog.Builder(this)
                .setTitle("اختر ثم جرّب: ما يفتح قائمة المصدر يُحفظ")
                .setItems(labels, (dlg, which) -> {
                    String flat = res.get(which)[2];
                    sp.edit().putString("src_cmp", flat).apply();
                    Toast.makeText(this, "تم الحفظ. إن لم تفتح قائمة المصدر فكرر البحث واختر غيرها", Toast.LENGTH_LONG).show();
                    launchComponent(flat);
                }).show();
    }

    void showInputsInfo() {
        StringBuilder sb = new StringBuilder();
        try {
            TvInputManager tm = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (tm == null) {
                sb.append("لا يوجد TvInputManager في هذه الشاشة");
            } else {
                List<TvInputInfo> list = tm.getTvInputList();
                sb.append("عدد المداخل: ").append(list.size()).append("\n\n");
                for (TvInputInfo info : list) {
                    sb.append(String.valueOf(info.loadLabel(this)))
                            .append("\nنوع: ").append(info.getType())
                            .append(" | مباشر: ").append(info.isPassthroughInput())
                            .append("\n").append(info.getId()).append("\n\n");
                }
            }
        } catch (Exception e) {
            sb.append("خطأ: ").append(e.toString());
        }
        new AlertDialog.Builder(this)
                .setTitle("مداخل الشاشة")
                .setMessage(sb.toString())
                .setPositiveButton("إغلاق", null)
                .show();
    }

    void switchInput(String id) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    TvContract.buildChannelUriForPassthroughInput(id)));
        } catch (Exception e) {
            Toast.makeText(this, "تعذر التبديل إلى هذا المدخل", Toast.LENGTH_LONG).show();
        }
    }

    LinearLayout.LayoutParams cellLp(int w, int hh, int m) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, hh);
        p.setMargins(m, m, m, m);
        return p;
    }

    void build() {
        midRow.removeAllViews();
        bottomRow.removeAllViews();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int m = dp(6);
        int cw = dm.widthPixels - dp(56);
        int availH = dm.heightPixels - dp(36) - dp(76) - dp(10);
        float ratio = 1.6f;
        int wS = (cw - 8 * m) / 4;
        int hS = (int) (wS / ratio);
        int wB = (cw - 10 * m) / 5;
        int hB = (int) (wB / ratio);
        if (2 * hS + hB + 6 * m > availH) {
            float f = (float) (availH - 6 * m) / (2 * hS + hB);
            wS = (int) (wS * f);
            hS = (int) (hS * f);
            wB = (int) (wB * f);
            hB = (int) (hB * f);
        }
        wS = (int) (wS * 0.95f);
        hS = (int) (hS * 0.95f);
        wB = (int) (wB * 0.95f);
        hB = (int) (hB * 0.95f);
        int wBig = 2 * wS + 2 * m;
        int hBig = 2 * hS + 2 * m;

        midRow.addView(tile(0, true, wBig, hBig), cellLp(wBig, hBig, m));
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            for (int c = 0; c < 2; c++) {
                row.addView(tile(1 + r * 2 + c, false, wS, hS), cellLp(wS, hS, m));
            }
            grid.addView(row, new LinearLayout.LayoutParams(-2, -2));
        }
        midRow.addView(grid, new LinearLayout.LayoutParams(-2, -2));
        for (int i = 5; i < 10; i++) {
            bottomRow.addView(tile(i, false, wB, hB), cellLp(wB, hB, m));
        }
        built = true;
        restoreFocus();
    }

    void restoreFocus() {
        final int fi = lastFocus >= 0 ? lastFocus : 0;
        midRow.post(() -> {
            View ft = tiles[fi];
            if (ft != null) ft.requestFocus();
        });
    }

    /** اللون الغالب في أيقونة التطبيق، لتلوين البطاقة */
    int dominant(Drawable dr) {
        try {
            int sz = 32;
            Bitmap bm = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            dr.setBounds(0, 0, sz, sz);
            dr.draw(cv);
            long r = 0, g = 0, b = 0, n = 0;
            for (int x = 0; x < sz; x++) {
                for (int y = 0; y < sz; y++) {
                    int px = bm.getPixel(x, y);
                    if (Color.alpha(px) < 200) continue;
                    r += Color.red(px);
                    g += Color.green(px);
                    b += Color.blue(px);
                    n++;
                }
            }
            if (n == 0) return 0xFF2A2F36;
            return Color.rgb((int) (r / n), (int) (g / n), (int) (b / n));
        } catch (Exception e) {
            return 0xFF2A2F36;
        }
    }

    int[] shades(int base) {
        float[] hsv = new float[3];
        Color.colorToHSV(base, hsv);
        if (hsv[1] < 0.22f) return new int[]{0xFF39424D, 0xFF1B2128};
        float[] a = hsv.clone();
        a[2] = Math.min(1f, hsv[2] * 0.95f);
        float[] bb = hsv.clone();
        bb[2] = hsv[2] * 0.55f;
        return new int[]{Color.HSVToColor(a), Color.HSVToColor(bb)};
    }

    FrameLayout appCard(String pkg, int hh, boolean big) {
        if (pkg == null) return null;
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            FrameLayout t = new FrameLayout(this);
            t.setFocusable(true);
            t.setClickable(true);
            Drawable banner = null;
            try {
                Intent li = pm.getLeanbackLaunchIntentForPackage(pkg);
                if (li == null) li = pm.getLaunchIntentForPackage(pkg);
                if (li != null) banner = pm.getActivityBanner(li);
            } catch (Exception ignored) { }
            if (banner == null) banner = pm.getApplicationBanner(ai);
            if (banner != null) {
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(banner);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setClipToOutline(true);
                iv.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View v, Outline o) {
                        o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(18));
                    }
                });
                t.addView(iv, new FrameLayout.LayoutParams(-1, -1));
            } else {
                Drawable icon = pm.getApplicationIcon(ai);
                int[] cols = shades(dominant(icon));
                GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, cols);
                gd.setCornerRadius(dp(18));
                t.setBackground(gd);
                LinearLayout box = new LinearLayout(this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setGravity(Gravity.CENTER);
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(icon);
                int s = (int) (hh * (big ? 0.56f : 0.5f));
                box.addView(iv, new LinearLayout.LayoutParams(s, s));
                TextView name = new TextView(this);
                name.setText(pm.getApplicationLabel(ai));
                name.setTextSize(big ? 22 : 15);
                name.setTextColor(0xFFFFFFFF);
                name.setShadowLayer(4f, 0f, 1f, 0x99000000);
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(1);
                name.setPadding(dp(6), dp(6), dp(6), 0);
                box.addView(name);
                t.addView(box, new FrameLayout.LayoutParams(-1, -1));
            }
            return t;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    FrameLayout emptyCard(boolean big) {
        FrameLayout t = new FrameLayout(this);
        t.setFocusable(true);
        t.setClickable(true);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        TextView plus = new TextView(this);
        plus.setText("＋");
        plus.setTextSize(big ? 52 : 34);
        plus.setTextColor(textColor());
        plus.setAlpha(0.7f);
        plus.setGravity(Gravity.CENTER);
        box.addView(plus);
        TextView lbl = new TextView(this);
        lbl.setText("أضف تطبيقاً");
        lbl.setTextSize(big ? 20 : 14);
        lbl.setTextColor(textColor());
        lbl.setAlpha(0.6f);
        lbl.setGravity(Gravity.CENTER);
        box.addView(lbl);
        t.addView(box, new FrameLayout.LayoutParams(-1, -1));
        bg(t, false, dark ? 0x12FFFFFF : 0x44FFFFFF, dark ? 0x40FFFFFF : 0x40000000);
        return t;
    }

    void addRing(final FrameLayout t, final int idx) {
        final View ring = new View(this);
        t.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        final GradientDrawable ringBg = new GradientDrawable();
        ringBg.setColor(Color.TRANSPARENT);
        ringBg.setCornerRadius(dp(18));
        ringBg.setStroke(dp(4), focusColor());
        t.setOnFocusChangeListener((v, f) -> {
            if (f && idx >= 0) lastFocus = idx;
            ring.setBackground(f ? ringBg : null);
            v.animate().scaleX(f ? 1.04f : 1f).scaleY(f ? 1.04f : 1f).setDuration(120).start();
        });
    }

    View moreTile(int hh) {
        FrameLayout t = new FrameLayout(this);
        t.setFocusable(true);
        t.setClickable(true);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        DotsView dv = new DotsView(this);
        dv.color = textColor();
        int s = (int) (hh * 0.42f);
        box.addView(dv, new LinearLayout.LayoutParams(s, s));
        TextView lbl = new TextView(this);
        lbl.setText("المزيد");
        lbl.setTextSize(16);
        lbl.setTextColor(textColor());
        lbl.setGravity(Gravity.CENTER);
        lbl.setPadding(0, dp(6), 0, 0);
        box.addView(lbl);
        t.addView(box, new FrameLayout.LayoutParams(-1, -1));
        bg(t, false, dark ? 0x2EFFFFFF : 0x99FFFFFF, dark ? 0x40FFFFFF : 0x40000000);
        addRing(t, 9);
        t.setOnClickListener(v -> {
            lastFocus = 9;
            showMore();
        });
        tiles[9] = t;
        return t;
    }

    View tile(final int i, boolean big, int w, int hh) {
        if (i == 9) return moreTile(hh);
        final String pkg = sp.getString("s" + i, null);
        FrameLayout card = appCard(pkg, hh, big);
        final boolean isFilled = card != null;
        final FrameLayout t = isFilled ? card : emptyCard(big);
        addRing(t, i);
        t.setOnClickListener(v -> {
            lastFocus = i;
            if (isFilled) {
                launch(pkg);
            } else {
                chooseApp("اختر تطبيقاً", p -> {
                    sp.edit().putString("s" + i, p).apply();
                    build();
                });
            }
        });
        t.setOnLongClickListener(v -> {
            if (isFilled) options(i);
            return true;
        });
        tiles[i] = t;
        return t;
    }

    List<String> extras() {
        List<String> out = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (String x : sp.getString("extra", "").split(",")) {
            if (x.isEmpty()) continue;
            try {
                pm.getApplicationInfo(x, 0);
                out.add(x);
            } catch (PackageManager.NameNotFoundException e) {
                // تطبيق محذوف
            }
        }
        return out;
    }

    void saveExtras(List<String> l) {
        sp.edit().putString("extra", android.text.TextUtils.join(",", l)).apply();
    }

    void showMore() {
        final Dialog dlg = new Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        panel.setPadding(dp(40), dp(28), dp(40), dp(20));
        panel.setBackgroundColor(0xFF0B1218);
        TextView title = new TextView(this);
        title.setText("المزيد");
        title.setTextSize(30);
        title.setTextColor(0xFFF4EFE6);
        panel.addView(title);
        TextView hint = new TextView(this);
        hint.setText("اضغط مطولاً على أي تطبيق لحذفه من هنا");
        hint.setTextSize(15);
        hint.setTextColor(0x99F4EFE6);
        panel.addView(hint);
        ScrollView sv = new ScrollView(this);
        LinearLayout host = new LinearLayout(this);
        host.setOrientation(LinearLayout.VERTICAL);
        host.setPadding(0, dp(14), 0, 0);
        sv.addView(host);
        panel.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        dlg.setContentView(panel);
        fillMore(dlg, host);
        dlg.show();
    }

    void fillMore(final Dialog dlg, final LinearLayout host) {
        host.removeAllViews();
        final List<String> pk = extras();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int cols = 5;
        int m = dp(8);
        int w = (dm.widthPixels - dp(80) - cols * 2 * m) / cols;
        int hh = (int) (w / 1.6f);
        LinearLayout row = null;
        View first = null;
        int total = pk.size() + 1;
        for (int k = 0; k < total; k++) {
            if (k % cols == 0) {
                row = new LinearLayout(this);
                host.addView(row, new LinearLayout.LayoutParams(-2, -2));
            }
            final FrameLayout c;
            if (k < pk.size()) {
                final String p = pk.get(k);
                FrameLayout cc = appCard(p, hh, false);
                if (cc == null) continue;
                c = cc;
                addRing(c, -1);
                c.setOnClickListener(v -> {
                    dlg.dismiss();
                    launch(p);
                });
                c.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setItems(new String[]{"حذف من المزيد"}, (d2, which) -> {
                                List<String> l = extras();
                                l.remove(p);
                                saveExtras(l);
                                fillMore(dlg, host);
                            }).show();
                    return true;
                });
            } else {
                c = emptyCard(false);
                addRing(c, -1);
                c.setOnClickListener(v -> chooseApp("اختر تطبيقاً لإضافته", p2 -> {
                    List<String> l = extras();
                    if (!l.contains(p2)) l.add(p2);
                    saveExtras(l);
                    fillMore(dlg, host);
                }));
            }
            row.addView(c, cellLp(w, hh, m));
            if (first == null) first = c;
        }
        final View ff = first;
        host.post(() -> {
            if (ff != null) ff.requestFocus();
        });
    }

    /** نفس طريقة المشغّلات الأصلية: يرجع التطبيق إلى الأمام إن كان يعمل بدل أن يبدأ من جديد */
    void launch(String pkg) {
        Intent li = null;
        if (pkg != null) {
            PackageManager pm = getPackageManager();
            li = pm.getLaunchIntentForPackage(pkg);
            if (li == null) li = pm.getLeanbackLaunchIntentForPackage(pkg);
        }
        if (li == null) {
            Toast.makeText(this, "التطبيق غير متاح", Toast.LENGTH_SHORT).show();
        } else {
            li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(li);
        }
    }

    void options(final int i) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{"تغيير التطبيق", "حذف من الخانة"}, (dlg, which) -> {
                    if (which == 0) {
                        chooseApp("اختر تطبيقاً", p -> {
                            sp.edit().putString("s" + i, p).apply();
                            build();
                        });
                    } else {
                        sp.edit().remove("s" + i).apply();
                        build();
                    }
                }).show();
    }

    void chooseApp(String title, final OnPick cb) {
        PackageManager pm = getPackageManager();
        Intent mi = new Intent(Intent.ACTION_MAIN);
        mi.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(mi, 0);
        final List<String[]> items = new ArrayList<>();
        for (ResolveInfo r : found) {
            String p = r.activityInfo.packageName;
            if (p.equals(getPackageName())) continue;
            items.add(new String[]{r.loadLabel(pm).toString(), p});
        }
        Collections.sort(items, (a, c) -> a[0].compareToIgnoreCase(c[0]));
        String[] labels = new String[items.size()];
        for (int k = 0; k < labels.length; k++) labels[k] = items.get(k)[0];
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels, (dlg, which) -> cb.on(items.get(which)[1]))
                .show();
    }
}
