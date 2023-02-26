package games.poker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.poker.PokerGameState;

import java.util.Objects;

public class Call extends AbstractAction implements IPrintable {

    private final int playerId;

    public Call(int id) {
        this.playerId = id;
    }


    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        int biggestBet = 0;
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (pgs.getPlayerBet()[i].getValue() > biggestBet) biggestBet = pgs.getPlayerBet()[i].getValue();
        }
        int diff = biggestBet - pgs.getPlayerBet()[playerId].getValue();

        pgs.placeBet(diff, playerId);
        pgs.getPlayerNeedsToCall()[playerId] = false;
        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(getString(gameState));
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Call";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Call)) return false;
        Call call = (Call) o;
        return playerId == call.playerId;
    }

    @Override
    public int hashCode() {
        return playerId + 3783;
    }

    @Override
    public Call copy() {
        return this; // immutable
    }

}
