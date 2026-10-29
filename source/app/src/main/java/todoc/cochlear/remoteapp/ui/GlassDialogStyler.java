package todoc.cochlear.remoteapp.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import todoc.cochlear.remoteapp.activity.R;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * 다이얼로그를 표시한 뒤 호출한다. 위치·크기·기존 dismiss listener는 유지한다.
 * 시스템 blur가 없는 기기에서는 Activity의 작은 정지 캡처만 흐려 받침으로 쓴다.
 */
public final class GlassDialogStyler {
    private static final int SNAPSHOT_MAX_EDGE = 256;
    private static final float CORNER_DP = 12f;
    private static final float BLUR_DP = 8f;
    private static final WeakHashMap<Window, FrostedBackdrop> APPLIED = new WeakHashMap<>();

    private GlassDialogStyler() { }

    public static void apply(Activity activity, AlertDialog dialog) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            activity.runOnUiThread(() -> apply(activity, dialog));
            return;
        }
        if (activity.isFinishing() || activity.isDestroyed() || !dialog.isShowing()) return;
        final Window window = dialog.getWindow();
        if (window == null) return;
        ensureReadableDefaultText(activity, dialog);
        if (APPLIED.containsKey(window)) return;

        final float density = activity.getResources().getDisplayMetrics().density;
        final View decor = window.getDecorView();
        final boolean reduceTransparency = reduceTransparency(activity);
        final boolean nativeBlur = !reduceTransparency && enableNativeBlur(activity, window, decor, density);
        final View activityDecor = activity.getWindow().getDecorView();
        final Rect source = new Rect();
        int[] location = new int[2];
        activityDecor.getLocationOnScreen(location);
        source.set(location[0], location[1], location[0] + activityDecor.getWidth(),
                location[1] + activityDecor.getHeight());
        Bitmap snapshot = nativeBlur || reduceTransparency ? null : captureBlurred(activityDecor, density);

        // Material의 창 안쪽 여백은 보존해 외곽 그림자가 잘리지 않게 한다.
        final Rect inset = new Rect();
        Drawable previous = window.getDecorView().getBackground();
        if (previous != null) previous.getPadding(inset);
        final FrostedBackdrop glass = new FrostedBackdrop(decor, source, snapshot,
                density, reduceTransparency || (!nativeBlur && snapshot == null));
        boolean noInset = inset.left == 0 && inset.top == 0 && inset.right == 0 && inset.bottom == 0;
        Drawable background = noInset ? glass : new InsetDrawable(glass,
                inset.left, inset.top, inset.right, inset.bottom);
        window.setBackgroundDrawable(background);
        window.setElevation(12 * density);
        decor.setElevation(12 * density);
        decor.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(inset.left, inset.top,
                        Math.max(inset.left, view.getWidth() - inset.right),
                        Math.max(inset.top, view.getHeight() - inset.bottom), CORNER_DP * density);
                outline.setAlpha(0.85f);
            }
        });
        decor.setClipToOutline(false);
        if (Build.VERSION.SDK_INT >= 28) {
            decor.setOutlineAmbientShadowColor(0x99000000);
            decor.setOutlineSpotShadowColor(0xBB000000);
        }
        APPLIED.put(window, glass);
        decor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) { }
            @Override public void onViewDetachedFromWindow(View view) {
                // 종료 콜백을 바꾸지 않고 캡처 참조만 놓는다. 강제 recycle은 RenderThread와 경합한다.
                glass.release();
                APPLIED.remove(window);
                view.removeOnAttachStateChangeListener(this);
            }
        });
    }

    private static void ensureReadableDefaultText(Activity activity, AlertDialog dialog) {
        Typeface font = ResourcesCompat.getFont(activity, R.font.ibm_plex_sans_kr);
        correctDefaultTextTree(dialog.getWindow().getDecorView(), font);
    }

    private static void correctDefaultTextTree(View view, Typeface font) {
        // customTitle에는 기본 title ID가 없다. 정적 텍스트만 순회하고 입력·선택·버튼은 제외한다.
        if (view instanceof TextView && !(view instanceof EditText)
                && !(view instanceof Button) && !(view instanceof CheckedTextView)) {
            correctDefaultText((TextView) view, font);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++)
                correctDefaultTextTree(group.getChildAt(index), font);
        }
    }

    private static void correctDefaultText(TextView text, Typeface font) {
        if (text.getVisibility() != View.VISIBLE) return;
        int style = text.getTypeface() == null ? Typeface.NORMAL : text.getTypeface().getStyle();
        text.setTypeface(font, style);
        if (!text.isEnabled()) return;
        int color = text.getCurrentTextColor();
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        // 밝은 테마에서 상속된 검정·짙은 회색만 교정한다. 경고/선택 색은 유지한다.
        if (hsv[1] > 0.12f || ColorUtils.calculateContrast(color, 0xFF39403E) >= 4.5) return;
        ColorStateList original = text.getTextColors();
        int disabled = original.getColorForState(new int[]{-android.R.attr.state_enabled},
                original.getDefaultColor());
        text.setTextColor(new ColorStateList(new int[][]{
                new int[]{-android.R.attr.state_enabled}, new int[0]},
                new int[]{disabled, 0xFFE1EBEC}));
    }

    private static boolean enableNativeBlur(Activity activity, Window window, View decor, float density) {
        if (Build.VERSION.SDK_INT < 31 || !decor.isHardwareAccelerated()) return false;
        WindowManager manager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
        try {
            if (manager == null || !manager.isCrossWindowBlurEnabled()) return false;
            window.setBackgroundBlurRadius(Math.round(BLUR_DP * density));
            return true;
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    private static boolean reduceTransparency(Activity activity) {
        // Android/OEM별 선택 키는 없는 기기에서 0이다. 사용자·개발자 blur 끄기를 우선한다.
        try {
            return Settings.Secure.getInt(activity.getContentResolver(), "accessibility_reduce_transparency", 0) == 1
                    || Settings.System.getInt(activity.getContentResolver(), "reduce_transparency", 0) == 1
                    || Settings.Global.getInt(activity.getContentResolver(), "disable_window_blurs", 0) == 1;
        } catch (SecurityException unavailable) {
            return true;
        }
    }

    private static Bitmap captureBlurred(View source, float density) {
        if (source.getWidth() <= 0 || source.getHeight() <= 0) return null;
        float scale = Math.min(1f, SNAPSHOT_MAX_EDGE / (float) Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, Math.round(source.getWidth() * scale));
        int height = Math.max(1, Math.round(source.getHeight() * scale));
        try {
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(0xFF181919);
            canvas.scale(scale, scale);
            source.draw(canvas);
            int[] pixels = new int[width * height];
            int[] scratch = new int[pixels.length];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            int radius = Math.max(1, Math.round(BLUR_DP * density * scale));
            // 작은 캡처에 두 번의 separable box blur: 읽을 수 있는 뒤 글자는 남기지 않는다.
            for (int pass = 0; pass < 2; pass++) {
                blur(pixels, scratch, width, height, radius, true);
                blur(scratch, pixels, width, height, radius, false);
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (RuntimeException | OutOfMemoryError error) {
            // 캡처 불가/메모리 압박에서는 같은 대비의 반사광·색 받침으로 유지한다.
            return null;
        }
    }

    private static void blur(int[] source, int[] target, int width, int height, int radius, boolean horizontal) {
        int lines = horizontal ? height : width;
        int count = horizontal ? width : height;
        int divisor = radius * 2 + 1;
        for (int line = 0; line < lines; line++) {
            int red = 0, green = 0, blue = 0;
            for (int offset = -radius; offset <= radius; offset++) {
                int position = Math.min(count - 1, Math.max(0, offset));
                int color = source[horizontal ? line * width + position : position * width + line];
                red += Color.red(color); green += Color.green(color); blue += Color.blue(color);
            }
            for (int position = 0; position < count; position++) {
                target[horizontal ? line * width + position : position * width + line] =
                        Color.rgb(red / divisor, green / divisor, blue / divisor);
                int remove = Math.max(0, position - radius);
                int add = Math.min(count - 1, position + radius + 1);
                int oldColor = source[horizontal ? line * width + remove : remove * width + line];
                int newColor = source[horizontal ? line * width + add : add * width + line];
                red += Color.red(newColor) - Color.red(oldColor);
                green += Color.green(newColor) - Color.green(oldColor);
                blue += Color.blue(newColor) - Color.blue(oldColor);
            }
        }
    }

    private static final class FrostedBackdrop extends Drawable {
        private final WeakReference<View> anchor;
        private final Rect sourceBounds;
        private final float radius, density;
        private final boolean opaque;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Path clip = new Path();
        private final ColorMatrixColorFilter saturation;
        private Bitmap bitmap;

        FrostedBackdrop(View anchor, Rect sourceBounds, Bitmap bitmap, float density, boolean opaque) {
            this.anchor = new WeakReference<>(anchor); this.sourceBounds = new Rect(sourceBounds);
            this.bitmap = bitmap; this.density = density; this.radius = CORNER_DP * density; this.opaque = opaque;
            ColorMatrix matrix = new ColorMatrix(); matrix.setSaturation(1.8f);
            saturation = new ColorMatrixColorFilter(matrix);
        }

        void release() { bitmap = null; }

        @Override public void draw(@NonNull Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            clip.reset(); clip.addRoundRect(bounds, radius, radius, Path.Direction.CW);
            int save = canvas.save(); canvas.clipPath(clip);
            paint.setStyle(Paint.Style.FILL); paint.setShader(null); paint.setColorFilter(null);
            View view = anchor.get();
            if (bitmap != null && view != null) {
                int[] location = new int[2]; view.getLocationOnScreen(location);
                float x = sourceBounds.left - location[0], y = sourceBounds.top - location[1];
                paint.setColorFilter(saturation);
                canvas.drawBitmap(bitmap, null, new RectF(x, y,
                        x + sourceBounds.width(), y + sourceBounds.height()), paint);
                paint.setColorFilter(null);
            } else if (opaque) {
                canvas.drawColor(0xFF1A211F);
            }
            paint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                    new int[]{opaque ? 0xFF1A211F : 0xE41A211F, opaque ? 0xFF1D2422 : 0xE61D2422,
                            opaque ? 0xFF171D1C : 0xEB171D1C}, new float[]{0, .5f, 1}, Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint);
            // 밝은 윗면과 약한 대각 반사만 추가한다. 본문 아래의 받침 밝기는 올리지 않는다.
            paint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                    new int[]{0x0AE4FFF1, 0x024E7270, 0x001D2422}, new float[]{0, .38f, 1}, Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.max(1, density));
            paint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom,
                    new int[]{0x75D5E9DF, 0x285F7771, 0x497A6874}, new float[]{0, .5f, 1}, Shader.TileMode.CLAMP));
            RectF border = new RectF(bounds); border.inset(density / 2f, density / 2f);
            canvas.drawRoundRect(border, radius, radius, paint);
            paint.setShader(null); canvas.restoreToCount(save);
        }

        @Override public void getOutline(@NonNull Outline outline) { outline.setRoundRect(getBounds(), radius); }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
