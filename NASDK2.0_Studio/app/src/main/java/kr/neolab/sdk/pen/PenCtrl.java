package kr.neolab.sdk.pen;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

import java.io.File;
import java.io.PipedInputStream;
import java.util.ArrayList;

import kr.neolab.sdk.broadcastreceiver.BTDuplicateRemoveBroadcasterReceiver;
import kr.neolab.sdk.metadata.IMetadataListener;
import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.bluetooth.BTAdt;
import kr.neolab.sdk.pen.bluetooth.BTLEAdt;
import kr.neolab.sdk.pen.bluetooth.BTOfflineAdt;
import kr.neolab.sdk.pen.bluetooth.lib.OutOfRangeException;
import kr.neolab.sdk.pen.bluetooth.lib.ProfileKeyValueLimitException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.pen.usb.UsbAdt;
import kr.neolab.sdk.util.NLog;
import kr.neolab.sdk.util.SDKVersion;
import kr.neolab.sdk.util.UseNoteData;

import static kr.neolab.sdk.pen.bluetooth.comm.CommProcessor20.PEN_UP_DOWN_SEPARATE_SUPPORT_PROTOCOL_VERSION;

/**
 * The class that provides the functionality of a pen
 *
 * @author CHY
 */
public class PenCtrl implements IPenCtrl {
    private IPenAdt currentAdt = null;
    private static PenCtrl myInstance = null;
    private static BTDuplicateRemoveBroadcasterReceiver mReceiver;

    private AdtMode adtMode = AdtMode.SPP_MODE;
	public enum AdtMode
	{
		SPP_MODE, LE_MODE, USB_MODE, OFFLINE_TO_ONLINE_MODE
	}

    private PenCtrl()
    {
        currentAdt = BTAdt.getInstance();
    }

    /**
	 * @deprecated use {@link #setAdtMode(AdtMode)} instead.
     */
    @Deprecated
    public boolean setOfflineToOnlineMode(boolean isOfflineToOnlineMode) {
        if(isOfflineToOnlineMode) {
			currentAdt = BTOfflineAdt.getInstance();
			adtMode = AdtMode.OFFLINE_TO_ONLINE_MODE;
		}
        else {
			currentAdt = BTAdt.getInstance();
			adtMode = AdtMode.SPP_MODE;
		}
        return isOfflineToOnlineMode;
    }

    public void setPipedInputStream(PipedInputStream pipedInputStream) {
        currentAdt.setPipedInputStream(pipedInputStream);
    }

	/**
	 * @deprecated use {@link #setAdtMode(AdtMode)} instead.
	 */
	@Deprecated
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

	public boolean setAdtMode(AdtMode adtMode)
	{
		if (currentAdt != null && currentAdt.getPenStatus() != IPenAdt.CONN_STATUS_IDLE)
		{
			return false;
		}
		this.adtMode = adtMode;

		if (adtMode == AdtMode.SPP_MODE)
		{
			currentAdt = BTAdt.getInstance();
		}
		else if(adtMode == AdtMode.LE_MODE)
		{
			currentAdt = BTLEAdt.getInstance();
		}
		else if(adtMode == AdtMode.USB_MODE)
		{
			currentAdt = UsbAdt.getInstance();
		}
		else
		{
			currentAdt = BTOfflineAdt.getInstance();
		}

		return true;
	}


	@Override
	public String getConnectDeviceName() throws ProtocolNotSupportedException {
		return currentAdt.getConnectDeviceName();
	}

	@Override
	public String getConnectSubName() throws ProtocolNotSupportedException {
		return currentAdt.getConnectSubName();
	}

	@Override
	public String getReceiveProtocolVer() throws ProtocolNotSupportedException {
		return currentAdt.getReceiveProtocolVer();
	}

	@Override
	public String getFirmwareVer() throws ProtocolNotSupportedException {
		return currentAdt.getFirmwareVer();
	}

	@Override
	public void createProfile ( String proFileName, byte[] password ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		currentAdt.createProfile ( proFileName, password );
	}

	@Override
	public void deleteProfile ( String proFileName, byte[] password ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		currentAdt.deleteProfile ( proFileName, password );

	}

