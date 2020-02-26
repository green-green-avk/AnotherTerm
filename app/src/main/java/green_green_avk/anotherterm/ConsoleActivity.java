package green_green_avk.anotherterm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
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
import android.widget.EditText;
import android.widget.ImageView;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;

import green_green_avk.anotherterm.backends.BackendModule;
import green_green_avk.anotherterm.backends.BackendUiInteractionActivityCtx;
import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.ui.ConsoleKeyboardView;
import green_green_avk.anotherterm.ui.ConsoleScreenView;
import green_green_avk.anotherterm.ui.MouseButtonsWorkAround;
import green_green_avk.anotherterm.ui.ScreenMouseView;
import green_green_avk.anotherterm.ui.UiUtils;

public final class ConsoleActivity extends AppCompatActivity implements ConsoleInput.OnInvalidateSink {

    private int mSessionKey = -1;
    private Session mSession = null;
    private ConsoleScreenView mCsv = null;
    private ConsoleKeyboardView mCkv = null;
    private ScreenMouseView mSmv = null;
    private ImageView mBell = null;
    private Animation mBellAnim = null;
    private ColorStateList toolbarIconColor = null;

    private int getFirstSessionKey() {
        return ConsoleService.sessionKeys.listIterator(0).next();
    }

    private int getLastSessionKey() {
        return ConsoleService.sessionKeys.listIterator(ConsoleService.sessionKeys.size()).previous();
    }

    private int getNextSessionKey(final int key) {
        final int i = ConsoleService.sessionKeys.indexOf(key) + 1;
        if (i == ConsoleService.sessionKeys.size()) return getFirstSessionKey();
        return ConsoleService.sessionKeys.listIterator(i).next();
    }

    private int getPreviousSessionKey(final int key) {
        final int i = ConsoleService.sessionKeys.indexOf(key);
        if (i == 0) return getLastSessionKey();
        return ConsoleService.sessionKeys.listIterator(i).previous();
    }

    private void startSelf(final int key) {
        finish();
        startActivity(new Intent(this, this.getClass()).putExtra(C.IFK_MSG_SESS_KEY, key));
    }

    private static int asSize(final Object o) {
        if (o instanceof Integer || o instanceof Long) return (int) o;
        return 0;
    }

    private GestureDetector mGestureDetector;

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
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
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isInMultiWindowMode())
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        toolbarIconColor = getResources().getColorStateList(R.color.console_toolbar_icon);

        setContentView(R.layout.activity_console);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
//        UiUtils.setHiddenSystemUi(this);
//        UiUtils.setShrinkBottomWhenCovered(this);

        mCsv = findViewById(R.id.screen);
        mCkv = findViewById(R.id.keyboard);
        mSmv = findViewById(R.id.mouse);
        mBell = findViewById(R.id.bell);
        mBellAnim = AnimationUtils.loadAnimation(this, R.anim.blink_ring);

        mCsv.setFont(FontsManager.consoleTypefaces);
        mCkv.setFont(FontsManager.consoleTypefaces); // Old Android devices have no glyphs for some special symbols

        mCsv.setFontSize(((App) getApplication()).settings.terminal_font_default_size_sp
                * getResources().getDisplayMetrics().scaledDensity);

        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(final MotionEvent e1, final MotionEvent e2,
                                   final float velocityX, final float velocityY) {
                if (mCsv.getSelectionMode()) return true;
                if (ConsoleService.sessionKeys.size() < 2) return true;
                if (e1 == null || e2 == null) return true; // avoid null events bug
                if (Math.abs(e1.getX() - e2.getX()) > 100) {
                    if (velocityX < -500) {
                        startSelf(getNextSessionKey(mSessionKey));
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
                        return true;
                    }
                    if (velocityX > 500) {
                        startSelf(getPreviousSessionKey(mSessionKey));
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
                        return true;
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });

        mCsv.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(final View v, final MotionEvent event) {
//                        UiUtils.hideSystemUi(ConsoleActivity.this); // For earlier versions
                        mGestureDetector.onTouchEvent(event);
                        return false;
                    }
                }
        );

        if (ConsoleService.sessionKeys.size() <= 0) {
            finish();
            return;
        }
        final Intent intent = getIntent();
        if (!intent.hasExtra(C.IFK_MSG_SESS_KEY)) {
            if (intent.hasExtra(C.IFK_MSG_SESS_TAIL)) {
                if (intent.getBooleanExtra(C.IFK_MSG_SESS_TAIL, false)) {
                    mSessionKey = getLastSessionKey();
                } else {
                    mSessionKey = getFirstSessionKey();
                }
            }
        } else {
            final int k = intent.getIntExtra(C.IFK_MSG_SESS_KEY, 0);
            if (!ConsoleService.sessionKeys.contains(k)) return;
            mSessionKey = k;
        }
        final int k = mSessionKey;
        mSession = ConsoleService.sessions.get(k);
        if (mSession == null) return;
        setTitle(mSession.input.currScrBuf.windowTitle);

        mCsv.setConsoleInput(mSession.input);
        mCkv.setConsoleInput(mSession.input);
        mSession.input.addOnInvalidateSink(this);

        mCsv.setScreenSize(asSize(mSession.connectionParams.get("screen_cols")),
                asSize(mSession.connectionParams.get("screen_rows")));
        mSession.uiState.csv.apply(mCsv);
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
        mCsv.setSelectionPadSize(((App) getApplication()).settings.terminal_selection_pad_size_dp
                * getResources().getDisplayMetrics().density);
        mCkv.setAutoRepeatAllowed(((App) getApplication()).settings.terminal_key_repeat);
        mCkv.setAutoRepeatDelay(((App) getApplication()).settings.terminal_key_repeat_delay);
        mCkv.setAutoRepeatInterval(((App) getApplication()).settings.terminal_key_repeat_interval);
        mCkv.setKeyHeightDp(((App) getApplication()).settings.terminal_key_height_dp);
        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(this);
    }

    @Override
    protected void onPause() {
        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(null);
        mSession.uiState.csv.save(mCsv);
        mSession.thumbnail = mCsv.makeThumbnail(256, 128);
        super.onPause();
    }

    private Menu mMenu = null;

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_console, menu);
        mMenu = menu;
        for (int mii = 0; mii < mMenu.size(); ++mii)
            UiUtils.setMenuItemIconState(mMenu.getItem(mii), new int[]{}, toolbarIconColor);
        final MenuItem chi = mMenu.findItem(R.id.action_charset);
