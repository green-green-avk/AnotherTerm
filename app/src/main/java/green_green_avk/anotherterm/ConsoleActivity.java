package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.NoSuchElementException;

import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendUiInteractionActivityCtx;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.ui.ConsoleKeyboardView;
import green_green_avk.anotherterm.ui.ConsoleScreenView;
import green_green_avk.anotherterm.ui.FontProvider;
import green_green_avk.anotherterm.ui.MouseButtonsWorkAround;
import green_green_avk.anotherterm.ui.ScreenMouseView;
import green_green_avk.anotherterm.ui.ScrollableView;
import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.ui.VisibilityAnimator;

public final class ConsoleActivity extends AppCompatActivity
        implements ConsoleInput.OnInvalidateSink, ScrollableView.OnScroll,
        ConsoleScreenView.OnStateChange {

    private int mSessionKey = -1;
    private Session mSession = null;
    private int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private ConsoleScreenView mCsv = null;
    private ConsoleKeyboardView mCkv = null;
    private ScreenMouseView mSmv = null;
    private View mBell = null;
    private Animation mBellAnim = null;
    private VisibilityAnimator mScrollHomeVA = null;

    private ViewGroup wNavBar = null;
    private View wOptionsMenu = null;

    private TextView wTitle = null;
    private ImageView wMouseMode = null;

    private View wConnecting = null;

    @Keep
    private final ConsoleService.Listener sessionsListener = new ConsoleService.Listener() {
        @Override
        protected void onSessionChange(final int key) {
            if (key != mSessionKey) return;
            if (ConsoleService.isSessionTerminated(mSessionKey)) {
                finish();
                return;
            }
            invalidateWakeLock();
            invalidateLoadingState();
        }
    };

    private void invalidateWakeLock() {
        if (mSession == null) return;
        mCkv.setLedsByCode(C.KEYCODE_LED_WAKE_LOCK, mSession.backend.wrapped.isWakeLockHeld());
        mCkv.invalidateModifierKeys(C.KEYCODE_LED_WAKE_LOCK);
    }

    private void invalidateLoadingState() {
        if (mSession == null) return;
        wConnecting.setVisibility(mSession.backend.isConnecting() ? View.VISIBLE : View.GONE);
    }

    private int getFirstSessionKey() {
        return ConsoleService.sessionKeys.listIterator(0).next();
    }

    private int getLastSessionKey() {
        return ConsoleService.sessionKeys.listIterator(ConsoleService.sessionKeys.size()).previous();
    }

    private static int asSize(final Object o) {
        if (o instanceof Integer || o instanceof Long) return (int) o;
        return 0;
    }

    /*
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) UiUtils.hideSystemUi(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        UiUtils.hideSystemUi(this);
        return super.dispatchTouchEvent(ev);
    }
    */

    @Override
    public void onMultiWindowModeChanged(final boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        getWindow().setFlags(isInMultiWindowMode ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ConsoleService.sessionKeys.size() <= 0) {
            finish();
            return;
        }
        final Intent intent = getIntent();
        if (!intent.hasExtra(C.IFK_MSG_SESS_KEY)) {
            if (intent.getBooleanExtra(C.IFK_MSG_SESS_TAIL, false)) {
                mSessionKey = getLastSessionKey();
            } else {
                mSessionKey = getFirstSessionKey();
            }
        } else {
            mSessionKey = intent.getIntExtra(C.IFK_MSG_SESS_KEY, 0);
        }
        try {
            mSession = ConsoleService.getSession(mSessionKey);
        } catch (final NoSuchElementException e) {
            finish();
            return;
        }

        screenOrientation = mSession.uiState.screenOrientation;
        setRequestedOrientation(screenOrientation);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isInMultiWindowMode())
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.console_activity);
//        UiUtils.setHiddenSystemUi(this);
//        UiUtils.setShrinkBottomWhenCovered(this);

        mCsv = findViewById(R.id.screen);
        mCkv = findViewById(R.id.keyboard);
        mSmv = findViewById(R.id.mouse);
        mBell = findViewById(R.id.bell);
        mBellAnim = AnimationUtils.loadAnimation(this, R.anim.blink_ring);
        mScrollHomeVA = new VisibilityAnimator(findViewById(R.id.scrollHome));

        wNavBar = findViewById(R.id.nav_bar);
        wOptionsMenu = findViewById(R.id.action_options_menu);

        wTitle = findViewById(R.id.title);
        wMouseMode = findViewById(R.id.action_mouse_mode);

        wConnecting = findViewById(R.id.connecting);

        registerForContextMenu(wOptionsMenu);

        final FontProvider fp = new ConsoleFontProvider();
        mCsv.setFont(fp);
        mCkv.setFont(fp); // Old Android devices have no glyphs for some special symbols

        mCsv.setFontSize(((App) getApplication()).settings.terminal_font_default_size_sp
                * getResources().getDisplayMetrics().scaledDensity);

