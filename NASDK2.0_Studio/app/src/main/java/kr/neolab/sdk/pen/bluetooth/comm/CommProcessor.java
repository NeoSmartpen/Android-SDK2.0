package kr.neolab.sdk.pen.bluetooth.comm;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import kr.neolab.sdk.ink.structure.DotType;
import kr.neolab.sdk.pen.bluetooth.BTAdt.ConnectedThread;
import kr.neolab.sdk.pen.bluetooth.cmd.CommandManager;
import kr.neolab.sdk.pen.bluetooth.cmd.EstablishCommand;
import kr.neolab.sdk.pen.bluetooth.cmd.ForceCalibrateCommand;
import kr.neolab.sdk.pen.bluetooth.cmd.FwUpgradeCommand;
import kr.neolab.sdk.pen.bluetooth.cmd.SetTimeCommand;
import kr.neolab.sdk.pen.bluetooth.lib.ByteConverter;
import kr.neolab.sdk.pen.bluetooth.lib.CMD;
import kr.neolab.sdk.pen.bluetooth.lib.Chunk;
import kr.neolab.sdk.pen.bluetooth.lib.Packet;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser.IParsedPacketListener;
import kr.neolab.sdk.pen.filter.Fdot;
import kr.neolab.sdk.pen.filter.FilterForFilm;
import kr.neolab.sdk.pen.filter.FilterForPaper;
import kr.neolab.sdk.pen.filter.IFilterListener;
import kr.neolab.sdk.pen.offline.OfflineFile;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsg;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

/**
 * BT 각 Connection in/out 패킷 처리
 *
 * @author CHY
 */
public class CommProcessor extends CommandManager implements IParsedPacketListener, IFilterListener
{
	/**
	 * 펜업 플래그
	 */
	private boolean isPrevDotDown = false;

	private boolean isStartWithDown = false;

	/**
	 * 이전 패킷 (펜업 도트 저장용)
	 */
	private Packet prevPacket;

	private ConnectedThread btConnection;

	private ProtocolParser parser;

	private int noteId = 0, pageId = 0;

	private int ownerId = 0, sectionId = 0;

	private int prevNoteId = 0, prevPageId = 0;
	private int prevOwnerId = 0, prevSectionId = 0;

    private int pidsNoteId = 0, pidsPageId = 0;
    private int pidsOwnerId = 0, pidsSectionId = 0;

	private long prevDotTime = 0;

	private int currColor = 0;

	private boolean isPenAuthenticated = false;
	
	private boolean isDoneInitHandshake = false;
	
	private final String defaultPassword = "0000";
	
	private Chunk chunk = null;

	/**
	 * The Is upgrading.
	 */
	public boolean isUpgrading = false;

	/**
	 * The Is upgrading suspended.
	 */
	public boolean isUpgradingSuspended = false;

	/**
	 * The Ol file.
	 */
	public OfflineFile olFile = null;

	private String FILE_NAME;
	private long FILE_SIZE;
	private short PACKET_COUNT, PACKET_SIZE;

	private FilterForPaper dotFilterPaper = null;
	private FilterForFilm dotFilterFilm = null;

	private int oTotalDataSize = 0, oRcvDataSize = 0;

	private Handler mHandler = new Handler( Looper.getMainLooper());

	private static final int OFFLINE_SEND_FAIL_TIME = 1000*20;

    private boolean isPIDS_On = false;

    private boolean isPIDS_Send = false;

	private class ChkOfflineFailRunnable implements Runnable{

