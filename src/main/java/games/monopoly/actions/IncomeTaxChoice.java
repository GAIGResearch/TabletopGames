package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.OneShotExtendedAction;
import games.monopoly.MonopolySquare;

import java.util.List;

/**
 * The player who has landed on Income Tax chooses how to pay it: the flat tax of the square (PayFlatTax), or
 * MonopolyParameters.incomeTaxPercent of their total worth (PayPercentTax).
 */
public class IncomeTaxChoice extends OneShotExtendedAction {

    public final MonopolySquare square; // in equals and hashCode through the name ("Pay " + square)

    public IncomeTaxChoice(int player, MonopolySquare square) {
        super("Pay " + square.name(), player, state -> List.of(new PayFlatTax(square), new PayPercentTax()));
        this.square = square;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof PayFlatTax || action instanceof PayPercentTax)
            executed = true;
    }

    @Override
    public IncomeTaxChoice copy() {
        IncomeTaxChoice copy = new IncomeTaxChoice(player, square);
        copy.executed = executed;
        return copy;
    }
}
