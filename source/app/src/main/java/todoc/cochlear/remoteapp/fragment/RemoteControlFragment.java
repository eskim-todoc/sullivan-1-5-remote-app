package todoc.cochlear.remoteapp.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentRemoteControlBinding;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.view_model.BleViewModel;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;

public class RemoteControlFragment extends Fragment
{
    static final private String TAG = "TODOC_" + RemoteControlFragment.class.getSimpleName();

    ActivityMainBinding mMainBinding;
    FragmentRemoteControlBinding mRemoteControlBinding;

    StatusViewModel mStatusViewModel;
    BleViewModel mBleViewModel;

    public AlertDialog mDialog;
    Chip[] chips;

    public RemoteControlFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        //((MainActivity) requireActivity()).scanLe(false);
        mBleViewModel.setSearching(BleViewModel.SEARCHING_DISABLED);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainBinding = ((MainActivity) requireActivity()).mBinding;
        mMainBinding.toolbar.setNavigationIcon(null);
        mMainBinding.toolbar.getMenu().findItem(R.id.settings).setVisible(true);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(true);
        mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Controller_Headline6);
        //mMainBinding.toolbar.setTitle("David");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mRemoteControlBinding = FragmentRemoteControlBinding.inflate(inflater, container, false);
        View view = mRemoteControlBinding.getRoot();

        //((MainActivity) requireActivity()).scanLe(true);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated is called!");

        mStatusViewModel = new ViewModelProvider(requireActivity()).get(StatusViewModel.class);
        mBleViewModel = new ViewModelProvider(requireActivity()).get(BleViewModel.class);

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
    }

    // LiveData - Connection
    private void liveDataConnection()
    {
        mBleViewModel.getObjectBleConnection().observe(getViewLifecycleOwner(), integer ->
        {
            Log.d(TAG, "Obsever : Connection -> " + integer);

            if (integer == BleViewModel.BLE_DISCONNECTED)
            {
                mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(View.VISIBLE);
            }
            else if (integer == BleViewModel.BLE_CONNECTING)
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
            String text = value + "%";

            Log.d(TAG, "Observer : Battery -> " + text);
            mRemoteControlBinding.remoteControlBatteryPercentTextview.setText(text);
        });
    }

    // LiveData - Notification
    private void liveDataNotification()
    {
        mStatusViewModel.getLiveDataNotification().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueNotification();

            Log.d(TAG, "Observer : Notification -> " + value);

            if (value == StatusViewModel.NOTIFICATION_ON)
            {
                mRemoteControlBinding.remoteControlNotificationImageButton.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.remote_control_ic_ripple_circle_background_on));
            }
            else if (value == StatusViewModel.NOTIFICATION_OFF)
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

            Log.d(TAG, "Observer : LED -> " + value);

            if (value == StatusViewModel.LED_ON)
            {
                mRemoteControlBinding.remoteControlLedImageButton.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.remote_control_ic_ripple_circle_background_on));
            }
            else if (value == StatusViewModel.LED_OFF)
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

            Log.d(TAG, "Observer : Telecoil -> " + value);

            if (value == StatusViewModel.TELECOIL_ON)
            {
                mRemoteControlBinding.remoteControlTelecoilImageButton.setBackground(AppCompatResources.getDrawable(requireContext(), R.drawable.remote_control_ic_ripple_circle_background_on));
            }
            else if (value == StatusViewModel.TELECOIL_OFF)
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

            Log.d(TAG, "Observer : Max output -> " + text);
            mRemoteControlBinding.remoteControlMaxOutputValueTextview.setText(text);
        });
    }

    // LiveData - Volume
    private void liveDataVolume()
    {
        mStatusViewModel.getLiveDataVolume().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueVolume();
            String text = value + "";
            Log.d(TAG, "Observer : Volume -> " + text);
            mRemoteControlBinding.remoteControlVolumeValueTextview.setText(text);
        });
    }

    // LiveData - Program
    private void liveDataProgram()
    {
        mStatusViewModel.getLiveDataProgram().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueProgram();
            String text = value + "";
            Log.d(TAG, "Observer : Program -> " + text);
            mRemoteControlBinding.remoteControlProgramValueTextview.setText(text);
        });
    }

    // Button click - Notification
    private void clickNotification()
    {
        mRemoteControlBinding.remoteControlNotificationImageButton.setOnClickListener(view ->
        {
            int value = mStatusViewModel.getValueNotification();

            if (value == StatusViewModel.NOTIFICATION_ON)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_SIMULATION, new byte[]{StatusViewModel.NOTIFICATION_OFF}, 2));
            }
            else if (value == StatusViewModel.NOTIFICATION_OFF)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_SIMULATION, new byte[]{StatusViewModel.NOTIFICATION_ON}, 2));
            }
        });
    }

    // Button click - Led
    private void clickLed()
    {
        mRemoteControlBinding.remoteControlLedImageButton.setOnClickListener(view ->
        {
            int value = mStatusViewModel.getValueLed();

            if (value == StatusViewModel.LED_ON)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_LED, new byte[]{StatusViewModel.LED_OFF}, 2));
            }
            else if (value == StatusViewModel.LED_OFF)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_LED, new byte[]{StatusViewModel.LED_ON}, 2));
            }
        });
    }

    // Button click - Telecoil
    private void clickTelecoil()
    {
        mRemoteControlBinding.remoteControlTelecoilImageButton.setOnClickListener(view ->
        {
            int value = mStatusViewModel.getValueTelecoil();

            if (value == StatusViewModel.TELECOIL_ON)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_TELECOIL, new byte[]{StatusViewModel.TELECOIL_OFF}, 2));
            }
            else if (value == StatusViewModel.TELECOIL_OFF)
            {
                ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_TELECOIL, new byte[]{StatusViewModel.TELECOIL_ON}, 2));
            }
        });
    }

    // Button click - MaxOutput
    private void clickMaxOutput()
    {
        mRemoteControlBinding.remoteControlMaxOutputUpImageButton.setOnClickListener(view ->
        {
            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_SENSITIVITY, new byte[]{StatusViewModel.MAX_OUTPUT_UP}, 2));
        });

        mRemoteControlBinding.remoteControlMaxOutputDownImageButton.setOnClickListener(view ->
        {
            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_SENSITIVITY, new byte[]{StatusViewModel.MAX_OUTPUT_DOWN}, 2));
        });
    }

    // Button click - Volume
    private void clickVolume()
    {
        mRemoteControlBinding.remoteControlVolumeUpImageButton.setOnClickListener(view ->
        {
            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_VOLUME, new byte[]{StatusViewModel.VOLUME_UP}, 2));
        });

        mRemoteControlBinding.remoteControlVolumeDownImageButton.setOnClickListener(view ->
        {
            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_VOLUME, new byte[]{StatusViewModel.VOLUME_DOWN}, 2));
        });
    }

    // Button click - Program
    private void clickProgram()
    {
        mRemoteControlBinding.remoteControlProgramUpImageButton.setOnClickListener(view ->
        {
            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_PROMGRAM, new byte[]{StatusViewModel.PROGRAM_UP}, 2));
        });

        mRemoteControlBinding.remoteControlProgramDownImageButton.setOnClickListener(view ->
        {
            ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(MainActivity.PACKET_HEADER_VALUE_PROMGRAM, new byte[]{StatusViewModel.PROGRAM_DOWN}, 2));
        });
    }

    public void initChipGroup()
    {
        List<EntityUser> users = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().findAll();
        chips = new Chip[users.size()];

        for (int i = 0; i < users.size(); i++)
        {
            chips[i] = new Chip(requireContext());
            chips[i].setId(View.generateViewId());
            chips[i].setText(users.get(i).name);
            chips[i].setChipBackgroundColor(requireContext().getColorStateList(R.color.remote_control_chip_background_selector));
            chips[i].setTextColor(requireContext().getColor(R.color.remote_control_chip_text_selector));
            chips[i].setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getContext().getResources().getDisplayMetrics()));
            chips[i].setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            chips[i].setCheckable(true);
            chips[i].setChipIcon(null);
            chips[i].setCheckedIcon(null);
            chips[i].setChecked(users.get(i).defaultUser.equals(EntityUser.USER_DEFAULT));
            mRemoteControlBinding.remoteControlChipGroup.addView(chips[i]);
        }

        mRemoteControlBinding.remoteControlChipGroup.setOnCheckedStateChangeListener(mUsersCheckedChangedListener);
    }

    private ChipGroup.OnCheckedStateChangeListener mUsersCheckedChangedListener = new ChipGroup.OnCheckedStateChangeListener()
    {
        @Override
        public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds)
        {
            String name = null;
            int size = checkedIds.size();

            if (size == 0)
            {
                EntityUser defaultUser = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);
                if (defaultUser != null)
                {
                    defaultUser.defaultUser = EntityUser.USER_NOT_DEFAULT;
                    ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().update(defaultUser);
                }
            }
            else
            {
                for (int k = 0; k < chips.length; k++)
                {
                    if (checkedIds.get(0) == chips[k].getId())
                    {
                        name = chips[k].getText().toString();
                        break;
                    }
                }

                if (name != null)
                {
                    EntityUser defaultUser = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);
                    if (defaultUser != null)
                    {
                        defaultUser.defaultUser = EntityUser.USER_NOT_DEFAULT;
                        ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().update(defaultUser);
                    }

                    EntityUser user = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByName(name);
                    if (user != null)
                    {
                        user.defaultUser = EntityUser.USER_DEFAULT;
                        ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().update(user);
                    }
                }
            }

            EntityUser logUser = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);
            if (logUser != null)
            {
                Log.d(TAG, "Now, default user -> " + logUser.name);

                mBleViewModel.setSearching(BleViewModel.SEARCHING_ENABLED);
            }
            else
            {
                Log.d(TAG, "Now, no default user selected.");

                mBleViewModel.setSearching(BleViewModel.SEARCHING_DISABLED);
            }

            if (mBleViewModel.getBleConnection() != BleViewModel.BLE_DISCONNECTED)
            {
                ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
            }
        }
    };

    public void checkRegisteredList()
    {
        if (mMainBinding.lockScreen.getVisibility() != View.VISIBLE)
        {
            if (((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().findAll().size() == 0)
            {
                makeNoUserDialog();
            }
            else if (((MainActivity) requireActivity()).mDatabaseDevices.daoDevices().findAll().size() == 0)
            {
                makeNoDeviceDialog();
            }
            else
            {
                EntityUser defaultUser = ((MainActivity) requireActivity()).mDatabaseUsers.daoUsers().getDbUserByDefaultUSer(EntityUser.USER_DEFAULT);
                if (defaultUser != null)
                {
                    mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Controller_Headline6);
                    mMainBinding.toolbar.setTitle(defaultUser.name);

                    mBleViewModel.setSearching(BleViewModel.SEARCHING_ENABLED);
                }
                else
                {
                    mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Default_Headline6);
                    mMainBinding.toolbar.setTitle("선택된 사용자 없음");
                }
            }
        }
    }

    private void makeNoUserDialog()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext(), R.style.add_user_screen_dialog)
                    .setTitle("안내")
                    .setMessage("리모컨 앱을 사용하려면 등록된 사용자 정보가 필요합니다. 아래의 등록 버튼을 눌러 사용자 정보를 등록해주세요.")
                    .setPositiveButton("등록", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            mDialog.dismiss();
                            mDialog = null;
                            requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddUserFragment()).commitAllowingStateLoss();
                        }
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
            mDialog = new MaterialAlertDialogBuilder(requireContext(), R.style.add_user_screen_dialog)
                    .setTitle("안내")
                    .setMessage("리모컨 앱을 사용하려면 등록된 사운드처리기 정보가 필요합니다. 아래의 등록 버튼을 눌러 사운드처리기 정보를 등록해주세요.")
                    .setPositiveButton("등록", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            mDialog.dismiss();
                            mDialog = null;
                            requireActivity().getSupportFragmentManager().beginTransaction().replace(mMainBinding.frame.getId(), new AddDeviceFragment()).commitAllowingStateLoss();
                        }
                    })
                    .setCancelable(false)
                    .create();

            mDialog.show();
        }
    }
}