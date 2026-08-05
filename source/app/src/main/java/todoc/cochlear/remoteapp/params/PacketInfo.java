package todoc.cochlear.remoteapp.params;

import androidx.annotation.NonNull;

public class PacketInfo
{
    // 패킷별 헤더 정보
    static public final byte HEADER_PASSWORD               = (byte) (0x40 & 0xff);
    static public final byte HEADER_SOUND_PROCESSOR_INFO   = (byte) (0x41 & 0xff);
    static public final byte HEADER_MAP_DATA_INFORMATION   = (byte) (0x42 & 0xff);
    static public final byte HEADER_SOUND_PROCESSOR_STATUS = (byte) (0x43 & 0xff);
    static public final byte HEADER_VALUE_PROMGRAM         = (byte) (0x44 & 0xff);
    static public final byte HEADER_VALUE_MAX_OUTPUT       = (byte) (0x45 & 0xff);
    static public final byte HEADER_VALUE_VOLUME           = (byte) (0x46 & 0xff);
    static public final byte HEADER_VALUE_TELECOIL         = (byte) (0x47 & 0xff);
    static public final byte HEADER_VALUE_NOTIFICATION     = (byte) (0x48 & 0xff);
    static public final byte HEADER_VALUE_LED              = (byte) (0x49 & 0xff);
    static public final byte HEADER_READ_ISD_ID_AND_USER   = (byte) (0x4A & 0xff);
    static public final byte HEADER_READ_MAP_DATA          = (byte) (0x4B & 0xff);
    static public final byte HEADER_WRITE_ISD_ID_AND_USER  = (byte) (0x4C & 0xff);
    static public final byte HEADER_WRITE_MAP_DATA         = (byte) (0x4D & 0xff);
    static public final byte HEADER_MAP_RESET_DEFAULT      = (byte) (0x50 & 0xff);
    static public final byte HEADER_SOUND_PROCESSING_PARAM = (byte) (0x52 & 0xff); // 0x52로 바뀜
    static public final byte HEADER_SPECIFIC_CMD           = (byte) (0x59 & 0xff); // 특수 시스템 동작 설정
    /* 부트(슬롯 상태 읽기 / 슬롯 선택)와 OTA 헤더.
     *
     * 사운드처리기 ble_communication.c 의 수신 분기가 아래 값으로 갈라진다.
     *   0xC2 : PKT_HEADER_BOOT            -> ci_ble_fetch_packet_boot()      (ci_ble_control_boot.h)
     *   0xC3 : CI_BLE_OTA_COMMAND_OTA     -> ci_ble_fetch_packet_ota()       (ci_ble_control_ota.h)
     *   0xC0 / 0xC1 : OTA START / END     -> ci_ble_fetch_packet_ota_start_end()
     *
     * 이전 값(0x80, 0x81)은 어느 분기에도 걸리지 않아 패킷이 통째로 무시됐다. */
    static public final byte HEADER_BOOT_STATUS            = (byte) (0xC2 & 0xff);
    static public final byte HEADER_OTA                    = (byte) (0xC3 & 0xff);
    static public final byte HEADER_ISD_ID                 = (byte) (0x98 & 0xff);
    static public final byte HEADER_SYSTEM_WARNING         = (byte) (0x99 & 0xff);
    static public final byte HEADER_ERROR                  = (byte) (0xf0 & 0xff);

    // 응답 패킷별 사이즈 정보
    static public final int PACKET_SIZE_PASSWORD                          = 2;
    static public final int PACKET_SIZE_PROCESSOR_INFO                    = 15;//9;
    static public final int PACKET_SIZE_MAP_DATA_INFO                     = 7;
    static public final int PACKET_SIZE_PROCESSOR_STATUS                  = 8;
    static public final int PACKET_SIZE_PROGRAM                           = 2;
    static public final int PACKET_SIZE_MAX_OUTPUT                        = 2;
    static public final int PACKET_SIZE_VOLUME                            = 2;
    static public final int PACKET_SIZE_TELECOIL                          = 2;
    static public final int PACKET_SIZE_NOTIFICATION                      = 2;
    static public final int PACKET_SIZE_LED                               = 2;
    static public final int PACKET_SIZE_AUDIO_INPUT_MAX_READ              = 15;
    static public final int PACKET_SIZE_SYSTEM_WARNING                    = 1;
    static public final int PACKET_SIZE_ISD_ID                            = 5;
    static public final int PACKET_SIZE_ERROR                             = 3;
    static public final int PACKET_SIZE_READ_ISD_ID_AND_USER_DATA_INDEX_1 = 20;
    static public final int PACKET_SIZE_READ_ISD_ID_AND_USER_DATA_INDEX_2 = 20;
    static public final int PACKET_SIZE_READ_ISD_ID_AND_USER_DATA_INDEX_3 = 12;

