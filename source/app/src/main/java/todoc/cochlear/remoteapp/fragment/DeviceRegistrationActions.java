package todoc.cochlear.remoteapp.fragment;

import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.Objects;

import todoc.cochlear.remoteapp.common.BooleanCondition;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.database.devices.EntityDevice;
import todoc.cochlear.remoteapp.database.devices.UtilDevice;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder;

final class DeviceRegistrationActions
{
    private final MainActivity activity;
    private final BooleanCondition attached;
    private AlertDialog deleteDialog;

    DeviceRegistrationActions(MainActivity activity, BooleanCondition attached)
    {
        this.activity = activity;
        this.attached = attached;
    }

    void close()
    {
        if (deleteDialog != null)
        {
            deleteDialog.dismiss();
            deleteDialog = null;
        }
    }

    /**
     * Existing device serials keep their database identity; form drafts are never passed to the storage API.
     * A null result confirms that both details and the requested activation were read back successfully.
     */
    String saveDevice(EntityDevice original, EntityDevice updated)
    {
        if (updated.serialNumber == null || updated.serialNumber.trim().isEmpty())
        {
            return "제조번호를 입력하세요.";
        }
        if (updated.pairingKey == null || !updated.pairingKey.matches("[0-9]{6}"))
        {
            return "페어링 키는 숫자 6자리로 입력하세요.";
        }
        if (original != null && activity.isDeviceInUse(original.serialNumber))
        {
            return "외부기의 연결을 먼저 해제한 뒤 수정하세요.";
        }
        EntityDevice stored = UtilDevice.instance.getDeviceBySerialNumber(updated.serialNumber);
        if (original == null && stored != null)
        {
            return "이미 등록된 제조번호입니다.";
        }
        if (original != null && (!Objects.equals(original.serialNumber, updated.serialNumber)
            || !sameDetails(original, stored) || original.isActive != stored.isActive))
        {
            return "등록 정보가 변경되었습니다. 목록에서 다시 확인하세요.";
        }

        EntityDevice write = copy(updated);
        if (original == null)
        {
            UtilDevice.instance.insert(write);
        }
        else
        {
            UtilDevice.instance.update(write);
        }
        EntityDevice readback = UtilDevice.instance.getDeviceBySerialNumber(updated.serialNumber);
        if (!sameDetails(updated, readback))
        {
            return "저장하지 못했습니다. 입력은 그대로 유지됩니다.";
        }
        if (!activity.setDeviceActive(updated.serialNumber, updated.isActive))
        {
            boolean restored = restore(original, updated);
            return restored ? "사용 여부를 저장하지 못했습니다. 입력은 그대로 유지됩니다."
                : "일부 정보가 저장되었습니다. 연결 상태와 등록 정보를 다시 확인하세요.";
        }
        readback = UtilDevice.instance.getDeviceBySerialNumber(updated.serialNumber);
        if (!sameDetails(updated, readback) || readback.isActive != updated.isActive)
        {
            return "저장 결과를 확인하지 못했습니다. 입력은 그대로 유지됩니다.";
        }
        UtilLog.instance.writeLog((original == null ? "외부기 등록 : " : "외부기 수정 : ") + updated.serialNumber);
        return null;
    }

    void confirmDeleteDevice(EntityDevice device, Runnable deleted)
    {
        if (rejectInUse(device))
        {
            return;
        }
        deleteDialog = new GlassAlertDialogBuilder(activity)
            .setTitle("외부기 삭제")
            .setMessage(device.serialNumber + "의 등록 정보를 삭제하시겠습니까?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제", (dialog, which) ->
            {
                if (!attached.getAsBoolean() || rejectInUse(device))
                {
                    return;
                }
                EntityDevice current = UtilDevice.instance.getDeviceBySerialNumber(device.serialNumber);
                if (!sameDetails(device, current) || current.isActive != device.isActive)
                {
                    Toast.makeText(activity, "등록 정보가 변경되었습니다. 다시 확인하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                UtilDevice.instance.delete(copy(device));
                if (UtilDevice.instance.getDeviceBySerialNumber(device.serialNumber) != null)
                {
                    Toast.makeText(activity, "삭제하지 못했습니다. 등록 정보는 유지됩니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                UtilLog.instance.writeLog("외부기 삭제 : " + device.serialNumber);
                deleted.run();
            }).show();
    }

    private boolean rejectInUse(EntityDevice device)
    {
        if (!activity.isDeviceInUse(device.serialNumber))
        {
            return false;
        }
        if (deleteDialog != null)
        {
            deleteDialog.dismiss();
        }
        deleteDialog = new GlassAlertDialogBuilder(activity)
            .setTitle("연결 중인 외부기")
            .setMessage("외부기의 연결을 먼저 해제한 뒤 삭제하세요.")
            .setPositiveButton("확인", null).show();
        return true;
    }

    private static boolean restore(EntityDevice original, EntityDevice updated)
    {
        if (original == null)
        {
            UtilDevice.instance.delete(copy(updated));
            return UtilDevice.instance.getDeviceBySerialNumber(updated.serialNumber) == null;
        }
        UtilDevice.instance.update(copy(original));
        EntityDevice restored = UtilDevice.instance.getDeviceBySerialNumber(original.serialNumber);
        return sameDetails(original, restored) && original.isActive == restored.isActive;
    }

    private static boolean sameDetails(EntityDevice first, EntityDevice second)
    {
        return first != null && second != null
            && Objects.equals(first.serialNumber, second.serialNumber)
            && Objects.equals(first.pairingKey, second.pairingKey)
            && Objects.equals(normalize(first.additionalInformation), normalize(second.additionalInformation));
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value;
    }

    private static EntityDevice copy(EntityDevice source)
    {
        EntityDevice copy = new EntityDevice();
        copy.serialNumber = source.serialNumber;
        copy.pairingKey = source.pairingKey;
        copy.additionalInformation = source.additionalInformation;
        copy.isActive = source.isActive;
        return copy;
    }
}
