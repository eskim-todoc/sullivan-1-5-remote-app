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
import java.util.List;
import java.util.Set;

import todoc.cochlear.remoteapp.database.Device;
import todoc.cochlear.remoteapp.logging.LoggingUtils;
import todoc.cochlear.remoteapp.params.AppParam;

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
      // 본딩된 볼루투스 기기 목록 중, 앱 데이터베이스에 등록된 기기를 전부 본딩 제거하는 함수
      //
      public void eraseAllBondedDevices()
      {
            // 1. 본딩된 블루투스 기기 목록 읽기.
            Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

            // 2. 앱 데이터베이스에 등록된 기기 목록 읽기.
            List<Device> registeredDevices = null;

            if (AppParam.getInstance().database != null)
            {
                  registeredDevices = AppParam.getInstance().database.deviceDao().findAll();
            }

            // 3. 앱 데이터베이스에 등록된 기기 목록에 해당하는 본딩된 블루투스 기기를 찾아내고, 해당 기기의 본딩을 제거.
            if (registeredDevices != null && 0 < bondedDevices.size())
            {
                  for (Device registerDevice : registeredDevices)
                  {
                        for (BluetoothDevice bondedDevice : bondedDevices)
                        {
                              if (registerDevice.getDeviceMacAddress().equals(bondedDevice.getAddress()))
                              {
                                    try
                                    {
                                          Method m = bondedDevice.getClass().getMethod("removeBond", (Class[]) null);
                                          m.invoke(bondedDevice, (Object[]) null);
                                          Log.d(TAG, "본딩기기 " + bondedDevice.getName() + ", " + bondedDevice.getAddress() + "를 제거했습니다.");
                                          writeLogging(LoggingUtils.LOGGING_BOND_REMOVED,
                                                      "Success:"
                                                                  + "name=" + bondedDevice.getName()
                                                                  + ", addr=" + bondedDevice.getAddress());
                                    }
                                    catch (Exception e)
                                    {
                                          Log.d(TAG, "본딩기기 " + bondedDevice.getName() + ", " + bondedDevice.getAddress() + "의 제거를 실패했습니다.");
                                          Log.e(TAG, e.getMessage());
                                          writeLogging(LoggingUtils.LOGGING_BOND_REMOVED,
                                                      "Failed:"
                                                                  + "name=" + bondedDevice.getName()
                                                                  + ", addr=" + bondedDevice.getAddress());
                                    }
                              }
                        }
                  }
            }
      }

      //
      // 데이터베이스에 등록된 모든 사운드처리기를 찾아내고, 입력 받은 주소를 제외한 모든 기기의 본딩을 해제하는 함수
      //
      public void eraseAllBondedDevicesExceptAnAddress(String addr)
      {
            // 데이터베이스가 null이 아닐 때만 수행한다.
            if (AppParam.getInstance().database != null && addr != null)
            {
                  // 1. 데이터베이스에 등록된 사운드처리기 정보 읽기
                  List<Device> deviceList = AppParam.getInstance().database.deviceDao().findAll();

                  Log.d(TAG, "현재 등록되어 있는 기기 목록:");
                  for (Device device : deviceList)
                  {
                        Log.d(TAG, "주소 = " + device.getDeviceMacAddress() + ", 이름 = " + device.getDeviceName());
                  }

                  // 2. 본딩된 블루투스 기기정보 읽기
                  Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

                  Log.d(TAG, "현재 본딩되어 있는 기기 목록:");
                  for (BluetoothDevice device : bondedDevices)
                  {
                        Log.d(TAG, "주소 = " + device.getAddress() + ", 이름 = " + device.getName());
                  }

                  // 3. 데이터베이스에 등록된 사운드처리기 정보를 가지고, 본딩된 블루투스 기기정보에서 일치하는 기기를 찾아낸다.
                  // 본딩된 블루투스 기기정보가 1개 이상일 때만 수행한다.
                  if (0 < bondedDevices.size())
                  {
                        for (int i = 0; i < deviceList.size(); i++)
                        {
                              for (BluetoothDevice device : bondedDevices)
                              {
                                    try
                                    {
                                          // 입력받은 주소와 같을 때는 무시해야 한다.
                                          if (device.getAddress().equals(addr))
                                          {
                                                Log.d(TAG, "본딩 장치(주소=" + device.getAddress() + ", 이름 = " + device.getName() + ")는 입력 받은 주소와 일치하여 본딩 해제를 생략합니다.");
                                          }
                                          else
                                          {
                                                if (deviceList.get(i).getDeviceMacAddress().equals(device.getAddress()))
                                                {
                                                      Log.d(TAG, "본딩 장치(주소=" + device.getAddress() + ", 이름 = " + device.getName() + ")의 본딩을 해제합니다.");
                                                      Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                                                      m.invoke(device, (Object[]) null);
                                                }
                                          }
                                    }
                                    catch (Exception e)
                                    {
                                          Log.d(TAG, "본딩 장치(주소=" + device.getAddress() + ")의 본딩 해제를 실패했습니다.");
                                          Log.e(TAG, e.getMessage());
                                    }
                              }

                        }
                  }
            }
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
                        Method method = device.getClass().getMethod(METHOD_NAME_REMOVE_BOND, (Class[]) null);
                        method.invoke(device, (Object[]) null);
                        Log.d(TAG, "본딩기기 " + device.getName() + ", " + device.getAddress() + "를 제거했습니다.");
                        writeLogging(LoggingUtils.LOGGING_BOND_REMOVED,
                                    "Success:" + "name=" + device.getName() + ", addr=" + device.getAddress());
                  }
                  catch (Exception e)
                  {
                        Log.d(TAG, "본딩기기 " + device.getName() + ", " + device.getAddress() + "의 제거를 실패했습니다.");
                        Log.e(TAG, e.getMessage());
                        writeLogging(LoggingUtils.LOGGING_BOND_REMOVED,
                                    "Failed:" + "name=" + device.getName() + ", addr=" + device.getAddress());
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

      private void writeLogging(int type, String message)
      {
            LoggingUtils.getInstance().writeMessage(LoggingUtils.getInstance().typeMessage(type) + " : " + message);
      }
}
