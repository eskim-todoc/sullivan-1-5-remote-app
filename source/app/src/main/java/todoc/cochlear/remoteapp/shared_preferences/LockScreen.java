package todoc.cochlear.remoteapp.shared_preferences;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.fragment.RemoteControlFragment;
import todoc.cochlear.remoteapp.params.Status;

public class LockScreen
{
    static private final String TAG = "TODOC_" + LockScreen.class.getSimpleName();

    static private final String SHARED_PREFERENCES_NAME = "LOCK_SCREEN";

    static private final String KEY_FOR_PASSWORD = "PASSWORD";
    static private final String KEY_FOR_ENABLE = "ENABLE";

    static private final int VALUE_LENGTH = 4;

    static public final int STATE_REGISTER = 0;
    static public final int STATE_COMPARE = 1;
    static public final int STATE_DECODE = 2;

    public int mState;
    public int mPasswordCounter;
    public int[] mPassword;
    public int mPasswordCompareCounter;
    public int[] mPasswordCompare;

    public String mPasswordString;
    public String mPasswordCompareString;

    private final Context mContext;
    private final MainActivity mMainActivity;
    private final ActivityMainBinding mBinding;

    public LockScreen(Context context, MainActivity activity, ActivityMainBinding binding)
    {
        mContext = context;
        mMainActivity = activity;
        mBinding = binding;
    }

    public void resume()
    {
        Log.v(TAG, "resume(" + mContext.toString() + ", " + mBinding.toString() + ") called.");

        bufferInit();
        stateInit();
        updateCircle(0);
        updateMessage();
        enableScreen(isEnabled());
    }

    public void bufferInit()
    {
        Log.v(TAG, "잠금화면 비밀번호 버퍼 초기화.");

        if (mPassword == null)
        {
            mPassword = new int[VALUE_LENGTH];
        }

        if (mPasswordCompare == null)
        {
            mPasswordCompare = new int[VALUE_LENGTH];
        }

        for (int i = 0; i < VALUE_LENGTH; i++)
        {
            mPassword[i] = 0;
            mPasswordCompare[i] = 0;
        }

        mPasswordCounter = 0;
        mPasswordCompareCounter = 0;
    }

    public void stateInit()
    {
        Log.v(TAG, "stateInit() called.");

        if (readPassword() == null)
        {
            mState = LockScreen.STATE_REGISTER;
        }
        else
        {
            mState = LockScreen.STATE_DECODE;
        }
    }

    public boolean isEnabled()
    {
        return mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(KEY_FOR_ENABLE, true);
    }

