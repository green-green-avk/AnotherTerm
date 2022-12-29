package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.math.MathUtils;
import androidx.core.widget.TextViewCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import green_green_avk.anotherterm.backends.BackendException;
import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendUiInteractionActivityCtx;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.backends.EventBasedBackendModuleWrapper;
import green_green_avk.anotherterm.ui.BackendUiDialogs;
import green_green_avk.anotherterm.ui.ChoreographerCompat;
import green_green_avk.anotherterm.ui.ConsoleKeyboardView;
import green_green_avk.anotherterm.ui.ConsoleScreenView;
import green_green_avk.anotherterm.ui.ExtToast;
import green_green_avk.anotherterm.ui.FontProvider;
import green_green_avk.anotherterm.ui.MessageLogView;
import green_green_avk.anotherterm.ui.MouseButtonsWorkAround;
import green_green_avk.anotherterm.ui.RichMenu;
import green_green_avk.anotherterm.ui.ScreenMouseView;
import green_green_avk.anotherterm.ui.ScrollableView;
import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.ui.VisibilityAnimator;
import green_green_avk.anotherterm.utils.LogMessage;
import green_green_avk.anotherterm.utils.Misc;

public final class AnsiConsoleActivity extends ConsoleActivity
        implements ConsoleInput.OnInvalidateSink, ScrollableView.OnScroll,
        ConsoleScreenView.OnStateChange {

    private AnsiSession mSession = null;
    private int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private boolean autoFitTerminal = false;
    private ConsoleScreenView mCsv = null;
    private ConsoleKeyboardView mCkv = null;
    private ScreenMouseView mSmv = null;
    private View mBell = null;
    private Animation mBellAnim = null;
    private VisibilityAnimator mScrollHomeVA = null;
    private ExtToast mScreenNote = null;

    private ViewGroup wNavBar = null;

    private ImageView wUp = null;
    private Drawable wUpImDef = null;
    private TextView wTitle = null;
    private ImageView wKeyboardMode = null;
    private ImageView wMouseMode = null;

    private View wConnecting = null;

    @Keep
    private final ConsoleService.Listener sessionsListener = new ConsoleService.Listener() {
        @Override
        protected void onSessionChange(final int key) {
            if (key != mSessionKey)
                return;
            if (ConsoleService.isSessionTerminated(mSessionKey)) {
                finish();
                return;
            }
            invalidateWakeLock();
            invalidateConnectionState();
        }
    };

    private void invalidateWakeLock() {
        if (mSession == null)
            return;
        final boolean v = mSession.backend.wrapped.isWakeLockHeld();
        mCkv.setLedsByCode(C.KEYCODE_LED_WAKE_LOCK, v);
        mCkv.invalidateModifierKeys(C.KEYCODE_LED_WAKE_LOCK);
        if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
            menuPopupWindow.getContentView().<CompoundButton>findViewById(R.id.wakelock)
                    .setChecked(v);
        }
    }

    private void invalidateConnectionState() {
        if (mSession == null)
            return;
        wConnecting.setVisibility(mSession.backend.isConnecting() ? View.VISIBLE : View.GONE);
        ((BackendUiDialogs) mSession.backend.wrapped.getUi())
                .setShowTerminateButton(!mSession.backend.isConnected());
    }

    private static int asSize(final Object o) {
        if (o instanceof Integer)
            return (int) o;
        if (o instanceof Long)
            return (int) (long) o;
        return 0;
    }

    private void applyKeepScreenOn() {
        getWindow().setFlags(mSession.uiState.keepScreenOn ?
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON : 0,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private float clampFontSize(final float size) {
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        return MathUtils.clamp(size,
                getResources().getInteger(R.integer.terminal_font_size_min_sp)
                        * dm.scaledDensity,
                getResources().getInteger(R.integer.terminal_font_size_max_sp)
                        * dm.scaledDensity);
    }

    // There are two dimensions, so a resize could lead to one more resize.
    private boolean inFitFontSize = false;

    private void fitFontSize() {
        if (inFitFontSize)
            return;
        inFitFontSize = true;
        try {
            final int width = mCsv.getWidth();
            final int height = mCsv.getHeight();
            float fsX = Float.POSITIVE_INFINITY;
            float fsY = Float.POSITIVE_INFINITY;
            final float base = 100F; // A reasonable value for the test.
            if (!mCsv.resizeBufferXOnUi || !mCsv.resizeBufferYOnUi) {
                final float[] charSize = mCsv.getCharSize(base);
                if (!mCsv.resizeBufferXOnUi) {
                    fsX = base * width / charSize[0] /
                            mSession.input.currScrBuf.getWidth();
                }
                if (!mCsv.resizeBufferYOnUi) {
                    fsY = base * height / charSize[1] /
                            mSession.input.currScrBuf.getHeight();
                }
                mCsv.setFontSize(clampFontSize(Math.min(fsX, fsY)));
            }
        } finally {
            inFitFontSize = false;
        }
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mSession = ConsoleService.getAnsiSession(mSessionKey);
        } catch (final NoSuchElementException e) {
            finish();
            return;
        }

        screenOrientation = mSession.uiState.screenOrientation;
        setRequestedOrientation(screenOrientation);
        applyKeepScreenOn();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isInMultiWindowMode())
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.ansi_console_activity);

        mCsv = findViewById(R.id.screen);
        mCkv = findViewById(R.id.keyboard);
        mSmv = findViewById(R.id.mouse);
        mBell = findViewById(R.id.bell);
        mBellAnim = AnimationUtils.loadAnimation(this, R.anim.blink_ring);
        mScrollHomeVA = new VisibilityAnimator(findViewById(R.id.scrollHome));
        mScreenNote = new ExtToast(this, R.layout.ansi_console_toast);

        wNavBar = findViewById(R.id.nav_bar);

        wUp = findViewById(R.id.action_nav_up);
        wUpImDef = wUp.getDrawable();
        wTitle = findViewById(R.id.title);
        wKeyboardMode = findViewById(R.id.action_ime);
        wMouseMode = findViewById(R.id.action_mouse_mode);

        wConnecting = findViewById(R.id.connecting);

        final boolean isNew = mSession.uiState.fontSizeDp == 0F;
        if (isNew) {
            autoFitTerminal =
                    Misc.toBoolean(mSession.connectionParams.get("font_size_auto"));
        } else {
            autoFitTerminal = mSession.uiState.fontSizeDp < 0F;
        }

        final FontProvider fp = new ConsoleFontProvider();
        mCsv.setFont(fp);
        mCkv.setFont(fp); // Old Android devices have no glyphs for some special symbols

        final App.Settings globalSettings = ((App) getApplication()).settings;
        final DisplayMetrics dm = getResources().getDisplayMetrics();

        if (isNew && !autoFitTerminal) {
            mSession.uiState.fontSizeDp =
                    globalSettings.terminal_font_default_size_sp *
                            (dm.scaledDensity / dm.density);
        }
        mCsv.setFontSize(mSession.uiState.fontSizeDp *
                getResources().getDisplayMetrics().density, false);

        switch (globalSettings.terminal_screen_keyboard_default_type) {
            case "ime":
                mCkv.setMode(ConsoleKeyboardView.MODE_IME);
                break;
            case "hidden":
                mCkv.setMode(ConsoleKeyboardView.MODE_HW_ONLY);
                break;
            default:
                mCkv.setMode(ConsoleKeyboardView.MODE_VISIBLE);
        }

        final RichMenu keyboardModeMenu =
                new RichMenu(R.layout.term_rich_menu_popup,
                        R.layout.term_rich_menu_entry);
        keyboardModeMenu.setItems(Arrays.asList(
                new RichMenu.Item(UiUtils.requireDrawable(this,
                        R.drawable.ic_keyboard_term),
                        getText(R.string.desc_builtin_keyboard),
                        () -> mCkv.setMode(ConsoleKeyboardView.MODE_VISIBLE)),
                new RichMenu.Item(UiUtils.requireDrawable(this,
                        R.drawable.ic_keyboard),
                        getText(R.string.desc_ime_keyboard),
                        () -> mCkv.setMode(ConsoleKeyboardView.MODE_IME)),
                new RichMenu.Item(UiUtils.requireDrawable(this,
                        R.drawable.ic_keyboard_hide),
                        getText(R.string.desc_hide_keyboard),
                        () -> mCkv.setMode(ConsoleKeyboardView.MODE_HW_ONLY))));
        keyboardModeMenu.wrap(wKeyboardMode);

        setSessionTitle(mSession.input.currScrBuf.windowTitle);

        mCsv.setConsoleInput(mSession.input);
        mCkv.setConsoleInput(mSession.input);
        mSession.input.addOnInvalidateSink(this);
        mCsv.onScroll = this;
        mCsv.onStateChange = this;

        if (isNew) {
            mCsv.setScreenSize(asSize(mSession.connectionParams.get("screen_cols")),
                    asSize(mSession.connectionParams.get("screen_rows")));
        }
        mSession.uiState.csv.apply(mCsv);
        mSession.uiState.ckv.apply(mCkv);
        if (mSession.uiState.mouseMode == AnsiSession.UiState.MouseMode.UNDEFINED)
            setMouseMode("overlaid".equals(globalSettings.terminal_ansi_screen_mouse_default_mode));
        else
            setMouseMode(mSession.uiState.mouseMode == AnsiSession.UiState.MouseMode.OVERLAID);

        ConsoleService.addListener(sessionsListener);
        invalidateWakeLock();
        invalidateConnectionState();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        onInvalidateSink(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCsv.unfreezeBlinking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ConsoleService.hasAnsiSession(mSessionKey)) {
            finish();
            return;
        }
        final App.Settings globalSettings = ((App) getApplication()).settings;
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        final int navBarH = (int) (globalSettings.terminal_key_height_dp
                * dm.density);
        if (wNavBar.getLayoutParams().height != navBarH) {
            wNavBar.getLayoutParams().height = navBarH;
            wNavBar.requestLayout();
        }
        mCsv.setSelectionPadSize(globalSettings.terminal_selection_pad_size_dp * dm.density);
        mCsv.setPopupOpacity(globalSettings.terminal_popup_opacity * 255 / 100);
        mCsv.setKeyHeightDp(globalSettings.terminal_key_height_dp);
        mCsv.setScrollFollowHistoryThreshold((float) globalSettings
                .terminal_scroll_follow_history_threshold / 100);
        mCkv.setAutoRepeatAllowed(globalSettings.terminal_key_repeat);
        mCkv.setAutoRepeatDelay(globalSettings.terminal_key_repeat_delay);
        mCkv.setAutoRepeatInterval(globalSettings.terminal_key_repeat_interval);
        mCkv.setKeyHeightDp(globalSettings.terminal_key_height_dp);
        mCkv.setHwKeyMap(HwKeyMapManager.get());
        mSmv.setButtons("wide".equals(globalSettings.terminal_mouse_layout) ?
                R.layout.screen_mouse_buttons_wide : R.layout.screen_mouse_buttons);
        UiUtils.setBackgroundAlpha(mScreenNote.getContentView().getBackground(),
                mCsv.getPopupOpacity(), Color.BLACK);
        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(this);
        if (getUseRecents())
            wUp.setImageResource(R.drawable.ic_recents);
        else
            wUp.setImageDrawable(wUpImDef);
        mSession.output.setMouseFocus(true);
    }

    @Override
    protected void onPause() {
        mSession.output.setMouseFocus(false);
        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(null);
        mSession.uiState.fontSizeDp = autoFitTerminal ? -1F : (mCsv.getFontSize() /
                getResources().getDisplayMetrics().density);
        mSession.uiState.csv.save(mCsv);
        mSession.uiState.ckv.save(mCkv);
        mSession.uiState.mouseMode = getMouseMode() ?
                AnsiSession.UiState.MouseMode.OVERLAID :
                AnsiSession.UiState.MouseMode.DIRECT;
        mSession.uiState.screenOrientation = screenOrientation;
        mSession.thumbnail = mCsv.makeThumbnail(256, 128);
        super.onPause();
    }

    @Override
    protected void onStop() {
        mCsv.freezeBlinking();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (menuPopupWindow != null)
            menuPopupWindow.dismiss();
        ConsoleService.removeListener(sessionsListener);
        if (mSession != null)
            mSession.input.removeOnInvalidateSink(this);
        if (mCkv != null)
            mCkv.unsetConsoleInput();
        if (mCsv != null)
            mCsv.unsetConsoleInput();
        super.onDestroy();
    }

    @Nullable
    private PopupWindow menuPopupWindow = null;

    private void processMenuPopupAction(@Nullable final Object arg) {
        if (arg instanceof BackendModule && mSession != null) {
            final BackendModule be = (BackendModule) arg;
            final int key;
            try {
                key = ConsoleService.startAnsiSession(this, mSession.connectionParams, be);
            } catch (final ConsoleService.Exception | BackendException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            finish();
            ConsoleActivity.showSession(this, key);
            return;
        }
        if (arg instanceof Intent) {
            startActivity((Intent) arg);
            return;
        }
    }

    @NonNull
    private PopupWindow createMenuPopup() {
        @SuppressLint("InflateParams") final View popupView =
                LayoutInflater.from(this).inflate(R.layout.console_menu, null);
        popupView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

// Module related
// --------------

        if (mSession != null) {
            final EventBasedBackendModuleWrapper wbe = mSession.backend;
            final BackendModule.Meta beMeta = BackendsList.get(wbe.wrapped.getClass()).meta;
            final CompoundButton terminate_on_disconnect_if_pes_0 =
                    popupView.findViewById(R.id.terminate_on_disconnect_if_pes_0);
            terminate_on_disconnect_if_pes_0
                    .setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (mSession == null)
                            return;
                        mSession.properties.terminateOnDisconnect = isChecked
                                ? AnsiSession.Properties.PROCESS_EXIT_STATUS_0
                                : AnsiSession.Properties.ALWAYS;
                    });
            terminate_on_disconnect_if_pes_0
                    .setVisibility((beMeta.getDisconnectionReasonTypes() &
                            BackendModule.DisconnectionReason.PROCESS_EXIT) ==
                            BackendModule.DisconnectionReason.PROCESS_EXIT ?
                            View.VISIBLE : View.GONE);
            final List<Map.Entry<Method, BackendModule.ExportedUIMethod>> uiMethods =
                    new LinkedList<>(beMeta.methods.entrySet());
            Collections.sort(uiMethods,
                    (o1, o2) -> Integer.compare(o1.getValue().order(), o2.getValue().order()));
            final ViewGroup moduleUiView = popupView.findViewById(R.id.module_ui);
            for (final Map.Entry<Method, BackendModule.ExportedUIMethod> m : uiMethods) {
                final Class<?>[] paramTypes = m.getKey().getParameterTypes();
                final Class<?> retType = m.getKey().getReturnType();
                if (paramTypes.length == 0) {
                    final TextView mi = (TextView) LayoutInflater.from(this)
                            .inflate(R.layout.module_ui_button,
                                    moduleUiView, false);
                    mi.setText(m.getValue().titleRes());
                    if (m.getValue().longTitleRes() != 0) {
                        final String desc = getString(m.getValue().longTitleRes());
                        mi.setContentDescription(desc);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            mi.setTooltipText(desc);
                    }
                    mi.setOnClickListener(item -> {
                        processMenuPopupAction(wbe.callWrappedMethod(m.getKey()));
                        if (menuPopupWindow != null)
                            menuPopupWindow.dismiss();
                    });
                    moduleUiView.addView(mi);
                } else if (paramTypes.length == 1 && retType == Void.TYPE) {
                    final Annotation[] aa = m.getKey().getParameterAnnotations()[0];
                    for (final Annotation a : aa) {
                        if (a instanceof BackendModule.ExportedUIMethodIntEnum
                                && paramTypes[0] == Integer.TYPE
                                || a instanceof BackendModule.ExportedUIMethodStrEnum
                                && paramTypes[0] == String.class) {
                            final int[] titles;
                            final Object[] values;
                            if (a instanceof BackendModule.ExportedUIMethodIntEnum) {
                                titles = ((BackendModule.ExportedUIMethodIntEnum) a).titleRes();
                                values = Misc.box(((BackendModule.ExportedUIMethodIntEnum) a)
                                        .values());
                            } else {
                                titles = ((BackendModule.ExportedUIMethodStrEnum) a).titleRes();
                                values = ((BackendModule.ExportedUIMethodStrEnum) a).values();
                            }
                            final ViewGroup sm = (ViewGroup) LayoutInflater.from(this)
                                    .inflate(R.layout.module_ui_group,
                                            moduleUiView, false);
                            sm.<TextView>findViewById(R.id.title)
                                    .setText(m.getValue().longTitleRes() != 0 ?
                                            m.getValue().longTitleRes() :
                                            m.getValue().titleRes());
                            final ViewGroup smg = sm.findViewById(R.id.content);
                            for (int i = 0; i < values.length; i++) {
                                final Object value = values[i];
                                final TextView mi = (TextView) LayoutInflater.from(this)
                                        .inflate(R.layout.module_ui_button,
                                                smg, false);
                                if (titles.length > i)
                                    mi.setText(titles[i]);
                                else
                                    mi.setText(value.toString());
                                mi.setOnClickListener(item -> {
                                    wbe.callWrappedMethod(m.getKey(), value);
                                    if (menuPopupWindow != null)
                                        menuPopupWindow.dismiss();
                                });
                                smg.addView(mi);
                            }
                            moduleUiView.addView(sm);
                            break;
                        }
                    }
                } else if (paramTypes.length == 2 && paramTypes[0] == Long.TYPE &&
                        paramTypes[1] == Long.TYPE && retType == Long.TYPE) {
                    final BackendModule.ExportedUIMethodFlags a =
                            m.getKey().getAnnotation(
                                    BackendModule.ExportedUIMethodFlags.class);
                    if (a != null) {
                        final TextView mi = (TextView) LayoutInflater.from(this)
                                .inflate(R.layout.module_ui_button,
                                        moduleUiView, false);
                        mi.setText(m.getValue().titleRes());
                        if (m.getValue().longTitleRes() != 0) {
                            final String desc = getString(m.getValue().longTitleRes());
                            mi.setContentDescription(desc);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                mi.setTooltipText(desc);
                        }
                        mi.setOnClickListener(item -> {
                            final long bits =
                                    (long) wbe.callWrappedMethod(m.getKey(), 0L, 0L);
                            final boolean[] values = new boolean[a.values().length];
                            final String[] titles = new String[a.values().length];
                            for (int ai = 0; ai < a.values().length; ai++) {
                                values[ai] = (bits & a.values()[ai]) == a.values()[ai];
                                titles[ai] = getString(a.titleRes()[ai]);
                            }
                            new AlertDialog.Builder(this)
                                    .setTitle(m.getValue().titleRes())
                                    .setMultiChoiceItems(titles, values,
                                            (dialog, which, isChecked) ->
                                                    wbe.callWrappedMethod(m.getKey(),
                                                            isChecked ?
                                                                    a.values()[which] : 0L,
                                                            a.values()[which]))
                                    .setCancelable(true)
                                    .show();
                        });
                        moduleUiView.addView(mi);
                    }
                }
            }
        }

