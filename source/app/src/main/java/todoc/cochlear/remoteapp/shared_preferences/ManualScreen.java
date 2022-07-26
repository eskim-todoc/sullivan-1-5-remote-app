package todoc.cochlear.remoteapp.shared_preferences;

import android.content.Context;

public class ManualScreen
{
    static private final String SHARED_PREFERENCES_NAME = "MANUAL_SCREEN";
    static private final String KEY_FOR_ENABLE = "ENABLE";

    private final Context mContext;

    public ManualScreen(Context context)
    {
        mContext = context;
    }

    public boolean isEnabled()
    {
        return mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(KEY_FOR_ENABLE, true);
    }

    public void setEnable(boolean enable)
    {
        mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_FOR_ENABLE, enable).apply();
    }
}
