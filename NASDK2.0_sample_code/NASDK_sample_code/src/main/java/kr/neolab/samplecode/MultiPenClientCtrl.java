package kr.neolab.samplecode;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import kr.neolab.sdk.pen.IMultiPenCtrl;
import kr.neolab.sdk.pen.MultiPenCtrl;
import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsg;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

/**
 * The type Pen client ctrl.
 */
public class MultiPenClientCtrl implements IPenMsgListener
{
    /**
     * The constant myInstance.
     */
    public static MultiPenClientCtrl myInstance;

	private IMultiPenCtrl iPenCtrl;

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
    public static int[] USING_NOTES = new int[] { 301, 302, 303, 28, 50, 101, 102, 103, 201, 202, 203, 600, 601, 602, 603, 605, 606, 607, 608 };

	private Context context;

	private SharedPreferences mPref;

	private boolean isConnected = false;
	private boolean isAuthorized = false;

//	private String curPass = "0000", newPass = "0000";

//	private String mPenFWVersion = null;
//	private String mPenSubName = null;
//	private String mPenName = null;
	private HashMap<String, String> mapPenFWVersion = new HashMap<String, String>();
	private HashMap<String, String> mapPenName = new HashMap<String, String>();
	private HashMap<String, String> mapPenSubName = new HashMap<String, String>();
	private HashMap<String, String> mapPenPassword = new HashMap<String, String>();
	private HashMap<String, String> mapNewPenPassword = new HashMap<String, String>();

	/**
     * Gets current password.
     *
     * @return the current password
     */
    public String getCurrentPassword(String macAddress )
	{
		return mapPenPassword.get( macAddress);
	}

	private MultiPenClientCtrl ( Context context )
	{
		this.context = context;

		iPenCtrl = MultiPenCtrl.getInstance();

		// Specify where to store the offline data. (Unless otherwise specified,
		// is stored in the default external storage)
		// inPath = this.getFilesDir().getAbsolutePath();
		// iPenCtrl.setOfflineDataLocation(inPath);


		// regist callback interface
		iPenCtrl.setListener( this );
//		iPenCtrl.setOffLineDataListener( this );
		mPref = PreferenceManager.getDefaultSharedPreferences( context );
	}


    /**
     * Gets instance.
     *
     * @param context the context
     * @return the instance
     */
    public static synchronized MultiPenClientCtrl getInstance( Context context )
	{
		if ( myInstance == null )
		{
			myInstance = new MultiPenClientCtrl( context );
		}

		return myInstance;
	}

    /**
     * Sets context.
     *
     * @param context the context
     */
    public void setContext(Context context)
	{
		iPenCtrl.setContext( context );
	}

//    /**
//     * Register broadcast bt duplicate.
//     */
//    public void registerBroadcastBTDuplicate()
//	{
//		PenClientCtrl.getInstance( context ).registerBroadcastBTDuplicate();
//	}
//
//    /**
//     * Unregister broadcast bt duplicate.
//     */
//    public void unregisterBroadcastBTDuplicate()
//	{
//		PenClientCtrl.getInstance( context ).unregisterBroadcastBTDuplicate();
//	}

//    /**
//     * Is authorized boolean.
//     *
//     * @return the boolean
//     */
//    public boolean isAuthorized()
//	{
//		return isAuthorized;
//	}

//    /**
//     * Is connected boolean.
//     *
//     * @return the boolean
//     */
//    public boolean isConnected()
//	{
//		return isConnected;
//	}

    /**
     * Connect.
     *
     * @param address the address
     */
    public void connect( String address, boolean isLeMode)
	{
		iPenCtrl.connect( address , isLeMode);
	}

    /**
     * Disconnect.
     */
    public void disconnect(String macAddress)
	{
		iPenCtrl.disconnect(macAddress);
	}

    /**
     * Upgrade pen.
     *
     * @param fwFile the fw file
     */
    public void upgradePen(String macAddress, File fwFile)
	{
		try
		{
			iPenCtrl.upgradePen( macAddress, fwFile );
		}catch (ProtocolNotSupportedException e )
		{

		}
	}

    /**
     * Upgrade pen 2.
     *
     * @param fwFile    the fw file
     * @param fwVersion the fw version
     */
    public void upgradePen2(String macAddress,File fwFile, String fwVersion)
	{
		try
		{
			iPenCtrl.upgradePen2( macAddress, fwFile, fwVersion );
		}catch (ProtocolNotSupportedException e )
		{
		}
	}

	public void upgradePen2(String macAddress,File fwFile, String fwVersion, boolean isCompress)
	{
		try
		{
			iPenCtrl.upgradePen2( macAddress, fwFile, fwVersion , isCompress);
		}catch (ProtocolNotSupportedException e )
		{
		}
	}


