package kr.neolab.sdk.ink.structure;

import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;

import kr.neolab.sdk.util.NLog;

/**
 * A collection of Dot (including down, move, up Dot)
 *
 * @author CHY
 */
public class Stroke implements Parcelable
{
	private ArrayList<Dot> dots = null;

	/**
	 * The constant STROKE_TYPE_NORMAL.
	 */
	public static final int STROKE_TYPE_NORMAL = 0;
	/**
	 * The constant STROKE_TYPE_PEN.
	 */
	public static final int STROKE_TYPE_PEN = 1;
	/**
	 * The constant STROKE_TYPE_HIGHLIGHT.
	 */
	public static final int STROKE_TYPE_HIGHLIGHT = 2;

	/**
	 * The constant PEN_TIP_TYPE_NORMAL.
	 */
	public static final int PEN_TIP_TYPE_NORMAL = 0;
	/**
	 * The constant PEN_TIP_TYPE_ERASER.
	 */
	public static final int PEN_TIP_TYPE_ERASER = 1;


	/*
	 * Stroke을 둘러싸는 사각형의 영역
	 */
	private RectF rectArea = null;

	/**
	 * The Time stamp start.
	 */
	public long timeStampStart = 0l;
	/**
	 * The Time stamp end.
	 */
	public long timeStampEnd = 0l;
	/**
	 * The Section id.
	 */
	public int sectionId = 0, /**
 * The Owner id.
 */
ownerId = 0, /**
 * The Note id.
 */
noteId, /**
 * The Page id.
 */
pageId, /**
 * The Color.
 */
color = Color.BLACK, /**
 * The Pen tip type.
 */
penTipType = PEN_TIP_TYPE_NORMAL, /**
 * The Thickness.
 */
thickness = 1;

	/**
	 * The Type.
	 */
	public int type = STROKE_TYPE_NORMAL;

	private boolean isReadonly = false;

	private Stroke()
	{
	}

	/**
	 * A constructor that constructs a Stroke
	 *
	 * @param sectionId the section id
	 * @param ownerId   the owner id
	 * @param noteId    the note id
	 * @param pageId    the page id
	 * @param color     the color
	 */
	public Stroke( int sectionId, int ownerId, int noteId, int pageId, int color )
	{
		this(sectionId, ownerId, noteId, pageId, color, STROKE_TYPE_NORMAL);
	}

	/**
	 * A constructor that constructs a Stroke
	 *
	 * @param sectionId the section id
	 * @param ownerId   the owner id
	 * @param noteId    the note id
	 * @param pageId    the page id
	 */
	public Stroke( int sectionId, int ownerId, int noteId, int pageId )
	{
		this(sectionId, ownerId, noteId, pageId, Color.BLACK, STROKE_TYPE_NORMAL);
	}

	/**
	 * A constructor that constructs a Stroke
	 *
	 * @param sectionId the section id
	 * @param ownerId   the owner id
	 * @param noteId    the note id
	 * @param pageId    the page id
	 * @param color     the color
	 * @param type      the type
	 */
	public Stroke( int sectionId, int ownerId, int noteId, int pageId, int color, int type )
	{
		this.dots = new ArrayList<Dot>();
		this.sectionId = sectionId;
		this.ownerId = ownerId;
		this.noteId = noteId;
		this.pageId = pageId;
		this.color = color;
		this.type = type;
	}
	
	/**
	 * Add Dot to Stroke
	 * 
	 * @param x
	 *            x-coordinate of Stroke
	 * @param y
	 *            y-coordinate of Stroke
	 * @param dotType
	 *            Separated by a dot enumeration type
	 * @param timestamp
	 *            timestamp
	 */
//	public boolean add( float x, float y, int pressure, int dotType, long timestamp )
//	{
//		return this.add( new Dot( x, y, pressure, dotType, timestamp ,sectionId, ownerId, noteId, pageId, color, penTipType) );
//	}

	/**
	 * Add Dot to Stroke
	 *
	 * @param dot instance of Dot Class
	 * @return boolean boolean
	 */
	public boolean add( Dot dot )
	{
		if ( isReadonly )
		{
			NLog.e( "failed to add the dots. this stroke was already sealed." );
			return false;
		}

		if ( DotType.isPenActionDown( dot.dotType ) || dots.size() <= 0 )
		{
			this.timeStampStart = dot.timestamp;
		}
		else if ( DotType.isPenActionUp( dot.dotType ) )
		{
			this.timeStampEnd = dot.timestamp;
			this.isReadonly = true;
			this.setRectArea();
		}
		else
		{
			this.timeStampEnd = dot.timestamp;
		}

		this.dots.add( dot );

		return true;
	}

	/**
	 * Returns the index of the corresponding dot.
	 *
	 * @param index The sequence number corresponding to the point
	 * @return dot dot
	 */
	public Dot get( int index )
	{
		return dots.get( index );
	}

	/**
	 * Returns the size of dot.
	 *
	 * @return int int
	 */
	public int size()
	{
		return dots.size();
	}

