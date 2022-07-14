package todoc.cochlear.remoteapp.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Vibrator;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.bluetooth.BtUtils;
import todoc.cochlear.remoteapp.bluetooth.MyBluetooth;
import todoc.cochlear.remoteapp.database.Device;
import todoc.cochlear.remoteapp.database.devices.DatabaseDevices;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.users.DatabaseUsers;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.fragment.AddDeviceFragment;
import todoc.cochlear.remoteapp.fragment.AddUserFragment;
import todoc.cochlear.remoteapp.fragment.DeviceFragment;
import todoc.cochlear.remoteapp.fragment.EditDeviceFragment;
import todoc.cochlear.remoteapp.fragment.EditUserFragment;
import todoc.cochlear.remoteapp.fragment.HomeFragment;
import todoc.cochlear.remoteapp.fragment.ManualFragment;
import todoc.cochlear.remoteapp.fragment.PasswordFragment;
import todoc.cochlear.remoteapp.fragment.RemoteControlFragment;
import todoc.cochlear.remoteapp.fragment.SettingsFragment;
import todoc.cochlear.remoteapp.fragment.UserFragment;
import todoc.cochlear.remoteapp.logging.LoggingUtils;
import todoc.cochlear.remoteapp.params.ActionMessage;
import todoc.cochlear.remoteapp.params.AppParam;
import todoc.cochlear.remoteapp.params.DeviceParam;
import todoc.cochlear.remoteapp.service.TerminationService;
import todoc.cochlear.remoteapp.shared_preferences.LockScreen;
import todoc.cochlear.remoteapp.shared_preferences.ManualScreen;
import todoc.cochlear.remoteapp.view_model.BleViewModel;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "TD2_" + MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_BLUETOOTH_ENABLE = 1;
    private static final int REQUEST_PERMISSION_FINE_LOCATION = 100;

    public static final ParcelUuid BLE_ADVERTISING_SERVICE_UUID = new ParcelUuid(UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"));
    public static final UUID BLE_UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID BLE_UUID_CHARACTERISTIC_CLIENT_TO_SERVER = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID BLE_UUID_CHARACTERISTIC_SERVER_TO_CLIENT = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    static private String BT_NAME_REGEX_FILTER = "^TD_.*$";
    static private ParcelUuid SERVICE_DATA_UUID = new ParcelUuid(UUID.fromString("00004944-0000-1000-8000-00805F9B34FB"));
    public static final UUID BLE_UUID_SCAN_RSP = UUID.fromString("00004944-0000-1000-8000-00805F9B34FB");

    //public static final ParcelUuid BLE_ADVERTISING_SERVICE_UUID = new ParcelUuid(UUID.fromString("6f400001-b5a3-f393-e0a9-e50e24dcca9e"));
    //public static final UUID BLE_UUID_SERVICE = UUID.fromString("6f400001-b5a3-f393-e0a9-e50e24dcca9e");
    //public static final UUID BLE_UUID_CHARACTERISTIC_CLIENT_TO_SERVER = UUID.fromString("6f400002-b5a3-f393-e0a9-e50e24dcca9e");
    //public static final UUID BLE_UUID_CHARACTERISTIC_SERVER_TO_CLIENT = UUID.fromString("6f400003-b5a3-f393-e0a9-e50e24dcca9e");

    public static final UUID BLE_UUID_DESCRIPTION_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int DELAY_IN_MS_FOR_BLUETOOTH_LE_SCANNER = 15000;
    private static final int DELAY_IN_MS_FOR_DISCOVER_SERVICES_TIMEOUT = 2000;
    private static final int DELAY_IN_MS_FOR_SEND_PACKET = 50;
    private static final int DELAY_IN_MS_FOR_PACKET_RESPONSE_TIMEOUT = 500;
    private static final int DELAY_IN_MS_FOR_BATTERY_CHECKING = 30000;

    //private static final int DELAY_IN_MS_FOR_LONG_TIME_IDLE = 10000; // 10 seconds.
    private static final int DELAY_IN_MS_FOR_LONG_TIME_IDLE = 600000; // 10 minutes.

    public static final byte PACKET_HEADER_PASSWORD = (byte) (0x40 & 0xff);
    public static final byte PACKET_HEADER_SOUND_PROCESSOR_INFO = (byte) (0x41 & 0xff);
    public static final byte PACKET_HEADER_SOUND_PROCESSOR_STATUS = (byte) (0x42 & 0xff);
    public static final byte PACKET_HEADER_VALUE_PROMGRAM = (byte) (0x43 & 0xff);
    public static final byte PACKET_HEADER_VALUE_SENSITIVITY = (byte) (0x44 & 0xff);
    public static final byte PACKET_HEADER_VALUE_VOLUME = (byte) (0x45 & 0xff);
    public static final byte PACKET_HEADER_VALUE_TELECOIL = (byte) (0x46 & 0xff);
    public static final byte PACKET_HEADER_VALUE_SIMULATION = (byte) (0x47 & 0xff);
    public static final byte PACKET_HEADER_VALUE_LED = (byte) (0x48 & 0xff);

    public static final byte PACKET_HEADER_MAP_INFO = (byte) (0x52 & 0xff);
    public static final byte PACKET_HEADER_ISD_USER_NAME = (byte) (0x53 & 0xff);

    //public static final byte PACKET_HEADER_VALUE_POWER_MODE = (byte) (0x99 & 0xff);
    public static final byte PACKET_HEADER_VALUE_WARNNING = (byte) (0x4B & 0xff);
    public static final byte PACKET_HEADER_ERROR = (byte) (0xf0 & 0xff);

    // Bluetooth
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner mBluetoothLeScanner;
    BluetoothDevice mBluetoothDevice;
    public BluetoothGatt mBluetoothGatt;
    BluetoothGattService mBluetoothGattService;
    BluetoothGattCharacteristic mCharClientToServer;
    BluetoothGattCharacteristic mCharServerToClient;

    public ActivityMainBinding mBinding;

    public ManualScreen mManualScreen;

    public DatabaseUsers mDatabaseUsers;
    public DatabaseDevices mDatabaseDevices;

    private BleViewModel mBleViewModel;
    private StatusViewModel mStatusViewModel;

    private String[] mUserList;
    private int mCheckItem;

    //
    // Long time idle state handler
    //
    Handler longTimeIdleHandler = new Handler();

    //
    // Long time idle state runner
    //
    Runnable longTimeIdleRunner = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d(TAG, "longTimeIdleRunner() called.");
            AppParam.getInstance().longTimeIdle.setValue(true);
        }
    };

    //
    // Long time idle handler updater
    //
    public void updateLongTimeIdleHandler()
    {
        Log.d(TAG, "updateLongTimeIdleHandler() called.");
        //longTimeIdleHandler.removeCallbacks(longTimeIdleRunner);
        //longTimeIdleHandler.postDelayed(longTimeIdleRunner, DELAY_IN_MS_FOR_LONG_TIME_IDLE);
    }

    /**
     * Callback - onDestroy
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        Log.d(TAG, "onDestroy() called.");

        //
        // 블루투스 핸들러 테스트
        //
        MyBluetooth.getInstance().uninit();

        AppParam appParam = AppParam.getInstance();

        AppParam.getInstance().willBeAppExit = true;

        Log.d(TAG, "onDestroy : Unregister broadcast receiver.");
        myUnregisterReceiver(); // Unregister broadcast receiver

        // Close Database
        if (appParam.database != null && appParam.database.isOpen())
        {
            Log.d(TAG, "onDestroy : Close database.");
            appParam.database.close();
            appParam.database = null;
        }

        LoggingUtils.getInstance().closeLoggingDatabase(getApplicationContext()); // 로깅 데이터베이스 닫기.

        //if (appParam.isTerminationServiceStarted)
        //{
        //Intent intent = new Intent(this, TerminationService.class);
        //stopService(intent);
        //}

        if (appParam.isBleScanning)
        {
            Log.d(TAG, "onDestroy : Stop BLE scanning.");
            scanLe(false);
        }

        if (mBluetoothGatt != null && mBluetoothDevice != null && AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
        {
            Log.d(TAG, "onDestroy : Disconnect BLE connection.");
            appParam.isDisconnectedByUser = true;
            mBluetoothGatt.disconnect();
        }

        AppParam.getInstance().isAppLockState = true;

        longTimeIdleHandler.removeCallbacks(longTimeIdleRunner); // Remove long time idle handler
    }

    /**
     * Callback - onCreate
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = mBinding.getRoot();
        setContentView(view);

        // Navigation bar color setting
        getWindow().setNavigationBarColor(getColor(R.color.color_primary_variant));

        // Init Tool bar.
        initToolBar();

        // Hide toolbar navigation icon.
        showToolBarNavigationIcon(false);

        // Ble view model.
        mBleViewModel = new ViewModelProvider(this).get(BleViewModel.class);

        // Status view model.
        mStatusViewModel = new ViewModelProvider(this).get(StatusViewModel.class);

        // Database for Users
        if (mDatabaseUsers == null)
        {
            mDatabaseUsers = Room.databaseBuilder(getApplicationContext(), DatabaseUsers.class, DatabaseUsers.DATABASE_NAME).addCallback(new RoomDatabase.Callback()
            {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db)
                {
                    super.onCreate(db);
                    db.execSQL(DatabaseUsers.DATABASE_ENCODING);
                }
            }).fallbackToDestructiveMigration().allowMainThreadQueries().build();
        }

        // Database for Devices
        if (mDatabaseDevices == null)
        {
            mDatabaseDevices = Room.databaseBuilder(getApplicationContext(), DatabaseDevices.class, DatabaseDevices.DATABASE_NAME).addCallback(new RoomDatabase.Callback()
            {
                @Override
                public void onCreate(@NonNull SupportSQLiteDatabase db)
                {
                    super.onCreate(db);
                    db.execSQL(DatabaseDevices.DATABASE_ENCODING);
                }
            }).fallbackToDestructiveMigration().allowMainThreadQueries().build();
        }

        //
        // 블루투스 핸들러 테스트
        //
        //MyBluetooth.getInstance().init();

        AppParam.getInstance().bleConnectionState = AppParam.BLE_CONNECTION_STATE_DISCONNECTED;

        Log.d(TAG, "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
        Log.d(TAG, "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
        Log.d(TAG, "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
        Log.d(TAG, "onCreate() called.");
        Log.d(TAG, AppParam.getInstance().toString());

        AppParam.getInstance().callCounter++;
        Log.d(TAG, "after callCounter++");
        Log.d(TAG, AppParam.getInstance().toString());

        DeviceParam deviceParam = new DeviceParam();
        Log.d(TAG, deviceParam.toString());

        ActionMessage actionMessage = new ActionMessage();
        Log.d(TAG, actionMessage.toString());

        // Check permissions
        if (grantPermissions())
        {
            // Check Bluetooth enabling/disabling
            if (enableBluetooth())
            {
                initMainActivity(); // Initiailze all of MainActivity
            }
        }
    } // onCreate

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Activity initialization
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initialize MainActivity (View, Database, Bluetooth, ...)
     */
    public void initMainActivity()
    {
        // Register broadcast receiver
        myRegisterReceiver();

        // Start termination service
        if (!AppParam.getInstance().isTerminationServiceStarted)
        {
            Log.d(TAG, "Try to start termination service.");
            Intent intent = new Intent(this, TerminationService.class);
            intent.setAction(TerminationService.ACTION_START_SERVICE);
            startService(intent);
        }
        else
        {
            Log.d(TAG, "Termination service created previously is not finished.");
        }

        // Initialize Bluetooth
        initBluetooth();

        // Shared Preferences for Manual Screen.
        if (mManualScreen == null)
        {
            mManualScreen = new ManualScreen(this);
        }

        if (mManualScreen.isEnabled())
        {
            Bundle bundle = new Bundle();
            bundle.putBoolean("firstScreen", true);
            ManualFragment manualFragment = new ManualFragment();
            manualFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, manualFragment).commitAllowingStateLoss();
        }
        else // If the user manual screen is disabled, the first screen must be the remote control screen.
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new RemoteControlFragment()).commitAllowingStateLoss();
        }

        // 로깅 관련 초기화
        LoggingUtils.getInstance().openLoggingDatabase(getApplicationContext()); // 로깅 데이터베이스 불러오기 + 번호 ROW 초기화.
        //LoggingUtils.getInstance().writeMessage(LoggingUtils.getInstance().getCurrentDate()); // 테스트 메시지 입력
        //LoggingUtils.getInstance().printAllMessages(); // 테스트 메시지 출력
    } // initMainActivity

    /**
     * 앱 완전 초기화 함수
     */
    public void initAllForApp()
    {
        // 1. 본딩된 블루투스 기기 목록 중, 앱 데이터베이스에 등록된 기기를 전부 본딩 제거하기.
        BtUtils.getInstance().eraseAllBondedDevices();

        // 2. 앱 데이터 베이스의 모든 사운드처리기를 삭제
        List<Device> devices = AppParam.getInstance().database.deviceDao().findAll();

        for (Device device : devices)
        {
            Log.d(TAG, "사운드처리기 " + device.getDeviceName() + ", " + device.getDeviceMacAddress() + "를 삭제합니다.");
            AppParam.getInstance().database.deviceDao().delete(device);

            writeMessage(LoggingUtils.LOGGING_DEVICE_REMOVED, "name=" + device.getDeviceName() + ", addr=" + device.getDeviceMacAddress() + ", serial=" + device.getDeviceSerial() + ", user=" + device.getImplantUserName()); // Logging
        }

        // 3. 데이터베이스 다시 로드하여 현재 정보 업데이트
        AppParam.getInstance().registeredDevices = AppParam.getInstance().database.deviceDao().findAll();
        Log.d(TAG, "데이터베이스 다시 로드");

        // 7. 화면을 home 화면으로 변경
        getSupportFragmentManager().beginTransaction().replace(R.id.frame, new HomeFragment()).commitAllowingStateLoss();
        Log.d(TAG, "Home 화면으로 설정");

        // 10. BLE 연결 중인 장치가 있다면 연결 해제 및 본딩이 되어 있다면 본딩 제거
        if (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED && mBluetoothDevice != null && mBluetoothGatt != null)
        {
            String addr = mBluetoothDevice.getAddress();

            AppParam.getInstance().isDisconnectedByUser = true;
            mBluetoothGatt.disconnect();
            Log.d(TAG, "연결된 장치가 있으므로 연결해제");

            Log.d(TAG, "본딩이 되어 있다면 본딩 제거");
            BtUtils.getInstance().eraseBondedDeviceUsingMacAddress(addr, BluetoothAdapter.getDefaultAdapter());
        }

        // 11. 앱 비밀번호 플래그 설정
        AppParam.getInstance().doesNeedToRegisterAppLockPassword = true;
        AppParam.getInstance().doesNeedToVerifyAppLockPassword = false;
        ConstraintLayout passLockLayout = findViewById(R.id.lock_screen); // 잠금 화면 켜기
        passLockLayout.setVisibility(View.VISIBLE);
        //((TextView) findViewById(R.id.main_applock_desc)).setText("의료기기 사이버 보안을 위해 등록할 앱 비밀번호를 입력해주세요."); // 텍스트뷰 설정
        Log.d(TAG, "앱 비밀번호 플래그 설정 완료");

        // 13. 확인 다이얼로그 생성
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.MyAlertDialogTheme);

        builder.setMessage("앱에 등록된 모든 사운드처리기의 정보를 삭제하고 앱 비밀번호를 초기화 하였습니다.");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                scanLe(false);
            }
        });
        AppParam.getInstance().lastDialog = builder.create();
        AppParam.getInstance().lastDialog.show();
        Log.d(TAG, "앱 비밀번호 초기화 확인 다이얼로그 생성");

        writeMessage(LoggingUtils.LOGGING_INIT_ALL_FOR_APP, "Init App"); // Logging
    }

    /**
     * Callback - 잠금 해제 화면에서 앱 비밀번호 초기화 버튼의 콜백 함수
     */
    public void onClickAppLockReset(View view)
    {
        updateLongTimeIdleHandler(); // Update long time idle handler
        Log.d(TAG, "앱 비밀번호 초기화 버튼 클릭!");
        vibrator(5);

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                android.app.AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.MyAlertDialogTheme);

                builder.setMessage("앱에 등록된 모든 사운드처리기의 정보를 삭제하고 앱 비밀번호를 초기화 할 수 있습니다. 비밀번호를 초기화 하시겠습니까?");
                builder.setPositiveButton("네", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        initAllForApp();
                    }
                });
                builder.setNegativeButton("아니오", null);
                AppParam.getInstance().lastDialog = builder.create();
                AppParam.getInstance().lastDialog.show();
                Log.d(TAG, "앱 비밀번호 초기화 다이얼로그 생성");
            }
        });
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        AppParam.getInstance().isAppLockState = true;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (AppParam.getInstance().lastDialog != null)
        {
            if (AppParam.getInstance().lastDialog.isShowing())
            {
                AppParam.getInstance().lastDialog.dismiss();
            }
        }

        // Check lock screen password whether registered or not.
        LockScreen.resume(this, mBinding);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Permission
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Callback - Request permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_FINE_LOCATION)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                if (enableBluetooth())
                {
                    initMainActivity(); // Initiailze all of MainActivity
                }
            }
            else
            {
                finish();
            }
        }
    }

    /**
     * Grant permissions
     */
    public boolean grantPermissions()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_FINE_LOCATION);

            return false;
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Broadcast Receiver
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Register broadcast receiver
     */
    private void myRegisterReceiver()
    {
        AppParam appParam = AppParam.getInstance();

        if (!appParam.isBroadcastReceiverRegistered)
        {
            registerReceiver(mBroadcastReceiver, makeIntentFilter());

            appParam.isBroadcastReceiverRegistered = true;

            Log.d(TAG, "Finished registering broadcast receiver.");
        }
    }

    /**
     * Unregister broadcast receiver
     */
    private void myUnregisterReceiver()
    {
        AppParam appParam = AppParam.getInstance();

        if (appParam.isBroadcastReceiverRegistered)
        {
            unregisterReceiver(mBroadcastReceiver);

            appParam.isBroadcastReceiverRegistered = false;

            Log.d(TAG, "Finished unregistering broadcast receiver.");
        }
    }

    /**
     * Broadcast receiver filter maker
     */
    private IntentFilter makeIntentFilter()
    {
        IntentFilter intentFilter = new IntentFilter();

        // Bluetooth globally...
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); // For Bluetooth state change
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);

        // Activity...
        intentFilter.addAction(ActionMessage.ACTIVITY_FINISH);

        // BLE...
        intentFilter.addAction(ActionMessage.BLE_SCAN_START);
        intentFilter.addAction(ActionMessage.BLE_SCAN_STOP);
        intentFilter.addAction(ActionMessage.BLE_SCAN_RESTART);
        intentFilter.addAction(ActionMessage.BLE_CONNECT);
        intentFilter.addAction(ActionMessage.BLE_DISCONNECT);

        // Fragment...
        intentFilter.addAction(ActionMessage.NEW_FRAGMENT_HOME);
        intentFilter.addAction(ActionMessage.NEW_FRAGMENT_HOME_WITH_STOP_BLE_SCAN);
        intentFilter.addAction(ActionMessage.NEW_FRAGMENT_PASSWORD);
        intentFilter.addAction(ActionMessage.NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE);

        // Database...
        intentFilter.addAction(ActionMessage.DATABASE_CHECK_EMPTY);

        // 사용자 관리 화면 전환
        intentFilter.addAction(ActionMessage.NEW_FRAGMENT_USER);
        intentFilter.addAction(ActionMessage.NEW_FRAGMENT_MANAGEMENT_USER);
        intentFilter.addAction(ActionMessage.NEW_FRAGMENT_MANAGEMENT_DEVICE);

        return intentFilter;
    }

    /**
     * Broadcast receiver
     */
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            Log.d(TAG, "Received action : " + action);

            // Detect disabling Bluetooth
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_OFF)
                {
                    finish();
                }
            } // BluetoothAdapter.ACTION_STATE_CHANGED
            else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST))
            {
                // Do not use... If use, app wiil be confused.
                //String pairingPin = mBleViewModel.getDevicePairingKeyConnecting();
                //Log.d(TAG, "Set pairing pin : " + pairingPin);
                //mBluetoothDevice.setPin(pairingPin.getBytes());
            }
            else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    Log.d(TAG, "BOND_BONDED, {" + device.getAddress() + "}");

                    if (0 < mBleViewModel.getBondFailCounter())
                    {
                        mBleViewModel.clearBondFailCounter();

                        Log.d(TAG, "0 < Bond_Fail_Counter -> So, retry connection process from discover services.");

                        // Make handler to check timeout for discovering services.
                        discoverServiceTimeoutHandler.postDelayed(discoverServiceTimeoutRunner, DELAY_IN_MS_FOR_DISCOVER_SERVICES_TIMEOUT);

                        if (!mBluetoothGatt.discoverServices())
                        {
                            mBluetoothGatt.disconnect();
                        }
                    }
                }
                else if (device.getBondState() == BluetoothDevice.BOND_BONDING)
                {
                    Log.d(TAG, "BOND_BONDING, {" + device.getAddress() + "}");
                    mBleViewModel.setBondState(BluetoothDevice.BOND_BONDING);
                }
                else if (device.getBondState() == BluetoothDevice.BOND_NONE)
                {
                    Log.d(TAG, "BOND_NONE, {" + device.getAddress() + "} -> This means that bonding was might failed.");

                    mBleViewModel.increaseBondFailCounter();

                    if (mBluetoothGatt != null)
                    {
                        mBluetoothGatt.disconnect();
                    }
                }
            }
            // ACTIVITY_FINISH
            else if (action.equals(ActionMessage.ACTIVITY_FINISH))
            {
                finish();
            }
            // BLE_SCAN_START
            else if (action.equals(ActionMessage.BLE_SCAN_START))
            {
                scanLe(true);
            }
            // BLE_SCAN_STOP
            else if (action.equals(ActionMessage.BLE_SCAN_STOP))
            {
                scanLe(false);
            }
            // BLE_SCAN_RESTART
            else if (action.equals(ActionMessage.BLE_SCAN_RESTART))
            {
                scanLe(false);
                scanLe(true);
            }
            // BLE_CONNECT
            else if (action.equals(ActionMessage.BLE_CONNECT))
            {
                Log.d(TAG, "Number of connected device before try to connect is " + mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT).size() + ".");
                Log.d(TAG, "MAC address want to connect is " + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress() + ".");

                mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(AppParam.getInstance().currentConnectDevice.getDeviceMacAddress());

                if (mBluetoothDevice != null)
                {
                    mBluetoothGatt = mBluetoothDevice.connectGatt(getApplicationContext(), false, mGattCallback);

                    if (mBluetoothGatt == null)
                    {
                        Log.d(TAG, "Bluetooth GATT instance is null.");
                    }
                }
                else
                {
                    Log.d(TAG, "Bluetooth device instance is null.");
                }
            }
            // BLE_DISCONNECT
            else if (action.equals(ActionMessage.BLE_DISCONNECT))
            {
                if (mBluetoothGatt != null && mBluetoothDevice != null && AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                {
                    Log.d(TAG, "장치 연결을 해제합니다.");
                    mBluetoothGatt.disconnect();
                    //mBluetoothGatt.close();
                    //mBluetoothGatt = null;
                    //mBluetoothDevice = null;
                    //mBleConnectionState = BLE_CONNECTION_STATE_DISCONNECTED;
                    //AppParam.getInstance().currentConnectDevice = null;
                    //AppParam.getInstance().isBusy = false;
                }
            }
            // FRAGMENT HOME
            else if (action.equals(ActionMessage.NEW_FRAGMENT_HOME))
            {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame, new HomeFragment()).commitAllowingStateLoss();
            }
            // FRAGMENT HOME + STOP BLE SCAN
            else if (action.equals(ActionMessage.NEW_FRAGMENT_HOME_WITH_STOP_BLE_SCAN))
            {
                scanLe(false);
                getSupportFragmentManager().beginTransaction().replace(R.id.frame, new HomeFragment()).commitAllowingStateLoss();
            }
            // FRAGMENT PASSWORD
            else if (action.equals(ActionMessage.NEW_FRAGMENT_PASSWORD))
            {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame, new PasswordFragment()).commitAllowingStateLoss();
            }
            // DATABASE CHECK EMPTY
            else if (action.equals(ActionMessage.DATABASE_CHECK_EMPTY))
            {
                //AppParam.getInstance().registeredDevices = AppParam.getInstance().database.deviceDao().findAll();

                //if (AppParam.getInstance().registeredDevices.size() == 0)
                if (AppParam.getInstance().databaseUsers.daoUsers().findAll().size() == 0)
                {
                    //enableScreenNoDeviceRegistered(true);
                }
            }
        } // onReceive
    };

    /**
     * 백버튼 콜백 메소드.
     */
    @Override
    public void onBackPressed()
    {

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);

        if (fragment instanceof RemoteControlFragment)
        {
            new MaterialAlertDialogBuilder(this, R.style.add_user_screen_dialog).setTitle("주의").setMessage("앱을 종료하시겠습니까?").setPositiveButton("종료", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    finish();
                }
            }).setNegativeButton("취소", null).show();
        }
        else if (fragment instanceof SettingsFragment)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new RemoteControlFragment()).commitAllowingStateLoss();
        }
        else if (fragment instanceof UserFragment)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
        }
        else if (fragment instanceof AddUserFragment)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new UserFragment()).commitAllowingStateLoss();
        }
        else if (fragment instanceof EditUserFragment)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new UserFragment()).commitAllowingStateLoss();
        }
        else if (fragment instanceof DeviceFragment)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
        }
        else if (fragment instanceof AddDeviceFragment)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new DeviceFragment()).commitAllowingStateLoss();
        }
        else if (fragment instanceof EditDeviceFragment)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new DeviceFragment()).commitAllowingStateLoss();
        }
        else if (fragment instanceof ManualFragment)
        {
            Bundle bundle = ((ManualFragment) fragment).getArguments();
            if (bundle != null && bundle.getBoolean("firstScreen"))
            {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame, new RemoteControlFragment()).commitAllowingStateLoss();
            }
            else
            {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Toolbar
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Bluetooth
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Callback - Activity results
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case REQUEST_CODE_BLUETOOTH_ENABLE:
            {
                if (resultCode != RESULT_OK)
                {
                    finish();
                }
                else
                {
                    initMainActivity();
                }
            }
            break;
        }
    }

    /**
     * Enable Bluetooth
     */
    public boolean enableBluetooth()
    {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled())
        {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_CODE_BLUETOOTH_ENABLE);
            return false;
        }

        return true;
    }

    /**
     * Get objects related with Bluetooth.
     */
    public void initBluetooth()
    {
        // uses-feature
        // android:name="android.hardware.bluetooth_le"
        // android:required="true"

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.stopScan(mScanCallback); // Stop scan if scanning

        AppParam.getInstance().willBeAppExit = false;
        AppParam.getInstance().isBleBusy = false;

        mBleViewModel.getObjectSearching().observe(this, mSearchingObserver);
        mBleViewModel.getObjectBleConnection().observe(this, mSearchingObserver);
    }

    Observer mSearchingObserver = new Observer()
    {
        @Override
        public void onChanged(Object o)
        {
            int connection = mBleViewModel.getBleConnection();
            int searching = mBleViewModel.getSearching();

            if (searching == BleViewModel.SEARCHING_ENABLED)
            {
                if (connection == BleViewModel.BLE_DISCONNECTED)
                {
                    Log.d(TAG, "Searching Observer : " + "SEARCHING_ENABLED + BLS_DISCONNECTED");
                    scanLe(true);
                }
                else if (connection == BleViewModel.BLE_CONNECTING || connection == BleViewModel.BLE_CONNECTED)
                {
                    Log.d(TAG, "Searching Observer : " + "SEARCHING_ENABLED + BLE_CONNECT*");
                    scanLe(false);
                }
            }
            else if (searching == BleViewModel.SEARCHING_DISABLED)
            {
                if (connection == BleViewModel.BLE_DISCONNECTED)
                {
                    Log.d(TAG, "Searching Observer : " + "SEARCHING_DISABLED + BLE_DISCONNECTED");
                    scanLe(false);
                }
                else if (connection == BleViewModel.BLE_CONNECTING || connection == BleViewModel.BLE_CONNECTED)
                {
                    Log.d(TAG, "Searching Observer : " + "SEARCHING_DISABLED + BLE_CONNECT*");
                    scanLe(false);
                }
            }
        }
    };

    /**
     * Handler for scan timeout.
     */
    Handler scanHandler = new Handler();

    /**
     * Runner for scan timeout.
     */
    Runnable scanRunner = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d(TAG, "scanRunner : Bluetooth LE scan timeout.");

            scanLe(false); // Stop scan
            scanLe(true); // Restart scan
        }
    }; // scanRunner

    //
    // Start/stop BLE scan.
    //
    public void scanLe(boolean enable)
    {
        if (enable)
        {
            mBluetoothLeScanner.startScan(mScanCallback);
            Log.d(TAG, "scanLE : Bluetooth LE scan started.");
        }
        else
        {
            mBluetoothLeScanner.stopScan(mScanCallback);
            Log.d(TAG, "scanLE : Bluetooth LE scan stopped.");
        }
        /*
        if (enable && (mBleViewModel.getScanState() == BleViewModel.SCAN_STATE.STOPPED))
        {
            mBleViewModel.setScanState(BleViewModel.SCAN_STATE.SCANNING);
            mBluetoothLeScanner.startScan(mScanCallback);
            Log.d(TAG, "scanLE : Bluetooth LE scan started.");
        }
        else if ((!enable) && (mBleViewModel.getScanState() == BleViewModel.SCAN_STATE.SCANNING))
        {
            scanHandler.removeCallbacks(scanRunner); // Remove callback for scan handler.
            mBluetoothLeScanner.stopScan(mScanCallback); // Stop scan.
            mBleViewModel.setScanState(BleViewModel.SCAN_STATE.STOPPED);
            Log.d(TAG, "scanLE : Bluetooth LE scan stopped.");
        }
        */
    }

    /**
     * Bluetooth LE scan callback.
     */
    ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            if (mBinding.lockScreen.getVisibility() == View.VISIBLE)
            {
                // If lock screen is enabled, return.
                return;
            }

            String name = result.getDevice().getName();

            if (name == null || !name.matches(BT_NAME_REGEX_FILTER))
            {
                // If the name is null or not mathed to the regex filter, return.
                return;
            }

            Map<ParcelUuid, byte[]> map = result.getScanRecord().getServiceData();

            if (map == null)
            {
                // If no service data available, return.
                return;
            }

            byte[] serviceBytes = map.get(SERVICE_DATA_UUID);

            if (serviceBytes == null)
            {
                // If no service data matched to SERVICE_DATA_UUID, return.
                return;
            }

            String serviceString = new String(serviceBytes);

            Log.d(TAG, "BLE Scan Result : NAME = " + name + ", SCAN RESPONSE = " + serviceString);

            EntityUser defaultUser = mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);
            List<EntityDevice> devices = mDatabaseDevices.daoDevices().findAll();
            EntityDevice targetDevice = null;

            if (defaultUser == null || devices == null)
            {
                // If no user or device available, return.
                return;
            }

            boolean isFound = false;

            for (EntityDevice device : devices)
            {
                String targetString = defaultUser.ear + "_" + defaultUser.name + "_" + device.serialNumber;

                if (targetString.equals(serviceString))
                {
                    Log.d(TAG, "Found Target Device : " + targetString);
                    isFound = true;
                    targetDevice = device;
                    break;
                }
            }

            if (isFound)
            {
                if (mBleViewModel.getConnState() == BleViewModel.CONN_STATE.DISCONNECTED)
                {
                    if (!mBleViewModel.getUserNameConnecting().equals(defaultUser.name) ||
                            !mBleViewModel.getDeviceSerialConnecting().equals(targetDevice.serialNumber))
                    {
                        Log.d(TAG, "Previous User name = " + mBleViewModel.getUserNameConnecting() + ", new User name = " + defaultUser.name);
                        Log.d(TAG, "Previous Device serial = " + mBleViewModel.getDeviceSerialConnecting() + ", new Device serial = " + targetDevice.serialNumber);

                        mBleViewModel.clearBondFailCounter();
                    }

                    //mBleViewModel.setBleConnection(BleViewModel.BLE_CONNECTING);
                    //scanLe(false);

                    mBleViewModel.setUserNameConnecting(defaultUser.name);
                    mBleViewModel.setUserEarConnecting(defaultUser.ear);
                    mBleViewModel.setUserPasswordConnecting(defaultUser.passKey);
                    mBleViewModel.setDeviceSerialConnecting(targetDevice.serialNumber);
                    mBleViewModel.setDevicePairingKeyConnecting(targetDevice.pairingKey);
                    mBleViewModel.setDeviceAddressConnecting(result.getDevice().getAddress());

                    mBluetoothDevice = result.getDevice();
                    mBluetoothGatt = mBluetoothDevice.connectGatt(getApplicationContext(), false, mGattCallback);

                    if (mBluetoothGatt == null)
                    {
                        Log.d(TAG, "Bluetooth GATT instance is NULL -> connectGatt failed.");
                        //scanLe(true);
                    }
                }
            }
        } // onScanResult
    }; // scanCallback

    /**
     * Hnadler for battery checking.
     */
    Handler checkBatteryHandler = new Handler();

    /**
     * Runner for battery checking
     */
    Runnable checkBatteryRunner = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d(TAG, "[ checkBatteryRunner ] Try to check battery level.");

            //if (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED && !AppParam.getInstance().isBleBusy && mBluetoothGatt != null && mBluetoothDevice != null)
            if (mBluetoothGatt != null)
            {
                // Try to read Sound Processor's status.
                sendPacket(packetMaker(PACKET_HEADER_SOUND_PROCESSOR_STATUS, null, 1));
            }
        }
    }; // discoverServiceTimeoutRunner

    /**
     * Hnadler for discovering services.
     */
    Handler discoverServiceTimeoutHandler = new Handler();

    /**
     * Runner for discovering services.
     */
    Runnable discoverServiceTimeoutRunner = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d(TAG, "[ discoverServiceTimeoutRunner ] Timeout occurred! -> discovering services.");

            AppParam.getInstance().isDisconnectedByUser = false;
            mBluetoothGatt.disconnect();
        }
    }; // discoverServiceTimeoutRunner

    /**
     * Callback - BLE GATT events
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            String name = gatt.getDevice().getName();
            String address = gatt.getDevice().getAddress();

            // Connected state.
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                //mBleViewModel.setBleConnection(BleViewModel.BLE_CONNECTING);
                mBleViewModel.postBleConnection(BleViewModel.BLE_CONNECTING);

                //new Handler(Looper.getMainLooper()).post(() ->
                //      mBleViewModel.setBleConnection(BleViewModel.BLE_CONNECTING));


                Log.d(TAG, "GATT Callback Connection State : STATE = CONNECTED, NAME = " + name + ", ADDRESS = " + address);

                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    Log.d(TAG, "Already bonded.");

                    // Make handler to check timeout for discovering services.
                    discoverServiceTimeoutHandler.postDelayed(discoverServiceTimeoutRunner, DELAY_IN_MS_FOR_DISCOVER_SERVICES_TIMEOUT);

                    if (!gatt.discoverServices())
                    {
                        gatt.disconnect();
                    }
                }
                else
                {
                    Log.d(TAG, "Try to bond.");
                    mBleViewModel.increaseBondFailCounter();
                    gatt.getDevice().createBond();
                }
            }
            // Disconnected state.
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.d(TAG, "GATT Callback Connection State : STATE = DISCONNECTED, NAME = " + name + ", ADDRESS = " + address);

                discoverServiceTimeoutHandler.removeCallbacks(discoverServiceTimeoutRunner);
                checkBatteryHandler.removeCallbacks(checkBatteryRunner);
                packetTimeoutHandler.removeCallbacks(packetTimeoutRunner);
                packetTimeoutHandler.removeCallbacks(packetTimeoutRunner2);

                gatt.close();
                mBluetoothGatt = null;

                //mBleViewModel.setBleConnection(BleViewModel.BLE_DISCONNECTED);
                mBleViewModel.postBleConnection(BleViewModel.BLE_DISCONNECTED);
                //new Handler(Looper.getMainLooper()).post(() ->
                //      mBleViewModel.setBleConnection(BleViewModel.BLE_DISCONNECTED));

                //scanLe(true);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            String name = gatt.getDevice().getName();
            String address = gatt.getDevice().getAddress();

            // Remove callback from handler checking timeout for discovering services.
            discoverServiceTimeoutHandler.removeCallbacks(discoverServiceTimeoutRunner);

            if (status != BluetoothGatt.GATT_SUCCESS)
            {
                Log.d(TAG, "GATT Callback Service Discovered : STATE = FAILED, NAME = " + name + ", ADDRESS = " + address);
                gatt.disconnect();
                return;
            }

            Log.d(TAG, "GATT Callback Service Discovered : STATE = SUCCESS, NAME = " + name + ", ADDRESS = " + address);

            List<BluetoothGattService> bluetoothGattServices = gatt.getServices();

            for (int i = 0; i < bluetoothGattServices.size(); i++)
            {
                Log.d(TAG, "Service " + i + " UUID : " + bluetoothGattServices.get(i).getUuid().toString());
            }

            // Get service from gatt.
            mBluetoothGattService = gatt.getService(BLE_UUID_SERVICE);

            if (mBluetoothGattService == null)
            {
                Log.d(TAG, "GATT Callback Service Discovered : Failed to get TODOC service.");
                gatt.disconnect();
                return;
            }

            // Get characteristics from service.
            mCharClientToServer = mBluetoothGattService.getCharacteristic(BLE_UUID_CHARACTERISTIC_CLIENT_TO_SERVER);
            mCharServerToClient = mBluetoothGattService.getCharacteristic(BLE_UUID_CHARACTERISTIC_SERVER_TO_CLIENT);

            if (mCharClientToServer == null || mCharServerToClient == null)
            {
                Log.d(TAG, "GATT Callback Service Discovered : Failed to get TODOC characteristics.");
                gatt.disconnect();
                return;
            }

            // Enable indication receiver for characteristic {server to client} on App.
            if (!mBluetoothGatt.setCharacteristicNotification(mCharServerToClient, true))
            {
                Log.d(TAG, "GATT Callback Service Discovered : Setting characteristic indication {server to client} is failed.");
                gatt.disconnect();
            }

            Log.d(TAG, "GATT Callback Service Discovered : Characteristic indication {server to client} is enabled.");

            // Get descriptor from characteristic {server to client}.
            BluetoothGattDescriptor descriptor = mCharServerToClient.getDescriptor(BLE_UUID_DESCRIPTION_CCCD);

            if (descriptor == null)
            {
                Log.d(TAG, "GATT Callback Service Discovered : Failed to get descriptor.");
                gatt.disconnect();
                return;
            }

            // Success discovering service, characteristics and descriptor.
            Log.d(TAG, "GATT Callback Service Discovered : Finally, success to discover service, characteristics and descriptor.");

            // Set descriptor on remote device.
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            if (!mBluetoothGatt.writeDescriptor(descriptor))
            {
                Log.d(TAG, "GATT Callback Service Discovered : Failed to write descriptor.");
                AppParam.getInstance().isDisconnectedByUser = false;
                gatt.disconnect();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            //super.onDescriptorWrite(gatt, descriptor, status);

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                // Success to write descriptor for indication on remote device.
                Log.d(TAG, "onDescriptorWrite : Success to write descriptor.");

                if (!sendPacket(packetMaker(PACKET_HEADER_PASSWORD, mBleViewModel.getUserPasswordConnecting().getBytes(), 5)))
                {
                    Log.d(TAG, "onDescriptorWrite : Failed to send password packet.");
                    gatt.disconnect();
                }
            }
            else
            {
                Log.d(TAG, "onDescriptorWrite : Failed to write descriptor.");
                gatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            new Handler(Looper.getMainLooper()).post(new Runnable()
            {
                @Override
                public void run()
                {
                    byte[] responsePacket = characteristic.getValue(); // Extract data from packet.
                    int packetSize = responsePacket.length; // Get size of packet data.

                    Log.d(TAG, "onCharacteristicChanged : " + printLogBytesToString(responsePacket));

                    if (packetSize < 1)
                    {
                        Log.d(TAG, "onCharacteristicChanged : size 0 packet has arrived.");
                        return;
                    }

                    // Remove callbacks from handler checking timeout for response about packet sent.
                    packetTimeoutHandler.removeCallbacks(packetTimeoutRunner);
                    packetTimeoutHandler.removeCallbacks(packetTimeoutRunner2);
                    AppParam.getInstance().isBleBusy = false; // release busy state.

                    // Get currently running fragment number.
                    int currentFragmentNumber = AppParam.getInstance().getCurrentFragmentNumber();

                    // Get packet header.
                    byte packetHeader = byteExtractor(responsePacket[0]);

                    // Process response packet based on packet header.
                    switch (packetHeader)
                    {
                        // Password
                        case PACKET_HEADER_PASSWORD:
                        {
                            if (packetSize < 2)
                            {
                                Log.d(TAG, "onCharacteristicChanged : Unexpected packet size error!");
                                Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_packet_length_error), Toast.LENGTH_LONG).show();
                                return;
                            }

                            // Correct password.
                            if (byteExtractor(responsePacket[1]) == 1)
                            {
                                Log.d(TAG, "onCharacteristicChanged : Correct password.");

                                // Try to get Sound Processor information.
                                sendPacket(packetMaker(PACKET_HEADER_SOUND_PROCESSOR_STATUS, null, 1));
                            }
                            // Not correct password.
                            else
                            {
                                Log.d(TAG, "onCharacteristicChanged : Not correct password.");
                                Log.d(TAG, "onCharacteristicChanged : Security password of Sound Processor is not correct. This can't be happened!");

                                mBluetoothGatt.disconnect();
                            }
                        } // PACKET_HEADER_PASSWORD
                        break;

                        // Sound Processor status
                        case PACKET_HEADER_SOUND_PROCESSOR_STATUS:
                        {
                            if (packetSize != 8)
                            {
                                Log.d(TAG, "onCharacteristicChanged : Unexpected packet size error!");
                                Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_packet_length_error), Toast.LENGTH_LONG).show();
                                break;
                            }

                            DeviceParam deviceParam = new DeviceParam();

                            deviceParam.setBattery((byte) (responsePacket[1] & 0xff));
                            deviceParam.setProgram((byte) (responsePacket[2] & 0xff));
                            deviceParam.setSensitivity((byte) (responsePacket[3] & 0xff));
                            deviceParam.setVolume((byte) (responsePacket[4] & 0xff));
                            deviceParam.setAlarmLed((byte) (responsePacket[5] & 0xff));
                            deviceParam.setTelecoil((byte) (responsePacket[6] & 0xff));
                            deviceParam.setAlarmStimulation((byte) (responsePacket[7] & 0xff));
                            //deviceParam.setPowerMode((byte) (responsePacket[8] & 0xff));
                            //deviceParam.setWarnning((byte) (responsePacket[8] & 0xff));

                            //AppParam.getInstance().currentStatusParams = deviceParam;

                            Log.d(TAG, "onCharacteristicChanged : Successfully get Sound Processor status.");
                            Log.d(TAG, "onCharacteristicChanged : Battery = " + deviceParam.getBattery());
                            Log.d(TAG, "onCharacteristicChanged : Program = " + deviceParam.getProgram());
                            Log.d(TAG, "onCharacteristicChanged : Volume = " + deviceParam.getVolume());
                            Log.d(TAG, "onCharacteristicChanged : Sensitivity = " + deviceParam.getSensitivity());
                            Log.d(TAG, "onCharacteristicChanged : Alarm LED = " + deviceParam.getAlarmLed());
                            Log.d(TAG, "onCharacteristicChanged : Telecoil = " + deviceParam.getTelecoil());
                            Log.d(TAG, "onCharacteristicChanged : Alarm Stimulation = " + deviceParam.getAlarmStimulation());

                            mStatusViewModel.setValueBatteryLevel(deviceParam.getBattery());
                            mStatusViewModel.setValueNotification(deviceParam.getAlarmStimulation());
                            mStatusViewModel.setValueLed(deviceParam.getAlarmLed());
                            mStatusViewModel.setValueTelecoil(deviceParam.getTelecoil());
                            mStatusViewModel.setValueMaxOutput(deviceParam.getSensitivity());
                            mStatusViewModel.setValueVolume(deviceParam.getVolume());
                            mStatusViewModel.setValueProgram(deviceParam.getProgram());

                            // Check value validation.

                            boolean invalid = false;

                            if (invalid)
                            {
                                makeDialogReattachSoundProcessorAndDisconnect();
                            }
                            else
                            {
                                // Make battery checking handler into main looper.
                                checkBatteryHandler.removeCallbacks(checkBatteryRunner);
                                checkBatteryHandler.postDelayed(checkBatteryRunner, DELAY_IN_MS_FOR_BATTERY_CHECKING);
                            }

                            //writeMessage(LoggingUtils.LOGGING_VALUE_STATUS, "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName() + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress() + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial() + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName() + ", invalid=" + invalid + ", batt=" + deviceParam.getBattery()); // Logging

                            mBleViewModel.setBleConnection(BleViewModel.BLE_CONNECTED);
                        }
                        break;

                        // Alarm stimulation
                        case PACKET_HEADER_VALUE_SIMULATION:
                        {
                            int value = responsePacket[1];
                            mStatusViewModel.setValueNotification(value);
                        }
                        break;

                        // Alarm LED
                        case PACKET_HEADER_VALUE_LED:
                        {
                            int value = responsePacket[1];
                            mStatusViewModel.setValueLed(value);
                        }
                        break;

                        // Alarm Telecoil
                        case PACKET_HEADER_VALUE_TELECOIL:
                        {
                            int value = responsePacket[1];
                            mStatusViewModel.setValueTelecoil(value);
                        }
                        break;

                        // Program
                        case PACKET_HEADER_VALUE_PROMGRAM:
                        {
                            int value = responsePacket[1];
                            mStatusViewModel.setValueProgram(value);
                        }
                        break;
                        // Sensitivity
                        case PACKET_HEADER_VALUE_SENSITIVITY:
                        {
                            int value = responsePacket[1];
                            mStatusViewModel.setValueMaxOutput(value);
                        }
                        break;
                        // Volume
                        case PACKET_HEADER_VALUE_VOLUME:
                        {
                            int value = responsePacket[1];
                            mStatusViewModel.setValueVolume(value);
                        }
                        break;
                        // Error
                        case PACKET_HEADER_ERROR:
                        {
                            if (packetSize < 3)
                            {
                                Log.d(TAG, "onCharacteristicChanged : Unexpected packet size error!");
                                Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_packet_length_error), Toast.LENGTH_LONG).show();
                                break;
                            }

                            byte errorType = byteExtractor(responsePacket[2]);

                            switch (errorType)
                            {
                                // Invalid command, Invalid packet data size
                                case 1:
                                case 2:
                                    Log.d(TAG, "onCharacteristicChanged : Packet error!");

                                    if (!AppParam.getInstance().isEnabledInvalidPacketToast)
                                    {
                                        Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_sound_processor_get_error_packet), Toast.LENGTH_LONG).show();

                                        AppParam.getInstance().isEnabledInvalidPacketToast = true;

                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                AppParam.getInstance().isEnabledInvalidPacketToast = false;
                                            }
                                        }, 3500);
                                    }
                                    break;

                                // Busy state
                                case 3:
                                    Log.d(TAG, "onCharacteristicChanged : Sound Processor is busy!");

                                    if (!AppParam.getInstance().isEnabledBusyToast)
                                    {
                                        Toast.makeText(getApplicationContext(), getString(R.string.fragment_password_toast_message_ble_busy), Toast.LENGTH_LONG).show();

                                        AppParam.getInstance().isEnabledBusyToast = true;

                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                AppParam.getInstance().isEnabledBusyToast = false;
                                            }
                                        }, 3500);
                                    }
                                    break;

                                // Password unlocked...
                                case 4:
                                    Log.d(TAG, "onCharacteristicChanged : Sound Processor is locked! Unlock Sound Processor using password first.");

                                    if (!AppParam.getInstance().isEnabledUnlockedToast)
                                    {
                                        Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_sound_processor_locked), Toast.LENGTH_LONG).show();
                                        AppParam.getInstance().isEnabledUnlockedToast = true;

                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                AppParam.getInstance().isEnabledUnlockedToast = false;
                                            }
                                        }, 3500);
                                    }
                                    break;

                                // Sound Processor system error...
                                case 5:
                                case 6:
                                case 7:
                                    Log.d(TAG, "onCharacteristicChanged : Sound Processor internal system error occurred...");

                                    if (!AppParam.getInstance().isEnabledInternalErrorToast)
                                    {
                                        Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_sound_processor_internal_error), Toast.LENGTH_LONG).show();
                                        AppParam.getInstance().isEnabledInternalErrorToast = true;

                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                AppParam.getInstance().isEnabledInternalErrorToast = false;
                                            }
                                        }, 3500);
                                    }
                                    break;
                            }
                        }
                        break;
                    } // switch
                } // run
            }); // handler mainLooper
        } // onCharacteristicChanged
    }; // BluetoothGattCallback

    private void vibrator(int ms)
    {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(ms);
    }

    /**
     * Value extractor - for byte
     */
    public byte byteExtractor(byte b)
    {
        return (byte) (b & 0xFF);
    }

    /**
     * Handler for packet timeout
     */
    Handler packetTimeoutHandler = new Handler();

    /**
     * Runner2 for packet timeout
     */
    Runnable packetTimeoutRunner2 = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d(TAG, "packetTImeoutRunner2 : Timeout occurred for response packet. Then try to disconnect from current connected Sound Processor.");

            if (mBluetoothGatt != null && AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
            {
                mBluetoothGatt.disconnect();
            }
        }
    }; // scanRunner

    /**
     * Runner for packet timeout
     */
    Runnable packetTimeoutRunner = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d(TAG, "packetTImeoutRunner : Timeout occurred for response packet. Then Try to send previous packet again.");

            if (mBluetoothGatt.writeCharacteristic(mCharClientToServer))
            {
                packetTimeoutHandler.postDelayed(packetTimeoutRunner2, DELAY_IN_MS_FOR_PACKET_RESPONSE_TIMEOUT); // Make Handler for timeout
            }

            //Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_packet_timeout), Toast.LENGTH_SHORT).show();
        }
    }; // scanRunner

    /**
     * Handler for send packet
     */
    Handler packetSendHandler = new Handler();

    /**
     * Runner for send packet
     */
    Runnable packetSendRunner = new Runnable()
    {
        @Override
        public void run()
        {
            if (mBluetoothGatt == null || mBluetoothDevice == null || mCharClientToServer == null || AppParam.getInstance().sendingPacket == null)
            {
                return;
            }

            if (mCharClientToServer.setValue(AppParam.getInstance().sendingPacket) && mBluetoothGatt.writeCharacteristic(mCharClientToServer))
            {
                packetTimeoutHandler.postDelayed(packetTimeoutRunner, DELAY_IN_MS_FOR_PACKET_RESPONSE_TIMEOUT); // Make Handler for timeout
            }
            else
            {
                return;
            }

            Log.d(TAG, "packetSendRunner : packet = " + printLogBytesToString(AppParam.getInstance().sendingPacket));
            AppParam.getInstance().sendingPacket = null;
        }
    };

    /**
     * Send packet that can be used anywhere
     */
    public boolean sendPacket(byte[] packet)
    {
        if (AppParam.getInstance().isBleBusy)
        {
            Log.d(TAG, "sendPacket : Failed to send packet why Characteristic is busy.");
            return false;
        }

        /*
        if (AppParam.getInstance().bleConnectionState == AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_ble_not_connected), Toast.LENGTH_SHORT).show();
            return false;
        }
        */

        AppParam.getInstance().isBleBusy = true;
        AppParam.getInstance().sendingPacket = packet;
        packetSendHandler.postDelayed(packetSendRunner, DELAY_IN_MS_FOR_SEND_PACKET);

        return true;
    }

    /**
     * Printer for String using bytes.
     */
    public String printLogBytesToString(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("0x");

        for (byte b : bytes)
        {
            sb.append(String.format("%02X ", (b & 0xFF)));
        }

        return sb.toString();
    }

    /**
     * Make a packet that must less than equal length 20.
     */
    public byte[] packetMaker(int header, byte[] payload, int maxLen)
    {
        byte[] packet = new byte[maxLen];

        packet[0] = (byte) (header & 0xFF);

        if (payload != null)
        {
            System.arraycopy(payload, 0, packet, 1, Math.min((maxLen - 1), payload.length));
        }

        Log.d(TAG, "New Packet = " + printLogBytesToString(packet));
        return packet;
    }

    /**
     * Make a dialog for relaunch app message.
     */
    public void makeDialogRelaunchApp()
    {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.MyAlertDialogTheme);

        String message = getString(R.string.activity_main_dialog_message_relaunch_app_by_status_parameters);

        builder.setMessage(message);

        builder.setPositiveButton(getString(R.string.dialog_message_ok), null);

        AppParam.getInstance().lastDialog = builder.create();
        AppParam.getInstance().lastDialog.show();
    }

    /**
     * Make a dialog for re-attach Sound Processor and disconnect.
     */
    public void makeDialogReattachSoundProcessorAndDisconnect()
    {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.MyAlertDialogTheme);

        String message = getString(R.string.activity_main_dialog_message_invalid_status_value);

        builder.setCancelable(false);

        builder.setMessage(message);

        builder.setPositiveButton(getString(R.string.dialog_message_ok), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                updateLongTimeIdleHandler(); // Update long time idle handler
                if (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED && mBluetoothDevice != null && mBluetoothGatt != null)
                {
                    AppParam.getInstance().isDisconnectedByUser = true;
                    mBluetoothGatt.disconnect();
                }
            }
        });

        AppParam.getInstance().invalidValueDialog = builder.create();
        AppParam.getInstance().lastDialog = AppParam.getInstance().invalidValueDialog;
        AppParam.getInstance().invalidValueDialog.show();
    }

    public void writeMessage(int type, String message)
    {
        LoggingUtils.getInstance().writeMessage(LoggingUtils.getInstance().typeMessage(type) + " : " + message);
    }

    //
    // Associated with "Tool bar".
    //
    public void showToolBarNavigationIcon(boolean enable)
    {
        if (enable)
        {
            mBinding.toolbar.setNavigationIcon(getDrawable(R.drawable.toolbar_ic_back_arrow_24dp));
        }
        else
        {
            mBinding.toolbar.setNavigationIcon(null);
        }
    }

    public void initToolBar()
    {
        mBinding.toolbar.setNavigationOnClickListener(mToolBarNavigationClickListener);
        mBinding.toolbar.setOnMenuItemClickListener(mToolBarMenuItemClickListener);
    }

    View.OnClickListener mToolBarNavigationClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            onBackPressed();
        }
    };

    Toolbar.OnMenuItemClickListener mToolBarMenuItemClickListener = new Toolbar.OnMenuItemClickListener()
    {
        @Override
        public boolean onMenuItemClick(MenuItem item)
        {
            if (item.getItemId() == R.id.settings)
            {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
                return true;
            }
            else if (item.getItemId() == R.id.toolbar_user)
            {
                if (mBleViewModel.getBleConnection() != BleViewModel.BLE_DISCONNECTED)
                {
                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("주의")
                            .setMessage("선택하신 사용자와 앱이 연결되어 있습니다. 연결을 종료하고 새 사용자를 선택하시겠습니까?")
                            .setPositiveButton("네", (dialogInterface, i) ->
                            {
                                mBleViewModel.setSearching(BleViewModel.SEARCHING_DISABLED);
                                mBluetoothGatt.disconnect();
                                mBinding.toolbar.setTitle("선택된 사용자 없음");
                                makeDialogSelectUser();
                            })
                            .setNegativeButton("아니오", null)
                            .show();
                }
                else // 현재 연결된 장치가 없을 때 수행.
                {
                    makeDialogSelectUser();
                }

                return true;
            }

            return false;
        }
    };

    private void makeDialogSelectUser()
    {
        List<EntityUser> users = mDatabaseUsers.daoUsers().findAll();
        mUserList = new String[users.size()];

        if (users.size() == 0)
        {
            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("안내")
                    .setMessage("등록된 사용자가 없습니다. 먼저 사용자를 등록해주세요.")
                    .setPositiveButton("확인", null)
                    .show();
        }
        else
        {
            mCheckItem = 0;

            for (int i = 0; i < users.size(); i++)
            {
                mUserList[i] = users.get(i).name;

                if (users.get(i).defaultUser.equals(EntityUser.USER_DEFAULT))
                {
                    mCheckItem = i;
                }
            }

            new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("사용자 목록")
                    .setPositiveButton("선택", (dialogInterface, i) ->
                    {
                        String selectName = mUserList[mCheckItem];
                        mBinding.toolbar.setTitle(selectName);

                        EntityUser defaultUser = mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);

                        if (defaultUser != null)
                        {
                            defaultUser.defaultUser = EntityUser.USER_NOT_DEFAULT;
                            mDatabaseUsers.daoUsers().update(defaultUser);
                        }

                        EntityUser selectUser = mDatabaseUsers.daoUsers().getDbUserByName(selectName);

                        if (selectUser != null)
                        {
                            selectUser.defaultUser = EntityUser.USER_DEFAULT;
                            mDatabaseUsers.daoUsers().update(selectUser);
                        }

                        mBinding.toolbar.setTitleTextAppearance(MainActivity.this, R.style.TextAppearance_RemoteControl_Controller_Headline6);
                        mBinding.toolbar.setTitle(selectUser.name);

                        mBleViewModel.setSearching(BleViewModel.SEARCHING_ENABLED);
                    })
                    .setSingleChoiceItems(mUserList, mCheckItem, (dialogInterface, i) ->
                    {
                        mCheckItem = i;
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    public void onLockScreenNumberClickedListener(View view)
    {
        LockScreen.numberClickListener(view);
    }

    public void onLockScreenForgetPasswordClickedListener(View view)
    {
        new MaterialAlertDialogBuilder(this, R.style.lock_screen_dialog).setTitle(getString(R.string.lock_screen_dialog_title)).setMessage(getString(R.string.lock_screen_dialog_message)).setPositiveButton(getString(R.string.lock_screen_dialog_positive), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                LockScreen.erasePassword();
                LockScreen.resume(MainActivity.this, mBinding);
            }
        }).setNegativeButton(getString(R.string.lock_screen_dialog_negative), null).show();
    }

    //
    // Associated with "Vibrator".
    //
    public void onVibrator(int ms)
    {
        vibrator(ms);
    }
}