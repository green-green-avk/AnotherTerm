package green_green_avk.anotherterm.whatsnew;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.Comparator;

import green_green_avk.anotherterm.BuildConfig;
import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.ui.DialogUtils;
import green_green_avk.anotherterm.ui.HtmlTextView;

public final class WhatsNewDialog {
    private WhatsNewDialog() {
    }

    static final class Entry {
        @StringRes
        private final int pageId;
        private final long timestamp;

        Entry(@StringRes final int pageId, final long timestamp) {
            this.pageId = pageId;
            this.timestamp = timestamp;
        }

        private static final Comparator<Entry> timeOrder =
                (o1, o2) -> Long.compare(o1.timestamp, o2.timestamp);
    }

    private static void setVersion(@NonNull final SharedPreferences.Editor ed) {
        ed.putInt("news_seen_v", BuildConfig.VERSION_CODE_MAIN);
    }

    private static void setVersion(@NonNull final SharedPreferences ps) {
        final SharedPreferences.Editor ed = ps.edit();
        setVersion(ed);
        ed.apply();
    }

    public static int getUnseen(@NonNull final Context ctx) {
        final SharedPreferences ps = PreferenceManager.getDefaultSharedPreferences(ctx);
        final long ts = ps.getLong("news_seen", 0);
        final int ver = ps.getInt("news_seen_v", 0);
        if (ver == 0)
            setVersion(ps);
        int idx = Arrays.binarySearch(Info.news, new Entry(0, ts),
                Entry.timeOrder);
        if (idx < 0)
            idx = -idx - 1;
        return Info.news.length - idx;
    }

    public static void setSeen(@NonNull final Context ctx) {
        if (Info.news.length <= 0)
            return;
        final SharedPreferences ps = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor ed = ps.edit();
        ed.putLong("news_seen", Info.news[Info.news.length - 1].timestamp + 1);
        setVersion(ed);
        ed.apply();
    }

    private static final class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final int num;

        public Adapter(final int num) {
            this.num = num;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.whats_new_entry, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            final HtmlTextView v = (HtmlTextView) holder.itemView;
            v.setXmlText(v.getContext().getString(Info.news[Info.news.length - position - 1].pageId));
        }

        @Override
        public int getItemCount() {
            return num;
        }
    }

    private static final class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    public static void show(@NonNull final Context ctx, final int num) {
        final View v = LayoutInflater.from(ctx).inflate(R.layout.whats_new_dialog, null);
        final RecyclerView list = v.findViewById(R.id.list);
        list.setAdapter(new Adapter(num));
        DialogUtils.wrapLeakageSafe(new AlertDialog.Builder(ctx).setView(v).setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> setSeen(ctx)).show(), null);
    }

    public static void showUnseen(@NonNull final Context ctx) {
        final int unseen = getUnseen(ctx);
        if (unseen > 0)
            show(ctx, unseen);
    }
}
