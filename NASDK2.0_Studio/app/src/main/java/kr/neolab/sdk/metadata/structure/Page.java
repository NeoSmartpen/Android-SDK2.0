package kr.neolab.sdk.metadata.structure;

/**
 * Created by LMS on 2016-07-21.
 */
public class Page
{
    /**
     * The Note id.
     */
    public int noteId, /**
 * The Page id.
 */
pageId, /**
 * The Angle.
 */
angle;
    /**
     * The Width.
     */
    public float width, /**
 * The Height.
 */
height, /**
 * The Margin left.
 */
margin_left, /**
 * The Margin top.
 */
margin_top, /**
 * The Margin right.
 */
margin_right, /**
 * The Margin bottom.
 */
margin_bottom;

    public String toString()
    {
        return "Page => noteId : " + noteId + ", pageId : " + pageId + ", angle : " + angle + ", width : " + width + ", height : " + height+", margin_left:"+ margin_left +", margin_top:"+ margin_top;
    }
}
