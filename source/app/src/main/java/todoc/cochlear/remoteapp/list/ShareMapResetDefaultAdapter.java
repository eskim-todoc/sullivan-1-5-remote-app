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

public class ShareMapResetDefaultAdapter extends RecyclerView.Adapter<ShareMapResetDefaultAdapter.ViewHolder>
{
    static private final String TAG = "TODOC_" + ShareMapResetDefaultAdapter.class.getSimpleName();

    private final ArrayList<MapResetDefaultItem> mMapResetDefaultItems;

    public ShareFragment mShareFragment;

    public ShareMapResetDefaultAdapter(ShareFragment fragment)
    {
        mMapResetDefaultItems = new ArrayList<>();

        if (mShareFragment == null)
        {
            mShareFragment = fragment;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_map_reset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        holder.onBind(mMapResetDefaultItems.get(position), position);
    }

    @Override
    public int getItemCount()
    {
        return mMapResetDefaultItems.size();
    }

    public int getResetDoneCount()
    {
        if (mMapResetDefaultItems.size() == 0)
        {
            return 0;
        }

        int count = 0;

        for (MapResetDefaultItem item : mMapResetDefaultItems)
        {
            if (item.isResetDone)
            {
                count++;
            }
        }

        return count;
    }

    public boolean isResetDone(int position)
    {
        return mMapResetDefaultItems.get(position).isResetDone;
    }

    public void setResetDone(int position, boolean share)
    {
        mMapResetDefaultItems.get(position).isResetDone = share;
        this.notifyDataSetChanged();
    }

    public boolean isSelected(int position)
    {
        return mMapResetDefaultItems.get(position).isSelected;
    }

    public void setSelect(int position, boolean select)
    {
        mMapResetDefaultItems.get(position).isSelected = select;
        this.notifyDataSetChanged();
    }

    public String getSerial(int position)
    {
        return mMapResetDefaultItems.get(position).entityDevice.serialNumber;
    }

    public String getSimpleName(int position)
    {
        String name = mMapResetDefaultItems.get(position).entityUser.name;

        return name.substring(0, name.length() - 2);
    }

    public String getEar(int position)
    {
        return mMapResetDefaultItems.get(position).entityUser.ear;
    }

    public EntityUser getEntityUser(int position)
    {
        return mMapResetDefaultItems.get(position).entityUser;
    }

    public BluetoothDevice getBtDevice(int position)
    {
        return mMapResetDefaultItems.get(position).btDevice;
    }

    public EntityDevice getEntityDevice(int position)
    {
        return mMapResetDefaultItems.get(position).entityDevice;
    }

    public void allItemsNoSelect()
    {
        for (MapResetDefaultItem item : mMapResetDefaultItems)
        {
            item.isSelected = false;
        }

        this.notifyDataSetChanged();
    }

    public void clearItems()
    {
        mMapResetDefaultItems.clear();
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

        MapResetDefaultItem item = new MapResetDefaultItem();

        item.entityUser = entityUser;
        item.entityDevice = entityDevice;
        item.btDevice = btDevice;
        item.isSelected = false;
        item.isResetDone = false;

        for (int i = 0; i < mMapResetDefaultItems.size(); i++)
        {
            if (btDevice.getAddress().equals(mMapResetDefaultItems.get(i).btDevice.getAddress()))
            {
                Log.d(TAG, "이미 추가된 정보입니다.");
                item = null;
                return;
            }
        }

        if (mMapResetDefaultItems.size() == getResetDoneCount())
        {
            mMapResetDefaultItems.add(item);
            mShareFragment.scanListUpdateMapReset();
        }
        else
        {
            mMapResetDefaultItems.add(item);
        }

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

            nameTv = itemView.findViewById(R.id.item_share_map_reset_name);
            earTv = itemView.findViewById(R.id.item_share_map_reset_ear);
            serialTv = itemView.findViewById(R.id.item_share_map_reset_serial);
            stateTv = itemView.findViewById(R.id.item_share_map_reset_result);

            itemView.setOnClickListener(view ->
            {
                int position = getAdapterPosition();
                MapResetDefaultItem item = mMapResetDefaultItems.get(position);
                boolean newSelected = !item.isSelected;
                allItemsNoSelect();
                setSelect(position, newSelected);

                Log.d(TAG, "맵 초기화 목록의 아이템 클릭 : 번호 = " + position + ", 선택 = " + (!newSelected) + "->" + newSelected);
                mShareFragment.listClickListenerMapReset(position, item.entityUser, item.entityDevice, item.btDevice);
            });
        }

        public void onBind(MapResetDefaultItem item, int position)
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

            if (item.isResetDone)
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

    static class MapResetDefaultItem
    {
        EntityUser entityUser;
        EntityDevice entityDevice;
        BluetoothDevice btDevice;
        boolean isSelected;
        boolean isResetDone;
    }
}
