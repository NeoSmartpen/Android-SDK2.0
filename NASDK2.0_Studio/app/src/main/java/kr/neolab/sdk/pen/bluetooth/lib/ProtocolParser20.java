package kr.neolab.sdk.pen.bluetooth.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor20;
import kr.neolab.sdk.util.NLog;
import kr.neolab.sdk.util.UseNoteData;

import static kr.neolab.sdk.pen.bluetooth.comm.CommProcessor20.PEN_UP_DOWN_SEPARATE_SUPPORT_PROTOCOL_VERSION;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_CREATE;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_DELETE;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_DELETE_VALUE;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_INFO;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_READ_VALUE;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_WRITE_VALUE;

/**
 * The type Protocol parser 20.
 *
 * @author Moo
 */
public class ProtocolParser20
{
    /**
     * The constant PKT_RESULT_SUCCESS.
     */
    public static final int PKT_RESULT_SUCCESS = 0x00;

    /**
     * The constant PKT_RESULT_FAIL.
     */
    public static final int PKT_RESULT_FAIL = 0x01;
    /**
     * The constant PKT_RESULT_FAIL2.
     */
    public static final int PKT_RESULT_FAIL2 = 0x02;


    private static final int PKT_ESCAPE = 0x20;

    private static final int PKT_START = 0xC0;
    private static final int PKT_END = 0xC1;
    private static final int PKT_EMPTY = 0x00;
    /**
     * The constant PKT_DLE.
     */
    public static final int PKT_DLE = 0x7D;

    private static final int PKT_CMD_POS = 0;
    private static final int PKT_ERROR_POS = 1;
    private static final int PKT_LENGTH_POS1 = 2;
    private static final int PKT_LENGTH_POS2 = 3;
    private static final int PKT_MAX_LEN = 32 * 1024;

    private int counter = 0;
    private int dataLength = 0;
    private int headerLength = 0;

    // length
    private byte[] lbuffer = new byte[2];

    private static int buffer_size = PKT_MAX_LEN + 1;

    private ByteBuffer nbuffer = ByteBuffer.allocate( buffer_size );
    private ByteBuffer escapeBuffer = ByteBuffer.allocate( buffer_size );

//    private boolean isStart = true;
    private boolean isEvent = false;
    private boolean isDle = false;

    private IParsedPacketListener listener = null;

    /**
     * Instantiates a new Protocol parser 20.
     *
     * @param listener the listener
     */
    public ProtocolParser20 ( IParsedPacketListener listener )
    {
        this.listener = listener;
    }

    /**
     * Parse byte data.
     *
     * @param data the data
     * @param size the size
     */
    public void parseByteData ( byte data[], int size )
    {
        NLog.d( "[ProtocolParser20] parseByteData " + "Packet:" + PacketBuilder.showPacket( data, size ) );
        // StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < size; i++ )
        {
            // int int_data = (int) (data[i] & 0xFF);
            //
            // sb.append(Integer.toHexString(int_data));
            // sb.append(", ");
            //
            // if ( int_data == 0xC1 )
            // {
            // NLog.d("[CommProcessor] parseByteData : " + sb.toString());
            // sb = new StringBuffer();
            // }

            parseOneByteDataEscape( data[i]);
        }

        // NLog.d("[CommProcessor] parseByteData : " + sb.toString());
    }

    private void parseOneByteDataEscape ( byte data)
    {
        int int_data = (int) ( data & 0xFF );
        if ( int_data == PKT_START )
        {
//            NLog.d( "[ProtocolParser20] parseOneByteDataEscape PKT_START" );
            counter = 0;
            nbuffer.clear();
            return;
        }
        else if ( int_data == PKT_END )
        {
//            NLog.d( "[ProtocolParser20] parseOneByteDataEscape PKT_END" );
//            nbuffer.put( counter, data );
            byte[] temp = nbuffer.array();
            int size = counter;
            NLog.d( "parseOneByteDataEscape=size=" + size + "Packet:" + PacketBuilder.showPacket( temp , size));

            for(int i = 0; i < size; i++)
            {
                parseOneByte( temp[i],i, size);
//                    break;
            }
            temp = null;
            counter = 0;
            nbuffer.clear();
            return;
        }
        else if(int_data == PKT_DLE && !isDle )
        {
//            NLog.d( "[ProtocolParser20] parseOneByteDataEscape PKT_DLE" );
            isDle = true;
            return;
        }
        else
        {
            if(isDle)
            {
                data = escapeData(data);
                int t_data = (int) ( data & 0xFF );
            NLog.d( "[ProtocolParser20] parseOneByteDataEscape PKT_DLE = "+t_data );
                isDle = false;
            }
        }
        nbuffer.put( counter, data );
        counter++;
    }

    private byte escapeData ( byte source )
    {
        return (byte) ( source ^ PKT_ESCAPE );
    }


