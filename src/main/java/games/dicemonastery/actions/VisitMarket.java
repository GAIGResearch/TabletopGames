package games.dicemonastery.actions;

import com.google.common.collect.ImmutableMap;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.dicemonastery.DiceMonasteryGameState;

import java.util.List;
import java.util.Objects;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static java.util.stream.Collectors.toList;

public class VisitMarket extends UseMonk implements IExtendedSequence {

    ImmutableMap<Resource, Integer> buyPrices;
    ImmutableMap<Resource, Integer> sellPrices;
    int player = -1;
    boolean traded = false;

    {
        buyPrices = ImmutableMap.of(GRAIN, 2, CALF_SKIN, 3);
        sellPrices = ImmutableMap.of(CANDLE, 3, BEER, 1, MEAD, 2);
    }

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
        List<AbstractAction> retValue = buyPrices.entrySet().stream()
                .filter(entry -> state.getResource(state.getCurrentPlayer(), SHILLINGS, STOREROOM) >= entry.getValue())
                .map(entry -> new Buy(entry.getKey(), entry.getValue()))
                .collect(toList());
        retValue.addAll(
                sellPrices.entrySet().stream()
                        .filter(entry -> state.getResource(state.getCurrentPlayer(), entry.getKey(), STOREROOM) >= 1)
                        .map(entry -> new Sell(entry.getKey(), entry.getValue()))
                        .collect(toList())
        );
        if (retValue.isEmpty())
            retValue.add(new DoNothing());
        return retValue;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
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
        retValue.buyPrices = buyPrices;  // immutable Map
        retValue.sellPrices = sellPrices; // immutable Map
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof VisitMarket) {
            VisitMarket other = (VisitMarket) obj;
            return other.traded == traded && other.player == player &&
                    other.sellPrices.equals(sellPrices) &&
                    other.buyPrices.equals(buyPrices);

        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(traded, player, buyPrices, sellPrices);
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
