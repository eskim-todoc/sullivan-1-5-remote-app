package todoc.cochlear.remoteapp.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.bluetooth.BtUtils;
import todoc.cochlear.remoteapp.database.AppDatabase;
import todoc.cochlear.remoteapp.database.Device;
import todoc.cochlear.remoteapp.databinding.MainData;
import todoc.cochlear.remoteapp.fragment.DeviceListFragment;
import todoc.cochlear.remoteapp.fragment.HomeFragment;
import todoc.cochlear.remoteapp.fragment.PasswordFragment;
import todoc.cochlear.remoteapp.fragment.SearchFragment;
import todoc.cochlear.remoteapp.logging.LoggingUtils;
import todoc.cochlear.remoteapp.params.AppParam;
import todoc.cochlear.remoteapp.params.DeviceParam;
import todoc.cochlear.remoteapp.params.ActionMessage;
import todoc.cochlear.remoteapp.service.TerminationService;
import todoc.cochlear.remoteapp.shared_preferences.AppPreferences;
import todoc.cochlear.remoteapp.viewmodel.MyModel;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "TD2_" + MainActivity.class.getSimpleName();

    private static final int REQUEST_CODE_BLUETOOTH_ENABLE = 1;
    private static final int REQUEST_PERMISSION_FINE_LOCATION = 100;

    public static final ParcelUuid BLE_ADVERTISING_SERVICE_UUID = new ParcelUuid(UUID.fromString("6f400001-b5a3-f393-e0a9-e50e24dcca9e"));
    public static final UUID BLE_UUID_SERVICE = UUID.fromString("6f400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID BLE_UUID_CHARACTERISTIC_CLIENT_TO_SERVER = UUID.fromString("6f400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID BLE_UUID_CHARACTERISTIC_SERVER_TO_CLIENT = UUID.fromString("6f400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID BLE_UUID_DESCRIPTION_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int DELAY_IN_MS_FOR_BLUETOOTH_LE_SCANNER = 15000;
    private static final int DELAY_IN_MS_FOR_DISCOVER_SERVICES_TIMEOUT = 2000;
    private static final int DELAY_IN_MS_FOR_SEND_PACKET = 50;
    private static final int DELAY_IN_MS_FOR_PACKET_RESPONSE_TIMEOUT = 500;
    private static final int DELAY_IN_MS_FOR_BATTERY_CHECKING = 30000;

    //private static final int DELAY_IN_MS_FOR_LONG_TIME_IDLE = 10000; // 10 seconds.
    private static final int DELAY_IN_MS_FOR_LONG_TIME_IDLE = 600000; // 10 minutes.

    public static final byte PACKET_HEADER_PASSWORD = (byte) (0x40 & 0xff);
    public static final byte PACKET_HEADER_SOUND_PROCESSOR_INFO = (byte) (0x43 & 0xff);
    public static final byte PACKET_HEADER_MAP_INFO = (byte) (0x44 & 0xff);
    public static final byte PACKET_HEADER_ISD_USER_NAME = (byte) (0x45 & 0xff);
    public static final byte PACKET_HEADER_SOUND_PROCESSOR_STATUS = (byte) (0x46 & 0xff);
    public static final byte PACKET_HEADER_VALUE_PROMGRAM = (byte) (0x47 & 0xff);
    public static final byte PACKET_HEADER_VALUE_VOLUME = (byte) (0x48 & 0xff);
    public static final byte PACKET_HEADER_VALUE_SENSITIVITY = (byte) (0x49 & 0xff);
    public static final byte PACKET_HEADER_VALUE_TELECOIL = (byte) (0x4A & 0xff);
    public static final byte PACKET_HEADER_VALUE_SIMULATION = (byte) (0x4B & 0xff);
    public static final byte PACKET_HEADER_VALUE_LED = (byte) (0x4C & 0xff);
    public static final byte PACKET_HEADER_VALUE_WARNNING = (byte) (0x4D & 0xff);
    public static final byte PACKET_HEADER_VALUE_POWER_MODE = (byte) (0x4E & 0xff);
    public static final byte PACKET_HEADER_ERROR = (byte) (0xf0 & 0xff);

    // Bluetooth
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner mBluetoothLeScanner;
    BluetoothDevice mBluetoothDevice;
    BluetoothGatt mBluetoothGatt;
    BluetoothGattService mBluetoothGattService;
    BluetoothGattCharacteristic mCharClientToServer;
    BluetoothGattCharacteristic mCharServerToClient;

    // //TODO: ViewModel 테스트
    private MyModel myModel;

    // TODO: DataBinding 테스트
    private ActivityMainBinding activityMainBinding;
    private MainData mainData;

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
        longTimeIdleHandler.removeCallbacks(longTimeIdleRunner);
        longTimeIdleHandler.postDelayed(longTimeIdleRunner, DELAY_IN_MS_FOR_LONG_TIME_IDLE);
    }

    /**
     * Callback - onDestroy
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        Log.d(TAG, "onDestroy() called.");

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

        AppParam.getInstance().appLock = true;
        initAppLockNums();

        longTimeIdleHandler.removeCallbacks(longTimeIdleRunner); // Remove long time idle handler
    }

    /**
     * Callback - onCreate
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mainData = new MainData();
        activityMainBinding.setMainData(mainData);

        AppParam.getInstance().bleConnectionState = AppParam.BLE_CONNECTION_STATE_DISCONNECTED;

        // Initialize Toolbar
        AppParam.getInstance().menuHome = true;
        AppParam.getInstance().menuTitle = "";
        AppParam.getInstance().menuSearch = true;
        AppParam.getInstance().menuList = true;
        setSupportActionBar(findViewById(R.id.main_toolbar));
        getSupportActionBar().setElevation(0);

        // Set color for bottom navigation bar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.color_bottom_navigation));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.color_status_bar));

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
        // Check language
        AppParam.getInstance().isKorean = getString(R.string.language).equals("한글");

        // Auto-connection shared preferences
        AppParam.getInstance().setAutoConnectionEnabled(
                AppPreferences.getInstance().isAutoConnectionEnabled(getApplicationContext()));

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

        // First screen is 'Home' fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new HomeFragment()).commitAllowingStateLoss();
        //getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new DeviceListFragment()).commitAllowingStateLoss();
        //getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new PasswordFragment()).commitAllowingStateLoss();

        // Check Database
        if (AppParam.getInstance().database == null)
        {
            AppParam.getInstance().database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, AppDatabase.DATABASE_NAME)
                    .addCallback(new RoomDatabase.Callback()
                    {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db)
                        {
                            super.onCreate(db);
                            db.execSQL(AppDatabase.DATABASE_ENCODING);
                        }
                    })
                    .fallbackToDestructiveMigration().allowMainThreadQueries().build();

            Log.d(TAG, "database = " + AppParam.getInstance().database);
        }

        initNumberLongClick();

        // 로깅 관련 초기화
        LoggingUtils.getInstance().openLoggingDatabase(getApplicationContext()); // 로깅 데이터베이스 불러오기 + 번호 ROW 초기화.
        //LoggingUtils.getInstance().writeMessage(LoggingUtils.getInstance().getCurrentDate()); // 테스트 메시지 입력
        LoggingUtils.getInstance().printAllMessages(); // 테스트 메시지 출력

        AppParam.getInstance().registeredDevices = AppParam.getInstance().database.deviceDao().findAll();

        enableScreenNoDeviceRegistered(AppParam.getInstance().registeredDevices.size() == 0);

        // Manual... shared preferences
        enableScreenUserGuide(AppPreferences.getInstance().isManualEnabled(getApplicationContext()));

        AppParam.getInstance().appLock = true;
        initAppLockNums();

        //TODO: Live Data를 이용한 장시간 미사용 테스트
        final Observer<Boolean> longTimeIdleObserver = new Observer<Boolean>()
        {
            @Override
            public void onChanged(Boolean aBoolean)
            {
                if (aBoolean != null && aBoolean.booleanValue())
                {
                    // Make Dialog
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                    builder.setCancelable(false);
                    builder.setMessage("장시간 미사용으로 인해 앱이 절전모드로 진입했습니다.\n절전모드 해제 버튼을 누르면 다시 정상동작을 시작합니다.");
                    builder.setPositiveButton("절전모드 해제", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            AppParam.getInstance().longTimeIdle.setValue(Boolean.valueOf(false));
                        }
                    });
                    builder.create().show();

                    AppParam.getInstance().setAutoConnectionEnabled(false);
                    if (AppParam.getInstance().isBleScanning)
                    {
                        scanLe(false);
                    }

                    // Disconnect BLE if connected
                    if (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                    {
                        AppParam.getInstance().isDisconnectedByUser = true;
                        mBluetoothGatt.disconnect();
                    }

                    // Do BackPressed if screen shows PASSWORD fragment currently
                    if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_PASSWORD)
                    {
                        getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new SearchFragment()).commitAllowingStateLoss();
                    } // PASSWORD 프래그먼트 끝

                    // 앱 잠금 화면을 활성화 시킴
                    AppParam.getInstance().appLock = true;
                    onResumeAppLock();
                } // Long Time Idle 이벤트 설정 발생
                else if (aBoolean != null && !aBoolean.booleanValue())
                {
                    // 자동 검색 설정을 데이터베이스 값으로 확인한다
                    if (AppPreferences.getInstance().isAutoConnectionEnabled(getApplicationContext()))
                    {
                        AppParam.getInstance().setAutoConnectionEnabled(true);
                    }

                    // 등록된 장치가 있거나, 검색 화면이라면 스캔을 시작한다
                    if (AppParam.getInstance().registeredDevices.size() > 0)
                    {
                        scanLe(true);
                    }
                    else
                    {
                        if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_SEARCH)
                        {
                            scanLe(true);
                        }
                    }

                    updateLongTimeIdleHandler();
                } // Long Time Idle 이벤트 해제 발생
            } // onChanged 끝.
        }; // 옵저버 끝.

        AppParam.getInstance().longTimeIdle.observe(this, longTimeIdleObserver);

        // Start & update long time idle handler
        updateLongTimeIdleHandler();
    } // initMainActivity

    public void onClickLiveData(View view)
    {

        Log.d(TAG, "onClickLiveData");

        AppParam.getInstance().longTimeIdle.setValue(Boolean.valueOf(true));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        Log.d(TAG, "##### onTouchEvent #####");
        return super.onTouchEvent(event);
    }


    /**
     * Callback - User guide clase button.
     */
    public void onClickUserGuideClose(View view)
    {
        updateLongTimeIdleHandler(); // Update long time idle handler
        boolean enable = !((CheckBox) findViewById(R.id.main_user_guide_checkbox)).isChecked();
        AppPreferences.getInstance().setManualToEnabled(getApplication(), enable);
        enableScreenUserGuide(false);
    }

    /**
     * Enable or disable showing user guide screen.
     */
    public void enableScreenUserGuide(boolean enable)
    {
        LinearLayout layout = findViewById(R.id.main_user_guide_layout);

        if (enable)
        {
            Log.d(TAG, "Show manual.");
            layout.setVisibility(View.VISIBLE);
        }
        else
        {
            Log.d(TAG, "Hide manual.");
            layout.setVisibility(View.GONE);
        }
    }

    /**
     * Callback - If no sound processor has registered, user must go to search a sound processor
     */
    public void onClickNoDeviceRegistered(View view)
    {
        updateLongTimeIdleHandler(); // Update long time idle handler
        sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE));
        enableScreenNoDeviceRegistered(false);

        AppParam.getInstance().setAutoConnectionEnabled(true);
        AppPreferences.getInstance().setAutoConnectionToEnabled(getApplicationContext(), true);
        updateToolbar();
    }

    /**
     * Set long click event for app lock screen's number buttons.
     */
    public void initNumberLongClick()
    {
        // DELETE
        findViewById(R.id.main_applock_num_del_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_del_btn).setOnLongClickListener(mNumberLongClickListener);

        // 0
        findViewById(R.id.main_applock_num_0_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_0_btn).setOnLongClickListener(mNumberLongClickListener);

        // 1
        findViewById(R.id.main_applock_num_1_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_1_btn).setOnLongClickListener(mNumberLongClickListener);

        // 2
        findViewById(R.id.main_applock_num_2_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_2_btn).setOnLongClickListener(mNumberLongClickListener);

        // 3
        findViewById(R.id.main_applock_num_3_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_3_btn).setOnLongClickListener(mNumberLongClickListener);

        // 4
        findViewById(R.id.main_applock_num_4_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_4_btn).setOnLongClickListener(mNumberLongClickListener);

        // 5
        findViewById(R.id.main_applock_num_5_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_5_btn).setOnLongClickListener(mNumberLongClickListener);

        // 6
        findViewById(R.id.main_applock_num_6_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_6_btn).setOnLongClickListener(mNumberLongClickListener);

        // 7
        findViewById(R.id.main_applock_num_7_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_7_btn).setOnLongClickListener(mNumberLongClickListener);

        // 8
        findViewById(R.id.main_applock_num_8_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_8_btn).setOnLongClickListener(mNumberLongClickListener);

        // 9
        findViewById(R.id.main_applock_num_9_btn).setLongClickable(true);
        findViewById(R.id.main_applock_num_9_btn).setOnLongClickListener(mNumberLongClickListener);
    }

    /**
     * Long click event listener for app lock screen's number buttons.
     */
    View.OnLongClickListener mNumberLongClickListener = new View.OnLongClickListener()
    {
        @Override
        public boolean onLongClick(View view)
        {
            updateLongTimeIdleHandler(); // Update long time idle handler
            int count = LoggingUtils.getInstance().mSecretNumberCount;

            Log.d(TAG, "Currently secret log viewer password count : " + count);

            switch (count)
            {
                case 0:
                case 1:
                case 4:
                case 5:
                    if (view.getId() == R.id.main_applock_num_del_btn)
                    {
                        Log.d(TAG, "Correct number.");
                        count++;
                    }
                    else
                    {
                        Log.d(TAG, "Incorrect number.");
                        count = 0;
                    }
                    break;

                case 2:
                    if (view.getId() == R.id.main_applock_num_1_btn)
                    {
                        Log.d(TAG, "Correct number.");
                        count++;
                    }
                    else
                    {
                        Log.d(TAG, "Incorrect number.");
                        count = 0;
                    }
                    break;

                case 3:
                    if (view.getId() == R.id.main_applock_num_4_btn)
                    {
                        Log.d(TAG, "Correct number.");
                        count++;
                    }
                    else
                    {
                        Log.d(TAG, "Incorrect number.");
                        count = 0;
                    }
                    break;

                case 6:
                    if (view.getId() == R.id.main_applock_num_0_btn)
                    {
                        Log.d(TAG, "Correct number.");
                        count++;
                    }
                    else
                    {
                        Log.d(TAG, "Incorrect number.");
                        count = 0;
                    }
                    break;

                case 7:
                    if (view.getId() == R.id.main_applock_num_7_btn)
                    {
                        Log.d(TAG, "Correct number.");
                        Log.d(TAG, "Secret log viewer will be shown");
                        ConstraintLayout screen = findViewById(R.id.main_logging_screen_layout);
                        screen.setVisibility(View.VISIBLE);
                        // 로깅 데이터 입력하기 시작
                        {
                            LoggingUtils.getInstance().printLoggingScreen((ListView) findViewById(R.id.main_logging_screen_lv));
                        } // 로깅 데이터 입력하기 끝
                    }
                    else
                    {
                        Log.d(TAG, "Incorrect number.");
                    }
                    count = 0;
                    break;

                default:
                    Log.d(TAG, "Incorrect number.");
                    count = 0;
                    break;
            }

            LoggingUtils.getInstance().mSecretNumberCount = count;

            return true;
        }
    };

    /**
     * Callback - Logging screen exit button
     */
    public void onClickExitLoggingScreen(View view)
    {
        updateLongTimeIdleHandler(); // Update long time idle handler
        ConstraintLayout screen = findViewById(R.id.main_logging_screen_layout);
        screen.setVisibility(View.GONE);
        LoggingUtils.getInstance().clearLoggingScreen();
    }

    /**
     * 앱 완전 초기화 함수
     */
    public void initAllForApp()
    {
        // 1. 등록된 사운드처리기 정보 가지고 오기.
        List<Device> devices = AppParam.getInstance().database.deviceDao().findAll();

        // 2. 본딩된 블루투스 기기 가지고 오기.
        Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

        // 3. 등록된 사운드처리기를 가지고 본딩된 블루투스 기기 목록에서 일치하는 기기를 찾아내고,
        // 해당 기기의 본딩을 해제한다.
        for (int i = 0; i < devices.size(); i++)
        {
            if (bondedDevices.size() > 0)
            {
                for (BluetoothDevice bluetoothDevice : bondedDevices)
                {
                    if (bluetoothDevice.getAddress().equals(devices.get(i).getDeviceMacAddress()))
                    {
                        try
                        {
                            Log.d(TAG, "본딩 장치 = " + bluetoothDevice.getAddress() + "를 본딩 해제 합니다.");
                            Method m = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
                            m.invoke(bluetoothDevice, (Object[]) null);
                        }
                        catch (Exception e)
                        {
                            Log.d(TAG, "본딩 장치 = " + bluetoothDevice.getAddress() + " 본딩 해제 실패.");
                            Log.e(TAG, e.getMessage());
                        }

                        break;
                    }
                }
            }
        } // 본딩 해제 끝

        // 4. 앱 데이터 베이스의 모든 사운드처리기를 삭제
        for (int i = 0; i < devices.size(); i++)
        {
            Log.d(TAG, "사운드처리기 = " + devices.get(i).getDeviceMacAddress() + "를 삭제합니다.");
            AppParam.getInstance().database.deviceDao().delete(devices.get(i));

            writeMessage(LoggingUtils.LOGGING_DEVICE_REMOVED,
                    "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                            + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                            + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                            + ", user=" + devices.get(i).getImplantUserName()); // Logging
        }

        // 5. 데이터베이스 다시 로드하여 현재 정보 업데이트
        AppParam.getInstance().registeredDevices = AppParam.getInstance().database.deviceDao().findAll();
        Log.d(TAG, "데이터베이스 다시 로드");

        // 6. 앱 비밀번호 초기화
        AppPreferences.getInstance().setNewPassword(getApplicationContext(), "");
        Log.d(TAG, "앱 비밀번호 초기화");

        // 7. 매뉴얼 값 초기화
        AppPreferences.getInstance().setManualToEnabled(getApplicationContext(), true);
        Log.d(TAG, "앱 매뉴얼 값 초기화");

        // 8. 자동 연결 값 초기화
        AppPreferences.getInstance().setAutoConnectionToEnabled(getApplicationContext(), true);
        Log.d(TAG, "앱 자동연결 값 초기화");

        // 9. 화면을 home 화면으로 변경
        getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new HomeFragment()).commitAllowingStateLoss();
        Log.d(TAG, "Home 화면으로 설정");

        // 10. NO 디바이스 화면 활성화
        enableScreenNoDeviceRegistered(AppParam.getInstance().registeredDevices.size() == 0);
        Log.d(TAG, "등록된 장치 없을 때 화면 활성화");

        // 11. 매뉴얼 화면 활성화 (상태 값에 따름)
        enableScreenUserGuide(AppPreferences.getInstance().isManualEnabled(getApplicationContext()));
        Log.d(TAG, "매뉴얼 화면 활성화");

        // 12. BLE 연결 중인 장치가 있다면 연결 해제
        if (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED
                && mBluetoothDevice != null && mBluetoothGatt != null)
        {
            // 단, 현재 연결 중인 장치가 있다면 본딩까지 됐는지 확인후 본딩 제거
            if (bondedDevices.size() > 0)
            {
                for (BluetoothDevice bluetoothDevice : bondedDevices)
                {
                    if (bluetoothDevice.getAddress().equals(mBluetoothDevice.getAddress()))
                    {
                        try
                        {
                            Log.d(TAG, "본딩 장치 = " + bluetoothDevice.getAddress() + "를 본딩 해제 합니다.");
                            Method m = bluetoothDevice.getClass().getMethod("removeBond", (Class[]) null);
                            m.invoke(bluetoothDevice, (Object[]) null);
                        }
                        catch (Exception e)
                        {
                            Log.d(TAG, "본딩 장치 = " + bluetoothDevice.getAddress() + " 본딩 해제 실패.");
                            Log.e(TAG, e.getMessage());
                        }

                        break;
                    }
                }
            }

            AppParam.getInstance().isDisconnectedByUser = true;
            mBluetoothGatt.disconnect();
            Log.d(TAG, "연결된 장치 있으므로 해제");
        }

        // 13. 앱 비밀번호 플래그 설정
        AppParam.getInstance().appLockPasswordRegister = true;
        AppParam.getInstance().appLockNumVerify = false;
        ConstraintLayout passLockLayout = findViewById(R.id.main_applock_layout); // 잠금 화면 켜기
        passLockLayout.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.main_applock_desc)).setText("등록할 앱 비밀번호를 입력해주세요."); // 텍스트뷰 설정
        Log.d(TAG, "앱 비밀번호 플래그 설정 완료");

        // 14. 앱 비밀번호 화면 초기화
        initAppLockNums();

        // 15. 확인 다이얼로그 생성
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

    /**
     * Callback - 잠금 해제 화면의 기능을 테스트 하기 위한 클릭 콜백 함수
     */
    public void onClickPassNumDelete(View view)
    {
        updateLongTimeIdleHandler(); // Update long time idle handler
        vibrator(5);
        LoggingUtils.getInstance().mSecretNumberCount = 0;

        switch (AppParam.getInstance().appLockNumIdx)
        {
            case 0:
                break;
            case 1:
                AppParam.getInstance().appLockNum1 = " ";
                ((TextView) findViewById(R.id.main_applock_password_1_val)).setText(" ");
                AppParam.getInstance().appLockNumIdx = 0;
                break;
            case 2:
                AppParam.getInstance().appLockNum2 = " ";
                ((TextView) findViewById(R.id.main_applock_password_2_val)).setText(" ");
                AppParam.getInstance().appLockNumIdx = 1;
                break;
            case 3:
                AppParam.getInstance().appLockNum3 = " ";
                ((TextView) findViewById(R.id.main_applock_password_3_val)).setText(" ");
                AppParam.getInstance().appLockNumIdx = 2;
                break;
            case 4:
                AppParam.getInstance().appLockNum4 = " ";
                ((TextView) findViewById(R.id.main_applock_password_4_val)).setText(" ");
                AppParam.getInstance().appLockNumIdx = 3;
                break;
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        initAppLockNums();
        AppParam.getInstance().appLock = true;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        onResumeAppLock();
    }

    public void onResumeAppLock()
    {
        if (AppParam.getInstance().lastDialog != null)
        {
            if (AppParam.getInstance().lastDialog.isShowing())
            {
                AppParam.getInstance().lastDialog.dismiss();
            }
        }

        // 1. 앱에 등록된 패스워드가 있는지 검사
        if (AppPreferences.getInstance().isTherePassword(getApplicationContext()))
        {
            Log.d(TAG, "등록된 앱 비밀번호가 있습니다.");
            // 2. 등록된 패스워드가 있다면 패스워드 입력 화면을 출력
            if (AppParam.getInstance().appLock)
            {
                ConstraintLayout passLockLayout = findViewById(R.id.main_applock_layout);
                passLockLayout.setVisibility(View.VISIBLE);

                ((TextView) findViewById(R.id.main_applock_desc)).setText("앱 비밀번호를 입력해주세요.");
                initAppLockNums();
            }
        }
        else
        {
            Log.d(TAG, "등록된 앱 비밀번호가 없습니다.");
            // 3. 등록된 패스워드가 없다면 패스워드 등록 화면을 출력
            AppParam.getInstance().appLockPasswordRegister = true;
            AppParam.getInstance().appLockNumVerify = false;
            // 잠금 화면 켜기
            ConstraintLayout passLockLayout = findViewById(R.id.main_applock_layout);
            passLockLayout.setVisibility(View.VISIBLE);
            // 텍스트뷰 설정
            ((TextView) findViewById(R.id.main_applock_desc)).setText("등록할 앱 비밀번호를 입력해주세요.");
            // 번호 초기화
            initAppLockNums();
        }
    }

    public void initAppLockNums()
    {
        Log.d(TAG, "앱 잠금 화면을 초기화 합니다.");

        AppParam.getInstance().appLockNumIdx = 0;

        AppParam.getInstance().appLockNum1 = " ";
        AppParam.getInstance().appLockNum2 = " ";
        AppParam.getInstance().appLockNum3 = " ";
        AppParam.getInstance().appLockNum4 = " ";

        TextView appLockNum1Tv = findViewById(R.id.main_applock_password_1_val);
        TextView appLockNum2Tv = findViewById(R.id.main_applock_password_2_val);
        TextView appLockNum3Tv = findViewById(R.id.main_applock_password_3_val);
        TextView appLockNum4Tv = findViewById(R.id.main_applock_password_4_val);

        appLockNum1Tv.setText(AppParam.getInstance().appLockNum1);
        appLockNum2Tv.setText(AppParam.getInstance().appLockNum2);
        appLockNum3Tv.setText(AppParam.getInstance().appLockNum3);
        appLockNum4Tv.setText(AppParam.getInstance().appLockNum4);
    }

    public void inputUnlockNum(int num)
    {
        String strNum = "" + num;

        Log.d(TAG, "번호 입력 = " + strNum);

        switch (AppParam.getInstance().appLockNumIdx)
        {
            case 0:
                AppParam.getInstance().appLockNum1 = strNum;
                ((TextView) findViewById(R.id.main_applock_password_1_val)).setText("*");
                AppParam.getInstance().appLockNumIdx = 1;
                break;
            case 1:
                AppParam.getInstance().appLockNum2 = strNum;
                ((TextView) findViewById(R.id.main_applock_password_2_val)).setText("*");
                AppParam.getInstance().appLockNumIdx = 2;
                break;
            case 2:
                AppParam.getInstance().appLockNum3 = strNum;
                ((TextView) findViewById(R.id.main_applock_password_3_val)).setText("*");
                AppParam.getInstance().appLockNumIdx = 3;
                break;
            case 3:
                AppParam.getInstance().appLockNum4 = strNum;
                ((TextView) findViewById(R.id.main_applock_password_4_val)).setText("*");
                AppParam.getInstance().appLockNumIdx = 4;
                break;
            default:
                break;
        }

        if (AppParam.getInstance().appLockNumIdx == 4)
        {
            // 앱 비밀번호 등록 과정
            if (AppParam.getInstance().appLockPasswordRegister)
            {
                // 확인을 위한 재 입력 과정
                if (AppParam.getInstance().appLockNumVerify)
                {
                    AppParam.getInstance().strSecondLockNum = AppParam.getInstance().appLockNum1 + AppParam.getInstance().appLockNum2 +
                            AppParam.getInstance().appLockNum3 + AppParam.getInstance().appLockNum4;

                    // 두 비밀번호 비교
                    if (AppParam.getInstance().strFirstLockNum.equals(AppParam.getInstance().strSecondLockNum))
                    {
                        Log.d(TAG, "등록을 위한 앱 비밀번호의 확인용 비밀번호가 일치합니다.");
                        // 비밀번호가 같을 때
                        AppPreferences.getInstance().setNewPassword(getApplicationContext(), AppParam.getInstance().strFirstLockNum);
                        AppParam.getInstance().appLockNumVerify = false;
                        AppParam.getInstance().appLockPasswordRegister = false;

                        initAppLockNums();

                        // 잠금 화면 해제
                        ConstraintLayout passLockLayout = findViewById(R.id.main_applock_layout);
                        passLockLayout.setVisibility(View.GONE);
                    }
                    else
                    {
                        // 비밓번호가 다를 때
                        AppParam.getInstance().appLockNumVerify = false;
                        ((TextView) findViewById(R.id.main_applock_desc)).setText("비밀번호가 다릅니다.\n등록할 앱 비밀번호를 다시 입력해주세요.");
                        initAppLockNums();
                    }
                }
                else // 비밀번호 첫 입력 과정
                {
                    Log.d(TAG, "등록을 위한 앱 비밀번호를 입력했습니다.");
                    AppParam.getInstance().strFirstLockNum = AppParam.getInstance().appLockNum1 + AppParam.getInstance().appLockNum2 +
                            AppParam.getInstance().appLockNum3 + AppParam.getInstance().appLockNum4;
                    AppParam.getInstance().appLockNumVerify = true;
                    ((TextView) findViewById(R.id.main_applock_desc)).setText("확인을 위해 앱 비밀번호를 다시 입력해주세요.");
                    initAppLockNums();
                }
            }
            else // 일반적인 앱 비밀번호 확인 과정
            {
                AppParam.getInstance().strFirstLockNum = AppParam.getInstance().appLockNum1 + AppParam.getInstance().appLockNum2 +
                        AppParam.getInstance().appLockNum3 + AppParam.getInstance().appLockNum4;

                if (AppPreferences.getInstance().isPasswordCorrect(getApplicationContext(), AppParam.getInstance().strFirstLockNum))
                {
                    Log.d(TAG, "앱 비밀번호가 일치합니다.");
                    initAppLockNums();

                    // 잠금 화면 해제
                    ConstraintLayout passLockLayout = findViewById(R.id.main_applock_layout);
                    passLockLayout.setVisibility(View.GONE);
                }
                else
                {
                    Log.d(TAG, "앱 비밀번호가 일치하지 않습니다.");
                    ((TextView) findViewById(R.id.main_applock_desc)).setText("앱 비밀번호가 다릅니다.\n앱 비밀번호를 다시 입력해주세요.");
                    initAppLockNums();
                }
            }
        }
    }

    public void onClickPassNumClicked(View view)
    {
        updateLongTimeIdleHandler(); // Update long time idle handler
        vibrator(5);
        LoggingUtils.getInstance().mSecretNumberCount = 0;

        switch (view.getId())
        {
            case R.id.main_applock_num_1_btn:
                inputUnlockNum(1);
                break;
            case R.id.main_applock_num_2_btn:
                inputUnlockNum(2);
                break;
            case R.id.main_applock_num_3_btn:
                inputUnlockNum(3);
                break;
            case R.id.main_applock_num_4_btn:
                inputUnlockNum(4);
                break;
            case R.id.main_applock_num_5_btn:
                inputUnlockNum(5);
                break;
            case R.id.main_applock_num_6_btn:
                inputUnlockNum(6);
                break;
            case R.id.main_applock_num_7_btn:
                inputUnlockNum(7);
                break;
            case R.id.main_applock_num_8_btn:
                inputUnlockNum(8);
                break;
            case R.id.main_applock_num_9_btn:
                inputUnlockNum(9);
                break;
            case R.id.main_applock_num_0_btn:
                inputUnlockNum(0);
                break;
        }
    }

    /**
     * Enable or disable showing no device screen on MainActivity
     */
    public void enableScreenNoDeviceRegistered(boolean enable)
    {
        // ESKIM start
        //enable = false;
        // ESKIM end
        if (enable)
        {
            findViewById(R.id.main_no_device_layout).setVisibility(View.VISIBLE);
        }
        else
        {
            findViewById(R.id.main_no_device_layout).setVisibility(View.GONE);
        }
    }

    public boolean isNoDeviceRegisteredScreenEnabled()
    {
        return findViewById(R.id.main_no_device_layout).getVisibility() == View.VISIBLE;
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
            Log.d(TAG, "Received broadcast : action = " + action);

            // Detect disabling Bluetooth
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_OFF)
                {
                    finish();
                    //enableBluetooth();
                }
            } // BluetoothAdapter.ACTION_STATE_CHANGED
            else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST))
            {
                Log.d(TAG, "Get action pairing request!");

                byte[] pinBytes = "123456".getBytes();
                //mBluetoothDevice.setPin(pinBytes);
                //mBluetoothDevice.setPairingConfirmation(true);

            }
            else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                if (mBluetoothDevice != null && mBluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE)
                {
                    Log.d(TAG, "Get action message : BOND_NONE");
                }
                else if (mBluetoothDevice != null && mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING)
                {
                    Log.d(TAG, "Get action message : BOND_BONDING");
                }
                else if (mBluetoothDevice != null && mBluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    Log.d(TAG, "Get action message : BOND_BONDED");

                    // Get currently running fragment number.
                    int currentFragmentNumber = AppParam.getInstance().getCurrentFragmentNumber();

                    switch (currentFragmentNumber)
                    {
                        case AppParam.FRAGMENT_NUMBER_HOME:
                        case AppParam.FRAGMENT_NUMBER_LIST:
                        {
                            Log.d(TAG, "onDescriptorWrite : Currently running fragment is home or device list. Therefore send password packet to server.");

                            if (!sendPacket(packetMaker(PACKET_HEADER_PASSWORD, AppParam.getInstance().currentConnectDevice.getDevicePassword().getBytes(), 5)))
                            {
                                Log.d(TAG, "onDescriptorWrite : Failed to send password packet.");
                                mBluetoothGatt.disconnect();
                            }

                        }
                        break;

                        case AppParam.FRAGMENT_NUMBER_SEARCH:
                        {
                            Log.d(TAG, "onDescriptorWrite : Currently running fragment is search. Therefore fragment change to password to get password from user.");
                            sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_PASSWORD));
                        }
                        break;
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
                getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new HomeFragment()).commitAllowingStateLoss();
            }
            // FRAGMENT HOME + STOP BLE SCAN
            else if (action.equals(ActionMessage.NEW_FRAGMENT_HOME_WITH_STOP_BLE_SCAN))
            {
                scanLe(false);
                getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new HomeFragment()).commitAllowingStateLoss();
            }
            // FRAGMENT PASSWORD
            else if (action.equals(ActionMessage.NEW_FRAGMENT_PASSWORD))
            {
                getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new PasswordFragment()).commitAllowingStateLoss();
            }
            // FRAGMENT SEARCH + DISCONNECT BLE
            else if (action.equals(ActionMessage.NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE))
            {
                if (mBluetoothGatt != null && mBluetoothDevice != null && AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                {
                    Log.d(TAG, "장치 연결을 해제합니다.");
                    mBluetoothGatt.disconnect();
                }

                getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new SearchFragment()).commitAllowingStateLoss();
            }
            // DATABASE CHECK EMPTY
            else if (action.equals(ActionMessage.DATABASE_CHECK_EMPTY))
            {
                AppParam.getInstance().registeredDevices = AppParam.getInstance().database.deviceDao().findAll();

                if (AppParam.getInstance().registeredDevices.size() == 0)
                {
                    enableScreenNoDeviceRegistered(true);
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
        updateLongTimeIdleHandler(); // Update long time idle handler
        // 홈 프래그먼트
        if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_HOME)
        {
            ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).makeDialogBackPressed();
        }
        // 기기 목록 프래그먼트
        else if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_LIST)
        {
            ((DeviceListFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).makeBackPressedWithoutDialog();
        }
        // 검색 프래그먼트
        else if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_SEARCH)
        {
            ((SearchFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).makeDialogBackPressed();
        }
        // 보안코드 프래그먼트
        else if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_PASSWORD)
        {
            ((PasswordFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).makeDialogBackPressed();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Toolbar
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Update toolbar menu and icons.
     */
    public void updateToolbar()
    {
        invalidateOptionsMenu(); // onCreateOptionsMenu will called.
    }

    /**
     * Callback for invalidateOptionMenu method.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.toolbar, menu);

        // Title
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false); // disable default title
        ((TextView) findViewById(R.id.toolbar_title)).setText(AppParam.getInstance().getMenuTitle()); // use custom title

        // Home button -> Used to 'Back Pressed'
        getSupportActionBar().setDisplayHomeAsUpEnabled(AppParam.getInstance().getMenuHome());

        // Menu
        menu.findItem(R.id.menu_search).setVisible(AppParam.getInstance().getMenuSearch());
        menu.findItem(R.id.menu_list).setVisible(AppParam.getInstance().getMenuList());
        menu.findItem(R.id.menu_auto_connection).setVisible(AppParam.getInstance().getMenuAutoConnection());

        if (AppParam.getInstance().isAutoConnectionEnabled())
        {
            menu.findItem(R.id.menu_auto_connection).setIcon(R.drawable.ic_toolbar_auto_connection_enabled);
        }
        else
        {
            menu.findItem(R.id.menu_auto_connection).setIcon(R.drawable.ic_toolbar_auto_connection_disabled);
        }

        //menu.findItem(R.id.menu_manual).setVisible(appParam.getMenuManual());
        //menu.findItem(R.id.menu_support).setVisible(appParam.getMenuSupport());

        return true;
    }

    DialogInterface.OnClickListener mDialogClickListenerMenuSearch = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialogInterface, int i)
        {
            updateLongTimeIdleHandler(); // Update long time idle handler
            if (mBluetoothDevice != null && mBluetoothGatt != null
                    && AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
            {
                Log.d(TAG, "새 장치 검색을 위해 현재 연결을 종료합니다.");
                AppParam.getInstance().isDisconnectedByUser = true;
                mBluetoothGatt.disconnect();
            }

            // Start search fragment...
            getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new SearchFragment()).commitAllowingStateLoss();
        }
    };

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        updateLongTimeIdleHandler(); // Update long time idle handler
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                onBackPressed();
            }
            break;

            case R.id.menu_search:
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyAlertDialogTheme);
                String message;

                if (!AppParam.getInstance().isAutoConnectionEnabled())
                {
                    message = getString(R.string.activity_main_dialog_message_please_enable_auto_connection);

                    builder.setPositiveButton(getString(R.string.dialog_message_ok), null);
                }
                else
                {
                    message = (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                            ? getString(R.string.activity_main_dialog_message_search_button_currently_connected)
                            : getString(R.string.activity_main_dialog_message_search_button_currently_disconnected);

                    builder.setNegativeButton(getString(R.string.dialog_message_no), null);
                    builder.setPositiveButton(getString(R.string.dialog_message_yes), mDialogClickListenerMenuSearch);
                }

                builder.setMessage(message);
                AppParam.getInstance().lastDialog = builder.create();
                AppParam.getInstance().lastDialog.show();
            }
            break;

            case R.id.menu_list:
            {
                if (false) // Test for infinitely sending packet.
                {
                    Thread thread = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            AppParam.getInstance().testVolume = 1;

                            while (AppParam.getInstance().bleConnectionState == AppParam.BLE_CONNECTION_STATE_CONNECTED)
                            {
                                Log.d(TAG, "[ TEST ] Make handler to Main Looper...!");

                                new Handler(Looper.getMainLooper()).post(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        boolean ret;

                                        ret = sendPacket(packetMaker(PACKET_HEADER_VALUE_VOLUME, new byte[]{AppParam.getInstance().testVolume}, 2));

                                        if (ret)
                                        {
                                            Log.d(TAG, "[ TEST ] Success to send test packet. volume = " + AppParam.getInstance().testVolume);

                                            AppParam.getInstance().testVolume++;

                                            if (AppParam.getInstance().testVolume == 11)
                                            {
                                                AppParam.getInstance().testVolume = 1;
                                            }
                                        }
                                        else
                                        {
                                            Log.d(TAG, "[ TEST ] Failed to send test packet. May be busy...");
                                        }
                                    }
                                });

                                try
                                {
                                    sleep(200);
                                }
                                catch (InterruptedException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                            Log.d(TAG, "[ TEST ] Finish while...!");
                        }
                    };
                    thread.start();
                }
                else
                {
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, new DeviceListFragment()).commitAllowingStateLoss();
                }
            }
            break;

            case R.id.menu_auto_connection:
            {
                if (AppParam.getInstance().isAutoConnectionEnabled())
                {
                    makeDialogStopAutoConnection();
                }
                else
                {
                    AppParam.getInstance().setAutoConnectionEnabled(true);
                    /*
                    SharedPreferences sp = getSharedPreferences(AppParam.SP_NAME, Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString(AppParam.AUTO_CONN_SP_NAME, AppParam.AUTO_CONN_SP_YES);
                    editor.commit();
                     */
                    AppPreferences.getInstance().setAutoConnectionToEnabled(getApplicationContext(), true);

                    updateToolbar();

                    switch (AppParam.getInstance().getCurrentFragmentNumber())
                    {
                        case AppParam.FRAGMENT_NUMBER_HOME:
                        case AppParam.FRAGMENT_NUMBER_LIST:
                            scanLe(true);
                            break;
                    }
                }

                updateToolbar();
            }
            break;

            case R.id.menu_manual:
            {
                //Toast.makeText(getApplicationContext(), "매뉴얼 버튼이 눌렸습니다.", Toast.LENGTH_SHORT).show();

                ((CheckBox) findViewById(R.id.main_user_guide_checkbox)).setChecked(
                        !AppPreferences.getInstance().isManualEnabled(getApplicationContext()));

                enableScreenUserGuide(true);
            }
            break;

            case R.id.menu_support:
                Toast.makeText(getApplicationContext(), "고객지원 버튼이 눌렸습니다.", Toast.LENGTH_SHORT).show();
                break;
        }

        return false;
    }

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
    }

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

    /**
     * Start or stop Bluetooth LE scanner.
     */
    public void scanLe(boolean enable)
    {
        if (enable) // If true, start scanner.
        {
            if (!AppParam.getInstance().isBleScanning && AppParam.getInstance().isAutoConnectionEnabled()) // Can be started when currently not scanning.
            {
                AppParam.getInstance().isBleScanning = true; // Set state.

                // Clear Sound Processor found list.
                AppParam.getInstance().searchedDevices.clear();

                if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_SEARCH)
                {
                    ((SearchFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).clearAllSearchItems();

                    // ESKIM start
                    /*
                    Device searchDevice = new Device();
                    searchDevice.setDeviceName("1a2b3c4d");
                    searchDevice.setDeviceMacAddress("");
                    searchDevice.setDeviceModel("01");
                    AppParam.getInstance().searchedDevices.add(searchDevice);
                    ((SearchFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateSearchItemFromActivity();
                    */
                    // ESKIM end
                }

                // Make handler for Bluetooth LE scanner running time.
                scanHandler.postDelayed(scanRunner, DELAY_IN_MS_FOR_BLUETOOTH_LE_SCANNER);

                // Scan configuration
                ScanSettings.Builder settingBuilder = new ScanSettings.Builder();
                settingBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
                settingBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE);
                settingBuilder.setNumOfMatches(ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT);
                settingBuilder.setReportDelay(0);
                //settingBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
                settingBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                ScanSettings scanSettings = settingBuilder.build();

                //
                // Filter configuration
                //
                ScanFilter.Builder filterBuilder = new ScanFilter.Builder();

                if (enable) // 무조건 enable == true 인 구간
                {
                    ParcelUuid serviceDataUuid = new ParcelUuid(UUID.fromString("00001407-0000-1000-8000-00805F9B34FB"));
                    filterBuilder.setServiceData(serviceDataUuid, new byte[]{(byte) 0x0005, (byte) 0x007F});
                    //filterBuilder.setServiceData(serviceDataUuid, null);
                }
                else
                {
                    filterBuilder.setServiceUuid(BLE_ADVERTISING_SERVICE_UUID);
                }

                ScanFilter scanFilter = filterBuilder.build();
                List<ScanFilter> filterList = new ArrayList<>();
                filterList.add(scanFilter);

                // Start scanner.
                mBluetoothLeScanner.startScan(filterList, scanSettings, mScanCallback);

                Log.d(TAG, "scanLE : Bluetooth LE scan started.");
            }
        }
        else // If false, stop scanner.
        {
            if (AppParam.getInstance().isBleScanning) // Can be stopped when currently scanning.
            {
                scanHandler.removeCallbacks(scanRunner); // Remove callback for scan handler.
                mBluetoothLeScanner.stopScan(mScanCallback); // Stop scan.
                AppParam.getInstance().isBleScanning = false; // Set state.

                Log.d(TAG, "scanLE : Bluetooth LE scan stopped.");
            }
        }
    }

    /**
     * Bluetooth LE scan callback.
     */
    ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            BluetoothDevice btDevice = result.getDevice();
            String deviceName = btDevice.getName();

            Log.d(TAG, "Scanned device name = " + deviceName + ", address = " + btDevice.getAddress());

            if (deviceName != null && deviceName.matches("^TDC[0-9a-fA-F][0-9a-fA-F]@.*$"))
            {
                String model = deviceName.substring(3, 5);
                String name = deviceName.substring(6);
                String address = btDevice.getAddress();

                Log.d(TAG, "Scanned device name = " + name + ", model = " + model + ", address = " + address);

                // -- start
                Map<ParcelUuid, byte[]> map = result.getScanRecord().getServiceData();

                if (map != null)
                {
                    Log.d(TAG, "Service Data Size = " + map.size());

                    byte[] data0 = map.get(new ParcelUuid(UUID.fromString("00004944-0000-1000-8000-00805F9B34FB")));
                    byte[] data1 = map.get(new ParcelUuid(UUID.fromString("00005057-0000-1000-8000-00805F9B34FB")));

                    if (data0 != null)
                    {
                        for (int i = 0; i < data0.length; i++)
                        {
                            data0[i] = (byte) (((byte) 0x00ff) & data0[i]);
                        }

                        Log.d(TAG, "Data 0 = " + new String(data0, StandardCharsets.UTF_8));
                    }
                    else
                    {
                        Log.d(TAG, "Data 0 = null");
                    }

                    if (data1 != null)
                    {
                        for (int i = 0; i < data1.length; i++)
                        {
                            data1[i] = (byte) (((byte) 0x00ff) & data1[i]);
                        }

                        Log.d(TAG, "Data 1 = " + new String(data1, StandardCharsets.UTF_8));
                    }
                    else
                    {
                        Log.d(TAG, "Data 1 = null");
                    }
                }
                // -- end

                switch (AppParam.getInstance().getCurrentFragmentNumber())
                {
                    case AppParam.FRAGMENT_NUMBER_LIST:
                    case AppParam.FRAGMENT_NUMBER_HOME:
                    {
                        // Get registered Sound Processor list.
                        List<Device> registeredDevices = AppParam.getInstance().registeredDevices;

                        // Compare currnently found Sound Processor and Sound Processors from registered list.
                        for (int i = 0; i < registeredDevices.size(); i++)
                        {
                            // When found.
                            if (registeredDevices.get(i).getDeviceMacAddress().equals(address)
                                    && registeredDevices.get(i).getDeviceName().equals(name)
                                    && registeredDevices.get(i).getDeviceModel().equals(model))
                            {
                                // If currently not connected state.
                                if (AppParam.getInstance().bleConnectionState == AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                                {
                                    AppParam.getInstance().bleConnectionState = AppParam.BLE_CONNECTION_STATE_CONNECTING; // 연결중인 상태로 변경한다.

                                    scanLe(false); // Stop scan.

                                    Log.d(TAG, "onScanResult : model = " + model + ", name = " + name + ", address = " + address);

                                    // Get Sound Processor information from registered Sound Processor list based on device name, address and model.
                                    AppParam.getInstance().currentConnectDevice = new Device();

                                    AppParam.getInstance().currentConnectDevice.setDevicePassword(registeredDevices.get(i).getDevicePassword());
                                    AppParam.getInstance().currentConnectDevice.setDeviceSerial(registeredDevices.get(i).getDeviceSerial());
                                    AppParam.getInstance().currentConnectDevice.setDeviceName(registeredDevices.get(i).getDeviceName());
                                    AppParam.getInstance().currentConnectDevice.setDeviceMacAddress(registeredDevices.get(i).getDeviceMacAddress());
                                    AppParam.getInstance().currentConnectDevice.setDeviceModel(registeredDevices.get(i).getDeviceModel());
                                    AppParam.getInstance().currentConnectDevice.setMapDate(registeredDevices.get(i).getMapDate());
                                    AppParam.getInstance().currentConnectDevice.setMapCount(registeredDevices.get(i).getMapCount());
                                    AppParam.getInstance().currentConnectDevice.setImplantUserName(registeredDevices.get(i).getImplantUserName());
                                    AppParam.getInstance().currentConnectDevice.setImplantDirection(registeredDevices.get(i).getImplantDirection());
                                    AppParam.getInstance().currentConnectDevice.setDeviceFwVersion(registeredDevices.get(i).getDeviceFwVersion());
                                    AppParam.getInstance().currentConnectDevice.setDeviceFwType(registeredDevices.get(i).getDeviceFwType());

                                    // Try to connect.
                                    sendBroadcast(new Intent(ActionMessage.BLE_CONNECT));
                                    break;
                                }
                            }
                        }
                    }
                    break;

                    case AppParam.FRAGMENT_NUMBER_SEARCH:
                    {
                        List<Device> searchedDevices = AppParam.getInstance().searchedDevices;

                        boolean isExist = false;

                        // Is there same Sound Processor in list of found Sound Processor?
                        for (int i = 0; i < searchedDevices.size(); i++)
                        {
                            if (searchedDevices.get(i).getDeviceMacAddress().equals(address) && searchedDevices.get(i).getDeviceName().equals(name))
                            {
                                isExist = true;
                                break;
                            }
                        }

                        // Is there same Sound Processor in list of registered Sound Processor?
                        List<Device> registeredDevices = AppParam.getInstance().registeredDevices;

                        for (int i = 0; i < registeredDevices.size(); i++)
                        {
                            if (registeredDevices.get(i).getDeviceMacAddress().equals(address) && registeredDevices.get(i).getDeviceName().equals(name))
                            {
                                isExist = true;
                                break;
                            }
                        }

                        // If not exist, lists it to list of found Sound Processor.
                        if (!isExist)
                        {
                            Device device = new Device();

                            device.setDeviceName(name);
                            device.setDeviceModel(model);
                            device.setDeviceMacAddress(address);

                            searchedDevices.add(device);

                            ((SearchFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateSearchItemFromActivity();
                            Log.d(TAG, "onScanResult : model = " + model + ", name = " + name + ", address = " + address);
                        }
                    }
                    break;
                } // switch
            } // end matches
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

            if (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED
                    && !AppParam.getInstance().isBleBusy
                    && mBluetoothGatt != null && mBluetoothDevice != null)
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
            Log.d(TAG, "onConnectionStateChange : name = " + gatt.getDevice().getName() + ", address = " + gatt.getDevice().getAddress());

            new Handler(Looper.getMainLooper()).post(new Runnable()
            {
                @Override
                public void run()
                {
                    String name = gatt.getDevice().getName();
                    String address = gatt.getDevice().getAddress();

                    // DISCONNECTED
                    if (newState == BluetoothProfile.STATE_DISCONNECTED)
                    {
                        Log.d(TAG, "onConnectionStateChange : Disconnected name = " + name + ", address = " + address);

                        // 앱 강제 종료하는 시점에서는 로깅 데이터베이스가 닫혀 있을 수 있다.
                        // 이런 경우 데이터베이스를 다시 열고 저장한 후 닫도록 수행한다.
                        if (LoggingUtils.mLoggingDatabase == null)
                        {
                            LoggingUtils.getInstance().openLoggingDatabase(getApplicationContext());

                            writeMessage(LoggingUtils.LOGGING_DEVICE_DISCONNECTED, "name=" + name + ", addr=" + address); // Logging

                            LoggingUtils.getInstance().closeLoggingDatabase(getApplicationContext());
                        }
                        else
                        {
                            writeMessage(LoggingUtils.LOGGING_DEVICE_DISCONNECTED, "name=" + name + ", addr=" + address); // Logging
                        }

                        // 현재 연결 해제된 장치가 본딩이 되어도 괜찮은 장치인지 판별 후 아니면 삭제
                        {
                            // 1. 이 장치가 앱에 등록된 장치인가?
                            boolean isRegisteredDevice = false;
                            List<Device> deviceList = AppParam.getInstance().registeredDevices;

                            for (Device d : deviceList)
                            {
                                if (d.getDeviceMacAddress().equals(address))
                                {
                                    isRegisteredDevice = true;
                                    break;
                                }
                            }
                            // 2. 등록되어 있지 않다면, 스마트폰에 본딩되어 있는가?
                            if (!isRegisteredDevice)
                            {
                                // 3. 앱에 등록되어 있지 않은데 스마트폰에 본딩되어 있으면 본딩을 제거한다.
                                BtUtils.getInstance().eraseBondedDeviceUsingMacAddress(address, mBluetoothAdapter);
                            }
                        }

                        gatt.close();
                        mBluetoothGatt = null;
                        mBluetoothDevice = null;
                        AppParam.getInstance().bleConnectionState = AppParam.BLE_CONNECTION_STATE_DISCONNECTED;
                        AppParam.getInstance().currentConnectDevice = null;
                        AppParam.getInstance().isBleBusy = false;

                        // If invalid value error dialog is enabled, finish it and clear.
                        if (AppParam.getInstance().invalidValueDialog != null)
                        {
                            AppParam.getInstance().invalidValueDialog.dismiss();
                            AppParam.getInstance().invalidValueDialog = null;
                        }

                        // Remove callbacks from handler checking battery level.
                        checkBatteryHandler.removeCallbacks(checkBatteryRunner);

                        // Remove callbacks from handler checking timeout for discovering services.
                        discoverServiceTimeoutHandler.removeCallbacks(discoverServiceTimeoutRunner);

                        // Remove callbacks from handler checking timeout for response about packet sent.
                        packetTimeoutHandler.removeCallbacks(packetTimeoutRunner);
                        packetTimeoutHandler.removeCallbacks(packetTimeoutRunner2);

                        // willBeAppExit flag is set when onDestroy is called.
                        // It means that App will be exit so just return from this callback.
                        if (AppParam.getInstance().willBeAppExit)
                        {
                            return;
                        }

                        // Get currently running fragment number.
                        int currentFragmentNumber = AppParam.getInstance().getCurrentFragmentNumber();

                        switch (currentFragmentNumber)
                        {
                            case AppParam.FRAGMENT_NUMBER_HOME:
                            {
                                AppParam.getInstance().isDisconnectedByUser = false;

                                if (AppParam.getInstance().isAutoConnectionEnabled())
                                {
                                    scanLe(true);
                                }

                                ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateScreen();
                            }
                            break;

                            case AppParam.FRAGMENT_NUMBER_LIST:
                            {
                                AppParam.getInstance().isDisconnectedByUser = false;

                                if (AppParam.getInstance().isAutoConnectionEnabled())
                                {
                                    scanLe(true);
                                }
                            }
                            break;

                            case AppParam.FRAGMENT_NUMBER_SEARCH:
                            {
                                scanLe(true);

                                if (AppParam.getInstance().isDisconnectedByUser)
                                {
                                    AppParam.getInstance().isDisconnectedByUser = false;
                                }
                                else
                                {
                                    ((SearchFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).makeDialogCommunicationError();
                                }
                            }
                            break;

                            case AppParam.FRAGMENT_NUMBER_PASSWORD:
                            {
                                if (AppParam.getInstance().isDisconnectedByUser)
                                {
                                    AppParam.getInstance().isDisconnectedByUser = false;
                                }
                                else
                                {
                                    ((PasswordFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).makeDialogCommunicationError();
                                }
                            }
                            break;
                        } // switch

                        // "NO 장치 등록 화면"이 활성화 중이면 스캔을 안한다.
                        if (isNoDeviceRegisteredScreenEnabled())
                        {
                            scanLe(false);
                        }
                    }
                    // CONNECTED
                    else if (newState == BluetoothProfile.STATE_CONNECTED)
                    {
                        Log.d(TAG, "onConnectionStateChange : Connected name = " + gatt.getDevice().getName() + ", address = " + gatt.getDevice().getAddress());

                        writeMessage(LoggingUtils.LOGGING_DEVICE_CONNECTED,
                                "name=" + gatt.getDevice().getName()
                                        + ", addr=" + gatt.getDevice().getAddress()); // Logging

                        AppParam.getInstance().bleConnectionState = AppParam.BLE_CONNECTION_STATE_CONNECTED;

                        // Make handler to check timeout about discovering services.
                        discoverServiceTimeoutHandler.postDelayed(discoverServiceTimeoutRunner, DELAY_IN_MS_FOR_DISCOVER_SERVICES_TIMEOUT);
                        if (!gatt.discoverServices())
                        {
                            AppParam.getInstance().isDisconnectedByUser = false;
                            gatt.disconnect();
                        }
                    }
                }
            });


        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            Log.d(TAG, "onServiceDiscovered : name = " + gatt.getDevice().getName() + ", address = " + gatt.getDevice().getAddress());

            // Remove callback from handler checking timeout for discovering services.
            discoverServiceTimeoutHandler.removeCallbacks(discoverServiceTimeoutRunner);

            if (status != BluetoothGatt.GATT_SUCCESS)
            {
                Log.d(TAG, "onServiceDiscovered : Failed to discover service.");
                gatt.disconnect();
                return;
            }

            // Get service from gatt.
            mBluetoothGattService = gatt.getService(BLE_UUID_SERVICE);

            if (mBluetoothGattService == null)
            {
                Log.d(TAG, "onServiceDiscovered : Failed to get service.");
                gatt.disconnect();
                return;
            }

            // Get characteristics from service.
            mCharClientToServer = mBluetoothGattService.getCharacteristic(BLE_UUID_CHARACTERISTIC_CLIENT_TO_SERVER);
            mCharServerToClient = mBluetoothGattService.getCharacteristic(BLE_UUID_CHARACTERISTIC_SERVER_TO_CLIENT);

            if (mCharClientToServer == null || mCharServerToClient == null)
            {
                Log.d(TAG, "onServiceDiscovered : Failed to get characteristics.");
                gatt.disconnect();
                return;
            }

            // Get descriptor from characteristic {server to client}.
            BluetoothGattDescriptor descriptor = mCharServerToClient.getDescriptor(BLE_UUID_DESCRIPTION_CCCD);

            if (descriptor == null)
            {
                Log.d(TAG, "onServiceDiscovered : Failed to get descriptor.");
                gatt.disconnect();
                return;
            }

            // Success discovering service, characteristics and descriptor.
            Log.d(TAG, "onServiceDiscovered : Success to discover service, characteristics and descriptor.");

            // Enable indication receiver for characteristic {server to client} on App.
            if (!mBluetoothGatt.setCharacteristicNotification(mCharServerToClient, true))
            {
                Log.d(TAG, "onServiceDiscovered : Setting characteristic indication {server to client} is failed.");
                AppParam.getInstance().isDisconnectedByUser = false;
                gatt.disconnect();
            }

            Log.d(TAG, "onServiceDiscovered : Characteristic indication {server to client} is enabled.");

            // Set descriptor on remote device.
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            if (!mBluetoothGatt.writeDescriptor(descriptor))
            {
                Log.d(TAG, "onServiceDiscovered : Failed to write descriptor.");
                AppParam.getInstance().isDisconnectedByUser = false;
                gatt.disconnect();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            //super.onDescriptorWrite(gatt, descriptor, status);

            Log.d(TAG, "onDescriptorWrite : name = " + gatt.getDevice().getName() + ", address = " + gatt.getDevice().getAddress());

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                // Success to write descriptor for indication on remote device.
                Log.d(TAG, "onDescriptorWrite : Success to write descriptor.");

                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE)
                {
                    Log.d(TAG, gatt.getDevice().getName() + " not bonded.");

                    if (!gatt.getDevice().createBond())
                    {
                        Log.d(TAG, "onDescriptorWrite : Failed to create bond.");
                        gatt.disconnect();
                    }
                }
                else
                {
                    Log.d(TAG, gatt.getDevice().getName() + " already bonded.");
                }

                // Get currently running fragment number.
                int currentFragmentNumber = AppParam.getInstance().getCurrentFragmentNumber();

                switch (currentFragmentNumber)
                {
                    case AppParam.FRAGMENT_NUMBER_HOME:
                    case AppParam.FRAGMENT_NUMBER_LIST:
                    {
                        Log.d(TAG, "onDescriptorWrite : Currently running fragment is home or device list. Therefore send password packet to server.");

                        if (!sendPacket(packetMaker(PACKET_HEADER_PASSWORD, AppParam.getInstance().currentConnectDevice.getDevicePassword().getBytes(), 5)))
                        {
                            Log.d(TAG, "onDescriptorWrite : Failed to send password packet.");
                            gatt.disconnect();
                        }

                    }
                    break;

                    case AppParam.FRAGMENT_NUMBER_SEARCH:
                    {
                        Log.d(TAG, "onDescriptorWrite : Currently running fragment is search. Therefore fragment change to password to get password from user.");
                        sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_PASSWORD));
                    }
                    break;
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

                                if (currentFragmentNumber == AppParam.FRAGMENT_NUMBER_PASSWORD)
                                {
                                    // Show message on screen when currently running fragment is password.
                                    ((PasswordFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).readingInformation();
                                }

                                // Try to get Sound Processor information.
                                sendPacket(packetMaker(PACKET_HEADER_SOUND_PROCESSOR_INFO, null, 1));
                            }
                            // Not correct password.
                            else
                            {
                                Log.d(TAG, "onCharacteristicChanged : Not correct password.");

                                if (currentFragmentNumber == AppParam.FRAGMENT_NUMBER_PASSWORD)
                                {
                                    // Delete saved password.
                                    AppParam.getInstance().currentConnectDevice.setDevicePassword(null);

                                    // Show message on screen when currently running fragment is password.
                                    ((PasswordFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).wrongPassword();
                                }
                                else if (currentFragmentNumber == AppParam.FRAGMENT_NUMBER_HOME || currentFragmentNumber == AppParam.FRAGMENT_NUMBER_LIST)
                                {
                                    Log.d(TAG, "onCharacteristicChanged : Security password of Sound Processor is not correct. This can't be happened!");
                                    Toast.makeText(getApplicationContext(),
                                            "'" + gatt.getDevice().getName() + "' " + getString(R.string.activity_main_toast_message_password_why_different),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        } // PACKET_HEADER_PASSWORD
                        break;

                        // Sound Processor information
                        case PACKET_HEADER_SOUND_PROCESSOR_INFO:
                        {
                            if (packetSize < 13)
                            {
                                Log.d(TAG, "onCharacteristicChanged : Unexpected packet size error!");
                                Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_packet_length_error), Toast.LENGTH_LONG).show();
                                break;
                            }

                            String model = String.format("%02x", responsePacket[1] & 0xff);
                            String serial = String.format("%02x%02x%02x%02x%02x%02x%02x%02x",
                                    responsePacket[2] & 0xff, responsePacket[3] & 0xff, responsePacket[4] & 0xff, responsePacket[5] & 0xff,
                                    responsePacket[6] & 0xff, responsePacket[7] & 0xff, responsePacket[8] & 0xff, responsePacket[9] & 0xff);
                            String fwModel = String.format("%02x", responsePacket[10] & 0xff);
                            String fwVersion = String.format("%02x%02x", responsePacket[11] & 0xff, responsePacket[12] & 0xff);

                            AppParam.getInstance().currentConnectDevice.setDeviceModel(model);
                            AppParam.getInstance().currentConnectDevice.setDeviceSerial(serial);
                            AppParam.getInstance().currentConnectDevice.setDeviceFwType(fwModel);
                            AppParam.getInstance().currentConnectDevice.setDeviceFwVersion(fwVersion);

                            Log.d(TAG, "onCharacteristicChanged : model = " + model + ", serial = " + serial + ",  fwModel = " + fwModel + "fwVersion = " + fwVersion);

                            // Try to get map date information.
                            sendPacket(packetMaker(PACKET_HEADER_MAP_INFO, null, 1));
                        }
                        break;

                        // Map date information
                        case PACKET_HEADER_MAP_INFO:
                        {
                            if (packetSize < 8)
                            {
                                Log.d(TAG, "onCharacteristicChanged : Unexpected packet size error!");
                                Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_packet_length_error), Toast.LENGTH_LONG).show();
                                break;
                            }

                            String mapDate = String.format(Locale.ENGLISH, "%d/%d/%d/%d/%d",
                                    responsePacket[1] & 0xff, responsePacket[2] & 0xff, responsePacket[3] & 0xff, responsePacket[4] & 0xff, responsePacket[5] & 0xff);
                            String mapCount = String.format("%02x", responsePacket[6] & 0xff);
                            String direction = String.format("%02x", responsePacket[7] & 0xff);

                            AppParam.getInstance().currentConnectDevice.setMapDate(mapDate);
                            AppParam.getInstance().currentConnectDevice.setMapCount(mapCount);
                            AppParam.getInstance().currentConnectDevice.setImplantDirection(direction);

                            Log.d(TAG, "onCharacteristicChanged : mapDate = " + mapDate + ", mapCount = " + mapCount + ", direction = " + direction);

                            // Try to get implant user name.
                            sendPacket(packetMaker(PACKET_HEADER_ISD_USER_NAME, null, 1));
                        }
                        break;

                        // Implant user information
                        case PACKET_HEADER_ISD_USER_NAME:
                        {
                            int nameLength = packetSize - 1;
                            byte[] nameBytes = new byte[nameLength];
                            String nameString;

                            System.arraycopy(responsePacket, 1, nameBytes, 0, nameLength);

                            nameString = new String(nameBytes, StandardCharsets.UTF_8);

                            Log.d(TAG, "onCharacteristicChanged : Name bytes = " + printLogBytesToString(nameBytes));
                            Log.d(TAG, "onCharacteristicChanged : Name string = " + nameString);

                            AppParam.getInstance().currentConnectDevice.setImplantUserName(nameString);

                            Log.d(TAG, "onCharacteristicChanged : All required information has been obtained.");

                            // Fragment : password
                            if (currentFragmentNumber == AppParam.FRAGMENT_NUMBER_PASSWORD)
                            {
                                // Before register Sound Processor, check map date and implant user name.
                                boolean isNewUserRegistered = false;
                                boolean isDifferentMapDate = false;

                                Device currentDevice = AppParam.getInstance().currentConnectDevice;
                                List<Device> registeredDevices = AppParam.getInstance().registeredDevices;

                                for (int i = 0; i < registeredDevices.size(); i++)
                                {
                                    if (!currentDevice.getImplantUserName().equals(registeredDevices.get(i).getImplantUserName()))
                                    {
                                        isNewUserRegistered = true;
                                        Log.d(TAG, "onCharacteristicChanged : Found new implant user name!");
                                        break;
                                    }

                                    if (!currentDevice.getMapDate().equals(registeredDevices.get(i).getMapDate()))
                                    {
                                        isDifferentMapDate = true;
                                        Log.d(TAG, "onCharacteristicChanged : Different map date!");
                                    }
                                    else
                                    {
                                        isDifferentMapDate = false;
                                    }
                                }

                                // When new implant user found
                                if (isNewUserRegistered)
                                {
                                    // Found a new implant user name Sound Processor. Will you register this Sound Processor after delete everything being registered?
                                    ((PasswordFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).makeDialogRegisterNewUser();
                                }
                                // When different map date found
                                else if (isDifferentMapDate)
                                {
                                    // Found a different map date Sound Processor. Will you register this Sound Processor?
                                    ((PasswordFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).makeDialogIgnoreMapDate();
                                }
                                // When no matter
                                else
                                {
                                    AppParam.getInstance().database.deviceDao().insert(AppParam.getInstance().currentConnectDevice);
                                    AppParam.getInstance().registeredDevices = AppParam.getInstance().database.deviceDao().findAll();

                                    Log.d(TAG, "onCharacteristicChanged : Normally register this Sound Processor.");
                                    Log.d(TAG, "onCharacteristicChanged : After registration, database count = " + AppParam.getInstance().database.deviceDao().findAll().size());

                                    writeMessage(LoggingUtils.LOGGING_DEVICE_REGISTERED,
                                            "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                                                    + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                                                    + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                                    + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()); // Logging

                                    // Change fragment to home.
                                    sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_HOME));

                                    // Try to read Sound Processor's status.
                                    sendPacket(packetMaker(PACKET_HEADER_SOUND_PROCESSOR_STATUS, null, 1));
                                }
                            }
                            // Fragment : home or device list
                            else if (currentFragmentNumber == AppParam.FRAGMENT_NUMBER_HOME || currentFragmentNumber == AppParam.FRAGMENT_NUMBER_LIST)
                            {
                                // Make sure that the connected device is listed in registered devices.
                                Device registeredDevice = null;

                                for (int i = 0; i < AppParam.getInstance().registeredDevices.size(); i++)
                                {
                                    if (AppParam.getInstance().currentConnectDevice.getDeviceSerial().equals(AppParam.getInstance().registeredDevices.get(i).getDeviceSerial()))
                                    {
                                        registeredDevice = AppParam.getInstance().registeredDevices.get(i);
                                    }
                                }

                                if (registeredDevice == null)
                                {
                                    Log.d(TAG, "onCharacteristicChanged : Unexpected behavior is occurred. Why can not find Sound Processor from device list?");
                                    Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_can_not_find_sound_processor), Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                    if (!registeredDevice.getDeviceName().equals(AppParam.getInstance().currentConnectDevice.getDeviceName())
                                            || !registeredDevice.getDevicePassword().equals(AppParam.getInstance().currentConnectDevice.getDevicePassword())
                                            || !registeredDevice.getDeviceModel().equals(AppParam.getInstance().currentConnectDevice.getDeviceModel())
                                            || !registeredDevice.getDeviceFwType().equals(AppParam.getInstance().currentConnectDevice.getDeviceFwType())
                                            || !registeredDevice.getDeviceFwVersion().equals(AppParam.getInstance().currentConnectDevice.getDeviceFwVersion())
                                            || !registeredDevice.getImplantDirection().equals(AppParam.getInstance().currentConnectDevice.getImplantDirection())
                                            || !registeredDevice.getImplantUserName().equals(AppParam.getInstance().currentConnectDevice.getImplantUserName())
                                            || !registeredDevice.getMapCount().equals(AppParam.getInstance().currentConnectDevice.getMapCount())
                                            || !registeredDevice.getMapDate().equals(AppParam.getInstance().currentConnectDevice.getMapDate()))
                                    {
                                        Log.d(TAG, "onCharacteristicChanged : Sound Processor information is different with registered it.");
                                        Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_differnt_sound_processor_information), Toast.LENGTH_LONG).show();
                                    }
                                    else
                                    {
                                        Log.d(TAG, "onCharacteristicChanged : Successfully connected!");
                                        Log.d(TAG, "onCharacteristicChanged : Name = " + AppParam.getInstance().currentConnectDevice.getDeviceName());
                                        Log.d(TAG, "onCharacteristicChanged : Address = " + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress());
                                        Log.d(TAG, "onCharacteristicChanged : Map date = " + AppParam.getInstance().currentConnectDevice.getMapDate());
                                        Log.d(TAG, "onCharacteristicChanged : User = " + AppParam.getInstance().currentConnectDevice.getImplantUserName());
                                    }

                                    // Try to read Sound Processor's status.
                                    sendPacket(packetMaker(PACKET_HEADER_SOUND_PROCESSOR_STATUS, null, 1));

                                    vibrator(100);
                                }
                            }
                        } // 끝, 내부기 사용자 이름 읽기에 대한 응답
                        break;

                        // Sound Processor status
                        case PACKET_HEADER_SOUND_PROCESSOR_STATUS:
                        {
                            if (packetSize < 9)
                            {
                                Log.d(TAG, "onCharacteristicChanged : Unexpected packet size error!");
                                Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_packet_length_error), Toast.LENGTH_LONG).show();
                                break;
                            }

                            DeviceParam deviceParam = new DeviceParam();

                            deviceParam.setBattery((byte) (responsePacket[1] & 0xff));
                            deviceParam.setProgram((byte) (responsePacket[2] & 0xff));
                            deviceParam.setVolume((byte) (responsePacket[3] & 0xff));
                            deviceParam.setSensitivity((byte) (responsePacket[4] & 0xff));
                            deviceParam.setAlarmLed((byte) (responsePacket[5] & 0xff));
                            deviceParam.setTelecoil((byte) (responsePacket[6] & 0xff));
                            deviceParam.setAlarmStimulation((byte) (responsePacket[7] & 0xff));
                            deviceParam.setWarnning((byte) (responsePacket[8] & 0xff));

                            AppParam.getInstance().currentStatusParams = deviceParam;

                            Log.d(TAG, "onCharacteristicChanged : Successfully get Sound Processor status.");
                            Log.d(TAG, "onCharacteristicChanged : Battery = " + deviceParam.getBattery());
                            Log.d(TAG, "onCharacteristicChanged : Program = " + deviceParam.getProgram());
                            Log.d(TAG, "onCharacteristicChanged : Volume = " + deviceParam.getVolume());
                            Log.d(TAG, "onCharacteristicChanged : Sensitivity = " + deviceParam.getSensitivity());
                            Log.d(TAG, "onCharacteristicChanged : Alarm LED = " + deviceParam.getAlarmLed());
                            Log.d(TAG, "onCharacteristicChanged : Alarm Stimulation = " + deviceParam.getAlarmStimulation());
                            Log.d(TAG, "onCharacteristicChanged : Telecoil = " + deviceParam.getTelecoil());

                            // Check value validation.

                            boolean invalid = false;

                            // 1. Alarm stimulation.
                            if (deviceParam.getAlarmStimulation() != DeviceParam.ALARM_STIMULATION_ON && deviceParam.getAlarmStimulation() != DeviceParam.ALARM_STIMULATION_OFF)
                            {
                                invalid = true;
                                Log.d(TAG, "onCharacteristicChanged : Alarm Stimulation value is invalid. Current value = " + deviceParam.getAlarmStimulation());
                            }

                            // 2. Alarm LED.
                            if (deviceParam.getAlarmLed() != DeviceParam.ALARM_LED_ON && deviceParam.getAlarmLed() != DeviceParam.ALARM_LED_OFF)
                            {
                                invalid = true;
                                Log.d(TAG, "onCharacteristicChanged : Alarm LED value is invalid. Current value = " + deviceParam.getAlarmLed());
                            }

                            // 3. Telecoil.
                            if (deviceParam.getTelecoil() != DeviceParam.TELECOIL_ON && deviceParam.getTelecoil() != DeviceParam.TELECOIL_OFF)
                            {
                                invalid = true;
                                Log.d(TAG, "onCharacteristicChanged : Telecoil value is invalid. Current value = " + deviceParam.getTelecoil());
                            }

                            // 4-1. Program
                            if (Byte.parseByte(AppParam.getInstance().currentConnectDevice.getMapCount()) < DeviceParam.LIMIT_MIN_PROGRAM)
                            {
                                invalid = true;
                                Log.d(TAG, "onCharacteristicChanged : Program count is invalid. Current value = " + Byte.parseByte(AppParam.getInstance().currentConnectDevice.getMapCount()));
                            }

                            // 4-2. Program
                            if (deviceParam.getProgram() < DeviceParam.LIMIT_MIN_PROGRAM || Byte.parseByte(AppParam.getInstance().currentConnectDevice.getMapCount()) < deviceParam.getProgram())
                            {
                                invalid = true;
                                Log.d(TAG, "onCharacteristicChanged : Program value is invalid. Current value = " + deviceParam.getProgram());
                            }

                            // 5. Sensitivity
                            if (deviceParam.getSensitivity() < DeviceParam.LIMIT_MIN_SENSITIVITY || DeviceParam.LIMIT_MAX_SENSITIVITY < deviceParam.getSensitivity())
                            {
                                invalid = true;
                                Log.d(TAG, "onCharacteristicChanged : Sensitivity value is invalid. Current value = " + deviceParam.getSensitivity());
                            }

                            // 6. Volume
                            if (deviceParam.getVolume() < DeviceParam.LIMIT_MIN_VOLUME || DeviceParam.LIMIT_MAX_VOLUME < deviceParam.getVolume())
                            {
                                invalid = true;
                                Log.d(TAG, "onCharacteristicChanged : Volume value is invalid. Current value = " + deviceParam.getVolume());
                            }

                            // 7. Battery
                            if (deviceParam.getBattery() < DeviceParam.LIMIT_MIN_BATTERY || DeviceParam.LIMIT_MAX_BATTERY < deviceParam.getBattery())
                            {
                                invalid = true;
                                Log.d(TAG, "onCharacteristicChanged : Battery value is invalid. Current value = " + deviceParam.getBattery());
                            }

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

                            if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_HOME)
                            {
                                // Update screen when currently running fragment is home.
                                ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateScreen();
                            }

                            writeMessage(LoggingUtils.LOGGING_VALUE_STATUS,
                                    "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                                            + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                                            + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                            + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()
                                            + ", invalid=" + invalid + ", batt=" + deviceParam.getBattery()); // Logging
                        }
                        break;

                        // Alarm stimulation
                        case PACKET_HEADER_VALUE_SIMULATION:
                        {
                            DeviceParam deviceParam = AppParam.getInstance().sendingStatusParams;

                            // Invalid value.
                            if (responsePacket[1] < DeviceParam.LIMIT_MIN_ALARM_STIMULATION || DeviceParam.LIMIT_MAX_ALARM_STIMULATION < responsePacket[1]
                                    || deviceParam.getAlarmStimulation() != responsePacket[1])
                            {
                                if (AppParam.getInstance().resendCount > 5)
                                {
                                    makeDialogRelaunchApp(); // Relaunch dialog.
                                }
                                else
                                {
                                    AppParam.getInstance().resendCount++;

                                    Log.d(TAG, "onCharacteristicChanged : Alarm stimulation value is invalid. Try to send packet again.");
                                    if (!sendPacket(packetMaker(PACKET_HEADER_VALUE_SIMULATION, new byte[]{deviceParam.getAlarmStimulation()}, 2)))
                                    {
                                        makeDialogRelaunchApp(); // Relaunch dialog.
                                    }
                                }

                                break;
                            }

                            // Save value to current status parameter.
                            AppParam.getInstance().currentStatusParams.setAlarmStimulation(deviceParam.getAlarmStimulation());
                            AppParam.getInstance().resendCount = 0;

                            writeMessage(LoggingUtils.LOGGING_VALUE_STIMULATION,
                                    "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                                            + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                                            + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                            + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()
                                            + " : " + AppParam.getInstance().currentStatusParams.getAlarmStimulation()); // Logging

                            // Update screen.
                            if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_HOME)
                            {
                                ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateScreen();
                                vibrator(100);
                            }
                        }
                        break;

                        // Alarm LED
                        case PACKET_HEADER_VALUE_LED:
                        {
                            DeviceParam deviceParam = AppParam.getInstance().sendingStatusParams;

                            // Invalid value range.
                            if (responsePacket[1] < DeviceParam.LIMIT_MIN_ALARM_LED || DeviceParam.LIMIT_MAX_ALARM_LED < responsePacket[1]
                                    || deviceParam.getAlarmLed() != responsePacket[1])
                            {
                                if (AppParam.getInstance().resendCount > 5)
                                {
                                    makeDialogRelaunchApp(); // Relaunch dialog.
                                }
                                else
                                {
                                    AppParam.getInstance().resendCount++;

                                    Log.d(TAG, "onCharacteristicChanged : Alarm LED value is invalid. Try to send packet again.");

                                    if (!sendPacket(packetMaker(PACKET_HEADER_VALUE_LED, new byte[]{deviceParam.getAlarmLed()}, 2)))
                                    {
                                        makeDialogRelaunchApp(); // Relaunch dialog.
                                    }
                                }

                                break;
                            }

                            // save value to current status parameter.
                            AppParam.getInstance().currentStatusParams.setAlarmLed(deviceParam.getAlarmLed());
                            AppParam.getInstance().resendCount = 0;

                            writeMessage(LoggingUtils.LOGGING_VALUE_LED,
                                    "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                                            + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                                            + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                            + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()
                                            + " : " + AppParam.getInstance().currentStatusParams.getAlarmLed()); // Logging

                            if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_HOME)
                            {
                                ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateScreen();
                                vibrator(100);
                            }
                        }
                        break;

                        // Alarm Telecoil
                        case PACKET_HEADER_VALUE_TELECOIL:
                        {
                            DeviceParam deviceParam = AppParam.getInstance().sendingStatusParams;

                            // Invalid value range.
                            if (responsePacket[1] < DeviceParam.LIMIT_MIN_TELECOIL || DeviceParam.LIMIT_MAX_TELECOIL < responsePacket[1]
                                    || deviceParam.getTelecoil() != responsePacket[1])
                            {
                                if (AppParam.getInstance().resendCount > 5)
                                {
                                    makeDialogRelaunchApp(); // Relaunch dialog.
                                }
                                else
                                {
                                    AppParam.getInstance().resendCount++;

                                    Log.d(TAG, "onCharacteristicChanged : Telecoil value is invalid. Try to send packet again.");

                                    if (!sendPacket(packetMaker(PACKET_HEADER_VALUE_TELECOIL, new byte[]{deviceParam.getTelecoil()}, 2)))
                                    {
                                        makeDialogRelaunchApp(); // Relaunch dialog.
                                    }
                                }

                                break;
                            }

                            // Save value to current status parameter.
                            AppParam.getInstance().currentStatusParams.setTelecoil(deviceParam.getTelecoil());
                            AppParam.getInstance().resendCount = 0;

                            writeMessage(LoggingUtils.LOGGING_VALUE_TELECOIL,
                                    "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                                            + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                                            + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                            + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()
                                            + " : " + AppParam.getInstance().currentStatusParams.getTelecoil()); // Logging

                            if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_HOME)
                            {
                                ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateScreen();
                                vibrator(100);
                            }
                        }
                        break;

                        // Power Mode
                        case PACKET_HEADER_VALUE_POWER_MODE:
                        {
                            DeviceParam deviceParam = AppParam.getInstance().sendingStatusParams;

                            // Invalid value range.
                            if (responsePacket[1] < DeviceParam.LIMIT_MIN_POWER_MODE || DeviceParam.LIMIT_MAX_POWER_MODE < responsePacket[1]
                                    || deviceParam.getPowerMode() != responsePacket[1])
                            {
                                if (AppParam.getInstance().resendCount > 5)
                                {
                                    makeDialogRelaunchApp(); // Relaunch dialog.
                                }
                                else
                                {
                                    AppParam.getInstance().resendCount++;

                                    Log.d(TAG, "onCharacteristicChanged : Power mode value is invalid. Try to send packet again.");

                                    if (!sendPacket(packetMaker(PACKET_HEADER_VALUE_POWER_MODE, new byte[]{deviceParam.getPowerMode()}, 2)))
                                    {
                                        makeDialogRelaunchApp();
                                    }
                                }

                                break;
                            }

                            // Save value to current status parameter.
                            AppParam.getInstance().currentStatusParams.setPowerMode(deviceParam.getPowerMode());
                            AppParam.getInstance().resendCount = 0;

                            writeMessage(LoggingUtils.LOGGING_VALUE_POWER_MODE,
                                    "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                                            + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                                            + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                            + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()
                                            + " : " + AppParam.getInstance().currentStatusParams.getPowerMode()); // Logging

                            if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_HOME)
                            {
                                ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateScreen();
                                vibrator(100);
                            }
                        }
                        break;

                        // Program
                        case PACKET_HEADER_VALUE_PROMGRAM:
                        {
                            DeviceParam deviceParam = AppParam.getInstance().sendingStatusParams;

                            // Invalid value range.
                            if (responsePacket[1] < DeviceParam.LIMIT_MIN_PROGRAM
                                    || Byte.parseByte(AppParam.getInstance().currentConnectDevice.getMapCount()) < responsePacket[1]
                                    || deviceParam.getProgram() != responsePacket[1])
                            {
                                if (AppParam.getInstance().resendCount > 5)
                                {
                                    makeDialogRelaunchApp(); // Relaunch dialog.
                                }
                                else
                                {
                                    AppParam.getInstance().resendCount++;

                                    Log.d(TAG, "onCharacteristicChanged : Program value is invalid. Try to send packet again.");
                                    if (!sendPacket(packetMaker(PACKET_HEADER_VALUE_PROMGRAM, new byte[]{deviceParam.getProgram()}, 2)))
                                    {
                                        makeDialogRelaunchApp();// Relaunch dialog.
                                    }
                                }

                                break;
                            }

                            // Save value to current status parameter.
                            AppParam.getInstance().currentStatusParams.setProgram(deviceParam.getProgram());
                            AppParam.getInstance().resendCount = 0;

                            writeMessage(LoggingUtils.LOGGING_VALUE_PROGRAM,
                                    "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                                            + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                                            + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                            + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()
                                            + " : " + AppParam.getInstance().currentStatusParams.getProgram()); // Logging

                            if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_HOME)
                            {
                                ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateScreen();
                                vibrator(100);
                            }
                        }
                        break;
                        // Sensitivity
                        case PACKET_HEADER_VALUE_SENSITIVITY:
                        {
                            DeviceParam deviceParam = AppParam.getInstance().sendingStatusParams;

                            // Invalid value range.
                            if (responsePacket[1] < DeviceParam.LIMIT_MIN_SENSITIVITY || DeviceParam.LIMIT_MAX_SENSITIVITY < responsePacket[1]
                                    || deviceParam.getSensitivity() != responsePacket[1])
                            {
                                if (AppParam.getInstance().resendCount > 5)
                                {
                                    makeDialogRelaunchApp(); // Relaunch dialog.
                                }
                                else
                                {
                                    AppParam.getInstance().resendCount++;

                                    Log.d(TAG, "onCharacteristicChanged : Sensitivity value is invalid. Try to send packet again.");
                                    if (!sendPacket(packetMaker(PACKET_HEADER_VALUE_SENSITIVITY, new byte[]{deviceParam.getSensitivity()}, 2)))
                                    {
                                        makeDialogRelaunchApp(); // Relaunch dialog.
                                    }
                                }

                                break;
                            }

                            // Save value to current status parameter.
                            AppParam.getInstance().currentStatusParams.setSensitivity(deviceParam.getSensitivity());
                            AppParam.getInstance().resendCount = 0;

                            writeMessage(LoggingUtils.LOGGING_VALUE_SENSITIVITY,
                                    "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                                            + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                                            + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                            + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()
                                            + " : " + AppParam.getInstance().currentStatusParams.getSensitivity()); // Logging

                            if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_HOME)
                            {
                                ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateScreen();
                                vibrator(100);
                            }
                        }
                        break;
                        // Volume
                        case PACKET_HEADER_VALUE_VOLUME:
                        {
                            DeviceParam deviceParam = AppParam.getInstance().sendingStatusParams;

                            // Invalid value range.
                            if (responsePacket[1] < DeviceParam.LIMIT_MIN_VOLUME || DeviceParam.LIMIT_MAX_VOLUME < responsePacket[1]
                                    || deviceParam.getVolume() != responsePacket[1])
                            {
                                if (AppParam.getInstance().resendCount > 5)
                                {
                                    makeDialogRelaunchApp(); // Relaunch dialog.
                                }
                                else
                                {
                                    AppParam.getInstance().resendCount++;

                                    Log.d(TAG, "onCharacteristicChanged : Volume value is invalid. Try to send packet again.");
                                    sendPacket(packetMaker(PACKET_HEADER_VALUE_VOLUME, new byte[]{deviceParam.getVolume()}, 2));
                                }

                                break;
                            }

                            // Save value to current status parameter.
                            AppParam.getInstance().currentStatusParams.setVolume(deviceParam.getVolume());
                            AppParam.getInstance().resendCount = 0;

                            writeMessage(LoggingUtils.LOGGING_VALUE_VOLUME,
                                    "name=" + AppParam.getInstance().currentConnectDevice.getDeviceName()
                                            + ", addr=" + AppParam.getInstance().currentConnectDevice.getDeviceMacAddress()
                                            + ", serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                            + ", user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()
                                            + " : " + AppParam.getInstance().currentStatusParams.getVolume()); // Logging

                            if (AppParam.getInstance().getCurrentFragmentNumber() == AppParam.FRAGMENT_NUMBER_HOME)
                            {
                                ((HomeFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.main_frame))).updateScreen();
                                vibrator(100);
                            }
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
                                    // Do nothing...
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

            if (mCharClientToServer.setValue(AppParam.getInstance().sendingPacket)
                    && mBluetoothGatt.writeCharacteristic(mCharClientToServer))
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

        if (AppParam.getInstance().bleConnectionState == AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
        {
            Toast.makeText(getApplicationContext(), getString(R.string.activity_main_toast_message_ble_not_connected), Toast.LENGTH_SHORT).show();
            return false;
        }

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
                if (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED
                        && mBluetoothDevice != null && mBluetoothGatt != null)
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

    /**
     * Make a dialog to stop auto connection feature.
     */
    public void makeDialogStopAutoConnection()
    {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.MyAlertDialogTheme);

        String message = (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                ? getString(R.string.activity_main_dialog_message_auto_connection_button_currently_connected)
                : getString(R.string.activity_main_dialog_message_auto_connection_button_currently_disconnected);

        builder.setMessage(message);

        builder.setNegativeButton(getString(R.string.dialog_message_no), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                updateLongTimeIdleHandler(); // Update long time idle handler
            }
        });

        builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                updateLongTimeIdleHandler(); // Update long time idle handler
                // Stop auto connection
                AppParam.getInstance().setAutoConnectionEnabled(false);
                AppPreferences.getInstance().setAutoConnectionToEnabled(getApplicationContext(), false);

                updateToolbar();

                if (mBluetoothDevice != null && mBluetoothGatt != null
                        && AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                {
                    Log.d(TAG, "자동 연결을 종료합니다.");
                    AppParam.getInstance().isDisconnectedByUser = true;
                    mBluetoothGatt.disconnect();
                }
            } // onClick
        }); // OnClickListener() for dialog positive button

        AppParam.getInstance().lastDialog = builder.create();
        AppParam.getInstance().lastDialog.show();
    }

    public void writeMessage(int type, String message)
    {
        LoggingUtils.getInstance().writeMessage(LoggingUtils.getInstance().typeMessage(type) + " : " + message);
    }
}