package games.puertorico.roles;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;

import java.util.*;

public class Prospector extends PuertoRicoRole<Prospector>{

    public Prospector(PuertoRicoGameState state) {
        super(state, PuertoRicoConstants.Role.PROSPECTOR);
    }

    public void prePhaseProcessing(PuertoRicoGameState state) {
        Arrays.fill(hasFinished, true);  // nothing to do
        state.changeDoubloons(roleOwner, 1);
    }
    protected Prospector(Prospector toCopy) {
        super(toCopy);
    }
    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        throw new IllegalArgumentException("Prospector should not be asked to compute actions");
    }

    @Override
    public Prospector copy() {
        return new Prospector(this);
    }
}
