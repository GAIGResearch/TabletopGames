package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.components.BoardNode;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.*;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.util.Objects;

import static core.CoreConstants.playersHash;
import static games.descent2e.DescentHelper.figureDeath;
import static games.descent2e.actions.Move.*;

public class ForcedMove extends DescentAction {

    final Vector2D whereTo;
    protected int maxDistance;
    final Monster.Direction orientation;
    int figureID;
    int sourceID;
    int directionID;
    Vector2D startPosition = new Vector2D(0, 0);

    public ForcedMove(int target, int source, Vector2D startPosition, Vector2D whereTo, int maxDistance) {
        super(Triggers.MOVE_INTO_SPACE);
        this.startPosition = startPosition;
        this.whereTo = whereTo;
        this.orientation = Monster.Direction.DOWN;
        this.directionID = -1;
        this.figureID = target;
        this.sourceID = source;
        this.maxDistance = maxDistance;
    }

    public ForcedMove(int target, int source, Vector2D startPosition, Vector2D whereTo, Monster.Direction orientation, int maxDistance) {
        super(Triggers.MOVE_INTO_SPACE);
        this.startPosition = startPosition;
        this.whereTo = whereTo;
        this.orientation = orientation;
        this.directionID = -1;
        this.figureID = target;
        this.sourceID = source;
        this.maxDistance = maxDistance;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(this.figureID);

        // We are just straight up picking up the figure off the board and placing it in its new position
        remove(dgs, f);
        forcedMove(dgs, f, whereTo, orientation);

        f.setHasMoved(true);
        return true;
    }

    protected static void forcedMove(DescentGameState dgs, Figure f, Vector2D position, Monster.Direction orientation) {
        // More or less copied from Move clas's place() function

        // Update location and orientation. Swap size if orientation is horizontal (relevant for medium monsters)
        f.setPosition(position.copy());
        Vector2D topLeftAnchor = position.copy();
        if (f instanceof Monster) {
            ((Monster) f).setOrientation(orientation);
            topLeftAnchor = ((Monster) f).applyAnchorModifier();
        }
        Pair<Integer, Integer> size = f.getSize().copy();
        if (orientation.ordinal() % 2 == 1) size.swap();

        // Place figure on all spaces occupied. Save the terrain with minimum ordinal (big monsters only take this penalty)
        int minTerrainOrdinal = DescentTypes.TerrainType.values().length;
        for (int i = 0; i < size.b; i++) {
            for (int j = 0; j < size.a; j++) {
                BoardNode destinationTile = dgs.getMasterBoard().getElement(topLeftAnchor.getX() + j, topLeftAnchor.getY() + i);
                PropertyInt placeFigureOnTile = new PropertyInt("players", f.getComponentID());
                destinationTile.setProperty(placeFigureOnTile);

                DescentTypes.TerrainType terrain = Utils.searchEnum(DescentTypes.TerrainType.class, destinationTile.getComponentName());
                if (terrain != null) {
                    if (terrain.ordinal() < minTerrainOrdinal) {
                        minTerrainOrdinal = terrain.ordinal();
                    }
                    if (terrain == DescentTypes.TerrainType.Pit) {
                        f.setAttributeToMin(Figure.Attribute.MovePoints);
                        f.getAttribute(Figure.Attribute.Health).decrement(2);

                        if (f.getAttribute(Figure.Attribute.Health).isMinimum()) {
                            figureDeath(dgs, f);
                        }
                    }
                }
            }
        }
    }

    @Override
    public ForcedMove copy() {
        ForcedMove retval = new ForcedMove(figureID, sourceID, startPosition, whereTo, orientation, maxDistance);
        retval.directionID = directionID;
        return retval;
    }

    @Override
    public String getString(AbstractGameState gameState) {

        Figure f = (Figure) gameState.getComponentById(figureID);
        Figure source = (Figure) gameState.getComponentById(sourceID);

        String name = f.getName().replace("Hero: ", "");
        String sourceName = source.getName().replace("Hero: ", "");

        String movement = "Forced Move: " + sourceName + " forces " + name + " to move from " + startPosition.toString() + " to " + whereTo.toString();

        movement = movement + (f.getSize().a > 1 || f.getSize().b > 1 ? "; Orientation: " + orientation : "");
        return movement;
    }

    @Override
    public String toString() {
        return "Forced Move by " + sourceID + " upon " + figureID + " from" + startPosition.toString() + " to " + whereTo.toString();
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        if (dgs.getComponentById(figureID) == null) return false;
        if (dgs.getComponentById(sourceID) == null) return false;
        BoardNode tile = dgs.getMasterBoard().getElement(whereTo);
        if (tile == null) return false;
        // Can only force move a figure onto an empty space
        if (((PropertyInt) tile.getProperty(playersHash)).value != -1) return false;

        Figure f = (Figure) dgs.getComponentById(figureID);

        // Monsters with the Unmovable passive (Sir Alric Farrow) cannot be forced to move
        if (f instanceof Monster)
            if (((Monster) f).hasPassive(MonsterAbilities.MonsterPassive.UNMOVABLE)) return false;

        return f.getAttributeValue(Figure.Attribute.MovePoints) >= maxDistance;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ForcedMove forcedMove = (ForcedMove) o;
        return figureID == forcedMove.figureID &&
                sourceID == forcedMove.sourceID &&
                maxDistance == forcedMove.maxDistance &&
                directionID == forcedMove.directionID &&
                Objects.equals(whereTo, forcedMove.whereTo) &&
                orientation == forcedMove.orientation &&
                Objects.equals(startPosition, forcedMove.startPosition);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(figureID, sourceID, whereTo, maxDistance, orientation, startPosition, directionID);
    }
}