	/**
     * Suspend pen upgrade.
     */
    public void suspendPenUpgrade(String macAddress)
	{
		iPenCtrl.suspendPenUpgrade(macAddress);
	}

    /**
     * Input password.
     *
     * @param password the password
     */
    public void inputPassword( String macAddress, String password )
	{
		mapPenPassword.put( macAddress,  password);
		iPenCtrl.inputPassword(macAddress, password );
	}

    /**
     * Req setup password.
     *
     * @param oldPassword the old password
     * @param newPassword the new password
     */
    public void reqSetupPassword(String macAddress, String oldPassword, String newPassword )
	{
		mapNewPenPassword.put(macAddress, newPassword );
		iPenCtrl.reqSetupPassword( macAddress, oldPassword, newPassword );
	}

    /**
     * Req offline data list.
     */
    public void reqOfflineDataList(String macAddress)
	{
		iPenCtrl.reqOfflineDataList(macAddress);
	}

    /**
     * Req pen status.
     */
    public void reqPenStatus(String macAddress)
	{
		iPenCtrl.reqPenStatus(macAddress);
	}

    /**
     * Req setup auto power on off.
     *
     * @param setOn the set on
     */
    public void reqSetupAutoPowerOnOff(String macAddress,boolean setOn)
	{
		iPenCtrl.reqSetupAutoPowerOnOff( macAddress,setOn );
	}

    /**
     * Req setup pen beep on off.
     *
     * @param setOn the set on
     */
    public void reqSetupPenBeepOnOff( String macAddress,boolean setOn )
    {
		iPenCtrl.reqSetupPenBeepOnOff( macAddress, setOn );
    }

    /**
     * Req setup pen tip color.
     *
     * @param color the color
     */
    public void reqSetupPenTipColor( String macAddress,int color )
	{
		iPenCtrl.reqSetupPenTipColor(macAddress, color );
	}

    /**
     * Req setup auto shutdown time.
     *
     * @param minute the minute
     */
    public void reqSetupAutoShutdownTime(String macAddress, short minute )
	{
		iPenCtrl.reqSetupAutoShutdownTime(macAddress, minute );
	}

    /**
     * Req setup pen sensitivity.
     *
     * @param level the level
     */
    public void reqSetupPenSensitivity(String macAddress, short level )
	{
		iPenCtrl.reqSetupPenSensitivity( macAddress, level );
	}

    /**
     * Req setup pen cap on off.
     *
     * @param on the on
     */
    public void reqSetupPenCapOnOff(String macAddress, boolean on )
	{
		try
		{
			iPenCtrl.reqSetupPenCapOff( macAddress, on );
		}catch (ProtocolNotSupportedException e )
		{
		}
	}

    /**
     * Req setup pen hover.
     *
     * @param on the on
     */
    public void reqSetupPenHover(String macAddress, boolean on )
	{
		try
		{
			iPenCtrl.reqSetupPenHover(macAddress, on );
		}catch (ProtocolNotSupportedException e )
		{
		}
	}

    /**
     * Sets allow offline data.
     *
     * @param on the on
     */
    public void setAllowOfflineData(String macAddress, boolean on )
	{
		iPenCtrl.setAllowOfflineData( macAddress,on );
	}


//	@Override
//	public void onReceiveDot( Dot dot )
//	{
//		NLog.d( "onReceiveDot sectionId=" + dot.sectionId + ",ownerId=" + dot.ownerId + ";noteId=" + dot.noteId + ";pageId=" + dot.pageId + ";x=" + dot.x + ";y=" + dot.y +  ";pressure=" + dot.pressure + ";timestamp=" + dot.timestamp + ";type=" + dot.dotType + ";color=" + dot.color + ";tiltX=" + dot.tiltX + ";tiltY=" + dot.tiltY + ";twist=" + dot.twist + ";penTipType=" + dot.penTipType );
//		sendPenDotByBroadcast( dot );
//	}


//	@Override
//	public void onReceiveOfflineStrokes ( Stroke[] strokes, int sectionId, int ownerId, int noteId )
//	{
//
//		NLog.d( "onReceiveOfflineStrokes strokes="+strokes.length );
//		Intent i = new Intent( Const.Broadcast.ACTION_OFFLINE_STROKES );
//		i.putExtra( Const.Broadcast.EXTRA_OFFLINE_STROKES, strokes );
//
//		context.sendBroadcast( i );
//	}

