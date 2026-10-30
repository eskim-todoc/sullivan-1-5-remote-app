package todoc.cochlear.remoteapp.connection;

/**
 * Immutable presentation row. Identity is the BLE address and connection generation, never its serial number.
 */
public final class ConnectionInfo
{
    public static final int STATE_DISCOVERED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTED = 3;
    public static final int STATE_FAILED = 4;

    public final String id, address, label;
    public final int state;
    public final boolean selected, registered;

    public ConnectionInfo(String id, String address, String label, int state,
                          boolean selected, boolean registered)
    {
        this.id = id;
        this.address = address;
        this.label = label;
        this.state = state;
        this.selected = selected;
        this.registered = registered;
    }
}
