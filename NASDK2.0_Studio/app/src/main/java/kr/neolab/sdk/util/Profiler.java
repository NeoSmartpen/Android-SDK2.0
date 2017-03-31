package kr.neolab.sdk.util;

import java.util.ArrayList;

import android.os.SystemClock;
import android.util.Log;

/**
 * The type Profiler.
 */
public class Profiler
{
	private static final String TAG = "Profiler";

	private ArrayList<String> labels;

	private ArrayList<Long> record;

	private boolean status;

	private long previous, current, total;

	/**
	 * The P type.
	 */
	public ProfileType pType;

	/**
	 * The enum Profile type.
	 */
	public static enum ProfileType
	{
		/**
		 * Elapsed realtime profile type.
		 */
		ElapsedRealtime, /**
	 * Current time millis profile type.
	 */
	CurrentTimeMillis, /**
	 * Memory usage profile type.
	 */
	MemoryUsage
	}

	/**
	 * Instantiates a new Profiler.
	 *
	 * @param clocktype the clocktype
	 */
	public Profiler( ProfileType clocktype )
	{
		this.record = new ArrayList<Long>();
		this.labels = new ArrayList<String>();
		this.status = false;
		this.pType = clocktype;
	}

	private long getCurrent()
	{
		if ( pType == ProfileType.ElapsedRealtime )
		{
			return SystemClock.elapsedRealtime();
		}
		else if ( pType == ProfileType.CurrentTimeMillis )
		{
			return System.currentTimeMillis();
		}
		else
		{
			return Runtime.getRuntime().freeMemory();
		}
	}

	/**
	 * Start.
	 */
	public void start()
	{
		if ( record.size() > 0 )
		{
			record.clear();
		}

		status = true;
		previous = getCurrent();
	}

	/**
	 * Lap.
	 *
	 * @param label the label
	 */
	public void lap( String label )
	{
		if ( status )
		{
			current = getCurrent();
			long lapRecord = current - previous;
			total += lapRecord;
			labels.add( label );
			record.add( lapRecord );
			previous = current;
		}
	}

	/**
	 * Stop.
	 */
	public void stop()
	{
		lap( "end" );
		status = false;
	}

	/**
	 * Report.
	 */
	public void report()
	{
		String report = "";

		for ( int i = 0; i < record.size(); i++ )
		{
			report += labels.get( i ) + " : " + record.get( i ) + " , ";
		}

		report += "total : " + total + "";

		Log.d( TAG, "[Profiler]" + report );
	}

	/**
	 * Release.
	 */
	public void release()
	{
		if ( record.size() > 0 )
		{
			record.clear();
		}

		record = null;
	}
}