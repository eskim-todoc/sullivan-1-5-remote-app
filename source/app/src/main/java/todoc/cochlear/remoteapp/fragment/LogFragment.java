package todoc.cochlear.remoteapp.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.List;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.FragmentLogBinding;
import todoc.cochlear.remoteapp.database.logs.EntityLog;
import todoc.cochlear.remoteapp.list.LogAdapter;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.params.PacketInfo;
import todoc.cochlear.remoteapp.params.Status;

public class LogFragment extends Fragment
{
    static private final String TAG = "TODOC_" + LogFragment.class.getSimpleName();

    public FragmentLogBinding mLogBinding;
    public boolean isStarted;

    public LogFragment()
    {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mLogBinding = FragmentLogBinding.inflate(inflater, container, false);
        return mLogBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity) requireActivity()).mBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.toolbar_ic_back_arrow_24dp));
        ((MainActivity) requireActivity()).mBinding.toolbar.getMenu().findItem(R.id.toolbar_settings).setVisible(false);
        ((MainActivity) requireActivity()).mBinding.toolbar.getMenu().findItem(R.id.toolbar_user).setVisible(false);
        ((MainActivity) requireActivity()).mBinding.toolbar.getMenu().findItem(R.id.toolbar_search).setVisible(false);
        ((MainActivity) requireActivity()).mBinding.toolbar.setTitle("시스템 로그");

        mLogBinding.logExitButton.setOnClickListener(view1 -> requireActivity().onBackPressed());

        mLogBinding.logLogButton.setOnClickListener(view1 ->
        {
            ((MainActivity) requireActivity()).mBinding.toolbar.setTitle("시스템 로그");
            mLogBinding.logDiagnosticsLayout.setVisibility(View.GONE);
            mLogBinding.logLayout.setVisibility(View.VISIBLE);
        });

        mLogBinding.logDiagnosticsButton.setOnClickListener(view1 ->
        {
            ((MainActivity) requireActivity()).mBinding.toolbar.setTitle("오디오 입력 측정");
            mLogBinding.logLayout.setVisibility(View.GONE);
            mLogBinding.logDiagnosticsLayout.setVisibility(View.VISIBLE);
        });

        String[] spinnerItems = requireActivity().getResources().getStringArray(R.array.stim_test_spinner);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(requireContext(), R.layout.spinner_stim_test, spinnerItems);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_stim_test);
        mLogBinding.logDiagnosticsSpinnerSpinner.setAdapter(spinnerAdapter);

        printHiddenLogs();

        isStarted = false;
        start_button();
        stop_button();
    }

    //
    // start button
    //
    public void start_button()
    {
        mLogBinding.logDiagnosticsStartButton.setOnClickListener(view ->
        {
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
            {
                if (!isStarted)
                {
                    ((MainActivity) requireActivity()).mCheckBatteryHandler.removeCallbacks(((MainActivity) requireActivity()).mCheckBatteryRunner);

                    isStarted = true;
                    mLogBinding.logAudioSignalStateTv.setText("동작");
                    byte ch = (byte) (0xff & (mLogBinding.logDiagnosticsSpinnerSpinner.getSelectedItemPosition() + 1));
                    ((MainActivity) requireActivity()).sendPacket(((MainActivity) requireActivity()).packetMaker(PacketInfo.HEADER_AUDIO_INPUT_MAX_READ, new byte[]{ch}, 2));
                }
            }
            else
            {
                Toast.makeText(requireContext(), "연결되지 않았습니다. 기본사용자를 기반으로 연결을 시도합니다.", Toast.LENGTH_SHORT).show();
                isStarted = true;
                mLogBinding.logAudioSignalStateTv.setText("동작");
                ((MainActivity) requireActivity()).scanLe(true);
            }
        });
    }

    //
    // stop button
    //
    public void stop_button()
    {
        mLogBinding.logDiagnosticsStopButton.setOnClickListener(view ->
        {
            ((MainActivity) requireActivity()).longTimeIdleHandlerUpdate(true);

            if (Status.instance().connectionState == Status.CONNECTION_STATE_CONNECTED)
            {
                if (isStarted)
                {
                    isStarted = false;
                    ((MainActivity) requireActivity()).mCheckBatteryHandler.postDelayed(((MainActivity) requireActivity()).mCheckBatteryRunner, MainActivity.CHECK_BATTERY_DELAY_IN_MS);
                }
            }
            else
            {
                Toast.makeText(requireContext(), "연결되지 않았습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void printHiddenLogs()
    {
        UtilLog.instance.printAllLogs();

        List<EntityLog> logs = UtilLog.instance.readAllLogs();
        LogAdapter adapter = new LogAdapter();
        mLogBinding.logRecyclerview.setAdapter(adapter);
        mLogBinding.logRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (logs != null) // No log data.
        {
            int size = logs.size();

            switch (size)
            {
                case 0:
                case 1:
                {
                    Log.d(TAG, "[히든로그] 저장된 로그가 없습니다.");
                }
                break;

                case 2:
                {
                    Log.d(TAG, "[히든로그] 오직 1개의 로그만 있습니다.");

                    adapter.addItem(logs.get(1));

                    Log.d(TAG, "[히든로그] {인덱스 = 1}, {일시 = " + logs.get(1).date + "}, {메시지 = " + logs.get(1).message + "}");
                }
                break;

                default:
                {
                    Log.d(TAG, "[히든로그] 다수의 로그가 있습니다. -> " + (size - 1) + "개.");

                    int oldestNumber = 1;

                    Log.d(TAG, "[히든로그] 가장 오래된 로그를 찾습니다.");

                    // find oldest date
                    for (int i = 1; i < size - 1; i++)
                    {
                        String front = logs.get(i).date;
                        String back = logs.get(i + 1).date;

                        //Log.d(TAG, "Log(" + i + "){" + front + "} vs Log(" + (i + 1) + "){" + back + "}");

                        if (0 < front.compareTo(back))
                        {
                            oldestNumber = i + 1;
                            break;
                        }
                    }

                    Log.d(TAG, "[히든로그] 가장 오래된 로그의 번호는 " + oldestNumber + "입니다.");

                    // print from oldest log data to last log data on database.
                    for (int i = oldestNumber; i < size; i++)
                    {
                        adapter.addItem(logs.get(i));
                        Log.d(TAG, "[히든로그] {인덱스 = " + i + "}, {일시 = " + logs.get(i).date + "}, {메시지 = " + logs.get(i).message + "}");
                    }

                    // print from first log data to before oldest log data.
                    for (int i = 1; i < oldestNumber; i++)
                    {
                        adapter.addItem(logs.get(i));
                        Log.d(TAG, "[히든로그] {인덱스 = " + i + "}, {일시 = " + logs.get(i).date + "}, {메시지 = " + logs.get(i).message + "}");
                    }
                }
                break;
            } // end, switch
        } //end, if

        mLogBinding.logRecyclerview.scrollToPosition(adapter.getItemCount() - 1);
    }
}