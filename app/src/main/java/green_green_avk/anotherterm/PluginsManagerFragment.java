package green_green_avk.anotherterm;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
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

import java.lang.ref.WeakReference;
import java.util.List;

import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anothertermshellpluginutils.Plugin;
import green_green_avk.anothertermshellpluginutils.Protocol;

public final class PluginsManagerFragment extends Fragment {

    private static final class PluginsAdapter
            extends RecyclerView.Adapter<PluginsAdapter.ViewHolder> {
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

        {
            PluginsManager.registerOnChanged(onChanged);
            plugins = PluginsManager.getPlugins();
        }

        @Override
        @NonNull
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent,
                                             final int viewType) {
            if (viewType == 0)
                return new ViewHolder(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.plugins_manager_entry,
                                        parent, false));
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.empty, parent, false));

        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            if (position >= plugins.size())
                return;
            final PackageInfo pkg = plugins.get(position);
            final TextView wWarning = holder.itemView.findViewById(R.id.warning);
            final CompoundButton wEnabled = holder.itemView.findViewById(R.id.enabled);
            final CompoundButton wEssential = holder.itemView.findViewById(R.id.essential);
            final View wInfo = holder.itemView.findViewById(R.id.info);
            final View wAppInfo = holder.itemView.findViewById(R.id.appInfo);
            final TextView wPkgName = holder.itemView.findViewById(R.id.package_name);
            final ImageView wIcon = holder.itemView.findViewById(R.id.icon);
            final TextView wTitle = holder.itemView.findViewById(R.id.title);
            final PackageManager pm = holder.itemView.getContext().getPackageManager();
            wTitle.setText(pkg.applicationInfo.loadLabel(pm));
            wIcon.setImageDrawable(pkg.applicationInfo.loadIcon(pm));
            wPkgName.setText(pkg.packageName);
            wPkgName.setOnClickListener(v -> {
                final ClipboardManager clipboard =
                        (ClipboardManager) v.getContext()
                                .getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard == null) return;
                clipboard.setPrimaryClip(ClipData.newPlainText(
                        null, pkg.packageName));
                Toast.makeText(v.getContext(), R.string.msg_copied_to_clipboard,
                        Toast.LENGTH_SHORT).show();
            });
            wEnabled.setChecked(PluginsManager.verify(pkg));
            wEnabled.setOnCheckedChangeListener((v, isChecked) -> {
                final Intent i = new Intent().setData(Uri.fromParts(
                        "package", BuildConfig.APPLICATION_ID,
                        isChecked ? null : "revoke"
                ));
                try {
                    UiUtils.requireActivity(v).startActivity(i.setClassName(
                            pkg.packageName,
                            "green_green_avk.anothertermshellpluginutils_perms.PermissionRequestActivity"
                    ));
                } catch (final SecurityException | ActivityNotFoundException e) {
                    // Old plugin
                    try {
                        v.getContext().sendBroadcast(i.setClassName(
                                pkg.packageName,
                                "green_green_avk.anothertermshellpluginutils_perms.PermissionRequestReceiver"
                        ));
                    } catch (final SecurityException ignored) {
                    }
                }
                if (isChecked) PluginsManager.grant(pkg);
                else PluginsManager.revoke(pkg);
            });
            wEssential.setChecked(PluginsManager.getBooleanFeature(pkg.packageName,
                    PluginsManager.F_ESSENTIAL));
            wEssential.setOnCheckedChangeListener((v, isChecked) ->
                    PluginsManager.setFeature(pkg.packageName, PluginsManager.F_ESSENTIAL,
                            isChecked));
            wInfo.setOnClickListener(v -> {
                final ComponentName cn = Plugin.getComponent(v.getContext(), pkg.packageName);
                if (cn == null) return;
                new InfoPageTask(v.getContext()).execute(cn);
            });
            wAppInfo.setOnClickListener(v -> {
                try {
                    v.getContext().startActivity(
                            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts(
                                            "package", pkg.packageName, null
                                    ))
                    );
                } catch (final ActivityNotFoundException | SecurityException ignored) {
                }
            });
            final boolean stalled = Plugin.isStalled(pkg.packageName);
            wWarning.setVisibility(stalled ? View.VISIBLE : View.GONE);
            wWarning.setText(stalled ?
                    wWarning.getContext()
                            .getString(R.string.msg_stalled_transactions_detected)
                    : ""); // TODO: Dynamic update?
        }

        private static final class InfoPageTask extends AsyncTask<Object, Object, Uri> {
            private static final Uri noInfoUri = Uri.parse("info://local/no_info");

            @SuppressLint("StaticFieldLeak")
            @NonNull
            private final Context appCtx;
            @NonNull
            private final WeakReference<Context> actCtx;

            private InfoPageTask(@NonNull final Context ctx) {
                this.appCtx = ctx.getApplicationContext();
                this.actCtx = new WeakReference<>(ctx);
            }

            @Override
            protected Uri doInBackground(final Object... args) {
                final ComponentName cn = (ComponentName) args[0];
                final int resId;
                final int type;
                try {
                    final Plugin plugin = Plugin.bind(appCtx, cn);
                    try {
                        final Bundle b = plugin.getMeta().data;
                        resId = b.getInt(Protocol.META_KEY_INFO_RES_ID, 0);
                        type = b.getInt(Protocol.META_KEY_INFO_RES_TYPE,
                                Protocol.STRING_CONTENT_TYPE_PLAIN);
                    } finally {
                        plugin.unbind();
                    }
                } catch (final Throwable e) {
                    return noInfoUri;
                }
                if (resId == 0) return noInfoUri;
                return new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(cn.getPackageName())
                        .appendPath(Integer.toString(resId))
                        .fragment(type == Protocol.STRING_CONTENT_TYPE_XML_AT ? "XML" : "PLAIN")
                        .build();
            }

            @Override
            protected void onPostExecute(final Uri uri) {
                final Context ctx = actCtx.get();
                if (ctx != null)
                    ctx.startActivity(new Intent(ctx, InfoActivity.class).setData(uri));
            }
        }

        @Override
        public int getItemCount() {
            return Math.max(plugins.size(), 1);
        }

        @Override
        public int getItemViewType(final int position) {
            return position >= plugins.size() ? 1 : 0;
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
        final View v = inflater.inflate(R.layout.plugins_manager_fragment, container, false);
        final RecyclerView l = v.findViewById(R.id.plugins);
        l.setLayoutManager(new LinearLayoutManager(container.getContext()));
        l.setAdapter(new PluginsAdapter());
        return v;
    }
}
