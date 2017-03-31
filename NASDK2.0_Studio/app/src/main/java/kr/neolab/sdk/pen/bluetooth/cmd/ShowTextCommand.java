package kr.neolab.sdk.pen.bluetooth.cmd;

import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser;

/**
 * The type Show text command.
 *
 * @author A
 */
public class ShowTextCommand extends Command
{
	private String status;

    /**
     * Instantiates a new Show text command.
     *
     * @param key  the key
     * @param comp the comp
     */
    public ShowTextCommand(int key, CommandManager comp)
    {
		super(key, comp);
	}

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(String status)
    {
        this.status = status;
    }
    
    protected void write() 
    {
        if(comp instanceof CommProcessor )
            super.comp.write( ProtocolParser.buildShowTextData(status) );
    }
}
