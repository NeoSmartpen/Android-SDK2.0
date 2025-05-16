package kr.neolab.sdk.pen;

import android.content.Context;

import java.io.File;
import java.io.PipedInputStream;
import java.util.ArrayList;

import kr.neolab.sdk.metadata.IMetadataListener;
import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.bluetooth.lib.OutOfRangeException;
import kr.neolab.sdk.pen.bluetooth.lib.ProfileKeyValueLimitException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.util.UseNoteData;

/**
 * Pen and an adapter class that defines the communication interface
 * <p>
 * Bluetooth communication is currently supported.
 * (If you want a different way to implement this interface must be provided.)
 *
 * @author CHY
 */
public interface IPenAdt
{
    /**
     * The constant CONN_STATUS_IDLE.
     */
    int CONN_STATUS_IDLE = 0x01;
    /**
     * The constant CONN_STATUS_BINDED.
     */
    int CONN_STATUS_BINDED = 0x02;
    /**
     * The constant CONN_STATUS_ESTABLISHED.
     */
    int CONN_STATUS_ESTABLISHED = 0x03;
    /**
     * The constant CONN_STATUS_AUTHORIZED.
     */
    int CONN_STATUS_AUTHORIZED = 0x04;
    /**
     * The constant CONN_STATUS_TRY.
     */
    int CONN_STATUS_TRY = 0x05;

    /**
     * set up listener of message from pen
     *
     * @param listener callback interface
     */
    public void setListener(IPenMsgListener listener);

    /**
     * set up listener of dot message from pen
     *
     * @param listener the listener
     */
    public void setDotListener(IPenDotListener listener);

    /**
     * set up listener of offlineData from pen
     * supported from Protocol 2.0
     *
     * @param listener callback interface
     */
    public void setOffLineDataListener(IOfflineDataListener listener);

    /**
     * set up listener of metadata processing
     * supported from Protocol 2.0
     *
     * @param listener callback interface
     */
    public void setMetadataListener( IMetadataListener listener );

    /**
     * get listener of message from pen
     *
     * @return IPenMsgListener listener
     */
    public IPenMsgListener getListener();

    public IPenDotListener getDotListener();

    /**
     * get listener of offlineData from pen
     * supported from Protocol 2.0
     *
     * @return IOfflineDataListener off line data listener
     */
    public IOfflineDataListener getOffLineDataListener();

    /**
     * get listener of metadata processing
     * supported from Protocol 2.0
     *
     * @return IMetadataListener metadata listener
     */
    public IMetadataListener getMetadataListener();

    /**
     * Attempts to connect to the pen.
     *
     * @param address MAC address of pen
     */
    public void connect(String address) throws BLENotSupportedException;

    /**
     * And disconnect the connection with pen
     */
    public void disconnect();

    /**
     * Confirm whether or not the MAC address to connect
     * If use ble adapter, throws BLENotSupprtedException
     *
     * @param mac the mac
     * @return true if can use, otherwise false
     * @throws BLENotSupportedException the ble not supported exception
     */
    public boolean isAvailableDevice( String mac ) throws BLENotSupportedException;

    /**
     * Confirm whether or not the MAC address to connect
     * NOTICE
     * SPP must use mac address bytes
     * BLE must use advertising full data ( ScanResult.getScanRecord().getBytes() )
     *
     * @param mac the mac
     * @return true if can use, otherwise false
     */
    public boolean isAvailableDevice( byte[] mac );

    /**
     * Connected to the pen's current information.
     *
     * @return connected device
     */
    public String getConnectedDevice();

    /**
     * Gets connecting device.
     *
     * @return the connecting device
     */
    public String getConnectingDevice();

    /**
     * When pen requested password, you can response password by this method.
     *
     * @param password the password
     */
    public void inputPassword( String password );

    /**
     * Change the password of pen.
     *
     * @param oldPassword current password
     * @param newPassword new password
     */
    public void reqSetupPassword( String oldPassword, String newPassword );

    /**
     * Req set up password off.
     * supported from Protocol 2.0
     *
     * @param oldPassword the old password
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetUpPasswordOff( String oldPassword ) throws ProtocolNotSupportedException;

    /**
     * Connected to the current state of the pen provided.
     */
    public void reqPenStatus();

