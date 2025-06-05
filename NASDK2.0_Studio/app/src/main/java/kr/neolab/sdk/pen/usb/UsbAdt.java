package kr.neolab.sdk.pen.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.json.JSONObject;

import java.io.File;
import java.io.PipedInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import kr.neolab.sdk.metadata.IMetadataListener;
import kr.neolab.sdk.pen.IPenAdt;
import kr.neolab.sdk.pen.bluetooth.IConnectedThread;
import kr.neolab.sdk.pen.bluetooth.cmd.CommandManager;
import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor;
import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor20;
import kr.neolab.sdk.pen.bluetooth.lib.OutOfRangeException;
import kr.neolab.sdk.pen.bluetooth.lib.ProfileKeyValueLimitException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser20;
import kr.neolab.sdk.pen.filter.Fdot;
import kr.neolab.sdk.pen.offline.OfflineByteData;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsg;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;
import kr.neolab.sdk.util.UseNoteData;

import static android.content.Context.USB_SERVICE;
import static kr.neolab.sdk.pen.usb.lib.NDACLib.ndac;


/**
 * Created by KIM on 2018-02-08.
 */

public class UsbAdt implements IPenAdt {
    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static final int BAUD_RATE = 460800;//460800;//9600;//115200;//115200; 128000;//9600; // BaudRate. Change this value if you need

    public static final int ACCEPT_VID = 1155;
    public static final int ACCEPT_PID = 22336;

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private boolean serialPortConnected;

    private static final boolean IS_SYNC_MODE = false;

    /**
     * if app is restarted while pen move without lift(=up) pen, remaining data can be received when read data for the first time.
     * So, discard first received pen info and request pen info again even though first pen info is normal.
     */
    private boolean isFirstPenInfo = false;

    private boolean isInitNDAC = false;

