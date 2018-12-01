package green_green_avk.anotherterm.utils;

public final class IntCaster implements Caster {
    @Override
    public Object cast(Object v) {
        if (v instanceof String) return Integer.parseInt((String) v);
        if (v instanceof Boolean) return (Boolean) v ? 1 : 0;
        if (v instanceof Integer) return v;
        if (v instanceof Long) return (int) (long) v;
        if (v instanceof Float) return (int) (float) v;
        if (v instanceof Double) return (int) (double) v;
        return null;
    }
}
