package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.DrawCard;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;

public class MoveSidewaysAction extends DrawCard {

    private final int sourceCompartment;
    private final int targetCompartment;

    public MoveSidewaysAction(int plannedActions, int playerDeck,
                             int sourceCompartment, int targetCompartment){
        super(plannedActions, playerDeck);
        this.sourceCompartment = sourceCompartment;
        this.targetCompartment = targetCompartment;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        Compartment source = (Compartment) gs.getComponentById(sourceCompartment);
        Compartment target = (Compartment) gs.getComponentById(targetCompartment);
        ColtExpressCard card = (ColtExpressCard) gs.getComponentById(cardId);

        if (source.playersInsideCompartment.contains(card.playerID)){
            source.playersInsideCompartment.remove(card.playerID);
            if (target.containsMarshal){
                ((ColtExpressGameState) gs).addNeutralBullet(card.playerID);
                target.playersOnTopOfCompartment.add(card.playerID);
            }
            else
                target.playersInsideCompartment.add(card.playerID);
        }
        else{
            source.playersOnTopOfCompartment.remove(card.playerID);
            target.playersOnTopOfCompartment.add(card.playerID);
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
        return "MoveSideways";
    }
}
