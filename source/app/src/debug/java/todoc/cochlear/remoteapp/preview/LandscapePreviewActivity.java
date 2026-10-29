package todoc.cochlear.remoteapp.preview;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** 로컬 HTML을 표시하는 debug 전용 화면 검토 진입점. BLE/Room에 연결하지 않는다. */
public final class LandscapePreviewActivity extends Activity {
    private WebView preview;
    private int cutoutLeftPx, cutoutTopPx, cutoutRightPx, cutoutBottomPx;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        configureDisplay();
        preview = new WebView(this) {
            @Override public InputConnection onCreateInputConnection(EditorInfo attributes) {
                InputConnection connection = super.onCreateInputConnection(attributes);
                // Landscape editing should keep the target and Save action visible.
                attributes.imeOptions |= EditorInfo.IME_FLAG_NO_EXTRACT_UI
                        | EditorInfo.IME_FLAG_NO_FULLSCREEN;
                return connection;
            }
        };
        preview.setBackgroundColor(0xff29364c);
        preview.getSettings().setJavaScriptEnabled(true);
        preview.getSettings().setAllowFileAccess(true);
        preview.getSettings().setAllowContentAccess(false);
        preview.getSettings().setBlockNetworkLoads(true);
        preview.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                DisplayCutout cutout = insets.getDisplayCutout();
                cutoutLeftPx = cutout == null ? 0 : cutout.getSafeInsetLeft();
                cutoutTopPx = cutout == null ? 0 : cutout.getSafeInsetTop();
                cutoutRightPx = cutout == null ? 0 : cutout.getSafeInsetRight();
                cutoutBottomPx = cutout == null ? 0 : cutout.getSafeInsetBottom();
                applyCutoutInsets();
            }
            return insets;
        });
        preview.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !request.getUrl().toString().startsWith("file:///android_asset/landscape-preview/");
            }

            @Override public void onPageFinished(WebView view, String url) {
                applyCutoutInsets();
            }
        });
        setContentView(preview, new ViewGroup.LayoutParams(-1, -1));
        preview.requestApplyInsets();
        showPreview();
    }

    private void applyCutoutInsets() {
        // 네이티브 px를 WebView CSS px로 변환하고 배경에는 여백을 주지 않는다.
        preview.evaluateJavascript("(function(){var d=window.devicePixelRatio||1;"
                + "var s=document.documentElement.style;"
                + "s.setProperty('--cutout-left',(" + cutoutLeftPx + "/d)+'px');"
                + "s.setProperty('--cutout-top',(" + cutoutTopPx + "/d)+'px');"
                + "s.setProperty('--cutout-right',(" + cutoutRightPx + "/d)+'px');"
                + "s.setProperty('--cutout-bottom',(" + cutoutBottomPx + "/d)+'px');"
                + "})()", null);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        configureDisplay();
        showPreview();
    }

    private void configureDisplay() {
        boolean portrait = "portrait".equals(getIntent().getStringExtra("design"));
        setRequestedOrientation(portrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(portrait ? 0
                : View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        getWindow().setStatusBarColor(0xff181919);
        getWindow().setNavigationBarColor(0xff181919);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.layoutInDisplayCutoutMode = portrait
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(attributes);
        }
    }

    private void showPreview() {
        String page = getIntent().getStringExtra("page");
        if (page == null || !page.matches("remote|advanced|ff|management|observation|register|ota|logs|disconnected")) page = "remote";
        String fw = getIntent().getStringExtra("fw");
        if (fw == null || !fw.matches("44|45|46")) fw = "45";
        boolean phone = "phone".equals(getIntent().getStringExtra("design"));
        boolean portrait = "portrait".equals(getIntent().getStringExtra("design"));
        if ((phone || portrait) && "more".equals(getIntent().getStringExtra("page"))) page = "more";
        String section = getIntent().getStringExtra("section");
        if (section == null || !section.matches("pmic|link|identity|maps|update|recovery")) section = "";
        String edit = getIntent().getStringExtra("edit");
        if (edit == null || !edit.matches("user|device")) edit = "";
        preview.loadUrl("file:///android_asset/landscape-preview/" + (portrait ? "portrait/" : phone ? "phone/" : "")
                + "app.html?page=" + page + "&fw=" + fw + "&section=" + section + "&edit=" + edit);
    }

    @Override public void onBackPressed() {
        preview.evaluateJavascript("window.phonePreview ? window.phonePreview.back() : false",
                consumed -> { if (!"true".equals(consumed)) finish(); });
    }

    @Override public void onDestroy() {
        if (preview != null) {
            if (preview.getParent() instanceof ViewGroup) {
                ((ViewGroup)preview.getParent()).removeView(preview);
            }
            preview.destroy();
            preview = null;
        }
        super.onDestroy();
    }
}
