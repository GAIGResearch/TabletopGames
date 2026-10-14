package core.components.grid;

import core.CoreConstants;
import core.components.Component;
import core.interfaces.IComponentContainer;

import java.util.*;

/**
 * A grid of hexes or squares with no fixed size or shape: a value at each position that has one. Suits a board that
 * grows as tiles or cards are laid, and a fixed board of any shape.
 * <p>
 * The values must be immutable: a copy of the grid shares them, so copying is cheap and still deep. To change a
 * position, set a new value there.
 * <p>
 * A grid made for a number of players is partially observable: each position can be hidden from each player. Every
 * position starts visible to everyone, whether or not it holds a value.
 * <p>
 * The values that are Components are the grid's contained components.
 */
public class SparseGrid<C extends GridCoord<C>, T> extends Component implements IComponentContainer<Component> {

    // the extra room on each side when the grid first takes a position
    private static final int MARGIN = 4;

    // the values and their positions in arrays over the rectangle of (first, second) from (minFirst, minSecond),
    // width by height; null where a position has no value
    private Object[] values = new Object[0], positions = new Object[0];
    private int minFirst, minSecond, width, height;
    private int size;
    // the positions hidden from each player, or null if the grid is fully observable
    private List<Set<C>> hiddenFrom;
    private Map<C, T> view;

    /**
     * A fully observable grid.
     */
    public SparseGrid(String name) {
        super(CoreConstants.ComponentType.BOARD, name);
    }

    /**
     * A partially observable grid.
     */
    public SparseGrid(String name, int nPlayers) {
        this(name);
        hiddenFrom = new ArrayList<>(nPlayers);
        for (int p = 0; p < nPlayers; p++)
            hiddenFrom.add(new HashSet<>());
    }

    // an empty grid with the original's name and id
    private SparseGrid(SparseGrid<C, T> orig) {
        super(CoreConstants.ComponentType.BOARD, orig.componentName, orig.componentID);
    }

    private int index(int first, int second) {
        int a = first - minFirst, b = second - minSecond;
        return a < 0 || b < 0 || a >= width || b >= height ? -1 : b * width + a;
    }

    private int index(C pos) {
        return index(pos.first(), pos.second());
    }

    /**
     * The value at the position, or null if it has none.
     */
    @SuppressWarnings("unchecked")
    public T get(C pos) {
        int i = index(pos);
        return i < 0 ? null : (T) values[i];
    }

    /**
     * Puts the value at the position, or takes away the value there if it is null.
     */
    public void set(C pos, T value) {
        if (value == null) {
            remove(pos);
            return;
        }
        int i = index(pos);
        if (i < 0) {
            growToInclude(pos);
            i = index(pos);
        }
        if (values[i] == null)
            size++;
        values[i] = value;
        positions[i] = pos;
    }

    /**
     * Takes away the value at the position, returning it (null if there was none).
     */
    @SuppressWarnings("unchecked")
    public T remove(C pos) {
        int i = index(pos);
        if (i < 0 || values[i] == null)
            return null;
        T old = (T) values[i];
        values[i] = null;
        positions[i] = null;
        size--;
        return old;
    }

    private void growToInclude(C pos) {
        int newMinFirst, newMinSecond, newMaxFirst, newMaxSecond;
        if (width == 0) {
            newMinFirst = pos.first() - MARGIN;
            newMaxFirst = pos.first() + MARGIN;
            newMinSecond = pos.second() - MARGIN;
            newMaxSecond = pos.second() + MARGIN;
        } else {
            // grow by at least half again on the side that needs it, so that a growing board is moved rarely
            int growFirst = Math.max(MARGIN, width / 2), growSecond = Math.max(MARGIN, height / 2);
            int maxFirst = minFirst + width - 1, maxSecond = minSecond + height - 1;
            newMinFirst = pos.first() < minFirst ? pos.first() - growFirst : minFirst;
            newMaxFirst = pos.first() > maxFirst ? pos.first() + growFirst : maxFirst;
            newMinSecond = pos.second() < minSecond ? pos.second() - growSecond : minSecond;
            newMaxSecond = pos.second() > maxSecond ? pos.second() + growSecond : maxSecond;
        }
        Object[] oldValues = values, oldPositions = positions;
        int oldMinFirst = minFirst, oldMinSecond = minSecond, oldWidth = width;
        minFirst = newMinFirst;
        minSecond = newMinSecond;
        width = newMaxFirst - newMinFirst + 1;
        height = newMaxSecond - newMinSecond + 1;
        values = new Object[width * height];
        positions = new Object[width * height];
        for (int i = 0; i < oldValues.length; i++)
            if (oldValues[i] != null) {
                int j = index(oldMinFirst + i % oldWidth, oldMinSecond + i / oldWidth);
                values[j] = oldValues[i];
                positions[j] = oldPositions[i];
            }
    }

