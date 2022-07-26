package todoc.cochlear.remoteapp.database.devices;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;

public class UtilDevice
{
    static private final String TAG = "TODOC_" + UtilDevice.class.getSimpleName();

    static public UtilDevice instance = new UtilDevice();

    private DatabaseDevice mDatabase;

    // 데이터베이스 닫기
    public void close()
    {
        if (mDatabase != null && mDatabase.isOpen())
        {
            mDatabase.close();
            mDatabase = null;

            Log.d(TAG, "기기 데이터베이스 '" + DatabaseDevice.DATABASE_NAME + "'를 닫았습니다.");
        }
        else
        {
            Log.d(TAG, "기기 데이터베이스 '" + DatabaseDevice.DATABASE_NAME + "'가 이미 닫혀있습니다.");
        }
    }

    // 데이터베이스 열기
    public void open(Context context)
    {
        if (mDatabase == null)
        {
            mDatabase = Room.databaseBuilder(context, DatabaseDevice.class, DatabaseDevice.DATABASE_NAME)
                    .addCallback(new RoomDatabase.Callback()
                    {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db)
                        {
                            super.onCreate(db);
                            db.execSQL(DatabaseDevice.DATABASE_ENCODING);
                            Log.d(TAG, "기기 데이터베이스 '" + DatabaseDevice.DATABASE_NAME + "'을 열었습니다.");
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        else
        {
            Log.d(TAG, "기기 데이터베이스 '" + DatabaseDevice.DATABASE_NAME + "'가 이미 열려있습니다.");
        }
    }

    // 데이터베이스 업데이트
    public void update(EntityDevice device)
    {
        if (mDatabase != null)
        {
            mDatabase.daoDevice().update(device);
        }
    }

    // 데이터베이스 삭제
    public void delete(EntityDevice device)
    {
        if (mDatabase != null)
        {
            mDatabase.daoDevice().delete(device);
        }
    }

    // 데이터베이스 추가
    public void insert(EntityDevice device)
    {
        if (mDatabase != null)
        {
            mDatabase.daoDevice().insert(device);
        }
    }

    // 모든 기기 가져오기
    public List<EntityDevice> getDevices()
    {
        if (mDatabase != null)
        {
            return mDatabase.daoDevice().findAll();
        }

        return null;
    }

    // 제품번호와 일치하는 기기 가져오기
    public EntityDevice getDeviceBySerialNumber(String serialNumber)
    {
        if (mDatabase != null)
        {
            return mDatabase.daoDevice().getDeviceBySerialNumber(serialNumber);
        }

        return null;
    }
}