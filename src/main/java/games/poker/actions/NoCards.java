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

import java.util.Random;

import static core.CoreConstants.VERBOSE;

public class NoCards extends AbstractAction implements IPrintable {

    // If the card drawn is playable, then play it
    @Override
    public boolean execute(AbstractGameState gs) {
        PokerGameState pgs = (PokerGameState)gs;
        Deck<FrenchCard> drawDeck = pgs.getDrawDeck();
        Deck<FrenchCard> discardDeck = pgs.getDiscardDeck();
        Deck<FrenchCard> playerDeck = pgs.getPlayerDecks().get(pgs.getTurnOrder().getCurrentPlayer(gs));

        Random r = new Random(pgs.getGameParameters().getRandomSeed() + pgs.getTurnOrder().getRoundCounter());
        //System.out.println("no cards from " + pgs.getCurrentPlayer());
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new NoCards();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NoCards;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Empty.";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("Empty.");
    }
}
