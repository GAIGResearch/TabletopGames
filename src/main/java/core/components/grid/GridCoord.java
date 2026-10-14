package core.components.grid;

import java.util.*;

/**
 * A position on a grid of hexes or squares. Directions are numbered from 0 (east) clockwise as drawn on screen, with
 * y increasing downwards, so turning by k steps is (d + k) % nDirections().
 * <p>
 * Positions are ordered row by row: by second(), then first().
 */
public interface GridCoord<C extends GridCoord<C>> extends Comparable<C> {

    int nDirections();

    /**
     * The position as a pair of integers, (q, r) for a hex and (x, y) for a square: with second(), it places the
     * position in an array.
     */
    int first();

    int second();

    @Override
    default int compareTo(C o) {
        return second() != o.second() ? Integer.compare(second(), o.second()) : Integer.compare(first(), o.first());
    }

    /**
     * The adjacent position in the direction.
     */
    C neighbour(int direction);

    /**
     * The number of steps between the positions, moving only between adjacent ones.
     */
    int distance(C other);

    /**
     * The adjacent positions, in direction order.
     */
    default List<C> neighbours() {
        List<C> retValue = new ArrayList<>(nDirections());
        for (int d = 0; d < nDirections(); d++)
            retValue.add(neighbour(d));
        return retValue;
    }

    default int opposite(int direction) {
        return (direction + nDirections() / 2) % nDirections();
    }

    /**
     * The positions split into groups connected by adjacency.
     */
    static <C extends GridCoord<C>> List<Set<C>> connectedGroups(Collection<C> positions) {
        Set<C> all = new HashSet<>(positions);
        List<Set<C>> retValue = new ArrayList<>();
        Set<C> seen = new HashSet<>();
        for (C start : all) {
            if (!seen.add(start))
                continue;
            Set<C> group = new HashSet<>();
            Deque<C> open = new ArrayDeque<>(List.of(start));
            while (!open.isEmpty()) {
                C c = open.pop();
                group.add(c);
                for (C n : c.neighbours())
                    if (all.contains(n) && seen.add(n))
                        open.push(n);
            }
            retValue.add(group);
        }
        return retValue;
    }
}
