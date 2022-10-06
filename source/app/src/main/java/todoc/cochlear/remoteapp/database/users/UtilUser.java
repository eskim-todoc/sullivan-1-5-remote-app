package todoc.cochlear.remoteapp.database.users;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;

public class UtilUser
{
    static private final String TAG = "TODOC_" + UtilUser.class.getSimpleName();

    static public UtilUser instance = new UtilUser();

    private DatabaseUser mDatabase;

    // 데이터베이스 닫기
    public void close()
    {
        if (mDatabase != null && mDatabase.isOpen())
        {
            mDatabase.close();
            mDatabase = null;

            Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'를 닫았습니다.");
        }
        else
        {
            Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'가 이미 닫혀있습니다.");
        }
    }

    // 데이터베이스 열기
    public void open(Context context)
    {
        if (mDatabase == null)
        {
            mDatabase = Room.databaseBuilder(context, DatabaseUser.class, DatabaseUser.DATABASE_NAME)
                    .addCallback(new RoomDatabase.Callback()
                    {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db)
                        {
                            super.onCreate(db);
                            db.execSQL(DatabaseUser.DATABASE_ENCODING);
                            Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'을 열었습니다.");
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        else
        {
            Log.d(TAG, "사용자 데이터베이스 '" + DatabaseUser.DATABASE_NAME + "'가 이미 열려있습니다.");
        }
    }

    // 데이터베이스 업데이트
    public void update(EntityUser user)
    {
        if (mDatabase != null)
        {
            mDatabase.daoUser().update(user);
        }
    }

    // 데이터베이스 삭제
    public void delete(EntityUser user)
    {
        if (mDatabase != null)
        {
            mDatabase.daoUser().delete(user);
        }
    }

    // 데이터베이스 추가
    public void insert(EntityUser user)
    {
        if (mDatabase != null)
        {
            mDatabase.daoUser().insert(user);
        }
    }

    // 기본사용자 가져오기
    public EntityUser getDefaultUser()
    {
        if (mDatabase != null)
        {
            return mDatabase.daoUser().getDefaultUser();
        }

        return null;
    }

    // 모든 사용자 가져오기
    public List<EntityUser> getUsers()
    {
        if (mDatabase != null)
        {
            return mDatabase.daoUser().findAll();
        }

        return null;
    }

    // 이름과 일치하는 사용자 가져오기
    public EntityUser getUserByName(String name)
    {
        if (mDatabase != null)
        {
            return mDatabase.daoUser().getUserByName(name);
        }

        return null;
    }

    // 착용위치를 제외한 이름 정보만 얻어오기
    static public String getNameOnly(String nameWithEar)
    {
        return nameWithEar.substring(0, nameWithEar.length() - (EntityUser.EAR_LEFT.length() + EntityUser.DELIMITER.length()));
    }

    // 착용 위치에 대한 한극 문자열 얻어오기
    static public String getEarKorean(String ear)
    {
        if (ear.equals(EntityUser.EAR_LEFT))
        {
            return EntityUser.EAR_LEFT_KR;
        }
        else
        {
            return EntityUser.EAR_RIGHT_KR;
        }
    }

    static public void copyData(EntityUser dst, EntityUser src)
    {
        if (src != null && dst != null)
        {
            dst.name = src.name;
            dst.ear = src.ear;
            dst.nickname = src.nickname;
            dst.passKey = src.passKey;
            dst.defaultUser = src.defaultUser;
        }
    }
}
