package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root.RootGameState;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class CraftCard extends AbstractAction {

    public final int playerID;
    public final int cardIdx, cardId;

    public CraftCard(int playerID, int cardIdx, int cardId) {
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID) {
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            RootCard card = hand.pick(cardIdx);
            currentState.getPlayerCraftedCards(playerID).add(card);
            return true;
        }
        return false;
    }

    @Override
    public CraftCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CraftCard craftCard = (CraftCard) o;
        return playerID == craftCard.playerID && cardIdx == craftCard.cardIdx && cardId == craftCard.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " crafts card " + cardIdx;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gameState.getComponentById(cardId);
        return gs.getPlayerFaction(playerID) + " crafts " + card.cardType.toString();
    }
}
