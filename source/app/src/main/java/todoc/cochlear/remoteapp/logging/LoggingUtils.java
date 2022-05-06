package todoc.cochlear.remoteapp.logging;

import android.content.Context;
import android.util.Log;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import todoc.cochlear.remoteapp.list.LoggingItemAdapter;

public class LoggingUtils extends AppCompatActivity
{
    private static final String TAG = "TD2_" + LoggingUtils.class.getSimpleName();

    private static final int LOG_SIZE_LIMIT = 1024;

    private final static LoggingUtils mInstance = new LoggingUtils();
    public static LoggingDatabase mLoggingDatabase;

    static SimpleDateFormat mDateFormat;

    public int mSecretNumberCount;
    public LoggingItemAdapter mLoggingItemAdapter;

    public static LoggingUtils getInstance()
    {
        return mInstance;
    }

    public LoggingUtils()
    {
        if (mDateFormat == null)
        {
            mDateFormat = new SimpleDateFormat("yyy-MM-dd hh:mm:ss.SSS", Locale.KOREA);
        }

        mSecretNumberCount = 0;
        mLoggingItemAdapter = null;
    }

    public String getCurrentDate()
    {
        return mDateFormat.format(new Date(System.currentTimeMillis()));
    }

    //
    // 로깅 데이터베이스 닫기
    //
    public void closeLoggingDatabase(Context context)
    {
        if (mLoggingDatabase != null)
        {
            if (mLoggingDatabase.isOpen())
            {
                mLoggingDatabase.close();
                mLoggingDatabase = null;
            }
        }

        Log.d(TAG, "Database '" + LoggingDatabase.DATABASE_NAME + "' has been closed.");
    }

    //
    // 로깅 데이터베이스 열기
    //
    public void openLoggingDatabase(Context context)
    {
        if (mLoggingDatabase == null)
        {
            mLoggingDatabase = Room.databaseBuilder(context, LoggingDatabase.class, LoggingDatabase.DATABASE_NAME)
                    .addCallback(mCallback)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }

        Log.d(TAG, "Database '" + LoggingDatabase.DATABASE_NAME + "' has been opened.");

        initRowIndexPointer(); // 데이터베이스 Number ROW 초기화
    }

