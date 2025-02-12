package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.root.RootGameState;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class TakeFromDiscard extends AbstractAction {
    public final int playerID;
    public final int cardIdx, cardId;

    public TakeFromDiscard(int playerID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if (state.getCurrentPlayer() == playerID){
            PartialObservableDeck<RootCard> hand = state.getPlayerHand(playerID);
            Deck<RootCard> discard = state.getDiscardPile();
            boolean[] visibility = new boolean[state.getNPlayers()];
            // Fill the array with true values
            for (int i = 0; i < state.getNPlayers(); i++) {
                visibility[i] = true;
            }
            RootCard card = discard.pick(cardIdx);
            hand.add(card, visibility);
            return true;
        }
        return false;
    }

    @Override
    public TakeFromDiscard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TakeFromDiscard that = (TakeFromDiscard) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " takes card " + cardIdx + " from discard";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " takes " + card.suit.toString() + " card " + card.cardType.toString() + " from the discard pile";
    }
}
