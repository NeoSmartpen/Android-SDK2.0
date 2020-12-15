package kr.neolab.sdk.pen.bluetooth.comm;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import kr.neolab.sdk.ink.structure.DotType;
import kr.neolab.sdk.pen.bluetooth.IConnectedThread;
import kr.neolab.sdk.pen.bluetooth.cmd.CommandManager;
import kr.neolab.sdk.pen.bluetooth.cmd.EstablishCommand;
import kr.neolab.sdk.pen.bluetooth.cmd.ForceCalibrateCommand;
import kr.neolab.sdk.pen.bluetooth.cmd.FwUpgradeCommand;
import kr.neolab.sdk.pen.bluetooth.cmd.SetTimeCommand;
import kr.neolab.sdk.pen.bluetooth.lib.ByteConverter;
import kr.neolab.sdk.pen.bluetooth.lib.CMD;
import kr.neolab.sdk.pen.bluetooth.lib.Chunk;
import kr.neolab.sdk.pen.bluetooth.lib.IChunk;
import kr.neolab.sdk.pen.bluetooth.lib.Packet;
import kr.neolab.sdk.pen.bluetooth.lib.PenProfile;
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
 * BT Connection in/out packet process
 *
 * @author CHY
 */
public class CommProcessor extends CommandManager implements IParsedPacketListener, IFilterListener
{
	/**
	 * Pen Up Flag
	 */
	private boolean isPrevDotDown = false;

	private boolean isStartWithDown = false;

	/**
	 * Previous packet (for storing pen-up dots)
	 */
	private Packet prevPacket;

	private IConnectedThread btConnection;  

	private ProtocolParser parser;

	private int noteId = 0, pageId = 0;

	private int ownerId = 0, sectionId = 0;

	private int prevNoteId = 0, prevPageId = 0;
	private int prevOwnerId = 0, prevSectionId = 0;

	private long prevDotTime = 0;

	private long down_MTIME = 0;

	private int currColor = 0;

	private boolean isPenAuthenticated = false;
	
	private boolean isDoneInitHandshake = false;
	

	private IChunk chunk = null;

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

	private String currentPassword = "";
	private String SW_VER = "";

	private float firmWareVer = 0f;

	private float[] factor = null;

	private int sensorType = 0;
	private String connectedDeviceName = null;

	private boolean isReceivedPageIdChange = true;  //[2018.03.05] Stroke Test

	/**
	 * The constant PEN_PROFILE_SUPPORT_VERSION_F110.
	 */
	public static final float PEN_PROFILE_SUPPORT_VERSION_F110 = 1.06f;
	/**
	 * The constant PEN_PROFILE_SUPPORT_VERSION_F110C.
	 */
	public static final float PEN_PROFILE_SUPPORT_VERSION_F110C = 1.06f;
	/**
	 * The constant PEN_MODEL_NAME_F110.
	 */
	public static final String PEN_MODEL_NAME_F110 = "NWP-F110";
	/**
	 * The constant PEN_MODEL_NAME_F110C.
	 */
	public static final String PEN_MODEL_NAME_F110C = "NWP-F110C";


	private Object extraData = null;
	private int stat_battery = 0;

	private boolean useProfile = true;

	private class ChkOfflineFailRunnable implements Runnable{

