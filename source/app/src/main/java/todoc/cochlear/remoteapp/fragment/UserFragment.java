package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentUserBinding;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.list.UserAdapter;

public class UserFragment extends Fragment
{
    private static final String TAG = "TODOC_" + UserFragment.class.getSimpleName();

    private ActivityMainBinding mMainBinding;
    private FragmentUserBinding mUserBinding;

    UserAdapter mUserAdapter;

    public UserFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mUserBinding = null;
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
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_management_user));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mUserBinding = FragmentUserBinding.inflate(inflater, container, false);
        View view = mUserBinding.getRoot();

        mUserAdapter = new UserAdapter(this);
        mUserBinding.userRecyclerview.setAdapter(mUserAdapter);
        mUserBinding.userRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        updateRecyclerView();
        initAddButton();

        return view;
    }

    public void mItemClickListener(int position)
    {
        EntityUser item = mUserAdapter.getItem(position);

        Log.d(TAG, "Position = " + position + ", " + item.toString());

        Bundle bundle = new Bundle();
        bundle.putString(EditUserFragment.ARG_NAME, item.name);
        bundle.putString(EditUserFragment.ARG_EAR, item.ear);
        bundle.putString(EditUserFragment.ARG_PASSKEY, item.passKey);
        bundle.putString(EditUserFragment.ARG_DEFAULT, item.defaultUser);

        EditUserFragment editUserFragment = new EditUserFragment();
        editUserFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), editUserFragment).commitAllowingStateLoss();
    }

    private void initAddButton()
    {
        mUserBinding.addBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddUserFragment()).commitAllowingStateLoss();
            }
        });
    }

    private void updateRecyclerView()
    {
        EntityUser defaultUser = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);

        if (defaultUser != null)
        {
            mUserAdapter.addItem(defaultUser);
        }

        List<EntityUser> entityUsers = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().findAll();

        for (EntityUser entityUser : entityUsers)
        {
            if (entityUser.defaultUser.equals(EntityUser.USER_NOT_DEFAULT))
            {
                mUserAdapter.addItem(entityUser);
            }
        }
    }
}