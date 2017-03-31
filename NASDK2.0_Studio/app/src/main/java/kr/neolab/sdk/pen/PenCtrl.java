package kr.neolab.sdk.pen;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;

import kr.neolab.sdk.broadcastreceiver.BTDuplicateRemoveBroadcasterReceiver;
import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.bluetooth.BTAdt;
import kr.neolab.sdk.pen.bluetooth.BTLEAdt;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.util.NLog;
import kr.neolab.sdk.util.SDKVersion;
import kr.neolab.sdk.util.UseNoteData;

/**
 * The class that provides the functionality of a pen
 *
 * @author CHY
 */
public class PenCtrl implements IPenCtrl {
    private IPenAdt currentAdt = null;
    private static PenCtrl myInstance = null;
    private static BTDuplicateRemoveBroadcasterReceiver mReceiver;

    private PenCtrl()
    {
        currentAdt = BTAdt.getInstance();
    }

    public boolean setLeMode(boolean isLeMode)
    {
        if (currentAdt != null && currentAdt.getPenStatus() != IPenAdt.CONN_STATUS_IDLE)
        {
            return false;
        }

        if (isLeMode)
        {
            if( Build.VERSION.SDK_INT >= 21)
                currentAdt = BTLEAdt.getInstance();
            else
                return false;
        }
        else
            currentAdt = BTAdt.getInstance();

        return true;
    }


    /**
     * Gets an instance of the Pen Controller
     *
     * @return instance instance
     */
    public synchronized static PenCtrl getInstance() {
        if (myInstance == null) {
            myInstance = new PenCtrl();
        }
        if (mReceiver == null) {
            mReceiver = new BTDuplicateRemoveBroadcasterReceiver();
        }

        return myInstance;
    }

    @Override
    public void setContext(Context context) {
        // TODO Auto-generated method stub
        if( Build.VERSION.SDK_INT >= 21)
            BTLEAdt.getInstance().setContext( context );
        BTAdt.getInstance().setContext(context);
    }

    @Override
    public void setListener(IPenMsgListener listener) {
        if( Build.VERSION.SDK_INT >= 21)
            BTLEAdt.getInstance().setListener( listener );
        BTAdt.getInstance().setListener(listener);
    }

    @Override
    public void setDotListener(IPenDotListener listener) {
        if( Build.VERSION.SDK_INT >= 21)
            BTLEAdt.getInstance().setDotListener( listener );
        BTAdt.getInstance().setDotListener(listener);
    }

    @Override
    public void setOffLineDataListener(IOfflineDataListener listener) {
        if( Build.VERSION.SDK_INT >= 21)
            BTLEAdt.getInstance().setOffLineDataListener( listener );
        BTAdt.getInstance().setOffLineDataListener(listener);
    }


    @Override
    public IPenMsgListener getListener() {
        // TODO Auto-generated method stub
        return currentAdt.getListener();
    }

    @Override
    public IOfflineDataListener getOffLineDataListener() {
        return currentAdt.getOffLineDataListener();
    }

    @Override
    public void startup() {
        NLog.i("[BTCtrl] startup");
        currentAdt.startListen();
    }

    @Override
    public void connect(String address) {
        currentAdt.connect(address);
    }

    @Override
    public void disconnect() {
        NLog.i("[BTCtrl] disconnect all pen");
        currentAdt.disconnect();
    }

    @Override
    public boolean isAvailableDevice(String mac) throws BLENotSupportedException {
        return currentAdt.isAvailableDevice(mac);
    }

    @Override
    public boolean isAvailableDevice(byte[] mac)
    {
        return currentAdt.isAvailableDevice(mac);
    }

    @Override
    public String getConnectedDevice() 
    {
        return currentAdt.getConnectedDevice();
    }   
    
	@Override
	public void inputPassword( String password )
	{
		currentAdt.inputPassword(password);
	}

	@Override
	public void reqSetupPassword( String oldPassword, String newPassword )
	{
		currentAdt.reqSetupPassword( oldPassword, newPassword );
	}

	@Override
	public void reqSetUpPasswordOff ( String oldPassword ) throws ProtocolNotSupportedException
	{
		currentAdt.reqSetUpPasswordOff( oldPassword );
	}

	@Override
	public void setAllowOfflineData(boolean allow) 
	{
		currentAdt.setAllowOfflineData( allow );
	}
	
	@Override
	public synchronized void setOfflineDataLocation(String path) 
	{
		currentAdt.setOfflineDataLocation( path );
	}
	
	@Override
	public void calibratePen() 
	{
		currentAdt.reqForceCalibrate();
	}
	
	@Override
	public void upgradePen(File fwFile) throws ProtocolNotSupportedException
	{   
		String target = "\\NEO1.zip";
		currentAdt.reqFwUpgrade( fwFile, target);
	}

