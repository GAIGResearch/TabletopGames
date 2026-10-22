package core.components;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * CircularArrayList must behave exactly like an ArrayList; it is checked against one.
 */
public class CircularArrayListTest {

    @Test
    public void isRandomAccess() {
        assertTrue(new CircularArrayList<>() instanceof RandomAccess);
    }

    @Test
    public void addingAtTheFrontKeepsOrder() {
        List<Integer> list = new CircularArrayList<>();
        for (int i = 0; i < 40; i++) list.add(0, i);  // wraps round the backing array, and grows it
        List<Integer> expected = new ArrayList<>();
        for (int i = 39; i >= 0; i--) expected.add(i);
        assertEquals(expected, list);
        assertEquals(expected.hashCode(), list.hashCode());
    }

    @Test
    public void removingFromBothEnds() {
        List<Integer> list = new CircularArrayList<>(List.of(1, 2, 3, 4, 5));
        assertEquals(Integer.valueOf(1), list.remove(0));
        assertEquals(Integer.valueOf(5), list.remove(list.size() - 1));
        assertEquals(List.of(2, 3, 4), list);
        list.clear();
        assertTrue(list.isEmpty());
        list.add(7);
        assertEquals(List.of(7), list);
    }

    @Test
    public void indexOutOfBoundsIsReported() {
        List<Integer> list = new CircularArrayList<>(List.of(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
    }

    @Test
    public void behavesLikeAnArrayListUnderRandomOperations() {
        Random rnd = new Random(7);
        for (int trial = 0; trial < 50; trial++) {
            List<Integer> expected = new ArrayList<>();
            List<Integer> actual = new CircularArrayList<>();
            for (int op = 0; op < 500; op++) {
                int size = expected.size();
                switch (rnd.nextInt(8)) {
                    case 0, 1 -> {  // add at the front (as Deck.add() does)
                        expected.add(0, op);
                        actual.add(0, op);
                    }
                    case 2 -> {
                        expected.add(op);
                        actual.add(op);
                    }
                    case 3 -> {
                        int i = rnd.nextInt(size + 1);
                        expected.add(i, op);
                        actual.add(i, op);
                    }
                    case 4, 5 -> {
                        if (size > 0) {
                            int i = rnd.nextBoolean() ? 0 : rnd.nextInt(size);
                            assertEquals(expected.remove(i), actual.remove(i));
                        }
                    }
                    case 6 -> {
                        if (size > 0) {
                            int i = rnd.nextInt(size);
                            assertEquals(expected.set(i, -op), actual.set(i, -op));
                        }
                    }
                    case 7 -> {
                        if (size > 1) {
                            long seed = rnd.nextLong();
                            Collections.shuffle(expected, new Random(seed));
                            Collections.shuffle(actual, new Random(seed));
                        }
                    }
                }
                assertEquals(expected, actual);
            }
            // subList views work too (Deck.shuffle(from, to) uses them)
            if (expected.size() > 4) {
                expected.subList(1, 3).clear();
                actual.subList(1, 3).clear();
                assertEquals(expected, actual);
            }
        }
    }
}