	@Override
	public void onReceiveMessage( String macAddress, PenMsg penMsg )
	{
		NLog.d( "PenClientCtrl onReceiveMessage penMsg="+penMsg.getPenMsgType()+",getContent:"+penMsg.getContent() );
		switch ( penMsg.penMsgType )
		{
			// Pens when the connection is complete (that is still going through the certification process state)
			case PenMsgType.PEN_CONNECTION_SUCCESS:

				NLog.d( "PenClientCtrl PEN_CONNECTION_SUCCESS getConnectingDevice"+getConnectingDevice() );
				isConnected = true;

				break;

			// Fired when ready to use pen
			case PenMsgType.PEN_AUTHORIZED:
				NLog.d( "PenClientCtrl PEN_AUTHORIZED getConnectDevice"+getConnectDevice() );
				isAuthorized = true;

				JSONObject obj = penMsg.getContentByJSONObject();
				mPref = PreferenceManager.getDefaultSharedPreferences( context );

				SharedPreferences.Editor edit = mPref.edit();

				try
				{
					mapPenPassword.put( macAddress,  obj.getString( JsonTag.STRING_PEN_PASSWORD ));
					String macaddress = obj.getString( JsonTag.STRING_PEN_MAC_ADDRESS);
					edit.putString( Const.Setting.KEY_PASSWORD, getCurrentPassword(macAddress) );
					edit.putString(Const.Setting.KEY_MAC_ADDRESS,  macaddress );
					edit.commit();
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}



				// notify using note
//				iPenCtrl.reqAddUsingNote( USING_SECTION_ID, USING_OWNER_ID, USING_NOTES );
//				iPenCtrl.reqAddUsingNote( USING_SECTION_ID, USING_OWNER_ID );
				iPenCtrl.reqAddUsingNoteAll(macAddress);

				// to request offline data list
				iPenCtrl.reqOfflineDataList(macAddress);
//				iPenCtrl.reqOfflineDataPageList(1,1,1);

				//iPenCtrl.reqOfflineData( 4, 301, 0 );
//				iPenCtrl.reqOfflineData( USING_SECTION_ID, USING_OWNER_ID, 301 );

				break;

			case PenMsgType.PEN_DISCONNECTED:

				isConnected = false;
				isAuthorized = false;

				break;

			case PenMsgType.PASSWORD_REQUEST:
			{
				JSONObject job = penMsg.getContentByJSONObject();

				try
				{
					int count = job.getInt( Const.JsonTag.INT_PASSWORD_RETRY_COUNT );

					NLog.d("password count : " + count);

				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;

			// Pen a response to the status request (whenever a message comes in should be reflected in the app)
			case PenMsgType.PEN_STATUS:
			{
				JSONObject job = penMsg.getContentByJSONObject();

				if ( job == null )
				{
					return;
				}

				NLog.d( job.toString() );

				mPref = PreferenceManager.getDefaultSharedPreferences( context );

				SharedPreferences.Editor editor = mPref.edit();

				try
				{
					String stat_version = job.getString( Const.JsonTag.STRING_PROTOCOL_VERSION );

//					int stat_timezone = job.getInt( Const.JsonTag.INT_TIMEZONE_OFFSET );
					long stat_timetick = job.getLong( Const.JsonTag.LONG_TIMETICK );
					int stat_forcemax = job.getInt( Const.JsonTag.INT_MAX_FORCE );
					int stat_battery = job.getInt( Const.JsonTag.INT_BATTERY_STATUS );
					int stat_usedmem = job.getInt( Const.JsonTag.INT_MEMORY_STATUS );

//					int stat_pencolor = job.getInt( Const.JsonTag.INT_PEN_COLOR );

					boolean stat_autopower = job.getBoolean( Const.JsonTag.BOOL_AUTO_POWER_ON );
					boolean pencap_on =false;
					try
					{
						pencap_on= job.getBoolean( Const.JsonTag.BOOL_PEN_CAP_ON );
					}catch ( Exception e )
					{

					}
//					boolean stat_accel = job.getBoolean( Const.JsonTag.BOOL_ACCELERATION_SENSOR );
					boolean stat_hovermode =false;
					try
					{
						stat_hovermode= job.getBoolean( Const.JsonTag.BOOL_HOVER );
					}catch ( Exception e )
					{
					}
					boolean stat_offlinesave = false;
					try
					{
						stat_offlinesave= job.getBoolean( Const.JsonTag.BOOL_OFFLINE_DATA_SAVE );
					}catch ( Exception e )
					{
					}


					boolean stat_beep = job.getBoolean( Const.JsonTag.BOOL_BEEP );

					int stat_autopower_time = job.getInt( Const.JsonTag.INT_AUTO_POWER_OFF_TIME );
					int stat_sensitivity = job.getInt( Const.JsonTag.INT_PEN_SENSITIVITY );

//					editor.putBoolean( Const.Setting.KEY_ACCELERATION_SENSOR, stat_accel );
					editor.putString( Const.Setting.KEY_AUTO_POWER_OFF_TIME, "" + stat_autopower_time );
					editor.putBoolean( Const.Setting.KEY_AUTO_POWER_ON, stat_autopower );
					editor.putBoolean( Const.Setting.KEY_BEEP, stat_beep );
//					editor.putString( Const.Setting.KEY_PEN_COLOR, ""+stat_pencolor );
					editor.putString( Const.Setting.KEY_SENSITIVITY, ""+stat_sensitivity );
					editor.putBoolean( Const.Setting.KEY_HOVER_MODE, stat_hovermode );
					editor.putBoolean( Const.Setting.KEY_PEN_CAP_ON, pencap_on );
					editor.putBoolean( Const.Setting.KEY_OFFLINE_DATA_SAVE, stat_offlinesave );

					editor.commit();
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}
			}
				break;

			case PenMsgType.PEN_FW_VERSION:
			{
				JSONObject job = penMsg.getContentByJSONObject();

				try
				{
					mapPenFWVersion.put( macAddress, job.getString( JsonTag.STRING_PEN_FW_VERSION ));
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
				try
				{
					mapPenSubName.put( macAddress, job.getString( JsonTag.STRING_SUB_NAME ));
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}

				try
				{
					mapPenName.put( macAddress, job.getString( JsonTag.STRING_DEVICE_NAME ));
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}

				NLog.d( "PEN_FW_VERSION="+job.toString() );
			}
			break;


			// Pen password change success response
			case PenMsgType.PASSWORD_SETUP_SUCCESS:
			{
				mapPenPassword.put( macAddress, mapNewPenPassword.get( macAddress ) );
			}
				break;

			// Pen password change fails, the response
			case PenMsgType.PASSWORD_SETUP_FAILURE:
			{
			}
			break;

			case PenMsgType.PEN_ILLEGAL_PASSWORD_0000:
			{
				Util.showToast( context, "PassWord do not allow 0000 !!!!" );
			}
			break;


			case PenMsgType.OFFLINE_DATA_NOTE_LIST:

				try
				{
					JSONArray list = new JSONArray( penMsg.getContent() );

					for ( int i = 0; i < list.length(); i++ )
					{
						JSONObject jobj = list.getJSONObject( i );

						int sectionId = jobj.getInt( Const.JsonTag.INT_SECTION_ID );
						int ownerId = jobj.getInt( Const.JsonTag.INT_OWNER_ID );
						int noteId = jobj.getInt( Const.JsonTag.INT_NOTE_ID );
						NLog.d( "offline(" + ( i + 1 ) + ") note => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId );
						if(i == 0)iPenCtrl.reqOfflineData(macAddress, sectionId,  ownerId, noteId );
					}

				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}

				break;
		}

		sendPenMsgByBroadcast(macAddress, penMsg );
	}

	private void sendPenMsgByBroadcast( String macAddress, PenMsg penMsg )
	{
		Intent i = new Intent( Const.Broadcast.ACTION_PEN_MESSAGE );

		i.putExtra( Const.Broadcast.PEN_ADDRESS, macAddress );
		i.putExtra( Const.Broadcast.MESSAGE_TYPE, penMsg.getPenMsgType() );
		i.putExtra( Const.Broadcast.CONTENT, penMsg.getContent() );

		context.sendBroadcast( i );
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
    public String getSDKVerions(){
		return this.iPenCtrl.getVersion();
	}

    /**
     * Gets protocol version.
     *
     * @return the protocol version
     */
    public int getProtocolVersion(String macAddress)
	{
		return this.iPenCtrl.getProtocolVersion(macAddress);
	}

	public ArrayList<String> getConnectDevice()
	{
		return this.iPenCtrl.getConnectedDevice();
	}

//	public IPenCtrl getIPenCtrl()
//	{
//		return this.iPenCtrl;
//	}

	public ArrayList<String> getConnectingDevice()
	{
		return this.iPenCtrl.getConnectingDevice();
	}

//	/**
//     * Gets device name.
//     * Notice!! Not Support Protocol 1.0
//     * Protocol 1.0 return null;
//     *
//     * @return the device name
//     */
    public String getDeviceName(String penAddress)
	{
		return mapPenName.get(penAddress);
	}

	public String getSubName(String penAddress)
	{
		return mapPenSubName.get(penAddress);
	}


	public String getPenFWVersion(String penAddress)
	{
		return mapPenFWVersion.get(penAddress);
	}


    /**
     * Confirm whether or not the MAC address to connect
     * If use ble adapter, throws BLENotSupprtedException
     *
     * @param mac the mac
     * @return true if can use, otherwise false
     */
    public boolean isAvailableDevice( String mac) throws BLENotSupportedException {
        return iPenCtrl.isAvailableDevice( mac );
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
    public boolean isAvailableDevice( byte[] mac, boolean isLeMode)
    {
        return iPenCtrl.isAvailableDevice( mac ,isLeMode );
    }

}
