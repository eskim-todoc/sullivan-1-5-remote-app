package todoc.cochlear.remoteapp.view_model;

import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import todoc.cochlear.remoteapp.params.PacketInfo;
import todoc.cochlear.remoteapp.params.Status;

public class StatusViewModel extends ViewModel {
    // BLE 연결 상태 관련
    static public final int CONNECTION_STATE_DISCONNECTED = 0;
    static public final int CONNECTION_STATE_CONNECTING = 1;
    static public final int CONNECTION_STATE_CONNECTED = 2;
    static public final int CONNECTION_STATE_DISCONNECTING = 3;

    private final MutableLiveData<Integer> mConnectionState = new MutableLiveData<>();

    public MutableLiveData<Integer> getObjectConnectionState() {
        return mConnectionState;
    }

    public int getConnectionState() {
        if (mConnectionState.getValue() == null) {
            mConnectionState.setValue(CONNECTION_STATE_DISCONNECTED);
        }

        return mConnectionState.getValue();
    }

    public void setConnectionState(int connectionState) {
        if (Looper.getMainLooper().isCurrentThread()) {
            mConnectionState.setValue(connectionState);
        } else {
            mConnectionState.postValue(connectionState);
        }
    }

    // OTE FW Version
    private final MutableLiveData<Integer> mFwVerUpper = new MutableLiveData<>();
    private final MutableLiveData<Integer> mFwVerLower = new MutableLiveData<>();

    public int getFwVerUpper() {
        if (mFwVerUpper.getValue() == null) {
            mFwVerUpper.setValue(-1);
        }

        return mFwVerUpper.getValue();
    }

    public int getFwVerLower() {
        if (mFwVerLower.getValue() == null) {
            mFwVerLower.setValue(-1);
        }

        return mFwVerLower.getValue();
    }

    public void setFwVerUpper(int fwVerUpper) {
        if (Looper.getMainLooper().isCurrentThread()) {
            mFwVerUpper.setValue(fwVerUpper);
        } else {
            mFwVerUpper.postValue(fwVerUpper);
        }
    }

    public void setFwVerLower(int fwVerLower) {
        if (Looper.myLooper().isCurrentThread()) {
            mFwVerLower.setValue(fwVerLower);
        } else {
            mFwVerLower.postValue(fwVerLower);
        }
    }

    // ISD ID
    private final MutableLiveData<Integer> mIsdID = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataIsdID() {
        return mIsdID;
    }

    public void setValueIsdID(int id) {
        mIsdID.setValue(id);
    }

    public int getValueIsdID() {
        if (mIsdID.getValue() == null) {
            mIsdID.setValue(0);
        }

        return mIsdID.getValue();
    }

    // Battery level
    private final MutableLiveData<Integer> mBatteryLevel = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataBatteryLevel() {
        return mBatteryLevel;
    }

    public void setValueBatteryLevel(int level) {
        mBatteryLevel.setValue(level);
    }

    public int getValueBatteryLevel() {
        if (mBatteryLevel.getValue() == null) {
            mBatteryLevel.setValue(PacketInfo.INIT_VALUE_BATTERY);
        }

        return mBatteryLevel.getValue();
    }

    // Notification
    private final MutableLiveData<Integer> mNotification = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataNotification() {
        return mNotification;
    }

    public void setValueNotification(int value) {
        mNotification.setValue(value);
    }

    public int getValueNotification() {
        if (mNotification.getValue() == null) {
            mNotification.setValue(PacketInfo.INIT_VALUE_NOTIFICATION);
        }

        return mNotification.getValue();
    }

    // LED
    private final MutableLiveData<Integer> mLed = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataLed() {
        return mLed;
    }

    public void setValueLed(int value) {
        mLed.setValue(value);
    }

    public int getValueLed() {
        if (mLed.getValue() == null) {
            mLed.setValue(PacketInfo.INIT_VALUE_LED);
        }

        return mLed.getValue();
    }

    // Telecoil
    private final MutableLiveData<Integer> mTelecoil = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataTelecoil() {
        return mTelecoil;
    }

    public void setValueTelecoil(int value) {
        mTelecoil.setValue(value);
    }

    public int getValueTelecoil() {
        if (mTelecoil.getValue() == null) {
            mTelecoil.setValue(PacketInfo.INIT_VALUE_TELECOIL);
        }

        return mTelecoil.getValue();
    }