		@Override
		public void run() {
			// fail process
			NLog.d( "[CommProcessor20] ChkOfflineFailRunnable Fail!!" );
			extraData = null;
			btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_SEND_FAILURE ) );
		}
	}

    /**
     * If pen up is missed, make up dot and send error message.
     */
    private class ChkMissingPenUpRunnable implements Runnable { //[2018.03.05] Stroke Test
        @Override
        public void run() {
            if ( prevPacket != null )
            {
                // In case of pen down, but prev dot is move. MISSING UP DOT

                int pX = prevPacket.getDataRangeInt( 1, 2 );
                int pY = prevPacket.getDataRangeInt( 3, 2 );
                int pFX = prevPacket.getDataRangeInt( 5, 1 );
                int pFY = prevPacket.getDataRangeInt( 6, 1 );
                int pFORCE = prevPacket.getDataRangeInt( 7, 1 );
                long pTimeLong = prevDotTime;

                NLog.e( "[CommProcessor / ChkMissingPenUpRunnable] prev stroke end with middle dot. " +
                        "TimeStamp="+ pTimeLong + ",ErrorType="+ JsonTag.ERROR_TYPE_MISSING_PEN_UP + ",Section=" + prevSectionId + ",Owner="+prevOwnerId+",Note="+prevNoteId+",Page"+prevPageId+",X="+pX+",Y="+pY+",Force="+pFORCE );

                processDot( sectionId, ownerId, noteId, pageId, pTimeLong, pX, pY, pFX, pFY, pFORCE, DotType.PEN_ACTION_UP.getValue(), currColor );

                try
                {
                    JSONObject job = new JSONObject()
                            .put( JsonTag.INT_SECTION_ID, prevSectionId )
                            .put( JsonTag.INT_OWNER_ID, prevOwnerId )
                            .put( JsonTag.INT_NOTE_ID, prevNoteId )
                            .put( JsonTag.INT_PAGE_ID, prevPageId )
                            .put( JsonTag.INT_LOG_X, pX )
                            .put( JsonTag.INT_LOG_Y, pY )
							.put( JsonTag.INT_LOG_FX, pFX)
							.put( JsonTag.INT_LOG_FY, pFY)
                            .put( JsonTag.INT_LOG_FORCE, pFORCE )
                            .put( JsonTag.INT_LOG_ERROR_TYPE, JsonTag.ERROR_TYPE_MISSING_PEN_UP)
                            .put( JsonTag.LONG_LOG_PEN_DOWN_TIMESTAMP, down_MTIME )
                            .put( JsonTag.LONG_LOG_TIMESTAMP, pTimeLong );
                    btConnection.onCreateMsg( new PenMsg( PenMsgType.ERROR_MISSING_PEN_UP, job ) );
                }catch ( Exception e )
                {
                    e.printStackTrace();
                }

            }
        }
    }

    private ChkMissingPenUpRunnable mChkMissingPenUpRunnable;

	private ChkOfflineFailRunnable mChkOfflineFailRunnable;

	/**
	 * Instantiates a new Comm processor.
	 *
	 * @param conn the conn
	 */
	public CommProcessor( IConnectedThread conn )
	{
		this.btConnection = conn;
		this.parser = new ProtocolParser( this );

		this.dotFilterPaper = new FilterForPaper( this );
		this.dotFilterFilm = new FilterForFilm( this );
		this.mChkOfflineFailRunnable = new ChkOfflineFailRunnable();

        this.mChkMissingPenUpRunnable = new ChkMissingPenUpRunnable();

		this.checkEstablish();
	}

    /**
     * Finish.
     */
    public void finish() {
        if ( prevPacket != null )
        {
            // In case of pen down, but prev dot is move. MISSING UP DOT
            int pX = prevPacket.getDataRangeInt( 1, 2 );
            int pY = prevPacket.getDataRangeInt( 3, 2 );
            int pFX = prevPacket.getDataRangeInt( 5, 1 );
            int pFY = prevPacket.getDataRangeInt( 6, 1 );
            int pFORCE = prevPacket.getDataRangeInt( 7, 1 );
            long pTimeLong = prevDotTime;

            NLog.e( "[CommProcessor] The Processor has finished. But prev stroke end with middle dot. " +
                    "TimeStamp="+ pTimeLong + ",ErrorType="+ JsonTag.ERROR_TYPE_MISSING_PEN_UP + ",Section=" + prevSectionId + ",Owner="+prevOwnerId+",Note="+prevNoteId+",Page"+prevPageId+",X="+pX+",Y="+pY+",Force="+pFORCE );

            this.processDot( sectionId, ownerId, noteId, pageId, pTimeLong, pX, pY, pFX, pFY, pFORCE, DotType.PEN_ACTION_UP.getValue(), currColor );

            try
            {
                JSONObject job = new JSONObject()
                        .put( JsonTag.INT_SECTION_ID, prevSectionId )
                        .put( JsonTag.INT_OWNER_ID, prevOwnerId )
                        .put( JsonTag.INT_NOTE_ID, prevNoteId )
                        .put( JsonTag.INT_PAGE_ID, prevPageId )
                        .put( JsonTag.INT_LOG_X, pX )
                        .put( JsonTag.INT_LOG_Y, pY )
                        .put( JsonTag.INT_LOG_FX, pFX)
                        .put( JsonTag.INT_LOG_FY, pFY)
                        .put( JsonTag.INT_LOG_FORCE, pFORCE )
                        .put( JsonTag.INT_LOG_ERROR_TYPE, JsonTag.ERROR_TYPE_MISSING_PEN_UP)
                        .put( JsonTag.LONG_LOG_PEN_DOWN_TIMESTAMP, down_MTIME )
                        .put( JsonTag.LONG_LOG_TIMESTAMP, pTimeLong );
                btConnection.onCreateMsg( new PenMsg( PenMsgType.ERROR_MISSING_PEN_UP, job ) );
            }catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }

	/**
	 * Req calibrate 2.
	 *
	 * @param factor the factor
	 */
	public void reqCalibrate2 ( float[] factor )
	{
		this.factor = factor;
	}



	public IConnectedThread getConn()
	{
		return this.btConnection;
	}

	/**
	 * save buffer in connected channel
	 * 
	 * @param data
	 * @param size
	 */
	public void fill( byte data[], int size )
	{
		parser.parseByteData( data, size );
	}

	/**
	 * record buffer with connected channel.
	 * 
	 * @param buffer
	 */
	public void write( byte[] buffer )
	{
		btConnection.write( buffer );
	}

	/**
	 * Incoming packet analysis
	 * 
	 * @param pack
	 */
	private void parsePacket( Packet pack )
	{
		NLog.d( "[CommProcessor] received parsePacket = "+pack.getCmd() );
		/**
		 * Packet processing other than dot
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

                //[2018.03.05] Stroke Test
				if ( timeLong < 10000 )
				{
					NLog.e( "[CommProcessor] Invalid Time." +
									"TimeStamp="+ timeLong + ",ErrorType="+ JsonTag.ERROR_TYPE_INVALID_TIME + ",Section=" + sectionId + ",Owner="+ownerId+",Note="+noteId+",Page"+pageId+",X="+X+",Y="+Y+",Force="+FORCE );

					try
					{
						JSONObject job = new JSONObject()
								.put( JsonTag.INT_SECTION_ID, sectionId )
								.put( JsonTag.INT_OWNER_ID, ownerId )
								.put( JsonTag.INT_NOTE_ID, noteId )
								.put( JsonTag.INT_PAGE_ID, pageId )
								.put( JsonTag.INT_LOG_X, X )
								.put( JsonTag.INT_LOG_Y, Y )
								.put( JsonTag.INT_LOG_FX, FLOAT_X)
								.put( JsonTag.INT_LOG_FY, FLOAT_Y)
								.put( JsonTag.INT_LOG_FORCE, FORCE )
								.put( JsonTag.INT_LOG_ERROR_TYPE, JsonTag.ERROR_TYPE_INVALID_TIME )
                                .put( JsonTag.LONG_LOG_PEN_DOWN_TIMESTAMP, down_MTIME )
								.put( JsonTag.LONG_LOG_TIMESTAMP, timeLong );
						btConnection.onCreateMsg( new PenMsg( PenMsgType.ERROR_INVALID_TIME, job ) );
					}catch ( Exception e )
					{
						e.printStackTrace();
					}
				}

                //[2018.03.05] Stroke Test
                if ( !isStartWithDown )
                {
                    NLog.e( "[CommProcessor] this stroke start with middle dot. " +
                                    "TimeStamp="+ timeLong + ",ErrorType="+ JsonTag.ERROR_TYPE_MISSING_PEN_DOWN + ",Section=" + sectionId + ",Owner="+ownerId+",Note="+noteId+",Page"+pageId+",X="+X+",Y="+Y+",Force="+FORCE );

                    try
                    {
                        JSONObject job = new JSONObject()
                                .put( JsonTag.INT_SECTION_ID, sectionId )
                                .put( JsonTag.INT_OWNER_ID, ownerId )
                                .put( JsonTag.INT_NOTE_ID, noteId )
                                .put( JsonTag.INT_PAGE_ID, pageId )
                                .put( JsonTag.INT_LOG_X, X )
                                .put( JsonTag.INT_LOG_Y, Y )
                                .put( JsonTag.INT_LOG_FX, FLOAT_X)
                                .put( JsonTag.INT_LOG_FY, FLOAT_Y)
                                .put( JsonTag.INT_LOG_FORCE, FORCE )
                                .put( JsonTag.INT_LOG_ERROR_TYPE, JsonTag.ERROR_TYPE_MISSING_PEN_DOWN)
                                .put( JsonTag.LONG_LOG_PEN_DOWN_TIMESTAMP, -1 )
                                .put( JsonTag.LONG_LOG_TIMESTAMP, timeLong );
                        btConnection.onCreateMsg( new PenMsg( PenMsgType.ERROR_MISSING_PEN_DOWN, job ) );
                    }catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                    timeLong = System.currentTimeMillis();
                    this.isPrevDotDown = true;
                    this.isStartWithDown = true;

//					return;
				}

                //[2018.03.05] Stroke Test
                if(isReceivedPageIdChange == false) {
                    NLog.e( "[CommProcessor] Page ID change NOT sent." +
                            "TimeStamp="+ timeLong + ",ErrorType="+ JsonTag.ERROR_TYPE_MISSING_PAGE_CHANGE + ",Section=" + sectionId + ",Owner="+ownerId+",Note="+noteId+",Page"+pageId+",X="+X+",Y="+Y+",Force="+FORCE );
                    try
                    {
                        JSONObject job = new JSONObject()
                                .put( JsonTag.INT_SECTION_ID, sectionId )
                                .put( JsonTag.INT_OWNER_ID, ownerId )
                                .put( JsonTag.INT_NOTE_ID, noteId )
                                .put( JsonTag.INT_PAGE_ID, ownerId )
                                .put( JsonTag.INT_LOG_X, X )
                                .put( JsonTag.INT_LOG_Y, Y )
								.put( JsonTag.INT_LOG_FX, FLOAT_X)
								.put( JsonTag.INT_LOG_FY, FLOAT_Y)
                                .put( JsonTag.INT_LOG_FORCE, FORCE )
                                .put( JsonTag.INT_LOG_ERROR_TYPE, JsonTag.ERROR_TYPE_MISSING_PAGE_CHANGE)
                                .put( JsonTag.LONG_LOG_PEN_DOWN_TIMESTAMP, down_MTIME )
                                .put( JsonTag.LONG_LOG_TIMESTAMP, timeLong );
                        btConnection.onCreateMsg( new PenMsg( PenMsgType.ERROR_TYPE_MISSING_PAGE_CHANGE, job ) );
                    }catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                }

				if ( isPrevDotDown )
				{
					// In case of pen-up, save as start dot
					this.isPrevDotDown = false;
					this.processDot( sectionId, ownerId, noteId, pageId, timeLong, X, Y, FLOAT_X, FLOAT_Y, FORCE, DotType.PEN_ACTION_DOWN.getValue(), currColor );
				}
				else
				{
					// If it is not a pen-up, save it as a middle dot.
					this.processDot( sectionId, ownerId, noteId, pageId, timeLong, X, Y, FLOAT_X, FLOAT_Y, FORCE, DotType.PEN_ACTION_MOVE.getValue() , currColor );
				}

				prevDotTime = timeLong;
				prevPacket = pack;

//                setMakeUpDotTimer();    //[2018.03.05] Stroke Test

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
				NLog.d( "[CommProcessor] received RES_EventPenUpDown() command. PEN_UP_DOWN = "+PEN_UP_DOWN );

				// NLog.d("[CommProcessor] new dot noteId = " + noteId + " / pageId = " + pageId + " / timeLong : " + NumberFormat.getInstance().format(MTIME) + " / state : " + PEN_UP_DOWN);

				if ( PEN_UP_DOWN == 0 )
				{
				    down_MTIME = MTIME;
                    //[2018.03.05] Stroke Test
					if ( prevPacket != null )
					{
						// In case of pen down, but prev dot is move. MISSING UP DOT

						int pX = prevPacket.getDataRangeInt( 1, 2 );
						int pY = prevPacket.getDataRangeInt( 3, 2 );
						int pFX = prevPacket.getDataRangeInt( 5, 1 );
						int pFY = prevPacket.getDataRangeInt( 6, 1 );
						int pFORCE = prevPacket.getDataRangeInt( 7, 1 );
						long pTimeLong = prevDotTime;

						NLog.e( "[CommProcessor] prev stroke end with middle dot. " +
										"TimeStamp="+ pTimeLong + ",ErrorType="+ JsonTag.ERROR_TYPE_MISSING_PEN_UP + ",Section=" + prevSectionId + ",Owner="+prevOwnerId+",Note="+prevNoteId+",Page"+prevPageId+",X="+pX+",Y="+pY+",Force="+pFORCE );

                        this.processDot( sectionId, ownerId, noteId, pageId, pTimeLong, pX, pY, pFX, pFY, pFORCE, DotType.PEN_ACTION_UP.getValue(), currColor );

						try
						{
							JSONObject job = new JSONObject()
									.put( JsonTag.INT_SECTION_ID, prevSectionId )
									.put( JsonTag.INT_OWNER_ID, prevOwnerId )
									.put( JsonTag.INT_NOTE_ID, prevNoteId )
									.put( JsonTag.INT_PAGE_ID, prevPageId )
									.put( JsonTag.INT_LOG_X, pX )
									.put( JsonTag.INT_LOG_Y, pY )
									.put( JsonTag.INT_LOG_FX, pFX)
									.put( JsonTag.INT_LOG_FY, pFY)
									.put( JsonTag.INT_LOG_FORCE, pFORCE )
									.put( JsonTag.INT_LOG_ERROR_TYPE, JsonTag.ERROR_TYPE_MISSING_PEN_UP)
                                    .put( JsonTag.LONG_LOG_PEN_DOWN_TIMESTAMP, down_MTIME )
									.put( JsonTag.LONG_LOG_TIMESTAMP, pTimeLong );
							btConnection.onCreateMsg( new PenMsg( PenMsgType.ERROR_MISSING_PEN_UP, job ) );
						}catch ( Exception e )
						{
							e.printStackTrace();
						}

					}

					// In case of pen down, set the timestamp of the Start Dot
					this.prevDotTime = MTIME;
					this.isPrevDotDown = true;
					this.isStartWithDown = true;

                    isReceivedPageIdChange = false;     //[2018.03.05] Stroke Test
				}
				else if ( PEN_UP_DOWN == 1 )
				{
					if ( prevPacket != null )
					{
						// In case of pen-up, Insert previous dots as End Dots
						int pX = prevPacket.getDataRangeInt( 1, 2 );
						int pY = prevPacket.getDataRangeInt( 3, 2 );
						int pFX = prevPacket.getDataRangeInt( 5, 1 );
						int pFY = prevPacket.getDataRangeInt( 6, 1 );
						int pFORCE = prevPacket.getDataRangeInt( 7, 1 );
						long pTimeLong = MTIME;

						this.processDot( sectionId, ownerId, noteId, pageId, pTimeLong, pX, pY, pFX, pFY, pFORCE, DotType.PEN_ACTION_UP.getValue(), currColor );
					}
					else
					{
                        //[2018.03.05] Stroke Test
                        //Check if it is miss pen move or pen down & move, and then send error message.
                        int errorType = isStartWithDown ? JsonTag.ERROR_TYPE_MISSING_PEN_MOVE : JsonTag.ERROR_TYPE_MISSING_PEN_DOWN_PEN_MOVE;
                        int penMsgType = isStartWithDown ? PenMsgType.ERROR_MISSING_PEN_MOVE : PenMsgType.ERROR_MISSING_PEN_DOWN_PEN_MOVE;
						// Previous dot was Down or Up dot. Missing Move Dots or get Only Up Dot.
						NLog.e( "[CommProcessor] Missing Pen Down Pen Move " +
										"TimeStamp="+ -1 + ",ErrorType="+ errorType );

						try
						{
							JSONObject job = new JSONObject()
                                    .put( JsonTag.LONG_LOG_PEN_DOWN_TIMESTAMP, down_MTIME )
									.put( JsonTag.LONG_LOG_TIMESTAMP, prevDotTime )
									.put( JsonTag.INT_LOG_ERROR_TYPE, errorType);
							btConnection.onCreateMsg( new PenMsg( penMsgType, job ) );
						}catch ( Exception e )
						{
							e.printStackTrace();
						}
					}

					isStartWithDown = false;

                    isReceivedPageIdChange = false;     //[2018.03.05] Stroke Test
				}

				this.prevPacket = null;

//                clearMakeUpDotTimer();    //[2018.03.05] Stroke Test

				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_DotIDChange (0x12)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_DotIDChange:

                isReceivedPageIdChange = true;      //[2018.03.05] Stroke Test

				// Change note page information
				noteId = pack.getDataRangeInt( 0, 2 );
				pageId = pack.getDataRangeInt( 2, 2 );

				NLog.d( "[CommProcessor] received A_DotIDChange = "+sectionId+",ownerId="+ownerId+",noteId=" + noteId );
				break;

			/*
			 * ------------------------------------------------------------------
			 * 
			 * A_DotIDChange32 (0x15)
			 * 
			 * ------------------------------------------------------------------
			 */
			case CMD.A_DotIDChange32:
                isReceivedPageIdChange = true;      //[2018.03.05] Stroke Test

				// Change note page information
				byte[] osbyte = pack.getDataRange( 0, 4 );
				prevSectionId = sectionId;
				prevOwnerId = ownerId;
				prevNoteId = noteId;
				prevPageId = pageId;

				sectionId = (int) (osbyte[3] & 0xFF);
				ownerId = ByteConverter.byteArrayToInt( new byte[] { osbyte[0], osbyte[1], osbyte[2], (byte) 0x00 } );

				noteId = pack.getDataRangeInt( 4, 4 );
				pageId = pack.getDataRangeInt( 8, 4 );

				NLog.d( "[CommProcessor] received A_DotIDChange32 = "+sectionId+",ownerId="+ownerId+",noteId=" + noteId );

				if ( prevPacket != null )
				{
					// In case of pen-up, Insert previous dots as End Dots
					int pX = prevPacket.getDataRangeInt( 1, 2 );
					int pY = prevPacket.getDataRangeInt( 3, 2 );
					int pFX = prevPacket.getDataRangeInt( 5, 1 );
					int pFY = prevPacket.getDataRangeInt( 6, 1 );
					int pFORCE = prevPacket.getDataRangeInt( 7, 1 );
					long pTimeLong = prevDotTime;

					this.processDot( prevSectionId, prevOwnerId, prevNoteId, prevPageId, pTimeLong, pX, pY, pFX, pFY, pFORCE, DotType.PEN_ACTION_UP.getValue(), currColor );

					NLog.d( "[CommProcessor] it write pen continually between another page!!" + ",Section=" + prevSectionId + ",Owner="+prevOwnerId+",Note="+prevNoteId+",Page"+prevPageId+",X="+pX+",Y="+pY+",Force="+pFORCE );

					isPrevDotDown = true;
					isStartWithDown = true;

//					clearMakeUpDotTimer();      //[2018.03.05] Stroke Test
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

				// Get the Pen Firmware Version
				SW_VER = pack.getDataRangeString( 10, 5 ).trim();

				NLog.d( "[CommProcessor] version of connected pen is " + SW_VER );

				String[] temp = SW_VER.split( "\\." );

				try
				{
					firmWareVer = Float.parseFloat( temp[0]+"."+temp[1] );
				}catch ( Exception e )
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
				String stat_version = pack.getDataRangeString( 0, 1 ).trim();
				String stat_status = pack.getDataRangeString( 1, 1 ).trim();

				int stat_timezone = pack.getDataRangeInt( 2, 4 );
				long stat_timetick = pack.getDataRangeLong( 6, 8 );
				int stat_forcemax = pack.getDataRangeInt( 14, 1 );
				stat_battery = pack.getDataRangeInt( 15, 1 );
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
					int len = pack.getDataRangeInt( 29, 1);
					connectedDeviceName = pack.getDataRangeString( 30, len).trim();
				}catch ( Exception e )
				{
				}

				if ( !isPenAuthenticated )
				{
					try
					{
						JSONObject job = new JSONObject()
								.put( JsonTag.STRING_PROTOCOL_VERSION, "1" )
								.put( JsonTag.STRING_PEN_FW_VERSION, SW_VER );
						// The pen that passes ModelName is assumed to be named F110C,
						// The pen that does not passes ModelName is assumed to be named F110.
						// by  Juyeon Lee
						if(connectedDeviceName == null)
						{
							sensorType = 0;
							job.put( JsonTag.INT_PRESS_SENSOR_TYPE, sensorType );
						}
						else
						{
							if(connectedDeviceName.equals( PEN_MODEL_NAME_F110 ))
								sensorType = 0;
							else
								sensorType = 1;
							job.put(  JsonTag.STRING_DEVICE_NAME, connectedDeviceName);
							job.put( JsonTag.INT_PRESS_SENSOR_TYPE, sensorType );
						}


						btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_FW_VERSION, job ) );
					}
					catch ( JSONException e )
					{
						e.printStackTrace();
					}



					isPenAuthenticated = true;
					btConnection.onAuthorized();

					try
					{
						JSONObject job = new JSONObject()
								.put( JsonTag.STRING_PEN_MAC_ADDRESS, btConnection.getMacAddress() )
								.put( JsonTag.INT_BATTERY_STATUS, stat_battery )
								.put( JsonTag.STRING_PEN_PASSWORD, currentPassword );

						btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_AUTHORIZED, job ) );
					}
					catch ( JSONException e )
					{
						e.printStackTrace();
					}
				}


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
					extraData = null;
					btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_SEND_FAILURE ) );
				}

				break;

			case CMD.A_OfflineResultResponse:
				if ( pack.getDataRangeInt( 0, 1 ) == 0x00 )
				{
					extraData = null;
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
				olFile = new OfflineFile( btConnection.getMacAddress(), FILE_NAME, PACKET_COUNT, FILE_NAME.endsWith( ".zip" ) ? true : false );

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

				// If the checksum is wrong, or if the count and size information do not match, discard.
				if ( cs[0] == Chunk.calcChecksum( data ) && PACKET_COUNT > index && PACKET_SIZE >= data.length )
				{
					mHandler.removeCallbacks( mChkOfflineFailRunnable );
					olFile.append( data, index );

					// If you have received the chunk, process the offline data.
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
								if(extraData != null)
									job.put( JsonTag.OFFLINE_EXTRA, extraData);
								btConnection.onCreateMsg( new PenMsg( PenMsgType.OFFLINE_DATA_FILE_CREATED, job ) );
							}
							catch ( JSONException e )
							{
								e.printStackTrace();
							}
						}

						olFile.clearTempFile(btConnection.getMacAddress());
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
			{
				int result = pack.getDataRangeInt( 0, 1 );
				NLog.d( "[CommProcessor] A_UsingNoteNotifyResponse : " + result );
				boolean isSuccess = result == 1 ? true : false;
				try
				{
					JSONObject job = new JSONObject().put( JsonTag.BOOL_RESULT, isSuccess );
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_USING_NOTE_SET_RESULT ,job) );
				}catch ( Exception e )
				{
					e.printStackTrace();
				}
			}
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
					reqInputPassword( );
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

			case CMD.A_ProfileResponse:
			{
				JSONArray jsonArray = null;
				int resultCode = pack.getResultCode();
				String profile_name = pack.getDataRangeString( 0, 8 ).trim();
				int res_type = pack.getDataRangeInt( 8, 1 );

				if ( resultCode == 0x00 )
				{
					JSONObject jsonObject = new JSONObject();
					if ( res_type == PenProfile.PROFILE_CREATE )
					{
						int res_status = pack.getDataRangeInt( 9, 1 );
						try
						{
							jsonObject.put( JsonTag.STRING_PROFILE_NAME, profile_name );
							jsonObject.put( JsonTag.INT_PROFILE_RES_STATUS, res_status );
//							jsonObject.put( JsonTag.STRING_PROFILE_RES_MSG, res_status );
						}
						catch ( JSONException e )
						{
							e.printStackTrace();
						}
						btConnection.onCreateMsg( new PenMsg( PenMsgType.PROFILE_CREATE, jsonObject ) );
					}
					else if ( res_type == PenProfile.PROFILE_DELETE )
					{
						int res_status = pack.getDataRangeInt( 9, 1 );

						try
						{
							jsonObject.put( JsonTag.STRING_PROFILE_NAME, profile_name );
							jsonObject.put( JsonTag.INT_PROFILE_RES_STATUS, res_status );
						}
						catch ( JSONException e )
						{
							e.printStackTrace();
						}
						btConnection.onCreateMsg( new PenMsg( PenMsgType.PROFILE_DELETE, jsonObject ) );

					}
					else if ( res_type == PenProfile.PROFILE_INFO )
					{
						int res_status = pack.getDataRangeInt( 9, 1 );
						try
						{
							jsonObject.put( JsonTag.STRING_PROFILE_NAME, profile_name );
							jsonObject.put( JsonTag.INT_PROFILE_RES_STATUS, res_status );
							if(res_status == 0x00)
							{
								int total = pack.getDataRangeInt( 18, 2 );
								int sector_size = pack.getDataRangeInt( 20, 2 );
								int use = pack.getDataRangeInt( 22, 2 );
								int key = pack.getDataRangeInt( 24, 2 );
								jsonObject.put( JsonTag.INT_PROFILE_INFO_TOTAL_SECTOR_COUNT, total );
								jsonObject.put( JsonTag.INT_PROFILE_INFO_SECTOR_SIZE, sector_size );
								jsonObject.put( JsonTag.INT_PROFILE_INFO_USE_SECTOR_COUNT, use );
								jsonObject.put( JsonTag.INT_PROFILE_INFO_USE_KEY_COUNT, key );
							}
						}
						catch ( JSONException e )
						{
							e.printStackTrace();
						}
						btConnection.onCreateMsg( new PenMsg( PenMsgType.PROFILE_DELETE, jsonObject ) );
					}
					else if ( res_type == PenProfile.PROFILE_READ_VALUE )
					{
						try
						{
							jsonObject.put( JsonTag.STRING_PROFILE_NAME, profile_name );
							int count = pack.getDataRangeInt( 9, 1 );
							jsonArray = new JSONArray( );
							int data_offset = 0;
							data_offset = 10;
							for(int i = 0; i < count; i++)
							{
								String key = pack.getDataRangeString( data_offset, 16 ).trim();
								data_offset += 16;
								int key_status = pack.getDataRangeInt( data_offset, 1 );
								data_offset++;
								int key_size = pack.getDataRangeInt( data_offset, 2 );
								data_offset += 2;
								byte[] data = pack.getDataRange(data_offset, key_size );
								data_offset += key_size;
								jsonArray.put( new  JSONObject().put(JsonTag.STRING_PROFILE_KEY, key  ).put( JsonTag.INT_PROFILE_RES_STATUS, key_status ).put( JsonTag.BYTE_PROFILE_VALUE, Base64.encodeToString(data ,Base64.DEFAULT)) );
							}
							jsonObject.put( JsonTag.ARRAY_PROFILE_RES, jsonArray);
						}
						catch ( JSONException e )
						{
							e.printStackTrace();
						}
						btConnection.onCreateMsg( new PenMsg( PenMsgType.PROFILE_READ_VALUE, jsonObject ) );
					}
					else if ( res_type == PenProfile.PROFILE_WRITE_VALUE )
					{
						try
						{
							jsonObject.put( JsonTag.STRING_PROFILE_NAME, profile_name );
							int count = pack.getDataRangeInt( 9, 1 );
							jsonArray = new JSONArray( );
							int data_offset = 0;
							data_offset = 10;
							for(int i = 0; i < count; i++)
							{
								String key = pack.getDataRangeString( data_offset, 16 ).trim();
								data_offset += 16;
								int key_status = pack.getDataRangeInt( data_offset, 1 );
								data_offset++;
								jsonArray.put( new  JSONObject().put(JsonTag.STRING_PROFILE_KEY, key  ).put( JsonTag.INT_PROFILE_RES_STATUS, key_status ) );
							}
							jsonObject.put( JsonTag.ARRAY_PROFILE_RES, jsonArray);
						}
						catch ( JSONException e )
						{
							e.printStackTrace();
						}
						btConnection.onCreateMsg( new PenMsg( PenMsgType.PROFILE_WRITE_VALUE, jsonObject ) );

					}
					else if ( res_type == PenProfile.PROFILE_DELETE_VALUE )
					{
						try
						{
							jsonObject.put( JsonTag.STRING_PROFILE_NAME, profile_name );
							int count = pack.getDataRangeInt( 9, 1 );
							jsonArray = new JSONArray( );
							int data_offset = 0;
							data_offset = 10;
							for(int i = 0; i < count; i++)
							{
								String key = pack.getDataRangeString( data_offset, 16 ).trim();
								data_offset += 16;
								int key_status = pack.getDataRangeInt( data_offset, 1 );
								data_offset++;
								jsonArray.put( new  JSONObject().put(JsonTag.STRING_PROFILE_KEY, key  ).put( JsonTag.INT_PROFILE_RES_STATUS, key_status ) );
							}
							jsonObject.put( JsonTag.ARRAY_PROFILE_RES, jsonArray);
						}
						catch ( JSONException e )
						{
							e.printStackTrace();
						}
						btConnection.onCreateMsg( new PenMsg( PenMsgType.PROFILE_DELETE_VALUE, jsonObject ) );
					}
				}
				else
				{
					btConnection.onCreateMsg( new PenMsg( PenMsgType.PROFILE_FAILURE ) );
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
		if(factor != null)
			dot.pressure = (int)factor[dot.pressure];
		btConnection.onCreateDot( dot );
	}

	@Override
	public void onCreatePacket( Packet packet )
	{
		parsePacket( packet );
	}

	/**
	 * Pen RTC Setting
	 */
	public void reqSetCurrentTime()
	{
		execute( new SetTimeCommand( CMD.P_RTCset, this ) );
	}

	/**
	 * Pen status request
	 */
	public void reqPenStatus()
	{
		write( ProtocolParser.buildPenStatusData() );
	}

	/**
	 * force calibrate request
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

	public void reqOfflineData( Object extra, int sectionId, int ownerId, int noteId )
	{
		if(extra instanceof Integer || extra instanceof Long || extra instanceof String || extra instanceof Double || extra instanceof JSONObject || extra instanceof JSONArray)
		{
			extraData = extra;
			write( ProtocolParser.buildReqOfflineData( sectionId, ownerId, noteId ) );
		}
		else
		{
			extraData = null;
			write( ProtocolParser.buildReqOfflineData( sectionId, ownerId, noteId ) );
			NLog.e( "[CommProcessor] reqOfflineData Unsupported extra Type!! Support extra Type is Integer, Long , Double, String, JSONObject, JSONArray" );

		}
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


	private void reqInputPassword()
	{
		currentPassword = "";
		write( ProtocolParser.buildPasswordInput( currentPassword ) );

	}

	public void reqInputPassword( String password )
	{
        if ( password.equals( "0000" ) )
        {
            btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_ILLEGAL_PASSWORD_0000) );
            return;
        }

		currentPassword = password;
        write( ProtocolParser.buildPasswordInput( password ) );
	}

	public void reqSetUpPassword( String oldPassword, String newPassword )
    {
        if ( newPassword.equals( "0000" ) )
        {
            btConnection.onCreateMsg( new PenMsg( PenMsgType.PEN_ILLEGAL_PASSWORD_0000) );
            return;
        }
		write( ProtocolParser.buildPasswordSetup( oldPassword, newPassword ) );
	}

	/**
	 * Pen SW Upgrade request
	 *
	 * @param source the source
	 * @param target the target
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
	 * Gets press sensor type.
	 *
	 * @return the press sensor type
	 */
	public int getPressSensorType()
	{
		return sensorType;
	}


	@Override
	public boolean isSupportPenProfile ()
	{
//		if(connectedDeviceName != null)
//		{
//			if(connectedDeviceName.equals( PEN_MODEL_NAME_F110 ) && firmWareVer >= PEN_PROFILE_SUPPORT_VERSION_F110)
//				return true;
//			else if(connectedDeviceName.equals( PEN_MODEL_NAME_F110C ) && firmWareVer >= PEN_PROFILE_SUPPORT_VERSION_F110C)
//				return true;
//			else
//				return false;
//		}
		return false;
	}

	@Override
	public boolean isSupportOfflineNoteInfo()
	{
		return false;
	}

	@Override
	public boolean isSupportCountLimit()
	{
		return false;
	}

	@Override
	public boolean isSupportHoverCommand()
	{
		return false;
	}

	@Override
	public void createProfile ( String proFileName, byte[] password )
	{
		write( ProtocolParser.buildProfileCreate(proFileName, password ) );

	}

	@Override
	public void deleteProfile ( String proFileName, byte[] password )
	{
		write( ProtocolParser.buildProfileDelete(proFileName, password ) );

	}

	@Override
	public void writeProfileValue ( String proFileName, byte[] password ,String[] keys, byte[][] data )
	{
		write( ProtocolParser.buildProfileWriteValue(proFileName, password ,keys ,data) );

	}

	@Override
	public void readProfileValue ( String proFileName, String[] keys )
	{
		write( ProtocolParser.buildProfileReadValue(proFileName, keys ) );

	}

	@Override
	public void deleteProfileValue ( String proFileName, byte[] password, String[] keys )
	{
		write( ProtocolParser.buildProfileDeleteValue(proFileName, password, keys) );

	}

	@Override
	public void getProfileInfo ( String proFileName )
	{
		write( ProtocolParser.buildProfileInfo(proFileName) );

	}

	/**
	 * Responding to a request to send a pen SW from a pen
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
	 * Processing according to pen SW upgrade status
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
	public IChunk getChunk()
	{
		return this.chunk;
	}

	public void setChunk( IChunk chunk )
	{
		this.chunk = chunk;
	}

    /**
     * Clear timer for make up dot when pen up is missed.
     */
    private void clearMakeUpDotTimer() {    //[2018.03.05] Stroke Test
        if(mChkMissingPenUpRunnable != null){
            mHandler.removeCallbacks(mChkMissingPenUpRunnable);
        }
    }

    /**
     * Set timer for make up dot when pen up is missed.
     */
    private void setMakeUpDotTimer() {  //[2018.03.05] Stroke Test
        if(mChkMissingPenUpRunnable != null){
            mHandler.removeCallbacks(mChkMissingPenUpRunnable);
        }
        mHandler.postDelayed(mChkMissingPenUpRunnable, 1000);
    }

	@Override
	public boolean isHoverMode()
	{
		return false;
	}
}
