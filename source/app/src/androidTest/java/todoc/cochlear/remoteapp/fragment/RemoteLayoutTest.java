package todoc.cochlear.remoteapp.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

import todoc.cochlear.remoteapp.activity.R;
import todoc.cochlear.remoteapp.activity.databinding.FragmentRemoteControlBinding;

import static org.junit.Assert.*;

/**
 * 실제 Android 위젯의 측정/그리기. BLE 연결이나 장치 명령은 사용하지 않는다.
 */
@RunWith(AndroidJUnit4.class)
public class RemoteLayoutTest
{
    @Test
    public void phoneContentScrollsWithOtaExpanded()
    {
        verify(411, 760, false, 1.0f, "phone");
    }

    @Test
    public void narrowPhoneWithLargeFont()
    {
        verify(360, 740, false, 1.3f, "phone-large-font");
    }

    @Test
    public void tabletColumnsAndExpandedOtaFitLargeFont()
    {
        verify(1100, 650, true, 1.3f, "tablet");
    }

    @Test
    public void managementScreensRenderGlass()
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
        {
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Configuration cfg = new Configuration(target.getResources().getConfiguration());
            cfg.densityDpi = 160;
            cfg.fontScale = 1.3f;
            Context context = new ContextThemeWrapper(target.createConfigurationContext(cfg), R.style.app_theme);
            int[] layouts = {R.layout.fragment_settings, R.layout.fragment_user, R.layout.fragment_add_user,
                R.layout.fragment_edit_user, R.layout.fragment_device, R.layout.fragment_add_device,
                R.layout.fragment_edit_device};
            for (int layout : layouts)
            {
                View root = LayoutInflater.from(context).inflate(layout, null, false);
                measure(root, 360, 740);
                assertNotNull(root.getBackground());
                capture(target, root, 360, 740, context.getResources().getResourceEntryName(layout));
            }
        });
    }

    @Test
    public void toolbarBackLabelDoesNotOverlapTitle()
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
        {
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Configuration cfg = new Configuration(target.getResources().getConfiguration());
            cfg.densityDpi = 160;
            cfg.fontScale = 1.3f;
            Context context = new ContextThemeWrapper(target.createConfigurationContext(cfg), R.style.app_theme);
            View root = LayoutInflater.from(context).inflate(R.layout.activity_main, null, false);
            com.google.android.material.appbar.MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
            android.widget.TextView back = root.findViewById(R.id.toolbar_navigation_message);
            toolbar.getMenu().clear();
            toolbar.setTitle("사용자 등록");
            back.setText("사용자 목록");
            measure(root, 360, 740);
            assertEquals(1, back.getLineCount());
            boolean found = false;
            for (int i = 0; i < toolbar.getChildCount(); i++)
            {
                View v = toolbar.getChildAt(i);
                if (v instanceof android.widget.TextView && "사용자 등록".contentEquals(((android.widget.TextView) v).getText()))
                {
                    assertTrue(v.getLeft() >= back.getRight());
                    found = true;
                }
            }
            assertTrue(found);
        });
    }

    private void verify(int width, int height, boolean tablet, float fontScale, String name)
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
        {
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Configuration cfg = new Configuration(target.getResources().getConfiguration());
            cfg.densityDpi = 160;
            cfg.screenWidthDp = width;
            cfg.screenHeightDp = height;
            cfg.smallestScreenWidthDp = Math.min(width, height);
            cfg.orientation = tablet ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
            cfg.fontScale = fontScale;
            Context context = new ContextThemeWrapper(target.createConfigurationContext(cfg), R.style.app_theme);
            FragmentRemoteControlBinding b = FragmentRemoteControlBinding.inflate(LayoutInflater.from(context));
            RemoteControlFragment.configureColumns(b, tablet, context.getResources().getDisplayMetrics().density);
            RemoteControlFragment.showControlTab(b, 0);
            b.remoteControlConnectionLayout.setVisibility(View.GONE);
            b.remoteControlConnectionUserNameTextview.setText("화면 검증용 사용자");
            b.remoteControlConnectionIsdIdTextview.setText("#1A31FFFF");
            b.otaFwReleaseTextview.setText("REL4.6");
            b.batteryPercentTextview.setText("80%");
            b.batteryVoltageTextview.setText("3.800 V");
            b.batteryCurrentTextview.setText("+30.0 mA");
            b.batteryPowerTextview.setText("+114.0 mW");
            b.batteryDetailTextview.setVisibility(View.GONE);
            b.otaProgramTextview.setText("Prog 1");
            b.otaVolumeTextview.setText("Vol 1");
            b.otaMaxOutputTextview.setText("Out 4");
            b.otaFfStatusTextview.setVisibility(View.VISIBLE);
            b.otaFfStatusTextview.setText("FF3 On · Zones 6 · Limit 11 · CFX OK");
            b.otaStrategyTextview.setText("Strategy CIS");
            b.otaPulseWidthTextview.setText("PW 25us");
            b.otaFrameNumTextview.setText("Frm 2");
            b.otaBacktelTextview.setText("BT 300ms");
            b.otaForcePmicTextview.setText("Fix Off");
            b.otaStepUpTextview.setText("Step 4");
            b.otaCurPmicTextview.setText("Cur 3.0V");
            b.otaPmicTextview.setText("PMIC 2.8V");
            b.otaMappingPmicTextview.setText("Map 4.3V");
            b.otaMaxPmicTextview.setText("Max 5.3V");
            b.otaFfControls.setVisibility(View.VISIBLE);
            b.otaFf3SettingsButton.setVisibility(View.VISIBLE);
            b.otaFf3DiagnosticsButton.setVisibility(View.VISIBLE);
            assertEquals("25 ㎲", b.otaPulseWidthTextview.getText().toString());
            assertEquals("300 ms", b.otaBacktelTextview.getText().toString());
            assertEquals("1", b.controlProgramValue.getText().toString());
            assertEquals("4", b.controlOutputValue.getText().toString());
            View root = b.getRoot();
            measure(root, width, height);
            assertEquals(View.GONE, b.otaExpandableContent.getVisibility());
            assertEquals(1, b.remoteContentScroll.getChildCount());
            assertEquals(0, b.otaLedButton.getLeft());
            assertEquals(((View) b.otaAlarmButton.getParent()).getWidth(), b.otaAlarmButton.getRight());
            assertEquals(root, b.remoteControlConnectionLayout.getParent());
            assertEquals(tablet ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL, b.remotePrimaryLayout.getOrientation());
            if (tablet)
            {
                assertTrue(b.remoteControlsColumn.getLeft() > b.remoteStatusColumn.getRight());
                assertEquals(b.remoteStatusColumn.getTop(), b.remoteControlsColumn.getTop());
            }
            assertEquals(0, b.remoteContentScroll.getScrollY());
            assertTrue(b.otaIdBox.getTop() >= 0);
            assertTrue(b.remoteControlConnectionUserNameTextview.getTop() >= b.otaIdBox.getPaddingTop());
            assertTrue(b.remoteControlConnectionUserNameTextview.getLayout().getEllipsisCount(0) == 0);
            assertTrue(b.batteryPowerTextview.getWidth() > 0);
            assertTrue(b.batteryPowerTextview.getLayout().getEllipsisCount(0) == 0);
            capture(target, root, width, height, name + "-collapsed");
            RemoteControlFragment.showControlTab(b, 1);
            assertEquals(View.GONE, b.otaRemoteBox.getVisibility());
            assertEquals(View.VISIBLE, b.otaEtcBox.getVisibility());
            measure(root, width, height);
            capture(target, root, width, height, name + "-settings");
            assertEquals(View.VISIBLE, b.otaStatusInfoBox.getVisibility());
            assertEquals(View.VISIBLE, b.batteryTelemetryBox.getVisibility());
            int[] batteryPosition = new int[2];
            b.batteryTelemetryBox.getLocationOnScreen(batteryPosition);
            assertTrue("제어 영역은 최소 120dp 높이를 확보해야 합니다", b.remoteContentScroll.getHeight() >= 120);
            assertEquals(tablet ? 4 : 3, b.specialControlGrid.getColumnCount());
            assertEquals(12, b.specialControlGrid.getChildCount());
            assertEquals(4, b.controlTabs.getTabCount());
            assertEquals(0, b.controlTabs.getLeft());
            assertEquals(width, b.controlTabs.getRight());
            assertEquals(height, b.controlTabs.getBottom());
            int[] tabPosition = new int[2];
            b.controlTabs.getLocationOnScreen(tabPosition);
            assertTrue("Tabs remain below the primary layout", b.controlTabs.getTop() >= b.remoteContentScroll.getBottom());
            android.view.ViewGroup tabStrip = (android.view.ViewGroup) b.controlTabs.getChildAt(0);
            assertTrue("Tab widths: " + tabStrip.getChildAt(0).getWidth() + "/" + tabStrip.getChildAt(3).getWidth(), Math.abs(tabStrip.getChildAt(0).getWidth() - tabStrip.getChildAt(3).getWidth()) <= 1);
            assertEquals(LinearLayout.VERTICAL, b.statusExpandableContent.getOrientation());
            assertEquals(2, b.statusExpandableContent.getChildCount());
            measure(root, width, height);
            capture(target, root, width, height, name + "-status");
            RemoteControlFragment.showControlTab(b, 2);
            measure(root, width, height);
            assertEquals(View.VISIBLE, b.ffTabBox.getVisibility());
            assertEquals(View.GONE, b.otaEtcBox.getVisibility());
            assertEquals(View.GONE, b.otaExpandableContent.getVisibility());
            assertEquals(b.ffTabBox, b.otaFfControls.getParent());
            capture(target, root, width, height, name + "-ff");
            RemoteControlFragment.showControlTab(b, 3);
            measure(root, width, height);
            assertEquals(2, b.slotRadioButtonGroup.getChildCount());
            assertTrue(b.slotInformationRow.getTop() > b.slotRadioButtonGroup.getBottom());
            assertEquals(b.slot1RadioButton.getTop(), b.slot2RadioButton.getTop());
            int maxScroll = Math.max(0, b.remoteContentScroll.getChildAt(0).getHeight() - b.remoteContentScroll.getHeight() + b.remoteContentScroll.getPaddingTop() + b.remoteContentScroll.getPaddingBottom());
            if (!tablet)
            {
                assertTrue("휴대전화의 OTA 내용은 스크롤로 접근합니다", maxScroll > 0);
            }
            assertTrue(b.otaCautionBox.getBottom() <= b.otaExpandableContent.getHeight());
            capture(target, root, width, height, name + "-expanded");
            b.remoteContentScroll.scrollTo(0, maxScroll);
            assertEquals(maxScroll, b.remoteContentScroll.getScrollY());
            int[] afterScroll = new int[2];
            b.batteryTelemetryBox.getLocationOnScreen(afterScroll);
            assertEquals(b.remoteContentScroll, b.remotePrimaryLayout.getParent());
            assertEquals(b.remotePrimaryLayout, b.remoteStatusColumn.getParent());
            assertEquals(b.remotePrimaryLayout, b.remoteControlsColumn.getParent());
            assertEquals(maxScroll, b.remoteContentScroll.getScrollY());
            assertEquals(View.VISIBLE, b.otaStatusInfoBox.getVisibility());
            assertTrue(b.batteryTelemetryBox.getBottom() <= b.remoteStatusColumn.getHeight());
            capture(target, root, width, height, name + "-bottom");
            RemoteControlFragment.showControlTab(b, 0);
            measure(root, width, height);
            assertEquals(View.GONE, b.otaExpandableContent.getVisibility());
        });
    }

    private void measure(View root, int width, int height)
    {
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
    }

    private void capture(Context context, View root, int width, int height, String name)
    {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        File folder = new File(context.getExternalFilesDir(null), "layout-verification");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png")))
        {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        }
        catch (Exception error)
        {
            throw new AssertionError(error);
        }
        finally
        {
            bitmap.recycle();
        }
    }
}