	@Override
	public void upgradePen2 ( File fwFile, String fwVersion ) throws ProtocolNotSupportedException
	{
		currentAdt.reqFwUpgrade2( fwFile, fwVersion );
	}

	@Override
	public void upgradePen(File fwFile, String targetPath)  throws ProtocolNotSupportedException
	{
		currentAdt.reqFwUpgrade(fwFile, targetPath);
	}
	
	@Override
	public void suspendPenUpgrade()
	{
		currentAdt.reqSuspendFwUpgrade();
	}
	
	@Override
	public void reqAddUsingNote(int sectionId, int ownerId, int[] noteIds) 
	{
		currentAdt.reqAddUsingNote( sectionId, ownerId, noteIds );
	}

	@Override
	public void reqAddUsingNote( int sectionId, int ownerId )
	{
		currentAdt.reqAddUsingNote(sectionId, ownerId);
	}

	@Override
	public void reqAddUsingNote(int[] sectionId, int[] ownerId) {
		currentAdt.reqAddUsingNote(sectionId, ownerId);

	}

	@Override
	public void reqAddUsingNoteAll()
	{
		currentAdt.reqAddUsingNoteAll();
	}

	@Override
	public void reqAddUsingNote ( ArrayList<UseNoteData> noteList ) throws ProtocolNotSupportedException
	{
		currentAdt.reqAddUsingNote(noteList);
	}

	@Override
	public void reqOfflineData(int sectionId, int ownerId, int noteId) 
	{
		currentAdt.reqOfflineData( sectionId, ownerId, noteId );
	}

	@Override
	public void reqOfflineData(int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException
	{
		currentAdt.reqOfflineData( sectionId, ownerId, noteId ,pageIds);
	}

	@Override
	public void reqOfflineDataList() 
	{
		currentAdt.reqOfflineDataList();
	}

	@Override
	public void reqOfflineDataList(int sectionId, int ownerId) throws ProtocolNotSupportedException
	{
		currentAdt.reqOfflineDataList(sectionId, ownerId);
	}

	@Override
	public void reqOfflineDataPageList(int sectionId, int ownerId, int noteId) throws ProtocolNotSupportedException
	{
		currentAdt.reqOfflineDataPageList( sectionId, ownerId, noteId );
	}

	@Override
	public void removeOfflineData( int sectionId, int ownerId ) throws ProtocolNotSupportedException
	{
		currentAdt.removeOfflineData( sectionId, ownerId );
	}

	@Override
	public void removeOfflineData ( int sectionId, int ownerId, int[] noteIds ) throws ProtocolNotSupportedException
	{
		currentAdt.removeOfflineData(sectionId, ownerId, noteIds);
	}

	@Override
	public void reqPenStatus() 
	{
		currentAdt.reqPenStatus();
	}
	
	@Override
	public void reqSetupAutoPowerOnOff(boolean setOn) 
	{
		currentAdt.reqSetupAutoPowerOnOff( setOn );
	}
	
	@Override
    public void reqSetupPenBeepOnOff( boolean setOn )
    {
		currentAdt.reqSetupPenBeepOnOff( setOn );
    }

	@Override
	public void reqSetupPenTipColor( int color )
	{
		currentAdt.reqSetupPenTipColor( color );
	}

	@Override
	public void reqSetupAutoShutdownTime( short minute )
	{
		currentAdt.reqSetupAutoShutdownTime( minute );
	}

	@Override
	public void reqSetupPenSensitivity( short level )
	{
		currentAdt.reqSetupPenSensitivity( level );
	}

	@Override
	public void reqSetupPenCapOff ( boolean on ) throws ProtocolNotSupportedException
	{
		currentAdt.reqSetupPenCapOff( on );
	}

	@Override
	public void reqSetupPenHover ( boolean on )
	{
		currentAdt.reqSetupPenHover( on );
	}


	@Override
	public void registerBroadcastBTDuplicate() {
		try {
			IntentFilter filter = new IntentFilter();
			filter.addAction(BTDuplicateRemoveBroadcasterReceiver.ACTION_BT_REQ_CONNECT);
			filter.addAction(BTDuplicateRemoveBroadcasterReceiver.ACTION_BT_RESPONSE_CONNECTED);
			currentAdt.getContext().registerReceiver(mReceiver, filter);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregisterBroadcastBTDuplicate() {
		try {
			currentAdt.getContext().unregisterReceiver(mReceiver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getVersion() {
		return SDKVersion.version;
	}

	@Override
	public int getProtocolVersion ()
	{
		return currentAdt.getProtocolVersion();
	}

	/**
	 * Gets pen status.
	 *
	 * @return the pen status
	 */
	public int getPenStatus() {
		return currentAdt.getPenStatus();
	}

}


