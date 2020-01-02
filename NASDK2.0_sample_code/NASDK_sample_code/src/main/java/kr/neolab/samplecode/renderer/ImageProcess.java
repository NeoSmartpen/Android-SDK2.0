package kr.neolab.samplecode.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;

import kr.neolab.sdk.ink.structure.ControlPoint;
import kr.neolab.sdk.ink.structure.Stroke;

public class ImageProcess {

    // the actual data
    public static float[] mX;
    public static float[] mY;

    public static int[] mP;
    public static int	mN;

    // for rendering
    public static float[] mFP;		// float pressure

    // pen attribute
    protected static float mPenThickness = 1;
    protected static int mPenAlpha = 255;

    // for line drawing
    protected static final Paint linePaint = new Paint();

    protected  static void readyToDraw(float[] xs, float[] ys, int[] ps)
    {

        // 획에 관련된 데이터를 복사
        mN = xs.length;
        mX = xs;
        mY = ys;
        mP = ps;

        mFP = new float[mN];
    }

    public static void setPenType(float penThickness, int penColor)
    {
        mPenThickness = penThickness;

        linePaint.setColor( penColor );
        linePaint.setAlpha( mPenAlpha );
        linePaint.setAntiAlias( true );
        linePaint.setStyle( Paint.Style.STROKE );
        linePaint.setStrokeCap( Paint.Cap.ROUND );
        linePaint.setStrokeWidth( mPenThickness );
        linePaint.setStrokeJoin( Paint.Join.ROUND );
    }

    private static void drawStrokeLine( Canvas canvas, float scale, float offset_x, float offset_y, List<ControlPoint> mid )
    {
        int count = mid.size();

        if ( count > 2 )
            PathControl.simplify( mid, mPenThickness );

        if ( count > 2 )
            PathControl.removeTail( mid, mPenThickness );

        Path p = PathControl.getBezierPath( mid, scale, offset_x, offset_y, false );
        canvas.drawPath( p, linePaint );
    }

    /**
     * @author HRHWANG@neolab.net
     */
    public static void drawStroke(Canvas canvas, Stroke stroke, float scale, float offset_x, float offset_y )
    {
        List<ControlPoint> pointList = stroke.getOutput();
        drawStrokeLine( canvas, scale, offset_x, offset_y, pointList );

        return;
    }
}
