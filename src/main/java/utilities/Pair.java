package utilities;

public class Pair<T,V> {
    public T a;
    public V b;

    public Pair(T a, V b) {
        this.a = a;
        this.b = b;
    }

    public void swap() {
        T c = a;
        a = (T) b;
        b = (V) c;
    }

    public Pair<V, V> copy() {
        return new Pair(a, b);
    }
}
