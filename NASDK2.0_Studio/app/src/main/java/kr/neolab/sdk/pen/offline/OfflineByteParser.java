package kr.neolab.sdk.pen.offline;

import android.graphics.Color;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

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
 * Created by Moo on 2016-02-16.
 */
public class OfflineByteParser implements IFilterListener
{
    private FilterForPaper filterPaper;
    private FilterForFilm filterFilm;

    private ArrayList<Stroke> strokes = new ArrayList<Stroke>();
    private Stroke stroke = null;

    /**
     * The Buffer.
     */
    byte[] buffer = null;
    /**
     * The Data.
     */
    byte[] data = null;
    private int sectionId, ownerId, noteId, pageId;
    private int strokeCount, sizeAfterCompress, sizeBeforeCompress;
    private boolean isCompressed = false;

    private final static int STROKE_HEADER_LENGTH = 27;
    private final static int BYTE_DOT_SIZE = 16;
    private float[] factor = null;
    private int maxPress = 0;

    /**
     * Instantiates a new Offline byte parser.
     *
     * @param buffer the buffer
     */
    public OfflineByteParser(byte[] buffer, int maxPressValue)
    {
        this.filterPaper = new FilterForPaper( this );
        this.filterFilm = new FilterForFilm( this );
        this.buffer = buffer;
        this.maxPress = maxPressValue;
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
     * parse loaded file
     *
     * @return array list
     * @throws Exception the exception
     */
    public ArrayList<Stroke> parse() throws Exception
    {
        NLog.i( "[OfflineByteParser] process start" );

        stroke = null;
        strokes.clear();


       // Header parsing
        NLog.i( "[OfflineByteParser] process parseHeader" );
        this.parseHeader();

        if(sizeBeforeCompress == 0 && sizeAfterCompress == 0)
            return strokes;

        // Merge files
        NLog.i( "[OfflineByteParser] process loadData" );
        this.loadData();


        // Body parsing
        NLog.i( "[OfflineByteParser] process parseBody" );
        this.parseBody();

        NLog.i( "[OfflineByteParser] process finished" );

        data = null;

//        if ( strokes == null || strokes.size() <= 0 )
//        {
//            return null;
//        }
//        else
//        {
            return strokes;
//        }
    }

    private void loadData() throws Exception
    {
//        try
//        {
            NLog.d( "[OfflineByteParser] isCompressed="+isCompressed +";sizeAfterCompress="+sizeAfterCompress+"sizeBeforeCompress="+sizeBeforeCompress);
            //buffer is missing SOF / EOF / CMD / LENGTH.
            if(isCompressed)
            {

                data = unzip(  Packet.copyOfRange(buffer, 21 -3, sizeAfterCompress),sizeBeforeCompress );
                NLog.d( "[OfflineByteParser] deCompressed length="+data.length);
            }
            else
            {
                data = Arrays.copyOfRange(buffer, 21, 21+ sizeBeforeCompress-1);
            }
//        }
//        catch ( Exception e )
//        {
//            e.printStackTrace();
//        }
    }

    private void parseHeader() throws Exception
    {
        this.isCompressed = ByteConverter.byteArrayToInt( Packet.copyOfRange( buffer, 2, 1 )) == 1 ? true : false;
        this.sizeBeforeCompress =  ByteConverter.byteArrayToInt( Packet.copyOfRange(buffer, 3, 2 ));
        this.sizeAfterCompress =  ByteConverter.byteArrayToInt( Packet.copyOfRange( buffer, 5, 2 ) );

        byte[] rxb = Packet.copyOfRange( buffer, 8, 4 );
        this.sectionId = (int) ( rxb[3] & 0xFF );
        this.ownerId = ByteConverter.byteArrayToInt( new byte[]{ rxb[0], rxb[1], rxb[2], (byte) 0x00 } );
        this.noteId = ByteConverter.byteArrayToInt( Packet.copyOfRange( buffer, 12, 4 ));
        this.strokeCount = ByteConverter.byteArrayToInt( Packet.copyOfRange( buffer, 16, 2 ) );
        NLog.i( "[OfflineByteParser] sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId + ", pageId : " + pageId + ", isCompressed : " + isCompressed + ", sizeBeforeCompress : " + sizeBeforeCompress + ", sizeAfterCompress : " + sizeAfterCompress + ", strokeCount : " + strokeCount );
    }

    private void parseBody() throws Exception
    {
        NLog.d( "[OfflineByteParser] parse file strokeCount="+strokeCount );
        long prevTimestamp = 0;

        int lhDotTotal = 0;
        byte lhCheckSum = 0;

        //The dot of the current line
        ArrayList<Fdot> tempDots = new ArrayList<Fdot>();
        int strokeIndex = 0;
        int checksumFailCount = 0;
        OUTER_STROKE_LOOP: for(int i = 0; i < strokeCount; i++)
        {
            int pageId =  ByteConverter.byteArrayToInt( Packet.copyOfRange( data, 0 + strokeIndex, 4 ) );
            long lhPenDnTime =  ByteConverter.byteArrayToLong( Packet.copyOfRange( data, 4 + strokeIndex, 8 ) );

//            lhPenDnTime =TimeUtil.convertUTCToLocalTime( lhPenDnTime );
            long lhPenUpTime =  ByteConverter.byteArrayToLong( Packet.copyOfRange( data, 12 + strokeIndex, 8 ) );
//            lhPenDnTime =TimeUtil.convertUTCToLocalTime( lhPenUpTime );
            int lhPenTipType =  ByteConverter.byteArrayToInt( Packet.copyOfRange( data, 20 + strokeIndex, 1 ) );
            int lhPenTipColor = Color.BLACK;

            byte[] cbyte =Packet.copyOfRange( data, 21 + strokeIndex, 4 );
            NLog.d( "a : " + Integer.toHexString( (int) ( cbyte[3] & 0xFF ) ) );
            NLog.d( "r : " + Integer.toHexString( (int) ( cbyte[2] & 0xFF ) ) );
            NLog.d( "g : " + Integer.toHexString( (int) ( cbyte[1] & 0xFF ) ) );
            NLog.d( "b : " + Integer.toHexString( (int) ( cbyte[0] & 0xFF ) ) );
            lhPenTipColor = ByteConverter.byteArrayToInt( new byte[]{ cbyte[0], cbyte[1], cbyte[2], cbyte[3] } );
            int dotCount =  ByteConverter.byteArrayToInt( Packet.copyOfRange( data, 25 + strokeIndex, 2 ) );
            NLog.d( "dotCount : " + dotCount );

            // Firmware Lee's request does not cause an error even if there is no dot
            if(dotCount == 0)
            {
                stroke = new Stroke( sectionId, ownerId, noteId, pageId, lhPenTipColor );
                strokes.add( stroke );
            }

            strokeIndex += STROKE_HEADER_LENGTH;
            int dotIndex = 0;
            tempDots = new ArrayList<Fdot>();
            for(int j = 0; j < dotCount; j++)
            {
                int sectionId = this.sectionId;
                int ownerId = this.ownerId;
                int noteId = this.noteId;

                long time = (int) (data[strokeIndex + dotIndex + 0] & 0xFF);
                int pressure =  ByteConverter.byteArrayToInt( Packet.copyOfRange( data, strokeIndex + dotIndex + 1, 2 ) );
                int x = ByteConverter.byteArrayToShort( Packet.copyOfRange( data, strokeIndex + dotIndex + 3, 2 ) );
                int y = ByteConverter.byteArrayToShort( Packet.copyOfRange( data, strokeIndex + dotIndex + 5, 2 ) );

                int fx = (int) (data[strokeIndex + dotIndex + 7] & 0xFF);
                int fy = (int) (data[strokeIndex + dotIndex + 8] & 0xFF);

                int tiltX = (int) (data[strokeIndex + dotIndex + 9] & 0xFF);
                int tiltY = (int) (data[strokeIndex + dotIndex + 10] & 0xFF);
                int twist = ByteConverter.byteArrayToShort( Packet.copyOfRange( data, strokeIndex + dotIndex + 11, 2 ) );
                if (strokeIndex + dotIndex + 16 > data.length) {
                    NLog.e("[OfflineByteParser] insufficient bytes for lhCheckSum: need "
                            + (strokeIndex + dotIndex + 16) + " but len=" + data.length);
                    break OUTER_STROKE_LOOP;
                }
                lhCheckSum = data[strokeIndex + dotIndex + 15];
                int color = lhPenTipColor;

                boolean isPenUp = false;

                int dotType;
                long timestamp;
                if ( j == 0 )
                {
                    dotType = DotType.PEN_ACTION_DOWN.getValue();
                    timestamp = lhPenDnTime + time;
                    prevTimestamp = timestamp;
                }
                else if ( j == dotCount - 1 )
                {
                    dotType = DotType.PEN_ACTION_UP.getValue();
                    timestamp = lhPenUpTime;
                    isPenUp = true;
                }
                else
                {
                    dotType = DotType.PEN_ACTION_MOVE.getValue();
                    timestamp = prevTimestamp + time;
                    prevTimestamp = timestamp;

                }
                dotIndex += BYTE_DOT_SIZE;

                if(pressure <= 852) {
                    // Down scale from maxPressValue to 256
                    if (maxPress == 0)
                        pressure = pressure / 4;
                    else
                        pressure = (pressure * 255) / maxPress;
                    if (factor != null)
                        pressure = (int) factor[pressure];

                    //If there is only a dot in stroke, make down dot, move dot and up dot.
                    if( dotCount == 1 ) {
                        Fdot downDot = new Fdot((x + (float) (fx * 0.01)), (y + (float) (fy * 0.01)), pressure, DotType.PEN_ACTION_DOWN.getValue(), timestamp, sectionId, ownerId, noteId, pageId, color, lhPenTipType, tiltX, tiltY, twist);
                        Fdot moveDot = new Fdot((x + (float) (fx * 0.01)), (y + (float) (fy * 0.01)), pressure, DotType.PEN_ACTION_MOVE.getValue(), timestamp, sectionId, ownerId, noteId, pageId, color, lhPenTipType, tiltX, tiltY, twist);
                        Fdot upDot = new Fdot((x + (float) (fx * 0.01)), (y + (float) (fy * 0.01)), pressure, DotType.PEN_ACTION_UP.getValue(), timestamp, sectionId, ownerId, noteId, pageId, color, lhPenTipType, tiltX, tiltY, twist);
                        tempDots.add(downDot);
                        tempDots.add(moveDot);
                        tempDots.add(upDot);

                        isPenUp = true;
                    }
                    else {
                        Fdot dot = new Fdot((x + (float) (fx * 0.01)), (y + (float) (fy * 0.01)), pressure, dotType, timestamp, sectionId, ownerId, noteId, pageId, color, lhPenTipType, tiltX, tiltY, twist);
                        tempDots.add(dot);
                    }
                }
                else {
                    NLog.e( "[OfflineByteParser] Dot pressure is greater than 852.This dot will be discarded.  pressure: " + pressure + ", max pressure : " + maxPress );
                }

                if ( isPenUp )
                {
//                    byte dotCalcCs = Chunk.calcChecksum( Packet.copyOfRange( data, strokeIndex, dotCount * BYTE_DOT_SIZE ) );
                    byte dotCalcCs = Chunk.calcChecksum( Packet.copyOfRange( data, strokeIndex +dotIndex - BYTE_DOT_SIZE, BYTE_DOT_SIZE-1) );

                    if ( dotCalcCs == lhCheckSum )
                    {
                        for ( int k = 0; k < tempDots.size(); k++ )
                        {
                            filterDot( tempDots.get( k ) );
                        }
                        NLog.d( "[OfflineByteParser] lhCheckSum Success Stroke cs : " + Integer.toHexString( (int) (lhCheckSum & 0xFF) ) + ", calc : " + Integer.toHexString( (int) (dotCalcCs & 0xFF) ) );
                    }
                    else
                    {
                        NLog.e( "[OfflineByteParser] lhCheckSum Fail Stroke cs : " + Integer.toHexString( (int) (lhCheckSum & 0xFF) ) + ", calc : " + Integer.toHexString( (int) (dotCalcCs & 0xFF) ) );
                        checksumFailCount++;
                    }

                    tempDots = new ArrayList<Fdot>();
                }
            }
            strokeIndex += dotIndex;

        }
        if(checksumFailCount > 3)
            throw new CheckSumException( "lhCheckSum Fail Count="+ checksumFailCount);


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

    /**
     * Unzip byte [ ].
     *
     * @param buffer             the buffer
     * @param sizeBeforeCompress the size before compress
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    public static byte[] unzip ( byte[] buffer, int sizeBeforeCompress) throws IOException
    {
        InflaterInputStream inStream = new InflaterInputStream(new ByteArrayInputStream( buffer));
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int readByte;
                byte[] buf = new byte[1024];
        try {
            while((readByte = inStream.read(buf)) != -1) {
                outStream.write(buf, 0, readByte);
            }
//            readByte = inStream.read(buf,0, sizeBeforeCompress);
//            outStream.write(buf, 0, readByte);
        } catch(Exception e)
        {
            e.printStackTrace();
        }
        byte[] ret = outStream.toByteArray();
        outStream.close();
        return ret;

    }

//    private static byte[] decompress(byte[] bytesToDecompress)
//    {
//        byte[] returnValues = null;
//
//        Inflater inflater = new Inflater();
//
//        int numberOfBytesToDecompress = bytesToDecompress.length;
//
//        inflater.setInput(bytesToDecompress, 0,numberOfBytesToDecompress);
//
//        int bufferSizeInBytes = numberOfBytesToDecompress;
//
//        int numberOfBytesDecompressedSoFar = 0;
//        List<Byte> bytesDecompressedSoFar = new ArrayList<Byte>();
//
//        try
//        {
//            while (inflater.needsInput() == false)
//            {
//                byte[] bytesDecompressedBuffer = new byte[bufferSizeInBytes];
//
//                int numberOfBytesDecompressedThisTime = inflater.inflate(bytesDecompressedBuffer );
//
//                numberOfBytesDecompressedSoFar += numberOfBytesDecompressedThisTime;
//
//                for (int b = 0; b < numberOfBytesDecompressedThisTime; b++)
//                {
//                    bytesDecompressedSoFar.add(bytesDecompressedBuffer[b]);
//                }
//            }
//
//            returnValues = new byte[bytesDecompressedSoFar.size()];
//            for (int b = 0; b < returnValues.length; b++)
//            {
//                returnValues[b] = (byte)(bytesDecompressedSoFar.get(b));
//            }
//
//        }
//        catch (DataFormatException dfe)
//        {
//            dfe.printStackTrace();
//        }
//
//        inflater.end();
//
//        return returnValues;
//    }



    @Override
    public void onFilteredDot ( Fdot dot )
    {
        if ( DotType.isPenActionDown( dot.dotType ) || stroke == null || stroke.isReadOnly() )
        {
            stroke = new Stroke( dot.sectionId, dot.ownerId, dot.noteId, dot.pageId, dot.color );
            strokes.add( stroke );
        }

        stroke.add( dot.toDot() );

    }
}
