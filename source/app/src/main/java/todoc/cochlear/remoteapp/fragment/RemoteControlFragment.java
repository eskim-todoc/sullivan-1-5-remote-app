package todoc.cochlear.remoteapp.fragment;

import android.app.Activity;

import todoc.cochlear.remoteapp.ff.Ff3Config;
import todoc.cochlear.remoteapp.ff.Ff3Dialog;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import todoc.cochlear.remoteapp.params.BatteryTelemetry;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Collections;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentRemoteControlBinding;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.ota.Ota;
import todoc.cochlear.remoteapp.params.PacketInfo;
import todoc.cochlear.remoteapp.params.Status;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;
import todoc.cochlear.remoteapp.connection.ConnectionInfo;
import todoc.cochlear.remoteapp.ui.OtaDeviceList;
import todoc.cochlear.remoteapp.ui.DeviceRail;
import todoc.cochlear.remoteapp.connection.OtaProgress;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.graphics.Paint;

public class RemoteControlFragment extends Fragment
{
    static final private String TAG = "TODOC_" + RemoteControlFragment.class.getSimpleName();
    public static final String ARG_CONTROL_TAB = "controlTab";

    public ActivityMainBinding mMainBinding;
    public FragmentRemoteControlBinding mRemoteControlBinding;

    StatusViewModel mStatusViewModel;

    public AlertDialog mDialog;
    private Ff3Dialog ff3Dialog;
    private OtaDeviceList otaDeviceList;
    private DeviceRail deviceRail;
    private List<OtaProgress> otaProgressStates = Collections.emptyList();
    private String appliedOtaControlState;
    private SessionObservation sessionObservation;
    private String observedSessionId;
    private int viewGeneration;
    private int selectedControlTab;
    private final int[] controlScrollPositions = new int[4];
    private boolean controlTabInitialized;
    private final Handler batteryUiHandler = new Handler(Looper.getMainLooper());
    private final Runnable batteryUiTick = new Runnable()
    {
        @Override
        public void run()
        {
            renderBatteryTelemetry();
            updateOtaTargetText();
            if (otaDeviceList != null)
            {
                otaDeviceList.refreshMetadata();
            }
            if (deviceRail != null)
            {
                deviceRail.refreshMetadata();
            }
            batteryUiHandler.postDelayed(this, 1000);
        }
    };

    Activity mActivity;
    MainActivity mMainActivity;
    Ota mOta;
    Ota mOtaFiles;

    public RemoteControlFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onDestroyView()
    {
        if (sessionObservation != null)
        {
            sessionObservation.destroy();
            sessionObservation = null;
        }
        if (ff3Dialog != null)
        {
            ff3Dialog.dismiss();
            ff3Dialog = null;
        }
        if (mDialog != null && mDialog.isShowing())
        {
            mDialog.dismiss();
            mDialog = null;
        }

        batteryUiHandler.removeCallbacks(batteryUiTick);
        if (mRemoteControlBinding != null)
        {
            mRemoteControlBinding.remoteControlConnectionGlass.stopWaves();
        }
        viewGeneration++;
        mRemoteControlBinding = null;
        mStatusViewModel = null;
        observedSessionId = null;
        otaDeviceList = null;
        deviceRail = null;
        otaProgressStates = Collections.emptyList();

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
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        mMainBinding.toolbar.setTitle("");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mRemoteControlBinding = FragmentRemoteControlBinding.inflate(inflater, container, false);

        configureColumns(mRemoteControlBinding, MainActivity.isTabletAtLaunch(),
            getResources().getDisplayMetrics().density);
        showControlTab(mRemoteControlBinding, 0);
        return mRemoteControlBinding.getRoot();
    }

    static void configureColumns(FragmentRemoteControlBinding binding, boolean tablet, float density)
    {
        configureStatusValues(binding);
        int columns = 3;
        binding.specialControlGrid.setColumnCount(columns);
        for (int i = 0; i < binding.specialControlGrid.getChildCount(); i++)
        {
            View button = binding.specialControlGrid.getChildAt(i);
            android.widget.GridLayout.LayoutParams params = (android.widget.GridLayout.LayoutParams) button.getLayoutParams();
            params.columnSpec = android.widget.GridLayout.spec(i % columns, 1f);
            params.rowSpec = android.widget.GridLayout.spec(i / columns);
            button.setLayoutParams(params);
        }

        // The approved control surface is a single portrait flow on every screen size.
        tablet = false;
        binding.remotePrimaryLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams status = new LinearLayout.LayoutParams(
            tablet ? 0 : ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, tablet ? 0.43f : 0);
        binding.remoteStatusColumn.setLayoutParams(status);
        LinearLayout.LayoutParams controls = new LinearLayout.LayoutParams(
            tablet ? 0 : ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, tablet ? 0.57f : 0);
        controls.setMarginStart(tablet ? Math.round(12 * density) : 0);
        controls.topMargin = tablet ? 0 : Math.round(6 * density);
        binding.remoteControlsColumn.setLayoutParams(controls);
    }

    /**
     * Keep protocol-oriented status labels out of the aligned value columns.
     */
    static String displayStatusValue(String raw)
    {
        return raw.replaceFirst("^(Prog|Vol|Out|Strategy|PW|Frm|Gating|LED|Alarm|BT|Step|Fix|Coin|Accel|Nop|Mode|Cur|PMIC|Map|Max)\\s+", "")
            .replace("us", "㎲").replace("μs", "㎲")
            .replace("mA", "㎃").replace("mW", "㎽").replace("mV", "㎷")
            .replaceAll("(?<=\\d)\\s*V\\b", " Ｖ")
            .replaceAll("(?<=\\d)(Ｖ|ms|㎲)", " $1");
    }

