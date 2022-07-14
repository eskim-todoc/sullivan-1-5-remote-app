package todoc.cochlear.remoteapp.database.devices;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 1, entities = {EntityDevice.class})
public abstract class DatabaseDevices extends RoomDatabase
{
    public static final String DATABASE_NAME = "DB_DEVICE";
    public static final String DATABASE_ENCODING = "PRAGMA encoding='UTF-8'";

    public abstract DaoDevices daoDevices();
}
