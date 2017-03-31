/**
 * 
 */
package kr.neolab.sdk.pen.bluetooth.lib;

import java.io.IOException;
import java.io.InputStream;

/**
 * binary 파일을 Chunk로 분리
 *
 * @author CHY
 */
public class Chunk
{
    private InputStream istream = null;

    /**
     * The Size.
     */
// chunk size is 16k
    // 실제 연동 후 FW 팀과 조절하기로 함
    public int size = 512;
    private int rows;
    
    private byte[] rBuffer;
    private byte[][] tBuffer;
    private boolean[] status;

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
        rows = (int) Math.ceil(filesize / size) + 1;
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
                // TODO Auto-generated catch block
                e.printStackTrace();
                break;
            }
            
            tBuffer[i++] = rBuffer;
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
