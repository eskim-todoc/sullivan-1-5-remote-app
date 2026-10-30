package todoc.cochlear.remoteapp.ui;

import java.util.List;

import todoc.cochlear.remoteapp.connection.ConnectionInfo;

/**
 * Keeps the selected target's connection state separate from the total live connection count.
 */
public final class ConnectionSelectorState
{
    public final String targetLabel;
    public final String totalLabel;
    public final String contentDescription;

    private ConnectionSelectorState(String targetLabel, String totalLabel, String contentDescription)
    {
        this.targetLabel = targetLabel;
        this.totalLabel = totalLabel;
        this.contentDescription = contentDescription;
    }

    public static ConnectionSelectorState from(List<ConnectionInfo> devices)
    {
        int connected = 0;
        ConnectionInfo selected = null;
        if (devices != null)
        {
            for (ConnectionInfo device : devices)
            {
                if (device.state == ConnectionInfo.STATE_CONNECTED)
                {
                    connected++;
                }
                if (device.selected)
                {
                    selected = device;
                }
            }
        }
        String total = "총 " + connected + "대 연결";
        if (selected == null)
        {
            return new ConnectionSelectorState("선택 없음", total,
                "선택한 제어 대상 없음. 전체 연결 장치 " + connected + "대. 장치 선택");
        }

        String name = selected.label == null || selected.label.isEmpty() ? "선택한 장치" : selected.label;
        String state;
        switch (selected.state)
        {
            case ConnectionInfo.STATE_CONNECTED:
                state = "연결됨";
                break;
            case ConnectionInfo.STATE_CONNECTING:
                state = "연결 중";
                break;
            case ConnectionInfo.STATE_DISCONNECTED:
                state = "연결 해제";
                break;
            case ConnectionInfo.STATE_FAILED:
                state = "연결 실패";
                break;
            default:
                state = "미연결";
                break;
        }
        // Put an unavailable target's state first so ellipsizing its name cannot hide the state.
        String label = selected.state == ConnectionInfo.STATE_CONNECTED ? name : state + " · " + name;
        return new ConnectionSelectorState(label, total, "제어 대상 " + name + ". 선택 장치 상태 " + state
            + ". 전체 연결 장치 " + connected + "대. 장치 선택");
    }
}
