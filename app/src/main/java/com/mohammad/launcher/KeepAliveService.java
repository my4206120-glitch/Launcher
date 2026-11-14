package com.mohammad.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/** خدمة صغيرة بإشعار دائم تجعل النظام أقل ميلاً لإغلاق الواجهة في الخلفية */
public class KeepAliveService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            String ch = "keep";
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.createNotificationChannel(new NotificationChannel(ch, "واجهتي",
                        NotificationManager.IMPORTANCE_MIN));
            }
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, ch) : new Notification.Builder(this);
            b.setContentTitle("واجهتي").setContentText("تعمل في الخلفية")
                    .setSmallIcon(android.R.drawable.ic_menu_view);
            startForeground(2, b.build());
        } catch (Exception ignored) { }
        return START_STICKY;
    }
}
