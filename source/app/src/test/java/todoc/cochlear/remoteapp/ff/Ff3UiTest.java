package todoc.cochlear.remoteapp.ff;

import org.junit.Test;

import static org.junit.Assert.*;

public class Ff3UiTest
{
    @Test
    public void sensitivityDoesNotConfusePowerLimitWithDetection()
    {
        Ff3Config c = Ff3SessionTest.preset();
        String before = Ff3Ui.sensitivity(c);
        c.set(50, 40);
        assertEquals(before, Ff3Ui.sensitivity(c));
        c.set(43, 5);
        assertTrue(Ff3Ui.sensitivity(c).contains("5dB"));
        assertNotEquals(before, Ff3Ui.sensitivity(c));
    }

    @Test
    public void arTimeAppearsOnlyWhenArIsSelected()
    {
        Ff3Config c = Ff3SessionTest.preset();
        assertFalse(Ff3Ui.sensitivity(c).contains("상승 반응"));
        c.set(59, 2);
        assertTrue(Ff3Ui.sensitivity(c).contains("상승 반응 1 ms"));
    }

    @Test
    public void changeReviewShowsOnlyModifiedValuesAndTheirDisplayUnits()
    {
        Ff3Config before = Ff3SessionTest.preset(), after = before.copy();
        after.set(43, 5);
        after.set(42, 30);
        after.set(57, 5);
        after.set(96, 5);
        String changes = Ff3Ui.changes(before, after);
        assertTrue(changes.contains("6 dB · 평소 입력의 약 2.00배로 증가 → 5 dB · 평소 입력의 약 1.78배로 증가"));
        assertTrue(changes.contains("250 ms → 300 ms"));
        assertTrue(changes.contains("400 ms → 500 ms"));
        assertTrue(changes.contains("40 ms → 50 ms"));
        assertFalse(changes.contains("보상 한도"));
        assertEquals(6, before.get(43));
    }

    @Test
    public void categoricalSettingsAreNamesRatherThanProtocolNumbers()
    {
        Ff3Config before = Ff3SessionTest.preset(), after = before.copy();
        after.set(59, 0);
        after.set(58, 0);
        String changes = Ff3Ui.changes(before, after);
        assertTrue(changes.contains("짧게 평균"));
        assertTrue(changes.contains("즉시 읽기"));
        assertTrue(changes.contains("사용 → 사용하지 않음"));
    }

    @Test
    public void decibelsUseAmplitudeRatherThanPowerRatios()
    {
        assertEquals(1, Ff3Ui.amplitudeRatio(0), 1e-9);
        assertEquals(10, Ff3Ui.amplitudeRatio(20), 1e-9);
        assertEquals(0.1, Ff3Ui.amplitudeRatio(-20), 1e-9);
        assertEquals(1.995262315, Ff3Ui.amplitudeRatio(6), 1e-8);
        assertEquals(251.1886432, Ff3Ui.amplitudeRatio(48), 1e-6);
    }

    @Test
    public void comparisonsDistinguishRiseDropAndStability()
    {
        assertTrue(Ff3Ui.value(43, 6).contains("2.00배로 증가"));
        assertTrue(Ff3Ui.value(46, 6).contains("0.50배로 감소"));
        assertTrue(Ff3Ui.value(56, 1).contains("0.89배"));
        assertTrue(Ff3Ui.value(52, 3).contains("1.41배 미만"));
        assertTrue(Ff3Ui.value(52, 0).contains("변화 없음"));
        java.util.Locale original = java.util.Locale.getDefault();
        try
        {
            java.util.Locale.setDefault(java.util.Locale.GERMANY);
            assertTrue(Ff3Ui.value(43, 3).contains("1.41배"));
        }
        finally
        {
            java.util.Locale.setDefault(original);
        }
    }

    @Test
    public void zoneRatiosUsePreviousRealBoundaryAndIgnoreZeroSentinels()
    {
        Ff3Config c = Ff3SessionTest.preset();
        assertFalse(Ff3Ui.zoneComparison(c, 64).contains("배"));
        assertTrue(Ff3Ui.zoneComparison(c, 65).contains("첫 경계"));
        assertTrue(Ff3Ui.zoneComparison(c, 66).contains("구간 2 경계 음압 대비 약 2.00배 (+6 dB)"));
        assertTrue(Ff3Ui.zoneComparison(c, 70).contains("미사용"));
        c.set(65, 45);
        assertTrue(Ff3Ui.zoneComparison(c, 66).contains("1.12배 (+1 dB)"));
        c.set(65, 46);
        assertFalse(Ff3Ui.zoneComparison(c, 66).contains("배"));
    }
}
