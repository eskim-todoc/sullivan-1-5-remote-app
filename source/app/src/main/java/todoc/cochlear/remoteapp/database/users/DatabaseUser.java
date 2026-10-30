package todoc.cochlear.remoteapp.database.users;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(version = 2, entities = {EntityUser.class})
public abstract class DatabaseUser extends RoomDatabase
{
    public static final String DATABASE_NAME = "user_info";
    public static final String DATABASE_ENCODING = "PRAGMA encoding='UTF-8'";

    public static final Migration MIGRATION_1_2 = new Migration(1, 2)
    {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database)
        {
            database.execSQL("ALTER TABLE EntityUser ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1");
        }
    };

    public abstract DaoUser daoUser();
}
