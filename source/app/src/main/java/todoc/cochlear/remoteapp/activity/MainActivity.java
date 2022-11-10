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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.logs.EntityLog;
import todoc.cochlear.remoteapp.database.maps.EntityMap;
import todoc.cochlear.remoteapp.database.maps.UtilMap;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.fragment.AddDeviceFragment;
import todoc.cochlear.remoteapp.fragment.AddUserFragment;
import todoc.cochlear.remoteapp.fragment.DeviceFragment;
import todoc.cochlear.remoteapp.fragment.EditDeviceFragment;
import todoc.cochlear.remoteapp.fragment.EditUserFragment;
import todoc.cochlear.remoteapp.fragment.LogFragment;
import todoc.cochlear.remoteapp.fragment.ManualFragment;
import todoc.cochlear.remoteapp.fragment.RemoteControlFragment;
import todoc.cochlear.remoteapp.fragment.SettingsFragment;
import todoc.cochlear.remoteapp.fragment.ShareFragment;
import todoc.cochlear.remoteapp.fragment.UserFragment;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.list.DeviceAdapter;
import todoc.cochlear.remoteapp.params.MapInfo;
import todoc.cochlear.remoteapp.params.Status;
import todoc.cochlear.remoteapp.params.PacketInfo;
import todoc.cochlear.remoteapp.service.ExitCaptureService;
import todoc.cochlear.remoteapp.shared_preferences.LockScreen;
import todoc.cochlear.remoteapp.shared_preferences.ManualScreen;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;

public class MainActivity extends AppCompatActivity
{
    static private final String TAG = "TODOC_" + MainActivity.class.getSimpleName();

    static private final int REQUEST_PERMISSION_CODE_NUMBER = 100;

    static public final UUID BLE_UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    static public final UUID BLE_UUID_CHARACTERISTIC_CLIENT_TO_SERVER = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    static public final UUID BLE_UUID_CHARACTERISTIC_SERVER_TO_CLIENT = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    static public final UUID BLE_UUID_DESCRIPTION_CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    static private final ParcelUuid SERVICE_DATA_UUID = new ParcelUuid(UUID.fromString("00004944-0000-1000-8000-00805F9B34FB"));
    static private final String BT_NAME_REGEX_FILTER = "^TD_.*$";

    static private final int DELAY_IN_MS_FOR_PACKET_RESPONSE_TIMEOUT = 500;

    static private final int LONG_TIME_IDLE_TIMEOUT_IN_MS = 600000;
    static public final int CHECK_BATTERY_DELAY_IN_MS = 30000;
    static public final int DISCOVER_SERVICES_TIMEOUT_IN_MS = 2000;
    static public final int CCCD_TIMEOUT_IN_MS = 2000;
    static public final int PASSWORD_TIMEOUT_IN_MS = 1000;
    static public final int STATUS_TIMEOUT_IN_MS = 1000;
    static public final int DEVICE_AND_MAP_INFO_IN_MS = 1000;
    static public final int SEND_PACKET_DELAY_IN_MS = 50;

    // Bluetooth
    public BluetoothDevice mBluetoothDevice;
    public BluetoothGatt mBluetoothGatt;
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner mBluetoothLeScanner;
    BluetoothGattService mBluetoothGattService;
    BluetoothGattCharacteristic mCharClientToServer;
    BluetoothGattCharacteristic mCharServerToClient;

    // 뷰 바인딩
    public ActivityMainBinding mBinding;
    public Status mStatus;

    // 잠금화면 관련
    public LockScreen mLockScreen;

    // 매뉴얼 화면 관련
    public ManualScreen mManualScreen;

    // 뷰모델 관련
    private StatusViewModel mStatusViewModel;

    // 사용자 전환 메뉴 버튼 관련
    private String[] mUserList;
    private int mCheckItem;

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


        // TD2-SW-RC-UNIT-Test-ID-26 [잠금 화면 활성화 유닛] 순서[1] 시작.
        /*
        {
            // onCreate -> onResume 순서로 호출 되므로 onCreate에서 생성한 mLockScreen을 사용해서 테스트한다.
            mLockScreen.setEnable(true);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-26 [잠금 화면 활성화 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-26 [잠금 화면 활성화 유닛] 순서[2] 시작.
        /*
        {
            // onCreate -> onResume 순서로 호출 되므로 onCreate에서 생성한 mLockScreen을 사용해서 테스트한다.
            mLockScreen.setEnable(false);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-26 [잠금 화면 활성화 유닛] 순서[2] 끝.


        // TD2-SW-RC-UNIT-Test-ID-27 [잠금 화면 체크 유닛] 순서[1] 시작.
        /*
        {
            // onCreate -> onResume 순서로 호출 되므로 onCreate에서 생성한 mLockScreen을 사용해서 테스트한다.
            Log.d(TAG, "Lock screen setting = " + mLockScreen.isEnabled());
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-27 [잠금 화면 체크 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-29 [암호 쓰기 유닛] 순서[1] 시작.
        /*
        {
            // onCreate -> onResume 순서로 호출 되므로 onCreate에서 생성한 mLockScreen을 사용해서 테스트한다.
            mLockScreen.writePassword("1111");
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-29 [암호 쓰기 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-29 [암호 쓰기 유닛] 순서[2] 시작.
        /*
        {
            // onCreate -> onResume 순서로 호출 되므로 onCreate에서 생성한 mLockScreen을 사용해서 테스트한다.
            mLockScreen.writePassword("2222");
        }
        //*/
        // TD2-SW-RC-UNIT-Test-ID-29 [암호 쓰기 유닛] 순서[2] 끝.


        // TD2-SW-RC-UNIT-Test-ID-28 [암호 삭제 유닛] 순서[1] 시작.
        /*
        {
            // onCreate -> onResume 순서로 호출 되므로 onCreate에서 생성한 mLockScreen을 사용해서 테스트한다.
            mLockScreen.erasePassword();
        }
        //*/
        // TD2-SW-RC-UNIT-Test-ID-29 [암호 삭제 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-30 [암호 읽기 유닛] 순서[1] 시작.
        /*
        {
            // onCreate -> onResume 순서로 호출 되므로 onCreate에서 생성한 mLockScreen을 사용해서 테스트한다.
            Log.d(TAG, "Lock screen password = " + mLockScreen.readPassword());
        }
        //*/
        // TD2-SW-RC-UNIT-Test-ID-30 [암호 읽기 유닛] 순서[1] 끝.


        mStatus.activityRunningState = Status.ACTIVITY_RUNNING_STATE_FOREGROUND;

        // 장시간 미사용 이벤트 핸들러 시작
        Log.d(TAG, "액티비티 onResume() 상태이므로, 장시간 미사용 핸들러를 업데이트합니다.");
        longTimeIdleHandlerUpdate(true);

        lastDialogDismiss();

        // Check lock screen password whether registered or not.
        mLockScreen.resume();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        Log.d(TAG, "액티비티를 종료합니다. -> onDestroy()");

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
        UtilMap.instance.close(); // 맵 데이터베이스 닫기.

        // 모든 핸들러 제거하기
        mLongTimeIdleHandler.removeCallbacks(mLongTimeIdleRunner); // 장시간 미사용 감지 핸들러 제거
        mScanHandler.removeCallbacks(mScanRunner); // 스캔 핸들러 제거
        mCheckBatteryHandler.removeCallbacks(mCheckBatteryRunner); // 배터리 체크 패킷 핸들러 제거
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

