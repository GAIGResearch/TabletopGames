package updated_core.players;

import updated_core.actions.IAction;
import updated_core.observations.Observation;

import java.util.ArrayList;

public abstract class AbstractPlayer {
    public final int playerID;

    protected AbstractPlayer(int playerID) {
        this.playerID = playerID;
    }

    public abstract void initializePlayer(Observation observation);
    public abstract void finalizePlayer();
    public abstract int getAction(Observation observation, ArrayList<IAction> actions);
}
