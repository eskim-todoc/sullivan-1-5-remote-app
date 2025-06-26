package todoc.cochlear.remoteapp.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentRemoteControlBinding;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.ota.Ota;
import todoc.cochlear.remoteapp.params.Status;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;

public class RemoteControlFragment extends Fragment
{
    static final private String TAG = "TODOC_" + RemoteControlFragment.class.getSimpleName();

    public ActivityMainBinding          mMainBinding;
    public FragmentRemoteControlBinding mRemoteControlBinding;

    StatusViewModel mStatusViewModel;

    public AlertDialog mDialog;

    Activity     mActivity;
    MainActivity mMainActivity;
    Ota          mOta;

    public RemoteControlFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        if (mDialog != null && mDialog.isShowing())
        {
            mDialog.dismiss();
            mDialog = null;
        }

        mRemoteControlBinding.remoteControlSearchingAnimator.stopRippleAnimation();

        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainBinding = ((MainActivity) requireActivity()).mBinding;
        mMainBinding.toolbar.setNavigationIcon(null);
        mMainBinding.toolbarNavigationMessage.setText("");
        mMainBinding.toolbarNavigationMessage.setVisibility(View.GONE);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(true);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(true);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        mMainBinding.toolbar.setTitle("");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mRemoteControlBinding = FragmentRemoteControlBinding.inflate(inflater, container, false);
        return mRemoteControlBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated is called!");

        mActivity = requireActivity();
        mMainActivity = (MainActivity) mActivity;

        mStatusViewModel = new ViewModelProvider(requireActivity()).get(StatusViewModel.class);

        liveDataConnection(); // LiveData for connection

        liveDataIsdID(); // LiveData for status

        checkRegisteredList_userAndDevice(); // User list

        mOta = Ota.getInstance();