    /**
     * To upgrade the firmware of the pen.
     *
     * @param fwFile     object of firmware
     * @param targetPath The file path to be stored in the pen
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @deprecated reqFwUpgrade(File fwFile, String targetPath) is replaced by reqFwUpgrade2( File fwFile, String fwVersion ) in the protocol 2.0
     */
    public void reqFwUpgrade(File fwFile, String targetPath) throws ProtocolNotSupportedException;

    /**
     * Req fw upgrade 2.
     * supported from Protocol 2.0
     * isCompress default true
     *
     * @param fwFile    the fw file
     * @param fwVersion the fw version
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqFwUpgrade2( File fwFile, String fwVersion) throws ProtocolNotSupportedException;

    /**
     * Req fw upgrade 2.
     *
     * @param fwFile     the fw file
     * @param fwVersion  the fw version
     * @param isCompress data compress true, uncompress false
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @deprecated isCompress param
     */
    public void reqFwUpgrade2( File fwFile, String fwVersion ,boolean isCompress)  throws ProtocolNotSupportedException;

    /**
     * To suspend Upgrading task.
     */
    public void reqSuspendFwUpgrade();

    /**
     * Adjust the pressure-sensor to the pen.
     */
    public void reqForceCalibrate();

    /**
     * Sets calibrate.
     *
     * @param factor the factor
     */
    public void reqCalibrate2(float[] factor);

    /**
     * Specify whether you want to get the data off-line.
     *
     * @param allow if allow receive offline data, set true
     */
    public void setAllowOfflineData(boolean allow);

    /**
     * Specify where to store the offline data. (Unless otherwise specified, is stored in the default external storage)
     *
     * @param path Be stored in the directory
     */
    public void setOfflineDataLocation(String path);

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteIds   array of note id
     */
    public void reqAddUsingNote(int sectionId, int ownerId, int[] noteIds) throws OutOfRangeException;

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     */
    public void reqAddUsingNote(int sectionId, int ownerId);

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param sectionId section ids of note
     * @param ownerId   owner ids of note
     */
    public void reqAddUsingNote(int[] sectionId, int[] ownerId);

    /**
     * Specifies that all of the available notes.
     * Note! It overwrites the using note data from protocol 2.0 .
     */
    public void reqAddUsingNoteAll();

    /**
     * Notes for use in applications specified.
     * supported from Protocol 2.0
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param noteList the note list
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqAddUsingNote(ArrayList<UseNoteData> noteList) throws ProtocolNotSupportedException, OutOfRangeException;

    /**
     * The pen is stored in an offline transfer of data requested.
     * (Please note that this function is not synchronized. If multiple threads concurrently try to run this function, explicit synchronization must be done externally.)
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteId    of note
     */
    public void reqOfflineData(int sectionId, int ownerId, int noteId);

    /**
     * The pen is stored in an offline transfer of data requested.
     * (Please note that this function is not synchronized. If multiple threads concurrently try to run this function, explicit synchronization must be done externally.)
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteId    of note
     * @param deleteOnFinished  delete offline data when transmission is finished
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineData(int sectionId, int ownerId, int noteId, boolean deleteOnFinished) throws ProtocolNotSupportedException;

    /**
     * The pen is stored in an offline transfer of data requested.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param pageIds   the page ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineData(int sectionId, int ownerId, int noteId, int[] pageIds) throws ProtocolNotSupportedException, OutOfRangeException;

    /**
     * The pen is stored in an offline transfer of data requested.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param deleteOnFinished  delete offline data when transmission is finished
     * @param pageIds   the page ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineData(int sectionId, int ownerId, int noteId, boolean deleteOnFinished, int[] pageIds) throws ProtocolNotSupportedException, OutOfRangeException;

    /**
     * The pen is stored in an offline transfer of data requested.
     * (Please note that this function is not synchronized. If multiple threads concurrently try to run this function, explicit synchronization must be done externally.)
     *
     * @param extra extra data
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteId    of note
     */
    public void reqOfflineData(Object extra,int sectionId, int ownerId, int noteId);

