package todoc.cochlear.remoteapp.connection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

/**
 * Main-thread owned registry. Tokens invalidate callbacks from a closed or superseded GATT.
 */
public final class SessionRegistry<T>
{
    public static final class Token
    {
        public final String id, address;
        public final long generation;
        private boolean active = true;

        private Token(String address, long generation)
        {
            this.address = address;
            this.generation = generation;
            id = address + "/" + generation;
        }

        public boolean isActive()
        {
            return active;
        }
    }

    private final Map<String, Token> tokens = new LinkedHashMap<>();
    private final Map<String, T> sessions = new LinkedHashMap<>();
    private long generation;

    public Token open(String address)
    {
        String normalized = address.toUpperCase(Locale.ROOT);
        Token previous = tokens.get(normalized);
        if (previous != null)
        {
            previous.active = false;
            sessions.remove(previous.id);
        }
        Token token = new Token(normalized, ++generation);
        tokens.put(normalized, token);
        return token;
    }

    public void attach(Token token, T session)
    {
        if (!accepts(token))
        {
            throw new IllegalStateException("Expired connection generation");
        }
        sessions.put(token.id, session);
    }

    public boolean accepts(Token token)
    {
        return token != null && token.active && tokens.get(token.address) == token;
    }

    /**
     * Recheck at execution, not when the transport originally enqueues the callback.
     */
    public Runnable guard(Token token, Runnable callback)
    {
        return () ->
        {
            if (accepts(token))
            {
                callback.run();
            }
        };
    }

    public void close(Token token)
    {
        if (token != null)
        {
            token.active = false;
        }
    }

    public T get(String id)
    {
        return sessions.get(id);
    }

    public T latest(String address)
    {
        Token token = tokens.get(address.toUpperCase(Locale.ROOT));
        return token == null ? null : sessions.get(token.id);
    }

    public java.util.List<T> all()
    {
        return new java.util.ArrayList<>(sessions.values());
    }
}
