package updated_core.games.uno.cards;

import components.IDeck;
import updated_core.actions.IAction;
import updated_core.actions.IPrintable;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;

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
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
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
    public void PrintToConsole() {
        System.out.println("Draw " + numberOfCards + " card(s)");
    }
}
