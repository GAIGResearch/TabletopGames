package pandemic.actions;

import actions.Action;
import core.GameState;
import pandemic.PandemicGameState;

public class QuietNight implements Action {

   @Override
    public boolean execute(GameState gs) {
       return true;
   }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        return other instanceof QuietNight;
    }
}
