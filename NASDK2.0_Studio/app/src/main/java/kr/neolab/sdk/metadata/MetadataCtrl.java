package kr.neolab.sdk.metadata;

import android.graphics.PointF;
import android.util.SparseArray;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.structure.Page;
import kr.neolab.sdk.metadata.structure.Segment;
import kr.neolab.sdk.metadata.structure.Symbol;
import kr.neolab.sdk.util.NLog;

/**
 * The type Metadata ctrl.
 */
public class MetadataCtrl implements IMetadataCtrl
{
    private static MetadataCtrl myInstance = null;

    private HashMap<String, HashMap<String, Segment>> segmentTable;
    private HashMap<String, ArrayList<Symbol>> symbolTable;
    private HashMap<String, Symbol> cropAreaTable;

    private HashMap<String, Page> pageTable;
    private HashMap<String, PointF> offsetTable;
    private SparseArray<Book> bookTable;

    /**
     * The constant PIXEL_TO_DOT_SCALE.
     */
    public static float PIXEL_TO_DOT_SCALE = 600f / 72f / 56f;

    private enum PaperValueType
    {
        /**
         * Width paper value type.
         */
        WIDTH, /**
     * Height paper value type.
     */
    HEIGHT, /**
     * Width include margin paper value type.
     */
    WIDTH_INCLUDE_MARGIN, /**
     * Height include margin paper value type.
     */
    HEIGHT_INCLUDE_MARGIN, /**
     * Margin left paper value type.
     */
    MARGIN_LEFT, /**
     * Margin top paper value type.
     */
    MARGIN_TOP, /**
     * Margin right paper value type.
     */
    MARGIN_RIGHT, /**
     * Margin bottom paper value type.
     */
    MARGIN_BOTTOM, /**
     * Offset left paper value type.
     */
    OFFSET_LEFT, /**
     * Offset top paper value type.
     */
    OFFSET_TOP,
    }

