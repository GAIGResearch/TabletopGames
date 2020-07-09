package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensTurnOrder;
import core.turnorders.TurnOrder;

public class SkipAction extends DrawCard implements IsNopeable, IPrintable {

    public SkipAction(int deckFrom, int deckTo, int index) {
        super(deckFrom, deckTo, index);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discard the card played
        super.execute(gs);
        // Execute action
        gs.setMainGamePhase();
        ((ExplodingKittensTurnOrder)gs.getTurnOrder()).endPlayerTurnStep(gs);
        return true;
    }

    @Override
    public String toString(){
        return "Player skips its draw";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Skip";
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
    public AbstractAction copy() {
        return new SkipAction(deckFrom, deckTo, fromIndex);
    }
}
