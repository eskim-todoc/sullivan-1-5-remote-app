package todoc.cochlear.remoteapp.connection;

import org.junit.Test;

import static org.junit.Assert.*;

public class BootReconnectPolicyTest
{
    @Test
    public void threeFreshAttemptsAreBoundedAndUseBootSpecificBackoff()
    {
        BootReconnectPolicy policy = new BootReconnectPolicy();
        assertEquals(7000, policy.nextDelayMs());
        assertEquals(5000, policy.nextDelayMs());
        assertEquals(5000, policy.nextDelayMs());
        assertEquals(-1, policy.nextDelayMs());
        assertEquals(-1, policy.nextDelayMs());
        assertEquals(3, policy.attempts());
        assertFalse(policy.active());
    }

    @Test
    public void successfulInitializationOrUserDisconnectStopsFurtherAttempts()
    {
        BootReconnectPolicy ready = new BootReconnectPolicy();
        ready.nextDelayMs();
        ready.complete();
        assertTrue(ready.succeeded());
        assertFalse(ready.active());
        assertEquals(-1, ready.nextDelayMs());
        BootReconnectPolicy cancelled = new BootReconnectPolicy();
        cancelled.nextDelayMs();
        cancelled.cancel();
        assertFalse(cancelled.active());
        assertEquals(-1, cancelled.nextDelayMs());
        assertFalse(cancelled.succeeded());
    }

    @Test
    public void failedAReplacesOnlyItsGenerationAndNeverConsumesOtherDeviceCallbacks()
    {
        SessionRegistry<String> sessions = new SessionRegistry<>();
        BootReconnectPolicy policy = new BootReconnectPolicy();
        SessionRegistry.Token a = sessions.open("AA:00");
        sessions.attach(a, "A first");
        SessionRegistry.Token b = sessions.open("BB:00");
        sessions.attach(b, "B");
        String selected = b.id;
        Runnable oldA = sessions.guard(a, () -> fail("Old A callback must be invalid"));
        policy.nextDelayMs();
        sessions.close(a);
        SessionRegistry.Token retry = sessions.open("AA:00");
        sessions.attach(retry, "A retry");
        oldA.run();
        assertFalse(sessions.accepts(a));
        assertTrue(sessions.accepts(retry));
        assertTrue(sessions.accepts(b));
        assertEquals(b.id, selected);
        assertEquals("B", sessions.get(selected));
    }
}
