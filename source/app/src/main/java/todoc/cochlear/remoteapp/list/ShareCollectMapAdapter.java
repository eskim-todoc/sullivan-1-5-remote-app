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
import todoc.cochlear.remoteapp.fragment.ShareFragment;

public class ShareCollectMapAdapter extends RecyclerView.Adapter<ShareCollectMapAdapter.ViewHolder>
{
    private final ArrayList<CollectedMapItem> mCollectedMapItems;

    public ShareCollectMapAdapter(ShareFragment fragment)
    {
        mCollectedMapItems = new ArrayList<>();
        if (mShareFragment == null)
        {
            mShareFragment = fragment;
        }
    }

    public ShareFragment mShareFragment;

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
        holder.onBind(mCollectedMapItems.get(position), position);
    }

    @Override
    public int getItemCount()
    {
        return mCollectedMapItems.size();
    }

    public int getCollectedCount()
    {
        if (mCollectedMapItems.size() == 0)
        {
            return 0;
        }

        int count = 0;

        for (CollectedMapItem item : mCollectedMapItems)
        {
            if (item.isCollected)
            {
                count++;
            }
        }

        return count;
    }

    public boolean isCollected(int position)
    {
        return mCollectedMapItems.get(position).isCollected;
    }

    public void setCollect(int position)
    {
        mCollectedMapItems.get(position).isCollected = true;
        this.notifyDataSetChanged();
    }

    public void setSerial(int position, String serial)
    {
        mCollectedMapItems.get(position).isScanned = true;
        mCollectedMapItems.get(position).serial = serial;
        this.notifyDataSetChanged();
    }

    public String getSerial(int position)
    {
        return mCollectedMapItems.get(position).serial;
    }

    public String getName(int position)
    {
        return mCollectedMapItems.get(position).name.substring(0, mCollectedMapItems.get(position).name.length() - 2);
    }

    public String getEar(int position)
    {
        return mCollectedMapItems.get(position).ear;
    }

    public boolean isScanned(int position)
    {
        return mCollectedMapItems.get(position).isScanned;
    }

    public void setScanned(int position, boolean state)
    {
        mCollectedMapItems.get(position).isScanned = state;
    }

    public boolean isSelected(int position)
    {
        return mCollectedMapItems.get(position).isSelected;
    }

    public void setSelect(int position, boolean select)
    {
        mCollectedMapItems.get(position).isSelected = select;
        this.notifyDataSetChanged();
    }

    public void setBtDevice(int position, BluetoothDevice btDevice)
    {
        mCollectedMapItems.get(position).btDevice = btDevice;
    }

    public BluetoothDevice getBtDevice(int position)
    {
        return mCollectedMapItems.get(position).btDevice;
    }

    public void allItemNoSelect()
    {
        for (CollectedMapItem item : mCollectedMapItems)
        {
            item.isSelected = false;
        }

        this.notifyDataSetChanged();
    }

    public EntityUser getItem(int position)
    {
        return (EntityUser) mCollectedMapItems.get(position);
    }

    public List<EntityUser> getCollectedItems()
    {
        List<EntityUser> users = new ArrayList<>();

        for (CollectedMapItem item : mCollectedMapItems)
        {
            if (item.isCollected)
            {
                users.add((EntityUser) item);
            }
        }

        return users;
    }

    public void clearItems()
    {
        mCollectedMapItems.clear();
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
        if (user != null)
        {
            CollectedMapItem item = new CollectedMapItem();
            item.name = user.name;
            item.ear = user.ear;
            item.passKey = user.passKey;
            item.nickname = user.nickname;
            item.defaultUser = user.defaultUser;
            item.isCollected = false;
            item.isScanned = false;
            item.isSelected = false;
            item.btDevice = null;

            mCollectedMapItems.add(item);
            this.notifyDataSetChanged();
        }
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
                int position = getAdapterPosition();
                boolean selected = mCollectedMapItems.get(position).isSelected;

                allItemNoSelect();
                setSelect(position, !selected);
                mShareFragment.listClickListenerCollectMap(position);
            });
        }

        public void onBind(CollectedMapItem item, int position)
        {
            this.position = position;

            nameTv.setText(item.name.substring(0, item.name.length() - 2));

            if (item.ear.equals(EntityUser.EAR_LEFT))
            {
                earTv.setText("왼쪽");
            }
            else
            {
                earTv.setText("오른쪽");
            }

            if (item.isScanned)
            {
                serialTv.setText(item.serial);
            }
            else
            {
                serialTv.setText("-");
            }

            if (item.isCollected)
            {
                resultTv.setText("완료");
            }
            else
            {
                resultTv.setText("-");
            }

            if (item.isSelected)
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
        boolean isSelected;
        boolean isScanned;
        boolean isCollected;
        String serial;
        BluetoothDevice btDevice;
    }
}
