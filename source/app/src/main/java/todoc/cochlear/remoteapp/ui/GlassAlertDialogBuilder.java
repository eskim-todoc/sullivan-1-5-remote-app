package todoc.cochlear.remoteapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * 모든 다이얼로그의 배경을 통일하고 각 화면의 클릭·종료 처리를 유지한다.
 */
public final class GlassAlertDialogBuilder extends MaterialAlertDialogBuilder
{
    private final Activity activity;

    public GlassAlertDialogBuilder(Context context)
    {
        super(context);
        activity = findActivity(context);
    }

    @Override
    public AlertDialog create()
    {
        AlertDialog dialog = super.create();
        if (activity != null)
        {
            // Prepare content and backing before the window is attached.
            dialog.create();
            dialog.getWindow().setWindowAnimations(0);
            GlassDialogStyler.apply(activity, dialog);
        }
        return dialog;
    }

    private static Activity findActivity(Context context)
    {
        while (context instanceof ContextWrapper)
        {
            if (context instanceof Activity)
            {
                return (Activity) context;
            }
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context)
            {
                return null;
            }
            context = base;
        }
        return null;
    }
}
