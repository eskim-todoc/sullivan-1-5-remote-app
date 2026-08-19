package todoc.cochlear.remoteapp.params;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;

public class Status
{
    // Instance and getter.
    private final static Status _instance = new Status();

    public static Status instance()
    {
        return _instance;
    }

    static public final int CONNECTION_STATE_DISCONNECTED  = 0;
    static public final int CONNECTION_STATE_CONNECTING    = 1;
    static public final int CONNECTION_STATE_CONNECTED     = 2;
    static public final int CONNECTION_STATE_DISCONNECTING = 3;

    static public final int SCAN_STATE_STOPPED = 0;
    static public final int SCAN_STATE_STARTED = 1;

    static public final int TRANSFER_STATE_IDLE = 0;
    static public final int TRANSFER_STATE_BUSY = 1;

    static public final int EXIT_CAPTURE_SERVICE_STATE_STOPPED = 0;
    static public final int EXIT_CAPTURE_SERVICE_STATE_STARTED = 1;

    static public final int ACTIVITY_RUNNING_STATE_NOT_FOREGROUND = 0;
    static public final int ACTIVITY_RUNNING_STATE_FOREGROUND     = 1;

    static public final int LOCK_SCREEN_STATE_LOCK             = 0;
    static public final int LOCK_SCREEN_STATE_TEMPORARY_UNLOCK = 1;

    static public final int RESENDING_PACKET_COUNT = 3;
    /*
    static public final int LONG_TIME_IDLE_STATE_NOT_TRIGGERED = 0;
    static public final int LONG_TIME_IDLE_STATE_TRIGGERED = 1;
    */

    public int typeOfFragment;

    public int          connectionState;         // BLE 기기 연결 상태
    public int          scanState;               // BLE 스캔 상태
    public int          transferState;           // BLE 패킷 전송 상태
    public int          exitCaptureState;        // 강제 종료 포착 서비스 상태
    public int          activityRunningState;    // 액티비티 동작 상태
    public int          lockScreenState;         // 일시적으로 잠금화면 기능을 해제한 상태

    /* 사운드처리기 펌웨어의 0x59 프로토콜 세대. 연결 직후 앱이 «물어봐서» 알아낸다.
     *
     * 세대마다 0x59 가 다르고, 그 세대가 모르는 옵션을 보내면 에러로 응답하거나
     * 아예 응답하지 않아 연결이 끊긴다. 필드에 여러 세대가 섞여 있으므로
     * 사람이 고르게 하지 않고 되는 것을 찾을 때까지 위에서부터 시도한다.
     *
     *   REL4_PLUS : 버전 조회(옵션 255)가 읽힌다. 도메인 구조를 쓴다.
     *   REL4      : 버전은 못 읽지만 링크 일괄 읽기(구 옵션 17)가 응답한다.
     *               링크 인덱스 1~8 까지만 있다. 제어 모드와 상승 가속은 그 뒤에 생겼다.
     *   REL3      : 둘 다 안 되고 게이팅(옵션 1)만 살아 있다.
     *   UNKNOWN   : 게이팅조차 응답이 없다. 아직 판별 전이거나 연결이 끊긴 상태다.
     *
     * 값의 대소가 곧 세대의 신구다. 기능 지원 여부를 부등호로 판단할 수 있게 이렇게 뒀다. */
    static public final int FW_RELEASE_UNKNOWN = 0;
    static public final int FW_RELEASE_3       = 3;
    static public final int FW_RELEASE_4       = 4;
    static public final int FW_RELEASE_4_PLUS  = 5;

    public int          fwRelease = FW_RELEASE_UNKNOWN; // 판별된 펌웨어 세대

