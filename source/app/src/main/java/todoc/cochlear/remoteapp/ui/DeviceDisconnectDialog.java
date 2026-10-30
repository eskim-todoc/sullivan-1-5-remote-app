package todoc.cochlear.remoteapp.ui;

import android.text.SpannableStringBuilder;
import android.widget.Toast;

import todoc.cochlear.remoteapp.activity.MainActivity;

/**
 * 확인 시에도 처음 길게 누른 연결 세대만 해제한다. 등록과 활성 상태는 바꾸지 않는다.
 */
public final class DeviceDisconnectDialog
{
    private DeviceDisconnectDialog()
    {
    }

    public static void show(MainActivity activity, String sessionId, String label)
    {
        if (activity.isFinishing() || activity.isDestroyed() || sessionId == null)
        {
            return;
        }
        SpannableStringBuilder message = new SpannableStringBuilder(DeviceLabelStyle.format(label));
        message.append("\n\n이 장치의 연결을 해제합니다.\n등록 정보와 활성 체크는 유지됩니다.");
        new GlassAlertDialogBuilder(activity)
            .setTitle("연결을 해제할까요?")
            .setMessage(message)
            .setNegativeButton("취소", null)
            .setPositiveButton("연결 해제", (dialog, which) ->
            {
                if (!activity.disconnectDevice(sessionId))
                {
                    Toast.makeText(activity, "연결 상태와 진행 중인 작업을 확인한 뒤 다시 시도하세요.", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }
}
