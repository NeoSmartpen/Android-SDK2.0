package kr.neolab.sdk.ink.structure;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;

/**
 * The class of information is stored to the dot
 *
 * @author CHY
 */
public class Dot implements Parcelable
{

    /**
     * The X.
     */
    public float x = 0, /**
 * The Y.
 */
y = 0;
    /**
     * The Dot type.
     */
    public int dotType = 0;
    /**
     * The Pressure.
     */
    public int pressure = 0;
    /**
     * The Timestamp.
     */
    public long timestamp = 0;
    /**
     * The Tilt x.
     */
    public int tiltX = 0, /**
 * The Tilt y.
 */
tiltY = 0, /**
 * The Twist.
 */
twist = 0;

    /**
     * The Section id.
     */
    public int sectionId = 0, /**
 * The Owner id.
 */
ownerId = 0, /**
 * The Note id.
 */
noteId = 0, /**
 * The Page id.
 */
pageId = 0, /**
 * The Color.
 */
color = Color.BLACK;
    /**
     * The Pen tip type.
     */
    public int penTipType = Stroke.PEN_TIP_TYPE_NORMAL;

    //[2018.03.05] Stroke Test
    public int dotCount = -1;
    public int totalImgCount = -1;
    public int processImgCount = -1;
    public int successImgCount = -1;
    public int sendImgCount = -1;

    public String macAddress = null;

    private Dot ()
    {

    }

    /**
     * Instantiates a new Dot.
     *
     * @param dot the dot
     */
    public Dot ( Dot dot )
    {
        this.x = dot.x;
        this.y = dot.y;
        this.pressure = dot.pressure;
        this.dotType = dot.dotType;
        this.timestamp = dot.timestamp;

        this.sectionId = dot.sectionId;
        this.ownerId = dot.ownerId;
        this.noteId = dot.noteId;
        this.pageId = dot.pageId;
        this.color = dot.color;
        this.penTipType = dot.penTipType;
        this.tiltX = dot.tiltX;
        this.tiltY = dot.tiltY;
        this.twist = dot.twist;

        //[2018.03.05] Stroke Test
        this.dotCount = dot.dotCount;
        this.totalImgCount = dot.totalImgCount;
        this.processImgCount = dot.processImgCount;
        this.successImgCount = dot.successImgCount;
        this.sendImgCount = dot.sendImgCount;
        if(dot.macAddress != null)
            this.macAddress = dot.macAddress;
    }

    /**
     * A constructor that constructs a Dot object
     *
     * @param x          x-coordinate of dot
     * @param y          y-coordinate of dot
     * @param pressure   level of pressure
     * @param dotType    type of dot
     * @param timestamp  generated time
     * @param sectionId  the section id
     * @param ownerId    the owner id
     * @param noteId     the note id
     * @param pageId     the page id
     * @param color      the color
     * @param penTipType the pen tip type
     * @param tiltX      the tilt x
     * @param tiltY      the tilt y
     * @param twist      the twist
     */
    public Dot ( float x, float y, int pressure, int dotType, long timestamp, int sectionId, int ownerId, int noteId, int pageId, int color, int penTipType, int tiltX, int tiltY, int twist )
    {
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.dotType = dotType;
        this.timestamp = timestamp;

        this.sectionId = sectionId;
        this.ownerId = ownerId;
        this.noteId = noteId;
        this.pageId = pageId;
        this.color = color;
        this.penTipType = penTipType;
        this.tiltX = tiltX;
        this.tiltY = tiltY;
        this.twist = twist;
    }

    /**
     * A constructor that constructs a Dot object
     *
     * @param x          x-coordinate of dot
     * @param y          y-coordinate of dot
     * @param pressure   level of pressure
     * @param dotType    type of dot
     * @param timestamp  generated time
     * @param sectionId  the section id
     * @param ownerId    the owner id
     * @param noteId     the note id
     * @param pageId     the page id
     * @param color      the color
     * @param penTipType the pen tip type
     * @param tiltX      the tilt x
     * @param tiltY      the tilt y
     * @param twist      the twist
     */
    public Dot ( float x, float y, int pressure, int dotType, long timestamp, int sectionId, int ownerId, int noteId, int pageId, int color, int penTipType, int tiltX, int tiltY, int twist, int dotCount, int totalImgCount, int processImgCount, int successImgCount, int sendImgCount )
    {
        this.x = x;
        this.y = y;
        this.pressure = pressure;
        this.dotType = dotType;
        this.timestamp = timestamp;

        this.sectionId = sectionId;
        this.ownerId = ownerId;
        this.noteId = noteId;
        this.pageId = pageId;
        this.color = color;
        this.penTipType = penTipType;
        this.tiltX = tiltX;
        this.tiltY = tiltY;
        this.twist = twist;

        //[2018.03.05] Stroke Test
        this.dotCount = dotCount;
        this.totalImgCount = totalImgCount;
        this.processImgCount = processImgCount;
        this.successImgCount = successImgCount;
        this.sendImgCount = sendImgCount;
    }

