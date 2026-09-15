package kr.neolab.sdk.pen.wifi;

import android.net.Network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import kr.neolab.sdk.util.NLog;

/**
 * 펜 SoftAP HTTP /v1 클라이언트 (NWP_WiFi_Protocol_v1.0).
 *
 * <p>모든 요청은 {@link Network#openConnection(URL)} 으로 SoftAP 네트워크에 바인딩 — 셀룰러
 * 트래픽과 분리.</p>
 *
 * <pre>
 *   GET    /v1/status?token=                                      — 상태
 *   GET    /v1/files?type=audio|offline&amp;page=N&amp;page_size=N&amp;token=  — 목록 (페이지네이션)
 *   GET    /v1/files/{file_id}?token=&amp;type=&amp;name=                — 다운로드 (octet-stream)
 *   DELETE /v1/files/{file_id}?token=&amp;type=&amp;name=                — 삭제
 * </pre>
 */
public final class PenFileClient {

    public interface DownloadListener {
        default void onStart(long totalBytes) { }
        default void onProgress(long received, long totalBytes) { }
        default void onComplete(@NonNull File file) { }
        default void onError(@NonNull String reason) { }
    }

    private static final int CONN_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_READ_TIMEOUT_MS = 60_000;
    private static final int DL_BUF = 8 * 1024;
    /** web 레퍼런스(file-manager.js)와 정렬. */
    private static final int PAGE_SIZE = 64;

    private final Network network;
    private final PenWifiConfig config;

    public PenFileClient(@NonNull Network network, @NonNull PenWifiConfig config) {
        this.network = network;
        this.config = config;
    }

    /** /v1/status — 연결 확인용. raw response body. */
    @NonNull
    public String getStatus() throws IOException {
        HttpURLConnection conn = open("/v1/status");
        try {
            int code = conn.getResponseCode();
            String body = readBody(conn);
            if (code != 200) throw new IOException("HTTP " + code + " " + body);
            return body;
        } finally { conn.disconnect(); }
    }

    /**
     * 특정 type 의 파일 목록. 모든 페이지를 합쳐 반환.
     * @param type {@link PenWifiFile#TYPE_AUDIO} 또는 {@link PenWifiFile#TYPE_OFFLINE}
     */
    @NonNull
    public List<PenWifiFile> listFiles(@NonNull String type) throws IOException {
        List<PenWifiFile> out = new ArrayList<>();
        int page = 0;
        long startNanos = System.nanoTime();
        NLog.d( "[wifi-list] GET /v1/files type=" + type + " start" );
        while (true) {
            HttpURLConnection conn = open("/v1/files?type=" + encode(type)
                    + "&page=" + page + "&page_size=" + PAGE_SIZE);
            try {
                int code = conn.getResponseCode();
                String body = readBody(conn);
                NLog.d( "[wifi-list] page=" + page + " HTTP " + code + " body_len=" + body.length() );
                if (code != 200) throw new IOException("HTTP " + code + " " + body);
                int got;
                try {
                    JSONObject json = new JSONObject(body);
                    JSONArray items = json.optJSONArray("items");
                    if (items == null) {
                        NLog.d( "[wifi-list] no 'items' field → ending" );
                        break;
                    }
                    got = items.length();
                    int total = json.optInt("total_count", -1);
                    NLog.d( "[wifi-list] page=" + page + " items=" + got + " total_count=" + total );
                    for (int i = 0; i < got; i++) {
                        JSONObject o = items.getJSONObject(i);
                        PenWifiFile f = new PenWifiFile(
                                o.optLong("file_id", -1),
                                o.optString("name", ""),
                                o.optLong("size_bytes", 0),
                                o.optString("type", type));
                        out.add(f);
                        NLog.d( "[wifi-list]   #" + out.size() + " id=" + f.fileId
                                + " name='" + f.name + "' size=" + f.sizeBytes + "B type=" + f.type );
                    }
                    if (total >= 0 && out.size() >= total) break;
                    if (got < PAGE_SIZE) break;
                    page++;
                } catch (JSONException je) {
                    throw new IOException("list parse failed: " + je.getMessage());
                }
            } finally { conn.disconnect(); }
        }
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        NLog.d( "[wifi-list] DONE type=" + type + " total=" + out.size()
                + " file(s) elapsed=" + elapsedMs + "ms" );
        return out;
    }

