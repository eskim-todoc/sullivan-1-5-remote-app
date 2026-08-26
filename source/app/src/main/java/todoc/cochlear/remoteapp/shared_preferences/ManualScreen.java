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

    /* 앱을 켤 때 사용설명서를 먼저 띄울지.
     *
     * 기본값이 «띄우지 않음» 이다. 예전에는 첫 실행마다 설명서가 먼저 나왔는데,
     * 설명서는 설정 화면에서 언제든 열 수 있으므로 시작 화면을 막을 이유가 없다.
     *
     * 기능을 없애지는 않았다. 설명서 화면의 «다시 보지 않기» 체크는 그대로 이 값을 쓴다. */
    public boolean isEnabled()
    {
        return mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(KEY_FOR_ENABLE, false);
    }

    public void setEnable(boolean enable)
    {
        mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_FOR_ENABLE, enable).apply();
    }
}