    /**
     * The pen is stored in an offline transfer of data requested.
     * supported from Protocol 2.0
     *
     * @param extra extra data
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param pageIds   the page ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineData(Object extra, int sectionId, int ownerId, int noteId , int[] pageIds) throws ProtocolNotSupportedException;


    /**
     * The offline data is stored in the pen to request information.
     */
    public void reqOfflineDataList();

    /**
     * The offline data is stored in the pen to request information.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineDataList(int sectionId, int ownerId) throws ProtocolNotSupportedException;

    /**
     * The offline data per page is stored in the pen to request information.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineDataPageList(int sectionId, int ownerId, int noteId) throws ProtocolNotSupportedException;

    /**
     * To Delete offline data of pen
     *
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @deprecated removeOfflineData(int sectionId, int ownerId) is replaced by removeOfflineData( int sectionId, int ownerId ,int[] noteIds) in the protocol 2.0
     */
    public void removeOfflineData(int sectionId, int ownerId) throws ProtocolNotSupportedException;

    /**
     * Remove offline data.
     * supported from Protocol 2.0
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteIds   the note ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void removeOfflineData( int sectionId, int ownerId ,int[] noteIds) throws ProtocolNotSupportedException;


    /**
     * Remove offline data.
     * supported from Protocol 2.23
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId   the note id
     * @param pageIds   the page ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void removeOfflineDataByPage ( int sectionId, int ownerId, int noteId, int[] pageIds ) throws ProtocolNotSupportedException;



    /**
     * Request offline note info.
     * supported from Protocol 2.16
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId   the note id
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineNoteInfo( int sectionId, int ownerId ,int noteId) throws ProtocolNotSupportedException;

    /**
     * Disable or enable Auto Power function
     *
     * @param setOn the set on
     */
    public void reqSetupAutoPowerOnOff( boolean setOn );

    /**
     * Disable or enable sound of pen
     *
     * @param setOn the set on
     */
    public void reqSetupPenBeepOnOff( boolean setOn );

    /**
     * Setup color of pen
     *
     * @param color the color
     */
    public void reqSetupPenTipColor( int color );

    /**
     * Setup auto shutdown time of pen
     *
     * @param minute shutdown wait time of pen
     */
    public void reqSetupAutoShutdownTime( short minute );

    /**
     * Setup Sensitivity level of pen
     *
     * @param level sensitivity level (0~4)
     */
    public void reqSetupPenSensitivity( short level );

    /**
     * Notice!!
     * This is API for hardware developer!
     * Software developers should never use this API.
     * <p>
     * Setup Sensitivity level of pen using FSC press sensor
     * <p>
     * supported from Protocol 2.0
     *
     * @param level sensitivity level (0~4)
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetupPenSensitivityFSC (  short level ) throws ProtocolNotSupportedException;

    /**
     * Req set pen cap on off.
     * supported from Protocol 2.0
     *
     * @param on the on
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetupPenCapOff ( boolean on ) throws ProtocolNotSupportedException;

    /**
     * Req setup pen hover.
     *
     * @param on the on
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetupPenHover ( boolean on ) throws ProtocolNotSupportedException;

    /**
     * Req setup pen disk reset.
     * supported from Protocol 2.15
     *
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetupPenDiskReset ( ) throws ProtocolNotSupportedException;

    /**
     * set Context
     *
     * @param context the context
     */
    public void setContext( Context context );

    /**
     * get Context
     *
     * @return Context context
     */
    public Context getContext();

    /**
     * Gets protocol version.
     * supported from Protocol 2.0
     *
     * @return the protocol version
     */
    public int getProtocolVersion();

    /**
     * Gets pen status.
     *
     * @return the pen status
     */
    public int getPenStatus ();

    /**
     * Get connected status
     *
     * @return true is connected, false is disconnected
     */
    public boolean isConnected();

    /**
     * Get Pen Mac Address
     * BT Mac address is Real mac address
     * BLE mac address is virtual mac address
     *
     * @return the pen address
     */
    public String getPenAddress();

    /**
     * Get Pen Bt Name
     *
     * @return the bt name
     */
    public String getPenBtName();

