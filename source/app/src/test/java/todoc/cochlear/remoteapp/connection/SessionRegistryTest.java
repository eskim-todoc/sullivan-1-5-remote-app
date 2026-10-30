package todoc.cochlear.remoteapp.connection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import todoc.cochlear.remoteapp.params.UserControlQueue;

import static org.junit.Assert.*;

/**
 * Two fake transports exercise the same generation gate used by the real session callbacks.
 */
public class SessionRegistryTest
{
    private static final class FakeDevice
    {
        final SessionRegistry<FakeDevice> registry;
        final SessionRegistry.Token token;
        final UserControlQueue controls = new UserControlQueue();
        final List<byte[]> sent = new ArrayList<>();
        final boolean detailedBattery;
        boolean busy;
        int volume;

        FakeDevice(SessionRegistry<FakeDevice> registry, String address, boolean detailedBattery)
        {
            this.registry = registry;
            this.token = registry.open(address);
            this.detailedBattery = detailedBattery;
            registry.attach(token, this);
        }

        Runnable response(int nextVolume)
        {
            return registry.guard(token, () ->
            {
                volume = nextVolume;
                busy = false;
            });
        }

        void enqueue(int value)
        {
            controls.offer(new byte[]{0x49, (byte) value}, 0);
        }

        void drain()
        {
            if (!busy && registry.accepts(token) && !controls.isEmpty())
            {
                sent.add(controls.poll());
            }
        }

        void close()
        {
            registry.close(token);
            controls.clear();
        }
    }

    @Test
    public void selectingAnotherTargetDoesNotRetargetPreviouslyEnqueuedCommands()
    {
        SessionRegistry<FakeDevice> registry = new SessionRegistry<>();
        FakeDevice a = new FakeDevice(registry, "AA:00", false), b = new FakeDevice(registry, "BB:00", true);
        FakeDevice selected = a;
        selected.enqueue(3);
        selected = b;
        selected.enqueue(7);
        a.drain();
        b.drain();
        assertArrayEquals(new byte[]{0x49, 3}, a.sent.get(0));
        assertArrayEquals(new byte[]{0x49, 7}, b.sent.get(0));
        a.response(3).run();
        b.response(7).run();
        assertEquals(3, a.volume);
        assertEquals(7, b.volume);
        assertFalse(a.detailedBattery);
        assertTrue(b.detailedBattery);
    }

    @Test
    public void busyOrClosedAHasNoEffectOnBTelemetryAndQueue()
    {
        SessionRegistry<FakeDevice> registry = new SessionRegistry<>();
        FakeDevice a = new FakeDevice(registry, "AA:00", false), b = new FakeDevice(registry, "BB:00", true);
        a.busy = true;
        a.enqueue(2);
        b.enqueue(5);
        a.drain();
        b.drain();
        assertTrue(a.sent.isEmpty());
        assertEquals(1, b.sent.size());
        Runnable lateA = a.response(99);
        a.close();
        lateA.run();
        b.response(5).run();
        assertEquals(0, a.volume);
        assertEquals(5, b.volume);
        assertTrue(a.controls.isEmpty());
        assertTrue(registry.accepts(b.token));
    }

    @Test
    public void reconnectInvalidatesOldCallbacksEvenWhenAddressAndSerialAreIdentical()
    {
        SessionRegistry<FakeDevice> registry = new SessionRegistry<>();
        FakeDevice old = new FakeDevice(registry, "aa:00", false);
        Runnable stale = old.response(99);
        FakeDevice fresh = new FakeDevice(registry, "AA:00", true);
        stale.run();
        fresh.response(4).run();
        assertEquals(0, old.volume);
        assertEquals(4, fresh.volume);
        assertFalse(registry.accepts(old.token));
        assertTrue(registry.accepts(fresh.token));
        assertNotEquals(old.token.id, fresh.token.id);
        assertSame(fresh, registry.latest("aa:00"));
        assertNull(registry.get(old.token.id));
    }

    @Test
    public void closedGenerationCannotBeAttachedAgain()
    {
        SessionRegistry<FakeDevice> registry = new SessionRegistry<>();
        FakeDevice a = new FakeDevice(registry, "AA:00", false);
        a.close();
        try
        {
            registry.attach(a.token, a);
            fail("Closed generation must be rejected");
        }
        catch (IllegalStateException expected)
        {
            assertFalse(registry.accepts(a.token));
        }
    }
}
