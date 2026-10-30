package todoc.cochlear.remoteapp.params;

import java.util.ArrayDeque;

/**
 * Bounded, connection-scoped absolute writes waiting behind telemetry traffic.
 */
public final class UserControlQueue
{
    public static final int CAPACITY = 8;
    public static final long MAX_AGE_MS = 10000;

    private static final class Entry
    {
        final byte[] packet;
        final long time;

        Entry(byte[] packet, long time)
        {
            this.packet = packet.clone();
            this.time = time;
        }
    }

    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    public boolean offer(byte[] packet, long now)
    {
        if (!isOrdinaryWrite(packet) || entries.size() >= CAPACITY)
        {
            return false;
        }
        entries.addLast(new Entry(packet, now));
        return true;
    }

    public boolean isEmpty()
    {
        return entries.isEmpty();
    }

    public void clear()
    {
        entries.clear();
    }

    public int expire(long now)
    {
        int count = 0;
        while (!entries.isEmpty() && now - entries.peekFirst().time > MAX_AGE_MS)
        {
            entries.removeFirst();
            count++;
        }
        return count;
    }

    public byte[] poll()
    {
        return entries.isEmpty() ? null : entries.removeFirst().packet;
    }

    public static boolean isOrdinaryWrite(byte[] packet)
    {
        if (packet == null || packet.length < 2)
        {
            return false;
        }
        switch (packet[0] & 255)
        {
            case 0x44:
            case 0x45:
            case 0x46:
            case 0x48:
            case 0x49:
                return packet.length == 2;
            case 0x59:
                if (packet.length != 5)
                {
                    return false;
                }
                // 게이팅은 도메인 구조가 아닌 구형 쓰기 형식이다. 주기 조회 뒤에 보낸다.
                if ((packet[1] & 255) == PacketInfo.RC_OPT_MUTE_WRITE)
                {
                    return packet[2] == PacketInfo.MUTE_SUB_OPT_NORMAL_MODE
                        && (packet[3] == PacketInfo.MUTE_ENABLE || packet[3] == PacketInfo.MUTE_DISABLE);
                }
                // 읽기·관리·맵 명령을 일반 제어 큐에 넣지 않는다. FF 독점은 호출자가 검사한다.
                return packet[2] == 1 && (packet[1] & 255) == 0x20;
            default:
                return false;
        }
    }
}
