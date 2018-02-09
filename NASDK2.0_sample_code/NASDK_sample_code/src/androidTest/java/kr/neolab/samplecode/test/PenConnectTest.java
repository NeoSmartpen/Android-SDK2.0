package kr.neolab.samplecode.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import kr.neolab.samplecode.Const;
import kr.neolab.samplecode.PenClientCtrl;
import kr.neolab.sdk.pen.penmsg.PenMsgType;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * Created by LMS on 2017-08-14.
 */


@RunWith(AndroidJUnit4.class)
@SmallTest
public class PenConnectTest
{
//    @Rule
//    public ActivityTestRule<DeviceListActivity> mActivityRule = new ActivityTestRule<>( DeviceListActivity.class );
    CountDownLatch signal = new CountDownLatch( 1 );
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

                switch ( penMsgType )
                {
                    case PenMsgType.PEN_AUTHORIZED:
                    case PenMsgType.PEN_CONNECTION_FAILURE:
                    case PenMsgType.PEN_DISCONNECTED:
                        signal.countDown();
                        break;
                    case PenMsgType.PASSWORD_REQUEST:
                        SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences( context );

                        String pass= mPref.getString( Const.Setting.KEY_PASSWORD, null );
                        PenClientCtrl.getInstance( context ).getIPenCtrl().inputPassword( pass );
//                        signal.countDown();
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
    }

    @Test
    public void PenConnect ()
    {
        SharedPreferences mPref = PreferenceManager.getDefaultSharedPreferences( context );

        String address = mPref.getString( Const.Setting.KEY_MAC_ADDRESS, null );
//        address="";
        assertNotNull( address );

        boolean isAvailableDevice = false;
        try
        {
            isAvailableDevice = PenClientCtrl.getInstance( context ).isAvailableDevice( address );
        }catch ( Exception e )
        {
            e.printStackTrace();
        }

        assertTrue(isAvailableDevice);
//        boolean isException = false;
//        try
//        {
//            PenClientCtrl.getInstance( context ).setLeMode( true );
//            isAvailableDevice = PenClientCtrl.getInstance( context ).isAvailableDevice( address );
//        }catch ( Exception e )
//        {
//            e.printStackTrace();
//            isException = true;
//        }
//        assertTrue(isException);
//
//        isException = false;
//        try
//        {
//            PenClientCtrl.getInstance( context ).setLeMode( false );
//            isAvailableDevice = PenClientCtrl.getInstance( context ).isAvailableDevice( address );
//        }catch ( Exception e )
//        {
//            e.printStackTrace();
//            isException = true;
//        }
//        assertFalse(isException);
        if(PenClientCtrl.getInstance( context ).getConnectDevice() == null)
        {
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
            assertNotNull( connectedAddress );
        }
        signal = new CountDownLatch( 1 );
        PenClientCtrl.getInstance( context ).disconnect();
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertNull( PenClientCtrl.getInstance( context ).getConnectDevice() );
    }

    @After
    public void close()
    {
        context.unregisterReceiver( connectBroadcastReceiver );

    }
}