    private MetadataCtrl()
    {
        segmentTable = new HashMap<String, HashMap<String, Segment>>();
        symbolTable = new HashMap<String, ArrayList<Symbol>>();
        pageTable = new HashMap<String, Page>();
        bookTable = new SparseArray<Book>();
        cropAreaTable = new HashMap<String, Symbol>();
        offsetTable = new HashMap<String, PointF>();
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static synchronized MetadataCtrl getInstance()
    {
        if ( myInstance == null )
            myInstance = new MetadataCtrl();

        return myInstance;
    }

    @Override
    public void loadFile( File file ) throws XmlPullParserException, IOException, SAXException, ParserConfigurationException
    {
        String fileName = file.getName().toLowerCase( Locale.US );

        NLog.d( "[MetadataCtrl] load file : " + fileName );

        if ( !fileName.endsWith( ".nproj" ) )
        {
            return;
        }

        InputStream is = new FileInputStream( file );

        this.parseBySAX( is );
    }

    @Override
    public void loadFiles( String fileDirectoryPath )
    {
        NLog.d( "[MetadataCtrl] loadFiles : " + fileDirectoryPath );

        try
        {
            File f = new File( fileDirectoryPath );
            File[] fileNames = f.listFiles();

            for ( int i = 0; i < fileNames.length; i++ )
            {
                File file = fileNames[i];

                if ( file.isFile() )
                {
                    this.loadFile( file );
                }
            }
        }
        catch ( Exception e )
        {
            NLog.e( "[MetadataCtrl] can not load nproj file from " + fileDirectoryPath, e );
            e.printStackTrace();
        }
    }

    private String getQueryString( int noteId, int pageId )
    {
        return noteId + "_" + pageId;
    }

    @Override
    public String getTitle( int noteId )
    {
        Book result = bookTable.get( noteId );

        if ( result == null )
        {
            return null;
        }

        return result.title;
    }

    @Override
    public int getOwnerCode( int noteId )
    {
        Book result = bookTable.get( noteId );

        if ( result == null )
        {
            return -1;
        }

        return result.ownerCode;
    }

    @Override
    public int getSectionCode( int noteId )
    {
        Book result = bookTable.get( noteId );

        if ( result == null )
        {
            return -1;
        }

        return result.sectionCode;
    }

    @Override
    public int getKind( int noteId )
    {
        Book result = bookTable.get( noteId );

        if ( result == null )
        {
            return 0;
        }

        return result.kind;
    }

    @Override
    public int getStartPage ( int noteId )
    {
        Book result = bookTable.get( noteId );

        if ( result == null )
        {
            return 0;
        }

        return result.startPage;
    }

    @Override
    public ArrayList<Integer> getNoteTypeList( int kind )
    {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        for(int i = 0; i < bookTable.size();i++)
        {
            int key = bookTable.keyAt(i);
            Book book = bookTable.get(key);
            if(book.kind == kind)
                ret.add(key);
        }
        return ret;
    }


    @Override
    public String getExtraInfo( int noteId )
    {
        Book result = bookTable.get( noteId );

        if ( result == null )
        {
            return null;
        }

        return result.extra_info;
    }

    /**
     * Gets note offset left.
     *
     * @param noteId the note id
     * @return the note offset left
     */
    public float getNoteOffsetLeft( int noteId )
    {
        return getPaperValue( noteId, 0 , PaperValueType.OFFSET_LEFT );
    }

    /**
     * Gets note offset top.
     *
     * @param noteId the note id
     * @return the note offset top
     */
    public float getNoteOffsetTop( int noteId )
    {
        return getPaperValue( noteId, 0 , PaperValueType.OFFSET_TOP );
    }



    @Override
    public float getPageWidthWithMargin( int noteId, int pageId )
    {
        return getPaperValue( noteId, pageId, PaperValueType.WIDTH_INCLUDE_MARGIN );
//		Page result = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//
//		if ( result == null )
//		{
//			return 0;
//		}
//
//		return result.width;
    }

    @Override
    public float getPageHeightWithMargin( int noteId, int pageId )
    {
        return getPaperValue( noteId, pageId, PaperValueType.HEIGHT_INCLUDE_MARGIN );
//		Page result = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//
//		if ( result == null )
//		{
//			return 0;
//		}
//
//		return result.height;
    }

    @Override
    public float getPageWidth( int noteId, int pageId )
    {
        return getPaperValue( noteId, pageId, PaperValueType.WIDTH );
//
//		Page result = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//
//		if ( result == null )
//		{
//			return 0;
//		}
//
//		return result.width;
    }

    @Override
    public float getPageHeight( int noteId, int pageId )
    {
        return getPaperValue( noteId, pageId, PaperValueType.HEIGHT );
//		Page result = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//
//		if ( result == null )
//		{
//			return 0;
//		}
//
//		return result.height;
    }

    private float getPaperValue( int noteId, int pageId , PaperValueType type)
    {
        Book result = this.bookTable.get( noteId );
        if(result == null)
            return 0;
        if(type == PaperValueType.WIDTH_INCLUDE_MARGIN || type == PaperValueType.HEIGHT_INCLUDE_MARGIN)
        {
            Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
            if ( pageResult == null )
                return 0;
            if(result.nprojVersion < 2.31f)
            {
                if ( type == PaperValueType.WIDTH_INCLUDE_MARGIN )
                    return pageResult.width;
                else return pageResult.height;
            }
            else
            {
                if ( type == PaperValueType.WIDTH_INCLUDE_MARGIN )
                    return pageResult.width + pageResult.margin_left + pageResult.margin_right;
                else return pageResult.height + pageResult.margin_top + pageResult.margin_bottom;

            }
        }
        else if(type == PaperValueType.OFFSET_LEFT || type == PaperValueType.OFFSET_TOP)
        {
            PointF p = offsetTable.get( ""+noteId );
            if(p == null)
                return 0;
            if ( type == PaperValueType.OFFSET_LEFT )
                return p.x;
            else return p.y;

        }
        else
        {
            if(result.nprojVersion < 2.31f)
            {
                Symbol symbol = cropAreaTable.get(""+noteId+"_"+pageId);
                if(symbol != null)
                {
                    if(type == PaperValueType.WIDTH)
                        return symbol.getWidth();
                    else if(type == PaperValueType.HEIGHT)
                        return symbol.getHeight();
                    else if(type == PaperValueType.MARGIN_LEFT)
                        return symbol.getX();
                    else if(type == PaperValueType.MARGIN_TOP)
                        return symbol.getY();
                    else if(type == PaperValueType.MARGIN_RIGHT)
                    {
                        Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
                        if ( pageResult == null )
                            return 0;
                        else
                            return pageResult.width - symbol.right;
                    }
                    else if(type == PaperValueType.MARGIN_BOTTOM)
                    {
                        Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
                        if ( pageResult == null )
                            return 0;
                        else
                            return pageResult.height - symbol.bottom;
                    }
                }

                Symbol[] symbols = findApplicableSymbols( noteId , pageId );

                if ( symbols == null )
                {
                    Symbol[] symbols2 = findApplicableSymbols( noteId , 1 );
                    if(symbols2 == null)
                        return 0;
                    else
                    {
                        for(Symbol s: symbols2)
                        {
                            if(s.getParam().equals( "crop_area_common" ))
                            {
                                cropAreaTable.put(""+noteId+"_"+pageId,s  );

                                if(type == PaperValueType.WIDTH)
                                    return s.getWidth();
                                else if(type == PaperValueType.HEIGHT)
                                    return s.getHeight();
                                else if(type == PaperValueType.MARGIN_LEFT)
                                    return s.getX();
                                else if(type == PaperValueType.MARGIN_TOP)
                                    return s.getY();
                                else if(type == PaperValueType.MARGIN_RIGHT)
                                {
                                    Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
                                    if ( pageResult == null )
                                        return 0;
                                    else
                                        return pageResult.width - s.right;
                                }
                                else if(type == PaperValueType.MARGIN_BOTTOM)
                                {
                                    Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
                                    if ( pageResult == null )
                                        return 0;
                                    else
                                        return pageResult.height - s.bottom;
                                }
                            }
                        }

                    }
                }
                else
                {
                    for(Symbol s: symbols)
                    {
                        if(s.getParam().equals( "crop_area" ))
                        {
                            cropAreaTable.put(""+noteId+"_"+pageId,s  );
                            if(type == PaperValueType.WIDTH)
                                return s.getWidth();
                            else if(type == PaperValueType.HEIGHT)
                                return s.getHeight();
                            else if(type == PaperValueType.MARGIN_LEFT)
                                return s.getX();
                            else if(type == PaperValueType.MARGIN_TOP)
                                return s.getY();
                            else if(type == PaperValueType.MARGIN_RIGHT)
                            {
                                Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
                                if ( pageResult == null )
                                    return 0;
                                else
                                    return pageResult.width - s.right;
                            }
                            else if(type == PaperValueType.MARGIN_BOTTOM)
                            {
                                Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
                                if ( pageResult == null )
                                    return 0;
                                else
                                    return pageResult.height - s.bottom;
                            }
                        }
                    }
                    Symbol[] symbols2 = findApplicableSymbols( noteId , 1 );
                    if(symbols2 == null)
                        return 0;
                    else
                    {
                        for(Symbol s: symbols2)
                        {
                            if(s.getParam().equals( "crop_area_common" ))
                            {
                                cropAreaTable.put(""+noteId+"_"+pageId,s  );
                                if(type == PaperValueType.WIDTH)
                                    return s.getWidth();
                                else if(type == PaperValueType.HEIGHT)
                                    return s.getHeight();
                                else if(type == PaperValueType.MARGIN_LEFT)
                                    return s.getX();
                                else if(type == PaperValueType.MARGIN_TOP)
                                    return s.getY();
                                else if(type == PaperValueType.MARGIN_RIGHT)
                                {
                                    Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
                                    if ( pageResult == null )
                                        return 0;
                                    else
                                        return pageResult.width - s.right;
                                }
                                else if(type == PaperValueType.MARGIN_BOTTOM)
                                {
                                    Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
                                    if ( pageResult == null )
                                        return 0;
                                    else
                                        return pageResult.height - s.bottom;
                                }
                            }
                        }

                    }
                }
            }
            else
            {
                Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );

                if ( pageResult == null )
                {
                    return 0;
                }
                else
                {
                    if(type == PaperValueType.WIDTH)
                        return pageResult.width;
                    else if(type == PaperValueType.HEIGHT)
                        return pageResult.height;
                    else if(type == PaperValueType.MARGIN_LEFT)
                        return pageResult.margin_left;
                    else if(type == PaperValueType.MARGIN_TOP)
                        return pageResult.margin_top;
                    else if(type == PaperValueType.MARGIN_RIGHT)
                    {
                        return pageResult.margin_right;
                    }
                    else if(type == PaperValueType.MARGIN_BOTTOM)
                    {
                        return pageResult.margin_bottom;
                    }
                }
            }
            return 0;
        }
    }


