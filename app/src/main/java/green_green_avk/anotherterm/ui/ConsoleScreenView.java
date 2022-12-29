package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import android.os.Build;
import android.os.Message;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.CheckResult;
import androidx.annotation.IntRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.math.MathUtils;
import androidx.core.view.ViewCompat;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

import green_green_avk.anotherterm.AnsiColorProfile;
import green_green_avk.anotherterm.ConsoleInput;
import green_green_avk.anotherterm.ConsoleOutput;
import green_green_avk.anotherterm.ConsoleScreenBuffer;
import green_green_avk.anotherterm.ConsoleScreenCharAttrs;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.CharsAutoSelector;
import green_green_avk.anotherterm.utils.CharsFinder;
import green_green_avk.anotherterm.utils.WeakHandler;

public class ConsoleScreenView extends ScrollableView
        implements ConsoleInput.OnInvalidateSink, ConsoleInput.OnBufferScroll {

    public interface OnStateChange {
        void onTerminalAreaResize(int width, int height);

        void onSelectionModeChange(boolean mode);

        /**
         * @param fontSize [px]
         */
        void onFontSizeChange(float fontSize);
    }

    public static class State {
        private PointF scrollPosition = null;
        private boolean resizeBufferXOnUi = true;
        private boolean resizeBufferYOnUi = true;
        private boolean appHScrollEnabled = false;

        public void save(@NonNull final ConsoleScreenView v) {
            scrollPosition = v.scrollPosition;
            resizeBufferXOnUi = v.resizeBufferXOnUi;
            resizeBufferYOnUi = v.resizeBufferYOnUi;
            appHScrollEnabled = v.appHScrollEnabled;
        }

        public void apply(@NonNull final ConsoleScreenView v) {
            if (scrollPosition == null)
                return;
            v.scrollPosition.set(scrollPosition);
            v.resizeBufferXOnUi = resizeBufferXOnUi;
            v.resizeBufferYOnUi = resizeBufferYOnUi;
            v.appHScrollEnabled = appHScrollEnabled;
            v.execOnScroll();
        }
    }

    protected static final int MSG_BLINK = 0;
    protected static final int INTERVAL_BLINK = 500; // ms
    protected static final long SELECTION_MOVE_START_DELAY = 200; // ms
    protected static final int AUTOSELECT_LINES_MAX = 128;
    protected static final int SEARCH_PATTERN_CELLS_MAX = 1024;
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
    protected Drawable selectionWrappedLineMarker = null;
    protected Drawable paddingMarkup = null;

    protected int terminalScrollOffset = 0; // px
    @LayoutRes
    protected int terminalScrollHorizontalLayout = R.layout.terminal_h_scrollbar;
    @LayoutRes
    protected int terminalScrollVerticalLayout = R.layout.terminal_v_scrollbar;

    @NonNull
    protected AnsiColorProfile colorProfile = ConsoleScreenCharAttrs.DEFAULT_COLOR_PROFILE;
    @NonNull
    protected FontProvider fontProvider = DefaultConsoleFontProvider.getInstance();
    protected float mFontSize = 16F; // px
    protected float mFontWidth;
    protected float mFontHeight;
    protected int keyHeightDp = 0;
    protected int popupOpacity = 0x44;
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

    public OnStateChange onStateChange = null;

    private boolean mBlinkState = true;
    protected boolean hasVisibleBlinking = false;
    protected boolean isChanging = false;

    protected static final int[] noneSelectionModeState = new int[0];
    protected static final int[] linesSelectionModeState = new int[]{R.attr.state_select_lines};
    protected static final int[] rectSelectionModeState = new int[]{R.attr.state_select_rect};
    protected static final int[] exprSelectionModeState = new int[]{R.attr.state_select_expr};

    protected static final int[] searchCaseSState = new int[0];
    protected static final int[] searchCaseIState = new int[]{android.R.attr.state_checked};

    protected class SelectionPopup {
        protected int keySize = 0; // px
        protected final int[] parentPos = new int[2];
        protected final PopupWindow window;
        protected AlertDialog auxDialog = null;
        protected final Drawable[] backgrounds;
        protected final Point pos = new Point(0, 0);
        protected final ImageView wSelMode;
        protected final TextView wSearch;
        protected final ImageView wCase;

        protected boolean searchCaseI = false;

        protected class WrapperView extends FrameLayout {
            protected WrapperView(@NonNull final Context context) {
                super(context);
            }

            @Override
            protected void onLayout(final boolean changed, final int left, final int top,
                                    final int right, final int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                if (changed) {
                    calcPos();
                    window.update(pos.x, pos.y, -1, -1);
                }
            }
        }

        private CharSequence searchHint = null;

        @NonNull
        private CharSequence getSearchHint() {
            if (searchHint == null) {
                final Spannable s = new SpannableString(getContext().getResources()
                        .getText(R.string.hint_text_to_search___));
                s.setSpan(new InlineImageSpan(getContext(), R.drawable.ic_mglass).useTextColor(),
                        0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                searchHint = s;
            }
            return searchHint;
        }

        protected void setPopupBackgroundAlpha(@IntRange(from = 0, to = 255) final int v) {
            for (final Drawable bg : backgrounds)
                UiUtils.setBackgroundAlpha(bg, v, Color.BLACK);
        }

        @NonNull
        private Drawable[] findPopupBackgrounds(@NonNull final ViewGroup vv) {
            final List<Drawable> r = new ArrayList<>();
            for (final View v : UiUtils.getIterable(vv)) {
                if ("popup_background_container".equals(v.getTag()))
                    r.add(v.getBackground());
            }
            return r.toArray(new Drawable[0]);
        }

        {
            final ViewGroup d = new WrapperView(getContext());
            d.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            inflate(getContext(), R.layout.terminal_select_search_popup, d);
            backgrounds = findPopupBackgrounds(d);
            window = new PopupWindow(d, WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT, false);
            window.setSplitTouchEnabled(true);
            window.setAnimationStyle(android.R.style.Animation_Dialog);
            final View cv = getContentView();
            wSelMode = cv.findViewById(R.id.b_select_mode);
            wSearch = cv.findViewById(R.id.f_search);
            wCase = cv.findViewById(R.id.b_case);
            wSearch.setHint(getSearchHint());
            refresh();
            cv.findViewById(R.id.b_close)
                    .setOnClickListener(v -> setSelectionMode(false));
            wSelMode.setOnClickListener(v -> {
                if (getSelectionModeIsExpr()) setSelectionModeIsExpr(false);
                else {
                    if (getSelectionIsRect()) setSelectionModeIsExpr(true);
                    setSelectionIsRect(!getSelectionIsRect());
                    onSelectionChanged();
                }
                refresh();
            });
            cv.findViewById(R.id.b_select_all)
                    .setOnClickListener(v -> {
                        setSelectionMode(true);
                        setSelectionModeIsExpr(false);
                        selectAll();
                        setSelectionIsRect(false);
                        onSelectionChanged();
                        invalidateSelectionUi(false);
                    });
            cv.findViewById(R.id.b_copy)
                    .setOnClickListener(v -> UiUtils.toClipboard(getContext(), getSelectedText()));
            cv.findViewById(R.id.b_put)
                    .setOnClickListener(v -> {
                        if (consoleInput == null || consoleInput.consoleOutput == null) return;
                        final String s = getSelectedText();
                        if (s != null) consoleInput.consoleOutput.paste(s);
                    });
            cv.findViewById(R.id.b_web)
                    .setOnClickListener(v -> {
                        final String s = getSelectedText();
                        if (s == null) {
                            Toast.makeText(getContext(), R.string.msg_nothing_to_search,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        UiUtils.tryWebSearch(getContext(), s);
                    });
            cv.findViewById(R.id.b_share)
                    .setOnClickListener(v ->
                            UiUtils.sharePlainText((Activity) getContext(), getSelectedText()));
            cv.findViewById(R.id.b_scratchpad)
                    .setOnClickListener(v ->
                            UiUtils.toScratchpad(getContext(), getSelectedText()));
            wSearch.setOnClickListener(v -> {
                final EditText et = new AppCompatEditText(getContext());
                et.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_NORMAL);
                et.setHint(getSearchHint());
                et.setText(wSearch.getText());
                auxDialog = new AlertDialog.Builder(getContext())
                        .setOnDismissListener(dialog -> auxDialog = null)
                        .setView(et)
                        .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                setSearchPattern(et.getText()))
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                        })
                        .show();
            });
            final CharsFinder.BufferView bufferView = new CharsFinder.BufferView() {
                private final CharBuffer emptyBuffer = CharBuffer.allocate(0);

                @Override
                @NonNull
                public CharsFinder.LineView get(final int y) {
                    if (consoleInput == null)
                        throw new IndexOutOfBoundsException();
                    final ConsoleScreenBuffer.BufferTextRange v =
                            new ConsoleScreenBuffer.BufferTextRange();
                    final int r = consoleInput.currScrBuf.getChars(0, y,
                            consoleInput.currScrBuf.getWidth(), v);
                    if (r < 0)
                        return new CharsFinder.LineView(emptyBuffer, true);
                    final CharBuffer line = v.toBuffer();
                    int i;
                    for (i = line.length() - 1; i >= 0 && line.charAt(i) == ' '; i--) ;
                    line.limit(line.position() + i + 1);
                    return new CharsFinder.LineView(line,
                            consoleInput.currScrBuf.isLineWrapped(y));
                }

                @Override
                public int getTop() {
                    if (consoleInput == null)
                        return 0;
                    return -consoleInput.currScrBuf.getScrollableHeight();
                }

                @Override
                public int getBottom() {
                    if (consoleInput == null)
                        return 0;
                    return consoleInput.currScrBuf.getHeight();
                }
            };
            cv.findViewById(R.id.b_case)
                    .setOnClickListener(v -> {
                        searchCaseI = !searchCaseI;
                        ((ImageView) v).setImageState(
                                searchCaseI ? searchCaseIState : searchCaseSState, true);
                    });
            cv.findViewById(R.id.b_search_up)
                    .setOnClickListener(v -> {
                        if (consoleInput == null || selection == null) return;
                        final String pattern = getSearchPattern();
                        if (pattern.isEmpty()) {
                            Toast.makeText(getContext(),
                                    R.string.msg_search_field_is_empty,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final CharsFinder cf = new CharsFinder(bufferView, pattern, searchCaseI);
                        final ConsoleScreenSelection s = selection.getDirect();
                        final int i = consoleInput.currScrBuf
                                .getCharIndex(s.first.y, s.first.x, 0, false);
                        if (!(cf.setPos(i, s.first.y, false) && cf.searchUp())) {
                            Toast.makeText(getContext(),
                                    R.string.msg_search_top_reached,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectSearchResult(cf);
                    });
            cv.findViewById(R.id.b_search_down)
                    .setOnClickListener(v -> {
                        if (consoleInput == null || selection == null) return;
                        final String pattern = getSearchPattern();
                        if (pattern.isEmpty()) {
                            Toast.makeText(getContext(),
                                    R.string.msg_search_field_is_empty,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        final CharsFinder cf = new CharsFinder(bufferView, pattern, searchCaseI);
                        final ConsoleScreenSelection s = selection.getDirect();
                        final int i = consoleInput.currScrBuf
                                .getCharIndex(s.last.y, s.last.x, 0, false);
                        if (!(cf.setPos(i, s.last.y, true) && cf.searchDown())) {
                            Toast.makeText(getContext(),
                                    R.string.msg_search_bottom_reached,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectSearchResult(cf);
                    });
        }

        protected void selectSearchResult(@NonNull final CharsFinder cf) {
            final Point first = cf.getFirst();
            selection.first.set(consoleInput.currScrBuf
                    .getCharPos(first.y, 0, first.x), first.y);
            final Point last = cf.getLast();
            final int lastX = consoleInput.currScrBuf
                    .getCharPos(last.y, 0, last.x);
            // What if it is a zero-screen-length match?
            selection.last.set(lastX > 0 ? lastX - 1 : 0, last.y);
            setSelectionIsRect(false);
            setSelectionModeIsExpr(false);
            invalidateSelectionUi(true);
            if (!isOnScreen(selectionMarkerFirst.x, selectionMarkerFirst.y))
                doScrollTextCenterTo(selectionMarkerFirst);
        }

        protected void calcPos() {
            getLocationInWindow(parentPos);
            final int sx = window.getContentView().getWidth();
            final int sy = window.getContentView().getHeight();
            final ConsoleScreenSelection s = selection.getDirect();
            int x = (int) getBufferDrawPosXF(
                    (s.first.x + s.last.x + 1) / 2f) - sx / 2;
            x = MathUtils.clamp(x, 0, getWidth() - sx);
            int paddingT = 0;
            int paddingB = 0;
            int y = (int) getBufferDrawPosYF(s.first.y) - sy;
            if (y < 0) {
                y = (int) getBufferDrawPosYF(s.last.y + 1);
                paddingT = keySize;
            }
            if (y > getHeight() - sy) {
                y = (int) (getBufferDrawPosYF(
                        (s.first.y + s.last.y + 1) / 2f)) - sy / 2;
                paddingB = keySize;
            }
            y = MathUtils.clamp(y, paddingT, getHeight() - sy - paddingB);
            pos.x = parentPos[0] + x;
            pos.y = parentPos[1] + y;
        }

        protected void setSizeDp(final float v) {
            keySize = (int) (getResources().getDisplayMetrics().density * v);
            final View cv = getContentView();
            final int rows = (cv instanceof LinearLayout &&
                    ((LinearLayout) cv).getOrientation() == LinearLayout.VERTICAL) ?
                    Math.max(((ViewGroup) cv).getChildCount(), 1) : 1;
            cv.getLayoutParams().height = keySize * rows;
            cv.requestLayout();
        }

        @CheckResult
        @NonNull
        protected String getSearchPattern() {
            final CharSequence v = wSearch.getText();
            return v != null ? v.toString() : "";
        }

        protected void setSearchPattern(@Nullable final CharSequence v) {
            wSearch.setText(v == null ? "" : v);
        }

        protected void refresh() {
            final int[] st;
            if (getSelectionModeIsExpr()) st = exprSelectionModeState;
            else if (getSelectionIsRect()) st = rectSelectionModeState;
            else st = linesSelectionModeState;
            wSelMode.setImageState(st, true);
        }

        @CheckResult
        protected View getContentView() {
            return ((ViewGroup) window.getContentView()).getChildAt(0);
        }

        protected void show() {
            if (selection != null && !window.isShowing()) {
                calcPos();
                window.showAtLocation(ConsoleScreenView.this, Gravity.NO_GRAVITY,
                        pos.x, pos.y);
            }
            refresh();
        }

        protected void hide() {
            if (auxDialog != null)
                auxDialog.dismiss();
            window.dismiss();
        }
    }

    public ConsoleScreenView(final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, R.attr.consoleScreenViewStyle);
    }

    public ConsoleScreenView(final Context context, @Nullable final AttributeSet attrs,
                             final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        appTextScroller = new ScrollerEx(context);
        init(context, attrs, defStyleAttr, R.style.AppConsoleScreenViewStyle);
        terminalScrollbars = new TerminalScrollbars();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConsoleScreenView(final Context context, @Nullable final AttributeSet attrs,
                             final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        appTextScroller = new ScrollerEx(context);
        init(context, attrs, defStyleAttr, defStyleRes);
        terminalScrollbars = new TerminalScrollbars();
    }

    protected void init(final Context context, @Nullable final AttributeSet attrs,
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
            selectionWrappedLineMarker = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ConsoleScreenView_selectionWrappedLineMarker, 0));
            terminalScrollOffset =
                    a.getDimensionPixelOffset(R.styleable.ConsoleScreenView_terminalScrollOffset,
                            terminalScrollOffset);
            terminalScrollHorizontalLayout =
                    a.getResourceId(R.styleable.ConsoleScreenView_terminalHorizontalScrollbarLayout,
                            terminalScrollHorizontalLayout);
            terminalScrollVerticalLayout =
                    a.getResourceId(R.styleable.ConsoleScreenView_terminalVerticalScrollbarLayout,
                            terminalScrollVerticalLayout);
            attrMarkupAlpha = (int) (a.getFloat(R.styleable.ConsoleScreenView_attrMarkupAlpha,
                    0.5f) * 255);
            paddingMarkup = AppCompatResources.getDrawable(context,
                    a.getResourceId(R.styleable.ConsoleScreenView_paddingMarkup, 0));
            paddingMarkupAlpha = (int) (a.getFloat(R.styleable.ConsoleScreenView_paddingMarkupAlpha,
                    0.2f) * 255);
        } finally {
            a.recycle();
        }

        paddingMarkup.setAlpha(paddingMarkupAlpha);

        cursorPaint.setColor(cursorColor);
        cursorPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        selectionPaint.setColor(selectionColor);
        selectionPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));

        if (selectionMarkerPtr != null)
            selectionMarkerPtr
                    .setColorFilter(selectionPaint.getColor(), PorterDuff.Mode.MULTIPLY);
        if (selectionMarkerPad != null)
            selectionMarkerPad
                    .setColorFilter(selectionPaint.getColor(), PorterDuff.Mode.MULTIPLY);
        if (selectionMarkerOOB != null)
            selectionMarkerOOB
                    .setColorFilter(selectionPaint.getColor(), PorterDuff.Mode.MULTIPLY);
        if (selectionWrappedLineMarker != null)
            selectionWrappedLineMarker
                    .setColorFilter(selectionPaint.getColor(), PorterDuff.Mode.MULTIPLY);

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
    public void setScreenSize(final int cols, final int rows) {
        setScreenSize(cols, rows, consoleInput.getMaxBufferHeight());
    }

    /**
     * Define fixed or variable (if dimension <= 0) screen size.
     */
    public void setScreenSize(int cols, int rows, final int bufferRows) {
        resizeBufferXOnUi = cols <= 0;
        resizeBufferYOnUi = rows <= 0;
        if (resizeBufferXOnUi)
            cols = getCols();
        if (resizeBufferYOnUi)
            rows = getRows();
        consoleInput.resize(cols, rows, bufferRows);
    }

    private final WeakHandler mHandler = new WeakHandler() {
        @Override
        public void handleMessage(@NonNull final Message msg) {
            switch (msg.what) {
                case MSG_BLINK:
                    mBlinkState = !mBlinkState;
                    if (consoleInput != null)
                        invalidateContent(hasVisibleBlinking ? null : getBufferDrawRect(
                                consoleInput.currScrBuf.getAbsPosX(),
                                consoleInput.currScrBuf.getAbsPosY()
                        ));
                    sendEmptyMessageDelayed(MSG_BLINK, INTERVAL_BLINK);
                    break;
            }
        }
    };

    public void unfreezeBlinking() {
        if (!mHandler.hasMessages(MSG_BLINK))
            mHandler.sendEmptyMessage(MSG_BLINK);
    }

    public void freezeBlinking() {
        mHandler.removeMessages(MSG_BLINK);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        adjustSelectionPopup();
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top,
                            final int right, final int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed)
            invalidateScroll();
    }

    @Override
    protected void onDetachedFromWindow() {
        selectionPopup.hide();
        freezeBlinking();
        super.onDetachedFromWindow();
    }

    @Override
    @CheckResult
    public float getTopScrollLimit() {
        return (consoleInput == null) ? 0F : Math.min(
                consoleInput.currScrBuf.getHeight() - getRows(),
                -consoleInput.currScrBuf.getScrollableHeight());
    }

    @Override
    @CheckResult
    public float getBottomScrollLimit() {
        return (consoleInput == null) ? 0F : Math.max(
                consoleInput.currScrBuf.getHeight() - getRows(),
                -consoleInput.currScrBuf.getScrollableHeight());
    }

    @Override
    @CheckResult
    public float getRightScrollLimit() {
        return (consoleInput == null) ? 0F : Math.max(
                consoleInput.currScrBuf.getWidth() - getCols(),
                0F);
    }

    @CheckResult
    @NonNull
    public float[] getCharSize(final float fontSize) {
        final float[] r = new float[2];
        final Paint p = new Paint();
        fontProvider.setPaint(p, Typeface.NORMAL);
        p.setTextSize(fontSize);
        r[1] = p.getFontSpacing();
        r[0] = p.measureText("A");
        return r;
    }

    protected void _setColorProfile(@NonNull final AnsiColorProfile v) {
        colorProfile = v;
    }

    public void setColorProfile(@NonNull final AnsiColorProfile v) {
        _setColorProfile(v);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    protected void applyFont() {
        final float[] charSize = getCharSize(mFontSize);
        mFontHeight = charSize[1];
        mFontWidth = charSize[0];
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

    /**
     * @return [px]
     */
    @CheckResult
    public float getFontSize() {
        return mFontSize;
    }

    /**
     * @param size [px]
     */
    public void setFontSize(final float size) {
        setFontSize(size, !isChanging);
    }

    /**
     * @param size   [px]
     * @param resize Do terminal screen resize
     */
    public void setFontSize(final float size, final boolean resize) {
        mFontSize = size;
        applyFont();
        if (resize) {
            resizeBuffer();
            ViewCompat.postInvalidateOnAnimation(this);
        }
        if (onStateChange != null)
            onStateChange.onFontSizeChange(getFontSize());
    }

    @CheckResult
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

    @CheckResult
    public float getSelectionPadSize() {
        return selectionPadSize;
    }

    /**
     * @param v [px]
     */
    public void setSelectionPadSize(final float v) {
        selectionPadSize = v;
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @CheckResult
    @IntRange(from = 0, to = 255)
    public int getPopupOpacity() {
        return popupOpacity;
    }

    /**
     * Overlay popups background opacity
     *
     * @param v [0..255]
     */
    public void setPopupOpacity(@IntRange(from = 0, to = 255) final int v) {
        popupOpacity = v;
        selectionPopup.setPopupBackgroundAlpha(v);
    }

    protected void adjustSelectionPopup() {
        if (selectionMode && !inGesture && mScroller.isFinished()) selectionPopup.show();
        else selectionPopup.hide();
    }

    @CheckResult
    public boolean getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(final boolean mode) {
        if (selectionMode == mode)
            return;
        if (mode) setMouseMode(false);
        selectionMode = mode;
        if (mode) {
            if (selection == null) {
                selection = new ConsoleScreenSelection();
                selection.first.x = selection.last.x = getCols() / 2 + Math.round(scrollPosition.x);
                selection.first.y = selection.last.y = getRows() / 2 + Math.round(scrollPosition.y);
                getCenterText(selection.first.x, selection.first.y, selectionMarkerExpr);
            }
            getCenterText(selection.first.x, selection.first.y, selectionMarkerFirst);
            getCenterText(selection.last.x, selection.last.y, selectionMarkerLast);
        } else {
            unsetCurrentSelectionMarker();
        }
        adjustSelectionPopup();
        ViewCompat.postInvalidateOnAnimation(this);
        if (onStateChange != null)
            onStateChange.onSelectionModeChange(mode);
    }

    public void invalidateSelectionUi(final boolean adjustPopup) {
        if (selection != null) {
            getCenterText(selection.first.x, selection.first.y, selectionMarkerFirst);
            getCenterText(selection.last.x, selection.last.y, selectionMarkerLast);
            if (adjustPopup && !inGesture) {
                inGesture = true;
                adjustSelectionPopup();
                inGesture = false;
                adjustSelectionPopup();
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @CheckResult
    public boolean getSelectionIsRect() {
        return selection != null && selection.isRectangular;
    }

    public void setSelectionIsRect(final boolean v) {
        if (selection != null) {
            selection.isRectangular = v;
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @CheckResult
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

    @CheckResult
    public boolean getMouseMode() {
        return mouseMode;
    }

    public void setMouseMode(final boolean mode) {
        if (mouseMode == mode)
            return;
        if (mode)
            setSelectionMode(false);
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

    @CheckResult
    public int getCols() {
        return (int) (getWidth() / mFontWidth);
    }

    @CheckResult
    public int getRows() {
        return (int) (getHeight() / mFontHeight);
    }

    @CheckResult
    public boolean isOnScreen(float x, float y) {
        x -= scrollPosition.x;
        y -= scrollPosition.y;
        return x >= 0 && x < getCols() && y >= 0 && y < getRows();
    }

    public void doScrollTextCenterTo(final float x, final float y) {
        doScrollToImmediate(x - (float) getCols() / 2, y - (float) getRows() / 2);
    }

    public void doScrollTextCenterTo(@NonNull final PointF marker) {
        doScrollTo(marker.x - (float) getCols() / 2,
                marker.y - (float) getRows() / 2,
                scrollableView ->
                        doScrollToImmediate(marker.x - (float) getCols() / 2,
                                marker.y - (float) getRows() / 2));
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
        final boolean inverse = consoleInput != null && consoleInput.currScrBuf.screenInverse;
        fontProvider.setPaint(fgPaint, (charAttrs.bold ? Typeface.BOLD : 0) |
                (charAttrs.italic ? Typeface.ITALIC : 0));
        fgPaint.setColor(colorProfile.getFgColor(charAttrs, inverse));
        fgPaint.setUnderlineText(charAttrs.underline);
        fgPaint.setStrikeThruText(charAttrs.crossed);
//        fgPaint.setShadowLayer(1, 0, 0, fgColor);
        bgPaint.setColor(colorProfile.getBgColor(charAttrs, inverse));
    }

    public void setConsoleInput(@NonNull final ConsoleInput consoleInput) {
        this.consoleInput = consoleInput;
        this.consoleInput.addOnInvalidateSink(this);
        this.consoleInput.addOnBufferScroll(this);
        resizeBuffer();
    }

    public void unsetConsoleInput() {
        if (consoleInput != null) {
            consoleInput.removeOnBufferScroll(this);
            consoleInput.removeOnInvalidateSink(this);
            consoleInput = null;
        }
    }

    @CheckResult
    public int getKeyHeightDp() {
        return keyHeightDp;
    }

    public void setKeyHeightDp(final int v) {
        if (this.keyHeightDp != v) {
            this.keyHeightDp = v;
            selectionPopup.setSizeDp(keyHeightDp);
        }
    }

    @CheckResult
    public int getSelectedCellsCount() {
        if (consoleInput == null || selection == null)
            return 0;
        final ConsoleScreenSelection s = selection.getDirect();
        if (s.first.y == s.last.y) {
            return s.last.x - s.first.x + 1;
        } else if (selection.isRectangular) {
            return (s.last.x - s.first.x + 1) * (s.last.y - s.first.y + 1);
        } else {
            return s.last.x - s.first.x + 1 +
                    consoleInput.currScrBuf.getWidth() * (s.last.y - s.first.y);
        }
    }

    @CheckResult
    @Nullable
    public String getSelectedText() {
        if (consoleInput == null || selection == null)
            return null;
        final ConsoleScreenSelection s = selection.getDirect();
        final StringBuilder sb = new StringBuilder();
        final ConsoleScreenBuffer.BufferTextRange v = new ConsoleScreenBuffer.BufferTextRange();
        int r;
        if (s.first.y == s.last.y) {
            r = consoleInput.currScrBuf
                    .getChars(s.first.x, s.first.y, s.last.x - s.first.x + 1, v);
            if (r >= 0)
                sb.append(v.toString().trim());
        } else if (selection.isRectangular) {
            for (int y = s.first.y; y <= s.last.y - 1; y++) {
                r = consoleInput.currScrBuf
                        .getChars(s.first.x, y, s.last.x - s.first.x + 1, v);
                if (r >= 0)
                    sb.append(v.toString().replaceAll(" *$", ""));
                sb.append('\n');
            }
            r = consoleInput.currScrBuf
                    .getChars(s.first.x, s.last.y, s.last.x - s.first.x + 1, v);
            if (r >= 0)
                sb.append(v.toString().replaceAll(" *$", ""));
        } else {
            r = consoleInput.currScrBuf.getChars(s.first.x, s.first.y,
                    consoleInput.currScrBuf.getWidth() - s.first.x, v);
            if (consoleInput.currScrBuf.isLineWrapped(s.first.y)) {
                if (r < 0)
                    return null;
                sb.append(v);
            } else {
                if (r >= 0)
                    sb.append(v.toString().replaceAll(" *$", ""));
                sb.append('\n');
            }
            for (int y = s.first.y + 1; y <= s.last.y - 1; y++) {
                r = consoleInput.currScrBuf.getChars(0, y,
                        consoleInput.currScrBuf.getWidth(), v);
                if (consoleInput.currScrBuf.isLineWrapped(y)) {
                    if (r < 0) return null;
                    sb.append(v);
                } else {
                    if (r >= 0) sb.append(v.toString().replaceAll(" *$", ""));
                    sb.append('\n');
                }
            }
            r = consoleInput.currScrBuf.getChars(0, s.last.y, s.last.x + 1, v);
            if (r >= 0)
                sb.append(v.toString().replaceAll(" *$", ""));
        }
        final String result = sb.toString();
        if (result.isEmpty())
            return null;
        return result;
    }

    protected void onSelectionChanged() {
        if (getSelectedCellsCount() <= SEARCH_PATTERN_CELLS_MAX)
            selectionPopup.setSearchPattern(getSelectedText());
        else
            selectionPopup.setSearchPattern("");
    }

    @CheckResult
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

    public void invalidateContent(@Nullable final Rect rect) {
        onInvalidateSink(rect);
    }

    protected boolean wasAltBuf = false;

    protected boolean invalidatingAll = false;
    protected final Rect invalidatingRegion = new Rect();

    protected void doInvalidateSink() {
        try {
            if (consoleInput != null) {
                if (!wasAltBuf && consoleInput.isAltBuf())
                    invalidateScroll();
                wasAltBuf = consoleInput.isAltBuf();
            }
            if (invalidatingAll)
                invalidate();
            else
                invalidate(invalidatingRegion);
        } finally {
            invalidatingAll = false;
            invalidatingRegion.setEmpty();
        }
    }

    @Override
    public void onInvalidateSink(@Nullable final Rect rect) {
        final boolean doPost = !invalidatingAll && invalidatingRegion.isEmpty();
        if (rect == null)
            invalidatingAll = true;
        else if (!invalidatingAll)
            invalidatingRegion.union(rect);
        if (doPost)
            ViewCompat.postOnAnimation(this, this::doInvalidateSink);
    }

    protected final Point invalidatingSize = new Point();

    protected void doInvalidateSinkResize() {
        try {
            invalidateScroll();
        } finally {
            invalidatingSize.set(0, 0);
        }
    }

    @Override
    public void onInvalidateSinkResize(final int cols, final int rows) {
        final boolean doPost = invalidatingSize.x <= 0 || invalidatingSize.y <= 0;
        invalidatingSize.set(cols, rows);
        if (doPost)
            ViewCompat.postOnAnimation(this, this::doInvalidateSinkResize);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (onStateChange != null) {
            isChanging = true;
            try {
                onStateChange.onTerminalAreaResize(w, h);
            } finally {
                isChanging = false;
            }
        }
        resizeBuffer();
        terminalScrollbars.onResize();
    }

    protected int mButtons = 0;
    protected final Point mXY = new Point();

    protected static int translateButtons(int buttons) {
        if ((buttons & MotionEvent.BUTTON_BACK) != 0)
            buttons |= MotionEvent.BUTTON_SECONDARY;
        if ((buttons & MotionEvent.BUTTON_STYLUS_PRIMARY) != 0)
            buttons |= MotionEvent.BUTTON_SECONDARY;
        if ((buttons & MotionEvent.BUTTON_STYLUS_SECONDARY) != 0)
            buttons |= MotionEvent.BUTTON_TERTIARY;
        return buttons;
    }

    protected static int getButtons(@NonNull final MotionEvent event) {
        final int action = event.getActionMasked();
        return translateButtons(event.getButtonState() |
                ((event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS &&
                        action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) ?
                        MotionEvent.BUTTON_PRIMARY : 0));
    }

    @CheckResult
    public static boolean isMouseEvent(@Nullable final MotionEvent event) {
        return event != null &&
                event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER;
    }

    @CheckResult
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

    @SuppressLint("ClickableViewAccessibility")
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
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (selectionMarker != null) {
                        if (!isOnScreen(selectionMarker.x, selectionMarker.y))
                            doScrollTextCenterTo(selectionMarker);
                        unsetCurrentSelectionMarker();
                        inGesture = false;
                        onSelectionChanged();
                        adjustSelectionPopup();
                        onTerminalScrollEnd();
                        ViewCompat.postInvalidateOnAnimation(this);
                        return true;
                    }
                    break;
            }
            if (selectionMarker != null)
                return true;
        } else if (mouseMode) { // No gestures here
            if (consoleInput != null && consoleInput.consoleOutput != null) {
                final int x = getBufferTextPosX(MathUtils.clamp((int) event.getX(),
                        0, getWidth() - 1));
                final int y = getBufferTextPosY(MathUtils.clamp((int) event.getY(),
                        0, getHeight() - 1));
                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        final int buttons;
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                            buttons = MotionEvent.BUTTON_PRIMARY;
                        } else {
                            final int bs = getButtons(event);
                            buttons = bs & ~mButtons;
                            if (buttons == 0)
                                break;
                            mButtons = bs;
                        }
                        consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.PRESS,
                                buttons, x, y);
                        mXY.x = x;
                        mXY.y = y;
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        if (mXY.x == x && mXY.y == y)
                            break;
                        final int buttons =
                                event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
                                        ? MotionEvent.BUTTON_PRIMARY
                                        : (mButtons = getButtons(event));

                        consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.MOVE,
                                buttons, x, y);
                        mXY.x = x;
                        mXY.y = y;
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        final int buttons;
                        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                            buttons = MotionEvent.BUTTON_PRIMARY;
                        } else {
                            final int bs = getButtons(event);
                            buttons = mButtons & ~bs;
                            if (buttons == 0)
                                break;
                            mButtons = bs;
                        }
                        consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.RELEASE,
                                buttons, x, y);
                        unsetCurrentSelectionMarker();
                        inGesture = false;
                        adjustSelectionPopup();
                        onTerminalScrollEnd();
                        ViewCompat.postInvalidateOnAnimation(this);
                        break;
                    }
                }
            }
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            if (event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER)
                mButtons = getButtons(event);
            onTerminalScrollBegin();
        }
        final boolean ret = super.onTouchEvent(event);
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (event.getToolType(0) != MotionEvent.TOOL_TYPE_FINGER)
                mButtons = getButtons(event);
            unsetCurrentSelectionMarker();
            inGesture = false;
            adjustSelectionPopup();
            onTerminalScrollEnd();
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
                final int x = getBufferTextPosX(MathUtils.clamp((int) event.getX(),
                        0, getWidth() - 1));
                final int y = getBufferTextPosY(MathUtils.clamp((int) event.getY(),
                        0, getHeight() - 1));
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_HOVER_MOVE: {
                        if (mXY.x == x && mXY.y == y)
                            break;
                        // Some fancy things for styluses (there are no button events before API 23)
                        final boolean isStylus = (event.getSource() & InputDevice.SOURCE_STYLUS)
                                == InputDevice.SOURCE_STYLUS;
                        final int buttons = getButtons(event) &
                                ~(isStylus ? MotionEvent.BUTTON_PRIMARY : 0);
                        final int downButtons = buttons & ~mButtons;
                        final int upButtons = ~buttons & mButtons;
                        mButtons = buttons;
                        if (downButtons != 0)
                            consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.PRESS,
                                    downButtons, x, y);
                        consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.MOVE,
                                buttons, x, y);
                        if (upButtons != 0)
                            consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.RELEASE,
                                    upButtons, x, y);
                        mXY.x = x;
                        mXY.y = y;
                        break;
                    }
                    case MotionEvent.ACTION_SCROLL: {
                        final float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                        if (vScroll != 0)
                            consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.VSCROLL,
                                    (int) vScroll, x, y);
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
        final int rows = (int) prevAppTextVScrollPos;
        prevAppTextVScrollPos -= rows;
        return rows;
    }

    private float prevAppTextHScrollPos = 0;

    private int getNextAppTextX(final float dX) {
        prevAppTextHScrollPos += dX;
        final int cols = (int) prevAppTextHScrollPos;
        prevAppTextHScrollPos -= cols;
        return cols;
    }

    @NonNull
    private final ScrollerEx appTextScroller;

    private boolean wasAppTextScrolling = false;

    private boolean appHScrollEnabled = false;

    @CheckResult
    public boolean isAppHScrollEnabled() {
        return appHScrollEnabled;
    }

    public void setAppHScrollEnabled(final boolean appHScrollEnabled) {
        this.appHScrollEnabled = appHScrollEnabled;
    }

    @Override
    public boolean onScroll(final MotionEvent e1, final MotionEvent e2,
                            final float distanceX, final float distanceY) {
        if (consoleInput != null && consoleInput.consoleOutput != null &&
                consoleInput.isAltBuf()) {
            float bufDX = distanceX;
            float bufDY = distanceY;
            boolean invalidateScroll = false;
            if (getRows() >= consoleInput.currScrBuf.getHeight()) {
                if (scrollPosition.y != 0) {
                    scrollPosition.y = 0;
                    invalidateScroll = true;
                }
                bufDY = 0;
                consoleInput.consoleOutput.vScroll(getNextAppTextY(distanceY / scrollScale.y));
            }
            if (appHScrollEnabled && getCols() >= consoleInput.currScrBuf.getWidth()) {
                if (scrollPosition.x != 0) {
                    scrollPosition.x = 0;
                    invalidateScroll = true;
                }
                bufDX = 0;
                consoleInput.consoleOutput.hScroll(getNextAppTextX(distanceX / scrollScale.x));
            }
            if (invalidateScroll) {
                execOnScroll();
                ViewCompat.postInvalidateOnAnimation(this);
            }
            return (bufDY == 0 && bufDX == 0) ||
                    super.onScroll(e1, e2, bufDX, bufDY);
        }
        return super.onScroll(e1, e2, distanceX, distanceY);
    }

    @Override
    public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                           final float velocityX, final float velocityY) {
        if (consoleInput != null && consoleInput.consoleOutput != null &&
                consoleInput.isAltBuf()) {
            float bufDX = velocityX;
            float bufDY = velocityY;
            float appDX = 0;
            float appDY = 0;
            boolean invalidateScroll = false;
            if (getRows() >= consoleInput.currScrBuf.getHeight()) {
                if (scrollPosition.y != 0) {
                    scrollPosition.y = 0;
                    invalidateScroll = true;
                }
                bufDY = 0;
                appDY = velocityY;
            }
            if (appHScrollEnabled && getCols() >= consoleInput.currScrBuf.getWidth()) {
                if (scrollPosition.x != 0) {
                    scrollPosition.x = 0;
                    invalidateScroll = true;
                }
                bufDX = 0;
                appDX = velocityX;
            }
            if (invalidateScroll) {
                execOnScroll();
                ViewCompat.postInvalidateOnAnimation(this);
            }
            if (appDY != 0 || appDX != 0) {
                appTextScroller.forceFinished(true);
                appTextScroller.fling(-(int) (appDX / scrollScale.x),
                        -(int) (appDY / scrollScale.y));
            }
            return (bufDY == 0 && bufDX == 0) ||
                    super.onFling(e1, e2, bufDX, bufDY);
        }
        return super.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent e) {
        if (wasAppTextScrolling)
            return true; // Just stop sending fling scrolling escapes to an application...
        final int x = getBufferTextPosX(MathUtils.clamp((int) e.getX(),
                0, getWidth() - 1));
        final int y = getBufferTextPosY(MathUtils.clamp((int) e.getY(),
                0, getHeight() - 1));
        if (isMouseSupported()) {
            final int buttons;
            if (e.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                buttons = MotionEvent.BUTTON_PRIMARY;
            } else {
                buttons = mButtons & ~getButtons(e);
            }
            if (buttons != 0) {
                consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.PRESS,
                        buttons, x, y);
                consoleInput.consoleOutput.feed(ConsoleOutput.MouseEventType.RELEASE,
                        buttons, x, y);
            }
        } else {
            setSelectionMode(true);
            setSelectionModeIsExpr(true);
            getCenterText(x, y, selectionMarkerExpr);
            doAutoSelect();
            onSelectionChanged();
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
            if (consoleInput.currScrBuf
                    .getChars(0, py, Integer.MAX_VALUE, chars) >= 0) {
                final int ptr = ConsoleScreenBuffer.getCharIndex(chars.text,
                        px, 0, true);
                final char sym = (ptr >= chars.text.length) ? '\0' : chars.text[ptr];
                CharsAutoSelector.select(chars.text,
                        chars.start, chars.start + chars.length,
                        ptr, sym, bb);
                selection.first.set(ConsoleScreenBuffer
                        .getCharPos(chars.text, 0, bb[0]), py);
                selection.last.set(ConsoleScreenBuffer
                        .getCharPos(chars.text, 0, bb[1]) - 1, py);
                doAutoSelectDown(py, sym,
                        doAutoSelectUp(py, sym, AUTOSELECT_LINES_MAX));
                getCenterText(selection.first.x, selection.first.y, selectionMarkerFirst);
                getCenterText(selection.last.x, selection.last.y, selectionMarkerLast);
            }
        }
    }

    protected int doAutoSelectUp(int py, final char sym, int ly) {
        final int[] bb = new int[2];
        final ConsoleScreenBuffer.BufferTextRange chars =
                new ConsoleScreenBuffer.BufferTextRange();
        py--;
        while (ly > 0 && selection.first.x == 0 && consoleInput.currScrBuf.isLineWrapped(py) &&
                consoleInput.currScrBuf
                        .getChars(0, py, Integer.MAX_VALUE, chars) >= 0) {
            final int ptr = chars.start + chars.length - 1;
            CharsAutoSelector.select(chars.text,
                    chars.start, chars.start + chars.length,
                    ptr, sym, bb);
            selection.first.set(ConsoleScreenBuffer
                    .getCharPos(chars.text, 0, bb[0]), py);
            ly--;
            py--;
        }
        return ly;
    }

    protected int doAutoSelectDown(int py, final char sym, int ly) {
        final int[] bb = new int[2];
        final ConsoleScreenBuffer.BufferTextRange chars =
                new ConsoleScreenBuffer.BufferTextRange();
        py++;
        while (ly > 0 && selection.last.x == consoleInput.currScrBuf.getWidth() - 1 &&
                consoleInput.currScrBuf.isLineWrapped(py - 1) &&
                consoleInput.currScrBuf
                        .getChars(0, py, Integer.MAX_VALUE, chars) >= 0) {
            final int ptr = chars.start;
            CharsAutoSelector.select(chars.text,
                    chars.start, chars.start + chars.length,
                    ptr, sym, bb);
            selection.last.set(ConsoleScreenBuffer
                    .getCharPos(chars.text, 0, bb[1]) - 1, py);
            ly--;
            py++;
        }
        return ly;
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
        if (scrollFollowHistoryThreshold > 0F
                && scrollPosition.y < Math.min((float) -getRows() * scrollFollowHistoryThreshold,
                getBottomScrollLimit() - 0.5F)) {
            scrollPtWithBuffer(scrollPosition, from, to, n);
            invalidateScroll();
        }
    }

    @Override
    protected void execOnScroll() {
        terminalScrollbars.onScroll();
        super.execOnScroll();
    }

    private boolean isBufWidthFit() {
        return consoleInput == null || getCols() >= consoleInput.currScrBuf.getWidth();
    }

    private boolean isBufHeightFit() {
        return consoleInput == null || getRows() >= consoleInput.currScrBuf.getMaxBufferHeight();
    }

    private final class TerminalScrollbars {
        private final PopupWindow popupH;
        private final PopupWindow popupV;
        private final View vTrackH;
        private final View vTrackV;
        private final ViewGroup.MarginLayoutParams lpH;
        private final ViewGroup.MarginLayoutParams lpV;
        private final View vMarkH;
        private final View vMarkV;
        private final View vHistory;

        {
            final ViewGroup vP = new FrameLayout(getContext());
            vTrackH = LayoutInflater.from(getContext())
                    .inflate(terminalScrollHorizontalLayout, vP, false);
            vTrackV = LayoutInflater.from(getContext())
                    .inflate(terminalScrollVerticalLayout, vP, false);
            lpH = (ViewGroup.MarginLayoutParams) vTrackH.getLayoutParams();
            lpV = (ViewGroup.MarginLayoutParams) vTrackV.getLayoutParams();
            vMarkH = vTrackH.findViewById(R.id.mark);
            vMarkV = vTrackV.findViewById(R.id.mark);
            vHistory = vTrackV.findViewById(R.id.history);
            popupH = new PopupWindow(vTrackH,
                    ViewGroup.LayoutParams.MATCH_PARENT, lpH.height);
            popupH.setClippingEnabled(false);
            popupH.setSplitTouchEnabled(false);
            popupH.setAnimationStyle(android.R.style.Animation_Dialog);
            popupV = new PopupWindow(vTrackV,
                    lpV.width, ViewGroup.LayoutParams.MATCH_PARENT);
            popupV.setClippingEnabled(false);
            popupV.setSplitTouchEnabled(false);
            popupV.setAnimationStyle(android.R.style.Animation_Dialog);
            vTrackH.addOnLayoutChangeListener(
                    (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                            refreshH());
            vTrackV.addOnLayoutChangeListener(
                    (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                            refreshV());
        }

        private void onResize() {
            popupH.setWidth(getWidth() - terminalScrollOffset * 2
                    - lpH.leftMargin - lpH.rightMargin);
            popupV.setHeight(getHeight() - terminalScrollOffset * 2
                    - lpV.topMargin - lpV.bottomMargin);
        }

        private void refreshH() {
            final int cols = getCols();
            final float left = getLeftScrollLimit();
            final int leftPad = vTrackH.getPaddingLeft();
            final float kH =
                    (vTrackH.getWidth() - leftPad - vTrackH.getPaddingRight())
                            / (getRightScrollLimit() - left + cols);
            vMarkH.setLeft(Math.round((scrollPosition.x - left) * kH) + leftPad);
            vMarkH.setRight(Math.round((scrollPosition.x - left + cols) * kH) + leftPad);
        }

        private void refreshV() {
            final int rows = getRows();
            final float top = getTopScrollLimit();
            final int topPad = vTrackV.getPaddingTop();
            final float kV =
                    (vTrackV.getHeight() - topPad - vTrackV.getPaddingBottom())
                            / (getBottomScrollLimit() - top + rows);
            vMarkV.setTop(Math.round((scrollPosition.y - top) * kV) + topPad);
            vMarkV.setBottom(Math.round((scrollPosition.y - top + rows) * kV) + topPad);
            vHistory.setBottom(Math.round((-top) * kV) + topPad);
        }

        private void onScroll() {
            if (popupH.isShowing()) refreshH();
            if (popupV.isShowing()) refreshV();
        }

        private final int[] mWindowCoords = new int[2];

        private void show() {
            getLocationInWindow(mWindowCoords);
            if (!isBufWidthFit())
                popupH.showAtLocation(ConsoleScreenView.this, Gravity.NO_GRAVITY,
                        mWindowCoords[0] + terminalScrollOffset + lpH.leftMargin,
                        mWindowCoords[1] + terminalScrollOffset + lpH.topMargin);
            if (!isBufHeightFit())
                popupV.showAtLocation(ConsoleScreenView.this, Gravity.NO_GRAVITY,
                        mWindowCoords[0] + terminalScrollOffset + lpV.leftMargin,
                        mWindowCoords[1] + terminalScrollOffset + lpV.topMargin);
        }

        private void hide() {
            popupH.dismiss();
            popupV.dismiss();
        }
    }

    @NonNull
    private final TerminalScrollbars terminalScrollbars;

    @CallSuper
    protected void onTerminalScrollBegin() {
        terminalScrollbars.show();
    }

    @CallSuper
    protected void onTerminalScrollEnd() {
        terminalScrollbars.hide();
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
        super.onDraw(canvas);
        drawContent(canvas);
        if (consoleInput != null) {
            drawSelection(canvas);
            if (mBlinkState && consoleInput.cursorVisibility) canvas.drawRect(getBufferDrawRect(
                            consoleInput.currScrBuf.getAbsPosX(),
                            consoleInput.currScrBuf.getAbsPosY()),
                    cursorPaint);
        }
    }

    protected final float getSelectionMarkerDistance(@NonNull final MotionEvent event,
                                                     @NonNull final PointF marker,
                                                     final float r) {
        final float eX = event.getX();
        final float eY = event.getY();
        final float mX = MathUtils.clamp(getBufferDrawPosXF(marker.x),
                0, getWidth() - 1);
        final float mY = MathUtils.clamp(getBufferDrawPosYF(marker.y),
                0, getHeight() - 1);
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
            final float dF = getSelectionMarkerDistance(event,
                    selectionMarkerFirst, r);
            final float dL = getSelectionMarkerDistance(event,
                    selectionMarkerLast, r);
            if (dF < dL) {
                selectionMarker = selectionMarkerFirst;
                scrollDisabled = true;
                ViewCompat.postInvalidateOnAnimation(this);
                return;
            }
            if (dF > dL || dF != Float.POSITIVE_INFINITY) {
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
                canvas.drawRect(getBufferDrawRectInc(selection.first, selection.last),
                        selectionPaint);
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
            if (selectionWrappedLineMarker != null && !selection.isRectangular)
                for (int j = _draw_textRect.top; j < _draw_textRect.bottom; j++)
                    if (consoleInput.currScrBuf.isLineWrapped(j)) {
                        final int x = getWidth();
                        final int y = (int) getBufferDrawPosYF(j + 0.5F);
                        final int sx = selectionWrappedLineMarker.getIntrinsicWidth();
                        final int sy = selectionWrappedLineMarker.getIntrinsicHeight();
                        canvas.save();
                        canvas.translate(x - sx, y - sy / 2);
                        selectionWrappedLineMarker.setBounds(0, 0, sx, sy);
                        selectionWrappedLineMarker.draw(canvas);
                        canvas.restore();
                    }
            if (selectionMarker != null) {
                if (selectionMarkerPtr != null) {
                    drawMarker(canvas, selectionMarkerPtr,
                            selectionMarker, mFontHeight * 3);
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
            boolean _hasVisibleBlinking = false;
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
                        ConsoleScreenBuffer.decodeFgAttrs(charAttrs,
                                consoleInput.currScrBuf.defaultFgAttrs);
                        ConsoleScreenBuffer.decodeBgAttrs(charAttrs,
                                consoleInput.currScrBuf.defaultBgAttrs);
                        applyCharAttrs();
                        canvas.drawRect(strFragLeft, strTop,
                                getWidth(), strBottom,
                                bgPaint);
                        break;
                    }
                    ConsoleScreenBuffer.decodeFgAttrs(charAttrs, _draw_run.fgAttrs);
                    ConsoleScreenBuffer.decodeBgAttrs(charAttrs, _draw_run.bgAttrs);
                    applyCharAttrs();
                    final float strFragRight = getBufferDrawPosXF(i + sr);
                    if (sr > 0) {
                        // background is only for non-zero length glyphs
                        // see https://en.wikipedia.org/wiki/Combining_character
                        canvas.drawRect(strFragLeft, strTop,
                                strFragRight, strBottom,
                                bgPaint);
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
                        _hasVisibleBlinking |= charAttrs.blinking;
                        if (!charAttrs.blinking || mBlinkState)
                            canvas.drawText(_draw_run.text,
                                    _draw_run.start, _draw_run.length,
                                    strFragLeft, strTop - fgPaint.ascent(), fgPaint);
                    }
                    i += sr;
                }
                _draw_run.init();
            }
            hasVisibleBlinking = _hasVisibleBlinking;
            if (paddingMarkup != null) {
                if (vDivBottom < getHeight())
                    drawDrawable(canvas, paddingMarkup,
                            0, (int) vDivBottom,
                            getWidth(), getHeight());
                if (hDiv < getWidth())
                    drawDrawable(canvas, paddingMarkup, (int) hDiv, 0,
                            getWidth(), Math.min(getHeight(), (int) vDivBottom));
            }
            canvas.drawLine(0, vDivBottom, getWidth(), vDivBottom,
                    paddingMarkupPaint);
            canvas.drawLine(0, vDivBuf, getWidth(), vDivBuf,
                    paddingMarkupPaint);
            canvas.drawLine(hDiv, 0, hDiv, getHeight(),
                    paddingMarkupPaint);
        }
    }

    protected final void drawDrawable(@NonNull final Canvas canvas,
                                      @Nullable final Drawable drawable,
                                      final int left, final int top,
                                      final int right, final int bottom) {
        if (drawable == null)
            return;
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
