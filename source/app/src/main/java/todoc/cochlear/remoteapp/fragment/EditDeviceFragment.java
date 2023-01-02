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

import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.FragmentEditDeviceBinding;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.params.Status;

public class EditDeviceFragment extends Fragment
{
    static private final String TAG = "TODOC_" + EditDeviceFragment.class.getSimpleName();

    static public final String ARG_SERIAL = "serial";
    static public final String ARG_PAIRING_KEY = "pairing_key";
    static public final String ARG_OPTION = "option";

    public FragmentEditDeviceBinding mEditDeviceBinding;
    private EntityDevice mItem;

    public EditDeviceFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditDeviceBinding.editDeviceSerialEdittext.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mEditDeviceBinding.editDeviePairingKeyEdittext.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mEditDeviceBinding.editDeviceOptionEdittext.getWindowToken(), 0);

        mEditDeviceBinding = null;
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
        mainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_edit_device));

        Bundle bundle = getArguments();

        if (bundle != null)
        {
            mItem = new EntityDevice();
            mItem.serialNumber = bundle.getString(EditDeviceFragment.ARG_SERIAL);
            mItem.additionalInformation = bundle.getString(EditDeviceFragment.ARG_OPTION);
            mItem.pairingKey = bundle.getString(EditDeviceFragment.ARG_PAIRING_KEY);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mEditDeviceBinding = FragmentEditDeviceBinding.inflate(inflater, container, false);
        return mEditDeviceBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        fillInformation();
        initDeleteButton();
        initEditButton();
    }

    private void fillInformation()
    {
        mEditDeviceBinding.editDeviceSerialEdittext.setText(mItem.serialNumber);
        mEditDeviceBinding.editDeviePairingKeyEdittext.setText(mItem.pairingKey);
        mEditDeviceBinding.editDeviceOptionEdittext.setText(mItem.additionalInformation);
    }

    private void initDeleteButton()
    {
        mEditDeviceBinding.editDeviceDelButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (Status.instance().lastDialog != null)
            {
                if (Status.instance().lastDialog.isShowing())
                {
                    Status.instance().lastDialog.dismiss();
                }
            }

            // 현재 어떠한 사운드처리기와 연결되어 있는 상태라면, 지금 제거하려는 사운드처리기인지 체크한다.
            if ((Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED
                    || Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTING)
                    && Status.instance().connectedDevice.serialNumber.equals(mItem.serialNumber))
            {
                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("현재 연결을 해제하고 외부기 정보를 삭제하시겠습니까?")
                                .setPositiveButton("삭제", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                                    UtilDevice.instance.delete(mItem);

                                    UtilLog.instance.writeLog(
                                            "사운드처리기 제거 : 제품번호=" + mItem.serialNumber + ", 보안코드=" + mItem.pairingKey + ", 추가정보=" + mItem.additionalInformation);

                                    if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED
                                            || Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTING)
                                    {
                                        Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                                        ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
                                    }

                                    requireActivity().onBackPressed();
                                })
                                .setNegativeButton("취소", null)
                                .setCancelable(false)
                                .create();
            }
            else
            {
                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("외부기 정보를 삭제하시겠습니까?")
                                .setPositiveButton("삭제", (dialogInterface, i) ->
                                {


                                    // TD2-SW-RC-UNIT-Test-ID-82 [기기 삭제 유닛] 순서[2] 시작.
                                    /*
                                    {
                                        Log.d(TAG, "serialNumber = " + mItem.serialNumber + ", pairingKey = " + mItem.pairingKey + ", additionalInformation = " + mItem.additionalInformation);
                                        Log.d(TAG, "delete device information.");
                                    }
                                    */
                                    // TD2-SW-RC-UNIT-Test-ID-82 [기기 삭제 유닛] 순서[2] 끝.


                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                                    UtilDevice.instance.delete(mItem);

                                    UtilLog.instance.writeLog(
                                            "사운드처리기 제거 : 제품번호=" + mItem.serialNumber + ", 보안코드=" + mItem.pairingKey + ", 추가정보=" + mItem.additionalInformation);

                                    requireActivity().onBackPressed();
                                })
                                .setNegativeButton("취소", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                })
                                .setCancelable(false)
                                .create();
            }
            Status.instance().lastDialog.show();
        });
    }

    private void initEditButton()
    {
        mEditDeviceBinding.editDeviceEditButton.setOnClickListener(view ->
        {
            mItem.pairingKey = Objects.requireNonNull(mEditDeviceBinding.editDeviePairingKeyEdittext.getText()).toString();
            mItem.additionalInformation = Objects.requireNonNull(mEditDeviceBinding.editDeviceOptionEdittext.getText()).toString();

            if (mItem.pairingKey == null || mItem.pairingKey.length() != 6)
            {
                //mEditDeviceBinding.editDeviePairingKeyEdittext.setError("보안코드는 6자리 번호입니다.");
                if (Status.instance().lastDialog != null)
                {
                    if (Status.instance().lastDialog.isShowing())
                    {
                        Status.instance().lastDialog.dismiss();
                    }
                }


                // TD2-SW-RC-UNIT-Test-ID-81 [기기 수정 유닛] 순서[2] 시작.
                /*
                {
                    Log.d(TAG, "pairingKey length = " + mItem.pairingKey.length());
                    Log.d(TAG, "pairingKey length must be equals to 6");
                }
                */
                // TD2-SW-RC-UNIT-Test-ID-81 [기기 수정 유닛] 순서[2] 끝.


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


                // TD2-SW-RC-UNIT-Test-ID-81 [기기 수정 유닛] 순서[3] 시작.
                /*
                {
                    Log.d(TAG, "serialNumber = " + mItem.serialNumber + ", pairingKey = " + mItem.pairingKey + ", additionalInformation = " + mItem.additionalInformation);
                    Log.d(TAG, "edit device information.");
                }
                */
                // TD2-SW-RC-UNIT-Test-ID-81 [기기 수정 유닛] 순서[3] 끝.


                UtilDevice.instance.update(mItem);

                UtilLog.instance.writeLog(
                        "사운드처리기 수정 : 제품번호=" + mItem.serialNumber + ", 보안코드=" + mItem.pairingKey + ", 추가정보=" + mItem.additionalInformation);

                requireActivity().onBackPressed();
            }
        });
    }
}