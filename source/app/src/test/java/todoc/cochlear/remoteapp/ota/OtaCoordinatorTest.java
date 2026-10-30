package todoc.cochlear.remoteapp.ota;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import todoc.cochlear.remoteapp.connection.OtaProgress;

import static org.junit.Assert.*;

public class OtaCoordinatorTest
{
    private static final class Device implements OtaCoordinator.Transport
    {
        final Ota ota = new Ota();
        long now;
        final OtaCoordinator coordinator = new OtaCoordinator(ota, this, () -> now);
        final List<byte[]> sent = new ArrayList<>();
        boolean available = true, rejectSend;

        Device()
        {
            ota.currentSlotNum = 1;
        }

        @Override
        public boolean ready()
        {
            return available;
        }

        @Override
        public boolean send(byte[] packet)
        {
            if (rejectSend)
            {
                return false;
            }
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

        void collect(int... lengths)
        {
            for (int i = 0; i < lengths.length; i++)
            {
                Ota.OtaFile file = ota.getFile(0, i + 1);
                file.totalBytes = lengths[i];
                file.collectSize = lengths[i];
                file.collectPercent = 100;
                file.buffer = new byte[lengths[i]];
                for (int j = 0; j < file.buffer.length; j++)
                {
                    file.buffer[j] = (byte) (i + j);
                }
            }
        }

        byte[] latest()
        {
            return sent.get(sent.size() - 1);
        }

        byte[] ackFor(byte[] request)
        {
            if (request.length == 1)
            {
                return new byte[]{0x60, 1};
            }
            if (request[1] == 0 && request[2] == 0 && request[3] == 0)
            {
                return new byte[]{(byte) 0xC3, 0, 0, 0, request[4], request[5], request[6], 1, 1};
            }
            return new byte[]{(byte) 0xC3, request[1], request[2], request[3], 1};
        }

        void ackLatest()
        {
            coordinator.receive(ackFor(latest()));
        }

        void readyAndStart()
        {
            assertTrue(coordinator.prepare());
            ackLatest();
            assertTrue(coordinator.start());
        }

        void finish()
        {
            int limit = 200000;
            while (coordinator.state() == OtaProgress.State.WRITING && --limit > 0)
            {
                ackLatest();
            }
            assertTrue(limit > 0);
        }
    }

    @Test
    public void speedCountsOnlyAcknowledgedPayloadAcrossFilesAndExpiresDuringStall()
    {
        Device d = new Device();
        d.collect(45, 10, 10, 10);
        d.readyAndStart();
        d.now = 100;
        d.ackLatest(); // Command ACK, first data packet has a four-byte prefix.
        assertEquals(0, d.coordinator.bytesPerSecond());
        d.now = 500;
        while (d.coordinator.currentFileNum() == 1)
        {
            d.ackLatest();
        }
        assertEquals(45, d.coordinator.acknowledgedBytes());
        assertEquals(90, d.coordinator.bytesPerSecond());
        d.now = 1500;
        assertEquals(0, d.coordinator.bytesPerSecond());
        d.finish();
        assertEquals(75, d.coordinator.acknowledgedBytes());
        assertEquals(50, d.coordinator.averageBytesPerSecond());
        assertEquals(0, d.coordinator.bytesPerSecond());
        d.now = 9000;
        assertEquals(50, d.coordinator.averageBytesPerSecond());
        assertTrue(d.coordinator.prepare());
        assertEquals(0, d.coordinator.averageBytesPerSecond());
    }

    @Test
    public void prepareRequiresExactPositiveAckAndNeverStartsDataAutomatically()
    {
        for (byte[] invalid : new byte[][]{{0x60, 0}, {0x60}, {0x60, 1, 0}, {(byte) 0xF0, 0x60}, {(byte) 0xFE, 0x60}})
        {
            Device d = new Device();
            d.collect(1, 1, 1, 1);
            assertTrue(d.coordinator.prepare());
            assertEquals(1, d.latest().length);
            assertFalse(d.coordinator.start());
            d.coordinator.receive(invalid);
            assertEquals(OtaProgress.State.FAILED, d.coordinator.state());
            assertEquals(1, d.sent.size());
            assertFalse(d.coordinator.canStart());
        }
        Device d = new Device();
        d.collect(1, 1, 1, 1);
        assertTrue(d.coordinator.prepare());
        d.ackLatest();
        assertEquals(OtaProgress.State.READY, d.coordinator.state());
        assertEquals(1, d.sent.size());
        assertEquals(0, d.coordinator.acknowledgedBytes());
    }