    private boolean parseOneByte ( byte data, int count, int end)
    {
        int int_data = (int) ( data & 0xFF );
//        NLog.d( "[ProtocolParser20] parseOneByte int_data="+int_data+",count="+count +"end="+end);


        if (count == 0)
        {
            NLog.d( "[ProtocolParser20] parseOneByte CMD" );
            counter = 0;
            headerLength = 0;
            escapeBuffer.clear();
        }
        if ( count == end  -1 )
        {
            escapeBuffer.put( counter, data );
            counter++;
            NLog.d( "[ProtocolParser20] parseOneByte PKT_END count=" + count + ", dataLength=" + dataLength + ", headerLength=" + headerLength +", isEvent="+isEvent+ ", Packet: " + PacketBuilder.showPacket( escapeBuffer.array(), counter ) );
            if(count == dataLength + headerLength - 1)
            {
                this.listener.onCreatePacket( new Packet( escapeBuffer.array(),2 ,isEvent) );

                dataLength = 0;
                counter = 0;
                escapeBuffer.clear();
            }
            return true;
        }
        else
        {
            if ( counter == PKT_CMD_POS )
            {
                if (CMD20.isEventCMD( data ))
                {
                    isEvent = true;
                }
                else
                    isEvent = false;
                headerLength++;

            }
            else
            {
                if ( isEvent )
                {
                    if ( counter == PKT_LENGTH_POS1 -1 )
                    {
                        lbuffer[0] = data;
                        headerLength++;

                    }
                    else if ( counter == PKT_LENGTH_POS2 - 1 )
                    {
                        lbuffer[1] = data;
                        dataLength = ByteConverter.byteArrayToShort( lbuffer );
                        headerLength++;

                    }

                }
                else
                {
                    if(counter == PKT_ERROR_POS )
                    {
                        headerLength++;
                        if(data != PKT_RESULT_SUCCESS)
                        {
                            escapeBuffer.put( counter, data );
                            this.listener.onCreatePacket( new Packet( escapeBuffer.array(), 2 ) );

                            dataLength = 0;
                            counter = 0;
                            headerLength = 0;
                            escapeBuffer.clear();
                            return true;
                        }
                    }
                    else if ( counter == PKT_LENGTH_POS1 )
                    {
                        lbuffer[0] = data;
                        headerLength++;
                    }
                    else if ( counter == PKT_LENGTH_POS2 )
                    {
                        lbuffer[1] = data;
                        dataLength = ByteConverter.byteArrayToShort( lbuffer );
                        headerLength++;
                    }
                }
            }

            escapeBuffer.put( counter, data );

            counter++;
        }
        return false;
    }

//    // 0x02 CMD20.P_PenOnResponse
//    public static byte[] buildPenOnOffData ( boolean status )
//    {
//        PacketBuilder builder = new PacketBuilder( 9 );
//
//        builder.setCommand( CMD20.P_PenOnResponse );
//
//        byte[] buffer = ByteConverter.longTobyte( System.currentTimeMillis() );
//
//        builder.write( buffer, 8 );
//
//        if ( status )
//        {
//            builder.write( (byte) 0x00 );
//        }
//        else
//        {
//            builder.write( (byte) 0x01 );
//        }
//
//        return builder.getPacket();
//    }

    /**
     * Build req pen info byte [ ].
     * 0x01 CMD20.REQ_PenInfo
     *
     * @param appVer    the app ver
     * @return the byte [ ]
     */
    public static byte[] buildReqPenInfo ( String appVer, short appType, String reqProtocolVer )
    {

        PacketBuilder builder = new PacketBuilder( 16 + 2 + 16 + 8);    //[2018.03.05] Stroke Test

        builder.setCommand( CMD20.REQ_PenInfo );

        builder.write( ByteConverter.stringTobyte( "" ), 16 );
        // type Android
        builder.write( ByteConverter.shortTobyte( appType ), 2 );
        // It may be empty depending on the situation (depending on whether Context is set).
        builder.write( ByteConverter.stringTobyte( appVer ), 16 );
        builder.write( ByteConverter.stringTobyte(reqProtocolVer), 8);   //[2018.03.05] Stroke Test
        NLog.d( "[ProtocolParser20] REQ  buildReqPenInfo. appVer=" + appVer + "Packet:" + builder.showPacket());
        return builder.getPacket();
    }

