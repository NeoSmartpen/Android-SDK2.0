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

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import kr.neolab.samplecode.Const;
import kr.neolab.samplecode.PenClientCtrl;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;
import kr.neolab.sdk.util.UseNoteData;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by LMS on 2017-08-16.
 */
@RunWith(AndroidJUnit4.class)
public class PenUseNoteSettingTest
{
//    @Rule
//    public ActivityTestRule<DeviceListActivity> mActivityRule = new ActivityTestRule<>( DeviceListActivity.class );

    CountDownLatch signal = new CountDownLatch( 1 );
    CountDownLatch stateSignal = new CountDownLatch( 1 );
    boolean isSettingSuccess = false;
    JSONObject statusObj = null;
    Context context = null;

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
                    case PenMsgType.PEN_USING_NOTE_SET_RESULT:
                        try
                        {
                            JSONObject job = new JSONObject( content );
                            isSettingSuccess = job.getBoolean( JsonTag.BOOL_RESULT );
                            signal.countDown();
                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }
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
        }
    }

    @Test
    public void UseNoteSettingTest ()
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

        int sectionId = 5;
        int ownerId = 3;
        int bookId = 603;
        int[] sectionIds = {5,0};
        int[] ownerIds = {3,10};
        int[] bookIds = {603};
        int[] pageIds = {1,2,3,4,5};
        ArrayList<Integer> pageIdArrayList = new ArrayList<Integer>();
        pageIdArrayList.add( 1 );
        UseNoteData data = new UseNoteData();
        data.sectionId = sectionId;
        data.ownerId = ownerId;
        data.noteIds = bookIds;
        ArrayList<UseNoteData> list = new ArrayList<UseNoteData>();
        list.add( data );

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).getIPenCtrl().reqAddUsingNote(sectionId, ownerId, bookIds);
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        NLog.d( "UseNoteSettingTest reqAddUsingNote(sectionId, ownerId, bookIds) = "+isSettingSuccess );
        assertTrue( isSettingSuccess);

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).getIPenCtrl().reqAddUsingNote(sectionId, ownerId);
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        NLog.d( "UseNoteSettingTest reqAddUsingNote(sectionId, ownerId) = "+isSettingSuccess );
        assertTrue( isSettingSuccess);

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).getIPenCtrl().reqAddUsingNote(sectionIds, ownerIds);
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        NLog.d( "UseNoteSettingTest reqAddUsingNote(sectionIds, ownerIds) = "+isSettingSuccess );
        assertTrue( isSettingSuccess);

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).getIPenCtrl().reqAddUsingNoteAll();
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        NLog.d( "UseNoteSettingTest reqAddUsingNoteAll() = "+isSettingSuccess );
        assertTrue( isSettingSuccess);

        if(PenClientCtrl.getInstance( context ).getProtocolVersion() == 2)
        {
            signal = new CountDownLatch( 1 );
            isSettingSuccess = false;
            try
            {
                PenClientCtrl.getInstance( context ).getIPenCtrl().reqAddUsingNote(list);
                signal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            NLog.d( "UseNoteSettingTest reqAddUsingNoteAll() = "+isSettingSuccess );
            assertTrue( isSettingSuccess);
        }
        else
        {
            signal = new CountDownLatch( 1 );
            isSettingSuccess = false;
            try
            {
                PenClientCtrl.getInstance( context ).getIPenCtrl().reqAddUsingNote(list);
            }
            catch ( ProtocolNotSupportedException e )
            {
                isSettingSuccess = true;
                e.printStackTrace();
            }
            NLog.d( "UseNoteSettingTest reqAddUsingNoteAll() = "+isSettingSuccess );
            assertTrue( isSettingSuccess);
        }
    }


    @After
    public void close()
    {
        context.unregisterReceiver( connectBroadcastReceiver );
    }


}
