package todoc.cochlear.remoteapp.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.fragment.DeviceFragment;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder>
{
    private ArrayList<EntityDevice> mItems;
    DeviceFragment mDeviceFragment;

    public DeviceAdapter(DeviceFragment fragment)
    {
        mItems = new ArrayList<>();
        mDeviceFragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceAdapter.ViewHolder holder, int position)
    {
        holder.onBind(mItems.get(position), position);
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
    }

    public EntityDevice getItem(int position)
    {
        return mItems.get(position);
    }

    public void clearItems()
    {
        mItems.clear();
        this.notifyDataSetChanged();
    }

    public void addItem(EntityDevice item)
    {
        if (item != null)
        {
            mItems.add(item);
            this.notifyDataSetChanged();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView serial;
        TextView option;
        int position;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            serial = itemView.findViewById(R.id.serial);
            option = itemView.findViewById(R.id.option);

            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    int position = getAdapterPosition();
                    mDeviceFragment.mItemClickListener(position);
                }
            });
        }

        public void onBind(EntityDevice item, int position)
        {
            this.position = position;
            serial.setText(item.serialNumber);
            option.setText(item.additionalInformation);
        }
    }
}
