package games.uno.actions;


import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.uno.UnoGameParameters;
import games.uno.UnoTurnOrder;
import games.uno.cards.UnoCard;
import games.uno.UnoGameState;
import utilities.Utils;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import static games.uno.UnoGameParameters.UnoScoring.CHALLENGE;

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
        UnoGameState ugs = (UnoGameState) gameState;
        UnoGameParameters ugp = (UnoGameParameters) gameState.getGameParameters();
        super.execute(gameState);

        Random r = new Random(ugp.getRandomSeed() + ugs.getTurnOrder().getRoundCounter());

        UnoCard cardToBePlayed = (UnoCard) gameState.getComponentById(cardId);
        ugs.updateCurrentCard(cardToBePlayed);

        int nextPlayer = gameState.getTurnOrder().nextPlayer(gameState);
        Deck<UnoCard> drawDeck = ugs.getDrawDeck();
        Deck<UnoCard> discardDeck = ugs.getDiscardDeck();
        List<Deck<UnoCard>> playerDecks = ugs.getPlayerDecks();

        int players = ugs.getNPlayers();
        if (ugp.scoringMethod == CHALLENGE) {
            players = 0;
            for (int p = 0; p < ugs.getNPlayers(); p++) {
                if (ugs.getPlayerResults()[p] == Utils.GameResult.GAME_ONGOING)
                    players++;
            }
        }

        switch (cardToBePlayed.type) {
            case Reverse:
                if (players == 2) { // Reverse cards are SKIP for 2 players
                    ((UnoTurnOrder) gameState.getTurnOrder()).skip();
                } else {
                    ((UnoTurnOrder) gameState.getTurnOrder()).reverse();
                }
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

    @Override
    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            Deck<UnoCard> deck = (Deck<UnoCard>) gs.getComponentById(deckFrom);
            if (fromIndex == deck.getSize()) return deck.get(fromIndex - 1);
            return deck.get(fromIndex);
        }
        return (UnoCard) gs.getComponentById(cardId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCard)) return false;
        if (!super.equals(o)) return false;
        PlayCard playCard = (PlayCard) o;
        return Objects.equals(color, playCard.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color);
    }

    @Override
    public AbstractAction copy() {
        return new PlayCard(deckFrom, deckTo, fromIndex, color);
    }
}

