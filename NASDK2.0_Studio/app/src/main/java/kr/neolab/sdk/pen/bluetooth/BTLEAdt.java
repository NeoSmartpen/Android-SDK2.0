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
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.PipedInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import kr.neolab.sdk.ink.structure.DotType;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.IMetadataListener;
import kr.neolab.sdk.metadata.MetadataCtrl;
import kr.neolab.sdk.metadata.structure.Symbol;
import kr.neolab.sdk.pen.IPenAdt;
import kr.neolab.sdk.pen.bluetooth.cmd.CommandManager;
import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor;
import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor20;
import kr.neolab.sdk.pen.bluetooth.lib.OutOfRangeException;
import kr.neolab.sdk.pen.bluetooth.lib.PenProfile;
import kr.neolab.sdk.pen.bluetooth.lib.ProfileKeyValueLimitException;
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

import static kr.neolab.sdk.pen.bluetooth.comm.CommProcessor20.PEN_UP_DOWN_SEPARATE_SUPPORT_PROTOCOL_VERSION;

/**
 * Created by CJY on 2017-01-26.
 * <p>
 * Not Support Protocol Version 1
 * protocol version 1 and protocol version 2 differ greatly in service and characteristic used in BLE
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class BTLEAdt implements IPenAdt
{
    public static enum UUID_VER
    {
        VER_2, VER_5
    }

    private static BTLEAdt instance = null;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static BTLEAdt getInstance ()
    {
        if ( instance == null )
        {
            synchronized ( BTLEAdt.class )
            {
                if ( instance == null ) instance = new BTLEAdt();
            }
        }
        return instance;
    }

    private boolean allowOffline = true;

    private IPenMsgListener listener = null;
    private IPenDotListener dotListener = null;
    private IOfflineDataListener offlineDataListener = null;
    private IMetadataListener metadataListener = null;

    private ConnectedThread mConnectionThread;
    /**
     * The Pen address.
     */
    private String penAddress = null;

//    private String penSppAddress = null;

    /**
     * The Pen bt name.
     */
    public String penBtName = null;

    private static final boolean USE_QUEUE = true;

    private boolean mIsRegularDisconnect = false;
	private boolean mIsWriteSuccessed = true;

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
     * In the BLE, the protocol version can be checked using the corresponding UUID.
     */
    private static final UUID ServiceUuidV2 = UUID.fromString( "000019F1-0000-1000-8000-00805F9B34FB" );
    private static final UUID WriteCharacteristicsUuidV2 = UUID.fromString( "00002BA0-0000-1000-8000-00805F9B34FB" );
    private static final UUID IndicateCharacteristicsUuidV2 = UUID.fromString( "00002BA1-0000-1000-8000-00805F9B34FB" );

    private static final UUID ServiceUuidV5 = UUID.fromString( "4f99f138-9d53-5bfa-9e50-b147491afe68" );
    private static final UUID WriteCharacteristicsUuidV5 = UUID.fromString( "8bc8cc7d-88ca-56b0-af9a-9bf514d0d61a" );
    private static final UUID IndicateCharacteristicsUuidV5 = UUID.fromString( "64cd86b1-2256-5aeb-9f04-2caf6c60ae57" );

    private UUID_VER curr_uuid_ver = UUID_VER.VER_2;

    private short curr_app_type = 0x1101;

    private String curr_req_protocol_ver = PEN_UP_DOWN_SEPARATE_SUPPORT_PROTOCOL_VERSION;

    private Boolean useProfile = true;

    /**
     *Descriptor uuid for setting Notify or Indicate
     */
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString( "00002902-0000-1000-8000-00805F9B34FB" );

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothGatt mBluetoothGatt = null;

    private BluetoothGattCharacteristic mWriteGattChacteristic = null;

    // The firmware team says 160 is the best, but when set to 256, if the MTU is successfully set, it will be faster than setting it to 160
    private final static int[] mtuLIst = { 512, 256, 160, 64, 23 };
    private int mtuIndex = 0;
    private int mtu;
    private int mProtocolVer = 0;

    private int penStatus = CONN_STATUS_IDLE;
    private float[] factor = null;

    /**
     * Instantiates a new Btle adt.
     */
    public BTLEAdt ()
    {
        initialize();
    }

