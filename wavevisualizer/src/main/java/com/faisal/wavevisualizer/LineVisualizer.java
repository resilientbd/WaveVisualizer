package com.faisal.wavevisualizer;



import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.audiofx.Visualizer;
import android.media.audiofx.Visualizer.OnDataCaptureListener;
import android.util.AttributeSet;
import android.view.View;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class LineVisualizer extends View {
    private final float smoothingFactor;
    private final int barsCount;
    private final int barsColor;
    private final int backgroundColor;
    private final Paint piePaint;
    private float[] magnitudes;
    private final List data;
    private Visualizer visualizer;

    private final float maxMagnitude;
    private static final int FFT_STEP = 2;
    private static final int FFT_OFFSET = 2;
    private static final int FFT_NEEDED_PORTION = 3;

    public final void visualizeData() {
        this.data.clear();
        float barWidth = (float)this.getWidth() / ((float)this.barsCount * 2.0F);
        int i = 0;

        for(int var3 = this.barsCount; i < var3; ++i) {
            float segmentSize = (float)this.magnitudes.length / (float)this.barsCount;
            float segmentStart = (float)i * segmentSize;
            float segmentEnd = segmentStart + segmentSize;
            float sum = 0.0F;
            int j = (int)segmentStart;

            for(int var9 = (int)segmentEnd; j < var9; ++j) {
                sum += this.magnitudes[j];
            }

            boolean var11 = false;
            float horizontalOffset = sum / segmentSize;
            var11 = false;
            horizontalOffset *= (float)this.getHeight();
            var11 = false;
            float amp = Math.max(horizontalOffset, barWidth);
            horizontalOffset = barWidth / (float)2;
            float startX = barWidth * (float)i * (float)2;
            float endX = startX + barWidth;
            int midY = this.getHeight() / 2;
            float startY = (float)midY - amp / (float)2;
            float endY = (float)midY + amp / (float)2;
            this.data.add(new RectF(startX + horizontalOffset, startY, endX + horizontalOffset, endY));
        }

        this.invalidate();
    }

    private final float[] convertFFTtoMagnitudes(byte[] fft) {
        if (fft.length == 0) {
            return new float[0];
        } else {
            int n = fft.length / 3;
            float[] curMagnitudes = new float[n / 2];
            float[] prevMagnitudes = this.magnitudes;
            if (prevMagnitudes.length == 0) {
                prevMagnitudes = new float[n];
            }

            int k = 0;

            for(int var6 = n / 2 - 1; k < var6; ++k) {
                int index = k * 2 + 2;
                byte real = fft[index];
                byte imaginary = fft[index + 1];
                float curMagnitude = this.calculateMagnitude((float)real, (float)imaginary);
                curMagnitudes[k] = curMagnitude + (prevMagnitudes[k] - curMagnitude) * this.smoothingFactor;
            }

            boolean map = false;
            float[] $this$mapTo$iv$iv = curMagnitudes;
            Collection destination$iv$iv = (Collection)(new ArrayList(curMagnitudes.length));
            boolean mapTo = false;
            int var21 = 0;

            for(int var11 = curMagnitudes.length; var21 < var11; ++var21) {
                float item$iv$iv = $this$mapTo$iv$iv[var21];
                boolean var14 = false;
                Float var16 = item$iv$iv / this.maxMagnitude;
                destination$iv$iv.add(var16);
            }

            return CollectionsKt.toFloatArray((Collection)((List)destination$iv$iv));
        }
    }

    private final float calculateMagnitude(float r, float i) {
        float var10000;
        if (i == 0.0F && r == 0.0F) {
            var10000 = 0.0F;
        } else {
            var10000 = (float)10;
            float var3 = r * r + i * i;
            var10000 *= (float)Math.log10((double)var3);
        }

        return var10000;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.visualizeData();
    }

    protected void onDraw(@NotNull Canvas canvas) {
        Intrinsics.checkNotNullParameter(canvas, "canvas");
        super.onDraw(canvas);

        Iterable iterable = data;

        Iterator iterator = iterable.iterator();

        while(iterator.hasNext()) {
            Object element = iterator.next();
            RectF it = (RectF)element;

            canvas.drawRoundRect(it, 25.0F, 25.0F, this.piePaint);
        }

    }

    protected void onDetachedFromWindow() {
        Visualizer var10000 = this.visualizer;
        if (var10000 != null) {
            var10000.release();
        }

        super.onDetachedFromWindow();
    }

    public LineVisualizer(@NotNull Context context, @NotNull AttributeSet attrs) {
        super(context,attrs);
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(attrs, "attrs");

        this.smoothingFactor = 0.2F;
        this.barsCount = 24;
        this.barsColor = Color.argb(200, 181, 111, 233);
        this.backgroundColor = Color.parseColor("#EEEEEE");
        Paint var3 = new Paint(1);
        boolean var5 = false;
        var3.setAntiAlias(true);
        var3.setColor(this.barsColor);
        Unit var7 = Unit.INSTANCE;
        this.piePaint = var3;
        this.setBackgroundColor(this.backgroundColor);
        this.magnitudes = new float[0];
        this.data = (List)(new ArrayList());

        this.maxMagnitude = this.calculateMagnitude(128.0F, 128.0F);
    }





}

