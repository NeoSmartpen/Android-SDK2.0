package kr.neolab.sdk.pen.bluetooth.cmd;

/**
 * The interface Command.
 *
 * @author CHY
 */
public interface ICommand
{
    /**
     * getId
     *
     * @return id id
     */
    public int getId();

    /**
     * excute
     */
    public void excute();

    /**
     * finish
     */
    public void finish();

    /**
     * isAlive
     *
     * @return boolean boolean
     */
    public boolean isAlive();
}
