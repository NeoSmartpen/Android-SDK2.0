package kr.neolab.sdk.ink.structure;

/**
 * Separated by a dot enumeration type
 *
 * @author CHY
 */
public enum DotType
{
	/**
	 * Pen action down dot type.
	 */
	PEN_ACTION_DOWN(17), /**
 * Pen action move dot type.
 */
PEN_ACTION_MOVE(18), /**
 * Pen action up dot type.
 */
PEN_ACTION_UP(20), /**
 * Pen action up dot type.
 */
PEN_ACTION_HOVER(25);

	
	private final int value;
	
	DotType(int i)
	{
		this.value = i;
	}

	/**
	 * Gets value.
	 *
	 * @return the value
	 */
	public int getValue()
	{
		return this.value;
	}
	
//	public static int toIntInkDot(DotType type)
//	{
//		return PEN_TYPE_INK.getValue() | type.getValue();
//	}
//
//	public static int toIntEraserDot(DotType type)
//	{
//		return PEN_TYPE_ERASER.getValue() | type.getValue();
//	}
//
//	public static DotType getPenType(int type)
//	{
//		if ( isEraserType(type) )
//		{
//			return PEN_TYPE_ERASER;
//		}
//
//		return PEN_TYPE_INK;
//	}
	
//	public static DotType getPenAction(int type)
//	{
//		if ( isPenActionDown(type) )
//		{
//			return PEN_ACTION_DOWN;
//		}
//		else if ( isPenActionUp(type) )
//		{
//			return PEN_ACTION_UP;
//		}
//
//		return PEN_ACTION_MOVE;
//	}

	/**
	 * Is pen action up boolean.
	 *
	 * @param type the type
	 * @return the boolean
	 */
	public static boolean isPenActionUp(int type)
	{
		return (type == PEN_ACTION_UP.getValue()) ? true : false;
	}

	/**
	 * Is pen action down boolean.
	 *
	 * @param type the type
	 * @return the boolean
	 */
	public static boolean isPenActionDown(int type)
	{
		return (type == PEN_ACTION_DOWN.getValue())  ? true : false;
	}

	/**
	 * Is pen action move boolean.
	 *
	 * @param type the type
	 * @return the boolean
	 */
	public static boolean isPenActionMove(int type)
	{
		return (type == PEN_ACTION_MOVE.getValue())  ? true : false;
	}

	/**
	 * Is pen action hover boolean.
	 *
	 * @param type the type
	 * @return the boolean
	 */
	public static boolean isPenActionHover(int type)
	{
		return (type == PEN_ACTION_HOVER.getValue())  ? true : false;
	}
	
};