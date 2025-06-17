package todoc.cochlear.remoteapp.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
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
import todoc.cochlear.remoteapp.dfu.Dfu;
import todoc.cochlear.remoteapp.params.PacketInfo;
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
    Dfu          mDfu;

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
        //mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Default_Headline6);
        //mMainBinding.toolbar.setTitle("리모컨");
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

        mStatusViewModel = new ViewModelProvider(requireActivity()).get(StatusViewModel.class);

        // LiveData for connection
        liveDataConnection();

        // LiveData for status
        liveDataIsdID();
        liveDataBattery();
        liveDataNotification();
        liveDataLed();
        liveDataTelecoil();
        liveDataMaxOutput();
        liveDataVolume();
        liveDataProgram();

        // Click
        clickNotification();
        clickLed();
        //clickTelecoil();
        clickMaxOutput();
        clickVolume();
        clickProgram();

        // User list
        checkRegisteredList();
        //initChipGroup();

        /* DFU 관련 */
        mActivity = requireActivity();
        mMainActivity = (MainActivity) mActivity;
        mDfu = Dfu.getInstance();

        mDfu.mCommState = Dfu.COMM_STATE_IDLE;
        mDfu.mCommDataIndex = 0;
        mDfu.mManifest.readDone = false;
        mDfu.mApp000.readDone = false;
        mDfu.mApp001.readDone = false;
        mDfu.mApp002.readDone = false;

        dfuClickReadManifest();
        dfuClickReadApp000();
        dfuClickReadApp001();
        dfuClickReadApp002();

        dfuClickSendManifest();
        dfuClickSendApp000();
        dfuClickSendApp001();
        dfuClickSendApp002();

        dfuClickUpdateBtn();

        // 현재 BLE 연결된 상태가 아니고, BLE 스캔도 멈춰 있는 상태라면 스캔을 시작한다.
        /*
        Status status = Status.instance();

        if (status.connectionState == Status.CONNECTION_STATE_DISCONNECTED)
        {
            if (status.scanState == Status.SCAN_STATE_STOPPED)
            {
                EntityUser defaultUser = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);
                List<EntityDevice> devices = ((MainActivity) requireActivity()).mDatabaseDevices.daoDevices().findAll();

                if ((defaultUser != null) && (0 < devices.size()))
                {
                    ((MainActivity) requireActivity()).scanLe(true);
                }
            }
        }
        */


        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[1] 시작.
        /*
        {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        Log.d(TAG, "Init the Status View Model.");
                        mStatusViewModel.setConnectionState(StatusViewModel.CONNECTION_STATE_DISCONNECTED);
                        mStatusViewModel.setValueBatteryLevel(0);
                        mStatusViewModel.setValueNotification(PacketInfo.NOTIFICATION_OFF);
                        mStatusViewModel.setValueLed(PacketInfo.LED_OFF);
                        mStatusViewModel.setValueMaxOutput(PacketInfo.INIT_VALUE_MAX_OUTPUT);
                        mStatusViewModel.setValueVolume(PacketInfo.INIT_VALUE_VOLUME);
                        mStatusViewModel.setValueProgram(PacketInfo.INIT_VALUE_PROGRAM);
                    },
                    5000);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[1] 끝.


        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[2] 입력 시작.
        /*
        {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        mStatusViewModel.setConnectionState(StatusViewModel.CONNECTION_STATE_CONNECTED);
                    },
                    7000);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[2] 입력 끝.


        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[3] 입력 시작.
        /*
        {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        mStatusViewModel.setValueBatteryLevel(40);
                    },
                    7000);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[3] 입력 끝.


        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[4] 입력 시작.
        /*
        {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        mStatusViewModel.setValueNotification(PacketInfo.NOTIFICATION_ON);
                    },
                    7000);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[4] 입력 끝.


        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[5] 입력 시작.
        /*
        {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        mStatusViewModel.setValueLed(PacketInfo.LED_ON);
                    },
                    7000);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[5] 입력 끝.


        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[6] 입력 시작.
        /*
        {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        mStatusViewModel.setValueTelecoil(PacketInfo.TELECOIL_ON);
                    },
                    7000);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[6] 입력 끝.


        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[7] 입력 시작.
        /*
        {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        mStatusViewModel.setValueMaxOutput(3);
                    },
                    7000);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[7] 입력 끝.


        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[8] 입력 시작.
        /*
        {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        mStatusViewModel.setValueVolume(7);
                    },
                    7000);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[8] 입력 끝.


        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[9] 입력 시작.
        /*
        {
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        mStatusViewModel.setValueProgram(3);
                    },
                    7000);
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[9] 입력 끝.


    }

    // LiveData - Connection
    private void liveDataConnection()
    {
        mStatusViewModel.getObjectConnectionState().observe(getViewLifecycleOwner(), integer ->
        {


            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[2] 시작.
            /*
            {
                Log.d(TAG, "observe --> connection state = " + integer);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[2] 끝.


            Log.v(TAG, "옵저버 : 연결상태 -> " + integer);

            if (integer == StatusViewModel.CONNECTION_STATE_DISCONNECTED)
            {
                mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(View.VISIBLE);

                if (Status.instance().scanState == Status.SCAN_STATE_STOPPED)
                {
                    mRemoteControlBinding.remoteControlSearchingAnimator.stopRippleAnimation();
                    mRemoteControlBinding.remoteControlBlurLayout.setVisibility(View.VISIBLE);
                    mRemoteControlBinding.remoteControlFindLayout.setVisibility(View.GONE);
                    mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(true);
                }
                else
                {
                    mRemoteControlBinding.remoteControlBlurLayout.setVisibility(View.GONE);
                    mRemoteControlBinding.remoteControlFindLayout.setVisibility(View.VISIBLE);
                    mRemoteControlBinding.remoteControlSearchingAnimator.startRippleAnimation();
                    mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
                }
            }
            else if (integer == StatusViewModel.CONNECTION_STATE_CONNECTING)
            {
                mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(View.VISIBLE);
            }
            else
            {
                mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(View.GONE);
                mRemoteControlBinding.remoteControlSearchingAnimator.stopRippleAnimation();
                mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
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

    // LiveData - Battery
    private void liveDataBattery()
    {
        mStatusViewModel.getLiveDataBatteryLevel().observe(getViewLifecycleOwner(), o ->
        {
            int    value     = mStatusViewModel.getValueBatteryLevel();
            String textValue = "" + value;
            Log.v(TAG, "옵저버 : 배터리 -> " + textValue + "%");
            mRemoteControlBinding.remoteControlBatteryPercentTextview.setText(textValue);
            mRemoteControlBinding.remoteControlBatteryProgressbar.setProgress(value);


            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[3] 시작.
            /*
            {
                Log.d(TAG, "observe --> battery = " + value);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[3] 끝.


        });
    }

    // LiveData - Notification
    private void liveDataNotification()
    {
        mStatusViewModel.getLiveDataNotification().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueNotification();

            Log.v(TAG, "옵저버 : 자극알림 -> " + value);


            if (value == PacketInfo.NOTIFICATION_ON)
            {
                mRemoteControlBinding.remoteControlNotificationImageButton.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.remote_control_ic_ripple_circle_background_on));
            }
            else if (value == PacketInfo.NOTIFICATION_OFF)
            {
                mRemoteControlBinding.remoteControlNotificationImageButton.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.remote_control_ic_ripple_circle_background));
            }


            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[4] 시작.
            /*
            {
                Log.d(TAG, "observe --> stim alarm = " + value);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[4] 끝.


        });
    }

    // LiveData - LED
    private void liveDataLed()
    {
        mStatusViewModel.getLiveDataLed().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueLed();

            Log.v(TAG, "옵저버 : LED -> " + value);

            if (value == PacketInfo.LED_ON)
            {
                mRemoteControlBinding.remoteControlLedImageButton.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.remote_control_ic_ripple_circle_background_on));
            }
            else if (value == PacketInfo.LED_OFF)
            {
                mRemoteControlBinding.remoteControlLedImageButton.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.remote_control_ic_ripple_circle_background));
            }


            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[5] 시작.
            /*
            {
                Log.d(TAG, "observe --> LED alarm = " + value);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[5] 끝.


        });
    }

    // LiveData - Telecoil
    private void liveDataTelecoil()
    {
        mStatusViewModel.getLiveDataTelecoil().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueTelecoil();

            Log.v(TAG, "옵저버 : 텔레코일 -> " + value);

            if (value == PacketInfo.TELECOIL_ON)
            {
                mRemoteControlBinding.remoteControlTelecoilImageButton.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.remote_control_ic_ripple_circle_background_on));
            }
            else if (value == PacketInfo.TELECOIL_OFF)
            {
                mRemoteControlBinding.remoteControlTelecoilImageButton.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.remote_control_ic_ripple_circle_background));
            }


            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[6] 시작.
            /*
            {
                Log.d(TAG, "observe --> telecoil setting = " + value);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[6] 끝.


        });
    }

    // LiveData - MaxOutput
    private void liveDataMaxOutput()
    {
        mStatusViewModel.getLiveDataMaxOutput().observe(getViewLifecycleOwner(), o ->
        {
            int    value   = mStatusViewModel.getValueMaxOutput();
            int    percent = 60 + (10 * value);
            String text    = percent + "%";

            Log.v(TAG, "옵저버 : 최대출력 -> " + text);
            mRemoteControlBinding.remoteControlMaxOutputValueTextview.setText(text);
            mRemoteControlBinding.remoteControlMaxOutputProgressbar.setProgress(value);


            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[6] 시작.
            /*
            {
                Log.d(TAG, "observe --> max output = " + value);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[6] 끝.


        });
    }

    // LiveData - Volume
    private void liveDataVolume()
    {
        mStatusViewModel.getLiveDataVolume().observe(getViewLifecycleOwner(), o ->
        {
            int    value = mStatusViewModel.getValueVolume();
            String text  = value + "";
            Log.v(TAG, "옵저버 : 볼륨 -> " + text);
            mRemoteControlBinding.remoteControlVolumeValueTextview.setText(text);
            mRemoteControlBinding.remoteControlVolumeProgressbar.setProgress(value);


            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[7] 시작.
            /*
            {
                Log.d(TAG, "observe --> volume = " + value);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[7] 끝.


        });
    }

    // LiveData - Program
    private void liveDataProgram()
    {
        mStatusViewModel.getLiveDataProgram().observe(getViewLifecycleOwner(), o ->
        {
            int    value = mStatusViewModel.getValueProgram();
            String text  = value + "";
            Log.v(TAG, "옵저버 : 프로그램 -> " + text);
            mRemoteControlBinding.remoteControlProgramValueTextview.setText(text);


            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[8] 시작.
            /*
            {
                Log.d(TAG, "observe --> program = " + value);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-58 [리모컨 화면 라이브데이터 처리 유닛] 순서[8] 끝.


        });
    }

    private void print_dfuBuffer(int dfuParam, MainActivity mainActivity)
    {
        ArrayList<String> stringList = new ArrayList<>();
        byte[]            buffer     = new byte[Dfu.PRINT_LOG_HEX_LENGTH];
        int               cnt        = 0;

        Dfu.DfuInfo dfuInfo = mDfu.getDfuInfo(dfuParam);

        if (!dfuInfo.readDone)
        {
            return;
        }

        for (int i = 0; i < dfuInfo.length; i++)
        {
            buffer[cnt++] = mDfu.mBuffer[dfuInfo.index + i];

            if (cnt == Dfu.PRINT_LOG_HEX_LENGTH || i == (dfuInfo.length - 1))
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

        Log.d(TAG, "File = " + dfuInfo.name);

        for (int i = 0; i < stringList.size(); i++)
        {
            Log.d(TAG, stringList.get(i));
        }
    }

    View.OnClickListener dfuUpdateOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (mDfu.mThreadState == Dfu.THREAD_STATE_BUSY)
            {
                Log.d(TAG, "이미 쓰레드 동작 중");
                return;
            }

            if (mDfu.mCommState != Dfu.COMM_STATE_IDLE)
            {
                Log.d(TAG, "이미 무선 프로토콜 전송 중, 현재 상태 = " + mDfu.mCommState);
                return;
            }

            mDfu.mThreadState = Dfu.THREAD_STATE_BUSY; // 쓰레드 바쁨 설정

            mDfu.mThreadParam = Dfu.THREAD_PARAM_STATUS;

            Dfu.DfuInfo dfuInfo = mDfu.getDfuInfo(Dfu.THREAD_PARAM_STATUS);

            if (mDfu.mStatus.readDone)
            {
                Log.d(TAG, "이미 읽었음 → " + "file = " + Dfu.FILE_STATUS + ", pos = " + mDfu.mStatus.index + ", len = " + mDfu.mStatus.length);
            }
            else
            {
                int total   = mDfu.mBufferIndex;
                int readLen = 0;

                if (mDfu.mBufferIndex < mDfu.mBuffer.length)
                {
                    mDfu.mBuffer[total] = (byte) 0x01;
                    total++;
                    readLen++;
                }

                mDfu.writeDfuInfo(mDfu.mThreadParam, mDfu.mBufferIndex, readLen, true, total);
            }

            print_dfuBuffer(mDfu.mThreadParam, mMainActivity);

            mDfu.mThreadState = Dfu.THREAD_STATE_IDLE; // 쓰레드 바쁨 해제

            byte[] packet = mDfu.prepare_dfuCommPacket(dfuInfo.fileType);

            if (packet == null)

            {
                mDfu.mCommState = Dfu.COMM_STATE_IDLE;
            }

            mDfu.mCommState = Dfu.COMM_STATE_SEND_COMMAND;

            mMainActivity.mCheckBatteryHandler.removeCallbacks(mMainActivity.mCheckBatteryRunner); // 배터리 체크 패킷 핸들러 제거
            mMainActivity.sendPacket(packet);

            mDfu.mCommState = Dfu.COMM_STATE_WAIT_RESP_COMMAND;
        }
    };

    View.OnClickListener dfuSendOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Dfu.DfuInfo dfuInfo;

            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (v.getId() == mRemoteControlBinding.dfuManifestSendBtn.getId()) // Manifest Send
            {
                dfuInfo = mDfu.getDfuInfo(Dfu.THREAD_PARAM_MANIFEST);
            }
            else if (v.getId() == mRemoteControlBinding.dfuApp0SendBtn.getId()) // App000 Send
            {
                dfuInfo = mDfu.getDfuInfo(Dfu.THREAD_PARAM_APP000);
            }
            else if (v.getId() == mRemoteControlBinding.dfuApp1SendBtn.getId()) // App001 Send
            {
                dfuInfo = mDfu.getDfuInfo(Dfu.THREAD_PARAM_APP001);
            }
            else if (v.getId() == mRemoteControlBinding.dfuApp2SendBtn.getId()) // App002 Send
            {
                dfuInfo = mDfu.getDfuInfo(Dfu.THREAD_PARAM_APP002);
            }
            else
            {
                return;
            }

            if (!dfuInfo.readDone)
            {
                Log.d(TAG, "아직 " + dfuInfo.name + " 파일 읽기가 진행 되지 않음");
                return;
            }

            if (mDfu.mCommState != Dfu.COMM_STATE_IDLE)
            {
                Log.d(TAG, "이미 무선 프로토콜 전송 중, 현재 상태 = " + mDfu.mCommState);
                return;
            }

            byte[] packet = mDfu.prepare_dfuCommPacket(dfuInfo.fileType);

            if (packet == null)
            {
                mDfu.mCommState = Dfu.COMM_STATE_IDLE;
            }

            mDfu.mCommState = Dfu.COMM_STATE_SEND_COMMAND;

            mMainActivity.mCheckBatteryHandler.removeCallbacks(mMainActivity.mCheckBatteryRunner); // 배터리 체크 패킷 핸들러 제거
            mMainActivity.sendPacket(packet);

            mDfu.mCommState = Dfu.COMM_STATE_WAIT_RESP_COMMAND;
        }
    };

    View.OnClickListener dfuReadOnClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (mDfu.mThreadState == Dfu.THREAD_STATE_BUSY)
            {
                Log.d(TAG, "이미 쓰레드 동작 중");
                return;
            }

            mActivity = requireActivity();

            if (v.getId() == mRemoteControlBinding.dfuManifestReadBtn.getId()) // Manifest Read
            {
                if (mDfu.mManifest.readDone)
                {
                    Log.d(TAG, "이미 읽었음 → " + "file = " + Dfu.FILE_MANIFEST + ", pos = " + mDfu.mManifest.index + ", len = " + mDfu.mManifest.length);
                    return;
                }

                mDfu.mThreadParam = Dfu.THREAD_PARAM_MANIFEST;
            }
            else if (v.getId() == mRemoteControlBinding.dfuApp0ReadBtn.getId()) // App000 Read
            {
                if (mDfu.mApp000.readDone)
                {
                    Log.d(TAG, "이미 읽었음 → " + "file = " + Dfu.FILE_APP000 + ", pos = " + mDfu.mApp000.index + ", len = " + mDfu.mApp000.length);
                    return;
                }

                mDfu.mThreadParam = Dfu.THREAD_PARAM_APP000;
            }
            else if (v.getId() == mRemoteControlBinding.dfuApp1ReadBtn.getId()) // App001 Read
            {
                if (mDfu.mApp001.readDone)
                {
                    Log.d(TAG, "이미 읽었음 → " + "file = " + Dfu.FILE_APP001 + ", pos = " + mDfu.mApp001.index + ", len = " + mDfu.mApp001.length);
                    return;
                }

                mDfu.mThreadParam = Dfu.THREAD_PARAM_APP001;
            }
            else if (v.getId() == mRemoteControlBinding.dfuApp2ReadBtn.getId()) // App002 Read
            {
                if (mDfu.mApp002.readDone)
                {
                    Log.d(TAG, "이미 읽었음 → " + "file = " + Dfu.FILE_APP002 + ", pos = " + mDfu.mApp002.index + ", len = " + mDfu.mApp002.length);
                    return;
                }

                mDfu.mThreadParam = Dfu.THREAD_PARAM_APP002;
            }

            mDfu.mThreadState = Dfu.THREAD_STATE_BUSY;

            // 파일 읽기 및 데이터를 저장하는 시간이 오래 걸리므로 쓰레드에서 작업을 진행한다.
            // 메인 쓰레드에서는 100msec 정도 작업이 지연되면 ANR 에러가 발생할 수 있다.
            new Thread(() ->
            {
                String   path = Environment.getExternalStorageDirectory().getAbsolutePath();
                TextView stateTv;

                switch (mDfu.mThreadParam)
                {
                    case Dfu.THREAD_PARAM_MANIFEST:
                        path = path + Dfu.FILE_MANIFEST;
                        stateTv = mRemoteControlBinding.dfuManifestStateTv;
                        break;
                    case Dfu.THREAD_PARAM_APP000:
                        path = path + Dfu.FILE_APP000;
                        stateTv = mRemoteControlBinding.dfuApp0StateTv;
                        break;
                    case Dfu.THREAD_PARAM_APP001:
                        path = path + Dfu.FILE_APP001;
                        stateTv = mRemoteControlBinding.dfuApp1StateTv;
                        break;
                    case Dfu.THREAD_PARAM_APP002:
                        path = path + Dfu.FILE_APP002;
                        stateTv = mRemoteControlBinding.dfuApp2StateTv;
                        break;
                    default:
                        return;
                }

                File file = new File(path);

                if (file.exists())
                {
                    // UI 쓰레드에서 View를 설정
                    mActivity.runOnUiThread(() ->
                    {
                        stateTv.setText("있음");
                    });

                    try (FileInputStream fis = new FileInputStream(file))
                    {
                        MainActivity mainActivity = (MainActivity) mActivity;

                        int total   = mDfu.mBufferIndex;
                        int readLen = 0;
                        int b;

                        while ((b = fis.read()) != -1 && total < mDfu.mBuffer.length)
                        {
                            mDfu.mBuffer[total] = (byte) (b & 0xFF);

                            total++;
                            readLen++;
                        }

                        fis.close();

                        mDfu.writeDfuInfo(mDfu.mThreadParam, mDfu.mBufferIndex, readLen, true, total);

                        print_dfuBuffer(mDfu.mThreadParam, mainActivity);

                        // UI 쓰레드에서 View를 설정
                        mActivity.runOnUiThread(() ->
                        {
                            stateTv.setText("준비");
                        });
                    }
                    catch (IOException e)
                    {
                        // UI 쓰레드에서 View를 설정
                        mActivity.runOnUiThread(() ->
                        {
                            stateTv.setText("에러");
                        });

                        e.printStackTrace();
                    }
                }
                else
                {
                    stateTv.setText("없음");
                }

                mDfu.mThreadParam = Dfu.THREAD_PARAM_NONE;
                mDfu.mThreadState = Dfu.THREAD_STATE_IDLE;
            }).start();

        } // onClick;
    }; // listener;

    // DFU button click - Manifest
    private void dfuClickReadManifest()
    {
        mRemoteControlBinding.dfuManifestReadBtn.setOnClickListener(dfuReadOnClickListener);
    }

    private void dfuClickReadApp000()
    {
        mRemoteControlBinding.dfuApp0ReadBtn.setOnClickListener(dfuReadOnClickListener);
    }

    private void dfuClickReadApp001()
    {
        mRemoteControlBinding.dfuApp1ReadBtn.setOnClickListener(dfuReadOnClickListener);
    }

    private void dfuClickReadApp002()
    {
        mRemoteControlBinding.dfuApp2ReadBtn.setOnClickListener(dfuReadOnClickListener);
    }

    private void dfuClickSendManifest()
    {
        mRemoteControlBinding.dfuManifestSendBtn.setOnClickListener(dfuSendOnClickListener);
    }

    private void dfuClickSendApp000()
    {
        mRemoteControlBinding.dfuApp0SendBtn.setOnClickListener(dfuSendOnClickListener);
    }

    private void dfuClickSendApp001()
    {
        mRemoteControlBinding.dfuApp1SendBtn.setOnClickListener(dfuSendOnClickListener);
    }

    private void dfuClickSendApp002()
    {
        mRemoteControlBinding.dfuApp2SendBtn.setOnClickListener(dfuSendOnClickListener);
    }

    private void dfuClickUpdateBtn()
    {
        mRemoteControlBinding.dfuFinalUpdateBtn.setOnClickListener(dfuUpdateOnClickListener);
    }

    // Button click - Notification
    private void clickNotification()
    {
        mRemoteControlBinding.remoteControlNotificationImageButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            int value = mStatusViewModel.getValueNotification();


            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[2] 시작.
            /*
            {
                if (value == PacketInfo.NOTIFICATION_ON)
                {
                    Log.d(TAG, "Click event --> stim alarm = " + PacketInfo.NOTIFICATION_OFF);
                }
                else if (value == PacketInfo.NOTIFICATION_OFF)
                {
                    Log.d(TAG, "Click event --> stim alarm = " + PacketInfo.NOTIFICATION_ON);
                }
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[2] 끝.


            if (value == PacketInfo.NOTIFICATION_ON)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_NOTIFICATION, new byte[]{PacketInfo.NOTIFICATION_OFF}, PacketInfo.PACKET_SIZE_NOTIFICATION));
            }
            else if (value == PacketInfo.NOTIFICATION_OFF)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_NOTIFICATION, new byte[]{PacketInfo.NOTIFICATION_ON}, PacketInfo.PACKET_SIZE_NOTIFICATION));
            }
        });
    }

    // Button click - Led
    private void clickLed()
    {
        mRemoteControlBinding.remoteControlLedImageButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            int value = mStatusViewModel.getValueLed();


            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[3] 시작.
            /*
            {
                if (value == PacketInfo.LED_ON)
                {
                    Log.d(TAG, "Click event --> led alarm = " + PacketInfo.LED_OFF);
                }
                else if (value == PacketInfo.LED_OFF)
                {
                    Log.d(TAG, "Click event --> led alarm = " + PacketInfo.LED_ON);
                }
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[3] 끝.


            if (value == PacketInfo.LED_ON)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_LED, new byte[]{PacketInfo.LED_OFF}, 2));
            }
            else if (value == PacketInfo.LED_OFF)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_LED, new byte[]{PacketInfo.LED_ON}, 2));
            }
        });
    }

    // Button click - Telecoil
    private void clickTelecoil()
    {
        mRemoteControlBinding.remoteControlTelecoilImageButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            int value = mStatusViewModel.getValueTelecoil();

            if (value == PacketInfo.TELECOIL_ON)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_TELECOIL, new byte[]{PacketInfo.TELECOIL_OFF}, 2));
            }
            else //if (value == PacketInfo.TELECOIL_OFF)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_TELECOIL, new byte[]{PacketInfo.TELECOIL_ON}, 2));
            }
        });
    }

    // Button click - MaxOutput
    private void clickMaxOutput()
    {
        mRemoteControlBinding.remoteControlMaxOutputUpImageButton.setOnClickListener(view ->
        {


            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[6] 시작.
            /*
            {
                Log.d(TAG, "Click event --> max output = " + PacketInfo.MAX_OUTPUT_UP);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[6] 끝.


            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_MAX_OUTPUT, new byte[]{PacketInfo.MAX_OUTPUT_UP}, 2));
        });

        mRemoteControlBinding.remoteControlMaxOutputDownImageButton.setOnClickListener(view ->
        {


            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[7] 시작.
            /*
            {
                Log.d(TAG, "Click event --> max output = " + PacketInfo.MAX_OUTPUT_DOWN);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[7] 끝.


            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_MAX_OUTPUT, new byte[]{PacketInfo.MAX_OUTPUT_DOWN}, 2));
        });
    }

    // Button click - Volume
    private void clickVolume()
    {
        mRemoteControlBinding.remoteControlVolumeUpImageButton.setOnClickListener(view ->
        {


            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[8] 시작.
            /*
            {
                Log.d(TAG, "Click event --> volume = " + PacketInfo.VOLUME_UP);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[8] 끝.


            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_VOLUME, new byte[]{PacketInfo.VOLUME_UP}, 2));
        });

        mRemoteControlBinding.remoteControlVolumeDownImageButton.setOnClickListener(view ->
        {


            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[9] 시작.
            /*
            {
                Log.d(TAG, "Click event --> volume = " + PacketInfo.VOLUME_DOWN);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[9] 끝.


            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_VOLUME, new byte[]{PacketInfo.VOLUME_DOWN}, 2));
        });
    }

    // Button click - Program
    private void clickProgram()
    {
        mRemoteControlBinding.remoteControlProgramUpImageButton.setOnClickListener(view ->
        {


            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[4] 시작.
            /*
            {
                Log.d(TAG, "Click event --> program = " + PacketInfo.PROGRAM_UP);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[4] 끝.


            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_PROMGRAM, new byte[]{PacketInfo.PROGRAM_UP}, 2));
        });

        mRemoteControlBinding.remoteControlProgramDownImageButton.setOnClickListener(view ->
        {


            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[5] 시작.
            /*
            {
                Log.d(TAG, "Click event --> program = " + PacketInfo.PROGRAM_DOWN);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-57 [리모컨 화면 뷰 이벤트 처리 유닛] 순서[5] 끝.


            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_PROMGRAM, new byte[]{PacketInfo.PROGRAM_DOWN}, 2));
        });
    }

    public void checkRegisteredList()
    {
        if (mMainBinding.lockScreen.getVisibility() != View.VISIBLE)
        {


            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 공통 사용 항목1 시작.
            /*
            List<EntityUser> users = UtilUser.instance.getUsers();
            List<EntityDevice> devices = UtilDevice.instance.getDevices();
            */
            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 공통 사용 항목1 끝.


            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 순서[1] 시작.
            /*
            {
                // 공통 사용 항목 1과 2를 활용한다.
                for (EntityUser user : UtilUser.instance.getUsers())
                {
                    UtilUser.instance.delete(user);
                }

                for (EntityDevice device : UtilDevice.instance.getDevices())
                {
                    UtilDevice.instance.delete(device);
                }
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 순서[1] 끝.


            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 순서[2] 시작.
            /*
            {
                // 공통 사용 항목 1과 2를 활용한다.
                for (EntityUser user : UtilUser.instance.getUsers())
                {
                    UtilUser.instance.delete(user);
                }

                for (EntityDevice device : UtilDevice.instance.getDevices())
                {
                    UtilDevice.instance.delete(device);
                }

                EntityUser user = new EntityUser();
                user.name = "AAAAA_R";
                user.nickname = "사용자 A";
                user.ear = EntityUser.EAR_RIGHT;
                user.passKey = "0481";
                user.defaultUser = EntityUser.USER_NOT_DEFAULT;

                UtilUser.instance.insert(user);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 순서[2] 끝.


            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 순서[3] 시작.
            /*
            {
                // 공통 사용 항목 1과 2를 활용한다.
                for (EntityUser user : UtilUser.instance.getUsers())
                {
                    UtilUser.instance.delete(user);
                }

                for (EntityDevice device : UtilDevice.instance.getDevices())
                {
                    UtilDevice.instance.delete(device);
                }

                EntityUser user = new EntityUser();
                user.name = "AAAAA_R";
                user.nickname = "사용자 A";
                user.ear = EntityUser.EAR_RIGHT;
                user.passKey = "0481";
                user.defaultUser = EntityUser.USER_NOT_DEFAULT;

                UtilUser.instance.insert(user);

                EntityDevice device = new EntityDevice();
                device.serialNumber = "A1B2";
                device.additionalInformation = "외부기 1";
                device.pairingKey = "123456";

                UtilDevice.instance.insert(device);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 순서[3] 끝.


            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 순서[4] 시작.
            /*
            {
                // 공통 사용 항목 1과 2를 활용한다.
                for (EntityUser user : UtilUser.instance.getUsers())
                {
                    UtilUser.instance.delete(user);
                }

                for (EntityDevice device : UtilDevice.instance.getDevices())
                {
                    UtilDevice.instance.delete(device);
                }

                EntityUser user = new EntityUser();
                user.name = "AAAAA_R";
                user.nickname = "사용자 A";
                user.ear = EntityUser.EAR_RIGHT;
                user.passKey = "0481";
                user.defaultUser = EntityUser.USER_DEFAULT;

                UtilUser.instance.insert(user);

                EntityDevice device = new EntityDevice();
                device.serialNumber = "A1B2";
                device.additionalInformation = "외부기 1";
                device.pairingKey = "123456";

                UtilDevice.instance.insert(device);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 순서[4] 끝.


            if (UtilUser.instance.getUsers().size() == 0)
            {
                makeNoUserDialog();
            }
            else if (UtilDevice.instance.getDevices().size() == 0)
            {
                makeNoDeviceDialog();
            }
            else
            {
                EntityUser defaultUser = UtilUser.instance.getDefaultUser();
                if (defaultUser != null)
                {
                    if (defaultUser.nickname != null && defaultUser.nickname.length() > 0)
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
                        else if (defaultUser.ear.equals(EntityUser.EAR_RIGHT))
                        {
                            name = name + " (오른쪽)";
                        }

                        mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(name);
                    }

                    if (mMainBinding.lockScreen.getVisibility() == View.GONE)
                    {
                        if (Status.instance().connectionState == Status.CONNECTION_STATE_DISCONNECTED)
                        {
                            Log.d(TAG, "연결 해제 상태이므로 검색을 시작합니다.");
                            //((MainActivity) requireActivity()).scanLe(true);
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

            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 공통 사용 항목2 시작.
            /*
            for (EntityUser user : UtilUser.instance.getUsers())
            {
                UtilUser.instance.delete(user);
            }

            for (EntityUser user : users)
            {
                UtilUser.instance.insert(user);
            }

            for (EntityDevice device : UtilDevice.instance.getDevices())
            {
                UtilDevice.instance.delete(device);
            }

            for (EntityDevice device : devices)
            {
                UtilDevice.instance.insert(device);
            }
            */
            // TD2-SW-RC-UNIT-Test-ID-59 [리모컨 화면 사용자/기기 목록 체크 유닛] 공통 사용 항목2 끝.


        }
    }

    // NO 사용자 다이얼로그
    private void makeNoUserDialog()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.remote_control_no_user_dialog_title)).setMessage(getString(R.string.remote_control_no_user_dialog_message)).setPositiveButton(getString(R.string.remote_control_no_user_positive), (dialogInterface, i) ->
            {
                // 장시간 미사용 핸들러 업데이트
                ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                mDialog.dismiss();
                mDialog = null;
                        /*
                        requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddUserFragment()).commitAllowingStateLoss();
                        */
                ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.USER_ADD);
            }).setCancelable(false).create();

            mDialog.show();
        }
    }

    // NO 사운드처리기 다이얼로그
    private void makeNoDeviceDialog()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.remote_control_no_device_dialog_title)).setMessage(getString(R.string.remote_control_no_device_dialog_message)).setPositiveButton(getString(R.string.remote_control_no_device_positive), (dialogInterface, i) ->
            {
                // 장시간 미사용 핸들러 업데이트
                ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                mDialog.dismiss();
                mDialog = null;
                        /*
                        requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddDeviceFragment()).commitAllowingStateLoss();
                        */
                ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.DEVICE_ADD);
            }).setCancelable(false).create();

            mDialog.show();
        }
    }
}