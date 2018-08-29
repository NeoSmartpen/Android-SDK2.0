package kr.neolab.sdk.pen;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import kr.neolab.sdk.broadcastreceiver.BTDuplicateRemoveBroadcasterReceiver;
import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.bluetooth.BTAdt;
import kr.neolab.sdk.pen.bluetooth.BTLEAdt;
import kr.neolab.sdk.pen.bluetooth.lib.ProfileKeyValueLimitException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.util.NLog;
import kr.neolab.sdk.util.SDKVersion;
import kr.neolab.sdk.util.UseNoteData;

import static kr.neolab.sdk.pen.IPenAdt.CONN_STATUS_IDLE;

/**
 * The class that provides the functionality of a pen
 *
 * @author Moo
 */
public class MultiPenCtrl implements IMultiPenCtrl {
//    private IPenAdt currentAdt = null;
	private HashMap<String, IPenAdt> mConnectedCollection;

	private static MultiPenCtrl myInstance = null;
    private static BTDuplicateRemoveBroadcasterReceiver mReceiver;
	private IPenMsgListener listener = null;
	private IPenDotListener dotListener = null;
	private IOfflineDataListener offlineDataListener = null;
	private Context mContext = null;

	private MultiPenCtrl ()
    {
		mConnectedCollection = new HashMap<String, IPenAdt>();

//		currentAdt = BTAdt.getInstance();
    }

	/**
	 * Gets an instance of the Pen Controller
	 *
	 * @return instance instance
	 */
	public synchronized static MultiPenCtrl getInstance() {
        if (myInstance == null) {
            myInstance = new MultiPenCtrl();
        }
        if (mReceiver == null) {
            mReceiver = new BTDuplicateRemoveBroadcasterReceiver();
        }

        return myInstance;
    }

    @Override
    public void setContext(Context context) {
		this.mContext = context;
		for(IPenAdt pen : mConnectedCollection.values())
		{
			pen.setContext( mContext );
		}
//        if( Build.VERSION.SDK_INT >= 21)
//            BTLEAdt.getInstance().setContext( context );
//        BTAdt.getInstance().setContext(context);
    }

    @Override
    public void setListener(IPenMsgListener listener) {
		this.listener = listener;
		for(IPenAdt pen : mConnectedCollection.values())
		{
			pen.setListener( this.listener );
		}
//		if( Build.VERSION.SDK_INT >= 21)
//            BTLEAdt.getInstance().setListener( listener );
//        BTAdt.getInstance().setListener(listener);
    }

    @Override
    public void setDotListener(IPenDotListener listener) {
		this.dotListener = listener;
		for(IPenAdt pen : mConnectedCollection.values())
		{
			pen.setDotListener( this.dotListener );
		}

//        if( Build.VERSION.SDK_INT >= 21)
//            BTLEAdt.getInstance().setDotListener( listener );
//        BTAdt.getInstance().setDotListener(listener);
    }

    @Override
    public void setOffLineDataListener(IOfflineDataListener listener) {
		this.offlineDataListener = listener;
		for(IPenAdt pen : mConnectedCollection.values())
		{
			pen.setOffLineDataListener( this.offlineDataListener );
		}
    }


    @Override
    public IPenMsgListener getListener(String address) {
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			return mPenAdt.getListener();
		else
			return null;
    }

	public IPenDotListener getDotListener(String address) {
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			return mPenAdt.getDotListener();
		else
			return null;
	}


	@Override
    public IOfflineDataListener getOffLineDataListener(String address) {
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			return mPenAdt.getOffLineDataListener();
		else
			return null;
    }

    @Override
    public void connect(String address, boolean isLeMode) {
		if(mConnectedCollection.containsKey(address) && ((isLeMode && mConnectedCollection.get(address) instanceof BTLEAdt) || (!isLeMode && mConnectedCollection.get(address) instanceof BTAdt)))
		{
			if(mConnectedCollection.get(address) instanceof BTAdt)
				((BTAdt)mConnectedCollection.get(address)).connect(address);
			else
				((BTLEAdt)mConnectedCollection.get(address)).connect(address);

		}
		else
		{
			IPenAdt mBTAdt;
			if ( !isLeMode ) mBTAdt = new BTAdt();
			else
				mBTAdt = new BTLEAdt();
			mBTAdt.setContext(mContext);
			mBTAdt.setListener(this.listener);
			mBTAdt.setDotListener( this.dotListener );
			if(offlineDataListener != null)
				mBTAdt.setOffLineDataListener( this.offlineDataListener );
			mConnectedCollection.put(address, mBTAdt);
			if(mBTAdt instanceof BTAdt )
				((BTAdt)mBTAdt).connect(address);
			else
				((BTLEAdt)mBTAdt).connect(address);
		}
    }

//	public boolean setLeMode(boolean isLeMode)
//	{
//		if (currentAdt != null && currentAdt.getPenStatus() != IPenAdt.CONN_STATUS_IDLE)
//		{
//			return false;
//		}
//
//		if (isLeMode)
//		{
//			if( Build.VERSION.SDK_INT >= 21)
//				currentAdt = BTLEAdt.getInstance();
//			else
//				return false;
//		}
//		else
//			currentAdt = BTAdt.getInstance();
//
//		return true;
//	}



