package todoc.cochlear.remoteapp.ff;

import java.util.Arrays;

/**
 * schema 3의 장치 조회값. 미조회는 -1이며 기본값으로 보충하지 않는다.
 */
public final class Ff3Config
{
    public static final int OFF = 0, FF1 = 1, FF3 = 3;
    public static final int[] MIN = {0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 2, 0, 1, 0, 0, 1};
    public static final int[] MAX = {4, 250, 200, 24, 48, 4, 24, 250, 20, 10, 40, 250, 12, 12, 20, 16, 6, 255, 1, 2, 1};
    public static final String[] LABELS = {
        "필터의 상승 반응 시간 (ms)", "작아진 소리를 따라가는 완만함", "평소 소리 추적 시간 (ms)", "감지할 소리 증가 폭 (dB)",
        "급격한 소리 증가 기준 (dB)", "상승 확인 길이 (1ms 입력 수)", "새 기준으로 삼을 소리 감소 폭 (dB)", "작은 소리 확인 시간 (ms)",
        "보상 요청 유효 시간 (ms)", "전원 조정 최소 간격 (ms)", "남아 있는 전원 보상 한도 (스텝)",
        "시작 시 소리 기준 준비 시간 (ms)", "작은 소리의 안정 허용 폭 (dB)", "같은 구간의 최고값 갱신 폭 (dB)",
        "입력 끊김 판단 시간 (ms)", "사용할 소리 구간 수", "구간 내려갈 때의 여유 폭 (dB)",
        "전원 여유 확인의 유효 시간 (ms)", "전원 여유 확인 사용", "소리 변화 읽는 방식", "마이크 보정표"};
    private final int[] values = new int[112];

    public Ff3Config()
    {
        Arrays.fill(values, -1);
    }

    public Ff3Config copy()
    {
        Ff3Config result = new Ff3Config();
        System.arraycopy(values, 0, result.values, 0, values.length);
        return result;
    }

    public static boolean isSetting(int index)
    {
        return (index >= 40 && index <= 60) || (index >= 64 && index <= 111);
    }

    public static int[] indexes()
    {
        int[] result = new int[69];
        int n = 0;
        for (int i = 40; i <= 111; i++)
        {
            if (isSetting(i))
            {
                result[n++] = i;
            }
        }
        return result;
    }

    public int get(int index)
    {
        return values[index];
    }

    public void set(int index, int value)
    {
        if (!isSetting(index) || value < 0 || value > 255)
        {
            throw new IllegalArgumentException("설정 범위 오류");
        }
        values[index] = value;
    }

    public boolean complete()
    {
        for (int i : indexes())
        {
            if (values[i] < 0)
            {
                return false;
            }
        }
        return true;
    }

    public boolean sameValues(Ff3Config other)
    {
        return Arrays.equals(values, other.values);
    }

    public static int displayScale(int index)
    {
        return index == 42 ? 10 : index == 57 ? 100 : 1;
    }

    public int cumulative(int zone)
    {
        int sum = 0;
        for (int i = 0; i <= zone; i++)
        {
            sum += get(80 + i);
        }
        return sum;
    }

    /**
     * 빈 실행 표는 조회할 수 있지만 정상 저장/활성화용으로 검증하지 않는다.
     */
    public String validate()
    {
        if (!complete())
        {
            return "모든 설정을 먼저 조회해야 합니다.";
        }
        for (int i = 40; i <= 60; i++)
        {
            if (get(i) < MIN[i - 40] || get(i) > MAX[i - 40])
            {
                return LABELS[i - 40] + " 범위를 확인하세요.";
            }
        }
        if (get(44) < get(43))
        {
            return "급격한 소리 증가 기준은 감지할 소리 증가 폭 이상이어야 합니다.";
        }
        if (get(48) <= get(49))
        {
            return "보상 요청 유효 시간은 전원 조정 최소 간격보다 길어야 합니다.";
        }
        int count = get(55), sum = 0;
        for (int i = 0; i < 16; i++)
        {
            int lower = get(64 + i), delta = get(80 + i), hold = get(96 + i);
            if (i >= count)
            {
                if (lower != 0 || delta != 0 || hold != 0)
                {
                    return "미사용 구간 " + (i + 1) + "의 세 값은 모두 0이어야 합니다.";
                }
                continue;
            }
            if (i == 0 && (lower != 0 || delta != 0))
            {
                return "구간 1의 하한·증가량은 0이어야 합니다.";
            }
            if (i > 0 && (lower < 29 || lower > 110 || lower <= get(63 + i) || lower - get(56) < 29))
            {
                return "구간 " + (i + 1) + "의 시작 소리 크기를 확인하세요. 29~110dBSPL 오름차순이며 내려갈 때의 여유 폭을 뺀 값도 29 이상이어야 합니다.";
            }
            if (delta > 40 || (i > 1 && delta > get(79 + i)))
            {
                return "상위 구간의 증가량은 하위 구간 이하, 최대 40입니다.";
            }
            if (hold < 1)
            {
                return "활성 구간의 유지 시간은 10~2550ms입니다.";
            }
            sum += delta;
        }
        if (sum == 0 || sum > get(50))
        {
            return "구간별 추가 보상의 합계 " + sum + "스텝은 0보다 크고 전원 보상 한도 " + get(50) + "스텝 이하여야 합니다. 보상 한도 또는 구간별 추가량을 확인하세요.";
        }
        return null;
    }

    public static String modeName(int mode)
    {
        switch (mode)
        {
            case OFF:
                return "꺼짐 (OFF)";
            case FF1:
                return "자극 크기에 따른 보상 (FF1)";
            case FF3:
                return "소리 변화에 따른 보상 (FF3)";
            default:
                return "확인 필요";
        }
    }

    public static boolean supportsFf(int major, int minor, boolean linkDomain)
    {
        // REL4.5 retains the REL4.4 BLE contract; FF starts at REL4.6.
        return linkDomain && major == 4 && minor == 6;
    }

    public static boolean supportsSchemaQuery(int major, int minor, boolean linkDomain)
    {
        return linkDomain && major == 4 && minor == 6;
    }
}
