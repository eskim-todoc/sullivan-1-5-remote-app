package todoc.cochlear.remoteapp.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;
import java.util.Set;

public class BtUtils
{
    private final static String TAG = "TD2_" + BtUtils.class.getSimpleName();
    private final static String METHOD_NAME_REMOVE_BOND = "removeBond";

    static private BtUtils mInstance = new BtUtils();

    //
    // 싱글톤 인스턴스
    //
    public static BtUtils getInstance()
    {
        return mInstance;
    }

    //
    // 입력받은 MAC 주소로 스마트폰에 본딩된 장치 중 일치하는 장치의 본딩 제거하는 함수
    //
    public void eraseBondedDeviceUsingMacAddress(String address, BluetoothAdapter adapter)
    {
        BluetoothDevice device = findBondedDeviceUsingMacAddress(address, adapter);

        if (device != null)
        {
            try
            {
                Log.d(TAG, "본딩된 장치 목록에서 " + device.getName() + "@" + device.getAddress() + " 장치를 제거합니다.");
                Method method = device.getClass().getMethod(METHOD_NAME_REMOVE_BOND, (Class[]) null);
                method.invoke(device, (Object[]) null);
            }
            catch (Exception e)
            {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    //
    // 입력받은 MAC 주소로 스마트폰에 본딩된 장치 중 일치하는 장치 찾기 함수
    //
    public BluetoothDevice findBondedDeviceUsingMacAddress(String address, BluetoothAdapter adapter)
    {
        if (adapter == null)
        {
            return null;
        }

        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();

        if (bondedDevices == null)
        {
            return null;
        }

        for (BluetoothDevice device : bondedDevices)
        {
            if (device.getAddress().equals(address))
            {
                Log.d(TAG, "본딩된 장치 목록에서 MAC 주소 " + address + "와 일치하는 장치를 찾았습니다.");
                return device;
            }
        }

        return null;
    }
}
