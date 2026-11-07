package com.glitchos.mobile;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Outline;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MobileVisuals {
    /** Small static gradients suggest depth without sampling or blurring the screen. */
    static final class GlassSurfaceDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF shape = new RectF();
        private final RectF inner = new RectF();
        private final float radius;
        private final boolean focused;
        private Shader body;
        private Shader edge;
        private Shader glint;

        GlassSurfaceDrawable(float radius, boolean focused) {
            this.radius = radius;
            this.focused = focused;
        }

        @Override protected void onBoundsChange(android.graphics.Rect bounds) {
            super.onBoundsChange(bounds);
            shape.set(bounds.left + 1f, bounds.top + 1f, bounds.right - 1f, bounds.bottom - 1f);
            inner.set(shape);
            inner.inset(1.6f, 1.6f);
            body = new LinearGradient(shape.left, shape.top, shape.right * .65f, shape.bottom,
                    focused ? new int[]{0xE6ECFAFF, 0xC4D0E7F5, 0xB2ADC9E7}
                            : new int[]{0x49C0EBF5, 0x2698AECF, 0x3A526D94},
                    new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP);
            edge = new LinearGradient(shape.left, shape.top, shape.right, shape.bottom,
                    focused ? new int[]{0xEFFFFFFF, 0x55DAF5FF, 0xB1D3E4FF}
                            : new int[]{0xA0E8FCFF, 0x16E8F9FF, 0x62C7D6FF},
                    new float[]{0f, .56f, 1f}, Shader.TileMode.CLAMP);
            glint = new LinearGradient(shape.left, 0, shape.right, 0,
                    new int[]{0x00FFFFFF, focused ? 0xDFFFFFFF : 0x9CFFFFFF, 0x00FFFFFF},
                    new float[]{0f, .24f, 1f}, Shader.TileMode.CLAMP);
        }

        @Override public void draw(Canvas canvas) {
            if (shape.isEmpty()) return;
            // The inner stroke changes Paint alpha; reset it for every redraw.
            paint.setAlpha(255);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(body);
            canvas.drawRoundRect(shape, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(focused ? 1.7f : 1.2f);
            paint.setShader(edge);
            canvas.drawRoundRect(shape, radius, radius, paint);
            paint.setShader(null);
            paint.setStrokeWidth(1f);
            paint.setColor(focused ? 0x40FFFFFF : 0x15FFFFFF);
            canvas.drawRoundRect(inner, Math.max(0f, radius - 1.6f), Math.max(0f, radius - 1.6f), paint);
            paint.setAlpha(255);
            paint.setShader(glint);
            canvas.drawLine(shape.left + radius, shape.top + 1.2f,
                    shape.right - radius, shape.top + 1.2f, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override public void getOutline(Outline outline) {
            outline.setRoundRect(getBounds(), radius);
            outline.setAlpha(.45f);
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    /** Transparent gloss preserves the app's original colors and recognizable artwork. */
    private static final class TileGlassDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF shape = new RectF();
        private final Path reflection = new Path();
        private final float radius;
        private final boolean focused;
        private Shader sheen;
        private Shader edge;

        TileGlassDrawable(float radius, boolean focused) {
            this.radius = radius;
            this.focused = focused;
        }

        @Override protected void onBoundsChange(android.graphics.Rect bounds) {
            super.onBoundsChange(bounds);
            shape.set(bounds.left + 1.1f, bounds.top + 1.1f, bounds.right - 1.1f, bounds.bottom - 1.1f);
            sheen = new LinearGradient(0, shape.top, shape.width() * .18f, shape.bottom,
                    new int[]{focused ? 0x47FFFFFF : 0x21FFFFFF, 0x00FFFFFF, 0x05061E39},
                    new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP);
            edge = new LinearGradient(shape.left, shape.top, shape.right, shape.bottom,
                    focused ? new int[]{0xFFFFFFFF, 0xA4E7F5FF, 0xE7E5DCFF}
                            : new int[]{0x86FFFFFF, 0x11E8F6FF, 0x44C8D6F5},
                    new float[]{0f, .58f, 1f}, Shader.TileMode.CLAMP);
            reflection.reset();
            reflection.moveTo(shape.left, shape.top);
            reflection.lineTo(shape.right, shape.top);
            reflection.cubicTo(shape.right * .87f, shape.bottom * .18f,
                    shape.width() * .45f, shape.bottom * .15f, shape.left, shape.bottom * .47f);
            reflection.close();
        }

        @Override public void draw(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(sheen);
            canvas.drawRoundRect(shape, radius, radius, paint);
            paint.setShader(null);
            paint.setColor(focused ? 0x14FFFFFF : 0x08FFFFFF);
            canvas.drawPath(reflection, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(focused ? 2.5f : 1f);
            paint.setShader(edge);
            canvas.drawRoundRect(shape, radius, radius, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    static final class LandscapeDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path ribbon = new Path();
        @Override public void draw(Canvas canvas) {
            float w = getBounds().width();
            float h = getBounds().height();
            if (w <= 0 || h <= 0) return;
            paint.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{0xFF214C5C, 0xFF243A60, 0xFF15213C}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(getBounds(), paint);
            paint.setShader(new RadialGradient(w * .07f, h * .12f, w * .60f,
                    new int[]{0xC04DBCA9, 0x3D3A9C9F, 0x003A9C9F},
                    new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(getBounds(), paint);
            paint.setShader(new RadialGradient(w * .89f, h * .13f, w * .53f,
                    new int[]{0xBB9A8EC9, 0x355A66B6, 0x005A66B6},
                    new float[]{0f, .55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(getBounds(), paint);
            ribbon.reset();
            ribbon.moveTo(-w * .08f, h * .75f);
            ribbon.cubicTo(w * .18f, -h * .05f, w * .64f, h * .83f, w * 1.08f, h * .12f);
            ribbon.lineTo(w * 1.08f, h * .45f);
            ribbon.cubicTo(w * .57f, h * .98f, w * .27f, h * .24f, -w * .08f, h * .96f);
            ribbon.close();
            paint.setShader(new LinearGradient(0, h * .20f, w, h * .83f,
                    new int[]{0x357DE4CC, 0x1E83B3DC, 0x1ABCB4F3}, null, Shader.TileMode.CLAMP));
            canvas.drawPath(ribbon, paint);
            paint.setShader(new RadialGradient(w * .28f, h * .92f, w * .43f,
                    new int[]{0x663087A0, 0x003087A0}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(getBounds(), paint);
            paint.setShader(new LinearGradient(0, h * .20f, 0, h,
                    new int[]{0x00081529, 0xA60B152C}, null, Shader.TileMode.CLAMP));
            canvas.drawRect(getBounds(), paint);
            paint.setShader(null);
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }
}

