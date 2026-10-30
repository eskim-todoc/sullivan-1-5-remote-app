package todoc.cochlear.remoteapp.ui;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.connection.ConnectionInfo;

/**
 * Connection actions are address/session based; selecting a target never changes the default user.
 */
public final class ConnectionManagerDialog
{
    private final MainActivity activity;
    private final LifecycleOwner owner;
    private final Runnable onTargetChanged;
    private final LinearLayout rows;
    private final TextView scanNote;
    private final AppCompatButton scanButton;
    private final AlertDialog dialog;
    private final Observer<List<ConnectionInfo>> connectionsObserver = this::render;
    private final Observer<Boolean> scanObserver = this::renderScanning;
    private String lastSignature;

    public ConnectionManagerDialog(MainActivity activity, LifecycleOwner owner, Runnable onTargetChanged)
    {
        this.activity = activity;
        this.owner = owner;
        this.onTargetChanged = onTargetChanged;
        LinearLayout body = vertical();
        body.setPadding(dp(16), 0, dp(16), dp(8));
        TextView description = text("선택한 장치 한 대를 제어합니다. 다른 장치의 연결은 유지됩니다.", 12, 0xffaebec7);
        body.addView(description, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout scan = new LinearLayout(activity);
        scan.setGravity(Gravity.CENTER_VERTICAL);
        scanNote = text("추가 장치를 검색해 연결하세요.", 11, 0xffaebec7);
        scan.addView(scanNote, new LinearLayout.LayoutParams(0, -2, 1));
        scanButton = button("장치 검색");
        scanButton.setOnClickListener(view -> activity.startAdditionalDeviceScan());
        LinearLayout.LayoutParams scanParams = new LinearLayout.LayoutParams(-2, dp(44));
        scanParams.leftMargin = dp(8);
        scan.addView(scanButton, scanParams);
        LinearLayout.LayoutParams scanRow = new LinearLayout.LayoutParams(-1, -2);
        scanRow.topMargin = dp(8);
        scanRow.bottomMargin = dp(8);
        body.addView(scan, scanRow);
        int available = activity.getResources().getDisplayMetrics().heightPixels;
        final int listMaxHeight = Math.min(dp(328), available / 2);
        ScrollView scroll = new ScrollView(activity)
        {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
            {
                int heightLimit = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED
                    ? listMaxHeight : Math.min(listMaxHeight, MeasureSpec.getSize(heightMeasureSpec));
                super.onMeasure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(heightLimit, MeasureSpec.AT_MOST));
            }
        };
        scroll.setClipToPadding(false);
        rows = vertical();
        scroll.addView(rows);
        body.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        dialog = new GlassAlertDialogBuilder(activity).setTitle("연결 장치")
            .setView(body).setNegativeButton("닫기", null).create();
        dialog.setOnDismissListener(ignored ->
        {
            activity.getConnectionStates().removeObserver(connectionsObserver);
            activity.getAdditionalScanState().removeObserver(scanObserver);
        });
    }

    public void show()
    {
        dialog.show();
        activity.getConnectionStates().observe(owner, connectionsObserver);
        activity.getAdditionalScanState().observe(owner, scanObserver);
    }

    public void dismiss()
    {
        dialog.dismiss();
    }

    private void renderScanning(Boolean scanning)
    {
        boolean active = Boolean.TRUE.equals(scanning);
        scanButton.setEnabled(!active);
        scanButton.setText(active ? "검색 중…" : "다시 검색");
        scanNote.setText(active ? "주변 BLE 장치를 찾고 있습니다." : "목록에 없으면 다시 검색하세요.");
    }

    private void render(List<ConnectionInfo> value)
    {
        List<ConnectionInfo> devices = value == null ? new ArrayList<>() : new ArrayList<>(value);
        java.util.Collections.sort(devices, (first, second) ->
        {
            int order = Integer.compare(priority(first), priority(second));
            return order != 0 ? order : (first.address == null ? "" : first.address)
                .compareTo(second.address == null ? "" : second.address);
        });
        StringBuilder signature = new StringBuilder();
        for (ConnectionInfo info : devices)
        {
            signature.append(info.id).append('|').append(info.address)
                .append('|').append(info.label).append('|').append(info.state).append('|')
                .append(info.selected).append('|').append(info.registered).append(';');
        }
        if (signature.toString().equals(lastSignature))
        {
            return;
        }
        lastSignature = signature.toString();
        rows.removeAllViews();
        if (devices.isEmpty())
        {
            TextView empty = text("아직 발견된 장치가 없습니다.\n장치 전원을 켠 뒤 검색하세요.", 13, 0xffc1d2cf);
            empty.setPadding(dp(8), dp(20), dp(8), dp(20));
            rows.addView(empty);
            return;
        }
        boolean foundUnregistered = false;
        int previousGroup = -1;
        for (ConnectionInfo info : devices)
        {
            int group = info.state == ConnectionInfo.STATE_CONNECTED || info.state == ConnectionInfo.STATE_CONNECTING ? 0 : 1;
            if (group != previousGroup)
            {
                TextView heading = text(group == 0 ? "연결 중인 장치" : "발견한 장치", 12, 0xffb9e0cf);
                heading.setPadding(0, dp(8), 0, dp(6));
                rows.addView(heading);
                previousGroup = group;
            }
            addDevice(info);
            foundUnregistered |= !info.registered;
        }
        if (foundUnregistered)
        {
            TextView note = text("등록이 필요한 장치는 더보기의 사용자·외부기 목록에서 먼저 등록하세요.", 11, 0xffaebec7);
            note.setPadding(0, dp(8), 0, dp(8));
            rows.addView(note);
        }
    }

