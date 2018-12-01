package green_green_avk.anotherterm.utils;

public final class StringCaster implements Caster {
    @Override
    public Object cast(Object v) {
        return v.toString();
    }
}
