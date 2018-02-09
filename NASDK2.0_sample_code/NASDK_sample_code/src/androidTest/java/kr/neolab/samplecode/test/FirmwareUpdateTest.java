package kr.neolab.samplecode.test;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import kr.neolab.samplecode.Const;
import kr.neolab.samplecode.PenClientCtrl;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

import static kr.neolab.samplecode.MainActivity.TAG;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by LMS on 2017-08-16.
 */
@RunWith(AndroidJUnit4.class)
public class FirmwareUpdateTest
{
//    @Rule
//    public ActivityTestRule<DeviceListActivity> mActivityRule = new ActivityTestRule<>( DeviceListActivity.class );

    CountDownLatch signal = new CountDownLatch( 1 );
    CountDownLatch stateSignal = new CountDownLatch( 1 );
    boolean isAPISuccess = false;
    boolean isUpdateFail = false;
    boolean isSuspendTest = false;

    JSONObject statusObj = null;
    Context context = null;

    private AlertDialog alertDialog = null;

    private static final String ASSET_FIRMWARE_FILES_FOLDER_NAME = "firmware_files";


    private static final String FW_F110_FILE = "N2_1.05.0158.zip";
    private static final String FW_F50_FILE = "NWP-F50_1.03.0052._v_";
    private static final String FW_F120_FILE = "NWP-F120_1.04.0057._v_";


