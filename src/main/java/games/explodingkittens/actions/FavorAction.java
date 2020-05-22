package games.explodingkittens.actions;

import core.actions.DrawCard;
import core.AbstractGameState;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import core.turnorder.TurnOrder;

import java.util.Objects;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.Favor;

public class FavorAction extends DrawCard implements IsNopeable, IPrintable {
    final int target;

    public FavorAction(int deckFrom, int deckTo, int index, int target) {
        super(deckFrom, deckTo, index);
        this.target = target;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        ExplodingKittensGameState ekgs = ((ExplodingKittensGameState)gs);
        ekgs.setGamePhase(Favor);
        ekgs.setPlayerGettingAFavor(gs.getTurnOrder().getCurrentPlayer(gs));

        ExplodingKittenTurnOrder ekto = (ExplodingKittenTurnOrder) gs.getTurnOrder();
        ekto.registerFavorAction(target);
        return true;
    }

    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player asks Player %d for a favor", target);
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
        FavorAction that = (FavorAction) o;
        return target == that.target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), target);
    }
}
