package games.conquest.components;

import core.CoreConstants;
import core.components.Component;
import games.conquest.CQGameState;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>Components represent a game piece, or encompass some unit of game information (e.g. cards, tokens, score counters, boards, dice etc.)</p>
 * <p>Components in the game can (and should, if applicable) extend one of the other components, in package {@link core.components}.
 * Or, the game may simply reuse one of the existing core components.</p>
 * <p>They need to extend at a minimum the {@link Component} super class and implement the {@link Component#copy()} method.</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>may</b> keep references to other components or actions (but these should be deep-copied in the copy() method, watch out for infinite loops!).</p>
 */
public class Cell extends Component {
    public final Vector2D position;

    public Cell(int x, int y) {
        super(CoreConstants.ComponentType.AREA, "Cell");
        position = new Vector2D(x, y);
    }

    protected Cell(int componentID, Vector2D position) {
        super(CoreConstants.ComponentType.AREA, "Cell", componentID);
        this.position = position;
    }

    public int getChebyshev(Cell to) {
        return getChebyshev(to.position);
    }
    public int getChebyshev(Vector2D to) {
        return Math.max(Math.abs(position.getX() - to.getX()), Math.abs(position.getY() - to.getY()));
    }
    public List<Cell> getNeighbors(Cell[][] board) {
        List<Cell> neighbors = new ArrayList<>();
        for (int i=0;i<9;i++) {
            if (i == 4) continue; // in a 3x3 grid, index 4 is a null move
            int x = i%3 + position.getX() - 1; // cycle through x 3 times
            int y = i/3 + position.getY() - 1; // cycle through y once every 3 steps
            if (x >= 0 && y >= 0 && x < board.length && y < board[0].length) {
                neighbors.add(board[x][y]);
            }
        }
        return neighbors;
    }

    /**
     * Check if this cell is walkable; can be false if either an obstacle, or occupied.
     * @param cqgs Game state, to check if there is a troop there
     * @return true if walkable, false otherwise.
     */
    public boolean isWalkable(CQGameState cqgs) {
        return cqgs.getTroopByLocation(this) == null;
    }

    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTComponent) and NOT the super class Component.
     * <p>
     * <b>IMPORTANT</b>: This should have the same componentID
     * (using the protected constructor on the Component super class which takes this as an argument).
     * </p>
     * <p>The function should also call the {@link Component#copyComponentTo(Component)} method, passing in as an
     * argument the new copy you've made.</p>
     * <p>If all variables in this class are final or effectively final, then you can just return <code>`this`</code>.</p>
     */
    @Override
    public Cell copy() {
        return this; // Immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cell)) return false;
        Cell cell = (Cell) o;
        return position.equals(cell.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position);
    }

    @Override
    public String toString() {
        return "Cell[" + position.getX() + "," + position.getY() + "]";
    }
}
