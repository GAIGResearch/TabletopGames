package pandemic.actions;

import actions.Action;
import core.GameState;

public class PlayCards implements Action {
    @Override
    public boolean execute(GameState gs) {
        return false;
    }
    // TODO: like PlayCard, but play multiple cards, check conditions, resolve effects (Discover a cure)


    @Override
    public boolean equals(Object other)
    {
        //TODO: Update when this class is implemented.
        if (this == other) return true;
        return other instanceof PlayCards;
    }
}
