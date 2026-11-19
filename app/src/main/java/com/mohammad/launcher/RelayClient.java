package com.mohammad.launcher;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/** يبقي الشاشة متصلة بخادم الوسيط، وينفّذ الأوامر التي يرسلها الهاتف */
public class RelayClient {
    // ضع كلمة السر من Render داخل علامتي التنصيص، ولا تنشر هذا الملف لأحد
    static final String TOKEN = "Mohammad7yaser2005";
    static final String SERVER = "wss://remote-relay-p7s8.onrender.com/";

    private final BrightnessService svc;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build();
    private WebSocket ws;
    private boolean stopped = true;
    private int tries = 0;

    public RelayClient(BrightnessService s) {
        svc = s;
    }

    public void start() {
        stopped = false;
        connect();
    }

    public void stop() {
        stopped = true;
        if (ws != null) ws.close(1000, "stop");
    }

    void connect() {
        if (stopped) return;
        Request req = new Request.Builder()
                .url(SERVER + "?role=tv&token=" + TOKEN)
                .build();
        ws = client.newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket w, Response r) {
                tries = 0;
                toast("ريموتي: متصل");
            }

            @Override
            public void onMessage(WebSocket w, String text) {
                handle(text);
            }

            @Override
            public void onFailure(WebSocket w, Throwable t, Response r) {
                retry();
            }

            @Override
            public void onClosed(WebSocket w, int code, String reason) {
                if (code == 4001) {
                    stopped = true;
                    toast("ريموتي: كلمة السر خاطئة");
                } else {
                    retry();
                }
            }
        });
    }

    void retry() {
        if (stopped) return;
        long delay = Math.min(30000L, 2000L * (1L << Math.min(tries++, 4)));
        h.postDelayed(this::connect, delay);
    }

    void handle(String text) {
        try {
            JSONObject o = new JSONObject(text);
            if ("key".equals(o.optString("cmd"))) {
                final String key = o.optString("key");
                h.post(() -> svc.performRemoteKey(key));
            }
        } catch (Exception ignored) { }
    }

    void toast(final String msg) {
        h.post(() -> Toast.makeText(svc, msg, Toast.LENGTH_SHORT).show());
    }
}
