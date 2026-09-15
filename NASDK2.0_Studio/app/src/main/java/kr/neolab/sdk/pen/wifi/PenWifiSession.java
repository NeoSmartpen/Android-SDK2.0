package kr.neolab.sdk.pen.wifi;

import android.content.Context;
import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.neolab.sdk.pen.bluetooth.BTLEAdt;
import kr.neolab.sdk.pen.wifi.ble.WifiBleSession;
import kr.neolab.sdk.util.NLog;

/**
 * NWP_WiFi_Protocol_v1.0 — 펜의 SoftAP 켜기 + 시스템 Wi-Fi 접속 + /v1 HTTP 호출 facade.
 *
 * <h3>시퀀스</h3>
 * <ol>
 *   <li>{@link #open(PenWifiConfig, Listener)} 호출.</li>
 *   <li>BLE 로 펜에게 {@code 0x40 CONFIG SET} (앱이 생성한 SSID/PW/TOKEN) → {@code 0x41 ON}.</li>
 *   <li>펜이 SoftAP 띄움 → {@code 0x43 AP_UP} 비동기 이벤트 도착.</li>
 *   <li>시스템 Wi-Fi 로 위에서 보낸 SSID/PW 사용해 접속 (사용자 승인 다이얼로그 1회).</li>
 *   <li>{@code onOpened} 콜백 → 호출부가 listFiles/downloadFile 등 호출.</li>
 *   <li>{@link #close()} → {@code 0x41 OFF} (best-effort) + 시스템 네트워크 해제.</li>
 * </ol>
 *
 * <h3>예시</h3>
 * <pre>
 *   PenWifiSession session = new PenWifiSession(ctx);
 *   session.open(PenWifiConfig.random(), new PenWifiSession.Listener() {
 *       public void onOpened(PenWifiSession s) {
 *           s.listFiles(PenWifiFile.TYPE_AUDIO, filesCb);
 *       }
 *       public void onFailed(String reason) { … }
 *   });
 * </pre>
 */
@RequiresApi(Build.VERSION_CODES.Q)
public final class PenWifiSession {

    public interface Listener {
        void onOpened(@NonNull PenWifiSession session);
        void onFailed(@NonNull String reason);
        default void onLost() { }
    }

    public interface ListFilesCallback {
        void onResult(@NonNull List<PenWifiFile> files);
        void onError(@NonNull String reason);
    }

    /** 펜 SoftAP 자동 OFF 분 — open ~ close 사이 활동 안 해도 안전망. */
    private static final int SOFTAP_TIMEOUT_MIN = 5;

    private final Context appContext;
    private final PenWifiConnector connector;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable private PenWifiConfig config;
    @Nullable private Network network;
    @Nullable private PenFileClient client;
    @Nullable private Listener listener;
    private boolean opened;

    public PenWifiSession(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.connector = new PenWifiConnector(appContext);
    }

    /** 펜 SoftAP 띄우고 시스템 Wi-Fi 접속까지 자동. 결과는 {@link Listener}. */
    public void open(@NonNull PenWifiConfig config, @NonNull Listener listener) {
        if (opened) {
            NLog.d( "[wifi] session already open — onOpened immediately" );
            main.post(() -> listener.onOpened(this));
            return;
        }
        this.config = config;
        this.listener = listener;

        WifiBleSession bleSession = BTLEAdt.getInstance().getWifiBleSession();
        if (bleSession == null) {
            NLog.d( "[wifi] open failed — pen does not expose WiFi BLE service (non-WiFi model or not connected)" );
            main.post(() -> listener.onFailed("WiFi BLE service unavailable — non-WiFi model pen"));
            return;
        }

        NLog.d( "[wifi] open — BLE turnOn ssid='" + config.ssid + "' token='" + config.token + "'" );
        bleSession.turnOn(config.ssid, config.password, config.token, SOFTAP_TIMEOUT_MIN,
                new WifiBleSession.ApOnListener() {
                    @Override public void onApUp() {
                        NLog.d( "[wifi] BLE AP_UP received → joining SoftAP" );
                        joinSystemWifi(config, listener);
                    }
                    @Override public void onFailed(@NonNull String reason) {
                        NLog.d( "[wifi] BLE turnOn failed: " + reason );
                        main.post(() -> listener.onFailed("BLE handshake: " + reason));
                    }
                });
    }

