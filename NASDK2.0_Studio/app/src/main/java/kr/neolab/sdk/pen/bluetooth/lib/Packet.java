package kr.neolab.sdk.pen.bluetooth.lib;

import java.util.Arrays;

/**
 * The type Packet.
 *
 * @author CHY
 */
public class Packet
{
    private static final int PKT_ESCAPE = 0xC0;

    /**
     * The Cmd.
     */
    public int cmd;

    /**
     * The Data length.
     */
    public int dataLength;

    /**
     * The Data.
     */
    public byte[] data;

    /**
     * The Check sum.
     */
    public int checkSum;
    /**
     * The Is event.
     */
    public boolean isEvent = false;

    /**
     * The Result code.
     */
    public byte resultCode = ProtocolParser20.PKT_RESULT_SUCCESS;
    private int protocolVer = 1;

    /**
     * Gets dot code.
     *
     * @return the dot code
     */
    public int getDotCode ()
	{
		return dotCode;
	}

    /**
     * Sets dot code.
     *
     * @param dotCode the dot code
     */
    public void setDotCode ( int dotCode )
	{
		this.dotCode = dotCode;
	}

	private int dotCode = 1;

    /**
     * Instantiates a new Packet.
     */
    public Packet()
	{
	}

    /**
     * Instantiates a new Packet.
     *
     * @param buffer the buffer
     */
    public Packet(byte[] buffer)
    {
        this.setValue(buffer);
    }

    /**
     * Instantiates a new Packet.
     *
     * @param buffer      the buffer
     * @param protocolVer the protocol ver
     */
    public Packet(byte[] buffer, int protocolVer)
    {
        this.protocolVer = protocolVer;
        this.setValue(buffer);
    }

    /**
     * Instantiates a new Packet.
     *
     * @param buffer      the buffer
     * @param protocolVer the protocol ver
     * @param isEvent     the is event
     */
    public Packet(byte[] buffer, int protocolVer, boolean isEvent)
    {
        this.protocolVer = protocolVer;
        this.isEvent = isEvent;
        this.setValue(buffer);
    }

    private void setValue(byte[] buffer) 
    {
        this.cmd = ByteConverter.byteArrayToInt( new byte[]{ buffer[0] } );
        if(protocolVer == 2)
        {
            if(isEvent)
            {
                this.dataLength = ByteConverter.byteArrayToInt( new byte[]{ buffer[1], buffer[2] } );
                this.data = Packet.copyOfRange(buffer, 3, dataLength);
            }
            else
            {
                this.resultCode =  buffer[1];
                this.dataLength = ByteConverter.byteArrayToInt(new byte[]{ buffer[2], buffer[3] });
                this.data = Packet.copyOfRange(buffer, 4, dataLength);
            }
        }
        else
        {
            this.dataLength = ByteConverter.byteArrayToInt( new byte[]{ buffer[1], buffer[2] } );
            this.data = Packet.copyOfRange(buffer, 3, dataLength);
        }

    }

    /**
     * Gets result code.
     *
     * @return the result code
     */
    public byte getResultCode()
    {
        return this.resultCode;
    }

    /**
     * Gets cmd.
     *
     * @return the cmd
     */
    public int getCmd()
    {
        return this.cmd;
    }

    /**
     * Gets data length.
     *
     * @return the data length
     */
    public int getDataLength()
    {
        return this.dataLength;
    }

    /**
     * Gets data range int.
     *
     * @param start the start
     * @param size  the size
     * @return the data range int
     */
    public int getDataRangeInt(int start, int size)
    {
        byte[] range = Packet.copyOfRange(data, start, size);
        return ByteConverter.byteArrayToInt(range);
    }

    /**
     * Gets data range short.
     *
     * @param start the start
     * @param size  the size
     * @return the data range short
     */
    public short getDataRangeShort(int start, int size)
    {
        byte[] range = Packet.copyOfRange(data, start, size);
        return ByteConverter.byteArrayToShort(range);
    }

    /**
     * Gets data range long.
     *
     * @param start the start
     * @param size  the size
     * @return the data range long
     */
    public long getDataRangeLong(int start, int size)
    {
        byte[] range = Packet.copyOfRange(data, start, size);
        return ByteConverter.byteArrayToLong(range);
    }

    /**
     * Gets data range string.
     *
     * @param start the start
     * @param size  the size
     * @return the data range string
     */
    public String getDataRangeString(int start, int size)
    {
        byte[] range = Packet.copyOfRange(data, start, size);
        return new String(range);
    }

    /**
     * Get data range byte [ ].
     *
     * @param start the start
     * @param size  the size
     * @return the byte [ ]
     */
    public byte[] getDataRange(int start, int size)
    {
        return Packet.copyOfRange(data, start, size);
    }

    /**
     * Copy of range byte [ ].
     *
     * @param buffer the buffer
     * @param start  the start
     * @param size   the size
     * @return the byte [ ]
     */
    public static byte[] copyOfRange(byte[] buffer, int start, int size)
    {
    	return Arrays.copyOfRange(buffer, start, start + size);
    }

    /**
     * Get data byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getData()
    {
        return this.data;
    }
}
