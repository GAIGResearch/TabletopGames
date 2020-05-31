package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.coltexpress.cards.ColtExpressCard;


public class DrawCardsAction extends AbstractAction implements IPrintable {

    private final PartialObservableDeck<ColtExpressCard> handCards;
    private final PartialObservableDeck<ColtExpressCard> deckCards;

    public DrawCardsAction(PartialObservableDeck<ColtExpressCard> handCards,
                           PartialObservableDeck<ColtExpressCard> deckCards){
        this.handCards = handCards;
        this.deckCards = deckCards;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        for (int i = 0; i < 3; i++)
        {
            ColtExpressCard c = deckCards.draw();
            if (c != null)
                handCards.add(c);
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
        //return false;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString(){
        return "Draw 3 cards";
    }
}
