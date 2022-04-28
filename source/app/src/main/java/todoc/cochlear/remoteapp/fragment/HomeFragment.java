package todoc.cochlear.remoteapp.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.Device;
import todoc.cochlear.remoteapp.params.AppParam;
import todoc.cochlear.remoteapp.params.DeviceParam;
import todoc.cochlear.remoteapp.params.ActionMessage;


public class HomeFragment extends Fragment
{
    private static final String TAG = "TD2_" + HomeFragment.class.getSimpleName();

    MainActivity mMainActivity;

    // device info
    ImageView mModelImageView;
    TextView mDeviceNameTextView;

    // alarm stimulation
    ImageButton mStimulationAlarmImageButton;

    // alarm led
    ImageButton mLedAlarmImageButton;

    // telecoil
    ImageButton mTelecoilImageButton;

    // power mode
    ImageButton mPowerModeImageButton;

    // program
    TextView mProgramTextView;
    ImageButton mProgramPlusImageButton;
    ImageButton mProgramMinusImageButton;

    // sensitivity
    TextView mSensitivityTextView;
    ProgressBar mSensitivityProgressBar;
    ImageButton mSensitivityPlusImageButton;
    ImageButton mSensitivityMinusImageButton;

    // volume
    TextView mVolumeTextView;
    ProgressBar mVolumeProgressBar;
    ImageButton mVolumePlusImageButton;
    ImageButton mVolumeMinusImageButton;

    // not yet connected
    ConstraintLayout mNotConnectedScreenConstraintLayout;

    // battery
    ProgressBar mBatteryProgressBar;
    TextView mBatteryTextView;

    public HomeFragment()
    {
        // Required empty public constructor
    }

    public void setToolbarMenu()
    {
        AppParam.getInstance().setMenuTitle(getString(R.string.toolbar_title_home));
        AppParam.getInstance().setMenuHome(false);
        AppParam.getInstance().setMenuSearch(true);
        AppParam.getInstance().setMenuList(true);
        AppParam.getInstance().setMenuManual(false);
        AppParam.getInstance().setMenuSupport(false);
        AppParam.getInstance().setMenuAutoConnection(true);
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_home, container, false);

        // Update toolbar menu
        setToolbarMenu();
        mMainActivity.updateToolbar();

        // Set fragment number
        AppParam.getInstance().setCurrentFragmentNumber(AppParam.FRAGMENT_NUMBER_HOME);

        // Find views...

        // device info
        mModelImageView = rootView.findViewById(R.id.home_sound_processor_device_image);
        mDeviceNameTextView = rootView.findViewById(R.id.home_sound_processor_device_name_text);

        // battery
        mBatteryProgressBar = rootView.findViewById(R.id.home_sound_processor_battery_progressbar);
        mBatteryTextView = rootView.findViewById(R.id.home_sound_processor_battery_value_textview);

        // alarm stimulation
        mStimulationAlarmImageButton = rootView.findViewById(R.id.home_stim_alarm_image_button);
        mStimulationAlarmImageButton.setOnClickListener(onClickListener);

        // alarm led
        mLedAlarmImageButton = rootView.findViewById(R.id.home_led_alarm_image_button);
        mLedAlarmImageButton.setOnClickListener(onClickListener);

        // telecoil
        mTelecoilImageButton = rootView.findViewById(R.id.home_telecoil_image_button);
        mTelecoilImageButton.setOnClickListener(onClickListener);

        // power mode
        mPowerModeImageButton = rootView.findViewById(R.id.home_low_power_image_button);
        mPowerModeImageButton.setOnClickListener(onClickListener);

        // program
        mProgramTextView = rootView.findViewById(R.id.home_program_value_text);
        mProgramPlusImageButton = rootView.findViewById(R.id.home_program_plus_button);
        mProgramMinusImageButton = rootView.findViewById(R.id.home_program_minus_button);
        mProgramPlusImageButton.setOnClickListener(onClickListener);
        mProgramMinusImageButton.setOnClickListener(onClickListener);

