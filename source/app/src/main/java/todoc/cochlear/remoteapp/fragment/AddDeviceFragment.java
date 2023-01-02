package todoc.cochlear.remoteapp.fragment;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
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
    static private final String TAG = "TODOC_" + AddDeviceFragment.class.getSimpleName();

    public FragmentAddDeviceBinding mAddDeviceBinding;

    public AddDeviceFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mAddDeviceBinding.addDevieSerialEdittext.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mAddDeviceBinding.addDeviePairingKeyEdittext.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mAddDeviceBinding.addDeviceOptionEdittext.getWindowToken(), 0);

        mAddDeviceBinding = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding mainBinding = ((MainActivity) requireContext()).mBinding;
        mainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mainBinding.toolbarNavigationMessage.setText("외부기\n목록");
        mainBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
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

            EntityDevice device = new EntityDevice();

            device.serialNumber = Objects.requireNonNull(mAddDeviceBinding.addDevieSerialEdittext.getText()).toString();
            device.pairingKey = Objects.requireNonNull(mAddDeviceBinding.addDeviePairingKeyEdittext.getText()).toString();
            device.additionalInformation = Objects.requireNonNull(mAddDeviceBinding.addDeviceOptionEdittext.getText()).toString();

            if (device.serialNumber.length() < 1)
            {
                //mAddDeviceBinding.addDevieSerialEdittext.setError("제조번호를 입력하세요.");

                if (Status.instance().lastDialog != null)
                {
                    if (Status.instance().lastDialog.isShowing())
                    {
                        Status.instance().lastDialog.dismiss();
                    }
                }


                // TD2-SW-RC-UNIT-Test-ID-80 [기기 등록 유닛] 순서[1] 시작.
                /*
                {
                    Log.d(TAG, "serialNumber length = " + device.serialNumber.length());
                    Log.d(TAG, "serialnumber must be greater than 1");
                }
                */
                // TD2-SW-RC-UNIT-Test-ID-80 [기기 등록 유닛] 순서[1] 끝.


                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("제조번호를 입력해주세요.")
                                .setPositiveButton("확인", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                })
                                .setCancelable(false)
                                .create();
                Status.instance().lastDialog.show();
            }
            else if (device.pairingKey == null || device.pairingKey.length() != 6)
            {
                //mAddDeviceBinding.addDeviePairingKeyEdittext.setError("페어링 키는 6자리입니다.");

                if (Status.instance().lastDialog != null)
                {
                    if (Status.instance().lastDialog.isShowing())
                    {
                        Status.instance().lastDialog.dismiss();
                    }
                }


                // TD2-SW-RC-UNIT-Test-ID-80 [기기 등록 유닛] 순서[2] 시작.
                /*
                {
                    Log.d(TAG, "pairingKey length = " + device.pairingKey.length());
                    Log.d(TAG, "pairingKey must be equals to 6");
                }
                */
                // TD2-SW-RC-UNIT-Test-ID-80 [기기 등록 유닛] 순서[2] 끝.


                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("페어링 키는 6자리 번호입니다.")
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
                    //mAddDeviceBinding.addDevieSerialEdittext.setError("등록된 제조번호입니다.");

                    if (Status.instance().lastDialog != null)
                    {
                        if (Status.instance().lastDialog.isShowing())
                        {
                            Status.instance().lastDialog.dismiss();
                        }
                    }


                    // TD2-SW-RC-UNIT-Test-ID-80 [기기 등록 유닛] 순서[3] 시작.
                    /*
                    {
                        Log.d(TAG, "serialNumber = " + device.serialNumber + ", pairingKey = " + device.pairingKey + ", additionalInformation = " + device.additionalInformation);
                        Log.d(TAG, "already registered device information.");
                    }
                    */
                    // TD2-SW-RC-UNIT-Test-ID-80 [기기 등록 유닛] 순서[3] 끝.


                    Status.instance().lastDialog =
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("주의")
                                    .setMessage("이미 등록된 제조번호입니다.")
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


                    // TD2-SW-RC-UNIT-Test-ID-80 [기기 등록 유닛] 순서[4] 시작.
                    /*
                    {
                        Log.d(TAG, "serialNumber = " + device.serialNumber + ", pairingKey = " + device.pairingKey + ", additionalInformation = " + device.additionalInformation);
                        Log.d(TAG, "register device information.");
                    }
                    */
                    // TD2-SW-RC-UNIT-Test-ID-80 [기기 등록 유닛] 순서[4] 끝.


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