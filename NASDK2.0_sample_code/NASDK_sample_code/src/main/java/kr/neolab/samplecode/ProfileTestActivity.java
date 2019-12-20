package kr.neolab.samplecode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import kr.neolab.sdk.pen.bluetooth.lib.PenProfile;
import kr.neolab.sdk.pen.bluetooth.lib.ProfileKeyValueLimitException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

import static kr.neolab.samplecode.Const.NEOLAB_PROFILE_PASS;

public class ProfileTestActivity extends Activity
{

    private TextView result_text;
    private RadioButton select_radio;
    private LinearLayout profile_name_layout, /*profile_pass_layout,*/ profile_key_layout,profile_value_layout;
    private EditText profile_name_edit,profile_pass_edit,profile_key_edit,profile_value_edit;
    private Button button_exe;

    private int type = 0;


    private final String[] request = {"Get Profile Info", "Create Profile", "Delete Profile", "Read Profile Value", "Write Profile Value", "Delete Profile Value"};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Setup the window

        setContentView( R.layout.profile_test );
        result_text = (TextView) findViewById( R.id.result_text );

        profile_name_layout = (LinearLayout) findViewById( R.id.profile_name_layout );
//        profile_pass_layout = (LinearLayout) findViewById( R.id.profile_pass_layout );
        profile_key_layout = (LinearLayout) findViewById( R.id.profile_key_layout );
        profile_value_layout = (LinearLayout) findViewById( R.id.profile_value_layout );

        profile_name_edit = (EditText) findViewById( R.id.profile_name_edit );
        profile_pass_edit = (EditText) findViewById( R.id.profile_pass_edit );
        profile_key_edit = (EditText) findViewById( R.id.profile_key_edit );
        profile_value_edit = (EditText) findViewById( R.id.profile_value_edit );