    @Test
    public void incompleteCollectionAndInvalidSlotCannotBeginOrChangeDenominator()
    {
        Device d = new Device();
        d.collect(45, 100, 20);
        assertTrue(d.coordinator.prepare());
        d.ackLatest();
        assertEquals(0, d.coordinator.collectedTotalBytes());
        assertFalse(d.coordinator.start());
        d.collect(45, 100, 20, 15);
        d.ota.currentSlotNum = 0;
        assertFalse(d.coordinator.start());
        assertEquals(0, d.coordinator.totalBytes());
        assertEquals(1, d.sent.size());
    }

    @Test
    public void aggregateProgressCountsAcknowledgedBytesAndFinishesOnlyAfterAllFourFiles()
    {
        Device d = new Device();
        d.collect(45, 205388, 4490, 5450);
        d.readyAndStart();
        assertEquals(215373, d.coordinator.totalBytes());
        d.ackLatest();
        assertEquals(0, d.coordinator.acknowledgedBytes()); // Command ACK sends first data, no bytes confirmed yet.
        while (d.coordinator.currentFileNum() == 1)
        {
            d.ackLatest();
        }
        assertEquals(45, d.coordinator.acknowledgedBytes());
        assertEquals(0, d.coordinator.percent());
        while (d.coordinator.currentFileNum() == 2)
        {
            d.ackLatest();
        }
        assertEquals(205433, d.coordinator.acknowledgedBytes());
        assertEquals(95, d.coordinator.percent());
        d.finish();
        assertEquals(OtaProgress.State.COMPLETE, d.coordinator.state());
        assertEquals(215373, d.coordinator.acknowledgedBytes());
        assertEquals(100, d.coordinator.percent());
        for (int i = 1; i <= 4; i++)
        {
            assertEquals(100, d.ota.getFile(0, i).writePercent);
        }
    }

    @Test
    public void finalDataBeingSentIsStillNinetyNineUntilItsAck()
    {
        Device d = new Device();
        d.collect(1, 1, 1, 1000);
        d.readyAndStart();
        while (!(d.coordinator.currentFileNum() == 4 && d.ota.commBufferIndex == 1000))
        {
            d.ackLatest();
        }
        assertTrue(d.coordinator.acknowledgedBytes() < d.coordinator.totalBytes());
        assertTrue(d.coordinator.percent() < 100);
        d.ackLatest();
        assertEquals(100, d.coordinator.percent());
    }

    @Test
    public void duplicateWrongIndexAndRejectionNeverAdvanceAcknowledgedBytes()
    {
        for (int kind = 0; kind < 3; kind++)
        {
            Device d = new Device();
            d.collect(50, 1, 1, 1);
            d.readyAndStart();
            d.ackLatest();
            byte[] first = d.ackFor(d.latest());
            d.coordinator.receive(first);
            long confirmed = d.coordinator.acknowledgedBytes();
            byte[] invalid = kind == 0 ? first : d.ackFor(d.latest());
            if (kind == 1)
            {
                invalid[3] = 99;
            }
            if (kind == 2)
            {
                invalid[4] = 0;
            }
            d.coordinator.receive(invalid);
            assertEquals(OtaProgress.State.FAILED, d.coordinator.state());
            assertEquals(confirmed, d.coordinator.acknowledgedBytes());
            assertTrue(d.coordinator.percent() < 100);
        }
    }

    @Test
    public void buffersAndTargetSlotRemainFrozenWhenAnotherSelectionChangesTheModel()
    {
        Device d = new Device();
        d.collect(17, 1, 1, 1);
        d.readyAndStart();
        byte original = d.ota.getFile(0, 1).buffer[0];
        d.ota.currentSlotNum = 2;
        d.ota.getFile(0, 1).buffer[0] = 99;
        d.ota.getFile(0, 1).totalBytes = 1;
        d.ackLatest();
        assertEquals(original, d.latest()[4]);
        assertEquals(20, d.coordinator.totalBytes());
        d.finish();
        assertEquals(1, d.ota.commSlotNum);
        assertEquals(20, d.coordinator.acknowledgedBytes());
    }