	private void setRectArea()
	{
		float[] xs = new float[dots.size()];
		float[] ys = new float[dots.size()];

		for ( int i = 0; i < dots.size(); i++ )
		{
			xs[i] = dots.get( i ).x;
			ys[i] = dots.get( i ).y;
		}

		Arrays.sort( xs );
		Arrays.sort( ys );

		float sx = xs[0];
		float sy = ys[0];

		float ex = xs[xs.length - 1];
		float ey = ys[ys.length - 1];

		this.rectArea = new RectF( sx, sy, ex, ey );
	}

//	/**
//	 * Draw all dot on the canvas object
//	 *
//	 * @param canvas
//	 *            Canvas object to draw stroke
//	 * @param scale
//	 *            To zoom in or out of scale for stroke
//	 * @param offset_x
//	 *            coordinates by moving the draw stroke
//	 * @param offset_y
//	 *            coordinates by moving the draw stroke
//	 */
//	public void drawToCanvas( Canvas canvas, float scale, float offset_x, float offset_y )
//	{
//		this.drawToCanvas( canvas, scale, offset_x, offset_y, 1, color, type );
//	}
//
//	/**
//	 * Draw all dot on the canvas object
//	 *
//	 * @param canvas
//	 *            Canvas object to draw stroke
//	 * @param scale
//	 *            To zoom in or out of scale for stroke
//	 * @param offset_x
//	 *            coordinates by moving the draw stroke
//	 * @param offset_y
//	 *            coordinates by moving the draw stroke
//	 */
//	public void drawToCanvas( Canvas canvas, float scale, float offset_x, float offset_y, int strokeType )
//	{
//		this.drawToCanvas( canvas, scale, offset_x, offset_y, 1, color, strokeType );
//	}
//
//	/**
//	 * Draw all dot on the canvas object
//	 *
//	 * @param canvas
//	 *            Canvas object to draw stroke
//	 * @param scale
//	 *            To zoom in or out of scale for stroke
//	 * @param offset_x
//	 *            coordinates by moving the draw stroke
//	 * @param offset_y
//	 *            coordinates by moving the draw stroke
//	 * @param width
//	 *            width of stroke
//	 * @param color
//	 *            color of stroke
//	 */
//	public void drawToCanvas( Canvas canvas, float scale, float offset_x, float offset_y, int width, int color )
//	{
//		this.drawToCanvas( canvas, scale, offset_x, offset_y, width, color, width, color, type );
//	}
//
//
//
//	/**
//	 * Draw all dot on the canvas object
//	 *
//	 * @param canvas
//	 *            Canvas object to draw stroke
//	 * @param scale
//	 *            To zoom in or out of scale for stroke
//	 * @param offset_x
//	 *            coordinates by moving the draw stroke
//	 * @param offset_y
//	 *            coordinates by moving the draw stroke
//	 * @param width
//	 *            width of stroke
//	 * @param color
//	 *            color of stroke
//	 * @param strokeType
//	 *            type of stroke
//	 */
//	public void drawToCanvas( Canvas canvas, float scale, float offset_x, float offset_y, int width, int color , int strokeType)
//	{
//		this.drawToCanvas( canvas, scale, offset_x, offset_y, width, color, width, color, strokeType );
//	}
//
//	/**
//	 * Draw all dot on the canvas object
//	 *
//	 * @param canvas
//	 *            Canvas object to draw stroke
//	 * @param scale
//	 *            To zoom in or out of scale for stroke
//	 * @param offset_x
//	 *            coordinates by moving the draw stroke
//	 * @param offset_y
//	 *            coordinates by moving the draw stroke
//	 * @param width1
//	 *            width of complete stroke
//	 * @param color1
//	 *            color of complete stroke
//	 * @param width2
//	 *            width of interim stroke
//	 * @param color2
//	 *            color of interim stroke
//	 */
//	public void drawToCanvas( Canvas canvas, float scale, float offset_x, float offset_y, int width1, int color1, int width2, int color2 )
//	{
//		this.drawToCanvas( canvas, scale, offset_x, offset_y, width1, color1, width2, color2, type );
//	}
//
//	/**
//	 * Draw all dot on the canvas object
//	 *
//	 * @param canvas
//	 *            Canvas object to draw stroke
//	 * @param scale
//	 *            To zoom in or out of scale for stroke
//	 * @param offset_x
//	 *            coordinates by moving the draw stroke
//	 * @param offset_y
//	 *            coordinates by moving the draw stroke
//	 * @param width1
//	 *            width of complete stroke
//	 * @param color1
//	 *            color of complete stroke
//	 * @param width2
//	 *            width of interim stroke
//	 * @param color2
//	 *            color of interim stroke
//	 * @param strokeType
//	 *            type of stroke
//	 */
//	public void drawToCanvas( Canvas canvas, float scale, float offset_x, float offset_y, int width1, int color1, int width2, int color2, int strokeType )
//	{
//		int length = dots.size();
//
//		if (length <= 0)
//		{
//			return;
//		}
//
//		float[] xs = new float[length];
//		float[] ys = new float[length];
//		int[] ps = new int[length];
//
//		for ( int i = 0; i < length; i++ )
//		{
//			xs[i] = dots.get( i ).x;
//			ys[i] = dots.get( i ).y;
//			ps[i] = dots.get( i ).pressure;
//		}
//
//		super.readyToDraw( xs, ys, ps );
//
//		if ( !isReadonly )
//		{
//			super.setPenType( width2, color2 );
//		}
//		else
//		{
//			super.setPenType( width1, color1 );
//		}
//
//		super.drawStroke( canvas, scale, offset_x, offset_y, false, strokeType );
//	}

