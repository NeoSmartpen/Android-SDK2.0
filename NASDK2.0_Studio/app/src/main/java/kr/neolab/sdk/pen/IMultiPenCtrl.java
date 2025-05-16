package kr.neolab.sdk.pen;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;

import kr.neolab.sdk.metadata.IMetadataListener;
import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.bluetooth.BTLEAdt;
import kr.neolab.sdk.pen.bluetooth.lib.OutOfRangeException;
import kr.neolab.sdk.pen.bluetooth.lib.ProfileKeyValueLimitException;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.util.UseNoteData;

/**
 * The interface that provides the functionality of a pen
 *
 * @author CHY
 */
public interface IMultiPenCtrl
{
    /**
     * Set up listener of message from pen
     *
     * @param listener callback interface
     */
    public void setListener ( IPenMsgListener listener );

    /**
     * set up listener of dot message from pen
     *
     * @param listener the listener
     */
    public void setDotListener ( IPenDotListener listener );

    /**
     * set up listener of offlineData from pen
     * supported from Protocol 2.0
     *
     * @param listener callback interface
     */
    public void setOffLineDataListener ( IOfflineDataListener listener );

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
     * @param address MAC address of pen
     * @return IPenMsgListener listener
     */
    public IPenMsgListener getListener ( String address );

    /**
     * get listener of offlineData from pen
     * supported from Protocol 2.0
     *
     * @param address MAC address of pen
     * @return IOfflineDataListener off line data listener
     */
    public IOfflineDataListener getOffLineDataListener ( String address );

    /**
     * get listener of metadata processing
     * supported from Protocol 2.0
     *
     * @param address MAC address of pen
     * @return IMetadataListener metadata listener
     */
    public IMetadataListener getMetadataListener(String address);

    /**
     * Attempts to connect to the pen.
     *
     * @param address MAC address of pen
     */
    public void connect(String address) throws BLENotSupportedException;

    /**
     * Attempts to connect to the pen.
     *
     * @param sppAddress  MAC address of pen
     * @param leAddress  leAddress
     * @param isLeMode the is le mode
     */
    public void connect ( String sppAddress, String leAddress, boolean isLeMode );

    /**
     * Attempts to connect to the pen.
     *
     * @param sppAddress    spp address of pen
     * @param leAddress     le address of pen
     * @param uuidVer        uuid version
     * @param appType       see document(example AOS NeoNotes: 0x1101)
     * @param reqProtocolVer  input the desired protocol version
     */
    public void connect(String sppAddress, String leAddress, boolean isLeMode, BTLEAdt.UUID_VER uuidVer, short appType, String reqProtocolVer);


    /**
    * And disconnect the connection with pen
    *
    * @param address MAC address of pen
    */
    public void disconnect ( String address );

    /**
     * Connected to the pen's current information.
     *
     * @return connected device
     */
    public ArrayList<String> getConnectedDevice ();

    /**
     * Gets connecting device.
     *
     * @return the connecting device
     */
    public ArrayList<String> getConnectingDevice ();

    /**
     * Confirm whether or not the MAC address to connect
     * If use ble adapter, throws BLENotSupprtedException
     *
     * @param mac MAC address of pen
     * @return true if can use, otherwise false
     * @throws BLENotSupportedException the ble not supported exception
     */
    public boolean isAvailableDevice ( String mac ) throws BLENotSupportedException;

    /**
     * Confirm whether or not the MAC address to connect
     * NOTICE
     * SPP must use mac address bytes
     * BLE must use advertising full data ( ScanResult.getScanRecord().getBytes() )
     *
     * @param mac      MAC address of pen
     * @param isLeMode the is le mode
     * @return true if can use, otherwise false
     */
    public boolean isAvailableDevice ( byte[] mac, boolean isLeMode );

    /**
     * When pen requested password, you can response password by this method.
     *
     * @param address  MAC address of pen
     * @param password the password
     */
    public void inputPassword ( String address, String password );

    /**
     * Change the password of pen.
     *
     * @param address     MAC address of pen
     * @param oldPassword current password
     * @param newPassword new password
     */
    public void reqSetupPassword ( String address, String oldPassword, String newPassword );

    /**
     * Req set up password off.
     * supported from Protocol 2.0
     *
     * @param address     MAC address of pen
     * @param oldPassword the old password
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetUpPasswordOff ( String address, String oldPassword ) throws ProtocolNotSupportedException;

    /**
     * Specify whether you want to get the data off-line.
     *
     * @param address MAC address of pen
     * @param allow   if allow receive offline data, set true
     */
    public void setAllowOfflineData ( String address, boolean allow );