    public boolean contains(C pos) {
        int i = index(pos);
        return i >= 0 && values[i] != null;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * The positions with values, in order.
     */
    public Set<C> positions() {
        return asMap().keySet();
    }

    /**
     * The positions with values, in order.
     */
    @SuppressWarnings("unchecked")
    public List<C> sortedPositions() {
        // the arrays hold the positions row by row, which is their order
        List<C> retValue = new ArrayList<>(size);
        for (Object p : positions)
            if (p != null)
                retValue.add((C) p);
        return retValue;
    }

    /**
     * A read-only view of the values by position, in order of position.
     */
    public Map<C, T> asMap() {
        if (view == null)
            view = new View();
        return view;
    }

    /**
     * Whether the position has a value and is next to one that does not.
     */
    public boolean isOnBorder(C pos) {
        if (!contains(pos))
            return false;
        for (C n : pos.neighbours())
            if (!contains(n))
                return true;
        return false;
    }

    /**
     * Whether the player can see the value at the position. Always true on a fully observable grid.
     */
    public boolean isVisible(C pos, int player) {
        return hiddenFrom == null || !hiddenFrom.get(player).contains(pos);
    }

    public void setVisible(C pos, int player, boolean visible) {
        if (hiddenFrom == null)
            throw new IllegalStateException(componentName + " is fully observable");
        if (visible)
            hiddenFrom.get(player).remove(pos);
        else
            hiddenFrom.get(player).add(pos);
    }

    /**
     * The values that are Components, in no particular order.
     */
    @Override
    public List<Component> getComponents() {
        // called on every copy of a game state, so not sorted
        List<Component> retValue = new ArrayList<>();
        for (Object value : values)
            if (value instanceof Component c)
                retValue.add(c);
        return retValue;
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return hiddenFrom == null ? CoreConstants.VisibilityMode.VISIBLE_TO_ALL : CoreConstants.VisibilityMode.MIXED_VISIBILITY;
    }

    @Override
    public SparseGrid<C, T> copy() {
        SparseGrid<C, T> copy = new SparseGrid<>(this);
        copy.values = values.clone();
        copy.positions = positions.clone();
        copy.minFirst = minFirst;
        copy.minSecond = minSecond;
        copy.width = width;
        copy.height = height;
        copy.size = size;
        if (hiddenFrom != null) {
            copy.hiddenFrom = new ArrayList<>(hiddenFrom.size());
            for (Set<C> hidden : hiddenFrom)
                copy.hiddenFrom.add(new HashSet<>(hidden));
        }
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SparseGrid<?, ?> that)) return false;
        // compared by position, as equal grids may have grown differently
        return componentID == that.componentID && asMap().equals(that.asMap()) && Objects.equals(hiddenFrom, that.hiddenFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentID, asMap(), hiddenFrom);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(componentName).append(":");
        for (C pos : sortedPositions())
            sb.append(" ").append(pos).append("=").append(get(pos));
        return sb.toString();
    }

    // the values by position, read from the arrays
    private final class View extends AbstractMap<C, T> {

        private int indexOf(Object key) {
            if (!(key instanceof GridCoord<?> g))
                return -1;
            int i = index(g.first(), g.second());
            return i >= 0 && key.equals(positions[i]) ? i : -1;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean containsKey(Object key) {
            return indexOf(key) >= 0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T get(Object key) {
            int i = indexOf(key);
            return i < 0 ? null : (T) values[i];
        }

        @Override
        public Set<Entry<C, T>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public int size() {
                    return size;
                }

                @Override
                public Iterator<Entry<C, T>> iterator() {
                    return new Iterator<>() {
                        int next = advance(0);

                        private int advance(int from) {
                            while (from < values.length && values[from] == null)
                                from++;
                            return from;
                        }

                        @Override
                        public boolean hasNext() {
                            return next < values.length;
                        }

                        @Override
                        @SuppressWarnings("unchecked")
                        public Entry<C, T> next() {
                            if (!hasNext())
                                throw new NoSuchElementException();
                            Entry<C, T> e = new SimpleImmutableEntry<>((C) positions[next], (T) values[next]);
                            next = advance(next + 1);
                            return e;
                        }
                    };
                }
            };
        }
    }
}
