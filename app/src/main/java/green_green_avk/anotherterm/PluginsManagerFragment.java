package green_green_avk.anotherterm;

import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import green_green_avk.anothertermshellpluginutils.Plugin;

public final class PluginsManagerFragment extends Fragment {

    private static final class PluginsAdapter extends RecyclerView.Adapter {
        @NonNull
        private List<PackageInfo> plugins;
        @Keep // Can be collected otherwise
        private final PluginsManager.OnChanged onChanged = new PluginsManager.OnChanged() {
            @Override
            public void onPackagesChanged(@NonNull final List<PackageInfo> plugins) {
                PluginsAdapter.this.plugins = plugins;
                notifyDataSetChanged();
            }

            @Override
            public void onSettingsChanged() {
//                notifyItemRangeChanged(0, getItemCount()); // TODO: Animation
            }
        };

        private PluginsAdapter(@NonNull final Context ctx) {
            PluginsManager.registerOnChanged(onChanged);
            plugins = PluginsManager.getPlugins();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                                          final int viewType) {
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.plugins_manager_entry, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder,
                                     final int position) {
            final PackageInfo pkg = plugins.get(position);
            final TextView wWarning = holder.itemView.findViewById(R.id.warning);
            final CompoundButton wEnabled = holder.itemView.findViewById(R.id.enabled);
            final View wInfo = holder.itemView.findViewById(R.id.info);
            final TextView wPkgName = holder.itemView.findViewById(R.id.package_name);
            final ImageView wIcon = holder.itemView.findViewById(R.id.icon);
            final TextView wTitle = holder.itemView.findViewById(R.id.title);
            final PackageManager pm = holder.itemView.getContext().getPackageManager();
            wTitle.setText(pkg.applicationInfo.loadLabel(pm));
            wIcon.setImageDrawable(pkg.applicationInfo.loadIcon(pm));
            wPkgName.setText(pkg.packageName);
            wPkgName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final ClipboardManager clipboard =
                            (ClipboardManager) v.getContext()
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard == null) return;
                    clipboard.setPrimaryClip(ClipData.newPlainText(
                            null, pkg.packageName));
                    Toast.makeText(v.getContext(), R.string.msg_copied_to_clipboard,
                            Toast.LENGTH_SHORT).show();
                }
            });
            wEnabled.setChecked(PluginsManager.verify(pkg));
            wEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView,
                                             final boolean isChecked) {
                    if (isChecked) PluginsManager.grant(pkg);
                    else PluginsManager.revoke(pkg);
                }
            });
            wInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    v.getContext().startActivity(
                            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts(
                                            "package", pkg.packageName, null
                                    ))
                    );
                }
            });
            wWarning.setText(Plugin.isStalled(pkg.packageName) ?
                    "Stalled transactions detected, plugin seems broken"
                    : ""); // TODO: Dynamic update?
        }

        @Override
        public int getItemCount() {
            return plugins.size();
        }

        private static final class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull final View itemView) {
                super(itemView);
            }
        }
    }

    public PluginsManagerFragment() {
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_plugins_manager, container, false);
        final RecyclerView l = v.findViewById(R.id.plugins);
        l.setLayoutManager(new LinearLayoutManager(container.getContext()));
        l.setAdapter(new PluginsAdapter(container.getContext()));
        return v;
    }
}