    @Override
    public float getPageMarginLeft ( int noteId, int pageId )
    {
        return getPaperValue( noteId, pageId, PaperValueType.MARGIN_LEFT );
//		Book result = this.bookTable.get( noteId );
//		if(result.nprojVersion < 2.31f)
//		{
//			Symbol symbol = cropAreaTable.get(""+noteId+"_"+pageId);
//			if(symbol != null)
//				return symbol.getX();
//
//			Symbol[] symbols = findApplicableSymbols( noteId , pageId );
//
//			if ( symbols == null )
//			{
//				Symbol[] symbols2 = findApplicableSymbols( noteId , 1 );
//				if(symbols2 == null)
//					return 0;
//				else
//				{
//					for(Symbol s: symbols2)
//					{
//						if(s.getParam().equals( "crop_area_common" ))
//						{
//							cropAreaTable.put(""+noteId+"_"+pageId,s  );
//							return s.getX();
//						}
//					}
//
//				}
//			}
//			else
//			{
//				for(Symbol s: symbols)
//				{
//					if(s.getParam().equals( "crop_area" ))
//					{
//						cropAreaTable.put(""+noteId+"_"+pageId,s  );
//						return s.getX();
//					}
//				}
//				Symbol[] symbols2 = findApplicableSymbols( noteId , 1 );
//				if(symbols2 == null)
//					return 0;
//				else
//				{
//					for(Symbol s: symbols2)
//					{
//						if(s.getParam().equals( "crop_area_common" ))
//						{
//							cropAreaTable.put(""+noteId+"_"+pageId,s  );
//							return s.getX();
//						}
//					}
//
//				}
//
//			}
//		}
//		else
//		{
//			Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//
//			if ( pageResult == null )
//			{
//				return 0;
//			}
//			else
//			{
//				return pageResult.margin_left;
//			}
//		}
//		return 0;
    }

    @Override
    public float getPageMarginTop ( int noteId, int pageId )
    {
        return getPaperValue( noteId, pageId, PaperValueType.MARGIN_TOP );
//		Book result = this.bookTable.get( noteId );
//		if(result.nprojVersion < 2.31f)
//		{
//			Symbol symbol = cropAreaTable.get(""+noteId+"_"+pageId);
//			if(symbol != null)
//				return symbol.getY();
//
//			Symbol[] symbols = findApplicableSymbols( noteId , pageId );
//
//			if ( symbols == null )
//			{
//				Symbol[] symbols2 = findApplicableSymbols( noteId , 1 );
//				if(symbols2 == null)
//					return 0;
//				else
//				{
//					for(Symbol s: symbols2)
//					{
//						if(s.getParam().equals( "crop_area_common" ))
//						{
//							cropAreaTable.put(""+noteId+"_"+pageId,s  );
//							return s.getY();
//						}
//					}
//
//				}
//			}
//			else
//			{
//				for(Symbol s: symbols)
//				{
//					if(s.getParam().equals( "crop_area" ))
//					{
//						cropAreaTable.put(""+noteId+"_"+pageId,s  );
//						return s.getY();
//					}
//				}
//			}
//		}
//		else
//		{
//			Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//
//			if ( pageResult == null )
//			{
//				return 0;
//			}
//			else
//			{
//				return pageResult.margin_top;
//			}
//		}
//		return 0;
    }

    @Override
    public float getPageMarginRight ( int noteId, int pageId )
    {
        return getPaperValue( noteId, pageId, PaperValueType.MARGIN_RIGHT );
//		Book result = this.bookTable.get( noteId );
//		if(result.nprojVersion < 2.31f)
//		{
//			Symbol symbol = cropAreaTable.get(""+noteId+"_"+pageId);
//			if(symbol != null)
//			{
//				Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//				if ( pageResult == null )
//				{
//					return 0;
//				}
//				else
//				{
//					return pageResult.width - symbol.right;
//				}
//			}
//
//			Symbol[] symbols = findApplicableSymbols( noteId , pageId );
//
//			if ( symbols == null )
//			{
//				Symbol[] symbols2 = findApplicableSymbols( noteId , 1 );
//				if(symbols2 == null)
//					return 0;
//				else
//				{
//					for(Symbol s: symbols2)
//					{
//						if(s.getParam().equals( "crop_area_common" ))
//						{
//							cropAreaTable.put(""+noteId+"_"+pageId,s  );
//							Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//							if ( pageResult == null )
//							{
//								return 0;
//							}
//							else
//							{
//								return pageResult.width - symbol.right;
//							}
//						}
//					}
//
//				}
//			}
//			else
//			{
//				for(Symbol s: symbols)
//				{
//					if(s.getParam().equals( "crop_area" ))
//					{
//						cropAreaTable.put(""+noteId+"_"+pageId,s  );
//						Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//						if ( pageResult == null )
//						{
//							return 0;
//						}
//						else
//						{
//							return pageResult.width - symbol.right;
//						}
//					}
//				}
//				Symbol[] symbols2 = findApplicableSymbols( noteId , 1 );
//				if(symbols2 == null)
//					return 0;
//				else
//				{
//					for(Symbol s: symbols2)
//					{
//						if(s.getParam().equals( "crop_area_common" ))
//						{
//							cropAreaTable.put(""+noteId+"_"+pageId,s  );
//							Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//							if ( pageResult == null )
//							{
//								return 0;
//							}
//							else
//							{
//								return pageResult.width - symbol.right;
//							}
//						}
//					}
//
//				}
//
//			}
//		}
//		else
//		{
//			Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//
//			if ( pageResult == null )
//			{
//				return 0;
//			}
//			else
//			{
//				return pageResult.margin_right;
//			}
//		}
//		return 0;
    }

