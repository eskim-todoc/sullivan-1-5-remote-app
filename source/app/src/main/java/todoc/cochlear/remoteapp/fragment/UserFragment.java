package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.FragmentUserBinding;
import todoc.cochlear.remoteapp.database.users.UserGroup;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.list.UserAdapter;

/** Compatibility host; the primary registration route uses RegistryFragment. */
public class UserFragment extends Fragment implements UserAdapter.Host
{
    private FragmentUserBinding binding;
    private UserAdapter adapter;
    private UserRegistrationActions actions;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ManagementUi.toolbar((MainActivity) requireActivity(), getString(R.string.toolbar_title_management_user));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentUserBinding.inflate(inflater, container, false);
        actions = new UserRegistrationActions((MainActivity) requireActivity(), this::isAdded);
        binding.userRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        if (adapter == null)
        {
            adapter = new UserAdapter(this);
            for (UserGroup group : UserGroup.fromUsers(UtilUser.instance.getUsers()))
            {
                adapter.addItem(group);
            }
        }
        binding.userRecyclerview.setAdapter(adapter);
        listChanged();
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
            binding.userRecyclerview.setAdapter(null);
        }
        binding = null;
        super.onDestroyView();
    }

    public void editGroup(UserGroup group, String focusEar)
    {
        adapter.openEditor(group, focusEar);
    }

    @Override
    public void listChanged()
    {
        if (binding != null)
        {
            binding.userTitle.setText("등록된 사용자 · " + adapter.getRegisteredCount());
        }
    }

    @Override
    public String saveGroup(UserGroup original, UserGroup updated)
    {
        return actions.saveGroup(original, updated);
    }

    @Override
    public void deleteGroup(UserGroup group, Runnable onDeleted)
    {
        actions.deleteGroup(group, onDeleted);
    }
}