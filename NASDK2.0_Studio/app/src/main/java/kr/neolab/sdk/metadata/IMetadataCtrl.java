package kr.neolab.sdk.metadata;

import android.content.Context;

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
 * The type Metadata file ctrl Interface.
 *
 * @author CHY
 */
public interface IMetadataCtrl
{
    /**
     * The Metadata file loading
     *
     * @param file the file
     * @throws XmlPullParserException       the xml pull parser exception
     * @throws IOException                  the io exception
     * @throws SAXException                 the sax exception
     * @throws ParserConfigurationException the parser configuration exception
     */
    public void loadFile ( File file ) throws XmlPullParserException, IOException, SAXException, ParserConfigurationException;

    /**
     * The Metadata file loading
     *
     * @param context  the context
     * @param filePath the file path
     * @throws XmlPullParserException       the xml pull parser exception
     * @throws IOException                  the io exception
     * @throws SAXException                 the sax exception
     * @throws ParserConfigurationException the parser configuration exception
     */
    public void loadFileAsset ( Context context, String filePath ) throws XmlPullParserException, IOException, SAXException, ParserConfigurationException;

    /**
     * The Metadata file loading
     *
     * @param fileDirectoryPath the file directory path
     */
    public void loadFiles ( String fileDirectoryPath );

    /**
     * Title
     *
     * @param noteId the note id
     * @return title title
     */
    public String getTitle ( int noteId );

    /**
     * OwnerCode
     *
     * @param noteId the note id
     * @return owner code
     */
    public int getOwnerCode ( int noteId );

    /**
     * SectionCode
     *
     * @param noteId the note id
     * @return section code
     */
    public int getSectionCode ( int noteId );

    /**
     * kind : 0 normal 1 franklin
     *
     * @param noteId the note id
     * @return kind kind
     */
    public int getKind ( int noteId );

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
    public ArrayList<Integer> getNoteTypeList ( int kind );

    /**
     * ExtraInfo
     *
     * @param noteId the note id
     * @return extra info
     */
    public String getExtraInfo ( int noteId );

    /**
     * Total Pages
     *
     * @param noteId the note id
     * @return total pages
     */
    public int getTotalPages ( int noteId );

    /**
     * Page width
     *
     * @param noteId the note id
     * @param pageId the page id
     * @return page width
     */
    public float getPageWidth ( int noteId, int pageId );

    /**
     * Page height
     *
     * @param noteId the note id
     * @param pageId the page id
     * @return page height
     */
    public float getPageHeight ( int noteId, int pageId );

    /**
     * Page width include margin values
     *
     * @param noteId the note id
     * @param pageId the page id
     * @return page width with margin
     */
    public float getPageWidthWithMargin ( int noteId, int pageId );

    /**
     * Page height include margin values
     *
     * @param noteId the note id
     * @param pageId the page id
     * @return page height with margin
     */
    public float getPageHeightWithMargin ( int noteId, int pageId );

    /**
     * Gets page margin left.
     *
     * @param noteId the note id
     * @param pageId the page id
     * @return page margin left
     */
    public float getPageMarginLeft ( int noteId, int pageId );

    /**
     * Gets page margin top.
     *
     * @param noteId the note id
     * @param pageId the page id
     * @return page margin top
     */
    public float getPageMarginTop ( int noteId, int pageId );

    /**
     * Gets page margin right.
     *
     * @param noteId the note id
     * @param pageId the page id
     * @return page margin right
     */
    public float getPageMarginRight ( int noteId, int pageId );

    /**
     * Gets page margin bottom.
     *
     * @param noteId the note id
     * @param pageId the page id
     * @return page margin bottom
     */
    public float getPageMarginBottom ( int noteId, int pageId );

    /**
     * find Symbol include Stroke
     *
     * @param nstr the nstr
     * @return symbol [ ]
     */
    public Symbol[] findApplicableSymbols ( Stroke nstr );

    /**
     * find Symbol include x,y coordinate
     *
     * @param noteId the note id
     * @param pageId the page id
     * @param x      the x
     * @param y      the y
     * @return symbol [ ]
     */
    public Symbol[] findApplicableSymbols ( int noteId, int pageId, float x, float y );

    /**
     * find all Symbol in Page.
     *
     * @param noteId the note id
     * @param pageId the page id
     * @return symbol [ ]
     */
    public Symbol[] findApplicableSymbols ( int noteId, int pageId );

    /**
     * find Symbol by ID.
     *
     * @param id the id
     * @return symbol symbol
     */
    public Symbol findApplicableSymbol ( String id );

    /**
     * Get Stroke that contains the symbol.
     *
     * @param symbol the symbol
     * @param strokes the strokes
     * @return Stroke[] strokes that contains the symbol
     */
    public Stroke[] getInsideStrokes( Symbol symbol, Stroke[] strokes );

    /**
     * find all Symbol.
     *
     * @return symbol [ ]
     */
    public Symbol[] getSymbols ();

    /**
     * Get segments segment [ ].
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     * @return the segment [ ]
     */
    public Segment[] getSegments ( int sectionId, int ownerId, int noteId );

    /**
     * Parse by sax.
     *
     * @param istream the istream
     * @throws IOException                  the io exception
     * @throws SAXException                 the sax exception
     * @throws ParserConfigurationException the parser configuration exception
     */
    public void parseBySAX ( InputStream istream ) throws IOException, SAXException, ParserConfigurationException;

    /**
     * Parse by xml pull parser.
     *
     * @param istream the istream
     * @throws XmlPullParserException the xml pull parser exception
     * @throws IOException            the io exception
     */
    public void parseByXmlPullParser ( InputStream istream ) throws XmlPullParserException, IOException;

    /**
     * Print.
     */
    public void print ();

    public void clear();
}
