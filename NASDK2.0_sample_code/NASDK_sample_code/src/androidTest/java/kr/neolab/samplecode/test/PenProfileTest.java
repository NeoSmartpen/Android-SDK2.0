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
import android.util.Base64;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.concurrent.CountDownLatch;

import kr.neolab.samplecode.Const;
import kr.neolab.samplecode.PenClientCtrl;
import kr.neolab.sdk.pen.bluetooth.lib.ByteConverter;
import kr.neolab.sdk.pen.bluetooth.lib.PenProfile;
import kr.neolab.sdk.pen.bluetooth.lib.ProfileKeyValueLimitException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_STATUS_EXIST_PROFILE_ALREADY;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_STATUS_NO_EXIST_KEY;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_STATUS_NO_EXIST_PROFILE;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_STATUS_SUCCESS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by LMS on 2017-08-16.
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder( MethodSorters.NAME_ASCENDING)
public class PenProfileTest
{
//    @Rule
//    public ActivityTestRule<DeviceListActivity> mActivityRule = new ActivityTestRule<>( DeviceListActivity.class );

    CountDownLatch signal = new CountDownLatch( 1 );
    CountDownLatch infoProfileSignal = new CountDownLatch( 1 );
    CountDownLatch createProfileSignal = new CountDownLatch( 1 );
    CountDownLatch deleteProfileSignal = new CountDownLatch( 1 );
    CountDownLatch writeValueSignal = new CountDownLatch( 1 );
    CountDownLatch readValueSignal = new CountDownLatch( 1 );
    CountDownLatch deleteValueSignal = new CountDownLatch( 1 );

    final String TEST_PROFILE_NAME = "neolab_t";

    final byte[] TEST_PROFILE_PASSWORD = { (byte)0x3e, (byte)0xd5, (byte)0x95, (byte)0x25, (byte)0x6, (byte)0xf7, (byte)0x83, (byte)0xdd };//"neolab_t";


    final String TEST_KEY1 = "testKey1";
    final String TEST_KEY2 = "testKey2";
    final String TEST_KEY_FAIL= "testKeyFail";


    final String TEST_VALUE1 = "testVal1";
    final String TEST_VALUE2 = "testVal2";

    boolean isSuccess = false;
    JSONObject profileObj = null;
    Context context = null;
    int apiStatus = -1;
    String profileName = "";

//    boolean isMakeProfile = false;

//    boolean isSupport = false;


