package games.explodingkittens.actions;

import core.actions.DrawCard;
import core.AbstractGameState;
import core.observations.IPrintable;
import core.turnorder.TurnOrder;
import games.explodingkittens.ExplodingKittensGameState;

import java.util.Random;

public class ShuffleAction extends DrawCard implements IsNopeable, IPrintable {

    public ShuffleAction(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((ExplodingKittensGameState)gs).getDrawPile().shuffle(new Random(gs.getGameParameters().getGameSeed()));
        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Player shuffles the draw pile";
    }

    @Override
    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}
