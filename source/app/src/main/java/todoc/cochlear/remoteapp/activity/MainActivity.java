package todoc.cochlear.remoteapp.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.ota.Ota;
import todoc.cochlear.remoteapp.fragment.AddDeviceFragment;
import todoc.cochlear.remoteapp.fragment.AddUserFragment;
import todoc.cochlear.remoteapp.fragment.DeviceFragment;
import todoc.cochlear.remoteapp.fragment.EditDeviceFragment;
import todoc.cochlear.remoteapp.fragment.EditUserFragment;
import todoc.cochlear.remoteapp.fragment.LogFragment;
import todoc.cochlear.remoteapp.fragment.ManualFragment;
import todoc.cochlear.remoteapp.fragment.RemoteControlFragment;
import todoc.cochlear.remoteapp.fragment.SettingsFragment;
import todoc.cochlear.remoteapp.fragment.UserFragment;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.params.Status;
import todoc.cochlear.remoteapp.params.PacketInfo;
import todoc.cochlear.remoteapp.service.ExitCaptureService;
import todoc.cochlear.remoteapp.shared_preferences.LockScreen;
import todoc.cochlear.remoteapp.shared_preferences.ManualScreen;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

public class MainActivity extends AppCompatActivity
{
    static private final String TAG = "TODOC_" + MainActivity.class.getSimpleName();

    static private final int REQUEST_PERMISSION_CODE_NUMBER = 100;

    static public final  UUID       BLE_UUID_SERVICE                         = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    static public final  UUID       BLE_UUID_CHARACTERISTIC_CLIENT_TO_SERVER = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    static public final  UUID       BLE_UUID_CHARACTERISTIC_SERVER_TO_CLIENT = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    static public final  UUID       BLE_UUID_DESCRIPTION_CCCD                = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    static private final ParcelUuid SERVICE_DATA_UUID                        = new ParcelUuid(UUID.fromString("00004944-0000-1000-8000-00805F9B34FB"));
    static private final String     BT_NAME_REGEX_FILTER                     = "^TD_.*$";

    static private final int DELAY_IN_MS_FOR_PACKET_RESPONSE_TIMEOUT = 1000;

    static private final int LONG_TIME_IDLE_TIMEOUT_IN_MS    = 3600000; // 1시간
    // 링크 감시 주기. 현재 Tx 파워는 매 주기, 기기 상태 정보는 15주기(30초)마다 읽는다.
    static public final  int LINK_MONITOR_PERIOD_IN_MS       = 2000;
    static public final  int LINK_MONITOR_STATUS_TICK_COUNT  = 15;

    // 링크 파라미터 전체 읽기를 하는 주기 위치. 기기 상태(0주기)와 겹치지 않게 떨어뜨린다.
    static public final  int LINK_MONITOR_PARAM_TICK      = 8;
    static public final  int DISCOVER_SERVICES_TIMEOUT_IN_MS = 2000;
    static public final  int CCCD_TIMEOUT_IN_MS              = 2000;
    static public final  int PASSWORD_TIMEOUT_IN_MS          = 1000;
    static public final  int STATUS_TIMEOUT_IN_MS            = 1000;
    static public final  int DEVICE_AND_MAP_INFO_IN_MS       = 1000;
    static public final  int SEND_PACKET_DELAY_IN_MS         = 1;//50;

    // Bluetooth
    public BluetoothDevice mBluetoothDevice;
    public BluetoothGatt   mBluetoothGatt;
    BluetoothManager            mBluetoothManager;
    BluetoothAdapter            mBluetoothAdapter;
    BluetoothLeScanner          mBluetoothLeScanner;
    BluetoothGattService        mBluetoothGattService;
    BluetoothGattCharacteristic mCharClientToServer;
    BluetoothGattCharacteristic mCharServerToClient;

    // 뷰 바인딩
    public ActivityMainBinding mBinding;
    public Status              mStatus;

    // 잠금화면 관련
    public LockScreen mLockScreen;

    // 매뉴얼 화면 관련
    public ManualScreen mManualScreen;

    // 뷰모델 관련
    private StatusViewModel mStatusViewModel;

    // 사용자 전환 메뉴 버튼 관련
    private String[] mUserList;
    private int      mCheckItem;

    public Ota mOta;

    @Override
    protected void onPause()
    {
        super.onPause();

        mStatus.activityRunningState = Status.ACTIVITY_RUNNING_STATE_NOT_FOREGROUND;

        // 화면이 가려질 때는 BLE 스캔 정지
        if (mStatus.scanState == Status.SCAN_STATE_STARTED)
        {
            scanLe(false);
        }

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);

        if (fragment instanceof AddDeviceFragment)
        {
            ((AddDeviceFragment) fragment).mAddDeviceBinding.addDevieSerialEdittext.setError(null);
            ((AddDeviceFragment) fragment).mAddDeviceBinding.addDeviePairingKeyEdittext.setError(null);
            ((AddDeviceFragment) fragment).mAddDeviceBinding.addDeviceOptionEdittext.setError(null);
        }
        else if (fragment instanceof EditDeviceFragment)
        {
            ((EditDeviceFragment) fragment).mEditDeviceBinding.editDeviceSerialEdittext.setError(null);
            ((EditDeviceFragment) fragment).mEditDeviceBinding.editDeviePairingKeyEdittext.setError(null);
            ((EditDeviceFragment) fragment).mEditDeviceBinding.editDeviceOptionEdittext.setError(null);
        }
        else if (fragment instanceof AddUserFragment)
        {
            ((AddUserFragment) fragment).mAddUserBinding.nameEdittext.setError(null);
            ((AddUserFragment) fragment).mAddUserBinding.passkeyEdittext.setError(null);
            ((AddUserFragment) fragment).mAddUserBinding.nicknameEdittext.setError(null);
        }
        else if (fragment instanceof EditUserFragment)
        {
            ((EditUserFragment) fragment).mEditUserBinding.edittextName.setError(null);
            ((EditUserFragment) fragment).mEditUserBinding.edittextName.clearFocus();
            ((EditUserFragment) fragment).mEditUserBinding.edittextPasskey.setError(null);
            ((EditUserFragment) fragment).mEditUserBinding.edittextName.clearFocus();
            ((EditUserFragment) fragment).mEditUserBinding.edittextNickname.setError(null);
            ((EditUserFragment) fragment).mEditUserBinding.edittextName.clearFocus();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        mStatus.activityRunningState = Status.ACTIVITY_RUNNING_STATE_FOREGROUND;

        // 장시간 미사용 이벤트 핸들러 시작
        Log.d(TAG, "액티비티 onResume() 상태이므로, 장시간 미사용 핸들러를 업데이트합니다.");
        longTimeIdleHandlerUpdate(true);

        lastDialogDismiss();

        // Check lock screen password whether registered or not.
        mLockScreen.resume();

    } // End, onResume();

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        Log.d(TAG, "액티비티를 종료합니다. -> onDestroy()");

        // 모든 파일 접근 권한 안내 다이얼로그 제거 (윈도우 누수 방지)
        if (mAllFilesAccessDialog != null)
        {
            if (mAllFilesAccessDialog.isShowing())
            {
                mAllFilesAccessDialog.dismiss();
            }

            mAllFilesAccessDialog = null;
        }

        // 장시간 미사용 핸들러를 제거
        longTimeIdleHandlerUpdate(false);

        // BLE가 연결된 상태인지 판별하고 사용자에 의한 연결해제로 연결해제를 진행.
        if (mStatus.connectionState != Status.CONNECTION_STATE_DISCONNECTED)
        {
            if (mStatus.connectionState != Status.CONNECTION_STATE_DISCONNECTING)
            {
                if (mBluetoothGatt != null)
                {
                    UtilLog.instance.writeLog("앱 종료 : 연결중인 장치이름=" + mBluetoothGatt.getDevice().getName());

                    mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                    mBluetoothGatt.disconnect();
                }
            }
        }

        // BLE 스캔 중이라면 스캔 정지.
        if (mStatus.scanState == Status.SCAN_STATE_STARTED)
        {
            scanLe(false);
        }

        // 강제 종료 포착 서비스가 동작중이면 서비스 정지.
        if (mStatus.exitCaptureState == Status.EXIT_CAPTURE_SERVICE_STATE_STARTED)
        {
            Intent intent = new Intent(this, ExitCaptureService.class);
            stopService(intent);
            mStatus.exitCaptureState = Status.EXIT_CAPTURE_SERVICE_STATE_STOPPED;
        }

        // 방송수신자 해제
        appUnregisterReceiver(); // Unregister broadcast receiver

        // 데이터베이스 관련
        UtilLog.instance.close(); // 히든 로그 데이터베이스 닫기.
        UtilUser.instance.close(); // 사용자 데이터베이스 닫기.
        UtilDevice.instance.close(); // 기기 데이터베이스 닫기.

        // 모든 핸들러 제거하기
        mLongTimeIdleHandler.removeCallbacks(mLongTimeIdleRunner); // 장시간 미사용 감지 핸들러 제거
        mScanHandler.removeCallbacks(mScanRunner); // 스캔 핸들러 제거
        stopLinkMonitor(); // 링크 감시 타이머 제거
        mDiscoverServicesHandler.removeCallbacks(mDiscoverServicesRunner); // 서비스 검색 핸들러 제거
        mCCCDHandler.removeCallbacks(mCCCDRunner); // CCCD 설정 핸들러 제거
        mPasswordHandler.removeCallbacks(mPasswordRunner); // 보안코드 인증 핸들러 제거
        mStatusHandler.removeCallbacks(mStatusRunner); // 사운드처리기 상태 정보 획득 핸들러 제거
        mDeviceAndMapInfoHandler.removeCallbacks(mDeviceAndMapInfoRunner); // 사운드처리기 기기 및 맵 정보 읽기 핸들러 제거
        mPacketResponseTimeoutHandler.removeCallbacks(mPacketResponseTimeoutRunner); // 패킷 응답 시간 초과 핸들러 제거
        mPacketSendHandler.removeCallbacks(mPacketSendRunner); // 패킷 전송 핸들러 제거

        mStatusViewModel = null; // 뷰모델 객체 제거
        mManualScreen = null; // 매뉴얼화면 객체 제거
        mLockScreen = null; // 잠금화면 객체 제거

        // 다이얼로그 생성되어있으면 제거
        lastDialogDismiss();

