package games.uno.actions;


import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.uno.UnoGameState;
import games.uno.cards.UnoCard;


public class NoCards extends AbstractAction implements IPrintable {

    // If the card drawn is playable, then play it
    @Override
    public boolean execute(AbstractGameState gs) {
        UnoGameState ugs = (UnoGameState)gs;
        Deck<UnoCard> drawDeck = ugs.getDrawDeck();
        Deck<UnoCard> discardDeck = ugs.getDiscardDeck();
        Deck<UnoCard> playerDeck = ugs.getPlayerDecks().get(ugs.getCurrentPlayer());

        if (drawDeck.getSize() == 0) {
            drawDeck.add(discardDeck);
            discardDeck.clear();

            // Add the current card to the discardDeck
            drawDeck.remove(ugs.getCurrentCard());
            discardDeck.add(ugs.getCurrentCard());

            drawDeck.shuffle(ugs.getRnd());
        }

        UnoCard card = drawDeck.draw();

        if (card == null)
            throw new AssertionError("Game should have already finished!");

        if (card.isPlayable((UnoGameState) gs)) {
            discardDeck.add(card);
            ugs.updateCurrentCard(card);
            if (ugs.getCoreGameParameters().verbose) {
                System.out.println("It can be played. " + card);
            }
        }
        else
            playerDeck.add(card);
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
        return "No playable cards. You must draw a card.";
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("No playable cards. You must draw a card.");
    }
}