	/**
	 * Return array of x-coordinates
	 *
	 * @return float [ ]
	 */
	public float[] getXArray()
	{
		float[] xs = new float[dots.size()];

		for ( int i = 0; i < dots.size(); i++ )
		{
			xs[i] = dots.get( i ).x;
		}

		return xs;
	}

	/**
	 * Return array of y-coordinates
	 *
	 * @return float [ ]
	 */
	public float[] getYArray()
	{
		float[] ys = new float[dots.size()];

		for ( int i = 0; i < dots.size(); i++ )
		{
			ys[i] = dots.get( i ).y;
		}

		return ys;
	}

	/**
	 * Return array of pressure
	 *
	 * @return short [ ]
	 */
	public short[] getPressureArray()
	{
		short[] ps = new short[dots.size()];

		for ( int i = 0; i < dots.size(); i++ )
		{
			ps[i] = (short)dots.get( i ).pressure;
		}

		return ps;
	}

	/**
	 * Return array of timestamp
	 *
	 * @return long [ ]
	 */
	public long[] getTimestampArray()
	{
		long[] ps = new long[dots.size()];

		for ( int i = 0; i < dots.size(); i++ )
		{
			ps[i] = dots.get( i ).getTimestamp();
		}

		return ps;
	}

	/**
	 * Gets dots.
	 *
	 * @return the dots
	 */
	public ArrayList<Dot> getDots()
	{
		return dots;
	}

	/**
	 * Change scale all the dots
	 *
	 * @param scale Scale to zoom in or out of scale for stroke
	 */
	public void changeScale( float scale )
	{
		for ( Dot p : dots )
		{
			p.x *= scale;
			p.y *= scale;
		}
	}

	/**
	 * Moves all the dots.
	 *
	 * @param dx Moving coordinate-x
	 * @param dy Moving coordinate-y
	 */
	public void changePos( float dx, float dy )
	{
		for ( Dot p : dots )
		{
			p.x += dx;
			p.y += dy;
		}
	}

	/**
	 * Contains boolean.
	 *
	 * @param area the area
	 * @return the boolean
	 */
	public boolean contains( RectF area )
	{
		for ( Dot p : dots )
		{
			if ( area.contains( p.x, p.y ) )
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Intersect boolean.
	 *
	 * @param area the area
	 * @return the boolean
	 */
	public boolean intersect( RectF area )
	{
		return this.rectArea.intersect( area );
	}

	/**
	 * Gets path.
	 *
	 * @return the path
	 */
	public Path getPath()
	{
		Path path = new Path();

		boolean isfirst = true;

		for ( Dot p : dots )
		{
			if ( isfirst )
			{
				path.moveTo( p.x, p.y );
				isfirst = false;
			}
			else
			{
				path.lineTo( p.x, p.y );
			}
		}

		return path;
	}

	/**
	 * Is read only boolean.
	 *
	 * @return the boolean
	 */
	public boolean isReadOnly()
	{
		return isReadonly;
	}

	@Override
	public int describeContents ()
	{
		return 0;
	}

	@Override
	public void writeToParcel ( Parcel parcel, int i )
	{
		parcel.writeInt( sectionId );
		parcel.writeInt( ownerId );
		parcel.writeInt( noteId );
		parcel.writeInt( pageId );
		parcel.writeInt( color );
		parcel.writeInt( type );
		parcel.writeInt( penTipType );
		parcel.writeList( dots );
	}

	/**
	 * The constant CREATOR.
	 */
	public static final Parcelable.Creator< Stroke > CREATOR = new Parcelable.Creator<Stroke>() {

		@Override
		public Stroke createFromParcel(Parcel source) {
			Stroke oStroke = new Stroke();

			oStroke.sectionId = source.readInt();
			oStroke.ownerId = source.readInt();
			oStroke.noteId = source.readInt();
			oStroke.pageId = source.readInt();
			oStroke.color = source.readInt();
			oStroke.type = source.readInt();
			oStroke.penTipType = source.readInt();

			oStroke.dots = source.readArrayList( Dot.class.getClassLoader() );
			return oStroke;
		}

		@Override
		public Stroke[] newArray(int size) {
			return new Stroke[ size ];
		}
	};

}