        select_radio = (RadioButton) findViewById( R.id.select_radio );
        select_radio.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick ( View v )
            {
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder( ProfileTestActivity.this );
                builder.setSingleChoiceItems( request, type, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick ( DialogInterface dialog, int which )
                    {
                        type = which;
                        select_radio.setText( request[type] );
                        selectType(type);
                        dialog.dismiss();
                    }
                });
                builder.setCancelable( false );
                builder.create().show();

            }
        } );
        selectType(type);

        button_exe = (Button) findViewById( R.id.button_exe );
        button_exe.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick ( View v )
            {
                exeType(type);
            }
        } );

        IntentFilter filter = new IntentFilter( Const.Broadcast.ACTION_PEN_MESSAGE );
        registerReceiver( mBroadcastReceiver, filter );

    }

    private void selectType(int i )
    {
//        "Get Profile Info", "Create Profile", "Delete Profile", "Read Profile Value", "Write Profile Value", "Delete Profile Value";
        switch ( i )
        {
            case 0:
                profile_name_layout.setVisibility( View.VISIBLE );
//                profile_pass_layout.setVisibility( View.GONE );
                profile_key_layout.setVisibility( View.GONE );
                profile_value_layout.setVisibility( View.GONE );
                break;

            case 1:
                profile_name_layout.setVisibility( View.VISIBLE );
//                profile_pass_layout.setVisibility( View.VISIBLE );
                profile_key_layout.setVisibility( View.GONE );
                profile_value_layout.setVisibility( View.GONE );
                break;

            case 2:
                profile_name_layout.setVisibility( View.VISIBLE );
//                profile_pass_layout.setVisibility( View.VISIBLE );
                profile_key_layout.setVisibility( View.GONE );
                profile_value_layout.setVisibility( View.GONE );
                break;

            case 3:
                profile_name_layout.setVisibility( View.VISIBLE );
//                profile_pass_layout.setVisibility( View.GONE );
                profile_key_layout.setVisibility( View.VISIBLE );
                profile_value_layout.setVisibility( View.GONE );
                break;

            case 4:
                profile_name_layout.setVisibility( View.VISIBLE );
//                profile_pass_layout.setVisibility( View.VISIBLE );
                profile_key_layout.setVisibility( View.VISIBLE );
                profile_value_layout.setVisibility( View.VISIBLE );
                break;
            case 5:
                profile_name_layout.setVisibility( View.VISIBLE );
//                profile_pass_layout.setVisibility( View.VISIBLE );
                profile_key_layout.setVisibility( View.VISIBLE );
                profile_value_layout.setVisibility( View.GONE );
                break;

        }
    }

    private void exeType(int i)
    {
        //        "Get Profile Info", "Create Profile", "Delete Profile", "Read Profile Value", "Write Profile Value", "Delete Profile Value";
        switch ( i )
        {
            case 0:
                try
                {
                    PenClientCtrl.getInstance( this ).getIPenCtrl().getProfileInfo( profile_name_edit.getText().toString() );
                }
                catch ( ProtocolNotSupportedException e )
                {
                    result_text.append( "\n"+e.getMessage());
                    e.printStackTrace();
                }
                catch ( ProfileKeyValueLimitException e1)
                {
                    result_text.append( "\n"+e1.getMessage());
                    e1.printStackTrace();
                }

                break;

            case 1:
                try
                {
                    PenClientCtrl.getInstance( this ).getIPenCtrl().createProfile( profile_name_edit.getText().toString(), NEOLAB_PROFILE_PASS );
                }
                catch ( ProtocolNotSupportedException e )
                {
                    result_text.append( "\n"+e.getMessage());
                    e.printStackTrace();
                }
                catch ( ProfileKeyValueLimitException e1)
                {
                    result_text.append( "\n"+e1.getMessage());
                    e1.printStackTrace();
                }
                break;

            case 2:
                try
                {
                    PenClientCtrl.getInstance( this ).getIPenCtrl().deleteProfile( profile_name_edit.getText().toString(), NEOLAB_PROFILE_PASS );
                }
                catch ( ProtocolNotSupportedException e )
                {
                    result_text.append( "\n"+e.getMessage());
                    e.printStackTrace();
                }
                catch ( ProfileKeyValueLimitException e1)
                {
                    result_text.append( "\n"+e1.getMessage());
                    e1.printStackTrace();
                }
                break;

            case 3:
                try
                {
                    PenClientCtrl.getInstance( this ).getIPenCtrl().readProfileValue( profile_name_edit.getText().toString(),new String[]{profile_key_edit.getText().toString()});
                }
                catch ( ProtocolNotSupportedException e )
                {
                    result_text.append( "\n"+e.getMessage());
                    e.printStackTrace();
                }
                catch ( ProfileKeyValueLimitException e1)
                {
                    result_text.append( "\n"+e1.getMessage());
                    e1.printStackTrace();
                }
                break;

            case 4:
                try
                {
                    byte[][] data = new byte[1][1];
                    data[0] = profile_value_edit.getText().toString().getBytes();

//                    data[0] = ByteBuffer.allocate( 4 ).putInt( 123 ).array();
                    PenClientCtrl.getInstance( this ).getIPenCtrl().writeProfileValue( profile_name_edit.getText().toString(), NEOLAB_PROFILE_PASS, new String[]{profile_key_edit.getText().toString()},data);
                }
                catch ( ProtocolNotSupportedException e )
                {
                    result_text.append( "\n"+e.getMessage());
                    e.printStackTrace();
                }
                catch ( ProfileKeyValueLimitException e1)
                {
                    result_text.append( "\n"+e1.getMessage());
                    e1.printStackTrace();
                }
                break;
            case 5:
                try
                {
                    byte[][] data = new byte[1][];
                    data[0] = profile_key_edit.getText().toString().getBytes();
                    PenClientCtrl.getInstance( this ).getIPenCtrl().deleteProfileValue( profile_name_edit.getText().toString(), NEOLAB_PROFILE_PASS, new String[]{profile_key_edit.getText().toString()});
                }
                catch ( ProtocolNotSupportedException e )
                {
                    result_text.append( "\n"+e.getMessage());
                    e.printStackTrace();
                }
                catch ( ProfileKeyValueLimitException e1)
                {
                    result_text.append( "\n"+e1.getMessage());
                    e1.printStackTrace();
                }
                break;

        }    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause ()
    {
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver( mBroadcastReceiver );
    }
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            String action = intent.getAction();

            if ( Const.Broadcast.ACTION_PEN_MESSAGE.equals( action ) )
            {
                int penMsgType = intent.getIntExtra( Const.Broadcast.MESSAGE_TYPE, 0 );
                String content = intent.getStringExtra( Const.Broadcast.CONTENT );

                handleMsg( penMsgType, content );
            }
        }
    };

    public void handleMsg(final int penMsgType,final String content )
    {
        NLog.d( "handleMsg : " + penMsgType);
        runOnUiThread( new Runnable()
        {
            @Override
            public void run ()
            {
                JSONObject profileObj = null;
                JSONArray array = null;
                String profileName = "";
                String result = "";
                switch ( penMsgType )
                {
                    case PenMsgType.PROFILE_DELETE:
                        try
                        {
                            profileObj = new JSONObject( content );
                            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
                            int status = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS);
                            result = "Response PROFILE_DELETE\nprofileName="+profileName+",status="+status+"(0x"+Integer.toHexString(status)+")";
                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }

                        break;
                    case PenMsgType.PROFILE_DELETE_VALUE:
                        try
                        {
                            profileObj = new JSONObject( content );
                            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
                            result = "Response PROFILE_DELETE_VALUE\nprofileName="+profileName;
                            array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
                            for(int i = 0; i < array.length(); i++)
                            {
                                JSONObject obj = array.getJSONObject( i );
                                String key = obj.getString( JsonTag.STRING_PROFILE_KEY );
                                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                                result += "\nkey="+key+",status="+status+"(0x"+Integer.toHexString(status)+")";
                            }
                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }
                        break;
                    case PenMsgType.PROFILE_READ_VALUE:
                        try
                        {
                            profileObj = new JSONObject( content );
                            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
                            result = "Response PROFILE_READ_VALUE\nprofileName="+profileName;
                            array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
                            for(int i = 0; i < array.length(); i++)
                            {
                                JSONObject obj = array.getJSONObject( i );
                                String key = obj.getString( JsonTag.STRING_PROFILE_KEY );
                                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                                String value = null;
                                if(status == PenProfile.PROFILE_STATUS_SUCCESS)
                                {
                                    String data = (String)obj.get( JsonTag.BYTE_PROFILE_VALUE );
                                    byte[] decodeByte = Base64.decode(data ,Base64.DEFAULT);
//                                    write 할때의 변수 타입에 따라
//                                    ByteBuffer buffer = ByteBuffer.allocate( decodeByte.length ).put( decodeByte );
//                                    buffer.getInt();
                                    value = new String( decodeByte );
                                    NLog.d( "test e = " +decodeByte.length);
                                }
                                result += "\nkey="+key+",status="+status+"(0x"+Integer.toHexString(status)+")";
                                if(value != null)
                                    result += ",value="+value;
                            }
                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }
                        break;
                    case PenMsgType.PROFILE_WRITE_VALUE:
                        try
                        {
                            profileObj = new JSONObject( content );
                            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
                            result = "Response PROFILE_WRITE_VALUE\nprofileName="+profileName;
                            array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
                            for(int i = 0; i < array.length(); i++)
                            {
                                JSONObject obj = array.getJSONObject( i );
                                String key = obj.getString( JsonTag.STRING_PROFILE_KEY );
                                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                                result += "\nkey="+key+",status="+status+"(0x"+Integer.toHexString(status)+")";
                            }
                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }
                        break;

                    case PenMsgType.PROFILE_CREATE:
                        try
                        {
                            profileObj = new JSONObject( content );
                            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
                            int status = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS);
                            result = "Response PROFILE_CREATE\nprofileName="+profileName+",status="+status+"(0x"+Integer.toHexString(status)+")";
                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }
                        break;
                    case PenMsgType.PROFILE_INFO:
                        try
                        {
                            profileObj = new JSONObject( content );
                            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
                            int status = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS);
                            result = "Response PROFILE_INFO\nprofileName="+profileName+",status="+status+"(0x"+Integer.toHexString(status)+")";
                            if(status == PenProfile.PROFILE_STATUS_SUCCESS)
                            {
                                int TOTAL_SECTOR_COUNT = profileObj.getInt( JsonTag.INT_PROFILE_INFO_TOTAL_SECTOR_COUNT );
                                int SECTOR_SIZE = profileObj.getInt( JsonTag.INT_PROFILE_INFO_SECTOR_SIZE );
                                int USE_SECTOR_COUNT = profileObj.getInt( JsonTag.INT_PROFILE_INFO_USE_SECTOR_COUNT );
                                int USE_KEY_COUNT = profileObj.getInt( JsonTag.INT_PROFILE_INFO_USE_KEY_COUNT );
                                result += "\nTOTAL_SECTOR_COUNT="+TOTAL_SECTOR_COUNT+"\nSECTOR_SIZE="+SECTOR_SIZE+"\nUSE_SECTOR_COUNT="+USE_SECTOR_COUNT+"\nUSE_KEY_COUNT="+USE_KEY_COUNT;

                            }

                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }
                        break;

                }
                if(result.length() == 0)
                    return;
                result_text.append( "\n"+result );
            }
        } );

    }


}
