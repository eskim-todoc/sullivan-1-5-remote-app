package todoc.cochlear.remoteapp.database.maps;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;

public class UtilMap
{
    static public final String TAG = "TODOC_" + UtilMap.class.getSimpleName();

    static public UtilMap instance = new UtilMap();

    private DatabaseMap mDatabase;

    // 데이터베이스 닫기
    public void close()
    {
        if (mDatabase != null && mDatabase.isOpen())
        {
            mDatabase.close();
            mDatabase = null;

            Log.d(TAG, "맵 데이터베이스 '" + DatabaseMap.DATABASE_NAME + "'를 닫았습니다.");
        }
        else
        {
            mDatabase = null;
            Log.d(TAG, "맵 데이터베이스 '" + DatabaseMap.DATABASE_NAME + "'가 이미 닫혀있습니다.");
        }
    }

    // 데이터베이스 열기
    public void open(Context context)
    {
        if (mDatabase == null)
        {
            mDatabase = Room.databaseBuilder(context, DatabaseMap.class, DatabaseMap.DATABASE_NAME)
                    .addCallback(new RoomDatabase.Callback()
                    {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db)
                        {
                            super.onCreate(db);
                            db.execSQL(DatabaseMap.DATABASE_ENCODING);
                            Log.d(TAG, "맵 데이터베이스 '" + DatabaseMap.DATABASE_NAME + "'을 생성합니다.");
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        else
        {
            Log.d(TAG, "맵 데이터베이스 '" + DatabaseMap.DATABASE_NAME + "'가 이미 열려있습니다.");
        }
    }

    // 데이터베이스 업데이트
    public void update(EntityMap map)
    {
        if (mDatabase != null)
        {
            mDatabase.daoMap().update(map);
        }
    }

    // 데이터베이스 삭제
    public void delete(EntityMap map)
    {
        if (mDatabase != null)
        {
            mDatabase.daoMap().delete(map);
        }
    }

    // 데이터베이스 추가
    public void insert(EntityMap map)
    {
        if (mDatabase != null)
        {
            mDatabase.daoMap().insert(map);
        }
    }

    // 모든 맵 정보 가져오기
    public List<EntityMap> getAll()
    {
        if (mDatabase != null)
        {
            return mDatabase.daoMap().getAll();
        }

        return null;
    }
}
