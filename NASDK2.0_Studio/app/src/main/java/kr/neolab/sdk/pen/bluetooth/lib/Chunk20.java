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
 * @author Moo
 */
public class Chunk20 implements IChunk
{
    private InputStream istream = null;

    /**
     * The Size.
     */
    public int size = 0;

    private byte[] rBuffer;

    private int filesize = 0;

    /**
     * Instantiates a new Chunk.
     *
     * @param is         the is
     * @param filesize   the filesize
     * @param packetSize the packet size
     */
    public Chunk20(InputStream is, int filesize, int packetSize)
    {
        size = packetSize;
        istream = is;
        this.filesize = filesize;
        NLog.d("Chunk packetSize="+packetSize+",filesize="+filesize);
        rBuffer = new byte[filesize];
    }

    /**
     * Load.
     */
    public void load()
    {
        try
        {
            istream.read(rBuffer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
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

    public int getFilesize() {
        return filesize;
    }


    public byte[] getChunk(int offset)
    {
        if(offset >= filesize)
            return null;
        byte[] chunk = null;
        int packetSize = 0;
        if(offset + size > filesize)
            packetSize = filesize - offset;
        else
            packetSize = size;

        chunk = Packet.copyOfRange(rBuffer, offset, packetSize);
        return chunk;
    }


    /**
     * Calc checksum byte.
     *
     * @return the byte
     */
    private byte calcChecksum()
    {
        int CheckSum = 0;

        for( int i = 0; i < rBuffer.length; i++)
        {
            CheckSum += (int)(rBuffer[i] & 0xFF);
        }

        return (byte)CheckSum;
    }
    public byte getChecksum()
    {
        return calcChecksum();
    }


}
