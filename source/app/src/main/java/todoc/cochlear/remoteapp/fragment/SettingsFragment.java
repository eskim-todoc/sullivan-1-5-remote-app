package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import todoc.cochlear.remoteapp.shared_preferences.LockScreen;
import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment
{
    private String TAG = "TODOC_" + SettingsFragment.class.getSimpleName();

    public SettingsFragment()
    {
        // Required empty public constructor
    }

    private ActivityMainBinding mMainBinding;
    private FragmentSettingsBinding mSettingBinding;

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mSettingBinding = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainBinding = ((MainActivity) requireContext()).mBinding;
        mMainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mMainBinding.toolbar.getMenu().findItem(R.id.settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Default_Headline6);
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_settings));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mSettingBinding = FragmentSettingsBinding.inflate(inflater, container, false);
        View view = mSettingBinding.getRoot();

        mSettingBinding.switchLockScreen.setOnCheckedChangeListener(mLockScreenSwitchListener);
        updateLockScreenSwitch();

        initManagementButtons();
        initSupportButtons();

        return view;
    }

    //
    // 화면잠금 스위치 관련
    //
    private void updateLockScreenSwitch()
    {
        mSettingBinding.switchLockScreen.setChecked(LockScreen.isEnabled(requireContext()));
    }

    private CompoundButton.OnCheckedChangeListener mLockScreenSwitchListener = new CompoundButton.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b)
        {
            if (compoundButton.getId() == R.id.switch_lock_screen)
            {
                LockScreen.setEnable(getContext(), b);
            }
        }
    };

    //
    // 고객지원 버튼 관련
    //
    private void initSupportButtons()
    {
        mSettingBinding.buttonManual.setOnClickListener(mSupportButtonClickListener);
    }

    private View.OnClickListener mSupportButtonClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            if (view.getId() == mSettingBinding.buttonManual.getId())
            {
                ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new ManualFragment()).commitAllowingStateLoss();
            }
        }
    };

    //
    // 등록관리 버튼 관련
    //
    private void initManagementButtons()
    {
        mSettingBinding.buttonUser.setOnClickListener(mManagementButtonClickListener);
        mSettingBinding.buttonDevice.setOnClickListener(mManagementButtonClickListener);
    }

    private View.OnClickListener mManagementButtonClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            if (view.getId() == mSettingBinding.buttonUser.getId())
            {
                ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new UserFragment()).commitAllowingStateLoss();
            }
            else if (view.getId() == mSettingBinding.buttonDevice.getId())
            {
                ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new DeviceFragment()).commitAllowingStateLoss();
            }
        }
    };
}