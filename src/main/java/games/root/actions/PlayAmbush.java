package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root.RootGameState;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class PlayAmbush extends AbstractAction {
    public final int playerID;
    public final int cardIdx, cardId;

    public PlayAmbush(int playerID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID){
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            RootCard card = hand.pick(cardIdx);
            currentState.getDiscardPile().add(card);
            return true;
        }
        return false;
    }

    @Override
    public PlayAmbush copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayAmbush that = (PlayAmbush) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID  + " plays ambush";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " plays ambush";
    }
}