	@Override
    public void disconnect(String address) {
        NLog.i("[BTCtrl] disconnect pen="+address);
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.disconnect();
    }

    @Override
    public boolean isAvailableDevice(String mac) throws BLENotSupportedException {
		return BTAdt.getInstance().isAvailableDevice( mac );
    }

    @Override
    public boolean isAvailableDevice(byte[] mac, boolean isLeMode)
    {
		if(isLeMode)
			return BTLEAdt.getInstance().isAvailableDevice( mac );
		else
			return BTAdt.getInstance().isAvailableDevice( mac );
    }

    @Override
    public ArrayList<String> getConnectedDevice()
    {
		ArrayList<String> ret = new ArrayList<String>();
		for(IPenAdt pen : mConnectedCollection.values())
		{
			if ( pen.isConnected() )
			{
				ret.add( pen.getConnectedDevice() );
			}
		}
        return ret;
    }

	@Override
	public ArrayList<String> getConnectingDevice ()
	{
		ArrayList<String> ret = new ArrayList<String>();
		for(IPenAdt pen : mConnectedCollection.values())
		{
			if ( pen.getConnectingDevice() != null )
			{
				ret.add( pen.getConnectingDevice() );
			}
		}
		return ret;
	}

	@Override
	public void inputPassword(String address, String password )
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.inputPassword(password);
	}

	@Override
	public void reqSetupPassword( String address, String oldPassword, String newPassword )
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSetupPassword( oldPassword, newPassword );
	}

	@Override
	public void reqSetUpPasswordOff ( String address, String oldPassword ) throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSetUpPasswordOff( oldPassword );
	}

	@Override
	public void setAllowOfflineData(String address, boolean allow)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.setAllowOfflineData( allow );
	}
	
	@Override
	public synchronized void setOfflineDataLocation(String address, String path)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.setOfflineDataLocation( path );

	}

	/**
	 * Sets calibrate 2.
	 *
	 * @param address the address
	 * @param factor  the factor
	 */
	public void setCalibrate2 ( String address, float[] factor )
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqCalibrate2( factor);
	}


	@Override
	public void upgradePen(String address, File fwFile) throws ProtocolNotSupportedException
	{   
		String target = "\\NEO1.zip";
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqFwUpgrade( fwFile, target);
	}

	@Override
	public void upgradePen2 ( String address, File fwFile, String fwVersion ) throws ProtocolNotSupportedException
	{
		upgradePen2 ( address, fwFile, fwVersion, true );
	}

	@Override
	public void upgradePen2 ( String address, File fwFile, String fwVersion, boolean isCompress ) throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqFwUpgrade2( fwFile, fwVersion, isCompress );
	}

	@Override
	public void upgradePen(String address, File fwFile, String targetPath)  throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqFwUpgrade(fwFile, targetPath);
	}
	
	@Override
	public void suspendPenUpgrade(String address )
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSuspendFwUpgrade();
	}
	
	@Override
	public void reqAddUsingNote(String address, int sectionId, int ownerId, int[] noteIds)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqAddUsingNote( sectionId, ownerId, noteIds );
	}

	@Override
	public void reqAddUsingNote( String address, int sectionId, int ownerId )
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqAddUsingNote(sectionId, ownerId);
	}

	@Override
	public void reqAddUsingNote(String address, int[] sectionId, int[] ownerId) {
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqAddUsingNote(sectionId, ownerId);

	}

	@Override
	public void reqAddUsingNoteAll(String address)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqAddUsingNoteAll();
	}

	@Override
	public void reqAddUsingNote ( String address, ArrayList<UseNoteData> noteList ) throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqAddUsingNote(noteList);
	}

	@Override
	public void reqOfflineData(String address, int sectionId, int ownerId, int noteId)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqOfflineData( sectionId, ownerId, noteId );
	}

	@Override
	public void reqOfflineData(String address, int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqOfflineData( sectionId, ownerId, noteId ,pageIds);
	}

	@Override
	public void reqOfflineDataList(String address)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqOfflineDataList();
	}

	@Override
	public void reqOfflineDataList(String address, int sectionId, int ownerId) throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqOfflineDataList(sectionId, ownerId);
	}

	@Override
	public void reqOfflineDataPageList(String address, int sectionId, int ownerId, int noteId) throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqOfflineDataPageList( sectionId, ownerId, noteId );
	}

	@Override
	public void removeOfflineData( String address, int sectionId, int ownerId ) throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.removeOfflineData( sectionId, ownerId );
	}

	@Override
	public void removeOfflineData ( String address, int sectionId, int ownerId, int[] noteIds ) throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.removeOfflineData(sectionId, ownerId, noteIds);
	}

	@Override
	public void reqPenStatus(String address)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqPenStatus();
	}

	@Override
	public void reqSetupAutoPowerOnOff(String address, boolean setOn)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSetupAutoPowerOnOff( setOn );
	}
	
	@Override
    public void reqSetupPenBeepOnOff( String address, boolean setOn )
    {
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSetupPenBeepOnOff( setOn );
    }

	@Override
	public void reqSetupPenTipColor( String address, int color )
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSetupPenTipColor( color );
	}

	@Override
	public void reqSetupAutoShutdownTime( String address, short minute )
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSetupAutoShutdownTime( minute );
	}

	@Override
	public void reqSetupPenSensitivity( String address, short level )
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSetupPenSensitivity( level );
	}

	@Override
	public void reqSetupPenCapOff ( String address, boolean on ) throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSetupPenCapOff( on );
	}

	@Override
	public void reqSetupPenHover ( String address, boolean on )  throws ProtocolNotSupportedException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.reqSetupPenHover( on );
	}


