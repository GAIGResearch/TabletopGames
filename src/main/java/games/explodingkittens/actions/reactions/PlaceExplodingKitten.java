package games.explodingkittens.actions.reactions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.ExplodingKittensTurnOrder;

public class PlaceExplodingKitten extends DrawCard implements IPrintable {

    public PlaceExplodingKitten(int deckFrom, int deckTo, int index, int targetIndex) {
        super(deckFrom, deckTo, index, targetIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        boolean success = super.execute(gs);

        gs.setGamePhase(CoreConstants.DefaultGamePhase.Main);
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        ((ExplodingKittensTurnOrder)ekgs.getTurnOrder()).endPlayerTurnStep(gs);
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

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PlaceExplodingKitten)) return false;
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode() - 293792;
    }
}
