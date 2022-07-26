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

import java.util.List;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.FragmentLogBinding;
import todoc.cochlear.remoteapp.database.logs.EntityLog;
import todoc.cochlear.remoteapp.list.LogAdapter;
import todoc.cochlear.remoteapp.database.logs.UtilLog;

public class LogFragment extends Fragment
{
    static private final String TAG = "TODOC_" + LogFragment.class.getSimpleName();

    private FragmentLogBinding mLogBinding;

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
        ((MainActivity) requireActivity()).mBinding.toolbar.setTitle("시스템 로그");

        mLogBinding.logExitButton.setOnClickListener(view1 -> requireActivity().onBackPressed());

        printHiddenLogs();
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