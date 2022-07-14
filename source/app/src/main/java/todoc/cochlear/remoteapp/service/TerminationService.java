package todoc.cochlear.remoteapp.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.params.AppParam;

public class TerminationService extends Service
{
    private static final String TAG = "TD2_" + TerminationService.class.getSimpleName();

    private static final String NOTIFICATION_CHANNEL_ID = "TODOC";
    private static final String NOTIFICATION_NAME = "REMOTE";
    public static final String ACTION_START_SERVICE = "START_TERMINATION_SERVICE";

    public TerminationService()
    {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "Received command : ID = " + startId + ", Intent = " + intent);

        if (intent != null)
        {
            if (intent.getAction() != null && intent.getAction().equals(ACTION_START_SERVICE))
            {
                if (!AppParam.getInstance().isTerminationServiceStarted)
                {
                    startForegroundService();
                    AppParam.getInstance().isTerminationServiceStarted = true;
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent)
    {
        //super.onTaskRemoved(rootIntent); // Do not use super method

        Log.d(TAG, "onTaskRemoved() called.");
        stopSelf();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        AppParam.getInstance().isTerminationServiceStarted = false;
        Log.d(TAG, "onDestroy() called.");
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

        Log.d(TAG, "Finish starting foreground service.");
    }
}