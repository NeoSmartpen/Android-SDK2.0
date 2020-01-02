package kr.neolab.samplecode.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.List;

import kr.neolab.sdk.ink.structure.ControlPoint;

public class PathControl {
    
    protected static double getAngle(ControlPoint p1, ControlPoint p2, ControlPoint p3 ) {
        double a,b,c;
        double angle, temp;

        a = Math.sqrt( Math.pow( p1.x - p3.x, 2 ) + Math.pow( p1.y - p3.y, 2 ) );
        b = Math.sqrt( Math.pow( p1.x - p2.x, 2 ) + Math.pow( p1.y - p2.y, 2 ) );
        c = Math.sqrt( Math.pow( p2.x - p3.x, 2 ) + Math.pow( p2.y - p3.y, 2 ) );

        temp = ( Math.pow( b, 2 ) + Math.pow( c, 2 ) - Math.pow( a, 2 ) ) / ( 2 * b * c );

        angle = Math.acos( temp );
        angle *= ( 180 / Math.PI );

        return angle;
    }

    private static ControlPoint getPoint12( ControlPoint pts1, double t ) {
        double x1 = pts1.x;
        double y1 = pts1.y;
        double x2 = pts1.outX;
        double y2 = pts1.outY;

        double x12 = (x2 - x1) * t + x1;
        double y12 = (y2 - y1) * t + y1;

        return new ControlPoint( x12, y12 );
    }

    private static ControlPoint getPoint34( ControlPoint pts2, double t ) {
        double x3 = pts2.inX;
        double y3 = pts2.inY;
        double x4 = pts2.x;
        double y4 = pts2.y;

        double x34 = (x4 - x3) * t + x3;
        double y34 = (y4 - y3) * t + y3;

        return new ControlPoint( x34, y34 );
    }

    private static ControlPoint getSplitPoint( ControlPoint pts1, ControlPoint pts2, double t ) {

        double x1 = pts1.x;
        double y1 = pts1.y;
        double x2 = pts1.outX;
        double y2 = pts1.outY;
        double x3 = pts2.inX;
        double y3 = pts2.inY;
        double x4 = pts2.x;
        double y4 = pts2.y;

        double x12 = (x2 - x1) * t + x1;
        double y12 = (y2 - y1) * t + y1;

        double x23 = (x3 - x2) * t + x2;
        double y23 = (y3 - y2) * t + y2;

        double x34 = (x4 - x3) * t + x3;
        double y34 = (y4 - y3) * t + y3;

        double x123 = (x23 - x12) * t + x12;
        double y123 = (y23 - y12) * t + y12;

        double x234 = (x34 - x23) * t + x23;
        double y234 = (y34 - y23) * t + y23;

        double x1234 = (x234 - x123) * t + x123;
        double y1234 = (y234 - y123) * t + y123;

        ControlPoint splitPoint = new ControlPoint( x1234, y1234 );
        splitPoint.setIn( x123, y123 );
        splitPoint.setOut( x234, y234 );

        return splitPoint;
    }

    protected static void getSplitPoints( List< ControlPoint > pts, double t, double t_angle ) {

        int count = pts.size();

        if ( count < 3 )
            return;

        double t2 = 1.0 - t;

        int i = 1;
        while ( i < pts.size() - 1 ) {
            double angle = getAngle( pts.get(i-1), pts.get(i), pts.get(i+1) );
            if ( angle < t_angle ) {

                {
                    ControlPoint point12 = getPoint12( pts.get( i-1 ), t2 );
                    ControlPoint point34 = getPoint34( pts.get( i ), t2 );
                    ControlPoint splitPoint = getSplitPoint( pts.get( i-1 ), pts.get( i ), t2 );
                    double d12 = splitPoint.getDistance( pts.get( i-1 ) );
                    double d23 = splitPoint.getDistance( pts.get( i ) );
                    double d123 = d12 + d23;
                    double force = pts.get( i-1 ).force + ( pts.get( i ).force - pts.get( i-1 ).force ) * ( d12 / d123 );

                    splitPoint.force = force;

                    pts.get( i-1 ).setOut( point12 );
                    pts.add( i, splitPoint );
                    pts.get( i+1 ).setIn( point34 );
                }

                {
                    ControlPoint point12 = getPoint12( pts.get( i+1 ), t );
                    ControlPoint point34 = getPoint34( pts.get( i+2 ), t );
                    ControlPoint splitPoint = getSplitPoint( pts.get( i+1 ), pts.get( i+2 ), t );
                    double d12 = splitPoint.getDistance( pts.get( i+1 ) );
                    double d23 = splitPoint.getDistance( pts.get( i+2 ) );
                    double d123 = d12 + d23;
                    double force = pts.get( i+1 ).force + ( pts.get( i+2 ).force - pts.get( i+1 ).force ) * ( d12 / d123 );

                    splitPoint.force = force;

                    pts.get( i+1 ).setOut( point12 );
                    pts.add( i+2, splitPoint );
                    pts.get( i+3 ).setIn( point34 );
                }
                i += 2;

            } else
                i++;
        }
    }

