package todoc.cochlear.remoteapp.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.maps.EntityMap;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.params.MapInfo;

public class ShareExistMapAdapter extends RecyclerView.Adapter<ShareExistMapAdapter.ViewHolder>
{
    private final ArrayList<ExistMapItem> mItems;

    public ShareExistMapAdapter()
    {
        mItems = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_exist_map, parent, false);
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

    public void clearItems()
    {
        mItems.clear();
        this.notifyDataSetChanged();
    }

    public EntityUser getUser(int position)
    {
        String name = mItems.get(position).name;
        String ear = mItems.get(position).ear;
        String primaryKey = EntityMap.makePrimaryKey(name, ear);

        return UtilUser.instance.getUserByName(primaryKey);
    }

    public void addMaps(List<EntityMap> maps)
    {
        for (EntityMap map : maps)
        {
            addItem(map);
        }
    }

    public void addItem(EntityMap map)
    {
        if (!map.name.equals(MapInfo.EMPTY_MAP_NAME))
        {
            ExistMapItem item = new ExistMapItem();

            int year, month, day, hour, minute, second;

            second = (int) (map.stamp & 0xffL);         // long to int
            minute = (int) ((map.stamp >> 8) & 0xffL);  // long to int
            hour = (int) ((map.stamp >> 16) & 0xffL);   // long to int
            day = (int) ((map.stamp >> 24) & 0xffL);    // long to int
            month = (int) ((map.stamp >> 32) & 0xffL);  // long to int
            year = (int) ((map.stamp >> 40) & 0xffL);   // long to int

            item.date = String.format(Locale.ENGLISH, "%4d-%02d-%02d\n%02d:%02d:%02d", 2000 + year, month, day, hour, minute, second);
            item.name = map.name;
            item.ear = map.ear;

            mItems.add(item);
            this.notifyDataSetChanged();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        int position;
        TextView nameTv;
        TextView earTv;
        TextView dateTv;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            nameTv = itemView.findViewById(R.id.item_share_exist_map_name);
            earTv = itemView.findViewById(R.id.item_share_exist_map_ear);
            dateTv = itemView.findViewById(R.id.item_share_exist_map_date);
        }

        public void onBind(ExistMapItem item, int position)
        {
            this.position = position;

            earTv.setText(UtilUser.getEarKorean(item.ear));
            nameTv.setText(item.name);
            dateTv.setText(item.date);
        }
    }

    static class ExistMapItem
    {
        String name;
        String ear;
        String date;
    }
}
