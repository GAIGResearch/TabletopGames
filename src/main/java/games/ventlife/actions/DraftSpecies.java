package games.ventlife.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.ventlife.VentlifeGameState;
import games.ventlife.components.Species;

/**
 * The current player drafts a species (the Advanced Variant): at 2 and 4 players a species to be used, at 3 players
 * one not to be used.
 */
public class DraftSpecies extends AbstractAction {

    public final Species species;

    public DraftSpecies(Species species) {
        this.species = species;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VentlifeGameState state = (VentlifeGameState) gs;
        if (state.getDrafted().contains(species))
            throw new IllegalStateException(species + " has already been drafted");
        state.addDrafted(species);
        return true;
    }

    @Override
    public DraftSpecies copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DraftSpecies other && other.species == species;
    }

    @Override
    public int hashCode() {
        return species.ordinal() + 771317;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Draft " + species;
    }
}
