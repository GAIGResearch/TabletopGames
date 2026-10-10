package games.ventlife.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.ventlife.VentlifeGameState;
import games.ventlife.components.Creature;
import games.ventlife.components.Hex;

import java.util.ArrayList;
import java.util.List;

/**
 * The creatures covered by a tile, still to be moved or returned, in the order they are resolved. The owner of a
 * creature with more than one destination chooses where it goes (a Displace).
 */
public class ResolveCovering implements IExtendedSequence {

    /**
     * A covered creature, the hex it was on, and that hex's level before the tile covered it.
     */
    public record Covered(Creature creature, Hex from, int fromLevel) {
    }

    private final List<Covered> pending;

    public ResolveCovering(List<Covered> pending) {
        this.pending = new ArrayList<>(pending);
    }

    /**
     * Resolves the covered creatures that need no decision, up to the first that does.
     *
     * @return true if a decision is waiting
     */
    public boolean resolveAutomatic(VentlifeGameState state) {
        while (!pending.isEmpty()) {
            Covered c = pending.get(0);
            List<Hex> destinations = destinations(state, c);
            if (destinations.size() > 1)
                return true;
            if (destinations.isEmpty())
                state.changeSupply(c.creature.owner(), c.creature.species(), c.creature.count());
            else
                state.setCreature(destinations.get(0), c.creature);
            pending.remove(0);
        }
        return false;
    }

    /**
     * The covered creature whose owner is to choose where it goes.
     */
    public Covered waiting() {
        return pending.get(0);
    }

    private static List<Hex> destinations(VentlifeGameState state, Covered c) {
        return c.creature.species().destinations(state, c.from, c.fromLevel);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        VentlifeGameState state = (VentlifeGameState) gs;
        Covered c = pending.get(0);
        List<AbstractAction> actions = new ArrayList<>();
        for (Hex to : destinations(state, c))
            actions.add(new Displace(c.creature, c.from, to));
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return pending.get(0).creature.owner();
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof Displace d && !pending.isEmpty() && d.from.equals(pending.get(0).from)
                && d.creature.equals(pending.get(0).creature)) {
            pending.remove(0);
            resolveAutomatic((VentlifeGameState) state);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return pending.isEmpty();
    }

    @Override
    public ResolveCovering copy() {
        return new ResolveCovering(pending);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ResolveCovering other && other.pending.equals(pending);
    }

    @Override
    public int hashCode() {
        return pending.hashCode() + 771307;
    }

    @Override
    public String toString() {
        return "Resolve covered creatures " + pending;
    }
}
