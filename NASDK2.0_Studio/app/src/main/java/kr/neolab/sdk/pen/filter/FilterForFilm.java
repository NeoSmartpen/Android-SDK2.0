package kr.neolab.sdk.pen.filter;

import kr.neolab.sdk.ink.structure.DotType;

/**
 * The type Filter for film.
 */
public class FilterForFilm
{

	private static final int delta = 2;
	private static final int filmSizeX = 60, filmSizeY = 90;

	private Fdot fdot1, fdot2;
	private Fdot makeDownDot,makeMoveDot;
	private boolean fsecondCheck = true, fthirdCheck = true;

	private IFilterListener listener = null;

	private static final int MAX_OWNER = 1024;
	private static final int MAX_NOTE_ID = 16384;
	private static final int MAX_PAGE_ID = 262143;

	/**
	 * Instantiates a new Filter for film.
	 *
	 * @param listener the listener
	 */
	public FilterForFilm( IFilterListener listener )
	{
		super();
		this.listener = listener;
	}

	/**
	 * Put.
	 *
	 * @param mdot the mdot
	 */
	public synchronized void put( Fdot mdot )
	{
		if ( !validateCode( mdot ) )
		{
			return;
		}

		// Start Dot is put in the first dot.
		if ( DotType.isPenActionDown( mdot.dotType ) )
		{
			fdot1 = mdot;
		}

		// Middle dot inserts the second and verifies from the third
		// First dot validation failure second -> first, current -> second
		// Successful first dot verification
		else if ( DotType.isPenActionMove( mdot.dotType ) )
		{
			// Just put it in the middle of the first
			if ( fsecondCheck )
			{
				fdot2 = mdot;
				fsecondCheck = false;
			}
			// Middle next Dot checks Middle validation check when first verification succeeds, and next Dot when failure
			else if ( fthirdCheck )
			{
				if ( validateStartDot( fdot1, fdot2, mdot ) )
				{
					listener.onFilteredDot( fdot1 );

					if ( validateMiddleDot( fdot1, fdot2, mdot ) )
					{
						listener.onFilteredDot( fdot2 );
						fdot1 = fdot2;
						fdot2 = mdot;
					}
					else
					{
						fdot2 = mdot;
					}
				}
				else
				{
					if( DotType.isPenActionDown( fdot1.dotType ))
					{
						fdot2.dotType = DotType.PEN_ACTION_DOWN.getValue();
					}

					fdot1 = fdot2;
					fdot2 = mdot;
				}

				fthirdCheck = false;
			}
			else
			{
				if ( validateMiddleDot( fdot1, fdot2, mdot ) )
				{
					listener.onFilteredDot( fdot2 );
					fdot1 = fdot2;
					fdot2 = mdot;
				}
				else
				{
					fdot2 = mdot;
				}
			}

		}
		else if ( DotType.isPenActionUp( mdot.dotType ) )
		{
			boolean validateStartDot = true;
			boolean validateMiddleDot = true;
			//If only one dot is entered and only one Down 1, Move 1, End is entered
			// (Even though only one dot is entered through A_DotData in CommProcessor, Move 1, End 1 data is passed to actual processDot through A_DotUpDownData.)
			if(fsecondCheck)
			{
//				fdot2 = fdot1;
				fdot2 = new Fdot(fdot1.x, fdot1.y, fdot1.pressure, DotType.PEN_ACTION_MOVE.getValue(), fdot1.timestamp, fdot1.sectionId, fdot1.ownerId, fdot1.noteId, fdot1.pageId, fdot1.color, fdot1.penTipType , fdot1.tiltX, fdot1.tiltY, fdot1.twist) ;
			}
			if(fthirdCheck && DotType.isPenActionDown( fdot1.dotType ) )
			{
				if ( validateStartDot( fdot1, fdot2, mdot ) )
				{
					listener.onFilteredDot( fdot1 );
				}
				else
				{
					validateStartDot = false;
				}
			}

			// Middle Dot Verification
			if ( validateMiddleDot( fdot1, fdot2, mdot ) )
			{

				if(!validateStartDot)
				{
					makeDownDot = new Fdot(fdot2.x, fdot2.y, fdot2.pressure, DotType.PEN_ACTION_DOWN.getValue(), fdot2.timestamp, fdot2.sectionId, fdot2.ownerId, fdot2.noteId, fdot2.pageId, fdot2.color, fdot2.penTipType , fdot2.tiltX, fdot2.tiltY, fdot2.twist) ;
					listener.onFilteredDot( makeDownDot );
				}

				listener.onFilteredDot( fdot2 );
			}
			else
			{
				validateMiddleDot = false;
			}

			// Last Dot Verification
			if ( validateEndDot( fdot1, fdot2, mdot ) )
			{
				if(!validateStartDot && !validateMiddleDot)
				{
					makeDownDot = new Fdot(mdot.x, mdot.y, mdot.pressure, DotType.PEN_ACTION_DOWN.getValue(), mdot.timestamp, mdot.sectionId, mdot.ownerId, mdot.noteId, mdot.pageId, mdot.color, mdot.penTipType , mdot.tiltX, mdot.tiltY, mdot.twist) ;
					listener.onFilteredDot( makeDownDot );
				}
				if(fthirdCheck && !validateMiddleDot)
				{
					makeMoveDot = new Fdot(mdot.x, mdot.y, mdot.pressure, DotType.PEN_ACTION_MOVE.getValue(), mdot.timestamp, mdot.sectionId, mdot.ownerId, mdot.noteId, mdot.pageId, mdot.color, mdot.penTipType , mdot.tiltX, mdot.tiltY, mdot.twist) ;
					listener.onFilteredDot( makeMoveDot );
				}
				listener.onFilteredDot( mdot );
			}
			else
			{
				fdot2.dotType = DotType.PEN_ACTION_UP.getValue();
				listener.onFilteredDot( fdot2 );
			}

			// Dot and variable initialization
			fdot1 = new Fdot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			fdot2 = new Fdot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			fsecondCheck = true;
			fthirdCheck = true;
		}
	}

