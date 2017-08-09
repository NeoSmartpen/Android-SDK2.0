package kr.neolab.samplecode;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.DotType;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.IMetadataCtrl;
import kr.neolab.sdk.metadata.MetadataCtrl;
import kr.neolab.sdk.metadata.structure.Symbol;
import kr.neolab.sdk.pen.IPenCtrl;
import kr.neolab.sdk.pen.PenCtrl;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.util.NLog;

public class NeoSampleService extends Service{

	public static Boolean SAVE_QUE_DATA_PLAN_B = false;


//	public static final String ACTION_SEND_DOT = "kr.neolab.samplecode" +".broadcast"+".action"+ ".send_dot";
//	public static final String EXTRA_DOT = "dot";

//	public static final String ACTION_READY_RECEIVE = "kr.neolab.samplecode" +".broadcast"+".action"+ ".ready_receive";
//	public static final String EXTRA_READY = "ready";
//	public static final String EXTRA_SAVE_TAG = "save_tag";

//	public static final String ACTION_CURRENT_VIEW_PAGE = "kr.neolab.samplecode" +".broadcast"+".action"+ ".current_view";

//	public static final String ACTION_SYMBOL_ACTION = "kr.neolab.samplecode" +".broadcast"+".action"+ ".symbol_action";
//	public static final String EXTRA_SYMBOL_ID = "symbolId";
//	public static final String EXTRA_DOT = "dot";

//	public static final String EXTRA_CURRENT_PAGE = "current_page";


	private IPenCtrl mPenCtrl;
	private Queue<Dot> mDotQueueForDB = null;
	private Queue<Dot> mDotQueueForBroadcast = null;
	private DotConsumerForDBThread mDBThread = null;
	private DotConsumerForBroadcastThread mBroadcastThread = null;

	private int curSectionId, curOwnerId,  curBookcodeId, curPageNumber;	// 현재 작성중인 notebook 과 page number

//	private PageInfo currentPageInfo = null;
//	private String currentSaveTag = "";
//	private int currentPageNumber = -1;
	private boolean ready = false;

	private Dot checkSymbolDownDot = null;
	ArrayList<Symbol> checkSymbols = new ArrayList<Symbol>();

	IMetadataCtrl metadataCtrl;
	private Handler mHandler;

//	private BroadcastReceiver readyBroadcastReceiver = new BroadcastReceiver()
//	{
//		@Override
//		public void onReceive ( Context context, Intent intent )
//		{
//			if(intent != null && intent.getAction().equals( ACTION_READY_RECEIVE ))
//			{
//				ready = intent.getBooleanExtra( EXTRA_READY, false );
//				NLog.d( "ACTION_READY_RECEIVE ready="+ready );
//
////				currentSaveTag = intent.getStringExtra( EXTRA_SAVE_TAG );
////				currentPageNumber = intent.getIntExtra( EXTRA_CURRENT_PAGE, -1 );
//
//				if(ready)
//				{
//					if(SAVE_QUE_DATA_PLAN_B)
//					{
//						synchronized (mDotQueueForDB) {
//							mDotQueueForDB.notifyAll();
//						}
//						synchronized (mDotQueueForBroadcast) {
//							mDotQueueForBroadcast.notifyAll();
//						}
//					}
//					else
//					{
//						synchronized (mDotQueueForDB) {
//							mDotQueueForDB.clear();
//						}
//						synchronized (mDotQueueForBroadcast) {
//							mDotQueueForBroadcast.clear();
//						}
//					}
//				}
//			}
//			else if(intent != null && intent.getAction().equals( ACTION_CURRENT_VIEW_PAGE ))
//			{
//				int sectionId = intent.getIntExtra( EXTRA_SECTION_ID,-1 );
//				int ownerId = intent.getIntExtra( EXTRA_OWNER_ID,-1 );
//				int bookcodeId = intent.getIntExtra( EXTRA_BOOKCODE_ID,-1 );
//				int pagenumber = intent.getIntExtra( EXTRA_PAGE_NUMBER,-1 );
//				curSectionId = sectionId;
//				curOwnerId = ownerId;
//				curBookcodeId = bookcodeId;
//				curPageNumber = pagenumber;
//			}
//		}
//	};