		@Override
		public void run() {
			// 실패 처리
			NLog.d( "[CommProcessor20] ChkOfflineFailRunnable Fail!!" );
			btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_SEND_FAILURE ) );
		}
	}

	private ChkOfflineFailRunnable mChkOfflineFailRunnable;

	/**
	 * Instantiates a new Comm processor.
	 *
	 * @param conn the conn
	 */
	public CommProcessor( ConnectedThread conn )
	{
		this.btConnection = conn;
		this.parser = new ProtocolParser( this );

		this.dotFilterPaper = new FilterForPaper( this );
		this.dotFilterFilm = new FilterForFilm( this );
		this.mChkOfflineFailRunnable = new ChkOfflineFailRunnable();

		// Connection후 Establish가 안 되는 경우 Connection 해제
		this.checkEstablish();
	}

	public ConnectedThread getConn()
	{
		return this.btConnection;
	}

	/**
	 * 연결된 채널에서 오는 버퍼를 저장
	 * 
	 * @param data
	 * @param size
	 */
	public void fill( byte data[], int size )
	{
		parser.parseByteData( data, size );
	}

	/**
	 * 연결된 채널로 버퍼를 기록한다.
	 * 
	 * @param buffer
	 */
	public void write( byte[] buffer )
	{
		btConnection.write( buffer );
	}

	/**
	 * 들어온 패킷 분석
	 * 
	 * @param pack
	 */
	private void parsePacket( Packet pack )
	{
		/**
		 * 도트 이외 패킷 처리
		 */
		switch ( pack.getCmd() )
		{
			/*
			 * --------------------------------------------------------------------
			 * 
			 * A_DotData (0x11)
			 * 
			 * --------------------------------------------------------------------
			 */
			case CMD.A_DotData:

				int X = pack.getDataRangeInt( 1, 2 );
				int Y = pack.getDataRangeInt( 3, 2 );
				int FLOAT_X = pack.getDataRangeInt( 5, 1 );
				int FLOAT_Y = pack.getDataRangeInt( 6, 1 );
				int FORCE = pack.getDataRangeInt( 7, 1 );
				long TIME = pack.getDataRangeInt( 0, 1 );
				long timeLong = prevDotTime + TIME;

				// NLog.d("[CommProcessor] new dot noteId = " + noteId +
				// " / pageId = " + pageId + " / timeLong : " + timeLong +
				// " / timeAdd : " + TIME + " / x : " + X + " / y : " + Y +
				// " / fx : " + FLOAT_X + " / fy : " + FLOAT_Y);

                NLog.d( "[CommProcessor] received A_DotData() isPIDS_On="+isPIDS_On+",X:"+X+",isPIDS_Send="+isPIDS_Send+",FLOAT_X:"+FLOAT_X+",Y:"+Y+",FLOAT_Y:"+FLOAT_Y);
                if ( isPIDS_On)
                {
                    prevDotTime = timeLong;
                    if(!isPIDS_Send)
                    {
                        long longX = X;
                        long longY = Y;
                        long ID = longX << 10;
                        ID = ID + longY;
                        try
                        {
                            JSONObject job = new JSONObject()
                                    .put( JsonTag.LONG_PIDS_ID, ID )
                                    .put( JsonTag.INT_SECTION_ID, pidsSectionId )
                                    .put( JsonTag.INT_OWNER_ID, pidsOwnerId )
                                    .put( JsonTag.INT_NOTE_ID, pidsNoteId )
                                    .put( JsonTag.INT_PAGE_ID, pidsPageId )
                                    .put( JsonTag.LONG_PIDS_TIMESTAMP, timeLong );
                            btConnection.onCreateMsg( new PenMsg( PenMsgType.EVENT_PIDS, job ) );
                        }
                        catch ( JSONException e )
                        {
                            e.printStackTrace();
                        }
                        isPIDS_Send = true;
                    }
                    return;
                }


                if ( !isStartWithDown || timeLong < 10000 )
				{
					NLog.e( "[CommProcessor] this stroke start with middle dot." );
					return;
				}

				if ( isPrevDotDown )
				{
					// 펜업의 경우 시작 도트로 저장
					this.isPrevDotDown = false;
					this.processDot( sectionId, ownerId, noteId, pageId, timeLong, X, Y, FLOAT_X, FLOAT_Y, FORCE, DotType.PEN_ACTION_DOWN.getValue(), currColor );
				}
				else
				{
					// 펜업이 아닌 경우 미들 도트로 저장
					this.processDot( sectionId, ownerId, noteId, pageId, timeLong, X, Y, FLOAT_X, FLOAT_Y, FORCE, DotType.PEN_ACTION_MOVE.getValue() , currColor );
				}

				prevDotTime = timeLong;
				prevPacket = pack;

				break;

			/*
			 * ----------------------------------------------------------------
			 * 
			 * A_DotUpDownData (0x13)
			 * 
			 * ----------------------------------------------------------------
			 */
			case CMD.A_DotUpDownDataNew:
			case CMD.A_DotUpDownData:

				// NLog.d("[CommProcessor] A_DotUpDownData");

				int color = Color.BLACK;

				if ( pack.dataLength > 9 )
				{
					byte[] cbyte = pack.getDataRange( 9, 4 );

					// NLog.d( "t : " + Integer.toHexString( (int) (cbyte[3] & 0xFF) ) );
					// NLog.d( "r : " + Integer.toHexString( (int) (cbyte[2] & 0xFF) ) );
					// NLog.d( "g : " + Integer.toHexString( (int) (cbyte[1] & 0xFF) ) );
					// NLog.d( "b : " + Integer.toHexString( (int) (cbyte[0] & 0xFF) ) );

					color = ByteConverter.byteArrayToInt( new byte[] { cbyte[0], cbyte[1], cbyte[2], (byte) 0XFF } );
				}

				currColor = color;

				long MTIME = 0;

				try
				{
					MTIME = pack.getDataRangeLong( 0, 8 );
				}
				catch ( Exception e )
				{
					e.printStackTrace();
				}

				int PEN_UP_DOWN = pack.getDataRangeInt( 8, 1 );

				// NLog.d("[CommProcessor] new dot noteId = " + noteId + " / pageId = " + pageId + " / timeLong : " + NumberFormat.getInstance().format(MTIME) + " / state : " + PEN_UP_DOWN);

				if ( PEN_UP_DOWN == 0 )
				{
					// 펜 다운 일 경우 Start Dot의 timestamp 설정
					this.prevDotTime = MTIME;
					this.isPrevDotDown = true;
					this.isStartWithDown = true;
				}
				else if ( PEN_UP_DOWN == 1 )
				{
					if ( prevPacket != null )
					{
						// 펜 업 일 경우 바로 이전 도트를 End Dot로 삽입
						int pX = prevPacket.getDataRangeInt( 1, 2 );
						int pY = prevPacket.getDataRangeInt( 3, 2 );
						int pFX = prevPacket.getDataRangeInt( 5, 1 );
						int pFY = prevPacket.getDataRangeInt( 6, 1 );
						int pFORCE = prevPacket.getDataRangeInt( 7, 1 );
						long pTimeLong = MTIME;

						this.processDot( sectionId, ownerId, noteId, pageId, pTimeLong, pX, pY, pFX, pFY, pFORCE, DotType.PEN_ACTION_UP.getValue(), currColor );
					}

					isStartWithDown = false;
				}

                this.isPIDS_Send = false;
				this.prevPacket = null;

				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_DotIDChange (0x12)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_DotIDChange:

				// 노트 페이지 정보가 변경
				noteId = pack.getDataRangeInt( 0, 2 );
				pageId = pack.getDataRangeInt( 2, 2 );

				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_DotIDChange32 (0x15)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_DotIDChange32:

				// 노트 페이지 정보가 변경
				byte[] osbyte = pack.getDataRange( 0, 4 );
                int tempSection = (int) (osbyte[3] & 0xFF);
                int tempOwner = ByteConverter.byteArrayToInt( new byte[] { osbyte[0], osbyte[1], osbyte[2], (byte) 0x00 } );
                int tempNoteId = pack.getDataRangeInt( 4, 4 );
                int tempPageId = pack.getDataRangeInt( 8, 4 );

                // PIDS Symbol 체크
                NLog.d( "[CommProcessor] received A_DotIDChange32 tempSection= "+tempSection+",tempOwner="+tempOwner );
                if(tempSection == 3 && tempOwner >= 512)
                {
                    pidsNoteId = tempNoteId;
                    pidsPageId = tempPageId;
                    pidsOwnerId = tempOwner - 512;
                    pidsSectionId = tempSection;

                    // 펜 업처리만
                    if ( prevPacket != null )
                    {
                        // 펜 업 일 경우 바로 이전 도트를 End Dot로 삽입
                        int pX = prevPacket.getDataRangeInt( 1, 2 );
                        int pY = prevPacket.getDataRangeInt( 3, 2 );
                        int pFX = prevPacket.getDataRangeInt( 5, 1 );
                        int pFY = prevPacket.getDataRangeInt( 6, 1 );
                        int pFORCE = prevPacket.getDataRangeInt( 7, 1 );
                        long pTimeLong = prevDotTime;

                        this.processDot( prevSectionId, prevOwnerId, prevNoteId, prevPageId, pTimeLong, pX, pY, pFX, pFY, pFORCE, DotType.PEN_ACTION_UP.getValue(), currColor );
                        isPrevDotDown = true;
                        isStartWithDown = true;
                        prevPacket = null;
                    }
                    isPIDS_On = true;
                    return;
                }
                else
                {
                    isPIDS_On = false;
                }

				prevSectionId = sectionId;
				prevOwnerId = ownerId;
				prevNoteId = noteId;
				prevPageId = pageId;

				sectionId = (int) (osbyte[3] & 0xFF);
				ownerId = ByteConverter.byteArrayToInt( new byte[] { osbyte[0], osbyte[1], osbyte[2], (byte) 0x00 } );

				noteId = pack.getDataRangeInt( 4, 4 );
				pageId = pack.getDataRangeInt( 8, 4 );


				if ( prevPacket != null )
				{
					// 펜 업 일 경우 바로 이전 도트를 End Dot로 삽입
					int pX = prevPacket.getDataRangeInt( 1, 2 );
					int pY = prevPacket.getDataRangeInt( 3, 2 );
					int pFX = prevPacket.getDataRangeInt( 5, 1 );
					int pFY = prevPacket.getDataRangeInt( 6, 1 );
					int pFORCE = prevPacket.getDataRangeInt( 7, 1 );
					long pTimeLong = prevDotTime;

					this.processDot( prevSectionId, prevOwnerId, prevNoteId, prevPageId, pTimeLong, pX, pY, pFX, pFY, pFORCE, DotType.PEN_ACTION_UP.getValue(), currColor );
					isPrevDotDown = true;
					isStartWithDown = true;
				}


				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_PenOnState (0x01)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_PenOnState:

				NLog.d( "[CommProcessor] received PenOnState(0x01) command." );

				int STATUS = pack.getDataRangeInt( 8, 1 );

				// 펜 SW 버전 내려줌
				String SW_VER = pack.getDataRangeString( 10, 5 ).trim();

				NLog.d( "[CommProcessor] version of connected pen is " + SW_VER );

				try
				{
					JSONObject job = new JSONObject()
					.put( JsonTag.STRING_PROTOCOL_VERSION, "1" )
					.put( JsonTag.STRING_PEN_FW_VERSION, SW_VER );

					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_FW_VERSION, job ) );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}

				if ( STATUS == 0x00 )
				{
					NLog.d( "[CommProcessor] received power off command. pen will be shutdown." );
					// init ack
					write( ProtocolParser.buildPenOnOffData( true ) );

					btConnection.unbind(true);
				}
				else if ( STATUS == 0x01 && !btConnection.getIsEstablished() )
				{
					NLog.d( "[CommProcessor] connection is establised." );
					btConnection.onEstablished();

					// init ack
					write( ProtocolParser.buildPenOnOffData( true ) );

					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_CONNECTION_SUCCESS ) );
					this.reqSetCurrentTime();
				}

				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_RTCsetResponse (0x04)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_RTCsetResponse:

				NLog.d( "[CommProcessor] received RTCsetResponse(0x04) command." );

				kill( CMD.P_RTCset );

				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_AlarmResponse (0x06)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_AlarmResponse:

				NLog.d( "[CommProcessor] received AlarmResponse(0x06) command." );

				kill( CMD.P_Alarmset );

				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_PenSWUpgradeStatus (0x08)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_ForceCalibrateResponse:

				NLog.d( "[CommProcessor] received ForceCalibrateResponse(0x08) command." );

				btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_CALIBRATION_FINISH ) );

				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_PenStatusResponse (0x25)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_PenStatusResponse:

				NLog.d( "[CommProcessor] received PenStatusResponse(0x25) command." );

				kill( CMD.P_PenStatusRequest );

				if ( !isPenAuthenticated )
				{
					isPenAuthenticated = true;
					btConnection.onAuthorized();
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_AUTHORIZED ) );
				}

				String stat_version = pack.getDataRangeString( 0, 1 ).trim();
				String stat_status = pack.getDataRangeString( 1, 1 ).trim();

				int stat_timezone = pack.getDataRangeInt( 2, 4 );
				long stat_timetick = pack.getDataRangeLong( 6, 8 );
				int stat_forcemax = pack.getDataRangeInt( 14, 1 );
				int stat_battery = pack.getDataRangeInt( 15, 1 );
				int stat_usedmem = pack.getDataRangeInt( 16, 1 );

				byte[] cbyte = pack.getDataRange( 17, 4 );

				int stat_pencolor = ByteConverter.byteArrayToInt( new byte[] { cbyte[0], cbyte[1], cbyte[2], (byte) 0XFF } );

				boolean stat_autopower = pack.getDataRangeInt( 21, 1 ) == 2 ? false : true;
				boolean stat_accel = pack.getDataRangeInt( 22, 1 ) == 2 ? false : true;
				boolean stat_hovermode = pack.getDataRangeInt( 23, 1 ) == 2 ? false : true;
				boolean stat_beep = pack.getDataRangeInt( 24, 1 ) == 2 ? false : true;

				int stat_autopower_time = pack.getDataRangeInt( 25, 2 );
				int stat_sensitivity = pack.getDataRangeInt( 27, 2 );

				try
				{
					JSONObject job = new JSONObject()
					.put( JsonTag.STRING_PROTOCOL_VERSION, "1" )
					.put( JsonTag.STRING_STATUS, stat_status )
					.put( JsonTag.INT_TIMEZONE_OFFSET, stat_timezone )
					.put( JsonTag.LONG_TIMETICK, stat_timetick )
					.put( JsonTag.INT_MAX_FORCE, stat_forcemax )
					.put( JsonTag.INT_BATTERY_STATUS, stat_battery )
					.put( JsonTag.INT_MEMORY_STATUS, stat_usedmem )
					.put( JsonTag.INT_PEN_COLOR, stat_pencolor )
					.put( JsonTag.BOOL_AUTO_POWER_ON, stat_autopower )
					.put( JsonTag.BOOL_ACCELERATION_SENSOR, stat_accel )
					.put( JsonTag.BOOL_HOVER, stat_hovermode )
					.put( JsonTag.BOOL_BEEP, stat_beep )
					.put( JsonTag.INT_AUTO_POWER_OFF_TIME, stat_autopower_time )
					.put( JsonTag.INT_PEN_SENSITIVITY, stat_sensitivity );

					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_STATUS, job ) );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}

				break;
				
			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_AutoShutdownTimeResponse (0xa)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_AutoShutdownTimeResponse:
			{
				boolean isSuccess = pack.getDataRangeInt( 0, 1 ) == 0x01 ? true : false;

				try
				{
					JSONObject job = new JSONObject().put( JsonTag.BOOL_RESULT, isSuccess );
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_SETUP_AUTO_SHUTDOWN_RESULT, job ) );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;
				
			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_PenSensitivityResponse (0x2d)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_PenSensitivityResponse:
			{
				boolean isSuccess = pack.getDataRangeInt( 0, 1 ) == 0x01 ? true : false;

				try
				{
					JSONObject job = new JSONObject().put( JsonTag.BOOL_RESULT, isSuccess );
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_SETUP_SENSITIVITY_RESULT, job ) );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_AutoPowerOnResponse (0x2b)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_AutoPowerOnResponse:
			{
				boolean isSuccess = pack.getDataRangeInt( 0, 1 ) == 0x01 ? true : false;

				try
				{
					JSONObject job = new JSONObject().put( JsonTag.BOOL_RESULT, isSuccess );
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_SETUP_AUTO_POWER_ON_RESULT, job ) );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_BeepSetResponse (0x2f)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_BeepSetResponse:
			{
				boolean isSuccess = pack.getDataRangeInt( 0, 1 ) == 0x01 ? true : false;

				try
				{
					JSONObject job = new JSONObject().put( JsonTag.BOOL_RESULT, isSuccess );
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_SETUP_BEEP_RESULT, job ) );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;
			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_PenColorSetResponse (0x29)
			 * 
			 * ------------------------------------------------------------------
			 */			
			case CMD.A_PenColorSetResponse:
			{
				boolean isSuccess = pack.getDataRangeInt( 0, 1 ) == 0x01 ? true : false;

				try
				{
					JSONObject job = new JSONObject().put( JsonTag.BOOL_RESULT, isSuccess );
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_SETUP_PEN_COLOR_RESULT, job ) );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;
				
			case CMD.A_OfflineDataInfo:

				this.oTotalDataSize = pack.getDataRangeInt( 4, 4 );
				this.oRcvDataSize = 0;

				NLog.d( "[CommProcessor] received A_OfflineDataInfo(0x49) command. data size =>" + oTotalDataSize );

				if ( oTotalDataSize > 0 )
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_SEND_START ) );
				}
				else
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_SEND_FAILURE ) );
				}

				break;

			case CMD.A_OfflineResultResponse:
				if ( pack.getDataRangeInt( 0, 1 ) == 0x00 )
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_SEND_FAILURE ) );
				}
				else
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_SEND_SUCCESS ) );
				}

				break;
				
			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_OfflineData (0x41)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_OfflineInfo:
			{
				NLog.d( "[CommProcessor] received OfflineInfo(0x41) command." );

				if ( !btConnection.getAllowOffline() )
				{
					return;
				}

				FILE_NAME = pack.getDataRangeString( 0, 128 ).trim();
				FILE_SIZE = pack.getDataRangeInt( 128, 4 );
				PACKET_COUNT = pack.getDataRangeShort( 132, 2 );
				PACKET_SIZE = pack.getDataRangeShort( 134, 2 );

				NLog.i( "[CommProcessor] offline file transfer is started ( name : " + FILE_NAME + ", size : " + FILE_SIZE + ", packet_qty : " + PACKET_COUNT + ", packet_size : " + PACKET_SIZE + " )" );

				olFile = null;
				olFile = new OfflineFile( FILE_NAME, PACKET_COUNT, FILE_NAME.endsWith( ".zip" ) ? true : false );

				write( ProtocolParser.buildOfflineInfoResponse( true ) );
			}
				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_OfflineChunk (0x43)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_OfflineChunk:
			{
				int index = pack.getDataRangeInt( 0, 2 );

				NLog.d( "[CommProcessor] received chunk of offline data file ( index : " + index + " )" );

				byte[] cs = pack.getDataRange( 2, 1 );
				byte[] data = pack.getDataRange( 3, pack.dataLength - 3 );

				// 체크섬이 틀리거나, 카운트, 사이즈 정보가 맞지 않으면 버린다.
				if ( cs[0] == Chunk.calcChecksum( data ) && PACKET_COUNT > index && PACKET_SIZE >= data.length )
				{
					mHandler.removeCallbacks( mChkOfflineFailRunnable );
					olFile.append( data, index );

					// 만약 Chunk를 다 받았다면 offline data를 처리한다.
					if ( PACKET_COUNT == olFile.getCount() )
					{
						String output = olFile.make();

						if ( output != null )
						{
							write( ProtocolParser.buildOfflineChunkResponse( index ) );

							NLog.i( "[CommProcessor] offline file is stored. ( name : " + FILE_NAME + ", size : " + FILE_SIZE + ", packet_qty : " + PACKET_COUNT + ", packet_size : " + PACKET_SIZE + " )" );

							JSONObject job;

							try
							{
								job = new JSONObject();
								job.put( JsonTag.INT_SECTION_ID, olFile.getSectionId() );
								job.put( JsonTag.INT_OWNER_ID, olFile.getOwnerId() );
								job.put( JsonTag.INT_NOTE_ID, olFile.getNoteId() );
								job.put( JsonTag.INT_PAGE_ID, olFile.getPageId() );
								job.put( JsonTag.STRING_FILE_PATH, output );

								btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_FILE_CREATED, job ) );
							}
							catch ( JSONException e )
							{
								e.printStackTrace();
							}
						}

						olFile.clearTempFile();
						olFile = null;
					}
					else
					{
						write( ProtocolParser.buildOfflineChunkResponse( index ) );
						mHandler.postDelayed( mChkOfflineFailRunnable, OFFLINE_SEND_FAIL_TIME );
					}

					this.oRcvDataSize += data.length;

					JSONObject job;

					try
					{
						job = new JSONObject();
						job.put( JsonTag.INT_TOTAL_SIZE, this.oTotalDataSize );
						job.put( JsonTag.INT_RECEIVED_SIZE, this.oRcvDataSize );

						btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_SEND_STATUS, job ) );
					}
					catch ( JSONException e )
					{
						e.printStackTrace();
					}
				}
				else
				{
					NLog.e( "[CommProcessor] offline data file verification failed ( index : " + index + " )" );
				}
			}
				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_PenSWUpgradeRequest (0x52)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_PenSWUpgradeRequest:

				int index = pack.getDataRangeInt( 0, 2 );

				NLog.d( "[CommProcessor] requested the chunk of firmware file. ( index : " + index + " )" );
				resPenSwRequest( index );

				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_PenSWUpgradeStatus (0x54)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_PenSWUpgradeStatus:

				int status = pack.getDataRangeInt( 0, 1 );
				resPenSwUpgStatus( status );

				break;

			case CMD.A_OfflineNoteListResponse:

				int isEnd = pack.getDataRangeInt( 0, 1 );

				byte[] rxb = pack.getDataRange( 1, 4 );

				int oSectionId = (int) (rxb[3] & 0xFF);
				int oOwnerId = ByteConverter.byteArrayToInt( new byte[] { rxb[0], rxb[1], rxb[2], (byte) 0x00 } );

				int noteCnt = pack.getDataRangeInt( 5, 1 );

				int startIndex = 6;

				NLog.d( "[CommProcessor] A_OfflineNoteListResponse => sectionId : " + oSectionId + ", ownerId : " + oOwnerId + ", noteCnt : " + noteCnt + ", isEnd : " + isEnd );

				try
				{
					for ( int i = 0; i < noteCnt; i++ )
					{
						int noteId = pack.getDataRangeInt( startIndex, 4 );

						offlineNoteInfos.put( new JSONObject().put( JsonTag.INT_OWNER_ID, oOwnerId ).put( JsonTag.INT_SECTION_ID, oSectionId ).put( JsonTag.INT_NOTE_ID, noteId ) );

						startIndex += 4;
					}

					if ( isEnd == 0x01 )
					{
						btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_NOTE_LIST, offlineNoteInfos ) );
						offlineNoteInfos = new JSONArray();
					}
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}

				break;

			case CMD.A_UsingNoteNotifyResponse:

				NLog.d( "[CommProcessor] A_UsingNoteNotifyResponse : " + pack.getDataRangeInt( 0, 1 ) );

				break;

			case CMD.A_OfflineDataRemoveResponse:

				NLog.d( "[CommProcessor] A_OfflineDataRemoveResponse : " + pack.getDataRangeInt( 0, 1 ) );

				btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_FILE_DELETED ) );

				break;

			case CMD.A_PenStatusSetupResponse:

				NLog.d( "[CommProcessor] A_PenStatusSetupResponse : " + pack.getDataRangeInt( 0, 1 ) );

				if ( pack.getDataRangeInt( 0, 1 ) == 0x00 )
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_SETUP_SUCCESS ) );
				}
				else
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_SETUP_FAILURE ) );
				}

				break;

			case CMD.A_PasswordRequest:
			{
				if ( !isDoneInitHandshake )
				{
					reqInputPassword( defaultPassword );
					NLog.d( "[CommProcessor] InitHandshaking done." );
					isDoneInitHandshake = true;
					return;
				}

				int countRetry = pack.getDataRangeInt( 0, 1 );
				int countReset = pack.getDataRangeInt( 1, 2 );

				NLog.d( "[CommProcessor] A_PasswordRequest ( " + countRetry + " / " + countReset + " )" );

				JSONObject job = null;

				try
				{
					job = new JSONObject();
					job.put( JsonTag.INT_PASSWORD_RETRY_COUNT, countRetry );
					job.put( JsonTag.INT_PASSWORD_RESET_COUNT, countReset );

					btConnection.onCreateMsg( new PenMsg( PenMsgType.PASSWORD_REQUEST, job ) );
				}
				catch ( JSONException e )
				{
					e.printStackTrace();
				}
			}
				break;

			case CMD.A_PasswordSetResponse:
			{
				int result = pack.getDataRangeInt( 0, 1 );

				NLog.d( "[CommProcessor] A_PasswordSetResponse => " + result );

				if ( result == 0x00 )
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PASSWORD_SETUP_SUCCESS ) );
				}
				else
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PASSWORD_SETUP_FAILURE ) );
				}
			}
				break;
		}
	}

	private JSONArray offlineNoteInfos = new JSONArray();

	private void processDot( int sectionId, int ownerId, int noteId, int pageId, long timeLong, int x, int y, int fx, int fy, int force, int type, int color )
	{

//		int sectionId = source.readInt();
//		int ownerId = source.readInt();
//		int noteId = source.readInt();
//		int pageId = source.readInt();
//		int x = source.readInt();
//		int y = source.readInt();
//		int fx = source.readInt();
//		int fy = source.readInt();
//		int pressure = source.readInt();
//		int color = source.readInt();
//		int dotType = source.readInt();
//		int penTipType = source.readInt();
//		int tiltX = source.readInt();
//		int tiltY = source.readInt();
//		int twist = source.readInt();
//		long timestamp = source.readLong();
		Fdot tempFdot = new Fdot((x + (float) (fx * 0.01)), (y + (float) (fy * 0.01)), force, type, timeLong, sectionId, ownerId, noteId, pageId, color,0 , 0, 0, 0 );

		if ( noteId == 45 && pageId == 1 )
		{
			dotFilterFilm.put( tempFdot );
		}
		else
		{
			dotFilterPaper.put( tempFdot );
		}
	}

	@Override
	public void onFilteredDot( Fdot dot )
	{
		btConnection.onCreateDot( dot );
	}

	@Override
	public void onCreatePacket( Packet packet )
	{
		parsePacket( packet );
	}

	/**
	 * 펜 RTC 설정
	 */
	private void reqSetCurrentTime()
	{
		execute( new SetTimeCommand( CMD.P_RTCset, this ) );
	}

	/**
	 * 펜 상태 요청
	 */
	public void reqPenStatus()
	{
		write( ProtocolParser.buildPenStatusData() );
	}

	/**
	 * force calibrate 요청
	 */
	public void reqForceCalibrate()
	{
		execute( new ForceCalibrateCommand( CMD.P_ForceCalibrate, this ) );
	}

	public void reqSetAutoShutdownTime( short min )
	{
		write( ProtocolParser.buildAutoShutdownTimeSetup( min ) );
	}

	public void reqSetPenSensitivity( short sensitivity )
	{
		write( ProtocolParser.buildPenSensitivitySetup( sensitivity ) );
	}

