package kr.neolab.sdk.pen.bluetooth.cmd;

/**
 * The interface Command.
 *
 * @author CHY
 */
public interface ICommand
{
    /**
     * 커맨드의 키를 반환한다.
     *
     * @return id id
     */
    public int getId();

    /**
     * 커맨드를 실행한다.
     */
    public void excute();

    /**
     * 현재 실행되고 있는 커맨드를 중단한다.
     */
    public void finish();

    /**
     * 이 커맨드가 실행중인 상태인지 조회
     *
     * @return boolean boolean
     */
    public boolean isAlive();
}
