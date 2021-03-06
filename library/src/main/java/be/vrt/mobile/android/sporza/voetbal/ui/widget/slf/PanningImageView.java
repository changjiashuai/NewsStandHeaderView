/*
 * Copyright 2014 Flavio Faria
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.vrt.mobile.android.sporza.voetbal.ui.widget.slf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;

public class PanningImageView extends ImageView {

    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DESIRED_FPS = 60;
    private static final long FRAME_DELAY = MILLISECONDS_PER_SECOND / DESIRED_FPS;
    private final Matrix matrix = new Matrix();
    private final RectF viewPortRect = new RectF();
    private TransitionGenerator transitionGenerator = new RandomTransitionGenerator();
    private TransitionListener transitionListener;
    private Transition currentTransition;
    private RectF drawableRect;
    private long elapsedTime;
    private long lastFrameTime;
    private boolean initialized;

    public PanningImageView(final Context context) {
        this(context, null);
    }

    public PanningImageView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PanningImageView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        initialized = true;

        super.setScaleType(ScaleType.MATRIX);
    }

    //<editor-fold desc="Overrides From ImageView">
    @Override
    public void setScaleType(final ScaleType scaleType) {
        // Prevent another scaleType than CENTER_CROP from being set
    }

    @Override
    public void setImageBitmap(final Bitmap bm) {
        super.setImageBitmap(bm);
        handleImageChange();
    }

    @Override
    public void setImageResource(final int resId) {
        super.setImageResource(resId);
        handleImageChange();
    }

    @Override
    public void setImageURI(final Uri uri) {
        super.setImageURI(uri);
        handleImageChange();
    }

    @Override
    public void setImageDrawable(final Drawable drawable) {
        super.setImageDrawable(drawable);
        handleImageChange();
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        restart();
    }
    //</editor-fold>

    @Override
    protected void onDraw(final Canvas canvas) {
        final Drawable image = getDrawable();
        if (image != null) {
            if (drawableRect.isEmpty()) {
                updateDrawableBounds();
            } else {
                if (hasBounds()) {
                    if (currentTransition == null) {
                        startNewTransition();
                    }

                    doKenBernsEffect();
                }
            }
            lastFrameTime = System.currentTimeMillis();
            postInvalidateDelayed(FRAME_DELAY);
        }
        super.onDraw(canvas);
    }

    private void doKenBernsEffect() {
        if (currentTransition.getDestinyRect() != null) {

            elapsedTime += System.currentTimeMillis() - lastFrameTime;
            RectF currentRect = currentTransition.getInterpolatedRect(elapsedTime);

            float widthScale = drawableRect.width() / currentRect.width();
            float heightScale = drawableRect.height() / currentRect.height();

            float currRectToDrwScale = Math.min(widthScale, heightScale);

            float vpWidthScale = viewPortRect.width() / currentRect.width();
            float vpHeightScale = viewPortRect.height() / currentRect.height();
            float currRectToVpScale = Math.min(vpWidthScale, vpHeightScale);

            float totalScale = currRectToDrwScale * currRectToVpScale;

            float translX = totalScale * (drawableRect.centerX() - currentRect.left);
            float translY = totalScale * (drawableRect.centerY() - currentRect.top);

            matrix.reset();
            matrix.postTranslate(-drawableRect.width() / 2, -drawableRect.height() / 2);
            matrix.postScale(totalScale, totalScale);
            matrix.postTranslate(translX, translY);

            setImageMatrix(matrix);

            if (elapsedTime >= currentTransition.getDuration()) {
                fireTransitionEnd(currentTransition);
                startNewTransition();
            }
        } else {
            fireTransitionEnd(currentTransition);
        }
    }

    private void startNewTransition() {
        if (!hasBounds()) {
            return;
        }
        currentTransition = transitionGenerator.generateNextTransition(drawableRect, viewPortRect);
        resetValues();
        fireTransitionStart(currentTransition);
    }

    private void resetValues() {
        elapsedTime = 0;
        lastFrameTime = System.currentTimeMillis();
    }

    public void restart() {
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) {
            return;
        }

        updateViewport(width, height);
        updateDrawableBounds();

        startNewTransition();
    }

    private boolean hasBounds() {
        return !viewPortRect.isEmpty();
    }

    //<editor-fold desc="Setters">
    public void setTransitionGenerator(final TransitionGenerator transitionGenerator) {
        this.transitionGenerator = transitionGenerator;
        startNewTransition();
    }

    public void setTransitionListener(final TransitionListener transitionListener) {
        this.transitionListener = transitionListener;
    }
    //</editor-fold>

    //<editor-fold desc="Layout Changes">
    private void updateViewport(final float width, final float height) {
        viewPortRect.set(0, 0, width, height);
    }

    private void updateDrawableBounds() {
        if (drawableRect == null) {
            drawableRect = new RectF();
        }
        final Drawable d = getDrawable();
        if (d != null && d.getIntrinsicHeight() > 0 && d.getIntrinsicWidth() > 0) {
            drawableRect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        }
    }

    private void handleImageChange() {
        updateDrawableBounds();

        if (initialized) {
            startNewTransition();
        }
    }
    //</editor-fold>

    //<editor-fold desc="Transition Listener">
    private void fireTransitionStart(final Transition transition) {
        if (transitionListener != null && transition != null) {
            transitionListener.onTransitionStart(transition);
        }
    }

    private void fireTransitionEnd(final Transition transition) {
        if (transitionListener != null && transition != null) {
            transitionListener.onTransitionEnd(transition);
        }
    }

    public interface TransitionListener {

        void onTransitionStart(Transition transition);

        void onTransitionEnd(Transition transition);
    }
    //</editor-fold>
}