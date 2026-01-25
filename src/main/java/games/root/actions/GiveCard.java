package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root.RootGameState;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class GiveCard extends AbstractAction {
    public final int playerID;
    public final int targetID;
    public final int cardIdx, cardId;

    public GiveCard(int playerID, int targetID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.targetID = targetID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID){
            PartialObservableDeck<RootCard> playerHand = currentState.getPlayerHand(playerID);
            PartialObservableDeck<RootCard> targetHand = currentState.getPlayerHand(targetID);
            boolean[] visibility = new boolean[currentState.getNPlayers()];
            visibility[playerID] = true;
            visibility[targetID] = true;
            RootCard card = playerHand.pick(cardIdx);
            targetHand.add(card, visibility);
            return true;
        }
        return false;
    }

    @Override
    public GiveCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GiveCard giveCard = (GiveCard) o;
        return playerID == giveCard.playerID && targetID == giveCard.targetID && cardIdx == giveCard.cardIdx && cardId == giveCard.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, targetID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " gives p" + targetID + " card " + cardIdx;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " gives " + gs.getPlayerFaction(targetID).toString() + " " + card.suit.toString() + " card " + card.cardType.toString();
    }
}
