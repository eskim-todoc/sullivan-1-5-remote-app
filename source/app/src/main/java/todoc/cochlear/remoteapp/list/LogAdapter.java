package todoc.cochlear.remoteapp.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.logs.EntityLog;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder>
{
    private final ArrayList<EntityLog> mItems;

    public LogAdapter()
    {
        mItems = new ArrayList<>();
    }

    @NonNull
    @Override
    public LogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hidden_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogAdapter.ViewHolder holder, int position)
    {
        holder.onBind(mItems.get(position), position);
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
    }

    public EntityLog getItem(int position)
    {
        return mItems.get(position);
    }

    public void clearItem()
    {
        mItems.clear();
        this.notifyDataSetChanged();
    }

    public void addItem(EntityLog item)
    {
        if (item != null)
        {
            mItems.add(item);
            this.notifyDataSetChanged();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView date;
        TextView message;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            date = itemView.findViewById(R.id.hidden_log_date);
            message = itemView.findViewById(R.id.hidden_log_message);
        }

        public void onBind(EntityLog item, int position)
        {
            date.setText(item.date);
            message.setText(item.message);
        }
    }
}
