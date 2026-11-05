package com.mohammad.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    static final int COUNT = 10;
    SharedPreferences sp;
    LinearLayout grid;
    float d;

    int dp(int v) { return (int) (v * d + 0.5f); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("launcher", MODE_PRIVATE);
        d = getResources().getDisplayMetrics().density;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(24), dp(20), dp(24), dp(20));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0F2A33, 0xFF1D4B52}));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
       // تنسيق الساعة بنظام 12 مع صباحاً ومساءً
clock.setFormat12Hour("hh:mm a");
clock.setFormat24Hour("hh:mm a");
clock.setTextSize(40");

// إضافة التاريخ تحته
android.widget.TextView dateView = new android.widget.TextView(this);
dateView.setTextSize(18);
dateView.setTextColor(0xFFFFFFFF);
dateView.setGravity(android.view.Gravity.CENTER);

java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE، d MMMM yyyy", java.util.Locale.getDefault());
dateView.setText(sdf.format(new java.util.Date()));

head.addView(dateView);
 

        TextView wifi = new TextView(this);
        wifi.setText("📶  الواي فاي");
        wifi.setTextSize(24);
        wifi.setTextColor(0xFF2A1A00);
        wifi.setPadding(dp(26), dp(16), dp(26), dp(16));
        wifi.setFocusable(true);
        wifi.setClickable(true);
        bg(wifi, false, 0xFFE9A23B, 0xFFE9A23B, 0xFFFFFFFF);
        wifi.setOnFocusChangeListener((v, f) -> bg(wifi, f, 0xFFE9A23B, 0xFFE9A23B, 0xFFFFFFFF));
        wifi.setOnClickListener(v -> openWifi());
        head.addView(wifi);
        root.addView(head);

        ScrollView sv = new ScrollView(this);
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(0, dp(20), 0, 0);
        sv.addView(grid);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
        
android.widget.SeekBar brightnessBar = new android.widget.SeekBar(this);
brightnessBar.setMax(100);
root.addView(brightnessBar);

brightnessBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
        float brightness = progress / 100.0f;
        android.view.WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = brightness;
        getWindow().setAttributes(layoutParams);
    }
    @Override
    public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
    @Override
    public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    @Override
    public void onBackPressed() { }

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

    void bg(View v, boolean focused, int fill, int stroke, int focusStroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(24));
        g.setStroke(dp(focused ? 4 : 1), focused ? focusStroke : stroke);
        v.setBackground(g);
    }

    LinearLayout.LayoutParams cellLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(150), 1);
        p.setMargins(dp(8), dp(8), dp(8), dp(8));
        return p;
    }

    void build() {
        grid.removeAllViews();
        int wDp = (int) (getResources().getDisplayMetrics().widthPixels / d) - 48;
        int cols = Math.max(2, wDp / 170);
        LinearLayout row = null;
        for (int i = 0; i < COUNT; i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                grid.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            row.addView(tile(i), cellLp());
        }
        for (int j = COUNT; j % cols != 0; j++) {
            row.addView(new View(this), cellLp());
        }
    }

    View tile(final int i) {
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
                t.addView(iv, new LinearLayout.LayoutParams(dp(64), dp(64)));
                TextView name = new TextView(this);
                name.setText(pm.getApplicationLabel(ai));
                name.setTextSize(18);
                name.setTextColor(0xFFF4EFE6);
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
            plus.setTextSize(40);
            plus.setTextColor(0xCCF4EFE6);
            plus.setGravity(Gravity.CENTER);
            t.addView(plus);
            TextView lbl = new TextView(this);
            lbl.setText("أضف تطبيقاً");
            lbl.setTextSize(16);
            lbl.setTextColor(0xAAF4EFE6);
            lbl.setGravity(Gravity.CENTER);
            t.addView(lbl);
        }
        final int fill = filled ? 0x24FFFFFF : 0x0FFFFFFF;
        final int stroke = filled ? 0x40FFFFFF : 0x60FFFFFF;
        bg(t, false, fill, stroke, 0xFFFFFFFF);
        t.setOnFocusChangeListener((v, f) -> bg(t, f, f ? 0x44FFFFFF : fill, stroke, 0xFFFFFFFF));
        t.setOnClickListener(v -> {
            if (isFilled) launch(pkg); else pick(i);
        });
        t.setOnLongClickListener(v -> {
            if (isFilled) options(i);
            return true;
        });
        return t;
    }

    void launch(String pkg) {
        Intent li = getPackageManager().getLaunchIntentForPackage(pkg);
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
                        pick(i);
                    } else {
                        sp.edit().remove("s" + i).apply();
                        build();
                    }
                }).show();
    }

    void pick(final int i) {
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
                .setTitle("اختر تطبيقاً")
                .setItems(labels, (dlg, which) -> {
                    sp.edit().putString("s" + i, items.get(which)[1]).apply();
                    build();
                }).show();
    }
}
