package todoc.cochlear.remoteapp.params;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import todoc.cochlear.remoteapp.database.AppDatabase;
import todoc.cochlear.remoteapp.database.Device;
import todoc.cochlear.remoteapp.database.devices.DatabaseDevices;
import todoc.cochlear.remoteapp.database.users.DatabaseUsers;
import todoc.cochlear.remoteapp.logging.LoggingDatabase;

public class AppParam
{
    // Instance and getter.
    private final static AppParam mInstance = new AppParam();

    public static AppParam getInstance()
    {
        return mInstance;
    }

    // Language indicator.
    public boolean isKorean;

    // For fragment number.
    public final static int FRAGMENT_NUMBER_NONE = 0;
    public final static int FRAGMENT_NUMBER_HOME = 1;
    public final static int FRAGMENT_NUMBER_LIST = 2;
    public final static int FRAGMENT_NUMBER_SEARCH = 3;
    public final static int FRAGMENT_NUMBER_PASSWORD = 4;
    public final static int FRAGMENT_NUMBER_USER = 5;
    public final static int FRAGMENT_NUMBER_MANAGEMENT_USER = 6;
    public final static int FRAGMENT_NUMBER_MANAGEMENT_DEVICE = 7;

    public int currentFragmentNumber;

    // For BLE connection state.
    public static final int BLE_CONNECTION_STATE_DISCONNECTED = 0;
    public static final int BLE_CONNECTION_STATE_CONNECTING = 1;
    public static final int BLE_CONNECTION_STATE_CONNECTED = 2;

    public int bleConnectionState;

    // For BLE transfer state.
    public boolean isBleBusy;

    // For BLE packet to send.
    public byte[] sendingPacket;

    // Currently connected Sound Processor BLE instance
    public Device currentConnectDevice;

    // Used for disconnecting when user want to disconnect from currently connected Sound Processor.
    public boolean isDisconnectedByUser;

    // BLE device is bonded?
    public boolean isBonded;

    // BLE scanner state.
    public boolean isBleScanning;

    // Shared Preference name
    public final static String SP_NAME = "sharedPreferences";

    // Auto connection shared preference name
    public final static String AUTO_CONN_SP_NAME = "autoConnection";

    public final static String AUTO_CONN_SP_YES = "yes";
    public final static String AUTO_CONN_SP_NO = "no";

    // Auto connection enabled?
    public boolean autoConnectionState;

    // 'callCounter' is indicator for knowing how many times app has been launched without memory release.
    public int callCounter;

    // For broadcast receiver.
    public boolean isBroadcastReceiverRegistered;

    // For termination service.
    public boolean isTerminationServiceStarted;

    // For exit app.
    public boolean willBeAppExit;

    // For Database.
    public AppDatabase database;

    // 사용자 정보 데이터베이스
    public DatabaseUsers databaseUsers;
    public DatabaseDevices databaseDevices;

    // For Logging database.
    public LoggingDatabase loggingDatabase;

    // For Toolbar.
    public String menuTitle;
    public boolean menuHome;
    public boolean menuSearch;
    public boolean menuList;
    public boolean menuAutoConnection;
    public boolean menuManual;
    public boolean menuSupport;
    public boolean menuUsers;
    public boolean menuSoundProcessors;

    // Lists for Sound Processor about registered on App and searched from Search Fragment.
    public List<Device> registeredDevices;
    public List<Device> searchedDevices;

    // Errors... used when App read Sound Processor status.
    public android.app.AlertDialog invalidValueDialog;

    // This flags are used to know whether toasts are already made or not when App get error packets.
    public boolean isEnabledInvalidPacketToast;
    public boolean isEnabledBusyToast;
    public boolean isEnabledUnlockedToast;
    public boolean isEnabledInternalErrorToast;

    // This value is used for testing for continuously send packets to Sound Processor.
    public byte testVolume;

    // This counter is used for resend a packet lastly sent to Sound Processor when App get a response packet that has invalid value range.
    // App immediately resend the packet lastly sent for maximum 5 times.
    // However if still the response packet has invalid value range, App will be make a error dialog to show this happen to user.
    public int resendCount;

    // Used for currently connected Sound Processor status.
    public DeviceParam currentStatusParams;

    // Used for sending a new status for Sound Processor using BLE packet.
    public DeviceParam sendingStatusParams;

    // 잠금화면을 위한 플래그
    public boolean isAppLockState;
    public boolean doesNeedToRegisterAppLockPassword;
    public boolean doesNeedToVerifyAppLockPassword;

    public String strFirstLockNum;      // 앱 잠금 비밀번호 등록시 첫번째 입력되는 비밀번호 (등록이 아닌, 일반적인 입력 상황에서도 사용됨)
    public String strSecondLockNum;     // 앱 잠금 비밀번호 등록시 두번째 입력되는 비밀번호 (첫번째 입력 값과 비교하는 용도)

    public int appLockNumIdx = 0;
    public String appLockNum1 = " ";
    public String appLockNum2 = " ";
    public String appLockNum3 = " ";
    public String appLockNum4 = " ";

    public android.app.AlertDialog lastDialog;

    // TODO: Live Data를 이용한 장시간 미사용 감지 테스트
    public MutableLiveData<Boolean> longTimeIdle;

