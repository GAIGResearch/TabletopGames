package games.uno.cards;

import core.actions.IAction;
import core.components.Card;
import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.observations.IPrintable;

public class DrawCards<T extends Component> implements IAction, IPrintable {

    private final Deck<T> deckFrom;
    private final Deck<T> deckTo;
    private final Deck<T> deckReserve;
    private final int numberOfCards;

    public DrawCards(Deck<T> drawPile, Deck<T> playerHand, Deck<T> discardPile, int numberOfCards) {
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
            if (deckFrom.getComponents().isEmpty())
            {
                T activeCard = deckReserve.draw();
                deckReserve.shuffle();
                while (!deckReserve.getComponents().isEmpty()){
                    deckFrom.add(deckReserve.draw());
                }
                deckReserve.add(activeCard);
            }
        }

        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public void printToConsole() {
        System.out.println("Draw " + numberOfCards + " card(s)");
    }
}
