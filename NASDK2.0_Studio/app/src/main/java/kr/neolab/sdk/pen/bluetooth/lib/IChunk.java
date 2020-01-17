package kr.neolab.sdk.pen.bluetooth.lib;

/**
 * The interface Chunk.
 *
 * @author Moo
 */
public interface IChunk
{
    public void load();

    public int getChunksize();

    public byte getChecksum();

}