/*
        mCsv.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(final View v, final MotionEvent event) {
//                        UiUtils.hideSystemUi(ConsoleActivity.this); // For earlier versions
                        return false;
                    }
                }
        );
*/

        setTitle(mSession.input.currScrBuf.windowTitle);

        mCsv.setConsoleInput(mSession.input);
        mCkv.setConsoleInput(mSession.input);
        mSession.input.addOnInvalidateSink(this);
        mCsv.onScroll = this;
        mCsv.onStateChange = this;

        mCsv.setScreenSize(asSize(mSession.connectionParams.get("screen_cols")),
                asSize(mSession.connectionParams.get("screen_rows")));
        mSession.uiState.csv.apply(mCsv);

        ConsoleService.addListener(sessionsListener);
        invalidateWakeLock();
        invalidateLoadingState();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        onInvalidateSink(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ConsoleService.sessionKeys.contains(mSessionKey)) {
            finish();
            return;
        }
        final int navBarH = (int) (((App) getApplication()).settings.terminal_key_height_dp
                * getResources().getDisplayMetrics().density);
        if (wNavBar.getLayoutParams().height != navBarH) {
            wNavBar.getLayoutParams().height = navBarH;
            wNavBar.requestLayout();
        }
        mCsv.setSelectionPadSize(((App) getApplication()).settings.terminal_selection_pad_size_dp
                * getResources().getDisplayMetrics().density);
        mCsv.setKeyHeightDp(((App) getApplication()).settings.terminal_key_height_dp);
        mCsv.setScrollFollowHistoryThreshold((float) ((App) getApplication()).settings
                .terminal_scroll_follow_history_threshold / 100);
        mCkv.setAutoRepeatAllowed(((App) getApplication()).settings.terminal_key_repeat);
        mCkv.setAutoRepeatDelay(((App) getApplication()).settings.terminal_key_repeat_delay);
        mCkv.setAutoRepeatInterval(((App) getApplication()).settings.terminal_key_repeat_interval);
        mCkv.setKeyHeightDp(((App) getApplication()).settings.terminal_key_height_dp);
        mSmv.setButtons("wide".equals(((App) getApplication()).settings.terminal_mouse_layout) ?
                R.layout.screen_mouse_buttons_wide : R.layout.screen_mouse_buttons);
        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(this);
    }

    @Override
    protected void onPause() {
        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(null);
        mSession.uiState.csv.save(mCsv);
        mSession.uiState.screenOrientation = screenOrientation;
        mSession.thumbnail = mCsv.makeThumbnail(256, 128);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_console, menu);
        if (mSession != null) {
            final BackendModule be = mSession.backend.wrapped;
            for (final Map.Entry<Method, BackendModule.ExportedUIMethod> m :
                    BackendsList.get(be.getClass()).meta.methods.entrySet()) {
                final Class<?>[] paramTypes = m.getKey().getParameterTypes();
                final Class<?> retType = m.getKey().getReturnType();
                if (paramTypes.length == 0 && retType == Void.TYPE) {
                    final MenuItem mi = menu.add(Menu.NONE, Menu.NONE,
                            100 + m.getValue().order(), m.getValue().titleRes());
                    mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(final MenuItem item) {
                            be.callMethod(m.getKey());
                            return true;
                        }
                    });
                } else if (paramTypes.length == 1 && retType == Void.TYPE) {
                    final Annotation[] aa = m.getKey().getParameterAnnotations()[0];
                    for (final Annotation a : aa) {
                        if (a instanceof BackendModule.ExportedUIMethodEnum) {
                            if (paramTypes[0] != Integer.TYPE) break;
                            final SubMenu sm = menu.addSubMenu(Menu.NONE, Menu.NONE,
                                    100 + m.getValue().order(), m.getValue().titleRes());
                            sm.getItem().setTitle(m.getValue().titleRes());
                            if (m.getValue().longTitleRes() != 0)
                                sm.setHeaderTitle(m.getValue().longTitleRes());
                            final BackendModule.ExportedUIMethodEnum ae =
                                    (BackendModule.ExportedUIMethodEnum) a;
                            for (int ai = 0; ai < ae.values().length; ai++) {
                                final int value = ae.values()[ai];
                                final MenuItem mi = sm.add(ae.titleRes()[ai]);
                                mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(final MenuItem item) {
                                        be.callMethod(m.getKey(), value);
                                        return true;
                                    }
                                });
                            }
                            break;
                        }
                    }
                } else if (paramTypes.length == 2 && paramTypes[0] == Long.TYPE &&
                        paramTypes[1] == Long.TYPE && retType == Long.TYPE) {
                    final BackendModule.ExportedUIMethodFlags a =
                            m.getKey().getAnnotation(BackendModule.ExportedUIMethodFlags.class);
                    if (a != null) {
                        final MenuItem mi = menu.add(Menu.NONE, Menu.NONE,
                                100 + m.getValue().order(), m.getValue().titleRes());
                        mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(final MenuItem item) {
                                final long bits = (long) be.callMethod(m.getKey(), 0L, 0L);
                                final boolean[] values = new boolean[a.values().length];
                                final String[] titles = new String[a.values().length];
                                for (int ai = 0; ai < a.values().length; ai++) {
                                    values[ai] = (bits & a.values()[ai]) == a.values()[ai];
                                    titles[ai] = getString(a.titleRes()[ai]);
                                }
                                new AlertDialog.Builder(ConsoleActivity.this)
                                        .setTitle(m.getValue().titleRes())
                                        .setMultiChoiceItems(titles, values,
                                                new DialogInterface.OnMultiChoiceClickListener() {
                                                    @Override
                                                    public void onClick(final DialogInterface dialog,
                                                                        final int which,
                                                                        final boolean isChecked) {
                                                        be.callMethod(m.getKey(),
                                                                isChecked ?
                                                                        a.values()[which] : 0L,
                                                                a.values()[which]);
                                                    }
                                                })
                                        .setCancelable(true)
                                        .show();
                                return true;
                            }
                        });
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        if (mSession != null)
            menu.findItem(R.id.action_charset).setTitle(getString(R.string.action_charset_p_s_p,
                    mSession.output.getCharset().name()));
        return true;
    }

    // It seems, Google restricts using of the options menu without an action bar in some old APIs
    // right after the Honeycomb:
    // https://android.developreference.com/article/24936634/openOptionsMenu+function+not+working+in+ICS%3F
    // Preserve *OptionsMenu style definitions at the moment though.
    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v,
                                    final ContextMenu.ContextMenuInfo menuInfo) {
        onCreateOptionsMenu(menu);
        onPrepareOptionsMenu(menu);
        final MenuItem.OnMenuItemClickListener mil = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem item) {
                return onOptionsItemSelected(item);
            }
        };
        wrapOptionsMenu(menu, mil);
    }

    private void wrapOptionsMenu(@NonNull final Menu menu,
                                 @NonNull final MenuItem.OnMenuItemClickListener mil) {
        for (int mii = 0; mii < menu.size(); ++mii) {
            final MenuItem mi = menu.getItem(mii);
            if (mi.getItemId() != Menu.NONE) mi.setOnMenuItemClickListener(mil);
            if (mi.hasSubMenu()) wrapOptionsMenu(mi.getSubMenu(), mil);
        }
    }

    @Override
    public void onInvalidateSink(@Nullable final Rect rect) {
        if (mSession != null) {
            if (mSession.input.currScrBuf.windowTitle != null)
                setTitle(mSession.input.currScrBuf.windowTitle);
            final boolean ms = mSession.output.isMouseSupported();
            if (ms != (wMouseMode.getVisibility() == View.VISIBLE)) {
                if (!ms) turnOffMouseMode();
                wMouseMode.setVisibility(ms ? View.VISIBLE : View.GONE);
            }
            if (mSession.input.getBell() != 0) {
                if (!mBellAnim.hasStarted() || mBellAnim.hasEnded())
                    mBell.startAnimation(mBellAnim);
            }
        }
    }

    @Override
    protected void onTitleChanged(final CharSequence title, final int color) {
        super.onTitleChanged(title, color);
        wTitle.setText(title);
        if (color != 0) wTitle.setTextColor(color);
    }

    @Override
    public void onScroll(@NonNull final ScrollableView scrollableView) {
        mScrollHomeVA.setVisibility(scrollableView.scrollPosition.y + 0.1F <
                Math.min(0F, scrollableView.getBottomScrollLimit()) ?
                View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onSelectionModeChange(final boolean mode) {
        if (mode) turnOffMouseMode();
    }

    @Override
    public void onFontSizeChange(final float fontSize) {
        final int v = Math.min(Math.round(fontSize), wTitle.getHeight());
        try {
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(wTitle, v,
                    TextViewCompat.getAutoSizeMaxTextSize(wTitle),
                    TextViewCompat.getAutoSizeStepGranularity(wTitle), TypedValue.COMPLEX_UNIT_PX);
        } catch (final IllegalArgumentException ignored) {
        }
    }

    private void turnOffMouseMode() {
        mCsv.setMouseMode(false);
        wMouseMode.setImageState(new int[]{}, true);
        if (mSmv.getVisibility() != View.GONE)
            mSmv.setVisibility(View.GONE);
    }

    public void onNavUp(final View v) {
        final Intent pa = getSupportParentActivityIntent();
        if (pa != null) supportNavigateUpTo(pa);
    }

    public void onMouseMode(final View v) {
        mCsv.setMouseMode(!mCsv.getMouseMode());
        if (mCsv.getMouseMode()) {
            ((ImageView) v).setImageState(new int[]{android.R.attr.state_checked}, true);
            mSmv.setVisibility(View.VISIBLE);
        } else {
            ((ImageView) v).setImageState(new int[]{}, true);
            mSmv.setVisibility(View.GONE);
        }
    }

    public void onSwitchIme(final View v) {
        mCkv.useIme(!mCkv.isIme());
//        UiUtils.hideSystemUi(this);
    }

    public void onSelectMode(final View v) {
        mCsv.setSelectionMode(!mCsv.getSelectionMode());
    }

    public void onPaste(final View v) {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        if (!clipboard.hasPrimaryClip()) return;
        final ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() < 1) return;
        final ClipData.Item clipItem = clipData.getItemAt(0);
        if (clipItem == null) return;
        mCkv.clipboardPaste(clipItem.coerceToText(this).toString());
    }

    public void onMenu(final View v) {
//        openOptionsMenu();
        openContextMenu(v);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_charset: {
                if (mSession == null) return true;
                final int p = C.charsetList.indexOf(mSession.output.getCharset().name());
                final ArrayAdapter<String> a = new ArrayAdapter<>(this,
                        R.layout.dialogmenu_entry, C.charsetList);
                new AlertDialog.Builder(this)
                        .setSingleChoiceItems(a, p, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                if (mSession == null) return;
                                final String charsetStr = a.getItem(which);
                                try {
                                    final Charset charset = Charset.forName(charsetStr);
                                    mSession.input.setCharset(charset);
                                    mSession.output.setCharset(charset);
                                } catch (IllegalArgumentException e) {
                                    Log.e("Charset", charsetStr, e);
                                }
                                dialog.dismiss();
                            }
                        }).setCancelable(true).show();
                return true;
            }
            case R.id.action_keymap: {
                if (mSession == null) return true;
                TermKeyMapManagerUi.showList(this, new TermKeyMapAdapter.OnSelectListener() {
                    @Override
                    public void onSelect(final boolean isBuiltIn, final String name,
                                         final TermKeyMapRules rules, final String title) {
                        if (mSession == null) return;
                        mSession.output.setKeyMap(rules);
                    }
                }, mSession.output.getKeyMap());
                return true;
            }
            case R.id.action_terminal_screen: {
                if (mSession == null) return true;
                final ViewGroup v = (ViewGroup)
                        getLayoutInflater().inflate(R.layout.buffer_size_dialog, null);
                final EditText wWidth = v.findViewById(R.id.width);
                final EditText wHeight = v.findViewById(R.id.height);
                final RadioGroup wOrientation = v.findViewById(R.id.orientation);
                if (mCsv.resizeBufferXOnUi)
                    wWidth.setHint(getString(R.string.hint_int_value_p_auto_p,
                            mSession.input.currScrBuf.getWidth()));
                else {
                    wWidth.setText(String.valueOf(mSession.input.currScrBuf.getWidth()));
                    wWidth.setHint(R.string.hint_auto);
                }
                if (mCsv.resizeBufferYOnUi)
                    wHeight.setHint(getString(R.string.hint_int_value_p_auto_p,
                            mSession.input.currScrBuf.getHeight()));
                else {
                    wHeight.setText(String.valueOf(mSession.input.currScrBuf.getHeight()));
                    wHeight.setHint(R.string.hint_auto);
                }
                switch (screenOrientation) {
                    case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                        wOrientation.check(R.id.orientation_landscape);
                        break;
                    case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                        wOrientation.check(R.id.orientation_portrait);
                        break;
                    case ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR:
                        wOrientation.check(R.id.orientation_sensor);
                        break;
                    default:
                        wOrientation.check(R.id.orientation_default);
                }
                new AlertDialog.Builder(this)
                        .setView(v)
                        .setTitle(R.string.dialog_title_terminal_screen)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                UiUtils.hideIME((Dialog) dialog);
                                if (mSession != null) {
                                    int width;
                                    int height;
                                    try {
                                        width = Integer.parseInt(wWidth.getText().toString());
                                    } catch (final IllegalArgumentException e) {
                                        width = 0;
                                    }
                                    try {
                                        height = Integer.parseInt(wHeight.getText().toString());
                                    } catch (final IllegalArgumentException e) {
                                        height = 0;
                                    }
                                    mCsv.setScreenSize(width, height);
                                    mCsv.onInvalidateSink(null);
                                    switch (wOrientation.getCheckedRadioButtonId()) {
                                        case R.id.orientation_landscape:
                                            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                                            break;
                                        case R.id.orientation_portrait:
                                            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                                            break;
                                        case R.id.orientation_sensor:
                                            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
                                            break;
                                        default:
                                            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                                    }
                                    setRequestedOrientation(screenOrientation);
                                }
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                UiUtils.hideIME((Dialog) dialog);
                                dialog.cancel();
                            }
                        })
                        .setCancelable(true)
                        .show();
                return true;
            }
            case R.id.action_help: {
                startActivity(new Intent(this, InfoActivity.class)
                        .setData(Uri.parse("info://local/help")));
                return true;
            }
            case R.id.action_toggle_wake_lock: {
                if (mSession == null) return true;
                if (mSession.backend.wrapped.isWakeLockHeld())
                    mSession.backend.wrapped.releaseWakeLock();
                else mSession.backend.wrapped.acquireWakeLock();
                return true;
            }
            case R.id.action_terminate: {
                try {
                    ConsoleService.stopSession(mSessionKey);
                } catch (final NoSuchElementException ignored) {
                }
                finish();
                return true;
            }
        }
        return true;
    }

    final MouseButtonsWorkAround mbwa = new MouseButtonsWorkAround(this);

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        return mbwa.onDispatchTouchEvent(ev) ? mbwa.result : super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(final MotionEvent ev) {
        return mbwa.onDispatchGenericMotionEvent(ev) ? mbwa.result : super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(final KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP: {
                    if (mCsv.getFontSize() < 64F)
                        mCsv.setFontSize(mCsv.getFontSize() + 1F);
                    return true;
                }
                case KeyEvent.KEYCODE_VOLUME_DOWN: {
                    if (mCsv.getFontSize() > 4F)
                        mCsv.setFontSize(mCsv.getFontSize() - 1F);
                    return true;
                }
            }
        }
        return mbwa.onDispatchKeyEvent(event) ? mbwa.result : super.dispatchKeyEvent(event);
    }

    public void onScrollHome(final View v) {
        mCsv.doScrollTo(0F, 0F);
    }
}
