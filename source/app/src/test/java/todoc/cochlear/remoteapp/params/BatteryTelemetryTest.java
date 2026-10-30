package todoc.cochlear.remoteapp.params;

import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

import static org.junit.Assert.*;

public class BatteryTelemetryTest
{
    @Test
    public void detailedBatteryCanBeProbedFromRel45()
    {
        assertFalse(BatteryTelemetry.supportsProtocol(4, 4));
        assertTrue(BatteryTelemetry.supportsProtocol(4, 5));
        assertFalse(BatteryTelemetry.supportsProtocol(0, 0));
        assertTrue(BatteryTelemetry.supportsProtocol(4, 6));
    }

    // 펌웨어 규격 §6의 3800mV / 80% / PMIC150 / -30000µA / 정상 / 100ms 예제.
    private static final byte[] GOLDEN = {
        0x59, 0x22, 0, 1, 15, 0x0E, (byte) 0xD8, 0x50, (byte) 0x96,
        1, 2, 3, 4, (byte) 0xFF, (byte) 0xFF, (byte) 0x8A, (byte) 0xD0, 0, 0, 0x64
    };

    @Test
    public void decodesFirmwareExampleAndCalculatesUnroundedPower()
    {
        BatteryTelemetry b = BatteryTelemetry.parse(GOLDEN);
        assertEquals(3800, b.voltageMv);
        assertEquals(80, b.percent);
        assertEquals(150, b.txPmicLevel);
        assertEquals(0x01020304L, b.systemTick);
        assertEquals(Integer.valueOf(-30000), b.averageCurrentUa);
        assertEquals(30.0, b.consumptionCurrentMa(), 0.000001);
        assertEquals(0.114, b.consumptionPowerW(), 0.000000001);
        assertEquals("+114.0 ㎽", b.powerText());
    }

    @Test
    public void chargingRetainsNegativeConsumptionAndLocaleDoesNotChangeUnits()
    {
        byte[] p = GOLDEN.clone();
        p[13] = 0;
        p[14] = 0;
        p[15] = 0x2E;
        p[16] = (byte) 0xE0; // +12000µA
        Locale before = Locale.getDefault();
        try
        {
            Locale.setDefault(Locale.GERMANY);
            BatteryTelemetry b = BatteryTelemetry.parse(p);
            assertEquals(-12.0, b.consumptionCurrentMa(), 0.000001);
            assertEquals(-0.0456, b.consumptionPowerW(), 0.000000001);
            assertEquals("-12.0 ㎃", b.currentText());
            assertEquals("-45.6 ㎽", b.powerText());
            assertTrue(b.detailText().contains("충전 중"));
        }
        finally
        {
            Locale.setDefault(before);
        }
    }

    @Test
    public void roundedCurrentDoesNotChangePowerAndTinyChargeDoesNotShowNegativeZero()
    {
        byte[] p = GOLDEN.clone();
        int ua = -16979;
        for (int i = 0; i < 4; i++)
        {
            p[13 + i] = (byte) (ua >> (24 - 8 * i));
        }
        BatteryTelemetry b = BatteryTelemetry.parse(p);
        assertEquals("+17.0 ㎃", b.currentText());
        assertEquals("+64.5 ㎽", b.powerText()); // 3.8V * 16.979mA, not rounded 17.0mA
        Arrays.fill(p, 13, 17, (byte) 0);
        p[16] = 1;
        assertEquals("+0.0 ㎃", BatteryTelemetry.parse(p).currentText());
        assertEquals("+0.0 ㎽", BatteryTelemetry.parse(p).powerText());
    }

    @Test
    public void legacyKeepsVoltageButDoesNotInventZeroCurrent()
    {
        byte[] p = Arrays.copyOf(GOLDEN, 13);
        p[4] = 8;
        BatteryTelemetry b = BatteryTelemetry.parse(p);
        assertEquals("3.800 Ｖ", b.voltageText());
        assertNull(b.averageCurrentUa);
        assertNull(b.consumptionCurrentMa());
        assertNull(b.consumptionPowerW());
        assertEquals("—", b.powerText());
    }

