package kr.neolab.sdk.pen.wifi.ble;

import android.os.Handler;
import android.os.Looper;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import kr.neolab.sdk.util.NLog;

/**
 * WiFi Protocol v1.0 — 고수준 세션.
 *
 * <p>BTLEAdt 가 신규 WiFi 서비스(3c6cca12) 발견 시 인스턴스화. write/notify 는
 * {@link Writer} 와 {@link #onNotify(byte[])} 를 통해 BTLEAdt 가 중개.</p>
 *
 * <p>제공 API:</p>
 * <ul>
 *   <li>{@link #turnOn(String, String, String, int, ApOnListener)} — 0x40 CONFIG SET → 0x41 ON
 *       → AP_UP 이벤트 대기 → onApUp. 각 단계 timeout 보호.</li>
 *   <li>{@link #turnOff(SimpleListener)} — 0x41 OFF → OFF_DONE 이벤트 대기.</li>
 *   <li>{@link #queryStatus(StatusListener)} — 0x42 STATUS 요청 → 0xC2 응답 파싱.</li>
 * </ul>
 *
 * <p>모든 콜백은 main thread 로 보장.</p>
 */
public final class WifiBleSession {

    /** GATT service UUID (NWP_WiFi_Protocol_v1.0 §2.1). */
    public static final UUID SERVICE_UUID =
            UUID.fromString("3c6cca12-cc16-4a3d-aadc-4178b5e21270");
    /** write characteristic (app → pen). */
    public static final UUID CHAR_WRITE_UUID =
            UUID.fromString("d6a21632-8f15-41c7-8974-862538e6ac35");
    /** notify characteristic (pen → app). */
    public static final UUID CHAR_NOTIFY_UUID =
            UUID.fromString("091671ed-be9a-4e3d-a1bb-d63663b95a8e");

    /** BTLEAdt 가 구현 — 실제 GATT write 트리거. */
    public interface Writer {
        /** 펜에게 SLIP-framed bytes 를 write. true 반환 = enqueue 성공. */
        boolean write(byte[] frame);
    }

    public interface ApOnListener {
        /** 0x40 ACK → 0x41 ACK → 0x43 AP_UP 모두 받은 직후. */
        void onApUp();
        void onFailed(String reason);
    }

    public interface SimpleListener {
        void onOk();
        void onFailed(String reason);
    }

    public interface StatusListener {
        void onStatus(WifiBleProto.StatusInfo info);
        void onFailed(String reason);
    }

    private static final long ACK_TIMEOUT_MS = 3000;
    private static final long AP_UP_TIMEOUT_MS = 15000;
    private static final long OFF_DONE_TIMEOUT_MS = 5000;

    private final Writer writer;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final WifiBleFrame.Decoder decoder = new WifiBleFrame.Decoder(this::onPayload);

    // 진행 중인 phase 추적. 한 번에 하나의 turnOn/turnOff/queryStatus 만 — 단순화.
    private volatile Pending pending = null;

    public WifiBleSession(Writer writer) {
        this.writer = writer;
    }

    /** BTLEAdt 가 notify 데이터 수신 시 호출. */
    public void onNotify(byte[] bytes) {
        decoder.feed(bytes);
    }

    // ── turnOn 시퀀스 ──

    public void turnOn(String ssid, String pw, String token, int timeoutMin,
                       ApOnListener listener) {
        if (pending != null) {
            postFail(listener, "another wifi op in progress (" + pending.phase + ")");
            return;
        }
        Pending p = new Pending();
        p.phase = Phase.CONFIG_ACK;
        p.apListener = listener;
        p.controlTimeoutMin = timeoutMin;
        pending = p;
        NLog.d("[wifi-ble] turnOn — sending 0x40 CONFIG (ssid='" + ssid + "', pw.len="
                + pw.length() + ", token='" + token + "', timeoutMin=" + timeoutMin + ")");
        byte[] data = WifiBleProto.buildConfigSet(ssid, pw, token, timeoutMin);
        byte[] frame = WifiBleFrame.encode(WifiBleProto.CMD_WIFI_CONFIG, data);
        NLog.d("[wifi-ble] TX 0x40 CONFIG frame=" + WifiBleFrame.hex(frame));
        if (!writer.write(frame)) {
            failNow("write 0x40 failed (writer rejected)");
            return;
        }
        scheduleTimeout(p, ACK_TIMEOUT_MS, "0x40 CONFIG ACK timeout");
    }

