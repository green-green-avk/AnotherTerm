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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.LinearLayoutManager;

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
        BackendUiInteractionActivityCtx {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final WeakBlockingSync<Activity> ctxRef = new WeakBlockingSync<>();

    private final Set<Dialog> dialogs = Collections.newSetFromMap(
            new WeakHashMap<Dialog, Boolean>());

    private final Object promptLock = new Object();
    private volatile Runnable promptState = null;
    private WeakReference<Dialog> promptDialog = new WeakReference<>(null);

    @UiThread
    private void showPrompt(@NonNull final Dialog d) {
        d.show();
        promptDialog = new WeakReference<>(d);
        dialogs.add(d);
    }

    private boolean isShownPrompt() {
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
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface dialog) {
                        msgQueue.clear();
                        msgAdapterRef = new WeakReference<>(null);
                    }
                })
                .show();
        dialogs.add(d);
    }

    @UiThread
    @Override
    public void setActivity(@Nullable final Activity ctx) {
        if (ctx == ctxRef.getNoBlock()) return;
        synchronized (msgQueueLock) {
            msgAdapterRef = new WeakReference<>(null);
            ctxRef.set(ctx);
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

    @Nullable
    @Override
    public String promptPassword(@NonNull final String message) throws InterruptedException {
        try {
            synchronized (promptLock) {
                final BlockingSync<String> result = new BlockingSync<>();
                promptState = new Runnable() {
                    @Override
                    public void run() {
                        if (isShownPrompt()) return;
                        final Activity ctx = ctxRef.getNoBlock();
                        if (ctx == null) return;
                        final EditText et = new EditText(ctx);
                        final DialogInterface.OnClickListener listener =
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int which) {
                        /* Workaround: the soft keyboard usually remains visible
                        after the dialog ends */
                                        UiUtils.hideIME((Dialog) dialog);
                                        // ---
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            promptState = null;
                                            result.set(et.getText().toString());
                                            dialog.dismiss();
                                        } else {
                                            promptState = null;
                                            result.set(null);
                                            dialog.dismiss();
                                        }
                                    }
                                };
                        et.setInputType(InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        final Dialog d = new AlertDialog.Builder(ctx)
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(final DialogInterface dialog) {
                                        promptState = null;
                                        result.set(null);
                                    }
                                })
                                .setCancelable(false)
                                .setMessage(message)
                                .setView(et)
                                .setNegativeButton(android.R.string.cancel, listener)
                                .setPositiveButton(android.R.string.ok, listener)
                                .create();
                        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(final TextView v, final int actionId,
                                                          final KeyEvent event) {
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                            /* We cannot react on ACTION_UP here but it's a reasonable way
                            to avoid a parasite ENTER keystroke in the underlying console view */
                                    listener.onClick(d, DialogInterface.BUTTON_POSITIVE);
                                    return true;
                                }
                                return false;
                            }
                        });
                        et.setOnKeyListener(new View.OnKeyListener() {
                            @Override
                            public boolean onKey(final View v, final int keyCode, KeyEvent event) {
                                if (event.getAction() == KeyEvent.ACTION_UP
                                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                                    listener.onClick(d, DialogInterface.BUTTON_POSITIVE);
                                    return true;
                                }
                                return false;
                            }
                        });
                /* Workaround: not all devices have the problem with
                the invisible soft keyboard here */
                        final Window w = d.getWindow();
                        if (w != null)
                            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                        // ---
                        showPrompt(d);
                    }
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
                promptState = new Runnable() {
                    @Override
                    public void run() {
                        if (isShownPrompt()) return;
                        final Activity ctx = ctxRef.getNoBlock();
                        if (ctx == null) return;
                        final DialogInterface.OnClickListener listener =
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int which) {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            promptState = null;
                                            result.set(true);
                                            dialog.dismiss();
                                        } else {
                                            promptState = null;
                                            result.set(false);
                                            dialog.dismiss();
                                        }
                                    }
                                };
                        final Dialog d = new AlertDialog.Builder(ctx)
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(final DialogInterface dialog) {
                                        promptState = null;
                                        result.set(false);
                                    }
                                })
                                .setCancelable(false)
                                .setMessage(message)
                                .setNegativeButton(android.R.string.no, listener)
                                .setPositiveButton(android.R.string.yes, listener)
                                .create();
                        showPrompt(d);
                    }
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
        handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (msgQueueLock) {
                    final Activity ctx = ctxRef.getNoBlock();
                    if (ctx == null) {
                        msgQueue.add(new LogMessage(message));
                        return;
                    }
                    msgQueue.add(new LogMessage(message));
                    final MessageLogView.Adapter a = msgAdapterRef.get();
                    if (a != null) a.notifyDataSetChanged();
                    else showQueuedMessages(ctx);
                }
            }
        });
    }

    @Override
    public void showToast(@NonNull final String message) {
        final Activity ctx = ctxRef.getNoBlock();
        if (ctx == null) return;
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public byte[] promptContent(@NonNull final String message, @NonNull final String mimeType)
            throws InterruptedException {
        try {
            synchronized (promptLock) {
                final BlockingSync<byte[]> result = new BlockingSync<>();
                promptState = new Runnable() {
                    @Override
                    public void run() {
                        final Activity ctx = ctxRef.getNoBlock();
                        if (ctx == null) return;
                        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    promptState = null;
                                    ContentRequester.request(result, ContentRequester.Type.BYTES,
                                            ctx, message, mimeType);
                                    dialog.dismiss();
                                } else {
                                    dialog.cancel();
                                }
                            }
                        };
                        final Dialog d = new AlertDialog.Builder(ctx)
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        promptState = null;
                                        result.set(null);
                                    }
                                })
                                .setCancelable(false)
                                .setMessage(message)
                                .setNegativeButton(android.R.string.cancel, listener)
                                .setPositiveButton(R.string.choose, listener)
                                .show();
                        dialogs.add(d);
                    }
                };
                handler.post(promptState);
                return result.get();
            }
        } finally {
            promptState = null;
        }
    }

    @Override
    public boolean promptPermissions(@NonNull final String[] perms) throws InterruptedException {
        final Activity ctx = ctxRef.get();
        int[] result = Permissions.requestBlocking(ctx, perms);
        boolean r = true;
        for (int v : result) r = r && v == PackageManager.PERMISSION_GRANTED;
        return r;
    }
}
