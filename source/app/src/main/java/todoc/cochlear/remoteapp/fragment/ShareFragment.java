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
import todoc.cochlear.remoteapp.activity.databinding.FragmentShareBinding;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.maps.EntityMap;
import todoc.cochlear.remoteapp.database.maps.UtilMap;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.list.ShareCollectMapAdapter;
import todoc.cochlear.remoteapp.list.ShareShareMapAdapter;
import todoc.cochlear.remoteapp.list.ShareExistMapAdapter;
import todoc.cochlear.remoteapp.list.ShareMapResetDefaultAdapter;
import todoc.cochlear.remoteapp.list.ShareSelectUserAdapter;
import todoc.cochlear.remoteapp.params.MapInfo;
import todoc.cochlear.remoteapp.params.PacketInfo;
import todoc.cochlear.remoteapp.params.Status;

public class ShareFragment extends Fragment
{
    static private final String TAG = "TODOC_" + ShareFragment.class.getSimpleName();

    static private final int HANDLER_TIMEOUT_IN_MS = 5000;
    static private final int HANDLER_START_DELAY_FOR_COLLECT_MAP_IN_MS = 50;
    static private final int HANDLER_START_DELAY_FOR_SHARE_MAP_IN_MS = 50;
    static private final int HANDLER_START_DELAY_FOR_RESET_MAP_IN_MS = 50;

    static private final int BUTTON_COLOR_NORMAL = 0xFFB2B2B2; // White 70
    static private final int BUTTON_COLOR_ACCENT = 0xFF2D968D; // Accent 70
    static private final int BUTTON_COLOR_ERROR = 0xFFB84068;  // Error 70

    static private final int AT_LEAST_SELECTED_USER_COUNT = 2;

    static public final int MAX_COUNT_CONNECTION_FAIL = 4;

    static public final int FSM_FIRST_SCREEN = 0;
    static public final int FSM_SELECT_USER_SCREEN = 1;
    static public final int FSM_COLLECT_MAP_SCREEN = 2;
    static public final int FSM_SHARE_MAP_SCREEN = 3;
    static public final int FSM_EXIST_MAP_SCREEN = 4;
    static public final int FSM_MAP_RESET_DEFAULT_SCREEN = 5;

    static public final int COLLECT_FSM_IDLE = 10;
    static public final int COLLECT_FSM_CONNECTING = 11;
    static public final int COLLECT_FSM_COLLECTING = 12;
    static public final int COLLECT_FSM_COLLECTED = 13;
    static public final int COLLECT_FSM_DISCONNECTING = 14;

    static public final int SHARE_FSM_IDLE = 20;
    static public final int SHARE_FSM_CONNECTING = 21;
    static public final int SHARE_FSM_USER_CHECK = 22;
    static public final int SHARE_FSM_SHARING = 23;
    static public final int SHARE_FSM_SHARED = 24;
    static public final int SHARE_FSM_DISCONNECTING = 25;

    static public final int RESET_FSM_IDLE = 30;
    static public final int RESET_FSM_CONNECTING = 31;
    static public final int RESET_FSM_RESETTING = 32;
    static public final int RESET_FSM_DONE = 33;
    static public final int RESET_FSM_DISCONNECTING = 34;

    static public final int COLLECT_DATA_TYPE_ID_AND_USER = 5000;
    static public final int COLLECT_DATA_TYPE_MAP_DATA = 5001;

    static public final int SHARE_DATA_TYPE_READ_ID_AND_USER = 6000;
    static public final int SHARE_DATA_TYPE_WRITE_ID_AND_USER = 6001;
    static public final int SHARE_DATA_TYPE_WRITE_MAP_DATA = 6002;

    private MainActivity mActivity;
    private Status mStatus;
    private FragmentShareBinding mShareBinding;

    public int mFsm = FSM_FIRST_SCREEN;
    public int mCollectFsm = COLLECT_FSM_IDLE;
    public int mShareFsm = SHARE_FSM_IDLE;
    public int mResetFsm = RESET_FSM_IDLE;
    public int mCollectDataType = COLLECT_DATA_TYPE_ID_AND_USER;
    public int mShareDataType = SHARE_DATA_TYPE_READ_ID_AND_USER;

    public ShareExistMapAdapter mExistMapAdapter;
    public ShareSelectUserAdapter mSelectUserAdapter;
    public ShareCollectMapAdapter mCollectMapAdapter;
    public ShareShareMapAdapter mShareMapAdapter;
    public ShareMapResetDefaultAdapter mMapResetAdapter;

    public BluetoothDevice mBtDevice;

    private MapInfo mMostRecentMapInfo;
    private MapInfo mShareMapInfo;

    private MapInfo[] mCollectedMapInfo;
    public int mCollectedMapInfoCurrentIndex = 0;

    public int mSelectedUserCount = 0;
    public int mCountForConnectionFail = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mActivity = (MainActivity) requireActivity();
        mStatus = Status.instance();