    private void joinSystemWifi(@NonNull PenWifiConfig cfg, @NonNull Listener listener) {
        connector.connect(cfg, new PenWifiConnector.Listener() {
            @Override public void onConnected(@NonNull Network net) {
                NLog.d( "[wifi] system Wi-Fi connected → session open" );
                network = net;
                client = new PenFileClient(net, cfg);
                opened = true;
                main.post(() -> listener.onOpened(PenWifiSession.this));
            }
            @Override public void onFailed(@NonNull String reason) {
                NLog.d( "[wifi] system Wi-Fi connect failed: " + reason + " — turning off SoftAP" );
                turnOffBleQuietly();
                main.post(() -> listener.onFailed(reason));
            }
            @Override public void onLost() {
                NLog.d( "[wifi] system Wi-Fi lost" );
                opened = false;
                main.post(listener::onLost);
            }
        });
    }

    public boolean isOpen() { return opened && client != null; }

    /** 파일 목록 (background thread). type = {@link PenWifiFile#TYPE_AUDIO} 또는 OFFLINE. */
    public void listFiles(@NonNull String type, @NonNull ListFilesCallback cb) {
        if (!isOpen() || client == null) {
            cb.onError("session not open");
            return;
        }
        final PenFileClient c = client;
        io.execute(() -> {
            try {
                List<PenWifiFile> list = c.listFiles(type);
                main.post(() -> cb.onResult(list));
            } catch (Throwable t) {
                String msg = t.getMessage() != null ? t.getMessage() : "list failed";
                main.post(() -> cb.onError(msg));
            }
        });
    }

    /** 단일 파일 다운로드. listener 콜백은 background thread. */
    public void downloadFile(@NonNull PenWifiFile file,
                             @NonNull File outDir,
                             @NonNull String outFileName,
                             @NonNull PenFileClient.DownloadListener listener) {
        if (!isOpen() || client == null) {
            listener.onError("session not open");
            return;
        }
        final PenFileClient c = client;
        io.execute(() -> c.downloadFile(file, outDir, outFileName, listener));
    }

    public void deleteFile(@NonNull PenWifiFile file,
                           @NonNull java.util.function.Consumer<Boolean> done) {
        if (!isOpen() || client == null) {
            main.post(() -> done.accept(false));
            return;
        }
        final PenFileClient c = client;
        io.execute(() -> {
            boolean ok;
            try { ok = c.deleteFile(file); } catch (Throwable t) { ok = false; }
            final boolean result = ok;
            main.post(() -> done.accept(result));
        });
    }

    /** 펜 STATUS (0x42) 비동기 조회 — 진단/디버그용. */
    public void queryPenStatus(@NonNull WifiBleSession.StatusListener cb) {
        WifiBleSession s = BTLEAdt.getInstance().getWifiBleSession();
        if (s == null) { main.post(() -> cb.onFailed("WiFi BLE session null")); return; }
        s.queryStatus(cb);
    }

    /** SoftAP 끄고 (BLE 0x41 OFF) 시스템 네트워크 해제. */
    public void close() {
        opened = false;
        client = null;
        network = null;
        try { connector.disconnect(); } catch (Throwable ignored) { }
        turnOffBleQuietly();
    }

    private void turnOffBleQuietly() {
        WifiBleSession s = BTLEAdt.getInstance().getWifiBleSession();
        if (s == null) return;
        s.turnOff(new WifiBleSession.SimpleListener() {
            @Override public void onOk() {
                NLog.d( "[wifi] BLE turnOff OK" );
            }
            @Override public void onFailed(@NonNull String reason) {
                // best-effort — 펜이 응답 안 줘도 다음 세션 못 여는 것 아님 (펜 timeout 으로 자동 OFF).
                NLog.d( "[wifi] BLE turnOff (best-effort) failed: " + reason );
            }
        });
        // Phase 1 정의에서 명시한 WAIT_OFF_DONE 이벤트가 펜에서 안 올 수도 — close 측은 ACK 만 기다리지 않고
        // 즉시 리턴. 다음 open() 호출은 같은 WifiBleSession 의 pending 클리어 후 진행.
    }
}
