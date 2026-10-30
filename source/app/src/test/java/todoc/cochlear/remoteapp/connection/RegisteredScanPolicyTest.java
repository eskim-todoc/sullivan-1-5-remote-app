package todoc.cochlear.remoteapp.connection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RegisteredScanPolicyTest
{
    @Test
    public void onlyFreshActiveRegisteredAdvertisementsCanStartConnections()
    {
        RegisteredScanPolicy policy = new RegisteredScanPolicy();
        long scan = policy.begin(true, 1000);
        List<String> connections = new ArrayList<>();

        if (policy.claimConnection(scan, 1001, "A", true, false))
        {
            connections.add("A");
        }
        if (policy.claimConnection(scan, 1002, "unknown", false, false))
        {
            connections.add("unknown");
        }
        if (policy.claimConnection(scan, 1003, "inactive", false, false))
        {
            connections.add("inactive");
        }
        if (policy.claimConnection(scan, 999, "stale", true, false))
        {
            connections.add("stale");
        }
        if (policy.claimConnection(scan, 1004, "B", true, false))
        {
            connections.add("B");
        }

        assertEquals(Arrays.asList("A"), connections);
    }

    @Test
    public void nextExplicitScanSkipsConnectedDeviceAndClaimsOnlyOneNewDevice()
    {
        RegisteredScanPolicy policy = new RegisteredScanPolicy();
        long first = policy.begin(true, 1000);
        assertTrue(policy.claimConnection(first, 1001, "A", true, false));
        assertFalse(policy.isActive());
        assertFalse(policy.claimConnection(first, 1002, "B", true, false));

        long second = policy.begin(true, 2000);
        assertFalse(policy.claimConnection(second, 2001, "A", true, true));
        assertTrue(policy.isActive());
        assertTrue(policy.claimConnection(second, 2002, "B", true, false));
        assertFalse(policy.claimConnection(second, 2003, "C", true, false));
    }

    @Test
    public void failedOrCompletedAttemptNeverRepeatsUntilANewExplicitScan()
    {
        RegisteredScanPolicy policy = new RegisteredScanPolicy();
        long first = policy.begin(true, 1000);
        assertTrue(policy.claimConnection(first, 1001, "A", true, false));
        for (int i = 0; i < 50; i++)
        {
            assertFalse(policy.claimConnection(first, 1002 + i, "A", true, false));
        }

        policy.stop();
        assertFalse(policy.claimConnection(first, 2000, "B", true, false));
        long second = policy.begin(true, 3000);
        assertTrue(policy.claimConnection(second, 3001, "A", true, false));
    }

    @Test
    public void liveDeviceIsPreservedAndItsLaterDisconnectDoesNotCauseSameScanReconnect()
    {
        RegisteredScanPolicy policy = new RegisteredScanPolicy();
        long scan = policy.begin(true, 1000);
        assertFalse(policy.claimConnection(scan, 1001, "A", true, true));
        assertTrue(policy.claimConnection(scan, 1002, "B", true, false));
        assertFalse(policy.claimConnection(scan, 1003, "A", true, false));
    }

    @Test
    public void cancelledAndPreviousGenerationCallbacksCannotPopulateOrConnectInNewScan()
    {
        RegisteredScanPolicy policy = new RegisteredScanPolicy();
        long old = policy.begin(true, 1000);
        policy.stop();
        assertFalse(policy.acceptsGeneration(old));
        assertFalse(policy.accepts(old, 1001));

        long current = policy.begin(true, 2000);
        assertFalse(policy.accepts(old, 2001));
        assertFalse(policy.accepts(current, 1999));
        assertTrue(policy.accepts(current, 2001));
        assertFalse(policy.claimConnection(old, 2001, "A", true, false));
        assertTrue(policy.claimConnection(current, 2001, "A", true, false));
    }

    @Test
    public void manualDiscoveryNeverConnectsAndBecomingActiveNeedsAnotherFreshAdvertisement()
    {
        RegisteredScanPolicy policy = new RegisteredScanPolicy();
        long discovery = policy.begin(false, 1000);
        assertTrue(policy.accepts(discovery, 1001));
        assertFalse(policy.claimConnection(discovery, 1001, "A", true, false));

        long automatic = policy.begin(true, 2000);
        assertFalse(policy.claimConnection(automatic, 2001, "A", false, false));
        assertTrue(policy.claimConnection(automatic, 2002, "A", true, false));
        assertFalse(policy.claimConnection(automatic, 2003, null, true, false));
        assertFalse(policy.claimConnection(automatic, 2003, "", true, false));
    }
}