    // old code. do not use.
    protected static Path getBezierPath( List< ControlPoint > pts, float scale, float offset_x, float offset_y, boolean closePath) {

        Path p = new Path();
        p.reset();
        p.moveTo( (float)pts.get( 0 ).x * scale + offset_x, (float)pts.get( 0 ).y* scale + offset_y );
        for ( int i = 0; i < pts.size() - 1; i++ ) {
            p.cubicTo(
                    (float)pts.get( i ).outX* scale + offset_x, 	  		(float)pts.get( i ).outY* scale + offset_y,
                    (float)pts.get( i+1 ).inX* scale + offset_x, 		(float)pts.get( i+1 ).inY* scale + offset_y,
                    (float)pts.get( i+1 ).x* scale + offset_x,   		(float)pts.get( i+1 ).y* scale + offset_y );
        }

        if ( closePath ) {
            p.cubicTo(
                    (float)pts.get( pts.size() - 1 ).outX* scale + offset_x, 	(float)pts.get( pts.size() - 1 ).outY* scale + offset_y,
                    (float)pts.get( 0 ).inX* scale + offset_x, 					(float)pts.get( 0 ).inY* scale + offset_y,
                    (float)pts.get( 0 ).x* scale + offset_x,   					(float)pts.get( 0 ).y* scale + offset_y );
        }

        return p;
    }

    protected static void drawBezierPath2( Canvas canvas, Paint paint, Path p, List< ControlPoint > left, List< ControlPoint > right, boolean closePath ) {

        int count = left.size();

        p.reset();

        float r = (float)left.get( 0 ).getDistance( right.get( 0 ) );
//        canvas.drawCircle( (float)(left.get( 0 ).x + right.get( 0 ).x) / 2 , (float)(left.get( 0 ).y + right.get( 0 ).y) / 2 , r, paint );
        p.addCircle( (float)(left.get( 0 ).x + right.get( 0 ).x) / 2 , (float)(left.get( 0 ).y + right.get( 0 ).y) / 2, r * 0.5f, Path.Direction.CW );

        for ( int i = 0; i < count - 1; ++i ) {

            p.moveTo( (float)left.get( i ).x, (float)left.get( i ).y );
            p.cubicTo(
                    (float)left.get( i ).outX, 	  	(float)left.get( i ).outY,
                    (float)left.get( i+1 ).inX, 	(float)left.get( i+1 ).inY,
                    (float)left.get( i+1 ).x,   	(float)left.get( i+1 ).y );

            p.lineTo( (float)right.get( i+1 ).x, (float)right.get( i+1 ).y );
            p.cubicTo(
                    (float)right.get( i+1 ).inX, 	(float)right.get( i+1 ).inY,
                    (float)right.get( i ).outX, 	(float)right.get( i ).outY,
                    (float)right.get( i ).x,   		(float)right.get( i ).y );

            p.lineTo(  (float)left.get( i ).x,  (float)left.get( i ).y );
        }

        r = (float)left.get( count - 1 ).getDistance( right.get( count - 1 ) );
//        canvas.drawCircle( (float)(left.get( count - 1 ).x + right.get( count - 1 ).x) / 2, (float)(left.get( count - 1 ).y + right.get( count - 1 ).y) / 2, r, paint );

        p.addCircle( (float)(left.get( count - 1 ).x + right.get( count - 1 ).x) / 2, (float)(left.get( count - 1 ).y + right.get( count - 1 ).y) / 2 , r * 0.5f, Path.Direction.CW );
        canvas.drawPath( p, paint );
    }

    private static int isNear( ControlPoint p1, ControlPoint p2, ControlPoint p3, double tp, double tl ) {

        double l12 = p1.getDistance( p2 );
        double l13 = p1.getDistance( p3 );
        double l23 = p2.getDistance( p3 );

        if ( l12 < tp )
            return 2;

        if ( l13 < tp )
            return 3;

        if ( l23 < tp )
            return 2;

        double prj = ( (p3.x - p1.x) * (p2.x - p1.x) + (p3.y - p1.y) * (p2.y - p1.y) ) / l12;
        double d = 0;
        if ( prj < 0 ) {
            d = p1.getDistance( p3 );
            if ( d < tp )
                return 1; // remove p1
        } else if ( prj > l12 ) {
            d = p2.getDistance( p3 );
            if ( d < tp )
                return 2; // remove p2
        } else {
            double area = Math.abs( (p1.x - p3.x) * (p2.y - p3.y) - (p1.y - p3.y) * (p2.x - p3.x) );
            d = area / l12;
            if ( p1.getDistance( p3 ) < tl || p2.getDistance( p3 ) < tl || d < tl / 2 )
                return 3; // remove p3
        }

        return 0;
    }

