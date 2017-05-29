package kr.neolab.sdk.metadata.structure;

import android.graphics.RectF;

/**
 * The type Symbol.
 */
public class Symbol extends RectF
{
	/**
	 * The Note id.
	 */
	public int noteId, /**
 * The Page id.
 */
pageId;
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
}