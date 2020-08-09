package games.coltexpress.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Compartment;

import java.util.Objects;

public class MoveVerticalAction extends DrawCard {

    private final int compartment;
    private final boolean climbRoof;

    public MoveVerticalAction(int plannedActions, int playerDeck, int cardIdx,
                              int compartment, boolean toRoof){
        super(plannedActions, playerDeck, cardIdx);

        this.compartment = compartment;
        this.climbRoof = toRoof;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        Compartment comp = (Compartment) gs.getComponentById(compartment);
        ColtExpressCard card = (ColtExpressCard) getCard(gs);

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoveVerticalAction)) return false;
        if (!super.equals(o)) return false;
        MoveVerticalAction that = (MoveVerticalAction) o;
        return compartment == that.compartment &&
                climbRoof == that.climbRoof;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), compartment, climbRoof);
    }

    public String toString(){
        return "MoveVerticalAction: climbRoof=" + climbRoof;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Move " + (climbRoof? "up": "down");
    }

    @Override
    public AbstractAction copy() {
        return new MoveVerticalAction(deckFrom, deckTo, fromIndex, compartment, climbRoof);
    }
}
