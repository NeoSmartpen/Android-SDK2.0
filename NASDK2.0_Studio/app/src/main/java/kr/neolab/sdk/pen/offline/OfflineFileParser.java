package kr.neolab.sdk.pen.offline;

import android.graphics.Color;
import android.os.Environment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kr.neolab.sdk.ink.structure.DotType;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.pen.bluetooth.lib.ByteConverter;
import kr.neolab.sdk.pen.bluetooth.lib.Chunk;
import kr.neolab.sdk.pen.bluetooth.lib.Packet;
import kr.neolab.sdk.pen.filter.Fdot;
import kr.neolab.sdk.pen.filter.FilterForFilm;
import kr.neolab.sdk.pen.filter.FilterForPaper;
import kr.neolab.sdk.pen.filter.IFilterListener;
import kr.neolab.sdk.util.NLog;

/**
 * For parsing the data file off-line function that implements the class
 *
 * @author CHY
 */
public class OfflineFileParser implements IFilterListener
{
	private byte[] data, body;

	private int sectionId, ownerId, noteId, pageId;
	private int lineCount, dataSize;
	// private byte headerCheckSum;

	private boolean isCompressed = false;

	private File target = null;

	private static final int LINE_MARK_1 = 0x4c;
	private static final int LINE_MARK_2 = 0x4e;

	private static final int BYTE_LINE_SIZE = 28;
	private static final int BYTE_DOT_SIZE = 8;
	private static final int BYTE_HEADER_SIZE = 64;

	private FilterForPaper filterPaper;
	private FilterForFilm filterFilm;

	private ArrayList<Stroke> strokes = new ArrayList<Stroke>();
	private Stroke stroke = null;
	private float[] factor = null;

	private OfflineFileParser()
	{
		// Filter registration
		this.filterPaper = new FilterForPaper( this );
		this.filterFilm = new FilterForFilm( this );
	}

	/**
	 * This constructor is offline, enter the name and location of the file to
	 * be loaded into memory take.
	 *
	 * @param filepath location of offline data file
	 * @param filename name of offline data file
	 */
	public OfflineFileParser( String filepath, String filename )
	{
		this();

		if ( !filepath.endsWith( "/" ) )
		{
			filepath = filepath + "/";
		}

		this.target = new File( filepath, filename );
	}

	/**
	 * This constructor receives the name of the Offline Files to be loaded into
	 * memory.
	 *
	 * @param filename name of offline data file
	 */
	public OfflineFileParser( String filename )
	{
		this();
		this.target = new File( getDefaultFilePath(), filename );
	}

	/**
	 * This constructor receives the instance of the Offline Files to be loaded
	 * into memory.
	 *
	 * @param file the file
	 */
	public OfflineFileParser( File file )
	{
		this();
		this.target = file;
	}

	/**
	 * Sets calibrate.
	 *
	 * @param factor the factor
	 */
	public void setCalibrate ( float[] factor )
	{
		this.factor = factor;
	}

	/**
	 * request default file path
	 *
	 * @return default file path
	 */
	public static String getDefaultFilePath()
	{
		return getExternalStoragePath() + "/neolab/offline/";
	}

	/**
	 * Gets external storage path.
	 *
	 * @return the external storage path
	 */
	public static String getExternalStoragePath()
	{
		if ( Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED ) )
		{
			return Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		else
		{
			return Environment.MEDIA_UNMOUNTED;
		}
	}

	/**
	 * find list of offline data file
	 *
	 * @param penAddress the pen address
	 * @return string [ ]
	 */
	public static String[] getOfflineFiles(String penAddress)
	{
		return getOfflineFiles( penAddress,OfflineFile.getOfflineFilePath() );
	}

	/**
	 * find list of offline data file
	 *
	 * @param penAddress the pen address
	 * @param filepath   The location of the data files are stored offline
	 * @return string [ ]
	 */
	public static String[] getOfflineFiles(String penAddress, String filepath )
	{
		ArrayList<String> filelist = new ArrayList<String>();

		penAddress = penAddress.replace( ":", "" );
		File path = new File( filepath);
		if ( path != null && path.listFiles() != null && path.listFiles().length > 0 )
		{
			for ( File file : path.listFiles() )
			{
				String filename = file.getName();

				if ( filename.endsWith( ".pen" ) || filename.endsWith( ".zip" ) )
				{
					filelist.add( filename );
				}
			}
		}

		String[] result = null;

		if ( filelist.size() > 0 )
		{
			result = filelist.toArray( new String[1] );
		}

		filelist.clear();
		filelist = null;

		return result;
	}

