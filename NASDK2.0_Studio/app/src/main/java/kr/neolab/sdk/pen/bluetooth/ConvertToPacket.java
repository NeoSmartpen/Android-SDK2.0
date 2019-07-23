package kr.neolab.sdk.pen.bluetooth;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import kr.neolab.sdk.util.NLog;

/**
 * Make and send the packet from offline file.
 */
public class ConvertToPacket {  //[2018.03.05] Stroke Test

    String strokeFileName;
    String dotFileName;
    PipedOutputStream pipedOutputStream;
    Options options;

    //    private static final int REPEAT_COUNT = 1;

    private int DELAY_DELIMETER_PACKET_COUNT = 0;
    private final int DELAY_DELIMETER_PACKET_MAX_COUNT = 3;
    private final int DELAY_TIME = 10;

    public static final String PATH = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS ).getPath()+ File.separator+"neolab_data"+File.separator;

    public ConvertToPacket(String strokeFileName, String dotFileName, PipedOutputStream pipedOutputStream, Options options) {
        this.strokeFileName = strokeFileName;
        this.dotFileName = dotFileName;
        this.pipedOutputStream = pipedOutputStream;
        this.options = options;

        DELAY_DELIMETER_PACKET_COUNT = 0;
    }

    public static class Options {
        public Options() {
//            options = 0;
            percent_miss_up = 0;
            percent_miss_down = 0;
            percent_miss_up_down = 0;
            percent_spark_dot = 0;
            percent_miss_page_id_change = 0;
        }

//        public static final int OPTION_MISS_UP = 0x00000001;
//        public static final int OPTION_MISS_DOWN = 0x00000002;
//        public static final int OPTION_MISS_UP_DOWN = 0x00000004;
//        public static final int OPTION_SPARK_DOT = 0x00000008;

//        private int options;
        public int percent_miss_up;
        public int percent_miss_down;
        public int percent_miss_up_down;
        public int percent_spark_dot;
        public int percent_miss_page_id_change;

//        public int setOption(int option) {
//            options = options | option;
//            return options;
//        }
//
//        public boolean isEnable(int option) {
//            return (options & option) != 0;
//        }
    }

    public void sendPacket() {
		File strokeFile = new File( PATH, strokeFileName );
		File dotFile = new File( PATH, dotFileName );
		//File statusFile = new File(currentPath, "31B261O");

        if(strokeFile.exists() == false) {
            NLog.d("[ConvertToPacket] strokeFile not exists.");
            return;
        }
        if(dotFile.exists() == false) {
            NLog.d("[ConvertToPacket] dotFile not exists.");
            return;
        }
        if(pipedOutputStream == null) {
            NLog.d("[ConvertToPacket] pipedOutputStream is NULL");
            return;
        }

		
		byte strokeBytes[] = getBytesFromFile( strokeFile );
		byte dotBytes[] = getBytesFromFile( dotFile );
		
		if ( makeStrokePacket( strokeBytes, dotBytes) == false ) {
			// error
            NLog.d("[ConvertToPacket] sendPacket occurs error.");
		}

        NLog.d("[ConvertToPacket] sendPacket has completed.");
	}
	
	private boolean makeStrokePacket( byte[] stroke, byte[] dot)
	{
		if ( stroke == null || dot == null )
			return false;
		else if ( stroke.length == 0 || dot.length == 0 )
			return false;
		else if ( stroke.length % 32 != 0 )
			return false;
		
		int section = stroke[ 3 ];
		int owner = stroke[ 4 ] << 16 | stroke[ 5 ] << 8 | stroke[ 6 ];
		int note = stroke[ 7 ] << 24 | stroke[ 8 ] << 16 | stroke[ 9 ] << 8 | stroke[ 10 ];
        byte[] sectionOwner = GetSectionOwnerByte( section, owner );
        int len = ( stroke.length - 32 ) / 32;
        
        int sIndex = 32;	// current stroke array index
        int dIndex = 32;	// current dot array index

        boolean isMissedUpPacket = false;
        
        for ( int i=0; i<len; ++i ) {
        	long downTime = GetLongValue( stroke, sIndex + 0 );
            int writingTime = GetIntValue( stroke, sIndex + 8 );
            int pageId = GetIntValue( stroke, sIndex + 12 );
            byte penTipType = stroke[ sIndex + 17 ];
            int penTipColor = GetIntValue( stroke, sIndex + 18 );
            int tableFileNumber = GetShortValue( stroke, sIndex + 22 );
            int tableFileOffset = GetIntValue( stroke, sIndex + 24 );
            int dotCount = GetShortValue( stroke, sIndex + 28 );
            int successRate = stroke[ sIndex + 30 ];
            byte strokeChecksum = stroke[ sIndex + 31 ];
            
            byte forChecksum = 0;
            for ( int sx=0; sx<31; ++sx )
            	forChecksum += stroke[ sIndex + sx ];
            
            if ( forChecksum != strokeChecksum ) {
            	// checksum error
            	return false;
            }
            
            byte shortTemp[] = new byte[ 2 ];
            byte intTemp[] = new byte[ 4 ];
            byte longTemp[] = new byte[ 8 ];
            // make pen down packet
            if( random() > options.percent_miss_down )
            {
            	List< Byte > penDownPacket = new ArrayList< Byte >();
            	
            	penDownPacket.add( ( byte )0xc0 );
            	penDownPacket.add( ( byte )0x63 );
            	shortTemp = shortToBytes( ( short )14 );	setBytes( penDownPacket, shortTemp );
            	penDownPacket.add( ( byte )0x00 );
            	longTemp = longToBytes( downTime );       	setBytes( penDownPacket, longTemp );
            	penDownPacket.add( penTipType );
            	intTemp = intToBytes( penTipColor );     	setBytes( penDownPacket, intTemp );
            	penDownPacket.add( ( byte )0xc1 );
            	
            	byte packetArray[] = new byte[ penDownPacket.size() ];
            	for ( int a=0; a<penDownPacket.size(); ++a )	packetArray[ a ] = penDownPacket.get( a );
            	printHex(packetArray);
                sendPacket(pipedOutputStream, packetArray);
            }
            else {
                NLog.d("[ConvertToPacket] miss DOWN dot : stroke = "+i);
            }
//            else if(isMissedUpPacket)
//                isMissedUpPacket = false;

            // make page id packet
            if( random() > options.percent_miss_page_id_change)
            {
            	List< Byte > pageIdPacket = new ArrayList< Byte >();
            	
            	pageIdPacket.add( ( byte )0xc0 );
            	pageIdPacket.add( ( byte )0x64 );
            	shortTemp = shortToBytes( ( short )12 );	setBytes( pageIdPacket, shortTemp );
            	pageIdPacket.add( sectionOwner[ 0 ] );
            	pageIdPacket.add( sectionOwner[ 1 ] );
            	pageIdPacket.add( sectionOwner[ 2 ] );
            	pageIdPacket.add( sectionOwner[ 3 ] );
            	intTemp = intToBytes( note );     			setBytes( pageIdPacket, intTemp );
            	intTemp = intToBytes( pageId) ;     		setBytes( pageIdPacket, intTemp );
            	pageIdPacket.add( ( byte )0xc1 );
            	
            	byte packetArray[] = new byte[ pageIdPacket.size() ];
            	for ( int a=0; a<pageIdPacket.size(); ++a )		packetArray[ a ] = pageIdPacket.get( a );
            	printHex(packetArray);
                sendPacket(pipedOutputStream, packetArray);
            }

            int spark_dot_count = 0;
            final int spark_dot_max = 1;

            for ( int j=0; j<dotCount; ++j ) {
            	byte time = dot[ dIndex + 0 ];
                short force = GetShortValue( dot, dIndex + 1 );
                short x = GetShortValue( dot, dIndex + 3 );
                short y = GetShortValue( dot, dIndex + 5 );
                byte floatX = dot[ dIndex + 7 ];
                byte floatY = dot[ dIndex + 8 ];
                byte tiltX = dot[ dIndex + 9 ];
                byte tiltY = dot[ dIndex + 10 ];
                short twist = GetShortValue( dot, dIndex + 11 );
                byte labelCount = dot[ dIndex + 13 ];
                byte brightness = ( byte )( dot[ dIndex + 14 ] >> 4 );
                byte processTime = ( byte )( dot[ dIndex + 14 ] & 0x0f );
                byte dotChecksum = dot[ dIndex + 15 ];
                
                forChecksum = 0;
                for ( int dx=0; dx<15; ++dx )
                	forChecksum += dot[ dIndex + dx ];
                
                if ( forChecksum != dotChecksum ) {
                	// checksum error
                	return false;
                }
                
                // make dot packet
                {
                	List< Byte > dotPacket = new ArrayList< Byte >();

                	short sx = x;
                    short sy = y;
                    if( spark_dot_count < spark_dot_max && random() <= options.percent_spark_dot )
                    {
                        sx += 50;
                        sy += 50;
                        spark_dot_count++;
                        NLog.d("[ConvertToPacket] spark dot : stroke = "+i+", dot = "+j);
                    }
                	
                	dotPacket.add( ( byte )0xc0 );
                	dotPacket.add( ( byte )0x65 );
                	shortTemp = shortToBytes( ( short )13 );	setBytes( dotPacket, shortTemp );
                	dotPacket.add( time );
                	shortTemp = shortToBytes( force );			setBytes( dotPacket, shortTemp );
                	shortTemp = shortToBytes( sx );				setBytes( dotPacket, shortTemp );
                	shortTemp = shortToBytes( sy );				setBytes( dotPacket, shortTemp );
                	dotPacket.add( floatX );
                	dotPacket.add( floatY );
                	dotPacket.add( tiltX );
                	dotPacket.add( tiltY );
                	shortTemp = shortToBytes( twist );			setBytes( dotPacket, shortTemp );
                	dotPacket.add( ( byte )0xc1 );
                	
                	byte packetArray[] = new byte[ dotPacket.size() ];
                	for ( int a=0; a<dotPacket.size(); ++a )		packetArray[ a ] = dotPacket.get( a );
                	printHex(packetArray);
                    sendPacket(pipedOutputStream, packetArray);
                }
                
                dIndex += 16;
            }
            
            // make pen up packet
//            if( (options.isEnable(Options.OPTION_MISS_UP_DOWN) == false || random() > options.percent_miss_up_down) )
//            {
//                isMissedUpPacket = false;
//
//            	List< Byte > penUpPacket = new ArrayList< Byte >();
//
//            	penUpPacket.add( ( byte )0xc0 );
//            	penUpPacket.add( ( byte )0x63 );
//            	shortTemp = shortToBytes( ( short )14 );	setBytes( penUpPacket, shortTemp );
//            	penUpPacket.add( ( byte )0x01 );
//            	longTemp = longToBytes( downTime );       	setBytes( penUpPacket, longTemp );
//            	penUpPacket.add( penTipType );
//            	intTemp = intToBytes( penTipColor );     	setBytes( penUpPacket, intTemp );
//            	penUpPacket.add( ( byte )0xc1 );
//
//            	byte packetArray[] = new byte[ penUpPacket.size() ];
//            	for ( int a=0; a<penUpPacket.size(); ++a )		packetArray[ a ] = penUpPacket.get( a );
//            	printHex(packetArray);
//                sendPacket(pipedOutputStream, packetArray);
//            }
//            else
//                isMissedUpPacket = true;

            if(random() > options.percent_miss_up )
            {
                List< Byte > penUpPacket = new ArrayList< Byte >();

                penUpPacket.add( ( byte )0xc0 );
                penUpPacket.add( ( byte )0x63 );
                shortTemp = shortToBytes( ( short )14 );	setBytes( penUpPacket, shortTemp );
                penUpPacket.add( ( byte )0x01 );
                longTemp = longToBytes( downTime );       	setBytes( penUpPacket, longTemp );
                penUpPacket.add( penTipType );
                intTemp = intToBytes( penTipColor );     	setBytes( penUpPacket, intTemp );
                penUpPacket.add( ( byte )0xc1 );

                byte packetArray[] = new byte[ penUpPacket.size() ];
                for ( int a=0; a<penUpPacket.size(); ++a )		packetArray[ a ] = penUpPacket.get( a );
                printHex(packetArray);
                sendPacket(pipedOutputStream, packetArray);
            }
            else {
                NLog.d("[ConvertToPacket] miss UP dot : stroke = "+i);
            }
            
            sIndex += 32;
        }
        	
        
        return true;
	}

	private void sendPacket(PipedOutputStream pipedOutputStream, byte[] array) {
        try {
            pipedOutputStream.write(array);
//            pipedOutputStream.flush();
        }catch (IOException e) {
            e.printStackTrace();
        }

        if(++DELAY_DELIMETER_PACKET_COUNT >= DELAY_DELIMETER_PACKET_MAX_COUNT) {
            DELAY_DELIMETER_PACKET_COUNT = 0;
            try {
                Thread.sleep(DELAY_TIME);
            }catch (InterruptedException e) {}
        }
    }
	
	private void printHex( byte[] array ) {
//		for ( int i=0; i<array.length; ++i )
//			System.out.print( String.format( "%02X ", array[i] ) );
//		System.out.println();
	}

    // escape 처리를 겸함
	private void setBytes( List< Byte > list, byte[] input ) {
		for ( int i=0; i<input.length; ++i ) {
			if ( input [ i ] == 0xc0 || input [ i ] == 0xc1 || input [ i ] == 0x7d ) {
				list.add( ( byte )0x7d );
				list.add( ( byte )(input[ i ] ^ 0x20) );
			}
			else
				list.add( input[ i ] );
		}
	}
	
	private byte[] shortToBytes( short x ) {
	    ByteBuffer buffer = ByteBuffer.allocate( Short.SIZE >> 3 );
        buffer.order( ByteOrder.LITTLE_ENDIAN );
	    buffer.putShort( x );
	    return buffer.array();
	}
	
	private byte[] intToBytes( int x ) {
	    ByteBuffer buffer = ByteBuffer.allocate( Integer.SIZE >> 3 );
        buffer.order( ByteOrder.LITTLE_ENDIAN );
	    buffer.putInt( x );
	    return buffer.array();
	}
	
	private byte[] longToBytes( long x ) {
	    ByteBuffer buffer = ByteBuffer.allocate( Long.SIZE >> 3 );
        buffer.order( ByteOrder.LITTLE_ENDIAN );
	    buffer.putLong( x );
	    return buffer.array();
	}
	
	private byte[] GetSectionOwnerByte( int section, int owner ) {
        byte[] ownerByte = new byte[ 4 ];
        ownerByte[ 0 ] = ( byte )( owner & 0xff );
        ownerByte[ 1 ] = ( byte )((owner >> 8 ) & 0xff );
        ownerByte[ 2 ] = ( byte )((owner >> 16 ) & 0xff );
        ownerByte[ 3 ] = ( byte )section;

        return ownerByte;
    }

    private long GetLongValue( byte[] array, int startIndex )
    {
//        long sum = array[startIndex + 8 - 1];
//        for (int i = 2; i <= 8; ++i)
//            sum |= (long)(array[startIndex + 8 - i] << (8 * (i-1)));
//        return sum;

        return ByteBuffer.wrap(array, startIndex, 8).getLong();
    }

    private int GetIntValue( byte[] array, int startIndex )
    {
//        int sum = array[startIndex + 4 - 1];
//        for (int i = 2; i <= 4; ++i)
//            sum |= (array[startIndex + 4 - i] << (8 * (i-1)));
//        return sum;

        return ByteBuffer.wrap(array, startIndex, 4).getInt();
    }

    private short GetShortValue( byte[] array, int startIndex )
    {
//        short sum = array[startIndex + 2 - 1];
//        for (int i = 2; i <= 2; ++i)
//            sum |= (short)(array[startIndex + 2 - i] << (8 * (i-1)));
//        return sum;

        return ByteBuffer.wrap(array, startIndex, 2).getShort();
    }
    
	private byte[] getBytesFromFile( File file )
	{
		DataInputStream dis = null;
		byte data[] = null;
		
        try {
            dis = new DataInputStream( new BufferedInputStream( new FileInputStream( file ) ) );
            data = new byte[dis.available()];
            dis.read(data);
        } catch ( FileNotFoundException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        } finally {
            if ( dis != null ) try { dis.close(); } catch ( IOException e ) {}
        }
		
		return data;
	}

	// 1 ~ 100 사이의 랜덤
	private int random() {
        return (int) (Math.random() * 100 + 1);
    }
}