    @Override
	public void onCreate ()
	{
		super.onCreate();
//		registerBroadcastReceiver();
		mPenCtrl = PenCtrl.getInstance();
		mPenCtrl.setDotListener( mPenReceiveDotListener );
		mPenCtrl.setOffLineDataListener( mOfflineDataListener );

		metadataCtrl = MetadataCtrl.getInstance();


		////////////////////////////////////////테스트
		mHandler = new Handler( );


		try
		{
			File f = new File( Const.SAMPLE_FOLDER_PATH );
			File[] fileNames = f.listFiles();

			for ( int i = 0; i < fileNames.length; i++ )
			{
				final File file = fileNames[i];

				if ( file.isFile() )
				{

//					final String fileName = file.getName().toLowerCase( Locale.US );
//					if ( !fileName.endsWith( ".nproj" ) )
//					{
//						continue;
//					}
//
//					final InputStream is = new FileInputStream( file );

					new Thread( new Runnable()
					{
						@Override
						public void run ()
						{
							try
							{
//								NLog.d( "parseBySAX fileName="+fileName );

								metadataCtrl.loadFile( file );
							}
							catch ( Exception e )
							{
								e.printStackTrace();
							}

						}
					} ).start();

				}
			}
		}
		catch ( Exception e )
		{
			e.printStackTrace();
		}


		////////////////////////////////////////테스트


//		metadataCtrl.loadFiles( Const.SAMPLE_FOLDER_PATH);

//		NLog.d( "Page Width=" +metadataCtrl.getPageWidth( 8, 2));

		mDotQueueForDB = new ConcurrentLinkedQueue<Dot>();
		mDotQueueForBroadcast = new ConcurrentLinkedQueue<Dot>();

		mDBThread = new DotConsumerForDBThread( NeoSampleService.this );
		mDBThread.setDaemon( true );
		mDBThread.start();

		mBroadcastThread = new DotConsumerForBroadcastThread();
		mBroadcastThread.setDaemon( true );
		mBroadcastThread.start();

		NLog.d( "Service Initialize complete" );
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		NLog.d( "onDestroy" );
		
		if(mDBThread != null)
			mDBThread.interrupt();
		if(mBroadcastThread != null)
			mBroadcastThread.interrupt();
//		unRegisterBroadcastReceiver( );

//		android.os.Process.killProcess( android.os.Process.myPid() );
	}

//	private void registerBroadcastReceiver()
//	{
//		IntentFilter i = new IntentFilter( );
//		i.addAction(ACTION_READY_RECEIVE);
//		i.addAction( ACTION_CURRENT_VIEW_PAGE );
//		registerReceiver( readyBroadcastReceiver,i );
//	}

//	private void unRegisterBroadcastReceiver()
//	{
//		unregisterReceiver( readyBroadcastReceiver );
//	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		NLog.d( "onStartCommand: " + startId );
		return START_STICKY;
	}

	@Override
	public IBinder onBind ( Intent intent )
	{
		NLog.d( "onBind - service binding" );
		return null;
	}


	@Override
	public void onRebind(Intent intent) {
		super.onRebind( intent);
		NLog.d( "onRebind" );
	}


	@Override
	public boolean onUnbind(Intent intent) {
		NLog.d( "onUnbind" );
		return super.onUnbind(intent);
	}
	
	private void enqueueDot(Dot dot){
		mDotQueueForDB.offer(dot);
		
		synchronized (mDotQueueForDB) {
			mDotQueueForDB.notifyAll();	
		}
	}
	
	private void enqueueDotForBroadcast(Dot dot){
		mDotQueueForBroadcast.offer(dot);

		synchronized (mDotQueueForBroadcast) {
			mDotQueueForBroadcast.notifyAll();	
		}
	}
	
	private void broadcastDot ( Dot dot )
	{

		NLog.d( "broadcastDot send: sectionId:" + dot.sectionId + " ownerId:" + dot.ownerId + " noteId:" + dot.noteId + " pageId:" + dot.pageId + " dotType:" + dot.dotType+",X="+dot.getX()+",Y="+dot.getY() );

		Intent intent = new Intent( Const.Broadcast.ACTION_PEN_DOT);
		intent.putExtra( Const.Broadcast.EXTRA_DOT, dot);
		sendBroadcast( intent );
//		LocalBroadcastManager.getInstance( this ).sendBroadcast(intent);
	}




	 // dot broadcast thread 가 멈춰 있는 상태에서 페이지가 추가 되거나 노트북이 변경되었을 때,
	  //PageDetailActivity 에서 dot 가 전달되지 않아 입력 페이지(PageRenderFragment) 로 ViewPager 를 이동 시킬 수 없는 문제가 있음
	  //(Dot 입력이 없어 ViewPager 가 이동 할 페이지를 알 수 없음)
	  //기존의 방식으로 dot 입력을 확인 해 page 를 이동하는 방식에 추가로 페이지나 노트북이 변경되었음을 알리는 broadcast 사용

	  //PageDetailActivity 는 이 broadcast 를 받고, 현재 입력되는 notebook 과 pagenumber 를 이용해 onLoadFinish 에서 해당 페이지로 이동시킴
	  //해당 페이지로 이동이 되면 PageRenderFragment 가 dot receiver 를 등록하고 dot 를 받을 수 있게 됨
	private void sendBroadcastIfPageChanged(int sectionId, int ownerId, int bookcodeId, int pageNumber){
		if(curSectionId != sectionId || curOwnerId != ownerId || curBookcodeId != bookcodeId ){
			curSectionId = sectionId;
			curOwnerId = ownerId;
			curBookcodeId = bookcodeId;

			curPageNumber = -1;
			curBookcodeId = bookcodeId;
		}
		
		if(curPageNumber != pageNumber){
			curPageNumber = pageNumber;
//			currentPageInfo = ActionController.getInstance( this ).getPageInfo( sectionId, ownerId, bookcodeId, pageNumber);
			sendPageChangedBroadcast();
			// 페이지가 바뀌면 ready 를 false 로..(도트데이터를 일단 넘기지 않음)
			ready = false;
			NLog.d( "sendBroadcastIfPageChanged ready="+ready );
		}
	}

	private void checkSymbol(Dot dot)
	{
		if(dot.penTipType != Stroke.PEN_TIP_TYPE_NORMAL)
			return;
		if ( DotType.isPenActionDown(dot.dotType)) {
			checkSymbolDownDot = dot;
		}
		else if(DotType.isPenActionUp(dot.dotType))
		{
			if(metadataCtrl!= null && checkSymbolDownDot != null)
			{
				Symbol[] upSymbols = metadataCtrl.findApplicableSymbols(dot.noteId,dot.pageId, dot.x, dot.y );
				Symbol[] downSymbols = metadataCtrl.findApplicableSymbols( checkSymbolDownDot.noteId, checkSymbolDownDot.pageId, checkSymbolDownDot.x, checkSymbolDownDot.y );
				checkSymbolDownDot = null;
				checkSymbols.clear();


				Symbol[] testSymbols = metadataCtrl.findApplicableSymbols(dot.noteId,dot.pageId );
				if(testSymbols != null)
				{
					for(Symbol symbol: testSymbols)
					{
						NLog.d( "testSymbols symbol=  x="+symbol.getX()+",y="+symbol.getY()+",w="+symbol.getWidth()+",h="+symbol.getHeight() );
					}
				}

				if(upSymbols == null && downSymbols == null)
					return;
				for(Symbol upSymbol: upSymbols)
				{
					for(Symbol downSymbol: downSymbols)
					{
						if(upSymbol.getId().equals( downSymbol.getId() ))
						{
							checkSymbols.add(upSymbol);
							break;
						}
					}
				}
				for(Symbol symbol: checkSymbols)
				{
					Intent intent = new Intent(Const.Broadcast.ACTION_SYMBOL_ACTION);
					intent.putExtra( Const.Broadcast.EXTRA_SECTION_ID, curSectionId);
					intent.putExtra(Const.Broadcast.EXTRA_OWNER_ID, curOwnerId);
					intent.putExtra(Const.Broadcast.EXTRA_BOOKCODE_ID, curBookcodeId);
					intent.putExtra(Const.Broadcast.EXTRA_PAGE_NUMBER, curPageNumber);
					intent.putExtra(Const.Broadcast.EXTRA_SYMBOL_ID, symbol.getId());
					sendBroadcast( intent );
				}
			}
		}
	}
	
	private IPenDotListener mPenReceiveDotListener = new IPenDotListener() {

		@Override
		public void onReceiveDot ( Dot dot )
		{
			NLog.d( "NeoSampleService onReceiveDot dotType=" + dot.dotType+" ,pressure="+dot.pressure+",x="+dot.getX()+",y="+dot.getY() );
			checkSymbol( dot);
			sendBroadcastIfPageChanged(dot.sectionId, dot.ownerId, dot.noteId, dot.pageId);
			enqueueDot( dot );
			enqueueDotForBroadcast(dot);

		}
	};

	private IOfflineDataListener mOfflineDataListener = new IOfflineDataListener()
	{
		@Override
		public void onReceiveOfflineStrokes ( Stroke[] strokes, int sectionId, int ownerId, int noteId )
		{
			// 도트가 0인 데이터 필터링
			ArrayList<Stroke> newArrayList = new ArrayList<Stroke>();
			for(Stroke s : strokes)
			{
				if(s.size() > 0 )
				{
					NLog.d( "onReceiveOfflineStrokes s sectionId="+s.sectionId+" ownerId="+s.ownerId+" noteId="+s.noteId+" pageId="+s.pageId );
					s.color = Color.BLACK;
					newArrayList.add( s );
				}
				else
				{
					NLog.e( "onReceiveOfflineStrokes Dot size =0!!");
				}
			}
			Intent i = new Intent( Const.Broadcast.ACTION_OFFLINE_STROKES );
			i.putExtra( Const.Broadcast.EXTRA_OFFLINE_STROKES, newArrayList.toArray(new Stroke[newArrayList.size()]) );
			getApplicationContext().sendBroadcast( i );

			// DB 에 insert

		}
	};
	

	private void sendPageChangedBroadcast(){
		
		NLog.i( "Page Changed-> curSectionId:" + curSectionId + " curOwnerId:" + curOwnerId + " curBookcodeId:" + curBookcodeId+ " curPageNumber:" + curPageNumber);
		
		Intent intent = new Intent(Const.Broadcast.ACTION_WRITE_PAGE_CHANGED);
		intent.putExtra(Const.Broadcast.EXTRA_SECTION_ID, curSectionId);
		intent.putExtra(Const.Broadcast.EXTRA_OWNER_ID, curOwnerId);
		intent.putExtra(Const.Broadcast.EXTRA_BOOKCODE_ID, curBookcodeId);
		intent.putExtra(Const.Broadcast.EXTRA_PAGE_NUMBER, curPageNumber);
		
		sendBroadcast( intent );
	}
	
	
	private final class DotConsumerForDBThread extends Thread{
		private int dotCount = 0;
		private int eraserDotCount = 0;

		private int currentSectionId = -1;
		private int currentOwnerId = -1;
		private int currentBookcodeId = -1;
		private int currentPageId = -1;
//		ActionController mActionController;
		private ArrayList<Dot> dotArray = new ArrayList<Dot>(100);

//		private ArrayList<Dot> eraserDotArray = new ArrayList<Dot>(100);

		public DotConsumerForDBThread(Context context) {
			super();
//			mActionController = ActionController.getInstance(context);
		}
		

		@Override
		public void run() {
			
			setName(this.getClass().getSimpleName());
			
			while(true){
				
				while(!mDotQueueForDB.isEmpty()){
					Dot dot = null;
//					if(ready)
//					{
						dot = mDotQueueForDB.poll();
//					}
//					else
//					{
//						break;
//					}
					if(dot != null )
						insert(dot); // offline data 전송중이면 Symbol 무시
				}
				
				try{
					synchronized (mDotQueueForDB) {
						mDotQueueForDB.wait();
					}
				} catch (InterruptedException e){
//					e.printStackTrace();
					NLog.d( "DotConsumerThread Interrupted!!" + e );
				}
			}
		}

		public void insert(Dot dot ){
			checkNotebookAndPageChange( dot );

			if (DotType.isPenActionDown(dot.dotType)) {
				checkNotebookAndPageChange( dot );
				if(dot.penTipType == Stroke.PEN_TIP_TYPE_NORMAL)
				{
					dotArray.add(dot);
					dotCount++;
				}
			}
			// dot middle, adding to stroke
			else if (DotType.isPenActionMove(dot.dotType)) {

				// 스트로크 시작 dot 가 없는데 move dot 이 들어오면
				// 시작 dot 을 추가
				if(dot.penTipType == Stroke.PEN_TIP_TYPE_NORMAL)
				{
					if(dotCount == 0){
						Dot startDot = new Dot( dot.x, dot.y, dot.pressure, DotType.PEN_ACTION_DOWN.getValue(), dot.timestamp, dot.sectionId, dot.ownerId, dot.noteId, dot.pageId, dot.color,dot.penTipType, dot.tiltX, dot.tiltY,dot.twist);
						insert(startDot);
					}
					dotArray.add(dot);
					dotCount++;
				}
			}
			// dot end, insert to db and then flush data
			else if (DotType.isPenActionUp(dot.dotType)) {

				if(dot.penTipType == Stroke.PEN_TIP_TYPE_NORMAL)
				{
					dotArray.add(dot);
					dotCount++;
					insertStrokeDotsArray(dotArray, false);
				}
			}
		}

//		private int currentSectionId = -1;
//		private int currentOwnerId = -1;
//		private int currentBookcodeId = -1;
//		private int currentPageId = -1;

		private void checkNotebookAndPageChange(Dot dot) {
			boolean changed = false;
			if (currentSectionId != dot.sectionId || currentOwnerId != dot.ownerId || currentBookcodeId != dot.noteId || currentPageId != dot.pageId) {
				currentSectionId = dot.sectionId;
				currentOwnerId = dot.ownerId;
				currentBookcodeId = dot.noteId;
				currentPageId = dot.pageId;
				changed = true;
			}

			if(changed)
			{
				if(dotArray.size() > 0){
					Dot lastDot = dotArray.get(dotArray.size()-1);
					//makeUpDot
					Dot upDot = new Dot( lastDot.x, lastDot.y, lastDot.pressure, DotType.PEN_ACTION_UP.getValue(), lastDot.timestamp, lastDot.sectionId, lastDot.ownerId, lastDot.noteId, lastDot.pageId, lastDot.color,lastDot.penTipType, lastDot.tiltX, lastDot.tiltY, lastDot.twist);
					dotArray.add( upDot );
					insertStrokeDotsArray(dotArray, false);
				}
			}
		}

		private void insertStrokeDotsArray(ArrayList<Dot> dotArray, boolean isEraser){
			Stroke s = null;
			for(Dot d : dotArray)
			{
				if(s == null)
				{
					s = new Stroke(d.sectionId, d.ownerId, d.noteId, d.pageId, Color.BLACK );
					s.penTipType = d.penTipType;
					if(s.penTipType == Stroke.PEN_TIP_TYPE_NORMAL)
						s.thickness = 1;
				}
				s.add( d );
			}
			dotArray.clear();
			if(isEraser)
				eraserDotCount = 0;
			else
				dotCount = 0;
			//DB Insert
//			mActionController.addStroke( s ,currentSaveTag);
		}

	}
	
	private final class DotConsumerForBroadcastThread extends Thread{
		
		@Override
		public void run() {
			
			setName( this.getClass().getSimpleName() );

			
			while(true){
				
				while(!mDotQueueForBroadcast.isEmpty()){
					Dot dot = null;
					// broadcast 할 dot 를 받을 page 가 준비(dot receiver 등록)가 되어 있을 때만 broadcast
//					if(ready)
//					{
						dot = mDotQueueForBroadcast.poll();
//					}
//					else
//					{
//						dot = mDotQueueForBroadcast.peek();
//						NLog.d( "DotConsumerForBroadcastThread dot="+dot.dotType );
//						dot = null;
//						break;
//					}

					if(dot != null){
						broadcastDot(dot);
					}
				}
				
				try{
					synchronized (mDotQueueForBroadcast) {
						mDotQueueForBroadcast.wait();
					}
				} catch (InterruptedException e){
//					e.printStackTrace();
					NLog.d( "DotConsumerThread Interrupted!!" + e);
				}
			}
		}
	}
}
