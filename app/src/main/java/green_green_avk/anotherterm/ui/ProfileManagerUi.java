package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;

import green_green_avk.anotherterm.R;
import green_green_avk.anotherterm.utils.ProfileManager;

public abstract class ProfileManagerUi<T> {
    @NonNull
    public abstract ProfileManager<T> getManager(@NonNull Context ctx);

    @NonNull
    public abstract ProfileAdapter<T> createAdapter(@NonNull Context ctx);

    public abstract void startEditor(@NonNull Context ctx);

    private static Spanned getWarnTitle(@NonNull final Context ctx, @StringRes final int resId) {
        final SpannableString v = new SpannableString(ctx.getString(resId));
        v.setSpan(new StyleSpan(Typeface.ITALIC), 0, v.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return v;
    }

    /**
     * For weird cases when you prefer to use a custom view instead of any sort of
     * {@link android.widget.Spinner}.
     *
     * @param ctx  a context to use
     * @param data a profile to retrieve a title for
     */
    @NonNull
    public Spanned getTitle(@NonNull final Context ctx,
                            @NonNull final T data) {
        final ProfileManager.Meta meta = getManager(ctx).getMeta(data);
        if (meta == null)
            return getWarnTitle(ctx, R.string.profile_title_anonymous);
        final SpannableString v = new SpannableString(meta.getTitle(ctx));
        if (meta.isBuiltIn)
            v.setSpan(new StyleSpan(Typeface.ITALIC), 0, v.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return v;
    }

    /**
     * For weird cases when you prefer to use a custom view instead of any sort of
     * {@link android.widget.Spinner}.
     *
     * @param where a container to update
     * @param meta  what to place
     */
    public void renderIn(@NonNull final ViewGroup where,
                         @Nullable final ProfileManager.Meta meta) {
        if (meta == null) {
            where.removeAllViews();
            return;
        }
        final ProfileAdapter<T> adapter = this.createAdapter(where.getContext())
                .setDropDownItemLayoutRes(R.layout.profile_manager_spinner_entry);
        final View target;
        if (where.getChildCount() > 0) {
            target = where.getChildAt(0);
        } else {
            target = adapter.createEntryFor(where);
            where.addView(target);
        }
        adapter.bindEntryTo(target, meta);
    }

    /**
     * For weird cases when you prefer to use a custom view instead of any sort of
     * {@link android.widget.Spinner}.
     *
     * @param where   a container to update
     * @param profile what to place
     */
    public void renderIn(@NonNull final ViewGroup where,
                         @Nullable final T profile) {
        final ProfileManager.Meta meta = profile != null ?
                this.getManager(where.getContext()).getMeta(profile) : null;
        renderIn(where, meta);
    }

    public interface Dismissible {
        void dismiss();
    }

    private static final String MSG_ILL_PARENT = "`parent' can be View or Context only";

    @NonNull
    public Dismissible showList(@NonNull final Object parent,
                                @NonNull final ProfileAdapter.OnClickListener onClickListener,
                                @NonNull final T data) {
        final Context ctx;
        if (parent instanceof View)
            ctx = ((View) parent).getContext();
        else if (parent instanceof Context)
            ctx = (Context) parent;
        else
            throw new IllegalArgumentException(MSG_ILL_PARENT);
        final ProfileManager.Meta meta = getManager(ctx).getMeta(data);
        return showList(parent, onClickListener,
                meta == null ? null : meta.name);
    }

    @NonNull
    public Dismissible showList(@NonNull final Object parent,
                                @NonNull final ProfileAdapter.OnClickListener onClickListener) {
        return showList(parent, onClickListener, (String) null);
    }

    @NonNull
    public Dismissible showList(@NonNull final Object parent,
                                @NonNull final ProfileAdapter.OnClickListener onClickListener,
                                @Nullable final String name) {
        if (parent instanceof View) {
            final Context ctx = ((View) parent).getContext();
            final PopupWindow d = new ExtPopupWindow(ctx);
            d.setBackgroundDrawable(AppCompatResources.getDrawable(ctx,
                    android.R.drawable.dialog_holo_light_frame));
            d.setFocusable(true);
            d.setAnimationStyle(android.R.style.Animation_Dialog);
            final ListView v = new ListView(ctx);
            final ProfileAdapter<T> a = createAdapter(ctx)
                    .setIncludeBuiltIns(true)
                    .setItemLayoutRes(R.layout.profile_manager_dialog_entry)
                    .setDropDownItemLayoutRes(R.layout.profile_manager_dialog_entry)
                    .setOnClickListener(meta -> {
                        onClickListener.onClick(meta);
                        d.dismiss();
                    })
                    .setMarked(name)
                    .setEditorEnabled(true);
            v.setAdapter(a.getAdapter());
            v.setFocusable(false);
            d.setContentView(v);
            d.showAsDropDown((View) parent);
            return d::dismiss;
        }
        if (parent instanceof Context) {
            final Context ctx = (Context) parent;
            final AlertDialog d = new AlertDialog.Builder(ctx).create();
            final ListView v = new ListView(ctx);
            final ProfileAdapter<T> a = createAdapter(ctx)
                    .setIncludeBuiltIns(true)
                    .setItemLayoutRes(R.layout.profile_manager_dialog_entry)
                    .setDropDownItemLayoutRes(R.layout.profile_manager_dialog_entry)
                    .setOnClickListener(meta -> {
                        onClickListener.onClick(meta);
                        d.dismiss();
                    })
                    .setMarked(name)
                    .setEditorEnabled(true);
            v.setAdapter(a.getAdapter());
            v.setFocusable(false);
            d.setView(v);
            d.show();
            DialogUtils.wrapLeakageSafe(d, null);
            return d::dismiss;
        }
        throw new IllegalArgumentException(MSG_ILL_PARENT);
    }
}
