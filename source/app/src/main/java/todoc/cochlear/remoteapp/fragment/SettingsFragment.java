package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.params.Status;

/**
 * Flat top-level More menu; child pages retain their own back navigation.
 */
public class SettingsFragment extends Fragment
{
    private todoc.cochlear.remoteapp.ui.DeviceRail deviceRail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
    {
        MainActivity activity = (MainActivity) requireActivity();
        ManagementUi.toolbar(activity, "세부 기능");
        activity.mBinding.toolbar.setNavigationIcon(null);
        activity.mBinding.toolbar.setNavigationOnClickListener(null);
        activity.mBinding.toolbar.setNavigationContentDescription((CharSequence) null);
        activity.mBinding.toolbarNavigationMessage.setText("");
        activity.mBinding.toolbarNavigationMessage.setOnClickListener(null);
        activity.mBinding.toolbarNavigationMessage.setVisibility(View.GONE);
        LinearLayout body = ManagementUi.body(requireContext());
        deviceRail = ManagementUi.deviceRail(activity, body, () ->
        {
        });
        menuItem(body, "제조번호 · 페어링키",
            clicked -> open(new DeviceManagementFragment()));
        menuItem(body, "맵 관리",
            clicked -> open(new MapManagementFragment()));
        menuItem(body, "등록 관리",
            clicked -> activity.replaceFragment(Status.TypeOfFragment.REGISTRY));
        menuItem(body, "통신 로그",
            clicked -> open(new LogFragment()));
        return ManagementUi.scroll(body);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved)
    {
        super.onViewCreated(view, saved);
        ((MainActivity) requireActivity()).getConnectionStates().observe(getViewLifecycleOwner(), rows -> deviceRail.render(rows));
    }

    @Override
    public void onDestroyView()
    {
        deviceRail = null;
        super.onDestroyView();
    }

    private void menuItem(LinearLayout body, String title, View.OnClickListener listener)
    {
        LinearLayout item = new LinearLayout(requireContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(ManagementUi.dp(requireContext(), 12), 0, ManagementUi.dp(requireContext(), 12), 0);
        item.setBackgroundResource(R.drawable.app_glass_action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = ManagementUi.dp(requireContext(), 8);
        body.addView(item, params);
        ManagementUi.menuRow(item, 0, title, "", listener);
    }

    private void open(Fragment fragment)
    {
        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.frame, fragment).commit();
    }
}
