package todoc.cochlear.remoteapp.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.params.Status;

public class ShareSelectUserAdapter extends RecyclerView.Adapter<ShareSelectUserAdapter.ViewHolder>
{
    private final ArrayList<SelectUserItem> mItems;

    public ShareSelectUserAdapter()
    {
        mItems = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share_select_user, parent, false);
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

    public int getSelectedUserCount()
    {
        int count = 0;

        if (0 < mItems.size())
        {
            for (int i = 0; i < mItems.size(); i++)
            {
                if (mItems.get(i).isSelected)
                {
                    count++;
                }
            }
        }

        return count;
    }

    public List<EntityUser> getSelectedUsers()
    {
        List<EntityUser> users = new ArrayList<>();

        for (SelectUserItem item : mItems)
        {
            if (item.isSelected)
            {
                users.add(item);
            }
        }

        return users;
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

        SelectUserItem item = new SelectUserItem();
        item.isSelected = false;
        UtilUser.copyData(item, user);

        if (Status.instance().connectionState != Status.CONNECTION_STATE_DISCONNECTED
                && Status.instance().connectedUser != null
                && Status.instance().connectedUser.name.equals(user.name))
        {
            item.isSelected = true;
        }

        mItems.add(item);
        this.notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        int position;
        TextView nameTv;
        TextView earTv;
        CheckBox selectCb;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            nameTv = itemView.findViewById(R.id.item_share_select_use_name);
            earTv = itemView.findViewById(R.id.item_share_select_user_ear);
            selectCb = itemView.findViewById(R.id.item_share_select_user_checkbox);

            selectCb.setOnCheckedChangeListener((compoundButton, b) ->
            {
                int position = getAdapterPosition();
                mItems.get(position).isSelected = b;
            });
        }

        public void onBind(SelectUserItem item, int position)
        {
            this.position = position;

            nameTv.setText(UtilUser.getNameOnly(item.name));
            earTv.setText(UtilUser.getEarKorean(item.ear));

            if (item.isSelected)
            {
                selectCb.setChecked(true);
            }
        }
    }

    static class SelectUserItem extends EntityUser
    {
        boolean isSelected;
    }
}
