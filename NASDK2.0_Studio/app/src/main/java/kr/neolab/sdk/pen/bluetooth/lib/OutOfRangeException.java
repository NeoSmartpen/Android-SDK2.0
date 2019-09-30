package kr.neolab.sdk.pen.bluetooth.lib;

/**
 * Created by HRL on 2019-08-13.
 */
public class OutOfRangeException extends Exception
{
    /**
     * Instantiates a Out of Range after protocol 2.15v exception.
     *
     * @param detailMessage the detail message
     */
    public OutOfRangeException(String detailMessage)
    {
        super(detailMessage);
    }
}