    /**
     * Specify where to store the offline data. (Unless otherwise specified, is stored in the default external storage)
     *
     * @param address MAC address of pen
     * @param path    Be stored in the directory
     * @deprecated
     */
    public void setOfflineDataLocation ( String address, String path );

    /**
     * To upgrade the firmware of the pen.
     *
     * @param address    MAC address of pen
     * @param fwFile     the fw file
     * @param targetPath The file path to be stored in the pen
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @deprecated upgradePen(File fwFile, String targetPath) is replaced by upgradePen2( File fwFile, String fwVersion ) in the protocol 2.0
     */
    public void upgradePen ( String address, File fwFile, String targetPath ) throws ProtocolNotSupportedException;

    /**
     * To upgrade the firmware of the pen.
     *
     * @param address MAC address of pen
     * @param fwFile  object of firmware
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @deprecated upgradePen(File fwFile) is replaced by upgradePen2( File fwFile, String fwVersion ) in the protocol 2.0
     */
    public void upgradePen ( String address, File fwFile ) throws ProtocolNotSupportedException;

    /**
     * fw upgrade 2.
     * supported from Protocol 2.0
     * isCompress default true
     *
     * @param address   MAC address of pen
     * @param fwFile    the fw file
     * @param fwVersion the fw version
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void upgradePen2 ( String address, File fwFile, String fwVersion ) throws ProtocolNotSupportedException;

    /**
     * Req fw upgrade 2.
     *
     * @param address    MAC address of pen
     * @param fwFile     the fw file
     * @param fwVersion  the fw version
     * @param isCompress data compress true, uncompress false
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void upgradePen2 ( String address, File fwFile, String fwVersion, boolean isCompress ) throws ProtocolNotSupportedException;

    /**
     * To suspend Upgrading task.
     *
     * @param address MAC address of pen
     */
    public void suspendPenUpgrade ( String address );

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param address   MAC address of pen
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteIds   array of note id
     */
    public void reqAddUsingNote ( String address, int sectionId, int ownerId, int[] noteIds ) throws OutOfRangeException;

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param address   MAC address of pen
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     */
    public void reqAddUsingNote ( String address, int sectionId, int ownerId );

    /**
     * Notes for use in applications specified.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param address   MAC address of pen
     * @param sectionId section ids of note
     * @param ownerId   owner ids of note
     */
    public void reqAddUsingNote ( String address, int[] sectionId, int[] ownerId );

    /**
     * Specifies that all of the available notes.
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param address MAC address of pen
     */
    public void reqAddUsingNoteAll ( String address );

    /**
     * Notes for use in applications specified.
     * supported from Protocol 2.0
     * Note! It overwrites the using note data from protocol 2.0 .
     *
     * @param address  MAC address of pen
     * @param noteList the note list
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqAddUsingNote ( String address, ArrayList<UseNoteData> noteList ) throws ProtocolNotSupportedException, OutOfRangeException;

    /**
     * The pen is stored in an offline transfer of data requested.
     * (Please note that this function is not synchronized. If multiple threads concurrently try to run this function, explicit synchronization must be done externally.)
     *
     * @param address   MAC address of pen
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteId    of note
     */
    public void reqOfflineData ( String address, int sectionId, int ownerId, int noteId );

    /**
     * The pen is stored in an offline transfer of data requested.
     * (Please note that this function is not synchronized. If multiple threads concurrently try to run this function, explicit synchronization must be done externally.)
     *
     * @param address   MAC address of pen
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteId    of note
     * @param deleteOnFinished  delete offline data when transmission is finished
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineData(String address, int sectionId, int ownerId, int noteId, boolean deleteOnFinished) throws ProtocolNotSupportedException;

    /**
     * The pen is stored in an offline transfer of data requested.
     * supported from Protocol 2.0
     *
     * @param address   MAC address of pen
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param pageIds   the page ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineData ( String address, int sectionId, int ownerId, int noteId, int[] pageIds ) throws ProtocolNotSupportedException, OutOfRangeException;

    /**
     * The pen is stored in an offline transfer of data requested.
     * supported from Protocol 2.0
     *
     * @param address   MAC address of pen
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param deleteOnFinished  delete offline data when transmission is finished
     * @param pageIds   the page ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineData(String address, int sectionId, int ownerId, int noteId,  boolean deleteOnFinished, int[] pageIds) throws ProtocolNotSupportedException, OutOfRangeException;

    /**
     * The pen is stored in an offline transfer of data requested.
     * (Please note that this function is not synchronized. If multiple threads concurrently try to run this function, explicit synchronization must be done externally.)
     *
     * @param extra extra data
     * @param address   MAC address of pen
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @param noteId    of note
     */
    public void reqOfflineData(Object extra,String address, int sectionId, int ownerId, int noteId);

