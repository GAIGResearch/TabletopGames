package games.explodingkittens.actions.reactions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensTurnOrder;

public class PlaceExplodingKitten extends DrawCard implements IPrintable {

    public PlaceExplodingKitten(int deckFrom, int deckTo, int index, int targetIndex) {
        super(deckFrom, deckTo, index, targetIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean success = super.execute(gs);

        gs.setMainGamePhase();
        ((ExplodingKittensTurnOrder)gs.getTurnOrder()).endPlayerTurnStep(gs);
        return success;
    }

    @Override
    public String toString(){
        return String.format("Player defuses the kitten and places it at index  %d", toIndex);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("[DEFUSED] Place kitten at index %d", toIndex);
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public AbstractAction copy() {
        return new PlaceExplodingKitten(deckFrom, deckTo, fromIndex, toIndex);
    }
}
