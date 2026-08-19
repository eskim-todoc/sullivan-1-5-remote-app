package todoc.cochlear.remoteapp.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
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
import todoc.cochlear.remoteapp.params.PacketInfo;
import todoc.cochlear.remoteapp.params.Status;
import todoc.cochlear.remoteapp.view_model.StatusViewModel;
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

        liveDataProgramAndLevels(); // LiveData for 프로그램 번호 / 볼륨 / 최대출력

        checkRegisteredList_userAndDevice(); // User list

        mOta = Ota.getInstance();

        mRemoteControlBinding.slotRadioButtonGroup.setOnCheckedChangeListener(onChecked_radioGroup);

        mRemoteControlBinding.otaInfoButton.setOnClickListener(onClick_infoButton);
        mRemoteControlBinding.otaCollectButton.setOnClickListener(onClick_collectButton);
        mRemoteControlBinding.otaWriteButton.setOnClickListener(onClick_writeButton);
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
        mRemoteControlBinding.otaPmicSelectButton.setOnClickListener(onClick_pmicSelectButton);
        mRemoteControlBinding.otaMappingPmicButton.setOnClickListener(onClick_mappingPmicButton);
        mRemoteControlBinding.otaBtSelectButton.setOnClickListener(onClick_btSelectButton);
        mRemoteControlBinding.otaLinkStepButton.setOnClickListener(onClick_linkStepButton);
        mRemoteControlBinding.otaFixedPmicButton.setOnClickListener(onClick_fixedPmicButton);
        mRemoteControlBinding.otaOneCoinButton.setOnClickListener(onClick_oneCoinButton);
        mRemoteControlBinding.otaCtrlModeButton.setOnClickListener(onClick_ctrlModeButton);
        mRemoteControlBinding.otaAccelButton.setOnClickListener(onClick_accelButton);

        mRemoteControlBinding.otaNopStandbyButton.setOnClickListener(onClick_nopStandbyButton);

        /* 아직 연결 전이라 세대가 미상이다. 잠긴 상태로 시작한다.
         * 옵저버는 값이 «바뀔 때» 만 오므로 첫 상태는 여기서 직접 맞춰야 한다. */
        applyFwRelease();
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
    static private final int[] OTA_FILE_ORDER = {Ota.FILE_NUM_MFST, Ota.FILE_NUM_APP0, Ota.FILE_NUM_APP1, Ota.FILE_NUM_APP2};

    Runnable collectRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (mOta.threadState == Ota.THREAD_STATE_BUSY)
            {
                Log.d(TAG, "[OTA][THREAD] BUSY 상태라서 collectThread 동작 실패");
                return;
            }

            mOta.threadState = Ota.THREAD_STATE_BUSY;

            int slotNum = mOta.currentSlotNum;
            int found   = 0;

            for (int fileNum : OTA_FILE_ORDER)
            {
                if (collectOneFile(slotNum, fileNum))
                {
                    found++;
                }
            }

            Log.d(TAG, "[OTA][THREAD] 순차 수집 완료 : 슬롯 " + slotNum + ", 읽은 파일 " + found + "개");

            final int foundCount = found;

            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(requireContext(), //
                                                                         "수집 완료 : " + foundCount + "개 파일", //
                                                                         Toast.LENGTH_SHORT).show());

            mOta.threadState = Ota.THREAD_STATE_IDLE;
        }
    };

    /* 파일 하나를 버퍼로 읽는다. 읽었으면 true, 파일이 없으면 false 다.
     * 호출하는 쪽이 이미 스레드이므로 여기서 스레드 상태를 건드리지 않는다. */
    private boolean collectOneFile(int slotNum, int fileNum)
    {
        Ota.OtaFile otaFile  = Ota.getFile(slotNum, fileNum);
        String      filePath = makePath(slotNum, fileNum);
        File        objFile  = new File(filePath);

        TextView collectSizeTv;
        TextView collectPercentTv;

        switch (fileNum)
        {
            case Ota.FILE_NUM_MFST:
                collectSizeTv = mRemoteControlBinding.mfstCollectSizeTextview;
                collectPercentTv = mRemoteControlBinding.mfstCollectPercentTextview;
                break;

            case Ota.FILE_NUM_APP0:
                collectSizeTv = mRemoteControlBinding.app0CollectSizeTextview;
                collectPercentTv = mRemoteControlBinding.app0CollectPercentTextview;
                break;

            case Ota.FILE_NUM_APP1:
                collectSizeTv = mRemoteControlBinding.app1CollectSizeTextview;
                collectPercentTv = mRemoteControlBinding.app1CollectPercentTextview;
                break;

            case Ota.FILE_NUM_APP2:
                collectSizeTv = mRemoteControlBinding.app2CollectSizeTextview;
                collectPercentTv = mRemoteControlBinding.app2CollectPercentTextview;
                break;

            default:
                Log.d(TAG, "[OTA][THREAD] 유효 하지 않은 FILE 번호 (" + fileNum + ")");
                return false;
        }

        Log.d(TAG, "[OTA][THREAD] 파일 " + filePath + "의 상태 초기화");

        otaFile.collectSize = 0;
        otaFile.collectPercent = 0;
        otaFile.writeSize = 0;
        otaFile.writePercent = 0;
        otaFile.totalBytes = 0;

        if (!objFile.exists())
        {
            new Handler(Looper.getMainLooper()).post(() ->
            {
                collectSizeTv.setText(otaFile.collectSize + "");
                collectPercentTv.setText(otaFile.collectPercent + "%");
            });

            Log.d(TAG, "파일 없음 : " + filePath);
            return false;
        }

        otaFile.totalBytes = (int) objFile.length();

        Log.d(TAG, "[OTA][THREAD] 파일 찾음 : " + filePath + ", " + otaFile.totalBytes + " bytes");

        if (otaFile.buffer == null || otaFile.buffer.length < otaFile.totalBytes)
        {
            otaFile.buffer = new byte[otaFile.totalBytes];
        }

        for (int i = 0; i < otaFile.totalBytes; i++)
        {
            otaFile.buffer[i] = 0;
        }

        try (FileInputStream fis = new FileInputStream(objFile))
        {
            int b;
            int lastPercent = -1;

            while ((b = fis.read()) != -1)
            {
                otaFile.buffer[otaFile.collectSize++] = (byte) (b & 0xFF);
                otaFile.collectPercent = (int) ((otaFile.collectSize * 100) / otaFile.totalBytes);

                // 100%는 항상 마지막이므로 99%->100%가 될 때, 항상 이 구문을 수행할 수 밖에 없음
                if (otaFile.collectPercent != lastPercent)
                {
                    lastPercent = otaFile.collectPercent;

                    new Handler(Looper.getMainLooper()).post(() ->
                    {
                        collectSizeTv.setText(otaFile.collectSize + "");
                        collectPercentTv.setText(otaFile.collectPercent + "%");
                    });
                }
            }

            fis.close();
        }
        catch (IOException e)
        {
            Log.d(TAG, "[OTA][THREAD] 익셉션 발생.");

            new Handler(Looper.getMainLooper()).post(() ->
            {
                collectSizeTv.setText(otaFile.collectSize + "");
                collectPercentTv.setText(otaFile.collectPercent + "%");
            });

            e.printStackTrace();
            return false;
        }

        printBuffer(0, null);

        return (otaFile.collectPercent == 100);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Runnable - write
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* MFST 부터 APP2 까지 순서대로 전송한다.
     *
     * 무선 전송은 응답을 받아 가며 이어지는 비동기 동작이라, 한 파일이 끝난 자리에서
     * 다음 파일을 시작하는 방식으로 잇는다. 네 개를 한꺼번에 밀어 넣을 수는 없다.
     *
     * 수집되지 않은 파일은 건너뛴다. 슬롯에 APP 이 세 개가 아닐 수 있기 때문이다. */
    private int mWriteOrderIndex = -1; // -1 = 순차 전송 중이 아님
    private int mWriteDoneCount  = 0;

    /* 전송을 시작하기 전에 매핑 연결(0x60) 응답을 기다리는 중인지.
     *
     * 사운드처리기는 매핑 프로그램이 붙은 상태에서만 이미지를 받는다. 그래서 Write 는
     * 0x60 을 먼저 보내고, 그 응답을 받은 자리에서 실제 전송을 시작한다.
     * 응답을 안 기다리고 바로 보내면 사운드처리기가 아직 준비되지 않은 상태다. */
    private boolean mWaitingMappingConnect = false;

    Runnable writeRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (mOta.commState != Ota.COMM_STATE_IDLE)
            {
                Log.d(TAG, "[OTA] 무선 전송 상태가 IDLE이 아님");
                return;
            }

            if (mWaitingMappingConnect)
            {
                Log.d(TAG, "[OTA] 이미 매핑 연결 응답을 기다리는 중입니다.");
                return;
            }

            // 보낼 것이 하나도 없으면 매핑 연결부터 할 이유가 없다.
            if (!hasCollectedFile())
            {
                Log.d(TAG, "[OTA] 전송할 파일이 없습니다. 먼저 Collect 를 하세요.");

                Toast.makeText(requireContext(), //
                               "전송할 파일이 없습니다.\n먼저 Collect 를 눌러 주세요.", //
                               Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "[OTA] 매핑 연결(0x60) 요청 후 전송을 시작합니다.");

            mWaitingMappingConnect = true;

            byte[] packet = new byte[PacketInfo.PACKET_SIZE_MAPPING_CONNECT_SEND];

            packet[0] = PacketInfo.HEADER_MAPPING_CONNECT;

            mMainActivity.sendPacket(packet);
        }
    };

    /* 연결이 끊겼을 때 액티비티가 부른다. 진행 중이던 전송 순서와 대기를 모두 접는다. */
    public void onDisconnectedForOta()
    {
        if (mWaitingMappingConnect || mWriteOrderIndex >= 0)
        {
            Log.d(TAG, "[OTA] 연결이 끊겨 전송을 접습니다.");
        }

        mWaitingMappingConnect = false;
        mWriteOrderIndex = -1;
    }

    // 수집이 끝난 파일이 하나라도 있는지.
    private boolean hasCollectedFile()
    {
        for (int fileNum : OTA_FILE_ORDER)
        {
            if (Ota.getFile(mOta.currentSlotNum, fileNum).collectPercent == 100)
            {
                return true;
            }
        }

        return false;
    }

    /* 매핑 연결 응답을 받은 자리. 여기서부터가 원래의 Write 동작이다.
     * 액티비티의 수신 분기가 호출한다. */
    public void onMappingConnected()
    {
        if (!mWaitingMappingConnect)
        {
            Log.d(TAG, "[OTA] 전송 대기 중이 아닌데 매핑 연결 응답이 왔습니다. 무시합니다.");
            return;
        }

        mWaitingMappingConnect = false;

        if (mOta.commState != Ota.COMM_STATE_IDLE)
        {
            Log.d(TAG, "[OTA] 매핑 연결은 되었으나 무선 전송 상태가 IDLE이 아님");
            return;
        }

        mWriteOrderIndex = -1;
        mWriteDoneCount = 0;

        if (!startNextWriteFile())
        {
            Log.d(TAG, "[OTA] 매핑 연결 후에도 전송할 파일이 없습니다.");

            Toast.makeText(requireContext(), //
                           "전송할 파일이 없습니다.\n먼저 Collect 를 눌러 주세요.", //
                           Toast.LENGTH_SHORT).show();
        }
    }

    /* 다음으로 전송할 파일을 찾아 시작한다. 시작했으면 true, 남은 것이 없으면 false 다.
     * 수집이 끝나지 않은(100%가 아닌) 파일은 그 자리에서 건너뛴다. */
    private boolean startNextWriteFile()
    {
        while (true)
        {
            mWriteOrderIndex++;

            if (mWriteOrderIndex >= OTA_FILE_ORDER.length)
            {
                mWriteOrderIndex = -1;
                return false;
            }

            int         fileNum = OTA_FILE_ORDER[mWriteOrderIndex];
            Ota.OtaFile otaFile = Ota.getFile(mOta.currentSlotNum, fileNum);

            if (otaFile.collectPercent != 100)
            {
                Log.d(TAG, "[OTA] " + Ota.getFileName(fileNum) + " 는 수집되지 않아 건너뜁니다.");
                continue;
            }

            startWriteFile(fileNum, otaFile);
            return true;
        }
    }

    // 파일 하나의 전송을 시작한다. 첫 커맨드 패킷을 보내는 데까지가 여기다.
    private void startWriteFile(int fileNum, Ota.OtaFile otaFile)
    {
        Log.d(TAG, "[OTA] 전송 시작 : 슬롯 " + mOta.currentSlotNum + ", " + Ota.getFileName(fileNum) + ", " + otaFile.totalBytes + " bytes");

        /* 진행률 표시가 mOta.currentFileNum 을 보고 칸을 고르므로 여기서 맞춰 둔다.
         * 파일 라디오 버튼이 없어진 뒤로 이 값을 정하는 곳은 여기뿐이다. */
        mOta.currentFileNum = fileNum;

        mOta.commState = Ota.COMM_STATE_PREPARE_COMMAND;

        mOta.commDataIndex = 0;
        mOta.commSlotNum = mOta.currentSlotNum;
        mOta.commFileNum = fileNum;
        mOta.commOption = 1;
        mOta.commTotalByte = otaFile.totalBytes;
        // 마지막 인덱스를 아래와 같이 계산하면, 계산 오류로 데이터 인덱스가 1 적게 계산되는 경우를 방지할 수 있다.
        mOta.commEndDataIndexNum = (otaFile.totalBytes + PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT - 1) / PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT;
        mOta.commEndDataIndexByte = otaFile.totalBytes % PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT;
        mOta.commBufferIndex = 0;
        mOta.commBuffer = otaFile.buffer;

        // 마지막 인덱스의 데이터 크기가 0이면, 나머지 연산 결과가 0인 것이다. 이는 곧, 마지막 인덱스에 최대 크기 바이트로 전송해야 한다는 것을 의미한다.
        if (mOta.commEndDataIndexByte == 0)
        {
            mOta.commEndDataIndexByte = PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT;
        }

        byte[] packet = new byte[18];

        packet[0] = PacketInfo.HEADER_OTA; // Header

        packet[1] = (byte) ((mOta.commDataIndex >> 16) & 0xFF);
        packet[2] = (byte) ((mOta.commDataIndex >> 8) & 0xFF);
        packet[3] = (byte) (mOta.commDataIndex & 0xFF);

        packet[4] = (byte) (mOta.commSlotNum & 0xFF);

        packet[5] = (byte) ((mOta.commFileNum >> 8) & 0xFF);
        packet[6] = (byte) (mOta.commFileNum & 0xFF);

        packet[7] = (byte) (mOta.commOption & 0xFF);

        packet[8] = (byte) ((mOta.commTotalByte >> 24) & 0xFF);
        packet[9] = (byte) ((mOta.commTotalByte >> 16) & 0xFF);
        packet[10] = (byte) ((mOta.commTotalByte >> 8) & 0xFF);
        packet[11] = (byte) (mOta.commTotalByte);

        packet[12] = (byte) ((mOta.commEndDataIndexNum >> 24) & 0xFF);
        packet[13] = (byte) ((mOta.commEndDataIndexNum >> 16) & 0xFF);
        packet[14] = (byte) ((mOta.commEndDataIndexNum >> 8) & 0xFF);
        packet[15] = (byte) (mOta.commEndDataIndexNum);

        packet[16] = (byte) ((mOta.commEndDataIndexByte >> 8) & 0xFF);
        packet[17] = (byte) (mOta.commEndDataIndexByte);

        /* 예전에는 여기서 배터리 조회 타이머를 직접 껐다.
         * 지금은 링크 감시 러너가 mOta.commState 를 보고 전송 중에는 스스로 쉬므로
         * 따로 끌 필요가 없다. 전송이 끝나면 다음 주기부터 저절로 재개된다. */
        mMainActivity.sendPacket(packet);

        mOta.commState = Ota.COMM_STATE_WAIT_RESP_COMMAND;
    }

    // 전송이 중간에 실패했을 때 남은 순서를 접는다.
    private void abortSequentialWrite()
    {
        if (mWriteOrderIndex < 0)
        {
            return;
        }

        Log.d(TAG, "[OTA] 순차 전송을 중단합니다. (완료 " + mWriteDoneCount + "개)");

        mWriteOrderIndex = -1;
        mWaitingMappingConnect = false;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Handle - resp data packet
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void handleRespDataPacket(byte[] packet)
    {
        int dataIndex;
        int slotNum;
        int fileNum;
        int option;
        int result;

        if (packet.length < 5)
        {
            Log.d(TAG, "[BLE] OTA 패킷 사이즈 에러 (" + packet.length + ")");
            mOta.commState = Ota.COMM_STATE_IDLE;
            makeDialog_invalidPacketSize();
            return;
        }

        dataIndex = ((packet[1] & 0xFF) << 16) | ((packet[2] & 0xFF) << 8) | (packet[3] & 0xFF);
        result = packet[4];

        if (dataIndex != mOta.commDataIndex)
        {
            Log.d(TAG, "[BLE] OTA 데이터 인덱스 에러, 예상 인덱스 = " + mOta.commDataIndex + ", 받은 인덱스 = " + dataIndex);
            mOta.commState = Ota.COMM_STATE_IDLE;
            abortSequentialWrite();
            makeDialog_invalidDataIndex();
            return;
        }

        if (result != 1)
        {
            Log.d(TAG, "[BLE] OTA 패킷 결과가 성공이 아님, 결과 = " + result);
            mOta.commState = Ota.COMM_STATE_IDLE;
            abortSequentialWrite();
            makeDialog_invalidResult();
            return;
        }

        // Option - Write 인 경우
        if (mOta.commOption == 1)
        {
            int percent = ((dataIndex * 100) / mOta.commEndDataIndexNum);

            getWriteSizeTextView(mOta.currentFileNum).setText(mOta.commBufferIndex + "");
            getWritePercentTextView(mOta.currentFileNum).setText(percent + "%");

            if (mOta.commDataIndex == mOta.commEndDataIndexNum)
            {
                Log.d(TAG, "[BLE] OTA 패킷 전송 완료 : " + Ota.getFileName(mOta.currentFileNum));

                mOta.commState = Ota.COMM_STATE_IDLE;
                mWriteDoneCount++;

                /* 한 파일이 끝난 자리에서 다음 파일을 시작한다.
                 * 남은 것이 없을 때만 완료를 알린다. */
                if (startNextWriteFile())
                {
                    return;
                }

                Log.d(TAG, "[BLE] OTA 순차 전송 완료 : " + mWriteDoneCount + "개");

                makeDialog_finishOTA(mWriteDoneCount);
                return;
            }
            else
            {
                byte[] sendPacket;
                int    sendSize;

                mOta.commState = Ota.COMM_STATE_PREPARE_DATA;

                mOta.commDataIndex++;

                if (mOta.commDataIndex == mOta.commEndDataIndexNum)
                {
                    sendPacket = new byte[4 + mOta.commEndDataIndexByte];
                    sendSize = mOta.commEndDataIndexByte;
                }
                else
                {
                    sendPacket = new byte[4 + PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT];
                    sendSize = PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT;
                }

                sendPacket[0] = PacketInfo.HEADER_OTA;

                sendPacket[1] = (byte) ((mOta.commDataIndex >> 16) & 0xFF); // Data index (MSB to LSB)
                sendPacket[2] = (byte) ((mOta.commDataIndex >> 8) & 0xFF);
                sendPacket[3] = (byte) (mOta.commDataIndex & 0xFF);

                for (int i = 0; i < sendSize; i++)
                {
                    sendPacket[4 + i] = mOta.commBuffer[mOta.commBufferIndex++];
                }

                mMainActivity.sendPacket(sendPacket);
                mOta.commState = Ota.COMM_STATE_WAIT_RESP_DATA;
            }
        }
        else
        {
            mOta.commState = Ota.COMM_STATE_IDLE;
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Handle - resp command packet
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void handleRespCommandPacket(byte[] packet)
    {
        int dataIndex;
        int slotNum;
        int fileNum;
        int option;
        int result;

        // 최소, Option - write, read는 충족하는지 먼저 확인
        if (packet.length < 9)
        {
            Log.d(TAG, "[BLE] OTA 패킷 사이즈 에러 (" + packet.length + ")");
            mOta.commState = Ota.COMM_STATE_IDLE;
            makeDialog_invalidPacketSize();
            return;
        }

        dataIndex = ((packet[1] & 0xFF) << 16) | ((packet[2] & 0xFF) << 8) | (packet[3] & 0xFF);
        slotNum = packet[4];
        fileNum = ((packet[5] & 0xFF) << 8) | (packet[6] & 0xFF);
        option = packet[7];
        result = packet[8];

        // Option - read는 크기가 다르므로 별도로 체크
        if ((option == 3) && (packet.length != 13))
        {
            Log.d(TAG, "[BLE] OTA 패킷 사이즈 에러 (" + packet.length + ")");
            mOta.commState = Ota.COMM_STATE_IDLE;
            makeDialog_invalidPacketSize();
            return;
        }

        if (dataIndex != mOta.commDataIndex)
        {
            Log.d(TAG, "[BLE] OTA 데이터 인덱스 에러, 예상 인덱스 = " + mOta.commDataIndex + ", 받은 인덱스 = " + dataIndex);
            mOta.commState = Ota.COMM_STATE_IDLE;
            abortSequentialWrite();
            makeDialog_invalidDataIndex();
            return;
        }

        if (result != 1)
        {
            Log.d(TAG, "[BLE] OTA 패킷 결과가 성공이 아님, 결과 = " + result);
            mOta.commState = Ota.COMM_STATE_IDLE;
            abortSequentialWrite();
            makeDialog_invalidResult();
            return;
        }

        // Option - Write 인 경우
        if (option == 1)
        {
            mOta.commState = Ota.COMM_STATE_PREPARE_DATA;

            byte[] sendPacket;
            int    sendSize;

            mOta.commDataIndex++;

            if (mOta.commDataIndex == mOta.commEndDataIndexNum)
            {
                sendPacket = new byte[4 + mOta.commEndDataIndexByte];
                sendSize = mOta.commEndDataIndexByte;
            }
            else
            {
                sendPacket = new byte[4 + PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT];
                sendSize = PacketInfo.PACKET_SIZE_OTA_SEND_DATA_UNIT;
            }

            Log.d(TAG, "[BLE] OTA 패킷 다음 전송 사이즈 = " + sendSize);

            sendPacket[0] = PacketInfo.HEADER_OTA;

            sendPacket[1] = (byte) ((mOta.commDataIndex >> 16) & 0xFF); // Data index (MSB to LSB)
            sendPacket[2] = (byte) ((mOta.commDataIndex >> 8) & 0xFF);
            sendPacket[3] = (byte) (mOta.commDataIndex & 0xFF);

            for (int i = 0; i < sendSize; i++)
            {
                sendPacket[4 + i] = mOta.commBuffer[mOta.commBufferIndex++];
            }

            mMainActivity.sendPacket(sendPacket);
            mOta.commState = Ota.COMM_STATE_WAIT_RESP_DATA;
        }
        else
        {
            mOta.commState = Ota.COMM_STATE_IDLE;
        }
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Func: Make path
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String makePath(int slotNum, int fileNum)
    {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + Ota.BASE_FOLDER + slotNum + "/" + Ota.getFileName(fileNum);
    }


    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// OnChecked - radio group
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    RadioGroup.OnCheckedChangeListener onChecked_radioGroup = new RadioGroup.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId)
        {
            mMainActivity.longTimeIdleHandlerUpdate(true);

            // Slot radio group
            if (checkedId == mRemoteControlBinding.slot1RadioButton.getId())
            {
                mOta.currentSlotNum = Ota.SLOT_NUM_1;
                Log.d(TAG, "slotNum = " + mOta.currentSlotNum);

                Ota.OtaFile mfstFile = Ota.getFile(Ota.SLOT_NUM_1, Ota.FILE_NUM_MFST);
                Ota.OtaFile app0File = Ota.getFile(Ota.SLOT_NUM_1, Ota.FILE_NUM_APP0);
                Ota.OtaFile app1File = Ota.getFile(Ota.SLOT_NUM_1, Ota.FILE_NUM_APP1);
                Ota.OtaFile app2File = Ota.getFile(Ota.SLOT_NUM_1, Ota.FILE_NUM_APP2);

                getCollectSizeTextView(Ota.FILE_NUM_MFST).setText(mfstFile.collectSize + "");
                getCollectPercentTextView(Ota.FILE_NUM_MFST).setText(mfstFile.collectPercent + "%");
                getWriteSizeTextView(Ota.FILE_NUM_MFST).setText(mfstFile.writeSize + "");
                getWritePercentTextView(Ota.FILE_NUM_MFST).setText(mfstFile.writePercent + "%");

                getCollectSizeTextView(Ota.FILE_NUM_APP0).setText(app0File.collectSize + "");
                getCollectPercentTextView(Ota.FILE_NUM_APP0).setText(app0File.collectPercent + "%");
                getWriteSizeTextView(Ota.FILE_NUM_APP0).setText(app0File.writeSize + "");
                getWritePercentTextView(Ota.FILE_NUM_APP0).setText(app0File.writePercent + "%");

                getCollectSizeTextView(Ota.FILE_NUM_APP1).setText(app1File.collectSize + "");
                getCollectPercentTextView(Ota.FILE_NUM_APP1).setText(app1File.collectPercent + "%");
                getWriteSizeTextView(Ota.FILE_NUM_APP1).setText(app1File.writeSize + "");
                getWritePercentTextView(Ota.FILE_NUM_APP1).setText(app1File.writePercent + "%");

                getCollectSizeTextView(Ota.FILE_NUM_APP2).setText(app2File.collectSize + "");
                getCollectPercentTextView(Ota.FILE_NUM_APP2).setText(app2File.collectPercent + "%");
                getWriteSizeTextView(Ota.FILE_NUM_APP2).setText(app2File.writeSize + "");
                getWritePercentTextView(Ota.FILE_NUM_APP2).setText(app2File.writePercent + "%");
            }
            else if (checkedId == mRemoteControlBinding.slot2RadioButton.getId())
            {
                mOta.currentSlotNum = Ota.SLOT_NUM_2;
                Log.d(TAG, "slotNum = " + mOta.currentSlotNum);

                Ota.OtaFile mfstFile = Ota.getFile(Ota.SLOT_NUM_2, Ota.FILE_NUM_MFST);
                Ota.OtaFile app0File = Ota.getFile(Ota.SLOT_NUM_2, Ota.FILE_NUM_APP0);
                Ota.OtaFile app1File = Ota.getFile(Ota.SLOT_NUM_2, Ota.FILE_NUM_APP1);
                Ota.OtaFile app2File = Ota.getFile(Ota.SLOT_NUM_2, Ota.FILE_NUM_APP2);


                getCollectSizeTextView(Ota.FILE_NUM_MFST).setText(mfstFile.collectSize + "");
                getCollectPercentTextView(Ota.FILE_NUM_MFST).setText(mfstFile.collectPercent + "%");
                getWriteSizeTextView(Ota.FILE_NUM_MFST).setText(mfstFile.writeSize + "");
                getWritePercentTextView(Ota.FILE_NUM_MFST).setText(mfstFile.writePercent + "%");

                getCollectSizeTextView(Ota.FILE_NUM_APP0).setText(app0File.collectSize + "");
                getCollectPercentTextView(Ota.FILE_NUM_APP0).setText(app0File.collectPercent + "%");
                getWriteSizeTextView(Ota.FILE_NUM_APP0).setText(app0File.writeSize + "");
                getWritePercentTextView(Ota.FILE_NUM_APP0).setText(app0File.writePercent + "%");

                getCollectSizeTextView(Ota.FILE_NUM_APP1).setText(app1File.collectSize + "");
                getCollectPercentTextView(Ota.FILE_NUM_APP1).setText(app1File.collectPercent + "%");
                getWriteSizeTextView(Ota.FILE_NUM_APP1).setText(app1File.writeSize + "");
                getWritePercentTextView(Ota.FILE_NUM_APP1).setText(app1File.writePercent + "%");

                getCollectSizeTextView(Ota.FILE_NUM_APP2).setText(app2File.collectSize + "");
                getCollectPercentTextView(Ota.FILE_NUM_APP2).setText(app2File.collectPercent + "%");
                getWriteSizeTextView(Ota.FILE_NUM_APP2).setText(app2File.writeSize + "");
                getWritePercentTextView(Ota.FILE_NUM_APP2).setText(app2File.writePercent + "%");
            }
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
            mMainActivity.longTimeIdleHandlerUpdate(true);

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
            mMainActivity.longTimeIdleHandlerUpdate(true);

            Thread thread = new Thread(collectRunnable);
            thread.start();
        } // onClick
    };// OnClickListener

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - write button
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_writeButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mMainActivity.longTimeIdleHandlerUpdate(true);

            Thread thread = new Thread(writeRunnable);
            thread.start();
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // OnClickListener - select button
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    View.OnClickListener onClick_selectButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mMainActivity.longTimeIdleHandlerUpdate(true);

            byte[] packet = new byte[PacketInfo.PACKET_SIZE_BOOT_SEND];

            packet[0] = PacketInfo.HEADER_BOOT_STATUS;
            packet[1] = (byte) PacketInfo.BOOT_OPTION_SELECT;
            packet[2] = (byte) (mOta.currentSlotNum & 0xFF);

            mMainActivity.sendPacket(packet);
        }
    };

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

            mMainActivity.sendPacket(packet);
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

        final int[]    types = {PacketInfo.MAP_INIT_TYPE_MRI_32CH, PacketInfo.MAP_INIT_TYPE_MRI_16CH};
        final String[] names = {PacketInfo.MAP_INIT_NAME_MRI_32CH, PacketInfo.MAP_INIT_NAME_MRI_16CH};
        final String[] items = {"32Ch (MRI 32채널 맵)", "16Ch (MRI 16채널 맵)"};

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("맵 초기화 종류 선택") //
                .setSingleChoiceItems(items, -1, (dialogInterface, which) ->
                {
                    final int    type = types[which];
                    final String name = names[which];

                    dialogInterface.dismiss();

                    /* 위 다이얼로그가 닫히면서 mDialog 를 비운 뒤에 확인 다이얼로그를 띄워야 한다.
                     * 같은 자리에서 바로 호출하면 mDialog 가 아직 남아 있어 무시된다. */
                    new Handler(Looper.getMainLooper()).post(() -> makeDialog_mapInitConfirm(type, name));
                }) //
                .setNegativeButton("취소", null) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
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

            mMainActivity.longTimeIdleHandlerUpdate(true);
            mMainActivity.sendPacket(mMainActivity.packetMaker(PacketInfo.HEADER_VALUE_PROMGRAM, new byte[]{PacketInfo.PROGRAM_DOWN}, PacketInfo.PACKET_SIZE_PROGRAM));
        }
    };

    View.OnClickListener onClick_programUpButton = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            Log.d(TAG, "[PROGRAM] 다음 프로그램 요청");

            mMainActivity.longTimeIdleHandlerUpdate(true);
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
    /// func - 값 조정 패킷 전송 ([헤더, 값] 2바이트 공통 형식)
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendValuePacket(byte header, byte value, int packetSize, String logMessage)
    {
        Log.d(TAG, logMessage);

        mMainActivity.longTimeIdleHandlerUpdate(true);
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
        final int maxLevel = PacketInfo.MIN_TX_PWR_SELECT_MAX;
        final int count    = (maxLevel - minLevel) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int level = minLevel + i;
            items[i] = String.format("%.3f V  (레벨 %d)", (level * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f, level);
        }

        // 현재 값을 미리 선택해 둔다. 아직 못 읽었으면 기본값 위치를 가리킨다.
        int currentLevel = mStatusViewModel.getValueMinTxPowerLevel();

        if (currentLevel < minLevel || maxLevel < currentLevel)
        {
            currentLevel = PacketInfo.MIN_TX_PWR_DEFAULT;
        }

        final int checkedIndex = currentLevel - minLevel;

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("PMIC 하한 선택") //
                .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
                {
                    sendMinTxPowerPacket(minLevel + which);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("Default", (dialogInterface, i) -> sendMinTxPowerPacket(PacketInfo.MIN_TX_PWR_DEFAULT)) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
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
        final int maxLevel = PacketInfo.MIN_TX_PWR_SELECT_MAX;
        final int count    = (maxLevel - minLevel) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int level = minLevel + i;
            items[i] = String.format("%.3f V  (레벨 %d)", (level * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f, level);
        }

        int currentLevel = mStatusViewModel.getValueMappingTxPowerLevel();

        if (currentLevel < minLevel || maxLevel < currentLevel)
        {
            currentLevel = PacketInfo.MAPPING_TX_PWR_DEFAULT;
        }

        final int checkedIndex = currentLevel - minLevel;

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("매핑 PMIC 하한 선택") //
                .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
                {
                    sendMappingTxPowerPacket(minLevel + which);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("Default", (dialogInterface, i) -> sendMappingTxPowerPacket(PacketInfo.MAPPING_TX_PWR_DEFAULT)) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
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
            makeDialog_coinModeSelect();
        }
    };

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

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("백텔 재시도 모드") //
                .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
                {
                    mMainActivity.sendCoinMode(modes[which]);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("취소", null) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
    }

    // 현재 코인 모드를 목록 위치로 바꾼다. 아직 못 읽었으면 아무것도 선택하지 않는다.
    private int getCurrentCoinModeIndex()
    {
        int infinite = mStatusViewModel.getValueInfiniteCoin();
        int oneCoin  = mStatusViewModel.getValueOneCoin();

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
        final int count    = (maxLevel - minLevel) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int level = minLevel + i;
            items[i] = String.format("%.3f V  (레벨 %d)", (level * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f, level);
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

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("고정 PMIC 선택") //
                .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
                {
                    sendForceTxPowerPacket(minLevel + which);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("해제", (dialogInterface, i) -> sendForceTxPowerPacket(PacketInfo.FORCE_TX_PWR_RELEASE)) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
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
        final int count   = (maxStep - minStep) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int step = minStep + i;
            items[i] = String.format("%d 스텝  (%d mV)", step, (step * PacketInfo.TX_PWR_LEVEL_STEP_MV));
        }

        int currentStep = mStatusViewModel.getValueTxStepUp();

        if (currentStep < minStep || maxStep < currentStep)
        {
            currentStep = PacketInfo.TX_STEP_UP_DEFAULT;
        }

        final int checkedIndex = currentStep - minStep;

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("링크 상승 스텝 선택") //
                .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
                {
                    sendTxStepUpPacket(minStep + which);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("Default", (dialogInterface, i) -> sendTxStepUpPacket(PacketInfo.TX_STEP_UP_DEFAULT)) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
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

        final int[] values     = PacketInfo.getNopUsableValues(frameNum);
        final int   defaultNop = PacketInfo.getNopDefaultByFrame(frameNum);

        String[] items = new String[values.length];

        for (int i = 0; i < values.length; i++)
        {
            int gapUs = PacketInfo.getNopGapUs(frameNum, i);
            int nop   = values[i];

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

            // 자극이 1msec 나 비는 지점은 눈에 띄게 표시한다.
            if (PacketInfo.NOP_GAP_WARN_US <= gapUs)
            {
                sb.append("  [주의]");
            }

            items[i] = sb.toString();
        }

        int currentNop   = mStatusViewModel.getValueNopStandbyCount();
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
        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setCustomTitle(makeDialogTitleView("전원 안정 Nop 선택", makeNopDialogMessage(frameNum))) //
                .setSingleChoiceItems(items, selected, (dialogInterface, which) ->
                {
                    sendNopStandbyPacket(values[which]);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("기본값", (dialogInterface, i) -> mMainActivity.sendNopStandbyDefault()) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
    }

    /* 패킷 수를 모를 때 쓰는 예전 방식 목록. 어느 값이 자리 맞춤에 맞는지 알 수 없으므로
     * 0 ~ 19 를 그대로 올린다. */
    private void makeDialog_nopStandbyPlain(int frameNum)
    {
        final int minCount = PacketInfo.NOP_STANDBY_SELECT_MIN;
        final int maxCount = PacketInfo.NOP_STANDBY_SELECT_MAX;
        final int count    = (maxCount - minCount) + 1;

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

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setCustomTitle(makeDialogTitleView("전원 안정 Nop 선택", message)) //
                .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
                {
                    sendNopStandbyPacket(minCount + which);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("기본값", (dialogInterface, i) -> mMainActivity.sendNopStandbyDefault()) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
    }

    /* 다이얼로그 머리말. 지금 맵이 어떤 상태인지와, 펄스폭과 패킷 수가 어긋난다면 그 이유를 적는다. */
    private String makeNopDialogMessage(int frameNum)
    {
        int pulseWidth = mStatusViewModel.getValuePulseWidth();
        int strategy   = mStatusViewModel.getValueStimStrategy();
        int nopEnable  = mStatusViewModel.getValueNopEnable();

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
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleView.setTypeface(null, Typeface.BOLD);

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
        final int count     = (maxPeriod - minPeriod) + 1;

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

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("백텔 주기 선택") //
                .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
                {
                    sendBacktelPeriodPacket(minPeriod + which);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("Default", (dialogInterface, i) -> sendBacktelPeriodPacket(PacketInfo.BACKTEL_PERIOD_DEFAULT_100MS)) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
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
    private void sendGatingPacket(int enableState)
    {
        /* 묵음은 릴리즈 2~3 유산이라 이 명령만 구형 포맷을 쓴다.
         * [커맨드, 옵션 2, 세부1, 세부2, T레벨 오프셋]
         *
         * 비활성화일 때 펌웨어는 오프셋을 N/A 로 무시하고 기존 값을 유지하므로,
         * 여기서 실어 보내는 오프셋은 활성화일 때만 실제로 반영된다. */
        int offset = PacketInfo.MUTE_T_LEVEL_OFFSET_DEFAULT;

        byte[] packet = new byte[PacketInfo.RC_REQ_LEN_MUTE_WRITE];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[1] = (byte) PacketInfo.RC_OPT_MUTE_WRITE;
        packet[2] = (byte) PacketInfo.MUTE_SUB_OPT_NORMAL_MODE;
        packet[3] = (byte) (enableState & 0xFF);
        packet[4] = (byte) (offset & 0xFF);

        Log.d(TAG, "[GATING] 게이팅 설정 요청 : " + ((enableState == PacketInfo.MUTE_ENABLE) ? "활성화" : "비활성화") + ", 오프셋 " + offset);

        mMainActivity.longTimeIdleHandlerUpdate(true);
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

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("맵 초기화") //
                .setMessage("사운드처리기에 등록된 모든 내부기의 맵 데이터를\n" + mapInitName + " 으로 덮어씁니다.\n\n되돌릴 수 없습니다. 진행하시겠습니까?") //
                .setPositiveButton("초기화", (dialogInterface, i) -> sendMapInitPacket(mapInitType, mapInitName)) //
                .setNegativeButton("취소", null) //
                .setCancelable(false) //
                .create();

        // 이 화면의 다른 다이얼로그와 달리 반복 사용해야 하므로, 닫힐 때 참조를 비워 다시 띄울 수 있게 한다.
        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 맵 초기화 패킷 전송
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendMapInitPacket(int mapInitType, String mapInitName)
    {
        /* 맵 초기화는 자극 도메인(0x21) 인덱스 1 의 쓰기다. 돌려받을 값이 없는 쓰기 전용이다.
         * 사운드처리기는 응답을 먼저 보내고 초기화를 시작하므로 응답 = 완료가 아니다. */
        Log.d(TAG, "[MAP INIT] 맵 초기화 요청 : " + mapInitName + " (type=" + String.format("%02X", mapInitType) + ")");

        mMainActivity.longTimeIdleHandlerUpdate(true);
        mMainActivity.sendMapInit(mapInitType);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - print buffer
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void printBuffer(int otaParam, MainActivity mainActivity)
    {
        ArrayList<String> stringList = new ArrayList<>();
        byte[]            buffer     = new byte[Ota.PRINT_LOG_HEX_LENGTH];
        int               cnt        = 0;

        Ota.OtaFile otaFile = Ota.getFile(mOta.currentSlotNum, mOta.currentFileNum);

        if (otaFile.collectPercent != 100)
        {
            return;
        }

        for (int i = 0; i < otaFile.totalBytes; i++)
        {
            buffer[cnt++] = otaFile.buffer[i];

            if (cnt == Ota.PRINT_LOG_HEX_LENGTH || i == (otaFile.totalBytes - 1))
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

        Log.d(TAG, "[OTA] 파일 " + Ota.getFileName(mOta.currentFileNum));

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

    // LiveData - 프로그램 번호 / 볼륨 / 최대출력
    //
    // 세 값 모두 연결 직후의 상태 패킷(0x43)으로 채워지고, 이후에는 해당 항목의
    // 응답 패킷을 받을 때마다 갱신된다. 볼륨과 최대출력은 표시 전용이다.
    private void liveDataProgramAndLevels()
    {
        mStatusViewModel.getLiveDataProgram().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueProgram();
            Log.v(TAG, "옵저버 : 프로그램 -> " + value);
            mRemoteControlBinding.otaProgramTextview.setText("Prog " + value);
        });

        mStatusViewModel.getLiveDataVolume().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueVolume();
            Log.v(TAG, "옵저버 : 볼륨 -> " + value);
            mRemoteControlBinding.otaVolumeTextview.setText("Vol " + value);
        });

        mStatusViewModel.getLiveDataMaxOutput().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueMaxOutput();
            Log.v(TAG, "옵저버 : 최대출력 -> " + value);
            mRemoteControlBinding.otaMaxOutputTextview.setText("Out " + value);
        });

        // LED 상태. 연결 직후 상태 패킷(0x43)으로 채워지고, LED 버튼 응답마다 갱신된다.
        mStatusViewModel.getLiveDataLed().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueLed();
            Log.v(TAG, "옵저버 : LED -> " + value);
            mRemoteControlBinding.otaLedTextview.setText("LED " + ((value == PacketInfo.LED_ON) ? "On" : "Off"));
        });

        // 자극알림 상태.
        mStatusViewModel.getLiveDataNotification().observe(getViewLifecycleOwner(), o ->
        {
            int value = mStatusViewModel.getValueNotification();
            Log.v(TAG, "옵저버 : 자극알림 -> " + value);
            mRemoteControlBinding.otaAlarmTextview.setText("Alarm " + ((value == PacketInfo.NOTIFICATION_ON) ? "On" : "Off"));
        });

        // 상시 동작 Tx 파워 하한(PMIC).
        mStatusViewModel.getLiveDataMinTxPowerLevel().observe(getViewLifecycleOwner(), o ->
        {
            int level = mStatusViewModel.getValueMinTxPowerLevel();
            mRemoteControlBinding.otaPmicTextview.setText(makeVoltageText("PMIC", level));
        });

        // 현재 Tx 파워(관찰값). 설정값이 아니라 지금 PMIC 에 실제로 쓰인 값이다.
        mStatusViewModel.getLiveDataCurTxPowerLevel().observe(getViewLifecycleOwner(), o ->
        {
            int level = mStatusViewModel.getValueCurTxPowerLevel();
            mRemoteControlBinding.otaCurPmicTextview.setText(makeVoltageText("Cur", level));
        });

        // 매핑(피팅) 전용 Tx 파워 하한.
        mStatusViewModel.getLiveDataMappingTxPowerLevel().observe(getViewLifecycleOwner(), o ->
        {
            int level = mStatusViewModel.getValueMappingTxPowerLevel();
            mRemoteControlBinding.otaMappingPmicTextview.setText(makeVoltageText("Map", level));
        });

        // Tx 파워 강제 고정. 0 이면 해제 상태다.
        mStatusViewModel.getLiveDataForceTxPowerLevel().observe(getViewLifecycleOwner(), o ->
        {
            int    level = mStatusViewModel.getValueForceTxPowerLevel();
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
        mStatusViewModel.getLiveDataTxStepUp().observe(getViewLifecycleOwner(), o ->
        {
            int step = mStatusViewModel.getValueTxStepUp();
            mRemoteControlBinding.otaStepUpTextview.setText((step == PacketInfo.LINK_VALUE_UNKNOWN) ? "Step -" : ("Step " + step));
        });

        /* 백텔 재시도 상태. one coin 과 infinite coin 두 값을 합쳐 한 칸에 보여준다.
         * infinite 가 우선하므로 켜져 있으면 one coin 값과 무관하게 무한대로 표시한다.
         * 두 값이 따로 도착하므로 옵저버도 둘 다 걸어 어느 쪽이 바뀌어도 갱신되게 한다. */
        mStatusViewModel.getLiveDataOneCoin().observe(getViewLifecycleOwner(), o -> updateCoinText());
        mStatusViewModel.getLiveDataInfiniteCoin().observe(getViewLifecycleOwner(), o -> updateCoinText());

        // 링크 제어 모드. 0 = 전원 상태 기준, 1 = 백텔 수신 기준.
        mStatusViewModel.getLiveDataLinkCtrlMode().observe(getViewLifecycleOwner(), o ->
        {
            int    value = mStatusViewModel.getValueLinkCtrlMode();
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

            mRemoteControlBinding.otaCtrlModeTextview.setText(text);
        });

        // Tx 파워 상승 가속.
        mStatusViewModel.getLiveDataTxPowerAccel().observe(getViewLifecycleOwner(), o ->
        {
            int    value = mStatusViewModel.getValueTxPowerAccel();
            String text;

            if (value == PacketInfo.LINK_VALUE_UNKNOWN)
            {
                text = "Accel -";
            }
            else
            {
                text = "Accel " + ((value == PacketInfo.LINK_FLAG_ENABLE) ? "On" : "Off");
            }

            mRemoteControlBinding.otaAccelTextview.setText(text);
        });

        /* 전원 안정용 NopStandby 개수.
         * 되읽은 값과 실제 동작 개수가 다를 수 있다. 펌웨어가 채널당 프레임 수에 맞춰
         * 매 백텔 사이클마다 다시 잘라내기 때문이다. 여기 보이는 값은 «설정값» 이다. */
        mStatusViewModel.getLiveDataNopStandbyCount().observe(getViewLifecycleOwner(), o -> updateNopText());

        /* 개수와 «어디서 온 값인지» 가 따로 도착하므로 둘 다 걸어 어느 쪽이 바뀌어도 갱신되게 한다. */
        mStatusViewModel.getLiveDataNopEnable().observe(getViewLifecycleOwner(), o -> updateNopText());

        // 자극 전략. 펄스폭과 패킷 수가 어긋나는 이유가 이 값에 있다.
        mStatusViewModel.getLiveDataStimStrategy().observe(getViewLifecycleOwner(), o ->
        {
            int    value = mStatusViewModel.getValueStimStrategy();
            String text  = (value == PacketInfo.LINK_VALUE_UNKNOWN) ? "Strat -" : makeStimStrategyName(value);

            mRemoteControlBinding.otaStrategyTextview.setText(text);
        });

        // 현재 맵의 자극 펄스폭.
        mStatusViewModel.getLiveDataPulseWidth().observe(getViewLifecycleOwner(), o ->
        {
            int    value = mStatusViewModel.getValuePulseWidth();
            String text  = (value == PacketInfo.LINK_VALUE_UNKNOWN) ? "PW -" : ("PW " + value + "us");

            mRemoteControlBinding.otaPulseWidthTextview.setText(text);
        });

        /* 패킷 수. 쓸 수 있는 Nop 표를 찾는 근거이자, 10 이상이면 백텔이 아예 나가지 않는다.
         * 그 구간은 눈에 띄게 표시한다. */
        mStatusViewModel.getLiveDataFrameNum().observe(getViewLifecycleOwner(), o ->
        {
            int    value = mStatusViewModel.getValueFrameNum();
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
        mStatusViewModel.getLiveDataFwRelease().observe(getViewLifecycleOwner(), o -> applyFwRelease());

        // 링크 백텔 주기. 100msec 단위로 받아 밀리초로 환산해 표시한다.
        mStatusViewModel.getLiveDataBacktelPeriod().observe(getViewLifecycleOwner(), o ->
        {
            int    period100ms = mStatusViewModel.getValueBacktelPeriod();
            String text;

            if (period100ms == PacketInfo.LINK_VALUE_UNKNOWN)
            {
                text = "BT -";
            }
            else
            {
                text = "BT " + (period100ms * PacketInfo.BACKTEL_PERIOD_UNIT_MS) + "ms";
            }

            Log.v(TAG, "옵저버 : 백텔 주기 -> " + period100ms);
            mRemoteControlBinding.otaBacktelTextview.setText(text);
        });

        // 게이팅(묵음) 활성화 상태.
        mStatusViewModel.getLiveDataGatingState().observe(getViewLifecycleOwner(), o ->
        {
            int    state = mStatusViewModel.getValueGatingState();
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

            Log.v(TAG, "옵저버 : 게이팅 상태 -> " + state);
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
        int value  = mStatusViewModel.getValueNopStandbyCount();
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

        mRemoteControlBinding.otaNopStandbyTextview.setText(text);
    }

    private void updateCoinText()
    {
        int    infinite = mStatusViewModel.getValueInfiniteCoin();
        int    oneCoin  = mStatusViewModel.getValueOneCoin();
        String text;

        /* infinite coin 이 없는 세대에서는 그 값을 «못 읽은 것» 으로 보면 안 된다.
         * 그러면 one coin 을 멀쩡히 읽고도 화면이 «Coin -» 로 남는다.
         * 표시의 근거는 one coin 이고, infinite 는 그것을 덮어쓰는 값일 뿐이다. */
        boolean hasInfinite = (mMainActivity != null) //
                              && mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_INFINITE_COIN);

        if (hasInfinite && infinite == PacketInfo.LINK_FLAG_ENABLE)
        {
            text = "Coin " + COIN_INFINITE_MARK;
        }
        else if (oneCoin == PacketInfo.LINK_VALUE_UNKNOWN //
                 || (hasInfinite && infinite == PacketInfo.LINK_VALUE_UNKNOWN))
        {
            text = "Coin -";
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
        String label;
        int    badgeColor;

        switch (fwRelease)
        {
            case Status.FW_RELEASE_3:
                label = "REL3";
                badgeColor = 0xFFFF7043; // 주황 - 되는 기능이 가장 적다
                break;

            case Status.FW_RELEASE_4:
                label = "REL4";
                badgeColor = 0xFFFFD54F; // 노랑
                break;

            case Status.FW_RELEASE_4_PLUS:
                label = "REL4+";
                badgeColor = 0xFF00E676; // 초록 - 전부 된다
                break;

            default:
                label = "";
                badgeColor = 0;
                break;
        }

        TextView badge = mRemoteControlBinding.otaFwReleaseTextview;

        badge.setText(label);
        badge.setVisibility(label.isEmpty() ? View.INVISIBLE : View.VISIBLE);

        if (badgeColor != 0)
        {
            /* 배지 바탕은 모든 세대가 같은 drawable 을 공유한다.
             * mutate() 없이 색을 바꾸면 같은 drawable 을 쓰는 다른 뷰까지 물든다. */
            Drawable background = badge.getBackground();

            if (background != null)
            {
                background = background.mutate();
                background.setColorFilter(new PorterDuffColorFilter(badgeColor, PorterDuff.Mode.SRC_IN));
                badge.setBackground(background);
            }
        }

        // 링크 파라미터 버튼. 세대가 그 인덱스를 아는지 액티비티에 물어본다.
        setButtonEnabled(mRemoteControlBinding.otaPmicSelectButton, mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_MIN_TX_POWER));
        setButtonEnabled(mRemoteControlBinding.otaMappingPmicButton, mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_MAPPING_MIN_POWER));
        setButtonEnabled(mRemoteControlBinding.otaBtSelectButton, mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_BACKTEL_PERIOD));
        setButtonEnabled(mRemoteControlBinding.otaLinkStepButton, mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_TX_POWER_STEP_UP));
        setButtonEnabled(mRemoteControlBinding.otaFixedPmicButton, mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_FORCE_TX_POWER));
        setButtonEnabled(mRemoteControlBinding.otaOneCoinButton, mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_ONE_COIN));
        setButtonEnabled(mRemoteControlBinding.otaCtrlModeButton, mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_CTRL_MODE));
        setButtonEnabled(mRemoteControlBinding.otaAccelButton, mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_TX_POWER_ACCEL));
        setButtonEnabled(mRemoteControlBinding.otaNopStandbyButton, mMainActivity.isLinkParamSupported(PacketInfo.RC_LINK_IDX_POWER_STABLE_NOP));

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

        Log.d(TAG, "[LINK] 화면에 펌웨어 세대를 반영했습니다 -> " + (label.isEmpty() ? "판별 전" : label));
    }

    /* 상태 칸 하나에 «이 세대엔 없음» 표시를 켜거나 끈다.
     *
     * 판별 전(세대 미상)에는 긋지 않는다. 아직 모르는 것이지 없는 것이 아니다. */
    private void markUnsupported(TextView view, int linkIndex)
    {
        boolean unknownYet  = (mStatusViewModel.getValueFwRelease() == Status.FW_RELEASE_UNKNOWN);
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
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.35f);
    }

    private String makeVoltageText(String label, int level)
    {
        if (level == PacketInfo.LINK_VALUE_UNKNOWN)
        {
            return label + " -";
        }

        return String.format("%s %.1fV", label, (level * PacketInfo.TX_PWR_LEVEL_STEP_MV) / 1000.0f);
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
                            String name = UtilUser.getNameOnly(defaultUser.name) + " (" + UtilUser.getEarKorean(defaultUser.ear) + ")";

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

    /* 아래 안내 다이얼로그들은 모두 «닫히면 mDialog 를 비운다».
     *
     * 예전에는 그 처리가 없어서 한 번 뜨고 나면 mDialog 가 계속 남았다. 그러면 다른
     * 버튼(Coin · PMIC · Nop 등)이 «이미 다이얼로그가 표시 중» 으로 막혀 아무것도
     * 뜨지 않는다. 화면을 나갔다 오면 프래그먼트가 다시 만들어져 풀리던 것이 그 증상이다. */
    public void makeDialog_finishOTA(int fileCount)
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                    .setTitle("안내") //
                    .setMessage("OTA 이미지 전송이 완료되었습니다.\n전송한 파일 " + fileCount + "개") //
                    .setPositiveButton("확인", null) //
                    .setCancelable(false) //
                    .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            mDialog.show();
        }
    }

    public void makeDialog_withMessage(String message)
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                    .setTitle("안내") //
                    .setMessage(message) //
                    .setPositiveButton("확인", null) //
                    .setCancelable(false) //
                    .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

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

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            mDialog.show();
        }
    }

    // OTA 패킷 사이즈 에러 다이얼로그
    private void makeDialog_invalidPacketSize()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                    .setTitle("안내") //
                    .setMessage("패킷 사이즈 에러 발생") //
                    .setPositiveButton("확인", null) //
                    .setCancelable(false) //
                    .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            mDialog.show();
        }
    }

    // OTA 결과 에러
    private void makeDialog_invalidResult()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                    .setTitle("안내") //
                    .setMessage("OTA 결과가 성공이 아님") //
                    .setPositiveButton("확인", null) //
                    .setCancelable(false) //
                    .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            mDialog.show();
        }
    }

    // OTA 데이터 인덱스 에러 다이얼로그
    private void makeDialog_invalidDataIndex()
    {
        if (mDialog == null)
        {
            mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                    .setTitle("안내") //
                    .setMessage("OTA 데이터 인덱스 에러 발생") //
                    .setPositiveButton("확인", null) //
                    .setCancelable(false) //
                    .create();

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

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

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

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

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

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

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

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

            mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

            mDialog.show();
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