    //
    // 로깅 데이베이스 콜백
    //
    private final RoomDatabase.Callback mCallback = new RoomDatabase.Callback()
    {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db)
        {
            super.onCreate(db);
            db.execSQL(LoggingDatabase.DATABASE_ENCODING);
        }
    };

    //
    // 데이터베이스의 ROW 인덱스 포인터 초기화
    //
    private void initRowIndexPointer()
    {
        if (mLoggingDatabase != null)
        {
            // 데이터베이스 ROW 인덱스 포인터 초기화
            Logging logging = mLoggingDatabase.loggingDao().getRowByNumber(0);

            if (logging == null) // 저장된 번호 데이터 없을 시 번호 1로 설정
            {
                logging = new Logging();
                logging.setNumber(0);
                logging.setDate(getCurrentDate());
                logging.setMessage("1");

                mLoggingDatabase.loggingDao().insert(logging);
                Log.d(TAG, "There is no Row Index Pointer. So, initialize the Row Index Pointer to 0 into the Database.");
            }

            Log.d(TAG, "Database Row Index Pointer = " + logging.getNumber() + ", " + logging.getDate() + ", " + logging.getMessage() + "!");
        }
    }

    public void writeMessage(String message)
    {
        if (mLoggingDatabase != null)
        {
            Logging numberLogging = mLoggingDatabase.loggingDao().getRowByNumber(0);

            if (numberLogging == null)
            {
                initRowIndexPointer();
                numberLogging = mLoggingDatabase.loggingDao().getRowByNumber(0);
            }

            int number = Integer.parseInt(numberLogging.getMessage());

            Logging newLogging = new Logging();
            newLogging.setNumber(number);
            newLogging.setDate(getCurrentDate());
            newLogging.setMessage(message);

            number++;
            if (number > LOG_SIZE_LIMIT)
            {
                number = 1;
            }

            numberLogging.setMessage("" + number);

            mLoggingDatabase.loggingDao().insert(newLogging);
            mLoggingDatabase.loggingDao().insert(numberLogging);
        }
    }

    public void printAllMessages()
    {
        if (mLoggingDatabase != null)
        {
            List<Logging> loggings = mLoggingDatabase.loggingDao().findAll();

            for (int i = 0; i < loggings.size(); i++)
            {
                Log.d(TAG, "Logging{" + i + "} = "
                        + loggings.get(i).getNumber() + ", "
                        + loggings.get(i).getDate() + ", "
                        + loggings.get(i).getMessage());
            }
        }
    }

    public static final int LOGGING_DEVICE_CONNECTED = 1;
    public static final int LOGGING_DEVICE_DISCONNECTED = 2;
    public static final int LOGGING_DEVICE_REGISTERED = 3;
    public static final int LOGGING_DEVICE_REMOVED = 4;
    public static final int LOGGING_BOND_BONDED = 5;
    public static final int LOGGING_BOND_REMOVED = 6;
    public static final int LOGGING_VALUE_STIMULATION = 20;
    public static final int LOGGING_VALUE_LED = 21;
    public static final int LOGGING_VALUE_TELECOIL = 22;
    public static final int LOGGING_VALUE_POWER_MODE = 23;
    public static final int LOGGING_VALUE_PROGRAM = 24;
    public static final int LOGGING_VALUE_SENSITIVITY = 25;
    public static final int LOGGING_VALUE_VOLUME = 26;
    public static final int LOGGING_VALUE_STATUS = 27;
    public static final int LOGGING_INIT_ALL_FOR_APP = 50;

    public String typeMessage(int type)
    {
        String ret;

        switch (type)
        {
            case LOGGING_DEVICE_CONNECTED:
                ret = "DEVICE_CONNECTED";
                break;
            case LOGGING_DEVICE_DISCONNECTED:
                ret = "DEVICE_DISCONNECTED";
                break;
            case LOGGING_DEVICE_REGISTERED:
                ret = "DEVICE_REGISTERED";
                break;
            case LOGGING_DEVICE_REMOVED:
                ret = "DEVICE_REMOVED";
                break;
            case LOGGING_BOND_BONDED:
                ret = "BOND_BONDED";
                break;
            case LOGGING_BOND_REMOVED:
                ret = "BOND_REMOVED";
                break;
            case LOGGING_VALUE_STIMULATION:
                ret = "VALUE_STIMULATION";
                break;
            case LOGGING_VALUE_LED:
                ret = "VALUE_LED";
                break;
            case LOGGING_VALUE_TELECOIL:
                ret = "VALUE_TELECOIL";
                break;
            case LOGGING_VALUE_POWER_MODE:
                ret = "VALUE_POWER_MODE";
                break;
            case LOGGING_VALUE_PROGRAM:
                ret = "VALUE_PROGRAM";
                break;
            case LOGGING_VALUE_SENSITIVITY:
                ret = "VALUE_SENSITIVITY";
                break;
            case LOGGING_VALUE_VOLUME:
                ret = "VALUE_VOLUME";
                break;
            case LOGGING_VALUE_STATUS:
                ret = "VALUE_STATUS";
                break;
            case LOGGING_INIT_ALL_FOR_APP:
                ret = "NIT_ALL_FOR_APP";
                break;
            default:
                ret = "NO_TYPE";
                break;
        }

        return ret;
    }

    public void printLoggingScreen(ListView listView)
    {
        if (mLoggingDatabase == null) // If there is no database for logging, be returned immediately.
        {
            return;
        }

        if (mLoggingItemAdapter == null) // If there is no adapter for list view, declare new adapter.
        {
            mLoggingItemAdapter = new LoggingItemAdapter();
        }

        listView.setAdapter(mLoggingItemAdapter);

        List<Logging> logs = mLoggingDatabase.loggingDao().findAll();

        if (logs != null) // No log data.
        {
            int size = logs.size();

            switch (size)
            {
                case 0:
                case 1:
                {
                    Log.d(TAG, "There is no logs being saved.");
                }
                break;

                case 2:
                {
                    Log.d(TAG, "There is one log. => 1");

                    String date = logs.get(1).getDate();
                    String message = logs.get(1).getMessage();

                    mLoggingItemAdapter.addItem(date, message);

                    Log.d(TAG, "Date: " + date + ", Message: " + message);

                }
                break;

                default:
                {
                    Log.d(TAG, "There are multiple logs. => " + (size - 1));

                    int oldestNumber = 1;

                    Log.d(TAG, "Try to find database number for oldest log's date.");

                    // find oldest date
                    for (int i = 1; i < size - 1; i++)
                    {
                        String front = logs.get(i).getDate();
                        String back = logs.get(i + 1).getDate();

                        Log.d(TAG, "Log(" + i + "){" + front + "} vs Log(" + (i + 1) + "){" + back + "}");

                        if (0 < front.compareTo(back))
                        {
                            oldestNumber = i + 1;
                            break;
                        }
                    }

                    Log.d(TAG, "The oldest log's database number = " + oldestNumber);

                    // print from oldest log data to last log data on database.
                    for (int i = oldestNumber; i < size; i++)
                    {
                        String date = logs.get(i).getDate();
                        String message = logs.get(i).getMessage();

                        mLoggingItemAdapter.addItem(date, message);

                        Log.d(TAG, "Date: " + date + ", Message: " + message);
                    }

                    // print from first log data to before oldest log data.
                    for (int i = 1; i < oldestNumber; i++)
                    {
                        String date = logs.get(i).getDate();
                        String message = logs.get(i).getMessage();

                        mLoggingItemAdapter.addItem(date, message);

                        Log.d(TAG, "Date: " + date + ", Message: " + message);
                    }
                }
                break;
            } // end, switch
        } //end, if
    } // end, printLoggingScreen()

    public void clearLoggingScreen()
    {
        if (mLoggingItemAdapter != null)
        {
            mLoggingItemAdapter.clearAllItems();
        }
    }
}
