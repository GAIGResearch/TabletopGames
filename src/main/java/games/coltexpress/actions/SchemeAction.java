package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.IAction;
import core.components.PartialObservableDeck;
import core.observations.IPrintable;
import games.coltexpress.cards.ColtExpressCard;


public class SchemeAction implements IAction, IPrintable {

    private final ColtExpressCard card;
    private final PartialObservableDeck<ColtExpressCard> handCards;
    private final PartialObservableDeck<ColtExpressCard> actionList;
    private final boolean hidden;

    public SchemeAction(ColtExpressCard card, PartialObservableDeck<ColtExpressCard> handCards,
                        PartialObservableDeck<ColtExpressCard> actionList, boolean hidden){
        this.card = card;
        this.handCards = handCards;
        this.actionList = actionList;
        this.hidden = hidden;
    }

    @Override
    public boolean execute(AbstractGameState gs){
        handCards.remove(card);
        if (hidden){
            actionList.add(card, actionList.getSize());
            actionList.setVisibilityOfCard(actionList.getSize()-1, card.playerID, true);
        } else{
            actionList.add(card, actionList.getSize());
        }
        return true;
    }

    @Override
    public String toString(){
        if (hidden)
            return "PlayCard(hidden): " + card.cardType;
        return "PlayCard: " + card.cardType;
    }

}
