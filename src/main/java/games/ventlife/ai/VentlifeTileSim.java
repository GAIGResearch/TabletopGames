package games.ventlife.ai;

import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeUtils;
import games.ventlife.actions.PlaceTile;
import games.ventlife.components.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies a tile placement to a copy of the state, as PlaceTile does, but without the forward model's bookkeeping
 * (listing the creature placements that follow, the phases and the sequence of covering decisions). The field,
 * creatures and supplies come out as they are when the forward model stops for the first covering decision (or
 * for the creature step): covered creatures with a single destination have moved there, those with none are back in
 * supply, and those from the first that needs a choice on are off the field. So the scores and heuristic values of
 * the result are those of the state the forward model would reach, for a fraction of the cost.
 */
public class VentlifeTileSim {

    private VentlifeTileSim() {
    }

    private record Covered(Creature creature, Hex from, int fromLevel) {
    }

    public static VentlifeGameState afterTile(VentlifeGameState state, PlaceTile t) {
        VentlifeGameState s = (VentlifeGameState) state.copy();
        int placer = s.getCurrentPlayer(), n = s.getNPlayers();
        s.getHand(placer).remove(t.tile);
        int level = s.getLevel(t.smoker) + 1;
        int tileId = s.takeNextTileId();
        List<Hex> hexes = VentlifeUtils.tileHexes(t.smoker, t.orientation);
        // the placer's creatures first, then the other players' clockwise; one player's in tile order
        List<Covered> covered = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            int owner = (placer + k) % n;
            for (Hex h : hexes) {
                Creature c = s.getCreature(h);
                if (c == null || c.owner() != owner) continue;
                if (h.equals(t.smoker) && c.species() == Species.TUBE_WORM) continue;
                covered.add(new Covered(s.removeCreature(h), h, level - 1));
            }
        }
        s.setCell(hexes.get(0), new HexCell(Terrain.BLACK_SMOKER, level, tileId));
        s.setCell(hexes.get(1), new HexCell(t.tile.left, level, tileId));
        s.setCell(hexes.get(2), new HexCell(t.tile.right, level, tileId));
        for (Covered c : covered) {
            List<Hex> destinations = c.creature.species().destinations(s, c.from, c.fromLevel, tileId);
            if (destinations.size() > 1)
                break;
            if (destinations.isEmpty())
                s.changeSupply(c.creature.owner(), c.creature.species(), c.creature.count());
            else
                s.setCreature(destinations.get(0), c.creature);
        }
        return s;
    }
}
