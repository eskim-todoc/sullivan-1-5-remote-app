package todoc.cochlear.remoteapp.ota;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import todoc.cochlear.remoteapp.connection.OtaProgress;
import todoc.cochlear.remoteapp.connection.SessionUserProfile;
import todoc.cochlear.remoteapp.database.users.EntityUser;

import static org.junit.Assert.*;

public class OtaFileSelectionTest
{
    private static Ota images(int... sizes)
    {
        Ota ota = new Ota();
        ota.collectFolderName = "chosen-images";
        for (int i = 0; i < sizes.length; i++)
        {
            Ota.OtaFile file = ota.getFile(Ota.SLOT_NUM_COLLECT, i + 1);
            file.totalBytes = file.collectSize = sizes[i];
            file.collectPercent = 100;
            file.buffer = new byte[sizes[i]];
            Arrays.fill(file.buffer, (byte) (i + 1));
        }
        return ota;
    }

    @Test
    public void completeSelectionCopiesIndependentBuffersWithoutChangingTargetSlotOrTransport()
    {
        Ota source = images(1, 4, 2, 1);
        Ota a = new Ota();
        Ota b = new Ota();
        a.currentSlotNum = 2;
        a.currentFileNum = 3;
        a.commBuffer = new byte[]{77};
        assertTrue(OtaFileSelection.copyInto(source, a, false));
        assertTrue(OtaFileSelection.copyInto(source, b, false));
        source.getFile(0, 2).buffer[0] = 9;
        a.getFile(0, 2).buffer[1] = 8;
        assertArrayEquals(new byte[]{2, 2, 2, 2}, b.getFile(0, 2).buffer);
        assertEquals(2, a.getFile(0, 2).buffer[0]);
        assertEquals(2, a.currentSlotNum);
        assertEquals(3, a.currentFileNum);
        assertArrayEquals(new byte[]{77}, a.commBuffer);
        assertEquals(Ota.COMM_STATE_IDLE, a.commState);
        assertEquals("chosen-images", a.collectFolderName);
        assertEquals(0, a.getFile(0, 2).writeSize);
    }

    @Test
    public void incompleteReadingMalformedAndBusySourcesLeaveExistingTargetUntouched()
    {
        List<Ota> invalid = new ArrayList<>();
        invalid.add(images(1, 4, 2));
        Ota reading = images(1, 4, 2, 1);
        reading.threadState = Ota.THREAD_STATE_BUSY;
        invalid.add(reading);
        Ota truncated = images(1, 4, 2, 1);
        truncated.getFile(0, 2).buffer = new byte[2];
        invalid.add(truncated);
        Ota duplicate = images(1, 4, 2, 1);
        duplicate.fileList.add(duplicate.fileList.get(0));
        invalid.add(duplicate);
        Ota target = images(3, 3, 3, 3);
        List<Ota.OtaFile> original = target.fileList;
        for (Ota source : invalid)
        {
            assertFalse(OtaFileSelection.isComplete(source));
            assertFalse(OtaFileSelection.copyInto(source, target, false));
            assertSame(original, target.fileList);
        }
        assertFalse(OtaFileSelection.copyInto(images(1, 4, 2, 1), target, true));
        assertSame(original, target.fileList);
        target.commState = Ota.COMM_STATE_WAIT_RESP_DATA;
        assertFalse(OtaFileSelection.copyInto(images(1, 4, 2, 1), target, false));
        assertSame(original, target.fileList);
    }

    private static final class Device implements OtaCoordinator.Transport
    {
        final Ota ota = new Ota();
        final OtaCoordinator coordinator = new OtaCoordinator(ota, this);
        final List<byte[]> sent = new ArrayList<>();

        @Override
        public boolean ready()
        {
            return true;
        }

        @Override
        public boolean send(byte[] packet)
        {
            sent.add(packet.clone());
            return true;
        }

        @Override
        public void changed()
        {
        }

        @Override
        public void log(String message)
        {
        }

