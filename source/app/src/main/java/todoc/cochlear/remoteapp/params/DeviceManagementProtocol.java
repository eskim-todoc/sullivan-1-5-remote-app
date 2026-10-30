package todoc.cochlear.remoteapp.params;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Device writes are sent once; echo responses verify flash read-back, never a disconnect.
 */
public final class DeviceManagementProtocol
{
    private DeviceManagementProtocol()
    {
    }

    public static byte[] serialPacket(String value)
    {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9]{4}"))
        {
            throw new IllegalArgumentException("제조번호는 영문 대문자·숫자 4자입니다.");
        }
        return asciiPacket(PacketInfo.HEADER_WRITE_EXTERNAL_SERIAL, normalized);
    }

    public static byte[] keyPacket(String value)
    {
        if (value == null || !value.matches("[0-9]{6}"))
        {
            throw new IllegalArgumentException("페어링키는 숫자 6자리입니다.");
        }
        return asciiPacket(PacketInfo.HEADER_WRITE_PAIRING_KEY, value);
    }

    private static byte[] asciiPacket(byte header, String value)
    {
        byte[] result = new byte[value.length() + 1];
        result[0] = header;
        System.arraycopy(value.getBytes(StandardCharsets.US_ASCII), 0, result, 1, value.length());
        return result;
    }

    public static boolean matches(byte[] request, byte[] response)
    {
        if (request == null || response == null || request.length == 0)
        {
            return false;
        }
        if (request[0] == PacketInfo.HEADER_FACTORY_MAPS)
        {
            return response.length == 2 && response[0] == request[0] && response[1] == 1;
        }
        if (request[0] == PacketInfo.HEADER_SPECIFIC_CMD && request.length == 5)
        {
            return response.length == 6 && response[0] == request[0] && response[1] == request[1]
                && response[2] == request[2] && response[3] == request[3]
                && response[4] == 1 && response[5] == request[4];
        }
        return Arrays.equals(request, response);
    }
}
