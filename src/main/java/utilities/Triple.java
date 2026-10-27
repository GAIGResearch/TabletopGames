package utilities;

import java.util.Objects;

public class Triple<T, U, V> {
    public T one;
    public U two;
    public V three;

    public static <T, U, V> Triple<T, U, V> of(T one, U two, V three) {
        return new Triple<>(one, two, three);
    }
    public Triple(T a, U b, V c) {
        this.one = a;
        this.two = b;
        this.three = c;
    }

    public Triple<T, U, V> copy() {
        return new Triple<>(one, two, three);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Triple)) return false;
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return Objects.equals(one, triple.one) &&
                Objects.equals(two, triple.two) &&
                Objects.equals(three, triple.three);
    }

    @Override
    public int hashCode() {
        return Objects.hash(one, two, three);
    }

    @Override
    public String toString() {
        return "<" + one + ";" + two + ";" + three + ">";
    }
}
