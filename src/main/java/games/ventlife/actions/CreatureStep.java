package games.ventlife.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.ventlife.VentlifeGameState;
import games.ventlife.components.Hex;
import games.ventlife.components.Species;

import java.util.*;

/**
 * The creature step of a turn: the player places creatures of one species. Most species place once; after a Volcano
 * Snail the player may place more Snails, each next to a Snail placed this turn, up to
 * VentlifeParameters.maxSnailsPerTurn, or stop.
 */
public class CreatureStep implements IExtendedSequence {

    private static final Comparator<Hex> HEX_ORDER = Comparator.comparingInt(Hex::q).thenComparingInt(Hex::r);

    private final int player;
    // whether a placement has been made this step
    private boolean placed;
    // the Volcano Snails placed this step, in order
    private final List<Hex> snails;
    private boolean stopped;

    public CreatureStep(int player) {
        this(player, false, List.of(), false);
    }

    private CreatureStep(int player, boolean placed, List<Hex> snails, boolean stopped) {
        this.player = player;
        this.placed = placed;
        this.snails = new ArrayList<>(snails);
        this.stopped = stopped;
    }

    /**
     * Whether the player has any legal placement, i.e. whether the step happens at all (it is compulsory).
     */
    public boolean hasPlacement(VentlifeGameState state) {
        return !firstPlacements(state).isEmpty();
    }

    /**
     * Every legal placement of one species' creatures, over the species in play with tokens left.
     */
    private List<AbstractAction> firstPlacements(VentlifeGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        List<Hex> hexes = new ArrayList<>(state.getField().keySet());
        hexes.sort(HEX_ORDER);
        for (Species s : state.getSpeciesInPlay()) {
            int supply = state.getSupply(player, s);
            if (supply == 0)
                continue;
            for (Hex h : hexes)
                if (s.canPlace(state, player, h)) {
                    int max = Math.min(supply, s.maxPlaced(state, h));
                    for (int n = 1; n <= max; n++)
                        actions.add(new PlaceCreature(s, h, n));
                }
        }
        return actions;
    }

    /**
     * The empty Basalt hexes next to a Snail placed this step.
     */
    private List<Hex> moreSnailHexes(VentlifeGameState state) {
        Set<Hex> retValue = new TreeSet<>(HEX_ORDER);
        for (Hex p : snails)
            for (Hex n : p.neighbours())
                if (Species.VOLCANO_SNAIL.canPlace(state, player, n))
                    retValue.add(n);
        return new ArrayList<>(retValue);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        VentlifeGameState state = (VentlifeGameState) gs;
        if (!placed)
            return firstPlacements(state);
        List<AbstractAction> actions = new ArrayList<>();
        for (Hex h : moreSnailHexes(state))
            actions.add(new PlaceCreature(Species.VOLCANO_SNAIL, h, 1));
        actions.add(new StopPlacing());
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof StopPlacing)
            stopped = true;
        else if (action instanceof PlaceCreature pc) {
            placed = true;
            if (pc.species == Species.VOLCANO_SNAIL)
                snails.add(pc.hex);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState gs) {
        if (!placed)
            return false;
        if (snails.isEmpty())
            return true;
        // after a Snail: until the player stops, or no more may or can be placed
        VentlifeGameState state = (VentlifeGameState) gs;
        return stopped || snails.size() >= state.getParams().maxSnailsPerTurn
                || state.getSupply(player, Species.VOLCANO_SNAIL) == 0 || moreSnailHexes(state).isEmpty();
    }

    @Override
    public CreatureStep copy() {
        return new CreatureStep(player, placed, snails, stopped);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CreatureStep other && other.player == player && other.placed == placed
                && other.stopped == stopped && other.snails.equals(snails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, placed, snails, stopped) + 771311;
    }

    @Override
    public String toString() {
        return "Creature step of player " + player + (snails.isEmpty() ? "" : ", Snails at " + snails);
    }
}
