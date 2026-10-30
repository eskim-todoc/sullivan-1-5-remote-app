package todoc.cochlear.remoteapp.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.Nullable;

import java.util.Random;

/**
 * 연결 안내의 불투명 받침. 후면 화면을 매 프레임 캡처하지 않고 반사광과 물결만 그린다.
 */
public final class GlassConnectionOverlay extends View
{
    private static final long FRAME_DELAY_MS = 33;
    private static final int WAVE_SEGMENTS = 32;

    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint protection = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crest = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] glow = new Paint[3];
    private final Paint[] ribbon = new Paint[2];
    private final Path crestPath = new Path();
    private final Random random = new Random();
    private final Wave[] waves = new Wave[2];

    private boolean attached;
    private boolean requested;
    private boolean framePosted;
    private boolean compact;
    private long lastTick;
    private float seconds;
    private float extent;
    private float ribbonHalfWidth;

    private final Runnable frame = new Runnable()
    {
        @Override
        public void run()
        {
            framePosted = false;
            if (!canAnimate())
            {
                return;
            }
            long now = SystemClock.uptimeMillis();
            seconds += Math.min(100, now - lastTick) / 1000f;
            lastTick = now;
            invalidate();
            scheduleFrame();
        }
    };

    public GlassConnectionOverlay(Context context)
    {
        this(context, null);
    }

    public GlassConnectionOverlay(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public GlassConnectionOverlay(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);

        for (int index = 0; index < glow.length; index++)
        {
            glow[index] = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        for (int index = 0; index < ribbon.length; index++)
        {
            ribbon[index] = new Paint(Paint.ANTI_ALIAS_FLAG);
            ribbon[index].setStyle(Paint.Style.STROKE);
            ribbon[index].setStrokeCap(Paint.Cap.ROUND);
            ribbon[index].setStrokeJoin(Paint.Join.ROUND);
            waves[index] = new Wave(index == 0 ? -24 : 39);
        }
        configureGlows();
        crest.setStyle(Paint.Style.STROKE);
        crest.setStrokeWidth(getResources().getDisplayMetrics().density);
        crest.setColor(0x0ED9D9D9);
        edge.setStrokeWidth(getResources().getDisplayMetrics().density);
    }

    /**
     * 제품 사진 위에서는 사진을 남기는 원형 반사광을 쓰고, 본문용 불투명 받침과 분리한다.
     */
    public void setCompactMode(boolean compact)
    {
        this.compact = compact;
        if (compact)
        {
            setOutlineProvider(new ViewOutlineProvider()
            {
                @Override
                public void getOutline(View view, Outline outline)
                {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
        }
        setClipToOutline(compact);
        configureGlows();
        if (getWidth() > 0 && getHeight() > 0)
        {
            configureSurface(getWidth(), getHeight());
        }
        invalidate();
    }

    private void configureGlows()
    {
        int[] colors = compact ? new int[]{0x40999999, 0x50A7A7A7, 0x28AFAFAF}
            : new int[]{0x266B6B6B, 0x2D828282, 0x19696969};
        for (int index = 0; index < glow.length; index++)
        {
            glow[index].setShader(new RadialGradient(0, 0, 1, colors[index],
                colors[index] & 0x00FFFFFF, Shader.TileMode.CLAMP));
        }
    }

    public void startWaves()
    {
        requested = true;
        updateAnimation();
    }

    public void stopWaves()
    {
        requested = false;
        cancelFrame();
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        attached = true;
        updateAnimation();
    }

    @Override
    protected void onDetachedFromWindow()
    {
        attached = false;
        cancelFrame();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility)
    {
        super.onVisibilityChanged(changedView, visibility);
        updateAnimation();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility)
    {
        super.onWindowVisibilityChanged(visibility);
        updateAnimation();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus)
    {
        super.onWindowFocusChanged(hasWindowFocus);
        updateAnimation();
    }

    private boolean motionAllowed()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            return ValueAnimator.areAnimatorsEnabled();
        }
        return Settings.Global.getFloat(getContext().getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0;
    }

    private boolean canAnimate()
    {
        return attached && requested && isShown() && getWindowVisibility() == VISIBLE
            && hasWindowFocus() && motionAllowed();
    }

    private void updateAnimation()
    {
        // View 생성자의 가시성 콜백은 필드 초기화 전에도 호출될 수 있다.
        if (frame == null)
        {
            return;
        }
        if (canAnimate())
        {
            if (!framePosted)
            {
                lastTick = SystemClock.uptimeMillis();
                scheduleFrame();
            }
        }
        else
        {
            cancelFrame();
        }
        invalidate();
    }

    private void scheduleFrame()
    {
        if (!framePosted)
        {
            framePosted = true;
            postDelayed(frame, FRAME_DELAY_MS);
        }
    }

    private void cancelFrame()
    {
        removeCallbacks(frame);
        framePosted = false;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width == 0 || height == 0)
        {
            return;
        }
        configureSurface(width, height);
    }

    private void configureSurface(int width, int height)
    {
        base.setShader(new LinearGradient(0, 0, width, height,
            compact ? new int[]{0x59242424, 0x45282828, 0x59242424}
                : new int[]{0x70242424, 0x60282828, 0x70242424}, null, Shader.TileMode.CLAMP));
        // 안내 글씨가 놓이는 넓은 중앙 띠는 움직이는 반사광과 별도로 대비를 고정한다.
        protection.setShader(new LinearGradient(0, 0, 0, height,
            new int[]{0x001D1D1D, 0x001D1D1D, 0x001D1D1D, 0x001D1D1D, 0x001D1D1D, 0x001D1D1D},
            new float[]{0, 0.15f, 0.25f, 0.58f, 0.72f, 1}, Shader.TileMode.CLAMP));
        extent = (float) Math.hypot(width, height);
        ribbonHalfWidth = width * (compact ? 0.11f : 0.20f);
        for (int index = 0; index < ribbon.length; index++)
        {
            int color = index == 0 ? 0xFFBBBBBB : 0xFFBBBBBB;
            ribbon[index].setShader(new LinearGradient(-extent, 0, extent, 0,
                new int[]{color & 0x00FFFFFF, color, color & 0x00FFFFFF},
                new float[]{0, 0.5f, 1}, Shader.TileMode.CLAMP));
        }
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        canvas.drawRect(0, 0, width, height, base);
        drawGlow(canvas, 0, width * (0.18f + 0.07f * (float) Math.sin(seconds * 0.20f)),
            height * (0.20f + 0.06f * (float) Math.cos(seconds * 0.144f)), width * 0.88f, height * 0.53f);
        drawGlow(canvas, 1, width * (0.90f + 0.06f * (float) Math.cos(seconds * 0.162f)),
            height * (0.55f + 0.08f * (float) Math.sin(seconds * 0.126f)), width * 0.90f, height * 0.57f);
        drawGlow(canvas, 2, width * (0.42f + 0.09f * (float) Math.sin(seconds * 0.135f)),
            height * 0.94f, width * 0.88f, height * 0.43f);
        for (int index = 0; index < waves.length; index++)
        {
            drawWave(canvas, waves[index], ribbon[index], width, height);
        }
        if (!compact)
        {
            canvas.drawRect(0, 0, width, height, protection);
        }
        edge.setColor(0x30E1E1E1);
        canvas.drawLine(0, 0, width, 0, edge);
        edge.setColor(0x18949494);
        canvas.drawLine(0, height, width, height, edge);
    }

    private void drawGlow(Canvas canvas, int index, float x, float y, float radiusX, float radiusY)
    {
        int saved = canvas.save();
        canvas.translate(x, y);
        canvas.scale(radiusX, radiusY);
        canvas.drawCircle(0, 0, 1, glow[index]);
        canvas.restoreToCount(saved);
    }

    private void drawWave(Canvas canvas, Wave wave, Paint paint, float width, float height)
    {
        wave.updateDirection(seconds);
        int saved = canvas.save();
        canvas.translate(width * 0.5f, height * wave.offset);
        canvas.rotate(wave.angle);
        crestPath.rewind();
        for (int index = 0; index <= WAVE_SEGMENTS; index++)
        {
            float x = -extent + extent * 2 * index / WAVE_SEGMENTS;
            float y = waveY(wave, x);
            if (index == 0)
            {
                crestPath.moveTo(x, y);
            }
            else
            {
                crestPath.lineTo(x, y);
            }
        }
        // 면의 고정 그라데이션 대신 파형을 따라 이동하는 세 겹의 낮은 알파로 부드러운 물결을 만든다.
        paint.setStrokeWidth(ribbonHalfWidth * 2);
        paint.setAlpha(compact ? 12 : 4);
        canvas.drawPath(crestPath, paint);
        paint.setStrokeWidth(ribbonHalfWidth * 1.2f);
        paint.setAlpha(compact ? 26 : 7);
        canvas.drawPath(crestPath, paint);
        paint.setStrokeWidth(ribbonHalfWidth * 0.5f);
        paint.setAlpha(compact ? 40 : 7);
        canvas.drawPath(crestPath, paint);
        crest.setColor(compact ? 0x80C5C5C5 : 0x0ED9D9D9);
        crest.setStrokeWidth(getResources().getDisplayMetrics().density * (compact ? 1.2f : 1));
        canvas.drawPath(crestPath, crest);
        canvas.restoreToCount(saved);
    }

    private float waveY(Wave wave, float x)
    {
        float unit = x / Math.max(1, extent);
        float flow = seconds * (compact ? 4.5f : 1.6f);
        return getWidth() * (compact ? 0.17f : 0.12f)
            * ((float) Math.sin(unit * 4.2f + flow * wave.speed + wave.phase)
            + 0.35f * (float) Math.sin(unit * 7.8f - flow * 0.26f + wave.phase));
    }

    private final class Wave
    {
        final float phase = random.nextFloat() * 6.28f;
        final float speed = 0.28f + random.nextFloat() * 0.18f;
        float offset = 0.30f + random.nextFloat() * 0.45f;
        float angle;
        float fromAngle;
        float targetAngle;
        float changedAt;
        float duration;

        Wave(float angle)
        {
            this.angle = angle;
            chooseDirection(0);
        }

        void chooseDirection(float time)
        {
            fromAngle = angle;
            targetAngle = angle + random.nextFloat() * 120 - 60;
            changedAt = time;
            duration = 6 + random.nextFloat() * 4;
        }

        void updateDirection(float time)
        {
            float progress = Math.min(1, Math.max(0, (time - changedAt) / duration));
            float eased = progress * progress * (3 - 2 * progress);
            angle = fromAngle + (targetAngle - fromAngle) * eased;
            if (progress >= 1)
            {
                chooseDirection(time);
            }
        }
    }
}
