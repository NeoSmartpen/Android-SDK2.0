package kr.neolab.sdk.pen.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import kr.neolab.sdk.broadcastreceiver.BTDuplicateRemoveBroadcasterReceiver;
import kr.neolab.sdk.pen.IPenAdt;
import kr.neolab.sdk.pen.bluetooth.cmd.CommandManager;
import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor;
import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor20;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.filter.Fdot;
import kr.neolab.sdk.pen.offline.OfflineByteData;
import kr.neolab.sdk.pen.offline.OfflineFile;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsg;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;
import kr.neolab.sdk.util.UseNoteData;

/**
 * Created by CJY on 2017-01-26.
 *
 * Protocol version 2에 대해서만 구현이 되어있으며, protocol version 1과 2간에 BLE 통신이 많이 다르므로
 * protocol version 1을 구현하게 된다면 많이 다른 방식으로 구현이 될것으로 판단됨.
 * (BLE 에서 사용하는 Service와 Characteristic이 크게 다름)
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BTLEAdt implements IPenAdt {
    private static BTLEAdt instance = null;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static BTLEAdt getInstance()
    {
        if ( instance == null )
        {
            synchronized (BTLEAdt.class)
            {
                if ( instance == null )
                    instance = new BTLEAdt();
            }
        }
        return instance;
    }

    private boolean allowOffline = true;

    private IPenMsgListener listener = null;
    private IPenDotListener dotListener = null;
    private IOfflineDataListener offlineDataListener = null;

    private ConnectedThread mConnectionThread;
    /**
     * The Pen address.
     */
    private String penAddress = null;

    private String penSppAddress = null;

    /**
     * The Pen bt name.
     */
    public String penBtName = null;

    private static final boolean USE_QUEUE = true;

    private boolean mIsRegularDisconnect = false;

    private Timer watchDog;
    private TimerTask watchDogTask;
    private boolean watchDogAlreadyCalled = false;

    /**
     * The constant QUEUE_DOT.
     */
    public static final int QUEUE_DOT = 1;
    /**
     * The constant QUEUE_MSG.
     */
    public static final int QUEUE_MSG = 2;
    /**
     * The constant QUEUE_OFFLINE.
     */
    public static final int QUEUE_OFFLINE = 3;

    private Context context;

    /**
     * The constant ALLOWED_MAC_PREFIX.
     */
    public static final String ALLOWED_MAC_PREFIX = "9C:7B:D2";
    /**
     * The constant DENIED_MAC_PREFIX.
     */
    public static final String DENIED_MAC_PREFIX = "9C:7B:D2:01";

    /**
     * F-50 F-120등 protocol version 2의 service uuid
     * BLE에선 해당 UUID를 이용하여 protocol version확인 가능
     */
    private static final UUID ServiceUuidV2 = UUID.fromString("000019F1-0000-1000-8000-00805F9B34FB");
    private static final UUID WriteCharacteristicsUuidV2 = UUID.fromString("00002BA0-0000-1000-8000-00805F9B34FB");
    private static final UUID IndicateCharacteristicsUuidV2 = UUID.fromString("00002BA1-0000-1000-8000-00805F9B34FB");
    /**
     * Notify or Indicate설정을 위해 필요한 descriptor uuid
     */
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothGatt mBluetoothGatt = null;

    private BluetoothGattCharacteristic mWriteGattChacteristic = null;

    private final static int[] mtuLIst = {256, 160, 64, 23};
    private int mtuIndex = 0;
    private int mtu;
    private int mProtocolVer = 0;

    private int writeIndex = 0;
    private boolean writeContinues = false;
    private byte[] writeDataBuffer;
    private ArrayDeque<byte[]> writeQueue;

    private int penStatus = CONN_STATUS_IDLE;

    /**
     * Instantiates a new Btle adt.
     *
     */
    public BTLEAdt()
    {
        initialize();
    }

    /**
     * Sets spp mac address.
     *
     * @param sppMacAddress the spp mac address
     */
    public void setSppMacAddress(String sppMacAddress)
    {
        this.penSppAddress = sppMacAddress;
    }

    private boolean initialize() {
        writeQueue = new ArrayDeque<>();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            NLog.e("Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * Gets pen spp address.
     *
     * @return the pen spp address
     */
    public String getPenSppAddress()
    {
        return  this.penSppAddress;
    }

    @Override
    public void setListener(IPenMsgListener listener) {
        this.listener = listener;
    }

    @Override
    public void setDotListener(IPenDotListener listener) {
        this.dotListener = listener;
    }

    @Override
    public void setOffLineDataListener(IOfflineDataListener listener) {
        this.offlineDataListener = listener;
    }

    @Override
    public IPenMsgListener getListener() {
        return null;
    }

    @Override
    public IOfflineDataListener getOffLineDataListener() {
        return null;
    }

    @Override
    public synchronized void connect(String address) {
	    if (mBluetoothAdapter == null || address == null) {
		    NLog.w("BluetoothAdapter not initialized or unspecified address.");
		    this.responseMsg(new PenMsg(PenMsgType.PEN_CONNECTION_FAILURE));
		    return;
	    }

	    if (penAddress != null)
	    {
		    if (this.penStatus == CONN_STATUS_AUTHORIZED)
		    {
			    responseMsg(new PenMsg(PenMsgType.PEN_ALREADY_CONNECTED));
			    return;
		    }
		    else if (this.penStatus != CONN_STATUS_IDLE)
		    {
			    return;
		    }
	    }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            NLog.w("Device not found.  Unable to connect.");
            this.responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
            return;
        }

        if ( device.getType() != BluetoothDevice.DEVICE_TYPE_LE )
        {
            NLog.w("MacAddress is not Bluetooth LE Type");
            this.responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
            return;
        }

	    if ( this.penStatus != CONN_STATUS_IDLE ) {
		    responseMsg(new PenMsg(PenMsgType.PEN_CONNECTION_FAILURE));
		    return;
	    }

	    this.penAddress = address;
        onConnectionTry();
	    responseMsg(new PenMsg(PenMsgType.PEN_CONNECTION_TRY));

        this.penBtName = device.getName();

        this.watchDog = new Timer();
        this.watchDogTask = new TimerTask() {
            @Override
            public void run() {
                watchDogAlreadyCalled = true;
                NLog.d("Run WatchDot : connect failed");
                responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE) );
                onDisconnected();
                close();
            }
        };

        this.watchDogAlreadyCalled = false;
        this.mBluetoothGatt = device.connectGatt(context, false, mBluetoothGattCallback);
	    try {
		    // schedule이 시작전에 connectGatt가 불려서 Cancel이 되어버리는 경우에 대한 exception처리
		    // 커넥션 이후엔 여기에 문제가 생겨도 지장없음.
		    this.watchDog.schedule(watchDogTask, 3000);  // 3초
	    }
	    catch (Exception e)
	    {
		    e.printStackTrace();
	    }
        NLog.d("Trying to create a new connection.");
    }

    public boolean isConnected()
    {
        return (penStatus == CONN_STATUS_AUTHORIZED || penStatus == CONN_STATUS_ESTABLISHED);
    }

    public String getPenAddress()
    {
        return penAddress;
    }

    public String getPenBtName()
    {
        return penBtName;
    }

    @Override
    public void disconnect()
    {
        if ( mBluetoothAdapter == null || mBluetoothGatt == null )
        {
            return;
        }

        mBluetoothGatt.disconnect();
    }

    @Override
    public void startListen() {

    }

    @Override
    public boolean isAvailableDevice(String mac) throws BLENotSupportedException {
        throw new BLENotSupportedException( "isAvailableDevice( String mac ) is supported from Bluetooth LE !!!" );
    }

    @Override
    public boolean isAvailableDevice( byte[] data)
    {
        int index = 0;
        int size = 0;
        byte flag = 0;
        while(data.length > index)
        {
            size = data[index++];
            // 이상한 케이스가 있어서 예외처리
            if ( data.length <= index )
                return false;
            flag = data[index];
            if ( (flag & 0xFF) == 0xFF )
            {
                ++index;
                byte[] mac = new byte[6];
                System.arraycopy(data, index, mac, 0, 6);
                StringBuilder sb = new StringBuilder(18);
                for (byte b : mac) {
                    if (sb.length() > 0)
                        sb.append(':');
                    sb.append(String.format("%02x", b));
                }
                String strMac = sb.toString().toUpperCase();
                return strMac.startsWith( ALLOWED_MAC_PREFIX ) && !strMac.startsWith( DENIED_MAC_PREFIX );
            }
            else
            {
                index += size;
            }
        }
        return false;
    }

    @Override
    public String getConnectedDevice() {
        NLog.d( "getConnectedDevice status="+penStatus );
        if(penStatus == CONN_STATUS_AUTHORIZED)
            return penAddress;
        else
            return null;
    }

    @Override
    public String getConnectingDevice ()
    {
        NLog.d( "getConnectingDevice status="+penStatus );
        if(penStatus == CONN_STATUS_TRY || penStatus == CONN_STATUS_BINDED|| penStatus == CONN_STATUS_ESTABLISHED)
            return penAddress;
        else
            return null;
    }

    @Override
    public void inputPassword(String password) {
        if ( !isConnected() )
            return;

        mConnectionThread.getPacketProcessor().reqInputPassword(password);
    }

    @Override
    public void reqSetupPassword(String oldPassword, String newPassword) {
        if ( !isConnected() )
            return;

        mConnectionThread.getPacketProcessor().reqSetUpPassword(oldPassword, newPassword);
    }

    @Override
    public void reqSetUpPasswordOff(String oldPassword) throws ProtocolNotSupportedException {
        if ( !isConnected() )
            return;

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetUpPasswordOff( oldPassword);
        else
        {
            NLog.e( "reqSetUpPasswordOff( String oldPassword ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqSetUpPasswordOff( String oldPassword ) is supported from protocol 2.0 !!!" );
        }

    }

    @Override
    public void reqPenStatus() {
        if ( !isConnected() )
            return;

        mConnectionThread.getPacketProcessor().reqPenStatus();
    }

    @Override
    public void reqFwUpgrade(File fwFile, String targetPath) throws ProtocolNotSupportedException {
        if ( !isConnected() )
            return;

        if ( !fwFile.exists() || !fwFile.canRead() )
        {
            responseMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
            return;
        }

        // TODO 현재는 솔찍히 LE에서 1.0에 대한처리는 하지 않음.
        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor)
            ((CommProcessor)mConnectionThread.getPacketProcessor()).reqPenSwUpgrade( fwFile, targetPath);
        else
        {
            NLog.e( "reqFwUpgrade( File fwFile, String targetPath ) is supported from protocol 1.0 !!!" );
            throw new ProtocolNotSupportedException( "reqFwUpgrade( File fwFile, String targetPath ) is supported from protocol 1.0 !!!");
        }
    }

    @Override
    public void reqFwUpgrade2(File fwFile, String fwVersion) throws ProtocolNotSupportedException {
        reqFwUpgrade2(fwFile, fwVersion, true);
    }

    @Override
    public void reqFwUpgrade2(File fwFile, String fwVersion, boolean isCompress) throws ProtocolNotSupportedException {
        if ( !isConnected() )
            return;

        if ( !fwFile.exists() || !fwFile.canRead() )
        {
            responseMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
            return;
        }
        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqPenSwUpgrade( fwFile, fwVersion, isCompress);
        else
        {
            NLog.e( "reqFwUpgrade2( File fwFile, String fwVersion ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqFwUpgrade2( File fwFile, String fwVersion ) is supported from protocol 2.0 !!!" );
        }
    }


    @Override
    public void reqSuspendFwUpgrade() {
        if ( !isConnected() )
            return;

        mConnectionThread.getPacketProcessor().reqSuspendPenSwUpgrade();
    }

    @Override
    public void reqForceCalibrate() {
        if ( !isConnected() )
            return;

        mConnectionThread.getPacketProcessor().reqForceCalibrate();
    }

    @Override
    public void setAllowOfflineData(boolean allow) {
        if ( getProtocolVersion() == 1 )
            allowOffline = allow;
        else {
            if (!isConnected())
                return;

            if (mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
                ((CommProcessor20) mConnectionThread.getPacketProcessor()).reqSetupPenOfflineDataSave(allow);
        }
    }

    @Override
    public void setOfflineDataLocation(String path) {
        OfflineFile.setOfflineFilePath( path );
    }

    @Override
    public void reqAddUsingNote(int sectionId, int ownerId, int[] noteIds) {
        if ( !isConnected() )
            return;
        mConnectionThread.getPacketProcessor().reqAddUsingNote(sectionId, ownerId, noteIds);
    }

    @Override
    public void reqAddUsingNote(int sectionId, int ownerId) {
        if ( !isConnected() )
            return;
        mConnectionThread.getPacketProcessor().reqAddUsingNote(sectionId, ownerId);
    }

    @Override
    public void reqAddUsingNote(int[] sectionId, int[] ownerId) {
        if ( !isConnected() )
            return;
        mConnectionThread.getPacketProcessor().reqAddUsingNote(sectionId, ownerId);
    }

    @Override
    public void reqAddUsingNoteAll() {
        if ( !isConnected() )
            return;
        mConnectionThread.getPacketProcessor().reqAddUsingNoteAll();
    }

    @Override
    public void reqAddUsingNote(ArrayList<UseNoteData> noteList) throws ProtocolNotSupportedException {
        if ( !isConnected() )
            return;

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqAddUsingNote( noteList );
        else
        {
            NLog.e( "reqAddUsingNote ( ArrayList<UseNoteData> noteList )is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqAddUsingNote ( ArrayList<UseNoteData> noteList )is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqOfflineData(int sectionId, int ownerId, int noteId) {
        if ( !isConnected() )
            return;

        mConnectionThread.getPacketProcessor().reqOfflineData(sectionId, ownerId, noteId);
    }

    @Override
    public void reqOfflineData(int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException {
        if ( !isConnected() )
            return;

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqOfflineData( sectionId, ownerId, noteId, pageIds );
        else
        {
            NLog.e( "reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds )is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds )is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqOfflineDataList() {
        if ( !isConnected() || !allowOffline)
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqOfflineDataList();
    }

    @Override
    public void reqOfflineDataList(int sectionId, int ownerId) throws ProtocolNotSupportedException {
        if ( !isConnected() || !allowOffline)
        {
            return;
        }
        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqOfflineDataList(sectionId, ownerId);
        else
        {
            NLog.e( "reqOfflineDataList ( int sectionId, int ownerId ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqOfflineDataList ( int sectionId, int ownerId ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqOfflineDataPageList(int sectionId, int ownerId, int noteId) throws ProtocolNotSupportedException {
        if ( !isConnected() || !allowOffline)
        {
            return;
        }
        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqOfflineDataPageList( sectionId, ownerId, noteId );
        else
        {
            NLog.e( "reqOfflineDataPageList ( int sectionId, int ownerId, int noteId ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqOfflineDataPageList ( int sectionId, int ownerId, int noteId ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void removeOfflineData(int sectionId, int ownerId) throws ProtocolNotSupportedException {
        if ( !isConnected() )
        {
            return;
        }
        // TODO processor 1.0은  LE에서 구현되어있지 않음. 즉 쓸일이 없음
        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor)
            ((CommProcessor)mConnectionThread.getPacketProcessor()).reqOfflineDataRemove( sectionId, ownerId );
        else
        {
            NLog.e( "removeOfflineData( int sectionId, int ownerId ) is supported from protocol 1.0 !!!" );
            throw new ProtocolNotSupportedException( "removeOfflineData( int sectionId, int ownerId ) is supported from protocol 1.0 !!!" );
        }
    }

    @Override
    public void removeOfflineData(int sectionId, int ownerId, int[] noteIds) throws ProtocolNotSupportedException {
        if ( !isConnected() )
        {
            return;
        }
        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqOfflineDataRemove( sectionId, ownerId, noteIds );
        else
        {
            NLog.e( "removeOfflineData( int sectionId, int ownerId, int[] noteIds ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "removeOfflineData( int sectionId, int ownerId, int[] noteIds ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqSetupAutoPowerOnOff(boolean setOn) {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqAutoPowerSetupOnOff( setOn );
    }

    @Override
    public void reqSetupPenBeepOnOff(boolean setOn) {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqPenBeepSetup( setOn );
    }

    @Override
    public void reqSetupPenTipColor(int color) {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqSetupPenTipColor( color );
    }

    @Override
    public void reqSetupAutoShutdownTime(short minute) {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqSetAutoShutdownTime( minute );
    }

    @Override
    public void reqSetupPenSensitivity(short level) {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqSetPenSensitivity( level );
    }

    @Override
    public void reqSetupPenCapOff(boolean on) throws ProtocolNotSupportedException {
        if ( !isConnected() )
        {
            return;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetPenCapOnOff( on );
        else
        {
            NLog.e( "reqSetupPenCapOff ( boolean on ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqSetupPenCapOff ( boolean on ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqSetupPenHover(boolean on) {
        if ( !isConnected() )
        {
            return;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetPenHover( on );
        else
            NLog.e( "reqSetupPenHover ( boolean on ) is supported from protocol 2.0 !!!" );

    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public int getProtocolVersion() {
        if (mConnectionThread == null )
            return 0;
        return mProtocolVer;
    }

    @Override
    public int getPenStatus() {
//        return status;
        return penStatus;
    }

    private void onLostConnection()
    {
        NLog.d( "[BTAdt/ConnectThread] onLostConnection mIsRegularDisconnect="+mIsRegularDisconnect );
        if(mIsRegularDisconnect)
            responseMsg( new PenMsg( PenMsgType.PEN_DISCONNECTED ) );
        else
        {
            try
            {
                JSONObject job = new JSONObject()
                        .put( JsonTag.BOOL_REGULAR_DISCONNECT, mIsRegularDisconnect );
                responseMsg( new PenMsg( PenMsgType.PEN_DISCONNECTED ,job) );
            }catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        onDisconnected();
        mIsRegularDisconnect = false;
        this.startListen();
    }

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
                listener.onReceiveMessage( msg );
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
                    dotListener.onReceiveDot(dot.toDot() );
            }
        }
    }

    public class ConnectedThread extends Thread implements IConnectedThread {
        private CommandManager processor;

        private String macAddress;
        private String sppMacAddress;
        private boolean isRunning = false;

        /**
         * Instantiates a new Connected thread.
         *
         * @param protocolVer the protocol ver
         */
        public ConnectedThread(int protocolVer) {
            if ( readQueue == null )
                readQueue = new ArrayBlockingQueue<>(128);

            readQueue.clear();

            macAddress = BTLEAdt.this.penAddress;
            sppMacAddress = BTLEAdt.this.penSppAddress;

            String version = "";
            if (context != null) {
                try {
                    version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
                } catch (Exception e) {
                }
            }

            // 사실 현재는 protocol Version 2만 들어오도록 되어있음
            if (protocolVer == 2)
                processor = new CommProcessor20(this, version);

            allowOffline = true;
            this.isRunning = true;
        }

        public void run() {
            NLog.d("[BTAdt/ConnectedThread] STARTED");
            setName("ConnectionThread");

            if (this.isRunning) {
                this.read();
            }

            onLostConnection();
        }

        private ArrayBlockingQueue<byte[]> readQueue = null;

        private void read() {
            while (this.isRunning) {
                    synchronized (readQueue)
                    {
                        try {
                            byte[] bs = readQueue.take();
                            // thread take release data byte array length 0
                            // 매번 if문 돌리는것 보단 안에서 알아서 걸러지기 때문에 그냥 length 0짜리 array넘김
                            processor.fill(bs, bs.length);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
            }
        }

        void read(byte[] data)
        {
            if ( data == null || data.length == 0 )
                return;

            try {
                readQueue.put(data);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * Stop running.
         */
        public void stopRunning()
        {
            NLog.d( "[BTAdt/ConnectedThread] stopRunning()" );
            if(processor != null)
            {
                if ( processor instanceof CommProcessor20 )
                {
                    ((CommProcessor20)processor).finish();
                }
            }

            try {
                // thread take release data byte array length 0
                // 일반적으론 들어가지 않도록 read함수에서 걸러냄
                readQueue.put(new byte[0]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.isRunning = false;
        }
        /**
         * Write Chacteristic
         * WriteCharacteristic으로 한번에 보낼 수 있는 최대 크기는 20bytes
         * 따라서 20bytes가 넘어갈 경우 나눠 보내야 한다고 한다.
         *
         * TODO write buffer가 쌓여있는 사이에 buffer를 덮어씌울 가능성?
         * TODO 그렇다면 queue같은거에 쌓아두고 처리를 해야하는거 아닌가?
         * 우선은 Queue를 만들어서 처리하도록함
         * @param buffer 전송할 Data buffer
         */
        public void write( byte[] buffer )
        {
            if ( mBluetoothGatt != null && mWriteGattChacteristic != null )
            {
                writeIndex = 0;
                byte[] bytes = new byte[buffer.length];
                System.arraycopy(buffer, 0, bytes, 0, buffer.length);
                writeQueue.add(bytes);

                continuousWrite();
            }
        }

        /**
         * 실제 mtu size에 맞게 나눠 보내기 위한 write함수
         */
        public void continuousWrite()
        {
            if ( mBluetoothGatt != null && mWriteGattChacteristic != null )
            {
                if ( !writeContinues )
                {
//                if ( dataBuffer == null || writeIndex >= dataBuffer.length ) {
                    writeDataBuffer = writeQueue.poll();
                    if ( writeDataBuffer == null )
                        return;
                    writeIndex = 0;
                    writeContinues = true;
                }
                int size = 0;
                int bufferSize = writeDataBuffer.length;
                // mtu transmission data size is mtu - 3 (opcode 1byte + attribute handle 2byte)
                if (writeIndex + mtu - 3 < bufferSize)
                    size = mtu - 3;
                else {
                    size = bufferSize - writeIndex;
                    writeContinues = false;
                }
                byte[] b = new byte[size];
                System.arraycopy(writeDataBuffer, writeIndex, b, 0, size);
                mWriteGattChacteristic.setValue(b);
                boolean ret = mBluetoothGatt.writeCharacteristic(mWriteGattChacteristic);
                writeIndex += size;
                NLog.d("write result : " + ret + ", size check : " + size);
            }
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
            mIsRegularDisconnect = isRegularDisconnect;
            mProtocolVer = 0;

            disconnect();
            stopRunning();
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
         * Gets spp mac address.
         *
         * @return the spp mac address
         */
        public String getSPPMacAddress()
        {
            return sppMacAddress;
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

        @Override
        public boolean getIsEstablished() {
            return penStatus == CONN_STATUS_ESTABLISHED || penStatus ==CONN_STATUS_AUTHORIZED;
        }

        /**
         * 접속후 연결되었을때 호출
         */
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
        public void onCreateOfflineStrokes( OfflineByteData offlineByteData)
        {
            responseOffLineStrokes( offlineByteData );
        }

        public boolean getAllowOffline()
        {
            return allowOffline;
        }
    }

    private Handler mHandler = new Handler( Looper.getMainLooper())
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
                        dotListener.onReceiveDot(dot  );
                }
                break;

                case QUEUE_MSG:
                {
                    PenMsg pmsg = (PenMsg) msg.obj;
                    pmsg.mac_address = penAddress;
                    if(pmsg.penMsgType == PenMsgType.PEN_DISCONNECTED || pmsg.penMsgType == PenMsgType.PEN_CONNECTION_FAILURE)
                    {
                        NLog.d( "[BTAdt/mHandler] PenMsgType.PEN_DISCONNECTED" );
                        penAddress = null;
                    }
                    listener.onReceiveMessage( pmsg );
                }
                break;

                case QUEUE_OFFLINE:
                {
                    OfflineByteData offlineByteData = (OfflineByteData) msg.obj;
                    offlineDataListener.onReceiveOfflineStrokes(offlineByteData.strokes, offlineByteData.sectionId, offlineByteData.ownerId, offlineByteData.noteId);
                }
                break;

            }
        }
    };

    private void responseOffLineStrokes( OfflineByteData offlineByteData )
    {
        if ( offlineDataListener != null )
        {
            if ( USE_QUEUE )
            {
//				Message msg = new Message();
//				msg.obj
                mHandler.obtainMessage( QUEUE_OFFLINE, offlineByteData ).sendToTarget();
            }
            else
            {
                offlineDataListener.onReceiveOfflineStrokes( offlineByteData.strokes, offlineByteData.sectionId, offlineByteData.ownerId, offlineByteData.noteId );
            }
        }
    }

    /**
     * BLE Gatt Callback
     */
    private final BluetoothGattCallback mBluetoothGattCallback= new BluetoothGattCallback() {

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            NLog.d("onConnectionStatusChange status " + status + ", newStatue " + newState);
            super.onConnectionStateChange(gatt, status, newState);
            watchDog.cancel();
            if ( watchDogAlreadyCalled )
            {
                return;
            }
            switch (newState)
            {
                case BluetoothProfile.STATE_CONNECTED:

                    mBluetoothGatt = gatt;
                    onBinded();
                    NLog.d("Connected");
                    mtuIndex = 0;
                    mtu = mtuLIst[mtuIndex];
                    boolean ret = gatt.requestMtu(mtu);
                    NLog.d("mtu test result : " + ret);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    NLog.d("Disconnected");
                    if ( mConnectionThread == null )
                    {
                        NLog.d("Connect failed");
                        responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE) );
                        onDisconnected();
                    }
                    else
                    {
                        mConnectionThread.stopRunning();
                    }
                    close();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // CHECK 이 부분이 언제언제 불리는지 확인할 필요가 있다 잘못하면 매번 생성하는수가 있다.
            // 따라서 필수 체크
            if ( status == BluetoothGatt.GATT_SUCCESS ) {
                BluetoothGattService service = gatt.getService(ServiceUuidV2);
                if ( service != null )
                {
                    mProtocolVer = 2;
                    mConnectionThread = new ConnectedThread(mProtocolVer);
                    mConnectionThread.start();
                    initCharacteristic(mProtocolVer);
                }
                else {
                    NLog.d("cannot find service");
                    disconnect();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            NLog.d("call onCharacteristicRead status : " + status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            NLog.d("call onCharacteristicWrite status : " + status);
            if ( status == BluetoothGatt.GATT_SUCCESS)
            {
                mConnectionThread.continuousWrite();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            NLog.d("call onCharacteristicChanged");
            mConnectionThread.read(characteristic.getValue());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            NLog.d("call onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            NLog.d("call onDescriptorWrite status : " + status);
            NLog.d("found service v2");

//            broadcastUpdate(ACTION_GATT_SERVICES_READY_TO_CONNECT);
            StartConnection();
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            NLog.d("call onREliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            NLog.d("call onReadRemoteRssi");
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            if ( status == BluetoothGatt.GATT_SUCCESS )
            {
                NLog.d("call onMtuChanged");
                gatt.discoverServices();
            }
            else
            {
                // 적용가능한 최대 크기의 MTU찾기
                NLog.d("call onMtuChanged status : " + status + ", mtu : " + mtu);
                if ( mtuIndex >= mtuLIst.length )
                {
                    NLog.d("error request mtu failed");
                }
                BTLEAdt.this.mtu = BTLEAdt.this.mtuLIst[++mtuIndex];
                gatt.requestMtu(BTLEAdt.this.mtu);
            }
        }
    };


    private void onBinded( )
    {
        penStatus = CONN_STATUS_BINDED;
    }

    private void onConnectionEstablished()
    {
        penStatus = CONN_STATUS_ESTABLISHED;
    }

    private void onConnectionAuthorized()
    {
        penStatus = CONN_STATUS_AUTHORIZED;
    }

    private void onConnectionTry()
    {
        penStatus = CONN_STATUS_TRY;
    }

    private void onDisconnected()
    {
        penStatus = CONN_STATUS_IDLE;
    }

    private void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        if ( mConnectionThread != null ) {
            mConnectionThread.stopRunning();
            mConnectionThread = null;
        }
    }

    private void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled)
    {
        if ( mBluetoothAdapter == null || mBluetoothGatt == null )
        {
            NLog.d("BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
        if ( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE )
        {
            // Enabled remote indication
            desc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        }
        else if ( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == BluetoothGattCharacteristic.PROPERTY_NOTIFY)
        {
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        }
        else
        {
            NLog.d("Error : Characteristic is not notify or indicate");
            return;
        }
        mBluetoothGatt.writeDescriptor(desc);
    }

    private void initCharacteristic(int protocolVer) {
        if ( protocolVer == 2 )
        {
            BluetoothGattService service = mBluetoothGatt.getService(ServiceUuidV2);

            mWriteGattChacteristic = service.getCharacteristic(WriteCharacteristicsUuidV2);

            BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(IndicateCharacteristicsUuidV2);

            setCharacteristicIndication(gattCharacteristic, true);
        }
    }

    private void StartConnection()
    {
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
        {
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqPenInfo();
        }
    }
}
