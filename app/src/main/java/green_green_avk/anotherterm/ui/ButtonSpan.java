package green_green_avk.anotherterm.ui;

import android.app.Activity;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ButtonSpan extends ClickableSpan {
    @NonNull
    protected final String mMethodName;

    public ButtonSpan(@NonNull final String methodName) {
        super();
        mMethodName = methodName;
    }

    @Override
    public void onClick(@NonNull final View widget) {
        final Activity ctx = UiUtils.getActivity(widget.getContext());
        if (ctx == null) {
            Log.e(getClass().getName(), "No underlying activity found");
            return;
        }
        try {
            final Method m = ctx.getClass().getMethod(mMethodName, View.class);
            m.invoke(ctx, widget);
        } catch (final NoSuchMethodException | SecurityException | IllegalAccessException e) {
            Log.e(getClass().getName(), "Method invocation error:", e);
        } catch (final InvocationTargetException e) {
            final Throwable t = e.getCause();
            if (t instanceof Error)
                throw (Error) t;
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
        }
    }
}
