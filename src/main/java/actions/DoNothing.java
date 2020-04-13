package actions;

import core.GameState;

public class DoNothing implements Action {

   @Override
    public boolean execute(GameState gs) {
       return true;
   }
}
