package todoc.cochlear.remoteapp.bluetooth;

// Bluetooth Low Energy Finite State Machine
public class BleFsm
{
    static public final int FSM_DISCONNECTED = 0;
    static public final int FSM_SEARCHING = 1;
    static public final int FSM_CONNECTING = 2;
    static public final int FSM_CONNECTED = 3;
    static public final int FSM_DISCONNECTING = 4;
}
