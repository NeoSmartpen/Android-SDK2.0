/**
 * 
 */
package kr.neolab.sdk.pen.bluetooth.lib;

import java.io.IOException;
import java.io.InputStream;

import kr.neolab.sdk.util.NLog;

/**
 * Separate binary files as chunks
 *
 * @author CHY
 */
public class Chunk implements IChunk
{
    private InputStream istream = null;

    /**
     * The Size.
     */
    public int size = 512;
    private int rows;
    
    private byte[] rBuffer;
    private byte[][] tBuffer;
    private boolean[] status;

    private long filesize = 0;

    /**
     * Instantiates a new Chunk.
     *
     * @param is         the is
     * @param filesize   the filesize
     * @param packetSize the packet size
     */
    public Chunk(InputStream is, long filesize, int packetSize)
    {
        size = packetSize;
        istream = is;
        this.filesize = filesize;
        rows = (int) (Math.ceil((double) filesize / (double)size));
        NLog.d("Chunk packetSize="+packetSize+",rows="+rows);
        rBuffer = new byte[size];
        tBuffer = new byte[rows][size];
        status  = new boolean[rows];
    }

    /**
     * Instantiates a new Chunk.
     *
     * @param is       the is
     * @param filesize the filesize
     */
    public Chunk(InputStream is, long filesize)
    {
        istream = is;
        rows = (int) Math.ceil(filesize / size) + 1;
        rBuffer = new byte[size];
        tBuffer = new byte[rows][size];
        status  = new boolean[rows];
    }

    /**
     * Load.
     */
    public void load()
    {
        int i = 0;
        
        while (true)
        {
            rBuffer = new byte[size];
            
            try 
            {
                int number = istream.read(rBuffer);
                if (number <= -1) 
                    break;
            }
            catch (IOException e) 
            {
                e.printStackTrace();
                break;
            }
            
            tBuffer[i] = rBuffer;
            i++;
        }
    }

    /**
     * Get chunk byte [ ].
     *
     * @param number the number
     * @return the byte [ ]
     */
    public byte[] getChunk(int number)
    {
        return (number >= 0 && tBuffer.length > number) ? tBuffer[number] : null;
    }

    /**
     * Offset to index int.
     *
     * @param offset the offset
     * @return the int
     */
    public int offsetToIndex(int offset)
    {

        return offset/size < tBuffer.length ? offset/size : -1 ;
    }

    /**
     * Gets chunk length.
     *
     * @return the chunk length
     */
    public int getChunkLength()
    {
        return rows;
    }

    /**
     * Gets chunksize.
     *
     * @return the chunksize
     */
    public int getChunksize()
    {
        return size;
    }

    /**
     * Gets checksum.
     *
     * @param number the number
     * @return the checksum
     */
    public byte getChecksum(int number)
    {
        return tBuffer.length > number ? calcChecksum(tBuffer[number]) : null;
    }

    /**
     * Gets checksum.
     *
     * @return the checksum
     */
    public byte getChecksum()
    {
        return calcChecksum(tBuffer);
    }

    /**
     * Calc checksum byte.
     *
     * @param bytes the bytes
     * @return the byte
     */
    public static byte calcChecksum(byte[] bytes)
    {
        int CheckSum = 0;
        
        for( int i = 0; i < bytes.length; i++)
        {
             CheckSum += (int)(bytes[i] & 0xFF);
        }

        return (byte)CheckSum;
    }

    /**
     * Calc checksum byte.
     *
     * @param bytes the bytes
     * @return the byte
     */
    public byte calcChecksum(byte[][] bytes)
    {
        int CheckSum = 0;

        for( int i = 0; i < bytes.length; i++)
        {
            for( int j = 0; j < bytes[i].length; j++)
            {
                CheckSum += (int)(bytes[i][j] & 0xFF);
            }
        }

        return (byte)CheckSum;
    }

    /**
     * Gets status.
     *
     * @return the status
     */
    public int getStatus()
    {
        int result = 0;
        
        for (boolean st : status) 
        {
            result += st ? 1 : 0;
        }
        
        return result;
    }

    /**
     * Gets status percent.
     *
     * @return the status percent
     */
    public int getStatusPercent()
    {         
        double status = (double)((double)getStatus() / (double)rows) * 100;

        return (int)status;
    }

    /**
     * Sets status.
     *
     * @param index  the index
     * @param status the status
     */
    public void setStatus(int index, boolean status)
    {
        this.status[index] = status;
    }
}