// ==============

        final PopupWindow window = new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        window.setBackgroundDrawable(AppCompatResources.getDrawable(this,
                android.R.drawable.dialog_holo_light_frame));
        window.setSplitTouchEnabled(true);
        window.setAnimationStyle(android.R.style.Animation_Dialog);
        return window;
    }

    private void refreshMenuPopupOnDisconnectCond(@NonNull final View popupView) {
        popupView.<CompoundButton>findViewById(R.id.terminate_on_disconnect_if_pes_0)
                .setChecked(mSession.properties.terminateOnDisconnect
                        == AnsiSession.Properties.PROCESS_EXIT_STATUS_0);
    }

    private void refreshMenuPopup() {
        if (menuPopupWindow == null || !menuPopupWindow.isShowing())
            return;
        final View popupView = menuPopupWindow.getContentView();
        popupView.<CompoundButton>findViewById(R.id.horizontal_app_scrolling)
                .setChecked(mCsv.isAppHScrollEnabled());
        if (mSession != null) {
            final BackendModule be = mSession.backend.wrapped;
            popupView.findViewById(R.id.show_log)
                    .setVisibility(be.getLog() != null ? View.VISIBLE : View.GONE);
            popupView.<CompoundButton>findViewById(R.id.wakelock).setChecked(be.isWakeLockHeld());
            popupView.<CompoundButton>findViewById(R.id.keep_screen_on)
                    .setChecked(mSession.uiState.keepScreenOn);
            popupView.<TextView>findViewById(R.id.term_compliance)
                    .setText(mSession.input.getComplianceLevel() == 0 ?
                            R.string.label_term_compliance_vt52compat :
                            R.string.label_term_compliance_ansi);
            popupView.<TextView>findViewById(R.id.charset)
                    .setText(mSession.output.getCharset().name());
            popupView.<TextView>findViewById(R.id.keymap)
                    .setText(TermKeyMapManagerUi.getTitle(this,
                            mSession.output.getKeyMap()));
            final String w;
            if (mCsv.resizeBufferXOnUi)
                w = getString(R.string.hint_int_value_p_auto_p,
                        mSession.input.currScrBuf.getWidth());
            else
                w = String.valueOf(mSession.input.currScrBuf.getWidth());
            final String h;
            if (mCsv.resizeBufferYOnUi)
                h = getString(R.string.hint_int_value_p_auto_p,
                        mSession.input.currScrBuf.getHeight());
            else
                h = String.valueOf(mSession.input.currScrBuf.getHeight());
            popupView.<TextView>findViewById(R.id.screen_size)
                    .setText(getString(R.string.label_dims_s2, w, h));
            popupView.<CompoundButton>findViewById(R.id.terminate_on_disconnect)
                    .setChecked(mSession.properties.terminateOnDisconnectEnabled);
            refreshMenuPopupOnDisconnectCond(popupView);
            popupView.<CompoundButton>findViewById(R.id.wakelock_release_on_disconnect)
                    .setChecked(be.isReleaseWakeLockOnDisconnect());
        }
    }

    private void showMenuPopup(@NonNull final View view) {
        if (menuPopupWindow == null)
            menuPopupWindow = createMenuPopup();
        menuPopupWindow.showAsDropDown(view);
        refreshMenuPopup();
    }

    private final ChoreographerCompat choreographer = ChoreographerCompat.getInstance();

    private boolean invalidating = false;

    private void doInvalidateSink() {
        invalidating = false;
        if (mSession != null) {
            if (mSession.input.currScrBuf.windowTitle != null)
                setSessionTitle(mSession.input.currScrBuf.windowTitle);
            final boolean ms = mSession.output.isMouseSupported();
            if (ms != (wMouseMode.getVisibility() == View.VISIBLE)) {
                wMouseMode.setVisibility(ms ? View.VISIBLE : View.GONE);
                applyMouseMode();
            }
            if (mSession.input.getBell() != 0) {
                if (!mBellAnim.hasStarted() || mBellAnim.hasEnded())
                    mBell.startAnimation(mBellAnim);
            }
        }
    }

    @Override
    public void onInvalidateSink(@Nullable final Rect rect) {
        if (!invalidating) {
            invalidating = true;
            choreographer.post(this::doInvalidateSink);
        }
    }

    private final Point invalidatingSize = new Point();

    private void doInvalidateSinkResize() {
        final int cols = invalidatingSize.x;
        final int rows = invalidatingSize.y;
        invalidatingSize.set(0, 0);
        if (autoFitTerminal)
            fitFontSize();
        mScreenNote.setText(getString(R.string.label_dims_i2, cols, rows));
        mScreenNote.show(mCsv, 1000);
    }

    @Override
    public void onInvalidateSinkResize(final int cols, final int rows) {
        final boolean doPost = invalidatingSize.x <= 0 || invalidatingSize.y <= 0;
        invalidatingSize.set(cols, rows);
        if (doPost)
            choreographer.post(this::doInvalidateSinkResize);
    }

    public void setSessionTitle(@Nullable final CharSequence title) {
        if (title != null)
            setTitle(title);
    }

    @Override
    protected void onTitleChanged(final CharSequence title, final int color) {
        super.onTitleChanged(title, color);
        wTitle.setText(title);
        if (color != 0)
            wTitle.setTextColor(color);
    }

    @Override
    public void onScroll(@NonNull final ScrollableView scrollableView) {
        mScrollHomeVA.setVisibility(scrollableView.scrollPosition.y + 0.1F <
                Math.min(0F, scrollableView.getBottomScrollLimit()) ?
                View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onTerminalAreaResize(final int width, final int height) {
        if (autoFitTerminal && mSession != null) {
            fitFontSize();
        }
    }

    @Override
    public void onSelectionModeChange(final boolean mode) {
        applyMouseMode();
    }

    @Override
    public void onFontSizeChange(final float fontSize) {
        final int v = Math.min(Math.round(fontSize), wTitle.getHeight());
        try {
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(wTitle,
                    v,
                    TextViewCompat.getAutoSizeMaxTextSize(wTitle),
                    TextViewCompat.getAutoSizeStepGranularity(wTitle),
                    TypedValue.COMPLEX_UNIT_PX);
        } catch (final IllegalArgumentException ignored) {
        }
    }

    public void onNavUp(final View v) {
        final Intent pa = getSupportParentActivityIntent();
        if (pa == null)
            return;
        if (getUseRecents()) {
            startActivity(pa.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        supportNavigateUpTo(pa);
    }

    private boolean mouseMode = false;

    private void applyMouseMode() {
        mCsv.setMouseMode(mouseMode && mSession.output.isMouseSupported() &&
                !mCsv.getSelectionMode());
        if (mCsv.getMouseMode()) {
            wMouseMode.setImageState(new int[]{android.R.attr.state_checked},
                    true);
            mSmv.setVisibility(View.VISIBLE);
        } else {
            wMouseMode.setImageState(new int[]{}, true);
            mSmv.setVisibility(View.GONE);
        }
    }

    public boolean getMouseMode() {
        return mouseMode;
    }

    public void setMouseMode(final boolean overlaid) {
        mouseMode = overlaid;
        applyMouseMode();
    }

    public void onMouseMode(final View v) {
        setMouseMode(!getMouseMode());
    }

    public void onSelectMode(final View v) {
        mCsv.setSelectionMode(!mCsv.getSelectionMode());
    }

    public void onPaste(final View v) {
        final ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null)
            return;
        if (!clipboard.hasPrimaryClip())
            return;
        final ClipData clipData = clipboard.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() < 1)
            return;
        final ClipData.Item clipItem = clipData.getItemAt(0);
        if (clipItem == null)
            return;
        mCkv.clipboardPaste(clipItem.coerceToText(this).toString());
    }

    public void onMenu(final View v) {
        showMenuPopup(v);
    }

    public void onMenuTermCompliance(final View view) {
        if (mSession == null)
            return;
        final int p = mSession.input.getComplianceLevel() == 0 ? 1 : 0;
        final ArrayAdapter<String> a = new ArrayAdapter<>(this,
                R.layout.dialogmenu_entry, new String[]{
                getString(R.string.label_term_compliance_ansi),
                getString(R.string.label_term_compliance_vt52compat)
        });
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(a, p, (dialog, which) -> {
                    if (mSession == null)
                        return;
                    mSession.input.setComplianceLevel(which == 1 ?
                            0 : ConsoleInput.defaultComplianceLevel);
                    mSession.input.invalidateSink();
                    refreshMenuPopup();
                    dialog.dismiss();
                }).setCancelable(true).show();
    }

    public void onMenuCharset(final View view) {
        if (mSession == null)
            return;
        final int p = C.charsetList.indexOf(mSession.output.getCharset().name());
        final ArrayAdapter<String> a = new ArrayAdapter<>(this,
                R.layout.dialogmenu_entry, C.charsetList);
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(a, p, (dialog, which) -> {
                    if (mSession == null)
                        return;
                    final String charsetStr = a.getItem(which);
                    try {
                        final Charset charset = Charset.forName(charsetStr);
                        mSession.input.setCharset(charset);
                        mSession.output.setCharset(charset);
                    } catch (final IllegalArgumentException e) {
                        Log.e("Charset", charsetStr, e);
                    }
                    refreshMenuPopup();
                    dialog.dismiss();
                }).setCancelable(true).show();
    }

    public void onMenuKeymap(final View view) {
        if (mSession == null)
            return;
        TermKeyMapManagerUi.showList(this, (isBuiltIn, name, rules, title) -> {
            if (mSession == null)
                return;
            mSession.output.setKeyMap(rules);
            refreshMenuPopup();
        }, mSession.output.getKeyMap());
    }

    public void onMenuScreenSize(final View view) {
        if (mSession == null)
            return;
        final ViewGroup v = (ViewGroup)
                getLayoutInflater().inflate(R.layout.buffer_size_dialog, null);
        final EditText wWidth = v.findViewById(R.id.width);
        final EditText wHeight = v.findViewById(R.id.height);
        final EditText wFontSize = v.findViewById(R.id.fontSize);
        final RadioGroup wOrientation = v.findViewById(R.id.orientation);
        final EditText wBufferHeight = v.findViewById(R.id.bufferHeight);
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
        if (autoFitTerminal && (!mCsv.resizeBufferXOnUi || !mCsv.resizeBufferYOnUi)) {
            wFontSize.setText("");
        } else {
            wFontSize.setText(NumberFormat.getNumberInstance()
                    .format(mCsv.getFontSize() /
                            getResources().getDisplayMetrics().scaledDensity));
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
        wBufferHeight.setText(String.valueOf(mSession.input.getMaxBufferHeight()));
        new AlertDialog.Builder(this)
                .setView(v)
                .setTitle(R.string.dialog_title_terminal_screen)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (mSession != null) {
                        int width;
                        int height;
                        int bufferHeight;
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
                        try {
                            bufferHeight = Integer.parseInt(wBufferHeight.getText().toString());
                        } catch (final IllegalArgumentException e) {
                            bufferHeight = mSession.input.getMaxBufferHeight();
                        }
                        final String fontSizeStr = wFontSize.getText().toString();
                        if (fontSizeStr.trim().isEmpty()) {
                            mCsv.setScreenSize(width, height, bufferHeight);
                            autoFitTerminal = !mCsv.resizeBufferXOnUi || !mCsv.resizeBufferYOnUi;
                            if (autoFitTerminal)
                                fitFontSize();
                        } else {
                            try {
                                final float fontSize = NumberFormat.getNumberInstance()
                                        .parse(fontSizeStr).floatValue();
                                autoFitTerminal = false;
                                mCsv.setFontSize(clampFontSize(fontSize *
                                                getResources().getDisplayMetrics().scaledDensity),
                                        false);
                                mCsv.setScreenSize(width, height, bufferHeight);
                            } catch (final ParseException ignored) {
                            }
                        }
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
                    refreshMenuPopup();
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> dialog.cancel())
                .setCancelable(true)
                .show();
    }

    public void onMenuTerminateOnDisconnect(final View view) {
        if (mSession == null)
            return;
        mSession.properties.terminateOnDisconnectEnabled =
                !mSession.properties.terminateOnDisconnectEnabled;
        if (view instanceof Checkable)
            ((Checkable) view).setChecked(mSession.properties.terminateOnDisconnectEnabled);
    }

    public void onMenuWakeLockReleaseOnDisconnect(final View view) {
        if (mSession == null)
            return;
        final BackendModule be = mSession.backend.wrapped;
        final boolean v = !be.isReleaseWakeLockOnDisconnect();
        be.setReleaseWakeLockOnDisconnect(v);
        if (view instanceof Checkable)
            ((Checkable) view).setChecked(v);
    }

    public void onMenuScratchpad(final View view) {
        startActivity(new Intent(this, ScratchpadActivity.class));
    }

    public void onMenuShowLog(final View view) {
        if (mSession == null)
            return;
        final List<LogMessage> log = mSession.backend.wrapped.getLog();
        if (log == null)
            return;
        final MessageLogView v = new MessageLogView(this);
        v.setLayoutManager(new LinearLayoutManager(this));
        v.setAdapter(new MessageLogView.Adapter(log));
        final AlertDialog d = new AlertDialog.Builder(this)
                .setView(v)
                .setCancelable(true)
                .show();
        v.addButton(R.layout.message_log_button,
                R.drawable.ic_check, R.string.action_close,
                _v -> d.cancel(), -1);
        final LifecycleObserver o = new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull final LifecycleOwner owner) {
                d.dismiss();
            }
        };
        d.setOnDismissListener(dialog -> getLifecycle().removeObserver(o));
        getLifecycle().addObserver(o);
    }

    public void onMenuHelp(final View view) {
        startActivity(new Intent(this, InfoActivity.class)
                .setData(Uri.parse("info://local/help")));
    }

    public void onMenuToggleWakeLock(final View view) {
        if (mSession == null)
            return;
        if (mSession.backend.wrapped.isWakeLockHeld())
            mSession.backend.wrapped.releaseWakeLock();
        else
            mSession.backend.wrapped.acquireWakeLock();
    }

    public void onMenuToggleKeepScreenOn(final View view) {
        if (mSession == null)
            return;
        mSession.uiState.keepScreenOn = !mSession.uiState.keepScreenOn;
        applyKeepScreenOn();
        if (view instanceof Checkable)
            ((Checkable) view).setChecked(mSession.uiState.keepScreenOn);
    }

    public void onMenuToggleHorizontalAppScrolling(final View view) {
        mCsv.setAppHScrollEnabled(!mCsv.isAppHScrollEnabled());
        if (view instanceof Checkable)
            ((Checkable) view).setChecked(mCsv.isAppHScrollEnabled());
    }

    final MouseButtonsWorkAround mbwa = new MouseButtonsWorkAround(this);

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        return mbwa.onDispatchTouchEvent(ev) ? mbwa.result : super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(final MotionEvent ev) {
        return mbwa.onDispatchGenericMotionEvent(ev) ?
                mbwa.result : super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent(final KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP: {
                    mCsv.setFontSize(clampFontSize(mCsv.getFontSize() +
                            getResources().getDisplayMetrics().density));
                    autoFitTerminal = false;
                    return true;
                }
                case KeyEvent.KEYCODE_VOLUME_DOWN: {
                    mCsv.setFontSize(clampFontSize(mCsv.getFontSize() -
                            getResources().getDisplayMetrics().density));
                    autoFitTerminal = false;
                    return true;
                }
            }
        }
        return mbwa.onDispatchKeyEvent(event) ? mbwa.result : super.dispatchKeyEvent(event);
    }

    public void onScrollHome(final View v) {
        mCsv.doScrollTo(0F, 0F, scrollableView ->
                scrollableView.doScrollToImmediate(0F, 0F));
    }
}
