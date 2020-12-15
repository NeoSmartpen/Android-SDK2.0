package kr.neolab.sdk.pen.filter;

import kr.neolab.sdk.ink.structure.DotType;

/**
 * The type Filter for paper.
 */
public class FilterForPaper
{
	private static final int delta = 10;

	private Fdot dot1, dot2;
	private Fdot makeDownDot,makeMoveDot;
	private boolean firstCheck = true, secondCheck = true, thirdCheck = true;

	private static final int MAX_X = 15070, MAX_Y = 8480;

	private IFilterListener listener = null;

	private static final int MAX_OWNER = 1024;
	private static final int MAX_NOTE_ID = 16384;
	private static final int MAX_PAGE_ID = 262143;

	/**
	 * Instantiates a new Filter for paper.
	 *
	 * @param listener the listener
	 */
	public FilterForPaper( IFilterListener listener )
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
		if ( DotType.isPenActionDown( mdot.dotType ))
		{
			firstCheck = true;
			secondCheck = true;
			thirdCheck = true;
			dot1 = mdot;
		}

		// Middle dot inserts the second and verifies from the third
		// First dot validation failure second -> first, current -> second
		// Successful first dot verification
		else if ( DotType.isPenActionMove( mdot.dotType ) || DotType.isPenActionHover( mdot.dotType ) )
		{
			if(DotType.isPenActionHover( mdot.dotType ) && firstCheck)
			{
				dot1 = mdot;
				firstCheck = false;
			}
			// Just put it in the middle of the first
			else if ( secondCheck )
			{
				dot2 = mdot;
				secondCheck = false;
			}
			// Middle next Dot checks Middle validation check when first verification succeeds, and next Dot when failure
			else if ( thirdCheck )
			{
				if ( validateStartDot( dot1, dot2, mdot ) )
				{
					listener.onFilteredDot( dot1 );

					if ( validateMiddleDot( dot1, dot2, mdot ) )
					{
						listener.onFilteredDot( dot2 );
						dot1 = dot2;
						dot2 = mdot;
					}
					else
					{

						dot2 = mdot;
					}
				}
				else
				{
					if( DotType.isPenActionDown( dot1.dotType ) )
					{
						dot2.dotType = DotType.PEN_ACTION_DOWN.getValue();
					}
					dot1 = dot2;
					dot2 = mdot;
				}

				thirdCheck = false;
			}
			else
			{
				if ( validateMiddleDot( dot1, dot2, mdot ) )
				{
					listener.onFilteredDot( dot2 );
					dot1 = dot2;
					dot2 = mdot;
				}
				else
				{
					dot2 = mdot;
				}
			}

		}
		else if ( DotType.isPenActionUp( mdot.dotType ) || DotType.isPenActionHover( mdot.dotType ) )
		{
			boolean validateStartDot = true;
			boolean validateMiddleDot = true;
			//If only one dot is entered and only one Down 1, Move 1, End is entered
			// (Even though only one dot is entered through A_DotData in CommProcessor, Move 1, End 1 data is passed to actual processDot through A_DotUpDownData.)
			if(secondCheck)
			{
//				dot2 = dot1;
				dot2 = new Fdot(dot1.x, dot1.y, dot1.pressure, DotType.PEN_ACTION_MOVE.getValue(), dot1.timestamp, dot1.sectionId, dot1.ownerId, dot1.noteId, dot1.pageId, dot1.color, dot1.penTipType , dot1.tiltX, dot1.tiltY, dot1.twist) ;
			}
			if(thirdCheck && DotType.isPenActionDown( dot1.dotType ) )
			{
				if ( validateStartDot( dot1, dot2, mdot ) )
				{
					listener.onFilteredDot( dot1 );
				}
				else
				{
					validateStartDot = false;
				}
			}

			// Middle Dot Verification
			if ( validateMiddleDot( dot1, dot2, mdot ) )
			{

				if(!validateStartDot)
				{
					makeDownDot = new Fdot(dot2.x, dot2.y, dot2.pressure, DotType.PEN_ACTION_DOWN.getValue(), dot2.timestamp, dot2.sectionId, dot2.ownerId, dot2.noteId, dot2.pageId, dot2.color, dot2.penTipType , dot2.tiltX, dot2.tiltY, dot2.twist) ;
					listener.onFilteredDot( makeDownDot );
				}
				listener.onFilteredDot( dot2 );
			}
			else
			{
				validateMiddleDot = false;
			}

			// Last Dot Verification
			if ( validateEndDot( dot1, dot2, mdot ) )
			{
				if(!validateStartDot && !validateMiddleDot)
				{
					makeDownDot = new Fdot(mdot.x, mdot.y, mdot.pressure, DotType.PEN_ACTION_DOWN.getValue(), mdot.timestamp, mdot.sectionId, mdot.ownerId, mdot.noteId, mdot.pageId, mdot.color, mdot.penTipType , mdot.tiltX, mdot.tiltY, mdot.twist) ;
					listener.onFilteredDot( makeDownDot );
				}
				if(thirdCheck && !validateMiddleDot)
				{
					makeMoveDot = new Fdot(mdot.x, mdot.y, mdot.pressure, DotType.PEN_ACTION_MOVE.getValue(), mdot.timestamp, mdot.sectionId, mdot.ownerId, mdot.noteId, mdot.pageId, mdot.color, mdot.penTipType , mdot.tiltX, mdot.tiltY, mdot.twist) ;
					listener.onFilteredDot( makeMoveDot );
				}
				listener.onFilteredDot( mdot );
			}
			else
			{
				dot2.dotType = DotType.PEN_ACTION_UP.getValue();
				listener.onFilteredDot( dot2 );
			}

			// Dot and variable initialization
			dot1 = new Fdot( 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			dot2 = new Fdot( 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
			secondCheck = true;
			thirdCheck = true;
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
		if ( dot1 == null || dot2 == null || dot3 == null )
			return false;

		if ( dot1.x > MAX_X || dot1.x < 1 )
			return false;
		if ( dot1.y > MAX_Y || dot1.y < 1 )
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
		if ( dot1 == null || dot2 == null || dot3 == null )
			return false;

		if ( dot2.x > MAX_X || dot2.x < 1 )
			return false;
		if ( dot2.y > MAX_Y || dot2.y < 1 )
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
		if ( dot1 == null || dot2 == null || dot3 == null )
			return false;

		if ( dot3.x > MAX_X || dot3.x < 1 )
			return false;
		if ( dot3.y > MAX_Y || dot3.y < 1 )
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
