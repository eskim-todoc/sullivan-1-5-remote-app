package todoc.cochlear.remoteapp.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 1, entities = {Device.class})
public abstract class AppDatabase extends RoomDatabase
{
    public static final String DATABASE_NAME = "remote_app";
    public static final String DATABASE_ENCODING = "PRAGMA encoding='UTF-8';";

    public abstract DeviceDao deviceDao();
}