    /**
     * Build req wired pen info byte [ ].
     * 0x01 CMD20.REQ_PenInfo
     *
     * @param appVer    the app ver
     * @return the byte [ ]
     */
    public static byte[] buildReqWiredPenInfo ( String appVer )
    {

        PacketBuilder builder = new PacketBuilder( 16 + 2 + 16 + 8);    //[2018.03.05] Stroke Test

        builder.setCommand( CMD20.REQ_PenInfo );
        builder.write( ByteConverter.stringTobyte( "" ), 16 );
        // type Android
        builder.write( ByteConverter.shortTobyte( (short) 0x1101 ), 2 );
        // It may be empty depending on the situation (depending on whether Context is set).
        builder.write( ByteConverter.stringTobyte( appVer ), 16 );
        builder.write( ByteConverter.stringTobyte(PEN_UP_DOWN_SEPARATE_SUPPORT_PROTOCOL_VERSION), 8);   //[2018.03.05] Stroke Test
        NLog.d( "[ProtocolParser20] REQ  buildReqPenInfo. appVer=" + appVer + "Packet:" + builder.showPacket());
        return builder.getPacket();
    }
    /**
     * Build password input byte [ ].
     * 0x02 CMD20.REQ_Password
     *
     * @param password the password
     * @return the byte [ ]
     */
    public static byte[] buildPasswordInput ( String password )
    {

        PacketBuilder sendbyte = new PacketBuilder( 16 );
        sendbyte.setCommand( CMD20.REQ_Password );
        sendbyte.write( ByteConverter.stringTobyte( password ), 16 );
        NLog.d( "[ProtocolParser20] REQ  buildPasswordInput." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build password setup byte [ ].
     * 0x03 CMD20.REQ_PasswordSet
     *
     * @param isUse       the is use
     * @param oldPassword the old password
     * @param newPassword the new password
     * @return the byte [ ]
     */
    public static byte[] buildPasswordSetup(boolean isUse, String oldPassword, String newPassword )
    {
        PacketBuilder sendbyte = new PacketBuilder( 32 +1);
        sendbyte.setCommand( CMD20.REQ_PasswordSet );
        sendbyte.write( (byte) ( isUse ? 1 : 0 ) );
        sendbyte.write( ByteConverter.stringTobyte( oldPassword ), 16 );
        sendbyte.write( ByteConverter.stringTobyte( newPassword ), 16 );

        NLog.d( "[ProtocolParser20] REQ buildPasswordSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen status data byte [ ].
     * 0x04 CMD20.REQ_PenStatus
     *
     * @return the byte [ ]
     */
    public static byte[] buildPenStatusData ()
    {
        PacketBuilder builder = new PacketBuilder( 0 );
        builder.setCommand( CMD20.REQ_PenStatus );

        NLog.d( "[ProtocolParser20] REQ buildPenStatusData." + "Packet:" + builder.showPacket() );
        return builder.getPacket();
    }

    public static byte[] buildSetCameraRegister( ArrayList<byte[]> values) {
        PacketBuilder sendbyte = new PacketBuilder( 9 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_Camera_Register ), 1 );

        sendbyte.write( (byte)(values.size()) );

//        long ts = TimeUtil.convertLocalTimeToUTC( System.currentTimeMillis());
        for(byte[] value : values) {
            sendbyte.write(value[0]);
            sendbyte.write(value[1]);
        }
        NLog.d( "[ProtocolParser20] REQ buildSetCameraRegister." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }



    /**
     * Build set current time data byte [ ].
     * 0x04 CMD20.REQ_PenStatus  0x01 REQ_PenStatusChange_TYPE_CurrentTimeSet
     *
     * @return the byte [ ]
     */
    public static byte[] buildSetCurrentTimeData ()
    {
        PacketBuilder sendbyte = new PacketBuilder( 9 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_CurrentTimeSet ), 1 );

//        long ts = TimeUtil.convertLocalTimeToUTC( System.currentTimeMillis());
        sendbyte.write( ByteConverter.longTobyte( System.currentTimeMillis() ), 8 );
        NLog.d( "[ProtocolParser20] REQ buildSetCurrentTimeData." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build auto shutdown time setup byte [ ].
     * 0x04 CMD20.REQ_PenStatus  0x02 REQ_PenStatusChange_TYPE_AutoShutdownTime
     *
     * @param shutdownTime the shutdown time
     * @return the byte [ ]
     */
    public static byte[] buildAutoShutdownTimeSetup ( short shutdownTime )
    {
        PacketBuilder sendbyte = new PacketBuilder( 3 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_AutoShutdownTime ), 1 );
        sendbyte.write( ByteConverter.shortTobyte( shutdownTime ), 2 );
        NLog.d( "[ProtocolParser20] REQ buildAutoShutdownTimeSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen cap on off setup byte [ ].
     * 0x04 CMD20.REQ_PenStatus  0x03 REQ_PenStatusChange_TYPE_PenCapOnOff
     *
     * 2.15 update
     * NAP400 based model- pen cap off
     * MT2523 based model(NWP-F51) - pen cap on/off
     *
     * @param on the on
     * @return the byte [ ]
     */
    public static byte[] buildPenCapOnOffSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_PenCapOnOff ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );

        NLog.d( "[ProtocolParser20] REQ buildPenCapOnOffSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen auto power setup byte [ ].
     * 0x04 CMD20.REQ_PenStatus  0x04 REQ_PenStatusChange_TYPE_AutoPowerOnSet
     *
     * @param on the on
     * @return the byte [ ]
     */
    public static byte[] buildPenAutoPowerSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_AutoPowerOnSet ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );

        NLog.d( "[ProtocolParser20] REQ buildPenAutoPowerSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen beep setup byte [ ].
     * 0x04 CMD20.REQ_PenStatus  0x05 REQ_PenStatusChange_TYPE_BeepOnOff
     *
     * @param on the on
     * @return the byte [ ]
     */
    public static byte[] buildPenBeepSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_BeepOnOff ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );
        NLog.d( "[ProtocolParser20] REQ buildPenBeepSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen hover setup byte [ ].
     * 0x04 CMD20.REQ_PenStatus  0x06 REQ_PenStatusChange_TYPE_HoverOnOff
     *
     * @param on the on
     * @return the byte [ ]
     */
    public static byte[] buildPenHoverSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_HoverOnOff ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );
        NLog.d( "[ProtocolParser20] REQ buildPenHoverSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen offline data save setup byte [ ].
     * 0x04 CMD20.REQ_PenStatus  0x07 REQ_PenStatusChange_TYPE_OfflineDataSaveOnOff
     *
     * @param on the on
     * @return the byte [ ]
     */
    public static byte[] buildPenOfflineDataSaveSetup ( boolean on )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_OfflineDataSaveOnOff ), 1 );
        sendbyte.write( (byte) ( on ? 1 : 0 ) );
        NLog.d( "[ProtocolParser20] REQ buildPenOfflineDataSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen tip color setup byte [ ].
     * 0x04 CMD20.REQ_PenStatus  0x08 REQ_PenStatusChange_TYPE_LEDColorSet
     *
     * @param color the color
     * @return the byte [ ]
     */
    public static byte[] buildPenTipColorSetup ( int color )
    {
        byte[] cbyte = ByteConverter.intTobyte( color );


        PacketBuilder sendbyte = new PacketBuilder( 5 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_LEDColorSet ), 1 );
        //a
        sendbyte.write( cbyte[3] );
        //r
        sendbyte.write( cbyte[2] );
        //g
        sendbyte.write( cbyte[1] );
        //b
        sendbyte.write( cbyte[0] );

        NLog.d( "[ProtocolParser20] REQ buildPenTipColorSetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen sensitivity setup byte [ ].
     * 0x04 CMD20.REQ_PenStatus  0x09 REQ_PenStatusChange_TYPE_SensitivitySet
     *
     * @param sensitivity the sensitivity
     * @return the byte [ ]
     */
    public static byte[] buildPenSensitivitySetup ( short sensitivity )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_SensitivitySet ), 1 );
        sendbyte.write( ByteConverter.shortTobyte( sensitivity ), 1 );
        NLog.d( "[ProtocolParser20] REQ buildPenSensitivitySetup." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen sensitivity setup fsc byte [ ].
     * Use only FSC pressure sensor model.
     *
     * @param sensitivity the sensitivity
     * @return the byte [ ]
     */
    public static byte[] buildPenSensitivitySetupFSC ( short sensitivity )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_SensitivitySet_FSC ), 1 );
        sendbyte.write( ByteConverter.shortTobyte( sensitivity ), 1 );
        NLog.d( "[ProtocolParser20] REQ buildPenSensitivitySetupFSC." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen disk reset byte [ ].
     *
     * @return the byte [ ]
     */
    public static byte[] buildPenDiskReset ( )
    {
        PacketBuilder sendbyte = new PacketBuilder( 5 );
        sendbyte.setCommand( CMD20.REQ_PenStatusChange );
        sendbyte.write( ByteConverter.intTobyte( CMD20.REQ_PenStatusChange_TYPE_Disk_Reset ), 1 );
        sendbyte.write( ByteConverter.intTobyte( 0x4F1C0B42 ), 4 );
        NLog.d( "[ProtocolParser20] REQ buildPenSensitivitySetupFSC." + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }


//    public static final int USING_NOTE_TYPE_NOTE = 1;
//    public static final int USING_NOTE_TYPE_SECTION_OWNER = 2;
//    public static final int USING_NOTE_TYPE_ALL = 3;

    private static byte[] buildAddUsingNotes ( int sectionId, int ownerId, int[] noteIds )
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );

        PacketBuilder sendbyte = new PacketBuilder( 2 + noteIds.length *8 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) noteIds.length ));

        for ( int noteId : noteIds )
        {
            sendbyte.write( ownerByte[0] );
            sendbyte.write( ownerByte[1] );
            sendbyte.write( ownerByte[2] );
            sendbyte.write( (byte) sectionId );
            sendbyte.write( ByteConverter.intTobyte( noteId ) );
        }

        NLog.d( "[ProtocolParser20] REQ buildAddUsingNotes.sectionId+"+sectionId+";ownerId="+ownerId+";noteIds len="+noteIds.length+ "Packet:" + sendbyte.showPacket());
        return sendbyte.getPacket();
    }

    /**
     * Build add using notes byte [ ].
     *
     * @param noteList the note list
     * @return the byte [ ]
     */
    public static byte[] buildAddUsingNotes ( ArrayList<UseNoteData> noteList )
    {
        int setCount = 0;
        for(UseNoteData data : noteList)
        {
            if(data.ownerId != -1 && data.sectionId != -1 )
            {
                if(data.noteIds == null)
                    setCount++;
                else
                    setCount += data.noteIds.length;
            }
        }
        PacketBuilder sendbyte = new PacketBuilder( 2 + setCount *8 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) setCount ) );
        for(UseNoteData data : noteList)
        {
            if(data.ownerId != -1 && data.sectionId != -1 )
            {
                byte[] ownerByte = ByteConverter.intTobyte( data.ownerId );
                byte sectionIdByte = (byte)data.sectionId;
                if(data.noteIds == null)
                {
                    sendbyte.write( ownerByte[0] );
                    sendbyte.write( ownerByte[1] );
                    sendbyte.write( ownerByte[2] );
                    sendbyte.write( (byte) sectionIdByte );
                    sendbyte.write( ByteConverter.intTobyte( (int) 0xFFFFFFFF ) );
                }
                else
                {
                    for(int noteId: data.noteIds)
                    {
                        sendbyte.write( ownerByte[0] );
                        sendbyte.write( ownerByte[1] );
                        sendbyte.write( ownerByte[2] );
                        sendbyte.write( (byte) sectionIdByte );
                        sendbyte.write( ByteConverter.intTobyte( noteId ) );
                    }
                }
            }
        }
        NLog.d( "[ProtocolParser20] REQ buildAddUsingNotes ( ArrayList<UseNoteData> noteList )+" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build add using notes byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @return the byte [ ]
     */
    public static byte[] buildAddUsingNotes ( int sectionId, int ownerId )
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );

