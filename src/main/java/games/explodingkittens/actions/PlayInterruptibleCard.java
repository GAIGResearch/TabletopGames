package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.List;

public class PlayInterruptibleCard extends AbstractAction implements IExtendedSequence {

    public final ExplodingKittensCard.CardType cardType;
    public final int cardPlayer;
    boolean hasBeenNoped;
    int currentInterrupter;

    public PlayInterruptibleCard(ExplodingKittensCard.CardType cardType, int playerID) {
        this.cardType = cardType;
        this.cardPlayer = playerID;
        currentInterrupter = -1;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        if (currentInterrupter == -1) {
            currentInterrupter = (state.getCurrentPlayer() + 1) % state.getNPlayers();
        }
        // Now we see if anyone has a Nope card
        while (currentInterrupter != state.getCurrentPlayer()) {
            Deck<ExplodingKittensCard> hand = state.getPlayerHand(currentInterrupter);
            boolean hasNope = hand.stream().anyMatch(c -> c.cardType == ExplodingKittensCard.CardType.NOPE);
            if (hasNope) break;
            currentInterrupter = (currentInterrupter + 1) % state.getNPlayers();
        }
        if (currentInterrupter == cardPlayer) {
            // No one has a Nope card, so we can play the card
            Deck<ExplodingKittensCard> hand = state.getPlayerHand(cardPlayer);
            ExplodingKittensCard card = hand.stream().filter(c -> c.cardType == cardType).findFirst().orElse(null);
            if (card == null)
                throw new AssertionError("Player " + cardPlayer + " does not have a " + cardType + " card to play");
            hand.remove(card);
            state.getDiscardPile().add(card);
        }
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return List.of();
    }

    public void nope() {
        hasBeenNoped = true;
    }
    public boolean isNoped() {
        return hasBeenNoped;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return currentInterrupter;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return currentInterrupter == cardPlayer;
    }

    @Override
    public PlayInterruptibleCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayInterruptibleCard && ((PlayInterruptibleCard) obj).cardType == cardType;
    }

    @Override
    public int hashCode() {
        return cardType.ordinal() + 32804;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Plays " + cardType;
    }
}