    /* REL4 기기가 실제로 아는 마지막 링크 인덱스.
     *
     * 그 세대는 버전 조회가 없어 무엇이 있는지 물어볼 방법이 없다. 대신 일괄 읽기
     * (구 옵션 17) 응답의 «값 개수» 가 그대로 답이다. 값은 인덱스 1부터 순서대로,
     * 뒤에만 늘어났기 때문이다.
     *
     *   값 7개 -> 인덱스 1~7  (one coin 까지. infinite 없음)
     *   값 8개 -> 인덱스 1~8  (infinite 추가)
     *   값 10개 -> 인덱스 1~10 (제어 모드 · 상승 가속 추가)
     *
     * 상수로 못박지 않는 이유가 이것이다. 같은 REL4 라도 시기마다 다르고,
     * 없는 옵션을 보내면 거절당하는데 앱은 그것을 미리 알 수 있다. */
    public int          rel4LinkIndexMax = 0; // 0 = 아직 일괄 읽기 전

    /* 사운드처리기 0x59 프로토콜 버전과 지원 옵션 비트맵.
     *
     * 연결 직후 옵션 255 로 물어서 채운다. 예전처럼 "옵션 17 이 읽히면 릴리즈4" 같은
     * 추정을 하지 않는다. 응답이 없거나 에러면 릴리즈 2~3 이하로 보고 묵음만 쓴다.
     *
     * 이 버전은 0x59 만 나타낸다. 펌웨어 전체 버전이 아니다. */
    public int          rcProtocolMajor = PacketInfo.LINK_VALUE_UNKNOWN;
    public int          rcProtocolMinor = PacketInfo.LINK_VALUE_UNKNOWN;
    public byte[]       rcOptionBitmap  = null;

    /*
    public int longTimeIdleState;       // 장시간 미사용 상태
    */
    public AlertDialog  lastDialog;      // 앱에서 마지막에 출력된 다이얼로그 정보
    /*
    public AlertDialog longTimeIdleDialog; // 장시간 미사용시 출력된 다이얼로그 정보
    */
    public EntityDevice connectedDevice;// 현재 연결을 시도하거나 연결중이거나 혹은 연결된 상태의 기기 정보
    public EntityUser   connectedUser;    // 현재 연결을 시도하거나 연결중이거나 혹은 연결된 상태의 사용자 정보

    // BLE 패킷 전송에 사용되는 바이트 배열
    public byte[]        sendingPacket;
    public Queue<byte[]> receivedPackets = new LinkedList<>();

    public int resendingCount;

    // BLE 에러 상황에서 출력하려는 각 토스트 메시지를 위한 플래그이다.
    // 연속적으로 에러 발생시 토스트 메시지가 계속 겹쳐서 생성되기 때문에 이를 방지하기 위함이다.
    public boolean isEnabledInvalidPacketToast;
    public boolean isEnabledBusyToast;
    public boolean isEnabledUnlockedToast;
    public boolean isEnabledInternalErrorToast;

    public Status()
    {
        isEnabledInvalidPacketToast = false;
        isEnabledBusyToast = false;
        isEnabledUnlockedToast = false;
        isEnabledInternalErrorToast = false;
    }

    @Override
    public String toString()
    {
        return "Status{" + "typeOfFragment=" + typeOfFragment + ", connectionState=" + connectionState + ", scanState=" + scanState + ", transferState=" + transferState + ", exitCaptureState=" + exitCaptureState + ", activityRunningState=" + activityRunningState + ", lockScreenState=" + lockScreenState + ", lastDialog=" + lastDialog + ", connectedDevice=" + connectedDevice + ", connectedUser=" + connectedUser + ", sendingPacket=" + Arrays.toString(sendingPacket) + ", isEnabledInvalidPacketToast=" + isEnabledInvalidPacketToast + ", isEnabledBusyToast=" + isEnabledBusyToast + ", isEnabledUnlockedToast=" + isEnabledUnlockedToast + ", isEnabledInternalErrorToast=" + isEnabledInternalErrorToast + '}';
    }

    static public class TypeOfFragment
    {
        static public final int NONE           = 0;
        static public final int REMOTE_CONTROL = 1;
        static public final int MENU           = 2;
        static public final int USER_LIST      = 3;
        static public final int USER_ADD       = 4;
        static public final int USER_EDIT      = 5;
        static public final int DEVICE_LIST    = 6;
        static public final int DEVICE_ADD     = 7;
        static public final int DEVICE_EDIT    = 8;
        static public final int MANUAL         = 9;
        static public final int HIDDEN_LOG     = 10;
    }
}