//	public void reqAddUsingNote( int sectionId, int ownerId, int[] noteIds )
//	{
//		int[] bucket = new int[9];
//		int bcount = 0;
//
//		for ( int i = 0; i < noteIds.length; i++ )
//		{
//			bucket[bcount++] = noteIds[i];
//
//			if ( i > 0 && i % 8 == 0 )
//			{
//				write( ProtocolParser.buildAddUsingNotes( sectionId, ownerId, bucket ) );
//				bcount = 0;
//			}
//			else if ( i == noteIds.length - 1 )
//			{
//				write( ProtocolParser.buildAddUsingNotes( sectionId, ownerId, Arrays.copyOfRange( bucket, 0, bcount ) ) );
//				break;
//			}
//		}
//	}
	
	private static int[][] chunkArray( int[] array, int chunkSize )
	{
		int numOfChunks = (int) Math.ceil( (double) array.length / chunkSize );
		int[][] output = new int[numOfChunks][];

		for ( int i = 0; i < numOfChunks; ++i )
		{
			int start = i * chunkSize;
			int length = Math.min( array.length - start, chunkSize );

			int[] temp = new int[length];
			System.arraycopy( array, start, temp, 0, length );
			output[i] = temp;
		}

		return output;
	}
	
	public void reqAddUsingNote( int sectionId, int ownerId, int[] noteIds )
	{
		int[][] noteIdss = chunkArray( noteIds, 9 );

		for ( int i = 0; i < noteIdss.length; i++ )
		{
			write( ProtocolParser.buildAddUsingNotes( sectionId, ownerId, noteIdss[i] ) );
		}
	}
	
	public void reqAddUsingNote( int sectionId, int ownerId )
	{
		write( ProtocolParser.buildAddUsingNotes( sectionId, ownerId ) );
	}

	@Override
	public void reqAddUsingNote(int[] sectionId, int[] ownerId) {
		for(int i = 0; i < sectionId.length; i++)
		{
			write( ProtocolParser.buildAddUsingNotes( sectionId[i], ownerId[i] ) );
		}
	}

	public void reqAddUsingNoteAll()
	{
		write( ProtocolParser.buildAddUsingAllNotes() );
	}

	public void reqOfflineData( int sectionId, int ownerId, int noteId )
	{
		write( ProtocolParser.buildReqOfflineData( sectionId, ownerId, noteId ) );
	}

	public void reqOfflineDataList()
	{
		write( ProtocolParser.buildReqOfflineDataList() );
	}

	/**
	 * Req offline data remove.
	 *
	 * @param sectionId the section id
	 * @param ownerId   the owner id
	 */
	public void reqOfflineDataRemove( int sectionId, int ownerId )
	{
		write( ProtocolParser.buildReqOfflineDataRemove( sectionId, ownerId ) );
	}

	public void reqAutoPowerSetupOnOff( boolean isOn )
	{
		write( ProtocolParser.buildPenAutoPowerSetup( isOn ) );
	}

	public void reqPenBeepSetup( boolean isOn )
	{
		write( ProtocolParser.buildPenBeepSetup( isOn ) );
	}

	public void reqSetupPenTipColor( int color )
	{
		write( ProtocolParser.buildPenTipColorSetup( color ) );
	}

	public void reqInputPassword( String password )
	{
		write( ProtocolParser.buildPasswordInput( password ) );
	}

	public void reqSetUpPassword( String oldPassword, String newPassword )
	{
		write( ProtocolParser.buildPasswordSetup( oldPassword, newPassword ) );
	}

	/**
	 * 펜 SW 업그레이드 요청
	 * 
	 * @param source
	 * @param target
	 */
	public void reqPenSwUpgrade( File source, String target)
	{
		NLog.d( "[CommProcessor] request pen firmware upgrade." );

		if ( isUpgrading )
		{
			NLog.e( "[CommProcessor] Upgrade task is still excuting." );
			return;
		}

		isUpgrading = true;
		isUpgradingSuspended = false;

		FwUpgradeCommand command = new FwUpgradeCommand( CMD.A_PenSWUpgradeRequest, this );
		command.setInfo( source, target );
		execute( command );
	}

	public void reqSuspendPenSwUpgrade()
	{
		if ( !isUpgrading )
		{
			NLog.e( "[CommProcessor] Upgrade task is not excuting." );
			return;
		}
		
		btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_SUSPEND ) );
		
		isUpgrading = false;
		isUpgradingSuspended = true;
		
		kill( CMD.A_PenSWUpgradeRequest );
		
		chunk = null;
	}

	/**
	 * 펜에서 온 펜 SW 전송 요청에 대한 응답
	 *
	 * @param index the index
	 */
	public void resPenSwRequest( int index )
	{
		if ( chunk == null )
		{
			NLog.e( "[CommProcessor] pen upgrade job has not been initialized." );
			return;
		}

		rQueue.add( index );
	}

	/**
	 * 펜 SW 업그레이드 상태에 따른 처리
	 *
	 * @param status the status
	 */
	public void resPenSwUpgStatus( int status )
	{
		switch ( status )
		{
			case 0x00:
				NLog.e( "[CommProcessor] received pen upgrade status : file send failure." );
				
				if ( !isUpgradingSuspended )
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
				}
				
				finishUpgrade();
				break;

			case 0x01:
				NLog.d( "[CommProcessor] received pen upgrade status : upgrade complete." );
				finishUpgrade();
				btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_SUCCESS ) );
				break;

			case 0x02:
				NLog.d( "[CommProcessor] received pen upgrade status : file is sending now." );
				break;

			case 0x03:
				NLog.e( "[CommProcessor] received pen upgrade status : insufficient storage space." );
				finishUpgrade();
				btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
				break;

			case 0x04:
				NLog.e( "[CommProcessor] received pen upgrade status : packet save failure." );
				finishUpgrade();
				btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
				break;

			case 0x05:
				NLog.e( "[CommProcessor] received pen upgrade status : no response." );
				finishUpgrade();
				btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
				break;

			default:
				NLog.e( "[CommProcessor] received pen upgrade status : unknown" );
				finishUpgrade();
		}
	}

	public void finishUpgrade()
	{
		isUpgrading = false;
		isUpgradingSuspended = false;
		chunk = null;
		
		kill( CMD.A_PenSWUpgradeRequest );
	}

	/**
	 * Check establish.
	 */
	public void checkEstablish()
	{
		execute( new EstablishCommand( 999999, this ) );
	}

	/**
	 * Gets chunk.
	 *
	 * @return the chunk
	 */
	public Chunk getChunk()
	{
		return this.chunk;
	}

	public void setChunk( Chunk chunk )
	{
		this.chunk = chunk;
	}
}
