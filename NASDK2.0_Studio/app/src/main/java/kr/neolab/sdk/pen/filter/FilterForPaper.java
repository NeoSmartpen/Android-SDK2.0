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
	private boolean secondCheck = true, thirdCheck = true;

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

		// Start Dot는 일단 1번에 넣줌
		if ( DotType.isPenActionDown( mdot.dotType ) )
		{
			dot1 = mdot;
		}

		// Middle dot는 두번째는 그냥 넣고 세번째부터 검증
		// 첫번쨰 dot 검증 실패시 두번째-> 첫번째, 현재-> 두번째
		// 첫번째 dot 건증 성공시
		else if ( DotType.isPenActionMove( mdot.dotType ) )
		{
			// Middle의 첫번째에서는 그냥 넣어줌
			if ( secondCheck )
			{
				dot2 = mdot;
				secondCheck = false;
			}
			// 미들의 다음Dot는 첫번째것 검증 성공시 Middle validation 첵, 실패시 다음Dot를 넣어줌
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
		else if ( DotType.isPenActionUp( mdot.dotType ) )
		{
			boolean validateStartDot = true;
			boolean validateMiddleDot = true;
			//실도트가 한개만 들어와서 Down 1, Move 1, End 1 개씩만 들어온경우
			// (CommProcessor 에서 A_DotData 을 통해 도트가 1개만 들어오더라도 A_DotUpDownData 를 통해 실제 processDot 로는 Move 1, End 1 씩 데이터가 넘어온다.
			// 그때에 대한 처리
			if(secondCheck)
			{
				dot2 = dot1;
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

			// 미들 Dot 검증
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

			// 마지막 Dot 검증
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

			// Dot 및 변수 초기화
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
	// 3점을 이용
	// 방향성과 Delta X, Delta Y 세가지를 이용
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
