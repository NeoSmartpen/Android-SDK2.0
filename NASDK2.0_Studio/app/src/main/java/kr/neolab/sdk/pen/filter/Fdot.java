package kr.neolab.sdk.pen.filter;

import android.os.Parcel;
import android.os.Parcelable;

import java.nio.ByteBuffer;

import kr.neolab.sdk.ink.structure.Dot;

/**
 * The type Fdot.
 */
public class Fdot extends Dot implements Parcelable
{
    /**
     * The Mac address.
     */
    public String mac_address = "";

    /**
     * The Dot data byte align.
     */
    public final int DOT_DATA_BYTE_ALIGN = 46;
    /**
     * The Dot data compact byte align.
     */
    public final int DOT_DATA_COMPACT_BYTE_ALIGN = 18;

//    public int sectionId, ownerId, noteId, pageId, x, y, fx, fy, force, color = Color.BLACK;
//    public long timestamp;

    /**
     * Instantiates a new Fdot.
     *
     * @param x          the x
     * @param y          the y
     * @param pressure   the pressure
     * @param dotType    the dot type
     * @param timestamp  the timestamp
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
    public Fdot(float x, float y, int pressure, int dotType, long timestamp, int sectionId, int ownerId, int noteId, int pageId, int color,int penTipType , int tiltX, int tiltY, int twist)
    {
        super( x, y, pressure, dotType, timestamp, sectionId, ownerId, noteId, pageId, color,penTipType, tiltX, tiltY, twist);
    }

    /**
     * Instantiates a new Fdot.
     *
     * @param x          the x
     * @param y          the y
     * @param pressure   the pressure
     * @param dotType    the dot type
     * @param timestamp  the timestamp
     * @param sectionId  the section id
     * @param ownerId    the owner id
     * @param noteId     the note id
     * @param pageId     the page id
     * @param color      the color
     * @param penTipType the pen tip type
     * @param tiltX      the tilt x
     * @param tiltY      the tilt y
     * @param twist      the twist
     * @param dotCount      the dot count
     * @param totalImgCount      the total imaga count
     * @param processImgCount      the process image count
     * @param successImgCount      the success image count
     * @param sendImgCount      the send image count
     */
    public Fdot(float x, float y, int pressure, int dotType, long timestamp, int sectionId, int ownerId, int noteId, int pageId, int color,int penTipType , int tiltX, int tiltY, int twist, int dotCount, int totalImgCount, int processImgCount, int successImgCount, int sendImgCount)
    {   //[2018.03.05] Stroke Test
        super( x, y, pressure, dotType, timestamp, sectionId, ownerId, noteId, pageId, color,penTipType, tiltX, tiltY, twist, dotCount, totalImgCount, processImgCount, successImgCount, sendImgCount);
    }

//    public void setDot(int dotType, int sectionId, int ownerId, int noteId, int pageId, int x, int y, long time, int fx, int fy, int force, int color)
//    {
//    	this.dotType = dotType;
//        this.sectionId = sectionId;
//        this.ownerId = ownerId;
//        this.noteId = noteId;
//        this.pageId = pageId;
//        this.x = x;
//        this.y = y;
//        this.timestamp = time;
//        this.fx = fx;
//        this.fy = fy;
//        this.force = force;
//        this.color = color;
//    }

    /**
     * Sets dot.
     *
     * @param dotType    the dot type
     * @param sectionId  the section id
     * @param ownerId    the owner id
     * @param noteId     the note id
     * @param pageId     the page id
     * @param x          the x
     * @param y          the y
     * @param time       the time
     * @param fx         the fx
     * @param fy         the fy
     * @param pressure   the pressure
     * @param color      the color
     * @param tiltX      the tilt x
     * @param tiltY      the tilt y
     * @param twist      the twist
     * @param penTipType the pen tip type
     */
    public void setDot(int dotType, int sectionId, int ownerId, int noteId, int pageId, int x, int y, long time, int fx, int fy, int pressure, int color, int tiltX, int tiltY, int twist , int penTipType )
    {
        this.dotType = dotType;
        this.sectionId = sectionId;
        this.ownerId = ownerId;
        this.noteId = noteId;
        this.pageId = pageId;
        this.x = (x + (float) (fx * 0.01));
        this.y = (y + (float) (fy * 0.01));
        this.timestamp = time;
//        this.fx = fx;
//        this.fy = fy;
        this.pressure = pressure;
        this.color = color;
        this.tiltX = tiltX;
        this.tiltY = tiltY;
        this.twist = twist;
        this.penTipType = penTipType;
    }

