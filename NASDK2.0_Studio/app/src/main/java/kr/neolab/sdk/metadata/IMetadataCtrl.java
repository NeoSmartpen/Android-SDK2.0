package kr.neolab.sdk.metadata;

import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.structure.Segment;
import kr.neolab.sdk.metadata.structure.Symbol;

/**
 * 메타데이터 파일을 로딩하고 데이터 조회 기능을 제공함
 *
 * @author CHY
 */
public interface IMetadataCtrl
{
	/**
	 * 메타데이터 파일 로딩 (개별파일)
	 *
	 * @param file the file
	 * @throws XmlPullParserException       the xml pull parser exception
	 * @throws IOException                  the io exception
	 * @throws SAXException                 the sax exception
	 * @throws ParserConfigurationException the parser configuration exception
	 */
	public void loadFile( File file ) throws XmlPullParserException, IOException, SAXException, ParserConfigurationException;

	/**
	 * 메타데이터 파일 로딩 (디렉토리)
	 *
	 * @param fileDirectoryPath the file directory path
	 */
	public void loadFiles( String fileDirectoryPath );

	/**
	 * 제목
	 *
	 * @param noteId the note id
	 * @return title title
	 */
	public String getTitle( int noteId );

	/**
	 * OwnerCode
	 *
	 * @param noteId the note id
	 * @return owner code
	 */
	public int getOwnerCode( int noteId );

	/**
	 * SectionCode
	 *
	 * @param noteId the note id
	 * @return section code
	 */
	public int getSectionCode( int noteId );

	/**
	 * kind : 0 normal 1 franklin
	 *
	 * @param noteId the note id
	 * @return kind kind
	 */
	public int getKind( int noteId );

	/**
	 * start page
	 *
	 * @param noteId the note id
	 * @return start page
	 */
	public int getStartPage ( int noteId );

	/**
	 * kind : 0 normal 1 franklin
	 *
	 * @param kind the kind
	 * @return note type list
	 */
	public ArrayList<Integer> getNoteTypeList( int kind );

	/**
	 * ExtraInfo
	 *
	 * @param noteId the note id
	 * @return extra info
	 */
	public String getExtraInfo( int noteId );

	/**
	 * 총 페이지 수
	 *
	 * @param noteId the note id
	 * @return total pages
	 */
	public int getTotalPages( int noteId );

	/**
	 * 페이지 width
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @return page width
	 */
	public float getPageWidth( int noteId, int pageId );

	/**
	 * 페이지 height
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @return page height
	 */
	public float getPageHeight( int noteId, int pageId );

	/**
	 * 마진 값을 포함한 페이지 width
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @return page width with margin
	 */
	public float getPageWidthWithMargin( int noteId, int pageId );

	/**
	 * 마진 값을 포함한 페이지 height
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @return page height with margin
	 */
	public float getPageHeightWithMargin( int noteId, int pageId );

	/**
	 * Gets page margin left.
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @return page margin left
	 */
	public float getPageMarginLeft( int noteId, int pageId );

	/**
	 * Gets page margin top.
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @return page margin top
	 */
	public float getPageMarginTop( int noteId, int pageId );

	/**
	 * Gets page margin right.
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @return page margin right
	 */
	public float getPageMarginRight( int noteId, int pageId );

	/**
	 * Gets page margin bottom.
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @return page margin bottom
	 */
	public float getPageMarginBottom( int noteId, int pageId );

	/**
	 * Stroke와 겹쳐진 Symbol을 구함
	 *
	 * @param nstr the nstr
	 * @return symbol [ ]
	 */
	public Symbol[] findApplicableSymbols( Stroke nstr );

	/**
	 * x,y 좌표를 포함하는 Symbol을 구함
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @param x      the x
	 * @param y      the y
	 * @return symbol [ ]
	 */
	public Symbol[] findApplicableSymbols( int noteId, int pageId, float x, float y );

	/**
	 * Page에 등록된 모든 Symbol을 구함
	 *
	 * @param noteId the note id
	 * @param pageId the page id
	 * @return symbol [ ]
	 */
	public Symbol[] findApplicableSymbols( int noteId, int pageId );

	/**
	 * Symbol ID로 Symbol 을 구함.
	 *
	 * @param id the id
	 * @return symbol
	 */
	public Symbol findApplicableSymbol( String id);

	/**
	 * 등록된 모든 Symbol을 구함
	 *
	 * @return symbol [ ]
	 */
	public Symbol[] getSymbols();

	/**
	 * Get segments segment [ ].
	 *
	 * @param sectionId the section id
	 * @param ownerId   the owner id
	 * @param noteId    the note id
	 * @return the segment [ ]
	 */
	public Segment[] getSegments(int sectionId, int ownerId, int noteId);

	/**
	 * Parse by sax.
	 *
	 * @param istream the istream
	 * @throws IOException                  the io exception
	 * @throws SAXException                 the sax exception
	 * @throws ParserConfigurationException the parser configuration exception
	 */
	public void parseBySAX( InputStream istream ) throws IOException, SAXException, ParserConfigurationException;

	/**
	 * Parse by xml pull parser.
	 *
	 * @param istream the istream
	 * @throws XmlPullParserException the xml pull parser exception
	 * @throws IOException            the io exception
	 */
	public void parseByXmlPullParser( InputStream istream ) throws XmlPullParserException, IOException;

	/**
	 * Print.
	 */
	public void print();
}