    public void turnOff(SimpleListener listener) {
        if (pending != null) {
            main.post(() -> listener.onFailed("another wifi op in progress (" + pending.phase + ")"));
            return;
        }
        Pending p = new Pending();
        p.phase = Phase.CONTROL_OFF_ACK;
        p.simpleListener = listener;
        pending = p;
        byte[] frame = WifiBleFrame.encode(WifiBleProto.CMD_WIFI_CONTROL, WifiBleProto.buildControlOff());
        NLog.d("[wifi-ble] TX 0x41 OFF frame=" + WifiBleFrame.hex(frame));
        if (!writer.write(frame)) {
            failNow("write 0x41 OFF failed");
            return;
        }
        scheduleTimeout(p, ACK_TIMEOUT_MS, "0x41 OFF ACK timeout");
    }

    public void queryStatus(StatusListener listener) {
        if (pending != null) {
            main.post(() -> listener.onFailed("another wifi op in progress (" + pending.phase + ")"));
            return;
        }
        Pending p = new Pending();
        p.phase = Phase.STATUS_ACK;
        p.statusListener = listener;
        pending = p;
        byte[] frame = WifiBleFrame.encode(WifiBleProto.CMD_WIFI_STATUS, WifiBleProto.buildStatusReq());
        NLog.d("[wifi-ble] TX 0x42 STATUS frame=" + WifiBleFrame.hex(frame));
        if (!writer.write(frame)) {
            failNow("write 0x42 STATUS failed");
            return;
        }
        scheduleTimeout(p, ACK_TIMEOUT_MS, "0x42 STATUS ACK timeout");
    }

    // ── 수신 라우팅 ──

    private void onPayload(byte[] payload) {
        int cmd = WifiBleFrame.cmdOf(payload);
        boolean isResp = WifiBleFrame.isResponse(payload);
        byte[] data = WifiBleFrame.dataOf(payload);
        NLog.d("[wifi-ble] RX cmd=0x" + Integer.toHexString(cmd)
                + (isResp ? " resp err=" + WifiBleProto.errName(WifiBleFrame.errOf(payload)) : " event")
                + " data=" + WifiBleFrame.hex(data));

        if (!isResp && cmd == WifiBleProto.CMD_WIFI_EVT) {
            handleEvent(data);
            return;
        }
        Pending p = pending;
        if (p == null) {
            NLog.d("[wifi-ble] orphan response cmd=0x" + Integer.toHexString(cmd));
            return;
        }
        switch (p.phase) {
            case CONFIG_ACK:
                if (cmd == WifiBleProto.RSP_WIFI_CONFIG) {
                    int err = WifiBleFrame.errOf(payload);
                    if (err != WifiBleProto.ERR_OK) {
                        failNow("0x40 CONFIG err=" + WifiBleProto.errName(err));
                        return;
                    }
                    // CONFIG 성공 → 곧장 CONTROL ON 전송.
                    sendControlOn(p);
                }
                break;
            case CONTROL_ON_ACK:
                if (cmd == WifiBleProto.RSP_WIFI_CONTROL) {
                    int err = WifiBleFrame.errOf(payload);
                    if (err != WifiBleProto.ERR_OK) {
                        failNow("0x41 ON err=" + WifiBleProto.errName(err));
                        return;
                    }
                    // ACK 받음 → AP_UP 이벤트 대기.
                    cancelTimeout(p);
                    p.phase = Phase.WAIT_AP_UP;
                    scheduleTimeout(p, AP_UP_TIMEOUT_MS, "AP_UP event timeout");
                }
                break;
            case CONTROL_OFF_ACK:
                if (cmd == WifiBleProto.RSP_WIFI_CONTROL) {
                    int err = WifiBleFrame.errOf(payload);
                    if (err != WifiBleProto.ERR_OK) {
                        failNow("0x41 OFF err=" + WifiBleProto.errName(err));
                        return;
                    }
                    // OFF ACK 받음 → OFF_DONE 이벤트 대기. timeout 짧게 — 펜이 빨리 응답해야.
                    cancelTimeout(p);
                    p.phase = Phase.WAIT_OFF_DONE;
                    scheduleTimeout(p, OFF_DONE_TIMEOUT_MS, "OFF_DONE event timeout");
                }
                break;
            case STATUS_ACK:
                if (cmd == WifiBleProto.RSP_WIFI_STATUS) {
                    int err = WifiBleFrame.errOf(payload);
                    if (err != WifiBleProto.ERR_OK) {
                        failNow("0x42 STATUS err=" + WifiBleProto.errName(err));
                        return;
                    }
                    WifiBleProto.StatusInfo info = WifiBleProto.parseStatus(data);
                    completeStatus(info);
                }
                break;
            default:
                break;
        }
    }

