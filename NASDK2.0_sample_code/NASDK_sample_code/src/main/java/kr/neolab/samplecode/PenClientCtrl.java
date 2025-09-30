package kr.neolab.samplecode;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

import kr.neolab.sdk.pen.IPenCtrl;
import kr.neolab.sdk.pen.PenCtrl;
import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.bluetooth.BTLEAdt;
import kr.neolab.sdk.pen.bluetooth.lib.OutOfRangeException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsg;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

/**
 * The type Pen client ctrl.
 */
public class PenClientCtrl implements IPenMsgListener {
    /**
     * The constant myInstance.
     */
    public static PenClientCtrl myInstance;
    public static ArrayList<Object> savedPages = new ArrayList<>();

    public class ReqOfflineData {
        public void run() {
            try {
                pageIds.add((Integer) ((Object[]) savedPages.get(0))[3]);
                iPenCtrl.reqOfflineData(
                        (Integer) ((Object[]) savedPages.get(0))[0],
                        (Integer) ((Object[]) savedPages.get(0))[1],
                        (Integer) ((Object[]) savedPages.get(0))[2],
                        false,
                        Util.convertIntegers(pageIds)
                );
                pageIds.clear();
                savedPages.remove(0);
            } catch (
                    ProtocolNotSupportedException e) {
                e.printStackTrace();
            } catch (
                    OutOfRangeException e) {
                e.printStackTrace();
            }
        }
    }

    public static ArrayList<Integer> pageIds = new ArrayList<>();

    private IPenCtrl iPenCtrl;

    /**
     * The constant USING_SECTION_ID.
     */
    public static int USING_SECTION_ID = 3;
    /**
     * The constant USING_OWNER_ID.
     */
    public static int USING_OWNER_ID = 27;

    /**
     * The constant USING_NOTES.
     */
    public static int[] USING_NOTES = new int[]{301, 302, 303, 28, 50, 101, 102, 103, 201, 202, 203, 600, 601, 602, 603, 605, 606, 607, 608};


    private Context context;

    private SharedPreferences mPref;

    private boolean isConnected = false;
    private boolean isAuthorized = false;

    private String curPass = "0000", newPass = "0000";

    private String mPenFWVersion = null;
    private String mPenSubName = null;
    private String mPenName = null;

    /**
     * Gets current password.
     *
     * @return the current password
     */
    public String getCurrentPassword() {
        return curPass;
    }

    private PenClientCtrl(Context context) {
        this.context = context;

        iPenCtrl = PenCtrl.getInstance();

        // Specify where to store the offline data. (Unless otherwise specified,
        // is stored in the default external storage)
        // inPath = this.getFilesDir().getAbsolutePath();
        // iPenCtrl.setOfflineDataLocation(inPath);


        // regist callback interface
        iPenCtrl.setListener(this);
//		iPenCtrl.setOffLineDataListener( this );
        mPref = PreferenceManager.getDefaultSharedPreferences(context);
    }


    /**
     * Bluetooth Mode Change
     *
     * @param isLeMode true is Bluetooth LE mode, false is Bluetooth(SPP) mode
     * @return true is success, false is failure
     */
    public boolean setLeMode(boolean isLeMode) {
        return iPenCtrl.setLeMode(isLeMode);
    }

    public boolean setAdtMode(PenCtrl.AdtMode adtMode) {
        return iPenCtrl.setAdtMode(adtMode);
    }

    /**
     * Gets instance.
     *
     * @param context the context
     * @return the instance
     */
    public static synchronized PenClientCtrl getInstance(Context context) {
        if (myInstance == null) {
            myInstance = new PenClientCtrl(context);
        }

        return myInstance;
    }

    /**
     * Sets context.
     *
     * @param context the context
     */
    public void setContext(Context context) {
        iPenCtrl.setContext(context);
    }

    /**
     * Register broadcast bt duplicate.
     */
    public void registerBroadcastBTDuplicate() {
        iPenCtrl.registerBroadcastBTDuplicate();
    }

