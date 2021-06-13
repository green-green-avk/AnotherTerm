package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.BackendUiInteraction;
import green_green_avk.anotherterm.backends.BackendUiInteractionActivityCtx;
import green_green_avk.anotherterm.utils.BlockingSync;
import green_green_avk.anotherterm.utils.LogMessage;
import green_green_avk.anotherterm.utils.WeakBlockingSync;

// TODO: Split into UI and UI thread connector queue classes
public final class BackendUiDialogs implements BackendUiInteraction,
        BackendUiInteractionActivityCtx, BackendUiSessionBridge {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final WeakBlockingSync<Activity> activityRef = new WeakBlockingSync<>();

    private final Set<Dialog> dialogs = Collections.newSetFromMap(
            new WeakHashMap<>());

    private final Object promptLock = new Object();
    private volatile Runnable promptState = null;
    private WeakReference<Dialog> promptDialog = new WeakReference<>(null);

    @UiThread
    private void showPrompt(@NonNull final Dialog d) {
        d.show();
        promptDialog = new WeakReference<>(d);
        dialogs.add(d);
    }

    @UiThread
    private boolean isShowingPrompt() {
        final Dialog d = promptDialog.get();
        return d != null && d.isShowing();
    }

    private final Object msgQueueLock = new Object();
    private final ArrayList<LogMessage> msgQueue = new ArrayList<>();
    private WeakReference<MessageLogView.Adapter> msgAdapterRef = new WeakReference<>(null);

    @UiThread
    private void showQueuedMessages(@NonNull final Activity ctx) {
        if (ctx.isFinishing()) return;
        final MessageLogView v = new MessageLogView(ctx);
        v.setLayoutManager(new LinearLayoutManager(ctx));
        final MessageLogView.Adapter a = new MessageLogView.Adapter(msgQueue);
        v.setAdapter(a);
        msgAdapterRef = new WeakReference<>(a);
        final Dialog d = new AlertDialog.Builder(ctx)
                .setView(v)
                .setOnCancelListener(dialog -> {
                    msgQueue.clear();
                    msgAdapterRef = new WeakReference<>(null);
                })
                .show();
        dialogs.add(d);
    }

    @Override
    @UiThread
    public void setActivity(@Nullable final Activity ctx) {
        if (ctx == activityRef.getNoBlock()) return;
        synchronized (msgQueueLock) {
            msgAdapterRef = new WeakReference<>(null);
            activityRef.set(ctx);
            if (ctx != null) {
                final Runnable ps = promptState;
                if (ps != null) ps.run();
                if (!msgQueue.isEmpty()) showQueuedMessages(ctx);
            } else {
                for (final Dialog d : dialogs) d.dismiss();
                dialogs.clear();
            }
        }
    }

    @Override
    @Nullable
    public String promptPassword(@NonNull final String message) throws InterruptedException {
        try {
            synchronized (promptLock) {
                final BlockingSync<String> result = new BlockingSync<>();
                promptState = () -> {
                    if (isShowingPrompt()) return;
                    final Activity ctx = activityRef.getNoBlock();
                    if (ctx == null) return;
                    final EditText et = new EditText(ctx);
                    final DialogInterface.OnClickListener listener = (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            promptState = null;
                            result.set(et.getText().toString());
                            dialog.dismiss();
                        } else {
                            promptState = null;
                            result.set(null);
                            dialog.dismiss();
                        }
                    };
                    et.setInputType(InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    final Dialog d = new AlertDialog.Builder(ctx)
                            .setOnCancelListener(dialog -> {
                                promptState = null;
                                result.set(null);
                            })
                            .setCancelable(false)
                            .setMessage(message)
                            .setView(et)
                            .setNegativeButton(android.R.string.cancel, listener)
                            .setPositiveButton(android.R.string.ok, listener)
                            .create();
                    et.setOnEditorActionListener((v, actionId, event) -> {
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            /* We cannot react on ACTION_UP here but it's a reasonable way
                            to avoid a parasite ENTER keystroke in the underlying console view */
                            listener.onClick(d, DialogInterface.BUTTON_POSITIVE);
                            return true;
                        }
                        return false;
                    });
                    et.setOnKeyListener((v, keyCode, event) -> {
                        if (event.getAction() == KeyEvent.ACTION_UP
                                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            listener.onClick(d, DialogInterface.BUTTON_POSITIVE);
                            return true;
                        }
                        return false;
                    });
                    et.requestFocus();
                    /* Workaround: not all devices have the problem with
                    the invisible soft keyboard here */
                    final Window w = d.getWindow();
                    if (w != null)
                        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    // ---
                    showPrompt(d);
                };
                handler.post(promptState);
                return result.get();
            }
        } finally {
            promptState = null;
        }
    }

    @Override
    public boolean promptYesNo(@NonNull final String message) throws InterruptedException {
        try {
            synchronized (promptLock) {
                final BlockingSync<Boolean> result = new BlockingSync<>();
                promptState = () -> {
                    if (isShowingPrompt()) return;
                    final Activity ctx = activityRef.getNoBlock();
                    if (ctx == null) return;
                    final DialogInterface.OnClickListener listener = (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            promptState = null;
                            result.set(true);
                            dialog.dismiss();
                        } else {
                            promptState = null;
                            result.set(false);
                            dialog.dismiss();
                        }
                    };
                    final Dialog d = new AlertDialog.Builder(ctx)
                            .setOnCancelListener(dialog -> {
                                promptState = null;
                                result.set(false);
                            })
                            .setCancelable(false)
                            .setMessage(message)
                            .setNegativeButton(android.R.string.no, listener)
                            .setPositiveButton(android.R.string.yes, listener)
                            .create();
                    showPrompt(d);
                };
                handler.post(promptState);
                return result.get();
            }
        } finally {
            promptState = null;
        }
    }

    @Override
    public void showMessage(@NonNull final String message) {
        handler.post(() -> {
            synchronized (msgQueueLock) {
                final Activity ctx = activityRef.getNoBlock();
                if (ctx == null) {
                    msgQueue.add(new LogMessage(message));
                    return;
                }
                msgQueue.add(new LogMessage(message));
                final MessageLogView.Adapter a = msgAdapterRef.get();
                if (a != null) a.notifyDataSetChanged();
                else showQueuedMessages(ctx);
            }
        });
    }

    @Override
    public void showToast(@NonNull final String message) {
        final Activity ctx = activityRef.getNoBlock();
        if (ctx == null) return;
        ctx.runOnUiThread(() -> Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    @Nullable
    public byte[] promptContent(@NonNull final String message, @NonNull final String mimeType,
                                final long sizeLimit) throws InterruptedException, IOException {
        try {
            synchronized (promptLock) {
                final BlockingSync<Object> result = new BlockingSync<>();
                promptState = () -> {
                    final Activity ctx = activityRef.getNoBlock();
                    if (ctx == null) return;
                    final DialogInterface.OnClickListener listener = (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            promptState = null;
                            ContentRequester.request(result, ContentRequester.Type.BYTES,
                                    sizeLimit,
                                    ctx, message, mimeType);
                            dialog.dismiss();
                        } else {
                            dialog.cancel();
                        }
                    };
                    final Dialog d = new AlertDialog.Builder(ctx)
                            .setOnCancelListener(dialog -> {
                                promptState = null;
                                result.set(null);
                            })
                            .setCancelable(false)
                            .setMessage(message)
                            .setNegativeButton(android.R.string.cancel, listener)
                            .setPositiveButton(R.string.choose, listener)
                            .show();
                    dialogs.add(d);
                };
                handler.post(promptState);
                final Object r = result.get();
                if (r instanceof byte[])
                    return (byte[]) r;
                if (r instanceof IOException)
                    throw (IOException) r;
                if (r instanceof Throwable)
                    throw new IOException(((Throwable) r).getLocalizedMessage());
                return null;
            }
        } finally {
            promptState = null;
        }
    }

    @Override
    public boolean promptPermissions(@NonNull final String[] perms) throws InterruptedException {
        final Activity ctx = activityRef.get();
        int[] result = Permissions.requestBlocking(ctx, perms);
        boolean r = true;
        for (int v : result) r = r && v == PackageManager.PERMISSION_GRANTED;
        return r;
    }

    public boolean hasUi() {
        return activityRef.getNoBlock() != null;
    }

    public void waitForUi() throws InterruptedException {
        activityRef.get();
    }

    private final int sessionKey;

    @Override
    public int getSessionKey() {
        return sessionKey;
    }

    public BackendUiDialogs(final int sessionKey) {
        this.sessionKey = sessionKey;
    }
}
