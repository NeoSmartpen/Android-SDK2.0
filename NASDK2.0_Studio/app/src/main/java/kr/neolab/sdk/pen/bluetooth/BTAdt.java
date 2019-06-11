package kr.neolab.sdk.pen.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import kr.neolab.sdk.broadcastreceiver.BTDuplicateRemoveBroadcasterReceiver;
import kr.neolab.sdk.ink.structure.DotType;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.IMetadataListener;
import kr.neolab.sdk.metadata.MetadataCtrl;
import kr.neolab.sdk.metadata.structure.Symbol;
import kr.neolab.sdk.pen.IPenAdt;
import kr.neolab.sdk.pen.bluetooth.cmd.CommandManager;
import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor;
import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor20;
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

/**
 * The type Bt adt.
 *
 * @author CHY
 */
public class BTAdt implements IPenAdt
{
	private static BTAdt myInstance = null;

	private int mProtocolVer = 0;
	/**
	 * The constant COD_20.
	 */
	public static final int COD_20 = 0x01059C;

	/**
	 * The constant NAME_PEN.
	 */
// Name for the SDP record when creating server socket
	public static final String NAME_PEN = "NWP-F";

	/**
	 * The constant ALLOWED_MAC_PREFIX.
	 */
	public static final String ALLOWED_MAC_PREFIX = "9C:7B:D2";
	/**
	 * The constant DENIED_MAC_PREFIX.
	 */
	public static final String DENIED_MAC_PREFIX = "9C:7B:D2:01";

	// Unique UUID for this application
	private static final UUID NeoOne_UUID = UUID.fromString( "00001101-0000-1000-8000-00805F9B34FB" );

	// Member fields
	private final BluetoothAdapter mAdapter;

	private ListenThread mListenThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectionThread;

	private IPenMsgListener listener = null;
	private IPenDotListener dotListener = null;
	private IOfflineDataListener offlineDataListener = null;
	private IMetadataListener metadataListener = null;


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
	 * The constant allowOffline.
	 */
	public static boolean allowOffline = true;

	private static final boolean USE_QUEUE = true;

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

	private boolean mIsRegularDisconnect = false;


