package todoc.cochlear.remoteapp.params;

import java.util.Locale;

/** 0x59/0x22 읽기 응답. 원시 전류는 충전 + / 방전 −, 표시값은 소비 방향을 +로 한다. */
public final class BatteryTelemetry {
    public static final int CURRENT_STALE_MS = 2500;
    public static final long RECEIVE_STALE_MS = 6000;

    /** REL4.5부터 읽기를 시도하되 실제 확장 응답으로 연결별 지원을 확인한다. */
    public static boolean supportsProtocol(int major, int minor) {
        return major == 4 && minor >= 5;
    }

    public final int voltageMv, percent, txPmicLevel;
    public final long systemTick;
    public final Integer averageCurrentUa;
    public final int currentStatus, currentAgeMs;

    private BatteryTelemetry(int voltageMv, int percent, int txPmicLevel, long systemTick,
                             Integer averageCurrentUa, int currentStatus, int currentAgeMs) {
        this.voltageMv = voltageMv;
        this.percent = percent;
        this.txPmicLevel = txPmicLevel;
        this.systemTick = systemTick;
        this.averageCurrentUa = averageCurrentUa;
        this.currentStatus = currentStatus;
        this.currentAgeMs = currentAgeMs;
    }

    /** 기존 L=8 또는 확장 L>=15를 수용한다. 잘린 확장/다른 응답은 상태를 갱신하지 않는다. */
    public static BatteryTelemetry parse(byte[] packet) {
        if (packet == null || packet.length < 5 || (packet[0] & 255) != 0x59
                || (packet[1] & 255) != 0x22 || packet[2] != 0
                || (packet[3] & 255) > 2) {
            throw new IllegalArgumentException("배터리 읽기 응답 형식 오류");
        }
        int length = packet[4] & 255;
        if ((length != 8 && length < 15) || packet.length < 5 + length
                || ((packet[3] & 255) == 2 && length < 15)) {
            throw new IllegalArgumentException("배터리 응답 길이 오류");
        }
        int percent = packet[7] & 255;
        if (percent > 100) throw new IllegalArgumentException("배터리 잔량 범위 오류");
        boolean extended = length >= 15;
        return new BatteryTelemetry(u16(packet, 5), percent, packet[8] & 255,
                i32(packet, 9) & 0xFFFFFFFFL, extended ? i32(packet, 13) : null,
                extended ? packet[17] & 255 : -1, extended ? u16(packet, 18) : 65535);
    }

    private static int u16(byte[] p, int offset) {
        return ((p[offset] & 255) << 8) | (p[offset + 1] & 255);
    }

    private static int i32(byte[] p, int offset) {
        return ((p[offset] & 255) << 24) | ((p[offset + 1] & 255) << 16)
                | ((p[offset + 2] & 255) << 8) | (p[offset + 3] & 255);
    }

    public boolean isCurrentValid() {
        return averageCurrentUa != null && currentStatus == 0 && currentAgeMs <= CURRENT_STALE_MS;
    }

    public Double consumptionCurrentMa() {
        return isCurrentValid() ? -averageCurrentUa.doubleValue() / 1000.0 : null;
    }

    /** mV × µA / 10^9 = W. 표시 전류의 반올림값을 다시 사용하지 않는다. */
    public Double consumptionPowerW() {
        return isCurrentValid() && voltageMv > 0
                ? -((double) voltageMv * averageCurrentUa) / 1_000_000_000.0 : null;
    }

    public String voltageText() {
        return voltageMv == 0 ? "전압 미교정" : String.format(Locale.ROOT, "%.3f V", voltageMv / 1000.0);
    }

    public String currentText() {
        Double value = consumptionCurrentMa();
        return value == null ? "—" : String.format(Locale.ROOT, "%+.1f mA", Math.abs(value) < 0.05 ? 0.0 : value);
    }

    public String powerText() {
        Double value = consumptionPowerW();
        return value == null ? "—" : String.format(Locale.ROOT, "%+.1f mW", Math.abs(value) < 0.00005 ? 0.0 : value * 1000.0);
    }

    public String detailText() {
        if (averageCurrentUa == null) return "구형 응답 / 전류 조회 미지원";
        if (!isCurrentValid()) {
            switch (currentStatus) {
                case 0: return currentAgeMs == 65535 ? "전류 샘플 없음" : "전류 샘플이 오래됨";
                case 1: return "다른 제어를 위해 전류 수집 대기";
                case 2: return "전류 측정기 준비 안 됨";
                case 3: return "전류 읽기 실패";
                case 4: return "전류 측정기 재시작";
                case 5: return "측정 후 통신 주소 복구 실패";
                case 6: return "전류 샘플 없음";
                case 7: return "전류 샘플이 오래됨";
                case 8: return "펌웨어에서 전류 측정 꺼짐";
                default: return "알 수 없는 전류 상태 " + currentStatus;
            }
        }
        String direction = averageCurrentUa < 0 ? "방전" : averageCurrentUa > 0 ? "충전 중 / 음수는 배터리로 유입" : "순전류 0";
        return direction
                + (voltageMv == 0 ? " / 전압 미교정으로 전력 계산 불가" : "");
    }
}
