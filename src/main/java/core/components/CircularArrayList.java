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
            grow();
        if (index < size / 2) {
            // move the elements before index one place towards the front
            head = head == 0 ? elements.length - 1 : head - 1;
            for (int i = 0; i < index; i++)
                elements[position(i)] = elements[position(i + 1)];
        } else {
            // move the elements from index one place towards the back
            for (int i = size; i > index; i--)
                elements[position(i)] = elements[position(i - 1)];
        }
        elements[position(index)] = element;
        size++;
        modCount++;
    }

    @Override
    public E remove(int index) {
        E old = get(index);
        if (index < size / 2) {
            // move the elements before index one place towards the back
            for (int i = index; i > 0; i--)
                elements[position(i)] = elements[position(i - 1)];
            elements[head] = null;
            head = position(1);
        } else {
            // move the elements after index one place towards the front
            for (int i = index; i < size - 1; i++)
                elements[position(i)] = elements[position(i + 1)];
            elements[position(size - 1)] = null;
        }
        size--;
        modCount++;
        return old;
    }

    @Override
    public void clear() {
        Arrays.fill(elements, null);
        head = 0;
        size = 0;
        modCount++;
    }

    private void grow() {
        Object[] bigger = new Object[elements.length * 2];
        for (int i = 0; i < size; i++)
            bigger[i] = elements[position(i)];
        elements = bigger;
        head = 0;
    }
}