        // 리모컨 화면 다이얼로그 제거
        Fragment fragment = getSupportFragmentManager().findFragmentById(mBinding.frame.getId());
        if (fragment instanceof RemoteControlFragment)
        {
            if (((RemoteControlFragment) fragment).mDialog != null)
            {
                if (((RemoteControlFragment) fragment).mDialog.isShowing())
                {
                    ((RemoteControlFragment) fragment).mDialog.dismiss();
                }
            }
        }
    }

    //
    // 화면 방향 — 폰이면 세로, 태블릿이면 가로로 «실행할 때» 정해 고정한다
    //
    // 안드로이드에는 «이 기기가 태블릿인가» 를 알려 주는 API 가 없다. 가장 작은 변의 길이로
    // 미루어 판정하는 것이 관례이고, 이 값은 방향을 돌려도 바뀌지 않아 기준으로 삼기 좋다.
    //
    // 600 은 res/values-sw600dp 폴더가 쓰는 경계와 같은 값이다. 둘이 어긋나면 방향은
    // 태블릿으로 잡히는데 치수는 폰 것을 쓰는 상태가 된다.
    //
    static private final int TABLET_MIN_SMALLEST_WIDTH_DP = 600;

    /* 실행할 때 정한 분류. 액티비티가 다시 만들어져도 같은 값을 쓰도록 프로세스에 남긴다.
     * 폴더블을 접거나 펴도 이 값은 그대로다. 바꾸려면 앱을 다시 시작해야 한다. */
    static private Boolean sIsTabletAtLaunch = null;

    private boolean mScreenClassDialogShown = false;

    static private boolean isTabletScreen(Configuration config)
    {
        return (TABLET_MIN_SMALLEST_WIDTH_DP <= config.smallestScreenWidthDp);
    }

    /* 실행 시점에 정한 화면 분류. 화면 구성이 이 값을 따라야 하므로 밖에서 읽을 수 있게 연다.
     *
     * 리소스 한정자(values-sw600dp)로 판정하지 않는 이유가 있다. 리소스는 구성이 바뀌면
     * 즉시 다시 풀리지만 이 값은 실행 시점에 고정된다. 폴더블을 접었을 때 둘이 어긋난다.
     * 방향 고정과 같은 근거를 써야 화면이 한 몸으로 움직인다. */
    static public boolean isTabletAtLaunch()
    {
        return (sIsTabletAtLaunch != null && sIsTabletAtLaunch);
    }

    private void applyScreenOrientation()
    {
        if (sIsTabletAtLaunch == null)
        {
            sIsTabletAtLaunch = isTabletScreen(getResources().getConfiguration());
        }

        int orientation = sIsTabletAtLaunch //
                          ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE //
                          : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        setRequestedOrientation(orientation);

        Log.d(TAG, "[SCREEN] 가장 작은 변 " + getResources().getConfiguration().smallestScreenWidthDp + "dp" //
                   + " -> " + (sIsTabletAtLaunch ? "태블릿, 가로 고정" : "폰, 세로 고정"));
    }

    /* 폴더블을 접거나 펴서 구성이 바뀌었을 때 불린다.
     *
     * 매니페스트에 configChanges 를 선언해 두었으므로 액티비티가 재생성되지 않는다.
     * 크기 분류가 그대로면 아무것도 하지 않는다 - 쓰던 방향이 실행 중에 뒤집히지 않게 하려는 것이다.
     * 분류가 바뀌었을 때만 이유를 알리고 다시 시작한다. */
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        boolean isTabletNow = isTabletScreen(newConfig);

        if (sIsTabletAtLaunch != null && isTabletNow == sIsTabletAtLaunch)
        {
            return;
        }

        if (mScreenClassDialogShown)
        {
            return; // 이미 안내 중이다
        }

        mScreenClassDialogShown = true;

        Log.d(TAG, "[SCREEN] 화면 분류가 바뀌었습니다 -> " + (isTabletNow ? "태블릿" : "폰"));

        makeDialog_screenClassChanged(isTabletNow);
    }

    private void makeDialog_screenClassChanged(boolean isTabletNow)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("화면 크기가 ");
        sb.append(sIsTabletAtLaunch ? "태블릿" : "폰");
        sb.append("에서 ");
        sb.append(isTabletNow ? "태블릿" : "폰");
        sb.append("으로 바뀌었습니다.\n\n");
        sb.append(isTabletNow ? "가로" : "세로");
        sb.append(" 화면으로 앱을 다시 시작합니다.");

        /* 전송 중이었으면 그 사실을 알린다. 다시 시작하면 이어지지 않는다. */
        if (mOta != null && mOta.commState != Ota.COMM_STATE_IDLE)
        {
            sb.append("\n\n[주의] OTA 전송이 진행 중입니다. 다시 시작하면 전송이 중단됩니다.");
        }

        lastDialogDismiss();

        mStatus.lastDialog = new MaterialAlertDialogBuilder(this) //
                .setTitle("화면 방향 변경") //
                .setMessage(sb.toString()) //
                .setPositiveButton("다시 시작", (dialogInterface, i) -> restartForScreenClass()) //
                .setCancelable(false) //
                .create();

        mStatus.lastDialog.show();
    }

    /* 앱을 처음부터 다시 띄운다.
     *
     * 방향은 액티비티가 만들어질 때 정해지므로 화면만 갈아 끼워서는 깔끔하게 바뀌지 않는다.
     * 실행할 때 정한 분류도 프로세스에 남아 있어, 프로세스를 새로 띄우는 편이 확실하다. */
    private void restartForScreenClass()
    {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());

        if (intent == null)
        {
            Log.e(TAG, "[SCREEN] 실행 인텐트를 찾지 못했습니다. 다시 시작하지 못합니다.");

            mScreenClassDialogShown = false;
            return;
        }

        Log.d(TAG, "[SCREEN] 화면 분류가 바뀌어 앱을 다시 시작합니다.");
        UtilLog.instance.writeLog("화면 분류 변경으로 앱 재시작");

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();

        Runtime.getRuntime().exit(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        /* savedInstanceState 를 그대로 넘기면 FragmentManager 가 이전 실행의 프래그먼트를
         * 여기서 되살린다. 그런데 되살아난 프래그먼트의 onCreate 는 바로 아래에서 만드는
         * mBinding 을 참조하므로, 아직 null 인 상태에서 접근해 NullPointerException 이 난다.
         *
         * 이 앱은 화면을 복원해서 쓰지 않고 initMainActivity() 에서 항상 직접 만들어 붙이므로,
         * 복원 자체를 하지 않도록 null 을 넘긴다.
         *
         * 참고: 화면 방향은 아래 applyScreenOrientation() 이 건다. 매니페스트로 고정하지 않는
         * 이유는 폰이냐 태블릿이냐를 실행할 때 봐야 하기 때문이다. 매니페스트에 configChanges 를
         * 선언해 두어 방향을 걸어도 액티비티가 재생성되지 않는다. */
        super.onCreate(null);

        applyScreenOrientation();

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = mBinding.getRoot();
        setContentView(view);

        Log.d(TAG, " \r\n*\r\n* *\r\n* * *\r\n* * * *\r\n* * * * *\r\n* * * * * *\r\n* * * * * * *\r\n* * * * * * * *\r\n* * * * * * * * *\n* * * * * * * * *" + " *");
        Log.d(TAG, "액티비티가 실행되었습니다. -> onCreate()");

        if (mStatus == null)
        {
            mStatus = Status.instance();
        }

        if (mOta == null)
        {
            mOta = Ota.getInstance();
        }

        mStatus.typeOfFragment = Status.TypeOfFragment.NONE;

        mLockScreen = new LockScreen(getApplicationContext(), MainActivity.this, mBinding); // 잠금화면 객체 생성

        initStatusNavigationToolBar(); // 상태바, 네비게이션바, 툴바 초기화

        // 권한 체크
        if (grantPermissions())
        {
            // 블루투스 활성화 체크
            if (enableBluetooth())
            {
                initMainActivity(); // 메인 액티비티 관련 초기화
            }
        }
    } // onCreate

    //
    // 메인 액티비티 초기화 함수
    //
    public void initMainActivity()
    {
        // 앱 종료 캡처 서비스 생성
        if (mStatus.exitCaptureState == Status.EXIT_CAPTURE_SERVICE_STATE_STOPPED)
        {
            Log.d(TAG, "강제 종료 포착 서비스를 시작합니다.");
            Intent intent = new Intent(this, ExitCaptureService.class);
            intent.setAction(ExitCaptureService.ACTION_START_SERVICE);
            startService(intent);
        }
        else
        {
            Log.d(TAG, "강제 종료 포착 서비스가 이미 동작중입니다.");
        }

        // 데이터 베이스 열기
        // 1) 로그
        UtilLog.instance.open(getApplicationContext());

        // 2) 사용자
        UtilUser.instance.open(getApplicationContext());

        // 3) 사운드처리기
        UtilDevice.instance.open(getApplicationContext());

        // 상태 값 뷰 모델
        Log.d(TAG, "사운드처리기 상태 값 뷰모델 클래스를 불러옵니다.");
        mStatusViewModel = new ViewModelProvider(this).get(StatusViewModel.class);
        mStatusViewModel.setConnectionState(StatusViewModel.CONNECTION_STATE_DISCONNECTED);

        mStatusViewModel.setValueIsdID(0);
        mStatusViewModel.setValueBatteryLevel(PacketInfo.INIT_VALUE_BATTERY);
        mStatusViewModel.setValueNotification(PacketInfo.INIT_VALUE_NOTIFICATION);
        mStatusViewModel.setValueLed(PacketInfo.INIT_VALUE_LED);
        mStatusViewModel.setValueTelecoil(PacketInfo.INIT_VALUE_TELECOIL);
        mStatusViewModel.setValueMaxOutput(PacketInfo.INIT_VALUE_MAX_OUTPUT);
        mStatusViewModel.setValueVolume(PacketInfo.INIT_VALUE_VOLUME);
        mStatusViewModel.setValueProgram(PacketInfo.INIT_VALUE_PROGRAM);

        // 방송수신자 등록
        appRegisterReceiver();

        // 블루투스 관련 객체 획득
        initBluetooth();

        // 생명주기 상 onResume 전에 onCreate가 호출된다. 실핼되는 최초의 한번은 onCreate에서 LockScreen.resume()을 호출해야
        // 리모컨 프래그먼트에서 LockScreen 활성화 여부에 따른 스캔 시작/정지를 결정한다.
        Log.d(TAG, "앱 실행 직후에 따른 잠금화면 초기화를 수행합니다.");
        mLockScreen.resume();

        // Shared Preferences for Manual Screen.
        if (mManualScreen == null)
        {
            mManualScreen = new ManualScreen(this);
        }

        Log.d(TAG, "Manual screen enabled = " + mManualScreen.isEnabled());

        if (mManualScreen.isEnabled())
        {
            Log.d(TAG, "사용설명서 표시가 활성화 되어 있습니다.");
            Bundle bundle = new Bundle();
            bundle.putBoolean(ManualFragment.ARG_FIRST_SCREEN, true);
            /*
            ManualFragment manualFragment = new ManualFragment();
            manualFragment.setArguments(bundle);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, manualFragment).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.MANUAL, bundle);
        }
        else // If the user manual screen is disabled, the first screen must be the remote control screen.
        {
            Log.d(TAG, "사용설명서 표시가 비활성화 되어 있습니다. 리모컨 화면을 출력합니다.");
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new RemoteControlFragment()).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.REMOTE_CONTROL);
        }

        /* OTA 이미지 읽기에 필요한 '모든 파일 접근' 권한을 확인한다.
         * 이 권한은 OTA 파일 읽기에만 쓰이므로, 앱 초기화를 끝낸 뒤에 안내한다.
         * 초기화보다 먼저 안내하면 권한을 얻기 전까지 DB와 화면이 준비되지 않아
         * 프래그먼트가 생성되는 순간 NullPointerException 이 발생한다. */
        checkAllFilesAccess();
    } // initMainActivity

    //
    // 장시간 미사용 이벤트 핸들러 관련
    //
    public void longTimeIdleHandlerUpdate(boolean enable)
    {
        if (enable)
        {
            mLongTimeIdleHandler.removeCallbacks(mLongTimeIdleRunner);
            mLongTimeIdleHandler.postDelayed(mLongTimeIdleRunner, LONG_TIME_IDLE_TIMEOUT_IN_MS); // 10분
            Log.v(TAG, "장시간 미사용 핸들러 업데이트 완료.");
        }
        else
        {
            mLongTimeIdleHandler.removeCallbacks(mLongTimeIdleRunner);
            Log.v(TAG, "장시간 미사용 핸들러 제거 완료.");
        }
    }

    private final Handler  mLongTimeIdleHandler = new Handler();
    private final Runnable mLongTimeIdleRunner  = () ->
    {
        Log.d(TAG, "장시간 미사용으로 인해 자동 절전모드로 진입합니다.");

        if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED || mStatus.connectionState == Status.CONNECTION_STATE_CONNECTING)
        {
            if (mBluetoothGatt != null)
            {
                Log.d(TAG, "현재 연결중인 사운드처리기와 연결을 해제합니다.");
                mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING; // 사용자에 의한 연결 해제 설정
                mBluetoothGatt.disconnect();
            }
        }

        if (mStatus.scanState == Status.SCAN_STATE_STARTED)
        {
            Log.d(TAG, "스캔을 정지합니다.");
            scanLe(false);
        }

        // 현재 리모컨 화면이고, 다이얼로그가 팝업되어 있다면 해당 다이얼로그를 제거한다.
        Fragment fragment = MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.frame);
        if (fragment instanceof RemoteControlFragment)
        {
            if (((RemoteControlFragment) fragment).mDialog != null && ((RemoteControlFragment) fragment).mDialog.isShowing())
            {
                Log.d(TAG, "리모컨 화면의 등록 유도 다이얼로그를 제거합니다.");
                ((RemoteControlFragment) fragment).mDialog.dismiss();
            }
        }

        lastDialogDismiss();
        mLockScreen.resume();
    };

    //
    // 런타임 권한 요청 결과
    //
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "권한 요청에 대한 결과입니다.");

        if (requestCode == REQUEST_PERMISSION_CODE_NUMBER)
        {
            boolean access_fine_location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean bluetooth_privileged = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_PRIVILEGED) == PackageManager.PERMISSION_GRANTED;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                boolean bluetooth_scan    = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
                boolean bluetooth_connect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

                if ((!bluetooth_scan) || (!bluetooth_connect))
                {
                    Log.d(TAG, "권한 획득이 안된 항목이 있습니다." + "LOCATION=" + access_fine_location
                            //+ ", PRIVILEGED=" + bluetooth_privileged
                            + ", SCAN=" + bluetooth_scan + ", CONNECT=" + bluetooth_connect);
                    UtilLog.instance.writeLog("앱 사용을 위한 블루투스 및 위치 관련 권한 요청이 거부됨.");
                    finish();
                    return;
                }
                else
                {
                    UtilLog.instance.writeLog("앱 사용을 위한 블루투스 및 위치 관련 권한이 획득됨.");
                }
            }
            else
            {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
                {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        Log.d(TAG, "외부 저장소 권한 획득 실패");
                        finish();
                        return;
                    }
                }

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "위치 권한 획득 실패");
                    UtilLog.instance.writeLog("위치 권한 획득 실패");
                    finish();
                    return;
                }
                else
                {
                    UtilLog.instance.writeLog("위치 권한 획득 성공");
                }
            }

            if (enableBluetooth())
            {
                initMainActivity(); // Initiailze all of MainActivity
            }
        }
    }

    //
    // 런타임 권한 요청
    //
    public boolean grantPermissions()
    {
        boolean access_fine_location = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean bluetooth_privileged = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_PRIVILEGED) == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // Android 12 이상
        {
            Log.d(TAG, "런타임 권한을 체크합니다. 버전코드가 S 이상입니다.");
            boolean bluetooth_scan    = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean bluetooth_connect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            if ((!bluetooth_scan) || (!bluetooth_connect))
            {
                Log.d(TAG, "권한 획득이 안된 항목이 있습니다." + "LOCATION=" + access_fine_location + ", SCAN=" + bluetooth_scan + ", CONNECT=" + bluetooth_connect);
                String[] permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE_NUMBER);
                UtilLog.instance.writeLog("앱 사용을 위한 블루투스 및 위치 관련 권한 요청.");
                return false;
            }
        }
        else
        {
            ArrayList<String> permissionList = new ArrayList<>();
            String[]          permissionArray;

            Log.d(TAG, "런타임 권한 체크 → 버전 코드 S 미만으로 확인");

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            {
                Log.d(TAG, "세부: 안드로이드 10, API29, 버전 코드 Q 이하");

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "외부 저장소 권한 없음 → 권한 요청 시도");
                    permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                Log.d(TAG, "위치 권한 없음 → 권한 요청 시도");
                UtilLog.instance.writeLog("위치 권한 요청");
                permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (!permissionList.isEmpty())
            {
                permissionArray = permissionList.toArray(new String[0]);
                ActivityCompat.requestPermissions(this, permissionArray, REQUEST_PERMISSION_CODE_NUMBER);

                return false;
            }
        }

        return true;
    }

    //
    // 0x59 요청 만들기 (릴리즈 4 공통 포맷)
    //
    // 요청 : [0x59, 옵션, 액세스, 인덱스, 값?]
    // 도메인이 무엇이든 같은 모양이라 이 두 함수로 전부 만든다.
    //
    public byte[] makeRcReadPacket(int option, int index)
    {
        byte[] packet = new byte[PacketInfo.RC_REQ_LEN_READ];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[PacketInfo.RC_REQ_OFS_OPTION] = (byte) (option & 0xFF);
        packet[PacketInfo.RC_REQ_OFS_ACCESS] = (byte) PacketInfo.RC_ACCESS_READ;
        packet[PacketInfo.RC_REQ_OFS_INDEX] = (byte) (index & 0xFF);

        return packet;
    }

    public byte[] makeRcWritePacket(int option, int index, int value)
    {
        byte[] packet = new byte[PacketInfo.RC_REQ_LEN_WRITE];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[PacketInfo.RC_REQ_OFS_OPTION] = (byte) (option & 0xFF);
        packet[PacketInfo.RC_REQ_OFS_ACCESS] = (byte) PacketInfo.RC_ACCESS_WRITE;
        packet[PacketInfo.RC_REQ_OFS_INDEX] = (byte) (index & 0xFF);
        packet[PacketInfo.RC_REQ_OFS_VALUE] = (byte) (value & 0xFF);

        return packet;
    }

    //
    // 구 프로토콜(릴리즈 3) 패킷
    //
    //   읽기 : [0x59, 옵션]        쓰기 : [0x59, 옵션, 값]
    //
    // 백텔 주기(구 옵션 4)만 읽기와 쓰기가 옵션 하나를 공유한다. 값 0 이 읽기라서
    // 읽기도 세 바이트로 보내야 하므로 makeLegacyWritePacket(4, 0) 을 쓴다.
    //
    public byte[] makeLegacyReadPacket(int legacyOption)
    {
        byte[] packet = new byte[PacketInfo.LEGACY_REQ_LEN_READ];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[PacketInfo.LEGACY_RSP_OFS_OPTION] = (byte) (legacyOption & 0xFF);

        return packet;
    }

    public byte[] makeLegacyWritePacket(int legacyOption, int value)
    {
        byte[] packet = new byte[PacketInfo.LEGACY_REQ_LEN_WRITE];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[PacketInfo.LEGACY_RSP_OFS_OPTION] = (byte) (legacyOption & 0xFF);
        packet[PacketInfo.LEGACY_RSP_OFS_VALUE] = (byte) (value & 0xFF);

        return packet;
    }

    // 링크 파라미터 하나를 읽는다. 판별된 세대에 맞는 포맷으로 보낸다.
    public void sendLinkParamRead(int index)
    {
        if (mStatus.fwRelease >= Status.FW_RELEASE_4_PLUS)
        {
            sendPacket(makeRcReadPacket(PacketInfo.RC_OPT_LINK, index));
            return;
        }

        /* 전체 읽기는 구 프로토콜에서 옵션 17 이다. 인덱스 대응표에는 자리가 없으므로
         * 여기서 따로 잡아 준다. */
        if (index == PacketInfo.RC_IDX_ALL)
        {
            sendPacket(makeLegacyReadPacket(PacketInfo.LEGACY_OPT_READ_ALL));
            return;
        }

        int legacyOption = PacketInfo.getLegacyReadOption(index);

        if (legacyOption == PacketInfo.LEGACY_OPT_NONE)
        {
            Log.d(TAG, "[LINK] 이 세대에는 인덱스 " + index + " 읽기가 없습니다.");
            return;
        }

        /* 백텔 주기만 읽기와 쓰기가 같은 옵션이다. 값 0 을 실어야 읽기로 처리된다. */
        sendPacket((legacyOption == PacketInfo.getLegacyWriteOption(index)) //
                   ? makeLegacyWritePacket(legacyOption, 0) //
                   : makeLegacyReadPacket(legacyOption));
    }

    /* 링크 파라미터 하나를 쓴다. 화면의 각 버튼이 이 함수를 쓴다.
     *
     * 연결 타입이 정한 포맷으로 보낸다. 그 타입이 모르는 파라미터면 보내지 않고 알린다.
     * 모르는 옵션을 보내면 사운드처리기가 에러로 응답하거나 아예 응답하지 않아
     * 연결이 끊기기 때문이다. */
    public boolean sendLinkParam(int index, int value)
    {
        if (!isLinkSettingUsable(index))
        {
            Log.d(TAG, "[LINK] 세대 " + mStatus.fwRelease + " 는 인덱스 " + index + " 쓰기를 모릅니다.");

            Toast.makeText(getApplicationContext(), //
                           "이 펌웨어(" + fwReleaseName() + ")에는 없는 설정입니다.", //
                           Toast.LENGTH_SHORT).show();
            return false;
        }

        Log.d(TAG, "[LINK] 쓰기 요청 : 인덱스=" + index + ", 값=" + value);

        longTimeIdleHandlerUpdate(true);

        if (mStatus.fwRelease >= Status.FW_RELEASE_4_PLUS)
        {
            sendPacket(makeRcWritePacket(PacketInfo.RC_OPT_LINK, index, value));
            return true;
        }

        sendPacket(makeLegacyWritePacket(PacketInfo.getLegacyWriteOption(index), value));

        return true;
    }

    /* 맵 강제 초기화. REL4+ 는 자극 도메인(0x21), REL4 는 구 옵션 8 이다.
     * REL3 에는 이 기능 자체가 없다. 잘못 보내면 맵이 날아가므로 세대를 확인하고 보낸다. */
    public void sendMapInit(int mapInitType)
    {
        if (!isMapInitSupported())
        {
            Log.d(TAG, "[STIM] 세대 " + mStatus.fwRelease + " 는 맵 초기화를 모릅니다.");

            Toast.makeText(getApplicationContext(), //
                           "이 펌웨어(" + fwReleaseName() + ")에는 없는 기능입니다.", //
                           Toast.LENGTH_SHORT).show();
            return;
        }

        longTimeIdleHandlerUpdate(true);

        sendPacket((mStatus.fwRelease >= Status.FW_RELEASE_4_PLUS) //
                   ? makeRcWritePacket(PacketInfo.RC_OPT_STIM, PacketInfo.RC_STIM_IDX_MAP_INIT, mapInitType) //
                   : makeLegacyWritePacket(PacketInfo.LEGACY_OPT_MAP_INIT, mapInitType));
    }

    //
    // 세대별 기능 지원 여부
    //
    // 화면의 버튼 활성화와 전송 차단이 같은 판단을 써야 어긋나지 않는다.
    //
    /* 화면과 로그에 쓰는 세대 이름.
     *
     * 버전 조회가 되는 세대는 «REL4+» 로 뭉뚱그리지 않고 읽어 온 major.minor 를 그대로
     * 쓴다. 지금 최신이 4.3 이면 «REL4.3» 이다. 어느 인덱스까지 있는지가 minor 로
     * 갈리므로, 화면에 그 숫자가 보여야 무엇이 되고 안 되는지 설명이 된다.
     *
     * 버전 조회가 없는 세대는 물어볼 방법이 없어 그대로 REL3 · REL4 다. */
    public String fwReleaseName()
    {
        switch (mStatus.fwRelease)
        {
            case Status.FW_RELEASE_3:
                return "REL3";

            case Status.FW_RELEASE_4:
                return "REL4";

            case Status.FW_RELEASE_4_PLUS:
                /* 버전을 못 읽은 채 이 세대로 잡히는 경우는 없다. 버전 응답이 와야
                 * REL4+ 가 되기 때문이다. 그래도 값이 비면 옛 표기로 물러난다. */
                if (mStatus.rcProtocolMajor == PacketInfo.LINK_VALUE_UNKNOWN //
                    || mStatus.rcProtocolMinor == PacketInfo.LINK_VALUE_UNKNOWN)
                {
                    return "REL4+";
                }

                return "REL" + mStatus.rcProtocolMajor + "." + mStatus.rcProtocolMinor;

            default:
                return "REL?";
        }
    }

    /* 링크 파라미터 하나를 이 세대가 아는지.
     *
     * REL4+ 는 도메인 구조라 인덱스를 그대로 쓴다. 다만 전원 안정 Nop(인덱스 11)은
     * 프로토콜 4.1 에서 생겼으므로 버전을 함께 본다. 버전 관리가 되는 세대라
     * 그 위로는 버전만 보면 되고 세대를 더 쪼갤 필요가 없다.
     *
     * REL4 는 인덱스 1~8 까지다. 링크 제어 모드(9)와 상승 가속(10)은 그 뒤에 생겼다.
     * 구 옵션 대응표에 자리가 있어도 이 경계를 넘으면 그 세대에는 없는 것이다.
     *
     * REL3 는 링크 파라미터가 아예 없다. */
    public boolean isLinkParamSupported(int index)
    {
        if (mStatus.fwRelease >= Status.FW_RELEASE_4_PLUS)
        {
            /* 인덱스는 프로토콜 버전이 올라가면서 뒤에 붙었다. 어느 버전에서 생겼는지를
             * 보고 가른다. 세대(REL4+)만으로는 4.0 인지 4.3 인지 알 수 없다. */
            switch (index)
            {
                case PacketInfo.RC_LINK_IDX_POWER_STABLE_NOP:
                    return isProtocolAtLeast(PacketInfo.RC_PROTOCOL_MAJOR_REQUIRED, PacketInfo.RC_PROTOCOL_MINOR_NOP);

                case PacketInfo.RC_LINK_IDX_PULSE_WIDTH:
                case PacketInfo.RC_LINK_IDX_FRAME_NUM:
                case PacketInfo.RC_LINK_IDX_NOP_ENABLE:
                    return isProtocolAtLeast(PacketInfo.RC_PROTOCOL_MAJOR_REQUIRED, PacketInfo.RC_PROTOCOL_MINOR_NOP_TABLE);

                case PacketInfo.RC_LINK_IDX_STIM_STRATEGY:
                    return isProtocolAtLeast(PacketInfo.RC_PROTOCOL_MAJOR_REQUIRED, PacketInfo.RC_PROTOCOL_MINOR_STRATEGY);

                /* 4.5 에서 생겼다. default 로 떨어뜨리면 «index <= RC_LINK_IDX_MAX» 만 보므로
                 * RC_LINK_IDX_MAX 를 16 으로 올린 순간 4.3·4.4 기기에서도 «지원» 이 되어 버린다. */
                case PacketInfo.RC_LINK_IDX_MAX_TX_POWER:
                case PacketInfo.RC_LINK_IDX_FF_ENABLE:
                case PacketInfo.RC_LINK_IDX_FF_COOLDOWN:
                case PacketInfo.RC_LINK_IDX_FF_STEP:
                case PacketInfo.RC_LINK_IDX_FF_RATIO_0:
                case PacketInfo.RC_LINK_IDX_FF_RATIO_1:
                case PacketInfo.RC_LINK_IDX_FF_RATIO_2:
                case PacketInfo.RC_LINK_IDX_FF_RATIO_3:
                case PacketInfo.RC_LINK_IDX_FF_RAISED:
                case PacketInfo.RC_LINK_IDX_FF_APPLIED:
                case PacketInfo.RC_LINK_IDX_FF_AVG_AMP:
                    return isProtocolAtLeast(PacketInfo.RC_PROTOCOL_MAJOR_REQUIRED, PacketInfo.RC_PROTOCOL_MINOR_MAX_TX_POWER);

                default:
                    return (index <= PacketInfo.RC_LINK_IDX_MAX);
            }
        }

        if (mStatus.fwRelease == Status.FW_RELEASE_4)
        {
            /* 상한은 일괄 읽기 응답에서 «관찰한» 값이다. 상수로 못박으면 초기 REL4
             * (infinite 가 없던 시기)에서 없는 옵션을 보내게 된다.
             * 아직 못 읽었으면 알려진 최소 구성으로 본다. */
            int observedMax = (mStatus.rel4LinkIndexMax > 0) //
                              ? mStatus.rel4LinkIndexMax //
                              : PacketInfo.RC_LINK_IDX_REL4_MIN;

            return (index <= observedMax) //
                   && (PacketInfo.getLegacyWriteOption(index) != PacketInfo.LEGACY_OPT_NONE);
        }

        return false;
    }

    /* Nop 다이얼로그를 띄우기 전에 링크 파라미터를 다시 읽는다.
     *
     * 쓸 수 있는 값의 목록은 «지금» 패킷 수로 정해진다. 맵이 바뀌면 패킷 수가 달라져
     * 목록 자체가 바뀌므로, 화면에 남아 있던 값으로 목록을 만들면 틀린 것을 보여 준다.
     *
     * 읽기를 보냈으면 true 다. 그때는 응답을 받은 자리에서 다이얼로그가 열린다.
     * 보내지 못했으면 false 이고, 부른 쪽이 지금 아는 값으로 바로 연다. */
    private boolean mNopDialogPending  = false;
    private int     mNopDialogRetryLeft = 0;

    private final Handler mLinkParamRefreshHandler = new Handler();

    /* 전송 중이면 조금 기다렸다 다시 시도한다.
     *
     * 감시 타이머가 2초마다 현재 Tx 파워를 읽으므로, 버튼을 누른 순간이 그 응답을
     * 기다리는 중일 확률이 낮지 않다. 예전에는 그때 그냥 포기하고 화면에 남아 있던
     * 값으로 목록을 만들었는데, 그러면 프로그램을 바꾼 직후에 «이전 프로그램의
     * 펄스폭 기준» 목록이 뜬다. */
    static private final int NOP_DIALOG_RETRY_MAX      = 20;  // 100ms 씩 최대 2초
    static private final int NOP_DIALOG_RETRY_DELAY_MS = 100;

    public boolean requestLinkParamsForNopDialog()
    {
        if (mStatus.connectionState != Status.CONNECTION_STATE_CONNECTED //
            || mStatus.fwRelease == Status.FW_RELEASE_UNKNOWN)
        {
            Log.d(TAG, "[LINK] 연결되지 않아 링크 파라미터를 읽을 수 없습니다. 아는 값으로 목록을 만듭니다.");
            return false;
        }

        Log.d(TAG, "[LINK] Nop 목록을 만들기 전에 링크 파라미터를 다시 읽습니다.");

        mNopDialogPending = true;
        mNopDialogRetryLeft = NOP_DIALOG_RETRY_MAX;

        tryNopDialogLinkRead();

        return true;
    }

    private void tryNopDialogLinkRead()
    {
        if (!mNopDialogPending)
        {
            return;
        }

        if (mStatus.transferState == Status.TRANSFER_STATE_IDLE)
        {
            longTimeIdleHandlerUpdate(true);
            sendLinkParamRead(PacketInfo.RC_IDX_ALL);
            return;
        }

        mNopDialogRetryLeft--;

        if (mNopDialogRetryLeft <= 0)
        {
            /* 끝내 못 읽었다. 목록을 못 띄우는 것보다는 아는 값으로라도 여는 편이 낫다.
             * 그 값이 오래됐을 수 있다는 것은 다이얼로그 머리말이 알려 준다. */
            Log.d(TAG, "[LINK] 링크 파라미터를 읽지 못했습니다. 아는 값으로 목록을 만듭니다.");

            notifyLinkParamsRefreshed();
            return;
        }

        mLinkParamRefreshHandler.postDelayed(this::tryNopDialogLinkRead, NOP_DIALOG_RETRY_DELAY_MS);
    }

    /* 맵이 바뀌었을 만한 일이 생겼을 때 링크 파라미터를 다시 읽는다.
     *
     * 프로그램을 바꾸면 맵이 바뀌고, 그에 따라 펄스폭 · 패킷 수 · 자극 전략이 달라진다.
     * 사운드처리기는 그 시점에 전원 안정 Nop 도 패킷 수별 기본값으로 다시 넣는다.
     * 그런데 앱은 30초짜리 감시 주기가 돌아올 때까지 그 사실을 몰랐다.
     * 그래서 Nop 목록을 이전 프로그램의 펄스폭 기준으로 만들어 버렸다.
     *
     * 맵 계산이 끝난 뒤라야 새 값이 나오므로 조금 뒤에 읽는다. */
    static private final int LINK_PARAM_REFRESH_DELAY_MS = 500;

    public void requestLinkParamRefresh(String reason)
    {
        if (mStatus.fwRelease < Status.FW_RELEASE_4)
        {
            return; // 링크 파라미터가 없는 세대다
        }

        Log.d(TAG, "[LINK] " + reason + " -> 링크 파라미터를 다시 읽습니다.");

        mLinkParamRefreshHandler.removeCallbacks(mLinkParamRefreshRunner);
        mLinkParamRefreshHandler.postDelayed(mLinkParamRefreshRunner, LINK_PARAM_REFRESH_DELAY_MS);
    }

    private final Runnable mLinkParamRefreshRunner = new Runnable()
    {
        @Override
        public void run()
        {
            if (mBluetoothGatt == null || mStatus.connectionState != Status.CONNECTION_STATE_CONNECTED)
            {
                return;
            }

            /* 연결 직후 세대 판별이 도는 중이면 끼어들지 않는다.
             * 판별이 끝나면 그 자체가 링크 파라미터를 다 읽어 오므로 따로 읽을 필요도 없다. */
            if (!isLinkInfoReadIdle())
            {
                return;
            }

            // 다른 패킷이 응답을 기다리는 중이면 조금 더 기다린다.
            if (mStatus.transferState != Status.TRANSFER_STATE_IDLE)
            {
                mLinkParamRefreshHandler.postDelayed(this, NOP_DIALOG_RETRY_DELAY_MS);
                return;
            }

            if (mOta != null && mOta.commState != Ota.COMM_STATE_IDLE)
            {
                return; // OTA 전송 중에는 끼어들지 않는다
            }

            sendLinkParamRead(PacketInfo.RC_IDX_ALL);
        }
    };

    /* 전체 읽기 응답을 다 반영한 자리에서 부른다. 기다리던 다이얼로그가 있으면 연다. */
    private void notifyLinkParamsRefreshed()
    {
        if (!mNopDialogPending)
        {
            return;
        }

        mNopDialogPending = false;

        Fragment fragment = getSupportFragmentManager().findFragmentById(mBinding.frame.getId());

        if (fragment instanceof RemoteControlFragment)
        {
            ((RemoteControlFragment) fragment).onLinkParamsRefreshedForNop();
        }
    }

    /* 전원 안정 Nop 을 패킷 수별 기본값으로 되돌린다. 인덱스 14 에 0 을 쓰면 된다.
     * 개수(인덱스 11)를 직접 쓰는 것과 달리 «어느 값이 맞는지» 를 사운드처리기가 정한다. */
    public void sendNopStandbyDefault()
    {
        sendLinkParam(PacketInfo.RC_LINK_IDX_NOP_ENABLE, PacketInfo.NOP_ENABLE_DEFAULT);
    }

    //
    // 설정 사이의 의존성 — 이 세대가 «알긴 아는데» 지금 설정으로는 무의미해지는 것들
    //
    // 사운드처리기 docs 의 «설정 의존성과 앱 UI 규칙» §6.1 을 그대로 옮겼다.
    // 펌웨어는 이런 조합을 «거부하지 않는다». 값은 정상으로 저장되고 응답도 성공으로
    // 돌아오는데 동작에 반영되지 않을 뿐이라, 앱이 막지 않으면 사용자는 알 수 없다.
    //
    // 백텔이 안 나가는 상태. 이때는 링크 판정 블록에 진입조차 못 해 링크 제어가 통째로 멈춘다.
    public boolean isLinkFrozen()
    {
        int frameNum = mStatusViewModel.getValueFrameNum();

        if (frameNum == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            return false; // 아직 모르면 막지 않는다
        }

        return (PacketInfo.LINK_FRAME_NUM_FROZEN <= frameNum);
    }

    // 링크 제어 모드가 «백텔 수신 기준» 인지.
    public boolean isBacktelCtrlMode()
    {
        return (mStatusViewModel.getValueLinkCtrlMode() == PacketInfo.LINK_CTRL_MODE_BACKTEL);
    }

    /* 재시도 코인이 «개수» 인가 «켜고 끄는 스위치» 인가.
     *
     * 인덱스 7 자체는 REL4 전 구간에 있다. 있느냐가 아니라 «어떤 뜻이냐» 의 문제라
     * isLinkParamSupported() 의 인덱스 게이트와 따로 둔다.
     *
     * 버전을 못 읽는 세대(REL4 · REL3)에서는 isProtocolAtLeast() 가 false 를 내므로
     * 옛 기기가 자동으로 4.3 이하 경로를 탄다. 따로 막을 필요가 없다. */
    /* Tx 파워 상한(인덱스 16)을 쓸 수 있는 기기인가.
     *
     * 인덱스 게이트(isLinkParamSupported)와 따로 두는 이유는 4.4 의 코인 개수와 같다 —
     * 화면을 숨길지 말지는 «인덱스가 있나» 가 아니라 «이 기능을 쓸 수 있나» 로 정해야 한다. */
    public boolean is45LinkFeatureSupported()
    {
        return isProtocolAtLeast(PacketInfo.RC_PROTOCOL_MAJOR_REQUIRED, //
                                 PacketInfo.RC_PROTOCOL_MINOR_MAX_TX_POWER);
    }

    /* 4.5 는 «인덱스 16 + 17~26» 하나다. 중간 상태(16 만 있는 4.5)는 없는 것으로 본다
     * (2026-09-01 은수님 - "기존 4.5를 하위호환하는건 없어도 되는거"). 그래서 게이트가 하나다. */
    public boolean isMaxTxPowerSupported()
    {
        return is45LinkFeatureSupported();
    }

    /* 상한을 개별 읽기로 한 번 가져온다.
     *
     * 전체 읽기(인덱스 0)에는 실리지 않으므로 이 경로가 유일한 채움 수단이다.
     * 사용자가 누른 것이 아니라 앱이 스스로 보내는 읽기라, 응답 토스트는 억제한다. */
    public void requestMaxTxPowerRead(String reason)
    {
        if (!isMaxTxPowerSupported())
        {
            return;
        }

        Log.d(TAG, "[LINK] Tx 파워 상한을 개별로 읽습니다. (" + reason + ")");

        mIsAutoMaxTxPowerRead = true;

        enqueueLinkRead(PacketInfo.RC_LINK_IDX_MAX_TX_POWER);
    }

    /* 연결 직후 읽는 4.5 설정 묶음. 상한(16)과 피드포워드 설정(17~23)이다.
     *
     * 관측값(24·25·26)은 여기 넣지 않는다 — 계속 변하는 값이라 한 번 읽어 두는 것이
     * 의미가 없고, 연결 초기화만 길어진다. FF 화면을 열 때 읽는다. */
    public void requestLinkSettings45(String reason)
    {
        if (!is45LinkFeatureSupported())
        {
            return;
        }

        Log.d(TAG, "[LINK] 4.5 설정을 개별로 읽습니다. (" + reason + ")");

        mIsAutoMaxTxPowerRead = true;

        enqueueLinkRead(PacketInfo.RC_LINK_IDX_MAX_TX_POWER, //
                        PacketInfo.RC_LINK_IDX_FF_ENABLE, //
                        PacketInfo.RC_LINK_IDX_FF_COOLDOWN, //
                        PacketInfo.RC_LINK_IDX_FF_STEP, //
                        PacketInfo.RC_LINK_IDX_FF_RATIO_0, //
                        PacketInfo.RC_LINK_IDX_FF_RATIO_1, //
                        PacketInfo.RC_LINK_IDX_FF_RATIO_2, //
                        PacketInfo.RC_LINK_IDX_FF_RATIO_3);
    }

    /* 피드포워드 관측값. FF 화면을 열 때만 읽는다. */
    public void requestFfObservation()
    {
        if (!is45LinkFeatureSupported())
        {
            return;
        }

        enqueueLinkRead(PacketInfo.RC_LINK_IDX_FF_RAISED, //
                        PacketInfo.RC_LINK_IDX_FF_APPLIED, //
                        PacketInfo.RC_LINK_IDX_FF_AVG_AMP);
    }

    public boolean isCoinCountSupported()
    {
        return isProtocolAtLeast(PacketInfo.RC_PROTOCOL_MAJOR_REQUIRED, //
                                 PacketInfo.RC_PROTOCOL_MINOR_COIN_COUNT);
    }

    /* 지금 무한 재시도 상태인가.
     *
     * 인덱스 7 의 255 와 인덱스 8 의 1 은 «같은 상태» 를 가리킨다. 그런데 인덱스 7 을
     * 먼저 보는 데는 이유가 있다 — 255 를 쓰면 인덱스 7 응답은 바로 반영되지만
     * 인덱스 8 은 다음 전체 읽기까지 낡은 값(0)으로 남는다. 인덱스 8 만 보면 방금 켠
     * 무한이 화면에 안 뜬다. */
    public boolean isCoinInfinite()
    {
        if (mStatusViewModel.getValueOneCoin() == PacketInfo.COIN_COUNT_INFINITE)
        {
            return true;
        }

        return (mStatusViewModel.getValueInfiniteCoin() == PacketInfo.LINK_FLAG_ENABLE) //
               && isLinkParamSupported(PacketInfo.RC_LINK_IDX_INFINITE_COIN);
    }

    /* 코인의 «개수 구간»(0 · 1~100)이 실제로 동작에 쓰이는가.
     *
     * 백텔 수신 기준 모드에는 코인 소모 분기가 아예 없어 개수가 무시된다.
     * 다만 무한(255)은 그 모드에서도 유효하다 — 끊김 판정 자체를 막는 것이라
     * 코인 소모 분기 밖에 있기 때문이다. 그래서 «설정할 수 있는가» 와 갈라 둔다. */
    public boolean isCoinCountEffective()
    {
        return !isLinkFrozen() && !isBacktelCtrlMode();
    }

    /* 지금 코인 재시도가 실제로 일어나는 상태인가.
     * 무한이거나, 개수가 1 이상이면서 그 개수가 쓰이는 모드일 때다. */
    public boolean isCoinRetryActive()
    {
        if (isCoinInfinite())
        {
            return true;
        }

        int coin = mStatusViewModel.getValueOneCoin();

        return (coin != PacketInfo.LINK_VALUE_UNKNOWN) //
               && (PacketInfo.COIN_COUNT_MIN <= coin) //
               && isCoinCountEffective();
    }

    /* 지금 이 설정을 «쓸 수 있는가». 세대가 아는가(isLinkParamSupported)에 더해
     * 지금 설정 조합에서 의미가 있는가까지 본다. 화면의 잠금과 전송 차단이 같은 답을 쓴다. */
    public boolean isLinkSettingUsable(int index)
    {
        if (!isLinkParamSupported(index))
        {
            return false;
        }

        switch (index)
        {
            case PacketInfo.RC_LINK_IDX_ONE_COIN:
                /* 4.4 부터는 백텔 수신 기준 모드에서도 «무한»(255)이 유효하다.
                 * 무시되는 것은 개수 구간(0 · 1~100)뿐이라 버튼 자체는 열어 둔다.
                 * 개수가 쓰이는지는 isCoinCountEffective() 로 따로 보고 화면에 알린다.
                 *
                 * 4.3 이하는 코인이 켜고 끄는 스위치뿐이라 이 모드에서 통째로 무의미하다. */
                if (isCoinCountSupported())
                {
                    return !isLinkFrozen();
                }

                return !isLinkFrozen() && !isBacktelCtrlMode();

            case PacketInfo.RC_LINK_IDX_TX_POWER_ACCEL:
                /* 가속 카운터는 백텔 수신 기준 제어 함수 안에만 있다. */
                return !isLinkFrozen() && isBacktelCtrlMode();

            case PacketInfo.RC_LINK_IDX_BACKTEL_PERIOD:
            case PacketInfo.RC_LINK_IDX_MIN_TX_POWER:
            case PacketInfo.RC_LINK_IDX_TX_POWER_STEP_UP:
            case PacketInfo.RC_LINK_IDX_CTRL_MODE:
            case PacketInfo.RC_LINK_IDX_POWER_STABLE_NOP:
            case PacketInfo.RC_LINK_IDX_NOP_ENABLE:
                return !isLinkFrozen();

            default:
                /* 강제 고정(6) · 매핑 하한(4) · infinite coin(8) 은 백텔이 멈춰도 그대로 둔다.
                 * 문서의 무력화 목록에 없고, 시험 시나리오가 쓰는 값들이기 때문이다. */
                return true;
        }
    }

    /* 지금 조합에서 알려야 할 경고. 없으면 빈 문자열이다.
     * 문서 §6.2 의 «켜 놓고 쓰되 알려 주는 것» 이다. 시험 중에는 의도된 상태라 막지 않는다. */
    /* 피드포워드를 켰을 때의 경고.
     *
     * 규격(설정 의존성과 앱 UI 규칙)이 조건과 심각도를 지정했다. 넷 중 셋은 «켜도 동작하지
     * 않는다» 이고, 마지막 하나는 «동작하지만 위험하다» 다. 켜지 않았으면 아무것도 알리지 않는다. */
    private void appendFeedForwardWarnings(StringBuilder sb)
    {
        if (mStatusViewModel.getValueFfEnable() != PacketInfo.LINK_FLAG_ENABLE)
        {
            return;
        }

        if (isBacktelCtrlMode())
        {
            append(sb, "피드포워드는 제어 모드 1 에서 동작하지 않습니다.");
        }

        if (isLinkFrozen())
        {
            append(sb, "백텔 미출력 구간 — 피드포워드가 동작하지 않습니다.");
        }

        int force = mStatusViewModel.getValueForceTxPowerLevel();

        if ((force != PacketInfo.LINK_VALUE_UNKNOWN && force != PacketInfo.FORCE_TX_PWR_RELEASE) //
            || isCoinInfinite())
        {
            append(sb, "강제 고정/무한 재시도 중 — 피드포워드가 올리지 않습니다.");
        }

        /* 올라가는 속도가 내려오는 속도를 넘으면 «일정한 소리에서도 상한까지» 간다.
         * 하강은 백텔 주기 300 msec 에 1스텝이라 초당 3.3 스텝뿐이다. */
        int rise = PacketInfo.makeFfRiseStepsPerSec(mStatusViewModel.getValueFfStep(), //
                                                    mStatusViewModel.getValueFfCooldown());

        if (rise != PacketInfo.LINK_VALUE_UNKNOWN && rise > PacketInfo.FF_FALL_STEPS_PER_SEC)
        {
            append(sb, "초당 상승 " + rise + "스텝이 하강(약 3.3스텝)보다 큽니다" //
                       + " — 일정한 소리에서도 상한까지 올라갈 수 있습니다.");
        }
    }

    /* 지금 «켜도 올라가지 않는» 조건을 문장으로 만든다.
     *
     * 피드포워드를 켜는 자리에서 보여 준다. 켜 놓고 아무 일도 안 일어나는 것이 가장 헷갈리는데,
     * 그 원인이 전부 «다른 설정» 이라 그 자리에서 말해 주지 않으면 찾기 어렵다. */
    public String makeFeedForwardBlockText()
    {
        StringBuilder sb = new StringBuilder();

        if (isBacktelCtrlMode())
        {
            append(sb, "제어 모드가 1 이라 동작하지 않습니다.");
        }

        if (isLinkFrozen())
        {
            append(sb, "백텔 미출력 구간이라 동작하지 않습니다.");
        }

        int force = mStatusViewModel.getValueForceTxPowerLevel();

        if ((force != PacketInfo.LINK_VALUE_UNKNOWN && force != PacketInfo.FORCE_TX_PWR_RELEASE) //
            || isCoinInfinite())
        {
            append(sb, "강제 고정/무한 재시도 중이라 올리지 않습니다.");
        }

        if (sb.length() == 0)
        {
            return "지금 설정에서는 정상 동작합니다. Normal 모드에서만 올립니다.";
        }

        return sb.toString();
    }

    /* 피드포워드 설정을 쓴다. 범위 밖이면 상한과 같은 경로로 en__OutOfDataRange 가 온다.
     * 쓰기 뒤 같은 인덱스를 다시 읽어 화면 값이 실제 값과 어긋나지 않게 한다. */
    public void sendFfParam(int index, int value)
    {
        if (!is45LinkFeatureSupported() || !PacketInfo.isFfLinkIndex(index))
        {
            Log.d(TAG, "[LINK] 피드포워드 설정을 지원하지 않는 기기입니다. 보내지 않습니다.");
            return;
        }

        Log.d(TAG, "[LINK] 피드포워드 설정 요청 : 인덱스 " + index + " = " + value);

        if (!sendLinkParam(index, value))
        {
            return;
        }

        enqueueLinkRead(index);
    }

    public String makeLinkWarningText()
    {
        StringBuilder sb = new StringBuilder();

        int force    = mStatusViewModel.getValueForceTxPowerLevel();
        int infinite = mStatusViewModel.getValueInfiniteCoin();
        int strategy = mStatusViewModel.getValueStimStrategy();
        int pulse    = mStatusViewModel.getValuePulseWidth();
        int mapping  = mStatusViewModel.getValueMappingTxPowerLevel();
        int minPower = mStatusViewModel.getValueMinTxPowerLevel();
        int nopEn    = mStatusViewModel.getValueNopEnable();
        int stepUp   = mStatusViewModel.getValueTxStepUp();

        if (isLinkFrozen())
        {
            append(sb, "백텔 미출력 — 링크 끊김을 감지하지 못합니다.");
        }

        appendFeedForwardWarnings(sb);

        if (force != PacketInfo.LINK_VALUE_UNKNOWN && force != PacketInfo.FORCE_TX_PWR_RELEASE)
        {
            if (isBacktelCtrlMode() && force != PacketInfo.FORCE_TX_PWR_SELECT_MAX)
            {
                append(sb, "강제 고정 중에는 끊김이 판정되지 않습니다.");
            }
            else
            {
                append(sb, "Tx 파워 강제 고정 중 — 백텔 판정이 파워에 반영되지 않습니다.");
            }
        }

        if (isCoinInfinite())
        {
            append(sb, "무한 재시도 중 — 링크 에러가 표시되지 않습니다.");
        }

        /* 4.4 부터 코인 재시도 때의 Tx 파워 상향이 인덱스 5(상승 스텝)를 따른다.
         * 무력화가 아니라 동반 효과라 막지 않고 알린다. 스텝이 작을 때는 소음이므로
         * 0.5V 이상일 때만, 그리고 코인이 실제로 쓰이는 상태일 때만 띄운다. */
        if (isCoinCountSupported() //
            && stepUp != PacketInfo.LINK_VALUE_UNKNOWN //
            && PacketInfo.COIN_STEP_UP_WARN_LEVEL <= stepUp //
            && isCoinRetryActive())
        {
            append(sb, String.format("코인 재시도 한 번에 Tx 파워가 %.2fV 오릅니다.", //
                                     (stepUp * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f));
        }

        if (strategy == PacketInfo.STIM_STRATEGY_NOFM //
            && pulse != PacketInfo.LINK_VALUE_UNKNOWN //
            && (pulse < PacketInfo.NOFM_PULSE_WIDTH_MIN || PacketInfo.NOFM_PULSE_WIDTH_MAX < pulse))
        {
            append(sb, "nOFm 3프레임 고정 전제를 벗어난 펄스폭입니다. (미계측 구간)");
        }

        if (strategy == PacketInfo.STIM_STRATEGY_MEDIUM)
        {
            append(sb, "medium 전략 — 자극 PCM 이 생성되지 않습니다.");
        }

        if (mapping != PacketInfo.LINK_VALUE_UNKNOWN //
            && minPower != PacketInfo.LINK_VALUE_UNKNOWN //
            && mapping < minPower)
        {
            append(sb, "매핑 하한이 상시 하한보다 낮습니다.");
        }

        if (nopEn == PacketInfo.NOP_ENABLE_MANUAL)
        {
            append(sb, "[S] 를 리모콘이 지정한 상태 — 맵이 바뀌면 기본값으로 돌아갑니다.");
        }

        return sb.toString();
    }

    private void append(StringBuilder sb, String line)
    {
        if (0 < sb.length())
        {
            sb.append("\n");
        }

        sb.append("· ").append(line);
    }

    public boolean isMapInitSupported()
    {
        return (mStatus.fwRelease >= Status.FW_RELEASE_4);
    }

    // 게이팅(묵음)은 세대를 가리지 않는다. 판별이 끝나 연결이 살아 있기만 하면 된다.
    public boolean isGatingSupported()
    {
        return (mStatus.fwRelease >= Status.FW_RELEASE_3);
    }

    // 읽어 온 0x59 프로토콜 버전이 기준 이상인지.
    public boolean isProtocolAtLeast(int major, int minor)
    {
        if (mStatus.rcProtocolMajor == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            return false;
        }

        if (mStatus.rcProtocolMajor != major)
        {
            return (mStatus.rcProtocolMajor > major);
        }

        return (mStatus.rcProtocolMinor >= minor);
    }

    //
    // 연결 직후 상태 읽기
    //
    // 연결 직후 세대 판별 및 상태 읽기
    //
    // 사람이 고르게 하지 않고 «되는 것» 을 찾을 때까지 위에서부터 물어본다.
    //
    //   1) 버전 조회 (옵션 255)   응답 O -> REL4+ . 이어서 링크 도메인 전체를 읽는다
    //                            응답 X -> 2 로
    //   2) 링크 일괄 읽기 (구 17)  응답 O -> REL4  . 값 10개를 바로 받는다
    //                            응답 X -> 3 으로
    //   3) 게이팅 읽기 (옵션 1)    응답 O -> REL3
    //                            응답 X -> UNKNOWN
    //
    // 판별과 상태 읽기가 같은 패킷으로 이뤄진다. 세대를 알아내려고 따로 왕복하지 않는다.
    //
    // sendPacket() 은 응답을 받기 전에는 다음 패킷을 버리므로 한 번에 보낼 수 없다.
    // 각 응답을 받은 자리에서 다음 단계를 이어 보낸다.
    //
    static private final int PROBE_STEP_NONE       = 0;
    static private final int PROBE_STEP_VERSION    = 1;
    static private final int PROBE_STEP_LINK_ALL   = 2;
    static private final int PROBE_STEP_LEGACY_ALL = 3;
    static private final int PROBE_STEP_GATING     = 4;

    private int mProbeStep = PROBE_STEP_NONE;

    /* 링크 파라미터 개별 읽기 큐.
     *
     * sendPacket() 은 transferState 가 BUSY 면 로그만 남기고 «패킷을 버린다»(:4230 부근).
     * 그래서 읽기를 연달아 늘어놓으면 첫 하나만 나가고 나머지가 사라진다. 인덱스 16 하나일
     * 때는 드러나지 않았는데, 4.5 후반의 17~26 이 붙으면서 연결 직후 읽을 것이 8개가 됐다.
     *
     * 그래서 큐에 넣어 두고 «응답이 온 자리에서 다음 하나» 를 보낸다.
     * 4.4 코인이 패킷 둘을 보내려고 만든 연쇄(mPendingOneCoinValue)의 일반화다.
     *
     * 거절이 와도 다음으로 넘어간다 — 하나에 막히면 뒤가 영영 안 읽힌다. */
    private final ArrayDeque<Integer> mLinkReadQueue = new ArrayDeque<>();

    /* 큐가 응답 유실로 멈추는 것을 막는 시한. 만료되면 큐를 비우고, 못 읽은 값은
     * LINK_VALUE_UNKNOWN 으로 남아 화면에 «-» 로 보인다. */
    static private final long LINK_READ_QUEUE_TIMEOUT_MS = 3500;

    private final Handler  mLinkReadQueueHandler = new Handler(Looper.getMainLooper());
    private final Runnable mLinkReadQueueTimeout = () ->
    {
        if (!mLinkReadQueue.isEmpty())
        {
            Log.d(TAG, "[LINK] 읽기 큐가 응답을 못 받아 남은 " + mLinkReadQueue.size() + "개를 버립니다.");
            mLinkReadQueue.clear();
        }
    };

    /** 개별 읽기를 큐에 넣는다. 지금 보낼 수 있으면 바로 하나 나간다. */
    private void enqueueLinkRead(int... indexes)
    {
        for (int index : indexes)
        {
            if (!mLinkReadQueue.contains(index))
            {
                mLinkReadQueue.add(index);
            }
        }

        pumpLinkReadQueue();
    }

    /** 큐에서 하나를 꺼내 보낸다. 응답·거절이 온 자리에서도 부른다. */
    private void pumpLinkReadQueue()
    {
        mLinkReadQueueHandler.removeCallbacks(mLinkReadQueueTimeout);

        if (mLinkReadQueue.isEmpty())
        {
            return;
        }

        if (mStatus.transferState != Status.TRANSFER_STATE_IDLE)
        {
            /* 아직 앞 패킷이 안 끝났다. 응답이 오면 다시 불리므로 여기서는 시한만 걸어 둔다. */
            mLinkReadQueueHandler.postDelayed(mLinkReadQueueTimeout, LINK_READ_QUEUE_TIMEOUT_MS);
            return;
        }

        int index = mLinkReadQueue.poll();

        Log.d(TAG, "[LINK] 큐에서 인덱스 " + index + " 를 읽습니다. (남은 " + mLinkReadQueue.size() + "개)");

        mLinkReadQueueHandler.postDelayed(mLinkReadQueueTimeout, LINK_READ_QUEUE_TIMEOUT_MS);

        sendLinkParamRead(index);
    }

    /* 앱이 스스로 보낸 상한 읽기인가. 사용자가 버튼으로 부른 것이 아니므로 응답 토스트를 억제한다.
     * 안 하면 연결할 때마다 «Tx 파워 상한 …» 토스트가 뜬다. */
    private boolean mIsAutoMaxTxPowerRead = false;

    /* 상한 쓰기를 보내고 응답을 기다리는 중인가.
     *
     * 에러 응답은 [0xF0, 실패 커맨드, 에러 타입 …] 이라 «어느 인덱스가 거부됐는지» 가 없다.
     * 그래서 «방금 상한을 썼다» 는 문맥을 앱이 들고 있어야 범위 밖 거부를 구분해 안내할 수 있다.
     * 응답이 오지 않아도 남지 않도록 타이머로 스스로 꺼진다. */
    private boolean mIsPendingMaxTxPowerWrite = false;

    // 쓰기 문맥을 들고 있는 시간. 기존 토스트 억제 타이머(3.5초)와 같은 값이다.
    static private final long MAX_TX_POWER_WRITE_CONTEXT_MS = 3500;

    public void resetLinkInfoRead()
    {
        mProbeStep = PROBE_STEP_NONE;
        mNopDialogPending = false;

        mLinkParamRefreshHandler.removeCallbacksAndMessages(null);
    }

    // 연결이 끝난 직후 호출한다.
    public void startLinkInfoRead()
    {
        mStatus.fwRelease = Status.FW_RELEASE_UNKNOWN;
        mStatus.rel4LinkIndexMax = 0;
        mStatus.rcProtocolMajor = PacketInfo.LINK_VALUE_UNKNOWN;
        mStatus.rcProtocolMinor = PacketInfo.LINK_VALUE_UNKNOWN;
        mStatus.rcOptionBitmap = null;

        publishFwRelease();

        Log.d(TAG, "[LINK] 연결 직후 세대 판별을 시작합니다. (버전 조회부터)");

        mProbeStep = PROBE_STEP_VERSION;
        sendProbePacket();
    }

    private void sendProbePacket()
    {
        byte[] packet;

        switch (mProbeStep)
        {
            case PROBE_STEP_VERSION:
                /* 버전 조회만 3바이트다. 인덱스가 없다.
                 * 규격서 예제가 "59 FF 00" 이므로 그대로 따른다. */
                packet = new byte[PacketInfo.RC_REQ_LEN_VERSION];
                packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
                packet[PacketInfo.RC_REQ_OFS_OPTION] = (byte) PacketInfo.RC_OPT_VERSION;
                packet[PacketInfo.RC_REQ_OFS_ACCESS] = (byte) PacketInfo.RC_ACCESS_READ;
                break;

            case PROBE_STEP_LINK_ALL:
                packet = makeRcReadPacket(PacketInfo.RC_OPT_LINK, PacketInfo.RC_IDX_ALL);
                break;

            case PROBE_STEP_LEGACY_ALL:
                packet = makeLegacyReadPacket(PacketInfo.LEGACY_OPT_READ_ALL);
                break;

            case PROBE_STEP_GATING:
                // 게이팅은 구형 포맷이라 [커맨드, 옵션] 두 바이트뿐이다.
                packet = new byte[PacketInfo.RC_REQ_LEN_MUTE_READ];
                packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
                packet[PacketInfo.RC_REQ_OFS_OPTION] = (byte) PacketInfo.RC_OPT_MUTE_READ;
                break;

            default:
                return;
        }

        sendPacket(packet);
    }

    // 지금 단계가 기다리는 옵션 번호.
    private int expectedProbeOption()
    {
        switch (mProbeStep)
        {
            case PROBE_STEP_VERSION:
                return PacketInfo.RC_OPT_VERSION;

            case PROBE_STEP_LINK_ALL:
                return PacketInfo.RC_OPT_LINK;

            case PROBE_STEP_LEGACY_ALL:
                return PacketInfo.LEGACY_OPT_READ_ALL;

            case PROBE_STEP_GATING:
                return PacketInfo.RC_OPT_MUTE_READ;

            default:
                return PacketInfo.LEGACY_OPT_NONE;
        }
    }

    // 응답을 받을 때마다 호출한다. 기다리던 옵션일 때만 다음 단계로 넘어간다.
    private void advanceLinkInfoRead(int option)
    {
        int expected = expectedProbeOption();

        if (expected == PacketInfo.LEGACY_OPT_NONE) // 판별 중이 아니다. (버튼으로 개별 조작한 경우)
        {
            return;
        }

        if (option != expected) // 기다리던 응답이 아니면 순서를 넘기지 않는다.
        {
            return;
        }

        switch (mProbeStep)
        {
            case PROBE_STEP_VERSION:
                /* 버전이 읽혔다. 다만 링크 도메인을 실제로 지원하는지는 비트맵으로 확인한다.
                 * 버전만 있고 링크가 없는 조합도 규격상 가능하기 때문이다. */
                setFwRelease(Status.FW_RELEASE_4_PLUS);

                if (!isLinkDomainSupported())
                {
                    Log.d(TAG, "[LINK] 버전은 읽혔지만 링크 도메인이 없습니다. 게이팅만 읽습니다.");

                    mProbeStep = PROBE_STEP_GATING;
                }
                else
                {
                    mProbeStep = PROBE_STEP_LINK_ALL;
                }
                break;

            case PROBE_STEP_LEGACY_ALL:
                // 버전은 못 읽었는데 구 옵션 17 이 응답했다. 그 세대다.
                /* 일괄 읽기 응답은 이 자리에 오기 «전» 에 해석된다.
                 * 그래서 여기서는 이미 rel4LinkIndexMax 가 잡혀 있다. */
                setFwRelease(Status.FW_RELEASE_4);
                applyRel4FixedValues();

                mProbeStep = PROBE_STEP_GATING;
                break;

            case PROBE_STEP_LINK_ALL:
                mProbeStep = PROBE_STEP_GATING;
                break;

            case PROBE_STEP_GATING:
                /* 게이팅까지 왔는데 아직 세대가 안 잡혔으면, 앞의 둘이 모두 거절된 것이다.
                 * 게이팅만 사는 세대다. */
                if (mStatus.fwRelease == Status.FW_RELEASE_UNKNOWN)
                {
                    setFwRelease(Status.FW_RELEASE_3);
                }

                mProbeStep = PROBE_STEP_NONE;

                Log.d(TAG, "[LINK] 세대 판별과 상태 읽기를 완료했습니다. -> " + fwReleaseName());
                UtilLog.instance.writeLog("펌웨어 세대 판별->" + fwReleaseName());

                /* 여기가 «4.5 로 판단되는 시점» 이다 (2026-09-01 은수님 지시).
                 *
                 * Tx 파워 상한(인덱스 16)은 20바이트 상한 때문에 전체 읽기에 실리지 않아
                 * 개별로 한 번 더 읽어야 화면에 값이 뜬다. 이 자리를 고른 이유는 둘이다.
                 *   - 버전이 PROBE_STEP_VERSION 에서 이미 확정돼 있다.
                 *   - 판별 상태 기계가 끝나 다음 요청과 겹치지 않는다.
                 * 4.5 미만이면 나가지 않는다. */
                requestLinkSettings45("세대 판별 완료");
                return;

            default:
                return;
        }

        sendProbePacket();
    }

    /* 판별 중 거절당했을 때 호출한다. 그 세대가 모르는 옵션이라는 뜻이므로 다음 후보로 내려간다.
     * 에러 응답도 판별의 정상적인 결과다. */
    public void onLinkInfoReadFailed(int option)
    {
        switch (mProbeStep)
        {
            case PROBE_STEP_VERSION:
                Log.d(TAG, "[LINK] 버전 조회가 거절되었습니다. 구 옵션 17 을 시도합니다.");
                UtilLog.instance.writeLog("0x59 버전 조회 실패 : REL4 이하로 판단");

                mProbeStep = PROBE_STEP_LEGACY_ALL;
                break;

            case PROBE_STEP_LEGACY_ALL:
                Log.d(TAG, "[LINK] 구 옵션 17 도 거절되었습니다. 게이팅만 확인합니다.");
                UtilLog.instance.writeLog("0x59 구 옵션 17 실패 : REL3 이하로 판단");

                mProbeStep = PROBE_STEP_GATING;
                break;

            case PROBE_STEP_LINK_ALL:
                /* 버전은 읽혔는데 링크 전체 읽기가 거절됐다. 세대는 REL4+ 가 맞지만
                 * 링크 값은 못 읽은 상태로 남는다. 게이팅으로 넘어간다. */
                Log.d(TAG, "[LINK] 링크 전체 읽기가 거절되었습니다. 게이팅으로 넘어갑니다.");

                mProbeStep = PROBE_STEP_GATING;
                break;

            case PROBE_STEP_GATING:
                Log.d(TAG, "[LINK] 게이팅 읽기까지 거절되었습니다. 세대를 판별하지 못했습니다.");
                UtilLog.instance.writeLog("펌웨어 세대 판별 실패");

                mProbeStep = PROBE_STEP_NONE;
                publishFwRelease();
                return;

            default:
                return;
        }

        sendProbePacket();
    }

    /* REL4 에는 링크 제어 모드와 상승 가속이 없다. 없다는 것은 곧 동작이 정해져 있다는
     * 뜻이다. 제어 모드는 전원 상태 기준 하나뿐이었고 가속은 쓰지 않았다.
     * 화면에 «-» 로 두면 못 읽은 것인지 없는 것인지 구분이 안 되므로 그 값으로 채운다.
     *
     * 다만 이미 읽힌 값이 있으면 건드리지 않는다. 같은 REL4 라도 막바지 펌웨어는
     * 일괄 읽기에 두 값을 실어 보내기 때문이다. */
    private void applyRel4FixedValues()
    {
        if (mStatusViewModel == null)
        {
            return;
        }

        if (mStatusViewModel.getValueLinkCtrlMode() == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            mStatusViewModel.setValueLinkCtrlMode(PacketInfo.LINK_CTRL_MODE_POWER_STATE);
        }

        if (mStatusViewModel.getValueTxPowerAccel() == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            mStatusViewModel.setValueTxPowerAccel(PacketInfo.LINK_FLAG_DISABLE);
        }

        /* infinite coin 이 없는 시기의 REL4 도 있다. 기능이 없다는 것은 곧 «꺼진 것»
         * 과 같으므로 그렇게 채운다. 이것을 미상으로 두면 one coin 값을 멀쩡히 읽고도
         * 화면이 «Coin -» 로 남는다. */
        if (!isLinkParamSupported(PacketInfo.RC_LINK_IDX_INFINITE_COIN) //
            && mStatusViewModel.getValueInfiniteCoin() == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            mStatusViewModel.setValueInfiniteCoin(PacketInfo.LINK_FLAG_DISABLE);
        }
    }

    private void setFwRelease(int fwRelease)
    {
        mStatus.fwRelease = fwRelease;

        Log.i(TAG, "[LINK] 펌웨어 세대 -> " + fwReleaseName());

        publishFwRelease();
    }

    // 화면이 세대에 맞춰 버튼과 배지를 갱신하도록 알린다.
    private void publishFwRelease()
    {
        if (mStatusViewModel != null)
        {
            mStatusViewModel.setValueFwRelease(mStatus.fwRelease);
        }
    }

    // 연결 직후 판별 중이 아닌지. 그때는 Toast 를 띄우지 않는다.
    private boolean isLinkInfoReadIdle()
    {
        return (mProbeStep == PROBE_STEP_NONE);
    }

    // 링크 도메인(옵션 0x20)을 쓸 수 있는 기기인지. 버전 응답의 비트맵으로 판단한다.
    public boolean isLinkDomainSupported()
    {
        if (mStatus.rcProtocolMajor < PacketInfo.RC_PROTOCOL_MAJOR_REQUIRED)
        {
            return false;
        }

        return PacketInfo.isOptionSupported(mStatus.rcOptionBitmap, PacketInfo.RC_OPT_LINK);
    }

    //
    // 모든 파일 접근 권한(안드로이드 11, API30 이상) 보유 여부
    //
    public boolean isAllFilesAccessGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) // 안드로이드 11 (API30) 이상
        {
            return Environment.isExternalStorageManager();
        }

        // API29 이하는 READ_EXTERNAL_STORAGE 로 충분하며, grantPermissions() 에서 이미 처리한다.
        return true;
    }

    //
    // 모든 파일 접근 권한 요청 결과
    //
    // 설정 화면은 결과 코드를 돌려주지 않으므로, 돌아온 뒤 권한 상태를 직접 다시 확인한다.
    //
    ActivityResultLauncher<Intent> mAllFilesAccessResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
    {
        if (isAllFilesAccessGranted())
        {
            Log.d(TAG, "모든 파일 접근 권한을 획득했습니다.");
            UtilLog.instance.writeLog("모든 파일 접근 권한 획득됨.");

            /* 폴더를 앱이 만들지는 않는다. 어느 폴더에서 읽을지는 Collect 에서 고르는 것이라,
             * 쓰지도 않을 슬롯 번호 폴더를 미리 만들어 두면 목록만 지저분해진다. */
            Toast.makeText(getApplicationContext(), //
                           "OTA 이미지 폴더\n" + Environment.getExternalStorageDirectory().getAbsolutePath() + Ota.BASE_FOLDER, //
                           Toast.LENGTH_LONG).show();
        }
        else
        {
            Log.d(TAG, "모든 파일 접근 권한을 거부당했습니다. OTA 이미지 읽기는 동작하지 않습니다.");
            UtilLog.instance.writeLog("모든 파일 접근 권한 거부됨.");

            Toast.makeText(getApplicationContext(), "모든 파일 접근 권한이 없어 OTA 이미지를 읽을 수 없습니다.", Toast.LENGTH_LONG).show();
        }
    });

    // 모든 파일 접근 권한 안내 다이얼로그
    //
    // onResume() 의 lastDialogDismiss() 가 mStatus.lastDialog 를 닫아버리므로,
    // 이 다이얼로그는 공용 참조를 쓰지 않고 별도 필드로 관리한다.
    private AlertDialog mAllFilesAccessDialog = null;

    // 안내는 앱 실행당 한 번만 한다. (거부해도 계속 되묻지 않도록)
    private boolean mIsAllFilesAccessAsked = false;

    //
    // 모든 파일 접근 권한 확인 및 안내
    //
    // 권한이 있으면 OTA 폴더만 준비하고, 없으면 설정 화면으로 안내한다.
    // 이 권한은 OTA 이미지 읽기에만 필요하므로 앱 진입을 막지 않는다.
    //
    public void checkAllFilesAccess()
    {
        if (isAllFilesAccessGranted())
        {
            return;
        }

        if (mIsAllFilesAccessAsked) // 이번 실행에서 이미 안내했다.
        {
            Log.d(TAG, "모든 파일 접근 권한이 없지만, 이번 실행에서 이미 안내했으므로 넘어갑니다.");
            return;
        }

        mIsAllFilesAccessAsked = true;

        Log.d(TAG, "모든 파일 접근 권한이 없습니다. 설정 화면으로 안내합니다.");

        if (mAllFilesAccessDialog != null && mAllFilesAccessDialog.isShowing())
        {
            return;
        }

        mAllFilesAccessDialog = new MaterialAlertDialogBuilder(MainActivity.this) //
                .setTitle("권한 필요") //
                .setMessage("OTA 이미지를 아래 폴더에서 읽으려면 '모든 파일 접근 허용' 권한이 필요합니다.\n\n" //
                            + Environment.getExternalStorageDirectory().getAbsolutePath() + Ota.BASE_FOLDER //
                            + "\n\n이어지는 설정 화면에서 허용해 주세요.\n허용하지 않아도 OTA 외의 기능은 사용할 수 있습니다.") //
                .setPositiveButton("설정으로 이동", (dialogInterface, i) ->
                {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()));
                    mAllFilesAccessResult.launch(intent);
                }) //
                .setNegativeButton("나중에", null) //
                .create();

        mAllFilesAccessDialog.setOnDismissListener(dialogInterface -> mAllFilesAccessDialog = null);

        mAllFilesAccessDialog.show();
    }

    //
    // 방송수신자 등록 관련
    //
    private void appRegisterReceiver()
    {
        registerReceiver(mBroadcastReceiver, makeIntentFilter());
        Log.d(TAG, "방송수신자를 등록했습니다.");
    }

    //
    // 방송수신자 해제 관련
    //
    private void appUnregisterReceiver()
    {
        try
        {
            unregisterReceiver(mBroadcastReceiver);
            Log.d(TAG, "방송수신자를 해제했습니다.");
        }
        catch (IllegalArgumentException exception)
        {
            Log.d(TAG, "방송수신자를 등록한적 없습니다. 그래서 해제할 필요가 없습니다.");
        }
    }

    //
    // 방송수신자 필터 생성
    //
    private IntentFilter makeIntentFilter()
    {
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); // 블루투스 자체에 대한 활성화/비활성화 이벤트
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); // 본딩 관련
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST); // 페어링 요청 관련

        return intentFilter;
    }

    //
    // 방송수신자 핸들러
    //
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            Log.d(TAG, "액션을 수신했습니다. -> " + action);

            // 블루투스 기능 꺼짐 감지
            switch (action)
            {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                    // 블루투스 기능을 꺼버리면, 앱도 같이 종료시킨다.
                    if (state == BluetoothAdapter.STATE_OFF)
                    {
                        finish();
                    }
                    break;

                // 블루투스 페어링 요청 감지
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    // Do not use... If use, app will be confused.
                    // But I will use here ㅋㅋㅋ.
                {
                    Log.d(TAG, "페어링 키를 자동으로 입력합니다.");

                    new Handler(Looper.getMainLooper()).post(() ->
                    {
                        mBinding.pairingKeyValue.setText(mStatus.connectedDevice.pairingKey);
                        mBinding.pairingKeyLayout.setVisibility(View.VISIBLE);

                        {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            {
                                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                notificationManager.createNotificationChannel(new NotificationChannel("TODOC_ALARM", "PAIRING_KEY", NotificationManager.IMPORTANCE_HIGH));
                            }

                            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "TODOC_ALARM");
                            builder.setPriority(NotificationCompat.PRIORITY_HIGH).setSmallIcon(R.drawable.ic_notification_small_24).setContentTitle("안내").setContentText("등록된 페어링 키는 '" + mStatus.connectedDevice.pairingKey + "' 입니다.").setDefaults(Notification.DEFAULT_VIBRATE);

                            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.notify(2, builder.build());
                        }
                    });
                }
                break;

                // 블루투스 본딩 상태 변경 감지
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    int bondState = device.getBondState();

                    if (bondState == BluetoothDevice.BOND_BONDED)
                    {
                        Log.d(TAG, "본딩되었습니다. -> {" + device.getAddress() + "}");

                        mBinding.pairingKeyLayout.setVisibility(View.GONE);

                        // 본딩이 성공한 후에는 의도적으로 연결을 해제하여, 다시 처음부터 연결을 진행하도록 한다.
                        if (mBluetoothGatt != null)
                        {
                            Log.d(TAG, "연결중이므로 연결을 강제 해제하겠습니다.");
                            mBluetoothGatt.disconnect();
                        }
                        else
                        {
                            Log.d(TAG, "왜 연결중이 아닐까요?");
                        }
                    }
                    else if (bondState == BluetoothDevice.BOND_BONDING)
                    {
                        Log.d(TAG, "본딩 중입니다. -> {" + device.getAddress() + "}");

                        if (mLockScreen.isEnabled())
                        {
                            mStatus.lockScreenState = Status.LOCK_SCREEN_STATE_TEMPORARY_UNLOCK;
                        }

                        // CCCD 쓰기 과정에서 보안 연결이 유효하지 않으면 다시 본딩을 시도한다.
                        // 그 때 이 곳에서 본딩 콜백을 수신하게 되므로, 이 경우에는 CCCD 시간초과 핸들러를 제거한다.
                        mCCCDHandler.removeCallbacks(mCCCDRunner);
                    }
                    else if (bondState == BluetoothDevice.BOND_NONE)
                    {
                        Log.d(TAG, "본딩이 실패했습니다. -> {" + device.getAddress() + "} -> This means that bonding was might failed.");

                        mBinding.pairingKeyLayout.setVisibility(View.GONE);

                        if (mBluetoothGatt != null)
                        {
                            mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                            mBluetoothGatt.disconnect();
                        }

                        lastDialogDismiss();

                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                        {
                            // 약 1초 뒤에 다이얼로그를 출력하기 전에, 아직도 잠금화면 상태가 임시 해제 상태면, 다시 잠금 상태로 되돌린다.
                            if (mStatus.lockScreenState == Status.LOCK_SCREEN_STATE_TEMPORARY_UNLOCK)
                            {
                                Log.d(TAG, "임시 잠금화면 해제를 취소하고, 다시 잠금 상태로 변경합니다.");
                                mStatus.lockScreenState = Status.LOCK_SCREEN_STATE_LOCK;
                            }

                            lastDialogDismiss();

                            mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this).setTitle("안내").setMessage("페어링을 실패했습니다.").setNegativeButton("재연결", (dialogInterface, i) ->
                            {
                                longTimeIdleHandlerUpdate(true);

                                if (mStatus.activityRunningState == Status.ACTIVITY_RUNNING_STATE_FOREGROUND)
                                {
                                    if (getSupportFragmentManager().findFragmentById(R.id.frame) instanceof RemoteControlFragment)
                                    {
                                        scanLe(true);
                                    }
                                }
                            }).setPositiveButton("확인", (dialogInterface, i) ->
                            {
                                longTimeIdleHandlerUpdate(true);
                            }).setCancelable(false).create();

                            mStatus.lastDialog.show();
                        }, 1000);
                    }
                    break;
            }
        } // onReceive
    };

    //
    // 백버튼 콜백 리스너
    //
    @Override
    public void onBackPressed()
    {
        // 장시간 미사용 핸들러 업데이트
        longTimeIdleHandlerUpdate(true);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);

        // 리모컨 화면
        if (fragment instanceof RemoteControlFragment || mBinding.lockScreen.getVisibility() == View.VISIBLE)
        {
            lastDialogDismiss();

            mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                    //.setTitle("주의")
                    .setMessage("앱을 종료하시겠습니까?").setPositiveButton("종료", (dialogInterface, i) -> finish()).setNegativeButton("취소", null).setCancelable(false).create();
            mStatus.lastDialog.show();
        }
        // 설정 화면
        else if (fragment instanceof SettingsFragment)
        {
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new RemoteControlFragment()).commitAllowingStateLoss();
             */
            replaceFragment(Status.TypeOfFragment.REMOTE_CONTROL);
        }
        // 사용자 화면
        else if (fragment instanceof UserFragment)
        {
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
             */
            replaceFragment(Status.TypeOfFragment.MENU);
        }
        // 사용자 추가 화면
        else if (fragment instanceof AddUserFragment)
        {
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new UserFragment()).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.USER_LIST);
        }
        // 사용자 수정 화면
        else if (fragment instanceof EditUserFragment)
        {
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new UserFragment()).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.USER_LIST);
        }
        // 사운드처리기 화면
        else if (fragment instanceof DeviceFragment)
        {
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.MENU);
        }
        // 사운드처리기 추가 화면
        else if (fragment instanceof AddDeviceFragment)
        {
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new DeviceFragment()).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.DEVICE_LIST);
        }
        // 사운드처리기 수정 화면
        else if (fragment instanceof EditDeviceFragment)
        {
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new DeviceFragment()).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.DEVICE_LIST);
        }
        // 사용설명서 화면
        else if (fragment instanceof ManualFragment)
        {
            Bundle bundle = ((ManualFragment) fragment).getArguments();
            if (bundle != null && bundle.getBoolean(ManualFragment.ARG_FIRST_SCREEN))
            {
                /*
                getSupportFragmentManager().beginTransaction().replace(R.id.frame, new RemoteControlFragment()).commitAllowingStateLoss();
                */
                replaceFragment(Status.TypeOfFragment.REMOTE_CONTROL);
            }
            else
            {
                /*
                getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
                */
                replaceFragment(Status.TypeOfFragment.MENU);
            }
        }
        // 시스템로그 화면
        else if (fragment instanceof LogFragment)
        {
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new AddUserFragment()).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.USER_ADD);
        }
    }

    //
    // 블루투스 활성화 요청 결과
    //
    ActivityResultLauncher<Intent> mBluetoothEnableResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
    {
        if (result.getResultCode() == Activity.RESULT_OK)
        {
            Log.d(TAG, "블루투스 활성화를 허용했습니다.");
            UtilLog.instance.writeLog("블루투스 활성화됨.");
            initMainActivity();
        }
        else
        {
            Log.d(TAG, "블루투스 활성화를 거부당했습니다.");
            UtilLog.instance.writeLog("블루투스 활성화 요청 거부.");
            finish();
        }
    });

    //
    // 블루투스 활성화 요청
    //
    public boolean enableBluetooth()
    {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled())
        {
            UtilLog.instance.writeLog("블루투스 비활성화 상태 -> 활성화 요청.");
            mBluetoothEnableResult.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return false;
        }

        return true;
    }

    //
    // 블루투스 관련 객체 획득
    //
    public void initBluetooth()
    {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        Log.d(TAG, "블루투스 매니저, 어댑터, 스캐너 객체를 획득했습니다.");
    }

    //
    // 스캔 핸들러 관련
    //
    Handler  mScanHandler = new Handler();
    Runnable mScanRunner  = () ->
    {
        Log.v(TAG, "BLE 스캔 시간이 초과되었습니다.");
        scanLe(false); // 스캔 정지
    }; // scanRunner

    //
    // Start/stop BLE scan.
    //
    public void scanLeWithDelay(boolean enable, int delay)
    {
        if (enable) // 스캔 시작
        {
            if (mStatus.scanState == Status.SCAN_STATE_STOPPED)
            {
                mStatus.scanState = Status.SCAN_STATE_STARTED;

                Log.v(TAG, "BLE 스캔을 시작합니다.");
                mScanHandler.postDelayed(mScanRunner, 20000); // 10초
                mBluetoothLeScanner.startScan(mScanCallback);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() ->
            {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);
                if (fragment instanceof RemoteControlFragment)
                {
                    mBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlFindLayout.setVisibility(View.VISIBLE);
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlSearchingAnimator.startRippleAnimation();
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlConnectionTitle.setText("외부기를 머리에 부착한 상태에서\n장치 연결을 진행하세요.");
                }
            }, delay);

        }
        else // 스캔 정지
        {
            if (mStatus.scanState == Status.SCAN_STATE_STARTED)
            {
                Log.v(TAG, "BLE 스캔을 종료합니다.");
                mBluetoothLeScanner.stopScan(mScanCallback);
                mScanHandler.removeCallbacks(mScanRunner);

                mStatus.scanState = Status.SCAN_STATE_STOPPED;
            }

            new Handler(Looper.getMainLooper()).postDelayed(() ->
            {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);
                if (fragment instanceof RemoteControlFragment && mStatus.connectionState == Status.CONNECTION_STATE_DISCONNECTED)
                {
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlSearchingAnimator.stopRippleAnimation();
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlFindLayout.setVisibility(View.VISIBLE);
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlConnectionTitle.setText("상단의 검색 버튼을 눌러\n검색을 다시 시작하세요.");
                    mBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(true);
                }
            }, delay);
        }
    }

    public void scanLe(boolean enable)
    {
        scanLeWithDelay(enable, 500);
    }

    //
    // 스캔 콜백 리스너
    //
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

            EntityUser         defaultUser  = UtilUser.instance.getDefaultUser();
            List<EntityDevice> devices      = UtilDevice.instance.getDevices();
            EntityDevice       targetDevice = null;

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

            Log.v(TAG, "BLE 스캔 결과 : 이름 = " + name + ", 스캔 응답 데이터 = " + serviceString);

            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);

            if (getSupportFragmentManager().findFragmentById(R.id.frame) instanceof RemoteControlFragment)
            {
                boolean isFound = false;

                for (EntityDevice device : devices)
                {
                    String targetString = defaultUser.ear + "_" + defaultUser.name.substring(0, defaultUser.name.length() - 2) + "_" + device.serialNumber;

                    if (targetString.equals(serviceString))
                    {
                        Log.d(TAG, "타겟 장치 발견 : " + targetString);
                        isFound = true;
                        targetDevice = device;
                        break;
                    }
                }

                if (isFound)
                {
                    // 모든 조건에 부합하므로, 검색된 장치와 연결한다. 단, 현재 BLE 연결 상태가 연결해제 상태여야 한다.
                    if (mStatus.connectionState == Status.CONNECTION_STATE_DISCONNECTED)
                    {
                        mStatus.connectionState = Status.CONNECTION_STATE_CONNECTING; // 연결 중 상태로 변경
                        scanLe(false); // 스캔 정지

                        // 연결을 시도하려는 사용자와 사운드처리기 정보를 저장.
                        mStatus.connectedUser = defaultUser;
                        mStatus.connectedDevice = targetDevice;

                        mBluetoothDevice = result.getDevice();
                        mBluetoothGatt = mBluetoothDevice.connectGatt(getApplicationContext(), false, mGattCallback);

                        if (mBluetoothGatt == null)
                        {
                            Log.d(TAG, "BLE 연결 시도가 실패했습니다. 검색을 다시 시작합니다.");

                            mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTED;
                            scanLe(true);
                        }
                    }
                }
            }
        } // onScanResult
    }; // scanCallback

    //
    // 링크 감시 타이머 (현재 Tx 파워 + 기기 상태)
    //
    // 두 가지를 주기적으로 읽어야 하는데, 각자 타이머를 두면 반드시 충돌한다.
    // sendPacket() 은 앞 패킷의 응답을 받기 전에는 다음 패킷을 조용히 버리기 때문이다.
    //
    // 그래서 타이머를 하나만 두고 한 주기에 패킷을 하나만 보낸다.
    //   매 주기(2초)     : 현재 Tx 파워 읽기 (0x59. 포맷은 세대가 정한다)
    //   15주기마다(30초) : 기기 상태 정보 읽기 (0x43, 배터리 · 볼륨 · LED 등)
    //   15주기마다(30초) : 링크 파라미터 전체 읽기 (Nop · 패킷 수 · 자극 전략 등)
    //
    // 뒤의 둘은 같은 30초 주기지만 8주기 어긋나게 두어 같은 주기에 겹치지 않는다.
    // 한 주기에 한 발이므로 셋이 부딪칠 수가 없다.
    //
    // 링크 파라미터를 주기적으로 다시 읽는 이유는, 맵이 바뀌면 사운드처리기가 패킷 수와
    // 전원 안정 Nop 을 스스로 새로 정하기 때문이다. 연결할 때 한 번만 읽으면 그 변화를 놓친다.
    //
    private int mLinkMonitorTick = 0;

    public final Handler mLinkMonitorHandler = new Handler();

    public final Runnable mLinkMonitorRunner = new Runnable()
    {
        @Override
        public void run()
        {
            // 어느 경로로 빠져나가든 감시가 끊기지 않도록 다음 주기를 먼저 예약한다.
            mLinkMonitorHandler.postDelayed(this, LINK_MONITOR_PERIOD_IN_MS);

            if (mBluetoothGatt == null || mStatus.connectionState != Status.CONNECTION_STATE_CONNECTED)
            {
                return;
            }

            /* 세대 판별이 끝나기 전에는 끼어들지 않는다.
             * 판별용 패킷과 겹치면 sendPacket() 이 한쪽을 버려 판별이 멈춘다. */
            if (mStatus.fwRelease == Status.FW_RELEASE_UNKNOWN)
            {
                return;
            }

            /* OTA 전송 중에는 쉰다. 이 패킷이 끼어들면 OTA 쪽 sendPacket() 이 버려져
             * 전송이 멈춘다. 전송이 끝나면 다음 주기부터 저절로 재개된다. */
            if (mOta != null && mOta.commState != Ota.COMM_STATE_IDLE)
            {
                return;
            }

            // 다른 패킷이 응답을 기다리는 중이면 이번 주기는 건너뛴다.
            if (mStatus.transferState != Status.TRANSFER_STATE_IDLE)
            {
                return;
            }

            mLinkMonitorTick++;

            if (mLinkMonitorTick >= LINK_MONITOR_STATUS_TICK_COUNT)
            {
                mLinkMonitorTick = 0;

                Log.v(TAG, "[LINK] 감시 : 기기 상태 정보 읽기");
                sendPacket(packetMaker(PacketInfo.HEADER_SOUND_PROCESSOR_STATUS, null, 1));
            }
            else if (mLinkMonitorTick == LINK_MONITOR_PARAM_TICK && Status.FW_RELEASE_4 <= mStatus.fwRelease)
            {
                /* 맵이 바뀌면 패킷 수와 전원 안정 Nop 이 사운드처리기 쪽에서 새로 정해진다.
                 * 전체 읽기 한 번이면 그 값들이 모두 따라온다. */
                Log.v(TAG, "[LINK] 감시 : 링크 파라미터 전체 읽기");
                sendLinkParamRead(PacketInfo.RC_IDX_ALL);
            }
            else
            {
                /* REL3 은 링크 파라미터가 없다. 읽을 것이 없으므로 이번 주기는 쉰다.
                 * 기기 상태 정보(30초)는 세대와 무관하게 그대로 돈다. */
                if (mStatus.fwRelease < Status.FW_RELEASE_4)
                {
                    return;
                }

                Log.v(TAG, "[LINK] 감시 : 현재 Tx 파워 읽기");
                sendLinkParamRead(PacketInfo.RC_LINK_IDX_CUR_TX_POWER);
            }
        }
    };

    //
    // 코인 모드 설정 (one coin + infinite coin 두 값의 조합)
    //
    // 화면에서는 한 번에 고르지만 패킷은 둘로 나뉜다. sendPacket() 이 응답 전에는 다음
    // 패킷을 버리므로, infinite(옵션 19)를 먼저 보내고 그 응답을 받은 자리에서
    // one coin(옵션 16)을 이어 보낸다.
    //
    private int mPendingOneCoinValue = PacketInfo.LINK_VALUE_UNKNOWN;

    /* 상한 쓰기가 범위 밖으로 거부됐을 때의 안내 문구.
     *
     * 바닥은 (상시 하한, 매핑 하한) 중 큰 값 + 1 이므로, 어느 쪽이 밀어 올렸는지를 밝혀야
     * 사용자가 무엇을 내려야 하는지 안다. 두 하한을 아직 못 읽었으면 범위만 적는다. */
    private String makeMaxTxPowerRangeText()
    {
        int minLevel     = mStatusViewModel.getValueMinTxPowerLevel();
        int mappingLevel = mStatusViewModel.getValueMappingTxPowerLevel();
        int floor        = PacketInfo.makeMaxTxPowerFloor(minLevel, mappingLevel);

        if (floor == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            return "PMIC 상한이 범위 밖입니다. 하한 설정보다 커야 합니다.";
        }

        String who = (mappingLevel > minLevel) ? "매핑 하한" : "상시 하한";

        return "PMIC 상한은 " + (floor + 1) + " 이상이어야 합니다." //
               + " (" + who + " " + floor + " 때문)" //
               + "  지금 범위 : " + (floor + 1) + " ~ " + PacketInfo.MAX_TX_PWR_SELECT_MAX;
    }

    /* Tx 파워 상한을 설정한다 (프로토콜 4.5 이상).
     *
     * 쓰기 뒤에 같은 인덱스를 다시 읽는다. 쓰기 성공 응답이 값을 되돌려 주는지 확인되지 않았고,
     * 되돌려 주든 아니든 이 한 번이면 화면 값이 실제 값과 맞는다.
     *
     * 범위 밖이면 사운드처리기가 en__OutOfDataRange(에러 타입 2)로 거부하는데, 에러 응답에
     * 인덱스가 없어 «무엇이 거부됐는지» 를 앱이 문맥으로 알아야 한다. 그 문맥이 아래 플래그다. */
    public void sendMaxTxPower(int level)
    {
        /* 버튼이 4.5 미만에서 숨겨지므로 여기까지 오지 않지만, 호출부가 늘어도 안전하도록 막는다.
         * 없는 인덱스를 보내면 사운드처리기가 거절하고 사용자에게는 «유효하지 않은 명령» 이 뜬다. */
        if (!isMaxTxPowerSupported())
        {
            Log.d(TAG, "[LINK] 이 기기는 Tx 파워 상한을 지원하지 않습니다. 보내지 않습니다.");
            return;
        }

        Log.d(TAG, "[LINK] Tx 파워 상한 설정 요청 : " + level);

        mIsPendingMaxTxPowerWrite = true;

        // 응답이 영영 안 와도 문맥이 남지 않도록 스스로 내려온다.
        new Handler(Looper.getMainLooper()).postDelayed(() -> mIsPendingMaxTxPowerWrite = false, //
                                                        MAX_TX_POWER_WRITE_CONTEXT_MS);

        if (!sendLinkParam(PacketInfo.RC_LINK_IDX_MAX_TX_POWER, level))
        {
            mIsPendingMaxTxPowerWrite = false;
            return;
        }

        requestMaxTxPowerRead("상한 설정");
    }

    /* 재시도 코인을 «개수» 로 설정한다 (프로토콜 4.4 이상).
     *
     * 4.3 까지는 infinite(인덱스 8)를 먼저 보내고 그 응답 자리에서 one coin(인덱스 7)을
     * 이어 보내야 했다. sendPacket() 이 응답 전 다음 패킷을 버리기 때문이다.
     * 4.4 부터는 인덱스 7 하나가 0 · 1~100 · 255 를 모두 받으므로 그 대기 기계가 필요 없다.
     *
     * 인덱스 8 은 같은 내부 플래그라 따라 바뀐다. 굳이 보내지 않는다.
     * 다만 앱이 아는 인덱스 8 값은 낡은 채로 남으므로 전체 읽기를 한 번 예약한다. */
    public void sendCoinCount(int coinCount)
    {
        Log.d(TAG, "[LINK] 재시도 코인 설정 요청 : " + coinCount //
                   + ((coinCount == PacketInfo.COIN_COUNT_INFINITE) ? " (무한)" : ""));

        if (!sendLinkParam(PacketInfo.RC_LINK_IDX_ONE_COIN, coinCount))
        {
            return;
        }

        requestLinkParamRefresh("코인 설정");
    }

    public void sendCoinMode(int coinMode)
    {
        int infinite = (coinMode == PacketInfo.COIN_MODE_INFINITE) //
                       ? PacketInfo.LINK_FLAG_ENABLE //
                       : PacketInfo.LINK_FLAG_DISABLE;

        /* infinite 를 끄고 나면 one coin 값이 그대로 드러나므로 모드마다 확정해 둔다.
         * 무한을 골랐을 때 one coin 을 켜 두면 무한 해제 시 자연스럽게 사용 으로 떨어진다. */
        mPendingOneCoinValue = (coinMode == PacketInfo.COIN_MODE_DISABLE) //
                               ? PacketInfo.LINK_FLAG_DISABLE //
                               : PacketInfo.LINK_FLAG_ENABLE;

        Log.d(TAG, "[LINK] 코인 모드 설정 요청 : 모드=" + coinMode + ", infinite=" + infinite + ", one coin=" + mPendingOneCoinValue);

        /* infinite coin 이 없는 세대에서는 그 패킷을 건너뛰고 one coin 만 보낸다.
         *
         * 예전에는 무조건 infinite 부터 보냈는데, 그것을 모르는 펌웨어에서는 거절당하고
         * 그 응답을 못 받으니 뒤이어 나가야 할 one coin 도 영영 안 나갔다.
         * one coin 은 REL4 전 구간에 있는 기능이라 반드시 설정할 수 있어야 한다. */
        if (!isLinkParamSupported(PacketInfo.RC_LINK_IDX_INFINITE_COIN))
        {
            int oneCoin = mPendingOneCoinValue;

            mPendingOneCoinValue = PacketInfo.LINK_VALUE_UNKNOWN;

            Log.d(TAG, "[LINK] 이 세대에는 infinite coin 이 없습니다. one coin 만 보냅니다.");

            sendLinkParam(PacketInfo.RC_LINK_IDX_ONE_COIN, oneCoin);
            return;
        }

        /* 보내지 못했으면 대기 중인 one coin 도 취소한다.
         * 그대로 두면 다음에 엉뚱한 응답을 받았을 때 튀어나간다. */
        if (!sendLinkParam(PacketInfo.RC_LINK_IDX_INFINITE_COIN, infinite))
        {
            mPendingOneCoinValue = PacketInfo.LINK_VALUE_UNKNOWN;
        }
    }

    // infinite 응답을 받은 자리에서 호출한다. 대기 중인 값이 없으면 아무것도 하지 않는다.
    private void sendPendingOneCoinPacket()
    {
        if (mPendingOneCoinValue == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            return;
        }

        int value = mPendingOneCoinValue;

        mPendingOneCoinValue = PacketInfo.LINK_VALUE_UNKNOWN;

        sendLinkParam(PacketInfo.RC_LINK_IDX_ONE_COIN, value);
    }

    //
    // 옵션 255 — 프로토콜 버전 응답
    //
    // [5] major, [6] minor, [7..14] 지원 옵션 비트맵 8바이트
    //
    private void handleRcVersionResponse(byte[] responsePacket, int valueLen)
    {
        if (valueLen < PacketInfo.RC_VERSION_VALUE_LEN)
        {
            Log.d(TAG, "[LINK] 버전 응답의 값이 짧습니다 : L = " + valueLen);
            return;
        }

        mStatus.rcProtocolMajor = responsePacket[PacketInfo.RC_VERSION_OFS_MAJOR] & 0xFF;
        mStatus.rcProtocolMinor = responsePacket[PacketInfo.RC_VERSION_OFS_MINOR] & 0xFF;

        byte[] bitmap = new byte[PacketInfo.RC_VERSION_BITMAP_BYTES];

        System.arraycopy(responsePacket, PacketInfo.RC_VERSION_OFS_BITMAP, bitmap, 0, PacketInfo.RC_VERSION_BITMAP_BYTES);

        mStatus.rcOptionBitmap = bitmap;

        StringBuilder sb = new StringBuilder();

        for (byte b : bitmap)
        {
            sb.append(String.format("%02X ", (b & 0xFF)));
        }

        Log.i(TAG, "[LINK] 0x59 프로토콜 버전 " + mStatus.rcProtocolMajor + "." + mStatus.rcProtocolMinor //
        + ", 지원 옵션 비트맵 " + sb.toString().trim() //
        + " (링크=" + PacketInfo.isOptionSupported(bitmap, PacketInfo.RC_OPT_LINK) //
        + ", 자극=" + PacketInfo.isOptionSupported(bitmap, PacketInfo.RC_OPT_STIM) //
        + ", 배터리=" + PacketInfo.isOptionSupported(bitmap, PacketInfo.RC_OPT_BATTERY) + ")");

        UtilLog.instance.writeLog("패킷 수신 : 0x59 프로토콜 버전->" + mStatus.rcProtocolMajor + "." + mStatus.rcProtocolMinor);

        mStatusViewModel.setValueRcProtocolMajor(mStatus.rcProtocolMajor);
        mStatusViewModel.setValueRcProtocolMinor(mStatus.rcProtocolMinor);
    }

    //
    // 구 프로토콜(릴리즈 3) 응답 처리
    //
    //   일괄 읽기 : [0x59, 17, 값 10개]   개별 : [0x59, 옵션, 값]
    //
    // 일괄 읽기의 값 순서는 릴리즈 4 링크 인덱스 1~10 과 같으므로 파싱을 공유한다.
    //
    private void handleLegacyResponse(byte[] responsePacket, int packetSize, int option)
    {
        if (option == PacketInfo.LEGACY_OPT_READ_ALL)
        {
            /* 일괄 읽기 응답의 길이는 펌웨어 시기마다 다르다. one coin 까지면 9,
             * infinite 까지면 10, 제어 모드와 가속까지면 12바이트다. 값은 뒤에만
             * 늘어났으므로 받은 만큼만 읽는다. */
            int count = packetSize - PacketInfo.LEGACY_ALL_OFS_VALUE;

            if (count < 1)
            {
                Log.d(TAG, "[LEGACY] 일괄 읽기 응답에 값이 없습니다 : 사이즈 = " + packetSize);
                return;
            }

            if (PacketInfo.LEGACY_ALL_MAX_COUNT < count)
            {
                Log.d(TAG, "[LEGACY] 일괄 읽기에 이 앱이 모르는 값이 " + (count - PacketInfo.LEGACY_ALL_MAX_COUNT) + "개 더 있습니다. 무시합니다.");

                count = PacketInfo.LEGACY_ALL_MAX_COUNT;
            }

            for (int i = 0; i < count; i++)
            {
                applyLinkParam(i + 1, responsePacket[PacketInfo.LEGACY_ALL_OFS_VALUE + i] & 0xFF);
            }

            /* 받은 값 개수가 곧 그 기기가 아는 인덱스 범위다. 화면의 버튼 잠금과
             * 전송 차단이 이 값을 쓴다. 잘라 낸 개수가 아니라 실제로 받은 개수를 쓴다. */
            mStatus.rel4LinkIndexMax = packetSize - PacketInfo.LEGACY_ALL_OFS_VALUE;

            Log.i(TAG, "[LEGACY] 링크 파라미터 일괄 읽기 완료 : " + count + "개 반영, 이 기기가 아는 인덱스 1~" + mStatus.rel4LinkIndexMax);
            UtilLog.instance.writeLog("패킷 수신 : 구 옵션 17 일괄 읽기->" + count + "개 (인덱스 1~" + mStatus.rel4LinkIndexMax + ")");

            notifyLinkParamsRefreshed();
            return;
        }

        if (option == PacketInfo.LEGACY_OPT_MAP_INIT)
        {
            Log.i(TAG, "[LEGACY] 맵 초기화 응답");
            UtilLog.instance.writeLog("패킷 수신 : 구 옵션 8 맵 초기화");

            Toast.makeText(getApplicationContext(), "맵 초기화 시작\n완료까지 수 초 이상 걸립니다.", Toast.LENGTH_LONG).show();
            return;
        }

        if (packetSize < PacketInfo.LEGACY_RSP_LEN_SINGLE)
        {
            Log.d(TAG, "[LEGACY] 응답이 짧습니다 : 옵션 = " + option + ", 사이즈 = " + packetSize);
            return;
        }

        int index = PacketInfo.getLinkIndexFromLegacyOption(option);

        if (index == PacketInfo.LEGACY_OPT_NONE)
        {
            Log.d(TAG, "[LEGACY] 이 앱이 모르는 구 옵션 수신 : " + option);
            return;
        }

        int value = responsePacket[PacketInfo.LEGACY_RSP_OFS_VALUE] & 0xFF;

        applyLinkParam(index, value);

        // 버튼으로 개별 조작한 결과만 알린다. 연결 직후 읽기나 감시 주기에는 띄우지 않는다.
        if (isLinkInfoReadIdle() && index != PacketInfo.RC_LINK_IDX_CUR_TX_POWER)
        {
            Toast.makeText(getApplicationContext(), makeLinkParamText(index, value), Toast.LENGTH_SHORT).show();
        }
    }

    //
    // 옵션 0x20 — 링크 파라미터 응답
    //
    // 인덱스 0(전체)이면 값이 인덱스 1부터 순서대로 이어진다.
    // 그 외에는 해당 인덱스의 값 하나다.
    //
    private void handleRcLinkResponse(byte[] responsePacket, int index, int valueLen)
    {
        if (index == PacketInfo.RC_IDX_ALL)
        {
            /* 전체 읽기의 값 개수는 펌웨어 버전마다 다르다. 인덱스는 뒤에만 추가되므로
             * 앞부분의 의미는 바뀌지 않는다. 이 앱이 아는 만큼만 읽고 나머지는 무시한다. */
            int count = Math.min(valueLen, PacketInfo.RC_LINK_IDX_ALL_READ_MAX);

            for (int i = 0; i < count; i++)
            {
                applyLinkParam(i + 1, responsePacket[PacketInfo.RC_RSP_OFS_VALUE + i] & 0xFF);
            }

            if (PacketInfo.RC_LINK_IDX_ALL_READ_MAX < valueLen)
            {
                Log.d(TAG, "[LINK] 전체 읽기에 이 앱이 모르는 인덱스가 " + (valueLen - PacketInfo.RC_LINK_IDX_ALL_READ_MAX) + "개 더 있습니다. 무시합니다.");
            }

            Log.i(TAG, "[LINK] 전체 읽기 완료 : " + count + "개");

            /* 상한(인덱스 16)은 전체 읽기에 안 실리므로 갱신 때마다 따로 읽어 준다.
             * 판별 중(=연결 직후)에는 판별 완료 자리에서 이미 읽으므로 여기서는 보내지 않는다.
             * 두 경로가 겹치면 연결할 때마다 같은 읽기가 두 번 나간다. */
            if (isLinkInfoReadIdle())
            {
                requestLinkSettings45("링크 파라미터 갱신");
            }

            notifyLinkParamsRefreshed();
            return;
        }

        if (valueLen < 1)
        {
            Log.d(TAG, "[LINK] 인덱스 " + index + " 응답에 값이 없습니다.");
            return;
        }

        int value = responsePacket[PacketInfo.RC_RSP_OFS_VALUE] & 0xFF;

        applyLinkParam(index, value);

        /* 앱이 스스로 보낸 상한 읽기의 응답이면 알리지 않는다. 사용자가 부른 것이 아니다.
         * 한 번만 억제하고 바로 내린다 — 이후 사용자가 버튼으로 바꾸면 그때는 알려야 한다. */
        if (index == PacketInfo.RC_LINK_IDX_MAX_TX_POWER && mIsAutoMaxTxPowerRead)
        {
            mIsAutoMaxTxPowerRead = false;
            mIsPendingMaxTxPowerWrite = false; // 값이 돌아왔으니 쓰기 대기 문맥도 끝났다

            pumpLinkReadQueue();
            return;
        }

        /* 4.5 인덱스는 앱이 큐로 스스로 읽는 것이라 알리지 않는다.
         * 사용자가 FF 화면에서 바꾼 결과는 그 화면이 보여 주므로 토스트가 겹칠 이유도 없다. */
        if (PacketInfo.isFfLinkIndex(index))
        {
            pumpLinkReadQueue();
            return;
        }

        // 버튼으로 개별 조작한 결과만 알린다. 연결 직후 읽기나 감시 주기에는 띄우지 않는다.
        if (isLinkInfoReadIdle() && index != PacketInfo.RC_LINK_IDX_CUR_TX_POWER)
        {
            Toast.makeText(getApplicationContext(), makeLinkParamText(index, value), Toast.LENGTH_SHORT).show();
        }

        if (index == PacketInfo.RC_LINK_IDX_MAX_TX_POWER)
        {
            mIsPendingMaxTxPowerWrite = false;
        }

        pumpLinkReadQueue();
    }

    // 링크 파라미터 하나를 뷰모델에 반영한다.
    private void applyLinkParam(int index, int value)
    {
        switch (index)
        {
            case PacketInfo.RC_LINK_IDX_CUR_TX_POWER:
                mStatusViewModel.setValueCurTxPowerLevel(value);
                break;

            case PacketInfo.RC_LINK_IDX_BACKTEL_PERIOD:
                mStatusViewModel.setValueBacktelPeriod(value);
                break;

            case PacketInfo.RC_LINK_IDX_MIN_TX_POWER:
                mStatusViewModel.setValueMinTxPowerLevel(value);
                break;

            case PacketInfo.RC_LINK_IDX_MAPPING_MIN_POWER:
                mStatusViewModel.setValueMappingTxPowerLevel(value);
                break;

            case PacketInfo.RC_LINK_IDX_TX_POWER_STEP_UP:
                mStatusViewModel.setValueTxStepUp(value);
                break;

            case PacketInfo.RC_LINK_IDX_FORCE_TX_POWER:
                mStatusViewModel.setValueForceTxPowerLevel(value);
                break;

            case PacketInfo.RC_LINK_IDX_ONE_COIN:
                mStatusViewModel.setValueOneCoin(value);
                break;

            case PacketInfo.RC_LINK_IDX_INFINITE_COIN:
                mStatusViewModel.setValueInfiniteCoin(value);

                // 코인 모드 설정의 두 번째 패킷(one coin)이 대기 중이면 이어서 보낸다.
                sendPendingOneCoinPacket();
                break;

            case PacketInfo.RC_LINK_IDX_CTRL_MODE:
                mStatusViewModel.setValueLinkCtrlMode(value);
                break;

            case PacketInfo.RC_LINK_IDX_TX_POWER_ACCEL:
                mStatusViewModel.setValueTxPowerAccel(value);
                break;

            case PacketInfo.RC_LINK_IDX_POWER_STABLE_NOP:
                mStatusViewModel.setValueNopStandbyCount(value);
                break;

            case PacketInfo.RC_LINK_IDX_PULSE_WIDTH:
                mStatusViewModel.setValuePulseWidth(value);
                break;

            case PacketInfo.RC_LINK_IDX_FRAME_NUM:
                mStatusViewModel.setValueFrameNum(value);
                break;

            case PacketInfo.RC_LINK_IDX_NOP_ENABLE:
                mStatusViewModel.setValueNopEnable(value);
                break;

            case PacketInfo.RC_LINK_IDX_STIM_STRATEGY:
                mStatusViewModel.setValueStimStrategy(value);
                break;

            case PacketInfo.RC_LINK_IDX_MAX_TX_POWER:
                mStatusViewModel.setValueMaxTxPowerLevel(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_ENABLE:
                mStatusViewModel.setValueFfEnable(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_COOLDOWN:
                mStatusViewModel.setValueFfCooldown(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_STEP:
                mStatusViewModel.setValueFfStep(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_RATIO_0:
                mStatusViewModel.setValueFfRatio0(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_RATIO_1:
                mStatusViewModel.setValueFfRatio1(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_RATIO_2:
                mStatusViewModel.setValueFfRatio2(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_RATIO_3:
                mStatusViewModel.setValueFfRatio3(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_RAISED:
                mStatusViewModel.setValueFfRaised(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_APPLIED:
                mStatusViewModel.setValueFfApplied(value);
                break;

            case PacketInfo.RC_LINK_IDX_FF_AVG_AMP:
                mStatusViewModel.setValueFfAvgAmp(value);
                break;

            default:
                Log.d(TAG, "[LINK] 이 앱이 모르는 인덱스 " + index + " 는 무시합니다.");
                break;
        }
    }

    // 조작 결과를 알릴 때 쓰는 문구.
    private String makeLinkParamText(int index, int value)
    {
        switch (index)
        {
            case PacketInfo.RC_LINK_IDX_BACKTEL_PERIOD:
                return "백텔 주기 " + (value * PacketInfo.BACKTEL_PERIOD_UNIT_MS) + "msec";

            case PacketInfo.RC_LINK_IDX_MIN_TX_POWER:
                return String.format("PMIC 하한 %d (%.3fV)", value, (value * PacketInfo.TX_PWR_LEVEL_STEP_MV / 1000.0f));

            case PacketInfo.RC_LINK_IDX_MAPPING_MIN_POWER:
                return String.format("매핑 PMIC 하한 %d (%.3fV)", value, (value * PacketInfo.TX_PWR_LEVEL_STEP_MV / 1000.0f));

            case PacketInfo.RC_LINK_IDX_TX_POWER_STEP_UP:
                return "상승 스텝 " + value + " (" + (value * PacketInfo.TX_PWR_LEVEL_STEP_MV) + "mV)";

            case PacketInfo.RC_LINK_IDX_FORCE_TX_POWER:
                return (value == PacketInfo.FORCE_TX_PWR_RELEASE) //
                       ? "고정 PMIC 해제됨 (링크 제어 재개)" //
                       : String.format("고정 PMIC %d (%.3fV)", value, (value * PacketInfo.TX_PWR_LEVEL_STEP_MV / 1000.0f));

            case PacketInfo.RC_LINK_IDX_ONE_COIN:
                return "One Coin " + ((value == PacketInfo.LINK_FLAG_DISABLE) ? "Off" : "On");

            case PacketInfo.RC_LINK_IDX_INFINITE_COIN:
                return "Infinite Coin " + ((value == PacketInfo.LINK_FLAG_DISABLE) ? "Off" : "On");

            case PacketInfo.RC_LINK_IDX_CTRL_MODE:
                return "링크 제어 모드 : " + ((value == PacketInfo.LINK_CTRL_MODE_BACKTEL) //
                                            ? "BT — 백텔 수신 여부로 판정" //
                                            : "ISD — 내부기 전원 상태로 판정");

            case PacketInfo.RC_LINK_IDX_TX_POWER_ACCEL:
                return "상승 가속 " + ((value == PacketInfo.LINK_FLAG_DISABLE) ? "Off" : "On");

            case PacketInfo.RC_LINK_IDX_POWER_STABLE_NOP:
                /* 펌웨어가 프레임 수에 맞춰 매 사이클 다시 잘라내므로 되읽은 값이 실제 동작
                 * 개수와 다를 수 있다. 그래서 «설정» 이라고만 적고 단정하지 않는다. */
                return "전원 안정 Nop " + value + "개 설정";

            case PacketInfo.RC_LINK_IDX_NOP_ENABLE:
                return (value == PacketInfo.NOP_ENABLE_DEFAULT) //
                       ? "전원 안정 Nop 을 패킷 수별 기본값으로 되돌렸습니다" //
                       : "전원 안정 Nop 을 리모콘 값으로 씁니다";

            default:
                return "링크 인덱스 " + index + " = " + value;
        }
    }

    //
    // 옵션 0x22 — 배터리 텔레메트리 응답 (값 8바이트)
    //
    //   +0 전압(mV) 2바이트, +2 잔량(%), +3 TX PMIC 레벨, +4 시스템 타이머 4바이트
    //   다중 바이트는 상위 바이트를 먼저 싣는다.
    //
    private void handleRcBatteryResponse(byte[] responsePacket, int valueLen)
    {
        if (valueLen < 8)
        {
            Log.d(TAG, "[BATTERY] 텔레메트리 값이 짧습니다 : L = " + valueLen);
            return;
        }

        int ofs = PacketInfo.RC_RSP_OFS_VALUE;

        int mv      = ((responsePacket[ofs] & 0xFF) << 8) | (responsePacket[ofs + 1] & 0xFF);
        int percent = responsePacket[ofs + 2] & 0xFF;
        int pmic    = responsePacket[ofs + 3] & 0xFF;
        int tick    = ((responsePacket[ofs + 4] & 0xFF) << 24) //
                      | ((responsePacket[ofs + 5] & 0xFF) << 16) //
                      | ((responsePacket[ofs + 6] & 0xFF) << 8) //
                      | (responsePacket[ofs + 7] & 0xFF);

        // 전압이 0 이면 보드 교정값이 로드되지 않은 것이다. 정상 범위는 3000~4200mV 다.
        Log.i(TAG, "[BATTERY] 텔레메트리 : " + mv + "mV, " + percent + "%, PMIC " + pmic + ", 타이머 " + tick);
        UtilLog.instance.writeLog("패킷 수신 : 배터리 텔레메트리->" + mv + "mV, " + percent + "%");

        mStatusViewModel.setValueBatteryLevel(percent);
        mStatusViewModel.setValueCurTxPowerLevel(pmic);
    }

    // 연결이 끝난 뒤 호출한다.
    public void startLinkMonitor()
    {
        stopLinkMonitor();

        mLinkMonitorTick = 0;
        mLinkMonitorHandler.postDelayed(mLinkMonitorRunner, LINK_MONITOR_PERIOD_IN_MS);

        Log.d(TAG, "[LINK] 링크 감시를 시작합니다.");
    }

    public void stopLinkMonitor()
    {
        mLinkMonitorHandler.removeCallbacks(mLinkMonitorRunner);
    }

    //
    // Discover services 시간초과 처리 핸들러
    //
    Handler  mDiscoverServicesHandler = new Handler();
    Runnable mDiscoverServicesRunner  = () ->
    {
        Log.d(TAG, "서비스 검색 시간초과입니다. 현재 연결된 기기와 연결해제합니다.");

        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.disconnect();
        }
    };

    //
    // Client Characteristic Configuration Descriptor 설정 시간초과 처리 핸들러
    //
    Handler  mCCCDHandler = new Handler();
    Runnable mCCCDRunner  = () ->
    {
        Log.d(TAG, "CCCD 설정 시간초과입니다. 현재 연결된 기기와 연결해제합니다.");

        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.disconnect();
        }
    };

    //
    // Password 인증 시간초과 처리 핸들러
    //
    Handler  mPasswordHandler = new Handler();
    Runnable mPasswordRunner  = () ->
    {
        Log.d(TAG, "보안코드 인증 시간초과입니다. 현재 연결된 기기와 연결해제합니다.");

        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.disconnect();
        }
    };

    //
    // 사운드처리기 기기 및 맵 정보 읽기 시간초과 처리 핸들러
    //
    Handler  mDeviceAndMapInfoHandler = new Handler();
    Runnable mDeviceAndMapInfoRunner  = () ->
    {
        Log.d(TAG, "사운드처리기 기기 및 맵 정보 읽기 시간초과입니다. 현재 연결된 기기와 연결해제합니다.");

        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.disconnect();
        }
    };

    //
    // 사운드처리기 상태 정보 획득 시간초과 처리 핸들러
    //
    Handler  mStatusHandler = new Handler();
    Runnable mStatusRunner  = () ->
    {
        Log.d(TAG, "사운드처리기 상태정보 획득 시간초과입니다. 현재 연결된 기기와 연결해제합니다.");

        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.disconnect();
        }
    };

    //
    // GATT 콜백 핸들러
    //
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            new Handler(Looper.getMainLooper()).post(() ->
            {
                String name    = gatt.getDevice().getName();
                String address = gatt.getDevice().getAddress();

                // Connected state.
                if (newState == BluetoothProfile.STATE_CONNECTED)
                {
                    mStatus.receivedPackets.clear();
                    mStatus.connectionState = Status.CONNECTION_STATE_CONNECTING;

                    Log.d(TAG, "BLE 연결 이벤트 발생 -> NAME = " + name + ", ADDRESS = " + address);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // Android 12 이상에서는 BLUETOOTH_CONNECT 권한이 필요함
                    {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                        {
                            Log.d(TAG, "[BLE] BLUETOOTH_CONNECT 권한이 없어서, CONNECTION PRIORITY 설정을 할 수 없음");
                        }
                        else
                        {
                            Log.d(TAG, "[BLE] BLUETOOTH_CONNECT 권한이 있어서, CONNECTION PRIORITY 설정 시도");
                            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                        }
                    }
                    else
                    {
                        Log.d(TAG, "[BLE] CONNECTION PRIORITY 설정 시도");
                        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                    }

                    if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED)
                    {
                        Log.d(TAG, "이미 본딩된 기기입니다.");

                        // 서비스 검색 시간초과 핸들러 설정 후 서비스 검색을 시작한다.
                        mDiscoverServicesHandler.postDelayed(mDiscoverServicesRunner, DISCOVER_SERVICES_TIMEOUT_IN_MS);

                        if (!gatt.discoverServices())
                        {
                            Log.d(TAG, "서비스 검색 시도가 실패했습니다.");

                            // 서비스 검색 시도가 실패했으므로, 서비스 검색 시간초과 핸들러도 제거한다.
                            mDiscoverServicesHandler.removeCallbacks(mDiscoverServicesRunner);
                            gatt.disconnect(); // 연결 해제
                        }
                    }
                    else
                    {
                        Log.d(TAG, "본딩을 시도합니다.");

                        gatt.getDevice().createBond();
                    }
                }
                // Disconnected state.
                else if (newState == BluetoothProfile.STATE_DISCONNECTED)
                {
                    Log.d(TAG, "BLE 연결해제 이벤트 발생 -> NAME = " + name + ", ADDRESS = " + address);

                    UtilLog.instance.writeLog("연결 종료 : 장치이름=" + gatt.getDevice().getName());

                    mPacketResponseTimeoutHandler.removeCallbacks(mPacketResponseTimeoutRunner);
                    mPacketSendHandler.removeCallbacks(mPacketSendRunner);
                    stopLinkMonitor();
                    mStatusHandler.removeCallbacks(mStatusRunner);
                    mDeviceAndMapInfoHandler.removeCallbacks(mDeviceAndMapInfoRunner);
                    mPasswordHandler.removeCallbacks(mPasswordRunner);
                    mCCCDHandler.removeCallbacks(mCCCDRunner);
                    mDiscoverServicesHandler.removeCallbacks(mDiscoverServicesRunner);

                    /* OTA 전송 상태를 되돌린다.
                     *
                     * Ota 는 싱글턴이라 이 값이 액티비티는 물론 연결과 재연결을 넘어 살아남는다.
                     * 전송이 응답을 기다리다 연결이 끊기면 IDLE 로 돌아갈 기회가 없어
                     * commState 가 «전송 중» 인 채로 굳는다.
                     *
                     * 그러면 링크 감시 러너와 Write 버튼이 «OTA 전송 중» 으로 보고 매번 물러난다.
                     * 다시 연결해도 풀리지 않아 주기적 읽기가 통째로 멈춘 것처럼 보인다.
                     * 프로세스를 새로 띄워야만 풀렸다. */
                    if (mOta != null && mOta.commState != Ota.COMM_STATE_IDLE)
                    {
                        Log.d(TAG, "[OTA] 연결이 끊겨 전송 상태를 되돌립니다. (이전 상태 " + mOta.commState + ")");
                        UtilLog.instance.writeLog("OTA 전송 상태 초기화 : 연결 종료 (이전 " + mOta.commState + ")");

                        mOta.commState = Ota.COMM_STATE_IDLE;
                    }

                    /* 연결이 끊겼으므로 세대 판별도 중단하고 결과를 지운다.
                     * 다음에 붙는 기기가 다른 세대일 수 있어 그대로 두면 안 된다. */
                    resetLinkInfoRead();

                    mStatus.fwRelease = Status.FW_RELEASE_UNKNOWN;
                    mStatus.rel4LinkIndexMax = 0;
                    publishFwRelease();

                    /* 매핑 연결 응답을 기다리던 중이었으면 그 대기도 푼다.
                     * 안 풀면 다시 붙은 뒤 Write 를 눌러도 «기다리는 중» 이라며 막힌다. */
                    Fragment otaFragment = getSupportFragmentManager().findFragmentById(mBinding.frame.getId());

                    if (otaFragment instanceof RemoteControlFragment)
                    {
                        ((RemoteControlFragment) otaFragment).onDisconnectedForOta();
                    }

                    if (mStatusViewModel != null)
                    {
                        mStatusViewModel.setValueMinTxPowerLevel(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueMappingTxPowerLevel(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueMaxTxPowerLevel(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfEnable(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfCooldown(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfStep(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfRatio0(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfRatio1(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfRatio2(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfRatio3(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfRaised(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfApplied(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueFfAvgAmp(PacketInfo.LINK_VALUE_UNKNOWN);

                        // 연결이 끊겼으니 남은 읽기 요청도 버린다.
                        mLinkReadQueue.clear();
                        mStatusViewModel.setValueTxStepUp(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueForceTxPowerLevel(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueCurTxPowerLevel(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueOneCoin(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueInfiniteCoin(PacketInfo.LINK_VALUE_UNKNOWN);

                        // 연결이 끊겼으므로 코인 모드 두 번째 패킷도 취소한다.
                        mPendingOneCoinValue = PacketInfo.LINK_VALUE_UNKNOWN;
                        mStatusViewModel.setValueBacktelPeriod(PacketInfo.LINK_VALUE_UNKNOWN);
                        mStatusViewModel.setValueGatingState(PacketInfo.LINK_VALUE_UNKNOWN);

                        mStatusViewModel.setValueIsdID(0);
                        mStatusViewModel.setValueBatteryLevel(PacketInfo.INIT_VALUE_BATTERY);
                        mStatusViewModel.setValueNotification(PacketInfo.INIT_VALUE_NOTIFICATION);
                        mStatusViewModel.setValueLed(PacketInfo.INIT_VALUE_LED);
                        mStatusViewModel.setValueTelecoil(PacketInfo.INIT_VALUE_TELECOIL);
                        mStatusViewModel.setValueMaxOutput(PacketInfo.INIT_VALUE_MAX_OUTPUT);
                        mStatusViewModel.setValueVolume(PacketInfo.INIT_VALUE_VOLUME);
                        mStatusViewModel.setValueProgram(PacketInfo.INIT_VALUE_PROGRAM);
                    }

                    gatt.close();
                    mBluetoothGatt = null;

                    // 패킷 전송 상태 초기화
                    mStatus.transferState = Status.TRANSFER_STATE_IDLE;

                    // 사용자의 의도로 연결해제가 발생한게 아니라면, 다시 스캔을 시작한다.
                    if (mStatus.connectionState != Status.CONNECTION_STATE_DISCONNECTING)
                    {
                        mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTED;

                        Log.d(TAG, "사용자가 아닌 다른 이유에 의한 연결해제로 인식되었습니다. (예, 장치 리셋, 통신 거리 벗어남 등)");

                        if (mBinding.lockScreen.getVisibility() == View.VISIBLE)
                        {
                            Log.d(TAG, "현재 잠금화면이 활성화 중이므로 BLE 스캔을 시작하지 않습니다.");
                        }
                        else
                        {
                            Fragment fragment = getSupportFragmentManager().findFragmentById(mBinding.frame.getId());

                            if (fragment instanceof RemoteControlFragment)
                            {
                                if (mStatus.activityRunningState == Status.ACTIVITY_RUNNING_STATE_FOREGROUND)
                                {

                                    Log.d(TAG, "현재 리모컨 화면이며, 액티비티 화면이 포그라운드 상태이므로 자동 재연결을 위해 BLE 스캔을 시작합니다.");
                                    scanLe(true);
                                }
                                else
                                {
                                    Log.d(TAG, "현재 리모컨 화면이지만, 액티비티 화면이 포그라운드 상태가 아니므로 BLE 스캔을 시작하지 않습니다.");
                                }
                            }
                        }
                    }
                    else // 사용자의 의도로 연결해제 한 것이라면,
                    {    // 이 후 사용자의 이벤트로 스캔을 다시 시작할 것이므로 지금은 스캔을 시작하지 않는다.
                        mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTED;
                        Log.d(TAG, "사용자에 의한 연결해제로 인식되었습니다.");

                    }

                    // 리모컨 화면 프래그먼트에서 "사운드처리기 검색 중" 화면을 출력시키기 위해 BLE 뷰모델 값을 업데이트한다.
                    // 리모컨 화면 프래그먼트에서 옵저버로 감시하고 있다.
                    if (mStatusViewModel != null)
                    {
                        mStatusViewModel.setConnectionState(StatusViewModel.CONNECTION_STATE_DISCONNECTED);
                    }
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            String name    = gatt.getDevice().getName();
            String address = gatt.getDevice().getAddress();

            // 서비스 검색이 완료 되었으므로 서비스 검색 시간초과 핸들러를 제거한다.
            mDiscoverServicesHandler.removeCallbacks(mDiscoverServicesRunner);

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

            mCCCDHandler.postDelayed(mCCCDRunner, CCCD_TIMEOUT_IN_MS);

            if (!mBluetoothGatt.writeDescriptor(descriptor))
            {
                // CCCD 쓰기에 실패했으므로, 곧 바로 CCCD 시간초과 핸들러 제거한다.
                mCCCDHandler.removeCallbacks(mCCCDRunner);

                Log.d(TAG, "GATT Callback Service Discovered : Failed to write descriptor.");
                gatt.disconnect();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            //super.onDescriptorWrite(gatt, descriptor, status);

            // CCCD 쓰기에 대한 응답을 받았으므로 시간초과 핸들러 제거한다.
            mCCCDHandler.removeCallbacks(mCCCDRunner);

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                // Success to write descriptor for indication on remote device.
                Log.d(TAG, "CCCD(클라이언트 특성 설정 설명자) 쓰기에 성공했습니다. 보안코드 인증 시간초과 핸들러를 생성하고, 보안코드 패킷을 전송합니다.");

                // 보안코드 인증하기 전에 핸들러를 등록한다.
                mPasswordHandler.postDelayed(mPasswordRunner, PASSWORD_TIMEOUT_IN_MS);
                byte[] passKey = mStatus.connectedUser.passKey.getBytes();
                sendPacket(packetMaker(PacketInfo.HEADER_PASSWORD, passKey, 5));
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
            characteristicChanged(gatt, characteristic.getValue());
        } // onCharacteristicChanged
    }; // BluetoothGattCallback

    private void characteristicChanged(BluetoothGatt gatt, byte[] packet)
    {
        //mStatus.receivedPackets.offer(characteristic.getValue());
        mStatus.receivedPackets.offer(packet);

        new Handler(Looper.getMainLooper()).post(() ->
        {
            // 수신 패킷 정보 획득
            //byte[] responsePacket = characteristic.getValue(); // Extract data from packet.
            byte[] responsePacket = mStatus.receivedPackets.poll();

            if (responsePacket == null)
            {
                Log.d(TAG, "Status의 receivedPackets이 null입니다.");
                return;
            }

            int packetSize = responsePacket.length; // Get size of packet data.

            Log.v(TAG, "BLE 특성 변화 감지 : " + printLogBytesToString(responsePacket));

            if (packetSize < 1)
            {
                Log.d(TAG, "사이즈가 1보다 작은 패킷을 수신하였습니다. 패킷은 1보다 작을 수 없습니다.");
                Log.d(TAG, "수신한 패킷에 문제가 있으므로, 바로 return; 하여 패킷 응답 시간초과가 발생하도록 유도합니다.");
                return;
            }

            // 패킷 응답 시간초과 핸들러 제거
            mPacketResponseTimeoutHandler.removeCallbacks(mPacketResponseTimeoutRunner);

            // 패킷 전송 상태 초기화 -> 다시 IDLE 상태로 돌아간다.
            mStatus.transferState = Status.TRANSFER_STATE_IDLE;

            // 수신 패킷 헤더 추출
            byte packetHeader = byteExtractor(responsePacket[0]);

            // 수신한 패킷 헤더에 따라 처리를 수행한다.
            switch (packetHeader)
            {
                // 보안코드
                case PacketInfo.HEADER_PASSWORD:
                {
                    // 보안코드 응답을 받았으므로, 핸들러를 제거한다.
                    mPasswordHandler.removeCallbacks(mPasswordRunner);

                    if (packetSize != PacketInfo.PACKET_SIZE_PASSWORD) // 보안코드 응답 패킷 사이즈 체크
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 보안코드 응답 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 보안코드 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
                        packetSizeErrorDialog();
                        return;
                    }
                    else // 보안코드 응답 패킷 사이즈가 올바를 때
                    {
                        // Correct password.
                        if (byteExtractor(responsePacket[1]) == PacketInfo.PASSWORD_PASS)
                        {

                            Log.d(TAG, "사용자의 내부기 키가 올바릅니다.");
                            Log.d(TAG, "내부기 키 인증에 성공했습니다. 상태정보 획득 시간초과 핸들러를 생성하고, 상태정보 획득 패킷을 전송합니다. ");

                            // 사운드처리기 기기 및 맵 정보 읽기 패킷을 보내기 전에 핸들러를 등록한다.
                            mDeviceAndMapInfoHandler.postDelayed(mDeviceAndMapInfoRunner, DEVICE_AND_MAP_INFO_IN_MS);
                            sendPacket(packetMaker(PacketInfo.HEADER_SOUND_PROCESSOR_INFO, null, 1));
                        }
                        // Not correct password.
                        else
                        {

                            Log.d(TAG, "사용자의 내부기 키가 올바르지 않습니다.");
                            Log.d(TAG, "스캔을 멈추고, 사운드처리기와의 연결을 해제합니다.");

                            UtilLog.instance.writeLog("패킷 에러 : 올바르지 않은 내부기 키");

                            // 사용자에 의한 연결 종료로 처리한다.
                            mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                            if (gatt != null)
                            {
                                gatt.disconnect();    // 연결 종료
                            }

                            // 이미 생성된 다이얼로그가 있다면, 그 다이얼로그를 종료하고 사용자 보안코드 재설정 다이얼로그를 생성해야 한다.
                            Log.d(TAG, "현재 생성된 다이얼로그가 있다면 종료하고, 사용자 내부기 키 재설정을 위한 다이얼로그를 새로 생성합니다.");

                            lastDialogDismiss();

                            mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this).setTitle("주의").setMessage("내부기 키가 일치하지 않습니다. 재설정하시겠습니까?").setPositiveButton("재설정", (dialogInterface, i) ->
                            {
                                longTimeIdleHandlerUpdate(true);

                                //EntityUser user = UtilUser.instance.getDefaultUser();
                                EntityUser user   = mStatus.connectedUser;
                                Bundle     bundle = new Bundle();
                                bundle.putString(EditUserFragment.ARG_NAME, user.name);
                                bundle.putString(EditUserFragment.ARG_PASSKEY, user.passKey);
                                bundle.putString(EditUserFragment.ARG_NICKNAME, user.nickname);
                                bundle.putString(EditUserFragment.ARG_EAR, user.ear);
                                bundle.putString(EditUserFragment.ARG_DEFAULT, user.defaultUser);
                                            /*
                                            EditUserFragment editUserFragment = new EditUserFragment();
                                            editUserFragment.setArguments(bundle);
                                            getSupportFragmentManager().beginTransaction().replace(mBinding.frame.getId(), editUserFragment)
                                            .commitNowAllowingStateLoss();
                                            */
                                replaceFragment(Status.TypeOfFragment.USER_EDIT, bundle);
                            }).setNegativeButton("취소", (dialogInterface, i) ->
                            {
                                longTimeIdleHandlerUpdate(true);
                            }).setCancelable(false).create();

                            mStatus.lastDialog.show();
                        }
                    }
                } // PacketInfo.HEADER_PASSWORD
                break;

                // 사운드처리기 기기 및 맵 정보 읽기
                case PacketInfo.HEADER_SOUND_PROCESSOR_INFO:
                {
                    // 기기 및 맵 정보 읽기 응답을 받았으므로, 핸들러를 제거한다.
                    mDeviceAndMapInfoHandler.removeCallbacks(mDeviceAndMapInfoRunner);

                    if (packetSize != PacketInfo.PACKET_SIZE_PROCESSOR_INFO && packetSize != 9)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 사운드처리기 기기 및 맵 정보 읽기 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 사운드처리기 기기 및 맵 정보 읽기 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        packetSizeErrorDialog();
                        break;
                    }

                    int fwVerLower = responsePacket[7];
                    int fwVerUpper = responsePacket[8];

                    mStatusViewModel.setFwVerLower(fwVerLower);
                    mStatusViewModel.setFwVerUpper(fwVerUpper);

                    Log.d(TAG, "사운드처리기 펌웨어 버전은 '" + fwVerUpper + "." + fwVerLower + "' 입니다.");

                    // 상태정보 획득 패킷을 보내기 전에 핸들러를 등록한다.
                    mStatusHandler.postDelayed(mStatusRunner, STATUS_TIMEOUT_IN_MS);

                    //sendPacket(packetMaker(PacketInfo.HEADER_SOUND_PROCESSOR_STATUS, null, 1)); // 리모콘 앱 연결 하는 정상 시퀀스
                    sendPacket(packetMaker(PacketInfo.HEADER_SOUND_PROCESSING_PARAM, new byte[]{1}, 2)); // 연결 중 내부기 ID를 읽는 시퀀스
                }
                break;

                // 내부기 ID 읽기
                //case PacketInfo.HEADER_ISD_ID:
                case PacketInfo.HEADER_SOUND_PROCESSING_PARAM:
                {
                    if (packetSize < 2)
                    {
                        // 상태정보 획득 응답은 아니지만, 이 핸들러를 사용했으므로 이 핸들러를 제거한다.
                        mStatusHandler.removeCallbacks(mStatusRunner);

                        Log.d(TAG, "'사운드 신호처리 파라미터' 패킷의 사이즈 이상 감지됨. 현재 패킷 사이즈 : " + packetSize);
                        packetSizeErrorDialog();
                        break;
                    }

                    int dataIndex;
                    int pkt9, pkt10, pkt11, pkt12;

                    dataIndex = (int) responsePacket[1] & 0x000000FF;

                    switch (dataIndex)
                    {
                        case PacketInfo.SOUND_PRECESSING_PARAM_INDEX_MIN:
                        {
                            // 상태정보 획득 응답은 아니지만, 이 핸들러를 사용했으므로 이 핸들러를 제거한다.
                            mStatusHandler.removeCallbacks(mStatusRunner);

                            if (packetSize != PacketInfo.PACKET_SIZE_SOUND_PROCESSING_PARAM_INDEX_1)
                            {
                                Log.d(TAG, "'사운드 신호처리 파라미터' 패킷의 사이즈 에러. 현재 패킷 사이즈 : " + packetSize);
                                UtilLog.instance.writeLog("'사운드 신호처리 파라미터' 패킷의 사이즈 에러 (사이즈->" + packetSize + ")");

                                packetSizeErrorDialog();
                                break;
                            }

                            pkt9 = (int) responsePacket[9] & 0x000000FF;
                            pkt10 = (int) responsePacket[10] & 0x000000FF;
                            pkt11 = (int) responsePacket[11] & 0x000000FF;
                            pkt12 = (int) responsePacket[12] & 0x000000FF;

                            int isdID = (pkt9 << 24) | (pkt10 << 16) | (pkt11 << 8) | pkt12;

                            if (isdID == 0x1891001F)
                            {
                                Log.d(TAG, "내부기 ID '" + String.format("#%08X", isdID) + "'는 '#1691000A'으로 교체합니다.");
                                isdID = 0x1691000A;
                            }
                            else if (isdID == 0x18910015)
                            {
                                Log.d(TAG, "내부기 ID '" + String.format("#%08X", isdID) + "'는 '#1691000B'으로 교체합니다.");
                                isdID = 0x1691000B;
                            }
                            else if (isdID == 0x1891000B)
                            {
                                Log.d(TAG, "내부기 ID '" + String.format("#%08X", isdID) + "'는 '#1691000C'으로 교체합니다.");
                                isdID = 0x1691000C;
                            }
                            else if (isdID == 0x18910001)
                            {
                                Log.d(TAG, "내부기 ID '" + String.format("#%08X", isdID) + "'는 '#1691000D'으로 교체합니다.");
                                isdID = 0x1691000D;
                            }
                            else if (isdID == 0x1891001C)
                            {
                                Log.d(TAG, "내부기 ID '" + String.format("#%08X", isdID) + "'는 '#1691000E'으로 교체합니다.");
                                isdID = 0x1691000E;
                            }

                            Log.d(TAG, "내부기 ID 읽기 패킷 수신 : " + "ID = " + String.format("#%08X", isdID));

                            mStatusViewModel.setValueIsdID(isdID);

                            // 데이터 인덱스 MAX 패킷까지 다 수신하는걸 검증하기 위해서 핸들러를 생성하는데, 별도의 핸들러 인스턴스를 할당하지 않았으므로
                            // 상태정보 획득 핸들러를 다시 사용하도록 한다.
                            mStatusHandler.postDelayed(mStatusRunner, STATUS_TIMEOUT_IN_MS);
                        }
                        break;

                        case PacketInfo.SOUND_PRECESSING_PARAM_INDEX_MAX:
                        {
                            // 상태정보 획득 응답은 아니지만, 이 핸들러를 사용했으므로 이 핸들러를 제거한다.
                            mStatusHandler.removeCallbacks(mStatusRunner);

                            // 데이터 인덱스 MAX 패킷까지 다 받은것으로 간주하고, 이제 상태정보 획득 패킷을 수신하도록 하면 된다.

                            // 상태정보 획득 패킷을 보내기 전에 핸들러를 등록한다.
                            mStatusHandler.postDelayed(mStatusRunner, STATUS_TIMEOUT_IN_MS);
                            sendPacket(packetMaker(PacketInfo.HEADER_SOUND_PROCESSOR_STATUS, null, 1));
                        }
                        break;

                        default:
                            break;
                    }
                }
                break;

                // 사운드처리기 상태 정보
                case PacketInfo.HEADER_SOUND_PROCESSOR_STATUS:
                {
                    // 상태정보 획득 응답을 받았으므로, 핸들러를 제거한다.
                    mStatusHandler.removeCallbacks(mStatusRunner);

                    if (packetSize != PacketInfo.PACKET_SIZE_PROCESSOR_STATUS)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 상태정보 응답 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 상태정보 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
                        packetSizeErrorDialog();
                        break;
                    }

                    PacketInfo packetInfo = new PacketInfo();

                    packetInfo.battery = (byte) (responsePacket[1] & 0xff);
                    packetInfo.program = (byte) (responsePacket[2] & 0xff);
                    packetInfo.maxOutput = (byte) (responsePacket[3] & 0xff);
                    packetInfo.volume = (byte) (responsePacket[4] & 0xff);
                    packetInfo.led = (byte) (responsePacket[5] & 0xff);
                    packetInfo.telecoil = (byte) (responsePacket[6] & 0xff);
                    packetInfo.notification = (byte) (responsePacket[7] & 0xff);

                    Log.d(TAG, "사운드처리기 상태 확인 패킷 수신 : " + "배터리 = " + packetInfo.battery + ", " + "맵번호 = " + packetInfo.program + ", " + "볼륨 = " + packetInfo.volume + "," + " " + "최대출력 = " + packetInfo.maxOutput + ", " + "LED = " + packetInfo.led + ", " + "텔레코일 = " + packetInfo.telecoil + ", " + "자극알림 = " + packetInfo.notification);

                    mStatusViewModel.setValueBatteryLevel(packetInfo.battery);
                    mStatusViewModel.setValueNotification(packetInfo.notification);
                    mStatusViewModel.setValueLed(packetInfo.led);
                    mStatusViewModel.setValueTelecoil(packetInfo.telecoil);
                    mStatusViewModel.setValueMaxOutput(packetInfo.maxOutput);
                    mStatusViewModel.setValueVolume(packetInfo.volume);
                    /* 기기에서 직접 프로그램을 바꿨을 수도 있다. 앱이 아는 값과 다르면
                     * 맵이 바뀐 것이므로 링크 파라미터를 다시 읽는다. */
                    if (mStatusViewModel.getValueProgram() != packetInfo.program)
                    {
                        requestLinkParamRefresh("기기 상태에서 프로그램 변경 감지");
                    }

                    mStatusViewModel.setValueProgram(packetInfo.program);

                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);

                    // 현재 BLE 연결중(CONNECTING)인 상태라면, 주기적인 배터리 상태 핸들러를 생성한다.
                    // 하지만 연결된(CONNECTED) 상태라면 핸들러를 생성하지 않는다.
                    if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTING)
                    {
                        if (gatt != null)
                        {
                            UtilLog.instance.writeLog("연결 성공 : 장치이름=" + gatt.getDevice().getName());
                        }
                        else
                        {
                            UtilLog.instance.writeLog("연결 성공 : 장치이름=null");
                        }

                        Log.d(TAG, "사운드처리기와 BLE 통신이 온전하게 연결되었습니다.");
                        mStatus.connectionState = Status.CONNECTION_STATE_CONNECTED;

                        // 리모컨 화면의 옵저버를 위해 뷰모델 값을 업데이트한다.
                        mStatusViewModel.setConnectionState(StatusViewModel.CONNECTION_STATE_CONNECTED);

                        /* 연결 과정의 마지막 단계다. 이어서 링크 설정 값(PMIC 하한, 백텔 주기,
                         * 게이팅 상태)을 차례로 읽어와 화면 상단에 표시한다. */
                        startLinkInfoRead();

                        /* 현재 Tx 파워와 기기 상태를 주기적으로 갱신한다.
                         * OTA 전용 모드나 OTA 전송 중에는 러너가 스스로 쉰다. */
                        startLinkMonitor();
                    }
                }
                break;

                // 자극알림
                case PacketInfo.HEADER_VALUE_NOTIFICATION:
                {
                    if (packetSize != PacketInfo.PACKET_SIZE_NOTIFICATION)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 자극알림 응답 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 자극알림 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
                        packetSizeErrorDialog();
                        break;
                    }

                    int value = responsePacket[1];
                    mStatusViewModel.setValueNotification(value);
                    UtilLog.instance.writeLog("패킷 수신 : 자극알림->" + value);

                }
                break;

                // LED
                case PacketInfo.HEADER_VALUE_LED:
                {
                    if (packetSize != PacketInfo.PACKET_SIZE_LED)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> LED 응답 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : LED 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
                        packetSizeErrorDialog();
                        break;
                    }

                    int value = responsePacket[1];
                    mStatusViewModel.setValueLed(value);
                    UtilLog.instance.writeLog("패킷 수신 : LED알림->" + value);

                }
                break;

                // 텔레코일
                case PacketInfo.HEADER_VALUE_TELECOIL:
                {
                    if (packetSize != PacketInfo.PACKET_SIZE_TELECOIL)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 텔레코일 응답 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 텔레코일 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
                        packetSizeErrorDialog();
                        break;
                    }

                    int value = responsePacket[1];
                    mStatusViewModel.setValueTelecoil(value);
                    UtilLog.instance.writeLog("패킷 수신 : 텔레코일->" + value);
                }
                break;

                // 맵번호
                case PacketInfo.HEADER_VALUE_PROMGRAM:
                {
                    if (packetSize != PacketInfo.PACKET_SIZE_PROGRAM)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 맵번호 응답 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 맵번호 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
                        packetSizeErrorDialog();
                        break;
                    }

                    int value = responsePacket[1];
                    mStatusViewModel.setValueProgram(value);
                    UtilLog.instance.writeLog("패킷 수신 : 프로그램->" + value);

                    /* 프로그램이 바뀌면 맵이 바뀐다. 펄스폭 · 패킷 수 · 자극 전략이 따라
                     * 달라지고 전원 안정 Nop 도 기본값으로 다시 잡히므로 곧바로 다시 읽는다. */
                    requestLinkParamRefresh("프로그램 변경");

                }
                break;
                // 최대출력
                case PacketInfo.HEADER_VALUE_MAX_OUTPUT:
                {
                    if (packetSize != PacketInfo.PACKET_SIZE_MAX_OUTPUT)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 최대출력 응답 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 최대출력 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
                        packetSizeErrorDialog();
                        break;
                    }

                    int value = responsePacket[1];
                    mStatusViewModel.setValueMaxOutput(value);
                    UtilLog.instance.writeLog("패킷 수신 : 최대출력->" + value);

                }
                break;
                // 볼륨
                case PacketInfo.HEADER_VALUE_VOLUME:
                {
                    if (packetSize != PacketInfo.PACKET_SIZE_VOLUME)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 볼륨 응답 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 볼륨 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
                        packetSizeErrorDialog();
                        break;
                    }

                    int value = responsePacket[1];
                    mStatusViewModel.setValueVolume(value);
                    UtilLog.instance.writeLog("패킷 수신 : 볼륨->" + value);

                }
                break;

                case PacketInfo.HEADER_BOOT_STATUS:
                {
                    RemoteControlFragment remoteFragment;
                    Fragment              fragment = getSupportFragmentManager().findFragmentById(mBinding.frame.getId());

                    if (!(fragment instanceof RemoteControlFragment))
                    {
                        Log.e(TAG, "[BLE][OTA] 리모콘 화면이 아닌데, 어떻게 호출됐지?");
                        return;
                    }

                    remoteFragment = (RemoteControlFragment) fragment;

                    // Info
                    if (responsePacket[1] == 1)
                    {
                        String boot_slot_num, last_boot_slot_num;

                        boot_slot_num = "" + (responsePacket[5] & 0xFF);
                        last_boot_slot_num = "" + (responsePacket[6] & 0xFF);

                        new Handler(Looper.getMainLooper()).post(() ->
                        {
                            remoteFragment.mRemoteControlBinding.currSlotTextView.setText(boot_slot_num);
                            remoteFragment.mRemoteControlBinding.lastSlotTextView.setText(last_boot_slot_num);
                        });
                    }
                    // Select
                    else if (responsePacket[1] == 2)
                    {
                        if (responsePacket[2] == 1)
                        {
                            Log.i(TAG, "[BLE][OTA] 슬롯 선택 성공.");
                            remoteFragment.makeDialog_withMessage("슬롯 선택 성공.");
                        }
                        else
                        {
                            Log.i(TAG, "[BLE][OTA] 슬롯 선택 실패.");
                            remoteFragment.makeDialog_withMessage("슬롯 선택 실패.");
                        }
                    }
                }
                break;

                // 특수 시스템 동작 설정 (릴리즈 4 프로토콜)
                case PacketInfo.HEADER_SPECIFIC_CMD:
                {
                    /* 옵션 바이트를 읽기 전에 최소 길이를 먼저 확인한다. */
                    if (packetSize < 2)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 0x59 응답에 옵션이 없습니다 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 0x59 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                        packetSizeErrorDialog();
                        break;
                    }

                    int option = responsePacket[PacketInfo.RC_RSP_OFS_OPTION] & 0xFF;

                    /* 묵음(옵션 1, 2)만 구형 포맷이다. 도메인 · 인덱스 구조가 아니라
                     * [커맨드, 옵션, 세부1, 상태, 오프셋] 다섯 바이트로 온다. */
                    if (option == PacketInfo.RC_OPT_MUTE_READ || option == PacketInfo.RC_OPT_MUTE_WRITE)
                    {
                        if (packetSize != PacketInfo.RC_RSP_LEN_MUTE)
                        {
                            Log.d(TAG, "BLE 특성 변경 감지 -> 묵음 응답 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                            UtilLog.instance.writeLog("패킷 에러 : 묵음 응답 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                            packetSizeErrorDialog();
                            break;
                        }

                        int    state     = responsePacket[PacketInfo.MUTE_RSP_OFS_STATE] & 0xFF;
                        int    offset    = responsePacket[PacketInfo.MUTE_RSP_OFS_OFFSET] & 0xFF;
                        String stateName = (state == PacketInfo.MUTE_ENABLE) ? "On" : "Off";

                        Log.i(TAG, "[GATING] 게이팅 : " + stateName + ", 오프셋 " + offset);
                        UtilLog.instance.writeLog("패킷 수신 : 게이팅->" + stateName + " (오프셋 " + offset + ")");

                        mStatusViewModel.setValueGatingState(state);

                        if (isLinkInfoReadIdle())
                        {
                            Toast.makeText(getApplicationContext(), "게이팅 " + stateName + " (T레벨 오프셋 " + offset + ")", Toast.LENGTH_SHORT).show();
                        }

                        advanceLinkInfoRead(option);
                        break;
                    }

                    /* 구 옵션(3~23)과 릴리즈 4 도메인(0x20~)은 번호 대역이 겹치지 않는다.
                     * 그래서 옵션 번호만으로 포맷이 갈린다. 세대 판별이 끝나기 전에 오는
                     * 응답도 옳게 갈리므로 세대 값에 기대지 않는다. */
                    if (PacketInfo.isLegacyFormatOption(option))
                    {
                        handleLegacyResponse(responsePacket, packetSize, option);

                        advanceLinkInfoRead(option);
                        break;
                    }

                    /* 여기부터는 릴리즈 4 공통 포맷이다.
                     * [커맨드, 옵션, 액세스, 인덱스, 값의 바이트 수 L, 값 L바이트] */
                    if (packetSize < PacketInfo.RC_RSP_HEADER_SIZE)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 0x59 응답 헤더가 짧습니다 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 0x59 응답 헤더 사이즈 에러 (사이즈->" + packetSize + ")");

                        packetSizeErrorDialog();
                        break;
                    }

                    int index     = responsePacket[PacketInfo.RC_RSP_OFS_INDEX] & 0xFF;
                    int valueLen  = responsePacket[PacketInfo.RC_RSP_OFS_LENGTH] & 0xFF;

                    // 값의 바이트 수가 실제 패킷 길이와 맞는지 확인한다.
                    if (packetSize < PacketInfo.RC_RSP_HEADER_SIZE + valueLen)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 0x59 응답 값 길이 불일치 : 사이즈 = " + packetSize + ", L = " + valueLen);
                        UtilLog.instance.writeLog("패킷 에러 : 0x59 응답 값 길이 불일치 (사이즈->" + packetSize + ", L->" + valueLen + ")");

                        packetSizeErrorDialog();
                        break;
                    }

                    if (option == PacketInfo.RC_OPT_VERSION)
                    {
                        handleRcVersionResponse(responsePacket, valueLen);
                    }
                    else if (option == PacketInfo.RC_OPT_LINK)
                    {
                        handleRcLinkResponse(responsePacket, index, valueLen);
                    }
                    else if (option == PacketInfo.RC_OPT_STIM)
                    {
                        // 맵 초기화는 쓰기 전용이라 돌려받을 값이 없다. 수락되었다는 뜻이다.
                        Log.i(TAG, "[STIM] 자극 제어 응답 : 인덱스 " + index);
                        UtilLog.instance.writeLog("패킷 수신 : 자극 제어 인덱스 " + index);

                        if (index == PacketInfo.RC_STIM_IDX_MAP_INIT)
                        {
                            Toast.makeText(getApplicationContext(), "맵 초기화 시작\n완료까지 수 초 이상 걸립니다.", Toast.LENGTH_LONG).show();
                        }
                    }
                    else if (option == PacketInfo.RC_OPT_BATTERY)
                    {
                        handleRcBatteryResponse(responsePacket, valueLen);
                    }
                    else
                    {
                        Log.d(TAG, "[SPECIFIC] 처리하지 않는 0x59 옵션 수신 : " + option);
                    }

                    advanceLinkInfoRead(option);
                }
                break; // PacketInfo.HEADER_SPECIFIC_CMD

                // 매핑 연결 응답 : OTA 전송 직전에 보내는 0x60 의 짝이다.
                case PacketInfo.HEADER_MAPPING_CONNECT:
                {
                    if (packetSize < PacketInfo.PACKET_SIZE_MAPPING_CONNECT_RESP)
                    {
                        Log.d(TAG, "BLE 특성 변경 감지 -> 매핑 연결 응답이 짧습니다 : 사이즈 = " + packetSize);
                        UtilLog.instance.writeLog("패킷 에러 : 매핑 연결 응답 사이즈 에러 (사이즈->" + packetSize + ")");
                        break;
                    }

                    Log.i(TAG, "[OTA] 매핑 연결 응답 수신");
                    UtilLog.instance.writeLog("패킷 수신 : 매핑 연결");

                    Fragment mappingFragment = getSupportFragmentManager().findFragmentById(mBinding.frame.getId());

                    if (mappingFragment instanceof RemoteControlFragment)
                    {
                        ((RemoteControlFragment) mappingFragment).onMappingConnected();
                    }
                }
                break; // PacketInfo.HEADER_MAPPING_CONNECT

                case PacketInfo.HEADER_OTA:
                {
                    Fragment fragment = getSupportFragmentManager().findFragmentById(mBinding.frame.getId());

                    if (fragment instanceof RemoteControlFragment)
                    {
                        if (mOta.commState == Ota.COMM_STATE_WAIT_RESP_COMMAND)
                        {
                            ((RemoteControlFragment) fragment).handleRespCommandPacket(responsePacket);
                        }
                        else if (mOta.commState == Ota.COMM_STATE_WAIT_RESP_DATA)
                        {
                            ((RemoteControlFragment) fragment).handleRespDataPacket(responsePacket);
                        }
                    }
                }
                break; // PacketInfo.HEADER_OTA


                // 에러
                case PacketInfo.HEADER_ERROR:
                {
                    RemoteControlFragment remoteFragment;
                    Fragment              fragment;

                    if (responsePacket[1] == PacketInfo.HEADER_OTA)
                    {
                        fragment = getSupportFragmentManager().findFragmentById(mBinding.frame.getId());

                        if (!(fragment instanceof RemoteControlFragment))
                        {
                            Log.e(TAG, "[BLE][OTA] 리모콘 화면이 아닌데, 어떻게 호출됐지?");
                            return;
                        }

                        remoteFragment = (RemoteControlFragment) fragment;

                        ((RemoteControlFragment) fragment).makeDialog_withMessage("OTA 에러 패킷 수신");
                    }
                    else if (responsePacket[1] == PacketInfo.HEADER_BOOT_STATUS)
                    {
                        fragment = getSupportFragmentManager().findFragmentById(mBinding.frame.getId());

                        if (!(fragment instanceof RemoteControlFragment))
                        {
                            Log.e(TAG, "[BLE][OTA] 리모콘 화면이 아닌데, 어떻게 호출됐지?");
                            return;
                        }

                        remoteFragment = (RemoteControlFragment) fragment;

                        ((RemoteControlFragment) fragment).makeDialog_withMessage("BOOT 에러 패킷 수신");
                    }
                    else
                    {
                        /* 에러 응답은 «정상적인 응답» 이다. 거절당했다는 뜻이지 프레임이
                         * 깨졌다는 뜻이 아니다. 그래서 여기서 연결을 끊지 않는다.
                         *
                         * 앱이 읽는 것은 앞의 세 바이트(헤더 · 커맨드 · 주에러)뿐이라
                         * 그만큼만 있으면 해석할 수 있다. */
                        if (packetSize < PacketInfo.PACKET_SIZE_ERROR_MIN)
                        {
                            Log.d(TAG, "BLE 특성 변경 감지 -> 에러 패킷이 너무 짧습니다 : 사이즈 = " + packetSize);
                            UtilLog.instance.writeLog("패킷 에러 : 에러 패킷 사이즈 부족 (사이즈->" + packetSize + ")");
                            break;
                        }

                        if (packetSize != PacketInfo.PACKET_SIZE_ERROR)
                        {
                            Log.d(TAG, "[BLE] 에러 패킷 길이가 규격과 다릅니다 : 사이즈 = " + packetSize + " (규격 " + PacketInfo.PACKET_SIZE_ERROR + ")");
                        }

                        byte errorType = byteExtractor(responsePacket[2]);
                        byte failedCmd = byteExtractor(responsePacket[1]);

                        /* 세대 판별 중에 0x59 가 거절되는 것은 «정상» 이다. 그 세대가 모르는
                         * 옵션을 일부러 보내 보는 중이기 때문이다. 다음 후보로 내려가고
                         * 사용자에게는 알리지 않는다. */
                        if (failedCmd == PacketInfo.HEADER_SPECIFIC_CMD && !isLinkInfoReadIdle())
                        {
                            Log.d(TAG, "[LINK] 판별 중 0x59 거절 (에러 " + errorType + "). 다음 후보로 넘어갑니다.");

                            onLinkInfoReadFailed(errorType);
                            break;
                        }

                        switch (errorType)
                        {
                            case 1: // 없는 명령
                            case 2: // 데이터 범위 이탈
                            case 8: // BLE 프로토콜 에러 (사운드처리기 en__EN__BLE_PROTOCOL_ERROR)
                                Log.d(TAG, "패킷 위반 에러를 수신했습니다. (에러 " + errorType + ", 커맨드 0x" + String.format("%02X", failedCmd) + ")");

                                /* 상한 쓰기를 보내 놓고 «범위 이탈» 이 왔으면 그것은 en__OutOfDataRange 다.
                                 * 에러 응답에 인덱스가 없어 이 문맥이 유일한 단서다.
                                 * 무엇을 고쳐야 하는지까지 알려 준다 — 하한이 바닥을 밀어 올린 것이기 때문이다. */
                                /* 거절이 와도 읽기 큐는 다음으로 넘어가야 한다.
                                 * 하나에 막히면 뒤의 인덱스가 영영 안 읽힌다. */
                                pumpLinkReadQueue();

                                if (errorType == 2 && mIsPendingMaxTxPowerWrite)
                                {
                                    mIsPendingMaxTxPowerWrite = false;

                                    Toast.makeText(getApplicationContext(), makeMaxTxPowerRangeText(), Toast.LENGTH_LONG).show();
                                    break;
                                }

                                if (!mStatus.isEnabledInvalidPacketToast)
                                {
                                    Toast.makeText(getApplicationContext(), "사운드처리기가 유효하지 않은 명령어를 전송했습니다.", Toast.LENGTH_LONG).show();

                                    mStatus.isEnabledInvalidPacketToast = true;

                                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    {
                                        mStatus.isEnabledInvalidPacketToast = false;
                                        longTimeIdleHandlerUpdate(true);
                                    }, 3500);
                                }
                                break;

                            case 3: // Busy
                                Log.d(TAG, "Busy 에러를 수신했습니다.");

                                if (!mStatus.isEnabledBusyToast)
                                {
                                    Toast.makeText(getApplicationContext(), "이전에 전송한 명령을 처리중입니다.", Toast.LENGTH_LONG).show();

                                    mStatus.isEnabledBusyToast = true;

                                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    {
                                        longTimeIdleHandlerUpdate(true);
                                        mStatus.isEnabledBusyToast = false;
                                    }, 3500);
                                }
                                break;

                            case 4: // 보안코드 미적용 에러
                                Log.d(TAG, "보안코드 미적용 에러를 수신했습니다.");

                                if (!mStatus.isEnabledUnlockedToast)
                                {
                                    Toast.makeText(getApplicationContext(), "사운드처리기의 암호가 풀리지 않았습니다. 보안 비밀번호를 사용해 잠금을 해제해주세요.", Toast.LENGTH_LONG).show();
                                    mStatus.isEnabledUnlockedToast = true;

                                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    {
                                        longTimeIdleHandlerUpdate(true);
                                        mStatus.isEnabledUnlockedToast = false;
                                    }, 3500);
                                }
                                break;

                            case 5: // SPI 통신 에러
                            case 6: // CFX_CM3 통신 에러
                            case 7: // NRF_FLASH 초기화 에러
                                Log.d(TAG, "사운드처리기에 문제가 발생했습니다.");

                                if (!mStatus.isEnabledInternalErrorToast)
                                {
                                    Toast.makeText(getApplicationContext(), "사운드처리기 내부에서 에러가 발생했습니다. 탈착 후 다시 부착해주세요.", Toast.LENGTH_LONG).show();
                                    mStatus.isEnabledInternalErrorToast = true;

                                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    {
                                        longTimeIdleHandlerUpdate(true);
                                        mStatus.isEnabledInternalErrorToast = false;
                                    }, 3500);
                                }
                                break;
                        }
                    }
                }
                break;
            } // switch
        }); // handler mainLooper
    }

    //
    // 패킷 사이즈 에러 다이얼로그
    //
    public void packetSizeErrorDialog()
    {
        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
        lastDialogDismiss();

        mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this).setTitle("에러").setMessage("통신 에러가 발생했습니다. 앱을 다시 시작해주세요. 같은 에러가 반복되면 외부기를 다시 " + "착용해주세요.").setPositiveButton("확인", null).setCancelable(false).create();
        mStatus.lastDialog.show();

        if (mBluetoothGatt != null)
        {
            if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTING || mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
            {
                mBluetoothGatt.disconnect();
            }
        }
    }

    //
    // 진동 발생 함수
    //
    private void vibrator(int ms)
    {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(ms);
    }

    //
    // 진동 생성기 호출자
    //
    public void onVibrator(int ms)
    {
        vibrator(ms);
    }

    //
    // 바이트 추출기
    //
    public byte byteExtractor(byte b)
    {
        return (byte) (b & 0xFF);
    }

    //
    // 패킷 응답 시간 초과 핸들러 관련
    //
    Handler  mPacketResponseTimeoutHandler = new Handler();
    Runnable mPacketResponseTimeoutRunner  = () ->
    {
        boolean retBool;

        if ((mBluetoothGatt == null) || (mStatus.connectionState != Status.CONNECTION_STATE_CONNECTED))
        {
            Log.e(TAG, "[BLE] 패킷 전송 실패 → 'GATT == null' 또는 '연결 상태 아님'.");
            return;
        }

        /* 세대 판별 중에는 «응답이 없는 것» 도 답이다. 그 세대가 모르는 옵션을 일부러
         * 보내 보는 중인데, 에러조차 안 주고 무시하는 펌웨어가 있다.
         *
         * 여기서 재전송하고 끊어 버리면 연결과 해제가 끝없이 반복된다. 같은 패킷을
         * 다시 보내 봐야 결과가 같으므로 재전송도 하지 않고 다음 후보로 내려간다. */
        if (!isLinkInfoReadIdle())
        {
            Log.d(TAG, "[LINK] 판별 중 응답이 없습니다. 다음 후보로 넘어갑니다.");

            mStatus.transferState = Status.TRANSFER_STATE_IDLE;
            onLinkInfoReadFailed(PacketInfo.LEGACY_OPT_NONE);
            return;
        }

        if (mStatus.resendingCount <= 0)
        {
            Log.e(TAG, "[BLE] 패킷 전송 재전송 횟수 초과.");
            mBluetoothGatt.disconnect();
            return;
        }

        mStatus.resendingCount--;

        retBool = mCharClientToServer.setValue(mStatus.sendingPacket);

        if (!retBool)
        {
            Log.e(TAG, "[BLE] 패킷 준비 실패.");
            mBluetoothGatt.disconnect();
            return;
        }

        retBool = mBluetoothGatt.writeCharacteristic(mCharClientToServer);

        if (!retBool)
        {
            Log.e(TAG, "[BLE] 패킷 재전송 실패.");
            mBluetoothGatt.disconnect();
            return;
        }

    }; // scanRunner

    //
    // 패킷 전송 핸들러 (너무 빠른 전송을 방지하기 위해 약 50ms의 딜레이를 생성하기 위함)
    //
    Handler  mPacketSendHandler = new Handler();
    Runnable mPacketSendRunner  = () ->
    {

        if (mBluetoothGatt == null || mStatus.sendingPacket == null)
        {
            Log.d(TAG, "패킷 전송 시도를 실패했습니다. -> GATT 객체가 null 이거나, 패킷 정보가 없습니다.");
            return;
        }

        boolean isSuccess = false;

        Log.v(TAG, "[BLE] 시간 초과 핸들러 생성 후 패킷 전송 : " + printLogBytesToString(mStatus.sendingPacket));

        mStatus.resendingCount = Status.RESENDING_PACKET_COUNT;
        mPacketResponseTimeoutHandler.postDelayed(mPacketResponseTimeoutRunner, DELAY_IN_MS_FOR_PACKET_RESPONSE_TIMEOUT);

        if (mCharClientToServer.setValue(mStatus.sendingPacket))
        {
            if (mBluetoothGatt.writeCharacteristic(mCharClientToServer))
            {
                isSuccess = true;
            }
        }

        if (!isSuccess)
        {
            Log.d(TAG, "패킷 전송을 실패했습니다. 패킷 전송 시간 초과 핸들러를 제거합니다.");
            mPacketResponseTimeoutHandler.removeCallbacks(mPacketResponseTimeoutRunner);
        }

        //mStatus.sendingPacket = null;
    };

    //
    // 패킷 전송 메소드
    //
    public void sendPacket(byte[] packet)
    {
        if (mStatus.transferState == Status.TRANSFER_STATE_BUSY)
        {
            Log.d(TAG, "패킷 전송 처리 중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        mStatus.transferState = Status.TRANSFER_STATE_BUSY;
        mStatus.sendingPacket = packet;
        mPacketSendHandler.postDelayed(mPacketSendRunner, SEND_PACKET_DELAY_IN_MS);
    }

    //
    // 바이트 배열을 문자열로 출력하는 함수
    //
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

    //
    // 패킷 생성기
    //
    public byte[] packetMaker(int header, byte[] payload, int maxLen)
    {
        byte[] packet = new byte[maxLen];

        packet[0] = (byte) (header & 0xFF);

        if (payload != null)
        {
            System.arraycopy(payload, 0, packet, 1, Math.min((maxLen - 1), payload.length));
        }

        Log.d(TAG, "새롭게 생성한 패킷 : " + printLogBytesToString(packet));
        return packet;
    }

    //
    // 상태바, 네비게이션바, 툴바 초기화
    //
    public void initStatusNavigationToolBar()
    {
        /* 안드로이드 15(API35)부터 targetSdk 35 이상 앱은 상태바 · 내비게이션바 아래까지
         * 화면 전체에 그리도록 강제된다(edge-to-edge). 그대로 두면 화면 맨 아래 버튼이
         * 내비게이션 바에 가려 눌리지 않으므로, 시스템 바와 디스플레이 컷아웃(펀치홀) 크기만큼
         * 루트 뷰에 여백을 주어 콘텐츠가 안전 영역 안에 들어오게 한다.
         *
         * 안드로이드 14 이하에서는 시스템이 알아서 콘텐츠를 시스템 바 아래로 배치하므로,
         * 여기서 여백을 또 주면 이중으로 밀린다. 그래서 API35 이상에서만 적용한다. */
        if (Build.VERSION.SDK_INT >= 35)
        {
            ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (view, windowInsets) ->
            {
                Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                Insets ime  = windowInsets.getInsets(WindowInsetsCompat.Type.ime());

                /* 키보드가 올라오면 내비게이션 바보다 더 많이 가리므로 둘 중 큰 값을 쓴다.
                 * (edge-to-edge 상태에서는 windowSoftInputMode 의 adjustResize 가 동작하지 않는다.) */
                int bottom = Math.max(bars.bottom, ime.bottom);

                view.setPadding(bars.left, bars.top, bars.right, bottom);

                return WindowInsetsCompat.CONSUMED;
            });
        }

        // 상태바, 하단 네비게이션바 그리고 툴바의 색상 설정
        getWindow().setStatusBarColor(getColor(R.color.status_bar));
        getWindow().setNavigationBarColor(getColor(R.color.bottom_navigation));
        mBinding.toolbar.setBackgroundColor(getColor(R.color.toolbar_background));

        // 툴바의 네비게이션버튼 및 타이틀은 안보이게 설정
        mBinding.toolbar.setNavigationIcon(null);
        mBinding.toolbar.setTitle("");

        // 툴바의 메뉴 아이콘들이 보이도록 설정
        mBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(true);
        mBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(true);
        mBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);

        // OTA 전용 연결 모드 아이콘을 현재 상태에 맞춘다.

        // 툴바의 네비게이션 및 메뉴 버튼 이벤트 리스너 등록
        mBinding.toolbar.setNavigationOnClickListener(mToolBarNavigationClickListener);
        mBinding.toolbar.setOnMenuItemClickListener(mToolBarMenuItemClickListener);
    }

    //
    // 툴바 네비게이션 클릭 리스너
    //
    View.OnClickListener mToolBarNavigationClickListener = view -> onBackPressed();

    //
    // 툴바 메뉴 버튼 클릭 리스너
    //
    Toolbar.OnMenuItemClickListener mToolBarMenuItemClickListener = item ->
    {
        // 장시간 미사용 핸들러 업데이트
        longTimeIdleHandlerUpdate(true);

        if (item.getItemId() == R.id.toolbar_settings)
        {
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.MENU);

            if (mStatus.scanState == Status.SCAN_STATE_STARTED)
            {
                scanLe(false);
            }

            return true;
        }
        else if (item.getItemId() == R.id.toolbar_search)
        {
            scanLeWithDelay(true, 0);
        }
        else if (item.getItemId() == R.id.toolbar_user)
        {
            lastDialogDismiss();

            if (mStatus.scanState == Status.SCAN_STATE_STARTED)
            {
                scanLe(false);
            }

            List<EntityUser> users = UtilUser.instance.getUsers();
            mUserList = new String[users.size()];
            String[] nicknameList = new String[users.size()];

            if (users.size() == 0)
            {
                mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this).setTitle("안내").setMessage("등록된 사용자가 없습니다. 먼저 사용자를 등록해주세요.").setPositiveButton("확인", (dialogInterface, i) ->
                {
                    // 장시간 미사용 핸들러 업데이트
                    longTimeIdleHandlerUpdate(true);
                }).setCancelable(false).create();
            }
            else
            {
                mCheckItem = 0;

                for (int i = 0; i < users.size(); i++)
                {
                    mUserList[i] = users.get(i).name;
                    nicknameList[i] = getUserNicknameIfExist(users.get(i));

                    if (users.get(i).defaultUser.equals(EntityUser.USER_DEFAULT))
                    {
                        mCheckItem = i;
                    }
                }

                mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this).setTitle("사용자 목록").setPositiveButton("선택", (dialogInterface, i) ->
                {
                    // 장시간 미사용 핸들러 업데이트
                    longTimeIdleHandlerUpdate(true);

                    String selectName = mUserList[mCheckItem];

                    EntityUser defaultUser = UtilUser.instance.getDefaultUser();

                    if (defaultUser != null)
                    {
                        defaultUser.defaultUser = EntityUser.USER_NOT_DEFAULT;
                        UtilUser.instance.update(defaultUser);
                    }

                    EntityUser selectUser = UtilUser.instance.getUserByName(selectName);

                    if (selectUser != null)
                    {
                        selectUser.defaultUser = EntityUser.USER_DEFAULT;
                        UtilUser.instance.update(selectUser);
                    }

                    Fragment fragment = MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.frame);

                    if (fragment instanceof RemoteControlFragment)
                    {
                        if (selectUser != null)
                        {
                            ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(UtilUser.makeDisplayName(MainActivity.this, selectUser));
                        }
                    }

                    if (mStatus.connectionState != Status.CONNECTION_STATE_DISCONNECTED)
                    {
                        mBluetoothGatt.disconnect();
                    }
                    else if (mStatus.scanState == Status.SCAN_STATE_STOPPED)
                    {
                        MainActivity.this.scanLe(true);
                    }

                }).setSingleChoiceItems(nicknameList, mCheckItem, (dialogInterface, i) ->
                {
                    longTimeIdleHandlerUpdate(true);
                    mCheckItem = i;
                }).setNegativeButton("취소", (dialogInterface, i) ->
                {
                    longTimeIdleHandlerUpdate(true);
                }).setCancelable(false).create();
            }
            mStatus.lastDialog.show();

            return true;
        }

        return false;
    };

    //
    // 사용자 선택 다이얼로그 생성 함수
    //
    public void makeDialogSelectUser()
    {
        lastDialogDismiss();

        if (mStatus.scanState == Status.SCAN_STATE_STARTED)
        {
            scanLe(false);
        }

        List<EntityUser> users = UtilUser.instance.getUsers();
        mUserList = new String[users.size()];
        String[] nicknameList = new String[users.size()];

        if (users.size() == 0)
        {
            mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this).setTitle("안내").setMessage("등록된 사용자가 없습니다. 먼저 사용자를 등록해주세요.").setPositiveButton("확인", (dialogInterface, i) ->
            {
                // 장시간 미사용 핸들러 업데이트
                longTimeIdleHandlerUpdate(true);
            }).setCancelable(false).create();
        }
        else
        {
            mCheckItem = 0;

            for (int i = 0; i < users.size(); i++)
            {
                mUserList[i] = users.get(i).name;
                nicknameList[i] = getUserNicknameIfExist(users.get(i));

                if (users.get(i).defaultUser.equals(EntityUser.USER_DEFAULT))
                {
                    mCheckItem = i;
                }
            }

            mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this).setTitle("사용자 목록").setPositiveButton("선택", (dialogInterface, i) ->
            {
                // 장시간 미사용 핸들러 업데이트
                longTimeIdleHandlerUpdate(true);

                String selectName = mUserList[mCheckItem];

                EntityUser defaultUser = UtilUser.instance.getDefaultUser();

                if (defaultUser != null)
                {
                    defaultUser.defaultUser = EntityUser.USER_NOT_DEFAULT;
                    UtilUser.instance.update(defaultUser);
                }

                EntityUser selectUser = UtilUser.instance.getUserByName(selectName);

                if (selectUser != null)
                {
                    selectUser.defaultUser = EntityUser.USER_DEFAULT;
                    UtilUser.instance.update(selectUser);
                }

                Fragment fragment = MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.frame);

                if (fragment instanceof RemoteControlFragment)
                {
                    if (selectUser != null)
                    {
                        ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(UtilUser.makeDisplayName(MainActivity.this, selectUser));
                    }
                }

                if (mStatus.scanState == Status.SCAN_STATE_STOPPED)
                {
                    MainActivity.this.scanLe(true);
                }
            }).setSingleChoiceItems(nicknameList, mCheckItem, (dialogInterface, i) ->
            {
                // 장시간 미사용 핸들러 업데이트
                longTimeIdleHandlerUpdate(true);
                mCheckItem = i;
            }).setCancelable(false).create();
        }
        mStatus.lastDialog.show();
    }

    //
    // 잠금화면 버튼 클릭 리스너
    //
    public void onLockScreenNumberClickedListener(View view)
    {
        // 장시간 미사용 핸들러 업데이트
        longTimeIdleHandlerUpdate(true);

        if (mBinding.lockScreen.getVisibility() == View.VISIBLE)
        {
            mLockScreen.numberClickListener(view);
        }
    }

    //
    // 잠금화면 비밀번호 잊어버렸습니다 버튼 리스너
    //
    public void onLockScreenForgetPasswordClickedListener(View view)
    {
        // 장시간 미사용 핸들러 업데이트
        longTimeIdleHandlerUpdate(true);

        lastDialogDismiss();

        mStatus.lastDialog = new MaterialAlertDialogBuilder(this).setTitle(getString(R.string.lock_screen_dialog_title)).setMessage(getString(R.string.lock_screen_dialog_message)).setPositiveButton(getString(R.string.lock_screen_dialog_positive), (dialogInterface, i) ->
        {
            // 장시간 미사용 핸들러 업데이트
            longTimeIdleHandlerUpdate(true);

            // 현재 연결 중인 사운드처리기가 있을 때
            if (mStatus.connectionState != Status.CONNECTION_STATE_DISCONNECTED)
            {
                mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                mBluetoothGatt.disconnect();
            }

            List<EntityUser>   users   = UtilUser.instance.getUsers();
            List<EntityDevice> devices = UtilDevice.instance.getDevices();

            for (EntityUser user : users)
            {
                UtilUser.instance.delete(user);
                Log.d(TAG, "사용자 " + user.name + "  삭제됨.");
            }

            for (EntityDevice device : devices)
            {
                UtilDevice.instance.delete(device);
                Log.d(TAG, "사운드처리기 " + device.serialNumber + "  삭제됨.");
            }

            /* 암호만 지우고 잠금을 다시 켜지는 않는다.
             *
             * 시작 화면에서 잠금을 뺐으므로 초기화 직후만 다시 잠기면 앞뒤가 맞지 않는다.
             * 지운 암호는 resume() 이 기본 암호로 채운다. 잠금은 설정 화면에서 켠다. */
            mLockScreen.erasePassword();
            mLockScreen.resume();

            /* 전체 초기화 뒤에도 설명서를 되살리지 않는다.
             *
             * 예전에는 여기서 설명서 표시를 켜고 그 화면으로 보냈다. 시작 화면에서 설명서를
             * 뺐으므로 초기화 직후만 다시 나오면 앞뒤가 맞지 않는다.
             * 설명서는 설정 화면에 그대로 있다. */
            replaceFragment(Status.TypeOfFragment.REMOTE_CONTROL);
        }).setNegativeButton(getString(R.string.lock_screen_dialog_negative), (dialogInterface, i) ->
        {
            // 장시간 미사용 핸들러 업데이트
            longTimeIdleHandlerUpdate(true);
        }).setCancelable(false).create();
        mStatus.lastDialog.show();
    }

    //
    // 다이얼로그 없애기
    //
    public void lastDialogDismiss()
    {
        if (mStatus.lastDialog != null)
        {
            if (mStatus.lastDialog.isShowing())
            {
                Log.d(TAG, "화면에 출력 중인 다이얼로그를 제거합니다.");
                mStatus.lastDialog.dismiss();
            }

            mStatus.lastDialog = null;
        }
    }

    //
    // 사용자 별칭 가져오기
    //
    public String getUserNicknameIfExist(EntityUser user)
    {
        String retString = "";

        if (user != null)
        {
            if (user.nickname != null && 0 < user.nickname.length())
            {
                retString = user.nickname;
            }
            else
            {
                retString = UtilUser.getNameOnly(user.name) + " (" + UtilUser.getEarKorean(user.ear) + ")";
            }
        }

        return retString;
    }

    //
    // 프래그먼트 전환기
    //
    public void replaceFragment(int typeOfFragment)
    {
        replaceFragment(typeOfFragment, null);
    }

    public void replaceFragment(int typeOfFragment, Bundle bundle)
    {
        if (typeOfFragment == Status.TypeOfFragment.REMOTE_CONTROL)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new RemoteControlFragment()).commitAllowingStateLoss();
        }
        else if (typeOfFragment == Status.TypeOfFragment.MENU)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
        }
        else if (typeOfFragment == Status.TypeOfFragment.USER_LIST)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new UserFragment()).commitAllowingStateLoss();
        }
        else if (typeOfFragment == Status.TypeOfFragment.USER_ADD)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new AddUserFragment()).commitAllowingStateLoss();
        }
        else if (typeOfFragment == Status.TypeOfFragment.USER_EDIT)
        {
            EditUserFragment editUserFragment = new EditUserFragment();

            if (bundle != null)
            {
                editUserFragment.setArguments(bundle);
            }

            getSupportFragmentManager().beginTransaction().replace(R.id.frame, editUserFragment).commitAllowingStateLoss();
        }
        else if (typeOfFragment == Status.TypeOfFragment.DEVICE_LIST)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new DeviceFragment()).commitAllowingStateLoss();
        }
        else if (typeOfFragment == Status.TypeOfFragment.DEVICE_ADD)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new AddDeviceFragment()).commitAllowingStateLoss();
        }
        else if (typeOfFragment == Status.TypeOfFragment.DEVICE_EDIT)
        {
            EditDeviceFragment editDeviceFragment = new EditDeviceFragment();

            if (bundle != null)
            {
                editDeviceFragment.setArguments(bundle);
            }

            getSupportFragmentManager().beginTransaction().replace(R.id.frame, editDeviceFragment).commitAllowingStateLoss();
        }
        else if (typeOfFragment == Status.TypeOfFragment.MANUAL)
        {
            ManualFragment manualFragment = new ManualFragment();

            if (bundle != null)
            {
                manualFragment.setArguments(bundle);
            }

            getSupportFragmentManager().beginTransaction().replace(R.id.frame, manualFragment).commitAllowingStateLoss();
        }
        else if (typeOfFragment == Status.TypeOfFragment.HIDDEN_LOG)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new LogFragment()).commitAllowingStateLoss();
        }
        else
        {
            Log.d(TAG, "알 수 없는 프래그먼트 번호입니다. (" + typeOfFragment + ")");
            return;
        }

        mStatus.typeOfFragment = typeOfFragment;
    }
}