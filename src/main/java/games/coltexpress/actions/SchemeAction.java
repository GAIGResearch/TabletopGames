package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.coltexpress.cards.ColtExpressCard;


public class SchemeAction extends AbstractAction implements IPrintable {

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
            actionList.setVisibilityOfComponent(actionList.getSize()-1, card.playerID, true);
        } else{
            actionList.add(card, actionList.getSize());
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
        if (hidden)
            return "PlayCard(hidden): " + card.cardType;
        return "PlayCard: " + card.cardType;
    }

}
