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

import java.util.ArrayList;
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
    private List<EntityLog> mLogs;

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
        ((MainActivity) requireActivity()).mBinding.toolbarNavigationMessage.setText("사용자\n등록");
        ((MainActivity) requireActivity()).mBinding.toolbarNavigationMessage.setVisibility(View.VISIBLE);


        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[1] 입력 시작.
        /*
        {
            for (EntityLog log : UtilLog.instance.readAllLogs())
            {
                UtilLog.instance.delete(log);
            }

            prepareLogs();
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[1] 입력 끝.


        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[2] 입력 시작.
        /*
        {
            EntityLog logData = new EntityLog();
            logData.number = 1;
            logData.date = "2022-10-01 00:00:00.000";
            logData.message = "2022-10-01";

            EntityLog logIndex = new EntityLog();
            logIndex.number = 0;
            logIndex.date = "2022-01-01 00:00:00.000";
            logIndex.message = "2";

            UtilLog.instance.insert(logData);
            UtilLog.instance.insert(logIndex);

            prepareLogs();
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[2] 입력 끝.

        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[3] 입력 시작.
        /*
        {
            EntityLog logExtra1 = new EntityLog();
            logExtra1.number = 2;
            logExtra1.date = "2022-11-01 00:00:00.000";
            logExtra1.message = "2022-11-01";

            EntityLog logExtra2 = new EntityLog();
            logExtra2.number = 3;
            logExtra2.date = "2022-01-01 00:00:00.000";
            logExtra2.message = "2022-01-01";

            EntityLog logExtra3 = new EntityLog();
            logExtra3.number = 4;
            logExtra3.date = "2022-02-01 00:00:00.000";
            logExtra3.message = "2022-02-01";

            EntityLog logIndex = new EntityLog();
            logIndex.number = 0;
            logIndex.date = "2022-01-01 00:00:00.000";
            logIndex.message = "5";

            UtilLog.instance.insert(logExtra1);
            UtilLog.instance.insert(logExtra2);
            UtilLog.instance.insert(logExtra3);
            UtilLog.instance.insert(logIndex);

            prepareLogs();
        }
        */
        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[3] 입력 끝.


        printHiddenLogs();

        mLogBinding.logExitButton.setOnClickListener(view1 -> requireActivity().onBackPressed());
    }

    private void prepareLogs()
    {
        if (mLogs == null)
        {
            mLogs = new ArrayList<>();
        }

        mLogs.clear();

        List<EntityLog> logs = UtilLog.instance.readAllLogs();

        if (logs != null)
        {
            int size = logs.size();

            switch (size)
            {
                case 0:
                case 1:
                    Log.d(TAG, "[히든로그] 저장된 로그가 없습니다.");


                    // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[1] 시작.
                    /*
                    Log.d(TAG, "there is no log written.");
                    */
                    // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[1] 끝.


                    break;

                case 2:
                    Log.d(TAG, "[히든로그] 오직 1개의 로그만 있습니다.");


                    // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[2] 시작.
                    /*
                    Log.d(TAG, "there is only 1 log written.");
                    Log.d(TAG, "number = " + logs.get(1).number + ", date = " + logs.get(1).date + ", message = " + logs.get(1).message);
                    */
                    // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[2] 끝.


                    mLogs.add(logs.get(1));
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

                        String[] front_space_splits = front.split(" ");
                        String[] back_space_splits = back.split(" ");

                        String[] front_date_splits = front_space_splits[0].split("-");
                        String[] back_date_splits = back_space_splits[0].split("-");

                        String[] front_time_splits = front_space_splits[1].split(":");
                        String[] back_time_splits = back_space_splits[1].split(":");

                        String[] front_second_splits = front_time_splits[2].split("\\.");
                        String[] back_second_splits = back_time_splits[2].split("\\.");

                        int frontYear = Integer.parseInt(front_date_splits[0]);
                        int frontMonth = Integer.parseInt(front_date_splits[1]);
                        int frontDay = Integer.parseInt(front_date_splits[2]);
                        int frontHour = Integer.parseInt(front_time_splits[0]);
                        int frontMin = Integer.parseInt(front_time_splits[1]);
                        int frontSecond = Integer.parseInt(front_second_splits[0]);
                        int frontMs = Integer.parseInt(front_time_splits[1]);

                        int backYear = Integer.parseInt(back_date_splits[0]);
                        int backMonth = Integer.parseInt(back_date_splits[1]);
                        int backDay = Integer.parseInt(back_date_splits[2]);
                        int backHour = Integer.parseInt(back_time_splits[0]);
                        int backMin = Integer.parseInt(back_time_splits[1]);
                        int backSecond = Integer.parseInt(back_second_splits[0]);
                        int backMs = Integer.parseInt(back_time_splits[1]);

                        //Log.d(TAG, "Log(" + i + "){" + front + "} vs Log(" + (i + 1) + "){" + back + "}");

                        Log.d(TAG, "i = " + i
                                + " -> Front=" + frontYear + "-" + frontMonth + "-" + frontDay + " " + frontHour + ":" + frontMin + ":" + frontSecond + "." + frontMs
                                + ", Back=" + backYear + "-" + backMonth + "-" + backDay + " " + backHour + ":" + backMin + ":" + backSecond + "." + backMs);

                        if (backYear < frontYear)
                        {
                            oldestNumber = i + 1;
                            break;
                        }

                        if (backMonth < frontMonth)
                        {
                            oldestNumber = i + 1;
                            break;
                        }

                        if (backDay < frontDay)
                        {
                            oldestNumber = i + 1;
                            break;
                        }

                        if (backHour < frontHour)
                        {
                            oldestNumber = i + 1;
                            break;
                        }

                        if (backDay < frontDay)
                        {
                            oldestNumber = i + 1;
                            break;
                        }

                        if (backSecond < frontSecond)
                        {
                            oldestNumber = i + 1;
                            break;
                        }

                        if (backMs < frontMs)
                        {
                            oldestNumber = i + 1;
                            break;
                        }
                    }

                    Log.d(TAG, "[히든로그] 가장 오래된 로그의 번호는 " + oldestNumber + "입니다.");


                    // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[3-1] 시작.
                    /*
                    Log.d(TAG, "total system log count = " + (size - 1) + ", the oldest log number = " + oldestNumber);
                    */
                    // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[3-1] 끝.

                    int mLogIndex = 0;

                    // print from oldest log data to last log data on database.
                    for (int i = oldestNumber; i < size; i++)
                    {
                        mLogs.add(mLogIndex, logs.get(i));
                        mLogIndex++;

                        Log.d(TAG, "[히든로그] {인덱스 = " + i + "}, {일시 = " + logs.get(i).date + "}, {메시지 = " + logs.get(i).message + "}");


                        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[3-2] 시작.
                        /*
                        Log.d(TAG, "number = " + logs.get(i).number + ", date = " + logs.get(i).date + ", message = " + logs.get(i).message);
                        */
                        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[3-2] 끝.


                    }

                    // print from first log data to before oldest log data.
                    for (int i = 1; i < oldestNumber; i++)
                    {
                        //mLogs.add(logs.get(i));
                        mLogs.add(mLogIndex, logs.get(i));
                        mLogIndex++;

                        Log.d(TAG, "[히든로그] {인덱스 = " + i + "}, {일시 = " + logs.get(i).date + "}, {메시지 = " + logs.get(i).message + "}");


                        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[3-3] 시작.
                        /*
                        Log.d(TAG, "number = " + logs.get(i).number + ", date = " + logs.get(i).date + ", message = " + logs.get(i).message);
                        */
                        // TD2-SW-RC-UNIT-Test-ID-71 [시스템 로그 시간 순서 정렬 유닛] 순서[3-3] 끝.


                    }
                }
                break;
            }
        }
    }

    private void printHiddenLogs()
    {
        UtilLog.instance.printAllLogs();

        prepareLogs();

        LogAdapter adapter = new LogAdapter();
        mLogBinding.logRecyclerview.setAdapter(adapter);
        mLogBinding.logRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (mLogs != null)
        {
            for (int i = 0; i < mLogs.size(); i++)
            {
                adapter.addItem(mLogs.get(i));
            }
        }

        mLogBinding.logRecyclerview.scrollToPosition(adapter.getItemCount() - 1);
    }
}