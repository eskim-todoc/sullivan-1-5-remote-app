package todoc.cochlear.remoteapp.connection;

import org.junit.Test;

import static org.junit.Assert.*;

public class OtaSlotPolicyTest
{
    @Test
    public void missingOrInvalidBootInformationNeverEnablesWriting()
    {
        for (Integer current : new Integer[]{null, -1, 0, 5, 254, 256})
        {
            assertFalse(OtaSlotPolicy.canWrite(current, 1));
            assertFalse(OtaSlotPolicy.canWrite(current, 2));
        }
    }

    @Test
    public void anApplicationCanOnlyBeReplacedInTheOtherSupportedSlot()
    {
        assertFalse(OtaSlotPolicy.canWrite(1, 1));
        assertTrue(OtaSlotPolicy.canWrite(1, 2));
        assertTrue(OtaSlotPolicy.canWrite(2, 1));
        assertFalse(OtaSlotPolicy.canWrite(2, 2));
        assertFalse(OtaSlotPolicy.canWrite(1, 3));
        assertFalse(OtaSlotPolicy.canWrite(2, 0xFF));
    }

    @Test
    public void factoryAndLegacyBootSlotsStillAllowBothBleWritableDestinations()
    {
        for (int current : new int[]{3, 4, 0xFF})
        {
            assertTrue(OtaSlotPolicy.canWrite(current, 1));
            assertTrue(OtaSlotPolicy.canWrite(current, 2));
        }
    }
}
