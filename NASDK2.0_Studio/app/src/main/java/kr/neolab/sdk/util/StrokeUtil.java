package kr.neolab.sdk.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import java.util.Locale;

import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.renderer.Renderer;

public class StrokeUtil
{
    public static Bitmap StrokeToImage( Stroke[] strokes, float scale)
    {
        float minX, minY, maxX, maxY, offsetX, offsetY ;
        int width, height;

        if( strokes == null || strokes.length == 0 )
            return null;

        Dot fd = strokes[0].getDots().get( 0 );
        minX = fd.getX();
        minY = fd.getY();
        maxX = (int)fd.getX();
        maxY = (int)fd.getY();

        // get strokes offset, width, height
        for( Stroke stroke : strokes )
        {
            for( Dot d : stroke.getDots() )
            {
                // get min X
                if( minX > d.getX() )
                    minX = d.getX();

                // get min Y
                if( minY > d.getY() )
                    minY = d.getY();


                // get max X
                if( maxX < d.getX() )
                    maxX = (int)d.getX();

                // get max Y
                if( maxY < d.getY() )
                    maxY = (int)d.getY();
            }
        }

        minX *= scale;
        minY *= scale;
        maxX *= scale;
        maxY *= scale;

        // 반올림
        width = (int)((maxX - minX) + 0.5f);
        height = (int)((maxY - minY) + 0.5f);

        offsetX = minX;
        offsetY = minY;

        // 여백 만들기
        width = width + 25;
        height = height + 25;
        offsetX = offsetX - 7;
        offsetY = offsetY - 7;

        Bitmap bitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );
        bitmap.eraseColor( Color.WHITE );
        Canvas canvas = new Canvas(bitmap);

        Renderer.draw( canvas, strokes, scale, -offsetX, -offsetY, Stroke.STROKE_TYPE_PEN );

        return bitmap;
    }

    public static String StrokeToNeoInk( String captureDevice, Stroke s )
    {
        String str = String.format( Locale.getDefault(),
                                    "[\"%s\"," +
                                            " \"%s\"," +
                                            " \"#%06X\"," +
                                            " \"%d\"," +
                                            " \"%d\",",
                                    captureDevice,
                                    s.penTipType== 2 ? "highlight" : "solid",
                                    ( 0xFFFFFF & s.color ),
                                    s.thickness,
                                    s.timeStampStart );

        if( s.getDots().size() > 0 )
            str += "[";


        StringBuilder builder = new StringBuilder( str );
        for( Dot d : s.getDots() )
        {
               String dotStr = String.format( Locale.getDefault(),
                                              "\"%d\"," +
                                                      "\"%.2f\"," +
                                                      "\"%.2f\",\"%.2f\"," +
                                                      "\"%d\",\"%d\"",
                                              d.timestamp - s.timeStampStart,
                                              d.x, d.y,
                                              (float) d.pressure * 100f / 255f,
                                              d.tiltX, d.twist );
               builder.append( dotStr );
        }

        builder.append( "]]" );

        return  builder.toString();
    }
}
