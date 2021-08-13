package kr.neolab.sdk.pen.penmsg;

/**
 * To identify messages that are sent to the pen.
 *
 * @author CHY
 */
public class PenMsgType
{
    /**
     * Pen events that occur when you attempt to connect to
     */
    public final static int PEN_CONNECTION_TRY			= 0x01;

    /**
     * Pens when the connection is successful, the events that occur
     */
    public final static int PEN_CONNECTION_SUCCESS		= 0x02;

    /**
     * Pens when the connection fails, an event that occurs
     */
    public final static int PEN_CONNECTION_FAILURE		= 0x03;

    /**
     * Pen events that occur when a connection is released
     */
    public final static int PEN_DISCONNECTED			= 0x04;

    /**
     * Pens when the pen authorized, the events that occur
     */
    public final static int PEN_AUTHORIZED			    = 0x05;

    /**
     * Pen events that occur  when pen already connected
     */
    public final static int PEN_ALREADY_CONNECTED			= 0x06;

    /**
     * The firmware version of pen
     */
    public final static int PEN_FW_VERSION			    = 0x10;

    /**
     * The status(battery, memory, ...) of pen
     */
    public final static int PEN_STATUS                  = 0x11;

    /**
     * The constant PEN_SETUP_SUCCESS.
     */
    public final static int PEN_SETUP_SUCCESS     		= 0x12;

    /**
     * The constant PEN_SETUP_FAILURE.
     */
    public final static int PEN_SETUP_FAILURE           = 0x13;

    /**
     * The constant PEN_SETUP_FAILURE_ILLEGAL_PASSWORD_0000.
     */
    public final static int PEN_ILLEGAL_PASSWORD_0000		= 0x26;

    /**
     * The constant PEN_SETUP_AUTO_SHUTDOWN_RESULT.
     */
    public final static int PEN_SETUP_AUTO_SHUTDOWN_RESULT = 0x14;

    /**
     * The constant PEN_SETUP_SENSITIVITY_RESULT.
     */
    public final static int PEN_SETUP_SENSITIVITY_RESULT   = 0x15;

    /**
     * The constant PEN_SETUP_AUTO_POWER_ON_RESULT.
     */
    public final static int PEN_SETUP_AUTO_POWER_ON_RESULT = 0x16;

    /**
     * The constant PEN_SETUP_BEEP_RESULT.
     */
    public final static int PEN_SETUP_BEEP_RESULT          = 0x17;

    /**
     * The constant PEN_SETUP_PEN_COLOR_RESULT.
     */
    public final static int PEN_SETUP_PEN_COLOR_RESULT     = 0x18;

    /**
     * The constant PEN_SETUP_SENSITIVITY_RESULT_FSC.
     */
    public final static int PEN_SETUP_SENSITIVITY_RESULT_FSC   = 0x19;




    /**
     * The constant PEN_USING_NOTE_SET_RESULT.
     */
    public final static int PEN_USING_NOTE_SET_RESULT     = 0x1a;

    /**
     * The constant PEN_SETUP_PEN_CAP_OFF.
     * supported from Protocol 2.0
     */
    public final static int PEN_SETUP_PEN_CAP_OFF = 0x60;
    /**
     * The constant PEN_SETUP_HOVER_ONOFF.
     * supported from Protocol 2.0
     */
    public final static int PEN_SETUP_HOVER_ONOFF = 0x61;
    /**
     * The constant PEN_SETUP_OFFLINEDATA_SAVE_ONOFF.
     * supported from Protocol 2.0
     */
    public final static int PEN_SETUP_OFFLINEDATA_SAVE_ONOFF = 0x62;

    /**
     * The constant EVENT_LOW_BATTERY.
     * supported from Protocol 2.0
     */
    public final static int EVENT_LOW_BATTERY = 0x63;

    /**
     * The constant EVENT_POWER_OFF.
     * supported from Protocol 2.0
     */
    public final static int EVENT_POWER_OFF = 0x64;

    /**
     * Events that occur when you start the pressure-adjusting
     *
     * @deprecated Protocol 2.0 
     */
    public final static int PEN_CALIBRATION_START		= 0x20;

    /**
     * Events that occur when you finish the pressure-adjusting
     *
     * @deprecated Protocol 2.0 
     */
    public final static int PEN_CALIBRATION_FINISH		= 0x21;

    /**
     * Message showing the status of the firmware upgrade pen
     */
    public final static int PEN_FW_UPGRADE_STATUS		= 0x22;

    /**
     * When the firmware upgrade is successful, the pen events that occur
     */
    public final static int PEN_FW_UPGRADE_SUCCESS		= 0x23;

    /**
     * When the firmware upgrade is fails, the pen events that occur
     */
    public final static int PEN_FW_UPGRADE_FAILURE		= 0x24;

    /**
     * When the firmware upgrade is suspended, the pen events that occur
     */
    public final static int PEN_FW_UPGRADE_SUSPEND		= 0x25;

    /**
     * The constant PEN_SETUP_SENSITIVITY_NOT_SUPPORT_DEVICE.
     */
    public final static int PEN_SETUP_SENSITIVITY_NOT_SUPPORT_DEVICE = 0x27;

