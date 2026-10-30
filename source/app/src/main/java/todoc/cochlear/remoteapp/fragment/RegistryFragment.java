package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.users.UserGroup;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.list.DeviceAdapter;
import todoc.cochlear.remoteapp.list.UserAdapter;

/** A single scrolling registration screen with shared dialog and storage action paths. */
public final class RegistryFragment extends Fragment implements UserAdapter.Host, DeviceAdapter.Host
{
    private UserAdapter users;
    private DeviceAdapter devices;
    private UserRegistrationActions userActions;
    private DeviceRegistrationActions deviceActions;
    private RecyclerView userList;
    private RecyclerView deviceList;
    private TextView userCount;
    private TextView deviceCount;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state)
    {
        MainActivity activity = (MainActivity) requireActivity();
        ManagementUi.toolbar(activity, "등록 관리");
        userActions = new UserRegistrationActions(activity, this::isAdded);
        deviceActions = new DeviceRegistrationActions(activity, this::isAdded);
        users = new UserAdapter(this);
        for (UserGroup group : UserGroup.fromUsers(UtilUser.instance.getUsers()))
        {
            users.addItem(group);
        }
        devices = new DeviceAdapter(this);
        devices.setItems(UtilDevice.instance.getDevices());

        LinearLayout body = ManagementUi.body(requireContext());
        LinearLayout userSection = section(body);
        userCount = heading(userSection, R.id.user_title);
        tableHeader(userSection, true);
        userList = list(userSection, R.id.user_recyclerview, users);
        LinearLayout deviceSection = section(body);
        deviceCount = heading(deviceSection, R.id.device_title);
        tableHeader(deviceSection, false);
        deviceList = list(deviceSection, R.id.device_recyclerview, devices);
        listChanged();
        onDeviceRowsChanged();
        return ManagementUi.scroll(body);
    }

    private LinearLayout section(LinearLayout body)
    {
        LinearLayout section = ManagementUi.panel(body);
        section.setPadding(0, 0, 0, 0);
        section.setClipToOutline(true);
        return section;
    }

    private TextView heading(LinearLayout section, int id)
    {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(8));
        TextView label = ManagementUi.text(requireContext(), id == R.id.user_title ? "사용자" : "외부기", 14);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        TextView title = ManagementUi.text(requireContext(), "", 12);
        title.setId(id);
        title.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        title.setTextColor(getResources().getColor(R.color.glass_muted_text, requireContext().getTheme()));
        row.addView(title, new LinearLayout.LayoutParams(-2, -2));
        section.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return title;
    }

    private void tableHeader(LinearLayout section, boolean user)
    {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), 0, dp(10), 0);
        addLabel(row, user ? "이니셜" : "제조번호", user ? 112 : 104, false);
        TextView alias = ManagementUi.text(requireContext(), "별칭", 11);
        alias.setTextColor(getResources().getColor(R.color.white_70, requireContext().getTheme()));
        LinearLayout.LayoutParams aliasParams = new LinearLayout.LayoutParams(0, -2, 1);
        aliasParams.rightMargin = dp(user ? 6 : 8);
        row.addView(alias, aliasParams);
        if (user)
        {
            addLabel(row, "L", 32, true);
            addLabel(row, "R", 32, true);
            addLabel(row, "F", 32, true);
        }
        else
        {
            addLabel(row, "사용 상태", 56, true);
        }
        section.addView(row, new LinearLayout.LayoutParams(-1, dp(26)));
    }

    private void addLabel(LinearLayout row, String value, int width, boolean centered)
    {
        TextView label = ManagementUi.text(requireContext(), value, 11);
        label.setSingleLine(true);
        label.setTextColor(getResources().getColor(R.color.white_70, requireContext().getTheme()));
        label.setGravity(centered ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
        row.addView(label, new LinearLayout.LayoutParams(dp(width), -1));
    }

    private RecyclerView list(LinearLayout section, int id, RecyclerView.Adapter<?> adapter)
    {
        RecyclerView list = new RecyclerView(requireContext());
        list.setId(id);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setNestedScrollingEnabled(false);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setAdapter(adapter);
        section.addView(list, new LinearLayout.LayoutParams(-1, -2));
        return list;
    }

    private int dp(int value)
    {
        return ManagementUi.dp(requireContext(), value);
    }

    @Override
    public void listChanged()
    {
        if (userCount != null)
        {
            userCount.setText("총 " + users.getRegisteredCount() + "명");
        }
    }

    @Override
    public void onDeviceRowsChanged()
    {
        if (deviceCount != null)
        {
            deviceCount.setText("총 " + devices.getRegisteredCount() + "대");
        }
    }

    @Override
    public String saveGroup(UserGroup original, UserGroup updated)
    {
        return userActions.saveGroup(original, updated);
    }

    @Override
    public void deleteGroup(UserGroup group, Runnable deleted)
    {
        userActions.deleteGroup(group, deleted);
    }

    @Override
    public String saveDevice(EntityDevice original, EntityDevice updated)
    {
        return deviceActions.saveDevice(original, updated);
    }

    @Override
    public void confirmDeleteDevice(EntityDevice device, Runnable deleted)
    {
        deviceActions.confirmDeleteDevice(device, deleted);
    }

    @Override
    public void onDestroyView()
    {
        if (users != null)
        {
            users.dismissEditor();
        }
        if (devices != null)
        {
            devices.dismissEditor();
        }
        if (userActions != null)
        {
            userActions.close();
        }
        if (deviceActions != null)
        {
            deviceActions.close();
        }
        if (userList != null)
        {
            userList.setAdapter(null);
        }
        if (deviceList != null)
        {
            deviceList.setAdapter(null);
        }
        userList = null;
        deviceList = null;
        userCount = null;
        deviceCount = null;
        super.onDestroyView();
    }
}