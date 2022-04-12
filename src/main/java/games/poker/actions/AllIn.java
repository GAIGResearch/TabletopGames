package games.poker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.poker.PokerGameState;

import java.util.Objects;

public class AllIn extends AbstractAction implements IPrintable {

    private final int playerId;

    public AllIn(int id) {
        this.playerId = id;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        pgs.placeBet(pgs.getPlayerMoney()[playerId].getValue(), playerId);
        pgs.setBet(true);
        pgs.getPlayerNeedsToCall()[playerId] = false;

        // Others can't check, unless all in
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (i != playerId && !pgs.getPlayerFold()[i] && !pgs.getPlayerMoney()[i].isMinimum()) {
                pgs.getPlayerNeedsToCall()[i] = true;
            }
        }

        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(getString(gameState));
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "All in";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AllIn)) return false;
        AllIn bet = (AllIn) o;
        return playerId == bet.playerId;
    }

    @Override
    public int hashCode() {
        return playerId - 479245298;
    }

    @Override
    public AllIn copy() {
        return this; // immutable
    }

}
