package todoc.cochlear.remoteapp.shared_preferences;

import android.content.Context;
import android.util.Log;

import java.util.regex.Pattern;

public class AppPreferences
{
      private static final String TAG = "TD2_" + AppPreferences.class.getSimpleName();

      // Defines instance and getter.
      private final static AppPreferences mInstance = new AppPreferences();

      private final static String NAME = "sharedPreferences";
      private final static String AUTO_CONNECTION = "autoConnection";
      private final static String MANUAL = "manual";
      private final static String APPLOCK_PASSWORD = "applock_password";
      private final static String IS_APPLOCK_SCREEN_ENABLED = "is_applock_screen_enabled";

      public static AppPreferences getInstance()
      {
            return mInstance;
      }

      public boolean isThereAppLockPassword(Context context)
      {
            String password = context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(APPLOCK_PASSWORD, "");
            boolean matches = Pattern.matches("[0-9][0-9][0-9][0-9]", password);

            Log.d(TAG, "Is there password? = " + matches);
            return matches;
      }

      public boolean isAppLockPasswordCorrect(Context context, String password)
      {
            String savedPassword;
            boolean ret;

            savedPassword = context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(APPLOCK_PASSWORD, "");
            ret = savedPassword.equals(password);

            Log.d(TAG, "Is password correct? = " + ret);
            return ret;
      }

      public void setNewAppLockPassword(Context context, String newPassword)
      {
            Log.d(TAG, "Set new password = " + newPassword);
            (((context.getSharedPreferences(NAME, Context.MODE_PRIVATE)).edit()).putString(APPLOCK_PASSWORD, newPassword)).apply();
      }

      public boolean isAutoConnectionEnabled(Context context)
      {
            boolean isEnabled = context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(AUTO_CONNECTION, true);
            Log.d(TAG, "Is auto connection enabled? = " + isEnabled);

            return isEnabled;
      }

      public void setAutoConnectionToEnabled(Context context, boolean enabled)
      {
            Log.d(TAG, "Set auto connection enabled = " + enabled);
            (((context.getSharedPreferences(NAME, Context.MODE_PRIVATE)).edit()).putBoolean(AUTO_CONNECTION, enabled)).apply();
      }

      public boolean isManualEnabled(Context context)
      {
            boolean isEnabled = true;

            try
            {
                  isEnabled = context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(MANUAL, true);
            }
            catch (ClassCastException e)
            {
                  Log.d(TAG, "Exception! " + e.getMessage());
            }

            Log.d(TAG, "Is manual enabled? = " + isEnabled);

            return isEnabled;
      }

      public void setManualToEnabled(Context context, boolean enabled)
      {
            Log.d(TAG, "Set manual enabled = " + enabled);
            (((context.getSharedPreferences(NAME, Context.MODE_PRIVATE)).edit()).putBoolean(MANUAL, enabled)).apply();
      }

      public boolean isApplockScreenOptionEnabled(Context context)
      {
            boolean isEnabled = true;

            try
            {
                  isEnabled = context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(IS_APPLOCK_SCREEN_ENABLED, true);
            }
            catch (ClassCastException e)
            {
                  Log.d(TAG, "Exception! " + e.getMessage());
            }

            Log.d(TAG, "Is applock screen enabled? = " + isEnabled);

            return isEnabled;
      }

      public void setApplockScreenToEnabled(Context context, boolean enabled)
      {
            Log.d(TAG, "Set applock screen = " + enabled);
            (((context.getSharedPreferences(NAME, Context.MODE_PRIVATE)).edit()).putBoolean(IS_APPLOCK_SCREEN_ENABLED, enabled)).apply();
      }
}
