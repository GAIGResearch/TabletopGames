package games.poker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.poker.PokerGameState;

import java.util.Objects;

public class Check extends AbstractAction implements IPrintable {

    private final int playerId;

    public Check(int id) {
        this.playerId = id;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        pgs.getPlayerNeedsToCall()[playerId] = false;
        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(getString(gameState));
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Check";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Check)) return false;
        Check call = (Check) o;
        return playerId == call.playerId;
    }

    @Override
    public int hashCode() {
        return playerId + 39128947;
    }

    @Override
    public Check copy() {
        return this; // immutable
    }

}
