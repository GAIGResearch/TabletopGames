package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.components.Building;

public class ShipCargo extends AbstractAction {

    public final PuertoRicoConstants.Crop cargo;
    public final int shipNumber;
    public final int amountToShip;

    public ShipCargo(PuertoRicoConstants.Crop cargo, int shipNumber, int amountToShip) {
        this.cargo = cargo;
        this.shipNumber = shipNumber;
        this.amountToShip = amountToShip;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // We move the requisite goods from the player store to the relevant ship
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        int player = state.getCurrentPlayer();
        if (shipNumber == 10 + player) {
            // This is the player's own ship
            // we mark it as used, and sell the stuff)
            Building wharf = state.getPlayerBoard(player).getBuildings().stream()
                    .filter(b -> b.buildingType == PuertoRicoConstants.BuildingType.WHARF)
                    .findFirst().orElse(null);
            if (wharf == null)
                throw new IllegalArgumentException("Player does not have a wharf");
            wharf.setUsed();
            state.changeSupplyOf(cargo, amountToShip);  // and we put the goods back in the supply
        } else {
            // this is a public ship
            state.getShip(shipNumber).load(cargo, amountToShip);
        }
        state.getPlayerBoard(state.getCurrentPlayer()).harvest(cargo, -amountToShip);
        state.addVP(player, amountToShip);
        if (state.hasActiveBuilding(player, PuertoRicoConstants.BuildingType.HARBOUR))
            state.addVP(player, 1);
        return true;
    }

    @Override
    public ShipCargo copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ShipCargo) {
            ShipCargo other = (ShipCargo) obj;
            return this.cargo == other.cargo && this.shipNumber == other.shipNumber && this.amountToShip == other.amountToShip;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return cargo.ordinal() + 31 * shipNumber + 31 * 31 * amountToShip;
    }

    @Override
    public String toString() {
        return String.format("Ship %d %s on ship %d", amountToShip, cargo.toString(), shipNumber);
    }
    @Override
    public String getString(AbstractGameState gameState) {
        if (shipNumber >= 10) {
            return String.format("Load %d %s on private ship (Wharf)", amountToShip, cargo.toString());
        }
        int shipTotalCapacity = ((PuertoRicoGameState) gameState).getShip(shipNumber).capacity;
        int shipCapacity = ((PuertoRicoGameState) gameState).getShip(shipNumber).getAvailableCapacity();
        return String.format("Load %d %s on ship with capacity %d of %d", amountToShip, cargo.toString(), shipCapacity, shipTotalCapacity);
    }
}
