package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;

import java.util.Objects;

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
        ColtExpressCard card = (ColtExpressCard) getCard(gs);

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MoveSidewaysAction that = (MoveSidewaysAction) o;
        return sourceCompartment == that.sourceCompartment &&
                targetCompartment == that.targetCompartment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceCompartment, targetCompartment);
    }

    public String toString(){
        return "MoveSideways";
    }

    @Override
    public AbstractAction copy() {
        return new MoveSidewaysAction(deckFrom, deckTo, sourceCompartment, targetCompartment);
    }
}
