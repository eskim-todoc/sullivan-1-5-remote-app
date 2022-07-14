package todoc.cochlear.remoteapp.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.fragment.UserFragment;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder>
{
    private ArrayList<EntityUser> mItems;
    private UserFragment mUserFragment;

    public UserAdapter(UserFragment fragment)
    {
        mItems = new ArrayList<>();
        mUserFragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.ViewHolder holder, int position)
    {
        holder.onBind(mItems.get(position), position);
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
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

    public void addItem(EntityUser item)
    {
        if (item != null)
        {
            mItems.add(item);
            this.notifyDataSetChanged();
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        int position;
        TextView nameTv;
        TextView earTv;
        ImageView defaultUserIv;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            defaultUserIv = itemView.findViewById(R.id.default_image);
            nameTv = itemView.findViewById(R.id.name);
            earTv = itemView.findViewById(R.id.ear);

            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    int position = getAdapterPosition();
                    mUserFragment.mItemClickListener(position);
                }
            });
        }

        public void onBind(EntityUser item, int position)
        {
            this.position = position;

            if (item.defaultUser.equals(EntityUser.USER_DEFAULT))
            {
                defaultUserIv.setVisibility(View.VISIBLE);
            }
            else
            {
                defaultUserIv.setVisibility(View.INVISIBLE);
            }

            nameTv.setText(item.name);

            if (item.ear.equals(EntityUser.EAR_LEFT))
            {
                earTv.setText("왼쪽");
            }
            else
            {
                earTv.setText("오른쪽");
            }
        }
    }
}
