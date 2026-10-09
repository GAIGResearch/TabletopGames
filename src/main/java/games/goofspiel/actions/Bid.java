package games.goofspiel.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.goofspiel.GoofspielGameState;

import java.util.Objects;

/**
 * A player bids a card from their hand, placing it face down.
 */
public class Bid extends AbstractAction {
    // All players bid at once, so the action carries the bidder. Inside a SimultaneousAction the turn owner is
    // whoever held the turn when it was applied.
    public final int player;
    public final FrenchCard card;

    public Bid(int player, FrenchCard card) {
        this.player = player;
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        GoofspielGameState state = (GoofspielGameState) gs;
        state.getHand(player).remove(card);
        state.getBid(player).add(card);
        return true;
    }

    @Override
    public Bid copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bid other && other.player == player && other.card.equals(card);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, card) + 740231;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Bid " + cardName();
    }

    /**
     * The card as players say it, e.g. "7 of Clubs" or "Queen of Spades".
     */
    private String cardName() {
        String rank = card.type == FrenchCard.FrenchCardType.Number ? String.valueOf(card.number) : card.type.name();
        return rank + " of " + card.suite;
    }

    @Override
    public String toString() {
        return "P" + player + " bids " + cardName();
    }
}
