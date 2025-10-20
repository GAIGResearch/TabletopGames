package games.descent2e.actions;

import core.AbstractGameState;
import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyInt;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import utilities.Pair;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static core.CoreConstants.playersHash;

public class Place extends DescentAction{

    int figureId;
    Vector2D position;
    String tile;
    Monster.Direction orientation;

    public Place(int figureId, Vector2D position, String tile) {
        super(Triggers.SETUP);
        this.figureId = figureId;
        this.position = position;
        this.tile = tile;
        this.orientation = null;
    }

    public Place(int figureId, Vector2D position, String tile, Monster.Direction orientation) {
        super(Triggers.SETUP);
        this.figureId = figureId;
        this.position = position;
        this.tile = tile;
        this.orientation = orientation;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureId);

        Vector2D position = this.position;
        f.setPosition(position);
        f.setOffMap(false);

        int h = 1;
        int w = 1;

        if (f instanceof Monster m) {
            if (orientation == null)
                orientation = Monster.Direction.DOWN;
            m.setOrientation(orientation);
            Pair<Integer, Integer> size = m.getSize();
            if (size.a > 1 || size.b > 1) {
                if (orientation.ordinal() % 2 == 0) { // UP or DOWN
                    w = size.a;
                    h = size.b;
                } else { // LEFT or RIGHT
                    w = size.b;
                    h = size.a;
                }
                position = m.applyAnchorModifier(position.copy(), orientation);
            }
        }
        PropertyInt prop = new PropertyInt("players", f.getComponentID());
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                dgs.getMasterBoard().getElement(position.getX() + j, position.getY() + i).setProperty(prop);
            }
        }
        return true;
    }

    @Override
    public DescentAction copy() {
        return new Place(figureId, position, tile, orientation);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(figureId);
        String string = "Place " + f.getName().replace("Hero: ", "") + " at " + position.toString();
        if (orientation != null) {
            string += " " + orientation;
        }
        return string;
    }

    @Override
    public String toString() {
        if (orientation == null) {
            return "Place " + figureId + " at " + position.toString();
        }
        return "Place " + figureId + " at " + position.toString() + " " + orientation;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(figureId);
        if (f == null) return false;
        if (!f.equals(dgs.getActingFigure())) return false;
        if (!f.isOffMap()) return false;
        if (f.getPosition() != null) return false;

        GridBoard board = dgs.getMasterBoard();
        BoardNode node = board.getElement(position);
        if (node == null) return false;

        List<Vector2D> possible = new ArrayList<>(dgs.getGridReferences().get(tile).keySet());
        if (!possible.contains(position)) return false;

        if (f instanceof Monster m) {
            Pair<Integer, Integer> size = m.getSize();
            if (size.a > 1 || size.b > 1) {
                Monster.Direction dir = orientation != null ? orientation : Monster.Direction.DOWN;
                Vector2D anchor = m.applyAnchorModifier(position.copy(), dir);
                int w, h;
                if (dir.ordinal() % 2 == 0) { // UP or DOWN
                    w = size.a;
                    h = size.b;
                } else { // LEFT or RIGHT
                    w = size.b;
                    h = size.a;
                }
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        Vector2D pos = new Vector2D(anchor.getX() + j, anchor.getY() + i);
                        if (!possible.contains(pos)) return false;
                        node = board.getElement(pos);
                        if (node == null) return false;
                        if (((PropertyInt) node.getProperty(playersHash)).value != -1) return false;
                        if (!DescentTypes.TerrainType.isStartingTerrain(node.getComponentName())) return false;
                    }
                }
                return true;
            }
        }

        if (((PropertyInt) node.getProperty(playersHash)).value != -1) return false;
        return DescentTypes.TerrainType.isStartingTerrain(node.getComponentName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(figureId, position, tile, orientation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Place other)) return false;
        return this.figureId == other.figureId && this.position.equals(other.position) &&
                Objects.equals(tile, other.tile) && Objects.equals(this.orientation, other.orientation);
    }

    public int getX() {
        return position.getX();
    }

    public int getY() {
        return position.getY();
    }
}
