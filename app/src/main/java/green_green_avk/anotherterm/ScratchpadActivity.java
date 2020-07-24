package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
                Collections.sort(list, new Comparator<ScratchpadManager.Entry>() {
                    @Override
                    public int compare(final ScratchpadManager.Entry o1,
                                       final ScratchpadManager.Entry o2) {
                        return (int) (o1.timestamp - o2.timestamp);
                    }
                });
                notifyDataSetChanged();
            }
        };

        public RecyclerAdapter(@NonNull final ScratchpadManager sm) {
            this.sm = sm;
            this.sm.addListener(onUpdate);
            onUpdate.run();
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
            if (position >= list.size()) return;
            final ScratchpadManager.Entry entry = list.get(position);
            holder.itemView.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final Context ctx = v.getContext();
                    UiUtils.confirm(ctx, ctx.getString(R.string.do_you_want_to_delete_this_entry),
                            new Runnable() {
                                @Override
                                public void run() {
                                    sm.remove(entry.name);
                                }
                            });
                }
            });
            holder.itemView.findViewById(R.id.copy).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final Uri uri;
                    try {
                        uri = sm.getUri(entry.name);
                    } catch (final FileNotFoundException e) {
                        return;
                    }
                    UiUtils.toClipboard(ScratchpadActivity.this, uri, getDesc(entry.name, 64));
                }
            });
            holder.itemView.findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final Uri uri;
                    try {
                        uri = sm.getUri(entry.name);
                    } catch (final FileNotFoundException e) {
                        return;
                    }
                    UiUtils.share(ScratchpadActivity.this, uri);
                }
            });
            holder.itemView.<TextView>findViewById(R.id.timestamp)
                    .setText(new Date(entry.timestamp).toString());
            holder.itemView.<TextView>findViewById(R.id.size)
                    .setText(UiUtils.makeHumanReadableBytes(entry.size));
            final TextView wDesc = holder.itemView.findViewById(R.id.description);
            // android:scrollHorizontally does not work
            wDesc.setHorizontallyScrolling(true);
            wDesc.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(final View v, final MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN)
                        if (wDesc.canScrollVertically(1) ||
                                wDesc.canScrollVertically(-1) ||
                                UiUtils.canScrollHorizontally(wDesc))
                            holder.parent.requestDisallowInterceptTouchEvent(true);
                    return false;
                }
            });
            wDesc.setText(getDesc(entry.name, DESCRIPTION_SIZE_LIMIT));
        }

        @Override
        public int getItemCount() {
            return Math.max(list.size(), 1);
        }

        @Override
        public int getItemViewType(final int position) {
            return list.size() > 0 ? 0 : 1;
        }
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ScratchpadManager sm = ((App) getApplication()).scratchpadManager;
        setContentView(R.layout.scratchpad_activity);
        this.<TextView>findViewById(R.id.location).setText(sm.locationDesc);
        final RecyclerView wList = findViewById(R.id.list);
        wList.setAdapter(new RecyclerAdapter(sm));
        wList.setLayoutManager(new LinearLayoutManager(this));
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