    BroadcastReceiver connectBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive ( Context context, Intent intent )
        {
            if ( Const.Broadcast.ACTION_PEN_MESSAGE.equals( intent.getAction() ) )
            {
                int penMsgType = intent.getIntExtra( Const.Broadcast.MESSAGE_TYPE, 0 );
                String content = intent.getStringExtra( Const.Broadcast.CONTENT );

                NLog.d( "PenProfileTest connectBroadcastReceiver penMsgType="+penMsgType+",content="+content );
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
                    case PenMsgType.PROFILE_DELETE:
                        try
                        {
                            profileObj = new JSONObject( content );
                            deleteProfileSignal.countDown();
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
                            deleteValueSignal.countDown();
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
                            readValueSignal.countDown();
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
                            writeValueSignal.countDown();
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
                            createProfileSignal.countDown();
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
                            infoProfileSignal.countDown();
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
            NLog.d( "PenProfileTest connectedAddress = "+connectedAddress );
            assertNotNull( connectedAddress );
        }
    }

    @Test
    public void aInfoTest ()
    {
        NLog.d( "PenProfileTest InfoTest Start");
        infoProfileSignal = null;
        infoProfileSignal = new CountDownLatch( 1 );
        new Handler( Looper.getMainLooper()).postDelayed( new Runnable()
        {
            @Override
            public void run ()
            {
                infoProfileSignal.countDown();
            }
        },500 );
        try
        {
            infoProfileSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        infoProfileSignal = null;
        infoProfileSignal = new CountDownLatch( 1 );

        if(PenClientCtrl.getInstance( context ).getIPenCtrl().isSupportPenProfile())
        {
            boolean isException = false;
            try
            {
                PenClientCtrl.getInstance( context ).getIPenCtrl().getProfileInfo( "1234567890" );
            }
            catch ( ProtocolNotSupportedException e )
            {
                e.printStackTrace();
            }
            catch ( ProfileKeyValueLimitException e1)
            {
                e1.printStackTrace();
                isException = true;
            }
            Assert.assertTrue( isException );

            try
            {
                PenClientCtrl.getInstance( context ).getIPenCtrl().getProfileInfo( TEST_PROFILE_NAME );
            }
            catch ( ProtocolNotSupportedException e )
            {
                e.printStackTrace();
            }
            catch ( ProfileKeyValueLimitException e1)
            {
                e1.printStackTrace();
            }


            try
            {
                infoProfileSignal.await();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            try
            {
                profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );

                Assert.assertEquals( TEST_PROFILE_NAME, profileName);

                apiStatus = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
            }
            catch ( JSONException e )
            {
                e.printStackTrace();
            }

        }
        else
        {
            boolean exception = false;
            try
            {
                PenClientCtrl.getInstance( context ).getIPenCtrl().getProfileInfo( TEST_PROFILE_NAME );
            }
            catch ( ProtocolNotSupportedException e )
            {
                exception = true;
                e.printStackTrace();
            }
            catch ( ProfileKeyValueLimitException e1)
            {
                e1.printStackTrace();
            }

            assertTrue( exception );
            NLog.d( "PenProfileTest isSupportPenProfile ="+false );
        }
        bCreateProfile();
    }

//    @Test
    public void bCreateProfile()
    {
        NLog.d( "PenProfileTest createProfile Start");
        //Default profile 존재함
        if(apiStatus == PROFILE_STATUS_SUCCESS)
        {
            /// 존재하는 프로파일 생성관련 테스트
            boolean isException = false;

            try
            {
                PenClientCtrl.getInstance( context ).getIPenCtrl().createProfile( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD /*"1234567890"*/ );
            }
            catch ( ProtocolNotSupportedException e )
            {
                e.printStackTrace();
            }
            catch ( ProfileKeyValueLimitException e1)
            {
                e1.printStackTrace();
                isException = true;
            }
            Assert.assertTrue( isException );

            try
            {
                PenClientCtrl.getInstance( context ).getIPenCtrl().createProfile( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD );
            }
            catch ( ProtocolNotSupportedException e )
            {
                e.printStackTrace();
            }
            catch ( ProfileKeyValueLimitException e1)
            {
                e1.printStackTrace();
            }


            try
            {
                createProfileSignal.await();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            try
            {
                profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );

                Assert.assertEquals( TEST_PROFILE_NAME, profileName);

                apiStatus = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS );

            }
            catch ( JSONException e )
            {
                e.printStackTrace();
            }
            Assert.assertEquals( apiStatus, PROFILE_STATUS_EXIST_PROFILE_ALREADY );

        }
        else if(apiStatus == PROFILE_STATUS_NO_EXIST_PROFILE)
        {
            ///////////////////  비번테스트
//            try
//            {
//                PenClientCtrl.getInstance( context ).getIPenCtrl().createProfile( TEST_PROFILE_NAME, "fail" );
//            }
//            catch ( ProtocolNotSupportedException e )
//            {
//                e.printStackTrace();
//            }
//            try
//            {
//                createProfileSignal.await();
//            }
//            catch ( Exception e )
//            {
//                e.printStackTrace();
//            }
//            try
//            {
//                profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
//
//                Assert.assertEquals( TEST_PROFILE_NAME, profileName);
//
//                apiStatus = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
//
//            }
//            catch ( JSONException e )
//            {
//                e.printStackTrace();
//            }
//            Assert.assertEquals( apiStatus, PROFILE_STATUS_NO_PERMISSION );
            /////////////// 정상 create 테스트
            apiStatus = -1;
            createProfileSignal = null;
            createProfileSignal = new CountDownLatch( 1 );
            try
            {
                PenClientCtrl.getInstance( context ).getIPenCtrl().createProfile( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD );
            }
            catch ( ProtocolNotSupportedException e )
            {
                e.printStackTrace();
            }
            catch ( ProfileKeyValueLimitException e1)
            {
                e1.printStackTrace();
            }

            try
            {
                createProfileSignal.await();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            try
            {
                profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );

                Assert.assertEquals( TEST_PROFILE_NAME, profileName);

                apiStatus = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS );

            }
            catch ( JSONException e )
            {
                e.printStackTrace();
            }
            Assert.assertEquals( apiStatus, PROFILE_STATUS_SUCCESS );
        }
        else
        {
            NLog.d( "PenProfileTest isSupportPenProfile ="+false );
            return;
        }


    }