    // 사용자 관리에 사용되는 뷰모델 데이터
    //public UserItem userItem;
    //public DeviceItem deviceItem;

    public AppParam()
    {
        callCounter = 0;
        currentFragmentNumber = FRAGMENT_NUMBER_NONE;

        isBroadcastReceiverRegistered = false;

        isTerminationServiceStarted = false;

        willBeAppExit = false;

        database = null;
        databaseUsers = null;
        databaseDevices = null;

        menuTitle = "";
        menuHome = false;
        menuSearch = false;
        menuList = false;
        menuManual = false;
        menuAutoConnection = false;
        menuSupport = false;
        menuUsers = false;
        menuSoundProcessors = false;

        isBonded = false;
        isBleScanning = false;
        isDisconnectedByUser = false;

        isEnabledInvalidPacketToast = false;
        isEnabledBusyToast = false;
        isEnabledUnlockedToast = false;
        isEnabledInternalErrorToast = false;

        searchedDevices = new ArrayList<>();

        resendCount = 0;
        currentStatusParams = new DeviceParam();
        sendingStatusParams = new DeviceParam();

        invalidValueDialog = null;

        isAppLockState = false;
        doesNeedToRegisterAppLockPassword = false;
        doesNeedToVerifyAppLockPassword = false;
        strFirstLockNum = "";
        strSecondLockNum = "";
        appLockNumIdx = 0;
        appLockNum1 = " ";
        appLockNum2 = " ";
        appLockNum3 = " ";
        appLockNum4 = " ";

        longTimeIdle = new MutableLiveData<Boolean>();

        //userItem = new UserItem();
        //deviceItem = new DeviceItem();
    }

    @Override
    public String toString()
    {
        return "AppParam{" +
                "isKorean=" + isKorean +
                ", currentFragmentNumber=" + currentFragmentNumber +
                ", bleConnectionState=" + bleConnectionState +
                ", isBleBusy=" + isBleBusy +
                ", sendingPacket=" + Arrays.toString(sendingPacket) +
                ", currentConnectDevice=" + currentConnectDevice +
                ", isDisconnectedByUser=" + isDisconnectedByUser +
                ", isBleScanning=" + isBleScanning +
                ", callCounter=" + callCounter +
                ", isBroadcastReceiverRegistered=" + isBroadcastReceiverRegistered +
                ", isTerminationServiceStarted=" + isTerminationServiceStarted +
                ", willBeAppExit=" + willBeAppExit +
                ", database=" + database +
                ", menuTitle='" + menuTitle + '\'' +
                ", menuHome=" + menuHome +
                ", menuSearch=" + menuSearch +
                ", menuList=" + menuList +
                ", menuAutoConnection=" + menuAutoConnection +
                ", menuManual=" + menuManual +
                ", menuSupport=" + menuSupport +
                ", registeredDevices=" + registeredDevices +
                ", searchedDevices=" + searchedDevices +
                ", invalidValueDialog=" + invalidValueDialog +
                ", isEnabledBusyToast=" + isEnabledBusyToast +
                ", isEnabledUnlockedToast=" + isEnabledUnlockedToast +
                ", isEnabledInternalErrorToast=" + isEnabledInternalErrorToast +
                ", testVolume=" + testVolume +
                ", resendCount=" + resendCount +
                ", currentStatusParams=" + currentStatusParams +
                ", sendingStatusParams=" + sendingStatusParams +
                '}';
    }

    public int getCurrentFragmentNumber()
    {
        return currentFragmentNumber;
    }

    public void setCurrentFragmentNumber(int mCurrentFragmentNumber)
    {
        this.currentFragmentNumber = mCurrentFragmentNumber;
    }

    public String getMenuTitle()
    {
        return menuTitle;
    }

    public void setMenuTitle(String mMenuTitle)
    {
        this.menuTitle = mMenuTitle;
    }

    public boolean getMenuHome()
    {
        return menuHome;
    }

    public void setMenuHome(boolean mMenuHome)
    {
        this.menuHome = mMenuHome;
    }

    public boolean getMenuSearch()
    {
        return menuSearch;
    }

    public void setMenuSearch(boolean mMenuSearch)
    {
        this.menuSearch = mMenuSearch;
    }

    public boolean getMenuList()
    {
        return menuList;
    }

    public void setMenuList(boolean mMenuList)
    {
        this.menuList = mMenuList;
    }

    public boolean getMenuAutoConnection()
    {
        return menuAutoConnection;
    }

    public void setMenuAutoConnection(boolean menuAutoConnection)
    {
        this.menuAutoConnection = menuAutoConnection;
    }

    public boolean getMenuManual()
    {
        return menuManual;
    }

    public void setMenuManual(boolean mMenuManual)
    {
        this.menuManual = mMenuManual;
    }

    public boolean getMenuSupport()
    {
        return menuSupport;
    }

    public void setMenuSupport(boolean mMenuSupport)
    {
        this.menuSupport = mMenuSupport;
    }

    public boolean isAutoConnectionEnabled()
    {
        return autoConnectionState;
    }

    public void setAutoConnectionEnabled(boolean autoConnectionEnabled)
    {
        autoConnectionState = autoConnectionEnabled;
    }
}
