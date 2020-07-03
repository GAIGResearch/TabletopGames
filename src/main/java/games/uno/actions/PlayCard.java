package games.uno.actions;


import core.AbstractGameState;
import core.actions.DrawCard;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.uno.UnoTurnOrder;
import games.uno.cards.UnoCard;
import games.uno.UnoGameState;

import java.util.List;
import java.util.Random;

public class PlayCard extends DrawCard implements IPrintable {

    private String color;

    public PlayCard(int deckFrom, int deckTo, int cardToBePlayed) {
        super(deckFrom, deckTo, cardToBePlayed);
    }
    public PlayCard(int deckFrom, int deckTo, int cardToBePlayed, String color) {
        super(deckFrom, deckTo, cardToBePlayed);
        this.color = color;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        UnoGameState ugs = (UnoGameState)gameState;
        super.execute(gameState);

        Random r = new Random(ugs.getGameParameters().getGameSeed() + ugs.getTurnOrder().getRoundCounter());

        UnoCard cardToBePlayed = (UnoCard) gameState.getComponentById(cardId);
        ugs.updateCurrentCard(cardToBePlayed);

        int nextPlayer = gameState.getTurnOrder().nextPlayer(gameState);
        Deck<UnoCard> drawDeck = ugs.getDrawDeck();
        Deck<UnoCard> discardDeck = ugs.getDiscardDeck();
        List<Deck<UnoCard>> playerDecks = ugs.getPlayerDecks();

        switch (cardToBePlayed.type) {
            case Reverse:
                ((UnoTurnOrder) gameState.getTurnOrder()).reverse();
                break;
            case Skip:
                ((UnoTurnOrder) gameState.getTurnOrder()).skip();
                break;
            case Draw:
                for (int i = 0; i < cardToBePlayed.drawN; i++) {
                    if (drawDeck.getSize() == 0) {
                        drawDeck.add(discardDeck);
                        discardDeck.clear();

                        // Add the current card to the discardDeck
                        drawDeck.remove(ugs.getCurrentCard());
                        discardDeck.add(ugs.getCurrentCard());

                        drawDeck.shuffle(r);
                    }
                    playerDecks.get(nextPlayer).add(drawDeck.draw());
                }
                ((UnoTurnOrder) gameState.getTurnOrder()).skip();
                break;
            case Wild:
                ugs.updateCurrentCard(cardToBePlayed, color);

                for (int i = 0; i < cardToBePlayed.drawN; i ++) {
                    if (drawDeck.getSize() == 0) {
                        drawDeck.add(discardDeck);
                        discardDeck.clear();

                        // Add the current card to the discardDeck
                        drawDeck.remove(ugs.getCurrentCard());
                        discardDeck.add(ugs.getCurrentCard());

                        drawDeck.shuffle(r);
                    }
                    playerDecks.get(nextPlayer).add(drawDeck.draw());
                }
                if (cardToBePlayed.drawN > 0) {
                    ((UnoTurnOrder) gameState.getTurnOrder()).skip();
                }
                break;
        }

        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(getString(gameState));
    }

    @Override
    public String toString() {
        if (color != null && !color.equals("")) {
            return "PlayWild{" +
                    "color=" + color +
                    '}';
        }
        return "Play card";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (color != null && !color.equals("")) {
            return getCard(gameState).toString() + "; Change to color " + color;
        }
        return "Play card " + getCard(gameState).toString();
    }
}