    @Override
    public float getPageMarginBottom ( int noteId, int pageId )
    {
        return getPaperValue( noteId, pageId, PaperValueType.MARGIN_BOTTOM );
//		Book result = this.bookTable.get( noteId );
//		if(result.nprojVersion < 2.31f)
//		{
//			Symbol symbol = cropAreaTable.get(""+noteId+"_"+pageId);
//			if(symbol != null)
//			{
//				Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//				if ( pageResult == null )
//				{
//					return 0;
//				}
//				else
//				{
//					return pageResult.height - symbol.bottom;
//				}
//			}
//
//			Symbol[] symbols = findApplicableSymbols( noteId , pageId );
//
//			if ( symbols == null )
//			{
//				Symbol[] symbols2 = findApplicableSymbols( noteId , 1 );
//				if(symbols2 == null)
//					return 0;
//				else
//				{
//					for(Symbol s: symbols2)
//					{
//						if(s.getParam().equals( "crop_area_common" ))
//						{
//							cropAreaTable.put(""+noteId+"_"+pageId,s  );
//							Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//							if ( pageResult == null )
//							{
//								return 0;
//							}
//							else
//							{
//								return pageResult.height - symbol.bottom;
//							}
//						}
//					}
//
//				}
//			}
//			else
//			{
//				for(Symbol s: symbols)
//				{
//					if(s.getParam().equals( "crop_area" ))
//					{
//						cropAreaTable.put(""+noteId+"_"+pageId,s  );
//						Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//						if ( pageResult == null )
//						{
//							return 0;
//						}
//						else
//						{
//							return pageResult.height - symbol.bottom;
//						}
//					}
//				}
//				Symbol[] symbols2 = findApplicableSymbols( noteId , 1 );
//				if(symbols2 == null)
//					return 0;
//				else
//				{
//					for(Symbol s: symbols2)
//					{
//						if(s.getParam().equals( "crop_area_common" ))
//						{
//							cropAreaTable.put(""+noteId+"_"+pageId,s  );
//							Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//							if ( pageResult == null )
//							{
//								return 0;
//							}
//							else
//							{
//								return pageResult.height - symbol.bottom;
//							}
//						}
//					}
//
//				}
//
//			}
//		}
//		else
//		{
//			Page pageResult = this.pageTable.get( this.getQueryString( noteId, pageId ) );
//
//			if ( pageResult == null )
//			{
//				return 0;
//			}
//			else
//			{
//				return pageResult.margin_bottom;
//			}
//		}
//		return 0;
    }

    @Override
    public int getTotalPages( int noteId )
    {
        Book result = bookTable.get( noteId );

        if ( result == null )
        {
            return 0;
        }

        return result.totalPage;
    }

    private void put( int noteId, int pageId, Symbol event )
    {
        ArrayList<Symbol> result = this.symbolTable.get( this.getQueryString( noteId, pageId ) );

        if ( result == null )
        {
            ArrayList<Symbol> newEvents = new ArrayList<Symbol>();
            newEvents.add( event );

            this.symbolTable.put( this.getQueryString( noteId, pageId ), newEvents );
        }
        else
        {
            result.add( event );
        }
    }

    private void put( int noteId, int pageId, Page page )
    {
        this.pageTable.put( this.getQueryString( noteId, pageId ), page );
    }

    @Override
    public Symbol[] findApplicableSymbols( int noteId, int pageId )
    {
        ArrayList<Symbol> candidates = this.symbolTable.get( getQueryString( noteId, pageId ) );

        if ( candidates == null || candidates.size() <= 0 )
        {
            return null;
        }

        ArrayList<Symbol> result = new ArrayList<Symbol>();

        for ( Symbol e : candidates )
        {
            result.add( e );
        }

        if ( result.size() > 0 )
        {
            return result.toArray( new Symbol[1] );
        }
        else
        {
            return null;
        }
    }

	@Override
	public Symbol findApplicableSymbol( String id)
	{
		Symbol[] symbols = getSymbols();
		if(symbols == null)
			return null;
		else
		{
			for(Symbol s : symbols)
			{
				if(s.getId().equals( id ))
				{
					return s;
				}
			}
		}
		return null;
	}


    @Override
    public Symbol[] findApplicableSymbols( Stroke nstr )
    {
        ArrayList<Symbol> candidates = this.symbolTable.get( getQueryString( nstr.noteId, nstr.pageId ) );

        if ( candidates == null || candidates.size() <= 0 )
        {
            return null;
        }

        ArrayList<Symbol> result = new ArrayList<Symbol>();

        for ( Symbol e : candidates )
        {
            for ( int i = 0; i < nstr.size(); i++ )
            {
                Dot pf = nstr.get( i );

                if ( e.contains( pf.x, pf.y ) )
                {
                    result.add( e );
                    break;
                }
            }
        }

        if ( result.size() > 0 )
        {
            return result.toArray( new Symbol[1] );
        }
        else
        {
            return null;
        }
    }

    @Override
    public Symbol[] findApplicableSymbols( int noteId, int pageId, float x, float y )
    {
        ArrayList<Symbol> candidates = this.symbolTable.get( getQueryString( noteId, pageId ) );

        if ( candidates == null || candidates.size() <= 0 )
        {
            return null;
        }

        ArrayList<Symbol> result = new ArrayList<Symbol>();

        for ( Symbol e : candidates )
        {
            if ( pageId == e.pageId && e.contains( x, y ) )
            {
                result.add( e );
            }
        }

        if ( result.size() > 0 )
        {
            return result.toArray( new Symbol[1] );
        }
        else
        {
            return null;
        }
    }

    @Override
    public Symbol[] getSymbols()
    {
        ArrayList<Symbol> result = new ArrayList<Symbol>();

        Set<String> keys = this.symbolTable.keySet();

        for ( String key : keys )
        {
            ArrayList<Symbol> candidates = this.symbolTable.get( key );

            if ( candidates != null && candidates.size() > 0 )
            {
                result.addAll( candidates );
            }
        }

        if ( result.size() > 0 )
        {
            return result.toArray( new Symbol[1] );
        }
        else
        {
            return null;
        }
    }

