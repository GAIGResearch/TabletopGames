package core.actions;

import core.AbstractGameState;

public class DoNothing implements IAction {

   @Override
    public boolean execute(AbstractGameState gs) {
       return true;
   }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof DoNothing;
    }
}
