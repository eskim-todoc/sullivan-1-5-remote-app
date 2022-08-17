package todoc.cochlear.remoteapp.params;

import androidx.annotation.NonNull;

public class PacketInfo
{
    // 패킷별 헤더 정보
    static public final byte HEADER_PASSWORD = (byte) (0x40 & 0xff);
    static public final byte HEADER_SOUND_PROCESSOR_INFO = (byte) (0x41 & 0xff);
    static public final byte HEADER_MAP_DATA_INFORMATION = (byte) (0x42 & 0xff);
    static public final byte HEADER_SOUND_PROCESSOR_STATUS = (byte) (0x43 & 0xff);
    static public final byte HEADER_VALUE_PROMGRAM = (byte) (0x44 & 0xff);
    static public final byte HEADER_VALUE_MAX_OUTPUT = (byte) (0x45 & 0xff);
    static public final byte HEADER_VALUE_VOLUME = (byte) (0x46 & 0xff);
    static public final byte HEADER_VALUE_TELECOIL = (byte) (0x47 & 0xff);
    static public final byte HEADER_VALUE_NOTIFICATION = (byte) (0x48 & 0xff);
    static public final byte HEADER_VALUE_LED = (byte) (0x49 & 0xff);
    static public final byte HEADER_SYSTEM_WARNING = (byte) (0x4A & 0xff);
    static public final byte HEADER_ERROR = (byte) (0xf0 & 0xff);

    // 응답 패킷별 사이즈 정보
    static public final int PACKET_SIZE_PASSWORD = 2;
    static public final int PACKET_SIZE_PROCESSOR_INFO = 9;
    static public final int PACKET_SIZE_MAP_DATA_INFO = 7;
    static public final int PACKET_SIZE_PROCESSOR_STATUS = 8;
    static public final int PACKET_SIZE_PROGRAM = 2;
    static public final int PACKET_SIZE_MAX_OUTPUT = 2;
    static public final int PACKET_SIZE_VOLUME = 2;
    static public final int PACKET_SIZE_TELECOIL = 2;
    static public final int PACKET_SIZE_NOTIFICATION = 2;
    static public final int PACKET_SIZE_LED = 2;
    static public final int PACKET_SIZE_SYSTEM_WARNING = 1;
    static public final int PACKET_SIZE_ERROR = 3;

    static public final int INIT_VALUE_BATTERY = 0;
    static public final int INIT_VALUE_PROGRAM = 1;
    static public final int INIT_VALUE_VOLUME = 1;
    static public final int INIT_VALUE_MAX_OUTPUT = 1;
    static public final int INIT_VALUE_LED = 2;
    static public final int INIT_VALUE_NOTIFICATION = 2;
    static public final int INIT_VALUE_TELECOIL = 2;

    static public final int LIMIT_MIN_BATTERY = 0;
    static public final int LIMIT_MIN_PROGRAM = 1;
    static public final int LIMIT_MIN_VOLUME = 1;
    static public final int LIMIT_MIN_MAX_OUTPUT = 1;
    static public final int LIMIT_MIN_LED = 1;
    static public final int LIMIT_MIN_NOTIFICATION = 1;
    static public final int LIMIT_MIN_TELECOIL = 1;

    static public final int LIMIT_MAX_BATTERY = 100;
    static public final int LIMIT_MAX_PROGRAM = 4;     // currently max : 4, but this value can be change dynamically during running app.
    static public final int LIMIT_MAX_VOLUME = 10;
    static public final int LIMIT_MAX_MAX_OUTPUT = 4;
    static public final int LIMIT_MAX_LED = 2;
    static public final int LIMIT_MAX_NOTIFICATION = 2;
    static public final int LIMIT_MAX_TELECOIL = 2;

    static public final byte PASSWORD_PASS = 1;
    static public final byte PASSWORD_FAIL = 2;

    static public final byte NOTIFICATION_ON = 1;
    static public final byte NOTIFICATION_OFF = 2;

    static public final byte LED_ON = 1;
    static public final byte LED_OFF = 2;

    static public final byte TELECOIL_ON = 1;
    static public final byte TELECOIL_OFF = 2;

    static public final byte MAX_OUTPUT_UP = 1;
    static public final byte MAX_OUTPUT_DOWN = 2;

    static public final byte VOLUME_UP = 1;
    static public final byte VOLUME_DOWN = 2;

    static public final byte PROGRAM_UP = 1;
    static public final byte PROGRAM_DOWN = 2;

    public byte battery;
    public byte program;
    public byte volume;
    public byte maxOutput;
    public byte led;
    public byte notification;
    public byte telecoil;
    public byte error;

    public PacketInfo()
    {
        battery = INIT_VALUE_BATTERY;
        program = INIT_VALUE_PROGRAM;
        volume = INIT_VALUE_VOLUME;
        maxOutput = INIT_VALUE_MAX_OUTPUT;
        led = INIT_VALUE_LED;
        notification = INIT_VALUE_NOTIFICATION;
        telecoil = INIT_VALUE_TELECOIL;
    }

    @NonNull
    @Override
    public String toString()
    {
        return "DeviceParam{" +
                "battery=" + battery +
                ", program=" + program +
                ", volume=" + volume +
                ", maxOutput=" + maxOutput +
                ", alarmLed=" + led +
                ", alarmStimulation=" + notification +
                ", telecoil=" + telecoil +
                '}';
    }
}
