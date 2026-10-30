package todoc.cochlear.remoteapp.ff;

import java.util.Locale;

/**
 * UI 설명과 단위 변환. 추천값이나 장치 쓰기는 만들지 않는다.
 */
public final class Ff3Ui
{
    private Ff3Ui()
    {
    }

    public static final String[] GOALS = {"작은 변화에도 빠르게 감지", "소리 크기별 전원 보상량", "다시 감지할 조건·처리 시간"};
    public static final String[] FILTERS = {"즉시 읽기 · 원시 입력 (raw)", "짧게 평균 · 2개 입력 (MA2)", "반응 시간 조절 · 필터 (AR)"};

    public static String label(int index)
    {
        if (index <= 60)
        {
            return Ff3Config.LABELS[index - 40];
        }
        int zone = index < 80 ? index - 63 : index < 96 ? index - 79 : index - 95;
        return "구간 " + zone + (index < 80 ? " 시작 소리 크기" : index < 96 ? " 추가 보상" : " 같은 소리로 묶는 시간");
    }

    public static int scale(int index)
    {
        return index >= 96 ? 10 : Ff3Config.displayScale(index);
    }

    public static boolean relativeDb(int index)
    {
        return index == 43 || index == 44 || index == 46 || index == 52 || index == 53 || index == 56;
    }

    /**
     * 진폭/음압비의 설명용 근사값. 프로토콜 dB 정수나 장치 보정표를 대체하지 않는다.
     */
    public static double amplitudeRatio(double db)
    {
        return Math.pow(10.0, db / 20.0);
    }

    private static String ratio(double db)
    {
        return String.format(Locale.ROOT, "%.2f", amplitudeRatio(db));
    }

    public static String dbMeaning(int index, int db)
    {
        switch (index)
        {
            case 43:
            case 44:
                return "평소 입력의 약 " + ratio(db) + "배로 증가";
            case 53:
                return "기록된 최고 입력의 약 " + ratio(db) + "배로 증가";
            case 46:
                return "최고 입력의 약 " + ratio(-db) + "배로 감소";
            case 52:
                return db == 0 ? "입력 크기 변화 없음 (1.00배)" : "큰 입력 ÷ 작은 입력 약 " + ratio(db) + "배 미만";
            case 56:
                return "상승 경계 음압의 약 " + ratio(-db) + "배";
            default:
                return "";
        }
    }

    public static String zoneComparison(Ff3Config c, int index)
    {
        if (index == 64)
        {
            return "하단 전체 · 0은 실제 0dBSPL이 아닙니다";
        }
        int zone = index - 63, level = c.get(index);
        if (level == 0)
        {
            return "미사용 · 비교할 경계 없음";
        }
        if (index == 65)
        {
            return "첫 경계 · 뒤 구간의 비교 기준";
        }
        int previous = c.get(index - 1);
        if (previous <= 0 || level <= previous)
        {
            return "직전 경계보다 큰 dBSPL을 입력하세요";
        }
        int db = level - previous;
        return "구간 " + (zone - 1) + " 경계 음압 대비 약 " + ratio(db) + "배 (+" + db + " dB)";
    }

    public static int goal(int index)
    {
        if (index == 50 || index == 55 || index >= 64)
        {
            return 1;
        }
        switch (index)
        {
            case 40:
            case 42:
            case 43:
            case 44:
            case 45:
            case 51:
            case 53:
            case 59:
                return 0;
            default:
                return 2;
        }
    }

    public static String value(int index, int raw)
    {
        if (index == 59)
        {
            return raw >= 0 && raw < FILTERS.length ? FILTERS[raw] : "미확인";
        }
        if (index == 58)
        {
            return raw == 1 ? "사용" : "사용하지 않음";
        }
        if (relativeDb(index))
        {
            return raw + " dB · " + dbMeaning(index, raw);
        }
        String unit = index == 50 || (index >= 80 && index < 96) ? "스텝"
            : index == 45 ? "블록" : index >= 96 || index == 40 || index == 42 || index == 47
            || index == 48 || index == 49 || index == 51 || index == 54 || index == 57 ? "ms"
                                   : index == 43 || index == 44 || index == 46 || index == 52 || index == 53 || index == 56 ? "dB"
                                     : index >= 64 && index < 80 ? "dBSPL" : "";
        return (raw * scale(index)) + (unit.isEmpty() ? "" : " " + unit);
    }

