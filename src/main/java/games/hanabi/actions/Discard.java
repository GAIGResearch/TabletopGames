package games.hanabi.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Deck;
import core.components.Counter;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;

import games.hanabi.HanabiGameState;
import games.hanabi.*;

public class Discard extends DrawCard implements IPrintable {
    public Discard(int deckFrom, int deckTo, int cardToBePlayed) {
        super(deckFrom, deckTo, cardToBePlayed);
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        HanabiGameState hbgs = (HanabiGameState) gameState;
        Deck<HanabiCard> drawDeck = hbgs.getDrawDeck();
        PartialObservableDeck<HanabiCard> playerDeck = hbgs.getPlayerDecks().get(hbgs.getCurrentPlayer());
        Counter hintCounter = hbgs.getHintCounter();
        super.execute(gameState);
        if(drawDeck.getComponents().size() > 0) {
            HanabiCard card = drawDeck.draw();
            playerDeck.add(card);
        }
        hintCounter.increment(1);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new Discard(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Discard;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Discard card " + fromIndex + " and draw a card.";
    }

    @Override
    public String toString() {
        return "Discard card " + fromIndex + " and draw a card.";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("Discarded card and draw a card.");
    }
}
