package kr.neolab.sdk.metadata.structure;

/**
 * Created by LMS on 2016-07-21.
 */
public class Segment
{
    /**
     * The M section.
     */
    public int mSection;

    /**
     * The M owner.
     */
    public int mOwner;

    /**
     * The M book.
     */
    public int mBook;

    /**
     * The M sub code.
     */
    public String mSubCode;

    /**
     * The M segment number.
     */
    public int mSegmentNumber;

    /**
     * The M start page number.
     */
    public int mStartPageNumber;

    /**
     * The M end page number.
     */
    public int mEndPageNumber;

    /**
     * The M total page size.
     */
    public int mTotalPageSize;

    /**
     * The M page size.
     */
    public int mPageSize;

    /**
     * Instantiates a new Segment.
     *
     * @param section         the section
     * @param owner           the owner
     * @param book            the book
     * @param subCode         the sub code
     * @param segmentNumber   the segment number
     * @param startPageNumber the start page number
     * @param endPageNumber   the end page number
     * @param totalPageSize   the total page size
     * @param pageSize        the page size
     */
    public Segment(int section, int owner, int book, String subCode,int segmentNumber, int startPageNumber,int  endPageNumber, int totalPageSize, int pageSize)
    {
        this.mSection = section;
        this.mOwner = owner;
        this.mBook = book;
        this.mSubCode = subCode;
        this.mSegmentNumber = segmentNumber;
        this.mStartPageNumber = startPageNumber;
        this.mEndPageNumber = endPageNumber;
        this.mTotalPageSize = totalPageSize;
        this.mPageSize = pageSize;

    }

}