    @Test
    public void cWriteValueTest ()
    {
        NLog.d( "PenProfileTest writeValueTest Start");
        writeValueSignal = null;
        writeValueSignal = new CountDownLatch( 1 );
        new Handler( Looper.getMainLooper()).postDelayed( new Runnable()
        {
            @Override
            public void run ()
            {
                writeValueSignal.countDown();
            }
        },500 );
        try
        {
            writeValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        writeValueSignal = null;
        writeValueSignal = new CountDownLatch( 1 );

        apiStatus = -1;
        String[] keys = {TEST_KEY1,"12345678901234567890"};
        String[] values = {TEST_VALUE1,TEST_VALUE2};
        byte[][] data = new byte[2][];
        data[0] = values[0].getBytes();
        data[1] = values[1].getBytes();
        boolean isException = false;
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, keys, data );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
            isException = true;
        }

        Assert.assertTrue( isException );
        keys[1] = TEST_KEY2;
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, keys, data );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
            isException = true;
        }

        try
        {
            writeValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
            Assert.assertEquals( TEST_PROFILE_NAME, profileName);

            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
            Assert.assertEquals( keys.length, array.length());

            for(int i = 0; i < array.length(); i++)
            {
                JSONObject obj = array.getJSONObject( i );
                String key = obj.getString( JsonTag.STRING_PROFILE_KEY );
                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                Assert.assertEquals( status, PROFILE_STATUS_SUCCESS );
            }
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }


        ////////////////////////////////// 없는 프로파일 네임 테스트
        writeValueSignal = null;
        writeValueSignal = new CountDownLatch( 1 );

        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( "fail", TEST_PROFILE_PASSWORD, keys, data );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            writeValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
            Assert.assertEquals( "fail", profileName);

            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
            Assert.assertEquals( keys.length, array.length());

            for(int i = 0; i < array.length(); i++)
            {
                JSONObject obj = array.getJSONObject( i );
                String key = obj.getString( JsonTag.STRING_PROFILE_KEY );
                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                Assert.assertEquals( status, PROFILE_STATUS_NO_EXIST_PROFILE );
            }
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }
        ////////////////////////////////// 비번 테스트
