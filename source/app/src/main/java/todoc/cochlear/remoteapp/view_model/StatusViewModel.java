package todoc.cochlear.remoteapp.view_model;

import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import todoc.cochlear.remoteapp.params.PacketInfo;

public class StatusViewModel extends ViewModel
{
    // BLE 연결 상태 관련
    static public final int CONNECTION_STATE_DISCONNECTED = 0;
    static public final int CONNECTION_STATE_CONNECTING = 1;
    static public final int CONNECTION_STATE_CONNECTED = 2;
    static public final int CONNECTION_STATE_DISCONNECTING = 3;

    private final MutableLiveData<Integer> mConnectionState = new MutableLiveData<>();

    public MutableLiveData<Integer> getObjectConnectionState()
    {
        return mConnectionState;
    }

    public int getConnectionState()
    {
        if (mConnectionState.getValue() == null)
        {
            mConnectionState.setValue(CONNECTION_STATE_DISCONNECTED);
        }

        return mConnectionState.getValue();
    }

    public void setConnectionState(int connectionState)
    {
        if (Looper.getMainLooper().isCurrentThread())
        {
            mConnectionState.setValue(connectionState);
        }
        else
        {
            mConnectionState.postValue(connectionState);
        }
    }

    // OTE FW Version
    private final MutableLiveData<Integer> mFwVerUpper = new MutableLiveData<>();
    private final MutableLiveData<Integer> mFwVerLower = new MutableLiveData<>();

    public int getFwVerUpper()
    {
        if (mFwVerUpper.getValue() == null)
        {
            mFwVerUpper.setValue(-1);
        }

        return mFwVerUpper.getValue();
    }

    public int getFwVerLower()
    {
        if (mFwVerLower.getValue() == null)
        {
            mFwVerLower.setValue(-1);
        }

        return mFwVerLower.getValue();
    }

    public void setFwVerUpper(int fwVerUpper)
    {
        if (Looper.getMainLooper().isCurrentThread())
        {
            mFwVerUpper.setValue(fwVerUpper);
        }
        else
        {
            mFwVerUpper.postValue(fwVerUpper);
        }
    }

    public void setFwVerLower(int fwVerLower)
    {
        if (Looper.myLooper().isCurrentThread())
        {
            mFwVerLower.setValue(fwVerLower);
        }
        else
        {
            mFwVerLower.postValue(fwVerLower);
        }
    }

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
            mBatteryLevel.setValue(PacketInfo.INIT_VALUE_BATTERY);
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
            mNotification.setValue(PacketInfo.INIT_VALUE_NOTIFICATION);
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
            mLed.setValue(PacketInfo.INIT_VALUE_LED);
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
            mTelecoil.setValue(PacketInfo.INIT_VALUE_TELECOIL);
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
            mMaxOutput.setValue(PacketInfo.INIT_VALUE_MAX_OUTPUT);
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
            mVolume.setValue(PacketInfo.INIT_VALUE_VOLUME);
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
            mProgram.setValue(PacketInfo.INIT_VALUE_PROGRAM);
        }

        return mProgram.getValue();
    }
}
