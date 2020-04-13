package pandemic.actions;

import actions.Action;
import core.GameState;
import pandemic.PandemicGameState;

public class QuietNight implements Action {

   @Override
    public boolean execute(GameState gs) {
       return true;
   }
}
