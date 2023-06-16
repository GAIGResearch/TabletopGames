package games.puertorico.roles;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.*;
import games.puertorico.actions.*;

import java.util.*;

import static games.puertorico.PuertoRicoConstants.BuildingType.*;
import static java.util.stream.Collectors.*;

public class Settler extends PuertoRicoRole<Settler> {

    boolean[] haciendaStep;

    public Settler(PuertoRicoGameState state) {
        super(state, PuertoRicoConstants.Role.SETTLER);
        haciendaStep = new boolean[state.getNPlayers()];
        for (int p = 0; p < state.getNPlayers(); p++)
            haciendaStep[p] = state.hasActiveBuilding(p, HACIENDA);
    }

    protected Settler(Settler toCopy) {
        super(toCopy);
        haciendaStep = Arrays.copyOf(toCopy.haciendaStep, toCopy.haciendaStep.length);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        PuertoRicoParameters params = (PuertoRicoParameters) state.getGameParameters();
        if (state.getPlayerBoard(state.getCurrentPlayer()).getPlantations().size() == params.plantationSlotsOnBoard) {
            haciendaStep[currentPlayer] = false;
            return Collections.singletonList(new DoNothing()); // no more plantations can be added
        }

        if (haciendaStep[currentPlayer] && state.numberOfPlantationsInStack() > 0) {
            // first they make a hacienda decision
            return Arrays.asList(new DrawPlantation(null), new DoNothing());
        }
        haciendaStep[currentPlayer] = false;

        Set<PuertoRicoConstants.Crop> uniqueVisibleCrops = state.getAvailablePlantations().stream()
                .map(p -> p.crop).collect(toSet());
        List<AbstractAction> retValue = uniqueVisibleCrops.stream().map(DrawPlantation::new).collect(toList());
        if (state.getQuarriesLeft() > 0 && (currentPlayer == roleOwner || state.hasActiveBuilding(currentPlayer, CONSTRUCTION_HUT)))
            retValue.add(new BuildQuarry());
        retValue.add(new DoNothing()); // we can always choose not pick a plantation
        return retValue;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        if (haciendaStep[currentPlayer]) {
            if (action.equals(new DrawPlantation(null)) || action.equals(new DoNothing())) {
                // we have finished the hacienda step
                haciendaStep[currentPlayer] = false;
            } else {
                if (!(action instanceof SelectRole))
                    throw new IllegalArgumentException("Invalid action taken in Hacienda step: " + action);
            }
        } else {
            super._afterAction(gs, action);
        }
    }

    @Override
    public Settler copy() {
        return new Settler(this);
    }

    @Override
    protected void postPhaseProcessing(PuertoRicoGameState state) {
        // we discard all remaining plantations, and re-draw to a full set
        state.discardRemainingPlantations();
        state.drawNewVisiblePlantations();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Settler) {
            return super.equals(o) && Arrays.equals(haciendaStep, ((Settler) o).haciendaStep);
        }
        return false;
    }
}
