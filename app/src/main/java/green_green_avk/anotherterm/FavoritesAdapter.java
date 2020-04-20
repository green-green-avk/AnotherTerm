package green_green_avk.anotherterm;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;

import green_green_avk.anotherterm.backends.BackendsList;
import green_green_avk.anotherterm.utils.PreferenceStorage;

public final class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {
    private String[] mDatasetKeys;
    private View.OnClickListener mOnClick = null;
    private View.OnCreateContextMenuListener mOnCreateContextMenuListener = null;

    @Keep // Must be kept to prevent its unexpected collection
    private final Runnable onFavsChanged = new Runnable() {
        @Override
        public void run() {
            updateDataset();
        }
    };

    public FavoritesAdapter() {
        FavoritesManager.addOnChangeListener(onFavsChanged);
        updateDataset();
    }

    public void updateDataset() { // TODO: ...
        final String[] keys = FavoritesManager.enumerate().toArray(new String[0]);
        Arrays.sort(keys);
        mDatasetKeys = keys;
        notifyDataSetChanged();
    }

    public String getName(final int i) {
        return mDatasetKeys[i];
    }

    public void setOnClickListener(final View.OnClickListener v) {
        mOnClick = v;
    }

    public void setOnCreateContextMenuListener(final View.OnCreateContextMenuListener v) {
        mOnCreateContextMenuListener = v;
    }

    @NonNull
    @Override
    public FavoritesAdapter.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                                          final int viewType) {
        final View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.favorites_entry, parent, false);
        v.setOnClickListener(mOnClick);
        v.setOnCreateContextMenuListener(mOnCreateContextMenuListener);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final String name = mDatasetKeys[position];
        final TextView nameView = holder.itemView.findViewById(R.id.name);
        nameView.setText(name);
        final PreferenceStorage ps = FavoritesManager.get(name);
        final int bli = BackendsList.getId(ps.get("type"));
        if (bli < 0) return;
        final BackendsList.Item blit = BackendsList.get(bli);
        final Context ctx = holder.itemView.getContext();
        final int textColor = nameView.getHintTextColors().getDefaultColor();
        final ImageView iconView = holder.itemView.findViewById(R.id.icon);
        if (iconView instanceof AppCompatImageView) {
            ImageViewCompat.setImageTintList(iconView,
                    ColorStateList.valueOf(textColor));
            iconView.setImageResource(blit.icon);
        } else {
            final Drawable icon = ctx.getResources().getDrawable(blit.icon);
            icon.mutate().setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
            iconView.setImageDrawable(icon);
        }
    }

    @Override
    public int getItemCount() {
        return mDatasetKeys.length;
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(final View v) {
            super(v);
        }
    }
}
