package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;

import green_green_avk.anotherterm.GraphicsConsoleLedsInput;
import green_green_avk.anotherterm.HwKeyMapManager;
import green_green_avk.anotherterm.IConsoleOutput;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.KeyIntervalDetector;

public class GraphicsConsoleKeyboardView extends ExtKeyboardView implements
        ExtKeyboardView.OnKeyboardActionListener, GraphicsConsoleLedsInput.OnInvalidateSink {
    protected GraphicsConsoleLedsInput consoleInput = null;
    protected IConsoleOutput consoleOutput = null;

    protected boolean ctrl = false;
    protected boolean alt = false;

    protected boolean wasKey = false;

    protected boolean imeEnabled = false;

    public static final int MODE_VISIBLE = 0;
    public static final int MODE_IME = 1;
    public static final int MODE_HW_ONLY = 2;
    protected static final int MODE_UNKNOWN = -1;
    protected int mode = MODE_VISIBLE;
    protected int prevMode = mode;
    protected int currMode = mode;
    protected boolean textMode; // ANSI-keyboard like input

    protected void initTextMode(final boolean v) {
        textMode = v;
        setPopupFunctions(v ? null : popupFunctionsSuppress);
    }

    {
        initTextMode(false);
    }

    protected int keyHeightDp = 0;
    @NonNull
    private HwKeyMap hwKeyMap = HwKeyMap.DEFAULT;
    private int hwDoubleKeyPressInterval = 500; // [ms]

    @AnyRes
    private int layoutRes = R.array.graphics_keyboard;

    public static class State {
        private int mode = MODE_UNKNOWN;
        private boolean textMode = false;

        public void save(@NonNull final GraphicsConsoleKeyboardView v) {
            mode = v.getMode();
            textMode = v.textMode;
        }

        public void apply(@NonNull final GraphicsConsoleKeyboardView v) {
            if (mode == MODE_UNKNOWN)
                return;
            v.initTextMode(textMode);
            v.setMode(mode);
        }
    }

    public GraphicsConsoleKeyboardView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GraphicsConsoleKeyboardView(final Context context, @Nullable final AttributeSet attrs,
                                       final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public GraphicsConsoleKeyboardView(final Context context, @Nullable final AttributeSet attrs,
                                       final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setOnKeyboardActionListener(this);
        setFocusableInTouchMode(true);
        requestFocus();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        applyConfig(getResources().getConfiguration());
    }

    private boolean numLed = false;
    private boolean capsLed = false;
    private boolean scrollLed = false;

    protected boolean invalidating = false;

    protected void doInvalidateSink() {
        invalidating = false;
        if (consoleInput != null) {
            if (consoleInput.numLed != numLed) {
                numLed = consoleInput.numLed;
                setLedsByCode(KeyEvent.KEYCODE_NUM_LOCK, numLed);
                invalidateModifierKeys(KeyEvent.KEYCODE_NUM_LOCK);
            }
            if (consoleInput.capsLed != capsLed) {
                capsLed = consoleInput.capsLed;
                setLedsByCode(KeyEvent.KEYCODE_CAPS_LOCK, capsLed);
                invalidateModifierKeys(KeyEvent.KEYCODE_CAPS_LOCK);
            }
            if (consoleInput.scrollLed != scrollLed) {
                scrollLed = consoleInput.scrollLed;
                setLedsByCode(KeyEvent.KEYCODE_SCROLL_LOCK, scrollLed);
                invalidateModifierKeys(KeyEvent.KEYCODE_SCROLL_LOCK);
            }
        }
        if (consoleOutput != null) {
            setLayoutRes(consoleOutput.getLayoutRes());
        }
    }

    @Override
    public void onInvalidateSink() {
        if (!invalidating) {
            invalidating = true;
            ViewCompat.postOnAnimation(this, this::doInvalidateSink);
        }
    }

    @SuppressLint("ResourceType")
    private void applyConfig(@NonNull final Configuration cfg) {
        final Resources res = getContext().getResources();
        final int layoutNarrow;
        final int layoutWide;
        final int layoutFull;
        final TypedArray layouts = res.obtainTypedArray(layoutRes);
        try {
            layoutNarrow = layouts.getResourceId(0, 0);
            layoutWide = layouts.getResourceId(1, layoutNarrow);
            layoutFull = layouts.length() > 2
                    ? layouts.getResourceId(2, layoutWide) : layoutWide;
        } finally {
            layouts.recycle();
        }
        final float kbdW = cfg.screenWidthDp / cfg.fontScale;
        final float keyW = res.getDimension(R.dimen.kbd_key_size)
                / res.getDisplayMetrics().scaledDensity;
        final float kbdKeys = kbdW / keyW;
        final int kbdRes = kbdKeys >= 20
                ? (kbdKeys >= 23 ? layoutFull : layoutWide) : layoutNarrow;
        final ExtKeyboard.Configuration kc = new ExtKeyboard.Configuration();
        kc.keyHeight = (int) (keyHeightDp * res.getDisplayMetrics().density);
        setKeyboard(new ExtKeyboard(getContext(), kbdRes, kc));
        applyMode(cfg);
    }

    public void setConsoleInput(@NonNull final GraphicsConsoleLedsInput consoleInput) {
        this.consoleInput = consoleInput;
        this.consoleOutput = this.consoleInput.consoleOutput;
        this.consoleInput.addOnInvalidateSink(this);
        onInvalidateSink();
    }

    public void setConsoleOutputOnly(@NonNull final IConsoleOutput consoleOutput) {
        this.consoleOutput = consoleOutput;
        onInvalidateSink();
    }

    public void unsetConsoleInput() {
        if (consoleInput != null)
            consoleInput.removeOnInvalidateSink(this);
        consoleOutput = null;
        consoleInput = null;
    }

    @Override
    public boolean getAutoRepeat() {
        return consoleOutput == null || consoleOutput.getKeyAutorepeat();
    }

    public int getKeyHeightDp() {
        return keyHeightDp;
    }

    public void setKeyHeightDp(final int v) {
        if (this.keyHeightDp != v) {
            this.keyHeightDp = v;
            if (getWindowToken() != null) // if attached
                applyConfig(getResources().getConfiguration());
        }
    }

    @AnyRes
    public int getLayoutRes() {
        return layoutRes;
    }

    public void setLayoutRes(@AnyRes final int v) {
        if (v != 0 && layoutRes != v) {
            layoutRes = v;
            applyConfig(getResources().getConfiguration());
        }
    }

    public void clipboardPaste(@Nullable final String v) {
        if (consoleOutput == null || v == null)
            return;
        consoleOutput.paste(v);
    }

    @Override
    public void onWindowFocusChanged(final boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) reapplyMode();
    }

    protected void _showIme() {
        final InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null)
            return;
        imeEnabled = true;
        requestFocus();
        imm.restartInput(this);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
    }

    protected void _hideIme() {
        final InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null)
            return;
        if (imm.isActive(this))
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        imeEnabled = false;
    }

    protected final Runnable rDelayed = () -> {
        final Context ctx = getContext();
        final int wmlp;
        final boolean hidden;
        switch (mode) {
            case MODE_VISIBLE:
                wmlp = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
                hidden = false;
                break;
            case MODE_IME:
                wmlp = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                hidden = true;
                break;
            default:
                wmlp = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
                hidden = true;
        }
        if (ctx instanceof Activity) {
            final Window w = ((Activity) ctx).getWindow();
            if (w != null)
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED | wmlp);
        }
        mHidden = hidden;
        requestLayout();
    };

    public int getMode() {
        return mode;
    }

    public void setMode(final int mode) {
        if (this.mode == mode)
            return;
        prevMode = this.mode;
        this.mode = mode;
        applyMode(getResources().getConfiguration());
    }

    public boolean isTextMode() {
        return textMode;
    }

    public void setTextMode(final boolean v) {
        if (textMode != v) {
            initTextMode(v);
            //reapplyMode();
        }
    }

    protected void reapplyMode() {
        prevMode = currMode = MODE_UNKNOWN;
        applyMode(getResources().getConfiguration());
    }

    protected void applyMode(@NonNull final Configuration cfg) {
        // Hardware keyboard backspace key suppression bug workaround
        if (cfg.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES && mode == MODE_IME)
            mode = MODE_HW_ONLY;
        if (currMode == mode)
            return;
        mHandler.removeCallbacks(rDelayed);
        if (!ViewCompat.isAttachedToWindow(this))
            return;
        switch (prevMode) {
            case MODE_VISIBLE:
            case MODE_HW_ONLY:
                rDelayed.run();
                break;
            default:
                mHandler.postDelayed(rDelayed, 500);
        }
        switch (mode) {
            case MODE_IME:
                _showIme();
                break;
            default:
                _hideIme();
        }
        currMode = mode;
    }

    @NonNull
    public HwKeyMap getHwKeyMap() {
        return hwKeyMap;
    }

    public void setHwKeyMap(@Nullable final HwKeyMap v) {
        this.hwKeyMap = v == null ? HwKeyMap.DEFAULT : v;
        metaStateFilterCache.clear();
    }

    public int getHwDoubleKeyPressInterval() {
        return hwDoubleKeyPressInterval;
    }

    public void setHwDoubleKeyPressInterval(final int v) {
        this.hwDoubleKeyPressInterval = v;
    }

    // No basic modifiers allowed in the intermediate calculations.
    private static int getMetaStateByKeycode(final int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:
                return KeyEvent.META_SHIFT_LEFT_ON;
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                return KeyEvent.META_SHIFT_RIGHT_ON;
            case KeyEvent.KEYCODE_ALT_LEFT:
                return KeyEvent.META_ALT_LEFT_ON;
            case KeyEvent.KEYCODE_ALT_RIGHT:
                return KeyEvent.META_ALT_RIGHT_ON;
            case KeyEvent.KEYCODE_CTRL_LEFT:
                return KeyEvent.META_CTRL_LEFT_ON;
            case KeyEvent.KEYCODE_CTRL_RIGHT:
                return KeyEvent.META_CTRL_RIGHT_ON;
            case KeyEvent.KEYCODE_META_LEFT:
                return KeyEvent.META_META_LEFT_ON;
            case KeyEvent.KEYCODE_META_RIGHT:
                return KeyEvent.META_META_RIGHT_ON;
            case KeyEvent.KEYCODE_SYM:
                return KeyEvent.META_SYM_ON;
            case KeyEvent.KEYCODE_FUNCTION:
                return KeyEvent.META_FUNCTION_ON;
        }
        return 0;
    }

    private static final int META_ALL_MASK = KeyEvent.normalizeMetaState(-1)
            & ~(KeyEvent.META_SHIFT_ON | KeyEvent.META_ALT_ON
            | KeyEvent.META_CTRL_ON | KeyEvent.META_META_ON);
    private static final int META_DEF_MASK = META_ALL_MASK
            & ~(KeyEvent.META_ALT_MASK | KeyEvent.META_CTRL_MASK);
    private static final int[] modifierKeys = new int[]{
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_SYM,
            KeyEvent.KEYCODE_FUNCTION
    };

    private final SparseArray<int[]> metaStateFilterCache = new SparseArray<>();
    private static final int[] metaStateFilterDefault = new int[]{META_ALL_MASK, META_DEF_MASK};

    private int[] getMetaStateFilter(@NonNull final KeyEvent event) {
        final int devType = hwKeyMap.getDevType(event);
        if (devType < 0)
            return metaStateFilterDefault;
        final int[] r = metaStateFilterDefault.clone();
        final int idx = metaStateFilterCache.indexOfKey(devType);
        if (idx >= 0)
            return metaStateFilterCache.valueAt(idx);
        for (final int k : modifierKeys) {
            final int t = hwKeyMap.get(k, devType);
            if (t == HwKeyMap.KEYCODE_ACTION_DEFAULT)
                continue;
            final int m = getMetaStateByKeycode(k);
            if (t == HwKeyMap.KEYCODE_ACTION_BYPASS) {
                r[0] &= ~m;
                r[1] |= m;
            } else {
                r[0] &= ~m;
                r[1] &= ~m;
            }
        }
        metaStateFilterCache.put(devType, r);
        return r;
    }

    // For the custom modifiers mapping.
    private int metaState = 0;
    private int metaStateToggle = 0;
    private int metaStateToggleOneShot = 0;

    private void metaStateOnKey() {
        metaStateToggle &= ~(metaState | metaStateToggleOneShot);
    }

    private final class MetaPopup {
        @SuppressLint("InflateParams")
        private final View g = LayoutInflater.from(getContext())
                .inflate(R.layout.hw_modifiers_overlay, null);
        private final PopupWindow w = new PopupWindow(g,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        private final TextView v =
                (TextView) ((g instanceof TextView) ? g : g.findViewById(R.id.text));

        {
            w.setFocusable(false);
            w.setTouchable(false);
        }

        private void show() {
            w.showAtLocation(getRootView(), 0, 0, 0);
        }

        private void hide() {
            w.dismiss();
        }

        private void setText(@NonNull final CharSequence text) {
            v.setText(text);
        }
    }

    private final MetaPopup metaPopup = new MetaPopup();
    private int prevStickyMetaState = 0;

    private void invalidateHwMetaIndication() {
        final int stickyMetaState = metaStateToggle & ~metaState;
        if (stickyMetaState == prevStickyMetaState)
            return;
        final StringBuilder text = new StringBuilder();
        if ((stickyMetaState & KeyEvent.META_ALT_MASK) != 0) {
            text.append("[");
            text.append(getContext().getString(R.string.label_mod_alt));
            text.append("]\uD83D\uDD12");
            if ((stickyMetaState & metaStateToggleOneShot & KeyEvent.META_ALT_MASK) != 0)
                text.append("¹");
        }
        if ((stickyMetaState & KeyEvent.META_CTRL_MASK) != 0) {
            if (text.length() > 0) text.append("+");
            text.append("[");
            text.append(getContext().getString(R.string.label_mod_control));
            text.append("]\uD83D\uDD12");
            if ((stickyMetaState & metaStateToggleOneShot & KeyEvent.META_CTRL_MASK) != 0)
                text.append("¹");
        }
        if (text.length() == 0) {
            metaPopup.hide();
            metaPopup.setText("");
        } else {
            metaPopup.setText(text);
            metaPopup.show();
        }
        prevStickyMetaState = stickyMetaState;
    }

    private int accent = 0;

    private boolean send(@NonNull final KeyEvent event) {
        if (consoleOutput == null)
            return false;
        final int[] filter = getMetaStateFilter(event);
        final int eventMetaState =
                KeyEvent.normalizeMetaState((event.getMetaState() & filter[0]) | metaState
                        | metaStateToggle);
        final boolean shift = (eventMetaState & KeyEvent.META_SHIFT_MASK) != 0;
        final boolean alt = (eventMetaState & KeyEvent.META_ALT_MASK) != 0;
        final boolean ctrl = (eventMetaState & KeyEvent.META_CTRL_MASK) != 0;
        int code = hwKeyMap.get(event);
        if (code < 0)
            code = event.getKeyCode();
        final String r = consoleOutput.getKeySeq(code, shift, alt, ctrl);
        if (r != null) {
            consoleOutput.feed(r);
            metaStateOnKey();
            return true;
        }
        final int c = event.getKeyCharacterMap().get(code,
                KeyEvent.normalizeMetaState(event.getMetaState() & filter[1]));
        if (c == 0) {
            consoleOutput.feed(c, shift, alt, ctrl);
            return true;
        }
        if ((c & KeyCharacterMap.COMBINING_ACCENT) == 0) {
            final int fullChar;
            if (accent != 0) {
                fullChar = KeyCharacterMap.getDeadChar(accent, c);
                accent = 0;
                if (fullChar == 0)
                    return true;
            } else fullChar = c;
            consoleOutput.feed(-fullChar, shift, alt, ctrl);
            metaStateOnKey();
        } else {
            accent = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }
        return true;
    }

    @Override
    public boolean onKeyPreIme(final int keyCode, final KeyEvent event) {
        if (HwKeyMapManager.isBypassKey(event) || hwKeyMap.get(event) < 0)
            return super.onKeyPreIme(keyCode, event);
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                onKeyDown(keyCode, event);
                break;
            case KeyEvent.ACTION_MULTIPLE:
                onKeyMultiple(keyCode, event.getRepeatCount(), event);
                break;
            case KeyEvent.ACTION_UP:
                onKeyUp(keyCode, event);
                break;
        }
        return true;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return imeEnabled;
    }

    @Override
    public InputConnection onCreateInputConnection(final EditorInfo outAttrs) {
        if (!imeEnabled)
            return null;
        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_NULL;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_FULLSCREEN |
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING : 0);
        return new BaseInputConnection(this, false);
    }

    private final KeyIntervalDetector metaStateDoublePressDetector = new KeyIntervalDetector();

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (!isTextMode()) {
            if (consoleOutput != null)
                consoleOutput.feed(keyCode, true);
            return true;
        }
        final int mk = hwKeyMap.get(event);
        if (isBypassKey(event, mk < 0))
            return super.onKeyDown(keyCode, event);
        final int ms = getMetaStateByKeycode(mk);
        if (ms != 0) {
            final int toggleMode = hwKeyMap.getToggleMode(event);
            final long interval = metaStateDoublePressDetector.sample(event);
            final boolean isDouble = interval >= 0 && interval <= hwDoubleKeyPressInterval;
            if (isDouble) {
                switch (toggleMode) {
                    case HwKeyMap.TOGGLE_ONESHOT:
                    case HwKeyMap.TOGGLE_ON_OFF:
                        metaStateToggleOneShot ^= ms;
                        metaStateToggle |= ms;
                        break;
                }
            } else {
                if ((metaStateToggle & ms) == 0)
                    switch (toggleMode) {
                        case HwKeyMap.TOGGLE_ONESHOT:
                            metaStateToggleOneShot |= ms;
                            metaStateToggle |= ms;
                            break;
                        case HwKeyMap.TOGGLE_ON_OFF:
                            metaStateToggleOneShot &= ~ms;
                            metaStateToggle |= ms;
                            break;
                    }
                else metaStateToggle &= ~ms;
            }
            metaState |= ms;
        }
        send(event);
        invalidateHwMetaIndication();
        return true;
    }

    @Override
    public boolean onKeyMultiple(final int keyCode, final int repeatCount, final KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
            if (consoleOutput != null) {
                final String cc = event.getCharacters();
                if (cc != null) consoleOutput.feed(cc);
            }
            return true;
        }
        final int mk = hwKeyMap.get(event);
        if (isBypassKey(event, mk < 0))
            return super.onKeyMultiple(keyCode, repeatCount, event);
        return true;
    }

    @Override
    public boolean onKeyLongPress(final int keyCode, final KeyEvent event) {
        final int mk = hwKeyMap.get(event);
        if (isBypassKey(event, mk < 0))
            return super.onKeyLongPress(keyCode, event);
        return true;
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if (!isTextMode()) {
            if (consoleOutput != null)
                consoleOutput.feed(keyCode, false);
            return true;
        }
        final int mk = hwKeyMap.get(event);
        if (isBypassKey(event, mk < 0))
            return super.onKeyUp(keyCode, event);
        metaState &= ~getMetaStateByKeycode(mk);
        invalidateHwMetaIndication();
        return true;
    }

    private static boolean isBypassKey(@NonNull final KeyEvent event, final boolean notRemapped) {
        return HwKeyMapManager.isBypassKey(event) || (notRemapped && event.isSystem());
    }

    @Override
    protected void onConfigurationChanged(final Configuration newConfig) {
//        Log.i("onConfigurationChanged", String.format("kh: %04X; hkh: %04X", newConfig.keyboardHidden, newConfig.hardKeyboardHidden));
        applyConfig(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPress(final int primaryCode) {
        if (primaryCode == ExtKeyboard.KEYCODE_NONE)
            return;
        if (isTextMode()) {
            switch (primaryCode) {
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    setModifiers(getModifiers() ^ SHIFT);
                    setLedsByCode(primaryCode, (getModifiers() & SHIFT) != 0);
                    invalidateAllKeys();
                    wasKey = false;
                    break;
                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    ctrl = !ctrl;
                    setLedsByCode(primaryCode, ctrl);
                    invalidateModifierKeys(primaryCode);
                    wasKey = false;
                    break;
                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    alt = !alt;
                    setLedsByCode(primaryCode, alt);
                    invalidateModifierKeys(primaryCode);
                    wasKey = false;
                    break;
            }
        } else {
            switch (primaryCode) {
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    setModifiers(getModifiers() | SHIFT);
                    break;
                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    ctrl = true;
                    break;
                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    alt = true;
                    break;
            }
            if (consoleOutput != null)
                consoleOutput.feed(primaryCode, true);
        }
    }

//    @Override
//    protected boolean onLongPress(Keyboard.Key popupKey) {
//        return super.onLongPress(popupKey);
//    }

    @Override
    public void onKey(final int primaryCode, final int modifiers, final int modifiersMask) {
        if (primaryCode == ExtKeyboard.KEYCODE_NONE)
            return;
        if (isTextMode()) {
            switch (primaryCode) {
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    if (wasKey) {
                        setModifiers(getModifiers() & ~SHIFT);
                        setLedsByCode(primaryCode, false);
                        invalidateAllKeys();
                    }
                    break;
                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    if (wasKey) {
                        ctrl = false;
                        setLedsByCode(primaryCode, false);
                        invalidateModifierKeys(primaryCode);
                    }
                    break;
                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    if (wasKey) {
                        alt = false;
                        setLedsByCode(primaryCode, false);
                        invalidateModifierKeys(primaryCode);
                    }
                    break;
                default:
                    wasKey = true;
                    if (consoleOutput == null)
                        return;
                    consoleOutput.feed(primaryCode,
                            (modifiersMask & SHIFT) != 0 ?
                                    (modifiers & SHIFT) != 0 :
                                    ((getModifiers() & SHIFT) != 0),
                            (modifiersMask & ALT) != 0 ?
                                    (modifiers & ALT) != 0 : alt,
                            (modifiersMask & CTRL) != 0 ?
                                    (modifiers & CTRL) != 0 : ctrl);
            }
        }
    }

    @Override
    public void onRelease(final int primaryCode) {
        if (primaryCode == ExtKeyboard.KEYCODE_NONE)
            return;
        if (!isTextMode()) {
            switch (primaryCode) {
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    setModifiers(getModifiers() & ~SHIFT);
                    break;
                case KeyEvent.KEYCODE_CTRL_LEFT:
                case KeyEvent.KEYCODE_CTRL_RIGHT:
                    ctrl = false;
                    break;
                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                    alt = false;
                    break;
            }
            if (consoleOutput != null)
                consoleOutput.feed(primaryCode, false);
        }
    }

    @Override
    public void onText(final CharSequence text) {
        consoleOutput.feed(text.toString());
    }
}