    /**
     * A constructor that constructs a Dot object
     *
     * @param x          x-coordinate of dot
     * @param y          y-coordinate of dot
     * @param fx         below decimal point of x-coordinate
     * @param fy         below decimal point of y-coordinate
     * @param pressure   level of pressure
     * @param dotType    type of dot
     * @param timestamp  generated time
     * @param sectionId  the section id
     * @param ownerId    the owner id
     * @param noteId     the note id
     * @param pageId     the page id
     * @param color      the color
     * @param penTipType the pen tip type
     * @param tiltX      the tilt x
     * @param tiltY      the tilt y
     * @param twist      the twist
     */
    public Dot ( int x, int y, int fx, int fy, int pressure, int dotType, long timestamp, int sectionId, int ownerId, int noteId, int pageId, int color, int penTipType, int tiltX, int tiltY, int twist )
    {
        this( ( x + (float) ( fx * 0.01 ) ), ( y + (float) ( fy * 0.01 ) ), pressure, dotType, timestamp, sectionId, ownerId, noteId, pageId, color, penTipType, tiltX, tiltY, twist );
    }

    /**
     * Returns the x coordinate of the dot
     *
     * @return x x
     */
    public float getX ()
    {
        return x;
    }

    /**
     * Returns the y coordinate of the dot
     *
     * @return y
     */
    public float getY ()
    {
        return y;
    }

    /**
     * Returns the type of the dot
     *
     * @return dotType dot type
     */
    public int getDotType ()
    {
        return dotType;
    }

    /**
     * Returns the pressure of the dot
     *
     * @return pressure pressure
     */
    public int getPressure ()
    {
        return pressure;
    }

    /**
     * Returns the timestamp of the dot
     *
     * @return timestamp timestamp
     */
    public long getTimestamp ()
    {
        return timestamp;
    }

    @Override
    public int describeContents ()
    {
        return 0;
    }

    @Override
    public void writeToParcel ( Parcel parcel, int i )
    {
        parcel.writeFloat( x );
        parcel.writeFloat( y );
        parcel.writeInt( dotType );
        parcel.writeInt( pressure );
        parcel.writeLong( timestamp );
        parcel.writeInt( tiltX );
        parcel.writeInt( tiltY );
        parcel.writeInt( twist );

        parcel.writeInt( sectionId );
        parcel.writeInt( ownerId );
        parcel.writeInt( noteId );
        parcel.writeInt( pageId );
        parcel.writeInt( color );
        parcel.writeInt( penTipType );

        //[2018.03.05] Stroke Test
        parcel.writeInt( dotCount );
        parcel.writeInt( totalImgCount );
        parcel.writeInt( processImgCount );
        parcel.writeInt( successImgCount );
        parcel.writeInt( sendImgCount );
        parcel.writeString( macAddress );

    }

    /**
     * The constant CREATOR.
     */
    public static final Parcelable.Creator<Dot> CREATOR = new Parcelable.Creator<Dot>()
    {

        @Override
        public Dot createFromParcel ( Parcel source )
        {

            Dot oDot = new Dot();
            oDot.x = source.readFloat();
            oDot.y = source.readFloat();
            oDot.dotType = source.readInt();
            oDot.pressure = source.readInt();
            oDot.timestamp = source.readLong();
            oDot.tiltX = source.readInt();
            oDot.tiltY = source.readInt();
            oDot.twist = source.readInt();
            oDot.sectionId = source.readInt();
            oDot.ownerId = source.readInt();
            oDot.noteId = source.readInt();
            oDot.pageId = source.readInt();
            oDot.color = source.readInt();
            oDot.penTipType = source.readInt();

            //[2018.03.05] Stroke Test
            oDot.dotCount = source.readInt();
            oDot.totalImgCount = source.readInt();
            oDot.processImgCount = source.readInt();
            oDot.successImgCount = source.readInt();
            oDot.sendImgCount = source.readInt();
            oDot.macAddress = source.readString();

            return oDot;
        }

        @Override
        public Dot[] newArray ( int size )
        {
            return new Dot[size];
        }
    };

    /**
     * To byte array byte [ ].
     *
     * @param DOT_DATA_BYTE_ALIGN the dot data byte align
     * @return the byte [ ]
     */
    public byte[] toByteArray ( int DOT_DATA_BYTE_ALIGN )
    {
        ByteBuffer buffer = ByteBuffer.allocate( DOT_DATA_BYTE_ALIGN );

        byte[] ret = null;
        if ( DOT_DATA_BYTE_ALIGN == 16 )
        {
            buffer.put( (byte) dotType );
            buffer.putShort( (short) x );
            buffer.putShort( (short) y );
            buffer.put( (byte) ( (int) ( ( x - (int) x ) * 100 ) ) );
            buffer.put( (byte) ( (int) ( ( y - (int) y ) * 100 ) ) );
            buffer.put( (byte) pressure );
            buffer.putLong( timestamp );
            ret = buffer.array();
        }
        else
        {
            buffer.put( (byte) dotType );
            buffer.putShort( (short) x );
            buffer.putShort( (short) y );
            buffer.put( (byte) ( (int) ( ( x - (int) x ) * 100 ) ) );
            buffer.put( (byte) ( (int) ( ( y - (int) y ) * 100 ) ) );
            buffer.put( (byte) pressure );
            buffer.putLong( timestamp );

            buffer.put( (byte) tiltX );
            buffer.put( (byte) tiltY );
            buffer.putShort( (short) twist );

            ret = buffer.array();

        }
        return ret;
    }

    /**
     * Make up dot dot.
     *
     * @param dot the dot
     * @return the dot
     */
    public static Dot makeUpDot ( Dot dot )
    {
        Dot upDot = new Dot( dot );
        upDot.dotType = 20;
        return upDot;
    }

    /**
     * Make down dot dot.
     *
     * @param dot the dot
     * @return the dot
     */
    public static Dot makeDownDot ( Dot dot )
    {
        Dot downDot = new Dot( dot );
        downDot.dotType = 17;
        return downDot;
    }

}
