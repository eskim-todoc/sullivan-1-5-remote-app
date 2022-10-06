package todoc.cochlear.remoteapp.list;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.fragment.ShareFragment;

public class ShareCollectMapAdapter extends RecyclerView.Adapter<ShareCollectMapAdapter.ViewHolder>
{
    private final ArrayList<CollectedMapItem> mItems;
    public ShareFragment mShareFragment;

    public ShareCollectMapAdapter(ShareFragment fragment)
    {
        mItems = new ArrayList<>();
        mShareFragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_collect_map, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        holder.onBind(mItems.get(position), position);
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
    }

    public int getCollectedMapCount()
    {
        int count = 0;

        if (0 < mItems.size())
        {
            for (int i = 0; i < mItems.size(); i++)
            {
                if (mItems.get(i).isMapCollected)
                {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean isMapCollected(int position)
    {
        return mItems.get(position).isMapCollected;
    }

    public void setMapCollectState(int position)
    {
        mItems.get(position).isMapCollected = true;
        this.notifyDataSetChanged();
    }

    public void setOteSerial(int position, String serial)
    {
        mItems.get(position).isBleScanned = true;
        mItems.get(position).oteSerial = serial;

        this.notifyDataSetChanged();
    }

    public String getOteSerial(int position)
    {
        return mItems.get(position).oteSerial;
    }

    public String getName(int position)
    {
        return mItems.get(position).name;
    }

    public String getEar(int position)
    {
        return mItems.get(position).ear;
    }

    public boolean isBleScanned(int position)
    {
        return mItems.get(position).isBleScanned;
    }

    public void setBleScannedState(int position, boolean state)
    {
        mItems.get(position).isBleScanned = state;
    }

    public boolean isItemSelected(int position)
    {
        return mItems.get(position).isItemSelected;
    }

    public void setItemSelectState(int position, boolean select)
    {
        mItems.get(position).isItemSelected = select;
        this.notifyDataSetChanged();
    }

    public void setBtDevice(int position, BluetoothDevice btDevice)
    {
        mItems.get(position).btDevice = btDevice;
    }

    public BluetoothDevice getBtDevice(int position)
    {
        return mItems.get(position).btDevice;
    }

    public void setItemSelectedStateForAll(boolean state)
    {
        if (mItems == null)
        {
            return;
        }

        for (int i = 0; i < mItems.size(); i++)
        {
            mItems.get(i).isItemSelected = state;
        }

        this.notifyDataSetChanged();
    }

    public EntityUser getItem(int position)
    {
        return mItems.get(position);
    }

    public void clearItems()
    {
        mItems.clear();
        this.notifyDataSetChanged();
    }

    public void addUsers(List<EntityUser> users)
    {
        for (EntityUser user : users)
        {
            addItem(user);
        }
    }

    public void addItem(EntityUser user)
    {
        if (user == null)
        {
            return;
        }

        CollectedMapItem item = new CollectedMapItem();

        UtilUser.copyData(item, user);

        item.isMapCollected = false;
        item.isBleScanned = false;
        item.isItemSelected = false;
        item.btDevice = null;

        mItems.add(item);
        this.notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        int position;
        TextView nameTv;
        TextView earTv;
        TextView serialTv;
        TextView resultTv;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            nameTv = itemView.findViewById(R.id.item_share_collect_map_user_name);
            earTv = itemView.findViewById(R.id.item_share_collect_map_ear);
            serialTv = itemView.findViewById(R.id.item_share_collect_map_serial);
            resultTv = itemView.findViewById(R.id.item_share_collect_map_result);

            itemView.setOnClickListener(view ->
            {
                mShareFragment.listClickListenerCollectMap(getAdapterPosition());
            });
        }

        public void onBind(CollectedMapItem item, int position)
        {
            this.position = position;

            nameTv.setText(UtilUser.getNameOnly(item.name));
            earTv.setText(UtilUser.getEarKorean(item.ear));

            if (item.isBleScanned)
            {
                serialTv.setText(item.oteSerial);
            }
            else
            {
                serialTv.setText("-");
            }

            if (item.isMapCollected)
            {
                resultTv.setText("완료");
            }
            else
            {
                resultTv.setText("-");
            }

            if (item.isItemSelected)
            {
                nameTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.field_70));
                earTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.field_70));
                serialTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.field_70));
                resultTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.field_70));
            }
            else
            {
                nameTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.white_70));
                earTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.white_70));
                serialTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.white_70));
                resultTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.white_70));
            }
        }
    }

    static class CollectedMapItem extends EntityUser
    {
        boolean isItemSelected;
        boolean isBleScanned;
        boolean isMapCollected;
        String oteSerial;
        BluetoothDevice btDevice;
    }
}
