package kr.neolab.sdk.pen.offline;

import kr.neolab.sdk.ink.structure.Stroke;

/**
 * Created by Moo on 2016-02-17.
 */
public class OfflineByteData
{
    /**
     * The Section id.
     */
    public int sectionId = 0, /**
 * The Owner id.
 */
ownerId = 0, /**
 * The Note id.
 */
noteId = 0;
    /**
     * The Strokes.
     */
    public Stroke[] strokes = null;

    public Object extraData = null;

    /**
     * Instantiates a new Offline byte data.
     *
     * @param extra   extra data
     * @param strokes   the strokes
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     */
    public OfflineByteData ( Object extra, Stroke[] strokes, int sectionId, int ownerId, int noteId )
    {
        this.sectionId = sectionId;
        this.ownerId = ownerId;
        this.noteId = noteId;
        this.strokes = strokes;
        this.extraData = extra;
    }
}
