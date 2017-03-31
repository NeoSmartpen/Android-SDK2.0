package kr.neolab.sdk.pen.bluetooth.cmd;

import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser20;

/**
 * The type Set time command.
 *
 * @author CHY
 */
public class SetTimeCommand extends Command
{
    /**
     * Instantiates a new Set time command.
     *
     * @param key  the key
     * @param comp the comp
     */
    public SetTimeCommand(int key, CommandManager comp)
    {
        super(key, comp);
    }
    
    protected void write() 
    {
        if(comp instanceof CommProcessor )
            super.comp.write( ProtocolParser.buildSetCurrentTimeData() );
        else
            super.comp.write( ProtocolParser20.buildSetCurrentTimeData() );
    }
}
