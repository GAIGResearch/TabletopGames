package utilities;

import java.util.Objects;

public class Group<T,V,K> {
    public T a;
    public V b;
    public K c;

    public Group(T a, V b, K c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group)) return false;
        Group<?, ?, ?> group = (Group<?, ?, ?>) o;
        return Objects.equals(a, group.a) &&
                Objects.equals(b, group.b) &&
                Objects.equals(c, group.c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c);
    }
}
