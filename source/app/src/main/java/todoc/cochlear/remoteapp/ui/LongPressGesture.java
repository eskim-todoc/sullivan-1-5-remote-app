package todoc.cochlear.remoteapp.ui;

/**
 * 터치의 시간·이동 판단만 소유하며 Android 콜백은 실행하지 않는다.
 */
final class LongPressGesture
{
    private enum State
    {
        IDLE, PRESSED, LONG_PRESSED
    }

    private long delayMs;
    private final float slopSquared;
    private State state = State.IDLE;
    private long generation;
    private long startedAtMs;
    private float downX;
    private float downY;

    LongPressGesture(int systemTimeoutMs, int touchSlopPixels)
    {
        setSystemTimeout(systemTimeoutMs);
        slopSquared = (float) touchSlopPixels * touchSlopPixels;
    }

    void setSystemTimeout(int systemTimeoutMs)
    {
        delayMs = (systemTimeoutMs * 3L + 1L) / 2L;
    }

    long delayMs()
    {
        return delayMs;
    }

    long start(long nowMs, float x, float y)
    {
        generation++;
        state = State.PRESSED;
        startedAtMs = nowMs;
        downX = x;
        downY = y;
        return generation;
    }

    boolean move(long token, float x, float y)
    {
        if (token != generation || state != State.PRESSED)
        {
            return false;
        }
        if (!withinSlop(x, y))
        {
            cancel();
            return false;
        }
        return true;
    }

    boolean claimLongPress(long token, long nowMs)
    {
        if (token != generation || state != State.PRESSED || nowMs - startedAtMs < delayMs)
        {
            return false;
        }
        state = State.LONG_PRESSED;
        return true;
    }

    void unhandledLongPress(long token)
    {
        if (token == generation && state == State.LONG_PRESSED)
        {
            state = State.PRESSED;
        }
    }

    boolean release(long token, float x, float y)
    {
        boolean click = token == generation && state == State.PRESSED && withinSlop(x, y);
        cancel();
        return click;
    }

    void cancel()
    {
        generation++;
        state = State.IDLE;
    }

    private boolean withinSlop(float x, float y)
    {
        float dx = x - downX;
        float dy = y - downY;
        return dx * dx + dy * dy <= slopSquared;
    }
}
