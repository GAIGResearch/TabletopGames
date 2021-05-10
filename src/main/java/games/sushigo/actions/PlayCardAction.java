package games.sushigo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import java.awt.font.TextHitInfo;

public class PlayCardAction extends AbstractAction {
    int deckFromId;
    int deckToId;
    int cardIndex;
    SGCard.SGCardType cardType;

    public PlayCardAction(int deckFromId, int deckToId, int cardIndex, SGCard.SGCardType cardType)
    {
        this.deckFromId = deckFromId;
        this.deckToId = deckToId;
        this.cardIndex = cardIndex;
        this.cardType = cardType;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SGGameState SGGS = (SGGameState) gs;
        Deck<SGCard> deckFrom = (Deck<SGCard>) SGGS.getComponentById(deckFromId);
        Deck<SGCard> deckTo = (Deck<SGCard>) SGGS.getComponentById(deckToId);
        SGCard cardToPlay = deckFrom.get(cardIndex);
        deckFrom.remove(cardIndex);
        deckTo.add(cardToPlay);
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
        return "Play " + cardType;
    }
}
