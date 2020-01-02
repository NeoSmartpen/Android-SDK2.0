package kr.neolab.samplecode.renderer;

import android.graphics.Canvas;
import android.graphics.Color;

import kr.neolab.sdk.ink.structure.Stroke;

public class Renderer2
{
    static private int color = Color.BLACK;

    /**
     * draw to canvas object
     *
     * @param canvas Canvas object to draw stroke
     * @param stroke draw stroke
     * @param scale To zoom in or out of scale for stroke
     * @param offset_x coordinates by moving the draw stroke
     * @param offset_y coordinates by moving the draw stroke
     */
    public static void draw( Canvas canvas, Stroke stroke, float scale, float offset_x, float offset_y )
    {
        draw( canvas, stroke, scale, offset_x, offset_y, 1, color );
    }

    /**
     * draw to canvas object
     *
     * @param canvas Canvas object to draw stroke
     * @param stroke draw stroke
     * @param scale To zoom in or out of scale for stroke
     * @param offset_x coordinates by moving the draw stroke
     * @param offset_y coordinates by moving the draw stroke
     * @param width width of complete stroke. pixel.
     * @param color color of complete stroke
     */
    public static void draw( Canvas canvas, Stroke stroke, float scale, float offset_x, float offset_y, float width, int color )
    {
        int length = stroke.getDots().size();

        if (length <= 0)
            return;

        float[] xs = new float[length];
        float[] ys = new float[length];
        int[] ps = new int[length];

        for (int j = 0; j < length; j++) {
            xs[j] = stroke.getDots().get(j).x;
            ys[j] = stroke.getDots().get(j).y;
            ps[j] = stroke.getDots().get(j).pressure;
        }

        ImageProcess.readyToDraw(xs, ys, ps);
        ImageProcess.setPenType(width, color);

        ImageProcess.drawStroke(canvas, stroke, scale, offset_x, offset_y);
    }

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
        draw( canvas, strokes, scale, offset_x, offset_y, 1, color );
    }


    /**
     * draw to canvas object
     *
     * @param canvas Canvas object to draw stroke
     * @param strokes A collection of draw stroke
     * @param scale To zoom in or out of scale for stroke
     * @param offset_x coordinates by moving the draw stroke
     * @param offset_y coordinates by moving the draw stroke
     * @param width width of complete stroke. pixel
     * @param color color of complete stroke
     */
    public static void draw( Canvas canvas, Stroke[] strokes, float scale, float offset_x, float offset_y, float width, int color )
    {
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

            ImageProcess.readyToDraw( xs, ys, ps );
            ImageProcess.setPenType( width, color );

            ImageProcess.drawStroke( canvas, s, scale, offset_x, offset_y );
        }
    }

}
