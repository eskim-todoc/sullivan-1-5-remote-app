package todoc.cochlear.remoteapp.database.maps;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 1, entities = {EntityMap.class})
public abstract class DatabaseMap extends RoomDatabase
{
    public static final String DATABASE_NAME = "map_info";
    public static final String DATABASE_ENCODING = "PRAGMA encoding='UTF-8'";

    public abstract DaoMap daoMap();
}
