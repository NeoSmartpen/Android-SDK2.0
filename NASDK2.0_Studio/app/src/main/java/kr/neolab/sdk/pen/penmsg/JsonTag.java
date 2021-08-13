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
     * The constant STRING_PACKAGE_NAME.
     */
    public final static String STRING_PACKAGE_NAME = "packageName";

    /**
     * The constant STRING_PEN_MAC_ADDRESS.
     */
    public final static String STRING_PEN_MAC_ADDRESS="pen_mac_address";

    /**
     * The constant STRING_PEN_PASSWORD.
     */
    public final static String STRING_PEN_PASSWORD="pen_password";

    /**
     * The constant INT_PRESS_SENSOR_TYPE.
     */
    public final static String INT_PRESS_SENSOR_TYPE = "press_sensor_type";

    /**
     * The constant BOOL_REGULAR_DISCONNECT.
     */
    public final static String BOOL_REGULAR_DISCONNECT = "regular";

    /**
     * The constant INT_WIREDPEN_DISCONNECT_REASON.
     */
    public final static String INT_WIREDPEN_DISCONNECT_REASON = "wiredpen_disconnect_reason";

    /**
	 * The constant BOOL_WRITE_FAILED_DISCONNECT.
	 */
	public final static String BOOL_SEND_DATA_FAILED_DISCONNECT = "send_data_failed";

    /**
     * The constant INT_POWER_OFF_REASON.
     * 0: auto power_off 1: low battery 2:update 3: power key
     */
    public final static String INT_POWER_OFF_REASON = "poweroff_reason";

    /**
     * The constant STRING_STATUS.
     *
     * @deprecated from Protocol 2.0
     */
    public final static String STRING_STATUS = "status";
    /**
     * The constant INT_TIMEZONE_OFFSET.
     *
     * @deprecated from Protocol 2.0
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
     * Added to Protocol 2.0
     * Edited to Protocol 2.15
     * NAP400 based model - pencap off
     * MT2523 based model(NWP-F51) - pencap on/off
     */
    public final static String BOOL_PENCAP_OFF = "pencap_onoff";
    /**
     * The constant INT_PEN_COLOR.
     *
     * @deprecated from Protocol 2.0
     */
    public final static String INT_PEN_COLOR = "pen_tip_color";
    /**
     * The constant BOOL_AUTO_POWER_ON.
     */
    public final static String BOOL_AUTO_POWER_ON = "auto_power_onoff";
    /**
     * The constant BOOL_ACCELERATION_SENSOR.
     *
     * @deprecated from Protocol 2.0
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
     * Added to Protocol 2.0
     */
    public final static String BOOL_OFFLINEDATA_SAVE = "offlinedata_save";

    /**
     * Added to Protocol 2.16
     */
    public final static String BOOL_OFFLINE_INFO_INVALID_PAGE = "offline_invalid_page";

    /**
     * Added to Protocol 2.16
     */
    public final static String STRING_OFFLINE_INFO_PAGE_LIST = "offline_page_list";

    /**
     * Added to Protocol 2.16
     */
    public final static String INT_NOTE_VERSION = "note_version";

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
     * The constant OFFLINE_EXTRA.
     */
    public final static String OFFLINE_EXTRA = "offline_extra";

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

    /**
     * The constant STRING_PROFILE_NAME.
     */
    public final static String STRING_PROFILE_NAME = "profile_name";

    /**
     * The constant STRING_PROFILE_KEY.
     */
    public final static String STRING_PROFILE_KEY = "profile_key";

    /**
     * The constant BYTE_PROFILE_VALUE.
     */
    public final static String BYTE_PROFILE_VALUE = "profile_value";

    /**
     * The constant INT_PROFILE_RES_STATUS.
     */
    public final static String INT_PROFILE_RES_STATUS = "profile_res_status";

    /**
     * The constant ARRAY_PROFILE_RES.
     */
    public final static String ARRAY_PROFILE_RES = "profile_res_array";