    @Test
    public void twoDevicesPrepareAndWriteIndependentlyAndCloseDoesNotStopTheOther()
    {
        Device a = new Device(), b = new Device();
        a.collect(17, 1, 1, 1);
        b.collect(5, 6, 7, 8);
        a.readyAndStart();
        b.readyAndStart();
        a.ackLatest();
        b.ackLatest();
        a.coordinator.disconnected();
        b.finish();
        assertEquals(OtaProgress.State.FAILED, a.coordinator.state());
        assertEquals(0, a.coordinator.acknowledgedBytes());
        assertEquals(OtaProgress.State.COMPLETE, b.coordinator.state());
        assertEquals(26, b.coordinator.acknowledgedBytes());
        assertFalse(a.coordinator.blocksOrdinaryTraffic());
        assertTrue(b.coordinator.blocksOrdinaryTraffic());
        b.coordinator.bootSelected();
        assertFalse(b.coordinator.blocksOrdinaryTraffic());
    }

    @Test
    public void sendFailureOrTimeoutHasBoundedFailureAndKeepsConfirmedProgress()
    {
        Device d = new Device();
        d.rejectSend = true;
        assertFalse(d.coordinator.prepare());
        assertEquals(OtaProgress.State.FAILED, d.coordinator.state());
        assertFalse(d.coordinator.waiting());
        Device e = new Device();
        e.collect(17, 1, 1, 1);
        e.readyAndStart();
        e.ackLatest();
        e.ackLatest();
        long confirmed = e.coordinator.acknowledgedBytes();
        e.coordinator.fail("timeout");
        assertFalse(e.coordinator.waiting());
        assertEquals(confirmed, e.coordinator.acknowledgedBytes());
        assertTrue(e.coordinator.blocksOrdinaryTraffic());
        e.coordinator.bootSelected();
        assertFalse(e.coordinator.blocksOrdinaryTraffic());
    }

    @Test
    public void lostPrepareAckDoesNotAssumeThatTheInternalLinkIsStillAttached()
    {
        Device d = new Device();
        assertTrue(d.coordinator.prepare());
        d.coordinator.fail("timeout");
        assertEquals(OtaProgress.State.FAILED, d.coordinator.state());
        assertTrue(d.coordinator.blocksOrdinaryTraffic());
        assertFalse(d.coordinator.canStart());
        assertTrue(d.coordinator.prepare());
        d.ackLatest();
        assertEquals(OtaProgress.State.READY, d.coordinator.state());
        d.coordinator.bootSelected();
        assertFalse(d.coordinator.blocksOrdinaryTraffic());
    }

    @Test
    public void lateFileAckCannotReleaseTheOutstandingPrepareRequest()
    {
        Device d = new Device();
        d.collect(17, 1, 1, 1);
        assertTrue(d.coordinator.prepare());
        for (byte[] late : new byte[][]{{(byte) 0xC3, 0, 0, 1, 1}, {(byte) 0xFE, (byte) 0xC3}})
        {
            assertFalse(d.coordinator.expectsResponse(late));
            d.coordinator.receive(late);
            assertEquals(OtaProgress.State.PREPARING, d.coordinator.state());
            assertTrue(d.coordinator.waiting());
        }
        assertEquals(1, d.sent.size());
        d.ackLatest();
        assertEquals(OtaProgress.State.READY, d.coordinator.state());
    }

    @Test
    public void latePrepareAckCannotReleaseTheOutstandingFileRequest()
    {
        Device d = new Device();
        d.collect(17, 1, 1, 1);
        d.readyAndStart();
        int sent = d.sent.size();
        for (byte[] late : new byte[][]{{0x60, 1}, {(byte) 0xF0, 0x60}})
        {
            assertFalse(d.coordinator.expectsResponse(late));
            d.coordinator.receive(late);
            assertEquals(OtaProgress.State.WRITING, d.coordinator.state());
            assertTrue(d.coordinator.waiting());
        }
        assertEquals(sent, d.sent.size());
        assertEquals(0, d.coordinator.acknowledgedBytes());
        d.ackLatest();
        assertEquals(sent + 1, d.sent.size());
    }

