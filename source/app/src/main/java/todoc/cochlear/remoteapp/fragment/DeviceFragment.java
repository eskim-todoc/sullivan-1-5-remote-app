package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentDeviceBinding;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.list.DeviceAdapter;
import todoc.cochlear.remoteapp.params.Status;

public class DeviceFragment extends Fragment
{
    private static final String TAG = "TODOC_" + DeviceFragment.class.getSimpleName();

    private ActivityMainBinding mMainBinding;
    private FragmentDeviceBinding mDeviceBinding;

    DeviceAdapter mDeviceAdapter;

    public DeviceFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mDeviceBinding = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainBinding = ((MainActivity) requireContext()).mBinding;
        mMainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mMainBinding.toolbarNavigationMessage.setText("메뉴");
        mMainBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_management_device));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mDeviceBinding = FragmentDeviceBinding.inflate(inflater, container, false);
        View view = mDeviceBinding.getRoot();

        mDeviceAdapter = new DeviceAdapter(this);
        mDeviceBinding.deviceRecyclerview.setAdapter(mDeviceAdapter);
        mDeviceBinding.deviceRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        updateRecyclerView();
        initAddButton();

        return view;
    }

    public void mItemClickListener(int position)
    {
        // 장시간 미사용 핸들러 업데이트
        ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

        EntityDevice item = mDeviceAdapter.getItem(position);

        Log.d(TAG, "Position = " + position + ", " + item.toString());

        Bundle bundle = new Bundle();
        bundle.putString(EditDeviceFragment.ARG_SERIAL, item.serialNumber);
        bundle.putString(EditDeviceFragment.ARG_PAIRING_KEY, item.pairingKey);
        bundle.putString(EditDeviceFragment.ARG_OPTION, item.additionalInformation);

        /*
        EditDeviceFragment editDeviceFragment = new EditDeviceFragment();
        editDeviceFragment.setArguments(bundle);
        ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), editDeviceFragment).commitAllowingStateLoss();
        */
        ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.DEVICE_EDIT, bundle);
    }

    private void initAddButton()
    {
        mDeviceBinding.addBtn.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            /*
            ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddDeviceFragment()).commitAllowingStateLoss();
            */
            ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.DEVICE_ADD);
        });
    }

    private void updateRecyclerView()
    {


        // TD2-SW-RC-UNIT-Test-ID-67 [기기 목록 표시 유닛] 순서[1] 시작.
        /*
        {
            List<EntityDevice> testDevices = UtilDevice.instance.getDevices();

            for (EntityDevice device : testDevices)
            {
                Log.d(TAG, "serialNumber = " + device.serialNumber + ", additionalInformation = " + device.additionalInformation + ", pairingKey = " + device.pairingKey);
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-67 [기기 목록 표시 유닛] 순서[1] 끝.


        mDeviceBinding.deviceTitle.setVisibility(View.GONE);

        List<EntityDevice> entityDevices = UtilDevice.instance.getDevices();

        for (EntityDevice entityDevice : entityDevices)
        {
            mDeviceAdapter.addItem(entityDevice);
            mDeviceBinding.deviceTitle.setVisibility(View.VISIBLE);
        }
    }
}