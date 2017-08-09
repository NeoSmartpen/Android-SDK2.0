package kr.neolab.sdk.pen.bluetooth.cmd;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Queue;

import kr.neolab.sdk.pen.bluetooth.IConnectedThread;
import kr.neolab.sdk.pen.bluetooth.lib.Chunk;
import kr.neolab.sdk.util.NLog;

/**
 * The type Command manager.
 *
 * @author CHY
 */
abstract public class CommandManager
{
    /**
     * The Commands.
     */
    public LinkedHashMap<Integer, ICommand> commands = new LinkedHashMap<Integer, ICommand>();
    /**
     * The R queue.
     */
    public Queue<Integer> rQueue = new LinkedList<Integer>();

    /**
     * Execute.
     *
     * @param command the command
     */
    public void execute(ICommand command)
    {
        if ( !commands.containsKey(command.getId()) )
        {
            commands.put(command.getId(), command);
            command.excute();
            return;
        }

        if ( commands.get(command.getId()).isAlive() )
        {
            NLog.e("[CommandManager] Command is still excuting.");
            command = null;
            return;
        }
        
        commands.remove(command.getId());
        commands.put(command.getId(), command);
        command.excute();
    }

    /**
     * Kill.
     *
     * @param key the key
     */
    public void kill(int key)
    {
        ICommand command = commands.get(key);
        
        if ( command != null )
        {
            command.finish();
        }
    }

    /**
     * Write.
     *
     * @param buffer the buffer
     */
    abstract public void write( byte[] buffer );

    /**
     * Gets conn.
     *
     * @return the conn
     */
    abstract public IConnectedThread getConn();

    /**
     * Sets chunk.
     *
     * @param chunk the chunk
     */
    abstract public void setChunk( Chunk chunk );

    /**
     * Finish upgrade.
     */
    abstract public void finishUpgrade();

    /**
     * Fill.
     *
     * @param data the data
     * @param size the size
     */
    abstract public void fill( byte data[], int size );

    /**
     *reqPenStatus
     */
    abstract public void reqPenStatus();

    /**
     * force calibrate
     */
    abstract public void reqForceCalibrate();

    /**
     * Req set auto shutdown time.
     *
     * @param min the min
     */
    abstract public void reqSetAutoShutdownTime( short min );

    /**
     * Req set pen sensitivity.
     *
     * @param sensitivity the sensitivity
     */
    abstract public void reqSetPenSensitivity( short sensitivity );

    /**
     * Req add using note.
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteIds   the note ids
     */
    abstract public void reqAddUsingNote( int sectionId, int ownerId, int[] noteIds );

    /**
     * Req add using note.
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     */
    abstract public void reqAddUsingNote( int sectionId, int ownerId );

    /**
     * Req add using note.
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     */
    abstract public void reqAddUsingNote( int[] sectionId, int[] ownerId );

    /**
     * Req add using note all.
     */
    abstract public void reqAddUsingNoteAll();

    /**
     * Req offline data.
     *
     * @param sectionId the section id
     * @param ownerId   the owner id
     * @param noteId    the note id
     */
    abstract public void reqOfflineData( int sectionId, int ownerId, int noteId );

    /**
     * Req offline data list.
     */
    abstract public void reqOfflineDataList();

//    /**
//     * Req offline data remove.
//     * @param sectionId the section id
//     * @param ownerId   the owner id
//     */
//    abstract public void reqOfflineDataRemove( int sectionId, int ownerId );
//
//    abstract public void reqOfflineDataRemove( int sectionId, int ownerId, int[] noteIds);

    /**
     * Req auto power setup on off.
     *
     * @param isOn the is on
     */
    abstract public void reqAutoPowerSetupOnOff( boolean isOn );

    /**
     * Req pen beep setup.
     *
     * @param isOn the is on
     */
    abstract public void reqPenBeepSetup( boolean isOn );

    /**
     * Req setup pen tip color.
     *
     * @param color the color
     */
    abstract public void reqSetupPenTipColor( int color );

    /**
     * Req input password.
     *
     * @param password the password
     */
    abstract public void reqInputPassword( String password );

    /**
     * Req set up password.
     *
     * @param oldPassword the old password
     * @param newPassword the new password
     */
    abstract public void reqSetUpPassword( String oldPassword, String newPassword );

//    /**
//     * reqPenSwUpgrade
//     *
//     * @param source the source
//     * @param target the target
//     */
//    abstract public void reqPenSwUpgrade( File source, String target);

    /**
     * Req suspend pen sw upgrade.
     */
    abstract public void reqSuspendPenSwUpgrade();

//    /**
//     *resPenSwRequest
//     *
//     * @param index
//     */
//    abstract public void resPenSwRequest( int index );
//
//    /**
//     * resPenSwUpgStatus
//     *
//     * @param status
//     */
//    abstract public void resPenSwUpgStatus( int status );

}
