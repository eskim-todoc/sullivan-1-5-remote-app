package todoc.cochlear.remoteapp.ff;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class Ff3SessionTest
{
    static Ff3Config preset()
    {
        Ff3Config c = new Ff3Config();
        int[] scalar = {1, 32, 25, 6, 12, 2, 6, 24, 4, 1, 20, 32, 3, 3, 4, 6, 1, 4, 1, 1, 1};
        for (int i = 0; i < 21; i++)
        {
            c.set(40 + i, scalar[i]);
        }
        int[] lower = {0, 40, 46, 55, 65, 80}, hold = {4, 4, 4, 5, 6, 8};
        for (int i = 0; i < 16; i++)
        {
            c.set(64 + i, i < 6 ? lower[i] : 0);
            c.set(80 + i, i > 0 && i < 6 ? 4 : 0);
            c.set(96 + i, i < 6 ? hold[i] : 0);
        }
        return c;
    }

    static class Device implements Ff3Session.Transport
    {
        final Ff3Session s = new Ff3Session(this);
        final List<byte[]> sent = new ArrayList<>();
        final ArrayDeque<Runnable> timers = new ArrayDeque<>(), polls = new ArrayDeque<>();
        Ff3Config current = preset(), staging;
        int mode = 3, schema = 3, status = 0x19, cursor;
        boolean corruptFile, pad = true;

        public boolean ready()
        {
            return true;
        }

        public void changed()
        {
        }

        public void later(Runnable r, long ms)
        {
            if (ms == 100)
            {
                polls.add(r);
            }
            else
            {
                timers.add(r);
            }
        }

        public void send(byte[] p)
        {
            sent.add(p);
        }

        void respond()
        {
            byte[] req = sent.get(cursor++);
            int index = req[3] & 255, access = req[2] & 255;
            byte[] payload;
            if (access == 1)
            {
                int v = req[4] & 255;
                if (index == 17)
                {
                    mode = v;
                }
                else if (index == 37)
                {
                    if (v == 1)
                    {
                        if (staging == null)
                        {
                            staging = current.copy();
                        }
                        status |= 4;
                    }
                    if (v == 3)
                    {
                        current = staging.copy();
                        staging = null;
                        status &= ~6;
                    }
                    if (v == 4)
                    {
                        staging = null;
                        status &= ~6;
                    }
                }
                else
                {
                    staging.set(index, v);
                    status |= 2;
                }
                payload = new byte[]{(byte) v};
            }
            else if (index == 38)
            {
                payload = new byte[]{(byte) schema};
            }
            else if (index == 17)
            {
                payload = new byte[]{(byte) mode};
            }
            else if (index == 39)
            {
                payload = new byte[]{(byte) status};
            }
            else if (index == 116)
            {
                payload = new byte[]{3, (byte) 0x80, 0, 0, 1, (byte) 0xFE, 0, 0, 1, (byte) (corruptFile ? 255 : 0)};
            }
            else if (index >= 112 && index <= 118)
            {
                payload = new byte[15];
            }
            else
            {
                payload = new byte[]{(byte) (staging != null ? staging : current).get(index)};
            }
            byte[] result = new byte[pad ? 20 : 5 + payload.length];
            result[0] = 0x59;
            result[1] = 0x20;
            result[2] = (byte) access;
            result[3] = (byte) index;
            result[4] = (byte) payload.length;
            System.arraycopy(payload, 0, result, 5, payload.length);
            s.receive(result);
        }

        void drain()
        {
            int limit = 500;
            while (cursor < sent.size() && --limit > 0)
            {
                respond();
            }
            assertTrue(limit > 0);
        }

        void load()
        {
            s.load();
            drain();
        }

        List<Integer> writes()
        {
            List<Integer> a = new ArrayList<>();
            for (byte[] p : sent)
            {
                if (p[2] == 1)
                {
                    a.add(p[3] & 255);
                }
            }
            return a;
        }
    }

    @Test
    public void summaryDoesNotFetchSettingsOrUnlockControls()
    {
        Device d = new Device();
        d.s.loadSummary();
        d.drain();
        assertEquals(3, d.sent.size());
        assertEquals(3, d.s.mode);
        assertEquals(0x19, d.s.status);
        assertFalse(d.s.settingsReady());
        assertFalse(d.s.config().complete());
        d.s.begin();
        d.s.changeMode(0);
        d.s.readDiagnostics();
        assertEquals(3, d.sent.size());
        assertTrue(d.writes().isEmpty());
        d.s.load();
        d.respond();
        assertFalse(d.s.settingsReady());
        d.drain();
        assertTrue(d.s.settingsReady());
        assertTrue(d.writes().isEmpty());
        d.s.reset();
        assertFalse(d.s.settingsReady());
    }

    @Test
    public void interruptedSettingsReadNeverUnlocksControls()
    {
        Device d = new Device();
        d.s.loadSummary();
        d.drain();
        d.s.load();
        d.respond();
        d.s.receive(new byte[]{0x59, 0x20, 0, 17, 1});
        assertFalse(d.s.settingsReady());
        assertTrue(d.s.needsReconnect());
    }

    @Test
    public void schemaAndPaddedReadsPreserveExistingG11WithoutWrites()
    {
        Device d = new Device();
        d.current.set(50, 11);
        int[] delta = {0, 3, 2, 2, 2, 2};
        for (int i = 0; i < 6; i++)
        {
            d.current.set(80 + i, delta[i]);
        }
        d.load();
        assertTrue(d.s.supported());
        assertEquals(3, d.s.mode);
        assertEquals(11, d.s.config().get(50));
        assertTrue(d.s.config().complete());
        assertTrue(d.writes().isEmpty());
        for (byte[] p : d.sent)
        {
            assertFalse((p[3] & 255) >= 61 && (p[3] & 255) <= 63);
        }
    }

    @Test
    public void unknownSchemaNeverReadsSettingsOrWrites()
    {
        Device d = new Device();
        d.schema = 2;
        d.load();
        d.s.begin();
        d.s.changeMode(3);
        assertEquals(1, d.sent.size());
        assertFalse(d.s.supported());
        assertFalse(d.s.config().complete());
    }

    @Test
    public void saveChangesOnlyDesiredValuesAndStaysOff()
    {
        Device d = new Device();
        d.current.set(50, 11);
        int[] delta = {0, 3, 2, 2, 2, 2};
        for (int i = 0; i < 6; i++)
        {
            d.current.set(80 + i, delta[i]);
        }
        d.load();
        d.s.begin();
        d.drain();
        assertTrue(d.s.editing());
        assertEquals(0, d.s.mode);
        d.sent.clear();
        d.cursor = 0;
        Ff3Config c = d.s.config();
        c.set(50, 20);
        for (int i = 81; i <= 85; i++)
        {
            c.set(i, 4);
        }
        d.s.save(c);
        d.drain();
        assertEquals(Arrays.asList(50, 81, 82, 83, 84, 85, 37, 37), d.writes());
        assertEquals(0, d.mode);
        assertFalse(d.s.editing());
        assertEquals(32, d.current.get(41));
        assertEquals(20, d.s.config().cumulative(5));
        assertTrue(d.s.message.contains("설정 저장됨"));
    }

    @Test
    public void invalidTablesAndMissingValuesNeverWrite()
    {
        assertNotNull(new Ff3Config().validate());
        assertNull(preset().validate());
        int[][] invalid = {{50, 11}, {82, 5}, {70, 42}, {48, 1}, {65, 29}, {96, 0}, {44, 5}, {60, 2}, {55, 1}};
        for (int[] change : invalid)
        {
            Ff3Config c = preset();
            c.set(change[0], change[1]);
            assertNotNull(Arrays.toString(change), c.validate());
        }
        Device d = new Device();
        d.load();
        d.s.begin();
        d.drain();
        int size = d.sent.size();
        Ff3Config c = d.s.config();
        c.set(50, 11);
        d.s.save(c);
        assertEquals(size, d.sent.size());
    }

    @Test
    public void shortReadDoesNotFillUnknownWithZero()
    {
        Device d = new Device();
        d.s.load();
        d.respond();
        d.s.receive(new byte[]{0x59, 0x20, 0, 17, 1});
        assertTrue(d.s.needsReconnect());
        assertFalse(d.s.config().complete());
        assertTrue(d.writes().isEmpty());
    }

    @Test
    public void wrongWriteEchoStopsBeforeValidate()
    {
        Device d = new Device();
        d.load();
        d.s.begin();
        d.s.receive(new byte[]{0x59, 0x20, 1, 17, 1, 3});
        assertTrue(d.s.needsReconnect());
        assertEquals(Arrays.asList(17), d.writes());
    }

    @Test
    public void firmwareErrorHasNoIndexAndDoesNotAdvance()
    {
        Device d = new Device();
        d.load();
        d.s.begin();
        d.drain();
        Ff3Config c = d.s.config();
        c.set(50, 21);
        d.s.save(c);
        d.s.receive(new byte[]{(byte) 0xF0, 0x59, 2, 1, 0x12, 0x34});
        assertEquals(Ff3Session.State.ERROR, d.s.state);
        assertTrue(d.s.message.contains("4660"));
        assertTrue(d.s.message.contains("파일 저장"));
        assertFalse(d.s.config().complete());
        assertEquals(Arrays.asList(17, 37, 50), d.writes());
    }

    @Test
    public void timeoutCannotAutomaticallyRetryOrActivate()
    {
        Device d = new Device();
        d.load();
        d.s.begin();
        d.drain();
        d.s.save(d.s.config());
        while (!d.timers.isEmpty())
        {
            d.timers.remove().run();
        }
        int count = d.sent.size();
        d.s.changeMode(3);
        d.s.load();
        assertTrue(d.s.needsReconnect());
        assertEquals(count, d.sent.size());
    }

    @Test
    public void fileFailureIsNotReportedAsSaved()
    {
        Device d = new Device();
        d.load();
        d.s.begin();
        d.drain();
        d.corruptFile = true;
        d.s.save(d.s.config());
        d.drain();
        assertEquals(Ff3Session.State.ERROR, d.s.state);
        assertFalse(d.s.message.startsWith("설정 저장됨"));
    }

    @Test
    public void pendingEditIsPreservedAndAbortRestoresExecutionValues()
    {
        Device d = new Device();
        d.mode = 0;
        d.staging = d.current.copy();
        d.staging.set(50, 21);
        d.status |= 4;
        d.load();
        assertTrue(d.s.editing());
        assertEquals(21, d.s.config().get(50));
        int count = d.sent.size();
        d.s.begin();
        assertEquals(count, d.sent.size());
        d.s.abort();
        d.drain();
        assertEquals(20, d.s.config().get(50));
        assertFalse(d.s.editing());
    }

    @Test
    public void ackDelayIsBoundedAndDisconnectCancelsDelayedWork()
    {
        Device d = new Device();
        d.load();
        d.s.begin();
        d.drain();
        d.status &= ~8;
        d.s.save(d.s.config());
        d.drain();
        assertTrue(d.s.busy());
        int count = d.sent.size();
        d.s.reset();
        while (!d.polls.isEmpty())
        {
            d.polls.remove().run();
        }
        while (!d.timers.isEmpty())
        {
            d.timers.remove().run();
        }
        assertEquals(count, d.sent.size());
        assertEquals(-1, d.s.schema);
    }

    @Test
    public void activationRequiresValidLedgerAndSeparateConfirmedModeWrite()
    {
        Device d = new Device();
        d.mode = 0;
        d.load();
        d.status &= ~16;
        d.s.changeMode(3);
        d.drain();
        assertTrue(d.writes().isEmpty());
        d.status |= 16;
        d.s.load();
        d.drain();
        d.s.changeMode(3);
        d.drain();
        assertEquals(Arrays.asList(17), d.writes());
        assertEquals(3, d.s.mode);
    }

    @Test
    public void unconfirmedFileCanBeCreatedByModeWrite()
    {
        Device d = new Device();
        d.mode = 0;
        d.corruptFile = true;
        d.load();
        d.s.changeMode(3);
        d.respond();
        d.respond();
        assertEquals(Arrays.asList(17), d.writes());
        d.respond();
        d.corruptFile = false;
        d.drain();
        assertEquals(3, d.s.mode);
        assertFalse(d.s.busy());
        assertNotEquals(Ff3Session.State.ERROR, d.s.state);
    }

    @Test
    public void modeWriteStillRequiresConfirmedFileAfterwards()
    {
        Device d = new Device();
        d.mode = 0;
        d.corruptFile = true;
        d.load();
        d.s.changeMode(3);
        d.drain();
        assertEquals(Arrays.asList(17), d.writes());
        assertEquals(Ff3Session.State.ERROR, d.s.state);
        assertTrue(d.s.message.contains("파일 저장"));
    }

    @Test
    public void runtimePrerequisitesRemainMandatory()
    {
        for (int bit : new int[]{1, 8, 16})
        {
            Device d = new Device();
            d.mode = 0;
            d.load();
            d.status &= ~bit;
            d.s.changeMode(3);
            d.drain();
            assertTrue(d.writes().isEmpty());
            assertEquals(Ff3Session.State.ERROR, d.s.state);
            if (bit == 16)
            {
                assertTrue(d.s.message.contains("OFF"));
            }
        }
    }

    @Test
    public void unsignedGenerationAndCrcDoNotSignExtend()
    {
        assertEquals(2147483649L, Ff3Session.unsigned(new byte[]{(byte) 0x80, 0, 0, 1}, 0, 4));
        assertEquals(4294967295L, Ff3Session.unsigned(new byte[]{-1, -1, -1, -1}, 0, 4));
    }

    @Test
    public void capabilityGateDoesNotAssumeFutureCompatibility()
    {
        assertFalse(Ff3Config.supportsFf(4, 4, true));
        assertFalse(Ff3Config.supportsFf(4, 5, true));
        assertTrue(Ff3Config.supportsFf(4, 6, true));
        assertFalse(Ff3Config.supportsSchemaQuery(4, 5, true));
        assertTrue(Ff3Config.supportsSchemaQuery(4, 6, true));
        assertFalse(Ff3Config.supportsSchemaQuery(4, 6, false));
        assertFalse(Ff3Config.supportsFf(4, 7, true));
        assertFalse(Ff3Config.supportsFf(5, 0, true));
    }

    @Test
    public void ackCanArriveLaterWithoutAnyAutomaticModeWrite()
    {
        Device d = new Device();
        d.load();
        d.s.begin();
        d.drain();
        d.status &= ~8;
        d.s.save(d.s.config());
        d.drain();
        assertTrue(d.s.busy());
        d.status |= 8;
        d.polls.remove().run();
        d.drain();
        assertFalse(d.s.busy());
        assertEquals(0, d.mode);
        assertTrue(d.s.message.startsWith("설정 저장됨"));
    }

    @Test
    public void ackDeadlineStopsAfterTwentyPolls()
    {
        Device d = new Device();
        d.load();
        d.s.begin();
        d.drain();
        d.status &= ~8;
        d.s.save(d.s.config());
        d.drain();
        int polls = 0;
        while (!d.polls.isEmpty() && polls < 30)
        {
            d.polls.remove().run();
            d.drain();
            polls++;
        }
        assertEquals(20, polls);
        assertEquals(Ff3Session.State.ERROR, d.s.state);
        assertEquals(0, d.mode);
    }

    @Test
    public void fullSixteenZonesCanBeEditedWithoutTruncation()
    {
        Ff3Config c = preset();
        c.set(55, 16);
        c.set(50, 40);
        for (int i = 0; i < 16; i++)
        {
            c.set(64 + i, i == 0 ? 0 : 30 + i * 5);
            c.set(80 + i, i == 0 ? 0 : 2);
            c.set(96 + i, 4);
        }
        assertNull(c.validate());
        assertEquals(30, c.cumulative(15));
    }

    @Test
    public void readbackMismatchDoesNotReportSuccess()
    {
        Device d = new Device();
        d.load();
        d.s.begin();
        d.drain();
        d.s.save(d.s.config());
        d.respond();
        d.respond(); // VALIDATE, SAVE_APPLY
        d.current.set(50, 21);
        d.drain();
        assertEquals(Ff3Session.State.ERROR, d.s.state);
    }
}
