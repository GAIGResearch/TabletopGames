package games.explodingkittens.actions;

import components.IDeck;
import core.AbstractGameState;
import observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import turnorder.TurnOrder;


public class AttackAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {
    int attackTargetID;

    public AttackAction(T card, IDeck<T> sourceDeck, IDeck<T> targetDeck, int attackTargetID) {
        super(card, sourceDeck, targetDeck);
        this.attackTargetID = attackTargetID;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        super.Execute(gs, turnOrder);
        ((ExplodingKittenTurnOrder) turnOrder).registerAttackAction(attackTargetID);
        return false;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player attacks player %d", attackTargetID);
    }

    @Override
    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.Execute(gs, turnOrder);
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}
