package games.coltexpress.actions;

import core.AbstractGameState;
import core.components.PartialObservableDeck;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;

public class MoveVerticalAction extends ColtExpressExecuteCardAction {

    private final Compartment compartment;
    private final boolean climbRoof;

    public MoveVerticalAction(ColtExpressCard card, PartialObservableDeck<ColtExpressCard> plannedActions,
                              PartialObservableDeck<ColtExpressCard> playerDeck,
                              Compartment compartment, boolean toRoof){
        super(card, plannedActions, playerDeck);
        this.compartment = compartment;
        this.climbRoof = toRoof;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        if (climbRoof){
            compartment.playersInsideCompartment.remove(card.playerID);
            compartment.playersOnTopOfCompartment.add(card.playerID);
        } else {
            compartment.playersOnTopOfCompartment.remove(card.playerID);
            compartment.playersInsideCompartment.add(card.playerID);
        }
        return false;
    }

    public String toString(){
        return "MoveVerticalAction: player " + card.playerID + "; climbRoof=" + climbRoof;
    }
}
