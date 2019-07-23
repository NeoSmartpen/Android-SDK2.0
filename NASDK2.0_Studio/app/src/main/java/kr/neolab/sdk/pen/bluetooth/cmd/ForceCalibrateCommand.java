package kr.neolab.sdk.pen.bluetooth.cmd;

import kr.neolab.sdk.pen.bluetooth.comm.CommProcessor;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser;

/**
 * The type Force calibrate command.
 *
 * @author CHY
 */
public class ForceCalibrateCommand extends Command
{
    /**
     * Instantiates a new Force calibrate command.
     *
     * @param key  the key
     * @param comp the comp
     */
    public ForceCalibrateCommand(int key, CommandManager comp)
    {
        super(key, comp);
    }

    protected void write() 
    {
        if(comp instanceof CommProcessor )
            super.comp.write( ProtocolParser.buildForceCalibrateData() );
    }
    
    public void run() 
    {
        this.write();
        super.isAlive = false;
    }
}
