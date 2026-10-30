package todoc.cochlear.remoteapp.ota;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import todoc.cochlear.remoteapp.connection.OtaProgress;

/**
 * Copies file/result data across a C2 reboot, without carrying any transport or mapping readiness.
 */
public final class OtaBootSnapshot
{
    public final boolean completed;
    public final long acknowledgedBytes, totalBytes, averageBytesPerSecond;
    public final String message;
    private final String folder;
    private final int selectedSlot, currentFile;
    private final List<Ota.OtaFile> files = new ArrayList<>();

    private OtaBootSnapshot(Ota source, OtaCoordinator coordinator, OtaBootSnapshot earlier)
    {
        boolean currentCompleted = coordinator.state() == OtaProgress.State.COMPLETE;
        boolean retainedCompleted = coordinator.state() == OtaProgress.State.IDLE && earlier != null && earlier.completed;
        completed = currentCompleted || retainedCompleted;
        acknowledgedBytes = currentCompleted ? coordinator.acknowledgedBytes() : retainedCompleted ? earlier.acknowledgedBytes : 0;
        totalBytes = currentCompleted ? coordinator.totalBytes() : retainedCompleted ? earlier.totalBytes : 0;
        averageBytesPerSecond = currentCompleted ? coordinator.averageBytesPerSecond() : retainedCompleted ? earlier.averageBytesPerSecond : 0;
        message = completed ? "4개 파일 업데이트 전송 완료" : "OTA 준비를 진행하세요.";
        folder = source.collectFolderName;
        selectedSlot = source.currentSlotNum;
        currentFile = source.currentFileNum;
        if (source.threadState == Ota.THREAD_STATE_IDLE && source.fileList != null)
        {
            for (Ota.OtaFile file : source.fileList)
            {
                files.add(copy(file));
            }
        }
    }

    public static OtaBootSnapshot capture(Ota source, OtaCoordinator coordinator, OtaBootSnapshot earlier)
    {
        return new OtaBootSnapshot(source, coordinator, earlier);
    }

    public void restoreFiles(Ota destination)
    {
        if (destination.threadState != Ota.THREAD_STATE_IDLE || destination.commState != Ota.COMM_STATE_IDLE)
        {
            throw new IllegalStateException("Restore only into a fresh, idle session");
        }
        destination.collectFolderName = folder;
        destination.currentSlotNum = selectedSlot;
        destination.currentFileNum = currentFile;
        destination.fileList = new ArrayList<>();
        for (Ota.OtaFile file : files)
        {
            destination.fileList.add(copy(file));
        }
    }

    private static Ota.OtaFile copy(Ota.OtaFile source)
    {
        Ota.OtaFile file = new Ota.OtaFile(source.slotNum, source.fileNum);
        file.collectSize = source.collectSize;
        file.collectPercent = source.collectPercent;
        file.writeSize = source.writeSize;
        file.writePercent = source.writePercent;
        file.totalBytes = source.totalBytes;
        file.buffer = source.buffer == null ? null : Arrays.copyOf(source.buffer, source.buffer.length);
        return file;
    }
}
