package kr.neolab.samplecode.renderer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import kr.neolab.sdk.ink.structure.Stroke;

@Deprecated
public class StrokeDrawer
{
	public static float LINE_THICKNESS_SCALE = 1 / 1400f;
	public static final float PEN_PRESSURE_SCALE = 255;

	public static boolean PEN_FORCE_CORRECTION = false;
	
	// the actual data
	public float[] mX;
	public float[] mY;

	public int[] mP;
	public int	mN;
	
	// for rendering
	public float[] mFP;		// float pressure

	// pen attribute
	protected int mPenThickness = 1;
	protected int mPenColor = Color.BLACK;
	protected int mPenAlpha = 255;

	// 획 그리기용 데이터 
	protected Path mDrawPath = new Path();
	protected final Paint mFountainPaint = new Paint();
	protected final Paint mPencilPaint = new Paint();
	protected final Paint mHeighLightPaint = new Paint();

	protected Paint mDebugLinePaint = new Paint();
	protected Paint mDebugCirclePaint = new Paint();
	
	protected int mDrawnPointIdx = 0;		// 
	
	//protected static final float EPSILON = 2e-4f;

	public StrokeDrawer()
	{
		// 그리기 속성과 준비
		this.InitPaints();
	}

	protected void readyToDraw( float[] xs, float[] ys, int[] ps ) 
	{

		mDrawnPointIdx = 0;
		
		// 획에 관련된 데이터를 복사
		mN = xs.length;
		mX = xs;
		mY = ys;
		mP = ps;
		
		mFP = new float[mN];

		recalcPressureFactor();
	}
	
	public void setPenType( int penThickness, int penColor )
	{
		mPenThickness = penThickness;
		mPenColor = penColor;
		this.InitPaints();
	}
	
	protected void InitPaints() 
	{
		mFountainPaint.setColor( mPenColor );
		mFountainPaint.setStrokeCap( Paint.Cap.ROUND );
		mFountainPaint.setStyle( Paint.Style.FILL );
		mFountainPaint.setAntiAlias( true );
		
		mDebugLinePaint.setStrokeWidth( 0.1f );
		mDebugLinePaint.setColor( Color.BLACK );
		mDebugLinePaint.setAlpha( 255 );
		mDebugLinePaint.setAntiAlias( true );
		
		mDebugCirclePaint.setColor( Color.RED );
		mDebugCirclePaint.setStyle( Paint.Style.FILL );
		mDebugCirclePaint.setDither( false );
		mDebugCirclePaint.setAntiAlias( true );
		mDebugCirclePaint.setAlpha( 110 );

		mPencilPaint.setColor( mPenColor );
		mPencilPaint.setAlpha( mPenAlpha );
		mPencilPaint.setAntiAlias( true );
		mPencilPaint.setStyle( Paint.Style.STROKE );
		mPencilPaint.setStrokeCap( Paint.Cap.ROUND );

		mHeighLightPaint.setColor( mPenColor );
		mHeighLightPaint.setStrokeWidth( 1 );
		mHeighLightPaint.setStrokeCap( Paint.Cap.SQUARE );
		mHeighLightPaint.setStyle( Paint.Style.STROKE );

	}

	protected void recalcPressureFactor() 
	{
		for ( int i=0; i<mN; i++) 
		{
			// 압력값의 1.0f 기준
			float force_f;
			int force = mP[i];
			
			force_f = getPressureFactor( force );
			
			// 거리별 압력 값도 곱해서 넣어 둔다. 
//			mFP[i] = force_f * getDistancePressureAt(i);
			mFP[i] = force_f;
		}

		// 필압 interpolation, 2014/02/10 kitty
		final int nAvrCnt = 5;
		
		float fp = mFP[mN-1];
		int j, k;
		
		for ( j=1; j<nAvrCnt && j<mN; j++ ) 
		{
			k = mN-1 - j;
			fp += mFP[k]; 
			mFP[k] = fp / (j+1);
		}
		
		for ( j=nAvrCnt; j<mN; j++)
		{
			k = mN-1 - j;

			fp += mFP[k];
//			fp -= getPressureFactor( mP[k+nAvrCnt] ) * getDistancePressureAt(k);
			fp -= getPressureFactor( mP[k+nAvrCnt] );
			mFP[k] = fp / nAvrCnt;
		}
		// 여기까지
	}
	
	// Attributes 세팅
	static public float getScaledPenThickness( float tPenThickness, float scale ) 
	{
		return tPenThickness * scale * LINE_THICKNESS_SCALE;
	}

	// 거리 압력 값 계산
	public float getPressureFactor( int force ) 
	{
		if ( Renderer.isPressureRecalculatorFactors )
		{

			if ( PEN_FORCE_CORRECTION )
			{
				return Renderer.mPressureFactorStrong[force];
			}
			else
			{
				return Renderer.mPressureFactor[force];
			}
		}
		return (float) force;
	}


