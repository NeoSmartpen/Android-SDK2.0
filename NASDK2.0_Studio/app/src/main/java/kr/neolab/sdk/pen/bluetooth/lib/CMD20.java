package kr.neolab.sdk.pen.bluetooth.lib;

/**
 * Protocol Command
 * A : to Activity
 * P : to Pen
 *
 * @author Moo
 */
public class CMD20
{
    /**
     * The constant RES_EventBattery.
     */
// EVENT CMD
    public static final int RES_EventBattery = 0x61;
    /**
     * The constant RES_EventPowerOff.
     */
    public static final int RES_EventPowerOff = 0x62;
    /**
     * The constant RES_EventPenUpDown.
     */
    public static final int RES_EventPenUpDown = 0x63;
    /**
     * The constant RES_EventIdChange.
     */
    public static final int RES_EventIdChange = 0x64;
    /**
     * The constant RES_EventDotData.
     */
    public static final int RES_EventDotData = 0x65;
	// RES_EventDotData2, RES_EventDotData3 는 전송이 느린 BT(ex D100)를 위해 추가된 프로토콜
	// 필기 데이터는 RES_EventDotData, RES_EventDotData2, RES_EventDotData3 중 한개로 넘어온다.
	public static final int RES_EventDotData2 = 0x66;
	public static final int RES_EventDotData3 = 0x67;
    /**
     * The constant RES_EventUploadPenFWChunk.
     */
    public static final int RES_EventUploadPenFWChunk = 0x32;

    /**
     * The constant REQ_PenInfo.
     */
// 일반 CMD
    public static final int REQ_PenInfo = 0x01;
    /**
     * The constant RES_PenInfo.
     */
    public static final int RES_PenInfo = 0x81;

    /**
     * The constant REQ_Password.
     */
    public static final int REQ_Password = 0x02;
    /**
     * The constant RES_Password.
     */
    public static final int RES_Password = 0x82;
    /**
     * The constant REQ_PasswordSet.
     */
    public static final int REQ_PasswordSet = 0x03;
    /**
     * The constant RES_PasswordSet.
     */
    public static final int RES_PasswordSet = 0x83;

    /**
     * The constant REQ_PenStatus.
     */
    public static final int REQ_PenStatus = 0x04;
    /**
     * The constant RES_PenStatus.
     */
    public static final int RES_PenStatus = 0x84;

    /**
     * The constant REQ_PenStatusChange.
     */
    public static final int REQ_PenStatusChange = 0x05;
    /**
     * The constant RES_PenStatusChange.
     */
    public static final int RES_PenStatusChange = 0x85;

    /**
     * The constant REQ_PenStatusChange_TYPE_CurrentTimeSet.
     */
    public static final int REQ_PenStatusChange_TYPE_CurrentTimeSet = 0x01;
    /**
     * The constant REQ_PenStatusChange_TYPE_AutoShutdownTime.
     */
    public static final int REQ_PenStatusChange_TYPE_AutoShutdownTime = 0x02;
    /**
     * The constant REQ_PenStatusChange_TYPE_PenCapOnOff.
     */
    public static final int REQ_PenStatusChange_TYPE_PenCapOnOff = 0x03;
    /**
     * The constant REQ_PenStatusChange_TYPE_AutoPowerOnSet.
     */
    public static final int REQ_PenStatusChange_TYPE_AutoPowerOnSet = 0x04;
    /**
     * The constant REQ_PenStatusChange_TYPE_BeepOnOff.
     */
    public static final int REQ_PenStatusChange_TYPE_BeepOnOff = 0x05;
    /**
     * The constant REQ_PenStatusChange_TYPE_HoverOnOff.
     */
    public static final int REQ_PenStatusChange_TYPE_HoverOnOff = 0x06;
    /**
     * The constant REQ_PenStatusChange_TYPE_OfflineDataSaveOnOff.
     */
    public static final int REQ_PenStatusChange_TYPE_OfflineDataSaveOnOff = 0x07;
    /**
     * The constant REQ_PenStatusChange_TYPE_LEDColorSet.
     */
    public static final int REQ_PenStatusChange_TYPE_LEDColorSet = 0x08;
    /**
     * The constant REQ_PenStatusChange_TYPE_SensitivitySet.
     */
    public static final int REQ_PenStatusChange_TYPE_SensitivitySet = 0x09;

    /**
     * The constant REQ_UsingNoteNotify.
     */
    public static final int REQ_UsingNoteNotify = 0x11;
    /**
     * The constant RES_UsingNoteNotify.
     */
    public static final int RES_UsingNoteNotify = 0x91;

    /**
     * The constant REQ_OfflineNoteList.
     */
    public static final int REQ_OfflineNoteList = 0x21;
    /**
     * The constant RES_OfflineNoteList.
     */
    public static final int RES_OfflineNoteList = 0xA1;
    /**
     * The constant REQ_OfflinePageList.
     */
    public static final int REQ_OfflinePageList = 0x22;
    /**
     * The constant RES_OfflinePageList.
     */
    public static final int RES_OfflinePageList = 0xA2;
    /**
     * The constant REQ_OfflineDataRequest.
     */
    public static final int REQ_OfflineDataRequest = 0x23;
    /**
     * The constant RES_OfflineDataRequest.
     */
    public static final int RES_OfflineDataRequest = 0xA3;
    /**
     * The constant RES_OfflineChunk.
     */
    public static final int RES_OfflineChunk = 0x24;
    /**
     * The constant ACK_OfflineChunk.
     */
    public static final int ACK_OfflineChunk = 0xA4;

    /**
     * The constant REQ_OfflineNoteRemove.
     */
    public static final int REQ_OfflineNoteRemove = 0x25;
    /**
     * The constant RES_OfflineNoteRemove.
     */
    public static final int RES_OfflineNoteRemove = 0xA5;

    /**
     * The constant REQ_PenFWUpgrade.
     */
    public static final int REQ_PenFWUpgrade = 0x31;
    /**
     * The constant RES_PenFWUpgrade.
     */
    public static final int RES_PenFWUpgrade = 0xB1;
    /**
     * The constant ACK_UploadPenFWChunk.
     */
    public static final int ACK_UploadPenFWChunk = 0xB2;

    /**
     * Is event cmd boolean.
     *
     * @param cmd the cmd
     * @return the boolean
     */
    public static boolean isEventCMD ( int cmd )
    {
        int CMD = (int) cmd;
		if ( cmd == CMD20.RES_EventBattery || cmd == CMD20.RES_EventDotData || cmd == CMD20.RES_EventDotData2 || cmd == CMD20.RES_EventDotData3 || cmd == CMD20.RES_EventIdChange || cmd == CMD20.RES_EventPenUpDown || cmd == CMD20.RES_EventPowerOff  || cmd == CMD20.RES_OfflineChunk || cmd == CMD20.RES_EventUploadPenFWChunk )
        {
            return true;
        }
        return false;

    }

    /**
     * Is event cmd boolean.
     *
     * @param cmd the cmd
     * @return the boolean
     */
    public static boolean isEventCMD ( byte cmd )
    {
        int newValue = ( (int) cmd ) & 0xFF;
        return isEventCMD( newValue );

    }

}
