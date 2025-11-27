package games.seasaltpaper.actions;

import core.AbstractGameState;
import core.actions.DrawCard;

// simple wrapper
public class SSPDrawCard extends DrawCard {
    public SSPDrawCard (int deckFrom, int deckTo, int fromIndex, int toIndex) {
        super(deckFrom, deckTo, fromIndex, toIndex);
    }

    public SSPDrawCard (int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    public SSPDrawCard (int deckFrom, int deckTo) {
        super(deckFrom, deckTo);
    }

    public SSPDrawCard(){}

    @Override
    public String toString() {
        return "Draw from " + deckFrom;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Draw from " + gameState.getComponentById(deckFrom).getComponentName();
    }
}
