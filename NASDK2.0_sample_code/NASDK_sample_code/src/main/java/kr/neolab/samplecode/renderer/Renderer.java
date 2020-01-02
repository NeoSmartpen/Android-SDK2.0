package kr.neolab.samplecode.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import kr.neolab.sdk.ink.structure.Stroke;

/**
 * The class that provides the drawing functions
 * 
 * @author CHY
 *
 */
@Deprecated
public class Renderer 
{

	static private int color = Color.BLACK;

	/**
	 * draw to canvas object
	 *
	 * @param canvas Canvas object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 */
	public static void draw( Canvas canvas, Stroke[] strokes, float scale, float offset_x, float offset_y )
	{
		draw( canvas, strokes, scale, offset_x, offset_y, Stroke.STROKE_TYPE_NORMAL );
	}

	/**
	 * draw to canvas object
	 * 
	 * @param canvas Canvas object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param strokeType Type of stroke.
	 */
	public static void draw( Canvas canvas, Stroke[] strokes, float scale, float offset_x, float offset_y, int strokeType )
	{
		draw( canvas, strokes, scale, offset_x, offset_y, 1, color,1, color,  strokeType );
	}

	/**
	 * draw to canvas object
	 *
	 * @param canvas Canvas object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param width width of stroke
	 * @param color color of stroke
	 */
	public static void draw( Canvas canvas, Stroke[] strokes, float scale, float offset_x, float offset_y, int width, int color )
	{
		draw( canvas, strokes, scale, offset_x, offset_y, width, color, Stroke.STROKE_TYPE_NORMAL );
	}
	
	/**
	 * draw to canvas object
	 * 
	 * @param canvas Canvas object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param width width of stroke
	 * @param color color of stroke
	 * @param strokeType Type of stroke.
	 */
	public static void draw( Canvas canvas, Stroke[] strokes, float scale, float offset_x, float offset_y, int width, int color, int strokeType)
	{
		draw( canvas, strokes, scale, offset_x, offset_y, width, color, width, color, strokeType);
	}

	/**
	 * draw to canvas object
	 *
	 * @param canvas Canvas object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param width1 width of complete stroke
	 * @param color1 color of complete stroke
	 * @param width2 width of interim stroke
	 * @param color2 color of interim stroke
	 */
	public static void draw( Canvas canvas, Stroke[] strokes, float scale, float offset_x, float offset_y, int width1, int color1, int width2, int color2 )
	{
		draw( canvas, strokes, scale, offset_x, offset_y, width1, color1, width2, color2, Stroke.STROKE_TYPE_NORMAL);
	}

	/**
	 * draw to canvas object
	 * 
	 * @param canvas Canvas object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param width1 width of complete stroke
	 * @param color1 color of complete stroke
	 * @param width2 width of interim stroke
	 * @param color2 color of interim stroke
	 * @param strokeType Type of stroke.
	 */
	public static void draw( Canvas canvas, Stroke[] strokes, float scale, float offset_x, float offset_y, int width1, int color1, int width2, int color2, int strokeType)
	{
		setPressureRecalculatorFactorsForF110();
		for ( int i=0; i<strokes.length; i++ )
		{
			Stroke s= strokes[i];
			int length = strokes[i].getDots().size();

			if (length <= 0)
			{
				break;
			}

			float[] xs = new float[length];
			float[] ys = new float[length];
			int[] ps = new int[length];

			for ( int j = 0; j < length; j++ )
			{
				xs[j] = s.getDots().get( j ).x;
				ys[j] = s.getDots().get( j ).y;
				ps[j] = s.getDots().get( j ).pressure;
			}
			StrokeDrawer drawer = new StrokeDrawer();
			drawer.readyToDraw( xs, ys, ps );
			drawer.setPenType(width1, color1 );
//			if ( !isReadonly )
//			{
//				super.setPenType( width2, color2 );
//			}
//			else
//			{
//				super.setPenType( width1, color1 );
//			}
			drawer.drawStroke( canvas, scale, offset_x, offset_y, false, strokeType  );
		}
	}
	
	/**
	 * draw to bitmap object
	 * 
	 * @param bitmap Bitmap object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 */
	public static void draw( Bitmap bitmap, Stroke[] strokes, float scale, float offset_x, float offset_y)
	{
		draw(bitmap, strokes, scale, offset_x, offset_y, Stroke.STROKE_TYPE_NORMAL);
	}

	/**
	 * draw to bitmap object
	 *
	 * @param bitmap Bitmap object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param strokeType Type of stroke.
	 */
	public static void draw( Bitmap bitmap, Stroke[] strokes, float scale, float offset_x, float offset_y, int strokeType)
	{
		Canvas canvas = new Canvas(bitmap);

		draw( canvas, strokes, scale, offset_x, offset_y, 1, color,1, color,  strokeType );
	}