/*
        final Spinner chs = (Spinner) chi.getActionView();
        chs.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, C.charsetList));
        if (mSession != null) {
            final int pos = C.charsetList.indexOf(mSession.output.getCharset().name());
            if (pos >= 0) chs.setSelection(pos);
        }
*/
        final SubMenu chs = chi.getSubMenu();
        for (final String chn : C.charsetList) {
            final MenuItem mi = chs.add(chn);
            mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    if (mSession != null) {
                        final String charsetStr = item.getTitle().toString();
                        try {
                            final Charset charset = Charset.forName(charsetStr);
                            mSession.input.setCharset(charset);
                            mSession.output.setCharset(charset);
                            chi.setTitle(charsetStr);
                        } catch (final IllegalArgumentException e) {
                            Log.e("Charset", charsetStr, e);
                        }
                    }
                    return true;
                }
            });
        }
        if (mSession != null) chi.setTitle(mSession.output.getCharset().name());

        if (mSession != null) {
            final BackendModule be = mSession.backend.wrapped;
            for (final Map.Entry<Method, BackendModule.ExportedUIMethod> m :
                    BackendsList.get(be.getClass()).meta.methods.entrySet()) {
                final MenuItem mi = menu.add(Menu.NONE, Menu.NONE, 100, m.getValue().titleRes());
                if (m.getKey().getTypeParameters().length == 0 &&
                        m.getKey().getReturnType() == Void.TYPE) {
                    mi.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(final MenuItem item) {
                            be.callMethod(m.getKey());
                            return true;
                        }
                    });
                }
            }
        }

        mMouseSupported = false;
        onInvalidateSink(null);

        return true;
    }

    private boolean mMouseSupported = false;

    @Override
    public void onInvalidateSink(final Rect rect) {
        if (mSession != null) {
            if (mSession.input.currScrBuf.windowTitle != null)
                setTitle(mSession.input.currScrBuf.windowTitle);
            final boolean ms = mSession.output.isMouseSupported();
            if (ms != mMouseSupported) {
                mMouseSupported = ms;
                if (!ms) turnOffMouseMode();
                if (mMenu != null) {
                    final MenuItem mi = mMenu.findItem(R.id.action_mouse);
                    if (mi.isVisible() != ms)
                        mi.setVisible(ms);
                }
            }
            if (mSession.input.getBell() != 0) {
                if (!mBellAnim.hasStarted() || mBellAnim.hasEnded())
                    mBell.startAnimation(mBellAnim);
            }
        }
    }

    private void turnOffMouseMode() {
        if (!mCsv.getMouseMode()) return;
        mCsv.setMouseMode(false);
        if (mMenu != null) {
            final MenuItem mi = mMenu.findItem(R.id.action_mouse);
            UiUtils.setMenuItemIconState(mi, new int[]{}, toolbarIconColor);
            mi.setChecked(false);
            mSmv.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_mouse: {
                mCsv.setMouseMode(!mCsv.getMouseMode());
                if (mCsv.getMouseMode()) {
                    UiUtils.setMenuItemIconState(item, new int[]{android.R.attr.state_checked}, toolbarIconColor);
                    item.setChecked(true);
                    mSmv.setVisibility(View.VISIBLE);
                } else {
                    UiUtils.setMenuItemIconState(item, new int[]{}, toolbarIconColor);
                    item.setChecked(false);
                    mSmv.setVisibility(View.GONE);
                }
                return true;
            }
            case R.id.action_ime: {
                mCkv.useIme(!mCkv.isIme());
                item.setChecked(mCkv.isIme());
//                UiUtils.hideSystemUi(this);
                return true;
            }
            case R.id.action_select: {
                mCsv.setSelectionMode(!mCsv.getSelectionMode());
                return true;
            }
            case R.id.action_paste: {
                final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard == null) return true;
                if (!clipboard.hasPrimaryClip()) return true;
                final String v = clipboard.getPrimaryClip().getItemAt(0)
                        .coerceToText(this).toString();
                mCkv.clipboardPaste(v);
                return true;
            }
            case R.id.action_charset: {
                /*
                if (mSession != null) {
                    final String charsetStr = ((Spinner)item.getActionView()).getSelectedItem().toString();
                    try {
                        final Charset charset = Charset.forName(charsetStr);
                        mSession.input.setCharset(charset);
                        mSession.output.setCharset(charset);
                    } catch (IllegalArgumentException e) {
                        Log.e("Charset", charsetStr, e);
                    }
                }
                */
                return true;
            }
            case R.id.action_keymap: {
                TermKeyMapManagerUi.showList(this, new TermKeyMapAdapter.OnSelectListener() {
                    @Override
                    public void onSelect(final boolean isBuiltIn, final String name,
                                         final TermKeyMapRules rules, final String title) {
                        mSession.output.setKeyMap(rules);
                    }
                }, mSession.output.getKeyMap());
                return true;
            }
            case R.id.action_set_terminal_size: {
                if (mSession == null) return true;
                final ViewGroup v = (ViewGroup)
                        getLayoutInflater().inflate(R.layout.buffer_size_dialog, null);
                final EditText widthV = v.findViewById(R.id.width);
                final EditText heightV = v.findViewById(R.id.height);
                if (mCsv.resizeBufferXOnUi)
                    widthV.setHint(getString(R.string.hint_int_value_p_auto_p,
                            mSession.input.currScrBuf.getWidth()));
                else {
                    widthV.setText(String.valueOf(mSession.input.currScrBuf.getWidth()));
                    widthV.setHint(R.string.hint_auto);
                }
                if (mCsv.resizeBufferYOnUi)
                    heightV.setHint(getString(R.string.hint_int_value_p_auto_p,
                            mSession.input.currScrBuf.getHeight()));
                else {
                    heightV.setText(String.valueOf(mSession.input.currScrBuf.getHeight()));
                    heightV.setHint(R.string.hint_auto);
                }
                new AlertDialog.Builder(this)
                        .setView(v)
                        .setTitle(R.string.dialog_title_set_terminal_screen_size)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                UiUtils.hideIME((Dialog) dialog);
                                if (mSession != null) {
                                    int width;
                                    int height;
                                    try {
                                        width = Integer.parseInt(widthV.getText().toString());
                                    } catch (final IllegalArgumentException e) {
                                        width = 0;
                                    }
                                    try {
                                        height = Integer.parseInt(heightV.getText().toString());
                                    } catch (final IllegalArgumentException e) {
                                        height = 0;
                                    }
                                    mCsv.setScreenSize(width, height);
                                    mCsv.onInvalidateSink(null);
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
            case R.id.action_terminate: {
                final int k = getNextSessionKey(mSessionKey);
                ConsoleService.stopSession(mSessionKey);
                if (ConsoleService.sessionKeys.size() <= 0) finish();
                else {
                    startSelf(k);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
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
                    if (mCsv.getFontSize() < 64)
                        mCsv.setFontSize(mCsv.getFontSize() + 1);
                    return true;
                }
                case KeyEvent.KEYCODE_VOLUME_DOWN: {
                    if (mCsv.getFontSize() > 4)
                        mCsv.setFontSize(mCsv.getFontSize() - 1);
                    return true;
                }
            }
        }
        return mbwa.onDispatchKeyEvent(event) ? mbwa.result : super.dispatchKeyEvent(event);
    }
}
