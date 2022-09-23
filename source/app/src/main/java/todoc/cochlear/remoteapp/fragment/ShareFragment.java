package todoc.cochlear.remoteapp.fragment;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.Arrays;
import java.util.List;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.activity.databinding.FragmentShareBinding;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.maps.EntityMap;
import todoc.cochlear.remoteapp.database.maps.UtilMap;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.list.ShareCollectMapAdapter;
import todoc.cochlear.remoteapp.list.ShareDistributeMapAdapter;
import todoc.cochlear.remoteapp.list.ShareExistMapAdapter;
import todoc.cochlear.remoteapp.list.ShareMapResetDefaultAdapter;
import todoc.cochlear.remoteapp.list.ShareSelectUserAdapter;
import todoc.cochlear.remoteapp.params.MapInfo;
import todoc.cochlear.remoteapp.params.PacketInfo;
import todoc.cochlear.remoteapp.params.Status;

public class ShareFragment extends Fragment
{
    static private final String TAG = "TODOC_" + ShareFragment.class.getSimpleName();

    private ActivityMainBinding mMainBinding;
    private FragmentShareBinding mShareBinding;

    public ShareSelectUserAdapter mSelectUserAdapter;
    public ShareCollectMapAdapter mCollectMapAdapter;
    public ShareDistributeMapAdapter mShareMapAdapter;
    public ShareExistMapAdapter mExistMapAdapter;
    public ShareMapResetDefaultAdapter mMapResetAdapter;

    public BluetoothDevice mConnBtDevice;

    Handler percentHandler = new Handler(Looper.getMainLooper());
    Runnable percentRunner1;
    Runnable percentRunner2;
    Runnable distPercentRunner1;
    Runnable distPercentRunner2;
    static private int MY_DEMO_DELAY_IN_MS = 20;

    static public int BUTTON_COLOR_NORMAL = 0;
    static public int BUTTON_COLOR_ACCENT = 0;
    static public int BUTTON_COLOR_ERROR = 0;

    static public final int FSM_FIRST_SCREEN = 0;
    static public final int FSM_SELECT_USER_SCREEN = 1;
    static public final int FSM_COLLECT_MAP_SCREEN = 2;
    static public final int FSM_SHARE_MAP_SCREEN = 3;
    static public final int FSM_EXIST_MAP_SCREEN = 4;
    static public final int FSM_MAP_RESET_DEFAULT_SCREEN = 5;
    public int mFsm;

    static public final int COLLECT_FSM_IDLE = 0;
    static public final int COLLECT_FSM_CONNECTING = 1;
    static public final int COLLECT_FSM_COLLECTING = 2;
    static public final int COLLECT_FSM_COLLECTED = 3;
    static public final int COLLECT_FSM_DISCONNECTING = 4;
    public int mCollectFsm;

    static public final int SHARE_FSM_IDLE = 0;
    static public final int SHARE_FSM_CONNECTING = 1;
    static public final int SHARE_FSM_USER_CHECK = 2;
    static public final int SHARE_FSM_SHARING = 3;
    static public final int SHARE_FSM_SHARED = 4;
    static public final int SHARE_FSM_DISCONNECTING = 5;
    public int mShareFsm;

    static public final int RESET_FSM_IDLE = 0;
    static public final int RESET_FSM_CONNECTING = 1;
    static public final int RESET_FSM_RESETTING = 2;
    static public final int RESET_FSM_DONE = 3;
    static public final int RESET_FSM_DISCONNECTING = 4;
    public int mResetFsm;

    static public final int COLLECT_DATA_TYPE_ID_AND_USER = 0;
    static public final int COLLECT_DATA_TYPE_MAP_DATA = 1;
    public int mCollectDataType;

    static public final int SHARE_DATA_TYPE_READ_ID_AND_USER = 2;
    static public final int SHARE_DATA_TYPE_WRITE_ID_AND_USER = 3;
    static public final int SHARE_DATA_TYPE_WRITE_MAP_DATA = 4;
    public int mShareDataType;

    // 맵 공유에 사용되는 버퍼
    MapInfo mMostRecentMapInfo;
    MapInfo[] mMapInfo;
    MapInfo mShareMapInfo;
    public int mSelectedUserCount;
    public int mMapInfoIndex;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mMainBinding = ((MainActivity) requireContext()).mBinding;
        mMainBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mMainBinding.toolbarNavigationMessage.setText("메뉴");
        mMainBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mMainBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        //mMainBinding.toolbar.setTitleTextAppearance(requireContext(), R.style.TextAppearance_RemoteControl_Default_Headline6);
        mMainBinding.toolbar.setTitle("외부기 공유");

        mFsm = FSM_FIRST_SCREEN;
        mCollectFsm = COLLECT_FSM_IDLE;
        mShareFsm = SHARE_FSM_IDLE;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mShareBinding = FragmentShareBinding.inflate(inflater, container, false);
        return mShareBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        BUTTON_COLOR_NORMAL = requireActivity().getColor(R.color.white_70);
        BUTTON_COLOR_ACCENT = requireActivity().getColor(R.color.accent_70);
        BUTTON_COLOR_ERROR = requireActivity().getColor(R.color.error_70);

        initOkButton();
        initDeleteButton();
        updateScreen();

        mSelectedUserCount = 0;

