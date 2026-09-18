package games.crazyeights;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.interfaces.IExtendedSequence;
import games.crazyeights.actions.NominateSuit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Phase D: before normal play, if the starter card is an Eight and CZEParameters.dealerNominatesStarterSuit is true,
 * the dealer (the last player, so that player 0 on the dealer's left still plays first) nominates the suit to match.
 * Put on the action stack by CZEForwardModel._setup; the dealer is offered one NominateSuit action per suit, and the
 * sequence is complete once a suit has been nominated.
 * <p>
 * AbstractGameState compares and hashes the action stack, so this class must be equal by value.
 */
public class CZEStarterSuitNomination implements IExtendedSequence {

    boolean nominated;

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return state.getNPlayers() - 1;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        int dealer = getCurrentPlayer(state);
        List<AbstractAction> actions = new ArrayList<>();
        for (FrenchCard.Suite suit : FrenchCard.Suite.values())
            actions.add(new NominateSuit(dealer, suit));
        return actions;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof NominateSuit)
            nominated = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return nominated;
    }

    @Override
    public CZEStarterSuitNomination copy() {
        CZEStarterSuitNomination copy = new CZEStarterSuitNomination();
        copy.nominated = nominated;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CZEStarterSuitNomination that && nominated == that.nominated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nominated, "CZEStarterSuitNomination");
    }
}
