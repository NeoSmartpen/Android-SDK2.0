package kr.neolab.samplecode;

import android.os.Environment;

import java.io.File;
import java.util.UUID;

public class Const
{
	public static String SAMPLE_FOLDER_PATH = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS ).getPath()+ File.separator+"neolab_data";

	public static String REQ_PROTOCOL_VER = "2.18";
	public static short APP_TYPE_FOR_PEN = 0x1201;

	public static final UUID ServiceUuidV2 = UUID.fromString("000019F1-0000-1000-8000-00805F9B34FB");
	public static final UUID ServiceUuidV5 = UUID.fromString("4f99f138-9d53-5bfa-9e50-b147491afe68");


	public class Setting 
	{
		public final static String KEY_PASSWORD = "password";
		public final static String KEY_PEN_COLOR = "pen_color";
		public final static String KEY_AUTO_POWER_ON = "auto_power_onoff";
		public final static String KEY_BEEP = "beep_onoff";
		public final static String KEY_AUTO_POWER_OFF_TIME = "auto_power_off_time";
		public final static String KEY_SENSITIVITY = "sensitivity";
		public final static String KEY_OFFLINE_DATA_SAVE = "offlinedata_save";
		public final static String KEY_HOVER_MODE = "hover_onoff";
		public final static String KEY_PEN_CAP_ON = "pencap_onoff";

		public final static String KEY_MAC_ADDRESS = "mac_address";



	}
	
	public class JsonTag
	{
		public final static String STRING_PROTOCOL_VERSION = "protocol_version";
		public final static String INT_TIMEZONE_OFFSET = "timezone";
		public final static String LONG_TIMETICK = "timetick";
		public final static String INT_MAX_FORCE = "force_max";
		public final static String INT_BATTERY_STATUS = "battery";
		public final static String INT_MEMORY_STATUS = "used_memory";
		public final static String INT_PEN_COLOR = "pen_tip_color";
		public final static String BOOL_AUTO_POWER_ON = "auto_power_onoff";
		public final static String BOOL_PEN_CAP_ON = "pencap_onoff";

		public final static String BOOL_ACCELERATION_SENSOR = "acceleration_sensor_onoff";
		public final static String BOOL_HOVER = "hover_mode";
		public final static String BOOL_OFFLINE_DATA_SAVE = "offlinedata_save";
		public final static String BOOL_BEEP = "beep";
		public final static String INT_AUTO_POWER_OFF_TIME = "auto_power_off_time";
		public final static String INT_PEN_SENSITIVITY = "sensitivity";
		
		public final static String INT_TOTAL_SIZE = "total_size";
		public final static String INT_SENT_SIZE = "sent_size";
		public final static String INT_RECEIVED_SIZE = "received_size";
		
		public final static String INT_SECTION_ID = "section_id";
		public final static String INT_OWNER_ID = "owner_id";
		public final static String INT_NOTE_ID = "note_id";
		public final static String INT_PAGE_ID = "page_id";
		public final static String STRING_FILE_PATH = "file_path";
		
		public final static String INT_PASSWORD_RETRY_COUNT = "retry_count";
		public final static String INT_PASSWORD_RESET_COUNT = "reset_count";
		
		public final static String BOOL_RESULT = "result";
	}
	
	public class Broadcast
	{
		public static final String PEN_ADDRESS = "pen_address";

		public static final String ACTION_PEN_MESSAGE = "action_pen_message";
		public static final String MESSAGE_TYPE = "message_type";
		public static final String CONTENT = "content";


		public static final String ACTION_SYMBOL_ACTION = "symbol_action";
		public static final String ACTION_WRITE_PAGE_CHANGED = "write_page_changed";
		public static final String EXTRA_SECTION_ID = "sectionId";
		public static final String EXTRA_OWNER_ID = "ownerId";
		public static final String EXTRA_BOOKCODE_ID = "bookcodeId";
		public static final String EXTRA_PAGE_NUMBER = "page_number";
		public static final String EXTRA_SYMBOL_ID = "symbolId";

		public static final String ACTION_PEN_DOT = "action_pen_dot";
		public static final String ACTION_OFFLINE_STROKES = "action_offline_strokes";
		public static final String EXTRA_DOT = "dot";
		public static final String EXTRA_OFFLINE_STROKES = "offline_strokes";
//		public static final String SECTION_ID = "sectionId";
//		public static final String OWNER_ID = "ownerId";
//		public static final String NOTE_ID = "noteId";
//		public static final String PAGE_ID = "pageId";
//		public static final String X = "x";
//		public static final String Y = "y";
//		public static final String FX = "fx";
//		public static final String FY = "fy";
//		public static final String PRESSURE = "pressure";
//		public static final String TIMESTAMP = "timestamp";
//		public static final String TYPE = "type";
//		public static final String COLOR = "color";
		
		public static final String ACTION_PEN_UPDATE = "action_firmware_update";
	}

    public static final byte[] NEOLAB_PROFILE_PASS = { (byte)0x6B, (byte)0xCA, (byte)0x6B, (byte)0x50, (byte)0x5D, (byte)0xEC, (byte)0xA7, (byte)0x8C };
}
