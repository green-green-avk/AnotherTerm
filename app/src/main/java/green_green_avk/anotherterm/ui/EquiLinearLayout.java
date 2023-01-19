package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import java.util.Arrays;

public class EquiLinearLayout extends LinearLayout {

    public EquiLinearLayout(final Context context) {
        super(context);
    }

    public EquiLinearLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public EquiLinearLayout(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (getOrientation() == VERTICAL) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void measureVertical(final int widthMeasureSpec, final int heightMeasureSpec) {
        measureChildrenWithMargins(widthMeasureSpec, heightMeasureSpec);
        final int cc = getChildCount();
        final int hPadding = getPaddingLeft() + getPaddingRight();
        int width = hPadding;
        int height = getPaddingTop() + getPaddingBottom();
        for (int i = 0; i < cc; ++i) {
            final View c = getChildAt(i);
            width = Math.max(c.getMeasuredWidth() + hPadding, width);
            height += c.getMeasuredHeight();
        }
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                if (height > heightSize) shrinkChildrenVertical(heightSize);
                height = heightSize;
                break;
            case MeasureSpec.AT_MOST:
                if (height > heightSize) {
                    shrinkChildrenVertical(heightSize);
                    height = heightSize;
                }
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
        }
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                width = widthSize;
                break;
            case MeasureSpec.AT_MOST:
                if (width > widthSize) width = widthSize;
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
        }
        setMeasuredDimension(width, height);
    }

    private void shrinkChildrenVertical(final int height) {
        final int cc = getChildCount();
        final View[] ca = new View[cc];
        for (int i = 0; i < cc; ++i) {
            ca[i] = getChildAt(i);
        }
        Arrays.sort(ca, (o1, o2) ->
                Integer.compare(o1.getMeasuredHeight(), o2.getMeasuredHeight()));
        int s = Math.max(0, height - getPaddingTop() - getPaddingBottom());
        int n = cc;
        for (final View c : ca) {
            final int m = s / n;
            final int v = c.getMeasuredHeight();
            if (v > m) {
                c.measure(MeasureSpec.makeMeasureSpec(c.getMeasuredWidth(),
                                MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(m,
                                MeasureSpec.EXACTLY));
                s -= m;
            } else {
                s -= v;
            }
            --n;
        }
    }

    private void measureHorizontal(final int widthMeasureSpec, final int heightMeasureSpec) {
        measureChildrenWithMargins(widthMeasureSpec, heightMeasureSpec);
        final int cc = getChildCount();
        final int vPadding = getPaddingTop() + getPaddingBottom();
        int width = getPaddingLeft() + getPaddingRight();
        int height = vPadding;
        for (int i = 0; i < cc; ++i) {
            final View c = getChildAt(i);
            height = Math.max(c.getMeasuredHeight() + vPadding, height);
            width += c.getMeasuredWidth();
        }
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                if (width > widthSize) shrinkChildrenHorizontal(widthSize);
                width = widthSize;
                break;
            case MeasureSpec.AT_MOST:
                if (width > widthSize) {
                    shrinkChildrenHorizontal(widthSize);
                    width = widthSize;
                }
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
        }
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                height = heightSize;
                break;
            case MeasureSpec.AT_MOST:
                if (height > heightSize) height = heightSize;
                break;
            case MeasureSpec.UNSPECIFIED:
            default:
        }
        setMeasuredDimension(width, height);
    }

    private void shrinkChildrenHorizontal(final int width) {
        final int cc = getChildCount();
        final View[] ca = new View[cc];
        for (int i = 0; i < cc; ++i) {
            ca[i] = getChildAt(i);
        }
        Arrays.sort(ca, (o1, o2) ->
                Integer.compare(o1.getMeasuredWidth(), o2.getMeasuredWidth()));
        int s = Math.max(0, width - getPaddingLeft() - getPaddingRight());
        int n = cc;
        for (final View c : ca) {
            final int m = s / n;
            final int v = c.getMeasuredWidth();
            if (v > m) {
                c.measure(MeasureSpec.makeMeasureSpec(m,
                                MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(c.getMeasuredHeight(),
                                MeasureSpec.EXACTLY));
                s -= m;
            } else {
                s -= v;
            }
            --n;
        }
    }

    private void measureChildrenWithMargins(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int cc = getChildCount();
        for (int i = 0; i < cc; ++i) {
            final View c = getChildAt(i);
            if (c.getVisibility() != GONE) {
                measureChildWithMargins(c, widthMeasureSpec, 0, heightMeasureSpec, 0);
            }
        }
    }
}
