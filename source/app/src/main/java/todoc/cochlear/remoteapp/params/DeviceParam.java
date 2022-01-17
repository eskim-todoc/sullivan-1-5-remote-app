package todoc.cochlear.remoteapp.params;

public class DeviceParam
{
    // For Sound Processor model number.
    public static final String DEVICE_MODEL_TD2 = "01";

    public static final byte INIT_VALUE_BATTERY = 0;
    public static final byte INIT_VALUE_PROGRAM = 1;
    public static final byte INIT_VALUE_VOLUME = 7;
    public static final byte INIT_VALUE_SENSITIVITY = 2;
    public static final byte INIT_VALUE_ALARM_LED = 1;
    public static final byte INIT_VALUE_ALARM_STIMULATION = 1;
    public static final byte INIT_VALUE_TELECOIL = 1;
    public static final byte INIT_VALUE_POWER_MODE = 1;

    public static final byte LIMIT_MIN_BATTERY = 0;
    public static final byte LIMIT_MIN_PROGRAM = 1;
    public static final byte LIMIT_MIN_VOLUME = 1;
    public static final byte LIMIT_MIN_SENSITIVITY = 1;
    public static final byte LIMIT_MIN_ALARM_LED = 1;
    public static final byte LIMIT_MIN_ALARM_STIMULATION = 1;
    public static final byte LIMIT_MIN_TELECOIL = 1;
    public static final byte LIMIT_MIN_POWER_MODE = 1;

    public static final byte LIMIT_MAX_BATTERY = 100;
    public static final byte LIMIT_MAX_PROGRAM = 4;     // currently max : 4, but this value can be change dynamically during running app.
    public static final byte LIMIT_MAX_VOLUME = 10;
    public static final byte LIMIT_MAX_SENSITIVITY = 4;
    public static final byte LIMIT_MAX_ALARM_LED = 2;
    public static final byte LIMIT_MAX_ALARM_STIMULATION = 2;
    public static final byte LIMIT_MAX_TELECOIL = 2;
    public static final byte LIMIT_MAX_POWER_MODE = 2;

    public static final byte ALARM_STIMULATION_ON = 1;
    public static final byte ALARM_STIMULATION_OFF = 2;

    public static final byte ALARM_LED_ON = 1;
    public static final byte ALARM_LED_OFF = 2;

    public static final byte TELECOIL_ON = 1;
    public static final byte TELECOIL_OFF = 2;

    public static final byte POWER_MODE_ON = 1;
    public static final byte POWER_MODE_OFF = 2;

    private byte battery;
    private byte program;
    private byte volume;
    private byte sensitivity;
    private byte alarmLed;
    private byte alarmStimulation;
    private byte telecoil;
    private byte warnning;
    private byte powerMode;

    public DeviceParam()
    {
        battery = INIT_VALUE_BATTERY;
        program = INIT_VALUE_PROGRAM;
        volume = INIT_VALUE_VOLUME;
        sensitivity = INIT_VALUE_SENSITIVITY;
        alarmLed = INIT_VALUE_ALARM_LED;
        alarmStimulation = INIT_VALUE_ALARM_STIMULATION;
        telecoil = INIT_VALUE_TELECOIL;
        powerMode = INIT_VALUE_POWER_MODE;
    }

    @Override
    public String toString()
    {
        return "DeviceParam{" +
                "battery=" + battery +
                ", program=" + program +
                ", volume=" + volume +
                ", sensitivity=" + sensitivity +
                ", alarmLed=" + alarmLed +
                ", alarmStimulation=" + alarmStimulation +
                ", telecoil=" + telecoil +
                ", powerMode=" + powerMode +
                '}';
    }

    /**
     * Copy Sound Processor status parameters from source to destination.
     */
    static public boolean copy(DeviceParam src, DeviceParam dst)
    {
        if (src == null || dst == null)
        {
            return false;
        }

        dst.setVolume(src.getVolume());
        dst.setProgram(src.getProgram());
        dst.setSensitivity(src.getSensitivity());
        dst.setTelecoil(src.getTelecoil());
        dst.setPowerMode(src.getPowerMode());
        dst.setAlarmStimulation(src.getAlarmStimulation());
        dst.setAlarmLed(src.getAlarmLed());
        dst.setWarnning(src.getWarnning());
        dst.setBattery(src.getBattery());

        return true;
    }

    public byte getBattery()
    {
        return battery;
    }

    public void setBattery(byte battery)
    {
        this.battery = battery;
    }

    public byte getProgram()
    {
        return program;
    }

    public void setProgram(byte program)
    {
        this.program = program;
    }

    public byte getVolume()
    {
        return volume;
    }

    public void setVolume(byte volume)
    {
        this.volume = volume;
    }

    public byte getSensitivity()
    {
        return sensitivity;
    }

    public void setSensitivity(byte sensitivity)
    {
        this.sensitivity = sensitivity;
    }

    public byte getAlarmLed()
    {
        return alarmLed;
    }

    public void setAlarmLed(byte alarmLed)
    {
        this.alarmLed = alarmLed;
    }

    public byte getAlarmStimulation()
    {
        return alarmStimulation;
    }

    public void setAlarmStimulation(byte alarmStimulation)
    {
        this.alarmStimulation = alarmStimulation;
    }

    public byte getTelecoil()
    {
        return telecoil;
    }

    public void setTelecoil(byte telecoil)
    {
        this.telecoil = telecoil;
    }

    public byte getPowerMode()
    {
        return powerMode;
    }

    public void setPowerMode(byte powerMode)
    {
        this.powerMode = powerMode;
    }

    public byte getWarnning()
    {
        return warnning;
    }

    public void setWarnning(byte warnning)
    {
        this.warnning = warnning;
    }
}
