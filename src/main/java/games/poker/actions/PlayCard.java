package games.poker.actions;

import core.actions.DrawCard;
import core.components.FrenchCard;
import core.interfaces.IPrintable;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Card;
import core.components.Deck;
import games.poker.PokerGameState;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class PlayCard extends DrawCard implements IPrintable {

    private String suite;

    public PlayCard(int deckFrom, int deckTo, int deckSize) {
        super(deckFrom, deckTo, deckSize);
    }

    public PlayCard(int deckFrom, int deckTo, int deckSize, String suite) {
        super(deckFrom, deckTo, deckSize);
        this.suite = suite;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        super.execute(gameState);

        Random r = new Random(pgs.getGameParameters().getRandomSeed() + pgs.getTurnOrder().getRoundCounter());

        FrenchCard cardToBePlayed = (FrenchCard) gameState.getComponentById(cardId);
        pgs.updateCurrentCard(cardToBePlayed);

        int nextPlayer = gameState.getTurnOrder().nextPlayer(gameState);
        Deck<FrenchCard> drawDeck = pgs.getDrawDeck();
        Deck<FrenchCard> discardDeck = pgs.getDiscardDeck();
        List<Deck<FrenchCard>> playerDecks = pgs.getPlayerDecks();
        //System.out.println("playcard from " + pgs.getCurrentPlayer());

        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(getString(gameState));
    }

    @Override
    public String toString() {
        if (suite != null && !suite.equals("")) {
            return "card{" +
                    "suite=" + suite +
                    '}';
        }
        return "Play card";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (suite != null && !suite.equals("")) {
            return getCard(gameState).toString() + "; Change to suite " + suite;
        }
        return "Play card " + getCard(gameState).toString();
    }

    @Override
    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            Deck<FrenchCard> deck = (Deck<FrenchCard>) gs.getComponentById(deckFrom);
            if (fromIndex == deck.getSize()) return deck.get(fromIndex-1);
            return deck.get(fromIndex);
        }
        return (FrenchCard) gs.getComponentById(cardId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof games.poker.actions.PlayCard)) return false;
        if (!super.equals(o)) return false;
        games.poker.actions.PlayCard playCard = (games.poker.actions.PlayCard) o;
        return Objects.equals(suite, playCard.suite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), suite);
    }

    @Override
    public AbstractAction copy() {
        return new games.poker.actions.PlayCard(deckFrom, deckTo, fromIndex, suite);
    }

}
