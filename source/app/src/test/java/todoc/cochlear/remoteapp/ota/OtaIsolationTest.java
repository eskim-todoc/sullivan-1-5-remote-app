package todoc.cochlear.remoteapp.ota;

import org.junit.Test;

import static org.junit.Assert.*;

public class OtaIsolationTest
{
    @Test
    public void fileBuffersAndProgressBelongToTheirOwningSession()
    {
        Ota first = new Ota();
        Ota.OtaFile firstFile = first.getFile(Ota.SLOT_NUM_COLLECT, Ota.FILE_NUM_APP0);
        firstFile.buffer = new byte[]{1, 2, 3};
        firstFile.totalBytes = 3;
        firstFile.collectSize = 3;
        firstFile.collectPercent = 100;
        firstFile.writeSize = 1;
        firstFile.writePercent = 33;

        // Creating the next session must neither reset nor reuse the first file list.
        Ota second = new Ota();
        assertNotSame(first.fileList, second.fileList);
        assertEquals(1, first.fileList.size());
        assertTrue(second.fileList.isEmpty());

        Ota.OtaFile secondFile = second.getFile(Ota.SLOT_NUM_COLLECT, Ota.FILE_NUM_APP0);
        assertNotSame(firstFile, secondFile);
        assertNull(secondFile.buffer);
        assertEquals(0, secondFile.totalBytes);
        secondFile.buffer = new byte[]{4, 5};
        secondFile.buffer[0] = 9;
        secondFile.totalBytes = 2;
        secondFile.writeSize = 2;
        secondFile.writePercent = 100;
        second.fileList.clear();

        assertSame(firstFile, first.getFile(Ota.SLOT_NUM_COLLECT, Ota.FILE_NUM_APP0));
        assertArrayEquals(new byte[]{1, 2, 3}, firstFile.buffer);
        assertEquals(3, firstFile.totalBytes);
        assertEquals(3, firstFile.collectSize);
        assertEquals(100, firstFile.collectPercent);
        assertEquals(1, firstFile.writeSize);
        assertEquals(33, firstFile.writePercent);
    }

    @Test
    public void slotAndFileKeysDoNotAliasWithinOneSession()
    {
        Ota ota = new Ota();
        Ota.OtaFile collected = ota.getFile(Ota.SLOT_NUM_COLLECT, Ota.FILE_NUM_APP0);
        Ota.OtaFile slotOne = ota.getFile(Ota.SLOT_NUM_1, Ota.FILE_NUM_APP0);
        Ota.OtaFile manifest = ota.getFile(Ota.SLOT_NUM_1, Ota.FILE_NUM_MFST);

        assertSame(collected, ota.getFile(Ota.SLOT_NUM_COLLECT, Ota.FILE_NUM_APP0));
        assertSame(slotOne, ota.getFile(Ota.SLOT_NUM_1, Ota.FILE_NUM_APP0));
        assertNotSame(collected, slotOne);
        assertNotSame(slotOne, manifest);
        assertEquals(3, ota.fileList.size());
    }

    @Test
    public void transferStateAndPacketBufferRemainIndependent()
    {
        Ota first = new Ota();
        first.collectFolderName = "REL4.5";
        first.currentSlotNum = Ota.SLOT_NUM_1;
        first.currentFileNum = Ota.FILE_NUM_APP0;
        first.threadState = Ota.THREAD_STATE_BUSY;
        first.commState = Ota.COMM_STATE_WAIT_RESP_DATA;
        first.commDataIndex = 12;
        first.commSlotNum = Ota.SLOT_NUM_1;
        first.commFileNum = Ota.FILE_NUM_APP0;
        first.commTotalByte = 1234;
        first.commBufferIndex = 48;
        first.commBuffer = new byte[]{1, 2};

        Ota second = new Ota();
        second.collectFolderName = "REL4.6";
        second.currentSlotNum = Ota.SLOT_NUM_2;
        second.currentFileNum = Ota.FILE_NUM_APP1;
        second.threadState = Ota.THREAD_STATE_IDLE;
        second.commState = Ota.COMM_STATE_IDLE;
        second.commDataIndex = 0;
        second.commSlotNum = Ota.SLOT_NUM_2;
        second.commFileNum = Ota.FILE_NUM_APP1;
        second.commTotalByte = 500;
        second.commBufferIndex = 0;
        second.commBuffer = new byte[]{3, 4};
        second.commBuffer[0] = 9;

        assertEquals("REL4.5", first.collectFolderName);
        assertEquals(Ota.SLOT_NUM_1, first.currentSlotNum);
        assertEquals(Ota.FILE_NUM_APP0, first.currentFileNum);
        assertEquals(Ota.THREAD_STATE_BUSY, first.threadState);
        assertEquals(Ota.COMM_STATE_WAIT_RESP_DATA, first.commState);
        assertEquals(12, first.commDataIndex);
        assertEquals(Ota.SLOT_NUM_1, first.commSlotNum);
        assertEquals(Ota.FILE_NUM_APP0, first.commFileNum);
        assertEquals(1234, first.commTotalByte);
        assertEquals(48, first.commBufferIndex);
        assertArrayEquals(new byte[]{1, 2}, first.commBuffer);
    }

    @Test
    public void constructingSessionsPreservesTheLegacyShellInstance()
    {
        Ota shell = Ota.getInstance();
        Object shellFiles = shell.fileList;
        int shellFileCount = shell.fileList.size();

        Ota session = new Ota();
        session.getFile(Ota.SLOT_NUM_COLLECT, Ota.FILE_NUM_APP2).buffer = new byte[]{9};

        assertNotSame(shell, session);
        assertSame(shell, Ota.getInstance());
        assertSame(shellFiles, shell.fileList);
        assertNotSame(shell.fileList, session.fileList);
        assertEquals(shellFileCount, shell.fileList.size());
    }
}