	/**
	 * parse loaded file
	 *
	 * @return stroke [ ]
	 * @throws Exception the exception
	 */
	public Stroke[] parse() throws Exception
	{
		NLog.i( "[OfflineFileParser] process start" );

		stroke = null;
		strokes.clear();

		if ( target == null )
		{
			return null;
		}

		String filename = target.getName();

		if ( !filename.endsWith( ".zip" ) && !filename.endsWith( ".pen" ) )
		{
			return null;
		}

		if ( filename.endsWith( ".zip" ) )
		{
			this.isCompressed = true;
		}

		// Merge files
		NLog.i( "[OfflineFileParser] process loadDataFromFile" );
		this.loadDataFromFile( target );

		// Header parsing
		NLog.i( "[OfflineFileParser] process parseHeader" );
		this.parseHeader();

		// Body parsing
		NLog.i( "[OfflineFileParser] process parseBody" );
		this.parseBody();

		NLog.i( "[OfflineFileParser] process finished" );

		data = null;
		body = null;
		
		if ( strokes == null || strokes.size() <= 0 )
		{
			return null;
		}
		else
		{
			return strokes.toArray( new Stroke[0] );
		}
	}

	/**
	 * delete loaded file
	 *
	 * @throws Exception the exception
	 */
	public void delete() throws Exception
	{
		target.delete();
	}

	private void loadDataFromFile( File file ) throws Exception
	{
		ByteArrayOutputStream ous = null;
		InputStream ios = null;

		try
		{
			byte[] buff = new byte[1024];
			ous = new ByteArrayOutputStream();
			ios = new FileInputStream( file );

			int count = 0;

			while ( (ios.available() > 0) && (count = ios.read( buff )) != -1 )
			{
				ous.write( buff, 0, count );
			}

			byte[] buffer = ous.toByteArray();

			if ( isCompressed )
			{
				buffer = unzip( buffer );
			}

			this.data = buffer;
		}
		finally
		{
			try
			{
				if ( ous != null )
				{
					ous.close();
				}

				if ( ios != null )
				{
					ios.close();
				}
			}
			catch ( IOException e )
			{
			}
		}
	}

