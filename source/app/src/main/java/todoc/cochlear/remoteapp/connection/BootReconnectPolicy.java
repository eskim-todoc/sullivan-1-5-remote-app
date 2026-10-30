package todoc.cochlear.remoteapp.connection;

/**
 * Bounded recovery for a single physical device after an accepted C2 reboot. Main-thread owned.
 */
public final class BootReconnectPolicy
{
    public static final int MAX_ATTEMPTS = 3;
    public static final long INITIAL_DELAY_MS = 7000;
    public static final long RETRY_DELAY_MS = 5000;
    public static final long ATTEMPT_TIMEOUT_MS = 15000;
    private int attempts;
    private boolean cancelled, succeeded, exhausted;

    public long nextDelayMs()
    {
        if (cancelled || succeeded || exhausted)
        {
            return -1;
        }
        if (attempts >= MAX_ATTEMPTS)
        {
            exhausted = true;
            return -1;
        }
        return attempts++ == 0 ? INITIAL_DELAY_MS : RETRY_DELAY_MS;
    }

    public int attempts()
    {
        return attempts;
    }

    public boolean active()
    {
        return !cancelled && !succeeded && !exhausted;
    }

    public boolean succeeded()
    {
        return succeeded;
    }

    public void cancel()
    {
        cancelled = true;
    }

    public void complete()
    {
        if (!cancelled && !exhausted)
        {
            succeeded = true;
        }
    }
}
