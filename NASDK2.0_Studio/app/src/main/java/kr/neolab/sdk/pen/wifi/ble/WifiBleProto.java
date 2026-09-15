package kr.neolab.sdk.pen.wifi.ble;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * WiFi Protocol v1.0 — 명령/TLV/이벤트 상수 + 페이로드 빌더.
 *
 * <p>Reference: NWP_WiFi_Protocol_v1.0 + js/app.js + 펌웨어 comm_pen2app.h</p>
 */
public final class WifiBleProto {

    /** TLV VERSION 값 — 펜은 1.1 미만 거부. */
    public static final String APP_WIFI_VER = "1.1";

    /** 0x44 NAND_FORMAT 매직 — 우발 wipe 방지. */
    public static final String NAND_FORMAT_MAGIC = "Jason";

    /** WiFi 명령 cmd 범위 (이 안에 들면 WIFI_SVC 로 라우팅). */
    public static final int WIFI_CMD_MIN = 0x40;
    public static final int WIFI_CMD_MAX = 0x44;

    // ── 명령 (TX cmd) ──
    public static final int CMD_WIFI_CONFIG  = 0x40;
    public static final int CMD_WIFI_CONTROL = 0x41;
    public static final int CMD_WIFI_STATUS  = 0x42;
    public static final int CMD_WIFI_EVT     = 0x43; // RX only (event)
    public static final int CMD_NAND_FORMAT  = 0x44;

    // ── 응답 cmd (TX cmd | 0x80) ──
    public static final int RSP_WIFI_CONFIG  = CMD_WIFI_CONFIG  | 0x80; // 0xC0
    public static final int RSP_WIFI_CONTROL = CMD_WIFI_CONTROL | 0x80; // 0xC1
    public static final int RSP_WIFI_STATUS  = CMD_WIFI_STATUS  | 0x80; // 0xC2
    public static final int RSP_NAND_FORMAT  = CMD_NAND_FORMAT  | 0x80; // 0xC4

    // ── ERR (response[1]) ──
    public static final int ERR_OK              = 0;
    public static final int ERR_FAIL            = 1;
    public static final int ERR_NOT_ALLOW       = 2;
    public static final int ERR_VALUE           = 3;
    public static final int ERR_LENGTH          = 4;
    public static final int ERR_UNKNOWN_CMD     = 5;
    public static final int ERR_NOT_READY       = 6;
    public static final int ERR_VER_UNSUPPORTED = 7;

    public static String errName(int err) {
        switch (err) {
            case ERR_OK:              return "OK";
            case ERR_FAIL:            return "FAIL";
            case ERR_NOT_ALLOW:       return "NOT_ALLOW";
            case ERR_VALUE:           return "VALUE";
            case ERR_LENGTH:          return "LENGTH";
            case ERR_UNKNOWN_CMD:     return "UNKNOWN_CMD";
            case ERR_NOT_READY:       return "NOT_READY";
            case ERR_VER_UNSUPPORTED: return "VER_UNSUPPORTED";
            default:                  return "0x" + Integer.toHexString(err);
        }
    }

    // ── 0x40 CONFIG op + TLV tag ──
    public static final int CONFIG_OP_SET = 0x01;

    public static final int TLV_VERSION = 0x00;
    public static final int TLV_SSID    = 0x01;
    public static final int TLV_PW      = 0x02;
    public static final int TLV_TOKEN   = 0x03;
    public static final int TLV_TIMEOUT = 0x04;
    public static final int TLV_CHANNEL = 0x05;

    // ── 0x41 CONTROL op ──
    public static final int CONTROL_OFF = 0x00;
    public static final int CONTROL_ON  = 0x01;

    // ── 0x43 EVT event 코드 ──
    public static final int EVT_AP_UP               = 1;
    public static final int EVT_CLIENT_CONNECTED    = 2;
    public static final int EVT_CLIENT_DISCONNECTED = 3;
    public static final int EVT_AUTO_OFF            = 4;
    public static final int EVT_OFF_DONE            = 5;
    public static final int EVT_ERROR               = 6;

    public static String eventName(int evt) {
        switch (evt) {
            case EVT_AP_UP:               return "AP_UP";
            case EVT_CLIENT_CONNECTED:    return "CLIENT_CONNECTED";
            case EVT_CLIENT_DISCONNECTED: return "CLIENT_DISCONNECTED";
            case EVT_AUTO_OFF:            return "AUTO_OFF";
            case EVT_OFF_DONE:            return "OFF_DONE";
            case EVT_ERROR:               return "ERROR";
            default:                      return "evt_" + evt;
        }
    }

    public static final int REASON_NONE    = 0;
    public static final int REASON_BLE_CMD = 1;
    public static final int REASON_TIMEOUT = 2;
    public static final int REASON_LOW_BATT = 3;
    public static final int REASON_ERROR   = 4;

    public static String reasonName(int r) {
        switch (r) {
            case REASON_NONE:    return "NONE";
            case REASON_BLE_CMD: return "BLE_CMD";
            case REASON_TIMEOUT: return "TIMEOUT";
            case REASON_LOW_BATT: return "LOW_BATT";
            case REASON_ERROR:   return "ERROR";
            default:             return "reason_" + r;
        }
    }

    private WifiBleProto() { }