    @Override
    public Segment[] getSegments ( int sectionId, int ownerId, int noteId )
    {
        HashMap<String, Segment> map = segmentTable.get( ""+sectionId+"_"+ownerId+"_"+noteId );

        Iterator<String> it = map.keySet().iterator();

        ArrayList<Segment> list = new ArrayList<Segment>();
//		Segment[] retArr = new Segment[it.]
        while ( it.hasNext() )
        {
            Segment segment = map.get( it.next() );

            if ( segment != null )
            {
                list.add( segment );
            }
        }
        if(list.size() == 0)
            return null;
        else
            return list.toArray( new Segment[list.size()] );
    }

    private int noteId = 0, ownerCode = 0, sectionCode = 0,totalPage = 0, kind = 0, startPage = 0;
    private String bookTitle = "", tag = "", extra_info = "";
    private boolean isSymbol = false;
    private Symbol symbol = null;
    private LinkedHashMap<String, Symbol> lnkTbl = new LinkedHashMap<String, Symbol>();
    private float nprojVersion = 2.2f;

    @Override
    synchronized public void parseBySAX( InputStream istream ) throws IOException, SAXException, ParserConfigurationException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        SAXParser parser = factory.newSAXParser();

        XMLReader reader = parser.getXMLReader();

        lnkTbl.clear();

        reader.setContentHandler( new DefaultHandler()
        {
            @Override
            public void startElement( String uri, String localName, String qName, Attributes atts ) throws SAXException
            {
                tag = localName;

                if ( tag.equals( "nproj" ) )
                {
                    nprojVersion = Float.parseFloat(atts.getValue( "version" ));
                }
                else if(tag.equals( "offset" ))
                {
                    PointF p = new PointF( );
                    try
                    {
                        p.x = Float.parseFloat( atts.getValue( "left" ) );
                        p.y = Float.parseFloat( atts.getValue( "top" ) );
                    }catch ( Exception e )
                    {
                    }
                    offsetTable.put( ""+noteId, p );

                }else
                {



                    // 2.2 ~ 2.3
                    if(nprojVersion < 2.31f)
                    {
                        if ( tag.equals( "pages" ) )
                        {
                            totalPage = Integer.parseInt( atts.getValue( "count" ) );
                        }
                        else if ( tag.equals( "page_item" ) )
                        {
                            Page page = new Page();

                            page.noteId = noteId;
                            page.pageId = Integer.parseInt( atts.getValue( "number" ) ) + 1;
                            page.angle = Integer.parseInt( atts.getValue( "rotate_angle" ) );
                            page.width = Float.parseFloat( atts.getValue( "x2" ) ) * PIXEL_TO_DOT_SCALE;
                            page.height = Float.parseFloat( atts.getValue( "y2" ) ) * PIXEL_TO_DOT_SCALE;

                            put( noteId, page.pageId, page );
                        }
                        else if ( tag.equals( "symbol" ) )
                        {
                            isSymbol = true;

                            int pageId = Integer.parseInt( atts.getValue( "page" ) ) + 1;
                            float x = Float.parseFloat( atts.getValue( "x" ) ) * PIXEL_TO_DOT_SCALE;
                            float y = Float.parseFloat( atts.getValue( "y" ) ) * PIXEL_TO_DOT_SCALE;
                            float width = Float.parseFloat( atts.getValue( "width" ) ) * PIXEL_TO_DOT_SCALE;
                            float height = Float.parseFloat( atts.getValue( "height" ) ) * PIXEL_TO_DOT_SCALE;
 
							symbol = new Symbol( noteId, pageId, "", "", "", x, y, x + width, y + height );
						}
						else if ( isSymbol && tag.equals( "command" ) )
						{
							symbol.action = atts.getValue( "action" );
							symbol.param = atts.getValue( "param" );
						}
						else if ( isSymbol && tag.equals( "matching_symbols" ) )
						{
							symbol.previousId = atts.getValue( "previous" );
							symbol.nextId = atts.getValue( "next" );
						}
					}
					else
					{
						if ( tag.equals( "segment_info" ) )
						{
							String subCode = atts.getValue( "sub_code" );
							int totalPageSize = Integer.parseInt( atts.getValue( "total_size" ) );
							int segmentPageSize = Integer.parseInt( atts.getValue( "size" ) );
							int segmentNumber = Integer.parseInt( atts.getValue( "current_sequence" ) );
							int segmentStartPage = Integer.parseInt( atts.getValue( "ncode_start_page" ) );
							int segmentEndPage = Integer.parseInt( atts.getValue( "ncode_end_page" ) );
							Segment segment = new Segment(sectionCode, ownerCode, noteId, subCode,segmentNumber, segmentStartPage, segmentEndPage, totalPageSize, segmentPageSize );
							if(segmentTable.get( ""+sectionCode+"_"+ownerCode+"_"+noteId ) == null)
							{
								HashMap<String, Segment> map = new HashMap<String, Segment>();
								map.put( ""+segmentNumber, segment );
								segmentTable.put( ""+sectionCode+"_"+ownerCode+"_"+noteId, map );
							}
							else
							{
								segmentTable.get( ""+sectionCode+"_"+ownerCode+"_"+noteId ).put(""+segmentNumber, segment );
							}
						}
						else if ( tag.equals( "pages" ) )
						{
							totalPage = Integer.parseInt( atts.getValue( "count" ) );
						}
						else if ( tag.equals( "page_item" ) )
						{
							float x1 = Float.parseFloat( atts.getValue( "x1" ) ) * PIXEL_TO_DOT_SCALE;
							float x2 = Float.parseFloat( atts.getValue( "x2" ) ) * PIXEL_TO_DOT_SCALE;
							float y1 = Float.parseFloat( atts.getValue( "y1" ) ) * PIXEL_TO_DOT_SCALE;
							float y2 = Float.parseFloat( atts.getValue( "y2" ) ) * PIXEL_TO_DOT_SCALE;

                            String crop = atts.getValue( "crop_margin" );
                            String[] crops = crop.split( "," );
                            float margin_left = Float.parseFloat( crops[0] ) * PIXEL_TO_DOT_SCALE;
                            float margin_right = Float.parseFloat( crops[1] ) * PIXEL_TO_DOT_SCALE;
                            float margin_top = Float.parseFloat( crops[2] ) * PIXEL_TO_DOT_SCALE;
                            float margin_bottom = Float.parseFloat( crops[3] ) * PIXEL_TO_DOT_SCALE;
                            Page page = new Page();
                            page.noteId = noteId;
                            page.pageId = Integer.parseInt( atts.getValue( "number" ) ) + 1;
                            page.angle = Integer.parseInt( atts.getValue( "rotate_angle" ) );
                            page.width = x2 - x1 - margin_left - margin_right;
                            page.height = y2 - y1 - margin_top - margin_bottom;
                            page.margin_left = margin_left;
                            page.margin_top = margin_top;
                            page.margin_right = margin_right;
                            page.margin_bottom = margin_bottom;

                            put( noteId, page.pageId, page );
                        }
                        else if ( tag.equals( "symbol" ) )
                        {
                            isSymbol = true;

                            int pageId = Integer.parseInt( atts.getValue( "page" ) ) + 1;
                            float x = Float.parseFloat( atts.getValue( "x" ) ) * PIXEL_TO_DOT_SCALE;
                            float y = Float.parseFloat( atts.getValue( "y" ) ) * PIXEL_TO_DOT_SCALE;
                            float width = Float.parseFloat( atts.getValue( "width" ) ) * PIXEL_TO_DOT_SCALE;
                            float height = Float.parseFloat( atts.getValue( "height" ) ) * PIXEL_TO_DOT_SCALE;

							symbol = new Symbol( noteId, pageId, "", "", "", x, y, x + width, y + height );
						}
						else if ( isSymbol && tag.equals( "command" ) )
						{
							symbol.action = atts.getValue( "action" );
							symbol.param = atts.getValue( "param" );
						}
						else if ( isSymbol && tag.equals( "matching_symbols" ) )
						{
							symbol.previousId = atts.getValue( "previous" );
							symbol.nextId = atts.getValue( "next" );
						}

                    }


                }
            }

            @Override
            public void endElement( String uri, String localName, String qName ) throws SAXException
            {
                if ( localName.equals( "symbol" ) )
                {
                    isSymbol = false;

                    lnkTbl.put( symbol.id, symbol );

                    put( noteId, symbol.pageId, symbol );

                    symbol = null;
                }
            }

            @Override
            public void characters( char[] ch, int start, int length ) throws SAXException
            {
                if ( length <= 0 || tag == null )
                {
                    return;
                }

                if ( tag.equals( "owner" ) )
                {
                    ownerCode = Integer.parseInt( charsToString( ch, 0, length ) );
                    tag = null;
                }
                else if ( tag.equals( "section" ) )
                {
                    sectionCode = Integer.parseInt( charsToString( ch, 0, length ) );
                    tag = null;
                }
                else if ( tag.equals( "code" ) )
                {
                    noteId = Integer.parseInt( charsToString( ch, 0, length ) );
                    tag = null;
                }
                else if ( tag.equals( "kind" ) )
                {
                    kind = Integer.parseInt( charsToString( ch, 0, length ) );
                    tag = null;
                }
                else if ( tag.equals( "title" ) )
                {
                    bookTitle = charsToString( ch, 0, length );
                    tag = null;
                }
                else if ( tag.equals( "extra_info" ) )
                {
                    extra_info = charsToString( ch, 0, length );
                    tag = null;
                }
                else if ( tag.equals( "start_page" ) )
                {
                    startPage = Integer.parseInt( charsToString( ch, 0, length ) );
                    tag = null;
                }

                if ( isSymbol )
                {
                    if ( tag.equals( "id" ) )
                    {
                        symbol.id = charsToString( ch, 0, length );
                        tag = null;
                    }
                    else if ( tag.equals( "name" ) )
                    {
                        symbol.name = charsToString( ch, 0, length );
                        tag = null;
                    }
                }
            }

        } );

