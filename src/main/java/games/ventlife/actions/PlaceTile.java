package games.ventlife.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeUtils;
import games.ventlife.components.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The current player places the tile from their hand with its Black Smoker at smoker, turned to orientation (see
 * VentlifeUtils.tileHexes), on the seafloor or on top of the field.
 */
public class PlaceTile extends AbstractAction {

    public final VentTile tile;
    public final Hex smoker;
    public final int orientation;

    public PlaceTile(VentTile tile, Hex smoker, int orientation) {
        this.tile = tile;
        this.smoker = smoker;
        this.orientation = orientation;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VentlifeGameState state = (VentlifeGameState) gs;
        Deck<VentTile> hand = state.getHand(state.getCurrentPlayer());
        // a mismatch means the state changed between computing the action and executing it
        if (!hand.getComponents().contains(tile))
            throw new IllegalStateException("Player " + state.getCurrentPlayer() + " does not hold " + tile);
        hand.remove(tile);
        // the tile is one level above the hexes beneath it, which the placement rules make all the same level
        int level = state.getLevel(smoker) + 1;
        int tileId = state.takeNextTileId();
        List<Hex> hexes = VentlifeUtils.tileHexes(smoker, orientation);
        List<ResolveCovering.Covered> covered = new ArrayList<>();
        for (Hex h : coveredCreatures(state, hexes)) {
            // Tube Worms on the Smoker this tile extends never leave it: they climb with the stack
            if (h.equals(smoker) && state.getCreature(h).species() == Species.TUBE_WORM)
                continue;
            covered.add(new ResolveCovering.Covered(state.removeCreature(h), h, level - 1));
        }
        state.setCell(hexes.get(0), new HexCell(Terrain.BLACK_SMOKER, level, tileId));
        state.setCell(hexes.get(1), new HexCell(tile.left, level, tileId));
        state.setCell(hexes.get(2), new HexCell(tile.right, level, tileId));
        ResolveCovering resolution = new ResolveCovering(covered);
        if (resolution.resolveAutomatic(state))
            state.setActionInProgress(resolution);
        return true;
    }

    /**
     * The tile's positions that hold creatures, in the order they are resolved: the placing player's first, then the
     * other players clockwise; one player's in tile order (Smoker, left, right).
     */
    private static List<Hex> coveredCreatures(VentlifeGameState state, List<Hex> hexes) {
        int placer = state.getCurrentPlayer(), n = state.getNPlayers();
        List<Hex> retValue = new ArrayList<>();
        for (int k = 0; k < n; k++)
            for (Hex h : hexes) {
                Creature c = state.getCreature(h);
                if (c != null && c.owner() == (placer + k) % n)
                    retValue.add(h);
            }
        return retValue;
    }

    @Override
    public PlaceTile copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlaceTile other && other.orientation == orientation && other.smoker.equals(smoker)
                && other.tile.equals(tile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tile, smoker, orientation) + 771301;
    }

    // the tile is public once placed
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Place " + tile + " with Smoker at " + smoker + ", orientation " + orientation;
    }
}
