package kr.neolab.samplecode;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import kr.neolab.samplecode.provider.DbOpenHelper;
import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.DotType;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.IMetadataCtrl;
import kr.neolab.sdk.metadata.IMetadataListener;
import kr.neolab.sdk.metadata.MetadataCtrl;
import kr.neolab.sdk.metadata.structure.Symbol;
import kr.neolab.sdk.pen.IMultiPenCtrl;
import kr.neolab.sdk.pen.IPenCtrl;
import kr.neolab.sdk.pen.MultiPenCtrl;
import kr.neolab.sdk.pen.PenCtrl;
import kr.neolab.sdk.pen.bluetooth.IConnectedThread;
import kr.neolab.sdk.pen.penmsg.IOfflineDataListener;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.PenMsg;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;
public class NeoSampleService extends Service {

    private IMultiPenCtrl mPenCtrl;
    private IPenCtrl mSinglePenCtrl;
    private Queue<Dot> mDotQueueForDB = null;
    private Queue<DotWithAddress> mDotQueueForBroadcast = null;
    private DotConsumerForDBThread mDBThread = null;
    private DotConsumerForBroadcastThread mBroadcastThread = null;

    private int curSectionId, curOwnerId, curBookcodeId, curPageNumber;

    private boolean ready = false;

    IMetadataCtrl metadataCtrl;

    private DbOpenHelper mDbOpenHelper;

    PenClientCtrl penClientCtrl = PenClientCtrl.getInstance(this);

    PenClientCtrl.ReqOfflineData reqOfflineData = penClientCtrl.new ReqOfflineData();

