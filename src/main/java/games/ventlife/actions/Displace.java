package games.ventlife.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.ventlife.VentlifeGameState;
import games.ventlife.components.Creature;
import games.ventlife.components.Hex;

import java.util.Objects;

/**
 * The owner of a creature covered by a tile moves it from the covered hex to a new hex.
 */
public class Displace extends AbstractAction {

    public final Creature creature;
    public final Hex from;
    public final Hex to;

    public Displace(Creature creature, Hex from, Hex to) {
        this.creature = creature;
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VentlifeGameState state = (VentlifeGameState) gs;
        if (!state.isEmptyHex(to))
            throw new IllegalStateException(to + " is not an empty hex");
        state.setCreature(to, creature);
        return true;
    }

    @Override
    public Displace copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Displace other && other.creature.equals(creature) && other.from.equals(from)
                && other.to.equals(to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creature, from, to) + 771305;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Move " + creature.species() + " covered at " + from + " to " + to;
    }
}
