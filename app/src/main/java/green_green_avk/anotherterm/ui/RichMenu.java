package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Elements can be clicked also by sliding pointer down.
 */
public final class RichMenu {
    @LayoutRes
    private final int containerLayoutRes;
    @LayoutRes
    private final int itemLayoutRes;

    private WheelPopupViewWrapper wrapper = null;

    public RichMenu(@LayoutRes final int containerLayoutRes, @LayoutRes final int itemLayoutRes) {
        this.containerLayoutRes = containerLayoutRes;
        this.itemLayoutRes = itemLayoutRes;
    }

    public static final class Item {
        @NonNull
        public final Drawable icon;
        @NonNull
        public final CharSequence description;
        @NonNull
        public final Runnable onClick;

        public Item(@NonNull final Drawable icon, @NonNull final CharSequence description,
                    @NonNull final Runnable onClick) {
            this.icon = icon;
            this.description = description;
            this.onClick = onClick;
        }
    }

    @Nullable
    private Collection<Item> items = null;

    public void setItems(@Nullable final Collection<Item> items) {
        this.items = items != null ? items : Collections.emptyList();
        rebuildContent();
    }

    private static final class RichMenuViewWrapper extends WheelPopupViewWrapper {
        public RichMenuViewWrapper(@NonNull final View ownerView) {
            super(ownerView);
        }

        @Override
        public int getPopupRefX() {
            return ownerView.getWidth() / 2;
        }

        @Override
        public int getPopupRefY() {
            return -ownerView.getHeight() / 2;
        }

        @Override
        protected int getAnimationStyle() {
            return android.R.style.Animation_Dialog;
        }
    }

    public void wrap(@NonNull final View view) {
        wrapper = new RichMenuViewWrapper(view);
        view.setOnClickListener(v -> wrapper.open());
        rebuildContent();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void rebuildContent() {
        if (wrapper == null || items == null)
            return;
        wrapper.setContentView(containerLayoutRes);
        final ViewGroup container = wrapper.getContentView();
        final LayoutInflater inflater = LayoutInflater.from(container.getContext());
        for (final Item item : items) {
            final ImageButton itemView = (ImageButton)
                    inflater.inflate(itemLayoutRes, container, false);
            itemView.setImageDrawable(item.icon);
            itemView.setContentDescription(item.description);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                itemView.setTooltipText(item.description);
            itemView.setOnTouchListener((view, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    view.performClick();
                return true;
            });
            itemView.setOnClickListener(view -> {
                wrapper.dismiss();
                item.onClick.run();
            });
            container.addView(itemView);
        }
    }
}
