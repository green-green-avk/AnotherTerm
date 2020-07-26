package green_green_avk.anotherterm;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ListView;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public final class TermKeyMapManagerUi {
    private TermKeyMapManagerUi() {
    }

    private static Spanned getWarnTitle(@NonNull final Context ctx, @StringRes final int resId) {
        final SpannableString v = new SpannableString(ctx.getString(resId));
        v.setSpan(new StyleSpan(Typeface.ITALIC), 0, v.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return v;
    }

    @NonNull
    public static Spanned getTitle(@NonNull final Context ctx,
                                   @NonNull final TermKeyMapRules rules) {
        final TermKeyMapManager.Meta m = TermKeyMapManager.getMeta(rules);
        if (m == null)
            return getWarnTitle(ctx, R.string.keymap_title_anonymous);
        final SpannableString v = new SpannableString(m.getTitle(ctx));
        if (m.isBuiltIn)
            v.setSpan(new StyleSpan(Typeface.ITALIC), 0, v.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return v;
    }

    public static void showList(@NonNull final Object parent,
                                @NonNull final TermKeyMapAdapter.OnSelectListener onSelectListener,
                                @NonNull final TermKeyMapRules rules) {
        final TermKeyMapManager.Meta m = TermKeyMapManager.getMeta(rules);
        showList(parent, onSelectListener, m == null ? null : m.name);
    }

    public static void showList(@NonNull final Object parent,
                                @NonNull final TermKeyMapAdapter.OnSelectListener onSelectListener) {
        showList(parent, onSelectListener, (String) null);
    }

    public static void showList(@NonNull final Object parent,
                                @NonNull final TermKeyMapAdapter.OnSelectListener onSelectListener,
                                @Nullable final String name) {
        if (parent instanceof View) {
            final Context ctx = ((View) parent).getContext();
            final PopupWindow d = new PopupWindow(ctx);
            d.setBackgroundDrawable(ctx.getResources().getDrawable(
                    android.R.drawable.dialog_holo_light_frame));
            d.setFocusable(true);
            d.setAnimationStyle(android.R.style.Animation_Dialog);
            final ListView v = new ListView(ctx);
            final TermKeyMapAdapter a = new TermKeyMapAdapter(ctx)
                    .setIncludeBuiltIns(true)
                    .setItemLayoutRes(R.layout.term_key_map_manager_dialog_entry)
                    .setDropDownItemLayoutRes(R.layout.term_key_map_manager_dialog_entry)
                    .setOnSelectListener(new TermKeyMapAdapter.OnSelectListener() {
                        @Override
                        public void onSelect(final boolean isBuiltIn, final String name,
                                             final TermKeyMapRules rules, final String title) {
                            onSelectListener.onSelect(isBuiltIn, name, rules, title);
                            d.dismiss();
                        }
                    })
                    .setMarked(name)
                    .setEditorEnabled(true);
            v.setAdapter(a);
            v.setFocusable(false);
            d.setContentView(v);
            d.showAsDropDown((View) parent);
            return;
        }
        if (parent instanceof Context) {
            final Context ctx = (Context) parent;
            final AlertDialog d = new AlertDialog.Builder(ctx).create();
            final ListView v = new ListView(ctx);
            final TermKeyMapAdapter a = new TermKeyMapAdapter(ctx)
                    .setIncludeBuiltIns(true)
                    .setItemLayoutRes(R.layout.term_key_map_manager_dialog_entry)
                    .setDropDownItemLayoutRes(R.layout.term_key_map_manager_dialog_entry)
                    .setOnSelectListener(new TermKeyMapAdapter.OnSelectListener() {
                        @Override
                        public void onSelect(final boolean isBuiltIn, final String name,
                                             final TermKeyMapRules rules, final String title) {
                            onSelectListener.onSelect(isBuiltIn, name, rules, title);
                            d.dismiss();
                        }
                    })
                    .setMarked(name)
                    .setEditorEnabled(true);
            v.setAdapter(a);
            v.setFocusable(false);
            d.setView(v);
            d.show();
            return;
        }
        throw new IllegalArgumentException("`parent' can be View or Context only");
    }
}
