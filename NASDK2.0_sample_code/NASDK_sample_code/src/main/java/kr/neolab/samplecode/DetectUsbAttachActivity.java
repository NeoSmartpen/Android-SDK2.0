package kr.neolab.samplecode;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.Nullable;

import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.pen.PenCtrl;
import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.usb.UsbAdt;

public class DetectUsbAttachActivity extends Activity {


    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable mConnectRunnable = new Runnable() {
        @Override
        public void run() {
            PenClientCtrl.getInstance(getApplicationContext()).setAdtMode(PenCtrl.AdtMode.USB_MODE);
            PenClientCtrl.getInstance(getApplicationContext()).setContext(getApplicationContext());
            try {
                PenClientCtrl.getInstance(getApplicationContext()).connect("");
            } catch (BLENotSupportedException e) {
                throw new RuntimeException(e);
            }

            // 무조건 MainActivity 호출
            Intent intent = new Intent(DetectUsbAttachActivity.this, MainActivity.class);
            // from usb
            intent.putExtra("fromUsb", true);
            startActivity(intent);
            finish();
        }
    };

    private Runnable mFinishRunnable = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent )
        {
            String action = intent.getAction();

            if ( UsbAdt.ACTION_USB_ATTACHED.equals( action ) )
            {
                mHandler.removeCallbacks(mFinishRunnable);
                mHandler.postDelayed(mConnectRunnable, 3000);
            }
            else if ( UsbAdt.ACTION_USB_DETACHED.equals( action ) )
            {
                mHandler.removeCallbacks(mConnectRunnable);
                mHandler.postDelayed(mFinishRunnable, 1000);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);
        IntentFilter filter = new IntentFilter(UsbAdt.ACTION_USB_ATTACHED);
        filter.addAction(UsbAdt.ACTION_USB_DETACHED);
        registerReceiver(mBroadcastReceiver, filter );
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHandler.postDelayed(mConnectRunnable, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        mHandler.removeCallbacks(mConnectRunnable);
    }
}
