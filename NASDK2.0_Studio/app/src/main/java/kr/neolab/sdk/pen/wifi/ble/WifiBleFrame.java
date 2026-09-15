package kr.neolab.sdk.pen.wifi.ble;

/**
 * WiFi Protocol v1.0 — BLE SLIP 프레이밍.
 *
 * <p>펌웨어 spec (web reference js/ble-transport.js, js/app.js) 과 정합 유지.</p>
 *
 * <pre>
 * frame   = [SOF] + stuff(payload) + [EOF]
 * payload = [cmd][len_lo][len_hi][data...]   (len = le16(data 길이))
 * stuffing: SOF / EOF / DLE 바이트 → DLE, (byte XOR 0x20)
 * </pre>
 *
 * <p>응답 / 이벤트 헤더 비대칭 (comm_pen2app.h 정합):</p>
 * <pre>
 * Response (cmd & 0x80) : [cmd][err][len_lo][len_hi][data...]   (헤더 4)
 * Event    (else)       : [cmd]      [len_lo][len_hi][data...]  (헤더 3)
 * </pre>
 *
 * <p>이 클래스는 GATT 와 무관 — 순수 byte 처리.</p>
 */
public final class WifiBleFrame {

    public static final int PKT_SOF = 0xC0;
    public static final int PKT_EOF = 0xC1;
    public static final int PKT_DLE = 0x7D;
    public static final int PKT_CHR = 0x20;

    private WifiBleFrame() { }

    /**
     * payload (cmd + data) 를 SLIP frame 으로 인코딩 — write 직전에 호출.
     */
    public static byte[] encode(int cmd, byte[] data) {
        if (data == null) data = new byte[0];
        int len = data.length;
        // payload = [cmd][len_lo][len_hi][data...]
        byte[] payload = new byte[3 + len];
        payload[0] = (byte) (cmd & 0xFF);
        payload[1] = (byte) (len & 0xFF);
        payload[2] = (byte) ((len >>> 8) & 0xFF);
        System.arraycopy(data, 0, payload, 3, len);

        // SLIP stuffing — 최악 2배 + SOF/EOF 2바이트.
        byte[] tmp = new byte[2 + payload.length * 2];
        int idx = 0;
        tmp[idx++] = (byte) PKT_SOF;
        for (byte b : payload) {
            int v = b & 0xFF;
            if (v == PKT_SOF || v == PKT_EOF || v == PKT_DLE) {
                tmp[idx++] = (byte) PKT_DLE;
                tmp[idx++] = (byte) (v ^ PKT_CHR);
            } else {
                tmp[idx++] = (byte) v;
            }
        }
        tmp[idx++] = (byte) PKT_EOF;
        byte[] out = new byte[idx];
        System.arraycopy(tmp, 0, out, 0, idx);
        return out;
    }

    /**
     * Notify stream → frame payload 파서 (SLIP 디프레이밍).
     *
     * <p>{@link WifiBleSession} 가 notify byte 가 들어올 때마다 {@link #feed(int)} 호출.
     * 한 frame 이 완성될 때마다 {@link Listener#onPayload(byte[])} 콜백 발생.
     * payload 는 헤더 포함된 raw bytes (`[cmd][len_lo][len_hi][...]` 또는
     * `[cmd][err][len_lo][len_hi][...]`) — 응답/이벤트 구분은 호출자가.</p>
     */
    public static final class Decoder {

        public interface Listener {
            void onPayload(byte[] payload);
        }

        private final Listener listener;
        private byte[] accum = new byte[256];
        private int accumLen = 0;
        private boolean inFrame = false;
        private boolean escape = false;

        public Decoder(Listener listener) {
            this.listener = listener;
        }

        public void feed(byte[] bytes) {
            if (bytes == null) return;
            for (byte b : bytes) feed(b & 0xFF);
        }

        public void feed(int b) {
            b &= 0xFF;
            if (b == PKT_SOF) {
                accumLen = 0;
                inFrame = true;
                escape = false;
                return;
            }
            if (!inFrame) return;
            if (b == PKT_EOF) {
                inFrame = false;
                byte[] out = new byte[accumLen];
                System.arraycopy(accum, 0, out, 0, accumLen);
                if (listener != null) listener.onPayload(out);
                return;
            }
            if (b == PKT_DLE) {
                escape = true;
                return;
            }
            int v = escape ? (b ^ PKT_CHR) : b;
            escape = false;
            push((byte) v);
        }

        private void push(byte b) {
            if (accumLen >= accum.length) {
                byte[] grown = new byte[accum.length * 2];
                System.arraycopy(accum, 0, grown, 0, accumLen);
                accum = grown;
            }
            accum[accumLen++] = b;
        }
    }

    /**
     * payload 가 response (cmd MSB=1) 인지 event 인지 판별.
     * payload[0] 가 cmd. response 면 헤더 4, event 면 헤더 3.
     */
    public static boolean isResponse(byte[] payload) {
        return payload != null && payload.length > 0 && (payload[0] & 0x80) != 0;
    }

    public static int cmdOf(byte[] payload) {
        return payload != null && payload.length > 0 ? (payload[0] & 0xFF) : -1;
    }

    /** Response 전용 — err 코드. event 에는 호출하지 말 것. */
    public static int errOf(byte[] payload) {
        return payload != null && payload.length > 1 ? (payload[1] & 0xFF) : -1;
    }

    /**
     * 응답/이벤트의 data 부분만 잘라 반환. 헤더는 자동 판별.
     */
    public static byte[] dataOf(byte[] payload) {
        if (payload == null || payload.length < 3) return new byte[0];
        boolean resp = isResponse(payload);
        int headerLen = resp ? 4 : 3;
        if (payload.length < headerLen) return new byte[0];
        int len;
        if (resp) {
            len = (payload[2] & 0xFF) | ((payload[3] & 0xFF) << 8);
        } else {
            len = (payload[1] & 0xFF) | ((payload[2] & 0xFF) << 8);
        }
        int end = Math.min(headerLen + len, payload.length);
        byte[] out = new byte[Math.max(0, end - headerLen)];
        System.arraycopy(payload, headerLen, out, 0, out.length);
        return out;
    }

    public static String hex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 3);
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(' ');
            int v = bytes[i] & 0xFF;
            if (v < 0x10) sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }
}
