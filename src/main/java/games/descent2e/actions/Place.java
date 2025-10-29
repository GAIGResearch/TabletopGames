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

                // Arbitrarily big numbers for initialisation, need to be positive so no -1 or null
                int remaining = 1000;
                int totalSize = 1000;
                for (List<Monster> monsters : dgs.getMonsters()) {
                    if (monsters.contains(m)) {
                        remaining = monsters.size() - monsters.indexOf(m) - 1;
                        totalSize = monsters.size() * w * h;
                        break;
                    }
                }
                // If this is the last monster in a group to be placed, we have no further need to check any deeper
                // Else, have to make sure that we're not denying any future monster placements by plodding ourselves here
                if (remaining < 1) return true;
                else {
                    // Technically speaking, there is no guarantee that this will protect us;
                    // there may be some ridiculously shaped tile that prevents us from placing anything
                    // but this stops even further ridiculous computation,
                    // e.g. Barghests checking every single rotation only to be told "True" for everything
                    // This is just a sensible-looking limit here, might change later
                    // Barghests and other 2x2 have total size of 8 (21), and Dragons have 12 (31)
                    // All the awkward-shaped tiles are either large enough to allow anyway, or have < 20 positions
                    if (possible.size() > (totalSize * 2.5) + 1) return true;

                    List<Vector2D> futurePossible = new ArrayList<>(List.copyOf(possible));
                    for (int i = 0; i < h; i++) {
                        for (int j = 0; j < w; j++) {
                            futurePossible.remove(new Vector2D(anchor.getX() + i, anchor.getY() + j));
                        }
                    }

                    return checkFuture(board, m, futurePossible, remaining);
                }
            }
        }

        if (((PropertyInt) node.getProperty(playersHash)).value != -1) return false;
        return DescentTypes.TerrainType.isStartingTerrain(node.getComponentName());
    }

    boolean checkFuture(GridBoard board, Monster m, List<Vector2D> possible, int remaining) {
        int canPlace = 0;
        Pair<Integer, Integer> size = m.getSize();
        int w = size.a;
        int h = size.b;
        boolean checkRotation = w != h;

        for (Vector2D anchor : possible) {
            // Immediately return true at the earliest legal convenience
            if (canPlace >= remaining) return true;

            List<Vector2D> available = new ArrayList<>();
            for (int i = 0; i < h; i++) {
                for (int j = 0; j < w; j++) {
                    Vector2D pos = new Vector2D(anchor.getX() + j, anchor.getY() + i);
                    if (!possible.contains(pos)) break;
                    BoardNode node = board.getElement(pos);
                    if (node == null) break;
                    if (((PropertyInt) node.getProperty(playersHash)).value != -1) break;
                    if (!DescentTypes.TerrainType.isStartingTerrain(node.getComponentName())) break;
                    available.add(pos);
                }
            }
            if (available.size() == w * h) {
                List<Vector2D> futurePossible = new ArrayList<>(List.copyOf(possible));
                futurePossible.removeAll(available);
                if (checkFuture(board, m, futurePossible, remaining - 1))
                    canPlace++;
            }
            // Do it again but vertically, if we must
            if (checkRotation) {
                if (canPlace >= remaining) return true;
                available.clear();
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        Vector2D pos = new Vector2D(anchor.getX() + j, anchor.getY() + i);
                        if (!possible.contains(pos)) break;
                        BoardNode node = board.getElement(pos);
                        if (node == null) break;
                        if (((PropertyInt) node.getProperty(playersHash)).value != -1) break;
                        if (!DescentTypes.TerrainType.isStartingTerrain(node.getComponentName())) break;
                        available.add(pos);
                    }
                }
                if (available.size() == w * h) {
                    List<Vector2D> futurePossible = new ArrayList<>(List.copyOf(possible));
                    futurePossible.removeAll(available);
                    if (checkFuture(board, m, futurePossible, remaining - 1))
                        canPlace++;
                }
            }
        }
        return canPlace >= remaining;
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