    /**
     * All dot data into a byte array.
     * (Mainly in real time transmission.)
     *
     * @return byte [ ]
     */
    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(DOT_DATA_BYTE_ALIGN);

        buffer.put( (byte) dotType ); // 1
        buffer.putShort((short) sectionId);// 2 = 3
        buffer.putShort((short) ownerId); // 2 = 5
        buffer.putShort( (short) noteId ); // 2 = 7
        buffer.putShort((short) pageId); // 2 = 9
        buffer.putFloat(x); // 4 = 13
        buffer.putFloat(y); // 4 = 17
        buffer.putLong(timestamp); // 8 = 25
        buffer.put((byte) pressure); //1 = 26
        buffer.putInt(color);// 4 = 30
        buffer.putInt(tiltX); // 4 = 34
        buffer.putInt(tiltY); // 4 = 38
        buffer.putInt(twist); // 4 = 42
        buffer.putInt(penTipType); // 4 = 46

        return buffer.array();
    }

    /**
     * Required dot data only in byte array.
     * (save)
     *
     * @return byte [ ]
     */
    public byte[] toCompactByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(DOT_DATA_COMPACT_BYTE_ALIGN);

        buffer.put( (byte) dotType );
        buffer.putFloat( x );
        buffer.putFloat( y);
        buffer.putLong(timestamp);
//        buffer.put((byte) fx);
//        buffer.put((byte) fy);
        buffer.put((byte) pressure);

        return buffer.array();
    }

    /**
     * To string string.
     *
     * @return the string
     */
    public String ToString()
    {
        String tempstring = "dotType:" + dotType + " sectionId: " + sectionId + " ownerId: " + ownerId + " noteId: " + noteId + " pageId: " + pageId + " x: " + x + " y: " + y + " pressure: " + pressure+" Time:"+timestamp+" penTipType:"+penTipType + " tiltX:"+tiltX+ " tiltY:" + tiltY+" twist:"+twist;
        return tempstring;
    }

    /**
     * To dot dot.
     *
     * @return the dot
     */
    public Dot toDot()
    {
        Dot d = new Dot( x, y, pressure, dotType, timestamp, sectionId, ownerId, noteId, pageId, color,penTipType,tiltX, tiltY,  twist);
        d.macAddress = mac_address;
        return d;
    }

    @Override
    public int describeContents ()
    {
        return 0;
    }

    @Override
    public void writeToParcel ( Parcel parcel, int i )
    {
        parcel.writeInt(sectionId);
        parcel.writeInt(ownerId);
        parcel.writeInt( noteId );
        parcel.writeInt( pageId );
        parcel.writeFloat( x );
        parcel.writeFloat( y );
        parcel.writeInt(pressure);
        parcel.writeInt(color);
        parcel.writeInt(dotType);
        parcel.writeInt(penTipType);
        parcel.writeInt(tiltX);
        parcel.writeInt(tiltY);
        parcel.writeInt(twist);
        parcel.writeLong( timestamp );
        parcel.writeString( mac_address );

        //[2018.03.05] Stroke Test
        parcel.writeInt(dotCount);
        parcel.writeInt(totalImgCount);
        parcel.writeInt(processImgCount);
        parcel.writeInt(successImgCount);
        parcel.writeInt(sendImgCount);
    }

    /**
     * The constant CREATOR.
     */
    public static final Parcelable.Creator<Fdot> CREATOR = new Parcelable.Creator<Fdot>()
    {
        @Override
        public Fdot createFromParcel(Parcel source)
        {

            int sectionId = source.readInt();
            int ownerId = source.readInt();
            int noteId = source.readInt();
            int pageId = source.readInt();
            float x = source.readFloat();
            float y = source.readFloat();
            int pressure = source.readInt();
            int color = source.readInt();
            int dotType = source.readInt();
            int penTipType = source.readInt();
            int tiltX = source.readInt();
            int tiltY = source.readInt();
            int twist = source.readInt();
            long timestamp = source.readLong();
            String address = source.readString();

            //[2018.03.05] Stroke Test
            int dotCount = source.readInt();
            int totalImgCount = source.readInt();
            int processImgCount = source.readInt();
            int successImgCount = source.readInt();
            int sendImgCount = source.readInt();
            Fdot oFdot = new Fdot(x, y,pressure, dotType, timestamp, sectionId, ownerId, noteId, pageId, color,penTipType,tiltX, tiltY, twist, dotCount, totalImgCount, processImgCount, successImgCount, sendImgCount);
            oFdot.mac_address = address;
            return oFdot;
        }

        @Override
        public Fdot[] newArray(int size)
        {
            return new Fdot[size];
        }

    };
}