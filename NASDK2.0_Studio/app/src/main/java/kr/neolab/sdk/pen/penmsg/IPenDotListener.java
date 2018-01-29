package kr.neolab.sdk.pen.penmsg;

import kr.neolab.sdk.ink.structure.Dot;

/**
 * Created by LMS on 2016-07-27.
 */
public interface IPenDotListener
{
    /**
     * Fired when a receive dot successfully, override to handle in your own code
     * supported from Protocol 2.0
     *
     * @param macAddress the mac address
     * @param dot        the dot
     */
    public void onReceiveDot(String macAddress, Dot dot);

}