    private void sendControlOn(Pending p) {
        cancelTimeout(p);
        p.phase = Phase.CONTROL_ON_ACK;
        byte[] frame = WifiBleFrame.encode(WifiBleProto.CMD_WIFI_CONTROL,
                WifiBleProto.buildControlOn(p.controlTimeoutMin));
        NLog.d("[wifi-ble] TX 0x41 ON timeoutMin=" + p.controlTimeoutMin
                + " frame=" + WifiBleFrame.hex(frame));
        if (!writer.write(frame)) {
            failNow("write 0x41 ON failed");
            return;
        }
        scheduleTimeout(p, ACK_TIMEOUT_MS, "0x41 ON ACK timeout");
    }

    private void handleEvent(byte[] data) {
        if (data.length < 1) return;
        int evt = data[0] & 0xFF;
        int reason = data.length > 1 ? (data[1] & 0xFF) : 0;
        NLog.d("[wifi-ble] EVT " + WifiBleProto.eventName(evt)
                + " reason=" + WifiBleProto.reasonName(reason));

        Pending p = pending;
        if (p == null) return;
        if (p.phase == Phase.WAIT_AP_UP && evt == WifiBleProto.EVT_AP_UP) {
            completeApUp();
        } else if (p.phase == Phase.WAIT_OFF_DONE
                && (evt == WifiBleProto.EVT_OFF_DONE || evt == WifiBleProto.EVT_AUTO_OFF)) {
            completeOff();
        } else if (evt == WifiBleProto.EVT_ERROR) {
            failNow("EVT_ERROR reason=" + WifiBleProto.reasonName(reason));
        }
    }

    // ── 완료/실패 헬퍼 ──

    private void completeApUp() {
        Pending p = pending;
        if (p == null) return;
        cancelTimeout(p);
        pending = null;
        ApOnListener l = p.apListener;
        if (l != null) main.post(l::onApUp);
    }

    private void completeOff() {
        Pending p = pending;
        if (p == null) return;
        cancelTimeout(p);
        pending = null;
        SimpleListener l = p.simpleListener;
        if (l != null) main.post(l::onOk);
    }

    private void completeStatus(WifiBleProto.StatusInfo info) {
        Pending p = pending;
        if (p == null) return;
        cancelTimeout(p);
        pending = null;
        StatusListener l = p.statusListener;
        if (l != null) main.post(() -> l.onStatus(info));
    }

    private void failNow(String reason) {
        Pending p = pending;
        if (p == null) return;
        cancelTimeout(p);
        pending = null;
        NLog.e("[wifi-ble] FAIL phase=" + p.phase + " reason=" + reason);
        if (p.apListener != null) {
            ApOnListener l = p.apListener;
            main.post(() -> l.onFailed(reason));
        } else if (p.simpleListener != null) {
            SimpleListener l = p.simpleListener;
            main.post(() -> l.onFailed(reason));
        } else if (p.statusListener != null) {
            StatusListener l = p.statusListener;
            main.post(() -> l.onFailed(reason));
        }
    }

    private void scheduleTimeout(Pending p, long ms, String reason) {
        cancelTimeout(p);
        AtomicBoolean fired = new AtomicBoolean(false);
        p.timeoutFired = fired;
        Runnable r = () -> {
            if (fired.compareAndSet(false, true) && pending == p) failNow(reason);
        };
        p.timeoutRunnable = r;
        main.postDelayed(r, ms);
    }

    private void cancelTimeout(Pending p) {
        if (p.timeoutRunnable != null) {
            main.removeCallbacks(p.timeoutRunnable);
            p.timeoutRunnable = null;
        }
    }

    private void postFail(ApOnListener l, String reason) {
        if (l != null) main.post(() -> l.onFailed(reason));
    }

    private enum Phase {
        CONFIG_ACK,        // 0x40 ACK 대기
        CONTROL_ON_ACK,    // 0x41 ON ACK 대기
        WAIT_AP_UP,        // 0x43 AP_UP 이벤트 대기
        CONTROL_OFF_ACK,   // 0x41 OFF ACK 대기
        WAIT_OFF_DONE,     // 0x43 OFF_DONE 이벤트 대기
        STATUS_ACK,        // 0x42 ACK 대기
    }

    private static final class Pending {
        Phase phase;
        ApOnListener apListener;
        SimpleListener simpleListener;
        StatusListener statusListener;
        int controlTimeoutMin;
        Runnable timeoutRunnable;
        AtomicBoolean timeoutFired;
    }
}
