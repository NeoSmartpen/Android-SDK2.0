package kr.neolab.sdk.pen.penmsg;

import kr.neolab.sdk.ink.structure.Stroke;

/**
 * Created by LMS on 2016-02-16.
 */
public interface IOfflineDataListener
{
    /**
     * On receive offline strokes.
     *
     * @param strokes   the strokes
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     */
    public void onReceiveOfflineStrokes(Stroke[] strokes,int sectionId, int ownerId, int noteId);

}
