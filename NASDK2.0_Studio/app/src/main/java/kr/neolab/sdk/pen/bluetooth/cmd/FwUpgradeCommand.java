package kr.neolab.sdk.pen.bluetooth.cmd;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

import kr.neolab.sdk.pen.bluetooth.lib.Chunk;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolParser;
import kr.neolab.sdk.pen.penmsg.PenMsg;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;

/**
 * The type Fw upgrade command.
 *
 * @author CHY
 */
public class FwUpgradeCommand extends Command
{
	private File source = null;
	/**
	 * The R queue.
	 */
	public Queue<Integer> rQueue = new LinkedList<Integer>();

	private String target;
	private String fwVersion;
	private String deviceName;


	private int count = 0;

	private boolean repeat = true;

	private int wait_timeout = 5000;

	private int wait = 10;

	/**
	 * Instantiates a new Fw upgrade command.
	 *
	 * @param key  the key
	 * @param comp the comp
	 */
	public FwUpgradeCommand( int key, CommandManager comp )
	{
		super( key, comp );
	}

	/**
	 * Sets info.
	 *
	 * @param source the source
	 * @param target the target
	 */
	public void setInfo( File source, String target )
	{
		this.source = source;
		this.target = target;

	}

	@Override
	protected void write()
	{
	}

	private void doUpgrade()
	{
		Chunk chunk = null;

		try
		{

			int filesize = (int) source.length();
			NLog.d( "doUpgrade filesize="+filesize +"source:"+source.getName());
//			filesize = 647929;
			InputStream is = new FileInputStream( source );
			chunk = new Chunk( is, filesize );

			chunk.load();

			comp.setChunk( chunk );

			byte[] datas = null;
			try
			{
				datas = ProtocolParser.buildPenSwUpgrade( target, filesize, (short) chunk.getChunkLength(), (short) chunk.getChunksize() );
				comp.write( datas );
			}
			catch ( Exception e )
			{
				NLog.e( "[FwUpgradeCommand] can't write upgrade request packet.", e );
				comp.getConn().onCreateMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
				return;
			}
		}
		catch ( IOException e )
		{
			NLog.e( "[FwUpgradeCommand] can't open firmware file.", e );
			comp.getConn().onCreateMsg( new PenMsg( PenMsgType.PEN_FW_UPGRADE_FAILURE ) );
			return;
		}

		Queue<Integer> queue = comp.rQueue;

		int timeout = wait_timeout / wait;

		boolean isFirst = true;
		
		while ( repeat )
		{
		    synchronized (queue) {
                if (!queue.isEmpty()) {
                    int index = (Integer) queue.poll();

                    if (isFirst && index > 0) {
                        for (int i = 0; i <= index; i++) {
                            chunk.setStatus(i, true);
                        }
                    }

                    isFirst = false;

                    count = 0;

                    try {
                        comp.write(ProtocolParser.buildPenSwUpgradeResponse(index, chunk.getChecksum(index), chunk.getChunk(index)));

                        chunk.setStatus(index, true);

                        int maximum = chunk.getChunkLength();
                        int current = chunk.getStatus() > maximum ? maximum : chunk.getStatus();

                        NLog.d("[FwUpgradeCommand] send progress => maximum : " + maximum + ", current : " + current);

                        JSONObject job;
                        try {
                            job = new JSONObject();
                            job.put("total_size", maximum);
                            job.put("sent_size", current);

                            comp.getConn().onCreateMsg(new PenMsg(PenMsgType.PEN_FW_UPGRADE_STATUS, job));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        NLog.e("[FwUpgradeCommand] can't write chunk packet.", e);

                        chunk.setStatus(index, false);
                    }
                }

                if (count >= timeout) {
                    NLog.e("[FwUpgradeCommand] tracing : wait timeout.");
                    comp.getConn().onCreateMsg(new PenMsg(PenMsgType.PEN_FW_UPGRADE_FAILURE));
                    comp.finishUpgrade();
                    repeat = false;

                    continue;
                }
            }

			try
			{
				Thread.sleep( wait );
			}
			catch ( Exception e )
			{
				e.printStackTrace();
			}

			count++;
		}
	}

	public void run()
	{
		this.doUpgrade();

		super.isAlive = false;
	}

	public void finish()
	{
		this.repeat = false;
	}
}