        // sensitivity
        mSensitivityTextView = rootView.findViewById(R.id.home_sensitivity_value_text);
        mSensitivityProgressBar = rootView.findViewById(R.id.home_sensitivity_progressbar);
        mSensitivityPlusImageButton = rootView.findViewById(R.id.home_sensitivity_plus_button);
        mSensitivityMinusImageButton = rootView.findViewById(R.id.home_sensitivity_minus_button);
        mSensitivityPlusImageButton.setOnClickListener(onClickListener);
        mSensitivityMinusImageButton.setOnClickListener(onClickListener);

        // volume
        mVolumeTextView = rootView.findViewById(R.id.home_volume_value_text);
        mVolumeProgressBar = rootView.findViewById(R.id.home_volume_progressbar);
        mVolumePlusImageButton = rootView.findViewById(R.id.home_volume_plus_button);
        mVolumeMinusImageButton = rootView.findViewById(R.id.home_volume_minus_button);
        mVolumePlusImageButton.setOnClickListener(onClickListener);
        mVolumeMinusImageButton.setOnClickListener(onClickListener);

        // not yet connected
        mNotConnectedScreenConstraintLayout = rootView.findViewById(R.id.home_not_yet_connected_layout);

        // If there is no Sound Processor registered on App, send broadcast for checking database to show database empty screen.
        if (AppParam.getInstance().database.deviceDao().findAll().size() == 0)
        {
            mMainActivity.sendBroadcast(new Intent(ActionMessage.DATABASE_CHECK_EMPTY));

            // ESKIM start
            /*
            AppParam.getInstance().bleConnectionState = AppParam.BLE_CONNECTION_STATE_CONNECTED;

            Device device = new Device();
            DeviceParam deviceParam = new DeviceParam();

            device.setDeviceName("abcdefgh");
            device.setDeviceModel("01");
            device.setMapCount("4");

            deviceParam.setBattery((byte) 17);
            deviceParam.setAlarmStimulation((byte) 2);
            deviceParam.setAlarmLed((byte) 1);
            deviceParam.setTelecoil((byte) 2);
            deviceParam.setProgram((byte) 2);
            deviceParam.setSensitivity((byte) 4);
            deviceParam.setVolume((byte) 1);

            AppParam.getInstance().currentConnectDevice = device;
            AppParam.getInstance().currentStatusParams = deviceParam;

            updateScreen();
            //mMainActivity.makeDialogRelaunchApp();
            //mMainActivity.makeDialogReattachSoundProcessorAndDisconnect();
            */
            // ESKIM end

        }
        else
        {
            // If there is a Sound Processor registered on App, update home screen.
            // Then check the connection state.
            updateScreen();

            // Send broadcast for scanning Sound Processor to connect to that.
            if (AppParam.getInstance().bleConnectionState == AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
            {
                if (AppParam.getInstance().isAutoConnectionEnabled())
                {
                    mMainActivity.sendBroadcast(new Intent(ActionMessage.BLE_SCAN_START));
                }
            }
        }

        return rootView;
    }

    /**
     * Callback for onClickListener.
     */
    View.OnClickListener onClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler

            if (AppParam.getInstance().isBleBusy)
            {
                Log.d(TAG, "Fragment Home : onClick is ignored because BLE is busy.");
                return;
            }

            byte[] value = null;
            byte[] sendPacket = null;

