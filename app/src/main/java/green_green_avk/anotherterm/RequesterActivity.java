package green_green_avk.anotherterm;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.concurrent.atomic.AtomicInteger;

public final class RequesterActivity extends AppCompatActivity {

    private static final AtomicInteger s_id = new AtomicInteger(0);

    private static int obtainId() {
        return s_id.getAndIncrement();
    }

    public interface OnResult {
        void onResult(@Nullable Intent result);
    }

    private static final class RequestData {
        private final int id;
        @Nullable
        private final OnResult onResult;
        private final boolean cancelOnClose;

        private RequestData(final int id, @Nullable final OnResult onResult,
                            final boolean cancelOnClose) {
            this.id = id;
            this.onResult = onResult;
            this.cancelOnClose = cancelOnClose;
        }
    }

    public static final class Request {
        @Nullable
        private Context appCtx;
        private final int id;

        private Request(@NonNull final Context ctx, final int id) {
            this.appCtx = ctx.getApplicationContext();
            this.id = id;
        }

        public void cancel() {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (appCtx != null) {
                    returnResult(id, null);
                    removeRequest(appCtx, id);
                    appCtx = null;
                }
            });
        }
    }

    private static final SparseArray<RequestData> requests = new SparseArray<>();

    @NonNull
    private static Intent makeOwnIntent(@NonNull final Context ctx,
                                        final int id,
                                        @NonNull final Intent request) {
        final Intent i = new Intent(ctx, RequesterActivity.class);
        i.setAction(C.IFK_ACTION_NEW);
        i.putExtra(C.IFK_MSG_ID, id);
        i.putExtra(C.IFK_MSG_INTENT, request);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    @NonNull
    private static PendingIntent makeOwnPendingIntent(@NonNull final Context ctx,
                                                      @NonNull final Intent ownIntent) {
        return PendingIntent.getActivity(ctx,
                ownIntent.getIntExtra(C.IFK_MSG_ID, 0),
                ownIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @NonNull
    private static Notification makeOwnNotification(@NonNull final Context ctx,
                                                    final String channelId,
                                                    final int priority,
                                                    @NonNull final Intent ownIntent,
                                                    @NonNull final CharSequence title,
                                                    @NonNull final CharSequence message) {
        final Intent rmIntent = new Intent(ownIntent);
        rmIntent.setAction(C.IFK_ACTION_CANCEL);
        return new NotificationCompat.Builder(ctx, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_stat_question)
                .setPriority(priority)
                .setContentIntent(makeOwnPendingIntent(ctx, ownIntent))
                .setDeleteIntent(makeOwnPendingIntent(ctx, rmIntent))
                .build();
    }

    private static void removeRequest(@NonNull final Context ctx, final int id) {
        NotificationManagerCompat.from(ctx).cancel(C.REQUEST_USER_TAG, id);
        requests.remove(id);
    }

    private static void returnResult(final int id, @Nullable final Intent data) {
        final RequestData rd = requests.get(id);
        if (rd != null && rd.onResult != null)
            rd.onResult.onResult(data);
    }

    private static boolean shouldCancelOnClose(final int id) {
        final RequestData rd = requests.get(id);
        return rd == null || rd.cancelOnClose;
    }

    public static void showAsNotification(@NonNull final Context ctx, @NonNull final Intent intent,
                                          @NonNull final CharSequence title,
                                          @NonNull final CharSequence message,
                                          @NonNull final String channelId,
                                          final int priority) {
        final int id = obtainId();
        new Handler(Looper.getMainLooper()).post(() -> {
            final Notification n = new NotificationCompat.Builder(ctx, channelId)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setSmallIcon(R.drawable.ic_stat_question)
                    .setPriority(priority)
                    .setContentIntent(makeOwnPendingIntent(ctx, intent))
                    .build();
            NotificationManagerCompat.from(ctx).notify(C.REQUEST_USER_TAG, id, n);
        });
    }

    @NonNull
    public static Request request(@NonNull final Context ctx, @NonNull final Intent intent,
                                  @Nullable final OnResult onResult,
                                  @NonNull final CharSequence title,
                                  @NonNull final CharSequence message,
                                  @Nullable final String channelId,
                                  final int priority,
                                  final boolean immediate) {
        final int id = obtainId();
        new Handler(Looper.getMainLooper()).post(() -> {
            requests.append(id, new RequestData(id, onResult, immediate));
            final Notification n = makeOwnNotification(ctx, channelId, priority,
                    makeOwnIntent(ctx, id, intent), title, message);
            NotificationManagerCompat.from(ctx).notify(C.REQUEST_USER_TAG, id, n);
            if (immediate) {
                ctx.startActivity(makeOwnIntent(ctx, id, intent));
            }
        });
        return new Request(ctx, id);
    }

    private int ownRequestCode = -1;
    private boolean restarted = false;
    private Runnable toResult = null;

    private void processIntent(@Nullable final Intent ownIntent) {
        if (ownIntent == null || !ownIntent.hasExtra(C.IFK_MSG_ID)) {
            finish();
            return;
        }
        final int id = ownIntent.getIntExtra(C.IFK_MSG_ID, 0);
        ownRequestCode = id;
        final Intent intent = ownIntent.getParcelableExtra(C.IFK_MSG_INTENT);
        if (C.IFK_ACTION_CANCEL.equals(ownIntent.getAction()) || intent == null) {
            returnResult(id, null);
            removeRequest(getApplicationContext(), id);
            finish();
            return;
        }
        startActivityForResult(intent, id);
    }

    private void processResult(final int requestCode, final int resultCode,
                               @Nullable final Intent data) {
        if (resultCode == RESULT_OK) {
            returnResult(requestCode, data);
            removeRequest(getApplicationContext(), requestCode);
        } else if (shouldCancelOnClose(requestCode)) {
            returnResult(requestCode, null);
            removeRequest(getApplicationContext(), requestCode);
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        restarted = true;
        finishActivity(ownRequestCode);
        processIntent(getIntent());
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            processIntent(getIntent());
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // At least, some frameworks automatically cancel the started-for-result activity
        // and call this method before `onNewIntent()` with `resultCode` = `RESULT_CANCELED`
        // thus the decision must be postponed.
        toResult = () -> {
            processResult(requestCode, resultCode, data);
            finish();
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!restarted && toResult != null) {
            toResult.run();
        }
        restarted = false;
        toResult = null;
    }
}