    static public final int PACKET_SIZE_SOUND_PROCESSING_PARAM_INDEX_1 = 13;
    static public final int PACKET_SIZE_SOUND_PROCESSING_PARAM_INDEX_2 = 20;
    static public final int PACKET_SIZE_SOUND_PROCESSING_PARAM_INDEX_3 = 16;
    static public final int PACKET_SIZE_SOUND_PROCESSING_PARAM_INDEX_4 = 18;
    static public final int PACKET_SIZE_SOUND_PROCESSING_PARAM_INDEX_5 = 18;
    static public final int PACKET_SIZE_SOUND_PROCESSING_PARAM_INDEX_6 = 18;
    static public final int PACKET_SIZE_SOUND_PROCESSING_PARAM_INDEX_7 = 18;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// OTA packet
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static public final int OTA_OPT_WRITE = 1;
    static public final int OTA_OPT_READ  = 2;
    static public final int OTA_OPT_SIZE  = 3;

    // Header=1, DataIndex=3, SlotNum=1, FileType=2, Option=1, TotalBytes=4, EndDataIndex=4, EndDataIndexByte=2
    // => 18 bytes (사운드처리기 ci_ble_control_ota.h 의 RECV_PKT_SIZE_OTA_WRITE 와 동일)
    static public final int PACKET_SIZE_OTA_SEND_COMMAND = 18;

    // Header=1, DataIndex=3, SlotNum=1, FileType=2, Option=1, Result=1
    // => 9 bytes (사운드처리기 ci_ble_control_ota.h 의 RESP_PKT_SIZE_OTA_WRITE 와 동일)
    static public final int PACKET_SIZE_OTA_RESP_COMMAND = 9;

    // 옵션 3(Size) 응답은 파일 크기 4바이트가 더 붙는다. (RESP_PKT_SIZE_OTA_SIZE)
    static public final int PACKET_SIZE_OTA_RESP_SIZE = 13;

    // 부트 패킷 : Info 는 [헤더, 옵션], Select 는 [헤더, 옵션, 슬롯번호]
    // 사운드처리기 ci_ble_control_boot.h 는 둘 다 3바이트로 정의한다.
    static public final int PACKET_SIZE_BOOT_SEND        = 3;
    static public final int PACKET_SIZE_BOOT_RESP_INFO   = 7;
    static public final int PACKET_SIZE_BOOT_RESP_SELECT = 3;

    static public final int BOOT_OPTION_INFO   = 1;
    static public final int BOOT_OPTION_SELECT = 2;

    // Header=1, DataIndex=3, Data=16 (단, 마지막 패킷에는 데이터가 16개가 아닐 수 있음)
    static public final int PACKET_SIZE_OTA_SEND_DATA = 20;

    // Header=1, DataIndex=3, PassFail=1
    // => 5 bytes
    static public final int PACKET_SIZE_OTA_RESP_DATA = 5;

    // Data=16 (단, 마지막 패킷에는 데이터가 16개가 아닐 수 있음)
    static public final int PACKET_SIZE_OTA_SEND_DATA_UNIT = 16;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 특수 시스템 동작 설정 (HEADER_SPECIFIC_CMD) - 게이팅(묵음) 제어
    ///
    /// 사운드처리기 remoteControl.c 의 case 1(읽기) / case 2(쓰기) 규격
    /// 쓰기 : [헤더, 2, 세부옵션1, 세부옵션2, T레벨 오프셋] => 5바이트
    ///        세부옵션1 = 1 (일반 모드), 세부옵션2 = 1(활성화) / 2(비활성화)
    /// 주의 : 비활성화(2) 일 때 펌웨어는 오프셋 값을 N/A 로 무시하고 기존 값을 유지한다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static public final int TX_PKT_OPT_SPECIFIC_CMD_GATING_READ  = 1;
    static public final int TX_PKT_OPT_SPECIFIC_CMD_GATING_WRITE = 2;
    static public final int TX_PKT_LEN_SPECIFIC_CMD_GATING_READ  = 2;
    static public final int TX_PKT_LEN_SPECIFIC_CMD_GATING_WRITE = 5;
    static public final int PACKET_SIZE_SPECIFIC_CMD_GATING      = 5;

    static public final int GATING_SUB_OPT_NORMAL_MODE = 1; // 세부옵션1 : 일반 모드
    static public final int GATING_ENABLE              = 1; // 세부옵션2 : 묵음 처리 활성화
    static public final int GATING_DISABLE             = 2; // 세부옵션2 : 묵음 처리 비활성화

