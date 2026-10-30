package todoc.cochlear.remoteapp.ui;

import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.HorizontalScrollView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import todoc.cochlear.remoteapp.activity.MainActivity;
import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.connection.ConnectionInfo;
import todoc.cochlear.remoteapp.connection.DeviceBadgeInfo;

/**
 * The rail selects a session; its add button only opens an explicit additional-device scan.
 */
public final class DeviceRail
{
    private final MainActivity activity;
    private final LinearLayout cards;
    private final HorizontalScrollView scroll;
    private final Runnable onSelectionChanged;
    private final Runnable onOpenConnections;
    private final Typeface typeface;
    private final Map<String, Row> rows = new LinkedHashMap<>();
    private final Map<String, Integer> addressOrder = new LinkedHashMap<>();
    private List<String> visibleIds = Collections.emptyList();
    private String lastSelectedId;
    private boolean userInteracting;
    private long userInteractionQuietUntil;

    public DeviceRail(MainActivity activity, LinearLayout cards, HorizontalScrollView scroll,
                      View addButton, Runnable onSelectionChanged, Runnable onOpenConnections)
    {
        this.activity = activity;
        this.cards = cards;
        this.scroll = scroll;
        this.onSelectionChanged = onSelectionChanged;
        this.onOpenConnections = onOpenConnections;
        typeface = ResourcesCompat.getFont(activity, R.font.app_sans);
        addButton.setOnClickListener(view -> onOpenConnections.run());
        scroll.setOnTouchListener((view, event) ->
        {
            switch (event.getActionMasked())
            {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    userInteracting = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    userInteracting = false;
                    userInteractionQuietUntil = SystemClock.uptimeMillis() + 800;
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    public void render(List<ConnectionInfo> connections)
    {
        List<ConnectionInfo> visible = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        if (connections != null)
        {
            for (ConnectionInfo info : connections)
            {
                if (activity.isHiddenFromDeviceRail(info.id)
                    && (info.state == ConnectionInfo.STATE_DISCONNECTED || info.state == ConnectionInfo.STATE_FAILED))
                {
                    continue;
                }
                if (info.state == ConnectionInfo.STATE_CONNECTED || info.state == ConnectionInfo.STATE_CONNECTING
                    || info.state == ConnectionInfo.STATE_DISCONNECTED
                    || info.state == ConnectionInfo.STATE_FAILED || info.selected)
                {
                    visible.add(info);
                    String address = addressKey(info);
                    if (!addressOrder.containsKey(address))
                    {
                        addressOrder.put(address, addressOrder.size());
                    }
                }
            }
        }
        java.util.Collections.sort(visible, (first, second) -> Integer.compare(
            addressOrder.get(addressKey(first)), addressOrder.get(addressKey(second))));
        for (ConnectionInfo info : visible)
        {
            ids.add(info.id);
        }
        boolean rosterChanged = !ids.equals(visibleIds) || cards.getChildCount() == 0;
        if (rosterChanged)
        {
            visibleIds = ids;
            cards.removeAllViews();
            rows.keySet().retainAll(ids);
            if (ids.isEmpty())
            {
                TextView empty = text(0xffaebec7);
                empty.setText("＋ 버튼으로 장치를 연결하세요");
                empty.setGravity(Gravity.CENTER_VERTICAL);
                cards.addView(empty, new LinearLayout.LayoutParams(-2, dp(56)));
            }
            else
            {
                for (ConnectionInfo info : visible)
                {
                    Row row = rows.get(info.id);
                    if (row == null)
                    {
                        row = new Row();
                        rows.put(info.id, row);
                    }
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(176), dp(56));
                    params.rightMargin = dp(8);
                    cards.addView(row.panel, params);
                }
            }
        }
        int live = 0;
        String selectedId = null;
        for (ConnectionInfo info : visible)
        {
            rows.get(info.id).render(info);
            if (info.state == ConnectionInfo.STATE_CONNECTED)
            {
                live++;
            }
            if (info.selected)
            {
                selectedId = info.id;
            }
        }
        boolean selectionChanged = !Objects.equals(lastSelectedId, selectedId);
        if (rosterChanged || selectionChanged)
        {
            lastSelectedId = selectedId;
            Row selected = rows.get(selectedId);
            if (selected != null)
            {
                scroll.post(() -> revealSelected(selected, selectionChanged));
            }
        }
        scroll.setContentDescription("연결 장치 " + live + "대. 옆으로 밀어 다른 장치를 확인합니다.");
    }

    /**
     * Metadata refresh is per session and never rebuilds the rail during ACK or status updates.
     */
    public void refreshMetadata()
    {
        for (Row row : rows.values())
        {
            row.refreshMetadata();
        }
    }

    private final class Row
    {
        final LinearLayout panel;
        final TextView wearingPosition, userName, serialNumber, release, connectionBadge;
        final GlassConnectionOverlay connectionWaves;
        ConnectionInfo info;
        DeviceBadgeInfo lastMetadata;
        int displayedConnectionState = Integer.MIN_VALUE;

        Row()
        {
            panel = new LinearLayout(activity);
            panel.setId(R.id.device_badge_card);
            panel.setOrientation(LinearLayout.HORIZONTAL);
            panel.setGravity(Gravity.CENTER_VERTICAL);
            panel.setPadding(dp(6), dp(6), dp(8), dp(6));
            panel.setBackgroundResource(R.drawable.remote_device_chip_surface);
            panel.setElevation(dp(1));
            panel.setClickable(true);
            panel.setFocusable(true);
            panel.setOnClickListener(view -> select());
            panel.setOnLongClickListener(view ->
            {
                if (info != null && (info.state == ConnectionInfo.STATE_CONNECTED
                    || info.state == ConnectionInfo.STATE_CONNECTING))
                {
                    DeviceDisconnectDialog.show(activity, info.id, info.label);
                }
                else
                {
                    Toast.makeText(activity, "이미 연결이 해제된 장치입니다.", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
            DelayedLongPress.attach(panel,
                () -> info != null && (info.state == ConnectionInfo.STATE_DISCONNECTED || info.state == ConnectionInfo.STATE_FAILED),
                () -> activity.hideDisconnectedRailDevice(info.id));
            panel.post(() -> panel.setOutlineProvider(new ViewOutlineProvider()
            {
                @Override
                public void getOutline(View view, Outline outline)
                {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getHeight() / 2f);
                    outline.setAlpha(0.4f);
                }
            }));

            FrameLayout photoFrame = new FrameLayout(activity);
            panel.addView(photoFrame, new LinearLayout.LayoutParams(dp(44), dp(44)));
            ImageView photo = new ImageView(activity);
            photo.setId(R.id.device_badge_photo);
            photo.setImageResource(R.drawable.rail_device_processor);
            photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            photo.setBackgroundResource(R.drawable.remote_device_photo_circle);
            photo.setPadding(dp(6), dp(6), dp(6), dp(6));
            photo.setClipToOutline(true);
            photo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            photoFrame.addView(photo, new FrameLayout.LayoutParams(-1, -1));
            connectionWaves = new GlassConnectionOverlay(activity);
            connectionWaves.setCompactMode(true);
            connectionWaves.setVisibility(View.GONE);
            photoFrame.addView(connectionWaves, new FrameLayout.LayoutParams(-1, -1));
            connectionBadge = text(0xffdce4e9);
            connectionBadge.setTextSize(10);
            connectionBadge.setTypeface(typeface, Typeface.BOLD);
            connectionBadge.setGravity(Gravity.CENTER);
            connectionBadge.setPadding(dp(2), dp(1), dp(2), dp(1));
            connectionBadge.setVisibility(View.GONE);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(-1, -2,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            badgeParams.leftMargin = dp(2);
            badgeParams.rightMargin = dp(2);
            badgeParams.bottomMargin = dp(2);
            photoFrame.addView(connectionBadge, badgeParams);

            LinearLayout values = new LinearLayout(activity);
            values.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams valuesParams = new LinearLayout.LayoutParams(0, -2, 1);
            valuesParams.leftMargin = dp(6);
            panel.addView(values, valuesParams);
            LinearLayout top = row();
            LinearLayout bottom = row();
            wearingPosition = text(0xffb9e0cf);
            userName = text(0xffe1ebec);
            serialNumber = text(0xffb8d7f4);
            release = text(0xffbfdd7b);
            wearingPosition.setId(R.id.device_badge_wearing_position);
            userName.setId(R.id.device_badge_user_name);
            serialNumber.setId(R.id.device_badge_serial_number);
            release.setId(R.id.device_badge_release);
            userName.setTypeface(typeface, Typeface.BOLD);
            release.setTypeface(typeface, Typeface.BOLD);
            top.addView(wearingPosition, new LinearLayout.LayoutParams(dp(16), -2));
            top.addView(userName, new LinearLayout.LayoutParams(0, -2, 1));
            bottom.addView(serialNumber, new LinearLayout.LayoutParams(dp(38), -2));
            bottom.addView(release, new LinearLayout.LayoutParams(0, -2, 1));
            values.addView(top);
            LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(-1, -2);
            bottomParams.topMargin = dp(2);
            values.addView(bottom, bottomParams);
        }

        void render(ConnectionInfo connection)
        {
            info = connection;
            panel.setSelected(info.selected);
            boolean disconnected = info.state != ConnectionInfo.STATE_CONNECTED
                && info.state != ConnectionInfo.STATE_CONNECTING;
            panel.setAlpha(disconnected ? 0.72f : 1f);
            updateConnectionBadge();
            refreshMetadata();
        }

        private void updateConnectionBadge()
        {
            if (displayedConnectionState == info.state)
            {
                return;
            }
            displayedConnectionState = info.state;
            boolean connecting = info.state == ConnectionInfo.STATE_CONNECTING;
            connectionWaves.setVisibility(connecting ? View.VISIBLE : View.GONE);
            if (connecting)
            {
                connectionWaves.startWaves();
            }
            else
            {
                connectionWaves.stopWaves();
            }
            if (info.state == ConnectionInfo.STATE_CONNECTED)
            {
                connectionBadge.setVisibility(View.GONE);
                return;
            }
            String label;
            int foreground, background, border;
            if (info.state == ConnectionInfo.STATE_CONNECTING)
            {
                label = "연결 중";
                foreground = 0xfff3d5df;
                background = 0xf24c303d;
                border = 0xffbe8b9d;
            }
            else if (info.state == ConnectionInfo.STATE_FAILED)
            {
                label = "실패";
                foreground = 0xffffd4b1;
                background = 0xf2513d32;
                border = 0xffb88d6b;
            }
            else
            {
                label = "해제";
                foreground = 0xffdce4e9;
                background = 0xf23a4149;
                border = 0xff8b9aa5;
            }
            GradientDrawable surface = new GradientDrawable();
            surface.setColor(background);
            surface.setCornerRadius(dp(6));
            surface.setStroke(dp(1), border);
            connectionBadge.setBackground(surface);
            connectionBadge.setTextColor(foreground);
            setTextIfChanged(connectionBadge, label);
            connectionBadge.setVisibility(View.VISIBLE);
        }

        void refreshMetadata()
        {
            if (info == null)
            {
                return;
            }
            DeviceBadgeInfo metadata = activity.getSessionDeviceBadgeInfo(info.id);
            if (metadata != null)
            {
                lastMetadata = metadata;
            }
            String name = lastMetadata == null ? "장치" : value(lastMetadata.userName, "장치");
            String side = lastMetadata == null ? "—" : value(lastMetadata.wearingPosition, "—");
            String serial = lastMetadata == null ? "—" : value(lastMetadata.serialNumber, "—");
            String version = lastMetadata == null ? "R—" : releaseName(lastMetadata.releaseName);
            String state = stateLabel(info.state);
            setTextIfChanged(wearingPosition, side);
            setTextIfChanged(userName, name);
            setTextIfChanged(serialNumber, serial);
            setTextIfChanged(release, version);
            userName.setTextColor(info.state == ConnectionInfo.STATE_FAILED ? 0xffe7b58e : 0xffe1ebec);
            String label = DeviceLabelStyle.forAccessibility(info.label);
            panel.setContentDescription(label + ", " + version + ", " + state
                + (info.selected ? ", 선택한 제어 대상" : "")
                + (info.state == ConnectionInfo.STATE_CONNECTED || info.state == ConnectionInfo.STATE_CONNECTING
                ? ". 눌러 제어 대상으로 선택. 길게 눌러 연결 해제" : ". 눌러 다시 연결"));
        }

        void select()
        {
            if (info == null)
            {
                return;
            }
            if (info.state != ConnectionInfo.STATE_CONNECTED && info.state != ConnectionInfo.STATE_CONNECTING)
            {
                if (activity.reconnectDeviceForControl(info.address))
                {
                    onSelectionChanged.run();
                }
                else
                {
                    Toast.makeText(activity, "사용자·외부기 체크 상태와 진행 중인 작업을 확인하세요.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (Objects.equals(info.id, activity.getSelectedSessionId()))
            {
                return;
            }
            if (activity.selectDeviceSession(info.id))
            {
                onSelectionChanged.run();
            }
            else
            {
                Toast.makeText(activity, "진행 중인 FF·관리 작업과 장치 연결 상태를 확인하세요.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private TextView text(int color)
    {
        TextView text = new TextView(activity);
        text.setTextSize(12);
        text.setTypeface(typeface);
        text.setTextColor(color);
        text.setIncludeFontPadding(false);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return text;
    }

    private LinearLayout row()
    {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private static void setTextIfChanged(TextView view, String text)
    {
        if (!text.contentEquals(view.getText()))
        {
            view.setText(text);
        }
    }

    private void revealSelected(Row row, boolean selectionChanged)
    {
        if (row.info == null || !Objects.equals(lastSelectedId, row.info.id))
        {
            return;
        }
        if (userInteracting || (!selectionChanged && SystemClock.uptimeMillis() < userInteractionQuietUntil))
        {
            return;
        }
        int left = row.panel.getLeft();
        int right = row.panel.getRight();
        int viewport = scroll.getWidth() - scroll.getPaddingEnd();
        if (left < scroll.getScrollX())
        {
            scroll.smoothScrollTo(left, 0);
        }
        else if (right > scroll.getScrollX() + viewport)
        {
            scroll.smoothScrollTo(right - viewport, 0);
        }
    }

    private static String addressKey(ConnectionInfo info)
    {
        return info.address == null || info.address.isEmpty() ? info.id : info.address;
    }

    private static String value(String value, String fallback)
    {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static String releaseName(String name)
    {
        return name == null || name.trim().isEmpty() ? "R—" : name.replaceFirst("^REL[- ]?", "R");
    }

    private static String stateLabel(int state)
    {
        switch (state)
        {
            case ConnectionInfo.STATE_CONNECTED:
                return "연결됨";
            case ConnectionInfo.STATE_CONNECTING:
                return "연결 중";
            case ConnectionInfo.STATE_FAILED:
                return "연결 실패";
            default:
                return "연결 해제";
        }
    }

    private int dp(int value)
    {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
