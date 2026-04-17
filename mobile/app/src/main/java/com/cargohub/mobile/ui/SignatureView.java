package com.cargohub.mobile.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom View that captures finger-drawn signatures on a canvas.
 */
public class SignatureView extends View {

    private final Paint paint = new Paint();
    private final Path path = new Path();
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private boolean hasSignature = false;

    public SignatureView(@NonNull Context context) {
        super(context);
        init();
    }

    public SignatureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignatureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);

        setBackgroundColor(Color.WHITE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmapCanvas = new Canvas(bitmap);
            bitmapCanvas.drawColor(Color.WHITE);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                hasSignature = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                if (bitmapCanvas != null) {
                    bitmapCanvas.drawPath(path, paint);
                }
                path.reset();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    /**
     * Returns the current signature as a Bitmap.
     */
    @Nullable
    public Bitmap getSignatureBitmap() {
        return bitmap;
    }

    /**
     * Returns true if the user has drawn anything.
     */
    public boolean hasSignature() {
        return hasSignature;
    }

    /**
     * Clears the signature canvas.
     */
    public void clear() {
        path.reset();
        hasSignature = false;
        if (bitmap != null) {
            bitmap.eraseColor(Color.WHITE);
        }
        invalidate();
    }
}
