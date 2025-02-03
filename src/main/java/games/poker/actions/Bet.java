package games.poker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;
import games.poker.PokerGameState;

import java.util.Objects;

public class Bet extends AbstractAction implements IPrintable {

    private final int playerId;
    private final int amount;

    public Bet(int id, int amount) {
        this.playerId = id;
        this.amount = amount;
    }
    @Override
    public boolean execute(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        pgs.placeBet(amount, playerId);
        pgs.setBet(true);
        pgs.getPlayerNeedsToCall()[playerId] = false;

        // Others can't check
        pgs.getPlayerMustCall(playerId);

        return true;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(getString(gameState));
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Bet " + amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bet)) return false;
        Bet bet = (Bet) o;
        return playerId == bet.playerId && amount == bet.amount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, amount) + 2912;
    }

    @Override
    public Bet copy() {
        return this; // immutable
    }

}