        temporaryTest();
    }

    //
    // 각종 임시 테스트를 위한 함수
    //
    public void temporaryTest()
    {
        byte[] srcBytes = new byte[]{1, 2, 3, 4, 'A', 'B', 'C', 'D', 'E', 0, 0, 1, 2, 3, 4, 0, 0, 0, 127, -128};
        String srcString = MapInfo.bytesToString(srcBytes);
        byte[] dstBytes = MapInfo.stringToBytes(srcString);

        Log.d("TemporaryTest", srcString);
        Log.d("TemporaryTest", ((MainActivity) requireActivity()).printLogBytesToString(srcBytes));
        Log.d("TemporaryTest", ((MainActivity) requireActivity()).printLogBytesToString(dstBytes));
    }

    //
    // 2. 매핑 데이터 수집 화면에서 수집 중 퍼센트 증가 디버그 함수
    //
    public void debugPercentCollectMap1()
    {
        new Handler(Looper.getMainLooper()).postDelayed(() ->
        {
            String strPercent = mShareBinding.shareCollectMapDataPercentTv.getText().toString();
            String[] splits = strPercent.split("%");
            int intPercent = Integer.parseInt(splits[0]);

            if (intPercent == 100)
            {
                mCollectFsm = COLLECT_FSM_COLLECTED;

                for (int i = 0; i < mCollectMapAdapter.getItemCount(); i++)
                {
                    if (mCollectMapAdapter.isSelected(i))
                    {
                        mCollectMapAdapter.setCollect(i);
                        setCollectInfoText(i);

                        if (mCollectMapAdapter.getCollectedCount() != mCollectMapAdapter.getItemCount())
                        {
                            setOkButtonVisibility(false);
                        }
                        else
                        {
                            setOkButtonVisibility(true);
                            setOkButtonText("다음 절차 진행");
                            setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
                        }
                        break;
                    }
                }
            }
            else
            {
                intPercent++;
                setCollectPercent(true, intPercent);
                debugPercentCollectMap1();
            }
        }, 20);
    }

    //
    // 3. 매핑 데이터 공유 화면에서 수집 중 퍼센트 증가 디버그 함수
    //
    public void debugPercentShareMap1()
    {
        new Handler(Looper.getMainLooper()).postDelayed(() ->
        {
            String strPercent = mShareBinding.shareDistributePercentTv.getText().toString();
            String[] splits = strPercent.split("%");
            int intPercent = Integer.parseInt(splits[0]);

            if (intPercent == 100)
            {
                mShareFsm = SHARE_FSM_SHARED;

                for (int i = 0; i < mShareMapAdapter.getItemCount(); i++)
                {
                    if (mShareMapAdapter.isSelected(i))
                    {
                        mShareMapAdapter.setShare(i, true);
                        setShareInfoText(i);

                        Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                        ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
                        break;
                    }
                }
            }
            else
            {
                intPercent++;
                setSharePercent(true, intPercent);
                debugPercentShareMap1();
            }
        }, 20);
    }

    public void updateScreen()
    {
        switch (mFsm)
        {
            case FSM_FIRST_SCREEN:
                updateFsmFirstScreen();
                break;

            case FSM_SELECT_USER_SCREEN:
                updateFsmSelectUSerScreen();
                break;

            case FSM_COLLECT_MAP_SCREEN:
                updateFsmCollectMapScreen();
                break;

            case FSM_SHARE_MAP_SCREEN:
                updateFsmShareMapScreen();
                break;

            case FSM_EXIST_MAP_SCREEN:
                updateFsmExistMapScreen();
                break;

            case FSM_MAP_RESET_DEFAULT_SCREEN:
                updateFsmMapResetDefaultScreen();
                break;

            default:
                break;
        }
    }

    //
    // 리사이클러뷰 준비 - 1. 내부기 선택 화면
    //
    public void updateListSelectUser()
    {
        if (mSelectUserAdapter == null)
        {
            mSelectUserAdapter = new ShareSelectUserAdapter();
            mShareBinding.shareSelectIsdUserRecyclerview.setAdapter(mSelectUserAdapter);
            mShareBinding.shareSelectIsdUserRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        mSelectUserAdapter.clearItems();

        List<EntityUser> registeredUsers = UtilUser.instance.getUsers();
        mSelectUserAdapter.addUsers(registeredUsers);
    }

    //
    // 리사이클러뷰 준비 - 2. 매핑 데이터 수집 화면
    //
    public void updateListCollectMap()
    {
        if (mCollectMapAdapter == null)
        {
            mCollectMapAdapter = new ShareCollectMapAdapter(this);
            mShareBinding.shareCollectMapDataRecyclerview.setAdapter(mCollectMapAdapter);
            mShareBinding.shareCollectMapDataRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        mCollectMapAdapter.clearItems();
        mCollectMapAdapter.addUsers(mSelectUserAdapter.getSelectedItems());
    }

    //
    // 리사이클러뷰 준비 - 3. 매핑 데이터 공유 화면
    //
    public void updateListShareMap()
    {
        if (mShareMapAdapter == null)
        {
            mShareMapAdapter = new ShareDistributeMapAdapter(this);
            mShareBinding.shareDistributeMapDataRecyclerview.setAdapter(mShareMapAdapter);
            mShareBinding.shareDistributeMapDataRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        mShareMapAdapter.clearItems();
    }

    //
    // 리사이클러뷰 준비 - 이미 등록된 맵 정보 화면
    //
    public void updateListExistMap()
    {
        if (mExistMapAdapter == null)
        {
            mExistMapAdapter = new ShareExistMapAdapter();
            mShareBinding.shareExistMapRecyclerview.setAdapter(mExistMapAdapter);
            mShareBinding.shareExistMapRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        mExistMapAdapter.clearItems();
        mExistMapAdapter.addMaps(UtilMap.instance.getAll());
    }

    //
    // 리사이클러뷰 준비 - 최초 매핑 시점 복귀 화면
    //
    public void updateListResetMap()
    {
        if (mMapResetAdapter == null)
        {
            mMapResetAdapter = new ShareMapResetDefaultAdapter(this);
            mShareBinding.shareMapResetDefaultRecyclerview.setAdapter(mMapResetAdapter);
            mShareBinding.shareMapResetDefaultRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        mMapResetAdapter.clearItems();
    }

    //
    // 화면 업데이트 - 맨 처음 진행순서 설명화면
    //
    public void updateFsmFirstScreen()
    {
        //버튼 설정
        setOkButtonText("진행");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(true);
        setDeleteButtonText("외부기 맵 데이터 초기화");
        setDeleteButtonBackgroundColor(BUTTON_COLOR_ERROR);
        setDeleteButtonVisibility(true);

        // 레이아웃 설정
        setLayoutSelectUser(false);
        setLayoutCollectMap(false);
        setLayoutDistributeMap(false);
        setLayoutFirstScreen(true);
        setLayoutMapResetDefault(false);
        setLayoutExistMap(false);
    }

    //
    // 화면 업데이트 - 1. 내부기 선택 화면
    //
    public void updateFsmSelectUSerScreen()
    {
        // 버튼 설정
        setOkButtonText("다음 절차 진행");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(true);
        setDeleteButtonVisibility(false);

        // 레이아웃 설정
        setLayoutFirstScreen(false);
        setLayoutCollectMap(false);
        setLayoutDistributeMap(false);
        setLayoutSelectUser(true);
        setLayoutMapResetDefault(false);
        setLayoutExistMap(false);

        // 리사이클러뷰 설정
        updateListSelectUser();
    }

    //
    // 화면 업데이트 - 2. 매핑 데이터 수집 화면
    //
    public void updateFsmCollectMapScreen()
    {
        // 버튼 설정
        setOkButtonText("시작");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(false);
        setDeleteButtonVisibility(false);

        // 레이아웃 설정
        setLayoutFirstScreen(false);
        setLayoutSelectUser(false);
        setLayoutDistributeMap(false);
        setLayoutCollectMap(true);
        setLayoutMapResetDefault(false);
        setLayoutExistMap(false);

        // 리사이클러뷰 설정
        updateListCollectMap();
        mCollectFsm = COLLECT_FSM_IDLE;
        ((MainActivity) requireActivity()).scanLeWithDelay(true, 10);

        mMapInfo = new MapInfo[mSelectedUserCount];
    }

    //
    // 화면 업데이트 - 3. 매핑 데이터 공유 화면
    //
    public void updateFsmShareMapScreen()
    {
        // 버튼 설정
        setOkButtonText("시작");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(false);
        setDeleteButtonVisibility(false);

        // 레이아웃 설정
        setLayoutFirstScreen(false);
        setLayoutSelectUser(false);
        setLayoutCollectMap(false);
        setLayoutDistributeMap(true);
        setLayoutMapResetDefault(false);
        setLayoutExistMap(false);

        // 리사이클러뷰 설정
        updateListShareMap();
        mShareFsm = SHARE_FSM_IDLE;
        ((MainActivity) requireActivity()).scanLeWithDelay(true, 10);
    }

    //
    // 화면 업데이트 - 이미 등록된 매핑 데이터 존재
    //
    public void updateFsmExistMapScreen()
    {
        // 버튼 설정
        setOkButtonText("새로운 매핑 데이터 수집 진행");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(true);
        setDeleteButtonText("시작");
        setDeleteButtonBackgroundColor(BUTTON_COLOR_NORMAL);
        setDeleteButtonVisibility(true);

        // 레이아웃 설정
        setLayoutFirstScreen(false);
        setLayoutSelectUser(false);
        setLayoutCollectMap(false);
        setLayoutDistributeMap(false);
        setLayoutMapResetDefault(false);
        setLayoutExistMap(true);

        // 리사이클러뷰 설정
        updateListExistMap();
    }

    //
    // 화면 업데이트 - 최초 매핑 시점 복귀 화면
    //
    public void updateFsmMapResetDefaultScreen()
    {
        // 버튼 설정
        setOkButtonText("초기화");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(false);
        setDeleteButtonVisibility(false);

        // 레이아웃 설정
        setLayoutFirstScreen(false);
        setLayoutSelectUser(false);
        setLayoutCollectMap(false);
        setLayoutDistributeMap(false);
        setLayoutExistMap(false);
        setLayoutMapResetDefault(true);

        // 리사이클러뷰 설정
        updateListResetMap();

        mResetFsm = RESET_FSM_IDLE;

        Status status = Status.instance();
        MainActivity activity = (MainActivity) requireActivity();

        if (status.connectionState == Status.CONNECTION_STATE_CONNECTED)
        {
            /*
            String name = status.connectedUser.name.substring(0, status.connectedUser.name.length() - 2);
            String ear = status.connectedUser.ear;
            String serial = status.connectedDevice.serialNumber;
            BluetoothDevice btDevice = activity.mBluetoothDevice;
            mMapResetAdapter.addItem(name, ear, serial, btDevice);
            */
            status.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
            activity.mBluetoothGatt.disconnect();
        }

        ((MainActivity) requireActivity()).scanLeWithDelay(true, 10);
    }

    //
    // 주 확인 버튼의 맨 처음 진행순서 설명화면 클릭 핸들러
    //
    public void okButtonFirstScreen()
    {
        // 이미 등록된 맵 정보가 있는지 체크해야 한다.
        List<EntityMap> maps = UtilMap.instance.getAll();

        if (maps == null || maps.size() == 0) // 저장된 맵 데이터 없음
        {
            mFsm = FSM_SELECT_USER_SCREEN;
        }
        else // 저장된 맵 데이터 있음
        {
            mFsm = FSM_EXIST_MAP_SCREEN;
        }

        updateScreen();
    }

    //
    // 주 확인 버튼의 1. 내부기 선택 화면 클릭 핸들러
    //
    public void okButtonSelectUserScreen()
    {
        if (mSelectUserAdapter == null)
        {
            return;
        }

        Log.d(TAG, "선택된 내부기의 수는 " + mSelectUserAdapter.getSelectedCount() + " 입니다.");

        mSelectedUserCount = mSelectUserAdapter.getSelectedCount();

        if (1 < mSelectUserAdapter.getSelectedCount())
        {
            if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
            {
                Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
            }

            mFsm = FSM_COLLECT_MAP_SCREEN;
            updateScreen();
        }
        else
        {
            Toast.makeText(requireActivity(), "내부기를 최소 2 이상 선택해주세요.", Toast.LENGTH_LONG).show();
        }
    }

    //
    // 주 확인 버튼의 2. 매핑 데이터 수집 화면 클릭 핸들러
    //
    public void okButtonCollectMapScreen()
    {
        if (mCollectMapAdapter.getCollectedCount() == mCollectMapAdapter.getItemCount())
        {
            // 수집 완료된 상태
            Log.d(TAG, "연결되어 있는 상태라면 연결을 해제하고, 다음 절차로 진행합니다.");

            if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
            {
                Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
            }

            mFsm = FSM_SHARE_MAP_SCREEN;
            updateScreen();
        }
        else
        {
            for (int i = 0; i < mCollectMapAdapter.getItemCount(); i++)
            {
                if (mCollectMapAdapter.isSelected(i) && mCollectMapAdapter.isScanned(i))
                {
                    String serial = mCollectMapAdapter.getSerial(i);
                    if (UtilDevice.instance.getDeviceBySerialNumber(serial) == null)
                    {
                        Toast.makeText(requireContext(), "등록되지 않은 외부기입니다. 등록 먼저 해주세요.", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
                        {
                            Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                            ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
                        }

                        mMapInfoIndex = i;

                        if (mMapInfo == null || mMapInfo.length != mCollectMapAdapter.getItemCount())
                        {
                            mMapInfo = new MapInfo[mCollectMapAdapter.getItemCount()];
                        }

                        if (mMapInfo[mMapInfoIndex] == null)
                        {
                            mMapInfo[mMapInfoIndex] = new MapInfo();
                        }

                        //Status.instance().connectionState = Status.CONNECTION_STATE_CONNECTING;
                        ((MainActivity) requireActivity()).scanLe(false);

                        Status.instance().connectedUser = mCollectMapAdapter.getItem(i);
                        Status.instance().connectedDevice = UtilDevice.instance.getDeviceBySerialNumber(serial);
                        mConnBtDevice = mCollectMapAdapter.getBtDevice(i);

                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                        {
                            Status.instance().connectionState = Status.CONNECTION_STATE_CONNECTING;

                            if (mCollectFsm != COLLECT_FSM_COLLECTING)
                            {
                                mCollectFsm = COLLECT_FSM_CONNECTING;

                                mCollectDataType = COLLECT_DATA_TYPE_ID_AND_USER;

                                mMapInfo[mMapInfoIndex].idUser.slotNum = MapInfo.SLOT_MIN;
                                mMapInfo[mMapInfoIndex].idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;
                                mMapInfo[mMapInfoIndex].mapData.slotNum = MapInfo.SLOT_MIN;
                                mMapInfo[mMapInfoIndex].mapData.mapNum = MapInfo.MAP_DATA_MAP_MIN;
                                mMapInfo[mMapInfoIndex].mapData.indexNum = MapInfo.MAP_DATA_INDEX_MIN;

                                mMapInfo[mMapInfoIndex].dataType = MapInfo.DATA_TYPE_ID_USER;
                            }

                            ((MainActivity) requireActivity()).mBluetoothDevice = mConnBtDevice;
                            ((MainActivity) requireActivity()).mBluetoothGatt =
                                    mConnBtDevice.connectGatt(requireContext().getApplicationContext(), false, ((MainActivity) requireActivity()).mGattCallback);

                            if (((MainActivity) requireActivity()).mBluetoothGatt == null)
                            {
                                Log.d(TAG, "수집 화면에서 BLE 연결 시도가 실패했습니다.");

                                setOkButtonVisibility(true);
                                Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTED;
                                Toast.makeText(requireContext(), "연결 실패입니다. 다시 연결 해주세요.", Toast.LENGTH_LONG).show();
                            }
                            else
                            {
                                setOkButtonVisibility(false);
                            }
                        }, 100);
                    }
                }
            }
        }
    }

    //
    // 주 확인 버튼의 3. 매핑 데이터 공유 화면 클릭 핸들러
    //
    public void okButtonShareMapScreen()
    {
        if (mShareMapAdapter.getSharedCount() == mShareMapAdapter.getItemCount())
        {
            Log.d(TAG, "모든 외부기로 공유가 완료된 상태입니다.");
        }
        else
        {
            for (int i = 0; i < mShareMapAdapter.getItemCount(); i++)
            {
                if (mShareMapAdapter.isSelected(i) && !mShareMapAdapter.isShared(i))
                {
                    if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
                    {
                        Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                        ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
                    }

                    if (mShareMapInfo == null)
                    {
                        mShareMapInfo = new MapInfo();
                    }

                    ((MainActivity) requireActivity()).scanLe(false);

                    Status.instance().connectedUser = mShareMapAdapter.getEntityUser(i);
                    Status.instance().connectedDevice = mShareMapAdapter.getEntityDevice(i);
                    mConnBtDevice = mShareMapAdapter.getBtDevice(i);

                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        Status.instance().connectionState = Status.CONNECTION_STATE_CONNECTING;
                        if (mShareFsm != SHARE_FSM_USER_CHECK && mShareFsm != SHARE_FSM_SHARING)
                        {
                            mShareFsm = SHARE_FSM_CONNECTING;

                            mShareDataType = SHARE_DATA_TYPE_READ_ID_AND_USER;

                            mShareMapInfo.idUser.slotNum = MapInfo.SLOT_MIN;
                            mShareMapInfo.idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;
                            mShareMapInfo.dataType = MapInfo.DATA_TYPE_ID_USER;
                        }


                        ((MainActivity) requireActivity()).mBluetoothDevice = mConnBtDevice;
                        ((MainActivity) requireActivity()).mBluetoothGatt =
                                mConnBtDevice.connectGatt(requireContext().getApplicationContext(), false, ((MainActivity) requireActivity()).mGattCallback);

                        if (((MainActivity) requireActivity()).mBluetoothGatt == null)
                        {
                            Log.d(TAG, "공유 화면에서 BLE 연결 시도가 실패했습니다.");

                            setOkButtonVisibility(true);
                            Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTED;
                            Toast.makeText(requireContext(), "연결 실패입니다. 다시 연결 해주세요.", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            setOkButtonVisibility(false);
                        }
                    }, 100);

                    break;
                }
            }
        }
    }

    //
    // 주 확인 버튼의 이미 등록된 맵 정보 화면 핸들러
    //
    public void okButtonExistMapScreen()
    {
        mFsm = FSM_SELECT_USER_SCREEN;
        updateScreen();
    }

    //
    // 주 확인 버튼의 최초 매핑 시점 복귀 화면 핸들러
    //
    public void okButtonMapReset()
    {
        if (mMapResetAdapter.getItemCount() == mMapResetAdapter.getResetDoneCount())
        {
            Log.d(TAG, "모든 외부기가 최초 매핑 시점으로 복귀된 상태입니다.");
            return;
        }

        MainActivity activity = (MainActivity) requireActivity();
        Status status = Status.instance();

        for (int i = 0; i < mMapResetAdapter.getItemCount(); i++)
        {
            if (mMapResetAdapter.isSelected(i) && !mMapResetAdapter.isResetDone(i))
            {
                if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
                {
                    Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                    activity.mBluetoothGatt.disconnect();
                }

                activity.scanLe(false);

                status.connectedUser = mMapResetAdapter.getEntityUser(i);
                status.connectedDevice = mMapResetAdapter.getEntityDevice(i);
                mConnBtDevice = mMapResetAdapter.getBtDevice(i);

                new Handler(Looper.getMainLooper()).postDelayed(() ->
                {
                    status.connectionState = Status.CONNECTION_STATE_CONNECTING;
                    mResetFsm = RESET_FSM_CONNECTING;

                    activity.mBluetoothDevice = mConnBtDevice;
                    activity.mBluetoothGatt = mConnBtDevice.connectGatt(activity, false, activity.mGattCallback);

                    if (activity.mBluetoothGatt == null)
                    {
                        Log.d(TAG, "맵 초기화 화면에서 BLE 연결 시도가 실패했습니다.");

                        setOkButtonVisibility(true);
                        status.connectionState = Status.CONNECTION_STATE_DISCONNECTED;
                        Toast.makeText(requireContext(), "연결 실패입니다. 다시 연결 해주세요.", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        setOkButtonVisibility(false);
                    }
                }, 100);

                break;
            }
        }
    }

    //
    // 주 확인 버튼 초기화 함수
    //
    public void initOkButton()
    {
        mShareBinding.shareOkButton.setOnClickListener(view ->
        {
            switch (mFsm)
            {
                case FSM_FIRST_SCREEN:
                    okButtonFirstScreen();
                    break;

                case FSM_SELECT_USER_SCREEN:
                    okButtonSelectUserScreen();
                    break;

                case FSM_COLLECT_MAP_SCREEN:
                    okButtonCollectMapScreen();
                    break;

                case FSM_SHARE_MAP_SCREEN:
                    okButtonShareMapScreen();
                    break;

                case FSM_EXIST_MAP_SCREEN:
                    okButtonExistMapScreen();
                    break;

                case FSM_MAP_RESET_DEFAULT_SCREEN:
                    okButtonMapReset();
                    break;
            }
        });
    }

    //
    // 주 취소 버튼의 맨 처음 진행순서 설명화면 클릭 핸들러
    //
    public void deleteButtonFirstScreen()
    {
        mFsm = FSM_MAP_RESET_DEFAULT_SCREEN;
        updateScreen();
    }

    //
    // 주 취소 버튼의 이미 등록된 맵 정보 화면 핸들러
    //
    public void deleteButtonMapScreen()
    {
        // 바로 공유하는 화면으로 어떻게 넘어갈까?
        Log.d(TAG, "이미 등록된 매핑 데이터를 공유하도록 합니다.");
        Log.d(TAG, "이를 위해서 mCollectAdapter에 매핑 데이터가 수집된 것처럼 설정하고, LE 검색에서 수집된 기기로 인식하게 합니다.");

        if (mCollectMapAdapter == null)
        {
            mCollectMapAdapter = new ShareCollectMapAdapter(this);
            mShareBinding.shareCollectMapDataRecyclerview.setAdapter(mCollectMapAdapter);
            mShareBinding.shareCollectMapDataRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        mCollectMapAdapter.clearItems();

        for (int i = 0; i < mExistMapAdapter.getItemCount(); i++)
        {
            mCollectMapAdapter.addItem(mExistMapAdapter.getUser(i));
            mCollectMapAdapter.setCollect(i);
        }

        // 최신 맵 구성하기
        if (mMostRecentMapInfo == null)
        {
            mMostRecentMapInfo = new MapInfo();
        }

        List<EntityMap> maps = UtilMap.instance.getAll();


        for (int slot_init_i = 0; slot_init_i < 4; slot_init_i++)
        {
            MapInfo.setMapInfoFromString(MapInfo.EMPTY_MAP_DATA_TD_OTE, mMostRecentMapInfo, slot_init_i);
        }

        mMostRecentMapInfo.metadata.isFilled = true;

        if (maps != null)
        {
            for (int slot_i = 0; slot_i < maps.size(); slot_i++)
            {
                MapInfo.setMapInfoFromString(maps.get(slot_i).serialize_map_data, mMostRecentMapInfo, slot_i);
            }
        }

        mMostRecentMapInfo.updateMetadata();

        for (int print_i = 0; print_i < 4; print_i++)
        {
            Log.d(TAG, "mMostRecentMapInfo -> slot " + print_i
                    + " : name = " + mMostRecentMapInfo.metadata.names[print_i]
                    + ", ear = " + mMostRecentMapInfo.metadata.ears[print_i]
                    + ", version = " + mMostRecentMapInfo.metadata.stamps[print_i]);
        }

        if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
        {
            Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
            ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
        }

        mFsm = FSM_SHARE_MAP_SCREEN;
        updateScreen();
    }

    //
    // 맵 데이터 초기화 버튼 함수
    //
    public void initDeleteButton()
    {
        mShareBinding.shareMapDeleteButton.setOnClickListener(view ->
        {
            switch (mFsm)
            {
                case FSM_FIRST_SCREEN:
                    deleteButtonFirstScreen();
                    break;

                case FSM_EXIST_MAP_SCREEN:
                    deleteButtonMapScreen();
                    break;
            }
        });
    }

    //
    // 리사이클러뷰 클릭 이벤트 - 2. 매핑 데이터 수집 화면
    //
    public void listClickListenerCollectMap(int position)
    {
        setCollectInfoText(position);
        setCollectInfoVisibility(mCollectMapAdapter.isSelected(position));
        setOkButtonVisibility(mCollectMapAdapter.isScanned(position) && mCollectMapAdapter.isSelected(position) && !mCollectMapAdapter.isCollected(position));

        if (mCollectMapAdapter.isCollected(position))
        {
            setCollectPercent(true, 100);
        }
        else
        {
            setCollectPercent(false, 0);
        }

        if (mCollectMapAdapter.getCollectedCount() == mCollectMapAdapter.getItemCount())
        {
            setOkButtonVisibility(true);
            setOkButtonText("다음 절차 진행");
            setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        }
    }

    //
    // 리사이클러뷰 클릭 이벤트 - 3. 매핑 데이터 공유 화면
    //
    public void listClickListenerShareMap(int position, EntityUser entityUser, EntityDevice entityDevice, BluetoothDevice btDevice)
    {
        setShareInfoText(position);
        setShareInfoVisibility(mShareMapAdapter.isSelected(position));
        setOkButtonVisibility(mShareMapAdapter.isSelected(position) && !mShareMapAdapter.isShared(position));

        if (mShareMapAdapter.isShared(position))
        {
            setSharePercent(true, 100);
        }
        else
        {
            setSharePercent(false, 0);
        }
    }

    //
    // 리사이클러뷰 클릭 이벤트 - 최초 매핑 시점 복귀 화면
    //
    public void listClickListenerMapReset(int position, EntityUser entityUser, EntityDevice entityDevice, BluetoothDevice btDevice)
    {
        setMapResetInfoText(position);
        setMapResetInfoVisibility(mMapResetAdapter.isSelected(position));
        setOkButtonVisibility(mMapResetAdapter.isSelected(position) && !mMapResetAdapter.isResetDone(position));
    }

    //
    // 최초 매핑 시점 복귀 화면의 새롭게 검색된 상태 정보 업데이트
    //
    public void scanListUpdateMapReset()
    {
        Status status = Status.instance();
        MainActivity activity = (MainActivity) requireActivity();

        for (int i = 0; i < mMapResetAdapter.getItemCount(); i++)
        {
            if (mMapResetAdapter.isSelected(i))
            {
                int position = i;

                new Handler(Looper.getMainLooper()).post(() ->
                {
                    setMapResetInfoText(position);
                });

                break;
            }
        }
    }

    //
    // 레이아웃 Visibility 설정 간편 함수들
    //
    public void setLayoutFirstScreen(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareFirstScreenLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareFirstScreenLayout.setVisibility(View.GONE);
        }
    }

    public void setLayoutSelectUser(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareSelectIsdLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareSelectIsdLayout.setVisibility(View.GONE);
        }
    }

    public void setLayoutCollectMap(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareCollectMapDataLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareCollectMapDataLayout.setVisibility(View.GONE);
        }
    }

    public void setLayoutDistributeMap(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareDistributeMapDataLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareDistributeMapDataLayout.setVisibility(View.GONE);
        }
    }

    public void setLayoutExistMap(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareExistMapLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareExistMapLayout.setVisibility(View.GONE);
        }
    }

    public void setLayoutMapResetDefault(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareMapResetDefaultLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareMapResetDefaultLayout.setVisibility(View.GONE);
        }
    }

    //
    // 2. 매핑 데이터 수집 화면의 맵 수집 정보 레이아웃 간편 함수들
    //
    public void setCollectInfoVisibility(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareCollectMapDataInfoLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareCollectMapDataInfoLayout.setVisibility(View.GONE);
        }
    }

    public void setCollectInfoText(int position)
    {
        String user = mCollectMapAdapter.getName(position);
        String ear = mCollectMapAdapter.getEar(position);
        String serial = mCollectMapAdapter.getSerial(position);
        String info;

        if (ear.equals(EntityUser.EAR_LEFT))
        {
            ear = "왼쪽";
        }
        else
        {
            ear = "오른쪽";
        }

        if (mCollectMapAdapter.isCollected(position))
        {
            if (mCollectMapAdapter.getItemCount() == mCollectMapAdapter.getCollectedCount())
            {
                info = "모든 매핑 데이터 수집이 완료되었습니다.";
            }
            else
            {
                info = user + "님 " + ear + " 내부기의 매핑 데이터는 수집 완료되었습니다.";
            }
        }
        else
        {
            if (mCollectMapAdapter.isScanned(position))
            {
                info = user + "님 " + ear + " 내부기의 매핑 데이터 수신을 진행합니다.";
            }
            else
            {
                info = user + "님 " + ear + " 내부기와 연결된 외부기가 검색되지 않았습니다.";
            }
        }
        mShareBinding.shareCollectMapDataProgressTv.setText(info);
    }

    public void setCollectPercent(boolean visible, int percent)
    {
        if (visible)
        {
            mShareBinding.shareCollectMapDataPercentTv.setVisibility(View.VISIBLE);
            mShareBinding.shareCollectMapDataPercentPb.setProgress(percent);
            mShareBinding.shareCollectMapDataPercentTv.setText(percent + "%");
        }
        else
        {
            mShareBinding.shareCollectMapDataPercentPb.setProgress(0);
            mShareBinding.shareCollectMapDataPercentTv.setVisibility(View.INVISIBLE);
        }

        Log.d(TAG, "수집 화면 프로그래스 출력 상태 = " + visible + ", 퍼센트 = " + percent);
    }

    //
    // 3. 매핑 데이터 공유 화면의 맵 공유 정보 레이아웃 간편 함수들
    //
    public void setShareInfoVisibility(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareDistributeTransferLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareDistributeTransferLayout.setVisibility(View.GONE);
        }
    }

    public void setShareInfoText(int position)
    {
        String serial = mShareMapAdapter.getSerial(position);
        String info;

        if (mShareMapAdapter.isShared(position))
        {
            if (mShareMapAdapter.getItemCount() == mShareMapAdapter.getSharedCount())
            {
                info = "모든 외부기에 매핑 데이터 전송을 완료했습니다.";
            }
            else
            {
                info = serial + " 외부기에 매핑 데이터 전송을 완료했습니다.";
            }
        }
        else
        {
            info = serial + " 외부기에 매핑 데이터를 전송합니다. 진행 중에는 소리가 들리지 않습니다.";
        }

        mShareBinding.shareDistributeProgressTv.setText(info);
    }

    public void setSharePercent(boolean visible, int percent)
    {
        if (visible)
        {
            mShareBinding.shareDistributePercentTv.setVisibility(View.VISIBLE);
            mShareBinding.shareDistributePercentProgressbar.setProgress(percent);
            mShareBinding.shareDistributePercentTv.setText(percent + "%");
        }
        else
        {
            mShareBinding.shareDistributePercentProgressbar.setProgress(0);
            mShareBinding.shareDistributePercentTv.setVisibility(View.INVISIBLE);
        }

        Log.d(TAG, "공유 화면 프로그래스 출력 상태 = " + visible + ", 퍼센트 = " + percent);
    }

    //
    // 최초 매핑 시점 복귀 화면 레이아웃 간편 함수들
    //
    public void setMapResetInfoVisibility(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareMapResetDefaultInfoLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareMapResetDefaultInfoLayout.setVisibility(View.GONE);
        }
    }

    public void setMapResetInfoText(int position)
    {
        String serial = mMapResetAdapter.getSerial(position);
        String info;

        if (mMapResetAdapter.isResetDone(position))
        {
            if (mMapResetAdapter.getItemCount() == mMapResetAdapter.getResetDoneCount())
            {
                info = "모든 외부기를 최초 매핑 시점으로 되돌렸습니다.";
            }
            else
            {
                info = serial + " 외부기를 최초 매핑 시점으로 되돌렸습니다.";
            }
        }
        else
        {
            info = serial + " 외부기를 최초 매핑 시점으로 되돌립니다. 진행 중에는 소리가 들리지 않습니다.";
        }

        mShareBinding.shareMapResetDefaultProgressTv.setText(info);
    }

    //
    // 주 확인 버튼 간편 함수들
    //
    public void setOkButtonVisibility(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareOkButton.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareOkButton.setVisibility(View.GONE);
        }
    }

    public void setOkButtonBackgroundColor(int color)
    {
        mShareBinding.shareOkButton.setBackgroundColor(color);
    }

    public void setOkButtonText(String text)
    {
        mShareBinding.shareOkButton.setText(text);
    }

    //
    // 맵 데이터 삭제 버튼 간편 함수들
    //
    public void setDeleteButtonVisibility(boolean visible)
    {
        if (visible)
        {
            mShareBinding.shareMapDeleteButton.setVisibility(View.VISIBLE);
        }
        else
        {
            mShareBinding.shareMapDeleteButton.setVisibility(View.GONE);
        }
    }

    public void setDeleteButtonBackgroundColor(int color)
    {
        mShareBinding.shareMapDeleteButton.setBackgroundColor(color);
    }

    public void setDeleteButtonText(String text)
    {
        mShareBinding.shareMapDeleteButton.setText(text);
    }

    //
    // 맵 데이터 수집을 위한 패킷을 어떻게 전송할지 판단하고, 해당 패킷을 전송하는 함수
    //
    public void whichPacketShouldBeTransferred()
    {
        if (mFsm == FSM_COLLECT_MAP_SCREEN)
        {
            mCollectFsm = COLLECT_FSM_COLLECTING;

            new Handler(Looper.getMainLooper()).post(() ->
            {
                boolean connError = false;

                if (mCollectDataType == COLLECT_DATA_TYPE_ID_AND_USER)
                {
                    if (mMapInfo[mMapInfoIndex].idUser.slotNum != MapInfo.SLOT_MIN
                            || mMapInfo[mMapInfoIndex].idUser.indexNum != MapInfo.ID_USER_INDEX_MIN)
                    {
                        connError = true;
                    }
                }
                else if (mCollectDataType == COLLECT_DATA_TYPE_MAP_DATA)
                {
                    if (mMapInfo[mMapInfoIndex].mapData.slotNum != MapInfo.SLOT_MIN
                            || mMapInfo[mMapInfoIndex].mapData.mapNum != MapInfo.MAP_DATA_MAP_MIN
                            || mMapInfo[mMapInfoIndex].mapData.indexNum != MapInfo.MAP_DATA_INDEX_MIN)
                    {
                        connError = true;
                    }
                }

                if (connError)
                {
                    Log.d("MapCollecting", "데이터 수집 중 통신 에러 발생 후 연결된 상태!");
                    Toast.makeText(requireContext(), "데이터 수집 중 통신 에러가 발생하여, 재연결 후 이어서 데이터를 수집합니다.", Toast.LENGTH_LONG).show();
                }
                else
                {
                    setCollectPercent(true, 0);
                }

                MainActivity activity = (MainActivity) requireActivity();

                if (mCollectDataType == COLLECT_DATA_TYPE_ID_AND_USER)
                {
                    mMapInfo[mMapInfoIndex].idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;
                    activity.sendPacket(
                            activity.packetMaker(
                                    PacketInfo.HEADER_READ_ISD_ID_AND_USER,
                                    new byte[]{(byte) mMapInfo[mMapInfoIndex].idUser.slotNum},
                                    2
                            )
                    );
                }
                else if (mCollectDataType == COLLECT_DATA_TYPE_MAP_DATA)
                {
                    mMapInfo[mMapInfoIndex].mapData.indexNum = MapInfo.MAP_DATA_INDEX_MIN;
                    activity.sendPacket(
                            activity.packetMaker(
                                    PacketInfo.HEADER_READ_MAP_DATA,
                                    new byte[]{(byte) mMapInfo[mMapInfoIndex].mapData.slotNum, (byte) mMapInfo[mMapInfoIndex].mapData.indexNum},
                                    3
                            )
                    );
                }
            });
        } // 끝, mFsm == FSM_COLLECT_MAP_SCREEN
        else if (mFsm == FSM_SHARE_MAP_SCREEN)
        {
            MainActivity activity = (MainActivity) requireActivity();

            new Handler(Looper.getMainLooper()).post(() ->
            {
                if (mShareFsm != SHARE_FSM_CONNECTING)
                {
                    mShareDataType = SHARE_DATA_TYPE_READ_ID_AND_USER;

                    Log.d("MapSharing", "데이터 공유 중 통신 에러 발생 후 다시 연결된 상태!");
                    Toast.makeText(requireContext(), "데이터 공유 중 통신 에러가 발생하여, 재연결 후 처음부터 다시 데이터를 공유합니다.", Toast.LENGTH_LONG).show();
                }

                setSharePercent(true, 0);

                mShareFsm = SHARE_FSM_USER_CHECK;

                activity.sendPacket(
                        activity.packetMaker(
                                PacketInfo.HEADER_READ_ISD_ID_AND_USER,
                                new byte[]{(byte) 1},
                                2
                        )
                );
            });
        }
    }

    public void packetProcessIdUser(byte[] packet)
    {
        MainActivity activity = (MainActivity) requireActivity();

        int slotNum = mMapInfo[mMapInfoIndex].idUser.slotNum;
        int indexNum = mMapInfo[mMapInfoIndex].idUser.indexNum;

        Log.d("MapCollecting", "IdUser 패킷 : " + activity.printLogBytesToString(packet));

        if (indexNum != packet[1])
        {
            Log.d("MapCollecting", "IdUSer 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);
            Log.d(TAG, "IdUSer 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);
            return;
        }

        byte[] data = mMapInfo[mMapInfoIndex].idUser.data[slotNum - 1][indexNum - 1]; // MIN 값이 1부터인데, 인덱스는 0부터니까 1씩 빼고 계산한다.

        System.arraycopy(packet, 0, data, 0, packet.length); // src, src_idx, dst, dst_idx, length

        if (packet[1] == MapInfo.ID_USER_INDEX_MAX)
        {
            mMapInfo[mMapInfoIndex].idUser.slotNum = slotNum + 1;
            mMapInfo[mMapInfoIndex].idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;

            if (MapInfo.SLOT_MAX < mMapInfo[mMapInfoIndex].idUser.slotNum)
            {
                Log.d(TAG, "IdUser 데이터 수집 완료!");

                mCollectDataType = COLLECT_DATA_TYPE_MAP_DATA;

                // MapData 수집을 시작해야 한다.
                activity.sendPacket(
                        activity.packetMaker(
                                PacketInfo.HEADER_READ_MAP_DATA,
                                new byte[]{(byte) mMapInfo[mMapInfoIndex].mapData.slotNum, (byte) mMapInfo[mMapInfoIndex].mapData.mapNum},
                                3
                        )
                );
            }
            else
            {
                activity.sendPacket(
                        activity.packetMaker(
                                PacketInfo.HEADER_READ_ISD_ID_AND_USER,
                                new byte[]{(byte) mMapInfo[mMapInfoIndex].idUser.slotNum},
                                2
                        )
                );
            }
        }
        else
        {
            mMapInfo[mMapInfoIndex].idUser.indexNum = indexNum + 1;
        }

        new Handler(Looper.getMainLooper()).post(() ->
        {
            int progress = ((slotNum - MapInfo.SLOT_MIN) * MapInfo.ID_USER_INDEX_MAX) + indexNum;
            int percent = (int) ((((double) progress) / 252) * 100); // 252 : IdUser -> 4 * 3, MapData -> 4 * 4 * 15

            setCollectPercent(true, percent);
        });
    }

    public void packetProcessMapData(byte[] packet)
    {
        MainActivity activity = (MainActivity) requireActivity();

        int slotNum = mMapInfo[mMapInfoIndex].mapData.slotNum;
        int mapNum = mMapInfo[mMapInfoIndex].mapData.mapNum;
        int indexNum = mMapInfo[mMapInfoIndex].mapData.indexNum;

        Log.d("MapCollecting", "MapData 패킷 : " + activity.printLogBytesToString(packet));

        if (indexNum != packet[1])
        {
            Log.d("MapCollecting", "MapData 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 맵 = " + mapNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);
            Log.d(TAG, "MapData 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 맵 = " + mapNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);
            return;
        }

        byte[] data = mMapInfo[mMapInfoIndex].mapData.data[slotNum - 1][mapNum - 1][indexNum - 1];

        System.arraycopy(packet, 0, data, 0, packet.length); // src, src_idx, dst, dst_idx, length

        if (packet[1] == MapInfo.MAP_DATA_INDEX_MAX)
        {
            mMapInfo[mMapInfoIndex].mapData.mapNum = mapNum + 1;
            mMapInfo[mMapInfoIndex].mapData.indexNum = MapInfo.MAP_DATA_INDEX_MIN;

            if (MapInfo.MAP_DATA_MAP_MAX < mMapInfo[mMapInfoIndex].mapData.mapNum)
            {
                mMapInfo[mMapInfoIndex].mapData.slotNum = slotNum + 1;
                mMapInfo[mMapInfoIndex].mapData.mapNum = MapInfo.MAP_DATA_MAP_MIN;

                if (MapInfo.SLOT_MAX < mMapInfo[mMapInfoIndex].mapData.slotNum)
                {
                    Log.d(TAG, "MapData 데이터 수집 완료!");

                    mCollectFsm = COLLECT_FSM_COLLECTED;

                    mMapInfo[mMapInfoIndex].metadata.isFilled = true;
                    mMapInfo[mMapInfoIndex].updateMetadata();

                    for (int i = 0; i < mCollectMapAdapter.getItemCount(); i++)
                    {
                        if (mCollectMapAdapter.isSelected(i))
                        {
                            mCollectMapAdapter.setCollect(i);
                            setCollectInfoText(i);

                            if (mCollectMapAdapter.getCollectedCount() != mCollectMapAdapter.getItemCount())
                            {
                                setOkButtonVisibility(false);

                                if (Status.instance().scanState == Status.SCAN_STATE_STOPPED)
                                {
                                    activity.scanLeWithDelay(true, 100);
                                }
                            }
                            else
                            {
                                setOkButtonVisibility(true);
                                setOkButtonText("다음 절차 진행");
                                setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);

                                Log.d("MapCollecting", "\r\n\r\n\n수신한 모든 맵 데이터 확인");
                                Log.d("MapCollecting", "mMapInfo[] 사이즈 = " + mMapInfo.length);
                                for (int k = 0; k < mMapInfo.length; k++)
                                {
                                    Log.d("MapCollecting", "mMapInfo[" + k + "].idUser.data[0][0] => " + activity.printLogBytesToString(mMapInfo[k].idUser.data[0][0]));
                                    Log.d("MapCollecting", "mMapInfo[" + k + "].idUser.data[0][1] => " + activity.printLogBytesToString(mMapInfo[k].idUser.data[0][1]));
                                }

                                for (int k = 0; k < mMapInfo.length; k++)
                                {
                                    if (mMapInfo[k].metadata.isFilled)
                                    {
                                        for (int n = 0; n < 4; n++)
                                        {
                                            Log.d("MapCollecting", "mMapInfo[" + k + "]의 슬롯 " + n
                                                    + " : name = " + mMapInfo[k].metadata.names[n]
                                                    + ", ear = " + mMapInfo[k].metadata.ears[n]
                                                    + ", version = " + mMapInfo[k].metadata.stamps[n]);
                                        }
                                    }
                                }

                                makeMostRecentMapInfo();

                                // 기존에 데이터베이스 저장되어 있던 맵 정보 삭제
                                List<EntityMap> entityMapList = UtilMap.instance.getAll();

                                if (entityMapList != null)
                                {
                                    for (EntityMap map : entityMapList)
                                    {
                                        UtilMap.instance.delete(map);
                                    }
                                }

                                // 새로 데이터베이스에 저장할 맵 정보 준비
                                EntityMap[] entityMaps = new EntityMap[4];

                                String[] stringMapDataSlots = new String[4];

                                for (int slot_i = 0; slot_i < 4; slot_i++)
                                {
                                    Log.d("MapCollecting", "최신 맵 정보의 슬롯 " + slot_i
                                            + " : name = " + mMostRecentMapInfo.metadata.names[slot_i]
                                            + ", ear = " + mMostRecentMapInfo.metadata.ears[slot_i]
                                            + ", version = " + mMostRecentMapInfo.metadata.stamps[slot_i]);

                                    stringMapDataSlots[slot_i] = MapInfo.getStringFromMapInfo(mMostRecentMapInfo, slot_i);

                                    entityMaps[slot_i] = new EntityMap();
                                    entityMaps[slot_i].name_ear = EntityMap.makePrimaryKey(mMostRecentMapInfo.metadata.names[slot_i], mMostRecentMapInfo.metadata.ears[slot_i]);
                                    entityMaps[slot_i].name = mMostRecentMapInfo.metadata.names[slot_i];
                                    entityMaps[slot_i].ear = mMostRecentMapInfo.metadata.ears[slot_i];
                                    entityMaps[slot_i].stamp = mMostRecentMapInfo.metadata.stamps[slot_i];
                                    entityMaps[slot_i].serialize_map_data = stringMapDataSlots[slot_i];

                                    if (!entityMaps[slot_i].name.equals("TD_OTE"))
                                    {
                                        UtilMap.instance.insert(entityMaps[slot_i]);
                                    }
                                }

                                Log.d("MapCollecting", "* * * * * *");
                                Log.d("MapCollecting", "문자열로 직렬화한 맵 데이터 (최신 맵) :");
                                Log.d("MapCollecting", stringMapDataSlots[0]);
                                Log.d("MapCollecting", stringMapDataSlots[1]);
                                Log.d("MapCollecting", stringMapDataSlots[2]);
                                Log.d("MapCollecting", stringMapDataSlots[3]);
                                Log.d("MapCollecting", "* * * * * *");

                                MapInfo tempMapInfo = new MapInfo();

                                if (MapInfo.setMapInfoFromString(stringMapDataSlots[0], tempMapInfo, 0)
                                        && MapInfo.setMapInfoFromString(stringMapDataSlots[1], tempMapInfo, 1)
                                        && MapInfo.setMapInfoFromString(stringMapDataSlots[2], tempMapInfo, 2)
                                        && MapInfo.setMapInfoFromString(stringMapDataSlots[3], tempMapInfo, 3))
                                {
                                    String cmpMapDataSlot0 = MapInfo.getStringFromMapInfo(mMostRecentMapInfo, 0);
                                    String cmpMapDataSlot1 = MapInfo.getStringFromMapInfo(mMostRecentMapInfo, 1);
                                    String cmpMapDataSlot2 = MapInfo.getStringFromMapInfo(mMostRecentMapInfo, 2);
                                    String cmpMapDataSlot3 = MapInfo.getStringFromMapInfo(mMostRecentMapInfo, 3);

                                    Log.d("MapCollecting", "* * * * * *");
                                    Log.d("MapCollecting", "문자열로 직렬화한 맵 데이터 (테스트 맵) :");
                                    Log.d("MapCollecting", cmpMapDataSlot0);
                                    Log.d("MapCollecting", cmpMapDataSlot1);
                                    Log.d("MapCollecting", cmpMapDataSlot2);
                                    Log.d("MapCollecting", cmpMapDataSlot3);
                                    Log.d("MapCollecting", "* * * * * *");

                                    boolean cmpResult0 = stringMapDataSlots[0].equals(cmpMapDataSlot0);
                                    boolean cmpResult1 = stringMapDataSlots[1].equals(cmpMapDataSlot1);
                                    boolean cmpResult2 = stringMapDataSlots[2].equals(cmpMapDataSlot2);
                                    boolean cmpResult3 = stringMapDataSlots[3].equals(cmpMapDataSlot3);

                                    Log.d("MapCollecting", "넣고 뺀 결과 문자열 비교 : cmpResult0 = "
                                            + cmpResult0 + ", cmpResult1 = " + cmpResult1 + ", cmpResult2 = " + cmpResult2 + ", cmpResult3 = " + cmpResult3);
                                }
                            } // end else (모든 데이터 수집 완료)
                        } // end if
                    }// end for
                }
                else
                {
                    // 슬롯 번호 값을 증가하고 전송 시작
                    activity.sendPacket(
                            activity.packetMaker(
                                    PacketInfo.HEADER_READ_MAP_DATA,
                                    new byte[]{(byte) mMapInfo[mMapInfoIndex].mapData.slotNum, (byte) mMapInfo[mMapInfoIndex].mapData.mapNum},
                                    3
                            )
                    );
                }
            }
            else
            {
                // 맵 번호 값을 올리고 전송 시작
                activity.sendPacket(
                        activity.packetMaker(
                                PacketInfo.HEADER_READ_MAP_DATA,
                                new byte[]{(byte) mMapInfo[mMapInfoIndex].mapData.slotNum, (byte) mMapInfo[mMapInfoIndex].mapData.mapNum},
                                3
                        )
                );
            }
        }
        else
        {
            mMapInfo[mMapInfoIndex].mapData.indexNum = indexNum + 1;
        }

        new Handler(Looper.getMainLooper()).post(() ->
        {
            int top = ((slotNum - MapInfo.SLOT_MIN) * MapInfo.MAP_DATA_MAP_MAX) * MapInfo.MAP_DATA_INDEX_MAX;
            int middle = (mapNum - MapInfo.MAP_DATA_INDEX_MIN) * MapInfo.MAP_DATA_INDEX_MAX;
            int low = indexNum;
            int progress = top + middle + low + 12; // 12 -> IdUser total size
            int percent = (int) ((((double) progress) / 252) * 100);

            setCollectPercent(true, percent);
        });
    }

    //
    // 맵 데이터 공유를 위한 패킷을 어떻게 전송할지 판단하고, 해당 패킷을 전송하는 함수
    //
    public void packetCheckIdUser(byte[] packet)
    {
        MainActivity activity = (MainActivity) requireActivity();

        int slotNum = mShareMapInfo.idUser.slotNum;
        int indexNum = mShareMapInfo.idUser.indexNum;

        Log.d("MapSharing", "IdUser 패킷 : " + activity.printLogBytesToString(packet));

        if (indexNum != packet[1])
        {
            Log.d("MapSharing", "IdUser 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);
            Log.d(TAG, "IdUSer 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);
            return;
        }

        byte[] data = mShareMapInfo.idUser.data[slotNum - 1][indexNum - 1]; // Min 값이 1부터라서, 1을 빼고 인덱스 0부터 시작하게 한다.

        System.arraycopy(packet, 0, data, 0, packet.length);

        if (packet[1] == MapInfo.ID_USER_INDEX_MAX)
        {
            mShareMapInfo.metadata.isFilled = true;
            mShareMapInfo.updateMetadata();

            String oteUser = mShareMapInfo.metadata.names[0];
            String oteEar = mShareMapInfo.metadata.ears[0];
            int mainUserIndex = -1;

            // OTE 메인 사용자 정보 인덱스 찾기
            for (int i = 0; i < 4; i++)
            {
                if (mMostRecentMapInfo.metadata.names[i].equals(oteUser)
                        && mMostRecentMapInfo.metadata.ears[i].equals(oteEar))
                {
                    mainUserIndex = i;
                }
            }

            if (mainUserIndex < 0)
            {
                Log.d("MapSharing", "최신 맵 정보에서 사용자 " + oteUser + ", 수술 부위 " + oteEar + "를 찾을 수 없습니다.");
            }
            else
            {
                switch (mainUserIndex)
                {
                    case 0:
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 0, 0);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 1, 1);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 2, 2);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 3, 3);
                        break;

                    case 1:
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 1, 0);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 0, 1);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 2, 2);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 3, 3);
                        break;

                    case 2:
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 2, 0);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 0, 1);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 1, 2);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 3, 3);
                        break;

                    case 3:
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 3, 0);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 0, 1);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 1, 2);
                        mapInfoSlotCopy(mMostRecentMapInfo, mShareMapInfo, 2, 3);
                        break;
                }

                mShareMapInfo.metadata.isFilled = true;
                mShareMapInfo.updateMetadata();

                Log.d("MapSharing", "공유할 데이터 생성 완료!");

                for (int print_i = 0; print_i < 4; print_i++)
                {
                    Log.d("MapSharing", "새로 생성한 공유 맵 정보의 슬롯 " + print_i
                            + " : name = " + mShareMapInfo.metadata.names[print_i]
                            + ", ear = " + mShareMapInfo.metadata.ears[print_i]
                            + ", version = " + mShareMapInfo.metadata.stamps[print_i]);
                }

                mShareDataType = SHARE_DATA_TYPE_WRITE_ID_AND_USER;

                mShareMapInfo.prepareWriting();

                // IdUser 정보부터 쓰기 시작해야 한다.
                activity.sendPacket(mShareMapInfo.idUser.writing[0][0]);
            }
        }
        else
        {
            mShareMapInfo.idUser.indexNum = indexNum + 1;
        }

        new Handler(Looper.getMainLooper()).post(() ->
        {
            int progress = ((slotNum - MapInfo.SLOT_MIN) * MapInfo.ID_USER_INDEX_MAX) + indexNum;
            // 264 : IdUser read -> 4 * 3, IdUser write -> 4 *3, MapData write -> 4 * 4 * 15
            int percent = (int) ((((double) progress) / 264) * 100);

            setSharePercent(true, percent);
        });
    }

    public void packetWrittenIdUser(byte[] packet)
    {
        MainActivity activity = (MainActivity) requireActivity();

        boolean isDone = false;
        int slotNum = mShareMapInfo.idUser.slotNum;
        int indexNum = mShareMapInfo.idUser.indexNum;

        Log.d("MapSharing", "IdUser 패킷 : " + activity.printLogBytesToString(packet));

        if (indexNum != packet[1])
        {
            Log.d("MapSharing", "IdUser 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);
            return;
        }

        mShareMapInfo.idUser.indexNum = indexNum + 1;

        if (MapInfo.ID_USER_INDEX_MAX < mShareMapInfo.idUser.indexNum)
        {
            mShareMapInfo.idUser.slotNum = slotNum + 1;
            mShareMapInfo.idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;

            if (MapInfo.SLOT_MAX < mShareMapInfo.idUser.slotNum)
            {
                Log.d("MapSharing", "IdUser 데이터 공유 완료!");
                isDone = true;
            }
        }

        if (isDone)
        {
            // MapData 공유 시작
            mShareDataType = SHARE_DATA_TYPE_WRITE_MAP_DATA;

            activity.sendPacket(mShareMapInfo.mapData.writing
                    [mShareMapInfo.mapData.slotNum - 1][mShareMapInfo.mapData.mapNum - 1][mShareMapInfo.mapData.indexNum - 1]);
        }
        else
        {
            // 다음 데이터 전송
            activity.sendPacket(mShareMapInfo.idUser.writing[mShareMapInfo.idUser.slotNum - 1][mShareMapInfo.idUser.indexNum - 1]);
        }

        new Handler(Looper.getMainLooper()).post(() ->
        {
            int progress = ((slotNum - MapInfo.SLOT_MIN) * MapInfo.ID_USER_INDEX_MAX) + indexNum + 12;
            // 264 : IdUser read -> 4 * 3, IdUser write -> 4 *3, MapData write -> 4 * 4 * 15
            int percent = (int) ((((double) progress) / 264) * 100);

            setSharePercent(true, percent);
        });
    }

    public void packetWrittenMapData(byte[] packet)
    {
        MainActivity activity = (MainActivity) requireActivity();

        boolean isDone = false;
        int slotNum = mShareMapInfo.mapData.slotNum;
        int mapNum = mShareMapInfo.mapData.mapNum;
        int indexNum = mShareMapInfo.mapData.indexNum;

        Log.d("MapSharing", "MapData 패킷 : " + activity.printLogBytesToString(packet));

        if (indexNum != packet[1])
        {
            Log.d("MapSharing", "MapData 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 맵 = " + mapNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);
            return;
        }

        mShareMapInfo.mapData.indexNum = indexNum + 1;

        if (MapInfo.MAP_DATA_INDEX_MAX < mShareMapInfo.mapData.indexNum)
        {
            mShareMapInfo.mapData.mapNum = mapNum + 1;
            mShareMapInfo.mapData.indexNum = MapInfo.MAP_DATA_INDEX_MIN;

            if (MapInfo.MAP_DATA_MAP_MAX < mShareMapInfo.mapData.mapNum)
            {
                mShareMapInfo.mapData.slotNum = slotNum + 1;
                mShareMapInfo.mapData.mapNum = MapInfo.MAP_DATA_MAP_MIN;

                if (MapInfo.SLOT_MAX < mShareMapInfo.mapData.slotNum)
                {
                    Log.d("MapSharing", "MapData 공유 완료!");

                    mShareFsm = SHARE_FSM_SHARED;

                    isDone = true;

                    for (int i = 0; i < mShareMapAdapter.getItemCount(); i++)
                    {
                        if (mShareMapAdapter.isSelected(i))
                        {
                            mShareMapAdapter.setShare(i, true);
                            setShareInfoText(i);

                            Status.instance().connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                            ((MainActivity) requireActivity()).mBluetoothGatt.disconnect();
                            break;
                        }
                    }
                }
            }
        }

        if (!isDone)
        {
            activity.sendPacket(mShareMapInfo.mapData.writing
                    [mShareMapInfo.mapData.slotNum - 1][mShareMapInfo.mapData.mapNum - 1][mShareMapInfo.mapData.indexNum - 1]);
        }

        new Handler(Looper.getMainLooper()).post(() ->
        {
            int top = ((slotNum - MapInfo.SLOT_MIN) * MapInfo.MAP_DATA_MAP_MAX) * MapInfo.MAP_DATA_INDEX_MAX;
            int middle = (mapNum - MapInfo.MAP_DATA_INDEX_MIN) * MapInfo.MAP_DATA_INDEX_MAX;
            int low = indexNum;
            // 264 : IdUser read -> 4 * 3, IdUser write -> 4 *3, MapData write -> 4 * 4 * 15
            int progress = top + middle + low + 12 + 12; // 24 -> IdUser r/w total size
            int percent = (int) ((((double) progress) / 264) * 100);

            setSharePercent(true, percent);
        });
    }

    //
    // 맵 데이터 초기화를 위한 패킷에 대한 함수
    //
    public void packetResetMapData(byte[] packet)
    {
        Status status = Status.instance();
        MainActivity activity = (MainActivity) requireActivity();

        if (packet.length != 2)
        {
            Log.d(TAG, "맵 초기화에 대한 응답 패킷의 길이가 이상합니다!");

            if (status.connectionState == Status.CONNECTION_STATE_CONNECTED)
            {
                activity.mBluetoothGatt.disconnect();
            }

            return;
        }

        if (packet[0] != PacketInfo.HEADER_MAP_RESET_DEFAULT || packet[1] != PacketInfo.MAP_RESET_DEFAULT_OK)
        {
            Log.d(TAG, "맵 초기화에 대한 응답 패킷 정보가 이상합니다. Header = " + packet[0] + ", Data = " + packet[1]);

            if (status.connectionState == Status.CONNECTION_STATE_CONNECTED)
            {
                activity.mBluetoothGatt.disconnect();
            }

            return;
        }

        Log.d(TAG, "맵 초기화 완료!");
        mResetFsm = RESET_FSM_DONE;

        for (int i = 0; i < mMapResetAdapter.getItemCount(); i++)
        {
            if (mMapResetAdapter.isSelected(i))
            {
                int position = i;

                new Handler(Looper.getMainLooper()).post(() ->
                {
                    mMapResetAdapter.setResetDone(position, true);
                    setMapResetInfoText(position);

                    status.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                    activity.mBluetoothGatt.disconnect();

                    if (mMapResetAdapter.getItemCount() == mMapResetAdapter.getResetDoneCount())
                    {
                        activity.scanLe(true);
                    }
                });

                break;
            }
        }
    }

    //
    // 수집된 매핑 데이터들에서 가장 최신 버전의 매핑 데이터만 남겨놓는 함수
    //
    public void makeMostRecentMapInfo()
    {
        //
        // mMapInfo 가 null이 아니고 데이터가 있다는 전재로 구현한다.
        //

        if (mMostRecentMapInfo == null)
        {
            mMostRecentMapInfo = new MapInfo();
        }

        mapInfoCopyAll(mMapInfo[0], mMostRecentMapInfo);

        if (1 < mMapInfo.length) // 저장된 맵 데이터가 1개 보다 많을 때
        {
            for (int len = 1; len < mMapInfo.length; len++) // 저장된 맵들 모두 비교할 것이다.
            {
                for (int slot_i = 0; slot_i < 4; slot_i++) // 슬롯 4개를 비교하기 위함.
                {
                    boolean isExist = false; // 가장 최신 맵 정보에 현재 맵 정보의 슬롯 정보가 이미 존재하는지 검사하기 위함.

                    for (int find_i = 0; find_i < 4; find_i++) // 현 맵 정보의 각 슬롯이 가장 최신 맵 정보의 슬롯 4개중에 일치하는게 있는지 찾기 위함
                    {
                        if (mMapInfo[len].metadata.names[slot_i].equals(mMostRecentMapInfo.metadata.names[find_i])
                                && mMapInfo[len].metadata.ears[slot_i].equals(mMostRecentMapInfo.metadata.ears[find_i]))
                        {
                            isExist = true;

                            // 가장 최신 맵 정보보다 더 최신인 데이터라면 복사한다.
                            if (mMostRecentMapInfo.metadata.stamps[find_i] < mMapInfo[len].metadata.stamps[slot_i])
                            {
                                mapInfoSlotCopy(mMapInfo[len], mMostRecentMapInfo, slot_i, find_i);
                            }

                            break;
                        }
                    }

                    // 검사 해봤는데, 가장 최신 맵 정보에 등록되어 있지 않은 정보라면, 새로 넣어준다.
                    if (!isExist)
                    {
                        for (int empty_i = 0; empty_i < 4; empty_i++)
                        {
                            // 가장 최신 맵 정보에서 TD_OTE 인 곳에다가 현재 맵 정보의 슬롯을 넣어준다.
                            if (mMostRecentMapInfo.metadata.names[empty_i].equals("TD_OTE"))
                            {
                                mapInfoSlotCopy(mMapInfo[len], mMostRecentMapInfo, slot_i, empty_i);
                                break;
                            }
                        }
                    }
                } // 슬롯 i
            } // 맵 정보 전체 len
        } // end 1 < mMapInfo.length
    } // end 함수

    public void mapInfoCopyAll(MapInfo src, MapInfo dst)
    {
        if (src == null || dst == null)
        {
            return;
        }

        dst.dataType = src.dataType;

        dst.metadata.isFilled = src.metadata.isFilled;

        dst.idUser.slotNum = src.idUser.slotNum;
        dst.idUser.indexNum = src.idUser.indexNum;

        dst.mapData.slotNum = src.mapData.slotNum;
        dst.mapData.mapNum = src.mapData.mapNum;
        dst.mapData.indexNum = src.mapData.indexNum;

        for (int i = 0; i < 4; i++) // 슬롯 4개
        {
            dst.metadata.names[i] = new String(src.metadata.names[i].getBytes());
            dst.metadata.ears[i] = new String(src.metadata.ears[i].getBytes());
            dst.metadata.stamps[i] = src.metadata.stamps[i];

            for (int j = 0; j < 3; j++) // IdUser의 인덱스 3개
            {
                dst.idUser.data[i][j] = Arrays.copyOf(src.idUser.data[i][j], src.idUser.data[i][j].length);
            }

            for (int k = 0; k < 4; k++) // MapData의 Map 4개
            {
                for (int n = 0; n < 15; n++) // MapData의 각 Map의 인덱스 15개
                {
                    dst.mapData.data[i][k][n] = Arrays.copyOf(src.mapData.data[i][k][n], src.mapData.data[i][k][n].length);
                }
            }
        }
    }

    public void mapInfoSlotCopy(MapInfo src, MapInfo dst, int srcSlotNum, int dstSlotNum)
    {
        if (src == null || dst == null)
        {
            return;
        }

        dst.metadata.names[dstSlotNum] = new String(src.metadata.names[srcSlotNum].getBytes());
        dst.metadata.ears[dstSlotNum] = new String(src.metadata.ears[srcSlotNum].getBytes());
        dst.metadata.stamps[dstSlotNum] = src.metadata.stamps[srcSlotNum];

        for (int j = 0; j < 3; j++) // IdUser의 인덱스 3개
        {
            dst.idUser.data[dstSlotNum][j] = Arrays.copyOf(src.idUser.data[srcSlotNum][j], src.idUser.data[srcSlotNum][j].length);
        }

        for (int k = 0; k < 4; k++) // MapData의 Map 4개
        {
            for (int n = 0; n < 15; n++) // MapData의 각 Map의 인덱스 15개
            {
                dst.mapData.data[dstSlotNum][k][n] = Arrays.copyOf(src.mapData.data[srcSlotNum][k][n], src.mapData.data[srcSlotNum][k][n].length);
            }
        }
    }
}