package todoc.cochlear.remoteapp.connection;

/**
 * 한 OTA 장치 행의 읽기 전용 값. 아직 확인하지 못한 수치는 null이다.
 */
public final class OtaDeviceInfo
{
    public final String sessionId, address;
    public final DeviceBadgeInfo badge;
    public final int connectionState;
    public final Integer batteryPercent, currentBootSlot, lastBootSlot;

    public OtaDeviceInfo(String sessionId, String address, DeviceBadgeInfo badge, int connectionState,
                         Integer batteryPercent, Integer currentBootSlot, Integer lastBootSlot)
    {
        this.sessionId = sessionId;
        this.address = address;
        this.badge = badge;
        this.connectionState = connectionState;
        this.batteryPercent = batteryPercent;
        this.currentBootSlot = currentBootSlot;
        this.lastBootSlot = lastBootSlot;
    }
}
