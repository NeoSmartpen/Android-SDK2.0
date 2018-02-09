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

import java.util.concurrent.CountDownLatch;

import kr.neolab.samplecode.Const;
import kr.neolab.samplecode.PenClientCtrl;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by LMS on 2017-08-16.
 */
@RunWith(AndroidJUnit4.class)
public class PenSettingTest
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
                    case PenMsgType.PEN_SETUP_AUTO_POWER_ON_RESULT:
                    case PenMsgType.PEN_SETUP_BEEP_RESULT:
                    case PenMsgType.PEN_SETUP_PEN_CAP_OFF:
                    case PenMsgType.PEN_SETUP_PEN_COLOR_RESULT:
                    case PenMsgType.PEN_SETUP_HOVER_ONOFF:
                    case PenMsgType.PEN_SETUP_OFFLINEDATA_SAVE_ONOFF:
                    case PenMsgType.PEN_SETUP_AUTO_SHUTDOWN_RESULT:
                    case PenMsgType.PEN_SETUP_SENSITIVITY_RESULT:
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
                    case PenMsgType.PEN_STATUS:
                        try
                        {
                            NLog.d( "PenSettingTest STATUS:" +content);
                            statusObj = null;
                            statusObj = new JSONObject( content );
                            assertNotNull( statusObj );
                            stateSignal.countDown();
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
    public void PenAutoPowerSettingTest ()
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


        stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        NLog.d( "PenSettingTest reqPenStatus");
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        boolean statusResult = false;
        try
        {
            statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_AUTO_POWER_ON );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        boolean testAutoPowerOn = !statusResult;

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).reqSetupAutoPowerOnOff( testAutoPowerOn );
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        NLog.d( "PenSettingTest reqSetupAutoPowerOnOff = "+isSettingSuccess );
        assertTrue( isSettingSuccess);

        stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        NLog.d( "PenSettingTest reqPenStatus");
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        try
        {
            statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_AUTO_POWER_ON );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertEquals(testAutoPowerOn,  statusResult);

        testAutoPowerOn = !testAutoPowerOn;

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).reqSetupAutoPowerOnOff( testAutoPowerOn );
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isSettingSuccess);
        stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        NLog.d( "PenSettingTest reqPenStatus");
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        try
        {
            statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_AUTO_POWER_ON );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertEquals(testAutoPowerOn,  statusResult);

    }


    @Test
    public void PenAutoShutdownTest ()
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

        stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        NLog.d( "PenSettingTest reqPenStatus");
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        int statusResult = 0;
        try
        {
            statusResult = statusObj.getInt( Const.JsonTag.INT_AUTO_POWER_OFF_TIME );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        int testAutoShutdown = 20;
        if(statusResult == testAutoShutdown)
            testAutoShutdown = 10;

        NLog.d( "PenSettingTest reqSetupAutoShutdownTime = "+testAutoShutdown+",statusResult="+statusResult );
        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).reqSetupAutoShutdownTime( (short)testAutoShutdown );
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isSettingSuccess);

        stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        NLog.d( "PenSettingTest reqPenStatus");
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        try
        {
            statusResult = statusObj.getInt( Const.JsonTag.INT_AUTO_POWER_OFF_TIME );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        NLog.d( "PenSettingTest testAutoShutdown = "+testAutoShutdown+",statusResult="+statusResult );
        assertEquals(testAutoShutdown,  statusResult);
    }

    @Test
    public void PenSensitivityTest ()
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

        stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        int statusResult = 0;
        try
        {
            statusResult = statusObj.getInt( Const.JsonTag.INT_PEN_SENSITIVITY );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        int testSensitivity = 1;
        if(statusResult == testSensitivity)
            testSensitivity = 4;

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).reqSetupPenSensitivity( (short)testSensitivity );
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isSettingSuccess);

          stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        try
        {
            statusResult = statusObj.getInt( Const.JsonTag.INT_PEN_SENSITIVITY );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertEquals(testSensitivity,  statusResult);
        testSensitivity = 4;
        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).reqSetupPenSensitivity( (short)testSensitivity );
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isSettingSuccess);

    }

    @Test
    public void PenSensitivityNotMatchPressSensorTest ()
    {

//        signal = new CountDownLatch( 1 );
//        new Handler( Looper.getMainLooper()).postDelayed( new Runnable()
//        {
//            @Override
//            public void run ()
//            {
//                signal.countDown();
//            }
//        },300 );
//        try
//        {
//            signal.await();
//        }
//        catch ( Exception e )
//        {
//            e.printStackTrace();
//        }
//
//        stateSignal = null;
//        stateSignal = new CountDownLatch( 1 );
//        if(PenClientCtrl.getInstance( context ).getProtocolVersion() == 1)
//            return;
//        int sensorType = -1;
//        api
//        try
//        {
//            sensorType = ((PenCtrl)PenClientCtrl.getInstance( context ).getIPenCtrl()).getPressSensorType();
//        }catch ( Exception e )
//        {
//            e.printStackTrace();
//        }
//        if(sensorType == 0)
//        {
//            try
//            {
//                PenClientCtrl.getInstance( context ).getIPenCtrl().reqSetupPenSensitivityFSC(((short)2));
//            }catch ( Exception e )
//            {
//                e.printStackTrace();
//            }
//
//        }


    }


    @Test
    public void PenBeepTest ()
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

        stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        boolean statusResult = false;
        try
        {
            statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_BEEP );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        boolean testBeep = !statusResult;

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).reqSetupPenBeepOnOff( testBeep );
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isSettingSuccess);

          stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        try
        {
            statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_BEEP );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertEquals(testBeep,  statusResult);

        testBeep = !testBeep;

        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).reqSetupPenBeepOnOff( testBeep );
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isSettingSuccess);

          stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        try
        {
            statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_BEEP );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertEquals(testBeep,  statusResult);
    }

    @Test
    public void PenColorTest ()
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

        stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        int intStatusResult = -1;
        try
        {
            intStatusResult = statusObj.getInt( Const.JsonTag.INT_PEN_COLOR );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        int intTestColor = 0xFF0000FF;
        if(intTestColor == intStatusResult)
            intTestColor = 0xFFFF0000;
        signal = new CountDownLatch( 1 );
        isSettingSuccess = false;
        PenClientCtrl.getInstance( context ).reqSetupPenTipColor( intTestColor );
        try
        {
            signal.await(); 
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertTrue( isSettingSuccess);

          stateSignal = null;
        stateSignal = new CountDownLatch( 1 );
        PenClientCtrl.getInstance( context ).reqPenStatus();
        try
        {
            stateSignal.await(); 
            intStatusResult = statusObj.getInt( Const.JsonTag.INT_PEN_COLOR );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        assertEquals(intTestColor,  intStatusResult);
    }


    @Test
    public void PenCapTest ()
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

        // 1.0 은 지원하지 않음
        if(PenClientCtrl.getInstance( context ).getProtocolVersion() == 1)
        {
            return;
        }
        else
        {
          stateSignal = null;
            stateSignal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).reqPenStatus();
            try
            {
                stateSignal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            boolean statusResult = false;
            try
            {
                statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_PEN_CAP_ON );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }

            boolean testPenCap = !statusResult;

            signal = new CountDownLatch( 1 );
            isSettingSuccess = false;
            PenClientCtrl.getInstance( context ).reqSetupPenCapOnOff( testPenCap );
            try
            {
                signal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertTrue( isSettingSuccess);

          stateSignal = null;
            stateSignal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).reqPenStatus();
            try
            {
                stateSignal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            try
            {
                statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_PEN_CAP_ON );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertEquals(testPenCap,  statusResult);

            testPenCap = !testPenCap;

            signal = new CountDownLatch( 1 );
            isSettingSuccess = false;
            PenClientCtrl.getInstance( context ).reqSetupPenCapOnOff( testPenCap );
            try
            {
                signal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertTrue( isSettingSuccess);

          stateSignal = null;
            stateSignal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).reqPenStatus();
            try
            {
                stateSignal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            try
            {
                statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_PEN_CAP_ON );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertEquals(testPenCap,  statusResult);

        }
    }

    @Test
    public void PenHoverTest ()
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

        // 1.0 은 지원하지 않음
        if(PenClientCtrl.getInstance( context ).getProtocolVersion() == 1)
        {
            return;
        }
        else
        {
          stateSignal = null;
            stateSignal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).reqPenStatus();
            try
            {
                stateSignal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            boolean statusResult = false;
            try
            {
                statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_HOVER );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }

            boolean testHover = !statusResult;

            signal = new CountDownLatch( 1 );
            isSettingSuccess = false;
            PenClientCtrl.getInstance( context ).reqSetupPenHover( testHover );
            try
            {
                signal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertTrue( isSettingSuccess);

          stateSignal = null;
            stateSignal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).reqPenStatus();
            try
            {
                stateSignal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            try
            {
                statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_HOVER );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertEquals(testHover,  statusResult);

            testHover = !testHover;

            signal = new CountDownLatch( 1 );
            isSettingSuccess = false;
            PenClientCtrl.getInstance( context ).reqSetupPenHover( testHover );
            try
            {
                signal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertTrue( isSettingSuccess);

            stateSignal = null;
            stateSignal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).reqPenStatus();
            try
            {
                stateSignal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            try
            {
                statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_HOVER );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertEquals(testHover,  statusResult);

        }
    }


    @Test
    public void PenOfflineDataSaveTest ()
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

        // 1.0 은 지원하지 않음
        if(PenClientCtrl.getInstance( context ).getProtocolVersion() == 1)
        {
            return;
        }
        else
        {
            stateSignal = null;
            stateSignal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).reqPenStatus();
            try
            {
                stateSignal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            boolean statusResult = false;
            try
            {
                statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_OFFLINE_DATA_SAVE );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }

            boolean testOfflineDataSave = !statusResult;

            signal = new CountDownLatch( 1 );
            isSettingSuccess = false;
            PenClientCtrl.getInstance( context ).setAllowOfflineData( testOfflineDataSave );
            try
            {
                signal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertTrue( isSettingSuccess);

            stateSignal = null;
            stateSignal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).reqPenStatus();
            try
            {
                stateSignal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            try
            {
                statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_OFFLINE_DATA_SAVE );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertEquals(testOfflineDataSave,  statusResult);

            testOfflineDataSave = !testOfflineDataSave;

            signal = new CountDownLatch( 1 );
            isSettingSuccess = false;
            PenClientCtrl.getInstance( context ).setAllowOfflineData( testOfflineDataSave );
            try
            {
                signal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertTrue( isSettingSuccess);

            stateSignal = null;
            stateSignal = new CountDownLatch( 1 );
            PenClientCtrl.getInstance( context ).reqPenStatus();
            try
            {
                stateSignal.await(); 
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            try
            {
                statusResult = statusObj.getBoolean( Const.JsonTag.BOOL_OFFLINE_DATA_SAVE );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            assertEquals(testOfflineDataSave,  statusResult);

        }
    }

    @After
    public void close()
    {
        context.unregisterReceiver( connectBroadcastReceiver );
    }


}
