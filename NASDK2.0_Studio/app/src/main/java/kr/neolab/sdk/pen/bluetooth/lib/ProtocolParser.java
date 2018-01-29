package kr.neolab.sdk.pen.bluetooth.lib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.TimeZone;

import kr.neolab.sdk.util.NLog;

import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_CREATE;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_DELETE;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_DELETE_VALUE;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_INFO;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_READ_VALUE;
import static kr.neolab.sdk.pen.bluetooth.lib.PenProfile.PROFILE_WRITE_VALUE;

/**
 * The type Protocol parser.
 *
 * @author CHY
 */
public class ProtocolParser
{
	private static final int PKT_START = 0xC0;
	private static final int PKT_END = 0xC1;
	private static final int PKT_EMPTY = 0x00;
	private static final int PKT_HEADER_LEN = 3;
	private static final int PKT_LENGTH_POS1 = 1;
	private static final int PKT_LENGTH_POS2 = 2;
	private static final int PKT_MAX_LEN = 8200;

	private int counter = 0;
	private int dataLength = 0;

	// length
	private byte[] lbuffer = new byte[2];

	private static int buffer_size = PKT_MAX_LEN + 1;

	private ByteBuffer nbuffer = ByteBuffer.allocate( buffer_size );

	private boolean isStart = true;

	private IParsedPacketListener listener = null;
	private static final String DEFAULT_PASSWORD = "0000";

	/**
	 * Instantiates a new Protocol parser.
	 *
	 * @param listener the listener
	 */
	public ProtocolParser( IParsedPacketListener listener )
	{
		this.listener = listener;
	}

	/**
	 * Parse byte data.
	 *
	 * @param data the data
	 * @param size the size
	 */
	public void parseByteData( byte data[], int size )
	{
		// StringBuffer sb = new StringBuffer();

		NLog.d( "parseOneByte Start : " );
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

			parseOneByte( data[i], listener );
		}
		NLog.d( "parseOneByte End : " );