    /**
     * Unregister broadcast bt duplicate.
     */
    public void unregisterBroadcastBTDuplicate() {
        iPenCtrl.unregisterBroadcastBTDuplicate();
    }

    /**
     * Is authorized boolean.
     *
     * @return the boolean
     */
    public boolean isAuthorized() {
        return isAuthorized;
    }

    /**
     * Is connected boolean.
     *
     * @return the boolean
     */
    public boolean isConnected() {
        return isConnected;
    }


    public boolean isSupportPenProfile() {
        return iPenCtrl.isSupportPenProfile();
    }

    /**
     * Connect.
     * If use ble adapter, throws BLENotSupprtedException
     *
     * @param address the address
     */
    public void connect(String address) throws BLENotSupportedException {
        iPenCtrl.setListener(this);
        iPenCtrl.connect(address);
    }

    /**
     * Connect.
     * If use ble adapter, throws BLENotSupprtedException
     *
     * @param sppAddress the spp address
     * @param leAddress  the le address
     */
    public void connect(String sppAddress, String leAddress) {
        iPenCtrl.connect(sppAddress, leAddress);
    }

    /**
     * Connect.
     * If use ble adapter, throws BLENotSupprtedException
     *
     * @param sppAddress the spp address
     * @param leAddress  the le address
     */
    public void connect(String sppAddress, String leAddress, boolean isUuidVer2) {
        BTLEAdt.UUID_VER uuidVer;
        if (isUuidVer2)
            uuidVer = BTLEAdt.UUID_VER.VER_2;
        else
            uuidVer = BTLEAdt.UUID_VER.VER_5;
        iPenCtrl.connect(sppAddress, leAddress, uuidVer, Const.APP_TYPE_FOR_PEN, Const.REQ_PROTOCOL_VER);
    }

    /**
     * Disconnect.
     */
    public void disconnect() {
        iPenCtrl.disconnect();
    }

    /**
     * Upgrade pen.
     *
     * @param fwFile the fw file
     */
    public void upgradePen(File fwFile) {
        try {
            iPenCtrl.upgradePen(fwFile);
        } catch (ProtocolNotSupportedException e) {

        }
    }

    /**
     * Upgrade pen 2.
     *
     * @param fwFile    the fw file
     * @param fwVersion the fw version
     */
    public void upgradePen2(File fwFile, String fwVersion) {
        try {
            iPenCtrl.upgradePen2(fwFile, fwVersion);
        } catch (ProtocolNotSupportedException e) {
        }
    }

    public void upgradePen2(File fwFile, String fwVersion, boolean isCompress) {
        try {
            iPenCtrl.upgradePen2(fwFile, fwVersion, isCompress);
        } catch (ProtocolNotSupportedException e) {
        }
    }


    /**
     * Suspend pen upgrade.
     */
    public void suspendPenUpgrade() {
        iPenCtrl.suspendPenUpgrade();
    }

    /**
     * Input password.
     *
     * @param password the password
     */
    public void inputPassword(String password) {
        curPass = password;
        iPenCtrl.inputPassword(password);
    }

    /**
     * Req setup password.
     *
     * @param oldPassword the old password
     * @param newPassword the new password
     */
    public void reqSetupPassword(String oldPassword, String newPassword) {
        iPenCtrl.reqSetupPassword(oldPassword, newPassword);
    }

    /**
     * Req offline data list.
     */
    public void reqOfflineDataList() {
        iPenCtrl.reqOfflineDataList();
    }

    /**
     * Req offline data list.
     *
     * @param sectionId section ID
     * @param ownerId   owner ID
     */
    public void reqOfflineDataList(int sectionId, int ownerId) throws ProtocolNotSupportedException {
        iPenCtrl.reqOfflineDataList(sectionId, ownerId);
    }

    /**
     * Req offline data page list.
     *
     * @param sectionId section ID
     * @param ownerId   owner ID
     * @param noteId    note ID
     */
    public void reqOfflineDataPageList(int sectionId, int ownerId, int noteId) throws ProtocolNotSupportedException {
        iPenCtrl.reqOfflineDataPageList(sectionId, ownerId, noteId);
    }

