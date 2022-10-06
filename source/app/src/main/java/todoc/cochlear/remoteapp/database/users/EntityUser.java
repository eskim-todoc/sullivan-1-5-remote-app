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
    static public final String EAR_LEFT_KR = "왼쪽";
    static public final String EAR_RIGHT_KR = "오른쪽";
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
