package games.explodingkittens.actions;

import core.actions.DrawCard;
import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;

public class PlaceExplodingKitten extends DrawCard implements IPrintable {

    public PlaceExplodingKitten(int deckFrom, int deckTo, int index, int targetIndex) {
        super(deckFrom, deckTo, index, targetIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean success = super.execute(gs);

        gs.setMainGamePhase();
        ((ExplodingKittenTurnOrder)gs.getTurnOrder()).endPlayerTurnStep(gs);
        return success;
    }

    @Override
    public String toString(){
        return String.format("Player defuses the kitten and places it at index  %d", toIndex);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}
