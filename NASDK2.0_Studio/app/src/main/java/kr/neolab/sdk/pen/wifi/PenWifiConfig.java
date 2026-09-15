package kr.neolab.sdk.pen.wifi;

import androidx.annotation.NonNull;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * SoftAP 접속 / HTTP 다운로드 서버 설정값.
 *
 * <p>NWP_WiFi_Protocol_v1.0 — 앱이 SSID/PW/TOKEN 을 동적으로 생성해 펜에게
 * {@code 0x40 WIFI_CONFIG SET} 으로 전달. 펜은 그 값으로 SoftAP 를 띄움.
 * host 는 펌웨어 spec 상 항상 {@code 192.168.4.1}.</p>
 */
public final class PenWifiConfig {

    /** SoftAP 게이트웨이 (펌웨어 고정). */
    public static final String DEFAULT_HOST = "192.168.4.1";
    /** HTTP /v1 포트 (펌웨어 고정, 80). */
    public static final int    DEFAULT_PORT = 80;

    public final String ssid;
    public final String password;
    public final String host;
    public final int    port;
    public final String token;

    public PenWifiConfig(@NonNull String ssid, @NonNull String password,
                         @NonNull String host, int port, @NonNull String token) {
        this.ssid = ssid;
        this.password = password;
        this.host = host;
        this.port = port;
        this.token = token;
    }

    /**
     * 매 세션마다 새로 — SSID/PW/TOKEN 모두 무작위. 펜에게 0x40 으로 보낼 값.
     *
     * <p>SSID: {@code NPEN-} + 8 hex chars (총 13글자, 32자 한도 안). WPA2 호환 위해 8자 이상 영숫자.<br>
     * PW: 12 영숫자 (WPA2 최소 8자 만족 + 충분한 엔트로피).<br>
     * TOKEN: UUID 의 대시를 제거한 32자 hex — HTTP /v1 query string 검증용.
     * ★프로토콜 spec §2.3 TOKEN 은 1~32 byte. 대시 포함 UUID(36자)는 한도 초과라
     * 펜이 0x40 CONFIG 를 ERR_VALUE(missing_mask=TOKEN) 로 거부한다 → 대시 제거 필수.</p>
     */
    public static PenWifiConfig random() {
        SecureRandom rnd = new SecureRandom();
        String ssid = "NPEN-" + hexNibbles(rnd, 8);
        String pw   = alnum(rnd, 12);
        String tok  = UUID.randomUUID().toString().replace("-", "");  // 36 → 32 hex (spec 32 byte 한도)
        return new PenWifiConfig(ssid, pw, DEFAULT_HOST, DEFAULT_PORT, tok);
    }

    public String baseUrl() {
        return "http://" + host + (port == 80 ? "" : ":" + port);
    }

    private static String hexNibbles(SecureRandom rnd, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(Integer.toHexString(rnd.nextInt(16)));
        return sb.toString();
    }

    private static String alnum(SecureRandom rnd, int n) {
        // 영숫자만 — WPA2 passphrase 호환 (특수문자는 펌웨어 입력 검증에서 거부 가능).
        final String pool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(pool.charAt(rnd.nextInt(pool.length())));
        return sb.toString();
    }
}
