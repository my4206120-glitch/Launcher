package com.mohammad.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    static final int ACCENT = 0xFFE9A23B;
    static final int[][] PD = {
        {0xFF0F2A33, 0xFF1D4B52}, {0xFF1B1035, 0xFF3B2A6B}, {0xFF2A1210, 0xFF5C2A1E},
        {0xFF0E1E14, 0xFF22513A}, {0xFF121212, 0xFF2B2B2B}};
    static final int[][] PL = {
        {0xFFE9F3F5, 0xFFB9D8DE}, {0xFFEFE9F8, 0xFFCFC2EC}, {0xFFF8EDE6, 0xFFEBC2AE},
        {0xFFE8F4EC, 0xFFB7DEC5}, {0xFFF5F5F5, 0xFFDADADA}};
    static final String[] PN = {"أزرق بحري", "بنفسجي ليلي", "ترابي دافئ", "أخضر غامق", "رمادي فحمي"};

    interface OnPick { void on(String pkg); }

    /** أيقونات مرسومة بالكود: 0 واي فاي، 1 سطوع، 2 مصدر HDMI، 3 إعدادات */
    static class IconBtn extends View {
        final int type;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int color = ACCENT;

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
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s * 0.07f);
            if (type == 0) {
                float bx = cx, by = cy + s * 0.2f;
                for (int k = 1; k <= 3; k++) {
                    float r = s * 0.13f * k;
                    RectF rf = new RectF(bx - r, by - r, bx + r, by + r);
                    c.drawArc(rf, 225, 90, false, p);
                }
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(bx, by, s * 0.05f, p);
            } else if (type == 1) {
                c.drawCircle(cx, cy, s * 0.15f, p);
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(k * 45);
                    float x1 = cx + (float) Math.cos(a) * s * 0.24f;
                    float y1 = cy + (float) Math.sin(a) * s * 0.24f;
                    float x2 = cx + (float) Math.cos(a) * s * 0.34f;
                    float y2 = cy + (float) Math.sin(a) * s * 0.34f;
                    c.drawLine(x1, y1, x2, y2, p);
                }
            } else if (type == 2) {
                Path path = new Path();
                path.moveTo(cx - 0.3f * s, cy - 0.18f * s);
                path.lineTo(cx + 0.3f * s, cy - 0.18f * s);
                path.lineTo(cx + 0.3f * s, cy + 0.04f * s);
                path.lineTo(cx + 0.2f * s, cy + 0.18f * s);
                path.lineTo(cx - 0.2f * s, cy + 0.18f * s);
                path.lineTo(cx - 0.3f * s, cy + 0.04f * s);
                path.close();
                c.drawPath(path, p);
                c.drawLine(cx - 0.14f * s, cy - 0.04f * s, cx + 0.14f * s, cy - 0.04f * s, p);
            } else {
                c.drawCircle(cx, cy, s * 0.14f, p);
                for (int k = 0; k < 8; k++) {
                    double a = Math.toRadians(k * 45);
                    float x1 = cx + (float) Math.cos(a) * s * 0.22f;
                    float y1 = cy + (float) Math.sin(a) * s * 0.22f;
                    float x2 = cx + (float) Math.cos(a) * s * 0.32f;
                    float y2 = cy + (float) Math.sin(a) * s * 0.32f;
                    c.drawLine(x1, y1, x2, y2, p);
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
    LinearLayout midRow, bottomRow;
    String loadedKey = null;
    final List<View> glass = new ArrayList<>();
    Handler h = new Handler(Looper.getMainLooper());
    Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            h.postDelayed(this, 20000);
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
        head.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout timeCard = card();
        clockTv = new TextView(this);
        clockTv.setTextSize(40);
        clockTv.setGravity(Gravity.CENTER);
        dateTv = new TextView(this);
        dateTv.setTextSize(16);
        dateTv.setGravity(Gravity.CENTER);
        dateTv.setAlpha(0.85f);
        timeCard.addView(clockTv);
        timeCard.addView(dateTv);
        head.addView(timeCard);

        LinearLayout weatherCard = card();
        weatherCard.setFocusable(true);
        weatherCard.setClickable(true);
        weatherCard.setOnClickListener(v -> askCity());
        weatherCard.setOnFocusChangeListener((v, f) -> glassBg(v, f));
        weatherTv = new TextView(this);
        weatherTv.setTextSize(26);
        weatherTv.setGravity(Gravity.CENTER);
        weatherCard.addView(weatherTv);
        LinearLayout.LayoutParams wcl = new LinearLayout.LayoutParams(-2, -1);
        wcl.setMarginStart(dp(14));
        head.addView(weatherCard, wcl);

        head.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));

        head.addView(iconBtn(0, v -> openWifi()), iconLp());
        head.addView(iconBtn(1, v -> brightnessDialog()), iconLp());
        head.addView(iconBtn(2, v -> openSource()), iconLp());
        head.addView(iconBtn(3, v -> openSettings()), iconLp());
        root.addView(head, new LinearLayout.LayoutParams(-1, dp(92)));

        midRow = new LinearLayout(this);
        midRow.setPadding(0, dp(10), 0, 0);
        root.addView(midRow, new LinearLayout.LayoutParams(-1, 0, 3));
        bottomRow = new LinearLayout(this);
        root.addView(bottomRow, new LinearLayout.LayoutParams(-1, 0, 2));

        dimView = new View(this);
        dimView.setFocusable(false);
        dimView.setClickable(false);
        frame.addView(dimView, new FrameLayout.LayoutParams(-1, -1));

        setContentView(frame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLook();
        build();
        tick();
        applyBrightness(sp.getInt("bright", 100));
        h.removeCallbacks(ticker);
        h.postDelayed(ticker, 20000);
        refreshWeather(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(ticker);
    }

    @Override
    public void onBackPressed() { }

    LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(22), dp(6), dp(22), dp(6));
        glass.add(c);
        return c;
    }

    IconBtn iconBtn(int type, View.OnClickListener l) {
        final IconBtn b = new IconBtn(this, type);
        b.setOnClickListener(l);
        b.setOnFocusChangeListener((v, f) -> glassBg(v, f));
        glass.add(b);
        return b;
    }

    LinearLayout.LayoutParams iconLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(64), dp(64));
        p.setMarginStart(dp(12));
        return p;
    }

    void glassBg(View v, boolean focused) {
        int fill = dark ? 0x30FFFFFF : 0xB0FFFFFF;
        int stroke = dark ? 0x45FFFFFF : 0x40000000;
        bg(v, focused, focused ? (dark ? 0x4DFFFFFF : 0xE6FFFFFF) : fill, stroke);
    }

    void bg(View v, boolean focused, int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(20));
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
        for (View v : glass) {
            glassBg(v, v.hasFocus());
            if (v instanceof IconBtn) {
                ((IconBtn) v).color = dark ? ACCENT : 0xFFB36B00;
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
                int s = 1;
                while (o.outWidth / s > 2000 || o.outHeight / s > 1200) s *= 2;
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = s;
                o2.inScaled = false;
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

    void applyBrightness(int lvl) {
        if (lvl < 10) lvl = 10;
        if (lvl > 100) lvl = 100;
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = lvl >= 100
                ? WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                : Math.max(0.05f, lvl / 100f);
        getWindow().setAttributes(lp);
        float dim = (100 - lvl) / 90f * 0.55f;
        dimView.setBackgroundColor(Color.argb((int) (dim * 255), 0, 0, 0));
    }

    void brightnessDialog() {
        int cur = sp.getInt("bright", 100);
        final TextView tv = new TextView(this);
        tv.setTextSize(20);
        tv.setText("السطوع: " + cur + "%");
        tv.setPadding(0, 0, 0, dp(12));
        final SeekBar sb = new SeekBar(this);
        sb.setMax(90);
        sb.setProgress(cur - 10);
        sb.setKeyProgressIncrement(5);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int lvl = p + 10;
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
                .setOnDismissListener(dlg -> sp.edit().putInt("bright", sb.getProgress() + 10).apply())
                .show();
    }

    void tick() {
        Date now = new Date();
        clockTv.setText(new SimpleDateFormat("hh:mm a", new Locale("ar")).format(now));
        dateTv.setText(new SimpleDateFormat("EEEE، d MMMM yyyy", new Locale("ar")).format(now));
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

    String icon(int c) {
        if (c == 0) return "☀️";
        if (c <= 2) return "⛅";
        if (c == 3) return "☁️";
        if (c == 45 || c == 48) return "🌫️";
        if (c >= 51 && c <= 57) return "🌦️";
        if ((c >= 61 && c <= 67) || (c >= 80 && c <= 82)) return "🌧️";
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return "❄️";
        if (c >= 95) return "⛈️";
        return "🌡️";
    }

    void refreshWeather(boolean force) {
        final String lat = sp.getString("lat", null);
        final String lon = sp.getString("lon", null);
        if (lat == null || lon == null) {
            weatherTv.setText("☁️ حدّد المدينة");
            return;
        }
        long last = sp.getLong("w_time", 0);
        String cached = sp.getString("w_text", null);
        if (cached != null) weatherTv.setText(cached);
        if (!force && cached != null && System.currentTimeMillis() - last < 20 * 60 * 1000) return;
        final String city = sp.getString("city", "");
        new Thread(() -> {
            try {
                String json = http("https://api.open-meteo.com/v1/forecast?latitude=" + lat
                        + "&longitude=" + lon + "&current=temperature_2m,weather_code&timezone=auto");
                JSONObject cur = new JSONObject(json).getJSONObject("current");
                int t = (int) Math.round(cur.getDouble("temperature_2m"));
                int code = cur.getInt("weather_code");
                final String txt = icon(code) + " " + t + "°  " + city;
                sp.edit().putString("w_text", txt).putLong("w_time", System.currentTimeMillis()).apply();
                runOnUiThread(() -> weatherTv.setText(txt));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (sp.getString("w_text", null) == null) weatherTv.setText("☁️ --");
                });
            }
        }).start();
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
                        .remove("w_text")
                        .apply();
                runOnUiThread(() -> refreshWeather(true));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "تعذر الاتصال بالإنترنت", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    void openSettings() {
        String[] items = {
            "🎨  تغيير الخلفية",
            dark ? "☀️  الوضع الفاتح" : "🌙  الوضع الداكن",
            "📍  مدينة الطقس",
            "🔄  تحديث الطقس"};
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
                    } else {
                        refreshWeather(true);
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

    void openSource() {
        final List<String> labels = new ArrayList<>();
        final List<String> ids = new ArrayList<>();
        try {
            TvInputManager tm = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
            if (tm != null) {
                for (TvInputInfo info : tm.getTvInputList()) {
                    if (info.isPassthroughInput()) {
                        labels.add(String.valueOf(info.loadLabel(this)));
                        ids.add(info.getId());
                    }
                }
            }
        } catch (Exception ignored) { }
        if (sp.getString("src_pkg", null) != null) {
            labels.add("▶  فتح تطبيق المصدر المحدد");
            ids.add("#pkg");
        }
        labels.add("⚙  اختيار تطبيق المصدر يدوياً");
        ids.add("#pick");
        new AlertDialog.Builder(this)
                .setTitle("المصدر")
                .setItems(labels.toArray(new String[0]), (dlg, which) -> {
                    String id = ids.get(which);
                    if (id.equals("#pick")) {
                        chooseApp("اختر تطبيق المصدر", pkg -> {
                            sp.edit().putString("src_pkg", pkg).apply();
                            launch(pkg);
                        });
                    } else if (id.equals("#pkg")) {
                        launch(sp.getString("src_pkg", ""));
                    } else {
                        switchInput(id);
                    }
                }).show();
    }

    void switchInput(String id) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    TvContract.buildChannelUriForPassthroughInput(id)));
        } catch (Exception e) {
            Toast.makeText(this, "تعذر التبديل، اختر تطبيق المصدر يدوياً", Toast.LENGTH_LONG).show();
        }
    }

    LinearLayout.LayoutParams cellLp(float w) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, w);
        p.setMargins(dp(6), dp(6), dp(6), dp(6));
        return p;
    }

    void build() {
        midRow.removeAllViews();
        bottomRow.removeAllViews();
        midRow.addView(tile(0, true), cellLp(1.2f));
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            for (int c = 0; c < 2; c++) {
                row.addView(tile(1 + r * 2 + c, false), cellLp(1f));
            }
            grid.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));
        }
        midRow.addView(grid, new LinearLayout.LayoutParams(0, -1, 1.6f));
        for (int i = 5; i < 10; i++) {
            bottomRow.addView(tile(i, false), cellLp(1f));
        }
    }

    View tile(final int i, boolean big) {
        final String pkg = sp.getString("s" + i, null);
        final LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        t.setClickable(true);

        PackageManager pm = getPackageManager();
        boolean filled = false;
        if (pkg != null) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(pm.getApplicationIcon(ai));
                int s = dp(big ? 110 : 60);
                t.addView(iv, new LinearLayout.LayoutParams(s, s));
                TextView name = new TextView(this);
                name.setText(pm.getApplicationLabel(ai));
                name.setTextSize(big ? 26 : 17);
                name.setTextColor(textColor());
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(1);
                name.setPadding(dp(6), dp(8), dp(6), 0);
                t.addView(name);
                filled = true;
            } catch (PackageManager.NameNotFoundException e) {
                filled = false;
            }
        }
        final boolean isFilled = filled;
        if (!filled) {
            TextView plus = new TextView(this);
            plus.setText("＋");
            plus.setTextSize(big ? 56 : 38);
            plus.setTextColor(textColor());
            plus.setAlpha(0.7f);
            plus.setGravity(Gravity.CENTER);
            t.addView(plus);
            TextView lbl = new TextView(this);
            lbl.setText("أضف تطبيقاً");
            lbl.setTextSize(big ? 22 : 15);
            lbl.setTextColor(textColor());
            lbl.setAlpha(0.6f);
            lbl.setGravity(Gravity.CENTER);
            t.addView(lbl);
        }
        final int fill = dark ? (filled ? 0x2EFFFFFF : 0x12FFFFFF) : (filled ? 0x99FFFFFF : 0x44FFFFFF);
        final int focusFill = dark ? 0x44FFFFFF : 0xCCFFFFFF;
        final int stroke = dark ? 0x40FFFFFF : 0x40000000;
        bg(t, false, fill, stroke);
        t.setOnFocusChangeListener((v, f) -> bg(t, f, f ? focusFill : fill, stroke));
        t.setOnClickListener(v -> {
            if (isFilled) launch(pkg);
            else chooseApp("اختر تطبيقاً", p -> {
                sp.edit().putString("s" + i, p).apply();
                build();
            });
        });
        t.setOnLongClickListener(v -> {
            if (isFilled) options(i);
            return true;
        });
        return t;
    }

    void launch(String pkg) {
        Intent li = pkg == null ? null : getPackageManager().getLaunchIntentForPackage(pkg);
        if (li == null) {
            Toast.makeText(this, "التطبيق غير متاح", Toast.LENGTH_SHORT).show();
        } else {
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

