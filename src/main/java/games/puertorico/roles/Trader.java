package games.puertorico.roles;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.*;
import games.puertorico.actions.Sell;

import java.util.*;
import java.util.stream.Collectors;

public class Trader extends PuertoRicoRole<Trader> {
    public Trader(PuertoRicoGameState state) {
        super(state, PuertoRicoConstants.Role.TRADER);
    }

    protected Trader(Trader toCopy) {
        super(toCopy);
    }

    private List<PuertoRicoConstants.Crop> cropsToSell(PuertoRicoGameState state, int player) {
        boolean hasOffice = state.hasActiveBuilding(player, PuertoRicoConstants.BuildingType.OFFICE);
        return Arrays.stream(PuertoRicoConstants.Crop.values())
                .filter(c -> state.getStoresOf(player, c) > 0)
                .filter(c -> hasOffice || !state.getMarket().contains(c))
                .collect(Collectors.toList());
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        // we get all the crops that the player has in stores that are not in the market
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        PuertoRicoParameters params = (PuertoRicoParameters) state.getGameParameters();
        if (state.getMarket().size() == params.marketCapacity) { // market is full
            return Collections.singletonList(new DoNothing());
        }
        int marketBonus = state.hasActiveBuilding(currentPlayer, PuertoRicoConstants.BuildingType.SMALL_MARKET) ? 1 : 0;
        marketBonus += state.hasActiveBuilding(currentPlayer, PuertoRicoConstants.BuildingType.LARGE_MARKET) ? 2 : 0;
        int bonus = marketBonus + (currentPlayer == roleOwner ? 1 : 0);
        List<AbstractAction> retValue = cropsToSell(state, currentPlayer).stream()
                .map(c -> new Sell(c, c.price + bonus))
                .collect(Collectors.toList());
        if (retValue.isEmpty()) {
            retValue.add(new DoNothing());
        } else {
            retValue.add(new Sell(null, 0));
        }
        return retValue;
    }


    @Override
    protected void postPhaseProcessing(PuertoRicoGameState state) {
        // we clear the market iff it is full
        PuertoRicoParameters params = (PuertoRicoParameters) state.getGameParameters();
        if (state.getMarket().size() == params.marketCapacity) {
            state.emptyMarket();
        }
    }

    @Override
    public Trader copy() {
        return new Trader(this);
    }
}