	/**
	 * draw to bitmap object
	 *
	 * @param bitmap Bitmap object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param width width of stroke
	 * @param color color of stroke
	 */
	public static void draw( Bitmap bitmap, Stroke[] strokes, float scale, float offset_x, float offset_y, int width, int color )
	{
		draw( bitmap, strokes, scale, offset_x, offset_y, width, color, Stroke.STROKE_TYPE_NORMAL );
	}

	/**
	 * draw to bitmap object
	 *
	 * @param bitmap Bitmap object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param width width of stroke
	 * @param color color of stroke
	 * @param strokeType Type of stroke.
	 */
	public static void draw( Bitmap bitmap, Stroke[] strokes, float scale, float offset_x, float offset_y, int width, int color, int strokeType)
	{
		Canvas canvas = new Canvas(bitmap);

		draw( canvas, strokes, scale, offset_x, offset_y, width, color,width, color,  strokeType );
	}

	/**
	 * draw to bitmap object
	 *
	 * @param bitmap Bitmap object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param width1 width of complete stroke
	 * @param color1 color of complete stroke
	 * @param width2 width of interim stroke
	 * @param color2 color of interim stroke
	 */
	public static void draw( Bitmap bitmap, Stroke[] strokes, float scale, float offset_x, float offset_y, int width1, int color1, int width2, int color2)
	{
		draw(bitmap, strokes, scale, offset_x, offset_y, width1, color1, width2, color2, Stroke.STROKE_TYPE_NORMAL);
	}

	/**
	 * draw to bitmap object
	 * 
	 * @param bitmap Bitmap object to draw stroke
	 * @param strokes A collection of draw stroke
	 * @param scale To zoom in or out of scale for stroke
	 * @param offset_x coordinates by moving the draw stroke
	 * @param offset_y coordinates by moving the draw stroke
	 * @param width1 width of complete stroke
	 * @param color1 color of complete stroke
	 * @param width2 width of interim stroke
	 * @param color2 color of interim stroke
	 * @param strokeType Type of stroke.
	 */
	public static void draw( Bitmap bitmap, Stroke[] strokes, float scale, float offset_x, float offset_y, int width1, int color1, int width2, int color2, int strokeType)
	{
		Canvas canvas = new Canvas(bitmap);


		draw(canvas, strokes, scale, offset_x, offset_y, width1, color1, width2, color2, strokeType);
	}

	public static boolean isPressureRecalculatorFactors = false;
	public static float mPressureFactor[];
	public static float mPressureFactorStrong[];

	public static float mPressureFactor_F100e[] = new float[256];
	public static float mPressureFactorStrong_F100e[] = new float[256];

	public static float mPressureFactor_F110[] = new float[256];
	public static float mPressureFactorStrong_F110[] = new float[256];

	//Line profile
	public static int lineProfile = 5;

	/**
	 * Recalculate Pressure Factors in a Linear.
	 * Only for F110 Pen Model.
	 */
	public static void setPressureRecalculatorFactorsForF110()
	{
		if( isPressureRecalculatorFactors )
			return;

		for ( int force = 0; force < 256; force++ )
		{
			float force_f;

			if ( force < 60 )
				force_f = 0.1f;
			else if ( force < 100 )
				force_f = 0.2f;
			else if ( force < 120 )
				force_f = 0.3f;
			else if ( force < 140 )
				force_f = 0.4f;
			else if ( force < 165 )
				force_f = 0.5f;
			else if ( force < 180 )
				force_f = 0.7f;
			else if ( force < 190 )
				force_f = 0.8f;
			else
				force_f = 0.95f;

			mPressureFactorStrong_F100e[force] = (float) Math.sqrt( (double) force ) * 16;
			mPressureFactor_F100e[force] = (int) (force_f * 255);

			// for F110
			if(force < 50) mPressureFactorStrong_F110[force] = 30;
			else mPressureFactorStrong_F110[force] = force*2/3;

			if(lineProfile==1){
				if (2 * force > 255) mPressureFactorStrong_F110[force] = 255;
				else mPressureFactorStrong_F110[force] = (float) 2 * force;
			}else if(lineProfile==2){
				if(force < 50) mPressureFactorStrong_F110[force] = 50;
				else mPressureFactorStrong_F110[force] =force;
			}else if(lineProfile==3){
				if (force < 50)    mPressureFactorStrong_F110[force] = 25;
				else    mPressureFactorStrong_F110[force] = force/2;
			}else if(lineProfile==4){
				if (force < 50)    mPressureFactorStrong_F110[force] = 100;
				else    mPressureFactorStrong_F110[force] = force * 2;
			}else if(lineProfile==5){
				mPressureFactorStrong_F110[force] = force/2 + 30;
			}else if(lineProfile==6){
				mPressureFactorStrong_F110[force] = force*2/3 + 30;
			}


			mPressureFactor_F110[force] = force;
		}

		mPressureFactor = mPressureFactorStrong_F110;
		mPressureFactorStrong = mPressureFactorStrong_F110;

		isPressureRecalculatorFactors = true;
	}
}
