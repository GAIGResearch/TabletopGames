package games.explodingkittens.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.explodingkittens.*;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.Objects;

public class GiveCard extends AbstractAction {

    public int giver;
    public int recipient;
    public ExplodingKittensCard.CardType cardType;

    public GiveCard(int giver, int recipient, ExplodingKittensCard.CardType cardType) {
        this.giver = giver;
        this.cardType = cardType;
        this.recipient = recipient;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ExplodingKittensGameState state = (ExplodingKittensGameState) gs;
        ExplodingKittensCard card = state.getPlayerHand(giver).stream()
                .filter(c -> c.cardType == cardType).findFirst()
                .orElseThrow(() -> new AssertionError("Card not found : " + cardType));
        state.getPlayerHand(giver).remove(card);
        state.getPlayerHand(recipient).add(card);
        return true;
    }

    @Override
    public GiveCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GiveCard gc) {
            return gc.giver == giver && gc.recipient == recipient && gc.cardType == cardType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(giver, recipient, cardType.ordinal());
    }

    @Override
    public String toString() {
        return "Give card " + cardType + " from player " + giver + " to player " + recipient;
    }

    @Override
    public String getString(AbstractGameState gameState, int perspectiveID) {
        if (perspectiveID == giver || perspectiveID == recipient) return toString();
        return getString(gameState);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Player " + giver + " gives a card to player " + recipient;
    }
}