        // 장시간 미사용 다이얼로그 제거
        /*
        if (mStatus.longTimeIdleDialog != null)
        {
            if (mStatus.longTimeIdleDialog.isShowing())
            {
                mStatus.longTimeIdleDialog.dismiss();
            }

            mStatus.longTimeIdleDialog = null;
        }
        */

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
        else if (fragment instanceof ShareFragment)
        {
            ((ShareFragment) fragment).exitFragment(); // 프래그먼트의 세부 동작 사항들을 종료시킨다.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = mBinding.getRoot();
        setContentView(view);

        Log.d(TAG, " \r\n*\r\n* *\r\n* * *\r\n* * * *\r\n* * * * *\r\n* * * * * *\r\n* * * * * * *\r\n* * * * * * * *\r\n* * * * * * * * *\n* * * * * * * * * *");
        Log.d(TAG, "액티비티가 실행되었습니다. -> onCreate()");

        if (mStatus == null)
        {
            mStatus = Status.instance();
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


        // TD2-SW-RC-UNIT-Test-ID-19 [DB 시스템 로그 쓰기 유닛] 순서[1] 시작.
        /*
        {
            UtilLog.instance.writeLog("테스트 메시지 1");
            UtilLog.instance.writeLog("테스트 메시지 2");
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-19 [DB 시스템 로그 쓰기 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-18 [DB 모든 시스템 로그 읽기 유닛] 순서[1] 시작.
        /*
        {
            List<EntityLog> logs = UtilLog.instance.readAllLogs();

            for (EntityLog log : logs)
            {
                Log.d(TAG, "index = " + log.number + ", data = " + log.date + ", message = " + log.message);
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-18 [DB 모든 시스템 로그 읽기 유닛] 순서[1] 끝.


        // 2) 사용자
        UtilUser.instance.open(getApplicationContext());


        // TD2-SW-RC-UNIT-Test-ID-6 [DB 사용자 정보 추가 유닛] 순서[1] 시작.
        /*
        {
            EntityUser testUserInfo1 = new EntityUser();
            testUserInfo1.name = "TEST-A_R";
            testUserInfo1.passKey = "1111";
            testUserInfo1.nickname = "테스트 A";
            testUserInfo1.ear = "R";
            testUserInfo1.defaultUser = "Y";

            EntityUser testUserInfo2 = new EntityUser();
            testUserInfo2.name = "TEST-B_L";
            testUserInfo2.passKey = "2222";
            testUserInfo2.nickname = "테스트 B";
            testUserInfo2.ear = "L";
            testUserInfo2.defaultUser = "N";

            UtilUser.instance.insert(testUserInfo1);
            UtilUser.instance.insert(testUserInfo2);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-6 [DB 사용자 정보 추가 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-3 [DB 모든 사용자 정보 읽기 유닛] 순서[1] 시작.
        /*
        {
            List<EntityUser> testUserInfos = UtilUser.instance.getUsers();

            for (EntityUser user : testUserInfos)
            {
                Log.d(TAG, "name = " + user.name + ", passKey = " + user.passKey + ", nickname = " + user.nickname + ", ear = " + user.ear + ", defaultUser = " + user.defaultUser);
            }

            UtilUser.instance.delete(testUserInfo1);
            UtilUser.instance.delete(testUserInfo2);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-3 [DB 모든 사용자 정보 읽기 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-4 [DB 기본 사용자 정보 읽기 유닛] 순서[1] 시작.
        /*
        {
            EntityUser testUserInfo1 = new EntityUser();
            testUserInfo1.name = "TEST-A_R";
            testUserInfo1.passKey = "1111";
            testUserInfo1.nickname = "테스트 A";
            testUserInfo1.ear = "R";
            testUserInfo1.defaultUser = "Y";

            EntityUser testUserInfo2 = new EntityUser();
            testUserInfo2.name = "TEST-B_L";
            testUserInfo2.passKey = "2222";
            testUserInfo2.nickname = "테스트 B";
            testUserInfo2.ear = "L";
            testUserInfo2.defaultUser = "N";

            UtilUser.instance.insert(testUserInfo1);
            UtilUser.instance.insert(testUserInfo2);

            EntityUser testDefaultUser = UtilUser.instance.getDefaultUser();

            Log.d(TAG, "name = " + testDefaultUser.name + ", passKey = " + testDefaultUser.passKey + ", nickname = " + testDefaultUser.nickname
                    + ", ear = " + testDefaultUser.ear + ", defaultUser = " + testDefaultUser.defaultUser);

            UtilUser.instance.delete(testUserInfo1);
            UtilUser.instance.delete(testUserInfo2);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-4 [DB 기본 사용자 정보 읽기 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-5 [DB 이름 기반 사용자 정보 읽기 유닛] 순서[1] 시작.
        /*
        {
            EntityUser testUserInfo = new EntityUser();
            testUserInfo.name = "TEST-A_R";
            testUserInfo.passKey = "1111";
            testUserInfo.nickname = "테스트 A";
            testUserInfo.ear = "R";
            testUserInfo.defaultUser = "Y";

            UtilUser.instance.insert(testUserInfo);

            EntityUser testUser = UtilUser.instance.getUserByName("TEST-A_R");

            if (testUser != null)
            {
                Log.d(TAG, "name = " + testUser.name + ", passKey = " + testUser.passKey + ", nickname = " + testUser.nickname
                        + ", ear = " + testUser.ear + ", defaultUser = " + testUser.defaultUser);
            }
            else
            {
                Log.d(TAG, "null");
            }

            UtilUser.instance.delete(testUserInfo);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-5 [DB 이름 기반 사용자 정보 읽기 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-5 [DB 이름 기반 사용자 정보 읽기 유닛] 순서[2] 시작.
        /*
        {
            EntityUser testUser = UtilUser.instance.getUserByName("TEST-B_L");
            if (testUser != null)
            {
                Log.d(TAG, "name = " + testUser.name + ", passKey = " + testUser.passKey + ", nickname = " + testUser.nickname
                        + ", ear = " + testUser.ear + ", defaultUser = " + testUser.defaultUser);
            }
            else
            {
                Log.d(TAG, "null");
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-5 [DB 이름 기반 사용자 정보 읽기 유닛] 순서[2] 끝.


        // TD2-SW-RC-UNIT-Test-ID-7 [DB 사용자 정보 업데이트 유닛] 순서[1] 시작.
        /*
        {
            List<EntityUser> users = UtilUser.instance.getUsers();

            for (EntityUser user : users)
            {
                if (user.name.equals("TEST-A_R"))
                {
                    user.nickname = "사용자 A";
                    UtilUser.instance.update(user);
                }
                else if (user.name.equals("TEST-B_L"))
                {
                    user.nickname = "사용자 B";
                    UtilUser.instance.update(user);
                }
            }

            users = UtilUser.instance.getUsers();

            for (EntityUser user : users)
            {
                Log.d(TAG, "name = " + user.name + ", passKey = " + user.passKey + ", nickname = " + user.nickname + ", ear = " + user.ear + ", defaultUser = " + user.defaultUser);

                if (user.name.equals("TEST-A_R"))
                {
                    UtilUser.instance.delete(user);
                }
                else if (user.name.equals("TEST-B_L"))
                {
                    UtilUser.instance.delete(user);
                }
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-7 [DB 사용자 정보 업데이트 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-8 [DB 사용자 정보 삭제 유닛] 순서[1] 시작.
        /*
        {
            List<EntityUser> users = UtilUser.instance.getUsers();

            for (EntityUser user : users)
            {
                if (user.name.equals("TEST-B_L"))
                {
                    user.nickname = "사용자 B";
                    UtilUser.instance.delete(user);
                }
            }

            users = UtilUser.instance.getUsers();

            for (EntityUser user : users)
            {
                Log.d(TAG, "name = " + user.name + ", passKey = " + user.passKey + ", nickname = " + user.nickname + ", ear = " + user.ear + ", defaultUser = " + user.defaultUser);

                if (user.name.equals("TEST-A_R"))
                {
                    UtilUser.instance.delete(user);
                }
                else if (user.name.equals("TEST-B_L"))
                {
                    UtilUser.instance.delete(user);
                }
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-8 [DB 사용자 정보 삭제 유닛] 순서[1] 끝.


        // 3) 사운드처리기
        UtilDevice.instance.open(getApplicationContext());


        // TD2-SW-RC-UNIT-Test-ID-13 [DB 기기 정보 추가 유닛] 순서[1] 시작.
        /*
        {
            EntityDevice testDeviceInfo1 = new EntityDevice();
            testDeviceInfo1.serialNumber = "A1B2";
            testDeviceInfo1.pairingKey = "123456";
            testDeviceInfo1.additionalInformation = "기기 A";

            EntityDevice testDeviceInfo2 = new EntityDevice();
            testDeviceInfo2.serialNumber = "A8B9";
            testDeviceInfo2.pairingKey = "654321";
            testDeviceInfo2.additionalInformation = "기기 B";

            UtilDevice.instance.insert(testDeviceInfo1);
            UtilDevice.instance.insert(testDeviceInfo2);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-13 [DB 기기 정보 추가 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-11 [DB 모든 기기 정보 읽기 유닛] 순서[1] 시작.
        /*
        {
            List<EntityDevice> testDeviceInfos = UtilDevice.instance.getDevices();

            for (EntityDevice device : testDeviceInfos)
            {
                Log.d(TAG, "serialNumber = " + device.serialNumber + ", pairingKey = " + device.pairingKey + ", additionalInformation = " + device.additionalInformation);

                if (device.serialNumber.equals("A1B2"))
                {
                    UtilDevice.instance.delete(device);
                }
                else if (device.serialNumber.equals("A8B9"))
                {
                    UtilDevice.instance.delete(device);
                }
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-11 [DB 모든 기기 정보 읽기 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-12 [DB 제품번호 기반 기기 정보 읽기 유닛] 순서[1] 시작.
        /*
        {
            EntityDevice testDeviceInfo = new EntityDevice();
            testDeviceInfo.serialNumber = "A1B2";
            testDeviceInfo.pairingKey = "123456";
            testDeviceInfo.additionalInformation = "기기 A";

            UtilDevice.instance.insert(testDeviceInfo);

            EntityDevice testDevice = UtilDevice.instance.getDeviceBySerialNumber("A1B2");

            if (testDevice != null)
            {
                Log.d(TAG, "serialNumber = " + testDevice.serialNumber + ", pairingKey = " + testDevice.pairingKey + ", additionalInformation = " + testDevice.additionalInformation);
            }
            else
            {
                Log.d(TAG, "null");
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-12 [DB 제품번호 기반 기기 정보 읽기 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-12 [DB 제품번호 기반 기기 정보 읽기 유닛] 순서[2] 시작.
        /*
        {
            EntityDevice testDevice = UtilDevice.instance.getDeviceBySerialNumber("A2B3");

            if (testDevice != null)
            {
                Log.d(TAG, "serialNumber = " + testDevice.serialNumber + ", pairingKey = " + testDevice.pairingKey + ", additionalInformation = " + testDevice.additionalInformation);
            }
            else
            {
                Log.d(TAG, "null");
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-12 [DB 제품번호 기반 기기 정보 읽기 유닛] 순서[2] 끝.


        // TD2-SW-RC-UNIT-Test-ID-14 [DB 기기 정보 업데이트 유닛] 순서[1] 시작.
        /*
        {
            List<EntityDevice> testDeviceInfos = UtilDevice.instance.getDevices();

            for (EntityDevice device : testDeviceInfos)
            {
                if (device.serialNumber.equals("A1B2"))
                {
                    device.additionalInformation = "외부기 A";
                    UtilDevice.instance.update(device);
                }
                else if (device.serialNumber.equals("A8B9"))
                {
                    device.additionalInformation = "외부기 B";
                    UtilDevice.instance.update(device);
                }
            }

            testDeviceInfos = UtilDevice.instance.getDevices();

            for (EntityDevice device : testDeviceInfos)
            {
                Log.d(TAG, "serialNumber = " + device.serialNumber + ", pairingKey = " + device.pairingKey + ", additionalInformation = " + device.additionalInformation);

                if (device.serialNumber.equals("A1B2"))
                {
                    UtilDevice.instance.delete(device);
                }
                else if (device.serialNumber.equals("A8B9"))
                {
                    UtilDevice.instance.delete(device);
                }
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-14 [DB 기기 정보 업데이트 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-15 [DB 기기 정보 삭제 유닛] 순서[1] 시작.
        /*
        {

            List<EntityDevice> testDeviceInfos = UtilDevice.instance.getDevices();

            for (EntityDevice device : testDeviceInfos)
            {
                if (device.serialNumber.equals("A1B2"))
                {
                    UtilDevice.instance.delete(device);
                }
            }

            testDeviceInfos = UtilDevice.instance.getDevices();

            for (EntityDevice device : testDeviceInfos)
            {
                Log.d(TAG, "serialNumber = " + device.serialNumber + ", pairingKey = " + device.pairingKey + ", additionalInformation = " + device.additionalInformation);

                if (device.serialNumber.equals("A1B2"))
                {
                    UtilDevice.instance.delete(device);
                }
                else if (device.serialNumber.equals("A8B9"))
                {
                    UtilDevice.instance.delete(device);
                }
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-15 [DB 기기 정보 삭제 유닛] 순서[1] 끝.


        // 4) 맵 정보
        UtilMap.instance.open(getApplicationContext());


        // TD2-SW-RC-UNIT-Test-ID-23 [DB 맵 정보 추가 유닛] 순서[1] 시작.
        /*
        {
            for (EntityMap map : UtilMap.instance.getAll())
            {
                UtilMap.instance.delete(map);
            }

            EntityMap testMapInfo1 = new EntityMap();
            testMapInfo1.name_ear = "AAAAA_R";
            testMapInfo1.stamp = 0;
            testMapInfo1.name = "AAAAA";
            testMapInfo1.ear = "R";
            testMapInfo1.serialize_map_data = "test";

            EntityMap testMapInfo2 = new EntityMap();
            testMapInfo2.name_ear = "BBBBB_L";
            testMapInfo2.stamp = 1;
            testMapInfo2.name = "BBBBB";
            testMapInfo2.ear = "L";
            testMapInfo2.serialize_map_data = "test";


            UtilMap.instance.insert(testMapInfo1);
            UtilMap.instance.insert(testMapInfo2);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-23 [DB 맵 정보 추가 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-22 [DB 모든 맵 정보 읽기 유닛] 순서[1] 시작.
        /*
        {
            for (EntityMap map : UtilMap.instance.getAll())
            {
                if (map.name_ear.equals("AAAAA_R") || map.name_ear.equals("BBBBB_L"))
                {
                    Log.d(TAG, "name_ear = " + map.name_ear + ", stamp = " + map.stamp
                            + ", name = " + map.name + ", ear = " + map.ear + ", serialize_map_data = " + map.serialize_map_data);
                }
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-22 [DB 모든 맵 정보 읽기 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-24 [DB 맵 정보 업데이트 유닛] 순서[1] 시작.
        /*
        {
            for (EntityMap map : UtilMap.instance.getAll())
            {
                if (map.name_ear.equals("AAAAA_R"))
                {
                    map.stamp = 2;
                    UtilMap.instance.update(map);
                }
                else if (map.name_ear.equals("BBBBB_L"))
                {
                    map.stamp = 3;
                    UtilMap.instance.update(map);
                }
            }

            for (EntityMap map : UtilMap.instance.getAll())
            {
                if (map.name_ear.equals("AAAAA_R") || map.name_ear.equals("BBBBB_L"))
                {
                    Log.d(TAG, "name_ear = " + map.name_ear + ", stamp = " + map.stamp
                            + ", name = " + map.name + ", ear = " + map.ear + ", serialize_map_data = " + map.serialize_map_data);
                }
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-24 [DB 맵 정보 업데이트 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-25 [DB 맵 정보 삭제 유닛] 순서[1] 시작.
        /*
        {
            for (EntityMap map : UtilMap.instance.getAll())
            {
                if (map.name_ear.equals("BBBBB_L"))
                {
                    UtilMap.instance.delete(map);
                }
            }

            for (EntityMap map : UtilMap.instance.getAll())
            {
                if (map.name_ear.equals("AAAAA_R") || map.name_ear.equals("BBBBB_L"))
                {
                    Log.d(TAG, "name_ear = " + map.name_ear + ", stamp = " + map.stamp
                            + ", name = " + map.name + ", ear = " + map.ear + ", serialize_map_data = " + map.serialize_map_data);
                }
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-25 [DB 맵 정보 삭제 유닛] 순서[1] 끝.


        // 상태 값 뷰 모델
        Log.d(TAG, "사운드처리기 상태 값 뷰모델 클래스를 불러옵니다.");
        mStatusViewModel = new ViewModelProvider(this).get(StatusViewModel.class);
        mStatusViewModel.setConnectionState(StatusViewModel.CONNECTION_STATE_DISCONNECTED);

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


        // TD2-SW-RC-UNIT-Test-ID-32 [사용설명서 상태 설정 유닛] 순서[1] 시작.
        /*
        {
            // onCreate에서 생성한 mManualScreen을 사용해서 테스트한다.
            mManualScreen.setEnable(true);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-32 [사용설명서 상태 설정 유닛] 순서[1] 끝.

        Log.d(TAG, "Manual screen enabled = " + mManualScreen.isEnabled());

        // TD2-SW-RC-UNIT-Test-ID-32 [사용설명서 상태 설정 유닛] 순서[2] 시작.
        /*
        {
            // onCreate에서 생성한 mManualScreen을 사용해서 테스트한다.
            mManualScreen.setEnable(false);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-32 [사용설명서 상태 설정 유닛] 순서[2] 끝.


        // TD2-SW-RC-UNIT-Test-ID-31 [사용설명서 화면 활성화 체크 유닛] 순서[1] 시작.
        /*
        {
            // onCreate에서 생성한 mManualScreen을 사용해서 테스트한다.
            Log.d(TAG, "Manual screen enabled = " + mManualScreen.isEnabled());
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-31 [사용설명서 화면 활성화 체크 유닛] 순서[1] 끝.


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


        // [맵 정보 모듈]의 유닛 테스트에서 공통으로 사용되는 인스턴스.
        /*
        MapInfo _testMapInfo;
        */

        // TD2-SW-RC-UNIT-Test-ID-35 [맵 정보 인스턴스 생성 및 초기화 유닛] 순서[1] 시작.
        /*
        {
            // [맵 정보 모듈]의 유닛 테스트에서 공통으로 사용되는 인스턴스 MapInfo _testMapInfo 사용.
            MapInfo testMapInfo = new MapInfo();

            Log.d(TAG, "testMapInfo = " + testMapInfo
                    + ", metadata = " + testMapInfo.metadata
                    + ", idUser = " + testMapInfo.idUser
                    + ", mapData = " + testMapInfo.mapData);

            Log.d(TAG, "metadata.isFilled = " + testMapInfo.metadata.isFilled
                    + ", metadata.names.length = " + testMapInfo.metadata.names.length
                    + ", metadata.ears.length = " + testMapInfo.metadata.ears.length
                    + ", metadata.stamps.length = " + testMapInfo.metadata.stamps.length);

            Log.d(TAG, "idUser.slotNum = " + testMapInfo.idUser.slotNum
                    + ", idUser.indexNum = " + testMapInfo.idUser.indexNum
                    + ", idUser.data.length = " + testMapInfo.idUser.data.length
                    + ", idUser.writing.length = " + testMapInfo.idUser.writing.length);

            for (int slot_i = 0; slot_i < 4; slot_i++)
            {
                Log.d(TAG, "idUser.data[" + slot_i + "].length = " + testMapInfo.idUser.data[slot_i].length
                        + ", idUser.writing[" + slot_i + "].length = " + testMapInfo.idUser.writing[slot_i].length);

                for (int index_i = 0; index_i < 3; index_i++)
                {
                    Log.d(TAG, "idUser.data[" + slot_i + "][" + index_i + "].length = " + testMapInfo.idUser.data[slot_i][index_i].length
                            + ", idUser.writing[" + slot_i + "][" + index_i + "].length = " + testMapInfo.idUser.writing[slot_i][index_i].length);
                }
            }

            Log.d(TAG, "mapData.slotNum = " + testMapInfo.mapData.slotNum
                    + ", mapData.mapNum = " + testMapInfo.mapData.mapNum
                    + ", mapData.indexNum = " + testMapInfo.mapData.indexNum
                    + ", mapData.data.length = " + testMapInfo.mapData.data.length
                    + ", mapData.writing.length = " + testMapInfo.mapData.writing.length);

            for (int slot_i = 0; slot_i < 4; slot_i++)
            {
                Log.d(TAG, "mapData.data[" + slot_i + "].length = " + testMapInfo.mapData.data[slot_i].length
                        + ", mapData.writing[" + slot_i + "].length = " + testMapInfo.mapData.writing[slot_i].length);

                for (int map_i = 0; map_i < 4; map_i++)
                {
                    Log.d(TAG, "mapData.data[" + slot_i + "][" + map_i + "].length = " + testMapInfo.mapData.data[slot_i][map_i].length
                            + ", mapData.writing[" + slot_i + "][" + map_i + "].length = " + testMapInfo.mapData.writing[slot_i][map_i].length);

                    for (int index_i = 0; index_i < 15; index_i++)
                    {
                        Log.d(TAG, "mapData.data[" + slot_i + "][" + map_i + "][" + index_i + "].length = " + testMapInfo.mapData.data[slot_i][map_i][index_i].length
                                + ", mapData.writing[" + slot_i + "][" + map_i + "][" + index_i + "].length = " + testMapInfo.mapData.writing[slot_i][map_i][index_i].length);
                    }
                }
            }

            _testMapInfo = testMapInfo;
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-35 [맵 정보 인스턴스 생성 및 초기화 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-38 [DB 맵 데이터를 맵 정보로 입력 유닛] 순서[1] 시작.
        /*
        {
            // [맵 정보 모듈]의 유닛 테스트에서 공통으로 사용되는 인스턴스 MapInfo _testMapInfo 사용.
            MapInfo testMapInfo = _testMapInfo;//new MapInfo();
            testMapInfo.metadata.isFilled = true;

            for (int slot_i = 0; slot_i < 4; slot_i++)
            {
                MapInfo.setMapInfoFromString(MapInfo.EMPTY_MAP_DATA, testMapInfo, slot_i);
            }

            for (int slot_i = 0; slot_i < 4; slot_i++)
            {
                for (int index_i = 0; index_i < 3; index_i++)
                {
                    Log.d(TAG, "idUser.data[" + slot_i + "][" + index_i + "] = " + MapInfo.bytesToString(testMapInfo.idUser.data[slot_i][index_i]));
                }
            }

            for (int slot_i = 0; slot_i < 4; slot_i++)
            {
                for (int map_i = 0; map_i < 4; map_i++)
                {
                    for (int index_i = 0; index_i < 15; index_i++)
                    {
                        Log.d(TAG, "mapData.data[" + slot_i + "][" + map_i + "][" + index_i + "] = " + MapInfo.bytesToString(testMapInfo.mapData.data[slot_i][map_i][index_i]));
                    }
                }
            }

            _testMapInfo = testMapInfo;
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-38 [DB 맵 데이터를 맵 정보로 입력 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-36 [맵 정보 메타데이터 업데이트 유닛] 순서[1] 시작.
        /*
        {
            // [맵 정보 모듈]의 유닛 테스트에서 공통으로 사용되는 인스턴스 MapInfo _testMapInfo 사용.
            MapInfo testMapInfo = _testMapInfo;//new MapInfo();
            testMapInfo.metadata.isFilled = true;
            testMapInfo.updateMetadata();

            for (int slot_i = 0; slot_i < 4; slot_i++)
            {
                Log.d(TAG, "name = " + testMapInfo.metadata.names[slot_i]
                        + ", ear = " + testMapInfo.metadata.ears[slot_i]
                        + ", stamp = " + testMapInfo.metadata.stamps[slot_i]);
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-36 [맵 정보 메타데이터 업데이트 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-39 [맵 정보를 DB 맵 데이터로 출력 유닛] 순서[1] 시작.
        /*
        {
            // [맵 정보 모듈]의 유닛 테스트에서 공통으로 사용되는 인스턴스 MapInfo _testMapInfo 사용.
            MapInfo testMapInfo = _testMapInfo;//new MapInfo();
            String[] dbMapData = new String[4];

            for (int slot_i = 0; slot_i < 4; slot_i++)
            {
                dbMapData[slot_i] = MapInfo.getStringFromMapInfo(testMapInfo, slot_i);

                Log.d(TAG, "Slot[" + slot_i + "] = " + dbMapData[slot_i]);
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-39 [맵 정보를 DB 맵 데이터로 출력 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-39 [맵 정보를 DB 맵 데이터로 출력 유닛] 순서[1] 시작.
        /*
        {
            // [맵 정보 모듈]의 유닛 테스트에서 공통으로 사용되는 인스턴스 MapInfo _testMapInfo 사용.
            MapInfo testMapInfo = _testMapInfo;
            testMapInfo.metadata.isFilled = true;
            testMapInfo.prepareWriting();

            for (int slot_i = 0; slot_i < 4; slot_i++)
            {
                for (int index_i = 0; index_i < 3; index_i++)
                {
                    Log.d(TAG, "idUser.writing[" + slot_i + "][" + index_i + "] = " + MapInfo.bytesToString(testMapInfo.idUser.writing[slot_i][index_i]));
                }
            }

            for (int slot_i = 0; slot_i < 4; slot_i++)
            {
                for (int map_i = 0; map_i < 4; map_i++)
                {
                    for (int index_i = 0; index_i < 15; index_i++)
                    {
                        Log.d(TAG, "mapData.writing[" + slot_i + "][" + map_i + "][" + index_i + "] = " + MapInfo.bytesToString(testMapInfo.mapData.writing[slot_i][map_i][index_i]));
                    }
                }
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-39 [맵 정보를 DB 맵 데이터로 출력 유닛] 순서[1] 끝.


    } // initMainActivity

    //
    // 장시간 미사용 이벤트 핸들러 관련
    //
    public void longTimeIdleHandlerUpdate(boolean enable)
    {
        if (enable)
        {
            /*
            if (mStatus.longTimeIdleState == Status.LONG_TIME_IDLE_STATE_NOT_TRIGGERED)
            {
            */
            mLongTimeIdleHandler.removeCallbacks(mLongTimeIdleRunner);
            mLongTimeIdleHandler.postDelayed(mLongTimeIdleRunner, LONG_TIME_IDLE_TIMEOUT_IN_MS); // 10분
            Log.v(TAG, "장시간 미사용 핸들러 업데이트 완료.");
            /*
            }
            */
        }
        else
        {
            mLongTimeIdleHandler.removeCallbacks(mLongTimeIdleRunner);
            Log.v(TAG, "장시간 미사용 핸들러 제거 완료.");
        }
    }

