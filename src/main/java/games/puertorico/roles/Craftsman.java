package games.puertorico.roles;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.*;
import games.puertorico.actions.GainCrop;
import games.puertorico.components.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Craftsman extends PuertoRicoRole<Craftsman> {

    List<PuertoRicoConstants.Crop> producedByRoleOwner;

    public Craftsman(PuertoRicoGameState state) {
        super(state, PuertoRicoConstants.Role.CRAFTSMAN);
    }

    protected Craftsman(Craftsman toCopy) {
        super(toCopy);
        producedByRoleOwner = new ArrayList<>(toCopy.producedByRoleOwner);
    }

    @Override
    protected void prePhaseProcessing(PuertoRicoGameState prgs) {
        // we immediately increment all goods for players if they have a staffed production building
        // and then the only action to take is for the roleSelection player to
        // pick an extra good
        producedByRoleOwner = new ArrayList<>();
        for (int player = 0; player < prgs.getNPlayers(); player++) {
            PRPlayerBoard pb = prgs.getPlayerBoard(player);
            Map<PuertoRicoConstants.Crop, Long> production = pb.getPlantations().stream()
                    .filter(Plantation::isOccupied)
                    .map(p -> p.crop)
                    .collect(groupingBy(c -> c, counting()));
            int cropsHarvested = 0;
            for (PuertoRicoConstants.Crop crop : production.keySet()) {
                if (crop == PuertoRicoConstants.Crop.QUARRY) continue;
                int productionCapacity = crop == PuertoRicoConstants.Crop.CORN ? 100 : pb.getBuildings().stream()
                                .filter(b -> b instanceof ProductionBuilding && ((ProductionBuilding) b).cropType == crop)
                                .mapToInt(Building::getOccupation).sum();
                int amountProduced = Math.min(productionCapacity, production.get(crop).intValue());
                // we now check if we have enough
                amountProduced = Math.min(amountProduced, prgs.getSupplyOf(crop));
                if (amountProduced > 0) {
                    cropsHarvested++;
                    pb.harvest(crop, amountProduced);
                    prgs.changeSupplyOf(crop, -amountProduced);
                    if (player == roleOwner)
                        producedByRoleOwner.add(crop);
                }
            }
            if (prgs.hasActiveBuilding(player, PuertoRicoConstants.BuildingType.FACTORY)){
                PuertoRicoParameters params = (PuertoRicoParameters) prgs.getGameParameters();
                prgs.changeDoubloons(player, params.factoryBonuses[cropsHarvested]);
            }
        }
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        PuertoRicoGameState prgs = (PuertoRicoGameState) state;
        List<AbstractAction> actions = producedByRoleOwner.stream()
                .filter(crop -> prgs.getSupplyOf(crop) > 0)
                .map(crop -> new GainCrop(crop, 1))
                .collect(toList());
        if (actions.isEmpty())
            actions.add(new DoNothing());
        return actions;
    }


    @Override
    public Craftsman copy() {
        return new Craftsman(this);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && producedByRoleOwner.equals(((Craftsman) obj).producedByRoleOwner);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + 31 * producedByRoleOwner.hashCode();
    }
}