    BroadcastReceiver connectBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive ( Context context, Intent intent )
        {
            if ( Const.Broadcast.ACTION_PEN_MESSAGE.equals( intent.getAction() ) )
            {
                int penMsgType = intent.getIntExtra( Const.Broadcast.MESSAGE_TYPE, 0 );
                String content = intent.getStringExtra( Const.Broadcast.CONTENT );

                NLog.d( "PenSettingTest connectBroadcastReceiver penMsgType="+penMsgType+",content="+content );
                switch ( penMsgType )
                {
                    case PenMsgType.PEN_AUTHORIZED:
                        signal.countDown();
                        break;
                    case PenMsgType.PASSWORD_REQUEST:
                        SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences( context );

                        String pass= mPref.getString( Const.Setting.KEY_PASSWORD, null );
                        PenClientCtrl.getInstance( context ).getIPenCtrl().inputPassword( pass );
//                        signal.countDown();
                        break;
                    // Message when a connection attempt is unsuccessful pen
                    case PenMsgType.PEN_CONNECTION_FAILURE:
                        signal.countDown();
                        break;
//                    case PenMsgType.PEN_USING_NOTE_SET_RESULT:
//                        try
//                        {
//                            JSONObject job = new JSONObject( content );
//                            isAPISuccess = job.getBoolean( JsonTag.BOOL_RESULT );
//                            signal.countDown();
//                        }
//                        catch ( JSONException e )
//                        {
//                            e.printStackTrace();
//                        }
//                        break;
                    case PenMsgType.PEN_FW_UPGRADE_FAILURE:
//                        if(alertDialog.isShowing())
//                        {
//                            alertDialog.dismiss();
//                        }
                        isUpdateFail = true;
                        signal.countDown();
                        break;
                    case PenMsgType.PEN_FW_UPGRADE_STATUS:
                        try
                        {
                            JSONObject job = new JSONObject( content );

                            int total = job.getInt( Const.JsonTag.INT_TOTAL_SIZE );
                            int sent = job.getInt( Const.JsonTag.INT_SENT_SIZE );
                            if(isSuspendTest && (sent*10/total) > 1)
                            {
                                isAPISuccess = true;
                                signal.countDown();
                            }

//                            alertDialog.setMessage( "Update total("+total+")/sent("+sent+")" );
//                            if(!alertDialog.isShowing())
//                            {
//                                alertDialog.show();
//                            }
                            Log.d( TAG, "pen fw upgrade status => total : " + total + ", progress : " + sent );
                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }
                        break;
                    case PenMsgType.PEN_FW_UPGRADE_SUSPEND:
                        if(isSuspendTest)
                        {
                            isAPISuccess = true;
                            signal.countDown();
                        }
                        break;
                    case PenMsgType.PEN_FW_UPGRADE_SUCCESS:
                        isAPISuccess = true;
                        signal.countDown();

                        break;

                }
            }
        }
    };


    @Before
    public void init ()
    {

        context = InstrumentationRegistry.getTargetContext();
        IntentFilter i = new IntentFilter();
        i.addAction( Const.Broadcast.ACTION_PEN_MESSAGE );
        context.registerReceiver( connectBroadcastReceiver, i );

        new Handler( Looper.getMainLooper()).post( new Runnable()
        {
            @Override
            public void run ()
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle( "Update Test" );
                builder.setMessage( "Update Test" );
                builder.setCancelable( false );
                alertDialog = builder.create();
            }
        } );


        String address = PenClientCtrl.getInstance( context).getConnectDevice();

        copyAssets(context);
        if(address == null)
        {
            SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences( context );

            address = mPref.getString( Const.Setting.KEY_MAC_ADDRESS, null );
            assertNotNull( address );
            signal = new CountDownLatch( 1 );
            new Handler( Looper.getMainLooper()).postDelayed( new Runnable()
            {
                @Override
                public void run ()
                {
                    signal.countDown();
                }
            },500 );
            try
            {
                signal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            signal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).connect(address);
            try
            {
                signal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }

            String connectedAddress = PenClientCtrl.getInstance( context ).getConnectDevice();
            NLog.d( "PenSettingTest connectedAddress = "+connectedAddress );
            assertNotNull( connectedAddress );
        }
    }

    @Test
    public void FirmwareUpdateNullTest ()
    {
        signal = new CountDownLatch( 1 );
        isUpdateFail = false;
        // File fwFile null test, have to receive PenMsgType.PEN_FW_UPGRADE_FAILURE.
        try
        {
            if ( PenClientCtrl.getInstance( context ).getProtocolVersion() == 1 )
                PenClientCtrl.getInstance( context ).getIPenCtrl().upgradePen( null );
            else PenClientCtrl.getInstance( context ).getIPenCtrl().upgradePen2( null, "" );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        try
        {
            signal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isUpdateFail );
    }

    @Test
    public void FirmwareUpdateNonMatchingTest ()
    {
        isAPISuccess = false;
        // non matching protocol test
        // expect ProtocolNotSupportedException.
        try
        {
            File file = null;
            if ( PenClientCtrl.getInstance( context ).getProtocolVersion() == 1 )
            {
                file = new File( context.getCacheDir(), FW_F120_FILE );
                PenClientCtrl.getInstance( context ).getIPenCtrl().upgradePen2( file, "" );
            }
            else
            {
                file = new File( context.getCacheDir(), FW_F110_FILE );
                PenClientCtrl.getInstance( context ).getIPenCtrl().upgradePen( file, "" );
            }

        }
        catch ( Exception e )
        {
            isAPISuccess = true;
            e.printStackTrace();
        }
        assertTrue( isAPISuccess );

    }
    @Test
    public void FirmwareUpdateSuspendTest ()
    {
        signal = new CountDownLatch( 1 );
        isAPISuccess = false;
        isSuspendTest = true;
        // this is fw update test.(protocol 1.0)
        try
        {
            File file = null;
            if ( PenClientCtrl.getInstance( context ).getProtocolVersion() == 1 )
            {
                file = new File( context.getCacheDir(), FW_F110_FILE );
                PenClientCtrl.getInstance( context ).getIPenCtrl().upgradePen( file, "\\abc.zip" );
            }
            else
            {
                if ( PenClientCtrl.getInstance( context ).getDeviceName().equals( "NWP-F50" ) )
                {
                    file = new File( context.getCacheDir(), FW_F50_FILE );
                }
                else
                {
                    file = new File( context.getCacheDir(), FW_F120_FILE );
                }
                PenClientCtrl.getInstance( context ).getIPenCtrl().upgradePen2( file, "\\abc.zip" );

            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            signal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isAPISuccess );
        isAPISuccess = false;
        signal = new CountDownLatch( 1 );
        PenClientCtrl.getInstance( context ).getIPenCtrl().suspendPenUpgrade();
        try
        {
            signal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isAPISuccess );
        isSuspendTest = false;
    }


        @Test
    public void FirmwareUpdateTest ()
    {
        signal = new CountDownLatch( 1 );
        isSuspendTest = false;
        isAPISuccess = false;
        // this is fw update test.(protocol 1.0)
        try
        {
            File file = null;
            if ( PenClientCtrl.getInstance( context ).getProtocolVersion() == 1 )
            {
                file = new File( context.getCacheDir(), FW_F110_FILE );
                PenClientCtrl.getInstance( context ).getIPenCtrl().upgradePen( file, "\\abc.zip" );
            }
            else
            {
                if ( PenClientCtrl.getInstance( context ).getDeviceName().equals( "NWP-F50" ) )
                {
                    file = new File( context.getCacheDir(), FW_F50_FILE );
                }
                else
                {
                    file = new File( context.getCacheDir(), FW_F120_FILE );
                }
                PenClientCtrl.getInstance( context ).getIPenCtrl().upgradePen2( file, "\\abc.zip" );

            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            signal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isAPISuccess );

    }


    @After
    public void close()
    {
        context.unregisterReceiver( connectBroadcastReceiver );
    }



    private void copyAssets( Context context )
    {
        AssetManager assetManager = context.getAssets();

        String[] files = null;

        try
        {
            files = assetManager.list( ASSET_FIRMWARE_FILES_FOLDER_NAME);
        }
        catch ( IOException e )
        {
        }

        for ( String filename : files )
        {
            InputStream in = null;
            OutputStream out = null;

            try
            {
                in = assetManager.open( ASSET_FIRMWARE_FILES_FOLDER_NAME + "/" + filename );

                File outFile = new File( context.getCacheDir(), filename );
                if(outFile.exists())
                    break;
                out = new FileOutputStream( outFile );
                copyFile( in, out );
            }
            catch ( IOException e )
            {
            }
            finally
            {
                if ( in != null )
                {
                    try
                    {
                        in.close();
                    }
                    catch ( IOException e )
                    {
                        // NOOP
                    }
                }
                if ( out != null )
                {
                    try
                    {
                        out.close();
                    }
                    catch ( IOException e )
                    {
                        // NOOP
                    }
                }
            }
        }
    }

    private void copyFile( InputStream in, OutputStream out ) throws IOException
    {
        byte[] buffer = new byte[1024];

        int read;

        while ( (read = in.read( buffer )) != -1 )
        {
            out.write( buffer, 0, read );
        }
    }

}
