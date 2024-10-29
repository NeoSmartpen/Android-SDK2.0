package kr.neolab.sdk.metadata.structure;

import android.graphics.RectF;

import java.util.HashMap;

import static kr.neolab.sdk.metadata.MetadataCtrl.PIXEL_TO_DOT_SCALE;

/**
 * The type Symbol.
 */
public class Symbol extends RectF
{
	public static final String TYPE_RECTANGLE = "Rectangle";
	public static final String TYPE_TRIANGLE = "Triangle";
	public static final String TYPE_ELLIPSE = "ELLIPSE";
	public static final String TYPE_POLYGON = "Polygon";

	public static final String ACTION_SCHEDULE = "schedule";
    public static final String ACTION_COLOR = "color";
    public static final String ACTION_THICKNESS = "thickness";
    public static final String ACTION_WEBVIEW = "webview";
    public static final String ACTION_BROWSER = "browser";
    public static final String ACTION_PAGE = "page";
    public static final String ACTION_SHARE = "share";
    public static final String ACTION_POST = "post";
	public static final String ACTION_IN_APP = "inapp";
	public static final String ACTION_IN_APP_POST = "inapppost";
	public static final String ACTION_AUDIO = "audio";

    public HashMap<String, Extra> extraMap = new HashMap<>();

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
			isInside = x >= centerX - xWidth;
		}
		else
		{
			isInside = x <= centerX + xWidth;
		}

		if( isInside )
			isInside = top <= y && y <= bottom;

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

	public boolean isAllDaySchedule() {
	    return ACTION_SCHEDULE.equals(action) && extraMap.get("area") != null && extraMap.get("hh") == null && extraMap.get("mm") == null;
    }

    public boolean isTimedSchedule() {
	    return ACTION_SCHEDULE.equals(action) && extraMap.get("area") != null && extraMap.get("hh") != null && extraMap.get("mm") != null;
    }

    public boolean isCropWithoutFrame() {
	    return ACTION_SHARE.equals(action) && "crop".equals(param) && extraMap.get("area") != null && extraMap.get("date") == null;
    }

    public boolean isCropWithFrame() {
        return ACTION_SHARE.equals(action) && "crop".equals(param) && extraMap.get("area") != null && extraMap.get("date") != null;
    }

    public boolean isPenColor() {
	    return ACTION_COLOR.equals(action);
    }

    public boolean isPenThickness() {
        return ACTION_THICKNESS.equals(action);
    }

    public boolean isWebview() {
	    return ACTION_WEBVIEW.equals(action);
    }

    public boolean isBrowser() {
        return ACTION_BROWSER.equals(action);
    }

    public boolean isGoToTag() {
        return ACTION_PAGE.equals(action) && "tag".equals(param);
    }

    public boolean isSetFavorite() {
        return ACTION_PAGE.equals(action) && "bookmark".equals(param);
    }

    public boolean isPostAction() {
	    return ACTION_POST.equals(action);
    }

    public int[] getPageNumbers() {
	    if( (isPostAction() || isInApp() || isInAppPost())
				&& extraMap.get("pages") != null) {
	        String[] pagesStrArr = extraMap.get("pages").param.split(",");
            int[] pages = new int[pagesStrArr.length];
            try {
                for(int i=0;i<pagesStrArr.length;i++) {
                    pages[i] = Integer.parseInt(pagesStrArr[i].trim());
                }
                return pages;
            }catch (NumberFormatException e) {
                e.printStackTrace();
                return new int[]{};
            }
        }
	    else
            return new int[]{};
    }

    public boolean isButton() {
		if (!isInApp() && !isInAppPost()) {
			return false;
		}

		Extra value = extraMap.get("button");
		return value == null || value.param.equals("true");
    }

    public RectF getArea() {
        Extra extra = extraMap.get("area");

	    if( extra != null && extra.param.matches("^([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))$") ) {
	        try {
                String[] array = extra.param.split(",");
                float left = Float.parseFloat(array[0].trim())*PIXEL_TO_DOT_SCALE;
                float top = Float.parseFloat(array[1].trim())*PIXEL_TO_DOT_SCALE;
                float right = left + ( Float.parseFloat(array[2].trim()) * PIXEL_TO_DOT_SCALE );
                float bottom = top + ( Float.parseFloat(array[3].trim()) * PIXEL_TO_DOT_SCALE );
                return new RectF(left, top, right, bottom);
            }catch (Exception e) {
	            e.printStackTrace();
	            return null;
            }

        }
	    else return null;
    }

    public RectF getHourArea() {
        Extra extra = extraMap.get("hh");

        if( extra != null && extra.param.matches("^([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))$") ) {
            try {
                String[] array = extra.param.split(",");
                float left = Float.parseFloat(array[0].trim())*PIXEL_TO_DOT_SCALE;
                float top = Float.parseFloat(array[1].trim())*PIXEL_TO_DOT_SCALE;
                float right = left + ( Float.parseFloat(array[2].trim()) * PIXEL_TO_DOT_SCALE );
                float bottom = top + ( Float.parseFloat(array[3].trim()) * PIXEL_TO_DOT_SCALE );
                return new RectF(left, top, right, bottom);
            }catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }
        else return null;
    }

    public RectF getMinuteArea() {
        Extra extra = extraMap.get("mm");

        if( extra != null && extra.param.matches("^([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))$") ) {
            try {
                String[] array = extra.param.split(",");
                float left = Float.parseFloat(array[0].trim())*PIXEL_TO_DOT_SCALE;
                float top = Float.parseFloat(array[1].trim())*PIXEL_TO_DOT_SCALE;
                float right = left + ( Float.parseFloat(array[2].trim()) * PIXEL_TO_DOT_SCALE );
                float bottom = top + ( Float.parseFloat(array[3].trim()) * PIXEL_TO_DOT_SCALE );
                return new RectF(left, top, right, bottom);
            }catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }
        else return null;
    }

    public String getDateParam() {
	    if(isDateParam()) {
	        return param;
        }
	    else
	        return "";
    }

    public String getDateExtra() {
        if(isDateExtra()) {
            return extraMap.get("date").param;
        }
        else
            return "";
    }

    private boolean isDateParam() {
	    return param.matches("^((\\d\\d)?\\d\\d)?([-\\s/.])?(0[1-9]|1[012])([-\\s/.])?(0[1-9]|[12][0-9]|3[01])$");
    }

    private boolean isDateExtra() {
        Extra extra = extraMap.get("date");
        return extra != null && extra.param.matches("^((\\d\\d)?\\d\\d)?([-\\s/.])?(0[1-9]|1[012])([-\\s/.])?(0[1-9]|[12][0-9]|3[01])$");
    }

    public boolean isInApp() {
		return ACTION_IN_APP.equals(action);
	}

	public boolean isInAppPost() {
		return ACTION_IN_APP_POST.equals(action);
	}

	public boolean isAudio() {
		return ACTION_AUDIO.equals(action);
	}

//    private boolean isArea() {
//	    Extra extra = extraMap.get("area");
//	    return extra != null && extra.param.matches("^([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))(\\s*,\\s*)([+-]?\\d*(\\.?\\d*))$");
//    }
}