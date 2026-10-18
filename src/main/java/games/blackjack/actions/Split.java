package games.blackjack.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.blackjack.BlackjackGameState;

/**
 * Split a pair: the two cards of the hand being played start two hands, the new one with a bet equal to the first,
 * and each is dealt a second card.
 * The new hand is inserted straight after the hand being played (index activeHand + 1) and takes the pair's second
 * card (the one dealt second: the top of the hand's Deck, index 0); the hand being played keeps the first. Each hand
 * is then dealt one card from the draw deck, the hand being played first, and play continues on it (activeHand
 * unchanged) - except split Aces, which are both finished at once.
 */
public class Split extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        BlackjackGameState state = (BlackjackGameState) gs;
        int player = state.getCurrentPlayer();
        int hand = state.getActiveHand();
        state.splitHand(player, hand);
        state.getPlayerHand(player, hand).add(state.drawCard());
        state.getPlayerHand(player, hand + 1).add(state.drawCard());
        return true;
    }

    @Override
    public Split copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Split;
    }

    @Override
    public int hashCode() {
        return 730273;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Split";
    }
}
