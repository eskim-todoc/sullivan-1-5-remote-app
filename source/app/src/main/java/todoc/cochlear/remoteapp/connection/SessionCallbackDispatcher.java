package todoc.cochlear.remoteapp.connection;

/**
 * Avoid a second queue hop which could move an already-delivered ACK behind its timeout.
 */
public final class SessionCallbackDispatcher
{
    public interface Poster
    {
        void post(Runnable task);
    }

    private SessionCallbackDispatcher()
    {
    }

    public static void dispatch(boolean alreadyOnLoop, Poster loop, Runnable guardedCallback)
    {
        if (alreadyOnLoop)
        {
            guardedCallback.run();
        }
        else
        {
            loop.post(guardedCallback);
        }
    }
}
