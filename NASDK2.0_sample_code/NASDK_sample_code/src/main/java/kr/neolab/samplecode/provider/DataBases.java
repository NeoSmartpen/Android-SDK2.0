package kr.neolab.samplecode.provider;

import android.provider.BaseColumns;

public final class DataBases
{
//    public static String AUTHORITY = "kr.neolab.samplecode.database";
//    private static final String CONTENT_AUTHORITY_SLASH = "content://" + AUTHORITY + "/";

    public static final class CreateDB implements BaseColumns
    {
        public static final String _ID = "_id";
        public static final String FULL_ID = "strokes._id";

        public static final String SECTION_ID = "section_id";
        public static final String OWNER_ID = "owner_id";
        public static final String NOTE_ID = "note_id";
        public static final String PAGE_ID = "page_id";

        public static final String COLOR = "color";
        public static final String THICKNESS = "thickness";

        public static final String TIME_START = "time_start";
        public static final String TIME_END = "time_end";

        public static final String DOTS = "dots";

        public static final String DOT_COUNT = "dot_count";

        public static final String TABLE_STROKES = "strokes";
        public static final String CREATE_TABLE_STROKES = "create table if not exists "+TABLE_STROKES+"("
                +_ID+" integer primary key autoincrement, "
                +SECTION_ID+" integer default 0 , "
                +OWNER_ID+" integer default 0 , "
                +NOTE_ID+" integer default 0 , "
                +PAGE_ID+" integer default 0 ,"
                +COLOR+" integer default 0 ,"
                +THICKNESS+" integer default 0 ,"
                +TIME_START+" integer default 0 ,"
                +TIME_END+" integer default 0 ,"
                +DOTS+" blob ,"
                +DOT_COUNT+" integer not null );";
    }
}
