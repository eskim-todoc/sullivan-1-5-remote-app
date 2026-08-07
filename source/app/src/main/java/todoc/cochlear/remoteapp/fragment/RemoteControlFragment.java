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
        mRemoteControlBinding.fileRadioButtonGroup.setOnCheckedChangeListener(onChecked_radioGroup);

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
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Runnable - collect
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    Runnable collectRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            int         slotNum;
            int         fileNum;
            Ota.OtaFile otaFile;
            String      filePath;
            File        objFile;
            TextView    collectSizeTv;
            TextView    collectPercentTv;

            if (mOta.threadState == Ota.THREAD_STATE_BUSY)
            {
                Log.d(TAG, "[OTA][THREAD] BUSY 상태라서 collectThread 동작 실패");
                return;
            }

            mOta.threadState = Ota.THREAD_STATE_BUSY;

            slotNum = mOta.currentSlotNum;
            fileNum = mOta.currentFileNum;
            otaFile = Ota.getFile(slotNum, fileNum);
            filePath = makePath(slotNum, fileNum);
            objFile = new File(filePath);

            Log.d(TAG, "[OTA][THREAD] 파일 " + filePath + "의 상태 초기화");

            otaFile.collectSize = 0;
            otaFile.collectPercent = 0;
            otaFile.writeSize = 0;
            otaFile.writePercent = 0;
            otaFile.totalBytes = 0;

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
                    mOta.threadState = Ota.THREAD_STATE_IDLE;
                    return;
            }

            if (!objFile.exists())
            {
                new Handler(Looper.getMainLooper()).post(() ->
                {
                    collectSizeTv.setText(otaFile.collectSize + "");
                    collectPercentTv.setText(otaFile.collectPercent + "%");
                });

                mOta.threadState = Ota.THREAD_STATE_IDLE;

                Log.d(TAG, "파일 없음 : " + filePath);
            }
            else
            {
                otaFile.totalBytes = (int) objFile.length();

                Log.d(TAG, "[OTA][THREAD] 파일 찾음 : " + filePath + ", " + otaFile.totalBytes + " bytes");

                if (otaFile.buffer == null)
                {
                    otaFile.buffer = new byte[otaFile.totalBytes];
                }
                else if (otaFile.buffer.length < otaFile.totalBytes)
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

                    collectSizeTv.setText(otaFile.collectSize + "");
                    collectPercentTv.setText(otaFile.collectPercent + "%");
                    e.printStackTrace();
                }

                printBuffer(0, null);

                mOta.threadState = Ota.THREAD_STATE_IDLE;
            }
        }
    };

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Runnable - write
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    Runnable writeRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            Ota.OtaFile otaFile = Ota.getFile(mOta.currentSlotNum, mOta.currentFileNum);

            if (otaFile.collectPercent != 100)
            {
                Log.d(TAG, "[OTA] 아직 " + Ota.getFileName(mOta.currentFileNum) + " 파일 읽기가 완료 되지 않았음");
                return;
            }

            if (mOta.commState != Ota.COMM_STATE_IDLE)
            {
                Log.d(TAG, "[OTA] 무선 전송 상태가 IDLE이 아님");
                return;
            }

            mOta.commState = Ota.COMM_STATE_PREPARE_COMMAND;

            mOta.commDataIndex = 0;
            mOta.commSlotNum = mOta.currentSlotNum;
            mOta.commFileNum = mOta.currentFileNum;
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

            mMainActivity.mCheckBatteryHandler.removeCallbacks(mMainActivity.mCheckBatteryRunner);
            mMainActivity.sendPacket(packet);

            mOta.commState = Ota.COMM_STATE_WAIT_RESP_COMMAND;
        }
    };

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
            makeDialog_invalidDataIndex();
            return;
        }

        if (result != 1)
        {
            Log.d(TAG, "[BLE] OTA 패킷 결과가 성공이 아님, 결과 = " + result);
            mOta.commState = Ota.COMM_STATE_IDLE;
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
                Log.d(TAG, "[BLE] OTA 패킷 전송 완료");
                mOta.commState = Ota.COMM_STATE_IDLE;
                makeDialog_finishOTA();
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
            makeDialog_invalidDataIndex();
            return;
        }

        if (result != 1)
        {
            Log.d(TAG, "[BLE] OTA 패킷 결과가 성공이 아님, 결과 = " + result);
            mOta.commState = Ota.COMM_STATE_IDLE;
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
            // File radio group
            else if (checkedId == mRemoteControlBinding.fileMfstRadioButton.getId())
            {
                mOta.currentFileNum = Ota.FILE_NUM_MFST;
                Log.d(TAG, "fileNum = " + mOta.currentFileNum);
            }
            else if (checkedId == mRemoteControlBinding.fileApp0RadioButton.getId())
            {
                mOta.currentFileNum = Ota.FILE_NUM_APP0;
                Log.d(TAG, "fileNum = " + mOta.currentFileNum);
            }
            else if (checkedId == mRemoteControlBinding.fileApp1RadioButton.getId())
            {
                mOta.currentFileNum = Ota.FILE_NUM_APP1;
                Log.d(TAG, "fileNum = " + mOta.currentFileNum);
            }
            else if (checkedId == mRemoteControlBinding.fileApp2RadioButton.getId())
            {
                mOta.currentFileNum = Ota.FILE_NUM_APP2;
                Log.d(TAG, "fileNum = " + mOta.currentFileNum);
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
            int state = (mStatusViewModel.getValueGatingState() == PacketInfo.GATING_ENABLE) ? PacketInfo.GATING_DISABLE : PacketInfo.GATING_ENABLE;

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

        final int minLevel = PacketInfo.MIN_TX_PWR_LEVEL_SELECT_MIN;
        final int maxLevel = PacketInfo.MIN_TX_PWR_LEVEL_SELECT_MAX;
        final int count    = (maxLevel - minLevel) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int level = minLevel + i;
            items[i] = String.format("%.3f V  (레벨 %d)", (level * PacketInfo.MIN_TX_PWR_LEVEL_STEP_MV) / 1000.0f, level);
        }

        // 현재 값을 미리 선택해 둔다. 아직 못 읽었으면 기본값 위치를 가리킨다.
        int currentLevel = mStatusViewModel.getValueMinTxPowerLevel();

        if (currentLevel < minLevel || maxLevel < currentLevel)
        {
            currentLevel = PacketInfo.MIN_TX_PWR_LEVEL_DEFAULT;
        }

        final int checkedIndex = currentLevel - minLevel;

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("PMIC 하한 선택") //
                .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
                {
                    sendMinTxPowerPacket(minLevel + which);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("Default", (dialogInterface, i) -> sendMinTxPowerPacket(PacketInfo.MIN_TX_PWR_LEVEL_DEFAULT)) //
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

        final int minLevel = PacketInfo.MIN_TX_PWR_LEVEL_SELECT_MIN;
        final int maxLevel = PacketInfo.MIN_TX_PWR_LEVEL_SELECT_MAX;
        final int count    = (maxLevel - minLevel) + 1;

        String[] items = new String[count];

        for (int i = 0; i < count; i++)
        {
            int level = minLevel + i;
            items[i] = String.format("%.3f V  (레벨 %d)", (level * PacketInfo.MIN_TX_PWR_LEVEL_STEP_MV) / 1000.0f, level);
        }

        int currentLevel = mStatusViewModel.getValueMappingTxPowerLevel();

        if (currentLevel < minLevel || maxLevel < currentLevel)
        {
            currentLevel = PacketInfo.MAPPING_TX_PWR_LEVEL_DEFAULT;
        }

        final int checkedIndex = currentLevel - minLevel;

        mDialog = new MaterialAlertDialogBuilder(requireContext()) //
                .setTitle("매핑 PMIC 하한 선택") //
                .setSingleChoiceItems(items, checkedIndex, (dialogInterface, which) ->
                {
                    sendMappingTxPowerPacket(minLevel + which);
                    dialogInterface.dismiss();
                }) //
                .setNegativeButton("Default", (dialogInterface, i) -> sendMappingTxPowerPacket(PacketInfo.MAPPING_TX_PWR_LEVEL_DEFAULT)) //
                .create();

        mDialog.setOnDismissListener(dialogInterface -> mDialog = null);

        mDialog.show();
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 매핑 전용 Tx 파워 하한 설정 패킷 전송
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendMappingTxPowerPacket(int level)
    {
        byte[] packet = new byte[PacketInfo.TX_PKT_LEN_SPECIFIC_CMD_MAPPING_TX_PWR_WRITE];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[1] = (byte) PacketInfo.TX_PKT_OPT_SPECIFIC_CMD_MAPPING_TX_PWR_WRITE;
        packet[2] = (byte) (level & 0xFF);

        Log.d(TAG, "[PMIC] 매핑 Tx 파워 하한 설정 요청 : " + level + " (" + (level * PacketInfo.MIN_TX_PWR_LEVEL_STEP_MV) + "mV)");

        mMainActivity.longTimeIdleHandlerUpdate(true);
        mMainActivity.sendPacket(packet);
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
            items[i] = String.format("%d 스텝  (%d mV)", step, (step * PacketInfo.MIN_TX_PWR_LEVEL_STEP_MV));
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
        byte[] packet = new byte[PacketInfo.TX_PKT_LEN_SPECIFIC_CMD_TX_STEP_UP_WRITE];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[1] = (byte) PacketInfo.TX_PKT_OPT_SPECIFIC_CMD_TX_STEP_UP_WRITE;
        packet[2] = (byte) (step & 0xFF);

        Log.d(TAG, "[PMIC] 링크 상승 스텝 설정 요청 : " + step + " (" + (step * PacketInfo.MIN_TX_PWR_LEVEL_STEP_MV) + "mV)");

        mMainActivity.longTimeIdleHandlerUpdate(true);
        mMainActivity.sendPacket(packet);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 링크 Tx 파워 하한 설정 패킷 전송
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendMinTxPowerPacket(int level)
    {
        byte[] packet = new byte[PacketInfo.TX_PKT_LEN_SPECIFIC_CMD_MIN_TX_PWR_WRITE];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[1] = (byte) PacketInfo.TX_PKT_OPT_SPECIFIC_CMD_MIN_TX_PWR_WRITE;
        packet[2] = (byte) (level & 0xFF);

        Log.d(TAG, "[PMIC] 링크 Tx 파워 하한 설정 요청 : " + level + " (" + (level * PacketInfo.MIN_TX_PWR_LEVEL_STEP_MV) + "mV)");

        mMainActivity.longTimeIdleHandlerUpdate(true);
        mMainActivity.sendPacket(packet);
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

        final int minPeriod = PacketInfo.BACKTEL_PERIOD_SELECT_MIN_100MS;
        final int maxPeriod = PacketInfo.BACKTEL_PERIOD_SELECT_MAX_100MS;
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
        byte[] packet = new byte[PacketInfo.TX_PKT_LEN_SPECIFIC_CMD_BACKTEL_PERIOD];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[1] = (byte) PacketInfo.TX_PKT_OPT_SPECIFIC_CMD_BACKTEL_PERIOD;
        packet[2] = (byte) (period100ms & 0xFF);

        Log.d(TAG, "[BACKTEL] 백텔 주기 설정 요청 : " + period100ms + " (" + (period100ms * PacketInfo.BACKTEL_PERIOD_UNIT_MS) + "msec)");

        mMainActivity.longTimeIdleHandlerUpdate(true);
        mMainActivity.sendPacket(packet);
    }

    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// func - 게이팅(묵음) 설정 패킷 전송
    ///
    /// 비활성화(GATING_DISABLE) 시 펌웨어는 오프셋 값을 N/A 로 무시하고 기존 값을 유지하므로,
    /// 여기서 실어 보내는 오프셋은 활성화일 때만 실제로 반영된다.
    /// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void sendGatingPacket(int enableState)
    {
        int offset = PacketInfo.GATING_T_LEVEL_OFFSET_DEFAULT;

        byte[] packet = new byte[PacketInfo.TX_PKT_LEN_SPECIFIC_CMD_GATING_WRITE];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[1] = (byte) PacketInfo.TX_PKT_OPT_SPECIFIC_CMD_GATING_WRITE;
        packet[2] = (byte) PacketInfo.GATING_SUB_OPT_NORMAL_MODE;
        packet[3] = (byte) (enableState & 0xFF);
        packet[4] = (byte) (offset & 0xFF);

        Log.d(TAG, "[GATING] 게이팅 설정 요청 : " + ((enableState == PacketInfo.GATING_ENABLE) ? "활성화" : "비활성화") + ", 오프셋 " + offset);

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
        byte[] packet = new byte[PacketInfo.TX_PKT_LEN_SPECIFIC_CMD_MAP_INIT];

        packet[0] = PacketInfo.HEADER_SPECIFIC_CMD;
        packet[1] = (byte) PacketInfo.TX_PKT_OPT_SPECIFIC_CMD_MAP_INIT;
        packet[2] = (byte) (mapInitType & 0xFF);

        Log.d(TAG, "[MAP INIT] 맵 초기화 패킷 전송 : " + mapInitName + " (type=" + String.format("%02X", mapInitType) + ")");

        mMainActivity.longTimeIdleHandlerUpdate(true);
        mMainActivity.sendPacket(packet);
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

        // 링크 Tx 파워 하한(PMIC). 레벨 1스텝 = 25mV 이므로 볼트로 환산해 표시한다.
        mStatusViewModel.getLiveDataMinTxPowerLevel().observe(getViewLifecycleOwner(), o ->
        {
            int    level = mStatusViewModel.getValueMinTxPowerLevel();
            String text;

            if (level == PacketInfo.LINK_VALUE_UNKNOWN)
            {
                text = "PMIC -";
            }
            else
            {
                text = String.format("PMIC %.1fV", (level * PacketInfo.MIN_TX_PWR_LEVEL_STEP_MV) / 1000.0f);
            }

            Log.v(TAG, "옵저버 : PMIC 하한 -> " + level);
            mRemoteControlBinding.otaPmicTextview.setText(text);
        });

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

            if (state == PacketInfo.GATING_ENABLE)
            {
                text = "Gating On";
            }
            else if (state == PacketInfo.GATING_DISABLE)
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