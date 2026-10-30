package todoc.cochlear.remoteapp.ota;

import java.util.ArrayDeque;

/**
 * 장치 ACK로 확인한 이미지 바이트만 집계한다. 시간은 단조 증가 밀리초다.
 */
public final class OtaTransferRate
{
    private static final long WINDOW_MS = 1000;
    private final ArrayDeque<long[]> samples = new ArrayDeque<>();
    private long startedAt, total, windowBytes, average;
    private boolean running;

    public void reset()
    {
        samples.clear();
        total = windowBytes = average = 0;
        running = false;
    }

    public void start(long now)
    {
        reset();
        startedAt = now;
        running = true;
    }

    public void acknowledge(int bytes, long now)
    {
        if (!running || bytes <= 0)
        {
            return;
        }
        total += bytes;
        windowBytes += bytes;
        samples.addLast(new long[]{now, bytes});
        expire(now);
    }

    private void expire(long now)
    {
        while (!samples.isEmpty() && now - samples.peekFirst()[0] >= WINDOW_MS)
        {
            windowBytes -= samples.removeFirst()[1];
        }
    }

    public long bytesPerSecond(long now)
    {
        if (!running)
        {
            return 0;
        }
        expire(now);
        return windowBytes * 1000 / Math.max(1, Math.min(WINDOW_MS, now - startedAt));
    }

    public void finish(long now)
    {
        if (running)
        {
            average = total * 1000 / Math.max(1, now - startedAt);
            running = false;
            samples.clear();
            windowBytes = 0;
        }
    }

    public long averageBytesPerSecond()
    {
        return average;
    }
}
