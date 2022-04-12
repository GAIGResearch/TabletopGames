package games.poker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.poker.PokerGameState;
import games.poker.PokerTurnOrder;

import java.util.Objects;

public class Fold extends AbstractAction implements IPrintable {

    final int playerId;

    public Fold(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        pgs.getPlayerFold()[playerId] = true;
        pgs.getPlayerNeedsToCall()[playerId] = false;

        ((PokerTurnOrder)pgs.getTurnOrder()).fold(pgs, playerId);

        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("Fold");
    }


    @Override
    public String getString(AbstractGameState gameState) {
        return "Fold";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fold)) return false;
        Fold fold = (Fold) o;
        return playerId == fold.playerId;
    }

    @Override
    public int hashCode() {
        return playerId - 1318;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

}
