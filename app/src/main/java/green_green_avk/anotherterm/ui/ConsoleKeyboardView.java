package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.inputmethodservice.KeyboardView;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import green_green_avk.anotherterm.ConsoleInput;
import green_green_avk.anotherterm.ConsoleOutput;
import green_green_avk.anotherterm.R;

public class ConsoleKeyboardView extends ExtKeyboardView implements
        KeyboardView.OnKeyboardActionListener, ConsoleInput.OnInvalidateSink {
    protected ConsoleInput consoleInput = null;
    protected ConsoleOutput consoleOutput = null;

    protected boolean ctrl = false;
    protected boolean alt = false;

    protected boolean wasKey = false;

    protected int keyHeightDp = 0;

//    protected final SpannableStringBuilder softEditable = new SpannableStringBuilder();

    public ConsoleKeyboardView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConsoleKeyboardView(final Context context, final AttributeSet attrs,
                               final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ConsoleKeyboardView(final Context context, final AttributeSet attrs,
                               final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setOnKeyboardActionListener(this);
        setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK)
                    return false; // Don't prevent default behavior
                if ((event.getSource() & InputDevice.SOURCE_ANY & (
                        InputDevice.SOURCE_MOUSE
                                | InputDevice.SOURCE_STYLUS
                                | InputDevice.SOURCE_TRACKBALL
                )) != 0) return false; // Mouse right & middle buttons...
                if (event.getAction() != KeyEvent.ACTION_UP) return true;
//                Log.w("Hard_input", Integer.toString(keyCode));
                if (consoleOutput != null) consoleOutput.feed(event);
                return true;
            }
        });
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
    public void onInvalidateSink(final Rect rect) {
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

    private void applyConfig(final Configuration cfg) {
        final Resources res = getContext().getResources();
        final float keyW = cfg.screenWidthDp / cfg.fontScale / 20;
        final int kbdRes =
                keyW >= res.getDimension(R.dimen.kbd_key_size)
                        / res.getDisplayMetrics().scaledDensity
                        ? R.xml.console_keyboard_wide : R.xml.console_keyboard;
        final ExtKeyboard.Configuration kc = new ExtKeyboard.Configuration();
        kc.keyHeight = (int) (keyHeightDp * res.getDisplayMetrics().density);
        setKeyboard(new ExtKeyboard(getContext(), kbdRes, kc));
        if (cfg.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            showIme(isHidden());
        } else {
            showIme(false); // Hardware keyboard backspace key suppression bug workaround
        }
    }

    public void setConsoleInput(final ConsoleInput consoleInput) {
        this.consoleInput = consoleInput;
        this.consoleOutput = this.consoleInput.consoleOutput;
        this.consoleInput.addOnInvalidateSink(this);
        onInvalidateSink(null);
    }

    public void unsetConsoleInput() {
        consoleInput.removeOnInvalidateSink(this);
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

    public void setKeyHeightDp(final int keyHeight) {
        if (this.keyHeightDp != keyHeight) {
            this.keyHeightDp = keyHeight;
            if (getWindowToken() != null) // if attached
                applyConfig(getResources().getConfiguration());
        }
    }

    public void clipboardPaste(@Nullable final String v) {
        if (consoleOutput == null || v == null) return;
        consoleOutput.paste(v);
    }

    protected int getImeHeight() {
        final View v = getRootView();
        final Rect vr = new Rect();
        v.getWindowVisibleDisplayFrame(vr);
        final int h = v.getBottom() - vr.bottom;
        if (h < 0) return 0;
        return (h > UiUtils.getNavBarHeight(getContext())) ? h : 0;
    }

    @Override
    public void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    protected boolean imeIsShown = false;

    @Override
    protected void onWindowVisibilityChanged(final int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            if (isHidden()) useIme(true);
        } else
            _hideIme();
    }

    protected void _showIme() {
        final InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        imeIsShown = imm.showSoftInput(this, InputMethodManager.SHOW_FORCED);
    }

    protected void _hideIme() {
        final InputMethodManager imm = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imeIsShown = !imm.hideSoftInputFromWindow(getWindowToken(), 0);
        requestLayout();
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
                Configuration.HARDKEYBOARDHIDDEN_YES)
            showIme(v); // Hardware keyboard backspace key suppression bug workaround
        else
            setHidden(v);
    }

    @Override
    public InputConnection onCreateInputConnection(final EditorInfo outAttrs) {
        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_NULL;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE;
        // in any case event dispatching seems very unreliable here I have no idea why, so - full editor
        return new BaseInputConnection(this, true) {
            /*
                        @Override
                        public Editable getEditable() {
                            return softEditable;
                        }
            */
            @Override
            public boolean sendKeyEvent(final KeyEvent event) {
                if (event.getAction() != KeyEvent.ACTION_UP) return true;
//                Log.w("Soft_input", Character.toString((char) event.getUnicodeChar(event.getMetaState())));
                if (consoleOutput != null) consoleOutput.feed(event);
                return true;
            }

            @Override
            public boolean commitText(final CharSequence text, final int newCursorPosition) {
//                Log.w("Soft_input (commit)", text.toString());
                if (consoleOutput != null) consoleOutput.feed(text.toString());
                return super.commitText(text, newCursorPosition);
            }

        };
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
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
