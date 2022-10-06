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

    private final ArrayList<MapResetDefaultItem> mItems;

    public ShareFragment mShareFragment;

    public ShareMapResetDefaultAdapter(ShareFragment fragment)
    {
        mItems = new ArrayList<>();

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
        holder.onBind(mItems.get(position), position);
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
    }

    public int getResetDoneCount()
    {
        if (mItems.size() == 0)
        {
            return 0;
        }

        int count = 0;

        for (MapResetDefaultItem item : mItems)
        {
            if (item.isMapResetDone)
            {
                count++;
            }
        }

        return count;
    }

    public boolean isMapResetDone(int position)
    {
        return mItems.get(position).isMapResetDone;
    }

    public void setResetDone(int position, boolean share)
    {
        mItems.get(position).isMapResetDone = share;
        this.notifyDataSetChanged();
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

    public String getOteSerial(int position)
    {
        return mItems.get(position).entityDevice.serialNumber;
    }

    public String getNameWithEar(int position)
    {
        return mItems.get(position).entityUser.name;
    }

    public String getEar(int position)
    {
        return mItems.get(position).entityUser.ear;
    }

    public EntityUser getEntityUser(int position)
    {
        return mItems.get(position).entityUser;
    }

    public BluetoothDevice getBtDevice(int position)
    {
        return mItems.get(position).btDevice;
    }

    public EntityDevice getEntityDevice(int position)
    {
        return mItems.get(position).entityDevice;
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

    public void clearItems()
    {
        mItems.clear();
        this.notifyDataSetChanged();
    }

    public void addItem(String nameWithEar, String serial, BluetoothDevice btDevice)
    {
        EntityUser entityUser = UtilUser.instance.getUserByName(nameWithEar);
        EntityDevice entityDevice = UtilDevice.instance.getDeviceBySerialNumber(serial);

        if (entityUser == null)
        {
            Log.d(TAG, "사용자 데이터베이스에 " + nameWithEar + " 정보가 없습니다.");
            return;
        }

        if (entityDevice == null)
        {
            Log.d(TAG, "외부기 데이터베이스에 " + serial + " 정보가 없습니다.");
            return;
        }

        MapResetDefaultItem item = new MapResetDefaultItem();

        item.entityUser = entityUser;
        item.entityDevice = entityDevice;
        item.btDevice = btDevice;
        item.isItemSelected = false;
        item.isMapResetDone = false;

        for (int i = 0; i < mItems.size(); i++)
        {
            if (btDevice.getAddress().equals(mItems.get(i).btDevice.getAddress())
                    && entityUser.name.equals(mItems.get(i).entityUser.name)
                    && entityDevice.serialNumber.equals(mItems.get(i).entityDevice.serialNumber))
            {
                Log.v(TAG, "이미 맵 초기화 어댑터에 추가된 정보입니다.");
                item = null;
                return;
            }
        }

        mItems.add(item);
        mShareFragment.scanListUpdateMapReset();

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
                mShareFragment.listClickListenerMapReset(getAdapterPosition());
            });
        }

        public void onBind(MapResetDefaultItem item, int position)
        {
            this.position = position;

            nameTv.setText(UtilUser.getNameOnly(item.entityUser.name));
            earTv.setText(UtilUser.getEarKorean(item.entityUser.ear));
            serialTv.setText(item.entityDevice.serialNumber);

            if (item.isMapResetDone)
            {
                stateTv.setText("완료");
            }
            else
            {
                stateTv.setText("미완료");
            }

            if (item.isItemSelected)
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
        boolean isItemSelected;
        boolean isMapResetDone;
    }
}
