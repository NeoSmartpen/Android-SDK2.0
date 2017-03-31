package kr.neolab.sdk.pen.penmsg;

/**
 * The type Json tag.
 */
public class JsonTag
{
	/**
	 * The constant STRING_PEN_FW_VERSION.
	 */
	public final static String STRING_PEN_FW_VERSION="pen_fw_version";
	/**
	 * The constant STRING_PROTOCOL_VERSION.
	 */
	public final static String STRING_PROTOCOL_VERSION = "protocol_version";
	/**
	 * The constant STRING_DEVICE_NAME.
	 */
	public final static String STRING_DEVICE_NAME = "device_name";
	/**
	 * The constant STRING_SUB_NAME.
	 */
	public final static String STRING_SUB_NAME = "sub_name";

	/**
	 * The constant BOOL_REGULAR_DISCONNECT.
	 */
// 브라질 펜 인증이슈로 추가함
	public final static String BOOL_REGULAR_DISCONNECT = "regular";

	/**
	 * The constant INT_POWER_OFF_REASON.
	 * 0: auto power_off 1: low battery 2:update 3: power key
	 */
	public final static String INT_POWER_OFF_REASON = "poweroff_reason";

	/**
	 * The constant STRING_STATUS.
	 *
	 * @deprecated Protocol 2.0 에서 삭제
	 */
	public final static String STRING_STATUS = "status";
	/**
	 * The constant INT_TIMEZONE_OFFSET.
	 *
	 * @deprecated Protocol 2.0 에서 삭제
	 */
	public final static String INT_TIMEZONE_OFFSET = "timezone";
	/**
	 * The constant LONG_TIMETICK.
	 */
	public final static String LONG_TIMETICK = "timetick";
	/**
	 * The constant INT_MAX_FORCE.
	 */
	public final static String INT_MAX_FORCE = "force_max";
	/**
	 * The constant INT_BATTERY_STATUS.
	 */
	public final static String INT_BATTERY_STATUS = "battery";
	/**
	 * The constant INT_MEMORY_STATUS.
	 */
	public final static String INT_MEMORY_STATUS = "used_memory";
	/**
	 * Protocol 2.0 에 추가
	 */
	public final static String BOOL_PENCAP_OFF = "pencap_onoff";
	/**
	 * The constant INT_PEN_COLOR.
	 *
	 * @deprecated Protocol 2.0 에서 삭제
	 */
	public final static String INT_PEN_COLOR = "pen_tip_color";
	/**
	 * The constant BOOL_AUTO_POWER_ON.
	 */
	public final static String BOOL_AUTO_POWER_ON = "auto_power_onoff";
	/**
	 * The constant BOOL_ACCELERATION_SENSOR.
	 *
	 * @deprecated Protocol 2.0 에서 삭제
	 */
	public final static String BOOL_ACCELERATION_SENSOR = "acceleration_sensor_onoff";
	/**
	 * The constant BOOL_HOVER.
	 */
	public final static String BOOL_HOVER = "hover_mode";
	/**
	 * The constant BOOL_BEEP.
	 */
	public final static String BOOL_BEEP = "beep";
	/**
	 * The constant INT_AUTO_POWER_OFF_TIME.
	 */
	public final static String INT_AUTO_POWER_OFF_TIME = "auto_power_off_time";

	/**
	 * Protocol 2.0 에 추가
	 */
	public final static String BOOL_OFFLINEDATA_SAVE = "offlinedata_save";
	/**
	 * The constant INT_PEN_SENSITIVITY.
	 */
	public final static String INT_PEN_SENSITIVITY = "sensitivity";
	/**
	 * The constant INT_TOTAL_SIZE.
	 */
	public final static String INT_TOTAL_SIZE = "total_size";
	/**
	 * The constant INT_SENT_SIZE.
	 */
	public final static String INT_SENT_SIZE = "sent_size";
	/**
	 * The constant INT_RECEIVED_SIZE.
	 */
	public final static String INT_RECEIVED_SIZE = "received_size";

	/**
	 * The constant INT_SECTION_ID.
	 */
	public final static String INT_SECTION_ID = "section_id";
	/**
	 * The constant INT_OWNER_ID.
	 */
	public final static String INT_OWNER_ID = "owner_id";
	/**
	 * The constant INT_NOTE_ID.
	 */
	public final static String INT_NOTE_ID = "note_id";
	/**
	 * The constant INT_PAGE_ID.
	 */
	public final static String INT_PAGE_ID = "page_id";
	/**
	 * The constant STRING_FILE_PATH.
	 */
	public final static String STRING_FILE_PATH = "file_path";

	/**
	 * The constant INT_PASSWORD_RETRY_COUNT.
	 */
	public final static String INT_PASSWORD_RETRY_COUNT = "retry_count";
	/**
	 * The constant INT_PASSWORD_RESET_COUNT.
	 */
	public final static String INT_PASSWORD_RESET_COUNT = "reset_count";

	/**
	 * The constant BOOL_RESULT.
	 */
	public final static String BOOL_RESULT = "result";

	// PDIS 관련
	public final static String LONG_PIDS_ID="pids_id";
	public final static String LONG_PIDS_TIMESTAMP="pids_timestamp";
}
