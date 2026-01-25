package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root.RootGameState;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class DiscardSupporter extends AbstractAction {
    protected final int playerID;
    protected final int cardIdx, cardId;
    protected final boolean passSubGamePhase;

    public DiscardSupporter(int playerID, int cardIdx, int cardId, boolean passSubGamePhase){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
        this.passSubGamePhase = passSubGamePhase;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(playerID == gs.getCurrentPlayer()) {
            Deck<RootCard> discardPile = currentState.getDiscardPile();
            Deck<RootCard> supporters = currentState.getSupporters();
            RootCard card = supporters.pick(cardIdx);
            discardPile.add(card);
            return true;
        }
        return false;
    }

    @Override
    public DiscardSupporter copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscardSupporter that = (DiscardSupporter) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId && passSubGamePhase == that.passSubGamePhase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId, passSubGamePhase);
    }

    @Override
    public String toString() {
        return "p" + playerID + " discards supporter " + cardIdx;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " discards supporter " + card.toString();
    }
}
