package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentAddDeviceBinding;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;

public class AddDeviceFragment extends Fragment
{
    private ActivityMainBinding mMainBinding;
    private FragmentAddDeviceBinding mAddDeviceBinding;

    public AddDeviceFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mAddDeviceBinding = null;
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
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_add_device));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mAddDeviceBinding = FragmentAddDeviceBinding.inflate(inflater, container, false);
        View view = mAddDeviceBinding.getRoot();

        initCancelButton();
        initAddButton();

        return view;
    }

    private void initCancelButton()
    {
        mAddDeviceBinding.addDeviceCancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                requireActivity().onBackPressed();
            }
        });
    }

    private void initAddButton()
    {
        mAddDeviceBinding.addDeviceAddButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean invalid = false;
                EntityDevice device = new EntityDevice();

                device.serialNumber = mAddDeviceBinding.addDevieSerialEdittext.getText().toString();
                device.pairingKey = mAddDeviceBinding.addDeviePairingKeyEdittext.getText().toString();
                device.additionalInformation = mAddDeviceBinding.addDeviceOptionEdittext.getText().toString();

                if (device.serialNumber == null || device.serialNumber.length() < 1)
                {
                    mAddDeviceBinding.addDevieSerialEdittext.setError("제조번호를 입력하세요.");
                    invalid = true;
                }

                if (device.pairingKey == null || device.pairingKey.length() != 6)
                {
                    mAddDeviceBinding.addDeviePairingKeyEdittext.setError("보안코드는 6자리 번호입니다.");
                    invalid = true;
                }

                if (invalid)
                {
                    new MaterialAlertDialogBuilder(requireContext(), R.style.add_user_screen_dialog)
                            .setTitle("주의")
                            .setMessage("정보가 올바로 입력되지 않았습니다. 제조번호 및 보안코드 항목은 필수 입력 사항입니다.")
                            .setPositiveButton("확인", null)
                            .show();
                }
                else
                {
                    EntityDevice readDevice = ((MainActivity) requireActivity()).mDatabaseDevices.daoDevices().getDbDeviceBySerialNumber(device.serialNumber);
                    if (readDevice != null)
                    {
                        mAddDeviceBinding.addDevieSerialEdittext.setError("이미 등록된 제조번호입니다.");

                        new MaterialAlertDialogBuilder(requireContext(), R.style.add_user_screen_dialog)
                                .setTitle("주의")
                                .setMessage("이미 같은 제조번호가 등록되어있습니다. 수정을 원하는 경우, 사운드처리기 목록에서 수정을 원하는 사운드처리기 제조번호를 누르세요.")
                                .setPositiveButton("확인", null)
                                .show();
                    }
                    else
                    {
                        ((MainActivity) requireActivity()).mDatabaseDevices.daoDevices().insert(device);

                        if (((MainActivity) requireActivity()).mDatabaseDevices.daoDevices().getDbDeviceBySerialNumber(device.serialNumber) != null)
                        {
                            requireActivity().onBackPressed();
                        }
                    }
                }
            }
        });
    }
}