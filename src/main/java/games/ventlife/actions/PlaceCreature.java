package games.ventlife.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.ventlife.VentlifeGameState;
import games.ventlife.components.Creature;
import games.ventlife.components.Hex;
import games.ventlife.components.Species;

import java.util.Objects;

/**
 * The current player places count creatures of the species from their supply on the hex. count is more than 1 only
 * for Tube Worms taking the Low-Vent Bonus.
 */
public class PlaceCreature extends AbstractAction {

    public final Species species;
    public final Hex hex;
    public final int count;

    public PlaceCreature(Species species, Hex hex, int count) {
        this.species = species;
        this.hex = hex;
        this.count = count;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VentlifeGameState state = (VentlifeGameState) gs;
        int player = state.getCurrentPlayer();
        if (state.getSupply(player, species) < count)
            throw new IllegalStateException("Player " + player + " has fewer than " + count + " " + species);
        state.changeSupply(player, species, -count);
        state.setCreature(hex, new Creature(species, player, count));
        return true;
    }

    @Override
    public PlaceCreature copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlaceCreature other && other.species == species && other.count == count
                && other.hex.equals(hex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(species, hex, count) + 771303;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Place " + (count > 1 ? count + " " : "") + species + " at " + hex;
    }
}
