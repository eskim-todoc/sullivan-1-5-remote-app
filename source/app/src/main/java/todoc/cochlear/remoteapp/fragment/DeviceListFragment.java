package todoc.cochlear.remoteapp.fragment;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.bluetooth.BtUtils;
import todoc.cochlear.remoteapp.database.Device;
import todoc.cochlear.remoteapp.list.DeviceListItemAdapter;
import todoc.cochlear.remoteapp.logging.LoggingUtils;
import todoc.cochlear.remoteapp.params.ActionMessage;
import todoc.cochlear.remoteapp.params.AppParam;

public class DeviceListFragment extends Fragment
{
      private static final String TAG = "TD2_" + DeviceListFragment.class.getSimpleName();

      MainActivity mMainActivity;
      DeviceListItemAdapter mDeviceListItemAdapter;
      ListView mDeviceListView;

      public DeviceListFragment()
      {
            // Required empty public constructor
      }

      @Override
      public void onAttach(@NonNull Context context)
      {
            super.onAttach(context);
            mMainActivity = (MainActivity) context;
      }

      public void setToolbarMenu()
      {
            AppParam.getInstance().setMenuTitle(getString(R.string.toolbar_title_lists));
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
      public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
      {
            ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_device_list, container, false);

            AppParam.getInstance().setCurrentFragmentNumber(AppParam.FRAGMENT_NUMBER_LIST);

            // Update toolbar
            setToolbarMenu();
            mMainActivity.updateToolbar();

            mDeviceListItemAdapter = new DeviceListItemAdapter(this);
            mDeviceListView = rootView.findViewById(R.id.device_list_list_view);
            mDeviceListView.setAdapter(mDeviceListItemAdapter);
            mDeviceListView.setEnabled(false);

            listViewUpdate();

            if (AppParam.getInstance().bleConnectionState == AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
            {
                  if (AppParam.getInstance().isAutoConnectionEnabled())
                  {
                        Log.d(TAG, "Here is Device List screen! And now you are not connected to Sound Processor. So App will search a Sound Processor to connect.");
                        mMainActivity.sendBroadcast(new Intent(ActionMessage.BLE_SCAN_START));
                  }
                  else
                  {
                        Log.d(TAG, "Here is Device List screen! And now you are not connected to Sound Processor. But, the auto connection feature is disabled.");
                  }
            }

            return rootView;
      }

      /**
       * Update ListView for Sound Processor list.
       */
      private void listViewUpdate()
      {
            AppParam appParam = AppParam.getInstance();

            appParam.registeredDevices = appParam.database.deviceDao().findAll();

            if (appParam.database != null && appParam.registeredDevices != null)
            {
                  for (int i = 0; i < appParam.registeredDevices.size(); i++)
                  {
                        mDeviceListItemAdapter.addItem(appParam.registeredDevices.get(i));
                  }
            }

            Utils.setListViewHeightBasedOnChildren(mDeviceListView);
      }

      /**
       * Button click callback.
       */
      public void onButtonClicked(Device device)
      {
            mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
            Log.d(TAG, "Sound Processor name = " + device.getDeviceName() + ", serial = " + device.getDeviceSerial());
            makeDialogToRemoveDevice(device);
      }

      /**
       * Dialog - For removing Sound Processor.
       */
      private void makeDialogToRemoveDevice(Device device)
      {
            mMainActivity.runOnUiThread(new Runnable()
            {
                  @Override
                  public void run()
                  {
                        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity, R.style.MyAlertDialogTheme);

                        // Check that whether this is connected or not.
                        if (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                        {
                              Device currentConnectDevice = AppParam.getInstance().currentConnectDevice;

                              if (currentConnectDevice.getDeviceSerial().equals(device.getDeviceSerial()))
                              {
                                    builder.setMessage(getString(R.string.fragment_device_list_dialog_message_delete_when_connected_1) + device.getDeviceName() + getString(R.string.fragment_device_list_dialog_message_delete_when_connected_2));
                              }
                              else
                              {
                                    builder.setMessage(getString(R.string.fragment_device_list_dialog_message_delete_when_not_connected_1) + device.getDeviceName() + getString(R.string.fragment_device_list_dialog_message_delete_when_not_connected_2));
                              }
                        }
                        else
                        {
                              builder.setMessage(getString(R.string.fragment_device_list_dialog_message_delete_when_not_connected_1) + device.getDeviceName() + getString(R.string.fragment_device_list_dialog_message_delete_when_not_connected_2));
                        }

                        builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener()
                        {
                              @Override
                              public void onClick(DialogInterface dialogInterface, int i)
                              {
                                    mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler

                                    AppParam.getInstance().database.deviceDao().delete(device);
                                    AppParam.getInstance().registeredDevices = AppParam.getInstance().database.deviceDao().findAll();

                                    writeMessage(LoggingUtils.LOGGING_DEVICE_REMOVED, "serial=" + device.getDeviceSerial() + "user=" + device.getImplantUserName()); // Logging

                                    mDeviceListItemAdapter.clearAllItem();
                                    mDeviceListView.setAdapter(mDeviceListItemAdapter);
                                    listViewUpdate();

                                    if (AppParam.getInstance().bleConnectionState != AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                                    {
                                          if (AppParam.getInstance().currentConnectDevice.getDeviceMacAddress().equals(device.getDeviceMacAddress()))
                                          {
                                                mMainActivity.sendBroadcast(new Intent(ActionMessage.BLE_DISCONNECT));
                                          }
                                    }

                                    mMainActivity.sendBroadcast(new Intent(ActionMessage.DATABASE_CHECK_EMPTY));

                                    // DELETE BONDED DEVICE
                                    BtUtils.getInstance().eraseBondedDeviceUsingMacAddress(device.getDeviceMacAddress(), BluetoothAdapter.getDefaultAdapter());
                              }
                        });

                        builder.setNegativeButton(getString(R.string.dialog_message_no), new DialogInterface.OnClickListener()
                        {
                              @Override
                              public void onClick(DialogInterface dialogInterface, int i)
                              {
                                    mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                              }
                        });

                        AppParam.getInstance().lastDialog = builder.create();
                        AppParam.getInstance().lastDialog.show();
                  }
            });
      }

      /**
       * BackPressed without dialog.
       */
      public void makeBackPressedWithoutDialog()
      {
            mMainActivity.sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_HOME));
      }

      // Util for Sound Processor ListView.
      static public class Utils
      {
            public static void setListViewHeightBasedOnChildren(ListView listView)
            {
                  ListAdapter listAdapter = listView.getAdapter();
                  if (listAdapter == null)
                  {
                        // pre-condition
                        return;
                  }

                  int totalHeight = 0;
                  for (int i = 0; i < listAdapter.getCount(); i++)
                  {
                        View listItem = listAdapter.getView(i, null, listView);
                        listItem.measure(0, 0);
                        totalHeight += listItem.getMeasuredHeight();
                  }

                  ViewGroup.LayoutParams params = listView.getLayoutParams();
                  params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
                  listView.setLayoutParams(params);
                  listView.requestLayout();
            }
      }

      public void writeMessage(int type, String message)
      {
            LoggingUtils.getInstance().writeMessage(LoggingUtils.getInstance().typeMessage(type) + " : " + message);
      }
}