package todoc.cochlear.remoteapp.connection;

import java.util.Locale;

/**
 * One session's Android write completion barrier and post-indication settling interval.
 */
public final class GattWriteGate
{
    public static final long OTA_ACK_SETTLE_MS = 50;
    public static final long WRITE_COMPLETION_TIMEOUT_MS = 1000;
    public static final long TELEMETRY_WRITE_COMPLETION_TIMEOUT_MS = 5000;
    public static final long BOOT_INFO_WRITE_COMPLETION_TIMEOUT_MS = 5000;
    public static final long WAIT_FOR_COMPLETION_MS = 5;

    public static final class Request
    {
        public final int header, dataIndex;
        public final boolean ota;
        public final long submittedAtMs;
        private final int slot, file, option;
        private final byte[] requestIdentity;
        private final boolean bootInfo;
        private final boolean periodicTelemetry;
        private final boolean deferredDeviceAck;
        private boolean acknowledged;

        private Request(byte[] packet, boolean ota, long submittedAtMs)
        {
            header = packet[0] & 255;
            requestIdentity = packet;
            bootInfo = header == 0xC2 && packet.length == 3 && packet[1] == 1;
            periodicTelemetry = !ota && header == 0x59 && packet.length >= 4 && packet[2] == 0
                && (((packet[1] & 255) == 0x20 && packet[3] == 1)
                || ((packet[1] & 255) == 0x22 && (packet[3] == 1 || packet[3] == 2)));
            deferredDeviceAck = bootInfo || periodicTelemetry;
            dataIndex = packet.length >= 4 && header == 0xC3 ? ((packet[1] & 255) << 16) | ((packet[2] & 255) << 8) | (packet[3] & 255) : -1;
            slot = packet.length >= 8 ? packet[4] & 255 : -1;
            file = packet.length >= 8 ? ((packet[5] & 255) << 8) | (packet[6] & 255) : -1;
            option = packet.length >= 8 ? packet[7] & 255 : -1;
            this.ota = ota;
            this.submittedAtMs = submittedAtMs;
        }

        public String describe()
        {
            return String.format(Locale.ROOT, "0x%02X%s", header, dataIndex < 0 ? "" : " index=" + dataIndex);
        }

        public boolean isAcknowledged()
        {
            return acknowledged;
        }

        public boolean failedWithoutDeviceAck(int status)
        {
            return status != 0 && !acknowledged;
        }

        public boolean isBootInfoQuery()
        {
            return bootInfo;
        }

        /**
         * C2 Info와 Current PMIC·배터리 주기 조회만 Android 완료 시한과 장치 ACK 시한을 분리한다.
         */
        public boolean shouldDeferDeviceAckTimeout()
        {
            return deferredDeviceAck;
        }

        public boolean isCurrentRequest(byte[] packet, boolean transferBusy)
        {
            return transferBusy && packet == requestIdentity;
        }

        public boolean shouldStartDeviceAckTimeout(byte[] packet, boolean transferBusy, int status)
        {
            return deferredDeviceAck && status == 0 && !acknowledged && isCurrentRequest(packet, transferBusy);
        }

        public long completionTimeoutMs()
        {
            return bootInfo ? BOOT_INFO_WRITE_COMPLETION_TIMEOUT_MS
                : periodicTelemetry ? TELEMETRY_WRITE_COMPLETION_TIMEOUT_MS : WRITE_COMPLETION_TIMEOUT_MS;
        }

        private boolean matchesSuccessfulAck(byte[] response)
        {
            if (response == null || response.length == 0 || (response[0] & 255) != header)
            {
                return false;
            }
            if (header == 0x60)
            {
                return response.length == 2 && response[1] == 1;
            }
            if (header != 0xC3 || response.length < 5)
            {
                return false;
            }
            int index = ((response[1] & 255) << 16) | ((response[2] & 255) << 8) | (response[3] & 255);
            if (index != dataIndex)
            {
                return false;
            }
            if (index > 0)
            {
                return response[4] == 1;
            }
            return response.length >= 9 && (response[4] & 255) == slot
                && (((response[5] & 255) << 8) | (response[6] & 255)) == file
                && (response[7] & 255) == option && response[8] == 1;
        }
    }

    private Request pending, lastSubmitted;
    private long nextWriteAtMs;
    private boolean lastWriteCompleted, poisoned;
    private int lastWriteStatus;

    public long delayBeforeWrite(long nowMs)
    {
        return pending != null ? WAIT_FOR_COMPLETION_MS : Math.max(0, nextWriteAtMs - nowMs);
    }

    public void accepted(byte[] packet, boolean ota, long nowMs)
    {
        if (poisoned || pending != null)
        {
            throw new IllegalStateException("Android GATT write is pending or this connection is unusable");
        }
        pending = new Request(packet, ota, nowMs);
        lastSubmitted = pending;
        lastWriteCompleted = false;
        lastWriteStatus = -1;
    }

    public Request completed(int status)
    {
        if (poisoned)
        {
            return null;
        }
        Request result = pending;
        pending = null;
        if (result != null)
        {
            lastWriteCompleted = true;
            lastWriteStatus = status;
        }
        return result;
    }

    public void otaAcknowledged(byte[] response, long nowMs)
    {
        if (!poisoned && lastSubmitted != null && lastSubmitted.ota && lastSubmitted.matchesSuccessfulAck(response))
        {
            lastSubmitted.acknowledged = true;
            nextWriteAtMs = Math.max(nextWriteAtMs, nowMs + OTA_ACK_SETTLE_MS);
        }
    }

    public Request pending()
    {
        return pending;
    }

    public void abandonPending()
    {
        pending = null;
    }

    public void poison()
    {
        poisoned = true;
        pending = null;
    }

    public boolean isPoisoned()
    {
        return poisoned;
    }

    public boolean completionExpired(long nowMs)
    {
        return pending != null && nowMs - pending.submittedAtMs >= pending.completionTimeoutMs();
    }

    public String diagnostics(long nowMs)
    {
        return lastSubmitted == null ? "request=none" : "request=" + lastSubmitted.describe() + ", elapsed=" + (nowMs - lastSubmitted.submittedAtMs)
                                                        + "ms, androidWriteCompleted=" + lastWriteCompleted + ", status=" + lastWriteStatus + ", deviceAcknowledged=" + lastSubmitted.acknowledged;
    }

    public void reset()
    {
        pending = null;
        lastSubmitted = null;
        nextWriteAtMs = 0;
        lastWriteCompleted = false;
        lastWriteStatus = -1;
    }
}
