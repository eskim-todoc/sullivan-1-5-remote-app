package todoc.cochlear.remoteapp.fragment;

import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.database.logs.UtilLog;
import todoc.cochlear.remoteapp.database.users.EntityUser;
import todoc.cochlear.remoteapp.database.users.UserGroup;
import todoc.cochlear.remoteapp.database.users.UtilUser;
import todoc.cochlear.remoteapp.ui.GlassAlertDialogBuilder;

/** Shared user registration actions for list hosts; storage and session ownership stay unchanged. */
final class UserRegistrationActions
{
    private final MainActivity activity;
    private final BooleanSupplier attached;
    private AlertDialog warningDialog;

    UserRegistrationActions(MainActivity activity, BooleanSupplier attached)
    {
        this.activity = activity;
        this.attached = attached;
    }

    String saveGroup(UserGroup original, UserGroup updated)
    {
        UserGroup collision = UtilUser.instance.getGroup(updated.initials);
        if (collision != null && (original == null || !Objects.equals(original.initials, updated.initials)))
        {
            return "이미 등록된 사용자 이니셜입니다.";
        }
        List<String> oldNames = original == null ? Collections.emptyList() : original.getNames();
        if (!activity.canUpdateUserGroup(oldNames, updated.getProfiles()))
        {
            return "진행 중인 작업이 끝난 뒤 연결 사용 여부를 바꾸세요.";
        }
        UtilUser.GroupWriteResult result = UtilUser.instance.saveGroup(original, updated);
        if (result != UtilUser.GroupWriteResult.SAVED)
        {
            if (result == UtilUser.GroupWriteResult.NAME_IN_USE)
            {
                return "이미 등록된 사용자 이니셜입니다.";
            }
            return result == UtilUser.GroupWriteResult.STALE
                ? "등록 정보가 변경되었습니다. 취소 후 목록에서 다시 확인하세요."
                : "저장하지 못했습니다. 입력한 내용은 유지됩니다.";
        }
        UserGroup saved = UtilUser.instance.getGroup(updated.initials);
        UserGroup applied = saved == null ? updated : saved;
        activity.onUserGroupUpdated(oldNames, applied.getProfiles());
        for (EntityUser profile : applied.getProfiles())
        {
            EntityUser before = original == null ? null : original.get(profile.ear);
            if (profile.isActive && (before == null || !before.isActive))
            {
                // Activation takes effect on the next explicit + search.
                break;
            }
        }
        UtilLog.instance.writeLog((original == null ? "사용자 등록 : " : "사용자 수정 : ") + updated.initials);
        return null;
    }

    private boolean rejectDeleteInUse(UserGroup group)
    {
        for (String name : group.getNames())
        {
            if (activity.isUserInUse(name))
            {
                warningDialog = new GlassAlertDialogBuilder(activity).setTitle("연결 중인 사용자")
                    .setMessage("이 사용자의 장치 연결을 모두 해제한 뒤 삭제하세요.")
                    .setPositiveButton("확인", null).create();
                warningDialog.show();
                return true;
            }
        }
        return false;
    }

    void deleteGroup(UserGroup group, Runnable onDeleted)
    {
        if (!attached.getAsBoolean() || rejectDeleteInUse(group))
        {
            return;
        }
        close();
        warningDialog = new GlassAlertDialogBuilder(activity).setTitle("사용자 삭제")
            .setMessage(group.displayName() + "에 등록된 모든 내부기와 코드를 삭제하시겠습니까?")
            .setNegativeButton("취소", null).setPositiveButton("삭제", (dialog, which) ->
            {
                if (!attached.getAsBoolean() || rejectDeleteInUse(group))
                {
                    return;
                }
                if (!UtilUser.instance.deleteGroup(group))
                {
                    Toast.makeText(activity, "삭제하지 못했습니다. 등록 정보를 다시 확인하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                UtilLog.instance.writeLog("사용자 삭제 : " + group.initials);
                onDeleted.run();
            }).create();
        warningDialog.show();
    }

    void close()
    {
        if (warningDialog != null)
        {
            warningDialog.dismiss();
            warningDialog = null;
        }
    }
}