package games.uno.actions;


import core.AbstractGameState;
import core.actions.DrawCard;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.uno.UnoTurnOrder;
import games.uno.cards.UnoCard;
import games.uno.cards.UnoDrawTwoCard;
import games.uno.cards.UnoReverseCard;
import games.uno.cards.UnoSkipCard;
import games.uno.UnoGameState;

import java.util.List;

public class PlayCard extends DrawCard implements IPrintable {

    public PlayCard(int deckFrom, int deckTo, int cardToBePlayed) {
        super(deckFrom, deckTo, cardToBePlayed);
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        UnoGameState ugs = (UnoGameState)gameState;
        super.execute(gameState);

        UnoCard cardToBePlayed = (UnoCard) gameState.getComponentById(cardId);
        ugs.updateCurrentCard(cardToBePlayed);

        if (cardToBePlayed instanceof UnoReverseCard)
            ((UnoTurnOrder)gameState.getTurnOrder()).reverse();
        else if (cardToBePlayed instanceof UnoSkipCard)
            ((UnoTurnOrder)gameState.getTurnOrder()).skip();
        else if (cardToBePlayed instanceof UnoDrawTwoCard) {
            int nextPlayer = gameState.getTurnOrder().nextPlayer(gameState);
            Deck<UnoCard> drawDeck = ugs.getDrawDeck();
            Deck<UnoCard> discardDeck = ugs.getDiscardDeck();
            List<Deck<UnoCard>> playerDecks = ugs.getPlayerDecks();
            for (int i = 0; i < 2; i ++) {
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
        }

        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println("Play card");
    }
}

