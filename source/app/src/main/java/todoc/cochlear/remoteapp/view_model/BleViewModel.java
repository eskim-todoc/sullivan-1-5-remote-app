package todoc.cochlear.remoteapp.view_model;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BleViewModel extends ViewModel
{
    //
    // For BLE connection state.
    //
    static public final int BLE_DISCONNECTED = 0;
    static public final int BLE_CONNECTING = 1;
    static public final int BLE_CONNECTED = 2;

    private MutableLiveData<Integer> mBleConnection = new MutableLiveData<>();

    public MutableLiveData<Integer> getObjectBleConnection()
    {
        return mBleConnection;
    }

    public int getBleConnection()
    {
        if (mBleConnection.getValue() == null)
        {
            mBleConnection.setValue(BLE_DISCONNECTED);
        }

        return mBleConnection.getValue();
    }

    public void setBleConnection(int connection)
    {
        mBleConnection.setValue(connection);
    }

    public void postBleConnection(int connection)
    {
        mBleConnection.postValue(connection);
    }

    //
    // For Searching state
    //
    static public final int SEARCHING_DISABLED = 0;
    static public final int SEARCHING_ENABLED = 1;

    private MutableLiveData<Integer> mSearching = new MutableLiveData<>();

    public MutableLiveData<Integer> getObjectSearching()
    {
        return mSearching;
    }

    public int getSearching()
    {
        if (mSearching.getValue() == null)
        {
            mSearching.setValue(SEARCHING_DISABLED);
        }

        return mSearching.getValue();
    }

    public void setSearching(int searching)
    {
        mSearching.setValue(searching);
    }

    //
    // Connection state.
    //
    public enum CONN_STATE
    {DISCONNECTED, CONNECTING, CONNECTED}

    private CONN_STATE mConnState = CONN_STATE.DISCONNECTED;

    public CONN_STATE getConnState()
    {
        return mConnState;
    }

    public void setConnState(CONN_STATE state)
    {
        mConnState = state;
    }

    //
    // Scan state.
    //
    public enum SCAN_STATE
    {
        STOPPED, SCANNING
    }

    private SCAN_STATE mScanState = SCAN_STATE.STOPPED;

    public SCAN_STATE getScanState()
    {
        return mScanState;
    }

    public void setScanState(SCAN_STATE state)
    {
        mScanState = state;
    }

    //
    // Bond state.
    //
    private int mBondState = BluetoothDevice.BOND_NONE;

    public int getBondState()
    {
        return mBondState;
    }

    public void setBondState(int state)
    {
        mBondState = state;
    }

    //
    // Bond fail counter.
    //
    private int mBondFailCounter = 0;

    public int getBondFailCounter()
    {
        return mBondFailCounter;
    }

    public void increaseBondFailCounter()
    {
        mBondFailCounter++;
    }

    public void clearBondFailCounter()
    {
        mBondFailCounter = 0;
    }

    //
    // Currently connecting device information.
    //
    private String mUserNameConnecting = "";
    private String mUserEarConnecting = "";
    private String mUserPasswordConnecting = "";
    private String mDeviceSerialConnecting = "";
    private String mDevicePairingKeyConnecting = "";
    private String mDeviceAddressConnecting = "";

    public String getUserNameConnecting()
    {
        return mUserNameConnecting;
    }

    public String getUserEarConnecting()
    {
        return mUserEarConnecting;
    }

    public String getUserPasswordConnecting()
    {
        return mUserPasswordConnecting;
    }

    public String getDeviceSerialConnecting()
    {
        return mDeviceSerialConnecting;
    }

    public String getDevicePairingKeyConnecting()
    {
        return mDevicePairingKeyConnecting;
    }

    public String getDeviceAddressConnecting()
    {
        return mDeviceAddressConnecting;
    }

    public void setUserNameConnecting(String userName)
    {
        mUserNameConnecting = userName;
    }

    public void setUserEarConnecting(String userEar)
    {
        mUserEarConnecting = userEar;
    }

    public void setUserPasswordConnecting(String userPassword)
    {
        mUserPasswordConnecting = userPassword;
    }

    public void setDeviceSerialConnecting(String deviceSerial)
    {
        mDeviceSerialConnecting = deviceSerial;
    }

    public void setDevicePairingKeyConnecting(String pairingKey)
    {
        mDevicePairingKeyConnecting = pairingKey;
    }

    public void setDeviceAddressConnecting(String deviceAddress)
    {
        mDeviceAddressConnecting = deviceAddress;
    }
}
