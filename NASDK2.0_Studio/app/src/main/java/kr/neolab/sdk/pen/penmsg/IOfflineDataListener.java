package kr.neolab.sdk.pen.penmsg;

import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.structure.Symbol;

/**
 * Created by LMS on 2016-02-16.
 */
public interface IOfflineDataListener
{
    /**
     * On receive offline strokes.
     *
     * @param extra extra data
     * @param penAddress the pen address
     * @param strokes    the strokes
     * @param sectionId  the section id
     * @param ownerId    the owner id
     * @param noteId     the note id
     * @param symbols   the detected symbols
     */
    public void onReceiveOfflineStrokes(Object extra,String penAddress, Stroke[] strokes,int sectionId, int ownerId, int noteId, Symbol[] symbols);

}