	// 외부 interface
	// 획 그리기
	protected void drawStroke( Canvas canvas, float scale, float offset_x, float offset_y, boolean bDebugMode, int type )
	{
		if ( bDebugMode ) 
		{
			drawWithLineAndCircle( canvas, scale, offset_x, offset_y );
		}
		else 
		{
			Paint tempPaint = null;

			if( type == Stroke.STROKE_TYPE_NORMAL )
				tempPaint = mFountainPaint;
			else if( type == Stroke.STROKE_TYPE_PEN )
				tempPaint = mPencilPaint;
			else if( type == Stroke.STROKE_TYPE_HIGHLIGHT )
				tempPaint = mHeighLightPaint;
			if( type == Stroke.STROKE_TYPE_NORMAL )
			{
				if ( mN < 3 )
				{
					drawWithStraightLine( canvas, scale, offset_x, offset_y );
				}
				else
				{
					drawFountainPen( canvas, scale, offset_x, offset_y );
				}
			}
			else
			{
				drawWriteStroke( canvas, scale, offset_x, offset_y, tempPaint);
			}


		}
		
		mDrawnPointIdx = 0;
	}

	// 직선만 그리기
	private void drawWithStraightLine( Canvas canvas, float scale, float offset_x, float offset_y ) 
	{
		final float scaled_pen_thickness = getScaledPenThickness( mPenThickness, scale );
		
		mFountainPaint.setStrokeWidth( scaled_pen_thickness );

		// 점 찍기
		if ( mN < 1 ) 
		{
			float x0 = mX[0] * scale + offset_x;
			float y0 = mY[0] * scale + offset_y;
			canvas.drawLine(x0, y0, x0, y0, mFountainPaint );
		}
		// 선 그리기
		else 
		{
			for (int j = 0; j < mN-1; j++) 
			{
				float x0 = mX[j] * scale + offset_x;
				float x1 = mX[j+1] * scale + offset_x;
				float y0 = mY[j] * scale + offset_y;
				float y1 = mY[j+1] * scale + offset_y;
	
				canvas.drawLine(x0, y0, x1, y1, mFountainPaint );
			}
		}
	}

	// 디버그 모드 그리기
	private void drawWithLineAndCircle( Canvas canvas, float scale, float offset_x, float offset_y ) 
	{
		float line_width = scale/32;
		
		if (line_width > 1f )
		{
			line_width = 1f;
		}
		
		mDebugLinePaint.setStrokeWidth( line_width );

		// 선 긋기
		for (int j = 0; j < mN-1; j++) 
		{
			float x0 = mX[j] * scale + offset_x;
			float x1 = mX[j+1] * scale + offset_x;
			float y0 = mY[j] * scale + offset_y;
			float y1 = mY[j+1] * scale + offset_y;
			
			canvas.drawLine(x0, y0, x1, y1, mDebugLinePaint );
		}
		
		// 점 그리기
		float base_width = scale/8;
		if (base_width > 32f ) base_width = 32f;
		
		for (int j = 0; j < mN; j++) 
		{
			float x0 = mX[j] * scale + offset_x;
			float y0 = mY[j] * scale + offset_y;
			
			float pen_width = base_width * (0.01f + 0.99f*(mFP[j]/256f)); 
			canvas.drawCircle( x0, y0, pen_width, mDebugCirclePaint );
		}
	}

