package todoc.cochlear.remoteapp.database.users;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 1, entities = {EntityUser.class})
public abstract class DatabaseUser extends RoomDatabase
{
    public static final String DATABASE_NAME = "user_info";
    public static final String DATABASE_ENCODING = "PRAGMA encoding='UTF-8';";

    public abstract DaoUser daoUser();
}
