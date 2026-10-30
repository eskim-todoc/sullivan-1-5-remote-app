package todoc.cochlear.remoteapp.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class UserControlQueueTest
{
    @Test
    public void telemetryReadDoesNotConsumeOrReplaceQueuedVolumeWrite()
    {
        UserControlQueue queue = new UserControlQueue();
        assertFalse(queue.offer(new byte[]{0x59, 0x22, 0, 2}, 0));
        assertTrue(queue.offer(new byte[]{0x46, 2}, 1));
        assertTrue(queue.offer(new byte[]{0x49, 1}, 2));
        // After the in-flight battery response, take the user's write before next poll.
        assertArrayEquals(new byte[]{0x46, 2}, queue.poll());
        assertArrayEquals(new byte[]{0x49, 1}, queue.poll());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void gatingWritesWaitBehindTelemetryAndKeepOrderWithOtherControls()
    {
        UserControlQueue queue = new UserControlQueue();
        byte[] enable = {0x59, 2, 1, 1, (byte) 255};
        byte[] disable = {0x59, 2, 1, 2, (byte) 255};
        assertFalse(queue.offer(new byte[]{0x59, 0x22, 0, 2}, 0));
        assertTrue(queue.offer(enable, 1));
        assertTrue(queue.offer(new byte[]{0x46, 2}, 2));
        assertTrue(queue.offer(disable, 3));
        // 큐는 호출자의 버퍼 변경과 격리되고, 최신 값만 남기는 방식으로 순서를 바꾸지 않는다.
        enable[4] = 2;
        assertArrayEquals(new byte[]{0x59, 2, 1, 1, (byte) 255}, queue.poll());
        assertArrayEquals(new byte[]{0x46, 2}, queue.poll());
        assertArrayEquals(disable, queue.poll());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void gatingClassificationRejectsReadsMalformedModesAndMapCommands()
    {
        assertTrue(UserControlQueue.isOrdinaryWrite(new byte[]{0x59, 2, 1, 1, 1}));
        assertTrue(UserControlQueue.isOrdinaryWrite(new byte[]{0x59, 2, 1, 2, (byte) 255}));
        for (byte[] packet : new byte[][]{
            null, {}, {0x59, 2, 1, 1}, {0x59, 2, 1, 1, 2, 0},
            {0x59, 1, 1, 1, 2}, {0x59, 2, 0, 1, 2}, {0x59, 2, 2, 1, 2},
            {0x59, 2, 1, 0, 2}, {0x59, 2, 1, 3, 2}, {0x59, 0x21, 1, 1, 0x10}
        })
        {
            assertFalse(UserControlQueue.isOrdinaryWrite(packet));
        }
    }

    @Test
    public void boundsCopiesExpiresAndClearsSessionWrites()
    {
        UserControlQueue queue = new UserControlQueue();
        byte[] command = {0x46, 2};
        assertTrue(queue.offer(command, 0));
        command[1] = 9;
        assertArrayEquals(new byte[]{0x46, 2}, queue.poll());
        for (int i = 0; i < 8; i++)
        {
            assertTrue(queue.offer(command, 10));
        }
        assertFalse(queue.offer(command, 10));
        assertEquals(0, queue.expire(10010));
        assertEquals(8, queue.expire(10011));
        queue.offer(command, 20000);
        queue.clear();
        assertNull(queue.poll());
    }

    @Test
    public void excludesDestructiveManagementAndReadCommands()
    {
        for (byte[] packet : new byte[][]{{0x55, 'A', 'B', '1', '2'}, {0x56, 1, 2, 3, 4, 5, 6}, {0x57},
            {0x59, 0x21, 1, 1, 0x10}, {0x59, 0x20, 0, 1}, {0x60}})
        {
            assertFalse(UserControlQueue.isOrdinaryWrite(packet));
        }
        assertTrue(UserControlQueue.isOrdinaryWrite(new byte[]{0x59, 0x20, 1, 2, 4}));
    }
}
