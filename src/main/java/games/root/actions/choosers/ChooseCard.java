package games.root.actions.choosers;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class ChooseCard extends AbstractAction {
    public final int playerID;
    public final int cardIdx, cardId;

    public ChooseCard(int playerID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        return gs.getCurrentPlayer() == playerID;
    }

    @Override
    public ChooseCard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChooseCard that = (ChooseCard) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " chooses card " + cardIdx;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " chooses " + card.suit.toString() + " card " + card.cardType.toString();
    }
}
