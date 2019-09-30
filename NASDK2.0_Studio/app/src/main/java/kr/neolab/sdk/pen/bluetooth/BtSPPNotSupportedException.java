package kr.neolab.sdk.pen.bluetooth;

/**
 * Created by CJY on 2017-02-23.
 */
public class BtSPPNotSupportedException extends Exception {
    /**
     * Instantiates a Bluetooth LE not supported exception.
     *
     * @param detailMessage the detail message
     */
    public BtSPPNotSupportedException(String detailMessage)
    {
        super(detailMessage);
    }
}