    public static String help(int index)
    {
        switch (index)
        {
            case 43:
                return "평소보다 얼마나 커져야 일반 상승으로 볼지 정합니다. 낮추면 작은 증가도 더 쉽게 감지하지만 잔변동에도 반응할 수 있습니다. 배수는 입력 진폭비이며 +6dB는 약2배, +12dB는 약4배입니다. 소리 구간 상승도 필요합니다. [기술명: Soft 증가]";
            case 45:
                return "일반 상승을 몇 개의 1ms 입력으로 연속 확인할지 정합니다. 줄이면 확인이 짧아지고 순간 변화도 더 쉽게 통과합니다. 모든 입력의 고정 지연은 아닙니다. [상승 확인]";
            case 59:
                return "즉시 읽기는 변화가 그대로 들어옵니다. 짧게 평균은 현재·직전 입력을 평균합니다. 반응 시간 조절을 선택하면 아래의 상승 반응 시간을 사용합니다. [상승 필터]";
            case 40:
                return "반응 시간 조절(AR)에서만 사용합니다. 줄이면 상승 입력을 더 빨리 따라갑니다. 0은 즉시 반응입니다. 다른 방식에서는 현재값을 보존합니다. [AR 어택]";
            case 44:
                return "갑자기 커진 소리가 이 기준과 더 높은 구간을 만족하면 첫 입력을 우선할 수 있습니다. 일반 증가 기준 이상이어야 합니다. [Hard 증가]";
            case 42:
                return "평소 소리 기준이 입력을 따라가는 시간입니다. 짧게 하면 기준도 빨리 바뀝니다. 이것만 줄인다고 보상이 항상 빨라지지는 않습니다. [배경 추적]";
            case 51:
                return "시작할 때 평소 소리 기준을 준비하는 시간입니다. 매번 발생하는 소리의 지연 시간이 아닙니다. [초기 배경 준비]";
            case 53:
                return "같은 최고 구간 안에서 최고값 기록을 바꿀 증가 폭입니다. 더 높은 구간의 요청을 막는 기준은 아닙니다. [Peak 갱신 여유]";
            case 50:
                return "아직 회수되지 않은 전원 보상의 최대량입니다. 크게 해도 감지 자체가 더 민감해지지 않습니다. 구간별 추가 보상 합계 이상이어야 합니다. [미회수 보상 상한 G]";
            case 55:
                return "앞에서부터 사용할 구간 수입니다. 2~16개이며 사용하지 않을 구간의 세 값은 모두 0으로 맞춰야 합니다. 값을 자동으로 지우지 않습니다.";
            case 41:
                return "작아진 소리를 얼마나 부드럽게 따라갈지 정합니다. 큰 값은 느리게 수렴하며 상승 입력 방식과 독립입니다. ms가 아닙니다. [릴리즈 EMA N]";
            case 46:
                return "소리가 이만큼 줄었을 때 더 작은 소리를 새 기준 후보로 삼습니다. 낮추면 작은 감소에도 후보가 될 수 있습니다. [재기준화 하강]";
            case 47:
                return "작아진 소리가 연속해서 유지돼야 할 시간입니다. 줄이면 확인이 짧아지지만 안정 폭·필터·전원 여유 조건도 함께 작용합니다. [낮은 입력 확인]";
            case 52:
                return "작은 소리를 새 기준으로 확인할 때 허용하는 흔들림입니다. 큰 값은 더 큰 변동을 허용합니다. [낮은 후보 안정 폭]";
            case 56:
                return "구간을 내려갈 때 상승 경계보다 낮게 잡는 여유 폭입니다. 경계 근처의 왕복을 줄입니다. [하강 히스테리시스]";
            case 58:
                return "최저 구간 밖에서 더 작은 소리를 새 기준으로 확정할 때, 새로 확인한 전원 여유가 필요한지 정합니다. [High 필요]";
            case 57:
                return "전원 여유 확인을 얼마나 오래된 것까지 사용할지 정합니다. 100ms 단위입니다. [High 최대 age]";
            case 48:
                return "소리 상승으로 만든 보상 요청을 유효하게 두는 시간입니다. 전원 조정 최소 간격보다 길어야 합니다. 늘려도 즉시 실행되는 것은 아닙니다. [상승 요청 TTL]";
            case 49:
                return "전원 설정을 연속으로 조정할 때의 최소 간격입니다. 앱과 장치 사이의 BLE 통신 간격과 다릅니다. [PMIC 쓰기 간격]";
            case 54:
                return "입력 갱신이 이 시간보다 오래 끊기면 오래된 입력으로 판단합니다. [입력 heartbeat TTL]";
            case 60:
                return "마이크 원시값과 소리 크기 경계를 연결하는 보정표입니다. 현재는 1번만 지원하므로 변경하지 않습니다. [입력 보정표 ID]";
            default:
                return "";
        }
    }

    public static String sensitivity(Ff3Config c)
    {
        return "평소 입력이 약 " + ratio(c.get(43)) + "배 (+" + c.get(43) + "dB)가 된 일반 상승을 " + c.get(45)
            + "개의 1ms 입력으로 확인합니다.\n읽는 방식: " + value(59, c.get(59))
            + (c.get(59) == 2 ? " · 상승 반응 " + value(40, c.get(40)) : "")
            + "\n더 높은 소리 구간 진입과 실행 조건도 필요합니다. 보상 한도는 감지 민감도가 아닙니다.";
    }

    public static String changes(Ff3Config before, Ff3Config after)
    {
        StringBuilder result = new StringBuilder();
        for (int i : Ff3Config.indexes())
        {
            if (before.get(i) != after.get(i))
            {
                result.append(label(i)).append(": ").append(value(i, before.get(i))).append(" → ").append(value(i, after.get(i))).append('\n');
                if (i >= 64 && i < 80)
                {
                    result.append("  ").append(zoneComparison(after, i)).append('\n');
                }
            }
        }
        return result.length() == 0 ? "설정 변경 없음" : result.toString();
    }

    public static String resultName(int value)
    {
        switch (value)
        {
            case 0:
                return "아직 결과 없음";
            case 1:
                return "전원 조정이 바빠 보류";
            case 2:
                return "전원 보상 추가 성공";
            case 3:
                return "추가량 없이 처리 완료";
            case 4:
                return "요청 유효 시간 지남";
            case 5:
                return "전원 쓰기 실패";
            default:
                return "확인 필요";
        }
    }
}
