package todoc.cochlear.remoteapp.params;

import org.junit.Test;

import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;

import static org.junit.Assert.*;

public class StatusIsolationTest
{
    @Test
    public void packetQueuesAndBuffersBelongToOneConnection()
    {
        Status first = new Status();
        first.sendingPacket = new byte[]{0x46, 1};
        first.rcOptionBitmap = new byte[]{1, 2};
        first.receivedPackets.offer(new byte[]{0x46, 2});

        Status second = new Status();
        assertNotSame(first.receivedPackets, second.receivedPackets);
        assertTrue(second.receivedPackets.isEmpty());
        assertNull(second.sendingPacket);
        assertNull(second.rcOptionBitmap);

        second.sendingPacket = new byte[]{0x46, 3};
        second.rcOptionBitmap = new byte[]{4, 5};
        second.receivedPackets.offer(new byte[]{0x46, 4});
        second.sendingPacket[1] = 9;
        second.rcOptionBitmap[0] = 9;
        second.receivedPackets.clear();

        assertArrayEquals(new byte[]{0x46, 1}, first.sendingPacket);
        assertArrayEquals(new byte[]{1, 2}, first.rcOptionBitmap);
        assertArrayEquals(new byte[]{0x46, 2}, first.receivedPackets.poll());
        assertTrue(first.receivedPackets.isEmpty());
    }

    @Test
    public void disconnectingOneDevicePreservesTheOtherIdentityAndProtocolState()
    {
        Status first = new Status();
        first.connectedDevice = device("FFFF");
        first.connectedUser = user("Left", EntityUser.EAR_LEFT);
        first.connectionState = Status.CONNECTION_STATE_CONNECTED;
        first.transferState = Status.TRANSFER_STATE_BUSY;
        first.resendingCount = 2;
        first.fwRelease = Status.FW_RELEASE_4;
        first.rel4LinkIndexMax = 8;
        first.rcProtocolMajor = 1;
        first.rcProtocolMinor = 2;

        Status second = new Status();
        second.connectedDevice = device("ABCD");
        second.connectedUser = user("Right", EntityUser.EAR_RIGHT);
        second.connectionState = Status.CONNECTION_STATE_CONNECTED;
        second.transferState = Status.TRANSFER_STATE_BUSY;
        second.resendingCount = 1;
        second.fwRelease = Status.FW_RELEASE_4_PLUS;
        second.rel4LinkIndexMax = 10;
        second.rcProtocolMajor = 2;
        second.rcProtocolMinor = 3;

        second.connectedDevice = null;
        second.connectedUser = null;
        second.connectionState = Status.CONNECTION_STATE_DISCONNECTED;
        second.transferState = Status.TRANSFER_STATE_IDLE;
        second.resendingCount = 0;
        second.fwRelease = Status.FW_RELEASE_UNKNOWN;
        second.rel4LinkIndexMax = 0;
        second.rcProtocolMajor = PacketInfo.LINK_VALUE_UNKNOWN;
        second.rcProtocolMinor = PacketInfo.LINK_VALUE_UNKNOWN;

        assertEquals("FFFF", first.connectedDevice.serialNumber);
        assertEquals("Left", first.connectedUser.name);
        assertEquals(EntityUser.EAR_LEFT, first.connectedUser.ear);
        assertEquals(Status.CONNECTION_STATE_CONNECTED, first.connectionState);
        assertEquals(Status.TRANSFER_STATE_BUSY, first.transferState);
        assertEquals(2, first.resendingCount);
        assertEquals(Status.FW_RELEASE_4, first.fwRelease);
        assertEquals(8, first.rel4LinkIndexMax);
        assertEquals(1, first.rcProtocolMajor);
        assertEquals(2, first.rcProtocolMinor);
    }

    @Test
    public void creatingDeviceSessionsDoesNotReplaceTheAppShellAlias()
    {
        Status shell = Status.instance();
        EntityDevice shellDevice = shell.connectedDevice;
        EntityUser shellUser = shell.connectedUser;
        int shellActivityState = shell.activityRunningState;
        int shellServiceState = shell.exitCaptureState;

        Status session = new Status();
        session.connectedDevice = device("1234");
        session.connectedUser = user("Session", EntityUser.EAR_LEFT);
        session.activityRunningState = shellActivityState + 1;
        session.exitCaptureState = shellServiceState + 1;

        assertNotSame(shell, session);
        assertSame(shell, Status.instance());
        assertSame(shellDevice, shell.connectedDevice);
        assertSame(shellUser, shell.connectedUser);
        assertEquals(shellActivityState, shell.activityRunningState);
        assertEquals(shellServiceState, shell.exitCaptureState);
    }

    private static EntityDevice device(String serial)
    {
        EntityDevice device = new EntityDevice();
        device.serialNumber = serial;
        return device;
    }

    private static EntityUser user(String name, String ear)
    {
        EntityUser user = new EntityUser();
        user.name = name;
        user.ear = ear;
        return user;
    }
}
