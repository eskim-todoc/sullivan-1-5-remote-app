package todoc.cochlear.remoteapp.shared_preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import todoc.cochlear.remoteapp.fragment.HomeFragment;

public class AppPreferences
{
    private static final String TAG = "TD2_" + AppPreferences.class.getSimpleName();

    // Defines instance and getter.
    private final static AppPreferences mInstance = new AppPreferences();

    private final static String NAME = "sharedPreferences";
    private final static String AUTO_CONNECTION = "autoConnection";
    private final static String MANUAL = "manual";
    private final static String PASSWORD = "password";

    public static AppPreferences getInstance()
    {
        return mInstance;
    }

    public boolean isTherePassword(Context context)
    {
        String password;
        boolean ret;

        password = context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(PASSWORD, "");
        ret = password.equals("");

        Log.d(TAG, "Is there password? = " + ret);
        return !ret;
    }

    public boolean isPasswordCorrect(Context context, String password)
    {
        String savedPassword;
        boolean ret;

        savedPassword = context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(PASSWORD, "");
        ret = savedPassword.equals(password);

        Log.d(TAG, "Is password correct? = " + ret);
        return ret;
    }

    public void setNewPassword(Context context, String newPassword)
    {
        Log.d(TAG, "Set new password = " + newPassword);
        (((context.getSharedPreferences(NAME, Context.MODE_PRIVATE)).edit()).putString(PASSWORD, newPassword)).apply();
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
}
