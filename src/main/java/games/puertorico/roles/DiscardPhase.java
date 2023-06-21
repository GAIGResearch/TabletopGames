package games.puertorico.roles;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.*;
import games.puertorico.actions.*;
import games.puertorico.components.Ship;

import java.util.*;
import java.util.stream.Collectors;

import static games.puertorico.PuertoRicoConstants.BuildingType.*;

public class DiscardPhase extends PuertoRicoRole<DiscardPhase> {

    public DiscardPhase(PuertoRicoGameState state) {
        super(state, PuertoRicoConstants.Role.DISCARD);
    }

    public DiscardPhase(DiscardPhase toCopy) {
        super(toCopy);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        return _computeAvailableActions(gs, currentPlayer);
    }

    private List<AbstractAction> _computeAvailableActions(AbstractGameState gs, int player) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        int warehouseSlotsAvailable = 0;
        if (state.hasActiveBuilding(player, SMALL_WAREHOUSE)) warehouseSlotsAvailable++;
        if (state.hasActiveBuilding(player, LARGE_WAREHOUSE)) warehouseSlotsAvailable += 2;
        // then we subtract the number already used
        warehouseSlotsAvailable -= state.getPlayerBoard(player).getCropsInWarehouses().size();

        if (warehouseSlotsAvailable > 0) {
            // in this case we first make decisions on what to put in the warehouse(s)
            List<AbstractAction> retValue = Arrays.stream(PuertoRicoConstants.Crop.values())
                    .filter(c -> state.getStoresOf(player, c) > 0 && !state.getPlayerBoard(player).getCropsInWarehouses().contains(c))
                    .map(WarehouseStorage::new)
                    .collect(Collectors.toList());
            if (retValue.size() <= warehouseSlotsAvailable) {
                // we can store everything in the warehouse
                for (AbstractAction a : retValue) {
                    WarehouseStorage ws = (WarehouseStorage) a;
                    state.getPlayerBoard(player).store(ws.storedCrop);
                }
                retValue = Collections.singletonList(new DoNothing());
            }
            return retValue;
        }

        // We can keep one good (ignoring the crops in warehouses)
        List<AbstractAction> retValue = Arrays.stream(PuertoRicoConstants.Crop.values())
                .filter(c -> state.getStoresOf(player, c) > 0 && !state.getPlayerBoard(player).getCropsInWarehouses().contains(c))
                .map(DiscardGoodsExcept::new)
                .collect(Collectors.toList());
        // We now have the special case that we have a single good
        if (retValue.isEmpty() || (retValue.size() == 1 && state.getStoresOf(player, ((DiscardGoodsExcept) retValue.get(0)).crop) == 1)) {
            retValue = Collections.singletonList(new DoNothing());
        }
        return retValue;
    }

    @Override
    protected void postPhaseProcessing(PuertoRicoGameState state) {
        // then we run through each ship, and if it is full we empty it to the supply
        for (Ship ship : state.getShips()) {
            if (ship.getAvailableCapacity() == 0) {
                state.changeSupplyOf(ship.getCurrentCargo(), ship.capacity);
                ship.unload();
            }
        }
        for (int p = 0; p < state.getNPlayers(); p++) {
            state.getPlayerBoard(p).clearCropsInWarehouses();
        }
    }

    @Override
    public DiscardPhase copy() {
        return new DiscardPhase(this);
    }


}
