package green_green_avk.anotherterm.utils;

public final class FloatCaster implements Caster {
    @Override
    public Object cast(Object v) {
        if (v instanceof String) return Float.parseFloat((String) v);
        if (v instanceof Boolean) return (Boolean) v ? 1 : 0;
        if (v instanceof Integer) return (float) (int) v;
        if (v instanceof Long) return (float) (long) v;
        if (v instanceof Float) return v;
        if (v instanceof Double) return (float) (double) v;
        return null;
    }
}
