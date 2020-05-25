package games.explodingkittens.actions;

import core.actions.DrawCard;
import core.AbstractGameState;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import core.turnorder.TurnOrder;

import java.util.Objects;


public class AttackAction extends DrawCard implements IsNopeable, IPrintable {
    int attackTargetID;

    public AttackAction(int deckFrom, int deckTo, int index, int attackTargetID) {
        super(deckFrom, deckTo, index);
        this.attackTargetID = attackTargetID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discard card played
        super.execute(gs);
        // Execute action
        ((ExplodingKittenTurnOrder) gs.getTurnOrder()).registerAttackAction(attackTargetID);
        return false;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player attacks player %d", attackTargetID);
    }

    @Override
    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AttackAction that = (AttackAction) o;
        return attackTargetID == that.attackTargetID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackTargetID);
    }
}
