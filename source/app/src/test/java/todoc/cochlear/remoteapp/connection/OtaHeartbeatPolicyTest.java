package todoc.cochlear.remoteapp.connection;

import org.junit.Test;

import static org.junit.Assert.*;

public class OtaHeartbeatPolicyTest
{
    @Test
    public void onlyConfirmedReadyAndCompletePermitHeartbeat()
    {
        for (OtaProgress.State state : OtaProgress.State.values())
        {
            OtaHeartbeatPolicy policy = new OtaHeartbeatPolicy();
            policy.synchronize(state, true, true, 0);
            assertEquals(state == OtaProgress.State.READY || state == OtaProgress.State.COMPLETE,
                policy.claim(30000, true));
        }
        OtaHeartbeatPolicy policy = new OtaHeartbeatPolicy();
        policy.synchronize(OtaProgress.State.READY, false, true, 0);
        assertFalse(policy.claim(30000, true));
        policy.synchronize(OtaProgress.State.COMPLETE, true, false, 0);
        assertFalse(policy.claim(30000, true));
    }

    @Test
    public void busyQueueYieldsWithoutLosingDueRequestOrSendingMoreThanOncePerPeriod()
    {
        OtaHeartbeatPolicy policy = new OtaHeartbeatPolicy();
        policy.synchronize(OtaProgress.State.READY, true, true, 100);
        assertFalse(policy.claim(30099, true));
        assertFalse(policy.claim(30100, false));
        assertEquals(0, policy.nextDelayMs(31100));
        assertTrue(policy.claim(31100, true));
        assertFalse(policy.claim(31100, true));
        assertEquals(30000, policy.nextDelayMs(31100));
        policy.synchronize(OtaProgress.State.WRITING, true, true, 32000);
        assertFalse(policy.claim(100000, true));
        assertEquals(-1, policy.nextDelayMs(100000));
    }

    @Test
    public void repeatedUiPublicationsCannotPostponeDeadlineAndBootDisarmsIt()
    {
        OtaHeartbeatPolicy policy = new OtaHeartbeatPolicy();
        policy.synchronize(OtaProgress.State.READY, true, true, 0);
        for (int seconds = 1; seconds <= 29; seconds++)
        {
            policy.synchronize(OtaProgress.State.READY, true, true, seconds * 1000L);
        }
        assertTrue(policy.claim(30000, true));
        policy.synchronize(OtaProgress.State.COMPLETE, false, true, 31000);
        assertEquals(-1, policy.nextDelayMs(100000));
        assertFalse(policy.claim(100000, true));
        policy.synchronize(OtaProgress.State.READY, true, true, 100000);
        assertEquals(30000, policy.nextDelayMs(100000));
    }

    @Test
    public void delayedCallbackAfterCloseCannotSendAndOtherDeviceKeepsItsOwnSchedule()
    {
        SessionRegistry<OtaHeartbeatPolicy> sessions = new SessionRegistry<>();
        SessionRegistry.Token a = sessions.open("AA:00"), b = sessions.open("BB:00");
        OtaHeartbeatPolicy first = new OtaHeartbeatPolicy(), second = new OtaHeartbeatPolicy();
        sessions.attach(a, first);
        sessions.attach(b, second);
        first.synchronize(OtaProgress.State.READY, true, true, 0);
        second.synchronize(OtaProgress.State.COMPLETE, true, true, 1000);
        Runnable stale = sessions.guard(a, () -> fail("Closed generation cannot send a heartbeat"));
        sessions.close(a);
        sessions.open("AA:00");
        stale.run();
        assertTrue(sessions.accepts(b));
        assertFalse(second.claim(30000, true));
        assertTrue(second.claim(31000, true));
    }

    @Test
    public void timedOutInfoCannotAttributeLaterOptionlessErrorToAnotherBootRequest()
    {
        OtaHeartbeatPolicy policy = new OtaHeartbeatPolicy();
        byte[] error = {(byte) 0xF0, (byte) 0xC2, 1};
        assertFalse(policy.isAmbiguousBootError(error));
        policy.responseTimedOut();
        assertTrue(policy.isAmbiguousBootError(error));
        assertTrue(policy.isAmbiguousBootError(new byte[]{(byte) 0xFE, (byte) 0xC2, 1}));
        assertFalse(policy.isAmbiguousBootError(new byte[]{(byte) 0xC2, 2, 1})); // 정상 Select ACK는 처리한다.
        assertFalse(policy.isAmbiguousBootError(new byte[]{(byte) 0xF0, (byte) 0xC3, 1}));
        assertFalse(new OtaHeartbeatPolicy().isAmbiguousBootError(error)); // 새 GATT 세대에는 전파하지 않는다.
    }
}
