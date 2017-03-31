package kr.neolab.sdk.pen.bluetooth.cmd;

import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser;

/**
 * The type Res pen on off command.
 *
 * @author CHY
 */
public class ResPenOnOffCommand extends Command
{
    private boolean status;

    /**
     * Instantiates a new Res pen on off command.
     *
     * @param key  the key
     * @param comp the comp
     */
    public ResPenOnOffCommand(int key, CommandManager comp)
    {
        super(key, comp);
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(boolean status)
    {
        this.status = status;
    }
    
    protected void write() 
    {
        super.comp.write( ProtocolParser.buildPenOnOffData(status) );
    }
    
    public void run() 
    {
        this.write();
        super.isAlive = false;
    }
}