            // Alarm stimulation
            if (view.getId() == R.id.home_stim_alarm_image_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Alarm stimulation.");

                // Currently enabled.
                if (AppParam.getInstance().currentStatusParams.getAlarmStimulation() == DeviceParam.ALARM_STIMULATION_ON)
                {
                    AppParam.getInstance().sendingStatusParams.setAlarmStimulation(DeviceParam.ALARM_STIMULATION_OFF);
                }
                // Currently disabled.
                else
                {
                    AppParam.getInstance().sendingStatusParams.setAlarmStimulation(DeviceParam.ALARM_STIMULATION_ON);

                }

                mMainActivity.sendPacket(
                        mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_SIMULATION,
                                new byte[]{AppParam.getInstance().sendingStatusParams.getAlarmStimulation()}, 2));
            }
            // Alarm LED
            else if (view.getId() == R.id.home_led_alarm_image_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Alarm LED.");

                // Currently enabled.
                if (AppParam.getInstance().currentStatusParams.getAlarmLed() == DeviceParam.ALARM_LED_ON)
                {
                    AppParam.getInstance().sendingStatusParams.setAlarmLed(DeviceParam.ALARM_LED_OFF);
                }
                // Currently disabled.
                else
                {
                    AppParam.getInstance().sendingStatusParams.setAlarmLed(DeviceParam.ALARM_LED_ON);
                }

                mMainActivity.sendPacket(
                        mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_LED,
                                new byte[]{AppParam.getInstance().sendingStatusParams.getAlarmLed()}, 2));
            }
            // Telecoil
            else if (view.getId() == R.id.home_telecoil_image_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Telecoil.");

                if (AppParam.getInstance().currentStatusParams.getTelecoil() == DeviceParam.TELECOIL_ON)
                {
                    AppParam.getInstance().sendingStatusParams.setTelecoil(DeviceParam.TELECOIL_OFF);
                }
                else
                {
                    AppParam.getInstance().sendingStatusParams.setTelecoil(DeviceParam.TELECOIL_ON);
                }

                mMainActivity.sendPacket(
                        mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_TELECOIL,
                                new byte[]{AppParam.getInstance().sendingStatusParams.getTelecoil()}, 2));
            }
            // Power Mode
            /*
            else if (view.getId() == R.id.home_low_power_image_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Power Mode.");

                if (AppParam.getInstance().currentStatusParams.getPowerMode() == DeviceParam.POWER_MODE_ON)
                {
                    AppParam.getInstance().sendingStatusParams.setPowerMode(DeviceParam.POWER_MODE_OFF);
                }
                else
                {
                    AppParam.getInstance().sendingStatusParams.setPowerMode(DeviceParam.POWER_MODE_ON);
                }

                mMainActivity.sendPacket(
                        mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_POWER_MODE,
                                new byte[]{AppParam.getInstance().sendingStatusParams.getPowerMode()}, 2));
            }
            */
            // Program plus
            else if (view.getId() == R.id.home_program_plus_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Program plus.");

                byte program;

                // If current program number is equal to last program number, program number will set to 1.
                if (AppParam.getInstance().currentStatusParams.getProgram() == Byte.parseByte(AppParam.getInstance().currentConnectDevice.getMapCount()))
                {
                    program = (byte) 1;
                }
                else
                {
                    program = (byte) ((AppParam.getInstance().currentStatusParams.getProgram() + 1) & 0xff);
                }

                AppParam.getInstance().sendingStatusParams.setProgram(program);

                mMainActivity.sendPacket(
                        mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_PROMGRAM,
                                new byte[]{AppParam.getInstance().sendingStatusParams.getProgram()}, 2));
            }
            // Program minus
            else if (view.getId() == R.id.home_program_minus_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Program minus.");

                byte program;

                // If current program number is equal to 1, program number will set to last program number.
                if (AppParam.getInstance().currentStatusParams.getProgram() == 1)
                {
                    program = Byte.parseByte(AppParam.getInstance().currentConnectDevice.getMapCount());
                }
                else
                {
                    program = (byte) ((AppParam.getInstance().currentStatusParams.getProgram() - 1) & 0xff);
                }

                AppParam.getInstance().sendingStatusParams.setProgram(program);

                mMainActivity.sendPacket(
                        mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_PROMGRAM,
                                new byte[]{AppParam.getInstance().sendingStatusParams.getProgram()}, 2));
            }
            // Sensitivity plus
            else if (view.getId() == R.id.home_sensitivity_plus_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Sensitivity plus.");

                if (AppParam.getInstance().currentStatusParams.getSensitivity() < 4)
                {
                    byte sensitivity = (byte) ((AppParam.getInstance().currentStatusParams.getSensitivity() + 1) & 0xff);

                    AppParam.getInstance().sendingStatusParams.setSensitivity(sensitivity);

                    mMainActivity.sendPacket(
                            mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_SENSITIVITY,
                                    new byte[]{AppParam.getInstance().sendingStatusParams.getSensitivity()}, 2));
                }
            }
            // Sensitivity minus
            else if (view.getId() == R.id.home_sensitivity_minus_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Sensitivity minus.");

                if (AppParam.getInstance().currentStatusParams.getSensitivity() > 1)
                {
                    byte sensitivity = (byte) ((AppParam.getInstance().currentStatusParams.getSensitivity() - 1) & 0xff);

                    AppParam.getInstance().sendingStatusParams.setSensitivity(sensitivity);

                    mMainActivity.sendPacket(
                            mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_SENSITIVITY,
                                    new byte[]{AppParam.getInstance().sendingStatusParams.getSensitivity()}, 2));
                }
            }
            // Volume plus
            else if (view.getId() == R.id.home_volume_plus_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Volume plus.");

                if (AppParam.getInstance().currentStatusParams.getVolume() < 10)
                {
                    byte volume = (byte) ((AppParam.getInstance().currentStatusParams.getVolume() + 1) & 0xff);

                    AppParam.getInstance().sendingStatusParams.setVolume(volume);

                    mMainActivity.sendPacket(
                            mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_VOLUME,
                                    new byte[]{AppParam.getInstance().sendingStatusParams.getVolume()}, 2));
                }
            }
            // Volume minus
            else if (view.getId() == R.id.home_volume_minus_button)
            {
                Log.d(TAG, "Fragment Home : onClick - Volume minus.");

                if (AppParam.getInstance().currentStatusParams.getVolume() > 1)
                {
                    byte volume = (byte) ((AppParam.getInstance().currentStatusParams.getVolume() - 1) & 0xff);

                    AppParam.getInstance().sendingStatusParams.setVolume(volume);

                    mMainActivity.sendPacket(
                            mMainActivity.packetMaker(MainActivity.PACKET_HEADER_VALUE_VOLUME,
                                    new byte[]{AppParam.getInstance().sendingStatusParams.getVolume()}, 2));
                }
            }
        } // onClick
    }; // onClickListener

    public void updateScreen()
    {
        new Handler(Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                Device currentConnectDevice = AppParam.getInstance().currentConnectDevice;
                DeviceParam currentStatusParams = AppParam.getInstance().currentStatusParams;

                if (currentStatusParams != null && currentConnectDevice != null
                        && AppParam.getInstance().bleConnectionState == AppParam.BLE_CONNECTION_STATE_CONNECTED)
                {
                    Log.d(TAG, "Start updating home screen.");

                    // Sound Processor name.
                    mDeviceNameTextView.setText(currentConnectDevice.getDeviceName());

                    // Sound Processor model.
                    updateScreenModel(currentConnectDevice.getDeviceModel());

                    // battery
                    updateScreenBattery(currentStatusParams.getBattery());

                    // alarm stimulation
                    updateScreenAlarmStimulation(currentStatusParams.getAlarmStimulation());

                    // alarm led
                    updateScreenAlarmLed(currentStatusParams.getAlarmLed());

                    // telecoil
                    updateScreenTelecoil(currentStatusParams.getTelecoil());

                    // powermode
                    updateScreenPowerMode(currentStatusParams.getPowerMode());

                    // program
                    updateScreenProgram(currentStatusParams.getProgram());

                    // sensitivity
                    updateScreenSensitivity(currentStatusParams.getSensitivity());

                    // volume
                    updateScreenVolume(currentStatusParams.getVolume());

                    // Not connected screen : disable.
                    mNotConnectedScreenConstraintLayout.setVisibility(View.GONE);
                }
                else // Currently not connected state.
                {
                    // Not connected screen : enable.
                    mNotConnectedScreenConstraintLayout.setVisibility(View.VISIBLE);
                }

                Log.d(TAG, "update home screen done.");
            } // run
        }); // handler using Loop.getMainLooper
    } // updateScreen

    // Updater model.
    private void updateScreenModel(String model)
    {
        if (model.equals(DeviceParam.DEVICE_MODEL_TD2))
        {
            mModelImageView.setImageDrawable(ContextCompat.getDrawable(mMainActivity, R.drawable.drawable_model_td2));
        }
        else
        {
            mModelImageView.setImageDrawable(ContextCompat.getDrawable(mMainActivity, R.drawable.drawable_model_unknown));
        }
    }

    // Updater battery.
    private void updateScreenBattery(byte battery)
    {
        if (battery > 100)
        {
            battery = 100;
        }

        if (battery < 0)
        {
            battery = 0;
        }

        mBatteryTextView.setText(String.format(Locale.ENGLISH, "%d%%", battery));
        mBatteryProgressBar.setProgress(battery);

        if (battery < 20)
        {
            mBatteryProgressBar.setProgressDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.custom_progressbar_battery_level_low));
        }
        else if (battery < 50)
        {
            mBatteryProgressBar.setProgressDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.custom_progressbar_battery_level_medium));
        }
        else
        {
            mBatteryProgressBar.setProgressDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.custom_progressbar_battery_level_high));
        }
    }

    // Updater alarm stimulation.
    private void updateScreenAlarmStimulation(byte stimulation)
    {
        if (stimulation == 1)
        {
            mStimulationAlarmImageButton.setImageTintList(ColorStateList.valueOf(requireActivity().getColor(R.color.color_home_enabled_button_tint)));
        }
        else
        {
            mStimulationAlarmImageButton.setImageTintList(ColorStateList.valueOf(requireActivity().getColor(R.color.color_home_disabled_button_tint)));
        }
    }

    // Updater alarm LED.
    private void updateScreenAlarmLed(byte led)
    {
        if (led == 1)
        {
            mLedAlarmImageButton.setImageTintList(ColorStateList.valueOf(requireActivity().getColor(R.color.color_home_enabled_button_tint)));
        }
        else
        {
            mLedAlarmImageButton.setImageTintList(ColorStateList.valueOf(requireActivity().getColor(R.color.color_home_disabled_button_tint)));
        }
    }

    // Updater telecoil.
    private void updateScreenTelecoil(byte telecoil)
    {
        if (telecoil == 1)
        {
            mTelecoilImageButton.setImageTintList(ColorStateList.valueOf(requireActivity().getColor(R.color.color_home_enabled_button_tint)));
        }
        else
        {
            mTelecoilImageButton.setImageTintList(ColorStateList.valueOf(requireActivity().getColor(R.color.color_home_disabled_button_tint)));
        }
    }

    // Updater power mode.
    private void updateScreenPowerMode(byte powermode)
    {
        if (powermode == 1)
        {

            mPowerModeImageButton.setImageResource(R.drawable.icon_low_power_disabled);
            mPowerModeImageButton.setImageTintList(ColorStateList.valueOf(requireActivity().getColor(R.color.color_home_enabled_button_tint)));
        }
        else
        {
            mPowerModeImageButton.setImageResource(R.drawable.icon_low_power_enabled);
            mPowerModeImageButton.setImageTintList(ColorStateList.valueOf(requireActivity().getColor(R.color.color_home_disabled_button_tint)));
        }
    }

    // Updater program.
    private void updateScreenProgram(byte program)
    {
        String number = "" + program;
        mProgramTextView.setText(number);
    }

    // Updater sensitivity.
    private void updateScreenSensitivity(byte sensitivity)
    {
        mSensitivityProgressBar.setProgress(sensitivity);
        mSensitivityTextView.setText(sensitivity + "");
    }

    // Updater volume.
    private void updateScreenVolume(byte volume)
    {
        mVolumeProgressBar.setProgress(volume);
        mVolumeTextView.setText(volume + "");
    }

    /**
     * Make dialog for back button pressed.
     */
    public void makeDialogBackPressed()
    {
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity, R.style.MyAlertDialogTheme);

        builder.setMessage(getString(R.string.activity_main_dialog_message_exit_app));

        builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                // Send broadcast for finishing activity.
                requireActivity().sendBroadcast(new Intent(ActionMessage.ACTIVITY_FINISH));
            }
        });

        builder.setNegativeButton(getString(R.string.dialog_message_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
            }
        });

        AppParam.getInstance().lastDialog = builder.create();
        AppParam.getInstance().lastDialog.show();
    }
}