    // Max output
    private final MutableLiveData<Integer> mMaxOutput = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataMaxOutput() {
        return mMaxOutput;
    }

    public void setValueMaxOutput(int value) {
        mMaxOutput.setValue(value);
    }

    public int getValueMaxOutput() {
        if (mMaxOutput.getValue() == null) {
            mMaxOutput.setValue(PacketInfo.INIT_VALUE_MAX_OUTPUT);
        }

        return mMaxOutput.getValue();
    }

    // Volume
    private final MutableLiveData<Integer> mVolume = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataVolume() {
        return mVolume;
    }

    public void setValueVolume(int value) {
        mVolume.setValue(value);
    }

    public int getValueVolume() {
        if (mVolume.getValue() == null) {
            mVolume.setValue(PacketInfo.INIT_VALUE_VOLUME);
        }

        return mVolume.getValue();
    }

    // Program
    private final MutableLiveData<Integer> mProgram = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataProgram() {
        return mProgram;
    }

    public void setValueProgram(int value) {
        mProgram.setValue(value);
    }

    public int getValueProgram() {
        if (mProgram.getValue() == null) {
            mProgram.setValue(PacketInfo.INIT_VALUE_PROGRAM);
        }

        return mProgram.getValue();
    }

    // 링크 Tx 파워 하한(PMIC) 레벨. 1스텝 = 25mV. 연결 직후 특수 명령(0x59) 옵션 5로 읽어온다.
    private final MutableLiveData<Integer> mMinTxPowerLevel = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataMinTxPowerLevel() {
        return mMinTxPowerLevel;
    }

    public void setValueMinTxPowerLevel(int value) {
        mMinTxPowerLevel.setValue(value);
    }

