package todoc.cochlear.remoteapp.ui;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import todoc.cochlear.remoteapp.connection.ConnectionInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectionSelectorStateTest
{
    private ConnectionInfo device(String name, int state, boolean selected)
    {
        return new ConnectionInfo(name, name, name, state, selected, true);
    }

    @Test
    public void disconnectedSelectedTargetIsNotPresentedAsTheOtherLiveConnection()
    {
        ConnectionSelectorState state = ConnectionSelectorState.from(Arrays.asList(
            device("A", ConnectionInfo.STATE_DISCONNECTED, true),
            device("B", ConnectionInfo.STATE_CONNECTED, false)));
        assertEquals("연결 해제 · A", state.targetLabel);
        assertEquals("총 1대 연결", state.totalLabel);
        assertTrue(state.contentDescription.contains("제어 대상 A. 선택 장치 상태 연결 해제"));
        assertTrue(state.contentDescription.contains("전체 연결 장치 1대"));
    }

    @Test
    public void aLiveConnectionDoesNotImplyThatATargetHasBeenSelected()
    {
        ConnectionSelectorState state = ConnectionSelectorState.from(Collections.singletonList(
            device("B", ConnectionInfo.STATE_CONNECTED, false)));
        assertEquals("선택 없음", state.targetLabel);
        assertEquals("총 1대 연결", state.totalLabel);
        assertTrue(state.contentDescription.startsWith("선택한 제어 대상 없음."));
    }

    @Test
    public void pendingUnselectedConnectionIsNotCountedAsLiveOrSelected()
    {
        ConnectionSelectorState state = ConnectionSelectorState.from(Collections.singletonList(
            device("B", ConnectionInfo.STATE_CONNECTING, false)));
        assertEquals("선택 없음", state.targetLabel);
        assertEquals("총 0대 연결", state.totalLabel);
    }

    @Test
    public void connectingSelectedTargetShowsItsOwnStateEvenWithAnotherLiveDevice()
    {
        ConnectionSelectorState state = ConnectionSelectorState.from(Arrays.asList(
            device("A", ConnectionInfo.STATE_CONNECTING, true),
            device("B", ConnectionInfo.STATE_CONNECTED, false)));
        assertEquals("연결 중 · A", state.targetLabel);
        assertEquals("총 1대 연결", state.totalLabel);
    }

    @Test
    public void failedTargetStatePrecedesALongEllipsizedName()
    {
        ConnectionSelectorState state = ConnectionSelectorState.from(Collections.singletonList(
            device("이름이 길어서 선택 행에서 끝이 생략될 수 있는 장치", ConnectionInfo.STATE_FAILED, true)));
        assertTrue(state.targetLabel.startsWith("연결 실패 · "));
        assertEquals("총 0대 연결", state.totalLabel);
    }

    @Test
    public void healthySelectedTargetKeepsItsCompactName()
    {
        ConnectionSelectorState state = ConnectionSelectorState.from(Arrays.asList(
            device("A", ConnectionInfo.STATE_CONNECTED, true),
            device("B", ConnectionInfo.STATE_CONNECTED, false)));
        assertEquals("A", state.targetLabel);
        assertEquals("총 2대 연결", state.totalLabel);
        assertTrue(state.contentDescription.contains("선택 장치 상태 연결됨"));
    }

    @Test
    public void initialEmptyStateHasNoSelectedTargetOrLiveConnections()
    {
        ConnectionSelectorState state = ConnectionSelectorState.from(null);
        assertEquals("선택 없음", state.targetLabel);
        assertEquals("총 0대 연결", state.totalLabel);
    }
}
