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
    /* 매핑 연결 · 해제.
     *
     * 요청은 헤더 한 바이트뿐이고 응답은 [헤더, 1] 두 바이트다.
     * 사운드처리기 mappingControl.c 가 이 명령을 받으면 mappingProgramConnected 를 세워
     * 매핑 프로그램이 붙은 것으로 취급한다. OTA 전송은 그 상태에서 해야 한다. */
    static public final byte HEADER_MAPPING_CONNECT        = (byte) (0x60 & 0xff);
    static public final byte HEADER_MAPPING_DISCONNECT     = (byte) (0x61 & 0xff);
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
    static public final int PACKET_SIZE_MAPPING_CONNECT_SEND               = 1; // 헤더만
    static public final int PACKET_SIZE_MAPPING_CONNECT_RESP               = 2; // [헤더, 1]
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
    /* 에러 응답 [0xF0, 커맨드, 주에러, 부에러, 라인 상위, 라인 하위] 여섯 바이트.
     *
     * 사운드처리기 error.c 의 sendErrorToApp() 이 이 형태로 보낸다.
     * 예전에는 세 바이트로 알고 있었는데 실제로는 라인 번호까지 실려 온다.
     *
     * 앱이 쓰는 것은 앞의 세 바이트뿐이므로 그만큼만 있으면 해석할 수 있다.
     * 길이가 달라도 «에러 응답» 자체는 정상적인 응답이라 연결을 끊으면 안 된다.
     * 끊었더니 옵션 하나 거절당할 때마다 연결과 해제가 반복됐다. */
    static public final int PACKET_SIZE_ERROR                             = 6;
    static public final int PACKET_SIZE_ERROR_MIN                         = 3;
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
    /// 특수 시스템 동작 설정 (0x59) 프로토콜 — 릴리즈 4
    ///
    /// 사운드처리기 tdc_rc_protocol.h 와 docs/참고/리모콘 프로토콜/0x59 프로토콜 규격.md 가 정본이다.
    ///
    /// 파라미터마다 옵션 번호를 하나씩 쓰던 방식을 버리고, 도메인 하나에 옵션 하나를 두고
    /// 값은 데이터 인덱스로 고르는 구조로 바뀌었다. 파라미터가 늘어도 옵션 번호는 늘지 않는다.
    ///
    ///   요청 : [0x59, 옵션, 액세스, 인덱스, 값?]
    ///   응답 : [0x59, 옵션, 액세스, 인덱스, 값의 바이트 수 L, 값 L바이트]
    ///
    /// 응답 [4]가 개수가 아니라 바이트 수인 점에 주의한다. 링크 파라미터는 전부 1바이트라
    /// 둘이 같지만 배터리 텔레메트리는 인덱스 하나가 8바이트다.
    ///
    /// 쓰기 응답은 요청한 값이 아니라 반영된 값이다. 클램프나 정규화 결과가 돌아온다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // 옵션 (요청 [1])
    static public final int RC_OPT_MUTE_READ  = 1;    // 묵음 읽기 — 구형 포맷
    static public final int RC_OPT_MUTE_WRITE = 2;    // 묵음 쓰기 — 구형 포맷
    static public final int RC_OPT_LINK       = 0x20; // 32 : 링크 파라미터
    static public final int RC_OPT_STIM       = 0x21; // 33 : 자극 제어
    static public final int RC_OPT_BATTERY    = 0x22; // 34 : 배터리
    static public final int RC_OPT_VERSION    = 0xFF; // 255 : 프로토콜 버전 (읽기 전용)

    /* 폐기 대역 3~23. 릴리즈 4 개발 중 쓰이다 없어졌고 영구 재사용 금지다.
     * 이 번호를 보내면 사운드처리기가 에러로 답한다. */
    static public final int RC_OPT_RETIRED_LOW  = 3;
    static public final int RC_OPT_RETIRED_HIGH = 23;

    // 액세스 (요청 [2])
    static public final int RC_ACCESS_READ  = 0;
    static public final int RC_ACCESS_WRITE = 1;

    // 요청 패킷 오프셋과 길이
    static public final int RC_REQ_OFS_OPTION = 1;
    static public final int RC_REQ_OFS_ACCESS = 2;
    static public final int RC_REQ_OFS_INDEX  = 3;
    static public final int RC_REQ_OFS_VALUE  = 4;

    static public final int RC_REQ_LEN_READ  = 4; // 커맨드, 옵션, 액세스, 인덱스
    static public final int RC_REQ_LEN_WRITE = 5; // + 값 1바이트

    // 응답 패킷 오프셋
    static public final int RC_RSP_OFS_OPTION = 1;
    static public final int RC_RSP_OFS_ACCESS = 2;
    static public final int RC_RSP_OFS_INDEX  = 3;
    static public final int RC_RSP_OFS_LENGTH = 4;
    static public final int RC_RSP_OFS_VALUE  = 5;

    static public final int RC_RSP_HEADER_SIZE = 5;

    // 인덱스 0 은 모든 도메인에서 전체를 뜻한다. 읽기 전용이다.
    static public final int RC_IDX_ALL = 0;

    /* 링크 도메인 인덱스. 인덱스는 반드시 뒤에만 추가되므로 앞부분의 의미는 절대 바뀌지 않는다.
     * 그래서 전체 읽기 응답이 아는 것보다 길어도 뒤쪽만 무시하면 된다. */
    static public final int RC_LINK_IDX_CUR_TX_POWER      = 1;  // 현재 Tx 파워 (관찰값, 읽기 전용)
    static public final int RC_LINK_IDX_BACKTEL_PERIOD    = 2;  // 백텔 주기 (100msec 단위)
    static public final int RC_LINK_IDX_MIN_TX_POWER      = 3;  // 상시 Tx 파워 하한
    static public final int RC_LINK_IDX_MAPPING_MIN_POWER = 4;  // 매핑 전용 Tx 파워 하한
    static public final int RC_LINK_IDX_TX_POWER_STEP_UP  = 5;  // Tx 파워 상승 스텝
    static public final int RC_LINK_IDX_FORCE_TX_POWER    = 6;  // Tx 파워 강제 고정 (0 = 해제, 휘발)
    static public final int RC_LINK_IDX_ONE_COIN          = 7;  // 백텔 1회 실패 재시도
    static public final int RC_LINK_IDX_INFINITE_COIN     = 8;  // 백텔 무한 재시도 (휘발)
    static public final int RC_LINK_IDX_CTRL_MODE         = 9;  // 링크 제어 모드
    static public final int RC_LINK_IDX_TX_POWER_ACCEL    = 10; // 상승 가속
    static public final int RC_LINK_IDX_POWER_STABLE_NOP  = 11; // 백텔 읽기 직전 전원 안정용 NopStandby 개수
    static public final int RC_LINK_IDX_PULSE_WIDTH       = 12; // 현재 맵의 펄스폭 (usec, 읽기 전용)
    static public final int RC_LINK_IDX_FRAME_NUM         = 13; // 패킷 수 = 채널당 프레임 수 (읽기 전용)
    static public final int RC_LINK_IDX_NOP_ENABLE        = 14; // 인덱스 11 을 리모콘이 정했는지 (0 을 쓰면 기본값 복귀)
    static public final int RC_LINK_IDX_STIM_STRATEGY     = 15; // 자극 전략 (읽기 전용)
    static public final int RC_LINK_IDX_MAX_TX_POWER      = 16; // Tx 파워 상한 (4.5+, 전체 읽기에 안 실린다)

    /* 자극 레벨 피드포워드 (4.5 후반 추가). 전부 개별 읽기다.
     *
     * CFX 가 자극 레벨 합의 급증을 보면 플래그를 세우고 CM3 가 PMIC 를 올린다.
     * 백텔(300 msec)보다 먼저 반응해 «조용하다 큰 소리» 에 내부기가 죽는 것을 막는다.
     * 올리기만 하고 내리는 것은 백텔 High 판정에 맡긴다. */
    static public final int RC_LINK_IDX_FF_ENABLE   = 17; // 사용 (0 · 1)
    static public final int RC_LINK_IDX_FF_COOLDOWN = 18; // 쿨다운 (msec)
    static public final int RC_LINK_IDX_FF_STEP     = 19; // 한 번에 올리는 스텝 (25mV 단위)
    static public final int RC_LINK_IDX_FF_RATIO_0  = 20; // 비율 구간 0 — 채널당 평균 0~63
    static public final int RC_LINK_IDX_FF_RATIO_1  = 21; //            1 — 64~127
    static public final int RC_LINK_IDX_FF_RATIO_2  = 22; //            2 — 128~191
    static public final int RC_LINK_IDX_FF_RATIO_3  = 23; //            3 — 192~255
    static public final int RC_LINK_IDX_FF_RAISED   = 24; // (R) CFX 가 플래그를 세운 횟수, 하위 8비트
    static public final int RC_LINK_IDX_FF_APPLIED  = 25; // (R) CM3 가 올린 횟수, 하위 8비트
    static public final int RC_LINK_IDX_FF_AVG_AMP  = 26; // (R) 현재 채널당 평균 amplitude

    /* 이 앱이 아는 마지막 인덱스.
     *
     * 전체 읽기 파서의 상한으로도 쓰이는데, 4.5 의 전체 읽기 응답은 여전히 «값 15개» 다
     * (인덱스 16 은 20바이트 상한 때문에 개별 읽기 전용이다). 파서가 min(응답 길이, 이 값)
     * 으로 자르므로 16 으로 올려도 전체 읽기 동작은 그대로다. */
    static public final int RC_LINK_IDX_MAX = 26; // 이 앱이 아는 마지막 인덱스

    /* 전체 읽기 응답에 실리는 마지막 인덱스. 인덱스 16 부터는 개별 읽기로만 온다. */
    static public final int RC_LINK_IDX_ALL_READ_MAX = 15;

    /* REL4 세대가 최소한 아는 마지막 인덱스.
     *
     * REL4 는 시기마다 아는 범위가 다르다. 일괄 읽기(구 옵션 17)가 처음 생겼을 때는
     * one coin 까지 값 7개였고, 뒤에 infinite coin 이 붙어 8개, 다시 제어 모드와
     * 상승 가속이 붙어 10개가 됐다.
     *
     * 그래서 실제 상한은 일괄 읽기 응답의 값 개수로 «관찰» 한다 (Status.rel4LinkIndexMax).
     * 이 상수는 아직 읽기 전에 쓰는 보수적인 기본값이다. one coin 까지는 어느 REL4 에나 있다. */
    static public final int RC_LINK_IDX_REL4_MIN = 7;

    // 자극 도메인 인덱스
    static public final int RC_STIM_IDX_MAP_INIT = 1; // 맵 강제 초기화 (쓰기 전용)

    // 배터리 도메인 인덱스
    static public final int RC_BATTERY_IDX_TELEMETRY = 1; // 텔레메트리 8바이트 (읽기 전용)

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 옵션 255 — 프로토콜 버전
    ///
    /// 요청 : [0x59, 0xFF, 0]  (3바이트. 규격서 예제가 이 형태다)
    /// 응답 : [0x59, 0xFF, 0, 0, 10, major, minor, 비트맵 8바이트]
    ///
    /// 응답이 없거나 에러면 릴리즈 2~3 이하이며 묵음(옵션 1, 2)만 쓸 수 있다.
    /// 이 버전은 0x59 만 나타낸다. 펌웨어 전체 버전이 아니다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static public final int RC_REQ_LEN_VERSION      = 3;
    static public final int RC_VERSION_VALUE_LEN    = 10; // major 1 + minor 1 + 비트맵 8
    static public final int RC_VERSION_BITMAP_BYTES = 8;

    static public final int RC_VERSION_OFS_MAJOR  = RC_RSP_OFS_VALUE;     // [5]
    static public final int RC_VERSION_OFS_MINOR  = RC_RSP_OFS_VALUE + 1; // [6]
    static public final int RC_VERSION_OFS_BITMAP = RC_RSP_OFS_VALUE + 2; // [7]

    // 이 앱이 요구하는 최소 프로토콜 세대. 이보다 낮으면 링크 도메인을 쓰지 않는다.
    static public final int RC_PROTOCOL_MAJOR_REQUIRED = 4;

    // 전원 안정 Nop(링크 인덱스 11)이 생긴 버전. 4.1 미만에는 그 인덱스가 없다.
    static public final int RC_PROTOCOL_MINOR_NOP = 1;

    /* 4.2 : 펄스폭(12) · 패킷 수(13) · Nop enable(14) 신설.
     *       이때부터 인덱스 11 이 «패킷 수별 기본값» 을 갖고 휘발성이 된다.
     * 4.3 : 자극 전략(15) 신설. */
    static public final int RC_PROTOCOL_MINOR_NOP_TABLE = 2;
    static public final int RC_PROTOCOL_MINOR_STRATEGY  = 3;

    /* 4.4 : 재시도 코인(인덱스 7)이 «켜고 끄는 스위치» 에서 «개수» 가 됐다.
     *
     * 인덱스가 새로 생긴 것이 아니라 «뜻» 이 넓어진 것이라 위의 것들과 성격이 다르다.
     * 그래서 isLinkParamSupported() 의 인덱스 게이트가 아니라 별도 판정으로 쓴다.
     * 0 과 1 의 해석은 그대로 보존되므로 구버전 앱이 쓰던 값이 같은 동작을 낸다. */
    static public final int RC_PROTOCOL_MINOR_COIN_COUNT = 4;

    /* 4.5 — Tx 파워 상한(인덱스 16)이 생겼다. 포맷은 안 바뀌어 minor 만 올랐다. */
    static public final int RC_PROTOCOL_MINOR_MAX_TX_POWER = 5;

    // 옵션 n 의 지원 여부는 비트맵[n / 8] 의 비트 n % 8 이다. 비트맵은 옵션 0~63 만 표현한다.
    static public boolean isOptionSupported(byte[] bitmap, int option)
    {
        int byteIndex = option >> 3;

        if (bitmap == null || byteIndex < 0 || bitmap.length <= byteIndex)
        {
            return false;
        }

        return ((bitmap[byteIndex] & (1 << (option & 0x07))) != 0);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 묵음(게이팅) — 옵션 1, 2. 릴리즈 2~3 유산이라 이 둘만 구형 포맷을 쓴다.
    ///
    /// 읽기 : [0x59, 1]                                   응답 5바이트
    /// 쓰기 : [0x59, 2, 세부1, 세부2, T레벨 오프셋]        응답 5바이트
    ///        세부1 = 1(일반 모드), 세부2 = 1(활성화) / 2(비활성화)
    ///
    /// 새 체계로 옮기면 필드의 구형 앱이 깨지므로 통합하면 안 된다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static public final int RC_REQ_LEN_MUTE_READ  = 2;
    static public final int RC_REQ_LEN_MUTE_WRITE = 5;
    static public final int RC_RSP_LEN_MUTE       = 5;

    static public final int MUTE_SUB_OPT_NORMAL_MODE = 1; // 세부1 : 일반 모드
    static public final int MUTE_ENABLE              = 1; // 세부2 : 묵음 처리 활성화
    static public final int MUTE_DISABLE             = 2; // 세부2 : 묵음 처리 비활성화

    static public final int MUTE_RSP_OFS_STATE  = 3; // 응답의 활성화 상태 위치
    static public final int MUTE_RSP_OFS_OFFSET = 4; // 응답의 T레벨 오프셋 위치

    // 펌웨어 ci_stim_mute.h 의 CI_STIM_MUTE_T_LEVEL_OFFSET_DEFAULT (허용 범위 0~255)
    static public final int MUTE_T_LEVEL_OFFSET_DEFAULT = 2;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 구 프로토콜 (릴리즈 3) — 파라미터 하나에 옵션 번호 하나
    ///
    /// 릴리즈 4 가 이 옵션들(3~23)을 폐기하고 도메인 구조로 갈아엎었다. 그래서 이 정의는
    /// 릴리즈 4 «이전» 펌웨어에만 쓴다. 필드에 그 세대가 남아 있어 앱이 둘 다 말할 줄 알아야 한다.
    ///
    ///   읽기 요청 : [0x59, 옵션]           쓰기 요청 : [0x59, 옵션, 값]
    ///   응답      : [0x59, 옵션, 값]       (3바이트. 쓰기 응답의 옵션은 쓰기 옵션 번호다)
    ///
    /// 일괄 읽기(옵션 17)만 응답이 길다.
    ///   [0x59, 17, 값 10개]  (총 12바이트)
    ///
    /// 그 값 10개의 순서는 릴리즈 4 링크 인덱스 1~10 과 정확히 같다. 릴리즈 4 가 그렇게
    /// 맞춰 설계했다. 덕분에 파싱을 applyLinkParam() 하나로 공유할 수 있다.
    ///
    /// 다만 세대마다 길이가 다르다. one coin 까지면 9, infinite 까지면 10, 제어 모드와
    /// 가속까지면 12바이트다. 뒤에만 늘어났으므로 받은 만큼만 읽으면 된다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static public final int LEGACY_OPT_NONE = -1; // 이 세대에 없는 파라미터

    /* 응답을 구형 포맷으로 읽어야 하는지.
     *
     * 구 옵션은 3 ~ 23 이고 릴리즈 4 도메인은 0x20 부터라 두 대역이 겹치지 않는다.
     * 그래서 옵션 번호 하나로 포맷이 정해진다. 판별이 끝나기 전(세대 미상)에도
     * 옳게 갈리므로, 세대 값을 보고 판단하면 안 된다.
     *
     * 실제로 그렇게 했다가, 버전 응답이 오는 시점에는 아직 세대가 미상이라
     * 버전 응답을 구형으로 읽어 버리는 문제가 있었다. */
    static public boolean isLegacyFormatOption(int option)
    {
        return (option < RC_OPT_LINK);
    }

    static public final int LEGACY_OPT_READ_ALL   = 17; // 링크 파라미터 일괄 읽기
    static public final int LEGACY_OPT_MAP_INIT   = 8;  // 맵 강제 초기화 (쓰기 전용)

    static public final int LEGACY_REQ_LEN_READ  = 2;
    static public final int LEGACY_REQ_LEN_WRITE = 3;

    static public final int LEGACY_RSP_OFS_OPTION = 1;
    static public final int LEGACY_RSP_OFS_VALUE  = 2;
    static public final int LEGACY_RSP_LEN_SINGLE = 3;

    // 일괄 읽기 응답에서 값이 시작하는 위치와, 이 앱이 아는 값의 최대 개수
    static public final int LEGACY_ALL_OFS_VALUE = 2;
    static public final int LEGACY_ALL_MAX_COUNT = 10;

    // 맵 초기화 요청은 [0x59, 8, 채널수] 3바이트다.
    static public final int LEGACY_REQ_LEN_MAP_INIT = 3;

    /* 릴리즈 4 링크 인덱스 -> 구 옵션 번호 대응표.
     *
     * 배열의 자리가 곧 인덱스다. 0번 자리(전체)는 옵션 17 이 따로 있으므로 비워 둔다.
     * 백텔 주기(인덱스 2)만 읽기와 쓰기가 옵션 4 하나를 공유한다. 값 0 을 보내면 읽기다.
     * 현재 Tx 파워(인덱스 1)는 관찰값이라 쓰기가 없다.
     * 전원 안정 Nop(인덱스 11)은 릴리즈 4.1 에서 생겨 구 옵션이 아예 없다. */
    static private final int[] LEGACY_READ_OPTION = {
            LEGACY_OPT_NONE, //  0 전체 (옵션 17 을 따로 쓴다)
            3,               //  1 현재 Tx 파워
            4,               //  2 백텔 주기 (값 0 = 읽기)
            5,               //  3 상시 Tx 파워 하한
            9,               //  4 매핑 Tx 파워 하한
            11,              //  5 Tx 파워 상승 스텝
            13,              //  6 Tx 파워 강제 고정
            15,              //  7 one coin
            18,              //  8 infinite coin
            20,              //  9 링크 제어 모드
            22,              // 10 상승 가속
            LEGACY_OPT_NONE, // 11 전원 안정 Nop (릴리즈 4.1 신설)
    };

    static private final int[] LEGACY_WRITE_OPTION = {
            LEGACY_OPT_NONE, //  0 전체는 읽기 전용
            LEGACY_OPT_NONE, //  1 현재 Tx 파워는 관찰값이라 쓰기가 없다
            4,               //  2 백텔 주기 (읽기와 같은 옵션)
            6,               //  3 상시 Tx 파워 하한
            10,              //  4 매핑 Tx 파워 하한
            12,              //  5 Tx 파워 상승 스텝
            14,              //  6 Tx 파워 강제 고정
            16,              //  7 one coin
            19,              //  8 infinite coin
            21,              //  9 링크 제어 모드
            23,              // 10 상승 가속
            LEGACY_OPT_NONE, // 11 전원 안정 Nop (릴리즈 4.1 신설)
    };

    // 릴리즈 4 링크 인덱스에 대응하는 구 옵션 번호. 없으면 LEGACY_OPT_NONE 이다.
    static public int getLegacyReadOption(int linkIndex)
    {
        return (linkIndex < 0 || LEGACY_READ_OPTION.length <= linkIndex) ? LEGACY_OPT_NONE : LEGACY_READ_OPTION[linkIndex];
    }

    static public int getLegacyWriteOption(int linkIndex)
    {
        return (linkIndex < 0 || LEGACY_WRITE_OPTION.length <= linkIndex) ? LEGACY_OPT_NONE : LEGACY_WRITE_OPTION[linkIndex];
    }

    /* 구 옵션 번호 -> 릴리즈 4 링크 인덱스. 응답을 받았을 때 쓴다.
     * 읽기 옵션과 쓰기 옵션 둘 다 같은 인덱스로 돌려준다. 응답이 어느 쪽이든 값의 의미는 같다. */
    static public int getLinkIndexFromLegacyOption(int legacyOption)
    {
        /* 대응표의 빈칸도 LEGACY_OPT_NONE 이라, 이 검사가 없으면 빈칸에 걸려
         * 엉뚱한 인덱스를 돌려준다. */
        if (legacyOption == LEGACY_OPT_NONE)
        {
            return LEGACY_OPT_NONE;
        }

        for (int index = 0; index < LEGACY_READ_OPTION.length; index++)
        {
            if (LEGACY_READ_OPTION[index] == legacyOption || LEGACY_WRITE_OPTION[index] == legacyOption)
            {
                return index;
            }
        }

        return LEGACY_OPT_NONE;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 링크 파라미터 값의 범위와 기본값
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // 레벨 1스텝당 전압(mV). Tx 파워 계열 공통이다.
    static public final int TX_PWR_LEVEL_STEP_MV = 25;

    // 상시 · 매핑 하한 (72 = 1.800V ~ 213 = 5.325V)
    static public final int MIN_TX_PWR_SELECT_MIN = 72;
    static public final int MIN_TX_PWR_SELECT_MAX = 213;

    static public final int MIN_TX_PWR_DEFAULT     = 160; // 4.000V
    static public final int MAPPING_TX_PWR_DEFAULT = 200; // 5.000V

    /* 강제 고정만 상한이 한 스텝 높은 214다. 하한 계열의 213 제한은 하한과 상한이 같으면
     * 증감 조건이 모두 거짓이 되어 제어가 멈추는 것을 막으려는 값인데,
     * 강제 고정은 제어를 멈추는 것이 목적이라 그 제약을 받지 않는다. */
    static public final int FORCE_TX_PWR_SELECT_MIN = 72;
    static public final int FORCE_TX_PWR_SELECT_MAX = 214;
    static public final int FORCE_TX_PWR_RELEASE    = 0; // 유효 범위 밖이라 해제 센티널로 쓴다

    /* 링크 Tx 파워 상한 (프로토콜 4.5, 인덱스 16).
     *
     * 링크 제어가 «올릴 수 있는» 최대이자 링크 초기화 램프업이 멈추는 지점이다.
     * 낮추면 초기 전압이 그만큼 낮아진다 — 이 설정을 만든 이유가 그것이다.
     *
     * 상한의 «하한선» 은 상수가 아니다. (인덱스 3, 4 중 큰 값) + 1 이고 둘 다 사용자가 바꾼다.
     * 그래서 MIN 상수를 두지 않고 makeMaxTxPowerFloor() 로 그때그때 계산한다. */
    static public final int MAX_TX_PWR_SELECT_MAX = 214; // 5.350V — 펌웨어 MaxVoltageControlValue
    static public final int MAX_TX_PWR_DEFAULT    = 214; // HW 상한이자 부팅 기본값

    /* 피드포워드 설정의 범위와 기본값. 근거는 0x59 프로토콜 규격.md 의 인덱스 17~23 이다. */
    static public final int FF_COOLDOWN_MIN     = 1;
    static public final int FF_COOLDOWN_MAX     = 255;
    static public final int FF_COOLDOWN_DEFAULT = 50; // msec

    static public final int FF_STEP_MIN     = 1;
    static public final int FF_STEP_MAX     = 40;
    static public final int FF_STEP_DEFAULT = 1;

    static public final int   FF_RATIO_MIN      = 1;
    static public final int   FF_RATIO_MAX      = 100;
    static public final int[] FF_RATIO_DEFAULT  = {5, 10, 15, 20};

    /* 비율 구간의 경계. 채널당 평균 amplitude 를 4등분한 것이고,
     * 인덱스 26 이 그 판정과 «같은 값» 이라 앱이 지금 구간을 그대로 안다. */
    static public final int FF_BAND_SIZE  = 64;
    static public final int FF_BAND_COUNT = 4;

    /* 백텔이 내리는 속도. 주기 300 msec 에 1스텝이므로 초당 3.3 스텝이다.
     * 상승이 이보다 크면 일정한 소리에서도 상한까지 올라간다 — 규격이 심각도 «높음» 으로 지정했다. */
    static public final int FF_FALL_STEPS_PER_SEC = 3;

    /** 초당 상승 스텝. 규격의 «19 × (1000 / 18)» 을 정수 손실 없이 계산한다. */
    static public int makeFfRiseStepsPerSec(int step, int cooldownMs)
    {
        if (step == LINK_VALUE_UNKNOWN || cooldownMs == LINK_VALUE_UNKNOWN || cooldownMs <= 0)
        {
            return LINK_VALUE_UNKNOWN;
        }

        return (step * 1000) / cooldownMs;
    }

    /** 이 인덱스가 피드포워드 계열인가 (17 ~ 26). */
    static public boolean isFfLinkIndex(int index)
    {
        return (RC_LINK_IDX_FF_ENABLE <= index) && (index <= RC_LINK_IDX_FF_AVG_AMP);
    }

    /** 평균 amplitude 가 속한 비율 구간 (0 ~ 3). 모르면 LINK_VALUE_UNKNOWN. */
    static public int makeFfBandIndex(int avgAmplitude)
    {
        if (avgAmplitude == LINK_VALUE_UNKNOWN || avgAmplitude < 0)
        {
            return LINK_VALUE_UNKNOWN;
        }

        return Math.min(FF_BAND_COUNT - 1, avgAmplitude / FF_BAND_SIZE);
    }

    /** 상한이 «넘어야 하는» 바닥. 펌웨어 tdc_get_max_tx_power_level_floor() 와 같은 식이다.
     *
     *  두 하한 중 하나라도 못 읽었으면 읽은 쪽만 본다. 둘 다 모르면 LINK_VALUE_UNKNOWN 을 돌려
     *  호출부가 «계산 불가» 로 다루게 한다 — 모르는 값을 0 으로 보고 목록을 72 부터 그리면
     *  펌웨어가 거부할 값을 고르게 된다. */
    static public int makeMaxTxPowerFloor(int minLevel, int mappingLevel)
    {
        boolean hasMin     = (minLevel != LINK_VALUE_UNKNOWN);
        boolean hasMapping = (mappingLevel != LINK_VALUE_UNKNOWN);

        if (!hasMin && !hasMapping)
        {
            return LINK_VALUE_UNKNOWN;
        }

        if (!hasMapping)
        {
            return minLevel;
        }

        if (!hasMin)
        {
            return mappingLevel;
        }

        return Math.max(minLevel, mappingLevel);
    }

    // 백텔 주기 (100msec 단위). 직접 선택은 100 ~ 1000msec 범위로 둔다.
    static public final int BACKTEL_PERIOD_UNIT_MS       = 100;
    static public final int BACKTEL_PERIOD_DEFAULT_100MS = 3; // 300msec
    static public final int BACKTEL_PERIOD_SELECT_MIN    = 1;
    static public final int BACKTEL_PERIOD_SELECT_MAX    = 10;

    // Tx 파워 상승 스텝 (1 = 25mV ~ 40 = 1.0V)
    static public final int TX_STEP_UP_SELECT_MIN = 1;
    static public final int TX_STEP_UP_SELECT_MAX = 40;
    static public final int TX_STEP_UP_DEFAULT    = 1;

    /* 백텔 읽기 «직전» 전원 안정용 NopStandby 개수.
     *
     * 펌웨어의 검사 범위는 0 ~ 19 다 (isd_interface.c 의 tdc_is_valid_power_stable_nop_count).
     * 하한 0 은 의도된 값이다. 이 Nop 을 넣기 이전 배치를 재현해 효과를 비교하려는 것이다.
     *
     * 주의 : 되읽은 값과 실제 동작 개수가 다를 수 있다. 채널당 프레임 수가 크면 24슬롯 FIFO 에
     * 다 들어가지 않아 펌웨어가 매 백텔 사이클마다 다시 잘라내기 때문이다.
     * 백텔 읽기 «뒤» 의 NopBacktel 3개는 FPGA 요구라 고정이며 설정 대상이 아니다. */
    static public final int NOP_STANDBY_SELECT_MIN = 0;
    static public final int NOP_STANDBY_SELECT_MAX = 19;
    static public final int NOP_STANDBY_DEFAULT    = 3;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 패킷 수별로 «쓸 수 있는» 전원 안정 Nop
    ///
    /// 아무 값이나 넣으면 자리 맞추기가 낭비된다. 자리 맞추기가 0 이 되는 값만 골라야
    /// FIFO 24 슬롯을 버리지 않는다. 그 값들이 패킷 수마다 정해져 있다.
    ///
    /// 표의 출처는 사운드처리기 docs/참고/백텔 링크 체크/예상 백텔 패턴 문서다.
    /// 실측으로 채운 값이라 앱이 계산해서 만들지 않는다.
    ///
    /// 배열의 자리가 패킷 수다. 0번은 쓰지 않는다.
    /// 패킷 수 10 이상이면 백텔이 나가지 않으므로 표에 없다.
    ///
    /// 주의 : 표를 찾을 때는 펄스폭(인덱스 12)이 아니라 «패킷 수»(인덱스 13)를 써야 한다.
    ///        nOFm 자극 방식은 패킷 수를 3 으로 고정해 펄스폭과 어긋난다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static public final int NOP_FRAME_NUM_MAX = 9; // 이보다 크면 백텔이 나가지 않는다

    // 패킷 수별 기본값. 맵 계산 시점에 사운드처리기가 자동으로 넣는 값이다.
    static private final int[] NOP_DEFAULT_BY_FRAME = {3, 3, 3, 3, 2, 4, 6, 1, 2, 3};

    /* 자극 공백은 «슬롯 수» 로 정해진다. 한 블록이 24 슬롯이고 그것이 1000usec 다.
     *
     * 패킷 수 1 은 0 ~ 19 를 다 쓸 수 있다. 기본 배치가 FW · BT · b 세 개로 5 슬롯이고
     * [S] 한 개마다 한 슬롯씩 늘어난다.
     *
     * 문서에는 "한 개에 42usec 씩" 이라고 적혀 있는데 그것은 반올림한 값이다.
     * 그대로 곱하면 S=19 에서 1006usec 가 나와 문서의 끝값 1000 과 어긋난다.
     * 슬롯으로 계산하면 양 끝(208 · 1000)이 정확히 맞는다. */
    static private final int NOP_BLOCK_SLOT_COUNT = 24;
    static private final int NOP_BLOCK_US         = 1000;
    static private final int NOP_FRAME1_BASE_SLOT = 5; // FW BT b×3

    // 패킷 수 2 이상에서 쓸 수 있는 값과, 그때 자극이 끊기는 시간(usec)
    static private final int[][] NOP_USABLE_BY_FRAME = { //
            {}, // 0 : 쓰지 않음
            {}, // 1 : 아래 함수가 0 ~ 19 로 만든다
            {1, 3, 5, 7, 9, 11, 13, 15, 17}, //
            {0, 3, 6, 9, 12, 15}, //
            {2, 6, 10, 14}, //
            {4, 9}, //
            {0, 6, 12}, //
            {1, 8}, //
            {2, 10}, //
            {3} //
    };

    static private final int[][] NOP_GAP_BY_FRAME = { //
            {}, //
            {}, //
            {333, 417, 500, 583, 667, 750, 833, 917, 1000}, //
            {375, 500, 625, 750, 875, 1000}, //
            {500, 667, 833, 1000}, //
            {625, 1000}, //
            {500, 750, 1000}, //
            {583, 1000}, //
            {667, 1000}, //
            {1000} //
    };

    // 자극이 이만큼 끊기면 경고한다. 표에서 ⚠ 로 표시된 지점이다.
    static public final int NOP_GAP_WARN_US = 1000;

    /* 패킷 수가 이 값 이상이면 백텔이 아예 나가지 않는다.
     *
     * 그러면 링크 판정 블록에 진입조차 못 해 링크 제어 전체가 멈춘다. 그런데 로그도
     * 에러도 나지 않는다. 리모콘으로 무엇을 설정하든 반영되지 않으므로 앱이 알려 줘야 한다.
     * 사운드처리기 docs 의 «설정 의존성과 앱 UI 규칙» 이 가장 조용한 함정이라고 부르는 상태다. */
    static public final int LINK_FRAME_NUM_FROZEN = 10;

    /* [S] 의 «실상한». 인덱스 11 은 0 ~ 19 를 받아 주지만 CFX 가 FIFO 잔여 슬롯으로
     * 한 번 더 자른다. 큰 값을 써도 거부되지 않고 조용히 이 값으로 줄어든다.
     *
     *   패킷 수 <= 3 : 24 - (패킷 수 x 2) - 3
     *   패킷 수 >= 4 : 24 - 패킷 수 - 6 */
    static public int getNopHardLimit(int frameNum)
    {
        if (!isNopFrameNumUsable(frameNum))
        {
            return NOP_STANDBY_SELECT_MAX;
        }

        int limit = (frameNum <= 3) //
                    ? (24 - (frameNum * 2) - 3) //
                    : (24 - frameNum - 6);

        return Math.min(NOP_STANDBY_SELECT_MAX, limit);
    }

    /* [S] 가 이 값을 넘으면 그 백텔 사이클의 자극이 «한 채널도» 나가지 않는다.
     *
     * 실상한과는 다른 이야기다. 실상한까지는 써지지만 그 자리는 아홉 패킷 수 모두 자극 0 이다.
     * 자극이 어차피 0 인 구간도 쓸 이유는 있다 - 자극 공백은 더 늘지 않으면서 전원 회복
     * 시간만 길어지기 때문이다. 그래서 막지 않고 알리기만 한다.
     *
     * 딱 맞는 값을 훑어야 나오는 값이라 식으로 만들지 않고 표를 그대로 옮겼다.
     * 패킷 수 9 는 어떤 값을 넣어도 자극이 남지 않아 -1 이다. */
    static private final int[] NOP_STIM_MAX_BY_FRAME = {-1, 18, 15, 12, 10, 4, 6, 1, 2, -1};

    static public int getNopStimMax(int frameNum)
    {
        return isNopFrameNumUsable(frameNum) ? NOP_STIM_MAX_BY_FRAME[frameNum] : -1;
    }

    // 이 패킷 수는 어떤 [S] 로도 자극을 남길 수 없다.
    static public boolean isNopStimAlwaysZero(int frameNum)
    {
        return (getNopStimMax(frameNum) < 0);
    }

    // 패킷 수를 알 수 있는지. 0 이나 범위 밖이면 표를 찾을 수 없다.
    static public boolean isNopFrameNumUsable(int frameNum)
    {
        return (frameNum >= 1 && frameNum <= NOP_FRAME_NUM_MAX);
    }

    static public int getNopDefaultByFrame(int frameNum)
    {
        return isNopFrameNumUsable(frameNum) ? NOP_DEFAULT_BY_FRAME[frameNum] : NOP_STANDBY_DEFAULT;
    }

    // 이 패킷 수에서 쓸 수 있는 [S] 값들. 패킷 수를 모르면 빈 배열이다.
    static public int[] getNopUsableValues(int frameNum)
    {
        if (!isNopFrameNumUsable(frameNum))
        {
            return new int[0];
        }

        if (frameNum == 1)
        {
            int[] values = new int[NOP_STANDBY_SELECT_MAX - NOP_STANDBY_SELECT_MIN + 1];

            for (int i = 0; i < values.length; i++)
            {
                values[i] = NOP_STANDBY_SELECT_MIN + i;
            }

            return values;
        }

        return NOP_USABLE_BY_FRAME[frameNum];
    }

    // 위 배열의 n번째 값을 골랐을 때 자극이 끊기는 시간(usec)
    static public int getNopGapUs(int frameNum, int valueIndex)
    {
        if (!isNopFrameNumUsable(frameNum) || valueIndex < 0)
        {
            return 0;
        }

        if (frameNum == 1)
        {
            // 패킷 수 1 은 값이 0 부터 1씩 늘어나므로 valueIndex 가 곧 [S] 다.
            return ((NOP_FRAME1_BASE_SLOT + valueIndex) * NOP_BLOCK_US) / NOP_BLOCK_SLOT_COUNT;
        }

        int[] gaps = NOP_GAP_BY_FRAME[frameNum];

        return (valueIndex < gaps.length) ? gaps[valueIndex] : 0;
    }

    // 전원 안정 Nop 을 리모콘이 정했는지
    static public final int NOP_ENABLE_DEFAULT = 0; // 패킷 수별 기본값이 들어 있다
    static public final int NOP_ENABLE_MANUAL  = 1; // 리모콘이 준 값이 들어 있다

    // 자극 전략 (인덱스 15)
    static public final int STIM_STRATEGY_CIS    = 1;
    static public final int STIM_STRATEGY_NOFM   = 2;
    static public final int STIM_STRATEGY_MEDIUM = 3;

    /* nOFm 은 밴드 16 이상이면 패킷 수를 3 으로 고정한다. 그때의 펄스폭은 34 ~ 54usec 다.
     * 그 밖이면 «3프레임 고정» 전제를 벗어난 것이라 계측된 바 없는 상태다. */
    static public final int NOFM_PULSE_WIDTH_MIN = 34;
    static public final int NOFM_PULSE_WIDTH_MAX = 54;

    // 불리언 파라미터 (one coin, infinite coin, 상승 가속)
    static public final int LINK_FLAG_DISABLE = 0;
    static public final int LINK_FLAG_ENABLE  = 1;

    // 링크 제어 모드
    static public final int LINK_CTRL_MODE_POWER_STATE = 0; // 전원 상태 기준
    static public final int LINK_CTRL_MODE_BACKTEL     = 1; // 백텔 수신 기준

    /* 화면에서 고르는 코인 모드. 실제로는 one coin 과 infinite coin 두 값의 조합이다.
     * infinite 가 one coin 보다 우선하므로 세 모드로 정리된다. */
    static public final int COIN_MODE_DISABLE  = 0; // one coin 0, infinite 0
    static public final int COIN_MODE_ENABLE   = 1; // one coin 1, infinite 0
    static public final int COIN_MODE_INFINITE = 2; // one coin 1, infinite 1

    /* 프로토콜 4.4 부터 링크 인덱스 7 이 받는 값.
     *
     *   0        : 비활성 — 백텔 1회 실패에 바로 끊는다
     *   1 ~ 100  : 개수   — 그 횟수만큼 견딘다 (파일에 남는다)
     *   255      : 무한   — 끊김 판정을 하지 않는다 (시험 전용, 전원을 껐다 켜면 풀린다)
     *
     * 101 ~ 254 는 사운드처리기가 거부한다. 앱은 그 값을 만들지 않는다.
     *
     * 255 는 인덱스 8(infinite coin)의 1 과 «같은 내부 플래그» 다. 어느 쪽으로 켜도
     * 나머지 하나가 따라 읽힌다. 그래서 신규 앱은 인덱스 7 만 쓰면 된다. */
    static public final int COIN_COUNT_DISABLE  = 0;
    static public final int COIN_COUNT_MIN      = 1;
    static public final int COIN_COUNT_MAX      = 100;
    static public final int COIN_COUNT_INFINITE = 255;

    /* 코인 재시도 때의 Tx 파워 상향이 4.4 부터 «2 스텝 고정» 이 아니라 인덱스 5(상승 스텝)를
     * 따른다. 스텝이 크면 재시도 한 번에 파워가 크게 뛴다 — 스텝 40 이면 1.0V 다.
     * 무력화가 아니라 «동반 효과» 라 막지 않고 알리기만 한다. 이 값(0.5V)부터 알린다. */
    static public final int COIN_STEP_UP_WARN_LEVEL = 20;

    // 연결 직후 읽어오기 전의 미확인 상태
    static public final int LINK_VALUE_UNKNOWN = -1;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// 자극 제어 — 맵 강제 초기화 (옵션 0x21, 인덱스 1, 쓰기 전용)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
