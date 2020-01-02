package kr.neolab.sdk.ink.structure;

public class ControlPoint {

    public double x;
    public double y;
    public double inX;
    public double inY;
    public double outX;
    public double outY;
    public double force;

    public ControlPoint() {
        x = y = inX = inY = outX = outY = force = 0;
    }

    public ControlPoint( double x, double y ) {
        this.x = x;
        this.y = y;
    }

    public ControlPoint( double x, double y, double force ) {
        this.x = x;
        this.y = y;
        this.force = force;
    }

    public ControlPoint( ControlPoint p ) {
        x = p.x;
        y = p.y;
        force = p.force;
    }

    public ControlPoint( ControlPoint p, double force ) {
        x = p.x;
        y = p.y;
        this.force = force;
    }

    public ControlPoint( Dot dot )
    {
        x = dot.x;
        y = dot.y;
        force = dot.pressure;
    }

    public void set( double x, double y ) {
        this.x = x;
        this.y = y;
    }

    public void set( double x, double y, double force ) {
        this.x = x;
        this.y = y;
        this.force = force;
    }

    public void set( ControlPoint p ) {
        this.x = p.x;
        this.y = p.y;
        this.force = p.force;
    }

    public void set( Dot dot ) {
        this.x = dot.x;
        this.y = dot.y;
        this.force = dot.pressure;
    }

    public void setIn( double x, double y ) {
        inX = x;
        inY = y;
    }

    public void setIn( ControlPoint p ) {
        inX = p.x;
        inY = p.y;
    }

    public void setOut( double x, double y ) {
        outX = x;
        outY = y;
    }

    public void setOut( ControlPoint p ) {
        outX = p.x;
        outY = p.y;
    }

    public double getDistance( ControlPoint p ) {
        double dx = p.x - x;
        double dy = p.y - y;

        return Math.sqrt( dx*dx + dy*dy );
    }

    public double getDistanceInOut() {
        double dx = inX - outX;
        double dy = inY - outY;

        return Math.sqrt( dx*dx + dy*dy );
    }
}

