package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.PartialObservableDeck;
import core.observations.IPrintable;
import games.coltexpress.cards.ColtExpressCard;


public class DrawCardsAction implements IAction, IPrintable {

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
    public String toString(){
        return "Draw 3 cards";
    }
}
