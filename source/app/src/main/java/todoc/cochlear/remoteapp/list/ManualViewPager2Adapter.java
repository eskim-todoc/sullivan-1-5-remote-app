package todoc.cochlear.remoteapp.list;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.fragment.ManualFragment;

public class ManualViewPager2Adapter extends RecyclerView.Adapter<ManualViewPager2Adapter.ViewHolder>
{
    private final ManualFragment mFragment;
    private final ArrayList<Drawable> mItems;

    public ManualViewPager2Adapter(ManualFragment fragment)
    {
        mFragment = fragment;
        mItems = new ArrayList<>();
    }

    @NonNull
    @Override
    public ManualViewPager2Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manual_viewpager2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        holder.onBind(mItems.get(position));
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
    }

    public void addItem(@NonNull Drawable drawable)
    {
        mItems.add(drawable);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        private ImageView mIv;

        public ViewHolder(@NonNull View view)
        {
            super(view);
            mIv = view.findViewById(R.id.item_manual_viewpager2_iv);
            mIv.setOnTouchListener(mTouchListener);
        }

        public void onBind(Drawable drawable)
        {
            mIv.setImageDrawable(drawable);
        }

        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener mTouchListener = (view, motionEvent) ->
        {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
            {
                mFragment.onTouchEvent();
                return true;
            }

            return false;
        };
    }
}
