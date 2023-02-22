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
import androidx.renderscript.ScriptGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentEditUserBinding;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.params.Status;

public class EditUserFragment extends Fragment
{
    static private final String TAG = "TODOC_" + EditUserFragment.class.getSimpleName();

    static public final String ARG_NAME = "name";
    static public final String ARG_PASSKEY = "passkey";
    static public final String ARG_NICKNAME = "nickname";
    static public final String ARG_EAR = "ear";
    static public final String ARG_DEFAULT = "default";

    public FragmentEditUserBinding mEditUserBinding;
    private EntityUser mItem;

    public EditUserFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();

        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditUserBinding.edittextName.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mEditUserBinding.edittextPasskey.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(mEditUserBinding.edittextNickname.getWindowToken(), 0);

        mEditUserBinding = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding mainBinding = ((MainActivity) requireContext()).mBinding;
        mainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mainBinding.toolbarNavigationMessage.setText("사용자\n목록");
        mainBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        mainBinding.toolbar.setTitle(requireContext().getString(R.string.toolbar_title_edit_user));

        Bundle bundle = getArguments();

        if (bundle != null)
        {
            mItem = new EntityUser();
            mItem.name = bundle.getString(ARG_NAME);
            mItem.passKey = bundle.getString(ARG_PASSKEY);
            mItem.nickname = bundle.getString(ARG_NICKNAME);
            mItem.ear = bundle.getString(ARG_EAR);
            mItem.defaultUser = bundle.getString(ARG_DEFAULT);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mEditUserBinding = FragmentEditUserBinding.inflate(inflater, container, false);
        return mEditUserBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        fillInformation();
        initEditButton();
        initDeleteButton();
    }

    private void fillInformation()
    {
        mEditUserBinding.edittextName.setText(mItem.name.substring(0, mItem.name.length() - 2));
        mEditUserBinding.edittextPasskey.setText(mItem.passKey);
        mEditUserBinding.edittextNickname.setText(mItem.nickname);

        if (mItem.ear.equals(EntityUser.EAR_LEFT))
        {
            mEditUserBinding.editUserLeftRadiobutton.setChecked(true);
            mEditUserBinding.editUserRightRadiobutton.setVisibility(View.GONE);
        }
        else if (mItem.ear.equals(EntityUser.EAR_RIGHT))
        {
            mEditUserBinding.editUserRightRadiobutton.setChecked(true);
            mEditUserBinding.editUserLeftRadiobutton.setVisibility(View.GONE);
        }

        boolean isDefault = mItem.defaultUser.equals(EntityUser.USER_DEFAULT);
        mEditUserBinding.checkboxDefaultUser.setChecked(isDefault);
    }

