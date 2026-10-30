package todoc.cochlear.remoteapp.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class DeviceManagementProtocolTest
{
    @Test
    public void validatesIdentityAtAsciiBoundary()
    {
        assertArrayEquals(new byte[]{0x55, 'A', 'B', '1', '2'}, DeviceManagementProtocol.serialPacket(" ab12 "));
        assertArrayEquals(new byte[]{0x56, '0', '1', '2', '3', '4', '5'}, DeviceManagementProtocol.keyPacket("012345"));
        for (String bad : new String[]{"", "12345", "가나다라", "AB_1"})
        {
            assertThrows(IllegalArgumentException.class, () -> DeviceManagementProtocol.serialPacket(bad));
        }
        for (String bad : new String[]{"12345", "1234567", "１２３４５６", "12a456", " 123456"})
        {
            assertThrows(IllegalArgumentException.class, () -> DeviceManagementProtocol.keyPacket(bad));
        }
    }

    @Test
    public void requiresExactReadBackRatherThanHeaderOrDisconnect()
    {
        byte[] request = DeviceManagementProtocol.keyPacket("012345");
        assertTrue(DeviceManagementProtocol.matches(request, request.clone()));
        byte[] other = request.clone();
        other[6] = '6';
        assertFalse(DeviceManagementProtocol.matches(request, other));
        assertFalse(DeviceManagementProtocol.matches(request, new byte[]{0x56, 1}));
        assertFalse(DeviceManagementProtocol.matches(request, null));
    }

    @Test
    public void mapAcceptanceAndFactoryCompletionUseDifferentContracts()
    {
        byte[] map = {0x59, 0x21, 1, 1, 0x10};
        assertTrue(DeviceManagementProtocol.matches(map, new byte[]{0x59, 0x21, 1, 1, 1, 0x10}));
        assertFalse(DeviceManagementProtocol.matches(map, new byte[]{0x59, 0x21, 1, 1, 1, 0x20}));
        assertTrue(DeviceManagementProtocol.matches(new byte[]{0x57}, new byte[]{0x57, 1}));
        assertFalse(DeviceManagementProtocol.matches(new byte[]{0x57}, new byte[]{0x57, 0}));
    }
}
