package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class VectorCompat {
    private VectorCompat() {
    }

    // ===> Copied from
    // {@link androidx.appcompat.app.AppCompatViewInflater#checkOnClickListener(View, AttributeSet)}
    public static final int[] onClick = new int[]{android.R.attr.onClick};

    /**
     * An implementation of OnClickListener that attempts to lazily load a
     * named click handling method from a parent or ancestor context.
     */
    private static final class DeclaredOnClickListener implements View.OnClickListener {
        private final View mHostView;
        private final String mMethodName;

        private Method mResolvedMethod;
        private Context mResolvedContext;

        public DeclaredOnClickListener(@NonNull final View hostView,
                                       @NonNull final String methodName) {
            mHostView = hostView;
            mMethodName = methodName;
        }

        @Override
        public void onClick(@NonNull final View v) {
            if (mResolvedMethod == null) {
                resolveMethod(mHostView.getContext());
            }
            try {
                mResolvedMethod.invoke(mResolvedContext, v);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(
                        "Could not execute non-public method for android:onClick", e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(
                        "Could not execute method for android:onClick", e);
            }
        }

        /**
         * A bit different from {@link UiUtils#getActivity(Context)}
         * just to preserve original semantics.
         */
        private void resolveMethod(@Nullable Context context) {
            while (context != null) {
                try {
                    if (!context.isRestricted()) {
                        mResolvedMethod = context.getClass().getMethod(mMethodName,
                                View.class);
                        mResolvedContext = context;
                        return;
                    }
                } catch (final NoSuchMethodException ignored) {
                }
                context = context instanceof ContextWrapper ?
                        ((ContextWrapper) context).getBaseContext() : null;
            }
            final int id = mHostView.getId();
            final String idText = id == View.NO_ID ? "" : " with id '"
                    + mHostView.getContext().getResources().getResourceEntryName(id) + "'";
            throw new IllegalStateException("Could not find method " + mMethodName
                    + "(View) in a parent or ancestor Context for android:onClick "
                    + "attribute defined on view " + mHostView.getClass() + idText);
        }
    }

    /**
     * Useful for prolonging life of deprecated {@code android:onCLick}-like event handler
     * assignments. (Or some compat stuff.)
     *
     * @param listenerAttr event attribute
     * @param view         target view
     * @param attrs        its attrs
     * @return wrapped event handler or {@code null}
     */
    @CheckResult
    @Nullable
    public static View.OnClickListener wrapListener(@Size(1) @NonNull final int[] listenerAttr,
                                                    @NonNull final View view,
                                                    @Nullable final AttributeSet attrs) {
        final TypedArray a =
                view.getContext().obtainStyledAttributes(attrs, listenerAttr);
        final String handlerName = a.getString(0);
        a.recycle();
        return handlerName != null ?
                new DeclaredOnClickListener(view, handlerName) : null;
    }
    // ===>

    @CheckResult
    @Nullable
    public static View.OnClickListener fixOnClickListener(@NonNull final View view,
                                                          @Nullable final AttributeSet attrs,
                                                          @Nullable final View.OnClickListener def) {
        if (!(view.getContext() instanceof ContextWrapper) ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return def;
        final View.OnClickListener r = wrapListener(onClick, view, attrs);
        return r != null ? r : def;
    }
}
