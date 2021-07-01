package green_green_avk.anotherterm;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.Comparator;

import green_green_avk.anotherterm.ui.HtmlTextView;

public final class NewsDialog {
    private NewsDialog() {
    }

    private static final class Entry {
        @StringRes
        private final int pageId;
        private final long timestamp;

        private Entry(@StringRes final int pageId, final long timestamp) {
            this.pageId = pageId;
            this.timestamp = timestamp;
        }

        private static final Comparator<Entry> timeOrder =
                (o1, o2) -> Long.compare(o1.timestamp, o2.timestamp);
    }

    private static final Entry[] news;

    static {
        news = new Entry[]{};
    }

    public static int getUnseen(@NonNull final Context ctx) {
        final SharedPreferences ps = PreferenceManager.getDefaultSharedPreferences(ctx);
        final long ts = ps.getLong("news_seen", 0);
        int idx = Arrays.binarySearch(news, new Entry(0, ts), Entry.timeOrder);
        if (idx < 0) idx = -idx - 1;
        return news.length - idx;
    }

    public static void setSeen(@NonNull final Context ctx) {
        if (news.length <= 0) return;
        final SharedPreferences ps = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor e = ps.edit();
        e.putLong("news_seen", news[news.length - 1].timestamp + 1);
        e.apply();
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
                    .inflate(R.layout.news_entry, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            final HtmlTextView v = (HtmlTextView) holder.itemView;
            v.setXmlText(v.getContext().getString(news[news.length - position - 1].pageId));
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
        final View v = LayoutInflater.from(ctx).inflate(R.layout.news_dialog, null);
        final RecyclerView list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(ctx));
        list.setAdapter(new Adapter(num));
        new AlertDialog.Builder(ctx).setView(v).setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> setSeen(ctx)).show();
    }

    public static void showUnseen(@NonNull final Context ctx) {
        final int unseen = getUnseen(ctx);
        if (unseen > 0) show(ctx, unseen);
    }
}
