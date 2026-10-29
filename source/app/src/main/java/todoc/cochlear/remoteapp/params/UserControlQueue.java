package todoc.cochlear.remoteapp.params;

import java.util.ArrayDeque;

/** Bounded, connection-scoped absolute writes waiting behind telemetry traffic. */
public final class UserControlQueue {
    public static final int CAPACITY = 8;
    public static final long MAX_AGE_MS = 10000;
    private static final class Entry {
        final byte[] packet;
        final long time;
        Entry(byte[] packet, long time) { this.packet = packet.clone(); this.time = time; }
    }
    private final ArrayDeque<Entry> entries = new ArrayDeque<>();
    public boolean offer(byte[] packet, long now) {
        if (!isOrdinaryWrite(packet) || entries.size() >= CAPACITY) return false;
        entries.addLast(new Entry(packet, now));
        return true;
    }
    public boolean isEmpty() { return entries.isEmpty(); }
    public void clear() { entries.clear(); }
    public int expire(long now) {
        int count = 0;
        while (!entries.isEmpty() && now - entries.peekFirst().time > MAX_AGE_MS) {
            entries.removeFirst(); count++;
        }
        return count;
    }
    public byte[] poll() { return entries.isEmpty() ? null : entries.removeFirst().packet; }

    public static boolean isOrdinaryWrite(byte[] packet) {
        if (packet == null || packet.length < 2) return false;
        switch (packet[0] & 255) {
            case 0x44: case 0x45: case 0x46: case 0x48: case 0x49: return packet.length == 2;
            case 0x59:
                // Domain writes only. Reads, identity writes, FF ownership and destructive
                // map commands are managed elsewhere and must never be silently queued.
                return packet.length == 5 && packet[2] == 1 && (packet[1] & 255) == 0x20;
            default: return false;
        }
    }
}
