package todoc.cochlear.remoteapp.list;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.checkbox.MaterialCheckBox;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder;

/**
 * List-local widgets and scrollable glass edit dialogs.
 */
final class InlineListUi
{
    private InlineListUi()
    {
    }

    static final class DialogForm
    {
        final LinearLayout editor;
        final TextView error;
        final AlertDialog dialog;
        private final boolean editing;

        DialogForm(Context context, String title, boolean editing)
        {
            int maximumHeight = Math.min(dp(context, 360), context.getResources().getDisplayMetrics().heightPixels / 2);
            ScrollView scroll = new ScrollView(context)
            {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
                {
                    int limit = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED
                        ? maximumHeight : Math.min(maximumHeight, MeasureSpec.getSize(heightMeasureSpec));
                    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST));
                }
            };
            scroll.setClipToPadding(false);
            scroll.setFillViewport(false);
            editor = editor(scroll);
            error = error(editor);
            TextView titleView = text(context, title);
            titleView.setTextSize(18);
            titleView.setGravity(Gravity.CENTER);
            titleView.setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 12));
            dialog = new GlassAlertDialogBuilder(context).setCustomTitle(titleView).setView(scroll)
                .setNegativeButton("취소", null).setNeutralButton("삭제", null)
                .setPositiveButton(editing ? "저장" : "등록", null).create();
            this.editing = editing;
        }

        void show(String kind, Runnable save, Runnable delete, Runnable dismissed)
        {
            dialog.setOnDismissListener(ignored ->
            {
                hideKeyboard(editor);
                if (dismissed != null)
                {
                    dismissed.run();
                }
            });
            dialog.show();
            if (dialog.getWindow() != null)
            {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            }
            int[] buttons = {AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL, AlertDialog.BUTTON_POSITIVE};
            String[] descriptions = {kind + " 편집 취소", kind + " 삭제", kind + " 정보 저장"};
            for (int index = 0; index < buttons.length; index++)
            {
                android.widget.Button button = dialog.getButton(buttons[index]);
                button.setTypeface(ResourcesCompat.getFont(editor.getContext(), R.font.app_sans));
                button.setTextSize(12);
                button.setIncludeFontPadding(false);
                button.setContentDescription(descriptions[index]);
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(clicked -> dismiss());
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(editing ? View.VISIBLE : View.INVISIBLE);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(clicked ->
            {
                if (delete != null)
                {
                    delete.run();
                }
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(clicked -> save.run());
        }

        void showError(String message, View input)
        {
            error.setText(message);
            error.setVisibility(View.VISIBLE);
            focusError(input, error);
        }

        void dismiss()
        {
            dialog.dismiss();
        }
    }

    static int dp(Context context, int value)
    {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static LinearLayout editor(ViewGroup parent)
    {
        Context context = parent.getContext();
        LinearLayout editor = new LinearLayout(context);
        editor.setOrientation(LinearLayout.VERTICAL);
        editor.setPadding(dp(context, 10), 0, dp(context, 10), dp(context, 12));
        parent.addView(editor, new ViewGroup.LayoutParams(-1, -2));
        return editor;
    }

    static final class Field
    {
        final AppCompatEditText input;
        final MaterialCheckBox active;

        Field(AppCompatEditText input, MaterialCheckBox active)
        {
            this.input = input;
            this.active = active;
        }
    }

    static Field field(LinearLayout editor, String label, int maximumLength, boolean withActive)
    {
        Context context = editor.getContext();
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(context, 48));
        rowParams.bottomMargin = dp(context, 4);
        editor.addView(row, rowParams);

        TextView caption = text(context, label);
        caption.setGravity(Gravity.CENTER);
        caption.setTextSize(11);
        caption.setTextColor(context.getColor(R.color.glass_accent_text));
        caption.setBackgroundResource(R.drawable.remote_badge_blue);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(dp(context, 62), dp(context, 28));
        captionParams.rightMargin = dp(context, 6);
        row.addView(caption, captionParams);

        AppCompatEditText input = new AppCompatEditText(context);
        input.setId(View.generateViewId());
        input.setTypeface(ResourcesCompat.getFont(context, R.font.app_sans));
        input.setTextSize(12);
        input.setTextColor(0xffe1ebec);
        input.setHintTextColor(0xffaebec7);
        input.setIncludeFontPadding(false);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setBackgroundResource(R.drawable.management_inline_input);
        input.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setContentDescription(label);
        if (maximumLength > 0)
        {
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maximumLength)});
        }
        row.addView(input, new LinearLayout.LayoutParams(0, dp(context, 44), 1));
        caption.setLabelFor(input.getId());

        MaterialCheckBox active = null;
        if (withActive)
        {
            active = new MaterialCheckBox(context);
            active.setText("사용");
            active.setTextSize(12);
            active.setTextColor(0xffe1ebec);
            active.setIncludeFontPadding(false);
            active.setTypeface(ResourcesCompat.getFont(context, R.font.app_sans));
            active.setUseMaterialThemeColors(false);
            active.setButtonTintList(AppCompatResources.getColorStateList(context, R.color.glass_choice_tint));
            active.setContentDescription(label + " 자동 연결 사용");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(context, 80), dp(context, 44));
            params.leftMargin = dp(context, 6);
            row.addView(active, params);
        }
        else
        {
            View spacer = new View(context);
            spacer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(context, 80), dp(context, 44));
            params.leftMargin = dp(context, 6);
            row.addView(spacer, params);
        }
        return new Field(input, active);
    }

    static TextView text(Context context, String value)
    {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextColor(0xffe1ebec);
        text.setTextSize(12);
        text.setIncludeFontPadding(false);
        text.setTypeface(ResourcesCompat.getFont(context, R.font.app_sans));
        return text;
    }

    static void identify(AppCompatEditText input, int id, String description)
    {
        input.setId(id);
        input.setContentDescription(description);
        ViewGroup row = (ViewGroup) input.getParent();
        ((TextView) row.getChildAt(0)).setLabelFor(id);
    }

    static TextView error(LinearLayout parent)
    {
        TextView error = text(parent.getContext(), "");
        error.setTextColor(AppCompatResources.getColorStateList(parent.getContext(), R.color.profile_error_amber));
        error.setVisibility(View.GONE);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        parent.addView(error, new LinearLayout.LayoutParams(-1, -2));
        return error;
    }

    static View footer(ViewGroup parent, String description, View.OnClickListener listener)
    {
        Context context = parent.getContext();
        FrameLayout footer = new FrameLayout(context);
        footer.setLayoutParams(new ViewGroup.LayoutParams(-1, dp(context, 76)));
        AppCompatImageButton add = new AppCompatImageButton(context);
        add.setContentDescription(description);
        add.setImageResource(R.drawable.fab_ic_add_24dp);
        add.setImageTintList(ColorStateList.valueOf(0xffe1ebec));
        add.setBackgroundResource(R.drawable.management_inline_add);
        add.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(context, 48), dp(context, 48), Gravity.CENTER);
        footer.addView(add, params);
        add.setOnClickListener(listener);
        return footer;
    }

    static void focus(View input)
    {
        input.post(() ->
        {
            if (input.isAttachedToWindow())
            {
                input.requestFocus();
                InputMethodManager keyboard = (InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    static void hideKeyboard(View view)
    {
        InputMethodManager keyboard = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    static void focusError(View input, TextView error)
    {
        input.requestFocus();
        hideKeyboard(input);
        error.post(() ->
        {
            if (error.isAttachedToWindow() && error.getVisibility() == View.VISIBLE)
            {
                Rect bounds = new Rect();
                error.getDrawingRect(bounds);
                bounds.inset(0, -dp(error.getContext(), 8));
                error.requestRectangleOnScreen(bounds, false);
            }
        });
    }

    static String value(TextView input)
    {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
