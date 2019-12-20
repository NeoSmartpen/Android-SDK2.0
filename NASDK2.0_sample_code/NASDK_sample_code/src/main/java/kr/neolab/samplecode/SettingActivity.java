package kr.neolab.samplecode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import kr.neolab.samplecode.Const.Broadcast;
import kr.neolab.samplecode.Const.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsgType;

public class SettingActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	public static final String TAG = "pensdk.sample";
	
	private EditTextPreference mPasswordPref;
	
	private PenClientCtrl penClient;
	private MultiPenClientCtrl multiPenClient;
	private String address = null;
	private boolean isSingleConnectionMode = true;
	
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		address = getIntent().getStringExtra( "pen_address" );
		if(address == null)
			isSingleConnectionMode = true;
		else
			isSingleConnectionMode = false;
		if(isSingleConnectionMode)
		{
			penClient = PenClientCtrl.getInstance( getApplicationContext() );
			if(penClient.getProtocolVersion() == 1)
				addPreferencesFromResource( R.xml.pref_settings );
			else
				addPreferencesFromResource( R.xml.pref_settings2 );
		}
		else
		{
			multiPenClient = MultiPenClientCtrl.getInstance( getApplicationContext() );
			if(multiPenClient.getProtocolVersion(address) == 1)
				addPreferencesFromResource( R.xml.pref_settings );
			else
				addPreferencesFromResource( R.xml.pref_settings2 );
		}

		mPasswordPref = (EditTextPreference) getPreferenceScreen().findPreference( Const.Setting.KEY_PASSWORD );

		EditText myEditText = (EditText) mPasswordPref.getEditText();
		myEditText.setKeyListener( DigitsKeyListener.getInstance( false, true ) );
	}

	public void onSharedPreferenceChanged( SharedPreferences sharedPreferences, String key )
	{
		if ( key.equals( Const.Setting.KEY_PASSWORD ) )
		{
			String oldPassword ="";
			String newPassword = sharedPreferences.getString( Const.Setting.KEY_PASSWORD, "" );

			if(isSingleConnectionMode)
			{
				oldPassword = penClient.getCurrentPassword();
				penClient.reqSetupPassword( oldPassword, newPassword );
			}
			else
			{
				oldPassword = multiPenClient.getCurrentPassword(address);
				multiPenClient.reqSetupPassword(address, oldPassword, newPassword );
			}

		}
		else if ( key.equals( Const.Setting.KEY_AUTO_POWER_ON ) )
		{
			boolean value = sharedPreferences.getBoolean( Const.Setting.KEY_AUTO_POWER_ON, true );

			if(isSingleConnectionMode)
			{
				penClient.reqSetupAutoPowerOnOff( value );
			}
			else
			{
				multiPenClient.reqSetupAutoPowerOnOff( address, value );
			}
		}
		else if ( key.equals( Const.Setting.KEY_BEEP ) )
		{
			boolean value = sharedPreferences.getBoolean( Const.Setting.KEY_BEEP, true );

			if(isSingleConnectionMode)
			{
				penClient.reqSetupPenBeepOnOff( value );
			}
			else
			{
				multiPenClient.reqSetupPenBeepOnOff( address, value );
			}
		}
		else if ( key.equals( Const.Setting.KEY_AUTO_POWER_OFF_TIME ) )
		{
			short value = Short.parseShort( sharedPreferences.getString( Const.Setting.KEY_AUTO_POWER_OFF_TIME, "10" ) );

			if(isSingleConnectionMode)
			{
				penClient.reqSetupAutoShutdownTime( value );
			}
			else
			{
				multiPenClient.reqSetupAutoShutdownTime( address, value );
			}
		}
		else if ( key.equals( Const.Setting.KEY_SENSITIVITY ) )
		{
			short value = Short.parseShort( sharedPreferences.getString( Const.Setting.KEY_SENSITIVITY, "0" ) );

			if(isSingleConnectionMode)
			{
				penClient.reqSetupPenSensitivity( value );
			}
			else
			{
				multiPenClient.reqSetupPenSensitivity( address, value );
			}
		}
		else if ( key.equals( Const.Setting.KEY_PEN_COLOR ) )
		{
			int value = Integer.parseInt(sharedPreferences.getString( Const.Setting.KEY_PEN_COLOR, "-15198184" ) );
			if(isSingleConnectionMode)
			{
				penClient.reqSetupPenTipColor( value );
			}
			else
			{
				multiPenClient.reqSetupPenTipColor( address, value );
			}
		}
		else if ( key.equals( Const.Setting.KEY_PEN_CAP_ON ) )
		{
			boolean value = sharedPreferences.getBoolean( Const.Setting.KEY_PEN_CAP_ON, true );
			if(isSingleConnectionMode)
			{
				penClient.reqSetupPenCapOnOff( value );
			}
			else
			{
				multiPenClient.reqSetupPenCapOnOff( address, value );
			}
		}
		else if ( key.equals( Const.Setting.KEY_OFFLINE_DATA_SAVE ) )
		{
			boolean value = sharedPreferences.getBoolean( Const.Setting.KEY_OFFLINE_DATA_SAVE, true );
			if(isSingleConnectionMode)
			{
				penClient.setAllowOfflineData( value );
			}
			else
			{
				multiPenClient.setAllowOfflineData( address, value );
			}
		}
		else if ( key.equals( Const.Setting.KEY_HOVER_MODE ) )
		{
			boolean value = sharedPreferences.getBoolean( Const.Setting.KEY_HOVER_MODE, true );

			if(isSingleConnectionMode)
			{
				penClient.reqSetupPenHover( value );
			}
			else
			{
				multiPenClient.reqSetupPenHover( address, value );
			}
		}

	}

	@Override
	protected void onResume()
	{
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener( this );
		IntentFilter filter = new IntentFilter( Broadcast.ACTION_PEN_MESSAGE );
		registerReceiver( mBroadcastReceiver, filter );
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener( this );
		unregisterReceiver( mBroadcastReceiver );
	}

	public void handleMsg( int penMsgType, String content )
	{
		Log.d( TAG, "handleMsg : " + penMsgType);
		
		switch ( penMsgType )
		{
			// Response to the pen automatically set End Time
			case PenMsgType.PEN_SETUP_AUTO_SHUTDOWN_RESULT:
			{
				try
				{
					JSONObject job = new JSONObject( content );

					Toast.makeText( this, "pen setup auto shutdown result : " + job.getBoolean( JsonTag.BOOL_RESULT ), Toast.LENGTH_SHORT ).show();
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;

			// Response to the pen sensitivity setting
			case PenMsgType.PEN_SETUP_SENSITIVITY_RESULT:
			{
				try
				{
					JSONObject job = new JSONObject( content );
					Toast.makeText( this, "pen setup sensitivity result : " + job.getBoolean( JsonTag.BOOL_RESULT ), Toast.LENGTH_SHORT ).show();
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;
				
			// Response to the pen auto power on setting
			case PenMsgType.PEN_SETUP_AUTO_POWER_ON_RESULT:
			{
				try
				{
					JSONObject job = new JSONObject( content );
					Toast.makeText( this, "pen auto power on setting result : " + job.getBoolean( JsonTag.BOOL_RESULT ), Toast.LENGTH_SHORT ).show();
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;
				
			// Response to the beep on/off setting			
			case PenMsgType.PEN_SETUP_BEEP_RESULT:
			{
				try
				{
					JSONObject job = new JSONObject( content );
					Toast.makeText( this, "beep on/off setting result : " + job.getBoolean( JsonTag.BOOL_RESULT ), Toast.LENGTH_SHORT ).show();
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;
				
			// Response to the pen color setting
			case PenMsgType.PEN_SETUP_PEN_COLOR_RESULT:
			{
				try
				{
					JSONObject job = new JSONObject( content );
					Toast.makeText( this, "pen color setting result : " + job.getBoolean( JsonTag.BOOL_RESULT ), Toast.LENGTH_SHORT ).show();
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;
				
				
				
			// Pen password change success response
			case PenMsgType.PASSWORD_SETUP_SUCCESS:
				break;

			// Pen password change fails, the response
			case PenMsgType.PASSWORD_SETUP_FAILURE:
				break;



			case PenMsgType.PEN_SETUP_PEN_CAP_OFF:
				try
				{
					JSONObject job = new JSONObject( content );
					Toast.makeText( this, "pencap off setting result : " + job.getBoolean( JsonTag.BOOL_RESULT ), Toast.LENGTH_SHORT ).show();
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
				break;

			case PenMsgType.PEN_SETUP_HOVER_ONOFF:
				try
				{
					JSONObject job = new JSONObject( content );
					Toast.makeText( this, "hover mode onoff setting result : " + job.getBoolean( JsonTag.BOOL_RESULT ), Toast.LENGTH_SHORT ).show();
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
				break;

			case PenMsgType.PEN_SETUP_OFFLINEDATA_SAVE_ONOFF:
				try
				{
					JSONObject job = new JSONObject( content );
					Toast.makeText( this, "offline data save onoff setting result : " + job.getBoolean( JsonTag.BOOL_RESULT ), Toast.LENGTH_SHORT ).show();
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
				break;
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
				int penMsgType = intent.getIntExtra( Broadcast.MESSAGE_TYPE, 0 );
				String content = intent.getStringExtra( Broadcast.CONTENT );

				handleMsg( penMsgType, content );
			}
		}
	};
}
