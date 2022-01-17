package todoc.cochlear.remoteapp.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.params.DeviceParam;

public class SearchItemAdapter extends BaseAdapter
{
    ArrayList<SearchItem> mItems;

    public SearchItemAdapter()
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
        SearchItemViewHolder viewHolder;

        if (view == null)
        {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_search, viewGroup, false);

            viewHolder = new SearchItemViewHolder();
            viewHolder.model = view.findViewById(R.id.item_image);
            viewHolder.name = view.findViewById(R.id.item_name);

            view.setTag(viewHolder);
        }

        viewHolder = (SearchItemViewHolder) view.getTag();

        SearchItem item = mItems.get(i);

        // Model image view.
        if (item.getModel().equals(DeviceParam.DEVICE_MODEL_TD2))
        {
            viewHolder.model.setImageDrawable(ContextCompat.getDrawable(viewGroup.getContext(), R.drawable.drawable_model_td2));
        }
        else
        {
            viewHolder.model.setImageDrawable(ContextCompat.getDrawable(viewGroup.getContext(), R.drawable.drawable_model_unknown));
        }

        // Name text view.
        viewHolder.name.setText(item.getName());

        return view;
    }

    /**
     * Add.
     */
    public void addItem(String model, String name, String address)
    {
        SearchItem item = new SearchItem();

        item.setModel(model);
        item.setName(name);
        item.setAddress(address);

        mItems.add(item);
        this.notifyDataSetChanged();
    }

    /**
     * Clear.
     */
    public void clearAllItem()
    {
        mItems.clear();
        this.notifyDataSetChanged();
    }

    /**
     * View holder class.
     */
    static private class SearchItemViewHolder
    {
        ImageView model;
        TextView name;
    }
}
