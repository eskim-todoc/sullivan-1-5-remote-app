package todoc.cochlear.remoteapp.list;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.fragment.ShareFragment;

public class ShareDistributeMapAdapter extends RecyclerView.Adapter<ShareDistributeMapAdapter.ViewHolder>
{
    static private final String TAG = "TODOC_" + ShareDistributeMapAdapter.class.getSimpleName();

    private final ArrayList<ShareMapItem> mShareMapItems;

    public ShareFragment mShareFragment;

    public ShareDistributeMapAdapter(ShareFragment fragment)
    {
        mShareMapItems = new ArrayList<>();

        if (mShareFragment == null)
        {
            mShareFragment = fragment;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_distribute_map, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        holder.onBind(mShareMapItems.get(position), position);
    }

    @Override
    public int getItemCount()
    {
        return mShareMapItems.size();
    }

    public int getSharedCount()
    {
        if (mShareMapItems.size() == 0)
        {
            return 0;
        }

        int count = 0;

        for (ShareMapItem item : mShareMapItems)
        {
            if (item.isShared)
            {
                count++;
            }
        }

        return count;
    }

    public boolean isShared(int position)
    {
        return mShareMapItems.get(position).isShared;
    }

    public void setShare(int position, boolean share)
    {
        mShareMapItems.get(position).isShared = share;
        this.notifyDataSetChanged();
    }

    public boolean isSelected(int position)
    {
        return mShareMapItems.get(position).isSelected;
    }

    public void setSelect(int position, boolean select)
    {
        mShareMapItems.get(position).isSelected = select;
        this.notifyDataSetChanged();
    }

    public String getSerial(int position)
    {
        return mShareMapItems.get(position).entityDevice.serialNumber;
    }

    public EntityUser getEntityUser(int position)
    {
        return mShareMapItems.get(position).entityUser;
    }

    public BluetoothDevice getBtDevice(int position)
    {
        return mShareMapItems.get(position).btDevice;
    }

    public EntityDevice getEntityDevice(int position)
    {
        return mShareMapItems.get(position).entityDevice;
    }

    public void allItemsNoSelect()
    {
        for (ShareMapItem item : mShareMapItems)
        {
            item.isSelected = false;
        }

        this.notifyDataSetChanged();
    }

    public void clearItems()
    {
        mShareMapItems.clear();
        this.notifyDataSetChanged();
    }

    public void addItem(String name, String ear, String serial, BluetoothDevice btDevice)
    {
        EntityUser entityUser = UtilUser.instance.getUserByName(name + "_" + ear);
        EntityDevice entityDevice = UtilDevice.instance.getDeviceBySerialNumber(serial);

        if (entityUser == null)
        {
            Log.d(TAG, "사용자 데이터베이스에 " + name + "_" + ear + " 정보가 등록되지 않았습니다.");
            return;
        }

        if (entityDevice == null)
        {
            Log.d(TAG, "외부기 데이터베이스에 " + serial + " 정보가 등록되지 않았습니다.");
            return;
        }

        ShareMapItem item = new ShareMapItem();

        item.entityUser = entityUser;
        item.entityDevice = entityDevice;
        item.btDevice = btDevice;
        item.isSelected = false;
        item.isShared = false;

        for (int i = 0; i < mShareMapItems.size(); i++)
        {
            if (btDevice.getAddress().equals(mShareMapItems.get(i).btDevice.getAddress()))
            {
                Log.d(TAG, "이미 추가된 정보입니다.");
                item = null;
                return;
            }
        }

        mShareMapItems.add(item);
        this.notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        int position;
        TextView nameTv;
        TextView earTv;
        TextView serialTv;
        TextView stateTv;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            nameTv = itemView.findViewById(R.id.item_share_distribute_map_user_name);
            earTv = itemView.findViewById(R.id.item_share_distribute_map_ear);
            serialTv = itemView.findViewById(R.id.item_share_distribute_ote_serial);
            stateTv = itemView.findViewById(R.id.item_share_distribute_state);

            itemView.setOnClickListener(view ->
            {
                int position = getAdapterPosition();
                ShareMapItem item = mShareMapItems.get(position);
                boolean newSelected = !item.isSelected;
                allItemsNoSelect();
                setSelect(position, newSelected);

                Log.d(TAG, "공유 목록의 아이템 클릭 : 번호 = " + position + ", 선택 = " + (!newSelected) + "->" + newSelected);
                mShareFragment.listClickListenerShareMap(position, item.entityUser, item.entityDevice, item.btDevice);
            });
        }

        public void onBind(ShareMapItem item, int position)
        {
            this.position = position;

            String user = item.entityUser.name.substring(0, item.entityUser.name.length() - 2);
            String ear = item.entityUser.ear;
            String serial = item.entityDevice.serialNumber;

            nameTv.setText(user);

            if (ear.equals(EntityUser.EAR_LEFT))
            {
                earTv.setText("왼쪽");
            }
            else
            {
                earTv.setText("오른쪽");
            }

            serialTv.setText(serial);

            if (item.isShared)
            {
                stateTv.setText("완료");
            }
            else
            {
                stateTv.setText("미완료");
            }

            if (item.isSelected)
            {
                nameTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.field_70));
                earTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.field_70));
                serialTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.field_70));
                stateTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.field_70));
            }
            else
            {
                nameTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.white_70));
                earTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.white_70));
                serialTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.white_70));
                stateTv.setBackgroundColor(mShareFragment.requireActivity().getColor(R.color.white_70));
            }
        }
    }

    static class ShareMapItem
    {
        EntityUser entityUser;
        EntityDevice entityDevice;
        BluetoothDevice btDevice;
        boolean isSelected;
        boolean isShared;
    }
}
