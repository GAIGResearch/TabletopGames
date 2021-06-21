package games.poker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.poker.PokerGameState;

import java.util.Objects;

public class Raise extends AbstractAction implements IPrintable {

    private final int playerId;
    private final double multiplier;

    public Raise(int id, double multiplier) {
        this.playerId = id;
        this.multiplier = multiplier;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        int biggestBet = 0;
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (pgs.getBets()[i] > biggestBet) biggestBet = pgs.getBets()[i];
        }

        int diff = biggestBet + (int)(biggestBet * multiplier)- pgs.getBets()[playerId];  // First call, then raise
        pgs.placeBet(diff, playerId);
        pgs.setBet(true);
        pgs.getPlayerNeedsToCall()[playerId] = false;

        // Others can't check
        for (int i = 0; i < gameState.getNPlayers(); i++) {
            if (i != playerId && !pgs.getPlayerFold()[i] && pgs.getCurrentMoney()[i] > 0) {
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
        return "Raise x" + multiplier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Raise)) return false;
        Raise raise = (Raise) o;
        return playerId == raise.playerId && multiplier == raise.multiplier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, multiplier);
    }

    @Override
    public Raise copy() {
        return new Raise(playerId, multiplier);
    }

}
