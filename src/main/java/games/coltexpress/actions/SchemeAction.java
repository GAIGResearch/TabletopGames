package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.coltexpress.cards.ColtExpressCard;


public class SchemeAction extends DrawCard implements IPrintable {

    private final boolean hidden;

    public SchemeAction(int handCards, int actionList, boolean hidden){
        super(handCards, actionList);
        this.hidden = hidden;
    }

    @Override
    public boolean execute(AbstractGameState gs){
        super.execute(gs);

        PartialObservableDeck<ColtExpressCard> actionList = (PartialObservableDeck<ColtExpressCard>) gs.getComponentById(deckTo);
        ColtExpressCard card = (ColtExpressCard) gs.getComponentById(cardId);

        if (hidden){
            actionList.setVisibilityOfComponent(actionList.getSize()-1, card.playerID, true);
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
            return "PlayCard(hidden)";
        return "PlayCard";
    }

}
