package todoc.cochlear.remoteapp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.Layout;
import android.util.AttributeSet;

import com.google.android.material.button.MaterialButton;

/**
 * Centers the visible letterforms, including short plus/minus symbols.
 */
public class GlassControlButton extends MaterialButton
{
    private final Rect letterBounds = new Rect();

    public GlassControlButton(Context context)
    {
        this(context, null);
    }

    public GlassControlButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        Layout layout = getLayout();
        if (layout == null || layout.getLineCount() != 1 || layout.getText().length() == 0)
        {
            super.onDraw(canvas);
            return;
        }
        String label = layout.getText().toString();
        getPaint().getTextBounds(label, 0, label.length(), letterBounds);
        float left = getCompoundPaddingLeft() + layout.getLineLeft(0) - getScrollX();
        float centerX = left + (letterBounds.left + letterBounds.right) / 2f;
        float centerY = getBaseline() - getScrollY() + (letterBounds.top + letterBounds.bottom) / 2f;
        int checkpoint = canvas.save();
        canvas.translate(getWidth() / 2f - centerX, getHeight() / 2f - centerY);
        super.onDraw(canvas);
        canvas.restoreToCount(checkpoint);
    }
}
