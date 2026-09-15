package kr.neolab.sdk.pen.wifi;

import androidx.annotation.NonNull;

/**
 * 펜 SoftAP {@code GET /v1/files} 응답 items[] 한 항목.
 *
 * <p>예시 JSON: {@code { "file_id": 17, "name": "REC_0017.OGG", "size_bytes": 340000,
 * "type": "audio" }}</p>
 *
 * <p>{@code type} 은 {@link #TYPE_AUDIO} 또는 {@link #TYPE_OFFLINE} — query string
 * {@code ?type=} 으로 다운로드/삭제 경로에도 전달.</p>
 */
public final class PenWifiFile {

    public static final String TYPE_AUDIO   = "audio";
    public static final String TYPE_OFFLINE = "offline";

    public final long fileId;
    @NonNull public final String name;
    public final long sizeBytes;
    @NonNull public final String type;

    public PenWifiFile(long fileId, @NonNull String name, long sizeBytes, @NonNull String type) {
        this.fileId = fileId;
        this.name = name;
        this.sizeBytes = sizeBytes;
        this.type = type;
    }

    @Override public String toString() {
        return name + " (id=" + fileId + ", " + sizeBytes + "B, " + type + ")";
    }
}
