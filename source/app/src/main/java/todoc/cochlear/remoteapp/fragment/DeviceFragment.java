package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;

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
import todoc.cochlear.remoteapp.list.DeviceAdapter;

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
        mMainBinding.toolbar.getMenu().findItem(R.id.settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Default_Headline6);
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_management_device));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
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
        EntityDevice item = mDeviceAdapter.getItem(position);

        Log.d(TAG, "Position = " + position + ", " + item.toString());

        Bundle bundle = new Bundle();
        bundle.putString(EditDeviceFragment.ARG_SERIAL, item.serialNumber);
        bundle.putString(EditDeviceFragment.ARG_PAIRING_KEY, item.pairingKey);
        bundle.putString(EditDeviceFragment.ARG_OPTION, item.additionalInformation);

        EditDeviceFragment editDeviceFragment = new EditDeviceFragment();
        editDeviceFragment.setArguments(bundle);

        ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), editDeviceFragment).commitAllowingStateLoss();
    }

    private void initAddButton()
    {
        mDeviceBinding.addBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                ((MainActivity) requireActivity()).getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddDeviceFragment()).commitAllowingStateLoss();
            }
        });
    }

    private void updateRecyclerView()
    {
        List<EntityDevice> entityDevices = ((MainActivity) requireActivity()).mDatabaseDevices.daoDevices().findAll();

        for (EntityDevice entityDevice : entityDevices)
        {
            mDeviceAdapter.addItem(entityDevice);
        }
    }
}