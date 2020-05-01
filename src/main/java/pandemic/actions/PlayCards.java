package pandemic.actions;

import actions.IAction;
import core.AbstractGameState;
import turnorder.TurnOrder;

public class PlayCards implements IAction {
    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
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