    protected static void simplify( List< ControlPoint > pts, float maxThickness ) {

        int midIndex = 0;
        double forceTemp;

        while ( midIndex < pts.size() - 2 && pts.size() > 2 ) {

            double t_point = 0;
            double t_line = 0;

            t_point = maxThickness / 2.0 / 56f;
            t_line = maxThickness / 10.0 / 56f;

            int result = isNear( pts.get(midIndex), pts.get(midIndex + 2), pts.get(midIndex + 1), t_point, t_line );

            switch (result) {
                case 0:
                    midIndex++;
                    break;
                case 1:
                    forceTemp = pts.get( midIndex ).force > pts.get( midIndex + 1 ).force
                            ? pts.get( midIndex ).force : pts.get( midIndex + 1 ).force;
                    pts.get( midIndex ).set( pts.get( midIndex + 1 ) );
                    pts.get( midIndex ).force = forceTemp;
                    pts.remove( midIndex + 1 );
                    break;
                case 2:
                    forceTemp = pts.get( midIndex + 1 ).force > pts.get( midIndex + 2 ).force
                            ? pts.get( midIndex + 1 ).force : pts.get( midIndex + 2 ).force;
                    pts.get( midIndex + 2 ).set( pts.get( midIndex + 1 ) );
                    pts.get( midIndex + 2 ).force = forceTemp;
                    pts.remove( midIndex + 1 );
                    break;
                case 3:
                    pts.get( midIndex ).force = pts.get( midIndex ).force > pts.get( midIndex + 1 ).force
                            ? pts.get( midIndex ).force : pts.get( midIndex + 1 ).force;
                    pts.get( midIndex + 2 ).force = pts.get( midIndex + 1 ).force > pts.get( midIndex + 2 ).force
                            ? pts.get( midIndex + 1 ).force : pts.get( midIndex + 2 ).force;
                    pts.remove( midIndex + 1 );
                    break;
            }
        }
    }

    protected static void removeTail( List< ControlPoint > pts, float maxThickness ) {

        double distTailMax = 0;

        distTailMax = maxThickness / 2 / 56f;

        boolean findFirstTail = true;
        int firstTailPoint = 0;
        int lastTailPoint = 0;
        double distFirstTail = 0;
        double finalDistFirstTail = 0;
        double distLastTail = 0;
        double lastTailAngle = 0;

        for ( int i = 0; i < pts.size() - 2; ++i ) {
            double angle = Math.abs( PathControl.getAngle( pts.get(i), pts.get(i+1), pts.get(i+2) ) );
            distLastTail += pts.get(i+1).getDistance( pts.get(i+2) );

            if ( findFirstTail )
                distFirstTail += pts.get(i).getDistance( pts.get(i+1) );

            if ( angle < 90 ) {
                lastTailPoint = i + 1;
                distLastTail = pts.get(i+1).getDistance( pts.get(i+2) );
                lastTailAngle = angle;

                if ( distFirstTail >= (180.0 - angle) / 90.0 * distTailMax )
                    findFirstTail = false;
                else if ( findFirstTail ) {
                    firstTailPoint = i + 1;
                    finalDistFirstTail = distFirstTail;
                }
            }
        }


        if ( firstTailPoint != 0 ) {
            if ( finalDistFirstTail > 0 && firstTailPoint < pts.size() ) {
                pts = pts.subList( firstTailPoint, pts.size() );
                lastTailPoint -= firstTailPoint;
            }
        }

        if ( lastTailPoint != 0 ) {
            if ( distLastTail < (180.0 - lastTailAngle) / 90.0 * distTailMax && lastTailPoint < pts.size() )
                pts = pts.subList( 0, lastTailPoint + 1 );
        }
    }

    // for debugging
//    protected static void drawControlPath( Graphics2D g, GeneralPath p, List< ControlPoint > pts, Color colorLine, Color colorPoint ) {
//
//        if ( colorLine == null )
//            g.setPaint( Color.yellow );
//        else
//            g.setPaint( colorLine );
//        p.reset();
//
//        for ( int i = 0; i < pts.size(); i++ ) {
//            p.moveTo( pts.get( i ).inX, pts.get( i ).inY );
//            p.lineTo( pts.get( i ).outX, pts.get( i ).outY );
//        }
//        g.draw( p );
//
//        if ( colorPoint == null )
//            g.setPaint( Color.cyan );
//        else
//            g.setPaint( colorPoint );
//
//        for ( int i = 0; i < pts.size(); i++ ) {
//            g.drawRect( ( int )( pts.get( i ).outX - 1 ), ( int )( pts.get( i ).outY - 1 ), 3, 3 );
//            g.drawRect( ( int )( pts.get( i ).inX - 1 ), ( int )( pts.get( i ).inY - 1 ), 3, 3 );
//        }
//    }
//
//    // for debugging
//    protected static void drawControlPoint( Graphics2D g, GeneralPath p, List< ControlPoint > pts, Color color ) {
//
//        if ( color == null )
//            g.setPaint( Color.CYAN );
//        else
//            g.setPaint( color );
//        p.reset();
//
//        for ( int i = 0; i < pts.size(); i++ ) {
//            //if ( i % 2 == 0 ) g.setPaint( Color.red );
//            //else g.setPaint( color );
//            g.drawRect( ( int )pts.get( i ).x - 0, ( int )pts.get( i ).y - 0, 1, 1 );
//        }
//
//    }
}