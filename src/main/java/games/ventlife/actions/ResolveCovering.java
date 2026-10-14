package games.ventlife.actions;

import core.AbstractGameState;
import evaluation.metrics.Event;
import core.actions.AbstractAction;
import core.components.grid.HexCoord;
import core.interfaces.IExtendedSequence;
import games.ventlife.VentlifeGameState;
import games.ventlife.components.Creature;

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
    public record Covered(Creature creature, HexCoord from, int fromLevel) {
    }

    private final List<Covered> pending;
    // the id of the tile that covered them, which none may move onto
    private final int newTile;

    public ResolveCovering(List<Covered> pending, int newTile) {
        this.pending = new ArrayList<>(pending);
        this.newTile = newTile;
    }

    /**
     * Resolves the covered creatures that need no decision, up to the first that does.
     *
     * @return true if a decision is waiting
     */
    public boolean resolveAutomatic(VentlifeGameState state) {
        while (!pending.isEmpty()) {
            Covered c = pending.get(0);
            List<HexCoord> destinations = destinations(state, c);
            if (destinations.size() > 1)
                return true;
            if (destinations.isEmpty()) {
                state.changeSupply(c.creature.owner(), c.creature.species(), c.creature.count());
                state.logEvent(Event.GameEvent.GAME_EVENT, () -> "Player " + c.creature.owner() + "'s "
                        + c.creature.species() + " covered at " + c.from + " has nowhere to go: returned to supply");
            } else {
                state.setCreature(destinations.get(0), c.creature);
                state.logEvent(Event.GameEvent.GAME_EVENT, () -> "Player " + c.creature.owner() + "'s "
                        + c.creature.species() + " covered at " + c.from + " moves to " + destinations.get(0)
                        + " (its only legal hex)");
            }
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

    private List<HexCoord> destinations(VentlifeGameState state, Covered c) {
        return c.creature.species().destinations(state, c.from, c.fromLevel, newTile);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        VentlifeGameState state = (VentlifeGameState) gs;
        Covered c = pending.get(0);
        List<AbstractAction> actions = new ArrayList<>();
        for (HexCoord to : destinations(state, c))
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
        return new ResolveCovering(pending, newTile);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ResolveCovering other && other.newTile == newTile && other.pending.equals(pending);
    }

    @Override
    public int hashCode() {
        return pending.hashCode() * 31 + newTile + 771307;
    }

    @Override
    public String toString() {
        return "Resolve covered creatures " + pending;
    }
}
