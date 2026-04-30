package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;

public class UnderdogAdviser implements IAdviceFilter {
    @Override
    public boolean advise(AbstractGameState state, AbstractPlayer player) {
        // TODO: We need to determine if they are behind.
        // We need a heuristic to estimate this....which could be a full oracle searh, but I'm happier for the moment if we just
        // make this a prelearned heuristic.
        return false;
    }
}
