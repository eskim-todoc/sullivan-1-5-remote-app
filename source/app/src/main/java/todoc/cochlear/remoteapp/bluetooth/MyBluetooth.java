package todoc.cochlear.remoteapp.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.params.AppParam;

public class MyBluetooth {
    private static MyBluetooth mInstance = new MyBluetooth();

    private String TAG = "TODOC_" + MyBluetooth.class.getSimpleName();

    private String BT_NAME_FILTER_REGEX = "^TD_.*$";

    private ParcelUuid SERVICE_DATA_UUID = new ParcelUuid(UUID.fromString("00004944-0000-1000-8000-00805F9B34FB"));

    private int SCAN_HANDLER_DELAY_TIME_IN_MS = 2000;

    private int SCAN_STATE_STOPPED = 0;
    private int SCAN_STATE_STARTED = 1;

    private int CONNECTION_STATE_DISCONNECTED = 0;
    private int CONNECTION_STATE_CONNECTING = 1;
    private int CONNECTION_STATE_CONNECTED = 2;

    BluetoothAdapter mBtAdapter;
    BluetoothDevice mBtDevice;
    BluetoothGatt mBtGatt;
    BluetoothLeScanner mLeScanner;

    int mScanState;
    int mConnectionState;

    Handler mScanHandler = new Handler();

    Runnable mScanRunner = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Scan Runner called.");

            int scanState, connectionState;

            if (mScanState == SCAN_STATE_STOPPED) {
                scanState = 0;
            } else {
                scanState = 1;
            }

            if (mConnectionState == CONNECTION_STATE_DISCONNECTED) {
                connectionState = 0;
            } else {
                connectionState = 2;
            }

            Log.d(TAG, "scanState + connectionState = " + (scanState + connectionState));

            switch (scanState + connectionState) {
                case 0: // stopped + disconnected
                    scan(true);
                    break;

                case 1: // started + disconnected
                    break;

                case 2: // stopped + not disconnected
                    break;

                case 3: // started + not disconnected
                    scan(false);
                    break;

                default:
                    break;
            }

            mScanHandler.postDelayed(mScanRunner, SCAN_HANDLER_DELAY_TIME_IN_MS);
        }
    };

    static public MyBluetooth getInstance() {
        return mInstance;
    }

    public void init() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mLeScanner = mBtAdapter.getBluetoothLeScanner();

        mScanState = SCAN_STATE_STOPPED;
        mConnectionState = CONNECTION_STATE_DISCONNECTED;

        Log.d(TAG, "init() -> mScanState = " + mScanState + ", mConnectionState = " + mConnectionState);
        mScanHandler.postDelayed(mScanRunner, 0);
    } // end - init();

    public void uninit() {
        mScanHandler.removeCallbacks(mScanRunner);
        scan(false);
    } // end - uninit();

    private void scan(boolean enable) {
        if (enable && mScanState == SCAN_STATE_STOPPED) {
            mLeScanner.stopScan(mScanCallback);
            mLeScanner.startScan(mScanCallback);
            mScanState = SCAN_STATE_STARTED;
            Log.d(TAG, "Scan started.");
        } else if (!enable && mScanState == SCAN_STATE_STARTED) {
            mLeScanner.stopScan(mScanCallback);
            mScanState = SCAN_STATE_STOPPED;
            Log.d(TAG, "Scan stopped.");
        }
    } // end - scan();

    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String name = result.getDevice().getName();

            if (name == null || !name.matches(BT_NAME_FILTER_REGEX)) {
                // If name is null or not matched to filter, return.
                return;
            }

            Map<ParcelUuid, byte[]> map = result.getScanRecord().getServiceData();

            if (map == null) {
                // If no service data available, return.
                return;
            }

            byte[] serviceBytes = map.get(SERVICE_DATA_UUID);

            if (serviceBytes == null) {
                // If no service data matched to SERVICE_DATA_UUID, return.
                return;
            }

            String serviceString = new String(serviceBytes);

            EntityUser user = AppParam.getInstance().databaseUsers.daoUsers().getDbUserByDefaultUSer("Y");
            List<EntityDevice> devices = AppParam.getInstance().databaseDevices.daoDevices().findAll();

            if (user == null || devices == null) {
                return;
            }

            for (EntityDevice device : devices) {
                String targetString = user.ear + "_" + user.name + "_" + device.serialNumber;

                if (serviceString.equals(targetString)) {
                    if (mConnectionState == CONNECTION_STATE_DISCONNECTED) {
                        Log.d(TAG, "Scanned service data = " + serviceString);
                        Log.d(TAG, "Target  service data = " + targetString);

                        mConnectionState = CONNECTION_STATE_CONNECTING;

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mConnectionState = CONNECTION_STATE_DISCONNECTED;
                            }
                        }, 5000);
                    }
                }
            }

        } // end - onScanResult();
    }; // end - mScanCallback
}