    private Queue<byte[]> mReadDataQueue = null;
    private NDACThread mNdacThread = null;

    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                NLog.d( "[UsbAdt] ACTION_USB_PERMISSION() granted="+granted );
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    UsbDevice tempDevice = arg1.getExtras().getParcelable(UsbManager.EXTRA_DEVICE);
                    device = tempDevice;
                    connection = usbManager.openDevice(device);
                    penBtName = device.getDeviceName();
                    new ConnectionThread().start();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            } else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                //Do nothing
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected. send an intent to the Main Activity
                disconnect();

                if(isInitNDAC)
                    ndac.Finalize();

                isInitNDAC = false;

                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
            }
        }
    };


    private static UsbAdt myInstance = null;

    private Context mContext;

    private IPenMsgListener listener = null;
    private IPenDotListener dotListener = null;

    private ConnectedThread mConnectionThread;

    private int mProtocolVer = 0;

    private int status = CONN_STATUS_IDLE;

    /**
     * The Pen address.
     */
    public String penAddress = null;
    /**
     * The Pen bt name.
     */
    public String penBtName = null;
    /**
     * The Is free pass.
     */
    public Boolean isFreePass = false;

    private boolean mIsRegularDisconnect = false;


    private static final boolean USE_QUEUE = true;

    private IMetadataListener metadataListener = null;

    /**
     * The constant QUEUE_DOT.
     */
    public static final int QUEUE_DOT = 1;
    /**
     * The constant QUEUE_MSG.
     */
    public static final int QUEUE_MSG = 2;

    Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage( Message msg )
        {
            switch ( msg.what )
            {
                case QUEUE_DOT:
                {
                    Fdot dot = (Fdot) msg.obj;
                    dot.mac_address = penAddress;
                    if(dotListener != null)
                        dotListener.onReceiveDot(penAddress, dot);
                }
                break;

                case QUEUE_MSG:
                {
                    PenMsg pmsg = (PenMsg) msg.obj;
                    pmsg.sppAddress = penAddress;
                    if(pmsg.penMsgType == PenMsgType.PEN_DISCONNECTED || pmsg.penMsgType == PenMsgType.PEN_CONNECTION_FAILURE)
                    {
                        NLog.d( "[UsbAdt/mHandler] PenMsgType.PEN_DISCONNECTED" );
                        penAddress = null;
                    }
                    listener.onReceiveMessage(pmsg.sppAddress, pmsg );
                }
                break;

                default:
                    break;

            }
        }
    };

    public synchronized static UsbAdt getInstance() {
        if(myInstance == null) {
            myInstance = new UsbAdt();
        }
        return myInstance;
    }

    public UsbAdt() {
        //mUsbManager = mContext.getSystemService(UsbManager.class);
    }



    /**
     * set up listener of message from pen
     *
     * @param listener callback interface
     */
    @Override
    public void setListener(IPenMsgListener listener) {
        this.listener = listener;
    }

    /**
     * set up listener of dot message from pen
     *
     * @param listener the listener
     */
    @Override
    public void setDotListener(IPenDotListener listener) {
        dotListener = listener;
    }

    /**
     * set up listener of offlineData from pen
     * supported from Protocol 2.0
     *
     * @param listener callback interface
     */
    @Override
    public void setOffLineDataListener(IOfflineDataListener listener) {
        NLog.e("setOffLineDataListener(IOfflineDataListener listener) is not supported in wired pen. !!!" );
    }

    @Override
    public void setMetadataListener(IMetadataListener listener) {
        this.metadataListener = listener;
    }

    /**
     * get up listener of message from pen
     *
     * @return IPenMsgListener listener
     */
    @Override
    public IPenMsgListener getListener() {
        return listener;
    }

    @Override
    public IPenDotListener getDotListener() {
        return this.dotListener;
    }

    /**
     * get up listener of offlineData from pen
     * supported from Protocol 2.0
     *
     * @return IOfflineDataListener off line data listener
     */
    @Override
    public IOfflineDataListener getOffLineDataListener() {
        return null;
    }

    @Override
    public IMetadataListener getMetadataListener() {
        return this.metadataListener;
    }

    /**
     * Attempts to connect to the pen.
     *
     * @param address MAC address of pen
     */
    @Override
    public synchronized void connect(String address) {
        connect( address, false);
    }

    /**
     * Connect.
     *
     * @param address    the address
     * @param isFreePass the is free pass
     */
    public synchronized void connect( String address, boolean isFreePass)
    {
        mIsRegularDisconnect = false;
        if(this.penAddress != null)
        {
            if(status == CONN_STATUS_AUTHORIZED)
            {
                responseMsg( new PenMsg( PenMsgType.PEN_ALREADY_CONNECTED));
                return;
            }
            else if(status != CONN_STATUS_IDLE)
            {
                return;
            }
        }

        if(mContext == null) {
            throw new NullPointerException("setContext() must be called before call connect().");
        }

        boolean ret = isAvailableDevice( address );

        if ( !ret )
        {
            NLog.e( "[UsbAdt] Your device is not allowed." );

            this.responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
            return;
        }

        if ( status != CONN_STATUS_IDLE )
        {
            this.responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
            return;
        }

        this.penAddress = address;
        this.isFreePass = isFreePass;

        // N3C6xy(1), N3C7xy(2), G3C6xy(3)
        // pen_mode (0) : BULK MODE, (1) CDC MODE
//        ndac.Init(1, 1);
        ndac.Init();
        isInitNDAC = true;
        if (!serialPortConnected)
            findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
    }

    /**
     * And disconnect the connection with pen
     */
    @Override
    public synchronized void disconnect() {
        NLog.i( "[UsbAdt] disconnect device" );

        if ( mConnectionThread != null )
        {
            mConnectionThread.unbind(true);
        }

        if(mContext != null) {
            try {
                mContext.unregisterReceiver(usbReceiver);
            }catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Confirm whether or not the MAC address to connect
     * If use ble adapter, throws BLENotSupprtedException
     *
     * @param mac the mac
     * @return true if can use, otherwise false
     */
    @Override
    public boolean isAvailableDevice(String mac) {
        return true;
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
    @Override
    public boolean isAvailableDevice(byte[] mac) {
        return true;
    }

    /**
     * Connected to the pen's current information.
     *
     * @return connected device
     */
    @Override
    public String getConnectedDevice() {
        NLog.d( "getConnectedDevice status="+status );
        if(status == CONN_STATUS_AUTHORIZED)
            return penAddress;
        else
            return null;
    }

    /**
     * Gets connecting device.
     *
     * @return the connecting device
     */
    @Override
    public String getConnectingDevice() {
        NLog.d( "getConnectingDevice status="+status );
        if(status == CONN_STATUS_TRY || status == CONN_STATUS_BINDED|| status == CONN_STATUS_ESTABLISHED)
            return penAddress;
        else
            return null;
    }

    /**
     * When pen requested password, you can response password by this method.
     *
     * @param password the password
     */
    @Override
    public void inputPassword(String password) {
        NLog.e("inputPassword(String password) is not supported in wired pen. !!!" );
    }

    /**
     * Change the password of pen.
     *
     * @param oldPassword current password
     * @param newPassword new password
     */
    @Override
    public void reqSetupPassword(String oldPassword, String newPassword) {
        NLog.e( "reqSetupPassword(String oldPassword, String newPassword) is not supported in wired pen. !!!" );
    }

    /**
     * Req set up password off.
     * supported from Protocol 2.0
     *
     * @param oldPassword the old password
     */
    @Override
    public void reqSetUpPasswordOff(String oldPassword) {
        NLog.e( "reqSetUpPasswordOff( String oldPassword ) is not supported in wired pen. !!!" );
    }

    /**
     * Connected to the current state of the pen provided.
     */
    @Override
    public void reqPenStatus() {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqPenStatus();
    }

    /**
     * To upgrade the firmware of the pen.
     *
     * @param fwFile     object of firmware
     * @param targetPath The file path to be stored in the pen
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @deprecated reqFwUpgrade(File fwFile, String targetPath) is replaced by reqFwUpgrade2( File fwFile, String fwVersion ) in the protocol 2.0
     */
    @Override
    public void reqFwUpgrade(File fwFile, String targetPath) throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "reqFwUpgrade( File fwFile, String targetPath ) is not supported in wired pen !!!");
    }

    /**
     * Req fw upgrade 2.
     * supported from Protocol 2.0
     * isCompress default true
     *
     * @param fwFile    the fw file
     * @param fwVersion the fw version
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    @Override
    public void reqFwUpgrade2(File fwFile, String fwVersion) throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "reqFwUpgrade2(File fwFile, String fwVersion) is not supported in wired pen !!!");
    }

    /**
     * Req fw upgrade 2.
     *
     * @param fwFile     the fw file
     * @param fwVersion  the fw version
     * @param isCompress data compress true, uncompress false
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    @Override
    public void reqFwUpgrade2(File fwFile, String fwVersion, boolean isCompress) throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "reqFwUpgrade2(File fwFile, String fwVersion, boolean isCompress) is not supported in wired pen !!!");
    }

    /**
     * To suspend Upgrading task.
     */
    @Override
    public void reqSuspendFwUpgrade(){
        NLog.e( "reqSuspendFwUpgrade() is not supported in wired pen. !!!" );
    }

    /**
     * Adjust the pressure-sensor to the pen.
     */
    @Override
    public void reqForceCalibrate(){
        NLog.e( "reqForceCalibrate() is not supported in wired pen. !!!" );
    }

    /**
     * Sets calibrate.
     *
     * @param factor the factor
     */
    @Override
    public void reqCalibrate2(float[] factor){
        NLog.e("reqCalibrate2(float[] factor) is not supported in wired pen. !!!" );
    }

    /**
     * Specify whether you want to get the data off-line.
     *
     * @param allow if allow receive offline data, set true
     */
    @Override
    public void setAllowOfflineData(boolean allow){
        NLog.e("setAllowOfflineData(boolean allow) is not supported in wired pen. !!!" );
    }

    /**
     * Specify where to store the offline data. (Unless otherwise specified, is stored in the default external storage)
     *
     * @param path Be stored in the directory
     */
    @Override
    public void setOfflineDataLocation(String path){
        NLog.e( "setOfflineDataLocation(String path) is not supported in wired pen. !!!" );
    }

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteIds   array of note id
     */
    @Override
    public void reqAddUsingNote(int sectionId, int ownerId, int[] noteIds) {
        if ( !isConnected() )
        {
            return;
        }

        UseNoteData data = new UseNoteData();
        data.sectionId = sectionId;
        data.ownerId = ownerId;
        data.noteIds = noteIds;
        ArrayList<UseNoteData> list = new ArrayList<UseNoteData>();
        list.add( data );

        byte[] response = ndac.RequestVpen( ProtocolParser20.buildAddUsingNotes( list ) );
        mConnectionThread.getPacketProcessor().fill(response, response.length);
    }

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     */
    @Override
    public void reqAddUsingNote(int sectionId, int ownerId) {
        if ( !isConnected() )
        {
            return;
        }

        byte[] response = ndac.RequestVpen( ProtocolParser20.buildAddUsingNotes( sectionId, ownerId ) );
        mConnectionThread.getPacketProcessor().fill(response, response.length);
    }

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param sectionId section ids of note
     * @param ownerId   owner ids of note
     */
    @Override
    public void reqAddUsingNote(int[] sectionId, int[] ownerId) {
        if ( !isConnected() )
        {
            return;
        }

        byte[] response = ndac.RequestVpen( ProtocolParser20.buildAddUsingNotes( sectionId, ownerId ) );
        mConnectionThread.getPacketProcessor().fill(response, response.length);
    }

    /**
     * Specifies that all of the available notes.
     * Note! It overwrites the using note data from protocol 2.0 .
     */
    @Override
    public void reqAddUsingNoteAll() {
        if ( !isConnected() )
        {
            return;
        }

        byte[] response = ndac.RequestVpen( ProtocolParser20.buildAddUsingAllNotes() );
        mConnectionThread.getPacketProcessor().fill(response, response.length);
    }

    /**
     * Notes for use in applications specified.
     * supported from Protocol 2.0
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param noteList the note list
     */
    @Override
    public void reqAddUsingNote(ArrayList<UseNoteData> noteList) {
        if ( !isConnected() )
        {
            return;
        }

        byte[] response = ndac.RequestVpen( ProtocolParser20.buildAddUsingNotes(noteList) );
        mConnectionThread.getPacketProcessor().fill(response, response.length);
    }

    /**
     * The pen is stored in an offline transfer of data requested.
     * (Please note that this function is not synchronized. If multiple threads concurrently try to run this function, explicit synchronization must be done externally.)
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteId    of note
     */
    @Override
    public void reqOfflineData(int sectionId, int ownerId, int noteId){
        NLog.e("reqOfflineData(int sectionId, int ownerId, int noteId) is not supported in wired pen.");
    }

    @Override
    public void reqOfflineData(int sectionId, int ownerId, int noteId, boolean deleteOnFinished) {
        NLog.e("reqOfflineData() is not supported in wired pen.");

    }

    /**
     * The pen is stored in an offline transfer of data requested.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param pageIds   the page ids
     */
    @Override
    public void reqOfflineData(int sectionId, int ownerId, int noteId, int[] pageIds) {
        NLog.e("reqOfflineData(int sectionId, int ownerId, int noteId, int[] pageIds) is not supported in wired pen.");
    }

    @Override
    public void reqOfflineData(int sectionId, int ownerId, int noteId, boolean deleteOnFinished, int[] pageIds) {
        NLog.e("reqOfflineData() is not supported in wired pen.");

    }

    @Override
    public void reqOfflineData(Object extra, int sectionId, int ownerId, int noteId) {
        NLog.e("reqOfflineData() is not supported in wired pen.");

    }

    @Override
    public void reqOfflineData(Object extra, int sectionId, int ownerId, int noteId, int[] pageIds) {
        NLog.e("reqOfflineData() is not supported in wired pen.");

    }

    /**
     * The offline data is stored in the pen to request information.
     */
    @Override
    public void reqOfflineDataList() {
        NLog.e("reqOfflineDataList() is not supported in wired pen.");
    }

    /**
     * The offline data is stored in the pen to request information.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     */
    @Override
    public void reqOfflineDataList(int sectionId, int ownerId) {
        NLog.e("reqOfflineDataList(int sectionId, int ownerId) is not supported in wired pen.");
    }

    /**
     * The offline data per page is stored in the pen to request information.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     */
    @Override
    public void reqOfflineDataPageList(int sectionId, int ownerId, int noteId) {
        NLog.e("reqOfflineDataList(int sectionId, int ownerId, int noteId) is not supported in wired pen.");
    }

    /**
     * To Delete offline data of pen
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @deprecated removeOfflineData(int sectionId, int ownerId) is replaced by removeOfflineData( int sectionId, int ownerId ,int[] noteIds) in the protocol 2.0
     */
    @Override
    public void removeOfflineData(int sectionId, int ownerId) {
        NLog.e("removeOfflineData(int sectionId, int ownerId) is not supported in wired pen.");
    }

    /**
     * Remove offline data.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteIds   the note ids
     */
    @Override
    public void removeOfflineData(int sectionId, int ownerId, int[] noteIds) {
        NLog.e("removeOfflineData(int sectionId, int ownerId, int[] noteIds) is not supported in wired pen.");
    }

    @Override
    public void removeOfflineDataByPage(int sectionId, int ownerId, int noteId, int[] pageIds) {
        NLog.e("removeOfflineDataByPage(int sectionId, int ownerId, int noteId, int[] pageIds) is not supported in wired pen.");
    }



    @Override
    public void reqOfflineNoteInfo(int sectionId, int ownerId, int noteId) {
        NLog.e("reqOfflineNoteInfo(int sectionId, int ownerId, int noteId) is not supported in wired pen.");

    }

    /**
     * Disable or enable Auto Power function
     *
     * @param setOn the set on
     */
    @Override
    public void reqSetupAutoPowerOnOff(boolean setOn) {
        NLog.e("reqSetupAutoPowerOnOff(boolean setOn) is not supported in wired pen. !!!" );
    }

    /**
     * Disable or enable sound of pen
     *
     * @param setOn the set on
     */
    @Override
    public void reqSetupPenBeepOnOff(boolean setOn) {
        NLog.e( "reqSetupPenBeepOnOff(boolean setOn) is not supported in wired pen. !!!" );
    }

    /**
     * Setup color of pen
     *
     * @param color the color
     */
    @Override
    public void reqSetupPenTipColor(int color) {
        NLog.e( "reqSetupPenTipColor(int color) is not supported in wired pen. !!!" );
    }

    /**
     * Setup auto shutdown time of pen
     *
     * @param minute shutdown wait time of pen
     */
    @Override
    public void reqSetupAutoShutdownTime(short minute) {
        NLog.e( "reqSetupAutoShutdownTime(short minute) is not supported in wired pen. !!!" );
    }

    /**
     * Setup Sensitivity level of pen
     *
     * @param level sensitivity level (0~4)
     */
    @Override
    public void reqSetupPenSensitivity(short level) {
        NLog.e( "reqSetupPenSensitivity(short level) is not supported in wired pen. !!!" );
    }

    /**
     * Notice!!
     * This is API for hardware developer!
     * Software developers should never use this API.
     * <p>
     * Setup Sensitivity level of pen using FSC press sensor
     * <p>
     * supported from Protocol 2.0
     *
     * @param level sensitivity level (0~4)
     */
    @Override
    public void reqSetupPenSensitivityFSC(short level) {
        NLog.e( "reqSetupPenSensitivityFSC(short level) is not supported in wired pen. !!!" );
    }

    /**
     * Req set pen cap on off.
     * supported from Protocol 2.0
     *
     * @param on the on
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    @Override
    public void reqSetupPenCapOff(boolean on) throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "reqSetupPenCapOff(boolean on) is not supported in wired pen. !!!" );
    }

    /**
     * Req setup pen hover.
     *
     * @param on the on
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    @Override
    public void reqSetupPenHover(boolean on) throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "reqSetupPenHover(boolean on) is not supported in wired pen. !!!" );
    }

    @Override
    public void reqSetupPenDiskReset(){
        NLog.e("reqSetupPenDiskReset() is not supported in wired pen.");

    }

    /**
     * set Context
     *
     * @param context the context
     */
    @Override
    public void setContext(Context context) {
        mContext = context;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
//        filter.addAction(ACTION_USB_ATTACHED);
        mContext.registerReceiver(usbReceiver, filter);
        NLog.d( "[UsbAdt] setContext()" );

        usbManager = (UsbManager) mContext.getSystemService(USB_SERVICE);
    }

    /**
     * get Context
     *
     * @return Context context
     */
    @Override
    public Context getContext() {
        return mContext;
    }

    /**
     * Gets protocol version.
     * supported from Protocol 2.0
     *
     * @return the protocol version
     */
    @Override
    public int getProtocolVersion() {
        return mProtocolVer;
    }

    /**
     * Gets pen status.
     *
     * @return the pen status
     */
    @Override
    public int getPenStatus() {
        return status;
    }

    /**
     * Get connected status
     *
     * @return true is connected, false is disconnected
     */
    @Override
    public boolean isConnected() {
        return (status == CONN_STATUS_AUTHORIZED || status == CONN_STATUS_ESTABLISHED);
    }

    /**
     * Get Pen Mac Address
     * BT Mac address is Real mac address
     * BLE mac address is virtual mac address
     *
     * @return the pen address
     */
    @Override
    public String getPenAddress() {
        return penAddress;
    }

    /**
     * Get Pen Bt Name
     *
     * @return the bt name
     */
    @Override
    public String getPenBtName() {
        return null;
    }

    /**
     * Gets press sensor type.
     *
     * @return the press sensor type
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    @Override
    public int getPressSensorType() {
        if ( !isConnected() )
        {
            return -1;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            return ((CommProcessor20)mConnectionThread.getPacketProcessor()).getPressSensorType();
        else
        {
            return ((CommProcessor)mConnectionThread.getPacketProcessor()).getPressSensorType();
//			NLog.e( "getPressSensorType( ) is supported from protocol 2.0 !!!" );
//			throw new ProtocolNotSupportedException( "getPressSensorType( ) is supported from protocol 2.0 !!!");
        }
    }

    /**
     * Gets connect device name.
     * supported from Protocol 2.0
     *
     * @return the connect device name
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    @Override
    public String getConnectDeviceName() throws ProtocolNotSupportedException {
        if ( !isConnected() )
        {
            return null;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            return ((CommProcessor20)mConnectionThread.getPacketProcessor()).getConnectDeviceName( );
        else
        {
            NLog.e( "getConnectDeviceName( ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "getConnectDeviceName( ) is supported from protocol 2.0 !!!");
        }
    }

    /**
     * Gets connect sub name.
     * supported from Protocol 2.0
     *
     * @return the connect sub name
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    @Override
    public String getConnectSubName() throws ProtocolNotSupportedException {
        if ( !isConnected() )
        {
            return null;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            return ((CommProcessor20)mConnectionThread.getPacketProcessor()).getConnectSubName( );
        else
        {
            NLog.e( "getConnectSubName( ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "getConnectSubName( ) is supported from protocol 2.0 !!!");
        }
    }

    @Override
    public String getReceiveProtocolVer() throws ProtocolNotSupportedException {
        if ( !isConnected() )
        {
            return null;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            return ((CommProcessor20)mConnectionThread.getPacketProcessor()).getReceiveProtocolVer( );
        else
        {
            NLog.e( "getReceiveProtocolVer( ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "getReceiveProtocolVer( ) is supported from protocol 2.0 !!!");
        }
    }

    @Override
    public String getFirmwareVer() throws ProtocolNotSupportedException {
        if ( !isConnected() )
        {
            return null;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            return ((CommProcessor20)mConnectionThread.getPacketProcessor()).getFirmwareVer( );
        else
        {
            NLog.e( "getFirmwareVer( ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "getFirmwareVer( ) is supported from protocol 2.0 !!!");
        }
    }

    @Override
    public void createProfile ( String proFileName, byte[] password ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
    {
        throw new ProtocolNotSupportedException( "createProfile ( String proFileName, String password ) is not supported in wired pen.");
    }

    @Override
    public void deleteProfile ( String proFileName, byte[] password ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
    {
        throw new ProtocolNotSupportedException( "deleteProfile ( String proFileName, String password ) is not supported in wired pen.");
    }

    @Override
    public void writeProfileValue ( String proFileName, byte[] password, String[] keys, byte[][] data ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
    {
        throw new ProtocolNotSupportedException( "writeProfileValue ( String proFileName, String password, String[] keys, byte[][] data ) is not supported in wired pen.");
    }

    @Override
    public void readProfileValue ( String proFileName, String[] keys ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
    {
        throw new ProtocolNotSupportedException( "readProfileValue ( String proFileName, String[] keys ) is not supported in wired pen.");
    }

    @Override
    public void deleteProfileValue ( String proFileName, byte[] password, String[] keys ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
    {
        throw new ProtocolNotSupportedException( "deleteProfileValue ( String proFileName, String password, String[] keys ) is not supported in wired pen.");
    }

    @Override
    public void getProfileInfo ( String proFileName ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
    {
        throw new ProtocolNotSupportedException( "getProfileInfo ( String proFileName ) is not supported in wired pen.");
    }

    @Override
    public boolean isSupportPenProfile ()
    {
        return false;
    }

    /**
     * Req set current time.
     */
    @Override
    public void reqSetCurrentTime() {
        if ( !isConnected() )
        {
            return;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetCurrentTime( );
        else
        {
            ((CommProcessor)mConnectionThread.getPacketProcessor()).reqSetCurrentTime( );
        }
    }

    @Override
    public void setPipedInputStream(PipedInputStream pipedInputStream) {
        return;
    }

    @Override
    public short getColorCode() throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "getProfileInfo ( String proFileName ) is not supported in wired pen.");
    }

    @Override
    public short getProductCode() throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "getProfileInfo ( String proFileName ) is not supported in wired pen.");
    }

    @Override
    public short getCompanyCode() throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "getProfileInfo ( String proFileName ) is not supported in wired pen.");
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean isSupportHoverCommand() throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "getProfileInfo ( String proFileName ) is not supported in wired pen.");
    }


    @Override
    public int getConnectPenType() throws ProtocolNotSupportedException {
        if ( !isConnected() )
        {
            return 0;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            return ((CommProcessor20)mConnectionThread.getPacketProcessor()).getConnectPenType( );
        else
        {
            NLog.e( "getConnectPenType( ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "getConnectPenType( ) is supported from protocol 2.0 !!!");
        }
    }

    @Override
    public void reqSystemInfo() throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "reqSetPerformance() is supported from protocol 2.0 !!!" );
    }

    @Override
    public void reqSetPerformance(int step) throws ProtocolNotSupportedException {
        throw new ProtocolNotSupportedException( "reqSetPerformance() is supported from protocol 2.0 !!!" );
    }

    @Override
    public boolean unpairDevice(String address) { return false; }

