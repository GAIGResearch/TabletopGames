package games.puertorico.roles;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.actions.ShipCargo;
import games.puertorico.components.Building;
import games.puertorico.components.Ship;

import java.util.*;
import java.util.stream.Collectors;

import static games.puertorico.PuertoRicoConstants.BuildingType.WHARF;

public class Captain extends PuertoRicoRole<Captain> {

    boolean captainShippedSomething = false;

    public Captain(PuertoRicoGameState state) {
        super(state, PuertoRicoConstants.Role.CAPTAIN);
    }

    private Captain(Captain toCopy) {
        super(toCopy);
        captainShippedSomething = toCopy.captainShippedSomething;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        List<AbstractAction> retValue = new ArrayList<>();

        Set<PuertoRicoConstants.Crop> loadedCrops = state.getShips().stream()
                .map(Ship::getCurrentCargo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (PuertoRicoConstants.Crop c : PuertoRicoConstants.Crop.values()) {
            if (state.getStoresOf(currentPlayer, c) > 0) {
                for (int s = 0; s < state.getShips().size(); s++) {
                    Ship ship = state.getShip(s);
                    if (ship.getAvailableCapacity() > 0 &&
                            (ship.getCurrentCargo() == c || (ship.getCurrentCargo() == null && !loadedCrops.contains(c)))) {
                        // we can load on this ship
                        retValue.add(new ShipCargo(c, s, Math.min(ship.getAvailableCapacity(), state.getStoresOf(currentPlayer, c))));
                    }
                }
            }
        }
        if (state.hasActiveBuilding(currentPlayer, WHARF)) {
            // in this case we can still ship from the wharf
            for (PuertoRicoConstants.Crop c : PuertoRicoConstants.Crop.values()) {
                if (state.getStoresOf(currentPlayer, c) > 0) {
                    retValue.add(new ShipCargo(c, 10 + currentPlayer, state.getStoresOf(currentPlayer, c)));
                }
            }
        }

        if (retValue.isEmpty()) {
            retValue.add(new DoNothing());
        }
        return retValue;
    }

    @Override
    public Captain copy() {
        return new Captain(this);
    }


    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        if (action instanceof ShipCargo && currentPlayer == roleOwner) {
            captainShippedSomething = true;
        }
        super._afterAction(gs, action);
    }

    @Override
    protected void postPhaseProcessing(PuertoRicoGameState state) {
        if (captainShippedSomething) {
            state.addVP(roleOwner, 1);
        }
        // we also unset all Wharfs
        for (int p = 0; p < state.getNPlayers(); p++) {
            state.getPlayerBoard(p).getBuildings().stream()
                    .filter(b -> b.buildingType == WHARF)
                    .findFirst().ifPresent(Building::refresh);
        }
        // We then put run through the DiscardPhase ... but only if we need to
        DiscardPhase discardPhase = new DiscardPhase(state);
        discardPhase.startNewPhase(state);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Captain) {
            return super.equals(obj) && captainShippedSomething == ((Captain) obj).captainShippedSomething;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * (captainShippedSomething ? 1 : 0);
    }

    @Override
    public String toString() {
        return String.format("Captain %d, CurrentShipper: %d", roleOwner, currentPlayer);
    }
}
