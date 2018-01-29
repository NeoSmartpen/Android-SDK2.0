package kr.neolab.sdk.pen.bluetooth.lib;

/**
 * Protocol Command
 * A : to Activity
 * P : to Pen
 *
 * @author CHY
 */
public class CMD
{
    /**
     * The constant A_PenOnState.
     */
    public static final int A_PenOnState                 = 0x01;
    /**
     * The constant P_PenOnResponse.
     */
    public static final int P_PenOnResponse              = 0x02;
    /**
     * The constant P_RTCset.
     */
    public static final int P_RTCset                     = 0x03;
    /**
     * The constant A_RTCsetResponse.
     */
    public static final int A_RTCsetResponse             = 0x04;
    /**
     * The constant P_Alarmset.
     */
    public static final int P_Alarmset                   = 0x05;
    /**
     * The constant A_AlarmResponse.
     */
    public static final int A_AlarmResponse              = 0x06;
    /**
     * The constant P_ForceCalibrate.
     */
    public static final int P_ForceCalibrate             = 0x07;
    /**
     * The constant A_ForceCalibrateResponse.
     */
    public static final int A_ForceCalibrateResponse     = 0x08;

    /**
     * The constant P_AutoShutdownTime.
     */
    public static final int P_AutoShutdownTime           = 0x09;
    /**
     * The constant A_AutoShutdownTimeResponse.
     */
    public static final int A_AutoShutdownTimeResponse   = 0x0A;
    /**
     * The constant P_PenSensitivity.
     */
    public static final int P_PenSensitivity             = 0x2C;
    /**
     * The constant A_PenSensitivityResponse.
     */
    public static final int A_PenSensitivityResponse     = 0x2D;
    /**
     * The constant P_PenColorSet.
     */
    public static final int P_PenColorSet                = 0x28;
    /**
     * The constant A_PenColorSetResponse.
     */
    public static final int A_PenColorSetResponse        = 0x29;
    /**
     * The constant P_AutoPowerOnSet.
     */
    public static final int P_AutoPowerOnSet             = 0x2A;
    /**
     * The constant A_AutoPowerOnResponse.
     */
    public static final int A_AutoPowerOnResponse        = 0x2B;
    /**
     * The constant P_BeepSet.
     */
    public static final int P_BeepSet                    = 0x2E;
    /**
     * The constant A_BeepSetResponse.
     */
    public static final int A_BeepSetResponse            = 0x2F;

    /**
     * The constant P_UsingNoteNotify.
     */
    public static final int P_UsingNoteNotify            = 0x0B;
    /**
     * The constant A_UsingNoteNotifyResponse.
     */
    public static final int A_UsingNoteNotifyResponse    = 0x0C;

    /**
     * The constant A_PasswordRequest.
     */
    public static final int A_PasswordRequest            = 0x0D;
    /**
     * The constant P_PasswordResponse.
     */
    public static final int P_PasswordResponse           = 0x0E;
    /**
     * The constant P_PasswordSet.
     */
    public static final int P_PasswordSet                = 0x0F;
    /**
     * The constant A_PasswordSetResponse.
     */
    public static final int A_PasswordSetResponse        = 0x10;

    /**
     * The constant A_Echo.
     */
    public static final int A_Echo                       = 0x09;
    /**
     * The constant P_EchoResponse.
     */
    public static final int P_EchoResponse               = 0x0A;
    /**
     * The constant A_DotData.
     */
    public static final int A_DotData                    = 0x11;
    /**
     * The constant A_DotIDChange.
     */
    public static final int A_DotIDChange                = 0x12;
    /**
     * The constant A_DotUpDownData.
     */
    public static final int A_DotUpDownData              = 0x13;
    /**
     * The constant P_DotUpDownResponse.
     */
    public static final int P_DotUpDownResponse          = 0x14;
    /**
     * The constant A_DotIDChange32.
     */
    public static final int A_DotIDChange32              = 0x15;
    /**
     * The constant A_DotUpDownDataNew.
     */
    public static final int A_DotUpDownDataNew           = 0x16;
    /**
     * The constant P_PenStatusRequest.
     */
    public static final int P_PenStatusRequest           = 0x21;

    /**
     * The constant A_PenStatusResponse.
     */
    public static final int A_PenStatusResponse          = 0x25;
    /**
     * The constant P_PenStatusSetup.
     */
    public static final int P_PenStatusSetup             = 0x26;
    /**
     * The constant A_PenStatusSetupResponse.
     */
    public static final int A_PenStatusSetupResponse     = 0x27;

    /**
     * The constant A_OfflineInfo.
     */
    public static final int A_OfflineInfo                = 0x41;
    /**
     * The constant P_OfflineInfoResponse.
     */
    public static final int P_OfflineInfoResponse        = 0x42;
    /**
     * The constant A_OfflineChunk.
     */
    public static final int A_OfflineChunk               = 0x43;
    /**
     * The constant P_OfflineChunkResponse.
     */
    public static final int P_OfflineChunkResponse       = 0x44;
    /**
     * The constant P_OfflineNoteList.
     */
    public static final int P_OfflineNoteList            = 0x45;
    /**
     * The constant A_OfflineNoteListResponse.
     */
    public static final int A_OfflineNoteListResponse    = 0x46;
    /**
     * The constant P_OfflineDataRequest.
     */
    public static final int P_OfflineDataRequest         = 0x47;
    /**
     * The constant A_OfflineResultResponse.
     */
    public static final int A_OfflineResultResponse 	 = 0x48;
    /**
     * The constant A_OfflineDataInfo.
     */
    public static final int A_OfflineDataInfo            = 0x49;
    /**
     * The constant P_OfflineDataRemove.
     */
    public static final int P_OfflineDataRemove          = 0x4A;
    /**
     * The constant A_OfflineDataRemoveResponse.
     */
    public static final int A_OfflineDataRemoveResponse  = 0x4B;

    /**
     * The constant P_PenSWUpgradeCommand.
     */
    public static final int P_PenSWUpgradeCommand        = 0x51;
    /**
     * The constant A_PenSWUpgradeRequest.
     */
    public static final int A_PenSWUpgradeRequest        = 0x52;
    /**
     * The constant P_PenSWUpgradeResponse.
     */
    public static final int P_PenSWUpgradeResponse       = 0x53;
    /**
     * The constant A_PenSWUpgradeStatus.
     */
    public static final int A_PenSWUpgradeStatus         = 0x54;
    /**
     * The constant A_PenDebug.
     */
    public static final int A_PenDebug                   = 0xE5;

    /**
     * The constant P_ProfileRequest.
     */
    public static final int P_ProfileRequest                   = 0x61;

    /**
     * The constant A_ProfileResponse.
     */
    public static final int A_ProfileResponse                   = 0x62;

}
