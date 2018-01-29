package kr.neolab.samplecode;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;

import kr.neolab.samplecode.Const.Broadcast;
import kr.neolab.samplecode.Const.JsonTag;
import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.MetadataCtrl;
import kr.neolab.sdk.metadata.structure.Symbol;
import kr.neolab.sdk.pen.offline.OfflineFileParser;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

public class MainActivity extends Activity
//		implements DrawablePage.DrawablePageListener, DrawableView.DrawableViewGestureListener
{
	public static final String TAG = "pensdk.sample";

	public static final int REQ_GPS_EXTERNAL_PERMISSION = 0x1002;

	private static final int REQUEST_CONNECT_DEVICE_SECURE = 4;

	private PenClientCtrl penClientCtrl;
	private MultiPenClientCtrl multiPenClientCtrl;

	private kr.neolab.samplecode.SampleView mSampleView;

	// Notification
	protected Notification.Builder mBuilder;
	protected NotificationManager mNotifyManager;
	protected Notification mNoti;
	
	public InputPasswordDialog inputPassDialog;

	private FwUpdateDialog fwUpdateDialog;

	private int currentSectionId = -1;
	private int currentOwnerId = -1;
	private int currentBookcodeId = -1;
	private int currentPagenumber = -1;
	private int connectionMode = 0;

	private ArrayList<String> connectedList= null;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		setContentView( R.layout.activity_main );

		try
		{
			ViewConfiguration config = ViewConfiguration.get( this );
			Field menuKeyField = ViewConfiguration.class.getDeclaredField( "sHasPermanentMenuKey" );

			if ( menuKeyField != null )
			{
				menuKeyField.setAccessible( true );
				menuKeyField.setBoolean( config, false );
			}
		}
		catch ( Exception ex )
		{
			// Ignore
		}

		mSampleView = new kr.neolab.samplecode.SampleView( this );
		FrameLayout.LayoutParams para = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		( (FrameLayout) findViewById( R.id.sampleview_frame ) ).addView( mSampleView, 0, para );
		
		PendingIntent pendingIntent = PendingIntent.getBroadcast( this, 0, new Intent( "firmware_update" ), PendingIntent.FLAG_UPDATE_CURRENT );
		 
		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new Notification.Builder(getApplicationContext());
		mBuilder.setContentTitle( "Update Pen" );
		mBuilder.setSmallIcon( R.drawable.ic_launcher_n );
		mBuilder.setContentIntent( pendingIntent );   


		chkPermissions ();
		Intent oIntent = new Intent();
		oIntent.setClass( this, NeoSampleService.class );
		startService( oIntent );


		AlertDialog.Builder builder;
		builder = new AlertDialog.Builder( this );
		builder.setSingleChoiceItems( new CharSequence[]{ "Single Connection Mode", "Multi Connection Mode" }, connectionMode, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick ( DialogInterface dialog, int which )
			{
				connectionMode = which;
				if(connectionMode == 0)
				{
					penClientCtrl = PenClientCtrl.getInstance( getApplicationContext() );
					fwUpdateDialog = new FwUpdateDialog( MainActivity.this,penClientCtrl, mNotifyManager, mBuilder);
					Log.d( TAG, "SDK Version " + penClientCtrl.getSDKVerions() );
				}else
				{
					multiPenClientCtrl = MultiPenClientCtrl.getInstance( getApplicationContext() );
					fwUpdateDialog = new FwUpdateDialog( MainActivity.this,multiPenClientCtrl, mNotifyManager, mBuilder);
					Log.d( TAG, "SDK Version " + multiPenClientCtrl.getSDKVerions() );
				}
				dialog.dismiss();
			}
		});
		builder.setCancelable( false );
		builder.create().show();
	}


	private void chkPermissions ()
	{
		if( Build.VERSION.SDK_INT >= 23)
		{
			int gpsPermissionCheck = ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION );
			final int writeExternalPermissionCheck = ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_EXTERNAL_STORAGE );

			if(gpsPermissionCheck == PackageManager.PERMISSION_DENIED || writeExternalPermissionCheck == PackageManager.PERMISSION_DENIED)
			{
				ArrayList<String> permissions = new ArrayList<String>();
				if(gpsPermissionCheck == PackageManager.PERMISSION_DENIED)
					permissions.add( Manifest.permission.ACCESS_FINE_LOCATION );
				if(writeExternalPermissionCheck == PackageManager.PERMISSION_DENIED)
					permissions.add( Manifest.permission.WRITE_EXTERNAL_STORAGE );
				requestPermissions( permissions.toArray( new String[permissions.size()] ), REQ_GPS_EXTERNAL_PERMISSION );
			}
		}
	}

	@Override
	public void onRequestPermissionsResult ( int requestCode, String[] permissions, int[] grantResults )
	{
		if (requestCode == REQ_GPS_EXTERNAL_PERMISSION )
		{
			boolean bGrantedExternal = false;
			boolean bGrantedGPS = false;
			for ( int i = 0; i < permissions.length; i++ )
			{
				if ( permissions[i].equals( Manifest.permission.WRITE_EXTERNAL_STORAGE ) && grantResults[i] == PackageManager.PERMISSION_GRANTED )
				{
					bGrantedExternal = true;
				}
				else if ( permissions[i].equals( Manifest.permission.ACCESS_FINE_LOCATION ) && grantResults[i] == PackageManager.PERMISSION_GRANTED )
				{
					bGrantedGPS = true;
				}
			}

			if ( ( permissions.length == 1 ) && ( bGrantedExternal || bGrantedGPS ) )
			{
				bGrantedExternal = true;
				bGrantedGPS = true;
			}

			if ( !bGrantedExternal || !bGrantedGPS )
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle( "Permission Check" );
				builder.setMessage( "PERMISSION_DENIED" );
				builder.setPositiveButton( "OK", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick ( DialogInterface dialog, int which )
					{
						finish();
					}
				} );
				builder.setCancelable( false );
				builder.create().show();
			}
		}
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		unregisterReceiver( mBroadcastReceiver );

	}

	@Override
	protected void onResume()
	{
		super.onResume();

		IntentFilter filter = new IntentFilter( Broadcast.ACTION_PEN_MESSAGE );
		filter.addAction( Broadcast.ACTION_PEN_DOT );
		filter.addAction( Broadcast.ACTION_OFFLINE_STROKES );
		filter.addAction( Broadcast.ACTION_WRITE_PAGE_CHANGED );
		filter.addAction( Broadcast.ACTION_SYMBOL_ACTION );




		filter.addAction( "firmware_update" );
		
		registerReceiver( mBroadcastReceiver, filter );
		

	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		switch ( requestCode )
		{
			case REQUEST_CONNECT_DEVICE_SECURE:
				// When DeviceListActivity returns with a device to connect
				if ( resultCode == Activity.RESULT_OK )
				{
					String address = null;

					if ( (address = data.getStringExtra( DeviceListActivity.EXTRA_DEVICE_ADDRESS )) != null )
					{
						boolean isLe = data.getBooleanExtra( DeviceListActivity.EXTRA_IS_BLUETOOTH_LE, false);
						if(connectionMode == 0)
						{
							penClientCtrl.setLeMode( isLe );
							penClientCtrl.connect( address );
						}
						else
						{
							multiPenClientCtrl.connect(address, isLe );
						}
					}
				}
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate( R.menu.main, menu );

		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
//        unregisterReceiver( mBTDuplicateRemoveBroadcasterReceiver );
		Intent oIntent = new Intent();
		oIntent.setClass( this, NeoSampleService.class );
		stopService( oIntent );

		if(penClientCtrl != null)
			penClientCtrl.disconnect();
		if(multiPenClientCtrl != null)
		{
			ArrayList<String> penAddressList = multiPenClientCtrl.getConnectDevice();
			for(String address : penAddressList)
				multiPenClientCtrl.disconnect( address );
		}

	}
	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		// Handle presses on the action bar items
		switch ( item.getItemId() )
		{
			case R.id.action_setting:

				if(connectionMode == 0)
				{
					if(penClientCtrl.isAuthorized())
					{
						Intent intent = new Intent( MainActivity.this, SettingActivity.class );
						startActivity( intent);
					}
				}
				else
				{
					connectedList = multiPenClientCtrl.getConnectDevice();
					if(connectedList.size() > 0)
					{
						AlertDialog.Builder builder;
						String[] addresses = connectedList.toArray( new String[connectedList.size()]);
						builder = new AlertDialog.Builder( this );
						builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick ( DialogInterface dialog, int which )
							{
								Intent intent = new Intent( MainActivity.this, SettingActivity.class );
								intent.putExtra("pen_address",  connectedList.get( which ) );
								startActivity( intent);
								dialog.dismiss();
							}
						});
						builder.create().show();
					}

				}
				return true;

			case R.id.action_connect:
				if(connectionMode == 1 || (connectionMode == 0 &&!penClientCtrl.isConnected()))
				{
					startActivityForResult( new Intent( MainActivity.this, DeviceListActivity.class ), 4 );
				}
				return true;

			case R.id.action_disconnect:
				if(connectionMode == 0)
				{
					if ( penClientCtrl.isConnected() )
					{
						penClientCtrl.disconnect();
					}
				}
				else
				{
					connectedList = multiPenClientCtrl.getConnectDevice();
					if(connectedList.size() > 0)
					{
						AlertDialog.Builder builder;
						String[] addresses = connectedList.toArray( new String[connectedList.size()]);
						builder = new AlertDialog.Builder( this );
						builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick ( DialogInterface dialog, int which )
							{
								multiPenClientCtrl.disconnect( connectedList.get( which ) );
								dialog.dismiss();
							}
						});
						builder.create().show();
					}

				}

				return true;

			case R.id.action_offline_list:
				if(connectionMode == 0)
				{
					if ( penClientCtrl.isAuthorized() )
					{
						// to process saved offline data
						penClientCtrl.reqOfflineDataList();
					}
				}
				else
				{
					connectedList = multiPenClientCtrl.getConnectDevice();
					if(connectedList.size() > 0)
					{
						AlertDialog.Builder builder;
						String[] addresses = connectedList.toArray( new String[connectedList.size()]);
						builder = new AlertDialog.Builder( this );
						builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick ( DialogInterface dialog, int which )
							{
								multiPenClientCtrl.reqOfflineDataList( connectedList.get( which ) );
								dialog.dismiss();
							}
						});
						builder.create().show();
					}
				}
				return true;

			case R.id.action_upgrade:
				if(connectionMode == 0)
				{
					if ( penClientCtrl.isAuthorized() )
					{
						fwUpdateDialog.show( penClientCtrl.getConnectDevice() );
					}
				}
				else
				{
					connectedList = multiPenClientCtrl.getConnectDevice();
					if(connectedList.size() > 0)
					{
						AlertDialog.Builder builder;
						String[] addresses = connectedList.toArray( new String[connectedList.size()]);
						builder = new AlertDialog.Builder( this );
						builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick ( DialogInterface dialog, int which )
							{
								fwUpdateDialog.show( connectedList.get( which ) );
								dialog.dismiss();
							}
						});
						builder.create().show();
					}


