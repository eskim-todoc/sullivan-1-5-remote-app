package todoc.cochlear.remoteapp.logging;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(version = 1, entities = {Logging.class})
public abstract class LoggingDatabase extends RoomDatabase
{
    public static final String DATABASE_NAME = "remote_log";
    public static final String DATABASE_ENCODING = "PRAGMA encoding='UTF-8'";

    public abstract LoggingDao loggingDao();
}
