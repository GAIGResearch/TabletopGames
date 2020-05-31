package games.uno.actions;

import core.AbstractGameState;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.uno.UnoGameState;
import games.uno.UnoTurnOrder;
import games.uno.cards.UnoCard;

import java.util.List;

public class PlayWildDraw4 extends PlayWild implements IPrintable {

    public PlayWildDraw4(int deckFrom, int deckTo, int fromIndex, String color) {
        super(deckFrom, deckTo, fromIndex, color);
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        UnoGameState ugs = (UnoGameState) gameState;
        super.execute(gameState);

        Deck<UnoCard> drawDeck = ugs.getDrawDeck();
        Deck<UnoCard> discardDeck = ugs.getDiscardDeck();
        List<Deck<UnoCard>> playerDecks = ugs.getPlayerDecks();

        int nextPlayer = ugs.getTurnOrder().nextPlayer(gameState);
        for (int i = 0; i < 4; i ++) {
            if (drawDeck.getSize() == 0) {
                drawDeck.add(discardDeck);
                discardDeck.clear();

                // Add the current card to the discardDeck
                drawDeck.remove(ugs.getCurrentCard());
                discardDeck.add(ugs.getCurrentCard());

                drawDeck.shuffle();
            }
            playerDecks.get(nextPlayer).add(drawDeck.draw());
        }

        ((UnoTurnOrder) gameState.getTurnOrder()).skip();
        return true;
    }
}
