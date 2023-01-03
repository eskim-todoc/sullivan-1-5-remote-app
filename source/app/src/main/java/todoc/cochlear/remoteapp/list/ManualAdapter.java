package todoc.cochlear.remoteapp.list;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.fragment.ManualFragment;

public class ManualAdapter extends RecyclerView.Adapter<ManualAdapter.ViewHolder>
{
    //private final ArrayList<Drawable[]> mItems;
    //private final ArrayList<Drawable> mItems;
    private final ArrayList<Integer> mItems;
    private final ManualFragment mFragment;

    public ManualAdapter(ManualFragment fragment)
    {
        mItems = new ArrayList<>();
        mFragment = fragment;
    }

    @Override
    public int getItemCount()
    {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        //View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manual, parent, false);
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manual_document, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        holder.onBind(mItems.get(position));
    }

    //public void addItem(Drawable[] item)
    //public void addItem(Drawable item)
    public void addItem(int item)
    {
        /*
        if (item != null)
        {
            mItems.add(item);
            notifyDataSetChanged();
        }
        */
        mItems.add(item);
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {
        //ImageView title;
        ImageView body;

        ScaleGestureDetector mScaleGestureDetector;
        float mScaleFactor = 1.0f;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            mScaleGestureDetector = new ScaleGestureDetector(mFragment.requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener()
            {
                @Override
                public boolean onScale(ScaleGestureDetector detector)
                {
                    // ScaleGestureDetector에서 factor를 받아 변수로 선언한 factor에 넣고
                    mScaleFactor *= detector.getScaleFactor();

                    // 최대 10배, 최소 10배 줌 한계 설정
                    mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));

                    // 이미지뷰 스케일에 적용
                    if (body != null)
                    {
                        body.setScaleX(mScaleFactor);
                        body.setScaleY(mScaleFactor);
                    }

                    return true;
                    //return super.onScale(detector);
                }
            });

            //title = itemView.findViewById(R.id.item_manual_title);
            //body = itemView.findViewById(R.id.item_manual_body);
            body = itemView.findViewById(R.id.item_manual_document_iv);

            //title.setClickable(true);
            body.setClickable(true);

            //title.setOnTouchListener(mTouchListener);
            body.setOnTouchListener(mTouchListener);
        }

        //public void onBind(Drawable[] item)
        //public void onBind(Drawable item)
        public void onBind(int item)
        {
            //title.setImageDrawable(item[0]);
            //body.setImageDrawable(item[1]);
            //body.setImageDrawable(item);
            body.setImageDrawable(AppCompatResources.getDrawable(mFragment.requireContext(), item));
        }

        @SuppressLint("ClickableViewAccessibility")
        View.OnTouchListener mTouchListener = (view, motionEvent) ->
        {

            //mScaleGestureDetector.onTouchEvent(motionEvent);

            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
            {
                mFragment.onTouchEvent();
                return true;
            }

            //return true;
            return false;
        };
    }
}
