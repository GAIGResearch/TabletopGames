package games.coltexpress.actions;

import core.components.PartialObservableDeck;
import games.coltexpress.cards.ColtExpressCard;

public class PunchAction  extends ColtExpressExecuteCardAction{
    public PunchAction(ColtExpressCard card, PartialObservableDeck<ColtExpressCard> plannedActions, PartialObservableDeck<ColtExpressCard> playerDeck) {
        super(card, plannedActions, playerDeck);
    }

    public String toString(){
        return "PunchAction not yet implemented";
    }
}