	@Override
	public void writeProfileValue ( String proFileName, byte[] password, String[] keys, byte[][] data ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		currentAdt.writeProfileValue ( proFileName, password ,keys, data);

	}

	@Override
	public void readProfileValue ( String proFileName, String[] keys ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		currentAdt.readProfileValue ( proFileName, keys );

	}

	@Override
	public void deleteProfileValue ( String proFileName, byte[] password, String[] keys ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		currentAdt.deleteProfileValue ( proFileName, password , keys);

	}

	@Override
	public void getProfileInfo ( String proFileName ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		currentAdt.getProfileInfo ( proFileName);
	}

	@Override
	public boolean isSupportPenProfile ()
	{
		return currentAdt.isSupportPenProfile();
	}

	@Override
	public boolean unpairDevice ( String address )
	{
		return currentAdt.unpairDevice(address);
	}

	@Override
	public short getColorCode()throws ProtocolNotSupportedException{
		return currentAdt.getColorCode();
	}

	@Override
	public short getProductCode()throws ProtocolNotSupportedException{
		return currentAdt.getProductCode();
	}

	@Override
	public short getCompanyCode()throws ProtocolNotSupportedException {
		return currentAdt.getCompanyCode();
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
        if( Build.VERSION.SDK_INT >= 21)
            BTLEAdt.getInstance().setContext( context );
        BTAdt.getInstance().setContext(context);
        BTOfflineAdt.getInstance().setContext(context);
        UsbAdt.getInstance().setContext(context);
    }

    @Override
    public void setListener(IPenMsgListener listener) {
        if( Build.VERSION.SDK_INT >= 21)
            BTLEAdt.getInstance().setListener( listener );
        BTAdt.getInstance().setListener(listener);
        BTOfflineAdt.getInstance().setListener(listener);
		UsbAdt.getInstance().setListener(listener);


    }

    @Override
    public void setDotListener(IPenDotListener listener) {
        if( Build.VERSION.SDK_INT >= 21)
            BTLEAdt.getInstance().setDotListener( listener );
        BTAdt.getInstance().setDotListener(listener);
        BTOfflineAdt.getInstance().setDotListener(listener);
		UsbAdt.getInstance().setDotListener(listener);
    }

    @Override
    public void setOffLineDataListener(IOfflineDataListener listener) {
        if( Build.VERSION.SDK_INT >= 21)
            BTLEAdt.getInstance().setOffLineDataListener( listener );
        BTAdt.getInstance().setOffLineDataListener(listener);
        BTOfflineAdt.getInstance().setOffLineDataListener(listener);
    }

	@Override
	public void setMetadataListener( IMetadataListener listener )
	{
		if( Build.VERSION.SDK_INT >= 21)
			BTLEAdt.getInstance().setMetadataListener( listener );
		BTAdt.getInstance().setMetadataListener( listener );
		BTOfflineAdt.getInstance().setMetadataListener( listener );
	}

	@Override
    public IPenMsgListener getListener() {
        return currentAdt.getListener();
    }

	public IPenDotListener getDotListener() {
		return currentAdt.getDotListener();
	}


	@Override
    public IOfflineDataListener getOffLineDataListener() {
        return currentAdt.getOffLineDataListener();
    }

	@Override
	public IMetadataListener getMetadataListener()
	{
		return currentAdt.getMetadataListener();
	}

	@Override
	public void connect(String address) throws BLENotSupportedException{
		currentAdt.connect(address);
	}

	@Deprecated
	public void connect( String sppAddress, String leAddress )
	{
		connect(sppAddress, leAddress, BTLEAdt.UUID_VER.VER_2, (short)0x1101, PEN_UP_DOWN_SEPARATE_SUPPORT_PROTOCOL_VERSION);
	}

