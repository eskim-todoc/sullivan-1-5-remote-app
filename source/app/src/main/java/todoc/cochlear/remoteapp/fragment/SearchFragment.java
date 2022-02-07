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
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;
import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.Device;
import todoc.cochlear.remoteapp.list.SearchItem;
import todoc.cochlear.remoteapp.list.SearchItemAdapter;
import todoc.cochlear.remoteapp.params.ActionMessage;
import todoc.cochlear.remoteapp.params.AppParam;

public class SearchFragment extends Fragment
{
    private static final String TAG = "TD2_" + SearchFragment.class.getSimpleName();

    MainActivity mMainActivity;

    SearchItemAdapter mFoundItemAdapter;
    ListView mFoundListView;

    public SearchFragment()
    {
    }

    public void setToolbarMenu()
    {
        AppParam appParam = AppParam.getInstance();

        appParam.setMenuTitle(getString(R.string.toolbar_title_search));
        appParam.setMenuHome(true);
        appParam.setMenuSearch(false);
        appParam.setMenuList(false);
        appParam.setMenuManual(false);
        appParam.setMenuSupport(false);
        appParam.setMenuAutoConnection(false);
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        mMainActivity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_search, container, false);

        AppParam.getInstance().setCurrentFragmentNumber(AppParam.FRAGMENT_NUMBER_SEARCH);

        // Update Toolbar menu
        setToolbarMenu();
        mMainActivity.updateToolbar();

        // Registered sound processor
        SearchItemAdapter registeredItemAdapter = new SearchItemAdapter();
        ListView registeredListView = rootView.findViewById(R.id.search_registered_list_view);
        registeredListView.setEnabled(false); // Disable clickable
        registeredListView.setAdapter(registeredItemAdapter);

        // Check database
        if (AppParam.getInstance().database != null)
        {
            List<Device> devices = AppParam.getInstance().registeredDevices;

            for (int i = 0; i < devices.size(); i++)
            {
                String deviceModel = devices.get(i).getDeviceModel();
                String deviceName = devices.get(i).getDeviceName();
                String deviceAddress = devices.get(i).getDeviceMacAddress();
                registeredItemAdapter.addItem(deviceModel, deviceName, deviceAddress);
            }
        }
        Utils.setListViewHeightBasedOnChildren(registeredListView);

        // Searched sound processor
        mFoundItemAdapter = new SearchItemAdapter();
        mFoundListView = rootView.findViewById(R.id.search_found_list_view);
        mFoundListView.setAdapter(mFoundItemAdapter);
        mFoundListView.setOnItemClickListener(onItemClickListener);
        Utils.setListViewHeightBasedOnChildren(mFoundListView);

        // Send command to search sound processor over BLE
        getActivity().sendBroadcast(new Intent(ActionMessage.BLE_SCAN_RESTART));


        // ESKIM start
        /*
        registeredItemAdapter.addItem("01", "12345678", "");
        registeredItemAdapter.addItem("01", "abcdefgh", "");
        Utils.setListViewHeightBasedOnChildren(registeredListView);
        */
        // ESKIM end

        return rootView;
    }

    /**
     * Clear all items for Sound Processor found when scan is started.
     */
    public void clearAllSearchItems()
    {
        if (mFoundItemAdapter != null)
        {
            mFoundItemAdapter.clearAllItem();
        }

        Utils.setListViewHeightBasedOnChildren(mFoundListView);
    }

    /**
     * When a Sound Processor is found by scanner, the Sound Processor is listed in found ListView.
     */
    public void updateSearchItemFromActivity()
    {
        List<Device> devices = AppParam.getInstance().searchedDevices;

        mFoundItemAdapter.clearAllItem();

        for (int i = 0; i < devices.size(); i++)
        {
            mFoundItemAdapter.addItem(devices.get(i).getDeviceModel(), devices.get(i).getDeviceName(), devices.get(i).getDeviceMacAddress());
            Utils.setListViewHeightBasedOnChildren(mFoundListView);
        }
    }

    /**
     * Make dialog to ask you for what whether you want to connect ot not.
     */
    AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
            SearchItem item = (SearchItem) adapterView.getAdapter().getItem(i);

            makeDialogTryConnect(item);
            //makeDialogEnterPassword(item.getName());
        }
    };

    /**
     * Dialog - BackPressed
     */
    public void makeDialogBackPressed()
    {
        LayoutInflater inflater = LayoutInflater.from(mMainActivity);

        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity, R.style.MyAlertDialogTheme);

        builder.setMessage(getString(R.string.fragment_search_dialog_message_search_stop_and_go_home));

        builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                // Stop scan and show Home screen.
                requireActivity().sendBroadcast(new Intent(ActionMessage.NEW_FRAGMENT_HOME_WITH_STOP_BLE_SCAN));
            }
        });

        builder.setNegativeButton(getString(R.string.dialog_message_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
            }
        });

        AppParam.getInstance().lastDialog = builder.create();
        AppParam.getInstance().lastDialog.show();
    }

    /**
     * Dialog - For connecting to Sound Processor
     */
    public void makeDialogTryConnect(SearchItem item)
    {
        String model = item.getModel();
        String name = item.getName();
        String address = item.getAddress();

        //LayoutInflater inflater = LayoutInflater.from(mMainActivity);
        //View viewTitle = inflater.inflate(R.layout.dialog_custom_title, null);
        //TextView titleTextView = viewTitle.findViewById(R.id.dialog_custom_title);
        //titleTextView.setText(name);

        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity, R.style.MyAlertDialogTheme);

        builder.setMessage(getString(R.string.fragment_search_dialog_message_try_to_connect_1)
                + name
                + getString(R.string.fragment_search_dialog_message_try_to_connect_2));

        builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int i)
            {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
                // Stop scan
                requireActivity().sendBroadcast(new Intent(ActionMessage.BLE_SCAN_STOP));

                // Request connection to Sound Processor through Activity.
                if (AppParam.getInstance().bleConnectionState == AppParam.BLE_CONNECTION_STATE_DISCONNECTED)
                {
                    Device device = new Device();
                    device.setDeviceModel(model);
                    device.setDeviceName(name);
                    device.setDeviceMacAddress(address);
                    AppParam.getInstance().currentConnectDevice = device;

                    getActivity().sendBroadcast(new Intent(ActionMessage.BLE_CONNECT));
                }
                else
                {
                    Log.d(TAG, "You can not come here. How do you come here? Please detach your Sound Processor and attach it again.");
                    Toast.makeText(getContext(), getString(R.string.fragment_search_toast_message_critical_error), Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.setNegativeButton(getString(R.string.dialog_message_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
            }
        });

        AppParam.getInstance().lastDialog = builder.create();
        AppParam.getInstance().lastDialog.show();
    }

    /**
     * Dialog - Communication error.
     */
    public void makeDialogCommunicationError()
    {
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity, R.style.MyAlertDialogTheme);
        builder.setMessage(getString(R.string.fragment_search_dialog_message_communication_error));
        builder.setPositiveButton(getString(R.string.dialog_message_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mMainActivity.updateLongTimeIdleHandler(); // Update long time idle handler
            }
        });
        AppParam.getInstance().lastDialog = builder.create();
        AppParam.getInstance().lastDialog.show();
    }

    /**
     * Util for ListView dynamic height.
     */
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
}