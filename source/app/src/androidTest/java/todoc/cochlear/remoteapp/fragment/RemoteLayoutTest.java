package todoc.cochlear.remoteapp.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    public void activityHasNoLegacyLockOverlayOrManualEntry()
    {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
        {
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            Context context = new ContextThemeWrapper(target, R.style.app_theme);
            View root = LayoutInflater.from(context).inflate(R.layout.activity_main, null, false);
            measure(root, 411, 760);
            ViewGroup main = root.findViewById(R.id.main_root);
            assertEquals("Only content and the required BLE pairing prompt remain", 2, main.getChildCount());
            assertEquals(View.GONE, root.findViewById(R.id.pairing_key_layout).getVisibility());
            assertTrue(root.findViewById(R.id.frame).getHeight() > 0);
            for (String removed : new String[]{"lock_screen", "forgot_password", "switch_lock_screen", "button_manual"})
            {
                assertEquals(removed, 0, context.getResources().getIdentifier(removed, "id", target.getPackageName()));
            }
        });
    }

    @Test
    public void phoneTabsAndMultiDeviceOtaContentFitAndScroll()
    {
        verify(411, 760, false, 1.0f, "phone");
    }

    @Test
    public void narrowPhoneWithLargeFont()
    {
        verify(360, 740, false, 1.3f, "phone-large-font");
    }

    @Test
    public void tabletRetainsApprovedSingleColumnFlowWithLargeFont()
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
            int[] layouts = {R.layout.fragment_user, R.layout.fragment_user_group,
                R.layout.fragment_device, R.layout.fragment_add_device,
                R.layout.fragment_edit_device};
            for (int layout : layouts)
            {
                // include를 감싼 투명 루트도 있으므로 실제 Activity의 frame 안에 붙여 그린다.
                View host = LayoutInflater.from(context).inflate(R.layout.activity_main, null, false);
                android.widget.FrameLayout frame = host.findViewById(R.id.frame);
                com.google.android.material.appbar.MaterialToolbar toolbar = host.findViewById(R.id.toolbar);
                toolbar.getMenu().clear();
                toolbar.setTitle("등록 관리");
                host.findViewById(R.id.toolbar_navigation_message).setVisibility(View.GONE);
                View root = LayoutInflater.from(context).inflate(layout, frame, false);
                frame.addView(root, new android.widget.FrameLayout.LayoutParams(-1, -1));
                populateFormValues(root);
                todoc.cochlear.remoteapp.ui.GlassSurfaces.lift(root);
                measure(host, 360, 740);
                assertEquals(frame.getWidth(), root.getWidth());
                assertEquals(frame.getHeight(), root.getHeight());
                assertTrue("Activity 앱바 아래에서 화면이 그려져야 합니다", frame.getTop() >= toolbar.getHeight());
                assertTrue("배경은 투명 include 루트 또는 내부 글래스 표면에서 제공되어야 합니다",
                    visibleBackgroundCount(root) > 0);
                assertVisibleChildrenFitWidth(root);
                assertFormValuesReadable(root);
                capture(target, host, 360, 740, context.getResources().getResourceEntryName(layout));
            }
        });
    }

    @Test
    public void toolbarTitleCentersWithLegacyBackLabelHidden()
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
            back.setVisibility(View.GONE);
            measure(root, 360, 740);
            assertEquals(View.GONE, back.getVisibility());
            boolean found = false;
            for (int i = 0; i < toolbar.getChildCount(); i++)
            {
                View v = toolbar.getChildAt(i);
                if (v instanceof android.widget.TextView && "사용자 등록".contentEquals(((android.widget.TextView) v).getText()))
                {
                    TextView title = (TextView) v;
                    assertEquals(1, title.getLineCount());
                    assertTrue(Math.abs(v.getLeft() + v.getRight() - toolbar.getWidth()) <= 2);
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
            b.remoteControlConnectionLayout.setVisibility(View.GONE);
            b.remoteControlConnectionUserNameTextview.setText("PEK");
            b.identitySideTextview.setText("L");
            b.identityExternalTextview.setText("FFFF");
            b.remoteControlConnectionIsdIdTextview.setText("#1A31FFFF");
            b.otaFwReleaseTextview.setText("R4.6");
            b.batteryPercentTextview.setText("80%");
            b.batteryVoltageTextview.setText("3.800 V");
            b.batteryCurrentTextview.setText("+30.0 mA");
            b.batteryPowerTextview.setText("+114.0 mW");
            b.otaProgramTextview.setText("Prog 1");
            b.otaVolumeTextview.setText("Vol 1");
            b.otaMaxOutputTextview.setText("Out 4");
            b.otaStrategyTextview.setText("Strategy CIS");
            b.otaPulseWidthTextview.setText("PW 25us");
            b.otaFrameNumTextview.setText("Frm 2");
            b.otaBacktelTextview.setText("BT 300ms");
            b.otaForcePmicTextview.setText("Fix Off");
            b.otaStepUpTextview.setText("Step 4");
            b.otaCurPmicTextview.setText("Cur 3.000 V");
            b.otaPmicTextview.setText("PMIC 2.800 V");
            b.otaMappingPmicTextview.setText("Map 4.300 V");
            b.otaMaxPmicTextview.setText("Max 5.300 V");
            b.otaFfControls.setVisibility(View.VISIBLE);
            b.ffUnavailableText.setVisibility(View.GONE);
            b.otaFf3SettingsButton.setVisibility(View.VISIBLE);
            b.otaFf3DiagnosticsButton.setVisibility(View.VISIBLE);
            assertEquals("25 ㎲", b.otaPulseWidthTextview.getText().toString());
            assertEquals("300 ms", b.otaBacktelTextview.getText().toString());
            assertEquals("+30.0 ㎃", b.batteryCurrentTextview.getText().toString());
            assertEquals("+114.0 ㎽", b.batteryPowerTextview.getText().toString());
            assertEquals("3800㎷", RemoteControlFragment.displayStatusValue("3800mV"));
            assertTrue(b.batteryCurrentTextview.getPaint().hasGlyph("㎃"));
            assertTrue(b.batteryPowerTextview.getPaint().hasGlyph("㎽"));
            View root = b.getRoot();
            assertMissingView(context, root, "battery_detail_textview");
            assertMissingView(context, root, "ota_mapping_notice");
            assertMissingView(context, root, "ota_prepare_hint");
            assertEquals(View.GONE, b.controlProgramValue.getVisibility());
            assertEquals(View.GONE, b.controlVolumeValue.getVisibility());
            assertEquals(View.GONE, b.controlOutputValue.getVisibility());
            assertEquals(View.GONE, b.otaGatingOffsetTextview.getVisibility());
            assertEquals(LinearLayout.VERTICAL, b.remotePrimaryLayout.getOrientation());
            assertEquals(b.remoteContentScroll, b.remotePrimaryLayout.getParent());
            assertEquals(b.remotePrimaryLayout, b.remoteStatusColumn.getParent());
            assertEquals(b.remotePrimaryLayout, b.remoteControlsColumn.getParent());
            assertEquals(1, b.remoteContentScroll.getChildCount());
            assertEquals(root, b.remoteControlConnectionLayout.getParent());
            String[] tabs = {"기본 제어", "특수 제어", "FF", "세부 기능", "OTA"};
            assertEquals(tabs.length, b.controlTabs.getTabCount());
            for (int i = 0; i < tabs.length; i++)
            {
                assertEquals(tabs[i], b.controlTabs.getTabAt(i).getText().toString());
            }

            RemoteControlFragment.showControlTab(b, 0);
            measure(root, width, height);
            assertStatusVisible(b);
            assertEquals(View.VISIBLE, b.otaRemoteBox.getVisibility());
            assertEquals(View.GONE, b.otaEtcBox.getVisibility());
            assertEquals(View.GONE, b.otaExpandableContent.getVisibility());
            assertEquals(0, b.controlTabs.getLeft());
            assertEquals(width, b.controlTabs.getRight());
            assertEquals(height, b.controlTabs.getBottom());
            assertTrue(b.controlTabs.getTop() >= b.remoteContentScroll.getBottom());
            ViewGroup tabStrip = (ViewGroup) b.controlTabs.getChildAt(0);
            for (int i = 1; i < tabStrip.getChildCount(); i++)
            {
                assertTrue(Math.abs(tabStrip.getChildAt(0).getWidth() - tabStrip.getChildAt(i).getWidth()) <= 1);
            }
            Rect battery = bounds(root, b.batteryTelemetryBox);
            View pmic = (View) b.otaCurPmicTextview.getParent().getParent();
            Rect pmicBounds = bounds(root, pmic);
            Rect status = bounds(root, b.statusExpandableContent);
            assertEquals(battery.left, pmicBounds.left);
            assertEquals(battery.right, pmicBounds.right);
            assertEquals(battery.left, status.left);
            assertEquals(battery.right, status.right);
            assertTrue(pmicBounds.top >= battery.bottom);
            assertTrue(status.top >= pmicBounds.bottom);
            assertEquals(bounds(root, b.otaProgramDownButton).left, bounds(root, b.otaMaxOutputDownButton).left);
            assertEquals(bounds(root, b.otaProgramUpButton).right, bounds(root, b.otaMaxOutputUpButton).right);
            assertEquals(bounds(root, b.otaVolumeDownButton).left, bounds(root, b.otaLedButton).left);
            assertEquals(bounds(root, b.otaVolumeUpButton).right, bounds(root, b.otaAlarmButton).right);
            assertEquals(b.otaLedButton.getHeight(), b.otaProgramDownButton.getHeight());
            assertEquals(b.otaAlarmButton.getHeight(), b.otaVolumeUpButton.getHeight());
            assertNoClipping(b.batteryPercentTextview, b.batteryVoltageTextview,
                b.batteryCurrentTextview, b.batteryPowerTextview);
            capture(target, root, width, height, name + "-basic");

            RemoteControlFragment.showControlTab(b, 1);
            measure(root, width, height);
            assertStatusVisible(b);
            assertEquals(View.GONE, b.otaRemoteBox.getVisibility());
            assertEquals(View.VISIBLE, b.otaEtcBox.getVisibility());
            assertEquals(3, b.specialControlGrid.getColumnCount());
            assertEquals(11, b.specialControlGrid.getChildCount());
            capture(target, root, width, height, name + "-special");

            RemoteControlFragment.showControlTab(b, 2);
            measure(root, width, height);
            assertStatusVisible(b);
            assertEquals(View.VISIBLE, b.ffTabBox.getVisibility());
            assertEquals(View.GONE, b.otaEtcBox.getVisibility());
            assertEquals(View.GONE, b.otaExpandableContent.getVisibility());
            assertEquals(b.ffTabBox, b.otaFfControls.getParent());
            capture(target, root, width, height, name + "-ff");

            // 실제 장치 명령 없이 여러 행이 누적될 때의 OTA 스크롤 구조를 검증한다.
            for (int i = 0; i < 6; i++)
            {
                TextView row = new TextView(context);
                row.setText("시험 장치 " + (i + 1) + " · 42% · 8,240 Bytes/s");
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                b.otaDeviceList.addView(row, new LinearLayout.LayoutParams(-1, 86));
            }
            RemoteControlFragment.showControlTab(b, 3);
            measure(root, width, height);
            assertEquals(4, b.controlTabs.getSelectedTabPosition());
            assertEquals(View.GONE, b.remoteStatusColumn.getVisibility());
            assertEquals(View.GONE, b.connectionTargetSelector.getVisibility());
            assertEquals(View.VISIBLE, b.otaExpandableContent.getVisibility());
            assertEquals(View.GONE, b.otaSlotBox.getVisibility());
            assertEquals(View.GONE, b.otaCautionBox.getVisibility());
            assertEquals(LinearLayout.VERTICAL, b.otaDeviceList.getOrientation());
            Rect files = bounds(root, b.otaUpdateContent);
            Rect devices = bounds(root, b.otaDevicesGroup);
            Rect actions = bounds(root, b.otaRecoveryContent);
            assertTrue(devices.top >= files.bottom);
            assertTrue(actions.top >= devices.bottom);
            assertTrue(bounds(root, b.otaAddDeviceButton).top >= bounds(root, b.otaDeviceList).bottom);
            for (int i = 1; i < b.otaDeviceList.getChildCount(); i++)
            {
                assertTrue(b.otaDeviceList.getChildAt(i).getTop() >= b.otaDeviceList.getChildAt(i - 1).getBottom());
            }
            int maxScroll = Math.max(0, b.remoteContentScroll.getChildAt(0).getHeight()
                - b.remoteContentScroll.getHeight() + b.remoteContentScroll.getPaddingTop()
                + b.remoteContentScroll.getPaddingBottom());
            assertTrue("여러 OTA 장치와 마지막 제어까지 스크롤로 접근해야 합니다", maxScroll > 0);
            capture(target, root, width, height, name + "-ota");
            b.remoteContentScroll.scrollTo(0, maxScroll);
            assertEquals(maxScroll, b.remoteContentScroll.getScrollY());
            assertTrue(bounds(root, b.otaRecoveryContent).bottom <= b.controlTabs.getTop());
            assertEquals(height, b.controlTabs.getBottom());
            capture(target, root, width, height, name + "-ota-bottom");
            b.remoteContentScroll.scrollTo(0, 0);
            RemoteControlFragment.showControlTab(b, 0);
            measure(root, width, height);
            assertStatusVisible(b);
            assertEquals(View.GONE, b.otaExpandableContent.getVisibility());
        });
    }

    private void assertStatusVisible(FragmentRemoteControlBinding b)
    {
        assertEquals(View.VISIBLE, b.remoteStatusColumn.getVisibility());
        assertEquals(View.VISIBLE, b.connectionTargetSelector.getVisibility());
        assertEquals(View.VISIBLE, b.batteryTelemetryBox.getVisibility());
        assertEquals(View.VISIBLE, b.otaStatusInfoBox.getVisibility());
        assertEquals(View.VISIBLE, b.statusExpandableContent.getVisibility());
        assertEquals(View.GONE, b.otaFfStatusTextview.getVisibility());
    }

    private void assertNoClipping(TextView... views)
    {
        for (TextView view : views)
        {
            assertNotNull(view.getLayout());
            assertEquals(1, view.getLayout().getLineCount());
            assertEquals(0, view.getLayout().getEllipsisCount(0));
            assertTrue("상태 값 잘림: " + view.getText(), view.getLayout().getLineWidth(0)
                <= view.getWidth() - view.getCompoundPaddingLeft() - view.getCompoundPaddingRight() + 1);
        }
    }

    private Rect bounds(View root, View child)
    {
        Rect result = new Rect(0, 0, child.getWidth(), child.getHeight());
        ((ViewGroup) root).offsetDescendantRectToMyCoords(child, result);
        return result;
    }

    private void assertMissingView(Context context, View root, String resourceName)
    {
        int id = context.getResources().getIdentifier(resourceName, "id", context.getPackageName());
        assertTrue("제거한 중간 안내가 다시 나타났습니다: " + resourceName,
            id == 0 || root.findViewById(id) == null);
    }

    private void populateFormValues(View view)
    {
        if (view.getVisibility() != View.VISIBLE)
        {
            return;
        }
        if (view instanceof android.widget.EditText)
        {
            TextView text = (TextView) view;
            String id = view.getId() == View.NO_ID ? "" : view.getResources().getResourceEntryName(view.getId());
            String value = id.contains("pairing") ? "123456" : id.contains("key") ? "1234"
                                                               : id.contains("serial") ? "FFFF" : id.contains("initial") ? "PEK" : "시험 별칭";
            text.setText(value);
        }
        if (view instanceof ViewGroup)
        {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
            {
                populateFormValues(group.getChildAt(i));
            }
        }
    }

    private int visibleBackgroundCount(View view)
    {
        if (view.getVisibility() != View.VISIBLE)
        {
            return 0;
        }
        int count = view.getBackground() == null ? 0 : 1;
        if (view instanceof ViewGroup)
        {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
            {
                count += visibleBackgroundCount(group.getChildAt(i));
            }
        }
        return count;
    }

    private void assertVisibleChildrenFitWidth(View view)
    {
        if (view.getVisibility() != View.VISIBLE || !(view instanceof ViewGroup))
        {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++)
        {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE)
            {
                continue;
            }
            assertTrue("화면의 왼쪽을 벗어난 항목: " + child, child.getLeft() >= -1);
            assertTrue("화면의 오른쪽을 벗어난 항목: " + child, child.getRight() <= group.getWidth() + 1);
            assertVisibleChildrenFitWidth(child);
        }
    }

    private void assertFormValuesReadable(View view)
    {
        if (view.getVisibility() != View.VISIBLE)
        {
            return;
        }
        if (view instanceof android.widget.EditText)
        {
            TextView input = (TextView) view;
            assertNotNull(input.getTypeface());
            assertTrue(input.getPaint().hasGlyph("한"));
            assertNoClipping(input);
        }
        if (view instanceof ViewGroup)
        {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
            {
                assertFormValuesReadable(group.getChildAt(i));
            }
        }
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
