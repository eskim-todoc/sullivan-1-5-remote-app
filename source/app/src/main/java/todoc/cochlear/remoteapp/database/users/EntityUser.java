package todoc.cochlear.remoteapp.database.users;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EntityUser
{
    static public final String DELIMITER = "_";
    static public final String EAR_LEFT = "L";
    static public final String EAR_RIGHT = "R";

    /* 착용 위치가 아니라 상태 값이다.
     * 외부기 펌웨어에서 맵 데이터를 공장 초기화하면 착용 위치 값이 F 가 되고,
     * 그 이름으로 광고를 내보낸다. 그 장치에 연결하려면 앱에도 F 사용자가 등록돼 있어야 한다. */
    static public final String EAR_FACTORY_RESET = "F";

    static public final String EAR_LEFT_KR = "왼쪽";
    static public final String EAR_RIGHT_KR = "오른쪽";
    static public final String EAR_FACTORY_RESET_KR = "초기화";
    static public final String USER_DEFAULT = "Y";
    static public final String USER_NOT_DEFAULT = "N";

    @PrimaryKey
    @NonNull
    public String name;
    public String passKey;
    public String nickname;
    public String ear;
    @ColumnInfo(name = "defaultUser")
    public String defaultUser;

    @NonNull
    @Override
    public String toString()
    {
        return "EntityUser{" +
                "name='" + name + '\'' +
                ", passKey='" + passKey + '\'' +
                ", nickname='" + nickname + '\'' +
                ", ear='" + ear + '\'' +
                ", defaultUser='" + defaultUser + '\'' +
                '}';
    }
}
