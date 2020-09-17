package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;

import green_green_avk.anotherterm.ConsoleInput;
import green_green_avk.anotherterm.ConsoleOutput;
import green_green_avk.anotherterm.HwKeyMapManager;
import green_green_avk.anotherterm.R;

public class ConsoleKeyboardView extends ExtKeyboardView implements
        KeyboardView.OnKeyboardActionListener, ConsoleInput.OnInvalidateSink {
    protected ConsoleInput consoleInput = null;
    protected ConsoleOutput consoleOutput = null;

    protected boolean ctrl = false;
    protected boolean alt = false;

    protected boolean wasKey = false;

    protected boolean imeEnabled = false;

    protected int keyHeightDp = 0;
    @NonNull
    private HwKeyMap hwKeyMap = HwKeyMap.DEFAULT;

    public static class State {
        private boolean init = false;
        private boolean useIme = false;

        public void save(@NonNull final ConsoleKeyboardView v) {
            useIme = v.isIme();
            init = true;
        }

        public void apply(@NonNull final ConsoleKeyboardView v) {
            if (!init) return;
            v.useIme(useIme);
        }
    }

    public ConsoleKeyboardView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConsoleKeyboardView(final Context context, @Nullable final AttributeSet attrs,
                               final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConsoleKeyboardView(final Context context, @Nullable final AttributeSet attrs,
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

    @Override
    public void onInvalidateSink(@Nullable final Rect rect) {
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
    }

    @Override
    public void onInvalidateSinkResize(final int cols, final int rows) {
    }

    private void applyConfig(@NonNull final Configuration cfg) {
        final Resources res = getContext().getResources();
        final float keyW = cfg.screenWidthDp / cfg.fontScale / 20;
        final int kbdRes =
                keyW >= res.getDimension(R.dimen.kbd_key_size)
                        / res.getDisplayMetrics().scaledDensity
                        ? R.xml.console_keyboard_wide : R.xml.console_keyboard;
        final ExtKeyboard.Configuration kc = new ExtKeyboard.Configuration();
        kc.keyHeight = (int) (keyHeightDp * res.getDisplayMetrics().density);
        setKeyboard(new ExtKeyboard(getContext(), kbdRes, kc));
        useIme(isIme());
    }

    public void setConsoleInput(@NonNull final ConsoleInput consoleInput) {
        this.consoleInput = consoleInput;
        this.consoleOutput = this.consoleInput.consoleOutput;
        this.consoleInput.addOnInvalidateSink(this);
        onInvalidateSink(null);
    }

    public void unsetConsoleInput() {
        if (consoleInput != null) consoleInput.removeOnInvalidateSink(this);
        consoleOutput = null;
        consoleInput = null;
    }

    @Override
    public boolean getAutoRepeat() {
        return consoleOutput == null || consoleOutput.keyAutorepeat;
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

    public void clipboardPaste(@Nullable final String v) {
        if (consoleOutput == null || v == null) return;
        consoleOutput.paste(v);
    }

    @Override
    public void onWindowFocusChanged(final boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) useIme(isIme());
    }

    protected void _showIme() {
        final InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imeEnabled = true;
        requestFocus();
        imm.restartInput(this);
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
    }

    protected void _hideIme() {
        final InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (imm.isActive(this)) imm.hideSoftInputFromWindow(getWindowToken(), 0);
        imeEnabled = false;
    }

    protected final Runnable rShowSelf = new Runnable() {
        @Override
        public void run() {
            final Context ctx = getContext();
            if (ctx instanceof Activity)
                ((Activity) ctx).getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED |
                                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
            mHidden = false;
            requestLayout();
        }
    };

    protected final Runnable rHideSelf = new Runnable() {
        @Override
        public void run() {
            final Context ctx = getContext();
            if (ctx instanceof Activity) {
                ((Activity) ctx).getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED |
                                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            mHidden = true;
            requestLayout();
        }
    };

    protected void showIme(final boolean v) {
        mHandler.removeCallbacks(rShowSelf);
        mHandler.removeCallbacks(rHideSelf);
        if (v) {
            rHideSelf.run();
            _showIme();
        } else {
            mHandler.postDelayed(rShowSelf, 500);
            _hideIme();
        }
    }

    public boolean isIme() {
        return isHidden();
    }

    public void useIme(final boolean v) {
        if (getResources().getConfiguration().hardKeyboardHidden ==
                Configuration.HARDKEYBOARDHIDDEN_YES && ViewCompat.isAttachedToWindow(this))
            showIme(v); // Hardware keyboard backspace key suppression bug workaround
        else
            setHidden(v);
    }

    @NonNull
    public HwKeyMap getHwKeyMap() {
        return hwKeyMap;
    }

    public void setHwKeyMap(@Nullable final HwKeyMap hwKeyMap) {
        this.hwKeyMap = hwKeyMap == null ? HwKeyMap.DEFAULT : hwKeyMap;
        metaStateFilterCache.clear();
    }

    private static int getMetaStateByKeycode(final int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:
                return KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON;
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                return KeyEvent.META_SHIFT_RIGHT_ON | KeyEvent.META_SHIFT_ON;
            case KeyEvent.KEYCODE_ALT_LEFT:
                return KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON;
            case KeyEvent.KEYCODE_ALT_RIGHT:
                return KeyEvent.META_ALT_RIGHT_ON | KeyEvent.META_ALT_ON;
            case KeyEvent.KEYCODE_CTRL_LEFT:
                return KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON;
            case KeyEvent.KEYCODE_CTRL_RIGHT:
                return KeyEvent.META_CTRL_RIGHT_ON | KeyEvent.META_CTRL_ON;
            case KeyEvent.KEYCODE_META_LEFT:
                return KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_ON;
            case KeyEvent.KEYCODE_META_RIGHT:
                return KeyEvent.META_META_RIGHT_ON | KeyEvent.META_META_ON;
            case KeyEvent.KEYCODE_SYM:
                return KeyEvent.META_SYM_ON;
            case KeyEvent.KEYCODE_FUNCTION:
                return KeyEvent.META_FUNCTION_ON;
        }
        return 0;
    }

    private static final int META_ALL_MASK = KeyEvent.normalizeMetaState(-1);
    private static final int META_DEF_MASK = META_ALL_MASK
            & ~(KeyEvent.META_ALT_ON
            | KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_RIGHT_ON
            | KeyEvent.META_CTRL_ON
            | KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_RIGHT_ON);
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
        final int id = hwKeyMap.getDevId(event);
        if (id < 0) return metaStateFilterDefault;
        final int[] r = metaStateFilterDefault.clone();
        final int idx = metaStateFilterCache.indexOfKey(id);
        if (idx >= 0) return metaStateFilterCache.valueAt(idx);
        for (final int k : modifierKeys) {
            final int t = hwKeyMap.get(k, id);
            if (t == HwKeyMap.KEYCODE_ACTION_DEFAULT) continue;
            final int m = getMetaStateByKeycode(k);
            if (t == HwKeyMap.KEYCODE_ACTION_BYPASS) {
                r[0] &= ~m;
                r[1] |= m;
            } else {
                r[0] &= ~m;
                r[1] &= ~m;
            }
        }
        metaStateFilterCache.put(id, r);
        return r;
    }

    private int metaState = 0; // For the custom modifiers mapping.
    private int accent = 0;

    private boolean send(@NonNull final KeyEvent event) {
        if (consoleOutput == null) return false;
        final int[] filter = getMetaStateFilter(event);
        final int eventMetaState =
                KeyEvent.normalizeMetaState((event.getMetaState() & filter[0]) | metaState);
        final boolean shift = (eventMetaState & KeyEvent.META_SHIFT_ON) != 0;
        final boolean alt = (eventMetaState & KeyEvent.META_ALT_ON) != 0;
        final boolean ctrl = (eventMetaState & KeyEvent.META_CTRL_ON) != 0;
        int code = hwKeyMap.get(event);
        if (code < 0) code = event.getKeyCode();
        final String r = consoleOutput.getKeySeq(code, shift, alt, ctrl);
        if (r != null) {
            consoleOutput.feed(r);
            return true;
        }
        final int c = event.getKeyCharacterMap().get(code,
                KeyEvent.normalizeMetaState(event.getMetaState() & filter[1]));
        if (c == 0) return false;
        if ((c & KeyCharacterMap.COMBINING_ACCENT) == 0) {
            final int fullChar;
            if (accent != 0) {
                fullChar = KeyCharacterMap.getDeadChar(accent, c);
                accent = 0;
                if (fullChar == 0) return true;
            } else fullChar = c;
            consoleOutput.feed(-fullChar, shift, alt, ctrl);
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
        if (!imeEnabled) return null;
        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_NULL;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_FULLSCREEN |
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING : 0);
        return new BaseInputConnection(this, false);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        final int mk = hwKeyMap.get(event);
        if (isBypassKey(event, mk < 0)) return super.onKeyDown(keyCode, event);
        metaState |= getMetaStateByKeycode(mk);
        send(event);
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
        final int mk = hwKeyMap.get(event);
        if (isBypassKey(event, mk < 0)) return super.onKeyUp(keyCode, event);
        metaState &= ~getMetaStateByKeycode(mk);
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
        switch (primaryCode) {
            case ExtKeyboard.KEYCODE_NONE:
                return;
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                setAltKeys(getAltKeys() ^ 1);
                setLedsByCode(primaryCode, getAltKeys() != 0);
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
    }

//    @Override
//    protected boolean onLongPress(Keyboard.Key popupKey) {
//        return super.onLongPress(popupKey);
//    }

    @Override
    public void onKey(final int primaryCode, final int[] keyCodes) {
        switch (primaryCode) {
            case ExtKeyboard.KEYCODE_NONE:
                return;
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
                if (wasKey) {
                    setAltKeys(0);
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
                if (consoleOutput == null) return;
                consoleOutput.feed(primaryCode, getAltKeys() != 0, alt, ctrl);
        }
    }

    @Override
    public void onRelease(final int primaryCode) {
    }

    @Override
    public void onText(final CharSequence text) {
        consoleOutput.feed(text.toString());
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeUp() {
    }

    @Override
    public void swipeDown() {
    }
}