	public BTAdt()
	{
		mAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	/**
	 * Gets instance.
	 *
	 * @return the instance
	 */
	public synchronized static BTAdt getInstance()
	{
		if ( myInstance == null )
		{
			if ( myInstance == null )
			{
				myInstance = new BTAdt();
			}
		}

		return myInstance;
	}

	@Override
	public void setContext(Context context) {
		this.context = context;

	}

	@Override
	public Context getContext()
	{
		return this.context;
	}

	@Override
	public int getProtocolVersion ()
	{
		return mProtocolVer;
	}

	@Override
	public int getPenStatus ()
	{
		return status;
	}

	public void setListener( IPenMsgListener listener )
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
	public IPenMsgListener getListener()
	{
		return this.listener;
	}

	@Override
	public IPenDotListener getDotListener ()
	{
		return this.dotListener;
	}

	@Override
	public IOfflineDataListener getOffLineDataListener()
	{
		return this.offlineDataListener;
	}

	@Override
	public IMetadataListener getMetadataListener()
	{
		return this.metadataListener;
	}

	private BluetoothAdapter getBluetoothAdapter()
	{
		return mAdapter;
	}


	@Override
	public synchronized void connect( String address )
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
		BluetoothDevice device = getBluetoothAdapter().getRemoteDevice( address );

        boolean ret;
        try {
            ret = isAvailableDevice( address );
        } catch (BLENotSupportedException e) {
            ret = false;
        }

        if ( !ret )
        {
            NLog.e( "[BTAdt] Your device is not allowed." );

            this.responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
            return;
        }

        if ( status != CONN_STATUS_IDLE )
        {
            this.responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
            return;
        }
        this.penAddress = address;
        onConnectionTry();
        responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_TRY));

        NLog.i( "[BTAdt] connect device : " + address );


        this.penBtName = device.getName();


        mConnectThread = new ConnectThread( device );
        mConnectThread.start();
	}

	/**
	 * Is connected boolean.
	 *
	 * @return the boolean
	 */
	public boolean isConnected()
	{
		return (status == CONN_STATUS_AUTHORIZED || status == CONN_STATUS_ESTABLISHED);
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
	public int getPressSensorType ()
	{
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
	public void createProfile ( String proFileName, byte[] password ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		if ( !isConnected() )
		{
			return;
		}

		if(mConnectionThread.getPacketProcessor().isSupportPenProfile())
		{
			if(proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME)
				throw new ProfileKeyValueLimitException("ProFileName byte length is over limit !! ProFileName byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME  );
			else if(password.length > PenProfile.LIMIT_BYTE_LENGTH_PASSWORD)
				throw new ProfileKeyValueLimitException("Password byte length is over limit !! Password byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PASSWORD  );

			mConnectionThread.getPacketProcessor().createProfile( proFileName, password );
		}
		else
		{
			NLog.e( "createProfile ( String proFileName, String password ) is not supported at this pen firmware version!!" );
			throw new ProtocolNotSupportedException( "createProfile ( String proFileName, String password ) is not supported at this pen firmware version!!");
		}
	}

	@Override
	public void deleteProfile ( String proFileName, byte[] password ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		if ( !isConnected() )
		{
			return;
		}

		if(mConnectionThread.getPacketProcessor().isSupportPenProfile())
		{
			if(proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME)
				throw new ProfileKeyValueLimitException("ProFileName byte length is over limit !! ProFileName byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME  );
			else if(password.length > PenProfile.LIMIT_BYTE_LENGTH_PASSWORD)
				throw new ProfileKeyValueLimitException("Password byte length is over limit !! Password byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PASSWORD  );
			mConnectionThread.getPacketProcessor().deleteProfile( proFileName, password );
		}
		else
		{
			NLog.e( "deleteProfile ( String proFileName, String password ) is not supported at this pen firmware version!!" );
			throw new ProtocolNotSupportedException( "deleteProfile ( String proFileName, String password ) is not supported at this pen firmware version!!");
		}

	}

	@Override
	public void writeProfileValue ( String proFileName, byte[] password, String[] keys, byte[][] data ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		if ( !isConnected() )
		{
			return;
		}

		if(mConnectionThread.getPacketProcessor().isSupportPenProfile())
		{
			if(proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME)
				throw new ProfileKeyValueLimitException("ProFileName byte length is over limit !! ProFileName byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME  );
			else if(password.length > PenProfile.LIMIT_BYTE_LENGTH_PASSWORD)
				throw new ProfileKeyValueLimitException("Password byte length is over limit !! Password byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PASSWORD  );
			else
			{
				for(int i = 0; i < keys.length; i++)
				{
					if(keys[i].getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_KEY)
					{

						throw new ProfileKeyValueLimitException("key("+keys[i]+" ) byte length is over limit !! ProFileName byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME  );
					}
					else
					{
						if(keys[i].equals( PenProfile.KEY_PEN_NAME ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_NAME)
						{
							throw new ProfileKeyValueLimitException("Value byte length of key("+keys[i]+" ) is over limit !! Value byte limit of key("+keys[i]+" ) is "+PenProfile.LIMIT_BYTE_LENGTH_PEN_NAME  );
						}
						else if(keys[i].equals( PenProfile.KEY_PEN_STROKE_THICKNESS_LEVEL ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_STROKE_THICKNESS)
						{
							throw new ProfileKeyValueLimitException("Value byte length of key("+keys[i]+" ) is over limit !! Value byte limit of key("+keys[i]+" ) is "+PenProfile.LIMIT_BYTE_LENGTH_PEN_STROKE_THICKNESS  );
						}
						else if(keys[i].equals( PenProfile.KEY_PEN_COLOR_INDEX ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_COLOR_INDEX)
						{
							throw new ProfileKeyValueLimitException("Value byte length of key("+keys[i]+" ) is over limit !! Value byte limit of key("+keys[i]+" ) is "+PenProfile.LIMIT_BYTE_LENGTH_PEN_STROKE_THICKNESS  );
						}
						else if(keys[i].equals( PenProfile.KEY_PEN_COLOR_AND_HISTORY ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_COLOR_AND_HISTORY)
						{
							throw new ProfileKeyValueLimitException("Value byte length of key("+keys[i]+" ) is over limit !! Value byte limit of key("+keys[i]+" ) is "+PenProfile.LIMIT_BYTE_LENGTH_PEN_COLOR_AND_HISTORY  );
						}
						else if(keys[i].equals( PenProfile.KEY_USER_CALIBRATION ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_USER_CALIBRATION)
						{
							throw new ProfileKeyValueLimitException("Value byte length of key("+keys[i]+" ) is over limit !! Value byte limit of key("+keys[i]+" ) is "+PenProfile.LIMIT_BYTE_LENGTH_USER_CALIBRATION  );
						}
						else if(keys[i].equals( PenProfile.KEY_PEN_BRUSH_TYPE ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_BRUSH_TYPE)
						{
							throw new ProfileKeyValueLimitException("Value byte length of key("+keys[i]+" ) is over limit !! Value byte limit of key("+keys[i]+" ) is "+PenProfile.LIMIT_BYTE_LENGTH_PEN_BRUSH_TYPE  );
						}
						else if(keys[i].equals( PenProfile.KEY_PEN_TIP_TYPE ) && data[i].length > PenProfile.LIMIT_BYTE_LENGTH_PEN_TIP_TYPE)
						{
							throw new ProfileKeyValueLimitException("Value byte length of key("+keys[i]+" ) is over limit !! Value byte limit of key("+keys[i]+" ) is "+PenProfile.LIMIT_BYTE_LENGTH_PEN_TIP_TYPE  );
						}

					}
				}
			}
			mConnectionThread.getPacketProcessor().writeProfileValue( proFileName, password ,keys ,data);
		}
		else
		{
			NLog.e( "writeProfileValue ( String proFileName, String password, String[] keys, byte[][] data ) is not supported at this pen firmware version!!" );
			throw new ProtocolNotSupportedException( "writeProfileValue ( String proFileName, String password, String[] keys, byte[][] data ) is not supported at this pen firmware version!!");
		}

	}

	@Override
	public void readProfileValue ( String proFileName, String[] keys ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		if ( !isConnected() )
		{
			return;
		}

		if(mConnectionThread.getPacketProcessor().isSupportPenProfile())
		{
			if(proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME)
				throw new ProfileKeyValueLimitException("ProFileName byte length is over limit !! ProFileName byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME  );
			else
			{
				for(int i = 0; i < keys.length; i++)
				{
					if(keys[i].getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_KEY)
					{
						throw new ProfileKeyValueLimitException("key("+keys[i]+" ) byte length is over limit !! ProFileName byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME  );
					}
				}
			}
			mConnectionThread.getPacketProcessor().readProfileValue( proFileName, keys);
		}
		else
		{
			NLog.e( "readProfileValue ( String proFileName, String[] keys ) is not supported at this pen firmware version!!" );
			throw new ProtocolNotSupportedException( "readProfileValue ( String proFileName, String[] keys ) is not supported at this pen firmware version!!");
		}

	}

	@Override
	public void deleteProfileValue ( String proFileName, byte[] password, String[] keys ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		if ( !isConnected() )
		{
			return;
		}

		if(mConnectionThread.getPacketProcessor().isSupportPenProfile())
		{
			if(proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME)
				throw new ProfileKeyValueLimitException("ProFileName byte length is over limit !! ProFileName byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME  );
			else if(password.length > PenProfile.LIMIT_BYTE_LENGTH_PASSWORD)
				throw new ProfileKeyValueLimitException("Password byte length is over limit !! Password byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PASSWORD  );
			else
			{
				for(int i = 0; i < keys.length; i++)
				{
					if(keys[i].getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_KEY)
					{
						throw new ProfileKeyValueLimitException("key("+keys[i]+" ) byte length is over limit !! ProFileName byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME  );
					}
				}
			}
			mConnectionThread.getPacketProcessor().deleteProfileValue( proFileName, password,  keys);
		}
		else
		{
			NLog.e( "deleteProfileValue ( String proFileName, String password, String[] keys )is not supported at this pen firmware version!!" );
			throw new ProtocolNotSupportedException( "deleteProfileValue ( String proFileName, String password, String[] keys ) is not supported at this pen firmware version!!");
		}

	}

	@Override
	public void getProfileInfo ( String proFileName ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		if ( !isConnected() )
		{
			return;
		}

		if(mConnectionThread.getPacketProcessor().isSupportPenProfile())
		{
			if(proFileName.getBytes().length > PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME)
				throw new ProfileKeyValueLimitException("ProFileName byte length is over limit !! ProFileName byte limit is "+PenProfile.LIMIT_BYTE_LENGTH_PROFILE_NAME  );
			mConnectionThread.getPacketProcessor().getProfileInfo( proFileName);
		}
		else
		{
			NLog.e( "getProfileInfo ( String proFileName )is not supported at this pen firmware version!!" );
			throw new ProtocolNotSupportedException( "getProfileInfo ( String proFileName ) is not supported at this pen firmware version!!");
		}

	}

	@Override
	public boolean isSupportPenProfile ()
	{
		return mConnectionThread.getPacketProcessor().isSupportPenProfile();
	}

	@Override
	public synchronized void disconnect()
	{
		NLog.i( "[BTAdt] disconnect device" );

		if ( mConnectionThread != null )
		{
			mConnectionThread.unbind(true);
		}
	}


	private synchronized void stopListen()
	{
		NLog.i( "[BTAdt] stop listen" );

		if ( mListenThread != null )
		{
			mListenThread.cancel();
		}

		mListenThread = null;
	}

	/**
	 * Endup.
	 */
	public void endup()
	{
		NLog.i( "[BTAdt] endup" );

		if ( mConnectThread != null )
		{
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if ( mListenThread != null )
		{
			mListenThread.cancel();
			mListenThread = null;
		}
	}

	@Override
	public boolean isAvailableDevice( String mac ) throws BLENotSupportedException
	{
		return mac.startsWith( ALLOWED_MAC_PREFIX ) && !mac.startsWith( DENIED_MAC_PREFIX );
	}

    @Override
    public boolean isAvailableDevice( byte[] mac )
    {
        StringBuilder sb = new StringBuilder(18);
        for (byte b : mac) {
            if (sb.length() > 0)
                sb.append(':');
            sb.append(String.format("%02x", b));
        }
        try {
            return isAvailableDevice(sb.toString().toUpperCase());
        } catch (BLENotSupportedException e) {
            return false;
        }
    }

	@Override
	public String getConnectedDevice()
	{
		NLog.d( "getConnectedDevice status="+status );
		if(status == CONN_STATUS_AUTHORIZED)
			return penAddress;
		else
			return null;
	}

	@Override
	public String getConnectingDevice ()
	{
		NLog.d( "getConnectingDevice status="+status );
		if(status == CONN_STATUS_TRY || status == CONN_STATUS_BINDED|| status == CONN_STATUS_ESTABLISHED)
			return penAddress;
		else
			return null;
	}

	@Override
	public void inputPassword( String password )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqInputPassword( password );
	}

	@Override
	public void reqSetupPassword( String oldPassword, String newPassword )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqSetUpPassword( oldPassword, newPassword );
	}


	public void reqSetUpPasswordOff( String oldPassword ) throws ProtocolNotSupportedException
	{
		if ( !isConnected() )
		{
			return;
		}
		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
			((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetUpPasswordOff( oldPassword);
		else
		{
			NLog.e( "reqSetUpPasswordOff( String oldPassword ) is supported from protocol 2.0 !!!" );
			throw new ProtocolNotSupportedException( "reqSetUpPasswordOff( String oldPassword ) is supported from protocol 2.0 !!!" );

		}
	}



	@Override
	public void reqPenStatus()
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqPenStatus();
	}

	@Override
	public void reqFwUpgrade( File fwFile, String targetPath ) throws ProtocolNotSupportedException
	{
		if ( !isConnected() )
		{
			return;
		}

		if ( fwFile == null || !fwFile.exists() || !fwFile.canRead() )
		{
			responseMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
			return;
		}
		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor)
			((CommProcessor)mConnectionThread.getPacketProcessor()).reqPenSwUpgrade( fwFile, targetPath);
		else
		{
			NLog.e( "reqFwUpgrade( File fwFile, String targetPath ) is supported from protocol 1.0 !!!" );
			throw new ProtocolNotSupportedException( "reqFwUpgrade( File fwFile, String targetPath ) is supported from protocol 1.0 !!!");
		}
	}

	@Override
	public void reqFwUpgrade2( File fwFile, String fwVersion )  throws ProtocolNotSupportedException
	{
		reqFwUpgrade2( fwFile, fwVersion , true);
	}


	@Override
	public void reqFwUpgrade2( File fwFile, String fwVersion , boolean isCompress)  throws ProtocolNotSupportedException
	{
		if ( !isConnected() )
		{
			return;
		}

		if ( fwFile == null || !fwFile.exists() || !fwFile.canRead() )
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
	public void reqSuspendFwUpgrade()
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqSuspendPenSwUpgrade();
	}

	@Override
	public void reqForceCalibrate()
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqForceCalibrate();
	}

	@Override
	public void reqCalibrate2 ( float[] factor )
	{
		if ( !isConnected() )
		{
			return;
		}
		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
			((CommProcessor20)mConnectionThread.getPacketProcessor()).reqCalibrate2( factor);
		else
			((CommProcessor)mConnectionThread.getPacketProcessor()).reqCalibrate2( factor);


	}

	/**
	 * Listen(Accept) Thread
	 *
	 * @author CHY
	 *
	 */
	private class ListenThread extends Thread
	{
		// The local server socket
		private BluetoothServerSocket mmServerSocket = null;

		/**
		 * Instantiates a new Listen thread.
		 */
		public ListenThread()
		{
			NLog.i( "[BTAdt] ListenThread startup" );
			setNewServerSocket();
		}

		/**
		 * Sets new server socket.
		 */
		public void setNewServerSocket()
		{
			BluetoothServerSocket tmp = null;
			try
			{
				if(mProtocolVer == 2)
					tmp = mAdapter.listenUsingRfcommWithServiceRecord( NAME_PEN, NeoOne_UUID );
				else
					tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord( NAME_PEN, NeoOne_UUID );
				NLog.d( "[BTAdt] ListenThread new BT ServerSocket assigned" );
			}
			catch ( IOException e )
			{
				NLog.e( "[BTAdt] ListenThread new BT ServerSocket assign fail", e );
			}

			mmServerSocket = tmp;
		}

		public void run()
		{
			NLog.i( "[BTAdt] ListenThread running" );

			setName( "ListenThread" );

			mAdapter.cancelDiscovery();

			BluetoothSocket socket = null;

			// Listen to the server socket if we're not connected
			while ( mmServerSocket != null )
			{
				try
				{
					NLog.d( "[BTAdt] ListenThread ready to connection" );

					if ( mmServerSocket != null )
					{
						NLog.d( "[BTAdt] Wait new connection" );
						socket = mmServerSocket.accept();
					}
				}
				catch ( IOException e )
				{
					NLog.e( "[BTAdt] ListenThread fail to listen socket", e );
					break;
				}

				if ( socket != null )
				{
					synchronized ( BTAdt.this )
					{
                        boolean ret;
                        try {
                            ret = isAvailableDevice( socket.getRemoteDevice().getAddress() );
                        } catch (BLENotSupportedException e) {
                            ret = false;
                        }
						if ( !ret )
						{
							NLog.e( "[BTAdt] Your device is not allowed." );
							continue;
						}

						NLog.d( "[BTAdt] ListenThread success to listen socket : " + socket.getRemoteDevice().getAddress() );

						bindConnection( socket );

						stopListen();
					}
				}
			}
		}

		/**
		 * Cancel.
		 */
		public void cancel()
		{
			NLog.d( "[BTAdt] ListenThread cancel" );

			try
			{
				if ( mmServerSocket != null )
				{
					mmServerSocket.close();
				}

				mmServerSocket = null;
			}
			catch ( IOException e )
			{
				NLog.e( "[BTAdt] ListenThread fail to close server socket", e );
			}
		}
	}

	/**
	 * Connect Thread
	 *
	 * @author CHY
	 *
	 */
	private class ConnectThread extends Thread
	{
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		/**
		 * Instantiates a new Connect thread.
		 *
		 * @param device the device
		 */
		public ConnectThread( BluetoothDevice device )
		{
			mmDevice = device;

			BluetoothSocket tmp = null;
			if(mmDevice.getBluetoothClass() == null)
				mProtocolVer = 1;
			else if(mmDevice.getBluetoothClass().toString().equals( "51c") || mmDevice.getBluetoothClass().toString().equals( "2510"))
				mProtocolVer = 2;
			else
				mProtocolVer = 1;

			try
			{
				if(mProtocolVer == 2 && Build.VERSION.SDK_INT >= 19)
					tmp = device.createRfcommSocketToServiceRecord( NeoOne_UUID );
				else
					tmp = device.createInsecureRfcommSocketToServiceRecord( NeoOne_UUID );


			}
			catch ( Exception e )
			{
				NLog.e( "[BTAdt/ConnectThread] Socket Type : create() failed", e );
			}

			mmSocket = tmp;
		}

		public void run()
		{
			NLog.d( "[BTAdt/ConnectThread] ConnectThread STARTED" );

			setName( "ConnectThread" );

			// Always cancel discovery because it will slow down a connection
			mAdapter.cancelDiscovery();

			try
			{
				// This is a blocking call and will only return on a successful
				// connection or an exception
				mmSocket.connect();
				NLog.d( "[BTAdt/ConnectThread] success to connect socket" );
			}
			catch ( IOException e )
			{
				NLog.e( "[BTAdt/ConnectThread] fail to connect socket.", e );

				try
				{
					mmSocket.close();
				}
				catch ( IOException e2 )
				{
					NLog.e( "[BTAdt/ConnectThread] fail to close socket", e2 );
				}
				status = CONN_STATUS_IDLE;
				responseMsg( new PenMsg( PenMsgType.PEN_CONNECTION_FAILURE ) );
				if(context != null)
				{
			    	Intent i = new Intent(BTDuplicateRemoveBroadcasterReceiver.ACTION_BT_REQ_CONNECT);
			    	i.putExtra(BTDuplicateRemoveBroadcasterReceiver.EXTRA_BT_CONNECT_PACKAGENAME, context.getPackageName());
			    	context.sendBroadcast(i);
				}

				return;
			}

			// Reset the ConnectThread because we're done
			synchronized ( BTAdt.this )
			{
				mConnectThread = null;
			}

			bindConnection( mmSocket );

			// stop listen
			stopListen();
		}

		/**
		 * Cancel.
		 */
		public void cancel()
		{
			NLog.d( "[BTAdt/ConnectThread] cancel()" );

			try
			{
				mmSocket.close();
			}
			catch ( IOException e )
			{
				NLog.e( "[BTAdt/ConnectThread] fail to close socket", e );
			}
		}
	}

	public void setAllowOfflineData( boolean allow )
	{

		if(mProtocolVer == 1)
			allowOffline = allow;
		else
		{
			if ( !isConnected() )
			{
				return;
			}
			if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
				((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetupPenOfflineDataSave( allow );
		}

	}

	@Override
	public synchronized void setOfflineDataLocation( String path )
	{
		OfflineFile.setOfflineFilePath( path );
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
				listener.onReceiveMessage( penAddress, msg );
			}
		}
	}

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
				offlineDataListener.onReceiveOfflineStrokes( penAddress,  offlineByteData.strokes, offlineByteData.sectionId, offlineByteData.ownerId, offlineByteData.noteId );
			}
		}
	}

	private Stroke curStroke;

	private void responseDot( Fdot dot )
	{
		if( curStroke == null || DotType.isPenActionDown( dot.dotType ) )
		{
			curStroke = new Stroke(dot.sectionId, dot.ownerId, dot.noteId, dot.pageId );
		}

		curStroke.add( dot );

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

		if( DotType.isPenActionUp( dot.dotType ) && metadataListener != null )
		{
			Symbol[] symbols = MetadataCtrl.getInstance().findApplicableSymbols( curStroke );

			if( symbols != null && symbols.length > 0 )
				metadataListener.onSymbolDetected( symbols );
		}
	}

	/**
	 * Connection bind
	 *
	 * @param socket
	 */
	private void bindConnection( BluetoothSocket socket )
	{

//		new Handler(Looper.getMainLooper()).post( new Runnable()
//		{
//			@Override
//			public void run ()
//			{
//				Toast.makeText( context, "BluetoothSocket ProtocolVer=" + mProtocolVer, Toast.LENGTH_LONG ).show();
//			}
//		} );
		NLog.i( "[BTAdt] bindConnection by BluetoothSocket : " + socket.getRemoteDevice().getAddress() + ";mProtocolVer=" + mProtocolVer + ";COD=" + socket.getRemoteDevice().getBluetoothClass().getDeviceClass() + ";getBluetoothClass=" + socket.getRemoteDevice().getBluetoothClass().toString() );

		mConnectionThread = new ConnectedThread(mProtocolVer);

		if ( mConnectionThread.bind( socket ) )
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

	/**
	 * Connection Object
	 *
	 * @author CHY
	 */
	public class ConnectedThread extends Thread implements IConnectedThread
	{
		private BluetoothSocket mmSocket;
		private InputStream mmInStream;
		private OutputStream mmOutStream;

		private CommandManager processor;

		private boolean isRunning = false;

		private String macAddress;

		/**
		 * Instantiates a new Connected thread.
		 *
		 * @param protocolVer the protocol ver
		 */
		public ConnectedThread(int protocolVer)
		{
			String version = "";
			if(context != null)
			{
				try
				{
					version = context.getPackageManager().getPackageInfo( context.getPackageName() , 0).versionName;
				}catch ( Exception e ){}

			}
			if(protocolVer == 2)
				processor = new CommProcessor20( this, version );
			else
				processor = new CommProcessor( this );

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
				else if( processor instanceof CommProcessor )
                {
                    ((CommProcessor)processor).finish();
                }
			}
			this.isRunning = false;
		}

		/**
		 * Bind boolean.
		 *
		 * @param socket the socket
		 * @return the boolean
		 */
		public boolean bind( BluetoothSocket socket )
		{

			mmSocket = socket;
			macAddress = mmSocket.getRemoteDevice().getAddress();

			// Get the BluetoothSocket input and output streams
			try
			{
				mmInStream = socket.getInputStream();
				mmOutStream = socket.getOutputStream();

				this.isRunning = true;
				startConnect();
				return true;
			}
			catch ( IOException e )
			{
				NLog.e( "[BTAdt/ConnectedThread] temporary sockets is not created", e );
			}

			return false;
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
			NLog.d( "[BTAdt/ConnectedThread] unbind() isRegularDisconnect="+isRegularDisconnect );
			mIsRegularDisconnect = isRegularDisconnect;
			mProtocolVer = 0;
			if ( mmSocket != null )
			{
				try
				{
					mmInStream.close();
					mmOutStream.close();
					mmSocket.close();
				}
				catch ( IOException e )
				{
					// TODO Auto-generated catch block
					NLog.e( "[BTAdt/ConnectedThread] socket closing fail at unbind time.", e );
				}
			}
			else
			{
				NLog.d( "[BTAdt/ConnectedThread] socket is null!!" );
			}

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
			Handler handler = new Handler( Looper.getMainLooper());
			NLog.d( "startConnect" );
			handler.postDelayed(  new Runnable()
			{
				@Override
				public void run ()
				{
					if ( processor instanceof CommProcessor20 )
					{
						( (CommProcessor20) processor ).reqPenInfo();
					}

				}
			}, 500 );
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
			NLog.d( "[BTAdt/ConnectedThread] STARTED" );
			setName( "ConnectionThread" );

			if ( this.isRunning )
			{
				this.read();
			}
		}

		/**
		 * Read.
		 */
		public void read()
		{
			byte[] buffer = new byte[512];
			int bytes;

			while ( this.isRunning )
			{
				NLog.d( "[BTAdt/ConnectedThread] read run!!!!!!!!!!!");
				try
				{
					bytes = mmInStream.read( buffer );
					NLog.d( "[BTAdt/ConnectedThread] TimeCheck read bytes="+bytes );

                    StringBuffer buff = new StringBuffer();
					for(int i=0;i < bytes;i++) {
                        byte item = buffer[i];
                        int int_data = (int) ( item & 0xFF );
                        buff.append( Integer.toHexString( int_data ) + ", " );
                    }
                    NLog.d( "[BTAdt/ConnectedThread] read bytes data = "+ buff.toString() );

					if ( bytes > 0 )
					{
						processor.fill( buffer, bytes );
					}
					else if ( bytes == -1 )
					{
						this.stopRunning();
					}
				}
				catch ( IOException e )
				{
					NLog.e( "[BTAdt/ConnectedThread] ConnectedThread read IOException occured.", e );
					this.stopRunning();
					break;
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
			try
			{
				mmOutStream.write( buffer );
				mmOutStream.flush();
			}
			catch ( IOException e )
			{
				NLog.e( "[BTAdt/ConnectedThread] IOException during write.", e );
				this.stopRunning();
			}
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

	/**
	 * The M handler.
	 */
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
						NLog.d( "[BTAdt/mHandler] PenMsgType.PEN_DISCONNECTED" );
						penAddress = null;
					}
					listener.onReceiveMessage(pmsg.sppAddress, pmsg );
				}
					break;

				case QUEUE_OFFLINE:
				{
					OfflineByteData offlineByteData = (OfflineByteData) msg.obj;
					offlineDataListener.onReceiveOfflineStrokes(penAddress, offlineByteData.strokes, offlineByteData.sectionId, offlineByteData.ownerId, offlineByteData.noteId);
				}
				break;

			}
		}
	};

	@Override
	public void reqAddUsingNote( int sectionId, int ownerId, int[] noteIds )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqAddUsingNote( sectionId, ownerId, noteIds );
	}

	@Override
	public void reqAddUsingNote( int sectionId, int ownerId )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqAddUsingNote( sectionId, ownerId );
	}

	@Override
	public void reqAddUsingNote(int[] sectionId, int[] ownerId) {
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqAddUsingNote( sectionId, ownerId );

	}

	@Override
	public void reqAddUsingNoteAll()
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqAddUsingNoteAll();
	}

	@Override
	public void reqAddUsingNote ( ArrayList<UseNoteData> noteList ) throws ProtocolNotSupportedException
	{
		if ( !isConnected() )
		{
			return;
		}
		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
			((CommProcessor20)mConnectionThread.getPacketProcessor()).reqAddUsingNote( noteList );
		else
		{
			NLog.e( "reqAddUsingNote ( ArrayList<UseNoteData> noteList )is supported from protocol 2.0 !!!" );
			throw new ProtocolNotSupportedException( "reqAddUsingNote ( ArrayList<UseNoteData> noteList )is supported from protocol 2.0 !!!" );
		}
	}

	@Override
	public void reqOfflineData( int sectionId, int ownerId, int noteId )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqOfflineData( sectionId, ownerId, noteId );
	}

	@Override
	public void reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds ) throws ProtocolNotSupportedException
	{
		if ( !isConnected() )
		{
			return;
		}
		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
			((CommProcessor20)mConnectionThread.getPacketProcessor()).reqOfflineData( sectionId, ownerId, noteId, pageIds );
		else
		{
			NLog.e( "reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds )is supported from protocol 2.0 !!!" );
			throw new ProtocolNotSupportedException( "reqOfflineData ( int sectionId, int ownerId, int noteId, int[] pageIds )is supported from protocol 2.0 !!!" );
		}

	}

	@Override
	public void reqOfflineDataList()
	{
		if ( !isConnected() || !allowOffline)
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqOfflineDataList();
	}

	@Override
	public void reqOfflineDataList ( int sectionId, int ownerId ) throws ProtocolNotSupportedException
	{
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
	public void reqOfflineDataPageList ( int sectionId, int ownerId, int noteId ) throws ProtocolNotSupportedException
	{
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
	public void removeOfflineData( int sectionId, int ownerId ) throws ProtocolNotSupportedException
	{
		if ( !isConnected() )
		{
			return;
		}
		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor)
			((CommProcessor)mConnectionThread.getPacketProcessor()).reqOfflineDataRemove( sectionId, ownerId );
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
		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
			((CommProcessor20)mConnectionThread.getPacketProcessor()).reqOfflineDataRemove( sectionId, ownerId, noteIds );
		else
		{
			NLog.e( "removeOfflineData( int sectionId, int ownerId, int[] noteIds ) is supported from protocol 2.0 !!!" );
			throw new ProtocolNotSupportedException( "removeOfflineData( int sectionId, int ownerId, int[] noteIds ) is supported from protocol 2.0 !!!" );
		}


	}

	@Override
	public void reqSetupAutoPowerOnOff( boolean setOn )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqAutoPowerSetupOnOff( setOn );
	}

	@Override
	public void reqSetupPenBeepOnOff( boolean setOn )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqPenBeepSetup( setOn );
	}

	@Override
	public void reqSetupPenTipColor( int color )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqSetupPenTipColor( color );
	}

	@Override
	public void reqSetupAutoShutdownTime( short minute )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqSetAutoShutdownTime( minute );
	}

	@Override
	public void reqSetupPenSensitivity( short level )
	{
		if ( !isConnected() )
		{
			return;
		}

		mConnectionThread.getPacketProcessor().reqSetPenSensitivity( level );
	}


	@Override
	public void reqSetupPenSensitivityFSC( short level ) throws ProtocolNotSupportedException
	{
		if ( !isConnected() )
		{
			return;
		}
		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
			((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetPenSensitivityFSC( level );
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

		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
			((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetPenCapOnOff( on );
		else
		{
			NLog.e( "reqSetupPenCapOff ( boolean on ) is supported from protocol 2.0 !!!" );
			throw new ProtocolNotSupportedException( "reqSetupPenCapOff ( boolean on ) is supported from protocol 2.0 !!!" );

		}

	}


	@Override
	public void reqSetupPenHover ( boolean on ) throws ProtocolNotSupportedException
	{
		if ( !isConnected() )
		{
			return;
		}

		if(mConnectionThread.getPacketProcessor() instanceof CommProcessor20)
			((CommProcessor20)mConnectionThread.getPacketProcessor()).reqSetPenHover( on );
		else
		{
			NLog.e( "reqSetupPenHover ( boolean on ) is supported from protocol 2.0 !!!" );
			throw new ProtocolNotSupportedException( "reqSetupPenHover ( boolean on ) is supported from protocol 2.0 !!!" );
		}
	}

	public boolean unpairDevice(String address)
	{
		boolean ret = false;
		BluetoothDevice bluetoothDevice = mAdapter.getRemoteDevice( address );
		try {
			Method method = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
			method.invoke(bluetoothDevice, (Object[]) null);
			ret = true;
		} catch (Exception e) {
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

}