    @Test
    public void rejectsTruncatedExtensionsHeadersAndOtherTransactions()
    {
        for (int length = 0; length < GOLDEN.length; length++)
        {
            final byte[] p = Arrays.copyOf(GOLDEN, length);
            assertThrows(IllegalArgumentException.class, () -> BatteryTelemetry.parse(p));
        }
        for (int length = 9; length < 15; length++)
        {
            byte[] p = GOLDEN.clone();
            p[4] = (byte) length;
            assertThrows(IllegalArgumentException.class, () -> BatteryTelemetry.parse(p));
        }
        for (int offset : new int[]{0, 1, 2})
        {
            byte[] p = GOLDEN.clone();
            p[offset] = 2;
            assertThrows(IllegalArgumentException.class, () -> BatteryTelemetry.parse(p));
        }
        byte[] wrongIndex = GOLDEN.clone();
        wrongIndex[3] = 3;
        assertThrows(IllegalArgumentException.class, () -> BatteryTelemetry.parse(wrongIndex));
        assertThrows(IllegalArgumentException.class, () -> BatteryTelemetry.parse(null));
        byte[] p = GOLDEN.clone();
        p[7] = 101;
        assertThrows(IllegalArgumentException.class, () -> BatteryTelemetry.parse(p));
    }

    @Test
    public void acceptsAllIndexAndFutureTailWithoutDependingOnReleaseMinor()
    {
        byte[] p = Arrays.copyOf(GOLDEN, 22);
        p[3] = 0;
        p[4] = 17;
        assertEquals(0.114, BatteryTelemetry.parse(p).consumptionPowerW(), 0.000000001);
    }

    @Test
    public void rel45ExtendedIndexRetainsSignedCurrentAndRejectsLegacyPayload()
    {
        byte[] p = GOLDEN.clone();
        p[3] = 2;
        assertEquals(-30000, BatteryTelemetry.parse(p).averageCurrentUa.intValue());
        p = Arrays.copyOf(p, 13);
        p[4] = 8;
        final byte[] legacy = p;
        assertThrows(IllegalArgumentException.class, () -> BatteryTelemetry.parse(legacy));
    }

    @Test
    public void invalidStatusesNeverExposeCachedCurrent()
    {
        for (int status : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 255})
        {
            byte[] p = GOLDEN.clone();
            p[17] = (byte) status;
            BatteryTelemetry b = BatteryTelemetry.parse(p);
            assertEquals(Integer.valueOf(-30000), b.averageCurrentUa);
            assertFalse(b.isCurrentValid());
            assertNull(b.consumptionPowerW());
            assertEquals("—", b.currentText());
        }
    }

    @Test
    public void sampleAgeBoundaryAndNeverSampledSentinelAreEnforced()
    {
        byte[] p = GOLDEN.clone();
        p[18] = 9;
        p[19] = (byte) 0xC4; // 2500
        assertTrue(BatteryTelemetry.parse(p).isCurrentValid());
        p[19] = (byte) 0xC5;
        assertFalse(BatteryTelemetry.parse(p).isCurrentValid());
        p[18] = (byte) 255;
        p[19] = (byte) 255;
        assertEquals("전류 샘플 없음", BatteryTelemetry.parse(p).detailText());
    }

    @Test
    public void uncalibratedVoltageSuppressesPowerButAllowsValidCurrent()
    {
        byte[] p = GOLDEN.clone();
        p[5] = 0;
        p[6] = 0;
        BatteryTelemetry b = BatteryTelemetry.parse(p);
        assertEquals(30.0, b.consumptionCurrentMa(), 0.000001);
        assertNull(b.consumptionPowerW());
        assertEquals("전압 미교정", b.voltageText());
    }

    @Test
    public void unsignedTimerAndSignedCurrentExtremesDoNotOverflow()
    {
        byte[] p = GOLDEN.clone();
        Arrays.fill(p, 9, 13, (byte) 255);
        p[13] = (byte) 128;
        p[14] = 0;
        p[15] = 0;
        p[16] = 0;
        BatteryTelemetry b = BatteryTelemetry.parse(p);
        assertEquals(4294967295L, b.systemTick);
        assertEquals(2147483.648, b.consumptionCurrentMa(), 0.000001);
        assertEquals(8160.4378624, b.consumptionPowerW(), 0.0000001);
        Arrays.fill(p, 13, 17, (byte) 0);
        assertEquals("+0.0 ㎽", BatteryTelemetry.parse(p).powerText());
    }
}
