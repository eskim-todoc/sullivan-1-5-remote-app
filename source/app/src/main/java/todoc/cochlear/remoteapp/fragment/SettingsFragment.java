package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentSettingsBinding;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.params.Status;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;

public class SettingsFragment extends Fragment
{
    //static private final String TAG = "TODOC_" + SettingsFragment.class.getSimpleName();

    public SettingsFragment()
    {
        // Required empty public constructor
    }

    private ActivityMainBinding mMainBinding;
    private FragmentSettingsBinding mSettingBinding;
    StatusViewModel mStatusViewModel;

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
        mMainBinding.toolbarNavigationMessage.setText("주화면");
        mMainBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
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

        mStatusViewModel = new ViewModelProvider(requireActivity()).get(StatusViewModel.class);

        updateLockScreenSwitch();

        initManagementButtons();
        initSupportButtons();
        initCheckConnOteInfoButton();
        initShareOteInfoButton();
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

                    //((MainActivity) requireActivity()).mLockScreen.setEnable(requireContext(), b);
                    ((MainActivity) requireActivity()).mLockScreen.setEnable(b);
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
                /*
                ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new ManualFragment()).commitAllowingStateLoss();
                */
                ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.MANUAL);
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
                /*
                ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new UserFragment()).commitAllowingStateLoss();
                */
                ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.USER_LIST);
            }
            else if (view.getId() == mSettingBinding.buttonDevice.getId())
            {
                /*
                ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new DeviceFragment()).commitAllowingStateLoss();
                */
                ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.DEVICE_LIST);
            }
        }
    };

    //
    // 연결된 외부기 정보 확인 버튼 관련
    //
    private void initCheckConnOteInfoButton()
    {
        mSettingBinding.settingsCheckConnOteInfoButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (Status.instance().lastDialog != null && Status.instance().lastDialog.isShowing())
            {
                Status.instance().lastDialog.dismiss();
            }

            if (Status.instance().connectionState != Status.CONNECTION_STATE_DISCONNECTED)
            {
                String msg;
                String ear;
                String serial = Status.instance().connectedDevice.serialNumber;
                String name = Status.instance().connectedUser.name.substring(0, Status.instance().connectedUser.name.length() - 2);
                String fwVersion = "" + mStatusViewModel.getFwVerUpper() + "." + mStatusViewModel.getFwVerLower();

                if (Status.instance().connectedUser.ear.equals(EntityUser.EAR_LEFT))
                {
                    ear = "왼쪽";
                }
                else
                {
                    ear = "오른쪽";
                }

                msg = "제조번호 : " + serial + "\n"
                        + "사용자 : " + name + "\n"
                        + "착용위치 : " + ear + "\n"
                        + "펌웨어 버전 : " + fwVersion;

                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("외부기 정보")
                                .setMessage(msg)
                                .setPositiveButton("닫기", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                })
                                .setCancelable(false)
                                .create();

                Status.instance().lastDialog.show();
            }
            else
            {
                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setMessage("외부기와 연결되지 않았습니다.")
                                .setPositiveButton("닫기", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                })
                                .setCancelable(false)
                                .create();

                Status.instance().lastDialog.show();
            }
        });
    }

    //
    // 외부기 공유 버튼 관련
    //
    private void initShareOteInfoButton()
    {
        mSettingBinding.settingsShareOteInfoButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            /*
            ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new ShareFragment()).commitAllowingStateLoss();
            */
            ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.SHARE_MAP);
        });
    }
}