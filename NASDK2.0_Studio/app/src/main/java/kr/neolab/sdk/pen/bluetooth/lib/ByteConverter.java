package kr.neolab.sdk.pen.bluetooth.lib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The type Byte converter.
 *
 * @author A
 */
public class ByteConverter
{
    /**
     * byte[] to hex String
     *
     * @param array array
     * @param size  the size
     * @return String string
     */
    public static String byteArrayToHexString(byte[] array,int size)
    {
        StringBuffer hexString = new StringBuffer();
        
        for (int i=0;i<size;i++) 
        {
            int intVal = array[i] & 0xff;
            
            if (intVal < 0x10)
            {
                hexString.append("0");
            }
            
            hexString.append(Integer.toHexString(intVal));
        }
        
        return hexString.toString();    
    }

    /**
     * byte[] to int
     *
     * @param bytes array must is 1 ~ 4 bytes. little endian
     * @return int value
     */
    public static int byteArrayToInt(byte[] bytes)
    {
        int newValue = 0;
        
        switch(bytes.length) 
        {
                case 1:
                    newValue |= ((int)bytes[0]) & 0xFF;
                    break;
                case 2:
                    newValue |= (((int)bytes[1]) << 8) & 0xFF00;
                    newValue |= ((int)bytes[0]) & 0xFF;
                    break;
                case 3:
                    newValue |= (((int)bytes[2]) << 16) & 0xFF0000;
                    newValue |= (((int)bytes[1]) << 8) & 0xFF00;
                    newValue |= ((int)bytes[0]) & 0xFF;
                    break;
                case 4:
                    newValue |= (((int)bytes[3]) << 24) & 0xFF000000;
                    newValue |= (((int)bytes[2]) << 16) & 0xFF0000;
                    newValue |= (((int)bytes[1]) << 8) & 0xFF00;
                    newValue |= ((int)bytes[0]) & 0xFF;
        }
        
        return newValue;
    }

    /**
     * byte[] to long
     *
     * @param bytes array must is 1 ~ 8 bytes. little endian
     * @return Long value
     */
    public static long byteArrayToLong(byte[] bytes)
    {
        Long newValue=0l;
        ByteBuffer temp = ByteBuffer.wrap(bytes);
        temp.order(ByteOrder.LITTLE_ENDIAN);
        newValue = temp.getLong();
        
        return newValue;
    }

    /**
     * byte[] to short
     *
     * @param bytes array must is 1 ~ 2 bytes. little endian
     * @return short value
     */
    public static short byteArrayToShort(byte[] bytes)
    {
        Short newValue;
        ByteBuffer temp = ByteBuffer.wrap(bytes);
        temp.order(ByteOrder.LITTLE_ENDIAN);
        newValue = temp.getShort();
        
        return newValue;
    }

    /**
     * int to byte[]
     *
     * @param value the value
     * @return byte array
     */
/*
    public static byte[] intTobyte(int value) {
        
        byte[] bytes=new byte[4];
        bytes[0]=(byte)((value&0xFF000000)>>24);
        bytes[1]=(byte)((value&0x00FF0000)>>16);
        bytes[2]=(byte)((value&0x0000FF00)>>8);
        bytes[3]=(byte) (value&0x000000FF);
        
        return bytes;
    }
    */
    public static byte[] intTobyte(int value) 
    { 
        byte[] byteArray = ByteBuffer.allocate(4).putInt(value).array();
        byte[] returnbyte = new byte[4];
        
        for(int i=0;i<4;i++)
        {
            returnbyte[i]=byteArray[3-i];
        }
        
        return returnbyte;
    }

    /**
     * long to byte[]
     *
     * @param value the value
     * @return byte array
     */
    public static byte[] longTobyte(long value)
    {
        byte[] byteArray = ByteBuffer.allocate(8).putLong(value).array();
        byte[] returnbyte = new byte[8];
        
        for(int i=0;i<8;i++)
        {
            returnbyte[i]=byteArray[7-i];
        }
        
        return returnbyte;
    }

    /**
     * short to byte[]
     *
     * @param value the value
     * @return byte [ ]
     */
    public static byte[] shortTobyte(short value)
    {
        byte[] byteArray = ByteBuffer.allocate(2).putShort(value).array();
        byte[] returnbyte = new byte[2];
        
        for(int i=0;i<2;i++)
        {
            returnbyte[i]=byteArray[1-i];
        }
        
        return returnbyte;
    }

    /**
     * Char array tobyte byte [ ].
     *
     * @param values the values
     * @return the byte [ ]
     */
    public static byte[] charArrayTobyte(char[] values)
    {
    	int byteLength = values.length * 2;
    	
    	ByteBuffer bf = ByteBuffer.allocate( byteLength );
        
    	for ( char v : values )
    	{
    		bf.putChar( v );
    	}
    	
    	byte[] byteArray = bf.array();
    	byte[] returnbyte = new byte[byteLength];
    	
        for(int i=0;i < byteLength; i++)
        {
            returnbyte[i] = byteArray[(byteLength-1)-i];
        }
        
        return returnbyte;
    }

    /**
     * String tobyte byte [ ].
     *
     * @param value the value
     * @return the byte [ ]
     */
    public static byte[] stringTobyte(String value)
    {
        ByteBuffer temp = ByteBuffer.wrap(value.getBytes());
        temp.order(ByteOrder.LITTLE_ENDIAN);
        return temp.array();
    }
}
