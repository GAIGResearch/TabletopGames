package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensTurnOrder;
import core.turnorders.TurnOrder;

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
        ((ExplodingKittensTurnOrder) gs.getTurnOrder()).registerAttackAction(attackTargetID);
        return false;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Attack player %d", attackTargetID);
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
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttackAction)) return false;
        if (!super.equals(o)) return false;
        AttackAction that = (AttackAction) o;
        return attackTargetID == that.attackTargetID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), attackTargetID);
    }

    @Override
    public AbstractAction copy() {
        return new AttackAction(deckFrom, deckTo, fromIndex, attackTargetID);
    }
}
