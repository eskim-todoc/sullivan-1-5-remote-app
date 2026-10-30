package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;

public class MapManagementFragment extends Fragment
{
    private TextView result;
    private AlertDialog warningDialog;
    private String targetSessionId;
    private todoc.cochlear.remoteapp.ui.DeviceRail deviceRail;
    private final List<AppCompatButton> actions = new ArrayList<>();
    private final List<TextView> resetBadges = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle saved)
    {
        MainActivity activity = (MainActivity) requireActivity();
        targetSessionId = activity.getSelectedSessionId();
        ManagementUi.toolbar(activity, "맵 관리");
        LinearLayout body = ManagementUi.body(requireContext());
        deviceRail = ManagementUi.deviceRail(activity, body, () ->
        {
            if (warningDialog != null) warningDialog.dismiss();
            targetSessionId = activity.getSelectedSessionId();
            result.setText("");
        });
        LinearLayout resetPanel = ManagementUi.panel(body);
        ManagementUi.heading(resetPanel, "초기화 맵");
        addReset(resetPanel, "공장 초기화", "전체 사용자 슬롯의 등록 정보·설정·맵을 제조 기본값으로 덮어씁니다. 개인 맵 백업을 되살리는 기능이 아닙니다.", -1,
            "TODOC_OTE L", R.drawable.remote_badge_mint, 0xffc8e4d7);
        addReset(resetPanel, "매핑 초기화", "현재 맵 파일을 펌웨어의 기본 맵으로 다시 만듭니다. 전체 사용자 슬롯 복원과는 다릅니다.", 0xff,
            "TODOC_OTE F", R.drawable.remote_badge_peach, 0xffe9c6aa);
        LinearLayout testPanel = ManagementUi.panel(body);
        ManagementUi.heading(testPanel, "MRI 시험 맵");
        add(testPanel, "16채널 시험용", "현재 맵 파일을 MRI 16채널 시험 맵으로 교체합니다.", 0x10);
        add(testPanel, "32채널 시험용", "현재 맵 파일을 MRI 32채널 시험 맵으로 교체합니다.", 0x20);
        result = ManagementUi.text(requireContext(), "", 12);
        result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(result);
        return ManagementUi.scroll(body);
    }

    private void add(LinearLayout panel, String title, String warning, int value)
    {
        AppCompatButton button = ManagementUi.action(panel, title, view -> showWarning(title, warning, value));
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setPadding(ManagementUi.dp(requireContext(), 12), 0, ManagementUi.dp(requireContext(), 12), 0);
        actions.add(button);
    }

    private void addReset(LinearLayout panel, String title, String warning, int value,
                          String label, int background, int color)
    {
        FrameLayout row = new FrameLayout(requireContext());
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, ManagementUi.dp(requireContext(), 44));
        rowParams.setMargins(0, ManagementUi.dp(requireContext(), 4), 0, ManagementUi.dp(requireContext(), 4));
        panel.addView(row, rowParams);

        AppCompatButton button = ManagementUi.action(panel, title, view -> showWarning(title, warning, value));
        panel.removeView(button);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setContentDescription(title + ", " + label);
        row.addView(button, new FrameLayout.LayoutParams(-1, -1));
        actions.add(button);

        TextView badge = ManagementUi.text(requireContext(), label, 11);
        badge.setSingleLine(true);
        badge.setEllipsize(TextUtils.TruncateAt.END);
        badge.setMaxWidth(ManagementUi.dp(requireContext(), 160));
        badge.setTextColor(color);
        badge.setGravity(Gravity.CENTER);
        badge.setBackgroundResource(background);
        badge.setPadding(ManagementUi.dp(requireContext(), 8), ManagementUi.dp(requireContext(), 5),
            ManagementUi.dp(requireContext(), 8), ManagementUi.dp(requireContext(), 5));
        badge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        badge.setClickable(false);
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL);
        badgeParams.rightMargin = ManagementUi.dp(requireContext(), 10);
        row.addView(badge, badgeParams);
        int badgeWidth = Math.min(ManagementUi.dp(requireContext(), 160),
            (int) Math.ceil(badge.getPaint().measureText(label)) + ManagementUi.dp(requireContext(), 16));
        button.setPadding(ManagementUi.dp(requireContext(), 12), 0,
            badgeWidth + ManagementUi.dp(requireContext(), 20), 0);
        resetBadges.add(badge);
    }

    private void showWarning(String title, String warning, int value)
    {
        if (!isAdded() || getView() == null || result == null)
        {
            return;
        }
        if (warningDialog != null)
        {
            warningDialog.dismiss();
        }
        final String warningTarget = targetSessionId;
        MainActivity owner = (MainActivity) requireActivity();
        todoc.cochlear.remoteapp.connection.DeviceBadgeInfo badge = owner.getSessionDeviceBadgeInfo(warningTarget);
        String targetText = "대상: " + badge.userName + " · " + badge.wearingPosition + " · " + badge.serialNumber;
        warningDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(targetText + "\n\n" + warning)
            .setNegativeButton("취소", null)
            .setPositiveButton("진행", (dialog, which) ->
            {
                if (!isAdded() || getView() == null || result == null)
                {
                    return;
                }
                setPending(true);
                result.setText("장치 응답 확인 중…");
                MainActivity activity = (MainActivity) requireActivity();
                if (!Objects.equals(warningTarget, activity.getSelectedSessionId()))
                {
                    setPending(false);
                    result.setText("제어 대상이 바뀌었습니다. 화면을 다시 여세요.");
                    return;
                }
                if (value < 0)
                {
                    activity.restoreFactoryMaps();
                }
                else
                {
                    activity.requestMapAction(value);
                }
            }).create();
        warningDialog.setOnDismissListener(dialog -> warningDialog = null);
        warningDialog.show();
    }

    private void setPending(boolean pending)
    {
        for (AppCompatButton action : actions)
        {
            action.setEnabled(!pending);
        }
        for (TextView badge : resetBadges)
        {
            badge.setAlpha(pending ? 0.45f : 1f);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved)
    {
        super.onViewCreated(view, saved);
        MainActivity activity = (MainActivity) requireActivity();
        activity.getConnectionStates().observe(getViewLifecycleOwner(), rows -> deviceRail.render(rows));
        activity.getSelectedManagementResult().observe(getViewLifecycleOwner(), value ->
        {
            if (!Objects.equals(targetSessionId, activity.getSelectedSessionId()))
            {
                return;
            }
            if (value == null)
            {
                setPending(false);
            }
            else if ("map".equals(value.operation) || "factory-map".equals(value.operation))
            {
                setPending(false);
                result.setText(value.message);
            }
        });
    }

    @Override
    public void onDestroyView()
    {
        if (warningDialog != null)
        {
            warningDialog.dismiss();
            warningDialog = null;
        }
        actions.clear();
        resetBadges.clear();
        deviceRail = null;
        result = null;
        super.onDestroyView();
    }
}
