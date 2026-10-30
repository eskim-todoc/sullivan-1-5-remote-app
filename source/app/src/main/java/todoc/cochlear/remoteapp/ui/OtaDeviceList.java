package todoc.cochlear.remoteapp.ui;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.connection.ConnectionInfo;
import todoc.cochlear.remoteapp.connection.DeviceBadgeInfo;
import todoc.cochlear.remoteapp.connection.OtaDeviceInfo;
import todoc.cochlear.remoteapp.connection.OtaProgress;
import todoc.cochlear.remoteapp.ota.Ota;

/**
 * 주소별 행을 유지하여 부팅·재연결과 다른 장치의 전송 중에도 목록 위치를 보존한다.
 */
public final class OtaDeviceList
{
    private final MainActivity activity;
    private final LinearLayout parent;
    private final Runnable onSelectionChanged;
    private final Map<String, Row> rows = new LinkedHashMap<>();
    private List<String> visibleAddresses = new ArrayList<>();

    public OtaDeviceList(MainActivity activity, LinearLayout parent, Runnable onSelectionChanged)
    {
        this.activity = activity;
        this.parent = parent;
        this.onSelectionChanged = onSelectionChanged;
    }

    public void render(List<OtaProgress> devices)
    {
        Map<String, OtaProgress> latest = new LinkedHashMap<>();
        if (devices != null)
        {
            for (OtaProgress device : devices)
            {
                latest.put(device.address, device);
            }
        }
        List<String> addresses = new ArrayList<>(latest.keySet());
        boolean rosterChanged = !addresses.equals(visibleAddresses) || parent.getChildCount() == 0;
        if (rosterChanged)
        {
            visibleAddresses = addresses;
            parent.removeAllViews();
            rows.keySet().retainAll(addresses);
            if (addresses.isEmpty())
            {
                TextView empty = text("아래 + 추가 연결로 체크한 장치를 찾아 연결합니다.", 12, 0xffaebec7);
                empty.setPadding(dp(2), dp(8), dp(2), dp(10));
                parent.addView(empty);
            }
            for (String address : addresses)
            {
                Row row = rows.get(address);
                if (row == null)
                {
                    row = new Row(address);
                    rows.put(address, row);
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.bottomMargin = dp(6);
                parent.addView(row.panel, params);
            }
        }
        for (Map.Entry<String, OtaProgress> entry : latest.entrySet())
        {
            rows.get(entry.getKey()).render(entry.getValue());
        }
    }

    public void refreshMetadata()
    {
        for (Row row : rows.values())
        {
            if (row.progress != null)
            {
                row.render(row.progress);
            }
        }
    }

    public static String stateLabel(OtaProgress.State state)
    {
        switch (state)
        {
            case PREPARING:
                return "준비 중";
            case READY:
                return "준비됨";
            case WRITING:
                return "전송 중";
            case COMPLETE:
                return "완료";
            case FAILED:
                return "실패";
            default:
                return "준비 전";
        }
    }

    private final class Row
    {
        final String address;
        final LinearLayout panel;
        final TextView name, release, battery, selectedBadge, stateText, percent, slots, mac, detail;
        final ProgressBar bar;
        OtaProgress progress;

        Row(String address)
        {
            this.address = address;
            panel = new LinearLayout(activity);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(dp(9), dp(7), dp(9), dp(7));
            panel.setBackgroundResource(R.drawable.remote_ota_device_surface);
            panel.setClickable(true);
            panel.setFocusable(true);
            panel.setOnClickListener(view -> selectOrReconnect());
            panel.setOnLongClickListener(view ->
            {
                OtaDeviceInfo metadata = progress == null ? null : activity.getSessionOtaDeviceInfo(progress.sessionId);
                if (metadata != null && (metadata.connectionState == ConnectionInfo.STATE_CONNECTED
                    || metadata.connectionState == ConnectionInfo.STATE_CONNECTING))
                {
                    DeviceDisconnectDialog.show(activity, progress.sessionId, progress.label);
                }
                else
                {
                    Toast.makeText(activity, "이미 연결이 해제된 장치입니다.", Toast.LENGTH_SHORT).show();
                }
                return true;
            });

            LinearLayout title = horizontal();
            name = text("", 12, 0xffe1ebec);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            title.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            release = text("R—", 12, 0xffbfdd7b);
            release.setTypeface(release.getTypeface(), Typeface.BOLD);
            release.setGravity(Gravity.CENTER);
            title.addView(release, new LinearLayout.LayoutParams(dp(45), -2));
            battery = text("—%", 12, 0xffb9e0cf);
            battery.setTypeface(battery.getTypeface(), Typeface.BOLD);
            battery.setGravity(Gravity.END);
            battery.setCompoundDrawablesWithIntrinsicBounds(R.drawable.remote_ota_battery, 0, 0, 0);
            battery.setCompoundDrawablePadding(dp(2));
            title.addView(battery, new LinearLayout.LayoutParams(dp(52), -2));
            panel.addView(title);

            LinearLayout status = horizontal();
            stateText = text("", 11, 0xffaebec7);
            status.addView(stateText, new LinearLayout.LayoutParams(0, -2, 1));
            selectedBadge = text("선택됨", 10, 0xfff0d4df);
            selectedBadge.setBackgroundResource(R.drawable.remote_badge_mint);
            selectedBadge.setPadding(dp(4), dp(1), dp(4), dp(1));
            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
            badgeParams.rightMargin = dp(7);
            status.addView(selectedBadge, badgeParams);
            percent = text("전체 0%", 11, 0xffe1ebec);
            percent.setTypeface(percent.getTypeface(), Typeface.BOLD);
            status.addView(percent);
            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
            statusParams.topMargin = dp(3);
            panel.addView(status, statusParams);

            bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xffB3BCFF));
            bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0x30967486));
            bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(0xffB3BCFF));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, dp(4));
            barParams.topMargin = dp(4);
            panel.addView(bar, barParams);

            slots = text("현재 — · 이전 —", 11, 0xffb8d7f4);
            LinearLayout slotLine = horizontal();
            slotLine.addView(slots, new LinearLayout.LayoutParams(0, -2, 1));
            mac = text("MAC " + address.substring(Math.max(0, address.length() - 5)), 10, 0xff8999a3);
            slotLine.addView(mac);
            LinearLayout.LayoutParams slotParams = new LinearLayout.LayoutParams(-1, -2);
            slotParams.topMargin = dp(4);
            panel.addView(slotLine, slotParams);
            detail = text("", 11, 0xffaebec7);
            detail.setMaxLines(2);
            detail.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
            detailParams.topMargin = dp(3);
            panel.addView(detail, detailParams);
            for (View child : new View[]{name, release, battery, selectedBadge, stateText, percent, slots, mac, detail, bar})
            {
                child.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
        }

        void selectOrReconnect()
        {
            if (progress == null)
            {
                return;
            }
            OtaDeviceInfo metadata = activity.getSessionOtaDeviceInfo(progress.sessionId);
            boolean disconnected = metadata == null || (metadata.connectionState != ConnectionInfo.STATE_CONNECTED
                && metadata.connectionState != ConnectionInfo.STATE_CONNECTING);
            boolean accepted = disconnected ? activity.reconnectDeviceForControl(address)
                : activity.selectDeviceSession(progress.sessionId);
            if (accepted)
            {
                onSelectionChanged.run();
            }
            else
            {
                Toast.makeText(activity, "사용자·외부기 체크 상태와 진행 중인 작업을 확인하세요.", Toast.LENGTH_SHORT).show();
            }
        }

        void render(OtaProgress update)
        {
            progress = update;
            OtaDeviceInfo metadata = activity.getSessionOtaDeviceInfo(update.sessionId);
            DeviceBadgeInfo badge = metadata == null ? null : metadata.badge;
            boolean selected = Objects.equals(update.sessionId, activity.getSelectedSessionId());
            panel.setSelected(selected);
            String identity = badge == null ? DeviceLabelStyle.forAccessibility(update.label)
                : badge.userName + " · " + badge.wearingPosition + " · " + badge.serialNumber;
            setTextIfChanged(name, identity);
            String version = badge == null || badge.releaseName == null || badge.releaseName.isEmpty() ? "R—"
                : badge.releaseName.replaceFirst("^REL[- ]?", "R");
            setTextIfChanged(release, version);
            Integer level = metadata == null ? null : metadata.batteryPercent;
            setTextIfChanged(battery, level == null ? "—%" : level + "%");
            selectedBadge.setVisibility(selected ? View.VISIBLE : View.GONE);
            int connection = metadata == null ? ConnectionInfo.STATE_DISCONNECTED : metadata.connectionState;
            String connectionLabel = connection == ConnectionInfo.STATE_CONNECTED ? "연결됨"
                : connection == ConnectionInfo.STATE_CONNECTING ? "연결 중"
                  : connection == ConnectionInfo.STATE_FAILED ? "연결 실패 · 눌러 재연결" : "연결 해제 · 눌러 재연결";
            String state = stateLabel(update.state);
            String file = fileLabel(update.currentFileNum);
            setTextIfChanged(stateText, connection == ConnectionInfo.STATE_CONNECTED ? state
                                                                                       + (update.state == OtaProgress.State.WRITING && !file.isEmpty() ? " · " + file : "") : connectionLabel);
            stateText.setTextColor(update.state == OtaProgress.State.FAILED || connection == ConnectionInfo.STATE_FAILED
                ? 0xffe7b58e : connection == ConnectionInfo.STATE_CONNECTED ? 0xffb9e0cf : 0xffaebec7);
            setTextIfChanged(percent, "전체 " + update.percent + "%");
            bar.setIndeterminate(update.state == OtaProgress.State.PREPARING);
            bar.setProgress(update.percent);
            String slotText = "현재 " + slotLabel(metadata == null ? null : metadata.currentBootSlot)
                + " · 이전 " + slotLabel(metadata == null ? null : metadata.lastBootSlot);
            setTextIfChanged(slots, slotText);
            String note = update.totalBytes > 0 ? compactBytes(update.acknowledgedBytes) + " / "
                                                  + compactBytes(update.totalBytes) : "";
            if (update.state == OtaProgress.State.WRITING || update.state == OtaProgress.State.COMPLETE)
            {
                long rate = update.state == OtaProgress.State.COMPLETE ? update.averageBytesPerSecond : update.bytesPerSecond;
                note += "\n" + (update.state == OtaProgress.State.COMPLETE ? "평균 " : "")
                    + String.format(java.util.Locale.US, "%,d Bytes/s", rate);
            }
            if (update.state == OtaProgress.State.FAILED && update.message != null && !update.message.isEmpty())
            {
                note += (note.isEmpty() ? "" : " · ") + update.message;
            }
            setTextIfChanged(detail, note);
            detail.setVisibility(note.isEmpty() ? View.GONE : View.VISIBLE);
            String description = identity + ", " + version + ", 배터리 " + (level == null ? "미확인" : level + "퍼센트")
                + (selected ? ", 선택한 장치" : "") + ", " + connectionLabel + ", OTA " + state
                + ", 전체 진행률 " + update.percent + "퍼센트, " + slotText
                + ", 확인된 " + update.acknowledgedBytes + "바이트 / 전체 " + update.totalBytes + "바이트. "
                + "블루투스 주소 끝 " + address.substring(Math.max(0, address.length() - 5)) + ". "
                + (update.state == OtaProgress.State.FAILED && update.message != null ? update.message + ". " : "")
                + (disconnected(connection) ? "눌러 다시 연결" : "눌러 제어 대상으로 선택. 길게 눌러 연결 해제");
            if (!TextUtils.equals(description, panel.getContentDescription()))
            {
                panel.setContentDescription(description);
            }
        }
    }

    private static void setTextIfChanged(TextView view, String text)
    {
        if (!text.contentEquals(view.getText()))
        {
            view.setText(text);
        }
    }

    private static boolean disconnected(int state)
    {
        return state != ConnectionInfo.STATE_CONNECTED && state != ConnectionInfo.STATE_CONNECTING;
    }

    private static String slotLabel(Integer slot)
    {
        return slot == null ? "—" : slot == 0xFF ? "FF" : String.valueOf(slot);
    }

    private static String fileLabel(int fileNum)
    {
        switch (fileNum)
        {
            case Ota.FILE_NUM_MFST:
                return "MFST";
            case Ota.FILE_NUM_APP0:
                return "APP0";
            case Ota.FILE_NUM_APP1:
                return "APP1";
            case Ota.FILE_NUM_APP2:
                return "APP2";
            default:
                return "";
        }
    }

    private static String compactBytes(long bytes)
    {
        if (bytes >= 1024 * 1024)
        {
            return String.format(Locale.ROOT, "%.2f MiB", bytes / (1024.0 * 1024));
        }
        if (bytes >= 1024)
        {
            return String.format(Locale.ROOT, "%.1f KiB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private LinearLayout horizontal()
    {
        LinearLayout layout = new LinearLayout(activity);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private TextView text(String value, int size, int color)
    {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setIncludeFontPadding(false);
        text.setTypeface(ResourcesCompat.getFont(activity, R.font.app_sans));
        return text;
    }

    private int dp(int value)
    {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
