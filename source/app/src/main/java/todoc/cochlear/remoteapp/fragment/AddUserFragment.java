package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentAddUserBinding;
import todoc.cochlear.remoteapp.database.users.EntityUser;

public class AddUserFragment extends Fragment
{
    static private final String TAG = "TODOC_" + AddUserFragment.class.getSimpleName();

    private ActivityMainBinding mMainBinding;
    private FragmentAddUserBinding mAddUserBinding;

    public AddUserFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mAddUserBinding = null;
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
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_add_user));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mAddUserBinding = FragmentAddUserBinding.inflate(inflater, container, false);
        View view = mAddUserBinding.getRoot();

        initCancelButton();
        initAddButton();

        return view;
    }

    private void initCancelButton()
    {
        mAddUserBinding.cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                requireActivity().onBackPressed();
            }
        });
    }

    private void initAddButton()
    {
        mAddUserBinding.addButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean invalid = false;
                EntityUser user = new EntityUser();

                user.name = mAddUserBinding.nameEdittext.getText().toString();

                user.passKey = mAddUserBinding.passkeyEdittext.getText().toString();

                if (mAddUserBinding.leftRadiobutton.isChecked())
                {
                    user.ear = EntityUser.EAR_LEFT;
                }
                else if (mAddUserBinding.rightRadiobutton.isChecked())
                {
                    user.ear = EntityUser.EAR_RIGHT;
                }
                else
                {
                    user.ear = null;
                }

                if (mAddUserBinding.defaultCheckbox.isChecked())
                {
                    user.defaultUser = EntityUser.USER_DEFAULT;
                }
                else
                {
                    user.defaultUser = EntityUser.USER_NOT_DEFAULT;
                }

                if (user.name == null || user.name.length() < 1)
                {
                    mAddUserBinding.nameEdittext.setError("이름을 입력하세요.");
                    invalid = true;
                }

                if (user.passKey == null || user.passKey.length() != 4)
                {
                    mAddUserBinding.passkeyEdittext.setError("내부기 키는 4자리 번호입니다.");
                    invalid = true;
                }

                if (user.ear == null)
                {
                    invalid = true;
                }

                if (invalid)
                {
                    new MaterialAlertDialogBuilder(requireContext(), R.style.add_user_screen_dialog)
                            .setTitle("주의")
                            .setMessage("정보가 올바로 입력되지 않았습니다. 이름, 내부기 키 그리고 수술 위치 항목은 필수 입력 사항입니다.")
                            .setPositiveButton("확인", null)
                            .show();
                }
                else
                {
                    EntityUser readUser = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByName(user.name);
                    if (readUser != null)
                    {
                        mAddUserBinding.nameEdittext.setError("이미 등록된 이름입니다.");

                        new MaterialAlertDialogBuilder(requireContext(), R.style.add_user_screen_dialog)
                                .setTitle("주의")
                                .setMessage("이미 같은 이름이 등록되어있습니다. 수정을 원하는 경우, 사용자 목록에서 수정을 원하는 사용자 이름을 누르세요.")
                                .setPositiveButton("확인", null)
                                .show();
                    }
                    else
                    {
                        if (user.defaultUser.equals(EntityUser.USER_DEFAULT))
                        {
                            EntityUser defaultUser = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);
                            if (defaultUser != null)
                            {
                                defaultUser.defaultUser = EntityUser.USER_NOT_DEFAULT;
                                ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().update(defaultUser);
                            }
                        }

                        ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().insert(user);

                        if (((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByName(user.name) != null)
                        {
                            requireActivity().onBackPressed();
                        }
                    }
                }
            }
        });
    }
}