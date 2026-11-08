package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolySquare;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A player owes more than their cash, but could raise enough (MonopolyGameState.getRaisableValue): they sell
 * buildings (SellBuilding) and mortgage properties (Mortgage) until their cash covers the debt, which is then paid to
 * the creditor (another player, or -1 for the Bank).
 */
public class RaiseMoney implements IExtendedSequence {

    public final int player;
    public final int creditor;
    public final int amount;
    // for the Jail fine forced after the last failed roll: once it is paid, the player leaves Jail and moves this far
    // (0 for any other debt)
    public final int thenMove;
    boolean complete;

    public RaiseMoney(int player, int creditor, int amount) {
        this(player, creditor, amount, 0);
    }

    public RaiseMoney(int player, int creditor, int amount, int thenMove) {
        this.player = player;
        this.creditor = creditor;
        this.amount = amount;
        this.thenMove = thenMove;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        List<AbstractAction> actions = new ArrayList<>();
        for (MonopolySquare s : state.getProperties(player)) {
            if (state.canSellBuilding(s))
                actions.add(new SellBuilding(s));
            if (state.canMortgage(s))
                actions.add(new Mortgage(s));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        MonopolyGameState state = (MonopolyGameState) gs;
        if (!(action instanceof Mortgage || action instanceof SellBuilding) || state.getCash(player) < amount)
            return;
        complete = true;
        state.pay(player, creditor, amount);
        if (thenMove > 0) {
            state.leaveJail(player);
            state.moveForward(player, thenMove);
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

    @Override
    public RaiseMoney copy() {
        RaiseMoney copy = new RaiseMoney(player, creditor, amount, thenMove);
        copy.complete = complete;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RaiseMoney r && r.player == player && r.creditor == creditor && r.amount == amount &&
                r.thenMove == thenMove && r.complete == complete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, creditor, amount, thenMove, complete) + 640247;
    }

    @Override
    public String toString() {
        return "Raise " + amount + " to pay " + (creditor == -1 ? "the Bank" : "player " + creditor);
    }
}
