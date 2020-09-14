package green_green_avk.anotherterm;

import android.app.AlertDialog;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.math.MathUtils;
import androidx.core.widget.TextViewCompat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
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
import green_green_avk.anotherterm.utils.BooleanCaster;

public final class ConsoleActivity extends AppCompatActivity
        implements ConsoleInput.OnInvalidateSink, ScrollableView.OnScroll,
        ConsoleScreenView.OnStateChange {

    private int mSessionKey = -1;
    private Session mSession = null;
    private int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    private boolean autoFitTerminal = false;
    private ConsoleScreenView mCsv = null;
    private ConsoleKeyboardView mCkv = null;
    private ScreenMouseView mSmv = null;
    private View mBell = null;
    private Animation mBellAnim = null;
    private VisibilityAnimator mScrollHomeVA = null;

    private ViewGroup wNavBar = null;

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
        final boolean v = mSession.backend.wrapped.isWakeLockHeld();
        mCkv.setLedsByCode(C.KEYCODE_LED_WAKE_LOCK, v);
        mCkv.invalidateModifierKeys(C.KEYCODE_LED_WAKE_LOCK);
        if (menuPopupWindow != null && menuPopupWindow.isShowing()) {
            menuPopupWindow.getContentView().<CompoundButton>findViewById(R.id.wakelock)
                    .setChecked(v);
        }
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
        if (o instanceof Integer) return (int) o;
        if (o instanceof Long) return (int) (long) o;
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
        if (inFitFontSize) return;
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
        applyKeepScreenOn();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isInMultiWindowMode())
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.console_activity);

        mCsv = findViewById(R.id.screen);
        mCkv = findViewById(R.id.keyboard);
        mSmv = findViewById(R.id.mouse);
        mBell = findViewById(R.id.bell);
        mBellAnim = AnimationUtils.loadAnimation(this, R.anim.blink_ring);
        mScrollHomeVA = new VisibilityAnimator(findViewById(R.id.scrollHome));

        wNavBar = findViewById(R.id.nav_bar);

        wTitle = findViewById(R.id.title);
        wMouseMode = findViewById(R.id.action_mouse_mode);

        wConnecting = findViewById(R.id.connecting);

        final boolean isNew = mSession.uiState.fontSizeDp == 0F;
        if (isNew) autoFitTerminal =
                BooleanCaster.CAST(mSession.connectionParams.get("font_size_auto"));
        else autoFitTerminal = mSession.uiState.fontSizeDp < 0F;

        final FontProvider fp = new ConsoleFontProvider();
        mCsv.setFont(fp);
        mCkv.setFont(fp); // Old Android devices have no glyphs for some special symbols

        final DisplayMetrics dm = getResources().getDisplayMetrics();

        if (isNew && !autoFitTerminal) mSession.uiState.fontSizeDp =
                ((App) getApplication()).settings.terminal_font_default_size_sp *
                        (dm.scaledDensity / dm.density);
        mCsv.setFontSize(mSession.uiState.fontSizeDp *
                getResources().getDisplayMetrics().density, false);

        mCkv.useIme(((App) getApplication()).settings.terminal_key_default_ime);

        setTitle(mSession.input.currScrBuf.windowTitle);

        mCsv.setConsoleInput(mSession.input);
        mCkv.setConsoleInput(mSession.input);
        mSession.input.addOnInvalidateSink(this);
        mCsv.onScroll = this;
        mCsv.onStateChange = this;

        if (isNew) mCsv.setScreenSize(asSize(mSession.connectionParams.get("screen_cols")),
                asSize(mSession.connectionParams.get("screen_rows")));
        mSession.uiState.csv.apply(mCsv);
        mSession.uiState.ckv.apply(mCkv);

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
        mCkv.setHwKeyMap(HwKeyMapManager.get());
        mSmv.setButtons("wide".equals(((App) getApplication()).settings.terminal_mouse_layout) ?
                R.layout.screen_mouse_buttons_wide : R.layout.screen_mouse_buttons);
        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(this);
    }

    @Override
    protected void onPause() {
        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(null);
        mSession.uiState.fontSizeDp = autoFitTerminal ? -1F : (mCsv.getFontSize() /
                getResources().getDisplayMetrics().density);
        mSession.uiState.csv.save(mCsv);
        mSession.uiState.ckv.save(mCkv);
        mSession.uiState.screenOrientation = screenOrientation;
        mSession.thumbnail = mCsv.makeThumbnail(256, 128);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (menuPopupWindow != null) menuPopupWindow.dismiss();
        mSession.input.removeOnInvalidateSink(this);
        mCkv.unsetConsoleInput();
        mCsv.unsetConsoleInput();
        super.onDestroy();
    }

    @Nullable
    private PopupWindow menuPopupWindow = null;

    @NonNull
    private PopupWindow createMenuPopup() {
        final View popupView =
                LayoutInflater.from(this).inflate(R.layout.console_menu, null);
        popupView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

// Module related
// --------------

        if (mSession != null) {
            final BackendModule be = mSession.backend.wrapped;
            final List<Map.Entry<Method, BackendModule.ExportedUIMethod>> uiMethods =
                    new LinkedList<>(BackendsList.get(be.getClass()).meta.methods.entrySet());
            Collections.sort(uiMethods, new Comparator<Map.Entry<Method, BackendModule.ExportedUIMethod>>() {
                @Override
                public int compare(final Map.Entry<Method, BackendModule.ExportedUIMethod> o1,
                                   final Map.Entry<Method, BackendModule.ExportedUIMethod> o2) {
                    return o1.getValue().order() - o2.getValue().order();
                }
            });
            final ViewGroup moduleUiView = popupView.findViewById(R.id.module_ui);
            for (final Map.Entry<Method, BackendModule.ExportedUIMethod> m : uiMethods) {
                final Class<?>[] paramTypes = m.getKey().getParameterTypes();
                final Class<?> retType = m.getKey().getReturnType();
                if (paramTypes.length == 0 && retType == Void.TYPE) {
                    final TextView mi = (TextView) LayoutInflater.from(this)
                            .inflate(R.layout.module_ui_button, moduleUiView, false);
                    mi.setText(m.getValue().titleRes());
                    mi.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View item) {
                            be.callMethod(m.getKey());
                            if (menuPopupWindow != null) menuPopupWindow.dismiss();
                        }
                    });
                    moduleUiView.addView(mi);
                } else if (paramTypes.length == 1 && retType == Void.TYPE) {
                    final Annotation[] aa = m.getKey().getParameterAnnotations()[0];
                    for (final Annotation a : aa) {
                        if (a instanceof BackendModule.ExportedUIMethodEnum) {
                            if (paramTypes[0] != Integer.TYPE) break;
                            final ViewGroup sm = (ViewGroup) LayoutInflater.from(this)
                                    .inflate(R.layout.module_ui_group, moduleUiView, false);
                            sm.<TextView>findViewById(R.id.title)
                                    .setText(m.getValue().longTitleRes() != 0 ?
                                            m.getValue().longTitleRes() :
                                            m.getValue().titleRes());
                            final ViewGroup smg = sm.findViewById(R.id.content);
                            final BackendModule.ExportedUIMethodEnum ae =
                                    (BackendModule.ExportedUIMethodEnum) a;
                            for (int ai = 0; ai < ae.values().length; ai++) {
                                final int value = ae.values()[ai];
                                final TextView mi = (TextView) LayoutInflater.from(this)
                                        .inflate(R.layout.module_ui_button, smg, false);
                                mi.setText(ae.titleRes()[ai]);
                                mi.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(final View item) {
                                        be.callMethod(m.getKey(), value);
                                        if (menuPopupWindow != null) menuPopupWindow.dismiss();
                                    }
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
                            m.getKey().getAnnotation(BackendModule.ExportedUIMethodFlags.class);
                    if (a != null) {
                        final TextView mi = (TextView) LayoutInflater.from(this)
                                .inflate(R.layout.module_ui_button, moduleUiView, false);
                        mi.setText(m.getValue().titleRes());
                        mi.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(final View item) {
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
                            }
                        });
                        moduleUiView.addView(mi);
                    }
                }
            }
        }

