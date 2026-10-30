package todoc.cochlear.remoteapp.database.devices;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(version = 2, entities = {EntityDevice.class})
public abstract class DatabaseDevice extends RoomDatabase
{
    public static final String DATABASE_NAME = "DB_DEVICE";
    public static final String DATABASE_ENCODING = "PRAGMA encoding='UTF-8'";
    public static final Migration MIGRATION_1_2 = new Migration(1, 2)
    {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db)
        {
            db.execSQL("ALTER TABLE `EntityDevice` ADD COLUMN `isActive` INTEGER NOT NULL DEFAULT 1");
        }
    };

    public abstract DaoDevice daoDevice();
}