    /**
     * 0x40 WIFI_CONFIG SET data 페이로드 빌드.
     * <p>형식: {@code [op=SET][TLV(VERSION=1.1)][TLV(SSID)][TLV(PW)][TLV(TOKEN)][TLV(TIMEOUT) opt]}</p>
     *
     * @param timeoutMin SoftAP 자동 OFF 분. {@code -1} 이면 TLV 생략.
     */
    public static byte[] buildConfigSet(String ssid, String pw, String token, int timeoutMin) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(CONFIG_OP_SET);
        writeTlv(out, TLV_VERSION, APP_WIFI_VER.getBytes(StandardCharsets.UTF_8));
        writeTlv(out, TLV_SSID,    ssid.getBytes(StandardCharsets.UTF_8));
        writeTlv(out, TLV_PW,      pw.getBytes(StandardCharsets.UTF_8));
        writeTlv(out, TLV_TOKEN,   token.getBytes(StandardCharsets.UTF_8));
        if (timeoutMin >= 0) {
            writeTlv(out, TLV_TIMEOUT, new byte[] { (byte) (timeoutMin & 0xFF) });
        }
        return out.toByteArray();
    }

    /**
     * 0x41 WIFI_CONTROL ON — timeout 분 후 자동 OFF.
     */
    public static byte[] buildControlOn(int timeoutMin) {
        return new byte[] { (byte) CONTROL_ON, (byte) (timeoutMin & 0xFF) };
    }

    public static byte[] buildControlOff() {
        return new byte[] { (byte) CONTROL_OFF };
    }

    /** 0x42 WIFI_STATUS — empty body. */
    public static byte[] buildStatusReq() {
        return new byte[0];
    }

    /** 0x44 NAND_FORMAT — magic "Jason". */
    public static byte[] buildNandFormat() {
        return NAND_FORMAT_MAGIC.getBytes(StandardCharsets.UTF_8);
    }

    private static void writeTlv(ByteArrayOutputStream out, int tag, byte[] value) {
        out.write(tag & 0xFF);
        out.write(value.length & 0xFF);
        out.write(value, 0, value.length);
    }

    /**
     * 0xC2 STATUS 응답 24바이트 payload 파싱.
     *
     * <pre>
     * [0]      state              0=OFF 1=STARTING 2=READY 3=CONNECTED 4=ERROR
     * [1]      clientCount
     * [2]      batteryPct
     * [3]      charging           0|1
     * [4..5]   timeoutRemainSec   le16
     * [6]      fileOp             0=idle 1=download 2=upload 3=delete 4=ota
     * [7]      fileOpProgressPct
     * [8..11]  freeKb             le32
     * [12..15] totalKb            le32
     * [16..17] audioCount         le16
     * [18..19] offlineCount       le16
     * [20]     rssi               int8
     * [21..23] ip 옥텟 3개
     * (24)     ip 옥텟 4 (24바이트 spec; 3 옥텟 + null 종단 또는 4 옥텟인지 펌웨어팀 확정 필요 — web 코드는 data[21..24] = 4 옥텟)
     * </pre>
     *
     * <p>web js/app.js 는 data[21..24] (4 옥텟) 까지 읽음 → payload 길이는 25바이트일 가능성.
     * 본 파서는 length-defensive — 짧으면 가능한 만큼만 채움.</p>
     */
    public static StatusInfo parseStatus(byte[] data) {
        StatusInfo s = new StatusInfo();
        if (data == null) return s;
        int n = data.length;
        if (n > 0)  s.state            = data[0] & 0xFF;
        if (n > 1)  s.clientCount      = data[1] & 0xFF;
        if (n > 2)  s.batteryPct       = data[2] & 0xFF;
        if (n > 3)  s.charging         = (data[3] & 0xFF) != 0;
        if (n > 5)  s.timeoutRemainSec = (data[4] & 0xFF) | ((data[5] & 0xFF) << 8);
        if (n > 6)  s.fileOp           = data[6] & 0xFF;
        if (n > 7)  s.fileOpProgressPct = data[7] & 0xFF;
        if (n > 11) s.freeKb  = (data[8]  & 0xFF) | ((data[9]  & 0xFF) << 8)
                              | ((data[10] & 0xFF) << 16) | ((data[11] & 0xFF) << 24);
        if (n > 15) s.totalKb = (data[12] & 0xFF) | ((data[13] & 0xFF) << 8)
                              | ((data[14] & 0xFF) << 16) | ((data[15] & 0xFF) << 24);
        if (n > 17) s.audioCount   = (data[16] & 0xFF) | ((data[17] & 0xFF) << 8);
        if (n > 19) s.offlineCount = (data[18] & 0xFF) | ((data[19] & 0xFF) << 8);
        if (n > 20) s.rssi = (byte) (data[20] & 0xFF); // int8
        if (n > 24) {
            s.ip = (data[21] & 0xFF) + "." + (data[22] & 0xFF) + "."
                 + (data[23] & 0xFF) + "." + (data[24] & 0xFF);
        } else if (n > 23) {
            s.ip = (data[21] & 0xFF) + "." + (data[22] & 0xFF) + "."
                 + (data[23] & 0xFF) + ".0";
        }
        return s;
    }

    /** 0xC2 STATUS 응답 파싱 결과. */
    public static final class StatusInfo {
        public int state;
        public int clientCount;
        public int batteryPct;
        public boolean charging;
        public int timeoutRemainSec;
        public int fileOp;
        public int fileOpProgressPct;
        public int freeKb;
        public int totalKb;
        public int audioCount;
        public int offlineCount;
        public int rssi;
        public String ip = "";

        public String stateName() {
            switch (state) {
                case 0: return "OFF";
                case 1: return "STARTING";
                case 2: return "READY";
                case 3: return "CONNECTED";
                case 4: return "ERROR";
                default: return "state_" + state;
            }
        }

        @Override public String toString() {
            return "StatusInfo{state=" + stateName() + " batt=" + batteryPct + "%"
                    + (charging ? "(chg)" : "")
                    + " rssi=" + rssi + " ip=" + ip
                    + " remain=" + timeoutRemainSec + "s"
                    + " audio=" + audioCount + " offline=" + offlineCount
                    + " free=" + freeKb + "KB/" + totalKb + "KB}";
        }
    }
}
