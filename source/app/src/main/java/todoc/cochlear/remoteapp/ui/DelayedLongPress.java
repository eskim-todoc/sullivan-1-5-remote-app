package todoc.cochlear.remoteapp.ui;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** 상단 장치 배지의 터치 롱클릭만 시스템 시간의 1.5배로 지연한다. */
public final class DelayedLongPress
{
    private static final Map<View, Binding> bindings = new WeakHashMap<>();

    private DelayedLongPress()
    {
    }

    /**
     * UI 스레드에서 클릭/롱클릭 리스너를 설정한 다음 한 번 연결한다.
     * 터치 리스너는 대체하지만 상위 스크롤의 가로채기와 접근성/키보드 액션은 유지한다.
     */
    public static void attach(View view)
    {
        attach(view, () -> false, () -> { });
    }

    public static void attach(View view, java.util.function.BooleanSupplier canDismiss, Runnable dismiss)
    {
        if (bindings.containsKey(view))
        {
            return;
        }
        Binding binding = new Binding(view, canDismiss, dismiss);
        bindings.put(view, binding);
        view.setOnTouchListener(binding);
        view.addOnAttachStateChangeListener(binding);
    }

    private static final class Binding implements View.OnTouchListener, View.OnAttachStateChangeListener
    {
        private final WeakReference<View> target;
        private final LongPressGesture gesture;
        private Runnable pending;
        private long token;

        private final java.util.function.BooleanSupplier canDismiss;
        private final Runnable dismiss;
        private float rawX, rawY, originalAlpha;
        private boolean dragging, horizontal;

        Binding(View view, java.util.function.BooleanSupplier canDismiss, Runnable dismiss)
        {
            this.canDismiss = canDismiss;
            this.dismiss = dismiss;
            target = new WeakReference<>(view);
            gesture = new LongPressGesture(ViewConfiguration.getLongPressTimeout(),
                ViewConfiguration.get(view.getContext()).getScaledTouchSlop());
        }

        @Override
        public boolean onTouch(View view, MotionEvent event)
        {
            if (!view.isEnabled())
            {
                cancel(view);
                return false;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
            {
                rawX = event.getRawX();
                rawY = event.getRawY();
                originalAlpha = view.getAlpha();
                dragging = false;
                horizontal = false;
                if (canDismiss.getAsBoolean()) view.getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1)
            {
                float dx = event.getRawX() - rawX;
                float dy = event.getRawY() - rawY;
                float slop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
                if (!dragging && Math.abs(dx) > slop && Math.abs(dx) >= Math.abs(dy))
                {
                    horizontal = true;
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (!horizontal && canDismiss.getAsBoolean() && Math.abs(dy) > slop && Math.abs(dy) > Math.abs(dx) * 1.3f)
                {
                    dragging = true;
                    cancel(view);
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    view.setTranslationY(Math.max(-view.getHeight(), Math.min(view.getHeight(), dy)));
                    view.setAlpha(originalAlpha * 0.6f);
                    return true;
                }
            }
            if (dragging && event.getActionMasked() == MotionEvent.ACTION_UP)
            {
                boolean remove = canDismiss.getAsBoolean() && Math.abs(event.getRawY() - rawY) >= view.getHeight();
                resetDrag(view);
                cancel(view);
                if (remove) dismiss.run();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getPointerCount() > 1)
            {
                resetDrag(view);
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP) view.getParent().requestDisallowInterceptTouchEvent(false);
            switch (event.getActionMasked())
            {
                case MotionEvent.ACTION_DOWN:
                    cancel(view);
                    gesture.setSystemTimeout(ViewConfiguration.getLongPressTimeout());
                    token = gesture.start(SystemClock.uptimeMillis(), event.getX(), event.getY());
                    view.drawableHotspotChanged(event.getX(), event.getY());
                    view.setPressed(true);
                    long capturedToken = token;
                    pending = () -> dispatchLongPress(capturedToken);
                    view.postDelayed(pending, gesture.delayMs());
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() != 1)
                    {
                        cancel(view);
                        break;
                    }
                    for (int index = 0; index < event.getHistorySize(); index++)
                    {
                        if (!gesture.move(token, event.getHistoricalX(index), event.getHistoricalY(index)))
                        {
                            cancel(view);
                            break;
                        }
                    }
                    if (!gesture.move(token, event.getX(), event.getY()))
                    {
                        cancel(view);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    removePending(view);
                    boolean click = gesture.release(token, event.getX(), event.getY());
                    view.setPressed(false);
                    if (click && usable(view) && inside(view, event.getX(), event.getY()))
                    {
                        view.performClick();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_POINTER_UP:
                    cancel(view);
                    break;
                default:
                    break;
            }
            // View.onTouchEvent의 기본 롱클릭 예약만 막는다. 부모의 intercept는 그대로다.
            return true;
        }

        private void dispatchLongPress(long capturedToken)
        {
            if (capturedToken != token)
            {
                return;
            }
            pending = null;
            View view = target.get();
            if (view == null)
            {
                gesture.cancel();
                return;
            }
            if (!usable(view))
            {
                cancel(view);
                return;
            }
            if (view.isLongClickable() && gesture.claimLongPress(capturedToken, SystemClock.uptimeMillis()))
            {
                view.setPressed(false);
                if (!view.performLongClick())
                {
                    gesture.unhandledLongPress(capturedToken);
                }
            }
        }

        private static boolean usable(View view)
        {
            return view.isEnabled() && view.isAttachedToWindow() && view.isShown()
                && view.getWindowVisibility() == View.VISIBLE && view.hasWindowFocus();
        }

        private static boolean inside(View view, float x, float y)
        {
            return x >= 0 && x < view.getWidth() && y >= 0 && y < view.getHeight();
        }

        private void resetDrag(View view)
        {
            if (dragging)
            {
                view.setTranslationY(0);
                view.setAlpha(originalAlpha);
                view.getParent().requestDisallowInterceptTouchEvent(false);
                dragging = false;
            }
        }

        private void removePending(View view)
        {
            if (pending != null)
            {
                view.removeCallbacks(pending);
                pending = null;
            }
        }

        private void cancel(View view)
        {
            removePending(view);
            gesture.cancel();
            view.setPressed(false);
        }

        @Override
        public void onViewAttachedToWindow(View view)
        {
        }

        @Override
        public void onViewDetachedFromWindow(View view)
        {
            resetDrag(view);
            cancel(view);
        }
    }
}
