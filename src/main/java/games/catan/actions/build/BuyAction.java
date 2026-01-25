package games.catan.actions.build;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.catan.CatanActionFactory;
import games.catan.CatanGameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BuyAction extends AbstractAction implements IExtendedSequence {

    public enum BuyType {
        Settlement,
        City,
        Road,
        DevCard
    }

    public final int playerID;
    public final BuyType type;

    boolean executed;

    public BuyAction(int playerID, BuyType type) {
        this.playerID = playerID;
        this.type = type;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        if (type == BuyType.Settlement) return CatanActionFactory.getBuySettlementActions((CatanGameState) state, playerID);
        else if (type == BuyType.Road) return CatanActionFactory.getBuyRoadActions((CatanGameState) state, playerID, false);
        return new ArrayList<>();
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public BuyAction copy() {
        BuyAction ba = new BuyAction(playerID, type);
        ba.executed = executed;
        return ba;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuyAction)) return false;
        BuyAction buyAction = (BuyAction) o;
        return playerID == buyAction.playerID && executed == buyAction.executed && type == buyAction.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, type, executed);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "p" + playerID + " Buy:" + type;
    }
}