    // 펌웨어 ci_stim_mute.h 의 CI_STIM_MUTE_T_LEVEL_OFFSET_DEFAULT 와 동일 (허용 범위 0~255)
    static public final int GATING_T_LEVEL_OFFSET_DEFAULT = 2;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 특수 시스템 동작 설정 (HEADER_SPECIFIC_CMD) - 링크 백텔 주기
    ///
    /// 사운드처리기 remoteControl.c 의 case 4 규격
    /// [헤더, 4, 주기] => 3바이트. 주기 0 = 읽기, 1~255 = 100msec 단위 설정
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static public final int TX_PKT_OPT_SPECIFIC_CMD_BACKTEL_PERIOD = 4;
    static public final int TX_PKT_LEN_SPECIFIC_CMD_BACKTEL_PERIOD = 3;
    static public final int PACKET_SIZE_SPECIFIC_CMD_BACKTEL       = 3;

    // 기본 백텔 주기 300msec (100msec 단위이므로 3)
    static public final int BACKTEL_PERIOD_DEFAULT_100MS = 3;
    static public final int BACKTEL_PERIOD_UNIT_MS       = 100;

    // 주기 값 0 을 실어 보내면 설정하지 않고 현재 값을 읽어온다.
    static public final int BACKTEL_PERIOD_READ = 0;

    /* 직접 선택(BT Select) 시 고를 수 있는 범위 : 100 ~ 1000msec.
     * 프로토콜 단위가 100msec 이므로 100msec 간격으로 고른다.
     * 펌웨어 자체 허용 범위는 1~255 (100msec ~ 25.5초) 로 더 넓다. */
    static public final int BACKTEL_PERIOD_SELECT_MIN_100MS = 1;
    static public final int BACKTEL_PERIOD_SELECT_MAX_100MS = 10;

    // 연결 직후 읽어오기 전의 미확인 상태
    static public final int LINK_VALUE_UNKNOWN = -1;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 특수 시스템 동작 설정 (HEADER_SPECIFIC_CMD) - 링크 Tx 파워 하한(PMIC)
    ///
    /// 사운드처리기 remoteControl.c 의 case 5(읽기) / case 6(쓰기) 규격
    /// 쓰기 : [헤더, 6, 레벨] => 3바이트. 레벨 1스텝 = 25mV
    /// 펌웨어 허용 범위는 72(1.800V) ~ 213(5.325V) 이다. (isd_interface.c)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static public final int TX_PKT_OPT_SPECIFIC_CMD_MIN_TX_PWR_READ  = 5;
    static public final int TX_PKT_OPT_SPECIFIC_CMD_MIN_TX_PWR_WRITE = 6;
    static public final int TX_PKT_LEN_SPECIFIC_CMD_MIN_TX_PWR_READ  = 2;
    static public final int TX_PKT_LEN_SPECIFIC_CMD_MIN_TX_PWR_WRITE = 3;
    static public final int PACKET_SIZE_SPECIFIC_CMD_MIN_TX_PWR      = 3;

    // 레벨 1스텝당 전압(mV). 표시용 환산에 쓴다.
    static public final int MIN_TX_PWR_LEVEL_STEP_MV = 25;

    /* 기본 링크 Tx 파워 하한 = 4.000V (25mV x 160)
     *
     * 참고: 펌웨어 driver_REN_ISL9122.h 의 부팅 기본값 MinTxPowerValue 는 164(4.100V)이며,
     *       그 주석("0.025*160=4V")은 값과 맞지 않는 오기다. 여기서는 4V 를 그대로 따른다. */
    static public final int MIN_TX_PWR_LEVEL_DEFAULT = 160;

    /* 직접 선택(PMIC Select) 시 고를 수 있는 레벨 범위.
     * 사운드처리기 isd_interface.c 의 tdc_is_valid_min_tx_power_level() 허용 범위와 같다.
     *   하한 72  = 25mV x 72  = 1.800V (ISL9122 VoltageSet 레지스터 하한)
     *   상한 213 = 25mV x 213 = 5.325V (MaxVoltageControlValue(214) - 1)
     * 이 범위를 벗어나면 펌웨어가 en__OutOfDataRange 에러로 거부한다. */
    static public final int MIN_TX_PWR_LEVEL_SELECT_MIN = 72;
    static public final int MIN_TX_PWR_LEVEL_SELECT_MAX = 213;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 특수 시스템 동작 설정 (HEADER_SPECIFIC_CMD) - 맵 데이터 강제 초기화
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 송신 : Header=1, Option=1, Type=1 => 3 bytes
    static public final int TX_PKT_OPT_SPECIFIC_CMD_MAP_INIT = 8;
    static public final int TX_PKT_LEN_SPECIFIC_CMD_MAP_INIT = 3;

    // 응답 : Header=1, Option=1, Type=1, MaxNumUser=1 => 4 bytes
    static public final int PACKET_SIZE_SPECIFIC_CMD_MAP_INIT = 4;