//        writeValueSignal = null;
//        writeValueSignal = new CountDownLatch( 1 );
//
//        try
//        {
//            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, "fail", keys, data );
//        }
//        catch ( ProtocolNotSupportedException e )
//        {
//            e.printStackTrace();
//        }
//        catch ( ProfileKeyValueLimitException e1)
//        {
//            e1.printStackTrace();
//        }
//
//        try
//        {
//            writeValueSignal.await();
//        }
//        catch ( Exception e )
//        {
//            e.printStackTrace();
//        }
//
//
//        try
//        {
//            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
//            Assert.assertEquals( TEST_PROFILE_NAME, profileName);
//
//            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
//            Assert.assertEquals( keys.length, array.length());
//
//            for(int i = 0; i < array.length(); i++)
//            {
//                JSONObject obj = array.getJSONObject( i );
//                String key = obj.getString( JsonTag.STRING_PROFILE_KEY );
//                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
//                Assert.assertEquals( status, PROFILE_STATUS_NO_PERMISSION );
//            }
//        }
//        catch ( JSONException e )
//        {
//            e.printStackTrace();
//        }



        ////////////////////// Neolab Define Key Limit Test //////////////////////////////////////


        //////////////////  KEY_PEN_NAME Test
        String[] testKeys = { PenProfile.KEY_PEN_NAME};
        String[] testValues = {"12345678901234567890123456789012345678901234567890123456789012345678901234567890"};//80byte
        byte[][] testData = new byte[1][];
        testData[0] = testValues[0].getBytes();
        isException = false;
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, testKeys, testData );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
            isException = true;
        }

        Assert.assertTrue( isException );

        //////////////////  KEY_PEN_STROKE_THICKNESS_LEVEL Test
        testKeys[0] = PenProfile.KEY_PEN_STROKE_THICKNESS_LEVEL;
        testValues[0] = "12";//2 byte
        testData[0] = testValues[0].getBytes();
        isException = false;
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, testKeys, testData );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
            isException = true;
        }

        Assert.assertTrue( isException );
        //////////////////  KEY_PEN_COLOR_AND_HISTORY Test
        testKeys[0] = PenProfile.KEY_PEN_COLOR_AND_HISTORY;

        testValues[0] = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"+
        "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";//200 byte
        testData[0] = testValues[0].getBytes();
        isException = false;
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, testKeys, testData );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
            isException = true;
        }

        Assert.assertTrue( isException );

        //////////////////  KEY_USER_CALIBRATION Test
        testKeys[0] = PenProfile.KEY_USER_CALIBRATION;
        testValues[0] = "12345678901234567890";//20 byte
        testData[0] = testValues[0].getBytes();
        isException = false;
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, testKeys, testData );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
            isException = true;
        }

        Assert.assertTrue( isException );

        //////////////////  KEY_PEN_BRUSH_TYPE Test
        testKeys[0] = PenProfile.KEY_PEN_BRUSH_TYPE;
        testValues[0] = "12";//2 byte
        testData[0] = testValues[0].getBytes();
        isException = false;
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, testKeys, testData );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
            isException = true;
        }

        Assert.assertTrue( isException );

        //////////////////  KEY_PEN_TIP_TYPE Test
        testKeys[0] = PenProfile.KEY_PEN_TIP_TYPE;
        testValues[0] = "12";//2 byte
        testData[0] = testValues[0].getBytes();
        isException = false;
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, testKeys, testData );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
            isException = true;
        }

        Assert.assertTrue( isException );


    }

    @Test
    public void dReadValueTest ()
    {
        NLog.d( "PenProfileTest readValueTest Start");

        readValueSignal = null;
        readValueSignal = new CountDownLatch( 1 );
        new Handler( Looper.getMainLooper()).postDelayed( new Runnable()
        {
            @Override
            public void run ()
            {
                readValueSignal.countDown();
            }
        },500 );
        try
        {
            readValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        readValueSignal = null;
        readValueSignal = new CountDownLatch( 1 );


        apiStatus = -1;
        String[] keys = {TEST_KEY1,TEST_KEY2,TEST_KEY_FAIL};
        /// 정상키, 없는키 섞어서 테스트
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().readProfileValue( TEST_PROFILE_NAME, keys);
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            readValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
            Assert.assertEquals( TEST_PROFILE_NAME, profileName);

            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
            Assert.assertEquals( keys.length, array.length());

            for(int i = 0; i < array.length(); i++)
            {
                JSONObject obj = array.getJSONObject( i );
                String key = obj.getString( JsonTag.STRING_PROFILE_KEY );
                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                String value = null;
                if(status == PROFILE_STATUS_SUCCESS)
                {
                    String data = (String)obj.get( JsonTag.BYTE_PROFILE_VALUE );
                    byte[] decodeByte = Base64.decode(data ,Base64.DEFAULT);

                    value = new String( decodeByte );
                }
                if(key.equals( TEST_KEY1 ))
                {
                    Assert.assertEquals( status, PROFILE_STATUS_SUCCESS);
                    Assert.assertEquals( value, TEST_VALUE1);
                }
                else if(key.equals( TEST_KEY2 ))
                {
                    Assert.assertEquals( status, PROFILE_STATUS_SUCCESS);
                    Assert.assertEquals( value, TEST_VALUE2);
                }
                else if(key.equals( TEST_KEY_FAIL ))
                {
                    Assert.assertEquals( status, PROFILE_STATUS_NO_EXIST_KEY);
                }
            }
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }
        ////////////////////////////////// 없는 프로파일 네임 테스트
        readValueSignal = null;
        readValueSignal = new CountDownLatch( 1 );
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().readProfileValue( "fail", keys);
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            readValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
            Assert.assertEquals( TEST_PROFILE_NAME, profileName);

            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
            Assert.assertEquals( keys.length, array.length());

            for(int i = 0; i < array.length(); i++)
            {
                JSONObject obj = array.getJSONObject( i );
                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                Assert.assertEquals( status, PROFILE_STATUS_NO_EXIST_PROFILE);
            }
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }

        /////// 리틀 엔디안 테스트

        readValueSignal = null;
        readValueSignal = new CountDownLatch( 1 );

        apiStatus = -1;
        String[] write_keys = {TEST_KEY1};
        int testValueInt = 1234;
        byte[][] data = new byte[1][];
        data[0] = ByteConverter.intTobyte( testValueInt );
        boolean isException = false;
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().writeProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, write_keys, data );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
            isException = true;
        }
        try
        {
            readValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
            Assert.assertEquals( TEST_PROFILE_NAME, profileName);

            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
            Assert.assertEquals( keys.length, array.length());

            for(int i = 0; i < array.length(); i++)
            {
                JSONObject obj = array.getJSONObject( i );
                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                Assert.assertEquals( status, PROFILE_STATUS_SUCCESS);
            }
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }



        //다시 읽기
        readValueSignal = null;
        readValueSignal = new CountDownLatch( 1 );
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().readProfileValue( TEST_PROFILE_NAME, write_keys);
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            readValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
            Assert.assertEquals( TEST_PROFILE_NAME, profileName);

            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
            Assert.assertEquals( keys.length, array.length());

            for(int i = 0; i < array.length(); i++)
            {
                JSONObject obj = array.getJSONObject( i );
                String key = obj.getString( JsonTag.STRING_PROFILE_KEY );
                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                byte[] decodeByte = null;
                if(status == PROFILE_STATUS_SUCCESS)
                {
                    String strData = (String)obj.get( JsonTag.BYTE_PROFILE_VALUE );
                    decodeByte = Base64.decode(strData ,Base64.DEFAULT);
                }
                if(key.equals( TEST_KEY1 ))
                {
                    int value = ByteConverter.byteArrayToInt( decodeByte );
                    Assert.assertEquals( status, PROFILE_STATUS_SUCCESS);
                    Assert.assertEquals( value, testValueInt);
                }
            }
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }


    }

    @Test
    public void eDeleteValueTest ()
    {

        NLog.d( "PenProfileTest deleteValueTest Start");

        apiStatus = -1;
        String[] keys = {TEST_KEY1,TEST_KEY2};

        deleteValueSignal = null;
        deleteValueSignal = new CountDownLatch( 1 );
        new Handler( Looper.getMainLooper()).postDelayed( new Runnable()
        {
            @Override
            public void run ()
            {
                deleteValueSignal.countDown();
            }
        },500 );
        try
        {
            deleteValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        ////////////////////////////////// 비번 테스트
//        deleteValueSignal = null;
//        deleteValueSignal = new CountDownLatch( 1 );
//
//        try
//        {
//            PenClientCtrl.getInstance( context ).getIPenCtrl().deleteProfileValue( TEST_PROFILE_NAME, "fail", keys);
//        }
//        catch ( ProtocolNotSupportedException e )
//        {
//            e.printStackTrace();
//        }
//        try
//        {
//            deleteValueSignal.await();
//        }
//        catch ( Exception e )
//        {
//            e.printStackTrace();
//        }
//
//        try
//        {
//            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
//            Assert.assertEquals( TEST_PROFILE_NAME, profileName);
//
//            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
//            Assert.assertEquals( keys.length, array.length());
//
//            for(int i = 0; i < array.length(); i++)
//            {
//                JSONObject obj = array.getJSONObject( i );
//                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
//                Assert.assertEquals( status, PROFILE_STATUS_NO_PERMISSION);
//            }
//        }
//        catch ( JSONException e )
//        {
//            e.printStackTrace();
//        }

        ////////////////////////////////// 없는 프로파일 네임 테스트
        deleteValueSignal = null;
        deleteValueSignal = new CountDownLatch( 1 );

        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().deleteProfileValue( "fail", TEST_PROFILE_PASSWORD, keys);
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            deleteValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
            Assert.assertEquals( "fail", profileName);

            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
            Assert.assertEquals( keys.length, array.length());

            for(int i = 0; i < array.length(); i++)
            {
                JSONObject obj = array.getJSONObject( i );
                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                Assert.assertEquals( status, PROFILE_STATUS_NO_EXIST_PROFILE);
            }
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }

        /// 정상키, 없는키 섞어서 테스트
        String[] test_keys = {TEST_KEY1,TEST_KEY2,TEST_KEY_FAIL};
        apiStatus = -1;
        deleteValueSignal = null;
        deleteValueSignal = new CountDownLatch( 1 );

        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().deleteProfileValue( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD, test_keys);
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            deleteValueSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
            Assert.assertEquals( TEST_PROFILE_NAME, profileName);

            JSONArray array = profileObj.getJSONArray( JsonTag.ARRAY_PROFILE_RES );
            Assert.assertEquals( test_keys.length, array.length());

            for(int i = 0; i < array.length(); i++)
            {
                JSONObject obj = array.getJSONObject( i );
                String key = obj.getString( JsonTag.STRING_PROFILE_KEY );
                int status = obj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
                if(key.equals( TEST_KEY1 ))
                {
                    Assert.assertEquals( status, PROFILE_STATUS_SUCCESS);
                }
                else if(key.equals( TEST_KEY2 ))
                {
                    Assert.assertEquals( status, PROFILE_STATUS_SUCCESS);
                }
                else if(key.equals( TEST_KEY_FAIL ))
                {
                    Assert.assertEquals( status, PROFILE_STATUS_NO_EXIST_KEY);
                }
            }
        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }


    }

    @Test
    public void fDeleteProfileTest ()
    {
        NLog.d( "PenProfileTest deleteProfileTest Start");
        ////////////////////// 없는 프로파일 네임 테스트
        apiStatus = -1;
        deleteProfileSignal = null;
        deleteProfileSignal = new CountDownLatch( 1 );
        new Handler( Looper.getMainLooper()).postDelayed( new Runnable()
        {
            @Override
            public void run ()
            {
                deleteProfileSignal.countDown();
            }
        },500 );
        try
        {
            deleteProfileSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        deleteProfileSignal = null;
        deleteProfileSignal = new CountDownLatch( 1 );
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().deleteProfile( "fail", TEST_PROFILE_PASSWORD );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            deleteProfileSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );

            Assert.assertEquals( "fail", profileName);

            apiStatus = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS );

        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }
        Assert.assertEquals( apiStatus, PROFILE_STATUS_NO_EXIST_PROFILE );
        /////////////////////////// 비번 테스트
