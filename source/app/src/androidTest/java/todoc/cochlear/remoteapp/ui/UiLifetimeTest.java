package todoc.cochlear.remoteapp.ui;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextThemeWrapper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UiLifetimeTest
{
    @Test
    public void detachedChipCancelNeverClicksOrRequiresAParent()
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
        {
            Context context = new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.app_theme);
            View chip = new View(context);
            AtomicInteger clicks = new AtomicInteger();
            chip.setOnClickListener(view -> clicks.incrementAndGet());
            chip.setOnLongClickListener(view ->
            {
                clicks.incrementAndGet();
                return true;
            });
            DelayedLongPress.attach(chip, () -> true, () -> clicks.incrementAndGet());
            Object binding = chip.getTag(R.id.delayed_long_press_binding);
            DelayedLongPress.attach(chip);
            assertSame(binding, chip.getTag(R.id.delayed_long_press_binding));
            MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 1, 1, 0);
            MotionEvent cancel = MotionEvent.obtain(0, 1, MotionEvent.ACTION_CANCEL, 1, 1, 0);
            chip.dispatchTouchEvent(down);
            chip.dispatchTouchEvent(cancel);
            down.recycle();
            cancel.recycle();
            assertFalse(chip.isPressed());
            assertEquals(0, clicks.get());
        });
    }

    @Test
    public void firmwareVersionCanArriveFromThreadWithoutLooper() throws Exception
    {
        StatusViewModel model = new StatusViewModel();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() ->
        {
            try
            {
                model.setFwVerLower(7);
            }
            catch (Throwable error)
            {
                failure.set(error);
            }
        });
        worker.start();
        worker.join(3000);
        assertFalse(worker.isAlive());
        assertNull(failure.get());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> assertEquals(7, model.getFwVerLower()));
    }
}
