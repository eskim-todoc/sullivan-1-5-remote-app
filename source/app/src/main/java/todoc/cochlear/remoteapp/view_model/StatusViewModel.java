package todoc.cochlear.remoteapp.view_model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class StatusViewModel extends ViewModel
{
    static public final int NOTIFICATION_ON = 1;
    static public final int NOTIFICATION_OFF = 2;

    static public final int LED_ON = 1;
    static public final int LED_OFF = 2;

    static public final int TELECOIL_ON = 1;
    static public final int TELECOIL_OFF = 2;

    static public final int MAX_OUTPUT_UP = 1;
    static public final int MAX_OUTPUT_DOWN = 2;

    static public final int VOLUME_UP = 1;
    static public final int VOLUME_DOWN = 2;

    static public final int PROGRAM_UP = 1;
    static public final int PROGRAM_DOWN = 2;

    static public final int BATTERY_DEFAULT_VALUE = 0;
    static public final int NOTIFICATION_DEFAULT_VALUE = 2;
    static public final int LED_DEFAULT_VALUE = 2;
    static public final int TELECOIL_DEFAULT_VALUE = 2;
    static public final int MAX_OUTPUT_DEFAULT_VALUE = 7;
    static public final int VOLUME_DEFAULT_VALUE = 1;
    static public final int PROGRAM_DEFAULT_VALUE = 1;

    // Battery level
    private final MutableLiveData<Integer> mBatteryLevel = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataBatteryLevel()
    {
        return mBatteryLevel;
    }

    public void setValueBatteryLevel(int level)
    {
        mBatteryLevel.setValue(level);
    }

    public int getValueBatteryLevel()
    {
        if (mBatteryLevel.getValue() == null)
        {
            mBatteryLevel.setValue(BATTERY_DEFAULT_VALUE);
        }

        return mBatteryLevel.getValue();
    }

    // Notification
    private final MutableLiveData<Integer> mNotification = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataNotification()
    {
        return mNotification;
    }

    public void setValueNotification(int value)
    {
        mNotification.setValue(value);
    }

    public int getValueNotification()
    {
        if (mNotification.getValue() == null)
        {
            mNotification.setValue(NOTIFICATION_DEFAULT_VALUE);
        }

        return mNotification.getValue();
    }

    // LED
    private final MutableLiveData<Integer> mLed = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataLed()
    {
        return mLed;
    }

    public void setValueLed(int value)
    {
        mLed.setValue(value);
    }

    public int getValueLed()
    {
        if (mLed.getValue() == null)
        {
            mLed.setValue(LED_DEFAULT_VALUE);
        }

        return mLed.getValue();
    }

    // Telecoil
    private final MutableLiveData<Integer> mTelecoil = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataTelecoil()
    {
        return mTelecoil;
    }

    public void setValueTelecoil(int value)
    {
        mTelecoil.setValue(value);
    }

    public int getValueTelecoil()
    {
        if (mTelecoil.getValue() == null)
        {
            mTelecoil.setValue(TELECOIL_DEFAULT_VALUE);
        }

        return mTelecoil.getValue();
    }

    // Max output
    private final MutableLiveData<Integer> mMaxOutput = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataMaxOutput()
    {
        return mMaxOutput;
    }

    public void setValueMaxOutput(int value)
    {
        mMaxOutput.setValue(value);
    }

    public int getValueMaxOutput()
    {
        if (mMaxOutput.getValue() == null)
        {
            mMaxOutput.setValue(MAX_OUTPUT_DEFAULT_VALUE);
        }

        return mMaxOutput.getValue();
    }

    // Volume
    private final MutableLiveData<Integer> mVolume = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataVolume()
    {
        return mVolume;
    }

    public void setValueVolume(int value)
    {
        mVolume.setValue(value);
    }

    public int getValueVolume()
    {
        if (mVolume.getValue() == null)
        {
            mVolume.setValue(VOLUME_DEFAULT_VALUE);
        }

        return mVolume.getValue();
    }

    // Program
    private final MutableLiveData<Integer> mProgram = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataProgram()
    {
        return mProgram;
    }

    public void setValueProgram(int value)
    {
        mProgram.setValue(value);
    }

    public int getValueProgram()
    {
        if (mProgram.getValue() == null)
        {
            mProgram.setValue(PROGRAM_DEFAULT_VALUE);
        }

        return mProgram.getValue();
    }
}