    @Override
    public void onCreate() {
        super.onCreate();

        // Set Listener at pen Ctrl
        mPenCtrl = MultiPenCtrl.getInstance();
        mPenCtrl.setDotListener(mPenReceiveDotListener);
//		if(mPenCtrl.getOffLineDataListener() != null)
//			mPenCtrl.setOffLineDataListener( null );
        mPenCtrl.setOffLineDataListener(mOfflineDataListener);
        mPenCtrl.setMetadataListener(mMetadataListener);

        mSinglePenCtrl = PenCtrl.getInstance();
        mSinglePenCtrl.setDotListener(mPenReceiveDotListener);
        mSinglePenCtrl.setOffLineDataListener(mOfflineDataListener);
        mSinglePenCtrl.setMetadataListener(mMetadataListener);


        // Set Listener at Multi pen Ctrl
//		MultiPenCtrl.getInstance().setDotListener( mPenReceiveDotListener );
//		MultiPenCtrl.getInstance().setOffLineDataListener( mOfflineDataListener );

        // metadata load
//		metadataCtrl = MetadataCtrl.getInstance();
//		metadataCtrl.loadFiles( Const.SAMPLE_FOLDER_PATH);

        // create Queue
        mDotQueueForDB = new ConcurrentLinkedQueue<Dot>();
        mDotQueueForBroadcast = new ConcurrentLinkedQueue<DotWithAddress>();

        mDBThread = new DotConsumerForDBThread(NeoSampleService.this);
        mDBThread.setDaemon(true);
        mDBThread.start();

        mBroadcastThread = new DotConsumerForBroadcastThread();
        mBroadcastThread.setDaemon(true);
        mBroadcastThread.start();

        mDbOpenHelper = new DbOpenHelper(this);
        mDbOpenHelper.open();
        mDbOpenHelper.create();

        NLog.d("Service Initialize complete");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NLog.d("onDestroy");

        if (mDBThread != null)
            mDBThread.interrupt();
        if (mBroadcastThread != null)
            mBroadcastThread.interrupt();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NLog.d("onStartCommand: " + startId);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        NLog.d("onBind - service binding");
        return null;
    }


    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        NLog.d("onRebind");
    }


    @Override
    public boolean onUnbind(Intent intent) {
        NLog.d("onUnbind");
        return super.onUnbind(intent);
    }

    private void enqueueDot(Dot dot) {
        mDotQueueForDB.offer(dot);

        synchronized (mDotQueueForDB) {
            mDotQueueForDB.notifyAll();
        }
    }

    private void enqueueDotForBroadcast(String macAddress, Dot dot) {

        mDotQueueForBroadcast.offer(new DotWithAddress(macAddress, dot));

        synchronized (mDotQueueForBroadcast) {
            mDotQueueForBroadcast.notifyAll();
        }
    }

    private void broadcastDot(String macAddress, Dot dot) {

        NLog.d("broadcastDot send: sectionId:" + dot.sectionId + " ownerId:" + dot.ownerId + " noteId:" + dot.noteId + " pageId:" + dot.pageId + " dotType:" + dot.dotType + ",X=" + dot.getX() + ",Y=" + dot.getY());

        Intent intent = new Intent(Const.Broadcast.ACTION_PEN_DOT);
        intent.putExtra(Const.Broadcast.PEN_ADDRESS, macAddress);
        intent.putExtra(Const.Broadcast.EXTRA_DOT, dot);
        sendBroadcast(intent);
    }

    private void sendBroadcastIfPageChanged(int sectionId, int ownerId, int bookcodeId, int pageNumber) {
        if (curSectionId != sectionId || curOwnerId != ownerId || curBookcodeId != bookcodeId) {
            curSectionId = sectionId;
            curOwnerId = ownerId;
            curBookcodeId = bookcodeId;

            curPageNumber = -1;
            curBookcodeId = bookcodeId;
        }

        if (curPageNumber != pageNumber) {
            curPageNumber = pageNumber;
            sendPageChangedBroadcast();
            ready = false;
            NLog.d("sendBroadcastIfPageChanged ready=" + ready);
        }
    }

    private IPenDotListener mPenReceiveDotListener = new IPenDotListener() {

        @Override
        public void onReceiveDot(String macAddress, Dot dot) {
            NLog.d("NeoSampleService onReceiveDot mac_address=" + macAddress + "dotType=" + dot.dotType + " ,pressure=" + dot.pressure + ",x=" + dot.getX() + ",y=" + dot.getY());
            sendBroadcastIfPageChanged(dot.sectionId, dot.ownerId, dot.noteId, dot.pageId);
            enqueueDot(dot);
            enqueueDotForBroadcast(macAddress, dot);

        }
    };

    private IOfflineDataListener mOfflineDataListener = new IOfflineDataListener() {
        @Override
        public void onReceiveOfflineStrokes(Object extra, String macAddress, Stroke[] strokes, int sectionId, int ownerId, int noteId, Symbol[] symbols) {

            if (strokes == null || strokes.length <= 0) {
                NLog.e("onReceiveOfflineStrokes strokes size =0!!");
                return;
            }

            // Filtering 0 size dot
            ArrayList<Stroke> newArrayList = new ArrayList<Stroke>();

            for (Stroke s : strokes) {
                if (s.size() > 0) {
                    NLog.d("onReceiveOfflineStrokes s sectionId=" + s.sectionId + " ownerId=" + s.ownerId + " noteId=" + s.noteId + " pageId=" + s.pageId + " s.size()=" + s.size());
                    s.color = Color.BLACK;
                    newArrayList.add(s);

                    // DB에 stroke 를 저장한다.
                    // DB Insert
                    mDbOpenHelper.open();
                    mDbOpenHelper.insertStroke(s);
                } else {
                    NLog.e("onReceiveOfflineStrokes Dot size =0!!");
                }
            }
            Intent i = new Intent(Const.Broadcast.ACTION_OFFLINE_STROKES);
            i.putExtra(Const.Broadcast.PEN_ADDRESS, macAddress);
            i.putExtra(Const.Broadcast.EXTRA_OFFLINE_STROKES, newArrayList.toArray(new Stroke[newArrayList.size()]));
            i.putExtra(Const.Broadcast.EXTRA_SECTION_ID, sectionId);
            i.putExtra(Const.Broadcast.EXTRA_OWNER_ID, ownerId);
            i.putExtra(Const.Broadcast.EXTRA_BOOKCODE_ID, noteId);
            getApplicationContext().sendBroadcast(i);
            if(PenClientCtrl.savedPages.size() != 0) {
                reqOfflineData.run();
            }
        }
    };

    // MetadataCtrl 에서 nproj 로딩, symbol 체크 시 callback
    private IMetadataListener mMetadataListener = new IMetadataListener() {
        @Override
        public void onSymbolDetected(Symbol[] symbols) {
            for (final Symbol s : symbols) {
                if (s != null) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Symbol Check!! Action=" + s.getAction() + ", Param=" + s.getParam(), Toast.LENGTH_LONG).show();
                        }
                    });

                }
            }
        }
    };


    private void sendPageChangedBroadcast() {

        NLog.i("Page Changed-> curSectionId:" + curSectionId + " curOwnerId:" + curOwnerId + " curBookcodeId:" + curBookcodeId + " curPageNumber:" + curPageNumber);

        Intent intent = new Intent(Const.Broadcast.ACTION_WRITE_PAGE_CHANGED);
        intent.putExtra(Const.Broadcast.EXTRA_SECTION_ID, curSectionId);
        intent.putExtra(Const.Broadcast.EXTRA_OWNER_ID, curOwnerId);
        intent.putExtra(Const.Broadcast.EXTRA_BOOKCODE_ID, curBookcodeId);
        intent.putExtra(Const.Broadcast.EXTRA_PAGE_NUMBER, curPageNumber);

        sendBroadcast(intent);
    }


    private final class DotConsumerForDBThread extends Thread {
        private int dotCount = 0;
        private int eraserDotCount = 0;

        private int currentSectionId = -1;
        private int currentOwnerId = -1;
        private int currentBookcodeId = -1;
        private int currentPageId = -1;
        private ArrayList<Dot> dotArray = new ArrayList<Dot>(100);


        public DotConsumerForDBThread(Context context) {
            super();
        }


        @Override
        public void run() {

            setName(this.getClass().getSimpleName());

            while (true) {

                while (!mDotQueueForDB.isEmpty()) {
                    Dot dot = null;

                    dot = mDotQueueForDB.poll();

                    if (dot != null)
                        insert(dot);
                }

                try {
                    synchronized (mDotQueueForDB) {
                        mDotQueueForDB.wait();
                    }
                } catch (InterruptedException e) {
                    NLog.d("DotConsumerThread Interrupted!!" + e);
                }
            }
        }

        public void insert(Dot dot) {
            checkNotebookAndPageChange(dot);

            if (DotType.isPenActionDown(dot.dotType)) {
                checkNotebookAndPageChange(dot);
                if (dot.penTipType == Stroke.PEN_TIP_TYPE_NORMAL) {
                    dotArray.add(dot);
                    dotCount++;
                }
            }
            // dot middle, adding to stroke
            else if (DotType.isPenActionMove(dot.dotType)) {

                // 스트로크 시작 dot 가 없는데 move dot 이 들어오면
                // 시작 dot 을 추가
                if (dot.penTipType == Stroke.PEN_TIP_TYPE_NORMAL) {
                    if (dotCount == 0) {
                        Dot startDot = new Dot(dot.x, dot.y, dot.pressure, DotType.PEN_ACTION_DOWN.getValue(), dot.timestamp, dot.sectionId, dot.ownerId, dot.noteId, dot.pageId, dot.color, dot.penTipType, dot.tiltX, dot.tiltY, dot.twist);
                        insert(startDot);
                    }
                    dotArray.add(dot);
                    dotCount++;
                }
            }
            // dot end, insert to db and then flush data
            else if (DotType.isPenActionUp(dot.dotType)) {

                if (dot.penTipType == Stroke.PEN_TIP_TYPE_NORMAL) {
                    dotArray.add(dot);
                    dotCount++;
                    insertStrokeDotsArray(dotArray, false);
                }
            }
        }

        private void checkNotebookAndPageChange(Dot dot) {
            boolean changed = false;
            if (currentSectionId != dot.sectionId || currentOwnerId != dot.ownerId || currentBookcodeId != dot.noteId || currentPageId != dot.pageId) {
                currentSectionId = dot.sectionId;
                currentOwnerId = dot.ownerId;
                currentBookcodeId = dot.noteId;
                currentPageId = dot.pageId;
                changed = true;
            }

            if (changed) {
                if (dotArray.size() > 0) {
                    Dot lastDot = dotArray.get(dotArray.size() - 1);
                    //makeUpDot
                    Dot upDot = new Dot(lastDot.x, lastDot.y, lastDot.pressure, DotType.PEN_ACTION_UP.getValue(), lastDot.timestamp, lastDot.sectionId, lastDot.ownerId, lastDot.noteId, lastDot.pageId, lastDot.color, lastDot.penTipType, lastDot.tiltX, lastDot.tiltY, lastDot.twist);
                    dotArray.add(upDot);
                    insertStrokeDotsArray(dotArray, false);
                }
            }
        }

        private void insertStrokeDotsArray(ArrayList<Dot> dotArray, boolean isEraser) {
            Stroke s = null;
            for (Dot d : dotArray) {
                if (s == null) {
                    s = new Stroke(d.sectionId, d.ownerId, d.noteId, d.pageId, Color.BLACK);
                    s.penTipType = d.penTipType;
                    if (s.penTipType == Stroke.PEN_TIP_TYPE_NORMAL)
                        s.thickness = 1;
                }
                s.add(d);
            }
            dotArray.clear();
            if (isEraser)
                eraserDotCount = 0;
            else
                dotCount = 0;

            // DB에 stroke 를 저장한다.
            // DB Insert
            mDbOpenHelper.open();
            mDbOpenHelper.insertStroke(s);
        }

    }

    private final class DotConsumerForBroadcastThread extends Thread {

        @Override
        public void run() {

            setName(this.getClass().getSimpleName());


            while (true) {

                while (!mDotQueueForBroadcast.isEmpty()) {
                    Dot dot = null;
                    DotWithAddress dotWithAddress = mDotQueueForBroadcast.poll();
                    dot = dotWithAddress.dot;

                    if (dot != null) {
                        broadcastDot(dotWithAddress.address, dot);
                    }
                }

                try {
                    synchronized (mDotQueueForBroadcast) {
                        mDotQueueForBroadcast.wait();
                    }
                } catch (InterruptedException e) {
                    NLog.d("DotConsumerThread Interrupted!!" + e);
                }
            }
        }
    }

    private class DotWithAddress {
        public Dot dot = null;
        public String address = null;

        public DotWithAddress(String address, Dot dot) {
            this.dot = dot;
            this.address = address;
        }
    }
}
