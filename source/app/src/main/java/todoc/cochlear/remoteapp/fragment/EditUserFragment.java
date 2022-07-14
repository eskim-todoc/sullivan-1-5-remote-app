package todoc.cochlear.remoteapp.fragment;

import android.content.DialogInterface;
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
import todoc.cochlear.remoteapp.activity.databinding.FragmentEditUserBinding;
import todoc.cochlear.remoteapp.database.users.EntityUser;

public class EditUserFragment extends Fragment
{
    static public final String ARG_NAME = "name";
    static public final String ARG_EAR = "ear";
    static public final String ARG_PASSKEY = "passkey";
    static public final String ARG_DEFAULT = "default";

    private ActivityMainBinding mMainBinding;
    private FragmentEditUserBinding mEditUserBinding;
    private EntityUser mItem;

    public EditUserFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mEditUserBinding = null;
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
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_edit_user));

        Bundle bundle = getArguments();

        if (bundle != null)
        {
            mItem = new EntityUser();
            mItem.name = bundle.getString(ARG_NAME);
            mItem.ear = bundle.getString(ARG_EAR);
            mItem.passKey = bundle.getString(ARG_PASSKEY);
            mItem.defaultUser = bundle.getString(ARG_DEFAULT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mEditUserBinding = FragmentEditUserBinding.inflate(inflater, container, false);
        View view = mEditUserBinding.getRoot();

        fillInformation();
        initCancelButton();
        initDeleteButton();
        initEditButton();

        return view;
    }

    private void fillInformation()
    {
        mEditUserBinding.edittextName.setText(mItem.name);
        mEditUserBinding.edittextPasskey.setText(mItem.passKey);

        if (mItem.ear.equals(EntityUser.EAR_LEFT))
        {
            mEditUserBinding.editUserLeftRadiobutton.setChecked(true);
        }
        else if (mItem.ear.equals(EntityUser.EAR_RIGHT))
        {
            mEditUserBinding.editUserRightRadiobutton.setChecked(true);
        }

        boolean isDefault = mItem.defaultUser.equals(EntityUser.USER_DEFAULT);
        mEditUserBinding.checkboxDefaultUser.setChecked(isDefault);
    }

    private void initCancelButton()
    {
        mEditUserBinding.editUserCancelButton.setOnClickListener(new View.OnClickListener()
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
        mEditUserBinding.editUserDelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new MaterialAlertDialogBuilder(requireContext(), R.style.edit_user_screen_dialog)
                        .setTitle("주의")
                        .setMessage("사용자 정보를 정말 삭제하시겠습니까?")
                        .setPositiveButton("삭제", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().delete(mItem);
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
        mEditUserBinding.editUserEditButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean invalid = false;

                String passKey = mEditUserBinding.edittextPasskey.getText().toString();
                if (passKey == null || passKey.length() != 4)
                {
                    mEditUserBinding.edittextPasskey.setError("내부기 키는 4자리 번호입니다.");
                    invalid = true;
                }

                if (invalid)
                {
                    new MaterialAlertDialogBuilder(requireContext(), R.style.edit_user_screen_dialog)
                            .setTitle("주의")
                            .setMessage("정보가 올바로 입력되지 않았습니다. 내부기 키 항목은 필수 입력 사항입니다.")
                            .setPositiveButton("확인", null)
                            .show();
                }
                else
                {
                    mItem.passKey = passKey;

                    if (mEditUserBinding.editUserLeftRadiobutton.isChecked())
                    {
                        mItem.ear = EntityUser.EAR_LEFT;
                    }
                    else
                    {
                        mItem.ear = EntityUser.EAR_RIGHT;
                    }

                    if (mEditUserBinding.checkboxDefaultUser.isChecked())
                    {
                        mItem.defaultUser = EntityUser.USER_DEFAULT;
                    }
                    else
                    {
                        mItem.defaultUser = EntityUser.USER_NOT_DEFAULT;
                    }

                    if (mItem.defaultUser.equals(EntityUser.USER_DEFAULT))
                    {
                        EntityUser defaultUSer = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);

                        if (defaultUSer != null)
                        {
                            if (!defaultUSer.name.equals(mItem.name))
                            {
                                defaultUSer.defaultUser = EntityUser.USER_NOT_DEFAULT;
                                ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().update(defaultUSer);
                            }
                        }
                    }

                    ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().update(mItem);
                    requireActivity().onBackPressed();
                }
            }
        });
    }
}