    // 맵 초기화 종류 (사운드처리기 remoteControl.c 의 case 8 규격)
    static public final int MAP_INIT_TYPE_MRI_16CH = 16;
    static public final int MAP_INIT_TYPE_MRI_32CH = 32;
    static public final int MAP_INIT_TYPE_DEFAULT  = 0xFF;

    static public final String MAP_INIT_NAME_MRI_16CH = "MRI 16채널 맵";
    static public final String MAP_INIT_NAME_MRI_32CH = "MRI 32채널 맵";
    static public final String MAP_INIT_NAME_DEFAULT  = "기본 맵(Map rollback)";

    // 맵 초기화 종류 값을 화면에 표시할 이름으로 변환한다.
    static public String mapInitTypeName(int mapInitType)
    {
        switch (mapInitType)
        {
            case MAP_INIT_TYPE_MRI_16CH:
                return MAP_INIT_NAME_MRI_16CH;

            case MAP_INIT_TYPE_MRI_32CH:
                return MAP_INIT_NAME_MRI_32CH;

            case MAP_INIT_TYPE_DEFAULT:
                return MAP_INIT_NAME_DEFAULT;

            default:
                return "알 수 없는 맵(" + mapInitType + ")";
        }
    }

    static public final int INIT_VALUE_BATTERY      = 0;
    static public final int INIT_VALUE_PROGRAM      = 1;
    static public final int INIT_VALUE_VOLUME       = 1;
    static public final int INIT_VALUE_MAX_OUTPUT   = 1;
    static public final int INIT_VALUE_LED          = 2;
    static public final int INIT_VALUE_NOTIFICATION = 2;
    static public final int INIT_VALUE_TELECOIL     = 2;

    static public final int LIMIT_MIN_BATTERY      = 0;
    static public final int LIMIT_MIN_PROGRAM      = 1;
    static public final int LIMIT_MIN_VOLUME       = 1;
    static public final int LIMIT_MIN_MAX_OUTPUT   = 1;
    static public final int LIMIT_MIN_LED          = 1;
    static public final int LIMIT_MIN_NOTIFICATION = 1;
    static public final int LIMIT_MIN_TELECOIL     = 1;

    static public final int LIMIT_MAX_BATTERY      = 100;
    static public final int LIMIT_MAX_PROGRAM      = 4;     // currently max : 4, but this value can be change dynamically during running app.
    static public final int LIMIT_MAX_VOLUME       = 10;
    static public final int LIMIT_MAX_MAX_OUTPUT   = 4;
    static public final int LIMIT_MAX_LED          = 2;
    static public final int LIMIT_MAX_NOTIFICATION = 2;
    static public final int LIMIT_MAX_TELECOIL     = 2;

    static public final byte PASSWORD_PASS = 1;
    static public final byte PASSWORD_FAIL = 2;

    static public final byte NOTIFICATION_ON  = 1;
    static public final byte NOTIFICATION_OFF = 2;

    static public final byte LED_ON  = 1;
    static public final byte LED_OFF = 2;

    static public final byte TELECOIL_ON  = 1;
    static public final byte TELECOIL_OFF = 2;

    static public final byte MAX_OUTPUT_UP   = 1;
    static public final byte MAX_OUTPUT_DOWN = 2;

    static public final byte VOLUME_UP   = 1;
    static public final byte VOLUME_DOWN = 2;

    static public final byte PROGRAM_UP   = 1;
    static public final byte PROGRAM_DOWN = 2;

    static public final int SLOT_MIN = 1;
    static public final int SLOT_MAX = 4;

    static public final int ID_AND_USER_INDEX_MIN = 1;
    static public final int ID_AND_USER_INDEX_MAX = 3;

    static public final int SOUND_PRECESSING_PARAM_INDEX_MIN = 1;
    static public final int SOUND_PRECESSING_PARAM_INDEX_MAX = 7;

    static public final int MAP_DATA_INDEX_MIN = 1;
    static public final int MAP_DATA_INDEX_MAX = 15;

    static public final int MAP_RESET_DEFAULT_OK = 1;

    public byte battery;
    public byte program;
    public byte volume;
    public byte maxOutput;
    public byte led;
    public byte notification;
    public byte telecoil;
    public byte error;
    public int  isdId;

    public PacketInfo()
    {
        battery = INIT_VALUE_BATTERY;
        program = INIT_VALUE_PROGRAM;
        volume = INIT_VALUE_VOLUME;
        maxOutput = INIT_VALUE_MAX_OUTPUT;
        led = INIT_VALUE_LED;
        notification = INIT_VALUE_NOTIFICATION;
        telecoil = INIT_VALUE_TELECOIL;
    }

    @NonNull
    @Override
    public String toString()
    {
        return "DeviceParam{" + "battery=" + battery + ", program=" + program + ", volume=" + volume + ", maxOutput=" + maxOutput + ", alarmLed=" + led + ", alarmStimulation=" + notification + ", telecoil=" + telecoil + '}';
    }
}