//	public final static String BYTE_PROFILE_S = "profile_res_status";

    /**
     * The constant INT_PROFILE_INFO_TOTAL_SECTOR_COUNT.
     */
    public final static String INT_PROFILE_INFO_TOTAL_SECTOR_COUNT = "profile_total_sector";

    /**
     * The constant INT_PROFILE_INFO_SECTOR_SIZE.
     */
    public final static String INT_PROFILE_INFO_SECTOR_SIZE = "profile_sector_size";

    /**
     * The constant INT_PROFILE_INFO_USE_SECTOR_COUNT.
     */
    public final static String INT_PROFILE_INFO_USE_SECTOR_COUNT = "profile_use_sector";

    /**
     * The constant INT_PROFILE_INFO_USE_KEY_COUNT.
     */
    public final static String INT_PROFILE_INFO_USE_KEY_COUNT = "profile_use_key_count";

    //[2018.03.05] Stroke Test - START
    /**
     * The constant LONG_LOG_NUMBER.
     */
    public final static String LONG_LOG_NUMBER = "log_number";

    /**
     * The constant LONG_LOG_TIMESTAMP.
     */
    public final static String LONG_LOG_TIMESTAMP = "log_timestamp";

    /**
     * The constant LONG_LOG_PEN_DOWN_TIMESTAMP
     */
    public final static String LONG_LOG_PEN_DOWN_TIMESTAMP = "log_pen_down_timestamp";

    /**
     * The constant LONG_LOG_ERROR_TYPE.
     */
    public final static String INT_LOG_ERROR_TYPE = "log_error_type";

    /**
     * The constant INT_LOG_ERROR_EVENT_COUNT.
     */
    public final static String INT_LOG_ERROR_EVENT_COUNT = "log_error_event_count";

    /**
     * The constant INT_LOG_ERROR_PREV_EVENT_COUNT.
     */
    public final static String INT_LOG_ERROR_PREV_EVENT_COUNT = "log_error_prev_event_count";

    /**
     * The constant INT_LOG_SECTION.
     */
    public final static String INT_LOG_SECTION = "log_section";

    /**
     * The constant INT_LOG_OWNER.
     */
    public final static String INT_LOG_OWNER = "log_owner";

    /**
     * The constant INT_LOG_book.
     */
    public final static String INT_LOG_BOOK = "log_book";

    /**
     * The constant INT_LOG_PAGE.
     */
    public final static String INT_LOG_PAGE = "log_page";

    /**
     * The constant INT_LOG_X.
     */
    public final static String INT_LOG_X = "log_x";

    /**
     * The constant INT_LOG_FX.
     */
    public final static String INT_LOG_FX = "log_fx";

    /**
     * The constant INT_LOG_Y.
     */
    public final static String INT_LOG_Y = "log_y";

    /**
     * The constant INT_LOG_FY.
     */
    public final static String INT_LOG_FY = "log_fy";

    /**
     * The constant FLOAT_LOG_FX.
     */
    public final static String FLOAT_LOG_FX = "log_fx";

    /**
     * The constant FLOAT_LOG_FY.
     */
    public final static String FLOAT_LOG_Y = "log_fy";

    /**
     * The constant INT_LOG_FORCE.
     */
    public final static String INT_LOG_FORCE = "log_force";

    /**
     * The constant INT_LOG_NDAC.
     */
    public final static String INT_LOG_NDAC = "log_ndac";

    /**
     * The constant INT_LOG_IMAGE_BRIGHTNESS.
     */
    public final static String INT_LOG_IMAGE_BRIGHTNESS = "log_image_brightness";

    /**
     * The constant INT_LOG_EXPOSURE_TIME.
     */
    public final static String INT_LOG_EXPOSURE_TIME = "log_exposure_time";

    /**
     * The constant INT_LOG_LABEL_COUNT.
     */
    public final static String INT_LOG_LABEL_COUNT = "log_label_count";

    /**
     * The constant INT_LOG_NDAC_ERROR_CODE.
     */
    public final static String INT_LOG_NDAC_ERROR_CODE = "log_ndac_error_code";

    /**
     * The constant INT_LOG_NDAC_ERROR_COUNT.
     */
    public final static String INT_LOG_NDAC_ERROR_COUNT = "log_ndac_error_count";

    /**
     * The constant INT_LOG_CLASS_TYPE.
     */
    public final static String INT_LOG_CLASS_TYPE = "log_class_type";

    /**
     * The constant INT_LOG_EXTRA_DATA.
     */
    public final static String INT_LOG_EXTRA_DATA = "log_extra_data";


    /**
     * The constant BOOL_SUPPORT_SYSTEM_SETTING
     */
    public final static String BOOL_SUPPORT_SYSTEM_SETTING = "set_system_setting";

    /**
     * The constant INT_PERFORMANCE_STEP
     *
     */
    public final static String INT_PERFORMANCE_STEP = "pen_performance_step";

    /**
     * The constant PERFORMANCE_STEP_INVALID
     */
    public final static String INT_SET_PERFORMANCE_STATUS = "set_performance_status";

    /**
     * The constant PERFORMANCE_STEP_NOT_SUPPORT
     */
    public final static int PERFORMANCE_STEP_NOT_SUPPORT = -1;

    /**
     * The constant PERFORMANCE_STEP_0
     * In case of MT2523, Max 104 MHz, 43 frame
     */
    public final static int PERFORMANCE_STEP_0 = 0;

    /**
     * The constant PERFORMANCE_STEP_0
     * In case of MT2523, Max 208 MHz, 86 frame
     */
    public final static int PERFORMANCE_STEP_1 = 1;


    /**
     * The constant ERROR_TYPE_MISSING_PEN_UP.
     */
    public final static int ERROR_TYPE_MISSING_PEN_UP	= 1;

    /**
     * The constant ERROR_TYPE_MISSING_PEN_DOWN.
     */
    public final static int ERROR_TYPE_MISSING_PEN_DOWN	= 2;

    /**
     * The constant ERROR_TYPE_INVALID_TIME.
     */
    public final static int ERROR_TYPE_INVALID_TIME	= 3;

    /**
     * The constant ERROR_TYPE_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_TYPE_MISSING_PEN_DOWN_PEN_MOVE	= 4;

    /**
     * The constant ERROR_TYPE_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_TYPE_IMAGE_PROCESSING_ERROR	= 5;

    /**
     * The constant ERROR_TYPE_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_TYPE_INVALID_EVENT_COUNT	= 6;

    /**
     * The constant ERROR_TYPE_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_TYPE_MISSING_PAGE_CHANGE	= 7;

    /**
     * The constant ERROR_TYPE_MISSING_PEN_DOWN_PEN_MOVE.
     */
    public final static int ERROR_TYPE_MISSING_PEN_MOVE	= 8;

    //[2018.03.05] Stroke Test - END

    /**
     * The constant INT_RSSI.
     */
    public final static String INT_RSSI = "rssi";


}
