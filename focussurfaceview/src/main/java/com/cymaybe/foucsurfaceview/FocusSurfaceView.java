package com.cymaybe.foucsurfaceview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.cymaybe.foucsurfaceview.animation.SimpleValueAnimator;
import com.cymaybe.foucsurfaceview.animation.SimpleValueAnimatorListener;
import com.cymaybe.foucsurfaceview.animation.ValueAnimatorV14;
import com.cymaybe.foucsurfaceview.animation.ValueAnimatorV8;

/**
 * Created by moubiao on 2016/11/2.
 * 裁剪图片的view
 */

public class FocusSurfaceView extends SurfaceView {
    private final String TAG = "moubiao";

    private static final int HANDLE_SIZE_IN_DP = 14;
    private static final int MIN_FRAME_SIZE_IN_DP = 50;
    private static final int FRAME_STROKE_WEIGHT_IN_DP = 1;
    private static final int GUIDE_STROKE_WEIGHT_IN_DP = 1;
    private static final float DEFAULT_INITIAL_FRAME_SCALE = 1f;
    private static final int DEFAULT_ANIMATION_DURATION_MILLIS = 100;

    private static final int TRANSPARENT = 0x00000000;
    private static final int TRANSLUCENT_WHITE = 0xBBFFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int TRANSLUCENT_BLACK = 0xBB000000;

    private boolean mIsInitialized = false;
    private float mBoundaryWidth = 0;//裁剪框可移动的范围的宽
    private float mBoundaryHeight = 0;//裁剪框可移动的范围的高
    private RectF mBoundaryRect;//裁剪框的大小和可移动范围
    private Paint mPaintFrame;
    private int mCropWidth;
    private int mCropHeight;
    private RectF mFrameRect;//裁剪框的rect
    private Paint mPaintTranslucent;

    private float mLastX, mLastY;
    private boolean mIsRotating = false;
    private boolean mIsAnimating = false;
    private SimpleValueAnimator mAnimator = null;
    private final Interpolator DEFAULT_INTERPOLATOR = new DecelerateInterpolator();
    private Interpolator mInterpolator = DEFAULT_INTERPOLATOR;
    // Instance variables for customizable attributes //////////////////////////////////////////////

    private TouchArea mTouchArea = TouchArea.OUT_OF_BOUNDS;

    private CropMode mCropMode = CropMode.SQUARE;
    private ShowMode mGuideShowMode = ShowMode.SHOW_ALWAYS;
    private ShowMode mHandleShowMode = ShowMode.SHOW_ALWAYS;
    private float mMinFrameSize;
    private int mHandleSize;
    private int mTouchPadding = 0;
    private boolean mShowGuide = true;
    private boolean mShowHandle = true;
    private boolean mIsCropEnabled = true;
    private boolean mIsEnabled = true;
    private boolean mIsChangeEnabled = false;
    private PointF mCustomRatio;
    private float mFrameStrokeWeight = 2.0f;
    private float mGuideStrokeWeight = 2.0f;
    private int mOverlayColor;
    private int mFrameColor;
    private int mHandleColor;
    private int mGuideColor;
    private Drawable mFrameBackground;
    private float mInitialFrameScale; // 0.01 ~ 1.0, 0.75 is default value
    private boolean mIsAnimationEnabled = true;
    private int mAnimationDurationMillis = DEFAULT_ANIMATION_DURATION_MILLIS;
    private boolean mIsHandleShadowEnabled = true;

    public FocusSurfaceView(Context context) {
        this(context, null);
    }

    public FocusSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocusSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        float density = getDensity();
        mHandleSize = (int) (density * HANDLE_SIZE_IN_DP);
        mMinFrameSize = density * MIN_FRAME_SIZE_IN_DP;
        mFrameStrokeWeight = density * FRAME_STROKE_WEIGHT_IN_DP;
        mGuideStrokeWeight = density * GUIDE_STROKE_WEIGHT_IN_DP;

        mPaintFrame = new Paint();
        mPaintTranslucent = new Paint();

        mFrameColor = WHITE;
        mOverlayColor = TRANSLUCENT_BLACK;
        mHandleColor = WHITE;
        mGuideColor = TRANSLUCENT_WHITE;