    /**
     * The pen is stored in an offline transfer of data requested.
     * supported from Protocol 2.0
     *
     * @param extra extra data
     * @param address   MAC address of pen
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param pageIds   the page ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineData(Object extra, String address, int sectionId, int ownerId, int noteId , int[] pageIds) throws ProtocolNotSupportedException;

    /**
     * The offline data is stored in the pen to request information.
     *
     * @param address MAC address of pen
     */
    public void reqOfflineDataList ( String address );

    /**
     * The offline data is stored in the pen to request information.
     * supported from Protocol 2.0
     *
     * @param address   MAC address of pen
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineDataList ( String address, int sectionId, int ownerId ) throws ProtocolNotSupportedException;

    /**
     * The offline data per page is stored in the pen to request information.
     * supported from Protocol 2.0
     *
     * @param address   MAC address of pen
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineDataPageList ( String address, int sectionId, int ownerId, int noteId ) throws ProtocolNotSupportedException;

    /**
     * To Delete offline data of pen
     *
     * @param address   MAC address of pen
     * @param sectionId section id of note
     * @param ownerId   owner id of note
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @deprecated removeOfflineData(int sectionId, int ownerId) is replaced by removeOfflineData( int sectionId, int ownerId ,int[] noteIds) in the protocol 2.0
     */
    public void removeOfflineData ( String address, int sectionId, int ownerId ) throws ProtocolNotSupportedException;

    /**
     * Remove offline data.
     * supported from Protocol 2.0
     *
     * @param address   MAC address of pen
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteIds   the note ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void removeOfflineData ( String address, int sectionId, int ownerId, int[] noteIds ) throws ProtocolNotSupportedException;


    /**
     * Remove offline data.
     * supported from Protocol 2.23
     *
     * @param address   MAC address of pen
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId   the note id
     * @param pageIds   the page ids
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void removeOfflineDataByPage ( String address, int sectionId, int ownerId, int noteId, int[] pageIds ) throws ProtocolNotSupportedException;


    /**
     * Request offline note info.
     * supported from Protocol 2.16
     *
     * @param address   MAC address of pen
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId   the note id
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqOfflineNoteInfo ( String address, int sectionId, int ownerId, int noteId ) throws ProtocolNotSupportedException;

    /**
     * Connected to the current state of the pen provided.
     *
     * @param address MAC address of pen
     */
    public void reqPenStatus ( String address );

    /**
     * Disable or enable Auto Power function
     *
     * @param address MAC address of pen
     * @param setOn   the set on
     */
    public void reqSetupAutoPowerOnOff ( String address, boolean setOn );

    /**
     * Disable or enable sound of pen
     *
     * @param address MAC address of pen
     * @param setOn   the set on
     */
    public void reqSetupPenBeepOnOff ( String address, boolean setOn );

    /**
     * Setup color of pen
     *
     * @param address MAC address of pen
     * @param color   the color
     */
    public void reqSetupPenTipColor ( String address, int color );

    /**
     * Setup auto shutdown time of pen
     *
     * @param address MAC address of pen
     * @param minute  shutdown wait time of pen
     */
    public void reqSetupAutoShutdownTime ( String address, short minute );

    /**
     * Setup Sensitivity level of pen
     *
     * @param address MAC address of pen
     * @param level   sensitivity level (0~4)
     */
    public void reqSetupPenSensitivity ( String address, short level );

    /**
     * Req set pen cap on off.
     * supported from Protocol 2.0
     *
     * @param address MAC address of pen
     * @param on      the on
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetupPenCapOff ( String address, boolean on ) throws ProtocolNotSupportedException;

    /**
     * Req setup pen hover.
     * supported from Protocol 2.0
     *
     * @param address MAC address of pen
     * @param on      the on
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetupPenHover ( String address, boolean on ) throws ProtocolNotSupportedException;

    /**
     * Req setup pen disk reset.
     * supported from Protocol 2.0
     *
     * @param address MAC address of pen
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public void reqSetupPenDiskReset ( String address ) throws ProtocolNotSupportedException;

//    /**
//     * register Broadcast for remove BT Duplicate connect.
//     */
//    public void registerBroadcastBTDuplicate ();
//
//    /**
//     * unregister Broadcast for remove BT Duplicate connect.
//     */
//    public void unregisterBroadcastBTDuplicate ();

