package pandemic.actions;

import actions.IAction;
import core.AbstractGameState;
import turnorder.TurnOrder;

public class QuietNight implements IAction {

   @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
       return true;
   }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof QuietNight;
    }
}