    private void addDevice(ConnectionInfo info)
    {
        LinearLayout panel = vertical();
        panel.setBackgroundResource(R.drawable.remote_glass_panel);
        panel.setPadding(dp(10), dp(8), dp(10), dp(6));
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(-1, -2);
        panelParams.bottomMargin = dp(6);
        rows.addView(panel, panelParams);
        String label = info.label == null || info.label.isEmpty() ? "BLE 장치" : info.label;
        TextView title = text("", 13, 0xffe1ebec);
        title.setText(DeviceLabelStyle.format(label));
        title.setContentDescription(DeviceLabelStyle.forAccessibility(label));
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        panel.addView(title);
        TextView state = text(stateLabel(info), 11,
            info.selected ? 0xffb9e0cf : 0xffaebec7);
        state.setPadding(0, dp(3), 0, dp(4));
        panel.addView(state);
        LinearLayout actions = new LinearLayout(activity);
        actions.setGravity(Gravity.END);
        if (info.state == ConnectionInfo.STATE_CONNECTED)
        {
            AppCompatButton select = button(info.selected ? "현재 제어 대상" : "제어");
            select.setEnabled(!info.selected);
            select.setAlpha(info.selected ? 0.55f : 1f);
            select.setOnClickListener(view ->
            {
                if (activity.selectDeviceSession(info.id))
                {
                    dialog.dismiss();
                    onTargetChanged.run();
                }
                else
                {
                    rejected("장치 연결 상태와 진행 중인 FF·관리 작업을 확인하세요.");
                }
            });
            addAction(actions, select);
            AppCompatButton disconnect = button("연결 해제");
            disconnect.setOnClickListener(view ->
            {
                if (!activity.disconnectDevice(info.id))
                {
                    rejected("진행 중인 작업을 마친 뒤 연결을 해제하세요.");
                }
            });
            addAction(actions, disconnect);
        }
        else if (info.state == ConnectionInfo.STATE_CONNECTING)
        {
            AppCompatButton cancel = button("연결 취소");
            cancel.setOnClickListener(view ->
            {
                if (!activity.disconnectDevice(info.id))
                {
                    rejected("연결 상태가 바뀌었습니다. 목록을 다시 확인하세요.");
                }
            });
            addAction(actions, cancel);
        }
        else
        {
            AppCompatButton connect = button(info.registered ? "연결" : "등록 필요");
            connect.setEnabled(info.registered);
            connect.setOnClickListener(view ->
            {
                if (!activity.connectDevice(info.address))
                {
                    rejected("연결을 시작하지 못했습니다. 등록 정보와 장치 상태를 확인하세요.");
                }
            });
            addAction(actions, connect);
        }
        panel.addView(actions, new LinearLayout.LayoutParams(-1, -2));
    }

    private static int priority(ConnectionInfo info)
    {
        return info.selected ? 0 : info.state == ConnectionInfo.STATE_CONNECTED ? 1
                                   : info.state == ConnectionInfo.STATE_CONNECTING ? 2 : 3;
    }

    private static String stateLabel(ConnectionInfo info)
    {
        if (info.selected && info.state == ConnectionInfo.STATE_CONNECTED)
        {
            return "현재 제어 대상 · 연결됨";
        }
        switch (info.state)
        {
            case ConnectionInfo.STATE_CONNECTED:
                return "연결됨";
            case ConnectionInfo.STATE_CONNECTING:
                return "연결 중…";
            case ConnectionInfo.STATE_FAILED:
                return "연결 실패 · 다시 시도할 수 있습니다";
            case ConnectionInfo.STATE_DISCONNECTED:
                return "연결 해제됨";
            default:
                return "검색됨";
        }
    }

    private void addAction(LinearLayout parent, AppCompatButton button)
    {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(44));
        params.leftMargin = dp(6);
        parent.addView(button, params);
    }

    private AppCompatButton button(String label)
    {
        AppCompatButton button = new AppCompatButton(activity);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTypeface(ResourcesCompat.getFont(activity, R.font.app_sans));
        button.setTextColor(0xffe1ebec);
        button.setBackgroundResource(R.drawable.app_glass_action);
        androidx.core.view.ViewCompat.setBackgroundTintList(button, null);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        return button;
    }

    private LinearLayout vertical()
    {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(String value, int size, int color)
    {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(ResourcesCompat.getFont(activity, R.font.app_sans));
        view.setIncludeFontPadding(false);
        return view;
    }

    private void rejected(String message)
    {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value)
    {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
