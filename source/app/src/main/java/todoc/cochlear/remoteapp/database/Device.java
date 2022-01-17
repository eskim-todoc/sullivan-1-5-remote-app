package todoc.cochlear.remoteapp.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 클래스 이름이 테이블 이름이다.
 * SQLite는 영어 대/소문자 구분을 하지 않는다.
 */
@Entity
public class Device
{
    /**
     * 필드 정의.
     * 멤버 변수들이 자동적으로 데이터베이스의 필드로 설정된다.
     */
    @PrimaryKey
    @NonNull
    // 사운드처리기 일련번호 (Primary Key)
    public String deviceSerial;

    // 사운드처리기 MAC 주소
    public String deviceMacAddress;

    // 사운드처리기 보안코드
    public String devicePassword;

    // 사운드처리기 장치 이름
    public String deviceName;

    // 사운드처리기 모델
    public String deviceModel;

    // 사운드처리기 펌웨어 종류(모델)
    public String deviceFwType;

    // 사운드처리기 펌웨어 버전
    public String deviceFwVersion;

    // 맵 일자
    public String mapDate;

    // 맵 개수
    public String mapCount;

    // 내부기 방향
    public String implantDirection;

    // 내부기 사용자 이름
    public String implantUserName;

    /**
     * Getters and setters
     */
    public String getDeviceSerial()
    {
        return deviceSerial;
    }

    public void setDeviceSerial(String deviceSerial)
    {
        this.deviceSerial = deviceSerial;
    }

    public String getDevicePassword()
    {
        return devicePassword;
    }

    public void setDevicePassword(String devicePassword)
    {
        this.devicePassword = devicePassword;
    }

    public String getDeviceMacAddress()
    {
        return deviceMacAddress;
    }

    public void setDeviceMacAddress(String deviceMacAddress)
    {
        this.deviceMacAddress = deviceMacAddress;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    public String getDeviceModel()
    {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel)
    {
        this.deviceModel = deviceModel;
    }

    public String getDeviceFwType()
    {
        return deviceFwType;
    }

    public void setDeviceFwType(String deviceFwType)
    {
        this.deviceFwType = deviceFwType;
    }

    public String getDeviceFwVersion()
    {
        return deviceFwVersion;
    }

    public void setDeviceFwVersion(String deviceFwVersion)
    {
        this.deviceFwVersion = deviceFwVersion;
    }

    public String getMapDate()
    {
        return mapDate;
    }

    public void setMapDate(String mapDate)
    {
        this.mapDate = mapDate;
    }

    public String getMapCount()
    {
        return mapCount;
    }

    public void setMapCount(String mapCount)
    {
        this.mapCount = mapCount;
    }

    public String getImplantDirection()
    {
        return implantDirection;
    }

    public void setImplantDirection(String implantDirection)
    {
        this.implantDirection = implantDirection;
    }

    public String getImplantUserName()
    {
        return implantUserName;
    }

    public void setImplantUserName(String implantUserName)
    {
        this.implantUserName = implantUserName;
    }

    @Override
    public String toString()
    {
        return "Device{" +
                "deviceSerial='" + deviceSerial + '\'' +
                ", deviceMacAddress='" + deviceMacAddress + '\'' +
                ", devicePassword='" + devicePassword + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", deviceFwType='" + deviceFwType + '\'' +
                ", deviceFwVersion='" + deviceFwVersion + '\'' +
                ", mapDate='" + mapDate + '\'' +
                ", mapCount='" + mapCount + '\'' +
                ", implantDirection='" + implantDirection + '\'' +
                ", implantUserName='" + implantUserName + '\'' +
                '}';
    }
}