        reader.parse( new InputSource( istream ) );

        if(nprojVersion < 2.31f)
        {
            Segment segment = new Segment(sectionCode, ownerCode, noteId, "",0, startPage, startPage + totalPage - 1, totalPage, totalPage );
            HashMap<String, Segment> map = new HashMap<String, Segment>();
            map.put( ""+0 , segment );
            segmentTable.put( ""+sectionCode+"_"+ownerCode+"_"+noteId, map );
        }
        Book nBook = new Book( noteId,ownerCode, sectionCode, totalPage, bookTitle, kind, startPage, extra_info ,nprojVersion);

        bookTable.put( noteId, nBook );

        Set<String> ids = lnkTbl.keySet();

        Iterator<String> it = ids.iterator();

        while ( it.hasNext() )
        {
            Symbol sym = lnkTbl.get( it.next() );

            if ( sym.previousId != null )
            {
                sym.previous = lnkTbl.get( sym.previousId );
            }

            if ( sym.nextId != null )
            {
                sym.next = lnkTbl.get( sym.nextId );
            }
        }
    }

    @Override
    public void print()
    {
        int books = bookTable.size();

        for ( int i = 0; i < books; i++ )
        {
            Book book = bookTable.get( bookTable.keyAt( i ) );
            NLog.d( "[MetadataCtrl] book : " + book.toString() );
        }

        Symbol[] syms = getSymbols();

        for ( Symbol sym : syms )
        {
            NLog.d( "[MetadataCtrl] " + sym.param + "/" + sym.next + "/" + sym.previous );
        }
    }

    private String charsToString( char[] ch, int start, int length )
    {
        String value = null;

        if ( ch.length > 0 )
        {
            StringBuilder item = new StringBuilder();
            item.append( ch, start, length );

            value = item.toString().trim();

            if ( value.equals( "" ) )
            {
                value = null;
            }

            item = null;
        }

        return value;
    }

    @Override
    public void parseByXmlPullParser( InputStream istream ) throws XmlPullParserException, IOException
    {
        XmlPullParser parser = Xml.newPullParser();

        String tag = null, nameSpace = null;

        boolean isSymbol = false;

        Symbol symbol = null;

        int noteId = 0, ownerCode = 0, sectionCode = 0, totalPage = 0, kind = 0, startPage = 0;
        String bookTitle = "", extra_info = "";
        float nprojVersion = 2.2f;

        LinkedHashMap<String, Symbol> lnkTbl = new LinkedHashMap<String, Symbol>();

        parser.setInput( new InputStreamReader( istream ) );

        for ( int eventType = parser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next() )
        {
            switch ( eventType )
            {
                case XmlPullParser.START_TAG:
                    tag = parser.getName();

                    if ( tag.equals( "nproj" ) )
                    {
                        nprojVersion = Float.parseFloat(parser.getAttributeValue( nameSpace,"version" ));
                    }
                    else
                    {
                        //?????? 2.2 ~ 2.3
                        if(nprojVersion < 2.31f)
                        {
                            if ( tag.equals( "pages" ) )
                            {
                                totalPage = Integer.parseInt(parser.getAttributeValue( nameSpace, "count" ) );
                            }
                            else if ( tag.equals( "page_item" ) )
                            {
                                Page page = new Page();

                                page.noteId = noteId;
                                page.pageId = Integer.parseInt( parser.getAttributeValue( nameSpace, "number" ) ) + 1;
                                page.angle = Integer.parseInt( parser.getAttributeValue( nameSpace, "rotate_angle" ) );
                                page.width = Float.parseFloat( parser.getAttributeValue( nameSpace, "x2" ) ) * PIXEL_TO_DOT_SCALE;
                                page.height = Float.parseFloat( parser.getAttributeValue( nameSpace, "y2" ) ) * PIXEL_TO_DOT_SCALE;

                                // ???? ?????????????
                                put( noteId, page.pageId, page );
                            }
                            else if ( tag.equals( "symbol" ) )
                            {
                                isSymbol = true;

                                int pageId = Integer.parseInt( parser.getAttributeValue( nameSpace, "page" ) ) + 1;
                                float x = Float.parseFloat( parser.getAttributeValue( nameSpace, "x" ) ) * PIXEL_TO_DOT_SCALE;
                                float y = Float.parseFloat( parser.getAttributeValue( nameSpace, "y" ) ) * PIXEL_TO_DOT_SCALE;
                                float width = Float.parseFloat( parser.getAttributeValue( nameSpace, "width" ) ) * PIXEL_TO_DOT_SCALE;
                                float height = Float.parseFloat( parser.getAttributeValue( nameSpace, "height" ) ) * PIXEL_TO_DOT_SCALE;

								symbol = new Symbol( noteId, pageId, "", "", "", x, y, x + width, y + height );
							}
							else if ( isSymbol && tag.equals( "command" ) )
							{
								symbol.action = parser.getAttributeValue( nameSpace, "action" );
								symbol.param = parser.getAttributeValue( nameSpace, "param" );
							}
							else if ( isSymbol && tag.equals( "matching_symbols" ) )
							{
								symbol.previousId = parser.getAttributeValue( nameSpace, "previous" );
								symbol.nextId = parser.getAttributeValue( nameSpace, "next" );
							}
						}
						//?????? 2.31f ???
						else
						{
							if ( tag.equals( "segment_info" ) )
							{
								String subCode = parser.getAttributeValue( nameSpace, "sub_code" );
								int totalPageSize = Integer.parseInt( parser.getAttributeValue( nameSpace, "total_size" ) );
								int segmentPageSize = Integer.parseInt( parser.getAttributeValue( nameSpace, "size" ) );
								int segmentNumber = Integer.parseInt( parser.getAttributeValue( nameSpace, "current_sequence" ) );
								int segmentStartPage = Integer.parseInt( parser.getAttributeValue( nameSpace, "ncode_start_page" ) );
								int segmentEndPage = Integer.parseInt( parser.getAttributeValue( nameSpace, "ncode_end_page" ) );
								Segment segment = new Segment(sectionCode, ownerCode, noteId, subCode,segmentNumber, segmentStartPage, segmentEndPage, totalPageSize, segmentPageSize );
								if(segmentTable.get( ""+sectionCode+"_"+ownerCode+"_"+noteId ) == null)
								{
									HashMap<String, Segment> map = new HashMap<String, Segment>();
									map.put( ""+segmentNumber, segment );
									segmentTable.put( ""+sectionCode+"_"+ownerCode+"_"+noteId, map );
								}
								else
								{
									segmentTable.get( ""+sectionCode+"_"+ownerCode+"_"+noteId ).put(""+segmentNumber, segment );
								}
							}
							else if ( tag.equals( "pages" ) )
							{
								totalPage = Integer.parseInt( parser.getAttributeValue( nameSpace, "count" ) );
							}
							else if ( tag.equals( "page_item" ) )
							{
								float x1 = Float.parseFloat( parser.getAttributeValue( nameSpace, "x1" ) ) * PIXEL_TO_DOT_SCALE;
								float x2 = Float.parseFloat( parser.getAttributeValue( nameSpace, "x2" ) ) * PIXEL_TO_DOT_SCALE;
								float y1 = Float.parseFloat( parser.getAttributeValue( nameSpace, "y1" ) ) * PIXEL_TO_DOT_SCALE;
								float y2 = Float.parseFloat( parser.getAttributeValue( nameSpace, "y2" ) ) * PIXEL_TO_DOT_SCALE;

                                String crop = parser.getAttributeValue( nameSpace, "crop_margin" );
                                String[] crops = crop.split( "," );
                                float margin_left = Float.parseFloat( crops[0] ) * PIXEL_TO_DOT_SCALE;
                                float margin_right = Float.parseFloat( crops[1] ) * PIXEL_TO_DOT_SCALE;
                                float margin_top = Float.parseFloat( crops[2] ) * PIXEL_TO_DOT_SCALE;
                                float margin_bottom = Float.parseFloat( crops[3] ) * PIXEL_TO_DOT_SCALE;
                                Page page = new Page();
                                page.noteId = noteId;
                                page.pageId = Integer.parseInt( parser.getAttributeValue( nameSpace, "number" ) ) + 1;
                                page.angle = Integer.parseInt( parser.getAttributeValue( nameSpace, "rotate_angle" ) );
                                page.width = x2 - x1 - margin_left - margin_right;
                                page.height = y2 - y1 - margin_top - margin_bottom;
                                page.margin_left = margin_left;
                                page.margin_top = margin_top;
                                page.margin_right = margin_right;
                                page.margin_bottom = margin_bottom;

                                put( noteId, page.pageId, page );
                            }
                            else if ( tag.equals( "symbol" ) )
                            {
                                isSymbol = true;

                                int pageId = Integer.parseInt( parser.getAttributeValue( nameSpace, "page" ) ) + 1;
                                float x = Float.parseFloat( parser.getAttributeValue( nameSpace, "x" ) ) * PIXEL_TO_DOT_SCALE;
                                float y = Float.parseFloat( parser.getAttributeValue( nameSpace, "y" ) ) * PIXEL_TO_DOT_SCALE;
                                float width = Float.parseFloat( parser.getAttributeValue( nameSpace, "width" ) ) * PIXEL_TO_DOT_SCALE;
                                float height = Float.parseFloat( parser.getAttributeValue( nameSpace, "height" ) ) * PIXEL_TO_DOT_SCALE;

								symbol = new Symbol( noteId, pageId, "", "", "", x, y, x + width, y + height );
							}
							else if ( isSymbol && tag.equals( "command" ) )
							{
								symbol.action = parser.getAttributeValue( nameSpace, "action" );
								symbol.param = parser.getAttributeValue( nameSpace, "param");
							}
							else if ( isSymbol && tag.equals( "matching_symbols" ) )
							{
								symbol.previousId = parser.getAttributeValue( nameSpace, "previous" );
								symbol.nextId = parser.getAttributeValue( nameSpace, "next" );
							}

                        }
                    }

                    break;

                case XmlPullParser.TEXT:

                    String value = parser.getText().trim();

                    if ( value != null && !value.equals( "" ) )
                    {
                        if ( tag.equals( "owner" ) )
                        {
                            ownerCode = Integer.parseInt( value );
                        }
                        else if ( tag.equals( "section" ) )
                        {
                            sectionCode = Integer.parseInt( value );
                        }
                        else if ( tag.equals( "code" ) )
                        {
                            noteId = Integer.parseInt( value );
                        }
                        else if ( tag.equals( "kind" ) )
                        {
                            kind = Integer.parseInt( value );
                        }
                        else if ( tag.equals( "title" ) )
                        {
                            bookTitle = value;
                        }
                        else if ( tag.equals( "extra_info" ) )
                        {
                            extra_info = value;
                        }
                        else if ( tag.equals( "start_page" ) )
                        {
                            startPage = Integer.parseInt( value );
                            tag = null;
                        }

                        if ( isSymbol )
                        {
                            if ( tag.equals( "id" ) )
                            {
                                symbol.id = value;
                            }
                            else if ( tag.equals( "name" ) )
                            {
                                symbol.name = value;
                            }
                        }
                    }

                    break;

                case XmlPullParser.END_TAG:

                    tag = parser.getName();

                    if ( tag.equals( "symbol" ) )
                    {
                        isSymbol = false;

                        lnkTbl.put( symbol.id, symbol );

                        this.put( noteId, symbol.pageId, symbol );

                        symbol = null;
                    }

                    break;

                default:
                    break;
            }
        }

        if(nprojVersion < 2.31f)
        {
            Segment segment = new Segment(sectionCode, ownerCode, noteId, "",0, startPage, startPage + totalPage - 1, totalPage, totalPage );
            HashMap<String, Segment> map = new HashMap<String, Segment>();
            map.put( ""+0 , segment );
            segmentTable.put( ""+sectionCode+"_"+ownerCode+"_"+noteId, map );
        }
        Book nBook = new Book( noteId,ownerCode, sectionCode, totalPage, bookTitle , kind, startPage, extra_info, nprojVersion);

        bookTable.put( noteId, nBook );

        Set<String> ids = lnkTbl.keySet();

        Iterator<String> it = ids.iterator();

        while ( it.hasNext() )
        {
            Symbol sym = lnkTbl.get( it.next() );

            if ( sym.previousId != null )
            {
                sym.previous = lnkTbl.get( sym.previousId );
            }

            if ( sym.nextId != null )
            {
                sym.next = lnkTbl.get( sym.nextId );
            }
        }
    }

