package todoc.cochlear.remoteapp.fragment;

import android.content.DialogInterface;
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
import todoc.cochlear.remoteapp.activity.databinding.FragmentEditDeviceBinding;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;

public class EditDeviceFragment extends Fragment
{
    static public final String ARG_SERIAL = "serial";
    static public final String ARG_PAIRING_KEY = "pairing_key";
    static public final String ARG_OPTION = "option";

    private ActivityMainBinding mMainBinding;
    private FragmentEditDeviceBinding mEditDeviceBinding;
    private EntityDevice mItem;

    public EditDeviceFragment()
    {
        // Required empty public constructor
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
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_edit_device));

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        mEditDeviceBinding = FragmentEditDeviceBinding.inflate(inflater, container, false);
        View view = mEditDeviceBinding.getRoot();

        fillInformation();
        initCancelButton();
        initDeleteButton();
        initEditButton();

        return view;
    }

    private void fillInformation()
    {
        mEditDeviceBinding.editDeviceSerialEdittext.setText(mItem.serialNumber);
        mEditDeviceBinding.editDeviePairingKeyEdittext.setText(mItem.pairingKey);
        mEditDeviceBinding.editDeviceOptionEdittext.setText(mItem.additionalInformation);
    }

    private void initCancelButton()
    {
        mEditDeviceBinding.editDeviceCancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                requireActivity().onBackPressed();
            }
        });
    }

    private void initDeleteButton()
    {
        mEditDeviceBinding.editDeviceDelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new MaterialAlertDialogBuilder(requireContext(), R.style.add_user_screen_dialog)
                        .setTitle("주의")
                        .setMessage("사운드처리기 정보를 정말 삭제하시겠습니까?")
                        .setPositiveButton("삭제", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                ((MainActivity) requireActivity()).mDatabaseDevices.daoDevices().delete(mItem);
                                requireActivity().onBackPressed();
                            }
                        })
                        .setNegativeButton("취소", null)
                        .show();

            }
        });
    }

    private void initEditButton()
    {
        mEditDeviceBinding.editDeviceEditButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean invalid = false;

                mItem.pairingKey = mEditDeviceBinding.editDeviePairingKeyEdittext.getText().toString();
                mItem.additionalInformation = mEditDeviceBinding.editDeviceOptionEdittext.getText().toString();

                if (mItem.pairingKey == null || mItem.pairingKey.length() != 6)
                {
                    mEditDeviceBinding.editDeviePairingKeyEdittext.setError("보안코드는 6자리 번호입니다.");
                    invalid = true;
                }

                if (invalid)
                {
                    new MaterialAlertDialogBuilder(requireContext(), R.style.add_user_screen_dialog)
                            .setTitle("주의")
                            .setMessage("정보가 올바로 입력되지 않았습니다. 보안코드 항목은 필수 입력 사항입니다.")
                            .setPositiveButton("확인", null)
                            .show();
                }
                else
                {
                    ((MainActivity) requireActivity()).mDatabaseDevices.daoDevices().update(mItem);
                    requireActivity().onBackPressed();
                }
            }
        });
    }
}