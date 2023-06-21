package games.puertorico.roles;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.*;
import games.puertorico.actions.Build;

import java.util.*;

import static java.util.stream.Collectors.*;

public class Builder extends PuertoRicoRole<Builder> {
    public Builder(PuertoRicoGameState state) {
        super(state, PuertoRicoConstants.Role.BUILDER);
    }

    protected Builder(Builder toCopy) {
        super(toCopy);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        PuertoRicoParameters params = (PuertoRicoParameters) state.getGameParameters();
        int budget = state.getDoubloons(currentPlayer);
        PRPlayerBoard playerBoard = state.getPlayerBoard(currentPlayer);
        int numberOfQuarries = (int) playerBoard.getPlantations().stream()
                .filter(p -> p.crop == PuertoRicoConstants.Crop.QUARRY && p.isOccupied()).count();
        int roleDiscount = roleOwner == currentPlayer ? 1 : 0;
        // we look for all the buildings that the player can afford, and which are available
        // and which we have space for
        int townSize = playerBoard.getTownSize();
        if (townSize == params.townSlotsOnBoard) {
            return Collections.singletonList(new DoNothing());
        }
        Set<PuertoRicoConstants.BuildingType> currentBuildings = playerBoard
                .getBuildings().stream().map(b -> b.buildingType).collect(toSet());
        int currentLargeBuildings = (int) playerBoard.getBuildings().stream()
                .filter(b -> b.buildingType.size == 2).count();
        List<AbstractAction> retValue = state.getAvailableBuildings().stream()
                .filter(b -> b.cost <= budget + roleDiscount + Math.min(numberOfQuarries, b.nMaxQuarryDiscount))
                .filter(b -> townSize + b.size <= params.townSlotsOnBoard)
                .filter(b -> !currentBuildings.contains(b))
                .filter(b -> b.size == 1 || currentLargeBuildings < params.townGridWidth)
                .map(b -> new Build(b, Math.max(0, b.cost - roleDiscount - Math.min(numberOfQuarries, b.vp))))
                .collect(toList());
        retValue.add(new DoNothing());
        return retValue;
    }

    @Override
    public Builder copy() {
        return new Builder(this);
    }
}
