package todoc.cochlear.remoteapp.database.logs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class UtilLog
{
    static private final String TAG = "TODOC_" + UtilLog.class.getSimpleName();

    static private final int LOG_INDEX_POINTER = 0;
    static private final int LOG_INDEX_FIRST = 1;
    static private final int LOG_INDEX_LAST = 2048;

    static public UtilLog instance = new UtilLog();

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.KOREAN);

    private DatabaseLog mDatabase;

    public UtilLog()
    {
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
    }

    // 현재시간(년/월/일/시/분/초) 획득 메서드
    public String getDate()
    {
        return dateFormat.format(new Date(System.currentTimeMillis()));
    }

    // 데이터베이스 닫기
    public void close()
    {
        if (mDatabase != null && mDatabase.isOpen())
        {
            mDatabase.close();
            mDatabase = null;

            Log.d(TAG, "[로그] 데이터베이스 '" + DatabaseLog.DATABASE_NAME + "'를 닫았습니다.");
        }
        else
        {
            Log.d(TAG, "[로그] 데이터베이스 '" + DatabaseLog.DATABASE_NAME + "'가 이미 닫혀있습니다.");
        }
    }

    // 데이터베이스 열기
    public void open(Context context)
    {
        if (mDatabase == null)
        {
            mDatabase = Room.databaseBuilder(context, DatabaseLog.class, DatabaseLog.DATABASE_NAME)
                    .addCallback(new RoomDatabase.Callback()
                    {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db)
                        {
                            super.onCreate(db);
                            db.execSQL(DatabaseLog.DATABASE_ENCODING);
                            Log.d(TAG, "[로그] 데이터베이스 '" + DatabaseLog.DATABASE_NAME + "'를 열었습니다.");
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        else
        {
            Log.d(TAG, "[로그] 데이터베이스 '" + DatabaseLog.DATABASE_NAME + "'가 이미 열려있습니다.");
        }

        initQueueIndex(); // 데이터베이스 Number ROW 초기화
    }

    // 데이터베이스의 인덱스 초기화
    private void initQueueIndex()
    {
        if (mDatabase != null)
        {
            // 데이터베이스의 Primary Key 0번을 메시지 입력 시 입력할 인덱스로 사용한다.

            EntityLog entityLog = mDatabase.daoLog().getIndex();

            // 저장된 인덱스 포인터 없을 시 포인터를 생성하고 인덱스를 1로 설정
            if (entityLog == null)
            {
                entityLog = new EntityLog();
                entityLog.number = LOG_INDEX_POINTER;
                entityLog.date = getDate();
                entityLog.message = String.valueOf(LOG_INDEX_FIRST);

                mDatabase.daoLog().insert(entityLog);
                Log.d(TAG, "[로그] 인덱스 포인터 정보가 없습니다. 데이터베이스의 " + LOG_INDEX_POINTER + "번 행을 로그를 입력할 인덱스 포인터로 사용합니다.");
            }

            Log.d(TAG, "[로그] {인덱스 = " + entityLog.number + "}, {번호 = " + entityLog.number + "}, {일시 = " + entityLog.date + "}, {메시지 = " + entityLog.message + "}");
        }
    }

    // 데이터베이스에 로그 추가
    public void writeLog(String message)
    {
        if (mDatabase != null)
        {
            EntityLog entityIndex = mDatabase.daoLog().getIndex();

            if (entityIndex == null)
            {
                initQueueIndex();
                entityIndex = mDatabase.daoLog().getIndex();
            }

            int number = Integer.parseInt(entityIndex.message);

            EntityLog entityNew = new EntityLog();
            entityNew.number = number;
            entityNew.date = getDate();
            entityNew.message = message;

            number++;

            if (LOG_INDEX_LAST < number)
            {
                number = LOG_INDEX_FIRST;
            }

            entityIndex.message = String.valueOf(number);

            mDatabase.daoLog().insert(entityNew); // 새 메시지 삽입
            mDatabase.daoLog().insert(entityIndex); // 인덱스 업데이트 (OnConflictStrategy.REPLACE 라서 업데이트 대신 추가를 사용)

            Log.d(TAG, "[로그] 로그 입력 : " + message);
        }
    }

    // 데이터베이스에 저장된 모든 로그 읽기
    public List<EntityLog> readAllLogs()
    {
        if (mDatabase != null)
        {
            return mDatabase.daoLog().findAll();
        }

        return null;
    }

    // 데이터베이스에 저장된 모든 로그 출력
    public void printAllLogs()
    {
        if (mDatabase != null)
        {
            Log.d(TAG, "[로그] 로그 전체 출력 : ");

            List<EntityLog> entityLogs = mDatabase.daoLog().findAll();

            for (int i = 0; i < entityLogs.size(); i++)
            {
                Log.d(TAG, "[로그] {인덱스 = " + entityLogs.get(i).number +
                        "}, {번호 = " + entityLogs.get(i).number +
                        "}, {일시 = " + entityLogs.get(i).date +
                        "}, {메시지 = " + entityLogs.get(i).message + "}");
            }
        }
    }
}
