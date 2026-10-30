package todoc.cochlear.remoteapp.connection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GattWriteGateTest
{
    @Test
    public void acceptedWriteMustCompleteBeforeAnotherRequestCanEnterAndroid()
    {
        GattWriteGate gate = new GattWriteGate();
        byte[] request = {(byte) 0xC3, 0, 4, (byte) 0x9C, 1};
        gate.accepted(request, true, 100);
        request[3] = 0;
        assertEquals(1180, gate.pending().dataIndex);
        assertTrue(gate.delayBeforeWrite(150) > 0);
        try
        {
            gate.accepted(new byte[]{0x60}, true, 150);
            fail("Concurrent write must be rejected");
        }
        catch (IllegalStateException expected)
        {
        }
        assertEquals(1180, gate.completed(0).dataIndex);
        assertEquals(0, gate.delayBeforeWrite(151));
    }

    @Test
    public void otaAckAddsSettlingTimeAndCompletionLossIsBounded()
    {
        GattWriteGate gate = new GattWriteGate();
        gate.accepted(new byte[]{(byte) 0xC3, 0, 0, 1}, true, 0);
        gate.otaAcknowledged(new byte[]{(byte) 0xC3, 0, 0, 1, 1}, 30);
        assertTrue(gate.delayBeforeWrite(35) > 0);
        gate.completed(0);
        assertEquals(45, gate.delayBeforeWrite(35));
        assertEquals(0, gate.delayBeforeWrite(80));
        gate.accepted(new byte[]{(byte) 0xC3, 0, 0, 2}, true, 80);
        assertFalse(gate.completionExpired(1079));
        assertTrue(gate.completionExpired(1080));
        assertTrue(gate.diagnostics(1080).contains("androidWriteCompleted=false"));
        gate.completed(133);
        assertTrue(gate.diagnostics(1080).contains("status=133"));
    }

    @Test
    public void mainLoopAckIsProcessedInlineRatherThanMovedBehindAnExpiredTimer()
    {
        List<Runnable> queue = new ArrayList<>();
        boolean[] waiting = {true}, failed = {false};
        // A transport callback was queued before its timeout, but UI work delayed both.
        Runnable timeout = () ->
        {
            if (waiting[0])
            {
                failed[0] = true;
            }
        };
        queue.add(() -> SessionCallbackDispatcher.dispatch(true, queue::add, () -> waiting[0] = false));
        queue.add(timeout);
        while (!queue.isEmpty())
        {
            queue.remove(0).run();
        }
        assertFalse(waiting[0]);
        assertFalse(failed[0]);
    }

    @Test
    public void backgroundCallbackPostsOnceAndStillChecksItsCapturedGeneration()
    {
        List<Runnable> queue = new ArrayList<>();
        SessionRegistry<String> registry = new SessionRegistry<>();
        SessionRegistry.Token token = registry.open("AA:00");
        registry.attach(token, "A");
        int[] calls = {0};
        SessionCallbackDispatcher.dispatch(false, queue::add, registry.guard(token, () -> calls[0]++));
        assertEquals(0, calls[0]);
        assertEquals(1, queue.size());
        registry.close(token);
        queue.remove(0).run();
        assertEquals(0, calls[0]);
    }

    @Test
    public void lateAndroidWriteFailureAfterDeviceAckDoesNotFailTheNextQueuedRequest()
    {
        GattWriteGate gate = new GattWriteGate();
        gate.accepted(new byte[]{(byte) 0xC3, 0, 0, 1, 7}, true, 0);
        gate.otaAcknowledged(new byte[]{(byte) 0xC3, 0, 0, 1, 1}, 30);
        GattWriteGate.Request completed = gate.completed(133);
        assertTrue(completed.isAcknowledged());
        assertFalse(completed.failedWithoutDeviceAck(133));
        gate.accepted(new byte[]{(byte) 0xC3, 0, 0, 2, 8}, true, 80);
        assertEquals(2, gate.pending().dataIndex);
        assertFalse(gate.pending().isAcknowledged());
    }

    @Test
    public void wrongOrRejectedDeviceAckDoesNotOverrideAndroidWriteFailure()
    {
        GattWriteGate gate = new GattWriteGate();
        gate.accepted(new byte[]{(byte) 0xC3, 0, 0, 1, 7}, true, 0);
        gate.otaAcknowledged(new byte[]{(byte) 0xC3, 0, 0, 2, 1}, 10);
        gate.otaAcknowledged(new byte[]{(byte) 0xC3, 0, 0, 1, 0}, 20);
        assertFalse(gate.pending().isAcknowledged());
        assertTrue(gate.completed(133).failedWithoutDeviceAck(133));
    }

    @Test
    public void completionTimeoutPoisonsOldGateAndLateCallbackCannotConsumeNewGenerationWrite()
    {
        SessionRegistry<GattWriteGate> registry = new SessionRegistry<>();
        SessionRegistry.Token old = registry.open("AA:00");
        GattWriteGate oldGate = new GattWriteGate();
        registry.attach(old, oldGate);
        oldGate.accepted(new byte[]{(byte) 0xC3, 0, 0, 1}, true, 0);
        Runnable late = registry.guard(old, () -> oldGate.completed(0));
        assertTrue(oldGate.completionExpired(1000));
        oldGate.poison();
        registry.close(old);
        try
        {
            oldGate.accepted(new byte[]{0x60}, true, 1001);
            fail("Poisoned connection cannot be reused");
        }
        catch (IllegalStateException expected)
        {
        }
        SessionRegistry.Token fresh = registry.open("AA:00");
        GattWriteGate freshGate = new GattWriteGate();
        registry.attach(fresh, freshGate);
        freshGate.accepted(new byte[]{0x60}, true, 1002);
        late.run();
        assertNotNull(freshGate.pending());
        assertEquals(0x60, freshGate.pending().header);
        assertFalse(freshGate.pending().isAcknowledged());
        assertNull(oldGate.completed(0));
    }

    @Test
    public void bootInfoMayCompleteAfterOneSecondBeforeItsDeviceAckDeadlineBegins()
    {
        GattWriteGate gate = new GattWriteGate();
        byte[] info = {(byte) 0xC2, 1, 0};
        gate.accepted(info, false, 0);
        assertTrue(gate.pending().isBootInfoQuery());
        assertFalse(gate.completionExpired(1200));
        GattWriteGate.Request completed = gate.completed(0);
        assertTrue(completed.shouldStartDeviceAckTimeout(info, true, 0));
        assertFalse(gate.completionExpired(5000));
        gate.accepted(info, false, 6000);
        assertFalse(gate.completionExpired(10999));
        assertTrue(gate.completionExpired(11000));
    }

    @Test
    public void bootInfoAckBeforeAndroidCompletionCannotArmAnOldDeviceAckTimer()
    {
        GattWriteGate gate = new GattWriteGate();
        byte[] info = {(byte) 0xC2, 1, 0};
        gate.accepted(info, false, 0);
        GattWriteGate.Request completed = gate.completed(0);
        assertFalse(completed.shouldStartDeviceAckTimeout(info, false, 0)); // ACK already made the transport idle.
        assertFalse(completed.shouldStartDeviceAckTimeout(new byte[]{0x59, 0x20, 0, 1}, true, 0)); // Next request is queued.
        assertFalse(completed.shouldStartDeviceAckTimeout(new byte[]{(byte) 0xC2, 1, 0}, true, 0)); // Same bytes, different request.
        assertFalse(completed.shouldStartDeviceAckTimeout(info, true, 133));
    }

    @Test
    public void bootSelectAndOtaKeepTheirOriginalOneSecondWriteCompletionLimit()
    {
        for (byte[] packet : new byte[][]{{(byte) 0xC2, 2, 2}, {(byte) 0xC3, 0, 0, 1, 7}, {0x60}})
        {
            GattWriteGate gate = new GattWriteGate();
            gate.accepted(packet, true, 0);
            assertFalse(gate.pending().isBootInfoQuery());
            assertFalse(gate.pending().shouldDeferDeviceAckTimeout());
            assertEquals(1000, gate.pending().completionTimeoutMs());
            assertTrue(gate.completionExpired(1000));
        }
    }

    @Test
    public void periodicTelemetryMayCompleteAfterFourAndHalfSecondsBeforeItsAckTimerStarts()
    {
        for (byte[] packet : new byte[][]{{0x59, 0x20, 0, 1}, {0x59, 0x22, 0, 1}, {0x59, 0x22, 0, 1, 0},
            {0x59, 0x22, 0, 2}, {0x59, 0x22, 0, 2, 0}})
        {
            GattWriteGate gate = new GattWriteGate();
            gate.accepted(packet, false, 0);
            assertEquals(5000, gate.pending().completionTimeoutMs());
            assertTrue(gate.pending().shouldDeferDeviceAckTimeout());
            assertFalse(gate.completionExpired(1000));
            assertFalse(gate.completionExpired(4000));
            assertFalse(gate.completionExpired(4500));
            assertTrue(gate.delayBeforeWrite(4500) > 0);
            GattWriteGate.Request completed = gate.completed(0);
            assertTrue(completed.shouldStartDeviceAckTimeout(packet, true, 0));
            assertFalse(gate.completionExpired(5500));
            assertEquals(0, gate.delayBeforeWrite(4501));
        }
    }

    @Test
    public void telemetryAckBeforeDelayedCompletionCannotArmOrFailTheNextQueuedRequest()
    {
        byte[] first = {0x59, 0x22, 0, 2};
        byte[] next = {0x59, 0x22, 0, 2};
        GattWriteGate gate = new GattWriteGate();
        gate.accepted(first, false, 0);
        assertFalse(gate.completionExpired(4500));
        GattWriteGate.Request completed = gate.completed(0);
        assertFalse(completed.shouldStartDeviceAckTimeout(first, false, 0)); // ACK가 먼저 와서 이미 idle이다.
        assertFalse(completed.shouldStartDeviceAckTimeout(next, true, 0)); // 같은 내용이어도 다음 요청이다.
        assertFalse(completed.isCurrentRequest(next, true));
        assertFalse(completed.shouldStartDeviceAckTimeout(first, true, 133));
        gate.accepted(next, false, 4501);
        assertTrue(gate.pending().isCurrentRequest(next, true));
        assertFalse(gate.pending().isCurrentRequest(first, true));
        assertFalse(completed.shouldStartDeviceAckTimeout(next, true, 0));
    }

    @Test
    public void missingTelemetryCompletionExpiresAtFiveSecondsWithoutAffectingAnotherSession()
    {
        SessionRegistry<GattWriteGate> registry = new SessionRegistry<>();
        SessionRegistry.Token oldA = registry.open("AA:00");
        SessionRegistry.Token b = registry.open("BB:00");
        GattWriteGate gateA = new GattWriteGate();
        GattWriteGate gateB = new GattWriteGate();
        registry.attach(oldA, gateA);
        registry.attach(b, gateB);
        byte[] packetA = {0x59, 0x20, 0, 1};
        byte[] packetB = {0x59, 0x22, 0, 2};
        gateA.accepted(packetA, false, 0);
        gateB.accepted(packetB, false, 2000);
        Runnable lateA = registry.guard(oldA, () -> gateA.completed(0));
        assertFalse(gateA.completionExpired(4999));
        assertTrue(gateA.completionExpired(5000));
        assertFalse(gateB.completionExpired(5000));
        gateA.poison();
        registry.close(oldA);
        SessionRegistry.Token freshA = registry.open("AA:00");
        GattWriteGate replacement = new GattWriteGate();
        registry.attach(freshA, replacement);
        replacement.accepted(packetA.clone(), false, 5001);
        lateA.run();
        assertNull(gateA.completed(0));
        assertNotNull(replacement.pending());
        assertNotNull(gateB.pending());
        assertTrue(registry.accepts(b));
        assertFalse(gateB.completionExpired(6500));
        assertTrue(gateB.completed(0).shouldStartDeviceAckTimeout(packetB, true, 0));
        assertNotNull(replacement.pending());
    }

    @Test
    public void initializationOtherReadsWritesAndOtaKeepTheirLegacyOneSecondCompletionDeadline()
    {
        for (byte[] packet : new byte[][]{{0x40, 0, 0, 0, 0}, {0x41}, {0x43}, {0x52, 1},
            {0x59, 0x20, 1, 1}, {0x59, 0x22, 1, 1}, {0x59, 0x22, 1, 2}, {0x59, 0x20, 0, 2}, {0x59, 0x22, 0, 3},
            {0x59, 0x21, 0, 1}, {0x59, 0x20, 0}, {(byte) 0xC2, 2, 2}, {(byte) 0xC3, 0, 0, 1, 7}, {0x60}})
        {
            GattWriteGate gate = new GattWriteGate();
            gate.accepted(packet, false, 0);
            assertEquals(1000, gate.pending().completionTimeoutMs());
            assertFalse(gate.pending().shouldDeferDeviceAckTimeout());
            assertFalse(gate.pending().shouldStartDeviceAckTimeout(packet, true, 0));
            assertFalse(gate.completionExpired(999));
            assertTrue(gate.completionExpired(1000));
        }
    }
}
