package todoc.cochlear.remoteapp.ota;

import org.junit.Test;

import static org.junit.Assert.*;

public class OtaTransferRateTest
{
    @Test
    public void rollingWindowAndIndependentDevices()
    {
        OtaTransferRate a = new OtaTransferRate();
        OtaTransferRate b = new OtaTransferRate();
        a.start(0);
        b.start(0);
        a.acknowledge(100, 200);
        a.acknowledge(200, 800);
        b.acknowledge(40, 900);
        assertEquals(300, a.bytesPerSecond(1000));
        assertEquals(40, b.bytesPerSecond(1000));
        assertEquals(200, a.bytesPerSecond(1200));
        assertEquals(0, a.bytesPerSecond(1800));
    }

    @Test
    public void finishedRateDoesNotGrowAndRestartClearsHistory()
    {
        OtaTransferRate rate = new OtaTransferRate();
        rate.start(1000);
        rate.acknowledge(255, 1200);
        rate.finish(1500);
        rate.acknowledge(100, 1600);
        assertEquals(510, rate.averageBytesPerSecond());
        assertEquals(0, rate.bytesPerSecond(1800));
        rate.start(2000);
        assertEquals(0, rate.averageBytesPerSecond());
        assertEquals(0, rate.bytesPerSecond(2000));
    }
}
