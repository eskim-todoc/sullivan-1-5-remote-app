package todoc.cochlear.remoteapp.connection;

/**
 * One device's immutable OTA presentation snapshot. Byte counts include acknowledged data only.
 */
public final class OtaProgress
{
    public enum State
    {IDLE, PREPARING, READY, WRITING, COMPLETE, FAILED}

    public final String sessionId, address, label, message;
    public final State state;
    public final long acknowledgedBytes, totalBytes, bytesPerSecond, averageBytesPerSecond;
    public final int percent, currentFileNum;
    public final boolean canPrepare, canStart, controlsPaused;

    public OtaProgress(String sessionId, String address, String label, State state, String message,
                       long acknowledgedBytes, long totalBytes, int percent, int currentFileNum,
                       boolean canPrepare, boolean canStart, boolean controlsPaused, long bytesPerSecond, long averageBytesPerSecond)
    {
        this.bytesPerSecond = bytesPerSecond;
        this.averageBytesPerSecond = averageBytesPerSecond;
        this.sessionId = sessionId;
        this.address = address;
        this.label = label;
        this.state = state;
        this.message = message;
        this.acknowledgedBytes = acknowledgedBytes;
        this.totalBytes = totalBytes;
        this.percent = percent;
        this.currentFileNum = currentFileNum;
        this.canPrepare = canPrepare;
        this.canStart = canStart;
        this.controlsPaused = controlsPaused;
    }
}
