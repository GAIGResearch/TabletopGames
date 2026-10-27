package games.crazyeights;

import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import core.components.FrenchCard;
import games.crazyeights.actions.NominateSuit;

import java.util.ArrayList;
import java.util.List;

/**
 * Before normal play, if the starter card is an Eight and CZEParameters.dealerNominatesStarterSuit is true,
 * the dealer (the last player, so that player 0 on the dealer's left still plays first) nominates the suit to match.
 * Put on the action stack by CZEForwardModel._setup; the dealer is offered one NominateSuit action per suit.
 */
public class CZEStarterSuitNomination extends OneShotExtendedAction {

    public CZEStarterSuitNomination(int dealer) {
        super("CZEStarterSuitNomination", dealer, state -> {
            List<AbstractAction> actions = new ArrayList<>();
            for (FrenchCard.Suite suit : FrenchCard.Suite.values())
                actions.add(new NominateSuit(suit));
            return actions;
        });
    }

    @Override
    public CZEStarterSuitNomination copy() {
        CZEStarterSuitNomination copy = new CZEStarterSuitNomination(player);
        copy.executed = executed;
        return copy;
    }
}
