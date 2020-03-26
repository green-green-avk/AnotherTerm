package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import green_green_avk.anotherterm.utils.Misc;

public final class CustomFontsFragment extends Fragment {

    private static final class Adapter extends RecyclerView.Adapter {

        private final Typeface[] typefaces = new Typeface[4];

        private void refresh() {
            FontsManager.loadFromFilesFb(typefaces, FontsManager.getConsoleFontFiles());
            notifyItemRangeChanged(0, getItemCount());
        }

        {
            refresh();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                                          final int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.custom_fonts_entry, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder,
                                     final int position) {
            final TextView wSample = holder.itemView.findViewById(R.id.sample);
            final View wSet = holder.itemView.findViewById(R.id.set);
            final View wUnset = holder.itemView.findViewById(R.id.unset);
            final boolean canBeSet = typefaces[0] != null || position == 0;
            final boolean canBeRemoved = FontsManager.getConsoleFontFiles()[position].exists();
            final boolean isCorrect = FontsManager.isExists(typefaces, position) || !canBeRemoved;
            if (isCorrect) {
                FontsManager.setPaint(wSample.getPaint(), typefaces, position);
                wSample.setTextColor(holder.itemView.getResources().getColor(R.color.colorPrimaryDark));
                wSample.setText(R.string.sample_text);
            } else {
                wSample.setTypeface(null, position);
                wSample.setTextColor(holder.itemView.getResources().getColor(R.color.colorHintWarning));
                wSample.setText(R.string.msg_not_a_valid_font);
            }
            wSet.setEnabled(canBeSet);
            wSet.setVisibility(canBeSet ? View.VISIBLE : View.INVISIBLE);
            wSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final RequesterActivity.OnResult onResult = new RequesterActivity.OnResult() {
                        @SuppressLint("StaticFieldLeak")
                        @Override
                        public void onResult(@Nullable final Intent result) {
                            if (result == null) return;
                            final Uri uri = result.getData();
                            if (uri == null) return;
                            FontsManager.prepareConsoleFontDir(true);
                            final InputStream is;
                            final OutputStream os;
                            try {
                                is = v.getContext().getContentResolver().openInputStream(uri);
                                if (is == null) return;
                                os = new FileOutputStream(FontsManager.getConsoleFontFiles()[position]);
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
                                    refresh(); // TODO: Correct this pattern - not good.
                                }
                            }.execute();
                        }
                    };
                    final Intent i = new Intent(Intent.ACTION_GET_CONTENT)
                            .addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                    RequesterActivity.request(
                            v.getContext(), Intent.createChooser(i, "Pick a font"), onResult);
                }
            });
            wUnset.setEnabled(canBeRemoved);
            wUnset.setVisibility(canBeRemoved ? View.VISIBLE : View.INVISIBLE);
            wUnset.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (position == 0)
                        for (final File f : FontsManager.getConsoleFontFiles()) f.delete();
                    else FontsManager.getConsoleFontFiles()[position].delete();
                    refresh();
                }
            });
        }

        @Override
        public int getItemCount() {
            return typefaces.length;
        }

        private static final class ViewHolder extends RecyclerView.ViewHolder {
            private ViewHolder(@NonNull final View itemView) {
                super(itemView);
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_custom_fonts, container, false);
        final TextView wLocation = v.findViewById(R.id.location);
        final RecyclerView wFont = v.findViewById(R.id.font);
        final CompoundButton wUse = v.findViewById(R.id.use);
        wFont.setLayoutManager(new LinearLayoutManager(container.getContext()));
        wFont.setAdapter(new Adapter());
        final SharedPreferences appSP = PreferenceManager.getDefaultSharedPreferences(
                container.getContext().getApplicationContext());
        wUse.setChecked(appSP.getBoolean("terminal_font_default_fromfiles", false));
        wUse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                final SharedPreferences.Editor spe = appSP.edit();
                spe.putBoolean("terminal_font_default_fromfiles", isChecked);
                spe.apply();
            }
        });
        wLocation.setText(FontsManager.LOCATION_DESC);
        return v;
    }
}