		// NLog.d("[CommProcessor] parseByteData : " + sb.toString());
	}

	private void parseOneByte( byte data, IParsedPacketListener listener )
	{
		int int_data = (int) (data & 0xFF);

		if ( int_data == PKT_START && isStart )
		{
			NLog.d( "parseOneByte PKT_START");
			counter = 0;
			isStart = false;
		}
		else if ( int_data == PKT_END && counter == dataLength + PKT_HEADER_LEN )
		{
			NLog.d( "parseOneByte PKT_END");
			this.listener.onCreatePacket( new Packet( nbuffer.array()) );

			dataLength = 0;
			counter = 10;
			nbuffer.clear();

			isStart = true;
		}
		else if ( counter > PKT_MAX_LEN )
		{
			NLog.d( "parseOneByte : counter > PKT_MAX_LEN" );
			counter = 10;
			dataLength = 0;

			isStart = true;
		}
		else
		{
			if ( counter == PKT_LENGTH_POS1 )
			{
				lbuffer[0] = data;
			}
			else if ( counter == PKT_LENGTH_POS2 )
			{
				lbuffer[1] = data;
				dataLength = ByteConverter.byteArrayToInt( lbuffer );
			}

			nbuffer.put( counter, data );

			counter++;
		}
	}

	/**
	 * Build pen on off data byte [ ].
	 * 0x02 CMD.P_PenOnResponse
	 *
	 * @param status the status
	 * @return the byte [ ]
	 */
	public static byte[] buildPenOnOffData( boolean status )
	{
		PacketBuilder builder = new PacketBuilder( 9 );

		builder.setCommand( CMD.P_PenOnResponse );

		byte[] buffer = ByteConverter.longTobyte( System.currentTimeMillis() );

		builder.write( buffer, 8 );

		if ( status )
		{
			builder.write( (byte) 0x00 );
		}
		else
		{
			builder.write( (byte) 0x01 );
		}

		return builder.getPacket();
	}

	/**
	 * Gets local time offset.
	 *
	 * @param timetick the timetick
	 * @return the local time offset
	 */
	public static int getLocalTimeOffset( long timetick )
	{
		return TimeZone.getDefault().getOffset( timetick );
	}

	/**
	 * Build set current time data byte [ ].
	 * 0x03 CMD.P_RTCset
	 *
	 * @return the byte [ ]
	 */
	public static byte[] buildSetCurrentTimeData()
	{
		PacketBuilder builder = new PacketBuilder( 12 );

		builder.setCommand( CMD.P_RTCset );

		long ts = System.currentTimeMillis();
		int offset = getLocalTimeOffset( ts );

		builder.write( ByteConverter.longTobyte( ts ), 8 );
		builder.write( ByteConverter.intTobyte( offset ), 4 );
		NLog.d( "[ProtocolParser] REQ  buildSetCurrentTimeData.");
		builder.showPacket();

		return builder.getPacket();
	}

	/**
	 * Build force calibrate data byte [ ].
	 * <p>
	 * 0x07 CMD.P_ForceCalibrate
	 *
	 * @return the byte [ ]
	 */
	public static byte[] buildForceCalibrateData()
	{
		PacketBuilder builder = new PacketBuilder( 0 );
		builder.setCommand( CMD.P_ForceCalibrate );
		NLog.d( "[ProtocolParser] REQ  buildForceCalibrateData.");
		builder.showPacket();

		return builder.getPacket();
	}

	/**
	 * Build pen echo response byte [ ].
	 * 0x0A CMD.P_EchoResponse
	 *
	 * @param echar the echar
	 * @return the byte [ ]
	 */
	public static byte[] buildPenEchoResponse( byte echar )
	{
		PacketBuilder builder = new PacketBuilder( 1 );
		builder.setCommand( CMD.P_EchoResponse );
		builder.write( echar );

		return builder.getPacket();
	}

	/**
	 * Build pen up respnse byte [ ].
	 *
	 * @param ts the ts
	 * @return the byte [ ]
	 */
	public static byte[] buildPenUpRespnse( long ts )
	{
		PacketBuilder builder = new PacketBuilder( 8 );
		builder.setCommand( CMD.P_DotUpDownResponse );
		builder.write( ByteConverter.longTobyte( ts ), 8 );

		return builder.getPacket();
	}

	/**
	 * Build pen status data byte [ ].
	 * 0x21 CMD.P_PenStatusRequest
	 *
	 * @return the byte [ ]
	 */
	public static byte[] buildPenStatusData()
	{
		PacketBuilder builder = new PacketBuilder( 0 );
		builder.setCommand( CMD.P_PenStatusRequest );

		NLog.d( "[ProtocolParser] REQ  buildPenStatusData.");
		builder.showPacket();
		return builder.getPacket();
	}

	/**
	 * Build offline info response byte [ ].
	 * 0x42 CMD.P_OfflineInfoResponse
	 *
	 * @param result the result
	 * @return the byte [ ]
	 */
	public static byte[] buildOfflineInfoResponse( boolean result )
	{
		PacketBuilder builder = new PacketBuilder( 2 );
		builder.setCommand( CMD.P_OfflineInfoResponse );
		builder.write( (byte) (result ? 0x01 : 0x00) );
		builder.write( (byte) (0x00) );
		NLog.d( "[ProtocolParser] REQ  buildOfflineInfoResponse.");
		builder.showPacket();
		return builder.getPacket();
	}

	/**
	 * Build offline chunk response byte [ ].
	 * 0x44 CMD.P_OfflineChunkResponse
	 *
	 * @param index the index
	 * @return the byte [ ]
	 */
	public static byte[] buildOfflineChunkResponse( int index )
	{
		PacketBuilder builder = new PacketBuilder( 2 );
		builder.setCommand( CMD.P_OfflineChunkResponse );
		builder.write( ByteConverter.shortTobyte( (short) index ) );

		NLog.d( "[ProtocolParser] REQ  buildOfflineChunkResponse.");
		builder.showPacket();
		return builder.getPacket();
	}

	/**
	 * Build pen sw upgrade byte [ ].
	 * 0x51 CMD.P_PenSWUpgradeCommand
	 *
	 * @param filename    the filename
	 * @param filesize    the filesize
	 * @param chunk_count the chunk count
	 * @param chunk_size  the chunk size
	 * @return the byte [ ]
	 */
	public static byte[] buildPenSwUpgrade( String filename, int filesize, short chunk_count, short chunk_size )
	{
		PacketBuilder builder = new PacketBuilder( 136 );

		builder.setCommand( CMD.P_PenSWUpgradeCommand );

		// FILE NAME
		ByteBuffer temp = ByteBuffer.wrap( filename.getBytes() );
		temp.order( ByteOrder.LITTLE_ENDIAN );
		byte[] bFilename = temp.array();

		builder.write( bFilename, 128 );

		// FILE SIZE
		byte[] bFilesize = ByteConverter.intTobyte( filesize );
		builder.write( bFilesize, 4 );

		// PACKET COUNT
		byte[] bChunkCount = ByteConverter.shortTobyte( chunk_count );
		builder.write( bChunkCount, 2 );

		// PACKET SIZE
		byte[] bChunkSize = ByteConverter.shortTobyte( chunk_size );
		builder.write( bChunkSize, 2 );

		// builder.showPacket();

		NLog.d( "[ProtocolParser] REQ  buildPenSwUpgrade.");
		return builder.getPacket();
	}

	/**
	 * Build pen sw upgrade response byte [ ].
	 * 0x53 P_PenSWUpgradeResponse
	 *
	 * @param index    the index
	 * @param checksum the checksum
	 * @param data     the data
	 * @return the byte [ ]
	 */
	public static byte[] buildPenSwUpgradeResponse( int index, byte checksum, byte[] data )
	{
		int dataLength = data.length + 3;

		PacketBuilder sendbyte = new PacketBuilder( dataLength );
		sendbyte.setCommand( CMD.P_PenSWUpgradeResponse );
		sendbyte.write( ByteConverter.shortTobyte( (short) index ) );
		sendbyte.write( checksum );
		sendbyte.write( data );

		NLog.d( "[ProtocolParser] REQ  buildPenSwUpgradeResponse.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * The constant USING_NOTE_TYPE_NOTE.
	 */
	public static final int USING_NOTE_TYPE_NOTE = 1;
	/**
	 * The constant USING_NOTE_TYPE_SECTION_OWNER.
	 */
	public static final int USING_NOTE_TYPE_SECTION_OWNER = 2;
	/**
	 * The constant USING_NOTE_TYPE_ALL.
	 */
	public static final int USING_NOTE_TYPE_ALL = 3;

	/**
	 * Build add using notes byte [ ].
	 *
	 * @param sectionId the section id
	 * @param ownerId   the owner id
	 * @param noteIds   the note ids
	 * @return the byte [ ]
	 */
	public static byte[] buildAddUsingNotes( int sectionId, int ownerId, int[] noteIds )
	{
		byte[] ownerByte = ByteConverter.intTobyte( ownerId );

		PacketBuilder sendbyte = new PacketBuilder( 42 );
		sendbyte.setCommand( CMD.P_UsingNoteNotify );
		sendbyte.write( (byte) USING_NOTE_TYPE_NOTE );
		sendbyte.write( (byte) noteIds.length );
		sendbyte.write( ownerByte[0] );
		sendbyte.write( ownerByte[1] );
		sendbyte.write( ownerByte[2] );
		sendbyte.write( (byte) sectionId );

		for ( int noteId : noteIds )
		{
			sendbyte.write( ByteConverter.intTobyte( noteId ) );
		}
		
		sendbyte.write( new byte[ (9-noteIds.length) * 4 ] );

		NLog.d( "[ProtocolParser] REQ  buildAddUsingNotes.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build add using notes byte [ ].
	 *
	 * @param sectionId the section id
	 * @param ownerId   the owner id
	 * @return the byte [ ]
	 */
	public static byte[] buildAddUsingNotes( int sectionId, int ownerId )
	{
		byte[] ownerByte = ByteConverter.intTobyte( ownerId );

		PacketBuilder sendbyte = new PacketBuilder( 42 );
		sendbyte.setCommand( CMD.P_UsingNoteNotify );
		sendbyte.write( (byte) USING_NOTE_TYPE_SECTION_OWNER );
		sendbyte.write( (byte) 1 );
		sendbyte.write( ownerByte[0] );
		sendbyte.write( ownerByte[1] );
		sendbyte.write( ownerByte[2] );
		sendbyte.write( (byte) sectionId );
		sendbyte.write( new byte[36] );

		NLog.d( "[ProtocolParser] REQ  buildAddUsingNotes.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build add using all notes byte [ ].
	 *
	 * @return the byte [ ]
	 */
	public static byte[] buildAddUsingAllNotes()
	{
		PacketBuilder sendbyte = new PacketBuilder( 42 );
		sendbyte.setCommand( CMD.P_UsingNoteNotify );
		sendbyte.write( (byte) USING_NOTE_TYPE_ALL );
		sendbyte.write( (byte) 0 );
		sendbyte.write( new byte[40] );

		NLog.d( "[ProtocolParser] REQ  buildAddUsingAllNotes.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build req offline data byte [ ].
	 *
	 * @param sectionId the section id
	 * @param ownerId   the owner id
	 * @param noteId    the note id
	 * @return the byte [ ]
	 */
	public static byte[] buildReqOfflineData( int sectionId, int ownerId, int noteId )
	{
		byte[] ownerByte = ByteConverter.intTobyte( ownerId );

		PacketBuilder sendbyte = new PacketBuilder( 45 );
		sendbyte.setCommand( CMD.P_OfflineDataRequest );
		sendbyte.write( ownerByte[0] );
		sendbyte.write( ownerByte[1] );
		sendbyte.write( ownerByte[2] );
		sendbyte.write( (byte) sectionId );
		sendbyte.write( (byte) 1 );
		sendbyte.write( ByteConverter.intTobyte( noteId ) );
		sendbyte.write( new byte[36] );

		NLog.d( "[ProtocolParser] REQ  buildReqOfflineData.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build req offline data list byte [ ].
	 *
	 * @return the byte [ ]
	 */
	public static byte[] buildReqOfflineDataList()
	{
		PacketBuilder sendbyte = new PacketBuilder( 1 );
		sendbyte.setCommand( CMD.P_OfflineNoteList );
		sendbyte.write( (byte) 0x00 );

		NLog.d( "[ProtocolParser] REQ  buildReqOfflineDataList.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build req offline data remove byte [ ].
	 *
	 * @param sectionId the section id
	 * @param ownerId   the owner id
	 * @return the byte [ ]
	 */
	public static byte[] buildReqOfflineDataRemove( int sectionId, int ownerId )
	{
		byte[] ownerByte = ByteConverter.intTobyte( ownerId );

		PacketBuilder sendbyte = new PacketBuilder( 12 );
		sendbyte.setCommand( CMD.P_OfflineDataRemove );
		sendbyte.write( ownerByte[0] );
		sendbyte.write( ownerByte[1] );
		sendbyte.write( ownerByte[2] );
		sendbyte.write( (byte) sectionId );
		sendbyte.write( (byte) 0x01 );
		sendbyte.write( (byte) 0x02 );
		sendbyte.write( (byte) 0x03 );
		sendbyte.write( (byte) 0x04 );
		sendbyte.write( (byte) 0x05 );
		sendbyte.write( (byte) 0x06 );
		sendbyte.write( (byte) 0x07 );
		sendbyte.write( (byte) 0x08 );

		NLog.d( "[ProtocolParser] REQ  buildReqOfflineDataRemove.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build pen auto power setup byte [ ].
	 *
	 * @param on the on
	 * @return the byte [ ]
	 */
	public static byte[] buildPenAutoPowerSetup( boolean on )
	{
		PacketBuilder sendbyte = new PacketBuilder( 1 );
		sendbyte.setCommand( CMD.P_AutoPowerOnSet );
		sendbyte.write( (byte) (on ? 1 : 0) );

		NLog.d( "[ProtocolParser] REQ  buildPenAutoPowerSetup.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build auto shutdown time setup byte [ ].
	 *
	 * @param shutdownTime the shutdown time
	 * @return the byte [ ]
	 */
	public static byte[] buildAutoShutdownTimeSetup( short shutdownTime )
	{
		PacketBuilder sendbyte = new PacketBuilder( 2 );
		sendbyte.setCommand( CMD.P_AutoShutdownTime );
		sendbyte.write( ByteConverter.shortTobyte( shutdownTime ), 2 );
		NLog.d( "[ProtocolParser] REQ  buildAutoShutdownTimeSetup.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build pen sensitivity setup byte [ ].
	 *
	 * @param sensitivity the sensitivity
	 * @return the byte [ ]
	 */
	public static byte[] buildPenSensitivitySetup( short sensitivity )
	{
		PacketBuilder sendbyte = new PacketBuilder( 2 );
		sendbyte.setCommand( CMD.P_PenSensitivity );
		sendbyte.write( ByteConverter.shortTobyte( sensitivity ), 2 );
		NLog.d( "[ProtocolParser] REQ  buildPenSensitivitySetup.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build pen beep setup byte [ ].
	 *
	 * @param on the on
	 * @return the byte [ ]
	 */
	public static byte[] buildPenBeepSetup( boolean on )
	{
		PacketBuilder sendbyte = new PacketBuilder( 1 );
		sendbyte.setCommand( CMD.P_BeepSet );
		sendbyte.write( (byte) (on ? 1 : 0) );
		NLog.d( "[ProtocolParser] REQ  buildPenBeepSetup.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build pen tip color setup byte [ ].
	 *
	 * @param color the color
	 * @return the byte [ ]
	 */
	public static byte[] buildPenTipColorSetup( int color )
	{
		byte[] cbyte = ByteConverter.intTobyte( color );

		byte[] nbyte = new byte[] { cbyte[0], cbyte[1], cbyte[2], (byte) 0x01 };

		PacketBuilder sendbyte = new PacketBuilder( 4 );
		sendbyte.setCommand( CMD.P_PenColorSet );
		sendbyte.write( nbyte, 4 );

		NLog.d( "[ProtocolParser] REQ  buildPenTipColorSetup.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build password input byte [ ].
	 *
	 * @param password the password
	 * @return the byte [ ]
	 */
	public static byte[] buildPasswordInput( String password )
	{
		PacketBuilder sendbyte = new PacketBuilder( 16 );
		sendbyte.setCommand( CMD.P_PasswordResponse );
		if(password.length() == 0)
			password = DEFAULT_PASSWORD;
		sendbyte.write( ByteConverter.stringTobyte( password ), 16 );
		NLog.d( "[ProtocolParser] REQ  buildPasswordInput.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build password setup byte [ ].
	 *
	 * @param oldPassword the old password
	 * @param newPassword the new password
	 * @return the byte [ ]
	 */
	public static byte[] buildPasswordSetup( String oldPassword, String newPassword )
	{
		PacketBuilder sendbyte = new PacketBuilder( 32 );
		sendbyte.setCommand( CMD.P_PasswordSet );
		if(oldPassword.length() == 0)
			oldPassword = "0000";
		if(newPassword.length() == 0)
			newPassword = "0000";
		sendbyte.write( ByteConverter.stringTobyte( oldPassword ), 16 );
		sendbyte.write( ByteConverter.stringTobyte( newPassword ), 16 );

		NLog.d( "[ProtocolParser] REQ  buildPasswordSetup.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
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
		sendbyte.setCommand( CMD.P_ProfileRequest );
		// Profile filename
		sendbyte.write(ByteConverter.stringTobyte( filename ),8 );
		// request type
		sendbyte.write(PROFILE_CREATE );
		// Profile password
		sendbyte.write(pass,8 );

		// sector size
		sendbyte.write( ByteConverter.shortTobyte( (short)32 ),2 );
		// sector count(2^N Currently fixed 2^8)
		sendbyte.write( ByteConverter.shortTobyte( (short)8 ),2 );
		NLog.d( "[ProtocolParser] REQ  buildProfileCreate.");
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
		sendbyte.setCommand( CMD.P_ProfileRequest );
		// Profile filename
		sendbyte.write(ByteConverter.stringTobyte( filename ),8 );
		// request type
		sendbyte.write(PROFILE_DELETE );
		// Profile password
		sendbyte.write(pass,8 );
		NLog.d( "[ProtocolParser] REQ  buildProfileDelete.");
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
		sendbyte.setCommand( CMD.P_ProfileRequest );
		// Profile filename
		sendbyte.write(ByteConverter.stringTobyte( filename ),8 );
		// request type
		sendbyte.write(PROFILE_INFO );
		NLog.d( "[ProtocolParser] REQ  buildProfileInfo.");
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
		sendbyte.setCommand( CMD.P_ProfileRequest );
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
		NLog.d( "[ProtocolParser] REQ  buildProfileReadValue.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
	}

	/**
	 * Build profile write value byte [ ].
	 *
	 * @param filename the filename
	 * @param pass     the pass
	 * @param keys     the keys
	 * @param data     the data
	 * @return the byte [ ]
	 */
	public static byte[] buildProfileWriteValue(String filename, byte[] pass, String[] keys, byte[][] data)
	{
		int data_len = 0;
		for(int i = 0; i < data.length; i++)
		{
			data_len += 16;
			data_len += 2;
			data_len += data[i].length;
		}

		PacketBuilder sendbyte = new PacketBuilder( 8+1+8+1+ data_len);
		sendbyte.setCommand( CMD.P_ProfileRequest );
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
			sendbyte.write(ByteConverter.stringTobyte( keys[i] ),16 );
			sendbyte.write(ByteConverter.shortTobyte( (short) data[i].length ),2 );
			sendbyte.write(data[i] );
		}
		NLog.d( "[ProtocolParser] REQ  buildProfileWriteValue.");
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
		sendbyte.setCommand( CMD.P_ProfileRequest );
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
		NLog.d( "[ProtocolParser20] REQ  buildProfileDeleteValue.");
		sendbyte.showPacket();
		return sendbyte.getPacket();
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
		public void onCreatePacket( Packet packet );
	}

	/**
	 * The type Packet builder.
	 */
	public static class PacketBuilder
	{
		/**
		 * The Packet.
		 */
		byte[] packet;
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
		 * Instantiates a new Packet builder.
		 *
		 * @param length the length
		 */
		public PacketBuilder( int length )
		{
			allocate( length );
		}

		/**
		 * write command field
		 *
		 * @param cmd the cmd
		 */
		public void setCommand( int cmd )
		{
			packet[1] = (byte) cmd;
		}

		/**
		 * buffer allocation and set packet frame
		 *
		 * @param length the length
		 */
		public void allocate( int length )
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
		 * write data to data field
		 *
		 * @param buffer the buffer
		 */
		public void write( byte[] buffer )
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
		public void write( byte[] buffer, int valid_size )
		{
			buffer = ResizeByteArray( buffer, valid_size );
			this.write( buffer );
		}

		/**
		 * write single data to data field
		 *
		 * @param data the data
		 */
		public void write( byte data )
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
		public byte[] ResizeByteArray( byte[] bytes, int newsize )
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
		public byte[] getPacket()
		{
			return packet;
		}

		/**
		 * Show packet.
		 */
		public void showPacket()
		{
			StringBuffer buff = new StringBuffer();

			for ( byte item : packet )
			{
				int int_data = (int) (item & 0xFF);
				buff.append( Integer.toHexString( int_data ) + ", " );
			}

			NLog.d( "[PacketBuilder] showPacket : " + buff.toString() );

			buff = null;
		}

		/**
		 * Show packet.
		 *
		 * @param bytes the bytes
		 */
		public static void showPacket( byte[] bytes )
		{
			StringBuffer buff = new StringBuffer();

			for ( byte item : bytes )
			{
				int int_data = (int) (item & 0xFF);
				buff.append( Integer.toHexString( int_data ) + ", " );
			}

			NLog.d( "[PacketBuilder] showPacket : " + buff.toString() );

			buff = null;
		}
	}
}
