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

        checkRegisteredList_userAndDevice(); // User list

        mOta = Ota.getInstance();

        mRemoteControlBinding.slotRadioButtonGroup.setOnCheckedChangeListener(onChecked_radioGroup);
        mRemoteControlBinding.fileRadioButtonGroup.setOnCheckedChangeListener(onChecked_radioGroup);

        mRemoteControlBinding.otaInfoButton.setOnClickListener(onClick_infoButton);
        mRemoteControlBinding.otaCollectButton.setOnClickListener(onClick_collectButton);
        mRemoteControlBinding.otaWriteButton.setOnClickListener(onClick_writeButton);
        mRemoteControlBinding.otaSelectButton.setOnClickListener(onClick_selectButton);
        mRemoteControlBinding.otaFactoryResetButton.setOnClickListener(onClick_factoryResetButton);
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

            byte[] packet = new byte[2];

            packet[0] = PacketInfo.HEADER_BOOT_STATUS;
            packet[1] = 1;

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

            byte[] packet = new byte[3];

            packet[0] = PacketInfo.HEADER_BOOT_STATUS;
            packet[1] = 2;
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
            byte[] packet = new byte[3];

            packet[0] = PacketInfo.HEADER_BOOT_STATUS;
            packet[1] = 2;
            packet[2] = (byte) 0xFF;

            mMainActivity.sendPacket(packet);
        }
    };

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