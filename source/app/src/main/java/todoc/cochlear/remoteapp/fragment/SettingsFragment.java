package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment
{
    //static private final String TAG = "TODOC_" + SettingsFragment.class.getSimpleName();

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
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        //mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Default_Headline6);
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_settings));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mSettingBinding = FragmentSettingsBinding.inflate(inflater, container, false);
        return mSettingBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        updateLockScreenSwitch();

        initManagementButtons();
        initSupportButtons();
    }

    //
    // 화면잠금 스위치 관련
    //
    private void updateLockScreenSwitch()
    {
        // 1) 리스너 등록 전에 먼저 상태 값을 설정하고,
        mSettingBinding.switchLockScreen.setChecked(((MainActivity) requireActivity()).mLockScreen.isEnabled());
        // 2) 리스너를 등록해야 초기 상태값 설정에 따른 리스너 호출이 발생하지 않는다.
        mSettingBinding.switchLockScreen.setOnCheckedChangeListener((compoundButton, b) ->
                {
                    // 장시간 미사용 핸들러 업데이트
                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                    ((MainActivity) requireActivity()).mLockScreen.setEnable(requireContext(), b);
                }
        );
    }

    //
    // 고객지원 버튼 관련
    //
    private void initSupportButtons()
    {
        mSettingBinding.buttonManual.setOnClickListener(mSupportButtonClickListener);
    }

    private final View.OnClickListener mSupportButtonClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

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

    private final View.OnClickListener mManagementButtonClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

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