package kr.neolab.sdk.pen.bluetooth.lib;

import android.graphics.PointF;

/**
 * Created by LMS on 2017-11-28.
 * To modify this restriction rule, be sure to record the revision history.
 */
public class PenProfile
{
    /**
     * The constant LIMIT_BYTE_LENGTH_PROFILE_NAME.
     */
    public static final int LIMIT_BYTE_LENGTH_PROFILE_NAME = 8;
    /**
     * The constant LIMIT_BYTE_LENGTH_PASSWORD.
     */
    public static final int LIMIT_BYTE_LENGTH_PASSWORD = 8;
    /**
     * The constant LIMIT_BYTE_LENGTH_KEY.
     */
    public static final int LIMIT_BYTE_LENGTH_KEY = 16;

    /**
     * The constant LIMIT_BYTE_LENGTH_PEN_NAME.
     */
    public static final int LIMIT_BYTE_LENGTH_PEN_NAME = 72;
    /**
     * The constant LIMIT_BYTE_LENGTH_PEN_STROKE_THICKNESS.
     */
    public static final int LIMIT_BYTE_LENGTH_PEN_STROKE_THICKNESS = 1;
    /**
     * The constant LIMIT_BYTE_LENGTH_PEN_COLOR_INDEX.
     */
    public static final int LIMIT_BYTE_LENGTH_PEN_COLOR_INDEX = 1;
    /**
     * The constant LIMIT_BYTE_LENGTH_PEN_COLOR_AND_HISTORY.
     */
    public static final int LIMIT_BYTE_LENGTH_PEN_COLOR_AND_HISTORY = 4*5*10;
    /**
     * The constant LIMIT_BYTE_LENGTH_USER_CALIBRATION.
     */
    public static final int LIMIT_BYTE_LENGTH_USER_CALIBRATION = 2*2*3;
    /**
     * The constant LIMIT_BYTE_LENGTH_PEN_BRUSH_TYPE.
     */
    public static final int LIMIT_BYTE_LENGTH_PEN_BRUSH_TYPE = 1;
    /**
     * The constant LIMIT_BYTE_LENGTH_PEN_TIP_TYPE.
     */
    public static final int LIMIT_BYTE_LENGTH_PEN_TIP_TYPE = 1;

    /**
     * The constant KEY_PEN_NAME.
     */
/////////////////
    // Key
    public static final String KEY_PEN_NAME = "N_name";
    /**
     * The constant KEY_PEN_STROKE_THICKNESS_LEVEL.
     */
    public static final String KEY_PEN_STROKE_THICKNESS_LEVEL = "N_thickness";
    /**
     * The constant KEY_PEN_COLOR_INDEX.
     */
    public static final String KEY_PEN_COLOR_INDEX = "N_color_index";
    /**
     * The constant KEY_PEN_COLOR_AND_HISTORY.
     */
    public static final String KEY_PEN_COLOR_AND_HISTORY = "N_color";
    /**
     * The constant KEY_USER_CALIBRATION.
     */
    public static final String KEY_USER_CALIBRATION = "N_pressure";
    /**
     * The constant KEY_PEN_BRUSH_TYPE.
     */
    public static final String KEY_PEN_BRUSH_TYPE = "N_brush";
    /**
     * The constant KEY_PEN_TIP_TYPE.
     */
    public static final String KEY_PEN_TIP_TYPE = "N_pt_change1";
    /**
     * The constant KEY_DEFAULT_CALIBRATION.
     */
    public static final String KEY_DEFAULT_CALIBRATION = "N_default_cali";

    /**
     * The constant PROFILE_CREATE.
     */
/////////////////
    // request type
    public static final byte PROFILE_CREATE = 0x01;
    /**
     * The constant PROFILE_DELETE.
     */
    public static final byte PROFILE_DELETE = 0x02;
    /**
     * The constant PROFILE_INFO.
     */
    public static final byte PROFILE_INFO = 0x03;
    /**
     * The constant PROFILE_READ_VALUE.
     */
    public static final byte PROFILE_READ_VALUE = 0x12;
    /**
     * The constant PROFILE_WRITE_VALUE.
     */
    public static final byte PROFILE_WRITE_VALUE = 0x11;
    /**
     * The constant PROFILE_DELETE_VALUE.
     */
    public static final byte PROFILE_DELETE_VALUE = 0x13;

