package kr.neolab.samplecode.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.Stroke;

public class DbOpenHelper
{
    public static final String DATABASE_NAME = "kr.neolab.samplecode.db";
    private static final int DATABASE_VERSION = 1;
    public static SQLiteDatabase mDB;
    private DatabaseHelper mDBHelper;
    public static final int DOT_DATA_BYTE_ALIGN = 16;
    private Context mCtx;

    private class DatabaseHelper extends SQLiteOpenHelper
    {

        public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db){
            db.execSQL(DataBases.CreateDB.CREATE_TABLE_STROKES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS "+DataBases.CreateDB.TABLE_STROKES);
            onCreate(db);
        }
    }

    public DbOpenHelper(Context context){
        this.mCtx = context;
    }

    public DbOpenHelper open() throws SQLException{
        mDBHelper = new DatabaseHelper(mCtx, DATABASE_NAME, null, DATABASE_VERSION);
        mDB = mDBHelper.getWritableDatabase();
        return this;
    }

    public void create(){
        mDBHelper.onCreate(mDB);
    }

    public void close(){
        mDB.close();
    }

    // Insert DB
    public long insertStroke( Stroke stroke ){

        ArrayList<Dot> dotArray = stroke.getDots();

        Uri strokeUri = null;
        final int N = dotArray.size();
        if(N == 0)
            return 0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream( N * DOT_DATA_BYTE_ALIGN);
        ContentValues values = new ContentValues();

        try
        {
            for(Dot d: dotArray)
            {
                baos.write(d.toByteArray(DOT_DATA_BYTE_ALIGN));
            }

            final Dot firstDot = dotArray.get(0);
            final Dot lastDot = dotArray.get(dotArray.size()-1);

            values.put(DataBases.CreateDB.SECTION_ID, stroke.sectionId);
            values.put(DataBases.CreateDB.OWNER_ID, stroke.ownerId);
            values.put(DataBases.CreateDB.NOTE_ID, stroke.noteId);
            values.put(DataBases.CreateDB.PAGE_ID, stroke.pageId);
            values.put(DataBases.CreateDB.COLOR, stroke.color);
            values.put(DataBases.CreateDB.THICKNESS, stroke.thickness);
            values.put(DataBases.CreateDB.TIME_START, stroke.timeStampStart);
            values.put(DataBases.CreateDB.TIME_END, stroke.timeStampEnd);
            values.put(DataBases.CreateDB.DOTS, baos.toByteArray());
            values.put(DataBases.CreateDB.DOT_COUNT, dotArray.size());


        } catch ( IOException e )
        {
            e.printStackTrace();
        }
        return mDB.insert(DataBases.CreateDB.TABLE_STROKES, null, values);
    }

    // Update DB
    public boolean updateStroke(long id, Stroke stroke){
        ArrayList<Dot> dotArray = stroke.getDots();

        Uri strokeUri = null;
        final int N = dotArray.size();
        if(N == 0)
            return false;

        ByteArrayOutputStream baos = new ByteArrayOutputStream( N * DOT_DATA_BYTE_ALIGN);
        ContentValues values = new ContentValues();

        try
        {
            for(Dot d: dotArray)
            {
                baos.write(d.toByteArray(DOT_DATA_BYTE_ALIGN));
            }

            final Dot firstDot = dotArray.get(0);
            final Dot lastDot = dotArray.get(dotArray.size()-1);

            values.put(DataBases.CreateDB.SECTION_ID, stroke.sectionId);
            values.put(DataBases.CreateDB.OWNER_ID, stroke.ownerId);
            values.put(DataBases.CreateDB.NOTE_ID, stroke.noteId);
            values.put(DataBases.CreateDB.PAGE_ID, stroke.pageId);
            values.put(DataBases.CreateDB.COLOR, stroke.color);
            values.put(DataBases.CreateDB.THICKNESS, stroke.thickness);
            values.put(DataBases.CreateDB.TIME_START, stroke.timeStampStart);
            values.put(DataBases.CreateDB.TIME_END, stroke.timeStampEnd);
            values.put(DataBases.CreateDB.DOTS, baos.toByteArray());
            values.put(DataBases.CreateDB.DOT_COUNT, dotArray.size());


        } catch ( IOException e )
        {
            e.printStackTrace();
        }

        return mDB.update(DataBases.CreateDB.TABLE_STROKES, values, "_id=" + id, null) > 0;
    }

    // Delete All
    public void deleteAllColumns() {
        mDB.delete(DataBases.CreateDB.TABLE_STROKES, null, null);
    }

    // Delete DB
    public boolean deleteColumn(long id){
        return mDB.delete(DataBases.CreateDB.TABLE_STROKES, "_id="+id, null) > 0;
    }
    // Select DB
    public Cursor selectColumns(){
        return mDB.query(DataBases.CreateDB.TABLE_STROKES, null, null, null, null, null, null);
    }

    // sort by column
    public Cursor sortColumn(String sort){
        Cursor c = mDB.rawQuery( "SELECT * FROM usertable ORDER BY " + sort + ";", null);
        return c;
    }
}
