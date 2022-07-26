package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.FragmentAddDeviceBinding;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.params.Status;

public class AddDeviceFragment extends Fragment
{
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

        todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding mainBinding = ((MainActivity) requireContext()).mBinding;
        mainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_add_device));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mAddDeviceBinding = FragmentAddDeviceBinding.inflate(inflater, container, false);
        return mAddDeviceBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        initAddButton();
    }

    private void initAddButton()
    {
        mAddDeviceBinding.addDeviceAddButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            boolean invalid = false;
            EntityDevice device = new EntityDevice();

            device.serialNumber = Objects.requireNonNull(mAddDeviceBinding.addDevieSerialEdittext.getText()).toString();
            device.pairingKey = Objects.requireNonNull(mAddDeviceBinding.addDeviePairingKeyEdittext.getText()).toString();
            device.additionalInformation = Objects.requireNonNull(mAddDeviceBinding.addDeviceOptionEdittext.getText()).toString();

            if (device.serialNumber.length() < 1)
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
                if (Status.instance().lastDialog != null)
                {
                    if (Status.instance().lastDialog.isShowing())
                    {
                        Status.instance().lastDialog.dismiss();
                    }
                }

                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("정보가 올바로 입력되지 않았습니다. 제조번호 및 보안코드 항목은 필수 입력 사항입니다.")
                                .setPositiveButton("확인", (dialogInterface, i) ->
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
                EntityDevice readDevice = UtilDevice.instance.getDeviceBySerialNumber(device.serialNumber);
                if (readDevice != null)
                {
                    mAddDeviceBinding.addDevieSerialEdittext.setError("이미 등록된 제조번호입니다.");

                    if (Status.instance().lastDialog != null)
                    {
                        if (Status.instance().lastDialog.isShowing())
                        {
                            Status.instance().lastDialog.dismiss();
                        }
                    }

                    Status.instance().lastDialog =
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("주의")
                                    .setMessage("이미 같은 제조번호가 등록되어있습니다. 수정을 원하는 경우, 사운드처리기 목록에서 수정을 원하는 사운드처리기 제조번호를 누르세요.")
                                    .setPositiveButton("확인", (dialogInterface, i) ->
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
                    UtilDevice.instance.insert(device);

                    UtilLog.instance.writeLog(
                            "사운드처리기 추가 : 제품번호=" + device.serialNumber + ", 보안코드=" + device.pairingKey + ", 추가정보=" + device.additionalInformation);

                    if (UtilDevice.instance.getDeviceBySerialNumber(device.serialNumber) != null)
                    {
                        requireActivity().onBackPressed();
                    }
                }
            }
        });
    }
}