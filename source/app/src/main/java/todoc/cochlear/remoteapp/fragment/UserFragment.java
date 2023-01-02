package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentUserBinding;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.list.UserAdapter;
import todoc.cochlear.remoteapp.params.Status;

public class UserFragment extends Fragment
{
    private static final String TAG = "TODOC_" + UserFragment.class.getSimpleName();

    private ActivityMainBinding mMainBinding;
    private FragmentUserBinding mUserBinding;

    UserAdapter mUserAdapter;
    UserAdapter mDefaultUserAdapter;

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
        mMainBinding.toolbarNavigationMessage.setText("메뉴");
        mMainBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_management_user));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mUserBinding = FragmentUserBinding.inflate(inflater, container, false);
        View view = mUserBinding.getRoot();

        mUserAdapter = new UserAdapter(this);
        mUserBinding.userRecyclerview.setAdapter(mUserAdapter);
        mUserBinding.userRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        mDefaultUserAdapter = new UserAdapter(this);
        mUserBinding.defaultUserRecyclerview.setAdapter(mDefaultUserAdapter);
        mUserBinding.defaultUserRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        updateRecyclerView();
        initAddButton();

        return view;
    }

    public void mDefaultItemClickListener(int position)
    {
        // 장시간 미사용 핸들러 업데이트
        ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

        EntityUser item = mDefaultUserAdapter.getItem(position);

        Log.d(TAG, "Default User Position = " + position + ", " + item.toString());

        Bundle bundle = new Bundle();
        bundle.putString(EditUserFragment.ARG_NAME, item.name);
        bundle.putString(EditUserFragment.ARG_PASSKEY, item.passKey);
        bundle.putString(EditUserFragment.ARG_NICKNAME, item.nickname);
        bundle.putString(EditUserFragment.ARG_EAR, item.ear);
        bundle.putString(EditUserFragment.ARG_DEFAULT, item.defaultUser);

        /*
        EditUserFragment editUserFragment = new EditUserFragment();
        editUserFragment.setArguments(bundle);
        requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), editUserFragment).commitAllowingStateLoss();
        */
        ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.USER_EDIT, bundle);
    }

    public void mItemClickListener(int position)
    {
        // 장시간 미사용 핸들러 업데이트
        ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

        EntityUser item = mUserAdapter.getItem(position);

        Log.d(TAG, "Position = " + position + ", " + item.toString());

        Bundle bundle = new Bundle();
        bundle.putString(EditUserFragment.ARG_NAME, item.name);
        bundle.putString(EditUserFragment.ARG_PASSKEY, item.passKey);
        bundle.putString(EditUserFragment.ARG_NICKNAME, item.nickname);
        bundle.putString(EditUserFragment.ARG_EAR, item.ear);
        bundle.putString(EditUserFragment.ARG_DEFAULT, item.defaultUser);

        /*
        EditUserFragment editUserFragment = new EditUserFragment();
        editUserFragment.setArguments(bundle);
        requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), editUserFragment).commitAllowingStateLoss();
        */
        ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.USER_EDIT, bundle);
    }

    private void initAddButton()
    {
        mUserBinding.addBtn.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            /*
            requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddUserFragment()).commitAllowingStateLoss();
            */
            ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.USER_ADD);
        });
    }

    private void updateRecyclerView()
    {


        // TD2-SW-RC-UNIT-Test-ID-72 [사용자 목록 표시 유닛] 순서[1] 시작.
        /*
        {
            List<EntityUser> testUsers = UtilUser.instance.getUsers();

            for (EntityUser user : testUsers)
            {
                Log.d(TAG, "name = " + user.name + ", ear = " + user.ear + ", nickname = " + user.nickname
                        + ", passKey = " + user.passKey + ", defaultUser = " + user.defaultUser);
            }
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-72 [사용자 목록 표시 유닛] 순서[1] 끝.


        mUserBinding.defaultUserTitle.setVisibility(View.GONE);
        mUserBinding.defaultUserRecyclerview.setVisibility(View.GONE);

        mUserBinding.userTitle.setVisibility(View.GONE);
        mUserBinding.userRecyclerview.setVisibility(View.GONE);

        EntityUser defaultUser = UtilUser.instance.getDefaultUser();

        if (defaultUser != null)
        {
            //mUserAdapter.addItem(defaultUser);
            mDefaultUserAdapter.addItem(defaultUser);
            mUserBinding.defaultUserTitle.setVisibility(View.VISIBLE);
            mUserBinding.defaultUserRecyclerview.setVisibility(View.VISIBLE);
        }

        List<EntityUser> entityUsers = UtilUser.instance.getUsers();


        for (EntityUser entityUser : entityUsers)
        {
            if (entityUser.defaultUser.equals(EntityUser.USER_NOT_DEFAULT))
            {
                mUserAdapter.addItem(entityUser);
                mUserBinding.userTitle.setVisibility(View.VISIBLE);
                mUserBinding.userRecyclerview.setVisibility(View.VISIBLE);
            }
        }
    }
}