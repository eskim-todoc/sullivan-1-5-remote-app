package todoc.cochlear.remoteapp.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.Device;
import todoc.cochlear.remoteapp.fragment.DeviceListFragment;
import todoc.cochlear.remoteapp.params.AppParam;
import todoc.cochlear.remoteapp.params.DeviceParam;

public class DeviceListItemAdapter extends BaseAdapter
{
      ArrayList<Device> mItems;
      DeviceListFragment mFragment;

      public DeviceListItemAdapter(DeviceListFragment fragment)
      {
            mItems = new ArrayList<>();
            mFragment = fragment;
      }

      @Override
      public int getCount()
      {
            return mItems.size();
      }

      @Override
      public long getItemId(int i)
      {
            return i;
      }

      @Override
      public Object getItem(int i)
      {
            return mItems.get(i);
      }

      @Override
      public View getView(int i, View view, ViewGroup viewGroup)
      {
            DeviceListItemViewHolder viewHolder;

            if (view == null)
            {
                  view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_device_list, viewGroup, false);

                  viewHolder = new DeviceListItemViewHolder();

                  viewHolder.modelImage = view.findViewById(R.id.device_list_model_image);
                  viewHolder.nameText = view.findViewById(R.id.device_list_device_name);
                  viewHolder.userText = view.findViewById(R.id.device_list_user_name);
                  viewHolder.mapDateText = view.findViewById(R.id.device_list_map_date);
                  viewHolder.modelText = view.findViewById(R.id.device_list_model);
                  viewHolder.serialText = view.findViewById(R.id.device_list_serial);
                  viewHolder.deleteButton = view.findViewById(R.id.device_list_remove);

                  view.setTag(viewHolder);
            }

            viewHolder = (DeviceListItemViewHolder) view.getTag();

            Device item = mItems.get(i);

            // Model image view.
            if (item.getDeviceModel().equals(DeviceParam.DEVICE_MODEL_TD2))
            {
                  viewHolder.modelImage.setImageDrawable(ContextCompat.getDrawable(viewGroup.getContext(), R.drawable.drawable_model_td2));
            }
            else
            {
                  viewHolder.modelImage.setImageDrawable(ContextCompat.getDrawable(viewGroup.getContext(), R.drawable.drawable_model_unknown));
            }

            // Name, User, Model, Serial text view.
            viewHolder.nameText.setText(item.getDeviceName());
            viewHolder.userText.setText(item.getImplantUserName());
            viewHolder.modelText.setText(item.getDeviceModel());
            viewHolder.serialText.setText(item.getDeviceSerial());

            String mapData;
            String[] mapDatas = item.getMapDate().split("/");

            if (AppParam.getInstance().isKorean)
            {
                  mapData = "20" + mapDatas[0] + "." + mapDatas[1] + "." + mapDatas[2] + " " + mapDatas[3] + ":" + mapDatas[4];
            }
            else
            {
                  mapData = mapDatas[2] + " " + mapDatas[1] + " " + "20" + mapDatas[0] + ", " + mapDatas[3] + ":" + mapDatas[4];
            }

            // Map date text view is depends on language.
            viewHolder.mapDateText.setText(mapData);

            // Delete button on click listener.
            viewHolder.deleteButton.setOnClickListener(new View.OnClickListener()
            {
                  @Override
                  public void onClick(View view)
                  {
                        View parentRow = (View) view.getParent();
                        ListView listView = (ListView) parentRow.getParent();
                        int position = listView.getPositionForView(parentRow);
                        Device device = mItems.get(position);

                        // Pass Device instance to device list fragment to process using fragment's on click listener.
                        mFragment.onButtonClicked(device);
                  }
            });

            return view;
      }

      /**
       * Clear.
       */
      public void clearAllItem()
      {
            mItems.clear();
            this.notifyDataSetChanged();
      }

      /**
       * Add.
       */
      public void addItem(Device item)
      {
            if (item != null)
            {
                  mItems.add(item);
                  this.notifyDataSetChanged();
            }
      }

      /**
       * View holder class.
       */
      static private class DeviceListItemViewHolder
      {
            ImageView modelImage;
            TextView nameText;
            TextView userText;
            TextView mapDateText;
            TextView modelText;
            TextView serialText;
            ImageButton deleteButton;
      }
}
