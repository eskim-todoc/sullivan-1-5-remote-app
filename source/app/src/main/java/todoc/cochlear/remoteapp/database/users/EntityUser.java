package todoc.cochlear.remoteapp.database.users;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class EntityUser
{
    static public final String EAR_LEFT = "L";
    static public final String EAR_RIGHT = "R";
    static public final String USER_DEFAULT = "Y";
    static public final String USER_NOT_DEFAULT = "N";

    @PrimaryKey
    @NonNull
    public String name;
    public String ear;
    public String passKey;
    public String defaultUser;

    @Override
    public String toString()
    {
        return "DbUser{" +
                "name='" + name + '\'' +
                ", ear='" + ear + '\'' +
                ", passKey='" + passKey + '\'' +
                ", defaultUser='" + defaultUser + '\'' +
                '}';
    }
}
