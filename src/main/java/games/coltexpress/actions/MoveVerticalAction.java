package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;

public class MoveVerticalAction extends DrawCard {

    private final int compartment;
    private final boolean climbRoof;

    public MoveVerticalAction(int plannedActions, int playerDeck,
                              int compartment, boolean toRoof){
        super(plannedActions, playerDeck);

        this.compartment = compartment;
        this.climbRoof = toRoof;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        Compartment comp = (Compartment) gs.getComponentById(compartment);
        ColtExpressCard card = (ColtExpressCard) gs.getComponentById(cardId);

        if (climbRoof){
            comp.playersInsideCompartment.remove(card.playerID);
            comp.playersOnTopOfCompartment.add(card.playerID);
        } else {
            comp.playersOnTopOfCompartment.remove(card.playerID);
            comp.playersInsideCompartment.add(card.playerID);
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

    public String toString(){
        return "MoveVerticalAction: climbRoof=" + climbRoof;

    }
}
