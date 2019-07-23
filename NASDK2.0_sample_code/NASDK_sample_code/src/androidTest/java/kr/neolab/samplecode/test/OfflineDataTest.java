package kr.neolab.samplecode.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import kr.neolab.samplecode.Const;
import kr.neolab.samplecode.PenClientCtrl;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.structure.Symbol;
import kr.neolab.sdk.pen.offline.OfflineFileParser;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

import static kr.neolab.samplecode.MainActivity.TAG;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by LMS on 2017-08-16.
 */
@RunWith(AndroidJUnit4.class)
public class OfflineDataTest
{
//    @Rule
//    public ActivityTestRule<DeviceListActivity> mActivityRule = new ActivityTestRule<>( DeviceListActivity.class );

    CountDownLatch signal = new CountDownLatch( 1 );
    CountDownLatch stateSignal = new CountDownLatch( 1 );
    boolean isSettingSuccess = false;
    JSONObject statusObj = null;
    Context context = null;
    JSONArray offlineDataJsonList = null;
    ArrayList<Stroke> strokeList = new ArrayList<Stroke>();

    BroadcastReceiver connectBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive ( Context context, Intent intent )
        {
            if ( Const.Broadcast.ACTION_PEN_MESSAGE.equals( intent.getAction() ) )
            {
                int penMsgType = intent.getIntExtra( Const.Broadcast.MESSAGE_TYPE, 0 );
                String content = intent.getStringExtra( Const.Broadcast.CONTENT );
                String penAddress = intent.getStringExtra( Const.Broadcast.PEN_ADDRESS );
                NLog.d( "PenSettingTest connectBroadcastReceiver penMsgType="+penMsgType+",content="+content );
                switch ( penMsgType )
                {
                    case PenMsgType.PEN_AUTHORIZED:
                        signal.countDown();
                        break;
                    // Message when a connection attempt is unsuccessful pen
                    case PenMsgType.PASSWORD_REQUEST:
                        SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences( context );

                        String pass= mPref.getString( Const.Setting.KEY_PASSWORD, null );
                        PenClientCtrl.getInstance( context ).getIPenCtrl().inputPassword( pass );
//                        signal.countDown();
                        break;
                    case PenMsgType.PEN_CONNECTION_FAILURE:
                        signal.countDown();
                        break;
                    case PenMsgType.OFFLINE_DATA_NOTE_LIST:
                        try
                        {
                            offlineDataJsonList = new JSONArray( content );
                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }
                        isSettingSuccess = true;
                        signal.countDown();
                        break;
                    case PenMsgType.OFFLINE_DATA_FILE_DELETED:
                    case PenMsgType.OFFLINE_DATA_PAGE_LIST:
                    case PenMsgType.OFFLINE_DATA_FILE_CREATED:
                    case PenMsgType.OFFLINE_DATA_SEND_START:
                        break;
                    case PenMsgType.OFFLINE_DATA_SEND_FAILURE:
                        signal.countDown();
                        break;
                    case PenMsgType.OFFLINE_DATA_SEND_SUCCESS:
                        if(PenClientCtrl.getInstance( context ).getProtocolVersion() ==1)
                        {
                            parseOfflineData(penAddress);
                            signal.countDown();
                        }
                        break;

                }
            }
        }
    };

    IOfflineDataListener listener = new IOfflineDataListener()
    {
        @Override
        public void onReceiveOfflineStrokes ( String penAddress, Stroke[] strokes, int sectionId, int ownerId, int noteId, Symbol[] symbols )
        {
            if ( strokes != null )
            {
                ArrayList<Stroke> temp = new ArrayList( Arrays.asList( strokes ) );
                strokeList.addAll( temp );
            }
            signal.countDown();
        }
    };

    @Before
    public void init ()
    {

        context = InstrumentationRegistry.getTargetContext();
        IntentFilter i = new IntentFilter();
        i.addAction( Const.Broadcast.ACTION_PEN_MESSAGE );
        context.registerReceiver( connectBroadcastReceiver, i );
        PenClientCtrl.getInstance( context).getIPenCtrl().setOffLineDataListener( listener );
        String address = PenClientCtrl.getInstance( context).getConnectDevice();
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
            PenClientCtrl.getInstance( context ).setAllowOfflineData( true );
        }
    }

    @Test
    public void OfflineDataTest ()
    {
        signal = new CountDownLatch( 1 );
        new Handler( Looper.getMainLooper()).postDelayed( new Runnable()
        {
            @Override
            public void run ()
            {
                signal.countDown();
            }
        },300 );
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).reqOfflineDataList();
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        NLog.d( "OfflineDataTest reqOfflineDataList(sectionId, ownerId, bookIds) = "+isSettingSuccess );
        assertTrue( isSettingSuccess);
        if(offlineDataJsonList != null && offlineDataJsonList.length() > 0)
        {
             try
            {

                for ( int i = 0; i < offlineDataJsonList.length(); i++ )
                {
                    signal = new CountDownLatch( 1 );
                    JSONObject jobj = offlineDataJsonList.getJSONObject( i );

                    int sectionId = jobj.getInt( Const.JsonTag.INT_SECTION_ID );
                    int ownerId = jobj.getInt( Const.JsonTag.INT_OWNER_ID );
                    int noteId = jobj.getInt( Const.JsonTag.INT_NOTE_ID );
                    NLog.d( "offline(" + ( i + 1 ) + ") note => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId );
                    PenClientCtrl.getInstance( context ).getIPenCtrl().reqOfflineData( sectionId, ownerId, noteId );
                    signal.await();
                }
            }catch ( Exception e )
            {
                e.printStackTrace();
            }

        }
        NLog.d( "OfflineDataTest strokeList="+strokeList.size() );
    }

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
                    ArrayList<Stroke> temp = new ArrayList( Arrays.asList( strokes ) );
                    strokeList.addAll( temp );
                }
                parser.delete();
                parser = null;
            }
            catch ( Exception e )
            {
                Log.e( TAG, "parse file exeption occured.", e );
            }
        }
    }

    @After
    public void close()
    {
        context.unregisterReceiver( connectBroadcastReceiver );
    }


}
