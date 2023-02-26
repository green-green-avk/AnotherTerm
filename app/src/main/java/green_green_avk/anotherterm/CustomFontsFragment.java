package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import green_green_avk.anotherterm.utils.Misc;

public final class CustomFontsFragment extends Fragment {
    private static final int FONT_TYPES = 4;
    private static final int IDS_OFFSET_FONT = 1;

    private final class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        private final Typeface[] typefaces = new Typeface[FONT_TYPES];

        public void refresh() {
            FontsManager.loadFromFiles(typefaces, FontsManager.getConsoleFontFiles());
            notifyItemRangeChanged(0, getItemCount());
        }

        {
            refresh();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                             final int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.custom_fonts_entry, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder,
                                     final int position) {
            final TextView wSample = holder.itemView.findViewById(R.id.sample);
            final View wSet = holder.itemView.findViewById(R.id.set);
            final View wUnset = holder.itemView.findViewById(R.id.unset);
            final boolean canBeSet = typefaces[0] != null || position == 0;
            final boolean canBeRemoved = FontsManager.getConsoleFontFiles()[position].exists();
            final boolean isCorrect =
                    FontsManager.exists(typefaces, position) || !canBeRemoved;
            if (isCorrect) {
                FontsManager.populatePaint(wSample.getPaint(), typefaces, position);
                wSample.setTextColor(holder.itemView.getResources()
                        .getColor(R.color.colorPrimaryDark));
                wSample.setText(R.string.sample_text);
            } else {
                wSample.setTypeface(null, position);
                wSample.setTextColor(holder.itemView.getResources()
                        .getColor(R.color.colorHintWarning));
                wSample.setText(R.string.msg_not_a_valid_font);
            }
            wSet.setEnabled(canBeSet);
            wSet.setVisibility(canBeSet ? View.VISIBLE : View.INVISIBLE);
            wSet.setOnClickListener(v -> {
                final Intent i = new Intent(Intent.ACTION_GET_CONTENT)
                        .addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                startActivityForResult(Intent.createChooser(i, "Pick a font"),
                        position + IDS_OFFSET_FONT);
            });
            wUnset.setEnabled(canBeRemoved);
            wUnset.setVisibility(canBeRemoved ? View.VISIBLE : View.INVISIBLE);
            wUnset.setOnClickListener(v -> {
                if (position == 0)
                    for (final File f : FontsManager.getConsoleFontFiles()) f.delete();
                else FontsManager.getConsoleFontFiles()[position].delete();
                refresh();
            });
        }

        @Override
        public int getItemCount() {
            return typefaces.length;
        }

        private final class ViewHolder extends RecyclerView.ViewHolder {
            private ViewHolder(@NonNull final View itemView) {
                super(itemView);
            }
        }
    }

    private Adapter adapter = null;

    private void refresh() {
        final Adapter a = adapter;
        if (a != null)
            a.refresh();
    }

    @SuppressLint("StaticFieldLeak")
    private void onFontPicked(final int fontPos, @Nullable final Uri uri) {
        if (uri == null)
            return;
        FontsManager.prepareConsoleFontDir(true);
        final InputStream is;
        final OutputStream os;
        try {
            is = getActivity().getContentResolver().openInputStream(uri);
            if (is == null)
                return;
            os = new FileOutputStream(FontsManager.getConsoleFontFiles()[fontPos]);
        } catch (final FileNotFoundException e) {
            return;
        }
        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(final Object... objects) {
                try {
                    Misc.copy(os, is);
                } catch (final IOException ignored) {
                } finally {
                    try {
                        is.close();
                    } catch (final IOException ignored) {
                    }
                    try {
                        os.close();
                    } catch (final IOException ignored) {
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(final Object o) {
                refresh(); // TODO: Suppose this task fast-running at the moment...
            }
        }.execute();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null)
            return;
        if (requestCode >= IDS_OFFSET_FONT && requestCode < (IDS_OFFSET_FONT + FONT_TYPES))
            onFontPicked(requestCode - IDS_OFFSET_FONT, data.getData());
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.custom_fonts_fragment,
                container, false);
        final TextView wLocation = v.findViewById(R.id.location);
        final RecyclerView wFont = v.findViewById(R.id.font);
        final CompoundButton wUse = v.findViewById(R.id.use);
        final Adapter a = new Adapter();
        wFont.setAdapter(a);
        adapter = a;
        final SharedPreferences appSP = PreferenceManager.getDefaultSharedPreferences(
                container.getContext().getApplicationContext());
        wUse.setChecked(((App) getActivity().getApplication()).settings
                .terminal_font_default_fromfiles);
        wUse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            final SharedPreferences.Editor spe = appSP.edit();
            spe.putBoolean("terminal_font_default_fromfiles", isChecked);
            spe.apply();
        });
        wLocation.setText(FontsManager.LOCATION_DESC);
        return v;
    }
}
