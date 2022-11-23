package com.faisal.wavevisualizer;

import static java.lang.Double.max;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.Rect;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.faisal.wavevisualizer.utils.AudioUtils;
import com.faisal.wavevisualizer.utils.SamplingUtils;
import com.faisal.wavevisualizer.utils.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import kotlin.collections.CollectionsKt;

public class WaveLineBarView extends View {
    private int barsColor;
    private float smoothingFactor = 0.2f;
    public static final int MODE_RECORDING = 1;
    public static final int MODE_PLAYBACK = 2;
    private float[] magnitudes;
    private static final int HISTORY_SIZE = 6;
    private int barsCount = 24;
    private TextPaint mTextPaint;
    private Paint mStrokePaint, mFillPaint, mMarkerPaint;
    private List data;
    // Used in draw
    private int brightness;
    private Rect drawRect;

    private int width, height;
    private float xStep, centerY;
    private int mMode, mAudioLength, mMarkerPosition, mSampleRate, mChannels;
    private short[] mSamples;
    private LinkedList<float[]> mHistoricalData;
    private Picture mCachedWaveform;
    private Bitmap mCachedWaveformBitmap;
    private int colorDelta = 255 / (HISTORY_SIZE + 1);
    private boolean showTextAxis = true;
    private int density = 50;
    private float gap = 4;
    private Paint middleLine;
    private float maxMagnitude;
    private float valueOffset = -30;

    public WaveLineBarView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WaveLineBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public WaveLineBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.WaveformView, defStyle, 0);

        mMode = a.getInt(R.styleable.WaveformView_mode, MODE_PLAYBACK);

        float strokeThickness = a.getFloat(R.styleable.WaveformView_waveformStrokeThickness, 1f);
        int mStrokeColor = a.getColor(R.styleable.WaveformView_waveformColor,
                ContextCompat.getColor(context, R.color.default_waveform));
        int mFillColor = a.getColor(R.styleable.WaveformView_waveformFillColor,
                ContextCompat.getColor(context, R.color.default_waveformFill));
        int mMarkerColor = a.getColor(R.styleable.WaveformView_playbackIndicatorColor,
                ContextCompat.getColor(context, R.color.default_playback_indicator));
        int mTextColor = a.getColor(R.styleable.WaveformView_timecodeColor,
                ContextCompat.getColor(context, R.color.default_timecode));

        a.recycle();

        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(TextUtils.getFontSize(getContext(),
                android.R.attr.textAppearanceSmall));

        mStrokePaint = new Paint();
        mStrokePaint.setColor(mStrokeColor);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(strokeThickness);
        mStrokePaint.setAntiAlias(true);

        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setAntiAlias(true);
        mFillPaint.setColor(mFillColor);

        mMarkerPaint = new Paint();
        mMarkerPaint.setStyle(Paint.Style.STROKE);
        mMarkerPaint.setStrokeWidth(0);
        mMarkerPaint.setAntiAlias(true);
        mMarkerPaint.setColor(mMarkerColor);

        middleLine = new Paint();
        middleLine.setColor(Color.BLUE);

        this.magnitudes = new float[0];
        this.data = (List)(new ArrayList());

        this.maxMagnitude = this.calculateMagnitude(128.0F, 128.0F);
        this.smoothingFactor = 0.2F;
        this.barsCount = 24;
        this.barsColor = Color.argb(200, 181, 111, 233);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.visualizeData();
    }
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


            float horizontalOffset = sum / segmentSize;

            horizontalOffset *= (float)this.getHeight();

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

    private final float calculateMagnitude(float r, float i) {
        float max;
        if (i == 0.0F && r == 0.0F) {
            max = 0.0F;
        } else {
            max = (float)10;
            float magnitude = r * r + i * i;
            Log.d("chkreal",""+magnitude);
            max = magnitude;
          //  max *= (float)Math.log10((double)magnitude);
        }

        return max;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Iterable iterable = data;

        Iterator iterator = iterable.iterator();

        while(iterator.hasNext()) {
            Object element = iterator.next();
            RectF it = (RectF)element;

            canvas.drawRoundRect(it, 25.0F, 25.0F, this.mStrokePaint);
        }
    }
    private final float[] convertFFTtoMagnitudes(short[] fft) {
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
                byte real = (byte) fft[index];
                byte imaginary = (byte) fft[index + 1];
                float curMagnitude = calculateMagnitude(real, imaginary);
                float magn = curMagnitude + (prevMagnitudes[k] - curMagnitude) * this.smoothingFactor;
                float per = (magn*valueOffset)/100;
                magn = magn-per;
                curMagnitudes[k] = magn;
                Log.d("chkmag","mg1:"+curMagnitude);
                Log.d("chkmag","mg2:"+magn);
                Log.d("chkmag","offset:"+valueOffset);
            }


            float[] $this$mapTo$iv$iv = curMagnitudes;
            Collection destination$iv$iv = (Collection)(new ArrayList(curMagnitudes.length));

            int var21 = 0;

            for(int var11 = curMagnitudes.length; var21 < var11; ++var21) {
                float item$iv$iv = $this$mapTo$iv$iv[var21];

                Float var16 = item$iv$iv / this.maxMagnitude;
                destination$iv$iv.add(var16);
            }

            return CollectionsKt.toFloatArray((Collection)((List)destination$iv$iv));
        }
    }




    public void setSamples(short[] samples) {
        mSamples = samples;
       /* calculateAudioLength();
        onSamplesChanged();*/
        magnitudes = convertFFTtoMagnitudes(samples);
        visualizeData();
    }

    public void calibrateValueDecrement() {
        valueOffset = valueOffset-10;
    }

    public void calibrateValueIncrement() {
        valueOffset = valueOffset+10;
        Log.d("chkoffset","offset:"+valueOffset);
    }
}
