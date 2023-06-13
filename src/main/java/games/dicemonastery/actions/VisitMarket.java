package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.MarketCard;

import java.util.ArrayList;
import java.util.List;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class VisitMarket extends UseMonk implements IExtendedSequence {

    int player = -1;
    boolean traded = false;

    public VisitMarket() {
        super(1);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        player = state.getCurrentPlayer();
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        MarketCard market = state.getCurrentMarket();

        List<AbstractAction> retValue = new ArrayList<>();
        int money = state.getResource(player, SHILLINGS, STOREROOM);
        if (money >= market.calf_skin && state.getSeason() == DiceMonasteryConstants.Season.SPRING)
            retValue.add(new Buy(CALF_SKIN, market.calf_skin));
        if (money >= market.grain)
            retValue.add(new Buy(GRAIN, market.grain));
        if (market.inkType != null && money >= market.inkPrice)
            retValue.add(new Buy(market.inkType, market.inkPrice));
        if (state.getResource(player, BEER, STOREROOM) > 0)
            retValue.add(new Sell(BEER, market.beer));
        if (state.getResource(player, MEAD, STOREROOM) > 0)
            retValue.add(new Sell(MEAD, market.mead));
        if (state.getResource(player, CANDLE, STOREROOM) > 0)
            retValue.add(new Sell(CANDLE, market.candle));

        if (retValue.isEmpty())
            retValue.add(new DoNothing());
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof Buy || action instanceof Sell || action instanceof DoNothing)
            traded = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return traded;
    }

    @Override
    public VisitMarket copy() {
        VisitMarket retValue = new VisitMarket();
        retValue.traded = traded;
        retValue.player = player;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof VisitMarket) {
            VisitMarket other = (VisitMarket) obj;
            return other.traded == traded && other.player == player;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (traded ? 2411 : 0) + player * 3023 + 43;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Visit Market";
    }
}
