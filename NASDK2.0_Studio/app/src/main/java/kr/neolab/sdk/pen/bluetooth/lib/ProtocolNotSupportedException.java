package kr.neolab.sdk.pen.bluetooth.lib;

/**
 * Created by LMS on 2016-03-24.
 */
public class ProtocolNotSupportedException extends Exception
{
    /**
     * Instantiates a new Protocol not supported exception.
     *
     * @param detailMessage the detail message
     */
    public ProtocolNotSupportedException(String detailMessage)
    {
        super(detailMessage);
    }
}