    /**
     * The constant PEN_SETUP_DISK_RESET_RESULT.
     */
    public final static int PEN_SETUP_DISK_RESET_RESULT   = 0x28;

    public final static int PEN_READ_RSSI   = 0x29;

    /**
     * Pen gesture detection events that occur when
     */
    public final static int PEN_ACTION_GESTURE			= 0x40;

    /**
     * Off-line data stored in the pen's
     */
    public final static int OFFLINE_DATA_NOTE_LIST	    = 0x30;

    /**
     * Off-line data stored in the pen's
     * supported from Protocol 2.0
     */
    public final static int OFFLINE_DATA_PAGE_LIST	    = 0x39;

    /**
     * The constant OFFLINE_DATA_SEND_START.
     */
    public final static int OFFLINE_DATA_SEND_START		= 0x31;

    /**
     * The constant OFFLINE_DATA_SEND_STATUS.
     */
    public final static int OFFLINE_DATA_SEND_STATUS	= 0x32;

    /**
     * The constant OFFLINE_DATA_SEND_SUCCESS.
     */
    public final static int OFFLINE_DATA_SEND_SUCCESS	= 0x33;

    /**
     * The constant OFFLINE_DATA_SEND_FAILURE.
     */
    public final static int OFFLINE_DATA_SEND_FAILURE	= 0x34;


    public final static int OFFLINE_DATA_SEND_ZERO	= 0x37;

    /**
     * The constant OFFLINE_DATA_FILE_CREATED.
     */
    public final static int OFFLINE_DATA_FILE_CREATED	= 0x35;

    /**
     * The constant OFFLINE_DATA_FILE_DELETED.
     */
    public final static int OFFLINE_DATA_FILE_DELETED	= 0x36;

    /**
     * The constant OFFLINE_NOTE_INFO.
     */
    public final static int OFFLINE_NOTE_INFO	= 0x38;

    /**
     * The constant PASSWORD_REQUEST.
     */
    public final static int PASSWORD_REQUEST			= 0x51;

    /**
     * The constant PASSWORD_SETUP_SUCCESS.
     */
    public final static int PASSWORD_SETUP_SUCCESS	    = 0x52;

    /**
     * The constant PASSWORD_SETUP_FAILURE.
     */
    public final static int PASSWORD_SETUP_FAILURE		= 0x53;

    /**
     * Pens when the connection fails cause duplicate BT connection, an event that occurs
     */
    public final static int PEN_CONNECTION_FAILURE_BTDUPLICATE	= 0x54;

    //[2018.03.05] Stroke Test - START
    /**
     * The constant ERROR_MISSING_PEN_UP.
     */
    public final static int ERROR_MISSING_PEN_UP	= 0x70;

    /**
     * The constant ERROR_MISSING_PEN_DOWN.
     */
    public final static int ERROR_MISSING_PEN_DOWN	= 0x71;

    /**
     * The constant ERROR_INVALID_TIME.
     */
    public final static int ERROR_INVALID_TIME	= 0x72;

    /**
     * The constant ERROR_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_MISSING_PEN_DOWN_PEN_MOVE	= 0x73;

    /**
     * The constant ERROR_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_TYPE_IMAGE_PROCESSING_ERROR	= 0x74;

    /**
     * The constant ERROR_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_TYPE_INVALID_EVENT_COUNT	= 0x75;

    /**
     * The constant ERROR_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_TYPE_MISSING_PAGE_CHANGE	= 0x76;

    /**
     * The constant ERROR_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_MISSING_PEN_MOVE	= 0x77;

    //[2018.03.05] Stroke Test - END

    /**
     * The constant PROFILE_FAILURE.
     */
    public final static int PROFILE_FAILURE			= 0x41;

    /**
     * The constant PROFILE_CREATE.
     */
    public final static int PROFILE_CREATE			= 0x42;

    /**
     * The constant PROFILE_DELETE.
     */
    public final static int PROFILE_DELETE			= 0x43;

    /**
     * The constant PROFILE_INFO.
     */
    public final static int PROFILE_INFO			= 0x44;

    /**
     * The constant PROFILE_READ_VALUE.
     */
    public final static int PROFILE_READ_VALUE			= 0x45;

    /**
     * The constant PROFILE_WRITE_VALUE.
     */
    public final static int PROFILE_WRITE_VALUE			= 0x46;

    /**
     * The constant PROFILE_DELETE_VALUE.
     */
    public final static int PROFILE_DELETE_VALUE			= 0x47;

	/**
     * The constant SYSTEM_INFO_FAILURE.
     */
    public final static int SYSTEM_INFO_FAILURE = 0x81;

    /**
     * The constant SYSTEM_INFO_VALUE.
     */
    public final static int SYSTEM_INFO_VALUE = 0x82;

    /**
     * The constant SYSTEM_INFO_PERFORMANCE_STEP.
     */
    public final static int SYSTEM_INFO_PERFORMANCE_STEP = 0x83;
}

