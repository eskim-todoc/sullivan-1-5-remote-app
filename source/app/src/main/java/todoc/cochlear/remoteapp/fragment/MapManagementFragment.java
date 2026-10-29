package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import todoc.cochlear.remoteapp.activity.MainActivity;

public class MapManagementFragment extends Fragment {
    private TextView result;
    private AlertDialog warningDialog;
    private final List<AppCompatButton> actions = new ArrayList<>();

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle saved) {
        MainActivity activity = (MainActivity) requireActivity();
        ManagementUi.toolbar(activity, "맵 관리");
        LinearLayout body = ManagementUi.body(requireContext());
        add(body, "16채널 시험 맵", "현재 맵 파일을 MRI 16채널 시험 맵으로 교체합니다.", 0x10);
        add(body, "32채널 시험 맵", "현재 맵 파일을 MRI 32채널 시험 맵으로 교체합니다.", 0x20);
        add(body, "기본 맵 재생성", "현재 맵 파일을 펌웨어의 기본 맵으로 다시 만듭니다. 전체 사용자 슬롯 복원과는 다릅니다.", 0xff);
        add(body, "맵·사용자 공장 복원", "전체 사용자 슬롯의 등록 정보·설정·맵을 제조 기본값으로 덮어씁니다. 개인 맵 백업을 되살리는 기능이 아닙니다.", -1);
        result = ManagementUi.text(requireContext(), "", 12);
        result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(result);
        return ManagementUi.scroll(body);
    }

    private void add(LinearLayout body, String title, String warning, int value) {
        LinearLayout panel = ManagementUi.panel(body);
        ManagementUi.heading(panel, title);
        panel.addView(ManagementUi.text(requireContext(), warning, 12));
        actions.add(ManagementUi.action(panel, "진행…", view -> showWarning(title, warning, value)));
    }

    private void showWarning(String title, String warning, int value) {
        if (!isAdded() || getView() == null || result == null) return;
        if (warningDialog != null) warningDialog.dismiss();
        warningDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(warning)
                .setNegativeButton("취소", null)
                .setPositiveButton("진행", (dialog, which) -> {
                    if (!isAdded() || getView() == null || result == null) return;
                    setPending(true);
                    result.setText("장치 응답 확인 중…");
                    MainActivity activity = (MainActivity) requireActivity();
                    if (value < 0) activity.restoreFactoryMaps();
                    else activity.requestMapAction(value);
                }).create();
        warningDialog.setOnDismissListener(dialog -> warningDialog = null);
        warningDialog.show();
    }

    private void setPending(boolean pending) {
        for (AppCompatButton action : actions) action.setEnabled(!pending);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        super.onViewCreated(view, saved);
        ((MainActivity) requireActivity()).managementResult.observe(getViewLifecycleOwner(), value -> {
            if (value == null) {
                setPending(false);
            } else if ("map".equals(value.operation) || "factory-map".equals(value.operation)) {
                setPending(false);
                result.setText(value.message);
            }
        });
    }

    @Override public void onDestroyView() {
        if (warningDialog != null) {
            warningDialog.dismiss();
            warningDialog = null;
        }
        actions.clear();
        result = null;
        super.onDestroyView();
    }
}