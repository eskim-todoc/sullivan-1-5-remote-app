package todoc.cochlear.remoteapp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

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
    public FragmentAddUserBinding mAddUserBinding;

    public AddUserFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mAddUserBinding.nameEdittext.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mAddUserBinding.passkeyEdittext.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mAddUserBinding.nicknameEdittext.getWindowToken(), 0);

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
        mMainBinding.toolbarNavigationMessage.setText("사용자\n목록");
        mMainBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        mMainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_add_user));

        EntityUser defaultUser = UtilUser.instance.getDefaultUser();
        if (defaultUser == null)
        {
            mAddUserBinding.defaultCheckbox.setChecked(true);
        }

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


            // TD2-SW-RC-UNIT-Test-ID-72 [사용자 목록 표시 유닛] 순서[1] 시작.
            /*
            {
                Log.d(TAG, "name = " + easterEggName + ", passKey = " + easterEggPassKey + ", nickname = " + easterEggNickname);
                Log.d(TAG, "Log screen will be displayed.");
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-72 [사용자 목록 표시 유닛] 순서[1] 끝.


            /*
            requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new LogFragment()).commitAllowingStateLoss();
            */
            ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.HIDDEN_LOG);
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
                //mAddUserBinding.nameEdittext.setError("사용자 이니셜을 입력해주세요.");
                if (Status.instance().lastDialog != null)
                {
                    if (Status.instance().lastDialog.isShowing())
                    {
                        Status.instance().lastDialog.dismiss();
                    }
                }


                // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[1] 시작.
                /*
                {
                    Log.d(TAG, "name length = " + user.name.length());
                    Log.d(TAG, "name length must be greater than 1");
                }
                */
                // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[1] 끝.


                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("사용자 이니셜을 입력해주세요.")
                                .setPositiveButton("확인", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                })
                                .setCancelable(false)
                                .create();
                Status.instance().lastDialog.show();
            }
            else if (user.passKey == null || user.passKey.length() != 4)
            {
                //mAddUserBinding.passkeyEdittext.setError("내부기 키는 4자리 번호입니다.");
                if (Status.instance().lastDialog != null)
                {
                    if (Status.instance().lastDialog.isShowing())
                    {
                        Status.instance().lastDialog.dismiss();
                    }
                }


                // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[2] 시작.
                /*
                {
                    Log.d(TAG, "passKey length = " + user.passKey.length());
                    Log.d(TAG, "passKey length must be equals to 4");
                }
                */
                // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[2] 끝.


                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("내부기 키는 4자리 번호입니다.")
                                .setPositiveButton("확인", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                })
                                .setCancelable(false)
                                .create();
                Status.instance().lastDialog.show();
            }
            else if (user.ear == null)
            {
                if (Status.instance().lastDialog != null)
                {
                    if (Status.instance().lastDialog.isShowing())
                    {
                        Status.instance().lastDialog.dismiss();
                    }
                }


                // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[3] 시작.
                /*
                {
                    Log.d(TAG, "ear must be selected. (left or right)");
                }
                */
                // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[3] 끝.


                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("착용 위치를 선택해주세요.")
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
                user.name = user.name + "_" + user.ear; // 사용자 이니셜 + 착용 위치를 사용자 이름으로 구성한다.
                EntityUser readUser = UtilUser.instance.getUserByName(user.name);
                if (readUser != null) // 이미 등록된 사용자 정보일 때
                {
                    //mAddUserBinding.nameEdittext.setError("이미 등록된 사용자 이니셜입니다.");

                    if (Status.instance().lastDialog != null)
                    {
                        if (Status.instance().lastDialog.isShowing())
                        {
                            Status.instance().lastDialog.dismiss();
                        }
                    }


                    // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[4] 시작.
                    /*
                    {
                        Log.d(TAG, "name = " + user.name + ", passKey = " + user.passKey + ", nickname = " + user.nickname + ", ear = " + user.ear);
                        Log.d(TAG, "already registered user information.");
                    }
                    */
                    // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[4] 끝.


                    Status.instance().lastDialog =
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("주의")
                                    .setMessage("이미 등록된 사용자 정보입니다.")
                                    .setPositiveButton("확인", (dialogInterface, i) ->
                                    {
                                        // 장시간 미사용 핸들러 업데이트
                                        ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                    })
                                    .setCancelable(false)
                                    .create();
                    Status.instance().lastDialog.show();
                }
                else // 새로 등록하는 사용자 정보일 때
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

                    if (user.defaultUser.equals(EntityUser.USER_DEFAULT)) // 새로 등록하려는 정보가 기본사용자일 떄
                    {


                        // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[6] 시작.
                        /*
                        {
                            Log.d(TAG, "name = " + user.name + ", passKey = " + user.passKey + ", nickname = " + user.nickname + ", ear = " + user.ear);
                            Log.d(TAG, "register default user information.");
                        }
                        */
                        // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[6] 끝.


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
                                                        .setMessage("현재 연결을 해제하고 기본사용자 정보를 등록하시겠습니까?")
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
                            else
                            {
                                defaultUser.defaultUser = EntityUser.USER_NOT_DEFAULT;
                                UtilUser.instance.update(defaultUser);

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
                        else // 등록된 기본 사용자가 없을 때
                        {
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
                    else // 그냥 일반 사용자로 등록할 때
                    {


                        // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[5] 시작.
                        /*
                        {
                            Log.d(TAG, "name = " + user.name + ", passKey = " + user.passKey + ", nickname = " + user.nickname + ", ear = " + user.ear);
                            Log.d(TAG, "register user information.");
                        }
                        */
                        // TD2-SW-RC-UNIT-Test-ID-73 [사용자 등록 유닛] 순서[5] 끝.


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
            }
        });
    }
}