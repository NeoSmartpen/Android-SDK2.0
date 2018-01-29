package kr.neolab.sdk.pen.penmsg;

/**
 * Been sent from the pen to implement an event callback interface
 *
 * @author CHY
 */
public interface IPenMsgListener
{

    /**
     * Fired when a receive message from pen, override to handle in your own code
     *
     * @param penAddress the pen address
     * @param penMsg     message object from pen
     */
    public void onReceiveMessage(String penAddress, PenMsg penMsg);
}
