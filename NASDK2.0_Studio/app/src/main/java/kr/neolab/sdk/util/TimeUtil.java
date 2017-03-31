package kr.neolab.sdk.util;

import java.util.TimeZone;

/**
 * Created by LMS on 2016-02-18.
 */
public class TimeUtil
{
    /**
     * Convert local time to utc long.
     *
     * @param pv_localDateTime the pv local date time
     * @return the long
     */
// Local Time -> UTC/GMT Time
    public static long convertLocalTimeToUTC(long pv_localDateTime)
    {
        long lv_UTCTime = pv_localDateTime;

        TimeZone z = TimeZone.getDefault();
        //int offset = z.getRawOffset(); // The offset not includes daylight savings time
        int offset = z.getOffset(pv_localDateTime); // The offset includes daylight savings time
        lv_UTCTime = pv_localDateTime - offset;
        return lv_UTCTime;
    }

    /**
     * Convert utc to local time long.
     *
     * @param pv_UTCDateTime the pv utc date time
     * @return the long
     */
// UTC/GMT Time -> Local Time
    public static long convertUTCToLocalTime(long pv_UTCDateTime)
    {
        long lv_localDateTime = pv_UTCDateTime;

        TimeZone z = TimeZone.getDefault();
        //int offset = z.getRawOffset(); // The offset not includes daylight savings time
        int offset = z.getOffset(pv_UTCDateTime); // The offset includes daylight savings time

        lv_localDateTime = pv_UTCDateTime + offset;

        return lv_localDateTime;
    }
}