        PacketBuilder sendbyte = new PacketBuilder( 2 + 4 + 4 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) 1 ) );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( (int) 0xFFFFFFFF ) );

        NLog.d( "[ProtocolParser20] REQ buildAddUsingNotes.sectionId+" + sectionId + ";ownerId=" + ownerId + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build add using notes byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @return the byte [ ]
     */
    public static byte[] buildAddUsingNotes ( int[] sectionId, int[] ownerId )
    {
        PacketBuilder sendbyte = new PacketBuilder( 2 +  2 + sectionId.length *8 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) sectionId.length ) );
        for(int i = 0; i < sectionId.length; i++)
        {
            byte[] ownerByte = ByteConverter.intTobyte( ownerId[i] );
            sendbyte.write( ownerByte[0] );
            sendbyte.write( ownerByte[1] );
            sendbyte.write( ownerByte[2] );
            sendbyte.write( (byte) (sectionId[i]) );
            sendbyte.write( ByteConverter.intTobyte( (int) 0xFFFFFFFF ) );
            NLog.d( "[ProtocolParser20] REQ buildAddUsingNotes.sectionId+" + sectionId[i] + ";ownerId=" + ownerId[i] + "Packet:" + sendbyte.showPacket() );
        }

        return sendbyte.getPacket();
    }

    /**
     * Build add using all notes byte [ ].
     *
     * @return the byte [ ]
     */
    public static byte[] buildAddUsingAllNotes ()
    {
       PacketBuilder sendbyte = new PacketBuilder( 2 );
        sendbyte.setCommand( CMD20.REQ_UsingNoteNotify );
        sendbyte.write( ByteConverter.shortTobyte( (short) 0xffff ) );
        NLog.d( "[ProtocolParser20] REQ buildAddUsingAllNotes" + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data list all byte [ ].
     *
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineDataListAll ()
    {
        PacketBuilder sendbyte = new PacketBuilder( 4 );
        sendbyte.setCommand( CMD20.REQ_OfflineNoteList );
        sendbyte.write( ByteConverter.intTobyte( (int) 0xFFFFFFFF ) );

        NLog.d( "[ProtocolParser20] REQ buildReqOfflineDataListAll" + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data list byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineDataList (int sectionId, int ownerId)
    {
        PacketBuilder sendbyte = new PacketBuilder( 4 );
        sendbyte.setCommand( CMD20.REQ_OfflineNoteList );
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );

        NLog.d( "[ProtocolParser20] REQ buildReqOfflineDataList sectionId=" + sectionId + ";ownerId=" + ownerId + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data page list byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineDataPageList (int sectionId, int ownerId, int noteId)
    {
        PacketBuilder sendbyte = new PacketBuilder( 8 );
        sendbyte.setCommand( CMD20.REQ_OfflinePageList );
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( noteId ) );
        NLog.d( "[ProtocolParser20] REQ buildReqOfflineDataPageList sectionId=" + sectionId + ";ownerId=" + ownerId + ";noteId=" + noteId + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param deleteOnFinished  delete offline data when transmission is finished
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineData ( int sectionId, int ownerId, int noteId, boolean deleteOnFinished )
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );

        PacketBuilder sendbyte = new PacketBuilder( 14 );
        sendbyte.setCommand( CMD20.REQ_OfflineDataRequest );

        // isOffline data remove after transfer
        // 0: not send req2
        // 1: send req2(after res1), remove offline data
        // 2: send req2(after res1), not remove offline data
        sendbyte.write( (byte) (deleteOnFinished ? 1 : 2) );

        // isCompress  1:compress(recommend)  0:uncompress
        sendbyte.write( (byte) 1 );

        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( noteId ) );
        //if pages count is  0 , all Note
        sendbyte.write( ByteConverter.intTobyte( 0 ) );

        NLog.d( "[ProtocolParser20] REQ buildReqOfflineData sectionId=" + sectionId + ";ownerId=" + ownerId + ";noteId=" + noteId + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build req offline data byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @param pageIds   the page ids
     * @param deleteOnFinished  delete offline data when transmission is finished
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineData ( int sectionId, int ownerId, int noteId, boolean deleteOnFinished, int[] pageIds )
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );
        int pageCount = 0;
        if(pageIds != null)
        {
            pageCount = pageIds.length;
        }

        PacketBuilder sendbyte = new PacketBuilder( 14 + pageCount * 4);
        sendbyte.setCommand( CMD20.REQ_OfflineDataRequest ); // 펜에게 호출

        // isOffline data remove after transfer
        // 0: not send req2
        // 1: send req2(after res1), remove offline data
        // 2: send req2(after res1), not remove offline data
        sendbyte.write( (byte) (deleteOnFinished ? 1 : 2) );

        // isCompress  1:compress  0:uncompress
        sendbyte.write( (byte) 1 );

        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( noteId ) );

        //if pages count is  0 , all Note
        sendbyte.write( ByteConverter.intTobyte( pageCount ) );
        if(pageCount != 0)
        {
            for(int pageId: pageIds)
                sendbyte.write( ByteConverter.intTobyte( pageId ) );
        }
        NLog.d( "[ProtocolParser20] REQ buildReqOfflineData sectionId=" + sectionId + ";ownerId=" + ownerId + ";noteId=" + noteId + ";deleteOnFinished=" + deleteOnFinished + ";pageIds=" + Arrays.toString(pageIds) + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build offline chunk response byte [ ].
     * 0x44 CMD20.P_OfflineChunkResponse
     *
     * @param errorCode the error code
     * @param packetId  the packet id
     * @param position  the position
     * @return the byte [ ]
     */
    public static byte[] buildOfflineChunkResponse ( int errorCode, int packetId, int position )
    {
        PacketBuilder builder = new PacketBuilder( 3,  errorCode);
        builder.setCommand( CMD20.ACK_OfflineChunk );
        builder.write( ByteConverter.intTobyte( packetId ), 2 );
        int isContinue = 0;
        if(position == 2)
            isContinue = 0;
        else
            isContinue = 1;
        builder.write( ByteConverter.intTobyte( isContinue ),1);

        NLog.d( "[ProtocolParser20] REQ buildOfflineChunkResponse :" + builder.showPacket() );
        return builder.getPacket();
    }

    /**
     * Build req offline data remove byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteIds   the note ids
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineDataRemove ( int sectionId, int ownerId, int[] noteIds)
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );
        int noteCount = 0;
        if(noteIds != null)
        {
            noteCount = noteIds.length;
        }

        PacketBuilder sendbyte = new PacketBuilder( 5 + noteCount *4 );
        sendbyte.setCommand( CMD20.REQ_OfflineNoteRemove );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( (byte) noteCount );
        if(noteCount != 0)
        {
            for(int noteId: noteIds)
                sendbyte.write( ByteConverter.intTobyte( noteId ) );
        }
        NLog.d( "[ProtocolParser20] REQ buildReqOfflineDataRemove :" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }


    /**
     * Build req offline data Page remove byte [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId   the note ids
     * @param pageIds   the pages ids
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineDataRemoveByPage ( int sectionId, int ownerId, int noteId, int[] pageIds)
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );
        int pageCount = 0;
        if(pageIds != null)
        {
            pageCount = pageIds.length;
        }

        PacketBuilder sendbyte = new PacketBuilder( 5+4 + pageCount *4 );
        sendbyte.setCommand( CMD20.REQ_OfflinePageRemove );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( noteId ) );
        sendbyte.write( (byte) pageCount );
        if(pageCount != 0)
        {
            for(int pageId: pageIds)
                sendbyte.write( ByteConverter.intTobyte( pageId ) );
        }
        NLog.d( "[ProtocolParser20] REQ buildReqOfflineDataRemoveByPage :" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }


    /**
     * Build req offline note Info byte [ ].
     *
     * v2.16
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId   the note id
     * @return the byte [ ]
     */
    public static byte[] buildReqOfflineNoteInfo ( int sectionId, int ownerId, int noteId)
    {
        byte[] ownerByte = ByteConverter.intTobyte( ownerId );

        PacketBuilder sendbyte = new PacketBuilder( 4 + 4 );
        sendbyte.setCommand( CMD20.REQ_OfflineNoteInfo );
        sendbyte.write( ownerByte[0] );
        sendbyte.write( ownerByte[1] );
        sendbyte.write( ownerByte[2] );
        sendbyte.write( (byte) sectionId );
        sendbyte.write( ByteConverter.intTobyte( noteId ) );
        NLog.d( "[ProtocolParser20] REQ buildReqOfflineNoteInfo :" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    /**
     * Build pen sw upgrade byte [ ].
     * 0x31 CMD20.REQ_PenFWUpgrade
     *
     * @param fwVersion  the fw version
     * @param deviceName the device name
     * @param filesize   the filesize
     * @param checkSum   the check sum
     * @param isCompress the is compress
     * @param packetSize the packet size
     * @return the byte [ ]
     */
    public static byte[] buildPenSwUpgrade ( String fwVersion, String deviceName, int filesize, byte checkSum, boolean isCompress, int packetSize)
    {
        PacketBuilder builder = new PacketBuilder( 16 + 16 + 4 + 4 + 1 + 1 );

        builder.setCommand( CMD20.REQ_PenFWUpgrade );
        builder.write( ByteConverter.stringTobyte( deviceName ), 16 );
        builder.write( ByteConverter.stringTobyte( fwVersion ),16 );
        builder.write( ByteConverter.intTobyte( filesize ) );
        builder.write( ByteConverter.intTobyte( packetSize ) );


        // 1:Compress , 0: Uncompress
        if(isCompress)
        {
            builder.write( (byte) 1 );
        }
        else
            builder.write( (byte) 0 );
        builder.write( checkSum );
        NLog.d( "[ProtocolParser20] REQ buildPenSwUpgrade deviceName="+deviceName+":" + builder.showPacket() );
        return builder.getPacket();
    }

    /**
     * Build pen sw upload chunk byte [ ].
     * 0xB2 ACK_UploadPenFWChunk
     *
     * @param offset     the offset
     * @param data       the data
     * @param status     the status
     * @param isCompress the is compress
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    public static byte[] buildPenSwUploadChunk( int offset, byte[] data ,int status, boolean isCompress) throws IOException
    {
//        If status is error, only error code is sent.
        if(status == CommProcessor20.FwPacketInfo.STATUS_ERROR)
        {
            PacketBuilder sendbyte = new PacketBuilder(0, 1 );
            sendbyte.setCommand( CMD20.ACK_UploadPenFWChunk );
            NLog.d( "[ProtocolParser20] REQ buildPenSwUploadChunk ERR :" + sendbyte.showPacket());
            return sendbyte.getPacket();

        }
        else
        {
            int beforeCompressSize = data.length;
            byte[] compressData = null;
            int afterCompressSize = 0;
            if(isCompress)
            {
                compressData = compress( data );
                afterCompressSize = compressData.length;
            }
            else
            {
                compressData = data;
                afterCompressSize = 0;
            }


            PacketBuilder sendbyte = new PacketBuilder( 1 + 4 + 1 + 4 + 4 + compressData.length, 0 );
            sendbyte.setCommand( CMD20.ACK_UploadPenFWChunk );
            sendbyte.write( (byte) 0 );
            sendbyte.write( ByteConverter.intTobyte( offset ) );
            sendbyte.write( Chunk.calcChecksum( data ) );
            sendbyte.write( ByteConverter.intTobyte( beforeCompressSize ) );
            sendbyte.write( ByteConverter.intTobyte(  afterCompressSize ) );
            sendbyte.write( compressData );
            NLog.d( "[ProtocolParser20] REQ buildPenSwUploadChunk beforeCompressSize:"+beforeCompressSize+",afterCompressSize:"+afterCompressSize+"," + sendbyte.showPacket() );
            return sendbyte.getPacket();

        }
    }

    /////////////////////////   Profile   ///////////////////////////////////

    /**
     * Build profile create byte [ ].
     *
     * @param filename the filename
     * @param pass     the pass
     * @return the byte [ ]
     */
    public static byte[] buildProfileCreate( String filename, byte[] pass )
    {
        PacketBuilder sendbyte = new PacketBuilder( 8+1+8+2+2 );
        sendbyte.setCommand( CMD20.REQ_PenProfile );
        // Profile filename
        sendbyte.write(ByteConverter.stringTobyte( filename ),8 );
        // request type
        sendbyte.write(PROFILE_CREATE );
        // Profile password
        sendbyte.write(pass,8 );

        // sector size
        sendbyte.write( ByteConverter.shortTobyte( (short)32 ),2 );
        // sector count(2^N Currently fixed 2^8)
        sendbyte.write( ByteConverter.shortTobyte( (short)128 ),2 );
        NLog.d( "[ProtocolParser20] REQ  buildProfileCreate.showPacket"+sendbyte.showPacket());
        sendbyte.showPacket();
        return sendbyte.getPacket();
    }

    /**
     * Build profile delete byte [ ].
     *
     * @param filename the filename
     * @param pass     the pass
     * @return the byte [ ]
     */
    public static byte[] buildProfileDelete( String filename, byte[] pass )
    {
        PacketBuilder sendbyte = new PacketBuilder( 8+1+8 );
        sendbyte.setCommand( CMD20.REQ_PenProfile );
        // Profile filename
        sendbyte.write(ByteConverter.stringTobyte( filename ),8 );
        // request type
        sendbyte.write(PROFILE_DELETE );
        // Profile password
        sendbyte.write(pass,8 );
        NLog.d( "[ProtocolParser20] REQ  buildProfileDelete.showPacket"+sendbyte.showPacket());
        sendbyte.showPacket();
        return sendbyte.getPacket();
    }

    /**
     * Build profile info byte [ ].
     *
     * @param filename the filename
     * @return the byte [ ]
     */
    public static byte[] buildProfileInfo( String filename )
    {
        PacketBuilder sendbyte = new PacketBuilder( 8+1);
        sendbyte.setCommand( CMD20.REQ_PenProfile );
        // Profile filename
        sendbyte.write(ByteConverter.stringTobyte( filename ),8 );
        // request type
        sendbyte.write(PROFILE_INFO );
        NLog.d( "[ProtocolParser20] REQ  buildProfileInfo. showPacket"+sendbyte.showPacket());
        sendbyte.showPacket();
        return sendbyte.getPacket();
    }

    /**
     * Build profile read value byte [ ].
     *
     * @param filename the filename
     * @param keys     the keys
     * @return the byte [ ]
     */
    public static byte[] buildProfileReadValue(String filename, String[] keys)
    {
        PacketBuilder sendbyte = new PacketBuilder( 8+1+1+16*keys.length );
        sendbyte.setCommand( CMD20.REQ_PenProfile );
        // Profile filename
        sendbyte.write(ByteConverter.stringTobyte( filename ),8 );
        // request type
        sendbyte.write(PROFILE_READ_VALUE );
        // key count
        sendbyte.write((byte)keys.length );
        for(int i = 0; i < keys.length; i++)
        {
            // key
            sendbyte.write(ByteConverter.stringTobyte( keys[i] ),16 );
        }
        NLog.d( "[ProtocolParser20] REQ  buildProfileReadValue.showPacket"+sendbyte.showPacket());
        sendbyte.showPacket();
        return sendbyte.getPacket();
    }

    /**
     * Build profile write value byte [ ].
     *
     * @param filename the filename
     * @param pass     the pass
     * @param key      the key
     * @param data     the data
     * @return the byte [ ]
     */
    public static byte[] buildProfileWriteValue(String filename, byte[] pass, String[] key, byte[][] data)
    {
        int data_len = 0;
        for(int i = 0; i < data.length; i++)
        {
            data_len += 16;
            data_len += 2;
            data_len += data[i].length;
        }

        PacketBuilder sendbyte = new PacketBuilder( 8+1+8+1+ data_len);
        sendbyte.setCommand( CMD20.REQ_PenProfile );
        // Profile filename
        sendbyte.write(ByteConverter.stringTobyte( filename ),8 );
        // request type
        sendbyte.write(PROFILE_WRITE_VALUE );
        // Profile password
        sendbyte.write(pass,8 );
        // key count
        sendbyte.write((byte)data.length );

        for(int i = 0; i < data.length; i++)
        {
            // key
            sendbyte.write(ByteConverter.stringTobyte( key[i] ),16 );
            sendbyte.write(ByteConverter.shortTobyte( (short) data[i].length ),2 );
            sendbyte.write(data[i] );
        }

        NLog.d( "[ProtocolParser20] REQ  buildProfileWriteValue.showPacket"+sendbyte.showPacket());
        sendbyte.showPacket();
        return sendbyte.getPacket();
    }

    /**
     * Build profile delete value byte [ ].
     *
     * @param filename the filename
     * @param pass     the pass
     * @param keys     the keys
     * @return the byte [ ]
     */
    public static byte[] buildProfileDeleteValue( String filename, byte[] pass, String[] keys)
    {
        PacketBuilder sendbyte = new PacketBuilder( 8+1+8+1+16*keys.length );
        sendbyte.setCommand( CMD20.REQ_PenProfile );
        // Profile filename
        sendbyte.write(ByteConverter.stringTobyte( filename ),8 );
        // request type
        sendbyte.write(PROFILE_DELETE_VALUE );
        // Profile password
        sendbyte.write(pass,8 );
        // key count
        sendbyte.write((byte)keys.length );
        for(int i = 0; i < keys.length; i++)
        {
            // key
            sendbyte.write(ByteConverter.stringTobyte( keys[i] ),16 );
        }
        NLog.d( "[ProtocolParser20] REQ  buildProfileDeleteValue.showPacket"+sendbyte.showPacket());
        sendbyte.showPacket();
        return sendbyte.getPacket();
    }

	public static byte[] buildReqSystemInfo()
    {
        PacketBuilder sendbyte = new PacketBuilder( 0 );
        sendbyte.setCommand( CMD20.REQ_SystemInfo);
        NLog.d( "[ProtocolParser20] REQ buildReqSystemInfo" + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    public static byte[] buildReqSetPerformance(int step)
    {
        PacketBuilder sendbyte = new PacketBuilder( 1+4+4 );
        sendbyte.setCommand( CMD20.REQ_SetPerformance);
        sendbyte.write( (byte) 1 ); //type = set performance(=eco mode) = 1

        sendbyte.write( ByteConverter.intTobyte(0) );   //Reserved 4byte

        // 0 :  104 MHz, 43 frame
        // 1 :  208 MHz, 86 frame
        sendbyte.write( ByteConverter.intTobyte(step) );

        NLog.d( "[ProtocolParser20] REQ buildReqSetPerformance" + "Packet:" + sendbyte.showPacket() );
        return sendbyte.getPacket();
    }

    static private byte[] compress(byte[] source) throws IOException
    {
//        ByteArrayInputStream in = new ByteArrayInputStream(source);
//        ByteArrayOutputStream bous = new ByteArrayOutputStream();
//        DeflaterOutputStream out =	new DeflaterOutputStream(bous);
//        byte[] buffer = new byte[1024];
//        int len;
//        while((len = in.read(buffer)) > 0) {
//            out.write(buffer, 0, len);
//        }
//        byte[] ret = bous.toByteArray();
//
//        bous.flush();
//        in.close();
//        out.close();
//        return ret;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFLATED);
        DeflaterOutputStream deflaterStream = new DeflaterOutputStream(baos, deflater);
        deflaterStream.write(source);
        deflaterStream.finish();

        return baos.toByteArray();
    }

    /**
     * The interface Parsed packet listener.
     */
    public interface IParsedPacketListener
    {
        /**
         * On create packet.
         *
         * @param packet the packet
         */
        public void onCreatePacket ( Packet packet );
    }

    /**
     * The type Packet builder.
     */
    public static class PacketBuilder
    {
        private ByteBuffer escapeBuffer = ByteBuffer.allocate( buffer_size );
        private int afterEscapeSize = 0;
        /**
         * The Packet.
         */
        byte[] packet;
        /**
         * The Ret packet.
         */
        byte[] retPacket;
        /**
         * The Total length.
         */
        int totalLength, /**
     * The Data length.
     */
    dataLength;
        /**
         * The Position.
         */
        int position = 4;
        /**
         * The Is escape.
         */
        boolean isEscape = false;

        /**
         * Instantiates a new Packet builder.
         *
         * @param length the length
         */
        public PacketBuilder ( int length )
        {
            allocate( length );
        }

        /**
         * Instantiates a new Packet builder.
         *
         * @param length    the length
         * @param errorCode the error code
         */
        public PacketBuilder ( int length, int errorCode )
        {
            allocate( length , errorCode);
        }

        /**
         * write command field
         *
         * @param cmd the cmd
         */
        public void setCommand ( int cmd )
        {
            packet[1] = (byte) cmd;
        }

        /**
         * buffer allocation and set packet frame
         *
         * @param length the length
         */
        public void allocate ( int length  )
        {
            totalLength = length + 5;

            dataLength = length;

            position = 4;

            packet = new byte[this.totalLength];

            Arrays.fill( packet, (byte) PKT_EMPTY );

            packet[0] = (byte) PKT_START;
            byte[] bLength = ByteConverter.shortTobyte( (short) length );

            packet[2] = bLength[0];
            packet[3] = bLength[1];

            packet[totalLength - 1] = (byte) PKT_END;
        }

        /**
         * buffer allocation and set packet frame
         *
         * @param length    the length
         * @param errorCode the error code
         */
        public void allocate ( int length, int errorCode  )
        {
            totalLength = length + 6;

            dataLength = length;

            position = 5;

            packet = new byte[this.totalLength];

            Arrays.fill( packet, (byte) PKT_EMPTY );

            packet[0] = (byte) PKT_START;
            byte[] bLength = ByteConverter.shortTobyte( (short) length );

            packet[2] = (byte) errorCode;
            packet[3] = bLength[0];
            packet[4] = bLength[1];

            packet[totalLength - 1] = (byte) PKT_END;
        }

        /**
         * write data to data field
         *
         * @param buffer the buffer
         */
        public void write ( byte[] buffer )
        {
            for ( int i = 0; i < buffer.length; i++ )
            {
                packet[position++] = buffer[i];
            }
        }

        /**
         * write data to data field (resize)
         *
         * @param buffer     the buffer
         * @param valid_size the valid size
         */
        public void write ( byte[] buffer, int valid_size )
        {
            buffer = ResizeByteArray( buffer, valid_size );
            this.write( buffer );
        }

        /**
         * write single data to data field
         *
         * @param data the data
         */
        public void write ( byte data )
        {
            packet[position++] = data;
        }

        /**
         * Resize byte array byte [ ].
         *
         * @param bytes   the bytes
         * @param newsize the newsize
         * @return the byte [ ]
         */
        public byte[] ResizeByteArray ( byte[] bytes, int newsize )
        {
            byte[] result = new byte[newsize];
            Arrays.fill( result, (byte) 0x00 );

            int length = newsize > bytes.length ? bytes.length : newsize;

            for ( int i = 0; i < length; i++ )
            {
                result[i] = bytes[i];
            }

            return result;
        }

        /**
         * Get packet byte [ ].
         *
         * @return the byte [ ]
         */
        public byte[] getPacket ()
        {
            if(!isEscape)
            {
                escapeBuffer.clear();
                afterEscapeSize = 0;
                for(int i = 0; i <  packet.length ; i++)
                {
                    boolean escape = false;
                    if(i != 0 && i != packet.length -1)
                    {
                        if(packet[i] == (byte)PKT_START || packet[i] == (byte)PKT_DLE || packet[i] == (byte)PKT_END)
                        {
                            escapeBuffer.put( (byte)PKT_DLE);
                            afterEscapeSize++;
                            escape = true;
                        }
                    }
                    if(escape)
                        escapeBuffer.put( escapeData( packet[i]));
                    else
                        escapeBuffer.put( packet[i]);
                    afterEscapeSize++;
                }
                retPacket = Packet.copyOfRange(escapeBuffer.array(), 0, afterEscapeSize);
                isEscape = true;
            }
            return retPacket;
        }
        private byte escapeData ( byte source )
        {
            return (byte) ( source ^ PKT_ESCAPE );
        }

        /**
         * Show packet string.
         *
         * @return the string
         */
        public String showPacket ()
        {
            if(!isEscape)
            {
                escapeBuffer.clear();
                afterEscapeSize = 0;

                for(int i = 0; i <  packet.length ; i++)
                {
                    boolean escape = false;
                    if(i != 0 && i != packet.length -1)
                    {
                        if(packet[i] == (byte)PKT_START || packet[i] == (byte)PKT_DLE || packet[i] == (byte)PKT_END)
                        {
                            escapeBuffer.put( (byte)PKT_DLE);
                            afterEscapeSize++;
                            escape = true;
                        }
                    }
                    if(escape)
                        escapeBuffer.put( escapeData( packet[i]));
                    else
                        escapeBuffer.put( packet[i]);
                    afterEscapeSize++;
                }
                retPacket = Packet.copyOfRange(escapeBuffer.array(), 0, afterEscapeSize);
            }
            StringBuffer buff = new StringBuffer();

            for ( byte item : retPacket )
            {
                int int_data = (int) ( item & 0xFF );
                buff.append( Integer.toHexString( int_data ) + ", " );
            }

//            NLog.d( "[PacketBuilder] showPacket : " + buff.toString() );

            String ret = buff.toString();
            buff = null;
            return ret;
        }

        /**
         * Show packet string.
         *
         * @param bytes the bytes
         * @param count the count
         * @return the string
         */
        public static String showPacket ( byte[] bytes , int count)
        {
            StringBuffer buff = new StringBuffer();

            for ( int i = 0; i < count; i++ )
            {
                byte item = bytes[i];
                int int_data = (int) ( item & 0xFF );
                buff.append( Integer.toHexString( int_data ) + ", " );
            }

//            NLog.d( "[PacketBuilder] showPacket : " + buff.toString() );

            String ret = buff.toString();
            buff = null;
            return ret;
        }
    }
}
