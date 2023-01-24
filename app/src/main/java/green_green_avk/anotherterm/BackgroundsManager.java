package green_green_avk.anotherterm;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import green_green_avk.anotherterm.ui.UiUtils;
import green_green_avk.anotherterm.utils.CollectionsViewSet;
import green_green_avk.anotherterm.utils.FileDrawableSource;
import green_green_avk.anotherterm.utils.ProfileManager;

public final class BackgroundsManager extends ProfileManager<BackgroundProfile> {
    private final Set<BuiltIn<BackgroundProfile>> defaultSet = new HashSet<>();
    @NonNull
    private final FileDrawableSource localSource;
    private final Set<BuiltIn<BackgroundProfile>> localSet = new HashSet<>();
    @NonNull
    private final Set<BuiltIn<? extends BackgroundProfile>> builtIns;

    private void reloadLocalSource() {
        final Set<?> keys = localSource.enumerate();
        localSet.clear();
        for (final Object key : keys) {
            final Drawable data;
            try {
                data = localSource.get(key);
            } catch (final Exception e) {
                continue;
            }
            localSet.add(new BuiltIn<>(" " + key, key.toString(),
                    new SimpleBackgroundProfile(data), 0x10));
        }
    }

    public BackgroundsManager(@NonNull final Context ctx) {
        defaultSet.add(new BuiltIn<>("", R.string.profile_title_builtin, new SimpleBackgroundProfile(
                UiUtils.requireDrawable(ctx, R.drawable.bg_term_screen_blank)),
                0));
        defaultSet.add(new BuiltIn<>(" lines", R.string.profile_title_builtin, new SimpleBackgroundProfile(
                UiUtils.requireDrawable(ctx, R.drawable.bg_term_screen_lines)),
                1));
        defaultSet.add(new BuiltIn<>(" lines_fade", R.string.profile_title_builtin, new SimpleBackgroundProfile(
                UiUtils.requireDrawable(ctx, R.drawable.bg_term_screen_lines_fade)),
                2));
        localSource = new FileDrawableSource(ctx, "backgrounds");
        localSource.setOnChanged(() -> {
            reloadLocalSource();
            execOnChangeListeners();
        });
        builtIns = new CollectionsViewSet<>(defaultSet, localSet);
        reloadLocalSource();
    }

    @Override
    public boolean containsCustom(@NonNull final String name) {
        return false;
    }

    @Override
    @NonNull
    public Set<? extends Meta> enumerate() {
        return enumerateBuiltIn();
    }

    @Override
    @NonNull
    public Set<BuiltIn<? extends BackgroundProfile>> enumerateBuiltIn() {
        return builtIns;
    }

    @Override
    @NonNull
    public Set<? extends Meta> enumerateCustom() {
        return Collections.emptySet();
    }

    @Override
    @Nullable
    public Meta getMeta(@Nullable final String name) {
        for (final BuiltIn<? extends BackgroundProfile> builtIn : builtIns) {
            if (builtIn.name.equals(name)) {
                return builtIn;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Meta getMeta(@NonNull final BackgroundProfile data) {
        for (final BuiltIn<? extends BackgroundProfile> builtIn : builtIns) {
            if (builtIn.data.equals(data)) {
                return builtIn;
            }
        }
        return null;
    }

    @Override
    @NonNull
    public BackgroundProfile get(@Nullable final String name) {
        final Meta r = getMeta(name);
        return r instanceof BuiltIn ?
                ((BuiltIn<? extends BackgroundProfile>) r).data : defaultSet.iterator().next().data;
    }

    @Override
    public void remove(@NonNull final String name) {
    }
}
