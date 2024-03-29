package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.ConsoleActivity;
import green_green_avk.anotherterm.PasswordService;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.backends.BackendUiInteraction;
import green_green_avk.anotherterm.backends.BackendUiInteractionActivityCtx;
import green_green_avk.anotherterm.backends.BackendUiPasswordStorage;
import green_green_avk.anotherterm.utils.BlockingSync;
import green_green_avk.anotherterm.utils.Erasable;
import green_green_avk.anotherterm.utils.LogMessage;
import green_green_avk.anotherterm.utils.Misc;
import green_green_avk.anotherterm.utils.Password;
import green_green_avk.anotherterm.utils.WeakBlockingSync;

// TODO: Split into UI and UI thread connector queue classes
public class BackendUiDialogs implements BackendUiInteraction,
        BackendUiPasswordStorage,
        BackendUiInteractionActivityCtx {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final WeakBlockingSync<Activity> activityRef = new WeakBlockingSync<>();

    private final Set<Dialog> dialogs = Collections.newSetFromMap(
            new WeakHashMap<>());

    private final Object promptLock = new Object();
    private volatile Runnable promptState = null;
    private WeakReference<Dialog> promptDialog = new WeakReference<>(null);

    @UiThread
    private boolean isShowingPrompt() {
        final Dialog d = promptDialog.get();
        return d != null && d.isShowing();
    }

    @UiThread
    private void showPrompt(@NonNull final Dialog d) {
        d.show();
        UiUtils.setMessageTextSelectable(d, true);
        promptDialog = new WeakReference<>(d);
        dialogs.add(d);
    }

    @UiThread
    private void expirePrompt() {
        final Dialog d = promptDialog.get();
        if (d != null)
            d.dismiss();
    }

    private final Object msgQueueLock = new Object();
    private final ArrayList<LogMessage> msgQueue = new ArrayList<>();
    private WeakReference<MessageLogView.Adapter> msgAdapterRef = new WeakReference<>(null);

    private WeakReference<View> terminateButton = new WeakReference<>(null);
    private boolean showTerminateButton = false;

    @UiThread
    private void showQueuedMessages(@NonNull final Activity ctx) {
        if (ctx.isFinishing())
            return;
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
        v.addButton(R.layout.message_log_button,
                R.drawable.ic_check, R.string.action_close,
                view -> d.cancel(), -1);
        final ConfirmingImageButton terminate = (ConfirmingImageButton) v.addButton(
                R.layout.message_log_button_with_confirmation,
                R.drawable.ic_bar_poweroff, R.string.action_terminate,
                view -> {
                    final Activity activity = activityRef.getNoBlock();
                    if (activity instanceof ConsoleActivity)
                        ((ConsoleActivity) activity).onTerminate(null);
                }, 0);
        ImageViewCompat.setImageTintList(terminate, ColorStateList.valueOf(
                ctx.getResources().getColor(R.color.colorImportantDark)));
        terminate.setConfirmationMessage(ctx.getText(R.string.prompt_terminate_the_session));
        terminate.setVisibility(showTerminateButton ? View.VISIBLE : View.GONE);
        terminateButton = new WeakReference<>(terminate);
        dialogs.add(d);
    }

    public boolean isShowTerminateButton() {
        return showTerminateButton;
    }

    public void setShowTerminateButton(final boolean showTerminateButton) {
        this.showTerminateButton = showTerminateButton;
        final View terminate = terminateButton.get();
        if (terminate != null)
            terminate.setVisibility(showTerminateButton ? View.VISIBLE : View.GONE);
    }

    // Super session levels...
    public BackendUiInteractionActivityCtx parent = null;

    @Override
    @UiThread
    public void setActivity(@Nullable final Activity ctx) {
        final BackendUiInteractionActivityCtx p = parent;
        if (p != null)
            p.setActivity(ctx);
        if (ctx == activityRef.getNoBlock())
            return;
        synchronized (msgQueueLock) {
            msgAdapterRef = new WeakReference<>(null);
            activityRef.set(ctx);
            if (ctx != null) {
                final Runnable ps = promptState;
                if (ps != null)
                    ps.run();
                if (!msgQueue.isEmpty())
                    showQueuedMessages(ctx);
            } else {
                for (final Dialog d : dialogs)
                    d.dismiss();
                dialogs.clear();
            }
        }
    }

    @Override
    public void erase(@NonNull final CharSequence v) {
        if (v instanceof Erasable) { // Must try first
            ((Erasable) v).erase();
        } else if (v instanceof Editable) {
            handler.post(() -> {
                try {
                    ((Editable) v)
                            .replace(0, v.length(), CharBuffer.allocate(v.length()))
                            .clear();
                } catch (final Exception ignored) {
                }
            });
        } else {
            Misc.erase(v);
        }
    }

    private interface GetValue<T> {
        @NonNull
        T getValue();
    }

    private final class CustomFieldsBuilder {
        public CustomPrompt prompt = null;
        public final List<CustomField> fields = new ArrayList<>();

        public void submit() {
            for (final CustomField field : fields) {
                if (field instanceof CustomValueField) {
                    final CustomFieldAction action =
                            field.getOpts().action;
                    if (action instanceof CustomValueAction) {
                        ((CustomValueAction<Object>) action).onSubmit(prompt,
                                ((CustomValueField<?>) field).getValue());
                    }
                }
            }
        }

        public void cancel() {
            for (final CustomField field : fields) {
                if (field instanceof CustomValueField) {
                    final CustomFieldAction action =
                            field.getOpts().action;
                    if (action instanceof CustomTextInputAction &&
                            ((CustomTextInputAction) action).getType() ==
                                    CustomTextInputAction.Type.PASSWORD) {
                        erase(((CustomValueField<? extends CharSequence>) field).getValue());
                    }
                }
            }
        }

        private <T> CustomValueField<T> makeValueField(@NonNull final CustomFieldOpts fieldOpts,
                                                       @NonNull final GetValue<? extends T> onGetValue) {
            return new CustomValueField<T>() {
                @Override
                @NonNull
                public CustomFieldOpts getOpts() {
                    return fieldOpts;
                }

                @Override
                @NonNull
                public T getValue() {
                    return onGetValue.getValue();
                }
            };
        }

        public void build(@NonNull final ViewGroup container,
                          @NonNull final List<CustomFieldOpts> fieldsOpts,
                          @NonNull final Runnable onSubmit) {
            final Context ctx = container.getContext();
            final CustomPrompt prompt = new CustomPrompt() {
                @Override
                public void submit() {
                    onSubmit.run();
                }

                @Override
                @NonNull
                public List<CustomField> getFields() {
                    return fields;
                }
            };
            for (final CustomFieldOpts fieldOpts : fieldsOpts) {
                if (fieldOpts.action instanceof CustomButtonAction) {
                    final CustomButtonAction action =
                            (CustomButtonAction) fieldOpts.action;
                    final Button extraView = new AppCompatButton(ctx);
                    extraView.setText(fieldOpts.label);
                    extraView.setOnClickListener(_view ->
                            action.onClick(prompt));
                    fields.add(() -> fieldOpts);
                    container.addView(extraView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                } else if (fieldOpts.action instanceof CustomCheckboxAction) {
                    final CustomCheckboxAction action =
                            (CustomCheckboxAction) fieldOpts.action;
                    final CompoundButton fieldView = new AppCompatCheckBox(ctx);
                    fieldView.setText(fieldOpts.label);
                    fieldView.setChecked(action.onInit());
                    fields.add(makeValueField(fieldOpts, fieldView::isChecked));
                    container.addView(fieldView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                } else if (fieldOpts.action instanceof CustomTextInputAction) {
                    final CustomTextInputAction action =
                            (CustomTextInputAction) fieldOpts.action;
                    if (fieldOpts.label.length() > 0) {
                        final TextView fieldLabelView = new AppCompatTextView(ctx);
                        fieldLabelView.setText(fieldOpts.label);
                        container.addView(fieldLabelView, new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        ));
                    }
                    final EditText fieldView = new AppCompatEditText(ctx);
                    fieldView.setContentDescription(fieldOpts.label);
                    fieldView.setInputType(InputType.TYPE_CLASS_TEXT |
                            (action.getType() == CustomTextInputAction.Type.PASSWORD ?
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD :
                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
                    fieldView.setText(action.onInit());
                    fields.add(makeValueField(fieldOpts, fieldView::getText));
                    container.addView(fieldView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                } else if (fieldOpts.action == CustomFieldAction.label) {
                    final TextView fieldLabelView = new AppCompatTextView(ctx, null,
                            android.R.attr.textAppearanceMedium);
                    fieldLabelView.setTextIsSelectable(true);
                    fieldLabelView.setMovementMethod(UiUtils.getFixedLinkMovementMethod());
                    fieldLabelView.setText(fieldOpts.label);
                    container.addView(fieldLabelView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                }
            }
        }
    }

    @Override
    public boolean promptFields(@NonNull final List<CustomFieldOpts> fieldsOpts)
            throws InterruptedException {
        synchronized (promptLock) {
            try {
                final BlockingSync<Boolean> result = new BlockingSync<>();
                promptState = () -> {
                    if (isShowingPrompt())
                        return;
                    final Activity ctx = activityRef.getNoBlock();
                    if (ctx == null)
                        return;
                    final CustomFieldsBuilder customFields = new CustomFieldsBuilder();
                    final LinearLayoutCompat container = new LinearLayoutCompat(ctx);
                    container.setOrientation(LinearLayoutCompat.VERTICAL);
                    final ScrollView scroller = new ScrollView(ctx);
                    scroller.setVerticalFadingEdgeEnabled(true);
                    final ViewGroup.MarginLayoutParams containerLp =
                            new ViewGroup.MarginLayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                    final int margin = ctx.getResources()
                            .getDimensionPixelSize(R.dimen.text_margin);
                    containerLp.setMargins(margin, margin, margin, margin);
                    scroller.addView(container, containerLp);
                    scroller.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                    final DialogInterface.OnClickListener listener = (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            customFields.submit();
                            promptState = null;
                            result.set(true);
                            dialog.dismiss();
                        } else {
                            customFields.cancel();
                            promptState = null;
                            result.set(false);
                            dialog.dismiss();
                        }
                    };
                    final Dialog d = new AlertDialog.Builder(ctx)
                            .setOnCancelListener(dialog -> {
                                customFields.cancel();
                                promptState = null;
                                result.set(false);
                            })
                            .setCancelable(false)
                            .setView(scroller)
                            .setNegativeButton(android.R.string.cancel, listener)
                            .setPositiveButton(android.R.string.ok, listener)
                            .create();
                    customFields.build(container, fieldsOpts, () ->
                            listener.onClick(d, DialogInterface.BUTTON_POSITIVE));
                    showPrompt(d);
                };
                handler.post(promptState);
                return result.get();
            } finally {
                promptState = null;
                handler.post(this::expirePrompt);
            }
        }
    }

    @Override
    @Nullable
    public CharSequence promptPassword(@NonNull final CharSequence message,
                                       @NonNull final List<CustomFieldOpts> extras)
            throws InterruptedException {
        synchronized (promptLock) {
            try {
                final BlockingSync<CharSequence> result = new BlockingSync<>();
                promptState = () -> {
                    if (isShowingPrompt())
                        return;
                    final Activity ctx = activityRef.getNoBlock();
                    if (ctx == null)
                        return;
                    final CustomFieldsBuilder customFields = new CustomFieldsBuilder();
                    final LinearLayoutCompat container = new LinearLayoutCompat(ctx);
                    container.setOrientation(LinearLayoutCompat.VERTICAL);
                    container.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                    final EditText et = new AppCompatEditText(ctx);
                    container.addView(et, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                    final DialogInterface.OnClickListener listener = (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            customFields.submit();
                            promptState = null;
                            result.set(et.getText());
                            dialog.dismiss();
                        } else {
                            customFields.cancel();
                            promptState = null;
                            erase(et.getText());
                            result.set(null);
                            dialog.dismiss();
                        }
                    };
                    et.setInputType(InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    final Dialog d = new AlertDialog.Builder(ctx)
                            .setOnCancelListener(dialog -> {
                                customFields.cancel();
                                promptState = null;
                                erase(et.getText());
                                result.set(null);
                            })
                            .setCancelable(false)
                            .setMessage(message)
                            .setView(container)
                            .setNegativeButton(android.R.string.cancel, listener)
                            .setPositiveButton(android.R.string.ok, listener)
                            .create();
                    customFields.build(container, extras, () ->
                            listener.onClick(d, DialogInterface.BUTTON_POSITIVE));
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
            } finally {
                promptState = null;
                handler.post(this::expirePrompt);
            }
        }
    }

    @Override
    public boolean promptYesNo(@NonNull final CharSequence message) throws InterruptedException {
        synchronized (promptLock) {
            try {
                final BlockingSync<Boolean> result = new BlockingSync<>();
                promptState = () -> {
                    if (isShowingPrompt())
                        return;
                    final Activity ctx = activityRef.getNoBlock();
                    if (ctx == null)
                        return;
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
            } finally {
                promptState = null;
                handler.post(this::expirePrompt);
            }
        }
    }

    @Override
    public void showMessage(@NonNull final CharSequence message) {
        handler.post(() -> {
            synchronized (msgQueueLock) {
                final Activity ctx = activityRef.getNoBlock();
                if (ctx == null) {
                    msgQueue.add(new LogMessage(LogMessage.Level.INFO, message));
                    return;
                }
                msgQueue.add(new LogMessage(LogMessage.Level.INFO, message));
                final MessageLogView.Adapter a = msgAdapterRef.get();
                if (a != null)
                    a.notifyItemInserted(a.getItemCount() - 1);
                else
                    showQueuedMessages(ctx);
            }
        });
    }

    @Override
    public void showToast(@NonNull final CharSequence message) {
        final Activity ctx = activityRef.getNoBlock();
        if (ctx == null)
            return;
        ctx.runOnUiThread(() ->
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    @Nullable
    public byte[] promptContent(@NonNull final CharSequence message, @NonNull final String mimeType,
                                final long sizeLimit) throws InterruptedException, IOException {
        synchronized (promptLock) {
            try {
                final BlockingSync<Object> result = new BlockingSync<>();
                promptState = () -> {
                    if (isShowingPrompt())
                        return;
                    final Activity ctx = activityRef.getNoBlock();
                    if (ctx == null)
                        return;
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
                            .create();
                    showPrompt(d);
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
            } finally {
                promptState = null;
                handler.post(this::expirePrompt);
            }
        }
    }

    @Override
    public boolean promptPermissions(@NonNull final String[] perms) throws InterruptedException {
        final Activity ctx = activityRef.get();
        final int[] result = Permissions.requestBlocking(ctx, perms);
        boolean r = true;
        for (final int v : result)
            r &= v == PackageManager.PERMISSION_GRANTED;
        return r;
    }

    @Override
    @Nullable
    public Password getPassword(@NonNull final String target) {
        return PasswordService.get(target);
    }

    @Override
    public void putPassword(@NonNull final String target, @NonNull final CharSequence pwd) {
        PasswordService.put(target, pwd);
    }

    @Override
    public void erasePassword(@NonNull final String target, @Nullable final CharSequence pwd) {
        PasswordService.remove(target, pwd);
    }

    public boolean hasUi() {
        return activityRef.getNoBlock() != null;
    }

    public void waitForUi() throws InterruptedException {
        activityRef.get();
    }
}
