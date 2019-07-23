package kr.neolab.sdk.pen.offline;

import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import kr.neolab.sdk.pen.bluetooth.lib.ByteConverter;
import kr.neolab.sdk.util.NLog;

/**
 * The type Offline file.
 */
public class OfflineFile
{
	/**
	 * The Append count.
	 */
	public int appendCount;

	/**
	 * The Packet count.
	 */
	public int packetCount;

	private boolean isCompressed = false;

	private int sectionId = 0, ownerId = 0, noteId = 0, pageId = 0;
	
	private static String OFFLINE_FILE_PATH = getDefaultFilePath();

	/**
	 * The constant DEFAULT_FILE_FORMAT.
	 */
	public static String DEFAULT_FILE_FORMAT = "%d_%d_%d_%d_%d.%s";
	
	private File tempFile = null;
	
	private BufferedOutputStream buffer;

	/**
	 * Instantiates a new Offline file.
	 *
	 * @param penAddress   the pen address
	 * @param fileinfo     the fileinfo
	 * @param packetCount  the packet count
	 * @param isCompressed the is compressed
	 */
	public OfflineFile( String penAddress, String fileinfo, int packetCount, boolean isCompressed )
	{
		String[] arr = fileinfo.split( "\\\\" );

		int sectionOwner = Integer.parseInt( arr[2] );
		
		byte[] bso = ByteConverter.intTobyte( sectionOwner );
		
		sectionId = (int) (bso[3] & 0xFF);
		ownerId = ByteConverter.byteArrayToInt( new byte[] { bso[0], bso[1], bso[2], (byte) 0x00 } );
		
		noteId = Integer.parseInt( arr[3] );
		pageId = Integer.parseInt( arr[4] );

		this.packetCount = packetCount;
		this.appendCount = 0;
		this.isCompressed = isCompressed;
		
		openTempFile(penAddress);
	}

	/**
	 * Instantiates a new Offline file.
	 * supported from Protocol 2.0
	 *
	 * @param penAddress   the pen address
	 * @param sectionId    the section id
	 * @param ownerId      the owner id
	 * @param noteId       the note id
	 * @param isCompressed the is compressed
	 */
	public OfflineFile( String penAddress, int sectionId, int ownerId, int noteId, boolean isCompressed )
	{
		this.sectionId = sectionId;
		this.ownerId = ownerId;
		this.noteId = noteId;

		this.appendCount = 0;
		this.isCompressed = isCompressed;

		openTempFile(penAddress);
	}


	private void openTempFile(String penAddress)
	{
		penAddress = penAddress.replace( ":", "" );
		File path = new File( OFFLINE_FILE_PATH );

		if ( !path.isDirectory() )
		{
			path.mkdirs();
		}

		try
		{
			tempFile = File.createTempFile( "_offline_"+penAddress, ".tmp", path );
			
			buffer = new BufferedOutputStream( new FileOutputStream( tempFile ) );
		}
		catch ( IOException e )
		{
			NLog.e("[OfflineFile] openTempFile exception", e);
		}
	}

	/**
	 * Clear temp file.
	 *
	 * @param penAddress the pen address
	 */
	public void clearTempFile(String penAddress)
	{
		penAddress = penAddress.replace( ":","" );
		File path = new File( OFFLINE_FILE_PATH);

		File[] files = path.listFiles();

		for ( File file : files )
		{
			if ( file.isFile() && file.getName().endsWith( ".tmp" ) )
			{
				file.delete();
			}
		}
	}

	/**
	 * Gets note id.
	 *
	 * @return the note id
	 */
	public int getNoteId()
	{
		return noteId;
	}

	/**
	 * Gets page id.
	 *
	 * @return the page id
	 */
	public int getPageId()
	{
		return pageId;
	}

	/**
	 * Gets section id.
	 *
	 * @return the section id
	 */
	public int getSectionId()
	{
		return sectionId;
	}

	/**
	 * Gets owner id.
	 *
	 * @return the owner id
	 */
	public int getOwnerId()
	{
		return ownerId;
	}

	/**
	 * Gets count.
	 *
	 * @return the count
	 */
	public int getCount()
	{
		return appendCount;
	}

	/**
	 * Sets offline file path.
	 *
	 * @param newPath the new path
	 * @return the offline file path
	 */
	public synchronized static boolean setOfflineFilePath( String newPath )
	{
		if ( !newPath.endsWith( "/" ) )
		{
			newPath = newPath + "/";
		}

		File newTarget = new File( newPath );

		if ( !newTarget.exists() || !newTarget.isDirectory() )
		{
			return false;
		}

		OFFLINE_FILE_PATH = newPath;

		return true;
	}

	/**
	 * Gets default file path.
	 *
	 * @return the default file path
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
	 * Gets offline file path.
	 *
	 * @return the offline file path
	 */
	public static String getOfflineFilePath()
	{
		return OFFLINE_FILE_PATH;
	}

	/**
	 * Append.
	 *
	 * @param data  the data
	 * @param index the index
	 */
	public void append( byte[] data, int index )
	{
		appendCount++;
		
		try
		{
			for ( int i = 0; i < data.length; i++ )
			{
				buffer.write( data, i, 1 );
			}
		}
		catch ( IOException e )
		{
			NLog.e("[OfflineFile] append exception", e);
		}
	}

	/**
	 * Make string.
	 *
	 * @return the string
	 */
	public String make()
	{
		if ( buffer != null )
		{
			try
			{
				buffer.close();
			}
			catch ( IOException e )
			{
				NLog.e("[OfflineFile] make exception", e);
			}
			
			buffer = null;
		}
		
		String filename = String.format( DEFAULT_FILE_FORMAT, sectionId, ownerId, noteId, pageId, System.currentTimeMillis(), isCompressed ? "zip" : "pen" );
		boolean result = tempFile.renameTo( new File(OFFLINE_FILE_PATH, filename) );
		
		NLog.d("[OfflineFile] result : " + result + ", filename : " + filename );
		
		return OFFLINE_FILE_PATH + filename;
	}
}
