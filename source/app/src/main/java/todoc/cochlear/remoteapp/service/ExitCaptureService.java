package todoc.cochlear.remoteapp.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.params.Status;

public class ExitCaptureService extends Service
{
    private static final String TAG = "TODOC_" + ExitCaptureService.class.getSimpleName();

    static private final String NOTIFICATION_CHANNEL_ID = "TODOC";
    static private final String NOTIFICATION_NAME = "REMOTE_CONTROL";
    static public final String ACTION_START_SERVICE = "START_EXIT_CAPTURE_SERVICE";

    public ExitCaptureService()
    {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "강제 종료 포착 서비스가 수신한 액션 -> Id = " + startId + ", Intent = " + intent);

        if (intent != null && intent.getAction() != null)
        {
            if (intent.getAction().equals(ACTION_START_SERVICE))
            {
                startForegroundService();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        //super.onTaskRemoved(rootIntent); // Do not use super method


        // TD2-SW-RC-UNIT-Test-ID-60 [포착 서비스 감지 유닛] 순서[1] 시작.
        /*
        {
            Log.d(TAG, "Exit capture service performed.");
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-60 [포착 서비스 감지 유닛] 순서[1] 끝.


        Log.d(TAG, "강제 종료 감지 서비스에서 태스크 삭제가 감지되었습니다. -> onTaskRemoved()");
        stopSelf();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        Status.instance().exitCaptureState = Status.EXIT_CAPTURE_SERVICE_STATE_STOPPED;
        Log.d(TAG, "강제 종료 포착 서비스를 종료합니다. -> onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void startForegroundService()
    {
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.service_custom_notification);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(R.drawable.ic_notification_small_24);
        builder.setCustomContentView(notificationLayout);
        builder.setShowWhen(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_LOW));
        }

        startForeground(1, builder.build());

        Status.instance().exitCaptureState = Status.EXIT_CAPTURE_SERVICE_STATE_STARTED;
        Log.d(TAG, "강제 종료 포착 서비스를 포그라운드 서비스로 등록했습니다.");
    }
}