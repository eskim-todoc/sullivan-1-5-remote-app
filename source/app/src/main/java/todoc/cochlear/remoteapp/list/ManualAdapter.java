package todoc.cochlear.remoteapp.list;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import todoc.cochlear.remoteapp.activity.R;

public class ManualAdapter extends RecyclerView.Adapter<ManualAdapter.ViewHolder>
{
    private ArrayList<Drawable[]> mItems;

    public ManualAdapter()
    {
        mItems = new ArrayList<>();
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manual, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        holder.onBind(mItems.get(position));
    }

    public void addItem(Drawable[] item)
    {
        if (item != null)
        {
            mItems.add(item);
            notifyDataSetChanged();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        ImageView title;
        ImageView body;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            title = itemView.findViewById(R.id.item_manual_title);
            body = itemView.findViewById(R.id.item_manual_body);
        }

        public void onBind(Drawable[] item)
        {
            title.setImageDrawable(item[0]);
            body.setImageDrawable(item[1]);
        }
    }
}
