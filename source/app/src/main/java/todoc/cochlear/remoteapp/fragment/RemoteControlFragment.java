package todoc.cochlear.remoteapp.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentRemoteControlBinding;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.params.PacketInfo;
import todoc.cochlear.remoteapp.params.Status;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;

public class RemoteControlFragment extends Fragment
{
    static final private String TAG = "TODOC_" + RemoteControlFragment.class.getSimpleName();

    public ActivityMainBinding mMainBinding;
    public FragmentRemoteControlBinding mRemoteControlBinding;

    StatusViewModel mStatusViewModel;

    public AlertDialog mDialog;

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

        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainBinding = ((MainActivity) requireActivity()).mBinding;
        mMainBinding.toolbar.setNavigationIcon(null);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(true);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(true);
        //mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Default_Headline6);
        mMainBinding.toolbar.setTitle("리모컨");
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
        clickTelecoil();
        clickMaxOutput();
        clickVolume();
        clickProgram();

        // User list
        checkRegisteredList();
        //initChipGroup();

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
            }
            else if (integer == StatusViewModel.CONNECTION_STATE_CONNECTING)
            {
                mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(View.VISIBLE);
            }
            else
            {
                mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(View.GONE);
            }
        });
    }

    // LiveData - Battery
    private void liveDataBattery()
    {
        mStatusViewModel.getLiveDataBatteryLevel().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueBatteryLevel();
            String textValue = "" + value;
            Log.v(TAG, "옵저버 : 배터리 -> " + textValue + "%");
            mRemoteControlBinding.remoteControlBatteryPercentTextview.setText(textValue);
            mRemoteControlBinding.remoteControlBatteryProgressbar.setProgress(value);
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
        });
    }

    // LiveData - MaxOutput
    private void liveDataMaxOutput()
    {
        mStatusViewModel.getLiveDataMaxOutput().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueMaxOutput();
            int percent = 60 + (10 * value);
            String text = percent + "%";

            Log.v(TAG, "옵저버 : 최대출력 -> " + text);
            mRemoteControlBinding.remoteControlMaxOutputValueTextview.setText(text);
            mRemoteControlBinding.remoteControlMaxOutputProgressbar.setProgress(value);
        });
    }

    // LiveData - Volume
    private void liveDataVolume()
    {
        mStatusViewModel.getLiveDataVolume().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueVolume();
            String text = value + "";
            Log.v(TAG, "옵저버 : 볼륨 -> " + text);
            mRemoteControlBinding.remoteControlVolumeValueTextview.setText(text);
            mRemoteControlBinding.remoteControlVolumeProgressbar.setProgress(value);
        });
    }

    // LiveData - Program
    private void liveDataProgram()
    {
        mStatusViewModel.getLiveDataProgram().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueProgram();
            String text = value + "";
            Log.v(TAG, "옵저버 : 프로그램 -> " + text);
            mRemoteControlBinding.remoteControlProgramValueTextview.setText(text);
        });
    }

    // Button click - Notification
    private void clickNotification()
    {
        mRemoteControlBinding.remoteControlNotificationImageButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            int value = mStatusViewModel.getValueNotification();

            if (value == PacketInfo.NOTIFICATION_ON)
            {
                ((MainActivity) requireActivity()).sendPacket(
                        ((MainActivity) requireActivity()).packetMaker(
                                PacketInfo.HEADER_VALUE_NOTIFICATION, new byte[]{PacketInfo.NOTIFICATION_OFF}, PacketInfo.PACKET_SIZE_NOTIFICATION));
            }
            else if (value == PacketInfo.NOTIFICATION_OFF)
            {
                ((MainActivity) requireActivity()).sendPacket(
                        ((MainActivity) requireActivity()).packetMaker(
                                PacketInfo.HEADER_VALUE_NOTIFICATION, new byte[]{PacketInfo.NOTIFICATION_ON}, PacketInfo.PACKET_SIZE_NOTIFICATION));
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
            else if (value == PacketInfo.TELECOIL_OFF)
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
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_MAX_OUTPUT, new byte[]{PacketInfo.MAX_OUTPUT_UP}, 2));
        });

        mRemoteControlBinding.remoteControlMaxOutputDownImageButton.setOnClickListener(view ->
        {
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
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_VOLUME, new byte[]{PacketInfo.VOLUME_UP}, 2));
        });

        mRemoteControlBinding.remoteControlVolumeDownImageButton.setOnClickListener(view ->
        {
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
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_PROMGRAM, new byte[]{PacketInfo.PROGRAM_UP}, 2));
        });

        mRemoteControlBinding.remoteControlProgramDownImageButton.setOnClickListener(view ->
        {
            // 장시간 미사용 핸들러 업데이트
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_VALUE_PROMGRAM, new byte[]{PacketInfo.PROGRAM_DOWN}, 2));
        });
    }

    public void checkRegisteredList()
    {
        if (mMainBinding.lockScreen.getVisibility() != View.VISIBLE)
        {
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
                        mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(defaultUser.name);
                    }

                    if (mMainBinding.lockScreen.getVisibility() == View.GONE)
                    {
                        if (Status.instance().connectionState == Status.CONNECTION_STATE_DISCONNECTED)
                        {
                            Log.d(TAG, "연결 해제 상태이므로 검색을 시작합니다.");
                            ((MainActivity) requireActivity()).scanLe(true);
                        }
                        else
                        {
                            Log.d(TAG, "연결 해제 상태가 아닙니다.");
                        }
                    }
                }
                else
                {
                    mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText("선택된 사용자 없음");
                    ((MainActivity) requireActivity()).makeDialogSelectUser();
                }
            }
        }
    }

    private void makeNoUserDialog()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("안내")
                    .setMessage("리모컨 앱을 사용하려면 등록된 사용자 정보가 필요합니다. 아래의 등록 버튼을 눌러 사용자 정보를 등록해주세요.")
                    .setPositiveButton("등록", (dialogInterface, i) ->
                    {
                        // 장시간 미사용 핸들러 업데이트
                        ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                        mDialog.dismiss();
                        mDialog = null;
                        requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddUserFragment()).commitAllowingStateLoss();
                    })
                    .setCancelable(false)
                    .create();

            mDialog.show();
        }
    }

    private void makeNoDeviceDialog()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("안내")
                    .setMessage("리모컨 앱을 사용하려면 등록된 사운드처리기 정보가 필요합니다. 아래의 등록 버튼을 눌러 사운드처리기 정보를 등록해주세요.")
                    .setPositiveButton("등록", (dialogInterface, i) ->
                    {
                        // 장시간 미사용 핸들러 업데이트
                        ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

                        mDialog.dismiss();
                        mDialog = null;
                        requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddDeviceFragment()).commitAllowingStateLoss();
                    })
                    .setCancelable(false)
                    .create();

            mDialog.show();
        }
    }
}