//        apiStatus = -1;
//        deleteProfileSignal = null;
//        deleteProfileSignal = new CountDownLatch( 1 );
//        try
//        {
//            PenClientCtrl.getInstance( context ).getIPenCtrl().deleteProfile( TEST_PROFILE_NAME, "fail" );
//        }
//        catch ( ProtocolNotSupportedException e )
//        {
//            e.printStackTrace();
//        }
//        try
//        {
//            deleteProfileSignal.await();
//        }
//        catch ( Exception e )
//        {
//            e.printStackTrace();
//        }
//        try
//        {
//            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );
//
//            Assert.assertEquals( TEST_PROFILE_NAME, profileName);
//
//            apiStatus = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS );
//
//        }
//        catch ( JSONException e )
//        {
//            e.printStackTrace();
//        }
//        Assert.assertEquals( apiStatus, PROFILE_STATUS_NO_PERMISSION );

        // 정상 테스트
        apiStatus = -1;
        deleteProfileSignal = null;
        deleteProfileSignal = new CountDownLatch( 1 );
        try
        {
            PenClientCtrl.getInstance( context ).getIPenCtrl().deleteProfile( TEST_PROFILE_NAME, TEST_PROFILE_PASSWORD );
        }
        catch ( ProtocolNotSupportedException e )
        {
            e.printStackTrace();
        }
        catch ( ProfileKeyValueLimitException e1)
        {
            e1.printStackTrace();
        }

        try
        {
            deleteProfileSignal.await();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        try
        {
            profileName = profileObj.getString( JsonTag.STRING_PROFILE_NAME );

            Assert.assertEquals( TEST_PROFILE_NAME, profileName);

            apiStatus = profileObj.getInt( JsonTag.INT_PROFILE_RES_STATUS );

        }
        catch ( JSONException e )
        {
            e.printStackTrace();
        }
        Assert.assertEquals( apiStatus, PROFILE_STATUS_SUCCESS );



    }

    @After
    public void close()
    {
        context.unregisterReceiver( connectBroadcastReceiver );
        PenClientCtrl.getInstance( context ).disconnect();
    }


}
