package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentAddUserBinding;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.params.Status;

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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mAddUserBinding = FragmentAddUserBinding.inflate(inflater, container, false);
        return mAddUserBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mMainBinding = ((MainActivity) requireContext()).mBinding;
        mMainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_add_user));

        initAddButton();
    }

    private boolean checkHiddenLogEasterEgg()
    {
        String easterEggName = Objects.requireNonNull(mAddUserBinding.nameEdittext.getText()).toString();
        String easterEggPassKey = Objects.requireNonNull(mAddUserBinding.passkeyEdittext.getText()).toString();
        String easterEggNickname = Objects.requireNonNull(mAddUserBinding.nicknameEdittext.getText()).toString();

        boolean nameMatched = easterEggName.equals("todoc.co.kr");
        boolean passKeyMatched = easterEggPassKey.equals("1003");
        boolean nicknameMatched = easterEggNickname.equals("1407");

        if (nameMatched && passKeyMatched && nicknameMatched)
        {
            requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new LogFragment()).commitAllowingStateLoss();
            return true;
        }

        return false;
    }

    private void initAddButton()
    {
        mAddUserBinding.addButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (checkHiddenLogEasterEgg()) // 히든로그 메시지 확인창으로 들어가는 이스터에그
            {
                return;
            }

            boolean invalid = false;
            EntityUser user = new EntityUser();

            user.name = Objects.requireNonNull(mAddUserBinding.nameEdittext.getText()).toString();

            user.passKey = Objects.requireNonNull(mAddUserBinding.passkeyEdittext.getText()).toString();

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

            if (user.name.length() < 1)
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
                if (Status.instance().lastDialog != null)
                {
                    if (Status.instance().lastDialog.isShowing())
                    {
                        Status.instance().lastDialog.dismiss();
                    }
                }

                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("정보를 입력해주세요. 사용자 이름, 내부기 키 그리고 착용 위치 항목은 필수 입력 사항입니다.")
                                .setPositiveButton("확인", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                })
                                .setCancelable(false)
                                .create();
                Status.instance().lastDialog.show();
            }
            else
            {
                EntityUser readUser = UtilUser.instance.getUserByName(user.name);
                if (readUser != null)
                {
                    mAddUserBinding.nameEdittext.setError("이미 등록된 이름입니다.");

                    if (Status.instance().lastDialog != null)
                    {
                        if (Status.instance().lastDialog.isShowing())
                        {
                            Status.instance().lastDialog.dismiss();
                        }
                    }

                    Status.instance().lastDialog =
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("주의")
                                    .setMessage("이미 같은 이름이 등록되어있습니다. 수정을 원하는 경우, 사용자 목록에서 수정을 원하는 사용자 이름을 누르세요.")
                                    .setPositiveButton("확인", (dialogInterface, i) ->
                                    {
                                        // 장시간 미사용 핸들러 업데이트
                                        ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                    })
                                    .setCancelable(false)
                                    .create();
                    Status.instance().lastDialog.show();
                }
                else
                {
                    String nickName = Objects.requireNonNull(mAddUserBinding.nicknameEdittext.getText()).toString();
                    if (nickName.length() == 0)
                    {
                        user.nickname = "";
                    }
                    else
                    {
                        user.nickname = nickName;
                    }

                    if (user.defaultUser.equals(EntityUser.USER_DEFAULT))
                    {
                        EntityUser defaultUser = UtilUser.instance.getDefaultUser();

                        // 등록하려는데, 이미 기본사용자가 등록되어 있다면
                        if (defaultUser != null)
                        {
                            // 그런데 지금 그 사용자가 연결되어 있다면
                            if (((MainActivity) requireActivity()).mBluetoothGatt != null)
                            {
                                if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTING ||
                                        Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
                                {
                                    if (defaultUser.name.equals(Status.instance().connectedUser.name))
                                    {
                                        if (Status.instance().lastDialog != null)
                                        {
                                            if (Status.instance().lastDialog.isShowing())
                                            {
                                                Status.instance().lastDialog.dismiss();
                                            }
                                        }

                                        Status.instance().lastDialog =
                                                new MaterialAlertDialogBuilder(requireContext())
                                                        .setTitle("주의")
                                                        .setMessage("현재 연결중인 기본사용자가 있습니다. 연결을 해제하고, 입력하신 사용자를 기본사용자로 등록하시겠습니까?")
                                                        .setPositiveButton("확인", (dialogInterface, i) ->
                                                        {
                                                            // 장시간 미사용 핸들러 업데이트
                                                            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                                                            if (((MainActivity) requireActivity()).mBluetoothGatt != null)
                                                            {
                                                                if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED ||
                                                                        Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTING)
                                                                {
                                                                    Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                                                                    ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
                                                                }
                                                            }
                                                            mAddUserBinding.addButton.callOnClick();
                                                        })
                                                        .setNegativeButton("취소", (dialogInterface, i) ->
                                                        {
                                                            // 장시간 미사용 핸들러 업데이트
                                                            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                                        })
                                                        .setCancelable(false)
                                                        .create();
                                        Status.instance().lastDialog.show();
                                        return;
                                    }
                                }
                            }

                            defaultUser.defaultUser = EntityUser.USER_NOT_DEFAULT;
                            UtilUser.instance.update(defaultUser);
                        }
                    }

                    UtilUser.instance.insert(user);
                    UtilLog.instance.writeLog(
                            "사용자 추가 : 이름=" + user.name + ", 패스키=" + user.passKey +
                                    ", 별칭=" + user.nickname + ", 위치=" + user.ear + ", 기본사용자=" + user.defaultUser);

                    if (UtilUser.instance.getUserByName(user.name) != null)
                    {
                        requireActivity().onBackPressed();
                    }
                }
            }
        });
    }
}