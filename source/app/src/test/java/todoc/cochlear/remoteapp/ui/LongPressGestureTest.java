package todoc.cochlear.remoteapp.ui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LongPressGestureTest
{
    @Test
    public void touchHoldWaitsBeyondSystemThresholdAndCannotAlsoClick()
    {
        LongPressGesture gesture = new LongPressGesture(500, 8);
        long token = gesture.start(1000, 20, 20);
        assertFalse(gesture.claimLongPress(token, 1500));
        assertFalse(gesture.claimLongPress(token, 1749));
        assertTrue(gesture.claimLongPress(token, 1750));
        assertFalse(gesture.claimLongPress(token, 1900));
        assertFalse(gesture.release(token, 20, 20));

        gesture.setSystemTimeout(1000);
        long slowerToken = gesture.start(0, 20, 20);
        assertFalse(gesture.claimLongPress(slowerToken, 1499));
        assertTrue(gesture.claimLongPress(slowerToken, 1500));
    }

    @Test
    public void crossingDragSlopCannotBecomeALongPressByReturningToTheStart()
    {
        LongPressGesture gesture = new LongPressGesture(500, 8);
        long token = gesture.start(0, 10, 10);
        assertTrue(gesture.move(token, 17, 10));
        assertFalse(gesture.move(token, 19, 10));
        assertFalse(gesture.move(token, 10, 10));
        assertFalse(gesture.claimLongPress(token, 2000));
        assertFalse(gesture.release(token, 10, 10));
    }

    @Test
    public void releaseCancelAndReplacementRejectOldCallbacksWithoutLosingTheNextTap()
    {
        LongPressGesture gesture = new LongPressGesture(500, 8);
        long released = gesture.start(0, 10, 10);
        assertTrue(gesture.release(released, 10, 10));
        assertFalse(gesture.claimLongPress(released, 1000));

        long cancelled = gesture.start(1000, 10, 10);
        gesture.cancel();
        long replacement = gesture.start(1200, 10, 10);
        assertFalse(gesture.claimLongPress(cancelled, 2500));
        assertTrue(gesture.release(replacement, 11, 10));
        assertFalse(gesture.claimLongPress(replacement, 3000));

        long unhandled = gesture.start(3000, 10, 10);
        assertTrue(gesture.claimLongPress(unhandled, 3750));
        gesture.unhandledLongPress(unhandled);
        assertTrue(gesture.release(unhandled, 10, 10));
    }
}