	@Override
	public void connect(String sppAddress, String leAddress, BTLEAdt.UUID_VER uuidVer, short appType, String reqProtocolVer) {
		if(currentAdt instanceof BTAdt)
			((BTAdt)currentAdt).connect(sppAddress);
		else
			((BTLEAdt)currentAdt).connect(sppAddress, leAddress, uuidVer, appType, reqProtocolVer,true);
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
	public String getConnectingDevice ()
	{
		return currentAdt.getConnectingDevice();
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

	/**
	 * Sets calibrate 2.
	 *
	 * @param factor the factor
	 */
//	@Override
	public void setCalibrate2 ( float[] factor )
	{
		currentAdt.reqCalibrate2( factor );
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
		upgradePen2 ( fwFile, fwVersion, true );
	}

	@Override
	public void upgradePen2 ( File fwFile, String fwVersion, boolean isCompress ) throws ProtocolNotSupportedException
	{
		currentAdt.reqFwUpgrade2( fwFile, fwVersion, isCompress );
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
	public void reqAddUsingNote(int sectionId, int ownerId, int[] noteIds) throws OutOfRangeException
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
	public void reqAddUsingNote ( ArrayList<UseNoteData> noteList ) throws ProtocolNotSupportedException, OutOfRangeException
	{
		currentAdt.reqAddUsingNote(noteList);
	}

	@Override
	public void reqOfflineData(int sectionId, int ownerId, int noteId) 
	{
        currentAdt.reqOfflineData( sectionId, ownerId, noteId);
	}

	@Override
	public void reqOfflineData(int sectionId, int ownerId, int noteId, boolean deleteOnFinished) throws ProtocolNotSupportedException
	{
		currentAdt.reqOfflineData( sectionId, ownerId, noteId, deleteOnFinished );
	}

	@Override
	public void reqOfflineData(int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException, OutOfRangeException
	{
		reqOfflineData( sectionId, ownerId, noteId, true, pageIds);
	}

	@Override
	public void reqOfflineData(int sectionId, int ownerId, int noteId, boolean deleteOnFinished, int[] pageIds) throws ProtocolNotSupportedException, OutOfRangeException
	{
		currentAdt.reqOfflineData( sectionId, ownerId, noteId, deleteOnFinished, pageIds);
	}

	@Override
	public void reqOfflineData(Object extra, int sectionId, int ownerId, int noteId) {
		currentAdt.reqOfflineData( extra, sectionId, ownerId, noteId );
	}


	@Override
	public void reqOfflineData(Object extra, int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException {
		currentAdt.reqOfflineData( extra, sectionId, ownerId, noteId ,pageIds);
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
	public void removeOfflineDataByPage(int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException {
		currentAdt.removeOfflineDataByPage(sectionId, ownerId, noteId,pageIds);
	}

	@Override
	public void reqOfflineNoteInfo( int sectionId, int ownerId, int noteId ) throws ProtocolNotSupportedException
	{
		currentAdt.reqOfflineNoteInfo( sectionId, ownerId, noteId );
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
	public void reqSetupPenSensitivityFSC( short level ) throws ProtocolNotSupportedException
	{
		currentAdt.reqSetupPenSensitivityFSC( level );
	}

	@Override
	public void reqSetupPenCapOff ( boolean on ) throws ProtocolNotSupportedException
	{
		currentAdt.reqSetupPenCapOff( on );
	}

	@Override
	public void reqSetupPenHover ( boolean on )  throws ProtocolNotSupportedException
	{
		currentAdt.reqSetupPenHover( on );
	}

    @Override
    public void reqSetupPenDiskReset()  throws ProtocolNotSupportedException
    {
        currentAdt.reqSetupPenDiskReset();
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

	/**
	 * Get press sensor type int.
	 *
	 * @return the int
	 */
	public int getPressSensorType(){
		return currentAdt.getPressSensorType();
	}

	/**
	 * Gets current adt.
	 *
	 * @return the current adt
	 */
	public IPenAdt getCurrentAdt()
	{
		return currentAdt;
	}



	public void clear()
	{
		currentAdt.clear();
	}

	@Override
	public boolean isSupportHoverCommand()  throws ProtocolNotSupportedException
	{
		return currentAdt.isSupportHoverCommand();
	}

	@Override
	public void reqSystemInfo() throws ProtocolNotSupportedException {
		currentAdt.reqSystemInfo();
	}

	@Override
	public void reqSetPerformance(int step) throws ProtocolNotSupportedException {
		currentAdt.reqSetPerformance(step);
	}

}