    public int getValueMinTxPowerLevel() {
        if (mMinTxPowerLevel.getValue() == null) {
            mMinTxPowerLevel.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mMinTxPowerLevel.getValue();
    }

    /* 링크 Tx 파워 상한(PMIC) 레벨. 1스텝 = 25mV. 프로토콜 4.5 의 인덱스 16 이다.
     *
     * 전체 읽기 응답에 실리지 않으므로 «판별이 끝난 뒤 개별 읽기» 로만 채워진다.
     * 4.5 미만이거나 아직 못 읽었으면 LINK_VALUE_UNKNOWN 이고, 그때는 화면에서 숨긴다. */
    private final MutableLiveData<Integer> mMaxTxPowerLevel = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataMaxTxPowerLevel() {
        return mMaxTxPowerLevel;
    }

    public void setValueMaxTxPowerLevel(int value) {
        mMaxTxPowerLevel.setValue(value);
    }

    public int getValueMaxTxPowerLevel() {
        if (mMaxTxPowerLevel.getValue() == null) {
            mMaxTxPowerLevel.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mMaxTxPowerLevel.getValue();
    }


    /* 자극 레벨 피드포워드 (프로토콜 4.5 후반). 전부 개별 읽기로만 채워진다. */

    // 피드포워드 사용 (인덱스 17). 0 · 1
    private final MutableLiveData<Integer> mFfEnable = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfEnable() {
        return mFfEnable;
    }

    public void setValueFfEnable(int value) {
        mFfEnable.setValue(value);
    }

    public int getValueFfEnable() {
        if (mFfEnable.getValue() == null) {
            mFfEnable.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfEnable.getValue();
    }

    // 피드포워드 쿨다운 msec (18)
    private final MutableLiveData<Integer> mFfCooldown = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfCooldown() {
        return mFfCooldown;
    }

    public void setValueFfCooldown(int value) {
        mFfCooldown.setValue(value);
    }

    public int getValueFfCooldown() {
        if (mFfCooldown.getValue() == null) {
            mFfCooldown.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfCooldown.getValue();
    }

    // 피드포워드 상승 스텝 (19). 25mV 단위
    private final MutableLiveData<Integer> mFfStep = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfStep() {
        return mFfStep;
    }

    public void setValueFfStep(int value) {
        mFfStep.setValue(value);
    }

    public int getValueFfStep() {
        if (mFfStep.getValue() == null) {
            mFfStep.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfStep.getValue();
    }

    // 비율 구간 0 % (20). 채널당 평균 0~63
    private final MutableLiveData<Integer> mFfRatio0 = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfRatio0() {
        return mFfRatio0;
    }

    public void setValueFfRatio0(int value) {
        mFfRatio0.setValue(value);
    }

    public int getValueFfRatio0() {
        if (mFfRatio0.getValue() == null) {
            mFfRatio0.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfRatio0.getValue();
    }

    // 비율 구간 1 % (21). 64~127
    private final MutableLiveData<Integer> mFfRatio1 = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfRatio1() {
        return mFfRatio1;
    }

    public void setValueFfRatio1(int value) {
        mFfRatio1.setValue(value);
    }

    public int getValueFfRatio1() {
        if (mFfRatio1.getValue() == null) {
            mFfRatio1.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfRatio1.getValue();
    }

    // 비율 구간 2 % (22). 128~191
    private final MutableLiveData<Integer> mFfRatio2 = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfRatio2() {
        return mFfRatio2;
    }

    public void setValueFfRatio2(int value) {
        mFfRatio2.setValue(value);
    }

    public int getValueFfRatio2() {
        if (mFfRatio2.getValue() == null) {
            mFfRatio2.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfRatio2.getValue();
    }

    // 비율 구간 3 % (23). 192~255
    private final MutableLiveData<Integer> mFfRatio3 = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfRatio3() {
        return mFfRatio3;
    }

    public void setValueFfRatio3(int value) {
        mFfRatio3.setValue(value);
    }

    public int getValueFfRatio3() {
        if (mFfRatio3.getValue() == null) {
            mFfRatio3.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfRatio3.getValue();
    }

    // CFX 가 플래그를 세운 횟수 (24). 하위 8비트라 차분을 mod 256 으로 본다
    private final MutableLiveData<Integer> mFfRaised = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfRaised() {
        return mFfRaised;
    }

    public void setValueFfRaised(int value) {
        mFfRaised.setValue(value);
    }

    public int getValueFfRaised() {
        if (mFfRaised.getValue() == null) {
            mFfRaised.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfRaised.getValue();
    }

    // CM3 가 실제로 올린 횟수 (25). 24 - 25 가 합쳐진 요청이다
    private final MutableLiveData<Integer> mFfApplied = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfApplied() {
        return mFfApplied;
    }

    public void setValueFfApplied(int value) {
        mFfApplied.setValue(value);
    }

    public int getValueFfApplied() {
        if (mFfApplied.getValue() == null) {
            mFfApplied.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfApplied.getValue();
    }

    // 현재 채널당 평균 amplitude (26). 비율 구간 판정과 같은 값이다
    private final MutableLiveData<Integer> mFfAvgAmp = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFfAvgAmp() {
        return mFfAvgAmp;
    }

    public void setValueFfAvgAmp(int value) {
        mFfAvgAmp.setValue(value);
    }

    public int getValueFfAvgAmp() {
        if (mFfAvgAmp.getValue() == null) {
            mFfAvgAmp.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFfAvgAmp.getValue();
    }

    // 매핑(피팅) 전용 Tx 파워 하한 레벨. 1스텝 = 25mV. 연결 직후 특수 명령(0x59) 옵션 9로 읽어온다.
    private final MutableLiveData<Integer> mMappingTxPowerLevel = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataMappingTxPowerLevel() {
        return mMappingTxPowerLevel;
    }

    public void setValueMappingTxPowerLevel(int value) {
        mMappingTxPowerLevel.setValue(value);
    }

    public int getValueMappingTxPowerLevel() {
        if (mMappingTxPowerLevel.getValue() == null) {
            mMappingTxPowerLevel.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mMappingTxPowerLevel.getValue();
    }

    // 링크 Tx 파워 상승 스텝. 1스텝 = 25mV. 연결 직후 특수 명령(0x59) 옵션 11로 읽어온다.
    private final MutableLiveData<Integer> mTxStepUp = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataTxStepUp() {
        return mTxStepUp;
    }

    public void setValueTxStepUp(int value) {
        mTxStepUp.setValue(value);
    }

    public int getValueTxStepUp() {
        if (mTxStepUp.getValue() == null) {
            mTxStepUp.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mTxStepUp.getValue();
    }

    // 링크 Tx 파워 강제 고정 값. 0 이면 해제 상태. 연결 직후 특수 명령(0x59) 옵션 13으로 읽어온다.
    private final MutableLiveData<Integer> mForceTxPowerLevel = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataForceTxPowerLevel() {
        return mForceTxPowerLevel;
    }

    public void setValueForceTxPowerLevel(int value) {
        mForceTxPowerLevel.setValue(value);
    }

    public int getValueForceTxPowerLevel() {
        if (mForceTxPowerLevel.getValue() == null) {
            mForceTxPowerLevel.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mForceTxPowerLevel.getValue();
    }

    // 현재 Tx 파워(관찰값). 지금 PMIC 에 실제로 쓰인 값이라 설정값들과 성격이 다르다.
    private final MutableLiveData<Integer> mCurTxPowerLevel = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataCurTxPowerLevel() {
        return mCurTxPowerLevel;
    }

    public void setValueCurTxPowerLevel(int value) {
        mCurTxPowerLevel.setValue(value);
    }

    public int getValueCurTxPowerLevel() {
        if (mCurTxPowerLevel.getValue() == null) {
            mCurTxPowerLevel.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mCurTxPowerLevel.getValue();
    }

    // 백텔 1회 실패 재시도(one coin) 사용 여부. 0 = 미사용, 1 = 사용.
    private final MutableLiveData<Integer> mOneCoin = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataOneCoin() {
        return mOneCoin;
    }

    public void setValueOneCoin(int value) {
        mOneCoin.setValue(value);
    }

    public int getValueOneCoin() {
        if (mOneCoin.getValue() == null) {
            mOneCoin.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mOneCoin.getValue();
    }

    // 백텔 무한 재시도(infinite coin) 사용 여부. one coin 보다 우선한다. 0 = 미사용, 1 = 사용.
    private final MutableLiveData<Integer> mInfiniteCoin = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataInfiniteCoin() {
        return mInfiniteCoin;
    }

    public void setValueInfiniteCoin(int value) {
        mInfiniteCoin.setValue(value);
    }

    public int getValueInfiniteCoin() {
        if (mInfiniteCoin.getValue() == null) {
            mInfiniteCoin.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mInfiniteCoin.getValue();
    }

    // 사운드처리기 0x59 프로토콜 버전. 연결 직후 옵션 255 로 읽어온다.
    private final MutableLiveData<Integer> mRcProtocolMajor = new MutableLiveData<>();
    private final MutableLiveData<Integer> mRcProtocolMinor = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataRcProtocolMajor() {
        return mRcProtocolMajor;
    }

    public void setValueRcProtocolMajor(int value) {
        mRcProtocolMajor.setValue(value);
    }

    public int getValueRcProtocolMajor() {
        if (mRcProtocolMajor.getValue() == null) {
            mRcProtocolMajor.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mRcProtocolMajor.getValue();
    }

    public void setValueRcProtocolMinor(int value) {
        mRcProtocolMinor.setValue(value);
    }

    public int getValueRcProtocolMinor() {
        if (mRcProtocolMinor.getValue() == null) {
            mRcProtocolMinor.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mRcProtocolMinor.getValue();
    }

    // 링크 제어 모드. 0 = 전원 상태 기준, 1 = 백텔 수신 기준.
    private final MutableLiveData<Integer> mLinkCtrlMode = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataLinkCtrlMode() {
        return mLinkCtrlMode;
    }

    public void setValueLinkCtrlMode(int value) {
        mLinkCtrlMode.setValue(value);
    }

    public int getValueLinkCtrlMode() {
        if (mLinkCtrlMode.getValue() == null) {
            mLinkCtrlMode.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mLinkCtrlMode.getValue();
    }

    // Tx 파워 상승 가속. 연속 미수신 시 상승 폭을 키운다. 0 = 미사용, 1 = 사용.
    private final MutableLiveData<Integer> mTxPowerAccel = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataTxPowerAccel() {
        return mTxPowerAccel;
    }

    public void setValueTxPowerAccel(int value) {
        mTxPowerAccel.setValue(value);
    }

    public int getValueTxPowerAccel() {
        if (mTxPowerAccel.getValue() == null) {
            mTxPowerAccel.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mTxPowerAccel.getValue();
    }

    /* 백텔 읽기 직전 전원 안정용 NopStandby 개수. 0 ~ 19.
     * 되읽은 값과 실제 동작 개수가 다를 수 있다. 펌웨어가 프레임 수에 맞춰 매 사이클 잘라내기 때문이다. */
    private final MutableLiveData<Integer> mNopStandbyCount = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataNopStandbyCount() {
        return mNopStandbyCount;
    }

    public void setValueNopStandbyCount(int value) {
        mNopStandbyCount.setValue(value);
    }

    public int getValueNopStandbyCount() {
        if (mNopStandbyCount.getValue() == null) {
            mNopStandbyCount.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mNopStandbyCount.getValue();
    }

    /* 현재 맵의 펄스폭(usec)과 패킷 수(채널당 프레임 수). 둘 다 읽기 전용이다.
     *
     * 앱은 «패킷 수» 로 쓸 수 있는 Nop 표를 찾는다. 펄스폭으로 찾으면 안 된다.
     * nOFm 자극 방식은 패킷 수를 3 으로 고정해 펄스폭과 어긋나기 때문이다. */
    private final MutableLiveData<Integer> mPulseWidth = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataPulseWidth() {
        return mPulseWidth;
    }

    public void setValuePulseWidth(int value) {
        mPulseWidth.setValue(value);
    }

    public int getValuePulseWidth() {
        if (mPulseWidth.getValue() == null) {
            mPulseWidth.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mPulseWidth.getValue();
    }

    private final MutableLiveData<Integer> mFrameNum = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFrameNum() {
        return mFrameNum;
    }

    public void setValueFrameNum(int value) {
        mFrameNum.setValue(value);
    }

    public int getValueFrameNum() {
        if (mFrameNum.getValue() == null) {
            mFrameNum.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mFrameNum.getValue();
    }

    /* 전원 안정 Nop 의 값이 어디서 왔는지. 0 = 패킷 수별 기본값, 1 = 리모콘이 정한 값.
     * 맵이 바뀌어 계산이 다시 돌면 사운드처리기가 0 으로 되돌린다. */
    private final MutableLiveData<Integer> mNopEnable = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataNopEnable() {
        return mNopEnable;
    }

    public void setValueNopEnable(int value) {
        mNopEnable.setValue(value);
    }

    public int getValueNopEnable() {
        if (mNopEnable.getValue() == null) {
            mNopEnable.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mNopEnable.getValue();
    }

    // 자극 전략. 펄스폭과 패킷 수가 어긋나는 이유를 설명하는 데 쓴다.
    private final MutableLiveData<Integer> mStimStrategy = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataStimStrategy() {
        return mStimStrategy;
    }

    public void setValueStimStrategy(int value) {
        mStimStrategy.setValue(value);
    }

    public int getValueStimStrategy() {
        if (mStimStrategy.getValue() == null) {
            mStimStrategy.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mStimStrategy.getValue();
    }

    /* 판별된 사운드처리기 펌웨어 세대. Status.FW_RELEASE_* 값이다.
     * 화면은 이 값을 보고 세대가 모르는 버튼을 잠그고 배지 글자를 바꾼다. */
    private final MutableLiveData<Integer> mFwRelease = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataFwRelease() {
        return mFwRelease;
    }

    public void setValueFwRelease(int value) {
        mFwRelease.setValue(value);
    }

    public int getValueFwRelease() {
        if (mFwRelease.getValue() == null) {
            mFwRelease.setValue(Status.FW_RELEASE_UNKNOWN);
        }

        return mFwRelease.getValue();
    }

    // 링크 백텔 주기. 100msec 단위. 연결 직후 특수 명령(0x59) 옵션 4로 읽어온다.
    private final MutableLiveData<Integer> mBacktelPeriod = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataBacktelPeriod() {
        return mBacktelPeriod;
    }

    public void setValueBacktelPeriod(int value) {
        mBacktelPeriod.setValue(value);
    }

    public int getValueBacktelPeriod() {
        if (mBacktelPeriod.getValue() == null) {
            mBacktelPeriod.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mBacktelPeriod.getValue();
    }

    // 게이팅(묵음) 활성화 상태. 1 = 활성화, 2 = 비활성화. 연결 직후 특수 명령(0x59) 옵션 1로 읽어온다.
    private final MutableLiveData<Integer> mGatingState = new MutableLiveData<>();

    public MutableLiveData<Integer> getLiveDataGatingState() {
        return mGatingState;
    }

    public void setValueGatingState(int value) {
        mGatingState.setValue(value);
    }

    public int getValueGatingState() {
        if (mGatingState.getValue() == null) {
            mGatingState.setValue(PacketInfo.LINK_VALUE_UNKNOWN);
        }

        return mGatingState.getValue();
    }
}
