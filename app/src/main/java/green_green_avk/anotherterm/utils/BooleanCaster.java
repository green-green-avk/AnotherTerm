package green_green_avk.anotherterm.utils;

public final class BooleanCaster implements Caster {
    public static Object CAST(Object v) {
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        if (v instanceof Boolean) return v;
        if (v instanceof Integer) return (int) v != 0;
        if (v instanceof Long) return (long) v != 0;
        if (v instanceof Float) return (float) v != 0;
        if (v instanceof Double) return (Double) v != 0;
        return false;
    }

    @Override
    public Object cast(Object v) {
        return CAST(v);
    }
}