    /**
     * Gets press sensor type.
     *
     * @return the press sensor type
     */
    public int getPressSensorType();

    /**
     * Gets connect device name.
     * supported from Protocol 2.0
     *
     * @return the connect device name
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public String getConnectDeviceName () throws ProtocolNotSupportedException;

    /**
     * Gets connect sub name.
     * supported from Protocol 2.0
     *
     * @return the connect sub name
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public String getConnectSubName () throws ProtocolNotSupportedException;

    /**
     * Gets Pen ProtocolVer.
     * supported from Protocol 2.0
     *
     * @return the Pen ProtocolVer
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public String getReceiveProtocolVer () throws ProtocolNotSupportedException;

    /**
     * Gets FirmwareVer.
     * supported from Protocol 2.0
     *
     * @return the FirmwareVer
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public String getFirmwareVer () throws ProtocolNotSupportedException;


    /**
     * Create profile.
     *
     * @param proFileName the pro file name
     * @param password    the password
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void createProfile ( String proFileName , byte[] password) throws ProtocolNotSupportedException,ProfileKeyValueLimitException;

    /**
     * Delete profile.
     *
     * @param proFileName the pro file name
     * @param password    the password
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void deleteProfile ( String proFileName, byte[] password ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException;

    /**
     * Write profile value.
     *
     * @param proFileName the pro file name
     * @param password    the password
     * @param keys        the keys
     * @param data        the data
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void writeProfileValue ( String proFileName, byte[] password ,String[] keys, byte[][] data ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException;

    /**
     * Read profile value.
     *
     * @param proFileName the pro file name
     * @param keys        the keys
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void readProfileValue ( String proFileName, String[] keys ) throws ProtocolNotSupportedException,ProfileKeyValueLimitException;

    /**
     * Delete profile value.
     *
     * @param proFileName the pro file name
     * @param password    the password
     * @param keys        the keys
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void deleteProfileValue ( String proFileName, byte[] password, String[] keys) throws ProtocolNotSupportedException,ProfileKeyValueLimitException;

    /**
     * Gets profile info.
     *
     * @param proFileName the pro file name
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void getProfileInfo ( String proFileName) throws ProtocolNotSupportedException,ProfileKeyValueLimitException;

    /**
     * Is support pen profile boolean.
     *
     * @return the boolean
     */
    public boolean isSupportPenProfile();

    /**
     * Unpair device boolean.
     *
     * @param address the address
     * @return if success, return true
     */
    public boolean unpairDevice(String address);

    /**
     * Req set current time.
     */
    public void reqSetCurrentTime ();


    /**
     * Set PipedInputStream
     * @param pipedInputStream
     */
    public void setPipedInputStream(PipedInputStream pipedInputStream);

    /**
     * unknown :0  1,2,3...
     *
     * @return the pen color(type) code
     */
    public short getColorCode()throws ProtocolNotSupportedException;
    /**
     * unknown :0  1,2,3...
     *
     * @return the pen product code
     */
    public short getProductCode()throws ProtocolNotSupportedException;
    /**
     * unknown :0  1,2,3...
     *
     * @return the pen company code
     */
    public short getCompanyCode()throws ProtocolNotSupportedException;


    /*
        if do not receive 'Disconnect Msg' when bluetooth turn off, you have to use this method when bluetooth turn off.
        (Device bug)
     */
    public void clear();

    /**
     * Is support hover command boolean.
     *
     * @return the boolean
     */
    public boolean isSupportHoverCommand()throws ProtocolNotSupportedException;


    /**
     * Gets connect pen type
     * 1 pen 2 eraser 3 player 4 wired pen 5 sound pen
     * @return the connect pen type
     */
    public int getConnectPenType () throws ProtocolNotSupportedException;


    /**
     * Request System Information.
     * @throws ProtocolNotSupportedException
     */
    void reqSystemInfo() throws ProtocolNotSupportedException;

    /**
     * Request setting performance step.
     *
     * @param step
     * @throws ProtocolNotSupportedException
     */
    void reqSetPerformance(int step) throws ProtocolNotSupportedException;
}
