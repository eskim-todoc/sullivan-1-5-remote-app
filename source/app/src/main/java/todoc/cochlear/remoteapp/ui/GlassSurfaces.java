package todoc.cochlear.remoteapp.ui;

import android.graphics.Outline;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

/**
 * 반투명 그라데이션의 알파와 독립적으로 패널 그림자의 외곽을 정의한다.
 */
public final class GlassSurfaces
{
    private GlassSurfaces()
    {
    }

    public static void lift(View view)
    {
        if (view instanceof ViewGroup && view.getBackground() != null && view.getElevation() > 0)
        {
            view.setOutlineProvider(new ViewOutlineProvider()
            {
                @Override
                public void getOutline(View target, Outline outline)
                {
                    outline.setRoundRect(0, 0, target.getWidth(), target.getHeight(),
                        12 * target.getResources().getDisplayMetrics().density);
                    outline.setAlpha(0.6f);
                }
            });
        }
        if (view instanceof ViewGroup)
        {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++)
            {
                lift(group.getChildAt(index));
            }
        }
    }
}
