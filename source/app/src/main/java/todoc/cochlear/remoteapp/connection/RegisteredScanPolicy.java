package todoc.cochlear.remoteapp.connection;

import java.util.HashSet;
import java.util.Set;

/**
 * 검색 세대별 새 광고만 접수하고, 먼저 발견한 활성 등록 장치 한 대만 연결을 시도한다.
 */
public final class RegisteredScanPolicy
{
    private final Set<String> observedAddresses = new HashSet<>();
    private long generation;
    private long startedAtNanos;
    private boolean active;
    private boolean autoConnecting;

    public long begin(boolean autoConnecting, long startedAtNanos)
    {
        generation++;
        this.startedAtNanos = startedAtNanos;
        this.autoConnecting = autoConnecting;
        active = true;
        observedAddresses.clear();
        return generation;
    }

    public void stop()
    {
        active = false;
    }

    public boolean isActive()
    {
        return active;
    }

    public boolean isAutoConnecting()
    {
        return active && autoConnecting;
    }

    public boolean accepts(long scanGeneration, long advertisementNanos)
    {
        return active && scanGeneration == generation && advertisementNanos >= startedAtNanos;
    }

    public boolean acceptsGeneration(long scanGeneration)
    {
        return active && scanGeneration == generation;
    }

    public boolean claimConnection(long scanGeneration, long advertisementNanos, String address,
                                   boolean registeredAndActive, boolean alreadyOpen)
    {
        if (!isAutoConnecting() || !accepts(scanGeneration, advertisementNanos)
            || !registeredAndActive || address == null || address.isEmpty())
        {
            return false;
        }

        // 검색 중 사용자가 해제한 장치를 같은 검색의 다음 광고로 다시 붙이지 않는다.
        if (!observedAddresses.add(address))
        {
            return false;
        }
        if (alreadyOpen)
        {
            return false;
        }
        // 같은 배치나 이미 대기 중인 광고도 두 번째 연결을 시작하지 못하게 한다.
        active = false;
        return true;
    }
}
