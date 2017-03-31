package kr.neolab.sdk.pen.bluetooth;

import kr.neolab.sdk.pen.filter.Fdot;
import kr.neolab.sdk.pen.offline.OfflineByteData;
import kr.neolab.sdk.pen.penmsg.PenMsg;

/**
 * Created by CJY on 2017-02-21.
 */

public interface IConnectedThread {
    /**
     * Unbind
     */
    void unbind();

    /**
     * Unbind
     *
     * @param isRegularDisconnect is regular disconnect
     */
    void unbind(boolean isRegularDisconnect);

    /**
     * Get the established status
     *
     * @return It is established status
     */
    boolean getIsEstablished();

    /**
     * Get the mac address.
     *
     * @return mac address (string)
     */
    String getMacAddress();
    /**
     * Set established status.
     */
    void onEstablished();

    /**
     * Set Authorized status.
     */
    void onAuthorized();

    /**
     * Send to data to pen.
     *
     * @param buffer output buffer
     */
    void write(byte[] buffer);

    /**
     * On create message.
     *
     * @param msg the pen message
     */
    void onCreateMsg(PenMsg msg);
    /**
     *  On create dot.
     *
     * @param dot the dot data
     */
    void onCreateDot(Fdot dot);

    /**
     * On create offline strokes.
     *
     * @param offlineByteData the offline byte data
     */
    void onCreateOfflineStrokes( OfflineByteData offlineByteData);

    /**
     * Get allowOffline
     * Adapter에서 static변수로 사용하던 allowOffline을 가져오는 함수
     * static으로 사용할 경우 여러가지 문제가 있으며, processor에서 접근에도 문제가 있기 때문에
     * 아래의 함수를 만들어 받아올 수 있도록 함 by jychoi
     *
     * @return allowOffline status
     */
    boolean getAllowOffline();
}
