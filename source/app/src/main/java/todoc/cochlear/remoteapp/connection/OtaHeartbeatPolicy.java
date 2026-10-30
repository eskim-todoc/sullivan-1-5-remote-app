package todoc.cochlear.remoteapp.connection;

/**
 * 세션별 OTA 대기 유지 주기. 준비/전송/실패 상태에서는 파일 시스템 읽기도 하지 않는다.
 */
public final class OtaHeartbeatPolicy
{
    public static final long INTERVAL_MS = 30000;
    public static final long BUSY_RECHECK_MS = 1000;
    private long nextAtMs = -1;
    private boolean uncertainResponse;

    public void synchronize(OtaProgress.State state, boolean mappingActive, boolean connected, long nowMs)
    {
        boolean allowed = connected && mappingActive
            && (state == OtaProgress.State.READY || state == OtaProgress.State.COMPLETE);
        if (!allowed)
        {
            nextAtMs = -1;
        }
        else if (nextAtMs < 0)
        {
            nextAtMs = nowMs + INTERVAL_MS;
        }
    }

    public long nextDelayMs(long nowMs)
    {
        return nextAtMs < 0 ? -1 : Math.max(0, nextAtMs - nowMs);
    }

    /**
     * 바쁜 통신을 덮어쓰지 않는다. 전송을 예약한 경우에만 다음 30초 주기를 시작한다.
     */
    public boolean claim(long nowMs, boolean transportIdle)
    {
        if (!transportIdle || nextAtMs < 0 || nowMs < nextAtMs)
        {
            return false;
        }
        nextAtMs = nowMs + INTERVAL_MS;
        return true;
    }

    /**
     * C2 오류에는 Info/Select 옵션이 없다. 시한이 지난 읽기 오류를 새 부팅의 오류로 단정할 수 없다.
     */
    public void responseTimedOut()
    {
        uncertainResponse = true;
    }

    public boolean isAmbiguousBootError(byte[] response)
    {
        if (!uncertainResponse || response == null || response.length < 2)
        {
            return false;
        }
        int header = response[0] & 255;
        return (header == 0xF0 || header == 0xFE) && (response[1] & 255) == 0xC2;
    }
}