// ==============

        final PopupWindow window = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        window.setBackgroundDrawable(getResources().getDrawable(
                android.R.drawable.dialog_holo_light_frame));
        window.setSplitTouchEnabled(true);
        window.setAnimationStyle(android.R.style.Animation_Dialog);
        return window;
    }

    private void refreshMenuPopup() {
        if (menuPopupWindow == null || !menuPopupWindow.isShowing()) return;
        if (mSession != null) {
            final BackendModule be = mSession.backend.wrapped;
            final View popupView = menuPopupWindow.getContentView();
            popupView.<CompoundButton>findViewById(R.id.wakelock).setChecked(be.isWakeLockHeld());
            popupView.<CompoundButton>findViewById(R.id.keep_screen_on)
                    .setChecked(mSession.uiState.keepScreenOn);
            popupView.<TextView>findViewById(R.id.charset)
                    .setText(mSession.output.getCharset().name());
            popupView.<TextView>findViewById(R.id.keymap)
                    .setText(TermKeyMapManagerUi.getTitle(this, mSession.output.getKeyMap()));
            final String w;
            if (mCsv.resizeBufferXOnUi)
                w = getString(R.string.hint_int_value_p_auto_p,
                        mSession.input.currScrBuf.getWidth());
            else w = String.valueOf(mSession.input.currScrBuf.getWidth());
            final String h;
            if (mCsv.resizeBufferYOnUi)
                h = getString(R.string.hint_int_value_p_auto_p,
                        mSession.input.currScrBuf.getHeight());
            else h = String.valueOf(mSession.input.currScrBuf.getHeight());
            popupView.<TextView>findViewById(R.id.screen_size)
                    .setText(w + " " + getString(R.string.label_dims_div) + " " + h);
            popupView.<CompoundButton>findViewById(R.id.terminate_on_disconnect)
                    .setChecked(mSession.properties.terminateOnDisconnect);
            popupView.<CompoundButton>findViewById(R.id.wakelock_release_on_disconnect)
                    .setChecked(be.isReleaseWakeLockOnDisconnect());
        }
    }

    private void showMenuPopup(@NonNull final View view) {
        if (menuPopupWindow == null) menuPopupWindow = createMenuPopup();
        menuPopupWindow.showAsDropDown(view);
        refreshMenuPopup();
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
    public void onInvalidateSinkResize(final int cols, final int rows) {
        if (autoFitTerminal) fitFontSize();
        Toast.makeText(this,
                cols + " " + getString(R.string.label_dims_div) + " " + rows,
                Toast.LENGTH_SHORT).show();
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
    public void onTerminalAreaResize(final int width, final int height) {
        if (autoFitTerminal && mSession != null) {
            fitFontSize();
        }
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
        showMenuPopup(v);
    }

    public void onMenuCharset(final View view) {
        if (mSession == null) return;
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
                        refreshMenuPopup();
                        dialog.dismiss();
                    }
                }).setCancelable(true).show();
    }

    public void onMenuKeymap(final View view) {
        if (mSession == null) return;
        TermKeyMapManagerUi.showList(this, new TermKeyMapAdapter.OnSelectListener() {
            @Override
            public void onSelect(final boolean isBuiltIn, final String name,
                                 final TermKeyMapRules rules, final String title) {
                if (mSession == null) return;
                mSession.output.setKeyMap(rules);
                refreshMenuPopup();
            }
        }, mSession.output.getKeyMap());
    }

    public void onMenuScreenSize(final View view) {
        if (mSession == null) return;
        final ViewGroup v = (ViewGroup)
                getLayoutInflater().inflate(R.layout.buffer_size_dialog, null);
        final EditText wWidth = v.findViewById(R.id.width);
        final EditText wHeight = v.findViewById(R.id.height);
        final EditText wFontSize = v.findViewById(R.id.fontSize);
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
        new AlertDialog.Builder(this)
                .setView(v)
                .setTitle(R.string.dialog_title_terminal_screen)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
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
                            final String fontSizeStr = wFontSize.getText().toString();
                            if (fontSizeStr.trim().isEmpty()) {
                                mCsv.setScreenSize(width, height);
                                autoFitTerminal = !mCsv.resizeBufferXOnUi || !mCsv.resizeBufferYOnUi;
                                if (autoFitTerminal) fitFontSize();
                            } else {
                                try {
                                    final float fontSize = NumberFormat.getNumberInstance()
                                            .parse(fontSizeStr).floatValue();
                                    autoFitTerminal = false;
                                    mCsv.setFontSize(clampFontSize(fontSize *
                                                    getResources().getDisplayMetrics().scaledDensity),
                                            false);
                                    mCsv.setScreenSize(width, height);
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
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dialog.cancel();
                    }
                })
                .setCancelable(true)
                .show();
    }

    public void onMenuTerminateOnDisconnect(final View view) {
        if (mSession == null) return;
        mSession.properties.terminateOnDisconnect = !mSession.properties.terminateOnDisconnect;
        if (view instanceof Checkable)
            ((Checkable) view).setChecked(mSession.properties.terminateOnDisconnect);
    }

    public void onMenuWakeLockReleaseOnDisconnect(final View view) {
        if (mSession == null) return;
        final BackendModule be = mSession.backend.wrapped;
        final boolean v = !be.isReleaseWakeLockOnDisconnect();
        be.setReleaseWakeLockOnDisconnect(v);
        if (view instanceof Checkable)
            ((Checkable) view).setChecked(v);
    }

    public void onMenuScratchpad(final View view) {
        startActivity(new Intent(this, ScratchpadActivity.class));
    }

    public void onMenuHelp(final View view) {
        startActivity(new Intent(this, InfoActivity.class)
                .setData(Uri.parse("info://local/help")));
    }

    public void onMenuToggleWakeLock(final View view) {
        if (mSession == null) return;
        if (mSession.backend.wrapped.isWakeLockHeld())
            mSession.backend.wrapped.releaseWakeLock();
        else mSession.backend.wrapped.acquireWakeLock();
    }

    public void onMenuToggleKeepScreenOn(final View view) {
        if (mSession == null) return;
        mSession.uiState.keepScreenOn = !mSession.uiState.keepScreenOn;
        applyKeepScreenOn();
        if (view instanceof Checkable)
            ((Checkable) view).setChecked(mSession.uiState.keepScreenOn);
    }

    public void onMenuTerminate(final View view) {
        if (view != null) {
            UiUtils.confirm(this, getString(R.string.prompt_terminate_the_session),
                    new Runnable() {
                        @Override
                        public void run() {
                            onMenuTerminate(null);
                        }
                    });
            return;
        }
        try {
            ConsoleService.stopSession(mSessionKey);
        } catch (final NoSuchElementException ignored) {
        }
        finish();
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
        mCsv.doScrollTo(0F, 0F);
    }
}
