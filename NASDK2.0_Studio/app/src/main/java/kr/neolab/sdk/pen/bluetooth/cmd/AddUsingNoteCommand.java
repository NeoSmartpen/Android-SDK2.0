package kr.neolab.sdk.pen.bluetooth.cmd;

import java.util.ArrayList;

import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser20;
import kr.neolab.sdk.util.UseNoteData;

/**
 * The type Set time command.
 *
 * @author CHY
 */
public class AddUsingNoteCommand extends Command
{
    /**
     * Instantiates a new Set time command.
     *
     * @param key  the key
     * @param comp the comp
     */
    public AddUsingNoteCommand(int key, CommandManager comp)
    {
        super(key, comp);
    }

    private int[]sectionId = null, ownerId = null;

    private ArrayList<UseNoteData> noteList = null;

    boolean All = false;

    public void setNote(int[] sectionId, int[] ownerId)
    {
        this.sectionId = sectionId;
        this.ownerId = ownerId;
    }

    public void setNote(ArrayList<UseNoteData> noteList)
    {
        this.noteList = noteList;
    }

    public void setNoteAll()
    {
        All = true;
    }


    protected void write() 
    {
        if(All)
            super.comp.write( ProtocolParser20.buildAddUsingAllNotes() );
        else if(noteList != null)
            super.comp.write( ProtocolParser20.buildAddUsingNotes(noteList));
        else
            super.comp.write( ProtocolParser20.buildAddUsingNotes(sectionId, ownerId) );
    }
}
