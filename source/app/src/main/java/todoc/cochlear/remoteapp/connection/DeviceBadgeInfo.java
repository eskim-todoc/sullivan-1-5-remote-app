package todoc.cochlear.remoteapp.connection;

/**
 * 한 장치 배지의 읽기 전용 표시값. 알 수 없는 값은 빈 문자열로 전달한다.
 */
public final class DeviceBadgeInfo
{
    public final String userName, wearingPosition, serialNumber, releaseName;

    public DeviceBadgeInfo(String userName, String wearingPosition, String serialNumber, String releaseName)
    {
        this.userName = userName == null ? "" : userName;
        this.wearingPosition = wearingPosition == null ? "" : wearingPosition;
        this.serialNumber = serialNumber == null ? "" : serialNumber;
        this.releaseName = releaseName == null ? "" : releaseName;
    }
}
