package actions;

import components.Card;
import content.PropertyString;
import core.GameState;
import utilities.Hash;

import static actions.MovePlayer.playerLocationHash;
import static pandemic.PandemicGameState.playerCardHash;

public class DoNothing implements Action {

   @Override
    public boolean execute(GameState gs) {
       return true;
   }
}