	private boolean validateCode( Fdot dot )
	{
		if ( MAX_NOTE_ID < dot.noteId || MAX_PAGE_ID < dot.pageId )
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	// ==============================================
	// Use 3 points
	// Directionality and Delta X, Delta Y
	// ==============================================

	private boolean validateStartDot( Fdot dot1, Fdot dot2, Fdot dot3 )
	{
		if ( dot1.x > filmSizeX || dot1.x < 1 )
			return false;

		if ( dot1.y > filmSizeY || dot1.y < 1 )
			return false;

		if ( (dot3.x - dot1.x) * (dot2.x - dot1.x) > 0 && Math.abs( dot3.x - dot1.x ) > delta && Math.abs( dot1.x - dot2.x ) > delta )
		{
			return false;
		}
		else if ( (dot3.y - dot1.y) * (dot2.y - dot1.y) > 0 && Math.abs( dot3.y - dot1.y ) > delta && Math.abs( dot1.y - dot2.y ) > delta )
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	private boolean validateMiddleDot( Fdot dot1, Fdot dot2, Fdot dot3 )
	{
		if ( dot2.x > filmSizeX || dot2.x < 1 )
			return false;

		if ( dot2.y > filmSizeY || dot2.y < 1 )
			return false;

		if ( (dot1.x - dot2.x) * (dot3.x - dot2.x) > 0 && Math.abs( dot1.x - dot2.x ) > delta && Math.abs( dot3.x - dot2.x ) > delta )
		{
			return false;
		}
		else if ( (dot1.y - dot2.y) * (dot3.y - dot2.y) > 0 && Math.abs( dot1.y - dot2.y ) > delta && Math.abs( dot3.y - dot2.y ) > delta )
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	private boolean validateEndDot( Fdot dot1, Fdot dot2, Fdot dot3 )
	{
		if ( dot3.x > filmSizeX || dot3.x < 1 )
			return false;

		if ( dot3.y > filmSizeY || dot3.y < 1 )
			return false;

		if ( (dot3.x - dot1.x) * (dot3.x - dot2.x) > 0 && Math.abs( dot3.x - dot1.x ) > delta && Math.abs( dot3.x - dot2.x ) > delta )
		{
			return false;
		}
		else if ( (dot3.y - dot1.y) * (dot3.y - dot2.y) > 0 && Math.abs( dot3.y - dot1.y ) > delta && Math.abs( dot3.y - dot2.y ) > delta )
		{
			return false;
		}
		else
		{
			return true;
		}
	}
}