//	private class Page
//	{
//		public int noteId, pageId, angle;
//		public float width, height;
//
//		public String toString()
//		{
//			return "Page => noteId : " + noteId + ", pageId : " + pageId + ", angle : " + angle + ", width : " + width + ", height : " + height;
//		}
//	}

    private static class Book
    {
        /**
         * The Note id.
         */
        public int noteId, /**
     * The Total page.
     */
    totalPage, /**
     * The Owner code.
     */
    ownerCode, /**
     * The Section code.
     */
    sectionCode, /**
     * The Kind.
     */
    kind, /**
     * The Start page.
     */
    startPage;
        /**
         * The Title.
         */
        public String title, /**
     * The Extra info.
     */
    extra_info;
        /**
         * The Nproj version.
         */
        public float nprojVersion;

        /**
         * Instantiates a new Book.
         *
         * @param noteId       the note id
         * @param ownerCode    the owner code
         * @param sectionCode  the section code
         * @param totalPage    the total page
         * @param title        the title
         * @param kind         the kind
         * @param startPage    the start page
         * @param extra_info   the extra info
         * @param nprojVersion the nproj version
         */
        public Book( int noteId, int ownerCode, int sectionCode, int totalPage, String title , int kind, int startPage, String extra_info, float nprojVersion)
        {
            this.noteId = noteId;
            this.ownerCode = ownerCode;
            this.sectionCode = sectionCode;
            this.kind = kind;
            this.totalPage = totalPage;
            this.title = title;
            if(this.title == null)
                this.title = "";

            this.extra_info = extra_info;
            if(this.extra_info == null)
                this.extra_info = "";
            this.startPage = startPage;
            this.nprojVersion = nprojVersion;
        }

        public String toString()
        {
            return "Book => nprojVersion:"+nprojVersion+" title : " + title + ", noteId : " + noteId  + ", ownerCode : " + ownerCode  + ", sectionCode : " + sectionCode + ", totalPage : " + totalPage + ", kind : " + kind + ", startPage : "+startPage+", extra_info : " + extra_info;
        }
    }
}
