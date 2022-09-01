package todoc.cochlear.remoteapp.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.params.Status;

public class ShareSelectUserAdapter extends RecyclerView.Adapter<ShareSelectUserAdapter.ViewHolder>
{
    private final ArrayList<SelectUserItem> mSelectUserItems;

    public ShareSelectUserAdapter()
    {
        mSelectUserItems = new ArrayList<>();
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
        holder.onBind(mSelectUserItems.get(position), position);
    }

    @Override
    public int getItemCount()
    {
        return mSelectUserItems.size();
    }

    public EntityUser getItem(int position)
    {
        return (EntityUser) mSelectUserItems.get(position);
    }

    public ArrayList<EntityUser> getSelectedItems()
    {
        ArrayList<EntityUser> users = new ArrayList<>();

        for (SelectUserItem item : mSelectUserItems)
        {
            if (item.selected)
            {
                users.add((EntityUser) item);
            }
        }

        return users;
    }

    public void clearItems()
    {
        mSelectUserItems.clear();
        this.notifyDataSetChanged();
    }

    public void addUsers(ArrayList<EntityUser> users)
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
            SelectUserItem item = new SelectUserItem();
            item.name = user.name;
            item.ear = user.ear;
            item.nickname = user.nickname;
            item.defaultUser = user.defaultUser;

            if (Status.instance().connectionState != Status.CONNECTION_STATE_DISCONNECTED)
            {
                if (item.defaultUser.equals(EntityUser.USER_DEFAULT))
                {
                    item.selected = true;
                }
            }

            mSelectUserItems.add(item);
            this.notifyDataSetChanged();
        }
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
        }

        public void onBind(SelectUserItem item, int position)
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

            if (item.selected)
            {
                selectCb.setChecked(true);
            }
        }
    }

    static class SelectUserItem extends EntityUser
    {
        boolean selected;
    }
}
