package todoc.cochlear.remoteapp.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.logging.LoggingUtils;
import todoc.cochlear.remoteapp.params.ActionMessage;
import todoc.cochlear.remoteapp.params.AppParam;

public class PasswordFragment extends Fragment
{
    private static final String TAG = "TD2_" + PasswordFragment.class.getSimpleName();
    MainActivity mMainActivity;

    ViewGroup rootView;
    EditText mPasswordEditText;
    TextView mWrongTextView;

    public PasswordFragment()
    {
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    public void setToolbarMenu()
    {
        AppParam.getInstance().setMenuTitle(getString(R.string.toolbar_title_password));
        AppParam.getInstance().setMenuHome(true);
        AppParam.getInstance().setMenuSearch(false);
        AppParam.getInstance().setMenuList(false);
        AppParam.getInstance().setMenuManual(false);
        AppParam.getInstance().setMenuSupport(false);
        AppParam.getInstance().setMenuAutoConnection(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_password, container, false);

        AppParam.getInstance().setCurrentFragmentNumber(AppParam.FRAGMENT_NUMBER_PASSWORD);

        // Update toolbar
        setToolbarMenu();
        mMainActivity.updateToolbar();

        // TextView for wrong password.
        mWrongTextView = rootView.findViewById(R.id.fragment_password_wrong_text);

        // EditText...
        mPasswordEditText = rootView.findViewById(R.id.fragment_password_input_edittext);

        // Clear EditText if not null
        if (mPasswordEditText.getText() != null)
        {
            mPasswordEditText.getText().clear();
        }

        // Button...
        Button passwordButton = rootView.findViewById(R.id.fragment_password_ok_button);
        passwordButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                ViewGroup rootView = (ViewGroup) view.getParent();

                // Get password from EditText.
                mPasswordEditText = rootView.findViewById(R.id.fragment_password_input_edittext);
                String password = mPasswordEditText.getText().toString();

                // Clear EditText after getting password.
                if (mPasswordEditText.getText() != null)
                {
                    mPasswordEditText.getText().clear();
                }

                // Check word count and expression.
                if (password.length() < 4 || !password.matches("^[0-9][0-9][0-9][0-9]$"))
                {
                    rootView.findViewById(R.id.fragment_password_warning_text).setVisibility(View.VISIBLE);
                    return;
                }

                // Send password to Sound Processor when BLE is not busy.
                if (AppParam.getInstance().isBleBusy)
                {
                    Toast.makeText(getContext(), getString(R.string.fragment_password_toast_message_ble_busy), Toast.LENGTH_SHORT).show();
                    return;
                }

                byte[] packet = mMainActivity.packetMaker(MainActivity.PACKET_HEADER_PASSWORD, password.getBytes(), 5);
                // ESKIM start
                //byte[] packet = mMainActivity.packetMaker(MainActivity.PACKET_HEADER_SOUND_PROCESSOR_INFO, password.getBytes(), 1);
                // ESKIM end

                // Send password packet Sound Processor.
                if (mMainActivity.sendPacket(packet))
                {
                    // Clear input keyboard window
                    InputMethodManager imm = (InputMethodManager) mMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mPasswordEditText.getWindowToken(), 0);

                    AppParam.getInstance().currentConnectDevice.setDevicePassword(password);
                    Log.d(TAG, "Password sent to Sound Processor.");
                }
            }
        });

        return rootView;
    }

    /**
     * Show message for currently reading Sound Processor information.
     */
    public void readingInformation()
    {
        if (mWrongTextView != null)
        {
            //Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable()
            mMainActivity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mWrongTextView.setText(getString(R.string.fragment_password_layout_message_read_information));
                    mWrongTextView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    /**
     * Show message for current password is not correct.
     */
    public void wrongPassword()
    {
        if (mWrongTextView != null)
        {
            //Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable()
            mMainActivity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mWrongTextView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    /**
     * Dialog - BackPressed
     */
    public void makeDialogBackPressed()
    {
        //Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable()
        mMainActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler

                LayoutInflater inflater = LayoutInflater.from(mMainActivity);

                android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity, R.style.MyAlertDialogTheme);

                builder.setMessage(getString(R.string.fragment_password_dialog_message_stop_registration_and_go_search_screen));

                // Clear input keyboard window
                InputMethodManager imm = (InputMethodManager) mMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mPasswordEditText.getWindowToken(), 0);

                builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        // Disconnect and show Search screen by User.
                        AppParam.getInstance().isDisconnectedByUser = true;
                        mMainActivity.sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE));
                    }
                });

                builder.setNegativeButton(getString(R.string.dialog_message_no), null);

                AppParam.getInstance().lastDialog = builder.create();
                AppParam.getInstance().lastDialog.show();
            }
        });
    }

    /**
     * Dialog - Communication error.
     */
    public void makeDialogCommunicationError()
    {
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity, R.style.MyAlertDialogTheme);
        builder.setCancelable(false);
        builder.setMessage(getString(R.string.fragment_password_dialog_message_communication_error));
        builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                // Show Search screen.
                mMainActivity.sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE));
            }
        });
        AppParam.getInstance().lastDialog = builder.create();
        AppParam.getInstance().lastDialog.show();
    }

    /**
     * Dialog - Register Sound Processor to database even though the map date is not equal to previously registered Sound Processor.
     */
    public void makeDialogIgnoreMapDate()
    {
        //Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable()
        mMainActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                LayoutInflater inflater = LayoutInflater.from(mMainActivity);

                android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity, R.style.MyAlertDialogTheme);
                builder.setCancelable(false);

                builder.setMessage(getString(R.string.fragment_password_dialog_message_different_map_date));

                // Clear input keyboard window
                InputMethodManager imm = (InputMethodManager) mMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mPasswordEditText.getWindowToken(), 0);

                builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                        // you are sure you want to register Sound Processor to database!
                        AppParam.getInstance().database.deviceDao().insert(AppParam.getInstance().currentConnectDevice);
                        AppParam.getInstance().registeredDevices = AppParam.getInstance().database.deviceDao().findAll();

                        writeMessage(LoggingUtils.LOGGING_DEVICE_REGISTERED,
                                "serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                        + "user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()); // Logging

                        // Show Home screen.
                        mMainActivity.sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_HOME));

                        // Read Sound Processor status.
                        mMainActivity.sendPacket(mMainActivity.packetMaker(MainActivity.PACKET_HEADER_SOUND_PROCESSOR_STATUS, null, 1));
                    }
                });

                builder.setNegativeButton(getString(R.string.dialog_message_no), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                        // Disconnect and show Search screen by User.
                        AppParam.getInstance().isDisconnectedByUser = true;
                        mMainActivity.sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE));
                    }
                });

                AppParam.getInstance().lastDialog = builder.create();
                AppParam.getInstance().lastDialog.show();
            }
        });
    }

    /**
     * Dialog - Register Sound Processor to database even though current user name is new.
     */

    public void makeDialogRegisterNewUser()
    {
        //Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable()
        mMainActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                LayoutInflater inflater = LayoutInflater.from(mMainActivity);

                android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity, R.style.MyAlertDialogTheme);
                builder.setCancelable(false);

                builder.setMessage(getString(R.string.fragment_password_dialog_message_new_user));

                // Clear input keyboard window.
                InputMethodManager imm = (InputMethodManager) mMainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mPasswordEditText.getWindowToken(), 0);

                builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                        // Delete all of registered Sound Processors because you want to register Sound Processor
                        for (int idx = 0; idx < AppParam.getInstance().registeredDevices.size(); idx++)
                        {
                            AppParam.getInstance().database.deviceDao().delete(AppParam.getInstance().registeredDevices.get(idx));

                            writeMessage(LoggingUtils.LOGGING_DEVICE_REMOVED,
                                    "serial=" + AppParam.getInstance().registeredDevices.get(idx).getDeviceSerial()
                                            + "user=" + AppParam.getInstance().registeredDevices.get(idx).getImplantUserName()); // Logging
                        }

                        AppParam.getInstance().database.deviceDao().insert(AppParam.getInstance().currentConnectDevice);
                        AppParam.getInstance().registeredDevices = AppParam.getInstance().database.deviceDao().findAll();

                        writeMessage(LoggingUtils.LOGGING_DEVICE_REGISTERED,
                                "serial=" + AppParam.getInstance().currentConnectDevice.getDeviceSerial()
                                        + "user=" + AppParam.getInstance().currentConnectDevice.getImplantUserName()); // Logging

                        // Show Home screen and read Sound Processor status.
                        mMainActivity.sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_HOME));
                        mMainActivity.sendPacket(mMainActivity.packetMaker(MainActivity.PACKET_HEADER_SOUND_PROCESSOR_STATUS, null, 1));
                    }
                });

                builder.setNegativeButton(getString(R.string.dialog_message_no), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                        // Disconnect and show Search screen by User.
                        AppParam.getInstance().isDisconnectedByUser = true;
                        mMainActivity.sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_SEARCH_WITH_DISCONNECT_BLE));
                    }
                });

                AppParam.getInstance().lastDialog = builder.create();
                AppParam.getInstance().lastDialog.show();
            }
        });
    }

    public void writeMessage(int type, String message)
    {
        LoggingUtils.getInstance().writeMessage(LoggingUtils.getInstance().typeMessage(type) + " : " + message);
    }
}