package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.coltexpress.cards.ColtExpressCard;

public abstract class ColtExpressExecuteCardAction extends AbstractAction {

    protected final ColtExpressCard card;
    protected final PartialObservableDeck<ColtExpressCard> playerDeck;
    protected final PartialObservableDeck<ColtExpressCard> plannedActions;

    public ColtExpressExecuteCardAction(ColtExpressCard card,
                                        PartialObservableDeck<ColtExpressCard> plannedActions,
                                        PartialObservableDeck<ColtExpressCard> playerDeck){
        this.card = card;
        this.playerDeck = playerDeck;
        this.plannedActions = plannedActions;
    }

    @Override
    public boolean execute(AbstractGameState gameState){
        plannedActions.remove(card);
        playerDeck.add(card);
        return true;
    }

    public String toString(){
        return "toString not implemented yet";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}