//	@Override
//	public void registerBroadcastBTDuplicate() {
//		try {
//			IntentFilter filter = new IntentFilter();
//			filter.addAction(BTDuplicateRemoveBroadcasterReceiver.ACTION_BT_REQ_CONNECT);
//			filter.addAction(BTDuplicateRemoveBroadcasterReceiver.ACTION_BT_RESPONSE_CONNECTED);
//			if(mContext != null)
//				mContext.registerReceiver(mReceiver, filter);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	@Override
//	public void unregisterBroadcastBTDuplicate() {
//		try {
//			if(mContext != null)
//				mContext.unregisterReceiver(mReceiver);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	@Override
	public String getVersion() {
		return SDKVersion.version;
	}

	@Override
	public int getProtocolVersion (String address)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			return mPenAdt.getProtocolVersion();
		else
			return 0;
	}

	@Override
	public boolean setLeMode(String address, boolean isLeMode)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if (mPenAdt != null && mPenAdt.getPenStatus() != IPenAdt.CONN_STATUS_IDLE)
		{
			return false;
		}

		if (isLeMode)
		{
			if( Build.VERSION.SDK_INT >= 21)
				mPenAdt = BTLEAdt.getInstance();
			else
				return false;
		}
		else
			mPenAdt = BTAdt.getInstance();

		return true;
	}

	@Override
	public void createProfile ( String address, String proFileName, byte[] password ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.createProfile ( proFileName, password );
	}

	@Override
	public void deleteProfile ( String address, String proFileName, byte[] password ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.deleteProfile ( proFileName, password );

	}

	@Override
	public void writeProfileValue ( String address, String proFileName, byte[] password, String[] keys, byte[][] data ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.writeProfileValue ( proFileName, password ,keys, data);

	}

	@Override
	public void readProfileValue ( String address, String proFileName, String[] keys ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.readProfileValue ( proFileName, keys );

	}

	@Override
	public void deleteProfileValue ( String address, String proFileName, byte[] password, String[] keys ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.deleteProfileValue ( proFileName, password , keys);

	}

	@Override
	public void getProfileInfo ( String address, String proFileName ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			mPenAdt.getProfileInfo ( proFileName);

	}

	@Override
	public boolean isSupportPenProfile (String address)
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			return mPenAdt.isSupportPenProfile();
		else
			return false;
	}

	@Override
	public boolean unpairDevice ( String address )
	{
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
		{
			if(mPenAdt.unpairDevice(address))
			{
				mConnectedCollection.remove( address );
				return true;
			}
			else
				return false;
		}
		else
			return false;
	}

	/**
	 * Gets pen status.
	 *
	 * @param address the address
	 * @return the pen status
	 */
	public int getPenStatus(String address) {
		IPenAdt mPenAdt = (mConnectedCollection.get(address));
		if(mPenAdt != null)
			return mPenAdt.getPenStatus();
		else
			return CONN_STATUS_IDLE;
	}

}


