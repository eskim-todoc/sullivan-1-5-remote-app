package todoc.cochlear.remoteapp.params;

import org.junit.Test;
import static org.junit.Assert.*;

public class UserControlQueueTest {
    @Test public void telemetryReadDoesNotConsumeOrReplaceQueuedVolumeWrite() {
        UserControlQueue queue = new UserControlQueue();
        assertFalse(queue.offer(new byte[]{0x59,0x22,0,2}, 0));
        assertTrue(queue.offer(new byte[]{0x46,2}, 1));
        assertTrue(queue.offer(new byte[]{0x49,1}, 2));
        // After the in-flight battery response, take the user's write before next poll.
        assertArrayEquals(new byte[]{0x46,2}, queue.poll());
        assertArrayEquals(new byte[]{0x49,1}, queue.poll());
        assertTrue(queue.isEmpty());
    }
    @Test public void boundsCopiesExpiresAndClearsSessionWrites() {
        UserControlQueue queue = new UserControlQueue(); byte[] command={0x46,2};
        assertTrue(queue.offer(command,0)); command[1]=9;
        assertArrayEquals(new byte[]{0x46,2},queue.poll());
        for(int i=0;i<8;i++)assertTrue(queue.offer(command,10));
        assertFalse(queue.offer(command,10));
        assertEquals(0,queue.expire(10010));assertEquals(8,queue.expire(10011));
        queue.offer(command,20000);queue.clear();assertNull(queue.poll());
    }
    @Test public void excludesDestructiveManagementAndReadCommands() {
        for(byte[] packet:new byte[][]{{0x55,'A','B','1','2'},{0x56,1,2,3,4,5,6},{0x57},
                {0x59,0x21,1,1,0x10},{0x59,0x20,0,1},{0x60}})
            assertFalse(UserControlQueue.isOrdinaryWrite(packet));
        assertTrue(UserControlQueue.isOrdinaryWrite(new byte[]{0x59,0x20,1,2,4}));
    }
}
