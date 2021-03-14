package green_green_avk.anotherterm;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import java.util.NoSuchElementException;

import green_green_avk.anotherterm.ui.ConsoleKeyboardView;
import green_green_avk.anotherterm.ui.FontProvider;
import green_green_avk.anotherterm.ui.GraphicsCompositorView;
import green_green_avk.anotherterm.ui.MouseButtonsWorkAround;
import green_green_avk.anotherterm.ui.ScreenMouseView;

public final class GraphicsConsoleActivity extends ConsoleActivity {
    private GraphicsSession mSession = null;

    private GraphicsCompositorView mGcv = null;
    private ConsoleKeyboardView mCkv = null;
    private ScreenMouseView mSmv = null;
    private View mBell = null;
    private Animation mBellAnim = null;

    private ViewGroup wNavBar = null;

    private ImageView wUp = null;
    private Drawable wUpImDef = null;
    private TextView wTitle = null;

    @Keep
    private final ConsoleService.Listener sessionsListener = new ConsoleService.Listener() {
        @Override
        protected void onSessionChange(final int key) {
            if (key != mSessionKey) return;
            if (ConsoleService.isSessionTerminated(mSessionKey)) {
                finish();
                return;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mSession = ConsoleService.getGraphicsSession(mSessionKey);
        } catch (final NoSuchElementException e) {
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (isInMultiWindowMode())
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.graphics_console_activity);

        mGcv = findViewById(R.id.screen);
        mCkv = findViewById(R.id.keyboard);
        mSmv = findViewById(R.id.mouse);
        mBell = findViewById(R.id.bell);
        mBellAnim = AnimationUtils.loadAnimation(this, R.anim.blink_ring);

        wNavBar = findViewById(R.id.nav_bar);

        wUp = findViewById(R.id.action_nav_up);
        wUpImDef = wUp.getDrawable();
        wTitle = findViewById(R.id.title);

        final FontProvider fp = new ConsoleFontProvider();
        mCkv.setFont(fp); // Old Android devices have no glyphs for some special symbols

        mCkv.useIme(((App) getApplication()).settings.terminal_key_default_ime);

        setSessionTitle(mSession.compositor.title);

        mGcv.setCompositor(mSession.compositor);
        mCkv.setConsoleOutputOnly(mSession.compositor.consoleOutput);

//        mSession.uiState.csv.apply(mCsv);
        mSession.uiState.ckv.apply(mCkv);

        ConsoleService.addListener(sessionsListener);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
//        onInvalidateSink(null);
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
        mCkv.setAutoRepeatAllowed(((App) getApplication()).settings.terminal_key_repeat);
        mCkv.setAutoRepeatDelay(((App) getApplication()).settings.terminal_key_repeat_delay);
        mCkv.setAutoRepeatInterval(((App) getApplication()).settings.terminal_key_repeat_interval);
        mCkv.setKeyHeightDp(((App) getApplication()).settings.terminal_key_height_dp);
        mCkv.setHwKeyMap(HwKeyMapManager.get());
        mSmv.setButtons("wide".equals(((App) getApplication()).settings.terminal_mouse_layout) ?
                R.layout.screen_mouse_buttons_wide : R.layout.screen_mouse_buttons);
//        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(this);
        if (getUseRecents())
            wUp.setImageResource(R.drawable.ic_recents_black);
        else
            wUp.setImageDrawable(wUpImDef);
//        mSession.output.setMouseFocus(true);
    }

    @Override
    protected void onPause() {
//        mSession.output.setMouseFocus(false);
//        ((BackendUiInteractionActivityCtx) mSession.backend.wrapped.getUi()).setActivity(null);
//        mSession.uiState.csv.save(mCsv);
        mSession.uiState.ckv.save(mCkv);
//        mSession.uiState.screenOrientation = screenOrientation;
        mSession.thumbnail = mGcv.makeThumbnail(256, 128);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
//        if (menuPopupWindow != null) menuPopupWindow.dismiss();
        ConsoleService.removeListener(sessionsListener);
//        if (mSession != null) mSession.input.removeOnInvalidateSink(this);
        if (mCkv != null) mCkv.unsetConsoleInput();
        if (mGcv != null) mGcv.unsetCompositor();
        super.onDestroy();
    }

    public void setSessionTitle(@Nullable final CharSequence title) {
        if (title != null)
            setTitle(title);
    }

    @Override
    protected void onTitleChanged(final CharSequence title, final int color) {
        super.onTitleChanged(title, color);
        wTitle.setText(title);
        if (color != 0) wTitle.setTextColor(color);
    }

    public void onNavUp(final View v) {
        final Intent pa = getSupportParentActivityIntent();
        if (pa == null) return;
        if (getUseRecents()) {
            startActivity(pa.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        supportNavigateUpTo(pa);
    }

    public void onSwitchIme(final View view) {
        mCkv.useIme(!mCkv.isIme());
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
        return mbwa.onDispatchKeyEvent(event) ? mbwa.result : super.dispatchKeyEvent(event);
    }
}