        initFile_manifest();
        initFile_app000();
        initFile_app001();
        initFile_app002();
        init_applyOTA();
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Read Thread
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeThread_read(int param)
    {
        mOta.mThread_param = param;
        mOta.mThread_state = Ota.THREAD_STATE_BUSY;

        // 파일 읽기 및 데이터를 저장하는 시간이 오래 걸리므로 쓰레드에서 작업을 진행한다.
        // 메인 쓰레드에서는 100msec 정도 작업이 지연되면 ANR 에러가 발생할 수 있다.
        new Thread(() ->
        {
            Ota.OtaFile otaFile = mOta.getOtaFile(mOta.mThread_param);

            File file = new File(otaFile.mPath);

            try (FileInputStream fis = new FileInputStream(file))
            {
                int b;
                int total        = 0;
                int last_percent = -1;
                int percent      = 0;

                while ((b = fis.read()) != -1)
                {
                    otaFile.mBuffer[total++] = (byte) (b & 0xFF);

                    percent = (int) ((total * 100) / otaFile.mLength);

                    if (last_percent != percent)
                    {
                        last_percent = percent;

                        String string_percent = percent + "%";
                        mActivity.runOnUiThread(() ->
                        {
                            otaFile.mTv_readPercent.setText(string_percent);
                        });
                    }


                }

                fis.close();
                otaFile.mReadDone = true;

                print_dfuBuffer(mOta.mThread_param, mMainActivity);
            }
            catch (IOException e)
            {
                Log.d(TAG, "[OTA] 익셉션 발생!");
                e.printStackTrace();
            }


            mOta.mThread_param = Ota.PARAM_NONE;
            mOta.mThread_state = Ota.THREAD_STATE_IDLE;
        }).start();

    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Read button - MANIFEST.TXT
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_readButton_manifest = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            mMainActivity.longTimeIdleHandlerUpdate(true);

            if (mOta.mThread_state == Ota.THREAD_STATE_BUSY)
            {
                Log.d(TAG, "[OTA] 이미 쓰레드 동작 중");
                return;
            }

            if (mOta.mFile_manifest != null)
            {
                if (mOta.mFile_manifest.mReadDone)
                {
                    Log.d(TAG, "[OTA] 이미 읽었음 → " + "파일 " + mOta.mFile_manifest.mName + ", 크기 = " + mOta.mFile_manifest.mLength + " 바이트");
                    return;
                }
            }
            else
            {
                return;
            }

            makeThread_read(Ota.PARAM_MANIFEST);
        } // onClick;
    }; // listener;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Read button - APP000.FEZ
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_readButton_app000 = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            mMainActivity.longTimeIdleHandlerUpdate(true);

            if (mOta.mThread_state == Ota.THREAD_STATE_BUSY)
            {
                Log.d(TAG, "[OTA] 이미 쓰레드 동작 중");
                return;
            }

            if (mOta.mFile_app000 != null)
            {
                if (mOta.mFile_app000.mReadDone)
                {
                    Log.d(TAG, "[OTA] 이미 읽었음 → " + "파일 " + mOta.mFile_app000.mName + ", 크기 = " + mOta.mFile_app000.mLength + " 바이트");
                    return;
                }
            }
            else
            {
                return;
            }

            makeThread_read(Ota.PARAM_APP000);
        } // onClick;
    }; // listener;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Read button - APP001.FEZ
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_readButton_app001 = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            mMainActivity.longTimeIdleHandlerUpdate(true);

            if (mOta.mThread_state == Ota.THREAD_STATE_BUSY)
            {
                Log.d(TAG, "[OTA] 이미 쓰레드 동작 중");
                return;
            }

            if (mOta.mFile_app001 != null)
            {
                if (mOta.mFile_app001.mReadDone)
                {
                    Log.d(TAG, "[OTA] 이미 읽었음 → " + "파일 " + mOta.mFile_app001.mName + ", 크기 = " + mOta.mFile_app001.mLength + " 바이트");
                    return;
                }
            }
            else
            {
                return;
            }

            makeThread_read(Ota.PARAM_APP001);
        } // onClick;
    }; // listener;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Read button - APP002.FEZ
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_readButton_app002 = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            mMainActivity.longTimeIdleHandlerUpdate(true);

            if (mOta.mThread_state == Ota.THREAD_STATE_BUSY)
            {
                Log.d(TAG, "[OTA] 이미 쓰레드 동작 중");
                return;
            }

            if (mOta.mFile_app002 != null)
            {
                if (mOta.mFile_app002.mReadDone)
                {
                    Log.d(TAG, "[OTA] 이미 읽었음 → " + "파일 " + mOta.mFile_app002.mName + ", 크기 = " + mOta.mFile_app002.mLength + " 바이트");
                    return;
                }
            }
            else
            {
                return;
            }

            makeThread_read(Ota.PARAM_APP002);
        } // onClick;
    }; // listener;

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Send button - Common use
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void onClick_sendButton_commonUse(Ota.OtaFile otaFile)
    {
        if (!otaFile.mReadDone)
        {
            Log.d(TAG, "아직 " + otaFile.mName + " 파일 읽기가 진행 되지 않음");
            return;
        }

        if (mOta.mCommState != Ota.COMM_STATE_IDLE)
        {
            Log.d(TAG, "이미 무선 프로토콜 전송 중, 현재 상태 = " + mOta.mCommState);
            return;
        }

        byte[] packet = mOta.prepare_dfuCommPacket(otaFile.mParam);

        if (packet == null)
        {
            mOta.mCommState = Ota.COMM_STATE_IDLE;
        }

        mOta.mCommState = Ota.COMM_STATE_SEND_COMMAND;

        mMainActivity.mCheckBatteryHandler.removeCallbacks(mMainActivity.mCheckBatteryRunner); // 배터리 체크 패킷 핸들러 제거
        mMainActivity.sendPacket(packet);

        mOta.mCommState = Ota.COMM_STATE_WAIT_RESP_COMMAND;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Send button - MANIFEST.TXT
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_sendButton_manifest = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            Ota.OtaFile otaFile = mOta.getOtaFile(Ota.PARAM_MANIFEST);
            onClick_sendButton_commonUse(otaFile);
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Send button - APP000.FEZ
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_sendButton_app000 = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            Ota.OtaFile otaFile = mOta.getOtaFile(Ota.PARAM_APP000);
            onClick_sendButton_commonUse(otaFile);
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Send button - APP001.FEZ
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_sendButton_app001 = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            Ota.OtaFile otaFile = mOta.getOtaFile(Ota.PARAM_APP001);
            onClick_sendButton_commonUse(otaFile);
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Send button - APP002.FEZ
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_sendButton_app002 = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            Ota.OtaFile otaFile = mOta.getOtaFile(Ota.PARAM_APP002);
            onClick_sendButton_commonUse(otaFile);
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Release button
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_releaseButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (mOta.mFile_manifest == null || mOta.mFile_app000 == null || mOta.mFile_app001 == null || mOta.mFile_app002 == null)
            {
                makeDialog_noFileOTA();
                return;
            }

            if (!mOta.mFile_manifest.mReadDone || !mOta.mFile_app000.mReadDone || !mOta.mFile_app001.mReadDone || !mOta.mFile_app002.mReadDone)
            {
                makeDialog_readImage_notFinish();
                return;
            }

            if (!mOta.mFile_manifest.mSendDone || !mOta.mFile_app000.mSendDone || !mOta.mFile_app001.mSendDone || !mOta.mFile_app002.mSendDone)
            {
                makeDialog_sendImage_notFinish();
                return;
            }

            Ota.OtaFile otaFile = mOta.getOtaFile(Ota.PARAM_STATUS);
            onClick_sendButton_commonUse(otaFile);
        }
    };

    private void initFile_manifest()
    {
        int    length;
        String name = Ota.FILE_NAME_MANIFEST;
        String path = make_path(name);
        File   file = new File(path);

        if (!file.exists())
        {
            Log.d(TAG, "[OTA] 파일 " + name + " 없음");

            mRemoteControlBinding.otaManifestNameTextView.setText("");
            mRemoteControlBinding.otaManifestLayout.setVisibility(View.GONE);
        }
        else
        {
            length = (int) file.length();
            Log.d(TAG, "[OTA] 파일 " + name + " 확인, 크기 = " + length + " 바이트");

            mOta.mFile_manifest = new Ota.OtaFile(Ota.FILE_NAME_MANIFEST, Ota.PARAM_MANIFEST, length);
            mOta.mFile_manifest.mPath = path;
            mOta.mFile_manifest.mTv_readPercent = mRemoteControlBinding.otaManifestReadPercentTextView;
            mOta.mFile_manifest.mTv_sendPercent = mRemoteControlBinding.otaManifestSendPercentTextView;

            mRemoteControlBinding.otaManifestNameTextView.setText(name);
            mRemoteControlBinding.otaManifestLayout.setVisibility(View.VISIBLE);
            mRemoteControlBinding.otaManifestReadPercentTextView.setText("0%");
            mRemoteControlBinding.otaManifestSendPercentTextView.setText("0%");

            mRemoteControlBinding.otaManifestReadImageButton.setOnClickListener(onClick_readButton_manifest);
            mRemoteControlBinding.otaManifestSendImageButton.setOnClickListener(onClick_sendButton_manifest);
        }
    }

    private void initFile_app000()
    {
        int    length;
        String name = Ota.FILE_NAME_APP000;
        String path = make_path(name);
        File   file = new File(path);

        if (!file.exists())
        {
            Log.d(TAG, "[OTA] 파일 " + name + " 없음");

            mRemoteControlBinding.otaApp000NameTextView.setText("");
            mRemoteControlBinding.otaApp000Layout.setVisibility(View.GONE);
        }
        else
        {
            length = (int) file.length();
            Log.d(TAG, "[OTA] 파일 " + name + " 확인, 크기 = " + length + " 바이트");

            mOta.mFile_app000 = new Ota.OtaFile(Ota.FILE_NAME_APP000, Ota.PARAM_APP000, length);
            mOta.mFile_app000.mPath = path;
            mOta.mFile_app000.mTv_readPercent = mRemoteControlBinding.otaApp000ReadPercentTextView;
            mOta.mFile_app000.mTv_sendPercent = mRemoteControlBinding.otaApp000SendPercentTextView;

            mRemoteControlBinding.otaApp000NameTextView.setText(name);
            mRemoteControlBinding.otaApp000Layout.setVisibility(View.VISIBLE);
            mRemoteControlBinding.otaApp000ReadPercentTextView.setText("0%");
            mRemoteControlBinding.otaApp000SendPercentTextView.setText("0%");

            mRemoteControlBinding.otaApp000ReadImageButton.setOnClickListener(onClick_readButton_app000);
            mRemoteControlBinding.otaApp000SendImageButton.setOnClickListener(onClick_sendButton_app000);
        }
    }

    private void initFile_app001()
    {
        int    length;
        String name = Ota.FILE_NAME_APP001;
        String path = make_path(name);
        File   file = new File(path);

        if (!file.exists())
        {
            Log.d(TAG, "[OTA] 파일 " + name + " 없음");

            mRemoteControlBinding.otaApp001NameTextView.setText("");
            mRemoteControlBinding.otaApp001Layout.setVisibility(View.GONE);
        }
        else
        {
            length = (int) file.length();
            Log.d(TAG, "[OTA] 파일 " + name + " 확인, 크기 = " + length + " 바이트");

            mOta.mFile_app001 = new Ota.OtaFile(Ota.FILE_NAME_APP001, Ota.PARAM_APP001, length);
            mOta.mFile_app001.mPath = path;
            mOta.mFile_app001.mTv_readPercent = mRemoteControlBinding.otaApp001ReadPercentTextView;
            mOta.mFile_app001.mTv_sendPercent = mRemoteControlBinding.otaApp001SendPercentTextView;

            mRemoteControlBinding.otaApp001NameTextView.setText(name);
            mRemoteControlBinding.otaApp001Layout.setVisibility(View.VISIBLE);
            mRemoteControlBinding.otaApp001ReadPercentTextView.setText("0%");
            mRemoteControlBinding.otaApp001SendPercentTextView.setText("0%");

            mRemoteControlBinding.otaApp001ReadImageButton.setOnClickListener(onClick_readButton_app001);
            mRemoteControlBinding.otaApp001SendImageButton.setOnClickListener(onClick_sendButton_app001);
        }
    }

    private void initFile_app002()
    {
        int    length;
        String name = Ota.FILE_NAME_APP002;
        String path = make_path(name);
        File   file = new File(path);

        if (!file.exists())
        {
            Log.d(TAG, "[OTA] 파일 " + name + " 없음");

            mRemoteControlBinding.otaApp002NameTextView.setText("");
            mRemoteControlBinding.otaApp002Layout.setVisibility(View.GONE);
        }
        else
        {
            length = (int) file.length();
            Log.d(TAG, "[OTA] 파일 " + name + " 확인, 크기 = " + length + " 바이트");

            mOta.mFile_app002 = new Ota.OtaFile(Ota.FILE_NAME_APP002, Ota.PARAM_APP002, length);
            mOta.mFile_app002.mPath = path;
            mOta.mFile_app002.mTv_readPercent = mRemoteControlBinding.otaApp002ReadPercentTextView;
            mOta.mFile_app002.mTv_sendPercent = mRemoteControlBinding.otaApp002SendPercentTextView;

            mRemoteControlBinding.otaApp002NameTextView.setText(name);
            mRemoteControlBinding.otaApp002Layout.setVisibility(View.VISIBLE);
            mRemoteControlBinding.otaApp002ReadPercentTextView.setText("0%");
            mRemoteControlBinding.otaApp002SendPercentTextView.setText("0%");

            mRemoteControlBinding.otaApp002ReadImageButton.setOnClickListener(onClick_readButton_app002);
            mRemoteControlBinding.otaApp002SendImageButton.setOnClickListener(onClick_sendButton_app002);
        }
    }

    private void init_applyOTA()
    {
        Ota.OtaFile otaFile;

        mOta.mFile_status = new Ota.OtaFile(Ota.FILE_NAME_STATUS, Ota.PARAM_STATUS, 1);

        otaFile = mOta.getOtaFile(Ota.PARAM_STATUS);
        otaFile.mBuffer[0] = 1;
        otaFile.mReadDone = true;

        mRemoteControlBinding.otaReleaseMaterialButton.setOnClickListener(onClick_releaseButton);
    }

    private String make_path(String file_name)
    {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + Ota.OTA_PATH + file_name;
    }

    private void print_dfuBuffer(int otaParam, MainActivity mainActivity)
    {
        ArrayList<String> stringList = new ArrayList<>();
        byte[]            buffer     = new byte[Ota.PRINT_LOG_HEX_LENGTH];
        int               cnt        = 0;

        Ota.OtaFile otaFile = mOta.getOtaFile(otaParam);

        if (!otaFile.mReadDone)
        {
            return;
        }

        for (int i = 0; i < otaFile.mLength; i++)
        {
            buffer[cnt++] = otaFile.mBuffer[i];

            if (cnt == Ota.PRINT_LOG_HEX_LENGTH || i == (otaFile.mLength - 1))
            {
                StringBuilder sb = new StringBuilder();

                for (int k = 0; k < cnt; k++)
                {
                    sb.append(String.format("%02X ", buffer[k]));
                }

                stringList.add(sb.toString());
                cnt = 0;
            }
        }

        Log.d(TAG, "[OTA] 파일 " + otaFile.mName);

        for (int i = 0; i < stringList.size(); i++)
        {
            Log.d(TAG, stringList.get(i));
        }
    }

    // LiveData - Connection
    private void liveDataConnection()
    {
        mStatusViewModel.getObjectConnectionState().observe(getViewLifecycleOwner(), integer ->
        {
            Log.v(TAG, "옵저버 : 연결상태 -> " + integer);

            if (integer == StatusViewModel.CONNECTION_STATE_DISCONNECTED)
            {
                mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(View.VISIBLE);

                if (Status.instance().scanState == Status.SCAN_STATE_STOPPED)
                {
                    mRemoteControlBinding.remoteControlSearchingAnimator.stopRippleAnimation();
                    mRemoteControlBinding.remoteControlFindLayout.setVisibility(View.VISIBLE);
                    mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(true);
                }
                else
                {
                    mRemoteControlBinding.remoteControlFindLayout.setVisibility(View.VISIBLE);
                    mRemoteControlBinding.remoteControlSearchingAnimator.startRippleAnimation();
                    mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
                }
            }
            else
            {
                if (integer == StatusViewModel.CONNECTION_STATE_CONNECTING)
                {
                    mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(View.VISIBLE);
                }
                else // CONNECTED
                {
                    mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(View.GONE);
                    mRemoteControlBinding.remoteControlSearchingAnimator.stopRippleAnimation();
                    mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
                }
            }
        });
    }

    // LiveData - IsdID
    private void liveDataIsdID()
    {
        mStatusViewModel.getLiveDataIsdID().observe(getViewLifecycleOwner(), o ->
        {
            int    value     = mStatusViewModel.getValueIsdID();
            String textValue = String.format("#%08X", value);
            Log.v(TAG, "옵저버 : 내부기 ID -> " + textValue);
            mRemoteControlBinding.remoteControlConnectionIsdIdTextview.setText(textValue);
        });
    }

    public void checkRegisteredList_userAndDevice()
    {
        if (mMainBinding.lockScreen.getVisibility() != View.VISIBLE)
        {
            if (UtilUser.instance.getUsers().isEmpty())
            {
                makeDialog_noUser();
            }
            else
            {
                if (UtilDevice.instance.getDevices().isEmpty())
                {
                    makeDialog_noDevice();
                }
                else
                {
                    EntityUser defaultUser = UtilUser.instance.getDefaultUser();

                    if (defaultUser != null)
                    {
                        if (defaultUser.nickname != null && !defaultUser.nickname.isEmpty())
                        {
                            mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(defaultUser.nickname);
                        }
                        else
                        {
                            String name = defaultUser.name.substring(0, defaultUser.name.length() - 2);
                            if (defaultUser.ear.equals(EntityUser.EAR_LEFT))
                            {
                                name = name + " (왼쪽)";
                            }
                            else
                            {
                                if (defaultUser.ear.equals(EntityUser.EAR_RIGHT))
                                {
                                    name = name + " (오른쪽)";
                                }
                            }

                            mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(name);
                        }

                        if (mMainBinding.lockScreen.getVisibility() == View.GONE)
                        {
                            if (Status.instance().connectionState == Status.CONNECTION_STATE_DISCONNECTED)
                            {
                                Log.d(TAG, "연결 해제 상태이므로 검색을 시작합니다.");
                                ((MainActivity) requireActivity()).scanLeWithDelay(true, 0);
                            }
                            else
                            {
                                Log.d(TAG, "연결 해제 상태가 아닙니다.");
                            }
                        }
                    }
                    else
                    {
                        mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText("");
                        ((MainActivity) requireActivity()).makeDialogSelectUser();
                    }
                }
            }
        }
    }

    public void makeDialog_finishOTA()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                    .setTitle("안내") //
                    .setMessage("OTA 이미지 전송이 완료되었습니다.") //
                    .setPositiveButton("확인", null) //
                    .setCancelable(false) //
                    .create();

            mDialog.show();
        }
    }

    // OTA 수집 미완료 다이얼로그
    private void makeDialog_readImage_notFinish()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                    .setTitle("안내") //
                    .setMessage("OTA 이미지 수집이 완료되지 않았습니다.") //
                    .setPositiveButton("확인", null) //
                    .setCancelable(false) //
                    .create();

            mDialog.show();
        }
    }

    // OTA 전송 미완료 다이얼로그
    private void makeDialog_sendImage_notFinish()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                    .setTitle("안내") //
                    .setMessage("OTA 이미지 전송이 완료되지 않았습니다.") //
                    .setPositiveButton("확인", null) //
                    .setCancelable(false) //
                    .create();

            mDialog.show();
        }
    }

    // OTA 파일 없음 다이얼로그
    private void makeDialog_noFileOTA()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                    .setTitle("안내") //
                    .setMessage("OTA 이미지가 준비되지 않았습니다.") //
                    .setPositiveButton("확인", null) //
                    .setCancelable(false) //
                    .create();

            mDialog.show();
        }
    }

    // NO 사용자 다이얼로그
    private void makeDialog_noUser()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.remote_control_no_user_dialog_title)).setMessage(getString(R.string.remote_control_no_user_dialog_message)).setPositiveButton(getString(R.string.remote_control_no_user_positive), (dialogInterface, i) ->
            {
                // 장시간 미사용 핸들러 업데이트
                ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                mDialog.dismiss();
                mDialog = null;
                ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.USER_ADD);
            }).setCancelable(false).create();

            mDialog.show();
        }
    }

    // NO 사운드처리기 다이얼로그
    private void makeDialog_noDevice()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.remote_control_no_device_dialog_title)).setMessage(getString(R.string.remote_control_no_device_dialog_message)).setPositiveButton(getString(R.string.remote_control_no_device_positive), (dialogInterface, i) ->
            {
                // 장시간 미사용 핸들러 업데이트
                ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                mDialog.dismiss();
                mDialog = null;
                ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.DEVICE_ADD);
            }).setCancelable(false).create();

            mDialog.show();
        }
    }
}