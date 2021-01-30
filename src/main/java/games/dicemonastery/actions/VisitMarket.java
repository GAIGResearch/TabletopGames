package games.dicemonastery.actions;

import com.google.common.collect.ImmutableMap;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Dice;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.IExtendedSequence;

import java.util.*;
import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static java.util.stream.Collectors.*;

public class VisitMarket extends UseMonk implements IExtendedSequence {

    ImmutableMap<Resource, Integer> buyPrices;
    ImmutableMap<Resource, Integer> sellPrices;
    int player = -1;
    boolean traded = false;

    {
        buyPrices = ImmutableMap.of(BREAD, 2, CALF_SKIN, 3, BEER, 3);
        sellPrices = ImmutableMap.of(BREAD, 1, BEER, 2, MEAD, 2);
    }

    public VisitMarket() {
        super(1);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.setActionInProgress(this);
        player = state.getCurrentPlayer();
        return true;
    }

    @Override
    public List<AbstractAction> followOnActions(DiceMonasteryGameState state) {
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
    public int getCurrentPlayer(DiceMonasteryGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(DiceMonasteryGameState state, AbstractAction action) {
        if (action instanceof Buy || action instanceof Sell || action instanceof DoNothing)
            traded = true;
    }

    @Override
    public boolean executionComplete(DiceMonasteryGameState state) {
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
