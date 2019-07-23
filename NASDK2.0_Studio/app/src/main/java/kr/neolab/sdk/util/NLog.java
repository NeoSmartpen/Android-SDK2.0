package kr.neolab.sdk.util;

import android.util.Log;

/**
 * The type N log.
 */
public class NLog
{
	private static final String TAG = "nasdk";
	private static final boolean D = true;
	private static final boolean I = true;
	private static final boolean E = true;
	private static final boolean W = true;

	/**
	 * D.
	 *
	 * @param text the text
	 */
	public static void d(String text)
	{
		if (D)
		{
			Log.d(TAG, text);
		}
	}

	/**
	 * D.
	 *
	 * @param tag  the tag
	 * @param text the text
	 */
	public static void d(String tag, String text)
	{
		if (D)
		{
			Log.d(TAG, text);
		}
	}

	/**
	 * .
	 *
	 * @param text the text
	 */
	public static void i(String text)
	{
		if (I)
		{
			Log.i(TAG, text);
		}
	}

	/**
	 * .
	 *
	 * @param tag  the tag
	 * @param text the text
	 */
	public static void i(String tag, String text)
	{
		if (I)
		{
			Log.i(TAG, text);
		}
	}

	/**
	 * W.
	 *
	 * @param text the text
	 */
	public static void w(String text)
	{
		if (W)
		{
			Log.w(TAG, text);
		}
	}

	/**
	 * W.
	 *
	 * @param tag  the tag
	 * @param text the text
	 */
	public static void w(String tag,  String text)
	{
		if (W)
		{
			Log.w(TAG, text);
		}
	}

	/**
	 * E.
	 *
	 * @param text the text
	 */
	public static void e(String text)
	{
		if (E)
		{
			Log.e(TAG, text);
		}
	}

	/**
	 * E.
	 *
	 * @param text the text
	 * @param e    the e
	 */
	public static void e(String text, Exception e)
	{
		if (E)
		{
			Log.e(TAG, text, e);
		}
	}	
}
