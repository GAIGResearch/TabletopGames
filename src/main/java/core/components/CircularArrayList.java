package core.components;

import java.util.*;

/**
 * A List backed by a circular array. Indexed access is O(1), as in an ArrayList, but adding or removing at either
 * end is also O(1), as in a LinkedList (or an ArrayDeque, which is not a List).
 * Deck keeps the top of the deck at index 0, so needs both.
 */
final class CircularArrayList<E> extends AbstractList<E> implements RandomAccess {

    private Object[] elements;
    private int head;  // position in elements of index 0
    private int size;

    CircularArrayList() {
        this(16);
    }

    CircularArrayList(int initialCapacity) {
        elements = new Object[Math.max(4, initialCapacity)];
    }

    CircularArrayList(Collection<? extends E> c) {
        elements = new Object[Math.max(16, c.size())];
        for (E e : c)
            elements[size++] = e;
    }

    // position in elements of the given index
    private int position(int index) {
        int p = head + index;
        return p < elements.length ? p : p - elements.length;
    }

    // copies n elements from index from to index to, as System.arraycopy does (the ranges may overlap)
    private void move(int from, int to, int n) {
        if (n <= 0) return;
        int pf = position(from), pt = position(to), cap = elements.length;
        if (pf + n <= cap && pt + n <= cap) {
            System.arraycopy(elements, pf, elements, pt, n);
        } else if (to < from) {
            for (int i = 0; i < n; i++)
                elements[position(to + i)] = elements[position(from + i)];
        } else {
            for (int i = n - 1; i >= 0; i--)
                elements[position(to + i)] = elements[position(from + i)];
        }
    }

    // copies n elements from index from into dest, starting at destPos
    private void copyOut(int from, Object[] dest, int destPos, int n) {
        if (n <= 0) return;
        int p = position(from);
        int first = Math.min(n, elements.length - p);
        System.arraycopy(elements, p, dest, destPos, first);
        System.arraycopy(elements, 0, dest, destPos + first, n - first);
    }

    @Override
    @SuppressWarnings("unchecked")
    public E get(int index) {
        Objects.checkIndex(index, size);
        return (E) elements[position(index)];
    }

    @Override
    public E set(int index, E element) {
        E old = get(index);
        elements[position(index)] = element;
        return old;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void add(int index, E element) {
        Objects.checkIndex(index, size + 1);
        if (size == elements.length)
            grow(size + 1);
        if (index < size / 2) {
            // the elements before index move one place towards the front
            head = head == 0 ? elements.length - 1 : head - 1;
            move(1, 0, index);
        } else {
            // the elements from index move one place towards the back
            move(index, index + 1, size - index);
        }
        elements[position(index)] = element;
        size++;
        modCount++;
    }

    @Override
    public E remove(int index) {
        E old = get(index);
        if (index < size / 2) {
            // the elements before index move one place towards the back
            move(0, 1, index);
            elements[head] = null;
            head = position(1);
        } else {
            // the elements after index move one place towards the front
            move(index + 1, index, size - 1 - index);
            elements[position(size - 1)] = null;
        }
        size--;
        modCount++;
        return old;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        Objects.checkIndex(index, size + 1);
        Object[] added = c.toArray();
        int k = added.length;
        if (k == 0) return false;
        Object[] all = new Object[Math.max(elements.length, Math.max(size + k, 2 * size))];
        copyOut(0, all, 0, index);
        System.arraycopy(added, 0, all, index, k);
        copyOut(index, all, index + k, size - index);
        elements = all;
        head = 0;
        size += k;
        modCount++;
        return true;
    }

    @Override
    public int indexOf(Object o) {
        int first = Math.min(size, elements.length - head);
        for (int i = 0; i < first; i++)
            if (Objects.equals(o, elements[head + i])) return i;
        for (int i = first; i < size; i++)
            if (Objects.equals(o, elements[i - first])) return i;
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = size - 1; i >= 0; i--)
            if (Objects.equals(o, elements[position(i)])) return i;
        return -1;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public Object[] toArray() {
        Object[] retValue = new Object[size];
        copyOut(0, retValue, 0, size);
        return retValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            a = (T[]) Arrays.copyOf(a, size, a.getClass());
        copyOut(0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            int next = 0, last = -1, expectedModCount = modCount;

            @Override
            public boolean hasNext() {
                return next < size;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                if (next >= size) throw new NoSuchElementException();
                last = next;
                return (E) elements[position(next++)];
            }

            @Override
            public void remove() {
                if (last < 0) throw new IllegalStateException();
                if (modCount != expectedModCount) throw new ConcurrentModificationException();
                CircularArrayList.this.remove(last);
                next = last;
                last = -1;
                expectedModCount = modCount;
            }
        };
    }

    @Override
    public void clear() {
        int first = Math.min(size, elements.length - head);
        Arrays.fill(elements, head, head + first, null);
        Arrays.fill(elements, 0, size - first, null);
        head = 0;
        size = 0;
        modCount++;
    }

    private void grow(int minCapacity) {
        Object[] bigger = new Object[Math.max(minCapacity, elements.length * 2)];
        copyOut(0, bigger, 0, size);
        elements = bigger;
        head = 0;
    }
}
