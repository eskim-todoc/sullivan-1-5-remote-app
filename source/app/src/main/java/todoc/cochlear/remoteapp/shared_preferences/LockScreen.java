package todoc.cochlear.remoteapp.shared_preferences;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.ActivityMainBinding;
import todoc.cochlear.remoteapp.fragment.RemoteControlFragment;
import todoc.cochlear.remoteapp.view_model.BleViewModel;

public class LockScreen
{
    static public LockScreen mInstance = new LockScreen();

    static private final String TAG = "TODOC_" + LockScreen.class.getSimpleName();

    static private final String SHARED_PREFERENCES_NAME = "LOCK_SCREEN";

    static private final String KEY_FOR_PASSWORD = "PASSWORD";
    static private final String KEY_FOR_ENABLE = "ENABLE";

    static private final int VALUE_LENGTH = 4;

    static public final int STATE_REGISTER = 0;
    static public final int STATE_COMPARE = 1;
    static public final int STATE_DECODE = 2;

    static public int mState;
    static public int mPasswordCounter;
    static public int[] mPassword;
    static public int mPasswordCompareCounter;
    static public int[] mPasswordCompare;

    static public String mPasswordString;
    static public String mPasswordCompareString;

    static private Context mContext;
    static private ActivityMainBinding mBinding;

    static public void resume(Context context, ActivityMainBinding binding)
    {
        Log.v(TAG, "resume(" + context.toString() + ", " + binding.toString() + ") called.");

        mContext = context;
        mBinding = binding;

        bufferInit();
        stateInit();
        updateCircle(0);
        updateMessage();
        enableScreen(isEnabled(context));
    }

    static public void bufferInit()
    {
        Log.v(TAG, "bufferInit() called.");

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

    static public void stateInit()
    {
        Log.v(TAG, "stateInit() called.");

        if (LockScreen.readPassword() == null)
        {
            LockScreen.mState = LockScreen.STATE_REGISTER;
        }
        else
        {
            LockScreen.mState = LockScreen.STATE_DECODE;
        }
    }

    static public boolean isEnabled(Context context)
    {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getBoolean(KEY_FOR_ENABLE, true);
    }

    static public void setEnable(Context context, boolean enable)
    {
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_FOR_ENABLE, enable).apply();
        Log.d(TAG, "Lock Screen Enable state = " + enable + ".");
    }

    static public void erasePassword()
    {
        Log.v(TAG, "Erase the lock screen password.");
        mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().putString(KEY_FOR_PASSWORD, null).apply();
    }

    static public void writePassword(String password)
    {
        Log.v(TAG, "writePassword() called.");

        mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().putString(KEY_FOR_PASSWORD, password).apply();
    }

    static public String readPassword()
    {
        Log.v(TAG, "readPassword() called.");

        String password = mContext.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getString(KEY_FOR_PASSWORD, null);

        if (password != null && password.length() == VALUE_LENGTH)
        {
            return password;
        }

        return null;
    }

    static public void updateMessage()
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

    static public void enableScreen(boolean enable)
    {
        Log.v(TAG, "enableScreen(" + enable + ") called.");

        if (enable)
        {
            BleViewModel viewModel = new ViewModelProvider((MainActivity) mContext).get(BleViewModel.class);

            if (viewModel.getBondState() == BluetoothDevice.BOND_BONDING)
            {
                viewModel.setBondState(BluetoothDevice.BOND_NONE);
                return;
            }

            mBinding.lockScreen.setVisibility(View.VISIBLE);

            Fragment fragment = ((MainActivity) mContext).getSupportFragmentManager().findFragmentById(R.id.frame);
            if (fragment instanceof RemoteControlFragment)
            {
                AlertDialog dialog = ((RemoteControlFragment) fragment).mDialog;

                if (dialog != null)
                {
                    dialog.dismiss();
                    ((RemoteControlFragment) fragment).mDialog = null;
                }
            }

            viewModel.setSearching(BleViewModel.SEARCHING_DISABLED);
        }
        else
        {
            mBinding.lockScreen.setVisibility(View.GONE);

            Fragment fragment = ((MainActivity) mContext).getSupportFragmentManager().findFragmentById(R.id.frame);
            if (fragment instanceof RemoteControlFragment)
            {
                ((RemoteControlFragment) fragment).checkRegisteredList();
            }
        }
    }

    static public int parseNumber(int id)
    {
        Log.v(TAG, "parseNumber() called.");

        int delId = R.id.lock_screen_number_del;

        if (id == delId)
        {
            return -1;
        }

        int[] numberIds = new int[]{R.id.lock_screen_number_0, R.id.lock_screen_number_1, R.id.lock_screen_number_2, R.id.lock_screen_number_3, R.id.lock_screen_number_4, R.id.lock_screen_number_5, R.id.lock_screen_number_6, R.id.lock_screen_number_7, R.id.lock_screen_number_8, R.id.lock_screen_number_9};

        for (int i = 0; i < numberIds.length; i++)
        {
            if (id == numberIds[i])
            {
                return i;
            }
        }

        return -1;
    }

    static public void updateCircle(int count)
    {
        Log.v(TAG, "updateCircle(" + count + ") called.");

        Drawable fill = mContext.getDrawable(R.drawable.lock_screen_ic_circle_fill_16dp);
        Drawable empty = mContext.getDrawable(R.drawable.lock_screen_ic_circle_empty_16dp);

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

    static public void processDel()
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

    static public void processNumber(int number)
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

    static public void processChecking()
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
                    mPasswordString += mPassword[i];
                    mPasswordCompareString += mPasswordCompare[i];
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
                    mPasswordString += mPassword[i];
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

    static public void numberClickListener(View view)
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

    static public void successAnimation()
    {
        Animation disappear = AnimationUtils.loadAnimation(mContext, R.anim.lock_screen_correct_password);
        mBinding.lockScreen.startAnimation(disappear);
    }

    static public void failAnimation()
    {
        Log.v(TAG, "failAnimation() called.");

        Animation shaker = AnimationUtils.loadAnimation(mContext, R.anim.lock_screen_incorrect_password);
        shaker.setAnimationListener(mAnimationListener);
        mBinding.circleLayout.startAnimation(shaker);
        ((MainActivity) mContext).onVibrator(50);
    }

    static private Animation.AnimationListener mAnimationListener = new Animation.AnimationListener()
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
