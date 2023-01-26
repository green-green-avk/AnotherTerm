package green_green_avk.anotherterm.ui.dragndrop;

import android.os.Build;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;

import me.saket.bettermovementmethod.BetterLinkMovementMethod;

public final class DragLinkMovementMethod {
    private static MovementMethod instance = null;

    @NonNull
    public static MovementMethod getInstance() {
        if (instance == null)
            instance = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN ?
                    new LegacyLinkMovementMethod() : new FixedLinkMovementMethod();
        return instance;
    }

    private DragLinkMovementMethod() {
    }

    private static final class LegacyLinkMovementMethod extends LinkMovementMethod {
        private final DragMovementMethodDelegate delegate = new DragMovementMethodDelegate();

        @Override
        public boolean onTouchEvent(final TextView widget, final Spannable buffer,
                                    final MotionEvent event) {
            return delegate.onTouchEvent(widget, buffer, event) ||
                    super.onTouchEvent(widget, buffer, event);
        }
    }

    private static final class FixedLinkMovementMethod extends BetterLinkMovementMethod {
        private final DragMovementMethodDelegate delegate = new DragMovementMethodDelegate();

        @Override
        public boolean onTouchEvent(final TextView widget, final Spannable buffer,
                                    final MotionEvent event) {
            return delegate.onTouchEvent(widget, buffer, event) ||
                    super.onTouchEvent(widget, buffer, event);
        }
    }
}
