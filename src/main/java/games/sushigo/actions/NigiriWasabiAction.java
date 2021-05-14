package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

public class NigiriWasabiAction extends AbstractAction {
    int playerId;
    int cardIndex;
    SGCard.SGCardType cardType;

    public NigiriWasabiAction(int playerId, int cardIndex, SGCard.SGCardType cardType)
    {
        this.playerId = playerId;
        this.cardIndex = cardIndex;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SGGameState SGGS = (SGGameState) gs;
        SGGS.setPlayerCardPick(cardIndex, playerId);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof DebugAction;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use wasabi with " + cardType;
    }
}
