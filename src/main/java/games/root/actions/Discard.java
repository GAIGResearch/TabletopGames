package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root.RootGameState;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class Discard extends AbstractAction {
    public final int playerID;
    public final int cardIdx, cardId;
    public final boolean passSubGamePhase;

    public Discard(int playerID, int cardIdx, int cardId, boolean passSubGamePhase){
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
            Deck<RootCard> playerHand = currentState.getPlayerHand(playerID);
            RootCard card = playerHand.pick(cardIdx);
            discardPile.add(card);
            return true;
        }
        return false;
    }

    @Override
    public Discard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Discard discard = (Discard) o;
        return playerID == discard.playerID && cardIdx == discard.cardIdx && cardId == discard.cardId && passSubGamePhase == discard.passSubGamePhase;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId, passSubGamePhase);
    }

    @Override
    public String toString() {
        return "p" + playerID + " discards card " + cardIdx;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " discards " + card.suit.toString() + " card " + card.toString();
    }
}
