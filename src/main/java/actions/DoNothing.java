package actions;

import actions.Action;
import core.GameState;

public class DoNothing implements Action {

   @Override
    public boolean execute(GameState gs) {
       return true;
   }
}
