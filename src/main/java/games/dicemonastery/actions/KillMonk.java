package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryConstants.ActionArea;
import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.GRAVEYARD;

public class KillMonk extends AbstractAction {

    final int piety;

    public KillMonk(int piety) {
        this.piety = piety;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        int player = state.getCurrentPlayer();

        int monkID = state.monksIn(null, player).stream().filter(m -> m.getPiety() == piety).findFirst().orElseThrow(
                () -> new AssertionError("No monks of piety level " + piety + " found")
        ).getComponentID();
        ActionArea currentLocation = state.getMonkLocation(monkID);
        state.moveMonk(monkID, currentLocation, GRAVEYARD);

        return true;
    }

    @Override
    public KillMonk copy() {
        return new KillMonk(piety);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof KillMonk && ((KillMonk) obj).piety == piety;
    }

    @Override
    public int hashCode() {
        return piety * 389 - 4093421;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
    return "Kill Monk of Piety " + piety;
    }
}
