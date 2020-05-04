package games.uno.cards;

import core.actions.IAction;
import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;

public class DrawCards<T> implements IAction, IPrintable {

    private final IDeck<T> deckFrom;
    private final IDeck<T> deckTo;
    private final IDeck<T> deckReserve;
    private final int numberOfCards;

    public DrawCards(IDeck<T> drawPile, IDeck<T> playerHand, IDeck<T> discardPile, int numberOfCards) {
        this.deckFrom = drawPile;
        this.deckTo = playerHand;
        this.deckReserve = discardPile;
        this.numberOfCards = numberOfCards;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        for (int i = 0; i < this.numberOfCards; i++){
            T card = deckFrom.draw();
            deckTo.add(card);
            if (deckFrom.getCards().isEmpty())
            {
                T activeCard = deckReserve.draw();
                deckReserve.shuffle();
                while (!deckReserve.getCards().isEmpty()){
                    deckFrom.add(deckReserve.draw());
                }
                deckReserve.add(activeCard);
            }
        }

        return true;
    }

    @Override
    public void printToConsole() {
        System.out.println("Draw " + numberOfCards + " card(s)");
    }
}