        mActivity.mBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        mActivity.mBinding.toolbarNavigationMessage.setText("메뉴");
        mActivity.mBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);
        mActivity.mBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        mActivity.mBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        mActivity.mBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        mActivity.mBinding.toolbar.setTitle("외부기 공유");
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

        mShareBinding.shareOkButton.setOnClickListener(mOkButtonClickListener);         // 버튼 클릭 리스너 - OK
        mShareBinding.shareCancelButton.setOnClickListener(mCancelButtonClickListener); // 버튼 클릭 리스너 - CANCEL
        updateScreen();
    }

    //
    // 프래그먼트 종료 함수
    //
    public void exitFragment()
    {
        // 동작 중인 핸들러를 제거한다.
        mDataIndexHandler.removeCallbacks(mDataIndexRunner);

        // 연결 중인 장치가 있다면, 연결을 해제시킨다.
        if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
        {
            if (mActivity.mBluetoothGatt != null)
            {
                mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                mActivity.mBluetoothGatt.disconnect();
            }
        }

        mActivity.scanLeWithDelay(false, 0);
    }

    //
    // 데이터인덱스 핸들러
    //
    public Handler mDataIndexHandler = new Handler();

    public Runnable mDataIndexRunner = () ->
    {
        Log.d(TAG, "데이터 인덱스 타임아웃 발생!");

        if (mActivity.mBluetoothGatt != null)
        {
            Log.d(TAG, "연결 중인 장치를 해제합니다.");

            mActivity.mBluetoothGatt.disconnect();
        }
    };

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
        }

        updateLayoutsWithFsm(); // FSM에 해당하는 레이아웃 표시
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 리사이클러뷰 관련
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //
    // 리사이클러뷰 준비 - 1. 내부기 선택 화면
    //
    public void updateListSelectUser()
    {
        // 생성된 사용자 선택 어댑터가 없다면, 사용자 선택 어댑터 생성
        if (mSelectUserAdapter == null)
        {
            mSelectUserAdapter = new ShareSelectUserAdapter();
            mShareBinding.shareSelectIsdUserRecyclerview.setAdapter(mSelectUserAdapter);
            mShareBinding.shareSelectIsdUserRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        // 리모컨 앱에 등록된 모든 사용자 정보를 읽어온다.
        List<EntityUser> registeredUsers = UtilUser.instance.getUsers();

        // 사용자 선택 어댑터의 내용을 비운다.
        mSelectUserAdapter.clearItems();

        // 사용자 선택 어탭터에 리모컨 앱에 등록된 모든 사용자 정보를 추가한다.
        mSelectUserAdapter.addUsers(registeredUsers);
    }

    //
    // 리사이클러뷰 준비 - 2. 매핑 데이터 수집 화면
    //
    public void updateListCollectMap()
    {
        // 생성된 맵 수집 어댑터가 없다면, 맵 수집 어댑터 생성
        if (mCollectMapAdapter == null)
        {
            mCollectMapAdapter = new ShareCollectMapAdapter(this);
            mShareBinding.shareCollectMapDataRecyclerview.setAdapter(mCollectMapAdapter);
            mShareBinding.shareCollectMapDataRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        // 맵 수집 어댑터의 내용을 비운다.
        mCollectMapAdapter.clearItems();

        // 맵 수집 어댑터에 사용자 선택 어댑터에 등록된 사용자들 중 선택된 사용자 정보만 추가한다.
        if (mSelectUserAdapter != null && 0 < mSelectUserAdapter.getSelectedUserCount())
        {
            mCollectMapAdapter.addUsers(mSelectUserAdapter.getSelectedUsers());
        }
    }

    //
    // 리사이클러뷰 준비 - 3. 매핑 데이터 공유 화면
    //
    public void updateListShareMap()
    {
        // 생성된 맵 공유 어댑터가 없다면, 맵 공유 어댑터 생성
        if (mShareMapAdapter == null)
        {
            mShareMapAdapter = new ShareShareMapAdapter(this);
            mShareBinding.shareDistributeMapDataRecyclerview.setAdapter(mShareMapAdapter);
            mShareBinding.shareDistributeMapDataRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        // 맵 공유 어댑터의 내용을 비운다.
        mShareMapAdapter.clearItems();
    }

    //
    // 리사이클러뷰 준비 - 이미 등록된 맵 정보 화면
    //
    public void updateListExistMap()
    {
        // 이미 등록된 맵 어댑터가 없다면, 이미 등록된 맵 어댑터 생성
        if (mExistMapAdapter == null)
        {
            mExistMapAdapter = new ShareExistMapAdapter();
            mShareBinding.shareExistMapRecyclerview.setAdapter(mExistMapAdapter);
            mShareBinding.shareExistMapRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        // 이미 등록된 맵 어댑터의 내용을 비운다.
        mExistMapAdapter.clearItems();

        // 데이터베이스에서 등록된 맵 정보를 읽어서 이미 등록된 맵 어댑터에 추가한다.
        mExistMapAdapter.addMaps(UtilMap.instance.getAll());
    }

    //
    // 리사이클러뷰 준비 - 최초 매핑 시점 복귀 화면
    //
    public void updateListResetMap()
    {
        // 맵 초기화 어댑터가 없다면, 맵 초기화 어댑터 생성
        if (mMapResetAdapter == null)
        {
            mMapResetAdapter = new ShareMapResetDefaultAdapter(this);
            mShareBinding.shareMapResetDefaultRecyclerview.setAdapter(mMapResetAdapter);
            mShareBinding.shareMapResetDefaultRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
        }

        // 맵 초기화 어댑터의 내용을 비운다.
        mMapResetAdapter.clearItems();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 화면 업데이트 관련
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //
    // 화면 업데이트 - 맨 처음 진행순서 설명화면
    //
    public void updateFsmFirstScreen()
    {
        // OK 버튼 설정
        setOkButtonText("진행");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(true);

        // CANCEL 버튼 설정
        setCancelButtonText("외부기 맵 데이터 초기화");
        setCancelButtonBackgroundColor(BUTTON_COLOR_ERROR);
        setCancelButtonVisibility(true);
    }

    //
    // 화면 업데이트 - 1. 내부기 선택 화면
    //
    public void updateFsmSelectUSerScreen()
    {
        // OK 버튼 설정
        setOkButtonText("다음 절차 진행");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(true);

        // CANCEL 버튼 설정
        setCancelButtonVisibility(false);

        // 리사이클러뷰 설정
        updateListSelectUser();
    }

    //
    // 화면 업데이트 - 2. 매핑 데이터 수집 화면
    //
    public void updateFsmCollectMapScreen()
    {
        // OK 버튼 설정
        setOkButtonText("시작");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(false);

        // CANCEL 버튼 설정
        setCancelButtonVisibility(false);

        // 리사이클러뷰 설정
        updateListCollectMap();

        // 수집 FSM 초기화
        mCollectFsm = COLLECT_FSM_IDLE;

        // BLE 스캔 시작
        mActivity.scanLeWithDelay(true, 10);
    }

    //
    // 화면 업데이트 - 3. 매핑 데이터 공유 화면
    //
    public void updateFsmShareMapScreen()
    {
        // OK 버튼 설정
        setOkButtonText("시작");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(false);

        // CANCEL 버큰 설정
        setCancelButtonVisibility(false);

        // 리사이클러뷰 설정
        updateListShareMap();

        // 공유할 맵 버퍼 생성
        mShareMapInfo = new MapInfo();

        // 공유 FSM 초기화
        mShareFsm = SHARE_FSM_IDLE;

        // BLE 스캔 시작
        mActivity.scanLeWithDelay(true, 10);
    }

    //
    // 화면 업데이트 - 이미 등록된 매핑 데이터 존재
    //
    public void updateFsmExistMapScreen()
    {
        // OK 버튼 설정
        setOkButtonText("새로운 매핑 데이터 수집 진행");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(true);

        // CANCEL 버튼 설정
        setCancelButtonText("시작");
        setCancelButtonBackgroundColor(BUTTON_COLOR_NORMAL);
        setCancelButtonVisibility(true);

        // 리사이클러뷰 설정
        updateListExistMap();
    }

    //
    // 화면 업데이트 - 최초 매핑 시점 복귀 화면
    //
    public void updateFsmMapResetDefaultScreen()
    {
        // OK 버튼 설정
        setOkButtonText("초기화");
        setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        setOkButtonVisibility(false);

        // CANCEL 버튼 설정
        setCancelButtonVisibility(false);

        // 리사이클러뷰 설정
        updateListResetMap();

        // 초기화 FSM 초기화
        mResetFsm = RESET_FSM_IDLE;

        // 연결된 장치가 있다면 연결해제
        if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
        {
            mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
            mActivity.mBluetoothGatt.disconnect();
        }

        // BLE 스캔 시작
        mActivity.scanLeWithDelay(true, 10);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 버튼 관련
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //
    // 주 확인 버튼의 맨 처음 진행순서 설명화면 클릭 핸들러
    //
    public void okButtonFirstScreen()
    {
        // 데이터베이스 상에 저장된 맵 정보가 있는지 체크하여
        // 데이터 수집을 위한 사용자 선택 화면
        // 또는 이미 등록된 맵 정보 화면 중 하나로 전환되도록 한다.

        List<EntityMap> maps = UtilMap.instance.getAll();

        if (maps == null || maps.size() == 0)
        {
            // 데이터베이스 상에 저장된 맵 정보가 없으므로, 데이터 수집을 위한 사용자 선택 화면 선택
            mFsm = FSM_SELECT_USER_SCREEN;
        }
        else
        {
            // 데이터베이스 상에 저장된 맵 정보가 있으므로, 이미 등록된 맵 정보 화면 선택
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

        // 선택된 사용자(내부기)의 개수를 구한다.
        mSelectedUserCount = mSelectUserAdapter.getSelectedUserCount();

        Log.d(TAG, "선택된 사용자(내부기) 수 : " + mSelectedUserCount);

        // 최소 사용자 선택 수 이상으로 사용자를 선택해야 다음 화면으로 진행할 수 있다.
        if (mSelectedUserCount < AT_LEAST_SELECTED_USER_COUNT)
        {
            String message = "사용자(내부기)를 최소 " + AT_LEAST_SELECTED_USER_COUNT + " 이상 선택해주세요.";
            Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
            return;
        }

        // 연결중인 외부기가 있다면 연결을 해제한다.
        if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
        {
            mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
            mActivity.mBluetoothGatt.disconnect();
        }

        mFsm = FSM_COLLECT_MAP_SCREEN;  // 맵 수집 상태로 변경하고
        updateScreen();                 // 화면을 업데이트 시킨다.
    }

    //
    // 주 확인 버튼의 2. 매핑 데이터 수집 화면 클릭 핸들러
    //
    public void okButtonCollectMapScreen()
    {
        // 맵 데이터 수집이 완료되었는지 체크하고, 수집이 완료되었다면 맵 공유 화면으로 전환한다.
        if (mCollectMapAdapter.getCollectedMapCount() == mCollectMapAdapter.getItemCount())
        {
            Log.d(TAG, "연결된 외부기가 있다면 연결을 해제하고, 맵 공유 화면으로 전환합니다.");

            if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
            {
                mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                mActivity.mBluetoothGatt.disconnect();
            }

            mFsm = FSM_SHARE_MAP_SCREEN;

            updateScreen();
        }
        // 아직 맵 데이터 수집이 완료되지 않았다면, 아래를 진행한다.
        else
        {
            // 맵 수집 어댑터에 추가된 모든 아이템에 대하여 현재 사용자에 의하여 수집되기 위해 선택된 상태인지,
            // 선택된 상태라면 블루투스로 외부기가 검색되었는지를 검사하고
            // 모든 조건에 부합되면 선택한 장치와 연결 후 맵 데이터 수집을 진행한다.
            for (int collectMapAdapterIndex = 0; collectMapAdapterIndex < mCollectMapAdapter.getItemCount(); collectMapAdapterIndex++)
            {
                if (mCollectMapAdapter.isItemSelected(collectMapAdapterIndex) && mCollectMapAdapter.isBleScanned(collectMapAdapterIndex))
                {
                    String oteSerial = mCollectMapAdapter.getOteSerial(collectMapAdapterIndex);

                    if (UtilDevice.instance.getDeviceBySerialNumber(oteSerial) == null)
                    {
                        Toast.makeText(requireContext(), "앱에 등록되지 않은 외부기입니다. 먼저 외부기 등록을 진행해주세요.", Toast.LENGTH_LONG).show();
                        break;
                    }

                    if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
                    {
                        mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                        mActivity.mBluetoothGatt.disconnect();
                    }

                    mActivity.scanLeWithDelay(false, 0);

                    mCollectedMapInfoCurrentIndex = collectMapAdapterIndex;

                    // 맵 수집 버퍼가 수집할 사용자(내부기) 수만큼 생성된 상태가 아니면, 맵 수집 버퍼를 새로 생성한다.
                    if (mCollectedMapInfo == null || mCollectedMapInfo.length != mCollectMapAdapter.getItemCount())
                    {
                        mCollectedMapInfo = new MapInfo[mCollectMapAdapter.getItemCount()];
                    }

                    // 맵 수집 어댑터의 인덱스와 일치하는 사용자(내부기)의 맵 수집 버퍼가 없으면 새로 생성한다.
                    if (mCollectedMapInfo[mCollectedMapInfoCurrentIndex] == null)
                    {
                        mCollectedMapInfo[mCollectedMapInfoCurrentIndex] = new MapInfo();
                    }

                    mStatus.connectedUser = mCollectMapAdapter.getItem(collectMapAdapterIndex);
                    mStatus.connectedDevice = UtilDevice.instance.getDeviceBySerialNumber(oteSerial);
                    mBtDevice = mCollectMapAdapter.getBtDevice(collectMapAdapterIndex);

                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                    {
                        mStatus.connectionState = Status.CONNECTION_STATE_CONNECTING;

                        if (mCollectFsm != COLLECT_FSM_COLLECTING)
                        {
                            mCollectFsm = COLLECT_FSM_CONNECTING;
                            mCollectDataType = COLLECT_DATA_TYPE_ID_AND_USER;

                            mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.slotNum = MapInfo.SLOT_MIN;
                            mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;
                            mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.slotNum = MapInfo.SLOT_MIN;
                            mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.mapNum = MapInfo.MAP_DATA_MAP_MIN;
                            mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.indexNum = MapInfo.MAP_DATA_INDEX_MIN;

                            //mCollectedMapInfo[mCollectedMapInfoCurrentIndex].dataType = MapInfo.DATA_TYPE_ID_USER;
                        }

                        mActivity.mBluetoothDevice = mBtDevice;
                        mActivity.mBluetoothGatt = mBtDevice.connectGatt(mActivity, false, mActivity.mGattCallback);

                        if (mActivity.mBluetoothGatt == null)
                        {
                            Log.d(TAG, "맵 수집 화면에서 BLE 연결 시도가 실패했습니다.");

                            Toast.makeText(requireContext(), "연결을 실패했습니다. 다시 연결을 시도해주세요.", Toast.LENGTH_LONG).show();
                            mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTED;
                            setOkButtonVisibility(true);
                        }
                        else
                        {
                            setOkButtonVisibility(false);
                        }
                    }, HANDLER_START_DELAY_FOR_COLLECT_MAP_IN_MS);
                } // End if (아이템이 선택됐고 BLE 스캔이 완료된 상황)
            } // End for (맵 수집 어댑터의 아이템 개수만큼 체크하는 부분)
        } // End if-else (맵 데이터 수집 완료 여부)
    }

    //
    // 주 확인 버튼의 3. 매핑 데이터 공유 화면 클릭 핸들러
    //
    public void okButtonShareMapScreen()
    {
        if (mShareMapAdapter.getSharedMapCount() == mShareMapAdapter.getItemCount())
        {
            Log.d(TAG, "검색된 모든 외부기로 매핑 데이터 공유를 완료했습니다.");
            return;
        }

        // 맵 공유 어댑터에 추가된 모든 아이템에 대하여 현재 사용자에 의하여 공유되기 위해 선택된 상태인지,
        // 선택된 상태라면 공유가 이미 된 상태인지를 검사하고
        // 모든 조건에 부합되면 선택한 장치와 연결 후 맵 데이터 공유를 진행한다.
        // 맵 공유 어댑터에 추가되는 아이템은 BLE 스캔을 통해 추가되어진다.
        for (int i = 0; i < mShareMapAdapter.getItemCount(); i++)
        {
            if (mShareMapAdapter.isItemSelected(i) && !mShareMapAdapter.isMapShared(i))
            {
                if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
                {
                    mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                    mActivity.mBluetoothGatt.disconnect();
                }

                mActivity.scanLeWithDelay(false, 0);

                mStatus.connectedUser = mShareMapAdapter.getEntityUser(i);
                mStatus.connectedDevice = mShareMapAdapter.getEntityDevice(i);

                mBtDevice = mShareMapAdapter.getBtDevice(i);

                new Handler(Looper.getMainLooper()).postDelayed(() ->
                {
                    mStatus.connectionState = Status.CONNECTION_STATE_CONNECTING;

                    if (mShareFsm != SHARE_FSM_USER_CHECK && mShareFsm != SHARE_FSM_SHARING)
                    {
                        mShareFsm = SHARE_FSM_CONNECTING;

                        mShareDataType = SHARE_DATA_TYPE_READ_ID_AND_USER;

                        mShareMapInfo.idUser.slotNum = MapInfo.SLOT_MIN;
                        mShareMapInfo.idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;
                        //mShareMapInfo.dataType = MapInfo.DATA_TYPE_ID_USER;
                    }

                    mActivity.mBluetoothDevice = mBtDevice;
                    mActivity.mBluetoothGatt = mBtDevice.connectGatt(mActivity, false, mActivity.mGattCallback);

                    if (mActivity.mBluetoothGatt == null)
                    {
                        Log.d(TAG, "맵 공유 화면에서 BLE 연결 시도가 실패했습니다.");

                        Toast.makeText(requireContext(), "연결을 실패했습니다. 다시 연결을 시도해주세요.", Toast.LENGTH_LONG).show();
                        mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTED;
                        setOkButtonVisibility(true);
                    }
                    else
                    {
                        setOkButtonVisibility(false);
                    }
                }, HANDLER_START_DELAY_FOR_SHARE_MAP_IN_MS);

                break;
            } // End if
        } // End for (mShareMapAdatper.getItemCount())
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
            Log.d(TAG, "검색된 모든 외부기가 최초 매핑 시점으로 복귀된 상태입니다.");
            return;
        }

        // 맵 초기화 어댑터에 추가된 모든 아이템에 대하여 현재 사용자에 의하여 초기화 되기 위해 선택된 상태인지,
        // 선택된 상태라면 초기화가 이미 된 상태인지를 검사하고
        // 모든 조건에 부합되면 선택한 장치와 연결 후 맵 데이터 초기화를 진행한다.
        // 맵 데이터 초기화 어댑터에 추가되는 아이템은 BLE 스캔을 통해 추가되어진다.
        for (int i = 0; i < mMapResetAdapter.getItemCount(); i++)
        {
            if (mMapResetAdapter.isItemSelected(i) && !mMapResetAdapter.isMapResetDone(i))
            {
                if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
                {
                    mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                    mActivity.mBluetoothGatt.disconnect();
                }

                mActivity.scanLeWithDelay(false, 0);

                mStatus.connectedUser = mMapResetAdapter.getEntityUser(i);
                mStatus.connectedDevice = mMapResetAdapter.getEntityDevice(i);

                mBtDevice = mMapResetAdapter.getBtDevice(i);

                new Handler(Looper.getMainLooper()).postDelayed(() ->
                {
                    mStatus.connectionState = Status.CONNECTION_STATE_CONNECTING;

                    if (mResetFsm != RESET_FSM_RESETTING)
                    {
                        mResetFsm = RESET_FSM_CONNECTING;
                    }

                    mActivity.mBluetoothDevice = mBtDevice;
                    mActivity.mBluetoothGatt = mBtDevice.connectGatt(mActivity, false, mActivity.mGattCallback);

                    if (mActivity.mBluetoothGatt == null)
                    {
                        Log.d(TAG, "맵 초기화 화면에서 BLE 연결 시도가 실패했습니다.");

                        Toast.makeText(requireContext(), "연결을 실패입니다. 다시 연결을 시도해주세요.", Toast.LENGTH_LONG).show();
                        mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTED;
                        setOkButtonVisibility(true);
                    }
                    else
                    {
                        setOkButtonVisibility(false);
                    }
                }, HANDLER_START_DELAY_FOR_RESET_MAP_IN_MS);

                break;
            } // End if
        } // End for (mMapResetAdapter.getItemCount())
    }

    //
    // OK 버튼 클릭 리스너
    //
    View.OnClickListener mOkButtonClickListener = view ->
    {
        mActivity.longTimeIdleHandlerUpdate(true);

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
    };

    //
    // 주 취소 버튼의 맨 처음 진행순서 설명화면 클릭 핸들러
    //
    public void cancelButtonFirstScreen()
    {
        mFsm = FSM_MAP_RESET_DEFAULT_SCREEN;
        updateScreen();
    }

    //
    // 주 취소 버튼의 이미 등록된 맵 정보 화면 핸들러
    //
    public void cancelButtonExistMapScreen()
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
            mCollectMapAdapter.setMapCollectState(i);
        }

        // 최신 맵 구성하기
        if (mMostRecentMapInfo == null)
        {
            mMostRecentMapInfo = new MapInfo();
        }

        List<EntityMap> maps = UtilMap.instance.getAll();


        for (int slot_init_i = 0; slot_init_i < 4; slot_init_i++)
        {
            MapInfo.setMapInfoFromString(MapInfo.EMPTY_MAP_DATA, mMostRecentMapInfo, slot_init_i);
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
            Log.d(TAG, "mMostRecentMapInfo -> slot " + print_i + " : name = " + mMostRecentMapInfo.metadata.names[print_i] + ", ear = " + mMostRecentMapInfo.metadata.ears[print_i] + ", version = " + mMostRecentMapInfo.metadata.stamps[print_i]);
        }

        if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
        {
            mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
            mActivity.mBluetoothGatt.disconnect();
        }

        mFsm = FSM_SHARE_MAP_SCREEN;
        updateScreen();
    }

    //
    // CANCEL 버튼 클릭 리스너
    //
    View.OnClickListener mCancelButtonClickListener = (view ->
    {
        mActivity.longTimeIdleHandlerUpdate(true);

        switch (mFsm)
        {
            case FSM_FIRST_SCREEN:
                cancelButtonFirstScreen();
                break;

            case FSM_EXIST_MAP_SCREEN:
                cancelButtonExistMapScreen();
                break;
        }
    });

    //
    // 리사이클러뷰 클릭 이벤트 - 2. 매핑 데이터 수집 화면
    //
    public void listClickListenerCollectMap(int position)
    {
        mActivity.longTimeIdleHandlerUpdate(true);

        Log.d(TAG, "맵 수집 어댑터의 " + position + "번 아이템이 클릭되었습니다.");

        // 맵 수집 상태가, 맵 수집 중이거나 외부기에 연결 시도 중인 상태라면 그냥 종료한다.
        if (mCollectFsm == COLLECT_FSM_COLLECTING || mCollectFsm == COLLECT_FSM_CONNECTING)
        {
            Log.d(TAG, "맵 수집 상태가 맵 수집 중 또는 외부기로 연결 시도 중이므로 리스트 클릭 이벤트를 무시합니다.");
            return;
        }

        boolean isMapCollected = mCollectMapAdapter.isMapCollected(position);
        boolean isItemSelected = mCollectMapAdapter.isItemSelected(position);
        boolean newItemSelectedState = !isItemSelected;

        mCollectMapAdapter.setItemSelectedStateForAll(false);
        mCollectMapAdapter.setItemSelectState(position, newItemSelectedState);

        setCollectInfoText(position);
        setCollectInfoVisibility(newItemSelectedState);
        setOkButtonVisibility(newItemSelectedState && !isMapCollected && mCollectMapAdapter.isBleScanned(position));

        if (isMapCollected)
        {
            setCollectPercent(true, 100);
        }
        else
        {
            setCollectPercent(false, 0);
        }

        if (mCollectMapAdapter.getCollectedMapCount() == mCollectMapAdapter.getItemCount())
        {
            setOkButtonVisibility(true);
            setOkButtonText("다음 절차 진행");
            setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);
        }
    }

    //
    // 리사이클러뷰 클릭 이벤트 - 3. 매핑 데이터 공유 화면
    //
    public void listClickListenerShareMap(int position)
    {
        mActivity.longTimeIdleHandlerUpdate(true);

        Log.d(TAG, "맵 공유 어댑터의 " + position + "번 아이템이 클릭되었습니다.");

        // 맵 공유 상태가, 사용자 정보 확인 중이거나 맵 공유 중이거나 외부기에 연결 시도 중인 상태라면 그냥 종료한다.
        if (mShareFsm == SHARE_FSM_USER_CHECK || mShareFsm == SHARE_FSM_SHARING || mShareFsm == SHARE_FSM_CONNECTING)
        {
            Log.d(TAG, "맵 공유 상태가 사용자 정보 확인, 맵 공유 중 또는 외부기로 연결 시도 중이므로 리스트 클릭 이벤트를 무시합니다.");
            return;
        }

        boolean isMapShared = mShareMapAdapter.isMapShared(position);
        boolean isItemSelected = mShareMapAdapter.isItemSelected(position);
        boolean newItemSelectedState = !isItemSelected;

        mShareMapAdapter.setItemSelectedStateForAll(false);
        mShareMapAdapter.setItemSelectState(position, newItemSelectedState);

        setShareInfoText(position);
        setShareInfoVisibility(newItemSelectedState);
        setOkButtonVisibility(newItemSelectedState && !isMapShared);

        if (isMapShared)
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
    public void listClickListenerMapReset(int position)
    {
        mActivity.longTimeIdleHandlerUpdate(true);

        Log.d(TAG, "맵 초기화 어댑터의 " + position + "번 아이템이 클릭되었습니다.");

        // 맵 초기화 상태가, 맵 초기화 중이거나 외부기에 연결 시도 중인 상태라면 그냥 종료한다.
        if (mResetFsm == RESET_FSM_RESETTING || mResetFsm == RESET_FSM_CONNECTING)
        {
            Log.d(TAG, "맵 초기화 상태가 맵 초기화 중 또는 외부기로 연결 시도 중이므로 리스트 클릭 이벤트를 무시합니다.");
            return;
        }

        boolean isMapResetDone = mMapResetAdapter.isMapResetDone(position);
        boolean isItemSelected = mMapResetAdapter.isItemSelected(position);
        boolean newItemSelectedState = !isItemSelected;

        mMapResetAdapter.setItemSelectedStateForAll(false);
        mMapResetAdapter.setItemSelectState(position, newItemSelectedState);

        setMapResetInfoText(position);
        setMapResetInfoVisibility(newItemSelectedState);
        setOkButtonVisibility(newItemSelectedState && !isMapResetDone);
    }

    //
    // 최초 매핑 시점 복귀 화면의 새롭게 검색된 상태 정보 업데이트
    //
    public void scanListUpdateMapReset()
    {
        for (int i = 0; i < mMapResetAdapter.getItemCount(); i++)
        {
            if (mMapResetAdapter.isItemSelected(i))
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

    public int getVisibleValue(boolean visible)
    {
        if (visible)
        {
            return View.VISIBLE;
        }
        else
        {
            return View.GONE;
        }
    }

    public void setLayoutFirstScreen(boolean visible)
    {
        mShareBinding.shareFirstScreenLayout.setVisibility(getVisibleValue(visible));
    }

    public void setLayoutSelectUser(boolean visible)
    {
        mShareBinding.shareSelectIsdLayout.setVisibility(getVisibleValue(visible));
    }

    public void setLayoutCollectMap(boolean visible)
    {
        mShareBinding.shareCollectMapDataLayout.setVisibility(getVisibleValue(visible));
    }

    public void setLayoutDistributeMap(boolean visible)
    {
        mShareBinding.shareDistributeMapDataLayout.setVisibility(getVisibleValue(visible));
    }

    public void setLayoutExistMap(boolean visible)
    {
        mShareBinding.shareExistMapLayout.setVisibility(getVisibleValue(visible));
    }

    public void setLayoutMapResetDefault(boolean visible)
    {
        mShareBinding.shareMapResetDefaultLayout.setVisibility(getVisibleValue(visible));
    }

    public void updateLayoutsWithFsm()
    {
        setLayoutFirstScreen(false);
        setLayoutSelectUser(false);
        setLayoutCollectMap(false);
        setLayoutDistributeMap(false);
        setLayoutExistMap(false);
        setLayoutMapResetDefault(false);

        switch (mFsm)
        {
            case FSM_FIRST_SCREEN:
                setLayoutFirstScreen(true);
                break;

            case FSM_SELECT_USER_SCREEN:
                setLayoutSelectUser(true);
                break;

            case FSM_COLLECT_MAP_SCREEN:
                setLayoutCollectMap(true);
                break;

            case FSM_SHARE_MAP_SCREEN:
                setLayoutDistributeMap(true);
                break;

            case FSM_EXIST_MAP_SCREEN:
                setLayoutExistMap(true);
                break;

            case FSM_MAP_RESET_DEFAULT_SCREEN:
                setLayoutMapResetDefault(true);
                break;
        }
    }

    //
    // 2. 매핑 데이터 수집 화면의 맵 수집 정보 레이아웃 간편 함수들
    //
    public void setCollectInfoVisibility(boolean visible)
    {
        mShareBinding.shareCollectMapDataInfoLayout.setVisibility(getVisibleValue(visible));
    }

    public void setCollectInfoText(int position)
    {
        String name = UtilUser.getNameOnly(mCollectMapAdapter.getName(position));
        String ear = UtilUser.getEarKorean(mCollectMapAdapter.getEar(position));
        String info;

        if (mCollectMapAdapter.isMapCollected(position))
        {
            if (mCollectMapAdapter.getItemCount() == mCollectMapAdapter.getCollectedMapCount())
            {
                info = "모든 매핑 데이터 수집이 완료되었습니다.";
            }
            else
            {
                info = name + "님 " + ear + " 내부기의 매핑 데이터 수집이 완료되었습니다.";
            }
        }
        else
        {
            if (mCollectMapAdapter.isBleScanned(position))
            {
                info = name + "님 " + ear + " 내부기의 매핑 데이터 수집을 진행합니다.";
            }
            else
            {
                info = name + "님 " + ear + " 내부기와 연결된 외부기가 검색되지 않았습니다.";
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

        Log.v(TAG, "수집 화면 프로그래스 출력 상태 = " + visible + ", 퍼센트 = " + percent);
    }

    //
    // 3. 매핑 데이터 공유 화면의 맵 공유 정보 레이아웃 간편 함수들
    //
    public void setShareInfoVisibility(boolean visible)
    {
        mShareBinding.shareDistributeTransferLayout.setVisibility(getVisibleValue(visible));
    }

    public void setShareInfoText(int position)
    {
        String serial = mShareMapAdapter.getOteSerial(position);
        String info;

        if (mShareMapAdapter.isMapShared(position))
        {
            if (mShareMapAdapter.getItemCount() == mShareMapAdapter.getSharedMapCount())
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

        Log.v(TAG, "공유 화면 프로그래스 출력 상태 = " + visible + ", 퍼센트 = " + percent);
    }

    //
    // 최초 매핑 시점 복귀 화면 레이아웃 간편 함수들
    //
    public void setMapResetInfoVisibility(boolean visible)
    {
        mShareBinding.shareMapResetDefaultInfoLayout.setVisibility(getVisibleValue(visible));
    }

    public void setMapResetInfoText(int position)
    {
        String serial = mMapResetAdapter.getOteSerial(position);
        String info;

        if (mMapResetAdapter.isMapResetDone(position))
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
        mShareBinding.shareOkButton.setVisibility(getVisibleValue(visible));
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
    public void setCancelButtonVisibility(boolean visible)
    {
        mShareBinding.shareCancelButton.setVisibility(getVisibleValue(visible));
    }

    public void setCancelButtonBackgroundColor(int color)
    {
        mShareBinding.shareCancelButton.setBackgroundColor(color);
    }

    public void setCancelButtonText(String text)
    {
        mShareBinding.shareCancelButton.setText(text);
    }

    //
    // 블루투스 LE 연결 해제 처리 관련
    //
    public void disconnectedEventProcessor()
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            mDataIndexHandler.removeCallbacks(mDataIndexRunner);
            mCountForConnectionFail++;

            switch (mFsm)
            {
                case FSM_COLLECT_MAP_SCREEN:
                {
                    if (mCollectFsm == COLLECT_FSM_COLLECTING || mCollectFsm == COLLECT_FSM_CONNECTING)
                    {
                        if (MAX_COUNT_CONNECTION_FAIL < mCountForConnectionFail)
                        {
                            boolean isAllItemScanned = true;

                            mCollectFsm = COLLECT_FSM_IDLE;

                            for (int i = 0; i < mCollectMapAdapter.getItemCount(); i++)
                            {
                                if (mCollectMapAdapter.isItemSelected(i))
                                {
                                    listClickListenerCollectMap(i);
                                }

                                if (!mCollectMapAdapter.isBleScanned(i))
                                {
                                    isAllItemScanned = false;
                                }
                            }

                            if (!isAllItemScanned)
                            {
                                Log.d(TAG, "모든 사용자가 스캔된 상태가 아니므로 BLE 스캔을 시작합니다.");
                                mActivity.scanLe(true);
                            }
                        }
                        else
                        {
                            okButtonCollectMapScreen();
                        }
                    }
                }
                break;

                case FSM_SHARE_MAP_SCREEN:
                {
                    if (mShareFsm == SHARE_FSM_CONNECTING || mShareFsm == SHARE_FSM_USER_CHECK || mShareFsm == SHARE_FSM_SHARING)
                    {
                        if (MAX_COUNT_CONNECTION_FAIL < mCountForConnectionFail)
                        {
                            mShareFsm = SHARE_FSM_IDLE;

                            for (int i = 0; i < mShareMapAdapter.getItemCount(); i++)
                            {
                                if (mShareMapAdapter.isItemSelected(i))
                                {
                                    listClickListenerShareMap(i);
                                }
                            }

                            Log.d(TAG, "공유할 외부기를 더 찾기 위하여 BLE 스캔을 시작합니다.");
                            mActivity.scanLe(true);
                        }
                        else
                        {
                            okButtonShareMapScreen();
                        }
                    }
                }
                break;

                case FSM_MAP_RESET_DEFAULT_SCREEN:
                {
                    if (mResetFsm == RESET_FSM_CONNECTING || mResetFsm == RESET_FSM_RESETTING)
                    {
                        if (MAX_COUNT_CONNECTION_FAIL < mCountForConnectionFail)
                        {
                            mResetFsm = RESET_FSM_IDLE;

                            for (int i = 0; i < mMapResetAdapter.getItemCount(); i++)
                            {
                                if (mMapResetAdapter.isItemSelected(i))
                                {
                                    listClickListenerMapReset(i);
                                }
                            }

                            Log.d(TAG, "초기화할 외부기를 더 찾기 위하여 BLE 스캔을 시작합니다.");
                            mActivity.scanLe(true);
                        }
                        else
                        {
                            okButtonMapReset();
                        }
                    }
                }
                break;
            } //  End of switch

            if (MAX_COUNT_CONNECTION_FAIL < mCountForConnectionFail)
            {
                Toast.makeText(mActivity, "외부기와 연결할 수 없습니다. 외부기가 내부기에 부착되어 있는지 확인해주세요.", Toast.LENGTH_LONG).show();
                mCountForConnectionFail = 0;
            }
        });
    }

    //
    // 맵 데이터 수집을 위한 패킷을 어떻게 전송할지 판단하고, 해당 패킷을 전송하는 함수
    //
    public void whichPacketShouldBeTransferred()
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            mActivity.longTimeIdleHandlerUpdate(true);

            switch (mFsm)
            {
                case FSM_COLLECT_MAP_SCREEN:
                {
                    mCollectFsm = COLLECT_FSM_COLLECTING;

                    boolean connError = false;

                    if (mCollectDataType == COLLECT_DATA_TYPE_ID_AND_USER
                            && (mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.slotNum != MapInfo.SLOT_MIN
                            || mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.indexNum != MapInfo.ID_USER_INDEX_MIN))
                    {
                        connError = true;
                    }
                    else if (mCollectDataType == COLLECT_DATA_TYPE_MAP_DATA
                            && (mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.slotNum != MapInfo.SLOT_MIN
                            || mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.mapNum != MapInfo.MAP_DATA_MAP_MIN
                            || mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.indexNum != MapInfo.MAP_DATA_INDEX_MIN))
                    {
                        connError = true;
                    }

                    if (connError)
                    {
                        Log.d(TAG, "데이터 수집 중 통신 에러가 발생되어 다시 연결되었습니다.");
                        Toast.makeText(requireContext(), "데이터 수집 중 통신 에러가 발생하여, 재연결 후 이어서 데이터를 수집합니다.", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        setCollectPercent(true, 0);
                    }

                    if (mCollectDataType == COLLECT_DATA_TYPE_ID_AND_USER)
                    {
                        mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);

                        mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;
                        mActivity.sendPacket(
                                mActivity.packetMaker(PacketInfo.HEADER_READ_ISD_ID_AND_USER,
                                        new byte[]{(byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.slotNum}, 2));
                    }
                    else if (mCollectDataType == COLLECT_DATA_TYPE_MAP_DATA)
                    {
                        mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.indexNum = MapInfo.MAP_DATA_INDEX_MIN;
                        mActivity.sendPacket(
                                mActivity.packetMaker(PacketInfo.HEADER_READ_MAP_DATA,
                                        new byte[]{(byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.slotNum,
                                                (byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.indexNum}, 3));
                    }
                }
                break;

                case FSM_SHARE_MAP_SCREEN:
                {
                    if (mShareFsm != SHARE_FSM_CONNECTING)
                    {
                        mShareDataType = SHARE_DATA_TYPE_READ_ID_AND_USER;

                        Log.d("MapSharing", "데이터 공유 중 통신 에러가 발생되어 다시 연결되었습니다.");
                        Toast.makeText(mActivity, "데이터 공유 중 통신 에러가 발생하여, 재연결 후 처음부터 데이터를 공유합니다.", Toast.LENGTH_LONG).show();
                    }

                    setSharePercent(true, 0);

                    mShareFsm = SHARE_FSM_USER_CHECK;

                    mShareDataType = SHARE_DATA_TYPE_READ_ID_AND_USER;

                    mShareMapInfo.idUser.slotNum = MapInfo.SLOT_MIN;
                    mShareMapInfo.idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;
                    //mShareMapInfo.dataType = MapInfo.DATA_TYPE_ID_USER;

                    mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);

                    mActivity.sendPacket(mActivity.packetMaker(PacketInfo.HEADER_READ_ISD_ID_AND_USER, new byte[]{(byte) 1}, 2));
                }
                break;

                case FSM_MAP_RESET_DEFAULT_SCREEN:
                {
                    if (mResetFsm != RESET_FSM_CONNECTING)
                    {
                        Log.d(TAG, "맵 초기화 중 통신 에러 발생 후 다시 연결된 상태입니다.");
                        Toast.makeText(mActivity, "매핑 데이터 초기화 중 통신 에러가 발생하여, 재연결 후 매핑 데이터 초기화를 계속 진행합니다.", Toast.LENGTH_LONG).show();
                    }

                    mResetFsm = RESET_FSM_RESETTING;
                    mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);
                    mActivity.sendPacket(mActivity.packetMaker(PacketInfo.HEADER_MAP_RESET_DEFAULT, null, 1));
                }
                break;
            } // End of switch (mFsm)
        }); // End of Handler
    }

    public void responsePacketProcessor(byte[] packet)
    {
        byte header = packet[0];

        switch (mFsm)
        {
            case FSM_COLLECT_MAP_SCREEN:
            {
                if (header == PacketInfo.HEADER_READ_ISD_ID_AND_USER)
                {
                    packetProcessCollectReadIdUser(packet);
                }
                else if (header == PacketInfo.HEADER_READ_MAP_DATA)
                {
                    packetProcessCollectReadMapData(packet);
                }
            }
            break;

            case FSM_SHARE_MAP_SCREEN:
            {
                if (header == PacketInfo.HEADER_READ_ISD_ID_AND_USER)
                {
                    packetProcessShareReadIdUser(packet);
                }
                else if (header == PacketInfo.HEADER_WRITE_ISD_ID_AND_USER)
                {
                    packetProcessShareWriteIdUser(packet);
                }
                else if (header == PacketInfo.HEADER_WRITE_MAP_DATA)
                {
                    packetProcessShareWriteMapData(packet);
                }
            }
            break;

            case FSM_MAP_RESET_DEFAULT_SCREEN:
            {
                if (header == PacketInfo.HEADER_MAP_RESET_DEFAULT)
                {
                    packetProcessResetMapData(packet);
                }
            }
            break;
        }
    }

    public void packetProcessCollectReadIdUser(byte[] packet)
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            mActivity.longTimeIdleHandlerUpdate(true);

            int slotNum = mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.slotNum;
            int indexNum = mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.indexNum;

            Log.v(TAG, "packetProcessCollectReadIdUser() : " + mActivity.printLogBytesToString(packet));

            if (packet[1] == MapInfo.ID_USER_INDEX_MAX)
            {
                mDataIndexHandler.removeCallbacks(mDataIndexRunner);
            }

            if (indexNum != packet[1])
            {
                Log.d(TAG, "packetProcessCollectReadIdUser() : 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);

                mDataIndexHandler.removeCallbacks(mDataIndexRunner);

                if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED && mActivity.mBluetoothGatt != null)
                {
                    Log.d(TAG, "packetProcessCollectReadIdUser() : 강제로 연결을 해제하여 외부기와 재연결 한 뒤 다시 매핑 데이터 수집을 시작하겠습니다.");
                    mActivity.mBluetoothGatt.disconnect();
                }

                return;
            }

            byte[] data = mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.data[slotNum - 1][indexNum - 1]; // MIN 값이 1부터인데, 인덱스는 0부터니까 1씩 빼고 계산한다.

            System.arraycopy(packet, 0, data, 0, packet.length); // src, src_idx, dst, dst_idx, length

            if (packet[1] == MapInfo.ID_USER_INDEX_MAX)
            {
                mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.slotNum = slotNum + 1;
                mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;

                if (MapInfo.SLOT_MAX < mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.slotNum)
                {
                    Log.d(TAG, "packetProcessCollectReadIdUser() : 데이터 수집 완료.");

                    mCollectDataType = COLLECT_DATA_TYPE_MAP_DATA;

                    mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);

                    // MapData 수집을 시작해야 한다.
                    mActivity.sendPacket(
                            mActivity.packetMaker(PacketInfo.HEADER_READ_MAP_DATA,
                                    new byte[]{(byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.slotNum,
                                            (byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.mapNum}, 3));
                }
                else
                {
                    mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);

                    mActivity.sendPacket(
                            mActivity.packetMaker(PacketInfo.HEADER_READ_ISD_ID_AND_USER,
                                    new byte[]{(byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.slotNum}, 2));
                }
            }
            else
            {
                mCollectedMapInfo[mCollectedMapInfoCurrentIndex].idUser.indexNum = indexNum + 1;
            }

            int progress = ((slotNum - MapInfo.SLOT_MIN) * MapInfo.ID_USER_INDEX_MAX) + indexNum;
            int percent = (int) ((((double) progress) / 252) * 100); // 252 : IdUser -> 4 * 3, MapData -> 4 * 4 * 15

            setCollectPercent(true, percent);
        });
    }

    public void packetProcessCollectReadMapData(byte[] packet)
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            mActivity.longTimeIdleHandlerUpdate(true);

            int slotNum = mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.slotNum;
            int mapNum = mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.mapNum;
            int indexNum = mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.indexNum;

            Log.v(TAG, "packetProcessCollectReadMapData() : " + mActivity.printLogBytesToString(packet));

            if (packet[1] == MapInfo.MAP_DATA_INDEX_MAX)
            {
                mDataIndexHandler.removeCallbacks(mDataIndexRunner);
            }

            if (indexNum != packet[1])
            {
                Log.d(TAG, "packetProcessCollectReadMapData() : 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 맵 = " + mapNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);

                mDataIndexHandler.removeCallbacks(mDataIndexRunner);

                if (mActivity.mBluetoothGatt != null)
                {
                    Log.d(TAG, "packetProcessCollectReadIdUser() : 강제로 연결을 해제하여 외부기와 재연결 한 뒤 다시 매핑 데이터 수집을 시작하겠습니다.");
                    mActivity.mBluetoothGatt.disconnect();
                }

                return;
            }

            byte[] data = mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.data[slotNum - 1][mapNum - 1][indexNum - 1];

            System.arraycopy(packet, 0, data, 0, packet.length); // src, src_idx, dst, dst_idx, length

            if (packet[1] == MapInfo.MAP_DATA_INDEX_MAX)
            {
                mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.mapNum = mapNum + 1;
                mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.indexNum = MapInfo.MAP_DATA_INDEX_MIN;

                if (MapInfo.MAP_DATA_MAP_MAX < mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.mapNum)
                {
                    mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.slotNum = slotNum + 1;
                    mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.mapNum = MapInfo.MAP_DATA_MAP_MIN;

                    if (MapInfo.SLOT_MAX < mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.slotNum)
                    {
                        Log.d(TAG, "packetProcessCollectReadMapData() : 데이터 수집 완료.");

                        mCollectFsm = COLLECT_FSM_COLLECTED;

                        mCollectedMapInfo[mCollectedMapInfoCurrentIndex].metadata.isFilled = true;
                        mCollectedMapInfo[mCollectedMapInfoCurrentIndex].updateMetadata();

                        if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED && mActivity.mBluetoothGatt != null)
                        {
                            mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                            mActivity.mBluetoothGatt.disconnect();
                        }

                        for (int i = 0; i < mCollectMapAdapter.getItemCount(); i++)
                        {
                            if (mCollectMapAdapter.isItemSelected(i))
                            {
                                mCollectMapAdapter.setMapCollectState(i);
                                setCollectInfoText(i);

                                if (mCollectMapAdapter.getCollectedMapCount() != mCollectMapAdapter.getItemCount())
                                {
                                    setOkButtonVisibility(false);
                                    mActivity.scanLeWithDelay(true, 100);
                                }
                                else
                                {
                                    setOkButtonVisibility(true);
                                    setOkButtonText("다음 절차 진행");
                                    setOkButtonBackgroundColor(BUTTON_COLOR_ACCENT);

                                    Log.d(TAG, "\r\n\n\n\n수집한 모든 매핑 데이터 정보 출력.");
                                    Log.d(TAG, "매핑 데이터 총 개수 -> MapInfo[" + mCollectedMapInfo.length + "]");

                                    for (int map_i = 0; map_i < mCollectedMapInfo.length; map_i++)
                                    {
                                        if (mCollectedMapInfo[map_i].metadata.isFilled)
                                        {
                                            for (int slot_i = 0; slot_i < 4; slot_i++)
                                            {
                                                Log.d(TAG, "MapInfo[" + map_i + "]의 슬롯 " + slot_i
                                                        + " : 이름 = " + mCollectedMapInfo[map_i].metadata.names[slot_i]
                                                        + ", 수술위치 = " + mCollectedMapInfo[map_i].metadata.ears[slot_i]
                                                        + ", 맵 버전 = " + mCollectedMapInfo[map_i].metadata.stamps[slot_i]);
                                            }
                                        }
                                    }

                                    Log.d(TAG, "\r\n\n수집된 모든 매핑 데이터에서 가장 최신의 맵 정보만 얻어옵니다.");
                                    makeMostRecentMapInfo();

                                    Log.d(TAG, "\r\n\n데이터베이스에 저장된 기존 매핑 데이터를 제거하고 현재 수집된 가장 최신의 맵 정보들을 저장합니다.");

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
                                        stringMapDataSlots[slot_i] = MapInfo.getStringFromMapInfo(mMostRecentMapInfo, slot_i);

                                        entityMaps[slot_i] = new EntityMap();
                                        entityMaps[slot_i].name_ear = EntityMap.makePrimaryKey(mMostRecentMapInfo.metadata.names[slot_i], mMostRecentMapInfo.metadata.ears[slot_i]);
                                        entityMaps[slot_i].name = mMostRecentMapInfo.metadata.names[slot_i];
                                        entityMaps[slot_i].ear = mMostRecentMapInfo.metadata.ears[slot_i];
                                        entityMaps[slot_i].stamp = mMostRecentMapInfo.metadata.stamps[slot_i];
                                        entityMaps[slot_i].serialize_map_data = stringMapDataSlots[slot_i];

                                        if (!entityMaps[slot_i].name.equals(MapInfo.EMPTY_MAP_NAME))
                                        {
                                            UtilMap.instance.insert(entityMaps[slot_i]);
                                        }

                                        Log.d(TAG, "RecentMapInfo의 슬롯 " + slot_i
                                                + " : 이름 = " + mMostRecentMapInfo.metadata.names[slot_i]
                                                + ", 수술위치 = " + mMostRecentMapInfo.metadata.ears[slot_i]
                                                + ", 맵 버전 = " + mMostRecentMapInfo.metadata.stamps[slot_i]);

                                        Log.d(TAG, "직렬화된 매핑 데이터 = " + stringMapDataSlots[slot_i]);
                                    }

                                    //
                                    // 데이터 직렬화 테스트
                                    //
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

                                        Log.d("MapSerialize", "\r\n\n매핑 데이터 직렬화 테스트 :");
                                        Log.d("MapSerialize", cmpMapDataSlot0);
                                        Log.d("MapSerialize", cmpMapDataSlot1);
                                        Log.d("MapSerialize", cmpMapDataSlot2);
                                        Log.d("MapSerialize", cmpMapDataSlot3);

                                        boolean cmpResult0 = stringMapDataSlots[0].equals(cmpMapDataSlot0);
                                        boolean cmpResult1 = stringMapDataSlots[1].equals(cmpMapDataSlot1);
                                        boolean cmpResult2 = stringMapDataSlots[2].equals(cmpMapDataSlot2);
                                        boolean cmpResult3 = stringMapDataSlots[3].equals(cmpMapDataSlot3);

                                        Log.d("MapSerialize", "결과 : cmpResult0 = " + cmpResult0 + ", cmpResult1 = " + cmpResult1
                                                + ", cmpResult2 = " + cmpResult2 + ", cmpResult3 = " + cmpResult3);
                                    }
                                } // end else (모든 데이터 수집 완료)
                            } // end if
                        }// end for
                    }
                    else
                    {
                        mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);

                        // 슬롯 번호 값을 증가하고 전송 시작
                        mActivity.sendPacket(
                                mActivity.packetMaker(PacketInfo.HEADER_READ_MAP_DATA,
                                        new byte[]{(byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.slotNum,
                                                (byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.mapNum}, 3));
                    }
                }
                else
                {
                    mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);

                    // 맵 번호 값을 올리고 전송 시작
                    mActivity.sendPacket(
                            mActivity.packetMaker(PacketInfo.HEADER_READ_MAP_DATA,
                                    new byte[]{(byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.slotNum,
                                            (byte) mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.mapNum}, 3));
                }
            }
            else
            {
                mCollectedMapInfo[mCollectedMapInfoCurrentIndex].mapData.indexNum = indexNum + 1;
            }

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
    public void packetProcessShareReadIdUser(byte[] packet)
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            mActivity.longTimeIdleHandlerUpdate(true);

            int slotNum = mShareMapInfo.idUser.slotNum;
            int indexNum = mShareMapInfo.idUser.indexNum;

            Log.v(TAG, "packetProcessShareReadIdUser() : " + mActivity.printLogBytesToString(packet));

            if (packet[1] == MapInfo.ID_USER_INDEX_MAX)
            {
                mDataIndexHandler.removeCallbacks(mDataIndexRunner);
            }

            if (indexNum != packet[1])
            {
                Log.d(TAG, "packetProcessShareReadIdUser() : 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);

                mDataIndexHandler.removeCallbacks(mDataIndexRunner);

                if (mActivity.mBluetoothGatt != null)
                {
                    Log.d(TAG, "packetProcessShareReadIdUser() : 강제로 연결을 해제하여 외부기와 재연결 한 뒤 다시 매핑 데이터 공유를 시작하겠습니다.");
                    mActivity.mBluetoothGatt.disconnect();
                }

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
                    if (mMostRecentMapInfo.metadata.names[i].equals(oteUser) && mMostRecentMapInfo.metadata.ears[i].equals(oteEar))
                    {
                        mainUserIndex = i;
                    }
                }

                if (mainUserIndex < 0)
                {
                    Log.d(TAG, "최신 맵 정보에서 사용자 " + oteUser + ", 수술 부위 " + oteEar + "를 찾을 수 없습니다.");
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

                    Log.d(TAG, "공유할 매핑 데이터 생성을 완료했습니다.");

                    for (int print_i = 0; print_i < 4; print_i++)
                    {
                        Log.d(TAG, "새로 생성한 공유할 매핑 데이터의 슬롯 " + print_i
                                + " : 이름 = " + mShareMapInfo.metadata.names[print_i]
                                + ", 수술위치 = " + mShareMapInfo.metadata.ears[print_i]
                                + ", 맵 버전 = " + mShareMapInfo.metadata.stamps[print_i]);
                    }

                    mShareDataType = SHARE_DATA_TYPE_WRITE_ID_AND_USER;

                    mShareMapInfo.prepareWriting();

                    // IdUser 정보부터 쓰기 시작해야 한다.
                    mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);
                    mActivity.sendPacket(mShareMapInfo.idUser.writing[0][0]);
                }
            }
            else
            {
                mShareMapInfo.idUser.indexNum = indexNum + 1;
            }

            int progress = ((slotNum - MapInfo.SLOT_MIN) * MapInfo.ID_USER_INDEX_MAX) + indexNum;
            // 264 : IdUser read -> 4 * 3, IdUser write -> 4 *3, MapData write -> 4 * 4 * 15
            int percent = (int) ((((double) progress) / 264) * 100);

            setSharePercent(true, percent);
        });
    }

    public void packetProcessShareWriteIdUser(byte[] packet)
    {
        mActivity.longTimeIdleHandlerUpdate(true);

        boolean isDone = false;
        int slotNum = mShareMapInfo.idUser.slotNum;
        int indexNum = mShareMapInfo.idUser.indexNum;

        Log.v(TAG, "packetProcessShareWriteIdUser() : " + mActivity.printLogBytesToString(packet));

        if (indexNum != packet[1])
        {
            Log.d(TAG, "packetProcessShareWriteIdUser() : 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);

            mDataIndexHandler.removeCallbacks(mDataIndexRunner);

            if (mActivity.mBluetoothGatt != null)
            {
                Log.d(TAG, "packetProcessShareWriteIdUser() : 강제로 연결을 해제하여 외부기와 재연결 한 뒤 다시 매핑 데이터 공유를 시작하겠습니다.");
                mActivity.mBluetoothGatt.disconnect();
            }

            return;
        }

        mDataIndexHandler.removeCallbacks(mDataIndexRunner);

        mShareMapInfo.idUser.indexNum = indexNum + 1;

        if (MapInfo.ID_USER_INDEX_MAX < mShareMapInfo.idUser.indexNum)
        {
            mShareMapInfo.idUser.slotNum = slotNum + 1;
            mShareMapInfo.idUser.indexNum = MapInfo.ID_USER_INDEX_MIN;

            if (MapInfo.SLOT_MAX < mShareMapInfo.idUser.slotNum)
            {
                Log.d(TAG, "packetProcessShareWriteIdUser() : 매핑 데이터 공유 완료.");
                isDone = true;
            }
        }

        if (isDone)
        {
            // MapData 공유 시작
            mShareDataType = SHARE_DATA_TYPE_WRITE_MAP_DATA;

            mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);
            mActivity.sendPacket(mShareMapInfo.mapData.writing[mShareMapInfo.mapData.slotNum - 1][mShareMapInfo.mapData.mapNum - 1][mShareMapInfo.mapData.indexNum - 1]);
        }
        else
        {
            // 다음 데이터 전송
            mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);
            mActivity.sendPacket(mShareMapInfo.idUser.writing[mShareMapInfo.idUser.slotNum - 1][mShareMapInfo.idUser.indexNum - 1]);
        }

        new Handler(Looper.getMainLooper()).post(() ->
        {
            int progress = ((slotNum - MapInfo.SLOT_MIN) * MapInfo.ID_USER_INDEX_MAX) + indexNum + 12;
            // 264 : IdUser read -> 4 * 3, IdUser write -> 4 *3, MapData write -> 4 * 4 * 15
            int percent = (int) ((((double) progress) / 264) * 100);

            setSharePercent(true, percent);
        });
    }

    public void packetProcessShareWriteMapData(byte[] packet)
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            mActivity.longTimeIdleHandlerUpdate(true);

            boolean isDone = false;

            int slotNum = mShareMapInfo.mapData.slotNum;
            int mapNum = mShareMapInfo.mapData.mapNum;
            int indexNum = mShareMapInfo.mapData.indexNum;

            Log.v(TAG, "packetProcessShareWriteMapData() : " + mActivity.printLogBytesToString(packet));

            if (indexNum != packet[1])
            {
                Log.d(TAG, "packetProcessShareWriteMapData() : 데이터 인덱스가 일치하지 않습니다. 슬롯 = " + slotNum + ", 맵 = " + mapNum + ", 앱 인덱스 = " + indexNum + ", 수신패킷 인덱스 = " + packet[1]);

                mDataIndexHandler.removeCallbacks(mDataIndexRunner);

                if (mActivity.mBluetoothGatt != null)
                {
                    Log.d(TAG, "packetProcessShareWriteMapData() : 강제로 연결을 해제하여 외부기와 재연결 한 뒤 다시 매핑 데이터 공유를 시작하겠습니다.");
                    mActivity.mBluetoothGatt.disconnect();
                }

                return;
            }

            mDataIndexHandler.removeCallbacks(mDataIndexRunner);

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
                        Log.d(TAG, "packetProcessShareWriteMapData() : 매핑 데이터 공유 완료.");

                        mShareFsm = SHARE_FSM_SHARED;

                        isDone = true;

                        for (int i = 0; i < mShareMapAdapter.getItemCount(); i++)
                        {
                            if (mShareMapAdapter.isItemSelected(i))
                            {
                                mShareMapAdapter.setMapShareState(i, true);
                                setShareInfoText(i);

                                mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                                mActivity.mBluetoothGatt.disconnect();
                                break;
                            }
                        }

                        Log.d(TAG, "공유할 더 많은 외부기를 찾기위해 BLE 스캔을 시작합니다.");
                        mActivity.scanLe(true);
                    }
                }
            }

            if (!isDone)
            {
                mDataIndexHandler.postDelayed(mDataIndexRunner, HANDLER_TIMEOUT_IN_MS);
                mActivity.sendPacket(mShareMapInfo.mapData.writing[mShareMapInfo.mapData.slotNum - 1][mShareMapInfo.mapData.mapNum - 1][mShareMapInfo.mapData.indexNum - 1]);
            }

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
    public void packetProcessResetMapData(byte[] packet)
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            mActivity.longTimeIdleHandlerUpdate(true);

            mDataIndexHandler.removeCallbacks(mDataIndexRunner);

            boolean isDone = false;

            if (packet.length != 2)
            {
                isDone = false;
                Log.d(TAG, "매핑 데이터 초기화에 대한 응답 패킷 사이즈가 올바르지 않습니다. 사이즈 = " + packet.length);
            }
            else if (packet[0] != PacketInfo.HEADER_MAP_RESET_DEFAULT || packet[1] != PacketInfo.MAP_RESET_DEFAULT_OK)
            {
                isDone = false;
                Log.d(TAG, "매핑 데이터 초기화에 대한 응답 패킷 정보가 올바르지 않습니다. 헤더 = " + packet[0] + ", 데이터 = " + packet[1]);
            }
            else
            {
                Log.d(TAG, "매핑 데이터 초기화 완료.");

                mResetFsm = RESET_FSM_DONE;

                for (int itemIndex = 0; itemIndex < mMapResetAdapter.getItemCount(); itemIndex++)
                {
                    if (mMapResetAdapter.isItemSelected(itemIndex))
                    {
                        mMapResetAdapter.setResetDone(itemIndex, true);
                        setMapResetInfoText(itemIndex);
                        break;
                    }
                }
            }

            if (mStatus.connectionState == Status.CONNECTION_STATE_CONNECTED)
            {
                if (isDone)
                {
                    mStatus.connectionState = Status.CONNECTION_STATE_DISCONNECTING;
                }

                mActivity.mBluetoothGatt.disconnect();
            }

            if (mMapResetAdapter.getItemCount() == mMapResetAdapter.getResetDoneCount())
            {
                mActivity.scanLe(true);
            }
        });
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

        mapInfoCopyAll(mCollectedMapInfo[0], mMostRecentMapInfo);

        if (1 < mCollectedMapInfo.length) // 저장된 맵 데이터가 1개 보다 많을 때
        {
            for (int len = 1; len < mCollectedMapInfo.length; len++) // 저장된 맵들 모두 비교할 것이다.
            {
                for (int slot_i = 0; slot_i < 4; slot_i++) // 슬롯 4개를 비교하기 위함.
                {
                    boolean isExist = false; // 가장 최신 맵 정보에 현재 맵 정보의 슬롯 정보가 이미 존재하는지 검사하기 위함.

                    for (int find_i = 0; find_i < 4; find_i++) // 현 맵 정보의 각 슬롯이 가장 최신 맵 정보의 슬롯 4개중에 일치하는게 있는지 찾기 위함
                    {
                        if (mCollectedMapInfo[len].metadata.names[slot_i].equals(mMostRecentMapInfo.metadata.names[find_i]) && mCollectedMapInfo[len].metadata.ears[slot_i].equals(mMostRecentMapInfo.metadata.ears[find_i]))
                        {
                            isExist = true;

                            // 가장 최신 맵 정보보다 더 최신인 데이터라면 복사한다.
                            if (mMostRecentMapInfo.metadata.stamps[find_i] < mCollectedMapInfo[len].metadata.stamps[slot_i])
                            {
                                mapInfoSlotCopy(mCollectedMapInfo[len], mMostRecentMapInfo, slot_i, find_i);
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
                            if (mMostRecentMapInfo.metadata.names[empty_i].equals(MapInfo.EMPTY_MAP_NAME))
                            {
                                mapInfoSlotCopy(mCollectedMapInfo[len], mMostRecentMapInfo, slot_i, empty_i);
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

        //dst.dataType = src.dataType;

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