    private final Handler mLongTimeIdleHandler = new Handler();
    private final Runnable mLongTimeIdleRunner = () ->
    {
        //mStatus.longTimeIdleState = Status.LONG_TIME_IDLE_STATE_TRIGGERED;

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

        // 현재 화면에 출력되어 있는 다이얼로그가 있다면 제거한다.
        lastDialogDismiss();

        // 잠금화면 기능이 활성화되어 있다면 화면을 잠금화면으로 설정한다.
        // 만약 활성화되어 있다면 이 부분에서 다이얼로그들을 전부 제거하겠지만, 비활성화 상태를 생각하여 위에서 다이얼로그를 미리 제거했다.
        mLockScreen.resume();

        // 잠금화면 상태의 다이얼로그는 항상 최상위에 출력되어야 한다. 그래서 다이얼로그 객체를 따로 저장하지 않고,
        // 오직 확인 버튼을 눌러서만 제거가 가능하도록 구현한다.
        /*
        mStatus.longTimeIdleDialog = new MaterialAlertDialogBuilder(MainActivity.this).setMessage("전력소모를 줄이기 위해 절전모드로 진입하였습니다.").setPositiveButton("해제", (dialogInterface, i) ->
        {
            Log.d(TAG, "절전모드에서 빠져나옵니다.");

            mStatus.longTimeIdleState = Status.LONG_TIME_IDLE_STATE_NOT_TRIGGERED;

            // 잠금화면 기능이 활성화 중이라면, 암호를 입력하고 진입하기 때문에 리모컨 화면의 경우 검색도 자동으로 시작하게 된다.
            // 하지만 잠금화면 기능이 비활성화 상태라면 현재 프래그먼트가 리모컨 화면이 체크하고,
            // 리모컨 화면이라면 등록 여부를 검사하는 것으로 검색까지 자동으로 시작하게 한다.
            if (!mLockScreen.isEnabled())
            {
                Fragment remoteControlFragment = MainActivity.this.getSupportFragmentManager().findFragmentById(R.id.frame);

                if (remoteControlFragment instanceof RemoteControlFragment)
                {
                    Log.d(TAG, "잠금화면 기능이 비활성화 중인데, 현재 프래그먼트가 리모컨 화면입니다. 등록상황부터 다시 체크를 시작합니다.");
                    ((RemoteControlFragment) remoteControlFragment).checkRegisteredList();
                }
            }

            // 다시 장시간 미사용 핸들러를 시작한다.
            longTimeIdleHandlerUpdate(true);
        }).setCancelable(false).create();
        mStatus.longTimeIdleDialog.show();
        */
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
                boolean bluetooth_scan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
                boolean bluetooth_connect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

                if ((!access_fine_location) || (!bluetooth_scan) || (!bluetooth_connect))// || (!bluetooth_privileged))
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

                if ((!access_fine_location))// || (!bluetooth_privileged))
                {
                    Log.d(TAG, "권한 획득이 안된 항목이 있습니다." + "LOCATION=" + access_fine_location);
                    //+ ", PRIVILEGED=" + bluetooth_privileged);
                    UtilLog.instance.writeLog("앱 사용을 위한 위치 관련 권한 요청이 거부됨.");
                    finish();
                    return;
                }
                else
                {
                    UtilLog.instance.writeLog("앱 사용을 위한 위치 관련 권한이 획득됨.");
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            Log.d(TAG, "런타임 권한을 체크합니다. 버전코드가 S 이상입니다.");
            boolean bluetooth_scan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            boolean bluetooth_connect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            if ((!access_fine_location) || (!bluetooth_scan) || (!bluetooth_connect))// || (!bluetooth_privileged))
            {
                Log.d(TAG, "권한 획득이 안된 항목이 있습니다." + "LOCATION=" + access_fine_location
                        //+ ", PRIVILEGED=" + bluetooth_privileged
                        + ", SCAN=" + bluetooth_scan + ", CONNECT=" + bluetooth_connect);
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
                //Manifest.permission.BLUETOOTH_PRIVILEGED};
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE_NUMBER);
                UtilLog.instance.writeLog("앱 사용을 위한 블루투스 및 위치 관련 권한 요청.");
                return false;
            }
        }
        else
        {
            Log.d(TAG, "런타임 권한을 체크합니다. 버전코드가 S 미만입니다.");
            if ((!access_fine_location))// || (!bluetooth_privileged))
            {
                Log.d(TAG, "권한 획득이 안된 항목이 있습니다." + "LOCATION=" + access_fine_location);
                //+ ", PRIVILEGED=" + bluetooth_privileged);
                String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};//, Manifest.permission.BLUETOOTH_PRIVILEGED};
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE_NUMBER);
                UtilLog.instance.writeLog("앱 사용을 위한 위치 관련 권한 요청.");
                return false;
            }
        }

        return true;
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

                            mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                                    .setTitle("안내")
                                    .setMessage("페어링을 실패했습니다.")
                                    .setNegativeButton("재연결", (dialogInterface, i) ->
                                    {
                                        longTimeIdleHandlerUpdate(true);

                                        if (mStatus.activityRunningState == Status.ACTIVITY_RUNNING_STATE_FOREGROUND)
                                        {
                                            if (getSupportFragmentManager().findFragmentById(R.id.frame) instanceof RemoteControlFragment)
                                            {
                                                scanLe(true);
                                            }
                                        }
                                    })
                                    .setPositiveButton("확인", (dialogInterface, i) ->
                                    {
                                        longTimeIdleHandlerUpdate(true);
                                    })
                                    .setCancelable(false)
                                    .create();

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
        else if (fragment instanceof ShareFragment)
        {
            ((ShareFragment) fragment).exitFragment(); // 프래그먼트의 세부 동작 사항들을 종료시킨다.
            /*
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commitAllowingStateLoss();
            */
            replaceFragment(Status.TypeOfFragment.MENU);
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
    Handler mScanHandler = new Handler();
    Runnable mScanRunner = () ->
    {
        Log.v(TAG, "BLE 스캔 시간이 초과되었습니다.");
        scanLe(false); // 스캔 정지

        if (getSupportFragmentManager().findFragmentById(R.id.frame) instanceof ShareFragment)
        {
            if (mStatus.connectionState == Status.CONNECTION_STATE_DISCONNECTED)
            {
                scanLeWithDelay(true, 10);
            }
        }
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
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlBlurLayout.setVisibility(View.GONE);
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlSearchingAnimator.startRippleAnimation();
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
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlFindLayout.setVisibility(View.GONE);
                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlBlurLayout.setVisibility(View.VISIBLE);
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

            EntityUser defaultUser = UtilUser.instance.getDefaultUser();
            List<EntityDevice> devices = UtilDevice.instance.getDevices();
            EntityDevice targetDevice = null;

            if (!(getSupportFragmentManager().findFragmentById(R.id.frame) instanceof ShareFragment))
            {
                if (defaultUser == null || devices == null)
                {
                    // If no user or device available, return.
                    return;
                }
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

            Log.v(TAG, "BLE 스캔 결과 : 이름 = " + name + ", 스캔 응답 데이터 = " + serviceString);

            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);

            if (fragment instanceof ShareFragment)
            {
                ShareFragment shareFragment = (ShareFragment) fragment;

                String[] splits = serviceString.split(EntityUser.DELIMITER);
                String serial = splits[splits.length - 1];
                String ear = splits[0];
                StringBuilder user = new StringBuilder(splits[1]);

                for (int i = 2; i < splits.length - 1; i++)
                {
                    user.append(EntityUser.DELIMITER).append(splits[i]);
                }

                String nameWithEar = user + EntityUser.DELIMITER + ear;

                if (shareFragment.mFsm == ShareFragment.FSM_COLLECT_MAP_SCREEN)
                {
                    for (int i = 0; i < shareFragment.mCollectMapAdapter.getItemCount(); i++)
                    {
                        if (shareFragment.mCollectMapAdapter.getName(i).equals(nameWithEar))
                        {
                            if (!shareFragment.mCollectMapAdapter.isBleScanned(i))
                            {
                                shareFragment.mCollectMapAdapter.setBleScannedState(i, true);
                                shareFragment.mCollectMapAdapter.setOteSerial(i, serial);
                                shareFragment.mCollectMapAdapter.setBtDevice(i, result.getDevice());
                                break;
                            }
                        }
                    }
                } // End of FSM_COLLECT_MAP_SCREEN;
                else if (shareFragment.mFsm == ShareFragment.FSM_SHARE_MAP_SCREEN)
                {
                    if (shareFragment.mShareMapAdapter != null)
                    {
                        for (int i = 0; i < shareFragment.mCollectMapAdapter.getItemCount(); i++)
                        {
                            if (shareFragment.mCollectMapAdapter.isMapCollected(i) && shareFragment.mCollectMapAdapter.getName(i).equals(nameWithEar))
                            {
                                shareFragment.mShareMapAdapter.addItem(nameWithEar, serial, result.getDevice());
                            }
                        }
                    }
                } // End of FSM_SHARE_MAP_SCREEN;
                else if (shareFragment.mFsm == ShareFragment.FSM_MAP_RESET_DEFAULT_SCREEN)
                {
                    if (shareFragment.mMapResetAdapter != null)
                    {
                        boolean isAlreadyAdded = false;

                        for (int i = 0; i < shareFragment.mMapResetAdapter.getItemCount(); i++)
                        {
                            if (shareFragment.mMapResetAdapter.getNameWithEar(i).equals(nameWithEar) && shareFragment.mMapResetAdapter.getOteSerial(i).equals(serial))
                            {
                                isAlreadyAdded = true;
                                break;
                            }
                        }

                        if (!isAlreadyAdded)
                        {
                            shareFragment.mMapResetAdapter.addItem(nameWithEar, serial, result.getDevice());
                        }
                    }
                }
            } // End of FSM_MAP_RESET_DEFAULT_SCREEN;
            else if (getSupportFragmentManager().findFragmentById(R.id.frame) instanceof RemoteControlFragment)
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
    // 배터리 체크 패킷 전송 핸들러
    //
    public Handler mCheckBatteryHandler = new Handler();
    public Runnable mCheckBatteryRunner = () ->
    {
        Log.d(TAG, "연결된 사운드처리기의 배터리 정보를 업데이트 하기 위해 상태정보 획득 패킷을 전송합니다.");

        if (mBluetoothGatt != null && mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
        {
            sendPacket(packetMaker(PacketInfo.HEADER_SOUND_PROCESSOR_STATUS, null, 1));
            mCheckBatteryHandler.postDelayed(MainActivity.this.mCheckBatteryRunner, CHECK_BATTERY_DELAY_IN_MS);
        }
    };

    //
    // Discover services 시간초과 처리 핸들러
    //
    Handler mDiscoverServicesHandler = new Handler();
    Runnable mDiscoverServicesRunner = () ->
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
    Handler mCCCDHandler = new Handler();
    Runnable mCCCDRunner = () ->
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
    Handler mPasswordHandler = new Handler();
    Runnable mPasswordRunner = () ->
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
    Handler mDeviceAndMapInfoHandler = new Handler();
    Runnable mDeviceAndMapInfoRunner = () ->
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
    Handler mStatusHandler = new Handler();
    Runnable mStatusRunner = () ->
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
                String name = gatt.getDevice().getName();
                String address = gatt.getDevice().getAddress();

                // Connected state.
                if (newState == BluetoothProfile.STATE_CONNECTED)
                {
                    mStatus.connectionState = Status.CONNECTION_STATE_CONNECTING;

                    Log.d(TAG, "BLE 연결 이벤트 발생 -> NAME = " + name + ", ADDRESS = " + address);

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
                    mCheckBatteryHandler.removeCallbacks(mCheckBatteryRunner);
                    mStatusHandler.removeCallbacks(mStatusRunner);
                    mDeviceAndMapInfoHandler.removeCallbacks(mDeviceAndMapInfoRunner);
                    mPasswordHandler.removeCallbacks(mPasswordRunner);
                    mCCCDHandler.removeCallbacks(mCCCDRunner);
                    mDiscoverServicesHandler.removeCallbacks(mDiscoverServicesRunner);

                    if (mStatusViewModel != null)
                    {
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
                            else if (isCurrentFragmentShare())
                            {
                                getCurrentFragmentShare().disconnectedEventProcessor();
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
            String name = gatt.getDevice().getName();
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
            // run
            new Handler(Looper.getMainLooper()).post(() ->
            {
                // 수신 패킷 정보 획득
                byte[] responsePacket = characteristic.getValue(); // Extract data from packet.
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
                                gatt.disconnect();    // 연결 종료

                                // 이미 생성된 다이얼로그가 있다면, 그 다이얼로그를 종료하고 사용자 보안코드 재설정 다이얼로그를 생성해야 한다.
                                Log.d(TAG, "현재 생성된 다이얼로그가 있다면 종료하고, 사용자 내부기 키 재설정을 위한 다이얼로그를 새로 생성합니다.");

                                lastDialogDismiss();

                                mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                                        .setTitle("주의")
                                        .setMessage("내부기 키가 일치하지 않습니다. 재설정하시겠습니까?")
                                        .setPositiveButton("재설정", (dialogInterface, i) ->
                                        {
                                            longTimeIdleHandlerUpdate(true);

                                            //EntityUser user = UtilUser.instance.getDefaultUser();
                                            EntityUser user = mStatus.connectedUser;
                                            Bundle bundle = new Bundle();
                                            bundle.putString(EditUserFragment.ARG_NAME, user.name);
                                            bundle.putString(EditUserFragment.ARG_PASSKEY, user.passKey);
                                            bundle.putString(EditUserFragment.ARG_NICKNAME, user.nickname);
                                            bundle.putString(EditUserFragment.ARG_EAR, user.ear);
                                            bundle.putString(EditUserFragment.ARG_DEFAULT, user.defaultUser);
                                            /*
                                            EditUserFragment editUserFragment = new EditUserFragment();
                                            editUserFragment.setArguments(bundle);
                                            getSupportFragmentManager().beginTransaction().replace(mBinding.frame.getId(), editUserFragment).commitNowAllowingStateLoss();
                                            */
                                            replaceFragment(Status.TypeOfFragment.USER_EDIT, bundle);
                                        })
                                        .setNegativeButton("취소", (dialogInterface, i) ->
                                        {
                                            longTimeIdleHandlerUpdate(true);
                                        })
                                        .setCancelable(false)
                                        .create();

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

                        if (packetSize != PacketInfo.PACKET_SIZE_PROCESSOR_INFO)
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
                        sendPacket(packetMaker(PacketInfo.HEADER_SOUND_PROCESSOR_STATUS, null, 1));
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

                        Log.d(TAG, "사운드처리기 상태 확인 패킷 수신 : " + "배터리 = " + packetInfo.battery + ", " + "맵번호 = " + packetInfo.program + ", " + "볼륨 = " + packetInfo.volume + ", " + "최대출력 = " + packetInfo.maxOutput + ", " + "LED = " + packetInfo.led + ", " + "텔레코일 = " + packetInfo.telecoil + ", " + "자극알림 = " + packetInfo.notification);

                        mStatusViewModel.setValueBatteryLevel(packetInfo.battery);
                        mStatusViewModel.setValueNotification(packetInfo.notification);
                        mStatusViewModel.setValueLed(packetInfo.led);
                        mStatusViewModel.setValueTelecoil(packetInfo.telecoil);
                        mStatusViewModel.setValueMaxOutput(packetInfo.maxOutput);
                        mStatusViewModel.setValueVolume(packetInfo.volume);
                        mStatusViewModel.setValueProgram(packetInfo.program);

                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame);

                        // 현재 화면이 로그 화면일 때
                        if (fragment instanceof ShareFragment)
                        {
                            ShareFragment shareFragment = (ShareFragment) fragment;

                            shareFragment.mCountForConnectionFail = 0; // 연결 실패 카운터 초기화

                            shareFragment.whichPacketShouldBeTransferred();

                            UtilLog.instance.writeLog("연결 성공 : 장치이름=" + gatt.getDevice().getName());
                            Log.d(TAG, "사운드처리기와 BLE 통신이 온전하게 연결되었습니다.");
                            mStatus.connectionState = Status.CONNECTION_STATE_CONNECTED;

                            // 리모컨 화면의 옵저버를 위해 뷰모델 값을 업데이트한다.
                            mStatusViewModel.setConnectionState(StatusViewModel.CONNECTION_STATE_CONNECTED);
                        }
                        else
                        {
                            // 현재 BLE 연결중(CONNECTING)인 상태라면, 주기적인 배터리 상태 핸들러를 생성한다.
                            // 하지만 연결된(CONNECTED) 상태라면 핸들러를 생성하지 않는다.
                            if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTING)
                            {
                                UtilLog.instance.writeLog("연결 성공 : 장치이름=" + gatt.getDevice().getName());

                                Log.d(TAG, "사운드처리기와 BLE 통신이 온전하게 연결되었습니다.");
                                mStatus.connectionState = Status.CONNECTION_STATE_CONNECTED;
                                mCheckBatteryHandler.postDelayed(mCheckBatteryRunner, CHECK_BATTERY_DELAY_IN_MS);

                                // 리모컨 화면의 옵저버를 위해 뷰모델 값을 업데이트한다.
                                mStatusViewModel.setConnectionState(StatusViewModel.CONNECTION_STATE_CONNECTED);
                            }
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

                    // 맵 공유 관련 패킷들
                    case PacketInfo.HEADER_READ_ISD_ID_AND_USER:
                    case PacketInfo.HEADER_READ_MAP_DATA:
                    case PacketInfo.HEADER_WRITE_ISD_ID_AND_USER:
                    case PacketInfo.HEADER_WRITE_MAP_DATA:
                    case PacketInfo.HEADER_MAP_RESET_DEFAULT: // 맵 초기화 관련 패킷
                    {
                        if (isCurrentFragmentShare())
                        {
                            getCurrentFragmentShare().responsePacketProcessor(responsePacket);
                        }
                    }
                    break;

                    // 에러
                    case PacketInfo.HEADER_ERROR:
                    {
                        if (packetSize != PacketInfo.PACKET_SIZE_ERROR)
                        {
                            Log.d(TAG, "BLE 특성 변경 감지 -> 에러 패킷 사이즈 에러 : 사이즈 = " + packetSize);
                            UtilLog.instance.writeLog("패킷 에러 : 에러 패킷 사이즈 에러 (사이즈->" + packetSize + ")");

                            // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
                            packetSizeErrorDialog();
                            break;
                        }

                        byte errorType = byteExtractor(responsePacket[2]);

                        switch (errorType)
                        {
                            case 1: // 없는 명령
                            case 2: // 데이터 범위 이탈
                                Log.d(TAG, "패킷 위반 에러를 수신했습니다.");

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
                    break;
                } // switch
            }); // handler mainLooper
        } // onCharacteristicChanged
    }; // BluetoothGattCallback

    //
    // 패킷 사이즈 에러 다이얼로그
    //
    private void packetSizeErrorDialog()
    {
        // 패킷 사이즈 문제가 발생하면, 경고창을 출력하고 연결을 해제하여 재연결을 시도한다.
        lastDialogDismiss();

        mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle("에러")
                .setMessage("통신 에러가 발생했습니다. 앱을 다시 시작해주세요. 같은 에러가 반복되면 외부기를 다시 착용해주세요.")
                .setPositiveButton("확인", null)
                .setCancelable(false)
                .create();
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
    Handler mPacketResponseTimeoutHandler = new Handler();
    Runnable mPacketResponseTimeoutRunner = () ->
    {
        //
        // 패킷 응답 시간 초과일 때 오디오 입력 최대 신호 측정 중일 때를 위한 부분입니다.
        //
        Log.d(TAG, "패킷 응답 시간 초과 발생 -> 기기와 연결을 해제하겠습니다.");

        if (mBluetoothGatt != null)
        {
            if (mStatus.connectionState != Status.CONNECTION_STATE_DISCONNECTED && mStatus.connectionState != Status.CONNECTION_STATE_DISCONNECTING)
            {
                mBluetoothGatt.disconnect();
            }
        }
    }; // scanRunner

    //
    // 패킷 전송 핸들러 (너무 빠른 전송을 방지하기 위해 약 50ms의 딜레이를 생성하기 위함)
    //
    Handler mPacketSendHandler = new Handler();
    Runnable mPacketSendRunner = () ->
    {
        if (mBluetoothGatt == null || mStatus.sendingPacket == null)
        {
            Log.d(TAG, "패킷 전송 시도를 실패했습니다. -> GATT 객체가 null 이거나, 패킷 정보가 없습니다.");
            return;
        }

        boolean isSuccess = false;

        Log.v(TAG, "패킷 전송 : " + printLogBytesToString(mStatus.sendingPacket));

        Log.v(TAG, "패킷 응답 시간 초과 핸들러를 생성합니다.");
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

        mStatus.sendingPacket = null;
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
                mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("안내")
                        .setMessage("등록된 사용자가 없습니다. 먼저 사용자를 등록해주세요.")
                        .setPositiveButton("확인", (dialogInterface, i) ->
                        {
                            // 장시간 미사용 핸들러 업데이트
                            longTimeIdleHandlerUpdate(true);
                        })
                        .setCancelable(false)
                        .create();
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

                mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("사용자 목록")
                        .setPositiveButton("선택", (dialogInterface, i) ->
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
                                if (selectUser != null && selectUser.nickname != null && selectUser.nickname.length() > 0)
                                {
                                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(selectUser.nickname);
                                }
                                else if (selectUser != null)
                                {
                                    String name = UtilUser.getNameOnly(selectUser.name) + " (" + UtilUser.getEarKorean(selectUser.ear) + ")";

                                    ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(name);
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

                        })
                        .setSingleChoiceItems(nicknameList, mCheckItem, (dialogInterface, i) ->
                        {
                            longTimeIdleHandlerUpdate(true);
                            mCheckItem = i;
                        })
                        .setNegativeButton("취소", (dialogInterface, i) ->
                        {
                            longTimeIdleHandlerUpdate(true);
                        })
                        .setCancelable(false)
                        .create();
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
            mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("안내")
                    .setMessage("등록된 사용자가 없습니다. 먼저 사용자를 등록해주세요.")
                    .setPositiveButton("확인", (dialogInterface, i) ->
                    {
                        // 장시간 미사용 핸들러 업데이트
                        longTimeIdleHandlerUpdate(true);
                    })
                    .setCancelable(false)
                    .create();
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

            mStatus.lastDialog = new MaterialAlertDialogBuilder(MainActivity.this)
                    .setTitle("사용자 목록")
                    .setPositiveButton("선택", (dialogInterface, i) ->
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
                            if (selectUser != null && selectUser.nickname != null && selectUser.nickname.length() > 0)
                            {
                                ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(selectUser.nickname);
                            }
                            else if (selectUser != null)
                            {
                                String name = UtilUser.getNameOnly(selectUser.name) + " (" + UtilUser.getEarKorean(selectUser.ear) + ")";
                                ((RemoteControlFragment) fragment).mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(name);
                            }
                        }

                        if (mStatus.scanState == Status.SCAN_STATE_STOPPED)
                        {
                            MainActivity.this.scanLe(true);
                        }
                    })
                    .setSingleChoiceItems(nicknameList, mCheckItem, (dialogInterface, i) ->
                    {
                        // 장시간 미사용 핸들러 업데이트
                        longTimeIdleHandlerUpdate(true);
                        mCheckItem = i;
                    })
                    .setCancelable(false).create();
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

        mStatus.lastDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.lock_screen_dialog_title))
                .setMessage(getString(R.string.lock_screen_dialog_message))
                .setPositiveButton(getString(R.string.lock_screen_dialog_positive), (dialogInterface, i) ->
                {
                    // 장시간 미사용 핸들러 업데이트
                    longTimeIdleHandlerUpdate(true);

                    // 현재 연결 중인 사운드처리기가 있을 때
                    if (mStatus.connectionState != Status.CONNECTION_STATE_DISCONNECTED)
                    {
                        mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                        mBluetoothGatt.disconnect();
                    }

                    List<EntityUser> users = UtilUser.instance.getUsers();
                    List<EntityDevice> devices = UtilDevice.instance.getDevices();
                    List<EntityMap> maps = UtilMap.instance.getAll();

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

                    for (EntityMap map : maps)
                    {
                        UtilMap.instance.delete(map);
                        Log.d(TAG, "맵 데이터 " + map.name_ear + " " + map.stamp + " 삭제됨.");
                    }

                    mLockScreen.erasePassword();
                    //mLockScreen.setEnable(this, true);
                    mLockScreen.setEnable(true);
                    mLockScreen.resume();
                    mManualScreen.setEnable(true);

                    Bundle bundle = new Bundle();
                    bundle.putBoolean(ManualFragment.ARG_FIRST_SCREEN, true);
                    /*
                    ManualFragment manualFragment = new ManualFragment();
                    manualFragment.setArguments(bundle);
                    getSupportFragmentManager().beginTransaction().replace(R.id.frame, manualFragment).commitAllowingStateLoss();
                    */
                    replaceFragment(Status.TypeOfFragment.MANUAL, bundle);
                })
                .setNegativeButton(getString(R.string.lock_screen_dialog_negative), (dialogInterface, i) ->
                {
                    // 장시간 미사용 핸들러 업데이트
                    longTimeIdleHandlerUpdate(true);
                })
                .setCancelable(false)
                .create();
        mStatus.lastDialog.show();
    }

    //
    // 프래그먼트 비교기
    //
    public boolean isCurrentFragmentShare()
    {
        return getSupportFragmentManager().findFragmentById(R.id.frame) instanceof ShareFragment;
    }

    public ShareFragment getCurrentFragmentShare()
    {
        return (ShareFragment) getSupportFragmentManager().findFragmentById(R.id.frame);
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
        else if (typeOfFragment == Status.TypeOfFragment.SHARE_MAP)
        {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new ShareFragment()).commitAllowingStateLoss();
        }
        else
        {
            Log.d(TAG, "알 수 없는 프래그먼트 번호입니다. (" + typeOfFragment + ")");
            return;
        }

        mStatus.typeOfFragment = typeOfFragment;
    }
}