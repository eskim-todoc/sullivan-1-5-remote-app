package todoc.cochlear.remoteapp.list;

import android.content.Context;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;

public class DeviceAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    public interface Host
    {
        Context requireContext();
        String saveDevice(EntityDevice original, EntityDevice updated);
        void confirmDeleteDevice(EntityDevice device, Runnable deleted);
        void onDeviceRowsChanged();
    }

    private static final int TYPE_DEVICE = 0;
    private static final int TYPE_ADD = 1;
    private final ArrayList<EntityDevice> items = new ArrayList<>();
    private final Host host;
    private InlineListUi.DialogForm activeForm;

    public DeviceAdapter(Host host)
    {
        this.host = host;
    }

    @Override
    public int getItemViewType(int position)
    {
        return position == items.size() ? TYPE_ADD : TYPE_DEVICE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if (viewType == TYPE_ADD)
        {
            return new FooterHolder(InlineListUi.footer(parent, "새 외부기 추가", view -> openEditor(null)));
        }
        return new DeviceHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        if (holder instanceof DeviceHolder)
        {
            ((DeviceHolder) holder).bind(items.get(position));
        }
    }

    @Override
    public int getItemCount()
    {
        return items.size() + 1;
    }

    public int getRegisteredCount()
    {
        return items.size();
    }

    public void setItems(List<EntityDevice> devices)
    {
        items.clear();
        if (devices != null)
        {
            for (EntityDevice device : devices)
            {
                items.add(copy(device));
            }
        }
        notifyDataSetChanged();
    }

    public void dismissEditor()
    {
        if (activeForm != null)
        {
            activeForm.dismiss();
            activeForm = null;
        }
    }

    private void openEditor(EntityDevice device)
    {
        dismissEditor();
        EntityDevice original = device == null ? null : UtilDevice.instance.getDeviceBySerialNumber(device.serialNumber);
        if (device != null && original == null)
        {
            setItems(UtilDevice.instance.getDevices());
            host.onDeviceRowsChanged();
            return;
        }
        InlineListUi.DialogForm form = new InlineListUi.DialogForm(host.requireContext(),
            original == null ? "외부기 등록" : "외부기 수정", original != null);
        activeForm = form;
        InlineListUi.Field serial = InlineListUi.field(form.editor, "제조번호", 0, false);
        InlineListUi.Field alias = InlineListUi.field(form.editor, "별칭", 0, false);
        InlineListUi.Field pairing = InlineListUi.field(form.editor, "페어링 키", 6, true);
        InlineListUi.identify(serial.input, R.id.edit_device_serial_edittext, "외부기 제조번호");
        InlineListUi.identify(alias.input, R.id.edit_device_option_edittext, "외부기 별칭");
        InlineListUi.identify(pairing.input, R.id.edit_devie_pairing_key_edittext, "외부기 페어링 키");
        pairing.active.setContentDescription("외부기 자동 연결 사용");
        pairing.input.setInputType(InputType.TYPE_CLASS_NUMBER);
        pairing.input.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
        serial.input.setText(original == null ? "" : original.serialNumber);
        alias.input.setText(original == null ? "" : original.additionalInformation);
        pairing.input.setText(original == null ? "" : original.pairingKey);
        pairing.active.setChecked(original == null || original.isActive);
        if (original != null)
        {
            serial.input.setKeyListener(null);
            serial.input.setFocusable(false);
            serial.input.setFocusableInTouchMode(false);
            serial.input.setTextColor(0xffe1ebec);
        }
        form.show("외부기", () ->
        {
            EntityDevice updated = new EntityDevice();
            updated.serialNumber = original == null ? InlineListUi.value(serial.input).trim() : original.serialNumber;
            updated.additionalInformation = InlineListUi.value(alias.input);
            updated.pairingKey = InlineListUi.value(pairing.input);
            updated.isActive = pairing.active.isChecked();
            String failure = host.saveDevice(original, updated);
            if (failure != null)
            {
                View target = !updated.pairingKey.matches("[0-9]{6}") ? pairing.input
                    : original == null && failure.contains("제조번호") ? serial.input : alias.input;
                form.showError(failure, target);
                return;
            }
            EntityDevice stored = UtilDevice.instance.getDeviceBySerialNumber(updated.serialNumber);
            if (stored == null)
            {
                form.showError("저장된 등록 정보를 읽지 못했습니다. 다시 확인하세요.", alias.input);
                return;
            }
            int position = indexOf(updated.serialNumber);
            if (position < 0)
            {
                position = items.size();
                items.add(copy(stored));
                notifyItemInserted(position);
            }
            else
            {
                items.set(position, copy(stored));
                notifyItemChanged(position);
            }
            host.onDeviceRowsChanged();
            form.dismiss();
        }, () ->
        {
            if (original != null)
            {
                host.confirmDeleteDevice(copy(original), () ->
                {
                    int position = indexOf(original.serialNumber);
                    if (position >= 0)
                    {
                        items.remove(position);
                        notifyItemRemoved(position);
                        host.onDeviceRowsChanged();
                    }
                    form.dismiss();
                });
            }
        }, () ->
        {
            if (activeForm == form)
            {
                activeForm = null;
            }
        });
    }

    private int indexOf(String serial)
    {
        for (int index = 0; index < items.size(); index++)
        {
            if (serial.equals(items.get(index).serialNumber))
            {
                return index;
            }
        }
        return -1;
    }

    private static EntityDevice copy(EntityDevice source)
    {
        if (source == null)
        {
            return null;
        }
        EntityDevice copy = new EntityDevice();
        copy.serialNumber = source.serialNumber;
        copy.additionalInformation = source.additionalInformation;
        copy.pairingKey = source.pairingKey;
        copy.isActive = source.isActive;
        return copy;
    }

    private static final class FooterHolder extends RecyclerView.ViewHolder
    {
        FooterHolder(View view)
        {
            super(view);
        }
    }

    private final class DeviceHolder extends RecyclerView.ViewHolder
    {
        private final TextView serial;
        private final TextView alias;
        private final TextView state;
        private EntityDevice device;

        DeviceHolder(View view)
        {
            super(view);
            serial = view.findViewById(R.id.serial);
            alias = view.findViewById(R.id.device_alias);
            state = view.findViewById(R.id.device_active_status);
            view.setOnClickListener(clicked ->
            {
                if (device != null)
                {
                    openEditor(device);
                }
            });
            state.setOnClickListener(clicked ->
            {
                if (device != null)
                {
                    openEditor(device);
                }
            });
        }

        void bind(EntityDevice value)
        {
            device = value;
            serial.setText(value.serialNumber);
            String label = value.additionalInformation == null ? "" : value.additionalInformation.trim();
            alias.setText(label.isEmpty() ? "—" : label);
            alias.setTextColor(itemView.getResources().getColor(label.isEmpty()
                ? R.color.glass_muted_text : R.color.white_90, itemView.getContext().getTheme()));
            state.setText(value.isActive ? "사용" : "중지");
            state.setActivated(value.isActive);
            state.setTextColor(itemView.getResources().getColor(value.isActive
                ? R.color.glass_accent_text : R.color.glass_muted_text, itemView.getContext().getTheme()));
            state.setContentDescription("외부기 " + value.serialNumber + (value.isActive ? " 사용. 눌러 수정" : " 중지. 눌러 수정"));
            itemView.setContentDescription("외부기 " + value.serialNumber + ". 눌러 수정");
        }
    }
}
