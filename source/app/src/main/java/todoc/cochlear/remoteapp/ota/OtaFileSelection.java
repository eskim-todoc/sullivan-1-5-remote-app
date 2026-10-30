package todoc.cochlear.remoteapp.ota;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 공통 이미지 선택을 대상 장치의 수집 버퍼로 복사한다. 슬롯과 통신 상태는 바꾸지 않는다.
 */
public final class OtaFileSelection
{
    private static final int[] FILES = {Ota.FILE_NUM_MFST, Ota.FILE_NUM_APP0, Ota.FILE_NUM_APP1, Ota.FILE_NUM_APP2};

    private OtaFileSelection()
    {
    }

    public static boolean isComplete(Ota source)
    {
        if (source == null || source.threadState != Ota.THREAD_STATE_IDLE
            || source.commState != Ota.COMM_STATE_IDLE)
        {
            return false;
        }
        for (int number : FILES)
        {
            if (completeFile(source, number) == null)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 실패하면 대상을 수정하지 않는다. 대상의 FF·관리·OTA 독점 상태는 호출자가 넘긴다.
     */
    public static boolean copyInto(Ota source, Ota destination, boolean destinationBusy)
    {
        if (destination == null || source == destination || destinationBusy
            || destination.threadState != Ota.THREAD_STATE_IDLE
            || destination.commState != Ota.COMM_STATE_IDLE || !isComplete(source))
        {
            return false;
        }
        List<Ota.OtaFile> copied = new ArrayList<>();
        for (int number : FILES)
        {
            Ota.OtaFile original = completeFile(source, number);
            Ota.OtaFile file = new Ota.OtaFile(Ota.SLOT_NUM_COLLECT, number);
            file.totalBytes = original.totalBytes;
            file.collectSize = original.totalBytes;
            file.collectPercent = 100;
            file.buffer = Arrays.copyOf(original.buffer, original.totalBytes);
            copied.add(file);
        }
        destination.fileList = new ArrayList<>(copied);
        destination.collectFolderName = source.collectFolderName;
        return true;
    }

    private static Ota.OtaFile completeFile(Ota source, int number)
    {
        if (source.fileList == null)
        {
            return null;
        }
        Ota.OtaFile found = null;
        for (Ota.OtaFile file : source.fileList)
        {
            if (file != null && file.slotNum == Ota.SLOT_NUM_COLLECT && file.fileNum == number)
            {
                if (found != null)
                {
                    return null;
                }
                found = file;
            }
        }
        return found != null && found.totalBytes > 0 && found.collectPercent == 100
            && found.collectSize == found.totalBytes && found.buffer != null
            && found.buffer.length >= found.totalBytes ? found : null;
    }
}
