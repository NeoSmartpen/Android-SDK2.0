package kr.neolab.sdk.pen.wifi;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import kr.neolab.sdk.util.NLog;

/**
 * 펜의 SoftAP 에 강제로 접속한 뒤 해당 {@link Network} 객체를 콜백으로 넘긴다.
 *
 * <p>Android 10+ ({@code WifiNetworkSpecifier}) 방식 사용 — 시스템 다이얼로그가 1회 떠서 사용자
 * 승인을 요구함. 일반 데이터 트래픽은 셀룰러로 계속 흐르고, 본 SDK 가 만든 HTTP 요청만 펜
 * 네트워크로 routing 하기 위해 호출부가 {@code Network#openConnection} 또는
 * {@code Network#bindSocket} 으로 묶어 사용해야 한다.</p>
 */
@RequiresApi(Build.VERSION_CODES.Q)
public final class PenWifiConnector {

    public interface Listener {
        /** SoftAP 접속 성공 — 이 network 로 모든 HTTP 요청을 묶어 보내야 함. */
        void onConnected(@NonNull Network network);
        /** 사용자 거부 / timeout / 펜 SoftAP 미발견. */
        void onFailed(@NonNull String reason);
        /** 접속이 끊김. */
        void onLost();
    }

    private final Context appContext;
    private final ConnectivityManager cm;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable private ConnectivityManager.NetworkCallback callback;

    public PenWifiConnector(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * SoftAP 접속 시도. 결과는 {@link Listener} 로 전달. 호출 후 사용자가 시스템 Wi-Fi
     * 선택 다이얼로그에서 허용해야 onConnected 가 도착함.
     */
    public void connect(@NonNull PenWifiConfig config, @NonNull Listener listener) {
        if (callback != null) {
            NLog.d( "[wifi] connect ignored — already requesting" );
            return;
        }
        // ===== 진단 로그 — 시스템 단에서 즉시 거부되는 경우 원인 추적 =====
        logWifiDiagnostics( config );

        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(config.ssid)
                .setWpa2Passphrase(config.password)
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                // SoftAP 는 인터넷 없음 — capability 명시 안 함
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build();

        callback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(@NonNull Network network) {
                NLog.d( "[wifi] onAvailable network=" + network
                        + " ssid='" + config.ssid + "' host=" + config.host + ":" + config.port );
                main.post(() -> listener.onConnected(network));
            }
            @Override public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                NLog.d( "[wifi] onCapabilitiesChanged network=" + network + " caps=" + caps );
            }
            @Override public void onUnavailable() {
                NLog.d( "[wifi] onUnavailable — SoftAP request rejected/timed-out (사용자 거부 가능)" );
                main.post(() -> listener.onFailed("unavailable"));
                cleanup();
            }
            @Override public void onLost(@NonNull Network network) {
                NLog.d( "[wifi] onLost network=" + network );
                main.post(listener::onLost);
            }
        };
        // 사전 권한 점검 — API 33+ NEARBY_WIFI_DEVICES 미허용 시 requestNetwork 가 즉시 onUnavailable.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            int nearbyState = appContext.checkSelfPermission( android.Manifest.permission.NEARBY_WIFI_DEVICES );
            NLog.d( "[wifi] NEARBY_WIFI_DEVICES granted=" + (nearbyState == android.content.pm.PackageManager.PERMISSION_GRANTED) );
        }
        NLog.d( "[wifi] requestNetwork ssid='" + config.ssid + "' host=" + config.host + ":" + config.port
                + " (사용자 승인 다이얼로그 노출 예정)" );
        cm.requestNetwork(request, callback);
    }

    /**
     * 접속 해제 — 시스템이 SoftAP 네트워크를 release.
     */
    public void disconnect() {
        cleanup();
    }

    private void cleanup() {
        if (callback != null) {
            try { cm.unregisterNetworkCallback(callback); }
            catch (Throwable t) { NLog.d( "[wifi] unregister callback failed: " + t.getMessage() ); }
            callback = null;
        }
    }

    /**
     * requestNetwork 호출 직전 Wi-Fi 시스템 상태를 dump. 즉시 onUnavailable 시 원인 추적용.
     */
    private void logWifiDiagnostics(@NonNull PenWifiConfig config) {
        try {
            WifiManager wm = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
            if (wm == null) {
                NLog.d( "[wifi-diag] WifiManager null" );
                return;
            }
            boolean enabled = wm.isWifiEnabled();
            int wifiState  = wm.getWifiState();   // 0=disabling 1=disabled 2=enabling 3=enabled 4=unknown
            NLog.d( "[wifi-diag] wifiEnabled=" + enabled + " state=" + wifiState );

            // 현재 STA 로 붙어있는 SSID (Android 8+ "<unknown ssid>" 가능)
            android.net.wifi.WifiInfo info = wm.getConnectionInfo();
            String currSsid = info != null ? info.getSSID() : "<null>";
            int rssi = info != null ? info.getRssi() : 0;
            NLog.d( "[wifi-diag] current STA ssid=" + currSsid + " rssi=" + rssi );

            // 스캔 결과 — 캐시된 last scan. NEARBY_WIFI_DEVICES 권한이 있어야 SSID 노출됨.
            List<ScanResult> scans = wm.getScanResults();
            if (scans == null || scans.isEmpty()) {
                NLog.d( "[wifi-diag] scanResults empty — try Settings > Wi-Fi 한 번 열어 강제 스캔 후 재시도" );
            } else {
                NLog.d( "[wifi-diag] scanResults size=" + scans.size() );
                boolean found = false;
                for (ScanResult sr : scans) {
                    String s = sr.SSID == null ? "" : sr.SSID;
                    if (s.equals(config.ssid) || s.equalsIgnoreCase(config.ssid)) {
                        found = true;
                        NLog.d( "[wifi-diag]   ✓ MATCH ssid='" + sr.SSID + "' bssid=" + sr.BSSID
                                + " freq=" + sr.frequency + "MHz level=" + sr.level
                                + " caps='" + sr.capabilities + "'" );
                    }
                }
                if (!found) {
                    NLog.d( "[wifi-diag]   ✗ '" + config.ssid + "' NOT in scan cache — listing top 5 by rssi:" );
                    // 신호 강한 순으로 5개만
                    scans.sort((a, b) -> Integer.compare(b.level, a.level));
                    int count = Math.min(5, scans.size());
                    for (int i = 0; i < count; i++) {
                        ScanResult sr = scans.get(i);
                        NLog.d( "[wifi-diag]     " + (i+1) + ". ssid='" + sr.SSID
                                + "' bssid=" + sr.BSSID + " level=" + sr.level
                                + " caps='" + sr.capabilities + "'" );
                    }
                }
            }
        } catch (Throwable t) {
            NLog.d( "[wifi-diag] dump failed: " + t.getMessage() );
        }
    }
}