        void acknowledge()
        {
            byte[] packet = sent.get(sent.size() - 1);
            if (packet.length == 1)
            {
                coordinator.receive(new byte[]{0x60, 1});
            }
            else if (packet[1] == 0 && packet[2] == 0 && packet[3] == 0)
            {
                coordinator.receive(new byte[]{(byte) 0xC3, 0, 0, 0, packet[4], packet[5], packet[6], 1, 1});
            }
            else
            {
                coordinator.receive(new byte[]{(byte) 0xC3, packet[1], packet[2], packet[3], 1});
            }
        }
    }

    @Test
    public void copyingFilesKeepsPreparationSeparateAndDoesNotSendOrSelectASlot()
    {
        Device device = new Device();
        assertTrue(device.coordinator.prepare());
        device.acknowledge();
        assertTrue(OtaFileSelection.copyInto(images(1, 4, 2, 1), device.ota, device.coordinator.busy()));
        assertEquals(OtaProgress.State.READY, device.coordinator.state());
        assertEquals(1, device.sent.size());
        assertFalse(device.coordinator.canStart());
        device.ota.currentSlotNum = 2;
        assertTrue(device.coordinator.canStart());
    }

    @Test
    public void changingSharedImagesOrStartingAnotherDeviceCannotAlterInFlightManifest()
    {
        Ota shared = images(1, 4, 2, 1);
        Device a = new Device();
        Device b = new Device();
        assertTrue(OtaFileSelection.copyInto(shared, a.ota, false));
        a.ota.currentSlotNum = 2;
        assertTrue(a.coordinator.prepare());
        a.acknowledge();
        assertTrue(a.coordinator.start());
        assertEquals(8, a.coordinator.totalBytes());

        shared.fileList = images(3, 7, 5, 2).fileList;
        assertFalse(OtaFileSelection.copyInto(shared, a.ota, a.coordinator.busy()));
        assertTrue(OtaFileSelection.copyInto(shared, b.ota, false));
        assertEquals(17, b.coordinator.collectedTotalBytes());
        assertEquals(8, a.coordinator.totalBytes());
        a.acknowledge();
        assertEquals(1, a.sent.get(a.sent.size() - 1)[4]);
        for (int limit = 0; limit < 100 && a.coordinator.state() == OtaProgress.State.WRITING; limit++)
        {
            a.acknowledge();
        }
        assertEquals(OtaProgress.State.COMPLETE, a.coordinator.state());
        assertEquals(8, a.coordinator.acknowledgedBytes());
        assertEquals(100, a.coordinator.percent());
        assertEquals(OtaProgress.State.IDLE, b.coordinator.state());
    }

    @Test
    public void editingTheRegisteredIdentityKeepsTheCurrentAuthenticationAndOtaTransfer()
    {
        EntityUser original = new EntityUser();
        original.name = "BEFORE_L";
        original.ear = "L";
        original.passKey = "original-auth";
        SessionUserProfile profile = new SessionUserProfile(original);
        Device device = new Device();
        assertTrue(OtaFileSelection.copyInto(images(1, 4, 2, 1), device.ota, false));
        device.ota.currentSlotNum = 2;
        assertTrue(device.coordinator.prepare());
        device.acknowledge();
        assertTrue(device.coordinator.start());
        byte[] transportBuffer = device.ota.commBuffer;
        int packets = device.sent.size();

        EntityUser edited = new EntityUser();
        edited.name = "AFTER_L";
        edited.ear = "L";
        edited.nickname = "new alias";
        edited.passKey = "future-auth";
        assertTrue(profile.canUpdate(edited, device.coordinator.busy()));
        profile.updateRegistration(edited);

        assertEquals("original-auth", profile.authenticationPassKey());
        assertEquals("AFTER_L", profile.registrationSnapshot().name);
        assertEquals("future-auth", profile.registrationSnapshot().passKey);
        assertSame(transportBuffer, device.ota.commBuffer);
        assertEquals(packets, device.sent.size());
        assertEquals(8, device.coordinator.totalBytes());
        assertEquals(OtaProgress.State.WRITING, device.coordinator.state());
        for (int limit = 0; limit < 100 && device.coordinator.state() == OtaProgress.State.WRITING; limit++)
        {
            device.acknowledge();
        }
        assertEquals(OtaProgress.State.COMPLETE, device.coordinator.state());
        assertEquals(8, device.coordinator.acknowledgedBytes());
        assertEquals(100, device.coordinator.percent());
    }
}
