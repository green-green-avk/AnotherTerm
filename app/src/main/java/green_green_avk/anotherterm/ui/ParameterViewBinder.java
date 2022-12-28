package green_green_avk.anotherterm.ui;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import java.util.Map;
import java.util.WeakHashMap;

import green_green_avk.anotherterm.utils.ValueConsumer;
import green_green_avk.anotherterm.utils.ValueProvider;

public final class ParameterViewBinder {
    private final Map<ParameterView<?>, ValueProvider<?>> map = new WeakHashMap<>();
    @NonNull
    private final Runnable onAnyChange;

    public ParameterViewBinder(@NonNull final Runnable onAnyChange) {
        this.onAnyChange = onAnyChange;
    }

    public <T> void bind(@NonNull final ParameterView<T> view,
                         @NonNull final ValueProvider<? extends T> provider,
                         @NonNull final ValueConsumer<? super T> consumer) {
        map.put(view, provider);
        view.setOnValueChanged(v -> {
            consumer.set(v);
            onAnyChange.run();
        });
    }

    public <T> void bind(@NonNull final View root,
                         @IdRes final int id,
                         @NonNull final ValueProvider<T> provider,
                         @NonNull final ValueConsumer<? super T> consumer) {
        bind(root.findViewById(id), provider, consumer);
    }

    public <T> void update(@NonNull final ParameterView<T> view) {
        final ValueProvider<T> provider = (ValueProvider<T>) map.get(view);
        if (provider != null)
            view.setValue(provider.get());
    }

    public void updateAll() {
        for (final Map.Entry<ParameterView<?>, ValueProvider<?>> entry : map.entrySet()) {
            ((ParameterView<Object>) entry.getKey()).setValue(entry.getValue().get());
        }
    }
}
