package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.math.MathUtils;
import androidx.core.view.ViewCompat;

import green_green_avk.anotherterm.ConsoleInput;
import green_green_avk.anotherterm.ConsoleOutput;
import green_green_avk.anotherterm.ConsoleScreenBuffer;
import green_green_avk.anotherterm.ConsoleScreenCharAttrs;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.CharsAutoSelector;
import green_green_avk.anotherterm.utils.WeakHandler;

public class ConsoleScreenView extends ScrollableView
        implements ConsoleInput.OnInvalidateSink, ConsoleInput.OnBufferScroll {

    public static class State {
        private PointF scrollPosition = null;
        private float fontSize = 0;
        private boolean resizeBufferXOnUi = true;
        private boolean resizeBufferYOnUi = true;

        public void save(@NonNull final ConsoleScreenView v) {
            scrollPosition = v.scrollPosition;
            resizeBufferXOnUi = v.resizeBufferXOnUi;
            resizeBufferYOnUi = v.resizeBufferYOnUi;
            fontSize = v.getFontSize();
        }

        public void apply(@NonNull final ConsoleScreenView v) {
            if (scrollPosition == null) return;
            v.scrollPosition.set(scrollPosition);
            v.resizeBufferXOnUi = resizeBufferXOnUi;
            v.resizeBufferYOnUi = resizeBufferYOnUi;
            v.setFontSize(fontSize);
            v.execOnScroll();
        }
    }

    protected static final int MSG_BLINK = 0;
    protected static final int INTERVAL_BLINK = 500; // ms
    protected static final long SELECTION_MOVE_START_DELAY = 200; // ms
    protected ConsoleInput consoleInput = null;
    public final ConsoleScreenCharAttrs charAttrs = new ConsoleScreenCharAttrs();
    protected final Paint fgPaint = new Paint();
    protected final Paint bgPaint = new Paint();
    protected final Paint cursorPaint = new Paint();
    protected final Paint selectionPaint = new Paint();
    protected final Paint paddingMarkupPaint = new Paint();
    protected Drawable selectionMarkerPtr = null;
    protected Drawable selectionMarkerPad = null;
    protected Drawable selectionMarkerOOB = null;
    protected Drawable attrMarkupBlinking = null;
    protected Drawable paddingMarkup = null;
    protected FontProvider fontProvider = new DefaultConsoleFontProvider();
    protected float mFontSize = 16;
    protected float mFontWidth;
    protected float mFontHeight;
    protected float selectionPadSize = 200; // px
    protected ConsoleScreenSelection selection = null;
    protected boolean selectionMode = false;
    protected boolean selectionModeIsExpr = false;
    protected SelectionPopup selectionPopup = null;
    protected final PointF selectionMarkerFirst = new PointF();
    protected final PointF selectionMarkerLast = new PointF();
    protected final PointF selectionMarkerExpr = new PointF();
    @Nullable
    protected PointF selectionMarker = null;
    protected boolean mouseMode = false;
    public boolean resizeBufferXOnUi = true;
    public boolean resizeBufferYOnUi = true;

    // Visible part of the history buffer to stick to the scroll position it
    protected float scrollFollowHistoryThreshold = 0.5F;

    private boolean mBlinkState = true;
    private WeakHandler mHandler = null;

    protected static final int popupMeasureSpec =
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    protected static final int[] noneSelectionModeState = new int[0];
    protected static final int[] linesSelectionModeState = new int[]{R.attr.state_select_lines};
    protected static final int[] rectSelectionModeState = new int[]{R.attr.state_select_rect};
    protected static final int[] exprSelectionModeState = new int[]{R.attr.state_select_expr};

    protected class SelectionPopup {
        protected final int[] parentPos = new int[2];
        protected final PopupWindow window;
        protected final ImageView smv;

        {
            final View v = inflate(getContext(), R.layout.select_search_popup, null);
            v.measure(popupMeasureSpec, popupMeasureSpec);
            window = new PopupWindow(v,
                    v.getMeasuredWidth(), v.getMeasuredHeight(), false);
            smv = getContentView().findViewById(R.id.b_select_mode);
            refresh();
            getContentView().findViewById(R.id.b_close)
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            setSelectionMode(false);
                        }
                    });
            smv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (getSelectionModeIsExpr()) setSelectionModeIsExpr(false);
                    else {
                        if (getSelectionIsRect()) setSelectionModeIsExpr(true);
                        setSelectionIsRect(!getSelectionIsRect());
                    }
                    refresh();
                }
            });
            getContentView().findViewById(R.id.b_select_all)
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            selectAll();
                            setSelectionModeIsExpr(false);
                            setSelectionIsRect(true);
                            setSelectionMode(true);
                        }
                    });
            getContentView().findViewById(R.id.b_copy)
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            UiUtils.toClipboard(getContext(), getSelectedText());
                        }
                    });
            getContentView().findViewById(R.id.b_put)
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            if (consoleInput == null || consoleInput.consoleOutput == null) return;
                            final String s = getSelectedText();
                            if (s != null) consoleInput.consoleOutput.paste(s);
                        }
                    });
            getContentView().findViewById(R.id.b_web)
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            final String s = getSelectedText();
                            if (s == null) {
                                Toast.makeText(getContext(), R.string.msg_nothing_to_search,
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            try {
                                getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(s.replaceAll("\\s+", ""))));
                            } catch (final ActivityNotFoundException e) {
                                try {
                                    getContext().startActivity(new Intent(Intent.ACTION_WEB_SEARCH)
                                            .putExtra(SearchManager.QUERY, s));
                                } catch (final ActivityNotFoundException ignored) {
                                }
                            }
                        }
                    });
            getContentView().findViewById(R.id.b_share)
                    .setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            UiUtils.sharePlainText((Activity) getContext(), getSelectedText());
                        }
                    });
        }

        protected void refresh() {
            final int[] st;
            if (getSelectionModeIsExpr()) st = exprSelectionModeState;
            else if (getSelectionIsRect()) st = rectSelectionModeState;
            else st = linesSelectionModeState;
            smv.setImageState(st, false);
        }

        public View getContentView() {
            return window.getContentView();
        }

        public void show() {
            if (selection != null && !window.isShowing()) {
                getLocationInWindow(parentPos);
                final int sx = window.getWidth();
                final int sy = window.getHeight();
                int x = (int) getBufferDrawPosXF((selection.first.x +
                        selection.last.x + 1) / 2f) - sx / 2;
                x = MathUtils.clamp(x, 0, getWidth() - sx);
                int y = (int) getBufferDrawPosYF(selection.getDirect().first.y) - sy;
                if (y < 0) y = (int) getBufferDrawPosYF(selection.getDirect().last.y + 1);
                if (y > getHeight() - sy)
                    y = (int) (getBufferDrawPosYF(
                            (selection.first.y + selection.last.y + 1) / 2f)) - sy / 2;
                y = MathUtils.clamp(y, 0, getHeight() - sy);
                window.showAtLocation(ConsoleScreenView.this, Gravity.NO_GRAVITY,
                        parentPos[0] + x, parentPos[1] + y);
            }
            refresh();
        }

        public void hide() {
            window.dismiss();
        }
    }

    public ConsoleScreenView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.consoleScreenViewStyle);
    }

    public ConsoleScreenView(final Context context, final AttributeSet attrs,
                             final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        appTextScroller = new ScrollerEx(context);
        init(context, attrs, defStyleAttr, R.style.AppConsoleScreenViewStyle);
    }

    public ConsoleScreenView(final Context context, final AttributeSet attrs,
                             final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        appTextScroller = new ScrollerEx(context);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void init(final Context context, final AttributeSet attrs,
                        final int defStyleAttr, final int defStyleRes) {
        final int cursorColor;
        final int selectionColor;
        final int attrMarkupAlpha;
        final int paddingMarkupAlpha;
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.ConsoleScreenView, defStyleAttr, defStyleRes);
        try {
            cursorColor = a.getColor(R.styleable.ConsoleScreenView_cursorColor,
                    Color.argb(127, 255, 255, 255));
            selectionColor = a.getColor(R.styleable.ConsoleScreenView_selectionColor,
                    Color.argb(127, 0, 255, 0));
//            selectionMarkerPtr = a.getDrawable(R.styleable.ConsoleScreenView_selectionMarker);
            selectionMarkerPtr = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ConsoleScreenView_selectionMarker, 0));
            selectionMarkerPad = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ConsoleScreenView_selectionMarkerPad, 0));
            selectionMarkerOOB = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ConsoleScreenView_selectionMarkerOOB, 0));
            attrMarkupBlinking = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ConsoleScreenView_attrMarkupBlinking, 0));
            attrMarkupAlpha = (int) (a.getFloat(R.styleable.ConsoleScreenView_attrMarkupAlpha,
                    0.5f) * 255);
            paddingMarkup = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ConsoleScreenView_paddingMarkup, 0));
            paddingMarkupAlpha = (int) (a.getFloat(R.styleable.ConsoleScreenView_paddingMarkupAlpha,
                    0.2f) * 255);
        } finally {
            a.recycle();
        }

        attrMarkupBlinking.setAlpha(attrMarkupAlpha);
        paddingMarkup.setAlpha(paddingMarkupAlpha);

        cursorPaint.setColor(cursorColor);
        cursorPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        selectionPaint.setColor(selectionColor);
        selectionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));

        if (selectionMarkerPtr != null)
            selectionMarkerPtr.setColorFilter(selectionPaint.getColor(), PorterDuff.Mode.MULTIPLY);
        if (selectionMarkerPad != null)
            selectionMarkerPad.setColorFilter(selectionPaint.getColor(), PorterDuff.Mode.MULTIPLY);
        if (selectionMarkerOOB != null)
            selectionMarkerOOB.setColorFilter(selectionPaint.getColor(), PorterDuff.Mode.MULTIPLY);

        selectionPopup = new SelectionPopup();

        paddingMarkupPaint.setColor(getResources().getColor(R.color.colorMiddle));
        paddingMarkupPaint.setStrokeWidth(3);
        paddingMarkupPaint.setStyle(Paint.Style.STROKE);
        paddingMarkupPaint.setAlpha(paddingMarkupAlpha);
        paddingMarkupPaint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
        if (Build.VERSION.SDK_INT >= 21) {
            // At least, devices with Android 4.4.2 can have monospace font width glitches with these settings.
            fgPaint.setHinting(Paint.HINTING_ON);
            fgPaint.setFlags(fgPaint.getFlags()
                    | Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            fgPaint.setElegantTextHeight(true);
        }
        applyFont();
        applyCharAttrs();

        mGestureDetector.setOnDoubleTapListener(null);
//        mGestureDetector.setContextClickListener(null);
        mGestureDetector.setIsLongpressEnabled(false);
    }

    protected void resizeBuffer(int cols, int rows) {
        if (consoleInput == null) return;
        if (cols <= 0 || !resizeBufferXOnUi) cols = consoleInput.currScrBuf.getWidth();
        if (rows <= 0 || !resizeBufferYOnUi) rows = consoleInput.currScrBuf.getHeight();
        consoleInput.resize(cols, rows);
    }

    protected void resizeBuffer() {
        resizeBuffer(getCols(), getRows());
    }

    /**
     * Define fixed or variable (if dimension <= 0) screen size.
     */
    public void setScreenSize(int cols, int rows) {
        resizeBufferXOnUi = cols <= 0;
        resizeBufferYOnUi = rows <= 0;
        if (resizeBufferXOnUi) cols = getCols();
        if (resizeBufferYOnUi) rows = getRows();
        consoleInput.resize(cols, rows);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHandler = new WeakHandler() {
            @Override
            public void handleMessage(@NonNull final Message msg) {
                switch (msg.what) {
                    case MSG_BLINK:
                        mBlinkState = !mBlinkState;
                        if (consoleInput != null)
                            invalidate(getBufferDrawRect(
                                    consoleInput.currScrBuf.getAbsPosX(),
                                    consoleInput.currScrBuf.getAbsPosY()
                            ));
                        sendEmptyMessageDelayed(MSG_BLINK, INTERVAL_BLINK);
                        break;
                }
            }
        };
        mHandler.sendEmptyMessage(MSG_BLINK);
    }

    @Override
    protected void onDetachedFromWindow() {
        mHandler.removeMessages(MSG_BLINK);
        mHandler = null;
        super.onDetachedFromWindow();
    }

    @Override
    public float getTopScrollLimit() {
        return (consoleInput == null) ? 0F : Math.min(
                consoleInput.currScrBuf.getHeight() - getRows(),
                -consoleInput.currScrBuf.getScrollableHeight());
    }

    @Override
    public float getBottomScrollLimit() {
        return (consoleInput == null) ? 0F : Math.max(
                consoleInput.currScrBuf.getHeight() - getRows(),
                -consoleInput.currScrBuf.getScrollableHeight());
    }

    @Override
    public float getRightScrollLimit() {
        return (consoleInput == null) ? 0F : Math.max(
                consoleInput.currScrBuf.getWidth() - getCols(),
                0F);
    }

    protected void applyFont() {
        final Paint p = new Paint();
        fontProvider.setPaint(p, Typeface.NORMAL);
        p.setTextSize(mFontSize);
        mFontHeight = p.getFontSpacing();
        mFontWidth = p.measureText("A");
        fgPaint.setTextSize(mFontSize);
        setScrollScale(mFontWidth, mFontHeight);
    }

    protected void _setFont(@NonNull final FontProvider fp) {
        fontProvider = fp;
    }

    public void setFont(@NonNull final FontProvider fp) {
        _setFont(fp);
        applyFont();
        resizeBuffer();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public float getFontSize() {
        return mFontSize;
    }

    public void setFontSize(final float size) {
        mFontSize = size;
        applyFont();
        resizeBuffer();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public float getScrollFollowHistoryThreshold() {
        return scrollFollowHistoryThreshold;
    }

    /**
     * Follow the history buffer when condition meets
     *
     * @param v % of visible history buffer (v <= 0 - off)
     */
    public void setScrollFollowHistoryThreshold(final float v) {
        scrollFollowHistoryThreshold = v;
    }

    public float getSelectionPadSize() {
        return selectionPadSize;
    }

    public void setSelectionPadSize(final float v) {
        selectionPadSize = v;
        ViewCompat.postInvalidateOnAnimation(this);
    }

    protected void adjustSelectionPopup() {
        if (selectionMode && !inGesture && mScroller.isFinished()) selectionPopup.show();
        else selectionPopup.hide();
    }

    public boolean getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(final boolean mode) {
        if (mode) setMouseMode(false);
        selectionMode = mode;
        if (mode) {
            if (selection == null) {
                selection = new ConsoleScreenSelection();
                selection.first.x = selection.last.x = getCols() / 2;
                selection.first.y = selection.last.y = getRows() / 2;
                getCenterText(selection.first.x, selection.first.y, selectionMarkerExpr);
            }
            getCenterText(selection.first.x, selection.first.y, selectionMarkerFirst);
            getCenterText(selection.last.x, selection.last.y, selectionMarkerLast);
        } else {
            unsetCurrentSelectionMarker();
        }
        adjustSelectionPopup();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public boolean getSelectionIsRect() {
        return selection != null && selection.isRectangular;
    }

    public void setSelectionIsRect(final boolean v) {
        if (selection != null) {
            selection.isRectangular = v;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public boolean getSelectionModeIsExpr() {
        return selectionModeIsExpr;
    }

    public void setSelectionModeIsExpr(final boolean v) {
        selectionModeIsExpr = v;
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void selectAll() {
        if (consoleInput != null) {
            if (selection == null) selection = new ConsoleScreenSelection();
            selection.first.x = 0;
            selection.first.y = consoleInput.currScrBuf.getHeight()
                    - consoleInput.currScrBuf.getBufferHeight();
            selection.last.x = consoleInput.currScrBuf.getWidth() - 1;
            selection.last.y = consoleInput.currScrBuf.getHeight() - 1;
        }
    }

    public boolean getMouseMode() {
        return mouseMode;
    }

    public void setMouseMode(final boolean mode) {
        if (mode) setSelectionMode(false);
        mouseMode = mode;
        scrollDisabled = mode;
    }

    protected void getCenter(final int x, final int y, @NonNull final Point r) {
        final Rect p = getBufferDrawRect(x, y);
        r.x = (p.left + p.right) / 2;
        r.y = (p.top + p.bottom) / 2;
    }

    protected void getCenterText(final float x, final float y, @NonNull final PointF r) {
        r.x = x + 0.5f;
        r.y = y + 0.5f;
    }

    protected Rect getBufferDrawRect(final int x, final int y) {
        return getBufferDrawRect(x, y, x + 1, y + 1);
    }

    protected void getBufferDrawRect(final int x, final int y, @NonNull final Rect rect) {
        getBufferDrawRect(x, y, x + 1, y + 1, rect);
    }

    protected Rect getBufferDrawRect(@NonNull final Rect rect) {
        return getBufferDrawRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    protected Rect getBufferDrawRect(final int left, final int top,
                                     final int right, final int bottom) {
        final Rect r = new Rect();
        getBufferDrawRect(left, top, right, bottom, r);
        return r;
    }

    protected void getBufferDrawRect(final int left, final int top,
                                     final int right, final int bottom,
                                     @NonNull final Rect rect) {
        rect.left = (int) ((left - scrollPosition.x) * mFontWidth);
        rect.top = (int) ((top - scrollPosition.y) * mFontHeight);
        rect.right = (int) ((right - scrollPosition.x) * mFontWidth);
        rect.bottom = (int) ((bottom - scrollPosition.y) * mFontHeight);
    }

    protected Rect getBufferTextRect(final int left, final int top,
                                     final int right, final int bottom) {
        final Rect r = new Rect();
        getBufferTextRect(left, top, right, bottom, r);
        return r;
    }

    protected void getBufferTextRect(final int left, final int top,
                                     final int right, final int bottom,
                                     @NonNull final Rect rect) {
        rect.left = (int) Math.floor(left / mFontWidth + scrollPosition.x);
        rect.top = (int) Math.floor(top / mFontHeight + scrollPosition.y);
        rect.right = (int) Math.ceil(right / mFontWidth + scrollPosition.x);
        rect.bottom = (int) Math.ceil(bottom / mFontHeight + scrollPosition.y);
    }

    protected Rect getBufferDrawRectInc(@NonNull final Point first, @NonNull final Point last) {
        return getBufferDrawRectInc(first.x, first.y, last.x, last.y);
    }

    protected Rect getBufferDrawRectInc(final int x1, final int y1, final int x2, final int y2) {
        final Rect r = new Rect();
        getBufferDrawRect(Math.min(x1, x2), Math.min(y1, y2),
                Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, r);
        return r;
    }

    protected float getBufferDrawPosXF(final float x) {
        return (x - scrollPosition.x) * mFontWidth;
    }

    protected float getBufferDrawPosYF(final float y) {
        return (y - scrollPosition.y) * mFontHeight;
    }

    protected float getBufferTextPosXF(final float x) {
        return x / mFontWidth + scrollPosition.x;
    }

    protected float getBufferTextPosYF(final float y) {
        return y / mFontHeight + scrollPosition.y;
    }

    protected void getBufferTextPosF(final float x, final float y, @NonNull final PointF r) {
        r.x = getBufferTextPosXF(x);
        r.y = getBufferTextPosYF(y);
    }

    protected int getBufferTextPosX(final float x) {
        return (int) Math.floor(getBufferTextPosXF(x));
    }

    protected int getBufferTextPosY(final float y) {
        return (int) Math.floor(getBufferTextPosYF(y));
    }

    protected void getBufferTextPos(final float x, final float y, @NonNull final Point r) {
        r.x = getBufferTextPosX(x);
        r.y = getBufferTextPosY(y);
    }

    public int getCols() {
        return (int) (getWidth() / mFontWidth);
    }

    public int getRows() {
        return (int) (getHeight() / mFontHeight);
    }

    public boolean isOnScreen(float x, float y) {
        x -= scrollPosition.x;
        y -= scrollPosition.y;
        return x >= 0 && x < getCols() && y >= 0 && y < getRows();
    }

    public void doScrollTextCenterTo(final float x, final float y) {
        doScrollTo(x - (float) getCols() / 2, y - (float) getRows() / 2);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        adjustSelectionPopup();
        if (appTextScroller.computeScrollOffset() &&
                consoleInput != null && consoleInput.consoleOutput != null) {
            consoleInput.consoleOutput.vScroll(appTextScroller.getDistanceY());
        }
    }

    public void applyCharAttrs() {
        fontProvider.setPaint(fgPaint, (charAttrs.bold ? Typeface.BOLD : 0) |
                (charAttrs.italic ? Typeface.ITALIC : 0));
        fgPaint.setColor(charAttrs.fgColor);
        fgPaint.setUnderlineText(charAttrs.underline);
        fgPaint.setStrikeThruText(charAttrs.crossed);
//        fgPaint.setShadowLayer(1, 0, 0, fgColor);
        bgPaint.setColor(charAttrs.bgColor);
    }

    public void setConsoleInput(@NonNull final ConsoleInput consoleInput) {
        this.consoleInput = consoleInput;
        this.consoleInput.addOnInvalidateSink(this);
        this.consoleInput.addOnBufferScroll(this);
        resizeBuffer();
    }

    public void unsetConsoleInput() {
        consoleInput.removeOnBufferScroll(this);
        consoleInput.removeOnInvalidateSink(this);
        consoleInput = null;
    }

    @Nullable
    public String getSelectedText() {
        if (consoleInput == null) return null;
        final ConsoleScreenSelection s = selection.getDirect();
        final StringBuilder sb = new StringBuilder();
        final ConsoleScreenBuffer.BufferTextRange v = new ConsoleScreenBuffer.BufferTextRange();
        int r;
        if (s.first.y == s.last.y) {
            r = consoleInput.currScrBuf.getChars(s.first.x, s.first.y, s.last.x - s.first.x + 1, v);
            if (r >= 0) sb.append(v.toString().trim());
        } else if (selection.isRectangular) {
            for (int y = s.first.y; y <= s.last.y - 1; y++) {
                r = consoleInput.currScrBuf.getChars(s.first.x, y, s.last.x - s.first.x + 1, v);
                if (r >= 0) sb.append(v.toString().replaceAll(" *$", ""));
                sb.append('\n');
            }
            r = consoleInput.currScrBuf.getChars(0, s.last.y, s.last.x + 1, v);
            if (r >= 0) sb.append(v.toString().replaceAll(" *$", ""));
        } else {
            r = consoleInput.currScrBuf.getChars(s.first.x, s.first.y, getCols() - s.first.x, v);
            if (r >= 0) sb.append(v.toString().replaceAll(" *$", ""));
            sb.append('\n');
            for (int y = s.first.y + 1; y <= s.last.y - 1; y++) {
                r = consoleInput.currScrBuf.getChars(0, y, getCols(), v);
                if (r >= 0) sb.append(v.toString().replaceAll(" *$", ""));
                sb.append('\n');
            }
            r = consoleInput.currScrBuf.getChars(0, s.last.y, s.last.x + 1, v);
            if (r >= 0) sb.append(v.toString().replaceAll(" *$", ""));
        }
        final String result = sb.toString();
        if (result.isEmpty()) return null;
        return result;
    }

    @Nullable
    public Bitmap makeThumbnail(int w, int h) {
        if (getWidth() <= 0 || getHeight() <= 0)
            return null;
        final float s = Math.min((float) w / getWidth(), (float) h / getHeight());
        w = Math.max((int) (getWidth() * s), 1);
        h = Math.max((int) (getHeight() * s), 1);
        final Bitmap r = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Canvas c = new Canvas(r);
        c.scale(s, s);
        drawContent(c);
        return r;
    }

    protected boolean wasAltBuf = false;

    @Override
    public void onInvalidateSink(@Nullable final Rect rect) {
        if (consoleInput != null) {
            if (!wasAltBuf && consoleInput.isAltBuf())
                invalidateScroll();
            wasAltBuf = consoleInput.isAltBuf();
        }
        if (rect == null) ViewCompat.postInvalidateOnAnimation(this);
        else invalidate(rect);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resizeBuffer();
    }

    protected int mButtons = 0;
    protected final Point mXY = new Point();

    protected int translateButtons(int buttons) {
        if ((buttons & MotionEvent.BUTTON_BACK) != 0)
            buttons |= MotionEvent.BUTTON_SECONDARY;
        if ((buttons & MotionEvent.BUTTON_STYLUS_PRIMARY) != 0)
            buttons |= MotionEvent.BUTTON_PRIMARY;
        if ((buttons & MotionEvent.BUTTON_STYLUS_SECONDARY) != 0)
            buttons |= MotionEvent.BUTTON_SECONDARY;
        return buttons;
    }

    public static boolean isMouseEvent(@Nullable final MotionEvent event) {
        return event != null &&
                event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER;
    }

    public boolean isMouseSupported() {
        return consoleInput != null && consoleInput.consoleOutput != null &&
                consoleInput.consoleOutput.isMouseSupported();
    }

    protected static class SubGesture {
        protected float x = 0F;
        protected float y = 0F;
        protected float dx = 0F;
        protected float dy = 0F;

        protected void init(@NonNull final MotionEvent event) {
            x = event.getX();
            y = event.getY();
            dx = 0F;
            dy = 0F;
        }

        protected void onMove(@NonNull final MotionEvent event) {
            dx = event.getX() - x;
            dy = event.getY() - y;
            x = event.getX();
            y = event.getY();
        }
    }

    protected boolean inGesture = false;
    protected SubGesture selectionGesture = new SubGesture();

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                inGesture = true;
                selectionGesture.init(event);
                wasAppTextScrolling = !appTextScroller.isFinished();
                appTextScroller.forceFinished(true);
                adjustSelectionPopup();
                ViewCompat.postInvalidateOnAnimation(this);
                break;
        }
        if (selectionMode) { // Possibly no gestures here
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    setCurrentSelectionMarker(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (selectionMarker != null) {
                        if (!isOnScreen(selectionMarker.x, selectionMarker.y) &&
                                event.getEventTime() - event.getDownTime()
                                        < SELECTION_MOVE_START_DELAY)
                            return true;
                        selectionGesture.onMove(event);
                        float smx = getBufferDrawPosXF(selectionMarker.x);
                        float smy = getBufferDrawPosYF(selectionMarker.y);
                        smx = MathUtils.clamp((int) (smx), 0, getWidth() - 1);
                        smy = MathUtils.clamp((int) (smy), 0, getHeight() - 1);
                        smx = MathUtils.clamp((int) (smx + selectionGesture.dx), 0,
                                getWidth() - 1);
                        smy = MathUtils.clamp((int) (smy + selectionGesture.dy), 0,
                                getHeight() - 1);
                        selectionMarker.x = getBufferTextPosXF(smx);
                        selectionMarker.y = getBufferTextPosYF(smy);
                        if (selectionMarker == selectionMarkerExpr) {
                            doAutoSelect();
                        } else {
                            selection.first.set((int) Math.floor(selectionMarkerFirst.x),
                                    (int) Math.floor(selectionMarkerFirst.y));
                            selection.last.set((int) Math.floor(selectionMarkerLast.x),
                                    (int) Math.floor(selectionMarkerLast.y));
                        }
                        ViewCompat.postInvalidateOnAnimation(this);
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (selectionMarker != null) {
                        if (!isOnScreen(selectionMarker.x, selectionMarker.y))
                            doScrollTextCenterTo(selectionMarker.x, selectionMarker.y);
                        unsetCurrentSelectionMarker();
                        inGesture = false;
                        adjustSelectionPopup();
                        ViewCompat.postInvalidateOnAnimation(this);
                        return true;
                    }
                    break;
            }
            if (selectionMarker != null) return true;
        } else if (mouseMode) { // No gestures here
            if (consoleInput != null && consoleInput.consoleOutput != null) {
                final int x = getBufferTextPosX(MathUtils.clamp((int) event.getX(), 0, getWidth() - 1));
                final int y = getBufferTextPosY(MathUtils.clamp((int) event.getY(), 0, getHeight() - 1));
                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        final int buttons;
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                            buttons = MotionEvent.BUTTON_PRIMARY;
                        } else {
                            final int bs = translateButtons(event.getButtonState());
                            buttons = bs & ~mButtons;
                            if (buttons == 0) break;
                            mButtons = bs;
                        }
                        consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.PRESS, buttons, x, y);
                        mXY.x = x;
                        mXY.y = y;
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        if (mXY.x == x && mXY.y == y) break;
                        final int buttons = event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                                ? MotionEvent.BUTTON_PRIMARY
                                : translateButtons(event.getButtonState());

                        consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.MOVE, buttons, x, y);
                        mXY.x = x;
                        mXY.y = y;
                        break;
                    }
                    case MotionEvent.ACTION_UP: {
                        final int buttons;
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                            buttons = MotionEvent.BUTTON_PRIMARY;
                        } else {
                            final int bs = translateButtons(event.getButtonState());
                            buttons = mButtons & ~bs;
                            if (buttons == 0) break;
                            mButtons = bs;
                        }
                        consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.RELEASE, buttons, x, y);
                        break;
                    }
                }
            }
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            if (event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER)
                mButtons = translateButtons(event.getButtonState());
        }
        final boolean ret = super.onTouchEvent(event);
        if (action == MotionEvent.ACTION_UP) {
            if (event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER)
                mButtons = translateButtons(event.getButtonState());
            unsetCurrentSelectionMarker();
            inGesture = false;
            adjustSelectionPopup();
            ViewCompat.postInvalidateOnAnimation(this);
        }
        return ret;
    }

    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if (selectionMode) {
            // Empty now
        } else if (mouseMode) {
            if (consoleInput != null && consoleInput.consoleOutput != null) {
                final int x = getBufferTextPosX(MathUtils.clamp((int) event.getX(), 0, getWidth() - 1));
                final int y = getBufferTextPosY(MathUtils.clamp((int) event.getY(), 0, getHeight() - 1));
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_HOVER_MOVE: {
                        if (mXY.x == x && mXY.y == y) break;
                        final int buttons = translateButtons(event.getButtonState());
                        consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.MOVE, buttons, x, y);
                        mXY.x = x;
                        mXY.y = y;
                        break;
                    }
                    case MotionEvent.ACTION_SCROLL: {
                        final float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                        if (vScroll != 0)
                            consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.VSCROLL, (int) vScroll, x, y);
                        break;
                    }
                }
            }
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private float prevAppTextVScrollPos = 0;

    private int getNextAppTextY(final float dY) {
        prevAppTextVScrollPos += dY;
        final int lines = (int) prevAppTextVScrollPos;
        prevAppTextVScrollPos -= lines;
        return lines;
    }

    final private ScrollerEx appTextScroller;

    private boolean wasAppTextScrolling = false;

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        if (consoleInput != null && consoleInput.consoleOutput != null &&
                consoleInput.isAltBuf() && getRows() >= consoleInput.currScrBuf.getHeight()) {
            if (scrollPosition.y != 0) {
                scrollPosition.y = 0;
                execOnScroll();
                ViewCompat.postInvalidateOnAnimation(this);
            }
            consoleInput.consoleOutput.vScroll(getNextAppTextY(distanceY / scrollScale.y));
            return super.onScroll(e1, e2, distanceX, 0);
        }
        return super.onScroll(e1, e2, distanceX, distanceY);
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                           final float velocityX, final float velocityY) {
        if (consoleInput != null && consoleInput.consoleOutput != null &&
                consoleInput.isAltBuf() && getRows() >= consoleInput.currScrBuf.getHeight()) {
            if (scrollPosition.y != 0) {
                scrollPosition.y = 0;
                execOnScroll();
                ViewCompat.postInvalidateOnAnimation(this);
            }
            appTextScroller.forceFinished(true);
            appTextScroller.fling(0, -(int) (velocityY / scrollScale.y));
            return super.onFling(e1, e2, velocityX, 0);
        }
        return super.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e) {
        if (wasAppTextScrolling)
            return true; // Just stop sending fling scrolling escapes to an application...
        final int x = getBufferTextPosX(MathUtils.clamp((int) e.getX(), 0, getWidth() - 1));
        final int y = getBufferTextPosY(MathUtils.clamp((int) e.getY(), 0, getHeight() - 1));
        if (isMouseSupported()) {
            final int buttons;
            if (e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                buttons = MotionEvent.BUTTON_PRIMARY;
            } else {
                buttons = mButtons & ~translateButtons(e.getButtonState());
            }
            if (buttons != 0) {
                consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.PRESS, buttons, x, y);
                consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.RELEASE, buttons, x, y);
            }
        } else {
            setSelectionMode(true);
            setSelectionModeIsExpr(true);
            getCenterText(x, y, selectionMarkerExpr);
            doAutoSelect();
            ViewCompat.postInvalidateOnAnimation(this);
        }
        return super.onSingleTapUp(e);
    }

    protected void doAutoSelect() {
        if (consoleInput != null) {
            final int[] bb = new int[2];
            final int px = (int) Math.floor(selectionMarkerExpr.x);
            final int py = (int) Math.floor(selectionMarkerExpr.y);
            final ConsoleScreenBuffer.BufferTextRange chars =
                    new ConsoleScreenBuffer.BufferTextRange();
            if (consoleInput.currScrBuf.getChars(0, py, Integer.MAX_VALUE, chars) >= 0) {
                CharsAutoSelector.select(chars.text, chars.start, chars.start + chars.length,
                        ConsoleScreenBuffer.getCharIndex(chars.text, px, 0, true), bb);
                selection.first.set(ConsoleScreenBuffer.getCharPos(chars.text, 0, bb[0]), py);
                selection.last.set(ConsoleScreenBuffer.getCharPos(chars.text, 0, bb[1]), py);
                getCenterText(selection.first.x, selection.first.y, selectionMarkerFirst);
                getCenterText(selection.last.x, selection.last.y, selectionMarkerLast);
            }
        }
    }

    protected boolean scrollPtWithBuffer(@NonNull final Point pt,
                                         final int from, final int to, final int n) {
        if (pt.y >= from && pt.y <= to) {
            pt.y -= n;
            if (pt.y < from) {
                pt.y = from;
                return true;
            }
            return false;
        }
        if (pt.y <= from && pt.y >= to) {
            pt.y += n;
            if (pt.y > from) {
                pt.y = from;
                return true;
            }
            return false;
        }
        return false;
    }

    protected boolean scrollPtWithBuffer(@NonNull final PointF pt,
                                         final int from, final int to, final int n) {
        if (pt.y >= from && pt.y < to + 1) {
            pt.y -= n;
            if (pt.y < from) {
                pt.y += Math.ceil(from - pt.y);
                return true;
            }
            return false;
        }
        if (pt.y < from + 1 && pt.y >= to) {
            pt.y += n;
            if (pt.y >= from + 1) {
                pt.y += Math.ceil(from - pt.y);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public void onBufferScroll(final int from, final int to, final int n) {
        if (selection != null) {
            final boolean fob = scrollPtWithBuffer(selection.first, from, to, n);
            final boolean lob = scrollPtWithBuffer(selection.last, from, to, n);
            if (fob && lob) {
                setSelectionMode(false);
                selection = null;
            } else {
                scrollPtWithBuffer(selectionMarkerFirst, from, to, n);
                scrollPtWithBuffer(selectionMarkerLast, from, to, n);
                scrollPtWithBuffer(selectionMarkerExpr, from, to, n);
            }
        }
        if (mScroller.isFinished() && scrollFollowHistoryThreshold > 0F
                && scrollPosition.y < Math.min((float) -getRows() * scrollFollowHistoryThreshold,
                getBottomScrollLimit() - 0.5F)) {
            scrollPtWithBuffer(scrollPosition, from, to, n);
            invalidateScroll();
        }
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        drawContent(canvas);
        if (consoleInput != null) {
            drawSelection(canvas);
            if (mBlinkState && consoleInput.cursorVisibility) canvas.drawRect(getBufferDrawRect(
                    consoleInput.currScrBuf.getAbsPosX(),
                    consoleInput.currScrBuf.getAbsPosY()),
                    cursorPaint);
        }
        super.onDraw(canvas);
    }

    protected final float getSelectionMarkerDistance(@NonNull final MotionEvent event,
                                                     @NonNull final PointF marker,
                                                     final float r) {
        final float eX = event.getX();
        final float eY = event.getY();
        final float mX = MathUtils.clamp(getBufferDrawPosXF(marker.x), 0, getWidth() - 1);
        final float mY = MathUtils.clamp(getBufferDrawPosYF(marker.y), 0, getHeight() - 1);
        final float d = (float) Math.hypot(eX - mX, eY - mY);
        return (d > r) ? Float.POSITIVE_INFINITY : d;
    }

    protected void setCurrentSelectionMarker(@NonNull final MotionEvent event) {
        selectionMarker = null;
        scrollDisabled = mouseMode;
        if (selectionMode) {
            final float r = selectionPadSize / 2;
            if (selectionModeIsExpr) {
                if (getSelectionMarkerDistance(event, selectionMarkerExpr, r)
                        != Float.POSITIVE_INFINITY) {
                    selectionMarker = selectionMarkerExpr;
                    scrollDisabled = true;
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                return;
            }
            final float dF = getSelectionMarkerDistance(event, selectionMarkerFirst, r);
            final float dL = getSelectionMarkerDistance(event, selectionMarkerLast, r);
            if (dF < dL) {
                selectionMarker = selectionMarkerFirst;
                scrollDisabled = true;
                ViewCompat.postInvalidateOnAnimation(this);
                return;
            }
            if (dF > dL || dL != Float.POSITIVE_INFINITY) {
                selectionMarker = selectionMarkerLast;
                scrollDisabled = true;
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    protected void unsetCurrentSelectionMarker() {
        if (selectionMarker != null) {
            selectionMarker = null;
            scrollDisabled = mouseMode;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    protected void drawSelection(@NonNull final Canvas canvas) {
        if (selectionMode && selection != null) {
            final int selH = Math.abs(selection.last.y - selection.first.y) + 1;
            if (selH == 1 || selection.isRectangular) {
                canvas.drawRect(getBufferDrawRectInc(selection.first, selection.last), selectionPaint);
            } else if (selH >= 2) {
                final ConsoleScreenSelection s = selection.getDirect();
                canvas.drawRect(getBufferDrawRectInc(
                        s.first.x,
                        s.first.y,
                        ConsoleScreenBuffer.MAX_ROW_LEN - 1,
                        s.first.y
                ), selectionPaint);
                if (selH > 2) {
                    canvas.drawRect(getBufferDrawRectInc(
                            0,
                            s.first.y + 1,
                            ConsoleScreenBuffer.MAX_ROW_LEN - 1,
                            s.last.y - 1
                    ), selectionPaint);
                }
                canvas.drawRect(getBufferDrawRectInc(
                        0,
                        s.last.y,
                        s.last.x,
                        s.last.y
                ), selectionPaint);
            }
            if (selectionMarker != null) {
                if (selectionMarkerPtr != null) {
                    drawMarker(canvas, selectionMarkerPtr, selectionMarker, mFontHeight * 3);
                }
            } else if (!inGesture) {
                if (selectionMarkerPad != null) {
                    if (selectionModeIsExpr) {
                        drawMarker(canvas, selectionMarkerPad,
                                selectionMarkerExpr, selectionPadSize);
                    } else {
                        drawMarker(canvas, selectionMarkerPad,
                                selectionMarkerFirst, selectionPadSize);
                        drawMarker(canvas, selectionMarkerPad,
                                selectionMarkerLast, selectionPadSize);
                    }
                }
            }
        }
    }

    protected final void drawMarker(@NonNull final Canvas canvas, @NonNull final Drawable drawable,
                                    @NonNull final PointF pos, final float size) {
        final float oPosX = getBufferDrawPosXF(pos.x);
        final float oPosY = getBufferDrawPosYF(pos.y);
        final float posX = MathUtils.clamp(oPosX, 0, getWidth() - 1);
        final float posY = MathUtils.clamp(oPosY, 0, getHeight() - 1);
        canvas.save();
        canvas.translate(posX - size / 2, posY - size / 2);
        canvas.clipRect(0, 0, size, size);
        drawable.setBounds(0, 0, (int) size, (int) size);
        drawable.draw(canvas);
        canvas.restore();
        if (selectionMarkerOOB != null && (oPosX != posX || oPosY != posY)) {
            canvas.save();
            final float sX = selectionMarkerOOB.getIntrinsicWidth();
            final float sY = selectionMarkerOOB.getIntrinsicHeight();
            canvas.translate(MathUtils.clamp(posX, sX / 2, getWidth() - sX / 2),
                    MathUtils.clamp(posY, sY / 2, getHeight() - sY / 2));
            canvas.rotate((float) Math.toDegrees(Math.atan2(oPosY - posY, oPosX - posX)));
            canvas.translate(-sX / 2, -sY / 2);
            canvas.clipRect(0, 0, sX, sY);
            selectionMarkerOOB.setBounds(0, 0, (int) sX, (int) sY);
            selectionMarkerOOB.draw(canvas);
            canvas.restore();
        }
    }

    protected final boolean isAllSpaces(@NonNull final ConsoleScreenBuffer.BufferRun s) {
        final int end = s.start + s.length;
        for (int i = s.start; i < end; ++i) if (s.text[i] != ' ') return false;
        return true;
    }

    protected final Rect _draw_textRect = new Rect();
    protected final ConsoleScreenBuffer.BufferRun _draw_run =
            new ConsoleScreenBuffer.BufferRun();

    protected void drawContent(@NonNull final Canvas canvas) {
        if (consoleInput != null) {
//            canvas.drawColor(charAttrs.bgColor);
            final float vDivBuf = getBufferDrawPosYF(0) - 1;
            final float vDivBottom = getBufferDrawPosYF(consoleInput.currScrBuf.getHeight()) - 1;
            final float hDiv = getBufferDrawPosXF(consoleInput.currScrBuf.getWidth()) - 1;
            getBufferTextRect(0, 0, getWidth(), getHeight(), _draw_textRect);
            for (int j = _draw_textRect.top; j < _draw_textRect.bottom; j++) {
                final float strTop = getBufferDrawPosYF(j);
                final float strBottom = getBufferDrawPosYF(j + 1)
                        + 1; // fix for old phones rendering glitch
                int i = _draw_textRect.left;
                i -= consoleInput.currScrBuf.initCharsRun(i, j, _draw_run);
                while (i < _draw_textRect.right) {
                    final float strFragLeft = getBufferDrawPosXF(i);
                    final int sr =
                            consoleInput.currScrBuf.getCharsRun(i, j, _draw_textRect.right,
                                    _draw_run);
                    if (sr < 0) {
                        ConsoleScreenBuffer.decodeAttrs(consoleInput.currScrBuf.defaultAttrs,
                                charAttrs);
                        applyCharAttrs();
                        canvas.drawRect(strFragLeft, strTop, getWidth(), strBottom, bgPaint);
                        if (charAttrs.blinking) drawDrawable(canvas, attrMarkupBlinking,
                                (int) strFragLeft, (int) strTop, getWidth(), (int) strBottom);
                        break;
                    }
                    ConsoleScreenBuffer.decodeAttrs(_draw_run.attrs, charAttrs);
                    applyCharAttrs();
                    final float strFragRight = getBufferDrawPosXF(i + sr);
                    if (sr > 0) {
                        // background is only for non-zero length glyphs
                        // see https://en.wikipedia.org/wiki/Combining_character
                        canvas.drawRect(strFragLeft, strTop, strFragRight, strBottom, bgPaint);
                        if (charAttrs.blinking) drawDrawable(canvas, attrMarkupBlinking,
                                (int) strFragLeft, (int) strTop, (int) strFragRight, (int) strBottom);
                    }
                    if (!charAttrs.invisible && charAttrs.fgColor != charAttrs.bgColor &&
                            _draw_run.length > 0 && !isAllSpaces(_draw_run)) {
                        fgPaint.setTextScaleX(1F);
                        if (_draw_run.glyphWidth > 1)
                            fgPaint.setTextScaleX(
                                    mFontWidth * sr /
                                            fgPaint.measureText(_draw_run.text,
                                                    _draw_run.start, _draw_run.length)
                            );
                        canvas.drawText(_draw_run.text, _draw_run.start, _draw_run.length,
                                strFragLeft, strTop - fgPaint.ascent(), fgPaint);
                    }
                    i += sr;
                }
                _draw_run.reinit();
            }
            if (paddingMarkup != null) {
                if (vDivBottom < getHeight())
                    drawDrawable(canvas, paddingMarkup, 0, (int) vDivBottom,
                            getWidth(), getHeight());
                if (hDiv < getWidth())
                    drawDrawable(canvas, paddingMarkup, (int) hDiv, 0,
                            getWidth(), Math.min(getHeight(), (int) vDivBottom));
            }
            canvas.drawLine(0, vDivBottom, getWidth(), vDivBottom, paddingMarkupPaint);
            canvas.drawLine(0, vDivBuf, getWidth(), vDivBuf, paddingMarkupPaint);
            canvas.drawLine(hDiv, 0, hDiv, getHeight(), paddingMarkupPaint);
        }
    }

    protected final void drawDrawable(@NonNull final Canvas canvas, @Nullable final Drawable drawable,
                                      final int left, final int top, final int right, final int bottom) {
        if (drawable == null) return;
        int xOff = 0;
        int yOff = 0;
        int xSize = 0;
        int ySize = 0;
        if (drawable instanceof BitmapDrawable) {
            final BitmapDrawable d = (BitmapDrawable) drawable;
            if (d.getTileModeX() != Shader.TileMode.CLAMP) {
                xSize = d.getIntrinsicWidth() * 2;
                xOff = (int) (scrollPosition.x * mFontWidth) % xSize;
            }
            if (d.getTileModeY() != Shader.TileMode.CLAMP) {
                ySize = d.getIntrinsicHeight() * 2;
                yOff = (int) (scrollPosition.y * mFontHeight) % ySize;
            }
        }
        canvas.save();
        canvas.clipRect(left, top, right, bottom);
        canvas.translate(-xOff, -yOff);
        drawable.setBounds(left - xSize, top - ySize,
                right + xSize, bottom + ySize);
        drawable.draw(canvas);
        canvas.restore();
    }
}