//    public void reqSetPenTipOffset(int x, int y) throws ProtocolNotSupportedException {
//        if ( !isConnected() )
//        {
//            return;
//        }
//
//        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
//            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetPenTipOffset(x, y);
//        else
//        {
//            throw new ProtocolNotSupportedException( "reqSetCurrentTime is not supported in protocol version 1.");
//        }
//    }

//    public void reqSetDefaultCameraRegister() throws ProtocolNotSupportedException {
//        if ( !isConnected() )
//        {
//            return;
//        }
//
//        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
//            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetDefaultCameraRegister();
//        else
//        {
//            throw new ProtocolNotSupportedException( "reqSetDefaultCameraRegister is not supported in protocol version 1.");
//        }
//    }

//    public void reqSetCameraRegister(ArrayList<byte[]> values) throws ProtocolNotSupportedException {
//        if ( !isConnected() )
//        {
//            return;
//        }
//
//        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
//            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetCameraRegister(values);
//        else
//        {
//            throw new ProtocolNotSupportedException( "reqSetCameraRegister is not supported in protocol version 1.");
//        }
//    }

    private void responseMsg( PenMsg msg )
    {
        if ( listener != null )
        {
            if ( USE_QUEUE )
            {
                mHandler.obtainMessage( QUEUE_MSG, msg ).sendToTarget();
            }
            else
            {
                listener.onReceiveMessage( penAddress, msg );
            }
        }
    }

    private void responseDot( Fdot dot )
    {
        if ( listener != null )
        {
            if ( USE_QUEUE )
            {
                mHandler.obtainMessage( QUEUE_DOT, dot ).sendToTarget();
            }
            else
            {
                if(dotListener != null)
                    dotListener.onReceiveDot(penAddress, dot.toDot() );
            }
        }
    }

    private void bindConnection(UsbDevice device)
    {
        NLog.i( "[UsbAdt] bindConnection by USB : " + device.getDeviceName() + ";mProtocolVer=" + mProtocolVer + ";Vendor ID = " + device.getVendorId() + ";Product ID = " + device.getProductId() );

        mConnectionThread = new ConnectedThread(mProtocolVer);

        if ( mConnectionThread.bind( device ) )
        {
            mConnectionThread.start();
            this.onBinded();
        }
        else
        {
            mProtocolVer = 0;
            mConnectionThread = null;
        }
    }

    private void onLostConnection()
    {
        status = CONN_STATUS_IDLE;

        NLog.d( "[UsbAdt/ConnectThread] onLostConnection mIsRegularDisconnect="+mIsRegularDisconnect );
        if(mIsRegularDisconnect)
            responseMsg( new PenMsg( PenMsgType.PEN_DISCONNECTED ) );
        else
        {
            try
            {
                int wiredPenStatus = mConnectionThread.getPacketProcessor() instanceof CommProcessor20 ? ((CommProcessor20) mConnectionThread.getPacketProcessor()).getWiredPenStatus() : CommProcessor20.WIRED_PEN_STATUS_NORMAL;

                JSONObject job = new JSONObject()
                        .put( JsonTag.BOOL_REGULAR_DISCONNECT, mIsRegularDisconnect )
                        .put( JsonTag.INT_WIREDPEN_DISCONNECT_REASON, wiredPenStatus );
                responseMsg( new PenMsg( PenMsgType.PEN_DISCONNECTED ,job) );
            }catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        mIsRegularDisconnect = false;
    }

    private void onBinded( )
    {
        status = CONN_STATUS_BINDED;
//		penAddress = address;
    }

    private void onConnectionEstablished()
    {
        status = CONN_STATUS_ESTABLISHED;
//		penAddress = address;
    }

    private void onConnectionAuthorized()
    {
        status = CONN_STATUS_AUTHORIZED;
//		penAddress = address;
    }

    private void onConnectionTry()
    {
        status = CONN_STATUS_TRY;
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                UsbDevice tempDevice = entry.getValue();
                if(tempDevice != null) {
                    int deviceVID = tempDevice.getVendorId();
                    int devicePID = tempDevice.getProductId();

                    if (deviceVID == ACCEPT_VID && devicePID == ACCEPT_PID) {
                        // There is a device connected to our Android device. Try to open it as a Serial Port.
                        if (usbManager.hasPermission(tempDevice)) {
                            device = tempDevice;
                            connection = usbManager.openDevice(device);
                            penBtName = device.getDeviceName();
                            new ConnectionThread().start();
                            return;
                        } else {
                            requestUserPermission(tempDevice);
                        }

                        keep = false;
                    } else {
                        connection = null;
                        device = null;
                    }
                }

                if (!keep)
                    break;
            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            mContext.sendBroadcast(intent);
        }
    }

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission(UsbDevice device) {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    public boolean isAlreadyConnected() {
        //If there is already a connected device, connect automatically.
        if(mContext == null) {
            throw new NullPointerException("setContext() must be called before call isAlreadyConnected().");
        }

        UsbManager usbManager = (UsbManager) mContext.getSystemService(USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                UsbDevice device = entry.getValue();
                if(device != null) {
                    int deviceVID = device.getVendorId();
                    int devicePID = device.getProductId();

                    if (deviceVID == UsbAdt.ACCEPT_VID && devicePID == UsbAdt.ACCEPT_PID) {
                        // There is a device connected to our Android device. Try to open it as a Serial Port.
                        if (usbManager.hasPermission(device)) {
                            return true;
                        } else {
                            requestUserPermission(device);
                        }
                    }
                }
            }
        }

        return false;
    }

    /*
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    public class ConnectionThread extends Thread {
        @Override
        public void run() {
            mProtocolVer = 2;
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null)
            {
                boolean isOpend = IS_SYNC_MODE ? serialPort.syncOpen() : serialPort.open();
                if ( isOpend ) {
                    serialPortConnected = true;
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);

                    /**
                     * Current flow control Options:
                     * UsbSerialInterface.FLOW_CONTROL_OFF
                     * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
                     * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
                     */
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

                    bindConnection(device);

                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_READY);
                    mContext.sendBroadcast(intent);
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort instanceof CDCSerialDevice) {
//                    if (serialPort instanceof XdcVcpSerialDevice) {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        mContext.sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        mContext.sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                mContext.sendBroadcast(intent);
            }
        }
    }

    public class ConnectedThread extends Thread implements IConnectedThread
    {
        private UsbDevice device;
        private CommandManager processor;

        private AtomicBoolean isRunning = new AtomicBoolean(false);

        private String macAddress = "";

        public ConnectedThread(int protocolVer)
        {
            String version = "";
            if(mContext != null)
            {
                try
                {
                    version = mContext.getPackageManager().getPackageInfo( mContext.getPackageName() , 0).versionName;
                }catch ( Exception e ){}

            }
            if(protocolVer == 2)
                processor = new CommProcessor20( this, version );
            else
                processor = new CommProcessor( this );

            mReadDataQueue = new ConcurrentLinkedQueue<byte[]>();
            mNdacThread = new NDACThread(processor);
            mNdacThread.setPriority(Thread.MAX_PRIORITY);
            mNdacThread.start();
        }

        /**
         * Stop running.
         */
        public void stopRunning()
        {
            NLog.d( "[UsbAdt/ConnectedThread] stopRunning()" );
            if(processor != null)
            {
                if ( processor instanceof CommProcessor20 )
                {
                    try {
                        ((CommProcessor20)processor).finish();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
            this.isRunning.set(false);

            onLostConnection();     //if serial port is async mode, onLostConnection() must be called in stopRunning()

            if(IS_SYNC_MODE && serialPort != null)
                serialPort.syncClose();
            else if(serialPort != null)
                serialPort.close();

            serialPortConnected = false;

            if(mNdacThread != null) {
                mNdacThread.setRunning(false);
                synchronized (mReadDataQueue) {
                    mReadDataQueue.notifyAll();
                }
                mNdacThread.interrupt();
            }
        }

        public boolean bind(UsbDevice device) {
            this.device = device;

            this.isRunning.set(true);
            startConnect();
            return true;
        }

        /**
         * Unbind.
         */
        public void unbind()
        {
            unbind(false);
        }

        /**
         * Unbind.
         *
         * @param isRegularDisconnect the is regular disconnect
         */
        public void unbind(boolean isRegularDisconnect)
        {
            NLog.d( "[UsbAdt/ConnectedThread] unbind() isRegularDisconnect="+isRegularDisconnect );
            mIsRegularDisconnect = isRegularDisconnect;
            mProtocolVer = 0;

            this.stopRunning();
            processor = null;
        }

        /**
         * Gets mac address.
         *
         * @return the mac address
         */
        public String getMacAddress()
        {
            return macAddress;
        }

        /**
         * Gets packet processor.
         *
         * @return the packet processor
         */
        public CommandManager getPacketProcessor()
        {
            return processor;
        }

        /**
         * Gets is established.
         *
         * @return the is established
         */
        public boolean getIsEstablished()
        {
            return status == CONN_STATUS_ESTABLISHED || status ==CONN_STATUS_AUTHORIZED;
        }

        /**
         * Start connect.
         */
        public void startConnect()
        {
//            Handler handler = new Handler( Looper.getMainLooper());
            NLog.d( "startConnect" );

            ( (CommProcessor20) processor ).reqPenInfoForWiredPen();
            isFirstPenInfo = true;
        }

        public void onEstablished()
        {
            onConnectionEstablished( );
        }

        /**
         * On authorized.
         */
        public void onAuthorized()
        {
            onConnectionAuthorized();
        }

        public void run()
        {
            NLog.d( "[UsbAdt/ConnectedThread] STARTED" );
            setName( "ConnectionThread" );

            if ( this.isRunning.get() ) {
                if (IS_SYNC_MODE) {
                    this.read();
                } else {
                    serialPort.read(new UsbSerialInterface.UsbReadCallback() {
                        //                    long previousTime = 0;
                        //                    int receivedCount = 0;
                        @Override
                        public void onReceivedData(byte[] readBuffer) {
                            //                        if(receivedCount == 0) {
                            //                            previousTime = System.currentTimeMillis();
                            //                        }
                            //                        receivedCount++;
                            //
                            //                        if( (System.currentTimeMillis() - previousTime) > 1000) {
                            ////                            Log.d("[ConnectedThread]", "fps : "+ receivedCount);
                            //                            try {
                            //                                JSONObject job = new JSONObject()
                            //                                        .put(JsonTag.INT_USB_READ_FPS, receivedCount);
                            //                                responseMsg(new PenMsg(PenMsgType.USB_READ_FPS, job));
                            //                            }catch (Exception e) {
                            //                                e.printStackTrace();
                            //                            }
                            //
                            //                            receivedCount = 0;
                            //                        }

                            //                        NLog.d( "[UsbAdt/ConnectedThread] onReceivedData : " + (System.currentTimeMillis() /* - previousTime*/ ) );

                            int readBytes = readBuffer.length;

//                            NLog.d( "[UsbAdt/ConnectedThread] TimeCheck read bytes="+readBytes );

                            if (readBytes > 0) {
//                                byte[] readBuffer2 = new byte[readBytes];
//                                System.arraycopy(readBuffer, 0, readBuffer2, 0, readBytes);
//                                StringBuffer sb = new StringBuffer();
//                                for (int i = 0; i < readBytes; i++) {
//                                    byte item = readBuffer2[i];
//                                    int int_data = (int) (item & 0xFF);
//                                    sb.append(Integer.toHexString(int_data) + ", ");
//                                }
//
//                                NLog.d("[UsbAdt/ConnectedThread] read bytes data = " + sb.toString());

                                //If first pen info is received, discard it and request pen info again.
                                if (isFirstPenInfo) {
                                    isFirstPenInfo = false;
                                    ((CommProcessor20) processor).reqPenInfoForWiredPen();
                                    NLog.d("[UsbAdt/ConnectedThread] First penInfo has been discarded and request second pen info.");
                                    return;
                                }

                                enqueueReadBuffer(readBuffer);

                                //                            previousTime = System.currentTimeMillis();

                                //                            byte[] decodedBuffer = ndac.Decode(readBuffer, readBytes);
                                //                            if(decodedBuffer != null && decodedBuffer.length > 0) {
                                //                                sb = new StringBuffer();
                                //                                for (int i = 0; i < decodedBuffer.length; i++) {
                                //                                    byte item = decodedBuffer[i];
                                //                                    int int_data = (int) (item & 0xFF);
                                //                                    sb.append(Integer.toHexString(int_data) + ", ");
                                //                                }
                                //                                NLog.d("[UsbAdt/ConnectedThread] Decode bytes length = " + decodedBuffer.length + "data = " + sb.toString());
                                //
                                //                                processor.fill(decodedBuffer, decodedBuffer.length);
                                //                            }
                                //                            else {
                                //                                NLog.e( "[UsbAdt/ConnectedThread] Decode data is null or empty. decodedBuffer = " + decodedBuffer);
                                //                            }
                            } else if (readBytes < 0) {
                                ConnectedThread.this.stopRunning();
                            }
                        }
                    });
                }
            }
        }

        /**
         * Read.
         */
        public void read()
        {
            byte[] readBuffer = new byte[4096];
            int readBytes;

            while ( this.isRunning.get() )
            {
                readBytes = serialPort.syncRead(readBuffer, 0);

                if (readBytes > 0) {
                    //                            byte[] readBuffer2 = new byte[readBytes];
                    //                            System.arraycopy(readBuffer, 0, readBuffer2, 0, readBytes);
                    //                            StringBuffer sb = new StringBuffer();
                    //                            for (int i = 0; i < readBytes; i++) {
                    //                                byte item = readBuffer2[i];
                    //                                int int_data = (int) (item & 0xFF);
                    //                                sb.append(Integer.toHexString(int_data) + ", ");
                    //                            }
                    //
                    //                            NLog.d("[UsbAdt/ConnectedThread] read bytes data = " + sb.toString());

                    //If first pen info is received, discard it and request pen info again.
                    if (isFirstPenInfo) {
                        isFirstPenInfo = false;
                        ((CommProcessor20) processor).reqPenInfoForWiredPen();
                        NLog.d("[UsbAdt/ConnectedThread] First penInfo has been discarded and request second pen info.");
                        continue;
                    }

                    enqueueReadBuffer(readBuffer);
                } else if (readBytes < 0) {
                    this.stopRunning();
                }
            }

            onLostConnection();
        }

        /**
         * write buffer at output stream.
         *
         * @param buffer the buffer
         */
        public void write( byte[] buffer )
        {
            if(IS_SYNC_MODE && serialPort != null)
                serialPort.syncWrite(buffer, 0);
            else if(serialPort != null)
                serialPort.write(buffer);
        }

        /**
         * On create msg.
         *
         * @param msg the msg
         */
        public void onCreateMsg( PenMsg msg )
        {
            responseMsg( msg );
        }

        /**
         * On create dot.
         *
         * @param dot the dot
         */
        public void onCreateDot( Fdot dot )
        {
            responseDot( dot );
        }

        /**
         * On create offline strokes.
         *
         * @param offlineByteData the offline byte data
         */
        public void onCreateOfflineStrokes( OfflineByteData offlineByteData) { }

        public boolean getAllowOffline()
        {
            return false;
        }

        public void enqueueReadBuffer(byte[] readBuffer) {
            mReadDataQueue.offer(readBuffer);

            synchronized (mReadDataQueue) {
                mReadDataQueue.notifyAll();
            }
        }
    }

    public class NDACThread extends Thread {
        private CommandManager processor;
        private AtomicBoolean isRunning = new AtomicBoolean(true);

        public NDACThread(CommandManager processor) {
            this.processor = processor;
        }

        public void setRunning(boolean isRunning) {
            this.isRunning.set(isRunning);
        }

        public void run() {
            setName(this.getClass().getSimpleName());

            while (isRunning.get()) {
                while (!mReadDataQueue.isEmpty()) {
//                    long startTime = System.currentTimeMillis();

                    byte[] readBuffer = mReadDataQueue.poll();
                    int readBytes = readBuffer.length;

//                    NLog.d( "[UsbAdt/NDACThread] TimeCheck read bytes="+readBytes );

                    if(readBuffer != null && readBytes > 0) {
//                        byte[] readBuffer2 = new byte[readBytes];
//                        System.arraycopy(readBuffer, 0, readBuffer2, 0, readBytes);
//                        StringBuffer sb = new StringBuffer();
//                        for (int i = 0; i < readBytes; i++) {
//                            byte item = readBuffer2[i];
//                            int int_data = (int) (item & 0xFF);
//                            sb.append(Integer.toHexString(int_data) + ", ");
//                        }
//
//                        NLog.d("[UsbAdt/NDACThread] read bytes data = " + sb.toString());

                        byte[] decodedBuffer = ndac.Decode(readBuffer, readBuffer.length);
                        if(decodedBuffer != null && decodedBuffer.length > 0) {
//                            sb = new StringBuffer();
//                            for (int i = 0; i < decodedBuffer.length; i++) {
//                                byte item = decodedBuffer[i];
//                                int int_data = (int) (item & 0xFF);
//                                sb.append(Integer.toHexString(int_data) + ", ");
//                            }
//                            NLog.d("[UsbAdt/NDACThread] Decode bytes length = " + decodedBuffer.length + ", data = " + sb.toString());

                            processor.fill(decodedBuffer, decodedBuffer.length);
                        }
                        else {
                            Log.d( "[NDACThread]", "read bytes="+readBytes );

                            byte[] tempReaddBuffer = new byte[readBytes];
                            System.arraycopy(readBuffer, 0, tempReaddBuffer, 0, readBytes);
                            StringBuffer sb = new StringBuffer();
                            for (int i = 0; i < readBytes; i++) {
                                byte item = tempReaddBuffer[i];
                                int int_data = (int) (item & 0xFF);
                                sb.append(Integer.toHexString(int_data) + ", ");
                            }

                            Log.d("[NDACThread]",  "read bytes data = " + sb.toString());

                            Log.e( "[NDACThread]", "[UsbAdt/NDACThread] Decode data is null or empty. decodedBuffer = " + decodedBuffer);
                        }
                    }

//                    NLog.d("[UsbAdt/NDACThread] Decode processing time = " + (System.currentTimeMillis() - startTime) +" ms" );
                }

                try {
                    synchronized (mReadDataQueue) {
                        mReadDataQueue.wait();
                    }
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                    NLog.d("NDACThread Interrupted!!" + e);
                }
            }
        }
    }
}
