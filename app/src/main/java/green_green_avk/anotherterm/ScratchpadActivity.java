package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import green_green_avk.anotherterm.ui.UiUtils;

public final class ScratchpadActivity extends AppCompatActivity {

    private static final class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerView parent;

        public RecyclerViewHolder(@NonNull final View itemView, @NonNull final ViewGroup parent) {
            super(itemView);
            this.parent = (RecyclerView) parent;
        }
    }

    private final class RecyclerAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {

        private static final int DESCRIPTION_SIZE_LIMIT = 8192;

        @NonNull
        private final ScratchpadManager sm;
        private final List<ScratchpadManager.Entry> list = new ArrayList<>();

        @Keep
        private final Runnable onUpdate = new Runnable() {
            @Override
            public void run() {
                list.clear();
                list.addAll(sm.enumerate());
                Collections.sort(list, (o1, o2) -> Long.compare(o2.timestamp, o1.timestamp));
                notifyDataSetChanged();
            }
        };

        @Nullable
        private String currName = null;

        public RecyclerAdapter(@NonNull final ScratchpadManager sm) {
            this.sm = sm;
            this.sm.addListener(onUpdate);
            onUpdate.run();
        }

        private int getPosByName(@Nullable final String name) {
            for (int i = 0; i < list.size(); i++) {
                final ScratchpadManager.Entry entry = list.get(i);
                if (entry.name.equals(name))
                    return i;
            }
            return -1;
        }

        @NonNull
        private String getDesc(@NonNull final String name, final int len) {
            try {
                return sm.getDesc(name, len);
            } catch (final IOException e) {
                return "---";
            }
        }

        @NonNull
        @Override
        public RecyclerViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                                     final int viewType) {
            if (viewType == 0)
                return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.scratchpad_entry, parent, false), parent);
            return new RecyclerViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.empty, parent, false), parent);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull final RecyclerViewHolder holder,
                                     final int position) {
            if (position >= list.size())
                return;
            final ScratchpadManager.Entry entry = list.get(position);
            holder.itemView.findViewById(R.id.delete).setOnClickListener(v -> {
                final Context ctx = v.getContext();
                UiUtils.confirm(ctx, ctx.getString(R.string.msg_do_you_want_to_delete_this_entry),
                        () -> sm.remove(entry.name));
            });
            holder.itemView.findViewById(R.id.edit).setOnClickListener(v -> {
                final Uri uri;
                try {
                    uri = sm.getUri(entry.name);
                } catch (final FileNotFoundException e) {
                    return;
                }
                final String type = ScratchpadActivity.this.getContentResolver().getType(uri);
                final Intent i = new Intent(Intent.ACTION_EDIT)
                        .setDataAndType(uri, type != null ? type : "text/plain")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivity(Intent.createChooser(i, getString(R.string.action_edit)));
                currName = entry.name;
            });
            holder.itemView.findViewById(R.id.view).setOnClickListener(v -> {
                final Uri uri;
                try {
                    uri = sm.getUri(entry.name);
                } catch (final FileNotFoundException e) {
                    return;
                }
                final String type = ScratchpadActivity.this.getContentResolver().getType(uri);
                final Intent i = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, type != null ? type : "text/plain")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, getString(R.string.action_view)));
            });
            holder.itemView.findViewById(R.id.copy).setOnClickListener(v -> {
                final Uri uri;
                try {
                    uri = sm.getUri(entry.name);
                } catch (final FileNotFoundException e) {
                    return;
                }
                UiUtils.toClipboard(ScratchpadActivity.this, uri, getDesc(entry.name, 64));
            });
            holder.itemView.findViewById(R.id.share).setOnClickListener(v -> {
                final Uri uri;
                try {
                    uri = sm.getUri(entry.name);
                } catch (final FileNotFoundException e) {
                    return;
                }
                UiUtils.share(ScratchpadActivity.this, uri);
            });
            final TextView wName = holder.itemView.findViewById(R.id.name);
            wName.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    if (UiUtils.canScrollHorizontally(wName))
                        holder.parent.requestDisallowInterceptTouchEvent(true);
                return false;
            });
            wName.setText(entry.name);
            holder.itemView.<TextView>findViewById(R.id.timestamp)
                    .setText(new Date(entry.timestamp).toString());
            holder.itemView.<TextView>findViewById(R.id.size)
                    .setText(UiUtils.makeHumanReadableBytes(entry.size));
            final TextView wDesc = holder.itemView.findViewById(R.id.description);
            // android:scrollHorizontally does not work
            wDesc.setHorizontallyScrolling(true);
            wDesc.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    if (wDesc.canScrollVertically(1) ||
                            wDesc.canScrollVertically(-1) ||
                            UiUtils.canScrollHorizontally(wDesc))
                        holder.parent.requestDisallowInterceptTouchEvent(true);
                return false;
            });
            wDesc.setText(getDesc(entry.name, DESCRIPTION_SIZE_LIMIT));
        }

        @Override
        public int getItemCount() {
            return Math.max(list.size(), 1);
        }

        @Override
        public int getItemViewType(final int position) {
            return position >= list.size() ? 1 : 0;
        }
    }

    RecyclerView wList = null;

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && wList != null) {
            final RecyclerAdapter a = (RecyclerAdapter) wList.getAdapter();
            if (a != null && a.currName != null) {
                a.onUpdate.run();
                final int pos = a.getPosByName(a.currName);
                if (pos >= 0)
                    wList.smoothScrollToPosition(pos);
                a.currName = null;
            }
        }
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ScratchpadManager sm = ((App) getApplication()).scratchpadManager;
        setContentView(R.layout.scratchpad_activity);
        this.<TextView>findViewById(R.id.location).setText(sm.locationDesc);
        wList = findViewById(R.id.list);
        wList.setAdapter(new RecyclerAdapter(sm));
        final Point sz = new Point();
        getWindowManager().getDefaultDisplay().getSize(sz);
        final App.Settings settings = ((App) getApplication()).settings;
        final int minColWidthSp = settings.scratchpad_column_width_min_sp;
        final Resources rr = getResources();
        final int cols = minColWidthSp < rr.getInteger(
                R.integer.scratchpad_column_width_min_sp_max) ?
                Math.max(sz.x / (int) (minColWidthSp * rr.getDisplayMetrics().scaledDensity), 1)
                : 1;
        wList.setLayoutManager(new GridLayoutManager(this, cols,
                RecyclerView.VERTICAL, false));
        final int sp = getResources().getDimensionPixelSize(R.dimen.field_margin_2x);
        wList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull final Rect outRect, @NonNull final View view,
                                       @NonNull final RecyclerView parent,
                                       @NonNull final RecyclerView.State state) {
                final int pos = parent.getChildAdapterPosition(view);
                if (pos < 0)
                    outRect.setEmpty();
                else
                    outRect.set(pos % cols == 0 ? 0 : sp, pos < cols ? 0 : sp, 0, 0);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scratchpad, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onInfo(final MenuItem menuItem) {
        startActivity(new Intent(this, InfoActivity.class)
                .setData(Uri.parse("info://local/scratchpad")));
    }
}
