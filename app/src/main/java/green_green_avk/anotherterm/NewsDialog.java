package green_green_avk.anotherterm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

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

        private static final Comparator<Entry> timeOrder = new Comparator<Entry>() {
            @Override
            public int compare(final Entry o1, final Entry o2) {
                return o1.timestamp < o2.timestamp ? -1 : o1.timestamp > o2.timestamp ? 1 : 0;
            }
        };
    }

    private static final Entry[] news;

    static {
        if (Build.VERSION.SDK_INT >= 29 && BuildConfig.TARGET_SDK_VERSION >= 29)
            news = new Entry[]{
                    new Entry(R.string.news_w_x,
                            Date.UTC(120, 9, 18, 0, 0, 0))
            };
        else if (Build.VERSION.SDK_INT >= 29 && "oldgoogleplay".equals(BuildConfig.FLAVOR))
            news = new Entry[]{
                    new Entry(R.string.news_w_x,
                            Date.UTC(120, 9, 17, 0, 0, 0))
            };
        else
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
            final HtmlTextView v = new HtmlTextView(parent.getContext());
            v.setTextIsSelectable(true);
            v.setBackgroundResource(android.R.drawable.gallery_thumb);
            return new ViewHolder(v);
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
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        setSeen(ctx);
                    }
                }).show();
    }

    public static void showUnseen(@NonNull final Context ctx) {
        final int unseen = getUnseen(ctx);
        if (unseen > 0) show(ctx, unseen);
    }
}