        // handle Styleable
        handleStyleable(context, attrs, defStyle, density);
    }

    /**
     * 处理各种属性
     */
    private void handleStyleable(Context context, AttributeSet attrs, int defStyle, float mDensity) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FocusSurfaceView, defStyle, 0);
        mCropMode = CropMode.SQUARE;
        try {
            for (CropMode mode : CropMode.values()) {
                if (ta.getInt(R.styleable.FocusSurfaceView_focus_mode, 3) == mode.getId()) {
                    mCropMode = mode;
                    break;
                }
            }
            float customRatioX = ta.getFloat(R.styleable.FocusSurfaceView_focus_frame_ratio_x, 1.0f);
            float customRatioY = ta.getFloat(R.styleable.FocusSurfaceView_focus_frame_ratio_y, 1.0f);
            mCustomRatio = new PointF(customRatioX, customRatioY);
            mOverlayColor = ta.getColor(R.styleable.FocusSurfaceView_focus_overlay_color, TRANSLUCENT_BLACK);
            mFrameColor = ta.getColor(R.styleable.FocusSurfaceView_focus_frame_color, WHITE);
            mHandleColor = ta.getColor(R.styleable.FocusSurfaceView_focus_handle_color, WHITE);
            mGuideColor = ta.getColor(R.styleable.FocusSurfaceView_focus_guide_color, TRANSLUCENT_WHITE);
            for (ShowMode mode : ShowMode.values()) {
                if (ta.getInt(R.styleable.FocusSurfaceView_focus_guide_show_mode, 1) == mode.getId()) {
                    mGuideShowMode = mode;
                    break;
                }
            }

            for (ShowMode mode : ShowMode.values()) {
                if (ta.getInt(R.styleable.FocusSurfaceView_focus_handle_show_mode, 1) == mode.getId()) {
                    mHandleShowMode = mode;
                    break;
                }
            }
            setGuideShowMode(mGuideShowMode);
            setHandleShowMode(mHandleShowMode);
            mHandleSize = ta.getDimensionPixelSize(R.styleable.FocusSurfaceView_focus_handle_size, (int) (HANDLE_SIZE_IN_DP * mDensity));
            mTouchPadding = ta.getDimensionPixelSize(R.styleable.FocusSurfaceView_focus_touch_padding, 0);

            mCropWidth = ta.getDimensionPixelSize(R.styleable.FocusSurfaceView_focus_crop_width, dip2px(getContext(), 200f));
            mCropHeight = ta.getDimensionPixelSize(R.styleable.FocusSurfaceView_focus_crop_height, dip2px(getContext(), 200f));

            mMinFrameSize = ta.getDimensionPixelSize(R.styleable.FocusSurfaceView_focus_min_frame_size, (int) (MIN_FRAME_SIZE_IN_DP * mDensity));
            mFrameStrokeWeight = ta.getDimensionPixelSize(R.styleable.FocusSurfaceView_focus_frame_stroke_weight, (int) (FRAME_STROKE_WEIGHT_IN_DP * mDensity));
            mGuideStrokeWeight = ta.getDimensionPixelSize(R.styleable.FocusSurfaceView_focus_guide_stroke_weight, (int) (GUIDE_STROKE_WEIGHT_IN_DP * mDensity));
            mIsCropEnabled = ta.getBoolean(R.styleable.FocusSurfaceView_focus_crop_enabled, true);
            mInitialFrameScale = constrain(ta.getFloat(R.styleable.FocusSurfaceView_focus_initial_frame_scale, DEFAULT_INITIAL_FRAME_SCALE),
                    0.01f, 1.0f, DEFAULT_INITIAL_FRAME_SCALE);
            mIsAnimationEnabled = ta.getBoolean(R.styleable.FocusSurfaceView_focus_animation_enabled, true);
            mAnimationDurationMillis = ta.getInt(R.styleable.FocusSurfaceView_focus_animation_duration, DEFAULT_ANIMATION_DURATION_MILLIS);
            mIsHandleShadowEnabled = ta.getBoolean(R.styleable.FocusSurfaceView_focus_handle_shadow_enabled, true);
            mIsChangeEnabled = ta.getBoolean(R.styleable.FocusSurfaceView_focus_frame_can_change, false);
            mFrameBackground = ta.getDrawable(R.styleable.FocusSurfaceView_focus_frame_background);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ta.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(viewWidth, viewHeight);

        mBoundaryWidth = viewWidth - getPaddingLeft() - getPaddingRight();
        mBoundaryHeight = viewHeight - getPaddingTop() - getPaddingBottom();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        setupLayout();
    }

    /**
     * 计算裁剪框的位置
     */
    private void setupLayout() {
        if (mBoundaryWidth == 0 || mBoundaryHeight == 0) {
            return;
        }
        mBoundaryRect = new RectF(0f, 0f, mBoundaryWidth, mBoundaryHeight);
        float left = (mBoundaryWidth - mCropWidth) / 2;
        float top = (mBoundaryHeight - mCropHeight) / 2;
        float right = left + mCropWidth;
        float bottom = top + mCropHeight;
        mFrameRect = new RectF(left, top, right, bottom);
        mIsInitialized = true;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mIsInitialized) {
            if (mFrameBackground != null) {
                canvas.save();
                canvas.translate(getWidth() / 2, getHeight() / 2);
                mFrameBackground.setBounds(-mCropWidth / 2, -mCropHeight / 2, mCropWidth / 2, mCropHeight / 2);
                mFrameBackground.draw(canvas);
                canvas.restore();
            }
            drawCropFrame(canvas);
        }
    }

    /**
     * 画裁剪框
     *
     * @param canvas
     */
    private void drawCropFrame(Canvas canvas) {
        if (!mIsCropEnabled) return;
        if (mIsRotating) return;
        drawOverlay(canvas);
        drawFrame(canvas);
        if (mShowGuide) drawGuidelines(canvas);
        if (mShowHandle) drawHandles(canvas);
    }

    /**
     * 画灰色的背景
     *
     * @param canvas
     */
    private void drawOverlay(Canvas canvas) {
        mPaintTranslucent.setAntiAlias(true);
        mPaintTranslucent.setFilterBitmap(true);
        mPaintTranslucent.setColor(mOverlayColor);
        mPaintTranslucent.setStyle(Paint.Style.FILL);
        Path path = new Path();
        if (!mIsAnimating
                && (mCropMode == CropMode.CIRCLE || mCropMode == CropMode.CIRCLE_SQUARE)) {
            path.addRect(mBoundaryRect, Path.Direction.CW);
            PointF circleCenter = new PointF((mFrameRect.left + mFrameRect.right) / 2,
                    (mFrameRect.top + mFrameRect.bottom) / 2);
            float circleRadius = (mFrameRect.right - mFrameRect.left) / 2;
            path.addCircle(circleCenter.x, circleCenter.y, circleRadius, Path.Direction.CCW);
            canvas.drawPath(path, mPaintTranslucent);
        } else {
            path.addRect(mBoundaryRect, Path.Direction.CW);
            path.addRect(mFrameRect, Path.Direction.CCW);
            canvas.drawPath(path, mPaintTranslucent);
        }
    }

    /**
     * 画裁剪框的的四条边
     *
     * @param canvas
     */
    private void drawFrame(Canvas canvas) {
        mPaintFrame.setAntiAlias(true);
        mPaintFrame.setFilterBitmap(true);
        mPaintFrame.setStyle(Paint.Style.STROKE);
        mPaintFrame.setColor(mFrameColor);
        mPaintFrame.setStrokeWidth(mFrameStrokeWeight);
        canvas.drawRect(mFrameRect, mPaintFrame);
    }

    /**
     * 画裁剪框里的横竖线
     */
    private void drawGuidelines(Canvas canvas) {
        mPaintFrame.setColor(mGuideColor);
        mPaintFrame.setStrokeWidth(mGuideStrokeWeight);
        float h1 = mFrameRect.left + (mFrameRect.right - mFrameRect.left) / 3.0f;
        float h2 = mFrameRect.right - (mFrameRect.right - mFrameRect.left) / 3.0f;
        float v1 = mFrameRect.top + (mFrameRect.bottom - mFrameRect.top) / 3.0f;
        float v2 = mFrameRect.bottom - (mFrameRect.bottom - mFrameRect.top) / 3.0f;
        canvas.drawLine(h1, mFrameRect.top, h1, mFrameRect.bottom, mPaintFrame);
        canvas.drawLine(h2, mFrameRect.top, h2, mFrameRect.bottom, mPaintFrame);
        canvas.drawLine(mFrameRect.left, v1, mFrameRect.right, v1, mPaintFrame);
        canvas.drawLine(mFrameRect.left, v2, mFrameRect.right, v2, mPaintFrame);
    }

    /**
     * 画裁剪框四个角上的圆点
     */
    private void drawHandles(Canvas canvas) {
        if (mIsHandleShadowEnabled) drawHandleShadows(canvas);
        mPaintFrame.setStyle(Paint.Style.FILL);
        mPaintFrame.setColor(mHandleColor);
        canvas.drawCircle(mFrameRect.left, mFrameRect.top, mHandleSize, mPaintFrame);
        canvas.drawCircle(mFrameRect.right, mFrameRect.top, mHandleSize, mPaintFrame);
        canvas.drawCircle(mFrameRect.left, mFrameRect.bottom, mHandleSize, mPaintFrame);
        canvas.drawCircle(mFrameRect.right, mFrameRect.bottom, mHandleSize, mPaintFrame);
    }

    /**
     * 画裁剪框四个角上的圆点的阴影
     */
    private void drawHandleShadows(Canvas canvas) {
        mPaintFrame.setStyle(Paint.Style.FILL);
        mPaintFrame.setColor(TRANSLUCENT_BLACK);
        RectF rect = new RectF(mFrameRect);
        rect.offset(0, 1);
        canvas.drawCircle(rect.left, rect.top, mHandleSize, mPaintFrame);
        canvas.drawCircle(rect.right, rect.top, mHandleSize, mPaintFrame);
        canvas.drawCircle(rect.left, rect.bottom, mHandleSize, mPaintFrame);
        canvas.drawCircle(rect.right, rect.bottom, mHandleSize, mPaintFrame);
    }

    /**
     * 计算裁剪框的区域
     *
     * @param imageRect 裁剪框的可活动范围
     */
    private RectF calcFrameRect(RectF imageRect) {
        float frameW = getRatioX(imageRect.width());
        float frameH = getRatioY(imageRect.height());
        float imgRatio = imageRect.width() / imageRect.height();
        float frameRatio = frameW / frameH;
        float l = imageRect.left, t = imageRect.top, r = imageRect.right, b = imageRect.bottom;
        if (frameRatio >= imgRatio) {
            l = imageRect.left;
            r = imageRect.right;
            float hy = (imageRect.top + imageRect.bottom) * 0.5f;
            float hh = (imageRect.width() / frameRatio) * 0.5f;
            t = hy - hh;
            b = hy + hh;
        } else if (frameRatio < imgRatio) {
            t = imageRect.top;
            b = imageRect.bottom;
            float hx = (imageRect.left + imageRect.right) * 0.5f;
            float hw = imageRect.height() * frameRatio * 0.5f;
            l = hx - hw;
            r = hx + hw;
        }
        float w = r - l;
        float h = b - t;
        float cx = l + w / 2;
        float cy = t + h / 2;
        float sw = w * mInitialFrameScale;
        float sh = h * mInitialFrameScale;
        return new RectF(cx - sw / 2, cy - sh / 2, cx + sw / 2, cy + sh / 2);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsChangeEnabled) return false;
        if (!mIsInitialized) return false;
        if (!mIsCropEnabled) return false;
        if (!mIsEnabled) return false;
        if (mIsRotating) return false;
        if (mIsAnimating) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onDown(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                onMove(event);
                if (mTouchArea != TouchArea.OUT_OF_BOUNDS) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                onCancel();
                return true;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                onUp(event);
                return true;
        }
        return false;
    }


    private void onDown(MotionEvent e) {
        invalidate();
        mLastX = e.getX();
        mLastY = e.getY();
        checkTouchArea(e.getX(), e.getY());
    }

    private void onMove(MotionEvent e) {
        float diffX = e.getX() - mLastX;
        float diffY = e.getY() - mLastY;
        switch (mTouchArea) {
            case CENTER:
                moveFrame(diffX, diffY);
                break;
            case LEFT_TOP:
                moveHandleLT(diffX, diffY);
                break;
            case RIGHT_TOP:
                moveHandleRT(diffX, diffY);
                break;
            case LEFT_BOTTOM:
                moveHandleLB(diffX, diffY);
                break;
            case RIGHT_BOTTOM:
                moveHandleRB(diffX, diffY);
                break;
            case OUT_OF_BOUNDS:
                break;
        }
        invalidate();
        mLastX = e.getX();
        mLastY = e.getY();
    }

    private void onUp(MotionEvent e) {
        if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = false;
        if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = false;
        mTouchArea = TouchArea.OUT_OF_BOUNDS;
        invalidate();
    }

    private void onCancel() {
        mTouchArea = TouchArea.OUT_OF_BOUNDS;
        invalidate();
    }

    /**
     * 检查手指触摸的区域
     *
     * @param x X坐标
     * @param y Y坐标
     */
    private void checkTouchArea(float x, float y) {
        if (isInsideCornerLeftTop(x, y)) {
            mTouchArea = TouchArea.LEFT_TOP;
            if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = true;
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            return;
        }
        if (isInsideCornerRightTop(x, y)) {
            mTouchArea = TouchArea.RIGHT_TOP;
            if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = true;
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            return;
        }
        if (isInsideCornerLeftBottom(x, y)) {
            mTouchArea = TouchArea.LEFT_BOTTOM;
            if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = true;
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            return;
        }
        if (isInsideCornerRightBottom(x, y)) {
            mTouchArea = TouchArea.RIGHT_BOTTOM;
            if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = true;
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            return;
        }
        if (isInsideFrame(x, y)) {
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            mTouchArea = TouchArea.CENTER;
            return;
        }
        mTouchArea = TouchArea.OUT_OF_BOUNDS;
    }

    private boolean isInsideFrame(float x, float y) {
        if (mFrameRect.left <= x && mFrameRect.right >= x) {
            if (mFrameRect.top <= y && mFrameRect.bottom >= y) {
                mTouchArea = TouchArea.CENTER;
                return true;
            }
        }
        return false;
    }

    private boolean isInsideCornerLeftTop(float x, float y) {
        float dx = x - mFrameRect.left;
        float dy = y - mFrameRect.top;
        float d = dx * dx + dy * dy;
        return sq(mHandleSize + mTouchPadding) >= d;
    }

    private boolean isInsideCornerRightTop(float x, float y) {
        float dx = x - mFrameRect.right;
        float dy = y - mFrameRect.top;
        float d = dx * dx + dy * dy;
        return sq(mHandleSize + mTouchPadding) >= d;
    }

    private boolean isInsideCornerLeftBottom(float x, float y) {
        float dx = x - mFrameRect.left;
        float dy = y - mFrameRect.bottom;
        float d = dx * dx + dy * dy;
        return sq(mHandleSize + mTouchPadding) >= d;
    }

    private boolean isInsideCornerRightBottom(float x, float y) {
        float dx = x - mFrameRect.right;
        float dy = y - mFrameRect.bottom;
        float d = dx * dx + dy * dy;
        return sq(mHandleSize + mTouchPadding) >= d;
    }

    // Adjust frame ////////////////////////////////////////////////////////////////////////////////

    private void moveFrame(float x, float y) {
        mFrameRect.left += x;
        mFrameRect.right += x;
        mFrameRect.top += y;
        mFrameRect.bottom += y;
        checkMoveBounds();
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void moveHandleLT(float diffX, float diffY) {
        if (mCropMode == CropMode.FREE) {
            mFrameRect.left += diffX;
            mFrameRect.top += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameWidth();
                mFrameRect.left -= offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameHeight();
                mFrameRect.top -= offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRect.left += dx;
            mFrameRect.top += dy;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameWidth();
                mFrameRect.left -= offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRect.top -= offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameHeight();
                mFrameRect.top -= offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRect.left -= offsetX;
            }
            float ox, oy;
            if (!isInsideHorizontal(mFrameRect.left)) {
                ox = mBoundaryRect.left - mFrameRect.left;
                mFrameRect.left += ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRect.top += oy;
            }
            if (!isInsideVertical(mFrameRect.top)) {
                oy = mBoundaryRect.top - mFrameRect.top;
                mFrameRect.top += oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRect.left += ox;
            }
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void moveHandleRT(float diffX, float diffY) {
        if (mCropMode == CropMode.FREE) {
            mFrameRect.right += diffX;
            mFrameRect.top += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameWidth();
                mFrameRect.right += offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameHeight();
                mFrameRect.top -= offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRect.right += dx;
            mFrameRect.top -= dy;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameWidth();
                mFrameRect.right += offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRect.top -= offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameHeight();
                mFrameRect.top -= offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRect.right += offsetX;
            }
            float ox, oy;
            if (!isInsideHorizontal(mFrameRect.right)) {
                ox = mFrameRect.right - mBoundaryRect.right;
                mFrameRect.right -= ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRect.top += oy;
            }
            if (!isInsideVertical(mFrameRect.top)) {
                oy = mBoundaryRect.top - mFrameRect.top;
                mFrameRect.top += oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRect.right -= ox;
            }
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void moveHandleLB(float diffX, float diffY) {
        if (mCropMode == CropMode.FREE) {
            mFrameRect.left += diffX;
            mFrameRect.bottom += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameWidth();
                mFrameRect.left -= offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameHeight();
                mFrameRect.bottom += offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRect.left += dx;
            mFrameRect.bottom -= dy;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameWidth();
                mFrameRect.left -= offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRect.bottom += offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameHeight();
                mFrameRect.bottom += offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRect.left -= offsetX;
            }
            float ox, oy;
            if (!isInsideHorizontal(mFrameRect.left)) {
                ox = mBoundaryRect.left - mFrameRect.left;
                mFrameRect.left += ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRect.bottom -= oy;
            }
            if (!isInsideVertical(mFrameRect.bottom)) {
                oy = mFrameRect.bottom - mBoundaryRect.bottom;
                mFrameRect.bottom -= oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRect.left += ox;
            }
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void moveHandleRB(float diffX, float diffY) {
        if (mCropMode == CropMode.FREE) {
            mFrameRect.right += diffX;
            mFrameRect.bottom += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameWidth();
                mFrameRect.right += offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameHeight();
                mFrameRect.bottom += offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRect.right += dx;
            mFrameRect.bottom += dy;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameWidth();
                mFrameRect.right += offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRect.bottom += offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameHeight();
                mFrameRect.bottom += offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRect.right += offsetX;
            }
            float ox, oy;
            if (!isInsideHorizontal(mFrameRect.right)) {
                ox = mFrameRect.right - mBoundaryRect.right;
                mFrameRect.right -= ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRect.bottom -= oy;
            }
            if (!isInsideVertical(mFrameRect.bottom)) {
                oy = mFrameRect.bottom - mBoundaryRect.bottom;
                mFrameRect.bottom -= oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRect.right -= ox;
            }
        }
    }

    // Frame position correction ///////////////////////////////////////////////////////////////////

    private void checkScaleBounds() {
        float lDiff = mFrameRect.left - mBoundaryRect.left;
        float rDiff = mFrameRect.right - mBoundaryRect.right;
        float tDiff = mFrameRect.top - mBoundaryRect.top;
        float bDiff = mFrameRect.bottom - mBoundaryRect.bottom;

        if (lDiff < 0) {
            mFrameRect.left -= lDiff;
        }
        if (rDiff > 0) {
            mFrameRect.right -= rDiff;
        }
        if (tDiff < 0) {
            mFrameRect.top -= tDiff;
        }
        if (bDiff > 0) {
            mFrameRect.bottom -= bDiff;
        }
    }

    private void checkMoveBounds() {
        float diff = mFrameRect.left - mBoundaryRect.left;
        if (diff < 0) {
            mFrameRect.left -= diff;
            mFrameRect.right -= diff;
        }
        diff = mFrameRect.right - mBoundaryRect.right;
        if (diff > 0) {
            mFrameRect.left -= diff;
            mFrameRect.right -= diff;
        }
        diff = mFrameRect.top - mBoundaryRect.top;
        if (diff < 0) {
            mFrameRect.top -= diff;
            mFrameRect.bottom -= diff;
        }
        diff = mFrameRect.bottom - mBoundaryRect.bottom;
        if (diff > 0) {
            mFrameRect.top -= diff;
            mFrameRect.bottom -= diff;
        }
    }

    private boolean isInsideHorizontal(float x) {
        return mBoundaryRect.left <= x && mBoundaryRect.right >= x;
    }

    private boolean isInsideVertical(float y) {
        return mBoundaryRect.top <= y && mBoundaryRect.bottom >= y;
    }

    private boolean isWidthTooSmall() {
        return getFrameWidth() < mMinFrameSize;
    }

    private boolean isHeightTooSmall() {
        return getFrameHeight() < mMinFrameSize;
    }

    // Frame aspect ratio correction ///////////////////////////////////////////////////////////////

    private void recalculateFrameRect(int durationMillis) {
        if (mBoundaryRect == null) return;
        if (mIsAnimating) {
            getAnimator().cancelAnimation();
        }
        final RectF currentRect = new RectF(mFrameRect);
        final RectF newRect = calcFrameRect(mBoundaryRect);
        final float diffL = newRect.left - currentRect.left;
        final float diffT = newRect.top - currentRect.top;
        final float diffR = newRect.right - currentRect.right;
        final float diffB = newRect.bottom - currentRect.bottom;
        if (mIsAnimationEnabled) {
            SimpleValueAnimator animator = getAnimator();
            animator.addAnimatorListener(new SimpleValueAnimatorListener() {
                @Override
                public void onAnimationStarted() {
                    mIsAnimating = true;
                }

                @Override
                public void onAnimationUpdated(float scale) {
                    mFrameRect = new RectF(currentRect.left + diffL * scale,
                            currentRect.top + diffT * scale,
                            currentRect.right + diffR * scale,
                            currentRect.bottom + diffB * scale);
                    invalidate();
                }

                @Override
                public void onAnimationFinished() {
                    mFrameRect = newRect;
                    invalidate();
                    mIsAnimating = false;
                }
            });
            animator.startAnimation(durationMillis);
        } else {
            mFrameRect = calcFrameRect(mBoundaryRect);
            invalidate();
        }
    }

    private float getRatioX(float w) {
        switch (mCropMode) {
            case FIT_IMAGE:
                return mBoundaryRect.width();
            case FREE:
                return w;
            case RATIO_4_3:
                return 4;
            case RATIO_3_4:
                return 3;
            case RATIO_16_9:
                return 16;
            case RATIO_9_16:
                return 9;
            case SQUARE:
            case CIRCLE:
            case CIRCLE_SQUARE:
                return 1;
            case CUSTOM:
                return mCustomRatio.x;
            default:
                return w;
        }
    }

    private float getRatioY(float h) {
        switch (mCropMode) {
            case FIT_IMAGE:
                return mBoundaryRect.height();
            case FREE:
                return h;
            case RATIO_4_3:
                return 3;
            case RATIO_3_4:
                return 4;
            case RATIO_16_9:
                return 9;
            case RATIO_9_16:
                return 16;
            case SQUARE:
            case CIRCLE:
            case CIRCLE_SQUARE:
                return 1;
            case CUSTOM:
                return mCustomRatio.y;
            default:
                return h;
        }
    }

    private float getRatioX() {
        switch (mCropMode) {
            case FIT_IMAGE:
                return mBoundaryRect.width();
            case RATIO_4_3:
                return 4;
            case RATIO_3_4:
                return 3;
            case RATIO_16_9:
                return 16;
            case RATIO_9_16:
                return 9;
            case SQUARE:
            case CIRCLE:
            case CIRCLE_SQUARE:
                return 1;
            case CUSTOM:
                return mCustomRatio.x;
            default:
                return 1;
        }
    }

    private float getRatioY() {
        switch (mCropMode) {
            case FIT_IMAGE:
                return mBoundaryRect.height();
            case RATIO_4_3:
                return 3;
            case RATIO_3_4:
                return 4;
            case RATIO_16_9:
                return 9;
            case RATIO_9_16:
                return 16;
            case SQUARE:
            case CIRCLE:
            case CIRCLE_SQUARE:
                return 1;
            case CUSTOM:
                return mCustomRatio.y;
            default:
                return 1;
        }
    }

    // Utility /////////////////////////////////////////////////////////////////////////////////////

    private float getDensity() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getMetrics(displayMetrics);
        return displayMetrics.density;
    }

    private float sq(float value) {
        return value * value;
    }

    private float constrain(float val, float min, float max, float defaultVal) {
        if (val < min || val > max) return defaultVal;
        return val;
    }

    /**
     * 旋转后的宽度
     *
     * @param angle 旋转的角度
     * @return 旋转后的宽度
     */
    private float getRotatedWidth(float angle) {
        return getRotatedWidth(angle, mBoundaryWidth, mBoundaryHeight);
    }

    /**
     * 旋转后的宽度
     *
     * @param angle  旋转的角度
     * @param width  view的宽
     * @param height view的高
     * @return 旋转后的宽度
     */
    private float getRotatedWidth(float angle, float width, float height) {
        return angle % 180 == 0 ? width : height;
    }

    /**
     * 旋转后的高度
     *
     * @param angle 旋转的角度
     * @return 旋转后的高度
     */
    private float getRotatedHeight(float angle) {
        return getRotatedHeight(angle, mBoundaryWidth, mBoundaryHeight);
    }

    /**
     * 旋转后的高度
     *
     * @param angle  旋转的角度
     * @param width  view的宽
     * @param height view的高
     * @return 旋转后的宽度
     */
    private float getRotatedHeight(float angle, float width, float height) {
        return angle % 180 == 0 ? height : width;
    }

    // Animation ///////////////////////////////////////////////////////////////////////////////////

    private SimpleValueAnimator getAnimator() {
        setupAnimatorIfNeeded();
        return mAnimator;
    }

    private void setupAnimatorIfNeeded() {
        if (mAnimator == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mAnimator = new ValueAnimatorV8(mInterpolator);
            } else {
                mAnimator = new ValueAnimatorV14(mInterpolator);
            }
        }
    }

    /**
     * Set crop mode
     *
     * @param mode           crop mode
     * @param durationMillis animation duration in milliseconds
     */
    public void setCropMode(CropMode mode, int durationMillis) {
        if (mode == CropMode.CUSTOM) {
            setCustomRatio(1, 1);
        } else {
            mCropMode = mode;
            recalculateFrameRect(durationMillis);
        }
    }

    /**
     * 设置裁剪模式
     */
    public void setCropMode(CropMode mode) {
        setCropMode(mode, mAnimationDurationMillis);
    }

    /**
     * Set custom aspect ratio to crop frame
     *
     * @param ratioX         ratio x
     * @param ratioY         ratio y
     * @param durationMillis animation duration in milliseconds
     */
    public void setCustomRatio(int ratioX, int ratioY, int durationMillis) {
        if (ratioX == 0 || ratioY == 0) return;
        mCropMode = CropMode.CUSTOM;
        mCustomRatio.set(ratioX, ratioY);
        recalculateFrameRect(durationMillis);
    }

    /**
     * 设置裁剪框的长宽比
     *
     * @param ratioX ratio x
     * @param ratioY ratio y
     */
    public void setCustomRatio(int ratioX, int ratioY) {
        setCustomRatio(ratioX, ratioY, mAnimationDurationMillis);
    }

    /**
     * 设置裁剪框以外的颜色
     *
     * @param overlayColor color resId or color int(ex. 0xFFFFFFFF)
     */
    public void setOverlayColor(int overlayColor) {
        this.mOverlayColor = overlayColor;
        invalidate();
    }

    /**
     * 设置裁剪框的颜色
     */
    public void setFrameColor(int frameColor) {
        this.mFrameColor = frameColor;
        invalidate();
    }

    /**
     * 裁剪框四个点的颜色
     */
    public void setHandleColor(int handleColor) {
        this.mHandleColor = handleColor;
        invalidate();
    }

    /**
     * 裁剪框横竖线的颜色
     */
    public void setGuideColor(int guideColor) {
        this.mGuideColor = guideColor;
        invalidate();
    }

    /**
     * 裁剪框的最小宽度，单位dp
     *
     * @param minDp crop frame minimum size in density-independent pixels
     */
    public void setMinFrameSizeInDp(int minDp) {
        mMinFrameSize = minDp * getDensity();
    }

    /**
     * 裁剪框的最小宽度，单位px
     *
     * @param minPx crop frame minimum size in pixels
     */
    public void setMinFrameSizeInPx(int minPx) {
        mMinFrameSize = minPx;
    }

    /**
     * 设置四个点的大小，单位dp
     *
     * @param handleDp handle radius in density-independent pixels
     */
    public void setHandleSizeInDp(int handleDp) {
        mHandleSize = (int) (handleDp * getDensity());
    }

    /**
     * 设置触摸区域的大小
     *
     * @param paddingDp crop frame handle touch padding(touch area) in density-independent pixels
     */
    public void setTouchPaddingInDp(int paddingDp) {
        mTouchPadding = (int) (paddingDp * getDensity());
    }

    /**
     * 设置 guideline 的现实模式
     * (SHOW_ALWAYS/NOT_SHOW/SHOW_ON_TOUCH)
     *
     * @param mode guideline show mode
     */
    public void setGuideShowMode(ShowMode mode) {
        mGuideShowMode = mode;
        switch (mode) {
            case SHOW_ALWAYS:
                mShowGuide = true;
                break;
            case NOT_SHOW:
            case SHOW_ON_TOUCH:
                mShowGuide = false;
                break;
        }
        invalidate();
    }

    /**
     * 设置 handle 的显示模式
     * (SHOW_ALWAYS/NOT_SHOW/SHOW_ON_TOUCH)
     *
     * @param mode handle show mode
     */
    public void setHandleShowMode(ShowMode mode) {
        mHandleShowMode = mode;
        switch (mode) {
            case SHOW_ALWAYS:
                mShowHandle = true;
                break;
            case NOT_SHOW:
            case SHOW_ON_TOUCH:
                mShowHandle = false;
                break;
        }
        invalidate();
    }

    /**
     * 设置裁剪框的宽
     */
    public void setFrameStrokeWeightInDp(int weightDp) {
        mFrameStrokeWeight = weightDp * getDensity();
        invalidate();
    }

    /**
     * 设置裁剪框内横竖线的宽
     */
    public void setGuideStrokeWeightInDp(int weightDp) {
        mGuideStrokeWeight = weightDp * getDensity();
        invalidate();
    }

    /**
     * 是否显示裁剪框
     *
     * @param enabled should show crop frame?
     */
    public void setCropEnabled(boolean enabled) {
        mIsCropEnabled = enabled;
        invalidate();
    }

    /**
     * 是否锁定裁剪框，若锁定怎裁剪框不能移动
     *
     * @param enabled should lock crop frame?
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    /**
     * Set initial scale of the frame.(0.01 ~ 1.0)
     *
     * @param initialScale initial scale
     */
    public void setInitialFrameScale(float initialScale) {
        mInitialFrameScale = constrain(initialScale, 0.01f, 1.0f, DEFAULT_INITIAL_FRAME_SCALE);
    }

    /**
     * 设置是否显示裁剪框上四个点的阴影效果
     */
    public void setHandleShadowEnabled(boolean handleShadowEnabled) {
        mIsHandleShadowEnabled = handleShadowEnabled;
    }

    /**
     * 获取裁剪框的宽
     */
    private float getFrameWidth() {
        return (mFrameRect.right - mFrameRect.left);
    }

    /**
     * 获取裁剪框的高
     */
    private float getFrameHeight() {
        return (mFrameRect.bottom - mFrameRect.top);
    }

    /**
     * 手指触摸区域
     */
    private enum TouchArea {
        OUT_OF_BOUNDS, CENTER, LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM
    }

    /**
     * 裁剪模式
     */
    public enum CropMode {
        FIT_IMAGE(0), RATIO_4_3(1), RATIO_3_4(2), SQUARE(3), RATIO_16_9(4), RATIO_9_16(5), FREE(
                6), CUSTOM(7), CIRCLE(8), CIRCLE_SQUARE(9);
        private final int ID;

        CropMode(final int id) {
            this.ID = id;
        }

        public int getId() {
            return ID;
        }
    }

    /**
     * 显示模式
     */
    public enum ShowMode {
        SHOW_ALWAYS(1), SHOW_ON_TOUCH(2), NOT_SHOW(3);
        private final int ID;

        ShowMode(final int id) {
            this.ID = id;
        }

        public int getId() {
            return ID;
        }
    }

    /**
     * 获取裁剪框
     */
    public RectF getFrameRect() {
        return mFrameRect;
    }

    /**
     * 获取照片
     *
     * @param data 从camera返回的数据
     * @return 裁剪后的bitmap
     */
    public Bitmap getPicture(byte[] data) {
        //原始照片
        Bitmap originBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        //原始照片的宽高
        float picWidth = originBitmap.getWidth();
        float picHeight = originBitmap.getHeight();

        //预览界面的宽高
        float preWidth = getWidth();
        float preHeight = getHeight();

        //预览界面和照片的比例
        float preRW = picWidth / preWidth;
        float preRH = picHeight / preHeight;

        //裁剪框的位置和宽高
        RectF frameRect = getFrameRect();
        float frameLeft = frameRect.left;
        float frameTop = frameRect.top;
        float frameWidth = frameRect.width();
        float frameHeight = frameRect.height();

        int cropLeft = (int) (frameLeft * preRW);
        int cropTop = (int) (frameTop * preRH);
        int cropWidth = (int) (frameWidth * preRW);
        int cropHeight = (int) (frameHeight * preRH);

        Bitmap cropBitmap = Bitmap.createBitmap(originBitmap, cropLeft, cropTop, cropWidth, cropHeight);

        if (mCropMode == CropMode.CIRCLE) {
            cropBitmap = getCircularBitmap(cropBitmap);
        }
        return cropBitmap;
    }

    /**
     * 获取圆形图片
     */
    public Bitmap getCircularBitmap(Bitmap square) {
        if (square == null) return null;
        Bitmap output = Bitmap.createBitmap(square.getWidth(), square.getHeight(), Bitmap.Config.ARGB_8888);

        final Rect rect = new Rect(0, 0, square.getWidth(), square.getHeight());
        Canvas canvas = new Canvas(output);

        int halfWidth = square.getWidth() / 2;
        int halfHeight = square.getHeight() / 2;

        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);

        canvas.drawCircle(halfWidth, halfHeight, Math.min(halfWidth, halfHeight), paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(square, rect, rect, paint);
        return output;
    }

    private int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }
}
