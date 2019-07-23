package kr.neolab.sdk.pen.offline;

/**
 * Created by CJY on 2017-02-23.
 */
public class CheckSumException extends Exception {
    /**
     * Instantiates a Bluetooth LE not supported exception.
     *
     * @param detailMessage the detail message
     */
    public CheckSumException ( String detailMessage)
    {
        super(detailMessage);
    }
}
