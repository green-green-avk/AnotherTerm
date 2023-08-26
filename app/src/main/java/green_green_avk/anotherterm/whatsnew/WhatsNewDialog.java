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
        private final int version;

        Entry(@StringRes final int pageId, final int version) {
            this.pageId = pageId;
            this.version = version;
        }
    }

    public static int getUnseen(@NonNull final Context ctx) {
        final SharedPreferences ps = PreferenceManager.getDefaultSharedPreferences(ctx);
        final int ver = ps.getInt("news_seen_v", BuildConfig.RELEASE_VERSION_CODE);
        int idx = 0;
        for (; idx < Info.news.length && Info.news[idx].version > ver; idx++) ;
        return idx;
    }

    public static void setSeen(@NonNull final Context ctx) {
        final SharedPreferences ps = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor ed = ps.edit();
        ed.putInt("news_seen_v", BuildConfig.RELEASE_VERSION_CODE);
        ed.apply();
    }

    private static final class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final int num;

        private Adapter(final int num) {
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
            v.setXmlText(v.getContext()
                    .getString(Info.news[position].pageId));
        }

        @Override
        public int getItemCount() {
            return num;
        }
    }

    private static final class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(@NonNull final View itemView) {
            super(itemView);
        }
    }

    public static void show(@NonNull final Context ctx, final int num) {
        final View v = LayoutInflater.from(ctx)
                .inflate(R.layout.whats_new_dialog, null);
        final RecyclerView list = v.findViewById(R.id.list);
        list.setAdapter(new Adapter(num));
        DialogUtils.wrapLeakageSafe(new AlertDialog.Builder(ctx)
                .setView(v)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> setSeen(ctx))
                .show(), null);
    }

    public static void showUnseen(@NonNull final Context ctx) {
        final int unseen = getUnseen(ctx);
        if (unseen > 0)
            show(ctx, unseen);
    }
}
