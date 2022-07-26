package todoc.cochlear.remoteapp.params;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;

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

    static public final int CONNECTION_STATE_DISCONNECTED = 0;
    static public final int CONNECTION_STATE_CONNECTING = 1;
    static public final int CONNECTION_STATE_CONNECTED = 2;
    static public final int CONNECTION_STATE_DISCONNECTING = 3;

    static public final int SCAN_STATE_STOPPED = 0;
    static public final int SCAN_STATE_STARTED = 1;

    static public final int TRANSFER_STATE_IDLE = 0;
    static public final int TRANSFER_STATE_BUSY = 1;

    static public final int EXIT_CAPTURE_SERVICE_STATE_STOPPED = 0;
    static public final int EXIT_CAPTURE_SERVICE_STATE_STARTED = 1;

    static public final int LOCK_SCREEN_STATE_LOCK = 0;
    static public final int LOCK_SCREEN_STATE_TEMPORARY_UNLOCK = 1;

    public int connectionState;         // BLE 기기 연결 상태
    public int scanState;               // BLE 스캔 상태
    public int transferState;           // BLE 패킷 전송 상태
    public int exitCaptureState;        // 강제 종료 포착 서비스 상태
    public int lockScreenState;         // 일시적으로 잠금화면 기능을 해제한 상태
    public AlertDialog lastDialog;      // 앱에서 마지막에 출력된 다이얼로그 정보
    public EntityDevice connectedDevice;// 현재 연결을 시도하거나 연결중이거나 혹은 연결된 상태의 기기 정보
    public EntityUser connectedUser;    // 현재 연결을 시도하거나 연결중이거나 혹은 연결된 상태의 사용자 정보

    // BLE 패킷 전송에 사용되는 바이트 배열
    public byte[] sendingPacket;

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

    @NonNull
    @Override
    public String toString()
    {
        return "Status{" +
                "connectionState=" + connectionState +
                ", scanState=" + scanState +
                ", transferState=" + transferState +
                ", exitCaptureState=" + exitCaptureState +
                ", lockScreenState=" + lockScreenState +
                ", lastDialog=" + lastDialog +
                ", connectedDevice=" + connectedDevice +
                ", connectedUser=" + connectedUser +
                ", sendingPacket=" + Arrays.toString(sendingPacket) +
                ", isEnabledInvalidPacketToast=" + isEnabledInvalidPacketToast +
                ", isEnabledBusyToast=" + isEnabledBusyToast +
                ", isEnabledUnlockedToast=" + isEnabledUnlockedToast +
                ", isEnabledInternalErrorToast=" + isEnabledInternalErrorToast +
                '}';
    }
}