	private void parseHeader() throws Exception
	{
		byte[] header = Packet.copyOfRange( data, data.length - BYTE_HEADER_SIZE, BYTE_HEADER_SIZE );

		byte[] osbyte = Packet.copyOfRange( header, 6, 4 );

		this.sectionId = (int) (osbyte[3] & 0xFF);
		this.ownerId = ByteConverter.byteArrayToInt( new byte[] { osbyte[0], osbyte[1], osbyte[2], (byte) 0x00 } );

		this.noteId = ByteConverter.byteArrayToInt( Packet.copyOfRange( header, 10, 4 ) );
		this.pageId = ByteConverter.byteArrayToInt( Packet.copyOfRange( header, 14, 4 ) );

		this.lineCount = ByteConverter.byteArrayToInt( Packet.copyOfRange( header, 22, 4 ) );
		this.dataSize = ByteConverter.byteArrayToInt( Packet.copyOfRange( header, 26, 4 ) );
		// this.headerCheckSum = header[BYTE_HEADER_SIZE-1];

		body = Packet.copyOfRange( data, 0, data.length - BYTE_HEADER_SIZE );
		
		if ( body.length != dataSize )
		{
			throw new Exception( "data size is invalid." );
		}
		
		NLog.i( "[OfflineFileParser] sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId + ", pageId : " + pageId + ", lineCount : " + lineCount + ", fileSize : " + dataSize + "byte" );
	}

	private void parseBody() throws Exception
	{
		NLog.d( "[OfflineFileParser] parse file" );

		long lhPenDnTime = 0, lhPenUpTime = 0, prevTimestamp = 0;

		int lhDotTotal = 0, dotCount = 0, dotStartIndex = 0, dotSize = 0;

		byte lhCheckSum = 0;
		
		int lhLineColor = Color.BLACK;

		// The dot of the current line
		ArrayList<Fdot> tempDots = new ArrayList<Fdot>();

		int idx = 0;

		if ( body == null || body.length <= 0 )
		{
			throw new Exception( "stroke data not found" );
		}
		
		while ( idx < body.length )
		{	
			if ( (int) (body[idx] & 0xFF) == LINE_MARK_1 && (int) (body[idx + 1] & 0xFF) == LINE_MARK_2 )
			{
				tempDots = new ArrayList<Fdot>();

				lhPenDnTime = ByteConverter.byteArrayToLong( Packet.copyOfRange( body, idx + 2, 8 ) );
				lhPenUpTime = ByteConverter.byteArrayToLong( Packet.copyOfRange( body, idx + 10, 8 ) );
				
				lhDotTotal = ByteConverter.byteArrayToInt( Packet.copyOfRange( body, idx + 18, 4 ) );
				
				byte[] lineColorBytes = Packet.copyOfRange( body, idx + 23, 4 );

				lhLineColor = ByteConverter.byteArrayToInt( new byte[] { lineColorBytes[0], lineColorBytes[1], lineColorBytes[2], (byte) 0XFF } );

				lhCheckSum = body[idx + 27];

				idx += BYTE_LINE_SIZE;

				dotStartIndex = idx;
				dotSize = 0;
				dotCount = 0;
			}
			else
			{
				dotCount++;

				// If it exceeds the number of dots defined in the line header, it moves the pointer by one byte until the LN is reached.
				if ( dotCount > lhDotTotal )
				{
					idx++;
					continue;
				}

				int sectionId = this.sectionId;
				int ownerId = this.ownerId;
				int noteId = this.noteId;
				int pageId = this.pageId;

				long time = (int) (body[idx] & 0xFF);

				int x = ByteConverter.byteArrayToShort( Packet.copyOfRange( body, idx + 1, 2 ) );
				int y = ByteConverter.byteArrayToShort( Packet.copyOfRange( body, idx + 3, 2 ) );

				int fx = (int) (body[idx + 5] & 0xFF);
				int fy = (int) (body[idx + 6] & 0xFF);

				int pressure = (int) (body[idx + 7] & 0xFF);

				int color = lhLineColor;

				boolean isPenUp = false;
				int dotType;
				long timestamp;

				if ( dotCount == 1 )
				{
					dotType = DotType.PEN_ACTION_DOWN.getValue();
					timestamp = lhPenDnTime + time;
					prevTimestamp = timestamp;
				}
				else if ( lhDotTotal > dotCount )
				{
					dotType = DotType.PEN_ACTION_MOVE.getValue();
					timestamp = prevTimestamp + time;
					prevTimestamp = timestamp;
				}
				else
				{
					dotType = DotType.PEN_ACTION_UP.getValue();
					timestamp = lhPenUpTime;
					isPenUp = true;
				}

				if(factor != null)
					pressure = (int)factor[pressure];
				Fdot dot = new Fdot((x + (float) (fx * 0.01)), (y + (float) (fy * 0.01)), pressure, dotType, timestamp, sectionId, ownerId, noteId, pageId, color,0 , 0, 0, 0);

				tempDots.add( dot );

				dotSize += 8;
				
				if ( isPenUp )
				{
					byte dotCalcCs = Chunk.calcChecksum( Packet.copyOfRange( body, dotStartIndex, dotSize ) );

					if ( dotCalcCs == lhCheckSum )
					{
						for ( int j = 0; j < tempDots.size(); j++ )
						{
							filterDot( tempDots.get( j ) );
						}
					}
					else
					{
						NLog.e( "[OfflineFileParser] Stroke cs : " + Integer.toHexString( (int) (lhCheckSum & 0xFF) ) + ", calc : " + Integer.toHexString( (int) (dotCalcCs & 0xFF) ) );
					}

					tempDots = new ArrayList<Fdot>();
				}
				
				idx += BYTE_DOT_SIZE;
			}
		}
	}

	private byte[] unzip( byte[] input_data ) throws Exception
	{
		ZipInputStream zipStream = new ZipInputStream( new ByteArrayInputStream( input_data ) );
		ZipEntry entry = null;

		byte[] result = null;

		while ( (entry = zipStream.getNextEntry()) != null )
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			byte[] buff = new byte[1024];

			int count = 0, loop = 0;

			while ( (count = zipStream.read( buff )) != -1 )
			{
				baos.write( buff, 0, count );
				// NLog.i("[OfflineFileParser] unzip read loop : " + loop);
				if ( loop++ > 1048567 )
				{
					throw new Exception();
				}
			}

			result = baos.toByteArray();

			zipStream.closeEntry();
		}

		zipStream.close();

		return result;
	}

	private void filterDot( Fdot mdot )
	{
		if ( mdot.noteId == 45 && mdot.pageId == 1 )
		{
			filterFilm.put( mdot );
		}
		else
		{
			filterPaper.put( mdot );
		}
	}

	@Override
	public void onFilteredDot( Fdot fdot )
	{
		if ( DotType.isPenActionDown( fdot.dotType ) || stroke == null || stroke.isReadOnly() )
		{
			stroke = new Stroke( fdot.sectionId, fdot.ownerId, fdot.noteId, fdot.pageId, fdot.color );
			strokes.add( stroke );
		}

		stroke.add( fdot.toDot() );
	}
}
