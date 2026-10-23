package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolySquare;

import java.util.ArrayList;
import java.util.List;

/**
 * The player who has landed on an unowned property decides whether to buy it: BuyProperty (only with the price in
 * cash) or DeclineProperty.
 */
public class BuyDecision extends OneShotExtendedAction {

    public final MonopolySquare square; // in equals and hashCode through the name ("Buy or decline " + square)

    public BuyDecision(int player, MonopolySquare square) {
        super("Buy or decline " + square.name(), player, state -> {
            List<AbstractAction> actions = new ArrayList<>();
            if (((MonopolyGameState) state).getCash(player) >= square.price())
                actions.add(new BuyProperty(square));
            actions.add(new DeclineProperty(square));
            return actions;
        });
        this.square = square;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        // only this decision completes it, not the roll that pushed it
        if (action instanceof BuyProperty || action instanceof DeclineProperty)
            executed = true;
    }

    @Override
    public BuyDecision copy() {
        BuyDecision copy = new BuyDecision(player, square);
        copy.executed = executed;
        return copy;
    }
}