    /** /v1/files/{id}?type=&name= — 단일 파일 다운로드. */
    public void downloadFile(@NonNull PenWifiFile file,
                             @NonNull File outDir,
                             @NonNull String outFileName,
                             @Nullable DownloadListener listener) {
        HttpURLConnection conn = null;
        FileOutputStream fos = null;
        InputStream in = null;
        File outFile = new File(outDir, outFileName);
        long startNanos = System.nanoTime();
        long lastLogReceived = 0;
        try {
            if (!outDir.exists() && !outDir.mkdirs()) {
                throw new IOException("Cannot create output dir: " + outDir);
            }
            NLog.d( "[wifi-dl] BEGIN file_id=" + file.fileId + " name='" + file.name
                    + "' type=" + file.type + " expectedSize=" + file.sizeBytes
                    + " → " + outFile.getAbsolutePath() );
            conn = open("/v1/files/" + file.fileId
                    + "?type=" + encode(file.type)
                    + "&name=" + encode(file.name));
            conn.setReadTimeout(DOWNLOAD_READ_TIMEOUT_MS);
            int code = conn.getResponseCode();
            if (code != 200) {
                String body = readBody(conn);
                throw new IOException("HTTP " + code + " " + body);
            }
            long total = conn.getContentLengthLong();
            String contentType = conn.getContentType();
            NLog.d( "[wifi-dl] HTTP 200 contentType=" + contentType
                    + " contentLength=" + total + " (sizeBytes fallback=" + file.sizeBytes + ")" );
            if (total <= 0) total = file.sizeBytes;
            if (listener != null) listener.onStart(total);

            in = conn.getInputStream();
            fos = new FileOutputStream(outFile);
            byte[] buf = new byte[DL_BUF];
            long received = 0;
            int n;
            long lastEmit = 0;
            long nextQuartile = total > 0 ? total / 4 : Long.MAX_VALUE;
            while ((n = in.read(buf)) != -1) {
                fos.write(buf, 0, n);
                received += n;
                long now = System.currentTimeMillis();
                if (listener != null && (now - lastEmit > 100 || received >= total)) {
                    listener.onProgress(received, total);
                    lastEmit = now;
                }
                if (received >= nextQuartile && total > 0) {
                    int pct = (int) (100L * received / total);
                    NLog.d( "[wifi-dl] progress '" + file.name + "' " + received + "/" + total
                            + " (" + pct + "%) +" + (received - lastLogReceived) + "B since prev" );
                    lastLogReceived = received;
                    nextQuartile += total / 4;
                }
            }
            fos.flush();
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            double kbps = elapsedMs > 0 ? (received / 1024.0) / (elapsedMs / 1000.0) : 0;
            NLog.d( "[wifi-dl] DONE name='" + file.name + "' saved=" + outFile.getAbsolutePath()
                    + " received=" + received + "B expected=" + total + "B"
                    + " elapsed=" + elapsedMs + "ms speed=" + String.format(java.util.Locale.ROOT, "%.1f", kbps) + " KB/s" );
            if (received < total && total > 0) {
                NLog.d( "[wifi-dl] WARNING: short read — received(" + received + ") < expected(" + total + ")" );
            }
            if (listener != null) listener.onComplete(outFile);
        } catch (IOException e) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            NLog.d( "[wifi-dl] FAILED name='" + file.name + "' after " + elapsedMs
                    + "ms reason=" + e.getMessage() );
            if (listener != null) listener.onError(e.getMessage() != null ? e.getMessage() : "io error");
        } finally {
            closeQuietly(in);
            closeQuietly(fos);
            if (conn != null) conn.disconnect();
        }
    }

    /** DELETE /v1/files/{id}?type=&name= — 단일 파일 삭제. */
    public boolean deleteFile(@NonNull PenWifiFile file) throws IOException {
        HttpURLConnection conn = open("/v1/files/" + file.fileId
                + "?type=" + encode(file.type)
                + "&name=" + encode(file.name));
        conn.setRequestMethod("DELETE");
        try {
            int code = conn.getResponseCode();
            String body = readBody(conn);
            NLog.d( "[wifi-del] file_id=" + file.fileId + " name='" + file.name
                    + "' HTTP " + code + " body=" + body );
            return code >= 200 && code < 300;
        } finally { conn.disconnect(); }
    }

    private HttpURLConnection open(String path) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        String url = config.baseUrl() + path + sep + "token=" + encode(config.token);
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) network.openConnection(u);
        conn.setConnectTimeout(CONN_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json, application/octet-stream");
        return conn;
    }

    private static String encode(@NonNull String s) {
        try { return URLEncoder.encode(s, "UTF-8"); }
        catch (java.io.UnsupportedEncodingException e) { return s; }
    }

    @NonNull
    private static String readBody(@NonNull HttpURLConnection conn) {
        InputStream in = null;
        try {
            in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) return "";
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (IOException e) {
            return "";
        } finally { closeQuietly(in); }
    }

    private static void closeQuietly(@Nullable java.io.Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) { }
    }
}
