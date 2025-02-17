package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class DiscardBirdCard extends AbstractAction {
    public final int playerID;
    public final int cardIdx, cardId;

    public DiscardBirdCard(int playerID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat){
            RootCard card = currentState.getPlayerHand(playerID).pick(cardIdx);
            currentState.getDiscardPile().add(card);
            currentState.decreaseActionsPlayed();
            return true;
        }
        return false;
    }

    @Override
    public DiscardBirdCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscardBirdCard that = (DiscardBirdCard) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " spends bird card " + cardIdx + " to get an additional action";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " spends bird card " + card.toString() + " to get an additional action";
    }
}
