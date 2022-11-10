package todoc.cochlear.remoteapp.database.logs;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 1, entities = {EntityLog.class})
public abstract class DatabaseLog extends RoomDatabase
{
    public static final String DATABASE_NAME = "remote_log";
    public static final String DATABASE_ENCODING = "PRAGMA encoding='UTF-8'";

    public abstract DaoLog daoLog();
}
