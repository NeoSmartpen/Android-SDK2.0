package kr.neolab.sdk.metadata.structure;

import android.graphics.RectF;

/**
 * The type Symbol.
 */
public class Symbol extends RectF
{
	public static final String TYPE_RECTANGLE = "Rectangle";
	public static final String TYPE_TRIANGLE = "Triangle";
	public static final String TYPE_ELLIPSE = "Ellipse";
	public static final String TYPE_POLYGON = "Polygon";

	/**
	 * The Note id.
	 */
	public int noteId, /**
 * The Page id.
 */
pageId;

	/**
	 * Symbol Type = Rectangle, Triangle, Ellipse, Polygon etc.
	 */
	public String type;

	/**
	 * The Id.
	 */
	public String id, /**
 * The Previous id.
 */
previousId, /**
 * The Next id.
 */
nextId;

	/**
	 * The Previous.
	 */
	public Symbol previous, /**
 * The Next.
 */
next;

	/**
	 * The Name.
	 */
	public String name, /**
 * The Action.
 */
action, /**
 * The Param.
 */
param;

	/**
	 * The Points.
	 */
	public String points;

	/**
	 * The Index Symbol
	 */
	public long index_symbol;

	/**
	 * Instantiates a new Symbol.
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @param name   the name
	 * @param action the action
	 * @param param  the param
	 * @param left   the left
	 * @param top    the top
	 * @param right  the right
	 * @param bottom the bottom
	 */
	public Symbol(int noteId, int pageId, String name, String action, String param, float left, float top, float right, float bottom)
	{
		super(left, top, right, bottom);

		this.noteId = noteId;
		this.pageId = pageId;
		this.name   = name;
		this.action = action;
		this.param  = param;
		this.type = TYPE_RECTANGLE;
	}

	/**
	 * Instantiates a new Symbol.
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @param name   the name
	 * @param action the action
	 * @param param  the param
	 * @param left   the left
	 * @param top    the top
	 * @param right  the right
	 * @param bottom the bottom
	 * @param type the symbol type
	 */
	public Symbol(int noteId, int pageId, String name, String action, String param, float left, float top, float right, float bottom, String type)
	{
		super(left, top, right, bottom);

		this.noteId = noteId;
		this.pageId = pageId;
		this.name   = name;
		this.action = action;
		this.param  = param;
		this.type = type;
	}

	/**
	 * Gets type.
	 *
	 * @return the type
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * Gets id.
	 *
	 * @return the id
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * Gets previous.
	 *
	 * @return the previous
	 */
	public Symbol getPrevious()
	{
		return previous;
	}

	/**
	 * Gets next.
	 *
	 * @return the next
	 */
	public Symbol getNext()
	{
		return next;
	}

	/**
	 * Gets name.
	 *
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Gets action.
	 *
	 * @return the action
	 */
	public String getAction()
	{
		return action;
	}

	/**
	 * Gets param.
	 *
	 * @return the param
	 */
	public String getParam()
	{
		return param;
	}

	/**
	 * Gets x.
	 *
	 * @return the x
	 */
	public float getX()
	{
		return this.left;
	}

	/**
	 * Gets y.
	 *
	 * @return the y
	 */
	public float getY()
	{
		return this.top;
	}

	/**
	 * Gets width.
	 *
	 * @return the width
	 */
	public float getWidth()
	{
		return this.width();
	}

	/**
	 * Gets height.
	 *
	 * @return the height
	 */
	public float getHeight()
	{
		return this.height();
	}

	public String toString()
	{
		return "Symbol => noteId : " + noteId + ", pageId : " + pageId + ", RectF (" + left + "," + top + "," + width() + "," + height() + "), param : " + param;
	}

	@Override
	public boolean contains( float x, float y )
	{
		if( type.compareToIgnoreCase( TYPE_RECTANGLE ) == 0 )
		{
			return super.contains( x, y );
		}
		else if ( type.compareToIgnoreCase( TYPE_TRIANGLE ) == 0 )
		{
			return checkPtInTriangle(x, y);
		}
		else if( type.compareToIgnoreCase( TYPE_ELLIPSE ) == 0 )
		{
			return checkPtInEllipse( x, y );
		}

		return false;
	}

	public boolean checkPtInTriangle( float x, float y )
	{
		boolean isInside = false;

		float halfWidth = this.width() / 2;
		float centerX = left + halfWidth;
		float degree = this.height() / halfWidth;

		float yOffset = y - top;

		float xWidth = yOffset / degree;

		if( x < centerX )
		{
			isInside =  x < centerX - xWidth ? false : true;
		}
		else
		{
			isInside = x > centerX + xWidth ? false : true;
		}

		if( isInside )
			isInside = ( top <= y && y <= bottom)? true:false;

		return isInside;

	}

	public boolean checkPtInEllipse( float x, float y )
	{
		float halfWidth = width() / 2;
		float halfHeight = height() / 2;

		float cx = left + halfWidth;
		float cy = top + halfHeight;

		float dx = x - cx;
		float dy = y - cy;

		float result = (dx*dx) / (halfWidth*halfWidth) + (dy*dy) / (halfHeight*halfHeight);

		return result <= 1.0f;
	}
}