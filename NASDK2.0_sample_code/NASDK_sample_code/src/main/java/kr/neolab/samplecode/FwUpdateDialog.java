package kr.neolab.samplecode;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

/**
 * Created by LMS on 2016-11-24.
 */

public class FwUpdateDialog extends Dialog
//        implements OnCheckListener,OnDownLoadListener
{

    // this sample shows how the pen updates fw file on server.
    // If you want to update fw files, you must implement file server.
    // after implement file server, refer assets files to upload file on server.
//    private static final String FW_VERSION_CHK_FILE_F50 = "protocol2.0_firmware_f50.properties";
//    private static final String FW_VERSION_CHK_FILE_F120 = "protocol2.0_firmware.properties";
//    private static final String FW_VERSION_CHK_FILE_F110 = "f1xx_firmware.properties";

    private static final String TAG = "FwUpdateDialog";

    // this server url is sample.
//    private static final String FW_BASE_URL = "http://domain/";
//    private static final String FW_BASE_URL_20 = "http://domain_fw20/";
//
//    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";

    private static final String CHANNEL_ID = "fw_update_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int PERMISSION_REQUEST_CODE = 123;

    protected NotificationCompat.Builder mBuilder;
    protected NotificationManager mNotifyManager;
    private Activity activity;

    private String lastServerVersion = "";
    private String fwLocation = "";

    private LinearLayout fw_loading_layout,fw_version_chk_layout, fw_version_layout, alert_layout;
//    private TextView fw_loading_text,fw_loading_progress_text,fw_version_server_text, fw_version_local_text,error_text;
//    private Button btn_download,btn_dismiss;
//    private ProgressBar fw_loading_prgressbar;
//    private OnCheckListener listener;

//    private String fwFilePath = "";
    private String fwFileName = "";
    private PenClientCtrl penClientCtrl;
    private MultiPenClientCtrl multiPenClientCtrl;
    private boolean success =false;
    private boolean isDisconnect = false;
    private boolean isSingleConnectionMode = false;
    private String penAddress = "";
    String fwVersion = "";
    int protocolVer = 0;
    String deviceName = "";

    public FwUpdateDialog ( Activity activity ,Object obj)
    {
        super( activity );
        this.activity = activity;
        if(obj instanceof PenClientCtrl)
        {
            this.penClientCtrl = (PenClientCtrl)obj;
            isSingleConnectionMode = true;
        }
        else
        {
            this.multiPenClientCtrl = (MultiPenClientCtrl)obj;
            isSingleConnectionMode = false;
        }
        requestWindowFeature( Window.FEATURE_NO_TITLE );
        getWindow().clearFlags( WindowManager.LayoutParams.FLAG_DIM_BEHIND );

        setContentView( R.layout.fw_update_dialog );

        setOnDismissListener( new OnDismissListener()
        {
            @Override
            public void onDismiss ( DialogInterface dialog )
            {
                resetUI();
            }
        } );
//        activity.registerReceiver(  )
        mNotifyManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        checkNotificationPermission();

        mBuilder = new NotificationCompat.Builder(activity, CHANNEL_ID)
                .setContentTitle("Firmware Update")
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

//        updateNotification("Sending", 0, false);

        fw_loading_layout = (LinearLayout)findViewById( R.id.fw_loading_layout );
        fw_loading_layout.setVisibility( View.INVISIBLE );
        fw_version_layout = (LinearLayout)findViewById( R.id.fw_version_layout );
        fw_version_layout.setVisibility( View.INVISIBLE );
        alert_layout = (LinearLayout)findViewById( R.id.alert_layout );
        alert_layout.setVisibility( View.INVISIBLE );
        fw_version_chk_layout = (LinearLayout)findViewById( R.id.fw_version_chk_layout );
        fw_version_chk_layout.setVisibility( View.VISIBLE );
    }

    public void show (String penAddress)
    {
        this.penAddress = penAddress;
        if(isSingleConnectionMode)
        {
            fwVersion = penClientCtrl.getPenFWVersion();
            protocolVer = penClientCtrl.getProtocolVersion();
        }
        else
        {
            fwVersion = multiPenClientCtrl.getPenFWVersion( penAddress );
            protocolVer = multiPenClientCtrl.getProtocolVersion(penAddress);
        }


        super.show();
    }

    public void setMsg( String penAddress, int type, String content)
    {
        switch ( type )
        {
            case PenMsgType.PEN_FW_UPGRADE_STATUS:
            {
                try
                {
                    JSONObject job = new JSONObject( content );

                    int total = job.getInt( Const.JsonTag.INT_TOTAL_SIZE );
                    int sent = job.getInt( Const.JsonTag.INT_SENT_SIZE );

                    this.onUpgrading( total, sent );

                    Log.d( TAG, "pen fw upgrade status => total : " + total + ", progress : " + sent );
                }
                catch ( JSONException e )
                {
                    e.printStackTrace();
                }
            }
            break;

            // Pen firmware update is complete
            case PenMsgType.PEN_FW_UPGRADE_SUCCESS:
                success = true;
                this.onUpgradeSuccess();

                Util.showToast( activity, "file transfer is complete." );

                break;

            // Pen Firmware Update Fails
            case PenMsgType.PEN_FW_UPGRADE_FAILURE:

                this.onUpgradeFailure( false );

                Util.showToast( activity, "file transfer has failed." );

                break;

            // When the pen stops randomly during the firmware update
            case PenMsgType.PEN_FW_UPGRADE_SUSPEND:

                this.onUpgradeFailure( true );

                Util.showToast( activity, "file transfer is suspended." );

                break;
            case PenMsgType.PEN_DISCONNECTED:
                isDisconnect = true;
                activity.runOnUiThread( new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        if(!isShowing() || success)
                            return;
                        NLog.d( "PEN_DISCONNECTED ");
                        fw_loading_layout.setVisibility( View.INVISIBLE );
                        fw_version_chk_layout.setVisibility( View.INVISIBLE );
                        fw_version_layout.setVisibility( View.INVISIBLE );
                        alert_layout.setVisibility( View.VISIBLE );
                        ((TextView) alert_layout.findViewById( R.id.alert_text )).setText( "Pen disconnected...");
                        ((Button) alert_layout.findViewById( R.id.btn_dismiss )).setOnClickListener( new View.OnClickListener()
                        {
                            @Override
                            public void onClick ( View v )
                            {
                                dismiss();
                            }
                        } );
                    }
                } );
                break;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Firmware Update",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows firmware update progress");
            mNotifyManager.createNotificationChannel(channel);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void updateNotification(String content, int progress, boolean indeterminate) {
        mBuilder.setContentText(content)
                .setProgress(100, progress, indeterminate);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void cancelNotification() {

        mNotifyManager.cancel(NOTIFICATION_ID);
    }

    private void onUpgrading( final int total, final int progress )
    {
        if(!isShowing() || isDisconnect)
            return;
        activity.runOnUiThread( new Runnable()
        {
            @Override
            public void run ()
            {
                int percent = progress*100/total;
                if(percent > 100)
                    percent = 100;
                ((TextView)fw_loading_layout.findViewById( R.id.fw_loading_text )).setText( "Sending Fw to Pen.. ");
                ((TextView)fw_loading_layout.findViewById( R.id.fw_loading_progress_text )).setText( percent+"%");

                ((ProgressBar)fw_loading_layout.findViewById( R.id.fw_loading_progressbar )).setProgress( percent );
                ((ProgressBar)fw_loading_layout.findViewById( R.id.fw_loading_progressbar )).setIndeterminate( false );

                updateNotification("Sending", percent, false);
            }
        });
    }

    private void onUpgradeFailure( final boolean isSuspend )
    {
        if(!isShowing() || isDisconnect)
            return;
        activity.runOnUiThread( new Runnable()
        {
            @Override
            public void run ()
            {
                NLog.d( "AsyncFirmDownloadTask onUpgradeFailure");
                fw_loading_layout.setVisibility( View.INVISIBLE );
                alert_layout.setVisibility( View.VISIBLE );
                ((TextView) alert_layout.findViewById( R.id.alert_text )).setText( "Pen Fw Update fail...");
                ((Button) alert_layout.findViewById( R.id.btn_dismiss )).setOnClickListener( new View.OnClickListener()
                {
                    @Override
                    public void onClick ( View v )
                    {
                        dismiss();
                    }
                } );

                if ( isSuspend )
                {
                    updateNotification("File transfer suspended", 0, false);
                }
                else
                {
                    updateNotification("File transfer failed", 0, false);
                }
            }
        } );

    }

    private void onUpgradeSuccess()
    {
//        if(!isShowing() || isDisconnect)
//            return;
        activity.runOnUiThread( new Runnable()
        {
            @Override
            public void run ()
            {
                NLog.d( "AsyncFirmDownloadTask onUpgradeSuccess");
                fw_loading_layout.setVisibility( View.INVISIBLE );
                alert_layout.setVisibility( View.VISIBLE );
                ((TextView) alert_layout.findViewById( R.id.alert_text )).setText( "Pen Fw Update Success...");
                ((Button) alert_layout.findViewById( R.id.btn_dismiss )).setOnClickListener( new View.OnClickListener()
                {
                    @Override
                    public void onClick ( View v )
                    {
                        dismiss();
                    }
                } );
                cancelNotification();
            }
        } );
    }

//    @Override
//    public void onCheckComplete ()
//    {
//
//        if(!isShowing() || isDisconnect)
//            return;
//        activity.runOnUiThread( new Runnable()
//        {
//            @Override
//            public void run ()
//            {
//                fw_version_chk_layout.setVisibility( View.INVISIBLE );
//                fw_version_layout.setVisibility( View.VISIBLE );
//                ((TextView)fw_version_layout.findViewById( R.id.fw_version_server_text )).setText( "Server FW Last version : "+lastServerVersion );
//                ((TextView)fw_version_layout.findViewById( R.id.fw_version_local_text )).setText( "Pen FW version : "+ fwVersion );
//                if(isUpgrade(fwVersion,lastServerVersion))
//                {
//                    ((Button)fw_version_layout.findViewById( R.id.btn_ok )).setVisibility( View.INVISIBLE );
//                    ((Button)fw_version_layout.findViewById( R.id.btn_download )).setVisibility( View.VISIBLE );
//                    ((Button)fw_version_layout.findViewById( R.id.btn_download )).setOnClickListener( new View.OnClickListener()
//                    {
//                        @Override
//                        public void onClick ( View v )
//                        {
//                            fw_version_layout.setVisibility( View.INVISIBLE );
//                            fw_loading_layout.setVisibility( View.VISIBLE );
//                            //다운로드
//                            ((TextView)fw_loading_layout.findViewById( R.id.fw_loading_text )).setText( "FW Downloading...");
//
//                            ((ProgressBar)fw_loading_layout.findViewById( R.id.fw_loading_progressbar )).setIndeterminate( true );
//                            String urlStr = "";
//                            if(protocolVer == 2)
//                            {
//                                urlStr = FW_BASE_URL_20+fwLocation;
//
//                            }
//                            else
//                                urlStr = FW_BASE_URL + fwLocation;
//
//                            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//                            File dir = new File(downloadDir.getAbsolutePath()+ File.separator + "temp_firmware"+File.separator+fwLocation);
//                            if(!dir.getParentFile().exists())
//                                dir.getParentFile().mkdirs();
//                            fwFilePath = dir.getAbsolutePath();
//                            AsyncFirmDownloadTask task = new AsyncFirmDownloadTask( urlStr, fwFilePath ,FwUpdateDialog.this);
//                            if ( Build.VERSION.SDK_INT >= 11 )
//                            {
//                                task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR );
//                            }
//                            else
//                            {
//                                task.execute();
//                            }
//
//                        }
//                    } );
//                }
//                else
//                {
//                    ((Button)fw_version_layout.findViewById( R.id.btn_download )).setVisibility( View.INVISIBLE );
//                    ((Button)fw_version_layout.findViewById( R.id.btn_ok )).setVisibility( View.VISIBLE );
//                    ((Button)fw_version_layout.findViewById( R.id.btn_ok )).setOnClickListener( new View.OnClickListener()
//                    {
//                        @Override
//                        public void onClick ( View v )
//                        {
//                            dismiss();
//                        }
//                    } );
//                }
//
//            }
//        } );
//    }

//    @Override
//    public void onError ()
//    {
//        if(!isShowing() || isDisconnect)
//            return;
//        activity.runOnUiThread( new Runnable()
//        {
//            @Override
//            public void run ()
//            {
//                fw_version_chk_layout.setVisibility( View.INVISIBLE );
//                alert_layout.setVisibility( View.VISIBLE );
//                ((TextView) alert_layout.findViewById( R.id.alert_text )).setText( "Pen Fw Check fail...");
//                ((Button) alert_layout.findViewById( R.id.btn_dismiss )).setOnClickListener( new View.OnClickListener()
//                {
//                    @Override
//                    public void onClick ( View v )
//                    {
//                        dismiss();
//                    }
//                } );
//            }
//        } );
//    }

    private void resetUI()
    {
        isDisconnect = false;
        fw_loading_layout.setVisibility( View.INVISIBLE );
        fw_version_layout.setVisibility( View.INVISIBLE );
        alert_layout.setVisibility( View.INVISIBLE );
        fw_version_chk_layout.setVisibility( View.VISIBLE );
    }

    @Override
    protected void onStart ()
    {
        super.onStart();
        chkFwVersion();

//        FirmUpgradeCheckAsyncTask task = new FirmUpgradeCheckAsyncTask(protocolVer, this);
//        if ( Build.VERSION.SDK_INT >= 11 )
//        {
//            task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR );
//        }
//        else
//        {
//            task.execute();
//        }

    }

    private void chkFwVersion() {
        //The existing example was an example that allowed you to test the method of importing from the NeoLab server, but the server is currently not supported.
        //lastServerVersion must be retrieved from each server or the latest firmware version information must be stored locally.
        lastServerVersion = "3.03.0166";
        //fwFile must also be downloaded from each server or saved as a file locally.
        //put the firmware file at Assets Folder(assets/firmware_files)
        fwFileName = "NWP-F130_3.03.0166._v_";
        fw_version_chk_layout.setVisibility(View.INVISIBLE);
        fw_version_layout.setVisibility(View.VISIBLE);
        ((TextView) fw_version_layout.findViewById(R.id.fw_version_server_text)).setText("Server FW Last version : " + lastServerVersion);
        ((TextView) fw_version_layout.findViewById(R.id.fw_version_local_text)).setText("Pen FW version : " + fwVersion);
        if (isUpgrade(fwVersion, lastServerVersion)) {
            ((Button) fw_version_layout.findViewById(R.id.btn_ok)).setVisibility(View.INVISIBLE);
            ((Button) fw_version_layout.findViewById(R.id.btn_download)).setVisibility(View.VISIBLE);
            ((Button) fw_version_layout.findViewById(R.id.btn_download)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateNotification("Sending", 0, false);

                    fw_version_layout.setVisibility(View.INVISIBLE);
                    fw_loading_layout.setVisibility(View.VISIBLE);

                    ((TextView) fw_loading_layout.findViewById(R.id.fw_loading_text)).setText("Send Fw to Pen.. ");
                    ((TextView) fw_loading_layout.findViewById(R.id.fw_loading_progress_text)).setText("0%");

                    ((ProgressBar) fw_loading_layout.findViewById(R.id.fw_loading_progressbar)).setProgress(0);
                    ((ProgressBar) fw_loading_layout.findViewById(R.id.fw_loading_progressbar)).setIndeterminate(true);
                    File fwFile = null;
                    try {
                        fwFile = copyFirmwareFileFromAssets(getContext(), "NWP-F130_3.03.0166._v_");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    NLog.d("fwFile=" + fwFile.getPath() + "," + fwFile.exists());
                    if (isSingleConnectionMode) {
                        if (protocolVer == 2) {
                            if (penClientCtrl.getDeviceName().equals("NSP-C200"))
                                penClientCtrl.upgradePen2(fwFile, lastServerVersion, false);
                            else
                                penClientCtrl.upgradePen2(fwFile, lastServerVersion,false);
                        } else {
                            penClientCtrl.upgradePen(fwFile);
                        }
                    } else {
                        if (protocolVer == 2) {
                            if (multiPenClientCtrl.getDeviceName(penAddress).equals("NSP-C200"))
                                multiPenClientCtrl.upgradePen2(penAddress, fwFile, lastServerVersion, false);
                            else
                                multiPenClientCtrl.upgradePen2(penAddress, fwFile, lastServerVersion,false);
                        } else {
                            multiPenClientCtrl.upgradePen(penAddress, fwFile);
                        }
                    }

                }
            });
        } else {
            ((Button) fw_version_layout.findViewById(R.id.btn_download)).setVisibility(View.INVISIBLE);
            ((Button) fw_version_layout.findViewById(R.id.btn_ok)).setVisibility(View.VISIBLE);
            ((Button) fw_version_layout.findViewById(R.id.btn_ok)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }


    private File copyFirmwareFileFromAssets(Context context, String assetFileName) throws IOException {
        // Define the path in the assets folder (e.g., assets/firmware_files/filename)
        String assetPath = "firmware_files/" + assetFileName;

        // Create the output file in the internal storage
        File outFile = new File(context.getFilesDir(), assetFileName);

        // Copy the file from assets to internal storage
        try (InputStream is = context.getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        }

        // Return the File object to be passed to the library
        return outFile;
    }

    /**
     * Check new version
     * @param v1 Pen version
     * @param v2 Server new version
     * @return if new version exist, true
     */
    private boolean isUpgrade(String v1, String v2) {
        String s1 = normalisedVersion(v1);
        String s2 = normalisedVersion(v2);
        int cmp = s1.compareTo(s2);

        return cmp < 0 ? true : false;
    }

    private String normalisedVersion(String version) {
        return normalisedVersion(version, ".", 4);
    }

    private String normalisedVersion(String version, String sep, int maxWidth) {
        String[] split = Pattern.compile(sep, Pattern.LITERAL).split(
                version);
        StringBuilder sb = new StringBuilder();
        for (String s : split) {
            sb.append(String.format("%" + maxWidth + 's', s));
        }
        return sb.toString();
    }

//    @Override
//    public void onDownloadComplete ()
//    {
//        if(!isShowing() || isDisconnect)
//            return;
//        activity.runOnUiThread( new Runnable()
//        {
//            @Override
//            public void run ()
//            {
//                ((TextView)fw_loading_layout.findViewById( R.id.fw_loading_text )).setText( "Download Complete and send Fw Pen.. ");
//                ((TextView)fw_loading_layout.findViewById( R.id.fw_loading_progress_text )).setText( "0%");
//
//                ((ProgressBar)fw_loading_layout.findViewById( R.id.fw_loading_progressbar )).setProgress( 0 );
//                ((ProgressBar)fw_loading_layout.findViewById( R.id.fw_loading_progressbar )).setIndeterminate( true );
//                File fwFile = new File(fwFilePath);
//                if(isSingleConnectionMode)
//                {
//                    if(protocolVer == 2)
//                    {
//                        if(penClientCtrl.getDeviceName().equals( "NSP-C200" ))
//                            penClientCtrl.upgradePen2(fwFile, lastServerVersion , false);
//                        else
//                            penClientCtrl.upgradePen2(fwFile, lastServerVersion );
//                    }
//                    else
//                    {
//                        penClientCtrl.upgradePen( fwFile );
//                    }
//                }
//                else
//                {
//                    if(protocolVer == 2)
//                    {
//                        if(multiPenClientCtrl.getDeviceName(penAddress).equals( "NSP-C200" ))
//                            multiPenClientCtrl.upgradePen2(penAddress ,fwFile, lastServerVersion , false);
//                        else
//                            multiPenClientCtrl.upgradePen2(penAddress, fwFile, lastServerVersion );
//                    }
//                    else
//                    {
//                        multiPenClientCtrl.upgradePen(penAddress, fwFile );
//                    }
//                }
//
//            }
//        } );
//    }
//
//    @Override
//    public void onDownloading ( final int percent, final long downloadBytes )
//    {
//        if(!isShowing() || isDisconnect)
//            return;
//        activity.runOnUiThread( new Runnable()
//        {
//            @Override
//            public void run ()
//            {
//                NLog.d( "AsyncFirmDownloadTask onDownloading percent=" +percent+" ,downloadBytes="+downloadBytes);
//                ((TextView)fw_loading_layout.findViewById( R.id.fw_loading_progress_text )).setText( downloadBytes+" byte : "+percent+"%");
//                ((ProgressBar)fw_loading_layout.findViewById( R.id.fw_loading_progressbar )).setIndeterminate( false );
//                ((ProgressBar)fw_loading_layout.findViewById( R.id.fw_loading_progressbar )).setProgress( percent );
//            }
//        } );
//
//    }
//
//    @Override
//    public void onDownloadFail ()
//    {
//        if(!isShowing() || isDisconnect)
//            return;
//        activity.runOnUiThread( new Runnable()
//        {
//            @Override
//            public void run ()
//            {
//                NLog.d( "AsyncFirmDownloadTask onDownloadFail");
//                fw_loading_layout.setVisibility( View.INVISIBLE );
//                alert_layout.setVisibility( View.VISIBLE );
//                ((TextView) alert_layout.findViewById( R.id.alert_text )).setText( "Pen Fw Update fail...");
//                ((Button) alert_layout.findViewById( R.id.btn_dismiss )).setOnClickListener( new View.OnClickListener()
//                {
//                    @Override
//                    public void onClick ( View v )
//                    {
//                        dismiss();
//                    }
//                } );
//            }
//        } );
//
//    }
//
//    class FirmUpgradeCheckAsyncTask extends AsyncTask<Void, Integer, Void>
//    {
//
//        private int protocolVer;
//        private OnCheckListener listener;
//
//        public FirmUpgradeCheckAsyncTask ( int protocolVer, OnCheckListener listener)
//        {
//            this.protocolVer = protocolVer;
//            this.listener = listener;
//        }
//
//        @Override
//        protected void onPreExecute ()
//        {
//            super.onPreExecute();
//        }
//
//        @Override
//        protected Void doInBackground ( Void... params )
//        {
//
//            try
//            {
//                String strUrl = "";
//                if ( protocolVer == 2 )
//                {
//                    String modelName ="";
//                    if(isSingleConnectionMode)
//                        modelName = penClientCtrl.getDeviceName();
//                    else
//                        modelName = multiPenClientCtrl.getDeviceName(penAddress);
//                    if ( modelName!= null && deviceName.equals( "NWP-F50" ) )
//                    {
//                        strUrl = FW_BASE_URL_20 + File.separator + FW_VERSION_CHK_FILE_F50;
//                    }
//                    else
//                    {
//                        strUrl = FW_BASE_URL_20 + File.separator + FW_VERSION_CHK_FILE_F120;
//                    }
//
//                }
//                else strUrl = FW_BASE_URL + File.separator + FW_VERSION_CHK_FILE_F110;
//                URL url = new URL( strUrl );
//
//                BufferedReader in = new BufferedReader( new InputStreamReader( url.openStream() ) );
//                String line;
//                while ( ( line = in.readLine() ) != null )
//                {
//                    String[] split = line.split( "=" );
//                    if ( split[0].equals( "version" ) ) lastServerVersion = split[1];
//                    else if ( split[0].equals( "location" ) ) fwLocation = split[1];
////                    else if ( split[0].equals( "size" ) ) size = Integer.parseInt( split[1] );
//                }
//                in.close();
//                this.listener.onCheckComplete();
//            }
//            catch ( IOException e )
//            {
//                e.printStackTrace();
//                listener.onError();
//            }
//
//            Log.i( "FW", "Checking...latestVer=" + lastServerVersion+",fwLocation="+fwLocation );
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute ( Void result )
//        {
//            super.onPostExecute( result );
//
//        }
//
//    }
//
//
//    class AsyncFirmDownloadTask extends AsyncTask<Void, Integer, Void>
//    {
//        private static final long  BR_INTERVAL = 100;
//        String url = "";
//        String filePath = "";
//
//        private OnDownLoadListener listener;
//
//        public AsyncFirmDownloadTask ( String url, String filePath , OnDownLoadListener listener)
//        {
//            this.url = url;
//            this.listener = listener;
//            this.filePath = filePath;
//        }
//
//        @Override
//        protected void onPreExecute ()
//        {
//            super.onPreExecute();
//        }
//
//        @Override
//        protected Void doInBackground ( Void... params )
//        {
//            if(!download(url, filePath))
//            {
//                listener.onDownloadFail();
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute ( Void result )
//        {
//            super.onPostExecute( result );
//
//        }
//        private boolean download(String url, String filePath)
//        {
////        url = url.replace( "\"","" );
//            HttpURLConnection con = null ;
//            InputStream is = null;
//            BufferedInputStream bis = null;
//            FileOutputStream fos = null;
//            BufferedOutputStream bos = null;
//            byte[] pBuffer = new byte[1024];
//            File destFile = null;
//
//            try
//            {
//                URL down_url = null;
//                try
//                {
//                    url = url.replace(" ", "%20");
//                    String encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS);
//                    down_url = new URL( encodedUrl);
//                }
//                catch ( MalformedURLException e )
//                {
//                    e.printStackTrace();
//                }
//
//                NLog.d( "AsyncFirmDownloadTask url=" +url.toString()+" ,filePath="+filePath);
//                con = (HttpURLConnection) ( down_url).openConnection();
//                con.setRequestMethod( "GET" );
//                con.setConnectTimeout( 30000 );
//                con.setReadTimeout( 30000 );
//                con.setDoInput( true );
////            con.setDoOutput( true );
//                con.connect();
//
//
//                if(con.getResponseCode() == HttpURLConnection.HTTP_OK)
//                {
//                    // Let's read the response
//                    int nLength = 0;
//                    int filesize = 0;
//                    long currTime = 0;
//                    long prevTime = 0;
//
//                    destFile = new File(filePath);
//                    if(destFile.exists())
//                        destFile.delete();
//                    is = con.getInputStream();
//                    bis = new BufferedInputStream(is);
//                    fos = new FileOutputStream(destFile);
//                    bos = new BufferedOutputStream(fos);
//
//                    int contentLength = con.getContentLength();
//                    int percent = 0;
//
//                    while ( ( nLength = bis.read( pBuffer) ) != -1 )
//                    {
//                        bos.write(pBuffer, 0, nLength);
//                        filesize += nLength;
//                        currTime = System.currentTimeMillis();
//                        if((currTime - prevTime) > BR_INTERVAL) // 시간 조건으로만 BR 전송
//                        {
//                            if(contentLength != 0)
//                                percent = (filesize*100)/contentLength;
//                            if(percent >= 100) percent = 99;
//                            listener.onDownloading( percent, filesize );
//                            prevTime = currTime;
//                        }
//                    }
//                    bos.flush();
//                    NLog.d( "AsyncFirmDownloadTask download filesize="+filesize+"contentLength="+contentLength+"destFile"+destFile.length() );
//                    listener.onDownloadComplete();
//                    return true;
//                }
//                else
//                {
//                    return false;
//                }
//            }
//            catch ( Exception e )
//            {
//                if(destFile != null && destFile.exists())
//                    destFile.delete();
//                e.printStackTrace();
//                return false;
//            }
//            finally {
//                try{
//                    if(is != null)
//                        is.close();
//                } catch(Exception t) {}
//                try{
//                    if(bis != null)
//                        bis.close();
//                } catch(Exception t) {}
//                try{
//                    if(fos != null)
//                        fos.close();
//                } catch(Exception t) {}
//                try{
//                    if(bos != null)
//                        bos.close();
//                } catch(Exception t) {}
//
//
//                try {
//                    if(con != null)
//                        con.disconnect();
//                } catch(Exception t) {}
//            }
//        }
//    }
}

//interface OnCheckListener {
//    public void onCheckComplete();
//    public void onError();
//}
//
//interface OnDownLoadListener {
//    public void onDownloadComplete();
//    public void onDownloading(int percent, long downloadBytes);
//    public void onDownloadFail();
//}