    public void setEnable(Context context, boolean enable)
    {
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_FOR_ENABLE, enable).apply();
        Log.d(TAG, "Lock Screen Enable state = " + enable + ".");
    }

    public void erasePassword()
    {
        Log.v(TAG, "Erase the lock screen password.");
        mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().putString(KEY_FOR_PASSWORD, null).apply();
    }

    public void writePassword(String password)
    {
        Log.v(TAG, "writePassword() called.");

        mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().putString(KEY_FOR_PASSWORD, password).apply();
    }

    public String readPassword()
    {
        Log.v(TAG, "readPassword() called.");

        String password = mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getString(KEY_FOR_PASSWORD, null);

        if (password != null && password.length() == VALUE_LENGTH)
        {
            return password;
        }

        return null;
    }

    public void updateMessage()
    {
        Log.v(TAG, "updateMessage() called.");

        if (mState == STATE_REGISTER)
        {
            mBinding.lockScreenInfo.setText(mContext.getString(R.string.lock_screen_register_password));
        }
        else if (mState == STATE_COMPARE)
        {
            mBinding.lockScreenInfo.setText(mContext.getString(R.string.lock_screen_compare_password));
        }
        else if (mState == STATE_DECODE)
        {
            mBinding.lockScreenInfo.setText(mContext.getString(R.string.lock_screen_decode_password));
        }
        else
        {
            mBinding.lockScreenInfo.setText(mContext.getString(R.string.lock_screen_invalid_state));
        }
    }

    public void enableScreen(boolean enable)
    {
        Log.v(TAG, "enableScreen(" + enable + ") called.");

        if (enable)
        {
            if (Status.instance().lockScreenState == Status.LOCK_SCREEN_STATE_TEMPORARY_UNLOCK)
            {
                Status.instance().lockScreenState = Status.LOCK_SCREEN_STATE_LOCK;
                return;
            }

            mBinding.lockScreen.setVisibility(View.VISIBLE);

            Fragment fragment = mMainActivity.getSupportFragmentManager().findFragmentById(R.id.frame);
            if (fragment instanceof RemoteControlFragment)
            {
                AlertDialog dialog = ((RemoteControlFragment) fragment).mDialog;

                if (dialog != null)
                {
                    dialog.dismiss();
                    ((RemoteControlFragment) fragment).mDialog = null;
                }
            }
        }
        else
        {
            mBinding.lockScreen.setVisibility(View.GONE);

            Fragment fragment = mMainActivity.getSupportFragmentManager().findFragmentById(R.id.frame);
            if (fragment instanceof RemoteControlFragment)
            {
                ((RemoteControlFragment) fragment).checkRegisteredList();
            }
        }
    }

    public int parseNumber(int id)
    {
        Log.v(TAG, "parseNumber() called.");

        int delId = R.id.lock_screen_number_del;

        if (id == delId)
        {
            return -1;
        }

        int[] numberIds = new int[]{
                R.id.lock_screen_number_0, R.id.lock_screen_number_1,
                R.id.lock_screen_number_2, R.id.lock_screen_number_3,
                R.id.lock_screen_number_4, R.id.lock_screen_number_5,
                R.id.lock_screen_number_6, R.id.lock_screen_number_7,
                R.id.lock_screen_number_8, R.id.lock_screen_number_9};

        for (int i = 0; i < numberIds.length; i++)
        {
            if (id == numberIds[i])
            {
                return i;
            }
        }

        return -1;
    }

    public void updateCircle(int count)
    {
        Log.v(TAG, "updateCircle(" + count + ") called.");

        Drawable fill = AppCompatResources.getDrawable(mContext, R.drawable.lock_screen_ic_circle_fill_16dp);
        Drawable empty = AppCompatResources.getDrawable(mContext, R.drawable.lock_screen_ic_circle_empty_16dp);

        switch (count)
        {
            case 0:
                mBinding.lockScreenCircle0.setImageDrawable(empty);
                mBinding.lockScreenCircle1.setImageDrawable(empty);
                mBinding.lockScreenCircle2.setImageDrawable(empty);
                mBinding.lockScreenCircle3.setImageDrawable(empty);
                break;

            case 1:
                mBinding.lockScreenCircle0.setImageDrawable(fill);
                mBinding.lockScreenCircle1.setImageDrawable(empty);
                mBinding.lockScreenCircle2.setImageDrawable(empty);
                mBinding.lockScreenCircle3.setImageDrawable(empty);
                break;

            case 2:
                mBinding.lockScreenCircle0.setImageDrawable(fill);
                mBinding.lockScreenCircle1.setImageDrawable(fill);
                mBinding.lockScreenCircle2.setImageDrawable(empty);
                mBinding.lockScreenCircle3.setImageDrawable(empty);
                break;

            case 3:
                mBinding.lockScreenCircle0.setImageDrawable(fill);
                mBinding.lockScreenCircle1.setImageDrawable(fill);
                mBinding.lockScreenCircle2.setImageDrawable(fill);
                mBinding.lockScreenCircle3.setImageDrawable(empty);
                break;

            case 4:
                mBinding.lockScreenCircle0.setImageDrawable(fill);
                mBinding.lockScreenCircle1.setImageDrawable(fill);
                mBinding.lockScreenCircle2.setImageDrawable(fill);
                mBinding.lockScreenCircle3.setImageDrawable(fill);
                break;
        }
    }

    public void processDel()
    {
        Log.v(TAG, "processDel() called.");

        if (mState == STATE_REGISTER || mState == STATE_DECODE)
        {
            if (0 < mPasswordCounter)
            {
                mPasswordCounter--;
            }

            updateCircle(mPasswordCounter);
        }
        else if (mState == STATE_COMPARE)
        {
            if (0 < mPasswordCompareCounter)
            {
                mPasswordCompareCounter--;
            }

            updateCircle(mPasswordCompareCounter);
        }
    }

    public void processNumber(int number)
    {
        Log.v(TAG, "processNumber(" + number + ") called.");

        if (number < 0 || 9 < number)
        {
            return;
        }

        if (mState == STATE_REGISTER || mState == STATE_DECODE)
        {
            if (mPasswordCounter < VALUE_LENGTH)
            {
                mPassword[mPasswordCounter] = number;
                mPasswordCounter++;
            }

            updateCircle(mPasswordCounter);
        }
        else if (mState == STATE_COMPARE)
        {
            if (mPasswordCompareCounter < VALUE_LENGTH)
            {
                mPasswordCompare[mPasswordCompareCounter] = number;
                mPasswordCompareCounter++;
            }

            updateCircle(mPasswordCompareCounter);
        }
    }

    public void processChecking()
    {
        Log.v(TAG, "processChecking() called.");

        if (mState == STATE_REGISTER)
        {
            if (mPasswordCounter == VALUE_LENGTH)
            {
                mState = STATE_COMPARE;
                updateMessage();
                updateCircle(mPasswordCompareCounter);
            }
        }
        else if (mState == STATE_COMPARE)
        {
            if (mPasswordCompareCounter == VALUE_LENGTH)
            {
                mPasswordString = "";
                mPasswordCompareString = "";

                for (int i = 0; i < VALUE_LENGTH; i++)
                {
                    mPasswordString = mPasswordString + mPassword[i];
                    mPasswordCompareString = mPasswordCompareString + mPasswordCompare[i];
                }

                if (mPasswordString.equals(mPasswordCompareString))
                {
                    writePassword(mPasswordString);
                    enableScreen(false);
                }
                else
                {
                    failAnimation();
                    bufferInit();
                    stateInit();
                    updateMessage();
                    //updateCircle(mPasswordCounter);
                }
            }
        }
        else if (mState == STATE_DECODE)
        {
            if (mPasswordCounter == VALUE_LENGTH)
            {
                mPasswordCompareString = readPassword();
                mPasswordString = "";

                for (int i = 0; i < VALUE_LENGTH; i++)
                {
                    mPasswordString = mPasswordString + mPassword[i];
                }

                if (mPasswordString.equals(mPasswordCompareString))
                {
                    enableScreen(false);
                    successAnimation();
                }
                else
                {
                    failAnimation();
                    bufferInit();
                    updateMessage();
                    //updateCircle(mPasswordCounter);
                }
            }
        }
    }

    public void numberClickListener(View view)
    {
        Log.v(TAG, "numberClickListener(" + view.toString() + ") called.");

        int inputNumber = parseNumber(view.getId());

        if (inputNumber == -1)
        {
            processDel();
        }
        else
        {
            processNumber(inputNumber);
        }

        processChecking();
    }

    public void successAnimation()
    {
        Animation disappear = AnimationUtils.loadAnimation(mContext, R.anim.lock_screen_correct_password);
        mBinding.lockScreen.startAnimation(disappear);
    }

    public void failAnimation()
    {
        Log.v(TAG, "failAnimation() called.");

        Animation shaker = AnimationUtils.loadAnimation(mContext, R.anim.lock_screen_incorrect_password);
        shaker.setAnimationListener(mAnimationListener);
        mBinding.circleLayout.startAnimation(shaker);
        mMainActivity.onVibrator(50);
    }

    private final Animation.AnimationListener mAnimationListener = new Animation.AnimationListener()
    {
        @Override
        public void onAnimationStart(Animation animation)
        {

        }

        @Override
        public void onAnimationEnd(Animation animation)
        {
            updateCircle(0);
        }

        @Override
        public void onAnimationRepeat(Animation animation)
        {

        }
    };
}