//					if(penClientCtrl.getProtocolVersion() == 1)
//					{
//						// location of firmware (you should locate file in this directory.)
//						String pathFirmware = getExternalStoragePath() + "/neolab/firmware/NEO1.zip";
//						// To request a firmware upgrade.
//						penClientCtrl.upgradePen( new File( pathFirmware ) );
//					}
//					else
//					{
//						String pathFirmware = getExternalStoragePath() + File.separator + "update_T.02.0003.bin";
//						// To request a firmware upgrade.
//						penClientCtrl.upgradePen2( new File( pathFirmware ), "0.03.0003" );
//					}
				}

				return true;

			case R.id.action_pen_status:
				if(connectionMode == 0)
				{
					if ( penClientCtrl.isAuthorized() )
					{
						penClientCtrl.reqPenStatus();
					}
				}
				else
				{
					connectedList = multiPenClientCtrl.getConnectDevice();
					if(connectedList.size() > 0)
					{
						AlertDialog.Builder builder;
						String[] addresses = connectedList.toArray( new String[connectedList.size()]);
						builder = new AlertDialog.Builder( this );
						builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick ( DialogInterface dialog, int which )
							{
								multiPenClientCtrl.reqPenStatus( connectedList.get( which ) );
								dialog.dismiss();
							}
						});
						builder.create().show();
					}
				}
				return true;

			case R.id.action_profile_test:
				if(penClientCtrl.isAuthorized())
				{
					if(penClientCtrl.isSupportPenProfile())
					{
						startActivity( new Intent( MainActivity.this, ProfileTestActivity.class ) );

					}
					else
					{
						Util.showToast( this, "Firmware of this pen is not support pen profile feature." );
					}
				}

				return true;

			case R.id.action_pen_unpairing:
				if(connectionMode == 0)
				{
					if(penClientCtrl.isAuthorized())
						penClientCtrl.unpairDevice( penClientCtrl.getConnectDevice() ) ;
				}
				else
				{
					connectedList = multiPenClientCtrl.getConnectDevice();
					if(connectedList.size() > 0)
					{
						AlertDialog.Builder builder;
						String[] addresses = connectedList.toArray( new String[connectedList.size()]);
						for(String addr  : addresses)
						{
							penClientCtrl.unpairDevice( addr ) ;
						}
					}
				}
					return true;


			default:
				return super.onOptionsItemSelected( item );
		}
	}

	public String getExternalStoragePath()
	{
		if ( Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED ) )
		{
			return Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		else
		{
			return Environment.MEDIA_UNMOUNTED;
		}
	}

	private void handleDot(String penAddress, Dot dot )
	{

		NLog.d( "penAddress="+penAddress+",handleDot type =" + dot.dotType );
		mSampleView.addDot(penAddress, dot );
	}

	private void handleMsg(String penAddress, int penMsgType, String content )
	{
		Log.d( TAG, "penAddress="+penAddress+",handleMsg : " + penMsgType );

		switch ( penMsgType )
		{
			// Message of the attempt to connect a pen
			case PenMsgType.PEN_CONNECTION_TRY:

				Util.showToast( this, "try to connect." );

				break;

			// Pens when the connection is completed (state certification process is not yet in progress)
			case PenMsgType.PEN_CONNECTION_SUCCESS:

				Util.showToast( this, "connection is successful." );
				break;


			case PenMsgType.PEN_AUTHORIZED:
				// OffLine Data set use
				if(connectionMode == 0)
					penClientCtrl.setAllowOfflineData( true );
				else
					multiPenClientCtrl.setAllowOfflineData(penAddress, true );
				Util.showToast( this, "connection is AUTHORIZED." );
			break;
			// Message when a connection attempt is unsuccessful pen
			case PenMsgType.PEN_CONNECTION_FAILURE:

				Util.showToast( this, "connection has failed." );

				break;

			
			case PenMsgType.PEN_CONNECTION_FAILURE_BTDUPLICATE:
				String connected_Appname = "";
				try
				{
					JSONObject job = new JSONObject( content );

					connected_Appname = job.getString("packageName");
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}

				Util.showToast( this, String.format("The pen is currently connected to %s app. If you want to proceed, please disconnect the pen from %s app.",connected_Appname,connected_Appname));
				break;
				
			// When you are connected and disconnected from the state pen
			case PenMsgType.PEN_DISCONNECTED:

				Util.showToast( this, "connection has been terminated." );
			// Pen transmits the state when the firmware update is processed.
			case PenMsgType.PEN_FW_UPGRADE_STATUS:
			case PenMsgType.PEN_FW_UPGRADE_SUCCESS:
			case PenMsgType.PEN_FW_UPGRADE_FAILURE:
			case PenMsgType.PEN_FW_UPGRADE_SUSPEND:
			{
				if(fwUpdateDialog != null)
					fwUpdateDialog.setMsg(penAddress, penMsgType, content);
			}
				break;


			// Offline Data List response of the pen
			case PenMsgType.OFFLINE_DATA_NOTE_LIST:

				try
				{
					JSONArray list = new JSONArray( content );

					for ( int i = 0; i < list.length(); i++ )
					{
						JSONObject jobj = list.getJSONObject( i );

						int sectionId = jobj.getInt( JsonTag.INT_SECTION_ID );
						int ownerId = jobj.getInt( JsonTag.INT_OWNER_ID );
						int noteId = jobj.getInt( JsonTag.INT_NOTE_ID );
						NLog.d( TAG, "offline(" + ( i + 1 ) + ") note => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId );
					}
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}

				// if you want to get offline data of pen, use this function.
				// you can call this function, after complete download.
				//
				break;

			// Messages for offline data transfer begins
			case PenMsgType.OFFLINE_DATA_SEND_START:

				break;

			// Offline data transfer completion
			case PenMsgType.OFFLINE_DATA_SEND_SUCCESS:
				if(connectionMode == 0)
				{
					if(penClientCtrl.getProtocolVersion() ==1)
						parseOfflineData(penAddress);
				}
				else
				{
					if(multiPenClientCtrl.getProtocolVersion( penAddress) == 1)
						parseOfflineData(penAddress);
				}

				break;

			// Offline data transfer failure
			case PenMsgType.OFFLINE_DATA_SEND_FAILURE:

				break;

			// Progress of the data transfer process offline
			case PenMsgType.OFFLINE_DATA_SEND_STATUS:
			{
				try
				{
					JSONObject job = new JSONObject( content );

					int total = job.getInt( JsonTag.INT_TOTAL_SIZE );
					int received = job.getInt( JsonTag.INT_RECEIVED_SIZE );

					Log.d( TAG, "offline data send status => total : " + total + ", progress : " + received );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;

			// When the file transfer process of the download offline
			case PenMsgType.OFFLINE_DATA_FILE_CREATED:
			{
				try
				{
					JSONObject job = new JSONObject( content );

					int sectionId = job.getInt( JsonTag.INT_SECTION_ID );
					int ownerId = job.getInt( JsonTag.INT_OWNER_ID );
					int noteId = job.getInt( JsonTag.INT_NOTE_ID );
					int pageId = job.getInt( JsonTag.INT_PAGE_ID );

					String filePath = job.getString( JsonTag.STRING_FILE_PATH );

					Log.d( TAG, "offline data file created => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId + ", pageId : " + pageId + " filePath : " + filePath );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;

			// Ask for your password in a message comes when the pen
			case PenMsgType.PASSWORD_REQUEST:
			{
				int retryCount = -1, resetCount = -1;

				try
				{
					JSONObject job = new JSONObject( content );

					retryCount = job.getInt( JsonTag.INT_PASSWORD_RETRY_COUNT );
					resetCount = job.getInt( JsonTag.INT_PASSWORD_RESET_COUNT );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}

				if(inputPassDialog == null)
					inputPassDialog = new InputPasswordDialog( this, this);
				inputPassDialog.show(penAddress);
			}
				break;
			case PenMsgType.PEN_ILLEGAL_PASSWORD_0000:
			{
				if(inputPassDialog == null)
					inputPassDialog = new InputPasswordDialog( this, this );
				inputPassDialog.show(penAddress);
			}
			break;

		}
	}

	public void inputPassword(String penAddress,  String password )
	{
		if(connectionMode == 0)
		{
			penClientCtrl.inputPassword(password);
		}
		else
		{
			multiPenClientCtrl.inputPassword(penAddress, password);
		}
	}

//	private void onUpgrading( int total, int progress )
//	{
//		mBuilder.setContentText( "Sending" ).setProgress( total, progress, false );
//		mNotifyManager.notify( 0, mBuilder.build() );
//	}
//
//	private void onUpgradeFailure( boolean isSuspend )
//	{
//		if ( isSuspend )
//		{
//			mBuilder.setContentText( "file transfer is suspended." ).setProgress( 0, 0, false );
//		}
//		else
//		{
//			mBuilder.setContentText( "file transfer has failed." ).setProgress( 0, 0, false );
//		}
//		mNotifyManager.notify( 0, mBuilder.build() );
//	}
//
//	private void onUpgradeSuccess()
//	{
//		mBuilder.setContentText( "The file transfer is complete." ).setProgress( 0, 0, false );
//		mNotifyManager.notify( 0, mBuilder.build() );
//	}

	private void parseOfflineData(String penAddress)
	{
		// obtain saved offline data file list
		String[] files = OfflineFileParser.getOfflineFiles(penAddress);

		if ( files == null || files.length == 0 )
		{
			return;
		}

		for ( String file : files )
		{
			try
			{
				// create offline file parser instance
				OfflineFileParser parser = new OfflineFileParser( file );

				// parser return array of strokes
				Stroke[] strokes = parser.parse();

				if ( strokes != null )
				{
//					ArrayList<Stroke> strokeList = new ArrayList( Arrays.asList( strokes ));
					mSampleView.addStrokes( penAddress, strokes );
				}

				// delete data file
				parser.delete();
				parser = null;
			}
			catch ( Exception e )
			{
				Log.e( TAG, "parse file exeption occured.", e );
			}
		}
	}

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive( Context context, Intent intent )
		{
			String action = intent.getAction();

			if ( Broadcast.ACTION_PEN_MESSAGE.equals( action ) )
			{
				String penAddress = intent.getStringExtra( Broadcast.PEN_ADDRESS );
				int penMsgType = intent.getIntExtra( Broadcast.MESSAGE_TYPE, 0 );
				String content = intent.getStringExtra( Broadcast.CONTENT );

				handleMsg( penAddress, penMsgType, content );
			}
			else if ( Broadcast.ACTION_PEN_DOT.equals( action ) )
			{
				String penAddress = intent.getStringExtra( Broadcast.PEN_ADDRESS );
				Dot dot = intent.getParcelableExtra(Broadcast.EXTRA_DOT );
				dot.color = Color.BLACK;
				handleDot(penAddress, dot );
			}
			else if(Broadcast.ACTION_OFFLINE_STROKES.equals( action ))
			{
				String penAddress = intent.getStringExtra( Broadcast.PEN_ADDRESS );
				Parcelable[] array = intent.getParcelableArrayExtra( Broadcast.EXTRA_OFFLINE_STROKES );
				if(array != null)
				{
					Stroke[] strokes  = new Stroke[array.length];
//					ArrayList<Stroke> offList = new ArrayList<Stroke>();
					for (int i = 0; i < array.length; i++) {
						strokes[i] = ((Stroke) array[i]);
					}
					mSampleView.addStrokes(penAddress, strokes);
				}
			}
			else if(Broadcast.ACTION_WRITE_PAGE_CHANGED.equals( action ))
			{
				int sectionId = intent.getIntExtra( Broadcast.EXTRA_SECTION_ID, -1);
				int ownerId = intent.getIntExtra( Broadcast.EXTRA_OWNER_ID, -1);
				int noteId = intent.getIntExtra( Broadcast.EXTRA_BOOKCODE_ID, -1);
				int pageNum = intent.getIntExtra( Broadcast.EXTRA_PAGE_NUMBER, -1);
				currentSectionId = sectionId;
				currentOwnerId = ownerId;
				currentBookcodeId = noteId;
				currentPagenumber =pageNum;
				mSampleView.changePage(sectionId, ownerId,noteId,pageNum );
			}
			else if(Broadcast.ACTION_SYMBOL_ACTION.equals( action ))
			{
				int sectionId = intent.getIntExtra( Broadcast.EXTRA_SECTION_ID, -1);
				int ownerId = intent.getIntExtra( Broadcast.EXTRA_OWNER_ID, -1);
				int noteId = intent.getIntExtra( Broadcast.EXTRA_BOOKCODE_ID, -1);
				int pageNum = intent.getIntExtra( Broadcast.EXTRA_PAGE_NUMBER, -1);
				if(currentSectionId == sectionId && currentOwnerId == ownerId && currentBookcodeId == noteId && currentPagenumber == pageNum)
				{
					String symbolId = intent.getStringExtra( Broadcast.EXTRA_SYMBOL_ID);
					MetadataCtrl metadataCtrl = MetadataCtrl.getInstance();
					Symbol s = metadataCtrl.getInstance().findApplicableSymbol( symbolId );
					if(s != null)
					{
						Toast.makeText(MainActivity.this, "Symbol Check!! Action="+s.getAction()+", Param="+s.getParam(), Toast.LENGTH_LONG).show();
					}
				}
			}
//			else if ( Broadcast.ACTION_PEN_DOT.equals( action ))
//			{
//				penClientCtrl.suspendPenUpgrade();
//			}
		}
	};

//	@Override
//	public void onFinish ()
//	{
//
//	}
//
//	@Override
//	public void onCatchSingleTab ()
//	{
//
//	}

//    private BroadcastReceiver mBTDuplicateRemoveBroadcasterReceiver = new BroadcastReceiver()
//    {
//
//        @Override
//        public void onReceive ( Context context, Intent intent )
//        {
//            if(intent != null && intent.getAction().equals( BTDuplicateRemoveBroadcasterReceiver.ACTION_BT_CONNECTED ))
//            {
//                String packageName = intent.getStringExtra( BTDuplicateRemoveBroadcasterReceiver.EXTRA_BT_CONNECT_PACKAGENAME );
//                NLog.d( "mBTDuplicateRemoveBroadcasterReceiver connect packageName="+packageName );
//                Util.showToast( context , "packageName �� BT�� ����Ǿ� �ִ� �����Դϴ�. packageName ���� BT ������ ����õ��ϼ���" );
//            }
//        }
//    };

}