    @Test
    public void liveAvailabilityRecoversFromTemporaryBusyWithoutRecollectingOrReselecting()
    {
        Device d = new Device();
        d.available = false;
        assertFalse(d.coordinator.canPrepare());
        d.available = true;
        assertTrue(d.coordinator.canPrepare());
        assertEquals(0, d.sent.size());
        assertTrue(d.ota.fileList.isEmpty());
        d.collect(17, 1, 1, 1);
        assertTrue(d.coordinator.prepare());
        d.ackLatest();
        int files = d.ota.fileList.size(), sent = d.sent.size();
        d.available = false;
        assertFalse(d.coordinator.canStart());
        d.available = true;
        assertTrue(d.coordinator.canStart());
        assertEquals(OtaProgress.State.READY, d.coordinator.state());
        assertEquals(files, d.ota.fileList.size());
        assertEquals(sent, d.sent.size());
        assertEquals(0, d.coordinator.acknowledgedBytes());
    }

    @Test
    public void availabilityQueriesDoNotCreateMissingFilesOrChangePreparationState()
    {
        Device d = new Device();
        assertTrue(d.coordinator.prepare());
        d.ackLatest();
        for (int i = 0; i < 3; i++)
        {
            assertFalse(d.coordinator.canStart());
            assertEquals(0, d.coordinator.collectedTotalBytes());
            assertTrue(d.coordinator.canPrepare());
        }
        assertTrue(d.ota.fileList.isEmpty());
        assertEquals(1, d.sent.size());
        assertEquals(OtaProgress.State.READY, d.coordinator.state());
    }

    @Test
    public void rebootSnapshotKeepsCompletedFilesAndBytesButDoesNotRestoreMappingMode()
    {
        Device original = new Device();
        original.ota.currentSlotNum = 2;
        original.collect(17, 1, 1, 1);
        original.readyAndStart();
        original.finish();
        OtaBootSnapshot snapshot = OtaBootSnapshot.capture(original.ota, original.coordinator, null);
        original.coordinator.disconnected();
        Device reconnected = new Device();
        snapshot.restoreFiles(reconnected.ota);
        assertTrue(snapshot.completed);
        assertEquals(20, snapshot.acknowledgedBytes);
        assertEquals(20, snapshot.totalBytes);
        assertEquals(OtaProgress.State.IDLE, reconnected.coordinator.state());
        assertFalse(reconnected.coordinator.blocksOrdinaryTraffic());
        assertFalse(reconnected.coordinator.canStart());
        assertTrue(reconnected.coordinator.canPrepare());
        assertEquals(2, reconnected.ota.currentSlotNum);
        assertEquals(0, reconnected.sent.size());
        for (int number = 1; number <= 4; number++)
        {
            assertEquals(100, reconnected.ota.getFile(0, number).writePercent);
        }
        original.ota.getFile(0, 1).buffer[0] = 99;
        assertNotEquals(99, reconnected.ota.getFile(0, 1).buffer[0]);
    }

    @Test
    public void repeatedRebootKeepsLastCompletedSummaryWithoutAffectingAnotherDeviceTransfer()
    {
        Device a = new Device();
        a.collect(17, 1, 1, 1);
        a.readyAndStart();
        a.finish();
        OtaBootSnapshot first = OtaBootSnapshot.capture(a.ota, a.coordinator, null);
        Device replacement = new Device();
        first.restoreFiles(replacement.ota);
        Device b = new Device();
        b.collect(2, 3, 4, 5);
        b.readyAndStart();
        int sent = b.sent.size();
        OtaBootSnapshot second = OtaBootSnapshot.capture(replacement.ota, replacement.coordinator, first);
        Device afterFactoryBoot = new Device();
        second.restoreFiles(afterFactoryBoot.ota);
        assertTrue(second.completed);
        assertEquals(20, second.totalBytes);
        assertEquals(20, second.acknowledgedBytes);
        assertFalse(afterFactoryBoot.coordinator.blocksOrdinaryTraffic());
        assertEquals(0, afterFactoryBoot.sent.size());
        assertEquals(sent, b.sent.size());
        assertEquals(OtaProgress.State.WRITING, b.coordinator.state());
        b.finish();
        assertEquals(14, b.coordinator.acknowledgedBytes());
        assertEquals(100, b.coordinator.percent());
    }
}