    private void initDeleteButton()
    {
        mEditUserBinding.editUserDelButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (Status.instance().lastDialog != null)
            {
                if (Status.instance().lastDialog.isShowing())
                {
                    Status.instance().lastDialog.dismiss();
                }
            }

            // 현재 어떠한 사용자와 연결되어 있는 상태라면, 지금 제거하려는 사용자인지 체크한다.
            if ((Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED
                    || Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTING)
                    && Status.instance().connectedUser.name.equals(mItem.name))
            {
                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("현재 연결을 해제하고 사용자 정보를 삭제하시겠습니까?")
                                .setPositiveButton("삭제", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                                    UtilUser.instance.delete(mItem);

                                    UtilLog.instance.writeLog(
                                            "사용자 제거 : 이름=" + mItem.name + ", 패스키=" + mItem.passKey +
                                                    ", 별칭=" + mItem.nickname + ", 위치=" + mItem.ear + ", 기본사용자=" + mItem.defaultUser);

                                    if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED
                                            || Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTING)
                                    {
                                        Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                                        ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
                                    }

                                    requireActivity().onBackPressed();
                                })
                                .setNegativeButton("취소", (dialogInterface, i) ->
                                {
                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);
                                })
                                .setCancelable(false)
                                .create();
            }
            else
            {
                Status.instance().lastDialog =
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle("주의")
                                .setMessage("사용자 정보를 삭제하시겠습니까?")
                                .setPositiveButton("삭제", (dialogInterface, i) ->
                                {


                                    // TD2-SW-RC-UNIT-Test-ID-63 [사용자 삭제 유닛] 순서[2] 시작.
                                    /*
                                    {
                                        Log.d(TAG, "name = " + mItem.name + ", passKey = " + mItem.passKey + ", nickname = " + mItem.nickname + ", ear = " + mItem.ear + ", defaultUser = " + mItem.defaultUser);
                                        Log.d(TAG, "delete user information.");
                                    }
                                    */
                                    // TD2-SW-RC-UNIT-Test-ID-63 [사용자 삭제 유닛] 순서[2] 끝.


                                    // 장시간 미사용 핸들러 업데이트
                                    ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                                    UtilUser.instance.delete(mItem);

                                    UtilLog.instance.writeLog(
                                            "사용자 제거 : 이름=" + mItem.name + ", 패스키=" + mItem.passKey +
                                                    ", 별칭=" + mItem.nickname + ", 위치=" + mItem.ear + ", 기본사용자=" + mItem.defaultUser);

                                    requireActivity().onBackPressed();
                                })
                                .setNegativeButton("취소", null)
                                .setCancelable(false)
                                .create();
            }
            Status.instance().lastDialog.show();
        });
    }

    private void initEditButton()
    {
        mEditUserBinding.editUserEditButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            String passKey = Objects.requireNonNull(mEditUserBinding.edittextPasskey.getText()).toString();
            if (passKey.length() != 4)
            {
                //mEditUserBinding.edittextPasskey.setError("내부기 키는 4자리 번호입니다.");
                if (Status.instance().lastDialog != null)
                {
                    if (Status.instance().lastDialog.isShowing())
                    {
                        Status.instance().lastDialog.dismiss();
                    }
                }


                // TD2-SW-RC-UNIT-Test-ID-62 [사용자 수정 유닛] 순서[2] 시작.
                /*
                {
                    Log.d(TAG, "passKey length = " + passKey.length());
                    Log.d(TAG, "passKey length must be equals to 4");
                }
                */
                // TD2-SW-RC-UNIT-Test-ID-62 [사용자 수정 유닛] 순서[2] 끝.


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
            else // 보안코드에 문제 없음
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

                String nickName = Objects.requireNonNull(mEditUserBinding.edittextNickname.getText()).toString();
                if (nickName.length() == 0)
                {
                    mItem.nickname = "";
                }
                else
                {
                    mItem.nickname = nickName;
                }

                if (mItem.defaultUser.equals(EntityUser.USER_NOT_DEFAULT))
                {
                    //기본사용자를 해제했는데,
                    // 현재 연결된 사용자가 이 사용자라면 연결을 해제하고 수정을 진행할 것인지 물어야 한다.
                    if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED ||
                            Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTING)
                    {
                        if (mItem.name.equals(Status.instance().connectedUser.name))
                        {
                            Log.d(TAG, "기본사용자를 해제하려고 하는데, 현재 연결중인 사용자입니다. 연결을 끊고 기본사용자를 해제할지 물어보기 위해 다이얼로그를 생성합니다.");

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
                                            .setMessage("현재 연결을 해제하고 사용자 정보를 수정하시겠습니까?")
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
                                                mEditUserBinding.editUserEditButton.callOnClick();
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

                if (mItem.defaultUser.equals(EntityUser.USER_DEFAULT))
                {
                    // 기본사용자로 선택했을 때, 현재 연결된 사용자가 있는지 확인하여 현재 연결중인 사용자가
                    // 지금 선택한 기본사용자랑 다르다면, 연결을 끊고 기본사용자를 변경할 것인지 물어보도록 한다.
                    if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTING ||
                            Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
                    {
                        if (((MainActivity) requireActivity()).mBluetoothGatt != null)
                        {
                            // 현재 연결중인 사용자와 지금 선택한 기본사용자 이름이 다르다.
                            if (!Status.instance().connectedUser.name.equals(mItem.name))
                            {
                                Log.d(TAG, "기본사용자를 변경하려고 하는데, 현재 연결중인 사용자가 있습니다. 연결을 끊고 기본사용자 변경을 할지 물어보기 위해 다이얼로그를 생성합니다.");

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
                                                .setMessage("현재 연결을 해제하고 사용자 정보를 수정하시겠습니까?")
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
                                                    mEditUserBinding.editUserEditButton.callOnClick();
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

                    EntityUser defaultUSer = UtilUser.instance.getDefaultUser();

                    if (defaultUSer != null)
                    {
                        if (!defaultUSer.name.equals(mItem.name))
                        {
                            defaultUSer.defaultUser = EntityUser.USER_NOT_DEFAULT;
                            UtilUser.instance.update(defaultUSer);
                        }
                    }
                }


                // TD2-SW-RC-UNIT-Test-ID-62 [사용자 수정 유닛] 순서[3] 시작.
                /*
                {
                    Log.d(TAG, "name = " + mItem.name + ", passKey = " + mItem.passKey + ", nickname = " + mItem.nickname + ", ear = " + mItem.ear + ", defaultUser = " + mItem.defaultUser);
                    Log.d(TAG, "edit user information.");
                }
                */
                // TD2-SW-RC-UNIT-Test-ID-62 [사용자 수정 유닛] 순서[3] 끝.


                UtilUser.instance.update(mItem);
                UtilLog.instance.writeLog(
                        "사용자 수정 : 이름=" + mItem.name + ", 패스키=" + mItem.passKey +
                                ", 별칭=" + mItem.nickname + ", 위치=" + mItem.ear + ", 기본사용자=" + mItem.defaultUser);

                requireActivity().onBackPressed();
            }
        });
    }
}