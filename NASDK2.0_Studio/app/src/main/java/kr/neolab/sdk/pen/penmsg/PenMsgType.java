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
	 * @deprecated Protocol 2.0 에서 삭제
	 */
	public final static int PEN_CALIBRATION_START		= 0x20;

	/**
	 * Events that occur when you finish the pressure-adjusting
	 *
	 * @deprecated Protocol 2.0 에서 삭제
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
	public final static int OFFLINE_DATA_PAGE_LIST	    = 0x63;

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

	/**
	 * The constant OFFLINE_DATA_FILE_CREATED.
	 */
	public final static int OFFLINE_DATA_FILE_CREATED	= 0x35;

	/**
	 * The constant OFFLINE_DATA_FILE_DELETED.
	 */
	public final static int OFFLINE_DATA_FILE_DELETED	= 0x36;

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

}

