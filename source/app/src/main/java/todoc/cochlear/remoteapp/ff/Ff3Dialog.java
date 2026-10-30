package todoc.cochlear.remoteapp.ff;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;

/**
 * 장치값과 편집값을 구분하는 FF3 상세 화면. 저장과 활성화는 별도 조작이다.
 */
public final class Ff3Dialog
{
    private final MainActivity activity;
    private final Ff3Session session;
    private final AlertDialog dialog;
    private final LinearLayout fields;
    private final ScrollView scroll;
    private final TextView state;
    private final MaterialButton reload, begin, save, abort, activate;
    private final Map<Integer, EditText> inputs = new LinkedHashMap<>();
    private final Map<Integer, TextView> dbHints = new LinkedHashMap<>();
    private boolean rebuild = true;
    private boolean editingAccepted;
    private int pendingChoice = -1;
    private final Map<Integer, MaterialButton> choices = new LinkedHashMap<>();
    private final LinearLayout[] groups = new LinearLayout[3];
    private final LinearLayout[] zoneRows = new LinearLayout[16];
    private TextView preview, validation;
    private MaterialButton goalButton;
    private Ff3Config baseline;
    private int selectedGoal;
    private boolean building;
    private boolean unusedExpanded;
    private final boolean diagnostic;

    public Ff3Dialog(MainActivity activity, boolean diagnostic)
    {
        this.activity = activity;
        this.session = activity.ff3;
        this.diagnostic = diagnostic;
        LinearLayout body = column();
        body.setPadding(dp(20), dp(4), dp(20), dp(4));
        body.setBackgroundColor(Color.TRANSPARENT);
        state = text("");
        state.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(state);
        reload = button(body, "장치에서 다시 읽기", this::reload);
        secondary(reload);
        fields = column();
        scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(fields);
        body.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout footer = column();
        body.addView(footer);
        validation = text("");
        validation.setTextColor(Color.rgb(240, 183, 164));
        footer.addView(validation);
        LinearLayout actions = new LinearLayout(activity);
        boolean wideActions = activity.getResources().getConfiguration().screenWidthDp >= 700;
        actions.setOrientation(wideActions ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        footer.addView(actions);
        begin = button(actions, "설정 바꾸기 · 보상 끄고 시작", () -> requestEditing(-1));
        save = button(actions, "변경 내용 확인 후 저장", this::save);
        activate = button(actions, "이 설정으로 소리 보상 켜기", () -> new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity)
            .setTitle("소리 변화에 따른 전원 보상을 켤까요?")
            .setMessage("저장된 설정으로 FF3를 선택합니다. 실제 보상은 연결·자극·소리 상승 조건을 만족할 때 실행됩니다.")
            .setNegativeButton("돌아가기", null).setPositiveButton("FF3 켜기", (d, w) -> session.changeMode(3)).show());
        abort = button(actions, "변경 취소 · 저장된 값으로 돌아가기", () -> new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity)
            .setTitle("저장하지 않은 변경을 취소할까요?")
            .setMessage("화면 입력과 장치에 남은 편집을 버리고 마지막 저장값을 읽습니다. 보상은 계속 꺼져 있습니다.")
            .setNegativeButton("계속 설정", null).setPositiveButton("변경 취소", (d, w) ->
            {
                rebuild = true;
                editingAccepted = false;
                session.abort();
            }).show());
        secondary(abort);
        if (wideActions)
        {
            for (MaterialButton b : new MaterialButton[]{begin, save, activate, abort})
            {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                params.setMarginEnd(dp(8));
                b.setLayoutParams(params);
            }
        }
        dialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity).setTitle(diagnostic ? "소리 보상 동작 확인 · FF3" : "소리 보상 설정 · FF3")
            .setView(body).setNegativeButton("닫기", null).create();
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> close());
        dialog.setOnKeyListener((d, key, event) ->
        {
            if (key != KeyEvent.KEYCODE_BACK)
            {
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_UP)
            {
                close();
            }
            return true;
        });
        if (dialog.getWindow() != null)
        {
            android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            dialog.getWindow().setLayout(Math.min(metrics.widthPixels - dp(24), dp(920)), Math.round(metrics.heightPixels * 0.9f));
        }
        refresh();
        if (diagnostic)
        {
            session.readDiagnostics();
        }
    }

    private int dp(int value)
    {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private LinearLayout column()
    {
        LinearLayout v = new LinearLayout(activity);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private TextView text(String value)
    {
        TextView v = new TextView(activity);
        v.setText(value);
        v.setTextSize(12);
        v.setTypeface(ResourcesCompat.getFont(activity, R.font.app_sans));
        v.setIncludeFontPadding(false);
        v.setTextColor(Color.rgb(233, 240, 242));
        v.setLineSpacing(dp(2), 1);
        v.setPadding(0, dp(6), 0, dp(6));
        return v;
    }

    private void heading(LinearLayout parent, String value)
    {
        TextView v = text(value);
        v.setTextSize(14);
        v.setTypeface(v.getTypeface(), Typeface.BOLD);
        v.setPadding(0, dp(16), 0, dp(8));
        parent.addView(v);
    }

    private MaterialButton button(LinearLayout parent, String label, Runnable action)
    {
        MaterialButton b = new MaterialButton(activity);
        b.setText(label);
        b.setTextSize(12);
        b.setTypeface(ResourcesCompat.getFont(activity, R.font.app_sans));
        b.setIncludeFontPadding(false);
        b.setAllCaps(false);
        b.setMinHeight(dp(44));
        b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}}, new int[]{Color.rgb(131, 148, 154), Color.rgb(233, 240, 242)}));
        b.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}}, new int[]{Color.argb(32, 184, 203, 210), Color.argb(190, 53, 74, 70)}));
        b.setCornerRadius(dp(12));
        b.setStrokeWidth(dp(1));
        b.setStrokeColor(ColorStateList.valueOf(Color.argb(72, 166, 194, 185)));
        b.setRippleColor(ColorStateList.valueOf(Color.argb(32, 152, 200, 191)));
        b.setOnClickListener(v -> action.run());
        parent.addView(b);
        return b;
    }

    private void secondary(MaterialButton b)
    {
        b.setBackgroundTintList(ColorStateList.valueOf(Color.argb(24, 184, 226, 211)));
        b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}}, new int[]{Color.rgb(131, 148, 154), Color.rgb(184, 226, 211)}));
    }

    public void dismiss()
    {
        dialog.dismiss();
    }

    public boolean showing()
    {
        return dialog.isShowing();
    }

    public void refresh()
    {
        if (!showing())
        {
            return;
        }
        boolean busy = session.busy(), supported = session.supported(), edit = session.editing();
        state.setText(Ff3Config.modeName(session.mode) + "\n" + session.message
            + (diagnostic ? "" : busy ? "\n장치 응답과 반영 상태를 확인 중입니다."
                                 : edit ? "\n② 값 조정 → ③ 저장 → ④ 직접 켜기 · 아직 저장 전입니다."
                                   : session.mode == 0 ? "\n저장된 값을 확인하고 직접 켜거나, 설정을 바꿀 수 있습니다." : "\n① 현재값 확인 → ② 보상 끄고 조정 → ③ 저장 → ④ 직접 켜기"));
        reload.setEnabled(!busy && !session.needsReconnect());
        begin.setText(edit ? "남아 있는 설정 이어서 바꾸기" : "설정 바꾸기 · 보상 끄고 시작");
        begin.setVisibility(diagnostic || (edit && editingAccepted) ? View.GONE : View.VISIBLE);
        save.setVisibility(!diagnostic && edit && editingAccepted ? View.VISIBLE : View.GONE);
        abort.setVisibility(!diagnostic && edit ? View.VISIBLE : View.GONE);
        begin.setEnabled(!busy && supported && session.config().complete());
        save.setEnabled(!busy && supported && edit && session.config().complete());
        abort.setEnabled(!busy && supported && edit && session.mode == 0);
        activate.setVisibility(!diagnostic && !edit && session.mode == 0 ? View.VISIBLE : View.GONE);
        activate.setEnabled(!busy && supported && session.config().complete() && (session.status & 0x19) == 0x19
            && session.file != null && session.file[9] == 0 && session.config().validate() == null);
        validation.setVisibility(diagnostic ? View.GONE : View.VISIBLE);
        if (diagnostic)
        {
            if (!busy)
            {
                fields.removeAllViews();
                heading(fields, "장치가 보고한 최근 동작");
                fields.addView(text(diagnosticsText(session.diagnostics())));
                LinearLayout detail = column();
                detail.setVisibility(View.GONE);
                secondary(button(fields, "기술 진단 펼치기 / 접기", () -> detail.setVisibility(detail.getVisibility() == View.GONE ? View.VISIBLE : View.GONE)));
                fields.addView(detail);
                detail.addView(text(engineeringText(session.diagnostics())));
            }
            return;
        }
        if (!busy && session.config().complete() && (rebuild || inputs.isEmpty()))
        {
            buildFields(session.config());
            rebuild = false;
        }
        boolean editable = !busy && edit && editingAccepted && supported && session.config().complete();
        for (Map.Entry<Integer, EditText> entry : inputs.entrySet())
        {
            entry.getValue().setEnabled(editable && entry.getKey() != 60 && entry.getKey() != 64 && entry.getKey() != 80);
        }
        boolean canChoose = !busy && supported && !session.needsReconnect() && session.config().complete();
        for (Map.Entry<Integer, MaterialButton> entry : choices.entrySet())
        {
            int index = entry.getKey();
            entry.getValue().setEnabled(canChoose);
            entry.getValue().setText(Ff3Ui.value(index, Integer.parseInt(inputs.get(index).getText().toString())) + " · 바꾸기 ▾");
        }
        if (inputs.containsKey(40) && inputs.containsKey(59))
        {
            inputs.get(40).setEnabled(editable && "2".equals(inputs.get(59).getText().toString()));
        }
        if (!busy && !edit)
        {
            validation.setText("");
        }
        if (!busy && pendingChoice >= 0)
        {
            int index = pendingChoice;
            pendingChoice = -1;
            if (editable && session.state == Ff3Session.State.EDITING)
            {
                openChoice(index);
            }
        }
    }

    private void requestEditing(int choiceIndex)
    {
        if (session.busy() || !session.supported() || session.needsReconnect() || !session.config().complete())
        {
            return;
        }
        if (session.editing())
        {
            editingAccepted = true;
            pendingChoice = choiceIndex;
            refresh();
            return;
        }
        new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity).setTitle("설정을 바꾸기 전에 보상을 끕니다")
            .setMessage("현재: " + Ff3Config.modeName(session.mode)
                + "\n설정 시작 후 값을 바꿀 수 있습니다. 변경 내용은 저장해야 반영되며, 보상은 직접 다시 켜야 합니다.")
            .setNegativeButton("돌아가기", null).setPositiveButton(session.mode == 0 ? "설정 시작 · 꺼짐 유지" : "보상 끄고 설정 시작", (d, w) ->
            {
                rebuild = true;
                editingAccepted = true;
                pendingChoice = choiceIndex;
                session.begin();
                if (!session.busy())
                {
                    pendingChoice = -1;
                }
            }).show();
    }

    private void choose(int index)
    {
        if (session.busy() || session.needsReconnect() || !session.supported() || !session.config().complete())
        {
            return;
        }
        if (!session.editing() || !editingAccepted)
        {
            requestEditing(index);
            return;
        }
        openChoice(index);
    }

    private void openChoice(int index)
    {
        if (!showing() || !inputs.containsKey(index))
        {
            return;
        }
        String[] labels = index == 59 ? Ff3Ui.FILTERS : new String[]{"사용하지 않음", "사용"};
        int selected = Integer.parseInt(inputs.get(index).getText().toString());
        new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity).setTitle(Ff3Ui.label(index))
            .setSingleChoiceItems(labels, selected, (d, which) ->
            {
                if (!session.busy() && !session.needsReconnect() && session.editing() && editingAccepted
                    && session.supported() && session.config().complete() && showing())
                {
                    inputs.get(index).setText(String.valueOf(which));
                    refresh();
                }
                d.dismiss();
            }).setNegativeButton("돌아가기", null).show();
    }

    private void field(LinearLayout parent, int index, String label, int value, int scale)
    {
        TextView name = text(label);
        parent.addView(name);
        EditText input = new EditText(activity);
        input.setId(View.generateViewId());
        name.setLabelFor(input.getId());
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(value * scale));
        input.setTypeface(ResourcesCompat.getFont(activity, R.font.app_sans));
        input.setIncludeFontPadding(false);
        input.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}}, new int[]{Color.rgb(145, 160, 167), Color.rgb(233, 240, 242)}));
        input.setHintTextColor(Color.rgb(174, 190, 199));
        input.setBackgroundTintList(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{}}, new int[]{Color.argb(64, 152, 200, 191), Color.rgb(152, 200, 191)}));
        input.setContentDescription(label);
        input.setMinHeight(dp(48));
        parent.addView(input);
        inputs.put(index, input);
        if (Ff3Ui.relativeDb(index) || (index >= 64 && index < 80))
        {
            TextView hint = text("");
            hint.setTextColor(Color.rgb(184, 226, 211));
            parent.addView(hint);
            dbHints.put(index, hint);
        }
        input.addTextChangedListener(new TextWatcher()
        {
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                if (!building)
                {
                    validation.setText("");
                    updateDbHints();
                    updatePreview();
                }
            }

            public void afterTextChanged(Editable e)
            {
            }
        });
    }

    private void setting(LinearLayout parent, int index, Ff3Config config)
    {
        LinearLayout titleRow = new LinearLayout(activity);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(titleRow);
        TextView title = text(Ff3Ui.label(index));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        if (index == 59 || index == 58)
        {
            field(parent, index, "", config.get(index), 1);
            inputs.get(index).setVisibility(View.GONE);
            MaterialButton choice = button(parent, Ff3Ui.value(index, config.get(index)), () -> choose(index));
            choices.put(index, choice);
            secondary(choice);
        }
        else
        {
            field(parent, index, "허용 범위 " + Ff3Config.MIN[index - 40] * Ff3Ui.scale(index) + "~" + Ff3Config.MAX[index - 40] * Ff3Ui.scale(index) + " · " + Ff3Ui.scale(index) + " 단위", config.get(index), Ff3Ui.scale(index));
        }
        inputs.get(index).setContentDescription(Ff3Ui.label(index));
        String help = Ff3Ui.help(index);
        int end = help.indexOf(". ");
        if (end >= 0)
        {
            end = help.indexOf(". ", end + 2);
        }
        parent.addView(text(end < 0 ? help : help.substring(0, end)));
    }

    private void buildFields(Ff3Config config)
    {
        building = true;
        baseline = config.copy();
        fields.removeAllViews();
        inputs.clear();
        choices.clear();
        dbHints.clear();
        goalButton = button(fields, "", () -> new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity).setTitle("무엇을 조정할까요?")
            .setSingleChoiceItems(Ff3Ui.GOALS, selectedGoal, (d, which) ->
            {
                showGoal(which);
                d.dismiss();
            }).show());
        secondary(goalButton);
        preview = text("");
        for (int g = 0; g < 3; g++)
        {
            groups[g] = column();
            fields.addView(groups[g]);
        }
        heading(groups[0], "작은 변화 감지 · 먼저 볼 3가지");
        LinearLayout basics = new LinearLayout(activity);
        boolean wide = activity.getResources().getConfiguration().screenWidthDp >= 700;
        basics.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        groups[0].addView(basics);
        for (int i : new int[]{43, 45, 59})
        {
            LinearLayout cell = column();
            cell.setPadding(0, 0, wide ? dp(12) : 0, dp(8));
            basics.addView(cell, wide ? new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1)
                : new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            setting(cell, i, config);
        }
        LinearLayout advanced = column();
        advanced.setVisibility(View.GONE);
        button(groups[0], "급격한 변화·평소 소리 기준 설정 펼치기 / 접기", () -> advanced.setVisibility(advanced.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
        groups[0].addView(advanced);
        for (int i : new int[]{40, 44, 42, 51, 53})
        {
            setting(advanced, i, config);
        }
        heading(groups[1], "얼마나 전원 보상을 추가할까요?");
        groups[1].addView(text("보상량을 늘리는 설정입니다. 감지 증가 폭과는 별개입니다. 1스텝은 전원 설정 25 ㎷ 변화이며 실측 전압은 아닙니다."));
        setting(groups[1], 50, config);
        setting(groups[1], 55, config);
        heading(groups[1], "소리 크기별 구간");
        groups[1].addView(text("시작 크기: 해당 구간이 시작되는 소리 크기(dBSPL). 추가 보상: 바로 아래 구간에서 올라갈 때 더할 스텝. 사건 유지: 같은 소리 사건을 묶는 시간이며 전압을 내리는 타이머는 아닙니다."));
        groups[1].addView(text("경계 아래 배수는 직전 구간 경계와 비교한 음압비입니다. 마이크 원시값은 장치 보정표에 따라 달라집니다."));
        button(groups[1], "사용하지 않는 구간 표시 / 숨기기", () ->
        {
            unusedExpanded = !unusedExpanded;
            updatePreview();
        });
        for (int i = 0; i < 16; i++)
        {
            LinearLayout zone = column();
            zoneRows[i] = zone;
            groups[1].addView(zone);
            zone.addView(text("구간 " + (i + 1) + (i == 0 ? " · 가장 작은 소리 전체, 시작/추가량은 0" : "")));
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            zone.addView(row);
            LinearLayout lower = column(), delta = column(), hold = column();
            for (LinearLayout cell : new LinearLayout[]{lower, delta, hold})
            {
                row.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                cell.setPadding(dp(2), 0, dp(2), 0);
            }
            field(lower, 64 + i, "시작 dBSPL", config.get(64 + i), 1);
            field(delta, 80 + i, "추가 스텝", config.get(80 + i), 1);
            field(hold, 96 + i, "소리 묶기 ms", config.get(96 + i), 10);
            inputs.get(64 + i).setContentDescription("구간 " + (i + 1) + " 시작 소리 크기 dBSPL");
            inputs.get(80 + i).setContentDescription("구간 " + (i + 1) + " 추가 보상 스텝");
            inputs.get(96 + i).setContentDescription("구간 " + (i + 1) + " 소리 묶기 ms, 10ms 단위");
        }
        heading(groups[2], "다음 소리를 다시 감지할 조건");
        groups[2].addView(text("작은 변화의 최초 감지는 첫 번째 목적에서 조정합니다. 여기서는 소리가 작아진 뒤 새 기준을 준비하고 요청을 처리하는 조건을 정합니다."));
        for (int i : new int[]{41, 46, 47, 52, 56, 58, 57})
        {
            setting(groups[2], i, config);
        }
        heading(groups[2], "요청 처리 시간·마이크 보정");
        for (int i : new int[]{48, 49, 54, 60})
        {
            setting(groups[2], i, config);
        }
        heading(fields, "현재 입력으로 이해하기");
        fields.addView(preview);
        building = false;
        updateDbHints();
        showGoal(selectedGoal);
        updatePreview();
    }

    private void updateDbHints()
    {
        if (building || baseline == null)
        {
            return;
        }
        Ff3Config c = baseline.copy();
        for (int i = 64; i < 80; i++)
        {
            try
            {
                c.set(i, Integer.parseInt(inputs.get(i).getText().toString()));
            }
            catch (NumberFormatException e)
            {
                c.set(i, -1);
            }
        }
        for (Map.Entry<Integer, TextView> entry : dbHints.entrySet())
        {
            int i = entry.getKey();
            String hint;
            try
            {
                int db = Integer.parseInt(inputs.get(i).getText().toString());
                int min = i < 64 ? Ff3Config.MIN[i - 40] : 0, max = i < 64 ? Ff3Config.MAX[i - 40] : 110;
                if (db < min || db > max)
                {
                    throw new NumberFormatException();
                }
                hint = i < 64 ? Ff3Ui.value(i, db) : Ff3Ui.zoneComparison(c, i);
            }
            catch (NumberFormatException e)
            {
                hint = "허용 범위의 dB 정수를 입력하면 배수를 표시합니다";
            }
            entry.getValue().setText(hint);
        }
    }

    private void showGoal(int goal)
    {
        selectedGoal = goal;
        goalButton.setText("조정 목적: " + Ff3Ui.GOALS[goal] + " ▾");
        for (int i = 0; i < 3; i++)
        {
            groups[i].setVisibility(i == goal ? View.VISIBLE : View.GONE);
        }
        updatePreview();
        scroll.post(() -> scroll.smoothScrollTo(0, 0));
    }

    private Ff3Config draft(boolean report)
    {
        if (baseline == null)
        {
            return null;
        }
        Ff3Config desired = baseline.copy();
        for (Map.Entry<Integer, EditText> entry : inputs.entrySet())
        {
            int i = entry.getKey(), scale = Ff3Ui.scale(i);
            try
            {
                int value = Integer.parseInt(entry.getValue().getText().toString());
                int min = i <= 60 ? Ff3Config.MIN[i - 40] * scale : 0;
                int max = i <= 60 ? Ff3Config.MAX[i - 40] * scale : (i < 80 ? 110 : i < 96 ? 40 : 2550);
                if (value < min || value > max || value % scale != 0)
                {
                    throw new IllegalArgumentException();
                }
                desired.set(i, value / scale);
            }
            catch (IllegalArgumentException e)
            {
                if (report)
                {
                    revealInput(i);
                    entry.getValue().setError("범위와 " + scale + " 단위 정수를 확인하세요.");
                }
                return null;
            }
        }
        return desired;
    }

    private void updatePreview()
    {
        if (building || preview == null)
        {
            return;
        }
        Ff3Config c = draft(false);
        if (c == null)
        {
            preview.setText("입력값을 완성하면 현재 조정 내용을 보여 줍니다.");
            return;
        }
        if (selectedGoal == 0)
        {
            preview.setText(Ff3Ui.sensitivity(c));
        }
        else if (selectedGoal == 1)
        {
            preview.setText("구간 추가 보상 합계 " + c.cumulative(c.get(55) - 1) + "스텝 / 보상 한도 " + c.get(50) + "스텝\n사용하지 않는 행은 세 값 모두 0으로 맞춰야 합니다.");
        }
        else
        {
            preview.setText("값을 바꾸면 화면 입력에만 반영됩니다. 저장 후 직접 켜야 장치 동작에 사용됩니다.");
        }
        for (int i = 0; i < 16; i++)
        {
            if (zoneRows[i] != null)
            {
                zoneRows[i].setVisibility(i < c.get(55) || unusedExpanded ? View.VISIBLE : View.GONE);
            }
        }
        if (inputs.containsKey(40))
        {
            inputs.get(40).setEnabled(!session.busy() && editingAccepted && session.editing() && c.get(59) == 2);
        }
    }

    private void save()
    {
        if (session.busy() || !editingAccepted || !session.editing() || !session.config().complete())
        {
            return;
        }
        Ff3Config desired = draft(true);
        if (desired == null)
        {
            return;
        }
        String error = desired.validate();
        if (error != null)
        {
            int badIndex = error.contains("구간") || error.contains("보상 한도") ? 50 : error.contains("전원 조정") ? 48 : error.contains("급격한") ? 44 : 43;
            for (int i = 40; i <= 60; i++)
            {
                if (error.startsWith(Ff3Ui.label(i)))
                {
                    badIndex = i;
                    break;
                }
            }
            revealInput(badIndex);
            validation.setText(error);
            return;
        }
        new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity).setTitle("이 변경 내용으로 저장할까요?")
            .setMessage(Ff3Ui.changes(baseline, desired) + "\n변경 항목만 장치에 보냅니다. 저장 후에도 보상은 꺼져 있으며 직접 켤 수 있습니다.")
            .setNegativeButton("더 수정", null).setPositiveButton("저장 · 꺼짐 유지", (d, w) ->
            {
                if (session.busy() || !session.editing() || !session.config().complete())
                {
                    return;
                }
                rebuild = true;
                session.save(desired);
            }).show();
    }

    private void revealInput(int index)
    {
        showGoal(Ff3Ui.goal(index));
        unusedExpanded = true;
        updatePreview();
        EditText input = inputs.get(index);
        android.view.ViewParent parent = input.getParent();
        while (parent instanceof View && parent != fields)
        {
            ((View) parent).setVisibility(View.VISIBLE);
            parent = parent.getParent();
        }
        input.requestFocus();
        scroll.post(() -> input.requestRectangleOnScreen(new android.graphics.Rect(0, 0, input.getWidth(), input.getHeight()), false));
    }

    private boolean hasDraftChanges()
    {
        Ff3Config d = draft(false);
        return editingAccepted && baseline != null && (d == null || !baseline.sameValues(d));
    }

    private void reload()
    {
        Runnable action = () ->
        {
            rebuild = true;
            if (diagnostic)
            {
                session.readDiagnostics();
            }
            else
            {
                session.load();
            }
        };
        if (!diagnostic && hasDraftChanges())
        {
            new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity).setTitle("화면 입력을 버리고 다시 읽을까요?")
                .setMessage("아직 저장하지 않은 화면 입력은 사라집니다. 장치에 남은 편집을 취소하는 동작은 아닙니다.")
                .setNegativeButton("입력 유지", null).setPositiveButton("다시 읽기", (d, w) -> action.run()).show();
        }
        else
        {
            action.run();
        }
    }

    private void close()
    {
        if (session.busy())
        {
            new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity).setTitle("장치와 통신 중입니다")
                .setMessage("창을 닫아도 진행 중인 장치 요청은 취소되지 않습니다. 완료 상태는 다시 열어 확인할 수 있습니다.")
                .setNegativeButton("완료 기다리기", null).setPositiveButton("창만 닫기", (d, w) -> dismiss()).show();
            return;
        }
        if (!diagnostic && hasDraftChanges())
        {
            new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(activity).setTitle("저장하지 않고 닫을까요?")
                .setMessage("화면에서 바꾼 값은 사라집니다. 장치 편집은 남고 보상은 꺼져 있습니다. 취소하려면 설정 화면의 변경 취소를 사용하세요.")
                .setNegativeButton("계속 설정", null).setPositiveButton("저장 없이 닫기", (d, w) -> dismiss()).show();
        }
        else
        {
            dismiss();
        }
    }

    private static int u(byte[] p, int i)
    {
        return p[i] & 255;
    }

    private static String zone(byte[] p, int i)
    {
        return u(p, i) < 16 ? String.valueOf(u(p, i) + 1) : "미확인";
    }

    public static String diagnosticsText(Map<Integer, byte[]> data)
    {
        byte[] last = data.get(113), counts = data.get(114), input = data.get(112);
        if (last == null || counts == null || input == null)
        {
            return "동작을 장치에서 읽어 주세요. 이 화면은 자동으로 갱신되지 않습니다.";
        }
        return "입력 준비: " + (u(input, 6) == 1 ? "완료" : "준비되지 않음") + " · 현재 소리 구간 " + zone(input, 3)
            + "\n최근 결과: " + Ff3Ui.resultName(u(last, 5)) + " · 추가 " + u(last, 4) + "스텝"
            + "\n아직 남아 있는 전원 보상: " + u(last, 1) + "스텝"
            + "\n보상 기록: " + (u(last, 13) == 1 ? "유효" : "확인 필요") + " · " + Ff3Config.modeName(u(last, 14))
            + "\n성공한 전원 보상 쓰기: " + Ff3Session.unsigned(counts, 3, 3) + "회"
            + "\n요청 만료: " + Ff3Session.unsigned(counts, 9, 3) + "회 · 보류 시도: " + Ff3Session.unsigned(counts, 12, 3) + "회"
            + "\n\n다시 읽은 뒤 성공 횟수가 늘었는지 비교하세요. 횟수는 누적값이며 최근 결과가 새 소리마다 바뀌는 것은 아닙니다. 장치 보고값으로, 실측 전압이나 보호 성능을 뜻하지 않습니다.";
    }

    private static String engineeringText(Map<Integer, byte[]> data)
    {
        StringBuilder s = new StringBuilder("마지막 채택 상태입니다. Raw는 audioInputMax이며 dBSPL이 아닙니다.\n");
        for (Map.Entry<Integer, byte[]> e : data.entrySet())
        {
            byte[] p = e.getValue();
            s.append('\n');
            switch (e.getKey())
            {
                case 112:
                    s.append("Raw ").append(Ff3Session.unsigned(p, 0, 3)).append(" · 구간 ").append(zone(p, 3))
                        .append(" / 기준 ").append(zone(p, 4)).append(" / 최고 ").append(zone(p, 5)).append("\nReady ").append(u(p, 6))
                        .append(" · 필터 ").append(u(p, 14)).append(" · 새 요청 ").append(u(p, 13)).append("\nsourceTick ").append(Ff3Session.unsigned(p, 7, 3))
                        .append(" · configToken ").append(Ff3Session.unsigned(p, 10, 3));
                    break;
                case 113:
                    s.append("PMIC 설정 레벨 ").append(u(p, 0)).append(" · 잔여 보상 R ").append(u(p, 1))
                        .append("\n요구 U ").append(u(p, 2)).append(" · 처리 요구 기준 A ").append(u(p, 3)).append(" · 실제 Δ ").append(u(p, 4))
                        .append("\n결과 ").append(u(p, 5)).append(" (0 초기/1 busy/2 가산/3 무가산/4 만료/5 실패)")
                        .append("\nevent ").append(Ff3Session.unsigned(p, 6, 3)).append(" · age ").append(u(p, 9)).append("ms · session ").append(Ff3Session.unsigned(p, 10, 3))
                        .append("\n장부 ").append(u(p, 13)).append(" · 모드 ").append(Ff3Config.modeName(u(p, 14)));
                    break;
                case 114:
                case 115:
                    String[] labels = e.getKey() == 114 ? new String[]{"스냅샷", "FF 쓰기", "무가산 종결", "TTL 만료", "busy 시도"} : new String[]{"장부 무효화", "stale", "후보 ACK", "session 갱신", "설정 공개"};
                    for (int j = 0; j < 5; j++)
                    {
                        s.append(labels[j]).append(' ').append(Ff3Session.unsigned(p, j * 3, 3)).append('\n');
                    }
                    break;
                case 116:
                    s.append("파일 schema ").append(u(p, 0)).append(" · generation ").append(Ff3Session.unsigned(p, 1, 4))
                        .append(" · CRC ").append(Long.toHexString(Ff3Session.unsigned(p, 5, 4))).append(" · 확인 ").append(p[9] == 0 ? "성공" : "불명·실패");
                    break;
                case 117:
                    s.append("하강 결과/이유 ").append(u(p, 0)).append('/').append(u(p, 6)).append(" (0 없음/1 High/2 Low/3 NoBacktel)")
                        .append("\nPMIC ").append(u(p, 1)).append(" → ").append(u(p, 2)).append(" · 실제 하강 ").append(u(p, 3))
                        .append(" · R ").append(u(p, 4)).append(" → ").append(u(p, 5)).append("\n관찰 seq ").append(Ff3Session.unsigned(p, 7, 3))
                        .append(" · wallCount ").append(Ff3Session.unsigned(p, 10, 3)).append(" · 장부 ").append(u(p, 13));
                    break;
                case 118:
                    s.append("후보 ").append(p[14] == 0 ? "비활성" : zone(p, 3)).append(" · ACK 일치 ").append(u(p, 4))
                        .append("\nevent ").append(Ff3Session.unsigned(p, 0, 3)).append(" · 시작 feedbackSeq ").append(Ff3Session.unsigned(p, 5, 3))
                        .append(" · feedback 결과 ").append(u(p, 8)).append("\nLow 경과 ").append(Ff3Session.unsigned(p, 9, 2)).append("ms · sourceTick ").append(Ff3Session.unsigned(p, 11, 3));
                    break;
            }
            s.append('\n');
        }
        return s.append("\n23bit 카운터는 순환합니다. sourceTick과 wallCount는 직접 비교하지 않습니다.").toString();
    }
}