    /**
     * set Context
     *
     * @param context the context
     */
    public void setContext ( Context context );

    /**
     * get SDK version
     *
     * @return version version
     */
    public String getVersion ();

    /**
     * Gets protocol version.
     * supported from Protocol 2.0
     *
     * @param address MAC address of pen
     * @return the protocol version
     */
    public int getProtocolVersion ( String address );

    /**
     * Sets le mode.
     *
     * @param address  the address
     * @param isLeMode the is le mode
     * @return the le mode
     */
    public boolean setLeMode ( String address, boolean isLeMode );

    /**
     * Gets connect device name.
     * supported from Protocol 2.0
     *
     * @return the connect device name
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public String getConnectDeviceName (String address) throws ProtocolNotSupportedException;

    /**
     * Gets connect sub name.
     * supported from Protocol 2.0
     *
     * @return the connect sub name
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public String getConnectSubName (String address) throws ProtocolNotSupportedException;

    /**
     * Gets Pen ProtocolVer.
     * supported from Protocol 2.0
     *
     * @return the Pen ProtocolVer
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public String getReceiveProtocolVer (String address) throws ProtocolNotSupportedException;

    /**
     * Gets FirmwareVer.
     * supported from Protocol 2.0
     *
     * @return the FirmwareVer
     * @throws ProtocolNotSupportedException the protocol not supported exception
     */
    public String getFirmwareVer (String address) throws ProtocolNotSupportedException;

    /**
     * Create profile.
     *
     * @param address     the address
     * @param proFileName the pro file name
     * @param password    the password
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void createProfile ( String address, String proFileName, byte[] password ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException;

    /**
     * Delete profile.
     *
     * @param address     the address
     * @param proFileName the pro file name
     * @param password    the password
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void deleteProfile ( String address, String proFileName, byte[] password ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException;

    /**
     * Write profile value.
     *
     * @param address     the address
     * @param proFileName the pro file name
     * @param password    the password
     * @param keys        the keys
     * @param data        the data
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void writeProfileValue ( String address, String proFileName, byte[] password, String[] keys, byte[][] data ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException;

    /**
     * Read profile value.
     *
     * @param address     the address
     * @param proFileName the pro file name
     * @param keys        the keys
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void readProfileValue ( String address, String proFileName, String[] keys ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException;

    /**
     * Delete profile value.
     *
     * @param address     the address
     * @param proFileName the pro file name
     * @param password    the password
     * @param keys        the keys
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void deleteProfileValue ( String address, String proFileName, byte[] password, String[] keys ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException;

    /**
     * Gets profile info.
     *
     * @param address     the address
     * @param proFileName the pro file name
     * @throws ProtocolNotSupportedException the protocol not supported exception
     * @throws ProfileKeyValueLimitException the profile key value limit exception
     */
    public void getProfileInfo ( String address, String proFileName ) throws ProtocolNotSupportedException, ProfileKeyValueLimitException;

    /**
     * Is support pen profile boolean.
     *
     * @param address the address
     * @return the boolean
     */
    public boolean isSupportPenProfile ( String address );

    /**
     * Unpair device boolean.
     *
     * @param address the address
     * @return if success, return true
     */
    public boolean unpairDevice ( String address );


    /**
     * unknown :0  1,2,3...
     *
     * @return the pen color(type) code
     */
    public short getColorCode(String address)throws ProtocolNotSupportedException;
    /**
     * unknown :0  1,2,3...
     *
     * @return the pen product code
     */
    public short getProductCode(String address)throws ProtocolNotSupportedException;
    /**
     * unknown :0  1,2,3...
     *
     * @return the pen company code
     */
    public short getCompanyCode(String address)throws ProtocolNotSupportedException;

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
    public boolean isSupportHoverCommand(String address)throws ProtocolNotSupportedException;


    /**
     * Request System Information.
     * @throws ProtocolNotSupportedException
     */
    void reqSystemInfo(String address) throws ProtocolNotSupportedException;

    /**
     * Request setting performance step.
     *
     * @param step
     * @throws ProtocolNotSupportedException
     */
    void reqSetPerformance(String address, int step) throws ProtocolNotSupportedException;

}
