package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentDeviceBinding;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.list.DeviceAdapter;

public class DeviceFragment extends Fragment implements DeviceAdapter.Host
{
    private FragmentDeviceBinding binding;
    private DeviceAdapter adapter;
    private DeviceRegistrationActions actions;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ActivityMainBinding main = ((MainActivity) requireActivity()).mBinding;
        main.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        main.toolbarNavigationMessage.setVisibility(View.GONE);
        main.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        main.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        main.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        main.toolbar.setTitle(R.string.toolbar_title_management_device);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentDeviceBinding.inflate(inflater, container, false);
        actions = new DeviceRegistrationActions((MainActivity) requireActivity(), this::isAdded);
        if (adapter == null)
        {
            adapter = new DeviceAdapter(this);
            adapter.setItems(UtilDevice.instance.getDevices());
        }
        binding.deviceRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.deviceRecyclerview.setAdapter(adapter);
        onDeviceRowsChanged();
        return ManagementUi.navigation(binding.getRoot());
    }

    @Override
    public void onDestroyView()
    {
        if (adapter != null)
        {
            adapter.dismissEditor();
        }
        if (actions != null)
        {
            actions.close();
            actions = null;
        }
        if (binding != null)
        {
            binding.deviceRecyclerview.setAdapter(null);
        }
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDeviceRowsChanged()
    {
        if (binding != null)
        {
            binding.deviceTitle.setText("등록된 외부기 · " + adapter.getRegisteredCount());
        }
    }

    @Override
    public String saveDevice(EntityDevice original, EntityDevice updated)
    {
        return actions.saveDevice(original, updated);
    }

    @Override
    public void confirmDeleteDevice(EntityDevice device, Runnable deleted)
    {
        actions.confirmDeleteDevice(device, deleted);
    }
}
