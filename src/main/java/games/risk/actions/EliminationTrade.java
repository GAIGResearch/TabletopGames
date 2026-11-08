package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.risk.RiskGameState;
import games.risk.RiskParameters;

import java.util.List;

/**
 * After eliminating a player and taking their cards, a player left with eliminationTradeLimit or more cards must
 * trade sets at once (TradeCards) until they hold fewer than handLimit, then place the armies won (PlaceArmy) before
 * they carry on attacking.
 */
public class EliminationTrade implements IExtendedSequence {

    public final int player;

    public EliminationTrade(int player) {
        this.player = player;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        if (mustTrade(state))
            return TradeCards.options(state, player);
        return PlaceArmy.options(state, player);
    }

    private boolean mustTrade(RiskGameState state) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        return state.getHand(player).getSize() >= params.handLimit;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        // what is left to do is read from the hand and the armies left to place; armies that fit nowhere are lost
        RiskGameState state = (RiskGameState) gs;
        if (!mustTrade(state))
            state.loseArmiesWithNoRoom(player);
    }

    @Override
    public boolean executionComplete(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        return !mustTrade(state) && state.getArmiesToPlace(player) == 0;
    }

    @Override
    public EliminationTrade copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EliminationTrade other && other.player == player;
    }

    @Override
    public int hashCode() {
        return player + 731221;
    }

    @Override
    public String toString() {
        return "EliminationTrade(" + player + ")";
    }
}