//    /**
//     * Sets spp mac address.
//     *
//     * @param sppMacAddress the spp mac address
//     */
//    public void setSppMacAddress ( String sppMacAddress )
//    {
//        this.penSppAddress = sppMacAddress;
//    }

    private boolean initialize ()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( mBluetoothAdapter == null )
        {
            NLog.e( "Unable to obtain a BluetoothAdapter." );
            return false;
        }
        return true;
    }

    /**
     * Gets pen spp address.
     *
     * @return the pen spp address
     */
    public String getPenSppAddress ()
    {
        return this.penAddress;
    }

    @Override
    public void setListener ( IPenMsgListener listener )
    {
        this.listener = listener;
    }

    @Override
    public void setDotListener ( IPenDotListener listener )
    {
        this.dotListener = listener;
    }

    @Override
    public void setOffLineDataListener ( IOfflineDataListener listener )
    {
        this.offlineDataListener = listener;
    }

    @Override
    public void setMetadataListener( IMetadataListener listener )
    {
        this.metadataListener = listener;
    }

    @Override
    public IPenMsgListener getListener ()
    {
        return this.listener;
    }

    @Override
    public IPenDotListener getDotListener ()
    {
        return this.dotListener;
    }

    @Override
    public IOfflineDataListener getOffLineDataListener ()
    {
        return this.offlineDataListener;
    }

    @Override
    public IMetadataListener getMetadataListener()
    {
        return this.metadataListener;
    }

    @Override
    @Deprecated
    public synchronized void connect(String address)  throws BLENotSupportedException
    {
        throw new BLENotSupportedException( "isAvailableDevice( String mac ) is supported from Bluetooth LE !!!" );
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param sppAddress    The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public synchronized void connect ( final String sppAddress, final String leAddress, UUID_VER uuid_ver, short appType, String reqProtocolVer , boolean useProfile)
    {
        this.useProfile = useProfile;
        curr_req_protocol_ver = reqProtocolVer;
        curr_app_type = appType;
        curr_uuid_ver = uuid_ver;
        if ( mBluetoothAdapter == null || sppAddress == null || leAddress == null)
        {
            NLog.w( "BluetoothAdapter not initialized or unspecified address." );
            PenMsg msg = new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE);
            msg.sppAddress = sppAddress;
            this.responseMsg( msg );

            return;
        }

        if ( penAddress != null )
        {
            if ( this.penStatus == CONN_STATUS_AUTHORIZED )
            {
                PenMsg msg = new PenMsg( PenMsgType.PEN_ALREADY_CONNECTED);
                msg.sppAddress = sppAddress;
                this.responseMsg( msg );
                return;
            }
            else if ( this.penStatus != CONN_STATUS_IDLE )
            {
                return;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice( leAddress );
        if ( device == null )
        {
            NLog.w( "Device not found.  Unable to connect." );
            PenMsg msg = new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE);
            msg.sppAddress = sppAddress;
            this.responseMsg( msg );
            return;
        }

        if ( device.getType() != BluetoothDevice.DEVICE_TYPE_LE
		        && device.getType() != BluetoothDevice.DEVICE_TYPE_UNKNOWN && device.getType() != BluetoothDevice.DEVICE_TYPE_DUAL)
        {
            NLog.w( "MacAddress is not Bluetooth LE Type" );
            PenMsg msg = new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE);
            msg.sppAddress = sppAddress;
            this.responseMsg( msg );
            return;
        }

        if ( this.penStatus != CONN_STATUS_IDLE )
        {
            PenMsg msg = new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE);
            msg.sppAddress = sppAddress;
            this.responseMsg( msg );
            return;
        }

        this.penAddress = sppAddress;
        onConnectionTry();
        responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_TRY ) );

        this.penBtName = device.getName();

        this.watchDog = new Timer();
        this.watchDogTask = new TimerTask()
        {
            @Override
            public void run ()
            {
                watchDogAlreadyCalled = true;
                NLog.d( "Run WatchDot : connect failed" );
                onDisconnected();
                responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
                close();
            }
        };

        this.watchDogAlreadyCalled = false;
        if(Build.VERSION.SDK_INT >= 23)
            this.mBluetoothGatt = device.connectGatt( context, false, mBluetoothGattCallback,BluetoothDevice.TRANSPORT_LE );
        else
            this.mBluetoothGatt = device.connectGatt( context, false, mBluetoothGattCallback );
        try
        {
            this.watchDog.schedule( watchDogTask, 10000 );  // 10seconds
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        NLog.d( "Trying to create a new connection." );
    }

    public boolean isConnected ()
    {
        return ( penStatus == CONN_STATUS_AUTHORIZED || penStatus == CONN_STATUS_ESTABLISHED );
    }

    public String getPenAddress ()
    {
        return penAddress;
    }

    public String getPenBtName ()
    {
        return penBtName;
    }

    @Override
    public int getPressSensorType ()
    {
        if ( !isConnected() )
        {
            return -1;
        }

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            return ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).getPressSensorType();
        else
        {
            return ( (CommProcessor) mConnectionThread.getPacketProcessor() ).getPressSensorType();
        }
    }

    @Override
    public String getConnectDeviceName () throws ProtocolNotSupportedException
    {
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

    @Override
    public String getConnectSubName () throws ProtocolNotSupportedException
    {
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
    public String getReceiveProtocolVer () throws ProtocolNotSupportedException
    {
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
    public String getFirmwareVer () throws ProtocolNotSupportedException
    {
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
    public void createProfile ( String proFileName, byte[] password ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mConnectionThread.getPacketProcessor().isSupportPenProfile() )
        {
            if ( proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME )
                throw new ProfileKeyValueLimitException( "ProFileName byte length is over limit !! ProFileName byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME );
            else if ( password.length > PenProfile.LIMIT_BYTE_LENGTH_PASSWORD )
                throw new ProfileKeyValueLimitException( "Password byte length is over limit !! Password byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PASSWORD );

            mConnectionThread.getPacketProcessor().createProfile( proFileName, password );
        }
        else
        {
            NLog.e( "createProfile ( String proFileName, String password ) is not supported at this pen firmware version!!" );
            throw new ProtocolNotSupportedException( "createProfile ( String proFileName, String password ) is not supported at this pen firmware version!!" );
        }
    }

    @Override
    public void deleteProfile ( String proFileName, byte[] password ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mConnectionThread.getPacketProcessor().isSupportPenProfile() )
        {
            if ( proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME )
                throw new ProfileKeyValueLimitException( "ProFileName byte length is over limit !! ProFileName byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME );
            else if ( password.length > PenProfile.LIMIT_BYTE_LENGTH_PASSWORD )
                throw new ProfileKeyValueLimitException( "Password byte length is over limit !! Password byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PASSWORD );
            mConnectionThread.getPacketProcessor().deleteProfile( proFileName, password );
        }
        else
        {
            NLog.e( "deleteProfile ( String proFileName, String password ) is not supported at this pen firmware version!!" );
            throw new ProtocolNotSupportedException( "deleteProfile ( String proFileName, String password ) is not supported at this pen firmware version!!" );
        }

    }

    @Override
    public void writeProfileValue ( String proFileName, byte[] password, String[] keys, byte[][] data ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mConnectionThread.getPacketProcessor().isSupportPenProfile() )
        {
            if ( proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME )
                throw new ProfileKeyValueLimitException( "ProFileName byte length is over limit !! ProFileName byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME );
            else if ( password.length > PenProfile.LIMIT_BYTE_LENGTH_PASSWORD )
                throw new ProfileKeyValueLimitException( "Password byte length is over limit !! Password byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PASSWORD );
            else
            {
                for ( int i = 0; i < keys.length; i++ )
                {
                    if ( keys[i].getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_KEY )
                    {

                        throw new ProfileKeyValueLimitException( "key(" + keys[i] + " ) byte length is over limit !! ProFileName byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME );
                    }
                    else
                    {
                        if ( keys[i].equals( PenProfile.KEY_PEN_NAME ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_NAME )
                        {
                            throw new ProfileKeyValueLimitException( "Value byte length of key(" + keys[i] + " ) is over limit !! Value byte limit of key(" + keys[i] + " ) is " + PenProfile.LIMIT_BYTE_LENGTH_PEN_NAME );
                        }
                        else if ( keys[i].equals( PenProfile.KEY_PEN_STROKE_THICKNESS_LEVEL ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_STROKE_THICKNESS )
                        {
                            throw new ProfileKeyValueLimitException( "Value byte length of key(" + keys[i] + " ) is over limit !! Value byte limit of key(" + keys[i] + " ) is " + PenProfile.LIMIT_BYTE_LENGTH_PEN_STROKE_THICKNESS );
                        }
                        else if ( keys[i].equals( PenProfile.KEY_PEN_COLOR_INDEX ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_COLOR_INDEX )
                        {
                            throw new ProfileKeyValueLimitException( "Value byte length of key(" + keys[i] + " ) is over limit !! Value byte limit of key(" + keys[i] + " ) is " + PenProfile.LIMIT_BYTE_LENGTH_PEN_STROKE_THICKNESS );
                        }
                        else if ( keys[i].equals( PenProfile.KEY_PEN_COLOR_AND_HISTORY ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_COLOR_AND_HISTORY )
                        {
                            throw new ProfileKeyValueLimitException( "Value byte length of key(" + keys[i] + " ) is over limit !! Value byte limit of key(" + keys[i] + " ) is " + PenProfile.LIMIT_BYTE_LENGTH_PEN_COLOR_AND_HISTORY );
                        }
                        else if ( keys[i].equals( PenProfile.KEY_USER_CALIBRATION ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_USER_CALIBRATION )
                        {
                            throw new ProfileKeyValueLimitException( "Value byte length of key(" + keys[i] + " ) is over limit !! Value byte limit of key(" + keys[i] + " ) is " + PenProfile.LIMIT_BYTE_LENGTH_USER_CALIBRATION );
                        }
                        else if ( keys[i].equals( PenProfile.KEY_PEN_BRUSH_TYPE ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_BRUSH_TYPE )
                        {
                            throw new ProfileKeyValueLimitException( "Value byte length of key(" + keys[i] + " ) is over limit !! Value byte limit of key(" + keys[i] + " ) is " + PenProfile.LIMIT_BYTE_LENGTH_PEN_BRUSH_TYPE );
                        }
                        else if ( keys[i].equals( PenProfile.KEY_PEN_TIP_TYPE ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_TIP_TYPE )
                        {
                            throw new ProfileKeyValueLimitException( "Value byte length of key(" + keys[i] + " ) is over limit !! Value byte limit of key(" + keys[i] + " ) is " + PenProfile.LIMIT_BYTE_LENGTH_PEN_TIP_TYPE );
                        }

                    }
                }
            }
            mConnectionThread.getPacketProcessor().writeProfileValue( proFileName, password, keys, data );
        }
        else
        {
            NLog.e( "writeProfileValue ( String proFileName, String password, String[] keys, byte[][] data ) is not supported at this pen firmware version!!" );
            throw new ProtocolNotSupportedException( "writeProfileValue ( String proFileName, String password, String[] keys, byte[][] data ) is not supported at this pen firmware version!!" );
        }

    }

    @Override
    public void readProfileValue ( String proFileName, String[] keys ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mConnectionThread.getPacketProcessor().isSupportPenProfile() )
        {
            if ( proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME )
                throw new ProfileKeyValueLimitException( "ProFileName byte length is over limit !! ProFileName byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME );
            else
            {
                for ( int i = 0; i < keys.length; i++ )
                {
                    if ( keys[i].getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_KEY )
                    {
                        throw new ProfileKeyValueLimitException( "key(" + keys[i] + " ) byte length is over limit !! ProFileName byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME );
                    }
                }
            }
            mConnectionThread.getPacketProcessor().readProfileValue( proFileName, keys );
        }
        else
        {
            NLog.e( "readProfileValue ( String proFileName, String[] keys ) is not supported at this pen firmware version!!" );
            throw new ProtocolNotSupportedException( "readProfileValue ( String proFileName, String[] keys ) is not supported at this pen firmware version!!" );
        }

    }

    @Override
    public void deleteProfileValue ( String proFileName, byte[] password, String[] keys ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mConnectionThread.getPacketProcessor().isSupportPenProfile() )
        {
            if ( proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME )
                throw new ProfileKeyValueLimitException( "ProFileName byte length is over limit !! ProFileName byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME );
            else if ( password.length > PenProfile.LIMIT_BYTE_LENGTH_PASSWORD )
                throw new ProfileKeyValueLimitException( "Password byte length is over limit !! Password byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PASSWORD );
            else
            {
                for ( int i = 0; i < keys.length; i++ )
                {
                    if ( keys[i].getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_KEY )
                    {
                        throw new ProfileKeyValueLimitException( "key(" + keys[i] + " ) byte length is over limit !! ProFileName byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME );
                    }
                }
            }
            mConnectionThread.getPacketProcessor().deleteProfileValue( proFileName, password, keys );
        }
        else
        {
            NLog.e( "deleteProfileValue ( String proFileName, String password, String[] keys )is not supported at this pen firmware version!!" );
            throw new ProtocolNotSupportedException( "deleteProfileValue ( String proFileName, String password, String[] keys ) is not supported at this pen firmware version!!" );
        }

    }

    @Override
    public void getProfileInfo ( String proFileName ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mConnectionThread.getPacketProcessor().isSupportPenProfile() )
        {
            if ( proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME )
                throw new ProfileKeyValueLimitException( "ProFileName byte length is over limit !! ProFileName byte limit is " + PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME );
            mConnectionThread.getPacketProcessor().getProfileInfo( proFileName );
        }
        else
        {
            NLog.e( "getProfileInfo ( String proFileName )is not supported at this pen firmware version!!" );
            throw new ProtocolNotSupportedException( "getProfileInfo ( String proFileName ) is not supported at this pen firmware version!!" );
        }

    }

    @Override
    public boolean isSupportPenProfile ()
    {
        return mConnectionThread.getPacketProcessor().isSupportPenProfile();
    }

    @Override
    public void disconnect ()
    {
        if ( mBluetoothAdapter == null || mBluetoothGatt == null )
        {
            return;
        }
        mIsRegularDisconnect = true;
        mBluetoothGatt.disconnect();

    }

    @Override
    public boolean isAvailableDevice ( String mac ) throws BLENotSupportedException
    {
        throw new BLENotSupportedException( "isAvailableDevice( String mac ) is supported from Bluetooth LE !!!" );
    }

    @Override
    public boolean isAvailableDevice ( byte[] data )
    {
        int index = 0;
        int size = 0;
        byte flag = 0;
        while ( data.length > index )
        {
            size = data[index++];
            if ( data.length <= index ) return false;
            flag = data[index];
            if ( ( flag & 0xFF ) == 0xFF )
            {
                ++index;
                byte[] mac = new byte[6];
                System.arraycopy( data, index, mac, 0, 6 );
                StringBuilder sb = new StringBuilder( 18 );
                for ( byte b : mac )
                {
                    if ( sb.length() > 0 ) sb.append( ':' );
                    sb.append( String.format( "%02x", b ) );
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
    public String getConnectedDevice ()
    {
        NLog.d( "getConnectedDevice status=" + penStatus );
        if ( penStatus == CONN_STATUS_AUTHORIZED ) return penAddress;
        else return null;
    }

    @Override
    public String getConnectingDevice ()
    {
        NLog.d( "getConnectingDevice status=" + penStatus );
        if ( penStatus == CONN_STATUS_TRY || penStatus == CONN_STATUS_BINDED || penStatus == CONN_STATUS_ESTABLISHED )
            return penAddress;
        else return null;
    }

    @Override
    public void inputPassword ( String password )
    {
        if ( !isConnected() ) return;

        mConnectionThread.getPacketProcessor().reqInputPassword( password );
    }

    @Override
    public void reqSetupPassword ( String oldPassword, String newPassword )
    {
        if ( !isConnected() ) return;

        mConnectionThread.getPacketProcessor().reqSetUpPassword( oldPassword, newPassword );
    }

    @Override
    public void reqSetUpPasswordOff ( String oldPassword ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() ) return;

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqSetUpPasswordOff( oldPassword );
        else
        {
            NLog.e( "reqSetUpPasswordOff( String oldPassword ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqSetUpPasswordOff( String oldPassword ) is supported from protocol 2.0 !!!" );
        }

    }

    @Override
    public void reqPenStatus ()
    {
        if ( !isConnected() ) return;

        mConnectionThread.getPacketProcessor().reqPenStatus();
    }

    @Override
    public void reqFwUpgrade ( File fwFile, String targetPath ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() ) return;

        if ( fwFile == null || !fwFile.exists() || !fwFile.canRead() )
        {
            responseMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
            return;
        }

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor )
            ( (CommProcessor) mConnectionThread.getPacketProcessor() ).reqPenSwUpgrade( fwFile, targetPath );
        else
        {
            NLog.e( "reqFwUpgrade( File fwFile, String targetPath ) is supported from protocol 1.0 !!!" );
            throw new ProtocolNotSupportedException( "reqFwUpgrade( File fwFile, String targetPath ) is supported from protocol 1.0 !!!" );
        }
    }

    @Override
    public void reqFwUpgrade2 ( File fwFile, String fwVersion ) throws ProtocolNotSupportedException
    {
        reqFwUpgrade2( fwFile, fwVersion, false );
    }


    @Override
    public void reqFwUpgrade2 ( File fwFile, String fwVersion, boolean isCompress ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() ) return;

        if ( fwFile == null || !fwFile.exists() || !fwFile.canRead() )
        {
            responseMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
            return;
        }
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqPenSwUpgrade( fwFile, fwVersion);
        else
        {
            NLog.e( "reqFwUpgrade2( File fwFile, String fwVersion ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqFwUpgrade2( File fwFile, String fwVersion ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqSuspendFwUpgrade ()
    {
        if ( !isConnected() ) return;

        mConnectionThread.getPacketProcessor().reqSuspendPenSwUpgrade();
    }

    @Override
    public void reqForceCalibrate ()
    {
        if ( !isConnected() ) return;

        mConnectionThread.getPacketProcessor().reqForceCalibrate();
    }

    @Override
    public void reqCalibrate2 ( float[] factor )
    {
        if ( !isConnected() )
        {
            return;
        }

        this.factor = factor;
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqCalibrate2( factor );
        else
            ((CommProcessor)mConnectionThread.getPacketProcessor()).reqCalibrate2( factor);

    }

    @Override
    public void setAllowOfflineData ( boolean allow )
    {
        if ( getProtocolVersion() == 1 ) allowOffline = allow;
        else
        {
            if ( !isConnected() ) return;

            if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
                ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqSetupPenOfflineDataSave( allow );
        }
    }

    @Override
    public void setOfflineDataLocation ( String path )
    {
        OfflineFile.setOfflineFilePath( path );
    }

    @Override
    public void reqAddUsingNote ( int sectionId, int ownerId, int[] noteIds ) throws OutOfRangeException
    {
        if ( !isConnected() ) return;

        if( mConnectionThread.getPacketProcessor().isSupportCountLimit() && noteIds.length > 128 )
        {
            NLog.e( "reqAddUsingNote ( int sectionId, int ownerId, int[] noteIds ) note must less then 128 from protocol 2.15 !!!" );
            throw new OutOfRangeException( "reqAddUsingNote ( int sectionId, int ownerId, int[] noteIds ) note must less then 128 from protocol 2.15 !!!" );
        }

        mConnectionThread.getPacketProcessor().reqAddUsingNote( sectionId, ownerId, noteIds );
    }

    @Override
    public void reqAddUsingNote ( int sectionId, int ownerId )
    {
        if ( !isConnected() ) return;
        mConnectionThread.getPacketProcessor().reqAddUsingNote( sectionId, ownerId );
    }

    @Override
    public void reqAddUsingNote ( int[] sectionId, int[] ownerId )
    {
        if ( !isConnected() ) return;
        mConnectionThread.getPacketProcessor().reqAddUsingNote( sectionId, ownerId );
    }

    @Override
    public void reqAddUsingNoteAll ()
    {
        if ( !isConnected() ) return;
        mConnectionThread.getPacketProcessor().reqAddUsingNoteAll();
    }

    @Override
    public void reqAddUsingNote ( ArrayList<UseNoteData> noteList ) throws ProtocolNotSupportedException, OutOfRangeException
    {
        if ( !isConnected() ) return;

        if( mConnectionThread.getPacketProcessor().isSupportCountLimit() && noteList.size() > 128 )
        {
            NLog.e( "reqAddUsingNote ( int sectionId, int ownerId, int[] noteIds ) note must less then 128 from protocol 2.15 !!!" );
            throw new OutOfRangeException( "reqAddUsingNote ( int sectionId, int ownerId, int[] noteIds ) note must less then 128 from protocol 2.15 !!!" );
        }

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqAddUsingNote( noteList );
        else
        {
            NLog.e( "reqAddUsingNote ( ArrayList<UseNoteData> noteList )is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqAddUsingNote ( ArrayList<UseNoteData> noteList )is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqOfflineData ( int sectionId, int ownerId, int noteId )
    {
        if ( !isConnected() ) return;

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqOfflineData( sectionId, ownerId, noteId, true );
        else
            mConnectionThread.getPacketProcessor().reqOfflineData( sectionId, ownerId, noteId );
    }

    @Override
    public void reqOfflineData ( int sectionId, int ownerId, int noteId, boolean deleteOnFinished) throws ProtocolNotSupportedException
    {
        if ( !isConnected() ) return;

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqOfflineData( sectionId, ownerId, noteId, deleteOnFinished );
        else
        {
            NLog.e( "reqOfflineData ( int sectionId, int ownerId, int noteId, boolean deleteOnFinished )is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqOfflineData ( int sectionId, int ownerId, int noteId, boolean deleteOnFinished )is supported from protocol 2.0 !!!" );
        }

    }

    @Override
    public void reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds ) throws ProtocolNotSupportedException, OutOfRangeException
    {
        reqOfflineData( sectionId, ownerId, noteId, true, pageIds );
    }

    @Override
    public void reqOfflineData ( int sectionId, int ownerId, int noteId, boolean deleteOnFinished, int[] pageIds ) throws ProtocolNotSupportedException, OutOfRangeException
    {
        if ( !isConnected() ) return;

        if( mConnectionThread.getPacketProcessor().isSupportCountLimit() && pageIds.length > 128 )
        {
            NLog.e( "reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds ) page must less then 128 from protocol 2.15 !!!" );
            throw new OutOfRangeException( "reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds ) page must less then 128 from protocol 2.15 !!!" );
        }

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqOfflineData( sectionId, ownerId, noteId, deleteOnFinished, pageIds );
        else
        {
            NLog.e( "reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds )is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds )is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqOfflineData(Object extra, int sectionId, int ownerId, int noteId) {
        if ( !isConnected() ) return;

        mConnectionThread.getPacketProcessor().reqOfflineData( extra, sectionId, ownerId, noteId );

    }

    @Override
    public void reqOfflineData(Object extra, int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException {
        if ( !isConnected() ) return;

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqOfflineData( extra, sectionId, ownerId, noteId, pageIds );
        else
        {
            NLog.e( "reqOfflineData ( Object extra, int sectionId, int ownerId, int noteId, int[] pageIds )is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqOfflineData ( Object extra, int sectionId, int ownerId, int noteId, int[] pageIds )is supported from protocol 2.0 !!!" );
        }

    }

    @Override
    public void reqOfflineDataList ()
    {
        if ( !isConnected() || !allowOffline )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqOfflineDataList();
    }

    @Override
    public void reqOfflineDataList ( int sectionId, int ownerId ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() || !allowOffline )
        {
            return;
        }
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqOfflineDataList( sectionId, ownerId );
        else
        {
            NLog.e( "reqOfflineDataList ( int sectionId, int ownerId ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqOfflineDataList ( int sectionId, int ownerId ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqOfflineDataPageList ( int sectionId, int ownerId, int noteId ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() || !allowOffline )
        {
            return;
        }
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqOfflineDataPageList( sectionId, ownerId, noteId );
        else
        {
            NLog.e( "reqOfflineDataPageList ( int sectionId, int ownerId, int noteId ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqOfflineDataPageList ( int sectionId, int ownerId, int noteId ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void removeOfflineData ( int sectionId, int ownerId ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() )
        {
            return;
        }
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor )
            ( (CommProcessor) mConnectionThread.getPacketProcessor() ).reqOfflineDataRemove( sectionId, ownerId );
        else
        {
            NLog.e( "removeOfflineData( int sectionId, int ownerId ) is supported from protocol 1.0 !!!" );
            throw new ProtocolNotSupportedException( "removeOfflineData( int sectionId, int ownerId ) is supported from protocol 1.0 !!!" );
        }
    }

    @Override
    public void removeOfflineData ( int sectionId, int ownerId, int[] noteIds ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() )
        {
            return;
        }
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqOfflineDataRemove( sectionId, ownerId, noteIds );
        else
        {
            NLog.e( "removeOfflineData( int sectionId, int ownerId, int[] noteIds ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "removeOfflineData( int sectionId, int ownerId, int[] noteIds ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void removeOfflineDataByPage(int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException {
        if ( !isConnected() )
        {
            return;
        }
        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20) {

            String protoVer = ((CommProcessor20) mConnectionThread.getPacketProcessor()).getReceiveProtocolVer();
            if (Double.parseDouble(protoVer) >= 2.23)
                ((CommProcessor20) mConnectionThread.getPacketProcessor()).reqOfflineDataRemoveByPage(sectionId, ownerId, noteId, pageIds);
            else
            {
                NLog.e( "removeOfflineDataByPage(int sectionId, int ownerId, int noteId, int[] pageIds) is supported from protocol 2.23 !!!" );
                throw new ProtocolNotSupportedException( "removeOfflineDataByPage(int sectionId, int ownerId, int noteId, int[] pageIds) is supported from protocol 2.23 !!!" );
            }

        }else
        {
            NLog.e( "removeOfflineDataByPage(int sectionId, int ownerId, int noteId, int[] pageIds) is supported from protocol 2.23 !!!" );
            throw new ProtocolNotSupportedException( "removeOfflineDataByPage(int sectionId, int ownerId, int noteId, int[] pageIds) is supported from protocol 2.23 !!!" );
        }
    }

    @Override
    public void reqOfflineNoteInfo( int sectionId, int ownerId, int noteId ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() )
        {
            return;
        }
        if(mConnectionThread.getPacketProcessor().isSupportOfflineNoteInfo())
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqOfflineNoteInfo( sectionId, ownerId, noteId );
        else
        {
            NLog.e( "reqOfflineNoteInfo( int sectionId, int ownerId, int noteId ) is supported from protocol 2.16 !!!" );
            throw new ProtocolNotSupportedException( "reqOfflineNoteInfo( int sectionId, int ownerId, int noteId ) is supported from protocol 2.16 !!!" );
        }
    }

    @Override
    public void reqSetupAutoPowerOnOff ( final boolean setOn )
    {
        if ( !isConnected() )
        {
            return;
        }
        mConnectionThread.getPacketProcessor().reqAutoPowerSetupOnOff( setOn );
    }

    @Override
    public void reqSetupPenBeepOnOff ( boolean setOn )
    {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqPenBeepSetup( setOn );
    }

    @Override
    public void reqSetupPenTipColor ( int color )
    {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqSetupPenTipColor( color );
    }

    @Override
    public void reqSetupAutoShutdownTime ( short minute )
    {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqSetAutoShutdownTime( minute );
    }

    @Override
    public void reqSetupPenSensitivity ( short level )
    {
        if ( !isConnected() )
        {
            return;
        }

        mConnectionThread.getPacketProcessor().reqSetPenSensitivity( level );
    }

    @Override
    public void reqSetupPenSensitivityFSC ( short level ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() )
        {
            return;
        }
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqSetPenSensitivityFSC( level );
        else
        {
            NLog.e( "reqSetupPenSensitivityFSC ( boolean on ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqSetupPenSensitivityFSC ( boolean on ) is supported from protocol 2.0 !!!" );

        }
    }

    @Override
    public void reqSetupPenCapOff ( boolean on ) throws ProtocolNotSupportedException
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqSetPenCapOnOff( on );
        else
        {
            NLog.e( "reqSetupPenCapOff ( boolean on ) is supported from protocol 2.0 !!!" );
            throw new ProtocolNotSupportedException( "reqSetupPenCapOff ( boolean on ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqSetupPenHover ( boolean on )
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqSetPenHover( on );
        else NLog.e( "reqSetupPenHover ( boolean on ) is supported from protocol 2.0 !!!" );

    }

    @Override
    public void reqSetupPenDiskReset ()
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 && mConnectionThread.getPacketProcessor().isSupportCountLimit() )
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqSetPenDiskReset();
        else NLog.e( "reqSetupPenDiskReset() is supported from protocol 2.0 !!!" );

    }

    @Override
    public void setContext ( Context context )
    {
        this.context = context;
    }

    @Override
    public Context getContext ()
    {
        return context;
    }

    @Override
    public int getProtocolVersion ()
    {
        if ( mConnectionThread == null ) return 0;
        return mProtocolVer;
    }

    @Override
    public int getPenStatus ()
    {
//        return status;
        return penStatus;
    }

    public void readRemoteRssi()
    {
        if ( !isConnected() )
        {
            return;
        }

        if ( mBluetoothGatt != null )
            mBluetoothGatt.readRemoteRssi();

    }

    private void onLostConnection ()
    {
        NLog.d( "[BTLEAdt/ConnectThread] onLostConnection mIsRegularDisconnect=" + mIsRegularDisconnect );
	    JSONObject job = new JSONObject();
        if ( mIsRegularDisconnect )
        {
	        try
	        {
		        job.put( JsonTag.BOOL_SEND_DATA_FAILED_DISCONNECT, mIsWriteSuccessed );
	        } catch (JSONException e)
	        {
		        e.printStackTrace();
	        }
        }
        else
        {
            try
            {
                job.put( JsonTag.BOOL_REGULAR_DISCONNECT, mIsRegularDisconnect );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }

        onDisconnected();
	    responseMsg( new PenMsg( PenMsgType.PEN_DISCONNECTED, job ) );

        mIsRegularDisconnect = false;
        mIsWriteSuccessed = true;
    }

    private void responseMsg ( PenMsg msg )
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

    private Stroke curStroke;

    private void responseDot ( Fdot dot )
    {
        if ( factor != null ) dot.pressure = (int) factor[dot.pressure];

        if( curStroke == null || DotType.isPenActionDown( dot.dotType ) )
        {
            curStroke = new Stroke(dot.sectionId, dot.ownerId, dot.noteId, dot.pageId );
        }

        if(!DotType.isPenActionHover( dot.dotType ))
            curStroke.add( dot );

        if ( listener != null )
        {
            if ( USE_QUEUE )
            {
                mHandler.obtainMessage( QUEUE_DOT, dot ).sendToTarget();
            }
            else
            {
                if ( dotListener != null ) dotListener.onReceiveDot( penAddress, dot.toDot() );
            }
        }


        if( DotType.isPenActionUp( dot.dotType ) && metadataListener != null )
        {
            Symbol[] symbols = MetadataCtrl.getInstance().findApplicableSymbols( curStroke );

            if( symbols != null && symbols.length > 0 )
                metadataListener.onSymbolDetected( symbols );
        }
    }

    /**
     * The type Connected thread.
     */
    public class ConnectedThread extends Thread implements IConnectedThread
    {
        private CommandManager processor;

	    private WriteCharacteristicThread mWriteCharacteristicThread;

        private String macAddress;
//        private String sppMacAddress;
        private boolean isRunning = false;

        /**
         * Instantiates a new Connected thread.
         *
         * @param protocolVer the protocol ver
         */
        public ConnectedThread ( int protocolVer )
        {
            if ( readQueue == null ) readQueue = new ArrayBlockingQueue<>( 128 );

	        mWriteCharacteristicThread = new WriteCharacteristicThread();
	        mWriteCharacteristicThread.start();

            readQueue.clear();

            macAddress = BTLEAdt.this.penAddress;
//            sppMacAddress = BTLEAdt.this.penSppAddress;

            String version = "";
            if ( context != null )
            {
                try
                {
                    version = context.getPackageManager().getPackageInfo( context.getPackageName(), 0 ).versionName;
                }
                catch ( Exception e )
                {
                }
            }

            if ( protocolVer == 2 ){
                processor = new CommProcessor20( this, version , curr_app_type, curr_req_protocol_ver);
                ((CommProcessor20)processor).setUseProfile(useProfile);
            }

            allowOffline = true;
            this.isRunning = true;
        }

        public void run ()
        {
            NLog.d( "[BTLEAdt/ConnectedThread] STARTED" );
            setName( "ConnectionThread" );

            if ( this.isRunning )
            {
                this.read();
            }

            onLostConnection();
        }

        private ArrayBlockingQueue<byte[]> readQueue = null;
//        private Queue<byte[]> readQueue = null;

        private void read ()
        {
            while ( this.isRunning )
            {
                NLog.d( "[BTLEAdt/ConnectedThread]  read" );
                synchronized ( readQueue )
                {
                    try
                    {
                        byte[] bs = readQueue.take();
                        // thread take release data byte array length 0
                        processor.fill( bs, bs.length );
                    }
                    catch ( InterruptedException e )
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * Read.
         *
         * @param data the data
         */
        void read ( byte[] data )
        {
            if ( data == null || data.length == 0 ) return;

            try
            {
                readQueue.put( data );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        }

        /**
         * Stop running.
         */
        public void stopRunning ()
        {
            NLog.d( "[BTLEAdt/ConnectedThread] stopRunning()" );

	        if (mWriteCharacteristicThread != null)
	        {
		        mWriteCharacteristicThread.stopRunning();
		        mWriteCharacteristicThread = null;
	        }

            if ( processor != null )
            {
                if ( processor instanceof CommProcessor20 )
                {
                    try {
                        ( (CommProcessor20) processor ).finish();
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else if( processor instanceof CommProcessor )
                {
                    ((CommProcessor)processor).finish();
                }
            }

            this.isRunning = false;
            try
            {
                // thread take release data byte array length 0
                readQueue.put( new byte[0] );
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
        }

        /**
         *
         * @param buffer write Data buffer
         */
        public void write ( byte[] buffer )
        {
            if(mWriteCharacteristicThread != null)
	            mWriteCharacteristicThread.write(buffer);
        }

        /**
         * Unbind.
         */
        public void unbind ()
        {
            unbind( false );
        }

        /**
         * Unbind.
         *
         * @param isRegularDisconnect the is regular disconnect
         */
        public void unbind ( boolean isRegularDisconnect )
        {
            mIsRegularDisconnect = isRegularDisconnect;
            mProtocolVer = 0;
            if ( mBluetoothAdapter == null || mBluetoothGatt == null )
            {
                return;
            }

            //2019.10.09 hrlee: set hover mode off when disconnect
            if( processor instanceof CommProcessor20 )
                ( (CommProcessor20) processor ).isHoverMode = false;

            mBluetoothGatt.disconnect();
            stopRunning();
        }

        /**
         * Gets mac address.
         *
         * @return the mac address
         */
        public String getMacAddress ()
        {
            return macAddress;
        }

        /**
         * Gets spp mac address.
         *
         * @return the spp mac address
         */
        public String getSPPMacAddress ()
        {
            return macAddress;
        }

        /**
         * Gets packet processor.
         *
         * @return the packet processor
         */
        public CommandManager getPacketProcessor ()
        {
            return processor;
        }

        @Override
        public boolean getIsEstablished ()
        {
            return penStatus == CONN_STATUS_ESTABLISHED || penStatus == CONN_STATUS_AUTHORIZED ;
        }

        public void onEstablished ()
        {
            onConnectionEstablished();
        }

        /**
         * On authorized.
         */
        public void onAuthorized ()
        {
            onConnectionAuthorized();
        }

        /**
         * On create msg.
         *
         * @param msg the msg
         */
        public void onCreateMsg ( PenMsg msg )
        {
            responseMsg( msg );
        }

        /**
         * On create dot.
         *
         * @param dot the dot
         */
        public void onCreateDot ( Fdot dot )
        {
            responseDot( dot );
        }

        /**
         * On create offline strokes.
         *
         * @param offlineByteData the offline byte data
         */
        public void onCreateOfflineStrokes ( OfflineByteData offlineByteData )
        {
            responseOffLineStrokes( offlineByteData );
        }

        public boolean getAllowOffline ()
        {
            return allowOffline;
        }

        public void releaseWriteThread()
        {
        	mWriteCharacteristicThread.release();
        }
    }

	public class WriteCharacteristicThread extends Thread {
		private boolean isRunning;

		private int writeIndex = 0;
		private boolean writeContinues = false;
		private byte[] writeDataBuffer;
		private ArrayBlockingQueue<byte[]> writeQueue = null;
		private int writeRetryCount;

		public WriteCharacteristicThread() {
			writeQueue = new ArrayBlockingQueue(128);
			isRunning = true;
			writeRetryCount = 0;
		}

		public void run() {
			while (isRunning) {
				continuousWrite();
			}
		}

		public void stopRunning() {
			try {
				writeQueue.put(new byte[0]);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			isRunning = false;
		}


		public boolean write(byte[] buffer) {
			if (mBluetoothGatt != null && mWriteGattChacteristic != null) {
				writeIndex = 0;
				byte[] bytes = new byte[buffer.length];
				System.arraycopy(buffer, 0, bytes, 0, buffer.length);
				writeQueue.add(bytes);
				return true;
			}

			return false;
		}

		public void release() {
			synchronized (this) {
				this.notify();
			}
		}


		private void continuousWrite() {
			if (mBluetoothGatt != null && mWriteGattChacteristic != null) {
				if (!writeContinues) {
//                if ( dataBuffer == null || writeIndex >= dataBuffer.length ) {
					try {
						writeDataBuffer = writeQueue.take();
					} catch (InterruptedException e) {
						e.printStackTrace();
						writeDataBuffer = null;
					}


					if (writeDataBuffer == null || writeDataBuffer.length == 0) return;
					writeIndex = 0;
					writeContinues = true;
				}
				int size = 0;
				int bufferSize = writeDataBuffer.length;
				// mtu transmission data size is mtu - 3 (opcode 1byte + attribute handle 2byte)
				if (writeIndex + mtu - 3 < bufferSize) size = mtu - 3;
				else {
					size = bufferSize - writeIndex;
					writeContinues = false;
				}
				byte[] b = new byte[size];
				System.arraycopy(writeDataBuffer, writeIndex, b, 0, size);
				mWriteGattChacteristic.setValue(b);

				synchronized (this) {

					boolean ret = mBluetoothGatt.writeCharacteristic(mWriteGattChacteristic);
					NLog.d("write result : " + ret + ", size check : " + size+",writeContinues="+writeContinues);

					if (ret) {
						writeIndex += size;
						writeRetryCount = 0;
						try {
							this.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} else // failed
					{
						if (writeRetryCount >= 3)
						{
							// error & disconnect
							mIsWriteSuccessed = false;
							mConnectionThread.unbind(true);
						}
						delay(10);
						++writeRetryCount;
						writeContinues = true;
					}
				}
			}
		}

		private void delay(long time)
		{
			try {
				sleep( time );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}


    private Handler mHandler = new Handler( Looper.getMainLooper() )
    {
        @Override
        public void handleMessage ( Message msg )
        {
            switch ( msg.what )
            {
                case QUEUE_DOT:
                {
                    Fdot dot = (Fdot) msg.obj;
                    dot.mac_address = penAddress;
                    if ( dotListener != null ) dotListener.onReceiveDot( penAddress, dot );
                }
                break;

                case QUEUE_MSG:
                {
                    PenMsg pmsg = (PenMsg) msg.obj;
                    if(pmsg.sppAddress == null || pmsg.sppAddress.length() == 0)
                        pmsg.sppAddress = penAddress;
                    if ( pmsg.penMsgType == PenMsgType.PEN_DISCONNECTED || pmsg.penMsgType == PenMsgType.PEN_CONNECTION_FAILURE )
                    {
                        NLog.d( "[BTLEAdt/mHandler] PenMsgType.PEN_DISCONNECTED or PenMsgType.PEN_CONNECTION_FAILURE" );
                        if(listener != null)
                            listener.onReceiveMessage( pmsg.sppAddress, pmsg );
                        penAddress = null;
                    }
                    else
                    {
                        if(listener != null)
                            listener.onReceiveMessage( pmsg.sppAddress, pmsg );
                    }

                }
                break;

                case QUEUE_OFFLINE:
                {
                    OfflineByteData offlineByteData = (OfflineByteData) msg.obj;

                    MetadataCtrl metadataCtrl = MetadataCtrl.getInstance();
                    ArrayList<Symbol> resultSymbol = new ArrayList<>();
                    if( metadataCtrl != null )
                    {
                        for (  Stroke stroke : offlineByteData.strokes )
                        {
                            Symbol[] symbols = metadataCtrl.findApplicableSymbols( stroke );

                            if( symbols != null && symbols.length > 0 )
                            {
                                for( Symbol symbol : symbols )
                                {
                                    if( !resultSymbol.contains( symbol ) )
                                        resultSymbol.add( symbol );
                                }
                            }
                        }
                    }
                    offlineDataListener.onReceiveOfflineStrokes( offlineByteData.extraData, penAddress, offlineByteData.strokes, offlineByteData.sectionId, offlineByteData.ownerId, offlineByteData.noteId, resultSymbol.toArray(new Symbol[resultSymbol.size()]) );

                }
                break;

            }
        }
    };

    private void responseOffLineStrokes ( OfflineByteData offlineByteData )
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
                MetadataCtrl metadataCtrl = MetadataCtrl.getInstance();
                ArrayList<Symbol> resultSymbol = new ArrayList<>();
                if( metadataCtrl != null )
                {
                    for (  Stroke stroke : offlineByteData.strokes )
                    {
                        Symbol[] symbols = metadataCtrl.findApplicableSymbols( stroke );

                        if( symbols != null && symbols.length > 0 )
                        {
                            for( Symbol symbol : symbols )
                            {
                                if( !resultSymbol.contains( symbol ) )
                                    resultSymbol.add( symbol );
                            }
                        }
                    }
                }

                offlineDataListener.onReceiveOfflineStrokes( offlineByteData.extraData, penAddress, offlineByteData.strokes, offlineByteData.sectionId, offlineByteData.ownerId, offlineByteData.noteId, resultSymbol.toArray(new Symbol[resultSymbol.size()]) );
            }
        }
    }

    /**
     * BLE Gatt Callback
     */
    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback()
    {

        @TargetApi( Build.VERSION_CODES.LOLLIPOP )
        @Override
        public void onConnectionStateChange ( BluetoothGatt gatt, int status, int newState )
        {
            NLog.d( "onConnectionStatusChange status " + status + ", newStatue " + newState );
            super.onConnectionStateChange( gatt, status, newState );
            watchDog.cancel();
            if ( watchDogAlreadyCalled )
            {
                return;
            }
            switch ( newState )
            {
                case BluetoothProfile.STATE_CONNECTED:

                    mBluetoothGatt = gatt;
                    onBinded();
                    NLog.d( "Connected" );
                    mtuIndex = 0;
                    mtu = mtuLIst[mtuIndex];
                    boolean ret = gatt.requestMtu( mtu );
                    NLog.d( "mtu test result : " + ret );
//                    boolean canReadRssi = mBluetoothGatt.readRemoteRssi();
//                    NLog.d( "canReadRemoteRssi : " + canReadRssi );
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    NLog.d( "Disconnected" );
                    boolean discanReadRssi = mBluetoothGatt.readRemoteRssi();
                    NLog.d( "discanReadRemoteRssi : " + discanReadRssi );
//                    if ( mConnectionThread != null )
//                    {
//                        if(mConnectionThread.getIsEstablished())
//                        {
//                            mConnectionThread.stopRunning();
//                        }
//                        else
//                        {
//                            NLog.d( "Connect failed" );
//                            responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
//                            onDisconnected();
//                        }
//                    }
                    if ( mConnectionThread == null )
                    {
                        onDisconnected();
                        responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
                    }
                    close();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered ( BluetoothGatt gatt, int status )
        {
            super.onServicesDiscovered( gatt, status );
            NLog.d( "onServicesDiscovered status="+status );
            if ( status == BluetoothGatt.GATT_SUCCESS )
            {
                BluetoothGattService service;
                if(curr_uuid_ver == UUID_VER.VER_2)
                {
                    service= gatt.getService( ServiceUuidV2 );
                }
                else
                {
                    service= gatt.getService( ServiceUuidV5 );
                }
                if ( service != null )
                {
                    mProtocolVer = 2;
                    mConnectionThread = new ConnectedThread( mProtocolVer);
                    mConnectionThread.start();

                    initCharacteristic( mProtocolVer );
                }
                else
                {
                    NLog.d( "cannot find service" );
                    disconnect();
                }
            }
        }

        @Override
        public void onCharacteristicRead ( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status )
        {
            super.onCharacteristicRead( gatt, characteristic, status );
            NLog.d( "call onCharacteristicRead status : " + status );
        }

        @Override
        public void onCharacteristicWrite ( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status )
        {
            super.onCharacteristicWrite( gatt, characteristic, status );
            NLog.d( "call onCharacteristicWrite status : " + status );
            if ( status == BluetoothGatt.GATT_SUCCESS )
            {
                mConnectionThread.releaseWriteThread();
            }
        }

        @Override
        public void onCharacteristicChanged ( BluetoothGatt gatt, BluetoothGattCharacteristic characteristic )
        {
            super.onCharacteristicChanged( gatt, characteristic );

            NLog.d( "call onCharacteristicChanged" );
            mConnectionThread.read( characteristic.getValue() );
        }

        @Override
        public void onDescriptorRead ( BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status )
        {
            super.onDescriptorRead( gatt, descriptor, status );
            NLog.d( "call onDescriptorRead" );
        }

        @Override
        public void onDescriptorWrite ( BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status )
        {
            super.onDescriptorWrite( gatt, descriptor, status );
            NLog.d( "call onDescriptorWrite status : " + status );
            NLog.d( "found service v2" );

//            broadcastUpdate(ACTION_GATT_SERVICES_READY_TO_CONNECT);
            StartConnection();
        }

        @Override
        public void onReliableWriteCompleted ( BluetoothGatt gatt, int status )
        {
            super.onReliableWriteCompleted( gatt, status );
            NLog.d( "call onReliableWriteCompleted" );
        }

        @Override
        public void onReadRemoteRssi ( BluetoothGatt gatt, int rssi, int status )
        {
            super.onReadRemoteRssi( gatt, rssi, status );
            NLog.d( "call onReadRemoteRssi="+rssi+",status" );
            try {
                JSONObject job = new JSONObject().put( JsonTag.INT_RSSI, rssi );
                PenMsg msg = new PenMsg( PenMsgType.PEN_READ_RSSI,job);
                msg.sppAddress = penAddress;
                responseMsg( msg );
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        @TargetApi( Build.VERSION_CODES.LOLLIPOP )
        @Override
        public void onMtuChanged ( BluetoothGatt gatt, int mtu, int status )
        {
            super.onMtuChanged( gatt, mtu, status );
            if ( status == BluetoothGatt.GATT_SUCCESS )
            {
                // set mtu automatically
                BTLEAdt.this.mtu = mtu;
                NLog.d( "call onMtuChanged" );
                gatt.discoverServices();
            }
            else
            {
                NLog.d( "call onMtuChanged status : " + status + ", mtu : " + mtu );
                if ( mtuIndex >= mtuLIst.length )
                {
                    NLog.d( "error request mtu failed" );
                }
                BTLEAdt.this.mtu = BTLEAdt.this.mtuLIst[++mtuIndex];
                gatt.requestMtu( BTLEAdt.this.mtu );
            }
        }
    };

    private void onBinded ()
    {
        penStatus = CONN_STATUS_BINDED;
    }

    private void onConnectionEstablished ()
    {
        penStatus = CONN_STATUS_ESTABLISHED;
    }

    private void onConnectionAuthorized ()
    {
        penStatus = CONN_STATUS_AUTHORIZED;
    }

    private void onConnectionTry ()
    {
        penStatus = CONN_STATUS_TRY;
    }

    private void onDisconnected ()
    {

        penStatus = CONN_STATUS_IDLE;
    }

    private void close ()
    {
        if ( mConnectionThread != null )
        {
            if(((CommProcessor20) mConnectionThread.getPacketProcessor()) != null && ((CommProcessor20) mConnectionThread.getPacketProcessor()).setTimeCommand != null && ((CommProcessor20) mConnectionThread.getPacketProcessor()).setTimeCommand.isAlive())
            {
                ((CommProcessor20) mConnectionThread.getPacketProcessor()).setTimeCommand.finish();
            }
            mConnectionThread.stopRunning();
            mConnectionThread = null;
        }
        if ( mBluetoothGatt == null )
        {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;

    }

    private void setCharacteristicIndication ( BluetoothGattCharacteristic characteristic, boolean enabled )
    {
        if ( mBluetoothAdapter == null || mBluetoothGatt == null )
        {
            NLog.d( "BluetoothAdapter not initialized" );
            return;
        }

        mBluetoothGatt.setCharacteristicNotification( characteristic, enabled );

        BluetoothGattDescriptor desc = characteristic.getDescriptor( CONFIG_DESCRIPTOR );
        if ( ( characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE ) == BluetoothGattCharacteristic.PROPERTY_INDICATE )
        {
            // Enabled remote indication
            desc.setValue( BluetoothGattDescriptor.ENABLE_INDICATION_VALUE );
        }
        else if ( ( characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY ) == BluetoothGattCharacteristic.PROPERTY_NOTIFY )
        {
            desc.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE );
        }
        else
        {
            NLog.d( "Error : Characteristic is not notify or indicate" );
            return;
        }
        mBluetoothGatt.writeDescriptor( desc );
    }

    private void initCharacteristic ( int protocolVer )
    {
        if ( protocolVer == 2 )
        {
            BluetoothGattService service;
            BluetoothGattCharacteristic gattCharacteristic;
            if(curr_uuid_ver == UUID_VER.VER_2)
            {
                service = mBluetoothGatt.getService( ServiceUuidV2 );
                mWriteGattChacteristic = service.getCharacteristic( WriteCharacteristicsUuidV2 );
                gattCharacteristic = service.getCharacteristic( IndicateCharacteristicsUuidV2 );

            }
            else {
                service = mBluetoothGatt.getService(ServiceUuidV5);
                mWriteGattChacteristic = service.getCharacteristic( WriteCharacteristicsUuidV5 );
                gattCharacteristic = service.getCharacteristic( IndicateCharacteristicsUuidV5 );
            }

            setCharacteristicIndication( gattCharacteristic, true );
        }
    }

    private void StartConnection ()
    {
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
        {
            ( (CommProcessor20) mConnectionThread.getPacketProcessor() ).reqPenInfo();
        }
    }

    public boolean unpairDevice ( String address )
    {
        boolean ret = false;
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice( address );
        try
        {
            Method method = bluetoothDevice.getClass().getMethod( "removeBond", (Class[]) null );
            method.invoke( bluetoothDevice, (Object[]) null );
            ret = true;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            ret = false;
        }
        return ret;
    }


    @Override
    public void reqSetCurrentTime ()
    {
        if ( !isConnected() )
        {
            return;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor)
            ((CommProcessor)mConnectionThread.getPacketProcessor()).reqSetCurrentTime( );
        else
        {
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetCurrentTime( );
        }

    }

    @Override
    public void setPipedInputStream(PipedInputStream pipedInputStream) {
        return;
    }


    @Override
    public short getColorCode() throws ProtocolNotSupportedException{
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 ) {
            if ( !isConnected() )
            {
                return 0;
            }
            return ((CommProcessor20) mConnectionThread.getPacketProcessor()).getColorCode();
        }
        else
        {
            throw new ProtocolNotSupportedException( "getColorCode ( ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public short getProductCode()throws ProtocolNotSupportedException {
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 ) {
            if ( !isConnected() )
            {
                return 0;
            }
            return ((CommProcessor20) mConnectionThread.getPacketProcessor()).getProductCode();

        }
        else
        {
            throw new ProtocolNotSupportedException( "getProductCode ( ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public short getCompanyCode() throws ProtocolNotSupportedException{
        if ( mConnectionThread.getPacketProcessor() instanceof CommProcessor20 )
        {
            if ( !isConnected() )
            {
                return 0;
            }
            return ((CommProcessor20) mConnectionThread.getPacketProcessor()).getCompanyCode();
        }
        else
        {
            throw new ProtocolNotSupportedException( "getCompanyCode ( ) is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void clear() {
        mIsRegularDisconnect = false;

        mProtocolVer = 0;
        penAddress = null;
        penBtName = null;

        if(USE_QUEUE) {
            mHandler.removeMessages(QUEUE_DOT);
            mHandler.removeMessages(QUEUE_MSG);
            mHandler.removeMessages(QUEUE_OFFLINE);

        }

        onDisconnected();
        close();
    }

    @Override
    public boolean isSupportHoverCommand() throws ProtocolNotSupportedException
    {
        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
        {
            if ( !isConnected() )
            {
                return false;
            }
            return ((CommProcessor20)mConnectionThread.getPacketProcessor()).isSupportHoverCommand();
        }
        else
        {
            throw new ProtocolNotSupportedException( "isSupportHoverCommand () is supported from protocol 2.0 !!!" );
        }
    }


    @Override
    public int getConnectPenType () throws ProtocolNotSupportedException
    {
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
        if ( !isConnected() ) {
            return;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20) {
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSystemInfo();
        }
        else {
            throw new ProtocolNotSupportedException( "reqSystemInfo() is supported from protocol 2.0 !!!" );
        }
    }

    @Override
    public void reqSetPerformance(int step) throws ProtocolNotSupportedException {
        if ( !isConnected() ) {
            return;
        }

        if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20) {
            ((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetPerformance(step);
        }
        else {
            throw new ProtocolNotSupportedException( "reqSetPerformance() is supported from protocol 2.0 !!!" );
        }
    }

}