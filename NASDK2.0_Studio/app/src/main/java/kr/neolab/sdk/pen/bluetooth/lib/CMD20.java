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
    /**
     * The constant RES_EventDotData2.
     */
    public static final int RES_EventDotData2 = 0x66;
    /**
     * The constant RES_EventDotData3.
     */
    public static final int RES_EventDotData3 = 0x67;
    /**
     * The constant RES_EventPenDown.
     */
    public static final int RES_EventPenDown = 0x69;
    /**
     * The constant RES_EventPenDown.
     */
    public static final int RES_EventPenUp = 0x6A;
    /**
     * The constant RES_EventIdChange2.
     */
    public static final int RES_EventIdChange2 = 0x6B;
    /**
     * The constant RES_EventDotData4.
     */
    public static final int RES_EventDotData4 = 0x6C;
    /**
     * The constant RES_EventDotData5(hover mode).
     */
    public static final int RES_EventDotData5 = 0x6F;
    /**
     * The constant RES_EventErrorDot2.
     */
    public static final int RES_EventErrorDot2 = 0x6D;
    /**
     * The constant RES_EventUploadPenFWChunk.
     */
    public static final int RES_EventUploadPenFWChunk = 0x32;


// normal CMD
    /**
     * The constant REQ_PenInfo.
     */
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
     * v2.08
     * The constant REQ_PenStatusChange_TYPE_SensitivitySet_FSC.
     */
    public static final int REQ_PenStatusChange_TYPE_SensitivitySet_FSC = 0x0D;

    /**
     * v2.15
     * The constant REQ_PenStatusChange_TYPE_Disk_Reset.
     */
    public static final int REQ_PenStatusChange_TYPE_Disk_Reset = 0x11;

    /**
     * The constant REQ_PenStatusChange_TYPE_Camera_Register.
     */
    public static final int REQ_PenStatusChange_TYPE_Camera_Register = 0x16;


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
     * The constant REQ_OfflineNoteInfo.
     */
    public static final int REQ_OfflineNoteInfo = 0x26;
    /**
     * The constant RES_OfflineNoteInfo.
     */
    public static final int RES_OfflineNoteInfo = 0xA6;

    /**
     * The constant REQ_OfflinePageRemove.
     */
    public static final int REQ_OfflinePageRemove = 0x27;
    /**
     * The constant RES_OfflinePageRemove.
     */
    public static final int RES_OfflinePageRemove = 0xA7;
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
     * The constant REQ_PenProfile.
     */
    public static final int REQ_PenProfile = 0x41;

    /**
     * The constant RES_PenProfile.
     */
    public static final int RES_PenProfile = 0xC1;


    //Check if pen is support setting performance
    public static final int REQ_SystemInfo = 0x07;

    public static final int RES_SystemInfo = 0x87;

    //For setting performance
    public static final int REQ_SetPerformance = 0x06;

    public static final int RES_SetPerformance = 0x86;
    /**
     * Is event cmd boolean.
     *
     * @param cmd the cmd
     * @return the boolean
     */
    public static boolean isEventCMD ( int cmd )
    {
        int CMD = (int) cmd;
        if((CMD >= 0x60 && CMD <= 0x6F) ||  CMD == CMD20.RES_OfflineChunk || CMD == CMD20.RES_EventUploadPenFWChunk || (CMD >= 0x78 && CMD <= 0x7F) )
            return true;
        else
            return false;
//        if ( cmd == CMD20.RES_EventBattery || cmd == CMD20.RES_EventDotData || cmd == CMD20.RES_EventDotData2 || cmd == CMD20.RES_EventDotData3 || cmd == CMD20.RES_EventIdChange || cmd == CMD20.RES_EventPenUpDown || cmd == CMD20.RES_EventPowerOff || cmd == CMD20.RES_OfflineChunk || cmd == CMD20.RES_EventUploadPenFWChunk )
//        {
//            return true;
//        }
//        return false;

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
