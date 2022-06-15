package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
import core.properties.PropertyBoolean;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Move extends AbstractAction {
    final List<Vector2D> positionsTraveled;
    final Monster.Direction orientation;

    public Move(List<Vector2D> whereTo) {
        this.positionsTraveled = whereTo;
        this.orientation = Monster.Direction.DOWN;
    }
    public Move(List<Vector2D> whereTo, Monster.Direction finalOrientation) {
        this.positionsTraveled = whereTo;
        this.orientation = finalOrientation;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DescentGameState dgs = (DescentGameState) gs;
        Figure f = ((DescentGameState) gs).getActingFigure();

        // Remove from old position
        remove(dgs, f);

        // Go through all positions traveled as part of this movement, except for final one, applying all costs and penalties
        for (int i = 0; i < positionsTraveled.size(); i++) {
            // Place only at final position with new orientation
            boolean place = i == positionsTraveled.size()-1;
            moveThrough(dgs, f, positionsTraveled.get(i), place, orientation);
        }


        return true;
    }


    /**
     * Moves through a tile, applying penalties, NOT final destination.
     * Big monsters don't need to be able to fully occupy all spaces travelled.
     * Orientation doesn't matter. When moving through tiles, all figures squish to 1x1, only expanding (if bigger size)
     *   in final destination.
     * @param dgs - game state
     * @param f - figure to apply penalties to
     * @param position - tile to go through
     */
    private static void moveThrough(DescentGameState dgs, Figure f, Vector2D position, boolean place, Monster.Direction orientation) {
        BoardNode destinationTile = dgs.getMasterBoard().getElement(position.getX(), position.getY());
        DescentTypes.TerrainType terrain = Utils.searchEnum(DescentTypes.TerrainType.class, destinationTile.getComponentName());
        if (terrain != null) {
            for (Map.Entry<Figure.Attribute, Integer> e : terrain.getMoveCosts().entrySet()) {
                f.incrementAttribute(e.getKey(), -e.getValue());
            }
        }

        if (place) {
            // TODO: the following code needs to be complemented by 3 things before commented back in:
            // - Move action calculations in FM allow big monsters to move as if 1x1, unless that completes their movement action
            //     -> BUT new position needs to allow figure to eventually reach a fully legal position (through future move actions) within its current movement points
            // - If position is temporary only AND if there are no legal positions to fully expand in current position, then force player to take another move action and block all other options
            // - If position is temporary only and another action is chosen, fully expand the figure and place properly on the board first, before doing the other action

            // Don't fully place large monsters, unless they're out of movement points or they touched a pit space
//            if (f instanceof Monster && (f.getSize().a > 1 || f.getSize().b > 1) &&
//                    f.getAttributeValue(Figure.Attribute.MovePoints) > 0 && terrain != DescentTypes.TerrainType.Pit) {
//                // Still got movement points, only place in this space temporarily
//                f.setPosition(position);
//                PropertyInt placeFigureOnTile = new PropertyInt("players", f.getComponentID());
//                destinationTile.setProperty(placeFigureOnTile);
//                PropertyBoolean tempPlacement = new PropertyBoolean("tempPlacement", true);
//                destinationTile.setProperty(tempPlacement);
//            }
            place(dgs, f, position, orientation);
        }
    }

    /**
     * Removes figure from its old position. For big monsters, all spaces previously occupied are cleared.
     * @param dgs - game state
     * @param f - figure to remove
     */
    private static void remove(DescentGameState dgs, Figure f) {
        Vector2D oldTopLeftAnchor = f.getPosition().copy();
        PropertyInt emptySpace = new PropertyInt("players", -1);

        BoardNode baseSpace = dgs.getMasterBoard().getElement(oldTopLeftAnchor.getX(), oldTopLeftAnchor.getY());
        PropertyBoolean temporary = (PropertyBoolean) baseSpace.getProperty("tempPlacement");
        if (temporary != null && temporary.value) {
            // Only remove figure from this tile
            baseSpace.setProperty(emptySpace);
            if (baseSpace.getComponentName().equalsIgnoreCase("pit")) {
                f.setAttributeToMin(Figure.Attribute.MovePoints);
            }
            temporary.value = false;
        } else {
            // Full remove figure of all spaces (including adjacent) that it occupies
            if (f instanceof Monster) {
                oldTopLeftAnchor = ((Monster) f).applyAnchorModifier();
            }
            Monster.Direction oldOrientation = Monster.Direction.DOWN;
            if (f instanceof Monster) {
                oldOrientation = ((Monster) f).getOrientation();
            }
            Pair<Integer, Integer> sizeOld = f.getSize().copy();
            if (oldOrientation.ordinal() % 2 == 1) sizeOld.swap();
            for (int i = 0; i < sizeOld.b; i++) {
                for (int j = 0; j < sizeOld.a; j++) {
                    BoardNode currentTile = dgs.getMasterBoard().getElement(oldTopLeftAnchor.getX() + j, oldTopLeftAnchor.getY() + i);
                    currentTile.setProperty(emptySpace);
                    if (currentTile.getComponentName().equalsIgnoreCase("pit")) {
                        f.setAttributeToMin(Figure.Attribute.MovePoints);
                    }
                }
            }
        }
    }

    /**
     * Moves to final destination space, where all spaces need to be occupied correctly.
     * @param dgs - game state
     * @param f - figure to place
     * @param position - final position for figure
     * @param orientation - final orientation for figure (possibly new)
     */
    private static void place(DescentGameState dgs, Figure f, Vector2D position, Monster.Direction orientation) {
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
        DescentTypes.TerrainType minTerrain = null;
        for (int i = 0; i < size.b; i++) {
            for (int j = 0; j < size.a; j++) {
                BoardNode destinationTile = dgs.getMasterBoard().getElement(topLeftAnchor.getX() + j, topLeftAnchor.getY() + i);
                PropertyInt placeFigureOnTile = new PropertyInt("players", f.getComponentID());
                destinationTile.setProperty(placeFigureOnTile);

                DescentTypes.TerrainType terrain = Utils.searchEnum(DescentTypes.TerrainType.class, destinationTile.getComponentName());
                if (terrain != null) {
                    if (terrain.ordinal() < minTerrainOrdinal) {
                        minTerrainOrdinal = terrain.ordinal();
                        minTerrain = terrain;
                    }
                    if (terrain == DescentTypes.TerrainType.Pit) f.setAttributeToMin(Figure.Attribute.MovePoints);
                }
            }
        }

        // Apply move costs and penalties
        // Large monsters pay the minimum cost only, other figures are 1 tile wide, looking at min terrain only
        if (minTerrain != null) {
            for (Map.Entry<Figure.Attribute, Integer> e : minTerrain.getMoveCosts().entrySet()) {
                f.incrementAttribute(e.getKey(), -e.getValue());
            }
        }
    }

    @Override
    public AbstractAction copy() {
        List<Vector2D> posTraveledCopy = new ArrayList<>();
        for (Vector2D pos: positionsTraveled) {
            posTraveledCopy.add(pos.copy());
        }
        return new Move(posTraveledCopy, orientation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move move = (Move) o;
        return orientation == move.orientation && Objects.equals(positionsTraveled, move.positionsTraveled);
    }

    @Override
    public int hashCode() {
        return Objects.hash(positionsTraveled, orientation);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = ((DescentGameState) gameState).getActingFigure();
        return "Move to " + positionsTraveled.toString() + (f.getSize().a > 1 || f.getSize().b > 1 ? " orientation:" + orientation : "");
    }

    public List<Vector2D> getPositionsTraveled() {
        return positionsTraveled;
    }
}
