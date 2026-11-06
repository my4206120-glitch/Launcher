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
        root.addView(midRo