	// 만년필처럼 그리기
	// return 값:
	//		다음 startWith가 될 값
	private void drawFountainPen(Canvas canvas, float scale, float offset_x, float offset_y ) 
	{
//		Assert.assertTrue( mN>=2 );
		
		final float scaled_pen_thickness = getScaledPenThickness( mPenThickness, scale );
		float x0, x1, x2, x3, y0, y1, y2, y3, p0, p1, p2, p3;
		float vx01, vy01, vx21, vy21; // unit tangent vectors 0->1 and 1<-2
		float norm;
		float n_x0, n_y0, n_x2, n_y2; // the normals

		// the first actual point is treated as a midpoint
		x0 = mX[ 0 ] * scale + offset_x + 0.1f;
		y0 = mY[ 0 ] * scale + offset_y;
		p0 = mFP[ 0 ];

		x1 = mX[ 1 ] * scale + offset_x + 0.1f;
		y1 = mY[ 1 ] * scale + offset_y;
		p1 = mFP[ 1 ];
		
		vx01 = x1 - x0;
		vy01 = y1 - y0;
		// instead of dividing tangent/norm by two, we multiply norm by 2
		norm = (float) Math.sqrt(vx01 * vx01 + vy01 * vy01 + 0.0001f) * 2f;
		vx01 = vx01 / norm * scaled_pen_thickness * p0;
		vy01 = vy01 / norm * scaled_pen_thickness * p0;
		n_x0 = vy01;
		n_y0 = -vx01;
		
		for ( int i=2; i<mN-1; i++ ) 
		{
			// (x0,y0) and (x2,y2) are midpoints, (x1,y1) and (x3,y3) are actual
			// points
			x3 = mX[i] * scale + offset_x + 0.1f;
			y3 = mY[i] * scale + offset_y;
			p3 = mFP[i];
//			p3 = mDP[i] * mFP[i];

			x2 = (x1 + x3) / 2f;
			y2 = (y1 + y3) / 2f;
			p2 = (p1 + p3) / 2f;
			vx21 = x1 - x2;
			vy21 = y1 - y2;
			norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0001f) * 2f;
			vx21 = vx21 / norm * scaled_pen_thickness * p2;
			vy21 = vy21 / norm * scaled_pen_thickness * p2;
			n_x2 = -vy21;
			n_y2 = vx21;

			mDrawPath.rewind();
			mDrawPath.moveTo(x0 + n_x0, y0 + n_y0);
			// The + boundary of the stroke
			mDrawPath.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
			// round out the cap
			mDrawPath.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2	- vx21, y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
			// THe - boundary of the stroke
			mDrawPath.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
			// round out the other cap
			mDrawPath.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
			canvas.drawPath(mDrawPath, mFountainPaint);

			x0 = x2;
			y0 = y2;
			p0 = p2;
			x1 = x3;
			y1 = y3;
			p1 = p3;
			vx01 = -vx21;
			vy01 = -vy21;
			n_x0 = n_x2;
			n_y0 = n_y2;
		}
		
		// the last actual point is treated as a midpoint
		x2 = mX[ mN-1 ] * scale + offset_x + 0.1f;
		y2 = mY[ mN-1 ] * scale + offset_y;
		p2 = mFP[ mN-1 ];

		vx21 = x1 - x2;
		vy21 = y1 - y2;
		norm = (float) Math.sqrt(vx21 * vx21 + vy21 * vy21 + 0.0001f) * 2f;
		vx21 = vx21 / norm * scaled_pen_thickness * p2;
		vy21 = vy21 / norm * scaled_pen_thickness * p2;
		n_x2 = -vy21;
		n_y2 = vx21;

		mDrawPath.rewind();
		mDrawPath.moveTo(x0 + n_x0, y0 + n_y0);
		mDrawPath.cubicTo(x1 + n_x0, y1 + n_y0, x1 + n_x2, y1 + n_y2, x2 + n_x2, y2 + n_y2);
		mDrawPath.cubicTo(x2 + n_x2 - vx21, y2 + n_y2 - vy21, x2 - n_x2 - vx21, 	y2 - n_y2 - vy21, x2 - n_x2, y2 - n_y2);
		mDrawPath.cubicTo(x1 - n_x2, y1 - n_y2, x1 - n_x0, y1 - n_y0, x0 - n_x0, y0 - n_y0);
		mDrawPath.cubicTo(x0 - n_x0 - vx01, y0 - n_y0 - vy01, x0 + n_x0 - vx01, y0 + n_y0 - vy01, x0 + n_x0, y0 + n_y0);
		canvas.drawPath(mDrawPath, mFountainPaint);
	}

	public void drawWriteStroke( Canvas canvas, float scale, float offset_x, float offset_y, Paint oPaint)
	{
		drawWriteStroke( canvas, scale, offset_x, offset_y, oPaint, 0);
	}

	public void drawWriteStroke( Canvas canvas, float scale, float offset_x, float offset_y, Paint oPaint, int alphaStrokeWidth)
	{
		Path path = new Path();
		for(int k = 0; k < mN; k++)
		{
			if(k == 0)
			{
				path.reset();
				path.moveTo(mX[k]*scale + offset_x, mY[k] * scale + offset_y );
			}
			else
			{
				path.quadTo( mX[k - 1] * scale + offset_x, mY[k - 1] * scale + offset_y, ( ( mX[k - 1] + mX[k] ) / 2 ) * scale + offset_x, ( ( mY[k - 1] + mY[k] ) / 2 ) * scale + offset_y );
				if(k == mN - 1) path.lineTo(mX[k-1]*scale + offset_x, mY[k-1] * scale + offset_y );
			}
		}

		float thickness = mPenThickness;
		final float scaled_pen_thickness = getScaledPenThickness( thickness, scale );
		float p0;
		float force_f = getPressureFactor( 255 );

		p0 = force_f;
		if(alphaStrokeWidth == 0)
			oPaint.setColor( mPenColor );
		oPaint.setStrokeWidth( scaled_pen_thickness * p0 +alphaStrokeWidth);
		canvas.drawPath(path, oPaint );
	}
}