    private static void configureStatusValues(FragmentRemoteControlBinding binding)
    {
        int[] ids = {R.id.ota_program_textview, R.id.ota_volume_textview, R.id.ota_max_output_textview,
            R.id.ota_strategy_textview, R.id.ota_pulse_width_textview, R.id.ota_frame_num_textview,
            R.id.ota_gating_textview, R.id.ota_led_textview, R.id.ota_alarm_textview,
            R.id.ota_backtel_textview, R.id.ota_step_up_textview, R.id.ota_force_pmic_textview,
            R.id.ota_one_coin_textview, R.id.ota_accel_textview, R.id.ota_nop_standby_textview,
            R.id.ota_ctrl_mode_textview, R.id.ota_cur_pmic_textview, R.id.ota_pmic_textview,
            R.id.ota_mapping_pmic_textview, R.id.ota_max_pmic_textview,
            R.id.battery_voltage_textview, R.id.battery_current_textview, R.id.battery_power_textview};
        for (int id : ids)
        {
            TextView value = binding.getRoot().findViewById(id);
            value.addTextChangedListener(new android.text.TextWatcher()
            {
                private boolean formatting;

                public void beforeTextChanged(CharSequence s, int start, int count, int after)
                {
                }

                public void onTextChanged(CharSequence s, int start, int before, int count)
                {
                }

                public void afterTextChanged(android.text.Editable editable)
                {
                    if (formatting)
                    {
                        return;
                    }
                    String formatted = displayStatusValue(editable.toString());
                    formatting = true;
                    if (!formatted.contentEquals(editable))
                    {
                        value.setText(formatted);
                    }
                    if (id == R.id.ota_led_textview || id == R.id.ota_alarm_textview ||
                        id == R.id.ota_gating_textview || id == R.id.ota_ctrl_mode_textview)
                    {
                        boolean on = "On".equals(formatted);
                        boolean off = "Off".equals(formatted);
                        value.setBackgroundResource(on ? R.drawable.status_badge_on : off ? R.drawable.status_badge_off : R.drawable.remote_badge_blue);
                        value.setTextColor(on ? 0xFFB7F0CD : off ? 0xFFB9BFC7 : 0xFFE1EBEC);
                    }
                    if (id == R.id.ota_program_textview)
                    {
                        binding.controlProgramValue.setText(formatted);
                    }
                    if (id == R.id.ota_volume_textview)
                    {
                        binding.controlVolumeValue.setText(formatted);
                    }
                    if (id == R.id.ota_max_output_textview)
                    {
                        binding.controlOutputValue.setText(formatted);
                    }
                    formatting = false;
                }
            });
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "onViewCreated is called!");

        mActivity = requireActivity();
        mMainActivity = (MainActivity) mActivity;
        mOtaFiles = mMainActivity.getOtaFileSelection();

        viewGeneration++;
        controlTabInitialized = false;
        bindSelectedSession();
        deviceRail = new DeviceRail(mMainActivity, mRemoteControlBinding.connectionDeviceCards,
            mRemoteControlBinding.connectionDeviceScroll, mRemoteControlBinding.connectionAddButton,
            () ->
            {
                bindSelectedSession();
                renderConnectionSelector(mMainActivity.getConnectionStates().getValue());
                renderOtaProgress();
            },
            mMainActivity::startRegisteredDeviceScan);
        mMainActivity.getConnectionStates().observe(getViewLifecycleOwner(), devices ->
        {
            bindSelectedSession();
            renderConnectionSelector(devices);
            renderOtaProgress();
            updateConnectionOverlay();
        });
        mMainActivity.getDeviceScanState().observe(getViewLifecycleOwner(), scanning ->
        {
            updateDeviceScanUi(Boolean.TRUE.equals(scanning));
            updateConnectionOverlay();
        });
        otaDeviceList = new OtaDeviceList(mMainActivity, mRemoteControlBinding.otaDeviceList,
            () ->
            {
                bindSelectedSession();
                renderOtaProgress();
            });
        mMainActivity.getOtaProgressStates().observe(getViewLifecycleOwner(), states ->
        {
            otaProgressStates = states == null ? Collections.emptyList() : states;
            renderOtaProgress();
        });

        checkRegisteredList_userAndDevice(); // User list

        if (savedInstanceState != null)
        {
            selectedControlTab = Math.min(3, Math.max(0, savedInstanceState.getInt("selectedControlTab", 0)));
            int[] savedScroll = savedInstanceState.getIntArray("controlScrollPositions");
            if (savedScroll != null && savedScroll.length == controlScrollPositions.length)
            {
                System.arraycopy(savedScroll, 0, controlScrollPositions, 0, controlScrollPositions.length);
            }
        }
        else if (getArguments() != null)
        {
            selectedControlTab = Math.min(3, Math.max(0, getArguments().getInt(ARG_CONTROL_TAB, 0)));
        }
        // Apply the masked ripple to each touch target so its animation uses local bounds.
        android.view.ViewGroup tabStrip = (android.view.ViewGroup) mRemoteControlBinding.controlTabs.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++)
        {
            tabStrip.getChildAt(i).setBackgroundResource(R.drawable.remote_tab_press);
        }
        mRemoteControlBinding.controlTabs.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab)
            {
                if (tab.getPosition() != 4 && isOtaTransferActive())
                {
                    mRemoteControlBinding.controlTabs.selectTab(mRemoteControlBinding.controlTabs.getTabAt(4));
                    Toast.makeText(requireContext(), "파일 읽기가 끝난 뒤 화면을 이동하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (tab.getPosition() == 3)
                {
                    mMainActivity.replaceFragment(Status.TypeOfFragment.MENU);
                    return;
                }
                if (controlTabInitialized)
                {
                    controlScrollPositions[selectedControlTab] = mRemoteControlBinding.remoteContentScroll.getScrollY();
                }
                selectedControlTab = tab.getPosition() == 4 ? 3 : tab.getPosition();
                showControlTab(mRemoteControlBinding, selectedControlTab);
                restoreControlScrollPosition();
                updateConnectionOverlay();
                updateOtaControlPause();
                updatePageTitle();
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab)
            {
            }

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab)
            {
            }
        });
        mRemoteControlBinding.controlTabs.selectTab(mRemoteControlBinding.controlTabs.getTabAt(selectedControlTab == 3 ? 4 : selectedControlTab));
        showControlTab(mRemoteControlBinding, selectedControlTab);
        restoreControlScrollPosition();
        controlTabInitialized = true;
        updateConnectionOverlay();
        updatePageTitle();
        updateIdentityBadges();
        updateOtaTargetText();

        mRemoteControlBinding.slotRadioButtonGroup.setOnCheckedChangeListener(onChecked_radioGroup);

        mRemoteControlBinding.otaInfoButton.setOnClickListener(onClick_infoButton);
        mRemoteControlBinding.otaCollectButton.setOnClickListener(onClick_collectButton);
        mRemoteControlBinding.otaChooseFilesButton.setOnClickListener(onClick_collectButton);
        mRemoteControlBinding.otaWriteButton.setOnClickListener(onClick_writeButton);
        mRemoteControlBinding.otaAddDeviceButton.setOnClickListener(clicked -> mMainActivity.startRegisteredDeviceScan());
        mRemoteControlBinding.otaPrepareButton.setOnClickListener(v ->
        {
            String sessionId = mMainActivity.getSelectedSessionId();
            if (sessionId == null || !mMainActivity.prepareOta(sessionId))
            {
                Toast.makeText(requireContext(), "장치 연결과 진행 중인 작업을 확인하세요.", Toast.LENGTH_SHORT).show();
            }
        });
        mRemoteControlBinding.otaSelectButton.setOnClickListener(onClick_selectButton);
        mRemoteControlBinding.otaFactoryResetButton.setOnClickListener(onClick_factoryResetButton);

        mRemoteControlBinding.otaMapInitDefaultButton.setOnClickListener(onClick_mapInitDefaultButton);

        // Remote 꼭지
        mRemoteControlBinding.otaProgramDownButton.setOnClickListener(onClick_programDownButton);
        mRemoteControlBinding.otaProgramUpButton.setOnClickListener(onClick_programUpButton);
        mRemoteControlBinding.otaVolumeDownButton.setOnClickListener(onClick_volumeDownButton);
        mRemoteControlBinding.otaVolumeUpButton.setOnClickListener(onClick_volumeUpButton);
        mRemoteControlBinding.otaMaxOutputDownButton.setOnClickListener(onClick_maxOutputDownButton);
        mRemoteControlBinding.otaMaxOutputUpButton.setOnClickListener(onClick_maxOutputUpButton);
        mRemoteControlBinding.otaLedButton.setOnClickListener(onClick_ledButton);
        mRemoteControlBinding.otaAlarmButton.setOnClickListener(onClick_alarmButton);

        // Etc 꼭지
        mRemoteControlBinding.otaMapSelectButton.setOnClickListener(onClick_mapSelectButton);
        mRemoteControlBinding.otaGatingButton.setOnClickListener(onClick_gatingButton);
        mRemoteControlBinding.otaGatingButton.setOnLongClickListener(button ->
        {
            showGatingOffsetDialog();
            return true;
        });
        mRemoteControlBinding.otaPmicSelectButton.setOnClickListener(onClick_pmicSelectButton);
        mRemoteControlBinding.otaMappingPmicButton.setOnClickListener(onClick_mappingPmicButton);
        mRemoteControlBinding.otaBtSelectButton.setOnClickListener(onClick_btSelectButton);
        mRemoteControlBinding.otaLinkStepButton.setOnClickListener(onClick_linkStepButton);
        mRemoteControlBinding.otaFixedPmicButton.setOnClickListener(onClick_fixedPmicButton);
        mRemoteControlBinding.otaOneCoinButton.setOnClickListener(onClick_oneCoinButton);
        mRemoteControlBinding.otaCtrlModeButton.setOnClickListener(onClick_ctrlModeButton);
        mRemoteControlBinding.otaAccelButton.setOnClickListener(onClick_accelButton);

        mRemoteControlBinding.otaNopStandbyButton.setOnClickListener(onClick_nopStandbyButton);
        mRemoteControlBinding.otaMaxPmicButton.setOnClickListener(onClick_maxPmicButton);
        mRemoteControlBinding.otaFfCheckButton.setOnClickListener(v ->
        {
            if (mMainActivity.isFf3Protocol())
            {
                mMainActivity.ff3.load();
            }
            updateFfControls();
        });
        mRemoteControlBinding.otaFfButton.setOnClickListener(onClick_ffButton);
        mRemoteControlBinding.otaFf1SettingsButton.setOnClickListener(v ->
        {
            mMainActivity.requestFf1Settings();
            makeDialog_ffSettings();
        });
        mRemoteControlBinding.otaFf3SettingsButton.setOnClickListener(v -> showFf3(false));
        mRemoteControlBinding.otaFf3DiagnosticsButton.setOnClickListener(v -> showFf3(true));

        /* 아직 연결 전이라 세대가 미상이다. 잠긴 상태로 시작한다.
         * 옵저버는 값이 «바뀔 때» 만 오므로 첫 상태는 여기서 직접 맞춰야 한다. */
        applyFwRelease();

        // 화면을 다시 열었을 때도 지난번에 수집한 폴더가 그대로 보이게 한다.
        updateCollectFolderText();
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Runnable - collect

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* MFST 부터 APP2 까지 순서대로 읽는다.
     *
     * 예전에는 화면에서 파일을 하나 골라 그것만 읽었다. 네 개를 다 올리려면 고르고
     * 누르기를 네 번 반복해야 했고, 하나를 빠뜨리면 전송 때가 되어서야 알았다.
     *
     * 없는 파일은 건너뛴다. 슬롯에 따라 APP 이 세 개가 아닐 수 있고, 그것이 오류는 아니다. */
    private static final class SessionObservation implements LifecycleOwner
    {
        private final LifecycleRegistry lifecycle = new LifecycleRegistry(this);

        SessionObservation()
        {
            lifecycle.setCurrentState(Lifecycle.State.CREATED);
        }

        @NonNull
        @Override
        public Lifecycle getLifecycle()
        {
            return lifecycle;
        }

        void start()
        {
            lifecycle.setCurrentState(Lifecycle.State.STARTED);
        }

        void stop()
        {
            lifecycle.setCurrentState(Lifecycle.State.CREATED);
        }

        void destroy()
        {
            lifecycle.setCurrentState(Lifecycle.State.DESTROYED);
        }
    }

    /**
     * Destroy the previous observer owner before publishing any selected-device values.
     */
    private void bindSelectedSession()
    {
        StatusViewModel selectedModel = mMainActivity.getSelectedStatusViewModel();
        String selectedId = mMainActivity.getSelectedSessionId();
        if (sessionObservation != null && selectedModel == mStatusViewModel
            && Objects.equals(selectedId, observedSessionId))
        {
            return;
        }
        if (sessionObservation != null)
        {
            sessionObservation.destroy();
        }
        if (ff3Dialog != null)
        {
            ff3Dialog.dismiss();
            ff3Dialog = null;
        }
        if (mDialog != null)
        {
            mDialog.dismiss();
            mDialog = null;
        }
        mStatusViewModel = selectedModel;
        observedSessionId = selectedId;
        mOta = mMainActivity.mOta;
        appliedOtaControlState = null;
        sessionObservation = new SessionObservation();
        clearSelectedValues();
        liveDataConnection();
        liveDataIsdID();
        liveDataProgramAndLevels();
        mStatusViewModel.getBatteryTelemetry().observe(sessionObservation, value -> renderBatteryTelemetry());
        mStatusViewModel.getLiveDataBatteryLevel().observe(sessionObservation, value -> renderBatteryTelemetry());
        mStatusViewModel.getBatteryTelemetryError().observe(sessionObservation, value -> renderBatteryTelemetry());
        mStatusViewModel.getObjectConnectionState().observe(sessionObservation, value -> renderBatteryTelemetry());
        mStatusViewModel.getLiveDataFwRelease().observe(sessionObservation, value -> renderBatteryTelemetry());
        mMainActivity.ff3Revision.observe(sessionObservation, value ->
        {
            updateFfControls();
            if (ff3Dialog != null)
            {
                ff3Dialog.refresh();
            }
        });
        mStatusViewModel.getLiveDataFfEnable().observe(sessionObservation, value -> updateFfControls());
        mStatusViewModel.getLiveDataFfStep().observe(sessionObservation, value -> updateFfControls());
        mStatusViewModel.getLiveDataFfCooldown().observe(sessionObservation, value -> updateFfControls());
        updateIdentityBadges();
        renderBatteryTelemetry();
        applyFwRelease();
        updateCollectFolderText();
        updateOtaTargetText();
        if (getViewLifecycleOwner().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            sessionObservation.start();
        }
    }

    private void clearSelectedValues()
    {
        int[] ids = {R.id.remote_control_connection_isd_id_textview, R.id.ota_program_textview,
            R.id.ota_volume_textview, R.id.ota_max_output_textview, R.id.ota_strategy_textview,
            R.id.ota_pulse_width_textview, R.id.ota_frame_num_textview, R.id.ota_gating_textview,
            R.id.ota_led_textview, R.id.ota_alarm_textview, R.id.ota_backtel_textview,
            R.id.ota_step_up_textview, R.id.ota_force_pmic_textview, R.id.ota_one_coin_textview,
            R.id.ota_accel_textview, R.id.ota_nop_standby_textview, R.id.ota_ctrl_mode_textview,
            R.id.ota_cur_pmic_textview, R.id.ota_pmic_textview, R.id.ota_mapping_pmic_textview,
            R.id.ota_max_pmic_textview};
        for (int id : ids)
        {
            ((TextView) mRemoteControlBinding.getRoot().findViewById(id)).setText("—");
        }
        mRemoteControlBinding.currSlotTextView.setText("—");
        mRemoteControlBinding.lastSlotTextView.setText("—");
        mRemoteControlBinding.slotRadioButtonGroup.setOnCheckedChangeListener(null);
        if (mOta.currentSlotNum == Ota.SLOT_NUM_1)
        {
            mRemoteControlBinding.slotRadioButtonGroup.check(R.id.slot_1_radio_button);
        }
        else if (mOta.currentSlotNum == Ota.SLOT_NUM_2)
        {
            mRemoteControlBinding.slotRadioButtonGroup.check(R.id.slot_2_radio_button);
        }
        else
        {
            mRemoteControlBinding.slotRadioButtonGroup.clearCheck();
        }
        mRemoteControlBinding.slotRadioButtonGroup.setOnCheckedChangeListener(onChecked_radioGroup);
        for (int fileNum : OTA_FILE_ORDER)
        {
            Ota.OtaFile file = mOtaFiles.getFile(Ota.SLOT_NUM_COLLECT, fileNum);
            getCollectSizeTextView(fileNum).setText(otaFileSize(file.collectSize));
            getCollectPercentTextView(fileNum).setText(file.collectPercent + "%");
            getWriteSizeTextView(fileNum).setText(Integer.toString(file.writeSize));
            getWritePercentTextView(fileNum).setText(file.writePercent + "%");
        }
    }

    private void renderConnectionSelector(List<ConnectionInfo> devices)
    {
        if (mRemoteControlBinding != null && deviceRail != null)
        {
            deviceRail.render(devices);
        }
    }

    private void updateDeviceScanUi(boolean scanning)
    {
        if (mRemoteControlBinding == null)
        {
            return;
        }
        mRemoteControlBinding.connectionAddButton.setEnabled(!scanning);
        mRemoteControlBinding.connectionAddButton.setImageAlpha(scanning ? 0 : 255);
        mRemoteControlBinding.connectionAddButton.setContentDescription(scanning ? "장치를 검색 중입니다" : "장치 검색 및 추가");
        mRemoteControlBinding.connectionAddProgress.setVisibility(scanning ? View.VISIBLE : View.GONE);
        mRemoteControlBinding.otaAddDeviceButton.setEnabled(!scanning);
        mRemoteControlBinding.otaAddDeviceButton.setText(scanning ? "검색 중…" : "+ 추가 연결");
    }

    private void restoreControlScrollPosition()
    {
        final FragmentRemoteControlBinding binding = mRemoteControlBinding;
        final int tab = selectedControlTab;
        binding.remoteContentScroll.post(() ->
        {
            if (binding == mRemoteControlBinding && tab == selectedControlTab)
            {
                binding.remoteContentScroll.scrollTo(0, controlScrollPositions[tab]);
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (mRemoteControlBinding != null)
        {
            bindSelectedSession();
        }
        if (sessionObservation != null)
        {
            sessionObservation.start();
        }
    }

    @Override
    public void onStop()
    {
        if (sessionObservation != null)
        {
            sessionObservation.stop();
        }
        super.onStop();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        batteryUiHandler.removeCallbacks(batteryUiTick);
        batteryUiHandler.post(batteryUiTick);
    }

    @Override
    public void onPause()
    {
        batteryUiHandler.removeCallbacks(batteryUiTick);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state)
    {
        super.onSaveInstanceState(state);
        state.putInt("selectedControlTab", selectedControlTab);
        if (mRemoteControlBinding != null)
        {
            controlScrollPositions[selectedControlTab] = mRemoteControlBinding.remoteContentScroll.getScrollY();
        }
        state.putIntArray("controlScrollPositions", controlScrollPositions);
    }

    /**
     * Transfers live in the device session; only the short local file read keeps this view busy.
     */
    public boolean isOtaTransferActive()
    {
        return mOtaFiles != null && mOtaFiles.threadState == Ota.THREAD_STATE_BUSY;
    }

    private void updateOtaTargetText()
    {
        if (mOta == null || mOtaFiles == null || mRemoteControlBinding == null)
        {
            return;
        }
        String sessionId = mMainActivity.getSelectedSessionId();
        boolean sameTarget = sessionId != null && Objects.equals(sessionId, observedSessionId);
        todoc.cochlear.remoteapp.connection.DeviceBadgeInfo badge = mMainActivity.getSessionDeviceBadgeInfo(sessionId);
        String target = badge == null ? "목록에서 장치를 선택하세요" : "선택 장치 · " + badge.userName
                                                           + " · " + badge.wearingPosition + " · " + badge.serialNumber;
        setOtaText(mRemoteControlBinding.otaUpdateTargetText, target);
        OtaProgress progress = selectedOtaProgress();
        boolean reading = isOtaTransferActive();
        boolean busy = progress != null && (progress.state == OtaProgress.State.PREPARING
            || progress.state == OtaProgress.State.WRITING);
        boolean connected = mStatusViewModel != null
            && mStatusViewModel.getConnectionState() == StatusViewModel.CONNECTION_STATE_CONNECTED;
        boolean ready = sameTarget && !reading && mMainActivity.canPrepareOta(sessionId);
        todoc.cochlear.remoteapp.connection.OtaDeviceInfo deviceInfo = mMainActivity.getSessionOtaDeviceInfo(sessionId);
        boolean slotKnown = deviceInfo != null && deviceInfo.currentBootSlot != null;
        setOtaActionEnabled(mRemoteControlBinding.otaPrepareButton, ready);
        setOtaActionEnabled(mRemoteControlBinding.otaWriteButton,
            ready && slotKnown && hasAllOtaFiles() && progress != null && progress.state == OtaProgress.State.READY);
        setOtaText(mRemoteControlBinding.otaPrepareButton, progress != null && progress.state == OtaProgress.State.PREPARING
            ? "준비 중…" : "OTA 준비");
        setOtaActionEnabled(mRemoteControlBinding.otaChooseFilesButton, !reading);
        setOtaActionEnabled(mRemoteControlBinding.otaCollectButton, !reading);
        setOtaActionEnabled(mRemoteControlBinding.otaInfoButton, ready);
        setOtaActionEnabled(mRemoteControlBinding.otaSelectButton, ready && slotKnown);
        setOtaActionEnabled(mRemoteControlBinding.otaFactoryResetButton, connected && sameTarget && !busy && !reading);

    }

    private static String otaFileSize(long bytes)
    {
        if (bytes <= 0)
        {
            return "—";
        }
        if (bytes >= 1024 * 1024)
        {
            return String.format(java.util.Locale.ROOT, "%.2f MiB", bytes / (1024.0 * 1024));
        }
        if (bytes >= 1024)
        {
            return String.format(java.util.Locale.ROOT, "%.1f KiB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private boolean hasAllOtaFiles()
    {
        if (mOtaFiles == null || mOtaFiles.fileList == null)
        {
            return false;
        }
        for (int fileNum : OTA_FILE_ORDER)
        {
            boolean collected = false;
            for (Ota.OtaFile file : mOtaFiles.fileList)
            {
                if (file.slotNum == Ota.SLOT_NUM_COLLECT && file.fileNum == fileNum)
                {
                    collected = file.collectPercent == 100 && file.totalBytes > 0;
                    break;
                }
            }
            if (!collected)
            {
                return false;
            }
        }
        return true;
    }

    private OtaProgress selectedOtaProgress()
    {
        for (OtaProgress progress : otaProgressStates)
        {
            if (Objects.equals(progress.sessionId, observedSessionId))
            {
                return progress;
            }
        }
        return null;
    }

    private void renderOtaProgress()
    {
        if (mRemoteControlBinding == null || mOta == null)
        {
            return;
        }
        if (otaDeviceList != null)
        {
            otaDeviceList.render(otaProgressStates);
        }
        for (int fileNum : OTA_FILE_ORDER)
        {
            Ota.OtaFile file = mOtaFiles.getFile(Ota.SLOT_NUM_COLLECT, fileNum);
            setOtaText(getCollectSizeTextView(fileNum), otaFileSize(file.collectSize));
            setOtaText(getCollectPercentTextView(fileNum), file.collectPercent + "%");
            setOtaText(getWriteSizeTextView(fileNum), Integer.toString(file.writeSize));
            setOtaText(getWritePercentTextView(fileNum), file.writePercent + "%");
        }
        updateOtaTargetText();
        updateConnectionOverlay();
        updateOtaControlPause();
    }

    private boolean selectedOtaControlsPaused()
    {
        OtaProgress progress = selectedOtaProgress();
        return progress != null && progress.controlsPaused;
    }

    private void updateOtaControlPause()
    {
        boolean paused = selectedOtaControlsPaused();
        boolean connected = mStatusViewModel != null
            && mStatusViewModel.getConnectionState() == StatusViewModel.CONNECTION_STATE_CONNECTED;
        String stateKey = observedSessionId + ":" + connected + ":" + paused;
        if (stateKey.equals(appliedOtaControlState))
        {
            return;
        }
        appliedOtaControlState = stateKey;
        for (View button : new View[]{mRemoteControlBinding.otaProgramDownButton, mRemoteControlBinding.otaProgramUpButton,
            mRemoteControlBinding.otaVolumeDownButton, mRemoteControlBinding.otaVolumeUpButton,
            mRemoteControlBinding.otaMaxOutputDownButton, mRemoteControlBinding.otaMaxOutputUpButton,
            mRemoteControlBinding.otaLedButton, mRemoteControlBinding.otaAlarmButton})
        {
            setOtaActionEnabled(button, connected && !paused);
        }
        applyFwRelease();
        updateFfControls();
    }

    private void updateConnectionOverlay()
    {
        if (mRemoteControlBinding == null || mStatusViewModel == null)
        {
            return;
        }
        boolean connected = mStatusViewModel.getConnectionState() == StatusViewModel.CONNECTION_STATE_CONNECTED;
        boolean visible = !connected && selectedControlTab != 3;
        mRemoteControlBinding.remoteControlConnectionLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible)
        {
            mRemoteControlBinding.remoteControlConnectionGlass.stopWaves();
            return;
        }

        boolean pending = Boolean.TRUE.equals(mMainActivity.getDeviceScanState().getValue());
        List<ConnectionInfo> states = mMainActivity.getConnectionStates().getValue();
        if (states != null)
        {
            for (ConnectionInfo state : states)
            {
                pending |= state.state == ConnectionInfo.STATE_CONNECTING;
            }
        }
        mRemoteControlBinding.remoteControlConnectionTitle.setVisibility(View.GONE);
        mRemoteControlBinding.remoteControlFindLayout.setVisibility(View.VISIBLE);
        mRemoteControlBinding.remoteControlProgressbar.setVisibility(pending ? View.VISIBLE : View.GONE);
        mRemoteControlBinding.remoteControlConnectionGlass.startWaves();
    }

    private boolean hasActiveRegistration()
    {
        boolean activeUser = false;
        for (EntityUser user : UtilUser.instance.getUsers())
        {
            activeUser |= user.isActive;
        }
        boolean activeDevice = false;
        for (todoc.cochlear.remoteapp.database.devices.EntityDevice device : UtilDevice.instance.getDevices())
        {
            activeDevice |= device.isActive;
        }
        return activeUser && activeDevice;
    }

    private void setOtaText(TextView view, String text)
    {
        if (!text.contentEquals(view.getText()))
        {
            view.setText(text);
        }
    }

    private void setOtaActionEnabled(View view, boolean enabled)
    {
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.45f);
    }

    static void showControlTab(FragmentRemoteControlBinding binding, int selected)
    {
        binding.otaRemoteBox.setVisibility(selected == 0 ? View.VISIBLE : View.GONE);
        binding.otaEtcBox.setVisibility(selected == 1 ? View.VISIBLE : View.GONE);
        binding.ffTabBox.setVisibility(selected == 2 ? View.VISIBLE : View.GONE);
        binding.remoteStatusColumn.setVisibility(selected == 3 ? View.GONE : View.VISIBLE);
        binding.connectionTargetSelector.setVisibility(selected == 3 ? View.GONE : View.VISIBLE);
        binding.otaStatusInfoBox.setVisibility(View.VISIBLE);
        binding.otaFfStatusTextview.setVisibility(View.GONE);
        if (binding.controlTabs.getSelectedTabPosition() != (selected == 3 ? 4 : selected))
        {
            binding.controlTabs.selectTab(binding.controlTabs.getTabAt(selected == 3 ? 4 : selected));
        }
        binding.statusExpandableContent.setVisibility(View.VISIBLE);
        binding.otaExpandableContent.setVisibility(selected == 3 ? View.VISIBLE : View.GONE);
        // 각 탭의 읽던 위치는 개별 보존하여 다른 탭의 스크롤이 넘어오지 않게 한다.
    }

    private void renderBatteryTelemetry()
    {
        if (mRemoteControlBinding == null || mStatusViewModel == null || mMainActivity == null)
        {
            return;
        }
        boolean supported = mMainActivity.isBatteryTelemetrySupported();

        boolean connected = mStatusViewModel.getConnectionState() == StatusViewModel.CONNECTION_STATE_CONNECTED;
        mRemoteControlBinding.legacyBatteryTextview.setVisibility(View.GONE);
        mRemoteControlBinding.batteryTelemetryBox.setVisibility(View.VISIBLE);
        for (TextView metric : new TextView[]{mRemoteControlBinding.batteryVoltageTextview,
            mRemoteControlBinding.batteryCurrentTextview, mRemoteControlBinding.batteryPowerTextview})
        {
            ((View) metric.getParent()).setVisibility(supported ? View.VISIBLE : View.GONE);
        }
        if (!supported)
        {
            Integer percent = mStatusViewModel.getLiveDataBatteryLevel().getValue();
            setBatteryText(mRemoteControlBinding.batteryPercentTextview,
                connected && percent != null && percent >= 0 && percent <= 100 ? percent + "%" : "—");
            return;
        }
        BatteryTelemetry sample = mStatusViewModel.getBatteryTelemetry().getValue();
        String error = mStatusViewModel.getBatteryTelemetryError().getValue();
        boolean fresh = sample != null && SystemClock.elapsedRealtime() - mStatusViewModel.batteryReceivedAtMs <= BatteryTelemetry.RECEIVE_STALE_MS;
        setBatteryText(mRemoteControlBinding.batteryPercentTextview, connected && fresh ? sample.percent + "%" : "— %");
        setBatteryText(mRemoteControlBinding.batteryVoltageTextview, connected && fresh ? sample.voltageText() : "—");
        setBatteryText(mRemoteControlBinding.batteryCurrentTextview, connected && fresh && error == null ? (sample.averageCurrentUa == null ? "미지원" : sample.currentText()) : "—");
        setBatteryText(mRemoteControlBinding.batteryPowerTextview, connected && fresh && error == null ? (sample.averageCurrentUa == null ? "미지원" : sample.powerText()) : "—");

    }

    private void setBatteryText(TextView view, String text)
    {
        text = displayStatusValue(text);
        // 같은 글자를 1초마다 다시 설정해 스크롤/접근성 이벤트를 계속 만들지 않는다.
        if (!text.contentEquals(view.getText()))
        {
            view.setText(text);
        }
    }

    static private final int[] OTA_FILE_ORDER = {Ota.FILE_NUM_MFST, Ota.FILE_NUM_APP0, Ota.FILE_NUM_APP1, Ota.FILE_NUM_APP2};

    private void collectFiles(MainActivity host, Ota target, String folderName, String sessionId, int generation)
    {
        int found = 0;
        try
        {
            for (int fileNum : OTA_FILE_ORDER)
            {
                if (collectOneFile(target, folderName, fileNum, sessionId, generation))
                {
                    found++;
                }
            }
            final int foundCount = found;
            batteryUiHandler.post(() ->
            {
                if (!isCurrentCollection(target, sessionId, generation))
                {
                    return;
                }
                Toast.makeText(mMainActivity, "파일 읽기 완료 · " + foundCount + "/4개", Toast.LENGTH_SHORT).show();
                updateOtaTargetText();
            });
        }
        catch (SecurityException failure)
        {
            Log.w(TAG, "[OTA] 이미지 폴더 접근 실패", failure);
            batteryUiHandler.post(() ->
            {
                if (isCurrentCollection(target, sessionId, generation))
                {
                    Toast.makeText(mMainActivity, "OTA 폴더 접근 권한을 확인하세요.", Toast.LENGTH_SHORT).show();
                }
            });
        }
        finally
        {
            target.threadState = Ota.THREAD_STATE_IDLE;
            batteryUiHandler.post(() ->
            {
                host.notifyOtaFilesChanged(sessionId);
                if (isCurrentCollection(target, sessionId, generation))
                {
                    renderOtaProgress();
                }
            });
        }
    }

    private boolean isCurrentCollection(Ota target, String sessionId, int generation)
    {
        return mRemoteControlBinding != null && viewGeneration == generation && mOtaFiles == target;
    }

    /**
     * Worker results can update only the view and device session which started the read.
     */
    private void postCollectProgress(Ota target, Ota.OtaFile file, int fileNum, String sessionId, int generation)
    {
        final int size = file.collectSize;
        final int percent = file.collectPercent;
        batteryUiHandler.post(() ->
        {
            if (!isCurrentCollection(target, sessionId, generation))
            {
                return;
            }
            TextView sizeView;
            TextView percentView;
            switch (fileNum)
            {
                case Ota.FILE_NUM_MFST:
                    sizeView = mRemoteControlBinding.mfstCollectSizeTextview;
                    percentView = mRemoteControlBinding.mfstCollectPercentTextview;
                    break;
                case Ota.FILE_NUM_APP0:
                    sizeView = mRemoteControlBinding.app0CollectSizeTextview;
                    percentView = mRemoteControlBinding.app0CollectPercentTextview;
                    break;
                case Ota.FILE_NUM_APP1:
                    sizeView = mRemoteControlBinding.app1CollectSizeTextview;
                    percentView = mRemoteControlBinding.app1CollectPercentTextview;
                    break;
                case Ota.FILE_NUM_APP2:
                    sizeView = mRemoteControlBinding.app2CollectSizeTextview;
                    percentView = mRemoteControlBinding.app2CollectPercentTextview;
                    break;
                default:
                    return;
            }
            sizeView.setText(otaFileSize(size));
            percentView.setText(percent + "%");
        });
    }

    private boolean collectOneFile(Ota target, String folderName, int fileNum, String sessionId, int generation)
    {
        Ota.OtaFile otaFile = target.getFile(Ota.SLOT_NUM_COLLECT, fileNum);
        File file = new File(makePath(folderName, fileNum));
        otaFile.collectSize = 0;
        otaFile.collectPercent = 0;
        otaFile.writeSize = 0;
        otaFile.writePercent = 0;
        otaFile.totalBytes = 0;
        postCollectProgress(target, otaFile, fileNum, sessionId, generation);
        long length = file.length();
        if (!file.isFile() || length <= 0 || length > Integer.MAX_VALUE)
        {
            return false;
        }
        otaFile.totalBytes = (int) length;
        if (otaFile.buffer == null || otaFile.buffer.length < otaFile.totalBytes)
        {
            otaFile.buffer = new byte[otaFile.totalBytes];
        }
        try (FileInputStream input = new FileInputStream(file))
        {
            int lastPercent = -1;
            while (otaFile.collectSize < otaFile.totalBytes)
            {
                int count = input.read(otaFile.buffer, otaFile.collectSize,
                    Math.min(4096, otaFile.totalBytes - otaFile.collectSize));
                if (count < 0)
                {
                    break;
                }
                otaFile.collectSize += count;
                otaFile.collectPercent = (int) ((long) otaFile.collectSize * 100 / otaFile.totalBytes);
                if (otaFile.collectPercent != lastPercent)
                {
                    lastPercent = otaFile.collectPercent;
                    postCollectProgress(target, otaFile, fileNum, sessionId, generation);
                }
            }
            return otaFile.collectSize == otaFile.totalBytes;
        }
        catch (IOException | SecurityException failure)
        {
            otaFile.collectPercent = 0;
            postCollectProgress(target, otaFile, fileNum, sessionId, generation);
            Log.w(TAG, "[OTA] 이미지 파일 읽기 실패", failure);
            return false;
        }
    }

    // OTA preparation, ACK handling and sequential transfer belong to each device session.

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Func: Make path
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String makePath(String folderName, int fileNum)
    {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + Ota.BASE_FOLDER + folderName + "/" + Ota.getFileName(fileNum);
    }


    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// OnChecked - radio group
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* List of slots 는 이제 «쓸 곳» 만 정한다.
     *
     * 예전에는 이 번호가 읽을 폴더까지 정했다. 그래서 슬롯을 바꾸면 상태표도 그 슬롯의
     * 수집 결과로 갈아 끼워야 했다. 지금은 어느 폴더에서 읽었든 결과가 한 벌뿐이라
     * 슬롯을 바꿔도 상태표는 그대로다. Write 와 Select 만 이 번호를 쓴다. */
    RadioGroup.OnCheckedChangeListener onChecked_radioGroup = new RadioGroup.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId)
        {

            if (checkedId == mRemoteControlBinding.slot1RadioButton.getId())
            {
                mOta.currentSlotNum = Ota.SLOT_NUM_1;
            }
            else if (checkedId == mRemoteControlBinding.slot2RadioButton.getId())
            {
                mOta.currentSlotNum = Ota.SLOT_NUM_2;
            }

            Log.d(TAG, "[OTA] 쓸 슬롯 -> " + mOta.currentSlotNum);
            mMainActivity.notifyOtaFilesChanged(observedSessionId);
            updateOtaTargetText();
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - info button
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_infoButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            String sessionId = mMainActivity.getSelectedSessionId();
            if (sessionId == null || !mMainActivity.canPrepareOta(sessionId))
            {
                Toast.makeText(requireContext(), "장치 통신이 끝난 뒤 다시 눌러주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            /* 사운드처리기는 옵션 바이트만 읽지만, ci_ble_control_boot.h 가 부트 패킷을
             * 3바이트(RECV_PKT_SIZE_BOOT_INFO)로 정의하므로 길이를 맞춰 보낸다. */
            byte[] packet = new byte[PacketInfo.PACKET_SIZE_BOOT_SEND];

            packet[0] = PacketInfo.HEADER_BOOT_STATUS;
            packet[1] = (byte) PacketInfo.BOOT_OPTION_INFO;
            packet[2] = 0;

            mMainActivity.sendPacket(packet);
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - collect button
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_collectButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {

            /* 어느 폴더에서 읽을지 고르게 한다.
             *
             * 예전에는 List of slots 가 폴더까지 정했다. 그래서 슬롯 2 에 넣으려면 파일을
             * 슬롯 2 폴더로 옮겨야 했다. 이제 읽는 곳과 쓰는 곳을 갈라, 어느 폴더의 이미지든
             * 어느 슬롯에도 넣을 수 있다. */
            makeDialog_collectFolderSelect();
        } // onClick
    };// OnClickListener

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 수집할 폴더 선택 다이얼로그
    ///
    /// OTA 폴더 바로 아래의 폴더를 모두 보여 준다. 이름은 사용자가 정하는 것이라
    /// 슬롯 번호일 수도 있고 펌웨어 버전일 수도 있다. 앱은 가리지 않는다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_collectFolderSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[OTA] 이미 다이얼로그가 표시 중이라서 폴더 목록을 띄우지 않습니다.");
            return;
        }

        File baseDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + Ota.BASE_FOLDER);

        if (!baseDir.exists() && !baseDir.mkdirs())
        {
            Log.d(TAG, "[OTA] OTA 폴더를 만들지 못했습니다 : " + baseDir.getAbsolutePath());
        }

        File[] found = baseDir.listFiles(File::isDirectory);

        if (found == null || found.length == 0)
        {
            makeDialog_withMessage("OTA 폴더에 하위 폴더가 없습니다.\n\n" //
                + baseDir.getAbsolutePath() //
                + "\n\n이 아래에 폴더를 만들고 이미지를 넣어 주세요.");
            return;
        }

        // 이름 순으로 세워 두면 버전이 이름에 들어 있을 때 찾기 쉽다.
        final File[] folders = found;

        Arrays.sort(folders, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        String[] items = new String[folders.length];

        for (int i = 0; i < folders.length; i++)
        {
            items[i] = folders[i].getName() + "   (" + countOtaFiles(folders[i]) + "/4)";
        }

        int checkedIndex = -1;

        for (int i = 0; i < folders.length; i++)
        {
            if (folders[i].getName().equals(mOta.collectFolderName))
            {
                checkedIndex = i;
                break;
            }
        }

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setCustomTitle(makeDialogTitleView("OTA 파일 폴더 선택", //
                "괄호 안은 사용할 OTA 파일 수입니다.\n" //
                    + "고른 폴더의 네 파일을 함께 사용합니다.")) //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                startCollect(folders[which].getName());
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("취소", null) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    // 이 폴더에 OTA 파일이 몇 개나 있는지. 고르기 전에 빈 폴더를 가려낼 수 있게 한다.
    private int countOtaFiles(File folder)
    {
        int count = 0;

        for (int fileNum : OTA_FILE_ORDER)
        {
            if (new File(folder, Ota.getFileName(fileNum)).exists())
            {
                count++;
            }
        }

        return count;
    }

    // 고른 폴더에서 수집을 시작한다.
    private void startCollect(String folderName)
    {
        final Ota target = mOtaFiles;
        if (target.threadState == Ota.THREAD_STATE_BUSY)
        {
            return;
        }
        target.threadState = Ota.THREAD_STATE_BUSY;
        target.collectFolderName = folderName;
        for (int fileNum : OTA_FILE_ORDER)
        {
            target.getFile(Ota.SLOT_NUM_COLLECT, fileNum);
        }
        final String sessionId = observedSessionId;
        final int generation = viewGeneration;
        final MainActivity host = mMainActivity;
        updateCollectFolderText();
        host.notifyOtaFilesChanged(sessionId);
        updateOtaTargetText();
        new Thread(() -> collectFiles(host, target, folderName, sessionId, generation), "ota-collect").start();
    }

    // 지금 수집한 폴더를 제목 옆에 보여 준다.
    private void updateCollectFolderText()
    {
        String name = mOtaFiles.collectFolderName;
        String text = (name == null || name.isEmpty()) //
            ? "선택된 폴더 없음" //
            : (Ota.BASE_FOLDER.replace("/", "") + "/" + name);

        mRemoteControlBinding.otaFolderTextview.setText(text);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - write button
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_writeButton = view -> showOtaSlotDialog(true);

    View.OnClickListener onClick_selectButton = view -> showOtaSlotDialog(false);

    private void showOtaSlotDialog(boolean transfer)
    {
        if (mDialog != null)
        {
            return;
        }
        final String sessionId = mMainActivity.getSelectedSessionId();
        if (sessionId == null || !Objects.equals(sessionId, observedSessionId))
        {
            return;
        }
        final Ota target = mOta;
        todoc.cochlear.remoteapp.connection.OtaDeviceInfo info = mMainActivity.getSessionOtaDeviceInfo(sessionId);
        final Integer currentSlot = info == null ? null : info.currentBootSlot;
        if (currentSlot == null)
        {
            Toast.makeText(requireContext(), "먼저 슬롯 정보 읽기를 눌러주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        final int[] slots = transfer ? new int[]{Ota.SLOT_NUM_1, Ota.SLOT_NUM_2}
            : new int[]{Ota.SLOT_NUM_1, Ota.SLOT_NUM_2, 0xFF};
        final String[] labels = transfer ? new String[]{"슬롯 1", "슬롯 2"}
            : new String[]{"슬롯 1", "슬롯 2", "공장 펌웨어 (FF)"};
        for (int index = 0; index < slots.length; index++)
        {
            if (currentSlot != null && slots[index] == currentSlot)
            {
                labels[index] += " · 현재 사용 중";
            }
        }
        android.widget.ArrayAdapter<String> choices = new android.widget.ArrayAdapter<String>(requireContext(),
            android.R.layout.simple_list_item_single_choice, labels)
        {
            @Override
            public boolean areAllItemsEnabled()
            {
                return false;
            }

            @Override
            public boolean isEnabled(int position)
            {
                return currentSlot == null || slots[position] != currentSlot;
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View recycled, @NonNull ViewGroup parent)
            {
                View row = super.getView(position, recycled, parent);
                boolean enabled = isEnabled(position);
                row.setEnabled(enabled);
                row.setAlpha(1f);
                android.widget.CheckedTextView label = (android.widget.CheckedTextView) row;
                label.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.app_sans));
                label.setTextColor(new android.content.res.ColorStateList(
                    new int[][]{new int[]{-android.R.attr.state_enabled}, new int[0]},
                    new int[]{0xff9daabc, 0xffedf0f8}));
                label.setCheckMarkTintList(new android.content.res.ColorStateList(
                    new int[][]{new int[]{-android.R.attr.state_enabled}, new int[]{android.R.attr.state_checked}, new int[0]},
                    new int[]{0xff8491a3, 0xffbec8ff, 0xffa7b5c8}));
                return row;
            }
        };
        final int[] selected = {-1};
        String title = transfer ? "OTA 기록 슬롯 선택" : "부팅할 슬롯 선택";
        String description = transfer ? "선택한 장치에 네 파일을 기록합니다." : "선택한 펌웨어로 장치를 재부팅합니다.";
        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext())
            .setCustomTitle(makeDialogTitleView(title, description))
            .setSingleChoiceItems(choices, -1, (dialog, which) ->
            {
                selected[0] = which;
                if (mDialog != null)
                {
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            })
            .setNegativeButton("취소", null)
            .setPositiveButton(transfer ? "OTA 진행" : "재부팅", (dialog, which) ->
            {
                if (selected[0] < 0 || !Objects.equals(sessionId, mMainActivity.getSelectedSessionId())
                    || target != mOta)
                {
                    return;
                }
                int slot = slots[selected[0]];
                todoc.cochlear.remoteapp.connection.OtaDeviceInfo latest = mMainActivity.getSessionOtaDeviceInfo(sessionId);
                if (latest == null || latest.currentBootSlot == null)
                {
                    Toast.makeText(requireContext(), "슬롯 정보를 다시 읽어주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (slot == latest.currentBootSlot)
                {
                    Toast.makeText(requireContext(), "현재 사용 중인 슬롯은 선택할 수 없습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (transfer)
                {
                    if (!mMainActivity.applyOtaFileSelection(sessionId))
                    {
                        Toast.makeText(requireContext(), "파일 4개와 장치 준비 상태를 확인하세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    target.currentSlotNum = slot;
                    mMainActivity.notifyOtaFilesChanged(sessionId);
                    if (!mMainActivity.startOta(sessionId))
                    {
                        Toast.makeText(requireContext(), "장치 통신이 끝난 뒤 OTA 진행을 다시 눌러주세요.", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    if (!mMainActivity.canPrepareOta(sessionId))
                    {
                        Toast.makeText(requireContext(), "장치 통신이 끝난 뒤 슬롯 선택하기를 다시 눌러주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    byte[] packet = new byte[PacketInfo.PACKET_SIZE_BOOT_SEND];
                    packet[0] = PacketInfo.HEADER_BOOT_STATUS;
                    packet[1] = (byte) PacketInfo.BOOT_OPTION_SELECT;
                    packet[2] = (byte) slot;
                    mMainActivity.sendPacket(packet);
                }
                updateOtaTargetText();
            })
            .create();
        mDialog.setOnDismissListener(dialog -> mDialog = null);
        showControlDialog(mDialog);
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - factoryReset button
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_factoryResetButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            /* 슬롯 0xFF 는 공장 초기화 이미지를 뜻한다.
             * 사운드처리기 _fetch_packet_boot_select() 가 슬롯 범위(1~2) 밖이어도 0xFF 만은 허용한다. */
            byte[] packet = new byte[PacketInfo.PACKET_SIZE_BOOT_SEND];

            packet[0] = PacketInfo.HEADER_BOOT_STATUS;
            packet[1] = (byte) PacketInfo.BOOT_OPTION_SELECT;
            packet[2] = (byte) 0xFF;

            new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext())
                .setTitle("공장 펌웨어 부팅")
                .setMessage("공장 이미지로 전환하고 장치를 재부팅합니다. 맵·사용자 공장 복원과는 별개입니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("진행", (dialog, which) -> mMainActivity.sendPacket(packet))
                .show();
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - map init 버튼 (기본 맵 / MRI 32채널 / MRI 16채널)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_mapInitDefaultButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            makeDialog_mapInitConfirm(PacketInfo.MAP_INIT_TYPE_DEFAULT, PacketInfo.MAP_INIT_NAME_DEFAULT);
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - Map 버튼 (초기화할 맵 종류를 선택)
    ///
    /// 목록에서 고른 뒤, 되돌릴 수 없는 동작이므로 확인 다이얼로그를 한 번 더 거친다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_mapSelectButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            makeDialog_mapSelect();
        }
    };

    private void makeDialog_mapSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[MAP INIT] 이미 다이얼로그가 표시 중이라서 선택 목록을 띄우지 않습니다.");
            return;
        }

        final int[] types = {PacketInfo.MAP_INIT_TYPE_MRI_32CH, PacketInfo.MAP_INIT_TYPE_MRI_16CH};
        final String[] names = {PacketInfo.MAP_INIT_NAME_MRI_32CH, PacketInfo.MAP_INIT_NAME_MRI_16CH};
        final String[] items = {"32Ch (MRI 32채널 맵)", "16Ch (MRI 16채널 맵)"};

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setTitle("맵 초기화 종류 선택") //
            .setSingleChoiceItems(items, -1, (dialogInterface, which) ->
            {
                final int type = types[which];
                final String name = names[which];

                dialogInterface.dismiss();

                /* 위 다이얼로그가 닫히면서 mDialog 를 비운 뒤에 확인 다이얼로그를 띄워야 한다.
                 * 같은 자리에서 바로 호출하면 mDialog 가 아직 남아 있어 무시된다. */
                new Handler(Looper.getMainLooper()).post(() -> makeDialog_mapInitConfirm(type, name));
            }) //
            .setNegativeButton("취소", null) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - 프로그램(맵번호) 이전 / 다음 버튼
    ///
    /// 응답 패킷(0x44)은 MainActivity 가 받아 뷰모델에 반영하고,
    /// 화면 표시는 liveDataProgramAndLevels() 의 옵저버가 담당한다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_programDownButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Log.d(TAG, "[PROGRAM] 이전 프로그램 요청");

            mMainActivity.sendPacket(mMainActivity.packetMaker(PacketInfo.HEADER_VALUE_PROMGRAM, new byte[]{PacketInfo.PROGRAM_DOWN}, PacketInfo.PACKET_SIZE_PROGRAM));
        }
    };

    View.OnClickListener onClick_programUpButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Log.d(TAG, "[PROGRAM] 다음 프로그램 요청");

            mMainActivity.sendPacket(mMainActivity.packetMaker(PacketInfo.HEADER_VALUE_PROMGRAM, new byte[]{PacketInfo.PROGRAM_UP}, PacketInfo.PACKET_SIZE_PROGRAM));
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - 볼륨 / 최대출력 증감 버튼
    ///
    /// 사운드처리기가 단계 조정을 처리하고 결과 값을 응답으로 돌려준다.
    /// 화면 표시는 MainActivity 가 뷰모델에 반영한 값을 옵저버가 받아 갱신한다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_volumeDownButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            sendValuePacket(PacketInfo.HEADER_VALUE_VOLUME, PacketInfo.VOLUME_DOWN, PacketInfo.PACKET_SIZE_VOLUME, "[VOLUME] 볼륨 내리기 요청");
        }
    };

    View.OnClickListener onClick_volumeUpButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            sendValuePacket(PacketInfo.HEADER_VALUE_VOLUME, PacketInfo.VOLUME_UP, PacketInfo.PACKET_SIZE_VOLUME, "[VOLUME] 볼륨 올리기 요청");
        }
    };

    View.OnClickListener onClick_maxOutputDownButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            sendValuePacket(PacketInfo.HEADER_VALUE_MAX_OUTPUT, PacketInfo.MAX_OUTPUT_DOWN, PacketInfo.PACKET_SIZE_MAX_OUTPUT, "[MAX OUT] 최대출력 내리기 요청");
        }
    };

    View.OnClickListener onClick_maxOutputUpButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            sendValuePacket(PacketInfo.HEADER_VALUE_MAX_OUTPUT, PacketInfo.MAX_OUTPUT_UP, PacketInfo.PACKET_SIZE_MAX_OUTPUT, "[MAX OUT] 최대출력 올리기 요청");
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - LED / 자극알림 토글 버튼
    ///
    /// 현재 상태의 반대 값을 보낸다. 상태를 아직 못 읽었으면 켜기부터 시도한다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_ledButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            byte value = (mStatusViewModel.getValueLed() == PacketInfo.LED_ON) ? PacketInfo.LED_OFF : PacketInfo.LED_ON;

            sendValuePacket(PacketInfo.HEADER_VALUE_LED, value, PacketInfo.PACKET_SIZE_LED, "[LED] LED " + ((value == PacketInfo.LED_ON) ? "켜기" : "끄기") + " 요청");
        }
    };

    View.OnClickListener onClick_alarmButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            byte value = (mStatusViewModel.getValueNotification() == PacketInfo.NOTIFICATION_ON) ? PacketInfo.NOTIFICATION_OFF : PacketInfo.NOTIFICATION_ON;

            sendValuePacket(PacketInfo.HEADER_VALUE_NOTIFICATION, value, PacketInfo.PACKET_SIZE_NOTIFICATION, "[ALARM] 자극알림 " + ((value == PacketInfo.NOTIFICATION_ON) ? "켜기" : "끄기") + " 요청");
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 값 조정 패킷 전송 ([헤더,값] 2바이트 공통 형식)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendValuePacket(byte header, byte value, int packetSize, String logMessage)
    {
        Log.d(TAG, logMessage);

        mMainActivity.sendPacket(mMainActivity.packetMaker(header, new byte[]{value}, packetSize));
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - Gating 토글 버튼 (자극 묵음 처리)
    ///
    /// 현재 상태의 반대 값을 보낸다. 상태를 아직 못 읽었으면 활성화부터 시도한다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_gatingButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            int state = (mStatusViewModel.getValueGatingState() == PacketInfo.MUTE_ENABLE) ? PacketInfo.MUTE_DISABLE : PacketInfo.MUTE_ENABLE;

            sendGatingPacket(state);
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - PMIC Select 버튼 (링크 Tx 파워 하한을 목록에서 직접 선택)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_pmicSelectButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            makeDialog_pmicSelect();
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 링크 Tx 파워 하한 선택 다이얼로그
    ///
    /// 25mV 단위로 1.800V ~ 5.325V 목록을 만들고, 고르는 즉시 그 값으로 설정한다.
    /// 현재 값이 있으면 미리 선택해 두어 목록이 그 위치로 스크롤되게 한다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_pmicSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[PMIC] 이미 다이얼로그가 표시 중이라서 선택 목록을 띄우지 않습니다.");
            return;
        }

        final int minLevel = PacketInfo.MIN_TX_PWR_SELECT_MIN;
        final int maxLevel = makeMinTxPowerSelectMax();
        final int count = (maxLevel - minLevel) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int level = minLevel + i;
            items[i] = String.format("%.3f Ｖ  (레벨 %d)", (level * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f, level);
        }

        // 현재 값을 미리 선택해 둔다. 아직 못 읽었으면 기본값 위치를 가리킨다.
        int currentLevel = mStatusViewModel.getValueMinTxPowerLevel();

        if (currentLevel < minLevel || maxLevel < currentLevel)
        {
            currentLevel = PacketInfo.MIN_TX_PWR_DEFAULT;
        }

        // 상한이 낮아져 목록이 짧아졌으면 현재 값이 목록 밖일 수 있다. 인덱스를 범위로 묶는다.
        final int checkedIndex = Math.max(0, Math.min(count - 1, currentLevel - minLevel));

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setTitle("PMIC 하한 선택") //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                sendMinTxPowerPacket(minLevel + which);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("기본값", (dialogInterface, i) -> sendMinTxPowerPacket(PacketInfo.MIN_TX_PWR_DEFAULT)) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - BT PMIC 버튼 (매핑 전용 Tx 파워 하한 선택)
    ///
    /// 매핑 중에는 임피던스나 ECAP 측정이 안정된 전력에서 이뤄져야 하므로,
    /// 상시 동작(Normal PMIC) 하한과 별도로 둔다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_mappingPmicButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            makeDialog_mappingTxPowerSelect();
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - Link Step 버튼 (링크 Tx 파워 상승 스텝 선택)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_linkStepButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            makeDialog_txStepUpSelect();
        }
    };

    View.OnClickListener onClick_nopStandbyButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            /* 목록은 «지금» 패킷 수로 만들어야 한다. 맵이 바뀌면 패킷 수가 달라지고
             * 쓸 수 있는 값도 통째로 바뀌기 때문이다. 그래서 먼저 읽고, 응답을 받은
             * 자리에서 목록을 만든다. */
            if (!mMainActivity.requestLinkParamsForNopDialog())
            {
                makeDialog_nopStandbySelect();
            }
        }
    };

    // 링크 파라미터를 다시 읽어 온 자리. 액티비티가 부른다.
    public void onLinkParamsRefreshedForNop()
    {
        makeDialog_nopStandbySelect();
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 매핑 전용 Tx 파워 하한 선택 다이얼로그
    ///
    /// 허용 범위는 상시 동작 하한과 같다. (사운드처리기가 두 값의 검사를 공유한다)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_mappingTxPowerSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[PMIC] 이미 다이얼로그가 표시 중이라서 매핑 선택 목록을 띄우지 않습니다.");
            return;
        }

        final int minLevel = PacketInfo.MIN_TX_PWR_SELECT_MIN;
        final int maxLevel = makeMinTxPowerSelectMax();
        final int count = (maxLevel - minLevel) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int level = minLevel + i;
            items[i] = String.format("%.3f Ｖ  (레벨 %d)", (level * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f, level);
        }

        int currentLevel = mStatusViewModel.getValueMappingTxPowerLevel();

        if (currentLevel < minLevel || maxLevel < currentLevel)
        {
            currentLevel = PacketInfo.MAPPING_TX_PWR_DEFAULT;
        }

        // 상한이 낮아져 목록이 짧아졌으면 현재 값이 목록 밖일 수 있다. 인덱스를 범위로 묶는다.
        final int checkedIndex = Math.max(0, Math.min(count - 1, currentLevel - minLevel));

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setTitle("매핑 PMIC 하한 선택") //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                sendMappingTxPowerPacket(minLevel + which);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("기본값", (dialogInterface, i) -> sendMappingTxPowerPacket(PacketInfo.MAPPING_TX_PWR_DEFAULT)) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 매핑 전용 Tx 파워 하한 설정 패킷 전송
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendMappingTxPowerPacket(int level)
    {
        mMainActivity.sendLinkParam(PacketInfo.RC_LINK_IDX_MAPPING_MIN_POWER, level);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - Fixed PMIC 버튼 (모드와 무관하게 PMIC 레벨 고정)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_fixedPmicButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            makeDialog_fixedTxPowerSelect();
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - Coin 버튼 (백텔 재시도 모드 선택)
    ///
    /// 사운드처리기에는 one coin 과 infinite coin 두 값이 있고 infinite 가 우선한다.
    /// 화면에서는 그 조합을 세 모드로 정리해 하나만 고르게 한다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - Ctrl Mode 토글 버튼 (링크 제어 모드)
    ///
    /// 0 = ISD (내부기 전원 상태로 판정), 1 = BT (백텔 수신 여부로 판정).
    /// 현재 상태의 반대 값을 보낸다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_ctrlModeButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            int value = (mStatusViewModel.getValueLinkCtrlMode() == PacketInfo.LINK_CTRL_MODE_BACKTEL) //
                ? PacketInfo.LINK_CTRL_MODE_POWER_STATE //
                : PacketInfo.LINK_CTRL_MODE_BACKTEL;

            mMainActivity.sendLinkParam(PacketInfo.RC_LINK_IDX_CTRL_MODE, value);
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - Accel 토글 버튼 (Tx 파워 상승 가속)
    ///
    /// 연속 미수신 시 상승 폭을 키운다. 현재 상태의 반대 값을 보낸다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_accelButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            int value = (mStatusViewModel.getValueTxPowerAccel() == PacketInfo.LINK_FLAG_ENABLE) //
                ? PacketInfo.LINK_FLAG_DISABLE //
                : PacketInfo.LINK_FLAG_ENABLE;

            mMainActivity.sendLinkParam(PacketInfo.RC_LINK_IDX_TX_POWER_ACCEL, value);
        }
    };

    View.OnClickListener onClick_oneCoinButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            /* 프로토콜 4.4 부터 코인이 «개수» 다. 그 전에는 켜고 끄는 스위치였다.
             * 목록의 성격이 아예 달라 다이얼로그를 따로 둔다. */
            if (mMainActivity.isCoinCountSupported())
            {
                makeDialog_coinCountSelect();
                return;
            }

            makeDialog_coinModeSelect();
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 백텔 재시도 코인 «개수» 선택 다이얼로그 (프로토콜 4.4 이상)
    ///
    /// 규격서가 정한 순서 그대로 무한 · 비활성 · 1 · 2 · … · 100 을 낸다.
    /// 102 줄이라 길지만 단일 선택 목록이 스크롤되고 현재 값에 체크가 붙어 그 자리로 열린다.
    /// 구간을 줄이면 규격과 어긋나고 시험 자유도가 깎인다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_coinCountSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[LINK] 이미 다이얼로그가 표시 중이라서 코인 개수 목록을 띄우지 않습니다.");
            return;
        }

        /* 표시 문자열과 보낼 값을 «한 반복에서 함께» 채운다.
         * 따로 만들면 하나가 어긋났을 때 엉뚱한 값이 나가는데 화면으로는 알 수 없다. */
        final int count = 2 + (PacketInfo.COIN_COUNT_MAX - PacketInfo.COIN_COUNT_MIN + 1);
        final int[] values = new int[count];
        final String[] items = new String[count];

        values[0] = PacketInfo.COIN_COUNT_INFINITE;
        items[0] = "Infinite " + COIN_INFINITE_MARK + "  (끊김 판정 안 함 · 저장 안 됨)";

        values[1] = PacketInfo.COIN_COUNT_DISABLE;
        items[1] = "Disable  (첫 실패에서 바로 끊김)";

        for (int n = PacketInfo.COIN_COUNT_MIN; n <= PacketInfo.COIN_COUNT_MAX; n++)
        {
            int at = 1 + n;

            values[at] = n;
            items[at] = n + makeCoinHoldTimeText(n);
        }

        final int checkedIndex = getCurrentCoinCountIndex();

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setCustomTitle(makeDialogTitleView("백텔 재시도 코인", makeCoinDialogMessage())) //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                mMainActivity.sendCoinCount(values[which]);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("취소", null) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /* 개수를 «견디는 시간» 으로 환산한다. 백텔 주기(인덱스 2)는 100msec 단위다.
     *
     * 주기를 아직 못 읽었으면 아무것도 붙이지 않는다. 모르는 값으로 계산한 그럴듯한
     * 숫자를 보여 주는 것이 가장 나쁘다. */
    private String makeCoinHoldTimeText(int coinCount)
    {
        int period = mStatusViewModel.getValueBacktelPeriod();

        if (period == PacketInfo.LINK_VALUE_UNKNOWN || period <= 0)
        {
            return "";
        }

        float seconds = (coinCount * period * PacketInfo.BACKTEL_PERIOD_UNIT_MS) / 1000.0f;

        return (seconds < 10.0f) //
            ? String.format("  (약 %.1f초)", seconds) //
            : String.format("  (약 %.0f초)", seconds);
    }

    /* 다이얼로그 위에 붙이는 안내. 지금 상태에서 알아야 할 것만 담는다. */
    private String makeCoinDialogMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("무한은 저장되지 않습니다. 전원을 껐다 켜면 직전 개수로 돌아갑니다.");

        /* 백텔 수신 기준 모드에는 코인 소모 분기가 없어 개수가 쓰이지 않는다.
         * 다만 무한은 끊김 판정 자체를 막는 것이라 이 모드에서도 유효하다. */
        if (!mMainActivity.isCoinCountEffective())
        {
            sb.append(System.lineSeparator()).append(System.lineSeparator());

            sb.append(mMainActivity.isLinkFrozen() //
                ? "이 맵에서는 백텔이 나가지 않아 코인이 쓰이지 않습니다." //
                : "백텔 수신 기준 모드입니다 — 개수는 쓰이지 않고 무한만 유효합니다.");
        }

        return sb.toString();
    }

    /* 지금 값이 목록의 몇 번째인가. 못 읽었으면 아무것도 선택하지 않는다. */
    private int getCurrentCoinCountIndex()
    {
        if (mMainActivity.isCoinInfinite())
        {
            return 0;
        }

        int coin = mStatusViewModel.getValueOneCoin();

        if (coin == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            return -1;
        }

        if (coin == PacketInfo.COIN_COUNT_DISABLE)
        {
            return 1;
        }

        if (coin < PacketInfo.COIN_COUNT_MIN || PacketInfo.COIN_COUNT_MAX < coin)
        {
            return -1; // 규격 밖의 값. 골라 준 것처럼 보이면 안 된다
        }

        return 1 + coin;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 백텔 재시도 모드 선택 다이얼로그
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_coinModeSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[LINK] 이미 다이얼로그가 표시 중이라서 코인 모드 목록을 띄우지 않습니다.");
            return;
        }

        /* infinite coin 은 one coin 보다 뒤에 생겼다. 그것을 모르는 펌웨어에서는
         * 목록에 아예 올리지 않는다. 골라 봐야 거절당할 선택지를 보여 주면
         * 눌러 보고 나서야 안 된다는 것을 알게 된다. */
        boolean hasInfinite = mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_INFINITE_COIN);

        final int[] modes = hasInfinite //
            ? new int[]{PacketInfo.COIN_MODE_DISABLE, PacketInfo.COIN_MODE_ENABLE, PacketInfo.COIN_MODE_INFINITE} //
            : new int[]{PacketInfo.COIN_MODE_DISABLE, PacketInfo.COIN_MODE_ENABLE};

        String[] items = hasInfinite //
            ? new String[]{ //
            "Disable  (첫 실패에서 바로 끊김)", //
            "Enable  (1회 실패까지 견딤)", //
            "Infinite " + COIN_INFINITE_MARK + "  (끊김 판정 안 함)" //
        } //
            : new String[]{ //
            "Disable  (첫 실패에서 바로 끊김)", //
            "Enable  (1회 실패까지 견딤)" //
        };

        int checkedIndexValue = getCurrentCoinModeIndex();

        // 목록에서 빠진 항목이 선택 상태로 남지 않게 한다.
        if (checkedIndexValue >= modes.length)
        {
            checkedIndexValue = -1;
        }

        final int checkedIndex = checkedIndexValue;

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setTitle("백텔 재시도 모드") //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                mMainActivity.sendCoinMode(modes[which]);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("취소", null) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    // 현재 코인 모드를 목록 위치로 바꾼다. 아직 못 읽었으면 아무것도 선택하지 않는다.
    private int getCurrentCoinModeIndex()
    {
        int infinite = mStatusViewModel.getValueInfiniteCoin();
        int oneCoin = mStatusViewModel.getValueOneCoin();

        if (infinite == PacketInfo.LINK_FLAG_ENABLE)
        {
            return PacketInfo.COIN_MODE_INFINITE;
        }

        if (infinite == PacketInfo.LINK_VALUE_UNKNOWN || oneCoin == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            return -1; // 선택 없음
        }

        return (oneCoin == PacketInfo.LINK_FLAG_ENABLE) ? PacketInfo.COIN_MODE_ENABLE : PacketInfo.COIN_MODE_DISABLE;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - Max PMIC 버튼 (링크 제어가 올릴 수 있는 상한, 프로토콜 4.6 이상)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private final View.OnClickListener onClick_maxPmicButton = view -> makeDialog_maxTxPowerSelect();

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - PMIC 상한 선택 다이얼로그
    ///
    /// 하한 3형제와 형태는 같지만 «목록의 시작점이 상수가 아니다». 펌웨어가 받는 값이
    /// (상시 하한, 매핑 하한 중 큰 값) + 1 이상이고, 두 하한은 사용자가 바꾼다.
    /// 그래서 열 때마다 다시 계산한다. 시작점 미만은 아예 목록에 넣지 않는다 —
    /// 넣으면 사운드처리기가 거부할 값을 고르게 하는 것이다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_maxTxPowerSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[PMIC] 이미 다이얼로그가 표시 중이라서 상한 목록을 띄우지 않습니다.");
            return;
        }

        int floor = PacketInfo.makeMaxTxPowerFloor(mStatusViewModel.getValueMinTxPowerLevel(), //
            mStatusViewModel.getValueMappingTxPowerLevel());

        if (floor == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            Toast.makeText(requireContext(), "하한 값을 아직 읽지 못해 상한 범위를 계산할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        final int minLevel = floor + 1;
        final int maxLevel = PacketInfo.MAX_TX_PWR_SELECT_MAX;

        if (maxLevel < minLevel)
        {
            Toast.makeText(requireContext(), "하한이 너무 높아 상한으로 고를 수 있는 값이 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        final int count = (maxLevel - minLevel) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int level = minLevel + i;
            items[i] = String.format("%.3f Ｖ  (레벨 %d)", (level * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f, level);
        }

        int currentLevel = mStatusViewModel.getValueMaxTxPowerLevel();

        if (currentLevel < minLevel || maxLevel < currentLevel)
        {
            currentLevel = PacketInfo.MAX_TX_PWR_DEFAULT;
        }

        // 기본값(214)조차 범위 밖일 수는 없지만, 계산이 바뀌어도 인덱스가 음수가 되지 않게 묶어 둔다.
        final int checkedIndex = Math.max(0, Math.min(count - 1, currentLevel - minLevel));

        int mappingLevel = mStatusViewModel.getValueMappingTxPowerLevel();
        String who = (mappingLevel > mStatusViewModel.getValueMinTxPowerLevel()) ? "매핑 하한" : "상시 하한";

        /* setMessage 와 setSingleChoiceItems 를 같이 주면 안 된다.
         * 둘이 같은 자리를 쓰는 탓에 메시지가 목록을 밀어내 «고를 것이 없는» 화면이 된다.
         * Nop 선택 다이얼로그가 같은 이유로 setCustomTitle 을 쓴다 — 그 방식을 따른다. */
        String guide = who + " " + floor + " 때문에 " + minLevel + " 이상만 고를 수 있습니다." //
            + System.lineSeparator() //
            + "지금보다 낮추면 현재 파워는 바로 내려가지 않습니다 - 올라가는 것만 막힙니다.";

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setCustomTitle(makeDialogTitleView("PMIC 상한 선택", guide)) //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                sendMaxTxPowerPacket(minLevel + which);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("기본값", (dialogInterface, i) -> sendMaxTxPowerPacket(PacketInfo.MAX_TX_PWR_DEFAULT)) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /* 하한 다이얼로그가 고를 수 있는 최댓값.
     *
     * 펌웨어는 «하한 > 상한» 조합을 막지 않는다(isd_interface.c 의 tdc_is_valid_min_tx_power_level
     * 이 상한을 보지 않는다). 그래서 앱이 막는다 — 상한을 180 으로 낮춰 둔 채 하한 210 을 쓰면
     * 성립하지 않는 조합이 파일에 남는다.
     *
     * 상한을 아직 못 읽었거나 4.6 미만이면 기존 상한(213)을 그대로 쓴다.
     * 여기서 막아 버리면 4.3·4.4 에서 되던 조작이 안 되는 퇴행이 된다. */
    private int makeMinTxPowerSelectMax()
    {
        int maxLevel = mStatusViewModel.getValueMaxTxPowerLevel();

        if (mMainActivity == null || !mMainActivity.isMaxTxPowerSupported() //
            || maxLevel == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            return PacketInfo.MIN_TX_PWR_SELECT_MAX;
        }

        return Math.min(PacketInfo.MIN_TX_PWR_SELECT_MAX, maxLevel - 1);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - FF 버튼 (자극 레벨 피드포워드, 프로토콜 4.6 이상)
    ///
    /// 설정이 7개라 버튼 하나로 묶었다. 여는 순간 관측값(24·25·26)을 읽어 제목에 얹는다 —
    /// 계속 변하는 값이라 미리 읽어 두는 것이 의미가 없고, 상시 폴링은 백텔에 영향을 줄 수 있다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private final View.OnClickListener onClick_ffButton = view ->
    {
        makeDialog_ffEnable();
    };

    /* 피드포워드 항목 하나. 인덱스·이름·범위·기본값을 묶어 목록과 값 선택이 같은 것을 보게 한다. */
    private static final class FfItem
    {
        final int index;
        final String name;
        final int min;
        final int max;
        final int def;
        final String unit;

        FfItem(int index, String name, int min, int max, int def, String unit)
        {
            this.index = index;
            this.name = name;
            this.min = min;
            this.max = max;
            this.def = def;
            this.unit = unit;
        }
    }

    private FfItem[] makeFfItems()
    {
        return new FfItem[]{ //
            new FfItem(PacketInfo.RC_LINK_IDX_FF_COOLDOWN, "쿨다운", //
                PacketInfo.FF_COOLDOWN_MIN, PacketInfo.FF_COOLDOWN_MAX, PacketInfo.FF_COOLDOWN_DEFAULT, " ms"), //
            new FfItem(PacketInfo.RC_LINK_IDX_FF_STEP, "상승 스텝", //
                PacketInfo.FF_STEP_MIN, PacketInfo.FF_STEP_MAX, PacketInfo.FF_STEP_DEFAULT, ""), //
            new FfItem(PacketInfo.RC_LINK_IDX_FF_RATIO_0, "비율 구간 0 (평균 0~63)", //
                PacketInfo.FF_RATIO_MIN, PacketInfo.FF_RATIO_MAX, PacketInfo.FF_RATIO_DEFAULT[0], " %"), //
            new FfItem(PacketInfo.RC_LINK_IDX_FF_RATIO_1, "비율 구간 1 (64~127)", //
                PacketInfo.FF_RATIO_MIN, PacketInfo.FF_RATIO_MAX, PacketInfo.FF_RATIO_DEFAULT[1], " %"), //
            new FfItem(PacketInfo.RC_LINK_IDX_FF_RATIO_2, "비율 구간 2 (128~191)", //
                PacketInfo.FF_RATIO_MIN, PacketInfo.FF_RATIO_MAX, PacketInfo.FF_RATIO_DEFAULT[2], " %"), //
            new FfItem(PacketInfo.RC_LINK_IDX_FF_RATIO_3, "비율 구간 3 (192~255)", //
                PacketInfo.FF_RATIO_MIN, PacketInfo.FF_RATIO_MAX, PacketInfo.FF_RATIO_DEFAULT[3], " %")};
    }

    private int getFfValue(int index)
    {
        switch (index)
        {
            case PacketInfo.RC_LINK_IDX_FF_COOLDOWN:
                return mStatusViewModel.getValueFfCooldown();
            case PacketInfo.RC_LINK_IDX_FF_STEP:
                return mStatusViewModel.getValueFfStep();
            case PacketInfo.RC_LINK_IDX_FF_RATIO_0:
                return mStatusViewModel.getValueFfRatio0();
            case PacketInfo.RC_LINK_IDX_FF_RATIO_1:
                return mStatusViewModel.getValueFfRatio1();
            case PacketInfo.RC_LINK_IDX_FF_RATIO_2:
                return mStatusViewModel.getValueFfRatio2();
            case PacketInfo.RC_LINK_IDX_FF_RATIO_3:
                return mStatusViewModel.getValueFfRatio3();
            default:
                return PacketInfo.LINK_VALUE_UNKNOWN;
        }
    }

    private static String makeFfValueText(int value, String unit)
    {
        return (value == PacketInfo.LINK_VALUE_UNKNOWN) ? "-" : (value + unit);
    }

    /* 다이얼로그 제목에 얹는 «지금 상태».
     *
     * 평균 amplitude(26)는 비율 구간 판정과 «같은 값» 이라 지금 어느 구간이 적용되는지를
     * 그대로 알 수 있다. 세움(24) - 올림(25) 은 합쳐진 요청 수인데, 둘 다 하위 8비트라
     * 차분을 mod 256 으로 봐야 한다. */
    private String makeFfStatusText()
    {
        StringBuilder sb = new StringBuilder();

        int avg = mStatusViewModel.getValueFfAvgAmp();
        int band = PacketInfo.makeFfBandIndex(avg);

        if (band == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            sb.append("평균 amplitude -");
        }
        else
        {
            sb.append("평균 ").append(avg).append(" → 비율 구간 ").append(band).append(" 적용");
        }

        int raised = mStatusViewModel.getValueFfRaised();
        int applied = mStatusViewModel.getValueFfApplied();

        if (raised != PacketInfo.LINK_VALUE_UNKNOWN && applied != PacketInfo.LINK_VALUE_UNKNOWN)
        {
            int merged = ((raised - applied) + 256) % 256; // 둘 다 하위 8비트다

            sb.append(System.lineSeparator()) //
                .append("세움 ").append(raised).append(" / 올림 ").append(applied);

            if (merged > 0)
            {
                sb.append(" (").append(merged).append("건 합쳐짐)");
            }
        }

        int rise = PacketInfo.makeFfRiseStepsPerSec(mStatusViewModel.getValueFfStep(), //
            mStatusViewModel.getValueFfCooldown());

        if (rise != PacketInfo.LINK_VALUE_UNKNOWN)
        {
            sb.append(System.lineSeparator()) //
                .append("초당 상승 ").append(rise).append("스텝 · 하강 약 3.3스텝");

            if (rise > PacketInfo.FF_FALL_STEPS_PER_SEC)
            {
                sb.append("  [주의] 일정한 소리에서도 상한까지 올라갈 수 있습니다");
            }
        }

        return sb.toString();
    }

    private void makeDialog_ffSettings()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[FF] 이미 다이얼로그가 표시 중이라서 피드포워드 목록을 띄우지 않습니다.");
            return;
        }

        final FfItem[] ffItems = makeFfItems();

        String[] items = new String[ffItems.length + 1];

        int enable = mStatusViewModel.getValueFfEnable();

        items[0] = "사용 : " + ((enable == PacketInfo.LINK_VALUE_UNKNOWN) //
            ? "-" //
            : ((enable == PacketInfo.LINK_FLAG_ENABLE) ? "켬" : "끔"));

        for (int i = 0; i < ffItems.length; i++)
        {
            items[i + 1] = ffItems[i].name + " : " + makeFfValueText(getFfValue(ffItems[i].index), ffItems[i].unit);
        }

        /* setMessage 와 목록을 같이 주면 목록이 사라진다. 제목 자리에 직접 그린다. */
        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setCustomTitle(makeDialogTitleView("자극 레벨 피드포워드", makeFfStatusText())) //
            .setItems(items, (dialogInterface, which) ->
            {
                dialogInterface.dismiss();

                if (which == 0)
                {
                    makeDialog_ffEnable();
                }
                else
                {
                    makeDialog_ffValue(ffItems[which - 1]);
                }
            }) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    private void makeDialog_ffEnable()
    {
        boolean ff3Supported = mMainActivity.isFf3Protocol() && mMainActivity.ff3.supported();
        String[] items = ff3Supported ? new String[]{"전원 보상 끄기 (OFF)", "자극 크기에 따라 보상 (FF1)", "소리 변화에 따라 보상 (FF3)"} : new String[]{"전원 보상 끄기 (OFF)", "자극 크기에 따라 보상 (FF1)"};
        int[] modes = ff3Supported ? new int[]{0, 1, 3} : new int[]{0, 1};

        int enable = mStatusViewModel.getValueFfEnable();
        int checked = enable == 3 ? 2 : (enable == 0 || enable == 1 ? enable : -1);

        // 켜도 동작하지 않는 조건이 넷이다. 켜는 자리에서 알려야 «켰는데 왜 안 되지» 가 안 생긴다.
        String guide = "전원을 미리 보상할 기준을 선택합니다. 선택한 방식은 장치에 저장됩니다."
            + "\n소리 보상 설정을 저장해도 자동으로 켜지지는 않습니다. 실제 보상에는 아래 조건도 필요합니다."
            + "\n" + mMainActivity.makeFeedForwardBlockText();

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setCustomTitle(makeDialogTitleView("전원 보상 방식", guide)) //
            .setSingleChoiceItems(items, checked, (dialogInterface, which) ->
            {
                mMainActivity.sendFfParam(PacketInfo.RC_LINK_IDX_FF_ENABLE, modes[which]);
                dialogInterface.dismiss();
            }) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    private void makeDialog_ffValue(FfItem item)
    {
        final int count = (item.max - item.min) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            items[i] = (item.min + i) + item.unit;
        }

        int current = getFfValue(item.index);

        if (current < item.min || item.max < current)
        {
            current = item.def;
        }

        final int checkedIndex = Math.max(0, Math.min(count - 1, current - item.min));

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setTitle(item.name) //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                mMainActivity.sendFfParam(item.index, item.min + which);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("기본값", (dialogInterface, i) -> mMainActivity.sendFfParam(item.index, item.def)) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    private void sendMaxTxPowerPacket(int level)
    {
        mMainActivity.sendMaxTxPower(level);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 고정 PMIC 선택 다이얼로그
    ///
    /// 목록에서 전압을 고르면 노말 / 매핑 어느 모드인지와 무관하게 그 값으로 고정한다.
    /// 아래 '해제' 를 누르면 고정을 풀고 원래대로 모드별 하한을 따르게 한다.
    ///
    /// 상한이 Normal PMIC(213)보다 1 스텝 높은 214 다. 하한 계열의 213 제한은 제어가
    /// 멈추는 것을 막으려는 값인데, 강제 고정은 멈추는 것이 목적이라 해당하지 않는다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_fixedTxPowerSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[PMIC] 이미 다이얼로그가 표시 중이라서 고정 PMIC 목록을 띄우지 않습니다.");
            return;
        }

        final int minLevel = PacketInfo.FORCE_TX_PWR_SELECT_MIN;
        final int maxLevel = PacketInfo.FORCE_TX_PWR_SELECT_MAX;
        final int count = (maxLevel - minLevel) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int level = minLevel + i;
            items[i] = String.format("%.3f Ｖ  (레벨 %d)", (level * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f, level);
        }

        /* 이미 고정 중이면 그 값을, 해제 상태면 상시 동작 하한을 기준점으로 삼아
         * 목록이 그 위치에서 열리게 한다. */
        int baseLevel = mStatusViewModel.getValueForceTxPowerLevel();

        if (baseLevel < minLevel || maxLevel < baseLevel)
        {
            baseLevel = mStatusViewModel.getValueMinTxPowerLevel();
        }

        if (baseLevel < minLevel || maxLevel < baseLevel)
        {
            baseLevel = PacketInfo.MIN_TX_PWR_DEFAULT;
        }

        final int checkedIndex = baseLevel - minLevel;

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setTitle("고정 PMIC 선택") //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                sendForceTxPowerPacket(minLevel + which);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("해제", (dialogInterface, i) -> sendForceTxPowerPacket(PacketInfo.FORCE_TX_PWR_RELEASE)) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 고정 PMIC 설정 / 해제 패킷 전송
    ///
    /// 레벨 0 이 해제 센티널이라 설정과 해제가 같은 옵션(14)을 쓴다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendForceTxPowerPacket(int level)
    {
        mMainActivity.sendLinkParam(PacketInfo.RC_LINK_IDX_FORCE_TX_POWER, level);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 링크 Tx 파워 상승 스텝 선택 다이얼로그
    ///
    /// 내부기 전원이 모자랄 때 한 번에 올리는 폭이다. 하강은 1 스텝 고정이므로,
    /// 값을 키우면 회복은 빨라지지만 경계에서의 진동 폭도 그만큼 커진다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_txStepUpSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[PMIC] 이미 다이얼로그가 표시 중이라서 상승 스텝 목록을 띄우지 않습니다.");
            return;
        }

        final int minStep = PacketInfo.TX_STEP_UP_SELECT_MIN;
        final int maxStep = PacketInfo.TX_STEP_UP_SELECT_MAX;
        final int count = (maxStep - minStep) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int step = minStep + i;
            items[i] = String.format("%d 스텝  (%d ㎷)", step, (step * PacketInfo.TX_PWR_LEVEL_STEP_MV));
        }

        int currentStep = mStatusViewModel.getValueTxStepUp();

        if (currentStep < minStep || maxStep < currentStep)
        {
            currentStep = PacketInfo.TX_STEP_UP_DEFAULT;
        }

        final int checkedIndex = currentStep - minStep;

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setTitle("링크 상승 스텝 선택") //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                sendTxStepUpPacket(minStep + which);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("기본값", (dialogInterface, i) -> sendTxStepUpPacket(PacketInfo.TX_STEP_UP_DEFAULT)) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 링크 Tx 파워 상승 스텝 설정 패킷 전송
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendTxStepUpPacket(int step)
    {
        mMainActivity.sendLinkParam(PacketInfo.RC_LINK_IDX_TX_POWER_STEP_UP, step);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 전원 안정용 NopStandby 개수 선택 다이얼로그
    ///
    /// 백텔을 «읽기 직전» 에 넣는 NopStandby 의 개수다. 읽기 «뒤» 의 NopBacktel 3개는
    /// FPGA 가 요구하는 물리 수신 시간이라 고정이며 여기서 건드리는 대상이 아니다.
    ///
    /// 아무 값이나 고르게 하지 않는다. 자리 맞추기가 0 이 되는 값만 FIFO 24 슬롯을
    /// 버리지 않으므로, 지금 패킷 수에서 «쓸 수 있는» 값만 목록에 올린다.
    /// 표는 패킷 수(인덱스 13)로 찾는다. 펄스폭으로 찾으면 nOFm 에서 틀린다.
    ///
    /// 각 값이 자극을 얼마나 끊는지 함께 보여 준다. 개수를 늘리면 전원은 안정되지만
    /// 그만큼 자극이 비므로, 고르는 사람이 그 대가를 보고 정할 수 있어야 한다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_nopStandbySelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[LINK] 이미 다이얼로그가 표시 중이라서 Nop 개수 목록을 띄우지 않습니다.");
            return;
        }

        int frameNum = mStatusViewModel.getValueFrameNum();

        /* 패킷 수를 모르면 표를 찾을 수 없다. 구형 펌웨어(4.2 미만)이거나 아직 못 읽은 상태다.
         * 그때는 예전처럼 0 ~ 19 를 전부 올린다. */
        if (!PacketInfo.isNopFrameNumUsable(frameNum))
        {
            makeDialog_nopStandbyPlain(frameNum);
            return;
        }

        final int[] values = PacketInfo.getNopUsableValues(frameNum);
        final int defaultNop = PacketInfo.getNopDefaultByFrame(frameNum);

        String[] items = new String[values.length];

        final int hardLimit = PacketInfo.getNopHardLimit(frameNum);
        final int stimMax = PacketInfo.getNopStimMax(frameNum);

        for (int i = 0; i < values.length; i++)
        {
            int gapUs = PacketInfo.getNopGapUs(frameNum, i);
            int nop = values[i];

            StringBuilder sb = new StringBuilder();

            sb.append(nop).append(" 개");

            if (0 < gapUs)
            {
                sb.append("   자극 공백 ").append(gapUs).append("us");
            }

            if (nop == defaultNop)
            {
                sb.append("   (기본)");
            }

            /* 자극이 한 채널도 안 나가는 구간은 그렇게 적는다. «주의» 라고만 하면
             * 무엇이 문제인지 알 수 없다. 막지는 않는다 - 자극이 어차피 0 인 구간도
             * 전원 회복 시간을 늘리려고 쓸 이유가 있기 때문이다. */
            if (stimMax < 0 || stimMax < nop)
            {
                sb.append("  [자극 0]");
            }

            /* 실상한을 넘는 값은 써도 CFX 가 조용히 잘라 낸다. 목록에 올리되 알린다. */
            if (hardLimit < nop)
            {
                sb.append("  [").append(hardLimit).append(" 로 잘림]");
            }

            items[i] = sb.toString();
        }

        int currentNop = mStatusViewModel.getValueNopStandbyCount();
        int checkedIndex = -1;

        for (int i = 0; i < values.length; i++)
        {
            if (values[i] == currentNop)
            {
                checkedIndex = i;
                break;
            }
        }

        final int selected = checkedIndex;

        /* setMessage 와 setSingleChoiceItems 를 같이 주면 안 된다.
         * 둘이 같은 자리를 쓰는 탓에 메시지가 목록을 밀어내 «고를 것이 없는» 화면이 된다.
         * 안내는 제목 자리에 직접 그려 넣는다. */
        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setCustomTitle(makeDialogTitleView("전원 안정 Nop 선택", makeNopDialogMessage(frameNum))) //
            .setSingleChoiceItems(items, selected, (dialogInterface, which) ->
            {
                sendNopStandbyPacket(values[which]);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("기본값", (dialogInterface, i) -> mMainActivity.sendNopStandbyDefault()) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /* 패킷 수를 모를 때 쓰는 예전 방식 목록. 어느 값이 자리 맞춤에 맞는지 알 수 없으므로
     * 0 ~ 19 를 그대로 올린다. */
    private void makeDialog_nopStandbyPlain(int frameNum)
    {
        final int minCount = PacketInfo.NOP_STANDBY_SELECT_MIN;
        final int maxCount = PacketInfo.NOP_STANDBY_SELECT_MAX;
        final int count = (maxCount - minCount) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int nop = minCount + i;

            items[i] = (nop == 0) ? "0 개  (Nop 없음)" : (nop + " 개");
        }

        int currentCount = mStatusViewModel.getValueNopStandbyCount();

        if (currentCount < minCount || maxCount < currentCount)
        {
            currentCount = PacketInfo.NOP_STANDBY_DEFAULT;
        }

        final int checkedIndex = currentCount - minCount;

        String message = (frameNum == PacketInfo.LINK_VALUE_UNKNOWN) //
            ? "패킷 수를 읽지 못해 쓸 수 있는 값을 가릴 수 없습니다." //
            : ("패킷 수 " + frameNum + " 은 백텔이 나가지 않는 구간입니다.");

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setCustomTitle(makeDialogTitleView("전원 안정 Nop 선택", message)) //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                sendNopStandbyPacket(minCount + which);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("기본값", (dialogInterface, i) -> mMainActivity.sendNopStandbyDefault()) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /* 다이얼로그 머리말. 지금 맵이 어떤 상태인지와, 펄스폭과 패킷 수가 어긋난다면 그 이유를 적는다. */
    private String makeNopDialogMessage(int frameNum)
    {
        int pulseWidth = mStatusViewModel.getValuePulseWidth();
        int strategy = mStatusViewModel.getValueStimStrategy();
        int nopEnable = mStatusViewModel.getValueNopEnable();

        StringBuilder sb = new StringBuilder();

        sb.append("패킷 수 ").append(frameNum);

        if (pulseWidth != PacketInfo.LINK_VALUE_UNKNOWN)
        {
            sb.append(" · 펄스폭 ").append(pulseWidth).append("us");
        }

        if (strategy != PacketInfo.LINK_VALUE_UNKNOWN)
        {
            sb.append(" · ").append(makeStimStrategyName(strategy));
        }

        sb.append("\n지금 값은 ");
        sb.append((nopEnable == PacketInfo.NOP_ENABLE_MANUAL) ? "리모콘이 정한 값" : "패킷 수별 기본값");
        sb.append("입니다.");

        /* 두 상한은 다른 이야기다. 실상한까지는 «써지고», 자극이 남는 최대를 넘으면
         * 그 사이클의 자극이 «한 채널도» 안 나간다. 실상한 자리는 아홉 패킷 수 모두 자극 0 이다. */
        int stimMax = PacketInfo.getNopStimMax(frameNum);

        sb.append("\n실상한 ").append(PacketInfo.getNopHardLimit(frameNum)).append("개");

        if (0 <= stimMax)
        {
            sb.append(" · 자극이 남는 최대 ").append(stimMax).append("개");
        }

        if (PacketInfo.isNopStimAlwaysZero(frameNum))
        {
            /* 패킷 수 9 는 기본값에서 이미 자극이 0 이다. [S] 를 줄여도 살아나지 않는다.
             * 이 사실을 적지 않으면 «줄이면 되겠지» 하고 헛수고를 하게 된다. */
            sb.append("\n\n[주의] 이 맵은 어떤 값을 넣어도 자극이 남지 않습니다. 줄여도 살아나지 않습니다.");
        }
        else if (stimMax == PacketInfo.getNopDefaultByFrame(frameNum))
        {
            // 패킷 수 5 · 7 은 기본값이 곧 한계라 한 칸만 올려도 자극이 사라진다.
            sb.append("\n\n[주의] 기본값이 곧 한계입니다. 한 칸만 올려도 자극이 없어집니다.");
        }

        /* nOFm 은 밴드 16 이상이면 패킷 수를 3 으로 고정한다. 펄스폭과 어긋나 보이는 것이
         * 정상이라는 뜻이라, 그 사실을 적어 두지 않으면 고장으로 오해한다. */
        if (strategy == PacketInfo.STIM_STRATEGY_NOFM)
        {
            sb.append("\n\nnOFm 은 패킷 수를 3 으로 고정하므로 펄스폭과 어긋나 보입니다.");

            if (pulseWidth != PacketInfo.LINK_VALUE_UNKNOWN //
                && (pulseWidth < PacketInfo.NOFM_PULSE_WIDTH_MIN || PacketInfo.NOFM_PULSE_WIDTH_MAX < pulseWidth))
            {
                sb.append("\n[주의] 펄스폭이 ") //
                    .append(PacketInfo.NOFM_PULSE_WIDTH_MIN).append(" ~ ").append(PacketInfo.NOFM_PULSE_WIDTH_MAX) //
                    .append("us 밖입니다. 3프레임 고정 전제를 벗어난 상태로, 계측된 바 없습니다.");
            }
        }
        else if (strategy == PacketInfo.STIM_STRATEGY_MEDIUM)
        {
            sb.append("\n\n[주의] medium 은 자극 PCM 이 채워지지 않습니다.");
        }

        return sb.toString();
    }

    /* 제목과 안내를 한 덩어리로 그린 뷰.
     *
     * AlertDialog 는 메시지와 선택 목록이 같은 자리를 쓴다. 둘 다 주면 목록이 사라지므로
     * 안내가 필요할 때는 제목 자리에 넣는다. */
    private View makeDialogTitleView(String title, String message)
    {
        int padding = (int) (16 * getResources().getDisplayMetrics().density);

        LinearLayout box = new LinearLayout(requireContext());

        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(padding, padding, padding, padding / 2);

        TextView titleView = new TextView(requireContext());

        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);

        box.addView(titleView);

        if (message != null && !message.isEmpty())
        {
            TextView messageView = new TextView(requireContext());

            messageView.setText(message);
            messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            messageView.setPadding(0, padding / 3, 0, 0);

            box.addView(messageView);
        }

        return box;
    }

    private String makeStimStrategyName(int strategy)
    {
        switch (strategy)
        {
            case PacketInfo.STIM_STRATEGY_CIS:
                return "CIS";

            case PacketInfo.STIM_STRATEGY_NOFM:
                return "nOFm";

            case PacketInfo.STIM_STRATEGY_MEDIUM:
                return "medium";

            default:
                return "전략 ?";
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 전원 안정용 NopStandby 개수 설정 패킷 전송
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendNopStandbyPacket(int nopCount)
    {
        mMainActivity.sendLinkParam(PacketInfo.RC_LINK_IDX_POWER_STABLE_NOP, nopCount);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 링크 Tx 파워 하한 설정 패킷 전송
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendMinTxPowerPacket(int level)
    {
        mMainActivity.sendLinkParam(PacketInfo.RC_LINK_IDX_MIN_TX_POWER, level);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - BT Select 버튼 (링크 백텔 주기를 목록에서 직접 선택)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_btSelectButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            makeDialog_backtelSelect();
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 링크 백텔 주기 선택 다이얼로그
    ///
    /// 프로토콜 단위가 100msec 이므로 100 ~ 1000msec 를 100msec 간격으로 고른다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_backtelSelect()
    {
        if (mDialog != null)
        {
            Log.d(TAG, "[BACKTEL] 이미 다이얼로그가 표시 중이라서 선택 목록을 띄우지 않습니다.");
            return;
        }

        final int minPeriod = PacketInfo.BACKTEL_PERIOD_SELECT_MIN;
        final int maxPeriod = PacketInfo.BACKTEL_PERIOD_SELECT_MAX;
        final int count = (maxPeriod - minPeriod) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            items[i] = ((minPeriod + i) * PacketInfo.BACKTEL_PERIOD_UNIT_MS) + " msec";
        }

        int currentPeriod = mStatusViewModel.getValueBacktelPeriod();

        if (currentPeriod < minPeriod || maxPeriod < currentPeriod)
        {
            currentPeriod = PacketInfo.BACKTEL_PERIOD_DEFAULT_100MS;
        }

        final int checkedIndex = currentPeriod - minPeriod;

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setTitle("백텔 주기 선택") //
            .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
            {
                sendBacktelPeriodPacket(minPeriod + which);
                dialogInterface.dismiss();
            }) //
            .setNegativeButton("기본값", (dialogInterface, i) -> sendBacktelPeriodPacket(PacketInfo.BACKTEL_PERIOD_DEFAULT_100MS)) //
            .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 링크 백텔 주기 설정 패킷 전송
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendBacktelPeriodPacket(int period100ms)
    {
        mMainActivity.sendLinkParam(PacketInfo.RC_LINK_IDX_BACKTEL_PERIOD, period100ms);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 게이팅(묵음) 설정 패킷 전송
    ///
    /// 비활성화(GATING_DISABLE) 시 펌웨어는 오프셋 값을 N/A 로 무시하고 기존 값을 유지하므로,
    /// 여기서 실어 보내는 오프셋은 활성화일 때만 실제로 반영된다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void showGatingOffsetDialog()
    {
        if (mDialog != null || !mMainActivity.isGatingSupported())
        {
            return;
        }
        final String targetSession = mMainActivity.getSelectedSessionId();
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setGravity(android.view.Gravity.CENTER);
        input.setTextColor(0xffedf0f8);
        input.setTextSize(18);
        input.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.app_sans));
        input.setContentDescription("Gating T-level offset, 1부터 255");
        int current = mStatusViewModel.getValueGatingOffset();
        input.setText(String.valueOf(current >= 1 && current <= 255 ? current : PacketInfo.MUTE_T_LEVEL_OFFSET_DEFAULT));
        android.widget.LinearLayout body = new android.widget.LinearLayout(requireContext());
        int padding = Math.round(20 * getResources().getDisplayMetrics().density);
        body.setPadding(padding, 0, padding, 0);
        body.addView(input, new android.widget.LinearLayout.LayoutParams(-1, -2));
        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext())
            .setCustomTitle(makeDialogTitleView("Gating 세부 설정", "T-level offset · 1–255 · 기본값 2\n값을 적용하면 Gating이 켜집니다."))
            .setView(body)
            .setNeutralButton("기본값 2", null)
            .setNegativeButton("취소", null)
            .setPositiveButton("적용 · On", null)
            .create();
        final AlertDialog dialog = mDialog;
        dialog.setOnDismissListener(ignored -> mDialog = null);
        showControlDialog(dialog);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> input.setText("2"));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view ->
        {
            int offset;
            try
            {
                offset = Integer.parseInt(input.getText().toString().trim());
            }
            catch (NumberFormatException error)
            {
                input.setError("1부터 255까지 입력하세요.");
                return;
            }
            if (offset < 1 || offset > 255)
            {
                input.setError("1부터 255까지 입력하세요.");
                return;
            }
            if (!Objects.equals(targetSession, mMainActivity.getSelectedSessionId())
                || !mRemoteControlBinding.otaGatingButton.isEnabled())
            {
                dialog.dismiss();
                return;
            }
            sendGatingPacket(PacketInfo.MUTE_ENABLE, offset);
            dialog.dismiss();
        });
    }

    private void sendGatingPacket(int enableState)
    {
        /* 묵음은 릴리즈 2~3 유산이라 이 명령만 구형 포맷을 쓴다.
         * [커맨드, 옵션 2, 세부1, 세부2, T레벨 오프셋]
         *
         * 비활성화일 때 펌웨어는 오프셋을 N/A 로 무시하고 기존 값을 유지하므로,
         * 여기서 실어 보내는 오프셋은 활성화일 때만 실제로 반영된다. */
        int offset = mStatusViewModel.getValueGatingOffset();
        sendGatingPacket(enableState, offset >= 0 && offset <= 255 ? offset : PacketInfo.MUTE_T_LEVEL_OFFSET_DEFAULT);
    }

    private void sendGatingPacket(int enableState, int offset)
    {
        byte[] packet = new byte[PacketInfo.RC_REQ_LEN_MUTE_WRITE];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[1] = (byte) PacketInfo.RC_OPT_MUTE_WRITE;
        packet[2] = (byte) PacketInfo.MUTE_SUB_OPT_NORMAL_MODE;
        packet[3] = (byte) (enableState & 0xFF);
        packet[4] = (byte) (offset & 0xFF);

        Log.d(TAG, "[GATING] 게이팅 설정 요청 : " + ((enableState == PacketInfo.MUTE_ENABLE) ? "활성화" : "비활성화") + ", 오프셋 " + offset);

        mMainActivity.sendPacket(packet);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 맵 초기화 확인 다이얼로그
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void makeDialog_mapInitConfirm(int mapInitType, String mapInitName)
    {
        // 다른 다이얼로그가 이미 떠 있으면 무시한다.
        if (mDialog != null)
        {
            Log.d(TAG, "[MAP INIT] 이미 다이얼로그가 표시 중이라서 맵 초기화 확인 다이얼로그를 띄우지 않습니다.");
            return;
        }

        mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
            .setTitle("맵 초기화") //
            .setMessage("사운드처리기에 등록된 모든 내부기의 맵 데이터를\n" + mapInitName + " 으로 덮어씁니다.\n\n되돌릴 수 없습니다. 진행하시겠습니까?") //
            .setPositiveButton("초기화", (dialogInterface, i) -> sendMapInitPacket(mapInitType, mapInitName)) //
            .setNegativeButton("취소", null) //
            .setCancelable(false) //
            .create();

        // 이 화면의 다른 다이얼로그와 달리 반복 사용해야 하므로, 닫힐 때 참조를 비워 다시 띄울 수 있게 한다.
        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        showControlDialog(mDialog);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 맵 초기화 패킷 전송
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendMapInitPacket(int mapInitType, String mapInitName)
    {
        /* 맵 초기화는 자극 도메인(0x21) 인덱스 1 의 쓰기다. 돌려받을 값이 없는 쓰기 전용이다.
         * 사운드처리기는 응답을 먼저 보내고 초기화를 시작하므로 응답 = 완료가 아니다. */
        Log.d(TAG, "[MAP INIT] 맵 초기화 요청 : " + mapInitName + " (type=" + String.format("%02X", mapInitType) + ")");

        mMainActivity.sendMapInit(mapInitType);
    }

    // LiveData - Connection
    private void liveDataConnection()
    {
        mStatusViewModel.getObjectConnectionState().observe(sessionObservation, integer ->
        {
            Log.v(TAG, "옵저버 : 연결상태 -> " + integer);
            updateIdentityBadges();
            updateConnectionOverlay();
            updateOtaControlPause();
        });
    }

    // LiveData - IsdID
    private void liveDataIsdID()
    {
        mStatusViewModel.getLiveDataIsdID().observe(sessionObservation, o ->
        {
            int value = mStatusViewModel.getValueIsdID();
            String textValue = String.format("#%08X", value);
            Log.v(TAG, "옵저버 : 내부기 ID -> " + textValue);
            mRemoteControlBinding.remoteControlConnectionIsdIdTextview.setText(textValue);
        });
    }

    // LiveData - 프로그램 번호 / 볼륨 / 최대출력
    //
    // 세 값 모두 연결 직후의 상태 패킷(0x43)으로 채워지고, 이후에는 해당 항목의
    // 응답 패킷을 받을 때마다 갱신된다. 볼륨과 최대출력은 표시 전용이다.
    private void liveDataProgramAndLevels()
    {
        mStatusViewModel.getLiveDataProgram().observe(sessionObservation, o ->
        {
            int value = mStatusViewModel.getValueProgram();
            Log.v(TAG, "옵저버 : Prog -> " + value);
            mRemoteControlBinding.otaProgramTextview.setText("Prog " + value);
        });

        mStatusViewModel.getLiveDataVolume().observe(sessionObservation, o ->
        {
            int value = mStatusViewModel.getValueVolume();
            Log.v(TAG, "옵저버 : Vol -> " + value);
            mRemoteControlBinding.otaVolumeTextview.setText("Vol " + value);
        });

        mStatusViewModel.getLiveDataMaxOutput().observe(sessionObservation, o ->
        {
            int value = mStatusViewModel.getValueMaxOutput();
            Log.v(TAG, "옵저버 : 최대출력 -> " + value);
            mRemoteControlBinding.otaMaxOutputTextview.setText("Out " + value);
        });

        // LED 상태. 연결 직후 상태 패킷(0x43)으로 채워지고, LED 버튼 응답마다 갱신된다.
        mStatusViewModel.getLiveDataLed().observe(sessionObservation, o ->
        {
            int value = mStatusViewModel.getValueLed();
            Log.v(TAG, "옵저버 : LED -> " + value);
            mRemoteControlBinding.otaLedTextview.setText("LED " + ((value == PacketInfo.LED_ON) ? "On" : "Off"));
        });

        // 자극알림 상태.
        mStatusViewModel.getLiveDataNotification().observe(sessionObservation, o ->
        {
            int value = mStatusViewModel.getValueNotification();
            Log.v(TAG, "옵저버 : 자극Alarm -> " + value);
            mRemoteControlBinding.otaAlarmTextview.setText("Alarm " + ((value == PacketInfo.NOTIFICATION_ON) ? "On" : "Off"));
        });

        // 상시 동작 Tx 파워 하한(PMIC).
        mStatusViewModel.getLiveDataMinTxPowerLevel().observe(sessionObservation, o -> updateMinPowerText());
        mStatusViewModel.getLiveDataMaxTxPowerLevel().observe(sessionObservation, o -> updateMaxPowerText());

        // 현재 Tx 파워(관찰값). 설정값이 아니라 지금 PMIC 에 실제로 쓰인 값이다.
        mStatusViewModel.getLiveDataCurTxPowerLevel().observe(sessionObservation, o ->
        {
            int level = mStatusViewModel.getValueCurTxPowerLevel();
            mRemoteControlBinding.otaCurPmicTextview.setText(level == 0 ? "미확인" : makeVoltageText("Cur", level));
        });

        // 매핑(피팅) 전용 Tx 파워 하한.
        mStatusViewModel.getLiveDataMappingTxPowerLevel().observe(sessionObservation, o ->
        {
            int level = mStatusViewModel.getValueMappingTxPowerLevel();
            mRemoteControlBinding.otaMappingPmicTextview.setText(makeVoltageText("Map", level));
        });

        // Tx 파워 강제 고정. 0 이면 해제 상태다.
        mStatusViewModel.getLiveDataForceTxPowerLevel().observe(sessionObservation, o ->
        {
            int level = mStatusViewModel.getValueForceTxPowerLevel();
            String text;

            if (level == PacketInfo.LINK_VALUE_UNKNOWN)
            {
                text = "Fix -";
            }
            else if (level == PacketInfo.FORCE_TX_PWR_RELEASE)
            {
                text = "Fix Off";
            }
            else
            {
                text = makeVoltageText("Fix", level);
            }

            mRemoteControlBinding.otaForcePmicTextview.setText(text);
        });

        // Tx 파워 상승 스텝.
        mStatusViewModel.getLiveDataTxStepUp().observe(sessionObservation, o -> updateStepUpText());

        /* 백텔 재시도 상태. one coin 과 infinite coin 두 값을 합쳐 한 칸에 보여준다.
         * infinite 가 우선하므로 켜져 있으면 one coin 값과 무관하게 무한대로 표시한다.
         * 두 값이 따로 도착하므로 옵저버도 둘 다 걸어 어느 쪽이 바뀌어도 갱신되게 한다. */
        mStatusViewModel.getLiveDataOneCoin().observe(sessionObservation, o -> updateCoinText());
        mStatusViewModel.getLiveDataInfiniteCoin().observe(sessionObservation, o -> updateCoinText());

        // 링크 제어 모드. 0 = 전원 상태 기준, 1 = 백텔 수신 기준.
        mStatusViewModel.getLiveDataLinkCtrlMode().observe(sessionObservation, o -> updateCtrlModeText());

        // Tx 파워 상승 가속.
        mStatusViewModel.getLiveDataTxPowerAccel().observe(sessionObservation, o -> updateAccelText());

        /* 전원 안정용 NopStandby 개수.
         * 되읽은 값과 실제 동작 개수가 다를 수 있다. 펌웨어가 채널당 프레임 수에 맞춰
         * 매 백텔 사이클마다 다시 잘라내기 때문이다. 여기 보이는 값은 «설정값» 이다. */
        mStatusViewModel.getLiveDataNopStandbyCount().observe(sessionObservation, o -> updateNopText());

        /* 개수와 «어디서 온 값인지» 가 따로 도착하므로 둘 다 걸어 어느 쪽이 바뀌어도 갱신되게 한다. */
        mStatusViewModel.getLiveDataNopEnable().observe(sessionObservation, o -> updateNopText());

        // 자극 전략. 펄스폭과 패킷 수가 어긋나는 이유가 이 값에 있다.
        mStatusViewModel.getLiveDataStimStrategy().observe(sessionObservation, o ->
        {
            int value = mStatusViewModel.getValueStimStrategy();
            String text = (value == PacketInfo.LINK_VALUE_UNKNOWN) ? "Strategy -" : "Strategy " + makeStimStrategyName(value);

            mRemoteControlBinding.otaStrategyTextview.setText(text);
        });

        // 현재 맵의 자극 펄스폭.
        mStatusViewModel.getLiveDataPulseWidth().observe(sessionObservation, o ->
        {
            int value = mStatusViewModel.getValuePulseWidth();
            String text = (value == PacketInfo.LINK_VALUE_UNKNOWN) ? "PW -" : ("PW " + value + "us");

            mRemoteControlBinding.otaPulseWidthTextview.setText(text);
        });

        /* 패킷 수. 쓸 수 있는 Nop 표를 찾는 근거이자, 10 이상이면 백텔이 아예 나가지 않는다.
         * 그 구간은 눈에 띄게 표시한다. */
        mStatusViewModel.getLiveDataFrameNum().observe(sessionObservation, o ->
        {
            int value = mStatusViewModel.getValueFrameNum();
            String text;

            if (value == PacketInfo.LINK_VALUE_UNKNOWN)
            {
                text = "Frm -";
            }
            else if (PacketInfo.NOP_FRAME_NUM_MAX < value)
            {
                text = "Frm " + value + "!";
            }
            else
            {
                text = "Frm " + value;
            }

            mRemoteControlBinding.otaFrameNumTextview.setText(text);
        });

        /* 판별된 펌웨어 세대. 배지 글자와 색을 바꾸고, 그 세대가 모르는 버튼을 잠근다. */
        mStatusViewModel.getLiveDataFwRelease().observe(sessionObservation, o -> applyFwRelease());

        /* 잠금 판정이 이 값들에 걸려 있다. 하나라도 바뀌면 다시 계산해야 한다.
         *
         *   제어 모드 : one coin 과 가속의 유효성이 서로 뒤바뀐다
         *   패킷 수   : 10 이상이면 링크 설정 대부분이 통째로 무의미해진다
         *   강제 고정 · infinite : 경고 문구가 달라진다 */
        mStatusViewModel.getLiveDataLinkCtrlMode().observe(sessionObservation, o -> applyFwRelease());
        mStatusViewModel.getLiveDataFrameNum().observe(sessionObservation, o -> applyFwRelease());
        mStatusViewModel.getLiveDataForceTxPowerLevel().observe(sessionObservation, o -> applyFwRelease());
        mStatusViewModel.getLiveDataInfiniteCoin().observe(sessionObservation, o -> applyFwRelease());

        // 링크 백텔 주기. 100msec 단위로 받아 밀리초로 환산해 표시한다.
        mStatusViewModel.getLiveDataBacktelPeriod().observe(sessionObservation, o -> updateBacktelText());

        mStatusViewModel.getLiveDataGatingOffset().observe(sessionObservation, value ->
        {
            int offset = mStatusViewModel.getValueGatingOffset();
            mRemoteControlBinding.otaGatingOffsetTextview.setVisibility(
                offset >= 0 && offset <= 255 && offset != PacketInfo.MUTE_T_LEVEL_OFFSET_DEFAULT ? View.VISIBLE : View.GONE);
            mRemoteControlBinding.otaGatingOffsetTextview.setText(String.valueOf(offset));
            mRemoteControlBinding.otaGatingOffsetTextview.setContentDescription("Gating T-level offset " + offset);
        });
        // 게이팅(묵음) 활성화 상태.
        mStatusViewModel.getLiveDataGatingState().observe(sessionObservation, o ->
        {
            int state = mStatusViewModel.getValueGatingState();
            String text;

            if (state == PacketInfo.MUTE_ENABLE)
            {
                text = "Gating On";
            }
            else if (state == PacketInfo.MUTE_DISABLE)
            {
                text = "Gating Off";
            }
            else
            {
                text = "Gating -";
            }

            Log.v(TAG, "옵저버 : Gating 상태 -> " + state);
            mRemoteControlBinding.otaGatingTextview.setText(text);
        });
    }

    // 무한 재시도 상태를 나타내는 기호. On / Off 와 한눈에 구분되도록 무한대 문자를 쓴다.
    static final private String COIN_INFINITE_MARK = "∞";

    // 백텔 재시도 상태를 한 칸에 표시한다. infinite 가 one coin 보다 우선한다.
    /* 전원 안정 Nop 표시.
     *
     * 값 뒤의 별표는 «리모콘이 정한 값» 이라는 뜻이다. 별표가 없으면 사운드처리기가
     * 패킷 수에 맞춰 넣은 기본값이다. 맵이 바뀌면 기본값으로 돌아가므로 이 구분이 필요하다. */
    private void updateNopText()
    {
        int value = mStatusViewModel.getValueNopStandbyCount();
        int enable = mStatusViewModel.getValueNopEnable();

        String text;

        if (value == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            text = "Nop -";
        }
        else if (enable == PacketInfo.NOP_ENABLE_MANUAL)
        {
            text = "Nop " + value + "*";
        }
        else
        {
            text = "Nop " + value;
        }

        mRemoteControlBinding.otaNopStandbyTextview.setText( //
            makeLinkStatusText(PacketInfo.RC_LINK_IDX_POWER_STABLE_NOP, "Nop", text));
    }

    /* 지금 «쓰이지 않는» 설정은 값 대신 N/A 를 보여 준다.
     *
     * 값이 남아 있어도 동작에 반영되지 않는 조합이 있다. 백텔 수신 기준 모드의 one coin
     * 이 그렇고, 백텔이 안 나가는 맵에서는 링크 설정 대부분이 그렇다. 펌웨어는 그런 값도
     * 정상으로 저장하고 성공으로 응답하기 때문에, 화면에 값이 그대로 보이면 그것이
     * 지금 쓰이는 값이라고 오해하게 된다.
     *
     * 세대가 아예 모르는 항목은 여기서 다루지 않는다. 그쪽은 취소선으로 구분한다.
     * «이 기기엔 없다» 와 «있지만 지금은 안 쓰인다» 는 다른 이야기다. */
    private String makeLinkStatusText(int linkIndex, String label, String text)
    {
        if (mMainActivity == null)
        {
            return text;
        }

        if (mMainActivity.isLinkParamSupported(linkIndex) && !mMainActivity.isLinkSettingUsable(linkIndex))
        {
            return label + " N/A";
        }

        return text;
    }

    private void updateMinPowerText()
    {
        int level = mStatusViewModel.getValueMinTxPowerLevel();

        mRemoteControlBinding.otaPmicTextview.setText( //
            makeLinkStatusText(PacketInfo.RC_LINK_IDX_MIN_TX_POWER, "PMIC", makeVoltageText("PMIC", level)));
    }

    /* 상한 미지원 세대는 값 대신 미지원을 표시하고 제어는 숨긴다. */
    private void updateMaxPowerText()
    {
        boolean supported = (mMainActivity != null) && mMainActivity.isMaxTxPowerSupported();

        mRemoteControlBinding.otaMaxPmicTextview.setVisibility(View.VISIBLE);
        mRemoteControlBinding.otaMaxPmicButton.setVisibility(supported ? View.VISIBLE : View.INVISIBLE);
        setButtonEnabled(mRemoteControlBinding.otaMaxPmicButton, supported);

        // 피드포워드도 4.6 에서 생겼다. 같은 게이트를 쓴다.
        updateFfControls();

        if (!supported)
        {
            mRemoteControlBinding.otaMaxPmicTextview.setText("미지원");
            return;
        }

        int level = mStatusViewModel.getValueMaxTxPowerLevel();

        mRemoteControlBinding.otaMaxPmicTextview.setText(makeVoltageText("Max", level));
    }

    private void showFf3(boolean diagnostic)
    {
        if (!mMainActivity.ff3.settingsReady() || mMainActivity.ff3.busy())
        {
            return;
        }
        if (ff3Dialog != null && ff3Dialog.showing())
        {
            return;
        }
        ff3Dialog = new Ff3Dialog(mMainActivity, diagnostic);
    }

    private static String ffValueText(int value)
    {
        return value < 0 ? "-" : String.valueOf(value);
    }

    private void updateFfControls()
    {
        if (mMainActivity == null || mRemoteControlBinding == null)
        {
            return;
        }
        boolean supported = mMainActivity.isExtendedLinkFeatureSupported();
        boolean v46 = mMainActivity.isFf3Protocol();
        boolean ff3 = v46 && mMainActivity.ff3.supported();
        boolean busy = mMainActivity.ff3.busy();
        boolean connected = mStatusViewModel.getConnectionState() == StatusViewModel.CONNECTION_STATE_CONNECTED;
        boolean ready = connected && (!v46 || mMainActivity.ff3.settingsReady());
        boolean canControl = supported && ready && !busy && !mMainActivity.ff3.needsReconnect() && !selectedOtaControlsPaused();
        mRemoteControlBinding.otaFfCheckButton.setVisibility(v46 ? View.VISIBLE : View.GONE);
        mRemoteControlBinding.ffCheckHint.setVisibility(v46 ? View.VISIBLE : View.GONE);
        mRemoteControlBinding.otaFfCheckButton.setEnabled(v46 && connected && !busy && !mMainActivity.ff3.needsReconnect() && !selectedOtaControlsPaused());
        mRemoteControlBinding.otaFfCheckButton.setAlpha(mRemoteControlBinding.otaFfCheckButton.isEnabled() ? 1f : 0.45f);
        mRemoteControlBinding.otaFfCheckButton.setText(busy ? "FF 설정 확인 중…" : ready ? "FF 설정 다시 확인   ›" : "FF 설정 확인   ›");
        String hint = selectedOtaControlsPaused() ? "OTA 준비 모드입니다. 부팅 후 FF 제어를 다시 사용할 수 있습니다."
            : !connected ? "장치 연결 후 FF 설정을 확인하세요."
              : mMainActivity.ff3.needsReconnect() ? "조회가 중단되었습니다. 장치를 다시 연결한 뒤 확인하세요."
                : busy ? "장치 설정을 읽고 있습니다. 완료되면 제어 버튼이 활성화됩니다."
                  : ready ? "설정 확인 완료 · 아래에서 모드와 설정을 제어할 수 있습니다."
                    : mMainActivity.ff3.state == todoc.cochlear.remoteapp.ff.Ff3Session.State.ERROR ? mMainActivity.ff3.message
                      : "먼저 FF 설정을 확인하면 아래 제어를 사용할 수 있습니다.";
        mRemoteControlBinding.ffCheckHint.setText(hint);
        int mode = mStatusViewModel.getValueFfEnable();
        mRemoteControlBinding.otaFfControls.setVisibility(supported ? View.VISIBLE : View.GONE);
        mRemoteControlBinding.ffUnavailableText.setVisibility(supported ? View.GONE : View.VISIBLE);
        mRemoteControlBinding.ffUnavailableText.setText("FF 미지원 · 이 장치에서는 FF 설정 조회와 제어를 사용할 수 없습니다.");
        mRemoteControlBinding.otaFfStatusTextview.setVisibility(View.GONE);
        mRemoteControlBinding.otaFfButton.setText("FF 모드 · " + (mode == 3 ? "FF3" : mode == 1 ? "FF1" : mode == 0 ? "Off" : "확인 필요") + "   ›");
        mRemoteControlBinding.otaFf3SettingsButton.setText("FF3 설정 · 감지 / 보상   ›");
        mRemoteControlBinding.otaFf3DiagnosticsButton.setText("FF3 진단 · 입력 / 상태   ›");
        mRemoteControlBinding.otaFfButton.setEnabled(canControl && mode >= 0 && (!v46 || (ff3 && !mMainActivity.ff3.editing() && mMainActivity.ff3.config().complete())));
        mRemoteControlBinding.otaFf1SettingsButton.setVisibility(mode == 1 ? View.VISIBLE : View.GONE);
        mRemoteControlBinding.otaFf1SettingsButton.setEnabled(canControl);
        // OFF에서도 활성화 전에 FF3 설정을 준비하거나 남은 편집을 복구할 수 있어야 한다.
        mRemoteControlBinding.otaFf3SettingsButton.setVisibility(v46 && mode != 1 ? View.VISIBLE : View.GONE);
        mRemoteControlBinding.otaFf3SettingsButton.setEnabled(canControl);
        mRemoteControlBinding.otaFf3DiagnosticsButton.setVisibility(ff3 && mode == 3 ? View.VISIBLE : View.GONE);
        mRemoteControlBinding.otaFf3DiagnosticsButton.setEnabled(canControl);
        for (View action : new View[]{mRemoteControlBinding.otaFfButton, mRemoteControlBinding.otaFf1SettingsButton,
            mRemoteControlBinding.otaFf3SettingsButton, mRemoteControlBinding.otaFf3DiagnosticsButton})
        {
            action.setAlpha(action.isEnabled() ? 1f : 0.45f);
        }
        String text = !supported ? "FF 미지원" : mode == 3 ? "FF3 On" : mode == 1 ? "FF1 On" : mode == 0 ? "FF Off" : "FF Unknown";
        if (supported && mode == 1)
        {
            text += " · Step " + ffValueText(mStatusViewModel.getValueFfStep()) + " · CD " + ffValueText(mStatusViewModel.getValueFfCooldown()) + "ms";
        }
        if (supported && mode == 3)
        {
            Ff3Config c = mMainActivity.ff3.config();
            text += c.complete() ? " · Zones " + c.get(55) + " · Limit " + c.get(50) : " · 설정 확인 필요";
            if (mMainActivity.ff3.status >= 0)
            {
                text += (mMainActivity.ff3.status & 8) != 0 ? " · CFX OK" : " · CFX Pending";
            }
            if (mMainActivity.ff3.status >= 0 && (mMainActivity.ff3.status & 16) == 0)
            {
                text += " · 보상 기록 확인 필요";
            }

        }
        String detail = text;
        if (v46 && busy)
        {
            text += " · 처리 중";
        }
        else if (v46 && mMainActivity.ff3.state == todoc.cochlear.remoteapp.ff.Ff3Session.State.ERROR)
        {
            text += " · FF 설정 재확인 필요";
        }
        else if (v46 && mMainActivity.ff3.editing())
        {
            text += " · 미저장 편집 있음";
        }
        mRemoteControlBinding.otaFfStatusTextview.setText(text);
        mRemoteControlBinding.otaFfStatusTextview.setContentDescription(detail
            + (v46 ? ". " + mMainActivity.ff3.message : ""));
    }

    private void updateStepUpText()
    {
        int step = mStatusViewModel.getValueTxStepUp();
        String text = (step == PacketInfo.LINK_VALUE_UNKNOWN) ? "Step -" : ("Step " + step);

        mRemoteControlBinding.otaStepUpTextview.setText( //
            makeLinkStatusText(PacketInfo.RC_LINK_IDX_TX_POWER_STEP_UP, "Step", text));
    }

    private void updateBacktelText()
    {
        int period100ms = mStatusViewModel.getValueBacktelPeriod();
        String text;

        if (period100ms == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            text = "BT -";
        }
        else
        {
            text = "BT " + (period100ms * PacketInfo.BACKTEL_PERIOD_UNIT_MS) + "ms";
        }

        mRemoteControlBinding.otaBacktelTextview.setText( //
            makeLinkStatusText(PacketInfo.RC_LINK_IDX_BACKTEL_PERIOD, "BT", text));
    }

    private void updateCtrlModeText()
    {
        int value = mStatusViewModel.getValueLinkCtrlMode();
        String text;

        if (value == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            text = "Mode -";
        }
        else
        {
            /* 사운드처리기가 쓰는 이름을 그대로 줄인 것이다.
             * isd_interface.c 의 tdc_get_link_ctrl_mode_name() 이
             * "ISD POWER STATE" 와 "BACKTEL PRESENCE" 를 돌려준다.
             *
             * ISD = 내부기가 보고한 전원 상태를 보고 판정한다 (기존 동작)
             * BT  = 백텔이 왔는지 안 왔는지만 보고 판정한다 */
            text = "Mode " + ((value == PacketInfo.LINK_CTRL_MODE_BACKTEL) ? "BT" : "ISD");
        }

        mRemoteControlBinding.otaCtrlModeTextview.setText( //
            makeLinkStatusText(PacketInfo.RC_LINK_IDX_CTRL_MODE, "Mode", text));
    }

    private void updateAccelText()
    {
        int value = mStatusViewModel.getValueTxPowerAccel();
        String text;

        if (value == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            text = "Accel -";
        }
        else
        {
            text = "Accel " + ((value == PacketInfo.LINK_FLAG_ENABLE) ? "On" : "Off");
        }

        mRemoteControlBinding.otaAccelTextview.setText( //
            makeLinkStatusText(PacketInfo.RC_LINK_IDX_TX_POWER_ACCEL, "Accel", text));
    }

    /* 지금 화면에 코인 «값» 을 보여 주는 것이 의미가 있는가.
     *
     * 4.4 부터는 백텔 수신 기준 모드에서 개수만 무시되고 무한은 유효하다. 그래서
     * 버튼 잠금(isLinkSettingUsable)과 답이 갈린다 — 버튼은 열려 있는데 개수는 N/A 인
     * 상태가 정상이다. 4.3 이하에서는 둘이 같은 답을 낸다. */
    private boolean isCoinValueMeaningful()
    {
        if (mMainActivity == null)
        {
            return true;
        }

        return mMainActivity.isCoinCountSupported() //
            ? mMainActivity.isCoinCountEffective() //
            : mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_ONE_COIN);
    }

    private void updateCoinText()
    {
        int infinite = mStatusViewModel.getValueInfiniteCoin();
        int oneCoin = mStatusViewModel.getValueOneCoin();
        String text;

        /* infinite coin 이 없는 세대에서는 그 값을 «못 읽은 것» 으로 보면 안 된다.
         * 그러면 one coin 을 멀쩡히 읽고도 화면이 «Coin -» 로 남는다.
         * 표시의 근거는 one coin 이고, infinite 는 그것을 덮어쓰는 값일 뿐이다. */
        boolean hasInfinite = (mMainActivity != null) //
            && mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_INFINITE_COIN);

        /* 무한은 두 인덱스 어느 쪽으로 켜져 있어도 무한이다.
         * 4.4 부터는 인덱스 7 의 255 가, 그 전에는 인덱스 8 의 1 이 근거다. */
        if (mMainActivity != null && mMainActivity.isCoinInfinite())
        {
            text = "Coin " + COIN_INFINITE_MARK;
        }
        else if (mMainActivity != null //
            && mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_ONE_COIN) //
            && !isCoinValueMeaningful())
        {
            /* 백텔 수신 기준 모드에는 one coin 참조가 아예 없다.
             *
             * 다만 infinite 는 그 모드에서도 유효하다. 끊김 판정 자체를 건너뛰는 값이라
             * 제어 모드와 무관하기 때문이다. 그래서 위에서 무한을 먼저 걸러 낸 뒤에
             * 여기로 온다. 순서를 바꾸면 무한인데도 N/A 로 덮인다. */
            text = "Coin N/A";
        }
        else if (oneCoin == PacketInfo.LINK_VALUE_UNKNOWN //
            || (hasInfinite && infinite == PacketInfo.LINK_VALUE_UNKNOWN //
            && (mMainActivity == null || !mMainActivity.isCoinCountSupported())))
        {
            /* 4.4 부터는 인덱스 7 하나로 상태가 다 드러난다. 인덱스 8 을 아직 못 읽었어도
             * 개수를 아는 이상 «미상» 이 아니다. 4.3 이하에서는 infinite 가 one coin 을
             * 덮어쓰므로 둘 다 알아야 표시할 수 있다. */
            text = "Coin -";
        }
        else if (mMainActivity != null && mMainActivity.isCoinCountSupported())
        {
            /* 4.4 부터는 개수다. 0 은 비활성이고 1~100 은 그 횟수만큼 견딘다. */
            text = "Coin " + ((oneCoin == PacketInfo.COIN_COUNT_DISABLE) ? "Off" : String.valueOf(oneCoin));
        }
        else
        {
            text = "Coin " + ((oneCoin == PacketInfo.LINK_FLAG_ENABLE) ? "On" : "Off");
        }

        mRemoteControlBinding.otaOneCoinTextview.setText(text);
    }

    // Tx 파워 레벨(1스텝 = 25mV)을 "이름 4.2V" 형태로 만든다. 아직 못 읽었으면 "이름 -" 이다.

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 판별된 펌웨어 세대를 화면에 반영
    ///
    /// 세대마다 사운드처리기가 아는 0x59 옵션이 다르다. 모르는 옵션을 보내면 에러로
    /// 응답하거나 아예 응답하지 않아 연결이 끊기므로, 애초에 누를 수 없게 잠근다.
    ///
    /// 숨기지 않고 잠그는 이유는 화면의 자리 배치를 흔들지 않기 위해서다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void applyFwRelease()
    {
        if (mRemoteControlBinding == null || mMainActivity == null)
        {
            return;
        }

        int fwRelease = mStatusViewModel.getValueFwRelease();

        // 배지
        /* 이름은 액티비티가 만든다. 화면과 로그와 Toast 가 같은 글자를 쓰게 하려는 것이다.
         * 버전 조회가 되는 세대는 «REL4.3» 처럼 읽어 온 버전이 그대로 나온다. */
        String label = (fwRelease == Status.FW_RELEASE_UNKNOWN) ? "R—" : mMainActivity.fwReleaseName().replaceFirst("^REL-?", "R");

        TextView badge = mRemoteControlBinding.otaFwReleaseTextview;
        badge.setText(label);
        badge.setVisibility(View.VISIBLE);
        updateIdentityBadges();

        /* 링크 파라미터 버튼. 세대가 그 인덱스를 아는지에 더해, 지금 설정 조합에서
         * 의미가 있는지까지 본다. 제어 모드를 바꾸면 one coin 과 가속의 유효성이 뒤바뀌고,
         * 백텔이 안 나가는 맵에서는 링크 설정 대부분이 통째로 무의미해진다. */
        setButtonEnabled(mRemoteControlBinding.otaPmicSelectButton, mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_MIN_TX_POWER));
        setButtonEnabled(mRemoteControlBinding.otaMappingPmicButton, mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_MAPPING_MIN_POWER));
        setButtonEnabled(mRemoteControlBinding.otaBtSelectButton, mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_BACKTEL_PERIOD));
        setButtonEnabled(mRemoteControlBinding.otaLinkStepButton, mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_TX_POWER_STEP_UP));
        setButtonEnabled(mRemoteControlBinding.otaFixedPmicButton, mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_FORCE_TX_POWER));
        setButtonEnabled(mRemoteControlBinding.otaOneCoinButton, mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_ONE_COIN));
        setButtonEnabled(mRemoteControlBinding.otaCtrlModeButton, mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_CTRL_MODE));
        setButtonEnabled(mRemoteControlBinding.otaAccelButton, mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_TX_POWER_ACCEL));
        setButtonEnabled(mRemoteControlBinding.otaNopStandbyButton, mMainActivity.isLinkSettingUsable(PacketInfo.RC_LINK_IDX_POWER_STABLE_NOP));

        // 맵 초기화는 REL4 부터, 게이팅은 REL3 부터 있다.
        setButtonEnabled(mRemoteControlBinding.otaMapSelectButton, mMainActivity.isMapInitSupported());
        setButtonEnabled(mRemoteControlBinding.otaMapInitDefaultButton, mMainActivity.isMapInitSupported());
        setButtonEnabled(mRemoteControlBinding.otaGatingButton, mMainActivity.isGatingSupported());

        /* 이 세대에 «없는» 상태 항목은 취소선으로 그어 둔다.
         *
         * 값이 «-» 이면 아직 못 읽은 것인지 원래 없는 것인지 알 수 없다. 둘은 전혀 다른
         * 이야기다. 못 읽은 것은 기다리면 되지만, 없는 것은 기다려도 오지 않는다.
         * 그래서 없는 항목은 그어서 «이 기기에는 해당 없음» 으로 읽히게 한다.
         *
         * 자리는 비우지 않는다. 격자의 칸이 옮겨 다니면 눈이 위치를 다시 익혀야 한다. */
        markUnsupported(mRemoteControlBinding.otaCtrlModeTextview, PacketInfo.RC_LINK_IDX_CTRL_MODE);
        markUnsupported(mRemoteControlBinding.otaAccelTextview, PacketInfo.RC_LINK_IDX_TX_POWER_ACCEL);
        markUnsupported(mRemoteControlBinding.otaNopStandbyTextview, PacketInfo.RC_LINK_IDX_POWER_STABLE_NOP);
        markUnsupported(mRemoteControlBinding.otaPulseWidthTextview, PacketInfo.RC_LINK_IDX_PULSE_WIDTH);
        markUnsupported(mRemoteControlBinding.otaFrameNumTextview, PacketInfo.RC_LINK_IDX_FRAME_NUM);
        markUnsupported(mRemoteControlBinding.otaStrategyTextview, PacketInfo.RC_LINK_IDX_STIM_STRATEGY);

        /* 심각도 높은 상태 셋은 값 자체를 붉게 칠한다. 잠그는 것이 아니라 알리는 것이다.
         * 시험 중에는 의도해서 켜 놓는 상태라 막으면 시험이 막힌다. */
        markAlert(mRemoteControlBinding.otaFrameNumTextview, mMainActivity.isLinkFrozen(), MAP_COLOR);
        markAlert(mRemoteControlBinding.otaForcePmicTextview, //
            mStatusViewModel.getValueForceTxPowerLevel() > PacketInfo.FORCE_TX_PWR_RELEASE, //
            VOLT_COLOR);
        markAlert(mRemoteControlBinding.otaOneCoinTextview, //
            mMainActivity != null && mMainActivity.isCoinInfinite(), //
            TIMING_COLOR);

        /* 무엇이 쓰이는지가 바뀌었으니 글자도 다시 만든다.
         * 옵저버는 그 «값» 이 바뀔 때만 오므로, 모드가 바뀌어 쓰임이 달라진 것은 여기서 반영한다. */
        updateMinPowerText();
        updateMaxPowerText();
        updateStepUpText();
        updateBacktelText();
        updateCtrlModeText();
        updateAccelText();
        updateCoinText();
        updateNopText();


        String warning = mMainActivity.makeLinkWarningText();

        if (!warning.isEmpty())
        {
            Log.i(TAG, "[LINK] 지금 조합의 경고\n" + warning);
        }
    }

    /* 상태 격자에서 쓰는 분류 색. 경고를 껐을 때 되돌릴 원래 색이다.
     * 레이아웃과 값이 같아야 하므로 한곳에 모아 둔다. */
    static private final int VOLT_COLOR = 0xFFFFD54F;
    static private final int TIMING_COLOR = 0xFF4DD0E1;
    static private final int MAP_COLOR = 0xFFF06292;
    static private final int ALERT_COLOR = 0xFFFF5252;

    // 경고 상태면 붉게, 아니면 원래 분류 색으로 되돌린다.
    private void markAlert(TextView view, boolean alert, int normalColor)
    {
        view.setTextColor(alert ? ALERT_COLOR : android.graphics.Color.rgb(233, 240, 242));
    }

    /* 상태 칸 하나에 «이 세대엔 없음» 표시를 켜거나 끈다.
     *
     * 판별 전(세대 미상)에는 긋지 않는다. 아직 모르는 것이지 없는 것이 아니다. */
    private void markUnsupported(TextView view, int linkIndex)
    {
        boolean unknownYet = (mStatusViewModel.getValueFwRelease() == Status.FW_RELEASE_UNKNOWN);
        boolean unsupported = !unknownYet && !mMainActivity.isLinkParamSupported(linkIndex);

        int flags = view.getPaintFlags();

        if (unsupported)
        {
            flags |= Paint.STRIKE_THRU_TEXT_FLAG;
        }
        else
        {
            flags &= ~Paint.STRIKE_THRU_TEXT_FLAG;
        }

        view.setPaintFlags(flags);
        view.setAlpha(unsupported ? 0.4f : 1.0f);
    }

    // 잠긴 버튼은 눌리지 않고 흐리게 보인다. 자리는 그대로 차지한다.
    private void setButtonEnabled(View button, boolean enabled)
    {
        enabled = enabled && !selectedOtaControlsPaused();
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.35f);
    }

    private String makeVoltageText(String label, int level)
    {
        if (level == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            return label + " -";
        }

        return String.format("%s %.3f Ｖ", label, (level * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f);
    }

    private void showControlDialog(AlertDialog dialog)
    {
        dialog.show();
        // 특수 제어의 선택 목록은 아래에 두어 위쪽 상태를 계속 읽을 수 있게 한다.
        if (selectedControlTab == 1 && dialog.getListView() != null && dialog.getWindow() != null)
        {
            float density = getResources().getDisplayMetrics().density;
            android.view.Window window = dialog.getWindow();
            window.setDimAmount(0.12f);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xffb8d8cf);
            android.view.WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.BOTTOM;
            params.y = Math.round(58 * density);
            window.setAttributes(params);
            ViewGroup.LayoutParams list = dialog.getListView().getLayoutParams();
            list.height = Math.round(160 * density);
            dialog.getListView().setLayoutParams(list);
            window.setLayout(getResources().getDisplayMetrics().widthPixels - Math.round(24 * density), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void updatePageTitle()
    {
        if (mMainActivity != null)
        {
            String[] titles = {"기본 제어", "특수 제어", "FF", "OTA"};
            mMainActivity.updateRemotePageTitle(titles[Math.max(0, Math.min(3, selectedControlTab))]);
        }
    }

    private void updateIdentityBadges()
    {
        if (mRemoteControlBinding == null)
        {
            return;
        }
        String product = mMainActivity.getSessionProductBadge(mMainActivity.getSelectedSessionId());
        mRemoteControlBinding.identityProductTextview.setVisibility(product.isEmpty() ? View.GONE : View.VISIBLE);
        setOtaText(mRemoteControlBinding.identityProductTextview, product);
        mRemoteControlBinding.identityProductTextview.setContentDescription(product.equals("SLVN") ? "SULLIVAN 제품"
            : product.equals("SND1") ? "SOUND1 제품" : "제품 미확인");
        EntityUser user = mMainActivity.mStatus.connectedUser;

        String name = "—";
        String ear = "—";
        if (user != null)
        {
            name = user.nickname != null && !user.nickname.isEmpty() ? user.nickname :
                user.name != null && user.name.length() > 2 ? UtilUser.getNameOnly(user.name) : user.name;
            ear = user.ear == null ? "—" : user.ear;
        }
        mRemoteControlBinding.remoteControlConnectionUserNameTextview.setText(name);
        mRemoteControlBinding.identitySideTextview.setText(ear);
        mRemoteControlBinding.identityExternalTextview.setText(mMainActivity.mStatus.connectedDevice == null ? "—" :
            mMainActivity.mStatus.connectedDevice.serialNumber);
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
                    updateIdentityBadges();
                    // Searching starts only from the explicit add/search control.
                    updateConnectionOverlay();
                }
            }
        }
    }

    /* 아래 안내 다이얼로그들은 모두 «닫히면 mDialog 를 비운다».
     *
     * 예전에는 그 처리가 없어서 한 번 뜨고 나면 mDialog 가 계속 남았다. 그러면 다른
     * 버튼(Coin · PMIC · Nop 등)이 «이미 다이얼로그가 표시 중» 으로 막혀 아무것도
     * 뜨지 않는다. 화면을 나갔다 오면 프래그먼트가 다시 만들어져 풀리던 것이 그 증상이다. */
    public void makeDialog_finishOTA(int fileCount)
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
                .setTitle("안내") //
                .setMessage("OTA 이미지 전송이 완료되었습니다.\n전송한 파일 " + fileCount + "개") //
                .setPositiveButton("확인", null) //
                .setCancelable(false) //
                .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }

    public void makeDialog_withMessage(String message)
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
                .setTitle("안내") //
                .setMessage(message) //
                .setPositiveButton("확인", null) //
                .setCancelable(false) //
                .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }

    // OTA 수집 미완료 다이얼로그
    private void makeDialog_readImage_notFinish()
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
                .setTitle("안내") //
                .setMessage("OTA 이미지 수집이 완료되지 않았습니다.") //
                .setPositiveButton("확인", null) //
                .setCancelable(false) //
                .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }

    // OTA 패킷 사이즈 에러 다이얼로그
    private void makeDialog_invalidPacketSize()
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
                .setTitle("안내") //
                .setMessage("패킷 사이즈 에러 발생") //
                .setPositiveButton("확인", null) //
                .setCancelable(false) //
                .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }

    // OTA 결과 에러
    private void makeDialog_invalidResult()
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
                .setTitle("안내") //
                .setMessage("OTA 결과가 성공이 아님") //
                .setPositiveButton("확인", null) //
                .setCancelable(false) //
                .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }

    // OTA 데이터 인덱스 에러 다이얼로그
    private void makeDialog_invalidDataIndex()
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
                .setTitle("안내") //
                .setMessage("OTA 데이터 인덱스 에러 발생") //
                .setPositiveButton("확인", null) //
                .setCancelable(false) //
                .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }

    // OTA 전송 미완료 다이얼로그
    private void makeDialog_sendImage_notFinish()
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
                .setTitle("안내") //
                .setMessage("OTA 이미지 전송이 완료되지 않았습니다.") //
                .setPositiveButton("확인", null) //
                .setCancelable(false) //
                .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }

    // OTA 파일 없음 다이얼로그
    private void makeDialog_noFileOTA()
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()) //
                .setTitle("안내") //
                .setMessage("OTA 이미지가 준비되지 않았습니다.") //
                .setPositiveButton("확인", null) //
                .setCancelable(false) //
                .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }

    // NO 사용자 다이얼로그
    private void makeDialog_noUser()
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()).setTitle(getString(R.string.remote_control_no_user_dialog_title)).setMessage(getString(R.string.remote_control_no_user_dialog_message)).setPositiveButton(getString(R.string.remote_control_no_user_positive), (dialogInterface, i) ->
            {

                mDialog.dismiss();
                mDialog = null;
                ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.USER_ADD);
            }).setCancelable(false).create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }

    // NO 사운드처리기 다이얼로그
    private void makeDialog_noDevice()
    {
        if (mDialog == null)
        {
            mDialog = new todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder(requireContext()).setTitle(getString(R.string.remote_control_no_device_dialog_title)).setMessage(getString(R.string.remote_control_no_device_dialog_message)).setPositiveButton(getString(R.string.remote_control_no_device_positive), (dialogInterface, i) ->
            {

                mDialog.dismiss();
                mDialog = null;
                ((MainActivity) requireActivity()).replaceFragment(Status.TypeOfFragment.DEVICE_ADD);
            }).setCancelable(false).create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            showControlDialog(mDialog);
        }
    }


    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - get collect size tv
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private TextView getCollectSizeTextView(int fileNum)
    {
        TextView tv = null;

        switch (fileNum)
        {
            case Ota.FILE_NUM_MFST:
                tv = mRemoteControlBinding.mfstCollectSizeTextview;
                break;

            case Ota.FILE_NUM_APP0:
                tv = mRemoteControlBinding.app0CollectSizeTextview;
                break;

            case Ota.FILE_NUM_APP1:
                tv = mRemoteControlBinding.app1CollectSizeTextview;
                break;

            case Ota.FILE_NUM_APP2:
                tv = mRemoteControlBinding.app2CollectSizeTextview;
                break;
        }

        return tv;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - get collect percent tv
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private TextView getCollectPercentTextView(int fileNum)
    {
        TextView tv = null;

        switch (fileNum)
        {
            case Ota.FILE_NUM_MFST:
                tv = mRemoteControlBinding.mfstCollectPercentTextview;
                break;

            case Ota.FILE_NUM_APP0:
                tv = mRemoteControlBinding.app0CollectPercentTextview;
                break;

            case Ota.FILE_NUM_APP1:
                tv = mRemoteControlBinding.app1CollectPercentTextview;
                break;

            case Ota.FILE_NUM_APP2:
                tv = mRemoteControlBinding.app2CollectPercentTextview;
                break;
        }

        return tv;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - get write size tv
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private TextView getWriteSizeTextView(int fileNum)
    {
        TextView tv = null;

        switch (fileNum)
        {
            case Ota.FILE_NUM_MFST:
                tv = mRemoteControlBinding.mfstWriteSizeTextview;
                break;

            case Ota.FILE_NUM_APP0:
                tv = mRemoteControlBinding.app0WriteSizeTextview;
                break;

            case Ota.FILE_NUM_APP1:
                tv = mRemoteControlBinding.app1WriteSizeTextview;
                break;

            case Ota.FILE_NUM_APP2:
                tv = mRemoteControlBinding.app2WriteSizeTextview;
                break;
        }

        return tv;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - get write percent tv
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private TextView getWritePercentTextView(int fileNum)
    {
        TextView tv = null;

        switch (fileNum)
        {
            case Ota.FILE_NUM_MFST:
                tv = mRemoteControlBinding.mfstWritePercentTextview;
                break;

            case Ota.FILE_NUM_APP0:
                tv = mRemoteControlBinding.app0WritePercentTextview;
                break;

            case Ota.FILE_NUM_APP1:
                tv = mRemoteControlBinding.app1WritePercentTextview;
                break;

            case Ota.FILE_NUM_APP2:
                tv = mRemoteControlBinding.app2WritePercentTextview;
                break;
        }

        return tv;
    }

}
