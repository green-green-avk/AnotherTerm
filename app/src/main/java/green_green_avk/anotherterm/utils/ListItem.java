package green_green_avk.anotherterm.utils;

public class ListItem {
    public String key;
    public Object value;

    public ListItem(String key, Object value) {
        this.key = key.intern();
        this.value = value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ListItem) return ((ListItem) obj).key.equals(key);
        return key.equals(obj);
    }
}