    /**
     * The constant PROFILE_STATUS_SUCCESS.
     */
///////////////////
    // status
    public static final byte PROFILE_STATUS_SUCCESS = 0x00;
    /**
     * The constant PROFILE_STATUS_FAILURE.
     */
    public static final byte PROFILE_STATUS_FAILURE = 0x01;
    /**
     * The constant PROFILE_STATUS_EXIST_PROFILE_ALREADY.
     */
    public static final byte PROFILE_STATUS_EXIST_PROFILE_ALREADY = 0x10;
    /**
     * The constant PROFILE_STATUS_NO_EXIST_PROFILE.
     */
    public static final byte PROFILE_STATUS_NO_EXIST_PROFILE = 0x11;
    /**
     * The constant PROFILE_STATUS_NO_EXIST_KEY.
     */
//    public static final byte PROFILE_STATUS_EXIST_KEY_ALREADY = 0x20;
    public static final byte PROFILE_STATUS_NO_EXIST_KEY = 0x21;
    /**
     * The constant PROFILE_STATUS_NO_PERMISSION.
     */
    public static final byte PROFILE_STATUS_NO_PERMISSION = 0x30;
    /**
     * The constant PROFILE_STATUS_BUFFER_SIZE_ERR.
     */
    public static final byte PROFILE_STATUS_BUFFER_SIZE_ERR = 0x40;

    /**
     * Get calibrate factor float [ ].
     *
     * @param cPX1 the c px 1
     * @param cPY1 the c py 1
     * @param cPX2 the c px 2
     * @param cPY2 the c py 2
     * @param cPX3 the c px 3
     * @param cPY3 the c py 3
     * @return the float [ ]
     */
    public static float[] getCalibrateFactor(int cPX1, int cPY1, int cPX2, int cPY2, int cPX3, int cPY3)
    {
        PointF[] pCurve = new PointF[2000];
        float[] mModifyPressureFactor = new float[256];

        PointF[] point = new PointF[4];
        point[0] = new PointF(cPX1,cPY1);
        point[1] = new PointF(cPX2,cPY2);
        point[2] = new PointF(cPX2,cPY2);
        point[3] = new PointF(cPX3,cPY3);
        ComputeBezier(  point, 2000, pCurve);

        int count = 0;
        int prevCount = 0;
        for(int i = 0; i < pCurve.length; i++)
        {
            if(i == pCurve.length-1)
            {
                mModifyPressureFactor[255] = pCurve[i].y;
                if(mModifyPressureFactor[255] > 255) mModifyPressureFactor[255]=255;
            }
            else
            {
                if(count < pCurve[i].x)
                {
                    if(Math.abs( pCurve[prevCount].x -  count) > Math.abs( pCurve[i].x -  count))
                        mModifyPressureFactor[count] = pCurve[i].y;
                    else
                        mModifyPressureFactor[count] = pCurve[prevCount].y;
                    if(mModifyPressureFactor[count] > 255) mModifyPressureFactor[count]=255;
                    count++;
                    if(count > 255)
                        count = 255;
                }
                else
                {
                    prevCount = count;
                }
            }
        }
//        for(int i = 0 ;i < mModifyPressureFactor.length ; i++)
//        {
//            NLog.d( "test mPressureFactor"+i+":"+mModifyPressureFactor[i] );
//
//        }

        return mModifyPressureFactor;

    }

    private static void ComputeBezier( PointF[] cp, int numberOfPoints, PointF[] curve )
    {
        float dt;
        int i;

        dt = 1.0f / ( numberOfPoints - 1 );

        for( i = 0; i < numberOfPoints; i++)
            curve[i] = PointOnCubicBezier( cp, i*dt );
    }

    private static PointF PointOnCubicBezier( PointF[] cp, float t )
    {
        float ax, bx, cx;
        float ay, by, cy;
        float tSquared, tCubed;
        PointF result = new PointF();

     /* Calculate polynomial coefficients */
        cx = 3.0f * (cp[1].x - cp[0].x);
        bx = 3.0f * (cp[2].x - cp[1].x) - cx;
        ax = cp[3].x - cp[0].x - cx - bx;

        cy = 3.0f * (cp[1].y - cp[0].y);
        by = 3.0f * (cp[2].y - cp[1].y) - cy;
        ay = cp[3].y - cp[0].y - cy - by;

     /* Calculate the curve point at the parameter value t */
        tSquared = t * t;
        tCubed = tSquared * t;

        result.x = (ax * tCubed) + (bx * tSquared) + (cx * t) +
                cp[0].x;
        result.y = (ay * tCubed) + (by * tSquared) + (cy * t) +
                cp[0].y;

        return result;
    }
}
