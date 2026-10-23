package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import games.monopoly.MonopolyGameState;

import java.util.ArrayList;
import java.util.List;

/**
 * The player who drew the "pay a fine or take a Chance" card chooses: PayFine (only with the amount in cash) or
 * TakeChance.
 */
public class PayOrChance extends OneShotExtendedAction {

    public final int amount; // in equals and hashCode through the name

    public PayOrChance(int player, int amount) {
        super("Pay " + amount + " or take a Chance", player, state -> {
            List<AbstractAction> actions = new ArrayList<>();
            if (((MonopolyGameState) state).getCash(player) >= amount)
                actions.add(new PayFine(amount));
            actions.add(new TakeChance());
            return actions;
        });
        this.amount = amount;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof PayFine || action instanceof TakeChance)
            executed = true;
    }

    @Override
    public PayOrChance copy() {
        PayOrChance copy = new PayOrChance(player, amount);
        copy.executed = executed;
        return copy;
    }
}
