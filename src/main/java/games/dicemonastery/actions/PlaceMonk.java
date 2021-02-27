package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.Monk;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class PlaceMonk extends AbstractAction implements IExtendedSequence {

    public final ActionArea destination;
    int monkPiety = 0;
    public final int playerId;

    public PlaceMonk(int playerId, ActionArea area) {
        this.playerId = playerId;
        destination = area;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        state.setActionInProgress(this);
        return true;
    }

    void updateMonk(int piety, DiceMonasteryGameState state) {
        monkPiety = piety;
        List<Monk> matchingMonks = availableMonks(state);

        if (!matchingMonks.isEmpty()) {
            state.moveMonk(matchingMonks.get(0).getComponentID(), ActionArea.DORMITORY, destination);
        } else {
            throw new AssertionError(String.format("No monk found in Dormitory for player %d with piety of %d", playerId, monkPiety));
        }
    }

    private List<Monk> availableMonks(DiceMonasteryGameState state) {
        return state.monksIn(null, playerId).stream()
                .filter(m -> (monkPiety == 0 || m.getPiety() == monkPiety)
                        && m.getPiety() >= destination.dieMinimum
                        && state.getMonkLocation(m.getComponentID()) == ActionArea.DORMITORY)
                .collect(toList());
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        // we can pick any of our monks...but we can ignore duplicate values
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        Set<Integer> pietyValues = availableMonks(state).stream().map(Monk::getPiety).collect(toSet());
        return pietyValues.stream().map(ChooseMonk::new).collect(toList());
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerId;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        // not used
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return monkPiety != 0;
    }

    @Override
    public PlaceMonk copy() {
        PlaceMonk retValue = new PlaceMonk(playerId, destination);
        retValue.monkPiety = monkPiety;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlaceMonk))
            return false;
        PlaceMonk other = (PlaceMonk) obj;
        return other.monkPiety == monkPiety && other.destination == destination;
    }

    @Override
    public int hashCode() {
        return Objects.hash(monkPiety, destination, playerId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Move monk (piety: %d, player: %d) to %s", monkPiety, playerId, destination);
    }
}

class ChooseMonk extends AbstractAction {

    final int piety;

    ChooseMonk(int id) {
        piety = id;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        PlaceMonk parent = (PlaceMonk) state.currentActionInProgress();
        // this will (correctly) throw an exception if not a PlaceMonk on the stack
        parent.updateMonk(piety, state);
        return true;
    }

    @Override
    public AbstractAction copy() {
        // no mutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChooseMonk) {
            ChooseMonk other = (ChooseMonk) obj;
            return piety == other.piety;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return piety * 567;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Choose Monk of Piety " + piety;
    }
}
