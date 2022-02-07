package todoc.cochlear.remoteapp.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;

public class LoggingItemAdapter extends BaseAdapter
{
    ArrayList<LoggingItem> mItems;

    public LoggingItemAdapter()
    {
        mItems = new ArrayList<>();
    }

    @Override
    public int getCount()
    {
        return mItems.size();
    }

    @Override
    public long getItemId(int i)
    {
        return i;
    }

    @Override
    public Object getItem(int i)
    {
        return mItems.get(i);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup)
    {
        LoggingItemViewHolder viewHolder;

        if (view == null)
        {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_logging, viewGroup, false);

            viewHolder = new LoggingItemViewHolder();
            viewHolder.date = view.findViewById(R.id.item_logging_date);
            viewHolder.message = view.findViewById(R.id.item_logging_message);

            view.setTag(viewHolder);
        }

        viewHolder = (LoggingItemViewHolder) view.getTag();

        LoggingItem item = mItems.get(i);

        viewHolder.date.setText(item.getDate());
        viewHolder.message.setText(item.getMessage());

        return view;
    }

    public void addItem(String date, String message)
    {
        LoggingItem item = new LoggingItem();

        item.setDate(date);
        item.setMessage(message);

        mItems.add(item);
        this.notifyDataSetChanged();
    }

    public void clearAllItems()
    {
        mItems.clear();
        this.notifyDataSetChanged();
    }

    static private class LoggingItemViewHolder
    {
        TextView date;
        TextView message;
    }
}
