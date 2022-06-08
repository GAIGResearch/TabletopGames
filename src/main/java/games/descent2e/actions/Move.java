package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.BoardNode;
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
    final int orientation;

    public Move(List<Vector2D> whereTo) {
        this.positionsTraveled = whereTo;
        this.orientation = 0;
    }
    public Move(List<Vector2D> whereTo, int finalOrientation) {
        this.positionsTraveled = whereTo;
        this.orientation = finalOrientation;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DescentGameState dgs = (DescentGameState) gs;
        Figure f = ((DescentGameState) gs).getActingFigure();

        // Go through all positions traveled as part of this movement, applying all costs and penalties
        for (Vector2D pos: positionsTraveled) {
            moveTo(dgs, f, pos.copy());
        }

        return true;
    }

    private void moveTo(DescentGameState dgs, Figure f, Vector2D position) {
        // Update location and orientation
        Vector2D oldLocation = f.getPosition().copy();
        f.setPosition(position.copy());
        int oldOrientation = 0;
        if (f instanceof Monster) {
            oldOrientation = ((Monster) f).getOrientation();
            ((Monster) f).setOrientation(orientation);
        }

        int w = 1, h = 1;
        int oldW = 1, oldH = 1;
        if (f.getSize() != null) {
            Pair<Integer, Integer> size = f.getSize().copy();
            Pair<Integer, Integer> sizeOld = f.getSize().copy();
            if (orientation % 2 == 1) size.swap();
            if (oldOrientation % 2 == 1) sizeOld.swap();
            w = size.a;
            h = size.b;
            oldW = sizeOld.a;
            oldH = sizeOld.b;
        }
        int minTerrainOrdinal = 100;
        DescentTypes.TerrainType minTerrain = null;
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                BoardNode destinationTile = dgs.getMasterBoard().getElement(position.getX() + j, position.getY() + i);
                PropertyInt placeFigureOnTile = new PropertyInt("players", f.getComponentID());
                destinationTile.setProperty(placeFigureOnTile);

                DescentTypes.TerrainType terrain = Utils.searchEnum(DescentTypes.TerrainType.class, destinationTile.getComponentName());
                if (terrain != null) {
                    if (terrain.ordinal() < minTerrainOrdinal) {
                        minTerrainOrdinal = terrain.ordinal();
                        minTerrain = terrain;
                    }
                }
            }
        }
        for (int i = 0; i < oldH; i++) {
            for (int j = 0; j < oldW; j++) {
                BoardNode currentTile = dgs.getMasterBoard().getElement(oldLocation.getX() + j, oldLocation.getY() + i);
                PropertyInt emptyTile = new PropertyInt("players", -1);
                currentTile.setProperty(emptyTile);
                if (currentTile.getComponentName().equalsIgnoreCase("pit")) {
                    f.setAttributeToMin(Figure.Attribute.MovePoints);
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
        return "Move to " + positionsTraveled.toString();
    }

    public List<Vector2D> getPositionsTraveled() {
        return positionsTraveled;
    }

    public int getOrientation() {
        return orientation;
    }
}