    public void removeOfflineData(int sectionId, int ownerId) throws ProtocolNotSupportedException {
        iPenCtrl.removeOfflineData(sectionId, ownerId);
    }

    public void removeOfflineData(int sectionId, int ownerId, int[] noteIds) throws ProtocolNotSupportedException {
        iPenCtrl.removeOfflineData(sectionId, ownerId, noteIds);
    }

    public void removeOfflineDataByPage(int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException {
        iPenCtrl.removeOfflineDataByPage(sectionId, ownerId, noteId, pageIds);
    }


    public void reqOfflineNoteInfo(int sectionId, int ownerId, int noteId) throws ProtocolNotSupportedException {
        iPenCtrl.reqOfflineNoteInfo(sectionId, ownerId, noteId);
    }

    /**
     * Req pen status.
     */
    public void reqPenStatus() {
        iPenCtrl.reqPenStatus();
    }

    /**
     * Req setup auto power on off.
     *
     * @param setOn the set on
     */
    public void reqSetupAutoPowerOnOff(boolean setOn) {
        iPenCtrl.reqSetupAutoPowerOnOff(setOn);
    }

    /**
     * Req setup pen beep on off.
     *
     * @param setOn the set on
     */
    public void reqSetupPenBeepOnOff(boolean setOn) {
        iPenCtrl.reqSetupPenBeepOnOff(setOn);
    }

    /**
     * Req setup pen tip color.
     *
     * @param color the color
     */
    public void reqSetupPenTipColor(int color) {
        iPenCtrl.reqSetupPenTipColor(color);
    }

    /**
     * Req setup auto shutdown time.
     *
     * @param minute the minute
     */
    public void reqSetupAutoShutdownTime(short minute) {
        iPenCtrl.reqSetupAutoShutdownTime(minute);
    }

    /**
     * Req setup pen sensitivity.
     *
     * @param level the level
     */
    public void reqSetupPenSensitivity(short level) {
        iPenCtrl.reqSetupPenSensitivity(level);
    }

    /**
     * Req setup pen cap on off.
     *
     * @param on the on
     */
    public void reqSetupPenCapOnOff(boolean on) {
        try {
            iPenCtrl.reqSetupPenCapOff(on);
        } catch (ProtocolNotSupportedException e) {
        }
    }

    /**
     * Req setup pen hover.
     *
     * @param on the on
     */
    public void reqSetupPenHover(boolean on) {
        try {
            iPenCtrl.reqSetupPenHover(on);
        } catch (ProtocolNotSupportedException e) {
        }
    }

    /**
     * Sets allow offline data.
     *
     * @param on the on
     */
    public void setAllowOfflineData(boolean on) {
        iPenCtrl.setAllowOfflineData(on);
    }

    @Override
    public void onReceiveMessage(String macAddress, PenMsg penMsg) {
        NLog.d("PenClientCtrl onReceiveMessage penMsg=" + penMsg.getPenMsgType() + ",getContent:" + penMsg.getContent());
        switch (penMsg.penMsgType) {
            // Pens when the connection is complete (that is still going through the certification process state)
            case PenMsgType.PEN_CONNECTION_SUCCESS:

                NLog.d("PenClientCtrl PEN_CONNECTION_SUCCESS getConnectingDevice" + getConnectingDevice());
                isConnected = true;

                break;

            // Fired when ready to use pen
            case PenMsgType.PEN_AUTHORIZED:
                NLog.d("PenClientCtrl PEN_AUTHORIZED getConnectDevice" + getConnectDevice());
                isAuthorized = true;

                JSONObject obj = penMsg.getContentByJSONObject();
                mPref = PreferenceManager.getDefaultSharedPreferences(context);

                SharedPreferences.Editor edit = mPref.edit();

                try {
                    curPass = obj.getString(JsonTag.STRING_PEN_PASSWORD);
                    String macaddress = obj.getString(JsonTag.STRING_PEN_MAC_ADDRESS);
                    edit.putString(Const.Setting.KEY_PASSWORD, getCurrentPassword());
                    edit.putString(Const.Setting.KEY_MAC_ADDRESS, macaddress);
                    edit.commit();
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                // notify using note
//				iPenCtrl.reqAddUsingNote( USING_SECTION_ID, USING_OWNER_ID, USING_NOTES );
//				iPenCtrl.reqAddUsingNote( USING_SECTION_ID, USING_OWNER_ID );
                iPenCtrl.reqAddUsingNoteAll();

                // to request offline data list
                // 오프라인 데이터 전체 요청
//				iPenCtrl.reqOfflineDataList();
                try {
                    iPenCtrl.reqSetupPenHover(true);
                } catch (ProtocolNotSupportedException e) {
                    e.printStackTrace();
//                    throw new RuntimeException(e);
                }

                // 오프라인 데이터 노트단위 요청
//				iPenCtrl.reqOfflineData( USING_SECTION_ID,USING_OWNER_ID,301 );

                // 오프라인 데이터 페이지 단위 요청
//				int[] pageIds = {0,1,5};
//				try
//				{
//					iPenCtrl.reqOfflineData(USING_SECTION_ID,USING_OWNER_ID,301, pageIds );
//				} catch ( ProtocolNotSupportedException e )
//				{
//					e.printStackTrace();
//				}

                break;

            case PenMsgType.PEN_DISCONNECTED:

                isConnected = false;
                isAuthorized = false;

                break;

            case PenMsgType.PASSWORD_REQUEST: {
                JSONObject job = penMsg.getContentByJSONObject();

                try {
                    int count = job.getInt(Const.JsonTag.INT_PASSWORD_RETRY_COUNT);

                    NLog.d("password count : " + count);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            break;

            // Pen a response to the status request (whenever a message comes in should be reflected in the app)
            case PenMsgType.PEN_STATUS: {
                JSONObject job = penMsg.getContentByJSONObject();

                if (job == null) {
                    return;
                }

                NLog.d(job.toString());

                mPref = PreferenceManager.getDefaultSharedPreferences(context);

                SharedPreferences.Editor editor = mPref.edit();

                try {
                    String stat_version = job.getString(Const.JsonTag.STRING_PROTOCOL_VERSION);

//					int stat_timezone = job.getInt( Const.JsonTag.INT_TIMEZONE_OFFSET );
                    long stat_timetick = job.getLong(Const.JsonTag.LONG_TIMETICK);
                    int stat_forcemax = job.getInt(Const.JsonTag.INT_MAX_FORCE);
                    int stat_battery = job.getInt(Const.JsonTag.INT_BATTERY_STATUS);
                    int stat_usedmem = job.getInt(Const.JsonTag.INT_MEMORY_STATUS);

//					int stat_pencolor = job.getInt( Const.JsonTag.INT_PEN_COLOR );

                    boolean stat_autopower = job.getBoolean(Const.JsonTag.BOOL_AUTO_POWER_ON);
                    boolean pencap_on = false;
                    try {
                        pencap_on = job.getBoolean(Const.JsonTag.BOOL_PEN_CAP_ON);
                    } catch (Exception e) {

                    }
//					boolean stat_accel = job.getBoolean( Const.JsonTag.BOOL_ACCELERATION_SENSOR );
                    boolean stat_hovermode = false;
                    try {
                        stat_hovermode = job.getBoolean(Const.JsonTag.BOOL_HOVER);
                    } catch (Exception e) {
                    }
                    boolean stat_offlinesave = false;
                    try {
                        stat_offlinesave = job.getBoolean(Const.JsonTag.BOOL_OFFLINE_DATA_SAVE);
                    } catch (Exception e) {
                    }


                    boolean stat_beep = job.getBoolean(Const.JsonTag.BOOL_BEEP);

                    int stat_autopower_time = job.getInt(Const.JsonTag.INT_AUTO_POWER_OFF_TIME);
                    int stat_sensitivity = job.getInt(Const.JsonTag.INT_PEN_SENSITIVITY);

//					editor.putBoolean( Const.Setting.KEY_ACCELERATION_SENSOR, stat_accel );
                    editor.putString(Const.Setting.KEY_AUTO_POWER_OFF_TIME, "" + stat_autopower_time);
                    editor.putBoolean(Const.Setting.KEY_AUTO_POWER_ON, stat_autopower);
                    editor.putBoolean(Const.Setting.KEY_BEEP, stat_beep);
//					editor.putString( Const.Setting.KEY_PEN_COLOR, ""+stat_pencolor );
                    editor.putString(Const.Setting.KEY_SENSITIVITY, "" + stat_sensitivity);
                    editor.putBoolean(Const.Setting.KEY_HOVER_MODE, stat_hovermode);
                    editor.putBoolean(Const.Setting.KEY_PEN_CAP_ON, pencap_on);
                    editor.putBoolean(Const.Setting.KEY_OFFLINE_DATA_SAVE, stat_offlinesave);

                    editor.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;

            case PenMsgType.PEN_FW_VERSION: {
                JSONObject job = penMsg.getContentByJSONObject();

                try {
                    mPenFWVersion = job.getString(JsonTag.STRING_PEN_FW_VERSION);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    mPenSubName = job.getString(JsonTag.STRING_SUB_NAME);
                } catch (JSONException e) {
                    mPenSubName = null;
                    e.printStackTrace();
                }

                try {
                    mPenName = job.getString(JsonTag.STRING_DEVICE_NAME);
                } catch (JSONException e) {
                    mPenName = null;
                    e.printStackTrace();
                }

                NLog.d("PEN_FW_VERSION=" + job.toString());
            }
            break;


            // Pen password change success response
            case PenMsgType.PASSWORD_SETUP_SUCCESS: {
                if (curPass != newPass) {
                    curPass = newPass;
                }
            }
            break;

            // Pen password change fails, the response
            case PenMsgType.PASSWORD_SETUP_FAILURE: {
                if (curPass != newPass) {
                    newPass = curPass;
                }
            }
            break;

            case PenMsgType.PEN_ILLEGAL_PASSWORD_0000: {
                Util.showToast(context, "PassWord do not allow 0000 !!!!");
            }
            break;


            case PenMsgType.OFFLINE_DATA_NOTE_LIST:

                try {
                    JSONArray list = new JSONArray(penMsg.getContent());

                    for (int i = 0; i < list.length(); i++) {
                        JSONObject jobj = list.getJSONObject(i);

                        int sectionId = jobj.getInt(Const.JsonTag.INT_SECTION_ID);
                        int ownerId = jobj.getInt(Const.JsonTag.INT_OWNER_ID);
                        int noteId = jobj.getInt(Const.JsonTag.INT_NOTE_ID);
                        NLog.d("offline(" + (i + 1) + ") note => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId);

//						// 오프라인 데이터 리스트 노트북 단위로 받기
//						iPenCtrl.reqOfflineData(sectionId,  ownerId, noteId );
                        // 오프라인 데이터 리스트 노트북 단위로 받기
                        try {
                            iPenCtrl.reqOfflineDataPageList(sectionId, ownerId, noteId);
                            break;
                        } catch (ProtocolNotSupportedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case PenMsgType.OFFLINE_DATA_PAGE_LIST:

                try {
                    ReqOfflineData reqOfflineData = new ReqOfflineData();
                    JSONArray list = new JSONArray(penMsg.getContent());
                    int prvSec = -1;
                    int prvOwn = -1;
                    int prvNote = -1;
                    if (savedPages.size() != list.length()) {
                        for (int i = 0; i < list.length(); i++) {

                            JSONObject jobj = list.getJSONObject(i);

                            int sectionId = jobj.getInt(Const.JsonTag.INT_SECTION_ID);
                            int ownerId = jobj.getInt(Const.JsonTag.INT_OWNER_ID);
                            int noteId = jobj.getInt(Const.JsonTag.INT_NOTE_ID);
                            int pageId = jobj.getInt(Const.JsonTag.INT_PAGE_ID);
                            Object[] pageInfo = new Object[]{sectionId, ownerId, noteId, pageId};
                            NLog.d("offline(" + (i + 1) + ") note => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId + ", pageId : " + pageId);

                            savedPages.add(pageInfo);
                        }
                    }
                    // 오프라인 데이터 리스트 페이지 단위로 받기
                    // deleteOnFinished 를 false 로 설정하였다면, 오프라인데이터를 받은 후 직접 삭제해주어야 한다.
                    reqOfflineData.run();

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                break;

            case PenMsgType.OFFLINE_DATA_SEND_START:

                break;
        }

        sendPenMsgByBroadcast(macAddress, penMsg);
    }

    public void sendPenMsgByBroadcast(String macAddress, PenMsg penMsg) {
        Intent i = new Intent(Const.Broadcast.ACTION_PEN_MESSAGE);
        i.putExtra(Const.Broadcast.PEN_ADDRESS, macAddress);
        i.putExtra(Const.Broadcast.MESSAGE_TYPE, penMsg.getPenMsgType());
        i.putExtra(Const.Broadcast.CONTENT, penMsg.getContent());

        context.sendBroadcast(i);
    }

//	private void sendPenDotByBroadcast( Dot dot )
//	{
//		Intent i = new Intent( Const.Broadcast.ACTION_PEN_DOT );
//		i.putExtra( Const.Broadcast.EXTRA_DOT, dot );
//
////		i.putExtra( Const.Broadcast.SECTION_ID, dot.sectionId );
////		i.putExtra( Const.Broadcast.OWNER_ID, dot.ownerId );
////		i.putExtra( Const.Broadcast.NOTE_ID, dot.noteId );
////		i.putExtra( Const.Broadcast.PAGE_ID, dot.pageId );
////		i.putExtra( Const.Broadcast.X, dot.x );
////		i.putExtra( Const.Broadcast.Y, dot.y );
////		i.putExtra( Const.Broadcast.FX, dot.fx );
////		i.putExtra( Const.Broadcast.FY, dot.fy );
////		i.putExtra( Const.Broadcast.PRESSURE, dot.force );
////		i.putExtra( Const.Broadcast.TIMESTAMP, dot.timestamp );
////		i.putExtra( Const.Broadcast.TYPE, dot.dotType );
////		i.putExtra( Const.Broadcast.COLOR, dot.color );
//
//		context.sendBroadcast( i );
//	}

    /**
     * Get sdk verions string.
     *
     * @return the string
     */
    public String getSDKVerions() {
        return this.iPenCtrl.getVersion();
    }

    /**
     * Gets protocol version.
     *
     * @return the protocol version
     */
    public int getProtocolVersion() {
        return this.iPenCtrl.getProtocolVersion();
    }

    public String getConnectDevice() {
        return this.iPenCtrl.getConnectedDevice();
    }

    public IPenCtrl getIPenCtrl() {
        return this.iPenCtrl;
    }

    public String getConnectingDevice() {
        return this.iPenCtrl.getConnectingDevice();
    }

    /**
     * Gets device name.
     * Notice!! Not Support Protocol 1.0
     * Protocol 1.0 return null;
     *
     * @return the device name
     */
    public String getDeviceName() {
        return mPenName;
    }


    public String getSubName() {
        return mPenSubName;
    }

    public String getPenFWVersion() {
        return mPenFWVersion;
    }


    /**
     * Confirm whether or not the MAC address to connect
     * If use ble adapter, throws BLENotSupprtedException
     *
     * @param mac the mac
     * @return true if can use, otherwise false
     */
    public boolean isAvailableDevice(String mac) throws BLENotSupportedException {
        return iPenCtrl.isAvailableDevice(mac);
    }

    /**
     * Confirm whether or not the MAC address to connect
     * NOTICE
     * SPP must use mac address bytes
     * BLE must use advertising full data ( ScanResult.getScanRecord().getBytes() )
     *
     * @param mac the mac
     * @return true if can use, otherwise false
     */
    public boolean isAvailableDevice(byte[] mac) {
        return iPenCtrl.isAvailableDevice(mac);
    }

    public boolean unpairDevice(String address) {
        return iPenCtrl.unpairDevice(address);
    }


}
