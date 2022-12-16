package green_green_avk.anotherterm.ui;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public interface CategorizedCollectionView<T> {
    final class ItemCategory {
        @StringRes
        final int descRes;
        @ColorRes
        final int colorRes;

        public ItemCategory(@StringRes final int descRes, @ColorRes final int colorRes) {
            this.descRes = descRes;
            this.colorRes = colorRes;
        }
    }

    interface GetItemCategory<T> {
        @Nullable
        ItemCategory getItemCategory(@NonNull T item);
    }

    void setOnGetItemCategory(@Nullable GetItemCategory<? super T> v);
}
