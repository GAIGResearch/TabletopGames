package games.coltexpress.actions;

import core.components.PartialObservableDeck;
import games.coltexpress.cards.ColtExpressCard;

public class CollectMoneyAction extends ColtExpressExecuteCardAction{
    public CollectMoneyAction(ColtExpressCard card, PartialObservableDeck<ColtExpressCard> plannedActions, PartialObservableDeck<ColtExpressCard> playerDeck) {
        super(card, plannedActions, playerDeck);
    }

    public String toString(){
        return "CollectMoneyAction not yet implemented";
    }
}
